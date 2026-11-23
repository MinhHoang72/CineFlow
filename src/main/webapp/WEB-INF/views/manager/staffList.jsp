<%@ page import="jakarta.servlet.http.HttpServletResponse" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page import="model.entity.User,model.entity.Centre,java.util.List" %>

<%
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

    String searchParam = request.getParameter("search");

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
                    <a href="${pageContext.request.contextPath}/staffList?action=create"
                       class="cf-btn cf-btn-primary">
                        <span style="font-weight:700;font-size:15px;margin-right:4px;">+</span>
                        Thêm nhân viên mới
                    </a>
                <% } %>

            </div>

            <!-- THÔNG BÁO -->
            <c:choose>
                <c:when test="${param.success == 'created'}">
                    <div class="cf-alert cf-alert-success">✓ <strong>Thành công!</strong> Đã tạo tài khoản nhân viên thành công.</div>
                </c:when>
                <c:when test="${param.success == 'updated'}">
                    <div class="cf-alert cf-alert-success">✓ <strong>Thành công!</strong> Đã cập nhật thông tin nhân viên thành công.</div>
                </c:when>
                <c:when test="${param.success == 'status_updated'}">
                    <div class="cf-alert cf-alert-success">✓ <strong>Thành công!</strong> Đã cập nhật trạng thái nhân viên thành công.</div>
                </c:when>
                <c:when test="${not empty param.error}">
                    <div class="cf-alert cf-alert-danger">⚠ <strong>Lỗi:</strong> Thao tác không thành công hoặc dữ liệu không hợp lệ. Vui lòng kiểm tra lại.</div>
                </c:when>
            </c:choose>

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
                                                + item.getUserId() + " bv"
                                                + item.getUserId() + " nv"
                                                + item.getUserId() + " "
                                                + staffName + " " + staffEmail + " "
                                                + staffAccount + " " + staffPhone + " bán vé").toLowerCase();

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
                                            <span class="cf-badge"
                                                  style="background:rgba(56,189,248,.15);
                                                         color:#38bdf8;
                                                         border:1px solid rgba(56,189,248,.35);
                                                         white-space:nowrap;">
                                                🎫 Bán vé
                                            </span>
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

                                                <a href="${pageContext.request.contextPath}/staffList?action=edit&userId=<%= item.getUserId() %>"
                                                   class="staff-edit-btn">
                                                    Sửa
                                                </a>

                                                <form method="post" action="${pageContext.request.contextPath}/staffList" class="form-inline-action" style="margin: 0; display: inline-flex;">
                                                    <input type="hidden" name="action" value="status">
                                                    <input type="hidden" name="userId" value="<%= item.getUserId() %>">
                                                    <div class="status-select-wrap">
                                                        <select name="status" class="cf-status-select val-<%= st.toLowerCase() %>"
                                                                data-name="<%= staffName.replace("\"", "&quot;") %>"
                                                                data-current-status="<%= st %>"
                                                                onchange="handleStaffStatusChange(this)"
                                                                title="Bấm để thay đổi trạng thái nhân viên">

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
                                                </form>
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

<div id="cf-toast-container" aria-live="polite"></div>

<script src="${pageContext.request.contextPath}/js/admin-pagination.js?v=<%= System.currentTimeMillis() %>"></script>
<script src="${pageContext.request.contextPath}/js/staff-management.js?v=<%= System.currentTimeMillis() %>"></script>
</body>
</html>
