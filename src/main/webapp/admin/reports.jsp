<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.ReportSummary,model.RevenueReport,model.ShowtimeMetric,java.util.List,java.math.BigDecimal" %>
<%
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && session.getAttribute("user") instanceof model.User) {
        authRole = ((model.User) session.getAttribute("user")).getRole();
    }
    boolean isAdmin = "ADMIN".equalsIgnoreCase(authRole);
    if (!isAdmin && !"MANAGER".equalsIgnoreCase(authRole)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập trang này.");
        return;
    }

    ReportSummary s = (ReportSummary) request.getAttribute("reportSummary");
    if (s == null) {
        response.sendRedirect(request.getContextPath() + "/reports");
        return;
    }

    request.setAttribute("activeNav", "reports");
    request.setAttribute("pageTitle", isAdmin ? "Báo cáo & Phân tích toàn chuỗi" : "Báo cáo hoạt động");
    request.setAttribute("pageBreadcrumb", isAdmin ? "Báo cáo toàn hệ thống" : "Báo cáo");
    List<RevenueReport> centres = (List<RevenueReport>) request.getAttribute("centreRevenue");
    List<RevenueReport> movies = (List<RevenueReport>) request.getAttribute("movieRevenue");
    List<ShowtimeMetric> showtimes = (List<ShowtimeMetric>) request.getAttribute("showtimeMetrics");

    BigDecimal totalRev = s != null && s.getRevenue() != null ? s.getRevenue() : BigDecimal.ZERO;
