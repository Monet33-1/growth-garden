import { ApiError, changePassword, completeLetter, createAdminLetter, createDiary, deleteAdminLetter, deleteDiary, getAdminLetters, getEmotions, getGardenInfo, getLetterDetail, getMyDiaries, getMyLetters, login, loginAdmin, updateAdminLetter, updateAdminLetterSort } from './api.js?v=19';
import { clearSession, getToken, saveSession } from './session.js?v=19';

const MAX_STARS = 30;
const page = document.getElementById('page');
let homeDiaries = [];
let homeDiaryListLoaded = false;

function navigate(route) {
  window.history.pushState({}, '', route);
  renderRoute();
}

function profileMenuMarkup(className, withCaption = false) {
  return `<div class="${className} profile-menu">
    <button class="home-profile__top profile-menu__trigger${withCaption ? ' profile-menu__trigger--captioned' : ''}" type="button" aria-label="打开个人菜单" aria-haspopup="true" aria-expanded="false">
      <span class="home-profile__icons"><span class="home-profile__avatar" aria-hidden="true"><svg viewBox="0 0 24 24"><circle cx="12" cy="8" r="3.4"/><path d="M5.4 19c.6-3.4 2.8-5.1 6.6-5.1s6 1.7 6.6 5.1"/></svg></span><span class="home-profile__chevron" aria-hidden="true">⌄</span></span>
      ${withCaption ? '<span class="home-profile__title">用户入口</span><span class="home-profile__subtitle">个人中心 / 设置</span>' : ''}
    </button>
    <div class="profile-menu__dropdown" hidden><button type="button" data-account-management>账号管理</button></div>
  </div>`;
}

function bindProfileMenu(root = page) {
  root.querySelectorAll('.profile-menu').forEach((menu) => {
    const trigger = menu.querySelector('.profile-menu__trigger');
    const dropdown = menu.querySelector('.profile-menu__dropdown');
    trigger?.addEventListener('click', () => {
      const willOpen = dropdown.hidden;
      dropdown.hidden = !willOpen;
      trigger.setAttribute('aria-expanded', String(willOpen));
    });
    menu.querySelector('[data-account-management]')?.addEventListener('click', () => navigate('/garden/account'));
  });
}

function showLogin(message = '') {
  document.getElementById('app').dataset.view = 'login';
  page.className = 'page';
  page.innerHTML = `
    <header class="brand">
      <p class="brand__english">Growth Garden</p>
      <div class="brand__title"><span></span><h1>成长花园</h1><span></span></div>
      <p class="brand__tagline">记录那些慢慢发生的故事</p>
    </header>
    <form class="login-form" id="login-form" novalidate>
      <label class="field">
        <span class="field__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><circle cx="12" cy="8" r="3.5"/><path d="M5.5 20c.5-3.4 2.8-5.2 6.5-5.2s6 1.8 6.5 5.2"/></svg></span>
        <span class="visually-hidden">花园主人</span>
        <input id="username" name="username" type="text" placeholder="花园主人" autocomplete="username" required />
      </label>
      <label class="field">
        <span class="field__icon" aria-hidden="true"><svg viewBox="0 0 24 24"><rect x="5.5" y="10" width="13" height="10" rx="1.8"/><path d="M8.5 10V7.3a3.5 3.5 0 0 1 7 0V10M12 14v2.5"/></svg></span>
        <span class="visually-hidden">花园钥匙</span>
        <input id="password" name="password" type="password" placeholder="花园钥匙" autocomplete="current-password" required />
        <button class="password-toggle" id="password-toggle" type="button" aria-label="显示密码" title="显示密码">
          <svg viewBox="0 0 24 24"><path d="M2.8 12s3.2-5.5 9.2-5.5 9.2 5.5 9.2 5.5-3.2 5.5-9.2 5.5S2.8 12 2.8 12Z"/><circle cx="12" cy="12" r="2.5"/></svg>
        </button>
      </label>
      <p class="form-message" id="form-message" role="status" aria-live="polite">${message}</p>
      <button class="submit-button" id="submit-button" type="submit">
        <span class="submit-button__seed" aria-hidden="true">✦</span>
        <span id="submit-label">进入花园</span>
        <span class="submit-button__arrow" aria-hidden="true">→</span>
      </button>
    </form>
    <p class="entry-note">只属于你的地方</p>
  `;

  const form = document.getElementById('login-form');
  const usernameInput = document.getElementById('username');
  const passwordInput = document.getElementById('password');
  const formMessage = document.getElementById('form-message');
  const submitButton = document.getElementById('submit-button');
  const submitLabel = document.getElementById('submit-label');

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const username = usernameInput.value.trim();
    const password = passwordInput.value;

    if (!username || !password.trim()) {
      formMessage.textContent = '请先填写花园主人和花园钥匙。';
      (!username ? usernameInput : passwordInput).focus();
      return;
    }

    formMessage.textContent = '';
    submitButton.disabled = true;
    submitLabel.textContent = '正在为你开门';

    try {
      const result = await login({ username, password });
      saveSession({ token: result.token, nickname: result.nickname });
      navigate('/garden');
    } catch (error) {
      formMessage.textContent = error instanceof TypeError
        ? '花园暂时没有回应，请稍后再来。'
        : '主人名字或花园钥匙不正确，请再试一次。';
      passwordInput.focus();
    } finally {
      submitButton.disabled = false;
      submitLabel.textContent = '进入花园';
    }
  });

  document.getElementById('password-toggle').addEventListener('click', (event) => {
    const button = event.currentTarget;
    const visible = passwordInput.type === 'text';
    passwordInput.type = visible ? 'password' : 'text';
    button.setAttribute('aria-label', visible ? '显示密码' : '隐藏密码');
    button.setAttribute('title', visible ? '显示密码' : '隐藏密码');
  });
}

function toast(message) {
  const notice = document.getElementById('home-toast');
  if (!notice) return;
  notice.textContent = message;
  notice.classList.add('is-visible');
  window.clearTimeout(toast.timeout);
  toast.timeout = window.setTimeout(() => notice.classList.remove('is-visible'), 2800);
}

function dateLabel(value) {
  if (!value) return '一段慢慢发生的时光';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '一段慢慢发生的时光' : new Intl.DateTimeFormat('zh-CN').format(date);
}

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, (character) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
  })[character]);
}

function removeDiaryStar(diaryId) {
  const layer = document.getElementById('diary-stars');
  if (!layer) return;
  layer.querySelectorAll('.diary-star').forEach((star) => {
    if (star.dataset.diaryId === String(diaryId)) star.remove();
  });
}

function updateHomeStoryCount() {
  const count = homeDiaries.length;
  const countLabel = document.getElementById('story-count');
  const jar = document.getElementById('story-jar');
  if (countLabel) countLabel.textContent = `${count} 篇随笔`;
  if (jar) jar.setAttribute('aria-label', `故事瓶，共${count}篇随笔，打开情绪分类`);
}

async function deleteDiaryFromDialog({ dialog, diaryId, title, button, message, onDeleted }) {
  if (!diaryId || !button) return false;
  if (!window.confirm(`确定删除“${title || '这篇随笔'}”吗？删除后，这篇随笔、情绪标签和星空中的星星都会消失，且无法恢复。`)) return false;

  const originalLabel = button.textContent;
  button.disabled = true;
  button.textContent = '正在删除…';
  if (message) message.textContent = '';

  try {
    await deleteDiary(diaryId);
    if (dialog?.open) dialog.close();
    onDeleted?.();
    return true;
  } catch (error) {
    if (error instanceof ApiError && [401, 403].includes(error.status)) {
      clearSession();
      navigate('/user/login');
      return false;
    }
    if (message) message.textContent = error.message || '删除没有成功，请稍后再试。';
    return false;
  } finally {
    button.disabled = false;
    button.textContent = originalLabel;
  }
}

function starPoint(diary, index) {
  const layer = document.getElementById('diary-stars');
  const bounds = layer.getBoundingClientRect();
  let point;

  for (let attempt = 0; attempt < 100; attempt += 1) {
    point = {
      x: 8 + Math.random() * 84,
      y: 16 + Math.random() * 39,
      size: 21 + Math.floor(Math.random() * 13),
    };
    const isClear = [...layer.querySelectorAll('.diary-star')]
      .filter((star) => star.dataset.diaryId !== String(diary.id ?? ''))
      .every((star) => {
        const xDistance = (point.x - Number.parseFloat(star.style.left)) * bounds.width / 100;
        const yDistance = (point.y - Number.parseFloat(star.style.top)) * bounds.height / 100;
        return Math.hypot(xDistance, yDistance) >= 56;
      });
    if (isClear) break;
  }

  return point;
}

