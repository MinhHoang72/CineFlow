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
 * Xử lý yêu cầu quên mật khẩu: tạo mã OTP xác thực và gửi qua email cho người dùng.
 */
public class ForgotPasswordServlet extends HttpServlet {

    private UserDAO userDAO;

    @Override
    public void init() {
        userDAO = new UserDAO();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        // 1. Lấy địa chỉ email từ biểu mẫu
        String email = request.getParameter("email");
        if (email == null || email.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email is required.");
            return;
        }

        email = email.trim();

        // 2. Kiểm tra email có tồn tại trong hệ thống hay không
        User user = userDAO.getUserByEmail(email);
        if (user == null) {
            String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
            response.sendRedirect(request.getContextPath() + "/auth/forgot-password.jsp?error=email&email=" + encodedEmail);
            return;
        }

        // 3. Tạo mã OTP gồm 6 chữ số ngẫu nhiên
        String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));

        // 4. Đặt thời gian hết hạn của OTP sau 15 phút
        long expiryTime = System.currentTimeMillis() + (15 * 60 * 1000);
        Timestamp expiry = new Timestamp(expiryTime);

        // 5. Lưu mã OTP và thời gian hết hạn vào cơ sở dữ liệu
        boolean saved = userDAO.saveResetToken(user.getUserId(), otp, expiry);
        if (!saved) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to save OTP.");
            return;
        }

        // 6. Gửi mã OTP xác thực qua email người dùng
        try {
            EmailUtils.sendResetPasswordOtpEmail(email, otp);
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to send OTP email.");
            return;
        }

        // 7. Chuyển hướng người dùng sang trang nhập mã OTP
        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
        response.sendRedirect(request.getContextPath() + "/auth/verify-otp.jsp?email=" + encodedEmail);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "GET method is not allowed.");
    }
}
