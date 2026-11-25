<%--
    Trang JSP: Danh mục phim dành cho Quản trị viên (Admin).
    Controller phục vụ: MovieManagementServlet (/admin/movies).
    Chức năng:
    - Hiển thị các thẻ thống kê KPI theo trạng thái phim (Sắp chiếu, Đang chiếu, Đã kết thúc, Lưu trữ).
    - Lọc theo trạng thái, tìm kiếm có dấu/không dấu và phân trang bằng JS (admin-pagination.js).
    - Điều hướng sang trang thêm mới hoặc chỉnh sửa thông tin phim (/admin/movie).
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.entity.Movie, java.util.List" %>
<%
    // 1. Bảo vệ trang: chỉ cho phép tài khoản có vai trò ADMIN truy cập
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && session.getAttribute("user") instanceof model.entity.User) {
        authRole = ((model.entity.User) session.getAttribute("user")).getRole();
    }
    if (!"ADMIN".equalsIgnoreCase(authRole)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Quản trị viên (ADMIN) mới có quyền truy cập trang này.");
        return;
    }

    // 2. Lấy danh sách phim do MovieManagementServlet truyền sang qua request scope
    List<Movie> movies = (List<Movie>) request.getAttribute("movies");
    if (movies == null) {
        response.sendRedirect(request.getContextPath() + "/admin/movies");
        return;
    }

    // 3. Thiết lập tiêu đề & mục đang chọn trên Sidebar/Topbar dùng chung
    request.setAttribute("activeNav", "movies");
    request.setAttribute("pageTitle", "Quản lý phim");
    request.setAttribute("pageBreadcrumb", "Danh mục phim");

    // 4. Đọc tham số thông báo sau khi lưu thành công từ movieForm.jsp
    String successParam = request.getParameter("success");
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
                                <p style="margin: 4px 0 0; color: var(--cf-muted, #94a3b8); font-size: 13px;">Quản lý thông tin phim trong hệ thống chuỗi rạp CineFlow.</p>
                            </div>
                        </div>
                        <a href="${pageContext.request.contextPath}/admin/movie" class="cf-btn cf-btn-primary" style="background: var(--cf-primary, #ff2855); font-weight: 600; padding: 10px 20px; border-radius: 8px; border: none; box-shadow: 0 4px 14px rgba(255, 40, 85, 0.3);">
                            Thêm phim mới
                        </a>
                    </div>

                    <!-- THÔNG BÁO ALERT KHI LƯU PHIM THÀNH CÔNG -->
                    <% if ("saved".equals(successParam)) { %>
                        <div class="cf-alert cf-alert-success" style="margin-bottom: 20px;">Đã lưu thông tin phim thành công vào hệ thống.</div>
                    <% } %>

                    <!-- CÁC THẺ CHỈ SỐ KPI THỐNG KÊ PHIM -->
                    <div class="cf-kpi-grid cf-kpi-grid-5">
                        <div class="cf-kpi-card">
                            <span class="cf-kpi-label">Tổng số phim</span>
                            <div class="cf-kpi-value">${totalMovies}</div>
                        </div>
                        <div class="cf-kpi-card">
                            <span class="cf-kpi-label" style="color: #0284c7;">Sắp khởi chiếu</span>
                            <div class="cf-kpi-value">${comingSoonCount}</div>
                        </div>
                        <div class="cf-kpi-card">
                            <span class="cf-kpi-label" style="color: #059669;">Đang chiếu</span>
                            <div class="cf-kpi-value">${nowShowingCount}</div>
                        </div>
                        <div class="cf-kpi-card">
                            <span class="cf-kpi-label" style="color: #d97706;">Đã kết thúc</span>
                            <div class="cf-kpi-value">${endedCount}</div>
                        </div>
                        <div class="cf-kpi-card">
                            <span class="cf-kpi-label" style="color: #64748b;">Lưu trữ</span>
                            <div class="cf-kpi-value">${archivedCount}</div>
                        </div>
                    </div>

                    <!-- BẢNG DANH MỤC PHIM VÀ BỘ LỌC ĐA NĂNG -->
                    <section class="cf-card">
                        <div class="cf-card-toolbar-compact">
                            <!-- BỘ LỌC TRẠNG THÁI -->
                            <div class="cf-toolbar-left">
                                <div class="cf-filter-pills">
                                    <button type="button" class="cf-pill active" data-status="ALL">Tất cả phim (${totalMovies})</button>
                                    <button type="button" class="cf-pill" data-status="NOW_SHOWING">Đang chiếu (${nowShowingCount})</button>
                                    <button type="button" class="cf-pill" data-status="COMING_SOON">Sắp khởi chiếu (${comingSoonCount})</button>
                                    <button type="button" class="cf-pill" data-status="ENDED">Đã kết thúc (${endedCount})</button>
                                    <button type="button" class="cf-pill" data-status="ARCHIVED">Lưu trữ (${archivedCount})</button>
                                </div>
                            </div>

                            <!-- Ô TÌM KIẾM + PHÂN TRANG -->
                            <div class="cf-toolbar-right">
                                <div class="cf-search-box cf-search-box-compact" id="movieSearchBoxWrap">
                                    <svg class="cf-search-icon" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                                        <circle cx="11" cy="11" r="8"></circle>
                                        <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                                    </svg>
                                    <input type="text" id="movieSearchInput" placeholder="Tìm tên phim, thể loại, đạo diễn..." autocomplete="off" spellcheck="false">
                                    <button type="button" id="movieSearchClearBtn" class="cf-search-clear-btn" title="Xóa từ khóa tìm kiếm" aria-label="Xóa từ khóa" style="display: none;">✕</button>
                                </div>

                                <div class="cf-page-size-wrap">
                                    <span>Hiển thị:</span>
                                    <select id="moviePageSizeSelect" class="cf-select-sm">
                                        <option value="8"selected>8 phim</option>
                                        <option value="12">12 phim</option>
                                        <option value="24">24 phim</option>
                                        <option value="50">50 phim</option>
                                        <option value="ALL">Tất cả</option>
                                    </select>
                                </div>
                            </div>
                        </div>

                        <!-- BẢNG DỮ LIỆU PHIM -->
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
                                    <% for (Movie m : movies) {
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
                                        int mCode = m.getMovieId();
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
                                                    <a href="${pageContext.request.contextPath}/admin/movie?movieId=<%= m.getMovieId() %>" style="font-weight: 700; color: var(--cf-text); text-decoration: none; font-size: 13.5px; display: block; line-height: 1.3;" onmouseover="this.style.color = 'var(--cf-primary)'" onmouseout="this.style.color = 'var(--cf-text)'">
                                                        <%= m.getTitle() %>
                                                    </a>
                                                    <div style="font-size: 12px; color: var(--cf-muted); margin-top: 3px; line-height: 1.3;">
                                                        Mã phim: <strong><%= mCode %></strong><% if (!director.isBlank()) { %> · Đạo diễn: <%= director %><% } %>
                                                    </div>
                                                </div>
                                            </div>
                                        </td>
                                        <td><%= genre %></td>
                                        <td><span class="cf-badge <%= ageBadgeClass %>"><%= age %></span></td>
                                        <td><strong><%= m.getDuration() %></strong> phút</td>
                                        <td><%= !releaseDateStr.isBlank() ? releaseDateStr : "—" %></td>
                                        <td><span class="cf-badge <%= badgeClass %>"><%= stText %></span></td>
                                        <%-- CỘT TỶ LỆ PHÂN PHỐI: Hiển thị dạng badge có liên kết bấm sang cấu hình phân phối (/admin/distribution?movieId=...) --%>
                                        <td style="text-align: center;">
                                            <% if (m.getTotalCentres() > 0 && m.getDistributedCentres() > 0) { %>
                                                <a href="${pageContext.request.contextPath}/admin/distribution?movieId=<%= m.getMovieId() %>"
                                                   class="cf-badge cf-badge-info"
                                                   style="text-decoration: none; font-weight: 700; font-size: 12px; display: inline-flex; align-items: center; gap: 4px;"
                                                   title="Đã phân phối tới <%= m.getDistributedCentres() %>/<%= m.getTotalCentres() %> cụm rạp. Bấm để điều chỉnh phân phối">
                                                    🏛 <%= m.getDistributionRatio() %>
                                                </a>
                                            <% } else if (m.getTotalCentres() > 0) { %>
                                                <a href="${pageContext.request.contextPath}/admin/distribution?movieId=<%= m.getMovieId() %>"
                                                   class="cf-badge cf-badge-muted"
                                                   style="text-decoration: none; font-size: 11.5px; display: inline-flex; align-items: center; gap: 4px;"
                                                   title="Chưa phân phối tới cụm rạp nào. Bấm để cấu hình phân phối">
                                                    0/<%= m.getTotalCentres() %>
                                                </a>
                                            <% } else { %>
                                                <span class="cf-badge cf-badge-muted">—</span>
                                            <% } %>
                                        </td>
                                        <td style="text-align: right;">
                                            <div class="cf-table-actions">
                                                <a href="${pageContext.request.contextPath}/admin/movie?movieId=<%= m.getMovieId() %>" class="cf-btn-edit" title="Chỉnh sửa thông tin phim">
                                                    <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                                                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                                                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                                                    </svg>
                                                    <span>Sửa</span>
                                                </a>
                                            </div>
                                        </td>
                                    </tr>
                                    <% } %>
                                </tbody>
                            </table>
                        </div>

                        <!-- KHÔNG TÌM THẤY DỮ LIỆU -->
                        <div id="movieEmptyState" style="display: none; padding: 48px 24px; text-align: center; color: var(--cf-muted);">
                            <div style="font-size: 32px; margin-bottom: 8px;">🔍</div>
                            <h3 style="margin: 0 0 6px; color: var(--cf-text); font-size: 16px;">Không tìm thấy phim phù hợp</h3>
                            <p style="margin: 0 0 14px; font-size: 13px;">Thử điều chỉnh lại từ khóa (hệ thống hỗ trợ gõ có dấu hoặc không dấu).</p>
                            <button type="button" id="btnEmptyReset" class="cf-btn cf-btn-secondary" style="font-size: 12.5px; padding: 6px 14px; border-radius: 6px;">Xóa từ khóa tìm kiếm</button>
                        </div>

                        <!-- PHÂN TRANG -->
                        <div class="cf-pagination-bar">
                            <div class="cf-pagination-info" id="moviePaginationInfo">Đang nạp danh mục phim...</div>
                            <div class="cf-pagination-controls" id="moviePaginationControls"></div>
                        </div>
                    </section>
                </main>
            </div>
        </div>

        <script src="${pageContext.request.contextPath}/js/admin-pagination.js?v=<%= System.currentTimeMillis() %>"></script>
        <script>
            document.addEventListener('DOMContentLoaded', function () {
                initAdminPagination({
                    tableBodyId: 'movieTableBody',
                    paginationId: 'moviePaginationControls',
                    searchInputId: 'movieSearchInput',
                    searchBoxWrapId: 'movieSearchBoxWrap',
                    clearButtonId: 'movieSearchClearBtn',
                    statusPillSelector: '.cf-pill[data-status]',
                    resetButtonId: 'btnResetFilters',
                    emptyResetButtonId: 'btnEmptyReset',
                    pageSizeSelectId: 'moviePageSizeSelect',
                    infoDisplayId: 'moviePaginationInfo',
                    tableWrapperId: 'movieTableWrapper',
                    emptyStateId: 'movieEmptyState'
                });

                document.querySelectorAll('.cf-alert-success, .cf-alert-danger').forEach(function (alert) {
                    setTimeout(function () {
                        alert.style.transition = 'opacity .4s ease, transform .4s ease';
                        alert.style.opacity = '0';
                        alert.style.transform = 'translateY(-8px)';
                        setTimeout(function () {
                            if (alert.parentNode) alert.parentNode.removeChild(alert);
                        }, 400);
                    }, 1500);
                });
            });
        </script>
    </body>
</html>
