<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.entity.User,model.entity.Centre,model.entity.StaffTransfer,model.entity.ManagerAssignment,java.util.List,java.text.SimpleDateFormat" %>
<%
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && session.getAttribute("user") instanceof model.entity.User) {
        authRole = ((model.entity.User) session.getAttribute("user")).getRole();
    }
    if (!"ADMIN".equalsIgnoreCase(authRole)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Quản trị viên (ADMIN) mới có quyền truy cập trang này.");
        return;
    }

    List<Centre> centresWithManager = (List<Centre>) request.getAttribute("centresWithManager");
    List<Centre> activeCentres = (List<Centre>) request.getAttribute("activeCentres");
    List<User> managers = (List<User>) request.getAttribute("managers");
    List<ManagerAssignment> managerAssignments = (List<ManagerAssignment>) request.getAttribute("managerAssignments");

    List<User> staffList = (List<User>) request.getAttribute("staffList");
    List<StaffTransfer> transferHistory = (List<StaffTransfer>) request.getAttribute("transferHistory");
    Integer selectedCentreId = (Integer) request.getAttribute("selectedCentreId");

    request.setAttribute("activeNav", "transfers");
    request.setAttribute("pageTitle", "Bổ nhiệm & Điều chuyển");

    String successParam = request.getParameter("success");
    String errorParam = request.getParameter("error");
    String activeTabParam = request.getParameter("tab");
    if (activeTabParam == null) activeTabParam = "manager";

    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy");
    String todayStr = new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
    String roleFilter = (String) request.getAttribute("roleFilter");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Bổ nhiệm & Điều chuyển Nhân sự | CineFlow Admin</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/reset.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/variables.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/buttons.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/badges.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/forms.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/tables.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/shell.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/dashboard.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/transfers.css?v=<%= System.currentTimeMillis() %>">
