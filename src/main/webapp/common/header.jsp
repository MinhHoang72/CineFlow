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
    String headerRole = headerUser == null ? "" : (headerUser.getRole() == null ? "" : headerUser.getRole().toUpperCase());
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
                <div class="cf-user-chip">
                    <span class="cf-user-avatar"><%= headerMemberName.isEmpty() ? "U" : headerMemberName.substring(0, 1).toUpperCase() %></span>
                    <span class="cf-user-greeting">Chào, <strong><%= safeEscape(headerMemberName) %></strong></span>
                    <a class="cf-account-link" href="<%= ctx %>/account" title="Trang cá nhân">Tài khoản</a>
                    <% if ("ADMIN".equals(headerRole)) { %>
                        <a class="cf-badge-role admin" href="<%= ctx %>/admin/movies" title="Bảng quản trị hệ thống">Admin</a>
                    <% } else if ("MANAGER".equals(headerRole)) { %>
                        <a class="cf-badge-role manager" href="<%= ctx %>/staff" title="Quản lý cụm rạp">Quản lý</a>
                    <% } %>
                    <form class="cf-logout-inline" action="<%= ctx %>/logout" method="post">
                        <button type="submit" class="cf-logout-btn" title="Đăng xuất khỏi hệ thống">
                            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
                        </button>
                    </form>
                </div>
            <% } %>
        </div>
    </div>
</header>
