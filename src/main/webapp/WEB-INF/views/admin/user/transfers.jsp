<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.entity.User,model.entity.Centre,java.util.List,java.text.SimpleDateFormat" %>
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
    List<User> staffList = (List<User>) request.getAttribute("staffList");
    Integer selectedCentreId = (Integer) request.getAttribute("selectedCentreId");

    request.setAttribute("activeNav", "transfers");
    request.setAttribute("pageTitle", "Bổ nhiệm & Điều chuyển");

    String successParam = request.getParameter("success");
    String errorParam = request.getParameter("error");
    String activeTabParam = request.getParameter("tab");
    if (activeTabParam == null) activeTabParam = "manager";

    String roleFilter = (String) request.getAttribute("roleFilter");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Bổ nhiệm &amp; Điều chuyển Nhân sự | CineFlow Admin</title>
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
    <jsp:include page="/WEB-INF/fragments/adminSidebar.jspf" />

    <div class="cf-manager-main">
        <jsp:include page="/WEB-INF/fragments/adminTopbar.jspf" />

        <main class="cf-content">
            <div class="cf-page-header cf-page-header-wrap">
                <div class="cf-page-header-info">
                    <div class="cf-page-icon icon-transfers">👥</div>
                    <div>
                        <h1 class="cf-page-title">Bổ nhiệm &amp; Điều chuyển nhân sự</h1>
                        <p class="cf-page-sub">Bổ nhiệm Quản lý và điều chuyển nhân sự giữa các chi nhánh.</p>
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
                    <span class="cf-kpi-label kpi-assigned">Có Quản lý hoạt động</span>
                    <div class="cf-kpi-value kpi-assigned"><%= request.getAttribute("kpiAssignedCentres") != null ? request.getAttribute("kpiAssignedCentres") : 0 %></div>
                    <span class="cf-kpi-sub">Cụm rạp có Quản lý đương nhiệm (ACTIVE)</span>
                </div>
                <div class="cf-kpi-card <%= (request.getAttribute("kpiUnassignedCentres") != null && (int)request.getAttribute("kpiUnassignedCentres") > 0) ? "kpi-unassigned-card" : "" %>">
                    <span class="cf-kpi-label kpi-unassigned">Chưa có Quản lý</span>
                    <div class="cf-kpi-value kpi-unassigned"><%= request.getAttribute("kpiUnassignedCentres") != null ? request.getAttribute("kpiUnassignedCentres") : 0 %></div>
                    <span class="cf-kpi-sub">Rạp đang trống vị trí Quản lý</span>
                </div>
                <div class="cf-kpi-card">
                    <span class="cf-kpi-label kpi-available">Quản lý sẵn sàng</span>
                    <div class="cf-kpi-value kpi-available"><%= request.getAttribute("kpiUnassignedManagers") != null ? request.getAttribute("kpiUnassignedManagers") : 0 %></div>
                    <span class="cf-kpi-sub">Quản lý tự do sẵn sàng bổ nhiệm</span>
                </div>
            </div>

            <% if ("manager_assigned".equals(successParam)) { %>
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> <strong>Thành công!</strong> Đã bổ nhiệm Quản lý phụ trách trung tâm và ghi nhận vào lịch sử phân công.
                </div>
            <% } else if ("manager_unassigned".equals(successParam)) { %>
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> <strong>Thành công!</strong> Đã miễn nhiệm Quản lý khỏi trung tâm. Rạp hiện đã trống và Quản lý sẵn sàng để bổ nhiệm sang cơ sở khác.
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
                    <span>⚠</span> <strong>Từ chối điều chuyển Quản lý:</strong> Trung tâm đích đã có Quản lý phụ trách. Hệ thống chỉ cho phép điều chuyển Quản lý tới các trung tâm đang trống vị trí Quản lý.
                </div>
            <% } else if ("manager_not_assigned".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Quản lý này chưa có phân công hiện hành, vui lòng sử dụng chức năng Bổ nhiệm.
                </div>
            <% } else if ("concurrency_conflict".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Xung đột đồng thời:</strong> Trung tâm hoặc Quản lý đã được phân công bởi một giao dịch khác vừa diễn ra. Vui lòng tải lại trang.
                </div>
            <% } else if ("missing_unassign_reason".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> Vui lòng nhập lý do miễn nhiệm Quản lý để phục vụ kiểm toán và lưu vết nhiệm kỳ.
                </div>
            <% } else if ("unassign_failed".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Thao tác miễn nhiệm Quản lý không thành công. Vui lòng thử lại.
                </div>
            <% } else if ("manager_transfer_not_allowed".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lưu ý:</strong> Vị trí Quản lý trung tâm chỉ được thao tác qua <strong>Bổ nhiệm</strong> &amp; <strong>Miễn nhiệm</strong> tại Tab 1. Không áp dụng điều chuyển trực tiếp cho Quản lý.
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
                    🏛 Bổ nhiệm &amp; Miễn nhiệm Quản lý Trung tâm
                </button>
                <button type="button" class="tab-btn <%= "staff".equals(activeTabParam) ? "active" : "" %>" onclick="switchMainTab('tab-staff-transfer')">
                    👥 Điều chuyển nhân sự
                </button>
            </div>

            <!-- ==================================================================== -->
            <!-- TAB 1: BỔ NHIỆM & BÃI NHIỆM QUẢN LÝ THEO TRUNG TÂM -->
            <!-- ==================================================================== -->
            <div class="tab-pane <%= "manager".equals(activeTabParam) ? "active" : "" %>" id="tab-manager-assignment">
                <section class="cf-card">
                    <div class="cf-card-header cf-card-header-flex">
                        <div>
                            <h2>Bổ nhiệm &amp; Miễn nhiệm Quản lý theo Trung tâm</h2>
                        </div>
                    </div>
                    <div class="cf-card-body">
                        <div class="cf-table-wrap">
                            <table class="cf-table" id="table-centre-assignment">
                                <thead>
                                    <tr>
                                        <th>Trung tâm chiếu phim</th>
                                        <th>Khu vực</th>
                                        <th class="sortable-th" onclick="sortCentreByCol(2)" title="Sắp xếp theo Mã Quản lý (QLxxxxx)">
                                            Mã nhân sự <span class="sort-arrow" id="arrow-centre-2">↕</span>
                                        </th>
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
                                        <tr class="centre-row" data-mgr-id="<%= isActiveManager ? b.getManagerId() : 999999 %>">
                                            <td>
                                                <strong class="centre-name"><%= b.getCentreName() %></strong>
                                                <small class="centre-address"><%= b.getAddress() %></small>
                                            </td>
                                            <td><span class="cf-badge cf-badge-info"><%= b.getCity() %></span></td>
                                            <td>
                                                <% if (isActiveManager) { %>
                                                    <code class="cf-badge cf-badge-primary cf-badge-mgr-code"><%= String.format("QL%05d", b.getManagerId()) %></code>
                                                <% } else { %>
                                                    <span class="text-muted-dash">—</span>
                                                <% } %>
                                            </td>
                                            <td>
                                                <% if (isActiveManager) { %>
                                                    <strong class="manager-name-text"><%= b.getManagerName() %></strong>
                                                    <small class="manager-email-text"><%= b.getManagerEmail() %></small>
                                                <% } else { %>
                                                    <span class="text-muted-dash">—</span>
                                                <% } %>
                                            </td>
                                            <td>
                                                <% if (isActiveManager) { %>
                                                    <span class="cf-badge cf-badge-success cf-badge-sm">Có Manager đang hoạt động</span>
                                                <% } else { %>
                                                    <span class="cf-badge cf-badge-warning cf-badge-sm">Chưa có Manager</span>
                                                <% } %>
                                            </td>
                                            <td>
                                                <span class="cf-badge <%= isCentreActive ? "cf-badge-success" : "cf-badge-danger" %>">
                                                    <%= b.getStatus() %>
                                                </span>
                                            </td>
                                            <td>
                                                <div class="assignment-actions">
                                                    <% if (isActiveManager) { %>
                                                        <button type="button" class="cf-btn cf-btn-sm assignment-action-btn unassign-btn"
                                                                title="Miễn nhiệm Quản lý khỏi trung tâm này"
                                                                onclick="openUnassignManagerModal(<%= b.getCentreId() %>, '<%= b.getCentreName().replace("'", "\\'") %>', <%= b.getManagerId() %>, '<%= String.format("QL%05d", b.getManagerId()) %>', '<%= b.getManagerName().replace("'", "\\'") %>')">
                                                            Miễn nhiệm
                                                        </button>
                                                    <% } else { %>
                                                        <button type="button" class="cf-btn cf-btn-sm assignment-action-btn assign-btn"
                                                                <%= !isCentreActive ? "disabled class=\"btn-disabled-action\"" : "" %>
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
            </div>

            <!-- ==================================================================== -->
            <!-- TAB 2: ĐIỀU CHUYỂN NHÂN SỰ TOÀN HỆ THỐNG (STAFF & MANAGER) -->
            <!-- ==================================================================== -->
            <div class="tab-pane <%= "staff".equals(activeTabParam) ? "active" : "" %>" id="tab-staff-transfer">
                <section class="cf-card">
                    <div class="cf-card-header cf-card-header-flex">
                        <div>
                            <h2>Danh sách nhân sự</h2>
                            <p class="card-subtitle-sm">Quản lý và nhân viên trong toàn hệ thống.</p>
                        </div>
                        <div class="staff-filter-toolbar">
                            <form method="get" action="${pageContext.request.contextPath}/admin/transfers" class="staff-filter-form">
                                <input type="hidden" name="tab" value="staff">
                                <select name="role" class="select-dark" onchange="this.form.submit()">
                                    <option value="ALL" <%= (roleFilter == null || "ALL".equalsIgnoreCase(roleFilter)) ? "selected" : "" %>>Tất cả vai trò</option>
                                    <option value="TICKET_SALES_STAFF" <%= "TICKET_SALES_STAFF".equalsIgnoreCase(roleFilter) ? "selected" : "" %>>Bán vé</option>
                                    <option value="TICKET_CHECKER_STAFF" <%= "TICKET_CHECKER_STAFF".equalsIgnoreCase(roleFilter) ? "selected" : "" %>>Soát vé</option>
                                </select>

                                <!-- BỘ LỌC SẮP XẾP MÃ NHÂN SỰ -->
                                <select id="staffSortDropdown" onchange="applyStaffSort(this.value)" class="select-dark">
                                    <option value="code_asc">Mã nhân sự tăng dần</option>
                                    <option value="code_desc">Mã nhân sự giảm dần</option>
                                    <option value="name_asc">Tên A → Z</option>
                                    <option value="name_desc">Tên Z → A</option>
                                </select>

                                <input type="text" name="search" id="staffFilterInput" 
                                       value="<%= request.getAttribute("searchParam") != null ? request.getAttribute("searchParam") : "" %>"
                                       placeholder="Tìm theo mã, tài khoản hoặc họ tên..." 
                                       class="input-dark staff-search-input">
                                <button type="submit" class="cf-btn cf-btn-secondary cf-btn-sm">Tìm kiếm</button>
                                <% if ((request.getAttribute("searchParam") != null && !((String)request.getAttribute("searchParam")).trim().isEmpty()) || (roleFilter != null && !"ALL".equalsIgnoreCase(roleFilter))) { %>
                                    <a href="${pageContext.request.contextPath}/admin/transfers?tab=staff" class="cf-btn cf-btn-secondary cf-btn-sm btn-clear-filter">Xóa lọc</a>
                                <% } %>
                            </form>
                        </div>
                    </div>
                    <div class="cf-card-body">
                        <div class="cf-table-wrap">
                            <table class="cf-table" id="table-staff-list">
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
                                        String searchData = (item.getEmployeeCode() + " " + item.getUserId() + " nv" + item.getUserId() + " ql" + item.getUserId() + " " + item.getFullName() + " " + item.getEmail() + " " + item.getAccount() + " " + centreDisplay).toLowerCase();
                                    %>
                                        <tr class="staff-row" data-search="<%= searchData %>" data-emp-id="<%= item.getUserId() %>" data-emp-code="<%= item.getEmployeeCode() %>" data-emp-name="<%= item.getFullName() %>">
                                            <td><code class="cf-badge <%= "MANAGER".equalsIgnoreCase(item.getRole()) ? "cf-badge-primary" : "cf-badge-info" %> cf-badge-emp-code"><%= item.getEmployeeCode() %></code></td>
                                            <td>
                                                <strong class="staff-name"><%= item.getFullName() %></strong>
                                                <small class="staff-email"><%= item.getEmail() %></small>
                                            </td>
                                            <td><code><%= item.getAccount() %></code></td>
                                            <td>
                                                <% if ("MANAGER".equalsIgnoreCase(item.getRole()) || item.isManager()) { %>
                                                    <span class="cf-badge cf-badge-primary cf-badge-role-manager">
                                                        Quản lý
                                                    </span>
                                                <% } else if (item.isTicketSalesStaff()) { %>
                                                    <span class="cf-badge cf-badge-role-sales">
                                                        🎫 Bán vé (Ticket Sales)
                                                    </span>
                                                <% } else if (item.isTicketCheckerStaff()) { %>
                                                    <span class="cf-badge cf-badge-role-checker">
                                                        🔍 Soát vé (Ticket Checker)
                                                    </span>
                                                <% } else { %>
                                                    <span class="cf-badge cf-badge-info">Nhân viên</span>
                                                <% } %>
                                            </td>
                                            <td>
                                                <% if (item.getCentreId() != null && item.getCentreId() > 0) { %>
                                                    <span class="staff-centre-assigned">🏛 <%= item.getCentreName() %></span>
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
                                                        <%= !isUserActive ? "disabled class=\"btn-disabled-action\"" : "" %>
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
                        <span class="cf-badge cf-badge-warning cf-badge-sm">Chưa có Quản lý</span>
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
                    <small class="form-helper-text">Chỉ hiển thị các Quản lý đang hoạt động (ACTIVE) và chưa được phân công tại bất kỳ rạp nào.</small>
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

<!-- ==================== MODAL BÃI NHIỆM MANAGER ==================== -->
<div class="cf-modal-backdrop" id="unassignManagerModal">
    <div class="cf-modal">
        <div class="cf-modal-header cf-modal-header-danger">
            <h3 class="cf-modal-title-danger">✕ Miễn nhiệm Quản lý Trung tâm</h3>
            <button type="button" class="cf-modal-close" onclick="closeUnassignManagerModal()">&times;</button>
        </div>
        <form method="post" action="${pageContext.request.contextPath}/admin/transfers">
            <input type="hidden" name="action" value="unassign_manager">
            <input type="hidden" name="centreId" id="unassignCentreId">
            <input type="hidden" name="managerId" id="unassignManagerId">
            <div class="cf-modal-body">
                <div class="info-strip info-strip-danger">
                    <div>
                        <span>Trung tâm:</span>
                        <strong id="unassignCentreName" class="info-strip-centre">—</strong>
                    </div>
                    <div>
                        <span>Mã Quản lý:</span>
                        <code class="cf-badge cf-badge-primary cf-badge-mgr-code" id="unassignManagerCode">QL00000</code>
                    </div>
                    <div>
                        <span>Quản lý đương nhiệm:</span>
                        <strong id="unassignManagerName" class="unassign-manager-target">—</strong>
                    </div>
                </div>

                <div class="cf-alert cf-alert-warning cf-alert-unassign-warning">
                    <span>⚠️</span>
                    <div>
                        Sau khi miễn nhiệm:
                        <ul class="alert-list-styled">
                            <li>Trung tâm này sẽ chuyển về trạng thái <strong>Chưa có Quản lý</strong> (sẵn sàng bổ nhiệm mới).</li>
                            <li>Quản lý sẽ về trạng thái tự do (chưa phân công), có thể bổ nhiệm phụ trách trung tâm khác.</li>
                            <li>Lịch sử nhiệm kỳ sẽ được kết thúc và lưu vết kiểm toán.</li>
                        </ul>
                    </div>
                </div>

                <div class="cf-form-group cf-form-group-mt">
                    <label>Lý do miễn nhiệm (Bắt buộc) *</label>
                    <textarea name="reason" rows="3" required placeholder="Ví dụ: Quyết định miễn nhiệm số 18/QĐ-BGD ngày 26/10/2026; Miễn nhiệm để bổ nhiệm sang trung tâm mới..."></textarea>
                </div>
            </div>
            <div class="cf-modal-footer">
                <button type="button" class="cf-btn cf-btn-secondary" onclick="closeUnassignManagerModal()">Hủy</button>
            <button type="submit" class="cf-btn cf-btn-danger unassign-confirm-btn">Xác nhận miễn nhiệm</button>
            </div>
        </form>
    </div>
</div>

<!-- ==================== MODAL ĐIỀU CHUYỂN NHÂN SỰ ==================== -->
<div class="cf-modal-backdrop" id="staffTransferModal">
    <div class="cf-modal">
        <div class="cf-modal-header">
            <h3 id="modalTransferTitle">Điều chuyển nhân sự giữa các trung tâm</h3>
            <button type="button" class="cf-modal-close" onclick="closeStaffTransferModal()">&times;</button>
        </div>
        <form method="post" action="${pageContext.request.contextPath}/admin/transfers">
            <input type="hidden" name="action" value="transfer_staff">
            <input type="hidden" name="userId" id="transferStaffUserId">
            <div class="cf-modal-body">
                <div class="info-strip">
                    <div>
                        <span>Mã nhân viên (Employee ID):</span>
                        <code class="cf-badge cf-badge-info cf-badge-emp-id" id="transferStaffEmployeeCode">NV00000</code>
                    </div>
                    <div>
                        <span>Nhân sự:</span>
                        <strong id="transferStaffUserName">—</strong>
                    </div>
                    <div>
                        <span>Vị trí công việc:</span>
                        <strong id="transferStaffRoleDisplay" class="staff-role-preview">—</strong>
                    </div>
                    <div>
                        <span>Tài khoản:</span>
                        <strong id="transferStaffAccount" class="staff-account-preview">—</strong>
                    </div>
                    <div>
                        <span>Trung tâm hiện tại:</span>
                        <strong id="transferStaffCurrentCentre" class="staff-current-centre-preview">—</strong>
                    </div>
                </div>

                <div class="cf-form-group">
                    <label>Trung tâm chuyển đến *</label>
                    <select name="toCentreId" id="toCentreSelect" required>
                        <option value="">-- Chọn trung tâm đích --</option>
                        <% if (centresWithManager != null) for (Centre b : centresWithManager) { %>
                            <option value="<%= b.getCentreId() %>" data-base-name="<%= b.getCentreName() %> (<%= b.getCity() %>)">
                                <%= b.getCentreName() %> (<%= b.getCity() %>)
                            </option>
                        <% } %>
                    </select>
                </div>

                <div class="cf-form-group" id="transferStaffRoleGroup">
                    <label>Chức vụ tại trung tâm mới</label>
                    <select name="newRole" id="transferStaffNewRole" onchange="handleNewRoleChange()">
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
                    <input type="text" name="reason" placeholder="Ví dụ: Điều động luân chuyển công tác Hà Nội ➔ TP.HCM / Khai trương chi nhánh mới">
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

        // Modal Miễn nhiệm Manager
    function openUnassignManagerModal(centreId, centreName, managerId, managerCode, managerName) {
        document.getElementById('unassignCentreId').value = centreId;
        document.getElementById('unassignCentreName').textContent = centreName;
        document.getElementById('unassignManagerId').value = managerId;
        document.getElementById('unassignManagerCode').textContent = managerCode;
        document.getElementById('unassignManagerName').textContent = managerName;
        document.getElementById('unassignManagerModal').classList.add('active');
    }
    function closeUnassignManagerModal() {
        document.getElementById('unassignManagerModal').classList.remove('active');
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

    var currentTransferUserRole = '';
    var currentTransferUserCentreId = null;

    function handleNewRoleChange() {
        updateToCentreOptions();
    }

    function updateToCentreOptions() {
        var select = document.getElementById('toCentreSelect');
        if (!select) return;
        var selectedVal = select.value;
        var hasValidSelected = false;

        for (var i = 0; i < select.options.length; i++) {
            var opt = select.options[i];
            if (!opt.value) continue;
            var optCentreId = parseInt(opt.value);
            var baseName = opt.getAttribute('data-base-name') || opt.textContent.replace(/•.*$/, '').trim();

            if (currentTransferUserCentreId !== null && optCentreId === currentTransferUserCentreId) {
                opt.disabled = true;
                opt.textContent = baseName + ' • Trung tâm hiện tại (Không thể chọn)';
            } else {
                opt.disabled = false;
                opt.textContent = baseName;
            }

            if (opt.value === selectedVal && !opt.disabled) {
                hasValidSelected = true;
            }
        }

        if (!hasValidSelected) {
            select.value = '';
        }
    }

    // Modal điều chuyển nhân viên & Quản lý
    function openStaffTransferModal(userId, empCode, name, account, role, currentCentreId, currentCentreName) {
        currentTransferUserRole = role;
        currentTransferUserCentreId = currentCentreId;

        document.getElementById('transferStaffUserId').value = userId;
        document.getElementById('transferStaffEmployeeCode').textContent = empCode;
        document.getElementById('transferStaffUserName').textContent = name;
        document.getElementById('transferStaffAccount').textContent = '@' + account;
        document.getElementById('transferStaffCurrentCentre').textContent = currentCentreName;

        var roleText = 'Nhân viên';
        if (role === 'TICKET_SALES_STAFF' || role === 'TICKET_SALES') {
            roleText = '🎫 Bán vé (Ticket Sales)';
        } else if (role === 'TICKET_CHECKER_STAFF' || role === 'TICKET_CHECKER') {
            roleText = '🔍 Soát vé (Ticket Checker)';
        }
        var roleEl = document.getElementById('transferStaffRoleDisplay');
        if (roleEl) roleEl.textContent = roleText;

        var titleEl = document.getElementById('modalTransferTitle');
        if (titleEl) {
            titleEl.textContent = 'Điều chuyển nhân sự giữa các trung tâm';
        }

        var newRoleSelect = document.getElementById('transferStaffNewRole');
        if (newRoleSelect) newRoleSelect.value = 'KEEP';

        var effectiveDateInput = document.getElementById('transferEffectiveDate');
        if (effectiveDateInput) {
            var today = new Date().toISOString().split('T')[0];
            effectiveDateInput.value = today;
        }

        updateToCentreOptions();
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

    // Quản lý sắp xếp bảng nhân viên
    var staffSortOrder = { col: 0, order: 'asc' };
    function sortStaffByCol(colIndex, dataType) {
        var table = document.getElementById('table-staff-list');
        if (!table) return;
        var tbody = table.querySelector('tbody');
        var rows = Array.from(tbody.querySelectorAll('tr.staff-row'));
        if (rows.length === 0) return;

        var nextOrder = (staffSortOrder.col === colIndex && staffSortOrder.order === 'asc') ? 'desc' : 'asc';
        staffSortOrder = { col: colIndex, order: nextOrder };

        rows.sort(function(a, b) {
            var cellA = a.children[colIndex] ? a.children[colIndex].innerText.trim() : '';
            var cellB = b.children[colIndex] ? b.children[colIndex].innerText.trim() : '';
            return nextOrder === 'asc' ? cellA.localeCompare(cellB, 'vi', { numeric: true }) : cellB.localeCompare(cellA, 'vi', { numeric: true });
        });

        rows.forEach(function(r) { tbody.appendChild(r); });

        table.querySelectorAll('.sort-arrow').forEach(function(ar) {
            ar.textContent = '↕';
            ar.classList.remove('active');
        });
        var targetArrow = document.getElementById('arrow-staff-' + colIndex);
        if (targetArrow) {
            targetArrow.textContent = nextOrder === 'asc' ? '▲ (Thấp ➔ Cao)' : '▼ (Cao ➔ Thấp)';
            targetArrow.classList.add('active');
        }
    }

    function applyStaffSort(sortVal) {
        if (sortVal === 'code_asc') {
            staffSortOrder = { col: 0, order: 'desc' };
            sortStaffByCol(0, 'str');
        } else if (sortVal === 'code_desc') {
            staffSortOrder = { col: 0, order: 'asc' };
            sortStaffByCol(0, 'str');
        } else if (sortVal === 'name_asc') {
            staffSortOrder = { col: 1, order: 'desc' };
            sortStaffByCol(1, 'str');
        } else if (sortVal === 'name_desc') {
            staffSortOrder = { col: 1, order: 'asc' };
            sortStaffByCol(1, 'str');
        }
    }

    // Sắp xếp mã Quản lý ở Tab cụm rạp
    var centreSortOrder = 'asc';
    function sortCentreByCol(colIndex) {
        var table = document.getElementById('table-centre-assignment');
        if (!table) return;
        var tbody = table.querySelector('tbody');
        var rows = Array.from(tbody.querySelectorAll('tr.centre-row'));
        if (rows.length === 0) return;

        centreSortOrder = centreSortOrder === 'asc' ? 'desc' : 'asc';
        rows.sort(function(a, b) {
            var cellA = a.children[colIndex] ? a.children[colIndex].innerText.trim() : '';
            var cellB = b.children[colIndex] ? b.children[colIndex].innerText.trim() : '';
            return centreSortOrder === 'asc' ? cellA.localeCompare(cellB, 'vi', { numeric: true }) : cellB.localeCompare(cellA, 'vi', { numeric: true });
        });
        rows.forEach(function(r) { tbody.appendChild(r); });

        var arrow = document.getElementById('arrow-centre-2');
        if (arrow) {
            arrow.textContent = centreSortOrder === 'asc' ? '▲ (Thấp ➔ Cao)' : '▼ (Cao ➔ Thấp)';
            arrow.classList.add('active');
        }
    }

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
