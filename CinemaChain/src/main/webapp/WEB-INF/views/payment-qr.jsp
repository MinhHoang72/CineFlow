<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="Thanh toán chuyển khoản" scope="request"/>
<c:set var="pageStep" value="4" scope="request"/>
<%@ include file="common/header.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="row justify-content-center">
    <div class="col-lg-8">
        <div class="card">
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-start mb-3">
                    <h5 class="mb-0">Đơn <c:out value="${booking.code}"/></h5>
                    <span class="badge bg-warning text-dark fs-6">Giữ ghế còn <span id="countdown">--:--</span></span>
                </div>

                <div class="row g-4 align-items-center">
                    <div class="col-md-5 text-center">
                        <img src="${qrImage}" alt="Mã QR chuyển khoản" class="img-fluid border rounded p-2">
                        <div class="small text-muted mt-2">Quét bằng app ngân hàng bất kỳ</div>
                    </div>
                    <div class="col-md-7">
                        <table class="table table-sm mb-3">
                            <tr><th>Ngân hàng</th><td><c:out value="${bankName}"/></td></tr>
                            <tr><th>Số tài khoản</th><td class="fw-bold"><c:out value="${accountNo}"/></td></tr>
                            <tr><th>Chủ tài khoản</th><td><c:out value="${accountName}"/></td></tr>
                            <tr><th>Số tiền</th>
                                <td class="fw-bold text-danger fs-5"><fmt:formatNumber value="${booking.totalAmount}" pattern="#,##0"/> đ</td></tr>
                            <tr><th>Nội dung</th><td class="fw-bold"><c:out value="${booking.code}"/></td></tr>
                        </table>
                        <div class="alert alert-info small mb-3">
                            Giữ nguyên <strong>số tiền</strong> và <strong>nội dung</strong> khi chuyển khoản.
                            Sau khi nhân viên xác nhận đã nhận tiền, trang này sẽ tự chuyển sang vé điện tử.
                        </div>
                        <button type="button" class="btn btn-success w-100" id="paidBtn">Tôi đã chuyển khoản</button>
                        <div id="waiting" class="text-center text-muted small mt-2 d-none">
                            <span class="spinner-border spinner-border-sm"></span> Đang chờ nhân viên xác nhận...
                        </div>
                    </div>
                </div>

                <div class="small text-muted mt-3">
                    <c:out value="${booking.showtime.movieTitle}"/> ·
                    <fmt:formatDate value="${booking.showtime.startTime}" pattern="HH:mm dd/MM/yyyy"/> ·
                    Ghế ${booking.seatLabels}
                </div>
            </div>
        </div>
        <a href="${ctx}/booking?action=checkout&bookingId=${booking.id}" class="btn btn-link mt-2">← Quay lại đơn hàng</a>
    </div>
</div>

<script>
    (function () {
        const statusUrl = '${ctx}/payment/status?bookingId=${booking.id}';
        const resultUrl = '${ctx}/booking?action=detail&bookingId=${booking.id}';
        const failedUrl = '${ctx}/payment/failed?bookingId=${booking.id}';
        let left = ${booking.secondsLeft};
        const el = document.getElementById('countdown');

        function tick() {
            if (left <= 0) { location.href = failedUrl; return; }
            el.textContent = String(Math.floor(left / 60)).padStart(2, '0') + ':' + String(left % 60).padStart(2, '0');
            left--;
        }

        function check() {
            fetch(statusUrl, {cache: 'no-store'})
                .then(r => {
                    if (r.redirected || !r.ok) { location.reload(); throw new Error('session'); }
                    return r.json();
                })
                .then(s => {
                    if (s.status === 'CONFIRMED') location.href = resultUrl;
                    else if (s.status !== 'PENDING' || s.paymentStatus === 'FAILED') location.href = failedUrl;
                    else left = s.secondsLeft;
                })
                .catch(() => {});
        }

        document.getElementById('paidBtn').addEventListener('click', function () {
            this.classList.add('d-none');
            document.getElementById('waiting').classList.remove('d-none');
            check();
        });

        tick();
        setInterval(tick, 1000);
        setInterval(check, 5000);
    })();
</script>

<%@ include file="common/footer.jspf" %>
