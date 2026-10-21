<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="In vé & hoá đơn" scope="request"/>
<c:set var="pageStep" value="4" scope="request"/>
<%@ include file="/WEB-INF/fragments/header.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="st" value="${booking.showtime}"/>

<div class="d-flex gap-2 mb-3 no-print">
    <button type="button" class="btn btn-primary" onclick="window.print()">🖨 In vé + hoá đơn</button>
    <a href="${ctx}/staff/counter" class="btn btn-outline-secondary">Bán vé tiếp</a>
</div>

<c:choose>
    <c:when test="${empty tickets}">
        <div class="alert alert-warning">Đơn <c:out value="${booking.code}"/> chưa được thanh toán nên chưa có vé.</div>
    </c:when>
    <c:otherwise>
        <%@ include file="/WEB-INF/views/booking/ticket.jspf" %>

        <%-- Hoa don --%>
        <div class="eticket mt-3" style="max-width: 420px">
            <div class="text-center">
                <div class="fw-bold">CINEFLOW · <c:out value="${st.branchName}"/></div>
                <div class="fs-5 fw-bold">HOÁ ĐƠN THANH TOÁN</div>
                <div class="small">Đơn <c:out value="${booking.code}"/></div>
                <div class="small text-muted">
                    <fmt:formatDate value="${paidTransaction.paidAt}" pattern="HH:mm:ss dd/MM/yyyy"/> ·
                    NV: <c:out value="${sessionScope.user.fullName}"/>
                </div>
            </div>
            <hr>
            <table class="table table-sm table-borderless mb-0">
                <c:forEach var="seat" items="${booking.seats}">
                    <tr><td>Vé ghế ${seat.label} (${seat.type})</td>
                        <td class="text-end"><fmt:formatNumber value="${seat.price}" pattern="#,##0"/></td></tr>
                </c:forEach>
                <c:forEach var="line" items="${booking.fnbLines}">
                    <tr><td><c:out value="${line.name}"/> × ${line.quantity}</td>
                        <td class="text-end"><fmt:formatNumber value="${line.lineTotal}" pattern="#,##0"/></td></tr>
                </c:forEach>
                <c:if test="${booking.discountAmount > 0}">
                    <tr><td>Giảm giá <c:out value="${booking.voucherCode}"/> (tiền vé)</td>
                        <td class="text-end">−<fmt:formatNumber value="${booking.discountAmount}" pattern="#,##0"/></td></tr>
                </c:if>
                <tr class="border-top fw-bold"><td>Tổng cộng</td>
                    <td class="text-end"><fmt:formatNumber value="${booking.totalAmount}" pattern="#,##0"/> đ</td></tr>
                <tr><td>Hình thức</td>
                    <td class="text-end">${paidTransaction.method == 'CASH' ? 'Tiền mặt' : 'VNPay'}</td></tr>
                <c:if test="${not empty paidTransaction.cashReceived}">
                    <tr><td>Tiền khách đưa</td>
                        <td class="text-end"><fmt:formatNumber value="${paidTransaction.cashReceived}" pattern="#,##0"/> đ</td></tr>
                    <tr class="fw-bold"><td>Tiền thối</td>
                        <td class="text-end"><fmt:formatNumber value="${paidTransaction.change}" pattern="#,##0"/> đ</td></tr>
                </c:if>
            </table>
            <hr>
            <div class="text-center small text-muted">Cảm ơn quý khách!</div>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/fragments/footer.jspf" %>
