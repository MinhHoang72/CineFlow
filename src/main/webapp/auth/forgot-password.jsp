<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    String error = request.getParameter("error");
    String submittedEmail = request.getParameter("email");
%>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="theme-color" content="#0B0F17">
    <title>CineFlow - Quên mật khẩu</title>

    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/base.css?v=<%= System.currentTimeMillis() %>">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/cineflow-auth.css?v=<%= System.currentTimeMillis() %>">
</head>

<body>

<div class="background">
    <div class="light light-1"></div>
    <div class="light light-2"></div>
</div>


<div class="container">

    <!-- CỘT GIỚI THIỆU BÊN TRÁI -->
    <div class="hero">
        <a class="logo" href="${pageContext.request.contextPath}/home/index.jsp">
            Cine<span>Flow</span>
        </a>

        <h1>
            Bạn quên<br>
            <span>mật khẩu?</span>
        </h1>

        <p>
            Đừng lo. Hãy nhập email đã đăng ký,
            chúng tôi sẽ gửi mã xác minh một lần.
        </p>

        <div class="steps">
            <div class="step">
                <div class="step-number">1</div>
                <span>Nhập email đã đăng ký</span>
            </div>

            <div class="step">
                <div class="step-number">2</div>
                <span>Nhập mã OTP được gửi qua email</span>
            </div>

            <div class="step">
                <div class="step-number">3</div>
                <span>Tạo mật khẩu mới</span>
            </div>
        </div>
    </div>

    <!-- KHỐI BIỂU MẪU ĐẶT LẠI MẬT KHẨU -->
    <div class="card">


        <h2>Đặt lại mật khẩu</h2>
        <div class="subtitle">
            Nhập địa chỉ email, chúng tôi sẽ gửi cho bạn một mã xác minh (OTP).
        </div>

        <form action="${pageContext.request.contextPath}/forgot-password" method="post">
            <div class="form-group">
                <label>Địa chỉ email</label>
                <div class="input-wrapper">
                    <span class="input-icon">@</span>
                    <input type="email"
                           name="email"
                           value="<%= submittedEmail == null ? "" : submittedEmail.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;") %>"
                           placeholder="Nhập email của bạn"
                           autocomplete="email"
                           required>
                </div>
            </div>

            <button type="submit" class="submit-btn">
                Gửi mã OTP
            </button>

            <% if ("email".equals(error)) { %>
                <p class="form-error" role="alert">Email này chưa được đăng ký. Vui lòng kiểm tra lại.</p>
            <% } %>
        </form>

        <div class="security-note">
            Vì lý do bảo mật, mã OTP sẽ hết hạn sau 15 phút.
        </div>

        <div class="back-login">
            Bạn nhớ mật khẩu rồi?
            <a href="${pageContext.request.contextPath}/auth/login.jsp">
                Quay lại đăng nhập
            </a>
        </div>
    </div>
</div>
</body>
</html>
