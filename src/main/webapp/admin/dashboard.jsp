<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.ReportSummary,model.RevenueReport,model.Movie,model.Centre,model.User,java.util.List,java.math.BigDecimal" %>
<%
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && session.getAttribute("user") instanceof model.User) {
        authRole = ((model.User) session.getAttribute("user")).getRole();
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
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/admin-style.css?v=<%= System.currentTimeMillis() %>">
    <style>
        .dashboard-welcome-banner {
            background: linear-gradient(135deg, #0f172a 0%, #1e1b4b 60%, #31102f 100%);
            border-radius: var(--cf-radius-lg);
            padding: 32px 36px;
            color: #ffffff;
            margin-bottom: 28px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            position: relative;
            overflow: hidden;
            box-shadow: 0 10px 25px -5px rgba(15, 23, 42, 0.3);
            border: 1px solid #334155;
        }
        .dashboard-welcome-banner::after {
            content: '';
            position: absolute;
            right: -60px;
            bottom: -60px;
            width: 260px;
            height: 260px;
            border-radius: 50%;
            background: radial-gradient(circle, rgba(225, 29, 72, 0.25) 0%, transparent 70%);
            pointer-events: none;
        }
        .banner-content {
            max-width: 650px;
            z-index: 1;
        }
        .banner-badge {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            background: rgba(225, 29, 72, 0.2);
            border: 1px solid rgba(225, 29, 72, 0.4);
            color: #fca5a5;
            padding: 4px 12px;
            border-radius: var(--cf-radius-pill);
            font-size: 11.5px;
            font-weight: 700;
            letter-spacing: 0.08em;
            margin-bottom: 12px;
        }
        .banner-content h1 {
            font-size: 28px;
            font-weight: 800;
            margin: 0 0 10px;
            letter-spacing: -0.03em;
            line-height: 1.2;
        }
        .banner-content p {
            margin: 0 0 20px;
            color: #cbd5e1;
            font-size: 14px;
            line-height: 1.6;
        }
        .banner-actions {
            display: flex;
            gap: 12px;
            flex-wrap: wrap;
        }
        .banner-actions .cf-btn-primary {
            background: var(--cf-primary);
            color: #fff;
            box-shadow: 0 4px 14px rgba(225, 29, 72, 0.4);
        }
        .banner-actions .cf-btn-ghost {
            background: rgba(255, 255, 255, 0.1);
            color: #ffffff;
            border: 1px solid rgba(255, 255, 255, 0.2);
        }
        .banner-actions .cf-btn-ghost:hover {
            background: rgba(255, 255, 255, 0.2);
        }
        .banner-stat-bubble {
            background: rgba(255, 255, 255, 0.05);
            border: 1px solid rgba(255, 255, 255, 0.12);
            padding: 20px 24px;
            border-radius: var(--cf-radius);
            backdrop-filter: blur(6px);
            text-align: right;
            z-index: 1;
        }
        .banner-stat-bubble small {
            display: block;
            color: #94a3b8;
            font-size: 11px;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 0.08em;
            margin-bottom: 4px;
        }
        .banner-stat-bubble strong {
            font-size: 26px;
            color: #ffffff;
            font-weight: 800;
        }
        .dashboard-split-grid {
            display: grid;
            grid-template-columns: 2fr 1fr;
            gap: 24px;
            margin-bottom: 24px;
        }
        .quick-nav-grid {
            display: grid;
            grid-template-columns: repeat(4, 1fr);
            gap: 16px;
            margin-bottom: 28px;
        }
        .quick-nav-card {
            background: #ffffff;
            border: 1px solid #E2E8F0;
            border-radius: var(--cf-radius, 10px);
            padding: 18px;
            display: flex;
            align-items: center;
            gap: 14px;
            text-decoration: none;
            color: #111827;
            box-shadow: 0 4px 14px rgba(0, 0, 0, 0.05);
            transition: var(--cf-transition);
        }
        .quick-nav-card:hover {
            border-color: #CBD5E1;
            transform: translateY(-2px);
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
        }
        .quick-nav-icon {
            width: 44px;
            height: 44px;
            border-radius: 10px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 20px;
            flex-shrink: 0;
        }
        .quick-nav-meta h3 {
            margin: 0 0 3px;
            font-size: 14px;
            font-weight: 700;
            color: #111827;
        }
        .quick-nav-meta p {
            margin: 0;
            font-size: 11.5px;
            color: #64748B;
        }
        .metric-progress-item {
            margin-bottom: 18px;
        }
        .metric-progress-header {
            display: flex;
            justify-content: space-between;
            font-size: 12.5px;
            margin-bottom: 6px;
        }
        .metric-progress-header span {
            color: var(--cf-text-secondary);
        }
        .metric-progress-header strong {
            color: var(--cf-text);
        }
        .progress-bar-bg {
            height: 8px;
            background: var(--cf-surface-secondary);
            border-radius: 4px;
            overflow: hidden;
        }
        .progress-bar-fill {
            height: 100%;
            border-radius: 4px;
            transition: width 0.6s ease;
        }
        @media (max-width: 1024px) {
            .dashboard-split-grid {
                grid-template-columns: 1fr;
            }
            .quick-nav-grid {
                grid-template-columns: repeat(2, 1fr);
            }
        }
        @media (max-width: 768px) {
            .dashboard-welcome-banner {
                flex-direction: column;
                align-items: flex-start;
                gap: 20px;
                padding: 24px;
            }
            .banner-stat-bubble {
                width: 100%;
                text-align: left;
            }
            .quick-nav-grid {
                grid-template-columns: 1fr;
            }
        }
    </style>
</head>
<body>
<div class="admin-layout">
    <jsp:include page="/admin/sidebar.jsp" />

    <div class="admin-main">
        <jsp:include page="/admin/topbar.jsp" />

        <main class="admin-content">
            <!-- BANNER CHÀO MỪNG QUẢN TRỊ VIÊN -->
            <div class="dashboard-welcome-banner">
                <div class="banner-content">
                    <div class="banner-badge">TRUNG TÂM ĐIỀU HÀNH TOÀN CHUỖI</div>
                    <h1>Xin chào, <%= adminName %>!</h1>
                    <p>Hệ thống đang hoạt động tối ưu. Toàn bộ thông tin suất chiếu, doanh thu các cụm rạp và nhân sự được đồng bộ tự động theo thời gian thực.</p>
                    <div class="banner-actions">
                        <a href="${pageContext.request.contextPath}/admin/movies" class="cf-btn cf-btn-primary">Quản lý phim</a>
                        <a href="${pageContext.request.contextPath}/admin/customers" class="cf-btn cf-btn-ghost">Khách hàng</a>
                        <a href="${pageContext.request.contextPath}/reports" class="cf-btn cf-btn-ghost">Báo cáo doanh thu</a>
                        <a href="${pageContext.request.contextPath}/admin/managers" class="cf-btn cf-btn-ghost">Quản lý nhân sự</a>
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

                <a href="${pageContext.request.contextPath}/staff" class="quick-nav-card">
                    <div class="quick-nav-icon" style="background: #E0F2FE; color: #0284c7;">♙</div>
                    <div class="quick-nav-meta">
                        <h3>Nhân sự & Chi nhánh</h3>
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
                                <strong><%= activeCentresCount %> trung tâm (100%)</strong>
                            </div>
                            <div class="progress-bar-bg">
                                <div class="progress-bar-fill" style="width: 100%; background: #10b981;"></div>
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
