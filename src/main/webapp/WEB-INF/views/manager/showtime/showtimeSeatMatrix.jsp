<%@ page pageEncoding="UTF-8" %>
<% request.setAttribute("pageTitle", "Tình trạng ghế"); request.setAttribute("pageDescription", "Theo dõi ghế trống, ghế đang giữ và ghế đã đặt theo suất chiếu."); request.setAttribute("activeTab", "showtimes"); %>
<%@ include file="/WEB-INF/fragments/cinemaBegin.jspf" %>
<c:choose><c:when test="${not empty showtime}">
<section class="st-panel"><div class="st-panel-heading"><div><h2><c:out value="${showtime.movieTitle}" /></h2><p>#${showtime.showtimeId} · <c:out value="${showtime.hallName}" /> · <c:out value="${showtime.hallType}" /> · <c:out value="${showtime.startTimeFormatted}" /></p></div><a class="st-button st-button-muted" href="${pageContext.request.contextPath}/showtimes">← Quay lại</a></div>
<c:set var="availableCount" value="0" /><c:set var="heldCount" value="0" /><c:set var="bookedCount" value="0" />
<c:forEach var="seat" items="${seats}"><c:if test="${seat.availabilityStatus == 'AVAILABLE'}"><c:set var="availableCount" value="${availableCount + 1}" /></c:if><c:if test="${seat.availabilityStatus == 'HELD'}"><c:set var="heldCount" value="${heldCount + 1}" /></c:if><c:if test="${seat.availabilityStatus == 'BOOKED'}"><c:set var="bookedCount" value="${bookedCount + 1}" /></c:if></c:forEach>
<div class="st-seat-summary"><span class="st-status st-status-COMPLETED">Ghế trống: ${availableCount}</span><span class="st-status st-status-ONGOING">Đang giữ: ${heldCount}</span><span class="st-status st-status-CANCELLED">Đã đặt: ${bookedCount}</span><span>Tổng: ${fn:length(seats)} ghế</span></div>
<div class="st-seat-grid" aria-label="Danh sách tình trạng từng ghế">
<c:forEach var="seat" items="${seats}"><div class="st-seat st-seat-${fn:escapeXml(seat.availabilityStatus)}"><strong><c:out value="${seat.seatRow}" />${seat.seatNumber}</strong><small><c:out value="${seat.seatType}" /></small><span><c:choose><c:when test="${seat.availabilityStatus == 'AVAILABLE'}">Trống</c:when><c:when test="${seat.availabilityStatus == 'HELD'}">Đang giữ</c:when><c:when test="${seat.availabilityStatus == 'BOOKED'}">Đã đặt</c:when><c:otherwise>Không khả dụng</c:otherwise></c:choose></span></div></c:forEach>
</div><c:if test="${empty seats}"><div class="st-empty"><h3>Chưa có dữ liệu ghế</h3><p>Phòng chiếu này chưa có danh sách ghế để hiển thị.</p></div></c:if>
<footer class="st-list-footer"><span>Danh sách ghế theo mã ghế · Chỉ xem</span><span>Tải lại trang để cập nhật tình trạng mới nhất</span></footer></section>
</c:when><c:otherwise><div class="st-empty"><h2>Không tìm thấy suất chiếu</h2><a class="st-link" href="${pageContext.request.contextPath}/showtimes">Quay lại danh sách</a></div></c:otherwise></c:choose>
<%@ include file="/WEB-INF/fragments/cinemaEnd.jspf" %>
