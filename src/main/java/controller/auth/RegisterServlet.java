package controller.auth;
import jakarta.servlet.annotation.WebServlet;

import dao.UserDAO;
import model.entity.User;
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
 * Controller: Xử lý đăng ký tài khoản khách hàng mới (CUSTOMER),
 * kiểm tra ràng buộc nghiệp vụ, băm mật khẩu và gửi mã OTP kích hoạt qua Email.
 * URL ánh xạ: /register
 */
@WebServlet(name = "RegisterServlet", urlPatterns = {"/register", "/auth/register"})
public class RegisterServlet extends HttpServlet {

    private UserDAO userDAO;

    /**
     * Khởi tạo Servlet và đối tượng DAO.
     */
    @Override
    public void init() {
        userDAO = new UserDAO();
    }

    /**
     * Xử lý yêu cầu HTTP POST: Đăng ký tài khoản mới, băm mật khẩu và gửi OTP xác thực.
     *
     * @param request  yêu cầu HTTP từ client
     * @param response phản hồi HTTP gửi về client
     * @throws ServletException nếu xảy ra lỗi servlet
     * @throws IOException      nếu xảy ra lỗi vào/ra dữ liệu
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setCharacterEncoding("UTF-8");

            // 1. Nhận các trường dữ liệu từ biểu mẫu đăng ký
            String fullName = request.getParameter("fullName");
            String account = request.getParameter("account");
            String email = request.getParameter("email");
            String password = request.getParameter("password");
            String confirmPassword = request.getParameter("confirmPassword");

            // 2. Kiểm tra tính toàn vẹn: Không trường nào được để trống
            if (ValidationUtils.isBlank(fullName) || ValidationUtils.isBlank(account)
                    || ValidationUtils.isBlank(email) || ValidationUtils.isBlank(password)
                    || ValidationUtils.isBlank(confirmPassword)) {
                response.sendRedirect(request.getContextPath() + "/register?error=missing_fields");
                return;
            }

            fullName = fullName.trim();
            account = account.trim();
            email = email.trim();

            // 3. Kiểm tra định dạng Họ và tên (chỉ chứa chữ cái và khoảng trắng)
            if (!ValidationUtils.isValidFullName(fullName)) {
                response.sendRedirect(request.getContextPath() + "/register?error=invalid_name&account="
                        + URLEncoder.encode(account, StandardCharsets.UTF_8)
                        + "&email=" + URLEncoder.encode(email, StandardCharsets.UTF_8));
                return;
            }

            // 4. Kiểm tra định dạng Tài khoản (3-50 ký tự, không chứa ký tự đặc biệt nguy hiểm)
            if (!ValidationUtils.isValidAccount(account)) {
                response.sendRedirect(request.getContextPath() + "/register?error=invalid_account&email="
                        + URLEncoder.encode(email, StandardCharsets.UTF_8));
                return;
            }

            // 5. Kiểm tra định dạng Email hợp lệ theo chuẩn RFC 5322
            if (!ValidationUtils.isValidEmail(email)) {
                response.sendRedirect(request.getContextPath() + "/register?error=invalid_email&account="
                        + URLEncoder.encode(account, StandardCharsets.UTF_8));
                return;
            }

            // 6. Chặn các hòm thư rác tạm thời dùng 1 lần (10minutemail, mailinator...)
            if (ValidationUtils.isDisposableEmail(email)) {
                response.sendRedirect(request.getContextPath() + "/register?error=tempmail&account="
                        + URLEncoder.encode(account, StandardCharsets.UTF_8));
                return;
            }

            // 7. Kiểm tra xác nhận mật khẩu có khớp hoàn toàn với mật khẩu đã nhập không
            if (!password.equals(confirmPassword)) {
                response.sendRedirect(request.getContextPath() + "/register?error=password_mismatch&account="
                        + URLEncoder.encode(account, StandardCharsets.UTF_8)
                        + "&email=" + URLEncoder.encode(email, StandardCharsets.UTF_8));
                return;
            }

            // 8. Kiểm tra độ mạnh của mật khẩu (tối thiểu 8 ký tự, gồm chữ hoa, chữ thường, số, ký tự đặc biệt)
            if (!ValidationUtils.isValidPassword(password)) {
                response.sendRedirect(request.getContextPath() + "/register?error=weak_password&account="
                        + URLEncoder.encode(account, StandardCharsets.UTF_8)
                        + "&email=" + URLEncoder.encode(email, StandardCharsets.UTF_8));
                return;
            }

            // 9. Kiểm tra tính duy nhất của Tên tài khoản trong DB
            boolean accountExists = userDAO.isAccountExists(account);
            if (accountExists) {
                String encodedAccount = URLEncoder.encode(account, StandardCharsets.UTF_8);
                String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
                response.sendRedirect(request.getContextPath()
                        + "/register?error=account&account=" + encodedAccount
                        + "&email=" + encodedEmail);
                return;
            }

            // 10. Kiểm tra Email trong DB:
            User existingUser = userDAO.getUserByEmail(email);
            if (existingUser != null) {
                // 10.1. Nếu tài khoản đã đăng ký trước đó nhưng chưa xác thực email, hỗ trợ tạo lại OTP mới để kích hoạt
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
                            + "/verify-otp?email=" + encodedEmail + "&action=register&resent=true");
                    return;
                }

                // 10.2. Nếu email đã được kích hoạt và sử dụng -> Báo lỗi email đã tồn tại
                String encodedAccount = URLEncoder.encode(account, StandardCharsets.UTF_8);
                String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
                response.sendRedirect(request.getContextPath()
                        + "/register?error=email&account=" + encodedAccount
                        + "&email=" + encodedEmail);
                return;
            }

            // 11. Băm mật khẩu bằng thuật toán BCrypt
            String hashedPassword = PasswordUtils.hashPassword(password);

            // 12. Khởi tạo đối tượng User và tạo mã OTP kích hoạt 6 chữ số (hạn dùng 15 phút)
            User user = new User();
            user.setFullName(fullName);
            user.setAccount(account);
            user.setEmail(email);
            user.setPassword(hashedPassword);

            String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
            Timestamp expiry = new Timestamp(System.currentTimeMillis() + (15 * 60 * 1000));

            // 13. Lưu tài khoản mới vào cơ sở dữ liệu với trạng thái INACTIVE (chờ xác thực OTP)
            boolean success = userDAO.register(user, otp, expiry);
            if (!success) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed.");
                return;
            }

            // 14. Gửi email kích hoạt tài khoản kèm mã OTP qua SMTP
            try {
                EmailUtils.sendAccountActivationOtpEmail(email, otp, fullName);
            } catch (Exception ex) {
                System.out.println("Warning: Failed to send activation email: " + ex.getMessage());
                ex.printStackTrace();
            }

            // 15. Chuyển hướng người dùng sang trang nhập mã OTP để hoàn tất kích hoạt
            String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
            response.sendRedirect(request.getContextPath()
                    + "/verify-otp?email=" + encodedEmail + "&action=register");

        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
        }
    }

    /**
     * Xử lý yêu cầu HTTP GET: Chặn truy cập trực tiếp bằng phương thức GET vào endpoint đăng ký.
     *
     * @param request  yêu cầu HTTP từ client
     * @param response phản hồi HTTP gửi về client
     * @throws ServletException nếu xảy ra lỗi servlet
     * @throws IOException      nếu xảy ra lỗi vào/ra dữ liệu
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/auth/authRegister.jsp").forward(request, response);
    }
}
