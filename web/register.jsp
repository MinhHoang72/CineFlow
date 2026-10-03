<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html lang="en">

<head>

    <meta charset="UTF-8">

    <meta name="viewport"
          content="width=device-width, initial-scale=1.0">

    <title>CineFlow - Create Account</title>


    <style>

        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
            font-family: "Segoe UI", Arial, sans-serif;
        }


        /* ================================
           BODY
           ================================ */

        body {

            min-height: 100vh;

            background:
                radial-gradient(
                    circle at 15% 20%,
                    rgba(229, 9, 20, 0.20),
                    transparent 30%
                ),
                radial-gradient(
                    circle at 85% 80%,
                    rgba(229, 9, 20, 0.12),
                    transparent 30%
                ),
                #080808;

            color: white;

            display: flex;

            align-items: center;

            justify-content: center;

        }


        /* ================================
           BACKGROUND
           ================================ */

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


        /* ================================
           MAIN CONTAINER
           ================================ */

        .container {

            width: 100%;

            max-width: 1180px;

            min-height: 700px;

            display: grid;

            grid-template-columns: 1fr 450px;

            gap: 70px;

            align-items: center;

            padding: 40px;

        }


        /* ================================
           LEFT SIDE
           ================================ */

        .hero {

            padding-left: 30px;

        }


        .logo {

            font-size: 34px;

            font-weight: 800;

            letter-spacing: -1px;

            margin-bottom: 65px;

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

            max-width: 530px;

            color: #999;

            font-size: 17px;

            line-height: 1.7;

        }


        .movie-line {

            width: 80px;

            height: 4px;

            background: #e50914;

            border-radius: 10px;

            margin-top: 35px;

        }


        /* ================================
           REGISTER CARD
           ================================ */

        .register-card {

            width: 100%;

            background: rgba(20, 20, 20, 0.90);

            border: 1px solid rgba(255,255,255,0.08);

            border-radius: 24px;

            padding: 38px 42px;

            box-shadow:
                0 30px 80px rgba(0,0,0,0.65),
                inset 0 1px 0 rgba(255,255,255,0.03);

            backdrop-filter: blur(20px);

        }


        .register-card h2 {

            font-size: 30px;

            margin-bottom: 8px;

        }


        .subtitle {

            color: #888;

            font-size: 14px;

            margin-bottom: 27px;

        }


        /* ================================
           FORM
           ================================ */

        .form-group {

            margin-bottom: 16px;

        }


        .form-group label {

            display: block;

            color: #d5d5d5;

            font-size: 13px;

            font-weight: 600;

            margin-bottom: 8px;

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

            height: 49px;

            background: #111;

            border: 1px solid #292929;

            border-radius: 11px;

            outline: none;

            color: white;

            padding: 0 44px;

            font-size: 14px;

            transition: 0.25s;

        }


        .input-wrapper input::placeholder {

            color: #555;

        }


        .input-wrapper input:focus {

            border-color: #e50914;

            box-shadow:
                0 0 0 3px rgba(229, 9, 20, 0.10);

        }


        /* ================================
           PASSWORD TOGGLE
           ================================ */

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


        /* ================================
           PASSWORD REQUIREMENTS
           ================================ */

        .password-requirements {

            margin-top: 10px;

            margin-bottom: 18px;

        }


        .requirement {

            display: flex;

            align-items: center;

            gap: 8px;

            font-size: 12px;

            margin: 5px 0;

            transition: 0.2s ease;

        }


        .requirement.invalid {

            color: #ef4444;

        }


        .requirement.valid {

            color: #22c55e;

        }


        .requirement-icon {

            width: 16px;

            min-width: 16px;

            font-weight: 700;

            text-align: center;

        }


        /* ================================
           CONFIRM PASSWORD MESSAGE
           ================================ */

        .confirm-password-message {

            display: none;

            margin-top: 8px;

            font-size: 12px;

        }


        .confirm-password-message.invalid {

            color: #ef4444;

        }


        .confirm-password-message.valid {

            color: #22c55e;

        }


        /* ================================
           REGISTER BUTTON
           ================================ */

        .register-btn {

            width: 100%;

            height: 51px;

            margin-top: 5px;

            border: none;

            border-radius: 11px;

            background: #e50914;

            color: white;

            font-size: 15px;

            font-weight: 700;

            cursor: pointer;

            transition: 0.25s;

            box-shadow:
                0 10px 25px rgba(229, 9, 20, 0.22);

        }


        .register-btn:hover {

            background: #f20d19;

            transform: translateY(-1px);

            box-shadow:
                0 14px 30px rgba(229, 9, 20, 0.30);

        }


        .register-btn:active {

            transform: translateY(0);

        }


        /* ================================
           TERMS
           ================================ */

        .terms {

            margin-top: 17px;

            color: #666;

            font-size: 11px;

            line-height: 1.5;

            text-align: center;

        }


        .terms span {

            color: #aaa;

        }


        /* ================================
           LOGIN LINK
           ================================ */

        .login-link {

            text-align: center;

            margin-top: 23px;

            color: #777;

            font-size: 13px;

        }


        .login-link a {

            color: white;

            text-decoration: none;

            font-weight: 600;

        }


        .login-link a:hover {

            color: #e50914;

        }


        /* ================================
           RESPONSIVE
           ================================ */

        @media (max-width: 900px) {

            body {

                padding: 20px 0;

            }


            .container {

                display: block;

                max-width: 500px;

                min-height: auto;

                padding: 20px;

            }


            .hero {

                display: none;

            }


            .register-card {

                padding: 34px 28px;

            }

        }


        @media (max-width: 450px) {

            .register-card {

                padding: 30px 22px;

                border-radius: 18px;

            }


            .register-card h2 {

                font-size: 26px;

            }

        }

    </style>

