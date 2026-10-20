<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.Hall,model.Centre,model.SeatConfigDTO,java.util.List" %>
<%
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && session.getAttribute("user") instanceof model.User) {
        authRole = ((model.User) session.getAttribute("user")).getRole();
    }
    if (!"MANAGER".equalsIgnoreCase(authRole) && !"ADMIN".equalsIgnoreCase(authRole)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Quản lý (MANAGER) hoặc Quản trị viên (ADMIN) mới có quyền truy cập trang này.");
        return;
    }

    boolean isAdmin = "ADMIN".equalsIgnoreCase(authRole);
    request.setAttribute("activeNav", "halls");

    Hall hall = (Hall) request.getAttribute("hall");
    Centre centre = (Centre) request.getAttribute("centre");
    String seatsJson = (String) request.getAttribute("seatsJson");
    Boolean hasBookedShowtimes = (Boolean) request.getAttribute("hasBookedShowtimes");
    if (hasBookedShowtimes == null) {
        hasBookedShowtimes = false;
    }

    List<SeatConfigDTO> pagedSeats = (List<SeatConfigDTO>) request.getAttribute("pagedSeats");
    Integer currentPage = (Integer) request.getAttribute("currentPage");
    if (currentPage == null) currentPage = 1;
    Integer pageSize = (Integer) request.getAttribute("pageSize");
    if (pageSize == null) pageSize = 12;
    Integer totalPages = (Integer) request.getAttribute("totalPages");
    if (totalPages == null) totalPages = 1;
    Integer totalFilteredSeats = (Integer) request.getAttribute("totalFilteredSeats");
    if (totalFilteredSeats == null) totalFilteredSeats = 0;

    String currentSearch = (String) request.getAttribute("currentSearch");
    String currentSeatType = (String) request.getAttribute("currentSeatType");
    String currentSeatStatus = (String) request.getAttribute("currentSeatStatus");

    String successParam = request.getParameter("success");
    String errorParam = request.getParameter("error");
    String customMsg = request.getParameter("msg");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cấu hình Sơ đồ ghế - <%= hall != null ? hall.getHallName() : "" %> | CineFlow</title>

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
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/seat-config.css?v=<%= System.currentTimeMillis() %>">
</head>
<body>
<div class="cf-manager-shell">
    <!-- Sidebar -->
    <jsp:include page="<%= isAdmin ? \"/admin/sidebar.jsp\" : \"/manager/sidebar.jsp\" %>" />

    <div class="cf-manager-main">
        <!-- Topbar -->
        <jsp:include page="/admin/topbar.jsp" />

        <main class="cf-content">
            <!-- Header trang -->
            <div class="cf-page-header">
                <div>
                    <h1>Cấu hình Sơ đồ &amp; Trạng thái ghế</h1>
                    <p>Khởi tạo lưới ma trận hàng/cột, phân hạng Standard, VIP, Sweetbox và khóa bảo trì vị trí ghế hỏng</p>
                </div>
                <a href="${pageContext.request.contextPath}/manager/halls<%= isAdmin && centre != null ? "?centreId=" + centre.getCentreId() : "" %>"
                   class="cf-btn cf-btn-secondary" style="display: flex; align-items: center; gap: 6px;">
                    ← Quay lại danh sách phòng
                </a>
            </div>

            <!-- Thông báo trạng thái -->
            <% if ("saved".equals(successParam)) { %>
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> <strong>Thành công!</strong> Đã lưu cấu hình sơ đồ ghế và tự động đồng bộ sức chứa phòng chiếu thành công.
                </div>
            <% } else if ("maintenance_toggled".equals(successParam)) { %>
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> <strong>Thành công!</strong> Đã cập nhật trạng thái bảo trì ghế và tự động đồng bộ lại sức chứa khả dụng của phòng chiếu.
                </div>
            <% } else if ("cannot_regenerate".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Cảnh báo:</strong> Không thể tạo lại cấu trúc lưới ghế vì phòng chiếu đang có lịch chiếu đã mở bán vé. Bạn chỉ được phép khóa bảo trì từng ghế đơn lẻ.
                </div>
            <% } else if ("booked_seat".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong><%= customMsg != null ? customMsg : "Không thể khóa bảo trì ghế đã có khách đặt vé trong suất chiếu tương lai!" %></strong>
                </div>
            <% } else if ("failed".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Không thể lưu cấu hình sơ đồ ghế. Vui lòng thử lại sau!
                </div>
            <% } %>

            <!-- Banner thông tin phòng chiếu -->
            <div class="cf-hall-banner">
                <div class="cf-hall-meta">
                    <div class="cf-hall-badge-icon">🎬</div>
                    <div>
                        <h2><%= hall != null ? hall.getHallName() : "" %> &bull; <%= centre != null ? centre.getCentreName() : "" %></h2>
                        <p>
                            Định dạng: <strong><%= hall != null ? hall.getFormattedType() : "" %></strong>
                            &nbsp;|&nbsp; Sức chứa thiết kế hiện tại: <strong><%= hall != null ? hall.getCapacity() : 0 %> ghế</strong>
                            &nbsp;|&nbsp; Trạng thái phòng:
                            <% if (hall != null && hall.isActive()) { %>
                                <span class="cf-badge cf-badge-active" style="padding: 2px 8px;">Active</span>
                            <% } else { %>
                                <span class="cf-badge cf-badge-inactive" style="padding: 2px 8px;">Inactive</span>
                            <% } %>
                        </p>
                    </div>
                </div>

                <% if (hasBookedShowtimes) { %>
                    <div style="background: rgba(245, 158, 11, 0.12); border: 1px solid rgba(245, 158, 11, 0.3); padding: 8px 14px; border-radius: 8px; font-size: 13px; color: #B45309;">
                        🔒 <strong>Phòng đã có lịch chiếu mở bán vé:</strong> Khóa tính năng tạo lại lưới hàng/cột (chỉ cho phép cập nhật loại ghế hoặc bảo trì).
                    </div>
                <% } %>
            </div>

            <!-- Khu vực điều khiển: Khởi tạo lưới & Palette công cụ -->
            <div class="cf-canvas-controls">
                <!-- Panel 1: Khởi tạo lưới ban đầu -->
                <div class="cf-panel">
                    <h3>
                        <span>📐</span> Khởi tạo lưới sơ đồ
                    </h3>
                    <div class="cf-grid-form">
                        <div class="cf-form-inline">
                            <label>Số hàng (A-Z):</label>
                            <input type="number" id="gridRowsInput" min="1" max="26" value="6" <%= hasBookedShowtimes ? "disabled" : "" %>>
                        </div>
                        <div class="cf-form-inline">
                            <label>Số ghế / hàng:</label>
                            <input type="number" id="gridColsInput" min="1" max="30" value="10" <%= hasBookedShowtimes ? "disabled" : "" %>>
                        </div>
                        <button type="button" class="cf-btn cf-btn-secondary" onclick="handleGenerateGridClick()" <%= hasBookedShowtimes ? "disabled style='opacity: 0.5; cursor: not-allowed;'" : "" %>>
                            Tạo lưới sơ đồ
                        </button>
                    </div>
                    <% if (hasBookedShowtimes) { %>
                        <p style="font-size: 12px; color: #B45309; margin: 8px 0 0 0;">
                            * Lưu ý: Không được phép tạo lại lưới khi phòng đang có suất chiếu đã bán vé.
                        </p>
                    <% } else { %>
                        <p style="font-size: 12px; color: #64748B; margin: 8px 0 0 0;">
                            * Nhập số hàng và cột để tạo lưới ô ban đầu (mặc định toàn bộ là ghế Standard).
                        </p>
                    <% } %>
                </div>

                <!-- Panel 2: Thanh công cụ chọn hạng ghế & bảo trì -->
                <div class="cf-panel">
                    <h3>
                        <span>🎨</span> Công cụ phân hạng &amp; Bảo trì
                    </h3>
                    <div class="cf-tool-palette">
                        <button type="button" class="cf-tool-btn active" data-tool="STANDARD" onclick="selectTool('STANDARD', this)">
                            <span class="cf-tool-swatch standard"></span>
                            Standard (Thường)
                        </button>
                        <button type="button" class="cf-tool-btn" data-tool="VIP" onclick="selectTool('VIP', this)">
                            <span class="cf-tool-swatch vip"></span>
                            VIP
                        </button>
                        <button type="button" class="cf-tool-btn" data-tool="SWEETBOX" onclick="selectTool('SWEETBOX', this)">
                            <span class="cf-tool-swatch sweetbox"></span>
                            Sweetbox (Đôi)
                        </button>
                        <button type="button" class="cf-tool-btn" data-tool="MAINTENANCE" onclick="selectTool('MAINTENANCE', this)">
                            <span class="cf-tool-swatch maintenance"></span>
                            🔧 Khóa bảo trì
                        </button>
                        <button type="button" class="cf-tool-btn" data-tool="EMPTY" onclick="selectTool('EMPTY', this)">
                            <span class="cf-tool-swatch empty"></span>
                            Lối đi / Trống
                        </button>
                    </div>
                    <p style="font-size: 12px; color: #64748B; margin: 10px 0 0 0;">
                        * Chọn một công cụ ở trên rồi <strong>click vào các ô ghế</strong> trên sơ đồ bên dưới để đổi phân hạng hoặc khóa bảo trì.
                    </p>
                </div>
            </div>

            <!-- Thanh thống kê sức chứa khả dụng thực tế (Live Capacity Bar) -->
            <div class="cf-capacity-summary">
                <div class="cf-capacity-stats">
                    <div class="cf-stat-item">
                        <span>Tổng ô:</span>
                        <span class="cf-stat-pill" id="statTotalCells">0</span>
                    </div>
                    <div class="cf-stat-item">
                        <span>Ghế Standard:</span>
                        <span class="cf-stat-pill standard" id="statStandard">0</span>
                    </div>
                    <div class="cf-stat-item">
                        <span>Ghế VIP:</span>
                        <span class="cf-stat-pill vip" id="statVip">0</span>
                    </div>
                    <div class="cf-stat-item">
                        <span>Ghế Sweetbox:</span>
                        <span class="cf-stat-pill sweetbox" id="statSweetbox">0</span>
                    </div>
                    <div class="cf-stat-item">
                        <span>🔧 Ghế bảo trì:</span>
                        <span class="cf-stat-pill maintenance" id="statMaintenance">0</span>
                    </div>
                    <div class="cf-stat-item">
                        <span>Lối đi / Trống:</span>
                        <span class="cf-stat-pill empty" id="statEmpty">0</span>
                    </div>
                </div>

                <div class="cf-capacity-highlight">
                    <span>Sức chứa khả dụng thực tế:</span>
                    <strong id="statAvailableCapacity">0</strong>
                    <span style="font-size: 13px; color: #64748B;">ghế mở bán</span>
                </div>
            </div>

            <!-- Vùng Canvas Sơ đồ ghế (Seat Map Canvas) -->
            <div class="cf-seat-canvas-wrapper">
                <!-- Màn hình chiếu -->
                <div class="cf-screen">
                    <div class="cf-screen-label">MÀN HÌNH CHIẾU</div>
                </div>

                <!-- Lưới ma trận ghế -->
                <div id="seatGridContainer" class="cf-seat-grid">
                    <!-- Được render động bởi JavaScript -->
                </div>

                <!-- Chú giải sơ đồ -->
                <div class="cf-legend">
                    <div class="cf-legend-item">
                        <span class="cf-legend-box standard"></span> Ghế Standard (Thường)
                    </div>
                    <div class="cf-legend-item">
                        <span class="cf-legend-box vip"></span> Ghế VIP
                    </div>
                    <div class="cf-legend-item">
                        <span class="cf-legend-box sweetbox"></span> Ghế Sweetbox (Đôi)
                    </div>
                    <div class="cf-legend-item">
                        <span class="cf-legend-box maintenance"></span> Ghế Disable / Đang bảo trì
                    </div>
                    <div class="cf-legend-item">
                        <span class="cf-legend-box empty"></span> Ô lối đi / Trống
                    </div>
                </div>
            </div>

            <!-- Form ẩn lưu dữ liệu cấu hình -->
            <form id="saveLayoutForm" method="POST" action="${pageContext.request.contextPath}/manager/seat-config">
                <input type="hidden" name="hallId" value="<%= hall != null ? hall.getHallId() : 0 %>">
                <input type="hidden" name="isRegenerating" id="isRegeneratingInput" value="false">
                <input type="hidden" name="seatData" id="seatDataInput" value="">

                <div class="cf-footer-actions">
                    <a href="${pageContext.request.contextPath}/manager/halls<%= isAdmin && centre != null ? "?centreId=" + centre.getCentreId() : "" %>"
                       class="cf-btn cf-btn-secondary">
                        Hủy thay đổi
                    </a>

                    <button type="button" class="cf-btn cf-btn-primary" onclick="submitSeatConfiguration()" style="padding: 10px 24px; font-size: 15px;">
                        💾 Lưu cấu hình sơ đồ ghế
                    </button>
                </div>
            </form>

            <!-- ============================================================== -->
            <!-- PHẦN 2: BẢNG TRA CỨU GHẾ NGỒI & QUẢN LÝ BẢO TRÌ (PAGINATION) -->
            <!-- ============================================================== -->
            <div class="cf-seats-table-card" style="margin-top: 36px;">
                <div class="cf-section-header">
                    <div>
                        <h2 style="font-size: 18px; font-weight: 700; color: #F8FAFC; margin: 0 0 4px 0;">
                            📋 Danh sách chi tiết ghế ngồi &amp; Quản lý bảo trì
                        </h2>
                        <p style="font-size: 13px; color: #94A3B8; margin: 0;">
                            Tra cứu từng vị trí ghế theo mã hoặc hàng, lọc phân hạng Standard/VIP/Sweetbox và thao tác bật/tắt bảo trì nhanh.
                        </p>
                    </div>
                </div>

                <!-- Thanh tìm kiếm & bộ lọc ghế -->
                <div class="cf-filter-card" style="margin-top: 16px;">
                    <form class="cf-filter-form" method="GET" action="${pageContext.request.contextPath}/manager/seat-config">
                        <input type="hidden" name="hallId" value="<%= hall != null ? hall.getHallId() : 0 %>">

                        <div class="cf-search-box">
                            <span class="icon">
                                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
                            </span>
                            <input type="text" name="search" placeholder="Tìm theo hàng hoặc số ghế (VD: A, 1, A1)..." value="<%= currentSearch != null ? currentSearch : "" %>">
                        </div>

                        <select name="seatType" class="cf-select">
                            <option value="ALL" <%= "ALL".equalsIgnoreCase(currentSeatType) ? "selected" : "" %>>Tất cả phân hạng</option>
                            <option value="STANDARD" <%= "STANDARD".equalsIgnoreCase(currentSeatType) ? "selected" : "" %>>Ghế Standard (Thường)</option>
                            <option value="VIP" <%= "VIP".equalsIgnoreCase(currentSeatType) ? "selected" : "" %>>Ghế VIP</option>
                            <option value="SWEETBOX" <%= "SWEETBOX".equalsIgnoreCase(currentSeatType) ? "selected" : "" %>>Ghế Sweetbox (Đôi)</option>
                        </select>

                        <select name="seatStatus" class="cf-select">
                            <option value="ALL" <%= "ALL".equalsIgnoreCase(currentSeatStatus) ? "selected" : "" %>>Tất cả trạng thái</option>
                            <option value="AVAILABLE" <%= "AVAILABLE".equalsIgnoreCase(currentSeatStatus) ? "selected" : "" %>>Khả dụng (Active)</option>
                            <option value="MAINTENANCE" <%= "MAINTENANCE".equalsIgnoreCase(currentSeatStatus) ? "selected" : "" %>>Đang bảo trì (Maintenance)</option>
                        </select>

                        <button type="submit" class="cf-btn cf-btn-primary">Lọc</button>
                        <% if ((currentSearch != null && !currentSearch.isEmpty()) || (currentSeatType != null && !"ALL".equalsIgnoreCase(currentSeatType)) || (currentSeatStatus != null && !"ALL".equalsIgnoreCase(currentSeatStatus))) { %>
                            <a href="${pageContext.request.contextPath}/manager/seat-config?hallId=<%= hall != null ? hall.getHallId() : 0 %>" class="cf-btn cf-btn-secondary">Xóa lọc</a>
                        <% } %>
                    </form>
                </div>

                <!-- Bảng danh sách ghế -->
                <div class="cf-table-container">
                    <table class="cf-table">
                        <thead>
                            <tr>
                                <th style="width: 80px;">STT</th>
                                <th style="min-width: 140px;">Vị trí ghế</th>
                                <th style="min-width: 160px;">Phân hạng ghế</th>
                                <th style="min-width: 160px;">Trạng thái hoạt động</th>
                                <th style="min-width: 160px; text-align: right;">Thao tác bảo trì</th>
                            </tr>
                        </thead>
                        <tbody>
                            <%
                                if (pagedSeats != null && !pagedSeats.isEmpty()) {
                                    int seatIndex = (currentPage - 1) * pageSize;
                                    for (SeatConfigDTO s : pagedSeats) {
                                        seatIndex++;
                                        boolean isAvailable = s.isAvailable();
                                        boolean isMaint = s.isMaintenance();
                                        boolean isUnavail = s.isUnavailable();
                                        String type = s.getSeatType() != null ? s.getSeatType().toUpperCase() : "STANDARD";
                            %>
                            <tr>
                                <td><strong>#<%= seatIndex %></strong></td>
                                <td>
                                    <% if (isUnavail) { %>
                                        <div style="display: inline-flex; align-items: center; justify-content: center; width: 42px; height: 32px; background: rgba(255, 255, 255, 0.04); border: 1px dashed rgba(255, 255, 255, 0.2); color: #94A3B8; font-weight: 700; border-radius: 6px; font-size: 13px;">
                                            —
                                        </div>
                                        <span style="font-size: 13px; color: #94A3B8; margin-left: 8px;">(Hàng <%= s.getSeatRow() %>, Cột <%= s.getSeatNumber() %> - Lối đi / Trống)</span>
                                    <% } else { %>
                                        <div style="display: inline-flex; align-items: center; justify-content: center; width: 42px; height: 32px; background: #1E293B; border: 1px solid rgba(255, 255, 255, 0.15); color: #F8FAFC; font-weight: 700; border-radius: 6px; font-size: 13px;">
                                            <%= s.getLabel() %>
                                        </div>
                                        <span style="font-size: 13px; color: #94A3B8; margin-left: 8px;">(Hàng <%= s.getSeatRow() %>, Ghế <%= s.getSeatNumber() %>)</span>
                                    <% } %>
                                </td>
                                <td>
                                    <% if (isUnavail) { %>
                                        <span class="cf-badge" style="background: rgba(148, 163, 184, 0.1); color: #94A3B8; border: 1px dashed rgba(148, 163, 184, 0.25);">Lối đi / Trống</span>
                                    <% } else if ("VIP".equals(type)) { %>
                                        <span class="cf-badge" style="background: rgba(245, 158, 11, 0.15); color: #FBBF24; border: 1px solid rgba(245, 158, 11, 0.35);">★ VIP</span>
                                    <% } else if ("SWEETBOX".equals(type)) { %>
                                        <span class="cf-badge" style="background: rgba(236, 72, 153, 0.15); color: #F472B6; border: 1px solid rgba(236, 72, 153, 0.35);">♥ Sweetbox</span>
                                    <% } else { %>
                                        <span class="cf-badge" style="background: rgba(255, 255, 255, 0.08); color: #E2E8F0; border: 1px solid rgba(255, 255, 255, 0.16);">Standard</span>
                                    <% } %>
                                </td>
                                <td>
                                    <% if (isAvailable) { %>
                                        <span class="cf-badge cf-badge-active">● Khả dụng (Active)</span>
                                    <% } else if (isMaint) { %>
                                        <span class="cf-badge" style="background: rgba(239, 68, 68, 0.15); color: #F87171; border: 1px solid rgba(239, 68, 68, 0.35);">🔧 Đang bảo trì</span>
                                    <% } else if (isUnavail) { %>
                                        <span class="cf-badge" style="background: rgba(148, 163, 184, 0.1); color: #94A3B8; border: 1px dashed rgba(148, 163, 184, 0.25);">— Không khả dụng</span>
                                    <% } else { %>
                                        <span class="cf-badge cf-badge-inactive">● <%= s.getStatus() %></span>
                                    <% } %>
                                </td>
                                <td style="text-align: right;">
                                    <% if (isUnavail) { %>
                                        <span style="color: #64748B; font-size: 13px; font-style: italic;">Lối đi / Trống</span>
                                    <% } else { %>
                                        <form method="POST" action="${pageContext.request.contextPath}/manager/seat-config" style="display: inline-block; margin: 0;"
                                              onsubmit="return confirm('<%= isAvailable ? ("Khóa bảo trì ghế " + s.getLabel() + "? Ghế này sẽ không thể đặt vé cho đến khi mở lại.") : ("Mở bảo trì lại ghế " + s.getLabel() + "?") %>');">
                                            <input type="hidden" name="action" value="toggleMaintenance">
                                            <input type="hidden" name="hallId" value="<%= hall != null ? hall.getHallId() : 0 %>">
                                            <input type="hidden" name="seatId" value="<%= s.getSeatId() %>">
                                            <input type="hidden" name="search" value="<%= currentSearch != null ? currentSearch : "" %>">
                                            <input type="hidden" name="seatType" value="<%= currentSeatType != null ? currentSeatType : "ALL" %>">
                                            <input type="hidden" name="seatStatus" value="<%= currentSeatStatus != null ? currentSeatStatus : "ALL" %>">
                                            <input type="hidden" name="page" value="<%= currentPage %>">

                                            <% if (isAvailable) { %>
                                                <button type="submit" class="cf-btn cf-btn-danger cf-btn-sm" title="Khóa bảo trì vị trí ghế này">
                                                    🔧 Khóa bảo trì
                                                </button>
                                            <% } else { %>
                                                <button type="submit" class="cf-btn cf-btn-primary cf-btn-sm" style="background: #16A34A; border-color: #16A34A;" title="Mở hoạt động lại vị trí ghế này">
                                                    ✓ Mở hoạt động
                                                </button>
                                            <% } %>
                                        </form>
                                    <% } %>
                                </td>
                            </tr>
                            <%
                                    }
                                } else {
                            %>
                            <tr>
                                <td colspan="5" style="text-align: center; padding: 40px; color: #94A3B8;">
                                    Không tìm thấy ghế nào phù hợp với điều kiện tìm kiếm / lọc.
                                </td>
                            </tr>
                            <% } %>
                        </tbody>
                    </table>

                    <% if (totalFilteredSeats != null && totalFilteredSeats > 0) { 
                        int startItem = (currentPage - 1) * pageSize + 1;
                        int endItem = Math.min(currentPage * pageSize, totalFilteredSeats);

                        String baseParams = "&hallId=" + (hall != null ? hall.getHallId() : 0);
                        if (currentSearch != null && !currentSearch.trim().isEmpty()) {
                            baseParams += "&search=" + java.net.URLEncoder.encode(currentSearch.trim(), "UTF-8");
                        }
                        if (currentSeatType != null && !currentSeatType.trim().isEmpty()) {
                            baseParams += "&seatType=" + java.net.URLEncoder.encode(currentSeatType.trim(), "UTF-8");
                        }
                        if (currentSeatStatus != null && !currentSeatStatus.trim().isEmpty()) {
                            baseParams += "&seatStatus=" + java.net.URLEncoder.encode(currentSeatStatus.trim(), "UTF-8");
                        }
                    %>
                    <!-- THANH PHÂN TRANG (PAGINATION) -->
                    <div class="cf-pagination-bar">
                        <div class="cf-pagination-info">
                            Hiển thị <strong><%= startItem %> - <%= endItem %></strong> trên tổng số <strong><%= totalFilteredSeats %></strong> vị trí ghế
                        </div>
                        <% if (totalPages > 1) { %>
                        <div class="cf-pagination-controls">
                            <!-- Nút trang trước (Previous) -->
                            <% if (currentPage > 1) { %>
                                <a href="${pageContext.request.contextPath}/manager/seat-config?page=<%= currentPage - 1 %><%= baseParams %>" 
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
                                    <a href="${pageContext.request.contextPath}/manager/seat-config?page=<%= i %><%= baseParams %>" 
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
                                <a href="${pageContext.request.contextPath}/manager/seat-config?page=<%= currentPage + 1 %><%= baseParams %>" 
                                   class="cf-page-btn" title="Trang sau">»</a>
                            <% } else { %>
                                <span class="cf-page-btn disabled" title="Trang sau">»</span>
                            <% } %>
                        </div>
                        <% } %>
                    </div>
                    <% } %>
                </div>
            </div>
        </main>
    </div>
