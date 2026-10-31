
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.entity.User,java.util.List,java.math.BigDecimal" %>
<%
    String authRole = (String) session.getAttribute("role");

    if (authRole == null && session.getAttribute("user") instanceof model.entity.User) {
        authRole = ((model.entity.User) session.getAttribute("user")).getRole();
    }

    if (!"ADMIN".equalsIgnoreCase(authRole)) {
        response.sendError(
            HttpServletResponse.SC_FORBIDDEN,
            "Chỉ Quản trị viên (ADMIN) mới có quyền truy cập trang này."
        );
        return;
    }

    request.setAttribute("activeNav", "customers");
    request.setAttribute("pageTitle", "Tài khoản Khách hàng");

    List<User> customers =(List<User>) request.getAttribute("customers");

    Integer totalCustomers =(Integer) request.getAttribute("totalCustomers");

    Integer activeCustomers =(Integer) request.getAttribute("activeCustomers");

    Integer lockedCustomers =(Integer) request.getAttribute("lockedCustomers");

    String currentSearch =(String) request.getAttribute("currentSearch");

    String currentStatus =(String) request.getAttribute("currentStatus");
    
    String successParam = request.getParameter("success");
    String errorParam = request.getParameter("error");

%>

<%!
    private String esc(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;");
    }

    private String money(BigDecimal amount) {
        if (amount == null) {
            return "0 đ";
        }

        return String.format("%,.0f đ", amount);
    }
%>

