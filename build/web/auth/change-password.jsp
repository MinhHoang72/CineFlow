<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<% String changePasswordRole = (String) session.getAttribute("role"); boolean managerPassword = "MANAGER".equalsIgnoreCase(changePasswordRole); %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <title>CineFlow - Đổi mật khẩu</title>

    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/change-password.css">
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
            Bảo vệ tài khoản<br>
            <span>an toàn hơn.</span>
        </h1>

        <p>
            Thường xuyên đổi mật khẩu CineFlow để bảo vệ tài khoản và dữ liệu của bạn.
        </p>

        <div class="security-list">

            <div class="security-item">
                <div class="check">✓</div>
                <span>Xác minh mật khẩu hiện tại</span>
            </div>

            <div class="security-item">
                <div class="check">✓</div>
                <span>Tạo mật khẩu mới</span>
            </div>

            <div class="security-item">
                <div class="check">✓</div>
                <span>Mật khẩu mới của bạn được mã hóa an toàn</span>
            </div>

        </div>

    </div>


    <!-- CARD -->

    <div class="card">

        <div class="icon-circle">
            🔐
        </div>

        <h2>Đổi mật khẩu</h2>

        <div class="subtitle">
            Nhập mật khẩu hiện tại và chọn mật khẩu mới cho tài khoản.
        </div>


        <form action="${pageContext.request.contextPath}/change-password"
              method="post">


            <!-- currentPasswordPASSWORD -->

            <div class="form-group">

                <label>Mật khẩu hiện tại</label>

                <div class="input-wrapper">

                    <span class="input-icon">●</span>

                    <input
                        id="currentPassword"
                        type="password"
                        name="currentPassword"
                        placeholder="Nhập mật khẩu hiện tại"
                        autocomplete="current-password"
                        required>

                    <span
                        class="password-toggle"
                        onclick="togglePassword('currentPassword', this)">
                        Hiện
                    </span>

                </div>

            </div>


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


            <!-- confirmPassword-->

            <div class="form-group">

                <label>Xác nhận mật khẩu mới</label>

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
                class="change-btn">
                Đổi mật khẩu
            </button>


            <div class="password-note">
                Mật khẩu mới phải khác mật khẩu hiện tại.
            </div>

        </form>


        <div class="back-home">

            <% if (managerPassword) { %>
            <a href="${pageContext.request.contextPath}/dashboard">← Quay lại Dashboard Manager</a>
            <% } else { %>
            <a href="${pageContext.request.contextPath}/index.jsp">
                ← Quay lại trang chủ
            </a>
            <% } %>

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






