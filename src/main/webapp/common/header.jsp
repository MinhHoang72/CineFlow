<%@ page pageEncoding="UTF-8" %>
<%@ page import="model.User" %>
<%!
    private User safeSessionUser(Object value) {
        if (value instanceof User) {
            return (User) value;
        }
        if (value == null) {
            return null;
        }
        try {
            User compatible = new User();
            Class<?> type = value.getClass();
            try {
                compatible.setUserId((Integer) type.getMethod("getUserId").invoke(value));
            } catch (Exception ignored) {}
            compatible.setFullName((String) type.getMethod("getFullName").invoke(value));
            compatible.setAccount((String) type.getMethod("getAccount").invoke(value));
            compatible.setRole((String) type.getMethod("getRole").invoke(value));
            try {
                compatible.setAvatar((String) type.getMethod("getAvatar").invoke(value));
            } catch (Exception ignored) {}
            return compatible;
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            return null;
        }
    }

    private String safeEscape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }
%>
<%
    String activeNav = request.getParameter("activeNav");
    if (activeNav == null || activeNav.isBlank()) {
        activeNav = (String) request.getAttribute("activeNav");
    }
    if (activeNav == null) {
        activeNav = "";
    }

    User headerUser = safeSessionUser(session.getAttribute("user"));
    String headerMemberName = headerUser == null ? "" : headerUser.getFullName();
    if (headerMemberName == null || headerMemberName.isBlank()) {
        headerMemberName = headerUser == null ? "" : headerUser.getAccount();
    }
    String headerAccount = headerUser == null ? "" : headerUser.getAccount();
    if (headerAccount == null) {
        headerAccount = "";
    }
    String headerRole = headerUser == null ? "" : (headerUser.getRole() == null ? "" : headerUser.getRole().toUpperCase());
    String headerAvatar = headerUser == null ? null : headerUser.getAvatar();
    String headerInitial = "U";
    if (!headerMemberName.isEmpty()) {
        headerInitial = headerMemberName.substring(0, 1).toUpperCase();
    } else if (!headerAccount.isEmpty()) {
        headerInitial = headerAccount.substring(0, 1).toUpperCase();
    }
    String ctx = request.getContextPath();
