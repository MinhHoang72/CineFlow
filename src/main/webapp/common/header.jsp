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
<div class="top-bar">
    <div class="top-inner">
        <div class="top-left">
            <span>CineFlow</span>
            <span>Hệ thống rạp chiếu phim</span>
        </div>
        <div class="top-right">
            <% if (headerUser == null) { %>
                <a href="<%= ctx %>/auth/login.jsp">Đăng nhập</a>
                <a href="<%= ctx %>/auth/register.jsp">Đăng ký</a>
            <% } else { %>
                <span>Xin chào, <%= safeEscape(headerMemberName) %></span>
                <a href="<%= ctx %>/account">Tài khoản của tôi</a>
                <% if ("ADMIN".equals(headerRole)) { %>
                    <a href="<%= ctx %>/admin/movies">Quản lý phim</a>
                <% } else if ("MANAGER".equals(headerRole)) { %>
                    <a href="<%= ctx %>/staff">Trung tâm của tôi</a>
                <% } %>
                <form class="account-form" action="<%= ctx %>/logout" method="post">
                    <button type="submit">Đăng xuất</button>
                </form>
            <% } %>
            <a href="<%= ctx %>/home/index.jsp#support">Hỗ trợ khách hàng</a>
        </div>
    </div>
</div>
<header class="brand-header">
    <a class="logo" href="<%= ctx %>/home/index.jsp" aria-label="CineFlow Cinema">
        <span class="logo-mark">C</span>
        <span class="logo-word">Cine<span>Flow</span></span>
    </a>
</header>
<nav class="main-nav" aria-label="Điều hướng chính">
    <div class="nav-inner">
        <a class="<%= "home".equalsIgnoreCase(activeNav) ? "active" : "" %>" href="<%= ctx %>/home/index.jsp">TRANG CHỦ</a>
        <a class="<%= "booking".equalsIgnoreCase(activeNav) ? "active" : "" %>" href="<%= ctx %>/booking/booking.jsp">MUA VÉ</a>
        <a class="<%= "promotions".equalsIgnoreCase(activeNav) ? "active" : "" %>" href="<%= ctx %>/home/index.jsp#promotions">TIN MỚI &amp; ƯU ĐÃI</a>
        <a class="<%= "cinema".equalsIgnoreCase(activeNav) ? "active" : "" %>" href="<%= ctx %>/cinema/index.jsp">RẠP CHIẾU PHIM</a>
        <a class="<%= "support".equalsIgnoreCase(activeNav) ? "active" : "" %>" href="<%= ctx %>/home/index.jsp#support">HỖ TRỢ KHÁCH HÀNG</a>
    </div>
</nav>
