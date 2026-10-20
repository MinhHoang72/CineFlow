<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Lịch sử soát vé | CineFlow Staff</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/dashboard.css">
</head>
<body>
<div class="cf-manager-shell">
    <jsp:include page="/WEB-INF/fragments/managerSidebar.jspf" />
    <div class="cf-manager-main">
        <jsp:include page="/WEB-INF/fragments/adminTopbar.jspf" />
        <main class="cf-content">
            <header class="cf-page-header">
                <div>
                    <span class="cf-eyebrow">SOÁT VÉ ĐIỆN TỬ</span>
                    <h1>Lịch sử soát vé trong ca trực</h1>
                </div>
                <a class="cf-btn cf-btn-primary" href="${pageContext.request.contextPath}/staff/checkin">Quét vé mới</a>
            </header>
            <div class="cf-card">
                <table class="cf-table">
                    <thead>
                        <tr>
                            <th>Mã vé</th>
                            <th>Suất chiếu</th>
                            <th>Phòng</th>
                            <th>Ghế</th>
                            <th>Thời gian quét</th>
                            <th>Trạng thái</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="item" items="${checkInLogs}">
                            <tr>
                                <td><strong>#${item.ticketId}</strong></td>
                                <td><c:out value="${item.showtimeInfo}" /></td>
                                <td><c:out value="${item.hallName}" /></td>
                                <td><c:out value="${item.seatRow}${item.seatNumber}" /></td>
                                <td><c:out value="${item.checkInTime}" /></td>
                                <td><span class="badge bg-success">HỢP LỆ</span></td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty checkInLogs}">
                            <tr>
                                <td colspan="6" class="text-center py-4 text-muted">Chưa có lượt soát vé nào trong ca làm việc này.</td>
                            </tr>
                        </c:if>
                    </tbody>
                </table>
            </div>
        </main>
    </div>
</div>
</body>
</html>
