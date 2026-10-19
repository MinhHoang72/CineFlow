<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.Hall,model.Centre,model.User,java.util.List" %>
<%
    // 1. Kiểm tra xác thực và quyền hạn (Primary Actor: Centre Manager hoặc Admin có toàn quyền)
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && session.getAttribute("user") instanceof model.User) {
        authRole = ((model.User) session.getAttribute("user")).getRole();
    }
    if (!"MANAGER".equalsIgnoreCase(authRole) && !"ADMIN".equalsIgnoreCase(authRole)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Quản lý cụm rạp (MANAGER) hoặc Quản trị viên (ADMIN) mới có quyền truy cập trang này.");
        return;
    }

    boolean isAdmin = "ADMIN".equalsIgnoreCase(authRole);
    request.setAttribute("activeNav", "halls");
    request.setAttribute("pageTitle", "Quản lý Phòng chiếu");

    Boolean noCentreAssigned = (Boolean) request.getAttribute("noCentreAssigned");
    Centre currentCentre = (Centre) request.getAttribute("currentCentre");
    Integer targetCentreId = (Integer) request.getAttribute("targetCentreId");
    List<Centre> allCentres = (List<Centre>) request.getAttribute("allCentres");
    List<Hall> halls = (List<Hall>) request.getAttribute("halls");

    Integer totalHalls = (Integer) request.getAttribute("totalHalls");
    Integer activeHalls = (Integer) request.getAttribute("activeHalls");
    Integer inactiveHalls = (Integer) request.getAttribute("inactiveHalls");
    Integer totalCapacity = (Integer) request.getAttribute("totalCapacity");

    String successParam = request.getParameter("success");
    String errorParam = request.getParameter("error");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quản lý Phòng chiếu (Cinema Hall Management) | CineFlow</title>

    <!-- CSS chung của hệ thống -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/reset.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/variables.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/buttons.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/badges.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/forms.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/tables.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/alerts.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/shell.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/dashboard.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/halls.css?v=<%= System.currentTimeMillis() %>">
