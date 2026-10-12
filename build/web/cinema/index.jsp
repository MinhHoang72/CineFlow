<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="dao.CinemaDAO,model.Branch,model.User" %>
<%@ page import="java.util.List,java.util.Map,java.util.LinkedHashMap,java.util.ArrayList" %>
<%!
    private User sessionUser(Object value) {
        if (value instanceof User) {
            return (User) value;
        }
        if (value == null) {
            return null;
        }
        try {
            User compatible = new User();
            Class<?> type = value.getClass();
            compatible.setFullName((String) type.getMethod("getFullName").invoke(value));
            compatible.setAccount((String) type.getMethod("getAccount").invoke(value));
            compatible.setRole((String) type.getMethod("getRole").invoke(value));
            return compatible;
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            return null;
        }
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    String contextPath = request.getContextPath();

    // Backward-compatibility: if branchId is supplied to index.jsp, redirect to details.jsp
    String branchIdParam = request.getParameter("branchId");
    if (branchIdParam != null && !branchIdParam.trim().isEmpty()) {
        response.sendRedirect(contextPath + "/cinema/details.jsp?branchId=" + java.net.URLEncoder.encode(branchIdParam.trim(), "UTF-8"));
        return;
    }

    User signedInUser = sessionUser(session.getAttribute("user"));
    String memberName = signedInUser == null ? "" : signedInUser.getFullName();
    if (memberName == null || memberName.trim().isEmpty()) {
        memberName = signedInUser == null ? "" : signedInUser.getAccount();
    }

    CinemaDAO cinemaDAO = new CinemaDAO();
    List<Branch> branches = cinemaDAO.getActiveBranches();

    // Group active branches by City from DB
    Map<String, List<Branch>> branchesByCity = new LinkedHashMap<>();
    for (Branch b : branches) {
        String city = b.getCity() == null || b.getCity().trim().isEmpty() ? "Khu vực khác" : b.getCity().trim();
        branchesByCity.computeIfAbsent(city, k -> new ArrayList<>()).add(b);
    }

    String selectedCity = request.getParameter("city");
    if (selectedCity != null) {
        selectedCity = selectedCity.trim();
    } else {
        selectedCity = "";
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="theme-color" content="#e21b23">
    <title>Hệ thống rạp chiếu phim | CineFlow</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Barlow+Condensed:wght@500;600;700&family=Be+Vietnam+Pro:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="<%= contextPath %>/css/home/index.css?v=<%= System.currentTimeMillis() %>">
    <link rel="stylesheet" href="<%= contextPath %>/css/cinema/cinemas.css?v=<%= System.currentTimeMillis() %>">
    <style>
        :root {
            --red: #e21b23;
            --red-dark: #bd1118;
            --gold: #c9b27a;
            --gold-dark: #b9a66c;
            --cream: #eee9d8;
            --cream-deep: #d9d0b4;
            --ink: #252525;
            --muted: #77736b;
            --line: #e5e1d7;
            --display: "Barlow Condensed", Impact, sans-serif;
            --body: "Be Vietnam Pro", "Segoe UI", sans-serif;
        }
        * { box-sizing: border-box; }
        html { scroll-behavior: smooth; }
        body { min-width: 320px; margin: 0; background: #fff; color: var(--ink); font-family: var(--body); }
        button, input, select { font-family: var(--body); }
        a { color: inherit; text-decoration: none; }
        button { cursor: pointer; }

        .page-shell { width: 100%; margin: 0 auto; }

        /* Top Bar */
        .top-bar { height: 38px; background: #f0eee4; border-bottom: 1px solid #ddd7c5; color: #555; font-size: 12px; }
        .top-inner { width: min(1250px, calc(100% - 40px)); height: 100%; display: flex; align-items: center; justify-content: space-between; margin: auto; }
        .top-left, .top-right { display: flex; align-items: center; gap: 24px; }
        .top-right a:hover { color: var(--red); }
        .account-form { display: inline-flex; margin: 0; }
        .account-form button { padding: 0; border: 0; background: transparent; color: inherit; cursor: pointer; font: inherit; }
        .account-form button:hover { color: var(--red); }

        /* Brand Header */
        .brand-header { height: 105px; display: flex; align-items: center; justify-content: center; border-bottom: 3px solid #d3c28d; background: #fff; }
        .logo { display: flex; align-items: center; gap: 10px; color: var(--red); font-size: 39px; font-weight: 800; text-decoration: none; }
        .logo-mark { width: 48px; height: 48px; display: grid; place-items: center; transform: rotate(-8deg); border-radius: 50% 50% 50% 8px; background: var(--red); color: #fff; font-family: Georgia, serif; font-size: 29px; }
        .logo-word span { color: #252525; }

        /* Main Navigation */
        .main-nav { border-bottom: 1px solid #ddd; background: #fff; }
        .nav-inner { width: min(1250px, 100%); min-height: 72px; display: flex; align-items: center; justify-content: center; margin: auto; }
        .nav-inner a { height: 27px; display: flex; align-items: center; padding: 0 24px; border-right: 1px solid #ddd; color: #404040; font-size: 14px; white-space: nowrap; text-decoration: none; font-weight: 600; }
        .nav-inner a:last-child { border-right: 0; }
        .nav-inner a:hover, .nav-inner a.active { color: var(--red); font-weight: 700; }

        /* Breadcrumb */
        .cinema-breadcrumb-wrap { background: #fdfaf3; border-bottom: 1px solid #eee7d5; }
        .breadcrumb-inner { width: min(1250px, calc(100% - 40px)); height: 40px; display: flex; align-items: center; gap: 8px; margin: 0 auto; font-size: 13px; color: #78716c; }
        .breadcrumb-inner a:hover { color: var(--red); }
        .breadcrumb-sep { color: #a8a29e; }
        .breadcrumb-current { color: var(--ink); font-weight: 700; }

        /* Search Section */
        .search-section { display: flex; align-items: center; justify-content: space-between; gap: 40px; padding: 35px max(20px, calc((100% - 1250px) / 2)); background: #f2f0e8; border-bottom: 1px solid #e2ded0; }
        .search-section h2 { margin: 0 0 7px; font-size: 21px; font-weight: 700; color: var(--ink); }
        .search-section p { margin: 0; color: #777; font-size: 13px; }
        .search-box { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
        .search-box input, .search-box select { height: 44px; padding: 0 13px; border: 1px solid #ccc; background: #fff; border-radius: 2px; }
        .search-box input { width: 220px; }
        .search-box select { width: 180px; }
        .search-box button { min-height: 44px; display: inline-flex; align-items: center; justify-content: center; padding: 0 22px; border: 0; background: var(--red); color: #fff; cursor: pointer; font-size: 13px; font-weight: 700; border-radius: 2px; }
        .search-box button:hover { background: var(--red-dark); }

        /* Cinema Section */
        .cinema-section { width: min(1250px, calc(100% - 40px)); margin: 40px auto 60px; }
        .cinema-section > .section-title { margin: 0; }
        .section-title { height: 50px; display: flex; align-items: center; justify-content: center; gap: 25px; background: #eee9d8; margin-bottom: 28px; }
        .section-title span { width: 90px; height: 1px; background: #b9a66c; }
        .section-title h2 { margin: 0; font-size: 16px; font-weight: 700; color: var(--ink); letter-spacing: 0.5px; }

        /* Branch Grid & Cards */
        .branch-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 22px; margin-top: 28px; }
        .branch-card { display: flex; align-items: flex-start; gap: 18px; padding: 24px; border: 1px solid #dedede; background: #fff; transition: transform .2s ease, box-shadow .2s ease, border-color .2s ease; }
        .branch-card:hover { transform: translateY(-3px); box-shadow: 0 8px 22px rgba(0,0,0,.08); border-color: #c9b27a; }
        .cinema-icon { width: 55px; height: 55px; display: grid; flex: 0 0 55px; place-items: center; background: #f0ece0; color: var(--red); font-size: 24px; border-radius: 4px; }
        .branch-info { flex: 1; min-width: 0; }
        .branch-city-tag { display: inline-block; padding: 2px 8px; border-radius: 3px; background: #eee9d8; color: #555; font-size: 11px; font-weight: 700; margin-bottom: 6px; }
        .branch-info h3 { margin: 0 0 8px; font-size: 17px; font-weight: 700; color: #252525; }
        .branch-address { margin: 0 0 8px; color: #666; font-size: 13px; line-height: 1.45; }
        .branch-meta { margin: 0 0 14px; color: #777; font-size: 12px; display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
        .branch-meta b { color: #ddd; font-weight: normal; }
        .branch-actions { display: flex; align-items: center; gap: 10px; }
        .branch-actions .btn-book { padding: 7px 16px; background: var(--red); color: #fff; font-size: 12px; font-weight: 700; border-radius: 3px; transition: background .15s ease; text-decoration: none; }
        .branch-actions .btn-book:hover { background: var(--red-dark); }
        .branch-actions .btn-detail { color: var(--red); font-size: 12px; font-weight: 700; padding: 6px 12px; border: 1px solid #ddd; border-radius: 3px; transition: all .15s ease; text-decoration: none; }
        .branch-actions .btn-detail:hover { border-color: var(--red); background: #fff5f5; }

        /* Empty state */
        .empty-state { grid-column: 1 / -1; padding: 48px; border: 1px dashed #cec8b8; color: var(--muted); text-align: center; font-size: 14px; background: #fdfdfd; }

        /* Quick rail */
        .quick-rail { position: fixed; z-index: 10; top: 43%; right: 0; width: 92px; border: 1px solid #ddd; background: #fff; }
        .quick-rail a { min-height: 78px; display: flex; align-items: center; justify-content: center; flex-direction: column; gap: 5px; border-bottom: 1px solid #e3e0d8; color: #4f4b43; font-size: 22px; text-align: center; }
        .quick-rail a:hover { color: var(--red); }
        .quick-icon { font-size: 23px; line-height: 1; }
        .quick-rail a span:last-child { font-size: 11px; }
        .quick-rail .top { min-height: 35px; background: var(--cream-deep); font-size: 10px; }

        /* Footer */
        .footer { padding: 45px 0 0; background: #202020; color: #ccc; font-size: 12px; }
        .footer-inner { width: min(1250px, calc(100% - 50px)); display: grid; grid-template-columns: 2fr 1fr 1fr 1fr; gap: 40px; margin: auto; padding-bottom: 45px; }
        .footer-brand .logo { color: var(--red); font-size: 26px; }
        .footer-brand .logo-mark { width: 34px; height: 34px; font-size: 20px; }
        .footer-brand p { margin-top: 15px; color: #999; }
        .footer h4 { margin: 0 0 18px; color: #fff; font-size: 13px; }
        .footer a { display: block; margin-bottom: 11px; color: #999; }
        .footer a:hover { color: #fff; }
        .copyright { padding: 18px; border-top: 1px solid #333; color: #777; text-align: center; font-size: 11px; }

        @media (max-width: 1050px) {
            .branch-grid { grid-template-columns: repeat(2, 1fr); }
            .footer-inner { grid-template-columns: 1fr 1fr; }
        }
        @media (max-width: 768px) {
            .search-section { flex-direction: column; align-items: stretch; gap: 15px; }
            .search-box { flex-direction: column; width: 100%; }
            .search-box input, .search-box select, .search-box button { width: 100%; }
            .branch-grid { grid-template-columns: 1fr; }
            .quick-rail { display: none; }
            .nav-inner { overflow-x: auto; justify-content: flex-start; }
        }
        @media (max-width: 480px) {
            .footer-inner { grid-template-columns: 1fr; }
        }
    </style>
</head>
<body id="top">
<div class="page-shell">

    <!-- Top Utility Bar (Đồng bộ với home/index.jsp) -->
    <div class="top-bar">
        <div class="top-inner">
            <div class="top-left">
                <span>CineFlow</span>
                <span>Hệ thống rạp chiếu phim</span>
            </div>
            <div class="top-right">
                <% if (signedInUser == null) { %>
                    <a href="<%= contextPath %>/auth/login.jsp">Đăng nhập</a>
                    <a href="<%= contextPath %>/auth/register.jsp">Đăng ký</a>
                <% } else { %>
                    <span>Xin chào, <%= escapeHtml(memberName) %></span>
                    <a href="<%= contextPath %>/account">Tài khoản của tôi</a>
                    <% if ("ADMIN".equals(signedInUser.getRole())) { %>
                        <a href="<%= contextPath %>/admin/movies">Quản lý phim</a>
                    <% } else if ("MANAGER".equals(signedInUser.getRole())) { %>
                        <a href="<%= contextPath %>/staff">Trung tâm của tôi</a>
                    <% } %>
                    <form class="account-form" action="<%= contextPath %>/logout" method="post">
                        <button type="submit">Đăng xuất</button>
                    </form>
                <% } %>
                <a href="<%= contextPath %>/home/index.jsp#support">Hỗ trợ khách hàng</a>
            </div>
        </div>
    </div>

    <!-- Brand Header (Đồng bộ với home/index.jsp) -->
    <header class="brand-header">
        <a class="logo" href="<%= contextPath %>/home/index.jsp" aria-label="CineFlow Cinema">
            <span class="logo-mark">C</span>
            <span class="logo-word">Cine<span>Flow</span></span>
        </a>
    </header>

    <!-- Main Navigation Bar (Đồng bộ với home/index.jsp) -->
    <nav class="main-nav" aria-label="Điều hướng chính">
        <div class="nav-inner">
            <a href="<%= contextPath %>/home/index.jsp">TRANG CHỦ</a>
            <a href="<%= contextPath %>/booking/booking.jsp">MUA VÉ</a>
            <a href="<%= contextPath %>/home/index.jsp#promotions">TIN MỚI &amp; ƯU ĐÃI</a>
            <a class="active" href="<%= contextPath %>/cinema/index.jsp">RẠP CHIẾU PHIM</a>
            <a href="<%= contextPath %>/home/index.jsp#support">HỖ TRỢ KHÁCH HÀNG</a>
        </div>
    </nav>

    <!-- Breadcrumb -->
    <div class="cinema-breadcrumb-wrap">
        <div class="breadcrumb-inner">
            <a href="<%= contextPath %>/home/index.jsp">Trang chủ</a>
            <span class="breadcrumb-sep">›</span>
            <span class="breadcrumb-current">Hệ thống rạp chiếu phim</span>
        </div>
    </div>

    <!-- Thanh tìm kiếm chuẩn dự án (.search-section) -->
    <section class="search-section">
        <div>
            <h2>TÌM RẠP CINEFLOW</h2>
            <p>Hệ thống rạp chiếu phim CineFlow hiện đại trên toàn quốc</p>
        </div>
        <div class="search-box">
            <input type="text" 
                   id="cinemaSearchInput" 
                   placeholder="Tên rạp hoặc địa chỉ..." 
                   oninput="filterCinemas()">
            <select id="citySelect" onchange="filterCinemas()">
                <option value="">Tất cả thành phố (<%= branches.size() %>)</option>
                <% for (Map.Entry<String, List<Branch>> entry : branchesByCity.entrySet()) { %>
                    <option value="<%= escapeHtml(entry.getKey()) %>" <%= entry.getKey().equalsIgnoreCase(selectedCity) ? "selected" : "" %>>
                        <%= escapeHtml(entry.getKey()) %> (<%= entry.getValue().size() %>)
                    </option>
                <% } %>
            </select>
            <button type="button" onclick="clearFilter()">Đặt lại</button>
        </div>
    </section>

    <!-- Danh sách cụm rạp chuẩn dự án (.cinema-section & .branch-grid) -->
    <main class="cinema-section">
        <div class="section-title">
            <span></span>
            <h2>HỆ THỐNG RẠP CINEFLOW</h2>
            <span></span>
        </div>

        <% if (branches.isEmpty()) { %>
            <p class="empty-state">Hệ thống hiện chưa có rạp chiếu phim nào hoạt động.</p>
        <% } else { %>
            <div class="branch-grid" id="branchGrid">
                <% for (Branch branch : branches) { 
                    String city = branch.getCity() == null ? "" : branch.getCity().trim();
                    String phone = branch.getPhone() != null && !branch.getPhone().trim().isEmpty() ? branch.getPhone().trim() : "1900 2099";
                    boolean matchesCity = selectedCity.isEmpty() || city.equalsIgnoreCase(selectedCity);
                %>
                    <article class="branch-card" 
                             data-city="<%= escapeHtml(city) %>"
                             data-name="<%= escapeHtml(branch.getBranchName()).toLowerCase() %>"
                             data-address="<%= escapeHtml(branch.getAddress()).toLowerCase() %>"
                             style="<%= matchesCity ? "" : "display: none;" %>">
                        <div class="cinema-icon">🏢</div>
                        <div class="branch-info">
                            <span class="branch-city-tag"><%= escapeHtml(city) %></span>
                            <h3><%= escapeHtml(branch.getBranchName()) %></h3>
                            <p class="branch-address"><%= escapeHtml(branch.getAddress()) %></p>
                            <p class="branch-meta">
                                <span>📞 <%= escapeHtml(phone) %></span>
                                <b>|</b>
                                <span>🏛️ <%= branch.getHallCount() %> phòng chiếu</span>
                                <b>|</b>
                                <span>💺 <%= branch.getSeatCount() %> ghế</span>
                            </p>
                            <div class="branch-actions">
                                <a class="btn-book" href="<%= contextPath %>/booking/booking.jsp?branchId=<%= branch.getBranchId() %>">Mua vé</a>
                                <a class="btn-detail" href="<%= contextPath %>/cinema/details.jsp?branchId=<%= branch.getBranchId() %>">Chi tiết rạp →</a>
                            </div>
                        </div>
                    </article>
                <% } %>
            </div>
            <p class="empty-state" id="noResultsMsg" style="display: none;">Không tìm thấy rạp chiếu phim phù hợp với điều kiện tìm kiếm.</p>
        <% } %>
    </main>

    <!-- Quick Floating Rail (Đồng bộ với home/index.jsp) -->
    <nav class="quick-rail" aria-label="Tiện ích nhanh">
        <a href="<%= contextPath %>/booking/booking.jsp"><span class="quick-icon">🎟</span><span>Đặt vé</span></a>
        <a href="<%= contextPath %>/home/index.jsp#support"><span class="quick-icon">❔</span><span>Hỗ trợ</span></a>
        <a class="top" href="#top"><span class="quick-icon">↑</span><span>Đầu trang</span></a>
    </nav>

    <!-- Footer chuẩn dự án (Đồng bộ với home/index.jsp) -->
    <footer class="footer">
        <div class="footer-inner">
            <div class="footer-brand">
                <a class="logo" href="<%= contextPath %>/home/index.jsp">
                    <span class="logo-mark">C</span>
                    <span class="logo-word">Cine<span>Flow</span></span>
                </a>
                <p>Hệ thống rạp chiếu phim CineFlow.</p>
            </div>
            <div>
                <h4>CINEFLOW</h4>
                <a href="<%= contextPath %>/home/index.jsp#promotions">Tin mới &amp; ưu đãi</a>
                <a href="<%= contextPath %>/cinema/index.jsp">Rạp chiếu phim</a>
                <a href="<%= contextPath %>/home/index.jsp#movies">Phim đang chiếu</a>
                <a href="<%= contextPath %>/booking/booking.jsp?view=schedule">Lịch chiếu</a>
            </div>
            <div id="support">
                <h4>HỖ TRỢ</h4>
                <a href="<%= contextPath %>/home/index.jsp#support">Hỗ trợ khách hàng</a>
                <a href="<%= contextPath %>/home/index.jsp#support">Câu hỏi thường gặp</a>
                <a href="<%= contextPath %>/home/index.jsp#support">Liên hệ</a>
            </div>
            <div>
                <h4>TÀI KHOẢN</h4>
                <% if (signedInUser == null) { %>
                    <a href="<%= contextPath %>/auth/login.jsp">Đăng nhập</a>
                    <a href="<%= contextPath %>/auth/register.jsp">Tạo tài khoản</a>
                <% } else { %>
                    <a href="<%= contextPath %>/account">Tài khoản của tôi</a>
                <% } %>
            </div>
        </div>
        <div class="copyright">© 2026 CineFlow. All rights reserved.</div>
    </footer>

</div>

<!-- Client-side Search & Filter Script -->
<script>
    function filterCinemas() {
        var query = (document.getElementById('cinemaSearchInput').value || '').trim().toLowerCase();
        var selectedCity = (document.getElementById('citySelect').value || '').trim().toLowerCase();
        var cards = document.querySelectorAll('#branchGrid .branch-card');
        var matchCount = 0;

        cards.forEach(function(card) {
            var cardCity = (card.dataset.city || '').toLowerCase();
            var cardName = card.dataset.name || '';
            var cardAddress = card.dataset.address || '';

            var cityMatches = !selectedCity || cardCity === selectedCity;
            var queryMatches = !query || cardName.indexOf(query) !== -1 || cardAddress.indexOf(query) !== -1 || cardCity.indexOf(query) !== -1;

            if (cityMatches && queryMatches) {
                card.style.display = '';
                matchCount++;
            } else {
                card.style.display = 'none';
            }
        });

        var noResults = document.getElementById('noResultsMsg');
        if (noResults) {
            noResults.style.display = (matchCount === 0) ? 'block' : 'none';
        }

        // Update URL query state
        if (window.history && window.history.replaceState) {
            var rawCity = document.getElementById('citySelect').value;
            var newUrl = window.location.pathname + (rawCity ? '?city=' + encodeURIComponent(rawCity) : '');
            window.history.replaceState(null, '', newUrl);
        }
    }

    function clearFilter() {
        var searchInput = document.getElementById('cinemaSearchInput');
        if (searchInput) searchInput.value = '';
        var citySelect = document.getElementById('citySelect');
        if (citySelect) citySelect.value = '';
        filterCinemas();
    }

    document.addEventListener('DOMContentLoaded', function() {
        filterCinemas();
    });
</script>
</body>
</html>
