const { app, BrowserWindow, dialog, ipcMain, shell } = require('electron');
const { spawn, spawnSync } = require('child_process');
const fs = require('fs');
const http = require('http');
const path = require('path');

app.setName('GrowthGarden');

const BACKEND_PORT = 18080;
const BACKEND_URL = `http://127.0.0.1:${BACKEND_PORT}/garden`;
const JAR_NAME = 'lyy-0.0.1-SNAPSHOT.jar';
const UPDATE_CHECK_TIMEOUT_MS = 30000;

let mainWindow = null;
let backend = null;
let quitting = false;
let autoUpdater = null;
let updateInfo = null;
let updateState = { phase: 'idle' };

function log(message) {
  try {
    const logsDir = path.join(app.getPath('userData'), 'logs');
    fs.mkdirSync(logsDir, { recursive: true });
    fs.appendFileSync(
      path.join(logsDir, 'desktop.log'),
      `[${new Date().toISOString()}] ${message}\n`,
      'utf8'
    );
  } catch {
    // Logging must never prevent the app from starting.
  }
}

function backendPaths() {
  if (app.isPackaged) {
    return {
      jar: path.join(process.resourcesPath, 'app.jar'),
      java: path.join(process.resourcesPath, 'runtime', 'bin', 'java.exe'),
    };
  }

  return {
    jar: path.resolve(__dirname, '..', '..', 'target', JAR_NAME),
    java: process.env.JAVA_HOME
      ? path.join(process.env.JAVA_HOME, 'bin', 'java.exe')
      : 'java',
  };
}

function startBackend() {
  const { jar, java } = backendPaths();
  if (!fs.existsSync(jar)) {
    throw new Error(`找不到后端程序：${jar}`);
  }

  const userData = app.getPath('userData');
  const logsDir = path.join(userData, 'logs');
  fs.mkdirSync(logsDir, { recursive: true });
  const logStream = fs.createWriteStream(
    path.join(logsDir, 'backend.log'),
    { flags: 'a' }
  );

  backend = spawn(
    java,
    [
      `-Duser.home=${userData}`,
      '-jar',
      jar,
      '--spring.profiles.active=desktop',
      `--server.port=${BACKEND_PORT}`,
    ],
    {
      cwd: userData,
      windowsHide: true,
      stdio: ['ignore', 'pipe', 'pipe'],
    }
  );

  backend.stdout.pipe(logStream);
  backend.stderr.pipe(logStream);
  backend.on('exit', (code) => {
    log(`backend exited with code ${code}`);
    if (!quitting) {
      dialog.showErrorBox(
        'Growth Garden 无法启动',
        `后端程序已退出（code ${code}）。\n请查看日志：${path.join(logsDir, 'backend.log')}`
      );
      app.quit();
    }
  });
}

function probeBackend() {
  return new Promise((resolve) => {
    const request = http.get(BACKEND_URL, (response) => {
      response.resume();
      resolve(response.statusCode >= 200 && response.statusCode < 500);
    });
    request.setTimeout(1500, () => {
      request.destroy();
      resolve(false);
    });
    request.on('error', () => resolve(false));
  });
}

async function waitForBackend(timeoutMs = 90000) {
  const started = Date.now();
  while (Date.now() - started < timeoutMs) {
    if (await probeBackend()) return;
    if (backend && backend.exitCode !== null) {
      throw new Error('后端进程提前退出');
    }
    await new Promise((resolve) => setTimeout(resolve, 500));
  }
  throw new Error('后端启动超时');
}

function stopBackend() {
  if (!backend || backend.exitCode !== null) return;
  try {
    if (process.platform === 'win32') {
      spawnSync(
        'taskkill',
        ['/pid', String(backend.pid), '/T', '/F'],
        { windowsHide: true }
      );
    } else {
      backend.kill('SIGTERM');
    }
  } catch (error) {
    log(`failed to stop backend: ${error.message}`);
  }
}

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1180,
    height: 820,
    minWidth: 900,
    minHeight: 620,
    show: false,
    backgroundColor: '#081323',
    title: 'Growth Garden',
    icon: path.join(__dirname, 'build', 'icon.png'),
    webPreferences: {
      contextIsolation: true,
      nodeIntegration: false,
      preload: path.join(__dirname, 'preload.js'),
    },
  });

  mainWindow.setMenuBarVisibility(false);
  mainWindow.once('ready-to-show', () => mainWindow.show());
  mainWindow.on('closed', () => {
    mainWindow = null;
  });

  const loadingHtml = `
    <!doctype html>
    <html lang="zh-CN">
      <meta charset="utf-8">
      <body style="margin:0;display:grid;min-height:100vh;place-items:center;background:#081323;color:#ead8b2;font-family:'Microsoft YaHei',sans-serif;">
        <div style="text-align:center;letter-spacing:.12em;">
          <p style="font-size:13px;color:#d7c39c;">GROWTH GARDEN</p>
          <p style="font-size:15px;">正在准备你的花园……</p>
        </div>
      </body>
    </html>
  `;
  mainWindow.loadURL(`data:text/html;charset=utf-8,${encodeURIComponent(loadingHtml)}`);

  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    if (url.startsWith('http://') || url.startsWith('https://')) {
      shell.openExternal(url);
    }
    return { action: 'deny' };
  });
}

function publishUpdate(next) {
  updateState = {
    ...next,
    currentVersion: app.getVersion(),
  };
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.webContents.send('update:state', updateState);
  }
}

function updaterErrorMessage(error, retry) {
  return {
    phase: 'error',
    message: error instanceof Error ? error.message : String(error),
    retry,
  };
}

