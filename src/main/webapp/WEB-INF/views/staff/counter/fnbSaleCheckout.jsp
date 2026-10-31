<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%-- Ban bap nuoc le tai quay, buoc 2: thu tien (tien mat / VNPay). Can co: sale (FnbSaleDTO). --%>
<c:set var="pageTitle" value="Bán bắp nước - Thu tiền" scope="request"/>
<c:set var="pageStep" value="2" scope="request"/>
<%@ include file="/WEB-INF/fragments/header.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="row g-4 justify-content-center">
    <div class="col-lg-6">
        <div class="card">
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-start">
                    <h5>Đơn <c:out value="${sale.code}"/> <span class="badge bg-warning text-dark">Bắp nước lẻ</span></h5>
                    <c:if test="${sale.pending}">
                        <span class="badge bg-warning text-dark fs-6">Còn <span id="countdown">--:--</span></span>
                    </c:if>
                </div>
                <table class="table table-sm mb-3">
                    <c:forEach var="line" items="${sale.lines}">
                        <tr><th class="fw-normal"><c:out value="${line.name}"/> × ${line.quantity}</th>
                            <td class="text-end"><fmt:formatNumber value="${line.lineTotal}" pattern="#,##0"/> đ</td></tr>
                    </c:forEach>
                    <tr class="border-top"><th class="fs-5">Tổng tiền</th>
                        <td class="text-end fs-4 fw-bold text-danger"><fmt:formatNumber value="${sale.totalAmount}" pattern="#,##0"/> đ</td></tr>
                </table>

                <c:choose>
                    <c:when test="${sale.pending}">
                        <div class="border rounded p-3 mb-3">
                            <h6>💵 Thanh toán tiền mặt</h6>
                            <form method="post" action="${ctx}/staff/fnb-sale" id="cashForm">
                                <input type="hidden" name="action" value="cash">
                                <input type="hidden" name="orderId" value="${sale.id}">
                                <label class="form-label small" for="cashReceived">Tiền khách đưa (đ)</label>
                                <input type="text" inputmode="numeric" class="form-control form-control-lg mb-2"
                                       id="cashReceived" name="cashReceived" autocomplete="off" required>
                                <div class="d-flex flex-wrap gap-1 mb-2" id="quickCash"></div>
                                <div class="d-flex justify-content-between fs-5 mb-2">
                                    <span>Tiền thối</span><strong id="change">—</strong>
                                </div>
                                <button type="submit" class="btn btn-primary w-100" id="cashBtn" disabled>Xác nhận thu tiền mặt</button>
                            </form>
                        </div>
                        <form method="post" action="${ctx}/staff/fnb-sale" class="mb-2">
                            <input type="hidden" name="action" value="vnpay">
                            <input type="hidden" name="orderId" value="${sale.id}">
                            <button type="submit" class="btn btn-primary w-100">📱 Thanh toán qua VNPay (QR / thẻ)</button>
                            <div class="form-text">Mở trang VNPay cho khách quét mã QR bằng app ngân hàng;
                                VNPay xác nhận xong sẽ tự chuyển về trang hoá đơn.</div>
                        </form>
                        <form method="post" action="${ctx}/staff/fnb-sale" onsubmit="return confirm('Huỷ đơn bắp nước này?');">
                            <input type="hidden" name="action" value="cancel">
                            <input type="hidden" name="orderId" value="${sale.id}">
                            <button type="submit" class="btn btn-outline-danger w-100">Huỷ đơn</button>
                        </form>
                    </c:when>
                    <c:otherwise>
                        <div class="alert alert-secondary">Đơn này đã bị huỷ hoặc hết thời gian chờ thanh toán.</div>
                        <a href="${ctx}/staff/fnb-sale" class="btn btn-primary w-100">Tạo đơn mới</a>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</div>

<c:if test="${sale.pending}">
<script>
    (function () {
        let left = ${sale.secondsLeft};
        const el = document.getElementById('countdown');
        function tick() {
            if (left <= 0) { location.reload(); return; }
            el.textContent = String(Math.floor(left / 60)).padStart(2, '0') + ':' + String(left % 60).padStart(2, '0');
            left--;
        }
        tick();
        setInterval(tick, 1000);

        // Tien mat: tinh tien thoi + goi y menh gia
        const total = ${sale.totalAmount.longValue()};
        const fmt = new Intl.NumberFormat('vi-VN');
        const input = document.getElementById('cashReceived');
        const changeEl = document.getElementById('change');
        const btn = document.getElementById('cashBtn');

        function refresh() {
            const digits = input.value.replace(/[^0-9]/g, '');
            const received = digits ? Number(digits) : 0;
            input.value = digits ? fmt.format(received) : '';
            if (!digits) { changeEl.textContent = '—'; changeEl.className = ''; btn.disabled = true; return; }
            const change = received - total;
            changeEl.textContent = change >= 0 ? fmt.format(change) + ' đ' : 'Thiếu ' + fmt.format(-change) + ' đ';
            changeEl.className = change >= 0 ? 'text-success' : 'text-danger';
            btn.disabled = change < 0;
        }

        const suggestions = [total, ...[50000, 100000, 200000, 500000].map(n => Math.ceil(total / n) * n)]
            .filter((v, i, arr) => arr.indexOf(v) === i).slice(0, 5);
        const quick = document.getElementById('quickCash');
        suggestions.forEach(v => {
            const b = document.createElement('button');
            b.type = 'button';
            b.className = 'btn btn-outline-secondary btn-sm';
            b.textContent = fmt.format(v);
            b.onclick = () => { input.value = String(v); refresh(); };
            quick.appendChild(b);
        });

        input.addEventListener('input', refresh);
        document.getElementById('cashForm').addEventListener('submit', function (e) {
            if (!confirm('Thu ' + input.value + ' đ, thối lại ' + changeEl.textContent + '?')) e.preventDefault();
        });
    })();
</script>
</c:if>

<%@ include file="/WEB-INF/fragments/footer.jspf" %>
