<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="header.jspf" %>
<%
    // Format ngày giờ yyyy-MM-ddTHH:mm cho thẻ input type="datetime-local"
    model.entity.Voucher v = (model.entity.Voucher) request.getAttribute("voucher");
    java.util.Map<String, String> rawMap = (java.util.Map<String, String>) request.getAttribute("raw");
    java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    String startVal = rawMap != null ? rawMap.get("startDate") : (v != null && v.getStartDate() != null ? v.getStartDate().format(fmt) : "");
    String endVal = rawMap != null ? rawMap.get("endDate") : (v != null && v.getEndDate() != null ? v.getEndDate().format(fmt) : "");
    pageContext.setAttribute("startVal", startVal);
    pageContext.setAttribute("endVal", endVal);
%>

<div class="cf-page-header">
    <div>
        <h1>${voucher.voucherId == 0 ? 'Thêm voucher' : 'Sửa voucher'}</h1>
        <c:choose>
            <c:when test="${isAdminScope}">
                <p>Voucher áp dụng cho <strong>toàn chuỗi</strong> CineFlow (CentreID = NULL).</p>
            </c:when>
            <c:otherwise>
                <p>Voucher áp dụng tại <strong>chi nhánh của bạn</strong> ${not empty voucher.centreName ? '('.concat(voucher.centreName).concat(')') : ''}.</p>
            </c:otherwise>
        </c:choose>
    </div>
    <a class="cf-btn cf-btn-ghost" href="${baseUrl}">Quay lại</a>
</div>

<div class="cf-card">
    <form class="vc-form" method="post" action="${baseUrl}/save">
        <input type="hidden" name="_csrf" value="${csrf}">
        <input type="hidden" name="id" value="${voucher.voucherId == 0 ? '' : voucher.voucherId}">

        <div class="cf-form-group">
            <label class="cf-form-label">Mã voucher</label>
            <input class="cf-form-control" type="text" name="code" maxlength="50" 
                   value="${voucher.voucherCode}" 
                   ${voucher.voucherId > 0 && voucher.usedCount > 0 ? 'readonly' : ''}>
            <c:if test="${voucher.voucherId > 0 && voucher.usedCount > 0}">
                <div class="cf-help">Voucher đã được sử dụng nên không đổi được mã.</div>
            </c:if>
            <c:if test="${not empty errors.code}">
                <div class="vc-error">${errors.code}</div>
            </c:if>
        </div>

        <div class="cf-form-group">
            <label class="cf-form-label">Tên voucher</label>
            <input class="cf-form-control" type="text" name="name" maxlength="150" value="${voucher.voucherName}">
            <c:if test="${not empty errors.name}">
                <div class="vc-error">${errors.name}</div>
            </c:if>
        </div>

        <div class="cf-form-group">
            <label class="cf-form-label">Mô tả</label>
            <textarea class="cf-form-textarea" name="description" rows="3" maxlength="500">${voucher.description}</textarea>
            <c:if test="${not empty errors.description}">
                <div class="vc-error">${errors.description}</div>
            </c:if>
        </div>

        <div class="cf-form-group">
            <label class="cf-form-label">Loại giảm giá</label>
            <select class="cf-form-control" name="type">
                <option value="PERCENT" ${voucher.discountType == 'PERCENT' ? 'selected' : ''}>Phần trăm (%)</option>
                <option value="FIXED" ${voucher.discountType == 'FIXED' ? 'selected' : ''}>Số tiền cố định (đ)</option>
            </select>
            <c:if test="${not empty errors.type}">
                <div class="vc-error">${errors.type}</div>
            </c:if>
        </div>

        <div class="cf-form-group">
            <label class="cf-form-label">Giá trị giảm</label>
            <input class="cf-form-control" type="text" name="discountValue" 
                   value="${not empty raw.discountValue ? raw.discountValue : voucher.discountValue}">
            <c:if test="${not empty errors.discountValue}">
                <div class="vc-error">${errors.discountValue}</div>
            </c:if>
        </div>

        <div class="cf-form-group">
            <label class="cf-form-label">Giá trị đơn tối thiểu (đ)</label>
            <input class="cf-form-control" type="text" name="minimumOrderValue" 
                   value="${not empty raw.minimumOrderValue ? raw.minimumOrderValue : (empty voucher.minimumOrderValue ? '0' : voucher.minimumOrderValue)}">
            <c:if test="${not empty errors.minimumOrderValue}">
                <div class="vc-error">${errors.minimumOrderValue}</div>
            </c:if>
        </div>

        <div class="cf-form-group">
            <label class="cf-form-label">Mức giảm tối đa (đ, để trống nếu không giới hạn)</label>
            <input class="cf-form-control" type="text" name="maximumDiscount" 
                   value="${not empty raw.maximumDiscount ? raw.maximumDiscount : voucher.maximumDiscount}">
            <c:if test="${not empty errors.maximumDiscount}">
                <div class="vc-error">${errors.maximumDiscount}</div>
            </c:if>
        </div>

        <div class="cf-form-group">
            <label class="cf-form-label">Bắt đầu</label>
            <input class="cf-form-control" type="datetime-local" name="startDate" value="${startVal}">
            <c:if test="${not empty errors.startDate}">
                <div class="vc-error">${errors.startDate}</div>
            </c:if>
        </div>

        <div class="cf-form-group">
            <label class="cf-form-label">Kết thúc</label>
            <input class="cf-form-control" type="datetime-local" name="endDate" value="${endVal}">
            <c:if test="${not empty errors.endDate}">
                <div class="vc-error">${errors.endDate}</div>
            </c:if>
        </div>

        <div class="cf-form-group">
            <label class="cf-form-label">Giới hạn lượt dùng (để trống nếu không giới hạn)</label>
            <input class="cf-form-control" type="text" name="usageLimit" 
                   value="${not empty raw.usageLimit ? raw.usageLimit : voucher.usageLimit}">
            <c:if test="${voucher.voucherId > 0}">
                <div class="cf-help">Đã dùng: ${voucher.usedCount}</div>
            </c:if>
            <c:if test="${not empty errors.usageLimit}">
                <div class="vc-error">${errors.usageLimit}</div>
            </c:if>
        </div>

        <div class="cf-form-group">
            <label class="cf-form-label">Trạng thái</label>
            <select class="cf-form-control" name="status">
                <option value="ACTIVE" ${voucher.status == 'ACTIVE' ? 'selected' : ''}>Hoạt động</option>
                <option value="INACTIVE" ${voucher.status == 'INACTIVE' ? 'selected' : ''}>Tạm ngưng</option>
            </select>
            <c:if test="${not empty errors.status}">
                <div class="vc-error">${errors.status}</div>
            </c:if>
        </div>

        <button class="cf-btn cf-btn-primary" type="submit">Lưu</button>
    </form>
</div>

<%@ include file="footer.jspf" %>