</head>
<body>
<div class="cf-manager-shell">
    <jsp:include page="/WEB-INF/views/admin/adminSidebar.jspf" />

    <div class="cf-manager-main">
        <jsp:include page="/WEB-INF/views/admin/adminTopbar.jspf" />

        <main class="cf-content">
            <div class="cf-page-header" style="margin-bottom: 24px;">
                <div style="display: flex; align-items: center; gap: 14px;">
                    <div style="width: 44px; height: 44px; background: rgba(255, 40, 85, 0.12); border: 1px solid rgba(255, 40, 85, 0.25); border-radius: 10px; display: grid; place-items: center; font-size: 22px;">👥</div>
                    <div>
                        <h1 style="margin: 0; font-size: 22px; font-weight: 700; color: #ffffff;">Bổ nhiệm & Điều chuyển nhân sự</h1>
                        <p style="margin: 4px 0 0; color: var(--cf-muted, #94a3b8); font-size: 13px;">Quản lý bổ nhiệm Quản lý trung tâm, điều chuyển nhân sự chi nhánh và phê chuẩn thăng chức toàn hệ thống CineFlow.</p>
                    </div>
                </div>
            </div>

            <!-- CHỈ SỐ KPI TỔNG QUAN NHÂN SỰ -->
            <div class="cf-kpi-grid-4">
                <div class="cf-kpi-card">
                    <span class="cf-kpi-label">Tổng cụm rạp</span>
                    <div class="cf-kpi-value"><%= request.getAttribute("kpiTotalCentres") != null ? request.getAttribute("kpiTotalCentres") : 0 %></div>
                    <span class="cf-kpi-sub">Quy mô mạng lưới CineFlow</span>
                </div>
                <div class="cf-kpi-card">
                    <span class="cf-kpi-label" style="color: #10b981;">Có Quản lý hoạt động</span>
                    <div class="cf-kpi-value" style="color: #10b981;"><%= request.getAttribute("kpiAssignedCentres") != null ? request.getAttribute("kpiAssignedCentres") : 0 %></div>
                    <span class="cf-kpi-sub">Cụm rạp có Quản lý đương nhiệm (ACTIVE)</span>
                </div>
                <div class="cf-kpi-card" style="<%= (request.getAttribute("kpiUnassignedCentres") != null && (int)request.getAttribute("kpiUnassignedCentres") > 0) ? "border-color: rgba(245, 158, 11, 0.4); background: rgba(245, 158, 11, 0.06);" : "" %>">
                    <span class="cf-kpi-label" style="color: #f59e0b;">Chưa có Quản lý</span>
                    <div class="cf-kpi-value" style="color: #f59e0b;"><%= request.getAttribute("kpiUnassignedCentres") != null ? request.getAttribute("kpiUnassignedCentres") : 0 %></div>
                    <span class="cf-kpi-sub">Rạp đang trống vị trí Quản lý</span>
                </div>
                <div class="cf-kpi-card">
                    <span class="cf-kpi-label" style="color: #38bdf8;">Quản lý sẵn sàng</span>
                    <div class="cf-kpi-value" style="color: #38bdf8;"><%= request.getAttribute("kpiUnassignedManagers") != null ? request.getAttribute("kpiUnassignedManagers") : 0 %></div>
                    <span class="cf-kpi-sub">Quản lý tự do sẵn sàng bổ nhiệm</span>
                </div>
            </div>

            <% if ("manager_assigned".equals(successParam)) { %>
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> <strong>Thành công!</strong> Đã bổ nhiệm Quản lý phụ trách trung tâm và ghi nhận vào lịch sử phân công.
                </div>
            <% } else if ("transferred".equals(successParam)) { %>
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> <strong>Thành công!</strong> Đã điều chuyển nhân sự sang trung tâm mới và lưu vết lịch sử kiểm toán.
                </div>
            <% } else if ("manager_already_assigned".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Từ chối bổ nhiệm:</strong> Quản lý này hiện đã có phân công tại cụm rạp khác. Không được phép phân công trùng lặp.
                </div>
            <% } else if ("centre_already_has_manager".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Từ chối:</strong> Trung tâm này hiện đã có Quản lý phụ trách. Không được phép bổ nhiệm đè.
                </div>
            <% } else if ("dest_centre_has_manager".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Từ chối điều chuyển:</strong> Trung tâm đích đã có Quản lý phụ trách. Hệ thống không cho phép ghi đè phân công của Quản lý tại trung tâm đích.
                </div>
            <% } else if ("manager_not_assigned".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Quản lý này chưa có phân công hiện hành, vui lòng sử dụng chức năng Bổ nhiệm.
                </div>
            <% } else if ("concurrency_conflict".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Xung đột đồng thời:</strong> Trung tâm hoặc Quản lý đã được phân công bởi một giao dịch khác vừa diễn ra. Vui lòng tải lại trang.
                </div>
            <% } else if ("unassign_not_supported".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> Nghiệp vụ miễn nhiệm Quản lý không thuộc phạm vi xử lý của đợt này.
                </div>
            <% } else if ("centre_inactive".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Từ chối:</strong> Trung tâm chiếu phim được chọn hiện đang tạm ngưng hoạt động hoặc bảo trì, không thể tiếp nhận nhân sự.
                </div>
            <% } else if ("user_inactive".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Từ chối:</strong> Tài khoản nhân sự đang ở trạng thái Tạm ngưng hoặc Khóa (LOCKED/INACTIVE), không thể bổ nhiệm hoặc điều chuyển.
                </div>
            <% } else if ("same_centre".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Trung tâm đích trùng với trung tâm hiện tại.
                </div>
            <% } else if ("missing_reason".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> Vui lòng nhập lý do điều chuyển nhân sự để phục vụ lưu vết kiểm toán.
                </div>
            <% } else if ("manager_inactive".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Tài khoản Quản lý đang ở trạng thái Tạm ngưng/Khóa, không thể bổ nhiệm vào trung tâm.
                </div>
            <% } else if ("not_manager".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Tài khoản được chọn không phải là vai trò Manager trong cơ sở dữ liệu.
                </div>
            <% } else if (errorParam != null) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> Thao tác thất bại. Vui lòng kiểm tra lại thông tin.
                </div>
            <% } %>

            <!-- THANH ĐIỀU HƯỚNG TAB CHÍNH -->
            <div class="tab-nav">
                <button type="button" class="tab-btn <%= "manager".equals(activeTabParam) ? "active" : "" %>" onclick="switchMainTab('tab-manager-assignment')">
                    🏛 Bổ nhiệm Quản lý Trung tâm
                </button>
                <button type="button" class="tab-btn <%= "staff".equals(activeTabParam) ? "active" : "" %>" onclick="switchMainTab('tab-staff-transfer')">
                    👥 Điều chuyển nhân viên
                </button>
            </div>

            <!-- ==================================================================== -->
            <!-- TAB 1: BỔ NHIỆM MANAGER TRUNG TÂM -->
            <!-- ==================================================================== -->
            <div class="tab-pane <%= "manager".equals(activeTabParam) ? "active" : "" %>" id="tab-manager-assignment">
                <section class="cf-card">
                    <div class="cf-card-header">
                        <div>
                            <h2>Phân công Quản lý theo Trung tâm</h2>
                            <p style="margin: 4px 0 0; color: #94a3b8; font-size: 12.5px;">Quản lý phân công duy nhất 1 Quản lý/trung tâm. Nhận diện độc lập giữa Đang hoạt động, Đang bị khóa và Chưa có Quản lý.</p>
                        </div>
                    </div>
                    <div class="cf-card-body">
                        <div class="cf-table-wrap">
                            <table class="cf-table">
                                <thead>
                                    <tr>
                                        <th>Trung tâm chiếu phim</th>
                                        <th>Khu vực</th>
                                        <th>Mã nhân sự</th>
                                        <th>Quản lý đương nhiệm</th>
                                        <th>Trạng thái phân công</th>
                                        <th>Trạng thái rạp</th>
                                        <th>Thao tác</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <% if (centresWithManager == null || centresWithManager.isEmpty()) { %>
                                        <tr><td colspan="7" class="cf-empty">Chưa có trung tâm nào trong hệ thống.</td></tr>
                                    <% } else for (Centre b : centresWithManager) {
                                        boolean isActiveManager = b.hasActiveManager();
                                        boolean isCentreActive = "ACTIVE".equalsIgnoreCase(b.getStatus());
                                    %>
                                        <tr>
                                            <td>
                                                <strong style="color: #fff; font-size: 14px;"><%= b.getCentreName() %></strong>
                                                <small style="display: block; color: #94a3b8;"><%= b.getAddress() %></small>
                                            </td>
                                            <td><span class="cf-badge cf-badge-info"><%= b.getCity() %></span></td>
                                            <td>
                                                <% if (isActiveManager) { %>
                                                    <code class="cf-badge cf-badge-primary" style="font-size: 12px; font-weight: 700; letter-spacing: 0.5px;"><%= String.format("QL%05d", b.getManagerId()) %></code>
                                                <% } else { %>
                                                    <span style="color: #64748b;">—</span>
                                                <% } %>
                                            </td>
                                            <td>
                                                <% if (isActiveManager) { %>
                                                    <strong style="color: #818cf8; font-size: 14px;"><%= b.getManagerName() %></strong>
                                                    <small style="display: block; color: #94a3b8;"><%= b.getManagerEmail() %></small>
                                                <% } else { %>
                                                    <span style="color: #94a3b8;">—</span>
                                                <% } %>
                                            </td>
                                            <td>
                                                <% if (isActiveManager) { %>
                                                    <span class="cf-badge cf-badge-success" style="font-size: 11px;">Có Manager đang hoạt động</span>
                                                <% } else { %>
                                                    <span class="cf-badge cf-badge-warning" style="font-size: 11px;">Chưa có Manager</span>
                                                <% } %>
                                            </td>
                                            <td>
                                                <span class="cf-badge <%= isCentreActive ? "cf-badge-success" : "cf-badge-danger" %>">
                                                    <%= b.getStatus() %>
                                                </span>
                                            </td>
                                            <td>
                                                <div style="display: flex; gap: 8px;">
                                                    <% if (isActiveManager) { %>
                                                        <span class="cf-badge cf-badge-info" style="padding: 6px 12px; font-size: 12px;" title="Trung tâm đã có Quản lý phụ trách.">
                                                            Đang đương nhiệm
                                                        </span>
                                                    <% } else { %>
                                                        <button type="button" class="cf-btn cf-btn-primary cf-btn-sm" 
                                                                <%= !isCentreActive ? "disabled style='opacity: 0.45; cursor: not-allowed;'" : "" %>
                                                                title="<%= !isCentreActive ? "Trung tâm đang ngừng hoạt động/bảo trì, không thể bổ nhiệm" : "Bổ nhiệm Quản lý mới cho trung tâm" %>"
                                                                onclick="<%= isCentreActive ? "openAssignManagerModal(" + b.getCentreId() + ", '" + b.getCentreName().replace("'", "\\'") + "')" : "" %>">
                                                            Bổ nhiệm
                                                        </button>
                                                    <% } %>
                                                </div>
                                            </td>
                                        </tr>
                                    <% } %>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </section>

                <!-- LỊCH SỬ BỔ NHIỆM MANAGER (CENTRE MANAGER ASSIGNMENTS) -->
                <section class="cf-card" style="margin-top: 24px;">
                    <div class="cf-card-header">
                        <div>
                            <h2>Lịch sử Bổ nhiệm Quản lý Trung tâm</h2>
                        </div>
                    </div>
                    <div class="cf-card-body">
                        <div class="cf-table-wrap">
                            <table class="cf-table">
                                <thead>
                                    <tr>
                                        <th>Mã</th>
                                        <th>Trung tâm</th>
                                        <th>Mã nhân sự</th>
                                        <th>Từ ngày</th>
                                        <th>Đến ngày</th>
                                        <th>Trạng thái</th>
                                        <th>Người bổ nhiệm (Admin)</th>
                                        <th>Ghi chú</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <% if (managerAssignments == null || managerAssignments.isEmpty()) { %>
                                        <tr><td colspan="9" class="cf-empty">Chưa có lịch sử bổ nhiệm nào được ghi nhận.</td></tr>
                                    <% } else for (ManagerAssignment ma : managerAssignments) { %>
                                        <tr>
                                            <td><strong><%= ma.getAssignmentId() %></strong></td>
                                            <td><strong><%= ma.getCentreName() %></strong> <small style="color: #94a3b8;">(<%= ma.getCentreCity() %>)</small></td>
                                            <td><code class="cf-badge cf-badge-primary" style="font-size: 11px; font-weight: 700;"><%= ma.getManagerCode() %></code></td>
                                          
                                            <td><%= ma.getStartDate() != null ? sdfDate.format(ma.getStartDate()) : "—" %></td>
                                            <td>
                                                <% if (ma.getEndDate() == null) { %>
                                                    <span class="cf-badge cf-badge-success">Đang đương nhiệm</span>
                                                <% } else { %>
                                                    <span style="color: #94a3b8;"><%= sdfDate.format(ma.getEndDate()) %></span>
                                                <% } %>
                                            </td>
                                            <td>
                                                <span class="cf-badge <%= "ACTIVE".equalsIgnoreCase(ma.getStatus()) ? "cf-badge-success" : "REPLACED".equalsIgnoreCase(ma.getStatus()) ? "cf-badge-warning" : "cf-badge-danger" %>">
                                                    <%= "ACTIVE".equalsIgnoreCase(ma.getStatus()) ? "Đang hiệu lực" : "REPLACED".equalsIgnoreCase(ma.getStatus()) ? "Đã thay thế" : "Đã kết thúc" %>
                                                </span>
                                            </td>
                                            <td><span class="cf-badge cf-badge-info"><%= ma.getAssignedByName() %></span></td>
                                            <td style="color: #94a3b8;"><%= ma.getNotes() != null ? ma.getNotes() : "—" %></td>
                                        </tr>
                                    <% } %>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </section>
            </div>

            <!-- ==================================================================== -->
            <!-- TAB 2: ĐIỀU CHUYỂN NHÂN VIÊN (STAFF TRANSFER) -->
            <!-- ==================================================================== -->
            <div class="tab-pane <%= "staff".equals(activeTabParam) ? "active" : "" %>" id="tab-staff-transfer">
                <section class="cf-card">
                    <div class="cf-card-header" style="display: flex; justify-content: space-between; align-items: center; gap: 16px; flex-wrap: wrap;">
                        <div>
                            <h2>Danh sách Nhân viên rạp (Staff Directory)</h2>
                            <p style="margin: 3px 0 0; color: #94a3b8; font-size: 12px;">Bao gồm Nhân viên bán vé và Nhân viên soát vé tại tất cả chi nhánh.</p>
                        </div>
                        <div style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap;">
                            <form method="get" action="${pageContext.request.contextPath}/admin/transfers" style="display: flex; gap: 8px; align-items: center; flex-wrap: wrap;">
                                <input type="hidden" name="tab" value="staff">
                                <select name="role" style="padding: 8px 12px; border: 1px solid #334155; border-radius: 6px; font-size: 13px; background: #1e293b; color: #f8fafc;" onchange="this.form.submit()">
                                    <option value="ALL" <%= (roleFilter == null || "ALL".equalsIgnoreCase(roleFilter)) ? "selected" : "" %>>Tất cả vai trò nhân viên</option>
                                    <option value="TICKET_SALES_STAFF" <%= "TICKET_SALES_STAFF".equalsIgnoreCase(roleFilter) ? "selected" : "" %>>Nhân viên bán vé (Ticket Sales)</option>
                                    <option value="TICKET_CHECKER_STAFF" <%= "TICKET_CHECKER_STAFF".equalsIgnoreCase(roleFilter) ? "selected" : "" %>>Nhân viên soát vé (Ticket Checker)</option>
                                </select>
                                <input type="text" name="search" id="staffFilterInput" 
                                       value="<%= request.getAttribute("searchParam") != null ? request.getAttribute("searchParam") : "" %>"
                                       placeholder="🔍 Tìm theo Mã nhân viên (VD: NV00012), tài khoản, họ tên..." 
                                       style="padding: 8px 14px; border: 1px solid #334155; border-radius: 6px; font-size: 13px; min-width: 300px; background: #1e293b; color: #f8fafc;">
                                <button type="submit" class="cf-btn cf-btn-secondary cf-btn-sm">Tìm kiếm</button>
                                <% if ((request.getAttribute("searchParam") != null && !((String)request.getAttribute("searchParam")).trim().isEmpty()) || (roleFilter != null && !"ALL".equalsIgnoreCase(roleFilter))) { %>
                                    <a href="${pageContext.request.contextPath}/admin/transfers?tab=staff" class="cf-btn cf-btn-secondary cf-btn-sm" style="color: #94a3b8;">Xóa lọc</a>
                                <% } %>
                            </form>
                        </div>
                    </div>
                    <div class="cf-card-body">
                        <div class="cf-table-wrap">
                            <table class="cf-table">
                                <thead>
                                    <tr>
                                        <th>Mã nhân sự</th>
                                        <th>Nhân sự</th>
                                        <th>Tên đăng nhập</th>
                                        <th>Vai trò</th>
                                        <th>Trung tâm hiện tại</th>
                                        <th>Trạng thái</th>
                                        <th>Thao tác</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <% if (staffList == null || staffList.isEmpty()) { %>
                                        <tr><td colspan="7" class="cf-empty">Không tìm thấy nhân sự nào phù hợp với bộ lọc và từ khóa tìm kiếm.</td></tr>
                                    <% } else for (User item : staffList) {
                                        String st = item.getStatus();
                                        boolean isUserActive = "ACTIVE".equalsIgnoreCase(st);
                                        String centreDisplay = item.getCentreName() != null ? item.getCentreName() : "Chưa phân trung tâm";
                                        String searchData = (item.getEmployeeCode() + " " + item.getUserId() + " nv" + item.getUserId() + " " + item.getFullName() + " " + item.getEmail() + " " + item.getAccount() + " " + centreDisplay).toLowerCase();
                                    %>
                                        <tr class="staff-row" data-search="<%= searchData %>">
                                            <td><code class="cf-badge cf-badge-info" style="font-size: 12px; font-weight: 700; letter-spacing: 0.5px;"><%= item.getEmployeeCode() %></code></td>
                                            <td>
                                                <strong style="color: #fff;"><%= item.getFullName() %></strong>
                                                <small style="display: block; color: #94a3b8;"><%= item.getEmail() %></small>
                                            </td>
                                            <td><code><%= item.getAccount() %></code></td>
                                            <td>
                                                <% if (item.isTicketSalesStaff()) { %>
                                                    <span class="cf-badge" style="background: rgba(56, 189, 248, 0.15); color: #38bdf8; border: 1px solid rgba(56, 189, 248, 0.35);">
                                                        🎫 Bán vé (Ticket Sales)
                                                    </span>
                                                <% } else if (item.isTicketCheckerStaff()) { %>
                                                    <span class="cf-badge" style="background: rgba(168, 85, 247, 0.15); color: #c084fc; border: 1px solid rgba(168, 85, 247, 0.35);">
                                                        🔍 Soát vé (Ticket Checker)
                                                    </span>
                                                <% } else { %>
                                                    <span class="cf-badge cf-badge-info">Nhân viên</span>
                                                <% } %>
                                            </td>
                                            <td>
                                                <% if (item.getCentreId() != null && item.getCentreId() > 0) { %>
                                                    <span style="font-weight: 500; color: #e2e8f0;">🏛 <%= item.getCentreName() %></span>
                                                <% } else { %>
                                                    <span class="cf-badge cf-badge-warning">Chưa phân trung tâm</span>
                                                <% } %>
                                            </td>
                                            <td>
                                                <span class="cf-badge <%= isUserActive ? "cf-badge-success" : "LOCKED".equalsIgnoreCase(st) ? "cf-badge-danger" : "cf-badge-warning" %>">
                                                    <%= isUserActive ? "Hoạt động" : "LOCKED".equalsIgnoreCase(st) ? "Đã khóa" : "Tạm ngưng" %>
                                                </span>
                                            </td>
                                            <td>
                                                <button type="button" class="cf-btn cf-btn-primary cf-btn-sm" 
                                                        <%= !isUserActive ? "disabled style='opacity: 0.45; cursor: not-allowed;'" : "" %>
                                                        title="<%= !isUserActive ? "Tài khoản đang bị khóa/tạm ngưng, không thể điều chuyển" : "Điều chuyển nhân sự" %>"
                                                        onclick="<%= isUserActive ? "openStaffTransferModal(" + item.getUserId() + ", '" + item.getEmployeeCode() + "', '" + item.getFullName().replace("'", "\\'") + "', '" + item.getAccount() + "', '" + item.getRole() + "', " + (item.getCentreId() != null ? item.getCentreId() : "null") + ", '" + (item.getCentreName() != null ? item.getCentreName() : "Chưa phân trung tâm").replace("'", "\\'") + "')" : "" %>">
                                                    Điều chuyển
                                                </button>
                                            </td>
                                        </tr>
                                    <% } %>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </section>

                <!-- LỊCH SỬ ĐIỀU CHUYỂN & THĂNG CHỨC -->
                <section class="cf-card" style="margin-top: 24px;">
                    <div class="cf-card-header">
                        <div>
                            <h2>Lịch sử điều chuyển nhân sự & phê chuẩn thăng chức</h2>
                            <p style="margin: 3px 0 0; color: #94a3b8; font-size: 12px;">Lưu vết kiểm toán toàn diện tất cả các đợt luân chuyển chi nhánh và bổ nhiệm nhân sự trong hệ thống.</p>
                        </div>
                    </div>
                    <div class="cf-card-body">
                        <div class="cf-table-wrap">
                            <table class="cf-table">
                                <thead>
                                    <tr>
                                        <th>Mã</th>
                                        <th>Thời gian ra quyết định</th>
                                        <th>Ngày hiệu lực</th>
                                        <th>Mã nhân sự</th>
                                        <th>Nhân sự</th>
                                        <th>Chức vụ (Trước ➔ Sau)</th>
                                        <th>Từ trung tâm</th>
                                        <th>Đến trung tâm</th>
                                        <th>Người thực hiện (Admin)</th>
                                        <th>Lý do & Quyết định</th>
                                        <th>Ghi chú</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <% if (transferHistory == null || transferHistory.isEmpty()) { %>
                                        <tr><td colspan="11" class="cf-empty">Chưa có lịch sử điều chuyển nào được ghi nhận.</td></tr>
                                    <% } else for (StaffTransfer t : transferHistory) { 
                                        String oldR = t.getOldRoleDisplayName();
                                        String newR = t.getNewRoleDisplayName();
                                        boolean roleChanged = (t.getOldRole() != null && t.getNewRole() != null && !t.getOldRole().equalsIgnoreCase(t.getNewRole()));
                                    %>
                                        <tr>
                                            <td><strong>#<%= t.getTransferId() %></strong></td>
                                            <td><%= t.getTransferDate() != null ? sdf.format(t.getTransferDate()) : "—" %></td>
                                            <td>
                                                <% if (t.getEffectiveDate() != null) { %>
                                                    <strong style="color: #38bdf8;"><%= sdfDate.format(t.getEffectiveDate()) %></strong>
                                                <% } else { %>
                                                    <span style="color: #94a3b8;"><%= t.getTransferDate() != null ? sdfDate.format(t.getTransferDate()) : "—" %></span>
                                                <% } %>
                                            </td>
                                            <td><code class="cf-badge <%= "MANAGER".equalsIgnoreCase(t.getUserRole()) ? "cf-badge-primary" : "cf-badge-info" %>" style="font-size: 11px; font-weight: 700;"><%= t.getEmployeeCode() %></code></td>
                                            <td>
                                                <strong style="color: #fff;"><%= t.getUserName() %></strong>
                                                <small style="display: block; color: #94a3b8;">@<%= t.getUserAccount() %></small>
                                            </td>
                                            <td>
                                                <% if (roleChanged) { %>
                                                    <div style="font-size: 12px; display: flex; align-items: center; gap: 4px;">
                                                        <span style="color: #94a3b8;"><%= oldR %></span>
                                                        <span style="color: #ff2855;">➔</span>
                                                        <strong style="color: #4ade80;"><%= newR %></strong>
                                                    </div>
                                                <% } else { %>
                                                    <span style="color: #cbd5e1; font-size: 12.5px;"><%= newR %></span>
                                                <% } %>
                                            </td>
                                            <td><%= t.getFromCentreName() != null ? t.getFromCentreName() : "Chưa phân trung tâm" %></td>
                                            <td><strong style="color: #4ade80;"><%= t.getToCentreName() %></strong></td>
                                            <td><span class="cf-badge cf-badge-info"><%= t.getTransferredByName() %></span></td>
                                            <td><%= t.getReason() != null ? t.getReason() : "—" %></td>
                                            <td style="color: #94a3b8;"><%= t.getNotes() != null && !t.getNotes().isEmpty() ? t.getNotes() : "—" %></td>
                                        </tr>
                                    <% } %>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </section>
            </div>
        </main>
    </div>
