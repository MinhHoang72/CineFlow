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
                    <div>
                        <h1 class="cf-page-title">Nhật ký nhân sự</h1>
                    </div>
                </div>
            </div>

            <!-- CHỈ SỐ KPI TỔNG QUAN KIỂM TOÁN -->
            <div class="cf-kpi-grid-3">
                <div class="cf-kpi-card">
                    <span class="cf-kpi-label">Tổng lượt bổ nhiệm Quản lý</span>
                    <div class="cf-kpi-value"><%= request.getAttribute("kpiTotalAssignments") != null ? request.getAttribute("kpiTotalAssignments") : 0 %></div>
                </div>
                <div class="cf-kpi-card">
                    <span class="cf-kpi-label kpi-active">Phân công hiện tại</span>
                    <div class="cf-kpi-value kpi-active"><%= request.getAttribute("kpiActiveAssignments") != null ? request.getAttribute("kpiActiveAssignments") : 0 %></div>
                </div>
                <div class="cf-kpi-card">
                    <span class="cf-kpi-label kpi-transfer">Lượt điều chuyển nhân viên</span>
                    <div class="cf-kpi-value kpi-transfer"><%= request.getAttribute("kpiTotalTransfers") != null ? request.getAttribute("kpiTotalTransfers") : 0 %></div>
                </div>
            </div>

            <!-- THANH ĐIỀU HƯỚNG TAB LỊCH SỬ CHUYÊN BIỆT (GIỐNG TRANSFERS.JSP) -->
            <div class="tab-nav">
                <button type="button" class="tab-btn <%= "assignment".equals(activeTab) ? "active" : "" %>" onclick="switchHistoryTab('tab-assignment-history', 'assignment')">
                    🏛 Bổ nhiệm &amp; Miễn nhiệm
                </button>
                <button type="button" class="tab-btn <%= "transfer".equals(activeTab) ? "active" : "" %>" onclick="switchHistoryTab('tab-transfer-history', 'transfer')">
                    👥 Điều chuyển nhân viên
                </button>
            </div>

            <!-- ==================================================================== -->
            <!-- TAB 1: LỊCH SỬ BỔ NHIỆM & BÃI NHIỆM QUẢN LÝ (CENTRE MANAGER ASSIGNMENTS) -->
            <!-- ==================================================================== -->
            <div class="tab-pane <%= "assignment".equals(activeTab) ? "active" : "" %>" id="tab-assignment-history">
                <section class="cf-card">
                    <div class="cf-card-header cf-card-header-flex">
                        <h2>Lịch sử Bổ nhiệm &amp; Miễn nhiệm</h2>
                        <div class="staff-filter-toolbar">
                            <form method="get" action="${pageContext.request.contextPath}/admin/transfers/history" class="staff-filter-form">
                                <input type="hidden" name="tab" value="assignment">
                                
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
                                    <a href="${pageContext.request.contextPath}/admin/transfers/history?tab=assignment" class="cf-btn cf-btn-secondary cf-btn-sm btn-clear-filter" title="Xóa lọc chi nhánh">✕</a>
                                <% } %>

                                <span class="history-filter-label">Sắp xếp</span>
                                <select id="assignmentSortDropdown" onchange="applyHistorySort(this.value, 'table-assignment')" class="history-filter-select sort-select">
                                    <option value="id_desc">Mã mới nhất</option>
                                    <option value="id_asc">Mã cũ nhất</option>
                                    <option value="emp_asc">Mã nhân sự tăng dần</option>
                                    <option value="emp_desc">Mã nhân sự giảm dần</option>
                                </select>

                                <div class="history-search-wrap">
                                    <span class="history-search-icon">
                                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                            <circle cx="11" cy="11" r="8"></circle>
                                            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                                        </svg>
                                    </span>
                                    <input type="text" id="assignmentSearchInput" placeholder="Tìm kiếm..." class="history-search-input">
                                    <button type="button" id="assignmentSearchClearBtn" class="cf-search-clear-btn" style="display: none;" title="Xóa tìm kiếm">✕</button>
                                </div>

                                <div class="cf-page-size-wrap">
                                    <span class="history-filter-label">Hiển thị:</span>
                                    <select id="assignmentPageSizeSelect" class="cf-select-sm history-filter-select">
                                        <option value="10" selected>10 bản ghi</option>
                                        <option value="20">20 bản ghi</option>
                                        <option value="50">50 bản ghi</option>
                                        <option value="ALL">Tất cả</option>
                                    </select>
                                </div>
                            </form>
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
                                    <tr class="empty-row" id="assignmentEmptyRow" style="display: none;">
                                        <td colspan="9" class="cf-empty">Không tìm thấy bản ghi bổ nhiệm nào phù hợp.</td>
                                    </tr>
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

                    <!-- THANH PHÂN TRANG TAB 1 -->
                    <div class="cf-pagination-bar">
                        <div class="cf-pagination-info" id="assignmentPaginationInfo">
                            Đang nạp nhật ký bổ nhiệm...
                        </div>
                        <div class="cf-pagination-controls" id="assignmentPaginationControls">
                            <!-- Các nút phân trang được sinh động bởi JS -->
                        </div>
                    </div>
                
                </section>
            </div>

            <!-- ==================================================================== -->
            <!-- TAB 2: LỊCH SỬ ĐIỀU CHUYỂN NHÂN SỰ TOÀN HỆ THỐNG (STAFF TRANSFERS) -->
            <!-- ==================================================================== -->
            <div class="tab-pane <%= "transfer".equals(activeTab) ? "active" : "" %>" id="tab-transfer-history">
                <section class="cf-card">
                    <div class="cf-card-header cf-card-header-flex">
                        <h2>Điều chuyển nhân viên</h2>
                        <div class="staff-filter-toolbar">
                            <form method="get" action="${pageContext.request.contextPath}/admin/transfers/history" class="staff-filter-form">
                                <input type="hidden" name="tab" value="transfer">
                                
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
                                    <a href="${pageContext.request.contextPath}/admin/transfers/history?tab=transfer" class="cf-btn cf-btn-secondary cf-btn-sm btn-clear-filter" title="Xóa lọc chi nhánh">✕</a>
                                <% } %>

                                <span class="history-filter-label">Sắp xếp</span>
                                <select id="transferSortDropdown" onchange="applyHistorySort(this.value, 'table-transfer')" class="history-filter-select sort-select">
                                    <option value="id_desc">Mã mới nhất</option>
                                    <option value="id_asc">Mã cũ nhất</option>
                                    <option value="emp_asc">Mã nhân sự tăng dần</option>
                                    <option value="emp_desc">Mã nhân sự giảm dần</option>
                                </select>

                                <select id="transferRoleFilter" onchange="filterTransferHistory()" class="history-filter-select history-role-select">
                                    <option value="">Tất cả chức vụ</option>
                                    <option value="TICKET_SALES_STAFF">Bán vé</option>
                                    <option value="TICKET_CHECKER_STAFF">Soát vé</option>
                                </select>

                                <div class="history-search-wrap">
                                    <span class="history-search-icon">
                                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                            <circle cx="11" cy="11" r="8"></circle>
                                            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                                        </svg>
                                    </span>
                                    <input type="text" id="transferSearchInput" placeholder="Tìm kiếm..." class="history-search-input">
                                    <button type="button" id="transferSearchClearBtn" class="cf-search-clear-btn" style="display: none;" title="Xóa tìm kiếm">✕</button>
                                </div>

                                <div class="cf-page-size-wrap">
                                    <span class="history-filter-label">Hiển thị:</span>
                                    <select id="transferPageSizeSelect" class="cf-select-sm history-filter-select">
                                        <option value="10" selected>10 bản ghi</option>
                                        <option value="20">20 bản ghi</option>
                                        <option value="50">50 bản ghi</option>
                                        <option value="ALL">Tất cả</option>
                                    </select>
                                </div>
                            </form>
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
                                        <th>Mã nhân viên</th>
                                        <th>Nhân viên</th>
                                        <th>Chức vụ</th>
                                        <th>Từ trung tâm</th>
                                        <th>Đến trung tâm</th>
                                        <th>Người thực hiện</th>
                                        <th>Lý do</th>
                                        <th>Ghi chú</th>
                                    </tr>
                                </thead>
                                <tbody id="tbody-transfer">
                                    <tr class="empty-row" id="transferEmptyRow" style="display: none;">
                                        <td colspan="11" class="cf-empty">Không tìm thấy bản ghi điều chuyển nào phù hợp.</td>
                                    </tr>
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
                                            <td><code class="cf-badge <%= "cf-badge-info" %> cf-badge-emp"><%= t.getEmployeeCode() %></code></td>
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

                    <!-- THANH PHÂN TRANG TAB 2 -->
                    <div class="cf-pagination-bar">
                        <div class="cf-pagination-info" id="transferPaginationInfo">
                            Đang nạp nhật ký điều chuyển...
                        </div>
                        <div class="cf-pagination-controls" id="transferPaginationControls">
                            <!-- Các nút phân trang được sinh động bởi JS -->
                        </div>
                    </div>
                
                </section>
            </div>
        </main>
    </div>
