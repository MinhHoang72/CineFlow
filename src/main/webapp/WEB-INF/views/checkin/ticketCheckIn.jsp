<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.dto.checkin.CheckInResultDTO, model.dto.booking.TicketInfoDTO, model.dto.checkin.CheckInLogDTO, java.util.List" %>
<%!
    static String esc(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    static String time(java.sql.Timestamp value) {
        return value == null ? "" : new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(value);
    }
%>
<%
    String ctx = request.getContextPath();
    CheckInResultDTO result = (CheckInResultDTO) request.getAttribute("result");
    List<CheckInLogDTO> logs = (List<CheckInLogDTO>) request.getAttribute("logs");
    boolean canVerify = Boolean.TRUE.equals(request.getAttribute("canVerify"));
    String csrf = (String) request.getAttribute("csrf");
    String code = (String) request.getAttribute("code");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Soát vé | CineFlow</title>
    <link rel="stylesheet" href="<%= ctx %>/css/common/reset.css">
    <link rel="stylesheet" href="<%= ctx %>/css/common/variables.css">
    <link rel="stylesheet" href="<%= ctx %>/css/common/buttons.css">
    <link rel="stylesheet" href="<%= ctx %>/css/common/badges.css">
    <link rel="stylesheet" href="<%= ctx %>/css/common/forms.css">
    <link rel="stylesheet" href="<%= ctx %>/css/common/tables.css">
    <link rel="stylesheet" href="<%= ctx %>/css/manager/shell.css">
    <link rel="stylesheet" href="<%= ctx %>/css/admin/dashboard.css">
    <style>
        .ck-form { display: flex; gap: 10px; padding: 18px; flex-wrap: wrap; }
        .ck-form input { flex: 1; min-width: 260px; font-size: 18px; }
        .ck-result { margin: 0 18px 18px; padding: 14px 16px; border-radius: 10px; }
        .ck-result.ok { background: #dcfce7; color: #166534; }
        .ck-result.bad { background: #fee2e2; color: #991b1b; }
        .ck-result h2 { margin: 0 0 6px; font-size: 18px; }
        .ck-result dl { display: grid; grid-template-columns: 130px 1fr; gap: 4px 10px; margin: 8px 0 0; }
        .ck-result dt { font-weight: 600; }
    </style>
</head>
<body>
<div class="cf-manager-shell">
    <jsp:include page="/WEB-INF/fragments/managerSidebar.jspf" />
    <div class="cf-manager-main">
        <jsp:include page="/WEB-INF/fragments/adminTopbar.jspf" />
        <main class="cf-content">
            <div class="cf-page-header">
                <div>
                    <h1>Soát vé</h1>
                    <p><%= canVerify ? "Nhập hoặc quét mã vé để cho khách vào phòng chiếu." : "Lịch sử soát vé của Centre (chỉ xem)." %></p>
                </div>
            </div>

            <% if (canVerify) { %>
            <div class="cf-card">
                <form class="ck-form" method="post" action="<%= ctx %>/checkin/verify">
                    <input type="hidden" name="_csrf" value="<%= esc(csrf) %>">
                    <input class="cf-form-control" type="text" name="code" maxlength="500" autofocus autocomplete="off"
                           placeholder="Mã vé hoặc mã QR" value="<%= esc(code) %>">
                    <button class="cf-btn cf-btn-primary" type="submit">Soát vé</button>
                </form>
                <% if (result != null) { %>
                <div class="ck-result <%= result.isSuccess() ? "ok" : "bad" %>">
                    <h2><%= result.isSuccess() ? "Hợp lệ" : "Từ chối" %></h2>
                    <div><%= esc(result.getMessage()) %></div>
                    <% TicketInfoDTO t = result.getTicket(); if (t != null) { %>
                    <dl>
                        <dt>Mã vé</dt><dd><%= esc(t.getTicketCode()) %></dd>
                        <dt>Đơn đặt vé</dt><dd><%= esc(t.getBookingCode()) %></dd>
                        <dt>Phim</dt><dd><%= esc(t.getMovieTitle()) %></dd>
                        <dt>Phòng / Ghế</dt><dd><%= esc(t.getHallName()) %> / <%= esc(t.getSeatLabel()) %></dd>
                        <dt>Suất chiếu</dt><dd><%= time(t.getStartTime()) %> - <%= time(t.getEndTime()) %></dd>
                        <dt>Centre</dt><dd><%= esc(t.getCentreName()) %></dd>
                    </dl>
                    <% } %>
                </div>
                <% } %>
            </div>
            <% } %>

            <div class="cf-card" style="margin-top: 16px;">
                <div class="cf-card-header"><h2>Lịch sử soát vé gần đây</h2></div>
                <div class="cf-table-wrap">
                    <table class="cf-table">
                        <thead>
                            <tr>
                                <th>Thời gian</th>
                                <th>Mã vé</th>
                                <th>Phim</th>
                                <th>Phòng / Ghế</th>
                                <th>Nhân viên</th>
                                <th>Kết quả</th>
                                <th>Ghi chú</th>
                            </tr>
                        </thead>
                        <tbody>
                        <% if (logs.isEmpty()) { %>
                            <tr><td colspan="7" class="cf-empty">Chưa có lượt soát vé nào.</td></tr>
                        <% } %>
                        <% for (CheckInLogDTO l : logs) { %>
                            <tr>
                                <td><%= time(l.getCheckInTime()) %></td>
                                <td><%= esc(l.getTicketCode()) %></td>
                                <td><%= esc(l.getMovieTitle()) %></td>
                                <td><%= esc(l.getHallName()) %> / <%= esc(l.getSeatLabel()) %></td>
                                <td><%= esc(l.getStaffName()) %></td>
                                <td><span class="cf-badge <%= "SUCCESS".equals(l.getResult()) ? "cf-badge-success" : "cf-badge-danger" %>"><%= "SUCCESS".equals(l.getResult()) ? "Thành công" : "Từ chối" %></span></td>
                                <td><%= esc(l.getNote()) %></td>
                            </tr>
                        <% } %>
                        </tbody>
                    </table>
                </div>
            </div>
        </main>
    </div>
</div>
</body>
</html>
