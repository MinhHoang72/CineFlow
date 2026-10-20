<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.dto.report.SalesSummaryDTO, model.dto.report.SalesRowDTO, model.dto.fnb.FnbSaleDTO, java.util.List, java.math.BigDecimal" %>
<%@ include file="header.jspf" %>
<%
    SalesSummaryDTO s = (SalesSummaryDTO) request.getAttribute("summary");
    List<SalesRowDTO> days = (List<SalesRowDTO>) request.getAttribute("days");
    List<SalesRowDTO> movies = (List<SalesRowDTO>) request.getAttribute("movies");
    List<SalesRowDTO> centreRows = (List<SalesRowDTO>) request.getAttribute("centreRows");
    List<FnbSaleDTO> fnbTop = (List<FnbSaleDTO>) request.getAttribute("fnbTop");
    String filters = (String) request.getAttribute("filters");
    boolean isAdmin = Boolean.TRUE.equals(request.getAttribute("isAdmin"));
    String filterAction = "/report/sales";
    BigDecimal maxDay = BigDecimal.ONE;
    for (SalesRowDTO d : days) {
        if (d.getTotal().compareTo(maxDay) > 0) {
            maxDay = d.getTotal();
        }
    }
%>
<div class="cf-page-header">
    <div>
        <h1>Doanh thu &amp; bán hàng</h1>
        <p>Doanh thu vé và bắp nước đã thanh toán trong khoảng thời gian chọn.</p>
    </div>
    <a class="cf-btn cf-btn-secondary" href="<%= ctx %>/report/sales/export?<%= esc(filters) %>">Xuất CSV</a>
</div>

<%@ include file="filter.jspf" %>

<div class="rp-kpis">
    <div class="rp-kpi"><div class="label">Tổng doanh thu</div><div class="value"><%= money(s.getTotalRevenue()) %></div><div class="note">Vé + bắp nước</div></div>
    <div class="rp-kpi"><div class="label">Doanh thu vé</div><div class="value"><%= money(s.getTicketRevenue()) %></div><div class="note"><%= s.getBookings() %> đơn, <%= s.getTickets() %> vé</div></div>
    <div class="rp-kpi"><div class="label">Doanh thu bắp nước</div><div class="value"><%= money(s.getFnbRevenue()) %></div><div class="note">Đơn đã chuẩn bị hoặc giao</div></div>
    <div class="rp-kpi"><div class="label">Giá trị đơn trung bình</div><div class="value"><%= money(s.getAverageOrder()) %></div><div class="note">Chỉ tính đơn vé</div></div>
    <div class="rp-kpi"><div class="label">Giảm giá voucher</div><div class="value"><%= money(s.getDiscountTotal()) %></div><div class="note"><%= s.getVoucherBookings() %> đơn dùng voucher</div></div>
</div>

<div class="cf-card">
    <div class="rp-card-title">Doanh thu theo ngày</div>
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
    <div class="cf-table-wrap">
        <table class="cf-table">
            <thead>
                <tr><th>Ngày</th><th class="rp-num">Đơn</th><th class="rp-num">Vé</th><th class="rp-num">Doanh thu vé</th><th class="rp-num">Bắp nước</th><th class="rp-num">Tổng</th></tr>
            </thead>
            <tbody>
            <% for (SalesRowDTO d : days) { %>
                <tr>
                    <td><%= esc(d.getLabel()) %></td>
                    <td class="rp-num"><%= d.getBookings() %></td>
                    <td class="rp-num"><%= d.getTickets() %></td>
                    <td class="rp-num"><%= money(d.getTicketRevenue()) %></td>
                    <td class="rp-num"><%= money(d.getFnbRevenue()) %></td>
                    <td class="rp-num"><strong><%= money(d.getTotal()) %></strong></td>
                </tr>
            <% } %>
            </tbody>
        </table>
    </div>
</div>

<div class="rp-grid">
    <div class="cf-card">
        <div class="rp-card-title">Top phim theo doanh thu vé</div>
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
        <div class="rp-card-title">Top bắp nước bán chạy</div>
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

<% if (isAdmin) { %>
<div class="cf-card" style="margin-top: 16px;">
    <div class="rp-card-title">Doanh thu theo Centre</div>
    <div class="cf-table-wrap">
        <table class="cf-table">
            <thead><tr><th>Centre</th><th class="rp-num">Đơn</th><th class="rp-num">Vé</th><th class="rp-num">Doanh thu vé</th><th class="rp-num">Bắp nước</th><th class="rp-num">Tổng</th></tr></thead>
            <tbody>
            <% for (SalesRowDTO c : centreRows) { %>
                <tr>
                    <td><%= esc(c.getLabel()) %></td>
                    <td class="rp-num"><%= c.getBookings() %></td>
                    <td class="rp-num"><%= c.getTickets() %></td>
                    <td class="rp-num"><%= money(c.getTicketRevenue()) %></td>
                    <td class="rp-num"><%= money(c.getFnbRevenue()) %></td>
                    <td class="rp-num"><strong><%= money(c.getTotal()) %></strong></td>
                </tr>
            <% } %>
            </tbody>
        </table>
    </div>
</div>
<% } %>
<%@ include file="footer.jspf" %>
