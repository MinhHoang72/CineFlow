package controller;
import jakarta.servlet.annotation.WebServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Controller: Xử lý đăng xuất tài khoản người dùng, hủy phiên làm việc hiện tại và chuyển hướng về trang chủ.
 * URL ánh xạ: /logout
 */
@WebServlet(name = "LogoutServlet", urlPatterns = {"/logout", "/auth/logout"})
public class LogoutServlet extends HttpServlet {

    /**
     * Xử lý yêu cầu HTTP POST: Hủy phiên làm việc (session) và chuyển hướng người dùng về trang chủ.
     *
     * @param request  yêu cầu HTTP từ client
     * @param response phản hồi HTTP gửi về client
     * @throws ServletException nếu xảy ra lỗi servlet
     * @throws IOException      nếu xảy ra lỗi vào/ra dữ liệu
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 1. Lấy phiên làm việc hiện tại nếu có (không tạo mới nếu chưa tồn tại)
        HttpSession session = request.getSession(false);

        // 2. Hủy bỏ phiên làm việc và toàn bộ thông tin đăng nhập đã lưu trong session
        if (session != null) {
            session.invalidate();
        }

        // 3. Chuyển hướng người dùng về lại trang chủ của hệ thống
        response.sendRedirect(request.getContextPath() + "/home/index.jsp");
    }

    /**
     * Xử lý yêu cầu HTTP GET: Chuyển tiếp sang doPost để hỗ trợ đăng xuất qua liên kết GET.
     *
     * @param request  yêu cầu HTTP từ client
     * @param response phản hồi HTTP gửi về client
     * @throws ServletException nếu xảy ra lỗi servlet
     * @throws IOException      nếu xảy ra lỗi vào/ra dữ liệu
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Gọi lại phương thức doPost để xử lý đăng xuất thống nhất
        doPost(request, response);
    }
}