</head>
<body>
<div class="cf-manager-shell">
    <!-- Sidebar điều hướng tương ứng theo vai trò Admin / Manager -->
    <jsp:include page="<%= isAdmin ? \"/admin/sidebar.jsp\" : \"/manager/sidebar.jsp\" %>" />

    <div class="cf-manager-main">
        <!-- Topbar chung -->
        <jsp:include page="/admin/topbar.jsp" />

        <main class="cf-content">
            <!-- Header trang -->
            <div class="cf-page-header">
                <div>
                    <h1>Quản lý Phòng chiếu (Cinema Hall Management)</h1>
                    <p>UC-FE-03.2: Quản lý danh sách phòng chiếu, định dạng kỹ thuật 2D/3D và sức chứa thiết kế trực thuộc cụm rạp</p>
                </div>
                <% if (Boolean.TRUE.equals(noCentreAssigned)) { %>
                    <!-- Không hiển thị nút thêm khi chưa có rạp -->
                <% } else { %>
                    <button type="button" class="cf-btn cf-btn-primary" onclick="openCreateHallModal()" style="display: flex; align-items: center; gap: 8px;">
                        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
                        Thêm phòng chiếu mới
                    </button>
                <% } %>
            </div>

            <!-- THÔNG BÁO THEO BẢNG ĐẶC TẢ UC-FE-03.2 -->
            <% if ("created".equals(successParam)) { %>
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> <strong>Thành công!</strong> Thêm phòng chiếu mới thành công.
                </div>
            <% } else if ("updated".equals(successParam)) { %>
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> <strong>Thành công!</strong> Cập nhật thông tin phòng chiếu thành công.
                </div>
            <% } else if ("status_updated".equals(successParam)) { %>
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> <strong>Thành công!</strong> Đã cập nhật trạng thái hoạt động của phòng chiếu thành công.
                </div>
            <% } else if ("duplicate_name".equals(errorParam)) { %>
                <!-- Alt 1: Trùng tên phòng chiếu trong cùng cụm rạp -->
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Tên phòng chiếu đã tồn tại tại cụm rạp này. Vui lòng nhập tên khác.
                </div>
            <% } else if ("has_future_bookings".equals(errorParam)) { %>
                <!-- Alt 2: Thay đổi trạng thái phòng đang có lịch chiếu / đặt vé -->
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Cảnh báo:</strong> Không thể vô hiệu hóa phòng chiếu vì đang có suất chiếu đã phát sinh vé đặt.
                </div>
            <% } else if ("missing_fields".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Vui lòng điền đầy đủ các thông tin bắt buộc của phòng chiếu.
                </div>
            <% } else if ("failed".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Không thể thực hiện thao tác. Vui lòng thử lại sau!
                </div>
            <% } %>

            <% if (Boolean.TRUE.equals(noCentreAssigned)) { %>
                <!-- Trường hợp Manager chưa được gán Centre -->
                <div class="cf-alert cf-alert-danger" style="margin-top: 20px;">
                    <span>⚠</span> <strong>Chưa phân công cụm rạp:</strong> Tài khoản Quản lý của bạn hiện chưa được phân công phụ trách Cụm rạp nào. Vui lòng liên hệ Quản trị viên (Admin) để được bổ nhiệm vào Cụm rạp.
                </div>
            <% } else { %>
                <!-- Banner Cụm rạp đang quản lý -->
                <div class="cf-centre-banner">
                    <div class="cf-centre-meta">
                        <div class="cf-centre-icon">🏢</div>
                        <div class="cf-centre-info">
                            <h2><%= currentCentre != null ? currentCentre.getCentreName() : "Cụm rạp #" + targetCentreId %></h2>
                            <p>
                                📍 <%= currentCentre != null ? currentCentre.getAddress() : "" %>
                                <% if (currentCentre != null && currentCentre.getCity() != null && !currentCentre.getCity().trim().isEmpty()) { %>
                                    , <%= currentCentre.getCity() %>
                                <% } %>
                                <% if (currentCentre != null && currentCentre.getPhone() != null && !currentCentre.getPhone().trim().isEmpty()) { %>
                                    &nbsp;|&nbsp; ☎ Hotline: <%= currentCentre.getPhone() %>
                                <% } %>
                            </p>
                        </div>
                    </div>

                    <% if (isAdmin && allCentres != null && allCentres.size() > 1) { %>
                        <!-- Dropdown chuyển đổi Cụm rạp dành cho Admin có toàn quyền -->
                        <div class="cf-centre-switch">
                            <label for="adminCentreSelect">Đổi Cụm rạp:</label>
                            <select id="adminCentreSelect" class="cf-centre-select" onchange="switchCentre(this.value)">
                                <% for (Centre c : allCentres) { %>
                                    <option value="<%= c.getCentreId() %>" <%= (targetCentreId != null && targetCentreId == c.getCentreId()) ? "selected" : "" %>>
                                        <%= c.getCentreName() %> (<%= c.getCity() != null ? c.getCity() : "Rạp #" + c.getCentreId() %>)
                                    </option>
                                <% } %>
                            </select>
                        </div>
                    <% } %>
                </div>

                <!-- Thống kê (Stat Cards) -->
                <div class="cf-stats-grid">
                    <div class="cf-stat-card">
                        <div class="cf-stat-icon primary">🎬</div>
                        <div class="cf-stat-info">
                            <h3><%= totalHalls != null ? totalHalls : 0 %></h3>
                            <p>Tổng số phòng chiếu</p>
                        </div>
                    </div>
                    <div class="cf-stat-card">
                        <div class="cf-stat-icon success">✓</div>
                        <div class="cf-stat-info">
                            <h3><%= activeHalls != null ? activeHalls : 0 %></h3>
                            <p>Đang hoạt động (Active)</p>
                        </div>
                    </div>
                    <div class="cf-stat-card">
                        <div class="cf-stat-icon warning">⏸</div>
                        <div class="cf-stat-info">
                            <h3><%= inactiveHalls != null ? inactiveHalls : 0 %></h3>
                            <p>Tạm ngưng (Inactive)</p>
                        </div>
                    </div>
                    <div class="cf-stat-card">
                        <div class="cf-stat-icon info">💺</div>
                        <div class="cf-stat-info">
                            <h3><%= totalCapacity != null ? totalCapacity : 0 %></h3>
                            <p>Tổng sức chứa thiết kế (Ghế)</p>
                        </div>
                    </div>
                </div>

                <!-- Bảng danh sách phòng chiếu -->
                <div class="cf-table-container">
                    <table class="cf-table">
                        <thead>
                            <tr>
                                <th style="width: 90px;">Mã phòng</th>
                                <th style="min-width: 180px;">Tên phòng chiếu</th>
                                <th style="min-width: 140px;">Định dạng kỹ thuật</th>
                                <th style="min-width: 140px;">Tổng sức chứa</th>
                                <th style="min-width: 140px;">Trạng thái</th>
                                <th style="min-width: 160px; text-align: right;">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <%
                                if (halls != null && !halls.isEmpty()) {
                                    for (Hall h : halls) {
                                        boolean isActive = h.isActive();
                                        boolean is3D = "3D".equalsIgnoreCase(h.getHallType());
                            %>
                            <tr>
                                <td><strong>#<%= h.getHallId() %></strong></td>
                                <td>
                                    <strong style="color: #0F172A; font-size: 15px;"><%= h.getHallName() %></strong>
                                </td>
                                <td>
                                    <% if (is3D) { %>
                                        <span class="cf-badge cf-badge-3d">★ 3D</span>
                                    <% } else { %>
                                        <span class="cf-badge cf-badge-2d">Standard 2D</span>
                                    <% } %>
                                </td>
                                <td>
                                    <strong><%= h.getCapacity() %></strong> ghế
                                </td>
                                <td>
                                    <% if (isActive) { %>
                                        <span class="cf-badge cf-badge-active">● Active</span>
                                    <% } else { %>
                                        <span class="cf-badge cf-badge-inactive">● Inactive</span>
                                    <% } %>
                                </td>
                                <td style="text-align: right;">
                                    <div class="cf-actions" style="justify-content: flex-end;">
                                        <!-- Nút Edit (Trường hợp B) -->
                                        <button type="button" class="cf-btn cf-btn-secondary cf-btn-sm"
                                                onclick="openEditHallModal(<%= h.getHallId() %>, '<%= h.getHallName().replace("'", "\\'") %>', '<%= h.getHallType() %>', <%= h.getCapacity() %>, '<%= isActive ? "AVAILABLE" : "INACTIVE" %>')">
                                            Edit
                                        </button>

                                        <!-- Nút Toggle Status (Bật/Tắt Active <-> Inactive) -->
                                        <form method="POST" action="${pageContext.request.contextPath}/manager/halls" style="display: inline-block; margin: 0;"
                                              onsubmit="return confirmToggleHall('<%= h.getHallName().replace("'", "\\'") %>', <%= isActive %>);">
                                            <input type="hidden" name="action" value="toggleStatus">
                                            <input type="hidden" name="hallId" value="<%= h.getHallId() %>">
                                            <input type="hidden" name="centreId" value="<%= targetCentreId %>">

                                            <% if (isActive) { %>
                                                <button type="submit" class="cf-btn cf-btn-danger cf-btn-sm" title="Vô hiệu hóa phòng chiếu">
                                                    Tắt
                                                </button>
                                            <% } else { %>
                                                <button type="submit" class="cf-btn cf-btn-primary cf-btn-sm" style="background: #16A34A; border-color: #16A34A;" title="Kích hoạt phòng chiếu">
                                                    Bật
                                                </button>
                                            <% } %>
                                        </form>
                                    </div>
                                </td>
                            </tr>
                            <%
                                    }
                                } else {
                            %>
                            <tr>
                                <td colspan="6" style="text-align: center; padding: 40px; color: #64748B;">
                                    Cụm rạp này hiện chưa có phòng chiếu nào. Bấm nút <strong>"Thêm phòng chiếu mới"</strong> để khởi tạo phòng đầu tiên.
                                </td>
                            </tr>
                            <% } %>
                        </tbody>
                    </table>
                </div>
            <% } %>
        </main>
    </div>
