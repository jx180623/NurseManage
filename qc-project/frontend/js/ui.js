/**
 * ui.js — 公共 UI 工具：Toast 提示、Loading、Modal 确认框
 */

/* ══ Toast ══════════════════════════════════════ */
let _toastTimer = null;

export function toast(msg, type = 'success', duration = 3000) {
  let el = document.getElementById('__toast__');
  if (!el) {
    el = document.createElement('div');
    el.id = '__toast__';
    el.style.cssText = `
      position:fixed;top:76px;right:28px;padding:13px 20px;border-radius:12px;
      font-size:14px;font-weight:500;z-index:9999;transform:translateY(-20px);
      opacity:0;transition:all .3s;pointer-events:none;font-family:'Noto Sans SC',sans-serif;
      box-shadow:0 6px 24px rgba(0,0,0,.2);max-width:320px;word-break:break-all;`;
    document.body.appendChild(el);
  }
  const colors = {
    success: ['#27ae60','#1e8449'],
    error:   ['#c0392b','#96281b'],
    info:    ['#2980b9','#1f6391'],
    warn:    ['#d4a843','#b8922a'],
  };
  const [bg] = colors[type] || colors.info;
  el.style.background = bg;
  el.style.color = '#fff';
  el.textContent = msg;
  el.style.opacity = '1';
  el.style.transform = 'translateY(0)';

  clearTimeout(_toastTimer);
  _toastTimer = setTimeout(() => {
    el.style.opacity = '0';
    el.style.transform = 'translateY(-20px)';
  }, duration);
}

/* ══ Loading overlay ═══════════════════════════ */
export function showLoading(text = '加载中…') {
  let el = document.getElementById('__loading__');
  if (!el) {
    el = document.createElement('div');
    el.id = '__loading__';
    el.style.cssText = `
      position:fixed;inset:0;background:rgba(26,26,46,.45);
      display:flex;align-items:center;justify-content:center;z-index:8888;`;
    el.innerHTML = `
      <div style="background:white;border-radius:16px;padding:32px 40px;text-align:center;box-shadow:0 8px 40px rgba(0,0,0,.25);">
        <div style="width:40px;height:40px;border:4px solid #f0ebe3;border-top-color:#c0392b;border-radius:50%;animation:spin .8s linear infinite;margin:0 auto 14px;"></div>
        <div id="__loading_text__" style="font-size:14px;color:#7a6e65;font-family:'Noto Sans SC',sans-serif;">${text}</div>
      </div>
      <style>@keyframes spin{to{transform:rotate(360deg)}}</style>`;
    document.body.appendChild(el);
  } else {
    const t = document.getElementById('__loading_text__');
    if (t) t.textContent = text;
    el.style.display = 'flex';
  }
}

export function hideLoading() {
  const el = document.getElementById('__loading__');
  if (el) el.style.display = 'none';
}

/* ══ Confirm Modal ══════════════════════════════ */
export function confirm(msg, title = '确认操作') {
  return new Promise(resolve => {
    let el = document.getElementById('__confirm__');
    if (el) el.remove();
    el = document.createElement('div');
    el.id = '__confirm__';
    el.style.cssText = `position:fixed;inset:0;background:rgba(26,26,46,.5);display:flex;align-items:center;justify-content:center;z-index:9000;`;
    el.innerHTML = `
      <div style="background:white;border-radius:18px;padding:32px;width:360px;max-width:90vw;box-shadow:0 16px 60px rgba(0,0,0,.3);">
        <div style="font-family:'Noto Serif SC',serif;font-size:18px;font-weight:700;color:#1a1a2e;margin-bottom:12px;">${title}</div>
        <div style="font-size:14px;color:#7a6e65;line-height:1.7;margin-bottom:24px;">${msg}</div>
        <div style="display:flex;gap:10px;justify-content:flex-end;">
          <button id="__confirm_cancel__" style="padding:9px 20px;border:1.5px solid #d9c9b8;border-radius:9px;background:white;font-size:14px;font-family:'Noto Sans SC',sans-serif;cursor:pointer;">取消</button>
          <button id="__confirm_ok__" style="padding:9px 20px;border:none;border-radius:9px;background:linear-gradient(135deg,#c0392b,#96281b);color:white;font-size:14px;font-family:'Noto Sans SC',sans-serif;font-weight:600;cursor:pointer;">确定</button>
        </div>
      </div>`;
    document.body.appendChild(el);
    document.getElementById('__confirm_ok__').onclick     = () => { el.remove(); resolve(true); };
    document.getElementById('__confirm_cancel__').onclick = () => { el.remove(); resolve(false); };
  });
}

/* ══ 工具函数 ═══════════════════════════════════ */
export function today() { return new Date().toISOString().slice(0, 10); }

export function guardLogin() {
  const token = localStorage.getItem('qc_token');
  const name  = localStorage.getItem('qc_name');
  const empId = localStorage.getItem('qc_emp_id');

  if (!token || !name || !empId) {
    // 调试：打印当前 localStorage 内容，帮助排查问题
    console.warn('[guardLogin] 登录信息缺失，重定向到登录页');
    console.warn('[guardLogin] token:', token ? '存在' : '缺失');
    console.warn('[guardLogin] name:',  name  ? name  : '缺失');
    console.warn('[guardLogin] empId:', empId ? empId : '缺失');
    // 使用绝对路径，避免因 pages/ 子目录导致路径错误
    location.href = '/index.html';
    return false;
  }
  return true;
}

export function guardAdmin() {
  if (!guardLogin()) return false;
  if (localStorage.getItem('qc_admin') !== '1') {
    toast('需要管理员权限', 'error');
    return false;
  }
  return true;
}