</div>

<!-- ==================== MODAL BỔ NHIỆM MANAGER ==================== -->
<div class="cf-modal-backdrop" id="assignManagerModal">
    <div class="cf-modal">
        <div class="cf-modal-header">
            <h3>Bổ nhiệm Quản lý Trung tâm</h3>
            <button type="button" class="cf-modal-close" onclick="closeAssignManagerModal()">&times;</button>
        </div>
        <form method="post" action="${pageContext.request.contextPath}/admin/transfers">
            <input type="hidden" name="action" value="assign_manager">
            <input type="hidden" name="centreId" id="assignCentreId">
            <div class="cf-modal-body">
                <div class="info-strip">
                    <div>
                        <span>Trung tâm tiếp nhận:</span>
                        <strong id="assignCentreName">—</strong>
                    </div>
                    <div>
                        <span>Tình trạng hiện tại:</span>
                        <span class="cf-badge cf-badge-warning" style="font-size: 11px;">Chưa có Quản lý</span>
                    </div>
                </div>

                <div class="cf-form-group">
                    <label>Chọn Quản lý được bổ nhiệm *</label>
                    <select name="managerId" id="assignManagerSelect" required>
                        <option value="">-- Chọn Quản lý chưa phân công --</option>
                        <% 
                        int unassignedActiveCount = 0;
                        if (managers != null) for (User m : managers) { 
                            if (!"ACTIVE".equalsIgnoreCase(m.getStatus())) continue;
                            if (m.getCentreId() != null && m.getCentreId() > 0) continue;
                            unassignedActiveCount++;
                            String mSearch = (m.getEmployeeCode() + " " + m.getUserId() + " ql" + m.getUserId() + " " + m.getFullName() + " " + m.getAccount()).toLowerCase();
                        %>
                            <option value="<%= m.getUserId() %>" data-search="<%= mSearch %>">
                                [<%= m.getEmployeeCode() %>] <%= m.getFullName() %> (@<%= m.getAccount() %>)
                            </option>
                        <% } 
                        if (unassignedActiveCount == 0) { %>
                            <option value="" disabled>-- Hiện không có Quản lý nào đang trống rạp --</option>
                        <% } %>
                    </select>
                    <small style="color: #64748B; font-size: 12px;">Chỉ hiển thị các Quản lý đang hoạt động (ACTIVE) và chưa được phân công tại bất kỳ rạp nào.</small>
                </div>

                <div class="cf-form-group">
                    <label>Ghi chú / Quyết định bổ nhiệm</label>
                    <textarea name="notes" rows="3" placeholder="Ví dụ: Quyết định bổ nhiệm số 24/QĐ-BGD ngày 14/10/2026"></textarea>
                </div>
            </div>
            <div class="cf-modal-footer">
                <button type="button" class="cf-btn cf-btn-secondary" onclick="closeAssignManagerModal()">Hủy</button>
                <button type="submit" class="cf-btn cf-btn-primary">Xác nhận bổ nhiệm</button>
            </div>
        </form>
    </div>
