<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.Movie,model.Centre,java.util.List,java.util.Map,java.util.LinkedHashMap" %>
<%
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && session.getAttribute("user") instanceof model.User) {
        authRole = ((model.User) session.getAttribute("user")).getRole();
    }
    if (!"ADMIN".equalsIgnoreCase(authRole)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Quản trị viên (ADMIN) mới có quyền truy cập trang này.");
        return;
    }
%>
<%!
    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
    private String cityKey(String city) {
        return city == null || city.isBlank() ? "" : city.trim();
    }
    private String cityLabel(String city) {
        return city == null || city.isBlank() ? "Chưa phân loại" : city.trim();
    }
%>
<%
    Movie movie = (Movie) request.getAttribute("movie");
    if (movie == null) {
        response.sendRedirect(request.getContextPath() + "/admin/movies");
        return;
    }
    List<Centre> centres = (List<Centre>) request.getAttribute("centres");
    if (centres == null) centres = java.util.Collections.emptyList();

    int totalActiveCentres = 0;
    int totalDistributed = 0;
    Map<String, Integer> cityActiveCounts = new LinkedHashMap<>();
    Map<String, Integer> cityDistributedCounts = new LinkedHashMap<>();

    for (Centre centre : centres) {
        String city = cityKey(centre.getCity());
        if ("ACTIVE".equalsIgnoreCase(centre.getStatus())) {
            totalActiveCentres++;
            cityActiveCounts.put(city, cityActiveCounts.getOrDefault(city, 0) + 1);
            if (centre.isDistributed()) {
                totalDistributed++;
                cityDistributedCounts.put(city, cityDistributedCounts.getOrDefault(city, 0) + 1);
            }
        }
    }

    request.setAttribute("activeNav", "movies");
    request.setAttribute("pageTitle", "Phân phối: " + (movie != null ? movie.getTitle() : "Phim"));
    request.setAttribute("pageBreadcrumb", "Phân phối phim");
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
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/forms.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/tables.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/alerts.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/shell.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/dashboard.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/distribution.css?v=<%= System.currentTimeMillis() %>">
    </head>
    <body>
        <div class="cf-manager-shell">
            <jsp:include page="/admin/sidebar.jsp" />

            <div class="cf-manager-main">
                <jsp:include page="/admin/topbar.jsp" />

                <main class="cf-content">
                    <!-- TIÊU ĐỀ TRANG -->
                    <div class="cf-page-header">
                        <div>
                            <p class="cf-breadcrumb">Quản trị nội dung / Phân phối phim</p>
                            <h1>Thiết lập phân phối trung tâm chiếu</h1>
                            <p>Cấp phép phát hành phim cho các trung tâm hoạt động trên toàn hệ thống.</p>
                        </div>
                        <a href="${pageContext.request.contextPath}/admin/movies" class="cf-btn cf-btn-secondary cf-btn-sm">
                            Quay lại danh mục
                        </a>
                    </div>

                    <!-- THÔNG BÁO THÀNH CÔNG -->

                    <% if ("1".equals(request.getParameter("saved"))) { %>
                    <div id="autoDismissAlert" class="cf-alert cf-alert-success">
                        Đã lưu cấu hình phân phối trung tâm chiếu thành công.
                    </div>
                    <% } %>

                    <script>
                        document.addEventListener('DOMContentLoaded', function () {
                            const alert = document.getElementById('autoDismissAlert');

                            if (alert) {
                                setTimeout(function () {
                                    alert.style.transition = 'opacity 0.4s ease, transform 0.4s ease';
                                    alert.style.opacity = '0';
                                    alert.style.transform = 'translateY(-8px)';

                                    setTimeout(function () {
                                        alert.remove();
                                    }, 400);
                                }, 2000);
                            }
                        });
                    </script>

                    <!-- 1. THẺ THÔNG TIN PHIM -->
                    <section class="cf-card" style="margin-bottom: 20px;">
                        <div class="cf-dist-movie-info">
                            <div class="cf-dist-movie-poster">
                                <% if (movie.getPosterUrl() != null && !movie.getPosterUrl().isBlank()) { %>
                                <img src="<%= movie.getPosterUrl() %>" alt="<%= esc(movie.getTitle()) %>" loading="lazy" onerror="this.parentElement.innerHTML='🎬';">
                                <% } else { %>
                                🎬
                                <% } %>
                            </div>
                            <div class="cf-dist-movie-details">
                                <div class="cf-dist-movie-title-row">
                                    <h2><%= esc(movie.getTitle()) %></h2>
                                    <% 
                                        String st = movie.getStatus() != null ? movie.getStatus() : "";
                                        String stText = "NOW_SHOWING".equals(st) ? "Đang chiếu" :
                                                        "COMING_SOON".equals(st) ? "Sắp chiếu" :
                                                        "ENDED".equals(st) ? "Đã kết thúc" : "Lưu trữ";
                                        String badgeClass = "NOW_SHOWING".equals(st) ? "cf-badge-success" :
                                                            "COMING_SOON".equals(st) ? "cf-badge-info" :
                                                            "ENDED".equals(st) ? "cf-badge-warning" : "cf-badge-muted";
                                        String age = (movie.getAgeRating() != null && !movie.getAgeRating().isBlank()) ? movie.getAgeRating() : "P";
                                    %>
                                    <span class="cf-badge <%= badgeClass %>"><%= stText %></span>
                                    <span class="cf-badge cf-badge-muted"><%= age %></span>
                                </div>

                                <div class="cf-dist-movie-code">
                                    MÃ PHIM · CF-<%= String.format("%04d", movie.getMovieId()) %>
                                </div>

                                <div class="cf-dist-movie-divider"></div>

                                <div class="cf-dist-movie-meta-grid">
                                    <div class="cf-dist-meta-col">
                                        <span class="cf-dist-meta-label">ĐẠO DIỄN</span>
                                        <strong class="cf-dist-meta-value"><%= (movie.getDirector() != null && !movie.getDirector().isBlank()) ? esc(movie.getDirector()) : "—" %></strong>
                                    </div>
                                    <div class="cf-dist-meta-col">
                                        <span class="cf-dist-meta-label">THỜI LƯỢNG</span>
                                        <strong class="cf-dist-meta-value"><%= (movie.getDuration() > 0) ? (movie.getDuration() + " phút") : "—" %></strong>
                                    </div>
                                    <div class="cf-dist-meta-col">
                                        <span class="cf-dist-meta-label">THỂ LOẠI</span>
                                        <strong class="cf-dist-meta-value"><%= (movie.getGenre() != null && !movie.getGenre().isBlank()) ? esc(movie.getGenre()) : "Chưa cập nhật" %></strong>
                                    </div>
                                    <div class="cf-dist-meta-col">
                                        <span class="cf-dist-meta-label">KHỞI CHIẾU</span>
                                        <strong class="cf-dist-meta-value"><%= (movie.getReleaseDate() != null) ? movie.getReleaseDate() : "—" %></strong>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </section>

                    <!-- 2. WORKSPACE PHÂN PHỐI RẠP CHIẾU (CHUẨN WHITELIST) -->
                    <form id="distForm" action="${pageContext.request.contextPath}/admin/distribution" method="POST">
                        <input type="hidden" name="movieId" value="<%= movie.getMovieId() %>">

                        <section class="cf-card" style="margin-bottom: 24px;">
                            <!-- THANH CÔNG CỤ TOOLBAR HIỆN ĐẠI -->
                            <div class="cf-dist-toolbar">
                                <div class="cf-dist-toolbar-left">
                                    <div class="cf-filter-pills" id="cityTabsContainer">
                                        <button type="button" class="cf-pill active" data-city="all">
                                            <span>Tất cả cụm rạp</span>
                                            <span class="cf-pill-count" id="badgeCity-all"><%= totalDistributed %>/<%= totalActiveCentres %></span>
                                        </button>
                                        <% 
                                            for (Map.Entry<String, Integer> entry : cityActiveCounts.entrySet()) {
                                                String cName = entry.getKey();
                                                int cActive = entry.getValue();
                                                int cDist = cityDistributedCounts.getOrDefault(cName, 0);
                                        %>
                                        <button type="button" class="cf-pill" data-city="<%= esc(cName) %>">
                                            <span><%= esc(cityLabel(cName)) %></span>
                                            <span class="cf-pill-count" id="badgeCity-<%= esc(cName.replace(" ", "_")) %>"><%= cDist %>/<%= cActive %></span>
                                        </button>
                                        <% } %>
                                    </div>
                                </div>

                                <div class="cf-dist-toolbar-right">
                                    <div class="cf-search-box">
                                        <input type="text" id="inputSearchCentre" placeholder="Tìm kiếm trung tâm, địa chỉ..." autocomplete="off">
                                    </div>
                                    <div class="cf-dist-btn-group">
                                        <button type="button" class="cf-btn cf-btn-secondary cf-btn-sm" id="btnSelectAll" title="Chọn tất cả trung tâm đang hiển thị">
                                            <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
                                            <span>Chọn tất cả</span>
                                        </button>
                                        <button type="button" class="cf-btn cf-btn-ghost cf-btn-sm" id="btnDeselectAll" title="Bỏ chọn tất cả trung tâm đang hiển thị">
                                            <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
                                            <span>Bỏ chọn</span>
                                        </button>
                                    </div>
                                </div>
                            </div>

                            <!-- TIÊU ĐỀ CÁC CỘT -->
                            <div class="cf-centre-list-header">
                                <div class="cf-col-header col-left">Trung tâm chiếu</div>
                                <div class="cf-col-header col-center">Trạng thái trung tâm</div>
                                <div class="cf-col-header col-right">Phân phối phim</div>
                            </div>

                            <!-- DANH SÁCH TRUNG TÂM CHIẾU -->
                            <div id="centreListWrapper">
                                <%
                                    for (Centre c : centres) {
                                        boolean isActive = "ACTIVE".equalsIgnoreCase(c.getStatus());
                                        boolean isDistributed = c.isDistributed();
                                        String cKey = cityKey(c.getCity());
                                %>
                                <div class="cf-centre-item centre-row <%= !isActive ? "is-disabled" : "" %>"
                                     data-city="<%= esc(cKey) %>"
                                     data-name="<%= esc(c.getCentreName().toLowerCase()) %>"
                                     data-address="<%= esc((c.getAddress() != null ? c.getAddress() : "").toLowerCase()) %>"
                                     data-active="<%= isActive ? "1" : "0" %>">
                                    <!-- BÊN TRÁI: Checkbox + Tên trung tâm + Địa chỉ -->
                                    <div class="cf-centre-left">
                                        <% if (isActive) { %>
                                        <label class="cf-centre-label">
                                            <input type="checkbox" name="centreIds" value="<%= c.getCentreId() %>"
                                                   class="cf-centre-checkbox centre-check"
                                                   data-city="<%= esc(cKey) %>"
                                                   <%= isDistributed ? "checked" : "" %>>
                                            <span class="cf-centre-info">
                                                <span class="cf-centre-title"><%= esc(c.getCentreName()) %></span>
                                                <span class="cf-centre-sub"><%= esc(cityLabel(c.getCity())) %> · <%= esc(c.getAddress()) %></span>
                                            </span>
                                        </label>
                                        <% } else { %>
                                        <div class="cf-centre-label" style="cursor: not-allowed;">
                                            <input type="checkbox" disabled class="cf-centre-checkbox">
                                            <span class="cf-centre-info">
                                                <span class="cf-centre-title"><%= esc(c.getCentreName()) %></span>
                                                <span class="cf-centre-sub"><%= esc(cityLabel(c.getCity())) %> · <%= esc(c.getAddress()) %></span>
                                            </span>
                                        </div>
                                        <% } %>
                                    </div>

                                    <!-- Ở GIỮA: Trạng thái vận hành của trung tâm -->
                                    <div class="cf-centre-center">
                                        <% if (isActive) { %>
                                        <span class="cf-op-status active">
                                            <span class="cf-dot"></span> Hoạt động
                                        </span>
                                        <% } else { %>
                                        <span class="cf-op-status inactive">
                                            <span class="cf-dot"></span> Tạm dừng
                                        </span>
                                        <% } %>
                                    </div>

                                    <!-- BÊN PHẢI: Trạng thái phân phối phim -->
                                    <div class="cf-centre-right">
                                        <% if (isActive) { %>
                                        <span class="cf-dist-state <%= isDistributed ? "state-showing" : "state-none" %> item-dist-badge">
                                            <%= isDistributed ? "Được chiếu" : "Không chiếu" %>
                                        </span>
                                        <% } else { %>
                                        <span class="cf-dist-state state-disabled">Không khả dụng</span>
                                        <% } %>
                                    </div>
                                </div>
                                <% } %>

                                <!-- DÒNG THÔNG BÁO KHI KHÔNG TÌM THẤY KẾT QUẢ -->
                                <div id="noMatchRow" style="display: none; padding: 40px 20px; text-align: center; color: var(--cf-muted); font-size: 13.5px;">
                                    Không tìm thấy trung tâm nào phù hợp với từ khóa hoặc bộ lọc đã chọn.
                                </div>
                            </div>

                            <!-- 3. THANH TÓM TẮT & NÚT LƯU (STICKY FOOTER) -->
                            <div class="cf-dist-sticky-bar">
                                <div class="cf-dist-summary-line">
                                    Đã phân phối: <strong id="footerCount"><%= totalDistributed %>/<%= totalActiveCentres %></strong> trung tâm đang hoạt động trên toàn hệ thống
                                </div>

                                <div class="cf-dist-actions">

                                    <button type="submit" class="cf-btn cf-btn-primary cf-btn-sm" id="btnSubmit">
                                        Lưu thiết lập phân phối
                                    </button>
                                </div>
                            </div>
                        </section>
                    </form>
                </main>
            </div>
        </div>

        <!-- MÃ JAVASCRIPT ĐIỀU KHIỂN PHÍA CLIENT -->
        <script>
            document.addEventListener('DOMContentLoaded', function () {
                let currentCityFilter = 'all';
                let searchQuery = '';

                const cityPills = document.querySelectorAll('#cityTabsContainer .cf-pill');
                const inputSearch = document.getElementById('inputSearchCentre');
                const btnSelectAll = document.getElementById('btnSelectAll');
                const btnDeselectAll = document.getElementById('btnDeselectAll');
                const rows = document.querySelectorAll('.centre-row');
                const noMatchRow = document.getElementById('noMatchRow');
                const footerCount = document.getElementById('footerCount');
                const badgeAll = document.getElementById('badgeCity-all');
                const distForm = document.getElementById('distForm');
                const btnSubmit = document.getElementById('btnSubmit');

                // 1. Áp dụng bộ lọc (tab thành phố + từ khóa tìm kiếm)
                function applyFilters() {
                    let visibleCount = 0;
                    rows.forEach(row => {
                        const rowCity = row.dataset.city || '';
                        const rowName = row.dataset.name || '';
                        const rowAddress = row.dataset.address || '';

                        const cityMatch = (currentCityFilter === 'all') || (rowCity === currentCityFilter);
                        const searchMatch = !searchQuery ||
                                rowName.includes(searchQuery) ||
                                rowAddress.includes(searchQuery) ||
                                rowCity.toLowerCase().includes(searchQuery);

                        const isVisible = cityMatch && searchMatch;
                        row.style.display = isVisible ? 'flex' : 'none';
                        if (isVisible)
                            visibleCount++;
                    });

                    if (noMatchRow) {
                        noMatchRow.style.display = (visibleCount === 0) ? 'block' : 'none';
                    }
                }

                // 2. Chuyển đổi Tab Thành phố
                cityPills.forEach(pill => {
                    pill.addEventListener('click', function () {
                        currentCityFilter = this.dataset.city || 'all';
                        cityPills.forEach(p => p.classList.toggle('active', p === pill));
                        applyFilters();
                    });
                });

                // 3. Tìm kiếm tức thì
                if (inputSearch) {
                    inputSearch.addEventListener('input', function () {
                        searchQuery = this.value.trim().toLowerCase();
                        applyFilters();
                    });
                }

                // 4. Đồng bộ nhãn hiển thị khi thay đổi checkbox
                function updateRowVisual(checkbox) {
                    const row = checkbox.closest('.centre-row');
                    const badge = row ? row.querySelector('.item-dist-badge') : null;
                    if (badge) {
                        if (checkbox.checked) {
                            badge.className = 'cf-dist-state state-showing item-dist-badge';
                            badge.textContent = 'Được chiếu';
                        } else {
                            badge.className = 'cf-dist-state state-none item-dist-badge';
                            badge.textContent = 'Không chiếu';
                        }
                    }
                }

                // 5. Cập nhật số đếm thống kê thời gian thực (realtime counters)
                function recalculateCounters() {
                    let totalActive = 0;
                    let totalDistributed = 0;
                    const cityStats = {};

                    rows.forEach(row => {
                        if (row.dataset.active === '1') {
                            totalActive++;
                            const c = row.dataset.city || '';
                            if (!cityStats[c])
                                cityStats[c] = {active: 0, distributed: 0};
                            cityStats[c].active++;

                            const chk = row.querySelector('.centre-check');
                            if (chk && chk.checked) {
                                totalDistributed++;
                                cityStats[c].distributed++;
                            }
                        }
                    });

                    if (badgeAll) {
                        badgeAll.textContent = totalDistributed + '/' + totalActive;
                    }
                    if (footerCount) {
                        footerCount.textContent = totalDistributed + '/' + totalActive;
                    }

                    Object.keys(cityStats).forEach(cName => {
                        const badgeId = 'badgeCity-' + cName.replace(/\s+/g, '_');
                        const badgeEl = document.getElementById(badgeId);
                        if (badgeEl) {
                            badgeEl.textContent = cityStats[cName].distributed + '/' + cityStats[cName].active;
                        }
                    });
                }

                // Lắng nghe sự kiện click checkbox của từng rạp
                document.querySelectorAll('.centre-check').forEach(chk => {
                    chk.addEventListener('change', function () {
                        updateRowVisual(this);
                        recalculateCounters();
                    });
                });

                // 6. Hành động nhanh: Chọn tất cả rạp đang hiển thị
                if (btnSelectAll) {
                    btnSelectAll.addEventListener('click', function () {
                        rows.forEach(row => {
                            if (row.style.display !== 'none' && row.dataset.active === '1') {
                                const chk = row.querySelector('.centre-check');
                                if (chk && !chk.checked) {
                                    chk.checked = true;
                                    updateRowVisual(chk);
                                }
                            }
                        });
                        recalculateCounters();
                    });
                }

                // 7. Hành động nhanh: Bỏ chọn tất cả rạp đang hiển thị
                if (btnDeselectAll) {
                    btnDeselectAll.addEventListener('click', function () {
                        rows.forEach(row => {
                            if (row.style.display !== 'none' && row.dataset.active === '1') {
                                const chk = row.querySelector('.centre-check');
                                if (chk && chk.checked) {
                                    chk.checked = false;
                                    updateRowVisual(chk);
                                }
                            }
                        });
                        recalculateCounters();
                    });
                }

                // 8. Trạng thái khi bấm Lưu
                if (distForm && btnSubmit) {
                    distForm.addEventListener('submit', function () {
                        btnSubmit.classList.add('is-submitting');
                        btnSubmit.textContent = 'Đang lưu thiết lập...';
                    });
                }
            });
        </script>
    </body>
</html>
