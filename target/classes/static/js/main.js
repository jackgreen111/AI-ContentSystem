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
// ============================================================
//  SIDEBAR v3 — Collapse + Mobile + Search + Toast + Skeleton
//  Добавить в КОНЕЦ main.js
// ============================================================

(function () {

  // ── Элементы ──────────────────────────────────────────────
  const sidebar  = document.getElementById('sidebar');
  const overlay  = document.getElementById('sidebarOverlay');
  const burger   = document.getElementById('sidebarBurger');
  const collapseBtn = document.getElementById('sidebarCollapseBtn');
  const COLLAPSE_KEY = 'acs-sidebar-collapsed';

  if (!sidebar) return;

  // ── Десктоп: collapse / expand ────────────────────────────
  function isDesktop() { return window.innerWidth > 768; }

  function setSidebarCollapsed(collapsed) {
    sidebar.classList.toggle('collapsed', collapsed);
    localStorage.setItem(COLLAPSE_KEY, collapsed ? '1' : '0');
  }

  // Восстановить состояние при загрузке
  if (isDesktop() && localStorage.getItem(COLLAPSE_KEY) === '1') {
    sidebar.classList.add('collapsed');
  }

  if (collapseBtn) {
    collapseBtn.addEventListener('click', function () {
      if (!isDesktop()) return;
      setSidebarCollapsed(!sidebar.classList.contains('collapsed'));
    });
  }

  // ── Мобильные: открыть / закрыть ──────────────────────────
  function openMobileSidebar() {
    sidebar.classList.add('mobile-open');
    overlay.classList.add('visible');
    burger && burger.classList.add('open');
    document.body.style.overflow = 'hidden';
  }

  function closeMobileSidebar() {
    sidebar.classList.remove('mobile-open');
    overlay.classList.remove('visible');
    burger && burger.classList.remove('open');
    document.body.style.overflow = '';
  }

  if (burger) {
    burger.addEventListener('click', function () {
      if (sidebar.classList.contains('mobile-open')) {
        closeMobileSidebar();
      } else {
        openMobileSidebar();
      }
    });
  }

  if (overlay) {
    overlay.addEventListener('click', closeMobileSidebar);
  }

  // Закрыть при навигации на мобильных
  document.querySelectorAll('.nav-item').forEach(function (item) {
    item.addEventListener('click', function () {
      if (!isDesktop()) closeMobileSidebar();
    });
  });

  // Закрыть по Escape
  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') closeMobileSidebar();
  });

  // ── Поиск по истории в sidebar ────────────────────────────
  const searchInput   = document.getElementById('sidebarSearch');
  const searchResults = document.getElementById('searchResults');

  if (searchInput && searchResults) {
    let debounceTimer;

    searchInput.addEventListener('input', function () {
      clearTimeout(debounceTimer);
      const q = this.value.trim();

      if (q.length < 2) {
        searchResults.classList.remove('open');
        searchResults.innerHTML = '';
        return;
      }

      debounceTimer = setTimeout(function () {
        fetch('/content/search?q=' + encodeURIComponent(q))
          .then(function (r) { return r.json(); })
          .then(function (data) {
            searchResults.innerHTML = '';
            if (!data.length) {
              searchResults.innerHTML = '<div class="search-result-empty">Ничего не найдено</div>';
            } else {
              data.slice(0, 6).forEach(function (item) {
                const a = document.createElement('a');
                a.className = 'search-result-item';
                a.href = '/content/view/' + item.id;
                a.textContent = item.topic;
                a.title = item.topic;
                searchResults.appendChild(a);
              });
            }
            searchResults.classList.add('open');
          })
          .catch(function () {
            searchResults.classList.remove('open');
          });
      }, 300);
    });

    // Закрыть при клике вне
    document.addEventListener('click', function (e) {
      if (!searchInput.contains(e.target) && !searchResults.contains(e.target)) {
        searchResults.classList.remove('open');
      }
    });

    searchInput.addEventListener('keydown', function (e) {
      if (e.key === 'Escape') {
        searchResults.classList.remove('open');
        searchInput.blur();
      }
    });
  }

  // ── Toast уведомления ─────────────────────────────────────
  // Создать контейнер
  const toastContainer = document.createElement('div');
  toastContainer.className = 'toast-container';
  document.body.appendChild(toastContainer);

  window.showToast = function (message, type, duration) {
    type = type || 'info';
    duration = duration || 3500;

    const icons = { success: '✅', error: '❌', warning: '⚠️', info: 'ℹ️' };
    const toast = document.createElement('div');
    toast.className = 'toast toast-' + type;
    toast.innerHTML =
      '<span class="toast-icon">' + (icons[type] || 'ℹ️') + '</span>' +
      '<span class="toast-msg">' + message + '</span>' +
      '<button class="toast-close" onclick="this.parentElement.remove()">✕</button>';

    toastContainer.appendChild(toast);

    setTimeout(function () {
      toast.classList.add('hiding');
      setTimeout(function () { toast.remove(); }, 300);
    }, duration);
  };

  // Заменить все alert() на toast (патч)
  const _origAlert = window.alert;
  window.alert = function (msg) {
    if (typeof msg === 'string' && msg.length < 200) {
      const type = msg.includes('❌') || msg.includes('Ошибка') ? 'error' :
                   msg.includes('✅') || msg.includes('сохран') || msg.includes('Скопиров') ? 'success' : 'info';
      window.showToast(msg, type);
    } else {
      _origAlert(msg);
    }
  };

  // Toast для flash-сообщений (если есть .alert на странице)
  document.querySelectorAll('.alert').forEach(function (el) {
    const type = el.classList.contains('alert-success') ? 'success' :
                 el.classList.contains('alert-error')   ? 'error'   : 'info';
    const msg = el.textContent.trim();
    if (msg) {
      setTimeout(function () { window.showToast(msg, type); }, 300);
    }
    el.style.display = 'none';
  });

})();

// ── Skeleton helpers ───────────────────────────────────────
window.showSkeleton = function (containerId, count) {
  const el = document.getElementById(containerId);
  if (!el) return;
  let html = '';
  for (let i = 0; i < (count || 3); i++) {
    html += '<div style="padding:16px;background:var(--bg-card);border-radius:var(--radius);margin-bottom:12px;border:1px solid var(--border)">' +
              '<div class="skeleton skeleton-title"></div>' +
              '<div class="skeleton skeleton-text"></div>' +
              '<div class="skeleton skeleton-text" style="width:75%"></div>' +
            '</div>';
  }
  el.innerHTML = html;
};