</div>

<!-- ============================================================== -->
<!-- MODAL A: THÊM MỚI PHÒNG CHIẾU (ADD HALL) -->
<!-- ============================================================== -->
<div class="cf-modal-backdrop" id="createHallModal">
    <div class="cf-modal">
        <div class="cf-modal-header">
            <h3>Add New Hall (Thêm phòng chiếu mới)</h3>
            <button type="button" class="cf-modal-close" onclick="closeCreateHallModal()">&times;</button>
        </div>
        <form method="POST" action="${pageContext.request.contextPath}/manager/halls">
            <input type="hidden" name="action" value="create">
            <input type="hidden" name="centreId" value="<%= targetCentreId != null ? targetCentreId : 0 %>">

            <div class="cf-modal-body">
                <div class="cf-form-group">
                    <label>Tên / Số phòng chiếu *</label>
                    <input type="text" name="hallName" placeholder="VD: Hall 01, Phòng 01..." required>
                    <span class="cf-form-hint">Tên phòng chiếu không được trùng lặp trong cùng Cụm rạp.</span>
                </div>

                <div class="cf-form-group">
                    <label>Định dạng kỹ thuật (Format) *</label>
                    <select name="hallType" class="cf-select">
                        <option value="2D" selected>Standard 2D</option>
                        <option value="3D">3D</option>
                    </select>
                </div>

                <div class="cf-form-group">
                    <label>Tổng sức chứa thiết kế (Số ghế) *</label>
                    <input type="number" name="capacity" value="60" min="1" max="500" required>
                    <span class="cf-form-hint">Tổng số lượng ghế dự kiến lắp đặt trong phòng chiếu.</span>
                </div>

                <div class="cf-form-group">
                    <label>Trạng thái khởi tạo</label>
                    <input type="text" value="Active (Đang hoạt động)" disabled style="background: #E2E8F0; color: #475569; font-weight: 600;">
                    <input type="hidden" name="status" value="AVAILABLE">
                </div>
            </div>

            <div class="cf-modal-footer">
                <button type="button" class="cf-btn cf-btn-secondary" onclick="closeCreateHallModal()">Hủy</button>
                <button type="submit" class="cf-btn cf-btn-primary">Save (Lưu phòng chiếu)</button>
            </div>
        </form>
    </div>
