<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.entity.Hall,model.entity.Centre,model.dto.pricing.SeatConfigDTO,java.util.*" %>
<%
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && session.getAttribute("user") instanceof model.entity.User) {
        authRole = ((model.entity.User) session.getAttribute("user")).getRole();
    }
    if (!"MANAGER".equalsIgnoreCase(authRole) && !"ADMIN".equalsIgnoreCase(authRole)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Quản lý (MANAGER) hoặc Quản trị viên (ADMIN) mới có quyền truy cập trang này.");
        return;
    }

    boolean isAdmin = "ADMIN".equalsIgnoreCase(authRole);
    request.setAttribute("activeNav", "halls");

    Hall hall = (Hall) request.getAttribute("hall");
    Centre centre = (Centre) request.getAttribute("centre");
    List<SeatConfigDTO> existingSeats = (List<SeatConfigDTO>) request.getAttribute("existingSeats");
    Boolean hasBookedShowtimes = (Boolean) request.getAttribute("hasBookedShowtimes");
    if (hasBookedShowtimes == null) {
        hasBookedShowtimes = false;
    }
//------------------------------------------------search seat-------------------------
   //tra cứu tọa độ 
    Map<String, SeatConfigDTO> seatLookup = new HashMap<>();
    //treeSeat lưu trữ không trùng lặp và theo thứ tựu , row set lưu số hàng A,B,C
    Set<String> rowSet = new TreeSet<>();
    // số cột lớn nhất 
    int maxCols = 0;
// check seat từ DB rỗng 
    if (existingSeats != null && !existingSeats.isEmpty()) {
        for (SeatConfigDTO s : existingSeats) {
            String r = s.getSeatRow().toUpperCase();
            int c = s.getSeatNumber();
            rowSet.add(r);
//A_1,seat
            seatLookup.put(r + "_" + c, s);
            //if khi cột lớn hơn max
            if (c > maxCols) {
                maxCols = c;
            }
        }
    } else {
        // Phòng mới chưa có ghế trong DB -> Mặc định 6 hàng x 10 cột tạo mặc định 5 hàng từ A đến F
        for (int i = 0; i < 6; i++) {
            rowSet.add(String.valueOf((char) ('A' + i)));
        }
        //defautl mỗi hàng 10 cột 
        maxCols = 10;
    }
// đổi TreeSet-> ArrayList
    List<String> rowList = new ArrayList<>(rowSet);

// thống kê // total row=treeseat.size()*colum
    int countTotalCells = rowList.size() * maxCols;
    int countStandard = 0, countVip = 0, countSweetbox = 0, countMaintenance = 0, countEmpty = 0;
    for (String r : rowList) {
        for (int c = 1; c <= maxCols; c++) {
        //seatLookup hash map(hàng_cột, object ),nếu DB không lưu ô này thì null ,get này lấy key trả về seat 
            SeatConfigDTO cell = seatLookup.get(r + "_" + c);
            //type hạng ghế STANDARD/VIP/SWEETBOX
            String type = (cell != null && cell.getSeatType() != null) ? cell.getSeatType().toUpperCase() : "STANDARD";
            //status trạn thái hoạt động của vị trí đo "UNAVAILABLE": ô trống lối đi 
            String status = (cell != null && cell.getStatus() != null)
                    ? cell.getStatus().toUpperCase()
                    : ((existingSeats == null || existingSeats.isEmpty()) ? "AVAILABLE" : "UNAVAILABLE");
            //lối đi 
            if ("UNAVAILABLE".equals(status)) {
                countEmpty++;
                //bảo trì
            } else if ("MAINTENANCE".equals(status)) {
                countMaintenance++;
            } else if ("VIP".equals(type)) {
                countVip++;
            } else if ("SWEETBOX".equals(type)) {
                countSweetbox++;
            } else {
                countStandard++;
            }
        }
    }
    int availableCapacity = countStandard + countVip + countSweetbox;

    List<SeatConfigDTO> pagedSeats = (List<SeatConfigDTO>) request.getAttribute("pagedSeats");
    Integer currentPage = (Integer) request.getAttribute("currentPage");
    if (currentPage == null) currentPage = 1;
    Integer pageSize = (Integer) request.getAttribute("pageSize");
    if (pageSize == null) pageSize = 12;
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
    <style>
        /* Nối đôi 2 ghế Sweetbox liền nhau thành 1 sofa đôi */
        .cf-seat-box.SWEETBOX.sweetbox-left {
            border-top-right-radius: 0 !important;
            border-bottom-right-radius: 0 !important;
            border-right: none !important;
            margin-right: -8px !important;
            z-index: 1;
        }
        .cf-seat-box.SWEETBOX.sweetbox-right {
            border-top-left-radius: 0 !important;
            border-bottom-left-radius: 0 !important;
            border-left: 1px dashed rgba(236, 72, 153, 0.45) !important;
        }
    </style>
