<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page import="model.entity.User, model.entity.Centre, java.util.List" %>
<%
    request.setAttribute("activeNav", "managers");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${manager != null ? 'Chỉnh sửa thông tin Quản lý' : 'Tạo tài khoản Quản lý mới'} | CineFlow Admin</title>

    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/reset.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/variables.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/buttons.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/badges.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/forms.css">
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
            color: #475569;
            font-weight: 500;
        }
        html[data-theme="dark"] .info-label {
            color: #94A3B8;
        }
        .info-code {
            background: #E2E8F0;
            color: #0F172A !important;
            -webkit-text-fill-color: #0F172A !important;
            padding: 3px 10px;
            border-radius: 6px;
            font-family: monospace;
            font-size: 13px;
            font-weight: 700;
            border: 1px solid #CBD5E1;
        }
        html[data-theme="dark"] .info-code {
            background: #1E293B;
            color: #F8FAFC !important;
            -webkit-text-fill-color: #F8FAFC !important;
            border-color: #334155;
        }
        .info-centre {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            background: #EEF2FF;
            color: #3730A3 !important;
            border: 1px solid #C7D2FE;
            padding: 4px 12px;
            border-radius: 6px;
            font-weight: 600;
            font-size: 13px;
        }
        html[data-theme="dark"] .info-centre {
            background: rgba(99, 102, 241, 0.15);
            color: #A5B4FC !important;
            border-color: rgba(99, 102, 241, 0.3);
        }
    </style>
