<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.User,model.BookingHistory,java.util.List,java.math.BigDecimal,java.text.SimpleDateFormat" %>
<%
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && session.getAttribute("user") instanceof model.User) {
        authRole = ((model.User) session.getAttribute("user")).getRole();
    }
    if (!"ADMIN".equalsIgnoreCase(authRole)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Quản trị viên (ADMIN) mới có quyền truy cập trang này.");
        return;
    }

    request.setAttribute("activeNav", "customers");
    request.setAttribute("pageTitle", "Tài khoản Khách hàng");
    request.setAttribute("pageBreadcrumb", "Tài khoản Khách hàng");

    List<User> customers = (List<User>) request.getAttribute("customers");
    Integer totalCustomers = (Integer) request.getAttribute("totalCustomers");
    Integer activeCustomers = (Integer) request.getAttribute("activeCustomers");
    Integer suspendedCustomers = (Integer) request.getAttribute("suspendedCustomers");
    String currentSearch = (String) request.getAttribute("currentSearch");
    String currentStatus = (String) request.getAttribute("currentStatus");

    User customerDetail = (User) request.getAttribute("customerDetail");
    List<BookingHistory> bookingHistory = (List<BookingHistory>) request.getAttribute("bookingHistory");

    String successParam = request.getParameter("success");
    String errorParam = request.getParameter("error");

    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    SimpleDateFormat dfDate = new SimpleDateFormat("dd/MM/yyyy");
