<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>CineFlow - Xác minh OTP</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/verify-otp.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/base.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/cineflow-auth.css">
</head>
<body>
    <main class="card">
        <a class="logo" href="${pageContext.request.contextPath}/home/index.jsp">Cine<span>Flow</span></a>
        <h1>Xác minh email</h1>
        <p>Nhập mã OTP gồm 6 chữ số được gửi đến email của bạn. Mã có hiệu lực trong 15 phút.</p>
        <form action="${pageContext.request.contextPath}/verify-otp" method="post">
            <input type="hidden" name="email" value="<%= request.getParameter("email") != null ? request.getParameter("email") : "" %>">
            <label for="otp">Verification code</label>
            <input id="otp" name="otp" type="text" inputmode="numeric" pattern="[0-9]{6}" maxlength="6" autocomplete="one-time-code" required>
            <button type="submit">Xác minh OTP</button>
        </form>
        <a class="back" href="${pageContext.request.contextPath}/auth/login.jsp">Quay lại đăng nhập</a>
    </main>
</body>
</html>




