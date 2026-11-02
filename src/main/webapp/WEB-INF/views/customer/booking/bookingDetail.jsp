<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="Chi tiết đơn" scope="request"/>
<c:set var="pageStep" value="📄" scope="request"/>
<%@ include file="/WEB-INF/fragments/header.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="st" value="${booking.showtime}"/>
<c:set var="isCancelled" value="${booking.status == 'CANCELLED' or (not empty st and st.status == 'CANCELLED')}"/>

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

<c:if test="${isCancelled}">
    <div class="alert alert-danger border-danger shadow-sm mb-4" role="alert" style="border-left: 5px solid #dc3545 !important; background-color: #fff5f5;">
        <div class="d-flex align-items-start gap-3">
            <div class="fs-2 text-danger" style="line-height: 1;">⚠️</div>
            <div class="flex-grow-1">
                <h5 class="alert-heading fw-bold mb-1 text-danger">Thông báo: Suất chiếu này đã bị huỷ</h5>
                <p class="mb-2 text-dark">
                    Rất tiếc, suất chiếu phim <strong><c:out value="${st.movieTitle}"/></strong> vào lúc 
                    <strong><fmt:formatDate value="${st.startTime}" pattern="HH:mm - dd/MM/yyyy"/></strong> 
                    tại <strong><c:out value="${st.centreName}"/></strong> đã bị huỷ vì lý do kỹ thuật hoặc sự cố vận hành bất khả kháng.
                </p>
                <div class="p-3 bg-white rounded border border-danger-subtle mb-2">
                    <div class="fw-semibold text-danger mb-1">Quyền lợi của quý khách được bảo lưu đầy đủ:</div>
                    <div class="text-secondary small mb-2">
                        Mã đơn đặt vé <code><c:out value="${booking.code}"/></code> của quý khách vẫn được lưu giữ an toàn trên hệ thống để đối soát quyền lợi.
                    </div>
                    <div class="fw-semibold text-dark small mb-1">Quý khách vui lòng chọn một trong các hình thức hỗ trợ sau:</div>
                    <ul class="mb-0 small text-secondary ps-3">
                        <li class="mb-1"><strong>Đổi vé tại quầy:</strong> Mang mã đơn hoặc vé điện tử này đến trực tiếp Quầy vé tại <strong><c:out value="${st.centreName}"/></strong> để nhân viên hỗ trợ đổi sang suất chiếu khác hoặc phim khác hoàn toàn miễn phí.</li>
                        <li class="mb-1"><strong>Liên hệ Tổng đài / Hotline:</strong> Gọi đến tổng đài hỗ trợ <strong>1900 6868</strong> để được nhân viên chăm sóc khách hàng hướng dẫn giải quyết.</li>
                        <li><strong>Hỗ trợ qua Email:</strong> Gửi thông tin mã đơn đến email <strong>cskh@cineflow.vn</strong> hoặc <strong>support@cineflow.vn</strong> để nhận voucher vé xem phim bù đắp quyền lợi.</li>
                    </ul>
                </div>
                <div class="small text-muted fst-italic">
                    * Lưu ý: Hệ thống không tự động hoàn tiền trực tuyến. Mọi quyền lợi đổi vé hoặc nhận voucher đền bù sẽ được giải quyết trực tiếp tại quầy rạp hoặc qua các kênh chăm sóc khách hàng ở trên.
                </div>
            </div>
        </div>
    </div>
</c:if>

<c:choose>
    <c:when test="${not empty tickets}">
        <h6>Vé điện tử</h6>
        <%@ include file="/WEB-INF/views/customer/booking/ticket.jspf" %>
    </c:when>
    <c:otherwise>
        <div class="card mb-3"><div class="card-body">
            <div class="fs-5 fw-bold"><c:out value="${st.movieTitle}"/></div>
            <div><c:out value="${st.centreName}"/> · <c:out value="${st.hallName}"/> (${st.hallType}) ·
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

