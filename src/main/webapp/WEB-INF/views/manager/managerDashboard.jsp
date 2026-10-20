<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && session.getAttribute("user") instanceof model.entity.User) {
        authRole = ((model.entity.User) session.getAttribute("user")).getRole();
    }
    if (!"MANAGER".equalsIgnoreCase(authRole) && !"ADMIN".equalsIgnoreCase(authRole)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Quản lý (MANAGER) mới có quyền truy cập trang này.");
        return;
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Bảng điều khiển quản lý | CineFlow</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/shell.css">
</head>
<body>
<div class="cf-manager-shell">
    <jsp:include page="/WEB-INF/fragments/managerSidebar.jspf"/>
    <div class="cf-manager-main">
        <jsp:include page="/WEB-INF/fragments/managerTopbar.jspf"/>
        <main class="cf-content">
            <div class="cf-page-header">
                <div>
                    <h1>Tổng quan Quản lý Cụm rạp</h1>
                    <p>Bảng điều phối vận hành, lịch chiếu, giá vé và nhân sự cụm rạp.</p>
                </div>
            </div>
            <div class="cf-card">
                <div class="cf-card-header">
                    <div>
                        <h2>Chức năng quản trị cụm rạp</h2>
                    </div>
                </div>
                <div class="cf-card-body">
                    <div class="cf-dashboard-links">
                        <a class="cf-dashboard-link" href="${pageContext.request.contextPath}/showtimes">
                            <strong>🕒 Lịch chiếu phim</strong>
                            <span>Tạo, quản lý suất chiếu và ma trận ghế của phòng chiếu.</span>
                        </a>
                        <a class="cf-dashboard-link" href="${pageContext.request.contextPath}/pricing">
                            <strong>🏷 Bảng giá &amp; Phụ thu</strong>
                            <span>Cấu hình giá vé cơ bản, khung giờ và phụ thu phòng/ghế.</span>
                        </a>
                        <a class="cf-dashboard-link" href="${pageContext.request.contextPath}/manager/halls">
                            <strong>🎬 Quản lý phòng chiếu</strong>
                            <span>Danh sách phòng, thiết lập sơ đồ ghế và trạng thái khả dụng.</span>
                        </a>
                        <a class="cf-dashboard-link" href="${pageContext.request.contextPath}/fnb">
                            <strong>🍿 Bắp nước &amp; Combo</strong>
                            <span>Thực đơn món, gói combo ưu đãi và quản lý kho hàng.</span>
                        </a>
                        <a class="cf-dashboard-link" href="${pageContext.request.contextPath}/staff">
                            <strong>♙ Nhân sự cụm rạp</strong>
                            <span>Quản lý tài khoản nhân viên bán vé và soát vé trực thuộc.</span>
                        </a>
                        <a class="cf-dashboard-link" href="${pageContext.request.contextPath}/reports">
                            <strong>▤ Báo cáo vận hành</strong>
                            <span>Xem doanh thu chi tiết, tỷ lệ lấp đầy phòng và hiệu suất.</span>
                        </a>
                    </div>
                </div>
            </div>
        </main>
    </div>
</div>
</body>
</html>
