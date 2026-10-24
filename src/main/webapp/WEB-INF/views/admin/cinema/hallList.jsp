<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.entity.Hall,model.entity.Centre,model.entity.User,java.util.List" %>
<%
    // 1. Kiểm tra xác thực và quyền hạn (Primary Actor: Centre Manager hoặc Admin có toàn quyền)
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && session.getAttribute("user") instanceof model.entity.User) {
        authRole = ((model.entity.User) session.getAttribute("user")).getRole();
    }
    if (!"MANAGER".equalsIgnoreCase(authRole) && !"ADMIN".equalsIgnoreCase(authRole)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Quản lý cụm rạp (MANAGER) hoặc Quản trị viên (ADMIN) mới có quyền truy cập trang này.");
        return;
    }

    boolean isAdmin = "ADMIN".equalsIgnoreCase(authRole);
    request.setAttribute("activeNav", "halls");
    request.setAttribute("pageTitle", "Quản lý Phòng chiếu");

    Boolean noCentreAssigned = (Boolean) request.getAttribute("noCentreAssigned");
    Centre currentCentre = (Centre) request.getAttribute("currentCentre");
    Integer targetCentreId = (Integer) request.getAttribute("targetCentreId");
    List<Centre> allCentres = (List<Centre>) request.getAttribute("allCentres");
    List<Hall> halls = (List<Hall>) request.getAttribute("halls");

    Integer totalHalls = (Integer) request.getAttribute("totalHalls");
    Integer activeHalls = (Integer) request.getAttribute("activeHalls");
    Integer inactiveHalls = (Integer) request.getAttribute("inactiveHalls");
    Integer totalCapacity = (Integer) request.getAttribute("totalCapacity");

    model.dto.common.PageDTO<?> pg = (model.dto.common.PageDTO<?>) request.getAttribute("page");
    int currentPage = (pg != null) ? pg.getPage() : 1;
    int pageSize    = (pg != null) ? pg.getSize() : 6;
    int totalPages  = (pg != null) ? pg.getTotalPages() : 1;
    int totalFilteredHalls = (pg != null) ? pg.getTotal() : 0;

    String currentSearch = (String) request.getAttribute("currentSearch");
    String currentStatus = (String) request.getAttribute("currentStatus");

    String successParam = request.getParameter("success");
    String errorParam = request.getParameter("error");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quản lý Phòng chiếu | CineFlow</title>

    <!-- Phông chữ chuẩn hệ thống CineFlow -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:ital,wght@0,300;0,400;0,500;0,600;0,700;0,800;1,400;1,600&display=swap" rel="stylesheet">

    <!-- CSS chung của hệ thống -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/reset.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/variables.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/buttons.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/badges.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/forms.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/tables.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/alerts.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/shell.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/dashboard.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/halls.css?v=<%= System.currentTimeMillis() %>">