async function checkForUpdatesWithTimeout() {
  let timer;
  const timeout = new Promise((_, reject) => {
    timer = setTimeout(
      () => reject(new Error('检查更新超时，请稍后重试。')),
      UPDATE_CHECK_TIMEOUT_MS
    );
  });
  try {
    return await Promise.race([autoUpdater.checkForUpdates(), timeout]);
  } finally {
    clearTimeout(timer);
  }
}

function configureUpdater() {
  ipcMain.handle('update:get-state', () => updateState);

  ipcMain.handle('update:check', async () => {
    if (!app.isPackaged) {
      publishUpdate({ phase: 'error', message: '开发模式不支持自动更新。', retry: 'check' });
      return updateState;
    }
    if (!autoUpdater) {
      publishUpdate({ phase: 'error', message: '更新组件没有加载。', retry: 'check' });
      return updateState;
    }
    if (['checking', 'downloading', 'installing'].includes(updateState.phase)) {
      return updateState;
    }
    publishUpdate({ phase: 'checking' });
    try {
      await checkForUpdatesWithTimeout();
    } catch (error) {
      log(`update check failed: ${error.message}`);
      publishUpdate(updaterErrorMessage(error, 'check'));
    }
    return updateState;
  });

  ipcMain.handle('update:download', async () => {
    if (!autoUpdater || !updateInfo) {
      publishUpdate({ phase: 'error', message: '没有可下载的更新。', retry: 'check' });
      return updateState;
    }
    if (['downloading', 'installing'].includes(updateState.phase)) {
      return updateState;
    }
    publishUpdate({ phase: 'downloading', version: updateInfo.version, percent: 0 });
    try {
      await autoUpdater.downloadUpdate();
    } catch (error) {
      log(`update download failed: ${error.message}`);
      publishUpdate(updaterErrorMessage(error, 'download'));
    }
    return updateState;
  });

  ipcMain.handle('update:install', () => {
    if (!autoUpdater || updateState.phase !== 'ready') {
      return updateState;
    }
    publishUpdate({ phase: 'installing', version: updateInfo?.version });
    quitting = true;
    autoUpdater.quitAndInstall(false, true);
    return updateState;
  });

  if (!app.isPackaged) {
    return;
  }

  try {
    ({ autoUpdater } = require('electron-updater'));
  } catch (error) {
    log(`electron-updater unavailable: ${error.message}`);
    publishUpdate({ phase: 'error', message: '更新组件没有加载。', retry: 'check' });
    return;
  }

  autoUpdater.autoDownload = false;
  autoUpdater.autoInstallOnAppQuit = false;
  autoUpdater.logger = {
    info: (message) => log(`updater: ${message}`),
    warn: (message) => log(`updater warn: ${message}`),
    error: (message) => log(`updater error: ${message}`),
  };

  autoUpdater.on('checking-for-update', () => {
    publishUpdate({ phase: 'checking' });
  });

  autoUpdater.on('update-available', (info) => {
    updateInfo = info;
    publishUpdate({
      phase: 'available',
      version: info.version,
      releaseNotes: info.releaseNotes ? String(info.releaseNotes) : '',
    });
  });

  autoUpdater.on('update-not-available', () => {
    updateInfo = null;
    publishUpdate({ phase: 'idle', message: '已经是最新版本。' });
  });

  autoUpdater.on('download-progress', (progress) => {
    if (!updateInfo) return;
    if (mainWindow) mainWindow.setProgressBar(progress.percent / 100);
    publishUpdate({
      phase: 'downloading',
      version: updateInfo.version,
      percent: Math.floor(progress.percent),
      transferred: progress.transferred,
      total: progress.total,
      bytesPerSecond: progress.bytesPerSecond,
    });
  });

  autoUpdater.on('update-downloaded', (info) => {
    updateInfo = info;
    if (mainWindow) mainWindow.setProgressBar(-1);
    publishUpdate({ phase: 'ready', version: info.version });
  });

  autoUpdater.on('error', (error) => {
    log(`updater error: ${error.message}`);
    if (mainWindow) mainWindow.setProgressBar(-1);
    const retry = updateState.phase === 'downloading' ? 'download' : 'check';
    publishUpdate(updaterErrorMessage(error, retry));
  });

  setTimeout(() => {
    if (updateState.phase === 'idle') {
      checkForUpdatesWithTimeout().catch((error) => {
        log(`update check failed: ${error.message}`);
      });
    }
  }, 5000);
}

async function start() {
  createWindow();

  try {
    startBackend();
    await waitForBackend();
    await mainWindow.loadURL(BACKEND_URL);
  } catch (error) {
    log(`startup failed: ${error.message}`);
    dialog.showErrorBox(
      'Growth Garden 无法启动',
      `${error.message}\n\n日志目录：${path.join(app.getPath('userData'), 'logs')}`
    );
    quitting = true;
    stopBackend();
    app.quit();
  }
}

if (!app.requestSingleInstanceLock()) {
  app.quit();
} else {
  app.on('second-instance', () => {
    if (mainWindow) {
      if (mainWindow.isMinimized()) mainWindow.restore();
      mainWindow.focus();
    }
  });

  app.whenReady().then(() => {
    app.setAppUserModelId('com.grow.lyy.desktop');
    updateState = { phase: 'idle', currentVersion: app.getVersion() };
    configureUpdater();
    start();
  });
}

app.on('before-quit', () => {
  quitting = true;
  stopBackend();
});

app.on('window-all-closed', () => {
  app.quit();
});
