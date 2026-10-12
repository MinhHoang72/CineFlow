<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.Movie,model.Branch,java.util.List,java.util.Set,java.util.HashSet,java.util.Map,java.util.LinkedHashMap" %>
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
    List<Branch> branches = (List<Branch>) request.getAttribute("branches");
    if (branches == null) branches = java.util.Collections.emptyList();
    Set<Integer> exceptionBranchIds = (Set<Integer>) request.getAttribute("exceptionBranchIds");
    if (exceptionBranchIds == null) exceptionBranchIds = new HashSet<>();
    String distributionScope = (String) request.getAttribute("distributionScope");
    if (!"system".equals(distributionScope)) distributionScope = "city";
    String distributionCity = cityKey((String) request.getAttribute("distributionCity"));
    
    int totalActiveBranches = 0;
    int systemExceptionCount = 0;
    Map<String, Integer> cityCounts = new LinkedHashMap<>();
    Map<String, Integer> cityActiveCounts = new LinkedHashMap<>();
    Map<String, Integer> cityDistributedCounts = new LinkedHashMap<>();
    
    for (Branch branch : branches) {
        String city = cityKey(branch.getCity());
        cityCounts.put(city, cityCounts.getOrDefault(city, 0) + 1);
        if ("ACTIVE".equalsIgnoreCase(branch.getStatus())) {
            totalActiveBranches++;
            cityActiveCounts.put(city, cityActiveCounts.getOrDefault(city, 0) + 1);
            if (branch.isDistributed()) {
                cityDistributedCounts.put(city, cityDistributedCounts.getOrDefault(city, 0) + 1);
            }
            if (exceptionBranchIds.contains(branch.getBranchId())) {
                systemExceptionCount++;
            }
        }
    }
    if (cityCounts.isEmpty()) distributionScope = "system";
    
    if (distributionCity.isEmpty() && !cityCounts.isEmpty()) {
        distributionCity = cityCounts.keySet().iterator().next();
    }

    int initRecipients = "system".equals(distributionScope) 
        ? (totalActiveBranches - systemExceptionCount) 
        : cityDistributedCounts.getOrDefault(distributionCity, 0);
    int initTotal = "system".equals(distributionScope) 
        ? totalActiveBranches 
        : cityActiveCounts.getOrDefault(distributionCity, 0);

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
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/distribution.css">
</head>
<body>
<div class="cf-manager-shell">
    <jsp:include page="/admin/sidebar.jsp" />

    <div class="cf-manager-main">
        <jsp:include page="/admin/topbar.jsp" />

        <main class="cf-content">
            <!-- PAGE HEADER -->
            <div class="cf-page-header">
                <div>
                    <p class="cf-breadcrumb">Quản trị nội dung / Phân phối phim</p>
                    <h1>Thiết lập phân phối rạp chiếu</h1>
                    <p>Cấu hình phạm vi phát hành phim theo toàn hệ thống hoặc theo cụm thành phố.</p>
                </div>
                <a href="${pageContext.request.contextPath}/admin/movies" class="cf-btn cf-btn-secondary cf-btn-sm">
                     Quay lại danh mục
                </a>
            </div>

            <!-- SUCCESS ALERT -->
            <% if ("1".equals(request.getParameter("saved"))) { %>
                <div class="cf-alert cf-alert-success">
                    Đã lưu cấu hình phân phối rạp chiếu thành công.
                </div>
            <% } %>

            <!-- 1. THẺ THÔNG TIN PHIM (ĐỒNG BỘ VỚI MẪU THAM CHIẾU) -->
            <section class="cf-card" style="margin-bottom: 20px;">
                <div class="cf-dist-movie-info">
                    <div class="cf-dist-movie-poster">
                        <% if (movie != null && movie.getPosterUrl() != null && !movie.getPosterUrl().isBlank()) { %>
                            <img src="<%= movie.getPosterUrl() %>" alt="<%= esc(movie.getTitle()) %>" loading="lazy" onerror="this.parentElement.innerHTML='🎬';">
                        <% } else { %>
                            🎬
                        <% } %>
                    </div>
                    <div class="cf-dist-movie-details">
                        <div class="cf-dist-movie-title-row">
                            <h2><%= movie != null ? esc(movie.getTitle()) : "Chưa chọn phim" %></h2>
                            <% 
                                String st = movie != null ? movie.getStatus() : "";
                                String stText = "NOW_SHOWING".equals(st) ? "Đang chiếu" :
                                                "COMING_SOON".equals(st) ? "Sắp chiếu" :
                                                "ENDED".equals(st) ? "Đã kết thúc" : "Lưu trữ";
                                String badgeClass = "NOW_SHOWING".equals(st) ? "cf-badge-success" :
                                                    "COMING_SOON".equals(st) ? "cf-badge-info" :
                                                    "ENDED".equals(st) ? "cf-badge-warning" : "cf-badge-muted";
                                String age = (movie != null && movie.getAgeRating() != null && !movie.getAgeRating().isBlank()) ? movie.getAgeRating() : "P";
                            %>
                            <span class="cf-badge <%= badgeClass %>"><%= stText %></span>
                            <span class="cf-badge" style="background: #edf1f4; color: var(--cf-text); font-weight: 700;"><%= age %></span>
                        </div>
                        
                        <div class="cf-dist-movie-code">
                            MÃ PHIM · CF-<%= movie != null ? String.format("%04d", movie.getMovieId()) : "0000" %>
                        </div>

                        <!-- Đường phân cách ngang mảnh -->
                        <div class="cf-dist-movie-divider"></div>

                        <!-- 4 Cột thông tin phụ -->
                        <div class="cf-dist-movie-meta-grid">
                            <div class="cf-dist-meta-col">
                                <span class="cf-dist-meta-label">ĐẠO DIỄN</span>
                                <strong class="cf-dist-meta-value"><%= (movie != null && movie.getDirector() != null && !movie.getDirector().isBlank()) ? esc(movie.getDirector()) : "—" %></strong>
                            </div>
                            <div class="cf-dist-meta-col">
                                <span class="cf-dist-meta-label">THỜI LƯỢNG</span>
                                <strong class="cf-dist-meta-value"><%= (movie != null && movie.getDuration() > 0) ? (movie.getDuration() + " phút") : "—" %></strong>
                            </div>
                            <div class="cf-dist-meta-col">
                                <span class="cf-dist-meta-label">THỂ LOẠI</span>
                                <strong class="cf-dist-meta-value"><%= (movie != null && movie.getGenre() != null && !movie.getGenre().isBlank()) ? esc(movie.getGenre()) : "Chưa cập nhật" %></strong>
                            </div>
                            <div class="cf-dist-meta-col">
                                <span class="cf-dist-meta-label">KHỞI CHIẾU</span>
                                <strong class="cf-dist-meta-value"><%= (movie != null && movie.getReleaseDate() != null) ? movie.getReleaseDate() : "—" %></strong>
                            </div>
                        </div>
                    </div>
                </div>
            </section>

            <!-- 2. KHU VỰC THIẾT LẬP PHÂN PHỐI RẠP CHIẾU -->
            <form id="distForm" action="${pageContext.request.contextPath}/admin/distribution" method="POST">
                <input type="hidden" name="movieId" value="<%= movie != null ? movie.getMovieId() : 0 %>">
                <input type="hidden" name="city" id="cityHiddenInput" value="<%= esc(distributionCity) %>">
                <input type="radio" name="scope" value="system" id="scopeRadioSystem" <%= "system".equals(distributionScope) ? "checked" : "" %> style="display: none;">
                <input type="radio" name="scope" value="city" id="scopeRadioCity" <%= "city".equals(distributionScope) ? "checked" : "" %> style="display: none;">

                <section class="cf-card" style="margin-bottom: 24px;">
                    <!-- THANH CHỌN PHẠM VI (GỌN GÀNG, KHÔNG DÙNG VIỀN ĐỎ DÀY) -->
                    <div class="cf-card-toolbar">
                        <div class="cf-toolbar-left">
                            <span style="font-size: 13px; font-weight: 700; color: var(--cf-text); margin-right: 6px;">Phạm vi phân phối:</span>
                            <div class="cf-filter-pills">
                                <button type="button" class="cf-pill <%= "system".equals(distributionScope) ? "active" : "" %>" id="tabBtnScopeSystem">
                                    Toàn hệ thống
                                </button>
                                <button type="button" class="cf-pill <%= "city".equals(distributionScope) ? "active" : "" %>" id="tabBtnScopeCity">
                                    Theo cụm thành phố
                                </button>
                            </div>
                        </div>
                    </div>

                    <!-- CHẾ ĐỘ A: TOÀN HỆ THỐNG -->
                    <div id="modeSystemContainer" style="<%= "system".equals(distributionScope) ? "" : "display: none;" %>">
                        <div class="cf-system-info-bar">
                            <span style="font-size: 13px; color: var(--cf-text);">
                                Mặc định toàn bộ <strong><%= totalActiveBranches %></strong> cụm rạp đang hoạt động trên hệ thống đều nhận chiếu phim này.
                            </span>
                            <button type="button" class="cf-btn cf-btn-secondary cf-btn-sm" id="btnExceptionToggle">
                                <span>Rạp ngoại lệ</span>
                                <span class="cf-city-counter" id="badgeExceptionCount" style="margin-left: 4px;"><%= systemExceptionCount %></span>
                                <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.5" id="iconChevron" style="transition: transform 0.2s;"><polyline points="6 9 12 15 18 9"></polyline></svg>
                            </button>
                        </div>

                        <!-- EXCEPTION DRAWER MỞ RỘNG NGAY DƯỚI NÚT -->
                        <div class="cf-dist-exception-panel" id="drawerException" style="display: none;">
                            <div class="cf-dist-exception-header">
                                <div class="cf-search-box">
                                    <input type="text" id="inputSearchException" placeholder="Tìm theo tên rạp, thành phố, địa chỉ...">
                                </div>
                                <span style="font-size: 12px; color: var(--cf-muted);">Đánh dấu checkbox để loại trừ rạp không nhận phim.</span>
                            </div>

                            <div class="cf-branch-list-header">
                                <div class="cf-col-header col-left">Cụm rạp ngoại lệ</div>
                                <div class="cf-col-header col-center">Trạng thái rạp</div>
                                <div class="cf-col-header col-right">Phân phối phim</div>
                            </div>

                            <div id="exceptionListWrapper">
                                <%
                                    for (Branch b : branches) {
                                        boolean isActive = "ACTIVE".equalsIgnoreCase(b.getStatus());
                                        if (!isActive) continue;
                                        boolean isExcluded = exceptionBranchIds.contains(b.getBranchId());
                                %>
                                <div class="cf-branch-item system-branch-row <%= isExcluded ? "is-excluded" : "" %>"
                                     data-name="<%= esc(b.getBranchName().toLowerCase()) %>"
                                     data-city="<%= esc(cityKey(b.getCity()).toLowerCase()) %>"
                                     data-address="<%= esc(b.getAddress().toLowerCase()) %>">
                                    <div class="cf-branch-left">
                                        <label class="cf-branch-label">
                                            <input type="checkbox" name="exceptionBranchIds" value="<%= b.getBranchId() %>"
                                                   class="cf-branch-checkbox system-exc-check"
                                                   <%= isExcluded ? "checked" : "" %>>
                                            <span class="cf-branch-info">
                                                <span class="cf-branch-title"><%= esc(b.getBranchName()) %></span>
                                                <span class="cf-branch-sub"><%= esc(cityLabel(b.getCity())) %> · <%= esc(b.getAddress()) %></span>
                                            </span>
                                        </label>
                                    </div>
                                    <div class="cf-branch-center">
                                        <span class="cf-op-status active">
                                            <span class="cf-dot"></span> Hoạt động
                                        </span>
                                    </div>
                                    <div class="cf-branch-right">
                                        <span class="cf-dist-state <%= isExcluded ? "state-excluded" : "state-default" %> item-status-badge">
                                            <%= isExcluded ? "Đã loại trừ" : "Nhận phim (Mặc định)" %>
                                        </span>
                                    </div>
                                </div>
                                <% } %>
                            </div>
                        </div>
                    </div>

                    <!-- CHẾ ĐỘ B: THEO CỤM THÀNH PHỐ -->
                    <div id="modeCityContainer" style="<%= "city".equals(distributionScope) ? "" : "display: none;" %>">
                        <!-- SUB-TOOLBAR: CITY TABS & QUICK ACTIONS -->
                        <div class="cf-city-subbar">
                            <div class="cf-city-tabs" id="cityTabsContainer">
                                <% 
                                    for (Map.Entry<String, Integer> entry : cityCounts.entrySet()) {
                                        String cName = entry.getKey();
                                        boolean isCur = cName.equals(distributionCity);
                                        int cActive = cityActiveCounts.getOrDefault(cName, 0);
                                        int cDist = cityDistributedCounts.getOrDefault(cName, 0);
                                %>
                                <button type="button" class="cf-city-tab <%= isCur ? "active" : "" %>" data-city-name="<%= esc(cName) %>">
                                    <span class="cf-city-name"><%= esc(cityLabel(cName)) %></span>
                                    <span class="cf-city-counter" id="tabBadge-<%= cName.replace(" ", "_") %>"><%= cDist %>/<%= cActive %></span>
                                </button>
                                <% } %>
                            </div>

                            <div class="cf-city-quick-actions">
                                <button type="button" class="cf-btn-text" id="btnCityCheckAll">Chọn tất cả</button>
                                <span style="color: var(--cf-border); font-size: 11px;">·</span>
                                <button type="button" class="cf-btn-text" id="btnCityUncheckAll">Bỏ chọn tất cả</button>
                            </div>
                        </div>

                        <!-- CỘT TIÊU ĐỀ BẢNG DANH SÁCH -->
                        <div class="cf-branch-list-header">
                            <div class="cf-col-header col-left">Cụm rạp</div>
                            <div class="cf-col-header col-center">Trạng thái rạp</div>
                            <div class="cf-col-header col-right">Phân phối phim</div>
                        </div>

                        <!-- DANH SÁCH RẠP THEO THÀNH PHỐ -->
                        <div id="cityPanelsContainer">
                            <%
                                for (String cName : cityCounts.keySet()) {
                                    boolean isCurCity = cName.equals(distributionCity);
                            %>
                            <div class="city-panel-item" id="cityPanel-<%= cName.replace(" ", "_") %>" data-city-name="<%= esc(cName) %>" style="<%= isCurCity ? "" : "display: none;" %>">
                                <%
                                    for (Branch b : branches) {
                                        if (cityKey(b.getCity()).equals(cName)) {
                                            boolean isActive = "ACTIVE".equalsIgnoreCase(b.getStatus());
                                            boolean isDistributed = b.isDistributed();
                                %>
                                <div class="cf-branch-item city-branch-row <%= !isActive ? "is-disabled" : "" %>" data-branch-id="<%= b.getBranchId() %>">
                                    <!-- BÊN TRÁI: Checkbox + Tên rạp + Mã/Địa chỉ -->
                                    <div class="cf-branch-left">
                                        <% if (isActive) { %>
                                            <label class="cf-branch-label">
                                                <input type="checkbox" name="branchIds" value="<%= b.getBranchId() %>"
                                                       class="cf-branch-checkbox city-branch-check"
                                                       data-city="<%= esc(cName) %>"
                                                       <%= isDistributed ? "checked" : "" %>>
                                                <span class="cf-branch-info">
                                                    <span class="cf-branch-title"><%= esc(b.getBranchName()) %></span>
                                                    <span class="cf-branch-sub">Mã: <%= b.getBranchId() %> · <%= esc(b.getAddress()) %></span>
                                                </span>
                                            </label>
                                        <% } else { %>
                                            <div class="cf-branch-label" style="cursor: not-allowed;">
                                                <input type="checkbox" disabled class="cf-branch-checkbox">
                                                <span class="cf-branch-info">
                                                    <span class="cf-branch-title"><%= esc(b.getBranchName()) %></span>
                                                    <span class="cf-branch-sub">Mã: <%= b.getBranchId() %> · <%= esc(b.getAddress()) %></span>
                                                </span>
                                            </div>
                                        <% } %>
                                    </div>

                                    <!-- Ở GIỮA: Trạng thái hoạt động nhỏ gọn -->
                                    <div class="cf-branch-center">
                                        <% if (isActive) { %>
                                            <span class="cf-op-status active">
                                                <span class="cf-dot"></span> Hoạt động
                                            </span>
                                        <% } else { %>
                                            <span class="cf-op-status inactive">
                                                <span class="cf-dot"></span> Tạm ngưng
                                            </span>
                                        <% } %>
                                    </div>

                                    <!-- BÊN PHẢI: Trạng thái lựa chọn tinh tế -->
                                    <div class="cf-branch-right">
                                        <% if (isActive) { %>
                                            <span class="cf-dist-state <%= isDistributed ? "state-showing" : "state-none" %> city-status-tag">
                                                <%= isDistributed ? "Đang chiếu" : "Không chiếu" %>
                                            </span>
                                        <% } else { %>
                                            <span class="cf-dist-state state-disabled">Không khả dụng</span>
                                        <% } %>
                                    </div>
                                </div>
                                <% }} %>
                            </div>
                            <% } %>
                        </div>
                    </div>

                    <!-- 3. THANH TÓM TẮT VÀ LƯU THIẾT LẬP (STICKY GỌN GÀNG) -->
                    <div class="cf-dist-sticky-bar">
                        <div class="cf-dist-summary-line" id="footerSummaryContainer">
                            Phạm vi: <strong id="footerScopeText"><%= "system".equals(distributionScope) ? "Toàn hệ thống" : distributionCity %></strong> · <strong id="footerLiveTargetCount"><%= initRecipients %>/<%= initTotal %></strong> rạp được chọn
                        </div>

                        <div class="cf-dist-actions">
                            <a href="${pageContext.request.contextPath}/admin/movies" class="cf-btn cf-btn-secondary cf-btn-sm">
                                Hủy bỏ
                            </a>
                            <button type="submit" class="cf-btn cf-btn-primary cf-btn-sm" id="btnSubmitDistribution">
                                Lưu thiết lập phân phối
                            </button>
                        </div>
                    </div>
                </section>
            </form>
        </main>
    </div>
