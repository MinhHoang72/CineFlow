<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.dto.report.ReportSummary,model.dto.report.RevenueReport,model.entity.Movie,model.entity.Centre,model.entity.User,java.util.List,java.math.BigDecimal" %>
<%
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && session.getAttribute("user") instanceof model.entity.User) {
        authRole = ((model.entity.User) session.getAttribute("user")).getRole();
    }
    if (!"ADMIN".equalsIgnoreCase(authRole)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Quản trị viên (ADMIN) mới có quyền truy cập trang này.");
        return;
    }

    ReportSummary s = (ReportSummary) request.getAttribute("reportSummary");
    if (s == null) {
        response.sendRedirect(request.getContextPath() + "/dashboard");
        return;
    }

    request.setAttribute("activeNav", "dashboard");
    request.setAttribute("pageTitle", "Tổng quan điều hành");
    request.setAttribute("pageBreadcrumb", "Dashboard");
    List<RevenueReport> topMovies = (List<RevenueReport>) request.getAttribute("topMovies");
    List<Movie> movies = (List<Movie>) request.getAttribute("movies");
    List<Centre> centres = (List<Centre>) request.getAttribute("centres");
    User currentUser = (User) session.getAttribute("user");
    String adminName = currentUser != null && currentUser.getFullName() != null ? currentUser.getFullName() : "Admin";

    BigDecimal revenue = s != null && s.getRevenue() != null ? s.getRevenue() : BigDecimal.ZERO;
    int bookingsCount = s != null ? s.getBookings() : 0;
    int ticketsCount = s != null ? s.getTickets() : 0;
    int activeMoviesCount = s != null ? s.getActiveMovies() : (movies != null ? movies.size() : 0);
    int activeCentresCount = s != null ? s.getActiveCentres() : (centres != null ? centres.size() : 0);
    int activeUsersCount = s != null ? s.getActiveUsers() : 0;
    double occupancyRate = s != null ? s.getOccupancy() : 0.0;
    int totalCentresCount = centres != null ? centres.size() : activeCentresCount;
    double centreReadyRate = totalCentresCount > 0 ? (activeCentresCount * 100.0 / totalCentresCount) : 0.0;
