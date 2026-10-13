<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">

        <title>CineFlow - Đăng nhập</title>

        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/login.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/base.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/cineflow-auth.css">
    </head>

    <body>

        <div class="background">
            <div class="lights light-1"></div>
            <div class="lights light-2"></div>
        </div>

        <div class="container">

            <!-- LEFT SIDE -->
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


            <!-- LOGIN CARD -->
            <div class="login-card">

                <h2>Chào mừng bạn trở lại</h2>

                <div class="subtitle">
                    Đăng nhập để tiếp tục với CineFlow
                </div>
                <% 
                    String redirectUrl = request.getParameter("redirect");
                    if ("session".equals(request.getParameter("error"))) { 
                %>
                <p class="form-error" role="alert">Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.</p>
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


                    <!-- Options -->
                    <div class="options">

                        <label class="remember">

                            <input type="checkbox">

                            <span>Ghi nhớ đăng nhập</span>

                        </label>

                        <a
                            class="forgot"
                            href="${pageContext.request.contextPath}/auth/forgot-password.jsp">
                            Quên mật khẩu?
                        </a>

                    </div>


                    <!-- Login -->
                    <button
                        type="submit"
                        class="login-btn">
                        Đăng nhập
                    </button>

                </form>



                <!-- Register -->
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

                const password =
                        document.getElementById("password");

                const toggle =
                        document.querySelector(".password-toggle");

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




