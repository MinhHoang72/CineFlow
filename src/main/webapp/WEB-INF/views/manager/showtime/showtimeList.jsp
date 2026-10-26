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
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/showtime-pricing/showtimes.css?v=20261024-3">
    <script src="${pageContext.request.contextPath}/js/showtimes.js?v=20261024-2" defer></script>
</head>
<body class="st-showtimes-page">
<div class="cf-manager-shell">
    <jsp:include page='<%= showtimeAdmin ? "/WEB-INF/fragments/adminSidebar.jspf" : "/WEB-INF/fragments/managerSidebar.jspf" %>' />
    <div class="cf-manager-main">
        <jsp:include page="/WEB-INF/fragments/adminTopbar.jspf" />
        <main class="cf-content st-page">
            <header class="st-heading">
                <div><span class="st-eyebrow">ĐIỀU HÀNH RẠP CHIẾU</span><h1>Quản lý suất chiếu</h1><p>Sắp xếp lịch chiếu, theo dõi phòng và quản lý từng suất phim.</p></div>
                <button class="st-button st-button-primary" type="button" id="st-open-create">＋ Tạo suất chiếu</button>
            </header>
            <c:if test="${not empty sessionScope.message}"><div class="st-notice st-success" role="status"><c:out value="${sessionScope.message}" /></div><c:remove var="message" scope="session" /></c:if>
            <c:if test="${not empty sessionScope.error}"><div class="st-notice st-error" role="alert"><c:out value="${sessionScope.error}" /></div><c:remove var="error" scope="session" /></c:if>
            <%@ include file="/WEB-INF/fragments/showtimePricingNav.jspf" %>
            <div class="st-stats" aria-label="Tổng quan lịch chiếu">
                <div><span>Tổng suất chiếu</span><strong>${fn:length(showtimes)}</strong><small>Trong danh sách của cụm rạp</small></div>
                <div><span>Phòng sẵn sàng</span><strong>${fn:length(halls)}</strong><small>Có thể sắp xếp lịch chiếu</small></div>
                <div><span>Phim có thể lên lịch</span><strong>${fn:length(movies)}</strong><small>Được phân phối tại cụm rạp</small></div>
            </div>
            <dialog class="st-create-dialog" id="create-showtime" aria-labelledby="create-title">
                <div class="st-panel-heading"><div><h2 id="create-title">Tạo suất chiếu mới</h2><p>Chọn phim, phòng chiếu và thời gian bắt đầu.</p></div><button type="button" class="st-dialog-close" data-close-create aria-label="Đóng form tạo suất chiếu">×</button></div>
                <form action="${pageContext.request.contextPath}/showtimes/create" method="post" class="st-create-form" id="st-create-form">
                    <div class="st-fields">
                        <div class="st-field"><label for="movieId">Phim <span>*</span></label><select id="movieId" name="movieId" required><option value="">Chọn phim</option><c:forEach var="m" items="${movies}"><option value="${m.movieId}" data-duration="${m.duration}"><c:out value="${m.title}" /> · ${m.duration} phút</option></c:forEach></select></div>
                        <div class="st-field"><label for="hallId">Phòng chiếu <span>*</span></label><select id="hallId" name="hallId" required><option value="">Chọn phòng chiếu</option><c:forEach var="h" items="${halls}"><option value="${h.hallId}"><c:out value="${h.hallName}" /> · <c:out value="${h.hallType}" /></option></c:forEach></select></div>
                        <div class="st-field"><label for="startTime">Thời gian bắt đầu <span>*</span></label><input id="startTime" name="startTime" type="datetime-local" required></div>
                    </div>
                    <div id="st-availability" class="st-availability" role="status" aria-live="polite">Chọn phim, phòng và giờ bắt đầu để kiểm tra lịch trống.</div>
                    <div class="st-form-footer"><p>Thời gian kết thúc được tính tự động theo thời lượng phim.</p><button type="button" class="st-button st-button-muted" data-close-create>Hủy</button><button id="st-create-submit" type="submit" class="st-button st-button-primary" ${empty movies or empty halls ? 'disabled' : ''}>Tạo suất chiếu</button></div>
                    <c:if test="${empty movies or empty halls}"><p class="st-prerequisite">Cần có phim được phân phối và phòng sẵn sàng để tạo suất chiếu.</p></c:if>
                </form>
            </dialog>
            <section class="st-panel st-schedule" aria-labelledby="schedule-title">
                <div class="st-panel-heading"><div><h2 id="schedule-title">Danh sách suất chiếu <span class="st-count">${fn:length(showtimes)}</span></h2><p>Theo dõi lịch chiếu và thao tác trực tiếp trên từng suất.</p></div><span class="st-step">02 / QUẢN LÝ</span></div>
                <div class="st-sticky-tools"><div class="st-filters" role="search" aria-label="Lọc suất chiếu">
                    <div class="st-field st-search"><label for="st-search">Tìm phim</label><input id="st-search" type="search" placeholder="Nhập tên phim…"></div>
                    <div class="st-field"><label for="st-date">Ngày chiếu</label><input id="st-date" type="date"></div>
                    <div class="st-field"><label for="st-hall">Phòng chiếu</label><select id="st-hall"><option value="">Tất cả phòng</option></select></div>
                    <div class="st-field"><label for="st-status">Trạng thái</label><select id="st-status"><option value="">Tất cả trạng thái</option><option value="SCHEDULED">Sắp chiếu</option><option value="ONGOING">Đang chiếu</option><option value="COMPLETED">Đã hoàn thành</option><option value="CANCELLED">Đã hủy</option></select></div>
                    <button type="button" class="st-button st-button-muted" id="st-reset">Xóa lọc</button>
                </div>
                <div class="st-view-toolbar">
                    <div class="st-quick-dates" role="group" aria-label="Lọc nhanh ngày chiếu"><button type="button" data-quick="today">Hôm nay</button><button type="button" data-quick="tomorrow">Ngày mai</button><button type="button" data-quick="week">Tuần này</button></div>
                    <div class="st-view-switch" role="group" aria-label="Chế độ hiển thị"><button type="button" data-view="table" aria-pressed="true">Dạng bảng</button><button type="button" data-view="timeline" aria-pressed="false">Dòng thời gian</button></div>
                </div><p id="st-range-label" role="status" aria-live="polite"></p></div>
                <div id="st-timeline" hidden>
                    <div class="st-timeline-toolbar"><label for="st-timeline-date">Lịch theo phòng ngày</label><input id="st-timeline-date" type="date"><span>Phần gạch chéo: dọn phòng · Viền đỏ: trùng lịch</span></div>
                    <div class="st-timeline-scroll" tabindex="0" role="region" aria-label="Dòng thời gian theo phòng"><div id="st-timeline-grid"></div></div>
                    <p id="st-timeline-summary" role="status" class="st-timeline-summary"></p>
                </div>
                <div id="st-table-view"><div class="st-table-scroll" role="region" aria-label="Bảng lịch chiếu" tabindex="0">
                    <table class="st-table"><thead><tr><th scope="col">Phim / Mã suất</th><th scope="col">Phòng chiếu</th><th scope="col">Bắt đầu</th><th scope="col">Kết thúc</th><th scope="col">Dọn phòng</th><th scope="col">Trạng thái</th><th scope="col">Thao tác</th></tr></thead>
                        <tbody id="st-rows">
                        <c:forEach var="s" items="${showtimes}">
                            <tr data-showtime data-showtime-id="${s.showtimeId}" data-start="${s.startTime}" data-end="${s.endTime}" data-buffer="${s.cleaningBufferMinutes}" data-title="<c:out value='${s.movieTitle}' />" data-hall-id="${s.hallId}" data-hall="<c:out value='${s.hallName}' />" data-date="${fn:substring(s.startTimeInputFormatted, 0, 10)}" data-status="<c:out value='${s.status}' />">
                                <td><strong class="st-movie"><c:out value="${s.movieTitle}" /></strong><span class="st-id">#${s.showtimeId}</span></td>
                                <td><span class="st-hall-name"><c:out value="${s.hallName}" /></span><span class="st-format"><c:out value="${s.hallType}" /></span></td>
                                <td class="st-time"><c:out value="${s.startTimeFormatted}" /></td><td class="st-time"><c:out value="${s.endTimeFormatted}" /></td>
                                <td class="st-buffer">${s.cleaningBufferMinutes} phút</td>
                                <td><span class="st-status st-status-${fn:escapeXml(s.status)}"><c:choose><c:when test="${s.status == 'SCHEDULED'}">Sắp chiếu</c:when><c:when test="${s.status == 'ONGOING'}">Đang chiếu</c:when><c:when test="${s.status == 'COMPLETED'}">Đã hoàn thành</c:when><c:when test="${s.status == 'CANCELLED'}">Đã hủy</c:when><c:otherwise><c:out value="${s.status}" /></c:otherwise></c:choose></span></td>
                                <td><div class="st-row-actions"><a class="st-link st-icon-action" title="Xem ghế" aria-label="Xem ghế suất chiếu #${s.showtimeId}" href="${pageContext.request.contextPath}/showtimes/seats?id=${s.showtimeId}"><svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/></svg></a><c:if test="${s.status == 'SCHEDULED'}"><a class="st-link st-icon-action" title="Sửa suất chiếu" aria-label="Sửa suất chiếu #${s.showtimeId}" href="${pageContext.request.contextPath}/showtimes/edit?id=${s.showtimeId}"><svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="m15 5 4 4M4 20l4-1L20 7a2.8 2.8 0 0 0-4-4L4 15l-1 6Z"/></svg></a><form action="${pageContext.request.contextPath}/showtimes/cancel" method="post" data-cancel-showtime><input type="hidden" name="showtimeId" value="${s.showtimeId}"><button class="st-link st-link-danger st-icon-action" title="Hủy suất chiếu" aria-label="Hủy suất chiếu #${s.showtimeId}" type="submit"><svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><circle cx="12" cy="12" r="9"/><path d="m9 9 6 6m0-6-6 6"/></svg></button></form></c:if></div></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
                </div><div class="st-empty" id="st-empty" ${not empty showtimes ? 'hidden' : ''}><span class="st-empty-icon" aria-hidden="true">▤</span><h3>${empty showtimes ? 'Chưa có suất chiếu' : 'Không tìm thấy suất chiếu'}</h3><p>${empty showtimes ? 'Tạo suất chiếu đầu tiên bằng biểu mẫu phía trên.' : 'Thử đổi từ khóa hoặc xóa bộ lọc để xem lại danh sách.'}</p></div>
                <footer class="st-list-footer"><span id="st-result-count" role="status" aria-live="polite">Hiển thị ${fn:length(showtimes)} suất chiếu</span><div class="st-pagination" id="st-pagination"><label for="st-page-size">Số dòng</label><select id="st-page-size"><option value="10">10</option><option value="25">25</option><option value="50">50</option></select><button type="button" id="st-prev-page" aria-label="Trang trước">‹</button><span id="st-page-info"></span><button type="button" id="st-next-page" aria-label="Trang sau">›</button></div></footer>
            </section>
        </main>
    </div>
</div>
</body>
</html>
