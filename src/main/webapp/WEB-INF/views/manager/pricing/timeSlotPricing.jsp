<%@ page pageEncoding="UTF-8" %>
<%
    request.setAttribute("pageTitle", "Phụ thu theo khung giờ");
    request.setAttribute("pageDescription", "Thiết lập mức phụ thu cho từng khung giờ chiếu tại cụm rạp.");
    request.setAttribute("activeTab", "time");
%>
<%@ include file="/WEB-INF/fragments/cinemaBegin.jspf" %>
<section class="st-panel">
    <div class="st-panel-heading">
        <div>
            <h2>Thêm khung giờ</h2>
            <p>Nhập thời gian bắt đầu, kết thúc và mức phụ thu.</p>
        </div>
        <span class="st-step">CỤM RẠP #${centreId}</span>
    </div>
    <form class="st-create-form" action="${pageContext.request.contextPath}/pricing/time-slots/save" method="post">
        <input type="hidden" name="centreId" value="${centreId}">
        <div class="st-fields st-fields-four">
            <div class="st-field">
                <label for="slot-name">Tên khung giờ <span>*</span></label>
                <input id="slot-name" name="slotName" type="text" required placeholder="Ví dụ: Suất tối">
            </div>
            <div class="st-field">
                <label for="slot-start">Bắt đầu <span>*</span></label>
                <input id="slot-start" type="time" name="startTime" required>
            </div>
            <div class="st-field">
                <label for="slot-end">Kết thúc <span>*</span></label>
                <input id="slot-end" type="time" name="endTime" required>
            </div>
            <div class="st-field">
                <label for="slot-amount">Phụ thu (VNĐ) <span>*</span></label>
                <input id="slot-amount" type="text" inputmode="numeric" data-money name="surcharge" required placeholder="Ví dụ: 10.000">
            </div>
        </div>
        <div class="st-form-footer">
            <p></p>
            <button class="st-button st-button-primary" type="submit">Thêm khung giờ</button>
        </div>
    </form>
</section>
<section class="st-panel">
    <div class="st-panel-heading">
        <div>
            <h2>Khung giờ đang áp dụng <span class="st-count">${fn:length(rules)}</span></h2>
            <p>Sửa mức phụ thu hoặc ngừng áp dụng một khung giờ.</p>
        </div>
    </div>
    <div class="st-table-scroll" tabindex="0" role="region" aria-label="Các khung giờ hiện tại">
        <table class="st-table st-table-compact">
            <thead>
                <tr>
                    <th scope="col">Khung giờ</th>
                    <th scope="col">Bắt đầu</th>
                    <th scope="col">Kết thúc</th>
                    <th scope="col">Phụ thu</th>
                    <th scope="col">Hiệu lực từ</th>
                    <th scope="col" class="pr-action-cell">Thao tác</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="r" items="${rules}">
                    <tr>
                        <td><strong class="st-movie"><c:out value="${r.slotName}" /></strong></td>
                        <td><c:out value="${r.startTimeFormatted}" /></td>
                        <td><c:out value="${r.endTimeFormatted}" /></td>
                        <td class="st-money"><c:out value="${r.surchargeFormatted}" /></td>
                        <td><c:out value="${r.effectiveFromFormatted}" /></td>
                        <td class="pr-action-cell">
                            <div class="st-row-actions">
                                <a
                                    class="st-link st-icon-action"
                                    title="Sửa"
                                    aria-label="Sửa cấu hình #${r.timeSlotPricingId}"
                                    href="${pageContext.request.contextPath}/pricing/time-slots/edit?id=${r.timeSlotPricingId}">
                                    <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="m15 5 4 4M4 20l4-1L20 7a2.8 2.8 0 0 0-4-4L4 15l-1 6Z"/></svg>
                                </a>
                                <form
                                    action="${pageContext.request.contextPath}/pricing/time-slots/delete"
                                    method="post"
                                    data-confirm="Ngừng áp dụng khung giờ này?">
                                    <input type="hidden" name="id" value="${r.timeSlotPricingId}">
                                    <button
                                        class="st-link st-link-danger st-icon-action"
                                        type="submit"
                                        title="Ngừng áp dụng"
                                        aria-label="Ngừng áp dụng cấu hình #${r.timeSlotPricingId}">
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
    <c:if test="${empty rules}">
        <div class="st-empty">
            <h3>Chưa có phụ thu khung giờ</h3>
            <p>Thêm khung giờ bằng biểu mẫu phía trên.</p>
        </div>
    </c:if>
</section>
<section class="st-panel">
    <div class="st-panel-heading">
        <div>
            <h2>Lịch sử phụ thu khung giờ</h2>
            <p>Toàn bộ mức phụ thu và thời gian áp dụng.</p>
        </div>
    </div>
    <div class="st-table-scroll" tabindex="0" role="region" aria-label="Lịch sử phụ thu khung giờ">
        <table class="st-table st-table-compact">
            <thead>
                <tr>
                    <th scope="col">STT</th>
                    <th scope="col">Khung giờ</th>
                    <th scope="col">Thời gian</th>
                    <th scope="col">Phụ thu</th>
                    <th scope="col">Hiệu lực từ</th>
                    <th scope="col">Hiệu lực đến</th>
                    <th scope="col">Người cập nhật</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="r" items="${history}" varStatus="loop">
                    <tr>
                        <td>${loop.count}</td>
                        <td><c:out value="${r.slotName}" /></td>
                        <td class="st-time"><c:out value="${r.startTimeFormatted}" /> – <c:out value="${r.endTimeFormatted}" /></td>
                        <td class="st-money"><c:out value="${r.surchargeFormatted}" /></td>
                        <td><c:out value="${r.effectiveFromFormatted}" /></td>
                        <td>
                            <c:choose>
                                <c:when test="${empty r.effectiveTo}"><span class="st-status st-status-COMPLETED">Đang áp dụng</span></c:when>
                                <c:otherwise><c:out value="${r.effectiveToFormatted}" /></c:otherwise>
                            </c:choose>
                        </td>
                        <td><c:out value="${empty r.updatedByName ? 'Chưa xác định' : r.updatedByName}" /></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>
    <c:if test="${empty history}">
        <div class="st-empty">
            <h3>Chưa có lịch sử điều chỉnh</h3>
        </div>
    </c:if>
</section>
<%@ include file="/WEB-INF/fragments/cinemaEnd.jspf" %>