%>
<%!
    private String money(BigDecimal n) {
        if (n == null) return "0 đ";
        return String.format("%,.0f đ", n);
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= isAdmin ? "Báo cáo toàn hệ thống | CineFlow Admin" : "Báo cáo hoạt động | CineFlow Manager" %></title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/admin-style.css?v=<%= System.currentTimeMillis() %>">
    <style>
        .report-tabs {
            display: flex;
            gap: 8px;
            border-bottom: 2px solid var(--cf-border);
            margin-bottom: 24px;
        }
        .report-tab-btn {
            padding: 12px 20px;
            background: transparent;
            border: none;
            border-bottom: 2px solid transparent;
            margin-bottom: -2px;
            font-size: 13.5px;
            font-weight: 600;
            color: var(--cf-text-muted);
            cursor: pointer;
            transition: var(--cf-transition);
            display: flex;
            align-items: center;
            gap: 8px;
        }
        .report-tab-btn:hover {
            color: var(--cf-text);
        }
        .report-tab-btn.active {
            color: var(--cf-primary);
            border-bottom-color: var(--cf-primary);
        }
        .report-tab-pane {
            display: none;
        }
        .report-tab-pane.active {
            display: block;
        }
    </style>
</head>
<body>
<div class="admin-layout">
    <jsp:include page='<%= isAdmin ? "/admin/sidebar.jsp" : "/manager/sidebar.jsp" %>' />

    <div class="admin-main">
        <jsp:include page='<%= isAdmin ? "/admin/topbar.jsp" : "/manager/topbar.jsp" %>' />

        <main class="admin-content">
            <!-- HERO -->
            <div class="page-hero">
                <div class="page-hero-text">
                    <p class="eyebrow"><%= isAdmin ? "PHÂN TÍCH TOÀN HỆ THỐNG" : "BÁO CÁO HOẠT ĐỘNG" %></p>
                    <h1><%= isAdmin ? "Báo cáo & Phân tích tài chính chuỗi CineFlow" : "Báo cáo & Phân tích hoạt động kinh doanh" %></h1>
                    <p><%= isAdmin ? "Theo dõi tổng hợp doanh thu vé, hiệu suất bán vé từng cụm rạp và thống kê phim ăn khách." : "Tổng hợp doanh thu, lượng vé bán và hiệu suất suất chiếu." %></p>
                </div>
                <div class="page-hero-actions">
                    <button type="button" class="cf-btn cf-btn-secondary" onclick="window.print();">
                        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 6 2 18 2 18 9"></polyline><path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"></path><rect x="6" y="14" width="12" height="8"></rect></svg>
                        In báo cáo
                    </button>
                </div>
            </div>

            <!-- 4 SUMMARY KPIS -->
            <div class="kpi-grid">
                <div class="kpi-card ruby">
                    <div class="kpi-card-header">
                        <span class="kpi-title"><%= isAdmin ? "DOANH THU TOÀN CHUỖI" : "TỔNG DOANH THU" %></span>
                        <span class="kpi-icon">💰</span>
                    </div>
                    <div class="kpi-value"><%= money(totalRev) %></div>
                    <div class="kpi-subtext">Doanh thu vé đã thanh toán thành công</div>
                </div>

                <div class="kpi-card emerald">
                    <div class="kpi-card-header">
                        <span class="kpi-title">ĐƠN ĐẶT VÉ</span>
                        <span class="kpi-icon">📋</span>
                    </div>
                    <div class="kpi-value"><%= s != null ? String.format("%,d", s.getBookings()) : 0 %></div>
                    <div class="kpi-subtext">Tổng số giao dịch thành công</div>
                </div>

                <div class="kpi-card sky">
                    <div class="kpi-card-header">
                        <span class="kpi-title">TRUNG TÂM ĐANG KINH DOANH</span>
                        <span class="kpi-icon">🏢</span>
                    </div>
                    <div class="kpi-value"><%= s != null ? s.getActiveCentres() : 0 %></div>
                    <div class="kpi-subtext">Trung tâm hoạt động</div>
                </div>

                <div class="kpi-card amber">
                    <div class="kpi-card-header">
                        <span class="kpi-title">TỔNG KHÁCH HÀNG</span>
                        <span class="kpi-icon">👥</span>
                    </div>
                    <div class="kpi-value"><%= s != null ? String.format("%,d", s.getActiveUsers()) : 0 %></div>
                    <div class="kpi-subtext">Tài khoản thành viên hoạt động</div>
                </div>
            </div>

            <!-- TABS -->
            <div class="report-tabs">
                <button type="button" class="report-tab-btn active" onclick="switchReportTab('tabCentres', this)">
                    <span>🏢</span> Doanh thu theo trung tâm (<%= centres != null ? centres.size() : 0 %>)
                </button>
                <button type="button" class="report-tab-btn" onclick="switchReportTab('tabMovies', this)">
                    <span>🎬</span> Xếp hạng phim (<%= movies != null ? movies.size() : 0 %>)
                </button>
                <button type="button" class="report-tab-btn" onclick="switchReportTab('tabShowtimes', this)">
                    <span>⏰</span> Hiệu suất suất chiếu (<%= showtimes != null ? showtimes.size() : 0 %>)
                </button>
            </div>

            <!-- TAB 1: REVENUE BY CENTRE -->
            <div id="tabCentres" class="report-tab-pane active">
                <div class="admin-card">
                    <div class="admin-card-header">
                        <div>
                            <h2>Doanh thu theo trung tâm chiếu phim</h2>
                            <p>Thống kê số lượng đơn đặt vé, vé bán ra và tổng tiền thu được tại từng trung tâm</p>
                        </div>
                    </div>
                    <div class="table-responsive">
                        <table class="cf-table">
                            <thead>
                                <tr>
                                    <th>Trung tâm</th>
                                    <th>Thành phố</th>
                                    <th>Số đơn đặt vé</th>
                                    <th>Tổng vé bán</th>
                                    <th style="text-align: right;">Doanh thu</th>
                                </tr>
                            </thead>
                            <tbody>
                                <% if (centres == null || centres.isEmpty()) { %>
                                    <tr>
                                        <td colspan="5" class="cf-empty-state">Chưa có dữ liệu giao dịch tại các trung tâm.</td>
                                    </tr>
                                <% } else {
                                    for (RevenueReport b : centres) {
                                %>
                                    <tr>
                                        <td><strong><%= b.getName() %></strong></td>
                                        <td><%= b.getCity() == null || b.getCity().isBlank() ? "—" : b.getCity() %></td>
                                        <td><%= String.format("%,d", b.getBookings()) %> đơn</td>
                                        <td><span class="cf-badge cf-badge-info"><%= String.format("%,d", b.getTickets()) %> vé</span></td>
                                        <td style="text-align: right; font-weight: 700; color: #F8FAFC;"><%= money(b.getRevenue()) %></td>
                                    </tr>
                                <% }} %>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

            <!-- TAB 2: REVENUE BY MOVIE -->
            <div id="tabMovies" class="report-tab-pane">
                <div class="admin-card">
                    <div class="admin-card-header">
                        <div>
                            <h2>Xếp hạng doanh thu theo phim</h2>
                            <p>Danh sách phim mang lại doanh thu cao nhất trên toàn bộ hệ thống CineFlow</p>
                        </div>
                    </div>
                    <div class="table-responsive">
                        <table class="cf-table">
                            <thead>
                                <tr>
                                    <th>Xếp hạng</th>
                                    <th>Tên phim</th>
                                    <th>Đơn đặt vé</th>
                                    <th>Vé đã bán</th>
                                    <th style="text-align: right;">Tổng doanh thu</th>
                                </tr>
                            </thead>
                            <tbody>
                                <% if (movies == null || movies.isEmpty()) { %>
                                    <tr>
                                        <td colspan="5" class="cf-empty-state">Chưa có dữ liệu doanh thu phim.</td>
                                    </tr>
                                <% } else {
                                    int rank = 1;
                                    for (RevenueReport m : movies) {
                                %>
                                    <tr>
                                        <td>
                                            <span style="font-weight: 800; color: <%= rank == 1 ? "#e11d48" : rank <= 3 ? "#f59e0b" : "#64748b" %>">
                                                #<%= rank %>
                                            </span>
                                        </td>
                                        <td><strong><%= m.getName() %></strong></td>
                                        <td><%= String.format("%,d", m.getBookings()) %> đơn</td>
                                        <td><span class="cf-badge cf-badge-info"><%= String.format("%,d", m.getTickets()) %> vé</span></td>
                                        <td style="text-align: right; font-weight: 700; color: #F8FAFC;"><%= money(m.getRevenue()) %></td>
                                    </tr>
                                <% rank++; }} %>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

            <!-- TAB 3: SHOWTIME METRICS -->
            <div id="tabShowtimes" class="report-tab-pane">
                <div class="admin-card">
                    <div class="admin-card-header">
                        <div>
                            <h2>Tỷ lệ lấp đầy suất chiếu</h2>
                            <p>Đo lường công suất sử dụng ghế tại từng phòng chiếu theo suất chiếu cụ thể</p>
                        </div>
                    </div>
                    <div class="table-responsive">
                        <table class="cf-table">
                            <thead>
                                <tr>
                                    <th>Phim</th>
                                    <th>Trung tâm</th>
                                    <th>Phòng chiếu</th>
                                    <th>Giờ chiếu</th>
                                    <th>Vé bán / Sức chứa</th>
                                    <th style="text-align: right;">Tỷ lệ lấp đầy</th>
                                </tr>
                            </thead>
                            <tbody>
                                <% if (showtimes == null || showtimes.isEmpty()) { %>
                                    <tr>
                                        <td colspan="6" class="cf-empty-state">Chưa có dữ liệu suất chiếu.</td>
                                    </tr>
                                <% } else {
                                    for (ShowtimeMetric st : showtimes) {
                                        double occ = st.getOccupancy();
                                        String occClass = occ >= 70 ? "cf-badge-success" : occ >= 40 ? "cf-badge-warning" : "cf-badge-muted";
                                %>
                                    <tr>
                                        <td><strong><%= st.getMovieTitle() %></strong></td>
                                        <td><%= st.getCentreName() %></td>
                                        <td><%= st.getHallName() %></td>
                                        <td><%= st.getStartTime() != null ? st.getStartTime().toString().substring(0, 16) : "—" %></td>
                                        <td><%= st.getSoldSeats() %> / <%= st.getCapacity() %> ghế</td>
                                        <td style="text-align: right;">
                                            <span class="cf-badge <%= occClass %>">
                                                <%= String.format("%.1f%%", occ) %>
                                            </span>
                                        </td>
                                    </tr>
                                <% }} %>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </main>
    </div>
</div>

<script>
    function switchReportTab(tabId, btn) {
        document.querySelectorAll('.report-tab-pane').forEach(p => p.classList.remove('active'));
        document.querySelectorAll('.report-tab-btn').forEach(b => b.classList.remove('active'));
        const pane = document.getElementById(tabId);
        if (pane) pane.classList.add('active');
        if (btn) btn.classList.add('active');
    }
</script>
</body>
</html>
