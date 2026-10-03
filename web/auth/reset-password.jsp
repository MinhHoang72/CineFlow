<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <title>CineFlow - Đặt lại mật khẩu</title>

    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/reset-password.css">
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
            Tạo mật khẩu<br>
            <span>password.</span>
        </h1>

        <p>
            OTP đã được xác minh. Hãy chọn mật khẩu mới cho tài khoản CineFlow.
        </p>

        <div class="security-list">

            <div class="security-item">
                <div class="check">✓</div>
                <span>Secure password recovery</span>
            </div>

            <div class="security-item">
                <div class="check">✓</div>
                <span>OTP verification is required</span>
            </div>

            <div class="security-item">
                <div class="check">✓</div>
                <span>Mật khẩu của bạn được mã hóa an toàn</span>
            </div>

        </div>

    </div>


    <!-- CARD -->

    <div class="card">

        <div class="icon-circle">
            🔒
        </div>

        <h2>Đặt lại mật khẩu</h2>

        <div class="subtitle">
            Nhập mật khẩu mới below.
        </div>


        <form action="${pageContext.request.contextPath}/reset-password"
              method="post">


            <!-- newPasswordPASSWORD -->

            <div class="form-group">

                <label>Mật khẩu mới</label>

                <div class="input-wrapper">

                    <span class="input-icon">●</span>

                    <input
                        id="newPassword"
                        type="password"
                        name="newPassword"
                        placeholder="Nhập mật khẩu mới"
                        autocomplete="new-password"
                        required>

                    <span
                        class="password-toggle"
                        onclick="togglePassword('newPassword', this)">
                        Hiện
                    </span>

                </div>

            </div>


            <!-- confirmPasswordPASSWORD -->

            <div class="form-group">

                <label>Xác nhận mật khẩu</label>

                <div class="input-wrapper">

                    <span class="input-icon">●</span>

                    <input
                        id="confirmPassword"
                        type="password"
                        name="confirmPassword"
                        placeholder="Xác nhận mật khẩu mới"
                        autocomplete="new-password"
                        required>

                    <span
                        class="password-toggle"
                        onclick="togglePassword('confirmPassword', this)">
                        Hiện
                    </span>

                </div>

            </div>


            <!-- BUTTON -->

            <button
                type="submit"
                class="reset-btn">
                Đặt lại mật khẩu
            </button>


            <div class="password-rule">
                Mã đặt lại mật khẩu có hiệu lực trong 15 phút.
                After successfully changing your password,
                the reset link can no longer be used.
            </div>

        </form>


        <div class="back-login">

            Bạn nhớ mật khẩu rồi?

            <a href="${pageContext.request.contextPath}/auth/login.jsp">
                Quay lại đăng nhập
            </a>

        </div>

    </div>

</div>


<script>

    function togglePassword(id, element) {

        const input = document.getElementById(id);

        if (input.type === "password") {

            input.type = "text";
            element.textContent = "Ẩn";

        } else {

            input.type = "password";
            element.textContent = "Hiện";

        }
    }

</script>

</body>
</html>