</div>

<!-- ==================== MODAL ĐIỀU CHUYỂN STAFF ==================== -->
<div class="cf-modal-backdrop" id="staffTransferModal">
    <div class="cf-modal">
        <div class="cf-modal-header">
            <h3>Điều chuyển nhân sự giữa các trung tâm</h3>
            <button type="button" class="cf-modal-close" onclick="closeStaffTransferModal()">&times;</button>
        </div>
        <form method="post" action="${pageContext.request.contextPath}/admin/transfers">
            <input type="hidden" name="action" value="transfer_staff">
            <input type="hidden" name="userId" id="transferStaffUserId">
            <div class="cf-modal-body">
                <div class="info-strip">
                    <div>
                        <span>Mã nhân viên (Employee ID):</span>
                        <code class="cf-badge cf-badge-info" style="font-size: 13px; font-weight: 700;" id="transferStaffEmployeeCode">NV00000</code>
                    </div>
                    <div>
                        <span>Nhân sự:</span>
                        <strong id="transferStaffUserName">—</strong>
                    </div>
                    <div>
                        <span>Vị trí công việc:</span>
                        <strong id="transferStaffRoleDisplay" style="color: #0284c7;">—</strong>
                    </div>
                    <div>
                        <span>Tài khoản:</span>
                        <strong id="transferStaffAccount" style="color: #64748B;">—</strong>
                    </div>
                    <div>
                        <span>Trung tâm hiện tại:</span>
                        <strong id="transferStaffCurrentCentre" style="color: #d97706;">—</strong>
                    </div>
                </div>

                <div class="cf-form-group">
                    <label>Trung tâm chuyển đến *</label>
                    <select name="toCentreId" id="toCentreSelect" required>
                        <option value="">-- Chọn trung tâm đích --</option>
                        <% if (activeCentres != null) for (Centre b : activeCentres) { %>
                            <option value="<%= b.getCentreId() %>">
                                <%= b.getCentreName() %> (<%= b.getCity() %>)
                            </option>
                        <% } %>
                    </select>
                </div>

                <div class="cf-form-group" id="transferStaffRoleGroup">
                    <label>Chức vụ tại trung tâm mới</label>
                    <select name="newRole" id="transferStaffNewRole">
                        <option value="KEEP">-- Giữ nguyên chức vụ hiện tại --</option>
                        <option value="TICKET_SALES_STAFF">🎫 Nhân viên Bán vé (Ticket Sales)</option>
                        <option value="TICKET_CHECKER_STAFF">🔍 Nhân viên Soát vé (Ticket Checker)</option>
                    </select>
                </div>

                <div class="cf-form-group">
                    <label>Ngày có hiệu lực *</label>
                    <input type="date" name="effectiveDate" id="transferEffectiveDate" required>
                </div>

                <div class="cf-form-group">
                    <label>Lý do điều chuyển (Bắt buộc) *</label>
                    <input type="text" name="reason" placeholder="Ví dụ: Tăng cường nhân sự mở trung tâm mới / Luân chuyển định kỳ" required>
                </div>

                <div class="cf-form-group">
                    <label>Ghi chú bổ sung</label>
                    <textarea name="notes" rows="3" placeholder="Ghi chú thêm..."></textarea>
                </div>
            </div>
            <div class="cf-modal-footer">
                <button type="button" class="cf-btn cf-btn-secondary" onclick="closeStaffTransferModal()">Hủy</button>
                <button type="submit" class="cf-btn cf-btn-primary">Xác nhận điều chuyển</button>
            </div>
        </form>
    </div>
