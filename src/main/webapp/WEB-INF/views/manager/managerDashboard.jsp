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
                    <h1>Tổng quan Manager</h1>
                </div>
            </div>
            <div class="cf-card">
                <div class="cf-card-header">
                    <div>
                        <h2>Chức năng quản lý</h2>
                    </div>
                </div>
                <div class="cf-card-body">
                    <div class="cf-dashboard-links">
                        <a class="cf-dashboard-link" href="${pageContext.request.contextPath}/staff">
                            <strong>Nhân sự trung tâm</strong>
                            <span>Quản lý tài khoản nhân viên theo phạm vi được cấp.</span>
                        </a>
                        <a class="cf-dashboard-link" href="${pageContext.request.contextPath}/reports">
                            <strong>Báo cáo hoạt động</strong>
                            <span>Xem doanh thu, tỷ lệ lấp đầy và phim bán chạy.</span>
                        </a>
                    </div>
                </div>
            </div>
        </main>
    </div>
</div>
</body>
</html>
