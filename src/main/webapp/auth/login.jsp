<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">

        <meta name="theme-color" content="#0B0F17">
        <title>CineFlow - Đăng nhập</title>

        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/base.css?v=<%= System.currentTimeMillis() %>">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/cineflow-auth.css?v=<%= System.currentTimeMillis() %>">
    </head>

    <body>

        <div class="background">
            <div class="lights light-1"></div>
            <div class="lights light-2"></div>
        </div>

        <div class="container">
            <!-- CỘT GIỚI THIỆU THƯƠNG HIỆU BÊN TRÁI -->
            <div class="hero">
                <a class="logo" href="${pageContext.request.contextPath}/home/index.jsp">
                    Cine<span>Flow</span>
                </a>

                <h1>
                    Bộ phim của bạn.<br>
                    Khoảnh khắc <span>của bạn.</span>
                </h1>

                <p>
                    Khám phá phim, xem lịch chiếu và quản lý trải nghiệm
                    điện ảnh của bạn cùng CineFlow.
                </p>

                <div class="features">
                    <div class="feature">
                        <div class="feature-icon">▶</div>
                        <span>Khám phá phim</span>
                    </div>

                    <div class="feature">
                        <div class="feature-icon">◷</div>
                        <span>Xem lịch chiếu</span>
                    </div>

                    <div class="feature">
                        <div class="feature-icon">★</div>
                        <span>Tận hưởng điện ảnh</span>
                    </div>
                </div>
            </div>

            <!-- KHỐI FORM ĐĂNG NHẬP -->
            <div class="login-card">

                <h2>Chào mừng bạn trở lại</h2>

                <div class="subtitle">
                    Đăng nhập để tiếp tục với CineFlow
                </div>
                <% 
                    String redirectUrl = request.getParameter("redirect");
                    String errorParam = request.getParameter("error");
                    String unverifiedEmail = request.getParameter("email");
                    String verifiedParam = request.getParameter("verified");
                %>
                <% if ("session".equals(errorParam)) { %>
                    <p class="form-error" role="alert">Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.</p>
                <% } else if ("invalid".equals(errorParam)) { %>
                    <p class="form-error" role="alert">Tài khoản/email hoặc mật khẩu không chính xác.</p>
                <% } else if ("locked".equals(errorParam)) { %>
                    <p class="form-error" role="alert">Tài khoản này đã bị tạm khóa bởi Quản trị viên.</p>
                <% } else if ("inactive".equals(errorParam)) { %>
                    <p class="form-error" role="alert">Tài khoản này đang tạm ngưng hoạt động. Vui lòng liên hệ Quản trị viên.</p>
                <% } else if ("unverified".equals(errorParam)) { %>
                    <div style="background: rgba(245, 158, 11, 0.12); border: 1px solid rgba(245, 158, 11, 0.3); color: #F59E0B; padding: 12px 14px; border-radius: 8px; font-size: 13px; margin-bottom: 18px; line-height: 1.5;">
                        ⚠ Tài khoản của bạn chưa được kích hoạt qua email.<br>
                        <a href="${pageContext.request.contextPath}/verify-otp?action=resend&email=<%= unverifiedEmail != null ? java.net.URLEncoder.encode(unverifiedEmail, "UTF-8") : "" %>&type=register" style="color: #FF2A54; font-weight: 700; text-decoration: underline;">
                            Nhận mã OTP và kích hoạt ngay ›
                        </a>
                    </div>
                <% } %>
                <% if ("password_reset".equals(request.getParameter("success"))) { %>
                    <div style="background: rgba(52, 211, 153, 0.12); border: 1px solid rgba(52, 211, 153, 0.3); color: #34D399; padding: 12px 14px; border-radius: 8px; font-size: 13px; margin-bottom: 18px;">
                        ✓ Đặt lại mật khẩu thành công! Bạn có thể đăng nhập bằng mật khẩu mới.
                    </div>
                <% } %>
                <% if ("true".equals(verifiedParam)) { %>
                    <div style="background: rgba(52, 211, 153, 0.12); border: 1px solid rgba(52, 211, 153, 0.3); color: #34D399; padding: 12px 14px; border-radius: 8px; font-size: 13px; margin-bottom: 18px;">
                        ✓ Kích hoạt tài khoản thành công! Bạn có thể đăng nhập ngay bây giờ.
                    </div>
                <% } %>


                <form action="${pageContext.request.contextPath}/login"
                      method="post">
                    <% if (redirectUrl != null && !redirectUrl.trim().isEmpty()) { %>
                        <input type="hidden" name="redirect" value="<%= redirectUrl.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;") %>">
                    <% } %>

                    <!-- Tài khoản / Email -->
                    <div class="form-group">

                        <label>Tài khoản hoặc email</label>

                        <div class="input-wrapper">

                            <span class="icon">@</span>

                            <input
                                type="text"
                                name="accountOrEmail"
                                placeholder="Nhập tài khoản hoặc email"
                                autocomplete="username"
                                required>
                        </div>

                    </div>


                    <!-- Mật khẩu -->
                    <div class="form-group">

                        <label>Mật khẩu</label>

                        <div class="input-wrapper">

                            <span class="icon">●</span>

                            <input
                                id="password"
                                type="password"
                                name="password"
                                placeholder="Nhập mật khẩu"
                                autocomplete="current-password"
                                required>

                            <span
                                class="password-toggle"
                                onclick="togglePassword()">
                                Hiện
                            </span>

                        </div>

                    </div>


                    <!-- TÙY CHỌN BỔ SUNG -->
                    <div class="options">
                        <label class="remember">
                            <input type="checkbox">
                            <span>Ghi nhớ đăng nhập</span>
                        </label>

                        <a class="forgot" href="${pageContext.request.contextPath}/auth/forgot-password.jsp">
                            Quên mật khẩu?
                        </a>
                    </div>

                    <!-- NÚT ĐĂNG NHẬP -->
                    <button type="submit" class="login-btn">
                        Đăng nhập
                    </button>
                </form>

                <!-- ĐIỀU HƯỚNG ĐĂNG KÝ -->
                <div class="register">
                    Chưa có tài khoản?
                    <a href="${pageContext.request.contextPath}/auth/register.jsp<%= redirectUrl != null && !redirectUrl.trim().isEmpty() ? ("?redirect=" + java.net.URLEncoder.encode(redirectUrl, "UTF-8")) : "" %>">
                        Tạo tài khoản
                    </a>
                </div>
            </div>
        </div>

        <script>
            function togglePassword() {
                const password = document.getElementById("password");
                const toggle = document.querySelector(".password-toggle");

                if (password.type === "password") {
                    password.type = "text";
                    toggle.textContent = "Ẩn";
                } else {
                    password.type = "password";
                    toggle.textContent = "Hiện";
                }
            }
        </script>
    </body>
</html>
