<%@ page pageEncoding="UTF-8" %>
<%
    String cfPath = request.getContextPath();
    String cfUri = request.getRequestURI();
    String activeNav = (String) request.getAttribute("activeNav");
    if (activeNav == null) {
        if (cfUri.contains("/admin/movies") || cfUri.contains("/admin/movie") || cfUri.contains("/admin/distribution")) activeNav = "movies";
        else if (cfUri.contains("/admin/customers")) activeNav = "customers";
        else if (cfUri.contains("/reports")) activeNav = "reports";
        else if (cfUri.contains("/staff")) activeNav = "staff";
        else if (cfUri.contains("/account") || cfUri.contains("profile.jsp")) activeNav = "account";
        else if (cfUri.contains("/dashboard")) activeNav = "dashboard";
        else activeNav = "";
    }
%>
<aside class="cf-sidebar" id="adminSidebar">
    <a class="cf-sidebar-brand" href="<%= cfPath %>/dashboard">Cine<span>Flow</span></a>

    <div class="cf-sidebar-section">TỔNG QUAN</div>
    <nav class="cf-sidebar-nav">
        <a class="<%= "dashboard".equals(activeNav) || cfUri.endsWith("/dashboard") || cfUri.contains("/dashboard.jsp") ? "active" : "" %>" href="<%= cfPath %>/dashboard">
            <span class="icon">◼</span>Dashboard
        </a>
    </nav>

    <div class="cf-sidebar-section">QUẢN TRỊ NỘI DUNG</div>
    <nav class="cf-sidebar-nav">
        <a class="<%= ("movies".equals(activeNav) || cfUri.contains("/admin/movies") || cfUri.contains("/admin/movie") || cfUri.contains("/admin/distribution")) ? "active" : "" %>" href="<%= cfPath %>/admin/movies">
            <span class="icon">🎬</span>Danh mục phim
        </a>
    </nav>

    <div class="cf-sidebar-section">BẮP NƯỚC</div>
    <nav class="cf-sidebar-nav">
        <a class="<%= "fnb".equals(activeNav) ? "active" : "" %>" href="<%= cfPath %>/fnb">
            <span class="icon">🍿</span>Món &amp; Combo
        </a>
    </nav>

    <div class="cf-sidebar-section">KHUYẾN MÃI</div>
    <nav class="cf-sidebar-nav">
        <a class="<%= "vouchers".equals(activeNav) || cfUri.contains("/admin/vouchers") ? "active" : "" %>" href="<%= cfPath %>/admin/vouchers">
            <span class="icon">🎟</span>Voucher
        </a>
    </nav>

    <div class="cf-sidebar-section">QUẢN LÝ NGƯỜI DÙNG</div>
    <nav class="cf-sidebar-nav">
        <a class="<%= "customers".equals(activeNav) || cfUri.contains("/admin/customers") ? "active" : "" %>" href="<%= cfPath %>/admin/customers">
            <span class="icon">👥</span>Tài khoản Khách hàng
        </a>
    </nav>

    <div class="cf-sidebar-section">QUẢN LÝ NHÂN SỰ</div>
    <nav class="cf-sidebar-nav">
        <a class="<%= "managers".equals(activeNav) || cfUri.contains("/admin/managers") ? "active" : "" %>" href="<%= cfPath %>/admin/managers">
            <span class="icon">👔</span>Tài khoản Quản lý
        </a>
        <a class="<%= "transfers".equals(activeNav) || cfUri.contains("/admin/transfers") ? "active" : "" %>" href="<%= cfPath %>/admin/transfers">
            <span class="icon">🔄</span>Bổ nhiệm & Điều chuyển
        </a>
    </nav>

    <div class="cf-sidebar-section">BÁO CÁO & PHÂN TÍCH</div>
    <nav class="cf-sidebar-nav">
        <a class="<%= cfUri.contains("/report/chain") ? "active" : "" %>" href="<%= cfPath %>/report/chain">
            <span class="icon">📈</span>Dashboard chuỗi
        </a>
        <a class="<%= cfUri.contains("/report/sales") ? "active" : "" %>" href="<%= cfPath %>/report/sales">
            <span class="icon">💰</span>Doanh thu &amp; bán hàng
        </a>
        <a class="<%= cfUri.contains("/report/operations") ? "active" : "" %>" href="<%= cfPath %>/report/operations">
            <span class="icon">⚙</span>Chỉ số vận hành
        </a>
    </nav>

    <div class="cf-sidebar-section">TÀI KHOẢN</div>
    <nav class="cf-sidebar-nav">
        <a class="<%= "account".equals(activeNav) || cfUri.contains("/account") || cfUri.contains("profile.jsp") ? "active" : "" %>" href="<%= cfPath %>/account">
            <span class="icon">👤</span>Hồ sơ của tôi
        </a>
        <a href="<%= cfPath %>/logout">
            <span class="icon">↪</span>Đăng xuất
        </a>
    </nav>
</aside>
