<%@ page pageEncoding="UTF-8" %>
<% String cfPath = request.getContextPath(); String cfUri = request.getRequestURI(); %>
<aside class="cf-sidebar">
    <a class="cf-sidebar-brand" href="<%= cfPath %>/dashboard">Cine<span>Flow</span></a>
    <div class="cf-sidebar-section">TỔNG QUAN</div>
    <nav class="cf-sidebar-nav"><a class="<%= cfUri.contains("/dashboard.jsp") ? "active" : "" %>" href="<%= cfPath %>/dashboard"><span class="icon">◼</span>Dashboard</a></nav>
    <div class="cf-sidebar-section">NHÂN SỰ</div>
    <nav class="cf-sidebar-nav"><a class="<%= cfUri.contains("/staff") ? "active" : "" %>" href="<%= cfPath %>/staff"><span class="icon">♙</span>Danh sách nhân viên</a></nav>
    <div class="cf-sidebar-section">BÁO CÁO</div>
    <nav class="cf-sidebar-nav"><a class="<%= cfUri.contains("/manager/reports") ? "active" : "" %>" href="<%= cfPath %>/reports"><span class="icon">▤</span>Phân tích hoạt động</a></nav>
    <div class="cf-sidebar-section">TÀI KHOẢN</div>
    <nav class="cf-sidebar-nav"><a class="<%= cfUri.contains("/account") ? "active" : "" %>" href="<%= cfPath %>/account"><span class="icon">●</span>Hồ sơ của tôi</a><a href="<%= cfPath %>/logout"><span class="icon">↪</span>Đăng xuất</a></nav>
</aside>

