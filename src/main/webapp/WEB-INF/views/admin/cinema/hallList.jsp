<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%
    // 1. Kiểm tra xác thực và quyền hạn (Primary Actor: Centre Manager hoặc Admin có toàn quyền)
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && session.getAttribute("user") instanceof model.entity.User) {
        authRole = ((model.entity.User) session.getAttribute("user")).getRole();
    }
    if (!"MANAGER".equalsIgnoreCase(authRole) && !"ADMIN".equalsIgnoreCase(authRole)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Quản lý cụm rạp (MANAGER) hoặc Quản trị viên (ADMIN) mới có quyền truy cập trang này.");
        return;
    }

    boolean isAdmin = "ADMIN".equalsIgnoreCase(authRole);
    request.setAttribute("activeNav", "halls");
    request.setAttribute("pageTitle", "Quản lý Phòng chiếu");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quản lý Phòng chiếu | CineFlow</title>

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
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/halls.css?v=<%= System.currentTimeMillis() %>">
</head>
<body>
<div class="cf-manager-shell">
    <!-- Sidebar điều hướng tương ứng theo vai trò Admin / Manager -->
    <jsp:include page="<%= isAdmin ? \"/WEB-INF/fragments/adminSidebar.jspf\" : \"/WEB-INF/fragments/managerSidebar.jspf\" %>" />

    <div class="cf-manager-main">
        <!-- Topbar chung -->
        <jsp:include page="/WEB-INF/fragments/adminTopbar.jspf" />

        <main class="cf-content">
            <!-- Header trang -->
            <div class="cf-page-header">
                <div>
                    <h1>Quản lý Phòng chiếu</h1>
                    <p>Danh sách phòng chiếu, định dạng kỹ thuật 2D/3D và sức chứa thiết kế trực thuộc cụm rạp</p>
                </div>
                <c:choose>
                    <c:when test="${noCentreAssigned}">
                        <!-- Không hiển thị nút thêm khi chưa có cụm rạp -->
                    </c:when>
                    <c:otherwise>
                        <a href="${baseUrl}?action=create" class="cf-btn cf-btn-primary" style="display: flex; align-items: center; gap: 8px;">
                            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
                            Thêm phòng chiếu mới
                        </a>
                    </c:otherwise>
                </c:choose>
            </div>

            <!-- Thông báo kết quả thao tác -->
            <c:if test="${param.success == 'created'}">
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> <strong>Thành công!</strong> Thêm phòng chiếu mới thành công.
                </div>
            </c:if>
            <c:if test="${param.success == 'updated'}">
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> <strong>Thành công!</strong> Cập nhật thông tin phòng chiếu thành công.
                </div>
            </c:if>
            <c:if test="${param.success == 'status_updated'}">
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> <strong>Thành công!</strong> Đã cập nhật trạng thái hoạt động của phòng chiếu thành công.
                </div>
            </c:if>
            <c:if test="${param.error == 'has_future_bookings'}">
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Cảnh báo:</strong> Không thể vô hiệu hóa phòng chiếu vì đang có suất chiếu tương lai đã phát sinh vé đặt.
                </div>
            </c:if>
            <c:if test="${param.error == 'invalid_id' || param.error == 'invalid_hall'}">
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Phòng chiếu không tồn tại hoặc không thuộc chi nhánh này!
                </div>
            </c:if>
            <c:if test="${param.error == 'failed'}">
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Không thể thực hiện thao tác. Vui lòng thử lại sau!
                </div>
            </c:if>

            <c:choose>
                <c:when test="${noCentreAssigned}">
                    <!-- Trường hợp Manager chưa được gán Centre -->
                    <div class="cf-alert cf-alert-danger" style="margin-top: 20px;">
                        <span>⚠</span> <strong>Chưa phân công cụm rạp:</strong> Tài khoản Quản lý của bạn hiện chưa được phân công phụ trách Cụm rạp nào. Vui lòng liên hệ Quản trị viên (Admin) để được bổ nhiệm vào Cụm rạp.
                    </div>
                </c:when>
                <c:otherwise>
                    <!-- Banner Cụm rạp đang quản lý -->
                    <div class="cf-centre-banner">
                        <div class="cf-centre-meta">
                            <div class="cf-centre-icon">🏢</div>
                            <div class="cf-centre-info">
                                <h2>${currentCentre != null ? currentCentre.centreName : 'Cụm rạp #'.concat(targetCentreId)}</h2>
                                <p>
                                    📍 ${currentCentre != null ? currentCentre.address : ''}
                                    <c:if test="${not empty currentCentre.city}">
                                        , ${currentCentre.city}
                                    </c:if>
                                    <c:if test="${not empty currentCentre.phone}">
                                        &nbsp;|&nbsp; ☎ Hotline: ${currentCentre.phone}
                                    </c:if>
                                </p>
                            </div>
                        </div>

                        <c:if test="${isAdmin && not empty allCentres && allCentres.size() > 1}">
                            <!-- Dropdown chuyển đổi Cụm rạp dành cho Admin có toàn quyền -->
                            <div class="cf-centre-switch">
                                <label for="adminCentreSelect">Đổi Cụm rạp:</label>
                                <select id="adminCentreSelect" class="cf-centre-select" onchange="switchCentre(this.value)">
                                    <c:forEach items="${allCentres}" var="c">
                                        <option value="${c.centreId}" ${targetCentreId == c.centreId ? 'selected' : ''}>
                                            ${c.centreName} (${not empty c.city ? c.city : 'Cụm rạp #'.concat(c.centreId)})
                                        </option>
                                    </c:forEach>
                                </select>
                            </div>
                        </c:if>
                    </div>

                    <!-- Thống kê (Stat Cards) -->
                    <div class="cf-stats-grid">
                        <div class="cf-stat-card">
                            <div class="cf-stat-icon primary">🎬</div>
                            <div class="cf-stat-info">
                                <h3>${totalHalls != null ? totalHalls : 0}</h3>
                                <p>Tổng số phòng chiếu</p>
                            </div>
                        </div>
                        <div class="cf-stat-card">
                            <div class="cf-stat-icon success">✓</div>
                            <div class="cf-stat-info">
                                <h3>${activeHalls != null ? activeHalls : 0}</h3>
                                <p>Đang hoạt động (Active)</p>
                            </div>
                        </div>
                        <div class="cf-stat-card">
                            <div class="cf-stat-icon warning">⏸</div>
                            <div class="cf-stat-info">
                                <h3>${inactiveHalls != null ? inactiveHalls : 0}</h3>
                                <p>Tạm ngưng (Inactive)</p>
                            </div>
                        </div>
                        <div class="cf-stat-card">
                            <div class="cf-stat-icon info">💺</div>
                            <div class="cf-stat-info">
                                <h3>${totalCapacity != null ? totalCapacity : 0}</h3>
                                <p>Tổng sức chứa thiết kế (Ghế)</p>
                            </div>
                        </div>
                    </div>

                    <!-- Thanh tìm kiếm & bộ lọc phòng chiếu -->
                    <div class="cf-filter-card">
                        <form class="cf-filter-form" method="GET" action="${baseUrl}">
                            <c:if test="${isAdmin && targetCentreId != null}">
                                <input type="hidden" name="centreId" value="${targetCentreId}">
                            </c:if>
                            <div class="cf-search-box">
                                <span class="icon">
                                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
                                </span>
                                <input type="text" name="search" placeholder="Tìm theo tên phòng (VD: Hall 01)..." value="${currentSearch}">
                            </div>

                            <select name="status" class="cf-select">
                                <option value="ALL" ${currentStatus == 'ALL' || empty currentStatus ? 'selected' : ''}>Tất cả trạng thái & định dạng</option>
                                <option value="ACTIVE" ${currentStatus == 'ACTIVE' ? 'selected' : ''}>Đang hoạt động (Active)</option>
                                <option value="INACTIVE" ${currentStatus == 'INACTIVE' ? 'selected' : ''}>Tạm ngưng (Inactive)</option>
                                <option value="2D" ${currentStatus == '2D' ? 'selected' : ''}>Định dạng Standard 2D</option>
                                <option value="3D" ${currentStatus == '3D' ? 'selected' : ''}>Định dạng 3D</option>
                            </select>

                            <button type="submit" class="cf-btn cf-btn-primary">Lọc</button>
                            <a href="${baseUrl}" class="cf-btn cf-btn-secondary">Xóa lọc</a>
                        </form>
                    </div>

                    <!-- Bảng danh sách phòng chiếu -->
                    <div class="cf-table-container">
                        <table class="cf-table">
                            <thead>
                                <tr>
                                    <th style="min-width: 180px;">Tên phòng chiếu</th>
                                    <th style="min-width: 140px;">Định dạng kỹ thuật</th>
                                    <th style="min-width: 140px;">Tổng sức chứa</th>
                                    <th style="min-width: 140px;">Trạng thái</th>
                                    <th style="min-width: 160px; text-align: right;">Thao tác</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:choose>
                                    <c:when test="${not empty result.items}">
                                        <c:forEach items="${result.items}" var="h">
                                            <c:set var="isActive" value="${h.status == 'AVAILABLE' || h.status == 'ACTIVE'}" />
                                            <c:set var="is3D" value="${h.hallType == '3D'}" />
                                            <tr>
                                                <td>
                                                    <strong style="color: #F8FAFC; font-size: 15px;">${h.hallName}</strong>
                                                </td>
                                                <td>
                                                    <c:choose>
                                                        <c:when test="${is3D}">
                                                            <span class="cf-badge cf-badge-3d">★ 3D</span>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <span class="cf-badge cf-badge-2d">Standard 2D</span>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </td>
                                                <td>
                                                    <strong style="color: #F8FAFC;">${h.capacity}</strong> ghế
                                                </td>
                                                <td>
                                                    <c:choose>
                                                        <c:when test="${isActive}">
                                                            <span class="cf-badge cf-badge-active">● Đang hoạt động</span>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <span class="cf-badge cf-badge-inactive">● Tạm ngưng</span>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </td>
                                                <td style="text-align: right;">
                                                    <div class="cf-actions" style="justify-content: flex-end;">
                                                        <!-- Nút Configure Seats / Cấu hình sơ đồ ghế -->
                                                        <a href="${pageContext.request.contextPath}/manager/seat-config?hallId=${h.hallId}"
                                                           class="cf-btn cf-btn-sm" style="background: rgba(59, 130, 246, 0.15); color: #60A5FA; border: 1px solid rgba(59, 130, 246, 0.35);"
                                                           title="Cấu hình ma trận sơ đồ ghế và bảo trì">
                                                            💺 Cấu hình ghế
                                                        </a>

                                                        <!-- Nút Sửa -->
                                                        <a href="${baseUrl}?action=edit&hallId=${h.hallId}" class="cf-btn cf-btn-secondary cf-btn-sm">
                                                            Sửa
                                                        </a>

                                                        <!-- Nút Toggle Status (Bật/Tắt) -->
                                                        <form method="POST" action="${baseUrl}" style="display: inline-block; margin: 0;">
                                                            <input type="hidden" name="action" value="toggleStatus">
                                                            <input type="hidden" name="hallId" value="${h.hallId}">
                                                            <input type="hidden" name="centreId" value="${targetCentreId}">
                                                            <input type="hidden" name="back" value="${backQuery}">
                                                            <input type="hidden" name="search" value="${currentSearch}">
                                                            <input type="hidden" name="status" value="${currentStatus}">
                                                            <input type="hidden" name="page" value="${result.page}">

                                                            <c:choose>
                                                                <c:when test="${isActive}">
                                                                    <button type="submit" class="cf-btn cf-btn-danger cf-btn-sm" title="Vô hiệu hóa phòng chiếu">
                                                                        Tắt
                                                                    </button>
                                                                </c:when>
                                                                <c:otherwise>
                                                                    <button type="submit" class="cf-btn cf-btn-primary cf-btn-sm" style="background: #16A34A; border-color: #16A34A;" title="Kích hoạt phòng chiếu">
                                                                        Bật
                                                                    </button>
                                                                </c:otherwise>
                                                            </c:choose>
                                                        </form>
                                                    </div>
                                                </td>
                                            </tr>
                                        </c:forEach>
                                    </c:when>
                                    <c:otherwise>
                                        <tr>
                                            <td colspan="5" style="text-align: center; padding: 40px; color: #94A3B8;">
                                                Không tìm thấy phòng chiếu nào phù hợp. Bấm nút <strong>"Thêm phòng chiếu mới"</strong> để khởi tạo phòng.
                                            </td>
                                        </tr>
                                    </c:otherwise>
                                </c:choose>
                            </tbody>
                        </table>

                        <c:if test="${result.total > 0}">
                            <!-- THANH PHÂN TRANG (PAGINATION) -->
                            <div class="cf-pagination-bar">
                                <div class="cf-pagination-info">
                                    Hiển thị <strong>${result.from} - ${result.to}</strong> trên tổng số <strong>${result.total}</strong> phòng chiếu
                                </div>
                                ${result.renderPager(pagerBase)}
                            </div>
                        </c:if>
                    </div>
                </c:otherwise>
            </c:choose>
        </main>
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

    // Chuyển đổi cụm rạp (Dành cho Admin nếu có)
    function switchCentre(centreId) {
        if (centreId) {
            window.location.href = '${baseUrl}?centreId=' + encodeURIComponent(centreId);
        }
    }
</script>
</body>
</html>
