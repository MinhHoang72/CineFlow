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

    int totalActiveCentres = 0;
    int totalDistributed = 0;
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
    <jsp:include page="/WEB-INF/fragments/adminSidebar.jsp" />

    <div class="cf-manager-main">
        <jsp:include page="/WEB-INF/fragments/adminTopbar.jsp" />

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
                            Chọn phim và cấu hình các cụm rạp được phép chiếu. Việc phân phối giúp kiểm soát phạm vi phát hành theo từng phim và từng cụm rạp.
                        </p>
                    </div>
                </div>
            </div>

            <!-- THÔNG BÁO LƯU THÀNH CÔNG -->
            <% if ("1".equals(request.getParameter("saved"))) { %>
                <div id="saveSuccessAlert" class="cf-alert cf-alert-success" style="margin-bottom: 20px;">
                    Cập nhật cấu hình phân phối rạp chiếu thành công.
                </div>
                <script>
                    setTimeout(function() {
                        const alert = document.getElementById('saveSuccessAlert');
                        if (alert) {
                            alert.style.transition = 'opacity 0.4s ease, transform 0.4s ease';
                            alert.style.opacity = '0';
                            alert.style.transform = 'translateY(-6px)';
                            setTimeout(function() { alert.remove(); }, 400);
                        }
                    }, 3000);
                </script>
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
                            Đã chọn <strong id="selectedCountNum" style="color: #ffffff;"><%= totalDistributed %></strong>/<%= totalActiveCentres %> cụm rạp
                        </div>
                    </div>

                    <!-- BỘ LỌC CỤM RẠP -->
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
                            <option value="ACTIVE">Hoạt động</option>
                            <option value="MAINTENANCE">Bảo trì / Tạm đóng</option>
                        </select>

                        <button type="button" class="cf-btn-reset-filter" id="btnResetFilters" title="Đặt lại bộ lọc">
                            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <polyline points="23 4 23 10 17 10"></polyline>
                                <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
                            </svg>
                        </button>
                    </div>

                    <!-- BẢNG DANH SÁCH CỤM RẠP -->
                    <div class="cf-dist-table-wrap">
                        <table class="cf-dist-table">
                            <thead>
                                <tr>
                                    <th style="width: 44px; text-align: center;">
                                        <input type="checkbox" id="masterCheckbox" title="Chọn / Bỏ chọn tất cả rạp đang hoạt động" style="cursor: pointer; width: 16px; height: 16px;">
                                    </th>
                                    <th>Cụm rạp</th>
                                    <th>Thành phố</th>
                                    <th>Trạng thái cụm rạp</th>
                                    <th style="width: 120px;">Cho phép chiếu</th>
                                </tr>
                            </thead>
                            <tbody id="centreTableBody">
                                <% for (Centre c : centres) {
                                    boolean isActive = "ACTIVE".equalsIgnoreCase(c.getStatus());
                                    boolean isDistributed = c.isDistributed();
                                    int futureCount = c.getFutureShowtimeCount();
                                    String cCity = c.getCity() != null ? c.getCity() : "";
                                %>
                                <tr class="cf-centre-row <%= !isActive ? "is-disabled" : "" %>"
                                    data-centre-id="<%= c.getCentreId() %>"
                                    data-name="<%= c.getCentreName().toLowerCase() %>"
                                    data-city="<%= cCity.toLowerCase() %>"
                                    data-city-raw="<%= cCity %>"
                                    data-status="<%= isActive ? "ACTIVE" : "MAINTENANCE" %>"
                                    data-active="<%= isActive ? "1" : "0" %>"
                                    data-future="<%= futureCount %>">
                                    <td style="text-align: center;">
                                        <input type="checkbox" name="centreIds" value="<%= c.getCentreId() %>"
                                               class="cf-row-check"
                                               id="check-<%= c.getCentreId() %>"
                                               <%= (isActive && isDistributed) ? "checked" : "" %>
                                               <%= !isActive ? "disabled" : "" %>
                                               style="cursor: pointer; width: 16px; height: 16px;">
                                    </td>
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
                                                    <line x1="16" y1="14" x2="16" y2="14.01"></line>
                                                    <line x1="8" y1="18" x2="8" y2="18.01"></line>
                                                    <line x1="16" y1="18" x2="16" y2="18.01"></line>
                                                </svg>
                                            </span>
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
                                            <span class="cf-badge-maintenance">Bảo trì / Tạm đóng</span>
                                        <% } %>
                                    </td>
                                    <td>
                                        <div class="cf-switch-cell-wrap">
                                            <label class="cf-switch <%= !isActive ? "is-disabled" : "" %>"
                                                   title="<%= !isActive ? "Cụm rạp đang tạm đóng, không thể phân phối" : "Bật/Tắt phân phối" %>">
                                                <input type="checkbox"
                                                       class="cf-switch-input"
                                                       data-target-check="check-<%= c.getCentreId() %>"
                                                       data-future="<%= futureCount %>"
                                                       <%= (isActive && isDistributed) ? "checked" : "" %>
                                                       <%= !isActive ? "disabled" : "" %>>
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

                    <!-- THÔNG BÁO GHI CHÚ NGUYÊN TẮC NGHIỆP VỤ -->
                    <div class="cf-dist-note-box">
                        <div class="cf-dist-note-icon">
                            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <circle cx="12" cy="12" r="10"></circle>
                                <line x1="12" y1="16" x2="12" y2="12"></line>
                                <line x1="12" y1="8" x2="12.01" y2="8"></line>
                            </svg>
                        </div>
                        <div>
                            <strong>Lưu ý:</strong> Chỉ các cụm rạp đang hoạt động mới được chọn. Phim đã kết thúc hoặc lưu trữ không được phân phối để chiếu mới.
                            Nếu bỏ chọn cụm rạp đã có lịch chiếu tương lai, hệ thống sẽ yêu cầu xác nhận để tránh ảnh hưởng đến vé đã đặt.
                        </div>
                    </div>

                    <!-- KHỐI NÚT HÀNH ĐỘNG HỦY VÀ LƯU PHÂN PHỐI -->
                    <div class="cf-dist-actions-bar">
                        <button type="button" class="cf-btn-cancel" onclick="location.reload();">
                            Hủy
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

<!-- CLIENT JAVASCRIPT ĐIỀU KHIỂN TƯƠNG TÁC -->
<script>
document.addEventListener('DOMContentLoaded', function () {
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
        if (!moviePickerContainer) return;
        moviePickerContainer.classList.add('is-open');
        if (movieInputBox) movieInputBox.classList.add('is-focused');
    }

    function closeMovieDropdown(restoreValue) {
        if (!moviePickerContainer) return;
        moviePickerContainer.classList.remove('is-open');
        if (movieInputBox) movieInputBox.classList.remove('is-focused');
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

    window.selectMovie = function(movieId) {
        closeMovieDropdown(false);
        window.location.href = '${pageContext.request.contextPath}/admin/distribution?movieId=' + movieId;
    };

    if (movieSearchInput) {
        // Nhấn vào ô input -> mở danh sách và bôi đen text để gõ tìm ngay
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
                if (movieSearchInput) movieSearchInput.focus();
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

    // Đóng dropdown khi click ra ngoài
    document.addEventListener('click', function (e) {
        if (moviePickerContainer && !moviePickerContainer.contains(e.target)) {
            closeMovieDropdown(true);
        }
    });

    // === 2. XỬ LÝ CẤU HÌNH PHÂN PHỐI CỤM RẠP ===
    const tableBody = document.getElementById('centreTableBody');
    if (!tableBody) return;

    const rows = Array.from(tableBody.querySelectorAll('.cf-centre-row'));
    const masterCheckbox = document.getElementById('masterCheckbox');
    const searchInput = document.getElementById('centreSearchInput');
    const cityFilter = document.getElementById('cityFilterSelect');
    const statusFilter = document.getElementById('statusFilterSelect');
    const btnReset = document.getElementById('btnResetFilters');
    const selectedCountNum = document.getElementById('selectedCountNum');

    const totalActive = <%= totalActiveCentres %>;

    // 1. Đồng bộ Switch Toggle với Row Checkbox
    function updateCounter() {
        let count = 0;
        rows.forEach(row => {
            if (row.dataset.active === '1') {
                const check = row.querySelector('.cf-row-check');
                if (check && check.checked) {
                    count++;
                }
            }
        });
        if (selectedCountNum) {
            selectedCountNum.textContent = count;
        }
        updateMasterCheckbox();
    }

    function updateMasterCheckbox() {
        if (!masterCheckbox) return;
        const visibleActiveRows = rows.filter(r => r.style.display !== 'none' && r.dataset.active === '1');
        if (visibleActiveRows.length === 0) {
            masterCheckbox.checked = false;
            masterCheckbox.indeterminate = false;
            return;
        }
        const checkedCount = visibleActiveRows.filter(r => {
            const ch = r.querySelector('.cf-row-check');
            return ch && ch.checked;
        }).length;

        if (checkedCount === visibleActiveRows.length) {
            masterCheckbox.checked = true;
            masterCheckbox.indeterminate = false;
        } else if (checkedCount > 0) {
            masterCheckbox.checked = false;
            masterCheckbox.indeterminate = true;
        } else {
            masterCheckbox.checked = false;
            masterCheckbox.indeterminate = false;
        }
    }

    // Xử lý khi click vào Switch Toggle
    rows.forEach(row => {
        const switchInput = row.querySelector('.cf-switch-input');
        const checkInput = row.querySelector('.cf-row-check');
        const futureCount = parseInt(row.dataset.future || '0', 10);

        if (switchInput && checkInput) {
            switchInput.addEventListener('change', function () {
                if (!this.checked && futureCount > 0) {
                    const confirmMsg = "Cảnh báo nghiệp vụ:\nCụm rạp này đang có " + futureCount + " suất chiếu trong tương lai của phim này.\n\nBỏ phân phối có thể gây ảnh hưởng đến lịch chiếu và vé khán giả đã đặt. Bạn có chắc chắn muốn bỏ chọn?";
                    if (!confirm(confirmMsg)) {
                        this.checked = true;
                        return;
                    }
                }
                checkInput.checked = this.checked;
                updateCounter();
            });

            checkInput.addEventListener('change', function () {
                if (!this.checked && futureCount > 0) {
                    const confirmMsg = "Cảnh báo nghiệp vụ:\nCụm rạp này đang có " + futureCount + " suất chiếu trong tương lai của phim này.\n\nBỏ phân phối có thể gây ảnh hưởng đến lịch chiếu và vé khán giả đã đặt. Bạn có chắc chắn muốn bỏ chọn?";
                    if (!confirm(confirmMsg)) {
                        this.checked = true;
                        return;
                    }
                }
                switchInput.checked = this.checked;
                updateCounter();
            });
        }
    });

    // 2. Master Checkbox
    if (masterCheckbox) {
        masterCheckbox.addEventListener('change', function () {
            const isChecked = this.checked;
            const visibleActiveRows = rows.filter(r => r.style.display !== 'none' && r.dataset.active === '1');

            visibleActiveRows.forEach(row => {
                const check = row.querySelector('.cf-row-check');
                const sw = row.querySelector('.cf-switch-input');
                if (check && !check.disabled) {
                    check.checked = isChecked;
                }
                if (sw && !sw.disabled) {
                    sw.checked = isChecked;
                }
            });
            updateCounter();
        });
    }

    // 3. Lọc & Tìm kiếm tức thì
    function applyFilters() {
        const query = (searchInput ? searchInput.value : '').trim().toLowerCase();
        const city = cityFilter ? cityFilter.value : 'ALL';
        const status = statusFilter ? statusFilter.value : 'ALL';

        rows.forEach(row => {
            const rName = row.dataset.name || '';
            const rCity = row.dataset.city || '';
            const rCityRaw = row.dataset.cityRaw || '';
            const rStatus = row.dataset.status || '';

            let matchesSearch = !query || rName.includes(query) || rCity.includes(query);
            let matchesCity = (city === 'ALL') || (rCityRaw.toLowerCase() === city.toLowerCase());
            let matchesStatus = (status === 'ALL') || (rStatus === status);

            if (matchesSearch && matchesCity && matchesStatus) {
                row.style.display = '';
            } else {
                row.style.display = 'none';
            }
        });

        updateMasterCheckbox();
    }

    if (searchInput) searchInput.addEventListener('input', applyFilters);
    if (cityFilter) cityFilter.addEventListener('change', applyFilters);
    if (statusFilter) statusFilter.addEventListener('change', applyFilters);

    if (btnReset) {
        btnReset.addEventListener('click', function () {
            if (searchInput) searchInput.value = '';
            if (cityFilter) cityFilter.value = 'ALL';
            if (statusFilter) statusFilter.value = 'ALL';
            applyFilters();
        });
    }
});
</script>
</body>
</html>
