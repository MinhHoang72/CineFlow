<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quản lý Cụm rạp | CineFlow Admin</title>

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
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/centres.css">
</head>
<body>
<div class="cf-manager-shell">
    <!-- Menu bên trái (Sidebar) -->
    <jsp:include page="/WEB-INF/fragments/adminSidebar.jspf" />

    <div class="cf-manager-main">
        <!-- Thanh tiêu đề trên (Topbar) -->
        <jsp:include page="/WEB-INF/fragments/adminTopbar.jspf" />

        <main class="cf-content">
            <!-- Header trang -->
            <div class="cf-page-header">
                <div>
                    <h1>Quản lý Cụm rạp</h1>
                    <p>Danh sách các cụm rạp, thông tin liên hệ và trạng thái hoạt động trên toàn hệ thống</p>
                </div>
                <a href="${baseUrl}?action=create" class="cf-btn cf-btn-primary" style="display: flex; align-items: center; gap: 8px;">
                    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
                    Thêm Cụm rạp mới
                </a>
            </div>

            <!-- Thông báo thành công / lỗi -->
            <c:if test="${param.success == 'created'}">
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> <strong>Thành công!</strong> Đã thêm mới cụm rạp vào hệ thống.
                </div>
            </c:if>
            <c:if test="${param.success == 'updated'}">
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> <strong>Thành công!</strong> Đã cập nhật thông tin liên hệ và địa chỉ cụm rạp thành công.
                </div>
            </c:if>
            <c:if test="${param.success == 'status_updated'}">
                <div class="cf-alert cf-alert-success">
                    <span>✓</span> <strong>Thành công!</strong> Đã cập nhật trạng thái hoạt động của cụm rạp.
                </div>
            </c:if>
            <c:if test="${param.error == 'invalid_id'}">
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Cụm rạp không tồn tại hoặc mã định danh không hợp lệ!
                </div>
            </c:if>
            <c:if test="${param.error == 'failed'}">
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Không thể thực hiện thao tác. Vui lòng thử lại sau!
                </div>
            </c:if>
            <c:if test="${param.error == 'invalid_transition'}">
                <div class="cf-alert cf-alert-danger">
                    <span>⚠</span> <strong>Lỗi:</strong> Không thể chuyển trực tiếp giữa hai trạng thái này. Cụm rạp INACTIVE cần được chuyển về ACTIVE trước khi chuyển sang SUSPENDED.
                </div>
            </c:if>

            <!-- Thẻ thống kê (Stat Cards) -->
            <div class="cf-stats-grid">
                <div class="cf-stat-card">
                    <div class="cf-stat-icon primary">🏢</div>
                    <div class="cf-stat-info">
                        <h3>${totalCentres != null ? totalCentres : 0}</h3>
                        <p>Tổng số Cụm rạp</p>
                    </div>
                </div>
                <div class="cf-stat-card">
                    <div class="cf-stat-icon success">✓</div>
                    <div class="cf-stat-info">
                        <h3>${activeCentres != null ? activeCentres : 0}</h3>
                        <p>Đang hoạt động</p>
                    </div>
                </div>
                <div class="cf-stat-card">
                    <div class="cf-stat-icon warning">⏸</div>
                    <div class="cf-stat-info">
                        <h3>${suspendedCentres != null ? suspendedCentres : 0}</h3>
                        <p>Tạm ngưng</p>
                    </div>
                </div>
                <div class="cf-stat-card">
                    <div class="cf-stat-icon" style="background: rgba(239,68,68,.12); color: #F87171;">■</div>
                    <div class="cf-stat-info">
                        <h3>${inactiveCentres != null ? inactiveCentres : 0}</h3>
                        <p>Ngừng hoạt động</p>
                    </div>
                </div>
                <div class="cf-stat-card">
                    <div class="cf-stat-icon info">🎬</div>
                    <div class="cf-stat-info">
                        <h3>${totalHalls != null ? totalHalls : 0}</h3>
                        <p>Tổng số Phòng chiếu (Halls)</p>
                    </div>
                </div>
            </div>

            <!-- Thanh lọc & tìm kiếm -->
            <div class="cf-filter-card">
                <form class="cf-filter-form" method="GET" action="${baseUrl}">
                    <div class="cf-search-box">
                        <span class="icon">
                            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
                        </span>
                        <input type="text" name="search" placeholder="Tìm theo tên cụm rạp, hotline, địa chỉ, thành phố..." value="${currentSearch}">
                    </div>

                    <select name="status" class="cf-select">
                        <option value="ALL" ${currentStatus == 'ALL' || empty currentStatus ? 'selected' : ''}>Tất cả trạng thái</option>
                        <option value="ACTIVE" ${currentStatus == 'ACTIVE' ? 'selected' : ''}>Đang hoạt động</option>
                        <option value="SUSPENDED" ${currentStatus == 'SUSPENDED' ? 'selected' : ''}>Tạm ngưng</option>
                        <option value="INACTIVE" ${currentStatus == 'INACTIVE' ? 'selected' : ''}>Ngừng hoạt động</option>
                    </select>

                    <button type="submit" class="cf-btn cf-btn-primary">Lọc</button>
                    <a href="${baseUrl}" class="cf-btn cf-btn-secondary">Xóa lọc</a>
                </form>
            </div>

            <!---------------------------------------------------- Bảng danh sách Cụm rạp ------------------------------------------------------>
            <div class="cf-table-container">
                <table class="cf-table">
                    <thead>
                        <tr>
                            <th style="min-width: 180px;">Tên Cụm rạp</th>
                            <th style="min-width: 240px;">Địa chỉ & Thành phố</th>
                            <th style="min-width: 130px;">Hotline</th>
                            <th style="min-width: 170px;">Phòng chiếu trực thuộc</th>
                            <th style="min-width: 120px;">Trạng thái</th>
                            <th style="min-width: 310px; text-align: right;">Thao tác / trạng thái</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${not empty result.items}">
                                <c:forEach items="${result.items}" var="c">
                                    <c:set var="isActive" value="${c.status == 'ACTIVE'}" />
                                    <c:set var="isSuspended" value="${c.status == 'SUSPENDED'}" />
                                    <c:set var="isInactive" value="${c.status == 'INACTIVE'}" />
                                    <tr>
                                        <td>
                                            <strong style="color: #F8FAFC; font-size: 15px;">${c.centreName}</strong>
                                        </td>
                                        <td>
                                            <div style="color: #F8FAFC;">${c.address}</div>
                                            <c:if test="${not empty c.city}">
                                                <small style="color: #94A3B8;">${c.city}</small>
                                            </c:if>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${not empty c.phone}">
                                                    <span style="color: #F8FAFC;">${c.phone}</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span style="color: #94A3B8;">Chưa có</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <span class="cf-badge cf-badge-halls" style="cursor: default;">
                                                <span>🎬</span>
                                                <span>${c.hallCount} Phòng (${c.seatCount} ghế)</span>
                                            </span>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${isActive}">
                                                    <span class="cf-badge cf-badge-active">Đang hoạt động</span>
                                                </c:when>
                                                <c:when test="${isSuspended}">
                                                    <span class="cf-badge" style="background: rgba(245,158,11,.15); color: #FBBF24; border: 1px solid rgba(245,158,11,.35);">Tạm ngưng</span>
                                                </c:when>
                                                <c:when test="${isInactive}">
                                                    <span class="cf-badge cf-badge-inactive">Ngừng hoạt động</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="cf-badge cf-badge-inactive">● <c:out value="${c.status}"/></span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td style="text-align: right;">
                                            <div class="cf-actions" style="justify-content: flex-end;">
