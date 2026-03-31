/**
 * api.js — 统一封装所有后端接口调用（与最新 HTML 完全对齐）
 * Nginx 反向代理 /api/* → 后端 :8080，前端无需改动 BASE_URL
 */

const BASE_URL = '/api';

/* ── 核心请求 ─────────────────────────────────────── */
async function request(method, path, body = null, isBlob = false) {
  const token = localStorage.getItem('qc_token');
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = 'Bearer ' + token;

  const opts = { method, headers };
  if (body) opts.body = JSON.stringify(body);

  const res = await fetch(BASE_URL + path, opts);

  if (isBlob) {
    if (!res.ok) throw new Error('导出失败，HTTP ' + res.status);
    const blob = await res.blob();
    const cd = res.headers.get('Content-Disposition') || '';
    const match = cd.match(/filename\*?=(?:UTF-8'')?([^;]+)/i);
    const filename = match ? decodeURIComponent(match[1]) : 'export.xlsx';
    return { blob, filename };
  }

  const data = await res.json();
  if (data.code === 401) {
    localStorage.clear();
    location.href = '/index.html';
    return;
  }
  return data;
}

const get  = (path)       => request('GET',    path);
const post = (path, body) => request('POST',   path, body);
const put  = (path, body) => request('PUT',    path, body);
const del  = (path)       => request('DELETE', path);

/* ── Auth ────────────────────────────────────────── */
export const AuthAPI = {
  login: (employeeId, password) =>
    post('/auth/login', { employeeId, password }),

  /** 护士修改自己的密码 */
  changeMyPassword: (currentPassword, newPassword) =>
    post('/auth/change-password', { currentPassword, newPassword }),
};

/* ── Nurses ──────────────────────────────────────── */
export const NurseAPI = {
  listEnabled:   ()         => get('/nurses/list'),
  listAll:       ()         => get('/nurses/all'),
  create:        (body)     => post('/nurses', body),
  update:        (id, body) => put(`/nurses/${id}`, body),
  delete:        (id)       => del(`/nurses/${id}`),
  /** 管理员重置指定护士密码 */
  resetPassword: (employeeId, newPassword) =>
    post('/nurses/reset-password', { employeeId, newPassword }),
};

/* ── Reports ──────────────────────────────────────── */
export const ReportAPI = {
  submit:    (body)       => post('/reports', body),
  myHistory: ()           => get('/reports/mine'),
  byDate:    (date)       => get(`/reports/by-date/${date}`),

  adminAll: (start, end) => {
    const p = [];
    if (start) p.push('start=' + start);
    if (end)   p.push('end='   + end);
    return get('/reports/admin/all' + (p.length ? '?' + p.join('&') : ''));
  },

  delete: (id) => del(`/reports/${id}`),

  exportExcel: (start, end) => {
    const p = [];
    if (start) p.push('start=' + start);
    if (end)   p.push('end='   + end);
    return request('GET', '/reports/admin/export' + (p.length ? '?' + p.join('&') : ''), null, true);
  },
};

/* ── Config（管理员密码） ──────────────────────────── */
export const ConfigAPI = {
  changeAdminPassword: (currentPw, newPw) =>
    post('/config/admin-password', { currentPw, newPw }),
};

/* ── 本地用户信息 ──────────────────────────────────── */
export const Auth = {
  save(data) {
    localStorage.setItem('qc_token',  data.token);
    localStorage.setItem('qc_emp_id', data.employeeId);
    localStorage.setItem('qc_name',   data.name);
    localStorage.setItem('qc_admin',  data.admin ? '1' : '0');
  },
  clear() {
    ['qc_token','qc_emp_id','qc_name','qc_admin'].forEach(k => localStorage.removeItem(k));
  },
  token()      { return localStorage.getItem('qc_token'); },
  employeeId() { return localStorage.getItem('qc_emp_id'); },
  name()       { return localStorage.getItem('qc_name'); },
  isAdmin()    { return localStorage.getItem('qc_admin') === '1'; },
  isLoggedIn() { return !!localStorage.getItem('qc_token'); },
};
