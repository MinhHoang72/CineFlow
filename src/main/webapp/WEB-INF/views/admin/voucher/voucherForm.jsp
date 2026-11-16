<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="header.jspf" %>
<%
    // Format ngày giờ yyyy-MM-ddTHH:mm cho thẻ input type="datetime-local"
    model.entity.Voucher v = (model.entity.Voucher) request.getAttribute("voucher");
    java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    pageContext.setAttribute("startVal", (v != null && v.getStartDate() != null) ? v.getStartDate().format(fmt) : "");
    pageContext.setAttribute("endVal", (v != null && v.getEndDate() != null) ? v.getEndDate().format(fmt) : "");
%>

<div class="cf-page-header">
    <div>
        <h1>${pageTitle}</h1>
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
        <input type="hidden" name="id" value="${voucher.voucherId == 0 ? '' : voucher.voucherId}">

        <div class="cf-form-group">
            <label class="cf-form-label">Mã voucher <span style="color: #ef4444;">*</span></label>
            <input class="cf-form-control" type="text" name="code" maxlength="50" 
                   value="${voucher.voucherCode}" required
                   placeholder="Ví dụ: CGY_STUDENT, VIP2026..."
                   <%-- 'readonly' chỉ đọc  --%>
                   ${voucher.voucherId > 0 && voucher.usedCount > 0 ? 'readonly' : ''}>
            <c:if test="${voucher.voucherId > 0 && voucher.usedCount > 0}">
                <div class="cf-help">Voucher đã được sử dụng nên không đổi được mã.</div>
            </c:if>
        </div>

        <div class="cf-form-group">
            <label class="cf-form-label">Tên voucher <span style="color: #ef4444;">*</span></label>
            <input class="cf-form-control" type="text" name="name" maxlength="150" value="${voucher.voucherName}" 
                   placeholder="Ví dụ: HSSV Siêu Tiết Kiệm - Giảm 20%" required>
        </div>

        <div class="cf-form-group">
            <label class="cf-form-label">Hình ảnh / Banner Voucher (URL)</label>
            <input class="cf-form-control" type="text" name="imageUrl" id="voucherImageUrl" maxlength="500"
                   placeholder="Dán đường dẫn ảnh (VD: https://...)"
                   value="${voucher.imageUrl}" oninput="updateVoucherImagePreview(this.value)">
            <div id="voucherImagePreviewContainer" style="margin-top: 10px; display: ${not empty voucher.imageUrl ? 'block' : 'none'};">
                <img id="voucherImagePreview" src="${voucher.imageUrl}" alt="Preview"
                     style="max-width: 260px; max-height: 140px; border-radius: 6px; border: 1px solid #cbd5e1; object-fit: cover;"
                     onerror="this.parentElement.style.display='none';">
            </div>
        </div>

        <div class="cf-form-group">
            <label class="cf-form-label">Mô tả</label>
            <textarea class="cf-form-textarea" name="description" rows="3" maxlength="500">${voucher.description}</textarea>
        </div>

        <div class="cf-form-group">
            <label class="cf-form-label">Loại giảm giá</label>
            <select class="cf-form-control" name="type">
                <option value="PERCENT" ${voucher.discountType == 'PERCENT' ? 'selected' : ''}>Phần trăm (%)</option>
                <option value="FIXED" ${voucher.discountType == 'FIXED' ? 'selected' : ''}>Số tiền cố định (đ)</option>
            </select>
        </div>

        <div class="cf-form-group">
            <label class="cf-form-label">Giá trị giảm <span style="color: #ef4444;">*</span></label>
            <input class="cf-form-control" type="text" name="discountValue" required
                   placeholder="Nhập số % (VD: 20) hoặc số tiền (VD: 30000)"
                   value="${not empty voucher.discountValue ? voucher.discountValue.stripTrailingZeros().toPlainString() : ''}">
        </div>

        <div class="cf-form-group">
            <label class="cf-form-label">Giá trị đơn tối thiểu (đ)</label>
            <input class="cf-form-control" type="text" name="minimumOrderValue" 
                   value="${not empty voucher.minimumOrderValue ? voucher.minimumOrderValue.stripTrailingZeros().toPlainString() : '0'}">
        </div>

        <div class="cf-form-group">
            <label class="cf-form-label">Mức giảm tối đa (đ, để trống nếu không giới hạn)</label>
            <input class="cf-form-control" type="text" name="maximumDiscount" 
                   placeholder="Chỉ áp dụng cho loại giảm % (VD: 50000)"
                   value="${not empty voucher.maximumDiscount ? voucher.maximumDiscount.stripTrailingZeros().toPlainString() : ''}">
        </div>

        <div class="cf-form-group">
            <label class="cf-form-label">Bắt đầu <span style="color: #ef4444;">*</span></label>
            <input class="cf-form-control" type="datetime-local" name="startDate" value="${startVal}" required>
        </div>

        <div class="cf-form-group">
            <label class="cf-form-label">Kết thúc <span style="color: #ef4444;">*</span></label>
            <input class="cf-form-control" type="datetime-local" name="endDate" value="${endVal}" required>
        </div>

        <div class="cf-form-group">
            <label class="cf-form-label">Giới hạn lượt dùng (để trống nếu không giới hạn)</label>
            <input class="cf-form-control" type="text" name="usageLimit" value="${voucher.usageLimit}">
            <c:if test="${voucher.voucherId > 0}">
                <div class="cf-help">Đã dùng: ${voucher.usedCount}</div>
            </c:if>
        </div>

        <div class="cf-form-group">
            <label class="cf-form-label">Trạng thái</label>
            <select class="cf-form-control" name="status">
                <option value="ACTIVE" ${voucher.status == 'ACTIVE' ? 'selected' : ''}>Hoạt động</option>
                <option value="INACTIVE" ${voucher.status == 'INACTIVE' ? 'selected' : ''}>Tạm ngưng</option>
            </select>
        </div>

        <button class="cf-btn cf-btn-primary" type="submit">Lưu</button>
    </form>
</div>

<%@ include file="footer.jspf" %>

<script>
    function updateVoucherImagePreview(url) {
        const container = document.getElementById('voucherImagePreviewContainer');
        const img = document.getElementById('voucherImagePreview');
        if (url && url.trim().length > 0) {
            img.src = url.trim();
            container.style.display = 'block';
        } else {
            container.style.display = 'none';
        }
    }
</script>