</head>
<body>
<div class="cf-manager-shell">
    <jsp:include page="/WEB-INF/fragments/adminSidebar.jspf" />

    <div class="cf-manager-main">
        <jsp:include page="/WEB-INF/fragments/adminTopbar.jspf" />

        <main class="cf-content">
            <div class="cf-page-header">
                <div>
                    <h1>${manager != null ? 'Chỉnh sửa thông tin Quản lý' : 'Tạo tài khoản Quản lý mới'}</h1>
                    <p>${manager != null ? 'Cập nhật lại thông tin cá nhân của Quản lý chi nhánh' : 'Khởi tạo tài khoản Quản lý và phân công cụm rạp phụ trách ban đầu'}</p>
                </div>
                <a href="${pageContext.request.contextPath}/admin/managers" class="cf-btn cf-btn-secondary" style="display: flex; align-items: center; gap: 6px;">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><line x1="19" y1="12" x2="5" y2="12"></line><polyline points="12 19 5 12 12 5"></polyline></svg>
                    Quay lại danh sách
                </a>
            </div>

            <!-- Thông báo lỗi nếu có -->
            <c:if test="${param.error == 'missing_fields'}">
                <div class="cf-alert cf-alert-danger"><span>⚠</span> <strong>Lỗi:</strong> Vui lòng điền đầy đủ các thông tin bắt buộc.</div>
            </c:if>
            <c:if test="${param.error == 'invalid_name'}">
                <div class="cf-alert cf-alert-danger"><span>⚠</span> <strong>Lỗi:</strong> Họ và tên phải từ 2 đến 100 ký tự.</div>
            </c:if>
            <c:if test="${param.error == 'invalid_account'}">
                <div class="cf-alert cf-alert-danger"><span>⚠</span> <strong>Lỗi:</strong> Tên tài khoản từ 3-30 ký tự, chỉ gồm chữ cái, số, gạch dưới, gạch ngang hoặc dấu chấm.</div>
            </c:if>
            <c:if test="${param.error == 'invalid_email'}">
                <div class="cf-alert cf-alert-danger"><span>⚠</span> <strong>Lỗi:</strong> Địa chỉ email không đúng định dạng chuẩn.</div>
            </c:if>
            <c:if test="${param.error == 'disposable_email'}">
                <div class="cf-alert cf-alert-danger"><span>⚠</span> <strong>Lỗi:</strong> Vui lòng không sử dụng địa chỉ email rác / email tạm thời.</div>
            </c:if>
            <c:if test="${param.error == 'weak_password'}">
                <div class="cf-alert cf-alert-danger"><span>⚠</span> <strong>Lỗi:</strong> Mật khẩu yếu: Yêu cầu tối thiểu 8 ký tự, gồm ít nhất 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt.</div>
            </c:if>
            <c:if test="${param.error == 'invalid_phone'}">
                <div class="cf-alert cf-alert-danger"><span>⚠</span> <strong>Lỗi:</strong> Số điện thoại không hợp lệ (yêu cầu số điện thoại di động Việt Nam 10 số).</div>
            </c:if>
            <c:if test="${param.error == 'invalid_centre'}">
                <div class="cf-alert cf-alert-danger"><span>⚠</span> <strong>Lỗi:</strong> Cụm rạp được chọn không hợp lệ.</div>
            </c:if>
            <c:if test="${param.error == 'centre_has_manager'}">
                <div class="cf-alert cf-alert-danger"><span>⚠</span> <strong>Lỗi:</strong> Cụm rạp được chọn hiện đã có Quản lý đang phụ trách (mỗi rạp chỉ có 1 Quản lý).</div>
            </c:if>
            <c:if test="${param.error == 'account_exists'}">
                <div class="cf-alert cf-alert-danger"><span>⚠</span> <strong>Lỗi:</strong> Tên tài khoản (Account) đã tồn tại trong hệ thống.</div>
            </c:if>
            <c:if test="${param.error == 'email_exists'}">
                <div class="cf-alert cf-alert-danger"><span>⚠</span> <strong>Lỗi:</strong> Địa chỉ Email đã được sử dụng bởi người dùng khác.</div>
            </c:if>
            <c:if test="${param.error == 'not_manager'}">
                <div class="cf-alert cf-alert-danger"><span>⚠</span> <strong>Lỗi:</strong> Tài khoản không tồn tại hoặc không phải là Quản lý.</div>
            </c:if>
            <c:if test="${param.error == 'invalid_data' || param.error == 'create_failed' || param.error == 'update_failed'}">
                <div class="cf-alert cf-alert-danger"><span>⚠</span> <strong>Lỗi:</strong> Thao tác thất bại. Vui lòng kiểm tra lại thông tin.</div>
            </c:if>

            <div class="cf-form-card">
                <form method="POST" action="${pageContext.request.contextPath}/admin/managers">
                    <input type="hidden" name="action" value="save">
                    <c:if test="${manager != null}">
                        <input type="hidden" name="userId" value="${manager.userId}">
                    </c:if>

                    <div style="display: flex; flex-direction: column; gap: 18px;">
                        <c:choose>
                            <%-- CHẾ ĐỘ CHỈNH SỬA --%>
                            <c:when test="${manager != null}">
                                <div class="info-banner">
                                    <div class="info-item">
                                        <span class="info-label">Mã Quản lý:</span>
                                        <code class="cf-badge cf-badge-primary">${manager.employeeCode}</code>
                                    </div>
                                    <div class="info-item">
                                        <span class="info-label">Tên đăng nhập:</span>
                                        <span class="info-code">${manager.account}</span>
                                    </div>
                                    <div class="info-item">
                                        <span class="info-label">Cụm rạp:</span>
                                        <span class="info-centre">🏛 ${manager.centreName != null ? manager.centreName : 'Chưa phân công'}</span>
                                    </div>
                                </div>

                                <div class="cf-form-group">
                                    <label>Họ và tên Quản lý *</label>
                                    <input type="text" name="fullName" value="${manager.fullName}" minlength="2" maxlength="100" required>
                                </div>

                                <div class="cf-form-grid-2">
                                    <div class="cf-form-group">
                                        <label>Số điện thoại</label>
                                        <input type="tel" name="phone" value="${manager.phone != null ? manager.phone : ''}" placeholder="09xxxxxxxx">
                                    </div>
                                    <div class="cf-form-group">
                                        <label>Email liên hệ *</label>
                                        <input type="email" name="email" value="${manager.email}" required>
                                    </div>
                                </div>
                            </c:when>

                            <%-- CHẾ ĐỘ TẠO MỚI --%>
                            <c:otherwise>
                                <div class="cf-form-group">
                                    <label>Cụm rạp phụ trách *</label>
                                    <select name="centreId" required class="modal-select-field">
                                        <c:choose>
                                            <c:when test="${empty centres}">
                                                <option value="">-- Hiện không có cụm rạp nào trống Quản lý --</option>
                                            </c:when>
                                            <c:otherwise>
                                                <option value="">-- Chọn cụm rạp phân công (chưa có Quản lý) --</option>
                                                <c:forEach var="c" items="${centres}">
                                                    <option value="${c.centreId}">${c.centreName} (${c.city})</option>
                                                </c:forEach>
                                            </c:otherwise>
                                        </c:choose>
                                    </select>
                                    <c:choose>
                                        <c:when test="${empty centres}">
                                            <span class="cf-form-hint" style="color: #ef4444;">⚠ Tất cả các cụm rạp hiện đã có Quản lý phụ trách. Mỗi rạp chỉ được phân công 1 Quản lý.</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="cf-form-hint">Hệ thống chỉ liệt kê các cụm rạp đang TRỐNG (chưa có Quản lý) để bảo đảm quy tắc 1 rạp - 1 Quản lý.</span>
                                        </c:otherwise>
                                    </c:choose>
                                </div>

                                <div class="cf-form-group">
                                    <label>Họ và tên Quản lý *</label>
                                    <input type="text" name="fullName" placeholder="Ví dụ: Hoàng Tuấn Anh" minlength="2" maxlength="100" required>
                                </div>

                                <div class="cf-form-grid-2">
                                    <div class="cf-form-group">
                                        <label>Tên đăng nhập (Account) *</label>
                                        <input type="text" name="account" placeholder="mgr_tuananh" minlength="3" maxlength="30" required>
                                    </div>
                                    <div class="cf-form-group">
                                        <label>Số điện thoại</label>
                                        <input type="tel" name="phone" placeholder="09xxxxxxxx">
                                    </div>
                                </div>

                                <div class="cf-form-group">
                                    <label>Email *</label>
                                    <input type="email" name="email" placeholder="manager.ta@cineflow.com" required>
                                </div>

                                <div class="cf-form-group">
                                    <label>Mật khẩu khởi tạo *</label>
                                    <input type="password" name="password" value="Manager@123" required>
                                    <span class="cf-form-hint">Mặc định: Manager@123 (tối thiểu 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt)</span>
                                </div>
                            </c:otherwise>
                        </c:choose>

                        <div class="cf-form-actions">
                            <button type="submit" class="cf-btn cf-btn-primary" ${manager == null && empty centres ? 'disabled' : ''}>
                                ${manager != null ? 'Lưu thay đổi' : 'Tạo tài khoản Manager'}
                            </button>
                            <a href="${pageContext.request.contextPath}/admin/managers" class="cf-btn cf-btn-secondary">Hủy</a>
                        </div>
                    </div>
                </form>
            </div>
        </main>
    </div>
</div>
</body>
</html>
