<%@ page pageEncoding="UTF-8" %>
<% request.setAttribute("pageTitle", "Tình trạng ghế"); request.setAttribute("pageDescription", "Sơ đồ phòng chiếu và tình trạng ghế theo suất chiếu."); request.setAttribute("activeTab", "showtimes"); request.setAttribute("seatMatrixView", true); %>
<%@ include file="/WEB-INF/fragments/cinemaBegin.jspf" %>
<div class="st-seat-view">
<c:choose><c:when test="${not empty showtime}">
<div class="cf-hall-banner"><div class="cf-hall-meta"><div class="cf-hall-badge-icon" aria-hidden="true">🎬</div><div><h2><c:out value="${showtime.hallName}" /> · <c:out value="${showtime.movieTitle}" /></h2><p>Suất #${showtime.showtimeId} &nbsp;|&nbsp; Định dạng: <strong><c:out value="${showtime.hallType}" /></strong> &nbsp;|&nbsp; Bắt đầu: <strong><c:out value="${showtime.startTimeFormatted}" /></strong></p></div></div><a class="st-button st-button-muted" href="${pageContext.request.contextPath}/showtimes">← Danh sách suất chiếu</a></div>
<c:set var="availableCount" value="0" /><c:set var="heldCount" value="0" /><c:set var="bookedCount" value="0" /><c:set var="maintenanceCount" value="0" /><c:set var="totalSeats" value="0" />
<c:forEach var="row" items="${seatRows}"><c:forEach var="seat" items="${row.value}">
<c:if test="${seat.availabilityStatus != 'UNAVAILABLE'}"><c:set var="totalSeats" value="${totalSeats + 1}" /></c:if>
<c:if test="${seat.availabilityStatus == 'AVAILABLE'}"><c:set var="availableCount" value="${availableCount + 1}" /></c:if>
<c:if test="${seat.availabilityStatus == 'HELD'}"><c:set var="heldCount" value="${heldCount + 1}" /></c:if>
<c:if test="${seat.availabilityStatus == 'BOOKED'}"><c:set var="bookedCount" value="${bookedCount + 1}" /></c:if>
<c:if test="${seat.availabilityStatus == 'MAINTENANCE'}"><c:set var="maintenanceCount" value="${maintenanceCount + 1}" /></c:if>
</c:forEach></c:forEach>
<div class="cf-capacity-summary"><div class="cf-capacity-stats"><div class="cf-stat-item"><span>Tổng ghế:</span><span class="cf-stat-pill standard">${totalSeats}</span></div><div class="cf-stat-item"><span>Trống:</span><span class="cf-stat-pill st-pill-available">${availableCount}</span></div><div class="cf-stat-item"><span>Đang giữ:</span><span class="cf-stat-pill st-pill-held">${heldCount}</span></div><div class="cf-stat-item"><span>Đã đặt:</span><span class="cf-stat-pill st-pill-booked">${bookedCount}</span></div><div class="cf-stat-item"><span>Bảo trì:</span><span class="cf-stat-pill st-pill-maintenance">${maintenanceCount}</span></div></div><span class="st-readonly-label">Chế độ chỉ xem</span></div>
<c:choose><c:when test="${not empty seatRows}">
<div class="cf-seat-canvas-wrapper" tabindex="0" role="region" aria-label="Sơ đồ ghế phòng chiếu, cuộn ngang để xem toàn bộ">
<div class="cf-screen"><div class="cf-screen-label">MÀN HÌNH CHIẾU</div></div>
<div class="cf-seat-grid" aria-label="Vị trí ghế theo hàng và cột">
<c:forEach var="row" items="${seatRows}"><div class="cf-grid-row"><span class="cf-row-label"><c:out value="${row.key}" /></span>
<c:forEach var="seat" items="${row.value}">
<c:choose><c:when test="${seat.availabilityStatus == 'UNAVAILABLE'}"><span class="cf-seat-box EMPTY" title="Lối đi / Trống" aria-label="Lối đi hoặc vị trí trống">—</span></c:when>
<c:otherwise><span class="cf-seat-box ${fn:escapeXml(seat.seatType)} ${fn:escapeXml(seat.availabilityStatus)}" title="<c:out value='${seat.displayLabel}' /> · <c:out value='${seat.seatType}' /> · <c:out value='${seat.availabilityLabel}' />" role="img" aria-label="<c:out value='${seat.displayLabel}' /> · <c:out value='${seat.seatType}' /> · <c:out value='${seat.availabilityLabel}' />"><c:out value="${seat.displayLabel}" /><c:if test="${seat.availabilityStatus == 'HELD'}"><span class="st-seat-marker" aria-hidden="true">●</span></c:if><c:if test="${seat.availabilityStatus == 'BOOKED'}"><span class="st-seat-marker" aria-hidden="true">×</span></c:if></span></c:otherwise></c:choose>
</c:forEach><span class="cf-row-label"><c:out value="${row.key}" /></span></div></c:forEach>
</div>
<div class="st-seat-legends">
<section class="st-legend-group" aria-labelledby="st-type-legend"><h3 id="st-type-legend">Loại ghế (khi còn trống)</h3><div class="st-legend-items"><span><i class="st-legend-swatch st-type-standard"></i>Ghế thường</span><span><i class="st-legend-swatch st-type-vip"></i>VIP</span><span><i class="st-legend-swatch st-type-sweetbox"></i>Ghế đôi Sweetbox</span><span><i class="st-legend-swatch st-type-aisle">—</i>Lối đi / Trống</span></div></section>
<section class="st-legend-group" aria-labelledby="st-status-legend"><h3 id="st-status-legend">Trạng thái</h3><div class="st-legend-items"><span><i class="st-legend-swatch st-type-standard"></i>Trống (màu theo loại ghế)</span><span><i class="st-legend-swatch st-state-held">●</i>Đang giữ</span><span><i class="st-legend-swatch st-state-booked">×</i>Đã đặt</span><span><i class="st-legend-swatch st-state-maintenance"></i>Đang bảo trì</span></div></section>
</div>
</div>
<p class="st-seat-note">Ghế được đánh số liên tiếp trong từng hàng, bỏ qua lối đi, giống sơ đồ cấu hình phòng chiếu. Tải lại trang để cập nhật tình trạng mới nhất.</p>
</c:when><c:otherwise><div class="st-empty"><h3>Chưa có sơ đồ ghế</h3><p>Phòng chiếu này chưa được cấu hình ghế.</p></div></c:otherwise></c:choose>
</c:when><c:otherwise><div class="st-empty"><h2>Không tìm thấy suất chiếu</h2><a class="st-link" href="${pageContext.request.contextPath}/showtimes">Quay lại danh sách</a></div></c:otherwise></c:choose>
</div>
<%@ include file="/WEB-INF/fragments/cinemaEnd.jspf" %>
