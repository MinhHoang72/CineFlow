<%@ page import="jakarta.servlet.http.HttpServletResponse" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.entity.User,model.entity.Centre,java.util.List" %>

<%
    String authRole = (String) session.getAttribute("role");

    if (authRole == null && session.getAttribute("user") instanceof User) {
        authRole = ((User) session.getAttribute("user")).getRole();
    }

    if (!"MANAGER".equalsIgnoreCase(authRole)) {
        response.sendError(
            HttpServletResponse.SC_FORBIDDEN,
            "Chỉ Manager mới được truy cập trang quản lý nhân sự."
        );
        return;
    }

    if (request.getAttribute("staff") == null) {
        response.sendRedirect(request.getContextPath() + "/staffList");
        return;
    }

    List<User> staff = (List<User>) request.getAttribute("staff");
    Centre assignedCentre = (Centre) request.getAttribute("assignedCentre");
    Integer selectedCentreId = (Integer) request.getAttribute("selectedCentreId");

    String currentCentreName = assignedCentre != null
            ? assignedCentre.getCentreName()
            : "Chưa được bổ nhiệm trung tâm";

    String successParam = request.getParameter("success");
    String errorParam = request.getParameter("error");
    String searchParam = request.getParameter("search");

    java.util.Map<String, String> successMsgs = java.util.Map.of(
        "created", "Đã tạo tài khoản nhân viên thành công.",
        "updated", "Đã cập nhật thông tin nhân viên thành công.",
        "status_updated", "Đã cập nhật trạng thái nhân viên."
    );
    java.util.Map<String, String> errorMsgs = java.util.Map.ofEntries(
        java.util.Map.entry("account_exists", "Tên tài khoản đã tồn tại."),
        java.util.Map.entry("email_exists", "Email đã được sử dụng."),
        java.util.Map.entry("missing_fields", "Vui lòng điền đầy đủ thông tin bắt buộc."),
        java.util.Map.entry("invalid_name", "Họ và tên phải từ 2 đến 100 ký tự."),
        java.util.Map.entry("invalid_account", "Tên tài khoản không hợp lệ."),
        java.util.Map.entry("invalid_email", "Email không đúng định dạng."),
        java.util.Map.entry("disposable_email", "Không được sử dụng email tạm thời."),
        java.util.Map.entry("weak_password", "Mật khẩu tối thiểu 8 ký tự, có kí tự viết hoa, có kí tự đặc biệt."),
        java.util.Map.entry("invalid_phone", "Số điện thoại không hợp lệ (tối đa 20 ký tự)."),
        java.util.Map.entry("unassigned_manager", "Bạn chưa được bổ nhiệm trung tâm."),
        java.util.Map.entry("create_failed", "Không thể tạo tài khoản nhân viên.")
    );

    request.setAttribute("activeNav", "staff");
    request.setAttribute("pageTitle", "Quản lý nhân sự");
    request.setAttribute("pageBreadcrumb", "Nhân sự");

    int totalStaff = staff != null ? staff.size() : 0;
    int activeStaffCount = 0;
    int inactiveStaffCount = 0;
    int salesCount = 0;

    if (staff != null) {
        for (User u : staff) {
            if ("ACTIVE".equalsIgnoreCase(u.getStatus())) {
                activeStaffCount++;
            } else {
                inactiveStaffCount++;
            }

            if (u.isTicketSalesStaff()) {
                salesCount++;
            }
        }
    }
%>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <title>Quản lý nhân sự | CineFlow</title>

    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/reset.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/variables.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/buttons.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/badges.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/forms.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/tables.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/shell.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/staff.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/staff/directory.css">

    <script>
        document.addEventListener("DOMContentLoaded", function () {
            document.querySelectorAll(".cf-alert").forEach(function (alert) {
                setTimeout(function () {
                    alert.style.transition = "opacity 0.4s ease";
                    alert.style.opacity = "0";

                    setTimeout(function () {
                        alert.remove();
                    }, 400);
                }, 3000);
            });
        });
    </script>
</head>

