<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.dto.PageDTO, model.dto.FnbRowDTO, model.dto.OptionDTO, java.util.List" %>
<%@ include file="header.jspf" %>
<%
    PageDTO<FnbRowDTO> result = (PageDTO<FnbRowDTO>) request.getAttribute("result");
    List<OptionDTO> centres = (List<OptionDTO>) request.getAttribute("centres");
    Integer selectedCentre = (Integer) request.getAttribute("selectedCentre");
    boolean canEdit = Boolean.TRUE.equals(request.getAttribute("canEdit"));
    boolean isAdmin = Boolean.TRUE.equals(request.getAttribute("isAdmin"));
    String q = (String) request.getAttribute("q");
    String status = (String) request.getAttribute("status");
    String sort = (String) request.getAttribute("sort");
    String minPrice = (String) request.getAttribute("minPrice");
    String maxPrice = (String) request.getAttribute("maxPrice");
    String pagerBase = (String) request.getAttribute("pagerBase");
    String csrf = (String) request.getAttribute("csrf");
%>
<div class="cf-page-header">
    <div>
        <h1>Danh sách Combo Bắp Nước</h1>
        <p><%= isAdmin ? "Xem danh sách combo của các cụm rạp (chỉ xem)." : "Quản lý các gói combo ưu đãi của cụm rạp." %></p>
    </div>
    <% if (canEdit) { %>
    <div>
        <a class="cf-btn cf-btn-secondary" href="<%= ctx %>/fnb">Tất cả món</a>
        <a class="cf-btn cf-btn-primary" href="<%= ctx %>/fnb/combo">+ Tạo combo mới</a>
    </div>
    <% } %>
</div>

<div class="cf-card">
    <form class="fnb-filter" method="get" action="<%= ctx %>/fnb">
        <input type="hidden" name="category" value="COMBO">
        <label>Tìm kiếm
            <input class="cf-form-control" type="text" name="q" maxlength="100" value="<%= esc(q) %>" placeholder="Tên combo...">
        </label>
        <% if (isAdmin && centres != null) { %>
        <label>Centre
            <select class="cf-form-control" name="centreId">
                <option value="">Tất cả</option>
                <% for (OptionDTO c : centres) { %>
                <option value="<%= c.getId() %>" <%= selectedCentre != null && selectedCentre == c.getId() ? "selected" : "" %>><%= esc(c.getLabel()) %></option>
                <% } %>
            </select>
        </label>
        <% } %>
        <label>Trạng thái
            <select class="cf-form-control" name="status">
                <option value="">Tất cả</option>
                <option value="ACTIVE" <%= "ACTIVE".equals(status) ? "selected" : "" %>>Đang bán</option>
                <option value="INACTIVE" <%= "INACTIVE".equals(status) ? "selected" : "" %>>Ngừng bán</option>
            </select>
        </label>
        <button class="cf-btn cf-btn-secondary" type="submit">Lọc</button>
    </form>
</div>

<div class="cf-card">
    <table class="cf-table">
        <thead>
            <tr>
                <th style="width:70px">Ảnh</th>
                <th>Tên Combo</th>
                <th>Giá bán</th>
                <th>Trạng thái</th>
                <% if (canEdit) { %><th style="width:140px">Thao tác</th><% } %>
            </tr>
        </thead>
        <tbody>
        <% if (result == null || result.getItems() == null || result.getItems().isEmpty()) { %>
            <tr><td colspan="<%= canEdit ? 5 : 4 %>" style="text-align:center;padding:32px;color:var(--cf-muted)">Không có combo nào phù hợp.</td></tr>
        <% } else {
            for (FnbRowDTO r : result.getItems()) { %>
            <tr>
                <td>
                    <% if (r.getImage() != null && !r.getImage().isBlank()) { %>
                        <img src="<%= ctx %>/uploads/<%= esc(r.getImage()) %>" alt="" style="width:48px;height:48px;object-fit:cover;border-radius:6px;">
                    <% } else { %>
                        <div style="width:48px;height:48px;background:var(--cf-card);border-radius:6px;display:flex;align-items:center;justify-content:center;color:var(--cf-muted)">🍿</div>
                    <% } %>
                </td>
                <td><strong><%= esc(r.getName()) %></strong></td>
                <td><%= r.getPrice() != null ? String.format("%,d đ", r.getPrice().longValue()) : "—" %></td>
                <td>
                    <span class="cf-badge <%= "ACTIVE".equals(r.getStatus()) ? "cf-badge-success" : "cf-badge-muted" %>">
                        <%= "ACTIVE".equals(r.getStatus()) ? "Đang bán" : "Ngừng bán" %>
                    </span>
                </td>
                <% if (canEdit) { %>
                <td>
                    <a class="cf-btn cf-btn-sm cf-btn-secondary" href="<%= ctx %>/fnb/combo?id=<%= r.getId() %>">Sửa</a>
                </td>
                <% } %>
            </tr>
        <%  }
        } %>
        </tbody>
    </table>
</div>

<%@ include file="footer.jspf" %>