</head>
<body>
<div class="cf-manager-shell">
    <!-- Sidebar điều hướng tương ứng theo vai trò Admin / Manager -->
    <jsp:include page="<%= isAdmin ? \"/WEB-INF/fragments/adminSidebar.jspf\" : \"/WEB-INF/fragments/managerSidebar.jspf\" %>" />

    <div class="cf-manager-main">
        <!-- Topbar chung -->
        <jsp:include page="/WEB-INF/fragments/adminTopbar.jspf" />

        <main class="cf-content">
            <!-- Header trang -->
            <div class="cf-page-header">
                <div>
                    <h1>Quản lý Phòng chiếu</h1>
                    <p>Danh sách phòng chiếu, định dạng kỹ thuật 2D/3D và sức chứa thiết kế trực thuộc cụm rạp</p>
                </div>
                <% if (Boolean.TRUE.equals(noCentreAssigned)) { %>
                    <!-- Không hiển thị nút thêm khi chưa có cụm rạp -->
                <% } else { %>
                    <button type="button" class="cf-btn cf-btn-primary" onclick="openCreateHallModal()" style="display: flex; align-items: center; gap: 8px;">
                        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
                        Thêm phòng chiếu mới
                    </button>
                <% } %>
            </div>

            <!-- Thông báo kết quả thao tác -->
            <% if ("created".equals(successParam)) { %>
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> <strong>Thành công!</strong> Thêm phòng chiếu mới thành công.
                </div>
            <% } else if ("updated".equals(successParam)) { %>
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> <strong>Thành công!</strong> Cập nhật thông tin phòng chiếu thành công.
                </div>
            <% } else if ("status_updated".equals(successParam)) { %>
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> <strong>Thành công!</strong> Đã cập nhật trạng thái hoạt động của phòng chiếu thành công.
                </div>
            <% } else if ("duplicate_name".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Tên phòng chiếu đã tồn tại tại cụm rạp này. Vui lòng nhập tên khác.
                </div>
            <% } else if ("has_future_bookings".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Cảnh báo:</strong> Không thể vô hiệu hóa phòng chiếu vì đang có suất chiếu đã phát sinh vé đặt.
                </div>
            <% } else if ("missing_fields".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Vui lòng điền đầy đủ các thông tin bắt buộc của phòng chiếu.
                </div>
            <% } else if ("failed".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Không thể thực hiện thao tác. Vui lòng thử lại sau!
                </div>
            <% } %>

            <% if (Boolean.TRUE.equals(noCentreAssigned)) { %>
                <!-- Trường hợp Manager chưa được gán Centre -->
                <div class="cf-alert cf-alert-danger" style="margin-top: 20px;">
                    <span>⚠</span> <strong>Chưa phân công cụm rạp:</strong> Tài khoản Quản lý của bạn hiện chưa được phân công phụ trách Cụm rạp nào. Vui lòng liên hệ Quản trị viên (Admin) để được bổ nhiệm vào Cụm rạp.
                </div>
            <% } else { %>
                <!-- Banner Cụm rạp đang quản lý -->
                <div class="cf-centre-banner">
                    <div class="cf-centre-meta">
                        <div class="cf-centre-icon">🏢</div>
                        <div class="cf-centre-info">
                            <h2><%= currentCentre != null ? currentCentre.getCentreName() : "Cụm rạp #" + targetCentreId %></h2>
                            <p>
                                📍 <%= currentCentre != null ? currentCentre.getAddress() : "" %>
                                <% if (currentCentre != null && currentCentre.getCity() != null && !currentCentre.getCity().trim().isEmpty()) { %>
                                    , <%= currentCentre.getCity() %>
                                <% } %>
                                <% if (currentCentre != null && currentCentre.getPhone() != null && !currentCentre.getPhone().trim().isEmpty()) { %>
                                    &nbsp;|&nbsp; ☎ Hotline: <%= currentCentre.getPhone() %>
                                <% } %>
                            </p>
                        </div>
                    </div>

                    <% if (isAdmin && allCentres != null && allCentres.size() > 1) { %>
                        <!-- Dropdown chuyển đổi Cụm rạp dành cho Admin có toàn quyền -->
                        <div class="cf-centre-switch">
                            <label for="adminCentreSelect">Đổi Cụm rạp:</label>
                            <select id="adminCentreSelect" class="cf-centre-select" onchange="switchCentre(this.value)">
                                <% for (Centre c : allCentres) { %>
                                    <option value="<%= c.getCentreId() %>" <%= (targetCentreId != null && targetCentreId == c.getCentreId()) ? "selected" : "" %>>
                                        <%= c.getCentreName() %> (<%= c.getCity() != null ? c.getCity() : "Cụm rạp #" + c.getCentreId() %>)
                                    </option>
                                <% } %>
                            </select>
                        </div>
                    <% } %>
                </div>

                <!-- Thống kê (Stat Cards) -->
                <div class="cf-stats-grid">
                    <div class="cf-stat-card">
                        <div class="cf-stat-icon primary">🎬</div>
                        <div class="cf-stat-info">
                            <h3><%= totalHalls != null ? totalHalls : 0 %></h3>
                            <p>Tổng số phòng chiếu</p>
                        </div>
                    </div>
                    <div class="cf-stat-card">
                        <div class="cf-stat-icon success">✓</div>
                        <div class="cf-stat-info">
                            <h3><%= activeHalls != null ? activeHalls : 0 %></h3>
                            <p>Đang hoạt động (Active)</p>
                        </div>
                    </div>
                    <div class="cf-stat-card">
                        <div class="cf-stat-icon warning">⏸</div>
                        <div class="cf-stat-info">
                            <h3><%= inactiveHalls != null ? inactiveHalls : 0 %></h3>
                            <p>Tạm ngưng (Inactive)</p>
                        </div>
                    </div>
                    <div class="cf-stat-card">
                        <div class="cf-stat-icon info">💺</div>
                        <div class="cf-stat-info">
                            <h3><%= totalCapacity != null ? totalCapacity : 0 %></h3>
                            <p>Tổng sức chứa thiết kế (Ghế)</p>
                        </div>
                    </div>
                </div>

                <!-- Thanh tìm kiếm & bộ lọc phòng chiếu -->
                <div class="cf-filter-card">
                    <form class="cf-filter-form" method="GET" action="${pageContext.request.contextPath}/manager/halls">
                        <% if (isAdmin && targetCentreId != null) { %>
                            <input type="hidden" name="centreId" value="<%= targetCentreId %>">
                        <% } %>
                        <div class="cf-search-box">
                            <span class="icon">
                                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
                            </span>
                            <input type="text" name="search" placeholder="Tìm theo tên phòng (VD: Hall 01)..." value="<%= currentSearch != null ? currentSearch : "" %>">
                        </div>

                        <select name="status" class="cf-select">
                            <option value="ALL" <%= "ALL".equalsIgnoreCase(currentStatus) ? "selected" : "" %>>Tất cả trạng thái & định dạng</option>
                            <option value="ACTIVE" <%= "ACTIVE".equalsIgnoreCase(currentStatus) ? "selected" : "" %>>Đang hoạt động (Active)</option>
                            <option value="INACTIVE" <%= "INACTIVE".equalsIgnoreCase(currentStatus) ? "selected" : "" %>>Tạm ngưng (Inactive)</option>
                            <option value="2D" <%= "2D".equalsIgnoreCase(currentStatus) ? "selected" : "" %>>Định dạng Standard 2D</option>
                            <option value="3D" <%= "3D".equalsIgnoreCase(currentStatus) ? "selected" : "" %>>Định dạng 3D</option>
                        </select>

                        <button type="submit" class="cf-btn cf-btn-primary">Lọc</button>
                        <% if ((currentSearch != null && !currentSearch.isEmpty()) || (currentStatus != null && !"ALL".equalsIgnoreCase(currentStatus))) { %>
                            <a href="${pageContext.request.contextPath}/manager/halls<%= isAdmin ? ("?centreId=" + targetCentreId) : "" %>" class="cf-btn cf-btn-secondary">Xóa lọc</a>
                        <% } %>
                    </form>
                </div>

                <!-- Bảng danh sách phòng chiếu -->
                <div class="cf-table-container">
                    <table class="cf-table">
                        <thead>
                            <tr>
                                <th style="width: 80px;">STT</th>
                                <th style="min-width: 180px;">Tên phòng chiếu</th>
                                <th style="min-width: 140px;">Định dạng kỹ thuật</th>
                                <th style="min-width: 140px;">Tổng sức chứa</th>
                                <th style="min-width: 140px;">Trạng thái</th>
                                <th style="min-width: 160px; text-align: right;">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <%
                                if (halls != null && !halls.isEmpty()) {
                                    int hallIndex = (pg != null) ? pg.getFrom() - 1 : 0;
                                    for (Hall h : halls) {
                                        hallIndex++;
                                        boolean isActive = h.isActive();
                                        boolean is3D = "3D".equalsIgnoreCase(h.getHallType());
                            %>
                            <tr>
                                <td><strong>#<%= hallIndex %></strong></td>
                                <td>
                                    <strong style="color: #F8FAFC; font-size: 15px;"><%= h.getHallName() %></strong>
                                </td>
                                <td>
                                    <% if (is3D) { %>
                                        <span class="cf-badge cf-badge-3d">★ 3D</span>
                                    <% } else { %>
                                        <span class="cf-badge cf-badge-2d">Standard 2D</span>
                                    <% } %>
                                </td>
                                <td>
                                    <strong style="color: #F8FAFC;"><%= h.getCapacity() %></strong> ghế
                                </td>
                                <td>
                                    <% if (isActive) { %>
                                        <span class="cf-badge cf-badge-active">● Đang hoạt động</span>
                                    <% } else { %>
                                        <span class="cf-badge cf-badge-inactive">● Tạm ngưng</span>
                                    <% } %>
                                </td>
                                <td style="text-align: right;">
                                    <div class="cf-actions" style="justify-content: flex-end;">
                                        <!-- Nút Configure Seats / Cấu hình sơ đồ ghế -->
                                        <a href="${pageContext.request.contextPath}/manager/seat-config?hallId=<%= h.getHallId() %>"
                                           class="cf-btn cf-btn-sm" style="background: rgba(59, 130, 246, 0.15); color: #60A5FA; border: 1px solid rgba(59, 130, 246, 0.35);"
                                           title="Cấu hình ma trận sơ đồ ghế và bảo trì">
                                            💺 Cấu hình ghế
                                        </a>

                                        <!-- Nút Sửa -->
                                        <button type="button" class="cf-btn cf-btn-secondary cf-btn-sm"
                                                onclick="openEditHallModal(<%= h.getHallId() %>, '<%= h.getHallName().replace("'", "\\'") %>', '<%= h.getHallType() %>', <%= h.getCapacity() %>, '<%= isActive ? "AVAILABLE" : "INACTIVE" %>')">
                                            Sửa
                                        </button>

                                        <!-- Nút Toggle Status (Bật/Tắt Active <-> Inactive) -->
                                        <form method="POST" action="${pageContext.request.contextPath}/manager/halls" style="display: inline-block; margin: 0;"
                                              onsubmit="return confirmToggleHall('<%= h.getHallName().replace("'", "\\'") %>', <%= isActive %>);">
                                            <input type="hidden" name="action" value="toggleStatus">
                                            <input type="hidden" name="hallId" value="<%= h.getHallId() %>">
                                            <input type="hidden" name="centreId" value="<%= targetCentreId %>">
                                            <input type="hidden" name="search" value="<%= currentSearch != null ? currentSearch : "" %>">
                                            <input type="hidden" name="status" value="<%= currentStatus != null ? currentStatus : "ALL" %>">
                                            <input type="hidden" name="page" value="<%= currentPage %>">

                                            <% if (isActive) { %>
                                                <button type="submit" class="cf-btn cf-btn-danger cf-btn-sm" title="Vô hiệu hóa phòng chiếu">
                                                    Tắt
                                                </button>
                                            <% } else { %>
                                                <button type="submit" class="cf-btn cf-btn-primary cf-btn-sm" style="background: #16A34A; border-color: #16A34A;" title="Kích hoạt phòng chiếu">
                                                    Bật
                                                </button>
                                            <% } %>
                                        </form>
                                    </div>
                                </td>
                            </tr>
                            <%
                                    }
                                } else {
                            %>
                            <tr>
                                <td colspan="6" style="text-align: center; padding: 40px; color: #94A3B8;">
                                    Không tìm thấy phòng chiếu nào phù hợp. Bấm nút <strong>"Thêm phòng chiếu mới"</strong> để khởi tạo phòng.
                                </td>
                            </tr>
                            <% } %>
                        </tbody>
                    </table>

                    <% if (totalFilteredHalls != null && totalFilteredHalls > 0) { 
                        int startItem = (currentPage - 1) * pageSize + 1;
                        int endItem = Math.min(currentPage * pageSize, totalFilteredHalls);

                        String baseParams = "";
                        if (isAdmin && targetCentreId != null) {
                            baseParams += "&centreId=" + targetCentreId;
                        }
                        if (currentSearch != null && !currentSearch.trim().isEmpty()) {
                            baseParams += "&search=" + java.net.URLEncoder.encode(currentSearch.trim(), "UTF-8");
                        }
                        if (currentStatus != null && !currentStatus.trim().isEmpty()) {
                            baseParams += "&status=" + java.net.URLEncoder.encode(currentStatus.trim(), "UTF-8");
                        }
                    %>
                    <!-- THANH PHÂN TRANG (PAGINATION) -->
                    <div class="cf-pagination-bar">
                        <div class="cf-pagination-info">
                            Hiển thị <strong><%= startItem %> - <%= endItem %></strong> trên tổng số <strong><%= totalFilteredHalls %></strong> phòng chiếu
                        </div>
                        <% if (totalPages > 1) { %>
                        <div class="cf-pagination-controls">
                            <!-- Nút trang trước (Previous) -->
                            <% if (currentPage > 1) { %>
                                <a href="${pageContext.request.contextPath}/manager/halls?page=<%= currentPage - 1 %><%= baseParams %>" 
                                   class="cf-page-btn" title="Trang trước">«</a>
                            <% } else { %>
                                <span class="cf-page-btn disabled" title="Trang trước">«</span>
                            <% } %>

                            <!-- Danh sách các số trang -->
                            <% for (int i = 1; i <= totalPages; i++) { 
                                if (i == 1 || i == totalPages || Math.abs(i - currentPage) <= 2) {
                            %>
                                <% if (i == currentPage) { %>
                                    <span class="cf-page-btn active"><%= i %></span>
                                <% } else { %>
                                    <a href="${pageContext.request.contextPath}/manager/halls?page=<%= i %><%= baseParams %>" 
                                       class="cf-page-btn"><%= i %></a>
                                <% } %>
                            <% 
                                } else if (i == currentPage - 3 || i == currentPage + 3) { 
                            %>
                                <span class="cf-page-ellipsis">...</span>
                            <% 
                                }
                            } 
                            %>

                            <!-- Nút trang sau (Next) -->
                            <% if (currentPage < totalPages) { %>
                                <a href="${pageContext.request.contextPath}/manager/halls?page=<%= currentPage + 1 %><%= baseParams %>" 
                                   class="cf-page-btn" title="Trang sau">»</a>
                            <% } else { %>
                                <span class="cf-page-btn disabled" title="Trang sau">»</span>
                            <% } %>
                        </div>
                        <% } %>
                    </div>
                    <% } %>
                </div>
            <% } %>
        </main>
    </div>
