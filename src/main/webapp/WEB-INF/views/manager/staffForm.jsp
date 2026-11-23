<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page import="model.entity.User, model.entity.Centre" %>
<%
    request.setAttribute("activeNav", "staff");
    request.setAttribute("pageTitle", "Quản lý nhân sự");
    request.setAttribute("pageBreadcrumb", "Nhân sự");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${staff != null ? 'Chỉnh sửa thông tin Nhân viên' : 'Thêm nhân viên mới'} | CineFlow</title>

    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/reset.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/variables.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/buttons.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/badges.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/forms.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/shell.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/staff.css">

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
        .cf-form-grid-2 {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 16px;
        }
        @media (max-width: 640px) {
            .cf-form-grid-2 {
                grid-template-columns: 1fr;
            }
        }
        .info-banner {
            display: flex;
            flex-wrap: wrap;
            align-items: center;
            gap: 16px 28px;
            background: #F1F5F9;
            border: 1px solid #CBD5E1;
            border-radius: 10px;
            padding: 14px 18px;
            margin-bottom: 24px;
        }
        html[data-theme="dark"] .info-banner {
            background: rgba(255, 255, 255, 0.05);
            border-color: rgba(255, 255, 255, 0.12);
        }
        .info-item {
            display: flex;
            align-items: center;
            gap: 8px;
            font-size: 14px;
        }
        .info-label {
            color: #0F172A !important;
            font-weight: 700;
        }
        html[data-theme="dark"] .info-label {
            color: #CBD5E1 !important;
        }
        .info-code {
            background: #E2E8F0;
            color: #0F172A !important;
            -webkit-text-fill-color: #0F172A !important;
            padding: 4px 10px;
            border-radius: 6px;
            font-family: monospace;
            font-size: 13.5px;
            font-weight: 700;
            border: 1px solid #94A3B8;
        }
        html[data-theme="dark"] .info-code {
            background: #1E293B;
            color: #F8FAFC !important;
            -webkit-text-fill-color: #F8FAFC !important;
            border-color: #475569;
        }
        .centre-tag {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            background: #E0E7FF;
            color: #1E1B4B !important;
            border: 1px solid #818CF8;
            padding: 4px 12px;
            border-radius: 6px;
            font-weight: 700;
            font-size: 13px;
        }
        html[data-theme="dark"] .centre-tag {
            background: rgba(99, 102, 241, 0.2);
            color: #E0E7FF !important;
            border-color: rgba(99, 102, 241, 0.4);
        }
    </style>