<body>
<div class="cf-manager-shell">

    <jsp:include page="/WEB-INF/fragments/managerSidebar.jspf"/>

    <div class="cf-manager-main">

        <jsp:include page="/WEB-INF/fragments/managerTopbar.jspf"/>

        <main class="cf-content">

            <!-- TIÊU ĐỀ -->
            <div class="cf-page-header"
                 style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:16px;">

                <div>
                    <h1>Nhân sự - <%= currentCentreName %></h1>
                </div>

                <% if (selectedCentreId != null) { %>
                    <button type="button"
                            class="cf-btn cf-btn-primary"
                            onclick="openCreateStaffModal()">
                        <span style="font-weight:700;font-size:15px;margin-right:4px;">+</span>
                        Thêm nhân viên mới
                    </button>
                <% } %>

            </div>

            <!-- THÔNG BÁO -->
            <% if (successParam != null && successMsgs.containsKey(successParam)) { %>
                <div class="cf-alert cf-alert-success">
                    ✓ <strong>Thành công!</strong> <%= successMsgs.get(successParam) %>
                </div>
            <% } else if (errorParam != null) { %>
                <div class="cf-alert cf-alert-danger">
                    ⚠ <strong>Lỗi:</strong> <%= errorMsgs.getOrDefault(errorParam, "Thao tác không thành công. Vui lòng kiểm tra lại.") %>
                </div>
            <% } %>

            <!-- MANAGER CHƯA ĐƯỢC BỔ NHIỆM -->
            <% if (selectedCentreId == null) { %>

                <section class="cf-card"
                         style="padding:40px 24px;text-align:center;margin-top:20px;">

                    <div style="font-size:40px;margin-bottom:14px;">🏛</div>

                    <h2 style="color:#fbbf24;margin-bottom:8px;">
                        Tài khoản chưa được bổ nhiệm trung tâm
                    </h2>

                    <p style="color:#94a3b8;max-width:540px;margin:0 auto 16px;
                              font-size:14px;line-height:1.7;">
                        Tài khoản Manager hiện chưa được phân công phụ trách
                        trung tâm chiếu phim nào.
                    </p>

                    <p style="font-size:13px;color:#cbd5e1;">
                        Vui lòng liên hệ Quản trị viên để được phân công trung tâm.
                    </p>

                </section>

            <% } else { %>

                <!-- THỐNG KÊ -->
                <div class="cf-kpi-grid staff-kpi-grid">

                    <div class="cf-kpi-card">
                        <span class="cf-kpi-label">Tổng số nhân viên</span>
                        <div class="cf-kpi-value" id="kpiTotalStaff">
                            <%= totalStaff %>
                        </div>
                    </div>

                    <div class="cf-kpi-card">
                        <span class="cf-kpi-label">Đang hoạt động</span>
                        <div class="cf-kpi-value" id="kpiActiveStaff"
                             style="color:#10b981;">
                            <%= activeStaffCount %>
                        </div>
                    </div>

                    <div class="cf-kpi-card">
                        <span class="cf-kpi-label">Tạm ngưng / Đã khóa</span>
                        <div class="cf-kpi-value" id="kpiInactiveStaff"
                             style="color:#ef4444;">
                            <%= inactiveStaffCount %>
                        </div>
                    </div>

                    <div class="cf-kpi-card">
                        <span class="cf-kpi-label">Nhân viên Bán vé</span>
                        <div class="cf-kpi-value" style="color:#0284c7;">
                            <%= salesCount %>
                        </div>
                    </div>

                </div>

                <!-- DANH SÁCH NHÂN VIÊN -->
                <section class="cf-card" style="margin-top:20px;">

                    <div class="staff-card-header">

                        <div class="staff-heading">
                            <h2>Danh sách nhân viên</h2>
                            <p id="staffCountLabel">
                                <%= totalStaff %> tài khoản đang hiển thị.
                            </p>
                        </div>

                        <div class="staff-toolbar">

                            <div class="staff-select-wrap">
                                <select id="staffRoleFilter">
                                    <option value="">Tất cả vị trí</option>
                                    <option value="role-sales">🎫 Bán vé</option>
                                </select>
                            </div>

                            <div class="staff-select-wrap">
                                <select id="staffStatusFilter">
                                    <option value="ALL">Tất cả trạng thái</option>
                                    <option value="ACTIVE">Hoạt động</option>
                                    <option value="INACTIVE">Tạm ngưng</option>
                                    <option value="LOCKED">Đã khóa</option>
                                </select>
                            </div>

                            <form class="staff-search-form"
                                  id="staffSearchForm"
                                  method="get"
                                  action="${pageContext.request.contextPath}/staffList">

                                <div class="staff-search-box">
                                    <span class="staff-search-icon">⌕</span>

                                    <input type="search"
                                           name="search"
                                           id="staffFilterInput"
                                           value="<%= searchParam != null ? searchParam : "" %>"
                                           placeholder="Tìm mã, họ tên, tài khoản..."
                                           autocomplete="off">

                                    <button type="button"
                                            id="staffSearchClearBtn"
                                            class="staff-search-clear-btn"
                                            style="display:none;">
                                        ✕
                                    </button>
                                </div>

                                <% if (searchParam != null && !searchParam.trim().isEmpty()) { %>
                                    <a href="${pageContext.request.contextPath}/staffList"
                                       class="staff-control-btn">
                                        Xóa lọc
                                    </a>
                                <% } %>

                            </form>

                            <button type="submit"
                                    form="staffSearchForm"
                                    class="staff-control-btn">
                                Tìm
                            </button>

                            <div class="staff-page-size-wrap">
                                <span>Hiển thị:</span>

                                <div class="staff-select-wrap">
                                    <select id="staffPageSizeSelect"
                                            class="cf-select-sm">
                                        <option value="5">5 nhân viên</option>
                                        <option value="10" selected>10 nhân viên</option>
                                        <option value="20">20 nhân viên</option>
                                        <option value="50">50 nhân viên</option>
                                        <option value="ALL">Tất cả</option>
                                    </select>
                                </div>
                            </div>

                        </div>
                    </div>

                    <!-- BẢNG -->
                    <div class="staff-table-body">
                        <div class="cf-table-wrap">

                            <table class="cf-table">

                                <thead>
                                <tr>
                                    <th class="col-code">Mã NV</th>
                                    <th class="col-staff">Nhân viên</th>
                                    <th class="col-account">Tài khoản</th>
                                    <th class="col-role">Vị trí</th>
                                    <th class="col-phone">Số ĐT</th>
                                    <th class="col-centre">Trung tâm</th>
                                    <th class="col-status">Trạng thái</th>
                                    <th class="col-actions">Thao tác</th>
                                </tr>
                                </thead>

                                <tbody id="staffTableBody">

                                <tr id="staffTableEmptyRow" style="display:none;">
                                    <td colspan="8" class="cf-empty"
                                        style="text-align:center;padding:32px 16px;">
                                        Không tìm thấy nhân viên phù hợp.
                                    </td>
                                </tr>

                                <% if (staff.isEmpty()) { %>

                                    <tr>
                                        <td colspan="8" class="cf-empty">
                                            Trung tâm chưa có nhân viên.
                                        </td>
                                    </tr>

                                <% } else {
                                    for (User item : staff) {

                                        String st = item.getStatus() != null
                                                ? item.getStatus() : "INACTIVE";

                                        String staffName = item.getFullName() != null
                                                ? item.getFullName() : "";

                                        String staffEmail = item.getEmail() != null
                                                ? item.getEmail() : "";

                                        String staffAccount = item.getAccount() != null
                                                ? item.getAccount() : "";

                                        String staffPhone = item.getPhone() != null
                                                ? item.getPhone() : "";

                                        String employeeCode = item.getEmployeeCode() != null
                                                ? item.getEmployeeCode() : "";

                                        String staffRole = item.getRole() != null
                                                ? item.getRole() : "";

                                        boolean isSales = item.isTicketSalesStaff();
                                        String roleClass = isSales ? "role-sales" : "";

                                        String searchVal = (employeeCode + " "
                                                + item.getUserId() + " nv"
                                                + item.getUserId() + " "
                                                + staffName + " " + staffEmail + " "
                                                + staffAccount + " " + staffPhone + " "
                                                + staffRole).toLowerCase();

                                        String stClass = "ACTIVE".equalsIgnoreCase(st)
                                                ? "st-active"
                                                : ("INACTIVE".equalsIgnoreCase(st)
                                                    ? "st-inactive" : "st-locked");

                                        String itemCentreName = item.getCentreName() != null
                                                ? item.getCentreName()
                                                : currentCentreName;
                                %>

                                    <tr class="staff-row <%= stClass %> <%= roleClass %>"
                                        data-search="<%= searchVal %>"
                                        data-status="<%= st.toUpperCase() %>"
                                        data-role="<%= roleClass %>">

                                        <td class="col-code">
                                            <code class="cf-badge cf-badge-info">
                                                <%= employeeCode %>
                                            </code>
                                        </td>

                                        <td class="col-staff">
                                            <strong><%= staffName %></strong>
                                            <small><%= staffEmail %></small>
                                        </td>

                                        <td class="col-account">
                                            <code><%= staffAccount %></code>
                                        </td>

                                        <td class="col-role">
                                            <% if (isSales) { %>
                                                <span class="cf-badge"
                                                      style="background:rgba(56,189,248,.15);
                                                             color:#38bdf8;
                                                             border:1px solid rgba(56,189,248,.35);
                                                             white-space:nowrap;">
                                                    🎫 Bán vé
                                                </span>
                                            <% } else { %>
                                                <span class="cf-badge cf-badge-info" style="white-space:nowrap;">
                                                    Nhân viên
                                                </span>
                                            <% } %>
                                        </td>

                                        <td class="col-phone">
                                            <%= !staffPhone.isEmpty() ? staffPhone : "—" %>
                                        </td>

                                        <td class="col-centre">
                                            <span class="centre-name-tag">
                                                🏛 <%= itemCentreName %>
                                            </span>
                                        </td>

                                        <td class="col-status">

                                            <span id="badge-status-<%= item.getUserId() %>"
                                                  class="cf-badge
                                                  <%= "ACTIVE".equalsIgnoreCase(st)
                                                        ? "cf-badge-success"
                                                        : ("LOCKED".equalsIgnoreCase(st)
                                                            ? "cf-badge-danger"
                                                            : "cf-badge-warning") %>">

                                                <%= "ACTIVE".equalsIgnoreCase(st)
                                                        ? "Hoạt động"
                                                        : ("LOCKED".equalsIgnoreCase(st)
                                                            ? "Đã khóa" : "Tạm ngưng") %>
                                            </span>

                                        </td>

                                        <td class="col-actions">

                                            <div class="staff-actions">

                                                <button type="button"
                                                        class="staff-edit-btn"
                                                        onclick="openEditStaffModal(
                                                            <%= item.getUserId() %>,
                                                            '<%= employeeCode.replace("'", "\\'") %>',
                                                            '<%= staffName.replace("'", "\\'") %>',
                                                            '<%= staffPhone.replace("'", "\\'") %>',
                                                            '<%= staffEmail.replace("'", "\\'") %>',
                                                            '<%= staffRole.replace("'", "\\'") %>'
                                                        )">
                                                    Sửa
                                                </button>

                                                <div class="status-select-wrap">

                                                    <select class="cf-status-select val-<%= st.toLowerCase() %>"
                                                            data-userid="<%= item.getUserId() %>"
                                                            data-name="<%= staffName.replace("\"", "&quot;") %>"
                                                            data-previous-status="<%= st %>"
                                                            onchange="handleStaffStatusChange(this)">

                                                        <option value="ACTIVE"
                                                            <%= "ACTIVE".equalsIgnoreCase(st) ? "selected" : "" %>>
                                                            Hoạt động
                                                        </option>

                                                        <option value="INACTIVE"
                                                            <%= "INACTIVE".equalsIgnoreCase(st) ? "selected" : "" %>>
                                                            Tạm ngưng
                                                        </option>

                                                        <option value="LOCKED"
                                                            <%= "LOCKED".equalsIgnoreCase(st) ? "selected" : "" %>>
                                                            Khóa
                                                        </option>

                                                    </select>

                                                </div>
                                            </div>
                                        </td>

                                    </tr>

                                <%  }
                                   } %>

                                </tbody>
                            </table>
                        </div>
                    </div>

                    <!-- PHÂN TRANG -->
                    <div class="cf-pagination-bar">

                        <div class="cf-pagination-info"
                             id="staffPaginationInfo">
                            Đang nạp danh sách nhân viên...
                        </div>

                        <div class="cf-pagination-controls"
                             id="staffPaginationControls">
                        </div>

                    </div>

                </section>

            <% } %>

        </main>
    </div>