</div>

<!-- ============================================================== -->
<!-- JAVASCRIPT: XỬ LÝ MA TRẬN SƠ ĐỒ GHẾ & TÍNH TOÁN REALTIME -->
<!-- ============================================================== -->
<script>
    // Dữ liệu ghế hiện có từ Server
    var existingSeatsData = <%= seatsJson != null ? seatsJson : "[]" %>;
    var hasBookedShowtimes = <%= hasBookedShowtimes %>;
    var currentTool = 'STANDARD';
    var isRegeneratingGridFlag = false;

    // Lưu trữ ma trận hiện tại: { "A": { 1: { type: "STANDARD", status: "AVAILABLE" }, ... } }
    var seatMatrix = {};
    var rowKeys = [];
    var maxCols = 0;

    // Ký tự hàng A, B, C...
    function getRowLetter(index) {
        return String.fromCharCode(65 + index); // 0 -> A, 1 -> B...
    }

    // Chọn công cụ từ Palette
    function selectTool(toolName, btnElement) {
        currentTool = toolName;
        document.querySelectorAll('.cf-tool-btn').forEach(function(b) {
            b.classList.remove('active');
        });
        if (btnElement) {
            btnElement.classList.add('active');
        }
    }

    // Khởi tạo ma trận từ dữ liệu có sẵn hoặc mặc định
    function initMatrix() {
        if (existingSeatsData && existingSeatsData.length > 0) {
            // Load từ DB
            existingSeatsData.forEach(function(s) {
                var r = s.seatRow.toUpperCase();
                var c = parseInt(s.seatNumber);
                if (!seatMatrix[r]) {
                    seatMatrix[r] = {};
                }
                seatMatrix[r][c] = {
                    type: s.seatType ? s.seatType.toUpperCase() : "STANDARD",
                    status: s.status ? s.status.toUpperCase() : "AVAILABLE"
                };

                if (c > maxCols) maxCols = c;
            });

            rowKeys = Object.keys(seatMatrix).sort();
            document.getElementById('gridRowsInput').value = rowKeys.length;
            document.getElementById('gridColsInput').value = maxCols;
        } else {
            // Mặc định sinh lưới 6x10
            generateNewGrid(6, 10);
        }

        renderGrid();
    }

    // Xử lý khi nhấn nút Generate Grid
    function handleGenerateGridClick() {
        if (hasBookedShowtimes) {
            alert("CẢNH BÁO: Không thể tạo lại cấu trúc lưới ghế vì phòng chiếu đang có lịch chiếu đã mở bán vé.");
            return;
        }

        if (existingSeatsData && existingSeatsData.length > 0) {
            if (!confirm("Khởi tạo lại lưới sẽ thiết lập lại toàn bộ sơ đồ ghế hiện có của phòng. Bạn có chắc chắn muốn tạo lại lưới không?")) {
                return;
            }
        }

        var numRows = parseInt(document.getElementById('gridRowsInput').value) || 6;
        var numCols = parseInt(document.getElementById('gridColsInput').value) || 10;

        if (numRows < 1 || numRows > 26) {
            alert("Số hàng phải từ 1 đến 26 (tương ứng A-Z)!");
            return;
        }
        if (numCols < 1 || numCols > 30) {
            alert("Số ghế mỗi hàng phải từ 1 đến 30!");
            return;
        }

        isRegeneratingGridFlag = true;
        generateNewGrid(numRows, numCols);
        renderGrid();
    }

    // Tạo mới cấu trúc lưới
    function generateNewGrid(numRows, numCols) {
        seatMatrix = {};
        rowKeys = [];
        maxCols = numCols;

        for (var i = 0; i < numRows; i++) {
            var r = getRowLetter(i);
            rowKeys.push(r);
            seatMatrix[r] = {};

            for (var c = 1; c <= numCols; c++) {
                seatMatrix[r][c] = {
                    type: 'STANDARD',
                    status: 'AVAILABLE'
                };
            }
        }
    }

    // Click vào một ô ghế trên lưới để áp dụng công cụ hiện tại
    function handleCellClick(row, col) {
        var cell = seatMatrix[row][col];
        if (!cell) {
            cell = { type: 'STANDARD', status: 'AVAILABLE' };
            seatMatrix[row][col] = cell;
        }

        if (currentTool === 'EMPTY') {
            // Chuyển sang ô trống / lối đi
            if (cell.status === 'UNAVAILABLE') {
                // Nếu đang là lối đi, click lần nữa chuyển về Standard
                cell.status = 'AVAILABLE';
                cell.type = 'STANDARD';
            } else {
                cell.status = 'UNAVAILABLE';
            }
        } else if (currentTool === 'MAINTENANCE') {
            // Chuyển sang khóa bảo trì (hoặc mở lại nếu đang bảo trì)
            if (cell.status === 'MAINTENANCE') {
                cell.status = 'AVAILABLE';
            } else {
                cell.status = 'MAINTENANCE';
            }
        } else {
            // Gán phân loại ghế: STANDARD, VIP, SWEETBOX
            cell.type = currentTool;
            cell.status = 'AVAILABLE'; // Luôn sẵn sàng khi chọn hạng ghế
        }

        renderGrid();
    }

    // Render lại giao diện lưới ghế và tính toán thống kê
    function renderGrid() {
        var container = document.getElementById('seatGridContainer');
        container.innerHTML = '';

        var countTotalCells = 0;
        var countStandard = 0;
        var countVip = 0;
        var countSweetbox = 0;
        var countMaintenance = 0;
        var countEmpty = 0;

        rowKeys.forEach(function(r) {
            var rowDiv = document.createElement('div');
            rowDiv.className = 'cf-grid-row';

            // Nhãn hàng bên trái
            var leftLabel = document.createElement('span');
            leftLabel.className = 'cf-row-label';
            leftLabel.innerText = r;
            rowDiv.appendChild(leftLabel);

            // Các ô ghế trong hàng: Đánh số tuần tự A1, A2, A3... khi bỏ ghế trống (lối đi)
            var seatIndexInRow = 0;
            for (var c = 1; c <= maxCols; c++) {
                countTotalCells++;
                var cell = seatMatrix[r] ? seatMatrix[r][c] : null;
                if (!cell) {
                    cell = { type: 'STANDARD', status: 'UNAVAILABLE' };
                }

                var seatBox = document.createElement('div');
                seatBox.className = 'cf-seat-box';

                // Xác định class hiển thị & đánh số ghế tuần tự
                if (cell.status === 'UNAVAILABLE') {
                    seatBox.classList.add('EMPTY');
                    seatBox.innerText = '—';
                    seatBox.title = 'Hàng ' + r + ' - Lối đi / Trống (Cột ' + c + ')';
                    countEmpty++;
                } else {
                    seatIndexInRow++;
                    var seatLabel = r + seatIndexInRow;

                    if (cell.status === 'MAINTENANCE') {
                        seatBox.classList.add(cell.type, 'MAINTENANCE');
                        seatBox.innerText = seatLabel;
                        seatBox.title = seatLabel + ' (Bảo trì / Hỏng - ' + cell.type + ')';
                        countMaintenance++;
                    } else {
                        seatBox.classList.add(cell.type);
                        seatBox.innerText = seatLabel;
                        seatBox.title = seatLabel + ' (' + cell.type + ')';

                        if (cell.type === 'VIP') countVip++;
                        else if (cell.type === 'SWEETBOX') countSweetbox++;
                        else countStandard++;
                    }
                }

                (function(row, col) {
                    seatBox.onclick = function() {
                        handleCellClick(row, col);
                    };
                })(r, c);

                rowDiv.appendChild(seatBox);
            }

            // Nhãn hàng bên phải
            var rightLabel = document.createElement('span');
            rightLabel.className = 'cf-row-label';
            rightLabel.innerText = r;
            rowDiv.appendChild(rightLabel);

            container.appendChild(rowDiv);
        });

        // Tính toán Sức chứa khả dụng thực tế:
        // Total Capacity = Tổng ô - Bảo trì - Lối đi
        var availableCapacity = countStandard + countVip + countSweetbox;

        document.getElementById('statTotalCells').innerText = countTotalCells;
        document.getElementById('statStandard').innerText = countStandard;
        document.getElementById('statVip').innerText = countVip;
        document.getElementById('statSweetbox').innerText = countSweetbox;
        document.getElementById('statMaintenance').innerText = countMaintenance;
        document.getElementById('statEmpty').innerText = countEmpty;
        document.getElementById('statAvailableCapacity').innerText = availableCapacity;
    }

    // Submit lưu cấu hình sơ đồ ghế
    function submitSeatConfiguration() {
        var seatList = [];

        rowKeys.forEach(function(r) {
            for (var c = 1; c <= maxCols; c++) {
                var cell = seatMatrix[r] ? seatMatrix[r][c] : null;
                if (cell) {
                    seatList.push({
                        seatRow: r,
                        seatNumber: c,
                        seatType: cell.type || "STANDARD",
                        status: cell.status || "AVAILABLE"
                    });
                }
            }
        });

        if (seatList.length === 0) {
            alert("Sơ đồ chưa có ghế nào. Vui lòng bấm Tạo lưới sơ đồ trước!");
            return;
        }

        document.getElementById('isRegeneratingInput').value = isRegeneratingGridFlag ? "true" : "false";
        document.getElementById('seatDataInput').value = JSON.stringify(seatList);
        document.getElementById('saveLayoutForm').submit();
    }

    // Khởi chạy khi tải trang
    document.addEventListener("DOMContentLoaded", function () {
        initMatrix();

        // Tự động ẩn thông báo Alert sau 3.5 giây
        document.querySelectorAll(".cf-alert").forEach(function (alert) {
            setTimeout(function () {
                alert.style.transition = "opacity 0.4s ease";
                alert.style.opacity = "0";
                setTimeout(function () { alert.remove(); }, 400);
            }, 3500);
        });
    });
</script>
</body>
</html>
