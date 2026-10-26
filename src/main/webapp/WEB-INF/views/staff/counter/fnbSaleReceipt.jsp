<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%-- Ban bap nuoc le tai quay, buoc 3: hoa don. Can co: sale (FnbSaleDTO), paidTransaction (PaymentTransaction). --%>
<c:set var="pageTitle" value="Hoá đơn bắp nước" scope="request"/>
<c:set var="pageStep" value="3" scope="request"/>
<%@ include file="/WEB-INF/fragments/header.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<style>
    .eticket { border: 2px dashed #333; border-radius: 10px; background: #fff; padding: 20px; }
    @media print {
        .no-print, .alert { display: none !important; }
        body { background: #fff; }
    }
</style>

<div class="d-flex gap-2 mb-3 no-print">
    <button type="button" class="btn btn-primary" onclick="window.print()">🖨 In hoá đơn</button>
    <a href="${ctx}/staff/fnb-sale" class="btn btn-outline-secondary">Bán đơn khác</a>
</div>

<div class="eticket" data-bs-theme="light" style="max-width: 420px">
    <div class="text-center">
        <div class="fw-bold">CINEFLOW · <c:out value="${sale.centreName}"/></div>
        <div class="fs-5 fw-bold">HOÁ ĐƠN BẮP NƯỚC</div>
        <div class="small">Đơn <c:out value="${sale.code}"/></div>
        <div class="small text-muted">
            <fmt:formatDate value="${paidTransaction.paidAt}" pattern="HH:mm:ss dd/MM/yyyy"/> ·
            NV: <c:out value="${sessionScope.user.fullName}"/>
        </div>
    </div>
    <hr>
    <table class="table table-sm table-borderless mb-0">
        <c:forEach var="line" items="${sale.lines}">
            <tr><td><c:out value="${line.name}"/> × ${line.quantity}</td>
                <td class="text-end"><fmt:formatNumber value="${line.lineTotal}" pattern="#,##0"/></td></tr>
        </c:forEach>
        <tr class="border-top fw-bold"><td>Tổng cộng</td>
            <td class="text-end"><fmt:formatNumber value="${sale.totalAmount}" pattern="#,##0"/> đ</td></tr>
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
    <div class="text-center small text-muted">Nhận món tại quầy bắp nước. Cảm ơn quý khách!</div>
</div>

<%@ include file="/WEB-INF/fragments/footer.jspf" %>