</div>

<!-- ============================================================== -->
<!-- MODAL A: THÊM MỚI PHÒNG CHIẾU -->
<!-- ============================================================== -->
<div class="cf-modal-backdrop" id="createHallModal">
    <div class="cf-modal">
        <div class="cf-modal-header">
            <h3>Thêm phòng chiếu mới</h3>
            <button type="button" class="cf-modal-close" onclick="closeCreateHallModal()">&times;</button>
        </div>
        <form method="POST" action="${pageContext.request.contextPath}/manager/halls">
            <input type="hidden" name="action" value="create">
            <input type="hidden" name="centreId" value="<%= targetCentreId != null ? targetCentreId : 0 %>">
            <input type="hidden" name="search" value="<%= currentSearch != null ? currentSearch : "" %>">
            <input type="hidden" name="status" value="<%= currentStatus != null ? currentStatus : "ALL" %>">
            <input type="hidden" name="page" value="1">

            <div class="cf-modal-body">
                <div class="cf-form-group">
                    <label>Tên / Số phòng chiếu *</label>
                    <input type="text" name="hallName" placeholder="VD: Hall 01, Phòng 01..." required>
                    <span class="cf-form-hint">Tên phòng chiếu không được trùng lặp trong cùng Cụm rạp.</span>
                </div>

                <div class="cf-form-group">
                    <label>Định dạng kỹ thuật *</label>
                    <select name="hallType" class="cf-select">
                        <option value="2D" selected>Standard 2D</option>
                        <option value="3D">3D</option>
                    </select>
                </div>

                <div class="cf-form-group">
                    <label>Tổng sức chứa thiết kế (Số ghế) *</label>
                    <input type="number" name="capacity" value="60" min="1" max="500" required>
                    <span class="cf-form-hint">Tổng số lượng ghế dự kiến lắp đặt trong phòng chiếu.</span>
                </div>

                <div class="cf-form-group">
                    <label>Trạng thái khởi tạo</label>
                    <input type="text" value="Đang hoạt động" disabled style="background: #1E293B; color: #34D399; border: 1px solid rgba(255, 255, 255, 0.14); font-weight: 600;">
                    <input type="hidden" name="status" value="AVAILABLE">
                </div>
            </div>

            <div class="cf-modal-footer">
                <button type="button" class="cf-btn cf-btn-secondary" onclick="closeCreateHallModal()">Hủy</button>
                <button type="submit" class="cf-btn cf-btn-primary">Lưu phòng chiếu</button>
            </div>
        </form>
    </div>
