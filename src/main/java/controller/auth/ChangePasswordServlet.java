package controller.auth;
import jakarta.servlet.annotation.WebServlet;

import dao.UserDAO;
import model.entity.User;
import utils.PasswordUtil;
import utils.ValidationUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Controller: Xử lý yêu cầu thay đổi mật khẩu từ người dùng đã đăng nhập vào hệ thống.
 * URL ánh xạ: /change-password
 */
@WebServlet(name = "ChangePasswordServlet", urlPatterns = {"/change-password", "/auth/change-password"})
public class ChangePasswordServlet extends HttpServlet {

    private UserDAO userDAO;

    /**
     * Khởi tạo Servlet và đối tượng DAO truy xuất cơ sở dữ liệu.
     */
    @Override
    public void init() {
        userDAO = new UserDAO();
    }

    /**
     * Xử lý yêu cầu HTTP POST: Xác thực mật khẩu cũ, kiểm tra chuẩn mật khẩu mới và cập nhật vào DB.
     *
     * @param request  yêu cầu HTTP từ client
     * @param response phản hồi HTTP gửi về client
     * @throws ServletException nếu xảy ra lỗi servlet
     * @throws IOException      nếu xảy ra lỗi vào/ra dữ liệu
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 1. Thiết lập encoding UTF-8
        request.setCharacterEncoding("UTF-8");

        // 2. Kiểm tra phiên đăng nhập (chỉ người dùng đã đăng nhập mới được đổi mật khẩu)
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

        // 3. Đọc dữ liệu từ biểu mẫu đổi mật khẩu
        String currentPassword = request.getParameter("currentPassword");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");

        // 4. Kiểm tra các trường thông tin bắt buộc không được để trống
        if (currentPassword == null || currentPassword.isEmpty()
                || newPassword == null || newPassword.isEmpty()
                || confirmPassword == null || confirmPassword.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "All password fields are required.");
            return;
        }

        // 5. Kiểm tra mật khẩu xác nhận trùng khớp
        if (!newPassword.equals(confirmPassword)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "New password and Confirm Password do not match.");
            return;
        }

        // 6. Kiểm tra độ mạnh của mật khẩu mới theo chuẩn bảo mật
        if (!ValidationUtil.isValidPassword(newPassword)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "New password must be at least 8 characters and contain uppercase, lowercase, number and special character.");
            return;
        }

        // 7. Truy vấn thông tin người dùng từ cơ sở dữ liệu
        User user = userDAO.getUserById(userId);
        if (user == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "User not found.");
            return;
        }

        // 8. Xác thực mật khẩu hiện tại (khớp với mật khẩu đã băm trong DB)
        String storedPassword = user.getPassword();
        if (storedPassword == null || !storedPassword.startsWith("$2")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Current password is not using BCrypt.");
            return;
        }

        boolean currentPasswordCorrect = PasswordUtil.checkPassword(currentPassword, storedPassword);
        if (!currentPasswordCorrect) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Current password is incorrect.");
            return;
        }

        // 9. Kiểm tra tính hợp lệ: Mật khẩu mới không được trùng với mật khẩu hiện tại
        if (PasswordUtil.checkPassword(newPassword, storedPassword)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "New password must be different from current password.");
            return;
        }

        // 10. Băm mật khẩu mới bằng thuật toán BCrypt và lưu vào cơ sở dữ liệu
        String hashedPassword = PasswordUtil.hashPassword(newPassword);
        boolean success = userDAO.changePassword(userId, hashedPassword);

        if (!success) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to change password.");
            return;
        }

        // 11. Đổi mật khẩu thành công, gửi phản hồi 200 OK
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println("Password changed successfully.");
    }

    /**
     * Xử lý yêu cầu HTTP GET: Chặn truy cập trực tiếp bằng GET vào endpoint đổi mật khẩu.
     *
     * @param request  yêu cầu HTTP từ client
     * @param response phản hồi HTTP gửi về client
     * @throws ServletException nếu xảy ra lỗi servlet
     * @throws IOException      nếu xảy ra lỗi vào/ra dữ liệu
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        request.getRequestDispatcher("/WEB-INF/views/auth/changePassword.jsp").forward(request, response);
    }
}
