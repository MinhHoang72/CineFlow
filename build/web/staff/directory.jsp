<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.User,model.Branch,java.util.List" %>
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
    List<Branch> branches = (List<Branch>) request.getAttribute("branches");
    Integer selectedBranchId = (Integer) request.getAttribute("selectedBranchId");
    String role = (String) session.getAttribute("role");
    boolean managerView = "MANAGER".equalsIgnoreCase(role);

    int totalStaff = staff != null ? staff.size() : 0;
    int managerCount = 0;
    int regularStaffCount = 0;
    int activeCount = 0;
    if (staff != null) {
        for (User u : staff) {
            if ("MANAGER".equalsIgnoreCase(u.getRole())) managerCount++;
            else regularStaffCount++;
            if ("ACTIVE".equalsIgnoreCase(u.getStatus())) activeCount++;
        }
    }

    if (!managerView) {
        request.setAttribute("activeNav", "staff");
        request.setAttribute("pageTitle", "Quản lý nhân sự");
        request.setAttribute("pageBreadcrumb", "Nhân sự toàn hệ thống");
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Danh sách nhân viên | CineFlow</title>
    <% if (managerView) { %>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/shell.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/forms.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/tables.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/badges.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/staff.css">
    <% } else { %>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/dashboard.css">
        <style>
            .staff-email {
                display: block;
                font-size: 11.5px;
                color: var(--cf-text-muted);
            }
            .staff-select {
                padding: 6px 10px;
                border: 1px solid var(--cf-border);
                border-radius: 6px;
                font-size: 12px;
                color: var(--cf-text);
                background: #fff;
            }
        </style>
    <% } %>
</head>
<body>
<% if (managerView) { %>
    <!-- MANAGER SHELL -->
    <div class="cf-manager-shell">
        <jsp:include page="/manager/sidebar.jsp"/>
        <div class="cf-manager-main">
            <jsp:include page="/manager/topbar.jsp"/>
            <main class="cf-content">
                <div class="cf-page-header">
                    <div>
                        <p class="cf-breadcrumb">Nhân sự / Danh sách nhân viên</p>
                        <h1>Nhân sự trung tâm</h1>
                        <p>Quản lý tài khoản nhân viên tại trung tâm được phân công.</p>
                    </div>
                </div>

                <section class="cf-card">
                    <div class="cf-card-header">
                        <div>
                            <h2>Danh sách nhân viên</h2>
                            <p><%= staff == null ? 0 : staff.size() %> tài khoản đang hiển thị.</p>
                        </div>
                    </div>
                    <div class="cf-card-body">
                        <div class="cf-table-wrap">
                            <table class="cf-table">
                                <thead>
                                    <tr>
                                        <th>Nhân viên</th>
                                        <th>Vai trò</th>
                                        <th>Trung tâm</th>
                                        <th>Trạng thái</th>
                                        <th>Thao tác</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <% if (staff == null || staff.isEmpty()) { %>
                                        <tr><td colspan="5" class="cf-empty">Chưa có nhân viên phù hợp.</td></tr>
                                    <% } else for (User item : staff) {
                                        String st = item.getStatus();
                                    %>
                                        <tr>
                                            <td>
                                                <strong><%= item.getFullName() %></strong>
                                                <small class="staff-email"><%= item.getEmail() %></small>
                                            </td>
                                            <td><span class="cf-badge cf-badge-info"><%= item.getRole() %></span></td>
                                            <td><%= branches != null && !branches.isEmpty() ? branches.get(0).getBranchName() : "—" %></td>
                                            <td>
                                                <span class="cf-badge <%= "ACTIVE".equals(st) ? "cf-badge-success" : "LOCKED".equals(st) ? "cf-badge-danger" : "cf-badge-warning" %>">
                                                    <%= "ACTIVE".equals(st) ? "Đang hoạt động" : "LOCKED".equals(st) ? "Đã khóa" : "Tạm ngưng" %>
                                                </span>
                                            </td>
                                            <td>
                                                <form id="staff-<%= item.getUserId() %>" method="post" action="${pageContext.request.contextPath}/staff" style="display:flex;gap:6px;align-items:center;">
                                                    <input type="hidden" name="userId" value="<%= item.getUserId() %>">
                                                    <% if (branches != null && !branches.isEmpty()) { %>
                                                        <input type="hidden" name="branchId" value="<%= branches.get(0).getBranchId() %>">
                                                    <% } %>
                                                    <select name="status" class="staff-select">
                                                        <option value="ACTIVE" <%= "ACTIVE".equals(st) ? "selected" : "" %>>Kích hoạt</option>
                                                        <option value="INACTIVE" <%= "INACTIVE".equals(st) ? "selected" : "" %>>Tạm ngưng</option>
                                                        <option value="LOCKED" <%= "LOCKED".equals(st) ? "selected" : "" %>>Khóa</option>
                                                    </select>
                                                    <button class="cf-btn cf-btn-primary cf-btn-sm" type="submit">Lưu</button>
                                                </form>
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
<% } else { %>
    <!-- ADMIN SHELL -->
    <div class="admin-layout">
        <jsp:include page="/admin/sidebar.jsp" />

        <div class="admin-main">
            <jsp:include page="/admin/topbar.jsp" />

            <main class="admin-content">
                <div class="page-hero">
                    <div class="page-hero-text">
                        <p class="eyebrow">QUẢN TRỊ NHÂN SỰ</p>
                        <h1>Danh sách & Phân công nhân viên</h1>
                        <p>Quản lý tài khoản Quản lý cụm rạp (Manager) và Nhân viên (Staff) trên toàn hệ thống CineFlow.</p>
                    </div>
                </div>

                <!-- KPI STATS -->
                <div class="kpi-grid">
                    <div class="kpi-card ruby">
                        <div class="kpi-card-header">
                            <span class="kpi-title">TỔNG NHÂN SỰ</span>
                            <span class="kpi-icon">👥</span>
                        </div>
                        <div class="kpi-value"><%= totalStaff %></div>
                        <div class="kpi-subtext">Manager và Staff toàn hệ thống</div>
                    </div>

                    <div class="kpi-card sky">
                        <div class="kpi-card-header">
                            <span class="kpi-title">QUẢN LÝ CỤM RẠP</span>
                            <span class="kpi-icon">👔</span>
                        </div>
                        <div class="kpi-value"><%= managerCount %></div>
                        <div class="kpi-subtext">Vai trò MANAGER</div>
                    </div>

                    <div class="kpi-card emerald">
                        <div class="kpi-card-header">
                            <span class="kpi-title">ĐANG HOẠT ĐỘNG</span>
                            <span class="kpi-icon">✓</span>
                        </div>
                        <div class="kpi-value"><%= activeCount %></div>
                        <div class="kpi-subtext">Tài khoản trạng thái ACTIVE</div>
                    </div>

                    <div class="kpi-card amber">
                        <div class="kpi-card-header">
                            <span class="kpi-title">NHÂN VIÊN RẠP</span>
                            <span class="kpi-icon">🎟️</span>
                        </div>
                        <div class="kpi-value"><%= regularStaffCount %></div>
                        <div class="kpi-subtext">Vai trò STAFF tại các rạp</div>
                    </div>
                </div>

                <!-- DATA CARD WITH TOOLBAR & PAGINATION -->
                <div class="admin-card">
                    <form class="cf-toolbar" method="get" action="${pageContext.request.contextPath}/staff" style="margin-bottom:0;">
                        <div class="toolbar-left">
                            <div class="search-box">
                                <span class="search-icon">🔍</span>
                                <input type="text" id="staffSearchInput" placeholder="Tìm theo tên hoặc email...">
                            </div>

                            <select name="branchId" class="filter-select" onchange="this.form.submit()">
                                <option value="">Tất cả cụm rạp (<%= branches != null ? branches.size() : 0 %>)</option>
                                <% if (branches != null) {
                                    for (Branch b : branches) {
                                %>
                                    <option value="<%= b.getBranchId() %>" <%= selectedBranchId != null && selectedBranchId == b.getBranchId() ? "selected" : "" %>>
                                        <%= b.getBranchName() %> (<%= b.getCity() %>)
                                    </option>
                                <% }} %>
                            </select>
                        </div>

                        <div class="toolbar-right">
                            <div class="pagination-size-select">
                                <span>Hiển thị:</span>
                                <select id="staffPageSizeSelect" class="filter-select">
                                    <option value="10" selected>10 nhân viên</option>
                                    <option value="20">20 nhân viên</option>
                                    <option value="50">50 nhân viên</option>
                                    <option value="ALL">Tất cả</option>
                                </select>
                            </div>
                        </div>
                    </form>

                    <div class="table-responsive">
                        <table class="cf-table">
                            <thead>
                                <tr>
                                    <th>Nhân viên</th>
                                    <th>Vai trò</th>
                                    <th>Cụm rạp phân công</th>
                                    <th>Trạng thái</th>
                                    <th style="text-align: right;">Cập nhật & Thao tác</th>
                                </tr>
                            </thead>
                            <tbody id="staffTableBody">
                                <% if (staff == null || staff.isEmpty()) { %>
                                    <tr id="staffEmptyRow">
                                        <td colspan="5" class="cf-empty-state">Chưa có nhân viên phù hợp với tiêu chí lọc.</td>
                                    </tr>
                                <% } else {
                                    for (User item : staff) {
                                        String st = item.getStatus();
                                        String roleBadge = "MANAGER".equalsIgnoreCase(item.getRole()) ? "cf-badge-warning" : "cf-badge-info";
                                %>
                                    <tr data-name="<%= item.getFullName() != null ? item.getFullName().toLowerCase() : "" %>"
                                        data-email="<%= item.getEmail() != null ? item.getEmail().toLowerCase() : "" %>"
                                        class="staff-data-row">
                                        <td>
                                            <div style="display: flex; align-items: center; gap: 10px;">
                                                <div style="width: 34px; height: 34px; border-radius: 50%; background: #e2e8f0; display: flex; align-items: center; justify-content: center; font-weight: 700; color: #475569; font-size: 13px;">
                                                    <%= item.getFullName() != null && !item.getFullName().isBlank() ? item.getFullName().substring(0, 1).toUpperCase() : "U" %>
                                                </div>
                                                <div>
                                                    <strong><%= item.getFullName() %></strong>
                                                    <span class="staff-email"><%= item.getEmail() %></span>
                                                </div>
                                            </div>
                                        </td>
                                        <td>
                                            <span class="cf-badge <%= roleBadge %>"><%= item.getRole() %></span>
                                        </td>
                                        <td>
                                            <select class="staff-select" name="branchId" form="staff-<%= item.getUserId() %>">
                                                <option value="">Chưa phân công cụm rạp</option>
                                                <% if (branches != null) {
                                                    for (Branch b : branches) {
                                                %>
                                                    <option value="<%= b.getBranchId() %>" <%= item.getBranchId() != null && item.getBranchId() == b.getBranchId() ? "selected" : "" %>>
                                                        <%= b.getBranchName() %>
                                                    </option>
                                                <% }} %>
                                            </select>
                                        </td>
                                        <td>
                                            <span class="cf-badge <%= "ACTIVE".equals(st) ? "cf-badge-success" : "LOCKED".equals(st) ? "cf-badge-danger" : "cf-badge-warning" %>">
                                                <span class="badge-dot"></span>
                                                <%= "ACTIVE".equals(st) ? "Hoạt động" : "LOCKED".equals(st) ? "Đã khóa" : "Tạm ngưng" %>
                                            </span>
                                        </td>
                                        <td style="text-align: right;">
                                            <form id="staff-<%= item.getUserId() %>" method="post" action="${pageContext.request.contextPath}/staff" style="display:inline-flex;gap:6px;align-items:center;">
                                                <input type="hidden" name="userId" value="<%= item.getUserId() %>">
                                                <select name="status" class="staff-select">
                                                    <option value="ACTIVE" <%= "ACTIVE".equals(st) ? "selected" : "" %>>Hoạt động</option>
                                                    <option value="INACTIVE" <%= "INACTIVE".equals(st) ? "selected" : "" %>>Tạm ngưng</option>
                                                    <option value="LOCKED" <%= "LOCKED".equals(st) ? "selected" : "" %>>Khóa tài khoản</option>
                                                </select>
                                                <button class="cf-btn cf-btn-primary cf-btn-sm" type="submit">Lưu</button>
                                            </form>
                                        </td>
                                    </tr>
                                <% }} %>
                            </tbody>
                        </table>
                    </div>

                    <!-- PAGINATION BAR -->
                    <div class="cf-pagination-bar">
                        <div class="pagination-info" id="staffPaginationInfo">
                            Hiển thị <%= totalStaff %> nhân viên
                        </div>
                        <div class="pagination-controls" id="staffPaginationControls">
                            <!-- Dynamic controls -->
                        </div>
                    </div>
                </div>
            </main>
        </div>
    </div>

    <!-- CLIENT SCRIPT FOR STAFF SEARCH & PAGINATION -->
    <script>
        document.addEventListener('DOMContentLoaded', function() {
            const rows = Array.from(document.querySelectorAll('.staff-data-row'));
            const searchInput = document.getElementById('staffSearchInput');
            const pageSizeSelect = document.getElementById('staffPageSizeSelect');
            const paginationControls = document.getElementById('staffPaginationControls');
            const paginationInfo = document.getElementById('staffPaginationInfo');

            let currentPage = 1;
            let pageSize = parseInt(pageSizeSelect.value, 10) || 10;
            let searchText = '';

            function renderStaffTable() {
                const filtered = rows.filter(r => {
                    const name = r.dataset.name || '';
                    const email = r.dataset.email || '';
                    if (searchText && !name.includes(searchText) && !email.includes(searchText)) {
                        return false;
                    }
                    return true;
                });

                const totalFiltered = filtered.length;
                const actualPageSize = pageSize === 0 ? totalFiltered : pageSize;
                const totalPages = Math.ceil(totalFiltered / actualPageSize) || 1;

                if (currentPage > totalPages) currentPage = totalPages;
                if (currentPage < 1) currentPage = 1;

                rows.forEach(r => r.style.display = 'none');

                const start = (currentPage - 1) * actualPageSize;
                const end = pageSize === 0 ? totalFiltered : Math.min(start + actualPageSize, totalFiltered);

                for (let i = start; i < end; i++) {
                    if (filtered[i]) filtered[i].style.display = '';
                }

                if (paginationInfo) {
                    if (totalFiltered === 0) {
                        paginationInfo.innerHTML = 'Không tìm thấy nhân viên phù hợp';
                    } else {
                        paginationInfo.innerHTML = `Hiển thị <strong>${start + 1} - ${end}</strong> trên <strong>${totalFiltered}</strong> nhân viên`;
                    }
                }

                renderStaffControls(totalPages);
            }

            function renderStaffControls(totalPages) {
                if (!paginationControls) return;
                paginationControls.innerHTML = '';
                if (totalPages <= 1) return;

                const prev = document.createElement('button');
                prev.className = 'page-btn';
                prev.innerHTML = '‹';
                prev.disabled = currentPage === 1;
                prev.onclick = () => { if (currentPage > 1) { currentPage--; renderStaffTable(); } };
                paginationControls.appendChild(prev);

                for (let p = 1; p <= totalPages; p++) {
                    const btn = document.createElement('button');
                    btn.className = 'page-btn' + (p === currentPage ? ' active' : '');
                    btn.textContent = p;
                    btn.onclick = () => { currentPage = p; renderStaffTable(); };
                    paginationControls.appendChild(btn);
                }

                const next = document.createElement('button');
                next.className = 'page-btn';
                next.innerHTML = '›';
                next.disabled = currentPage === totalPages;
                next.onclick = () => { if (currentPage < totalPages) { currentPage++; renderStaffTable(); } };
                paginationControls.appendChild(next);
            }

            if (searchInput) {
                searchInput.addEventListener('input', (e) => {
                    searchText = e.target.value.toLowerCase().trim();
                    currentPage = 1;
                    renderStaffTable();
                });
            }

            if (pageSizeSelect) {
                pageSizeSelect.addEventListener('change', () => {
                    const val = pageSizeSelect.value;
                    pageSize = val === 'ALL' ? 0 : parseInt(val, 10);
                    currentPage = 1;
                    renderStaffTable();
                });
            }

            renderStaffTable();
        });
    </script>
<% } %>
</body>
</html>