</head>
<body>
<div class="cf-manager-shell">
    <!-- Sidebar -->
    <jsp:include page="<%= isAdmin ? \"/WEB-INF/fragments/adminSidebar.jspf\" : \"/WEB-INF/fragments/managerSidebar.jspf\" %>" />

    <div class="cf-manager-main">
        <!-- Topbar -->
        <jsp:include page="/WEB-INF/fragments/adminTopbar.jspf" />

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
                    <form class="cf-grid-form" method="POST" action="${pageContext.request.contextPath}/manager/seat-config" onsubmit="return checkGridInput();">
                        <input type="hidden" name="action" value="generateGrid">
                        <input type="hidden" name="hallId" value="<%= hall != null ? hall.getHallId() : 0 %>">
                        <div class="cf-form-inline">
                            <label>Số hàng (A-Z):</label>
                            <input type="number" id="gridRowsInput" name="rows" min="1" max="26" value="<%= rowList.size() %>" <%= hasBookedShowtimes ? "disabled" : "" %>>
                        </div>
                        <div class="cf-form-inline">
                            <label>Số ghế / hàng:</label>
                            <input type="number" id="gridColsInput" name="cols" min="1" max="30" value="<%= maxCols %>" <%= hasBookedShowtimes ? "disabled" : "" %>>
                        </div>
                        <button type="submit" class="cf-btn cf-btn-secondary" <%= hasBookedShowtimes ? "disabled style='opacity: 0.5; cursor: not-allowed;'" : "" %>>
                            Tạo lưới sơ đồ
                        </button>
                    </form>
                    <% if (hasBookedShowtimes) { %>
                        <p style="font-size: 12px; color: #B45309; margin: 8px 0 0 0;">
                            * Lưu ý: Không được phép tạo lại lưới khi phòng đang có suất chiếu đã bán vé.
                        </p>
                    <% } else { %>
                        <p style="font-size: 12px; color: #64748B; margin: 8px 0 0 0;">
                            * Nhập số hàng và cột để tạo mới toàn bộ lưới ghế (mặc định toàn bộ là Standard).
                        </p>
                    <% } %>
                </div>

                <!-- Panel 2: Thanh công cụ chọn hạng ghế & bảo trì -->
                <div class="cf-panel">
                    <h3>
                        <span>🎨</span> Công cụ phân hạng &amp; Bảo trì
                    </h3>
                    <div class="cf-tool-palette">
                        <button type="button" class="cf-tool-btn active" data-tool="STANDARD" onclick="chooseTool('STANDARD', this)">
                            <span class="cf-tool-swatch standard"></span>
                            Standard (Thường)
                        </button>
                        <button type="button" class="cf-tool-btn" data-tool="VIP" onclick="chooseTool('VIP', this)">
                            <span class="cf-tool-swatch vip"></span>
                            VIP
                        </button>
                        <button type="button" class="cf-tool-btn" data-tool="SWEETBOX" onclick="chooseTool('SWEETBOX', this)">
                            <span class="cf-tool-swatch sweetbox"></span>
                            Sweetbox (Đôi)
                        </button>
                        <button type="button" class="cf-tool-btn" data-tool="MAINTENANCE" onclick="chooseTool('MAINTENANCE', this)">
                            <span class="cf-tool-swatch maintenance"></span>
                            🔧 Khóa bảo trì
                        </button>
                        <button type="button" class="cf-tool-btn" data-tool="EMPTY" onclick="chooseTool('EMPTY', this)">
                            <span class="cf-tool-swatch empty"></span>
                            Lối đi / Trống
                        </button>
                    </div>
                    <p style="font-size: 12px; color: #64748B; margin: 10px 0 0 0;">
                        * Chọn một công cụ ở trên rồi <strong>click vào các ô ghế</strong> trên sơ đồ bên dưới để đổi phân hạng hoặc khóa bảo trì.
                    </p>
                </div>
            </div>

            <!-- Thanh thống kê sức chứa khả dụng thực tế -->
            <div class="cf-capacity-summary">
                <div class="cf-capacity-stats">
                    <div class="cf-stat-item">
                        <span>Tổng ô:</span>
                        <span class="cf-stat-pill" id="statTotalCells"><%= countTotalCells %></span>
                    </div>
                    <div class="cf-stat-item">
                        <span>Ghế Standard:</span>
                        <span class="cf-stat-pill standard" id="statStandard"><%= countStandard %></span>
                    </div>
                    <div class="cf-stat-item">
                        <span>Ghế VIP:</span>
                        <span class="cf-stat-pill vip" id="statVip"><%= countVip %></span>
                    </div>
                    <div class="cf-stat-item">
                        <span>Ghế Sweetbox:</span>
                        <span class="cf-stat-pill sweetbox" id="statSweetbox"><%= countSweetbox %></span>
                    </div>
                    <div class="cf-stat-item">
                        <span>🔧 Ghế bảo trì:</span>
                        <span class="cf-stat-pill maintenance" id="statMaintenance"><%= countMaintenance %></span>
                    </div>
                    <div class="cf-stat-item">
                        <span>Lối đi / Trống:</span>
                        <span class="cf-stat-pill empty" id="statEmpty"><%= countEmpty %></span>
                    </div>
                </div>

                <div class="cf-capacity-highlight">
                    <span>Sức chứa khả dụng thực tế:</span>
                    <strong id="statAvailableCapacity"><%= availableCapacity %></strong>
                    <span style="font-size: 13px; color: #64748B;">ghế mở bán</span>
                </div>
            </div>

