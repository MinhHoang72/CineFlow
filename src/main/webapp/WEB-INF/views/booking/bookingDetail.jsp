<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="Chi tiết đơn" scope="request"/>
<c:set var="pageStep" value="📄" scope="request"/>
<%@ include file="/WEB-INF/fragments/header.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="st" value="${booking.showtime}"/>

<div class="d-flex flex-wrap justify-content-between align-items-center mb-3 gap-2">
    <h5 class="mb-0">Đơn <c:out value="${booking.code}"/>
        <span class="badge ${booking.statusBadge}">${booking.statusLabel}</span>
        <span class="badge text-bg-secondary">${booking.counter ? 'Tại quầy' : 'Online'}</span>
    </h5>
    <div class="d-flex gap-2 no-print">
        <c:if test="${isOwner and booking.holdActive}">
            <a href="${ctx}/booking?action=checkout&bookingId=${booking.id}" class="btn btn-warning">Tiếp tục thanh toán</a>
        </c:if>
        <c:if test="${isOwner}">
            <a href="${ctx}/account" class="btn btn-outline-primary">← Lịch sử đặt vé</a>
        </c:if>
    </div>
</div>

<c:choose>
    <c:when test="${not empty tickets}">
        <h6>Vé điện tử</h6>
        <%@ include file="/WEB-INF/views/booking/ticket.jspf" %>
    </c:when>
    <c:otherwise>
        <div class="card mb-3"><div class="card-body">
            <div class="fs-5 fw-bold"><c:out value="${st.movieTitle}"/></div>
            <div><c:out value="${st.branchName}"/> · <c:out value="${st.hallName}"/> (${st.hallType}) ·
                <fmt:formatDate value="${st.startTime}" pattern="HH:mm dd/MM/yyyy"/></div>
            <table class="table table-sm mt-3 mb-0">
                <thead><tr><th>Hạng mục</th><th>Chi tiết</th><th class="text-end">Thành tiền</th></tr></thead>
                <tbody>
                <c:forEach var="seat" items="${booking.seats}">
                    <tr><td>Ghế <strong>${seat.label}</strong></td><td>${seat.type}</td>
                        <td class="text-end"><fmt:formatNumber value="${seat.price}" pattern="#,##0"/> đ</td></tr>
                </c:forEach>
                <c:forEach var="line" items="${booking.fnbLines}">
                    <tr><td><c:out value="${line.name}"/></td><td>${line.combo ? 'Combo' : 'Bắp nước'} × ${line.quantity}</td>
                        <td class="text-end"><fmt:formatNumber value="${line.lineTotal}" pattern="#,##0"/> đ</td></tr>
                </c:forEach>
                <c:if test="${booking.discountAmount > 0}">
                    <tr><td>Giảm giá</td><td><c:out value="${booking.voucherCode}"/> (trên tiền vé)</td>
                        <td class="text-end text-success">− <fmt:formatNumber value="${booking.discountAmount}" pattern="#,##0"/> đ</td></tr>
                </c:if>
                </tbody>
                <tfoot><tr><th colspan="2">Tổng cộng</th>
                    <th class="text-end"><fmt:formatNumber value="${booking.totalAmount}" pattern="#,##0"/> đ</th></tr></tfoot>
            </table>
            <div class="small text-muted mt-2">Đơn chưa được thanh toán nên chưa có vé điện tử.</div>
        </div></div>
    </c:otherwise>
</c:choose>

<div class="card mt-3 no-print">
    <div class="card-body">
        <h6>Thanh toán</h6>
        <c:choose>
            <c:when test="${empty transactions}">
                <div class="text-muted small">Chưa có giao dịch thanh toán nào.</div>
            </c:when>
            <c:otherwise>
                <div class="table-responsive">
                    <table class="table table-sm mb-0">
                        <thead>
                        <tr><th>Mã giao dịch</th><th>Hình thức</th><th class="text-end">Số tiền</th>
                            <th>Trạng thái</th><th>Tạo lúc</th><th>Thanh toán lúc</th></tr>
                        </thead>
                        <tbody>
                        <c:forEach var="p" items="${transactions}">
                            <tr>
                                <td><code><c:out value="${p.transactionCode}"/></code></td>
                                <td>${p.method == 'CASH' ? 'Tiền mặt' : 'VNPay'}
                                    <c:if test="${not empty p.cashReceived}">
                                        <br><small class="text-muted">Khách đưa <fmt:formatNumber value="${p.cashReceived}" pattern="#,##0"/> đ,
                                        thối <fmt:formatNumber value="${p.change}" pattern="#,##0"/> đ</small>
                                    </c:if>
                                </td>
                                <td class="text-end"><fmt:formatNumber value="${p.amount}" pattern="#,##0"/> đ</td>
                                <td>
                                    <c:choose>
                                        <c:when test="${p.status == 'PAID'}"><span class="badge bg-success">Đã thanh toán</span></c:when>
                                        <c:when test="${p.status == 'PENDING'}"><span class="badge bg-warning text-dark">Đang chờ</span></c:when>
                                        <c:when test="${p.status == 'FAILED'}"><span class="badge bg-danger">Thất bại</span></c:when>
                                        <c:when test="${p.status == 'TIMEOUT'}"><span class="badge bg-secondary">Hết hạn</span></c:when>
                                        <c:otherwise><span class="badge bg-secondary">Đã huỷ</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td><fmt:formatDate value="${p.createdAt}" pattern="HH:mm:ss dd/MM"/></td>
                                <td><fmt:formatDate value="${p.paidAt}" pattern="HH:mm:ss dd/MM"/></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<a href="${ctx}/booking" class="btn btn-link mt-3 no-print">Đặt vé khác</a>

<%@ include file="/WEB-INF/fragments/footer.jspf" %>
