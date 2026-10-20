<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%
    request.setAttribute("pageTitle", "Thanh toán thành công");
%>
<%@ include file="/WEB-INF/fragments/header.jspf" %>
<div class="container my-5">
    <div class="row justify-content-center">
        <div class="col-md-8 col-lg-6">
            <div class="card shadow-sm border-0 text-center p-4">
                <div class="mb-3">
                    <span class="display-1 text-success">&#10004;</span>
                </div>
                <h2 class="card-title text-success fw-bold">Thanh toán thành công!</h2>
                <p class="text-muted">Cảm ơn bạn đã lựa chọn CineFlow. Vé điện tử đã được phát hành.</p>
                <div class="card bg-light border-0 my-3 text-start p-3">
                    <div class="d-flex justify-content-between py-1">
                        <span class="text-muted">Mã đơn đặt vé:</span>
                        <strong class="font-monospace">#${booking.bookingId}</strong>
                    </div>
                    <div class="d-flex justify-content-between py-1">
                        <span class="text-muted">Trạng thái:</span>
                        <span class="badge bg-success">ĐÃ THANH TOÁN</span>
                    </div>
                    <div class="d-flex justify-content-between py-1">
                        <span class="text-muted">Tổng tiền:</span>
                        <strong class="text-danger"><fmt:formatNumber value="${booking.totalAmount}" type="currency" currencySymbol="đ"/></strong>
                    </div>
                </div>
                <div class="d-grid gap-2 d-sm-flex justify-content-sm-center mt-3">
                    <a href="${pageContext.request.contextPath}/booking?action=detail&id=${booking.bookingId}" class="btn btn-primary px-4">Xem vé điện tử</a>
                    <a href="${pageContext.request.contextPath}/home/index.jsp" class="btn btn-outline-secondary px-4">Về trang chủ</a>
                </div>
            </div>
        </div>
    </div>
</div>
<%@ include file="/WEB-INF/fragments/footer.jspf" %>
