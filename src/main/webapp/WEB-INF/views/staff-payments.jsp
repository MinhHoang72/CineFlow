<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="Theo dõi giao dịch thanh toán" scope="request"/>
<c:set var="pageStep" value="₫" scope="request"/>
<%@ include file="common/header.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<form method="get" action="${ctx}/staff/payments" class="row g-2 align-items-end mb-3">
    <div class="col-sm-3">
        <label class="form-label small" for="days">Thời gian</label>
        <select class="form-select" id="days" name="days" onchange="this.form.submit()">
            <option value="1" ${days == 1 ? 'selected' : ''}>24 giờ qua</option>
            <option value="7" ${days == 7 ? 'selected' : ''}>7 ngày qua</option>
            <option value="30" ${days == 30 ? 'selected' : ''}>30 ngày qua</option>
        </select>
    </div>
    <div class="col-sm-3">
        <label class="form-label small" for="method">Phương thức</label>
        <select class="form-select" id="method" name="method" onchange="this.form.submit()">
            <option value="">Tất cả</option>
            <option value="QR" ${method == 'QR' ? 'selected' : ''}>VNPay</option>
            <option value="CASH" ${method == 'CASH' ? 'selected' : ''}>Tiền mặt</option>
        </select>
    </div>
    <div class="col-sm-3">
        <label class="form-label small" for="status">Trạng thái</label>
        <select class="form-select" id="status" name="status" onchange="this.form.submit()">
            <option value="">Tất cả</option>
            <option value="PAID" ${status == 'PAID' ? 'selected' : ''}>Thành công</option>
            <option value="PENDING" ${status == 'PENDING' ? 'selected' : ''}>Đang chờ</option>
            <option value="FAILED" ${status == 'FAILED' ? 'selected' : ''}>Thất bại</option>
            <option value="TIMEOUT" ${status == 'TIMEOUT' ? 'selected' : ''}>Hết hạn</option>
            <option value="CANCELLED" ${status == 'CANCELLED' ? 'selected' : ''}>Đã huỷ</option>
        </select>
    </div>
    <div class="col-sm-3 text-sm-end">
        <a href="${ctx}/staff/payments?days=${days}&method=${method}&status=${status}" class="btn btn-outline-secondary">Làm mới</a>
    </div>
</form>

<div class="alert alert-light border d-flex flex-wrap gap-4">
    <span>Số giao dịch: <strong>${payments.size()}</strong></span>
    <span>Thành công: <strong>${paidCount}</strong></span>
    <span>Tổng tiền đã thu: <strong class="text-success"><fmt:formatNumber value="${paidTotal}" pattern="#,##0"/> đ</strong></span>
</div>

<c:choose>
    <c:when test="${empty payments}">
        <div class="alert alert-info">Không có giao dịch nào phù hợp.</div>
    </c:when>
    <c:otherwise>
        <div class="card">
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light">
                    <tr>
                        <th>Thời gian</th><th>Mã giao dịch / Đơn</th><th>Khách hàng</th><th>Phim / Rạp</th>
                        <th>Phương thức</th><th class="text-end">Số tiền</th><th>Trạng thái</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="p" items="${payments}">
                        <tr>
                            <td>
                                <fmt:formatDate value="${p.createdAt}" pattern="HH:mm:ss dd/MM"/>
                                <c:if test="${not empty p.paidAt}">
                                    <br><small class="text-muted">Thu lúc <fmt:formatDate value="${p.paidAt}" pattern="HH:mm:ss"/></small>
                                </c:if>
                            </td>
                            <td>
                                <small class="text-muted"><c:out value="${p.transactionCode}"/></small><br>
                                <c:choose>
                                    <c:when test="${p.counter}"><strong><c:out value="${p.bookingCode}"/></strong></c:when>
                                    <c:otherwise>
                                        <a href="${ctx}/booking?action=detail&bookingId=${p.bookingId}"><strong><c:out value="${p.bookingCode}"/></strong></a>
                                    </c:otherwise>
                                </c:choose>
                                <span class="badge ${p.counter ? 'bg-info text-dark' : 'bg-light text-dark border'}">${p.counter ? 'Tại quầy' : 'Online'}</span>
                            </td>
                            <td><c:out value="${p.customerName}" default="Khách tại quầy"/><br><small class="text-muted"><c:out value="${p.customerPhone}"/></small></td>
                            <td><c:out value="${p.movieTitle}"/><br><small class="text-muted"><c:out value="${p.branchName}"/></small></td>
                            <td>${p.methodLabel}</td>
                            <td class="text-end fw-bold"><fmt:formatNumber value="${p.amount}" pattern="#,##0"/> đ</td>
                            <td><span class="badge ${p.statusBadge}">${p.statusLabel}</span></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </div>
        <p class="small text-muted mt-2">Giao dịch VNPay được xác nhận tự động khi VNPay báo kết quả; tiền mặt được ghi
            nhận khi nhân viên quầy thu tiền. Trang chỉ để theo dõi, không cần thao tác xác nhận.</p>
    </c:otherwise>
</c:choose>

<script>setTimeout(() => location.reload(), 30000);</script>

<%@ include file="common/footer.jspf" %>
