<%@ page pageEncoding="UTF-8" %>
<%
    String cfPath = request.getContextPath();
    String cfUri = request.getRequestURI();
%>
<%
    String cfRole = (String) session.getAttribute("role");
    if (cfRole == null && session.getAttribute("user") instanceof model.User) {
        cfRole = ((model.User) session.getAttribute("user")).getRole();
    }
    boolean isManagerOrAdmin = "MANAGER".equalsIgnoreCase(cfRole) || "ADMIN".equalsIgnoreCase(cfRole);
%>
<aside class="cf-sidebar">
    <a class="cf-sidebar-brand" href="<%= cfPath %>/dashboard">Cine<span>Flow</span></a>

    <div class="cf-sidebar-section">TỔNG QUAN</div>
    <nav class="cf-sidebar-nav">
        <a class="<%= cfUri.contains("/dashboard") ? "active" : "" %>" href="<%= cfPath %>/dashboard">
            <span class="icon">◼</span>Dashboard
        </a>
    </nav>

    <% if (isManagerOrAdmin) { %>
    <div class="cf-sidebar-section">NHÂN SỰ</div>
    <nav class="cf-sidebar-nav">
        <a class="<%= cfUri.contains("/staff") ? "active" : "" %>" href="<%= cfPath %>/staff">
            <span class="icon">♙</span>Danh sách nhân viên
        </a>
    </nav>

    <div class="cf-sidebar-section">BÁO CÁO</div>
    <nav class="cf-sidebar-nav">
        <a class="<%= cfUri.contains("/reports") ? "active" : "" %>" href="<%= cfPath %>/reports">
            <span class="icon">▤</span>Phân tích hoạt động
        </a>
    </nav>
    <% } %>

    <div class="cf-sidebar-section">TÀI KHOẢN</div>
    <nav class="cf-sidebar-nav">
        <a class="<%= cfUri.contains("/account") ? "active" : "" %>" href="<%= cfPath %>/account">
            <span class="icon">●</span>Hồ sơ của tôi
        </a>
        <a href="<%= cfPath %>/logout">
            <span class="icon">↪</span>Đăng xuất
        </a>
    </nav>
</aside>
