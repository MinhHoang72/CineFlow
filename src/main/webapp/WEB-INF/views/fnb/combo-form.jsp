<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.FnbCombo, model.FnbItem, model.ComboLine, java.util.List, java.util.Map" %>
<%@ include file="head.jspf" %>
<%
    FnbCombo combo = (FnbCombo) request.getAttribute("combo");
    List<FnbItem> items = (List<FnbItem>) request.getAttribute("items");
    Map<String, String> errors = (Map<String, String>) request.getAttribute("errors");
    String csrf = (String) request.getAttribute("csrf");
    String comboStatus = combo.getStatus() == null ? "ACTIVE" : combo.getStatus();
%>
<div class="cf-page-header">
    <div>
        <h1><%= combo.getComboId() == 0 ? "Tạo combo" : "Sửa combo" %></h1>
        <p>Combo gồm các món đang bán của Centre bạn.</p>
    </div>
    <a class="cf-btn cf-btn-ghost" href="<%= ctx %>/fnb">Quay lại</a>
</div>
<div class="cf-card">
    <form class="fnb-form" method="post" enctype="multipart/form-data" action="<%= ctx %>/fnb/combo/save">
        <input type="hidden" name="_csrf" value="<%= esc(csrf) %>">
        <input type="hidden" name="id" value="<%= combo.getComboId() == 0 ? "" : combo.getComboId() %>">
        <div class="cf-form-group">
            <label class="cf-form-label">Tên combo</label>
            <input class="cf-form-control" type="text" name="name" maxlength="100" value="<%= esc(combo.getComboName()) %>">
            <% if (errors != null && errors.get("name") != null) { %><div class="fnb-error"><%= esc(errors.get("name")) %></div><% } %>
        </div>
        <div class="cf-form-group">
            <label class="cf-form-label">Giá (đ)</label>
            <input class="cf-form-control" type="text" name="price" value="<%= combo.getPrice() == null ? "" : combo.getPrice().stripTrailingZeros().toPlainString() %>">
            <% if (errors != null && errors.get("price") != null) { %><div class="fnb-error"><%= esc(errors.get("price")) %></div><% } %>
        </div>
        <div class="cf-form-group">
            <label class="cf-form-label">Mô tả</label>
            <textarea class="cf-form-textarea" name="description" rows="3" maxlength="500"><%= esc(combo.getDescription()) %></textarea>
            <% if (errors != null && errors.get("description") != null) { %><div class="fnb-error"><%= esc(errors.get("description")) %></div><% } %>
        </div>
        <div class="cf-form-group">
            <label class="cf-form-label">Hình ảnh</label>
            <% if (combo.getImageUrl() != null && !combo.getImageUrl().isEmpty()) { %>
            <div style="margin-bottom: 8px;">
                <img class="fnb-preview" src="<%= esc(combo.getImageUrl()) %>" alt="">
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
                <option value="ACTIVE" <%= "ACTIVE".equals(comboStatus) ? "selected" : "" %>>Đang bán</option>
                <option value="INACTIVE" <%= "INACTIVE".equals(comboStatus) ? "selected" : "" %>>Lưu trữ</option>
            </select>
            <% if (errors != null && errors.get("status") != null) { %><div class="fnb-error"><%= esc(errors.get("status")) %></div><% } %>
        </div>
        <div class="cf-form-group">
            <label class="cf-form-label">Các món trong combo</label>
            <div class="fnb-lines" id="lines">
                <% for (ComboLine line : combo.getLines()) { %>
                <div class="fnb-line">
                    <select class="cf-form-control" name="lineItem">
                        <option value="">-- Chọn món --</option>
                        <% for (FnbItem i : items) { %>
                        <option value="<%= i.getFnbItemId() %>" <%= i.getFnbItemId() == line.getFnbItemId() ? "selected" : "" %>><%= esc(i.getItemName()) %></option>
                        <% } %>
                    </select>
                    <input class="cf-form-control" type="number" name="lineQty" min="1" max="99" value="<%= line.getQuantity() %>" style="width: 90px;">
                    <button type="button" class="cf-btn cf-btn-sm cf-btn-danger" onclick="this.parentNode.remove()">Xóa</button>
                </div>
                <% } %>
            </div>
            <button type="button" class="cf-btn cf-btn-sm cf-btn-secondary" id="addLine" style="margin-top: 8px;">Thêm dòng</button>
            <% if (errors != null && errors.get("lines") != null) { %><div class="fnb-error"><%= esc(errors.get("lines")) %></div><% } %>
        </div>
        <button class="cf-btn cf-btn-primary" type="submit">Lưu</button>
    </form>
</div>
<template id="lineTemplate">
    <div class="fnb-line">
        <select class="cf-form-control" name="lineItem">
            <option value="">-- Chọn món --</option>
            <% for (FnbItem i : items) { %>
            <option value="<%= i.getFnbItemId() %>"><%= esc(i.getItemName()) %></option>
            <% } %>
        </select>
        <input class="cf-form-control" type="number" name="lineQty" min="1" max="99" value="1" style="width: 90px;">
        <button type="button" class="cf-btn cf-btn-sm cf-btn-danger" onclick="this.parentNode.remove()">Xóa</button>
    </div>
</template>
<script>
    var lines = document.getElementById('lines');
    var template = document.getElementById('lineTemplate');
    document.getElementById('addLine').addEventListener('click', function () {
        lines.appendChild(template.content.cloneNode(true));
    });
    if (!lines.children.length) {
        lines.appendChild(template.content.cloneNode(true));
    }
</script>
<%@ include file="foot.jspf" %>
