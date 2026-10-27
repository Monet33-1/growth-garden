const TOKEN_KEY = 'growth-garden-token';
const NICKNAME_KEY = 'growth-garden-nickname';

export function getToken() {
  return window.localStorage.getItem(TOKEN_KEY);
}

export function saveSession({ token, nickname }) {
  window.localStorage.setItem(TOKEN_KEY, token);
  if (nickname) window.localStorage.setItem(NICKNAME_KEY, nickname);
  else window.localStorage.removeItem(NICKNAME_KEY);
}

export function getNickname() {
  return window.localStorage.getItem(NICKNAME_KEY) || '';
}

export function clearSession() {
  window.localStorage.removeItem(TOKEN_KEY);
  window.localStorage.removeItem(NICKNAME_KEY);
}
