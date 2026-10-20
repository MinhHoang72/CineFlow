<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.dto.StockRowDTO, java.util.List" %>
<%@ include file="header.jspf" %>
<%
    List<StockRowDTO> rows = (List<StockRowDTO>) request.getAttribute("rows");
    boolean canEdit = Boolean.TRUE.equals(request.getAttribute("canEdit"));
    String csrf = (String) request.getAttribute("csrf");
%>
<div class="cf-page-header">
    <div>
        <h1>Kho theo Centre</h1>
        <p><%= canEdit ? "Bật/tắt tình trạng còn hàng của từng món." : "Tình trạng còn hàng của các món (chỉ xem)." %></p>
    </div>
</div>
<div class="cf-card">
    <div class="cf-table-wrap">
        <table class="cf-table">
            <thead>
                <tr>
                    <th>Món</th>
                    <th>Danh mục</th>
                    <th>Tình trạng</th>
                    <% if (canEdit) { %><th>Thao tác</th><% } %>
                </tr>
            </thead>
            <tbody>
            <% if (rows.isEmpty()) { %>
                <tr><td colspan="4" class="cf-empty">Centre chưa có món nào.</td></tr>
            <% } %>
            <% for (StockRowDTO r : rows) { %>
                <tr>
                    <td><strong><%= esc(r.getItemName()) %></strong></td>
                    <td><%= esc(r.getCategoryName()) %></td>
                    <td><span class="cf-badge <%= r.isAvailable() ? "cf-badge-success" : "cf-badge-danger" %>"><%= r.isAvailable() ? "Còn hàng" : "Hết hàng" %></span></td>
                    <% if (canEdit) { %>
                    <td>
                        <form class="fnb-inline" method="post" action="<%= ctx %>/fnb/stock/toggle">
                            <input type="hidden" name="_csrf" value="<%= esc(csrf) %>">
                            <input type="hidden" name="itemId" value="<%= r.getFnbItemId() %>">
                            <input type="hidden" name="available" value="<%= r.isAvailable() ? "false" : "true" %>">
                            <button class="cf-btn cf-btn-sm <%= r.isAvailable() ? "cf-btn-danger" : "cf-btn-primary" %>" type="submit"><%= r.isAvailable() ? "Đặt hết hàng" : "Đặt còn hàng" %></button>
                        </form>
                    </td>
                    <% } %>
                </tr>
            <% } %>
            </tbody>
        </table>
    </div>
</div>
<%@ include file="footer.jspf" %>