</div>

<!-- ============================================================== -->
<!-- MODAL B: CHỈNH SỬA PHÒNG CHIẾU -->
<!-- ============================================================== -->
<div class="cf-modal-backdrop" id="editHallModal">
    <div class="cf-modal">
        <div class="cf-modal-header">
            <h3>Chỉnh sửa thông tin phòng chiếu</h3>
            <button type="button" class="cf-modal-close" onclick="closeEditHallModal()">&times;</button>
        </div>
        <form method="POST" action="${pageContext.request.contextPath}/manager/halls">
            <input type="hidden" name="action" value="update">
            <input type="hidden" name="hallId" id="editHallId">
            <input type="hidden" name="centreId" value="<%= targetCentreId != null ? targetCentreId : 0 %>">
            <input type="hidden" name="search" value="<%= currentSearch != null ? currentSearch : "" %>">
            <input type="hidden" name="status" value="<%= currentStatus != null ? currentStatus : "ALL" %>">
            <input type="hidden" name="page" value="<%= currentPage %>">

            <div class="cf-modal-body">
                <div class="cf-form-group">
                    <label>Tên / Số phòng chiếu *</label>
                    <input type="text" name="hallName" id="editHallName" required>
                    <span class="cf-form-hint">Kiểm tra không trùng tên phòng khác trong Cụm rạp.</span>
                </div>

                <div class="cf-form-group">
                    <label>Định dạng kỹ thuật *</label>
                    <select name="hallType" id="editHallType" class="cf-select">
                        <option value="2D">Standard 2D</option>
                        <option value="3D">3D</option>
                    </select>
                </div>

                <div class="cf-form-group">
                    <label>Tổng sức chứa thiết kế (Số ghế) *</label>
                    <input type="number" name="capacity" id="editCapacity" min="1" max="500" required>
                </div>

                <div class="cf-form-group">
                    <label>Trạng thái hoạt động *</label>
                    <select name="status" id="editStatus" class="cf-select">
                        <option value="AVAILABLE">Đang hoạt động</option>
                        <option value="INACTIVE">Tạm ngưng hoạt động</option>
                    </select>
                    <span class="cf-form-hint">Lưu ý: Không thể chuyển sang tạm ngưng nếu phòng đang có suất chiếu tương lai đã bán vé.</span>
                </div>
            </div>

            <div class="cf-modal-footer">
                <button type="button" class="cf-btn cf-btn-secondary" onclick="closeEditHallModal()">Hủy</button>
                <button type="submit" class="cf-btn cf-btn-primary">Cập nhật thay đổi</button>
            </div>
        </form>
    </div>
