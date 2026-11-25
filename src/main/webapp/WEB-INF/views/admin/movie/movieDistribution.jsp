<%--
    Trang JSP: Phân phối phim tới các cụm rạp chiếu phim.
    Controller phục vụ: DistributionServlet (GET/POST /admin/distribution).
    Luồng hoạt động:
    - Bước 1: Chọn bộ phim cần cấu hình phân phối (chuyển trang ?movieId=...).
    - Bước 2: Tích chọn hoặc bỏ tích các cụm rạp trong bảng -> gửi mảng checkbox "centreIds" về Server.
    - Chỉ hiển thị và phân phối các phim Đang chiếu (NOW_SHOWING) và Sắp chiếu (COMING_SOON).
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.entity.Movie, model.entity.Centre, java.util.List, java.util.Set" %>
<%
    // 1. Kiểm tra quyền Quản trị viên (ADMIN)
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && session.getAttribute("user") instanceof model.entity.User) {
        authRole = ((model.entity.User) session.getAttribute("user")).getRole();
    }
    if (!"ADMIN".equalsIgnoreCase(authRole)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Quản trị viên (ADMIN) mới có quyền truy cập trang này.");
        return;
    }

    // 2. Lấy dữ liệu sạch do DistributionServlet nạp sẵn từ MovieService
    Movie movie = (Movie) request.getAttribute("movie");
    List<Movie> allMovies = (List<Movie>) request.getAttribute("allMovies");
    List<Centre> centres = (List<Centre>) request.getAttribute("centres");
    Set<String> cities = (Set<String>) request.getAttribute("cities");
    int totalDistributed = request.getAttribute("totalDistributed") != null ? (Integer) request.getAttribute("totalDistributed") : 0;
    int totalCentres = request.getAttribute("totalCentres") != null ? (Integer) request.getAttribute("totalCentres") : 0;
    if (allMovies == null) allMovies = java.util.Collections.emptyList();
    if (centres == null) centres = java.util.Collections.emptyList();
    if (cities == null) cities = java.util.Collections.emptySet();
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Phân phối rạp chiếu | CineFlow Admin</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/reset.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/variables.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/buttons.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/badges.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/alerts.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/shell.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/dashboard.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/distribution.css?v=<%= System.currentTimeMillis() %>">
</head>
<body>
<div class="cf-manager-shell">
    <jsp:include page="/WEB-INF/fragments/adminSidebar.jspf" />

    <div class="cf-manager-main">
        <jsp:include page="/WEB-INF/fragments/adminTopbar.jspf" />

        <main class="cf-content">
            <!-- TIÊU ĐỀ TRANG -->
            <div class="cf-page-header" style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px;">
                <div style="display: flex; align-items: center; gap: 14px;">
                    <div class="cf-dist-icon-box">
                        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <rect x="2" y="3" width="20" height="14" rx="2" ry="2"></rect>
                            <line x1="8" y1="21" x2="16" y2="21"></line>
                            <line x1="12" y1="17" x2="12" y2="21"></line>
                        </svg>
                    </div>
                    <div>
                        <h1 style="margin: 0; font-size: 22px; font-weight: 700; color: #ffffff;">Phân phối rạp chiếu</h1>
                        <p style="margin: 4px 0 0; color: var(--cf-muted, #94a3b8); font-size: 13px;">Gán phim vào các cụm rạp trong hệ thống để rạp có quyền tạo suất chiếu.</p>
                    </div>
                </div>
            </div>

            <!-- THÔNG BÁO LƯU THÀNH CÔNG HOẶC LỖI -->
            <% if ("1".equals(request.getParameter("saved"))) { %>
                <div id="saveSuccessAlert" class="cf-alert cf-alert-success" style="margin-bottom: 20px;">
                    ✓ Cập nhật cấu hình phân phối rạp chiếu thành công.
                </div>
            <% } else if (request.getParameter("error") != null) { %>
                <div class="cf-alert cf-alert-danger" style="margin-bottom: 20px;">
                    ⚠ Không thể lưu phân phối phim. Vui lòng kiểm tra lại trạng thái phim hoặc thử lại sau.
                </div>
            <% } %>

            <!-- BƯỚC 1: CHỌN PHIM -->
            <section class="cf-dist-section">
                <div class="cf-step-header">
                    <div class="cf-step-title-wrap">
                        <div class="cf-step-badge">1</div>
                        <h2 class="cf-step-title">Chọn phim</h2>
                    </div>
                </div>

                <form action="${pageContext.request.contextPath}/admin/distribution" method="GET" style="margin-bottom: 16px;">
                    <select name="movieId"
                            class="cf-filter-select"
                            style="width: 100%; height: 44px; font-size: 14px;"
                            onchange="this.form.submit()"
                            <%= allMovies.isEmpty() ? "disabled" : "" %>>
                        <% if (allMovies.isEmpty()) { %>
                            <option value="">Chưa có phim nào trong hệ thống</option>
                        <% } else {
                            for (Movie m : allMovies) {
                                boolean isSelected = (movie != null && m.getMovieId() == movie.getMovieId());
                                String stText = "NOW_SHOWING".equals(m.getStatus()) ? "Đang chiếu" : "Sắp khởi chiếu";
                        %>
                        <option value="<%= m.getMovieId() %>" <%= isSelected ? "selected" : "" %>>
                            [Mã phim: <%= m.getMovieId() %>] <%= m.getTitle() %> — <%= stText %>
                        </option>
                        <% }} %>
                    </select>
                </form>

                <!-- THẺ THÔNG TIN PHIM ĐƯỢC CHỌN -->
                <% if (movie != null) {
                    String st = movie.getStatus();
                    String stText = "NOW_SHOWING".equals(st) ? "Đang chiếu" : "Sắp khởi chiếu";
                    String badgeClass = "NOW_SHOWING".equals(st) ? "cf-badge-success" : "cf-badge-info";
                %>
                <div class="cf-selected-movie-card">
                    <div class="cf-movie-card-poster">
                        <% if (movie.getPosterUrl() != null && !movie.getPosterUrl().isBlank()) { %>
                            <img src="<%= movie.getPosterUrl() %>" alt="<%= movie.getTitle() %>" loading="lazy" onerror="this.parentElement.innerHTML='🎬';">
                        <% } else { %>
                            🎬
                        <% } %>
                    </div>
                    <div class="cf-movie-card-details">
                        <div class="cf-movie-card-title-row">
                            <h3 class="cf-movie-card-title"><%= movie.getTitle() %></h3>
                            <span class="cf-badge <%= badgeClass %>"><%= stText %></span>
                        </div>
                        <div class="cf-movie-card-meta">
                            Mã phim: <%= movie.getMovieId() %> · Thể loại: <%= movie.getGenre() != null ? movie.getGenre() : "Chưa cập nhật" %>
                        </div>
                        <div class="cf-movie-card-meta" style="margin-top: 3px;">
                            Thời lượng: <%= movie.getDuration() %> phút · Khởi chiếu: <%= movie.getReleaseDate() != null ? movie.getReleaseDate() : "—" %>
                        </div>
                    </div>
                    <div class="cf-movie-card-action">
                        <a href="${pageContext.request.contextPath}/admin/movie?movieId=<%= movie.getMovieId() %>" class="cf-btn-view-movie">
                            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <circle cx="12" cy="12" r="10"></circle>
                                <line x1="12" y1="16" x2="12" y2="12"></line>
                                <line x1="12" y1="8" x2="12.01" y2="8"></line>
                            </svg>
                            Xem thông tin phim
                        </a>
                    </div>
                </div>
                <% } %>
            </section>

            <%--
                BƯỚC 2: Form POST gửi lên /admin/distribution.
                - Gửi movieId (ẩn) và danh sách mảng checkbox "centreIds" của các rạp được tích chọn.
                - Khi bỏ tích rạp, CentreID đó sẽ không có trong mảng gửi lên -> Server cập nhật bản ghi phân phối sang CANCELLED.
            --%>
            <% if (movie != null) { %>
            <form id="distributionForm" action="${pageContext.request.contextPath}/admin/distribution" method="POST">
                <input type="hidden" name="movieId" value="<%= movie.getMovieId() %>">
                <section class="cf-dist-section">
                    <div class="cf-step-header">
                        <div class="cf-step-title-wrap">
                            <div class="cf-step-badge">2</div>
                            <h2 class="cf-step-title">Chọn cụm rạp phân phối</h2>
                        </div>
                        <div class="cf-step-counter">
                            Đã chọn <strong id="selectedCountNum" style="color: #ffffff;"><%= totalDistributed %></strong>/<%= totalCentres %> cụm rạp
                        </div>
                    </div>

                    <!-- BỘ LỌC TÌM KIẾM RẠP -->
                    <div class="cf-dist-toolbar">
                        <div class="cf-search-input-wrap">
                            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                                <circle cx="11" cy="11" r="8"></circle>
                                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                            </svg>
                            <input type="text" id="centreSearchInput" placeholder="Tìm cụm rạp theo tên hoặc thành phố...">
                        </div>

                        <select id="cityFilterSelect" class="cf-filter-select">
                            <option value="ALL">Tất cả thành phố</option>
                            <% for (String cName : cities) { %>
                                <option value="<%= cName %>"><%= cName %></option>
                            <% } %>
                        </select>

                        <select id="statusFilterSelect" class="cf-filter-select">
                            <option value="ALL">Tất cả trạng thái</option>
                            <option value="ACTIVE">Đang hoạt động</option>
                            <option value="SUSPENDED">Tạm ngưng</option>
                        </select>

                        <button type="button" id="btnToggleAllCentres" class="cf-btn-toggle-all" title="Bấm để phân phối tới toàn bộ cụm rạp">                        
                            <span id="btnToggleAllText">Phân phối tất cả</span>
                        </button>
                    </div>
                    <!-- BẢNG DANH SÁCH CỤM RẠP -->
                    <div class="cf-dist-table-wrap">
                        <table class="cf-dist-table">
                            <thead>
                                <tr>
                                    <th>Cụm rạp</th>
                                    <th>Thành phố</th>
                                    <th>Trạng thái rạp</th>
                                    <th style="width: 100px; text-align: center;">Thao tác</th>
                                </tr>
                            </thead>
                            <tbody id="centreTableBody">
                                <% for (Centre c : centres) {
                                    String rawStatus = c.getStatus() != null ? c.getStatus().trim().toUpperCase() : "ACTIVE";
                                    boolean isActive = "ACTIVE".equals(rawStatus);
                                    boolean isDistributed = c.isDistributed();
                                    String cCity = c.getCity() != null ? c.getCity() : "";
                                %>
                                <tr class="cf-centre-row"
                                    data-name="<%= c.getCentreName().toLowerCase() %>"
                                    data-city="<%= cCity.toLowerCase() %>"
                                    data-city-raw="<%= cCity %>"
                                    data-status="<%= rawStatus %>">
                                    <td>
                                        <div class="cf-centre-name-cell">
                                             <span><%= c.getCentreName() %></span>
                                        </div>
                                    </td>
                                    <td style="color: #94a3b8;">
                                        <%= !cCity.isBlank() ? cCity : "—" %>
                                    </td>
                                    <td>
                                        <% if (isActive) { %>
                                            <span class="cf-badge-active">Hoạt động</span>
                                        <% } else { %>
                                            <span class="cf-badge-maintenance">Tạm ngưng</span>
                                        <% } %>
                                    </td>
                                    <td style="text-align: center;">
                                        <div class="cf-switch-cell-wrap">
                                            <label class="cf-checkbox-custom" title="Tích chọn để phân phối">
                                                <input type="checkbox"
                                                       name="centreIds"
                                                       value="<%= c.getCentreId() %>"
                                                       class="cf-centre-checkbox"
                                                       <%= isDistributed ? "checked" : "" %>>
                                            </label>
                                        </div>
                                    </td>
                                </tr>
                                <% } %>
                            </tbody>
                        </table>
                    </div>

                    <!-- NÚT LƯU PHÂN PHỐI -->
                    <div class="cf-dist-actions-bar">
                        <button type="button" class="cf-btn-cancel" onclick="location.reload();">
                            Hủy thay đổi
                        </button>
                        <button type="submit" class="cf-btn-save">
                            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
                                <polyline points="17 21 17 13 7 13 7 21"></polyline>
                                <polyline points="7 3 7 8 15 8"></polyline>
                            </svg>
                            Lưu phân phối
                        </button>
                    </div>
                </section>
            </form>
            <% } %>
        </main>
    </div>
</div>

<script src="${pageContext.request.contextPath}/js/distribution.js?v=<%= System.currentTimeMillis() %>"></script>
</body>
</html>
