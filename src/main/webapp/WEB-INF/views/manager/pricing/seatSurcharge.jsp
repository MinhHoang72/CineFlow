<%@ page pageEncoding="UTF-8" %>
<%
    request.setAttribute("pageTitle", "Phụ thu ghế & phòng");
    request.setAttribute("pageDescription", "Quản lý mức phụ thu theo loại ghế, loại phòng và theo dõi lịch sử thay đổi.");
    request.setAttribute("activeTab", "surcharge");
%>
<%@ include file="/WEB-INF/fragments/cinemaBegin.jspf" %>
<dialog class="pr-dialog" id="pr-surcharge-dialog" aria-labelledby="pr-surcharge-title" data-reopen="${sessionScope.reopenSurcharge}">
    <div class="st-panel-heading">
        <div>
            <h2 id="pr-surcharge-title">Thêm phụ thu ghế &amp; phòng</h2>
            <p>Cấu hình phụ thu áp dụng ngay sau khi lưu.</p>
        </div>
        <button type="button" class="pr-dialog-close" data-close-surcharge aria-label="Đóng">×</button>
    </div>
    <c:if test="${not empty surchargeFormError}">
        <div class="st-notice st-error" role="alert"><c:out value="${surchargeFormError}" /></div>
    </c:if>
    <form class="st-create-form" action="${pageContext.request.contextPath}/pricing/seat-surcharges" method="post">
        <div class="st-fields st-fields-two">
            <div class="st-field">
                <label for="pr-rule-type">Loại phụ thu <span>*</span></label>
                <select id="pr-rule-type" name="surchargeType" required>
                    <option value="SEAT">Loại ghế</option><option value="HALL" ${sessionScope.surchargeDraft.surchargeType == 'HALL' ? 'selected' : ''}>Loại phòng</option>
                </select>
            </div>
            <div class="st-field">
                <label for="pr-rule-target">Loại ghế / phòng áp dụng <span>*</span></label>
                <select id="pr-rule-target" name="targetValue" required>
                    <option value="">Chọn loại ghế / phòng</option>
                    <c:forEach var="target" items="${seatTargets}">
                        <option value="<c:out value='${target}' />" data-target-type="SEAT" ${sessionScope.surchargeDraft.surchargeType != 'HALL' and sessionScope.surchargeDraft.targetValue == target ? 'selected' : ''}><c:out value="${target}" /></option>
                    </c:forEach>
                    <c:forEach var="target" items="${hallTargets}">
                        <option value="<c:out value='${target}' />" data-target-type="HALL" ${sessionScope.surchargeDraft.surchargeType == 'HALL' and sessionScope.surchargeDraft.targetValue == target ? 'selected' : ''}><c:out value="${target}" /></option>
                    </c:forEach>
                </select>
                <small id="pr-target-help">Chỉ hiển thị các loại đã có trong cấu hình rạp.</small>
            </div>
            <div class="st-field">
                <label for="pr-rule-name">Tên phụ thu <span>*</span></label>
                <input
                    id="pr-rule-name"
                    name="surchargeName"
                    maxlength="100"
                    required
                    placeholder="Ví dụ: Phụ thu ghế VIP"
                    value="<c:out value='${sessionScope.surchargeDraft.surchargeName}' />">
            </div>
            <div class="st-field">
                <label for="pr-rule-amount">Phụ thu (VNĐ) <span>*</span></label>
                <input
                    id="pr-rule-amount"
                    name="amount"
                    type="text"
                    data-money
                    inputmode="numeric"
                    required
                    placeholder="Ví dụ: 10.000"
                    value="<c:out value='${sessionScope.surchargeDraft.amount}' />">
            </div>
        </div>
        <p class="pr-help">Chọn loại ghế hoặc phòng có sẵn trong hệ thống. Ghế STANDARD sử dụng giá cơ bản, không có phụ thu ghế.</p>
        <div class="st-form-footer">
            <p>Không tạo trùng đối tượng đã có phụ thu.</p>
            <div class="st-row-actions">
                <button type="button" class="st-button st-button-muted" data-close-surcharge>Hủy</button>
                <button type="submit" class="st-button st-button-primary">Thêm phụ thu</button>
            </div>
        </div>
    </form>
