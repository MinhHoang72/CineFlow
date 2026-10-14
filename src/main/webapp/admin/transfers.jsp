<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.User,model.Branch,model.StaffTransfer,model.ManagerAssignment,java.util.List,java.text.SimpleDateFormat" %>
<%
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && session.getAttribute("user") instanceof model.User) {
        authRole = ((model.User) session.getAttribute("user")).getRole();
    }
    if (!"ADMIN".equalsIgnoreCase(authRole)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Quản trị viên (ADMIN) mới có quyền truy cập trang này.");
        return;
    }

    List<Branch> branchesWithManager = (List<Branch>) request.getAttribute("branchesWithManager");
    List<Branch> activeBranches = (List<Branch>) request.getAttribute("activeBranches");
    List<User> managers = (List<User>) request.getAttribute("managers");
    List<ManagerAssignment> managerAssignments = (List<ManagerAssignment>) request.getAttribute("managerAssignments");

    List<User> staffList = (List<User>) request.getAttribute("staffList");
    List<StaffTransfer> transferHistory = (List<StaffTransfer>) request.getAttribute("transferHistory");
    Integer selectedBranchId = (Integer) request.getAttribute("selectedBranchId");

    request.setAttribute("activeNav", "transfers");
    request.setAttribute("pageTitle", "Bổ nhiệm & Điều chuyển");
    request.setAttribute("pageBreadcrumb", "Hệ thống");

    String successParam = request.getParameter("success");
    String errorParam = request.getParameter("error");
    String activeTabParam = request.getParameter("tab");
    if (activeTabParam == null) activeTabParam = "manager";

    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy");
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
    <style>
        .cf-alert {
            padding: 12px 18px;
            border-radius: 8px;
            margin-bottom: 20px;
            display: flex;
            align-items: center;
            gap: 10px;
            font-size: 14px;
        }
        .cf-alert-success {
            background: rgba(34, 197, 94, 0.15);
            border: 1px solid rgba(34, 197, 94, 0.4);
            color: #4ade80;
        }
        .cf-alert-danger {
            background: rgba(239, 68, 68, 0.15);
            border: 1px solid rgba(239, 68, 68, 0.4);
            color: #f87171;
        }
        .tab-nav {
            display: flex;
            gap: 12px;
            margin-bottom: 22px;
            border-bottom: 1px solid rgba(255, 255, 255, 0.08);
            padding-bottom: 10px;
        }
        .tab-btn {
            background: none;
            border: none;
            color: #94a3b8;
            font-size: 15px;
            font-weight: 600;
            padding: 9px 18px;
            border-radius: 8px;
            cursor: pointer;
            transition: all 0.2s;
        }
        .tab-btn:hover { color: #fff; background: rgba(255,255,255,0.05); }
        .tab-btn.active {
            color: #fff;
            background: #FF2855;
            border: 1px solid #FF2855;
        }
        .tab-pane { display: none; }
        .tab-pane.active { display: block; }
        .cf-modal-backdrop {
            display: none;
            position: fixed;
            inset: 0;
            background: rgba(0, 0, 0, 0.75);
            backdrop-filter: blur(5px);
            z-index: 9999;
            align-items: center;
            justify-content: center;
            padding: 20px;
        }
        .cf-modal-backdrop.active { display: flex; }
        .cf-modal {
            background: #ffffff;
            border: 1px solid #E2E8F0;
            border-radius: 14px;
            width: 100%;
            max-width: 560px;
            box-shadow: 0 24px 50px rgba(0, 0, 0, 0.35);
            overflow: hidden;
            animation: modalFadeIn 0.2s ease-out;
        }
        @keyframes modalFadeIn {
            from { opacity: 0; transform: translateY(-16px) scale(0.98); }
            to { opacity: 1; transform: translateY(0) scale(1); }
        }
        .cf-modal-header {
            padding: 20px 24px;
            border-bottom: 1px solid #E2E8F0;
            display: flex;
            justify-content: space-between;
            align-items: center;
            background: #ffffff;
        }
        .cf-modal-header h3 { margin: 0; font-size: 18px; color: #111827; font-weight: 700; }
        .cf-modal-close {
            background: none; border: none; color: #64748B; font-size: 24px; cursor: pointer; line-height: 1;
        }
        .cf-modal-close:hover { color: #111827; }
        .cf-modal-body {
            padding: 24px;
            display: flex;
            flex-direction: column;
            gap: 16px;
            background: #ffffff;
            color: #1E293B;
        }
        .cf-modal-footer {
            padding: 16px 24px;
            border-top: 1px solid #E2E8F0;
            display: flex;
            justify-content: flex-end;
            gap: 12px;
            background: #F8FAFC;
        }
        .cf-form-group {
            display: flex;
            flex-direction: column;
            gap: 6px;
        }
        .cf-form-group label {
            font-size: 13px;
            font-weight: 600;
            color: #111827;
        }
        .cf-form-group input, .cf-form-group select, .cf-form-group textarea {
            background: #F1F5F9;
            border: 1px solid #CBD5E1;
            border-radius: 8px;
            padding: 9px 14px;
            color: #1E293B;
            font-size: 14px;
            outline: none;
            transition: all 0.2s;
        }
        .cf-form-group input::placeholder, .cf-form-group textarea::placeholder {
            color: #64748B;
            opacity: 1;
        }
        .cf-form-group select option {
            background: #ffffff;
            color: #1E293B;
        }
        .cf-form-group input:focus, .cf-form-group select:focus, .cf-form-group textarea:focus {
            border-color: #FF2855;
            background: #ffffff;
            box-shadow: 0 0 0 3px rgba(255, 40, 85, 0.2);
        }
        .info-strip {
            background: #F8FAFC;
            border: 1px solid #E2E8F0;
            border-radius: 8px;
            padding: 12px 16px;
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 8px;
            font-size: 13px;
        }
        .info-strip div span { color: #64748B; display: block; font-size: 11px; margin-bottom: 2px; }
        .info-strip div strong { color: #111827; }
    </style>
</head>
<body>
<div class="cf-manager-shell">
    <jsp:include page="/admin/sidebar.jsp" />

    <div class="cf-manager-main">
        <jsp:include page="/admin/topbar.jsp" />

        <main class="cf-content">
            <div class="cf-page-header">
                <div>
                    <h1>Bổ nhiệm & Điều chuyển Nhân sự</h1>
                    <p>Bổ nhiệm Manager phụ trách cụm rạp, luân chuyển nhân sự giữa các chi nhánh và theo dõi lịch sử phân công.</p>
                </div>
            </div>

            <% if ("manager_assigned".equals(successParam)) { %>
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> <strong>Thành công!</strong> Đã bổ nhiệm Quản lý phụ trách chi nhánh và ghi nhận vào lịch sử phân công.
                </div>
            <% } else if ("manager_unassigned".equals(successParam)) { %>
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> Đã hoàn tất miễn nhiệm Quản lý khỏi chi nhánh.
                </div>
            <% } else if ("transferred".equals(successParam)) { %>
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> <strong>Thành công!</strong> Đã điều chuyển nhân viên sang chi nhánh mới và cập nhật lịch sử.
                </div>
            <% } else if ("same_branch".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Chi nhánh đích trùng với chi nhánh hiện tại của nhân sự.
                </div>
            <% } else if ("missing_reason".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> Vui lòng nhập lý do điều chuyển nhân sự để phục vụ lưu vết kiểm toán.
                </div>
            <% } else if ("manager_inactive".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Tài khoản Quản lý đang ở trạng thái Tạm ngưng, không thể bổ nhiệm vào chi nhánh.
                </div>
            <% } else if ("not_manager".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Tài khoản được chọn không phải là vai trò Manager.
                </div>
            <% } else if (errorParam != null) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> Thao tác thất bại. Vui lòng kiểm tra lại thông tin.
                </div>
            <% } %>

            <!-- TAB CONTROLS -->
            <div class="tab-nav">
                <button type="button" class="tab-btn <%= "manager".equals(activeTabParam) ? "active" : "" %>" onclick="switchMainTab('tab-manager-assignment')">
                    🏛 Bổ nhiệm Quản lý chi nhánh
                </button>
                <button type="button" class="tab-btn <%= "staff".equals(activeTabParam) ? "active" : "" %>" onclick="switchMainTab('tab-staff-transfer')">
                    👥 Điều chuyển nhân viên
                </button>
            </div>

            <!-- ==================================================================== -->
            <!-- TAB 1: BỔ NHIỆM MANAGER CHI NHÁNH -->
            <!-- ==================================================================== -->
            <div class="tab-pane <%= "manager".equals(activeTabParam) ? "active" : "" %>" id="tab-manager-assignment">
                <section class="cf-card">
                    <div class="cf-card-header">
                        <div>
                            <h2>Phân công Quản lý theo Chi nhánh</h2>
                            <p>Bổ nhiệm Manager mới, thay thế hoặc miễn nhiệm Manager phụ trách tại từng cụm rạp.</p>
                        </div>
                    </div>
                    <div class="cf-card-body">
                        <div class="cf-table-wrap">
                            <table class="cf-table">
                                <thead>
                                    <tr>
                                        <th>Cụm rạp</th>
                                        <th>Khu vực</th>
                                        <th>Mã nhân sự</th>
                                        <th>Quản lý đương nhiệm</th>
                                        <th>Trạng thái rạp</th>
                                        <th>Thao tác bổ nhiệm</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <% if (branchesWithManager == null || branchesWithManager.isEmpty()) { %>
                                        <tr><td colspan="6" class="cf-empty">Chưa có chi nhánh nào trong hệ thống.</td></tr>
                                    <% } else for (Branch b : branchesWithManager) {
                                        boolean hasManager = b.getManagerId() != null && b.getManagerId() > 0;
                                    %>
                                        <tr>
                                            <td>
                                                <strong style="color: #fff; font-size: 14px;"><%= b.getBranchName() %></strong>
                                                <small style="display: block; color: #94a3b8;"><%= b.getAddress() %></small>
                                            </td>
                                            <td><span class="cf-badge cf-badge-info"><%= b.getCity() %></span></td>
                                            <td>
                                                <% if (hasManager) { %>
                                                    <code class="cf-badge cf-badge-primary" style="font-size: 12px; font-weight: 700; letter-spacing: 0.5px;"><%= String.format("QL%05d", b.getManagerId()) %></code>
                                                <% } else { %>
                                                    <span style="color: #64748b;">—</span>
                                                <% } %>
                                            </td>
                                            <td>
                                                <% if (hasManager) { %>
                                                    <strong style="color: #818cf8; font-size: 14px;"><%= b.getManagerName() %></strong>
                                                    <small style="display: block; color: #94a3b8;"><%= b.getManagerEmail() %></small>
                                                <% } else { %>
                                                    <span class="cf-badge cf-badge-warning">Chưa có Quản lý</span>
                                                <% } %>
                                            </td>
                                            <td>
                                                <span class="cf-badge <%= "ACTIVE".equalsIgnoreCase(b.getStatus()) ? "cf-badge-success" : "cf-badge-danger" %>">
                                                    <%= b.getStatus() %>
                                                </span>
                                            </td>
                                            <td>
                                                <div style="display: flex; gap: 8px;">
                                                    <button type="button" class="cf-btn cf-btn-primary cf-btn-sm" 
                                                            onclick="openAssignManagerModal(<%= b.getBranchId() %>, '<%= b.getBranchName().replace("'", "\\'") %>', <%= hasManager ? b.getManagerId() : "null" %>, '<%= (hasManager ? b.getManagerName() : "Chưa có").replace("'", "\\'") %>')">
                                                        <%= hasManager ? "🔄 Thay thế" : "➕ Bổ nhiệm" %>
                                                    </button>
                                                    <% if (hasManager) { %>
                                                        <button type="button" class="cf-btn cf-btn-secondary cf-btn-sm" style="color: #f87171;"
                                                                onclick="openUnassignManagerModal(<%= b.getBranchId() %>, '<%= b.getBranchName().replace("'", "\\'") %>', <%= b.getManagerId() %>, '<%= b.getManagerName().replace("'", "\\'") %>')">
                                                            Miễn nhiệm
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

                <!-- LỊCH SỬ BỔ NHIỆM MANAGER (BRANCH MANAGER ASSIGNMENTS) -->
                <section class="cf-card" style="margin-top: 24px;">
                    <div class="cf-card-header">
                        <div>
                            <h2>Lịch sử Bổ nhiệm Quản lý (Assignment History)</h2>
                            <p>Theo dõi ai phụ trách chi nhánh nào và trong khoảng thời gian nào (Quy ước: <em>Đến ngày = Đang đương nhiệm</em> nếu đang còn hiệu lực).</p>
                        </div>
                    </div>
                    <div class="cf-card-body">
                        <div class="cf-table-wrap">
                            <table class="cf-table">
                                <thead>
                                    <tr>
                                        <th>Mã #</th>
                                        <th>Cụm rạp</th>
                                        <th>Mã nhân sự</th>
                                        <th>Quản lý (Manager)</th>
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
                                            <td><strong>#<%= ma.getAssignmentId() %></strong></td>
                                            <td><strong><%= ma.getBranchName() %></strong> <small style="color: #94a3b8;">(<%= ma.getBranchCity() %>)</small></td>
                                            <td><code class="cf-badge cf-badge-primary" style="font-size: 11px; font-weight: 700;"><%= ma.getManagerCode() %></code></td>
                                            <td>
                                                <strong style="color: #fff;"><%= ma.getManagerName() %></strong>
                                                <small style="display: block; color: #94a3b8;">@<%= ma.getManagerAccount() %> - <%= ma.getManagerEmail() %></small>
                                            </td>
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
                            <h2>Danh sách Nhân viên chi nhánh</h2>
                            <p>Điều chuyển nhân viên tác nghiệp giữa các cụm rạp. Tìm kiếm nhanh theo <strong>Mã số nhân sự (Mã NS)</strong> để tránh nhầm lẫn khi trùng tên trong hệ thống.</p>
                        </div>
                        <div style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap;">
                            <form method="get" action="${pageContext.request.contextPath}/admin/transfers" style="display: flex; gap: 8px; align-items: center;">
                                <input type="hidden" name="tab" value="staff">
                                <input type="text" name="search" id="staffFilterInput" 
                                       value="<%= request.getAttribute("searchParam") != null ? request.getAttribute("searchParam") : "" %>"
                                       placeholder="🔍 Tìm theo Mã nhân sự (VD: NV00012, 12), tài khoản, họ tên..." 
                                       style="padding: 8px 14px; border: 1px solid #334155; border-radius: 6px; font-size: 13px; min-width: 320px; background: #1e293b; color: #f8fafc;">
                                <button type="submit" class="cf-btn cf-btn-secondary cf-btn-sm">Tìm kiếm</button>
                                <% if (request.getAttribute("searchParam") != null && !((String)request.getAttribute("searchParam")).trim().isEmpty()) { %>
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
                                        <th>Nhân viên</th>
                                        <th>Tên đăng nhập</th>
                                        <th>Vai trò</th>
                                        <th>Chi nhánh hiện tại</th>
                                        <th>Trạng thái</th>
                                        <th>Thao tác</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <% if (staffList == null || staffList.isEmpty()) { %>
                                        <tr><td colspan="7" class="cf-empty">Không tìm thấy nhân viên nào phù hợp với Mã nhân sự hoặc từ khóa tìm kiếm.</td></tr>
                                    <% } else for (User item : staffList) {
                                        String st = item.getStatus();
                                        String branchDisplay = item.getBranchName() != null ? item.getBranchName() : "Chưa phân rạp";
                                        String searchData = (item.getEmployeeCode() + " " + item.getUserId() + " nv" + item.getUserId() + " " + item.getFullName() + " " + item.getEmail() + " " + item.getAccount() + " " + branchDisplay).toLowerCase();
                                    %>
                                        <tr class="staff-row" data-search="<%= searchData %>">
                                            <td><code class="cf-badge cf-badge-info" style="font-size: 12px; font-weight: 700; letter-spacing: 0.5px;"><%= item.getEmployeeCode() %></code></td>
                                            <td>
                                                <strong style="color: #fff;"><%= item.getFullName() %></strong>
                                                <small style="display: block; color: #94a3b8;"><%= item.getEmail() %></small>
                                            </td>
                                            <td><code><%= item.getAccount() %></code></td>
                                            <td>
                                                <span class="cf-badge <%= "MANAGER".equalsIgnoreCase(item.getRole()) ? "cf-badge-primary" : "cf-badge-info" %>">
                                                    <%= item.getRole() %>
                                                </span>
                                            </td>
                                            <td>
                                                <% if (item.getBranchId() != null && item.getBranchId() > 0) { %>
                                                    <span style="font-weight: 500; color: #e2e8f0;">🏛 <%= item.getBranchName() %></span>
                                                <% } else { %>
                                                    <span class="cf-badge cf-badge-warning">Chưa phân cụm rạp</span>
                                                <% } %>
                                            </td>
                                            <td>
                                                <span class="cf-badge <%= "ACTIVE".equalsIgnoreCase(st) ? "cf-badge-success" : "LOCKED".equalsIgnoreCase(st) ? "cf-badge-danger" : "cf-badge-warning" %>">
                                                    <%= "ACTIVE".equalsIgnoreCase(st) ? "Hoạt động" : "LOCKED".equalsIgnoreCase(st) ? "Đã khóa" : "Tạm ngưng" %>
                                                </span>
                                            </td>
                                            <td>
                                                <button type="button" class="cf-btn cf-btn-primary cf-btn-sm" 
                                                        onclick="openStaffTransferModal(<%= item.getUserId() %>, '<%= item.getEmployeeCode() %>', '<%= item.getFullName().replace("'", "\\'") %>', '<%= item.getAccount() %>', '<%= item.getRole() %>', <%= item.getBranchId() != null ? item.getBranchId() : "null" %>, '<%= (item.getBranchName() != null ? item.getBranchName() : "Chưa phân rạp").replace("'", "\\'") %>')">
                                                    🔄 Điều chuyển
                                                </button>
                                            </td>
                                        </tr>
                                    <% } %>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </section>

                <!-- LỊCH SỬ ĐIỀU CHUYỂN STAFF -->
                <section class="cf-card" style="margin-top: 24px;">
                    <div class="cf-card-header">
                        <div>
                            <h2>Lịch sử điều chuyển nhân viên (Staff Transfer Audit Trail)</h2>
                            <p>Toàn bộ vết điều chuyển nhân viên từ rạp này sang rạp khác. Lưu vết rõ ràng theo Mã số nhân sự để đối soát.</p>
                        </div>
                    </div>
                    <div class="cf-card-body">
                        <div class="cf-table-wrap">
                            <table class="cf-table">
                                <thead>
                                    <tr>
                                        <th>Mã #</th>
                                        <th>Thời gian</th>
                                        <th>Mã nhân sự</th>
                                        <th>Nhân viên</th>
                                        <th>Từ chi nhánh</th>
                                        <th>Đến chi nhánh</th>
                                        <th>Người thực hiện (Admin)</th>
                                        <th>Lý do điều chuyển</th>
                                        <th>Ghi chú</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <% if (transferHistory == null || transferHistory.isEmpty()) { %>
                                        <tr><td colspan="9" class="cf-empty">Chưa có lịch sử điều chuyển nào được ghi nhận.</td></tr>
                                    <% } else for (StaffTransfer t : transferHistory) { %>
                                        <tr>
                                            <td><strong>#<%= t.getTransferId() %></strong></td>
                                            <td><%= t.getTransferDate() != null ? sdf.format(t.getTransferDate()) : "—" %></td>
                                            <td><code class="cf-badge cf-badge-info" style="font-size: 11px; font-weight: 700;"><%= t.getEmployeeCode() %></code></td>
                                            <td>
                                                <strong style="color: #fff;"><%= t.getUserName() %></strong>
                                                <small style="display: block; color: #94a3b8;">@<%= t.getUserAccount() %> - <%= t.getUserEmail() %> (<%= t.getUserRole() %>)</small>
                                            </td>
                                            <td><%= t.getFromBranchName() != null ? t.getFromBranchName() : "Chưa phân rạp" %></td>
                                            <td><strong style="color: #4ade80;"><%= t.getToBranchName() %></strong></td>
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

<!-- ==================== MODAL BỔ NHIỆM / THAY THẾ MANAGER ==================== -->
<div class="cf-modal-backdrop" id="assignManagerModal">
    <div class="cf-modal">
        <div class="cf-modal-header">
            <h3>Bổ nhiệm Quản lý chi nhánh</h3>
            <button type="button" class="cf-modal-close" onclick="closeAssignManagerModal()">&times;</button>
        </div>
        <form method="post" action="${pageContext.request.contextPath}/admin/transfers">
            <input type="hidden" name="action" value="assign_manager">
            <input type="hidden" name="branchId" id="assignBranchId">
            <div class="cf-modal-body">
                <div class="info-strip">
                    <div>
                        <span>Chi nhánh tiếp nhận:</span>
                        <strong id="assignBranchName">—</strong>
                    </div>
                    <div>
                        <span>Quản lý hiện tại:</span>
                        <strong id="assignCurrentManagerName" style="color: #d97706;">—</strong>
                    </div>
                </div>

                <div class="cf-form-group">
                    <label>Tìm nhanh Quản lý (theo Mã QL hoặc Tên)</label>
                    <input type="text" id="assignManagerFilterInput" placeholder="🔍 Nhập Mã QL (VD: QL00003, 3) hoặc Tên Manager..." oninput="filterManagerDropdown(this.value)">
                </div>

                <div class="cf-form-group">
                    <label>Chọn Quản lý được bổ nhiệm *</label>
                    <select name="managerId" id="assignManagerSelect" required>
                        <option value="">-- Chọn Manager --</option>
                        <% if (managers != null) for (User m : managers) { 
                            if (!"ACTIVE".equalsIgnoreCase(m.getStatus())) continue;
                            String mSearch = (m.getEmployeeCode() + " " + m.getUserId() + " ql" + m.getUserId() + " " + m.getFullName() + " " + m.getAccount()).toLowerCase();
                        %>
                            <option value="<%= m.getUserId() %>" data-search="<%= mSearch %>">
                                [<%= m.getEmployeeCode() %>] <%= m.getFullName() %> (@<%= m.getAccount() %>) - <%= m.getBranchName() != null ? "Hiện ở: " + m.getBranchName() : "Chưa có rạp" %>
                            </option>
                        <% } %>
                    </select>
                    <small style="color: #64748B; font-size: 12px;">Chỉ hiển thị các tài khoản Manager đang hoạt động (ACTIVE). Tìm theo Mã QL để tránh nhầm lẫn khi trùng tên.</small>
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

<!-- ==================== MODAL MIỄN NHIỆM MANAGER ==================== -->
<div class="cf-modal-backdrop" id="unassignManagerModal">
    <div class="cf-modal">
        <div class="cf-modal-header">
            <h3>Miễn nhiệm Quản lý chi nhánh</h3>
            <button type="button" class="cf-modal-close" onclick="closeUnassignManagerModal()">&times;</button>
        </div>
        <form method="post" action="${pageContext.request.contextPath}/admin/transfers">
            <input type="hidden" name="action" value="unassign_manager">
            <input type="hidden" name="branchId" id="unassignBranchId">
            <input type="hidden" name="managerId" id="unassignManagerId">
            <div class="cf-modal-body">
                <div class="info-strip">
                    <div>
                        <span>Chi nhánh:</span>
                        <strong id="unassignBranchName">—</strong>
                    </div>
                    <div>
                        <span>Quản lý được miễn nhiệm:</span>
                        <strong id="unassignManagerName" style="color: #dc2626;">—</strong>
                    </div>
                </div>
                <div class="cf-form-group">
                    <label>Lý do miễn nhiệm / Kết thúc nhiệm kỳ *</label>
                    <input type="text" name="reason" placeholder="Ví dụ: Hết thời hạn phân công, chuyển công tác khác..." required>
                </div>
            </div>
            <div class="cf-modal-footer">
                <button type="button" class="cf-btn cf-btn-secondary" onclick="closeUnassignManagerModal()">Hủy</button>
                <button type="submit" class="cf-btn cf-btn-primary" style="background: #ef4444; border-color: #ef4444;">Xác nhận miễn nhiệm</button>
            </div>
        </form>
    </div>
</div>

<!-- ==================== MODAL ĐIỀU CHUYỂN STAFF ==================== -->
<div class="cf-modal-backdrop" id="staffTransferModal">
    <div class="cf-modal">
        <div class="cf-modal-header">
            <h3>Điều chuyển nhân sự chi nhánh</h3>
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
                        <span>Tài khoản:</span>
                        <strong id="transferStaffAccount" style="color: #64748B;">—</strong>
                    </div>
                    <div>
                        <span>Cụm rạp hiện tại:</span>
                        <strong id="transferStaffCurrentBranch" style="color: #d97706;">—</strong>
                    </div>
                </div>

                <div class="cf-form-group">
                    <label>Chi nhánh chuyển đến *</label>
                    <select name="toBranchId" id="toBranchSelect" required>
                        <option value="">-- Chọn cụm rạp đích --</option>
                        <% if (activeBranches != null) for (Branch b : activeBranches) { %>
                            <option value="<%= b.getBranchId() %>">
                                <%= b.getBranchName() %> (<%= b.getCity() %>)
                            </option>
                        <% } %>
                    </select>
                </div>

                <div class="cf-form-group">
                    <label>Lý do điều chuyển (Bắt buộc) *</label>
                    <input type="text" name="reason" placeholder="Ví dụ: Tăng cường nhân sự mở rạp mới / Luân chuyển định kỳ" required>
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
    function openAssignManagerModal(branchId, branchName, currentManagerId, currentManagerName) {
        document.getElementById('assignBranchId').value = branchId;
        document.getElementById('assignBranchName').textContent = branchName;
        document.getElementById('assignCurrentManagerName').textContent = currentManagerName;
        var filterInput = document.getElementById('assignManagerFilterInput');
        if (filterInput) {
            filterInput.value = '';
            filterManagerDropdown('');
        }
        document.getElementById('assignManagerModal').classList.add('active');
    }
    function closeAssignManagerModal() {
        document.getElementById('assignManagerModal').classList.remove('active');
    }

    function filterManagerDropdown(q) {
        q = (q || '').toLowerCase().trim();
        var sel = document.getElementById('assignManagerSelect');
        if (!sel) return;
        for (var i = 0; i < sel.options.length; i++) {
            var opt = sel.options[i];
            if (!opt.value) continue;
            var s = opt.getAttribute('data-search') || '';
            if (!q || s.indexOf(q) !== -1) {
                opt.style.display = '';
            } else {
                opt.style.display = 'none';
            }
        }
    }

    // Modal Miễn nhiệm Manager
    function openUnassignManagerModal(branchId, branchName, managerId, managerName) {
        document.getElementById('unassignBranchId').value = branchId;
        document.getElementById('unassignBranchName').textContent = branchName;
        document.getElementById('unassignManagerId').value = managerId;
        document.getElementById('unassignManagerName').textContent = managerName;
        document.getElementById('unassignManagerModal').classList.add('active');
    }
    function closeUnassignManagerModal() {
        document.getElementById('unassignManagerModal').classList.remove('active');
    }

    // Modal Staff Transfer
    function openStaffTransferModal(userId, empCode, name, account, role, currentBranchId, currentBranchName) {
        document.getElementById('transferStaffUserId').value = userId;
        document.getElementById('transferStaffEmployeeCode').textContent = empCode;
        document.getElementById('transferStaffUserName').textContent = name;
        document.getElementById('transferStaffAccount').textContent = '@' + account;
        document.getElementById('transferStaffCurrentBranch').textContent = currentBranchName;

        var select = document.getElementById('toBranchSelect');
        select.value = "";
        for (var i = 0; i < select.options.length; i++) {
            var opt = select.options[i];
            if (opt.value && currentBranchId !== null && parseInt(opt.value) === currentBranchId) {
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

    // Backdrop click close
    document.querySelectorAll('.cf-modal-backdrop').forEach(function(backdrop) {
        backdrop.addEventListener('click', function(e) {
            if (e.target === this) this.classList.remove('active');
        });
    });

    // Staff filter search (Instant client-side filter for rendered rows)
    document.addEventListener('DOMContentLoaded', function() {
        var input = document.getElementById('staffFilterInput');
        var rows = document.querySelectorAll('.staff-row');
        if (input && rows.length > 0) {
            input.addEventListener('input', function() {
                var q = this.value.toLowerCase().trim();
                rows.forEach(function(r) {
                    var s = r.getAttribute('data-search') || '';
                    if (!q || s.indexOf(q) !== -1) r.style.display = '';
                    else r.style.display = 'none';
                });
            });
        }
    });
</script>
</body>
</html>
