<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%-- Cấu hình trang: tiêu đề, bước 4, header; st = suất chiếu --%>
<c:set var="pageTitle" value="Xác nhận & thanh toán" scope="request"/>
<c:set var="pageStep" value="4" scope="request"/>
<%@ include file="/WEB-INF/fragments/header.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="st" value="${booking.showtime}"/>

<div class="row justify-content-center">
    <div class="col-lg-7">
        <div class="card">
            <div class="card-body">
                <%-- [1] MÃ ĐƠN + nhãn "Giữ ghế còn" (chỉ khi còn hạn) --%>
                <div class="d-flex justify-content-between align-items-start">
                    <h5>Đơn <c:out value="${booking.code}"/></h5>
                    <c:if test="${booking.holdActive}">
                        <span class="badge bg-warning text-dark fs-6">Giữ ghế còn <span id="countdown">--:--</span></span>
                    </c:if>
                </div>

                <%-- [2] THÔNG TIN ĐƠN: phim, rạp, suất chiếu, từng ghế kèm giá --%>
                <table class="table mb-3">
                    <tr><th>Phim</th><td><c:out value="${st.movieTitle}"/></td></tr>
                    <tr><th>Rạp</th><td><c:out value="${st.centreName}"/> · <c:out value="${st.hallName}"/></td></tr>
                    <tr><th>Suất chiếu</th><td><fmt:formatDate value="${st.startTime}" pattern="HH:mm, dd/MM/yyyy"/></td></tr>
                    <tr><th>Ghế</th>
                        <td>
                            <c:forEach var="seat" items="${booking.seats}">
                                <div>${seat.label} (${seat.type}) — <fmt:formatNumber value="${seat.price}" pattern="#,##0"/> đ</div>
                            </c:forEach>
                        </td>
                    </tr>
                </table>
                <%-- [3] BẢNG TIỀN: amountRows.jspf (tiền vé, giảm giá, bắp nước) + Tổng thanh toán (booking.totalAmount) --%>
                <table class="table table-sm mb-3">
                    <%@ include file="/WEB-INF/views/customer/booking/amountRows.jspf" %>
                    <tr class="border-top"><th class="fs-5">Tổng thanh toán</th>
                        <td class="text-end fs-5 fw-bold"><fmt:formatNumber value="${booking.totalAmount}" pattern="#,##0"/> đ</td></tr>
                </table>

                <%-- [4] NÚT HÀNH ĐỘNG: còn hạn → voucher + thanh toán + huỷ; hết hạn / đã huỷ → thông báo + Chọn ghế lại --%>
                <c:choose>
                    <c:when test="${booking.holdActive}">
                        <%-- Chèn khung Sửa bắp nước + Mã giảm giá (bookingVoucher.jspf) --%>
                        <%@ include file="/WEB-INF/views/customer/booking/bookingVoucher.jspf" %>
                        <%-- FORM THANH TOÁN: POST /payment/vnpay (sang PaymentServlet), chỉ gửi bookingId --%>
                        <form method="post" action="${ctx}/payment/vnpay" class="mb-2">
                            <input type="hidden" name="bookingId" value="${booking.id}">
                            <button type="submit" class="btn btn-primary w-100 btn-lg">Thanh toán qua VNPay</button>
                            <div class="form-text text-center">Quét mã QR bằng app ngân hàng hoặc dùng thẻ ATM/Visa.
                                Vé được xuất ngay khi VNPay xác nhận.</div>
                        </form>
                        <%-- FORM HUỶ ĐƠN: POST /booking action=cancel (hỏi xác nhận) --%>
                        <form method="post" action="${ctx}/booking"
                              onsubmit="return confirm('Huỷ đơn và nhả ghế?');">
                            <input type="hidden" name="action" value="cancel">
                            <input type="hidden" name="bookingId" value="${booking.id}">
                            <button type="submit" class="btn btn-outline-danger w-100">Huỷ đơn, chọn lại ghế</button>
                        </form>
                    </c:when>
                    <%-- Hết hạn hoặc đã huỷ: thông báo + link Chọn ghế lại --%>
                    <c:otherwise>
                        <div class="alert alert-secondary">
                            <c:choose>
                                <c:when test="${booking.status == 'CANCELLED'}">Đơn này đã bị huỷ.</c:when>
                                <c:otherwise>Đơn đã hết thời gian giữ ghế.</c:otherwise>
                            </c:choose>
                        </div>
                        <a href="${ctx}/booking?action=select-seats&showtimeId=${st.id}" class="btn btn-primary w-100">Chọn ghế lại</a>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</div>

<%-- [5] JAVASCRIPT ĐẾM NGƯỢC (chỉ khi còn hạn): hết giờ thì tải lại trang --%>
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