</div>

<!-- CLIENT CONTROLLER JAVASCRIPT -->
<script>
document.addEventListener('DOMContentLoaded', function() {
    const radioSystem = document.getElementById('scopeRadioSystem');
    const radioCity = document.getElementById('scopeRadioCity');
    const tabBtnScopeSystem = document.getElementById('tabBtnScopeSystem');
    const tabBtnScopeCity = document.getElementById('tabBtnScopeCity');
    const modeSystem = document.getElementById('modeSystemContainer');
    const modeCity = document.getElementById('modeCityContainer');
    const cityHiddenInput = document.getElementById('cityHiddenInput');

    const footerScopeText = document.getElementById('footerScopeText');
    const footerLiveTarget = document.getElementById('footerLiveTargetCount');
    const btnSubmit = document.getElementById('btnSubmitDistribution');
    const distForm = document.getElementById('distForm');

    const totalActive = <%= totalActiveBranches %>;

    // ==========================================
    // 1. CHUYỂN PHẠM VI (TOÀN HỆ THỐNG VS THEO CỤM)
    // ==========================================
    function setScope(scope) {
        const isSystem = (scope === 'system');
        radioSystem.checked = isSystem;
        radioCity.checked = !isSystem;

        tabBtnScopeSystem.classList.toggle('active', isSystem);
        tabBtnScopeCity.classList.toggle('active', !isSystem);

        modeSystem.style.display = isSystem ? 'block' : 'none';
        modeCity.style.display = isSystem ? 'none' : 'block';

        // Chỉ gửi inputs của chế độ đang kích hoạt
        document.querySelectorAll('.system-exc-check').forEach(i => {
            i.disabled = !isSystem;
        });
        syncCityInputStates();
        updateAllCounts();
    }

    if (tabBtnScopeSystem) {
        tabBtnScopeSystem.addEventListener('click', function() { setScope('system'); });
    }
    if (tabBtnScopeCity) {
        tabBtnScopeCity.addEventListener('click', function() { setScope('city'); });
    }

    // ==========================================
    // 2. CHẾ ĐỘ TOÀN HỆ THỐNG: MỞ/ĐÓNG RẠP NGOẠI LỆ
    // ==========================================
    const btnExceptionToggle = document.getElementById('btnExceptionToggle');
    const drawerException = document.getElementById('drawerException');
    const iconChevron = document.getElementById('iconChevron');

    if (btnExceptionToggle && drawerException) {
        btnExceptionToggle.addEventListener('click', function(e) {
            e.preventDefault();
            const isOpen = drawerException.style.display !== 'none';
            drawerException.style.display = isOpen ? 'none' : 'block';
            if (iconChevron) {
                iconChevron.style.transform = isOpen ? 'rotate(0deg)' : 'rotate(180deg)';
            }
            if (!isOpen) {
                const searchInp = document.getElementById('inputSearchException');
                if (searchInp) searchInp.focus();
            }
        });
    }

    // TÌM KIẾM RẠP TRONG DANH SÁCH NGOẠI LỆ
    const inputSearchException = document.getElementById('inputSearchException');
    if (inputSearchException) {
        inputSearchException.addEventListener('input', function() {
            const q = this.value.trim().toLowerCase();
            const rows = document.querySelectorAll('#exceptionListWrapper .system-branch-row');
            rows.forEach(r => {
                const name = r.dataset.name || '';
                const city = r.dataset.city || '';
                const addr = r.dataset.address || '';
                const match = name.includes(q) || city.includes(q) || addr.includes(q);
                r.style.display = match ? 'flex' : 'none';
            });
        });
    }

    // CHECKBOX NGOẠI LỆ THAY ĐỔI
    const systemExcChecks = document.querySelectorAll('.system-exc-check');
    systemExcChecks.forEach(cb => {
        cb.addEventListener('change', function() {
            const row = this.closest('.system-branch-row');
            const badge = row ? row.querySelector('.item-status-badge') : null;
            if (this.checked) {
                if (row) row.classList.add('is-excluded');
                if (badge) {
                    badge.className = 'cf-dist-state state-excluded item-status-badge';
                    badge.textContent = 'Đã loại trừ';
                }
            } else {
                if (row) row.classList.remove('is-excluded');
                if (badge) {
                    badge.className = 'cf-dist-state state-default item-status-badge';
                    badge.textContent = 'Nhận phim (Mặc định)';
                }
            }
            updateAllCounts();
        });
    });

    // ==========================================
    // 3. CHẾ ĐỘ THEO CỤM: TAB THÀNH PHỐ & CHỌN RẠP
    // ==========================================
    const cityTabBtns = document.querySelectorAll('.cf-city-tab');
    cityTabBtns.forEach(tab => {
        tab.addEventListener('click', function() {
            const targetCity = this.dataset.cityName;
            cityHiddenInput.value = targetCity;

            cityTabBtns.forEach(t => t.classList.toggle('active', t.dataset.cityName === targetCity));

            const panels = document.querySelectorAll('.city-panel-item');
            panels.forEach(p => {
                p.style.display = (p.dataset.cityName === targetCity) ? 'block' : 'none';
            });

            syncCityInputStates();
            updateAllCounts();
        });
    });

    function syncCityInputStates() {
        const isCityMode = radioCity.checked;
        const curCity = cityHiddenInput.value;
        const panels = document.querySelectorAll('.city-panel-item');

        panels.forEach(panel => {
            const isCur = (panel.dataset.cityName === curCity);
            const inputs = panel.querySelectorAll('.city-branch-check');
            inputs.forEach(input => {
                input.disabled = !isCityMode || !isCur;
            });
        });
    }

    // ĐỒNG BỘ TRẠNG THÁI HIỂN THỊ CỦA DÒNG RẠP (ĐANG CHIẾU / KHÔNG CHIẾU)
    function syncCityBranchVisual(chk) {
        const row = chk.closest('.city-branch-row');
        const tag = row ? row.querySelector('.city-status-tag') : null;
        if (tag) {
            tag.className = 'cf-dist-state ' + (chk.checked ? 'state-showing' : 'state-none') + ' city-status-tag';
            tag.textContent = chk.checked ? 'Đang chiếu' : 'Không chiếu';
        }
    }

    const cityChecks = document.querySelectorAll('.city-branch-check');
    cityChecks.forEach(chk => {
        chk.addEventListener('change', function() {
            syncCityBranchVisual(this);
            updateCityBadge(this.dataset.city);
            updateAllCounts();
        });
    });

    // THAO TÁC NHANH: CHỌN TẤT CẢ / BỎ CHỌN TẤT CẢ
    const btnCityCheckAll = document.getElementById('btnCityCheckAll');
    if (btnCityCheckAll) {
        btnCityCheckAll.addEventListener('click', function() {
            const curCity = cityHiddenInput.value;
            const panel = document.getElementById('cityPanel-' + curCity.replace(/\s+/g, '_'));
            if (!panel) return;
            panel.querySelectorAll('.city-branch-check').forEach(chk => {
                if (!chk.disabled) {
                    chk.checked = true;
                    syncCityBranchVisual(chk);
                }
            });
            updateCityBadge(curCity);
            updateAllCounts();
        });
    }

    const btnCityUncheckAll = document.getElementById('btnCityUncheckAll');
    if (btnCityUncheckAll) {
        btnCityUncheckAll.addEventListener('click', function() {
            const curCity = cityHiddenInput.value;
            const panel = document.getElementById('cityPanel-' + curCity.replace(/\s+/g, '_'));
            if (!panel) return;
            panel.querySelectorAll('.city-branch-check').forEach(chk => {
                if (!chk.disabled) {
                    chk.checked = false;
                    syncCityBranchVisual(chk);
                }
            });
            updateCityBadge(curCity);
            updateAllCounts();
        });
    }

    function updateCityBadge(city) {
        const panel = document.getElementById('cityPanel-' + city.replace(/\s+/g, '_'));
        const badge = document.getElementById('tabBadge-' + city.replace(/\s+/g, '_'));
        if (panel && badge) {
            const selectedCount = panel.querySelectorAll('.city-branch-check:checked').length;
            const activeTotal = panel.querySelectorAll('.city-branch-check').length;
            badge.textContent = selectedCount + '/' + activeTotal;
        }
    }

    // ==========================================
    // 4. CẬP NHẬT TỔNG HỢP VÀ DÒNG TÓM TẮT DƯỚI ĐÁY
    // Format: "Phạm vi: Đà Nẵng · 1/1 rạp được chọn"
    // ==========================================
    function updateAllCounts() {
        const isSystem = radioSystem.checked;

        if (isSystem) {
            const excludedCount = document.querySelectorAll('.system-exc-check:checked:not(:disabled)').length;
            const distributed = Math.max(0, totalActive - excludedCount);

            const badgeExc = document.getElementById('badgeExceptionCount');
            if (badgeExc) {
                badgeExc.textContent = excludedCount;
            }

            if (footerScopeText) footerScopeText.textContent = 'Toàn hệ thống';
            if (footerLiveTarget) footerLiveTarget.textContent = distributed + '/' + totalActive;
        } else {
            const curCity = cityHiddenInput.value;
            const curPanel = document.getElementById('cityPanel-' + curCity.replace(/\s+/g, '_'));
            const cityRecip = curPanel ? curPanel.querySelectorAll('.city-branch-check:checked').length : 0;
            const cityActive = curPanel ? curPanel.querySelectorAll('.city-branch-check').length : 0;

            if (footerScopeText) footerScopeText.textContent = curCity;
            if (footerLiveTarget) footerLiveTarget.textContent = cityRecip + '/' + cityActive;
        }
    }

    // PHẢN HỒI NÚT LƯU FORM
    if (distForm && btnSubmit) {
        distForm.addEventListener('submit', function() {
            btnSubmit.classList.add('is-submitting');
            btnSubmit.textContent = 'Đang lưu thiết lập...';
        });
    }

    // KHỞI TẠO BAN ĐẦU
    syncCityInputStates();
    updateAllCounts();
});
</script>
</body>
</html>
