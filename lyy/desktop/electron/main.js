const { app, BrowserWindow, dialog, shell } = require('electron');
const { spawn, spawnSync } = require('child_process');
const fs = require('fs');
const http = require('http');
const path = require('path');

app.setName('GrowthGarden');

const BACKEND_PORT = 18080;
const BACKEND_URL = `http://127.0.0.1:${BACKEND_PORT}/garden`;
const JAR_NAME = 'lyy-0.0.1-SNAPSHOT.jar';

let mainWindow = null;
let backend = null;
let quitting = false;

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

function setupUpdater() {
  if (!app.isPackaged) return;

  let autoUpdater;
  try {
    ({ autoUpdater } = require('electron-updater'));
  } catch (error) {
    log(`electron-updater unavailable: ${error.message}`);
    return;
  }

  autoUpdater.autoDownload = false;
  autoUpdater.on('error', (error) => log(`updater error: ${error.message}`));

  autoUpdater.on('update-available', async (info) => {
    if (!mainWindow) return;
    const result = await dialog.showMessageBox(mainWindow, {
      type: 'info',
      title: '发现新版本',
      message: `Growth Garden ${info.version} 已经准备好。`,
      detail: info.releaseNotes
        ? String(info.releaseNotes)
        : '可以现在下载，下载完成后重新启动即可更新。',
      buttons: ['稍后', '下载更新'],
      defaultId: 1,
      cancelId: 0,
    });
    if (result.response === 1) {
      autoUpdater.downloadUpdate().catch((error) => {
        log(`download failed: ${error.message}`);
        dialog.showErrorBox('更新下载失败', error.message);
      });
    }
  });

  autoUpdater.on('download-progress', (progress) => {
    if (mainWindow) {
      mainWindow.setProgressBar(progress.percent / 100);
    }
  });

  autoUpdater.on('update-downloaded', async (info) => {
    if (mainWindow) mainWindow.setProgressBar(-1);
    const result = await dialog.showMessageBox(mainWindow, {
      type: 'info',
      title: '更新已下载',
      message: `Growth Garden ${info.version} 已准备好。`,
      detail: '点击“现在重启”完成更新。',
      buttons: ['稍后', '现在重启'],
      defaultId: 1,
      cancelId: 0,
    });
    if (result.response === 1) {
      quitting = true;
      autoUpdater.quitAndInstall();
    }
  });

  setTimeout(() => {
    autoUpdater.checkForUpdates().catch((error) => {
      log(`update check failed: ${error.message}`);
    });
  }, 5000);
}

async function start() {
  createWindow();

  try {
    startBackend();
    await waitForBackend();
    await mainWindow.loadURL(BACKEND_URL);
    setupUpdater();
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
