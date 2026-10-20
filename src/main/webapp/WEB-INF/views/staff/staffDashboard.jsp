<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.entity.User" %>
<%
    User currentUser = (User) session.getAttribute("user");
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && currentUser != null) {
        authRole = currentUser.getRole();
    }
    if (authRole == null || "CUSTOMER".equalsIgnoreCase(authRole)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Khách hàng không có quy?n truy c?p không gian nhân viên.");
        return;
    }

    boolean isSales = currentUser != null && currentUser.isTicketSalesStaff();
    boolean isChecker = currentUser != null && currentUser.isTicketCheckerStaff();
    boolean isGenericStaff = currentUser != null && "STAFF".equalsIgnoreCase(currentUser.getRole());
    boolean isAdminOrManager = "ADMIN".equalsIgnoreCase(authRole) || "MANAGER".equalsIgnoreCase(authRole);

    // N?u là Admin/Manager ho?c Staff chung thì hi?n th? c? 2 phân h? d? ti?n ki?m tra
    boolean showSales = isSales || isGenericStaff || isAdminOrManager;
    boolean showChecker = isChecker || isGenericStaff || isAdminOrManager;

    String fullName = currentUser != null && currentUser.getFullName() != null ? currentUser.getFullName() : "Nhân viên";
    String employeeCode = currentUser != null ? currentUser.getEmployeeCode() : "NV00000";
    String centreName = currentUser != null && currentUser.getCentreName() != null ? currentUser.getCentreName() : "Toàn h? th?ng CineFlow";
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

                <!-- BANNER THÔNG TIN NHÂN S? -->
                <div class="staff-role-banner">
                    <div class="staff-role-meta">
                        <div>
                            <span style="font-size: 12px; color: #94a3b8; display: block; margin-bottom: 2px;">Mã nhân s?:</span>
                            <code class="cf-badge cf-badge-info" style="font-size: 13px; font-weight: 700;"><%= employeeCode %></code>
                        </div>
                        <div>
                            <span style="font-size: 12px; color: #94a3b8; display: block; margin-bottom: 2px;">Trung tâm tr?c thu?c:</span>
                            <strong style="color: #f8fafc; font-size: 14px;">?? <%= centreName %></strong>
                        </div>
                    </div>
                    <div>
                        <% if (isSales) { %>
                            <span class="staff-role-badge-sales">
                                ?? Nhân viên Bán vé & F&B (Ticket Sales Staff)
                            </span>
                        <% } else if (isChecker) { %>
                            <span class="staff-role-badge-checker">
                                ?? Nhân viên Soát vé t?i c?a r?p (Ticket Checker Staff)
                            </span>
                        <% } else { %>
                            <span class="staff-role-badge-sales" style="background: rgba(99, 102, 241, 0.18); color: #a5b4fc; border-color: rgba(99, 102, 241, 0.4);">
                                ?? Nhân viên CineFlow (Staff)
                            </span>
                        <% } %>
                    </div>
                </div>

                <!-- ============================================================== -->
                <!-- PHÂN H? 1: TICKET SALES STAFF (BÁN VÉ & B?P NU?C T?I QU?Y) -->
                <!-- ============================================================== -->
                <% if (showSales) { %>
                <div class="cf-card" style="margin-bottom: 24px;">
                    <div class="cf-card-header">
                        <div>
                            <div class="staff-section-title">
                                <span>??</span> Qu?y vé & F&B (Ticket Sales Staff)
                            </div>
                            <p>Quy trình ph?c v? bán vé tr?c ti?p, ch?n gh? so d? r?p, d?t combo b?p nu?c và in hóa don t?i qu?y.</p>
                        </div>
                        <% if (!isSales && isAdminOrManager) { %>
                            <span class="cf-badge cf-badge-info" style="font-size: 11px;">Ch? d? xem Qu?n lý</span>
                        <% } %>
                    </div>
                    <div class="cf-card-body">
                        <div class="cf-dashboard-links">
                            <a class="cf-dashboard-link cf-dashboard-link-featured" href="${pageContext.request.contextPath}/home" title="M? giao di?n d?t vé tr?c ti?p">
                                <span class="cf-feature-tag tag-blue">Tác nghi?p qu?y</span>
                                <strong style="color: #38bdf8; font-size: 15px;">?? Bán vé t?i qu?y (Counter Booking)</strong>
                                <span>Tra c?u su?t chi?u dang m? bán trong ngày, ch?n gh? ng?i tr?c ti?p theo so d? r?p cho khách hàng.</span>
                            </a>

                            <div class="cf-dashboard-link" style="opacity: 0.95;">
                                <span class="cf-feature-tag tag-amber">F&B Concessions</span>
                                <strong style="color: #fbbf24; font-size: 15px;">?? Ð?t B?p nu?c & Combo F&B</strong>
                                <span>Danh m?c combo b?p rang bo (Caramel, Phô mai), nu?c gi?i khát và snack uu dãi kèm vé xem phim.</span>
                            </div>

                            <div class="cf-dashboard-link" style="opacity: 0.95;">
                                <span class="cf-feature-tag tag-emerald">Thu ngân</span>
                                <strong style="color: #34d399; font-size: 15px;">?? Thanh toán & Xu?t hóa don</strong>
                                <span>Ti?p nh?n thanh toán ti?n m?t, th? POS ho?c quét QR chuy?n kho?n. In vé xem phim và hóa don cho khách.</span>
                            </div>

                            <a class="cf-dashboard-link" href="${pageContext.request.contextPath}/home">
                                <span class="cf-feature-tag tag-blue">Tra c?u</span>
                                <strong>?? L?ch chi?u & Gh? tr?ng</strong>
                                <span>Ki?m tra danh sách su?t chi?u còn vé, s? lu?ng gh? tr?ng theo t?ng phòng chi?u t?i <%= centreName %>.</span>
                            </a>

                            <a class="cf-dashboard-link" href="${pageContext.request.contextPath}/account">
                                <span class="cf-feature-tag tag-emerald">L?ch s?</span>
                                <strong>?? L?ch s? ca bán vé</strong>
                                <span>Xem các don hàng và vé dã xu?t trong ca làm vi?c d? th?c hi?n d?i soát cu?i ngày.</span>
                            </a>

                            <a class="cf-dashboard-link" href="${pageContext.request.contextPath}/account">
                                <span class="cf-feature-tag tag-amber">Cá nhân</span>
                                <strong>?? Thông tin nhân viên</strong>
                                <span>H? so tài kho?n <%= employeeCode %>, d?i m?t kh?u và thông tin liên h?.</span>
                            </a>
                        </div>
                    </div>
                </div>
                <% } %>

                <!-- ============================================================== -->
                <!-- PHÂN H? 2: TICKET CHECKER STAFF (SOÁT VÉ C?A R?P & CHECK-IN) -->
                <!-- ============================================================== -->
                <% if (showChecker) { %>
                <div class="cf-card" style="margin-bottom: 24px;">
                    <div class="cf-card-header">
                        <div>
                            <div class="staff-section-title">
                                <span>??</span> C?a phòng chi?u & Soát vé (Ticket Checker Staff)
                            </div>
                            <p>Quy trình ki?m tra mã vé QR/Code, ch?n su?t chi?u dón khách và qu?n lý danh sách khán gi? dã check-in.</p>
                        </div>
                        <% if (!isChecker && isAdminOrManager) { %>
                            <span class="cf-badge cf-badge-info" style="font-size: 11px;">Ch? d? xem Qu?n lý</span>
                        <% } %>
                    </div>
                    <div class="cf-card-body">
                        <div class="cf-dashboard-links">
                            <div class="cf-dashboard-link cf-dashboard-link-purple">
                                <span class="cf-feature-tag tag-purple">C?a ki?m soát</span>
                                <strong style="color: #c084fc; font-size: 15px;">?? Quét mã QR & Soát vé</strong>
                                <span>Quét mã QR t? di?n tho?i c?a khán gi? ho?c nh?p mã vé d? xác th?c tính h?p l? vào phòng chi?u.</span>
                            </div>

                            <div class="cf-dashboard-link" style="opacity: 0.95;">
                                <span class="cf-feature-tag tag-blue">Su?t chi?u</span>
                                <strong style="color: #38bdf8; font-size: 15px;">?? Ch?n su?t chi?u dón khách</strong>
                                <span>Kích ho?t c?ng soát vé cho su?t chi?u chu?n b? b?t d?u (m? dón khách 15-30 phút tru?c gi? chi?u).</span>
                            </div>

                            <div class="cf-dashboard-link" style="opacity: 0.95;">
                                <span class="cf-feature-tag tag-emerald">Th?i gian th?c</span>
                                <strong style="color: #34d399; font-size: 15px;">?? Danh sách dã Check-in</strong>
                                <span>Theo dõi s? lu?ng khách dã qua c?a soát vé, s? vé chua check-in và so d? gh? khán gi? trong phòng.</span>
                            </div>

                            <div class="cf-dashboard-link" style="opacity: 0.95;">
                                <span class="cf-feature-tag tag-amber">Giám sát</span>
                                <strong style="color: #fbbf24; font-size: 15px;">?? Tr?ng thái phòng chi?u</strong>
                                <span>Ki?m tra tr?ng thái phòng chi?u t?i <%= centreName %> (Ðón khách, Ðang chi?u, K?t thúc, D?n v? sinh).</span>
                            </div>

                            <a class="cf-dashboard-link" href="${pageContext.request.contextPath}/home">
                                <span class="cf-feature-tag tag-blue">L?ch trình</span>
                                <strong>?? L?ch su?t chi?u hôm nay</strong>
                                <span>Xem toàn b? l?ch chi?u các phòng trong ngày d? s?p x?p ca tr?c c?a ki?m soát vé.</span>
                            </a>

                            <a class="cf-dashboard-link" href="${pageContext.request.contextPath}/account">
                                <span class="cf-feature-tag tag-purple">Cá nhân</span>
                                <strong>?? H? so ki?m soát viên</strong>
                                <span>Thông tin tài kho?n <%= employeeCode %>, d?i m?t kh?u và ca tr?c du?c phân công.</span>
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
