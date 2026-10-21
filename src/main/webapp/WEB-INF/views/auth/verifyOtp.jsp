<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    String email = request.getParameter("email") != null ? request.getParameter("email").trim() : "";
    String action = request.getParameter("action") != null ? request.getParameter("action").trim() : "reset";
    String error = request.getParameter("error");
    String resent = request.getParameter("resent");
    boolean isRegister = "register".equalsIgnoreCase(action);
    boolean isChangeEmail = "changeEmail".equalsIgnoreCase(action);

    String pageTitle = isRegister ? "Kích hoạt tài khoản" : (isChangeEmail ? "Xác nhận đổi email" : "Xác minh email");
    String buttonText = isRegister ? "Kích hoạt & Đăng nhập" : (isChangeEmail ? "Xác nhận & Cập nhật Email" : "Xác minh OTP");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="theme-color" content="#0B0F17">
    <title>CineFlow - <%= pageTitle %></title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/base.css?v=<%= System.currentTimeMillis() %>">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/verify-otp.css?v=<%= System.currentTimeMillis() %>">
</head>
<body>
    <main class="card">
        <a class="logo" href="${pageContext.request.contextPath}/home">Cine<span>Flow</span></a>
        <h1><%= pageTitle %></h1>
        
        <p>
            Nhập mã OTP gồm 6 chữ số được gửi đến 
            <strong style="color: #F8FAFC;"><%= email.isEmpty() ? "email của bạn" : email %></strong>
            <%= isChangeEmail ? " để xác nhận cập nhật địa chỉ email mới" : "" %>. 
            Mã có hiệu lực trong 15 phút.
        </p>

        <% if ("true".equals(resent)) { %>
            <div style="background: rgba(52, 211, 153, 0.12); border: 1px solid rgba(52, 211, 153, 0.3); color: #34D399; padding: 12px 14px; border-radius: 8px; font-size: 13px; margin-bottom: 20px;">
                ✓ Đã gửi mã OTP mới đến hòm thư của bạn. Vui lòng kiểm tra hộp thư đến (hoặc thư rác).
            </div>
        <% } %>

        <% if ("invalid".equals(error)) { %>
            <div style="background: rgba(248, 113, 113, 0.12); border: 1px solid rgba(248, 113, 113, 0.3); color: #F87171; padding: 12px 14px; border-radius: 8px; font-size: 13px; margin-bottom: 20px;">
                ✕ Mã OTP không chính xác hoặc đã hết hạn. Vui lòng thử lại hoặc bấm gửi lại mã.
            </div>
        <% } %>

        <form action="${pageContext.request.contextPath}/verify-otp" method="post">
            <input type="hidden" name="email" value="<%= email.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;") %>">
            <input type="hidden" name="action" value="<%= action %>">
            
            <label for="otp">Mã xác thực 6 chữ số</label>
            <input id="otp" name="otp" type="text" inputmode="numeric" pattern="[0-9]{6}" maxlength="6" autocomplete="one-time-code" placeholder="000000" required autofocus>
            
            <button type="submit"><%= buttonText %></button>
        </form>

        <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 24px; font-size: 13px;">
            <a class="resend-link" href="${pageContext.request.contextPath}/verify-otp?action=resend&email=<%= java.net.URLEncoder.encode(email, "UTF-8") %>&type=<%= action %>" style="color: var(--cf-primary, #FF2A54); text-decoration: none; font-weight: 600;">
                ↻ Gửi lại mã OTP
            </a>
            <% if (isChangeEmail) { %>
                <a class="back" href="${pageContext.request.contextPath}/account" style="margin-top: 0;">
                    Về hồ sơ cá nhân
                </a>
            <% } else { %>
                <a class="back" href="${pageContext.request.contextPath}/login" style="margin-top: 0;">
                    Quay lại đăng nhập
                </a>
            <% } %>
        </div>
    </main>
</body>
</html>
