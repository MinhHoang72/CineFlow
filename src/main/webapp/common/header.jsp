<%@ page pageEncoding="UTF-8" %>
<%@ page import="model.entity.User" %>
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
<%@ include file="/WEB-INF/fragments/theme.jspf" %>
<header class="cf-main-header" id="cfMainHeader">
    <div class="cf-header-container">
        <!-- Logo thương hiệu -->
        <% String homeBrandLink = "ADMIN".equalsIgnoreCase(headerRole) ? (ctx + "/dashboard") : (ctx + "/home"); %><a class="cf-brand" href="<%= homeBrandLink %>" aria-label="CineFlow Cinema Home">
            <span class="cf-brand-icon">C</span>
            <span class="cf-brand-name">Cine<span class="cf-brand-glow">Flow</span></span>
        </a>

        <!-- Menu điều hướng chính -->
        <nav class="cf-navbar" aria-label="Điều hướng chính">
            <a class="cf-nav-item <%= "home".equalsIgnoreCase(activeNav) ? "active" : "" %>" href="<%= ctx %>/home">TRANG CHỦ</a>
            <a class="cf-nav-item <%= "booking".equalsIgnoreCase(activeNav) ? "active" : "" %>" href="<%= ctx %>/booking">MUA VÉ</a>
            <a class="cf-nav-item <%= "promotions".equalsIgnoreCase(activeNav) ? "active" : "" %>" href="<%= ctx %>/home#promotions">TIN MỚI &amp; ƯU ĐÃI</a>
            <a class="cf-nav-item <%= "cinema".equalsIgnoreCase(activeNav) ? "active" : "" %>" href="<%= ctx %>/cinema">RẠP CHIẾU PHIM</a>
            <a class="cf-nav-item <%= "support".equalsIgnoreCase(activeNav) ? "active" : "" %>" href="<%= ctx %>/support">HỖ TRỢ</a>
        </nav>

        <!-- Khu vực tài khoản và xác thực bên phải -->
        <div class="cf-header-right">
            <% if (headerUser == null) { %>
                <a class="cf-auth-link" href="<%= ctx %>/login">Đăng nhập</a>
                <a class="cf-auth-btn" href="<%= ctx %>/register">Đăng ký</a>
            <% } else { %>
                <% if ("ADMIN".equals(headerRole)) { %>
                    <a class="cf-badge-role admin" href="<%= ctx %>/dashboard" title="Bảng quản trị hệ thống">Admin</a>
                <% } else if ("MANAGER".equals(headerRole)) { %>
                    <a class="cf-badge-role manager" href="<%= ctx %>/staff" title="Quản lý cụm rạp">Quản lý</a>
                <% } %>
                <a class="cf-account-btn" href="<%= ctx %>/account" title="Trang cá nhân của <%= safeEscape(headerMemberName) %>">Tài khoản</a>
                <form class="cf-logout-inline" action="<%= ctx %>/logout" method="post">
                    <button type="submit" class="cf-logout-btn" title="Đăng xuất khỏi hệ thống">
                        Đăng xuất
                    </button>
                </form>
            <% } %>
        </div>
    </div>
</header>