</div>

<!-- MODAL TẠO NHÂN VIÊN -->
<div class="cf-modal-backdrop" id="createStaffModal">

    <div class="cf-modal" role="dialog" aria-modal="true">

        <div class="cf-modal-header">
            <h3>Thêm nhân viên mới tại <%= currentCentreName %></h3>

            <button type="button"
                    class="cf-modal-close"
                    onclick="closeCreateStaffModal()">
                &times;
            </button>
        </div>

        <form method="post"
              action="${pageContext.request.contextPath}/staffList">

            <input type="hidden" name="action" value="create">

            <div class="cf-modal-body">

                <p style="font-size:13px;color:#94a3b8;margin-bottom:12px;">
                    Nhân viên mới sẽ trực thuộc trung tâm:
                    <strong style="color:#f8fafc;">
                        <%= currentCentreName %>
                    </strong>
                </p>

                <div class="cf-form-group">
                    <label for="createStaffRole">Vị trí công việc *</label>

                    <select id="createStaffRole" name="role" required>
                        <option value="TICKET_SALES_STAFF">
                            🎫 Nhân viên bán vé & F&B
                        </option>
                    </select>
                </div>

                <div class="cf-form-group">
                    <label for="createStaffFullName">Họ và tên *</label>

                    <input type="text"
                           id="createStaffFullName"
                           name="fullName"
                           placeholder="Ví dụ: Nguyễn Văn B"
                           required>
                </div>

                <div style="display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px;">

                    <div class="cf-form-group">
                        <label for="createStaffAccount">Tên đăng nhập *</label>

                        <input type="text"
                               id="createStaffAccount"
                               name="account"
                               maxlength="50"
                               required>
                    </div>

                    <div class="cf-form-group">
                        <label for="createStaffPhone">Số điện thoại</label>

                        <input type="tel"
                               id="createStaffPhone"
                               name="phone">
                    </div>

                </div>

                <div class="cf-form-group">
                    <label for="createStaffEmail">Email *</label>

                    <input type="email"
                           id="createStaffEmail"
                           name="email"
                           required>
                </div>

                <div class="cf-form-group">
                    <label for="createStaffPassword">Mật khẩu ban đầu *</label>

                    <input type="text"
                           id="createStaffPassword"
                           name="password"
                           value="Staff@123"
                           required>

                    <small style="color:#94a3b8;font-size:12px;">
                        Mật khẩu tối thiểu 8 ký tự, có kí tự viết hoa, có kí tự đặc biệt.
                    </small>
                </div>

            </div>

            <div class="cf-modal-footer">
                <button type="button"
                        class="cf-btn cf-btn-secondary"
                        onclick="closeCreateStaffModal()">
                    Hủy
                </button>

                <button type="submit" class="cf-btn cf-btn-primary">
                    Tạo tài khoản
                </button>
            </div>

        </form>
    </div>
