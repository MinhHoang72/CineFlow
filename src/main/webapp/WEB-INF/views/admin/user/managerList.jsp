<%--
    Trang JSP: Quản lý Tài khoản Quản lý chi nhánh (Manager) dành cho Quản trị viên (Admin).
    Controller phục vụ: ManagerManagementServlet (GET/POST /admin/managers).
    Chức năng:
    - Hiển thị KPI tổng số Manager theo trạng thái (Hoạt động, Tạm ngưng, Đã khóa).
    - Tìm kiếm, lọc trạng thái và phân trang bằng JS (admin-pagination.js + manager-management.js).
    - Modal 1: Tạo tài khoản Manager mới kèm chọn cụm rạp còn trống Quản lý (action="create").
    - Modal 2: Cập nhật thông tin cá nhân của Manager (action="update").
--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.entity.User, java.util.List, model.entity.Centre" %>
<%
    // 1. Bảo vệ trang: chỉ Quản trị viên (ADMIN) mới có quyền truy cập
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && session.getAttribute("user") instanceof model.entity.User) {
        authRole = ((model.entity.User) session.getAttribute("user")).getRole();
    }
    if (!"ADMIN".equalsIgnoreCase(authRole)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Quản trị viên (ADMIN) mới có quyền truy cập trang này.");
        return;
    }

    // 2. Lấy danh sách Manager & danh sách cụm rạp chưa có Quản lý từ ManagerManagementServlet
    List<User> managers = (List<User>) request.getAttribute("managers");
    List<Centre> centres = (List<Centre>) request.getAttribute("centres");

    request.setAttribute("activeNav", "managers");
    request.setAttribute("pageTitle", "Tài khoản Quản lý");

    // 3. Thống kê số lượng Manager theo trạng thái để hiển thị trên 4 thẻ KPI
    int totalManagers = managers != null ? managers.size() : 0;
    int activeCount = 0;
    int suspendedCount = 0;
    int lockedCount = 0;

    if (managers != null) {
        for (User m : managers) {
            String s = m.getStatus() != null ? m.getStatus().toUpperCase() : "INACTIVE";
            if ("ACTIVE".equals(s)) {
                activeCount++;
            } else if ("LOCKED".equals(s)) {
                lockedCount++;
            } else {
                suspendedCount++;
            }
        }
    }

    // 4. Ánh xạ mã thông báo thành công / mã lỗi từ URL sang nội dung tiếng Việt
    String successParam = request.getParameter("success");
    String errorParam = request.getParameter("error");

    java.util.Map<String, String> successMsgs = java.util.Map.of(
        "created", "Đã tạo mới tài khoản Quản lý và phân công phụ trách cụm rạp thành công.",
        "updated", "Đã cập nhật thông tin Quản lý (Manager) thành công.",
        "status_updated", "Đã cập nhật trạng thái hoạt động của tài khoản Manager."
    );
    java.util.Map<String, String> errorMsgs = java.util.Map.ofEntries(
        java.util.Map.entry("account_exists", "Tên tài khoản (Account) đã tồn tại trong hệ thống."),
        java.util.Map.entry("email_exists", "Địa chỉ Email đã được sử dụng bởi người dùng khác."),
        java.util.Map.entry("missing_fields", "Vui lòng điền đầy đủ các thông tin bắt buộc."),
        java.util.Map.entry("invalid_name", "Họ và tên phải từ 2 đến 100 ký tự."),
        java.util.Map.entry("invalid_account", "Tên tài khoản từ 3-30 ký tự, chỉ gồm chữ cái, số, gạch dưới, gạch ngang hoặc dấu chấm."),
        java.util.Map.entry("invalid_email", "Địa chỉ email không đúng định dạng chuẩn."),
        java.util.Map.entry("disposable_email", "Vui lòng không sử dụng địa chỉ email rác / email tạm thời."),
        java.util.Map.entry("weak_password", "Mật khẩu yếu: Yêu cầu tối thiểu 8 ký tự, gồm ít nhất 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt."),
        java.util.Map.entry("invalid_phone", "Số điện thoại không hợp lệ (yêu cầu số điện thoại di động Việt Nam 10 số)."),
        java.util.Map.entry("missing_centre", "Theo quy định bắt buộc của hệ thống, tài khoản Quản lý phải được phân công một cụm rạp phụ trách."),
        java.util.Map.entry("centre_has_manager", "Cụm rạp được chọn hiện đã có Quản lý đang phụ trách (mỗi rạp chỉ có 1 Quản lý)."),
        java.util.Map.entry("centre_already_has_manager", "Cụm rạp được chọn hiện đã có Quản lý đang phụ trách."),
        java.util.Map.entry("status_failed", "Cập nhật trạng thái Quản lý thất bại. Vui lòng thử lại."),
        java.util.Map.entry("not_manager", "Tài khoản không tồn tại hoặc không phải là Quản lý.")
    );
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quản lý Tài khoản Manager | CineFlow Admin</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/reset.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/variables.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/buttons.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/badges.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/forms.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/tables.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/shell.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/dashboard.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/managers.css?v=<%= System.currentTimeMillis() %>">
</head>
<body>
<div class="cf-manager-shell">
    <jsp:include page="/WEB-INF/fragments/adminSidebar.jspf" />

    <div class="cf-manager-main">
        <jsp:include page="/WEB-INF/fragments/adminTopbar.jspf" />

        <main class="cf-content">
            <div class="cf-page-header">
                <div>
                    <h1>Quản lý Tài khoản Manager</h1>
                </div>
                <button type="button" class="cf-btn cf-btn-primary btn-create-manager" onclick="openCreateManagerModal()">
                    <span></span> Tạo tài khoản quản lý mới
                </button>
            </div>

            <% if (successParam != null && successMsgs.containsKey(successParam)) { %>
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> <strong>Thành công!</strong> <%= successMsgs.get(successParam) %>
                </div>
            <% } else if (errorParam != null) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> <%= errorMsgs.getOrDefault(errorParam, "Thao tác thất bại. Vui lòng kiểm tra lại thông tin.") %>
                </div>
            <% } %>

            <!-- KPI METRICS: TỔNG SỐ, ĐANG HOẠT ĐỘNG, TẠM NGƯNG, ĐÃ KHÓA -->
            <div class="cf-kpi-grid-4">
                <div class="cf-kpi-card">
                    <span class="cf-kpi-label">Tổng số Manager</span>
                    <div class="cf-kpi-value"><%= totalManagers %></div>
                </div>
                <div class="cf-kpi-card">
                    <span class="cf-kpi-label">Đang hoạt động</span>
                    <div class="cf-kpi-value kpi-active"><%= activeCount %></div>
                </div>
                <div class="cf-kpi-card">
                    <span class="cf-kpi-label">Tạm ngưng (nghỉ phép/ốm)</span>
                    <div class="cf-kpi-value kpi-suspended" style="color: #fbbf24;"><%= suspendedCount %></div>
                </div>
                <div class="cf-kpi-card">
                    <span class="cf-kpi-label">Đã khóa (nghỉ việc)</span>
                    <div class="cf-kpi-value kpi-locked" style="color: #ef4444;"><%= lockedCount %></div>
                </div>
            </div>

            <!-- BẢNG DANH SÁCH QUẢN LÝ -->
            <section class="cf-card manager-card-section">
                <div class="cf-card-header manager-card-header">
                    <div>
                        <h2>Danh sách Quản lý </h2>
                        <p id="managerCountLabel"><%= totalManagers %> tài khoản trong hệ thống.</p>
                    </div>
                    <div class="manager-filter-toolbar">
                        <div class="cf-search-box cf-search-box-compact">
                            <svg class="cf-search-icon" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                                <circle cx="11" cy="11" r="8"></circle>
                                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                            </svg>
                            <input type="text"
                                   id="managerFilterInput" 
                                   placeholder="Tìm mã nhân sự, username, họ tên, rạp..." 
                                   value="<%= request.getAttribute("searchParam") != null ? request.getAttribute("searchParam") : "" %>"
                                   autocomplete="off"
                                   spellcheck="false">
                            <button type="button" id="managerSearchClearBtn" class="cf-search-clear-btn" style="display: none;" title="Xóa tìm kiếm">✕</button>
                        </div>
                        
                        <select id="statusFilterSelect" class="manager-status-select cf-select-sm" title="Lọc theo trạng thái tài khoản">
                            <option value="ALL">Tất cả trạng thái</option>
                            <option value="ACTIVE">Đang hoạt động</option>
                            <option value="INACTIVE">Tạm ngưng</option>
                            <option value="LOCKED">Đã khóa</option>
                        </select>

                        <div class="cf-page-size-wrap">
                            <span>Hiển thị:</span>
                            <select id="managerPageSizeSelect" class="cf-select-sm">
                                <option value="10" selected>10 quản lý</option>
                                <option value="20">20 quản lý</option>
                                <option value="50">50 quản lý</option>
                                <option value="ALL">Tất cả</option>
                            </select>
                        </div>
                    </div>
                </div>
                <div class="cf-card-body">
                    <div class="cf-table-wrap">
                        <table class="cf-table">
                            <thead>
                                <tr>
                                    <th class="th-emp-code">Mã nhân sự</th>
                                    <th class="th-manager">Quản lý</th>
                                    <th class="th-account">Tên đăng nhập</th>
                                    <th class="th-phone">Số điện thoại</th>
                                    <th class="th-centre">Trung tâm phụ trách</th>
                                    <th class="th-status">Trạng thái tài khoản</th>
                                    <th class="th-action">Thao tác</th>
                                </tr>
                            </thead>
                            <tbody id="managerTableBody">
                                <tr class="empty-row" id="managerTableEmptyRow" style="display: none;">
                                    <td colspan="7" class="cf-empty">Không tìm thấy tài khoản quản lý nào phù hợp.</td>
                                </tr>
                                <% if (managers == null || managers.isEmpty()) { %>
                                    <tr><td colspan="7" class="cf-empty">Chưa có tài khoản Manager nào trong hệ thống.</td></tr>
                                <% } else for (User m : managers) {
                                    String st = m.getStatus() != null ? m.getStatus().toUpperCase() : "INACTIVE";
                                    boolean isActive = "ACTIVE".equals(st);
                                    boolean isSuspended = "INACTIVE".equals(st);
                                    boolean isLocked = "LOCKED".equals(st);
                                    String centreName = m.getCentreName();
                                    boolean hasCentre = centreName != null && !centreName.trim().isEmpty();
                                    String searchData = (m.getEmployeeCode() + " " + m.getUserId() + " ql" + m.getUserId() + " " + m.getFullName() + " " + m.getAccount() + " " + m.getEmail() + " " + (hasCentre ? centreName : "")).toLowerCase();
                                    String statusClass = isActive ? "st-active" : (isSuspended ? "st-inactive" : "st-locked");
                                %>
                                    <tr class="manager-row <%= statusClass %>" data-search="<%= searchData %>" data-status="<%= st %>">
                                        <td><code class="cf-badge cf-badge-primary cf-badge-mgr-code"><%= m.getEmployeeCode() %></code></td>
                                        <td>
                                            <strong class="manager-name"><%= m.getFullName() %></strong>
                                            <small class="manager-email"><%= m.getEmail() %></small>
                                        </td>
                                        <td><code><%= m.getAccount() %></code></td>
                                        <td><%= m.getPhone() != null && !m.getPhone().isEmpty() ? m.getPhone() : "—" %></td>
                                        <td>
                                            <% if (hasCentre) { %>
                                                <span class="manager-centre-assigned">🏛 <%= centreName %></span>
                                            <% } else { %>
                                                <span class="cf-badge cf-badge-unassigned" title="Quản lý tự do, hiện chưa được phân công phụ trách cụm rạp nào.">Chưa phân công</span>
                                            <% } %>
                                        </td>
                                        <td>
                                            <form method="post" action="${pageContext.request.contextPath}/admin/managers" class="form-inline-action" style="margin: 0;">
                                                <input type="hidden" name="action" value="toggle_status">
                                                <input type="hidden" name="userId" value="<%= m.getUserId() %>">
                                                <div class="status-select-wrap">
                                                    <select name="status" class="cf-status-select val-<%= st.toLowerCase() %>"
                                                            data-name="<%= m.getFullName() != null ? m.getFullName().replace("\"", "&quot;") : "" %>"
                                                            data-current-status="<%= st %>"
                                                            onchange="handleManagerStatusChange(this)"
                                                            title="Bấm để thay đổi trạng thái Quản lý">
                                                        <option value="ACTIVE" <%= isActive ? "selected" : "" %>>Hoạt động</option>
                                                        <option value="INACTIVE" <%= isSuspended ? "selected" : "" %>>Tạm ngưng</option>
                                                        <option value="LOCKED" <%= isLocked ? "selected" : "" %>>Đã khóa</option>
                                                    </select>
                                                </div>
                                            </form>
                                        </td>
                                        <td class="cell-nowrap">
                                            <div class="manager-actions-row">
                                                <!-- Nút Sửa thông tin tài khoản -->
                                                <button type="button" class="cf-btn cf-btn-secondary cf-btn-sm btn-edit-manager" 
                                                        onclick="openEditManagerModal(<%= m.getUserId() %>, '<%= m.getEmployeeCode() %>', '<%= m.getFullName().replace("'", "\\'") %>', '<%= m.getPhone() != null ? m.getPhone().replace("'", "\\'") : "" %>', '<%= m.getEmail().replace("'", "\\'") %>')"
                                                        title="Chỉnh sửa thông tin cá nhân">
                                                    Sửa
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                <% } %>
                            </tbody>
                        </table>
                    </div>
                </div>

                <!-- THANH PHÂN TRANG -->
                <div class="cf-pagination-bar">
                    <div class="cf-pagination-info" id="managerPaginationInfo">
                        Đang nạp danh sách quản lý...
                    </div>
                    <div class="cf-pagination-controls" id="managerPaginationControls">
                        <!-- Các nút phân trang được sinh động bởi JS -->
                    </div>
                </div>
            </section>
        </main>
    </div>
