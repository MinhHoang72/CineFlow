<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%
    String showtimeRole = (String) session.getAttribute("role");
    if (showtimeRole == null && session.getAttribute("user") instanceof model.entity.User) {
        showtimeRole = ((model.entity.User) session.getAttribute("user")).getRole();
    }
    boolean showtimeAdmin = "ADMIN".equalsIgnoreCase(showtimeRole);
    request.setAttribute("activeNav", "showtimes");
    request.setAttribute("activeTab", "showtimes");
    request.setAttribute("pageTitle", "Quản lý suất chiếu");
    request.setAttribute("pageBreadcrumb", "Lịch chiếu");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Quản lý suất chiếu | CineFlow</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/dashboard.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/showtime-pricing/showtimes.css">
    <script src="${pageContext.request.contextPath}/js/showtimes.js" defer></script>
</head>
<body>
<div class="cf-manager-shell">
    <jsp:include page='<%= showtimeAdmin ? "/WEB-INF/views/admin/adminSidebar.jspf" : "/WEB-INF/views/manager/managerSidebar.jspf" %>' />
    <div class="cf-manager-main">
        <jsp:include page="/WEB-INF/views/admin/adminTopbar.jspf" />
        <main class="cf-content st-page">
            <header class="st-heading">
                <div><span class="st-eyebrow">ĐIỀU HÀNH RẠP CHIẾU</span><h1>Quản lý suất chiếu</h1><p>Sắp xếp lịch chiếu, theo dõi phòng và quản lý từng suất phim.</p></div>
                <a class="st-button st-button-primary" href="#create-showtime">＋ Tạo suất chiếu</a>
            </header>
            <c:if test="${not empty sessionScope.message}"><div class="st-notice st-success" role="status"><c:out value="${sessionScope.message}" /></div><c:remove var="message" scope="session" /></c:if>
            <c:if test="${not empty sessionScope.error}"><div class="st-notice st-error" role="alert"><c:out value="${sessionScope.error}" /></div><c:remove var="error" scope="session" /></c:if>
            <%@ include file="/WEB-INF/fragments/cinema/navigation.jspf" %>
            <div class="st-stats" aria-label="Tổng quan lịch chiếu">
                <div><span>Tổng suất chiếu</span><strong>${fn:length(showtimes)}</strong><small>Trong danh sách của chi nhánh</small></div>
                <div><span>Phòng sẵn sàng</span><strong>${fn:length(halls)}</strong><small>Có thể sắp xếp lịch chiếu</small></div>
                <div><span>Phim có thể lên lịch</span><strong>${fn:length(movies)}</strong><small>Được phân phối tại chi nhánh</small></div>
            </div>
            <section class="st-panel" id="create-showtime" aria-labelledby="create-title">
                <div class="st-panel-heading"><div><h2 id="create-title">Tạo suất chiếu mới</h2><p>Chọn phim, phòng chiếu và thời gian bắt đầu.</p></div><span class="st-step">01 / LÊN LỊCH</span></div>
                <form action="${pageContext.request.contextPath}/showtimes/create" method="post" class="st-create-form">
                    <div class="st-fields">
                        <div class="st-field"><label for="movieId">Phim <span>*</span></label><select id="movieId" name="movieId" required><option value="">Chọn phim</option><c:forEach var="m" items="${movies}"><option value="${m.movieId}"><c:out value="${m.title}" /> · ${m.duration} phút</option></c:forEach></select></div>
                        <div class="st-field"><label for="hallId">Phòng chiếu <span>*</span></label><select id="hallId" name="hallId" required><option value="">Chọn phòng chiếu</option><c:forEach var="h" items="${halls}"><option value="${h.hallId}"><c:out value="${h.hallName}" /> · <c:out value="${h.hallType}" /></option></c:forEach></select></div>
                        <div class="st-field"><label for="startTime">Thời gian bắt đầu <span>*</span></label><input id="startTime" name="startTime" type="datetime-local" required></div>
                    </div>
                    <div class="st-form-footer"><p>Thời gian kết thúc được tính tự động theo thời lượng phim.</p><button type="submit" class="st-button st-button-primary" ${empty movies or empty halls ? 'disabled' : ''}>Tạo suất chiếu</button></div>
                    <c:if test="${empty movies or empty halls}"><p class="st-prerequisite">Cần có phim được phân phối và phòng sẵn sàng để tạo suất chiếu.</p></c:if>
                </form>
            </section>
            <section class="st-panel" aria-labelledby="schedule-title">
                <div class="st-panel-heading"><div><h2 id="schedule-title">Danh sách suất chiếu <span class="st-count">${fn:length(showtimes)}</span></h2><p>Theo dõi lịch chiếu và thao tác trực tiếp trên từng suất.</p></div><span class="st-step">02 / QUẢN LÝ</span></div>
                <div class="st-filters" role="search" aria-label="Lọc suất chiếu">
                    <div class="st-field st-search"><label for="st-search">Tìm phim</label><input id="st-search" type="search" placeholder="Nhập tên phim…"></div>
                    <div class="st-field"><label for="st-date">Ngày chiếu</label><input id="st-date" type="date"></div>
                    <div class="st-field"><label for="st-hall">Phòng chiếu</label><select id="st-hall"><option value="">Tất cả phòng</option></select></div>
                    <div class="st-field"><label for="st-status">Trạng thái</label><select id="st-status"><option value="">Tất cả trạng thái</option><option value="SCHEDULED">Đã lên lịch</option><option value="ONGOING">Đang chiếu</option><option value="COMPLETED">Đã hoàn thành</option><option value="CANCELLED">Đã hủy</option></select></div>
                    <button type="button" class="st-button st-button-muted" id="st-reset">Xóa lọc</button>
                </div>
                <div class="st-table-scroll" role="region" aria-label="Bảng lịch chiếu" tabindex="0">
                    <table class="st-table"><thead><tr><th scope="col">Phim / Mã suất</th><th scope="col">Phòng chiếu</th><th scope="col">Bắt đầu</th><th scope="col">Kết thúc</th><th scope="col">Dọn phòng</th><th scope="col">Trạng thái</th><th scope="col">Thao tác</th></tr></thead>
                        <tbody id="st-rows">
                        <c:forEach var="s" items="${showtimes}">
                            <tr data-showtime data-title="<c:out value='${s.movieTitle}' />" data-hall-id="${s.hallId}" data-hall="<c:out value='${s.hallName}' />" data-date="${fn:substring(s.startTimeInputFormatted, 0, 10)}" data-status="<c:out value='${s.status}' />">
                                <td><strong class="st-movie"><c:out value="${s.movieTitle}" /></strong><span class="st-id">#${s.showtimeId}</span></td>
                                <td><span class="st-hall-name"><c:out value="${s.hallName}" /></span><span class="st-format"><c:out value="${s.hallType}" /></span></td>
                                <td class="st-time"><c:out value="${s.startTimeFormatted}" /></td><td class="st-time"><c:out value="${s.endTimeFormatted}" /></td>
                                <td class="st-buffer">${s.cleaningBufferMinutes} phút</td>
                                <td><span class="st-status st-status-${fn:escapeXml(s.status)}"><c:choose><c:when test="${s.status == 'SCHEDULED'}">Đã lên lịch</c:when><c:when test="${s.status == 'ONGOING'}">Đang chiếu</c:when><c:when test="${s.status == 'COMPLETED'}">Đã hoàn thành</c:when><c:when test="${s.status == 'CANCELLED'}">Đã hủy</c:when><c:otherwise><c:out value="${s.status}" /></c:otherwise></c:choose></span></td>
                                <td><div class="st-row-actions"><a class="st-link" href="${pageContext.request.contextPath}/showtimes/seats?id=${s.showtimeId}">Xem ghế</a><c:if test="${s.status == 'SCHEDULED'}"><a class="st-link" href="${pageContext.request.contextPath}/showtimes/edit?id=${s.showtimeId}">Sửa</a><form action="${pageContext.request.contextPath}/showtimes/cancel" method="post" data-cancel-showtime><input type="hidden" name="showtimeId" value="${s.showtimeId}"><button class="st-link st-link-danger" type="submit">Hủy</button></form></c:if></div></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
                <div class="st-empty" id="st-empty" ${not empty showtimes ? 'hidden' : ''}><span class="st-empty-icon" aria-hidden="true">▤</span><h3>${empty showtimes ? 'Chưa có suất chiếu' : 'Không tìm thấy suất chiếu'}</h3><p>${empty showtimes ? 'Tạo suất chiếu đầu tiên bằng biểu mẫu phía trên.' : 'Thử đổi từ khóa hoặc xóa bộ lọc để xem lại danh sách.'}</p></div>
                <footer class="st-list-footer"><span id="st-result-count" role="status" aria-live="polite">Hiển thị ${fn:length(showtimes)} suất chiếu</span><span>Lịch chiếu theo chi nhánh hiện tại</span></footer>
            </section>
        </main>
    </div>
</div>
</body>
</html>