</div>

<!-- MODAL SỬA NHÂN VIÊN -->
<div class="cf-modal-backdrop" id="editStaffModal">

    <div class="cf-modal" role="dialog" aria-modal="true">

        <div class="cf-modal-header">
            <h3>Chỉnh sửa thông tin nhân viên</h3>

            <button type="button"
                    class="cf-modal-close"
                    onclick="closeEditStaffModal()">
                &times;
            </button>
        </div>

        <form method="post"
              action="${pageContext.request.contextPath}/staffList">

            <input type="hidden" name="action" value="update">
            <input type="hidden" name="userId" id="editStaffUserId">

            <div class="cf-modal-body">

                <div class="cf-form-group">
                    <label>Mã nhân viên</label>
                    <code class="cf-badge cf-badge-info" id="editStaffCode">
                        NV00000
                    </code>
                </div>

                <div class="cf-form-group">
                    <label for="editStaffRole">Vị trí công việc *</label>

                    <select name="role" id="editStaffRole" required>
                        <option value="TICKET_SALES_STAFF">🎫 Nhân viên bán vé</option>
                    </select>
                </div>

                <div class="cf-form-group">
                    <label for="editStaffFullName">Họ và tên *</label>

                    <input type="text"
                           name="fullName"
                           id="editStaffFullName"
                           required>
                </div>

                <div class="cf-form-group">
                    <label for="editStaffPhone">Số điện thoại</label>

                    <input type="tel"
                           name="phone"
                           id="editStaffPhone">
                </div>

                <div class="cf-form-group">
                    <label for="editStaffEmail">Email *</label>

                    <input type="email"
                           name="email"
                           id="editStaffEmail"
                           required>
                </div>

            </div>

            <div class="cf-modal-footer">

                <button type="button"
                        class="cf-btn cf-btn-secondary"
                        onclick="closeEditStaffModal()">
                    Hủy
                </button>

                <button type="submit" class="cf-btn cf-btn-primary">
                    Lưu thay đổi
                </button>

            </div>

        </form>
    </div>
</div>

<div id="cf-toast-container" aria-live="polite"></div>

<script src="${pageContext.request.contextPath}/js/admin-pagination.js?v=<%= System.currentTimeMillis() %>"></script>
<script src="${pageContext.request.contextPath}/js/staff-management.js?v=<%= System.currentTimeMillis() %>"></script>
</body>
</html>
