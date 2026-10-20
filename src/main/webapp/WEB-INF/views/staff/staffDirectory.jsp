<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.entity.User,model.entity.Centre,java.util.List" %>
<%
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && session.getAttribute("user") instanceof model.entity.User) {
        authRole = ((model.entity.User) session.getAttribute("user")).getRole();
    }

    if (!"MANAGER".equalsIgnoreCase(authRole) && !"ADMIN".equalsIgnoreCase(authRole)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN,
            "Bạn không có quyền truy cập trang quản lý nhân sự.");
        return;
    }

    if (request.getAttribute("staff") == null) {
        response.sendRedirect(request.getContextPath() + "/staff");
        return;
    }

    List<User> staff = (List<User>) request.getAttribute("staff");
    List<Centre> centres = (List<Centre>) request.getAttribute("centres");
    Centre assignedCentre = (Centre) request.getAttribute("assignedCentre");
    Integer selectedCentreId = (Integer) request.getAttribute("selectedCentreId");
    boolean isAdminRole = Boolean.TRUE.equals(request.getAttribute("isAdmin"));

    String currentCentreName = assignedCentre != null
        ? assignedCentre.getCentreName()
        : (selectedCentreId == null ? "Chưa bổ nhiệm trung tâm" : "Toàn hệ thống");

    String successParam = request.getParameter("success");
    String errorParam = request.getParameter("error");
    String searchParam = request.getParameter("search");
    String cFilterVal = request.getParameter("centreId");
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

    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/staff/directory.css?v=<%= System.currentTimeMillis() %>">
</head>

