<%@ page pageEncoding="UTF-8" %>
<%
    String policyType=(String)request.getAttribute("policyType");
    String policyTitle="CENTRE".equals(policyType)?"Phụ thu chi nhánh":"HALL".equals(policyType)?"Phụ thu phòng chiếu":"SEAT".equals(policyType)?"Phụ thu ghế":"MOVIE".equals(policyType)?"Phụ thu phim":"Phụ thu khung giờ";
    request.setAttribute("activeTab",policyType);
    request.setAttribute("pageTitle",policyTitle);
    request.setAttribute("pageDescription","");
%>
<%@ include file="/WEB-INF/fragments/cinemaBegin.jspf" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<section class="st-panel" id="policy-editor" aria-labelledby="policy-editor-title" ${empty editPolicy && param.create != 'true' ? 'hidden' : ''}>
    <div class="st-panel-heading"><div><h2 id="policy-editor-title" tabindex="-1"><c:out value="${empty editPolicy ? 'Thêm chính sách' : 'Điều chỉnh chính sách'}" /></h2><p><c:out value="${centreName}" /></p></div></div>
    <form class="st-create-form" method="post" action="${pageContext.request.contextPath}/pricing/surcharges/save">
        <input type="hidden" name="type" value="${policyType}"><input type="hidden" name="id" value="${editPolicy.SurchargeID}">
        <div class="st-form-grid">
            <c:choose><c:when test="${policyType == 'TIME'}">
                <div class="st-field"><label for="policy-start">Từ giờ</label><input id="policy-start" type="time" step="1" name="startTime" value="${editPolicy.StartTime}" required></div>
                <div class="st-field"><label for="policy-end">Đến giờ (không bao gồm)</label><input id="policy-end" type="time" step="1" name="endTime" value="${editPolicy.EndTime}" required></div>
            </c:when><c:otherwise>
                <div class="st-field"><label for="policy-target">Đối tượng áp dụng</label>
                    <select id="policy-target" name="target" required>
                        <c:forEach var="target" items="${targets}"><option value="<c:out value='${target.key}' />" ${target.key == editPolicy.TargetValue ? 'selected' : ''}><c:out value="${target.value}" /></option></c:forEach>
                    </select>
                </div>
            </c:otherwise></c:choose>
            <div class="st-field"><label for="policy-name">Tên chính sách</label><input id="policy-name" name="name" maxlength="100" value="<c:out value='${editPolicy.SurchargeName}' />" required></div>
            <div class="st-field"><label for="policy-amount">Phụ thu (VNĐ)</label><input id="policy-amount" name="amount" inputmode="numeric" data-money value="${empty editPolicy ? '' : editPolicy.Amount.longValue()}" required placeholder="Ví dụ: 10.000"></div>
            <div class="st-field"><label for="policy-mode">Bắt đầu áp dụng</label><select id="policy-mode" name="effectiveMode" aria-controls="policy-effective-field" aria-expanded="false"><option value="now">Ngay bây giờ</option><option value="scheduled">Chọn ngày / giờ</option></select></div>
            <div class="st-field" id="policy-effective-field" hidden><label for="policy-from">Ngày / giờ áp dụng</label><input id="policy-from" name="effectiveFrom" type="datetime-local" disabled></div>
        </div>
        <div class="st-form-footer"><a class="st-button st-button-muted" href="?" data-policy-cancel><c:out value="${empty editPolicy ? 'Hủy' : 'Hủy chỉnh sửa'}" /></a><button class="st-button st-button-primary" type="submit"><c:out value="${empty editPolicy ? 'Thêm chính sách' : 'Lưu thay đổi'}" /></button></div>
    </form>