</div>

<!-- ============================================================== -->
<!-- MODAL B: CHỈNH SỬA PHÒNG CHIẾU (EDIT HALL) -->
<!-- ============================================================== -->
<div class="cf-modal-backdrop" id="editHallModal">
    <div class="cf-modal">
        <div class="cf-modal-header">
            <h3>Edit Hall (Chỉnh sửa phòng chiếu)</h3>
            <button type="button" class="cf-modal-close" onclick="closeEditHallModal()">&times;</button>
        </div>
        <form method="POST" action="${pageContext.request.contextPath}/manager/halls">
            <input type="hidden" name="action" value="update">
            <input type="hidden" name="hallId" id="editHallId">
            <input type="hidden" name="centreId" value="<%= targetCentreId != null ? targetCentreId : 0 %>">

            <div class="cf-modal-body">
                <div class="cf-form-group">
                    <label>Tên / Số phòng chiếu *</label>
                    <input type="text" name="hallName" id="editHallName" required>
                    <span class="cf-form-hint">Kiểm tra không trùng tên phòng khác trong Cụm rạp.</span>
                </div>

                <div class="cf-form-group">
                    <label>Định dạng kỹ thuật (Format) *</label>
                    <select name="hallType" id="editHallType" class="cf-select">
                        <option value="2D">Standard 2D</option>
                        <option value="3D">3D</option>
                    </select>
                </div>

                <div class="cf-form-group">
                    <label>Tổng sức chứa thiết kế (Số ghế) *</label>
                    <input type="number" name="capacity" id="editCapacity" min="1" max="500" required>
                </div>

                <div class="cf-form-group">
                    <label>Trạng thái hoạt động *</label>
                    <select name="status" id="editStatus" class="cf-select">
                        <option value="AVAILABLE">Active (Đang hoạt động)</option>
                        <option value="INACTIVE">Inactive (Tạm ngưng)</option>
                    </select>
                    <span class="cf-form-hint">Lưu ý: Không thể chuyển Inactive nếu phòng đang có suất chiếu tương lai đã bán vé.</span>
                </div>
            </div>

            <div class="cf-modal-footer">
                <button type="button" class="cf-btn cf-btn-secondary" onclick="closeEditHallModal()">Hủy</button>
                <button type="submit" class="cf-btn cf-btn-primary">Update (Cập nhật)</button>
            </div>
        </form>
    </div>