%>
<header class="cf-main-header" id="cfMainHeader">
    <div class="cf-header-container">
        <!-- Logo Brand -->
        <a class="cf-brand" href="<%= ctx %>/home/index.jsp" aria-label="CineFlow Cinema Home">
            <span class="cf-brand-icon">C</span>
            <span class="cf-brand-name">Cine<span class="cf-brand-glow">Flow</span></span>
        </a>

        <!-- Main Nav Menu -->
        <nav class="cf-navbar" aria-label="Điều hướng chính">
            <a class="cf-nav-item <%= "home".equalsIgnoreCase(activeNav) ? "active" : "" %>" href="<%= ctx %>/home/index.jsp">TRANG CHỦ</a>
            <a class="cf-nav-item <%= "booking".equalsIgnoreCase(activeNav) ? "active" : "" %>" href="<%= ctx %>/booking/booking.jsp">MUA VÉ</a>
            <a class="cf-nav-item <%= "promotions".equalsIgnoreCase(activeNav) ? "active" : "" %>" href="<%= ctx %>/home/index.jsp#promotions">TIN MỚI &amp; ƯU ĐÃI</a>
            <a class="cf-nav-item <%= "cinema".equalsIgnoreCase(activeNav) ? "active" : "" %>" href="<%= ctx %>/cinema/index.jsp">RẠP CHIẾU PHIM</a>
            <a class="cf-nav-item <%= "support".equalsIgnoreCase(activeNav) ? "active" : "" %>" href="<%= ctx %>/home/index.jsp#support">HỖ TRỢ</a>
        </nav>

        <!-- Right Side / Account & Auth Actions -->
        <div class="cf-header-right">
            <% if (headerUser == null) { %>
                <a class="cf-auth-link" href="<%= ctx %>/auth/login.jsp">Đăng nhập</a>
                <a class="cf-auth-btn" href="<%= ctx %>/auth/register.jsp">Đăng ký</a>
            <% } else { %>
                <!-- Modern User Dropdown Menu -->
                <div class="cf-user-menu" id="cfUserMenu">
                    <!-- Trigger pill: Avatar + Name + Chevron -->
                    <button class="cf-user-trigger" id="cfUserTrigger" aria-haspopup="true" aria-expanded="false" aria-label="Tài khoản của tôi">
                        <% if (headerAvatar != null && !headerAvatar.isBlank()) { %>
                            <img class="cf-trigger-avatar" src="<%= safeEscape(headerAvatar) %>" alt="<%= safeEscape(headerMemberName) %>">
                        <% } else { %>
                            <span class="cf-trigger-avatar cf-trigger-initial"><%= headerInitial %></span>
                        <% } %>
                        <span class="cf-trigger-name"><%= safeEscape(headerMemberName.length() > 16 ? headerMemberName.substring(0, 15) + "…" : headerMemberName) %></span>
                        <svg class="cf-trigger-chevron" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
                    </button>

                    <!-- Dropdown Panel -->
                    <div class="cf-dropdown-panel" id="cfDropdownPanel" role="menu">
                        <!-- User Info Card -->
                        <div class="cf-dp-header">
                            <% if (headerAvatar != null && !headerAvatar.isBlank()) { %>
                                <img class="cf-dp-avatar" src="<%= safeEscape(headerAvatar) %>" alt="<%= safeEscape(headerMemberName) %>">
                            <% } else { %>
                                <span class="cf-dp-avatar cf-dp-initial"><%= headerInitial %></span>
                            <% } %>
                            <div class="cf-dp-userinfo">
                                <span class="cf-dp-fullname"><%= safeEscape(headerMemberName) %></span>
                                <span class="cf-dp-account">@<%= safeEscape(headerAccount) %></span>
                            </div>
                            <% if ("ADMIN".equals(headerRole)) { %>
                                <span class="cf-dp-badge admin">Admin</span>
                            <% } else if ("MANAGER".equals(headerRole)) { %>
                                <span class="cf-dp-badge manager">Manager</span>
                            <% } %>
                        </div>

                        <div class="cf-dp-divider"></div>

                        <!-- Nav Items -->
                        <nav class="cf-dp-nav" role="none">
                            <a class="cf-dp-item" href="<%= ctx %>/account" role="menuitem">
                                <span class="cf-dp-item-icon">
                                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 3.6-7 8-7s8 3 8 7"/></svg>
                                </span>
                                Tài khoản của tôi
                            </a>
                            <a class="cf-dp-item" href="<%= ctx %>/account#history" role="menuitem">
                                <span class="cf-dp-item-icon">
                                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/><path d="M8 14h.01M12 14h.01M16 14h.01M8 18h.01M12 18h.01"/></svg>
                                </span>
                                Lịch sử đặt vé
                            </a>
                            <a class="cf-dp-item" href="<%= ctx %>/auth/change-password.jsp" role="menuitem">
                                <span class="cf-dp-item-icon">
                                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                                </span>
                                Đổi mật khẩu
                            </a>
                            <% if ("ADMIN".equals(headerRole)) { %>
                            <a class="cf-dp-item cf-dp-item--admin" href="<%= ctx %>/admin/movies" role="menuitem">
                                <span class="cf-dp-item-icon">
                                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>
                                </span>
                                Bảng quản trị
                            </a>
                            <% } else if ("MANAGER".equals(headerRole)) { %>
                            <a class="cf-dp-item cf-dp-item--manager" href="<%= ctx %>/staff" role="menuitem">
                                <span class="cf-dp-item-icon">
                                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
                                </span>
                                Quản lý cụm rạp
                            </a>
                            <% } %>
                        </nav>

                        <div class="cf-dp-divider"></div>

                        <!-- Logout -->
                        <div class="cf-dp-footer">
                            <form action="<%= ctx %>/logout" method="post">
                                <button type="submit" class="cf-dp-logout" role="menuitem">
                                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
                                    Đăng xuất
                                </button>
                            </form>
                        </div>
                    </div>
                </div>
            <% } %>
        </div>
    </div>
</header>

<script>
(function () {
    var trigger = document.getElementById('cfUserTrigger');
    var panel   = document.getElementById('cfDropdownPanel');
    var menu    = document.getElementById('cfUserMenu');
    if (!trigger || !panel) return;

    function open() {
        panel.classList.add('cf-dp-open');
        trigger.setAttribute('aria-expanded', 'true');
        menu.classList.add('cf-menu-open');
    }
    function close() {
        panel.classList.remove('cf-dp-open');
        trigger.setAttribute('aria-expanded', 'false');
        menu.classList.remove('cf-menu-open');
    }
    trigger.addEventListener('click', function (e) {
        e.stopPropagation();
        panel.classList.contains('cf-dp-open') ? close() : open();
    });
    document.addEventListener('click', function (e) {
        if (!menu.contains(e.target)) close();
    });
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') close();
    });
})();
</script>