function createDiaryStar(diary, index) {
  const layer = document.getElementById('diary-stars');
  const dialog = document.getElementById('diary-dialog');
  const point = starPoint(diary, index);
  const star = document.createElement('button');
  star.type = 'button';
  star.className = `diary-star${index % 5 === 0 ? ' diary-star--bright' : ''}`;
  star.dataset.diaryId = String(diary.id ?? `memory-${index}`);
  star.dataset.createdAt = diary.createTime || '';
  star.style.left = `${point.x}%`;
  star.style.top = `${point.y}%`;
  star.style.setProperty('--star-size', `${point.size}px`);
  star.style.setProperty('--star-delay', `${(index % 9) * 120}ms`);
  star.setAttribute('aria-label', `查看随笔：${diary.title || '无标题'}`);
  star.title = diary.title || '打开这段记忆';
  star.innerHTML = '<span aria-hidden="true">✦</span>';
  star.addEventListener('click', () => {
    dialog.dataset.diaryId = String(diary.id);
    document.getElementById('diary-dialog-title').textContent = diary.title || '无标题';
    document.getElementById('diary-dialog-date').textContent = dateLabel(diary.createTime);
    document.getElementById('diary-dialog-content').textContent = diary.content || '这篇随笔还没有写下内容。';
    const message = document.getElementById('diary-dialog-message');
    if (message) message.textContent = '';
    if (!dialog.open) dialog.showModal();
  });
  return star;
}

function renderDiaryStars(diaries) {
  const layer = document.getElementById('diary-stars');
  layer.replaceChildren();
  for (const [index, diary] of diaries.slice(0, MAX_STARS).entries()) {
    layer.append(createDiaryStar(diary, index));
  }
}

function oldestDisplayedStar(layer) {
  return [...layer.querySelectorAll('.diary-star')]
    .sort((first, second) => {
      const firstDate = Date.parse(first.dataset.createdAt) || 0;
      const secondDate = Date.parse(second.dataset.createdAt) || 0;
      return firstDate - secondDate;
    })[0] || null;
}

function foldPaper(paper) {
  if (!paper) return Promise.resolve();
  paper.classList.remove('is-folding');
  void paper.offsetWidth;
  paper.classList.add('is-folding');
  return new Promise((resolve) => {
    const timeout = window.setTimeout(() => {
      resolve();
    }, 620);
    paper.addEventListener('animationend', () => {
      window.clearTimeout(timeout);
      resolve();
    }, { once: true });
  });
}

function animateStarFromPaper(star, layer, start) {
  const layerBounds = layer.getBoundingClientRect();
  const destination = { x: Number.parseFloat(star.style.left), y: Number.parseFloat(star.style.top) };
  const left = `${destination.x}%`;
  const top = `${destination.y}%`;
  star.classList.add('diary-star--arriving');
  star.style.left = left;
  star.style.top = top;

  if (typeof star.animate !== 'function') {
    star.classList.remove('diary-star--arriving');
    star.classList.add('diary-star--settled');
    return Promise.resolve();
  }

  const animation = star.animate([
    { left: `${start.x}%`, top: `${start.y}%`, opacity: 0, transform: 'translate(-50%, -50%) scale(.16)', filter: 'brightness(1)' },
    { offset: .72, left, top, opacity: 1, transform: 'translate(-50%, -50%) scale(1.18)', filter: 'brightness(1.7) drop-shadow(0 0 15px rgba(255, 220, 148, .95))' },
    { left, top, opacity: .94, transform: 'translate(-50%, -50%) scale(1)', filter: 'brightness(1.1) drop-shadow(0 0 8px rgba(255, 220, 148, .72))' },
  ], { duration: 760, easing: 'cubic-bezier(.2,.68,.24,1)' });

  return animation.finished.catch(() => {}).then(() => {
    animation.cancel();
    star.classList.remove('diary-star--arriving');
    star.classList.add('diary-star--settled');
  });
}

async function addDiaryStar(diary) {
  const layer = document.getElementById('diary-stars');
  const paper = document.getElementById('write-entry');
  if (!layer) return;

  const stars = [...layer.querySelectorAll('.diary-star')];
  const replacing = stars.length >= MAX_STARS;
  const oldest = replacing ? oldestDisplayedStar(layer) : null;
  const paperBounds = paper?.getBoundingClientRect();
  const layerBounds = layer.getBoundingClientRect();
  const sourceX = paperBounds ? paperBounds.left + paperBounds.width / 2 : layerBounds.left + layerBounds.width / 2;
  const sourceY = paperBounds ? paperBounds.top + paperBounds.height / 2 : layerBounds.top + layerBounds.height * .85;
  const start = {
    x: ((sourceX - layerBounds.left) / layerBounds.width) * 100,
    y: ((sourceY - layerBounds.top) / layerBounds.height) * 100,
  };
  const point = starPoint(diary, 0);
  if (paper) paper.disabled = true;
  try {
    const foldAnimation = foldPaper(paper);
    let fadeAnimation = Promise.resolve();

    if (oldest) {
      oldest.style.pointerEvents = 'none';
      if (typeof oldest.animate === 'function') {
        const fade = oldest.animate([
          { opacity: .94, filter: 'brightness(1.05) drop-shadow(0 0 8px rgba(255, 220, 148, .6))', transform: 'translate(-50%, -50%) scale(1)' },
          { opacity: .58, filter: 'brightness(.7)', transform: 'translate(-50%, -50%) scale(.96)', offset: .35 },
          { opacity: 0, filter: 'brightness(.35)', transform: 'translate(-50%, -50%) scale(.58)' },
        ], { duration: 480, easing: 'ease-in' });
        fadeAnimation = fade.finished.catch(() => {}).then(() => oldest.remove());
      } else {
        oldest.classList.add('diary-star--fading');
        fadeAnimation = new Promise((resolve) => window.setTimeout(() => {
          oldest.remove();
          resolve();
        }, 480));
      }
    }

    await Promise.all([foldAnimation, fadeAnimation]);
    const star = createDiaryStar(diary, 0);
    star.classList.add('diary-star--arriving');
    layer.prepend(star);
    while (layer.children.length > MAX_STARS) layer.lastElementChild.remove();
    await animateStarFromPaper(star, layer, start);
  } finally {
    if (paper) {
      paper.classList.remove('is-folding');
      paper.disabled = false;
    }
  }
}

function openDiaryEditor() {
  const editor = document.getElementById('write-editor');
  const form = document.getElementById('write-editor-form');
  const titleInput = document.getElementById('write-editor-title');
  const contentInput = document.getElementById('write-editor-content');
  const message = document.getElementById('write-editor-message');
  const confirmLayer = document.getElementById('write-editor-confirm');

  if (!editor || editor.open) return;
  form.reset();
  message.textContent = '';
  confirmLayer.hidden = true;
  document.getElementById('write-editor-date').textContent = new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric', month: 'long', day: 'numeric', weekday: 'long',
  }).format(new Date());
  editor.showModal();
  window.requestAnimationFrame(() => contentInput.focus({ preventScroll: true }));

  const hasUnsavedChanges = () => Boolean(titleInput.value || contentInput.value);
  const askToReturn = () => {
    if (document.getElementById('write-editor-save').disabled) return;
    if (!hasUnsavedChanges()) {
      editor.close();
      return;
    }
    confirmLayer.hidden = false;
    document.getElementById('write-editor-keep').focus();
  };

  document.getElementById('write-editor-back').onclick = askToReturn;
  editor.oncancel = (event) => {
    event.preventDefault();
    askToReturn();
  };
  document.getElementById('write-editor-keep').onclick = () => {
    confirmLayer.hidden = true;
    contentInput.focus();
  };
  document.getElementById('write-editor-discard').onclick = () => {
    confirmLayer.hidden = true;
    editor.close();
  };

  form.onsubmit = async (event) => {
    event.preventDefault();
    const content = contentInput.value.trim();
    if (!content) {
      message.textContent = '先写下几句话，再把故事收进花园吧。';
      contentInput.focus();
      return;
    }

    const saveButton = document.getElementById('write-editor-save');
    const saveLabel = document.getElementById('write-editor-save-label');
    const submittedTitle = titleInput.value.trim();
    saveButton.disabled = true;
    message.textContent = '';
    saveLabel.textContent = '正在收好';

    try {
      const newDiary = await createDiary({ title: submittedTitle, content });
      try {
        homeDiaries = await getMyDiaries();
        homeDiaryListLoaded = true;
      } catch {
        homeDiaries = [newDiary, ...homeDiaries];
      }
      document.getElementById('story-count').textContent = homeDiaryListLoaded
        ? `${homeDiaries.length} 篇随笔`
        : '— 篇随笔';
      document.getElementById('story-jar').setAttribute('aria-label', homeDiaryListLoaded
        ? `故事瓶，共${homeDiaries.length}篇随笔，打开情绪分类`
        : '故事瓶，随笔已保存，数量暂时无法读取');
      editor.close();
      await addDiaryStar(newDiary);
      toast(newDiary.title
        ? '故事已经收进花园，夜空多了一颗星。'
        : '故事已经收好，标题暂时留白。');
    } catch (error) {
      if (error instanceof ApiError && [401, 403].includes(error.status)) {
        clearSession();
        navigate('/user/login');
        return;
      }
      message.textContent = error instanceof TypeError
        ? '花园暂时没有回应，刚才写下的内容还留在这里。'
        : error.message || '这段故事暂时没有保存下来，请稍后再试。';
    } finally {
      saveButton.disabled = false;
      saveLabel.textContent = '收进花园';
    }
  };
}

