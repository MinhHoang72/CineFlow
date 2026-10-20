<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.entity.User" %>
<%
    User currentUser = (User) session.getAttribute("user");
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && currentUser != null) {
        authRole = currentUser.getRole();
    }
    if (authRole == null || "CUSTOMER".equalsIgnoreCase(authRole)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Kh�ch h�ng kh�ng c� quy?n truy c?p kh�ng gian nh�n vi�n.");
        return;
    }

    boolean isSales = currentUser != null && currentUser.isTicketSalesStaff();
    boolean isChecker = currentUser != null && currentUser.isTicketCheckerStaff();
    boolean isGenericStaff = currentUser != null && "STAFF".equalsIgnoreCase(currentUser.getRole());
    boolean isAdminOrManager = "ADMIN".equalsIgnoreCase(authRole) || "MANAGER".equalsIgnoreCase(authRole);

    // N?u l� Admin/Manager ho?c Staff chung th� hi?n th? c? 2 ph�n h? d? ti?n ki?m tra
    boolean showSales = isSales || isGenericStaff || isAdminOrManager;
    boolean showChecker = isChecker || isGenericStaff || isAdminOrManager;

    String fullName = currentUser != null && currentUser.getFullName() != null ? currentUser.getFullName() : "Nh�n vi�n";
    String employeeCode = currentUser != null ? currentUser.getEmployeeCode() : "NV00000";
    String centreName = currentUser != null && currentUser.getCentreName() != null ? currentUser.getCentreName() : "To�n h? th?ng CineFlow";
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Kh�ng gian nh�n vi�n | CineFlow</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/shell.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/staff/dashboard.css?v=<%= System.currentTimeMillis() %>">
</head>
<body>
    <div class="cf-manager-shell">
        <jsp:include page="/WEB-INF/fragments/managerSidebar.jsp"/>
        <div class="cf-manager-main">
            <jsp:include page="/WEB-INF/fragments/managerTopbar.jsp"/>
            <main class="cf-content">
                <div class="cf-page-header">
                    <div>
                        <h1>Xin ch�o, <%= fullName %>!</h1>
                    </div>
                </div>

                <!-- BANNER TH�NG TIN NH�N S? -->
                <div class="staff-role-banner">
                    <div class="staff-role-meta">
                        <div>
                            <span style="font-size: 12px; color: #94a3b8; display: block; margin-bottom: 2px;">M� nh�n s?:</span>
                            <code class="cf-badge cf-badge-info" style="font-size: 13px; font-weight: 700;"><%= employeeCode %></code>
                        </div>
                        <div>
                            <span style="font-size: 12px; color: #94a3b8; display: block; margin-bottom: 2px;">Trung t�m tr?c thu?c:</span>
                            <strong style="color: #f8fafc; font-size: 14px;">?? <%= centreName %></strong>
                        </div>
                    </div>
                    <div>
                        <% if (isSales) { %>
                            <span class="staff-role-badge-sales">
                                ?? Nh�n vi�n B�n v� & F&B (Ticket Sales Staff)
                            </span>
                        <% } else if (isChecker) { %>
                            <span class="staff-role-badge-checker">
                                ?? Nh�n vi�n So�t v� t?i c?a r?p (Ticket Checker Staff)
                            </span>
                        <% } else { %>
                            <span class="staff-role-badge-sales" style="background: rgba(99, 102, 241, 0.18); color: #a5b4fc; border-color: rgba(99, 102, 241, 0.4);">
                                ?? Nh�n vi�n CineFlow (Staff)
                            </span>
                        <% } %>
                    </div>
                </div>

                <!-- ============================================================== -->
                <!-- PH�N H? 1: TICKET SALES STAFF (B�N V� & B?P NU?C T?I QU?Y) -->
                <!-- ============================================================== -->
                <% if (showSales) { %>
                <div class="cf-card" style="margin-bottom: 24px;">
                    <div class="cf-card-header">
                        <div>
                            <div class="staff-section-title">
                                <span>??</span> Qu?y v� & F&B (Ticket Sales Staff)
                            </div>
                            <p>Quy tr�nh ph?c v? b�n v� tr?c ti?p, ch?n gh? so d? r?p, d?t combo b?p nu?c v� in h�a don t?i qu?y.</p>
                        </div>
                        <% if (!isSales && isAdminOrManager) { %>
                            <span class="cf-badge cf-badge-info" style="font-size: 11px;">Ch? d? xem Qu?n l�</span>
                        <% } %>
                    </div>
                    <div class="cf-card-body">
                        <div class="cf-dashboard-links">
                            <a class="cf-dashboard-link cf-dashboard-link-featured" href="${pageContext.request.contextPath}/home" title="M? giao di?n d?t v� tr?c ti?p">
                                <span class="cf-feature-tag tag-blue">T�c nghi?p qu?y</span>
                                <strong style="color: #38bdf8; font-size: 15px;">?? B�n v� t?i qu?y (Counter Booking)</strong>
                                <span>Tra c?u su?t chi?u dang m? b�n trong ng�y, ch?n gh? ng?i tr?c ti?p theo so d? r?p cho kh�ch h�ng.</span>
                            </a>

                            <div class="cf-dashboard-link" style="opacity: 0.95;">
                                <span class="cf-feature-tag tag-amber">F&B Concessions</span>
                                <strong style="color: #fbbf24; font-size: 15px;">?? �?t B?p nu?c & Combo F&B</strong>
                                <span>Danh m?c combo b?p rang bo (Caramel, Ph� mai), nu?c gi?i kh�t v� snack uu d�i k�m v� xem phim.</span>
                            </div>

                            <div class="cf-dashboard-link" style="opacity: 0.95;">
                                <span class="cf-feature-tag tag-emerald">Thu ng�n</span>
                                <strong style="color: #34d399; font-size: 15px;">?? Thanh to�n & Xu?t h�a don</strong>
                                <span>Ti?p nh?n thanh to�n ti?n m?t, th? POS ho?c qu�t QR chuy?n kho?n. In v� xem phim v� h�a don cho kh�ch.</span>
                            </div>

                            <a class="cf-dashboard-link" href="${pageContext.request.contextPath}/home">
                                <span class="cf-feature-tag tag-blue">Tra c?u</span>
                                <strong>?? L?ch chi?u & Gh? tr?ng</strong>
                                <span>Ki?m tra danh s�ch su?t chi?u c�n v�, s? lu?ng gh? tr?ng theo t?ng ph�ng chi?u t?i <%= centreName %>.</span>
                            </a>

                            <a class="cf-dashboard-link" href="${pageContext.request.contextPath}/account">
                                <span class="cf-feature-tag tag-emerald">L?ch s?</span>
                                <strong>?? L?ch s? ca b�n v�</strong>
                                <span>Xem c�c don h�ng v� v� d� xu?t trong ca l�m vi?c d? th?c hi?n d?i so�t cu?i ng�y.</span>
                            </a>

                            <a class="cf-dashboard-link" href="${pageContext.request.contextPath}/account">
                                <span class="cf-feature-tag tag-amber">C� nh�n</span>
                                <strong>?? Th�ng tin nh�n vi�n</strong>
                                <span>H? so t�i kho?n <%= employeeCode %>, d?i m?t kh?u v� th�ng tin li�n h?.</span>
                            </a>
                        </div>
                    </div>
                </div>
                <% } %>

                <!-- ============================================================== -->
                <!-- PH�N H? 2: TICKET CHECKER STAFF (SO�T V� C?A R?P & CHECK-IN) -->
                <!-- ============================================================== -->
                <% if (showChecker) { %>
                <div class="cf-card" style="margin-bottom: 24px;">
                    <div class="cf-card-header">
                        <div>
                            <div class="staff-section-title">
                                <span>??</span> C?a ph�ng chi?u & So�t v� (Ticket Checker Staff)
                            </div>
                            <p>Quy tr�nh ki?m tra m� v� QR/Code, ch?n su?t chi?u d�n kh�ch v� qu?n l� danh s�ch kh�n gi? d� check-in.</p>
                        </div>
                        <% if (!isChecker && isAdminOrManager) { %>
                            <span class="cf-badge cf-badge-info" style="font-size: 11px;">Ch? d? xem Qu?n l�</span>
                        <% } %>
                    </div>
                    <div class="cf-card-body">
                        <div class="cf-dashboard-links">
                            <div class="cf-dashboard-link cf-dashboard-link-purple">
                                <span class="cf-feature-tag tag-purple">C?a ki?m so�t</span>
                                <strong style="color: #c084fc; font-size: 15px;">?? Qu�t m� QR & So�t v�</strong>
                                <span>Qu�t m� QR t? di?n tho?i c?a kh�n gi? ho?c nh?p m� v� d? x�c th?c t�nh h?p l? v�o ph�ng chi?u.</span>
                            </div>

                            <div class="cf-dashboard-link" style="opacity: 0.95;">
                                <span class="cf-feature-tag tag-blue">Su?t chi?u</span>
                                <strong style="color: #38bdf8; font-size: 15px;">?? Ch?n su?t chi?u d�n kh�ch</strong>
                                <span>K�ch ho?t c?ng so�t v� cho su?t chi?u chu?n b? b?t d?u (m? d�n kh�ch 15-30 ph�t tru?c gi? chi?u).</span>
                            </div>

                            <div class="cf-dashboard-link" style="opacity: 0.95;">
                                <span class="cf-feature-tag tag-emerald">Th?i gian th?c</span>
                                <strong style="color: #34d399; font-size: 15px;">?? Danh s�ch d� Check-in</strong>
                                <span>Theo d�i s? lu?ng kh�ch d� qua c?a so�t v�, s? v� chua check-in v� so d? gh? kh�n gi? trong ph�ng.</span>
                            </div>

                            <div class="cf-dashboard-link" style="opacity: 0.95;">
                                <span class="cf-feature-tag tag-amber">Gi�m s�t</span>
                                <strong style="color: #fbbf24; font-size: 15px;">?? Tr?ng th�i ph�ng chi?u</strong>
                                <span>Ki?m tra tr?ng th�i ph�ng chi?u t?i <%= centreName %> (��n kh�ch, �ang chi?u, K?t th�c, D?n v? sinh).</span>
                            </div>

                            <a class="cf-dashboard-link" href="${pageContext.request.contextPath}/home">
                                <span class="cf-feature-tag tag-blue">L?ch tr�nh</span>
                                <strong>?? L?ch su?t chi?u h�m nay</strong>
                                <span>Xem to�n b? l?ch chi?u c�c ph�ng trong ng�y d? s?p x?p ca tr?c c?a ki?m so�t v�.</span>
                            </a>

                            <a class="cf-dashboard-link" href="${pageContext.request.contextPath}/account">
                                <span class="cf-feature-tag tag-purple">C� nh�n</span>
                                <strong>?? H? so ki?m so�t vi�n</strong>
                                <span>Th�ng tin t�i kho?n <%= employeeCode %>, d?i m?t kh?u v� ca tr?c du?c ph�n c�ng.</span>
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