%>
<%!
    private String formatMoney(BigDecimal n) {
        if (n == null) return "0 đ";
        return String.format("%,.0f đ", n);
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard Admin | CineFlow</title>
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
<div class="admin-layout">
    <jsp:include page="/WEB-INF/fragments/adminSidebar.jspf" />

    <div class="admin-main">
        <jsp:include page="/WEB-INF/fragments/adminTopbar.jspf" />

        <main class="admin-content">
            <!-- BANNER CHÀO MỪNG QUẢN TRỊ VIÊN -->
            <div class="dashboard-welcome-banner">
                <div class="banner-content">
                    <div class="banner-badge">TRUNG TÂM ĐIỀU HÀNH TOÀN CHUỖI</div>
                    <h1>Xin chào, <%= adminName %>!</h1>
                    <p>Tổng quan vận hành CineFlow và các chỉ số quan trọng trong ngày.</p>
                    <div class="banner-actions">
                        <a href="${pageContext.request.contextPath}/admin/movies" class="cf-btn cf-btn-primary">Quản lý phim</a>
                        <a href="${pageContext.request.contextPath}/reports" class="cf-btn cf-btn-ghost">Báo cáo doanh thu</a>
                    </div>
                </div>
                <div class="banner-stat-bubble">
                    <small>DOANH THU TOÀN HỆ THỐNG</small>
                    <strong><%= formatMoney(revenue) %></strong>
                </div>
            </div>

            <!-- CÁC THẺ CHỈ SỐ DOANH THU VÀ HIỆU SUẤT KPI -->
            <div class="kpi-grid">
                <div class="kpi-card ruby">
                    <div class="kpi-card-header">
                        <span class="kpi-title">Tổng doanh thu</span>
                        <span class="kpi-icon">💰</span>
                    </div>
                    <div class="kpi-value"><%= formatMoney(revenue) %></div>
                    <div class="kpi-subtext">
                        <span class="kpi-trend-up">●</span> Từ <%= bookingsCount %> giao dịch hoàn tất
                    </div>
                </div>

                <div class="kpi-card emerald">
                    <div class="kpi-card-header">
                        <span class="kpi-title">Vé bán ra</span>
                        <span class="kpi-icon">🎟️</span>
                    </div>
                    <div class="kpi-value"><%= String.format("%,d", ticketsCount) %></div>
                    <div class="kpi-subtext">
                        Lượt ghế xem phim được xác nhận
                    </div>
                </div>

                <div class="kpi-card sky">
                    <div class="kpi-card-header">
                        <span class="kpi-title">Phim trong hệ thống</span>
                        <span class="kpi-icon">🎬</span>
                    </div>
                    <div class="kpi-value"><%= activeMoviesCount %></div>
                    <div class="kpi-subtext">
                        Phim đang chiếu & sắp khởi chiếu
                    </div>
                </div>

                <div class="kpi-card amber">
                    <div class="kpi-card-header">
                        <span class="kpi-title">Trung tâm hoạt động</span>
                        <span class="kpi-icon">🏢</span>
                    </div>
                    <div class="kpi-value"><%= activeCentresCount %></div>
                    <div class="kpi-subtext">
                        Trung tâm chiếu phim CineFlow
                    </div>
                </div>
            </div>

            <!-- PHÍM TẮT ĐIỀU HƯỚNG NHANH -->
            <div class="quick-nav-grid">
                <a href="${pageContext.request.contextPath}/admin/movies" class="quick-nav-card">
                    <div class="quick-nav-icon" style="background: #FEE2E2; color: #FF2855;">▣</div>
                    <div class="quick-nav-meta">
                        <h3>Danh mục phim</h3>
                        <p>Thêm phim & thiết lập phân phối</p>
                    </div>
                </a>

                <a href="${pageContext.request.contextPath}/reports" class="quick-nav-card">
                    <div class="quick-nav-icon" style="background: #D1FAE5; color: #059669;">▤</div>
                    <div class="quick-nav-meta">
                        <h3>Báo cáo doanh thu</h3>
                        <p>Phân tích theo rạp và từng phim</p>
                    </div>
                </a>

                <a href="${pageContext.request.contextPath}/staffList" class="quick-nav-card">
                    <div class="quick-nav-icon" style="background: #E0F2FE; color: #0284c7;">♙</div>
                    <div class="quick-nav-meta">
                        <h3>Nhân sự & Cụm rạp</h3>
                        <p>Quản lý tài khoản quản trị</p>
                    </div>
                </a>

                <a href="${pageContext.request.contextPath}/account" class="quick-nav-card">
                    <div class="quick-nav-icon" style="background: #F3E8FF; color: #7e22ce;">●</div>
                    <div class="quick-nav-meta">
                        <h3>Hồ sơ quản trị</h3>
                        <p>Thông tin cá nhân & bảo mật</p>
                    </div>
                </a>
            </div>

            <!-- BẢNG PHÂN CHIA 2 CỘT: TOP PHIM & TRẠNG THÁI HỆ THỐNG -->
            <div class="dashboard-split-grid">
                <!-- BẢNG XẾP HẠNG DOANH THU THEO PHIM -->
                <div class="admin-card" style="margin-bottom: 0;">
                    <div class="admin-card-header">
                        <div>
                            <h2>Top phim doanh thu cao nhất</h2>
                            <p>Xếp hạng hiệu quả bán vé trên toàn bộ hệ thống rạp</p>
                        </div>
                        <a href="${pageContext.request.contextPath}/reports" class="cf-btn cf-btn-secondary cf-btn-sm">Xem đầy đủ →</a>
                    </div>
                    <div class="table-responsive">
                        <table class="cf-table">
                            <thead>
                                <tr>
                                    <th>Thứ tự</th>
                                    <th>Tên phim</th>
                                    <th>Đơn đặt vé</th>
                                    <th>Vé đã bán</th>
                                    <th style="text-align: right;">Doanh thu</th>
                                </tr>
                            </thead>
                            <tbody>
                                <% if (topMovies == null || topMovies.isEmpty()) { %>
                                    <tr>
                                        <td colspan="5" class="cf-empty-state">Chưa có giao dịch đặt vé nào được ghi nhận.</td>
                                    </tr>
                                <% } else {
                                    int rank = 1;
                                    for (RevenueReport tr : topMovies) {
                                        if (rank > 5) break;
                                %>
                                    <tr>
                                        <td>
                                            <span style="font-weight: 700; color: <%= rank == 1 ? "#FF2855" : rank <= 3 ? "#f59e0b" : "#94a3b8" %>">
                                                <%= rank %>
                                            </span>
                                        </td>
                                        <td><strong><%= tr.getName() %></strong></td>
                                        <td><%= String.format("%,d", tr.getBookings()) %> đơn</td>
                                        <td><span class="cf-badge cf-badge-info"><%= String.format("%,d", tr.getTickets()) %> vé</span></td>
                                        <td style="text-align: right; font-weight: 700; color: #F8FAFC;"><%= formatMoney(tr.getRevenue()) %></td>
                                    </tr>
                                <% rank++; }} %>
                            </tbody>
                        </table>
                    </div>
                </div>

                <!-- SYSTEM STATS & METRICS -->
                <div class="admin-card" style="margin-bottom: 0;">
                    <div class="admin-card-header">
                        <div>
                            <h2>Chỉ số vận hành</h2>
                            <p>Đo lường hiệu suất chuỗi rạp</p>
                        </div>
                    </div>
                    <div class="admin-card-body">
                        <div class="metric-progress-item">
                            <div class="metric-progress-header">
                                <span>Tỷ lệ lấp đầy suất chiếu</span>
                                <strong><%= String.format("%.1f%%", occupancyRate) %></strong>
                            </div>
                            <div class="progress-bar-bg">
                                <div class="progress-bar-fill" style="width: <%= Math.min(100.0, occupancyRate) %>%; background: #FF2855;"></div>
                            </div>
                        </div>

                        <div class="metric-progress-item">
                            <div class="metric-progress-header">
                                <a href="${pageContext.request.contextPath}/admin/customers" style="color: inherit; text-decoration: none;" onmouseover="this.style.color='var(--cf-primary)'" onmouseout="this.style.color='inherit'">
                                    <span>Thành viên hoạt động →</span>
                                </a>
                                <strong><%= activeUsersCount %> tài khoản</strong>
                            </div>
                            <div class="progress-bar-bg">
                                <div class="progress-bar-fill" style="width: 85%; background: #0284c7;"></div>
                            </div>
                        </div>

                        <div class="metric-progress-item">
                            <div class="metric-progress-header">
                                <span>Trung tâm sẵn sàng phục vụ</span>
                                <strong><%= activeCentresCount %>/<%= totalCentresCount %> trung tâm (<%= String.format("%.0f", centreReadyRate) %>%)</strong>
                            </div>
                            <div class="progress-bar-bg">
                                <div class="progress-bar-fill" style="width: <%= Math.min(100.0, centreReadyRate) %>%; background: #10b981;"></div>
                            </div>
                        </div>

                        <div style="margin-top: 24px; padding: 14px; background: rgba(255, 255, 255, 0.04); border: 1px solid var(--cf-border); border-radius: 8px;">
                            <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 4px;">
                                <span style="font-size: 16px;">💡</span>
                                <strong style="font-size: 13px; color: #F8FAFC;">Gợi ý điều hành</strong>
                            </div>
                            <p style="margin: 0; font-size: 12px; color: #94a3b8; line-height: 1.5;">
                                Hãy thường xuyên kiểm tra mục <a href="${pageContext.request.contextPath}/admin/movies" style="color: var(--cf-primary); font-weight: 600;">Quản lý phim</a> để chuyển trạng thái phim sang "Đang chiếu" khi đến ngày phát hành.
                            </p>
                        </div>
                    </div>
                </div>
            </div>
        </main>
    </div>
</div>
</body>
</html>