<!-- Form lưu toàn bộ sơ đồ ghế (JSP vẽ thẳng các ô ghế và input bên trong) -->
            <form id="saveLayoutForm" method="POST" action="${pageContext.request.contextPath}/manager/seat-config">
                <input type="hidden" name="hallId" value="<%= hall != null ? hall.getHallId() : 0 %>">

                <!-- Vùng Canvas Sơ đồ ghế -->
                <div class="cf-seat-canvas-wrapper">
                    <!-- Màn hình chiếu -->
                    <div class="cf-screen">
                        <div class="cf-screen-label">MÀN HÌNH CHIẾU</div>
                    </div>

<!-- Lưới sơ đồ ghế vẽ trực tiếp bằng Java/JSP -->
                    <div id="seatGridContainer" class="cf-seat-grid">
               <%-- lặp từng hàng --%>
                        <% for (String r : rowList) {
                        //khởi tạo biến đếm ghế
                            int seatIndexInRow = 0;
                        %>
                        <%-- 1 row--%>
                        <div class="cf-grid-row">
                            <%-- in tên hàng bên trái --%>
                            <span class="cf-row-label"><%= r %></span>
                <%-- chạy cột ở mỗi hàng --%>
                            <% for (int c = 1; c <= maxCols; c++) {
                            //tra cứu trong hash map để lấy cell
                                SeatConfigDTO cell = seatLookup.get(r + "_" + c);
                                //lấy type ghế 
                                String type = (cell != null && cell.getSeatType() != null) ? cell.getSeatType().toUpperCase() : "STANDARD";
                                //lấy status ghế 
                                String status = (cell != null && cell.getStatus() != null)
                                        ? cell.getStatus().toUpperCase()
                                        : ((existingSeats == null || existingSeats.isEmpty()) ? "AVAILABLE" : "UNAVAILABLE");
                               // biến lưu danh sách cho css ghế
                                String cssClass = "cf-seat-box";
                                //biến lưu trên mặt ô ghế
                                String displayText;
                                String titleText;
                                String fallbackLabel;
                                  //gắn lối trống -       
                                if ("UNAVAILABLE".equals(status)) {
                                    cssClass += " EMPTY";
                                    displayText = "—";
                                    fallbackLabel = r + c;
                                    titleText = "Hàng " + r + " - Lối đi / Trống (Cột " + c + ")";
                                } else {
                                    seatIndexInRow++;
                                    //ghép tên hàng và số ghế thật vào ghế
                                    String seatLabel = r + seatIndexInRow;
                                    displayText = seatLabel;
                                    //màu
                                    fallbackLabel = seatLabel;
                                   //nếu cột chẵn thì ghép bên phải , lẻ thì ghép bên trái 
                                    if ("SWEETBOX".equals(type)) {
                                        boolean isLeft = (c % 2 == 1);
                                        int partnerC = isLeft ? (c + 1) : (c - 1);
                                        SeatConfigDTO partner = seatLookup.get(r + "_" + partnerC);
                                        //cùng loại ghế 
                                        if (partner != null && "SWEETBOX".equalsIgnoreCase(partner.getSeatType()) && !"UNAVAILABLE".equalsIgnoreCase(partner.getStatus())) {
                                            cssClass += isLeft ? " sweetbox-left" : " sweetbox-right";
                                        }
                                    }

                                    if ("MAINTENANCE".equals(status)) {
                                        cssClass += " " + type + " MAINTENANCE";
                                        titleText = seatLabel + " (Bảo trì / Hỏng - " + type + ")";
                                    } else {
                                        cssClass += " " + type;
                                        titleText = seatLabel + " (" + type + ")";
                                    }
                                }
                            %>
                            <div class="<%= cssClass %>"
                                 id="seat-<%= r %>-<%= c %>"
                                 data-row="<%= r %>"
                                 data-col="<%= c %>"
                                 data-label="<%= fallbackLabel %>"
                                 title="<%= titleText %>"
                                 onclick="clickSeat(this)">
                                <span class="cf-seat-label"><%= displayText %></span>
                                <input type="hidden" name="seatRow" value="<%= r %>">
                                <input type="hidden" name="seatNumber" value="<%= c %>">
                                <input type="hidden" name="seatType" class="inp-type" value="<%= type %>">
                                <input type="hidden" name="status" class="inp-status" value="<%= status %>">
                            </div>
                            <% } %>
                            <span class="cf-row-label"><%= r %></span>
                        </div>
                        <% } %>
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

                <div class="cf-footer-actions">
                    <a href="${pageContext.request.contextPath}/manager/halls<%= isAdmin && centre != null ? "?centreId=" + centre.getCentreId() : "" %>"
                       class="cf-btn cf-btn-secondary">
                        Hủy thay đổi
                    </a>

                    <button type="submit" class="cf-btn cf-btn-primary" style="padding: 10px 24px; font-size: 15px;">
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
                    <form class="cf-filter-form" method="GET" action="${baseUrl}">
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
                        <a href="${baseUrl}?hallId=<%= hall != null ? hall.getHallId() : 0 %>" class="cf-btn cf-btn-secondary">Xóa lọc</a>
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
                                        <form method="POST" action="${baseUrl}" style="display: inline-block; margin: 0;"
                                              onsubmit="return true; // confirm('<%= isAvailable ? ("Khóa bảo trì ghế " + s.getLabel() + "? Ghế này sẽ không thể đặt vé cho đến khi mở lại.") : ("Mở bảo trì lại ghế " + s.getLabel() + "?") %>');">
                                            <input type="hidden" name="action" value="toggleMaintenance">
                                            <input type="hidden" name="hallId" value="<%= hall != null ? hall.getHallId() : 0 %>">
                                            <input type="hidden" name="seatId" value="<%= s.getSeatId() %>">
                                            <input type="hidden" name="back" value="${backQuery}">
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

                    <% if (totalFilteredSeats != null && totalFilteredSeats > 0) { %>
                    <!-- THANH PHÂN TRANG (PAGINATION) -->
                    <div class="cf-pagination-bar">
                        <div class="cf-pagination-info">
                            Hiển thị <strong>${result.from} - ${result.to}</strong> trên tổng số <strong>${result.total}</strong> vị trí ghế
                        </div>
                        ${result.renderPager(pagerBase)}
                    </div>
                    <% } %>
                </div>
            </div>
        </main>
    </div>