</div>

<%-- MODAL 1: Biểu mẫu POST (action="create") tạo tài khoản Manager mới kèm phân công cụm rạp trống --%>
<div class="cf-modal-backdrop" id="createManagerModal">
    <div class="cf-modal">
        <div class="cf-modal-header">
            <h3>Tạo tài khoản Manager mới</h3>
            <button type="button" class="cf-modal-close" onclick="closeCreateManagerModal()">&times;</button>
        </div>
        <form method="post" action="${pageContext.request.contextPath}/admin/managers">
            <input type="hidden" name="action" value="create">
            <div class="cf-modal-body">
                <div class="cf-form-group">
                    <label>Cụm rạp phụ trách *</label>
                    <select name="centreId" required class="modal-select-field">
                        <% if (centres == null || centres.isEmpty()) { %>
                            <option value="">-- Hiện không có cụm rạp nào trống Quản lý --</option>
                        <% } else { %>
                            <option value="">-- Chọn cụm rạp phân công (chưa có Quản lý) --</option>
                            <% for (Centre c : centres) { %>
                                <option value="<%= c.getCentreId() %>"><%= c.getCentreName() %> (<%= c.getCity() %>)</option>
                            <% } %>
                        <% } %>
                    </select>
                    <% if (centres == null || centres.isEmpty()) { %>
                        <small class="modal-warning-danger">⚠ Tất cả các cụm rạp hiện đã có Quản lý phụ trách. Mỗi rạp chỉ được phân công 1 Quản lý.</small>
                    <% } else { %>
                        <small class="modal-helper-text">Hệ thống chỉ liệt kê các cụm rạp đang TRỐNG (chưa có Quản lý) để bảo đảm quy tắc 1 rạp - 1 Quản lý.</small>
                    <% } %>
                </div>
                <div class="cf-form-group">
                    <label>Họ và tên Quản lý *</label>
                    <input type="text" name="fullName" placeholder="Ví dụ: Hoàng Tuấn Anh" required>
                </div>
                <div class="modal-form-grid-2">
                    <div class="cf-form-group">
                        <label>Tên đăng nhập (Account) *</label>
                        <input type="text" name="account" placeholder="mgr_tuananh" required>
                    </div>
                    <div class="cf-form-group">
                        <label>Số điện thoại</label>
                        <input type="tel" name="phone" placeholder="09xxxxxxxx">
                    </div>
                </div>
                <div class="cf-form-group">
                    <label>Email *</label>
                    <input type="email" name="email" placeholder="manager.ta@cineflow.com" required>
                </div>
                <div class="cf-form-group">
                    <label>Mật khẩu khởi tạo *</label>
                    <input type="password" name="password" value="Manager@123" required>
                    <small class="modal-helper-text">Mặc định: Manager@123 (tối thiểu 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt)</small>
                </div>
            </div>
            <div class="cf-modal-footer">
                <button type="button" class="cf-btn cf-btn-secondary" onclick="closeCreateManagerModal()">Hủy</button>
                <button type="submit" class="cf-btn cf-btn-primary" <%= (centres == null || centres.isEmpty()) ? "disabled class=\"btn-disabled-action\"" : "" %>>Tạo tài khoản Manager</button>
            </div>
        </form>
    </div>
