<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CineFlow - Verify OTP</title>
    <style>
        * { box-sizing: border-box; font-family: "Segoe UI", Arial, sans-serif; }
        body { min-height: 100vh; margin: 0; display: grid; place-items: center; color: white; background: #080808; }
        .card { width: min(430px, calc(100% - 40px)); padding: 42px; background: #151515; border: 1px solid #2a2a2a; border-radius: 20px; box-shadow: 0 25px 70px #000; }
        .logo { font-size: 30px; font-weight: 800; margin-bottom: 50px; }
        .logo span { color: #e50914; }
        h1 { margin: 0 0 10px; font-size: 30px; }
        p { color: #999; line-height: 1.6; font-size: 14px; margin: 0 0 28px; }
        label { display: block; color: #ddd; font-size: 13px; font-weight: 600; margin-bottom: 9px; }
        input { width: 100%; height: 52px; border: 1px solid #333; border-radius: 10px; background: #0e0e0e; color: white; padding: 0 15px; font-size: 18px; letter-spacing: 5px; text-align: center; outline: none; }
        input:focus { border-color: #e50914; box-shadow: 0 0 0 3px rgba(229,9,20,.12); }
        button { width: 100%; height: 52px; margin-top: 22px; border: 0; border-radius: 10px; background: #e50914; color: white; font-size: 15px; font-weight: 700; cursor: pointer; }
        button:hover { background: #f20d19; }
        .back { display: block; margin-top: 22px; color: #999; text-align: center; font-size: 13px; text-decoration: none; }
        .back:hover { color: white; }
    </style>
</head>
<body>
    <main class="card">
        <div class="logo">Cine<span>Flow</span></div>
        <h1>Verify your email</h1>
        <p>Enter the 6-digit OTP sent to your email address. The code expires in 15 minutes.</p>
        <form action="${pageContext.request.contextPath}/verify-otp" method="post">
            <input type="hidden" name="email" value="<%= request.getParameter("email") != null ? request.getParameter("email") : "" %>">
            <label for="otp">Verification code</label>
            <input id="otp" name="otp" type="text" inputmode="numeric" pattern="[0-9]{6}" maxlength="6" autocomplete="one-time-code" required>
            <button type="submit">Verify OTP</button>
        </form>
        <a class="back" href="${pageContext.request.contextPath}/login.jsp">Back to login</a>
    </main>
</body>
</html>