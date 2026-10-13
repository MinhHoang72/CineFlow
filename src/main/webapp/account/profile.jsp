<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.User,model.BookingHistory,java.util.List,java.text.SimpleDateFormat" %>
<%
    if (session.getAttribute("userId") == null) {
        String loginUrl = request.getContextPath() + "/auth/login.jsp?redirect=" + java.net.URLEncoder.encode("/account", "UTF-8");
        response.sendRedirect(loginUrl);
        return;
    }
    if (request.getAttribute("accountUser") == null) {
        response.sendRedirect(request.getContextPath() + "/account");
        return;
    }
%>
<%!
    private String esc(String value) { return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
    private String money(java.math.BigDecimal value) { return value == null ? "0 đ" : String.format("%,.0f đ", value); }
%>
<% User user = (User) request.getAttribute("accountUser"); List<BookingHistory> history = (List<BookingHistory>) request.getAttribute("bookingHistory"); String error = request.getParameter("error"); String updated = request.getParameter("updated"); SimpleDateFormat dateTime = new SimpleDateFormat("dd/MM/yyyy HH:mm"); String avatarUrl = user == null || user.getAvatar() == null ? "" : user.getAvatar(); %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Tài khoản của tôi | CineFlow</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account/account.css?v=20261011-profile4">
</head>
<body>
<div class="page-shell">
    <!-- Common Header & Navigation -->
    <jsp:include page="/common/header.jsp"/>
<main class="account-page">
    <% if ("true".equals(updated)) { %><div class="notice success">Thông tin cá nhân đã được cập nhật.</div><% } %>
    <% if ("email".equals(error)) { %><div class="notice error">Email này đã được sử dụng bởi tài khoản khác.</div><% } %>
    <% if ("required".equals(error)) { %><div class="notice error">Họ tên và email là thông tin bắt buộc.</div><% } %>
    <% if ("avatarType".equals(error)) { %><div class="notice error">Ảnh đại diện chỉ hỗ trợ PNG, JPG hoặc WEBP.</div><% } %>
    <section class="profile-overview"><div class="profile-avatar-wrap"><div class="profile-avatar"><% if (!avatarUrl.isBlank()) { %><img id="avatarPreview" src="<%= esc(avatarUrl) %>" alt="Ảnh đại diện"><% } else { %><span id="avatarInitial"><%= user.getFullName() == null || user.getFullName().isBlank() ? "C" : esc(user.getFullName().substring(0,1).toUpperCase()) %></span><% } %></div><label class="avatar-camera" for="avatarUpload" aria-label="Chọn ảnh đại diện">▣</label></div><div class="overview-copy"><h2><%= esc(user.getFullName()) %></h2><div class="overview-meta"><span class="role-pill">♙ <%= esc(user.getRole()) %></span></div><div class="contact-strip"><span>✉ <%= esc(user.getEmail()) %></span><span>♧ <%= user.getPhone() == null || user.getPhone().isBlank() ? "Chưa có SĐT" : esc(user.getPhone()) %></span><span class="active-indicator">● <%= esc(user.getStatus()) %></span><% if (user.isEmailVerified()) { %><span style="display:inline-flex; align-items:center; gap:4px; padding:2px 8px; border-radius:12px; font-size:11px; font-weight:600; background:rgba(52,211,153,0.12); color:#34D399; border:1px solid rgba(52,211,153,0.3);">✓ Đã xác thực email</span><% } else { %><span style="display:inline-flex; align-items:center; gap:4px; padding:2px 8px; border-radius:12px; font-size:11px; font-weight:600; background:rgba(245,158,11,0.12); color:#F59E0B; border:1px solid rgba(245,158,11,0.3);">⚠ Chưa xác thực email</span><% } %></div><p class="member-since">Tham gia từ <%= user.getCreatedAt() == null ? "—" : new SimpleDateFormat("dd/MM/yyyy").format(user.getCreatedAt()) %><span>•</span>Cập nhật lần cuối: <%= user.getUpdatedAt() == null ? "—" : new SimpleDateFormat("dd/MM/yyyy").format(user.getUpdatedAt()) %></p></div></section>
    <section class="account-grid">
        <article class="panel profile-panel"><div class="panel-heading profile-heading"><span class="heading-icon">♙</span><div><h2>Thông tin cá nhân</h2><p class="panel-description">Cập nhật thông tin cá nhân của bạn để sử dụng dịch vụ tốt hơn.</p></div></div>
            <form method="post" enctype="multipart/form-data" action="${pageContext.request.contextPath}/account" class="profile-form">
                <label class="full-row">Họ và tên <em>*</em><input name="fullName" value="<%= esc(user.getFullName()) %>" required></label>
                <label>Tài khoản <em>*</em><input value="<%= esc(user.getAccount()) %>" disabled></label>
                <label>Email <em>*</em><input type="email" name="email" value="<%= esc(user.getEmail()) %>" required></label>
                <label class="full-row">Số điện thoại<input name="phone" value="<%= esc(user.getPhone()) %>" placeholder="Nhập số điện thoại (dùng để nhận vé tại quầy/Kiosk)"></label>
                <input id="avatarValue" type="hidden" name="avatar" value="<%= esc(avatarUrl) %>"><input id="avatarUpload" type="file" name="avatarFile" accept="image/png,image/jpeg,image/webp" hidden onchange="previewAvatar(this)">
                <button type="submit">▣ &nbsp; Lưu thay đổi</button>
            </form>
        </article>
        <div class="account-side"><article class="panel security-panel"><h2><span>♢</span>Bảo mật tài khoản</h2><a class="security-link" href="${pageContext.request.contextPath}/auth/change-password.jsp"><i>♙</i><span><strong>Đổi mật khẩu</strong><small>Thay đổi mật khẩu đăng nhập định kỳ</small></span><b>›</b></a><div style="margin-top: 14px; padding: 12px 14px; background: rgba(255,255,255,0.03); border-radius: 8px; border: 1px solid rgba(255,255,255,0.06); font-size: 12px; color: #94A3B8; line-height: 1.5;"><strong style="color: #CBD5E1; display: block; margin-bottom: 4px;">Mẹo bảo mật:</strong>Nên đổi mật khẩu định kỳ và không cung cấp mã OTP cho người khác để bảo vệ vé và quyền lợi thành viên.</div></article>
        <article class="panel account-summary"><h2><span>▤</span>Tổng quan tài khoản</h2><p><span>Loại tài khoản</span><strong><%= esc(user.getRole()) %></strong></p><p><span>Trạng thái</span><strong class="summary-active">● <%= esc(user.getStatus()) %></strong></p><p><span>Xác thực email</span><% if (user.isEmailVerified()) { %><strong style="color: #34D399;">✓ Đã xác thực</strong><% } else { %><strong style="color: #F59E0B;">Chưa xác thực <a href="${pageContext.request.contextPath}/verify-otp?action=resend&email=<%= java.net.URLEncoder.encode(user.getEmail(), "UTF-8") %>&type=register" style="color: #FF2A54; font-size: 11px; margin-left: 6px; text-decoration: underline;">[Xác minh ngay]</a></strong><% } %></p><p><span>Ngày tham gia</span><strong><%= user.getCreatedAt() == null ? "—" : new SimpleDateFormat("dd/MM/yyyy").format(user.getCreatedAt()) %></strong></p><p><span>Tổng số vé đã đặt</span><strong><%= history == null ? 0 : history.size() %> vé</strong></p></article></div>
    </section>

    <section class="panel history-panel"><div class="panel-heading"><h2>Lịch sử đặt vé</h2><a href="${pageContext.request.contextPath}/booking/booking.jsp">Đặt vé mới</a></div>
        <% if (history == null || history.isEmpty()) { %><p class="empty">Bạn chưa có lần đặt vé nào.</p><% } else { %>
        <div class="history-list"><% for (BookingHistory item : history) { %><article class="history-item"><div><strong><%= esc(item.getMovieTitle()) %></strong><span>Mã đặt vé: <%= esc(item.getBookingCode()) %></span><span><%= esc(item.getBranchName()) %> · <%= item.getStartTime() == null ? "Chưa có lịch" : dateTime.format(item.getStartTime()) %></span></div><div class="history-right"><b><%= money(item.getTotalAmount()) %></b><span class="status"><%= esc(item.getBookingStatus()) %></span></div></article><% } %></div>
        <% } %>
    </section>
</main>
    <!-- Common Footer -->
    <jsp:include page="/common/footer.jsp"/>
</div>
</body>
<script>function previewAvatar(input){if(!input.files||!input.files[0])return;const file=input.files[0];if(file.size>2000000){alert('Ảnh đại diện không được lớn hơn 2MB.');input.value='';return;}const reader=new FileReader();reader.onload=function(e){let preview=document.getElementById('avatarPreview');if(preview){preview.src=e.target.result;}else{const image=document.createElement('img');image.id='avatarPreview';image.alt='Ảnh đại diện';image.src=e.target.result;document.getElementById('avatarInitial').replaceWith(image);}};reader.readAsDataURL(file);}</script>
</html>


