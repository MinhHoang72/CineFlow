<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<% String changePasswordRole = (String) session.getAttribute("role"); boolean managerPassword = "MANAGER".equalsIgnoreCase(changePasswordRole); %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="theme-color" content="#0B0F17">
    <title>CineFlow - Đổi mật khẩu</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/base.css?v=<%= System.currentTimeMillis() %>">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/cineflow-auth.css?v=<%= System.currentTimeMillis() %>">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/change-password.css?v=<%= System.currentTimeMillis() %>">
</head><body><div class="background"><div class="lights light-1"></div><div class="lights light-2"></div></div>
<div class="container"><section class="hero"><a class="logo" href="${pageContext.request.contextPath}/home/index.jsp">Cine<span>Flow</span></a><h1>Bảo vệ tài khoản.<br><span>An tâm hơn.</span></h1><p>Đổi mật khẩu thường xuyên để bảo vệ tài khoản và trải nghiệm điện ảnh của bạn cùng CineFlow.</p><div class="features"><div class="feature"><div class="feature-icon">✓</div><span>Xác minh mật khẩu hiện tại</span></div><div class="feature"><div class="feature-icon">✓</div><span>Tạo mật khẩu mới</span></div><div class="feature"><div class="feature-icon">★</div><span>Bảo mật tài khoản</span></div></div></section>
<main class="login-card change-password-card"><h2>Đổi mật khẩu</h2><p class="subtitle">Nhập mật khẩu hiện tại và chọn mật khẩu mới cho tài khoản.</p><form action="${pageContext.request.contextPath}/change-password" method="post">
<div class="form-group"><label for="currentPassword">Mật khẩu hiện tại</label><div class="input-wrapper"><input id="currentPassword" type="password" name="currentPassword" placeholder="Nhập mật khẩu hiện tại" autocomplete="current-password" required><button type="button" class="password-toggle" aria-label="Hiện mật khẩu" onclick="togglePassword('currentPassword',this)">Hiện</button></div></div>
<div class="form-group"><label for="newPassword">Mật khẩu mới</label><div class="input-wrapper"><input id="newPassword" type="password" name="newPassword" placeholder="Nhập mật khẩu mới" autocomplete="new-password" required><button type="button" class="password-toggle" aria-label="Hiện mật khẩu" onclick="togglePassword('newPassword',this)">Hiện</button></div></div>
<div class="form-group"><label for="confirmPassword">Xác nhận mật khẩu mới</label><div class="input-wrapper"><input id="confirmPassword" type="password" name="confirmPassword" placeholder="Nhập lại mật khẩu mới" autocomplete="new-password" required><button type="button" class="password-toggle" aria-label="Hiện mật khẩu" onclick="togglePassword('confirmPassword',this)">Hiện</button></div></div>
<button class="login-btn change-btn" type="submit">Đổi mật khẩu</button><p class="password-note">Mật khẩu mới phải khác mật khẩu hiện tại.</p></form><div class="back-home"><a href="<%= managerPassword ? request.getContextPath() + "/dashboard" : request.getContextPath() + "/account" %>"> Quay lại <%= managerPassword ? "Dashboard Manager" : "tài khoản của tôi" %></a></div></main></div>
<script>function togglePassword(id,button){const input=document.getElementById(id);const visible=input.type==='password';input.type=visible?'text':'password';button.textContent=visible?'Ẩn':'Hiện';button.setAttribute('aria-label',visible?'Ẩn mật khẩu':'Hiện mật khẩu');}</script></body></html>
