package controller;

import dao.UserDAO;
import model.User;
import utils.EmailUtils;
import utils.PasswordUtils;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Xử lý đăng ký tài khoản khách hàng mới, kiểm tra tính hợp lệ của mật khẩu và gửi mã OTP kích hoạt qua email.
 */
public class RegisterServlet extends HttpServlet {

    private UserDAO userDAO;

    @Override
    public void init() {
        userDAO = new UserDAO();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setCharacterEncoding("UTF-8");

            // 1. Lấy dữ liệu từ biểu mẫu đăng ký
            String fullName = request.getParameter("fullName");
            String account = request.getParameter("account");
            String email = request.getParameter("email");
            String password = request.getParameter("password");
            String confirmPassword = request.getParameter("confirmPassword");

            // 2. Kiểm tra các trường dữ liệu bắt buộc
            if (fullName == null || fullName.trim().isEmpty()
                    || account == null || account.trim().isEmpty()
                    || email == null || email.trim().isEmpty()
                    || password == null || password.isEmpty()
                    || confirmPassword == null || confirmPassword.isEmpty()) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "All fields are required.");
                return;
            }

            fullName = fullName.trim();
            account = account.trim();
            email = email.trim();
            password = password.trim();
            confirmPassword = confirmPassword.trim();

            // 3. Kiểm tra mật khẩu xác nhận trùng khớp
            if (!password.equals(confirmPassword)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Password and Confirm Password do not match.");
                return;
            }

            // 4. Kiểm tra độ mạnh của mật khẩu (tối thiểu 9 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt)
            String passwordRegex = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{9,}$";
            if (!password.matches(passwordRegex)) {
                response.sendError(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Password must be at least 9 characters and contain uppercase, lowercase, number and special character."
                );
                return;
            }

            // 5. Kiểm tra tên tài khoản (username) đã tồn tại hay chưa
            boolean accountExists = userDAO.isAccountExists(account);
            if (accountExists) {
                String encodedAccount = URLEncoder.encode(account, StandardCharsets.UTF_8);
                String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
                response.sendRedirect(request.getContextPath()
                        + "/auth/register.jsp?error=account&account=" + encodedAccount
                        + "&email=" + encodedEmail);
                return;
            }

            // 5.1 Kiểm tra và chặn các dịch vụ email rác, email tạm thời
            String lowerEmail = email.toLowerCase();
            if (lowerEmail.endsWith("@tempmail.com") || lowerEmail.endsWith("@10minutemail.com")
                    || lowerEmail.endsWith("@guerrillamail.com") || lowerEmail.endsWith("@mailinator.com")
                    || lowerEmail.endsWith("@throwawaymail.com")) {
                String encodedAccount = URLEncoder.encode(account, StandardCharsets.UTF_8);
                response.sendRedirect(request.getContextPath()
                        + "/auth/register.jsp?error=tempmail&account=" + encodedAccount);
                return;
            }

            // 6. Kiểm tra địa chỉ email đã được sử dụng hay chưa
            User existingUser = userDAO.getUserByEmail(email);
            if (existingUser != null) {
                // Nếu tài khoản đã đăng ký nhưng chưa kích hoạt email thì tạo và gửi lại OTP kích hoạt
                if ("INACTIVE".equalsIgnoreCase(existingUser.getStatus()) || !existingUser.isEmailVerified()) {
                    String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
                    Timestamp expiry = new Timestamp(System.currentTimeMillis() + (15 * 60 * 1000));
                    userDAO.updateActivationOtp(existingUser.getUserId(), otp, expiry);
                    try {
                        EmailUtils.sendAccountActivationOtpEmail(email, otp, existingUser.getFullName());
                    } catch (Exception ex) {
                        System.out.println("Warning: Could not resend activation email: " + ex.getMessage());
                    }
                    String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
                    response.sendRedirect(request.getContextPath()
                            + "/auth/verify-otp.jsp?email=" + encodedEmail + "&action=register&resent=true");
                    return;
                }

                String encodedAccount = URLEncoder.encode(account, StandardCharsets.UTF_8);
                String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
                response.sendRedirect(request.getContextPath()
                        + "/auth/register.jsp?error=email&account=" + encodedAccount
                        + "&email=" + encodedEmail);
                return;
            }

            // 7. Băm mật khẩu bằng BCrypt
            String hashedPassword = PasswordUtils.hashPassword(password);

            // 8. Tạo đối tượng người dùng và tạo mã OTP kích hoạt 6 chữ số
            User user = new User();
            user.setFullName(fullName);
            user.setAccount(account);
            user.setEmail(email);
            user.setPassword(hashedPassword);

            String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
            Timestamp expiry = new Timestamp(System.currentTimeMillis() + (15 * 60 * 1000));

            // 9. Lưu tài khoản ở trạng thái INACTIVE chờ kích hoạt qua email
            boolean success = userDAO.register(user, otp, expiry);
            if (!success) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed.");
                return;
            }

            // 10. Gửi email chứa mã OTP kích hoạt tài khoản
            try {
                EmailUtils.sendAccountActivationOtpEmail(email, otp, fullName);
            } catch (Exception ex) {
                System.out.println("Warning: Failed to send activation email: " + ex.getMessage());
                ex.printStackTrace();
            }

            // 11. Chuyển hướng người dùng sang trang xác thực mã OTP
            String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
            response.sendRedirect(request.getContextPath()
                    + "/auth/verify-otp.jsp?email=" + encodedEmail + "&action=register");

        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "GET method is not allowed.");
    }
}
