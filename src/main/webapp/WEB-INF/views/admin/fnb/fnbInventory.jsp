<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.dto.fnb.StockRowDTO, java.util.List" %>
<%@ include file="header.jspf" %>
<%
    List<StockRowDTO> rows = (List<StockRowDTO>) request.getAttribute("rows");
    boolean canEdit = Boolean.TRUE.equals(request.getAttribute("canEdit"));
    String csrf = (String) request.getAttribute("csrf");
%>
<div class="cf-page-header">
    <div>
        <h1>Kho theo Centre</h1>
        <p>
            <% if (canEdit) { %>
                Quản lý số lượng tồn kho và trạng thái phục vụ của chi nhánh. Nhập số lượng 0 sẽ tự động chuyển thành <strong>Hết hàng</strong>; có thể bấm nút để bật/tắt bán khẩn cấp.
            <% } else { %>
                Tình trạng tồn kho các món tại chi nhánh (nhân viên bán vé chỉ xem).
            <% } %>
        </p>
    </div>
</div>

<div class="cf-card">
    <div class="cf-table-wrap">
        <table class="cf-table">
            <thead>
                <tr>
                    <th>Món</th>
                    <th>Danh mục</th>
                    <th style="min-width: 180px;">Số lượng tồn kho</th>
                    <th>Tình trạng</th>
                    <% if (canEdit) { %>
                    <th style="min-width: 150px;">Bật/Tắt bán</th>
                    <% } %>
                </tr>
            </thead>
            <tbody>
            <% if (rows == null || rows.isEmpty()) { %>
                <tr>
                    <td colspan="<%= canEdit ? 5 : 4 %>" class="cf-empty">Chi nhánh chưa có món bắp nước nào.</td>
                </tr>
            <% } else { %>
                <% for (StockRowDTO r : rows) { 
                    boolean isOut = !r.isAvailable() || r.getStockQuantity() <= 0;
                    String badgeClass = r.isAvailable() ? "cf-badge-success" : "cf-badge-danger";
                    String statusLabel;
                    if (r.isAvailable() && r.getStockQuantity() > 0) {
                        statusLabel = "Còn hàng";
                    } else if (!r.isAvailable() && r.getStockQuantity() > 0) {
                        statusLabel = "Tạm ngưng bán";
                    } else {
                        statusLabel = "Hết hàng";
                    }
                %>
                <tr>
                    <td><strong><%= esc(r.getItemName()) %></strong></td>
                    <td><%= esc(r.getCategoryName()) %></td>
                    <td>
                        <% if (canEdit) { %>
                        <form class="fnb-inline-qty" method="post" action="<%= ctx %>/fnb/stock/quantity">
                            <input type="hidden" name="_csrf" value="<%= esc(csrf) %>">
                            <input type="hidden" name="itemId" value="<%= r.getFnbItemId() %>">
                            <input class="cf-form-control fnb-qty-input" type="number" name="quantity" 
                                   min="0" max="999999" value="<%= r.getStockQuantity() %>" required>
                            <button class="cf-btn cf-btn-sm cf-btn-secondary" type="submit" title="Lưu số lượng tồn kho">Lưu</button>
                        </form>
                        <% } else { %>
                            <span class="fnb-stock-stat"><%= r.getStockQuantity() %></span>
                        <% } %>
                    </td>
                    <td>
                        <span class="cf-badge <%= badgeClass %>"><%= statusLabel %></span>
                    </td>
                    <% if (canEdit) { %>
                    <td>
                        <form class="fnb-inline" method="post" action="<%= ctx %>/fnb/stock/toggle">
                            <input type="hidden" name="_csrf" value="<%= esc(csrf) %>">
                            <input type="hidden" name="itemId" value="<%= r.getFnbItemId() %>">
                            <input type="hidden" name="available" value="<%= r.isAvailable() ? "false" : "true" %>">
                            <% if (r.isAvailable()) { %>
                                <button class="cf-btn cf-btn-sm cf-btn-danger" type="submit" title="Tạm ngưng phục vụ món này">
                                    Tắt (Báo hết)
                                </button>
                            <% } else { %>
                                <button class="cf-btn cf-btn-sm cf-btn-primary" type="submit" title="Mở bán lại món này">
                                    Bật (Còn hàng)
                                </button>
                            <% } %>
                        </form>
                    </td>
                    <% } %>
                </tr>
                <% } %>
            <% } %>
            </tbody>
        </table>
    </div>
</div>
<%@ include file="footer.jspf" %>
