<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.User,model.BookingHistory,java.util.List,java.text.SimpleDateFormat" %>
<%!
    private String esc(String value) { return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
    private String money(java.math.BigDecimal value) { return value == null ? "0 đ" : String.format("%,.0f đ", value); }
%>
<% User user = (User) request.getAttribute("accountUser"); List<BookingHistory> history = (List<BookingHistory>) request.getAttribute("bookingHistory"); String error = request.getParameter("error"); String updated = request.getParameter("updated"); SimpleDateFormat dateTime = new SimpleDateFormat("dd/MM/yyyy HH:mm"); %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Tài khoản của tôi | CineFlow</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account/account.css">
</head>
<body>
<main class="account-page">
    <a class="back-link" href="${pageContext.request.contextPath}/index.jsp">← Về trang chủ</a>
    <header class="account-header"><p class="eyebrow">CINEFLOW ACCOUNT</p><h1>Tài khoản của tôi</h1><p>Quản lý thông tin cá nhân và theo dõi các lần đặt vé.</p></header>
    <% if ("true".equals(updated)) { %><div class="notice success">Thông tin cá nhân đã được cập nhật.</div><% } %>
    <% if ("email".equals(error)) { %><div class="notice error">Email này đã được sử dụng bởi tài khoản khác.</div><% } %>
    <% if ("required".equals(error)) { %><div class="notice error">Họ tên và email là thông tin bắt buộc.</div><% } %>
    <section class="account-grid">
        <article class="panel">
            <div class="panel-heading"><h2>Thông tin cá nhân</h2><a href="${pageContext.request.contextPath}/auth/change-password.jsp">Đổi mật khẩu</a></div>
            <form method="post" action="${pageContext.request.contextPath}/account" class="profile-form">
                <label>Họ và tên<input name="fullName" value="<%= esc(user.getFullName()) %>" required></label>
                <label>Tài khoản<input value="<%= esc(user.getAccount()) %>" disabled></label>
                <label>Email<input type="email" name="email" value="<%= esc(user.getEmail()) %>" required></label>
                <label>Số điện thoại<input name="phone" value="<%= esc(user.getPhone()) %>"></label>
                <label>Ảnh đại diện (URL)<input name="avatar" value="<%= esc(user.getAvatar()) %>"></label>
                <button type="submit">Lưu thay đổi</button>
            </form>
        </article>
        <article class="panel account-summary"><h2>Tóm tắt tài khoản</h2><p><span>Vai trò</span><strong><%= esc(user.getRole()) %></strong></p><p><span>Trạng thái</span><strong><%= esc(user.getStatus()) %></strong></p><p><span>Email</span><strong><%= user.isEmailVerified() ? "Đã xác minh" : "Chưa xác minh" %></strong></p></article>
    </section>
    <section class="panel history-panel"><div class="panel-heading"><h2>Lịch sử đặt vé</h2><a href="${pageContext.request.contextPath}/booking/booking.jsp">Đặt vé mới</a></div>
        <% if (history == null || history.isEmpty()) { %><p class="empty">Bạn chưa có lần đặt vé nào.</p><% } else { %>
        <div class="history-list"><% for (BookingHistory item : history) { %><article class="history-item"><div><strong><%= esc(item.getMovieTitle()) %></strong><span>Mã đặt vé: <%= esc(item.getBookingCode()) %></span><span><%= esc(item.getBranchName()) %> · <%= item.getStartTime() == null ? "Chưa có lịch" : dateTime.format(item.getStartTime()) %></span></div><div class="history-right"><b><%= money(item.getTotalAmount()) %></b><span class="status"><%= esc(item.getBookingStatus()) %></span></div></article><% } %></div>
        <% } %>
    </section>
</main>
</body>
</html>