</section>
<section class="st-panel">
    <div class="st-panel-heading"><div><h2>Chính sách &amp; lịch sử</h2><p><c:out value="${centreName}" /></p></div><a class="st-button st-button-primary" href="?create=true#policy-editor" id="policy-add" aria-controls="policy-editor" aria-expanded="${not empty editPolicy || param.create == 'true'}" ${not empty editPolicy || param.create == 'true' ? 'hidden' : ''}>+ Thêm phụ thu</a></div>
    <div class="bp-history-filters" role="search" aria-label="Lọc lịch sử phụ thu">
        <div class="st-field"><label for="bp-history-from">Áp dụng từ ngày</label><input id="bp-history-from" type="date"></div>
        <div class="st-field"><label for="bp-history-to">Đến ngày</label><input id="bp-history-to" type="date"></div>
        <div class="st-field"><label for="bp-history-updater">Người cập nhật</label><select id="bp-history-updater"><option value="">Tất cả</option></select></div>
        <button class="st-button st-button-muted" type="button" id="bp-history-reset">Xóa bộ lọc</button>
        <p id="bp-history-filter-error" class="bp-filter-error" role="alert" hidden>Ngày kết thúc phải bằng hoặc sau ngày bắt đầu.</p>
    </div>
    <div class="st-table-scroll"><table class="st-table st-table-compact pr-policy-table">
        <thead><tr><th>Tên / Đối tượng</th><th>Phụ thu</th><th>Hiệu lực từ</th><th>Hiệu lực đến</th><th>Người cập nhật</th><th class="pr-policy-actions-cell">Thao tác</th></tr></thead>
        <tbody><c:forEach var="p" items="${policies}">
            <tr data-price-history data-effective-date="${fn:substring(p.EffectiveFrom, 0, 10)}" data-updater="<c:out value='${empty p.UpdatedByName ? "Hệ thống" : p.UpdatedByName}' />"><td><strong><c:out value="${p.SurchargeName}" /></strong><br><c:out value="${policyType == 'TIME' ? p.TargetValue : targets[p.TargetValue]}" /><br><small><c:out value="${p.PolicyState}" /></small></td>
                <td><fmt:formatNumber value="${p.Amount}" maxFractionDigits="0" /> đ</td><td><fmt:formatDate value="${p.EffectiveFrom}" pattern="dd/MM/yyyy HH:mm" /></td>
                <td><c:choose><c:when test="${empty p.EffectiveTo}">Chưa kết thúc</c:when><c:otherwise><fmt:formatDate value="${p.EffectiveTo}" pattern="dd/MM/yyyy HH:mm" /></c:otherwise></c:choose></td>
                <td><c:out value="${p.UpdatedByName}" default="Hệ thống" /></td>
                <td class="pr-policy-actions-cell"><c:if test="${p.PolicyState != 'Đã kết thúc'}"><div class="st-row-actions pr-policy-actions">
                    <a class="st-icon-action" href="?edit=${p.SurchargeID}#policy-editor" title="Sửa / Lên lịch giá mới" aria-label="Sửa hoặc lên lịch giá mới cho chính sách #${p.SurchargeID}">
                        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m16 3 5 5-12 12-6 1 1-6Z M14 5l5 5" /></svg>
                    </a>
                    <form method="post" action="${pageContext.request.contextPath}/pricing/surcharges/end" data-confirm="Ngừng áp dụng chính sách phụ thu này?"><input type="hidden" name="type" value="${policyType}"><input type="hidden" name="id" value="${p.SurchargeID}"><button class="st-icon-action st-link-danger" type="submit" title="Ngừng áp dụng" aria-label="Ngừng áp dụng chính sách #${p.SurchargeID}">
                        <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9" /><path d="m6 6 12 12" /></svg>
                    </button></form>
                </div></c:if></td>
            </tr>
        </c:forEach><tr id="bp-history-empty" ${not empty policies ? 'hidden' : ''}><td colspan="6" class="bp-empty">${empty policies ? 'Chưa có phụ thu' : 'Không có chính sách phù hợp'}</td></tr></tbody>
    </table></div>
    <footer class="st-list-footer">
        <span id="bp-history-range" role="status" aria-live="polite"></span>
        <span id="bp-history-count" hidden></span>
        <nav class="st-pagination" aria-label="Phân trang lịch sử phụ thu">
            <button type="button" id="bp-history-prev" aria-label="Trang trước">‹</button>
            <span id="bp-history-page" aria-live="polite"></span>
            <button type="button" id="bp-history-next" aria-label="Trang sau">›</button>
        </nav>
    </footer>
</section>
<%@ include file="/WEB-INF/fragments/cinemaEnd.jspf" %>
