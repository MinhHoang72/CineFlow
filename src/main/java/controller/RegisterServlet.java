package controller;

import dao.UserDAO;
import model.User;
import utils.EmailUtils;
import utils.PasswordUtils;
import utils.ValidationUtils;

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
            if (ValidationUtils.isBlank(fullName) || ValidationUtils.isBlank(account)
                    || ValidationUtils.isBlank(email) || ValidationUtils.isBlank(password)
                    || ValidationUtils.isBlank(confirmPassword)) {
                response.sendRedirect(request.getContextPath() + "/auth/register.jsp?error=missing_fields");
                return;
            }

            fullName = fullName.trim();
            account = account.trim();
            email = email.trim();

            // 3. Kiểm tra tính hợp lệ của họ tên, tài khoản và email
            if (!ValidationUtils.isValidFullName(fullName)) {
                response.sendRedirect(request.getContextPath() + "/auth/register.jsp?error=invalid_name&account="
                        + URLEncoder.encode(account, StandardCharsets.UTF_8)
                        + "&email=" + URLEncoder.encode(email, StandardCharsets.UTF_8));
                return;
            }

            if (!ValidationUtils.isValidAccount(account)) {
                response.sendRedirect(request.getContextPath() + "/auth/register.jsp?error=invalid_account&email="
                        + URLEncoder.encode(email, StandardCharsets.UTF_8));
                return;
            }

            if (!ValidationUtils.isValidEmail(email)) {
                response.sendRedirect(request.getContextPath() + "/auth/register.jsp?error=invalid_email&account="
                        + URLEncoder.encode(account, StandardCharsets.UTF_8));
                return;
            }

            if (ValidationUtils.isDisposableEmail(email)) {
                response.sendRedirect(request.getContextPath() + "/auth/register.jsp?error=tempmail&account="
                        + URLEncoder.encode(account, StandardCharsets.UTF_8));
                return;
            }

            // 4. Kiểm tra mật khẩu xác nhận trùng khớp
            if (!password.equals(confirmPassword)) {
                response.sendRedirect(request.getContextPath() + "/auth/register.jsp?error=password_mismatch&account="
                        + URLEncoder.encode(account, StandardCharsets.UTF_8)
                        + "&email=" + URLEncoder.encode(email, StandardCharsets.UTF_8));
                return;
            }

            // 5. Kiểm tra độ mạnh của mật khẩu
            if (!ValidationUtils.isValidPassword(password)) {
                response.sendRedirect(request.getContextPath() + "/auth/register.jsp?error=weak_password&account="
                        + URLEncoder.encode(account, StandardCharsets.UTF_8)
                        + "&email=" + URLEncoder.encode(email, StandardCharsets.UTF_8));
                return;
            }

            // 6. Kiểm tra tên tài khoản (username) đã tồn tại hay chưa
            boolean accountExists = userDAO.isAccountExists(account);
            if (accountExists) {
                String encodedAccount = URLEncoder.encode(account, StandardCharsets.UTF_8);
                String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
                response.sendRedirect(request.getContextPath()
                        + "/auth/register.jsp?error=account&account=" + encodedAccount
                        + "&email=" + encodedEmail);
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