</div>

<!-- ============================================================== -->
<!-- JAVASCRIPT ĐƠN GIẢN: TÔ MÀU GHẾ & ĐẾM SỨC CHỨA TRỰC TIẾP       -->
<!-- ============================================================== -->
<script>
    var selectedTool = 'STANDARD';
    var maxCols = <%= maxCols %>;

    // 1. Chọn công cụ trên Palette
    function chooseTool(toolName, btnElement) {
        selectedTool = toolName;
        document.querySelectorAll('.cf-tool-btn').forEach(function (btn) {
            btn.classList.remove('active');
        });
        if (btnElement) {
            btnElement.classList.add('active');
        }
    }

    // Hàm phụ: cập nhật input ẩn và class CSS của 1 ô ghế
    function applySeatStyle(box, type, status, pairClass) {
        box.querySelector('.inp-type').value = type;
        box.querySelector('.inp-status').value = status;
        box.className = 'cf-seat-box';

        if (status === 'UNAVAILABLE') {
            box.classList.add('EMPTY');
        } else if (status === 'MAINTENANCE') {
            box.classList.add(type, 'MAINTENANCE');
        } else {
            box.classList.add(type);
        }
        if (pairClass && status !== 'UNAVAILABLE') {
            box.classList.add(pairClass);
        }
    }

    // 2. Xử lý khi click trực tiếp vào 1 ô ghế
    function clickSeat(seatBox) {
        var row = seatBox.dataset.row;
        var col = parseInt(seatBox.dataset.col, 10);
        var type = seatBox.querySelector('.inp-type').value;
        var status = seatBox.querySelector('.inp-status').value;
        var partnerCol = (col % 2 === 1) ? (col + 1) : (col - 1);
        var partnerBox = document.getElementById('seat-' + row + '-' + partnerCol);

        if (selectedTool === 'EMPTY') {
            if (status === 'UNAVAILABLE') {
                applySeatStyle(seatBox, 'STANDARD', 'AVAILABLE');
            } else {
                applySeatStyle(seatBox, 'STANDARD', 'UNAVAILABLE');
                if (partnerBox) partnerBox.classList.remove('sweetbox-left', 'sweetbox-right');
            }
        } else if (selectedTool === 'MAINTENANCE') {
            var pairClass = seatBox.classList.contains('sweetbox-left') ? 'sweetbox-left'
                          : (seatBox.classList.contains('sweetbox-right') ? 'sweetbox-right' : '');
            var nextStatus = (status === 'MAINTENANCE') ? 'AVAILABLE' : 'MAINTENANCE';
            applySeatStyle(seatBox, type, nextStatus, pairClass);
        } else if (selectedTool === 'SWEETBOX') {
            var leftCol = (col % 2 === 1) ? col : (col - 1);
            var rightCol = leftCol + 1;
            if (rightCol <= maxCols) {
                var leftBox = document.getElementById('seat-' + row + '-' + leftCol);
                var rightBox = document.getElementById('seat-' + row + '-' + rightCol);
                var bothSweet = leftBox.querySelector('.inp-type').value === 'SWEETBOX'
                             && rightBox.querySelector('.inp-type').value === 'SWEETBOX'
                             && leftBox.querySelector('.inp-status').value !== 'UNAVAILABLE'
                             && rightBox.querySelector('.inp-status').value !== 'UNAVAILABLE';
                if (bothSweet) {
                    applySeatStyle(leftBox, 'STANDARD', 'AVAILABLE');
                    applySeatStyle(rightBox, 'STANDARD', 'AVAILABLE');
                } else {
                    applySeatStyle(leftBox, 'SWEETBOX', 'AVAILABLE', 'sweetbox-left');
                    applySeatStyle(rightBox, 'SWEETBOX', 'AVAILABLE', 'sweetbox-right');
                }
            }
        } else {
            applySeatStyle(seatBox, selectedTool, 'AVAILABLE');
            if (partnerBox) partnerBox.classList.remove('sweetbox-left', 'sweetbox-right');
        }

        updateStats();
    }

    // 3. Đếm lại số lượng ghế & đánh số thứ tự A1, A2... trên giao diện
    function updateStats() {
        var total = 0, std = 0, vip = 0, sweet = 0, maint = 0, empty = 0;

        document.querySelectorAll('.cf-grid-row').forEach(function (rowEl) {
            var seatNum = 0;
            rowEl.querySelectorAll('.cf-seat-box').forEach(function (box) {
                total++;
                var row = box.dataset.row;
                var type = box.querySelector('.inp-type').value;
                var status = box.querySelector('.inp-status').value;
                var labelSpan = box.querySelector('.cf-seat-label');

                if (status === 'UNAVAILABLE') {
                    empty++;
                    labelSpan.innerText = '—';
                } else {
                    seatNum++;
                    labelSpan.innerText = row + seatNum;
                    if (status === 'MAINTENANCE') maint++;
                    else if (type === 'VIP') vip++;
                    else if (type === 'SWEETBOX') sweet++;
                    else std++;
                }
            });
        });

        document.getElementById('statTotalCells').innerText = total;
        document.getElementById('statStandard').innerText = std;
        document.getElementById('statVip').innerText = vip;
        document.getElementById('statSweetbox').innerText = sweet;
        document.getElementById('statMaintenance').innerText = maint;
        document.getElementById('statEmpty').innerText = empty;
        document.getElementById('statAvailableCapacity').innerText = std + vip + sweet;
    }

    // 4. Kiểm tra số hàng/cột trước khi gửi form Tạo lưới mới
    function checkGridInput() {
        var numRows = parseInt(document.getElementById('gridRowsInput').value, 10) || 0;
        var numCols = parseInt(document.getElementById('gridColsInput').value, 10) || 0;
        if (numRows < 1 || numRows > 26) {
            alert("Số hàng phải từ 1 đến 26 (tương ứng A-Z)!");
            return false;
        }
        if (numCols < 1 || numCols > 30) {
            alert("Số ghế mỗi hàng phải từ 1 đến 30!");
            return false;
        }
        return true;
    }

    // 5. Tự động ẩn thông báo Alert sau 3.5 giây
    document.addEventListener("DOMContentLoaded", function () {
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
