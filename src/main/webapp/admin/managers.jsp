<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.User,java.util.List" %>
<%
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && session.getAttribute("user") instanceof model.User) {
        authRole = ((model.User) session.getAttribute("user")).getRole();
    }
    if (!"ADMIN".equalsIgnoreCase(authRole)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Quản trị viên (ADMIN) mới có quyền truy cập trang này.");
        return;
    }

    List<User> managers = (List<User>) request.getAttribute("managers");

    request.setAttribute("activeNav", "managers");
    request.setAttribute("pageTitle", "Tài khoản Quản lý");
    request.setAttribute("pageBreadcrumb", "Hệ thống");

    int totalManagers = managers != null ? managers.size() : 0;
    int activeCount = 0;
    int inactiveCount = 0;

    if (managers != null) {
        for (User m : managers) {
            if ("ACTIVE".equalsIgnoreCase(m.getStatus())) {
                activeCount++;
            } else {
                inactiveCount++;
            }
        }
    }

    String successParam = request.getParameter("success");
    String errorParam = request.getParameter("error");
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
        .cf-form-group input {
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
        .cf-form-group input:focus {
            border-color: #FF2855;
            background: #ffffff;
            box-shadow: 0 0 0 3px rgba(255, 40, 85, 0.2);
        }
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
                    <h1>Quản lý Tài khoản Quản lý (Manager)</h1>
                    <p>Khởi tạo tài khoản, cập nhật thông tin cá nhân và quản lý trạng thái hoạt động của Quản lý.</p>
                </div>
                <button type="button" class="cf-btn cf-btn-primary" onclick="openCreateManagerModal()" style="display: flex; align-items: center; gap: 8px;">
                    <span>+</span> Tạo tài khoản Manager mới
                </button>
            </div>

            <% if ("created".equals(successParam)) { %>
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> <strong>Thành công!</strong> Đã tạo mới tài khoản Quản lý (Manager). Việc phân công cụm rạp được thực hiện tại màn hình <em>Bổ nhiệm & Điều chuyển</em>.
                </div>
            <% } else if ("updated".equals(successParam)) { %>
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> <strong>Thành công!</strong> Đã cập nhật thông tin Quản lý (Manager) thành công.
                </div>
            <% } else if ("status_updated".equals(successParam)) { %>
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> Đã cập nhật trạng thái hoạt động của tài khoản Manager.
                </div>
            <% } else if ("account_exists".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Tên tài khoản (Account) đã tồn tại trong hệ thống.
                </div>
            <% } else if ("email_exists".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Địa chỉ Email đã được sử dụng bởi người dùng khác.
                </div>
            <% } else if ("missing_fields".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Vui lòng điền đầy đủ các thông tin bắt buộc.
                </div>
            <% } else if (errorParam != null) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> Thao tác thất bại. Vui lòng kiểm tra lại thông tin.
                </div>
            <% } %>

            <!-- KPI METRICS: TỔNG SỐ, ĐANG HOẠT ĐỘNG, TẠM NGƯNG -->
            <div class="cf-kpi-grid">
                <div class="cf-kpi-card">
                    <span class="cf-kpi-label">Tổng số Manager</span>
                    <div class="cf-kpi-value"><%= totalManagers %></div>
                </div>
                <div class="cf-kpi-card">
                    <span class="cf-kpi-label">Đang hoạt động</span>
                    <div class="cf-kpi-value" style="color: #059669;"><%= activeCount %></div>
                </div>
                <div class="cf-kpi-card">
                    <span class="cf-kpi-label">Tạm ngưng</span>
                    <div class="cf-kpi-value" style="color: #d97706;"><%= inactiveCount %></div>
                </div>
            </div>

            <!-- TABLE CARD -->
            <section class="cf-card" style="margin-top: 24px;">
                <div class="cf-card-header" style="display: flex; justify-content: space-between; align-items: center; gap: 16px; flex-wrap: wrap;">
                    <div>
                        <h2>Danh sách Quản lý (Manager Directory)</h2>
                        <p id="managerCountLabel"><%= totalManagers %> tài khoản trong hệ thống.</p>
                    </div>
                    <div style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap;">
                        <form method="get" action="${pageContext.request.contextPath}/admin/managers" style="display: flex; gap: 8px; align-items: center;">
                            <input type="text" name="search" id="managerFilterInput" 
                                   value="<%= request.getAttribute("searchParam") != null ? request.getAttribute("searchParam") : "" %>"
                                   placeholder="🔍 Tìm theo Mã nhân sự (VD: QL00003, 3), username, họ tên, rạp..." 
                                   style="padding: 8px 14px; border: 1px solid #334155; border-radius: 6px; font-size: 13px; min-width: 280px; background: #1e293b; color: #f8fafc;">
                            <button type="submit" class="cf-btn cf-btn-secondary cf-btn-sm">Tìm</button>
                            <% if (request.getAttribute("searchParam") != null && !((String)request.getAttribute("searchParam")).trim().isEmpty()) { %>
                                <a href="${pageContext.request.contextPath}/admin/managers" class="cf-btn cf-btn-secondary cf-btn-sm" style="color: #94a3b8;">Xóa lọc</a>
                            <% } %>
                        </form>
                        
                        <select id="statusFilterSelect" style="padding: 8px 12px; border: 1px solid #334155; border-radius: 6px; font-size: 13px; background: #1e293b; color: #f8fafc;">
                            <option value="">Tất cả trạng thái</option>
                            <option value="st-active">Đang hoạt động</option>
                            <option value="st-inactive">Tạm ngưng</option>
                        </select>
                    </div>
                </div>
                <div class="cf-card-body">
                    <div class="cf-table-wrap">
                        <table class="cf-table">
                            <thead>
                                <tr>
                                    <th>Mã nhân sự</th>
                                    <th>Quản lý</th>
                                    <th>Tên đăng nhập</th>
                                    <th>Số điện thoại</th>
                                    <th>Chi nhánh phụ trách</th>
                                    <th>Trạng thái tài khoản</th>
                                    <th>Thao tác</th>
                                </tr>
                            </thead>
                            <tbody>
                                <% if (managers == null || managers.isEmpty()) { %>
                                    <tr><td colspan="7" class="cf-empty">Chưa có tài khoản Manager nào trong hệ thống.</td></tr>
                                <% } else for (User m : managers) {
                                    String st = m.getStatus();
                                    boolean isActive = "ACTIVE".equalsIgnoreCase(st);
                                    String branchName = m.getBranchName();
                                    boolean hasBranch = branchName != null && !branchName.trim().isEmpty();
                                    String searchData = (m.getEmployeeCode() + " " + m.getUserId() + " ql" + m.getUserId() + " " + m.getFullName() + " " + m.getAccount() + " " + m.getEmail() + " " + (hasBranch ? branchName : "")).toLowerCase();
                                    String statusClass = isActive ? "st-active" : "st-inactive";
                                %>
                                    <tr class="manager-row <%= statusClass %>" data-search="<%= searchData %>">
                                        <td><code class="cf-badge cf-badge-primary" style="font-size: 12px; font-weight: 700; letter-spacing: 0.5px;"><%= m.getEmployeeCode() %></code></td>
                                        <td>
                                            <strong style="font-size: 14px; color: #fff;"><%= m.getFullName() %></strong>
                                            <small style="display: block; color: #94a3b8;"><%= m.getEmail() %></small>
                                        </td>
                                        <td><code><%= m.getAccount() %></code></td>
                                        <td><%= m.getPhone() != null && !m.getPhone().isEmpty() ? m.getPhone() : "—" %></td>
                                        <td>
                                            <% if (hasBranch) { %>
                                                <span style="font-weight: 500; color: #a5b4fc;">🏛 <%= branchName %></span>
                                            <% } else { %>
                                                <span style="color: #64748b;">Chưa phân công</span>
                                            <% } %>
                                        </td>
                                        <td>
                                            <span class="cf-badge <%= isActive ? "cf-badge-success" : "cf-badge-warning" %>">
                                                <%= isActive ? "Đang hoạt động" : "Tạm ngưng" %>
                                            </span>
                                        </td>
                                        <td>
                                            <div style="display: flex; gap: 6px; align-items: center; flex-wrap: wrap;">
                                                <!-- Nút Sửa thông tin tài khoản -->
                                                <button type="button" class="cf-btn cf-btn-secondary cf-btn-sm" 
                                                        onclick="openEditManagerModal(<%= m.getUserId() %>, '<%= m.getEmployeeCode() %>', '<%= m.getFullName().replace("'", "\\'") %>', '<%= m.getPhone() != null ? m.getPhone().replace("'", "\\'") : "" %>', '<%= m.getEmail().replace("'", "\\'") %>')"
                                                        title="Chỉnh sửa thông tin cá nhân">
                                                    Sửa
                                                </button>

                                                <!-- Nút Kích hoạt / Tạm ngưng tài khoản -->
                                                <form method="post" action="${pageContext.request.contextPath}/admin/managers" style="display: inline;">
                                                    <input type="hidden" name="action" value="toggle_status">
                                                    <input type="hidden" name="userId" value="<%= m.getUserId() %>">
                                                    <% if (isActive) { %>
                                                        <input type="hidden" name="status" value="INACTIVE">
                                                        <button type="submit" class="cf-btn cf-btn-secondary cf-btn-sm" style="color: #f59e0b;" 
                                                                onclick="return confirm('Bạn có chắc muốn TẠM NGƯNG tài khoản Quản lý này? Lưu ý: Việc này không làm gián đoạn trạng thái hoạt động của rạp.');">
                                                            Tạm ngưng
                                                        </button>
                                                    <% } else { %>
                                                        <input type="hidden" name="status" value="ACTIVE">
                                                        <button type="submit" class="cf-btn cf-btn-secondary cf-btn-sm" style="color: #4ade80;">
                                                            Kích hoạt
                                                        </button>
                                                    <% } %>
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
        </main>
    </div>
</div>

<!-- MODAL 1: TẠO TÀI KHOẢN MANAGER MỚI -->
<div class="cf-modal-backdrop" id="createManagerModal">
    <div class="cf-modal">
        <div class="cf-modal-header">
            <h3>Tạo tài khoản Manager mới</h3>
            <button type="button" class="cf-modal-close" onclick="closeCreateManagerModal()">&times;</button>
        </div>
        <form method="post" action="${pageContext.request.contextPath}/admin/managers">
            <input type="hidden" name="action" value="create">
            <div class="cf-modal-body">
                <p style="font-size: 13px; color: #64748B; margin: 0 0 6px;">
                    Tài khoản được tạo độc lập. Việc bổ nhiệm Manager vào cụm rạp được thực hiện riêng tại trang <strong>Bổ nhiệm & Điều chuyển</strong>.
                </p>
                <div class="cf-form-group">
                    <label>Họ và tên Quản lý *</label>
                    <input type="text" name="fullName" placeholder="Ví dụ: Hoàng Tuấn Anh" required>
                </div>
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
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
                    <input type="password" name="password" value="123456" required>
                    <small style="color: #64748b; font-size: 12px;">Mặc định: 123456</small>
                </div>
            </div>
            <div class="cf-modal-footer">
                <button type="button" class="cf-btn cf-btn-secondary" onclick="closeCreateManagerModal()">Hủy</button>
                <button type="submit" class="cf-btn cf-btn-primary">Tạo tài khoản Manager</button>
            </div>
        </form>
    </div>
</div>

<!-- MODAL 2: CHỈNH SỬA THÔNG TIN CÁ NHÂN MANAGER -->
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
                <div style="background: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 8px; padding: 10px 14px; margin-bottom: 4px; display: flex; align-items: center; justify-content: space-between;">
                    <span style="font-size: 13px; color: #64748B; font-weight: 500;">Mã Quản lý (ID):</span>
                    <code class="cf-badge cf-badge-primary" style="font-size: 13px; font-weight: 700;" id="editManagerCode">QL00000</code>
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

<script>
    function openCreateManagerModal() {
        document.getElementById('createManagerModal').classList.add('active');
    }
    function closeCreateManagerModal() {
        document.getElementById('createManagerModal').classList.remove('active');
    }

    function openEditManagerModal(id, code, fullName, phone, email) {
        document.getElementById('editUserId').value = id;
        document.getElementById('editManagerCode').textContent = code;
        document.getElementById('editFullName').value = fullName;
        document.getElementById('editPhone').value = phone;
        document.getElementById('editEmail').value = email;
        document.getElementById('editManagerModal').classList.add('active');
    }
    function closeEditManagerModal() {
        document.getElementById('editManagerModal').classList.remove('active');
    }

    document.querySelectorAll('.cf-modal-backdrop').forEach(function(modal) {
        modal.addEventListener('click', function(e) {
            if (e.target === this) {
                this.classList.remove('active');
            }
        });
    });

    document.addEventListener('DOMContentLoaded', function() {
        var input = document.getElementById('managerFilterInput');
        var statusSelect = document.getElementById('statusFilterSelect');
        var rows = document.querySelectorAll('.manager-row');
        var label = document.getElementById('managerCountLabel');

        function applyFilter() {
            var q = input ? input.value.toLowerCase().trim() : '';
            var statusVal = statusSelect ? statusSelect.value : '';
            var count = 0;

            rows.forEach(function(r) {
                var s = r.getAttribute('data-search') || '';
                var matchSearch = !q || s.indexOf(q) !== -1;
                var matchStatus = true;

                if (statusVal) {
                    matchStatus = r.classList.contains(statusVal);
                }

                if (matchSearch && matchStatus) {
                    r.style.display = '';
                    count++;
                } else {
                    r.style.display = 'none';
                }
            });

            if (label) label.textContent = count + ' tài khoản đang hiển thị.';
        }

        if (input) input.addEventListener('input', applyFilter);
        if (statusSelect) statusSelect.addEventListener('change', applyFilter);
    });
</script>
</body>
</html>
