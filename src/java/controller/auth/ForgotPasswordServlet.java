package controller.auth;

import dao.UserDAO;
import model.User;
import utils.EmailUtils;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.concurrent.ThreadLocalRandom;

public class ForgotPasswordServlet extends HttpServlet {

    private UserDAO userDAO;

    @Override
    public void init() {
        userDAO = new UserDAO();
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        // ========================================
        // 1. Lấy email từ form
        // ========================================

        String email = request.getParameter("email");

        if (email == null || email.trim().isEmpty()) {

            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Email is required."
            );

            return;
        }

        email = email.trim();

        // ========================================
        // 2. Kiểm tra email có tồn tại không
        // ========================================

        User user = userDAO.getUserByEmail(email);

        if (user == null) {
            String encodedEmail = java.net.URLEncoder.encode(
                    email,
                    java.nio.charset.StandardCharsets.UTF_8
            );
            response.sendRedirect(
                    request.getContextPath()
                    + "/auth/forgot-password.jsp?error=email&email="
                    + encodedEmail
            );
            return;
        }

        // ========================================
        // 3. Tạo OTP 6 số
        // ========================================

        String otp = String.format(
                "%06d",
                ThreadLocalRandom.current()
                        .nextInt(0, 1_000_000)
        );

        // ========================================
        // 4. OTP hết hạn sau 15 phút
        // ========================================

        long expiryTime =
                System.currentTimeMillis()
                + (15 * 60 * 1000);

        Timestamp expiry =
                new Timestamp(expiryTime);

        // ========================================
        // 5. Lưu OTP + thời gian hết hạn vào DB
        // ========================================

        boolean saved =
                userDAO.saveResetToken(
                        user.getUserId(),
                        otp,
                        expiry
                );

        if (!saved) {

            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Failed to save OTP."
            );

            return;
        }

        // ========================================
        // 6. Gửi OTP qua email
        // ========================================

        try {

            EmailUtils.sendResetPasswordOtpEmail(
                    email,
                    otp
            );

        } catch (Exception e) {

            e.printStackTrace();

            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Failed to send OTP email."
            );

            return;
        }

        // ========================================
        // 7. Gửi thành công
        // Chuyển sang trang nhập OTP
        // ========================================

        String encodedEmail =
                java.net.URLEncoder.encode(
                        email,
                        java.nio.charset.StandardCharsets.UTF_8
                );

        response.sendRedirect(
                request.getContextPath()
                + "/auth/verify-otp.jsp?email="
                + encodedEmail
        );
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.sendError(
                HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "GET method is not allowed."
        );
    }
}


