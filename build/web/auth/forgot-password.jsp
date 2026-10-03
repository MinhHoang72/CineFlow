<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <title>CineFlow - Quên mật khẩu</title>

    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/forgot-password.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/base.css">
</head>

<body>

<div class="background">
    <div class="light light-1"></div>
    <div class="light light-2"></div>
</div>


<div class="container">

    <!-- LEFT -->

    <div class="hero">

        <div class="logo">
            Cine<span>Flow</span>
        </div>

        <h1>
            Bạn quên<br>
            <span>password?</span>
        </h1>

        <p>
            Đừng lo. Hãy nhập địa chỉ email đã đăng ký
            with your CineFlow account and we'll send you
            a one-time verification code.
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


    <!-- CARD -->

    <div class="card">

        <div class="icon-circle">
            @
        </div>

            <h2>Đặt lại mật khẩu</h2>

        <div class="subtitle">
            Nhập địa chỉ email, chúng tôi sẽ gửi cho bạn
            a one-time password (OTP).
        </div>


        <form action="${pageContext.request.contextPath}/forgot-password"
              method="post">

            <div class="form-group">

                <label>Địa chỉ email</label>

                <div class="input-wrapper">

                    <span class="input-icon">@</span>

                    <input
                        type="email"
                        name="email"
                        placeholder="Nhập email của bạn"
                        autocomplete="email"
                        required>

                </div>

            </div>


            <button
                type="submit"
                class="submit-btn">
                Send OTP
            </button>

        </form>


        <div class="security-note">
            For security, the OTP will expire after 15 minutes.
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




