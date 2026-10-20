<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.Centre,model.Hall,java.util.List" %>
<%
    // 1. Kiểm tra quyền hạn Admin (RBAC)
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && session.getAttribute("user") instanceof model.User) {
        authRole = ((model.User) session.getAttribute("user")).getRole();
    }
    if (!"ADMIN".equalsIgnoreCase(authRole)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Quản trị viên (ADMIN) mới có quyền truy cập trang này.");
        return;
    }

    request.setAttribute("activeNav", "centres");
    request.setAttribute("pageTitle", "Quản lý Chi nhánh");

    List<Centre> centres = (List<Centre>) request.getAttribute("centres");
    Integer totalCentres = (Integer) request.getAttribute("totalCentres");
    Integer activeCentres = (Integer) request.getAttribute("activeCentres");
    Integer inactiveCentres = (Integer) request.getAttribute("inactiveCentres");
    Integer totalHalls = (Integer) request.getAttribute("totalHalls");

    Integer currentPage = (Integer) request.getAttribute("currentPage");
    if (currentPage == null) currentPage = 1;
    Integer pageSize = (Integer) request.getAttribute("pageSize");
    if (pageSize == null) pageSize = 6;
    Integer totalPages = (Integer) request.getAttribute("totalPages");
    if (totalPages == null) totalPages = 1;
    Integer totalFilteredCentres = (Integer) request.getAttribute("totalFilteredCentres");
    if (totalFilteredCentres == null) totalFilteredCentres = 0;

    String currentSearch = (String) request.getAttribute("currentSearch");
    String currentStatus = (String) request.getAttribute("currentStatus");

    String successParam = request.getParameter("success");
    String errorParam = request.getParameter("error");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quản lý Chi nhánh | CineFlow Admin</title>

    <!-- Phông chữ chuẩn hệ thống CineFlow -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:ital,wght@0,300;0,400;0,500;0,600;0,700;0,800;1,400;1,600&display=swap" rel="stylesheet">

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
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/centres.css?v=<%= System.currentTimeMillis() %>">
</head>
<body>
<div class="cf-manager-shell">
    <!-- Menu bên trái (Sidebar) -->
    <jsp:include page="/admin/sidebar.jsp" />

    <div class="cf-manager-main">
        <!-- Thanh tiêu đề trên (Topbar) -->
        <jsp:include page="/admin/topbar.jsp" />

        <main class="cf-content">
            <!-- Header trang -->
            <div class="cf-page-header">
                <div>
                    <h1>Quản lý Chi nhánh</h1>
                    <p>Danh sách các chi nhánh, thông tin liên hệ và trạng thái hoạt động trên toàn hệ thống</p>
                </div>
                <button type="button" class="cf-btn cf-btn-primary" onclick="openCreateCentreModal()" style="display: flex; align-items: center; gap: 8px;">
                    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
                    Thêm Chi nhánh mới
                </button>
            </div>

            <!-- Thông báo thành công / lỗi -->
            <% if ("created".equals(successParam)) { %>
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> <strong>Thành công!</strong> Đã thêm mới chi nhánh vào hệ thống.
                </div>
            <% } else if ("updated".equals(successParam)) { %>
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> <strong>Thành công!</strong> Đã cập nhật thông tin liên hệ và địa chỉ chi nhánh thành công.
                </div>
            <% } else if ("status_updated".equals(successParam)) { %>
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> <strong>Thành công!</strong> Đã cập nhật trạng thái hoạt động của chi nhánh.
                </div>
            <% } else if ("duplicate_name".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Tên chi nhánh đã tồn tại trong hệ thống. Vui lòng chọn tên khác!
                </div>
            <% } else if ("missing_fields".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Vui lòng điền đầy đủ các thông tin bắt buộc (Tên chi nhánh, Địa chỉ, Hotline).
                </div>
            <% } else if ("failed".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Không thể thực hiện thao tác. Vui lòng thử lại sau!
                </div>
            <% } %>

            <!-- Thẻ thống kê (Stat Cards) -->
            <div class="cf-stats-grid">
                <div class="cf-stat-card">
                    <div class="cf-stat-icon primary">🏢</div>
                    <div class="cf-stat-info">
                        <h3><%= totalCentres != null ? totalCentres : 0 %></h3>
                        <p>Tổng số Chi nhánh</p>
                    </div>
                </div>
                <div class="cf-stat-card">
                    <div class="cf-stat-icon success">✓</div>
                    <div class="cf-stat-info">
                        <h3><%= activeCentres != null ? activeCentres : 0 %></h3>
                        <p>Đang hoạt động</p>
                    </div>
                </div>
                <div class="cf-stat-card">
                    <div class="cf-stat-icon warning">⏸</div>
                    <div class="cf-stat-info">
                        <h3><%= inactiveCentres != null ? inactiveCentres : 0 %></h3>
                        <p>Tạm ngưng</p>
                    </div>
                </div>
                <div class="cf-stat-card">
                    <div class="cf-stat-icon info">🎬</div>
                    <div class="cf-stat-info">
                        <h3><%= totalHalls != null ? totalHalls : 0 %></h3>
                        <p>Tổng số Phòng chiếu (Halls)</p>
                    </div>
                </div>
            </div>

            <!-- Thanh lọc & tìm kiếm -->
            <div class="cf-filter-card">
                <form class="cf-filter-form" method="GET" action="${pageContext.request.contextPath}/admin/centres">
                    <div class="cf-search-box">
                        <span class="icon">
                            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
                        </span>
                        <input type="text" name="search" placeholder="Tìm theo tên chi nhánh, hotline, địa chỉ, thành phố..." value="<%= currentSearch != null ? currentSearch : "" %>">
                    </div>

                    <select name="status" class="cf-select">
                        <option value="ALL" <%= "ALL".equalsIgnoreCase(currentStatus) ? "selected" : "" %>>Tất cả trạng thái</option>
                        <option value="ACTIVE" <%= "ACTIVE".equalsIgnoreCase(currentStatus) ? "selected" : "" %>>Đang hoạt động (ACTIVE)</option>
                        <option value="INACTIVE" <%= "INACTIVE".equalsIgnoreCase(currentStatus) ? "selected" : "" %>>Tạm ngưng (INACTIVE)</option>
                    </select>

                    <button type="submit" class="cf-btn cf-btn-primary">Lọc</button>
                    <% if ((currentSearch != null && !currentSearch.isEmpty()) || (currentStatus != null && !"ALL".equalsIgnoreCase(currentStatus))) { %>
                        <a href="${pageContext.request.contextPath}/admin/centres" class="cf-btn cf-btn-secondary">Xóa lọc</a>
                    <% } %>
                </form>
            </div>

            <!-- Bảng danh sách Chi nhánh -->
            <div class="cf-table-container">
                <table class="cf-table">
                    <thead>
                        <tr>
                            <th style="width: 60px;">ID</th>
                            <th style="min-width: 180px;">Tên Chi nhánh</th>
                            <th style="min-width: 240px;">Địa chỉ & Thành phố</th>
                            <th style="min-width: 130px;">Hotline</th>
                            <th style="min-width: 170px;">Phòng chiếu trực thuộc</th>
                            <th style="min-width: 120px;">Trạng thái</th>
                            <th style="min-width: 190px; text-align: right;">Thao tác</th>
                        </tr>
                    </thead>
                    <tbody>
                        <%
                            if (centres != null && !centres.isEmpty()) {
                                for (Centre c : centres) {
                                    boolean isActive = "ACTIVE".equalsIgnoreCase(c.getStatus());
                        %>
                        <tr>
                            <td><strong>#<%= c.getCentreId() %></strong></td>
                            <td>
                                <strong style="color: #F8FAFC; font-size: 15px;"><%= c.getCentreName() %></strong>
                            </td>
                            <td>
                                <div style="color: #F8FAFC;"><%= c.getAddress() %></div>
                                <% if (c.getCity() != null && !c.getCity().trim().isEmpty()) { %>
                                    <small style="color: #94A3B8;"><%= c.getCity() %></small>
                                <% } %>
                            </td>
                            <td>
                                <%= (c.getPhone() != null && !c.getPhone().trim().isEmpty()) ? "<span style='color: #F8FAFC;'>" + c.getPhone() + "</span>" : "<span style='color: #94A3B8;'>Chưa có</span>" %>
                            </td>
                            <td>
                                <!-- BR-HS-01: Một Centre quản lý danh sách các Hall trực thuộc -->
                                <button type="button" class="cf-badge cf-badge-halls"
                                        onclick="openViewHallsModal(<%= c.getCentreId() %>, '<%= c.getCentreName().replace("'", "\\'") %>')"
                                        title="Bấm để xem danh sách phòng chiếu trực thuộc">
                                    <span>🎬</span>
                                    <span><%= c.getHallCount() %> Phòng (<%= c.getSeatCount() %> ghế)</span>
                                </button>
                            </td>
                            <td>
                                <% if (isActive) { %>
                                    <span class="cf-badge cf-badge-active">● Hoạt động</span>
                                <% } else { %>
                                    <span class="cf-badge cf-badge-inactive">● Tạm ngưng</span>
                                <% } %>
                            </td>
                            <td style="text-align: right;">
                                <div class="cf-actions" style="justify-content: flex-end;">
                                    <!-- Nút Sửa -->
                                    <button type="button" class="cf-btn cf-btn-secondary cf-btn-sm"
                                            onclick="openEditCentreModal(<%= c.getCentreId() %>, '<%= c.getCentreName().replace("'", "\\'") %>', '<%= c.getAddress().replace("'", "\\'") %>', '<%= c.getPhone() != null ? c.getPhone().replace("'", "\\'") : "" %>', '<%= c.getCity() != null ? c.getCity().replace("'", "\\'") : "" %>')">
                                        Sửa
                                    </button>

                                    <!-- Nút Toggle Status (Bật / Tắt trạng thái) -->
                                    <form method="POST" action="${pageContext.request.contextPath}/admin/centres" style="display: inline-block; margin: 0;"
                                          onsubmit="return confirmToggleStatus('<%= c.getCentreName().replace("'", "\\'") %>', <%= isActive %>);">
                                        <input type="hidden" name="action" value="toggleStatus">
                                        <input type="hidden" name="centreId" value="<%= c.getCentreId() %>">
                                        <input type="hidden" name="search" value="<%= currentSearch != null ? currentSearch : "" %>">
                                        <input type="hidden" name="status" value="<%= currentStatus != null ? currentStatus : "ALL" %>">
                                        <input type="hidden" name="page" value="<%= currentPage %>">

                                        <% if (isActive) { %>
                                            <button type="submit" class="cf-btn cf-btn-danger cf-btn-sm" title="Tạm ngưng hoạt động chi nhánh">
                                                Tắt
                                            </button>
                                        <% } else { %>
                                            <button type="submit" class="cf-btn cf-btn-primary cf-btn-sm" style="background: #16A34A; border-color: #16A34A;" title="Kích hoạt lại chi nhánh">
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
                            <td colspan="7" style="text-align: center; padding: 40px; color: #94A3B8;">
                                Không tìm thấy Chi nhánh nào phù hợp với điều kiện tìm kiếm.
                            </td>
                        </tr>
                        <% } %>
                    </tbody>
                </table>

                <% if (totalFilteredCentres != null && totalFilteredCentres > 0) { 
                    int startItem = (currentPage - 1) * pageSize + 1;
                    int endItem = Math.min(currentPage * pageSize, totalFilteredCentres);

                    String baseParams = "";
                    if (currentSearch != null && !currentSearch.trim().isEmpty()) {
                        baseParams += "&search=" + java.net.URLEncoder.encode(currentSearch.trim(), "UTF-8");
                    }
                    if (currentStatus != null && !currentStatus.trim().isEmpty()) {
                        baseParams += "&status=" + java.net.URLEncoder.encode(currentStatus.trim(), "UTF-8");
                    }
                %>
                <!-- THANH PHÂN TRANG (PAGINATION) -->
                <div class="cf-pagination-bar">
                    <div class="cf-pagination-info">
                        Hiển thị <strong><%= startItem %> - <%= endItem %></strong> trên tổng số <strong><%= totalFilteredCentres %></strong> chi nhánh
                    </div>
                    <% if (totalPages > 1) { %>
                    <div class="cf-pagination-controls">
                        <!-- Nút trang trước (Previous) -->
                        <% if (currentPage > 1) { %>
                            <a href="${pageContext.request.contextPath}/admin/centres?page=<%= currentPage - 1 %><%= baseParams %>" 
                               class="cf-page-btn" title="Trang trước">«</a>
                        <% } else { %>
                            <span class="cf-page-btn disabled" title="Trang trước">«</span>
                        <% } %>

                        <!-- Danh sách các số trang -->
                        <% for (int i = 1; i <= totalPages; i++) { 
                            if (i == 1 || i == totalPages || Math.abs(i - currentPage) <= 2) {
                        %>
                            <% if (i == currentPage) { %>
                                <span class="cf-page-btn active"><%= i %></span>
                            <% } else { %>
                                <a href="${pageContext.request.contextPath}/admin/centres?page=<%= i %><%= baseParams %>" 
                                   class="cf-page-btn"><%= i %></a>
                            <% } %>
                        <% 
                            } else if (i == currentPage - 3 || i == currentPage + 3) { 
                        %>
                            <span class="cf-page-ellipsis">...</span>
                        <% 
                            }
                        } 
                        %>

                        <!-- Nút trang sau (Next) -->
                        <% if (currentPage < totalPages) { %>
                            <a href="${pageContext.request.contextPath}/admin/centres?page=<%= currentPage + 1 %><%= baseParams %>" 
                               class="cf-page-btn" title="Trang sau">»</a>
                        <% } else { %>
                            <span class="cf-page-btn disabled" title="Trang sau">»</span>
                        <% } %>
                    </div>
                    <% } %>
                </div>
                <% } %>
            </div>
        </main>
    </div>
</div>

<!-- ============================================================== -->
<!-- MODAL 1: THÊM CHI NHÁNH MỚI -->
<!-- ============================================================== -->
<div class="cf-modal-backdrop" id="createCentreModal">
    <div class="cf-modal">
        <div class="cf-modal-header">
            <h3>Thêm Chi nhánh mới</h3>
            <button type="button" class="cf-modal-close" onclick="closeCreateCentreModal()">&times;</button>
        </div>
        <form method="POST" action="${pageContext.request.contextPath}/admin/centres">
            <input type="hidden" name="action" value="create">
            <input type="hidden" name="search" value="<%= currentSearch != null ? currentSearch : "" %>">
            <input type="hidden" name="status" value="<%= currentStatus != null ? currentStatus : "ALL" %>">
            <input type="hidden" name="page" value="1">

            <div class="cf-modal-body">
                <div class="cf-form-group">
                    <label>Tên Chi nhánh *</label>
                    <input type="text" name="centreName" placeholder="Ví dụ: CineFlow Cầu Giấy" required>
                    <span class="cf-form-hint">Hệ thống sẽ kiểm tra đảm bảo không trùng tên với chi nhánh khác.</span>
                </div>

                <div class="cf-form-group">
                    <label>Địa chỉ chi tiết *</label>
                    <input type="text" name="address" placeholder="Ví dụ: 123 Cầu Giấy, Quan Hoa" required>
                </div>

                <div class="cf-form-group">
                    <label>Hotline / Điện thoại liên hệ *</label>
                    <input type="text" name="phone" placeholder="Ví dụ: 02411111111" required>
                </div>

                <div class="cf-form-group">
                    <label>Thành phố / Tỉnh thành</label>
                    <input type="text" name="city" placeholder="Ví dụ: Hà Nội, TP. Hồ Chí Minh, Đà Nẵng...">
                </div>
            </div>

            <div class="cf-modal-footer">
                <button type="button" class="cf-btn cf-btn-secondary" onclick="closeCreateCentreModal()">Hủy</button>
                <button type="submit" class="cf-btn cf-btn-primary">Lưu Chi nhánh</button>
            </div>
        </form>
    </div>
</div>

<!-- ============================================================== -->
<!-- MODAL 2: SỬA THÔNG TIN CHI NHÁNH -->
<!-- ============================================================== -->
<div class="cf-modal-backdrop" id="editCentreModal">
    <div class="cf-modal">
        <div class="cf-modal-header">
            <h3>Sửa thông tin Chi nhánh</h3>
            <button type="button" class="cf-modal-close" onclick="closeEditCentreModal()">&times;</button>
        </div>
        <form method="POST" action="${pageContext.request.contextPath}/admin/centres">
            <input type="hidden" name="action" value="update">
            <input type="hidden" name="centreId" id="editCentreId">
            <input type="hidden" name="search" value="<%= currentSearch != null ? currentSearch : "" %>">
            <input type="hidden" name="status" value="<%= currentStatus != null ? currentStatus : "ALL" %>">
            <input type="hidden" name="page" value="<%= currentPage %>">

            <div class="cf-modal-body">
                <div class="cf-form-group">
                    <label>Tên Chi nhánh *</label>
                    <input type="text" name="centreName" id="editCentreName" required>
                    <span class="cf-form-hint">Kiểm tra không trùng lặp với tên của chi nhánh khác.</span>
                </div>

                <div class="cf-form-group">
                    <label>Địa chỉ chi tiết *</label>
                    <input type="text" name="address" id="editAddress" required>
                </div>

                <div class="cf-form-group">
                    <label>Hotline / Điện thoại liên hệ *</label>
                    <input type="text" name="phone" id="editPhone" required>
                </div>

                <div class="cf-form-group">
                    <label>Thành phố / Tỉnh thành</label>
                    <input type="text" name="city" id="editCity">
                </div>
            </div>

            <div class="cf-modal-footer">
                <button type="button" class="cf-btn cf-btn-secondary" onclick="closeEditCentreModal()">Hủy</button>
                <button type="submit" class="cf-btn cf-btn-primary">Cập nhật thay đổi</button>
            </div>
        </form>
    </div>
</div>

<!-- ============================================================== -->
<!-- MODAL 3: XEM DANH SÁCH HALL TRỰC THUỘC (BR-HS-01) -->
<!-- ============================================================== -->
<div class="cf-modal-backdrop" id="viewHallsModal">
    <div class="cf-modal cf-modal-lg">
        <div class="cf-modal-header">
            <div>
                <h3 id="viewHallsTitle">Danh sách phòng chiếu</h3>
                <small id="viewHallsSubtitle" style="color: #94A3B8; font-size: 13px;">Danh sách các phòng chiếu hiện có trực thuộc chi nhánh này</small>
            </div>
            <button type="button" class="cf-modal-close" onclick="closeViewHallsModal()">&times;</button>
        </div>

        <div class="cf-modal-body" style="padding: 0;">
            <div id="hallsLoading" style="text-align: center; padding: 30px; color: #94A3B8;">
                Đang tải danh sách phòng chiếu...
            </div>

            <div id="hallsContent" style="display: none;">
                <table class="cf-halls-table">
                    <thead>
                        <tr>
                            <th style="width: 80px;">Mã phòng</th>
                            <th>Tên phòng chiếu</th>
                            <th>Loại phòng</th>
                            <th>Sức chứa (Ghế)</th>
                            <th>Trạng thái phòng</th>
                        </tr>
                    </thead>
                    <tbody id="hallsTableBody">
                        <!-- Render qua Javascript AJAX -->
                    </tbody>
                </table>
            </div>
        </div>

        <div class="cf-modal-footer">
            <button type="button" class="cf-btn cf-btn-secondary" onclick="closeViewHallsModal()">Đóng</button>
        </div>
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

    // Mở / Đóng Modal Thêm Centre
    function openCreateCentreModal() {
        document.getElementById('createCentreModal').classList.add('active');
    }
    function closeCreateCentreModal() {
        document.getElementById('createCentreModal').classList.remove('active');
    }

    // Mở / Đóng Modal Sửa Centre
    function openEditCentreModal(id, name, address, phone, city) {
        document.getElementById('editCentreId').value = id;
        document.getElementById('editCentreName').value = name;
        document.getElementById('editAddress').value = address;
        document.getElementById('editPhone').value = phone;
        document.getElementById('editCity').value = city;
        document.getElementById('editCentreModal').classList.add('active');
    }
    function closeEditCentreModal() {
        document.getElementById('editCentreModal').classList.remove('active');
    }

    // Xác nhận Toggle Status
    function confirmToggleStatus(centreName, isCurrentlyActive) {
        if (isCurrentlyActive) {
            return confirm("CẢNH BÁO: Khi tạm ngưng chi nhánh \"" + centreName + "\", tất cả các phòng chiếu và suất chiếu của chi nhánh này sẽ tạm dừng hoạt động trên luồng đặt vé.\n\nBạn có chắc chắn muốn TẮT trạng thái hoạt động của chi nhánh này không?");
        } else {
            return confirm("Bạn có muốn KÍCH HOẠT lại chi nhánh \"" + centreName + "\" để chi nhánh hoạt động bình thường trên hệ thống?");
        }
    }

    // BR-HS-01: Mở Modal xem danh sách các Hall trực thuộc Centre
    function openViewHallsModal(centreId, centreName) {
        document.getElementById('viewHallsTitle').innerText = "Danh sách phòng chiếu - " + centreName;
        document.getElementById('hallsLoading').style.display = 'block';
        document.getElementById('hallsContent').style.display = 'none';
        document.getElementById('viewHallsModal').classList.add('active');

        // Gọi AJAX lấy danh sách Hall của Centre
        fetch('${pageContext.request.contextPath}/admin/centres?action=getHalls&centreId=' + centreId)
            .then(function(res) { return res.json(); })
            .then(function(data) {
                var tbody = document.getElementById('hallsTableBody');
                tbody.innerHTML = '';

                if (!data || data.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="5" style="text-align: center; padding: 24px; color: #94A3B8;">Chi nhánh này hiện chưa có phòng chiếu nào trực thuộc.</td></tr>';
                } else {
                    data.forEach(function(hall) {
                        var statusBadge = '';
                        if (hall.status === 'AVAILABLE') {
                            statusBadge = '<span class="cf-badge cf-badge-active">Hoạt động</span>';
                        } else if (hall.status === 'MAINTENANCE') {
                            statusBadge = '<span class="cf-badge" style="background: rgba(245, 158, 11, 0.15); color: #FBBF24; border: 1px solid rgba(245, 158, 11, 0.35);">Bảo trì</span>';
                        } else {
                            statusBadge = '<span class="cf-badge cf-badge-inactive">Tạm ngưng</span>';
                        }

                        var tr = document.createElement('tr');
                        tr.innerHTML = '<td><strong>#' + hall.hallId + '</strong></td>'
                                     + '<td><strong style="color: #F8FAFC;">' + hall.hallName + '</strong></td>'
                                     + '<td><span class="cf-badge" style="background: rgba(56, 189, 248, 0.15); color: #38BDF8; border: 1px solid rgba(56, 189, 248, 0.3);">' + hall.hallType + '</span></td>'
                                     + '<td><strong style="color: #F8FAFC;">' + hall.capacity + '</strong> ghế</td>'
                                     + '<td>' + statusBadge + '</td>';
                        tbody.appendChild(tr);
                    });
                }

                document.getElementById('hallsLoading').style.display = 'none';
                document.getElementById('hallsContent').style.display = 'block';
            })
            .catch(function(err) {
                console.error(err);
                document.getElementById('hallsLoading').innerHTML = '<span style="color: #DC2626;">Không thể tải danh sách phòng chiếu. Vui lòng thử lại sau!</span>';
            });
    }
    function closeViewHallsModal() {
        document.getElementById('viewHallsModal').classList.remove('active');
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