</div>

<!-- JavaScript điều khiển giao diện -->
<script>
    // Tự động ẩn thông báo Alert sau 3.5 giây
    document.addEventListener("DOMContentLoaded", function () {
        document.querySelectorAll(".cf-alert").forEach(function (alert) {
            setTimeout(function () {
                alert.style.transition = "opacity 0.4s ease";
                alert.style.opacity = "0";
                setTimeout(function () { alert.remove(); }, 400);
            }, 3500);
        });
    });

    // Mở / Đóng Modal Thêm Hall
    function openCreateHallModal() {
        document.getElementById('createHallModal').classList.add('active');
    }
    function closeCreateHallModal() {
        document.getElementById('createHallModal').classList.remove('active');
    }

    // Mở / Đóng Modal Sửa Hall
    function openEditHallModal(id, name, type, capacity, status) {
        document.getElementById('editHallId').value = id;
        document.getElementById('editHallName').value = name;
        document.getElementById('editHallType').value = type ? type.toUpperCase() : "2D";
        document.getElementById('editCapacity').value = capacity;
        document.getElementById('editStatus').value = status ? status.toUpperCase() : "AVAILABLE";
        document.getElementById('editHallModal').classList.add('active');
    }
    function closeEditHallModal() {
        document.getElementById('editHallModal').classList.remove('active');
    }

    // Xác nhận Toggle Status
    function confirmToggleHall(hallName, isCurrentlyActive) {
        if (isCurrentlyActive) {
            return confirm("Bạn có chắc chắn muốn TẮT trạng thái hoạt động của phòng \"" + hallName + "\"?\n\nLưu ý: Hệ thống sẽ từ chối nếu phòng đang có suất chiếu tương lai đã phát sinh vé đặt.");
        } else {
            return confirm("Bạn có muốn KÍCH HOẠT (Active) lại phòng \"" + hallName + "\" để sẵn sàng xếp suất chiếu?");
        }
    }

    // Chuyển đổi Cụm rạp (Dành cho Admin)
    function switchCentre(centreId) {
        window.location.href = '${pageContext.request.contextPath}/manager/halls?centreId=' + centreId;
    }

    // Đóng modal khi click ra ngoài backdrop
    document.querySelectorAll('.cf-modal-backdrop').forEach(function(modal) {
        modal.addEventListener('click', function(e) {
            if (e.target === this) {
                this.classList.remove('active');
            }
        });
    });
</script>
</body>
</html>
