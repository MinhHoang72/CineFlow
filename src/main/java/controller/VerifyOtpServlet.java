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

    @Override
    public void init() {
        userDAO = new UserDAO();
    }

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
        if ("register".equalsIgnoreCase(action) || "INACTIVE".equalsIgnoreCase(user.getStatus())) {
            userDAO.activateUser(user.getUserId());
            response.sendRedirect(request.getContextPath() + "/auth/login.jsp?verified=true");
            return;
        }

        // 2. Luồng đặt lại mật khẩu (Forgot Password)
        request.getSession(true).setAttribute("resetUserId", user.getUserId());
        request.getSession().setAttribute("resetVerified", true);
        response.sendRedirect(request.getContextPath() + "/reset-password");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        String email = request.getParameter("email");
        String type = request.getParameter("type");

        // Xử lý gửi lại mã OTP khi người dùng bấm "Gửi lại mã"
        if ("resend".equalsIgnoreCase(action) && email != null && !email.trim().isEmpty()) {
            email = email.trim();
            User user = userDAO.getUserByEmail(email);
            if (user != null) {
                String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
                Timestamp expiry = new Timestamp(System.currentTimeMillis() + (15 * 60 * 1000));
                userDAO.updateActivationOtp(user.getUserId(), otp, expiry);
                try {
                    if ("register".equalsIgnoreCase(type) || "INACTIVE".equalsIgnoreCase(user.getStatus())) {
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