</head>
<body>
<div class="cf-manager-shell">
    <jsp:include page="/WEB-INF/fragments/managerSidebar.jspf"/>

    <div class="cf-manager-main">
        <jsp:include page="/WEB-INF/fragments/managerTopbar.jspf"/>

        <main class="cf-content">
            <div class="cf-page-header">
                <div>
                    <h1>${staff != null ? 'Chỉnh sửa thông tin Nhân viên' : 'Thêm nhân viên mới'}</h1>
                    <p>
                        Trung tâm trực thuộc:
                        <span class="centre-tag">🏛 ${assignedCentre != null ? assignedCentre.centreName : 'Trung tâm'}</span>
                    </p>
                </div>
                <a href="${pageContext.request.contextPath}/staffList" class="cf-btn cf-btn-secondary" style="display: flex; align-items: center; gap: 6px;">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><line x1="19" y1="12" x2="5" y2="12"></line><polyline points="12 19 5 12 12 5"></polyline></svg>
                    Quay lại danh sách
                </a>
            </div>

            <!-- THÔNG BÁO LỖI -->
            <c:if test="${param.error == 'missing_fields'}">
                <div class="cf-alert cf-alert-danger"><span>⚠</span> <strong>Lỗi:</strong> Vui lòng điền đầy đủ các thông tin bắt buộc.</div>
            </c:if>
            <c:if test="${param.error == 'invalid_name'}">
                <div class="cf-alert cf-alert-danger"><span>⚠</span> <strong>Lỗi:</strong> Họ và tên phải từ 2 đến 100 ký tự.</div>
            </c:if>
            <c:if test="${param.error == 'invalid_account'}">
                <div class="cf-alert cf-alert-danger"><span>⚠</span> <strong>Lỗi:</strong> Tên tài khoản không hợp lệ (tối đa 50 ký tự).</div>
            </c:if>
            <c:if test="${param.error == 'invalid_email'}">
                <div class="cf-alert cf-alert-danger"><span>⚠</span> <strong>Lỗi:</strong> Email không đúng định dạng chuẩn.</div>
            </c:if>
            <c:if test="${param.error == 'weak_password'}">
                <div class="cf-alert cf-alert-danger"><span>⚠</span> <strong>Lỗi:</strong> Mật khẩu tối thiểu 8 ký tự, gồm ít nhất 1 chữ hoa và 1 ký tự đặc biệt.</div>
            </c:if>
            <c:if test="${param.error == 'invalid_phone'}">
                <div class="cf-alert cf-alert-danger"><span>⚠</span> <strong>Lỗi:</strong> Số điện thoại không hợp lệ (tối đa 20 ký tự).</div>
            </c:if>
            <c:if test="${param.error == 'account_exists'}">
                <div class="cf-alert cf-alert-danger"><span>⚠</span> <strong>Lỗi:</strong> Tên tài khoản đã tồn tại trong hệ thống.</div>
            </c:if>
            <c:if test="${param.error == 'email_exists'}">
                <div class="cf-alert cf-alert-danger"><span>⚠</span> <strong>Lỗi:</strong> Email đã được sử dụng bởi tài khoản khác.</div>
            </c:if>
            <c:if test="${param.error == 'forbidden'}">
                <div class="cf-alert cf-alert-danger"><span>⚠</span> <strong>Lỗi:</strong> Bạn không có quyền thao tác trên nhân sự này.</div>
            </c:if>
            <c:if test="${param.error == 'not_found'}">
                <div class="cf-alert cf-alert-danger"><span>⚠</span> <strong>Lỗi:</strong> Không tìm thấy thông tin nhân viên.</div>
            </c:if>
            <c:if test="${param.error == 'unassigned_manager'}">
                <div class="cf-alert cf-alert-danger"><span>⚠</span> <strong>Lỗi:</strong> Tài khoản Manager của bạn chưa được phân công trung tâm.</div>
            </c:if>
            <c:if test="${param.error == 'create_failed' || param.error == 'update_failed' || param.error == 'invalid_data'}">
                <div class="cf-alert cf-alert-danger"><span>⚠</span> <strong>Lỗi:</strong> Thao tác thất bại. Vui lòng kiểm tra lại thông tin.</div>
            </c:if>

            <div class="cf-form-card">
                <form method="POST" action="${pageContext.request.contextPath}/staffList">
                    <input type="hidden" name="action" value="save">
                    <c:if test="${staff != null}">
                        <input type="hidden" name="userId" value="${staff.userId}">
                    </c:if>

                    <div style="display: flex; flex-direction: column; gap: 18px;">
                        <c:choose>
                            <%-- CHẾ ĐỘ SỬA --%>
                            <c:when test="${staff != null}">
                                <div class="info-banner">
                                    <div class="info-item">
                                        <span class="info-label">Mã nhân viên:</span>
                                        <code class="cf-badge cf-badge-primary">${staff.employeeCode}</code>
                                    </div>
                                    <div class="info-item">
                                        <span class="info-label">Tên đăng nhập:</span>
                                        <span class="info-code">${staff.account}</span>
                                    </div>
                                    <div class="info-item">
                                        <span class="info-label">Cụm rạp:</span>
                                        <span class="centre-tag">🏛 ${assignedCentre != null ? assignedCentre.centreName : 'Chưa phân công'}</span>
                                    </div>
                                </div>

                                <div class="cf-form-group">
                                    <label>Họ và tên nhân viên *</label>
                                    <input type="text" name="fullName" value="${staff.fullName}" minlength="2" maxlength="100" required>
                                </div>

                                <div class="cf-form-grid-2">
                                    <div class="cf-form-group">
                                        <label>Số điện thoại</label>
                                        <input type="tel" name="phone" value="${staff.phone != null ? staff.phone : ''}" placeholder="09xxxxxxxx">
                                    </div>
                                    <div class="cf-form-group">
                                        <label>Email liên hệ *</label>
                                        <input type="email" name="email" value="${staff.email}" required>
                                    </div>
                                </div>

                                <div class="cf-form-group">
                                    <label>Vị trí / Vai trò *</label>
                                    <select name="role" required>
                                        <option value="STAFF" ${staff.role == 'STAFF' ? 'selected' : ''}>Nhân viên thông thường</option>
                                        <option value="TICKET_SALES_STAFF" ${staff.role == 'TICKET_SALES_STAFF' ? 'selected' : ''}>Nhân viên bán vé</option>
                                    </select>
                                </div>
                            </c:when>

                            <%-- CHẾ ĐỘ TẠO MỚI --%>
                            <c:otherwise>
                                <div class="cf-form-group">
                                    <label>Họ và tên nhân viên *</label>
                                    <input type="text" name="fullName" placeholder="Ví dụ: Nguyễn Văn An" minlength="2" maxlength="100" required>
                                </div>

                                <div class="cf-form-grid-2">
                                    <div class="cf-form-group">
                                        <label>Tên đăng nhập (Account) *</label>
                                        <input type="text" name="account" placeholder="staff_an" minlength="3" maxlength="50" required>
                                    </div>
                                    <div class="cf-form-group">
                                        <label>Số điện thoại</label>
                                        <input type="tel" name="phone" placeholder="09xxxxxxxx">
                                    </div>
                                </div>

                                <div class="cf-form-group">
                                    <label>Email *</label>
                                    <input type="email" name="email" placeholder="staff.an@cineflow.com" required>
                                </div>

                                <div class="cf-form-group">
                                    <label>Mật khẩu khởi tạo *</label>
                                    <input type="password" name="password" value="Staff@123" required>
                                    <span class="cf-form-hint">Mặc định: Staff@123 (tối thiểu 8 ký tự, có chữ hoa và ký tự đặc biệt)</span>
                                </div>

                                <div class="cf-form-group">
                                    <label>Vị trí / Vai trò *</label>
                                    <select name="role" required>
                                        <option value="STAFF">Nhân viên thông thường</option>
                                        <option value="TICKET_SALES_STAFF">Nhân viên bán vé</option>
                                    </select>
                                </div>
                            </c:otherwise>
                        </c:choose>

                        <div class="cf-form-actions">
                            <button type="submit" class="cf-btn cf-btn-primary">
                                ${staff != null ? 'Lưu thay đổi' : 'Tạo tài khoản nhân viên'}
                            </button>
                            <a href="${pageContext.request.contextPath}/staffList" class="cf-btn cf-btn-secondary">Hủy</a>
                        </div>
                    </div>
                </form>
            </div>
        </main>
    </div>
</div>
</body>
</html>