</div>

<!-- JavaScript điều khiển giao diện -->
<script>
    // Tự động ẩn thông báo Alert sau 3.5 giây
    document.addEventListener("DOMContentLoaded", function () {
        document.querySelectorAll(".cf-alert").forEach(function (alert) {
            setTimeout(function () {
                alert.style.transition = "opacity 0.4s ease";
                alert.style.opacity = "0";
                setTimeout(function () { alert.remove(); }, 400);
            }, 3500);
        });
    });

    // Mở / Đóng Modal Thêm Hall
    function openCreateHallModal() {
        document.getElementById('createHallModal').classList.add('active');
    }
    function closeCreateHallModal() {
        document.getElementById('createHallModal').classList.remove('active');
    }

    // Mở / Đóng Modal Sửa Hall
    function openEditHallModal(id, name, type, capacity, status) {
        document.getElementById('editHallId').value = id;
        document.getElementById('editHallName').value = name;
        document.getElementById('editHallType').value = type ? type.toUpperCase() : "2D";
        document.getElementById('editCapacity').value = capacity;
        document.getElementById('editStatus').value = status ? status.toUpperCase() : "AVAILABLE";
        document.getElementById('editHallModal').classList.add('active');
    }
    function closeEditHallModal() {
        document.getElementById('editHallModal').classList.remove('active');
    }

    // Xác nhận Toggle Status
    function confirmToggleHall(hallName, isCurrentlyActive) {
        if (isCurrentlyActive) {
            return confirm("Bạn có chắc chắn muốn TẮT trạng thái hoạt động của phòng \"" + hallName + "\"?\n\nLưu ý: Hệ thống sẽ từ chối nếu phòng đang có suất chiếu tương lai đã phát sinh vé đặt.");
        } else {
            return confirm("Bạn có muốn KÍCH HOẠT (Active) lại phòng \"" + hallName + "\" để sẵn sàng xếp suất chiếu?");
        }
    }

    // Chuyển đổi Cụm rạp (Dành cho Admin)
    function switchCentre(centreId) {
        window.location.href = '${pageContext.request.contextPath}/manager/halls?centreId=' + centreId;
    }

    // Đóng modal khi click ra ngoài backdrop
    document.querySelectorAll('.cf-modal-backdrop').forEach(function(modal) {
        modal.addEventListener('click', function(e) {
            if (e.target === this) {
                this.classList.remove('active');
            }
        });
    });
</script>
</body>
</html>
