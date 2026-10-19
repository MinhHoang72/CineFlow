<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.FnbItem, model.OptionDTO, java.util.List, java.util.Map" %>
<%@ include file="head.jspf" %>
<%
    FnbItem item = (FnbItem) request.getAttribute("item");
    List<OptionDTO> categories = (List<OptionDTO>) request.getAttribute("categories");
    Map<String, String> errors = (Map<String, String>) request.getAttribute("errors");
    String csrf = (String) request.getAttribute("csrf");
    String itemStatus = item.getStatus() == null ? "ACTIVE" : item.getStatus();
%>
<div class="cf-page-header">
    <div>
        <h1><%= item.getFnbItemId() == 0 ? "Thêm món" : "Sửa món" %></h1>
        <p>Món thuộc Centre của bạn.</p>
    </div>
    <a class="cf-btn cf-btn-ghost" href="<%= ctx %>/fnb">Quay lại</a>
</div>
<div class="cf-card">
    <form class="fnb-form" method="post" enctype="multipart/form-data" action="<%= ctx %>/fnb/item/save">
        <input type="hidden" name="_csrf" value="<%= esc(csrf) %>">
        <input type="hidden" name="id" value="<%= item.getFnbItemId() == 0 ? "" : item.getFnbItemId() %>">
        <div class="cf-form-group">
            <label class="cf-form-label">Tên món</label>
            <input class="cf-form-control" type="text" name="name" maxlength="100" value="<%= esc(item.getItemName()) %>">
            <% if (errors != null && errors.get("name") != null) { %><div class="fnb-error"><%= esc(errors.get("name")) %></div><% } %>
        </div>
        <div class="cf-form-group">
            <label class="cf-form-label">Danh mục</label>
            <select class="cf-form-control" name="categoryId">
                <option value="">-- Chọn --</option>
                <% for (OptionDTO c : categories) { %>
                <option value="<%= c.getId() %>" <%= c.getId() == item.getCategoryId() ? "selected" : "" %>><%= esc(c.getLabel()) %></option>
                <% } %>
            </select>
            <% if (errors != null && errors.get("categoryId") != null) { %><div class="fnb-error"><%= esc(errors.get("categoryId")) %></div><% } %>
        </div>
        <div class="cf-form-group">
            <label class="cf-form-label">Giá (đ)</label>
            <input class="cf-form-control" type="text" name="price" value="<%= item.getPrice() == null ? "" : item.getPrice().stripTrailingZeros().toPlainString() %>">
            <% if (errors != null && errors.get("price") != null) { %><div class="fnb-error"><%= esc(errors.get("price")) %></div><% } %>
        </div>
        <div class="cf-form-group">
            <label class="cf-form-label">Mô tả</label>
            <textarea class="cf-form-textarea" name="description" rows="3" maxlength="500"><%= esc(item.getDescription()) %></textarea>
            <% if (errors != null && errors.get("description") != null) { %><div class="fnb-error"><%= esc(errors.get("description")) %></div><% } %>
        </div>
        <div class="cf-form-group">
            <label class="cf-form-label">Hình ảnh</label>
            <% if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) { %>
            <div style="margin-bottom: 8px;">
                <img class="fnb-preview" src="<%= esc(item.getImageUrl()) %>" alt="">
                <label style="display: block; margin-top: 6px;"><input type="checkbox" name="removeImage" value="1" style="width: auto; min-height: 0;"> Xóa ảnh hiện tại</label>
            </div>
            <% } %>
            <input class="cf-form-control" type="file" name="image" accept="image/png,image/jpeg,image/webp">
            <div class="cf-help">JPG, PNG hoặc WEBP, tối đa 2 MB. Để trống nếu không đổi ảnh.</div>
            <% if (errors != null && errors.get("image") != null) { %><div class="fnb-error"><%= esc(errors.get("image")) %></div><% } %>
        </div>
        <div class="cf-form-group">
            <label class="cf-form-label">Trạng thái</label>
            <select class="cf-form-control" name="status">
                <option value="ACTIVE" <%= "ACTIVE".equals(itemStatus) ? "selected" : "" %>>Đang bán</option>
                <option value="INACTIVE" <%= "INACTIVE".equals(itemStatus) ? "selected" : "" %>>Lưu trữ</option>
            </select>
            <% if (errors != null && errors.get("status") != null) { %><div class="fnb-error"><%= esc(errors.get("status")) %></div><% } %>
        </div>
        <button class="cf-btn cf-btn-primary" type="submit">Lưu</button>
    </form>
</div>
<%@ include file="foot.jspf" %>
