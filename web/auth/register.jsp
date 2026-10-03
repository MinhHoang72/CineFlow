<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html lang="en">

<head>

    <meta charset="UTF-8">

    <meta name="viewport"
          content="width=device-width, initial-scale=1.0">

    <title>CineFlow - Tạo tài khoản</title>


    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/register.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/base.css">

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

                Tạo<br>

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

                Tạo tài khoản

            </h2>


            <div class="subtitle">

                Nhập thông tin để bắt đầu sử dụng CineFlow

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
                            placeholder="Nhập họ và tên"
                            autocomplete="name"
                            required>

                    </div>

                </div>



                <!-- ================================
                     ACCOUNT
                     ================================ -->

                <div class="form-group">

                    <label>

                        Tài khoản

                    </label>


                    <div class="input-wrapper">

                        <span class="input-icon">

                            @

                        </span>


                        <input
                            type="text"
                            name="account"
                            placeholder="Chọn tên tài khoản"
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
                            placeholder="Nhập email của bạn"
                            autocomplete="email"
                            required>

                    </div>

                </div>



                <!-- ================================
                     PASSWORD
                     ================================ -->

                <div class="form-group">

                    <label>

                        Mật khẩu

                    </label>


                    <div class="input-wrapper">

                        <span class="input-icon">

                            ●

                        </span>


                        <input
                            id="password"
                            type="password"
                            name="password"
                            placeholder="Tạo mật khẩu"
                            autocomplete="new-password"
                            required>


                        <span
                            class="password-toggle"
                            onclick="togglePassword('password', this)">

                            Hiện

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
                     confirmPasswordPASSWORD
                     ================================ -->

                <div class="form-group">

                    <label>

                        Xác nhận mật khẩu

                    </label>


                    <div class="input-wrapper">

                        <span class="input-icon">

                            ●

                        </span>


                        <input
                            id="confirmPassword"
                            type="password"
                            name="confirmPassword"
                            placeholder="Xác nhận mật khẩu"
                            autocomplete="new-password"
                            required>


                        <span
                            class="password-toggle"
                            onclick="togglePassword('confirmPassword', this)">

                            Hiện

                        </span>

                    </div>


                    <!-- confirmPasswordPASSWORD MESSAGE -->

                    <div
                        id="confirmPassword"
                        class="confirmPasswordMessage">

                    </div>

                </div>



                <!-- ================================
                     SUBMIT
                     ================================ -->

                <button
                    type="submit"
                    class="register-btn">

                    Tạo tài khoản

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
                    href="${pageContext.request.contextPath}/auth/login.jsp">

                    Đăng nhập

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

                element.textContent = "Ẩn";

            } else {

                input.type = "password";

                element.textContent = "Hiện";

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
                   confirmPasswordPASSWORD MESSAGE
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
                            /[^A-Za-z0-9]/.test(password)
                        );


                        /*
                         * Check Xác nhận mật khẩu
                         */

                        checkConfirmPassword();

                    }
                );



                /* =============================
                   confirmPasswordPASSWORD INPUT
                   ============================= */

                confirmPasswordInput.addEventListener(
                    "input",
                    function () {

                        checkConfirmPassword();

                    }
                );



                /* =============================
                   CHECK confirmPasswordPASSWORD
                   ============================= */

                function checkConfirmPassword() {


                    const password =
                        passwordInput.value;


                    const confirmPassword =
                        confirmPasswordInput.value;


                    /*
                     * ChÆ°a nháº­p
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
                     * Mật khẩu giá»‘ng nhau
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
                            "✓ Mật khẩu khớp nhau";

                    }


                    /*
                     * Mật khẩu khÃ¡c nhau
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
                            "✕ Mật khẩu không khớp";

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







