<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="Thanh toán không thành công" scope="request"/>
<c:set var="pageStep" value="!" scope="request"/>
<%@ include file="/WEB-INF/fragments/header.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="st" value="${booking.showtime}"/>

<div class="row justify-content-center">
    <div class="col-lg-7">
        <div class="card border-danger">
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-start mb-2">
                    <h5 class="text-danger mb-0">
                        <c:choose>
                            <c:when test="${reason == 'PAYMENT_FAILED'}">Thanh toán VNPay không thành công</c:when>
                            <c:when test="${reason == 'EXPIRED'}">Đã hết thời gian giữ ghế</c:when>
                            <c:otherwise>Đơn đã bị huỷ</c:otherwise>
                        </c:choose>
                    </h5>
                    <c:if test="${booking.holdActive}">
                        <span class="badge bg-warning text-dark fs-6">Giữ ghế còn <span id="countdown">--:--</span></span>
                    </c:if>
                </div>

                <p class="mb-3">
                    <c:choose>
                        <c:when test="${reason == 'PAYMENT_FAILED'}">
                            VNPay báo giao dịch cho đơn <strong><c:out value="${booking.code}"/></strong> không thành công
                            và bạn chưa bị trừ tiền. Ghế vẫn đang được giữ: bạn có thể thanh toán lại hoặc huỷ đơn.
                        </c:when>
                        <c:when test="${reason == 'EXPIRED'}">
                            Đơn <strong><c:out value="${booking.code}"/></strong> chưa được thanh toán trong thời gian giữ ghế
                            nên ghế đã được nhả cho người khác. Nếu tài khoản của bạn đã bị trừ tiền, vui lòng liên hệ quầy vé để được hỗ trợ.
                        </c:when>
                        <c:otherwise>
                            Đơn <strong><c:out value="${booking.code}"/></strong> đã được huỷ, ghế đã được nhả.
                        </c:otherwise>
                    </c:choose>
                </p>

                <div class="small text-muted mb-3">
                    <c:out value="${st.movieTitle}"/> · <c:out value="${st.branchName}"/> ·
                    <fmt:formatDate value="${st.startTime}" pattern="HH:mm dd/MM/yyyy"/> · Ghế ${booking.seatLabels} ·
                    <fmt:formatNumber value="${booking.totalAmount}" pattern="#,##0"/> đ
                </div>

                <c:choose>
                    <c:when test="${reason == 'PAYMENT_FAILED' and booking.holdActive}">
                        <form method="post" action="${ctx}/payment/vnpay" class="mb-2">
                            <input type="hidden" name="bookingId" value="${booking.id}">
                            <button type="submit" class="btn btn-primary w-100 btn-lg">Thanh toán lại qua VNPay</button>
                        </form>
                        <form method="post" action="${ctx}/booking" onsubmit="return confirm('Huỷ đơn và nhả ghế?');">
                            <input type="hidden" name="action" value="cancel">
                            <input type="hidden" name="bookingId" value="${booking.id}">
                            <button type="submit" class="btn btn-outline-danger w-100">Huỷ đơn</button>
                        </form>
                    </c:when>
                    <c:otherwise>
                        <a href="${ctx}/booking?action=select-seats&showtimeId=${st.id}" class="btn btn-primary w-100 mb-2">Đặt lại suất này</a>
                        <a href="${ctx}/booking?action=detail&bookingId=${booking.id}" class="btn btn-outline-secondary w-100">Xem chi tiết đơn</a>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</div>

<c:if test="${booking.holdActive}">
<script>
    (function () {
        let left = ${booking.secondsLeft};
        const el = document.getElementById('countdown');
        function tick() {
            if (left <= 0) { location.reload(); return; }
            el.textContent = String(Math.floor(left / 60)).padStart(2, '0') + ':' + String(left % 60).padStart(2, '0');
            left--;
        }
        tick();
        setInterval(tick, 1000);
    })();
</script>
</c:if>

<%@ include file="/WEB-INF/fragments/footer.jspf" %>
