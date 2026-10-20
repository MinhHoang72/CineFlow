<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.PageDTO, model.FnbRowDTO, model.OptionDTO, java.util.List" %>
<%@ include file="head.jspf" %>
<%
    PageDTO<FnbRowDTO> result = (PageDTO<FnbRowDTO>) request.getAttribute("result");
    List<OptionDTO> categories = (List<OptionDTO>) request.getAttribute("categories");
    List<OptionDTO> centres = (List<OptionDTO>) request.getAttribute("centres");
    Integer selectedCentre = (Integer) request.getAttribute("selectedCentre");
    boolean canEdit = Boolean.TRUE.equals(request.getAttribute("canEdit"));
    boolean isAdmin = Boolean.TRUE.equals(request.getAttribute("isAdmin"));
    String q = (String) request.getAttribute("q");
    String category = (String) request.getAttribute("category");
    String status = (String) request.getAttribute("status");
    String sort = (String) request.getAttribute("sort");
    String minPrice = (String) request.getAttribute("minPrice");
    String maxPrice = (String) request.getAttribute("maxPrice");
    String pagerBase = (String) request.getAttribute("pagerBase");
    String backQuery = (String) request.getAttribute("backQuery");
    String csrf = (String) request.getAttribute("csrf");
%>
<div class="cf-page-header">
    <div>
        <h1>Món &amp; Combo</h1>
        <p><%= isAdmin ? "Xem thực đơn của các Centre (chỉ xem)." : "Thực đơn bắp nước của Centre bạn." %></p>
    </div>
    <% if (canEdit) { %>
    <div>
        <a class="cf-btn cf-btn-primary" href="<%= ctx %>/fnb/item">Thêm món</a>
        <a class="cf-btn cf-btn-secondary" href="<%= ctx %>/fnb/combo">Tạo combo</a>
    </div>
    <% } %>
</div>

<div class="cf-card">
    <form class="fnb-filter" method="get" action="<%= ctx %>/fnb">
        <label>Tìm kiếm
            <input class="cf-form-control" type="text" name="q" maxlength="100" value="<%= esc(q) %>">
        </label>
        <% if (isAdmin) { %>
        <label>Centre
            <select class="cf-form-control" name="centreId">
                <option value="">Tất cả</option>
                <% for (OptionDTO c : centres) { %>
                <option value="<%= c.getId() %>" <%= selectedCentre != null && selectedCentre == c.getId() ? "selected" : "" %>><%= esc(c.getLabel()) %></option>
                <% } %>
            </select>
        </label>
        <% } %>
        <label>Danh mục
            <select class="cf-form-control" name="category">
                <option value="">Tất cả</option>
                <option value="COMBO" <%= "COMBO".equals(category) ? "selected" : "" %>>Combo</option>
                <% for (OptionDTO c : categories) { %>
                <option value="<%= c.getId() %>" <%= String.valueOf(c.getId()).equals(category) ? "selected" : "" %>><%= esc(c.getLabel()) %></option>
                <% } %>
            </select>
        </label>
        <label>Trạng thái
            <select class="cf-form-control" name="status">
                <option value="">Tất cả</option>
                <option value="ACTIVE" <%= "ACTIVE".equals(status) ? "selected" : "" %>>Đang bán</option>
                <option value="INACTIVE" <%= "INACTIVE".equals(status) ? "selected" : "" %>>Lưu trữ</option>
            </select>
        </label>
        <label>Giá từ
            <input class="cf-form-control" type="number" min="0" step="any" name="minPrice" value="<%= esc(minPrice) %>">
        </label>
        <label>Giá đến
            <input class="cf-form-control" type="number" min="0" step="any" name="maxPrice" value="<%= esc(maxPrice) %>">
        </label>
        <label>Sắp xếp
            <select class="cf-form-control" name="sort">
                <option value="">Mặc định</option>
                <option value="name" <%= "name".equals(sort) ? "selected" : "" %>>Tên A-Z</option>
                <option value="price_asc" <%= "price_asc".equals(sort) ? "selected" : "" %>>Giá tăng</option>
                <option value="price_desc" <%= "price_desc".equals(sort) ? "selected" : "" %>>Giá giảm</option>
                <option value="newest" <%= "newest".equals(sort) ? "selected" : "" %>>Mới nhất</option>
            </select>
        </label>
        <button class="cf-btn cf-btn-primary" type="submit">Lọc</button>
        <a class="cf-btn cf-btn-ghost" href="<%= ctx %>/fnb">Xóa lọc</a>
    </form>

    <div class="cf-table-wrap">
        <table class="cf-table">
            <thead>
                <tr>
                    <th>Ảnh</th>
                    <th>Tên</th>
                    <th>Loại</th>
                    <th>Danh mục</th>
                    <% if (isAdmin) { %><th>Centre</th><% } %>
                    <th>Giá</th>
                    <th>Trạng thái</th>
                    <% if (canEdit) { %><th>Thao tác</th><% } %>
                </tr>
            </thead>
            <tbody>
            <% if (result.getItems().isEmpty()) { %>
                <tr><td colspan="8" class="cf-empty">Không có món hoặc combo nào.</td></tr>
            <% } %>
            <% for (FnbRowDTO r : result.getItems()) { %>
                <tr>
                    <td><% if (r.getImageUrl() != null && !r.getImageUrl().isEmpty()) { %><img class="fnb-thumb" src="<%= esc(r.getImageUrl()) %>" alt=""><% } else { %><span class="fnb-thumb fnb-noimg">&#127871;</span><% } %></td>
                    <td><strong><%= esc(r.getName()) %></strong></td>
                    <td><%= r.isCombo() ? "Combo" : "Món" %></td>
                    <td><%= esc(r.getCategoryName()) %></td>
                    <% if (isAdmin) { %><td><%= esc(r.getCentreName()) %></td><% } %>
                    <td><%= money(r.getPrice()) %></td>
                    <td><span class="cf-badge <%= r.isActive() ? "cf-badge-success" : "cf-badge-muted" %>"><%= r.isActive() ? "Đang bán" : "Lưu trữ" %></span></td>
                    <% if (canEdit) { %>
                    <td class="cf-table-actions">
                        <a class="cf-btn cf-btn-sm cf-btn-secondary" href="<%= ctx %>/fnb/<%= r.isCombo() ? "combo" : "item" %>?id=<%= r.getId() %>">Sửa</a>
                        <form class="fnb-inline" method="post" action="<%= ctx %>/fnb/status">
                            <input type="hidden" name="_csrf" value="<%= esc(csrf) %>">
                            <input type="hidden" name="kind" value="<%= r.isCombo() ? "COMBO" : "ITEM" %>">
                            <input type="hidden" name="id" value="<%= r.getId() %>">
                            <input type="hidden" name="newStatus" value="<%= r.isActive() ? "INACTIVE" : "ACTIVE" %>">
                            <input type="hidden" name="back" value="<%= esc(backQuery) %>">
                            <button class="cf-btn cf-btn-sm <%= r.isActive() ? "cf-btn-danger" : "cf-btn-primary" %>" type="submit"><%= r.isActive() ? "Lưu trữ" : "Khôi phục" %></button>
                        </form>
                    </td>
                    <% } %>
                </tr>
            <% } %>
            </tbody>
        </table>
    </div>

    <div class="fnb-pager">
        <span class="info">Hiển thị <%= result.getFrom() %>-<%= result.getTo() %> trên tổng <%= result.getTotal() %> kết quả</span>
        <%= pager(ctx, pagerBase, result.getPage(), result.getTotalPages()) %>
    </div>
</div>
<%@ include file="foot.jspf" %>
