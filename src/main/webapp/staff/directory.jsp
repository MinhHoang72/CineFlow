<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.User,model.Centre,java.util.List" %>
<%
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && session.getAttribute("user") instanceof model.User) {
        authRole = ((model.User) session.getAttribute("user")).getRole();
    }
    if (!"MANAGER".equalsIgnoreCase(authRole) && !"ADMIN".equalsIgnoreCase(authRole)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập trang quản lý nhân sự.");
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

    String currentCentreName = assignedCentre != null ? assignedCentre.getCentreName() : (selectedCentreId == null ? "Chưa bổ nhiệm trung tâm" : "Toàn hệ thống");

    String successParam = request.getParameter("success");
    String errorParam = request.getParameter("error");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Quản lý Nhân sự | CineFlow</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/reset.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/variables.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/buttons.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/badges.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/forms.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/tables.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/shell.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/staff.css">
    <style>
        .cf-alert {
            padding: 12px 16px;
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
        .cf-modal-backdrop.active {
            display: flex;
        }
        .cf-modal {
            background: #ffffff;
            border: 1px solid #E2E8F0;
            border-radius: 14px;
            width: 100%;
            max-width: 520px;
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
        .cf-modal-header h3 {
            margin: 0;
            font-size: 18px;
            color: #111827;
            font-weight: 700;
        }
        .cf-modal-close {
            background: none;
            border: none;
            color: #64748B;
            font-size: 24px;
            cursor: pointer;
            line-height: 1;
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
        .cf-form-group input, .cf-form-group select {
            background: #F1F5F9;
            border: 1px solid #CBD5E1;
            border-radius: 8px;
            padding: 9px 14px;
            color: #1E293B;
            font-size: 14px;
            outline: none;
            transition: all 0.2s;
        }
        .cf-form-group input::placeholder {
            color: #64748B;
            opacity: 1;
        }
        .cf-form-group select option {
            background: #ffffff;
            color: #1E293B;
        }
        .cf-form-group input:focus, .cf-form-group select:focus {
            border-color: #FF2855;
            background: #ffffff;
            box-shadow: 0 0 0 3px rgba(255, 40, 85, 0.2);
        }
        .cf-modal-footer {
            padding: 16px 24px;
            border-top: 1px solid #E2E8F0;
            display: flex;
            justify-content: flex-end;
            gap: 12px;
            background: #F8FAFC;
        }
    </style>
</head>
<body>
    <div class="cf-manager-shell">
        <% if (isAdminRole) { %>
            <jsp:include page="/admin/sidebar.jsp"/>
        <% } else { %>
            <jsp:include page="/manager/sidebar.jsp"/>
        <% } %>
        <div class="cf-manager-main">
            <% if (isAdminRole) { %>
                <jsp:include page="/admin/topbar.jsp"/>
            <% } else { %>
                <jsp:include page="/manager/topbar.jsp"/>
            <% } %>
            <main class="cf-content">
                <div class="cf-page-header">
                    <div>
                        <p class="cf-breadcrumb">Nhân sự / Danh sách nhân viên</p>
                        <h1>Nhân sự <%= currentCentreName %></h1>
                        <p>Quản lý tài khoản, tạo nhân viên, cập nhật thông tin và kiểm soát trạng thái hoạt động tại trung tâm.</p>
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
                        <span>⚠</span> <strong>Lỗi:</strong> Tên tài khoản (Username) đã tồn tại trong hệ thống.
                    </div>
                <% } else if ("email_exists".equals(errorParam)) { %>
                    <div class="cf-alert cf-alert-danger">
                        <span>⚠</span> <strong>Lỗi:</strong> Địa chỉ Email này đã được sử dụng bởi người dùng khác.
                    </div>
                <% } else if ("missing_fields".equals(errorParam)) { %>
                    <div class="cf-alert cf-alert-danger">
                        <span>⚠</span> Vui lòng điền đầy đủ các thông tin bắt buộc.
                    </div>
                <% } else if ("unassigned_manager".equals(errorParam)) { %>
                    <div class="cf-alert cf-alert-danger">
                        <span>⚠</span> <strong>Lỗi:</strong> Tài khoản Quản lý chưa được bổ nhiệm vào trung tâm nên không thể tạo nhân viên.
                    </div>
                <% } else if (errorParam != null) { %>
                    <div class="cf-alert cf-alert-danger">
                        <span>⚠</span> Thao tác không thành công. Vui lòng kiểm tra lại thông tin.
                    </div>
                <% } %>

                <% if (selectedCentreId == null && !isAdminRole) { %>
                    <section class="cf-card" style="padding: 40px 24px; text-align: center; margin-top: 20px;">
                        <div style="font-size: 44px; margin-bottom: 14px;">🏛</div>
                        <h2 style="color: #f59e0b; margin-bottom: 8px;">Tài khoản chưa được bổ nhiệm trung tâm</h2>
                        <p style="color: #94a3b8; max-width: 540px; margin: 0 auto 16px; font-size: 14px; line-height: 1.6;">
                            Tài khoản Quản lý của bạn hiện chưa được phân công phụ trách trung tâm chiếu phim nào trong hệ thống CineFlow.
                            Theo quy định bảo mật, Quản lý chỉ được tạo và quản lý nhân viên khi đã được bổ nhiệm chính thức vào một trung tâm.
                        </p>
                        <p style="font-size: 13px; color: #cbd5e1;">Vui lòng liên hệ Quản trị viên hệ thống (Admin HQ) để được phân công trung tâm.</p>
                    </section>
                <% } else { %>

                <section class="cf-card" style="margin-top: 20px;">
                    <div class="cf-card-header" style="display: flex; justify-content: space-between; align-items: center; gap: 16px; flex-wrap: wrap;">
                        <div>
                            <h2>Danh sách nhân viên</h2>
                            <p id="staffCountLabel"><%= staff == null ? 0 : staff.size() %> tài khoản đang hiển thị.</p>
                        </div>
                        <div style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap;">
                            <form method="get" action="${pageContext.request.contextPath}/staff" style="display: flex; gap: 8px; align-items: center;">
                                <% 
                                    String cFilterVal = request.getParameter("centreId");
                                    if (cFilterVal != null) { 
                                %>
                                    <input type="hidden" name="centreId" value="<%= cFilterVal %>">
                                <% } %>
                                <input type="text" name="search" id="staffFilterInput" 
                                       value="<%= request.getParameter("search") != null ? request.getParameter("search") : "" %>"
                                       placeholder="🔍 Tìm theo Mã nhân sự (VD: NV00012, 12), username, họ tên, SĐT..." 
                                       style="padding: 8px 14px; border: 1px solid #334155; border-radius: 6px; font-size: 13px; min-width: 280px; background: #1e293b; color: #f8fafc;">
                                <button type="submit" class="cf-btn cf-btn-secondary cf-btn-sm">Tìm</button>
                                <% if (request.getParameter("search") != null && !request.getParameter("search").trim().isEmpty()) { %>
                                    <a href="${pageContext.request.contextPath}/staff<%= cFilterVal != null ? "?centreId=" + cFilterVal : "" %>" class="cf-btn cf-btn-secondary cf-btn-sm" style="color: #94a3b8;">Xóa lọc</a>
                                <% } %>
                            </form>
                            
                            <select id="staffStatusFilter" style="padding: 8px 12px; border: 1px solid #334155; border-radius: 6px; font-size: 13px; background: #1e293b; color: #f8fafc;">
                                <option value="">Tất cả trạng thái</option>
                                <option value="st-active">Đang hoạt động</option>
                                <option value="st-inactive">Tạm ngưng</option>
                                <option value="st-locked">Đã khóa</option>
                            </select>

                            <button type="button" class="cf-btn cf-btn-primary" onclick="openCreateStaffModal()" style="display: flex; align-items: center; gap: 6px;">
                                <span>+</span> Thêm nhân viên
                            </button>
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
                                        <th>Số điện thoại</th>
                                        <th>Trung tâm</th>
                                        <th>Trạng thái</th>
                                        <th>Thao tác</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <% if (staff == null || staff.isEmpty()) { %>
                                        <tr><td colspan="7" class="cf-empty">Chưa có nhân viên nào phù hợp với Mã NV hoặc từ khóa tìm kiếm.</td></tr>
                                    <% } else for (User item : staff) {
                                        String st = item.getStatus();
                                        String staffName = item.getFullName() != null ? item.getFullName() : "";
                                        String staffEmail = item.getEmail() != null ? item.getEmail() : "";
                                        String staffAccount = item.getAccount() != null ? item.getAccount() : "";
                                        String staffPhone = item.getPhone() != null ? item.getPhone() : "";
                                        String searchVal = (item.getEmployeeCode() + " " + item.getUserId() + " nv" + item.getUserId() + " " + staffName + " " + staffEmail + " " + staffAccount + " " + staffPhone).toLowerCase();
                                        String stClass = "ACTIVE".equalsIgnoreCase(st) ? "st-active" : ("INACTIVE".equalsIgnoreCase(st) ? "st-inactive" : "st-locked");
                                        String itemCentreName = item.getCentreName() != null ? item.getCentreName() : currentCentreName;
                                    %>
                                        <tr class="staff-row <%= stClass %>" data-search="<%= searchVal %>">
                                            <td><code class="cf-badge cf-badge-info" style="font-size: 12px; font-weight: 700; letter-spacing: 0.5px;"><%= item.getEmployeeCode() %></code></td>
                                            <td>
                                                <strong style="color: #fff; font-size: 14px;"><%= staffName %></strong>
                                                <small style="display: block; color: #94a3b8;"><%= staffEmail %></small>
                                            </td>
                                            <td><code><%= staffAccount %></code></td>
                                            <td><%= !staffPhone.isEmpty() ? staffPhone : "—" %></td>
                                            <td>🏛 <%= itemCentreName %></td>
                                            <td>
                                                <span class="cf-badge <%= "ACTIVE".equalsIgnoreCase(st) ? "cf-badge-success" : ("LOCKED".equalsIgnoreCase(st) ? "cf-badge-danger" : "cf-badge-warning") %>">
                                                    <%= "ACTIVE".equalsIgnoreCase(st) ? "Đang hoạt động" : ("LOCKED".equalsIgnoreCase(st) ? "Đã khóa" : "Tạm ngưng") %>
                                                </span>
                                            </td>
                                            <td>
                                                <div style="display: flex; gap: 8px; align-items: center; flex-wrap: wrap;">
                                                    <!-- Nút sửa thông tin -->
                                                    <button type="button" class="cf-btn cf-btn-secondary cf-btn-sm" 
                                                            onclick="openEditStaffModal(<%= item.getUserId() %>, '<%= item.getEmployeeCode() %>', '<%= staffName.replace("'", "\\'") %>', '<%= staffPhone.replace("'", "\\'") %>', '<%= staffEmail.replace("'", "\\'") %>')"
                                                            title="Chỉnh sửa thông tin">
                                                        Sửa
                                                    </button>

                                                    <!-- Form cập nhật trạng thái -->
                                                    <form method="post" action="${pageContext.request.contextPath}/staff" style="display:flex;gap:6px;align-items:center;">
                                                        <input type="hidden" name="action" value="status">
                                                        <input type="hidden" name="userId" value="<%= item.getUserId() %>">
                                                        <select name="status" style="background: rgba(255,255,255,0.08); color: #fff; border: 1px solid rgba(255,255,255,0.15); border-radius: 4px; padding: 4px 8px; font-size: 13px;">
                                                             <option value="ACTIVE" <%= "ACTIVE".equalsIgnoreCase(st) ? "selected" : "" %>>Hoạt động</option>
                                                             <option value="INACTIVE" <%= "INACTIVE".equalsIgnoreCase(st) ? "selected" : "" %>>Tạm ngưng</option>
                                                             <option value="LOCKED" <%= "LOCKED".equalsIgnoreCase(st) ? "selected" : "" %>>Khóa</option>
                                                        </select>
                                                        <button class="cf-btn cf-btn-primary cf-btn-sm" type="submit">Lưu</button>
                                                    </form>
                                                </div>
                                            </td>
                                        </tr>
                                    <% } %>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </section>
                <% } %>
            </main>
        </div>
    </div>

    <!-- MODAL 1: TẠO NHÂN VIÊN MỚI -->
    <div class="cf-modal-backdrop" id="createStaffModal">
        <div class="cf-modal">
            <div class="cf-modal-header">
                <h3>Thêm nhân viên mới tại <%= currentCentreName %></h3>
                <button type="button" class="cf-modal-close" onclick="closeCreateStaffModal()">&times;</button>
            </div>
            <form method="post" action="${pageContext.request.contextPath}/staff">
                <input type="hidden" name="action" value="create">
                <div class="cf-modal-body">
                    <p style="font-size: 13px; color: #94a3b8; margin: 0 0 8px;">
                        Tài khoản được tạo sẽ tự động được gán vai trò <strong>STAFF</strong> và trực thuộc trung tâm <strong><%= currentCentreName %></strong>.
                    </p>
                    <div class="cf-form-group">
                        <label>Họ và tên nhân viên *</label>
                        <input type="text" name="fullName" placeholder="Ví dụ: Nguyễn Văn B" required>
                    </div>
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
                        <div class="cf-form-group">
                            <label>Tên đăng nhập (Account) *</label>
                            <input type="text" name="account" placeholder="staff_cg_01" required>
                        </div>
                        <div class="cf-form-group">
                            <label>Số điện thoại</label>
                            <input type="tel" name="phone" placeholder="09xxxxxxxx">
                        </div>
                    </div>
                    <div class="cf-form-group">
                        <label>Email *</label>
                        <input type="email" name="email" placeholder="staff@cineflow.com" required>
                    </div>
                    <div class="cf-form-group">
                        <label>Mật khẩu ban đầu *</label>
                        <input type="password" name="password" value="123456" placeholder="Mật khẩu khởi tạo" required>
                        <small style="color: #64748b; font-size: 12px;">Mặc định: 123456 (nhân viên có thể đổi sau khi đăng nhập).</small>
                    </div>
                </div>
                <div class="cf-modal-footer">
                    <button type="button" class="cf-btn cf-btn-secondary" onclick="closeCreateStaffModal()">Hủy</button>
                    <button type="submit" class="cf-btn cf-btn-primary">Tạo tài khoản</button>
                </div>
            </form>
        </div>
    </div>

    <!-- MODAL 2: CHỈNH SỬA THÔNG TIN NHÂN VIÊN -->
    <div class="cf-modal-backdrop" id="editStaffModal">
        <div class="cf-modal">
            <div class="cf-modal-header">
                <h3>Chỉnh sửa thông tin nhân viên</h3>
                <button type="button" class="cf-modal-close" onclick="closeEditStaffModal()">&times;</button>
            </div>
            <form method="post" action="${pageContext.request.contextPath}/staff">
                <input type="hidden" name="action" value="update">
                <input type="hidden" name="userId" id="editStaffUserId">
                <div class="cf-modal-body">
                    <div style="background: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 8px; padding: 10px 14px; margin-bottom: 4px; display: flex; align-items: center; justify-content: space-between;">
                        <span style="font-size: 13px; color: #64748B; font-weight: 500;">Mã nhân viên (ID):</span>
                        <code class="cf-badge cf-badge-info" style="font-size: 13px; font-weight: 700;" id="editStaffCode">NV00000</code>
                    </div>
                    <div class="cf-form-group">
                        <label>Họ và tên nhân viên *</label>
                        <input type="text" name="fullName" id="editStaffFullName" required>
                    </div>
                    <div class="cf-form-group">
                        <label>Số điện thoại</label>
                        <input type="tel" name="phone" id="editStaffPhone">
                    </div>
                    <div class="cf-form-group">
                        <label>Email *</label>
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

    <script>
        function openCreateStaffModal() {
            document.getElementById('createStaffModal').classList.add('active');
        }
        function closeCreateStaffModal() {
            document.getElementById('createStaffModal').classList.remove('active');
        }

        function openEditStaffModal(id, code, fullName, phone, email) {
            document.getElementById('editStaffUserId').value = id;
            document.getElementById('editStaffCode').textContent = code;
            document.getElementById('editStaffFullName').value = fullName;
            document.getElementById('editStaffPhone').value = phone;
            document.getElementById('editStaffEmail').value = email;
            document.getElementById('editStaffModal').classList.add('active');
        }
        function closeEditStaffModal() {
            document.getElementById('editStaffModal').classList.remove('active');
        }

        document.querySelectorAll('.cf-modal-backdrop').forEach(function(modal) {
            modal.addEventListener('click', function(e) {
                if (e.target === this) {
                    this.classList.remove('active');
                }
            });
        });

        document.addEventListener('DOMContentLoaded', function() {
            var searchInput = document.getElementById('staffFilterInput');
            var statusFilter = document.getElementById('staffStatusFilter');
            var rows = document.querySelectorAll('.staff-row');
            var countLabel = document.getElementById('staffCountLabel');

            function filterRows() {
                var query = searchInput ? searchInput.value.toLowerCase().trim() : '';
                var statusVal = statusFilter ? statusFilter.value : '';
                var visible = 0;

                rows.forEach(function(r) {
                    var searchVal = r.getAttribute('data-search') || '';
                    var matchText = !query || searchVal.indexOf(query) !== -1;
                    var matchStatus = !statusVal || r.classList.contains(statusVal);

                    if (matchText && matchStatus) {
                        r.style.display = '';
                        visible++;
                    } else {
                        r.style.display = 'none';
                    }
                });
                if (countLabel) {
                    countLabel.textContent = visible + ' tài khoản đang hiển thị.';
                }
            }

            if (searchInput) searchInput.addEventListener('input', filterRows);
            if (statusFilter) statusFilter.addEventListener('change', filterRows);
        });
    </script>
</body>
</html>
