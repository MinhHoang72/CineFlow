<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.ReportSummary,model.RevenueReport,model.ShowtimeMetric,java.util.List,java.text.SimpleDateFormat" %>
<%
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && session.getAttribute("user") instanceof model.User) {
        authRole = ((model.User) session.getAttribute("user")).getRole();
    }
    if (!"MANAGER".equalsIgnoreCase(authRole) && !"ADMIN".equalsIgnoreCase(authRole)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Quản lý (MANAGER) mới có quyền truy cập trang này.");
        return;
    }
    if (request.getAttribute("reportSummary") == null) {
        response.sendRedirect(request.getContextPath() + "/reports");
        return;
    }
%>
<%! private String money(java.math.BigDecimal n){return String.format("%,.0f đ", n == null ? java.math.BigDecimal.ZERO : n);} %>
<% ReportSummary summary=(ReportSummary)request.getAttribute("reportSummary"); List<RevenueReport> branches=(List<RevenueReport>)request.getAttribute("branchRevenue"); List<RevenueReport> movies=(List<RevenueReport>)request.getAttribute("movieRevenue"); List<ShowtimeMetric> shows=(List<ShowtimeMetric>)request.getAttribute("showtimeMetrics"); SimpleDateFormat df=new SimpleDateFormat("dd/MM/yyyy HH:mm"); %>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Báo cáo hoạt động | CineFlow</title><link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/shell.css"><link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/tables.css"><link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/reports.css"></head><body><div class="cf-manager-shell"><jsp:include page="/manager/sidebar.jsp"/><div class="cf-manager-main"><jsp:include page="/manager/topbar.jsp"/><main class="cf-content">
<div class="cf-page-header"><div><p class="cf-breadcrumb">Báo cáo / Phân tích hoạt động</p><h1>Báo cáo &amp; phân tích</h1><p>Tổng hợp doanh thu, lượng vé bán và hiệu suất suất chiếu.</p></div></div>
<div class="report-kpis"><div class="report-kpi"><span>DOANH THU</span><strong><%=money(summary.getRevenue())%></strong><small>Từ đơn đặt vé đã xác nhận</small></div><div class="report-kpi"><span>ĐƠN ĐẶT VÉ</span><strong><%=summary.getBookings()%></strong><small>Đã xác nhận / hoàn tất</small></div><div class="report-kpi"><span>VÉ ĐÃ BÁN</span><strong><%=summary.getTickets()%></strong><small>Tổng số ghế trong đơn hợp lệ</small></div><div class="report-kpi"><span>LẤP ĐẦY</span><strong><%=String.format("%.1f%%",summary.getOccupancy())%></strong><small>Trên các suất chiếu có dữ liệu</small></div></div>
<div class="report-grid"><section class="cf-card"><div class="cf-card-header"><div><h2>Doanh thu theo trung tâm</h2><p>So sánh kết quả kinh doanh giữa các rạp.</p></div></div><div class="cf-card-body"><div class="cf-table-wrap"><table class="cf-table"><thead><tr><th>Trung tâm</th><th>Thành phố</th><th>Đơn</th><th>Vé</th><th>Doanh thu</th></tr></thead><tbody><%for(RevenueReport x:branches){%><tr><td><strong><%=x.getName()%></strong></td><td><%=x.getCity()==null?"—":x.getCity()%></td><td><%=x.getBookings()%></td><td><%=x.getTickets()%></td><td class="amount"><%=money(x.getRevenue())%></td></tr><%}%></tbody></table></div></div></section>
<section class="cf-card"><div class="cf-card-header"><div><h2>Top phim bán chạy</h2><p>Xếp hạng theo doanh thu.</p></div></div><div class="cf-card-body"><div class="cf-table-wrap"><table class="cf-table"><thead><tr><th>#</th><th>Phim</th><th>Vé</th><th>Doanh thu</th></tr></thead><tbody><%int rank=1;for(RevenueReport x:movies){%><tr><td class="rank"><%=rank++%></td><td><strong><%=x.getName()%></strong></td><td><%=x.getTickets()%></td><td class="amount"><%=money(x.getRevenue())%></td></tr><%}%></tbody></table></div></div></section></div>
<section class="cf-card"><div class="cf-card-header"><div><h2>Tỷ lệ lấp đầy theo suất chiếu</h2><p>Số ghế đã bán so với sức chứa phòng chiếu.</p></div></div><div class="cf-card-body"><div class="cf-table-wrap"><table class="cf-table"><thead><tr><th>Phim</th><th>Trung tâm / Phòng</th><th>Suất chiếu</th><th>Ghế bán</th><th>Lấp đầy</th></tr></thead><tbody><%for(ShowtimeMetric x:shows){%><tr><td><strong><%=x.getMovieTitle()%></strong></td><td><%=x.getBranchName()%> · <%=x.getHallName()%></td><td><%=df.format(x.getStartTime())%></td><td><%=x.getSoldSeats()%> / <%=x.getCapacity()%></td><td><div class="meter"><i style="width:<%=Math.min(100,x.getOccupancy())%>%"></i></div><b><%=String.format("%.1f%%",x.getOccupancy())%></b></td></tr><%}%></tbody></table></div></div></section>
</main></div></div></body></html>