</div>

<script src="${pageContext.request.contextPath}/js/admin-pagination.js?v=<%= System.currentTimeMillis() %>"></script>
<script>
    var assignmentPager = null;
    var transferPager = null;

    function switchHistoryTab(tabId, tabName) {
        document.querySelectorAll('.tab-btn').forEach(function(b) { b.classList.remove('active'); });
        document.querySelectorAll('.tab-pane').forEach(function(p) { p.classList.remove('active'); });
        
        if (window.event && window.event.currentTarget) {
            window.event.currentTarget.classList.add('active');
        } else if (window.event && window.event.target) {
            var btn = window.event.target.closest('.tab-btn');
            if (btn) btn.classList.add('active');
        }

        var targetPane = document.getElementById(tabId);
        if (targetPane) targetPane.classList.add('active');

        if (window.history.replaceState) {
            var url = new URL(window.location);
            url.searchParams.set('tab', tabName);
            window.history.replaceState({}, '', url);
        }

        if (tabName === 'assignment' && assignmentPager) {
            assignmentPager.render();
        } else if (tabName === 'transfer' && transferPager) {
            transferPager.render();
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
        if (assignmentPager) assignmentPager.refreshRows();
        if (transferPager) transferPager.refreshRows();

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
    function applyHistorySort(sortVal, targetTableId) {
        var tableId = targetTableId;
        if (!tableId) {
            var activeTab = document.querySelector('.tab-pane.active');
            if (!activeTab) return;
            var table = activeTab.querySelector('table');
            if (!table) return;
            tableId = table.id;
        }

        if (sortVal === 'id_asc') {
            sortTableByCol(tableId, 0, 'num');
            if (sortStates[tableId + '_0'] !== 'asc') sortTableByCol(tableId, 0, 'num');
        } else if (sortVal === 'id_desc') {
            sortTableByCol(tableId, 0, 'num');
            if (sortStates[tableId + '_0'] !== 'desc') sortTableByCol(tableId, 0, 'num');
        } else if (sortVal === 'emp_asc') {
            var empColIndex = tableId === 'table-assignment' ? 2 : 3;
            sortTableByCol(tableId, empColIndex, 'str');
            if (sortStates[tableId + '_' + empColIndex] !== 'asc') sortTableByCol(tableId, empColIndex, 'str');
        } else if (sortVal === 'emp_desc') {
            var empColIndex = tableId === 'table-assignment' ? 2 : 3;
            sortTableByCol(tableId, empColIndex, 'str');
            if (sortStates[tableId + '_' + empColIndex] !== 'desc') sortTableByCol(tableId, empColIndex, 'str');
        }
    }

    // Khởi tạo phân trang & tìm kiếm client-side
    document.addEventListener('DOMContentLoaded', function() {
        assignmentPager = initTablePagination({
            tableBodyId: 'tbody-assignment',
            rowSelector: 'tr.history-row',
            emptyRowId: 'assignmentEmptyRow',
            paginationId: 'assignmentPaginationControls',
            infoDisplayId: 'assignmentPaginationInfo',
            pageSizeSelectId: 'assignmentPageSizeSelect',
            searchInputId: 'assignmentSearchInput',
            searchClearBtnId: 'assignmentSearchClearBtn',
            itemLabel: 'bản ghi',
            defaultPageSize: 10
        });

        transferPager = initTablePagination({
            tableBodyId: 'tbody-transfer',
            rowSelector: 'tr.history-row',
            emptyRowId: 'transferEmptyRow',
            paginationId: 'transferPaginationControls',
            infoDisplayId: 'transferPaginationInfo',
            pageSizeSelectId: 'transferPageSizeSelect',
            searchInputId: 'transferSearchInput',
            searchClearBtnId: 'transferSearchClearBtn',
            itemLabel: 'bản ghi',
            defaultPageSize: 10,
            customFilter: function(row) {
                var rf = document.getElementById('transferRoleFilter');
                var role = rf ? rf.value.toLowerCase() : '';
                if (!role) return true;
                var rowRole = (row.getAttribute('data-role') || '').toLowerCase();
                return rowRole === role;
            }
        });

        window.filterTransferHistory = function() {
            if (transferPager) transferPager.render();
        };

        var rf = document.getElementById('transferRoleFilter');
        if (rf) {
            rf.addEventListener('change', function() {
                if (transferPager) transferPager.render();
            });
        }
    });
</script>
</body>
</html>