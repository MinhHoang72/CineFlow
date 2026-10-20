<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    String error = (String) request.getAttribute("error");
    if (error == null) {
        error = request.getParameter("error");
    }
%>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="theme-color" content="#0B0F17">
    <title>CineFlow - Đặt lại mật khẩu</title>

    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/base.css?v=<%= System.currentTimeMillis() %>">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/cineflow-auth.css?v=<%= System.currentTimeMillis() %>">
</head>
<body>
    <div class="background">
        <div class="light light-1"></div>
        <div class="light light-2"></div>
    </div>

    <div class="container">
        <!-- CỘT GIỚI THIỆU THƯƠNG HIỆU BÊN TRÁI -->
        <div class="hero">
            <a class="logo" href="${pageContext.request.contextPath}/home/index.jsp">
                Cine<span>Flow</span>
            </a>

            <h1>
                Tạo mật khẩu<br>
                <span>mới.</span>
            </h1>

            <p>
                OTP đã được xác minh. Hãy chọn mật khẩu mới cho tài khoản CineFlow.
            </p>

            <div class="security-list">
                <div class="security-item">
                    <div class="check">✓</div>
                    <span>Khôi phục mật khẩu an toàn</span>
                </div>

                <div class="security-item">
                    <div class="check">✓</div>
                    <span>Yêu cầu xác minh OTP</span>
                </div>

                <div class="security-item">
                    <div class="check">✓</div>
                    <span>Mật khẩu của bạn được mã hóa an toàn</span>
                </div>
            </div>
        </div>

        <!-- KHỐI BIỂU MẪU ĐẶT LẠI MẬT KHẨU -->
        <div class="card">
            <h2>Đặt lại mật khẩu</h2>
            <div class="subtitle">
                Nhập mật khẩu mới cho tài khoản của bạn.
            </div>

            <% if ("missing_fields".equals(error)) { %>
                <p class="form-error" role="alert" style="margin-bottom: 16px;">Vui lòng nhập đầy đủ các trường mật khẩu.</p>
            <% } else if ("password_mismatch".equals(error)) { %>
                <p class="form-error" role="alert" style="margin-bottom: 16px;">Mật khẩu xác nhận không khớp.</p>
            <% } else if ("weak_password".equals(error)) { %>
                <p class="form-error" role="alert" style="margin-bottom: 16px;">Mật khẩu yếu. Yêu cầu tối thiểu 8 ký tự, gồm ít nhất 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt.</p>
            <% } else if ("reset_failed".equals(error)) { %>
                <p class="form-error" role="alert" style="margin-bottom: 16px;">Đặt lại mật khẩu thất bại. Vui lòng thử lại sau.</p>
            <% } %>

            <form action="${pageContext.request.contextPath}/reset-password" method="post">
                <!-- MẬT KHẨU MỚI -->
                <div class="form-group">
                    <label>Mật khẩu mới</label>
                    <div class="input-wrapper">
                        <span class="input-icon">●</span>
                        <input id="newPassword"
                               type="password"
                               name="newPassword"
                               placeholder="Nhập mật khẩu mới"
                               autocomplete="new-password"
                               required>
                        <span class="password-toggle" onclick="togglePassword('newPassword', this)">
                            Hiện
                        </span>
                    </div>
                </div>

                <!-- XÁC NHẬN MẬT KHẨU MỚI -->
                <div class="form-group">
                    <label>Xác nhận mật khẩu</label>
                    <div class="input-wrapper">
                        <span class="input-icon">●</span>
                        <input id="confirmPassword"
                               type="password"
                               name="confirmPassword"
                               placeholder="Xác nhận mật khẩu mới"
                               autocomplete="new-password"
                               required>
                        <span class="password-toggle" onclick="togglePassword('confirmPassword', this)">
                            Hiện
                        </span>
                    </div>
                </div>

                <!-- NÚT XÁC NHẬN ĐỔI MẬT KHẨU -->
                <button type="submit" class="reset-btn">
                    Đặt lại mật khẩu
                </button>

                <div class="password-rule">
                    Mã đặt lại mật khẩu có hiệu lực trong 15 phút.
                    Sau khi đổi mật khẩu thành công, mã đặt lại sẽ không thể sử dụng lại.
                </div>
            </form>

            <div class="back-login">
                Bạn nhớ mật khẩu rồi?
                <a href="${pageContext.request.contextPath}/login">
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
