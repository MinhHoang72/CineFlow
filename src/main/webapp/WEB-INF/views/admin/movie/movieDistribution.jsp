<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.entity.Movie,model.entity.Centre,java.util.List,java.util.Set,java.util.LinkedHashSet" %>
<%
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && session.getAttribute("user") instanceof model.entity.User) {
        authRole = ((model.entity.User) session.getAttribute("user")).getRole();
    }
    if (!"ADMIN".equalsIgnoreCase(authRole)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Quản trị viên (ADMIN) mới có quyền truy cập trang này.");
        return;
    }

    Movie movie = (Movie) request.getAttribute("movie");
    List<Movie> allMovies = (List<Movie>) request.getAttribute("allMovies");
    List<Centre> centres = (List<Centre>) request.getAttribute("centres");
    if (centres == null) centres = java.util.Collections.emptyList();
    if (allMovies == null) allMovies = java.util.Collections.emptyList();

    int totalCentres = centres.size();
    int totalDistributed = 0;
    int totalActiveCentres = 0;
    Set<String> cities = new LinkedHashSet<>();

    for (Centre c : centres) {
        if (c.getCity() != null && !c.getCity().isBlank()) {
            cities.add(c.getCity().trim());
        }
        if ("ACTIVE".equalsIgnoreCase(c.getStatus())) {
            totalActiveCentres++;
            if (c.isDistributed()) {
                totalDistributed++;
            }
        }
    }
    boolean isAllDistributed = (totalActiveCentres > 0 && totalDistributed == totalActiveCentres);

    request.setAttribute("activeNav", "distribution");
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
                                <p style="margin: 4px 0 0; color: var(--cf-muted, #94a3b8); font-size: 13px;">
                                </p>
                            </div>
                        </div>
                    </div>

                    <!-- THÔNG BÁO LƯU THÀNH CÔNG -->
                    <% if ("1".equals(request.getParameter("saved"))) { %>
                    <div id="saveSuccessAlert" class="cf-alert cf-alert-success" style="margin-bottom: 20px;">
                        Cập nhật cấu hình phân phối rạp chiếu thành công.
                    </div>
                    <% } else if ("movie_ended_or_archived".equals(request.getParameter("error"))) { %>
                    <div class="cf-alert cf-alert-danger" style="margin-bottom: 20px;">
                        Phim ở trạng thái Lưu trữ (ARCHIVED) hoặc Đã kết thúc (ENDED) không thể phân phối tại các rạp chiếu.
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

                        <!-- BỘ CHỌN PHIM COMBOBOX TÌM KIẾM THÔNG MINH (CUSTOM FLOATING DROPDOWN) -->
                        <div class="cf-movie-picker-container" id="moviePickerContainer">
                            <div class="cf-movie-input-box" id="movieInputBox">
                                <span class="cf-movie-search-icon">
                                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <circle cx="11" cy="11" r="8"></circle>
                                    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                                    </svg>
                                </span>
                                <input type="text"
                                       id="movieSearchInput"
                                       class="cf-movie-search-input"
                                       placeholder="Tìm kiếm phim theo tên hoặc mã phim..."
                                       value="<%= movie != null ? movie.getTitle() : "" %>"
                                       data-selected-title="<%= movie != null ? movie.getTitle().replace("\"", "&quot;") : "" %>"
                                       autocomplete="off">
                                <div class="cf-movie-input-actions">
                                    <button type="button" class="cf-btn-clear-search" id="btnClearMovieSearch" title="Xóa từ khóa" style="display: none;">✕</button>
                                    <span class="cf-movie-dropdown-icon" id="toggleMovieDropdown">
                                        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                                        <polyline points="6 9 12 15 18 9"></polyline>
                                        </svg>
                                    </span>
                                </div>
                            </div>

                            <!-- POPUP DANH SÁCH PHIM (FLOATING ABSOLUTE, GIỚI HẠN 300PX, KHÔNG ĐẨY BỐ CỤC) -->
                            <div class="cf-movie-dropdown-panel" id="movieDropdownPanel">
                                <div class="cf-movie-list-scroll" id="movieListScroll">
                                    <% if (allMovies.isEmpty()) { %>
                                    <div class="cf-movie-empty-opt">Chưa có phim nào trong hệ thống</div>
                                    <% } else {
                                        for (Movie m : allMovies) {
                                            boolean isSelected = (movie != null && m.getMovieId() == movie.getMovieId());
                                            String mCode = "CF-" + String.format("%04d", m.getMovieId());
                                            String st = m.getStatus();
                                            String stText = "NOW_SHOWING".equals(st) ? "Đang chiếu" :
                                                            "COMING_SOON".equals(st) ? "Sắp khởi chiếu" :
                                                            "ENDED".equals(st) ? "Đã kết thúc" : "Lưu trữ";
                                            String badgeClass = "NOW_SHOWING".equals(st) ? "cf-badge-success" :
                                                                "COMING_SOON".equals(st) ? "cf-badge-info" :
                                                                "ENDED".equals(st) ? "cf-badge-warning" : "cf-badge-muted";
                                    %>
                                    <div class="cf-movie-option <%= isSelected ? "is-selected" : "" %>"
                                         data-movie-id="<%= m.getMovieId() %>"
                                         data-title="<%= m.getTitle() != null ? m.getTitle().toLowerCase().replace("\"", "&quot;") : "" %>"
                                         data-code="<%= mCode.toLowerCase() %>"
                                         onclick="selectMovie(<%= m.getMovieId() %>)">
                                        <div class="cf-movie-opt-poster">
                                            <% if (m.getPosterUrl() != null && !m.getPosterUrl().isBlank()) { %>
                                            <img src="<%= m.getPosterUrl() %>" alt="<%= m.getTitle() %>" loading="lazy" onerror="this.parentElement.innerHTML='🎬';">
                                            <% } else { %>
                                            🎬
                                            <% } %>
                                        </div>
                                        <div class="cf-movie-opt-info">
                                            <div class="cf-movie-opt-title-row">
                                                <span class="cf-movie-opt-title"><%= m.getTitle() %></span>
                                                <span class="cf-badge <%= badgeClass %>"><%= stText %></span>
                                            </div>
                                            <div class="cf-movie-opt-sub">
                                                <span><%= mCode %></span> · <span><%= m.getGenre() != null ? m.getGenre() : "Chưa cập nhật" %></span>
                                                <% if (m.getDuration() > 0) { %> · <span><%= m.getDuration() %> phút</span><% } %>
                                            </div>
                                        </div>
                                        <% if (isSelected) { %>
                                        <span class="cf-movie-opt-check" title="Đang chọn">✓</span>
                                        <% } %>
                                    </div>
                                    <% }} %>
                                    <div class="cf-movie-empty-opt" id="movieEmptyOption" style="display: none;">
                                        Không tìm thấy phim nào khớp với từ khóa
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- THẺ THÔNG TIN PHIM ĐƯỢC CHỌN -->
                        <% if (movie != null) {
                            String st = movie.getStatus();
                            String stText = "NOW_SHOWING".equals(st) ? "Đang chiếu" :
                                            "COMING_SOON".equals(st) ? "Sắp khởi chiếu" :
                                            "ENDED".equals(st) ? "Đã kết thúc" : "Lưu trữ";
                            String badgeClass = "NOW_SHOWING".equals(st) ? "cf-badge-success" :
                                                "COMING_SOON".equals(st) ? "cf-badge-info" :
                                                "ENDED".equals(st) ? "cf-badge-warning" : "cf-badge-muted";
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
                                    Mã phim: CF-<%= String.format("%04d", movie.getMovieId()) %> · Thể loại: <%= movie.getGenre() != null ? movie.getGenre() : "Chưa cập nhật" %>
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
                        <% boolean isEndedOrArchived = ("ARCHIVED".equalsIgnoreCase(movie.getStatus()) || "ENDED".equalsIgnoreCase(movie.getStatus()));
                           if (isEndedOrArchived) { %>
                        <div class="cf-alert" style="margin-top: 14px; background: rgba(239, 68, 68, 0.12); border: 1px solid rgba(239, 68, 68, 0.35); color: #f87171; display: flex; align-items: center; gap: 10px; padding: 12px 16px; border-radius: 8px;">
                            <span style="font-size: 18px;">⛔</span>
                            <div>
                                <strong>Phim không còn được phân phối:</strong> Phim đang ở trạng thái <strong><%= "ARCHIVED".equalsIgnoreCase(movie.getStatus()) ? "Lưu trữ (ARCHIVED)" : "Đã kết thúc (ENDED)" %></strong>. Toàn bộ phân phối tại các rạp đã ngừng hoạt động. Để tiếp tục phân phối lại, vui lòng kích hoạt lại phim trong mục Chỉnh sửa thông tin phim.
                            </div>
                        </div>
                        <% } %>
                        <% } %>
                    </section>

                    <!-- BƯỚC 2: CẤU HÌNH PHÂN PHỐI -->
                    <% if (movie != null) { %>
                    <form id="distributionForm" action="${pageContext.request.contextPath}/admin/distribution" method="POST">
                        <input type="hidden" name="movieId" value="<%= movie.getMovieId() %>">

                        <section class="cf-dist-section">
                            <div class="cf-step-header">
                                <div class="cf-step-title-wrap">
                                    <div class="cf-step-badge">2</div>
                                    <h2 class="cf-step-title">Cấu hình phân phối</h2>
                                </div>
                                <div class="cf-step-counter" id="selectedCounterText">
                                    Đã chọn <strong id="selectedCountNum" style="color: #ffffff;"><%= totalDistributed %></strong>/<%= totalCentres %> cụm rạp
                                </div>
                            </div>

                            <!-- BỘ LỌC CỤM RẠP & HÀNH ĐỘNG NHANH -->
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
                                    <option value="SUSPENDED">Tạm ngưng (SUSPENDED)</option>
                                    <option value="INACTIVE">Ngừng hoạt động (INACTIVE)</option>
                                </select>

                                <button type="button"
                                        class="cf-btn-dist-all <%= isAllDistributed ? "is-cancel" : "" %>"
                                        id="btnSelectAll"
                                        data-is-all="<%= isAllDistributed ? "1" : "0" %>"
                                        <%= (movie != null && ("ARCHIVED".equalsIgnoreCase(movie.getStatus()) || "ENDED".equalsIgnoreCase(movie.getStatus()))) ? "disabled style='opacity: 0.4; cursor: not-allowed;' title='Phim đã kết thúc/lưu trữ, không thể phân phối'" : ("title=\"" + (isAllDistributed ? "Hủy phân phối toàn bộ các cụm rạp đang hoạt động" : "Phân phối cho tất cả cụm rạp đang hoạt động") + "\"") %>>
                                    <span class="cf-btn-dist-text"><%= isAllDistributed ? "Hủy phân phối toàn bộ" : "Phân phối toàn bộ" %></span>
                                </button>

                            </div>

                            <!-- BẢNG DANH SÁCH CỤM RẠP -->
                            <div class="cf-dist-table-heading">
                                <span>Danh sách cụm rạp</span>
                            </div>
                            <div class="cf-dist-table-wrap">
                                <table class="cf-dist-table">
                                    <thead>
                                        <tr>
                                            <th>Cụm rạp</th>
                                            <th>Thành phố</th>
                                            <th>Trạng thái cụm rạp</th>
                                            <th style="width: 130px; text-align: center;">Cho phép chiếu</th>
                                        </tr>
                                    </thead>
                                    <tbody id="centreTableBody">
                                        <% for (Centre c : centres) {
                                            String rawStatus = c.getStatus() != null ? c.getStatus().trim().toUpperCase() : "ACTIVE";
                                            boolean isActive = "ACTIVE".equals(rawStatus);
                                            boolean isSuspended = "SUSPENDED".equals(rawStatus) || "TEMPORARILY_CLOSED".equals(rawStatus);
                                            boolean isDistributed = c.isDistributed();
                                            int futureCount = c.getFutureShowtimeCount();
                                            int soldCount = c.getSoldTicketCount();
                                            String cCity = c.getCity() != null ? c.getCity() : "";

                                            String badgeText = isActive ? "Hoạt động" : (isSuspended ? "Tạm ngưng (SUSPENDED)" : "Ngừng HĐ (INACTIVE)");
                                            String switchTitle = isActive ? "Bật/Tắt phân phối chiếu" :
                                                                 (isSuspended ? "Cụm rạp đang tạm ngưng (SUSPENDED), phân phối tạm mất hiệu lực và không thể tạo mới" :
                                                                 "Cụm rạp đang ngừng hoạt động (INACTIVE), phân phối đã bị hủy");
                                        %>
                                        <tr class="cf-centre-row <%= !isActive ? "is-disabled" : "" %>"
                                            data-id="<%= c.getCentreId() %>"
                                            data-active="<%= isActive ? "1" : "0" %>"
                                            data-name="<%= c.getCentreName().toLowerCase() %>"
                                            data-centre-name="<%= c.getCentreName() %>"
                                            data-city="<%= cCity.toLowerCase() %>"
                                            data-city-raw="<%= cCity %>"
                                            data-status="<%= rawStatus %>"
                                            data-future="<%= futureCount %>"
                                            data-sold="<%= soldCount %>">
                                            <td>
                                                <div class="cf-centre-name-cell">
                                                    <span class="cf-centre-icon">
                                                        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                                        <rect x="4" y="2" width="16" height="20" rx="2" ry="2"></rect>
                                                        <line x1="9" y1="22" x2="9" y2="22.01"></line>
                                                        <line x1="15" y1="22" x2="15" y2="22.01"></line>
                                                        <line x1="8" y1="6" x2="8" y2="6.01"></line>
                                                        <line x1="16" y1="6" x2="16" y2="6.01"></line>
                                                        <line x1="8" y1="10" x2="8" y2="10.01"></line>
                                                        <line x1="16" y1="10" x2="16" y2="10.01"></line>
                                                        <line x1="8" y1="14" x2="8" y2="14.01"></line>
                                                        <line x1="16" y1="14" x2="14.01"></line>
                                                        <line x1="8" y1="18" x2="8" y2="18.01"></line>
                                                        <line x1="16" y1="18" x2="18.01"></line>
                                                        </svg>
                                                    </span>
                                                    <span><%= c.getCentreName() %></span>
                                                    <% if (futureCount > 0 || soldCount > 0) { %>
                                                    <span class="cf-stat-tag" title="Số lượng suất chiếu tương lai và vé đã bán">
                                                        <%= futureCount %> suất · <%= soldCount %> vé
                                                    </span>
                                                    <% } %>
                                                </div>
                                            </td>
                                            <td style="color: #94a3b8;">
                                                <%= !cCity.isBlank() ? cCity : "—" %>
                                            </td>
                                            <td>
                                                <% if (isActive) { %>
                                                <span class="cf-badge-active">Hoạt động</span>
                                                <% } else if (isSuspended) { %>
                                                <span class="cf-badge-maintenance" style="background: rgba(245, 158, 11, 0.15); color: #fbbf24; border: 1px solid rgba(245, 158, 11, 0.3);" title="Cụm rạp tạm ngừng hoạt động">Tạm ngưng</span>
                                                <% } else { %>
                                                <span class="cf-badge-maintenance" style="background: rgba(239, 68, 68, 0.15); color: #f87171; border: 1px solid rgba(239, 68, 68, 0.3);" title="Cụm rạp ngừng hoạt động lâu dài">Ngừng HĐ</span>
                                                <% } %>
                                            </td>
                                            <td>
                                                <div class="cf-switch-cell-wrap">
                                                    <% boolean isMovieArchivedOrEnded = (movie != null && ("ARCHIVED".equalsIgnoreCase(movie.getStatus()) || "ENDED".equalsIgnoreCase(movie.getStatus())));
                                                       boolean canToggleSwitch = isActive && !isMovieArchivedOrEnded; %>
                                                    <label class="cf-switch <%= !canToggleSwitch ? "is-disabled" : "" %>"
                                                           title="<%= isMovieArchivedOrEnded ? "Phim ở trạng thái ARCHIVED/ENDED, không thể phân phối" : switchTitle %>">
                                                        <input type="checkbox"
                                                               name="centreIds"
                                                               value="<%= c.getCentreId() %>"
                                                               class="cf-switch-input"
                                                               data-future="<%= futureCount %>"
                                                               data-sold="<%= soldCount %>"
                                                               data-centre-name="<%= c.getCentreName() %>"
                                                               <%= (canToggleSwitch && isDistributed) ? "checked" : "" %>
                                                               <%= !canToggleSwitch ? "disabled" : "" %>>
                                                        <span class="cf-switch-slider"></span>
                                                    </label>
                                                    <span class="cf-row-arrow">
                                                        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                                                        <polyline points="9 18 15 12 9 6"></polyline>
                                                        </svg>
                                                    </span>
                                                </div>
                                            </td>
                                        </tr>
                                        <% } %>
                                    </tbody>
                                </table>
                            </div>

                            <!-- KHỐI NÚT HÀNH ĐỘNG HỦY VÀ LƯU PHÂN PHỐI -->
                            <div class="cf-dist-actions-bar">
                                <button type="button" class="cf-btn-cancel" onclick="location.reload();">
                                    Hủy thay đổi
                                </button>
                                <button type="submit" class="cf-btn-save" <%= (movie != null && ("ARCHIVED".equalsIgnoreCase(movie.getStatus()) || "ENDED".equalsIgnoreCase(movie.getStatus()))) ? "disabled style='opacity: 0.4; cursor: not-allowed;' title='Phim đã kết thúc/lưu trữ, không thể lưu phân phối'" : "" %>>
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

                <!-- MODAL XÁC NHẬN CẢNH BÁO NGHIỆP VỤ MỨC CAO KHI TẮT PHÂN PHỐI -->
                <div class="cf-modal-backdrop" id="highRiskConfirmModal">
                    <div class="cf-modal-risk">
                        <div class="cf-modal-risk-header">
                            <h3 class="cf-modal-risk-title">
                                <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="#ef4444" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
                                <line x1="12" y1="9" x2="12" y2="13"></line>
                                <line x1="12" y1="17" x2="12.01" y2="17"></line>
                                </svg>
                                Xác nhận cảnh báo nghiệp vụ
                            </h3>
                            <button type="button" class="cf-modal-risk-close" id="btnRiskModalClose" aria-label="Đóng">&times;</button>
                        </div>
                        <div class="cf-modal-risk-body">
                            <div class="cf-risk-data-card">
                                <p class="cf-risk-card-label">
                                    Cụm rạp <strong id="riskCentreName" class="cf-risk-centre-name"></strong> đang có dữ liệu vận hành:
                                </p>
                                <div class="cf-risk-stats-grid">
                                    <div class="cf-risk-stat-box">
                                        <div class="cf-risk-stat-title">Suất chiếu tương lai</div>
                                        <div class="cf-risk-stat-number" id="riskFutureCount">0</div>
                                    </div>
                                    <div class="cf-risk-stat-box">
                                        <div class="cf-risk-stat-title">Vé đã bán</div>
                                        <div class="cf-risk-stat-number" id="riskSoldCount">0</div>
                                    </div>
                                </div>
                            </div>
                            <div class="cf-risk-notice-callout">
                                <p class="cf-risk-notice-text">
                                    <strong>Lưu ý nghiệp vụ:</strong> Hệ thống cho phép tắt phân phối trong trường hợp khẩn cấp (thu hồi phim hoặc sự cố rạp). Khi tắt, rạp không thể tạo thêm suất chiếu mới và các suất chiếu tương lai có thể cần xử lý hủy.
                                </p>
                            </div>
                            <p class="cf-risk-question">
                                Bạn có chắc chắn muốn tắt phân phối không?
                            </p>
                        </div>
                        <div class="cf-modal-risk-footer">
                            <button type="button" class="cf-btn-risk-cancel" id="btnRiskCancel">
                                Hủy bỏ (Giữ nguyên)
                            </button>
                            <button type="button" class="cf-btn-risk-confirm" id="btnRiskConfirm">
                                Xác nhận tắt phân phối
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- CLIENT JAVASCRIPT ĐIỀU KHIỂN TƯƠNG TÁC -->
        <script>
            //xu ly thong bao
            document.addEventListener('DOMContentLoaded', function () {
               
                const alert = document.getElementById('saveSuccessAlert');

                if (alert) {
                    setTimeout(function () {
                        alert.style.transition = 'opacity 0.4s ease, transform 0.4s ease';
                        alert.style.opacity = '0';
                        alert.style.transform = 'translateY(-6px)';

                        setTimeout(function () {
                            alert.remove();
                        }, 400);
                    }, 1000);
                }
                 // === 1. XỬ LÝ CHỌN PHIM COMBOBOX TÌM KIẾM THÔNG MINH ===
                const moviePickerContainer = document.getElementById('moviePickerContainer');
                const movieInputBox = document.getElementById('movieInputBox');
                const movieSearchInput = document.getElementById('movieSearchInput');
                const movieDropdownPanel = document.getElementById('movieDropdownPanel');
                const btnClearMovieSearch = document.getElementById('btnClearMovieSearch');
                const toggleMovieDropdown = document.getElementById('toggleMovieDropdown');
                const movieOptions = Array.from(document.querySelectorAll('.cf-movie-option'));
                const movieEmptyOption = document.getElementById('movieEmptyOption');

                function openMovieDropdown() {
                    if (!moviePickerContainer)
                        return;
                    moviePickerContainer.classList.add('is-open');
                    if (movieInputBox)
                        movieInputBox.classList.add('is-focused');
                }

                function closeMovieDropdown(restoreValue) {
                    if (!moviePickerContainer)
                        return;
                    moviePickerContainer.classList.remove('is-open');
                    if (movieInputBox)
                        movieInputBox.classList.remove('is-focused');
                    if (restoreValue && movieSearchInput) {
                        movieSearchInput.value = movieSearchInput.dataset.selectedTitle || '';
                        filterMovieOptions('');
                    }
                }

                function filterMovieOptions(keyword) {
                    const q = (keyword || '').trim().toLowerCase();
                    let matchCount = 0;
                    movieOptions.forEach(opt => {
                        const title = opt.dataset.title || '';
                        const code = opt.dataset.code || '';
                        if (!q || title.includes(q) || code.includes(q)) {
                            opt.style.display = 'flex';
                            matchCount++;
                        } else {
                            opt.style.display = 'none';
                        }
                    });
                    if (movieEmptyOption) {
                        movieEmptyOption.style.display = (matchCount === 0) ? 'block' : 'none';
                    }
                    if (btnClearMovieSearch) {
                        btnClearMovieSearch.style.display = q ? 'flex' : 'none';
                    }
                }

                window.selectMovie = function (movieId) {
                    closeMovieDropdown(false);
                    window.location.href = '${pageContext.request.contextPath}/admin/distribution?movieId=' + movieId;
                };

                if (movieSearchInput) {
                    movieSearchInput.addEventListener('focus', function () {
                        openMovieDropdown();
                        this.select();
                    });

                    movieSearchInput.addEventListener('click', function () {
                        openMovieDropdown();
                    });

                    movieSearchInput.addEventListener('input', function () {
                        openMovieDropdown();
                        filterMovieOptions(this.value);
                    });

                    movieSearchInput.addEventListener('keydown', function (e) {
                        if (e.key === 'Escape') {
                            closeMovieDropdown(true);
                            this.blur();
                        }
                    });
                }

                if (toggleMovieDropdown) {
                    toggleMovieDropdown.addEventListener('click', function (e) {
                        e.stopPropagation();
                        if (moviePickerContainer.classList.contains('is-open')) {
                            closeMovieDropdown(true);
                        } else {
                            openMovieDropdown();
                            if (movieSearchInput)
                                movieSearchInput.focus();
                        }
                    });
                }

                if (btnClearMovieSearch) {
                    btnClearMovieSearch.addEventListener('click', function (e) {
                        e.stopPropagation();
                        if (movieSearchInput) {
                            movieSearchInput.value = '';
                            filterMovieOptions('');
                            movieSearchInput.focus();
                        }
                    });
                }

                document.addEventListener('click', function (e) {
                    if (moviePickerContainer && !moviePickerContainer.contains(e.target)) {
                        closeMovieDropdown(true);
                    }
                });

                // === 2. XỬ LÝ CẤU HÌNH PHÂN PHỐI CỤM RẠP ===
                const tableBody = document.getElementById('centreTableBody');
                if (!tableBody)
                    return;

                const rows = Array.from(tableBody.querySelectorAll('.cf-centre-row'));
                const searchInput = document.getElementById('centreSearchInput');
                const cityFilter = document.getElementById('cityFilterSelect');
                const statusFilter = document.getElementById('statusFilterSelect');
                const btnSelectAll = document.getElementById('btnSelectAll');
                const selectedCountNum = document.getElementById('selectedCountNum');

                function updateDistributeAllButtonState(activeTotal, activeChecked) {
                    if (!btnSelectAll)
                        return;
                    const isAllSelected = (activeTotal > 0 && activeChecked === activeTotal);
                    btnSelectAll.dataset.isAll = isAllSelected ? "1" : "0";

                    const textSpan = btnSelectAll.querySelector('.cf-btn-dist-text');

                    if (isAllSelected) {
                        btnSelectAll.classList.add('is-cancel');
                        btnSelectAll.title = "Hủy phân phối toàn bộ các cụm rạp đang hoạt động";
                        if (textSpan)
                            textSpan.textContent = "Hủy phân phối toàn bộ";
                    } else {
                        btnSelectAll.classList.remove('is-cancel');
                        btnSelectAll.title = "Phân phối cho tất cả cụm rạp đang hoạt động";
                        if (textSpan)
                            textSpan.textContent = "Phân phối toàn bộ";
                    }
                }

                function updateCounter() {
                    let count = 0;
                    let activeTotal = 0;
                    let activeChecked = 0;

                    rows.forEach(row => {
                        const isActive = row.dataset.active === '1';
                        const sw = row.querySelector('.cf-switch-input');
                        if (isActive) {
                            activeTotal++;
                            if (sw && sw.checked) {
                                activeChecked++;
                            }
                        }
                        if (sw && sw.checked) {
                            count++;
                        }
                    });

                    if (selectedCountNum) {
                        selectedCountNum.textContent = count;
                    }

                    updateDistributeAllButtonState(activeTotal, activeChecked);
                }

                // Hàm hiển thị Pop-up Cảnh báo nghiệp vụ mức cao khi tắt phân phối
                function showRiskConfirmModal(centreName, futureCount, soldCount, onConfirm, onCancel) {
                    const modal = document.getElementById('highRiskConfirmModal');
                    if (!modal) {
                        // Fallback xác nhận chuẩn theo đúng yêu cầu
                        const confirmMsg = "CẢNH BÁO NGHIỆP VỤ MỨC CAO:\n\n"
                                + "Rạp " + centreName + " đang có " + futureCount + " suất chiếu tương lai và " + soldCount + " vé đã bán.\n\n"
                                + "Bạn có chắc chắn muốn tắt phân phối không?";
                        if (confirm(confirmMsg)) {
                            onConfirm();
                        } else {
                            onCancel();
                        }
                        return;
                    }

                    document.getElementById('riskCentreName').textContent = centreName;
                    document.getElementById('riskFutureCount').textContent = futureCount;
                    document.getElementById('riskSoldCount').textContent = soldCount;

                    const btnConfirm = document.getElementById('btnRiskConfirm');
                    const btnCancel = document.getElementById('btnRiskCancel');
                    const btnClose = document.getElementById('btnRiskModalClose');

                    function cleanup() {
                        modal.classList.remove('active');
                        btnConfirm.onclick = null;
                        btnCancel.onclick = null;
                        btnClose.onclick = null;
                    }

                    btnConfirm.onclick = function () {
                        cleanup();
                        onConfirm();
                    };

                    btnCancel.onclick = function () {
                        cleanup();
                        onCancel();
                    };

                    btnClose.onclick = function () {
                        cleanup();
                        onCancel();
                    };

                    modal.classList.add('active');
                }

                // Xử lý khi click vào Switch Toggle của từng rạp
                rows.forEach(row => {
                    const switchInput = row.querySelector('.cf-switch-input');
                    const futureCount = parseInt(row.dataset.future || '0', 10);
                    const soldCount = parseInt(row.dataset.sold || '0', 10);
                    const centreName = row.dataset.centreName || 'Cụm rạp';

                    if (switchInput) {
                        switchInput.addEventListener('change', function () {
                            if (!this.checked && (futureCount > 0 || soldCount > 0)) {
                                const targetSw = this;
                                showRiskConfirmModal(centreName, futureCount, soldCount,
                                        function () {
                                            // Đồng ý tắt
                                            updateCounter();
                                        },
                                        function () {
                                            // Hủy bỏ: phục hồi lại checked
                                            targetSw.checked = true;
                                            updateCounter();
                                        }
                                );
                            } else {
                                updateCounter();
                            }
                        });
                    }
                });

                // Nút "Phân phối toàn bộ" / "Hủy phân phối toàn bộ"
                if (btnSelectAll) {
                    btnSelectAll.addEventListener('click', function () {
                        const isCurrentlyAll = (btnSelectAll.dataset.isAll === '1');

                        if (isCurrentlyAll) {
                            // TRẠNG THÁI: HỦY PHÂN PHỐI TOÀN BỘ
                            let totalFuture = 0;
                            let totalSold = 0;

                            rows.forEach(row => {
                                if (row.dataset.active === '1') {
                                    const sw = row.querySelector('.cf-switch-input');
                                    if (sw && sw.checked) {
                                        totalFuture += parseInt(row.dataset.future || '0', 10);
                                        totalSold += parseInt(row.dataset.sold || '0', 10);
                                    }
                                }
                            });

                            if (totalFuture > 0 || totalSold > 0) {
                                showRiskConfirmModal("toàn bộ các cụm rạp đang phân phối", totalFuture, totalSold,
                                        function () {
                                            rows.forEach(row => {
                                                if (row.dataset.active === '1') {
                                                    const sw = row.querySelector('.cf-switch-input');
                                                    if (sw && !sw.disabled) {
                                                        sw.checked = false;
                                                    }
                                                }
                                            });
                                            updateCounter();
                                        },
                                        function () {
                                            // Hủy bỏ: giữ nguyên trạng thái
                                        }
                                );
                            } else {
                                rows.forEach(row => {
                                    if (row.dataset.active === '1') {
                                        const sw = row.querySelector('.cf-switch-input');
                                        if (sw && !sw.disabled) {
                                            sw.checked = false;
                                        }
                                    }
                                });
                                updateCounter();
                            }
                        } else {
                            // TRẠNG THÁI: PHÂN PHỐI TOÀN BỘ
                            rows.forEach(row => {
                                if (row.dataset.active === '1') {
                                    const sw = row.querySelector('.cf-switch-input');
                                    if (sw && !sw.disabled) {
                                        sw.checked = true;
                                    }
                                }
                            });
                            updateCounter();
                        }
                    });
                }



                // Bộ lọc & Tìm kiếm rạp
                function applyFilters() {
                    const query = (searchInput ? searchInput.value : '').trim().toLowerCase();
                    const city = cityFilter ? cityFilter.value : 'ALL';
                    const status = statusFilter ? statusFilter.value : 'ALL';

                    rows.forEach(row => {
                        const rName = row.dataset.name || '';
                        const rCity = row.dataset.city || '';
                        const rCityRaw = row.dataset.cityRaw || '';
                        const rStatus = (row.dataset.status || '').toUpperCase();

                        let matchesSearch = !query || rName.includes(query) || rCity.includes(query);
                        let matchesCity = (city === 'ALL') || (rCityRaw.toLowerCase() === city.toLowerCase());
                        let matchesStatus = (status === 'ALL') || (rStatus === status) ||
                                (status === 'SUSPENDED' && (rStatus === 'SUSPENDED' || rStatus === 'TEMPORARILY_CLOSED')) ||
                                (status === 'INACTIVE' && rStatus === 'INACTIVE') ||
                                (status === 'ACTIVE' && rStatus === 'ACTIVE');

                        if (matchesSearch && matchesCity && matchesStatus) {
                            row.style.display = '';
                        } else {
                            row.style.display = 'none';
                        }
                    });
                }

                if (searchInput)
                    searchInput.addEventListener('input', applyFilters);
                if (cityFilter)
                    cityFilter.addEventListener('change', applyFilters);
                if (statusFilter)
                    statusFilter.addEventListener('change', applyFilters);


            });
        </script>
    </body>
</html>
