<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.SalesSummaryDTO, model.OperationsSummaryDTO, model.SalesRowDTO, model.FnbSaleDTO, java.util.List, java.util.Map, java.math.BigDecimal" %>
<%@ include file="head.jspf" %>
<%
    SalesSummaryDTO s = (SalesSummaryDTO) request.getAttribute("summary");
    OperationsSummaryDTO ops = (OperationsSummaryDTO) request.getAttribute("ops");
    List<SalesRowDTO> days = (List<SalesRowDTO>) request.getAttribute("days");
    List<SalesRowDTO> centreRows = (List<SalesRowDTO>) request.getAttribute("centreRows");
    Map<String, Double> occupancy = (Map<String, Double>) request.getAttribute("occupancy");
    List<SalesRowDTO> movies = (List<SalesRowDTO>) request.getAttribute("movies");
    List<FnbSaleDTO> fnbTop = (List<FnbSaleDTO>) request.getAttribute("fnbTop");
    int activeCentres = (Integer) request.getAttribute("activeCentres");
    int customers = (Integer) request.getAttribute("customers");
    String filterAction = "/report/chain";
    BigDecimal maxDay = BigDecimal.ONE;
    for (SalesRowDTO d : days) {
        if (d.getTotal().compareTo(maxDay) > 0) {
            maxDay = d.getTotal();
        }
    }
    BigDecimal maxCentre = BigDecimal.ONE;
    for (SalesRowDTO c : centreRows) {
        if (c.getTotal().compareTo(maxCentre) > 0) {
            maxCentre = c.getTotal();
        }
    }
%>
<div class="cf-page-header">
    <div>
        <h1>Dashboard toàn chuỗi</h1>
        <p>Tổng quan kinh doanh và vận hành của toàn bộ hệ thống CineFlow.</p>
    </div>
    <a class="cf-btn cf-btn-secondary" href="<%= ctx %>/report/sales/export?<%= esc(request.getAttribute("filters")) %>">Xuất CSV doanh thu</a>
</div>

<div style="display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 18px;">
    <a class="cf-btn cf-btn-primary" href="<%= ctx %>/admin/movies">Quản lý phim</a>
    <a class="cf-btn cf-btn-ghost" href="<%= ctx %>/admin/customers">Khách hàng</a>
    <a class="cf-btn cf-btn-ghost" href="<%= ctx %>/reports">Báo cáo doanh thu</a>
    <a class="cf-btn cf-btn-ghost" href="<%= ctx %>/admin/managers">Quản lý nhân sự</a>
</div>

<%@ include file="filter.jspf" %>

<div class="rp-kpis">
    <div class="rp-kpi"><div class="label">Tổng doanh thu</div><div class="value"><%= money(s.getTotalRevenue()) %></div><div class="note">Vé <%= money(s.getTicketRevenue()) %> · Bắp nước <%= money(s.getFnbRevenue()) %></div></div>
    <div class="rp-kpi"><div class="label">Vé đã bán</div><div class="value"><%= num(s.getTickets()) %></div><div class="note"><%= num(s.getBookings()) %> đơn đặt vé</div></div>
    <div class="rp-kpi"><div class="label">Tỷ lệ lấp đầy</div><div class="value"><%= pct(ops.getOccupancy()) %></div><div class="note"><%= num(ops.getShowtimes()) %> suất chiếu</div></div>
    <div class="rp-kpi"><div class="label">Tỷ lệ đến xem</div><div class="value"><%= pct(ops.getCheckInRate()) %></div><div class="note"><%= num(ops.getCheckedIn()) %> vé đã soát</div></div>
    <div class="rp-kpi"><div class="label">Centre đang hoạt động</div><div class="value"><%= activeCentres %></div><div class="note"><%= num(customers) %> khách hàng hoạt động</div></div>
    <div class="rp-kpi"><div class="label">Voucher đã dùng</div><div class="value"><%= s.getVoucherBookings() %></div><div class="note">Giảm <%= money(s.getDiscountTotal()) %></div></div>
</div>

