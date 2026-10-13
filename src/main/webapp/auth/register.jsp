<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%!
    private String escRegister(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }
%>
<%
    String registerError = request.getParameter("error");
    String submittedAccount = request.getParameter("account");
    String submittedEmail = request.getParameter("email");
%>

<!DOCTYPE html>
<html lang="vi">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="theme-color" content="#0B0F17">
    <title>CineFlow - Tạo tài khoản</title>

    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/base.css?v=<%= System.currentTimeMillis() %>">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth/cineflow-auth.css?v=<%= System.currentTimeMillis() %>">
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

            <a class="logo" href="${pageContext.request.contextPath}/home/index.jsp">

                Cine<span>Flow</span>

            </a>


            <h1>

                Tạo<br>

                <span>tài khoản</span> CineFlow.

            </h1>


            <p>

                Tham gia CineFlow để khám phá phim, xem lịch chiếu
                và quản lý hành trình điện ảnh trong một nơi.

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

                        Họ và tên

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
                            value="<%= escRegister(submittedAccount) %>"
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
                            value="<%= escRegister(submittedEmail) %>"
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

                                Ít nhất 9 ký tự

                            </span>

                        </div>



                        <div
                            id="uppercaseRequirement"
                            class="requirement invalid">

                            <span class="requirement-icon">

                            ✕

                            </span>

                            <span>

                                Có chữ cái viết hoa

                            </span>

                        </div>



                        <div
                            id="lowercaseRequirement"
                            class="requirement invalid">

                            <span class="requirement-icon">

                            ✕

                            </span>

                            <span>

                                Có chữ cái viết thường

                            </span>

                        </div>



                        <div
                            id="numberRequirement"
                            class="requirement invalid">

                            <span class="requirement-icon">

                            ✕
                            
                            </span>

                            <span>

                                Có chữ số

                            </span>

                        </div>

                        <div
                            id="specialRequirement"
                            class="requirement invalid">

                            <span class="requirement-icon">

                            ✕

                            </span>

                            <span>

                                Có ký tự đặc biệt

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
                        id="confirmPasswordMessage"
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
                <% if ("account".equals(registerError)) { %>
                    <p class="form-error" role="alert">Tên tài khoản này đã tồn tại. Vui lòng chọn tên khác.</p>
                <% } else if ("email".equals(registerError)) { %>
                    <p class="form-error" role="alert">Email này đã được đăng ký. Vui lòng sử dụng email khác.</p>
                <% } %>
                <!-- TERMS -->
                <div class="terms">
                    Khi tạo tài khoản, bạn đồng ý với
                    <span>
                        điều khoản sử dụng
                    </span>
                    of CineFlow.

                </div>
            </form>

            <!-- ================================
                 LOGIN LINK
                 ================================ -->

            <div class="login-link">

                Đã có tài khoản?


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
                        passwordInput.value.trim();


                    const confirmPassword =
                        confirmPasswordInput.value.trim();


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

                        confirmPasswordMessage.style.display = "none";
                        confirmPasswordMessage.classList.remove("valid", "invalid");
                        confirmPasswordMessage.textContent = "";

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







