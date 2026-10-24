<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.entity.User" %>
<%
    User currentUser = (User) session.getAttribute("user");
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && currentUser != null) {
        authRole = currentUser.getRole();
    }
    if (authRole == null || "CUSTOMER".equalsIgnoreCase(authRole)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Khách hàng không có quyền truy cập không gian nhân viên.");
        return;
    }

    boolean isSales = currentUser != null && currentUser.isTicketSalesStaff();
    boolean isChecker = currentUser != null && currentUser.isTicketCheckerStaff();
    boolean isGenericStaff = currentUser != null && "STAFF".equalsIgnoreCase(currentUser.getRole());
    boolean isAdminOrManager = "ADMIN".equalsIgnoreCase(authRole) || "MANAGER".equalsIgnoreCase(authRole);

    // Nếu là Admin/Manager hoặc Staff chung thì hiển thị cả 2 phân hệ để tiện kiểm tra
    boolean showSales = isSales || isGenericStaff || isAdminOrManager;
    boolean showChecker = isChecker || isGenericStaff || isAdminOrManager;

    String fullName = currentUser != null && currentUser.getFullName() != null ? currentUser.getFullName() : "Nhân viên";
    String employeeCode = currentUser != null ? currentUser.getEmployeeCode() : "NV00000";
    String centreName = currentUser != null && currentUser.getCentreName() != null ? currentUser.getCentreName() : "Toàn hệ thống CineFlow";
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Không gian nhân viên | CineFlow</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/shell.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/staff/dashboard.css?v=<%= System.currentTimeMillis() %>">
</head>
<body>
    <div class="cf-manager-shell">
        <jsp:include page="/WEB-INF/fragments/managerSidebar.jspf"/>
        <div class="cf-manager-main">
            <jsp:include page="/WEB-INF/fragments/managerTopbar.jspf"/>
            <main class="cf-content">
                <div class="cf-page-header">
                    <div>
                        <h1>Xin chào, <%= fullName %>!</h1>
                    </div>
                </div>

                <!-- BANNER THÔNG TIN NHÂN SỰ -->
                <div class="staff-role-banner">
                    <div class="staff-role-meta">
                        <div>
                            <span style="font-size: 12px; color: #94a3b8; display: block; margin-bottom: 2px;">Mã nhân sự:</span>
                            <code class="cf-badge cf-badge-info" style="font-size: 13px; font-weight: 700;"><%= employeeCode %></code>
                        </div>
                        <div>
                            <span style="font-size: 12px; color: #94a3b8; display: block; margin-bottom: 2px;">Trung tâm trực thuộc:</span>
                            <strong style="color: #f8fafc; font-size: 14px;">🏛 <%= centreName %></strong>
                        </div>
                    </div>
                    <div>
                        <% if (isSales) { %>
                            <span class="staff-role-badge-sales">
                                🎫 Nhân viên Bán vé & F&B (Ticket Sales Staff)
                            </span>
                        <% } else if (isChecker) { %>
                            <span class="staff-role-badge-checker">
                                🔍 Nhân viên Soát vé tại cửa rạp (Ticket Checker Staff)
                            </span>
                        <% } else { %>
                            <span class="staff-role-badge-sales" style="background: rgba(99, 102, 241, 0.18); color: #a5b4fc; border-color: rgba(99, 102, 241, 0.4);">
                                👥 Nhân viên CineFlow (Staff)
                            </span>
                        <% } %>
                    </div>
                </div>

                <!-- ============================================================== -->
                <!-- PHÂN HỆ 1: TICKET SALES STAFF (BÁN VÉ & BẮP NƯỚC TẠI QUẦY) -->
                <!-- ============================================================== -->
                <% if (showSales) { %>
                <div class="cf-card" style="margin-bottom: 24px;">
                    <div class="cf-card-header">
                        <div>
                            <div class="staff-section-title">
                                <span>🎫</span> Quầy vé & F&B (Ticket Sales Staff)
                            </div>
                            <p>Quy trình phục vụ bán vé trực tiếp, chọn ghế sơ đồ rạp, đặt combo bắp nước và in hóa đơn tại quầy.</p>
                        </div>
                        <% if (!isSales && isAdminOrManager) { %>
                            <span class="cf-badge cf-badge-info" style="font-size: 11px;">Chế độ xem Quản lý</span>
                        <% } %>
                    </div>
                    <div class="cf-card-body">
                        <div class="cf-dashboard-links">
                            <a class="cf-dashboard-link cf-dashboard-link-featured" href="${pageContext.request.contextPath}/staff/counter" title="Mở giao diện đặt vé trực tiếp">
                                <span class="cf-feature-tag tag-blue">Tác nghiệp quầy</span>
                                <strong style="color: #38bdf8; font-size: 15px;">🎟 Bán vé tại quầy (Counter Booking)</strong>
                                <span>Tra cứu suất chiếu đang mở bán trong ngày, chọn ghế ngồi trực tiếp theo sơ đồ rạp cho khách hàng.</span>
                            </a>

                            <div class="cf-dashboard-link" style="opacity: 0.95;">
                                <span class="cf-feature-tag tag-amber">F&B Concessions</span>
                                <strong style="color: #fbbf24; font-size: 15px;">🍿 Đặt Bắp nước & Combo F&B</strong>
                                <span>Danh mục combo bắp rang bơ (Caramel, Phô mai), nước giải khát và snack ưu đãi kèm vé xem phim.</span>
                            </div>

                            <div class="cf-dashboard-link" style="opacity: 0.95;">
                                <span class="cf-feature-tag tag-emerald">Thu ngân</span>
                                <strong style="color: #34d399; font-size: 15px;">💳 Thanh toán & Xuất hóa đơn</strong>
                                <span>Tiếp nhận thanh toán tiền mặt, thẻ POS hoặc quét QR chuyển khoản. In vé xem phim và hóa đơn cho khách.</span>
                            </div>

                            <a class="cf-dashboard-link" href="${pageContext.request.contextPath}/staff/counter">
                                <span class="cf-feature-tag tag-blue">Tra cứu</span>
                                <strong>🕒 Lịch chiếu & Ghế trống</strong>
                                <span>Kiểm tra danh sách suất chiếu còn vé, số lượng ghế trống theo từng phòng chiếu tại <%= centreName %>.</span>
                            </a>

                            <a class="cf-dashboard-link" href="${pageContext.request.contextPath}/account">
                                <span class="cf-feature-tag tag-emerald">Lịch sử</span>
                                <strong>📋 Lịch sử ca bán vé</strong>
                                <span>Xem các đơn hàng và vé đã xuất trong ca làm việc để thực hiện đối soát cuối ngày.</span>
                            </a>

                            <a class="cf-dashboard-link" href="${pageContext.request.contextPath}/account">
                                <span class="cf-feature-tag tag-amber">Cá nhân</span>
                                <strong>👤 Thông tin nhân viên</strong>
                                <span>Hồ sơ tài khoản <%= employeeCode %>, đổi mật khẩu và thông tin liên hệ.</span>
                            </a>
                        </div>
                    </div>
                </div>
                <% } %>

                <!-- ============================================================== -->
                <!-- PHÂN HỆ 2: TICKET CHECKER STAFF (SOÁT VÉ CỬA RẠP & CHECK-IN) -->
                <!-- ============================================================== -->
                <% if (showChecker) { %>
                <div class="cf-card" style="margin-bottom: 24px;">
                    <div class="cf-card-header">
                        <div>
                            <div class="staff-section-title">
                                <span>🔍</span> Cửa phòng chiếu & Soát vé (Ticket Checker Staff)
                            </div>
                            <p>Quy trình kiểm tra mã vé QR/Code, chọn suất chiếu đón khách và quản lý danh sách khán giả đã check-in.</p>
                        </div>
                        <% if (!isChecker && isAdminOrManager) { %>
                            <span class="cf-badge cf-badge-info" style="font-size: 11px;">Chế độ xem Quản lý</span>
                        <% } %>
                    </div>
                    <div class="cf-card-body">
                        <div class="cf-dashboard-links">
                            <div class="cf-dashboard-link cf-dashboard-link-purple">
                                <span class="cf-feature-tag tag-purple">Cửa kiểm soát</span>
                                <strong style="color: #c084fc; font-size: 15px;">📱 Quét mã QR & Soát vé</strong>
                                <span>Quét mã QR từ điện thoại của khán giả hoặc nhập mã vé để xác thực tính hợp lệ vào phòng chiếu.</span>
                            </div>

                            <div class="cf-dashboard-link" style="opacity: 0.95;">
                                <span class="cf-feature-tag tag-blue">Suất chiếu</span>
                                <strong style="color: #38bdf8; font-size: 15px;">🎬 Chọn suất chiếu đón khách</strong>
                                <span>Kích hoạt cổng soát vé cho suất chiếu chuẩn bị bắt đầu (mở đón khách 15-30 phút trước giờ chiếu).</span>
                            </div>

                            <div class="cf-dashboard-link" style="opacity: 0.95;">
                                <span class="cf-feature-tag tag-emerald">Thời gian thực</span>
                                <strong style="color: #34d399; font-size: 15px;">📋 Danh sách đã Check-in</strong>
                                <span>Theo dõi số lượng khách đã qua cửa soát vé, số vé chưa check-in và sơ đồ ghế khán giả trong phòng.</span>
                            </div>

                            <div class="cf-dashboard-link" style="opacity: 0.95;">
                                <span class="cf-feature-tag tag-amber">Giám sát</span>
                                <strong style="color: #fbbf24; font-size: 15px;">🏛 Trạng thái phòng chiếu</strong>
                                <span>Kiểm tra trạng thái phòng chiếu tại <%= centreName %> (Đón khách, Đang chiếu, Kết thúc, Dọn vệ sinh).</span>
                            </div>

                            <a class="cf-dashboard-link" href="${pageContext.request.contextPath}/checkin">
                                <span class="cf-feature-tag tag-blue">Lịch trình</span>
                                <strong>🕒 Lịch suất chiếu hôm nay</strong>
                                <span>Xem toàn bộ lịch chiếu các phòng trong ngày để sắp xếp ca trực cửa kiểm soát vé.</span>
                            </a>

                            <a class="cf-dashboard-link" href="${pageContext.request.contextPath}/account">
                                <span class="cf-feature-tag tag-purple">Cá nhân</span>
                                <strong>👤 Hồ sơ kiểm soát viên</strong>
                                <span>Thông tin tài khoản <%= employeeCode %>, đổi mật khẩu và ca trực được phân công.</span>
                            </a>
                        </div>
                    </div>
                </div>
                <% } %>

            </main>
        </div>
    </div>
</body>
</html>
