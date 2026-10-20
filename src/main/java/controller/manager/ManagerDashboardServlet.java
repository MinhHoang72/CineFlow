package controller.manager;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.entity.User;

import java.io.IOException;
import java.net.URLEncoder;

/**
 * Controller: Bảng điều khiển Quản lý cụm rạp (Cinema Manager Dashboard).
 * URL ánh xạ: /manager/dashboard, /manager
 */
@WebServlet(name = "ManagerDashboardServlet", urlPatterns = {"/manager/dashboard", "/manager"})
public class ManagerDashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            String loginUrl = request.getContextPath() + "/auth/login.jsp?redirect=" + URLEncoder.encode("/manager/dashboard", "UTF-8");
            response.sendRedirect(loginUrl);
            return;
        }

        String role = (String) session.getAttribute("role");
        if (role == null && session.getAttribute("user") instanceof User) {
            role = ((User) session.getAttribute("user")).getRole();
        }

        if (!"MANAGER".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Quản lý (MANAGER) mới có quyền truy cập trang này.");
            return;
        }

        request.getRequestDispatcher("/WEB-INF/views/manager/managerDashboard.jsp").forward(request, response);
    }
}
