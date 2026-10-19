package controller;

import dao.UserDAO;
import model.User;
import utils.EmailUtils;

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
 * Xử lý xác thực mã OTP cho luồng kích hoạt tài khoản đăng ký mới và luồng quên mật khẩu.
 */
public class VerifyOtpServlet extends HttpServlet {

    private UserDAO userDAO;

    /**
     * Khởi tạo Servlet và đối tượng DAO truy xuất cơ sở dữ liệu.
     */
    @Override
    public void init() {
        userDAO = new UserDAO();
    }

    /**
     * Xử lý yêu cầu HTTP POST: Xác thực mã OTP cho các luồng kích hoạt tài khoản, đổi mật khẩu hoặc đổi email.
     *
     * @param request  yêu cầu HTTP từ client
     * @param response phản hồi HTTP gửi về client
     * @throws ServletException nếu xảy ra lỗi servlet
     * @throws IOException      nếu xảy ra lỗi vào/ra dữ liệu
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        String email = request.getParameter("email");
        String otp = request.getParameter("otp");
        String action = request.getParameter("action");

        // Kiểm tra tính hợp lệ của tham số đầu vào (mã OTP phải gồm đúng 6 chữ số)
        if (email == null || email.trim().isEmpty() || otp == null || !otp.matches("\\d{6}")) {
            String actionParam = (action != null && !action.isEmpty()) ? "&action=" + action : "";
            response.sendRedirect(request.getContextPath() + "/auth/verify-otp.jsp?error=invalid&email="
                    + (email == null ? "" : URLEncoder.encode(email.trim(), StandardCharsets.UTF_8)) + actionParam);
            return;
        }

        email = email.trim();
        otp = otp.trim();

        // 1. Luồng xác thực thay đổi email từ trang hồ sơ người dùng
        if ("changeEmail".equalsIgnoreCase(action)) {
            jakarta.servlet.http.HttpSession session = request.getSession(false);
            Integer currentUserId = session != null ? (Integer) session.getAttribute("userId") : null;
            String pendingNewEmail = session != null ? (String) session.getAttribute("pendingNewEmail") : null;

            if (currentUserId == null || pendingNewEmail == null) {
                User otpUser = userDAO.getUserByResetToken(otp);
                if (otpUser != null) {
                    currentUserId = otpUser.getUserId();
                    pendingNewEmail = email;
                } else {
                    response.sendRedirect(request.getContextPath() + "/account?error=sessionExpired");
                    return;
                }
            }

            User user = userDAO.getUserById(currentUserId);
            if (user == null || user.getResetToken() == null || !otp.equals(user.getResetToken())
                    || (user.getResetTokenExpiry() != null && user.getResetTokenExpiry().before(new Timestamp(System.currentTimeMillis())))) {
                response.sendRedirect(request.getContextPath() + "/auth/verify-otp.jsp?error=invalid&email="
                        + URLEncoder.encode(pendingNewEmail, StandardCharsets.UTF_8) + "&action=changeEmail");
                return;
            }

            // Kiểm tra xem email mới có bị tài khoản khác đăng ký trong lúc chờ OTP hay không
            if (userDAO.isEmailUsedByOtherUser(pendingNewEmail, currentUserId)) {
                response.sendRedirect(request.getContextPath() + "/account?error=email");
                return;
            }

            // Cập nhật email mới chính thức và xác thực thành công
            boolean updated = userDAO.updateUserEmail(currentUserId, pendingNewEmail, true);
            if (updated) {
                if (session != null) {
                    session.removeAttribute("pendingNewEmail");
                    session.removeAttribute("pendingEmailUserId");
                    session.setAttribute("user", userDAO.getUserById(currentUserId));
                }
                response.sendRedirect(request.getContextPath() + "/account?updated=emailVerified");
            } else {
                response.sendRedirect(request.getContextPath() + "/account?error=updateFailed");
            }
            return;
        }

        User user = userDAO.getUserByEmail(email);
        User otpUser = user == null ? null : userDAO.getUserByResetToken(otp);

        // Kiểm tra OTP có khớp với tài khoản sở hữu email đó hay không
        if (otpUser == null || user == null || otpUser.getUserId() != user.getUserId()) {
            String actionParam = (action != null && !action.isEmpty()) ? "&action=" + action : "";
            response.sendRedirect(request.getContextPath() + "/auth/verify-otp.jsp?error=invalid&email="
                    + URLEncoder.encode(email, StandardCharsets.UTF_8) + actionParam);
            return;
        }

        // 1. Luồng kích hoạt tài khoản đăng ký mới
        if ("register".equalsIgnoreCase(action)) {
            userDAO.activateUser(user.getUserId());
            response.sendRedirect(request.getContextPath() + "/auth/login.jsp?verified=true");
            return;
        }

        // 2. Luồng đặt lại mật khẩu (Forgot Password)
        request.getSession(true).setAttribute("resetUserId", user.getUserId());
        request.getSession().setAttribute("resetVerified", true);
        response.sendRedirect(request.getContextPath() + "/reset-password");
    }

    /**
     * Xử lý yêu cầu HTTP GET: Gửi lại mã OTP (resend) khi người dùng yêu cầu hoặc chuyển tiếp tới trang verify-otp.jsp.
     *
     * @param request  yêu cầu HTTP từ client
     * @param response phản hồi HTTP gửi về client
     * @throws ServletException nếu xảy ra lỗi servlet
     * @throws IOException      nếu xảy ra lỗi vào/ra dữ liệu
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        String email = request.getParameter("email");
        String type = request.getParameter("type");

        // Xử lý gửi lại mã OTP khi người dùng bấm "Gửi lại mã"
        if ("resend".equalsIgnoreCase(action) && email != null && !email.trim().isEmpty()) {
            email = email.trim();

            if ("changeEmail".equalsIgnoreCase(type)) {
                jakarta.servlet.http.HttpSession session = request.getSession(false);
                Integer currentUserId = session != null ? (Integer) session.getAttribute("userId") : null;
                if (currentUserId != null) {
                    User user = userDAO.getUserById(currentUserId);
                    if (user != null) {
                        String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
                        Timestamp expiry = new Timestamp(System.currentTimeMillis() + (15 * 60 * 1000));
                        userDAO.updateActivationOtp(user.getUserId(), otp, expiry);
                        try {
                            EmailUtils.sendEmailChangeOtpEmail(email, otp, user.getFullName());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
                response.sendRedirect(request.getContextPath() + "/auth/verify-otp.jsp?email="
                        + URLEncoder.encode(email, StandardCharsets.UTF_8) + "&action=changeEmail&resent=true");
                return;
            }

            User user = userDAO.getUserByEmail(email);
            if (user != null) {
                String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
                Timestamp expiry = new Timestamp(System.currentTimeMillis() + (15 * 60 * 1000));
                userDAO.updateActivationOtp(user.getUserId(), otp, expiry);
                try {
                    if ("register".equalsIgnoreCase(type)) {
                        EmailUtils.sendAccountActivationOtpEmail(user.getEmail(), otp, user.getFullName());
                    } else {
                        EmailUtils.sendResetPasswordOtpEmail(user.getEmail(), otp);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            String typeParam = (type != null && !type.isEmpty()) ? "&action=" + type : "";
            response.sendRedirect(request.getContextPath() + "/auth/verify-otp.jsp?email="
                    + URLEncoder.encode(email, StandardCharsets.UTF_8) + typeParam + "&resent=true");
            return;
        }

        response.sendRedirect(request.getContextPath() + "/auth/verify-otp.jsp");
    }
}
