/*
 * Giao dien sang / toi cho toan bo CineFlow.
 * - Mac dinh SANG; lua chon luu o localStorage ("cf-theme").
 * - Dat <html data-theme="light|dark" data-bs-theme="..."> ngay khi nap (truoc khi ve noi dung).
 * - Chen nut doi giao dien vao goc tren ben phai: header trang khach (.cf-header-right),
 *   topbar trang quan ly / nhan vien (.cf-topbar), neu khong co thi nut noi o goc phai.
 * Mau sang nam o css/common/theme-light.css (tu sinh tu tools/theme/generate-light-theme.js).
 */
(function () {
    var KEY = 'cf-theme';
    var root = document.documentElement;

    function read() {
        try { return localStorage.getItem(KEY) === 'dark' ? 'dark' : 'light'; } catch (e) { return 'light'; }
    }

    function apply(theme) {
        root.setAttribute('data-theme', theme);
        root.setAttribute('data-bs-theme', theme);
        var btn = document.getElementById('cf-theme-toggle');
        if (btn) {
            btn.textContent = theme === 'light' ? '☾' : '☀';
            btn.title = theme === 'light' ? 'Chuyển sang giao diện tối' : 'Chuyển sang giao diện sáng';
            btn.setAttribute('aria-label', btn.title);
        }
    }

    apply(read());

    /*
     * Gan class cf-css-<duong-dan> (vd cf-css-auth-base) cho moi file CSS cua du an trang dang nap, ke ca @import.
     * Mau sang cua file rieng tung trang chi ap khi co class nay, giong nhu giao dien toi chi ap noi nap file.
     * Phai khop voi cssClass() trong tools/theme/generate-light-theme.js.
     */
    function cssClass(href) {
        var m = /\/css\/(.+?)\.css(?:[?#]|$)/i.exec(href || '');
        return m ? 'cf-css-' + m[1].replace(/[^a-z0-9]+/gi, '-').toLowerCase() : null;
    }

    function markCss(sheet, seen) {
        if (!sheet || seen.indexOf(sheet) >= 0) return;
        seen.push(sheet);
        var cls = cssClass(sheet.href);
        if (cls) root.classList.add(cls);
        var rules;
        try { rules = sheet.cssRules; } catch (e) { return; }      // CSS khac ten mien (CDN, Google Fonts)
        if (!rules) return;
        for (var i = 0; i < rules.length; i++) {
            if (rules[i].type === 3) markCss(rules[i].styleSheet, seen);   // CSSRule.IMPORT_RULE
        }
    }

    function markLoadedCss() {
        var seen = [];
        for (var i = 0; i < document.styleSheets.length; i++) markCss(document.styleSheets[i], seen);
    }

    markLoadedCss();
    document.addEventListener('DOMContentLoaded', markLoadedCss);
    window.addEventListener('load', markLoadedCss);

    function toggle() {
        var next = root.getAttribute('data-theme') === 'light' ? 'dark' : 'light';
        try { localStorage.setItem(KEY, next); } catch (e) { /* trinh duyet chan luu: chi doi trang hien tai */ }
        apply(next);
    }

    function mount() {
        if (document.getElementById('cf-theme-toggle')) return;
        var btn = document.createElement('button');
        btn.type = 'button';
        btn.id = 'cf-theme-toggle';
        btn.className = 'cf-theme-toggle';
        btn.addEventListener('click', toggle);

        var headerRight = document.querySelector('.cf-header-right');
        var topbar = document.querySelector('.cf-topbar');
        if (headerRight) {
            headerRight.insertBefore(btn, headerRight.firstChild);
        } else if (topbar) {
            var user = topbar.querySelector('.cf-topbar-user');
            if (user && user.parentNode === topbar) topbar.insertBefore(btn, user);
            else topbar.appendChild(btn);
        } else {
            btn.classList.add('cf-theme-toggle-floating');
            document.body.appendChild(btn);
        }
        apply(root.getAttribute('data-theme'));
    }

    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', mount);
    else mount();
})();
