<%@ page pageEncoding="UTF-8" %>
<%
    request.setAttribute("pageTitle", "Giá vé cơ bản");
    request.setAttribute("pageDescription", "Quản lý giá cơ bản và theo dõi lịch sử điều chỉnh tại cụm rạp.");
    request.setAttribute("activeTab", "base");
%>
<%@ include file="/WEB-INF/fragments/cinemaBegin.jspf" %>
<div class="st-pricing-grid">
    <section class="st-panel">
        <div class="st-panel-heading">
            <div>
                <h2>Giá cơ bản hiện tại</h2>
                <p><c:out value="${centreName}" /></p>
            </div>
        </div>
        <div class="st-price-card">
            <c:choose>
                <c:when test="${not empty currentPrice}">
                    <span class="st-status st-status-COMPLETED">Đang áp dụng</span>
                    <strong class="st-price-value"><c:out value="${currentPrice.basePriceFormatted}" /></strong>
                    <p>Hiệu lực từ <c:out value="${currentPrice.effectiveFromFormatted}" /></p>
                </c:when>
                <c:otherwise>
                    <strong class="st-price-value">Chưa thiết lập</strong>
                    <p>Nhập giá cơ bản để cấu hình bảng giá cho cụm rạp.</p>
                </c:otherwise>
            </c:choose>
        </div>
    </section>
    <section class="st-panel">
        <div class="st-panel-heading">
            <div>
                <h2>Cập nhật giá cơ bản</h2>
                <p>Áp dụng ngay hoặc hẹn thời điểm bắt đầu trong tương lai.</p>
            </div>
        </div>
        <form class="st-create-form" action="${pageContext.request.contextPath}/pricing/update" method="post">
            <input type="hidden" name="centreId" value="${centreId}">
            <div class="st-field">
                <label for="base-price">Giá cơ bản mới (VNĐ) <span>*</span></label>
                <input id="base-price" type="text" inputmode="numeric" data-money name="basePrice" required placeholder="Ví dụ: 80.000">
            </div>
            <fieldset class="pr-effective">
                <legend>Thời gian bắt đầu áp dụng</legend>
                <div class="pr-effective-options">
                    <label>
                        <input type="radio" name="effectiveMode" value="now" checked>
                        Ngay bây giờ
                    </label>
                    <label>
                        <input type="radio" name="effectiveMode" value="scheduled">
                        Chọn ngày / giờ
                    </label>
                </div>
                <div class="st-field" id="pr-effective-field" hidden>
                    <label for="pr-effective-from">Ngày và giờ áp dụng <span>*</span></label>
                    <input id="pr-effective-from" type="datetime-local" name="effectiveFrom" disabled>
                    <small>Giờ Việt Nam (UTC+7). Giá hiện tại được giữ đến thời điểm đã chọn.</small>
                </div>
            </fieldset>
            <div class="st-form-footer">
                <p></p>
                <button class="st-button st-button-primary" id="pr-save-price" type="submit">Cập nhật giá</button>
            </div>
        </form>
    </section>
</div>
<section class="st-panel">
    <div class="st-panel-heading">
        <div>
            <h2>Lịch sử giá cơ bản <span class="st-count">${fn:length(history)}</span></h2>
            <p>Các mức giá và thời gian áp dụng.</p>
        </div>
    </div>
    <div class="st-table-scroll" tabindex="0" role="region" aria-label="Lịch sử giá cơ bản">
        <table class="st-table st-table-compact">
            <thead>
                <tr>
                    <th scope="col">STT</th>
                    <th scope="col">Giá cơ bản</th>
                    <th scope="col">Hiệu lực từ</th>
                    <th scope="col">Hiệu lực đến</th>
                    <th scope="col">Người cập nhật</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="p" items="${history}" varStatus="loop">
                    <tr>
                        <td>${loop.count}</td>
                        <td><strong class="st-money"><c:out value="${p.basePriceFormatted}" /></strong></td>
                        <td><c:out value="${p.effectiveFromFormatted}" /></td>
                        <td>
                            <c:choose>
                                <c:when test="${p.scheduled}">
                                    <span class="st-status st-status-SCHEDULED">Đã lên lịch</span>
                                    <c:if test="${not empty p.effectiveTo}">
                                        <small class="pr-date-note">Đến <c:out value="${p.effectiveToFormatted}" /></small>
                                    </c:if>
                                </c:when>
                                <c:when test="${p.current}">
                                    <span class="st-status st-status-COMPLETED">Đang áp dụng</span>
                                    <c:if test="${not empty p.effectiveTo}">
                                        <small class="pr-date-note">Đến <c:out value="${p.effectiveToFormatted}" /></small>
                                    </c:if>
                                </c:when>
                                <c:otherwise><c:out value="${p.effectiveToFormatted}" /></c:otherwise>
                            </c:choose>
                        </td>
                        <td><c:out value="${empty p.updatedByName ? 'Chưa xác định' : p.updatedByName}" /></td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>
    <c:if test="${empty history}">
        <div class="st-empty">
            <h3>Chưa có lịch sử giá</h3>
            <p>Các lần cập nhật giá sẽ xuất hiện tại đây.</p>
        </div>
    </c:if>
</section>
<%@ include file="/WEB-INF/fragments/cinemaEnd.jspf" %>
