<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="Xác nhận chuyển khoản" scope="request"/>
<c:set var="pageStep" value="✔" scope="request"/>
<%@ include file="common/header.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="d-flex justify-content-between align-items-center mb-3">
    <p class="text-muted mb-0">Đối chiếu <strong>số tiền</strong> và <strong>nội dung (mã đơn)</strong> với lịch sử
        nhận tiền trong app ngân hàng trước khi xác nhận.</p>
    <a href="${ctx}/staff/payments" class="btn btn-outline-secondary btn-sm">Làm mới</a>
</div>

<c:choose>
    <c:when test="${empty payments}">
        <div class="alert alert-info">Không có giao dịch nào đang chờ xác nhận.</div>
    </c:when>
    <c:otherwise>
        <div class="card">
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light">
                    <tr>
                        <th>Mã đơn (nội dung CK)</th><th>Khách hàng</th><th>Phim / Rạp</th>
                        <th class="text-end">Số tiền</th><th>Trạng thái</th><th></th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="p" items="${payments}">
                        <tr>
                            <td>
                                <strong><c:out value="${p.bookingCode}"/></strong><br>
                                <small class="text-muted"><fmt:formatDate value="${p.createdAt}" pattern="HH:mm:ss dd/MM"/></small>
                            </td>
                            <td><c:out value="${p.customerName}" default="Khách tại quầy"/><br><small class="text-muted"><c:out value="${p.customerPhone}"/></small></td>
                            <td><c:out value="${p.movieTitle}"/><br><small class="text-muted"><c:out value="${p.branchName}"/></small></td>
                            <td class="text-end fw-bold"><fmt:formatNumber value="${p.amount}" pattern="#,##0"/> đ</td>
                            <td>
                                <c:choose>
                                    <c:when test="${p.status == 'PENDING'}">
                                        <span class="badge bg-warning text-dark">Đang giữ ghế</span><br>
                                        <small class="text-muted">đến <fmt:formatDate value="${p.holdUntil}" pattern="HH:mm:ss"/></small>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="badge bg-secondary">Hết hạn giữ ghế</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td class="text-end">
                                <form method="post" action="${ctx}/staff/payments"
                                      onsubmit="return confirm('Xác nhận đã nhận đủ tiền cho đơn ${p.bookingCode}?');">
                                    <input type="hidden" name="paymentId" value="${p.id}">
                                    <input type="hidden" name="bookingCode" value="${p.bookingCode}">
                                    <button type="submit" class="btn btn-success btn-sm">Xác nhận đã nhận tiền</button>
                                </form>
                                <c:if test="${p.status == 'PENDING'}">
                                    <form method="post" action="${ctx}/staff/payments" class="mt-1"
                                          onsubmit="return confirm('Đánh dấu KHÔNG nhận được tiền cho đơn ${p.bookingCode}?');">
                                        <input type="hidden" name="action" value="reject">
                                        <input type="hidden" name="paymentId" value="${p.id}">
                                        <input type="hidden" name="bookingCode" value="${p.bookingCode}">
                                        <button type="submit" class="btn btn-outline-danger btn-sm">Không nhận được tiền</button>
                                    </form>
                                </c:if>
                                <a href="${ctx}/booking?action=detail&bookingId=${p.bookingId}" class="small d-block mt-1">Chi tiết đơn</a>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </div>
        <p class="small text-muted mt-2">Đơn "Hết hạn giữ ghế" vẫn xác nhận được nếu khách chuyển muộn: hệ thống
            giữ lại ghế nếu ghế còn trống, nếu ghế đã có người khác đặt thì đơn bị huỷ và cần hoàn tiền.</p>
    </c:otherwise>
</c:choose>

<script>setTimeout(() => location.reload(), 15000);</script>

<%@ include file="common/footer.jspf" %>
