<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.entity.Movie,java.util.List,java.util.Set,java.util.TreeSet" %>
<%
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && session.getAttribute("user") instanceof model.entity.User) {
        authRole = ((model.entity.User) session.getAttribute("user")).getRole();
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
<%!
    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
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
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/alerts.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/shell.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/dashboard.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/movies.css?v=<%= System.currentTimeMillis() %>">
</head>
<body>
<div class="cf-manager-shell">
    <jsp:include page="/WEB-INF/fragments/adminSidebar.jspf" />

    <div class="cf-manager-main">
        <jsp:include page="/WEB-INF/fragments/adminTopbar.jspf" />

        <main class="cf-content">
            <!-- TIÊU ĐỀ TRANG VÀ NÚT THÊM PHIM MỚI -->
            <div class="cf-page-header" style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px;">
                <div style="display: flex; align-items: center; gap: 14px;">
                    <div style="width: 44px; height: 44px; background: rgba(255, 40, 85, 0.12); border: 1px solid rgba(255, 40, 85, 0.25); border-radius: 10px; display: grid; place-items: center; font-size: 22px;">🎬</div>
                    <div>
                        <h1 style="margin: 0; font-size: 22px; font-weight: 700; color: #ffffff;">Danh mục phim</h1>
                        <p style="margin: 4px 0 0; color: var(--cf-muted, #94a3b8); font-size: 13px;">Quản lý thông tin phim trong hệ thống. Thêm mới, chỉnh sửa, tìm kiếm và cập nhật trạng thái phim.</p>
                    </div>
                </div>
                <!-- Thêm phim mới -->
                <a href="${pageContext.request.contextPath}/admin/movie" class="cf-btn cf-btn-primary" style="background: var(--cf-primary, #ff2855); font-weight: 600; padding: 10px 20px; border-radius: 8px; border: none; box-shadow: 0 4px 14px rgba(255, 40, 85, 0.3);">
                    Thêm phim mới
                </a>
            </div>

            <!-- THÔNG BÁO ALERT -->
            <% if ("statusUpdated".equals(request.getParameter("success"))) { %>
                <div class="cf-alert cf-alert-success" style="margin-bottom: 20px;">
                    ✓ Cập nhật trạng thái phim thành công.
                </div>
            <% } else if ("saved".equals(request.getParameter("success"))) { %>
                <div class="cf-alert cf-alert-success" style="margin-bottom: 20px;">
                    ✓ Đã lưu thông tin phim thành công vào hệ thống.
                </div>
            <% } else if ("invalid_status".equals(request.getParameter("error"))) { %>
                <div class="cf-alert cf-alert-danger" style="margin-bottom: 20px;">
                    ⚠ Trạng thái cập nhật không hợp lệ.
                </div>
            <% } %>

            <!-- CÁC THẺ CHỈ SỐ KPI THỐNG KÊ PHIM -->
            <div class="cf-kpi-grid cf-kpi-grid-5">
                <div class="cf-kpi-card">
                    <span class="cf-kpi-label">Tổng số phim</span>
                    <div class="cf-kpi-value"><%= totalMovies %></div>
                    <span class="cf-kpi-sub">Tất cả phim trong hệ thống</span>
                </div>

                <div class="cf-kpi-card">
                    <span class="cf-kpi-label" style="color: #0284c7;">Sắp khởi chiếu</span>
                    <div class="cf-kpi-value"><%= comingSoonCount %></div>
                    <span class="cf-kpi-sub">Phim đang chờ phát hành</span>
                </div>

                <div class="cf-kpi-card">
                    <span class="cf-kpi-label" style="color: #059669;">Đang chiếu</span>
                    <div class="cf-kpi-value"><%= nowShowingCount %></div>
                    <span class="cf-kpi-sub">Phim có trạng thái đang chiếu</span>
                </div>

                <div class="cf-kpi-card">
                    <span class="cf-kpi-label" style="color: #d97706;">Đã kết thúc</span>
                    <div class="cf-kpi-value"><%= endedCount %></div>
                    <span class="cf-kpi-sub">Phim đã dừng chiếu nhưng chưa lưu trữ</span>
                </div>

                <div class="cf-kpi-card">
                    <span class="cf-kpi-label" style="color: #64748b;">Lưu trữ</span>
                    <div class="cf-kpi-value"><%= archivedCount %></div>
                    <span class="cf-kpi-sub">Phim đã được lưu (Admin)</span>
                </div>
            </div>

            <!-- BẢNG DANH MỤC PHIM VÀ BỘ LỌC ĐA NĂNG -->
            <section class="cf-card">
                <!-- THANH CÔNG CỤ: TÌM KIẾM VÀ BỘ LỌC -->
                <div class="cf-card-toolbar-multi">
                    <!-- HÀNG 1: TÌM KIẾM TỪ KHÓA, MÃ PHIM VÀ PILLS TRẠNG THÁI -->
                    <div class="cf-toolbar-row">
                        <div class="cf-toolbar-left">
                            <div class="cf-search-box">
                                <input type="text" id="movieSearchInput" placeholder="Tìm theo tên, thể loại, đạo diễn...">
                            </div>

                            <div class="cf-code-search-box">
                                <span class="cf-code-icon">#</span>
                                <input type="text" id="movieIdInput" placeholder="Mã phim (vd: 18, CF-0018)...">
                            </div>
                        </div>

                        <div class="cf-filter-pills">
                            <button type="button" class="cf-pill active" data-status="ALL">Tất cả phim (<%= totalMovies %>)</button>
                            <button type="button" class="cf-pill" data-status="NOW_SHOWING">Đang chiếu (<%= nowShowingCount %>)</button>
                            <button type="button" class="cf-pill" data-status="COMING_SOON">Sắp khởi chiếu (<%= comingSoonCount %>)</button>
                            <button type="button" class="cf-pill" data-status="ENDED">Đã kết thúc (<%= endedCount %>)</button>
                            <button type="button" class="cf-pill" data-status="ARCHIVED">Lưu trữ (<%= archivedCount %>)</button>
                        </div>
                    </div>

                    <!-- HÀNG 2: BỘ LỌC NÂNG CAO (THỂ LOẠI, ĐỘ TUỔI, NGÀY KHỞI CHIẾU, RESET, PHÂN TRANG) -->
                    <div class="cf-toolbar-row cf-toolbar-row-filters">
                        <div class="cf-toolbar-left cf-filters-wrap">
                            <!-- Lọc Thể loại -->
                            <select id="movieGenreSelect" class="cf-select-sm" title="Lọc theo thể loại phim">
                                <option value="ALL">Tất cả thể loại</option>
                                <% for (String g : allGenres) { %>
                                    <option value="<%= g %>"><%= g %></option>
                                <% } %>
                            </select>

                            <!-- Lọc Độ tuổi (Age Rating) -->
                            <select id="movieAgeSelect" class="cf-select-sm" title="Lọc theo độ tuổi khán giả">
                                <option value="ALL">Tất cả độ tuổi</option>
                                <option value="P">P - Mọi độ tuổi</option>
                                <option value="K">K - Dưới 13 tuổi (có người lớn)</option>
                                <option value="T13">T13 - Khán giả từ 13 tuổi</option>
                                <option value="T16">T16 - Khán giả từ 16 tuổi</option>
                                <option value="T18">T18 - Khán giả từ 18 tuổi</option>
                            </select>

                            <!-- Lọc Ngày khởi chiếu (Mốc nhanh) -->
                            <select id="movieReleasePresetSelect" class="cf-select-sm" title="Lọc theo mốc ngày khởi chiếu">
                                <option value="ALL">Tất cả ngày chiếu</option>
                                <option value="RELEASED">Đã khởi chiếu (≤ hôm nay)</option>
                                <option value="UPCOMING">Sắp khởi chiếu (&gt; hôm nay)</option>
                                <option value="THIS_MONTH">Khởi chiếu tháng này</option>
                                <option value="CUSTOM">Chọn khoảng ngày...</option>
                            </select>

                            <!-- Khoảng ngày Từ - Đến -->
                            <div class="cf-date-filter-wrap" id="movieDateFilterWrap">
                                <span class="cf-date-label">Từ</span>
                                <input type="date" id="movieReleaseFrom" class="cf-date-input" title="Khởi chiếu từ ngày">
                                <span class="cf-date-label">Đến</span>
                                <input type="date" id="movieReleaseTo" class="cf-date-input" title="Khởi chiếu đến ngày">
                            </div>

                            <!-- Nút Reset bộ lọc -->
                            <button type="button" class="cf-btn-reset-filter" id="btnResetFilters" title="Đặt lại tất cả bộ lọc về mặc định">
                                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <polyline points="23 4 23 10 17 10"></polyline>
                                    <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
                                </svg>
                            </button>
                        </div>

                        <div class="cf-toolbar-right">
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
                </div>

                <!-- TABLE WRAPPER (CHUỖI DỌC) -->
                <div class="cf-table-wrap" id="movieTableWrapper" style="margin: 0; border: none; border-radius: 0;">
                    <table class="cf-table">
                        <thead>
                            <tr>
                                <th style="width: 320px;">Phim & Mã số</th>
                                <th>Thể loại</th>
                                <th>Độ tuổi</th>
                                <th>Thời lượng</th>
                                <th>Khởi chiếu</th>
                                <th>Trạng thái</th>
                                <th style="text-align: center; width: 100px;">Phân phối</th>
                                <th style="text-align: right; width: 140px;">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody id="movieTableBody">
                            <% if (movies != null) {
                                for (Movie m : movies) {
                                    String st = m.getStatus();
                                    String stText = "NOW_SHOWING".equals(st) ? "Đang chiếu" :
                                                    "COMING_SOON".equals(st) ? "Sắp khởi chiếu" :
                                                    "ENDED".equals(st) ? "Đã kết thúc" : "Lưu trữ";
                                    String badgeClass = "NOW_SHOWING".equals(st) ? "cf-badge-success" :
                                                        "COMING_SOON".equals(st) ? "cf-badge-info" :
                                                        "ENDED".equals(st) ? "cf-badge-warning" : "cf-badge-muted";
                                    String age = m.getAgeRating() != null ? m.getAgeRating() : "P";
                                    String ageBadgeClass = "P".equalsIgnoreCase(age) ? "cf-badge-age-p" :
                                                           "K".equalsIgnoreCase(age) ? "cf-badge-age-k" :
                                                           "T13".equalsIgnoreCase(age) ? "cf-badge-age-t13" :
                                                           "T16".equalsIgnoreCase(age) ? "cf-badge-age-t16" :
                                                           "T18".equalsIgnoreCase(age) ? "cf-badge-age-t18" : "cf-badge-muted";
                                    String genre = m.getGenre() != null ? m.getGenre() : "Chưa cập nhật";
                                    String director = m.getDirector() != null ? m.getDirector() : "";
                                    String mCode = "CF-" + String.format("%04d", m.getMovieId());
                                    String releaseDateStr = m.getReleaseDate() != null ? m.getReleaseDate().toString() : "";
                            %>
                            <tr data-movie-id="<%= m.getMovieId() %>"
                                data-code="<%= mCode %>"
                                data-title="<%= m.getTitle() != null ? m.getTitle().replace("\"", "&quot;") : "" %>"
                                data-genre="<%= genre.replace("\"", "&quot;") %>"
                                data-director="<%= director.replace("\"", "&quot;") %>"
                                data-status="<%= st %>"
                                data-age-rating="<%= age %>"
                                data-release-date="<%= releaseDateStr %>">
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
                                                Mã phim: <strong><%= mCode %></strong><% if (!director.isBlank()) { %> · Đạo diễn: <%= director %><% } %>
                                            </div>
                                        </div>
                                    </div>
                                </td>
                                <td><%= genre %></td>
                                <td>
                                    <span class="cf-badge <%= ageBadgeClass %>"><%= age %></span>
                                </td>
                                <td><strong><%= m.getDuration() %></strong> phút</td>
                                <td><%= !releaseDateStr.isBlank() ? releaseDateStr : "—" %></td>
                                <td>
                                    <span class="cf-badge <%= badgeClass %>">
                                        <%= stText %>
                                    </span>
                                </td>
                                <td style="text-align: center; font-weight: 600; color: #edf2f9; font-size: 13px;">
                                    <%= m.getDistributionRatio() %>
                                </td>
                                <td style="text-align: right;">
                                    <div class="cf-table-actions">
                                        <!-- NÚT SỬA ĐẸP, TRỰC QUAN -->
                                        <a href="${pageContext.request.contextPath}/admin/movie?movieId=<%= m.getMovieId() %>"
                                           class="cf-btn-edit"
                                           title="Chỉnh sửa thông tin phim">
                                            <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                                                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                                                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                                            </svg>
                                            <span>Sửa</span>
                                        </a>
                                    </div>
                                </td>
                            </tr>
                            <% }} %>
                        </tbody>
                    </table>
                </div>

                <!-- TRẠNG THÁI KHÔNG TÌM THẤY DỮ LIỆU -->
                <div id="movieEmptyState" style="display: none; padding: 48px 24px; text-align: center; color: var(--cf-muted);">
                    <div style="font-size: 32px; margin-bottom: 8px;">🔍</div>
                    <h3 style="margin: 0 0 6px; color: var(--cf-text); font-size: 16px;">Không tìm thấy phim phù hợp</h3>
                    <p style="margin: 0; font-size: 13px;">Thử điều chỉnh lại các tiêu chí tìm kiếm hoặc bấm nút "Đặt lại bộ lọc".</p>
                </div>

                <!-- THANH PHÂN TRANG -->
                <div class="cf-pagination-bar">
                    <div class="cf-pagination-info" id="moviePaginationInfo">
                        Đang nạp danh mục phim...
                    </div>
                    <div class="cf-pagination-controls" id="moviePaginationControls">
                        <!-- Các nút phân trang được sinh động bởi JS -->
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
            codeSearchInputId: 'movieIdInput',
            statusPillSelector: '.cf-pill[data-status]',
            genreSelectId: 'movieGenreSelect',
            ageSelectId: 'movieAgeSelect',
            releasePresetSelectId: 'movieReleasePresetSelect',
            releaseFromInputId: 'movieReleaseFrom',
            releaseToInputId: 'movieReleaseTo',
            resetButtonId: 'btnResetFilters',
            pageSizeSelectId: 'moviePageSizeSelect',
            infoDisplayId: 'moviePaginationInfo',
            tableWrapperId: 'movieTableWrapper',
            emptyStateId: 'movieEmptyState'
        });

        document.querySelectorAll('.cf-alert-success, .cf-alert-danger').forEach(function(alert) {
            setTimeout(function() {
                alert.style.transition = 'opacity .4s ease, transform .4s ease';
                alert.style.opacity = '0';
                alert.style.transform = 'translateY(-8px)';

                setTimeout(function() {
                    if (alert.parentNode) alert.parentNode.removeChild(alert);
                }, 400);
            }, 3000);
        });
    });

    function toggleMovieMenu(event, movieId) {
        event.stopPropagation();
        const currentMenu = document.getElementById('movieMenu-' + movieId);
        const allMenus = document.querySelectorAll('.cf-action-menu');
        allMenus.forEach(function(menu) {
            if (menu !== currentMenu) {
                menu.classList.remove('show');
            }
        });
        if (currentMenu) {
            currentMenu.classList.toggle('show');
        }
    }

    document.addEventListener('click', function () {
        document.querySelectorAll('.cf-action-menu').forEach(function(menu) {
            menu.classList.remove('show');
        });
    });
</script>
</body>
</html>