</dialog>
<c:remove var="reopenSurcharge" scope="session" />
<c:remove var="surchargeDraft" scope="session" />
<section class="st-panel">
    <div class="st-panel-heading">
        <div>
            <h2>Phụ thu đang áp dụng <span class="st-count">${fn:length(currentRules)}</span></h2>
            <p>Phân biệt phụ thu loại ghế và phụ thu loại phòng chiếu.</p>
        </div>
        <button type="button" class="st-button st-button-primary" id="pr-open-surcharge">+ Thêm phụ thu</button>
    </div>
    <div class="st-table-scroll" tabindex="0" role="region" aria-label="Các phụ thu hiện tại">
        <table class="st-table st-table-compact">
            <thead>
                <tr>
                    <th scope="col">Loại / Đối tượng</th>
                    <th scope="col">Tên phụ thu</th>
                    <th scope="col">Số tiền</th>
                    <th scope="col">Hiệu lực từ</th>
                    <th scope="col" class="pr-action-cell">Thao tác</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="s" items="${currentRules}">
                    <tr>
                        <td>
                            <span class="st-format">${s.surchargeType == 'HALL' ? 'Phòng chiếu' : 'Ghế'}</span><span class="st-hall-name"><c:out value="${s.targetValue}" /></span>
                        </td>
                        <td><strong class="st-movie"><c:out value="${s.surchargeName}" /></strong></td>
                        <td class="st-money"><c:out value="${s.amountFormatted}" /></td>
                        <td><c:out value="${s.effectiveFromFormatted}" /></td>
                        <td class="pr-action-cell">
                            <div class="st-row-actions">
                                <a
                                    class="st-link st-icon-action"
                                    title="Sửa"
                                    aria-label="Sửa cấu hình #${s.surchargeId}"
                                    href="${pageContext.request.contextPath}/pricing/seat-surcharges/edit?id=${s.surchargeId}">
                                    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="m15 5 4 4M4 20l4-1L20 7a2.8 2.8 0 0 0-4-4L4 15l-1 6Z"/></svg>
                                </a>
                                <form
                                    action="${pageContext.request.contextPath}/pricing/seat-surcharges/delete"
                                    method="post"
                                    data-confirm="Ngừng áp dụng phụ thu này?">
                                    <input type="hidden" name="id" value="${s.surchargeId}">
                                    <button
                                        class="st-link st-link-danger st-icon-action"
                                        type="submit"
                                        title="Ngừng áp dụng"
                                        aria-label="Ngừng áp dụng cấu hình #${s.surchargeId}">
                                        <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><circle cx="12" cy="12" r="9"/><path d="m6 6 12 12"/></svg>
                                    </button>
                                </form>
                            </div>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>
    <c:if test="${empty currentRules}">
        <div class="st-empty">
            <h3>Chưa có phụ thu đang áp dụng</h3>
            <p>Các phụ thu loại ghế và loại phòng sẽ hiển thị tại đây.</p>
        </div>
    </c:if>
</section>
<section class="st-panel">
    <div class="st-panel-heading">
        <div>
            <h2>Lịch sử phụ thu</h2>
            <p>Theo dõi đối tượng, mức tiền và thời gian hiệu lực.</p>
        </div>
    </div>
    <div class="st-table-scroll" tabindex="0" role="region" aria-label="Lịch sử phụ thu">
        <table class="st-table st-table-compact">
            <thead>
                <tr>
                    <th scope="col">STT</th>
                    <th scope="col">Loại / Đối tượng</th>
                    <th scope="col">Tên phụ thu</th>
                    <th scope="col">Số tiền</th>
                    <th scope="col">Hiệu lực từ</th>
                    <th scope="col">Hiệu lực đến</th>
                    <th scope="col">Người cập nhật</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="s" items="${history}" varStatus="loop">
                    <tr>
                        <td>${loop.count}</td>
                        <td><span class="st-format">${s.surchargeType == 'HALL' ? 'Phòng chiếu' : 'Ghế'}</span><c:out value="${s.targetValue}" /></td>
                        <td><c:out value="${s.surchargeName}" /></td>
                        <td class="st-money"><c:out value="${s.amountFormatted}" /></td>
                        <td><c:out value="${s.effectiveFromFormatted}" /></td>
                        <td>
                            <c:choose>
                                <c:when test="${empty s.effectiveTo}"><span class="st-status st-status-COMPLETED">Đang áp dụng</span></c:when>
                                <c:otherwise><c:out value="${s.effectiveToFormatted}" /></c:otherwise>
                            </c:choose>
                        </td>
                        <td><c:out value="${empty s.updatedByName ? 'Chưa xác định' : s.updatedByName}" /></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>
    <c:if test="${empty history}">
        <div class="st-empty">
            <h3>Chưa có lịch sử phụ thu</h3>
        </div>
    </c:if>
</section>
<%@ include file="/WEB-INF/fragments/cinemaEnd.jspf" %>