<%--------- SỬA--%>
                                                <a href="${baseUrl}?action=edit&centreId=${c.centreId}" class="cf-btn cf-btn-secondary cf-btn-sm">
                                                    Sửa
                                                </a>
                                                <form method="POST" action="${baseUrl}" style="display: inline-flex; align-items: center; gap: 6px; margin: 0;"
                                                      onsubmit="return confirm('Xác nhận cập nhật trạng thái cụm rạp? Việc tạm ngưng hoặc ngừng hoạt động có thể hủy các suất chiếu tương lai và vé liên quan.');">
                                                    <input type="hidden" name="action" value="changeStatus">
                                                    <input type="hidden" name="centreId" value="${c.centreId}">
                                                    <input type="hidden" name="back" value="${backQuery}">
                                                    <select name="targetStatus" class="cf-select" aria-label="Trạng thái cụm rạp" style="min-width: 155px; padding: 7px 26px 7px 9px;">
                                                        <option value="ACTIVE" ${isActive ? 'selected' : ''}>ACTIVE</option>
                                                        <option value="SUSPENDED" ${isSuspended ? 'selected' : ''}>SUSPENDED</option>
                                                        <option value="INACTIVE" ${isInactive ? 'selected' : ''}>INACTIVE</option>
                                                    </select>
                                                    <button type="submit" class="cf-btn cf-btn-primary cf-btn-sm">Lưu</button>
                                                </form>
                                            </div>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </c:when>
                            <c:otherwise>
                                <tr>
                                    <td colspan="6" style="text-align: center; padding: 40px; color: #94A3B8;">
                                        Không tìm thấy Cụm rạp nào phù hợp với điều kiện tìm kiếm.
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
                            Hiển thị <strong>${result.from} - ${result.to}</strong> trên tổng số <strong>${result.total}</strong> cụm rạp
                        </div>
                        ${result.renderPager(pagerBase)}
                    </div>
                </c:if>
            </div>
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
</script>
</body>
</html>
