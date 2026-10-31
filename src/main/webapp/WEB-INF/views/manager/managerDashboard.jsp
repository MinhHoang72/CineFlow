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
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Dashboard Manager | CineFlow</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/dashboard.css">
</head>
<body>
<div class="admin-layout cf-manager-shell">
    <jsp:include page="/WEB-INF/fragments/managerSidebar.jspf" />
    <div class="admin-main cf-manager-main">
        <jsp:include page="/WEB-INF/fragments/managerTopbar.jspf" />
        <main class="admin-content cf-content manager-dashboard">
            <section class="dashboard-welcome-banner" aria-labelledby="manager-welcome">
                <div class="banner-content">
                    <div class="banner-badge">ĐIỀU HÀNH CỤM RẠP</div>
                    <h1 id="manager-welcome">Xin chào, <c:out value="${sessionScope.user.fullName}" default="Quản lý" />!</h1>
                    <p>Theo dõi lịch chiếu, phòng, giá vé và các hoạt động tại cụm rạp của anh/chị.</p>
                    <div class="banner-actions">
                        <a href="${pageContext.request.contextPath}/showtimes" class="cf-btn cf-btn-primary">Quản lý suất chiếu</a>
                        <a href="${pageContext.request.contextPath}/pricing" class="cf-btn cf-btn-ghost">Giá vé &amp; Phụ thu</a>
                        <a href="${pageContext.request.contextPath}/report/sales" class="cf-btn cf-btn-ghost">Báo cáo doanh thu</a>
                    </div>
                </div>
                <div class="banner-stat-bubble">
                    <small>CỤM RẠP ĐANG QUẢN LÝ</small>
                    <strong><c:out value="${managedCentre.centreName}" default="Chưa phân công" /></strong>
                    <c:if test="${not empty managedCentre}"><p><c:out value="${managedCentre.city}" /></p></c:if>
                </div>
            </section>
            <c:choose>
                <c:when test="${not empty dashboardError}"><div class="cf-alert cf-alert-warning" role="alert"><c:out value="${dashboardError}" /></div></c:when>
                <c:otherwise>
                    <section class="kpi-grid" aria-label="Chỉ số cụm rạp">
                        <div class="kpi-card sky"><div class="kpi-card-header"><span class="kpi-title">Tổng phòng chiếu</span></div><div class="kpi-value"><c:out value="${totalHalls}" /></div><p class="kpi-subtext">Phòng thuộc cụm rạp đang quản lý</p></div>
                        <div class="kpi-card emerald"><div class="kpi-card-header"><span class="kpi-title">Phòng sẵn sàng</span></div><div class="kpi-value"><c:out value="${availableHalls}" /></div><p class="kpi-subtext">Phòng có trạng thái khả dụng</p></div>
                        <div class="kpi-card ruby"><div class="kpi-card-header"><span class="kpi-title">Suất chiếu hôm nay</span></div><div class="kpi-value"><c:out value="${todayShowtimes}" /></div><p class="kpi-subtext">Không bao gồm suất đã hủy</p></div>
                        <div class="kpi-card amber"><div class="kpi-card-header"><span class="kpi-title">Tổng sức chứa</span></div><div class="kpi-value"><c:out value="${totalCapacity}" /></div><p class="kpi-subtext">Theo cấu hình các phòng chiếu</p></div>
                    </section>
                </c:otherwise>
            </c:choose>
            <section class="admin-card">
                <div class="admin-card-header"><div><h2>Truy cập nhanh</h2><p>Các chức năng quản lý và vận hành cụm rạp.</p></div></div>
                <div class="admin-card-body"><div class="cf-dashboard-links">
                    <a class="cf-dashboard-link" href="${pageContext.request.contextPath}/showtimes"><strong>Suất chiếu<span aria-hidden="true">↗</span></strong><span>Tạo và điều chỉnh lịch chiếu, xem tình trạng ghế.</span></a>
                    <a class="cf-dashboard-link" href="${pageContext.request.contextPath}/pricing"><strong>Giá vé &amp; Phụ thu<span aria-hidden="true">↗</span></strong><span>Cấu hình giá cơ bản, thời điểm áp dụng và phụ thu.</span></a>
                    <a class="cf-dashboard-link" href="${pageContext.request.contextPath}/manager/halls"><strong>Phòng chiếu &amp; Ghế<span aria-hidden="true">↗</span></strong><span>Quản lý phòng, bố trí ghế và bảo trì.</span></a>
                    <a class="cf-dashboard-link" href="${pageContext.request.contextPath}/staff"><strong>Nhân sự cụm rạp<span aria-hidden="true">↗</span></strong><span>Theo dõi nhân viên bán vé và soát vé.</span></a>
                    <a class="cf-dashboard-link" href="${pageContext.request.contextPath}/fnb"><strong>Bắp nước &amp; Combo<span aria-hidden="true">↗</span></strong><span>Xem thực đơn và các combo tại rạp.</span></a>
                    <a class="cf-dashboard-link" href="${pageContext.request.contextPath}/fnb/stock"><strong>Kho bắp nước<span aria-hidden="true">↗</span></strong><span>Theo dõi số lượng và tình trạng hàng hóa.</span></a>
                    <a class="cf-dashboard-link" href="${pageContext.request.contextPath}/manager/vouchers"><strong>Voucher<span aria-hidden="true">↗</span></strong><span>Quản lý chương trình giảm giá của cụm rạp.</span></a>
                    <a class="cf-dashboard-link" href="${pageContext.request.contextPath}/report/operations"><strong>Chỉ số vận hành<span aria-hidden="true">↗</span></strong><span>Theo dõi lịch chiếu và hiệu suất phòng.</span></a>
                    <a class="cf-dashboard-link" href="${pageContext.request.contextPath}/staff/payments"><strong>Theo dõi giao dịch<span aria-hidden="true">↗</span></strong><span>Tra cứu trạng thái các giao dịch thanh toán.</span></a>
                </div></div>
            </section>
        </main>
    </div>
</div>
</body>
</html>
