<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.entity.User,model.dto.report.OperationsSummaryDTO" %>
<%
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && session.getAttribute("user") instanceof User) {
        authRole = ((User) session.getAttribute("user")).getRole();
    }
    if (!"MANAGER".equalsIgnoreCase(authRole) && !"ADMIN".equalsIgnoreCase(authRole)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Quản lý (MANAGER) hoặc Quản trị viên (ADMIN) mới có quyền truy cập trang này.");
        return;
    }

    request.setAttribute("activeNav", "dashboard");
    request.setAttribute("pageTitle", "Tổng quan điều hành cụm rạp");
    request.setAttribute("pageBreadcrumb", "Dashboard");

    User manager = (User) request.getAttribute("managerUser");
    if (manager == null && session.getAttribute("user") instanceof User) {
        manager = (User) session.getAttribute("user");
    }

    String managerName = manager != null && manager.getFullName() != null && !manager.getFullName().isBlank()
            ? manager.getFullName().trim() : "Quản lý";
    String centreName = (String) request.getAttribute("managerCentreName");
    if (centreName == null || centreName.isBlank()) {
        centreName = manager != null && manager.getCentreName() != null && !manager.getCentreName().isBlank()
                ? manager.getCentreName().trim() : ("ADMIN".equalsIgnoreCase(authRole) ? "Toàn hệ thống" : "Cụm rạp của bạn");
    }

    OperationsSummaryDTO summary = (OperationsSummaryDTO) request.getAttribute("operationsSummary");
    int showtimes = summary == null ? 0 : summary.getShowtimes();
    int soldSeats = summary == null ? 0 : summary.getSoldSeats();
    double occupancy = summary == null ? 0 : summary.getOccupancy();
    int stockAlerts = summary == null ? 0 : summary.getOutOfStockItems();
    double checkInRate = summary == null ? 0 : summary.getCheckInRate();
    int totalHalls = request.getAttribute("totalHalls") instanceof Number
            ? ((Number) request.getAttribute("totalHalls")).intValue() : 0;
    int availableHalls = request.getAttribute("availableHalls") instanceof Number
            ? ((Number) request.getAttribute("availableHalls")).intValue() : 0;
    int totalCapacity = request.getAttribute("totalCapacity") instanceof Number
            ? ((Number) request.getAttribute("totalCapacity")).intValue() : 0;
    long todayShowtimeCount = request.getAttribute("todayShowtimes") instanceof Number
            ? ((Number) request.getAttribute("todayShowtimes")).longValue() : 0L;
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Dashboard Quản lý | CineFlow</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/reset.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/variables.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/buttons.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/badges.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/forms.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/tables.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/alerts.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/shell.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/dashboard.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/admin-style.css?v=1">
</head>
<body>
<div class="admin-layout cf-manager-shell">
    <jsp:include page="/WEB-INF/fragments/managerSidebar.jspf"/>

    <div class="admin-main cf-manager-main">
        <jsp:include page="/WEB-INF/fragments/managerTopbar.jspf"/>

        <main class="admin-content cf-content manager-dashboard">
            <c:if test="${not empty dashboardError}">
                <div class="cf-alert cf-alert-warning" role="alert"><c:out value="${dashboardError}" /></div>
            </c:if>
            <!-- BANNER CHÀO MỪNG QUẢN LÝ -->
            <div class="dashboard-welcome-banner">
                <div class="banner-content">
                    <div class="banner-badge">TRUNG TÂM ĐIỀU HÀNH CỤM RẠP</div>
                    <h1>Xin chào, <%= managerName %>!</h1>
                    <p>Quản lý lịch chiếu, phòng chiếu và nhân sự tại <strong><%= centreName %></strong>.</p>
                    <div class="banner-actions">
                        <a href="${pageContext.request.contextPath}/showtimes" class="cf-btn cf-btn-primary">Quản lý lịch chiếu</a>
                        <a href="${pageContext.request.contextPath}/report/operations" class="cf-btn cf-btn-ghost">Chỉ số vận hành</a>
                    </div>
                </div>
                <div class="banner-stat-bubble">
                    <small>SUẤT CHIẾU 7 NGÀY TỚI</small>
                    <strong><%= String.format("%,d", showtimes) %></strong>
                </div>
            </div>

            <!-- CÁC THẺ CHỈ SỐ KPI VẬN HÀNH RẠP CHIẾU -->
            <div class="kpi-grid" aria-label="Chỉ số cụm rạp">
                <div class="kpi-card ruby">
                    <div class="kpi-card-header">
                        <span class="kpi-title">Suất chiếu 7 ngày tới</span>
                        <span class="kpi-icon">🎬</span>
                    </div>
                    <div class="kpi-value"><%= String.format("%,d", showtimes) %></div>
                    <div class="kpi-subtext">
                        <span class="kpi-trend-up">●</span> Theo lịch tại <%= centreName %>
                    </div>
                </div>

                <div class="kpi-card emerald">
                    <div class="kpi-card-header">
                        <span class="kpi-title">Ghế đã bán</span>
                        <span class="kpi-icon">🎟️</span>
                    </div>
                    <div class="kpi-value"><%= String.format("%,d", soldSeats) %></div>
                    <div class="kpi-subtext">
                        Lượt vé xem phim được xác nhận
                    </div>
                </div>

                <div class="kpi-card sky">
                    <div class="kpi-card-header">
                        <span class="kpi-title">Tỷ lệ lấp đầy</span>
                        <span class="kpi-icon">◔</span>
                    </div>
                    <div class="kpi-value"><%= String.format("%.1f%%", occupancy) %></div>
                    <div class="kpi-subtext">
                        Tính trên tổng sức chứa phòng chiếu
                    </div>
                </div>

                <div class="kpi-card amber">
                    <div class="kpi-card-header">
                        <span class="kpi-title">Cảnh báo tồn kho</span>
                        <span class="kpi-icon">🍿</span>
                    </div>
                    <div class="kpi-value"><%= String.format("%,d", stockAlerts) %> mặt hàng</div>
                    <div class="kpi-subtext">
                        <%= stockAlerts > 0 ? "Mặt hàng bắp nước cần nhập thêm" : "Kho hàng hoạt động ổn định" %>
                    </div>
                </div>
            </div>

            <!-- PHÍM TẮT TRUY CẬP NHANH VẬN HÀNH -->
            <div class="quick-nav-grid" aria-label="Truy cập nhanh">
                <a href="${pageContext.request.contextPath}/showtimes" class="quick-nav-card">
                    <div class="quick-nav-icon" style="background:#FEE2E2;color:#FF2855">▣</div>
                    <div class="quick-nav-meta">
                        <h3>Lịch chiếu phim</h3>
                        <p>Tạo và quản lý suất chiếu, phòng và ghế</p>
                    </div>
                </a>

                <a href="${pageContext.request.contextPath}/manager/halls" class="quick-nav-card">
                    <div class="quick-nav-icon" style="background:#CFFAFE;color:#0E7490">🎬</div>
                    <div class="quick-nav-meta">
                        <h3>Phòng chiếu &amp; Ghế</h3>
                        <p>Sơ đồ phòng chiếu, định dạng 2D, 3D, IMAX</p>
                    </div>
                </a>

                <a href="${pageContext.request.contextPath}/staffList" class="quick-nav-card">
                    <div class="quick-nav-icon" style="background:#E0F2FE;color:#0284C7">♙</div>
                    <div class="quick-nav-meta">
                        <h3>Nhân sự cụm rạp</h3>
                        <p>Quản lý tài khoản bán vé &amp; soát vé</p>
                    </div>
                </a>

                <a href="${pageContext.request.contextPath}/fnb" class="quick-nav-card">
                    <div class="quick-nav-icon" style="background:#D1FAE5;color:#059669">🍿</div>
                    <div class="quick-nav-meta">
                        <h3>Bắp nước &amp; Kho</h3>
                        <p>Quản lý thực đơn combo và tồn kho chi nhánh</p>
                    </div>
                </a>

                <a href="${pageContext.request.contextPath}/manager/payments" class="quick-nav-card">
                    <div class="quick-nav-icon" style="background:#F3E8FF;color:#7E22CE">▤</div>
                    <div class="quick-nav-meta">
                        <h3>Theo dõi giao dịch</h3>
                        <p>Tra cứu thanh toán tại quầy và online</p>
                    </div>
                </a>
            </div>

            <!-- KHỐI 2 CỘT: CHỈ SỐ VẬN HÀNH & GỢI Ý ĐIỀU HÀNH -->
            <div class="dashboard-split-grid">
                <!-- CỘT 1: CHỈ SỐ TIẾN ĐỘ -->
                <div class="admin-card" style="margin-bottom:0">
                    <div class="admin-card-header">
                        <div>
                            <h2>Tình hình cụm rạp</h2>
                            <p>Hiệu suất hoạt động trong 7 ngày tới</p>
                        </div>
                        <a href="${pageContext.request.contextPath}/report/operations" class="cf-btn cf-btn-secondary cf-btn-sm">Báo cáo chi tiết →</a>
                    </div>
                    <div class="admin-card-body">
                        <div class="metric-progress-item">
                            <div class="metric-progress-header">
                                <span>Sức chứa phòng chiếu đã sử dụng</span>
                                <strong><%= String.format("%.1f%%", occupancy) %></strong>
                            </div>
                            <div class="progress-bar-bg">
                                <div class="progress-bar-fill" style="width:<%= Math.min(100.0, occupancy) %>%;background:#FF2855"></div>
                            </div>
                        </div>

                        <div class="metric-progress-item">
                            <div class="metric-progress-header">
                                <span>Tỷ lệ khách đã soát vé check-in</span>
                                <strong><%= String.format("%.1f%%", checkInRate) %></strong>
                            </div>
                            <div class="progress-bar-bg">
                                <div class="progress-bar-fill" style="width:<%= Math.min(100.0, checkInRate) %>%;background:#0284c7"></div>
                            </div>
                        </div>

                        <div class="metric-progress-item">
                            <div class="metric-progress-header">
                                <span>Mặt hàng cần bổ sung tồn kho</span>
                                <strong><%= String.format("%,d", stockAlerts) %> mặt hàng</strong>
                            </div>
                            <div class="progress-bar-bg">
                                <div class="progress-bar-fill" style="width:<%= stockAlerts > 0 ? 100 : 0 %>%;background:#f59e0b"></div>
                            </div>
                        </div>

                        <div class="metric-progress-item" style="margin-top: 20px;">
                            <div class="metric-progress-header">
                                <span>Phòng chiếu khả dụng</span>
                                <strong><%= String.format("%,d / %,d", availableHalls, totalHalls) %></strong>
                            </div>
                            <div class="progress-bar-bg">
                                <div class="progress-bar-fill" style="width:<%= totalHalls > 0 ? Math.min(100.0, availableHalls * 100.0 / totalHalls) : 0 %>%;background:#10b981"></div>
                            </div>
                            <p style="margin: 6px 0 0; font-size: 12px; color: #94a3b8;">
                                Tổng sức chứa <%= String.format("%,d", totalCapacity) %> ghế
                            </p>
                        </div>
                        <div class="metric-progress-item" style="margin-top: 18px; margin-bottom: 18px;">
                            <div class="metric-progress-header">
                                <span>Suất chiếu hôm nay</span>
                                <strong><%= String.format("%,d", todayShowtimeCount) %></strong>
                            </div>
                            <p style="margin: 6px 0 0; font-size: 12px; color: #94a3b8;">
                                Đã loại trừ các suất chiếu bị hủy
                            </p>
                        </div>

                        <div style="margin-top: 24px; padding: 14px; background: rgba(255, 255, 255, 0.04); border: 1px solid var(--cf-border); border-radius: 8px;">
                            <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 4px;">
                                <span style="font-size: 16px;">💡</span>
                                <strong style="font-size: 13px; color: #F8FAFC;">Điều hành cụm rạp</strong>
                            </div>
                            <p style="margin: 0; font-size: 12px; color: #94a3b8; line-height: 1.5;">
                                Hãy kiểm tra <a href="${pageContext.request.contextPath}/showtimes" style="color: var(--cf-primary); font-weight: 600;">Lịch chiếu phim</a> để đảm bảo phòng chiếu và các suất giờ vàng được bố trí hợp lý.
                            </p>
                        </div>
                    </div>
                </div>

                <!-- CỘT 2: GỢI Ý & ĐIỀU HÀNH NHANH -->
                <div class="admin-card" style="margin-bottom:0">
                    <div class="admin-card-header">
                        <div>
                            <h2>Gợi ý điều hành &amp; Tác vụ</h2>
                            <p>Các công việc trọng tâm cần theo dõi định kỳ</p>
                        </div>
                    </div>
                    <div class="admin-card-body">
                        <div class="metric-progress-item" style="margin-bottom: 18px;">
                            <div class="metric-progress-header">
                                <span style="font-weight: 600; color: #F8FAFC;">Lịch chiếu phim</span>
                                <a href="${pageContext.request.contextPath}/showtimes" style="color: var(--cf-primary); font-size: 12px; text-decoration: none;">Mở quản lý →</a>
                            </div>
                            <p style="margin: 4px 0 0; font-size: 12px; color: #94a3b8;">Rà soát phòng, khung thời gian chiếu và ghế trống trước giờ mở màn hình.</p>
                        </div>

                        <div class="metric-progress-item" style="margin-bottom: 18px;">
                            <div class="metric-progress-header">
                                <span style="font-weight: 600; color: #F8FAFC;">Tồn kho bắp nước</span>
                                <a href="${pageContext.request.contextPath}/fnb/stock" style="color: var(--cf-primary); font-size: 12px; text-decoration: none;">Kiểm tra kho →</a>
                            </div>
                            <p style="margin: 4px 0 0; font-size: 12px; color: #94a3b8;">Theo dõi nguyên vật liệu và đồ ăn uống để kịp thời lập phiếu bổ sung kho.</p>
                        </div>

                        <div class="metric-progress-item" style="margin-bottom: 18px;">
                            <div class="metric-progress-header">
                                <span style="font-weight: 600; color: #F8FAFC;">Theo dõi giao dịch</span>
                                <a href="${pageContext.request.contextPath}/manager/payments" style="color: var(--cf-primary); font-size: 12px; text-decoration: none;">Xem giao dịch →</a>
                            </div>
                            <p style="margin: 4px 0 0; font-size: 12px; color: #94a3b8;">Đối soát các thanh toán tại quầy và online để đảm bảo tính minh bạch doanh thu.</p>
                        </div>

                        <div class="metric-progress-item">
                            <div class="metric-progress-header">
                                <span style="font-weight: 600; color: #F8FAFC;">Đội ngũ nhân sự</span>
                                <a href="${pageContext.request.contextPath}/staffList" style="color: var(--cf-primary); font-size: 12px; text-decoration: none;">Xem nhân sự →</a>
                            </div>
                            <p style="margin: 4px 0 0; font-size: 12px; color: #94a3b8;">Phân công vị trí Bán vé tại quầy và Soát vé Check-in cho nhân viên chi nhánh.</p>
                        </div>
                    </div>
                </div>
            </div>
        </main>
    </div>
</div>
</body>
</html>
