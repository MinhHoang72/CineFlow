<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">

        <title>CineFlow - Login</title>

        <style>
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
                font-family: "Segoe UI", Arial, sans-serif;
            }
            body {
                background:
                    radial-gradient(
                    circle at 20% 20%,
                    rgba(229, 9, 20, 0.20),
                    transparent 30%
                    ),
                    radial-gradient(
                    circle at 80% 80%,
                    rgba(229, 9, 20, 0.12),
                    transparent 30%
                    ),
                    #080808;

                color: white;
                display: flex;
                align-items: center;
                justify-content: center;
                overflow: hidden;
            }

            /* Background */
            .background {
                position: fixed;
                inset: 0;
                z-index: -1;
                overflow: hidden;
            }

            .background::before {
                content: "";
                position: absolute;
                inset: 0;
                background:
                    linear-gradient(
                    90deg,
                    rgba(0,0,0,0.95) 0%,
                    rgba(0,0,0,0.75) 45%,
                    rgba(0,0,0,0.92) 100%
                    );
            }

            .lights {
                position: absolute;
                width: 500px;
                height: 500px;
                border-radius: 50%;
                background: rgba(229, 9, 20, 0.10);
                filter: blur(100px);
            }

            .light-1 {
                top: -200px;
                left: -100px;
            }

            .light-2 {
                bottom: -200px;
                right: -100px;
            }

            /* Main */
            .container {
                width: 100%;
                max-width: 1180px;
                min-height: 680px;
                display: grid;
                grid-template-columns: 1fr 430px;
                gap: 70px;
                align-items: center;
                padding: 40px;
            }

            /* Left */
            .hero {
                padding-left: 30px;
            }

            .logo {
                font-size: 34px;
                font-weight: 800;
                letter-spacing: -1px;
                margin-bottom: 70px;
            }

            .logo span {
                color: #e50914;
            }

            .hero h1 {
                font-size: 58px;
                line-height: 1.05;
                letter-spacing: -2px;
                margin-bottom: 25px;
            }

            .hero h1 span {
                color: #e50914;
            }

            .hero p {
                max-width: 540px;
                color: #a7a7a7;
                font-size: 17px;
                line-height: 1.7;
            }

            .features {
                display: flex;
                gap: 35px;
                margin-top: 45px;
            }

            .feature {
                display: flex;
                align-items: center;
                gap: 12px;
                color: #bdbdbd;
                font-size: 14px;
            }

            .feature-icon {
                width: 38px;
                height: 38px;
                border-radius: 10px;
                background: rgba(229, 9, 20, 0.12);
                border: 1px solid rgba(229, 9, 20, 0.25);
                display: flex;
                align-items: center;
                justify-content: center;
                color: #e50914;
                font-size: 18px;
            }

            /* Login Card */
            .login-card {
                width: 100%;
                background: rgba(20, 20, 20, 0.88);
                border: 1px solid rgba(255,255,255,0.08);
                border-radius: 24px;
                padding: 42px;
                box-shadow:
                    0 30px 80px rgba(0,0,0,0.65),
                    inset 0 1px 0 rgba(255,255,255,0.03);
                backdrop-filter: blur(20px);
            }

            .login-card h2 {
                font-size: 30px;
                margin-bottom: 8px;
            }

            .subtitle {
                color: #888;
                font-size: 14px;
                margin-bottom: 32px;
            }

            /* Form */
            .form-group {
                margin-bottom: 20px;
            }

            .form-group label {
                display: block;
                font-size: 13px;
                font-weight: 600;
                color: #d4d4d4;
                margin-bottom: 9px;
            }

            .input-wrapper {
                position: relative;
            }

            .input-wrapper .icon {
                position: absolute;
                left: 15px;
                top: 50%;
                transform: translateY(-50%);
                color: #666;
                font-size: 16px;
            }

            .input-wrapper input {
                width: 100%;
                height: 52px;
                background: #111;
                border: 1px solid #292929;
                border-radius: 12px;
                color: white;
                outline: none;
                padding: 0 45px;
                font-size: 14px;
                transition: 0.25s;
            }

            .input-wrapper input::placeholder {
                color: #555;
            }

            .input-wrapper input:focus {
                border-color: #e50914;
                box-shadow: 0 0 0 3px rgba(229, 9, 20, 0.10);
            }

            .password-toggle {
                position: absolute;
                right: 15px;
                top: 50%;
                transform: translateY(-50%);
                color: #666;
                cursor: pointer;
                font-size: 14px;
                user-select: none;
            }

            .password-toggle:hover {
                color: white;
            }

            /* Options */
            .options {
                display: flex;
                justify-content: space-between;
                align-items: center;
                margin: 3px 0 25px;
                font-size: 13px;
            }

            .remember {
                display: flex;
                align-items: center;
                gap: 8px;
                color: #888;
            }

            .remember input {
                accent-color: #e50914;
            }

            .forgot {
                color: #e50914;
                text-decoration: none;
            }

            .forgot:hover {
                text-decoration: underline;
            }

            /* Button */
            .login-btn {
                width: 100%;
                height: 52px;
                border: none;
                border-radius: 12px;
                background: #e50914;
                color: white;
                font-size: 15px;
                font-weight: 700;
                cursor: pointer;
                transition: 0.25s;
                box-shadow: 0 10px 25px rgba(229, 9, 20, 0.22);
            }

            .login-btn:hover {
                background: #f20d19;
                transform: translateY(-1px);
                box-shadow: 0 14px 30px rgba(229, 9, 20, 0.30);
            }

            .login-btn:active {
                transform: translateY(0);
            }

            /* Register */
            .register {
                text-align: center;
                color: #777;
                font-size: 13px;
                margin-top: 28px;
            }

            .register a {
                color: white;
                font-weight: 600;
                text-decoration: none;
            }

            .register a:hover {
                color: #e50914;
            }

            /* Divider */
            .divider {
                display: flex;
                align-items: center;
                gap: 12px;
                margin: 25px 0;
                color: #555;
                font-size: 12px;
            }

            .divider::before,
            .divider::after {
                content: "";
                flex: 1;
                height: 1px;
                background: #292929;
            }

            /* Responsive */
            @media (max-width: 900px) {
                body {
                    overflow: auto;
                }

                .container {
                    grid-template-columns: 1fr;
                    max-width: 500px;
                    padding: 25px;
                }

                .hero {
                    display: none;
                }

                .login-card {
                    padding: 35px 28px;
                }
            }

            @media (max-width: 450px) {
                .login-card {
                    padding: 30px 22px;
                    border-radius: 18px;
                }

                .login-card h2 {
                    font-size: 26px;
                }
            }
        </style>
    </head>

    <body>

        <div class="background">
            <div class="lights light-1"></div>
            <div class="lights light-2"></div>
        </div>

        <div class="container">

            <!-- LEFT SIDE -->
            <div class="hero">

                <div class="logo">
                    Cine<span>Flow</span>
                </div>

                <h1>
                    Your movies.<br>
                    Your <span>moment.</span>
                </h1>

                <p>
                    Discover movies, explore showtimes and manage your
                    cinema experience with CineFlow.
                </p>

                <div class="features">

                    <div class="feature">
                        <div class="feature-icon">▶</div>
                        <span>Discover Movies</span>
                    </div>

                    <div class="feature">
                        <div class="feature-icon">◷</div>
                        <span>Find Showtimes</span>
                    </div>

                    <div class="feature">
                        <div class="feature-icon">★</div>
                        <span>Enjoy Cinema</span>
                    </div>

                </div>

            </div>


            <!-- LOGIN CARD -->
            <div class="login-card">

                <h2>Welcome back</h2>

                <div class="subtitle">
                    Sign in to continue to CineFlow
                </div>

                <form action="${pageContext.request.contextPath}/login"
                      method="post">

                    <!-- Account / Email -->
                    <div class="form-group">

                        <label>Account or Email</label>

                        <div class="input-wrapper">

                            <span class="icon">◉</span>

                            <input
                                type="text"
                                name="accountOrEmail"
                                placeholder="Enter your account or email"
                                autocomplete="username"
                                required>

                        </div>

                    </div>


                    <!-- Password -->
                    <div class="form-group">

                        <label>Password</label>

                        <div class="input-wrapper">

                            <span class="icon">●</span>

                            <input
                                id="password"
                                type="password"
                                name="password"
                                placeholder="Enter your password"
                                autocomplete="current-password"
                                required>

                            <span
                                class="password-toggle"
                                onclick="togglePassword()">
                                Show
                            </span>

                        </div>

                    </div>


                    <!-- Options -->
                    <div class="options">

                        <label class="remember">

                            <input type="checkbox">

                            <span>Remember me</span>

                        </label>

                        <a
                            class="forgot"
                            href="${pageContext.request.contextPath}/forgot-password.jsp">
                            Forgot password?
                        </a>

                    </div>


                    <!-- Login -->
                    <button
                        type="submit"
                        class="login-btn">
                        Sign In
                    </button>

                </form>


                <div class="divider">
                    OR
                </div>


                <!-- Register -->
                <div class="register">

                    Don't have an account?

                    <a href="${pageContext.request.contextPath}/register.jsp">
                        Create account
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
                    toggle.textContent = "Hide";

                } else {

                    password.type = "password";
                    toggle.textContent = "Show";

                }
            }

        </script>

    </body>
</html>