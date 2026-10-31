<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%-- Ban bap nuoc le tai quay (khong kem ve), buoc 1: chon mon. Can co: fnbMenu (List<FnbMenuItemDTO>). --%>
<c:set var="pageTitle" value="Bán bắp nước tại quầy" scope="request"/>
<c:set var="pageStep" value="1" scope="request"/>
<%@ include file="/WEB-INF/fragments/header.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>

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

<c:choose>
    <c:when test="${empty fnbMenu}">
        <div class="alert alert-secondary">Rạp hiện chưa có món nào đang bán (hết hàng hoặc chưa cấu hình kho).</div>
    </c:when>
    <c:otherwise>
        <form method="post" action="${ctx}/staff/fnb-sale" id="fnbForm">
            <input type="hidden" name="action" value="create">
            <div class="row g-4">
                <div class="col-lg-8">
                    <div class="card"><div class="card-body">
                        <h5 class="mb-1">🍿 Bắp nước bán lẻ</h5>
                        <p class="text-muted small mb-2">Bán cho khách không mua vé. Chọn món rồi bấm <strong>Tạo đơn &amp; thu tiền</strong>.</p>
                        <c:forEach var="m" items="${fnbMenu}">
                            <div class="fnb-item">
                                <div class="fnb-info">
                                    <strong><c:out value="${m.name}"/></strong>
                                    <c:if test="${m.combo}"><span class="badge bg-info text-dark">Combo</span></c:if>
                                    <c:if test="${not empty m.description}">
                                        <div class="small text-muted"><c:out value="${m.description}"/></div>
                                    </c:if>
                                    <div class="small"><fmt:formatNumber value="${m.price}" pattern="#,##0"/> đ
                                        <span class="text-muted">· còn ${m.available}</span></div>
                                </div>
                                <div class="fnb-stepper">
                                    <button type="button" class="btn btn-outline-secondary btn-sm" data-step="-1" aria-label="Bớt ${m.name}">−</button>
                                    <input type="number" class="form-control form-control-sm fnb-qty" name="qty_${m.key}"
                                           min="0" max="${m.maxQuantity}" value="0" data-price="${m.price}" data-name="${m.name}"
                                           aria-label="Số lượng ${m.name}">
                                    <button type="button" class="btn btn-outline-secondary btn-sm" data-step="1" aria-label="Thêm ${m.name}">+</button>
                                </div>
                            </div>
                        </c:forEach>
                    </div></div>
                </div>

                <div class="col-lg-4">
                    <div class="card"><div class="card-body">
                        <h6>Đơn đang chọn</h6>
                        <ul class="list-unstyled small mb-2" id="fnbLines"><li class="text-muted">Chưa chọn món nào.</li></ul>
                        <div class="d-flex justify-content-between fs-5 border-top pt-2 mb-3">
                            <span>Tổng tiền</span><strong><span id="fnbTotal">0</span> đ</strong>
                        </div>
                        <button type="submit" class="btn btn-primary btn-lg w-100" id="createBtn" disabled>Tạo đơn &amp; thu tiền</button>
                        <p class="small text-muted mt-2 mb-0">Đơn chưa thu tiền sẽ tự huỷ sau 15 phút.</p>
                    </div></div>
                </div>
            </div>
        </form>

        <script>
            (function () {
                const fmt = new Intl.NumberFormat('vi-VN');
                const inputs = document.querySelectorAll('.fnb-qty');

                function clamp(input) {
                    const max = Number(input.max), v = parseInt(input.value, 10);
                    input.value = isNaN(v) || v < 0 ? 0 : Math.min(v, max);
                }

                function refresh() {
                    let total = 0, count = 0;
                    const lines = document.getElementById('fnbLines');
                    lines.innerHTML = '';
                    inputs.forEach(i => {
                        const q = Number(i.value) || 0;
                        if (q <= 0) return;
                        total += q * Number(i.dataset.price);
                        count += q;
                        const li = document.createElement('li');
                        li.textContent = i.dataset.name + ' × ' + q;
                        lines.appendChild(li);
                    });
                    if (!count) lines.innerHTML = '<li class="text-muted">Chưa chọn món nào.</li>';
                    document.getElementById('fnbTotal').textContent = fmt.format(total);
                    document.getElementById('createBtn').disabled = count === 0;
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
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/fragments/footer.jspf" %>
