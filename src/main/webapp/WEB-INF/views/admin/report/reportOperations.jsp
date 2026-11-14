<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.dto.report.OperationsSummaryDTO, model.dto.showtime.ShowtimeStatDTO, model.dto.common.PageDTO, java.util.List" %>
<%@ include file="header.jspf" %>
<%
    OperationsSummaryDTO s = (OperationsSummaryDTO) request.getAttribute("summary");
    PageDTO<ShowtimeStatDTO> result = (PageDTO<ShowtimeStatDTO>) request.getAttribute("result");
    String pagerBase = (String) request.getAttribute("pagerBase");
    String filterAction = "/report/operations";
    java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
%>
<div class="cf-page-header">
    <div>
        <h1>Chỉ số vận hành</h1>
        <p>Lấp đầy phòng chiếu, số vé bán theo suất chiếu và tồn kho bắp nước.</p>
    </div>
</div>

<%@ include file="filter.jspf" %>

<div class="rp-kpis">
    <div class="rp-kpi"><div class="label">Suất chiếu</div><div class="value"><%= num(s.getShowtimes()) %></div><div class="note">Không tính suất đã hủy</div></div>
    <div class="rp-kpi"><div class="label">Tỷ lệ lấp đầy</div><div class="value"><%= pct(s.getOccupancy()) %></div><div class="note"><%= num(s.getSoldSeats()) %> / <%= num(s.getCapacity()) %> ghế</div></div>
    <div class="rp-kpi"><div class="label">Món hết hàng</div><div class="value"><%= num(s.getOutOfStockItems()) %></div><div class="note">Món đang bán nhưng hết kho</div></div>
</div>

<div class="cf-card">
    <div class="rp-card-title">Hiệu suất theo suất chiếu</div>
    <div class="cf-table-wrap">
        <table class="cf-table">
            <thead>
                <tr><th>Thời gian</th><th>Phim</th><th>Centre</th><th>Phòng</th><th class="rp-num">Đã bán / Sức chứa</th><th>Lấp đầy</th></tr>
            </thead>
            <tbody>
            <% if (result.getItems().isEmpty()) { %><tr><td colspan="6" class="cf-empty">Không có suất chiếu trong khoảng này.</td></tr><% } %>
            <% for (ShowtimeStatDTO r : result.getItems()) {
                double occ = r.getOccupancy();
                String level = occ >= 70 ? "good" : (occ >= 30 ? "" : "warn");
            %>
                <tr>
                    <td><%= timeFormat.format(r.getStartTime()) %></td>
                    <td><%= esc(r.getMovieTitle()) %></td>
                    <td><%= esc(r.getCentreName()) %></td>
                    <td><%= esc(r.getHallName()) %></td>
                    <td class="rp-num"><%= r.getSoldSeats() %> / <%= r.getCapacity() %></td>
                    <td>
                        <div class="rp-bar <%= level %>"><span style="width: <%= Math.min(100, (int) Math.round(occ)) %>%"></span></div>
                        <small><%= pct(occ) %></small>
                    </td>
                </tr>
            <% } %>
            </tbody>
        </table>
    </div>
    <div class="rp-pager">
        <span class="info">Hiển thị <%= result.getFrom() %>-<%= result.getTo() %> trên tổng <%= result.getTotal() %> suất chiếu</span>
        <%= pager(ctx, pagerBase, result.getPage(), result.getTotalPages()) %>
    </div>
</div>

<%@ include file="footer.jspf" %>
