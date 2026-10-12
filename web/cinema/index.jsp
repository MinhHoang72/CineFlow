<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="dao.CinemaDAO,model.Branch,java.util.List,java.util.Map,java.util.LinkedHashMap" %>
<%!
    private String esc(String value) { return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
%>
<%
    String contextPath = request.getContextPath();
    List<Branch> branches = new CinemaDAO().getActiveBranches();
    Map<String, List<Branch>> branchesByCity = new LinkedHashMap<>();
    for (Branch branch : branches) {
        String city = branch.getCity() == null || branch.getCity().trim().isEmpty() ? "Khu vực khác" : branch.getCity().trim();
        branchesByCity.computeIfAbsent(city, key -> new java.util.ArrayList<>()).add(branch);
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Hệ thống rạp CineFlow</title>
    <link rel="stylesheet" href="<%= contextPath %>/css/cinema/cinemas.css">
</head>
<body>
<div class="cinema-page">
    <div class="cinema-utility"><div class="utility-inner"><div class="utility-left"><span class="utility-badge">CF</span><span>CineFlow Cinema</span><span class="utility-divider">|</span><span>Hệ thống rạp chiếu phim</span></div><div class="utility-right"><a href="<%= contextPath %>/auth/login.jsp">Đăng nhập</a><a href="<%= contextPath %>/auth/register.jsp">Thẻ thành viên</a><a href="<%= contextPath %>/home/index.jsp#support">Hỗ trợ khách hàng</a></div></div></div>
    <header class="cinema-brand-row"><a class="cinema-logo" href="<%= contextPath %>/home/index.jsp"><span class="cinema-mark">C</span><span class="logo-word">Cine<span>Flow</span></span></a><p>ĐIỆN ẢNH TRỌN VẸN · KHOẢNH KHẮC ĐÁNG NHỚ</p></header>
    <nav class="cinema-main-nav"><div class="nav-inner"><a href="<%= contextPath %>/home/index.jsp">TRANG CHỦ</a><a href="<%= contextPath %>/booking/booking.jsp">MUA VÉ</a><a href="<%= contextPath %>/home/index.jsp#promotions">TIN MỚI &amp; ƯU ĐÃI</a><a class="active" href="<%= contextPath %>/cinema/index.jsp">RẠP CHIẾU PHIM</a></div></nav>
    <% if (!branchesByCity.isEmpty()) { %>
    <nav class="region-bar" aria-label="Chọn thành phố"><div class="region-inner"><span class="region-label">KHU VỰC</span><% for (String city : branchesByCity.keySet()) { %><button type="button" class="region-tab" data-city="<%= esc(city) %>"><%= esc(city) %></button><% } %></div></nav>
    <% } %>
    <a class="cinema-promo" href="<%= contextPath %>/home/index.jsp#promotions" aria-label="Xem tin mới và ưu đãi CineFlow"><div class="promo-copy"><span class="promo-kicker">CINEFLOW PRESENTS</span><strong>Phim hay.<br>Khoảnh khắc lớn.</strong><span>Khám phá lịch chiếu tại hệ thống rạp CineFlow</span><i>XEM TIN MỚI &amp; ƯU ĐÃI <b>→</b></i></div><div class="promo-art"><span class="promo-orbit orbit-one"></span><span class="promo-orbit orbit-two"></span><span class="promo-reel">CF</span><span class="promo-spark spark-one">✦</span><span class="promo-spark spark-two">✦</span></div></a>
    <main class="cinema-content">
        <div class="page-heading"><div><p class="eyebrow">CINEFLOW CINEMAS</p><h1>Chọn rạp chiếu phim</h1><p class="intro">Chọn thành phố để khám phá các rạp và lịch chiếu gần bạn.</p></div><span class="heading-mark">C</span></div>
        <% if (branchesByCity.isEmpty()) { %><p class="empty-state">Chưa có rạp đang hoạt động.</p><% } else { %>
            <div class="branch-panels">
                <% for (Map.Entry<String, List<Branch>> entry : branchesByCity.entrySet()) { %>
                    <section class="branch-panel" data-city-panel="<%= esc(entry.getKey()) %>" hidden>
                        <div class="city-title"><div><p class="eyebrow">ĐIỂM ĐẾN CINEFLOW</p><h2>Rạp tại <%= esc(entry.getKey()) %></h2></div><span class="city-count"><%= entry.getValue().size() %> RẠP</span></div>
                        <div class="cinema-grid">
                            <% for (Branch branch : entry.getValue()) { %><a class="cinema-card" href="<%= contextPath %>/booking/booking.jsp?branchId=<%= branch.getBranchId() %>&amp;view=schedule"><div class="cinema-card-visual"><span class="visual-line"></span><span class="visual-mark">C</span><span class="visual-caption">CINEFLOW<br>CINEMA</span></div><div class="cinema-card-info"><p class="card-kicker">CINEFLOW CINEMA</p><h3><%= esc(branch.getBranchName()) %></h3><p class="branch-address"><span>⌖</span><%= esc(branch.getAddress()) %></p><div class="cinema-card-footer"><span class="branch-city"><%= esc(branch.getCity()) %></span><span class="schedule-link">XEM LỊCH CHIẾU <b>→</b></span></div></div></a><% } %>
                        </div>
                    </section>
                <% } %>
            </div>
        <% } %>
    </main>
</div>
<script>
    document.querySelectorAll('.region-tab').forEach(function (tab) {
        tab.addEventListener('click', function () {
            var city = tab.dataset.city;
            document.querySelectorAll('.region-tab').forEach(function (item) { item.classList.toggle('active', item === tab); });
            document.querySelectorAll('.branch-panel').forEach(function (panel) { panel.hidden = panel.dataset.cityPanel !== city; });
            var selectedPanel = Array.from(document.querySelectorAll('.branch-panel')).find(function (panel) { return panel.dataset.cityPanel === city; });
            if (selectedPanel) selectedPanel.scrollIntoView({ behavior: 'smooth', block: 'start' });
        });
    });
</script>
</body>
</html>