async function showHome() {
  const app = document.getElementById('app');
  app.dataset.view = 'home';
  page.className = 'page page--home';
  page.innerHTML = `
    <div class="scene__sprout" aria-hidden="true">
      <svg viewBox="0 0 100 145" fill="none">
        <path d="M49 136c.3-31 1.7-56 7.9-83" stroke="#A4B783" stroke-width="3" stroke-linecap="round" />
        <path d="M55 76C36 74 23 63 21 46c20-1 35 9 34 30Z" fill="#76935E" />
        <path d="M55 61c3-19 15-31 33-32 1 20-10 34-33 32Z" fill="#A9BF7D" />
        <path d="M24 137c16-7 37-8 54 0" stroke="#9C8965" stroke-width="2" stroke-linecap="round" />
      </svg>
    </div>
    <header class="home-brand" aria-label="Growth Garden 成长花园">
      <svg viewBox="0 0 40 40" aria-hidden="true"><path d="M18 34c0-9 1-16 3-22"/><path d="M20 20C11 19 7 13 8 7c8 0 13 5 12 13Z"/><path d="M21 17c1-8 6-12 13-12 0 7-4 13-13 12Z"/></svg>
      <span>Growth Garden</span>
    </header>
    ${profileMenuMarkup('home-profile', true)}

    <div id="diary-stars" class="diary-stars" aria-label="花园星空中的随笔"></div>

    <button class="home-paper" id="write-entry" type="button" aria-label="开始写一篇随笔">
      <img class="home-paper__image" src="/images/write-entry-envelope.png" alt="" draggable="false" />
      <span class="home-paper__label">写下新的故事</span>
    </button>

    <button class="home-jar" id="story-jar" type="button" aria-label="打开故事瓶中的情绪分类">
      <span class="home-jar__halo" aria-hidden="true"></span>
      <img class="home-jar__art" src="/images/story-jar.png" alt="" draggable="false" />
      <span class="home-jar__title" aria-hidden="true">我的故事</span>
      <span class="home-jar__count" id="story-count">正在数故事…</span>
    </button>

    <p class="home-motto">在这个小小的花园里<br />种下你的故事，让时间帮你记住。</p>
    <p id="home-toast" class="home-toast" role="status" aria-live="polite"></p>

    <dialog id="diary-dialog" class="diary-dialog">
      <button class="diary-dialog__close" id="diary-dialog-close" type="button" aria-label="关闭">×</button>
      <p class="diary-dialog__eyebrow">A MEMORY IN YOUR SKY</p>
      <h2 id="diary-dialog-title"></h2>
      <time id="diary-dialog-date"></time>
      <p id="diary-dialog-content" class="diary-dialog__content"></p>
      <p class="diary-dialog__message" id="diary-dialog-message" role="status" aria-live="polite"></p>
      <div class="diary-dialog__actions">
        <button class="diary-dialog__delete" id="diary-dialog-delete" type="button">删除这篇随笔</button>
      </div>
    </dialog>

    <dialog id="write-editor" class="write-editor" aria-labelledby="write-editor-heading">
      <section class="write-editor__surface">
        <header class="write-editor__header">
          <button class="write-editor__back" id="write-editor-back" type="button" aria-label="返回花园" title="返回花园">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M19 12H5m7 7-7-7 7-7" /></svg>
          </button>
          <div>
            <p class="write-editor__eyebrow" id="write-editor-heading">A NOTE TO YOURSELF</p>
            <time id="write-editor-date"></time>
          </div>
        </header>
        <form class="write-editor__form" id="write-editor-form" novalidate>
          <label class="write-editor__title-wrap">
            <span class="visually-hidden">标题（选填）</span>
            <input class="write-editor__title" id="write-editor-title" name="title" type="text" maxlength="80" placeholder="给这段故事起个名字" autocomplete="off" />
          </label>
          <p class="write-editor__ai-note" id="write-editor-ai-note">标题可以留空，保存时会尝试根据正文生成。</p>
          <label class="write-editor__body-wrap">
            <span class="visually-hidden">随笔正文</span>
            <textarea class="write-editor__body" id="write-editor-content" name="content" placeholder="把此刻的想法，慢慢写下来……" required></textarea>
          </label>
          <p class="write-editor__message" id="write-editor-message" role="status" aria-live="polite"></p>
          <footer class="write-editor__footer">
            <span class="write-editor__save-note">写下来的故事，只属于你</span>
            <button class="write-editor__save" id="write-editor-save" type="submit">
              <span id="write-editor-save-label">收进花园</span><span aria-hidden="true">→</span>
            </button>
          </footer>
        </form>
        <div class="write-editor__confirm" id="write-editor-confirm" role="alertdialog" aria-modal="true" aria-labelledby="write-editor-confirm-title" hidden>
          <div class="write-editor__confirm-card">
            <p class="write-editor__eyebrow">A MOMENT NOT YET SAVED</p>
            <h2 id="write-editor-confirm-title">这段故事还没有收好</h2>
            <p>现在返回，刚才写下的内容会消失。</p>
            <div class="write-editor__confirm-actions">
              <button class="write-editor__keep" id="write-editor-keep" type="button">继续书写</button>
              <button class="write-editor__discard" id="write-editor-discard" type="button">放弃并返回</button>
            </div>
          </div>
        </div>
      </section>
    </dialog>
  `;

  const dialog = document.getElementById('diary-dialog');
  const diaryDeleteButton = document.getElementById('diary-dialog-delete');
  document.getElementById('diary-dialog-close').addEventListener('click', () => dialog.close());
  dialog.addEventListener('click', (event) => {
    if (event.target === dialog) dialog.close();
  });
  diaryDeleteButton.addEventListener('click', () => {
    const diaryId = dialog.dataset.diaryId;
    if (!diaryId) return;
    const diary = homeDiaries.find((item) => String(item.id) === diaryId);
    void deleteDiaryFromDialog({
      dialog,
      diaryId,
      title: diary?.title,
      button: diaryDeleteButton,
      message: document.getElementById('diary-dialog-message'),
      onDeleted: () => {
        removeDiaryStar(diaryId);
        homeDiaries = homeDiaries.filter((item) => String(item.id) !== diaryId);
        if (homeDiaryListLoaded) updateHomeStoryCount();
        toast('这篇随笔已经离开花园。');
      },
    });
  });
  document.getElementById('story-jar').addEventListener('click', () => navigate('/garden/emotions'));
  bindProfileMenu();
  document.getElementById('write-entry').addEventListener('click', () => {
    openDiaryEditor();
  });
  try {
    const [gardenResult, diaryResult, letterResult] = await Promise.allSettled([
      getGardenInfo(),
      getMyDiaries(),
      getMyLetters(),
    ]);
    const garden = gardenResult.status === 'fulfilled' ? gardenResult.value : null;
    const diaries = diaryResult.status === 'fulfilled' ? diaryResult.value : [];
    homeDiaryListLoaded = diaryResult.status === 'fulfilled';
    homeDiaries = diaries;
    const letters = letterResult.status === 'fulfilled' ? letterResult.value : [];

    if ([gardenResult, diaryResult, letterResult].some((result) => result.status === 'rejected'
      && result.reason instanceof ApiError && [401, 403].includes(result.reason.status))) {
      clearSession();
      navigate('/user/login');
      return;
    }

    const storyCount = diaryResult.status === 'fulfilled' ? diaries.length : null;
    document.getElementById('story-count').textContent = storyCount === null ? '— 篇随笔' : `${storyCount} 篇随笔`;
    document.getElementById('story-jar').setAttribute('aria-label', storyCount === null
      ? '故事瓶，暂时无法读取随笔数量'
      : `故事瓶，共${storyCount}篇随笔，打开情绪分类`);
    renderDiaryStars(diaries);
      const diaryToOpen = new URLSearchParams(window.location.search).get('diaryId');
    if (diaryToOpen) {
      const savedDiary = diaries.find((diary) => String(diary.id) === diaryToOpen);
      if (savedDiary) {
        document.getElementById('diary-dialog').dataset.diaryId = String(savedDiary.id);
        document.getElementById('diary-dialog-title').textContent = savedDiary.title || '无标题';
        document.getElementById('diary-dialog-date').textContent = dateLabel(savedDiary.createTime);
        document.getElementById('diary-dialog-content').textContent = savedDiary.content || '这篇随笔还没有写下内容。';
        const message = document.getElementById('diary-dialog-message');
        if (message) message.textContent = '';
        document.getElementById('diary-dialog').showModal();
        window.history.replaceState({}, '', '/garden');
      }
    }

    const level = Math.max(1, Number(garden?.level) || 1);
    app.style.setProperty('--plant-scale', String(1 + Math.min(level - 1, 5) * .055));
    app.dataset.stage = garden?.stage || 'SEED';
    const unreadLetters = letters.filter((letter) => letter.status === 'UNREAD');
    if (letterResult.status === 'fulfilled' && unreadLetters.length > 0) {
      const entry = document.createElement('button');
      entry.className = 'home-letter';
      entry.id = 'home-letter';
      entry.type = 'button';
      entry.setAttribute('aria-label', `有 ${unreadLetters.length} 封已抵达的信件`);
      entry.innerHTML = '<img class="home-letter__image" src="/images/hidden-letter-envelope.png" alt="" draggable="false" /><span class="home-letter__glow" aria-hidden="true"></span><span class="home-letter__count">' + unreadLetters.length + '</span>';
      entry.addEventListener('click', () => navigate('/garden/letters'));
      page.append(entry);
    }
    if ([gardenResult, diaryResult, letterResult].some((result) => result.status === 'rejected')) {
      toast('有些花园记忆暂时没有加载出来，请稍后再试。');
    }
  } catch {
    toast('花园正在整理星光，请稍后再看。');
  }
}