%>
<%!
    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
    private String money(BigDecimal n) {
        if (n == null) return "0 đ";
        return String.format("%,.0f đ", n);
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Quản lý Tài khoản Khách hàng | CineFlow Admin</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/reset.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/variables.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/buttons.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/badges.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/forms.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/tables.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/alerts.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/shell.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/dashboard.css">
    <style>
        .cf-stats-row {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 16px;
            margin-bottom: 24px;
        }
        .cf-stat-card {
            background: var(--cf-surface);
            border: 1px solid var(--cf-border);
            border-radius: var(--cf-radius-lg);
            padding: 20px 24px;
            display: flex;
            align-items: center;
            justify-content: space-between;
        }
        .cf-stat-label {
            font-size: 12px;
            font-weight: 700;
            color: var(--cf-muted);
            letter-spacing: .05em;
            text-transform: uppercase;
            margin-bottom: 6px;
        }
        .cf-stat-val {
            font-size: 26px;
            font-weight: 800;
            color: var(--cf-text);
        }
        .cf-stat-icon {
            width: 48px;
            height: 48px;
            border-radius: 12px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 22px;
        }

        .customer-filter-bar {
            display: flex;
            justify-content: space-between;
            align-items: center;
            gap: 16px;
            flex-wrap: wrap;
            margin-bottom: 18px;
            background: var(--cf-surface);
            padding: 16px 20px;
            border: 1px solid var(--cf-border);
            border-radius: var(--cf-radius-md);
        }
        .filter-tabs {
            display: flex;
            gap: 8px;
        }
        .filter-tab {
            padding: 7px 14px;
            border-radius: 6px;
            font-size: 13px;
            font-weight: 600;
            text-decoration: none;
            color: var(--cf-muted);
            border: 1px solid transparent;
            transition: all 0.2s;
        }
        .filter-tab:hover {
            color: var(--cf-text);
            background: rgba(255, 255, 255, 0.05);
        }
        .filter-tab.active {
            background: var(--cf-primary);
            color: #fff;
        }
        .search-box {
            display: flex;
            align-items: center;
            gap: 8px;
        }
        .search-input {
            width: 280px;
            background: rgba(255, 255, 255, 0.05);
            border: 1px solid var(--cf-border);
            border-radius: 6px;
            padding: 8px 12px;
            color: var(--cf-text);
            font-size: 13px;
            outline: none;
        }
        .search-input:focus {
            border-color: var(--cf-primary);
        }

        .user-cell {
            display: flex;
            align-items: center;
            gap: 12px;
        }
        .user-avatar-circle {
            width: 38px;
            height: 38px;
            border-radius: 50%;
            background: #233543;
            color: #FF2855;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: 700;
            font-size: 14px;
            overflow: hidden;
            flex-shrink: 0;
            border: 1px solid rgba(255, 40, 85, 0.3);
        }
        .user-avatar-circle img {
            width: 100%;
            height: 100%;
            object-fit: cover;
        }
        .user-name-link {
            font-weight: 700;
            color: var(--cf-text);
            text-decoration: none;
            font-size: 13.5px;
        }
        .user-code-tag {
            font-size: 11px;
            color: var(--cf-muted);
            margin-top: 2px;
        }

        /* Modal styling */
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
            background: #161B26;
            border: 1px solid var(--cf-border);
            border-radius: 14px;
            width: 100%;
            max-width: 720px;
            max-height: 90vh;
            display: flex;
            flex-direction: column;
            box-shadow: 0 24px 50px rgba(0, 0, 0, 0.5);
            overflow: hidden;
            animation: modalFadeIn 0.2s ease-out;
            color: #F8FAFC;
        }
        @keyframes modalFadeIn {
            from { opacity: 0; transform: translateY(-16px) scale(0.98); }
            to { opacity: 1; transform: translateY(0) scale(1); }
        }
        .cf-modal-header {
            padding: 20px 24px;
            border-bottom: 1px solid var(--cf-border);
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .cf-modal-header h3 { margin: 0; font-size: 17px; color: #F8FAFC; font-weight: 700; }
        .cf-modal-close {
            background: none; border: none; color: #94A3B8; font-size: 24px; cursor: pointer; line-height: 1;
        }
        .cf-modal-close:hover { color: #F8FAFC; }
        .cf-modal-body {
            padding: 24px;
            overflow-y: auto;
            display: flex;
            flex-direction: column;
            gap: 16px;
        }
        .cf-modal-footer {
            padding: 16px 24px;
            border-top: 1px solid var(--cf-border);
            display: flex;
            justify-content: flex-end;
            gap: 12px;
            background: #0F131D;
        }
    </style>
</head>
<body>
<div class="cf-manager-shell">
    <jsp:include page="/admin/sidebar.jsp" />

    <div class="cf-manager-main">
        <jsp:include page="/admin/topbar.jsp" />

        <main class="cf-content">
            <!-- TIÊU ĐỀ TRANG -->
            <div class="cf-page-header">
                <div>
                    <p class="cf-breadcrumb">Quản lý người dùng / Khách hàng</p>
                    <h1>Quản lý Tài khoản Khách hàng</h1>
                    <p>Theo dõi danh sách khách hàng đã đăng ký, kiểm soát trạng thái tài khoản và tra cứu lịch sử mua vé toàn hệ thống.</p>
                </div>
            </div>

            <!-- THÔNG BÁO ALERT -->
            <% if ("statusUpdated".equals(successParam)) { %>
                <div class="cf-alert cf-alert-success">✓ Cập nhật trạng thái tài khoản khách hàng thành công.</div>
            <% } else if ("statusFailed".equals(errorParam)) { %>
                <div class="cf-alert cf-alert-danger">✕ Cập nhật trạng thái thất bại. Vui lòng thử lại.</div>
            <% } %>

            <!-- THẺ THỐNG KÊ TỔNG QUAN -->
            <div class="cf-stats-row">
                <div class="cf-stat-card">
                    <div>
                        <div class="cf-stat-label">Tổng khách hàng</div>
                        <div class="cf-stat-val"><%= totalCustomers != null ? String.format("%,d", totalCustomers) : "0" %></div>
                    </div>
                    <div class="cf-stat-icon" style="background: rgba(255, 40, 85, 0.12); color: #FF2855;">👥</div>
                </div>

                <div class="cf-stat-card">
                    <div>
                        <div class="cf-stat-label">Đang hoạt động</div>
                        <div class="cf-stat-val" style="color: #10B981;"><%= activeCustomers != null ? String.format("%,d", activeCustomers) : "0" %></div>
                    </div>
                    <div class="cf-stat-icon" style="background: rgba(16, 185, 129, 0.12); color: #10B981;">✓</div>
                </div>

                <div class="cf-stat-card">
                    <div>
                        <div class="cf-stat-label">Bị tạm khóa</div>
                        <div class="cf-stat-val" style="color: #EF4444;"><%= suspendedCustomers != null ? String.format("%,d", suspendedCustomers) : "0" %></div>
                    </div>
                    <div class="cf-stat-icon" style="background: rgba(239, 68, 68, 0.12); color: #EF4444;">🔒</div>
                </div>
            </div>

            <!-- THANH TÌM KIẾM VÀ LỌC TRẠNG THÁI -->
            <div class="customer-filter-bar">
                <div class="filter-tabs">
                    <a href="${pageContext.request.contextPath}/admin/customers?status=ALL<%= currentSearch != null && !currentSearch.isBlank() ? "&search=" + esc(currentSearch) : "" %>"
                       class="filter-tab <%= "ALL".equalsIgnoreCase(currentStatus) ? "active" : "" %>">
                        Tất cả (<%= totalCustomers != null ? totalCustomers : 0 %>)
                    </a>
                    <a href="${pageContext.request.contextPath}/admin/customers?status=ACTIVE<%= currentSearch != null && !currentSearch.isBlank() ? "&search=" + esc(currentSearch) : "" %>"
                       class="filter-tab <%= "ACTIVE".equalsIgnoreCase(currentStatus) ? "active" : "" %>">
                        Đang hoạt động (<%= activeCustomers != null ? activeCustomers : 0 %>)
                    </a>
                    <a href="${pageContext.request.contextPath}/admin/customers?status=LOCKED<%= currentSearch != null && !currentSearch.isBlank() ? "&search=" + esc(currentSearch) : "" %>"
                       class="filter-tab <%= "LOCKED".equalsIgnoreCase(currentStatus) || "SUSPENDED".equalsIgnoreCase(currentStatus) ? "active" : "" %>">
                        Bị tạm khóa (<%= suspendedCustomers != null ? suspendedCustomers : 0 %>)
                    </a>
                </div>

                <form method="get" action="${pageContext.request.contextPath}/admin/customers" class="search-box">
                    <input type="hidden" name="status" value="<%= esc(currentStatus) %>">
                    <input type="text" name="search" class="search-input" placeholder="Tìm tên, username, email, SĐT..." value="<%= esc(currentSearch) %>">
                    <button type="submit" class="cf-btn cf-btn-secondary" style="padding: 7px 14px;">Tìm kiếm</button>
                    <% if (currentSearch != null && !currentSearch.isBlank()) { %>
                        <a href="${pageContext.request.contextPath}/admin/customers?status=<%= esc(currentStatus) %>" class="cf-btn cf-btn-secondary" style="padding: 7px 10px;" title="Xóa tìm kiếm">✕</a>
                    <% } %>
                </form>
            </div>

            <!-- BẢNG DANH SÁCH KHÁCH HÀNG -->
            <div class="cf-card">
                <div class="cf-table-wrap" style="margin: 0; border: none;">
                    <table class="cf-table">
                        <thead>
                            <tr>
                                <th>Khách hàng</th>
                                <th>Email & Xác minh</th>
                                <th>Số điện thoại</th>
                                <th>Đơn đặt vé</th>
                                <th>Tổng chi tiêu</th>
                                <th>Trạng thái</th>
                                <th style="text-align: right; min-width: 170px;">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% if (customers == null || customers.isEmpty()) { %>
                                <tr>
                                    <td colspan="7" class="cf-empty-state" style="padding: 48px; text-align: center; color: var(--cf-muted);">
                                        <div style="font-size: 32px; margin-bottom: 8px;">🔍</div>
                                        <strong>Không tìm thấy khách hàng nào phù hợp</strong>
                                        <p style="margin: 4px 0 0; font-size: 13px;">Hãy thử tìm kiếm với từ khóa khác hoặc bấm chọn "Tất cả".</p>
                                    </td>
                                </tr>
                            <% } else {
                                for (User c : customers) {
                                    boolean isActive = "ACTIVE".equalsIgnoreCase(c.getStatus());
                                    String initial = c.getFullName() != null && !c.getFullName().isBlank() 
                                            ? c.getFullName().substring(0, 1).toUpperCase() : "C";
                            %>
                                <tr>
                                    <!-- CỘT KHÁCH HÀNG -->
                                    <td>
                                        <div class="user-cell">
                                            <div class="user-avatar-circle">
                                                <% if (c.getAvatar() != null && !c.getAvatar().isBlank()) { %>
                                                    <img src="<%= c.getAvatar() %>" alt="<%= esc(c.getFullName()) %>" onerror="this.parentElement.innerHTML='<%= initial %>';">
                                                <% } else { %>
                                                    <%= initial %>
                                                <% } %>
                                            </div>
                                            <div>
                                                <div class="user-name-link"><%= esc(c.getFullName()) %></div>
                                                <div class="user-code-tag">
                                                    <%= c.getCustomerCode() %> · @<%= esc(c.getAccount()) %>
                                                </div>
                                            </div>
                                        </div>
                                    </td>

                                    <!-- CỘT EMAIL -->
                                    <td>
                                        <div style="font-weight: 500;"><%= esc(c.getEmail()) %></div>
                                        <div style="margin-top: 3px;">
                                            <% if (c.isEmailVerified()) { %>
                                                <span class="cf-badge cf-badge-success" style="font-size: 10px; padding: 1px 6px;">✓ Đã xác minh</span>
                                            <% } else { %>
                                                <span class="cf-badge cf-badge-muted" style="font-size: 10px; padding: 1px 6px;">Chưa xác minh</span>
                                            <% } %>
                                        </div>
                                    </td>

                                    <!-- CỘT SĐT -->
                                    <td>
                                        <%= c.getPhone() != null && !c.getPhone().isBlank() ? esc(c.getPhone()) : "—" %>
                                    </td>

                                    <!-- CỘT SỐ ĐƠN VÉ -->
                                    <td>
                                        <strong><%= c.getBookingCount() %></strong> đơn
                                    </td>

                                    <!-- CỘT TỔNG CHI TIÊU -->
                                    <td>
                                        <strong style="color: #F8FAFC;"><%= money(c.getTotalSpent()) %></strong>
                                    </td>

                                    <!-- CỘT TRẠNG THÁI -->
                                    <td>
                                        <% if (isActive) { %>
                                            <span class="cf-badge cf-badge-success">Đang hoạt động</span>
                                        <% } else if ("LOCKED".equalsIgnoreCase(c.getStatus()) || "SUSPENDED".equalsIgnoreCase(c.getStatus())) { %>
                                            <span class="cf-badge cf-badge-danger">Bị tạm khóa</span>
                                        <% } else { %>
                                            <span class="cf-badge cf-badge-muted"><%= esc(c.getStatus()) %></span>
                                        <% } %>
                                    </td>

                                    <!-- CỘT THAO TÁC -->
                                    <td style="text-align: right;">
                                        <div style="display: flex; align-items: center; justify-content: flex-end; gap: 6px;">
                                            <!-- Nút xem lịch sử vé -->
                                            <a href="${pageContext.request.contextPath}/admin/customers?status=<%= esc(currentStatus) %><%= currentSearch != null && !currentSearch.isBlank() ? "&search=" + esc(currentSearch) : "" %>&viewUserId=<%= c.getUserId() %>"
                                               class="cf-btn cf-btn-secondary cf-btn-sm" title="Xem lịch sử đặt vé">
                                                🎟️ Lịch sử vé
                                            </a>

                                            <!-- Form khóa / mở khóa -->
                                            <form method="post" action="${pageContext.request.contextPath}/admin/customers" style="display: inline;"
                                                  onsubmit="return confirm('<%= isActive ? "Bạn có chắc chắn muốn TẠM KHÓA tài khoản của khách hàng " + esc(c.getFullName()) + "?" : "Mở khóa tài khoản cho khách hàng " + esc(c.getFullName()) + "?" %>');">
                                                <input type="hidden" name="action" value="toggleStatus">
                                                <input type="hidden" name="userId" value="<%= c.getUserId() %>">
                                                <input type="hidden" name="newStatus" value="<%= isActive ? "LOCKED" : "ACTIVE" %>">
                                                <input type="hidden" name="status" value="<%= esc(currentStatus) %>">
                                                <input type="hidden" name="search" value="<%= esc(currentSearch) %>">

                                                <% if (isActive) { %>
                                                    <button type="submit" class="cf-btn cf-btn-sm" style="background: rgba(239, 68, 68, 0.15); color: #ef4444; border: 1px solid rgba(239, 68, 68, 0.4);" title="Khóa tài khoản này">
                                                        🔒 Khóa
                                                    </button>
                                                <% } else { %>
                                                    <button type="submit" class="cf-btn cf-btn-sm" style="background: rgba(16, 185, 129, 0.15); color: #10b981; border: 1px solid rgba(16, 185, 129, 0.4);" title="Mở khóa tài khoản này">
                                                        🔓 Mở khóa
                                                    </button>
                                                <% } %>
                                            </form>
                                        </div>
                                    </td>
                                </tr>
                            <% }} %>
                        </tbody>
                    </table>
                </div>
            </div>
        </main>
    </div>
</div>

<!-- =========================================================
     MODAL: XEM LỊCH SỬ ĐẶT VÉ CỦA KHÁCH HÀNG
     ========================================================= -->
<% if (customerDetail != null) { %>
<div class="cf-modal-backdrop active" id="historyModal">
    <div class="cf-modal">
        <div class="cf-modal-header">
            <div>
                <h3>Lịch sử đặt vé: <%= esc(customerDetail.getFullName()) %></h3>
                <div style="font-size: 12px; color: var(--cf-muted); margin-top: 3px;">
                    <%= customerDetail.getCustomerCode() %> · Email: <%= esc(customerDetail.getEmail()) %> · SĐT: <%= customerDetail.getPhone() != null ? esc(customerDetail.getPhone()) : "—" %>
                </div>
            </div>
            <a href="${pageContext.request.contextPath}/admin/customers?status=<%= esc(currentStatus) %><%= currentSearch != null && !currentSearch.isBlank() ? "&search=" + esc(currentSearch) : "" %>"
               class="cf-modal-close">&times;</a>
        </div>

        <div class="cf-modal-body">
            <% if (bookingHistory == null || bookingHistory.isEmpty()) { %>
                <div style="text-align: center; padding: 40px 20px; color: var(--cf-muted);">
                    <div style="font-size: 32px; margin-bottom: 8px;">🎫</div>
                    <strong>Khách hàng này chưa có giao dịch đặt vé nào</strong>
                </div>
            <% } else { %>
                <table class="cf-table">
                    <thead>
                        <tr>
                            <th>Mã đơn</th>
                            <th>Phim</th>
                            <th>Cụm rạp</th>
                            <th>Suất chiếu</th>
                            <th>Tổng tiền</th>
                            <th>Trạng thái</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% for (BookingHistory h : bookingHistory) {
                            String bStatus = h.getBookingStatus();
                            String bBadge = "CONFIRMED".equalsIgnoreCase(bStatus) || "PAID".equalsIgnoreCase(bStatus) ? "cf-badge-success" :
                                            "CANCELLED".equalsIgnoreCase(bStatus) ? "cf-badge-danger" : "cf-badge-info";
                        %>
                            <tr>
                                <td><strong style="color: #FF2855;"><%= esc(h.getBookingCode()) %></strong></td>
                                <td><%= esc(h.getMovieTitle()) %></td>
                                <td><%= esc(h.getCentreName()) %></td>
                                <td><%= h.getStartTime() != null ? sdf.format(h.getStartTime()) : "—" %></td>
                                <td><strong><%= money(h.getTotalAmount()) %></strong></td>
                                <td>
                                    <span class="cf-badge <%= bBadge %>"><%= esc(bStatus) %></span>
                                </td>
                            </tr>
                        <% } %>
                    </tbody>
                </table>
            <% } %>
        </div>

        <div class="cf-modal-footer">
            <a href="${pageContext.request.contextPath}/admin/customers?status=<%= esc(currentStatus) %><%= currentSearch != null && !currentSearch.isBlank() ? "&search=" + esc(currentSearch) : "" %>"
               class="cf-btn cf-btn-secondary">Đóng</a>
        </div>
    </div>
</div>
<% } %>

<script>
    document.addEventListener('DOMContentLoaded', function() {
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
</script>
</body>
</html>
