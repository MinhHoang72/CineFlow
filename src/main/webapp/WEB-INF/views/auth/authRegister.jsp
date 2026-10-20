<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%!
    private String escRegister(String value) {
        return value == null ? "" : value
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
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
    <!-- HIỆU ỨNG ÁNH SÁNG NỀN -->
    <div class="background">
        <div class="light light-1"></div>
        <div class="light light-2"></div>
    </div>

    <!-- CONTAINER CHÍNH -->
    <div class="container">
        <!-- CỘT GIỚI THIỆU THƯƠNG HIỆU BÊN TRÁI -->
        <div class="hero">
            <a class="logo" href="${pageContext.request.contextPath}/home">
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

        <!-- KHỐI FORM ĐĂNG KÝ -->
        <div class="register-card">
            <h2>Tạo tài khoản</h2>
            <div class="subtitle">
                Nhập thông tin để bắt đầu sử dụng CineFlow
            </div>

            <!-- FORM ĐIỀN THÔNG TIN ĐĂNG KÝ -->
            <form action="${pageContext.request.contextPath}/register" method="post">
                <!-- HỌ VÀ TÊN -->
                <div class="form-group">
                    <label>Họ và tên</label>
                    <div class="input-wrapper">
                        <span class="input-icon">●</span>
                        <input type="text"
                               name="fullName"
                               placeholder="Nhập họ và tên"
                               autocomplete="name"
                               required>
                    </div>
                </div>

                <!-- TÊN TÀI KHOẢN -->
                <div class="form-group">
                    <label>Tài khoản</label>
                    <div class="input-wrapper">
                        <span class="input-icon">@</span>
                        <input type="text"
                               name="account"
                               value="<%= escRegister(submittedAccount) %>"
                               placeholder="Chọn tên tài khoản"
                               autocomplete="username"
                               required>
                    </div>
                </div>

                <!-- ĐỊA CHỈ EMAIL -->
                <div class="form-group">
                    <label>Email</label>
                    <div class="input-wrapper">
                        <span class="input-icon">@</span>
                        <input type="email"
                               name="email"
                               value="<%= escRegister(submittedEmail) %>"
                               placeholder="Nhập email của bạn"
                               autocomplete="email"
                               required>
                    </div>
                </div>

                <!-- MẬT KHẨU -->
                <div class="form-group">
                    <label>Mật khẩu</label>
                    <div class="input-wrapper">
                        <span class="input-icon">●</span>
                        <input id="password"
                               type="password"
                               name="password"
                               placeholder="Tạo mật khẩu"
                               autocomplete="new-password"
                               required>
                        <span class="password-toggle" onclick="togglePassword('password', this)">
                            Hiện
                        </span>
                    </div>

                    <!-- TIÊU CHÍ BẢO MẬT MẬT KHẨU -->
                    <div class="password-requirements">
                        <div id="lengthRequirement" class="requirement invalid">
                            <span class="requirement-icon">✕</span>
                            <span>Ít nhất 9 ký tự</span>
                        </div>
                        <div id="uppercaseRequirement" class="requirement invalid">
                            <span class="requirement-icon">✕</span>
                            <span>Có chữ cái viết hoa</span>
                        </div>
                        <div id="lowercaseRequirement" class="requirement invalid">
                            <span class="requirement-icon">✕</span>
                            <span>Có chữ cái viết thường</span>
                        </div>
                        <div id="numberRequirement" class="requirement invalid">
                            <span class="requirement-icon">✕</span>
                            <span>Có chữ số</span>
                        </div>
                        <div id="specialRequirement" class="requirement invalid">
                            <span class="requirement-icon">✕</span>
                            <span>Có ký tự đặc biệt</span>
                        </div>
                    </div>
                </div>

                <!-- XÁC NHẬN MẬT KHẨU -->
                <div class="form-group">
                    <label>Xác nhận mật khẩu</label>
                    <div class="input-wrapper">
                        <span class="input-icon">●</span>
                        <input id="confirmPassword"
                               type="password"
                               name="confirmPassword"
                               placeholder="Xác nhận mật khẩu"
                               autocomplete="new-password"
                               required>
                        <span class="password-toggle" onclick="togglePassword('confirmPassword', this)">
                            Hiện
                        </span>
                    </div>

                    <!-- THÔNG BÁO XÁC NHẬN MẬT KHẨU -->
                    <div id="confirmPasswordMessage" class="confirmPasswordMessage"></div>
                </div>

                <!-- NÚT ĐĂNG KÝ -->
                <button type="submit" class="register-btn">
                    Tạo tài khoản
                </button>

                <% if ("account".equals(registerError)) { %>
                    <p class="form-error" role="alert">Tên tài khoản này đã tồn tại. Vui lòng chọn tên khác.</p>
                <% } else if ("email".equals(registerError)) { %>
                    <p class="form-error" role="alert">Email này đã được đăng ký. Vui lòng sử dụng email khác.</p>
                <% } else if ("tempmail".equals(registerError)) { %>
                    <p class="form-error" role="alert">Vui lòng sử dụng địa chỉ email chính thức (Gmail, Outlook, v.v.) để đăng ký và nhận vé xem phim.</p>
                <% } else if ("invalid_email".equals(registerError)) { %>
                    <p class="form-error" role="alert">Định dạng email không hợp lệ. Vui lòng kiểm tra lại.</p>
                <% } else if ("invalid_account".equals(registerError)) { %>
                    <p class="form-error" role="alert">Tên tài khoản không hợp lệ (3 - 30 ký tự, gồm chữ, số, dấu chấm, gạch dưới, gạch ngang).</p>
                <% } else if ("invalid_name".equals(registerError)) { %>
                    <p class="form-error" role="alert">Họ và tên không hợp lệ (từ 2 đến 100 ký tự).</p>
                <% } else if ("password_mismatch".equals(registerError)) { %>
                    <p class="form-error" role="alert">Mật khẩu xác nhận không trùng khớp.</p>
                <% } else if ("weak_password".equals(registerError)) { %>
                    <p class="form-error" role="alert">Mật khẩu chưa đủ độ mạnh (tối thiểu 8 ký tự, có chữ hoa, thường, số và ký tự đặc biệt).</p>
                <% } else if ("missing_fields".equals(registerError)) { %>
                    <p class="form-error" role="alert">Vui lòng điền đầy đủ các thông tin bắt buộc.</p>
                <% } %>

                <!-- ĐIỀU KHOẢN VÀ CHÍNH SÁCH -->
                <div class="terms">
                    Khi tạo tài khoản, bạn đồng ý với
                    <span>điều khoản sử dụng</span>
                    của CineFlow.
                </div>
            </form>

            <!-- LIÊN KẾT ĐĂNG NHẬP -->
            <div class="login-link">
                Đã có tài khoản?
                <a href="${pageContext.request.contextPath}/login">
                    Đăng nhập
                </a>
            </div>
        </div>
    </div>

    <!-- XỬ LÝ JAVASCRIPT KIỂM TRA MẬT KHẨU PHÍA CLIENT -->
    <script>
        // Hiển thị hoặc ẩn mật khẩu
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

        // Kiểm tra hợp lệ mật khẩu và khớp mật khẩu khi người dùng nhập
        document.addEventListener("DOMContentLoaded", function () {
            const passwordInput = document.getElementById("password");
            const confirmPasswordInput = document.getElementById("confirmPassword");

            const lengthRequirement = document.getElementById("lengthRequirement");
            const uppercaseRequirement = document.getElementById("uppercaseRequirement");
            const lowercaseRequirement = document.getElementById("lowercaseRequirement");
            const numberRequirement = document.getElementById("numberRequirement");
            const specialRequirement = document.getElementById("specialRequirement");
            const confirmPasswordMessage = document.getElementById("confirmPasswordMessage");

            passwordInput.addEventListener("input", function () {
                const password = passwordInput.value;

                updateRequirement(lengthRequirement, password.length >= 9);
                updateRequirement(uppercaseRequirement, /[A-Z]/.test(password));
                updateRequirement(lowercaseRequirement, /[a-z]/.test(password));
                updateRequirement(numberRequirement, /[0-9]/.test(password));
                updateRequirement(specialRequirement, /[^A-Za-z0-9]/.test(password));

                checkConfirmPassword();
            });

            confirmPasswordInput.addEventListener("input", function () {
                checkConfirmPassword();
            });

            function checkConfirmPassword() {
                const password = passwordInput.value.trim();
                const confirmPassword = confirmPasswordInput.value.trim();

                // Chưa nhập xác nhận mật khẩu
                if (confirmPassword.length === 0) {
                    confirmPasswordMessage.style.display = "none";
                    confirmPasswordMessage.classList.remove("valid", "invalid");
                    confirmPasswordMessage.textContent = "";
                    return;
                }

                // Mật khẩu trùng khớp
                if (password === confirmPassword) {
                    confirmPasswordMessage.style.display = "none";
                    confirmPasswordMessage.classList.remove("valid", "invalid");
                    confirmPasswordMessage.textContent = "";
                } else {
                    // Mật khẩu không trùng khớp
                    confirmPasswordMessage.style.display = "block";
                    confirmPasswordMessage.classList.remove("valid");
                    confirmPasswordMessage.classList.add("invalid");
                    confirmPasswordMessage.textContent = "✕ Mật khẩu không khớp";
                }
            }

            function updateRequirement(element, valid) {
                const icon = element.querySelector(".requirement-icon");
                if (valid) {
                    element.classList.remove("invalid");
                    element.classList.add("valid");
                    icon.textContent = "✓";
                } else {
                    element.classList.remove("valid");
                    element.classList.add("invalid");
                    icon.textContent = "✕";
                }
            }
        });
    </script>
</body>
</html>
