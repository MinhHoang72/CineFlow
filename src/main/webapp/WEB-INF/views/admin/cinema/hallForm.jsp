<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${hall != null ? 'Sửa thông tin Phòng chiếu' : 'Thêm Phòng chiếu mới'} | CineFlow</title>

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
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/alerts.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/shell.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/dashboard.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/halls.css">

    <style>
        .cf-form-card {
            background: var(--cf-surface, #161B26);
            border: 1px solid var(--cf-border, rgba(255, 255, 255, 0.08));
            border-radius: 14px;
            padding: 28px;
            max-width: 680px;
            box-shadow: 0 4px 24px rgba(0, 0, 0, 0.3);
        }
        .cf-form-actions {
            display: flex;
            align-items: center;
            gap: 12px;
            margin-top: 24px;
            padding-top: 20px;
            border-top: 1px solid rgba(255, 255, 255, 0.08);
        }
    </style>
</head>
<body>
<div class="cf-manager-shell">
    <!-- Menu bên trái (Sidebar) -->
    <jsp:include page="/WEB-INF/fragments/managerSidebar.jspf" />

    <div class="cf-manager-main">
        <!-- Thanh tiêu đề trên (Topbar) -->
        <jsp:include page="/WEB-INF/fragments/adminTopbar.jspf" />

        <main class="cf-content">
            <!-- Header trang -->
            <div class="cf-page-header">
                <div>
                    <h1>${hall != null ? 'Sửa thông tin Phòng chiếu' : 'Thêm Phòng chiếu mới'}</h1>
                    <p>
                        🏢 Cụm rạp: <strong>${currentCentre != null ? currentCentre.centreName : 'Chi nhánh'}</strong>
                        <c:if test="${not empty currentCentre.city}">
                            (${currentCentre.city})
                        </c:if>
                    </p>
                </div>
                <a href="${baseUrl}" class="cf-btn cf-btn-secondary" style="display: flex; align-items: center; gap: 6px;">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><line x1="19" y1="12" x2="5" y2="12"></line><polyline points="12 19 5 12 12 5"></polyline></svg>
                    Quay lại danh sách
                </a>
            </div>

            <!-- Thông báo lỗi nếu có -->
            <c:if test="${param.error == 'duplicate_name'}">
                <div class="cf-alert cf-alert-danger" style="margin-bottom: 20px;">
                    <span>⚠</span> <strong>Lỗi:</strong> Tên phòng chiếu đã tồn tại tại cụm rạp này. Vui lòng chọn tên khác!
                </div>
            </c:if>
            <c:if test="${param.error == 'missing_fields'}">
                <div class="cf-alert cf-alert-danger" style="margin-bottom: 20px;">
                    <span>⚠</span> <strong>Lỗi:</strong> Vui lòng điền đầy đủ các thông tin bắt buộc (Tên phòng, Sức chứa).
                </div>
            </c:if>
            <c:if test="${param.error == 'has_future_bookings'}">
                <div class="cf-alert cf-alert-danger" style="margin-bottom: 20px;">
                    <span>⚠</span> <strong>Cảnh báo:</strong> Không thể vô hiệu hóa phòng chiếu vì đang có suất chiếu tương lai đã phát sinh vé đặt!
                </div>
            </c:if>
            <c:if test="${param.error == 'failed'}">
                <div class="cf-alert cf-alert-danger" style="margin-bottom: 20px;">
                    <span>⚠</span> <strong>Lỗi:</strong> Không thể lưu thông tin phòng chiếu. Vui lòng thử lại sau!
                </div>
            </c:if>

            <!-- Khung Form dùng chung -->
            <div class="cf-form-card">
                <form method="POST" action="${baseUrl}">
                    <!-- Nếu có hall (sửa) thì action=update, ngược lại action=create -->
                    <input type="hidden" name="action" value="${hall != null ? 'update' : 'create'}">
                    <c:if test="${hall != null}">
                        <input type="hidden" name="hallId" value="${hall.hallId}">
                    </c:if>

                    <div style="display: flex; flex-direction: column; gap: 18px;">
                        <div class="cf-form-group">
                            <label>Tên Phòng chiếu *</label>
                            <input type="text" name="hallName" value="${hall != null ? hall.hallName : ''}" 
                                   placeholder="Ví dụ: Phòng chiếu 01, Cinema 02..." required>
                            <span class="cf-form-hint">Tên phòng không được trùng lặp với phòng khác trong cùng chi nhánh.</span>
                        </div>

                        <div class="cf-form-group">
                            <label>Định dạng kỹ thuật *</label>
                            <select name="hallType" class="cf-select" style="width: 100%;">
                                <option value="2D" ${hall != null && hall.hallType == '2D' ? 'selected' : ''}>Standard 2D</option>
                                <option value="3D" ${hall != null && hall.hallType == '3D' ? 'selected' : ''}>Digital 3D</option>
                            </select>
                            <span class="cf-form-hint">Định dạng chiếu phim được trang bị tại phòng này.</span>
                        </div>

                        <div class="cf-form-group">
                            <label>Sức chứa thiết kế (Số ghế) *</label>
                            <input type="number" name="capacity" min="10" max="500" 
                                   value="${hall != null ? hall.capacity : 60}" required>
                            <span class="cf-form-hint">Mặc định 60 ghế (hệ thống sẽ tạo ma trận sơ đồ ghế tương ứng).</span>
                        </div>

                        <div class="cf-form-group">
                            <label>Trạng thái hoạt động</label>
                            <select name="status" class="cf-select" style="width: 100%;">
                                <option value="AVAILABLE" ${hall == null || hall.status == 'AVAILABLE' || hall.status == 'ACTIVE' ? 'selected' : ''}>
                                    Đang hoạt động (AVAILABLE)
                                </option>
                                <option value="INACTIVE" ${hall != null && (hall.status == 'INACTIVE' || hall.status == 'MAINTENANCE') ? 'selected' : ''}>
                                    Tạm ngưng / Bảo trì (INACTIVE)
                                </option>
                            </select>
                        </div>
                    </div>

                    <div class="cf-form-actions">
                        <button type="submit" class="cf-btn cf-btn-primary">
                            ${hall != null ? 'Cập nhật thay đổi' : 'Lưu Phòng chiếu'}
                        </button>
                        <a href="${baseUrl}" class="cf-btn cf-btn-secondary">Hủy bỏ</a>
                    </div>
                </form>
            </div>
        </main>
    </div>
</div>
</body>
</html>
