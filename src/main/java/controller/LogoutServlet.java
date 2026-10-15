package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Xử lý đăng xuất tài khoản người dùng, hủy phiên làm việc hiện tại và chuyển hướng về trang chủ.
 */
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Lấy phiên làm việc hiện tại nếu có
        HttpSession session = request.getSession(false);

        // Hủy bỏ phiên làm việc
        if (session != null) {
            session.invalidate();
        }

        response.sendRedirect(request.getContextPath() + "/home/index.jsp");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }
}
