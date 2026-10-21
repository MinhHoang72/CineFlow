<%@ page pageEncoding="UTF-8" %>
<% request.setAttribute("pageTitle", "Sửa suất chiếu"); request.setAttribute("pageDescription", "Điều chỉnh phim, phòng và thời gian của suất chiếu đã lên lịch."); request.setAttribute("activeTab", "showtimes"); %>
<%@ include file="/WEB-INF/fragments/cinemaBegin.jspf" %>
<c:choose><c:when test="${not empty showtime}">
<section class="st-panel"><div class="st-panel-heading"><div><h2>Thông tin suất chiếu #${showtime.showtimeId}</h2><p>Thời gian kết thúc được tính lại theo thời lượng phim.</p></div><a class="st-link" href="${pageContext.request.contextPath}/showtimes">← Danh sách suất chiếu</a></div>
<form class="st-create-form" action="${pageContext.request.contextPath}/showtimes/update" method="post">
<input type="hidden" name="showtimeId" value="${showtime.showtimeId}">
<div class="st-fields">
<div class="st-field"><label for="edit-movie">Phim <span>*</span></label><select id="edit-movie" name="movieId" required><c:forEach var="m" items="${movies}"><option value="${m.movieId}" ${m.movieId == showtime.movieId ? 'selected' : ''}><c:out value="${m.title}" /> · ${m.duration} phút</option></c:forEach></select></div>
<div class="st-field"><label for="edit-hall">Phòng chiếu <span>*</span></label><select id="edit-hall" name="hallId" required><c:forEach var="h" items="${halls}"><option value="${h.hallId}" ${h.hallId == showtime.hallId ? 'selected' : ''}><c:out value="${h.hallName}" /> · <c:out value="${h.hallType}" /></option></c:forEach></select></div>
<div class="st-field"><label for="edit-start">Thời gian bắt đầu <span>*</span></label><input id="edit-start" type="datetime-local" name="startTime" value="${showtime.startTimeInputFormatted}" required></div>
</div><div class="st-form-footer"><p>Dọn phòng: ${showtime.cleaningBufferMinutes} phút sau mỗi suất chiếu.</p><div class="st-row-actions"><a class="st-button st-button-muted" href="${pageContext.request.contextPath}/showtimes">Quay lại</a><button class="st-button st-button-primary" type="submit">Lưu thay đổi</button></div></div>
</form></section>
</c:when><c:otherwise><div class="st-empty"><h2>Không tìm thấy suất chiếu</h2><a class="st-link" href="${pageContext.request.contextPath}/showtimes">Quay lại danh sách</a></div></c:otherwise></c:choose>
<%@ include file="/WEB-INF/fragments/cinemaEnd.jspf" %>