<div class="cf-card">
    <div class="rp-card-title">Xu hướng doanh thu (tối đa 31 ngày gần nhất của khoảng chọn)</div>
    <div class="rp-trend">
        <% for (SalesRowDTO d : days) {
            int ticketHeight = d.getTicketRevenue().multiply(BigDecimal.valueOf(100)).divide(maxDay, 0, java.math.RoundingMode.HALF_UP).intValue();
            int fnbHeight = d.getFnbRevenue().multiply(BigDecimal.valueOf(100)).divide(maxDay, 0, java.math.RoundingMode.HALF_UP).intValue();
        %>
        <div class="col" title="<%= esc(d.getLabel()) %>: <%= money(d.getTotal()) %>">
            <span class="fnb" style="height: <%= fnbHeight %>%"></span>
            <span style="height: <%= ticketHeight %>%"></span>
        </div>
        <% } %>
    </div>
    <div class="rp-legend"><span><i style="background:#ff2a54"></i>Vé</span><span><i style="background:#38bdf8"></i>Bắp nước</span></div>
</div>

<div class="cf-card" style="margin-top: 16px;">
    <div class="rp-card-title">Xếp hạng Centre</div>
    <div class="cf-table-wrap">
        <table class="cf-table">
            <thead>
                <tr><th>#</th><th>Centre</th><th class="rp-num">Vé</th><th class="rp-num">Doanh thu vé</th><th class="rp-num">Bắp nước</th><th class="rp-num">Tổng</th><th>Tỷ trọng</th><th>Lấp đầy</th></tr>
            </thead>
            <tbody>
            <% int rank = 1; for (SalesRowDTO c : centreRows) {
                int share = c.getTotal().multiply(BigDecimal.valueOf(100)).divide(maxCentre, 0, java.math.RoundingMode.HALF_UP).intValue();
                double occ = occupancy.containsKey(c.getLabel()) ? occupancy.get(c.getLabel()) : 0;
            %>
                <tr>
                    <td><%= rank++ %></td>
                    <td><strong><%= esc(c.getLabel()) %></strong></td>
                    <td class="rp-num"><%= c.getTickets() %></td>
                    <td class="rp-num"><%= money(c.getTicketRevenue()) %></td>
                    <td class="rp-num"><%= money(c.getFnbRevenue()) %></td>
                    <td class="rp-num"><strong><%= money(c.getTotal()) %></strong></td>
                    <td><div class="rp-bar"><span style="width: <%= share %>%"></span></div></td>
                    <td><div class="rp-bar <%= occ >= 70 ? "good" : (occ >= 30 ? "" : "warn") %>"><span style="width: <%= Math.min(100, (int) Math.round(occ)) %>%"></span></div><small><%= pct(occ) %></small></td>
                </tr>
            <% } %>
            </tbody>
        </table>
    </div>
</div>

<div class="rp-grid">
    <div class="cf-card">
        <div class="rp-card-title">Top 5 phim</div>
        <div class="cf-table-wrap">
            <table class="cf-table">
                <thead><tr><th>Phim</th><th class="rp-num">Vé</th><th class="rp-num">Doanh thu</th></tr></thead>
                <tbody>
                <% if (movies.isEmpty()) { %><tr><td colspan="3" class="cf-empty">Chưa có dữ liệu.</td></tr><% } %>
                <% for (SalesRowDTO m : movies) { %>
                    <tr><td><%= esc(m.getLabel()) %></td><td class="rp-num"><%= m.getTickets() %></td><td class="rp-num"><%= money(m.getTicketRevenue()) %></td></tr>
                <% } %>
                </tbody>
            </table>
        </div>
    </div>
    <div class="cf-card">
        <div class="rp-card-title">Top 5 bắp nước</div>
        <div class="cf-table-wrap">
            <table class="cf-table">
                <thead><tr><th>Món / Combo</th><th class="rp-num">Số lượng</th><th class="rp-num">Doanh thu</th></tr></thead>
                <tbody>
                <% if (fnbTop.isEmpty()) { %><tr><td colspan="3" class="cf-empty">Chưa có dữ liệu.</td></tr><% } %>
                <% for (FnbSaleDTO f : fnbTop) { %>
                    <tr><td><%= esc(f.getName()) %></td><td class="rp-num"><%= f.getQuantity() %></td><td class="rp-num"><%= money(f.getRevenue()) %></td></tr>
                <% } %>
                </tbody>
            </table>
        </div>
    </div>
</div>
<%@ include file="foot.jspf" %>
