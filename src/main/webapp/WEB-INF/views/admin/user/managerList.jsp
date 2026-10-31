<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.entity.User,java.util.List,model.entity.Centre" %>
<%
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && session.getAttribute("user") instanceof model.entity.User) {
        authRole = ((model.entity.User) session.getAttribute("user")).getRole();
    }
    if (!"ADMIN".equalsIgnoreCase(authRole)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Quản trị viên (ADMIN) mới có quyền truy cập trang này.");
        return;
    }

    List<User> managers = (List<User>) request.getAttribute("managers");
    List<Centre> centres = (List<Centre>) request.getAttribute("centres");

    request.setAttribute("activeNav", "managers");
    request.setAttribute("pageTitle", "Tài khoản Quản lý");

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
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/managers.css?v=<%= System.currentTimeMillis() %>">
</head>
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

            <% if ("created".equals(successParam)) { %>
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> <strong>Thành công!</strong> Đã tạo mới tài khoản Quản lý và phân công phụ trách cụm rạp thành công.
                </div>
            <% } else if ("updated".equals(successParam)) { %>
                <div class="cf-alert cf-alert-success">
                    <span></span> <strong>Thành công!</strong> Đã cập nhật thông tin Quản lý (Manager) thành công.
                </div>
            <% } else if ("status_updated".equals(successParam)) { %>
                <div class="cf-alert cf-alert-success">
                    <span></span> Đã cập nhật trạng thái hoạt động của tài khoản Manager.
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
            <% } else if ("missing_centre".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Theo quy định bắt buộc của hệ thống, tài khoản Quản lý phải được phân công một cụm rạp phụ trách.
                </div>
            <% } else if ("centre_has_manager".equals(errorParam) || "centre_already_has_manager".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Cụm rạp đã có Quản lý:</strong> Cụm rạp được chọn hiện đã có Quản lý đang phụ trách (mỗi rạp chỉ có 1 Quản lý). Để thay thế hoặc luân chuyển Quản lý, vui lòng sử dụng chức năng Điều chuyển.
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
                    <div class="cf-kpi-value kpi-active"><%= activeCount %></div>
                </div>
                <div class="cf-kpi-card">
                    <span class="cf-kpi-label">Tạm ngưng</span>
                    <div class="cf-kpi-value kpi-inactive"><%= inactiveCount %></div>
                </div>
            </div>

            <!-- BẢNG DANH SÁCH QUẢN LÝ -->
            <section class="cf-card manager-card-section">
                <div class="cf-card-header manager-card-header">
                    <div>
                        <h2>Danh sách Quản lý (Manager Directory)</h2>
                        <p id="managerCountLabel"><%= totalManagers %> tài khoản trong hệ thống.</p>
                    </div>
                    <div class="manager-filter-toolbar">
                        <form method="get" action="${pageContext.request.contextPath}/admin/managers" class="manager-search-form">
                            <input type="text" name="search" id="managerFilterInput" 
                                   value="<%= request.getAttribute("searchParam") != null ? request.getAttribute("searchParam") : "" %>"
                                   placeholder="🔍 Tìm theo Mã nhân sự (VD: QL00003, 3), username, họ tên, rạp..." 
                                   class="manager-search-input">
                            <button type="submit" class="cf-btn cf-btn-secondary cf-btn-sm">Tìm</button>
                            <% if (request.getAttribute("searchParam") != null && !((String)request.getAttribute("searchParam")).trim().isEmpty()) { %>
                                <a href="${pageContext.request.contextPath}/admin/managers" class="cf-btn cf-btn-secondary cf-btn-sm btn-clear-filter">Xóa lọc</a>
                            <% } %>
                        </form>
                        
                        <select id="statusFilterSelect" class="manager-status-select">
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
                                    <th class="th-emp-code">Mã nhân sự</th>
                                    <th class="th-manager">Quản lý</th>
                                    <th class="th-account">Tên đăng nhập</th>
                                    <th class="th-phone">Số điện thoại</th>
                                    <th class="th-centre">Trung tâm phụ trách</th>
                                    <th class="th-status">Trạng thái tài khoản</th>
                                    <th class="th-action">Thao tác</th>
                                </tr>
                            </thead>
                            <tbody>
                                <% if (managers == null || managers.isEmpty()) { %>
                                    <tr><td colspan="7" class="cf-empty">Chưa có tài khoản Manager nào trong hệ thống.</td></tr>
                                <% } else for (User m : managers) {
                                    String st = m.getStatus();
                                    boolean isActive = "ACTIVE".equalsIgnoreCase(st);
                                    String centreName = m.getCentreName();
                                    boolean hasCentre = centreName != null && !centreName.trim().isEmpty();
                                    String searchData = (m.getEmployeeCode() + " " + m.getUserId() + " ql" + m.getUserId() + " " + m.getFullName() + " " + m.getAccount() + " " + m.getEmail() + " " + (hasCentre ? centreName : "")).toLowerCase();
                                    String statusClass = isActive ? "st-active" : "st-inactive";
                                %>
                                    <tr class="manager-row <%= statusClass %>" data-search="<%= searchData %>">
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
                                            <span class="cf-badge <%= isActive ? "cf-badge-success" : "cf-badge-warning" %>">
                                                <%= isActive ? "Đang hoạt động" : "Tạm ngưng" %>
                                            </span>
                                        </td>
                                        <td class="cell-nowrap">
                                            <div class="manager-actions-row">
                                                <!-- Nút Sửa thông tin tài khoản -->
                                                <button type="button" class="cf-btn cf-btn-secondary cf-btn-sm btn-edit-manager" 
                                                        onclick="openEditManagerModal(<%= m.getUserId() %>, '<%= m.getEmployeeCode() %>', '<%= m.getFullName().replace("'", "\\'") %>', '<%= m.getPhone() != null ? m.getPhone().replace("'", "\\'") : "" %>', '<%= m.getEmail().replace("'", "\\'") %>')"
                                                        title="Chỉnh sửa thông tin cá nhân">
                                                    Sửa
                                                </button>

                                                <!-- Nút Kích hoạt / Tạm ngưng tài khoản -->
                                                <form method="post" action="${pageContext.request.contextPath}/admin/managers" class="form-inline-action">
                                                    <input type="hidden" name="action" value="toggle_status">
                                                    <input type="hidden" name="userId" value="<%= m.getUserId() %>">
                                                    <% if (isActive) { %>
                                                        <input type="hidden" name="status" value="INACTIVE">
                                                        <button type="submit" class="cf-btn cf-btn-secondary cf-btn-sm btn-toggle-suspend" 
                                                                >
                                                            Tạm ngưng
                                                        </button>
                                                    <% } else { %>
                                                        <input type="hidden" name="status" value="ACTIVE">
                                                        <button type="submit" class="cf-btn cf-btn-secondary cf-btn-sm btn-toggle-activate">
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
                        <small class="modal-warning-danger">⚠ Tất cả các cụm rạp hiện đã có Quản lý phụ trách. Nếu muốn thay thế Quản lý, vui lòng sử dụng chức năng Bổ nhiệm Quản lý.</small>
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
