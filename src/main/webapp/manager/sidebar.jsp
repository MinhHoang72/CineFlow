<%@ page pageEncoding="UTF-8" %>
<% String cfPath = request.getContextPath(); String cfUri = request.getRequestURI(); %>
<aside class="cf-sidebar">
    <a class="cf-sidebar-brand" href="<%= cfPath %>/dashboard">Cine<span>Flow</span></a>
    <div class="cf-sidebar-section">TỔNG QUAN</div>
    <nav class="cf-sidebar-nav"><a class="<%= cfUri.contains("/dashboard.jsp") ? "active" : "" %>" href="<%= cfPath %>/dashboard"><span class="icon">◼</span>Dashboard</a></nav>
    <div class="cf-sidebar-section">NHÂN SỰ</div>
    <nav class="cf-sidebar-nav"><a class="<%= cfUri.contains("/staff") ? "active" : "" %>" href="<%= cfPath %>/staff"><span class="icon">♙</span>Danh sách nhân viên</a></nav>
    <div class="cf-sidebar-section">BẮP NƯỚC</div>
    <nav class="cf-sidebar-nav"><a class="<%= cfUri.endsWith("/fnb") || cfUri.contains("/fnb/item") || cfUri.contains("/fnb/combo") || cfUri.contains("/views/fnb/list") || cfUri.contains("/views/fnb/item") || cfUri.contains("/views/fnb/combo") ? "active" : "" %>" href="<%= cfPath %>/fnb"><span class="icon">🍿</span>Món &amp; Combo</a><a class="<%= cfUri.contains("/fnb/stock") ? "active" : "" %>" href="<%= cfPath %>/fnb/stock"><span class="icon">▣</span>Kho theo Centre</a></nav>
    <div class="cf-sidebar-section">VẬN HÀNH</div>
    <nav class="cf-sidebar-nav"><a class="<%= cfUri.contains("/checkin") ? "active" : "" %>" href="<%= cfPath %>/checkin"><span class="icon">✔</span>Soát vé</a></nav>
    <div class="cf-sidebar-section">BÁO CÁO</div>
    <nav class="cf-sidebar-nav"><a class="<%= cfUri.contains("/report/sales") ? "active" : "" %>" href="<%= cfPath %>/report/sales"><span class="icon">▤</span>Doanh thu &amp; bán hàng</a><a class="<%= cfUri.contains("/report/operations") ? "active" : "" %>" href="<%= cfPath %>/report/operations"><span class="icon">▤</span>Chỉ số vận hành</a></nav>
    <div class="cf-sidebar-section">TÀI KHOẢN</div>
    <nav class="cf-sidebar-nav"><a class="<%= cfUri.contains("/account") ? "active" : "" %>" href="<%= cfPath %>/account"><span class="icon">●</span>Hồ sơ của tôi</a><a href="<%= cfPath %>/logout"><span class="icon">↪</span>Đăng xuất</a></nav>
</aside>

