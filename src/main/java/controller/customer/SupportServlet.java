package controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.entity.User;
import utils.WebUtil;

import java.io.IOException;

/**
 * Controller: Trang Trung tâm hỗ trợ & Liên hệ khách hàng CineFlow (/support, /contact).
 * Cung cấp thông tin đường dây nóng, email CSKH và câu hỏi thường gặp.
 * Điều hướng tới View chuẩn hóa /WEB-INF/views/customer/supportIndex.jsp.
 */
@WebServlet(name = "SupportServlet", urlPatterns = {"/support", "/support/index.jsp", "/contact"})
public class SupportServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Silent Redirect: Tài khoản nội bộ (ADMIN, MANAGER, STAFF) không vào trang hỗ trợ khách hàng
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            String role = (String) session.getAttribute("role");
            if (role == null && session.getAttribute("user") instanceof User) {
                role = ((User) session.getAttribute("user")).getRole();
            }
            if (role != null && !"CUSTOMER".equalsIgnoreCase(role)) {
                response.sendRedirect(request.getContextPath() + WebUtil.getRoleDefaultPath(session));
                return;
            }
        }
        request.getRequestDispatcher("/WEB-INF/views/customer/supportIndex.jsp").forward(request, response);
    }
}
