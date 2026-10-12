<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.Movie,java.util.List,java.util.Set,java.util.TreeSet" %>
<%
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && session.getAttribute("user") instanceof model.User) {
        authRole = ((model.User) session.getAttribute("user")).getRole();
    }
    if (!"ADMIN".equalsIgnoreCase(authRole)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Quản trị viên (ADMIN) mới có quyền truy cập trang này.");
        return;
    }

    List<Movie> movies = (List<Movie>) request.getAttribute("movies");
    if (movies == null) {
        response.sendRedirect(request.getContextPath() + "/admin/movies");
        return;
    }

    request.setAttribute("activeNav", "movies");
    request.setAttribute("pageTitle", "Quản lý phim");
    request.setAttribute("pageBreadcrumb", "Danh mục phim");
    int totalMovies = movies != null ? movies.size() : 0;
    int nowShowingCount = 0;
    int comingSoonCount = 0;
    int endedCount = 0;
    int archivedCount = 0;
    Set<String> allGenres = new TreeSet<>();

    if (movies != null) {
        for (Movie m : movies) {
            String st = m.getStatus();
            if ("NOW_SHOWING".equals(st)) nowShowingCount++;
            else if ("COMING_SOON".equals(st)) comingSoonCount++;
            else if ("ENDED".equals(st)) endedCount++;
            else if ("ARCHIVED".equals(st)) archivedCount++;

            if (m.getGenre() != null && !m.getGenre().isBlank()) {
                String[] parts = m.getGenre().split("[,/]");
                for (String p : parts) {
                    String clean = p.trim();
                    if (!clean.isEmpty()) allGenres.add(clean);
                }
            }
        }
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Danh mục phim | CineFlow Admin</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/reset.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/variables.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/buttons.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/badges.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/forms.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/tables.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/shell.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/dashboard.css">
</head>
<body>
<div class="cf-manager-shell">
    <jsp:include page="/admin/sidebar.jsp" />

    <div class="cf-manager-main">
        <jsp:include page="/admin/topbar.jsp" />

        <main class="cf-content">
            <!-- PAGE HEADER WITH ADD MOVIE ACTION -->
            <div class="cf-page-header">
                <div>
                    <h1>Quản lý phim</h1>
                    <p>Danh sách và quản lý thông tin các bộ phim được phát hành trên toàn hệ thống CineFlow.</p>
                </div>
                <a href="${pageContext.request.contextPath}/admin/movie" class="cf-btn cf-btn-primary">
                    + Thêm phim mới
                </a>
            </div>

            <!-- KPI METRICS STRIP -->
            <div class="cf-kpi-grid">
                <div class="cf-kpi-card">
                    <span class="cf-kpi-label">Tổng số phim</span>
                    <div class="cf-kpi-value"><%= totalMovies %></div>
                    <span class="cf-kpi-sub">Kho phim lưu trữ hệ thống</span>
                </div>

                <div class="cf-kpi-card">
                    <span class="cf-kpi-label" style="color: var(--cf-success);">Đang chiếu</span>
                    <div class="cf-kpi-value"><%= nowShowingCount %></div>
                    <span class="cf-kpi-sub">Đang mở bán vé tại rạp</span>
                </div>

                <div class="cf-kpi-card">
                    <span class="cf-kpi-label" style="color: var(--cf-info);">Sắp khởi chiếu</span>
                    <div class="cf-kpi-value"><%= comingSoonCount %></div>
                    <span class="cf-kpi-sub">Kế hoạch truyền thông & mở bán</span>
                </div>

                <div class="cf-kpi-card">
                    <span class="cf-kpi-label" style="color: var(--cf-warning);">Đã kết thúc / Lưu trữ</span>
                    <div class="cf-kpi-value"><%= endedCount + archivedCount %></div>
                    <span class="cf-kpi-sub">Đã ngừng lịch chiếu</span>
                </div>
            </div>

            <!-- MAIN DATA PANEL (VERTICAL TABLE ONLY) -->
            <section class="cf-card">
                <!-- TOOLBAR: SEARCH & FILTERS -->
                <div class="cf-card-toolbar">
                    <div class="cf-toolbar-left">
                        <div class="cf-search-box">
                            <input type="text" id="movieSearchInput" placeholder="Tìm theo tên, thể loại, đạo diễn, mã phim...">
                        </div>

                        <div class="cf-filter-pills">
                            <button type="button" class="cf-pill active" data-status="ALL">Tất cả (<%= totalMovies %>)</button>
                            <button type="button" class="cf-pill" data-status="NOW_SHOWING">Đang chiếu (<%= nowShowingCount %>)</button>
                            <button type="button" class="cf-pill" data-status="COMING_SOON">Sắp chiếu (<%= comingSoonCount %>)</button>
                            <button type="button" class="cf-pill" data-status="ENDED">Đã kết thúc (<%= endedCount %>)</button>
                            <button type="button" class="cf-pill" data-status="ARCHIVED">Lưu trữ (<%= archivedCount %>)</button>
                        </div>
                    </div>

                    <div class="cf-toolbar-right">
                        <select id="movieGenreSelect" class="cf-select-sm">
                            <option value="ALL">Tất cả thể loại</option>
                            <% for (String g : allGenres) { %>
                                <option value="<%= g %>"><%= g %></option>
                            <% } %>
                        </select>

                        <div style="display: flex; align-items: center; gap: 6px; font-size: 12px; color: var(--cf-muted);">
                            <span>Hiển thị:</span>
                            <select id="moviePageSizeSelect" class="cf-select-sm">
                                <option value="8">8 phim</option>
                                <option value="12" selected>12 phim</option>
                                <option value="24">24 phim</option>
                                <option value="50">50 phim</option>
                                <option value="ALL">Tất cả</option>
                            </select>
                        </div>
                    </div>
                </div>

                <!-- TABLE WRAPPER (CHUỖI DỌC) -->
                <div class="cf-table-wrap" id="movieTableWrapper" style="margin: 0; border: none; border-radius: 0;">
                    <table class="cf-table">
                        <thead>
                            <tr>
                                <th style="width: 340px;">Phim & Mã số</th>
                                <th>Thể loại</th>
                                <th>Độ tuổi</th>
                                <th>Thời lượng</th>
                                <th>Khởi chiếu</th>
                                <th>Trạng thái</th>
                                <th style="text-align: right; min-width: 170px;">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody id="movieTableBody">
                            <% if (movies != null) {
                                for (Movie m : movies) {
                                    String st = m.getStatus();
                                    String stText = "NOW_SHOWING".equals(st) ? "Đang chiếu" :
                                                    "COMING_SOON".equals(st) ? "Sắp chiếu" :
                                                    "ENDED".equals(st) ? "Đã kết thúc" : "Lưu trữ";
                                    String badgeClass = "NOW_SHOWING".equals(st) ? "cf-badge-success" :
                                                        "COMING_SOON".equals(st) ? "cf-badge-info" :
                                                        "ENDED".equals(st) ? "cf-badge-warning" : "cf-badge-muted";
                                    String age = m.getAgeRating() != null ? m.getAgeRating() : "P";
                                    String genre = m.getGenre() != null ? m.getGenre() : "Chưa cập nhật";
                                    String director = m.getDirector() != null ? m.getDirector() : "";
                            %>
                            <tr data-movie-id="<%= m.getMovieId() %>"
                                data-title="<%= m.getTitle() != null ? m.getTitle().replace("\"", "&quot;") : "" %>"
                                data-genre="<%= genre.replace("\"", "&quot;") %>"
                                data-director="<%= director.replace("\"", "&quot;") %>"
                                data-status="<%= st %>">
                                <td>
                                    <div style="display: flex; align-items: center; gap: 12px;">
                                        <div class="movie-thumb">
                                            <% if (m.getPosterUrl() != null && !m.getPosterUrl().isBlank()) { %>
                                                <img src="<%= m.getPosterUrl() %>" alt="<%= m.getTitle() %>" loading="lazy" onerror="this.parentElement.innerHTML='🎬';">
                                            <% } else { %>
                                                🎬
                                            <% } %>
                                        </div>
                                        <div style="min-width: 0;">
                                            <a href="${pageContext.request.contextPath}/admin/movie?movieId=<%= m.getMovieId() %>" style="font-weight: 700; color: var(--cf-text); text-decoration: none; font-size: 13.5px; display: block; line-height: 1.3;" onmouseover="this.style.color='var(--cf-primary)'" onmouseout="this.style.color='var(--cf-text)'">
                                                <%= m.getTitle() %>
                                            </a>
                                            <div style="font-size: 12px; color: var(--cf-muted); margin-top: 3px; line-height: 1.3;">
                                                Mã phim: <%= m.getMovieId() %><% if (!director.isBlank()) { %> · Đạo diễn: <%= director %><% } %>
                                            </div>
                                        </div>
                                    </div>
                                </td>
                                <td><%= genre %></td>
                                <td>
                                    <span class="cf-badge" style="background: #edf1f4; color: var(--cf-text); font-weight: 700;"><%= age %></span>
                                </td>
                                <td><strong><%= m.getDuration() %></strong> phút</td>
                                <td><%= m.getReleaseDate() != null ? m.getReleaseDate() : "—" %></td>
                                <td>
                                    <span class="cf-badge <%= badgeClass %>">
                                        <%= stText %>
                                    </span>
                                </td>
                                <td style="text-align: right;">
                                    <div style="display: flex; align-items: center; justify-content: flex-end; gap: 6px;">
                                        <a href="${pageContext.request.contextPath}/admin/movie?movieId=<%= m.getMovieId() %>" class="cf-btn cf-btn-secondary cf-btn-sm" title="Chỉnh sửa thông tin phim">
                                            Sửa
                                        </a>
                                        <a href="${pageContext.request.contextPath}/admin/distribution?movieId=<%= m.getMovieId() %>" class="cf-btn cf-btn-primary cf-btn-sm" title="Phân phối rạp chiếu">
                                            Phân phối
                                        </a>
                                    </div>
                                </td>
                            </tr>
                            <% }} %>
                        </tbody>
                    </table>
                </div>

                <!-- EMPTY STATE -->
                <div id="movieEmptyState" style="display: none; padding: 48px 24px; text-align: center; color: var(--cf-muted);">
                    <div style="font-size: 32px; margin-bottom: 8px;">🔍</div>
                    <h3 style="margin: 0 0 6px; color: var(--cf-text); font-size: 16px;">Không tìm thấy phim phù hợp</h3>
                    <p style="margin: 0; font-size: 13px;">Thử tìm kiếm với từ khóa khác hoặc bấm "Tất cả" trên bộ lọc trạng thái.</p>
                </div>

                <!-- PAGINATION BAR -->
                <div class="cf-pagination-bar">
                    <div class="cf-pagination-info" id="moviePaginationInfo">
                        Đang nạp danh mục phim...
                    </div>
                    <div class="cf-pagination-controls" id="moviePaginationControls">
                        <!-- Dynamic page buttons -->
                    </div>
                </div>
            </section>
        </main>
    </div>
</div>

<script src="${pageContext.request.contextPath}/js/admin-pagination.js?v=<%= System.currentTimeMillis() %>"></script>
<script>
    document.addEventListener('DOMContentLoaded', function() {
        initAdminPagination({
            tableBodyId: 'movieTableBody',
            paginationId: 'moviePaginationControls',
            searchInputId: 'movieSearchInput',
            statusPillSelector: '.cf-pill[data-status]',
            genreSelectId: 'movieGenreSelect',
            pageSizeSelectId: 'moviePageSizeSelect',
            infoDisplayId: 'moviePaginationInfo',
            tableWrapperId: 'movieTableWrapper',
            emptyStateId: 'movieEmptyState'
        });
    });
</script>
</body>
</html>
