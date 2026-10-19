<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:if test="${empty pageTitle}"><c:set var="pageTitle" value="Chọn ghế" scope="request"/></c:if>
<c:set var="pageStep" value="2" scope="request"/>
<%@ include file="common/header.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<%-- Ban tai quay truyen seatFormAction/seatBackUrl; dat ve online dung mac dinh --%>
<c:if test="${empty seatFormAction}"><c:set var="seatFormAction" value="${ctx}/booking"/></c:if>
<c:if test="${empty seatBackUrl}">
    <c:set var="seatBackUrl" value="${ctx}/booking?branchId=${showtime.branchId}&movieId=${showtime.movieId}"/>
</c:if>

<style>
    .screen { background: #333; color: #fff; text-align: center; border-radius: 4px; padding: 4px; margin-bottom: 20px; }
    .seat-row { display: flex; align-items: center; justify-content: center; gap: 6px; margin-bottom: 6px; }
    .row-label { width: 24px; font-weight: bold; text-align: center; }
    .seat input { display: none; }
    .seat span { display: inline-block; width: 40px; height: 34px; line-height: 34px; text-align: center;
                 border-radius: 6px; font-size: 12px; cursor: pointer; border: 1px solid #bbb; background: #fff; }
    .seat.VIP span { border-color: #f0ad4e; background: #fff6e5; }
    .seat.SWEETBOX span { border-color: #e83e8c; background: #fde8f1; width: 56px; }
    .seat input:checked + span { background: #198754; border-color: #198754; color: #fff; }
    .seat input:disabled + span { background: #adb5bd; border-color: #adb5bd; color: #fff; cursor: not-allowed; }
    .legend span { display: inline-block; width: 18px; height: 18px; border-radius: 4px; vertical-align: middle;
                   margin-right: 4px; border: 1px solid #bbb; }
</style>

<div class="card mb-4">
    <div class="card-body">
        <h5 class="mb-1"><c:out value="${showtime.movieTitle}"/></h5>
        <div class="text-muted">
            <c:out value="${showtime.branchName}"/> · <c:out value="${showtime.hallName}"/> (${showtime.hallType}) ·
            <fmt:formatDate value="${showtime.startTime}" pattern="HH:mm dd/MM/yyyy"/>
        </div>
    </div>
</div>

<form method="post" action="${seatFormAction}" id="seatForm">
    <input type="hidden" name="action" value="hold">
    <input type="hidden" name="showtimeId" value="${showtime.id}">

    <div class="row g-4">
        <div class="col-lg-8">
            <div class="card"><div class="card-body">
                <div class="screen">MÀN HÌNH</div>
                <c:forEach var="row" items="${seatRows}">
                    <div class="seat-row">
                        <span class="row-label">${row.key}</span>
                        <c:forEach var="seat" items="${row.value}">
                            <label class="seat ${seat.type}" title="${seat.label} - ${seat.type}">
                                <input type="checkbox" name="seatId" value="${seat.id}"
                                       data-label="${seat.label}" data-price="${seat.price}"
                                       ${seat.selectable ? '' : 'disabled'}>
                                <span>${seat.label}</span>
                            </label>
                        </c:forEach>
                    </div>
                </c:forEach>
                <div class="legend d-flex flex-wrap gap-3 justify-content-center mt-3 small">
                    <div><span></span>Thường</div>
                    <div><span style="background:#fff6e5;border-color:#f0ad4e"></span>VIP</div>
                    <div><span style="background:#fde8f1;border-color:#e83e8c"></span>Sweetbox</div>
                    <div><span style="background:#198754;border-color:#198754"></span>Đang chọn</div>
                    <div><span style="background:#adb5bd;border-color:#adb5bd"></span>Đã đặt</div>
                </div>
            </div></div>
        </div>

        <div class="col-lg-4">
            <div class="card"><div class="card-body">
                <h6>Giá vé</h6>
                <ul class="list-unstyled small mb-3">
                    <c:forEach var="p" items="${priceByType}">
                        <li>${p.key}: <fmt:formatNumber value="${p.value}" pattern="#,##0"/> đ</li>
                    </c:forEach>
                </ul>
                <div>Ghế đã chọn: <strong id="selectedLabels">—</strong></div>
                <div class="fs-5 my-2">Tổng: <strong id="total">0</strong> đ</div>
                <p class="small text-muted">Tối đa ${maxSeats} ghế. Ghế sẽ được giữ ${holdMinutes} phút để bạn thanh toán.</p>
                <button type="submit" class="btn btn-success w-100" id="submitBtn" disabled>Tiếp tục</button>
                <a href="${seatBackUrl}" class="btn btn-link w-100">← Chọn suất khác</a>
            </div></div>
        </div>
    </div>
</form>

<script>
    (function () {
        const maxSeats = ${maxSeats};
        const boxes = document.querySelectorAll('input[name="seatId"]');
        const fmt = new Intl.NumberFormat('vi-VN');

        function refresh() {
            const checked = [...boxes].filter(b => b.checked);
            const total = checked.reduce((sum, b) => sum + Number(b.dataset.price), 0);
            document.getElementById('selectedLabels').textContent =
                checked.length ? checked.map(b => b.dataset.label).join(', ') : '—';
            document.getElementById('total').textContent = fmt.format(total);
            document.getElementById('submitBtn').disabled = checked.length === 0;
        }

        boxes.forEach(b => b.addEventListener('change', function () {
            if (this.checked && [...boxes].filter(x => x.checked).length > maxSeats) {
                this.checked = false;
                alert('Mỗi lần chỉ được chọn tối đa ' + maxSeats + ' ghế.');
            }
            refresh();
        }));
    })();
</script>

<%@ include file="common/footer.jspf" %>
