<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.entity.Voucher, model.entity.User, java.util.Map" %>
<%@ include file="header.jspf" %>
<%
    Voucher v = (Voucher) request.getAttribute("voucher");
    Map<String, String> errors = (Map<String, String>) request.getAttribute("errors");
    Map<String, String> raw = (Map<String, String>) request.getAttribute("raw");
    String csrf = (String) request.getAttribute("csrf");
    boolean adminScope = Boolean.TRUE.equals(request.getAttribute("isAdminScope"));
    boolean locked = v.getVoucherId() > 0 && v.getUsedCount() > 0;
    java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    String discountValue = raw != null ? raw.get("discountValue") : (v.getDiscountValue() == null ? "" : v.getDiscountValue().stripTrailingZeros().toPlainString());
    String minimum = raw != null ? raw.get("minimumOrderValue") : (v.getMinimumOrderValue() == null ? "0" : v.getMinimumOrderValue().stripTrailingZeros().toPlainString());
    String maximum = raw != null ? raw.get("maximumDiscount") : (v.getMaximumDiscount() == null ? "" : v.getMaximumDiscount().stripTrailingZeros().toPlainString());
    String startValue = raw != null ? raw.get("startDate") : (v.getStartDate() == null ? "" : v.getStartDate().format(fmt));
    String endValue = raw != null ? raw.get("endDate") : (v.getEndDate() == null ? "" : v.getEndDate().format(fmt));
    String limit = raw != null ? raw.get("usageLimit") : (v.getUsageLimit() == null ? "" : String.valueOf(v.getUsageLimit()));
%>
<div class="cf-page-header">
    <div>
        <h1><%= v.getVoucherId() == 0 ? "Thêm voucher" : "Sửa voucher" %></h1>
        <% if (adminScope) { %>
            <p>Voucher áp dụng cho <strong>toàn chuỗi</strong> CineFlow (CentreID = NULL).</p>
        <% } else { %>
            <p>Voucher áp dụng tại <strong>chi nhánh của bạn</strong>
               <%= v.getCentreName() != null ? "(" + esc(v.getCentreName()) + ")" : "" %>.</p>
        <% } %>
    </div>
    <a class="cf-btn cf-btn-ghost" href="<%= esc(baseUrl) %>">Quay lại</a>
</div>
<div class="cf-card">
    <form class="vc-form" method="post" action="<%= esc(baseUrl) %>/save">
        <input type="hidden" name="_csrf" value="<%= esc(csrf) %>">
        <input type="hidden" name="id" value="<%= v.getVoucherId() == 0 ? "" : v.getVoucherId() %>">
        <div class="cf-form-group">
            <label class="cf-form-label">Mã voucher</label>
            <input class="cf-form-control" type="text" name="code" maxlength="50" value="<%= esc(v.getVoucherCode()) %>" <%= locked ? "readonly" : "" %>>
            <% if (locked) { %><div class="cf-help">Voucher đã được sử dụng nên không đổi được mã.</div><% } %>
            <% if (errors != null && errors.get("code") != null) { %><div class="vc-error"><%= esc(errors.get("code")) %></div><% } %>
        </div>
        <div class="cf-form-group">
            <label class="cf-form-label">Tên voucher</label>
            <input class="cf-form-control" type="text" name="name" maxlength="150" value="<%= esc(v.getVoucherName()) %>">
            <% if (errors != null && errors.get("name") != null) { %><div class="vc-error"><%= esc(errors.get("name")) %></div><% } %>
        </div>
        <div class="cf-form-group">
            <label class="cf-form-label">Mô tả</label>
            <textarea class="cf-form-textarea" name="description" rows="3" maxlength="500"><%= esc(v.getDescription()) %></textarea>
            <% if (errors != null && errors.get("description") != null) { %><div class="vc-error"><%= esc(errors.get("description")) %></div><% } %>
        </div>
        <div class="cf-form-group">
            <label class="cf-form-label">Loại giảm giá</label>
            <select class="cf-form-control" name="type">
                <option value="PERCENT" <%= "PERCENT".equals(v.getDiscountType()) ? "selected" : "" %>>Phần trăm (%)</option>
                <option value="FIXED" <%= "FIXED".equals(v.getDiscountType()) ? "selected" : "" %>>Số tiền cố định (đ)</option>
            </select>
            <% if (errors != null && errors.get("type") != null) { %><div class="vc-error"><%= esc(errors.get("type")) %></div><% } %>
        </div>
        <div class="cf-form-group">
            <label class="cf-form-label">Giá trị giảm</label>
            <input class="cf-form-control" type="text" name="discountValue" value="<%= esc(discountValue) %>">
            <% if (errors != null && errors.get("discountValue") != null) { %><div class="vc-error"><%= esc(errors.get("discountValue")) %></div><% } %>
        </div>
        <div class="cf-form-group">
            <label class="cf-form-label">Giá trị đơn tối thiểu (đ)</label>
            <input class="cf-form-control" type="text" name="minimumOrderValue" value="<%= esc(minimum) %>">
            <% if (errors != null && errors.get("minimumOrderValue") != null) { %><div class="vc-error"><%= esc(errors.get("minimumOrderValue")) %></div><% } %>
        </div>
        <div class="cf-form-group">
            <label class="cf-form-label">Mức giảm tối đa (đ, để trống nếu không giới hạn)</label>
            <input class="cf-form-control" type="text" name="maximumDiscount" value="<%= esc(maximum) %>">
            <% if (errors != null && errors.get("maximumDiscount") != null) { %><div class="vc-error"><%= esc(errors.get("maximumDiscount")) %></div><% } %>
        </div>
        <div class="cf-form-group">
            <label class="cf-form-label">Bắt đầu</label>
            <input class="cf-form-control" type="datetime-local" name="startDate" value="<%= esc(startValue) %>">
            <% if (errors != null && errors.get("startDate") != null) { %><div class="vc-error"><%= esc(errors.get("startDate")) %></div><% } %>
        </div>
        <div class="cf-form-group">
            <label class="cf-form-label">Kết thúc</label>
            <input class="cf-form-control" type="datetime-local" name="endDate" value="<%= esc(endValue) %>">
            <% if (errors != null && errors.get("endDate") != null) { %><div class="vc-error"><%= esc(errors.get("endDate")) %></div><% } %>
        </div>
        <div class="cf-form-group">
            <label class="cf-form-label">Giới hạn lượt dùng (để trống nếu không giới hạn)</label>
            <input class="cf-form-control" type="text" name="usageLimit" value="<%= esc(limit) %>">
            <% if (v.getVoucherId() > 0) { %><div class="cf-help">Đã dùng: <%= v.getUsedCount() %></div><% } %>
            <% if (errors != null && errors.get("usageLimit") != null) { %><div class="vc-error"><%= esc(errors.get("usageLimit")) %></div><% } %>
        </div>
        <div class="cf-form-group">
            <label class="cf-form-label">Trạng thái</label>
            <select class="cf-form-control" name="status">
                <option value="ACTIVE" <%= "ACTIVE".equals(v.getStatus()) ? "selected" : "" %>>Hoạt động</option>
                <option value="INACTIVE" <%= "INACTIVE".equals(v.getStatus()) ? "selected" : "" %>>Tạm ngưng</option>
            </select>
            <% if (errors != null && errors.get("status") != null) { %><div class="vc-error"><%= esc(errors.get("status")) %></div><% } %>
        </div>
        <button class="cf-btn cf-btn-primary" type="submit">Lưu</button>
    </form>
</div>
<%@ include file="footer.jspf" %>
