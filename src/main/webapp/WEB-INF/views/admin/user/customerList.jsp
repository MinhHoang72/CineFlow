<%--
    Trang JSP: Quản lý Tài khoản Khách hàng dành cho Quản trị viên (Admin).
    Controller phục vụ: CustomerManagementServlet (GET/POST /admin/customers).
    Chức năng:
    - Hiển thị KPI tổng số khách hàng, đang hoạt động và bị tạm khóa.
    - Tìm kiếm không dấu, lọc trạng thái và phân trang trên bảng (admin-pagination.js + customer-management.js).
    - Gửi POST action="toggleStatus" để Khóa (LOCKED) hoặc Mở khóa (ACTIVE) tài khoản khách hàng.
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page import="model.entity.User, java.util.List" %>
<%
    // Thiết lập Sidebar/Topbar & nhận dữ liệu từ CustomerManagementServlet
    request.setAttribute("activeNav", "customers");
    request.setAttribute("pageTitle", "Tài khoản Khách hàng");

    List<User> customers = (List<User>) request.getAttribute("customers");
    Integer totalCustomers = (Integer) request.getAttribute("totalCustomers");
    Integer activeCustomers = (Integer) request.getAttribute("activeCustomers");
    Integer lockedCustomers = (Integer) request.getAttribute("lockedCustomers");
