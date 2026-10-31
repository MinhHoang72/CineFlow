<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%-- Buoc 3: chon bap nuoc cho don dang giu ghe (dat ve online va ban tai quay).
     Can co: booking, fnbMenu (List<FnbMenuItemDTO>), extrasAction (URL POST: /booking hoac /staff/counter). --%>
<c:if test="${empty pageTitle}"><c:set var="pageTitle" value="Chọn bắp nước" scope="request"/></c:if>
<c:set var="pageStep" value="3" scope="request"/>
<%@ include file="/WEB-INF/fragments/header.jspf" %>
<c:set var="st" value="${booking.showtime}"/>
<c:set var="fnbQty" value="${booking.fnbQuantities}"/>

<style>
    .fnb-item { display: flex; align-items: center; gap: 12px; padding: 12px 0; border-bottom: 1px solid var(--bs-border-color); }
    .fnb-item:last-child { border-bottom: 0; }
    .fnb-item .fnb-info { flex: 1; min-width: 0; }
    .fnb-stepper { display: flex; align-items: center; gap: 6px; }
    .fnb-stepper input { width: 52px; text-align: center; }
    .fnb-stepper input::-webkit-outer-spin-button,
    .fnb-stepper input::-webkit-inner-spin-button { -webkit-appearance: none; margin: 0; }
    .fnb-stepper input[type=number] { -moz-appearance: textfield; }
</style>

<form method="post" action="${extrasAction}" id="fnbForm">
    <input type="hidden" name="action" value="save-fnb">
    <input type="hidden" name="bookingId" value="${booking.id}">

    <div class="row g-4">
        <div class="col-lg-8">
            <div class="card"><div class="card-body">
                <h5 class="mb-1">🍿 Bắp nước</h5>
                <p class="text-muted small mb-2">Không bắt buộc. Bấm <strong>Tiếp tục</strong> để sang bước mã giảm giá và thanh toán.</p>
                <c:forEach var="m" items="${fnbMenu}">
                    <div class="fnb-item">
                        <div class="fnb-info">
                            <strong><c:out value="${m.name}"/></strong>
                            <c:if test="${m.combo}"><span class="badge bg-info text-dark">Combo</span></c:if>
                            <c:if test="${not empty m.description}">
                                <div class="small text-muted"><c:out value="${m.description}"/></div>
                            </c:if>
                            <div class="small"><fmt:formatNumber value="${m.price}" pattern="#,##0"/> đ</div>
                        </div>
                        <div class="fnb-stepper">
                            <button type="button" class="btn btn-outline-secondary btn-sm" data-step="-1" aria-label="Bớt ${m.name}">−</button>
                            <input type="number" class="form-control form-control-sm fnb-qty" name="qty_${m.key}"
                                   min="0" max="${m.maxQuantity}" data-price="${m.price}" data-name="${m.name}"
                                   value="${empty fnbQty[m.key] ? 0 : fnbQty[m.key]}" aria-label="Số lượng ${m.name}">
                            <button type="button" class="btn btn-outline-secondary btn-sm" data-step="1" aria-label="Thêm ${m.name}">+</button>
                        </div>
                    </div>
                </c:forEach>
            </div></div>
        </div>

        <div class="col-lg-4">
            <div class="card"><div class="card-body">
                <div class="d-flex justify-content-between align-items-start mb-2">
                    <h6 class="mb-0">Đơn <c:out value="${booking.code}"/></h6>
                    <span class="badge bg-warning text-dark">Giữ ghế còn <span id="countdown">--:--</span></span>
                </div>
                <div class="small text-muted mb-2">
                    <c:out value="${st.movieTitle}"/><br>
                    <c:out value="${st.centreName}"/> · <c:out value="${st.hallName}"/> ·
                    <fmt:formatDate value="${st.startTime}" pattern="HH:mm dd/MM/yyyy"/>
                </div>
                <table class="table table-sm mb-2">
                    <tr><th class="fw-normal">Ghế
                            <c:forEach var="seat" items="${booking.seats}" varStatus="s">${seat.label}${s.last ? '' : ', '}</c:forEach></th>
                        <td class="text-end"><fmt:formatNumber value="${booking.subTotal}" pattern="#,##0"/> đ</td></tr>
                    <c:if test="${booking.discountAmount > 0}">
                        <tr><th class="fw-normal">Giảm giá (<c:out value="${booking.voucherCode}"/>)</th>
                            <td class="text-end text-success">− <fmt:formatNumber value="${booking.discountAmount}" pattern="#,##0"/> đ</td></tr>
                    </c:if>
                    <tr><th class="fw-normal">Bắp nước</th>
                        <td class="text-end"><span id="fnbTotal">0</span> đ</td></tr>
                    <tr class="border-top"><th>Tạm tính</th>
                        <td class="text-end fw-bold"><span id="grandTotal">0</span> đ</td></tr>
                </table>
                <ul class="list-unstyled small mb-3" id="fnbLines"></ul>
                <button type="submit" class="btn btn-primary btn-lg w-100">Tiếp tục</button>
                <button type="submit" form="cancelForm" class="btn btn-link w-100">← Huỷ đơn, chọn lại ghế</button>
            </div></div>
        </div>
    </div>
</form>
<form method="post" action="${extrasAction}" id="cancelForm" onsubmit="return confirm('Huỷ đơn và nhả ghế?');">
    <input type="hidden" name="action" value="cancel">
    <input type="hidden" name="bookingId" value="${booking.id}">
</form>

<script>
    (function () {
        let left = ${booking.secondsLeft};
        const el = document.getElementById('countdown');
        function tick() {
            if (left <= 0) { location.href = '${extrasAction}?action=checkout&bookingId=${booking.id}'; return; }
            el.textContent = String(Math.floor(left / 60)).padStart(2, '0') + ':' + String(left % 60).padStart(2, '0');
            left--;
        }
        tick();
        setInterval(tick, 1000);

        const ticketTotal = ${booking.subTotal.longValue() - booking.discountAmount.longValue()};
        const fmt = new Intl.NumberFormat('vi-VN');
        const inputs = document.querySelectorAll('.fnb-qty');

        function clamp(input) {
            const max = Number(input.max), v = parseInt(input.value, 10);
            input.value = isNaN(v) || v < 0 ? 0 : Math.min(v, max);
        }

        function refresh() {
            let fnb = 0;
            const lines = document.getElementById('fnbLines');
            lines.innerHTML = '';
            inputs.forEach(i => {
                const q = Number(i.value) || 0;
                if (q <= 0) return;
                fnb += q * Number(i.dataset.price);
                const li = document.createElement('li');
                li.textContent = i.dataset.name + ' × ' + q;
                lines.appendChild(li);
            });
            document.getElementById('fnbTotal').textContent = fmt.format(fnb);
            document.getElementById('grandTotal').textContent = fmt.format(ticketTotal + fnb);
        }

        document.querySelectorAll('.fnb-stepper button').forEach(b => b.addEventListener('click', function () {
            const input = this.parentElement.querySelector('input');
            input.value = (Number(input.value) || 0) + Number(this.dataset.step);
            clamp(input);
            refresh();
        }));
        inputs.forEach(i => i.addEventListener('input', refresh));
        inputs.forEach(i => i.addEventListener('change', function () { clamp(this); refresh(); }));
        refresh();
    })();
</script>

<%@ include file="/WEB-INF/fragments/footer.jspf" %>