const emotionBottleAssets = {
  开心: { image: '/images/emotion-gold-happy.png', layout: 'happy' },
  平静: { image: '/images/emotion-green-calm.png', layout: 'calm' },
  孤独: { image: '/images/emotion-violet-solitude.png', layout: 'lonely' },
  难过: { image: '/images/emotion-blue-sad.png', layout: 'sad' },
  感动: { image: '/images/emotion-pink-touched.png', layout: 'touched' },
};

const emotionDescriptions = {
  开心: '那些让人嘴角上扬的瞬间，都被我小心地收藏在这个瓶子里。',
  平静: '那些让心慢慢安静下来的时刻，也值得被温柔记住。',
  孤独: '那些独自走过的夜晚，也有星光替你轻轻收好。',
  难过: '那些没能说出口的难过，也值得被认真安放。',
  感动: '那些心里微微发热的瞬间，都在这里留下了光。',
};

function renderEmotionBottles(emotions, diaries) {
  const container = document.getElementById('emotion-bottles');
  const counts = new Map(emotions.map((emotion) => [String(emotion.id), 0]));

  for (const diary of diaries) {
    const counted = new Set();
    for (const emotion of diary.emotions || []) {
      const id = String(emotion.id);
      if (counts.has(id) && !counted.has(id)) {
        counts.set(id, counts.get(id) + 1);
        counted.add(id);
      }
    }
  }

  if (emotions.length === 0) {
    container.innerHTML = '<p class="emotion-page__empty">花园里还没有情绪瓶。</p>';
    return;
  }

  container.innerHTML = emotions.map((emotion) => {
    const name = String(emotion.name || '未命名');
    const asset = emotionBottleAssets[name] || { image: '/images/emotion-gold-happy.png', layout: 'other' };
    const count = counts.get(String(emotion.id)) || 0;
    return `
      <button class="emotion-bottle" data-layout="${asset.layout}" data-emotion-id="${escapeHtml(emotion.id)}" type="button" aria-label="打开${escapeHtml(name)}瓶，收藏 ${count} 个瞬间">
        <img class="emotion-bottle__image" src="${asset.image}" alt="" draggable="false" />
        <h2 class="emotion-bottle__name">${escapeHtml(name)}</h2>
        <p class="emotion-bottle__count">收藏 ${count} 个瞬间</p>
      </button>
    `;
  }).join('');
}

async function showEmotionBottles() {
  const app = document.getElementById('app');
  app.dataset.view = 'emotions';
  page.className = 'page page--emotions';
  page.innerHTML = `
    <header class="emotion-page__header">
      <button class="emotion-page__back" id="emotion-page-back" type="button" aria-label="返回花园" title="返回花园">
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M19 12H5m7 7-7-7 7-7" /></svg>
      </button>
      <span class="emotion-page__brand">Growth Garden</span>
      ${profileMenuMarkup('emotion-page__profile')}
    </header>
    <section class="emotion-bottles" id="emotion-bottles" aria-label="情绪故事瓶">
      <p class="emotion-page__loading">正在整理花园里的故事……</p>
    </section>
  `;
  document.getElementById('emotion-page-back').addEventListener('click', () => navigate('/garden'));
  bindProfileMenu();

  try {
    const [emotions, diaries] = await Promise.all([getEmotions(), getMyDiaries()]);
    if (window.location.pathname !== '/garden/emotions') return;
    renderEmotionBottles(emotions, diaries);
    document.querySelectorAll('.emotion-bottle').forEach((bottle) => {
      bottle.addEventListener('click', () => navigate('/garden/emotions?emotionId=' + encodeURIComponent(bottle.dataset.emotionId)));
    });
  } catch (error) {
    if (error instanceof ApiError && [401, 403].includes(error.status)) {
      clearSession();
      navigate('/user/login');
      return;
    }
    const container = document.getElementById('emotion-bottles');
    if (container && window.location.pathname === '/garden/emotions') {
      container.innerHTML = `
        <div class="emotion-page__error" role="status">
          <p>情绪瓶暂时没有显现，请稍后再试。</p>
          <button id="emotion-page-retry" type="button">再试一次</button>
        </div>
      `;
      document.getElementById('emotion-page-retry').addEventListener('click', showEmotionBottles);
    }
  }
}

function renderEmotionDiaryCards(diaries) {
  if (!diaries.length) {
    return '<p class="emotion-detail__empty">这个瓶子还很安静，新的故事会在某一天来到这里。</p>';
  }

  return diaries.map((diary) => {
    const title = String(diary.title || '').trim() || '一段无题时光';
    const content = diaryExcerptText(diary.content);
    const preview = content || '这段记忆还没有写下更多内容。';
    return '<button class="emotion-detail__diary" type="button" data-diary-id="' + escapeHtml(diary.id) + '">' +
      '<span class="emotion-detail__diary-main">' +
        '<span class="emotion-detail__diary-title">' + escapeHtml(title) + '</span>' +
        '<span class="emotion-detail__diary-preview">' + escapeHtml(preview) + '</span>' +
      '</span>' +
      '<span class="emotion-detail__diary-meta">' +
        '<time>' + escapeHtml(dateLabel(diary.createTime)) + '</time>' +
        '<span class="emotion-detail__diary-star" aria-hidden="true">★</span>' +
      '</span>' +
    '</button>';
  }).join('');
}

