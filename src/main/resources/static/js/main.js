document.addEventListener('DOMContentLoaded', function () {

  // ── Тёмная тема ────────────────────────────────────────────
  const html   = document.documentElement;
  const toggle = document.getElementById('themeToggle');
  const saved  = localStorage.getItem('acs-theme') || 'light';
  applyTheme(saved);

  if (toggle) {
    toggle.addEventListener('click', function () {
      const next = html.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
      applyTheme(next);
      localStorage.setItem('acs-theme', next);
    });
  }

  function applyTheme(theme) {
    html.setAttribute('data-theme', theme);
    if (toggle) toggle.textContent = theme === 'dark' ? '☀️' : '🌙';
    const mdCss = document.getElementById('mdCss');
    if (mdCss) {
      mdCss.href = theme === 'dark'
        ? 'https://cdn.jsdelivr.net/npm/github-markdown-css@5/github-markdown-dark.min.css'
        : 'https://cdn.jsdelivr.net/npm/github-markdown-css@5/github-markdown-light.min.css';
    }
  }

  // ── Sidebar active link ─────────────────────────────────────
  const path = window.location.pathname;
  document.querySelectorAll('.nav-item').forEach(function(item) {
    const href = item.getAttribute('href');
    if (href && path.startsWith(href) && href !== '/') {
      item.classList.add('active');
    } else if (href === '/dashboard' && path === '/dashboard') {
      item.classList.add('active');
    }
  });

  // ── Счётчик символов ───────────────────────────────────────
  const topicInput = document.getElementById('topic');
  const topicCount = document.getElementById('topicCount');
  if (topicInput && topicCount) {
    topicCount.textContent = topicInput.value.length;
    topicInput.addEventListener('input', function () {
      const len = this.value.length;
      topicCount.textContent = len;
      topicCount.style.color = len > 450 ? '#ef4444' : 'var(--text-muted)';
    });
  }

  // ── Загрузочный экран ───────────────────────────────────────
  const form    = document.getElementById('generateForm');
  const overlay = document.getElementById('loadingOverlay');
  const submit  = document.getElementById('submitBtn');
  if (form && overlay) {
    form.addEventListener('submit', function () {
      const hasImg = document.getElementById('imageFile') &&
                     document.getElementById('imageFile').files.length > 0;
      const msgEl = document.getElementById('loadingMsg');
      if (msgEl) msgEl.textContent = hasImg ? '📸 AI читает фото...' : '✨ AI генерирует контент...';
      overlay.style.display = 'flex';
      if (submit) { submit.disabled = true; submit.textContent = '⏳ Генерирую...'; }
    });
  }

  // ── Авто-скрытие уведомлений ────────────────────────────────
  document.querySelectorAll('.alert').forEach(function (el) {
    setTimeout(function () {
      el.style.transition = 'opacity .5s';
      el.style.opacity = '0';
      setTimeout(function () { el.remove(); }, 500);
    }, 5000);
  });

  // ── Ripple эффект на кнопках ────────────────────────────────
  document.querySelectorAll('.btn').forEach(function(btn) {
    btn.addEventListener('click', function(e) {
      const rect = btn.getBoundingClientRect();
      const ripple = document.createElement('span');
      ripple.style.cssText = `
        position:absolute;border-radius:50%;
        width:80px;height:80px;
        background:rgba(255,255,255,.3);
        left:${e.clientX-rect.left-40}px;
        top:${e.clientY-rect.top-40}px;
        transform:scale(0);
        animation:rippleAnim .5s ease forwards;
        pointer-events:none;
      `;
      if (!document.getElementById('ripple-style')) {
        const s = document.createElement('style');
        s.id = 'ripple-style';
        s.textContent = '@keyframes rippleAnim{to{transform:scale(3);opacity:0;}}';
        document.head.appendChild(s);
      }
      btn.appendChild(ripple);
      setTimeout(() => ripple.remove(), 500);
    });
  });
});
