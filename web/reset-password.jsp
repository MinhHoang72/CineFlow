<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <title>CineFlow - Reset Password</title>

    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
            font-family: "Segoe UI", Arial, sans-serif;
        }

        body {
            min-height: 100vh;
            background:
                radial-gradient(circle at 15% 20%, rgba(229, 9, 20, 0.20), transparent 30%),
                radial-gradient(circle at 85% 80%, rgba(229, 9, 20, 0.12), transparent 30%),
                #080808;
            color: white;
            display: flex;
            align-items: center;
            justify-content: center;
            overflow: hidden;
        }

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
                    rgba(0,0,0,0.95),
                    rgba(0,0,0,0.72),
                    rgba(0,0,0,0.94)
                );
        }

        .light {
            position: absolute;
            width: 450px;
            height: 450px;
            border-radius: 50%;
            background: rgba(229, 9, 20, 0.10);
            filter: blur(100px);
        }

        .light-1 {
            top: -180px;
            right: -100px;
        }

        .light-2 {
            bottom: -200px;
            left: -100px;
        }

        .container {
            width: 100%;
            max-width: 1100px;
            min-height: 650px;
            display: grid;
            grid-template-columns: 1fr 430px;
            gap: 80px;
            align-items: center;
            padding: 40px;
        }

        /* LEFT */

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
            font-size: 56px;
            line-height: 1.05;
            letter-spacing: -2px;
            margin-bottom: 25px;
        }

        .hero h1 span {
            color: #e50914;
        }

        .hero p {
            max-width: 510px;
            color: #999;
            font-size: 17px;
            line-height: 1.7;
        }

        .security-list {
            margin-top: 38px;
        }

        .security-item {
            display: flex;
            align-items: center;
            gap: 13px;
            margin-bottom: 15px;
            color: #999;
            font-size: 14px;
        }

        .check {
            width: 29px;
            height: 29px;
            border-radius: 8px;
            background: rgba(229, 9, 20, 0.12);
            border: 1px solid rgba(229, 9, 20, 0.25);
            color: #e50914;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 13px;
        }

        /* CARD */

        .card {
            width: 100%;
            background: rgba(20, 20, 20, 0.90);
            border: 1px solid rgba(255,255,255,0.08);
            border-radius: 24px;
            padding: 42px;
            box-shadow:
                0 30px 80px rgba(0,0,0,0.65),
                inset 0 1px 0 rgba(255,255,255,0.03);
            backdrop-filter: blur(20px);
        }

        .icon-circle {
            width: 58px;
            height: 58px;
            border-radius: 16px;
            background: rgba(229, 9, 20, 0.12);
            border: 1px solid rgba(229, 9, 20, 0.25);
            display: flex;
            align-items: center;
            justify-content: center;
            color: #e50914;
            font-size: 23px;
            margin-bottom: 25px;
        }

        .card h2 {
            font-size: 30px;
            margin-bottom: 9px;
        }

        .subtitle {
            color: #888;
            font-size: 14px;
            line-height: 1.6;
            margin-bottom: 28px;
        }

        /* FORM */

        .form-group {
            margin-bottom: 20px;
        }

        .form-group label {
            display: block;
            color: #d5d5d5;
            font-size: 13px;
            font-weight: 600;
            margin-bottom: 9px;
        }

        .input-wrapper {
            position: relative;
        }

        .input-icon {
            position: absolute;
            left: 15px;
            top: 50%;
            transform: translateY(-50%);
            color: #666;
            font-size: 15px;
        }

        .input-wrapper input {
            width: 100%;
            height: 52px;
            background: #111;
            border: 1px solid #292929;
            border-radius: 12px;
            outline: none;
            color: white;
            padding: 0 65px 0 44px;
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
            right: 14px;
            top: 50%;
            transform: translateY(-50%);
            color: #666;
            font-size: 12px;
            cursor: pointer;
            user-select: none;
        }

        .password-toggle:hover {
            color: white;
        }

        /* BUTTON */

        .reset-btn {
            width: 100%;
            height: 52px;
            margin-top: 3px;
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

        .reset-btn:hover {
            background: #f20d19;
            transform: translateY(-1px);
            box-shadow: 0 14px 30px rgba(229, 9, 20, 0.30);
        }

        .reset-btn:active {
            transform: translateY(0);
        }

        /* PASSWORD RULE */

        .password-rule {
            margin-top: 20px;
            padding: 13px 15px;
            border-radius: 10px;
            background: rgba(255,255,255,0.025);
            border: 1px solid rgba(255,255,255,0.05);
            color: #666;
            font-size: 11px;
            line-height: 1.6;
        }

        /* BACK */

        .back-login {
            text-align: center;
            margin-top: 25px;
            color: #777;
            font-size: 13px;
        }

        .back-login a {
            color: white;
            text-decoration: none;
            font-weight: 600;
        }

        .back-login a:hover {
            color: #e50914;
        }

        /* RESPONSIVE */

        @media (max-width: 900px) {

            body {
                overflow: auto;
            }

            .container {
                display: block;
                max-width: 500px;
                min-height: auto;
                padding: 25px;
            }

            .hero {
                display: none;
            }

            .card {
                padding: 35px 28px;
            }
        }

        @media (max-width: 450px) {

            .card {
                padding: 30px 22px;
                border-radius: 18px;
            }

            .card h2 {
                font-size: 26px;
            }
        }
    </style>
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
            Create a new<br>
            <span>password.</span>
        </h1>

        <p>
            OTP verified. Choose a new password for your CineFlow account.
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
                <span>Your password is securely encrypted</span>
            </div>

        </div>

    </div>


    <!-- CARD -->

    <div class="card">

        <div class="icon-circle">
            🔒
        </div>

        <h2>Reset password</h2>

        <div class="subtitle">
            Enter your new password below.
        </div>


        <form action="${pageContext.request.contextPath}/reset-password"
              method="post">


            <!-- NEW PASSWORD -->

            <div class="form-group">

                <label>New Password</label>

                <div class="input-wrapper">

                    <span class="input-icon">●</span>

                    <input
                        id="newPassword"
                        type="password"
                        name="newPassword"
                        placeholder="Enter your new password"
                        autocomplete="new-password"
                        required>

                    <span
                        class="password-toggle"
                        onclick="togglePassword('newPassword', this)">
                        Show
                    </span>

                </div>

            </div>


            <!-- CONFIRM PASSWORD -->

            <div class="form-group">

                <label>Confirm Password</label>

                <div class="input-wrapper">

                    <span class="input-icon">●</span>

                    <input
                        id="confirmPassword"
                        type="password"
                        name="confirmPassword"
                        placeholder="Confirm your new password"
                        autocomplete="new-password"
                        required>

                    <span
                        class="password-toggle"
                        onclick="togglePassword('confirmPassword', this)">
                        Show
                    </span>

                </div>

            </div>


            <!-- BUTTON -->

            <button
                type="submit"
                class="reset-btn">
                Reset Password
            </button>


            <div class="password-rule">
                Your reset link is valid for 15 minutes.
                After successfully changing your password,
                the reset link can no longer be used.
            </div>

        </form>


        <div class="back-login">

            Remember your password?

            <a href="${pageContext.request.contextPath}/login.jsp">
                Back to Login
            </a>

        </div>

    </div>

</div>


<script>

    function togglePassword(id, element) {

        const input = document.getElementById(id);

        if (input.type === "password") {

            input.type = "text";
            element.textContent = "Hide";

        } else {

            input.type = "password";
            element.textContent = "Show";

        }
    }

</script>

</body>
</html>