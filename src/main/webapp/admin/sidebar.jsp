<%@ page pageEncoding="UTF-8" %>
<%
    String cfPath = request.getContextPath();
    String cfUri = request.getRequestURI();
    String activeNav = (String) request.getAttribute("activeNav");
    if (activeNav == null) {
        if (cfUri.contains("/admin/distribution")) {
            activeNav = "distribution";
        } else if (cfUri.contains("/admin/centres") || cfUri.contains("/admin/centre")) {
            activeNav = "centres";
        } else if (cfUri.contains("/admin/movies") || cfUri.contains("/admin/movie")) {
            activeNav = "movies";
        } else if (cfUri.contains("/admin/customers")) {
            activeNav = "customers";
        } else if (cfUri.contains("/admin/managers")) {
            activeNav = "managers";
        } else if (cfUri.contains("/admin/transfers")) {
            activeNav = "transfers";
        } else if (cfUri.contains("/reports")) {
            activeNav = "reports";
        } else if (cfUri.contains("/staff")) {
            activeNav = "staff";
        } else if (cfUri.contains("/account") || cfUri.contains("profile.jsp")) {
            activeNav = "account";
        } else if (cfUri.contains("/dashboard")) {
            activeNav = "dashboard";
        } else {
            activeNav = "";
        }
    }
%>
<aside class="cf-sidebar" id="adminSidebar">
    <a class="cf-sidebar-brand" href="<%= cfPath %>/dashboard">Cine<span>Flow</span></a>

    <div class="cf-sidebar-section">TỔNG QUAN</div>
    <nav class="cf-sidebar-nav">
        <a class="<%= "dashboard".equals(activeNav) || cfUri.endsWith("/dashboard") || cfUri.contains("/dashboard.jsp") ? "active" : "" %>" href="<%= cfPath %>/dashboard">
            <span class="icon">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path><polyline points="9 22 9 12 15 12 15 22"></polyline></svg>
            </span>Dashboard
        </a>
    </nav>

    <div class="cf-sidebar-section">QUẢN TRỊ NỘI DUNG</div>
    <nav class="cf-sidebar-nav">
        <a class="<%= "centres".equals(activeNav) ? "active" : "" %>" href="<%= cfPath %>/admin/centres">
            <span class="icon">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 21h18M3 7v14M21 7v14M6 7V3h12v4M9 11h2M13 11h2M9 15h2M13 15h2"/></svg>
            </span>Quản lý Cụm rạp
        </a>
        <a class="<%= "movies".equals(activeNav) ? "active" : "" %>" href="<%= cfPath %>/admin/movies">
            <span class="icon">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="2" width="20" height="20" rx="2.18" ry="2.18"></rect><line x1="7" y1="2" x2="7" y2="22"></line><line x1="17" y1="2" x2="17" y2="22"></line><line x1="2" y1="12" x2="22" y2="12"></line><line x1="2" y1="7" x2="7" y2="7"></line><line x1="2" y1="17" x2="7" y2="17"></line><line x1="17" y1="17" x2="22" y2="17"></line><line x1="17" y1="7" x2="22" y2="7"></line></svg>
            </span>Danh mục phim
        </a>
        <a class="<%= "distribution".equals(activeNav) ? "active" : "" %>" href="<%= cfPath %>/admin/distribution">
            <span class="icon">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="3" width="20" height="14" rx="2" ry="2"></rect><line x1="8" y1="21" x2="16" y2="21"></line><line x1="12" y1="17" x2="12" y2="21"></line></svg>
            </span>Phân phối rạp chiếu
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
            <span class="icon">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
            </span>Tài khoản Khách hàng
        </a>
        <a class="<%= "managers".equals(activeNav) || cfUri.contains("/admin/managers") ? "active" : "" %>" href="<%= cfPath %>/admin/managers">
            <span class="icon">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
            </span>Tài khoản Quản lý
        </a>
        <a class="<%= "transfers".equals(activeNav) || cfUri.contains("/admin/transfers") ? "active" : "" %>" href="<%= cfPath %>/admin/transfers">
            <span class="icon">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="17 1 21 5 17 9"></polyline><path d="M3 11V9a4 4 0 0 1 4-4h14"></path><polyline points="7 23 3 19 7 15"></polyline><path d="M21 13v2a4 4 0 0 1-4 4H3"></path></svg>
            </span>Bổ nhiệm & Điều chuyển
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