%>
<%-- Hàm esc() mã hóa ký tự HTML đặc biệt để chống tấn công XSS khi in dữ liệu người dùng --%>
<%!
    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/reset.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/variables.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/buttons.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/badges.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/forms.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/tables.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/alerts.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/shell.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/dashboard.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/customers.css?v=<%= System.currentTimeMillis() %>">
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
                <c:choose>
                    <c:when test="${param.success == 'statusUpdated'}">
                        <div class="cf-alert cf-alert-success">Cập nhật trạng thái tài khoản khách hàng thành công.</div>
                    </c:when>
                    <c:when test="${param.error == 'invalidStatus'}">
                        <div class="cf-alert cf-alert-danger">Trạng thái tài khoản không hợp lệ.</div>
                    </c:when>
                    <c:when test="${param.error == 'statusFailed'}">
                        <div class="cf-alert cf-alert-danger">Cập nhật trạng thái thất bại. Vui lòng thử lại.</div>
                    </c:when>
                </c:choose>

                <!-- THỐNG KÊ -->
                <div class="cf-stats-row">
                    <div class="cf-stat-card">
                        <div>
                            <div class="cf-stat-label">Tổng khách hàng</div>
                            <div class="cf-stat-val"><%= totalCustomers != null ? String.format("%,d", totalCustomers) : "0" %></div>
                        </div>
                        <div class="cf-stat-icon icon-total">👥</div>
                    </div>
                    <div class="cf-stat-card">
                        <div>
                            <div class="cf-stat-label">Đang hoạt động</div>
                            <div class="cf-stat-val stat-active"><%= activeCustomers != null ? String.format("%,d", activeCustomers) : "0" %></div>
                        </div>
                        <div class="cf-stat-icon icon-active">✓</div>
                    </div>
                    <div class="cf-stat-card">
                        <div>
                            <div class="cf-stat-label">Bị tạm khóa</div>
                            <div class="cf-stat-val stat-locked"><%= lockedCustomers != null ? String.format("%,d", lockedCustomers) : "0" %></div>
                        </div>
                        <div class="cf-stat-icon icon-locked">🔒</div>
                    </div>
                </div>

                <!-- BẢNG DANH SÁCH KHÁCH HÀNG -->
                <section class="cf-card customer-card-section">
                    <div class="cf-card-header customer-card-header">
                        <div>
                            <h2>Danh sách Khách hàng</h2>
                            <p id="customerCountLabel"><%= totalCustomers != null ? totalCustomers : 0 %> tài khoản trong hệ thống.</p>
                        </div>
                        <div class="customer-filter-toolbar">
                            <div class="cf-search-box cf-search-box-compact">
                                <svg class="cf-search-icon" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                                    <circle cx="11" cy="11" r="8"></circle>
                                    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                                </svg>
                                <input type="text" id="customerSearchInput" placeholder="Tìm tên, username, email, SĐT..." autocomplete="off" spellcheck="false">
                                <button type="button" id="customerSearchClearBtn" class="cf-search-clear-btn" style="display: none;" title="Xóa tìm kiếm">✕</button>
                            </div>

                            <select id="statusFilterSelect" class="customer-status-select cf-select-sm" title="Lọc theo trạng thái tài khoản">
                                <option value="ALL" selected>Tất cả trạng thái</option>
                                <option value="ACTIVE">Đang hoạt động</option>
                                <option value="LOCKED">Bị tạm khóa</option>
                            </select>

                            <div class="cf-page-size-wrap">
                                <span>Hiển thị:</span>
                                <select id="customerPageSizeSelect" class="cf-select-sm">
                                    <option value="10" selected>10 khách hàng</option>
                                    <option value="20">20 khách hàng</option>
                                    <option value="50">50 khách hàng</option>
                                    <option value="ALL">Tất cả</option>
                                </select>
                            </div>
                        </div>
                    </div>

                    <div class="cf-card-body">
                        <div class="cf-table-wrap search-form-clean">
                            <table class="cf-table customer-table">
                                <thead>
                                    <tr>
                                        <th class="th-cust-code">Mã KH</th>
                                        <th>Khách hàng</th>
                                        <th>Email &amp; Xác minh</th>
                                        <th>Số điện thoại</th>
                                        <th>Trạng thái</th>
                                        <th>Thao tác</th>
                                    </tr>
                                </thead>
                                <tbody id="customerTableBody">
                                    <tr class="empty-row" id="customerTableEmptyRow" style="display: none;">
                                        <td colspan="6" class="cf-empty-state customer-empty-cell">
                                            <div class="customer-empty-icon">🔍</div>
                                            <strong>Không tìm thấy khách hàng nào phù hợp</strong>
                                            <p class="customer-empty-sub">Hãy thử tìm kiếm với từ khóa khác hoặc chọn hiển thị tất cả.</p>
                                        </td>
                                    </tr>
                                    <% if (customers == null || customers.isEmpty()) { %>
                                    <tr>
                                        <td colspan="6" class="cf-empty-state customer-empty-cell">
                                            <div class="customer-empty-icon">🔍</div>
                                            <strong>Không tìm thấy khách hàng nào phù hợp</strong>
                                            <p class="customer-empty-sub">Hãy thử tìm kiếm với từ khóa khác hoặc bấm chọn "Tất cả".</p>
                                        </td>
                                    </tr>
                                    <% } else {
                                        for (User c : customers) {
                                            boolean isActive = "ACTIVE".equalsIgnoreCase(c.getStatus());
                                            String initial = (c.getFullName() != null && !c.getFullName().isBlank()) ? c.getFullName().substring(0, 1).toUpperCase() : "C";
                                    %>
                                    <tr class="customer-row" data-status="<%= esc(c.getStatus()) %>" data-search="<%= esc(c.getFullName()) %> <%= esc(c.getCustomerCode()) %> <%= esc(c.getAccount()) %> <%= esc(c.getEmail()) %> <%= esc(c.getPhone() != null ? c.getPhone() : "") %>">
                                        <!-- MÃ KHÁCH HÀNG -->
                                        <td>
                                            <code class="cf-badge cf-badge-primary cf-badge-cust-code"><%= esc(c.getCustomerCode()) %></code>
                                        </td>

                                        <!-- KHÁCH HÀNG -->
                                        <td>
                                            <div class="user-cell">
                                                <div class="user-avatar-circle">
                                                    <% if (c.getAvatar() != null && !c.getAvatar().isBlank()) { %>
                                                    <img src="<%= esc(c.getAvatar()) %>" alt="<%= esc(c.getFullName()) %>">
                                                    <% } else { %>
                                                    <%= esc(initial) %>
                                                    <% } %>
                                                </div>
                                                <div class="user-cell-info">
                                                    <div class="user-name-link"><%= esc(c.getFullName()) %></div>
                                                    <small class="user-account-tag" style="color: var(--cf-muted); font-size: 12px;">@<%= esc(c.getAccount()) %></small>
                                                </div>
                                            </div>
                                        </td>

                                        <!-- EMAIL VÀ XÁC MINH -->
                                        <td>
                                            <div class="customer-email-cell"><%= esc(c.getEmail()) %></div>
                                            <div class="customer-badge-row">
                                                <% if (c.isEmailVerified()) { %>
                                                <span class="cf-badge cf-badge-success cf-badge-xs">Đã xác minh</span>
                                                <% } else { %>
                                                <span class="cf-badge cf-badge-muted cf-badge-xs">Chưa xác minh</span>
                                                <% } %>
                                            </div>
                                        </td>

                                        <!-- SỐ ĐIỆN THOẠI -->
                                        <td><%= (c.getPhone() != null && !c.getPhone().isBlank()) ? esc(c.getPhone()) : "—" %></td>

                                        <!-- TRẠNG THÁI -->
                                        <td>
                                            <% if (isActive) { %>
                                            <span class="cf-badge cf-badge-success">Đang hoạt động</span>
                                            <% } else if ("LOCKED".equalsIgnoreCase(c.getStatus())) { %>
                                            <span class="cf-badge cf-badge-danger">Bị tạm khóa</span>
                                            <% } else { %>
                                            <span class="cf-badge cf-badge-muted"><%= esc(c.getStatus()) %></span>
                                            <% } %>
                                        </td>

                                        <!-- THAO TÁC -->
                                        <td>
                                            <div>
                                                <form method="post" action="${pageContext.request.contextPath}/admin/customers">
                                                    <input type="hidden" name="userId" value="<%= c.getUserId() %>">
                                                    <input type="hidden" name="newStatus" value="<%= isActive ? "LOCKED" : "ACTIVE" %>">
                                                    <% if (isActive) { %>
                                                    <button type="submit" class="cf-btn cf-btn-sm btn-modal-lock-confirm" title="Khóa tài khoản này">🔒 Khóa</button>
                                                    <% } else { %>
                                                    <button type="submit" class="cf-btn cf-btn-sm btn-modal-unlock-confirm" title="Mở khóa tài khoản này">🔓 Mở khóa</button>
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

                    <!-- THANH PHÂN TRANG -->
                    <div class="cf-pagination-bar">
                        <div class="cf-pagination-info" id="customerPaginationInfo">Đang nạp danh sách khách hàng...</div>
                        <div class="cf-pagination-controls" id="customerPaginationControls">
                            <!-- Các nút phân trang được sinh động bởi JS -->
                        </div>
                    </div>
                </section>
            </main>
        </div>
    </div>

    <script src="${pageContext.request.contextPath}/js/admin-pagination.js?v=<%= System.currentTimeMillis() %>"></script>
    <script src="${pageContext.request.contextPath}/js/customer-management.js?v=<%= System.currentTimeMillis() %>"></script>
</body>
</html>
