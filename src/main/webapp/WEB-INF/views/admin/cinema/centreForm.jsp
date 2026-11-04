<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${centre != null ? 'Sửa thông tin Cụm rạp' : 'Thêm Cụm rạp mới'} | CineFlow Admin</title>

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
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/centres.css">

    <style>
        .cf-form-card {
            background: var(--cf-surface, #161B26);
            border: 1px solid var(--cf-border, rgba(255, 255, 255, 0.08));
            border-radius: 14px;
            padding: 28px;
            max-width: 720px;
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
    <jsp:include page="/WEB-INF/fragments/adminSidebar.jspf" />

    <div class="cf-manager-main">
        <!-- Thanh tiêu đề trên (Topbar) -->
        <jsp:include page="/WEB-INF/fragments/adminTopbar.jspf" />

        <main class="cf-content">
            <!-- Header trang -->
            <div class="cf-page-header">
                <div>
                    <h1>${centre != null ? 'Sửa thông tin Cụm rạp' : 'Thêm Cụm rạp mới'}</h1>
                    <p>${centre != null ? 'Cập nhật lại thông tin liên hệ và địa chỉ của cụm rạp' : 'Tạo mới một cụm rạp vào hệ thống chuỗi rạp CineFlow'}</p>
                </div>
                <a href="${baseUrl}" class="cf-btn cf-btn-secondary" style="display: flex; align-items: center; gap: 6px;">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><line x1="19" y1="12" x2="5" y2="12"></line><polyline points="12 19 5 12 12 5"></polyline></svg>
                    Quay lại danh sách
                </a>
            </div>

            <!-- Thông báo lỗi nếu có -->
            <c:if test="${param.error == 'duplicate_name'}">
                <div class="cf-alert cf-alert-danger" style="margin-bottom: 20px;">
                    <span>⚠</span> <strong>Lỗi:</strong> Tên cụm rạp đã tồn tại trong hệ thống. Vui lòng chọn tên khác!
                </div>
            </c:if>
            <c:if test="${param.error == 'missing_fields'}">
                <div class="cf-alert cf-alert-danger" style="margin-bottom: 20px;">
                    <span>⚠</span> <strong>Lỗi:</strong> Vui lòng điền đầy đủ các thông tin bắt buộc (Tên cụm rạp, Địa chỉ, Hotline).
                </div>
            </c:if>
            <c:if test="${param.error == 'failed'}">
                <div class="cf-alert cf-alert-danger" style="margin-bottom: 20px;">
                    <span>⚠</span> <strong>Lỗi:</strong> Không thể lưu thông tin. Vui lòng thử lại sau!
                </div>
            </c:if>

            <!-- Khung Form dùng chung -->
            <div class="cf-form-card">
                <form method="POST" action="${baseUrl}">
                    <!-- Nếu có centre (sửa) thì action=update, ngược lại action=create -->
                    <input type="hidden" name="action" value="${centre != null ? 'update' : 'create'}">
                    <c:if test="${centre != null}">
                        <input type="hidden" name="centreId" value="${centre.centreId}">
                    </c:if>

                    <div style="display: flex; flex-direction: column; gap: 18px;">
                        <div class="cf-form-group">
                            <label>Tên Cụm rạp *</label>
                            <input type="text" name="centreName" value="${centre != null ? centre.centreName : ''}" 
                                   placeholder="Ví dụ: CineFlow Cầu Giấy" required>
                            <span class="cf-form-hint">Hệ thống sẽ kiểm tra đảm bảo không trùng tên với cụm rạp khác.</span>
                        </div>

                        <div class="cf-form-group">
                            <label>Địa chỉ chi tiết *</label>
                            <input type="text" name="address" value="${centre != null ? centre.address : ''}" 
                                   placeholder="Ví dụ: 123 Cầu Giấy, Quan Hoa" required>
                        </div>

                        <div class="cf-form-group">
                            <label>Hotline / Điện thoại liên hệ *</label>
                            <input type="text" name="phone" value="${centre != null ? centre.phone : ''}" 
                                   placeholder="Ví dụ: 02411111111" required>
                        </div>

                        <div class="cf-form-group">
                            <label>Thành phố / Tỉnh thành</label>
                            <input type="text" name="city" value="${centre != null ? centre.city : ''}" 
                                   placeholder="Ví dụ: Hà Nội, TP. Hồ Chí Minh, Đà Nẵng...">
                        </div>
                    </div>

                    <div class="cf-form-actions">
                        <button type="submit" class="cf-btn cf-btn-primary">
                            ${centre != null ? 'Cập nhật thay đổi' : 'Lưu Cụm rạp'}
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
