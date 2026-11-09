<%@ page pageEncoding="UTF-8" %>
<%
    request.setAttribute("pageTitle", "Giá vé cơ bản");
    request.setAttribute("pageDescription", "");
    request.setAttribute("activeTab", "base");
    request.setAttribute("pageClass", "bp-page");
%>
<%@ include file="/WEB-INF/fragments/cinemaBegin.jspf" %>
<div class="bp-overview">
    <c:if test="${sessionScope.role == 'ADMIN'}">
        <a class="st-button st-button-muted bp-back" href="${pageContext.request.contextPath}/pricing">← Bảng giá các rạp</a>
    </c:if>
    <section class="bp-summary" aria-label="Giá cơ bản hiện tại">
        <div class="bp-current">
            <div class="bp-centre"><span class="bp-centre-icon" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M3 21h18M5 21V7l7-4 7 4v14M9 10h2m2 0h2m-6 4h2m2 0h2m-5 7v-4h4v4"/></svg></span><h2><c:out value="${centreName}" /></h2></div>
            <span class="bp-label">Giá cơ bản hiện tại</span>
            <div class="bp-price-line"><strong class="bp-price"><c:out value="${empty currentPrice ? 'Chưa thiết lập' : currentPrice.basePriceFormatted}" /></strong>
                <c:if test="${not empty currentPrice}"><span class="bp-badge bp-badge-current">Đang áp dụng</span></c:if>
            </div>
        </div>
        <dl class="bp-metadata">
            <div><dt>Hiệu lực từ</dt><dd><c:out value="${empty currentPrice ? '—' : currentPrice.effectiveFromFormatted}" /></dd></div>
            <div><dt>Người cập nhật</dt><dd><c:out value="${empty currentPrice.updatedByName ? '—' : currentPrice.updatedByName}" /></dd></div>
        </dl>
    </section>
    <c:if test="${not empty nextPrice}">
        <section class="bp-upcoming" aria-label="Giá sắp áp dụng"><span class="bp-badge bp-badge-scheduled">Đã lên lịch</span><strong><c:out value="${nextPrice.basePriceFormatted}" /></strong><span>Từ <c:out value="${nextPrice.effectiveFromFormatted}" /></span></section>
    </c:if>
    <c:if test="${sessionScope.role == 'ADMIN'}">
        <section class="st-panel bp-admin-editor"><div class="st-panel-heading"><h2>Cập nhật giá cơ bản</h2></div>
        <form class="st-create-form" action="${pageContext.request.contextPath}/pricing/update" method="post">
            <input type="hidden" name="centreId" value="${centreId}">
            <div class="st-field">
                <label for="base-price">Giá cơ bản mới (VNĐ) <span>*</span></label>
                <input id="base-price" type="text" inputmode="numeric" data-money name="basePrice" required placeholder="Ví dụ: 80.000">
            </div>
            <div class="st-field">
                <label for="pr-effective-mode">Bắt đầu áp dụng</label>
                <select id="pr-effective-mode" name="effectiveMode" aria-controls="pr-effective-field" aria-expanded="false"><option value="now">Ngay bây giờ</option><option value="scheduled">Chọn ngày / giờ</option></select>
            </div>
                <div class="st-field" id="pr-effective-field" hidden>
                    <label for="pr-effective-from">Ngày và giờ áp dụng <span>*</span></label>
                    <input id="pr-effective-from" type="datetime-local" name="effectiveFrom" disabled>

                </div>
            <div class="st-form-footer">
                <button class="st-button st-button-primary" id="pr-save-price" type="submit">Cập nhật giá</button>
            </div>
        </form>
        </section>
    </c:if>
    <section class="st-panel bp-history">
        <div class="st-panel-heading"><h2>Lịch sử điều chỉnh <span class="st-count" id="bp-history-count" aria-live="polite">${fn:length(history)}</span></h2></div>
        <div class="bp-history-filters" role="search" aria-label="Lọc lịch sử giá cơ bản">
            <div class="st-field"><label for="bp-history-from">Áp dụng từ ngày</label><input id="bp-history-from" type="date"></div>
            <div class="st-field"><label for="bp-history-to">Đến ngày</label><input id="bp-history-to" type="date"></div>
            <div class="st-field"><label for="bp-history-updater">Người cập nhật</label><select id="bp-history-updater"><option value="">Tất cả</option></select></div>
            <button class="st-button st-button-muted" type="button" id="bp-history-reset">Xóa bộ lọc</button>
            <p id="bp-history-filter-error" class="bp-filter-error" role="alert" hidden>Ngày kết thúc phải bằng hoặc sau ngày bắt đầu.</p>
        </div>
        <div class="st-table-scroll" tabindex="0" role="region" aria-label="Lịch sử giá cơ bản">
            <table class="st-table st-table-compact">
                <thead><tr><th scope="col">Giá cơ bản</th><th scope="col">Trạng thái</th><th scope="col">Hiệu lực từ</th><th scope="col">Hiệu lực đến</th><th scope="col">Người cập nhật</th></tr></thead>
                <tbody><c:forEach var="p" items="${history}">
                    <tr class="${p.current ? 'bp-row-current' : ''}" data-price-history data-effective-date="${fn:substring(p.effectiveFrom, 0, 10)}" data-updater="<c:out value='${empty p.updatedByName ? "—" : p.updatedByName}' />">
                        <td><strong class="bp-history-price"><c:out value="${p.basePriceFormatted}" /></strong></td>
                        <td><c:choose>
                            <c:when test="${p.current}"><span class="bp-badge bp-badge-current">Đang áp dụng</span></c:when>
                            <c:when test="${p.scheduled}"><span class="bp-badge bp-badge-scheduled">Đã lên lịch</span></c:when>
                            <c:otherwise><span class="bp-badge bp-badge-ended">Đã kết thúc</span></c:otherwise>
                        </c:choose></td>
                        <td><c:out value="${p.effectiveFromFormatted}" /></td>
                        <td><c:out value="${empty p.effectiveTo ? '—' : p.effectiveToFormatted}" /></td>
                        <td><c:out value="${empty p.updatedByName ? '—' : p.updatedByName}" /></td>
                    </tr>
                </c:forEach><tr id="bp-history-empty" ${not empty history ? 'hidden' : ''}><td colspan="5" class="bp-empty">${empty history ? 'Chưa có lịch sử giá' : 'Không có lịch sử phù hợp'}</td></tr></tbody>
            </table>
        </div>
        <footer class="st-list-footer">
            <span id="bp-history-range" role="status" aria-live="polite"></span>
            <nav class="st-pagination" aria-label="Phân trang lịch sử giá">
                <button type="button" id="bp-history-prev" aria-label="Trang trước">‹</button>
                <span id="bp-history-page" aria-live="polite"></span>
                <button type="button" id="bp-history-next" aria-label="Trang sau">›</button>
            </nav>
        </footer>
    </section>
</div>
<%@ include file="/WEB-INF/fragments/cinemaEnd.jspf" %>