</div>

<script>
    function switchMainTab(tabId) {
        document.querySelectorAll('.tab-btn').forEach(function(b) { b.classList.remove('active'); });
        document.querySelectorAll('.tab-pane').forEach(function(p) { p.classList.remove('active'); });
        event.target.classList.add('active');
        document.getElementById(tabId).classList.add('active');
    }

    // Modal Bổ nhiệm Manager
    function openAssignManagerModal(centreId, centreName) {
        document.getElementById('assignCentreId').value = centreId;
        document.getElementById('assignCentreName').textContent = centreName;
        var select = document.getElementById('assignManagerSelect');
        if (select) select.value = '';
        document.getElementById('assignManagerModal').classList.add('active');
    }
    function closeAssignManagerModal() {
        document.getElementById('assignManagerModal').classList.remove('active');
    }

    // Modal điều chuyển nhân viên
    function openStaffTransferModal(userId, empCode, name, account, role, currentCentreId, currentCentreName) {
        document.getElementById('transferStaffUserId').value = userId;
        document.getElementById('transferStaffEmployeeCode').textContent = empCode;
        document.getElementById('transferStaffUserName').textContent = name;
        document.getElementById('transferStaffAccount').textContent = '@' + account;
        document.getElementById('transferStaffCurrentCentre').textContent = currentCentreName;

        var roleText = role || '';
        if (role === 'TICKET_SALES_STAFF' || role === 'TICKET_SALES') {
            roleText = '🎫 Bán vé (Ticket Sales)';
        } else if (role === 'TICKET_CHECKER_STAFF' || role === 'TICKET_CHECKER') {
            roleText = '🔍 Soát vé (Ticket Checker)';
        }
        var roleEl = document.getElementById('transferStaffRoleDisplay');
        if (roleEl) roleEl.textContent = roleText;

        var roleGroup = document.getElementById('transferStaffRoleGroup');
        var newRoleSelect = document.getElementById('transferStaffNewRole');
        if (roleGroup) roleGroup.style.display = 'block';
        if (newRoleSelect) newRoleSelect.value = 'KEEP';

        var effectiveDateInput = document.getElementById('transferEffectiveDate');
        if (effectiveDateInput) {
            var today = new Date().toISOString().split('T')[0];
            effectiveDateInput.value = today;
        }

        var select = document.getElementById('toCentreSelect');
        select.value = "";
        for (var i = 0; i < select.options.length; i++) {
            var opt = select.options[i];
            if (opt.value && currentCentreId !== null && parseInt(opt.value) === currentCentreId) {
                opt.disabled = true;
            } else {
                opt.disabled = false;
            }
        }
        document.getElementById('staffTransferModal').classList.add('active');
    }
    function closeStaffTransferModal() {
        document.getElementById('staffTransferModal').classList.remove('active');
    }

    // Đóng modal khi bấm vào vùng nền mờ
    document.querySelectorAll('.cf-modal-backdrop').forEach(function(backdrop) {
        backdrop.addEventListener('click', function(e) {
            if (e.target === this) this.classList.remove('active');
        });
    });

    // Xử lý thông báo và bộ lọc nhân viên client-side
    document.addEventListener('DOMContentLoaded', function () {
        var alerts = document.querySelectorAll(
            '.cf-alert-success, .cf-alert-danger'
        );

        alerts.forEach(function (alert) {
            window.setTimeout(function () {
                alert.style.transition = 'opacity 0.4s ease, transform 0.4s ease';
                alert.style.opacity = '0';
                alert.style.transform = 'translateY(-8px)';

                window.setTimeout(function () {
                    if (alert.parentNode) {
                        alert.parentNode.removeChild(alert);
                    }
                }, 400);
            }, 2500);
        });

        var input = document.getElementById('staffFilterInput');
        var rows = document.querySelectorAll('.staff-row');

        if (input) {
            input.addEventListener('input', function () {
                var q = this.value.toLowerCase().trim();

                rows.forEach(function (row) {
                    var s = row.getAttribute('data-search') || '';
                    row.style.display = (!q || s.indexOf(q) !== -1)
                        ? ''
                        : 'none';
                });
            });
        }
    });
</script>
</body>
</html>
