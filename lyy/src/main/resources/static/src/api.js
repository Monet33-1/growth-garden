import { getToken } from './session.js?v=23';

export class ApiError extends Error {
  constructor(message, status = 0) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

async function request(path, options = {}) {
  const token = getToken();
  const response = await fetch(path, {
    ...options,
    headers: {
      Accept: 'application/json',
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });

  let payload;
  try {
    payload = await response.json();
  } catch {
    payload = null;
  }

  if (!response.ok || (payload && typeof payload.code === 'number' && payload.code !== 200)) {
    throw new ApiError(payload?.message || '请求没有成功', response.status);
  }
  return payload;
}

export async function login({ username, password }) {
  const result = await request('/user/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });
  if (!result?.token) throw new ApiError('登录信息不完整');
  return result;
}

export async function getSetupStatus() {
  const result = await request('/user/setup/status');
  return result?.data === true;
}

export async function setupAccount({ username, password, nickname, deepseekApiKey }) {
  const result = await request('/user/setup', {
    method: 'POST',
    body: JSON.stringify({ username, password, nickname, deepseekApiKey }),
  });
  if (!result?.data?.token) {
    throw new ApiError(result?.message || '初始化没有完成');
  }
  return result.data;
}

export async function changePassword({ currentPassword, newPassword, confirmPassword }) {
  return request('/user/password', {
    method: 'PUT',
    body: JSON.stringify({ currentPassword, newPassword, confirmPassword }),
  });
}

export async function getGardenInfo() {
  const result = await request('/garden/info');
  if (!result?.data) throw new ApiError(result?.message || '还没有读到花园信息');
  return result.data;
}

export async function getMyDiaries() {
  const result = await request('/diary/list');
  if (!Array.isArray(result?.data)) throw new ApiError(result?.message || '暂时无法读取随笔');
  return result.data;
}

export async function getEmotions() {
  const result = await request('/emotion/list');
  if (!Array.isArray(result?.data)) throw new ApiError(result?.message || '暂时无法读取情绪瓶');
  return result.data;
}

export async function addDiaryEmotion(diaryId, emotionId) {
  return request(`/emotion/diary/${encodeURIComponent(diaryId)}/${encodeURIComponent(emotionId)}`, {
    method: 'POST',
  });
}

export async function createDiary({ title, content }) {
  const result = await request('/diary', {
    method: 'POST',
    body: JSON.stringify({ title, content }),
  });
  if (!result?.data || typeof result.data !== 'object') {
    throw new ApiError(result?.message || '随笔已提交，但没有收到保存结果');
  }
  return result.data;
}

export async function deleteDiary(id) {
  return request(`/diary/${encodeURIComponent(id)}`, { method: 'DELETE' });
}

export async function getMyLetters() {
  const result = await request('/letter/list');
  if (!Array.isArray(result?.data)) throw new ApiError(result?.message || '暂时无法读取信件');
  return result.data;
}

export async function getLetterDetail(id) {
  const result = await request(`/letter/${encodeURIComponent(id)}`);
  if (!result?.data || typeof result.data !== 'object') {
    throw new ApiError(result?.message || '暂时无法打开这封信');
  }
  return result.data;
}

export async function completeLetter(id) {
  const result = await request(`/letter/${encodeURIComponent(id)}/complete`, { method: 'POST' });
  if (!result?.data || typeof result.data !== 'object' || !result.data.diaryId) {
    throw new ApiError(result?.message || '信件暂时没有收进花园');
  }
  return result.data;
}

function adminHeaders(adminToken) {
  return adminToken ? { Authorization: `Bearer ${adminToken}` } : {};
}

export async function loginAdmin({ username, password }) {
  const result = await request('/letter/admin/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });
  if (typeof result?.data !== 'string' || !result.data) {
    throw new ApiError(result?.message || '管理员验证没有成功');
  }
  return result.data;
}

export async function getAdminLetters(adminToken) {
  const result = await request('/letter/admin/list', { headers: adminHeaders(adminToken) });
  if (!Array.isArray(result?.data)) throw new ApiError(result?.message || '暂时无法读取预置信件');
  return result.data;
}

export async function createAdminLetter(letter, adminToken) {
  const result = await request('/letter/admin', {
    method: 'POST',
    body: JSON.stringify(letter),
    headers: adminHeaders(adminToken),
  });
  if (!result?.data?.id) throw new ApiError(result?.message || '没有收到新信件信息');
  return result.data;
}

export async function updateAdminLetter(id, letter, adminToken) {
  const result = await request(`/letter/admin/${encodeURIComponent(id)}`, {
    method: 'PUT',
    body: JSON.stringify(letter),
    headers: adminHeaders(adminToken),
  });
  if (!result?.data?.id) throw new ApiError(result?.message || '没有收到更新后的信件信息');
  return result.data;
}

export async function deleteAdminLetter(id, adminToken) {
  return request(`/letter/admin/${encodeURIComponent(id)}`, {
    method: 'DELETE',
    headers: adminHeaders(adminToken),
  });
}

export async function updateAdminLetterSort(letters, adminToken) {
  return request('/letter/admin/sort', {
    method: 'PUT',
    body: JSON.stringify(letters),
    headers: adminHeaders(adminToken),
  });
}
