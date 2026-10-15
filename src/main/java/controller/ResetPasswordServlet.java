package controller;

import dao.UserDAO;
import utils.PasswordUtils;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Xử lý đặt lại mật khẩu mới cho người dùng sau khi đã xác thực mã OTP thành công.
 */
public class ResetPasswordServlet extends HttpServlet {

    private UserDAO userDAO;

    @Override
    public void init() {
        userDAO = new UserDAO();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        Integer resetUserId = session == null ? null : (Integer) session.getAttribute("resetUserId");
        Boolean resetVerified = session == null ? null : (Boolean) session.getAttribute("resetVerified");

        // Bắt buộc phải có cờ xác thực OTP thành công trong session
        if (resetUserId == null || !Boolean.TRUE.equals(resetVerified)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "OTP verification is required.");
            return;
        }

        // 1. Lấy dữ liệu mật khẩu mới từ biểu mẫu
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");

        if (newPassword == null || newPassword.isEmpty()
                || confirmPassword == null || confirmPassword.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "All password fields are required.");
            return;
        }

        // 2. Kiểm tra mật khẩu xác nhận
        if (!newPassword.equals(confirmPassword)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "New password and Confirm Password do not match.");
            return;
        }

        // 3. Băm mật khẩu mới và lưu vào cơ sở dữ liệu, đồng thời hủy bỏ token đặt lại
        String hashedPassword = PasswordUtils.hashPassword(newPassword);
        boolean success = userDAO.resetPassword(resetUserId, hashedPassword);

        if (!success) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to reset password.");
            return;
        }

        // 4. Hủy phiên làm việc tạm và chuyển về trang đăng nhập
        session.invalidate();
        response.sendRedirect(request.getContextPath() + "/auth/login.jsp");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || !Boolean.TRUE.equals(session.getAttribute("resetVerified"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "OTP verification is required.");
            return;
        }

        request.getRequestDispatcher("/auth/reset-password.jsp").forward(request, response);
    }
}