function diaryExcerptText(value) {
  const source = String(value || '');
  const parsed = new DOMParser().parseFromString(source, 'text/html');
  const text = parsed.body.textContent || source;
  return text
    .replace(/!\[([^\]]*)\]\([^)]*\)/g, '$1')
    .replace(/\[([^\]]+)\]\([^)]*\)/g, '$1')
    .replace(/(^|\n)\s{0,3}#{1,6}\s+/g, '$1')
    .replace(/(^|\n)\s{0,3}>\s?/g, '$1')
    .replace(/(^|\n)\s*[-*+]\s+/g, '$1')
    .replace(/[`*_~]/g, '')
    .replace(/\s+/g, ' ')
    .trim();
}

function showEmotionDiary(dialog, diary) {
  dialog.dataset.diaryId = String(diary.id);
  document.getElementById('emotion-diary-title').textContent = diary.title || '一段无题时光';
  document.getElementById('emotion-diary-date').textContent = dateLabel(diary.createTime);
  document.getElementById('emotion-diary-content').textContent = diary.content || '这篇随笔还没有写下内容。';
  const message = document.getElementById('emotion-diary-message');
  if (message) message.textContent = '';
  if (!dialog.open) dialog.showModal();
}

function renderEmotionDetail(emotion, diaries) {
  const app = document.getElementById('app');
  const asset = emotionBottleAssets[emotion.name] || { image: '/images/emotion-gold-happy.png', layout: 'other' };
  const name = String(emotion.name || '未命名');
  const description = emotionDescriptions[name] || '每一种感受都值得被认真收藏，故事在这里慢慢生长。';
  app.dataset.view = 'emotion-detail';
  page.className = 'page page--emotion-detail';

  page.innerHTML = '<header class="emotion-detail__header">' +
      '<button class="emotion-page__back" id="emotion-detail-back" type="button" aria-label="返回情绪瓶" title="返回情绪瓶">' +
        '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M19 12H5m7 7-7-7 7-7" /></svg>' +
      '</button>' +
      '<span class="emotion-page__brand">Growth Garden</span>' +
      profileMenuMarkup('emotion-page__profile') +
    '</header>' +
    '<section class="emotion-detail__scene" data-layout="' + asset.layout + '">' +
      '<div class="emotion-detail__intro"><span class="emotion-detail__mark" aria-hidden="true">✦</span>' +
        '<div><h1>' + escapeHtml(name) + '瓶</h1><p>收藏 ' + diaries.length + ' 个瞬间</p></div>' +
        '<p class="emotion-detail__description">' + escapeHtml(description) + '</p>' +
      '</div>' +
      '<img class="emotion-detail__bottle" src="' + asset.image + '" alt="' + escapeHtml(name) + '故事瓶" draggable="false" />' +
      '<section class="emotion-detail__stories" aria-label="' + escapeHtml(name) + '瓶中的随笔">' +
        '<h2><span aria-hidden="true">⟶</span> ' + escapeHtml(name) + ' · ' + diaries.length + ' 篇随笔</h2>' +
        '<div class="emotion-detail__list">' + renderEmotionDiaryCards(diaries) + '</div>' +
        '<div class="emotion-detail__ornament" aria-hidden="true"><span></span>✣<span></span></div>' +
      '</section>' +
    '</section>' +
    '<dialog class="diary-dialog emotion-detail__dialog" id="emotion-diary-dialog">' +
      '<button class="diary-dialog__close" id="emotion-diary-close" type="button" aria-label="关闭">×</button>' +
      '<p class="diary-dialog__eyebrow">A MEMORY IN YOUR GARDEN</p>' +
      '<h2 id="emotion-diary-title"></h2><time id="emotion-diary-date"></time>' +
      '<p id="emotion-diary-content" class="diary-dialog__content"></p>' +
      '<p class="diary-dialog__message" id="emotion-diary-message" role="status" aria-live="polite"></p>' +
      '<div class="diary-dialog__actions"><button class="diary-dialog__delete" id="emotion-diary-delete" type="button">删除这篇随笔</button></div>' +
    '</dialog>';

  document.getElementById('emotion-detail-back').addEventListener('click', () => navigate('/garden/emotions'));
  bindProfileMenu();
  const dialog = document.getElementById('emotion-diary-dialog');
  const emotionDeleteButton = document.getElementById('emotion-diary-delete');
  document.getElementById('emotion-diary-close').addEventListener('click', () => dialog.close());
  emotionDeleteButton.addEventListener('click', () => {
    const diaryId = dialog.dataset.diaryId;
    if (!diaryId) return;
    const diary = diaries.find((item) => String(item.id) === diaryId);
    void deleteDiaryFromDialog({
      dialog,
      diaryId,
      title: diary?.title,
      button: emotionDeleteButton,
      message: document.getElementById('emotion-diary-message'),
      onDeleted: () => {
        diaries = diaries.filter((item) => String(item.id) !== diaryId);
        const card = [...page.querySelectorAll('.emotion-detail__diary')]
          .find((item) => item.dataset.diaryId === diaryId);
        card?.remove();
        const remaining = page.querySelectorAll('.emotion-detail__diary').length;
        const heading = page.querySelector('.emotion-detail__stories h2');
        if (heading) {
          heading.innerHTML = '<span aria-hidden="true">⟶</span> ' + escapeHtml(name) + ' · ' + remaining + ' 篇随笔';
        }
        const introCount = page.querySelector('.emotion-detail__intro > div > p');
        if (introCount) introCount.textContent = '收藏 ' + remaining + ' 个瞬间';
        if (remaining === 0) {
          const list = page.querySelector('.emotion-detail__list');
          if (list) list.innerHTML = '<p class="emotion-detail__empty">这个瓶子还很安静，新的故事会在某一天来到这里。</p>';
        }
      },
    });
  });
  page.querySelectorAll('.emotion-detail__diary').forEach((card) => {
    card.addEventListener('click', () => {
      const diary = diaries.find((item) => String(item.id) === card.dataset.diaryId);
      if (diary) showEmotionDiary(dialog, diary);
    });
  });
}

async function showEmotionDetail(emotionId) {
  const app = document.getElementById('app');
  app.dataset.view = 'emotion-detail';
  page.className = 'page page--emotion-detail';
  page.innerHTML = '<p class="emotion-detail__loading">正在拾起这个瓶子里的故事……</p>';

  try {
    const [emotions, allDiaries] = await Promise.all([getEmotions(), getMyDiaries()]);
    if (window.location.pathname !== '/garden/emotions' || new URLSearchParams(window.location.search).get('emotionId') !== String(emotionId)) return;
    const emotion = emotions.find((item) => String(item.id) === String(emotionId));
    if (!emotion) {
      page.innerHTML = '<div class="emotion-detail__not-found"><p>这只故事瓶暂时没有找到。</p><button id="emotion-detail-back" type="button">返回情绪瓶</button></div>';
      document.getElementById('emotion-detail-back').addEventListener('click', () => navigate('/garden/emotions'));
      return;
    }

    const diaries = allDiaries.filter((diary) => (diary.emotions || []).some((item) => String(item.id) === String(emotion.id)));
    renderEmotionDetail(emotion, diaries);
  } catch (error) {
    if (error instanceof ApiError && [401, 403].includes(error.status)) {
      clearSession();
      navigate('/user/login');
      return;
    }
    if (window.location.pathname !== '/garden/emotions' || new URLSearchParams(window.location.search).get('emotionId') !== String(emotionId)) return;
    page.innerHTML = '<div class="emotion-detail__not-found" role="status">' +
      '<p>故事暂时没有显现，请稍后再试。</p>' +
      '<button id="emotion-detail-retry" type="button">再试一次</button>' +
      '<button id="emotion-detail-back" type="button">返回情绪瓶</button>' +
    '</div>';
    document.getElementById('emotion-detail-retry').addEventListener('click', () => showEmotionDetail(emotionId));
    document.getElementById('emotion-detail-back').addEventListener('click', () => navigate('/garden/emotions'));
  }
}

function letterHeader(backLabel, backRoute) {
  return '<header class="letter-page__header">' +
    '<button class="letter-page__back" type="button" aria-label="' + escapeHtml(backLabel) + '" title="' + escapeHtml(backLabel) + '" data-back-route="' + escapeHtml(backRoute) + '">←</button>' +
    '<span class="letter-page__brand">Growth Garden</span>' +
    profileMenuMarkup('letter-page__profile') +
    '<a class="letter-page__home" href="/garden" data-home-link>返回花园</a>' +
    '</header>';
}

function showUserLetters() {
  const app = document.getElementById('app');
  app.dataset.view = 'letters';
  page.className = 'page page--letters';
  page.innerHTML = letterHeader('返回花园', '/garden') +
    '<main class="letter-page__panel"><p class="letter-page__eyebrow">A LETTER HAS ARRIVED</p>' +
    '<h1>花园来信</h1><p class="letter-page__lead">在合适的时刻抵达的信，正在这里等你。</p>' +
    '<p class="letter-page__status" id="user-letters-status" role="status">正在确认有没有新的来信……</p>' +
    '<section class="letter-page__list" id="user-letters-list" aria-live="polite"></section></main>';
  page.querySelector('[data-back-route]').addEventListener('click', () => navigate('/garden'));
  page.querySelector('[data-home-link]').addEventListener('click', (event) => { event.preventDefault(); navigate('/garden'); });
  bindProfileMenu();

  getMyLetters().then((letters) => {
    if (window.location.pathname !== '/garden/letters') return;
    const unread = letters.filter((letter) => letter.status === 'UNREAD');
    const list = document.getElementById('user-letters-list');
    const status = document.getElementById('user-letters-status');
    if (!unread.length) {
      status.textContent = '暂时没有尚未阅读的来信。新的信会在花园成长后抵达。';
      return;
    }
    status.textContent = `有 ${unread.length} 封信已经抵达。`;
    list.innerHTML = unread.map((letter) => '<button class="letter-page__card" type="button" data-letter-id="' + escapeHtml(letter.id) + '">' +
      (letter.cover ? '<img src="' + escapeHtml(letter.cover) + '" alt="" />' : '<span class="letter-page__seal" aria-hidden="true">✉</span>') +
      '<span><strong>' + escapeHtml(letter.title || '一封花园来信') + '</strong><small>打开这封信</small></span><span class="letter-page__arrow" aria-hidden="true">→</span></button>').join('');
    list.querySelectorAll('[data-letter-id]').forEach((button) => button.addEventListener('click', () => navigate('/garden/letters/' + encodeURIComponent(button.dataset.letterId))));
  }).catch((error) => {
    if (error instanceof ApiError && [401, 403].includes(error.status)) {
      clearSession();
      navigate('/user/login');
      return;
    }
    const status = document.getElementById('user-letters-status');
    if (!status || window.location.pathname !== '/garden/letters') return;
    status.innerHTML = '来信暂时没有显现。<button class="letter-page__retry" id="user-letters-retry" type="button">再试一次</button>';
    document.getElementById('user-letters-retry').addEventListener('click', showUserLetters);
  });
}

async function showUserLetterDetail(letterId) {
  const app = document.getElementById('app');
  app.dataset.view = 'letter-detail';
  page.className = 'page page--letter-detail';
  page.innerHTML = letterHeader('返回来信', '/garden/letters') +
    '<main class="letter-reading" id="letter-reading"><p class="letter-page__status">正在轻轻展开这封信……</p></main>';
  page.querySelector('[data-back-route]').addEventListener('click', () => navigate('/garden/letters'));
  page.querySelector('[data-home-link]').addEventListener('click', (event) => { event.preventDefault(); navigate('/garden'); });
  bindProfileMenu();

  try {
    const letter = await getLetterDetail(letterId);
    if (window.location.pathname !== '/garden/letters/' + letterId) return;
    const cover = letter.cover ? '<img class="letter-reading__cover" src="' + escapeHtml(letter.cover) + '" alt="" />' : '';
    const content = letter.contentHtml || '<p>' + escapeHtml(letter.content || '').replace(/\n/g, '<br>') + '</p>';
    const done = letter.status === 'SAVED';
    page.querySelector('#letter-reading').innerHTML = cover +
      '<p class="letter-page__eyebrow">A LETTER FOR THIS MOMENT</p><h1>' + escapeHtml(letter.title || '花园来信') + '</h1>' +
      '<article class="letter-reading__content">' + content + '</article>' +
      '<p class="letter-reading__feedback" id="letter-reading-feedback" role="status" aria-live="polite">' + (done ? '这封信已经收进你的成长花园。' : '') + '</p>' +
      (done
        ? '<a class="letter-page__primary" href="/garden?diaryId=' + encodeURIComponent(letter.diaryId) + '" data-view-diary>查看这篇随笔 →</a>'
        : '<button class="letter-page__primary" id="letter-complete" type="button">读完了，收进我的回忆</button>');

    if (!done) {
      const button = document.getElementById('letter-complete');
      button.addEventListener('click', async () => {
        button.disabled = true;
        button.textContent = '正在收进花园……';
        try {
          const saved = await completeLetter(letterId);
          const feedback = document.getElementById('letter-reading-feedback');
          feedback.textContent = '这封信已经收进你的成长花园。';
          button.outerHTML = '<a class="letter-page__primary" href="/garden?diaryId=' + encodeURIComponent(saved.diaryId) + '" data-view-diary>查看这篇随笔 →</a>';
        } catch (error) {
          if (error instanceof ApiError && [401, 403].includes(error.status)) {
            clearSession();
            navigate('/user/login');
            return;
          }
          document.getElementById('letter-reading-feedback').textContent = error.message || '暂时没有收好这封信，请再试一次。';
          button.disabled = false;
          button.textContent = '再试一次，收进我的回忆';
        }
      });
    }
    page.querySelector('[data-view-diary]')?.addEventListener('click', (event) => {
      event.preventDefault();
      navigate(event.currentTarget.getAttribute('href'));
    });
  } catch (error) {
    if (error instanceof ApiError && [401, 403].includes(error.status)) {
      clearSession();
      navigate('/user/login');
      return;
    }
    if (window.location.pathname !== '/garden/letters/' + letterId) return;
    page.querySelector('#letter-reading').innerHTML = '<div class="letter-reading__error"><p>' + escapeHtml(error.message || '这封信暂时无法打开。') + '</p><button class="letter-page__retry" id="letter-detail-retry" type="button">再试一次</button></div>';
    document.getElementById('letter-detail-retry').addEventListener('click', () => showUserLetterDetail(letterId));
  }
}

function adminDateLabel(value) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? '—'
    : new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(date);
}

function markdownInlinePreview(value) {
  return escapeHtml(value)
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\*([^*]+)\*/g, '<em>$1</em>');
}

function renderMarkdownPreview(markdown) {
  const blocks = String(markdown || '').trim().split(/\n\s*\n/).filter(Boolean);
  if (!blocks.length) return '<p class="letter-admin__preview-empty">正文预览会显示在这里。</p>';

  return blocks.map((block) => {
    const trimmed = block.trim();
    const codeBlock = trimmed.match(/^```(?:[\w-]+)?\s*\n([\s\S]*?)\n```$/);
    if (codeBlock) return '<pre><code>' + escapeHtml(codeBlock[1]) + '</code></pre>';
    const heading = trimmed.match(/^(#{1,3})\s+(.+)$/);
    if (heading) return '<h' + heading[1].length + '>' + markdownInlinePreview(heading[2]) + '</h' + heading[1].length + '>';
    const lines = trimmed.split('\n');
    if (lines.every((line) => /^\s*[-*+]\s+/.test(line))) {
      return '<ul>' + lines.map((line) => '<li>' + markdownInlinePreview(line.replace(/^\s*[-*+]\s+/, '')) + '</li>').join('') + '</ul>';
    }
    if (lines.every((line) => /^\s*\d+[.)]\s+/.test(line))) {
      return '<ol>' + lines.map((line) => '<li>' + markdownInlinePreview(line.replace(/^\s*\d+[.)]\s+/, '')) + '</li>').join('') + '</ol>';
    }
    if (lines.every((line) => /^\s*>\s?/.test(line))) {
      return '<blockquote>' + lines.map((line) => markdownInlinePreview(line.replace(/^\s*>\s?/, ''))).join('<br>') + '</blockquote>';
    }
    return '<p>' + lines.map(markdownInlinePreview).join('<br>') + '</p>';
  }).join('');
}

function renderAdminLetterCards(letters) {
  if (!letters.length) {
    return '<div class="letter-admin__empty"><span aria-hidden="true">✉</span><p>还没有预置信件</p><small>可以从“新增信件”开始准备花园里的来信。</small></div>';
  }

  return letters.map((letter) => {
    const unlockLabel = letter.unlockType === 'DIARY_COUNT'
      ? '累计 ' + Number(letter.unlockValue || 0) + ' 篇随笔后解锁'
      : String(letter.unlockType || '未知规则') + ' · ' + String(letter.unlockValue ?? '—');
    return '<article class="letter-admin__card" data-letter-id="' + escapeHtml(letter.id) + '">' +
      '<div class="letter-admin__card-copy">' +
        '<h2>' + escapeHtml(letter.title || '无标题信件') + '</h2>' +
        '<p>' + escapeHtml(unlockLabel) + '</p>' +
        '<small>封面：' + escapeHtml(letter.cover || '尚未设置') + '</small>' +
      '</div>' +
      '<div class="letter-admin__card-controls">' +
        '<label>排序值<input type="number" min="0" step="1" required data-letter-sort="' + escapeHtml(letter.id) + '" value="' + escapeHtml(letter.sort ?? 0) + '" /></label>' +
        '<small>更新于 ' + escapeHtml(adminDateLabel(letter.updateTime || letter.createTime)) + '</small>' +
        '<div class="letter-admin__card-actions">' +
          '<button type="button" data-letter-edit="' + escapeHtml(letter.id) + '">编辑</button>' +
          '<button type="button" class="is-danger" data-letter-delete="' + escapeHtml(letter.id) + '">删除</button>' +
        '</div>' +
      '</div>' +
    '</article>';
  }).join('');
}

async function showAdminLetters() {
  const app = document.getElementById('app');
  app.dataset.view = 'letter-admin';
  page.className = 'page page--letter-admin';
  page.innerHTML = `
    <header class="letter-admin__header">
      <button class="letter-admin__back" id="letter-admin-back" type="button" aria-label="返回花园" title="返回花园">←</button>
      <span class="letter-admin__brand">Growth Garden</span>
    </header>
    <main class="letter-admin__workspace">
      <div class="letter-admin__heading">
        <div><p class="letter-admin__eyebrow">LETTERS FOR THE GARDEN</p><h1>预置信件</h1><p class="letter-admin__intro">整理那些会在合适时刻抵达的信。</p></div>
        <button class="letter-admin__primary" id="letter-admin-create" type="button" hidden>＋ 新增信件</button>
      </div>
      <form class="letter-admin__gate" id="letter-admin-gate">
        <p class="letter-admin__eyebrow">ADMIN ACCESS</p>
        <h2>管理员验证</h2>
        <p>输入管理员账号和密码，继续整理花园里的预置信件。</p>
        <label>管理员账号<input name="username" autocomplete="username" required /></label>
        <label>管理员密码<input name="password" type="password" autocomplete="current-password" required /></label>
        <p class="letter-admin__gate-message" id="letter-admin-gate-message" role="status" aria-live="polite"></p>
        <button class="letter-admin__primary" id="letter-admin-gate-submit" type="submit">验证并进入</button>
      </form>
      <p class="letter-admin__message" id="letter-admin-message" role="status" aria-live="polite" hidden></p>
      <section class="letter-admin__list" id="letter-admin-list" aria-live="polite" hidden></section>
      <div class="letter-admin__sort-actions" id="letter-admin-sort-actions" hidden><span>调整每封信的排序值后保存。</span><button id="letter-admin-save-sort" type="button">保存排序</button></div>
    </main>
    <dialog class="letter-admin__dialog" id="letter-admin-dialog" aria-labelledby="letter-admin-dialog-title">
      <form id="letter-admin-form" novalidate>
        <header class="letter-admin__dialog-header"><div><p class="letter-admin__eyebrow">A PRESET LETTER</p><h2 id="letter-admin-dialog-title">新增信件</h2></div><button class="letter-admin__close" id="letter-admin-close" type="button" aria-label="关闭">×</button></header>
        <div class="letter-admin__fields">
          <label>信件标题<input name="title" type="text" maxlength="200" required placeholder="例如：写给未来的你" /></label>
          <div class="letter-admin__field-row">
            <label>解锁类型<select name="unlockType"><option value="DIARY_COUNT">累计随笔数量</option></select></label>
            <label>解锁所需随笔数<input name="unlockValue" type="number" min="0" step="1" required value="1" /></label>
            <label>排序值<input name="sort" type="number" min="0" step="1" required value="0" /></label>
          </div>
          <label>封面图片路径<input name="cover" type="text" maxlength="1000" placeholder="/images/letter-cover.jpg" /></label>
          <label>Markdown 正文<textarea name="content" rows="9" required placeholder="写下这封信的内容……"></textarea></label>
          <div class="letter-admin__preview-wrap"><div class="letter-admin__preview-label">正文预览</div><div class="letter-admin__preview" id="letter-admin-preview"></div></div>
        </div>
        <p class="letter-admin__dialog-message" id="letter-admin-dialog-message" role="status" aria-live="polite"></p>
        <footer class="letter-admin__dialog-actions"><button class="letter-admin__secondary" id="letter-admin-cancel" type="button">取消</button><button class="letter-admin__primary" id="letter-admin-submit" type="submit">保存信件</button></footer>
      </form>
    </dialog>
  `;

  const list = document.getElementById('letter-admin-list');
  const status = document.getElementById('letter-admin-message');
  const sortActions = document.getElementById('letter-admin-sort-actions');
  const gate = document.getElementById('letter-admin-gate');
  const gateMessage = document.getElementById('letter-admin-gate-message');
  const gateForm = gate;
  const createButton = document.getElementById('letter-admin-create');
  const dialog = document.getElementById('letter-admin-dialog');
  const form = document.getElementById('letter-admin-form');
  const contentInput = form.elements.content;
  const preview = document.getElementById('letter-admin-preview');
  let letters = [];
  let editingId = null;
  let adminToken = sessionStorage.getItem('growthGardenAdminToken');

  document.getElementById('letter-admin-back').addEventListener('click', () => navigate('/garden'));

  const refreshLetters = async (successMessage = '') => {
    if (!adminToken) return;
    gate.hidden = true;
    list.hidden = false;
    document.getElementById('letter-admin-message').hidden = false;
    createButton.hidden = false;
    list.innerHTML = '<p class="letter-admin__loading">正在取回信件……</p>';
    status.textContent = '';
    try {
      letters = await getAdminLetters(adminToken);
      if (window.location.pathname !== '/admin/letters') return;
      list.innerHTML = renderAdminLetterCards(letters);
      sortActions.hidden = letters.length === 0;
      status.textContent = successMessage;

      list.querySelectorAll('[data-letter-edit]').forEach((button) => {
        button.addEventListener('click', () => openEditor(letters.find((letter) => String(letter.id) === button.dataset.letterEdit)));
      });
      list.querySelectorAll('[data-letter-delete]').forEach((button) => {
        button.addEventListener('click', async () => {
          const letter = letters.find((item) => String(item.id) === button.dataset.letterDelete);
          if (!letter || !window.confirm('确定删除“' + letter.title + '”吗？删除后，用户端将不再展示这封信。')) return;
          button.disabled = true;
          try {
            await deleteAdminLetter(letter.id, adminToken);
            await refreshLetters('信件已删除。');
          } catch (error) {
            button.disabled = false;
            status.textContent = error.message || '删除没有成功，请稍后重试。';
          }
        });
      });
    } catch (error) {
      if (error instanceof ApiError && [401, 403].includes(error.status)) {
        adminToken = null;
        sessionStorage.removeItem('growthGardenAdminToken');
        gate.hidden = false;
        list.hidden = true;
        document.getElementById('letter-admin-message').hidden = true;
        createButton.hidden = true;
        sortActions.hidden = true;
        gateMessage.textContent = '管理员验证已过期，请重新登录。';
        gateForm.elements.password.value = '';
        gateForm.elements.password.focus();
        return;
      }
      sortActions.hidden = true;
      list.innerHTML = '<div class="letter-admin__access" role="alert"><h2>' +
        escapeHtml(error.status === 403 ? '这里暂时只对花园管理员开放' : '信件暂时没有显现') +
        '</h2><p>' + escapeHtml(error.message || '请检查连接后重试。') + '</p>' +
        '<button id="letter-admin-retry" type="button">再试一次</button></div>';
      document.getElementById('letter-admin-retry').addEventListener('click', () => refreshLetters());
    }
  };

  function openEditor(letter = null) {
    editingId = letter?.id ?? null;
    form.reset();
    form.elements.title.value = letter?.title || '';
    form.elements.unlockType.value = letter?.unlockType || 'DIARY_COUNT';
    form.elements.unlockValue.value = letter?.unlockValue ?? 1;
    form.elements.sort.value = letter?.sort ?? (letters.length ? Math.max(...letters.map((item) => Number(item.sort) || 0)) + 1 : 0);
    form.elements.cover.value = letter?.cover || '';
    form.elements.content.value = letter?.content || '';
    document.getElementById('letter-admin-dialog-title').textContent = letter ? '编辑信件' : '新增信件';
    document.getElementById('letter-admin-submit').textContent = letter ? '保存修改' : '保存信件';
    document.getElementById('letter-admin-dialog-message').textContent = '';
    preview.innerHTML = renderMarkdownPreview(contentInput.value);
    dialog.showModal();
  }

  document.getElementById('letter-admin-create').addEventListener('click', () => openEditor());
  document.getElementById('letter-admin-close').addEventListener('click', () => dialog.close());
  document.getElementById('letter-admin-cancel').addEventListener('click', () => dialog.close());
  contentInput.addEventListener('input', () => { preview.innerHTML = renderMarkdownPreview(contentInput.value); });

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    if (!form.reportValidity()) return;
    const values = new FormData(form);
    const letter = {
      title: String(values.get('title') || '').trim(),
      unlockType: String(values.get('unlockType') || ''),
      unlockValue: Number(values.get('unlockValue')),
      sort: Number(values.get('sort')),
      cover: String(values.get('cover') || '').trim() || null,
      content: String(values.get('content') || ''),
    };
    const saveButton = document.getElementById('letter-admin-submit');
    const message = document.getElementById('letter-admin-dialog-message');
    saveButton.disabled = true;
    message.textContent = '';
    try {
      if (editingId == null) await createAdminLetter(letter, adminToken);
      else await updateAdminLetter(editingId, letter, adminToken);
      dialog.close();
      await refreshLetters(editingId == null ? '信件已新增。' : '信件已更新。');
      editingId = null;
    } catch (error) {
      message.textContent = error.message || '保存没有成功，请检查填写内容后重试。';
    } finally {
      saveButton.disabled = false;
    }
  });

  document.getElementById('letter-admin-save-sort').addEventListener('click', async (event) => {
    const inputs = [...list.querySelectorAll('[data-letter-sort]')];
    if (!inputs.every((input) => input.reportValidity())) return;
    const updates = inputs.map((input) => ({ id: Number(input.dataset.letterSort), sort: Number(input.value) }));
    const button = event.currentTarget;
    button.disabled = true;
    try {
      await updateAdminLetterSort(updates, adminToken);
      await refreshLetters('排序已保存。');
    } catch (error) {
      status.textContent = error.message || '排序没有保存，请稍后重试。';
    } finally {
      button.disabled = false;
    }
  });

  gateForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    if (!gateForm.reportValidity()) return;
    const submit = document.getElementById('letter-admin-gate-submit');
    const credentials = {
      username: String(new FormData(gateForm).get('username') || '').trim(),
      password: String(new FormData(gateForm).get('password') || ''),
    };
    submit.disabled = true;
    gateMessage.textContent = '正在验证管理员身份……';
    try {
      adminToken = await loginAdmin(credentials);
      sessionStorage.setItem('growthGardenAdminToken', adminToken);
      gateForm.elements.password.value = '';
      await refreshLetters();
    } catch (error) {
      adminToken = null;
      sessionStorage.removeItem('growthGardenAdminToken');
      gateMessage.textContent = error instanceof ApiError && error.status === 403
        ? '管理员账号或密码不正确，请重试。'
        : (error.message || '暂时无法验证，请稍后重试。');
    } finally {
      submit.disabled = false;
    }
  });

  if (adminToken) await refreshLetters();
}

function showAccountManagement() {
  const app = document.getElementById('app');
  app.dataset.view = 'account';
  page.className = 'page page--account';
  page.innerHTML = '<header class="account-page__header">' +
    '<button class="emotion-page__back" id="account-back" type="button" aria-label="返回花园" title="返回花园">' +
      '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M19 12H5m7 7-7-7 7-7" /></svg>' +
    '</button><span class="account-page__brand">Growth Garden</span>' + profileMenuMarkup('emotion-page__profile') +
    '</header>' +
    '<main class="account-panel"><p class="account-panel__eyebrow">A QUIET PLACE FOR YOU</p>' +
      '<h1>账号管理</h1><p class="account-panel__lead">更新进入花园的钥匙。</p>' +
      '<section class="account-password"><h2>修改登录密码</h2>' +
        '<form id="change-password-form" novalidate>' +
          '<label class="account-password__field"><span>当前密码</span><input name="currentPassword" type="password" autocomplete="current-password" required /></label>' +
          '<label class="account-password__field"><span>新密码</span><input name="newPassword" type="password" autocomplete="new-password" minlength="8" maxlength="72" required /><small>请使用 8 至 72 个字符。</small></label>' +
          '<label class="account-password__field"><span>确认新密码</span><input name="confirmPassword" type="password" autocomplete="new-password" minlength="8" maxlength="72" required /></label>' +
          '<p class="account-password__message" id="change-password-message" role="status" aria-live="polite"></p>' +
          '<div class="account-password__actions"><button class="account-password__cancel" id="account-cancel" type="button">取消</button><button class="account-password__submit" id="change-password-submit" type="submit">确认修改</button></div>' +
        '</form>' +
      '</section>' +
    '</main>';

  document.getElementById('account-back').addEventListener('click', () => navigate('/garden'));
  document.getElementById('account-cancel').addEventListener('click', () => navigate('/garden'));
  bindProfileMenu();

  const form = document.getElementById('change-password-form');
  const message = document.getElementById('change-password-message');
  const submit = document.getElementById('change-password-submit');
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const currentPassword = form.elements.currentPassword.value;
    const newPassword = form.elements.newPassword.value;
    const confirmPassword = form.elements.confirmPassword.value;
    if (!currentPassword || !newPassword || !confirmPassword) {
      message.textContent = '请填写全部三项密码。';
      (!currentPassword ? form.elements.currentPassword : !newPassword ? form.elements.newPassword : form.elements.confirmPassword).focus();
      return;
    }
    if (newPassword.length < 8 || newPassword.length > 72) {
      message.textContent = '新密码长度需为 8 至 72 个字符。';
      form.elements.newPassword.focus();
      return;
    }
    if (newPassword !== confirmPassword) {
      message.textContent = '两次输入的新密码不一致。';
      form.elements.confirmPassword.focus();
      return;
    }
    if (newPassword === currentPassword) {
      message.textContent = '新密码需要与当前密码不同。';
      form.elements.newPassword.focus();
      return;
    }

    message.textContent = '';
    submit.disabled = true;
    submit.textContent = '正在更新…';
    try {
      await changePassword({ currentPassword, newPassword, confirmPassword });
      form.reset();
      message.textContent = '登录密码已更新。当前花园会话仍然有效。';
    } catch (error) {
      if (error instanceof ApiError && [401, 403].includes(error.status)) {
        clearSession();
        navigate('/user/login');
        return;
      }
      message.textContent = error instanceof TypeError
        ? '花园暂时没有回应，请稍后重试。'
        : error.message || '密码暂时没有更新，请稍后重试。';
    } finally {
      submit.disabled = false;
      submit.textContent = '确认修改';
    }
  });
}

function renderRoute() {
  const path = window.location.pathname;
  const userLetterDetail = path.match(/^\/garden\/letters\/([^/]+)$/);
  const emotionDetailId = path === '/garden/emotions'
    ? new URLSearchParams(window.location.search).get('emotionId')
    : null;
  if (path === '/garden' || path === '/garden/emotions' || path === '/garden/letters' || path === '/garden/account' || userLetterDetail) {
    if (getToken()) {
      if (path === '/garden/account') showAccountManagement();
      else if (path === '/garden/letters') showUserLetters();
      else if (userLetterDetail) void showUserLetterDetail(decodeURIComponent(userLetterDetail[1]));
      else if (emotionDetailId) void showEmotionDetail(emotionDetailId);
      else if (path === '/garden/emotions') void showEmotionBottles();
      else void showHome();
      return;
    }
    window.history.replaceState({}, '', '/user/login');
  } else if (path === '/admin/letters') {
    void showAdminLetters();
    return;
  } else if (!['/login', '/user/login', '/'].includes(path)) {
    window.history.replaceState({}, '', '/user/login');
  }

  // Visiting a login URL always shows the login form, even if an older JWT
  // remains in localStorage. A successful login will navigate to /garden.
  showLogin();
}

window.addEventListener('popstate', renderRoute);
renderRoute();
