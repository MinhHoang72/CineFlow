<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.entity.User,model.entity.Centre,model.entity.StaffTransfer,model.entity.ManagerAssignment,java.util.List,java.text.SimpleDateFormat" %>
<%
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && session.getAttribute("user") instanceof model.entity.User) {
        authRole = ((model.entity.User) session.getAttribute("user")).getRole();
    }
    if (!"ADMIN".equalsIgnoreCase(authRole)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Quản trị viên (ADMIN) mới có quyền truy cập trang này.");
        return;
    }

    List<Centre> activeCentres = (List<Centre>) request.getAttribute("activeCentres");
    List<ManagerAssignment> managerAssignments = (List<ManagerAssignment>) request.getAttribute("managerAssignments");
    List<StaffTransfer> transferHistory = (List<StaffTransfer>) request.getAttribute("transferHistory");
    Integer selectedCentreId = (Integer) request.getAttribute("selectedCentreId");

    request.setAttribute("activeNav", "staff-history");
    request.setAttribute("pageTitle", "Nhật ký Nhân sự");

    String activeTab = (String) request.getAttribute("activeTab");
    if (activeTab == null || activeTab.trim().isEmpty()) {
        activeTab = "assignment";
    }

    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy");
%>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Nhật ký Nhân sự &amp; Kiểm toán | CineFlow Admin</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/reset.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/variables.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/buttons.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/badges.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/forms.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/tables.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/shell.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/dashboard.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/history.css?v=<%= System.currentTimeMillis() %>">
</head>
<body>
<div class="cf-manager-shell">
    <jsp:include page="/WEB-INF/fragments/adminSidebar.jspf" />

    <div class="cf-manager-main">
        <jsp:include page="/WEB-INF/fragments/adminTopbar.jspf" />

        <main class="cf-content">
            <!-- TIÊU ĐỀ TRANG -->
            <div class="cf-page-header cf-page-header-wrap">
                <div class="cf-page-header-info">
                    <div class="cf-page-icon icon-history">📜</div>
                    <div>
                        <h1 class="cf-page-title">Nhật ký nhân sự</h1>
                        <p class="cf-page-sub">Lịch sử bổ nhiệm và điều chuyển trong hệ thống.</p>
                    </div>
                </div>
            </div>

            <!-- CHỈ SỐ KPI TỔNG QUAN KIỂM TOÁN -->
            <div class="cf-kpi-grid-3">
                <div class="cf-kpi-card">
                    <span class="cf-kpi-label">Tổng lượt bổ nhiệm Quản lý</span>
                    <div class="cf-kpi-value"><%= request.getAttribute("kpiTotalAssignments") != null ? request.getAttribute("kpiTotalAssignments") : 0 %></div>
                    <span class="cf-kpi-sub">Tổng số bản ghi</span>
                </div>
                <div class="cf-kpi-card">
                    <span class="cf-kpi-label kpi-active">Phân công hiện tại</span>
                    <div class="cf-kpi-value kpi-active"><%= request.getAttribute("kpiActiveAssignments") != null ? request.getAttribute("kpiActiveAssignments") : 0 %></div>
                    <span class="cf-kpi-sub">Đang có hiệu lực</span>
                </div>
                <div class="cf-kpi-card">
                    <span class="cf-kpi-label kpi-transfer">Lượt điều chuyển</span>
                    <div class="cf-kpi-value kpi-transfer"><%= request.getAttribute("kpiTotalTransfers") != null ? request.getAttribute("kpiTotalTransfers") : 0 %></div>
                    <span class="cf-kpi-sub">Tổng số bản ghi</span>
                </div>
            </div>

            <!-- THANH BỘ LỌC CƠ SỞ RẠP, SẮP XẾP MÃ & TÌM KIẾM -->
            <div class="cf-card history-filter-card">
                <form method="get" action="${pageContext.request.contextPath}/admin/transfers/history" class="history-filter-form">
                    <input type="hidden" name="tab" id="filterTabInput" value="<%= activeTab %>">
                    
                    <div class="history-filter-group">
                        <span class="history-filter-label">Chi nhánh</span>
                        <select name="centreId" onchange="this.form.submit()" class="history-filter-select centre-select">
                            <option value="">Tất cả chi nhánh</option>
                            <% if (activeCentres != null) for (Centre c : activeCentres) { %>
                                <option value="<%= c.getCentreId() %>" <%= (selectedCentreId != null && selectedCentreId == c.getCentreId()) ? "selected" : "" %>>
                                    <%= c.getCentreName() %> (<%= c.getCity() %>)
                                </option>
                            <% } %>
                        </select>
                        <% if (selectedCentreId != null) { %>
                            <a href="${pageContext.request.contextPath}/admin/transfers/history?tab=<%= activeTab %>" class="cf-btn cf-btn-secondary cf-btn-sm btn-clear-filter">Xóa lọc</a>
                        <% } %>

                        <span class="history-filter-label sort-label">Sắp xếp</span>
                        <select id="historySortDropdown" onchange="applyHistorySort(this.value)" class="history-filter-select">
                            <option value="id_desc">Mã mới nhất</option>
                            <option value="id_asc">Mã cũ nhất</option>
                            <option value="emp_asc">Mã nhân sự tăng dần</option>
                            <option value="emp_desc">Mã nhân sự giảm dần</option>
                        </select>

                    </div>

                    <div>
                        <input type="text" id="liveSearchInput" placeholder="Tìm theo mã, tên hoặc tài khoản..." class="history-search-input">
                        <select id="transferRoleFilter" onchange="filterTransferHistory()" class="history-role-select">
                            <option value="">Tất cả chức vụ</option>
                            <option value="TICKET_SALES_STAFF">Bán vé</option>
                            <option value="TICKET_CHECKER_STAFF">Soát vé</option>
                        </select>
                    </div>
                </form>
            </div>

            <!-- THANH ĐIỀU HƯỚNG TAB LỊCH SỬ CHUYÊN BIỆT -->
            <div class="tab-nav">
                <button type="button" class="tab-btn <%= "assignment".equals(activeTab) ? "active" : "" %>" onclick="switchHistoryTab('tab-assignment-history', 'assignment')">
                    Bổ nhiệm &amp; miễn nhiệm
                </button>
                <button type="button" class="tab-btn <%= "transfer".equals(activeTab) ? "active" : "" %>" onclick="switchHistoryTab('tab-transfer-history', 'transfer')">
                    Điều chuyển
                </button>
            </div>

            <!-- ==================================================================== -->
            <!-- TAB 1: LỊCH SỬ BỔ NHIỆM & BÃI NHIỆM QUẢN LÝ (CENTRE MANAGER ASSIGNMENTS) -->
            <!-- ==================================================================== -->
            <div class="tab-pane <%= "assignment".equals(activeTab) ? "active" : "" %>" id="tab-assignment-history">
                <section class="cf-card">
                    <div class="cf-card-header">
                        <div>
                            <h2>Lịch sử Bổ nhiệm &amp; Miễn nhiệm Quản lý Trung tâm</h2>
                        </div>
                    </div>
                    <div class="cf-card-body">
                        <div class="cf-table-wrap">
                            <table class="cf-table" id="table-assignment">
                                <thead>
                                    <tr>
                                        <th>Mã</th>
                                        <th>Trung tâm chiếu phim</th>
                                        <th>Mã nhân sự</th>
                                        <th>Quản lý đương nhiệm</th>
                                        <th>Từ ngày</th>
                                        <th>Đến ngày</th>
                                        <th>Trạng thái nhiệm kỳ</th>
                                        <th>Người bổ nhiệm (Admin)</th>
                                        <th>Quyết định / Ghi chú</th>
                                    </tr>
                                </thead>
                                <tbody id="tbody-assignment">
                                    <% if (managerAssignments == null || managerAssignments.isEmpty()) { %>
                                        <tr><td colspan="9" class="cf-empty">Chưa có lịch sử bổ nhiệm nào được ghi nhận.</td></tr>
                                    <% } else for (ManagerAssignment ma : managerAssignments) { 
                                        String mSearch = (ma.getAssignmentId() + " " + ma.getManagerCode() + " " + ma.getManagerName() + " " + ma.getManagerAccount() + " " + ma.getCentreName() + " " + ma.getCentreCity()).toLowerCase();
                                    %>
                                        <tr class="history-row" data-search="<%= mSearch %>" data-id="<%= ma.getAssignmentId() %>" data-emp-id="<%= ma.getManagerId() %>" data-emp-code="<%= ma.getManagerCode() %>">
                                            <td><strong><%= ma.getAssignmentId() %></strong></td>
                                            <td>
                                                <strong class="history-cell-title"><%= ma.getCentreName() %></strong>
                                                <small class="history-cell-sub"><%= ma.getCentreCity() %></small>
                                            </td>
                                            <td><code class="cf-badge cf-badge-primary cf-badge-emp"><%= ma.getManagerCode() %></code></td>
                                            <td>
                                                <strong class="history-manager-name"><%= ma.getManagerName() %></strong>
                                                <small class="history-cell-sub">@<%= ma.getManagerAccount() %></small>
                                            </td>
                                            <td><%= ma.getStartDate() != null ? sdfDate.format(ma.getStartDate()) : "—" %></td>
                                            <td>
                                                <% if (ma.getEndDate() == null) { %>
                                                    <span class="cf-badge cf-badge-success">Đang đương nhiệm</span>
                                                <% } else { %>
                                                    <span class="history-date-ended"><%= sdfDate.format(ma.getEndDate()) %></span>
                                                <% } %>
                                            </td>
                                            <td>
                                                <span class="cf-badge <%= "ACTIVE".equalsIgnoreCase(ma.getStatus()) ? "cf-badge-success" : "REPLACED".equalsIgnoreCase(ma.getStatus()) ? "cf-badge-warning" : "cf-badge-danger" %>">
                                                    <%= "ACTIVE".equalsIgnoreCase(ma.getStatus()) ? "Đang hiệu lực" : "REPLACED".equalsIgnoreCase(ma.getStatus()) ? "Đã thay thế" : "Đã kết thúc" %>
                                                </span>
                                            </td>
                                            <td><span class="cf-badge cf-badge-info"><%= ma.getAssignedByName() %></span></td>
                                            <td class="history-notes"><%= ma.getNotes() != null ? ma.getNotes() : "—" %></td>
                                        </tr>
                                    <% } %>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </section>
            </div>

            <!-- ==================================================================== -->
            <!-- TAB 2: LỊCH SỬ ĐIỀU CHUYỂN NHÂN SỰ (STAFF TRANSFER) -->
            <!-- ==================================================================== -->
            <div class="tab-pane <%= "transfer".equals(activeTab) ? "active" : "" %>" id="tab-transfer-history">
                <section class="cf-card">
                    <div class="cf-card-header">
                        <div>
                            <h2>Điều chuyển nhân sự</h2>
                        </div>
                    </div>
                    <div class="cf-card-body">
                        <div class="cf-table-wrap">
                            <table class="cf-table" id="table-transfer">
                                <thead>
                                    <tr>
                                        <th>Mã</th>
                                        <th>Thời điểm</th>
                                        <th>Ngày hiệu lực</th>
                                        <th>Mã nhân sự</th>
                                        <th>Nhân sự</th>
                                        <th>Chức vụ</th>
                                        <th>Từ trung tâm</th>
                                        <th>Đến trung tâm</th>
                                        <th>Người thực hiện</th>
                                        <th>Lý do</th>
                                        <th>Ghi chú</th>
                                    </tr>
                                </thead>
                                <tbody id="tbody-transfer">
                                    <% if (transferHistory == null || transferHistory.isEmpty()) { %>
                                        <tr><td colspan="11" class="cf-empty">Chưa có lịch sử điều chuyển nào được ghi nhận.</td></tr>
                                    <% } else for (StaffTransfer t : transferHistory) { 
                                        String oldR = t.getOldRoleDisplayName();
                                        String newR = t.getNewRoleDisplayName();
                                        boolean roleChanged = (t.getOldRole() != null && t.getNewRole() != null && !t.getOldRole().equalsIgnoreCase(t.getNewRole()));
                                        String tSearch = (t.getTransferId() + " " + t.getEmployeeCode() + " " + t.getUserName() + " " + t.getUserAccount() + " " + (t.getFromCentreName() != null ? t.getFromCentreName() : "") + " " + t.getToCentreName() + " " + (t.getReason() != null ? t.getReason() : "")).toLowerCase();
                                    %>
                                        <tr class="history-row" data-search="<%= tSearch %>" data-role="<%= t.getNewRole() != null ? t.getNewRole() : t.getUserRole() %>" data-id="<%= t.getTransferId() %>" data-emp-id="<%= t.getUserId() %>" data-emp-code="<%= t.getEmployeeCode() %>">
                                            <td><strong><%= t.getTransferId() %></strong></td>
                                            <td><%= t.getTransferDate() != null ? sdf.format(t.getTransferDate()) : "—" %></td>
                                            <td>
                                                <% if (t.getEffectiveDate() != null) { %>
                                                    <strong class="history-effective-date"><%= sdfDate.format(t.getEffectiveDate()) %></strong>
                                                <% } else { %>
                                                    <span class="history-date-sub"><%= t.getTransferDate() != null ? sdfDate.format(t.getTransferDate()) : "—" %></span>
                                                <% } %>
                                            </td>
                                            <td><code class="cf-badge <%= "MANAGER".equalsIgnoreCase(t.getUserRole()) ? "cf-badge-primary" : "cf-badge-info" %> cf-badge-emp"><%= t.getEmployeeCode() %></code></td>
                                            <td>
                                                <strong class="history-cell-title"><%= t.getUserName() %></strong>
                                                <small class="history-cell-sub">@<%= t.getUserAccount() %></small>
                                            </td>
                                            <td>
                                                <% if (roleChanged) { %>
                                                    <div class="history-role-change">
                                                        <span class="history-old-role"><%= oldR %></span>
                                                        <span class="history-role-arrow">➔</span>
                                                        <strong class="history-new-role"><%= newR %></strong>
                                                    </div>
                                                <% } else { %>
                                                    <span class="history-same-role"><%= newR %></span>
                                                <% } %>
                                            </td>
                                            <td><%= t.getFromCentreName() != null ? t.getFromCentreName() : "Chưa phân trung tâm" %></td>
                                            <td><strong class="history-dest-centre"><%= t.getToCentreName() %></strong></td>
                                            <td><span class="cf-badge cf-badge-info"><%= t.getTransferredByName() %></span></td>
                                            <td class="history-reason"><%= t.getReason() != null ? t.getReason() : "—" %></td>
                                            <td class="history-notes"><%= t.getNotes() != null && !t.getNotes().isEmpty() ? t.getNotes() : "—" %></td>
                                        </tr>
                                    <% } %>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </section>
            </div>
        </main>
    </div>