</head>


<body>


    <!-- ================================
         BACKGROUND
         ================================ -->

    <div class="background">

        <div class="light light-1"></div>

        <div class="light light-2"></div>

    </div>



    <!-- ================================
         MAIN CONTAINER
         ================================ -->

    <div class="container">


        <!-- ================================
             LEFT SIDE
             ================================ -->

        <div class="hero">

            <div class="logo">

                Cine<span>Flow</span>

            </div>


            <h1>

                Create your<br>

                <span>cinema</span> account.

            </h1>


            <p>

                Join CineFlow and make your movie experience
                simpler. Discover films, find showtimes and
                manage your cinema journey in one place.

            </p>


            <div class="movie-line"></div>

        </div>



        <!-- ================================
             REGISTER CARD
             ================================ -->

        <div class="register-card">


            <h2>

                Create account

            </h2>


            <div class="subtitle">

                Enter your information to get started

            </div>



            <!-- ================================
                 REGISTER FORM
                 ================================ -->

            <form
                action="${pageContext.request.contextPath}/register"
                method="post">


                <!-- ================================
                     FULL NAME
                     ================================ -->

                <div class="form-group">

                    <label>

                        Full Name

                    </label>


                    <div class="input-wrapper">

                        <span class="input-icon">

                            ●

                        </span>


                        <input
                            type="text"
                            name="fullName"
                            placeholder="Enter your full name"
                            autocomplete="name"
                            required>

                    </div>

                </div>



                <!-- ================================
                     ACCOUNT
                     ================================ -->

                <div class="form-group">

                    <label>

                        Account

                    </label>


                    <div class="input-wrapper">

                        <span class="input-icon">

                            ◉

                        </span>


                        <input
                            type="text"
                            name="account"
                            placeholder="Choose an account"
                            autocomplete="username"
                            required>

                    </div>

                </div>



                <!-- ================================
                     EMAIL
                     ================================ -->

                <div class="form-group">

                    <label>

                        Email

                    </label>


                    <div class="input-wrapper">

                        <span class="input-icon">

                            @

                        </span>


                        <input
                            type="email"
                            name="email"
                            placeholder="Enter your email"
                            autocomplete="email"
                            required>

                    </div>

                </div>



                <!-- ================================
                     PASSWORD
                     ================================ -->

                <div class="form-group">

                    <label>

                        Password

                    </label>


                    <div class="input-wrapper">

                        <span class="input-icon">

                            ●

                        </span>


                        <input
                            id="password"
                            type="password"
                            name="password"
                            placeholder="Create a password"
                            autocomplete="new-password"
                            required>


                        <span
                            class="password-toggle"
                            onclick="togglePassword('password', this)">

                            Show

                        </span>

                    </div>


                    <!-- PASSWORD REQUIREMENTS -->

                    <div class="password-requirements">


                        <div
                            id="lengthRequirement"
                            class="requirement invalid">

                            <span class="requirement-icon">

                                ✕

                            </span>

                            <span>

                                At least 9 characters

                            </span>

                        </div>



                        <div
                            id="uppercaseRequirement"
                            class="requirement invalid">

                            <span class="requirement-icon">

                                ✕

                            </span>

                            <span>

                                Contains an uppercase letter

                            </span>

                        </div>



                        <div
                            id="lowercaseRequirement"
                            class="requirement invalid">

                            <span class="requirement-icon">

                                ✕

                            </span>

                            <span>

                                Contains a lowercase letter

                            </span>

                        </div>



                        <div
                            id="numberRequirement"
                            class="requirement invalid">

                            <span class="requirement-icon">

                                ✕
                            
                            </span>

                            <span>

                                Contains a number

                            </span>

                        </div>



                        <div
                            id="specialRequirement"
                            class="requirement invalid">

                            <span class="requirement-icon">

                                ✕

                            </span>

                            <span>

                                Contains a special character

                            </span>

                        </div>

                    </div>

                </div>



                <!-- ================================
                     CONFIRM PASSWORD
                     ================================ -->

                <div class="form-group">

                    <label>

                        Confirm Password

                    </label>


                    <div class="input-wrapper">

                        <span class="input-icon">

                            ●

                        </span>


                        <input
                            id="confirmPassword"
                            type="password"
                            name="confirmPassword"
                            placeholder="Confirm your password"
                            autocomplete="new-password"
                            required>


                        <span
                            class="password-toggle"
                            onclick="togglePassword('confirmPassword', this)">

                            Show

                        </span>

                    </div>


                    <!-- CONFIRM PASSWORD MESSAGE -->

                    <div
                        id="confirmPasswordMessage"
                        class="confirm-password-message">

                    </div>

                </div>



                <!-- ================================
                     SUBMIT
                     ================================ -->

                <button
                    type="submit"
                    class="register-btn">

                    Create Account

                </button>



                <!-- TERMS -->

                <div class="terms">

                    By creating an account, you agree to the

                    <span>

                        terms and conditions

                    </span>

                    of CineFlow.

                </div>


            </form>



            <!-- ================================
                 LOGIN LINK
                 ================================ -->

            <div class="login-link">

                Already have an account?


                <a
                    href="${pageContext.request.contextPath}/login.jsp">

                    Sign in

                </a>

            </div>


        </div>

    </div>



    <!-- ================================
         JAVASCRIPT
         ================================ -->

    <script>


        /* =================================
           SHOW / HIDE PASSWORD
           ================================= */

        function togglePassword(id, element) {

            const input =
                document.getElementById(id);


            if (input.type === "password") {

                input.type = "text";

                element.textContent = "Hide";

            } else {

                input.type = "password";

                element.textContent = "Show";

            }

        }



        /* =================================
           PASSWORD VALIDATION
           ================================= */

        document.addEventListener(
            "DOMContentLoaded",
            function () {


                const passwordInput =
                    document.getElementById("password");


                const confirmPasswordInput =
                    document.getElementById("confirmPassword");


                /* =============================
                   REQUIREMENT ELEMENTS
                   ============================= */

                const lengthRequirement =
                    document.getElementById(
                        "lengthRequirement"
                    );


                const uppercaseRequirement =
                    document.getElementById(
                        "uppercaseRequirement"
                    );


                const lowercaseRequirement =
                    document.getElementById(
                        "lowercaseRequirement"
                    );


                const numberRequirement =
                    document.getElementById(
                        "numberRequirement"
                    );


                const specialRequirement =
                    document.getElementById(
                        "specialRequirement"
                    );


                /* =============================
                   CONFIRM PASSWORD MESSAGE
                   ============================= */

                const confirmPasswordMessage =
                    document.getElementById(
                        "confirmPasswordMessage"
                    );



                /* =============================
                   PASSWORD INPUT
                   ============================= */

                passwordInput.addEventListener(
                    "input",
                    function () {


                        const password =
                            passwordInput.value;


                        /*
                         * At least 9 characters
                         */

                        updateRequirement(
                            lengthRequirement,
                            password.length >= 9
                        );


                        /*
                         * Uppercase
                         */

                        updateRequirement(
                            uppercaseRequirement,
                            /[A-Z]/.test(password)
                        );


                        /*
                         * Lowercase
                         */

                        updateRequirement(
                            lowercaseRequirement,
                            /[a-z]/.test(password)
                        );


                        /*
                         * Number
                         */

                        updateRequirement(
                            numberRequirement,
                            /[0-9]/.test(password)
                        );


                        /*
                         * Special character
                         */

                        updateRequirement(
                            specialRequirement,
                            /[@#$%^&*!]/.test(password)
                        );


                        /*
                         * Check Confirm Password
                         */

                        checkConfirmPassword();

                    }
                );



                /* =============================
                   CONFIRM PASSWORD INPUT
                   ============================= */

                confirmPasswordInput.addEventListener(
                    "input",
                    function () {

                        checkConfirmPassword();

                    }
                );



                /* =============================
                   CHECK CONFIRM PASSWORD
                   ============================= */

                function checkConfirmPassword() {


                    const password =
                        passwordInput.value;


                    const confirmPassword =
                        confirmPasswordInput.value;


                    /*
                     * Chưa nhập
                     */

                    if (confirmPassword.length === 0) {

                        confirmPasswordMessage.style.display =
                            "none";

                        confirmPasswordMessage.classList.remove(
                            "valid"
                        );

                        confirmPasswordMessage.classList.remove(
                            "invalid"
                        );

                        confirmPasswordMessage.textContent =
                            "";

                        return;

                    }


                    /*
                     * Password giống nhau
                     */

                    if (password === confirmPassword) {

                        confirmPasswordMessage.style.display =
                            "block";


                        confirmPasswordMessage.classList.remove(
                            "invalid"
                        );


                        confirmPasswordMessage.classList.add(
                            "valid"
                        );


                        confirmPasswordMessage.textContent =
                            "✓ Passwords match";

                    }


                    /*
                     * Password khác nhau
                     */

                    else {

                        confirmPasswordMessage.style.display =
                            "block";


                        confirmPasswordMessage.classList.remove(
                            "valid"
                        );


                        confirmPasswordMessage.classList.add(
                            "invalid"
                        );


                        confirmPasswordMessage.textContent =
                            "✕ Passwords do not match";

                    }

                }



                /* =============================
                   UPDATE REQUIREMENT
                   ============================= */

                function updateRequirement(
                    element,
                    valid
                ) {


                    const icon =
                        element.querySelector(
                            ".requirement-icon"
                        );


                    if (valid) {


                        element.classList.remove(
                            "invalid"
                        );


                        element.classList.add(
                            "valid"
                        );


                        icon.textContent =
                            "✓";


                    } else {


                        element.classList.remove(
                            "valid"
                        );


                        element.classList.add(
                            "invalid"
                        );


                        icon.textContent =
                            "✕";

                    }

                }

            }
        );

    </script>

</body>

</html>