package controller;

import dao.UserDAO;
import model.User;
import utils.PasswordUtils;
import utils.ValidationUtils;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Xử lý yêu cầu thay đổi mật khẩu từ người dùng đã đăng nhập.
 */
public class ChangePasswordServlet extends HttpServlet {

    private UserDAO userDAO;

    @Override
    public void init() {
        userDAO = new UserDAO();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        // 1. Kiểm tra phiên đăng nhập
        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "You are not logged in.");
            return;
        }

        Integer userIdObj = (Integer) session.getAttribute("userId");
        if (userIdObj == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "You are not logged in.");
            return;
        }

        int userId = userIdObj;

        // 2. Lấy dữ liệu mật khẩu từ biểu mẫu
        String currentPassword = request.getParameter("currentPassword");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");

        // 3. Kiểm tra các trường dữ liệu bắt buộc
        if (currentPassword == null || currentPassword.isEmpty()
                || newPassword == null || newPassword.isEmpty()
                || confirmPassword == null || confirmPassword.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "All password fields are required.");
            return;
        }

        // 4. Kiểm tra xác nhận mật khẩu mới trùng khớp
        if (!newPassword.equals(confirmPassword)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "New password and Confirm Password do not match.");
            return;
        }

        // 4.1 Kiểm tra độ mạnh của mật khẩu mới
        if (!ValidationUtils.isValidPassword(newPassword)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "New password must be at least 8 characters and contain uppercase, lowercase, number and special character.");
            return;
        }

        // 5. Kiểm tra người dùng tồn tại trong cơ sở dữ liệu
        User user = userDAO.getUserById(userId);
        if (user == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "User not found.");
            return;
        }

        // 6. Kiểm tra mật khẩu hiện tại
        String storedPassword = user.getPassword();
        if (storedPassword == null || !storedPassword.startsWith("$2")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Current password is not using BCrypt.");
            return;
        }

        boolean currentPasswordCorrect = PasswordUtils.checkPassword(currentPassword, storedPassword);
        if (!currentPasswordCorrect) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Current password is incorrect.");
            return;
        }

        // 7. Mật khẩu mới không được trùng với mật khẩu cũ
        if (PasswordUtils.checkPassword(newPassword, storedPassword)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "New password must be different from current password.");
            return;
        }

        // 8. Băm mật khẩu mới bằng BCrypt và lưu vào cơ sở dữ liệu
        String hashedPassword = PasswordUtils.hashPassword(newPassword);
        boolean success = userDAO.changePassword(userId, hashedPassword);

        if (!success) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to change password.");
            return;
        }

        // 9. Đổi mật khẩu thành công
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println("Password changed successfully.");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "GET method is not allowed.");
    }
}