</div>

<script>
    function switchHistoryTab(tabId, tabName) {
        document.querySelectorAll('.tab-btn').forEach(function(b) { b.classList.remove('active'); });
        document.querySelectorAll('.tab-pane').forEach(function(p) { p.classList.remove('active'); });
        event.target.classList.add('active');
        document.getElementById(tabId).classList.add('active');
        var roleFilter = document.getElementById('transferRoleFilter');
        if (roleFilter) roleFilter.style.display = tabName === 'transfer' ? '' : 'none';

        var filterTab = document.getElementById('filterTabInput');
        if (filterTab) filterTab.value = tabName;

        if (window.history.replaceState) {
            var url = new URL(window.location);
            url.searchParams.set('tab', tabName);
            window.history.replaceState({}, '', url);
        }
    }

    // Bộ quản lý trạng thái sắp xếp
    var sortStates = {};

    function sortTableByCol(tableId, colIndex, dataType) {
        var table = document.getElementById(tableId);
        if (!table) return;
        var tbody = table.querySelector('tbody');
        var rows = Array.from(tbody.querySelectorAll('tr.history-row'));
        if (rows.length === 0) return;

        var key = tableId + '_' + colIndex;
        var currentOrder = sortStates[key] || 'desc';
        var nextOrder = currentOrder === 'asc' ? 'desc' : 'asc';
        sortStates[key] = nextOrder;

        rows.sort(function(a, b) {
            var cellA = a.children[colIndex] ? a.children[colIndex].innerText.trim() : '';
            var cellB = b.children[colIndex] ? b.children[colIndex].innerText.trim() : '';

            if (dataType === 'num') {
                var numA = parseFloat(cellA.replace(/[^0-9.-]+/g, '')) || 0;
                var numB = parseFloat(cellB.replace(/[^0-9.-]+/g, '')) || 0;
                return nextOrder === 'asc' ? numA - numB : numB - numA;
            } else {
                return nextOrder === 'asc' ? cellA.localeCompare(cellB, 'vi', { numeric: true }) : cellB.localeCompare(cellA, 'vi', { numeric: true });
            }
        });

        rows.forEach(function(r) { tbody.appendChild(r); });

        // Cập nhật mũi tên hiển thị
        table.querySelectorAll('.sort-arrow').forEach(function(ar) {
            ar.textContent = '↕';
            ar.classList.remove('active');
        });
        var targetArrow = document.getElementById('arrow-' + tableId + '-' + colIndex);
        if (targetArrow) {
            targetArrow.textContent = nextOrder === 'asc' ? '▲ (Thấp ➔ Cao)' : '▼ (Cao ➔ Thấp)';
            targetArrow.classList.add('active');
        }
    }

    // Xử lý khi chọn dropdown sắp xếp
    function applyHistorySort(sortVal) {
        var activeTab = document.querySelector('.tab-pane.active');
        if (!activeTab) return;
        var table = activeTab.querySelector('table');
        if (!table) return;

        if (sortVal === 'id_asc') {
            sortTableByCol(table.id, 0, 'num');
            if (sortStates[table.id + '_0'] !== 'asc') sortTableByCol(table.id, 0, 'num');
        } else if (sortVal === 'id_desc') {
            sortTableByCol(table.id, 0, 'num');
            if (sortStates[table.id + '_0'] !== 'desc') sortTableByCol(table.id, 0, 'num');
        } else if (sortVal === 'emp_asc') {
            var empColIndex = table.id === 'table-assignment' ? 2 : 3;
            sortTableByCol(table.id, empColIndex, 'str');
            if (sortStates[table.id + '_' + empColIndex] !== 'asc') sortTableByCol(table.id, empColIndex, 'str');
        } else if (sortVal === 'emp_desc') {
            var empColIndex = table.id === 'table-assignment' ? 2 : 3;
            sortTableByCol(table.id, empColIndex, 'str');
            if (sortStates[table.id + '_' + empColIndex] !== 'desc') sortTableByCol(table.id, empColIndex, 'str');
        }
    }

    // Live search lọc client-side
    document.addEventListener('DOMContentLoaded', function() {
        var input = document.getElementById('liveSearchInput');
        var roleFilter = document.getElementById('transferRoleFilter');
        if (roleFilter) roleFilter.style.display = '<%= "transfer".equals(activeTab) ? "" : "none" %>';
        function applyHistoryFilters() {
            var q = input ? input.value.toLowerCase().trim() : '';
            var role = roleFilter ? roleFilter.value.toLowerCase() : '';
            document.querySelectorAll('#tbody-transfer .history-row').forEach(function(row) {
                var matchesSearch = !q || (row.getAttribute('data-search') || '').indexOf(q) !== -1;
                var rowRole = (row.getAttribute('data-role') || '').toLowerCase();
                var matchesRole = !role || rowRole === role;
                row.style.display = matchesSearch && matchesRole ? '' : 'none';
            });
        }
        window.filterTransferHistory = applyHistoryFilters;
        if (input) {
            input.addEventListener('input', function() {
                applyHistoryFilters();
            });
        }
        if (roleFilter) roleFilter.addEventListener('change', applyHistoryFilters);
    });
</script>
</body>
</html>
