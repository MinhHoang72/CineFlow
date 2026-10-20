<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.dto.PageDTO, model.entity.Voucher" %>
<%@ include file="head.jspf" %>
<%
    PageDTO<Voucher> result = (PageDTO<Voucher>) request.getAttribute("result");
    String q = (String) request.getAttribute("q");
    String status = (String) request.getAttribute("status");
    String type = (String) request.getAttribute("type");
    String pagerBase = (String) request.getAttribute("pagerBase");
    String backQuery = (String) request.getAttribute("backQuery");
    String csrf = (String) request.getAttribute("csrf");
    java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
%>
<div class="cf-page-header">
    <div>
        <h1>Voucher</h1>
        <p>Quản lý mã giảm giá áp dụng cho toàn chuỗi.</p>
    </div>
    <a class="cf-btn cf-btn-primary" href="<%= ctx %>/admin/vouchers/form">Thêm voucher</a>
</div>

<div class="cf-card">
    <form class="vc-filter" method="get" action="<%= ctx %>/admin/vouchers">
        <label>Tìm kiếm
            <input class="cf-form-control" type="text" name="q" maxlength="100" value="<%= esc(q) %>" placeholder="Mã hoặc tên">
        </label>
        <label>Trạng thái
            <select class="cf-form-control" name="status">
                <option value="">Tất cả</option>
                <option value="ACTIVE" <%= "ACTIVE".equals(status) ? "selected" : "" %>>Đang hiệu lực</option>
                <option value="INACTIVE" <%= "INACTIVE".equals(status) ? "selected" : "" %>>Tạm ngưng</option>
                <option value="EXPIRED" <%= "EXPIRED".equals(status) ? "selected" : "" %>>Hết hạn</option>
                <option value="EXHAUSTED" <%= "EXHAUSTED".equals(status) ? "selected" : "" %>>Hết lượt</option>
            </select>
        </label>
        <label>Loại
            <select class="cf-form-control" name="type">
                <option value="">Tất cả</option>
                <option value="PERCENT" <%= "PERCENT".equals(type) ? "selected" : "" %>>Phần trăm</option>
                <option value="FIXED" <%= "FIXED".equals(type) ? "selected" : "" %>>Số tiền</option>
            </select>
        </label>
        <button class="cf-btn cf-btn-primary" type="submit">Lọc</button>
        <a class="cf-btn cf-btn-ghost" href="<%= ctx %>/admin/vouchers">Xóa lọc</a>
    </form>

    <div class="cf-table-wrap">
        <table class="cf-table">
            <thead>
                <tr>
                    <th>Mã</th>
                    <th>Tên</th>
                    <th>Giảm</th>
                    <th>Đơn tối thiểu</th>
                    <th>Thời gian</th>
                    <th>Đã dùng</th>
                    <th>Trạng thái</th>
                    <th>Thao tác</th>
                </tr>
            </thead>
            <tbody>
            <% if (result.getItems().isEmpty()) { %>
                <tr><td colspan="8" class="cf-empty">Không có voucher nào.</td></tr>
            <% } %>
            <% for (Voucher v : result.getItems()) {
                boolean active = "ACTIVE".equals(v.getStatus());
                String label;
                String badge;
                if (!active) {
                    label = "Tạm ngưng";
                    badge = "cf-badge-muted";
                } else if (v.isExpired()) {
                    label = "Hết hạn";
                    badge = "cf-badge-warning";
                } else if (v.isExhausted()) {
                    label = "Hết lượt";
                    badge = "cf-badge-warning";
                } else {
                    label = "Đang hiệu lực";
                    badge = "cf-badge-success";
                }
            %>
                <tr>
                    <td><strong><%= esc(v.getVoucherCode()) %></strong></td>
                    <td><%= esc(v.getVoucherName()) %></td>
                    <td>
                        <% if ("PERCENT".equals(v.getDiscountType())) { %>
                            <%= v.getDiscountValue().stripTrailingZeros().toPlainString() %>%<%= v.getMaximumDiscount() == null ? "" : " (tối đa " + money(v.getMaximumDiscount()) + ")" %>
                        <% } else { %>
                            <%= money(v.getDiscountValue()) %>
                        <% } %>
                    </td>
                    <td><%= money(v.getMinimumOrderValue()) %></td>
                    <td><%= v.getStartDate().format(fmt) %><br><%= v.getEndDate().format(fmt) %></td>
                    <td><%= v.getUsedCount() %><%= v.getUsageLimit() == null ? "" : " / " + v.getUsageLimit() %></td>
                    <td><span class="cf-badge <%= badge %>"><%= label %></span></td>
                    <td class="cf-table-actions">
                        <a class="cf-btn cf-btn-sm cf-btn-secondary" href="<%= ctx %>/admin/vouchers/form?id=<%= v.getVoucherId() %>">Sửa</a>
                        <form class="vc-inline" method="post" action="<%= ctx %>/admin/vouchers/status">
                            <input type="hidden" name="_csrf" value="<%= esc(csrf) %>">
                            <input type="hidden" name="id" value="<%= v.getVoucherId() %>">
                            <input type="hidden" name="newStatus" value="<%= active ? "INACTIVE" : "ACTIVE" %>">
                            <input type="hidden" name="back" value="<%= esc(backQuery) %>">
                            <button class="cf-btn cf-btn-sm <%= active ? "cf-btn-danger" : "cf-btn-primary" %>" type="submit"><%= active ? "Tạm ngưng" : "Kích hoạt" %></button>
                        </form>
                    </td>
                </tr>
            <% } %>
            </tbody>
        </table>
    </div>

    <div class="vc-pager">
        <span class="info">Hiển thị <%= result.getFrom() %>-<%= result.getTo() %> trên tổng <%= result.getTotal() %> kết quả</span>
        <%= pager(ctx, pagerBase, result.getPage(), result.getTotalPages()) %>
    </div>
</div>
<%@ include file="foot.jspf" %>