</div>

<%-- MODAL 2: Biểu mẫu POST (action="update") chỉnh sửa họ tên, SĐT, email của Manager --%>
<div class="cf-modal-backdrop" id="editManagerModal">
    <div class="cf-modal">
        <div class="cf-modal-header">
            <h3>Chỉnh sửa thông tin Manager</h3>
            <button type="button" class="cf-modal-close" onclick="closeEditManagerModal()">&times;</button>
        </div>
        <form method="post" action="${pageContext.request.contextPath}/admin/managers">
            <input type="hidden" name="action" value="update">
            <input type="hidden" name="userId" id="editUserId">
            <div class="cf-modal-body">
                <div class="modal-code-banner">
                    <span class="modal-code-label">Mã Quản lý (ID):</span>
                    <code class="cf-badge cf-badge-primary modal-code-badge" id="editManagerCode">QL00000</code>
                </div>
                <div class="cf-form-group">
                    <label>Họ và tên Quản lý *</label>
                    <input type="text" name="fullName" id="editFullName" required>
                </div>
                <div class="cf-form-group">
                    <label>Số điện thoại</label>
                    <input type="tel" name="phone" id="editPhone">
                </div>
                <div class="cf-form-group">
                    <label>Email liên hệ *</label>
                    <input type="email" name="email" id="editEmail" required>
                </div>
            </div>
            <div class="cf-modal-footer">
                <button type="button" class="cf-btn cf-btn-secondary" onclick="closeEditManagerModal()">Hủy</button>
                <button type="submit" class="cf-btn cf-btn-primary">Lưu thay đổi</button>
            </div>
        </form>
    </div>
</div>
<%-- Nạp JS phân trang dùng chung, xử lý modal Manager và tự ẩn thông báo sau 1.5s --%>
<script src="${pageContext.request.contextPath}/js/admin-pagination.js?v=<%= System.currentTimeMillis() %>"></script>
<script src="${pageContext.request.contextPath}/js/manager-management.js?v=<%= System.currentTimeMillis() %>"></script>
<script>
    document.addEventListener("DOMContentLoaded", function () {
        document.querySelectorAll(".cf-alert").forEach(function (alert) {
            setTimeout(function () {
                alert.style.transition = "opacity 0.4s ease";
                alert.style.opacity = "0";
                setTimeout(function () { alert.remove(); }, 400);
            }, 1500);
        });
    });
</script>
</body>
</html>
