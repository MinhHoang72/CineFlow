<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    String cfPath = request.getContextPath();
    String cfUri = request.getRequestURI();
%>
<%
    String cfRole = (String) session.getAttribute("role");
    if (cfRole == null && session.getAttribute("user") instanceof model.entity.User) {
        cfRole = ((model.entity.User) session.getAttribute("user")).getRole();
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
    <div class="cf-sidebar-section">PHÒNG CHIẾU</div>
    <nav class="cf-sidebar-nav">
        <a class="<%= cfUri.contains("/halls") ? "active" : "" %>" href="<%= cfPath %>/manager/halls">
            <span class="icon">🎬</span>Quản lý phòng chiếu
        </a>
    </nav>

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
        <a class="<%= cfUri.contains("/report/sales") ? "active" : "" %>" href="<%= cfPath %>/report/sales">
            <span class="icon">💰</span>Doanh thu &amp; bán hàng
        </a>
        <a class="<%= cfUri.contains("/report/operations") ? "active" : "" %>" href="<%= cfPath %>/report/operations">
            <span class="icon">⚙</span>Chỉ số vận hành
        </a>
    </nav>
    <% } %>

    <% if ("MANAGER".equalsIgnoreCase(cfRole) || "TICKET_SALES_STAFF".equalsIgnoreCase(cfRole)) { %>
    <div class="cf-sidebar-section">BẮP NƯỚC</div>
    <nav class="cf-sidebar-nav">
        <a class="<%= cfUri.endsWith("/fnb") || cfUri.contains("/fnb/item") || cfUri.contains("/fnb/combo") ? "active" : "" %>" href="<%= cfPath %>/fnb">
            <span class="icon">🍿</span>Món &amp; Combo
        </a>
        <a class="<%= cfUri.contains("/fnb/stock") ? "active" : "" %>" href="<%= cfPath %>/fnb/stock">
            <span class="icon">▣</span>Kho theo Centre
        </a>
    </nav>
    <% } %>

    <% if ("MANAGER".equalsIgnoreCase(cfRole) || "TICKET_CHECKER_STAFF".equalsIgnoreCase(cfRole)) { %>
    <div class="cf-sidebar-section">VẬN HÀNH</div>
    <nav class="cf-sidebar-nav">
        <a class="<%= cfUri.contains("/checkin") ? "active" : "" %>" href="<%= cfPath %>/checkin">
            <span class="icon">✔</span>Soát vé
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