<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">

        <title>Quản lý Tài khoản Khách hàng | CineFlow Admin</title>

        <link rel="stylesheet"
              href="${pageContext.request.contextPath}/css/common/reset.css">

        <link rel="stylesheet"
              href="${pageContext.request.contextPath}/css/common/variables.css">

        <link rel="stylesheet"
              href="${pageContext.request.contextPath}/css/common/buttons.css">

        <link rel="stylesheet"
              href="${pageContext.request.contextPath}/css/common/badges.css">

        <link rel="stylesheet"
              href="${pageContext.request.contextPath}/css/common/forms.css">

        <link rel="stylesheet"
              href="${pageContext.request.contextPath}/css/common/tables.css">

        <link rel="stylesheet"
              href="${pageContext.request.contextPath}/css/common/alerts.css">

        <link rel="stylesheet"
              href="${pageContext.request.contextPath}/css/manager/shell.css">

        <link rel="stylesheet"
              href="${pageContext.request.contextPath}/css/admin/dashboard.css">

        <link rel="stylesheet"
              href="${pageContext.request.contextPath}/css/admin/customers.css?v=<%= System.currentTimeMillis() %>">
    </head>

    <body>
        <div class="cf-manager-shell">
            <jsp:include page="/WEB-INF/fragments/adminSidebar.jspf" />

            <div class="cf-manager-main">
                <jsp:include page="/WEB-INF/fragments/adminTopbar.jspf" />

                <main class="cf-content">

                    <!-- TIÊU ĐỀ TRANG -->
                    <div class="cf-page-header">
                        <div>
                            <h1>Quản lý Tài khoản Khách hàng</h1>
                        </div>
                    </div>

                    <!-- THÔNG BÁO -->
                    <% if ("statusUpdated".equals(successParam)) { %>
                    <div class="cf-alert cf-alert-success">
                        Cập nhật trạng thái tài khoản khách hàng thành công.
                    </div>
                    <% } else if ("invalidStatus".equals(errorParam)) { %>
                    <div class="cf-alert cf-alert-danger">Trạng thái tài khoản không hợp lệ.</div>
                    <% } else if ("statusFailed".equals(errorParam)) { %>
                    <div class="cf-alert cf-alert-danger">
                        Cập nhật trạng thái thất bại. Vui lòng thử lại.
                    </div>
                    <% } %>

                    <!-- THỐNG KÊ -->
                    <div class="cf-stats-row">
                        <div class="cf-stat-card">
                            <div>
                                <div class="cf-stat-label">Tổng khách hàng</div>
                                <div class="cf-stat-val">
                                    <%= totalCustomers != null
                                        ? String.format("%,d", totalCustomers)
                                        : "0" %>
                                </div>
                            </div>

                            <div class="cf-stat-icon icon-total">
                                👥
                            </div>
                        </div>

                        <div class="cf-stat-card">
                            <div>
                                <div class="cf-stat-label">Đang hoạt động</div>
                                <div class="cf-stat-val stat-active">
                                    <%= activeCustomers != null
                                        ? String.format("%,d", activeCustomers)
                                        : "0" %>
                                </div>
                            </div>

                            <div class="cf-stat-icon icon-active">
                                ✓
                            </div>
                        </div>

                        <div class="cf-stat-card">
                            <div>
                                <div class="cf-stat-label">Bị tạm khóa</div>
                                <div class="cf-stat-val stat-locked">
                                    <%= lockedCustomers != null
                                        ? String.format("%,d", lockedCustomers)
                                        : "0" %>
                                </div>
                            </div>

                            <div class="cf-stat-icon icon-locked">
                                🔒
                            </div>
                        </div>
                    </div>

                    <!-- TÌM KIẾM VÀ LỌC -->
                    <div class="customer-filter-bar">
                        <div class="filter-tabs">
                            <a href="${pageContext.request.contextPath}/admin/customers?status=ALL<%= currentSearch != null && !currentSearch.isBlank() ? "&search=" + esc(currentSearch) : "" %>"
                               class="filter-tab <%= "ALL".equalsIgnoreCase(currentStatus) ? "active" : "" %>">
                                Tất cả (<%= totalCustomers != null ? totalCustomers : 0 %>)
                            </a>

                            <a href="${pageContext.request.contextPath}/admin/customers?status=ACTIVE<%= currentSearch != null && !currentSearch.isBlank() ? "&search=" + esc(currentSearch) : "" %>"
                               class="filter-tab <%= "ACTIVE".equalsIgnoreCase(currentStatus) ? "active" : "" %>">
                                Đang hoạt động (<%= activeCustomers != null ? activeCustomers : 0 %>)
                            </a>

                            <a href="${pageContext.request.contextPath}/admin/customers?status=LOCKED<%= currentSearch != null && !currentSearch.isBlank() ? "&search=" + esc(currentSearch) : "" %>"
                               class="filter-tab <%= "LOCKED".equalsIgnoreCase(currentStatus) ? "active" : "" %>">
                                Bị tạm khóa (<%= lockedCustomers != null ? lockedCustomers : 0 %>)
                            </a>
                        </div>

                        <form method="get"
                              action="${pageContext.request.contextPath}/admin/customers"
                              class="search-box">

                            <input type="hidden"
                                   name="status"
                                   value="<%= esc(currentStatus) %>">

                            <input type="text"
                                   name="search"
                                   class="search-input"
                                   placeholder="🔍 Tìm tên, username, email, SĐT..."
                                   value="<%= esc(currentSearch) %>">

                            <button type="submit" class="cf-btn cf-btn-secondary btn-filter-search">
                                Tìm kiếm
                            </button>

                            <% if (currentSearch != null && !currentSearch.isBlank()) { %>
                            <a href="${pageContext.request.contextPath}/admin/customers?status=<%= esc(currentStatus) %>" class="cf-btn cf-btn-secondary btn-filter-clear" title="Xóa tìm kiếm">
                                ✕
                            </a>
                            <% } %>
                        </form>
                    </div>

                    <!-- BẢNG KHÁCH HÀNG -->
                    <div class="cf-card">
                        <div class="cf-table-wrap search-form-clean">

                            <!-- Đã thêm customer-table để CSS cột được áp dụng -->
                            <table class="cf-table customer-table">
                                <thead>
                                    <tr>
                                        <th>Khách hàng</th>
                                        <th>Email &amp; Xác minh</th>
                                        <th>Số điện thoại</th>
                                        <th>Đơn đặt vé</th>
                                        <th>Tổng chi tiêu</th>
                                        <th>Trạng thái</th>
                                        <th>Thao tác</th>
                                    </tr>
                                </thead>

                                <tbody>
                                    <% if (customers == null || customers.isEmpty()) { %>
                                    <tr>
                                        <td colspan="7" class="cf-empty-state customer-empty-cell">
                                            <div class="customer-empty-icon">
                                                🔍
                                            </div>

                                            <strong>
                                                Không tìm thấy khách hàng nào phù hợp
                                            </strong>

                                            <p class="customer-empty-sub">
                                                Hãy thử tìm kiếm với từ khóa khác hoặc bấm chọn "Tất cả".
                                            </p>
                                        </td>
                                    </tr>
                                    <% } else {
                                        for (User c : customers) {
                                            boolean isActive =
                                                "ACTIVE".equalsIgnoreCase(c.getStatus());

                                            String initial =
                                                c.getFullName() != null
                                                && !c.getFullName().isBlank()
                                                ? c.getFullName().substring(0, 1).toUpperCase()
                                                : "C";
                                    %>

                                    <tr>
                                        <!-- KHÁCH HÀNG -->
                                        <td>
                                            <div class="user-cell">
                                                <div class="user-avatar-circle">
                                                    <% if (c.getAvatar() != null
                                                           && !c.getAvatar().isBlank()) { %>
                                                    <img src="<%= esc(c.getAvatar()) %>"
                                                         alt="<%= esc(c.getFullName()) %>">
                                                    <% } else { %>
                                                    <%= esc(initial) %>
                                                    <% } %>
                                                </div>

                                                <div class="user-cell-info">
                                                    <div class="user-name-link">
                                                        <%= esc(c.getFullName()) %>
                                                    </div>

                                                    <div class="user-code-tag">
                                                        <%= esc(c.getCustomerCode()) %>
                                                        · @<%= esc(c.getAccount()) %>
                                                    </div>
                                                </div>
                                            </div>
                                        </td>

                                        <!-- EMAIL VÀ XÁC MINH -->
                                        <td>
                                            <div class="customer-email-cell">
                                                <%= esc(c.getEmail()) %>
                                            </div>

                                            <div class="customer-badge-row">
                                                <% if (c.isEmailVerified()) { %>
                                                <span class="cf-badge cf-badge-success cf-badge-xs">
                                                    Đã xác minh
                                                </span>
                                                <% } else { %>
                                                <span class="cf-badge cf-badge-muted cf-badge-xs">
                                                    Chưa xác minh
                                                </span>
                                                <% } %>
                                            </div>
                                        </td>

                                        <!-- SỐ ĐIỆN THOẠI -->
                                        <td>
                                            <%= c.getPhone() != null && !c.getPhone().isBlank()
                                                ? esc(c.getPhone())
                                                : "—" %>
                                        </td>

                                        <!-- ĐƠN ĐẶT VÉ -->
                                        <td>
                                            <strong><%= c.getBookingCount() %></strong>
                                            đơn
                                        </td>

                                        <!-- TỔNG CHI TIÊU -->
                                        <td>
                                            <strong class="customer-total-spent">
                                                <%= money(c.getTotalSpent()) %>
                                            </strong>
                                        </td>

                                        <!-- TRẠNG THÁI -->
                                        <td>
                                            <% if (isActive) { %>
                                            <span class="cf-badge cf-badge-success">
                                                Đang hoạt động
                                            </span>
                                            <% } else if (
                                                "LOCKED".equalsIgnoreCase(c.getStatus())
                                            ) { %>
                                            <span class="cf-badge cf-badge-danger">
                                                Bị tạm khóa
                                            </span>
                                            <% } else { %>
                                            <span class="cf-badge cf-badge-muted">
                                                <%= esc(c.getStatus()) %>
                                            </span>
                                            <% } %>
                                        </td>

                                        <!-- THAO TÁC -->
                                        <td>
                                            <div>
                                                <form method="post" action="${pageContext.request.contextPath}/admin/customers">
                                                    <%--Gửi hành động toggleStatus để backend biết yêu cầu là thay đổi trạng thái tài khoản.--%>
                                                    <input type="hidden"
                                                           name="action"
                                                           value="toggleStatus">
                                                    <%--Gửi ID của khách hàng cần thay đổi trạng thái.--%>

                                                    <input type="hidden"
                                                           name="userId"
                                                           value="<%= c.getUserId() %>">
                                                    <%--Xác định trạng thái mới--%>
                                                    <input type="hidden"
                                                           name="newStatus"
                                                           value="<%= isActive ? "LOCKED" : "ACTIVE" %>">
                                                    <%--Giúp backend biết trạng thái lọc và từ khóa tìm kiếm hiện tại khi xử lý yêu cầu.--%>
                                                    <input type="hidden"
                                                           name="status"
                                                           value="<%= esc(currentStatus) %>">

                                                    <input type="hidden"
                                                           name="search"
                                                           value="<%= esc(currentSearch) %>">

                                                    <% if (isActive) { %>
                                                    <button type="submit" class="cf-btn cf-btn-sm btn-modal-lock-confirm" title="Khóa tài khoản này">
                                                        🔒 Khóa
                                                    </button>
                                                    <% } else { %>
                                                    <button type="submit" class="cf-btn cf-btn-sm btn-modal-unlock-confirm" title="Mở khóa tài khoản này">
                                                        🔓 Mở khóa
                                                    </button>
                                                    <% } %>
                                                </form>
                                            </div>
                                        </td>
                                    </tr>

                                    <%  }
                                       }
                                    %>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </main>
            </div>
        </div>

        <script>
            document.addEventListener('DOMContentLoaded', function () {
                document.querySelectorAll(
                        '.cf-alert-success, .cf-alert-danger'
                        ).forEach(function (alert) {
                    setTimeout(function () {
                        alert.style.transition =
                                'opacity .4s ease, transform .4s ease';

                        alert.style.opacity = '0';
                        alert.style.transform = 'translateY(-8px)';

                        setTimeout(function () {
                            if (alert.parentNode) {
                                alert.parentNode.removeChild(alert);
                            }
                        }, 400);
                    }, 1000);
                });
            });
        </script>
    </body>
</html>