<body>
<div class="cf-manager-shell">
    <% if (isAdminRole) { %>
        <jsp:include page="/WEB-INF/views/admin/adminSidebar.jspf"/>
    <% } else { %>
        <jsp:include page="/WEB-INF/views/manager/managerSidebar.jspf"/>
    <% } %>

    <div class="cf-manager-main">
        <% if (isAdminRole) { %>
            <jsp:include page="/WEB-INF/views/admin/adminTopbar.jspf"/>
        <% } else { %>
            <jsp:include page="/WEB-INF/views/manager/managerTopbar.jspf"/>
        <% } %>

        <main class="cf-content">
            <div class="cf-page-header">
                <div>
                    <h1>Nhân sự <%= currentCentreName %></h1>
                </div>
            </div>

            <% if ("created".equals(successParam)) { %>
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> <strong>Thành công!</strong> Đã tạo mới tài khoản nhân viên cho trung tâm.
                </div>
            <% } else if ("updated".equals(successParam)) { %>
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> <strong>Thành công!</strong> Đã cập nhật thông tin nhân viên thành công.
                </div>
            <% } else if ("status_updated".equals(successParam)) { %>
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> Đã cập nhật trạng thái hoạt động của nhân viên.
                </div>
            <% } else if ("account_exists".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Tên tài khoản đã tồn tại trong hệ thống.
                </div>
            <% } else if ("email_exists".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Địa chỉ Email này đã được sử dụng.
                </div>
            <% } else if ("missing_fields".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> Vui lòng điền đầy đủ các thông tin bắt buộc.
                </div>
            <% } else if ("invalid_name".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Họ và tên phải từ 2 đến 100 ký tự.
                </div>
            <% } else if ("invalid_account".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Tên tài khoản từ 3-30 ký tự, chỉ gồm chữ cái, số, gạch dưới, gạch ngang hoặc dấu chấm.
                </div>
            <% } else if ("invalid_email".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Địa chỉ email không đúng định dạng chuẩn.
                </div>
            <% } else if ("disposable_email".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Vui lòng không sử dụng địa chỉ email rác / email tạm thời.
                </div>
            <% } else if ("weak_password".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Mật khẩu yếu:</strong> Yêu cầu tối thiểu 8 ký tự, gồm ít nhất 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt.
                </div>
            <% } else if ("invalid_phone".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Số điện thoại không hợp lệ (yêu cầu số điện thoại di động Việt Nam 10 số).
                </div>
            <% } else if ("unassigned_manager".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> Quản lý chưa được bổ nhiệm trung tâm nên không thể tạo nhân viên.
                </div>
            <% } else if ("missing_centre".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Vui lòng chọn trung tâm/rạp chiếu cho nhân viên.
                </div>
            <% } else if ("invalid_centre".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Trung tâm được chọn không hợp lệ hoặc không tồn tại.
                </div>
            <% } else if ("create_failed".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Không thể tạo tài khoản nhân viên. Vui lòng kiểm tra lại thông tin.
                </div>
            <% } else if (errorParam != null) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> Thao tác không thành công. Vui lòng kiểm tra lại thông tin.
                </div>
            <% } %>

            <% if (selectedCentreId == null && !isAdminRole) { %>
                <section class="cf-card" style="padding:40px 24px;text-align:center;margin-top:20px;">
                    <div style="font-size:40px;margin-bottom:14px;">🏛</div>
                    <h2 style="color:#fbbf24;margin-bottom:8px;">Tài khoản chưa được bổ nhiệm trung tâm</h2>
                    <p style="color:#94a3b8;max-width:540px;margin:0 auto 16px;font-size:14px;line-height:1.7;">
                        Tài khoản Quản lý của bạn hiện chưa được phân công phụ trách trung tâm chiếu phim nào.
                        Quản lý chỉ được tạo và quản lý nhân viên tại trung tâm đã được bổ nhiệm.
                    </p>
                    <p style="font-size:13px;color:#cbd5e1;">
                        Vui lòng liên hệ Quản trị viên hệ thống để được phân công trung tâm.
                    </p>
                </section>
            <% } else { %>

            <section class="cf-card" style="margin-top:20px;">
                <div class="staff-card-header">
                    <div class="staff-heading">
                        <h2>Danh sách nhân viên</h2>
                        <p id="staffCountLabel"><%= staff == null ? 0 : staff.size() %> tài khoản đang hiển thị.</p>
                    </div>

                    <div class="staff-toolbar">
                        <form class="staff-search-form" method="get"
                              action="${pageContext.request.contextPath}/staff">
                            <% if (cFilterVal != null) { %>
                                <input type="hidden" name="centreId" value="<%= cFilterVal %>">
                            <% } %>

                            <div class="staff-search-box">
                                <span class="staff-search-icon" aria-hidden="true">⌕</span>
                                <input type="search" name="search" id="staffFilterInput"
                                       value="<%= searchParam != null ? searchParam : "" %>"
                                       placeholder="Tìm mã NV, họ tên, tài khoản..."
                                       aria-label="Tìm nhân viên">
                            </div>

                            <button type="submit" class="staff-control-btn">Tìm kiếm</button>

                            <% if (searchParam != null && !searchParam.trim().isEmpty()) { %>
                                <a href="${pageContext.request.contextPath}/staff<%= cFilterVal != null ? "?centreId=" + cFilterVal : "" %>"
                                   class="staff-control-btn">Xóa lọc</a>
                            <% } %>
                        </form>

                        <div class="staff-select-wrap">
                            <select id="staffRoleFilter" aria-label="Lọc theo vị trí công việc">
                                <option value="">Tất cả vị trí</option>
                                <option value="role-sales">🎫 Bán vé (Ticket Sales)</option>
                                <option value="role-checker">🔍 Soát vé (Ticket Checker)</option>
                            </select>
                        </div>

                        <div class="staff-select-wrap">
                            <select id="staffStatusFilter" aria-label="Lọc theo trạng thái">
                                <option value="">Tất cả trạng thái</option>
                                <option value="st-active">Đang hoạt động</option>
                                <option value="st-inactive">Tạm ngưng</option>
                                <option value="st-locked">Đã khóa</option>
                            </select>
                        </div>

                        <button type="button"
                                class="staff-control-btn staff-control-btn-primary"
                                onclick="openCreateStaffModal()">
                            <span aria-hidden="true"></span> Thêm nhân viên
                        </button>
                    </div>
                </div>

                <div class="staff-table-body">
                    <div class="cf-table-wrap">
                        <table class="cf-table">
                            <thead>
                            <tr>
                                <th>Mã nhân sự</th>
                                <th>Nhân viên</th>
                                <th>Tên đăng nhập</th>
                                <th>Vị trí công việc</th>
                                <th>Số điện thoại</th>
                                <th>Trung tâm</th>
                                <th>Trạng thái</th>
                                <th>Thao tác</th>
                            </tr>
                            </thead>
                            <tbody>
                            <% if (staff == null || staff.isEmpty()) { %>
                                <tr>
                                    <td colspan="8" class="cf-empty">
                                        Chưa có nhân viên phù hợp với từ khóa tìm kiếm.
                                    </td>
                                </tr>
                            <% } else {
                                for (User item : staff) {
                                    String st = item.getStatus() != null ? item.getStatus() : "INACTIVE";
                                    String staffName = item.getFullName() != null ? item.getFullName() : "";
                                    String staffEmail = item.getEmail() != null ? item.getEmail() : "";
                                    String staffAccount = item.getAccount() != null ? item.getAccount() : "";
                                    String staffPhone = item.getPhone() != null ? item.getPhone() : "";
                                    String employeeCode = item.getEmployeeCode() != null ? item.getEmployeeCode() : "";
                                    String staffRole = item.getRole() != null ? item.getRole() : "TICKET_SALES_STAFF";
                                    boolean isSales = item.isTicketSalesStaff();
                                    boolean isChecker = item.isTicketCheckerStaff();
                                    String roleClass = isSales ? "role-sales" : (isChecker ? "role-checker" : "");
                                    String searchVal = (employeeCode + " " + item.getUserId() + " nv" + item.getUserId()
                                        + " " + staffName + " " + staffEmail + " " + staffAccount + " " + staffPhone + " " + staffRole).toLowerCase();
                                    String stClass = "ACTIVE".equalsIgnoreCase(st) ? "st-active"
                                        : ("INACTIVE".equalsIgnoreCase(st) ? "st-inactive" : "st-locked");
                                    String itemCentreName = item.getCentreName() != null
                                        ? item.getCentreName() : currentCentreName;
                            %>
                                <tr class="staff-row <%= stClass %> <%= roleClass %>" data-search="<%= searchVal %>">
                                    <td>
                                        <code class="cf-badge cf-badge-info"><%= employeeCode %></code>
                                    </td>
                                    <td>
                                        <strong><%= staffName %></strong>
                                        <small><%= staffEmail %></small>
                                    </td>
                                    <td><code><%= staffAccount %></code></td>
                                    <td>
                                        <% if (isSales) { %>
                                            <span class="cf-badge" style="background: rgba(56, 189, 248, 0.15); color: #38bdf8; border: 1px solid rgba(56, 189, 248, 0.35);">
                                                🎫 Bán vé (Ticket Sales)
                                            </span>
                                        <% } else if (isChecker) { %>
                                            <span class="cf-badge" style="background: rgba(168, 85, 247, 0.15); color: #c084fc; border: 1px solid rgba(168, 85, 247, 0.35);">
                                                🔍 Soát vé (Ticket Checker)
                                            </span>
                                        <% } else { %>
                                            <span class="cf-badge cf-badge-info">
                                                Nhân viên
                                            </span>
                                        <% } %>
                                    </td>
                                    <td><%= !staffPhone.isEmpty() ? staffPhone : "—" %></td>
                                    <td>🏛 <%= itemCentreName %></td>
                                    <td>
                                        <span id="badge-status-<%= item.getUserId() %>"
                                              class="cf-badge <%= "ACTIVE".equalsIgnoreCase(st) ? "cf-badge-success" : ("LOCKED".equalsIgnoreCase(st) ? "cf-badge-danger" : "cf-badge-warning") %>">
                                            <%= "ACTIVE".equalsIgnoreCase(st) ? "Đang hoạt động"
                                                : ("LOCKED".equalsIgnoreCase(st) ? "Đã khóa" : "Tạm ngưng") %>
                                        </span>
                                    </td>
                                    <td>
                                        <div class="staff-actions">
                                            <button type="button" class="staff-edit-btn"
                                                    onclick="openEditStaffModal(<%= item.getUserId() %>, '<%= employeeCode.replace("'", "\\'") %>', '<%= staffName.replace("'", "\\'") %>', '<%= staffPhone.replace("'", "\\'") %>', '<%= staffEmail.replace("'", "\\'") %>', '<%= staffRole.replace("'", "\\'") %>')"
                                                    title="Chỉnh sửa thông tin">
                                                Sửa
                                            </button>

                                            <div class="status-select-wrap">
                                                <select class="cf-status-select val-<%= st.toLowerCase() %>"
                                                        data-userid="<%= item.getUserId() %>"
                                                        data-name="<%= staffName.replace("\"", "&quot;") %>"
                                                        onchange="handleStaffStatusChange(this)"
                                                        title="Thay đổi trạng thái tài khoản">
                                                    <option value="ACTIVE" <%= "ACTIVE".equalsIgnoreCase(st) ? "selected" : "" %>>Hoạt động</option>
                                                    <option value="INACTIVE" <%= "INACTIVE".equalsIgnoreCase(st) ? "selected" : "" %>>Tạm ngưng</option>
                                                    <option value="LOCKED" <%= "LOCKED".equalsIgnoreCase(st) ? "selected" : "" %>>Khóa</option>
                                                </select>
                                                <span class="status-indicator"
                                                      id="status-ind-<%= item.getUserId() %>"
                                                      style="display:none;">✓</span>
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
            </section>
            <% } %>
        </main>
    </div>
</div>

<!-- TẠO NHÂN VIÊN -->
<div class="cf-modal-backdrop" id="createStaffModal">
    <div class="cf-modal" role="dialog" aria-modal="true" aria-labelledby="createStaffTitle">
        <div class="cf-modal-header">
            <h3 id="createStaffTitle">Thêm nhân viên mới<%= !isAdminRole ? (" tại " + currentCentreName) : "" %></h3>
            <button type="button" class="cf-modal-close" onclick="closeCreateStaffModal()" aria-label="Đóng">&times;</button>
        </div>

        <form method="post" action="${pageContext.request.contextPath}/staff">
            <input type="hidden" name="action" value="create">

            <div class="cf-modal-body">
                <% if (isAdminRole) { %>
                    <div class="cf-form-group">
                        <label for="createStaffCentre">Trung tâm / Rạp chiếu trực thuộc *</label>
                        <select id="createStaffCentre" name="centreId" required>
                            <option value="">-- Chọn trung tâm trực thuộc --</option>
                            <% if (centres != null) {
                                for (Centre c : centres) { %>
                                    <option value="<%= c.getCentreId() %>" <%= (selectedCentreId != null && selectedCentreId == c.getCentreId()) ? "selected" : "" %>>
                                        <%= c.getCentreName() %> (<%= c.getCity() %>)
                                    </option>
                            <%   }
                               } %>
                        </select>
                    </div>
                <% } else { %>
                    <input type="hidden" name="centreId" value="<%= assignedCentre != null ? assignedCentre.getCentreId() : "" %>">
                    <p style="font-size:13px;color:#94a3b8;margin:0 0 12px;line-height:1.6;">
                        Tài khoản nhân sự sẽ trực thuộc trung tâm:
                        <strong style="color:#f8fafc;"><%= currentCentreName %></strong>.
                    </p>
                <% } %>

                <div class="cf-form-group">
                    <label for="createStaffRole">Vị trí công việc *</label>
                    <select id="createStaffRole" name="role" required>
                        <option value="TICKET_SALES_STAFF">🎫 Nhân viên bán vé & F&B (Ticket Sales Staff)</option>
                        <option value="TICKET_CHECKER_STAFF">🔍 Nhân viên soát vé tại cửa rạp (Ticket Checker Staff)</option>
                    </select>
                </div>

                <div class="cf-form-group">
                    <label for="createStaffFullName">Họ và tên nhân viên *</label>
                    <input type="text" id="createStaffFullName" name="fullName"
                           placeholder="Ví dụ: Nguyễn Văn B" required>
                </div>

                <div style="display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px;">
                    <div class="cf-form-group">
                        <label for="createStaffAccount">Tên đăng nhập *</label>
                        <input type="text" id="createStaffAccount" name="account"
                               placeholder="staff_td_06" required>
                    </div>
                    <div class="cf-form-group">
                        <label for="createStaffPhone">Số điện thoại</label>
                        <input type="tel" id="createStaffPhone" name="phone"
                               placeholder="09xxxxxxxx">
                    </div>
                </div>

                <div class="cf-form-group">
                    <label for="createStaffEmail">Email *</label>
                    <input type="email" id="createStaffEmail" name="email"
                           placeholder="staff@cineflow.vn" required>
                </div>

                <div class="cf-form-group">
                    <label for="createStaffPassword">Mật khẩu ban đầu *</label>
                    <input type="text" id="createStaffPassword" name="password"
                           value="Staff@123" required>
                    <small style="color:#94a3b8;font-size:12px;line-height:1.5;">
                        Mật khẩu khởi tạo mặc định: <code>Staff@123</code> (tối thiểu 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt).
                    </small>
                </div>
            </div>

            <div class="cf-modal-footer">
                <button type="button" class="cf-btn cf-btn-secondary" onclick="closeCreateStaffModal()">Hủy</button>
                <button type="submit" class="cf-btn cf-btn-primary">Tạo tài khoản</button>
            </div>
        </form>
    </div>
</div>

<!-- CHỈNH SỬA NHÂN VIÊN -->
<div class="cf-modal-backdrop" id="editStaffModal">
    <div class="cf-modal" role="dialog" aria-modal="true" aria-labelledby="editStaffTitle">
        <div class="cf-modal-header">
            <h3 id="editStaffTitle">Chỉnh sửa thông tin nhân viên</h3>
            <button type="button" class="cf-modal-close" onclick="closeEditStaffModal()" aria-label="Đóng">&times;</button>
        </div>

        <form method="post" action="${pageContext.request.contextPath}/staff">
            <input type="hidden" name="action" value="update">
            <input type="hidden" name="userId" id="editStaffUserId">

            <div class="cf-modal-body">
                <div style="display:flex;align-items:center;justify-content:space-between;gap:12px;padding:12px 14px;border:1px solid #303b4b;border-radius:10px;background:#10151d;">
                    <span style="font-size:12px;color:#94a3b8;font-weight:600;">Mã nhân viên</span>
                    <code class="cf-badge cf-badge-info" id="editStaffCode">NV00000</code>
                </div>

                <div class="cf-form-group">
                    <label for="editStaffRole">Vị trí công việc *</label>
                    <select name="role" id="editStaffRole" required>
                        <option value="TICKET_SALES_STAFF">🎫 Nhân viên bán vé & F&B (Ticket Sales Staff)</option>
                        <option value="TICKET_CHECKER_STAFF">🔍 Nhân viên soát vé tại cửa rạp (Ticket Checker Staff)</option>
                    </select>
                </div>

                <div class="cf-form-group">
                    <label for="editStaffFullName">Họ và tên nhân viên *</label>
                    <input type="text" name="fullName" id="editStaffFullName" required>
                </div>

                <div class="cf-form-group">
                    <label for="editStaffPhone">Số điện thoại</label>
                    <input type="tel" name="phone" id="editStaffPhone">
                </div>

                <div class="cf-form-group">
                    <label for="editStaffEmail">Email *</label>
                    <input type="email" name="email" id="editStaffEmail" required>
                </div>
            </div>

            <div class="cf-modal-footer">
                <button type="button" class="cf-btn cf-btn-secondary" onclick="closeEditStaffModal()">Hủy</button>
                <button type="submit" class="cf-btn cf-btn-primary">Lưu thay đổi</button>
            </div>
        </form>
    </div>
</div>

<div id="cf-toast-container" aria-live="polite" aria-atomic="false"></div>

<script>
    function openCreateStaffModal() {
        document.getElementById('createStaffModal').classList.add('active');
    }

    function closeCreateStaffModal() {
        document.getElementById('createStaffModal').classList.remove('active');
    }

    function openEditStaffModal(id, code, fullName, phone, email, role) {
        document.getElementById('editStaffUserId').value = id;
        document.getElementById('editStaffCode').textContent = code;
        document.getElementById('editStaffFullName').value = fullName;
        document.getElementById('editStaffPhone').value = phone;
        document.getElementById('editStaffEmail').value = email;
        var roleSelect = document.getElementById('editStaffRole');
        if (roleSelect && role) {
            roleSelect.value = role;
        }
        document.getElementById('editStaffModal').classList.add('active');
    }

    function closeEditStaffModal() {
        document.getElementById('editStaffModal').classList.remove('active');
    }

    document.querySelectorAll('.cf-modal-backdrop').forEach(function(modal) {
        modal.addEventListener('click', function(event) {
            if (event.target === modal) {
                modal.classList.remove('active');
            }
        });
    });

    document.addEventListener('keydown', function(event) {
        if (event.key === 'Escape') {
            closeCreateStaffModal();
            closeEditStaffModal();
        }
    });

    document.addEventListener('DOMContentLoaded', function() {
        var searchInput = document.getElementById('staffFilterInput');
        var roleFilter = document.getElementById('staffRoleFilter');
        var statusFilter = document.getElementById('staffStatusFilter');
        var rows = document.querySelectorAll('.staff-row');
        var countLabel = document.getElementById('staffCountLabel');

        function filterRows() {
            var query = searchInput ? searchInput.value.toLowerCase().trim() : '';
            var roleVal = roleFilter ? roleFilter.value : '';
            var statusVal = statusFilter ? statusFilter.value : '';
            var visible = 0;

            rows.forEach(function(row) {
                var searchVal = row.getAttribute('data-search') || '';
                var matchText = !query || searchVal.indexOf(query) !== -1;
                var matchRole = !roleVal || row.classList.contains(roleVal);
                var matchStatus = !statusVal || row.classList.contains(statusVal);
                var showRow = matchText && matchRole && matchStatus;

                row.style.display = showRow ? '' : 'none';
                if (showRow) visible++;
            });

            if (countLabel) {
                countLabel.textContent = visible + ' tài khoản đang hiển thị.';
            }
        }

        if (searchInput) searchInput.addEventListener('input', filterRows);
        if (roleFilter) roleFilter.addEventListener('change', filterRows);
        if (statusFilter) statusFilter.addEventListener('change', filterRows);

        document.querySelectorAll('.cf-alert-success, .cf-alert-danger').forEach(function(alert) {
            setTimeout(function() {
                alert.style.transition = 'opacity .4s ease, transform .4s ease';
                alert.style.opacity = '0';
                alert.style.transform = 'translateY(-8px)';

                setTimeout(function() {
                    if (alert.parentNode) alert.parentNode.removeChild(alert);
                }, 400);
            }, 3000);
        });
    });

    function handleStaffStatusChange(selectEl) {
        var userId = selectEl.getAttribute('data-userid');
        var staffName = selectEl.getAttribute('data-name') || 'nhân viên';
        var newStatus = selectEl.value;
        var previousStatus = selectEl.getAttribute('data-previous-status') || '';
        var ind = document.getElementById('status-ind-' + userId);
        var badge = document.getElementById('badge-status-' + userId);
        var row = selectEl.closest('.staff-row');

        if (!previousStatus) {
            previousStatus = selectEl.querySelector('option[selected]') ?
                selectEl.querySelector('option[selected]').value : newStatus;
        }

        selectEl.disabled = true;

        if (ind) {
            ind.textContent = '…';
            ind.style.display = 'inline';
            ind.style.color = '#94a3b8';
        }

        var formData = new URLSearchParams();
        formData.append('action', 'status');
        formData.append('ajax', '1');
        formData.append('userId', userId);
        formData.append('status', newStatus);

        fetch('${pageContext.request.contextPath}/staff', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                'X-Requested-With': 'XMLHttpRequest'
            },
            body: formData.toString()
        })
        .then(function(response) {
            if (!response.ok) throw new Error('HTTP ' + response.status);
            return response.json();
        })
        .then(function(data) {
            if (!data.success) throw new Error(data.message || 'Lỗi xử lý');

            selectEl.setAttribute('data-previous-status', newStatus);
            selectEl.className = 'cf-status-select val-' + newStatus.toLowerCase();

            if (badge) {
                if (newStatus === 'ACTIVE') {
                    badge.className = 'cf-badge cf-badge-success';
                    badge.textContent = 'Đang hoạt động';
                } else if (newStatus === 'INACTIVE') {
                    badge.className = 'cf-badge cf-badge-warning';
                    badge.textContent = 'Tạm ngưng';
                } else {
                    badge.className = 'cf-badge cf-badge-danger';
                    badge.textContent = 'Đã khóa';
                }
            }

            if (row) {
                row.classList.remove('st-active', 'st-inactive', 'st-locked');
                row.classList.add('st-' + newStatus.toLowerCase());
            }

            if (ind) {
                ind.textContent = '✓';
                ind.style.color = '#6ee7b7';
                ind.style.display = 'inline';

                setTimeout(function() {
                    ind.style.display = 'none';
                }, 3000);
            }

            var statusLabel = newStatus === 'ACTIVE' ? 'Hoạt động'
                : (newStatus === 'INACTIVE' ? 'Tạm ngưng' : 'Đã khóa');

            showToast('Đã cập nhật trạng thái của ' + staffName + ' thành "' + statusLabel + '".', 'success');
        })
        .catch(function(error) {
            if (previousStatus) selectEl.value = previousStatus;
            showToast('Cập nhật trạng thái thất bại. Vui lòng thử lại.', 'error');
        })
        .finally(function() {
            selectEl.disabled = false;
            if (ind && ind.textContent === '…') ind.style.display = 'none';
        });
    }

    function showToast(message, type) {
        var container = document.getElementById('cf-toast-container');
        if (!container) return;

        var toast = document.createElement('div');
        toast.className = 'cf-toast' + (type === 'error' ? ' cf-toast-error' : '');
        toast.textContent = message;
        container.appendChild(toast);

        setTimeout(function() {
            toast.classList.add('cf-toast-fadeout');

            setTimeout(function() {
                if (toast.parentNode) toast.parentNode.removeChild(toast);
            }, 400);
        }, 3000);
    }
</script>
</body>
</html>
