<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.dto.OperationsSummaryDTO, model.dto.ShowtimeStatDTO, model.dto.StaffScanDTO, model.dto.PageDTO, java.util.List" %>
<%@ include file="header.jspf" %>
<%
    OperationsSummaryDTO s = (OperationsSummaryDTO) request.getAttribute("summary");
    PageDTO<ShowtimeStatDTO> result = (PageDTO<ShowtimeStatDTO>) request.getAttribute("result");
    List<StaffScanDTO> scans = (List<StaffScanDTO>) request.getAttribute("scans");
    String pagerBase = (String) request.getAttribute("pagerBase");
    String filterAction = "/report/operations";
    java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
%>
<div class="cf-page-header">
    <div>
        <h1>Chỉ số vận hành</h1>
        <p>Lấp đầy phòng chiếu, tỷ lệ khách đến xem và hiệu suất soát vé.</p>
    </div>
</div>

<%@ include file="filter.jspf" %>

<div class="rp-kpis">
    <div class="rp-kpi"><div class="label">Suất chiếu</div><div class="value"><%= num(s.getShowtimes()) %></div><div class="note">Không tính suất đã hủy</div></div>
    <div class="rp-kpi"><div class="label">Tỷ lệ lấp đầy</div><div class="value"><%= pct(s.getOccupancy()) %></div><div class="note"><%= num(s.getSoldSeats()) %> / <%= num(s.getCapacity()) %> ghế</div></div>
    <div class="rp-kpi"><div class="label">Vé đã soát</div><div class="value"><%= num(s.getCheckedIn()) %></div><div class="note">Tỷ lệ đến xem <%= pct(s.getCheckInRate()) %></div></div>
    <div class="rp-kpi"><div class="label">Lượt soát bị từ chối</div><div class="value"><%= num(s.getRejectedScans()) %></div><div class="note">Vé sai, hết hạn hoặc đã dùng</div></div>
    <div class="rp-kpi"><div class="label">Món hết hàng</div><div class="value"><%= num(s.getOutOfStockItems()) %></div><div class="note">Món đang bán nhưng hết kho</div></div>
</div>

<div class="cf-card">
    <div class="rp-card-title">Hiệu suất theo suất chiếu</div>
    <div class="cf-table-wrap">
        <table class="cf-table">
            <thead>
                <tr><th>Thời gian</th><th>Phim</th><th>Centre</th><th>Phòng</th><th class="rp-num">Đã bán / Sức chứa</th><th>Lấp đầy</th><th class="rp-num">Đã soát</th></tr>
            </thead>
            <tbody>
            <% if (result.getItems().isEmpty()) { %><tr><td colspan="7" class="cf-empty">Không có suất chiếu trong khoảng này.</td></tr><% } %>
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
                    <td class="rp-num"><%= r.getCheckedIn() %></td>
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

<div class="cf-card" style="margin-top: 16px;">
    <div class="rp-card-title">Soát vé theo nhân viên</div>
    <div class="cf-table-wrap">
        <table class="cf-table">
            <thead><tr><th>Nhân viên</th><th class="rp-num">Thành công</th><th class="rp-num">Từ chối</th><th class="rp-num">Tổng lượt</th></tr></thead>
            <tbody>
            <% if (scans.isEmpty()) { %><tr><td colspan="4" class="cf-empty">Chưa có lượt soát vé nào.</td></tr><% } %>
            <% for (StaffScanDTO r : scans) { %>
                <tr>
                    <td><%= esc(r.getStaffName()) %></td>
                    <td class="rp-num"><%= r.getSuccess() %></td>
                    <td class="rp-num"><%= r.getRejected() %></td>
                    <td class="rp-num"><%= r.getSuccess() + r.getRejected() %></td>
                </tr>
            <% } %>
            </tbody>
        </table>
    </div>
</div>
<%@ include file="footer.jspf" %>
