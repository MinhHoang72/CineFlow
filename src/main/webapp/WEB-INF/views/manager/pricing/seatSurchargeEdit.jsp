<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%
    request.setAttribute("pageTitle", "Sửa phụ thu ghế & phòng");
    request.setAttribute("pageDescription", "Cập nhật tên và mức phụ thu cho đối tượng đã chọn.");
    request.setAttribute("activeTab", "surcharge");
%>
<%@ include file="/WEB-INF/fragments/cinemaBegin.jspf" %>
<c:choose>
    <c:when test="${not empty rule}">
        <section class="st-panel">
            <div class="st-panel-heading">
                <div>
                    <h2>Thông tin phụ thu</h2>
                    <p>Loại và đối tượng áp dụng được giữ nguyên khi cập nhật.</p>
                </div>
                <a class="st-link" href="${pageContext.request.contextPath}/pricing/seat-surcharges">← Danh sách phụ thu</a>
            </div>
            <form class="st-create-form" action="${pageContext.request.contextPath}/pricing/seat-surcharges/update" method="post">
                <input type="hidden" name="id" value="${rule.surchargeId}">
                <div class="st-fields st-fields-two">
                    <div class="st-field">
                        <label for="surcharge-type">Loại phụ thu</label>
                        <input id="surcharge-type" value="${rule.surchargeType == 'HALL' ? 'Phòng chiếu' : 'Loại ghế'}" readonly>
                    </div>
                    <div class="st-field">
                        <label for="surcharge-target">Đối tượng áp dụng</label>
                        <input id="surcharge-target" name="targetValue" value="<c:out value='${rule.targetValue}' />" readonly>
                    </div>
                    <div class="st-field">
                        <label for="surcharge-name">Tên phụ thu <span>*</span></label>
                        <input id="surcharge-name" name="surchargeName" value="<c:out value='${rule.surchargeName}' />" required>
                    </div>
                    <div class="st-field">
                        <label for="surcharge-amount">Số tiền (VNĐ) <span>*</span></label>
                        <input
                            id="surcharge-amount"
                            type="text"
                            inputmode="numeric"
                            data-money
                            name="amount"
                            value="<fmt:formatNumber value='${rule.amount}' pattern='0' groupingUsed='false' />"
                            required>
                    </div>
                </div>
                <div class="st-form-footer">
                    <p>Mức phụ thu cũ được giữ lại trong lịch sử.</p>
                    <div class="st-row-actions">
                        <a class="st-button st-button-muted" href="${pageContext.request.contextPath}/pricing/seat-surcharges">Quay lại</a>
                        <button class="st-button st-button-primary" type="submit">Lưu thay đổi</button>
                    </div>
                </div>
            </form>
        </section>
    </c:when>
    <c:otherwise>
        <div class="st-empty">
            <h2>Không tìm thấy phụ thu</h2>
            <a class="st-link" href="${pageContext.request.contextPath}/pricing/seat-surcharges">Quay lại danh sách</a>
        </div>
    </c:otherwise>
</c:choose>
<%@ include file="/WEB-INF/fragments/cinemaEnd.jspf" %>
