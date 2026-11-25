package controller.admin.user;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.entity.User;
import service.UserService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controller: Quản lý Tài khoản Khách hàng (/admin/customers).
 */
@WebServlet(name = "CustomerManagementServlet", urlPatterns = {"/admin/customers"})
public class CustomerManagementServlet extends HttpServlet {

    private final UserService userService = new UserService();

    private boolean isAdmin(HttpSession session) {
        if (session == null) return false;
        Object user = session.getAttribute("user");
        String role = (user instanceof User) ? ((User) user).getRole() : (String) session.getAttribute("role");
        return "ADMIN".equalsIgnoreCase(role);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (!isAdmin(session)) {
            response.sendRedirect(request.getContextPath() + "/login?redirect="
                    + URLEncoder.encode("/admin/customers", StandardCharsets.UTF_8));
            return;
        }

        // Lấy danh sách khách hàng và tính KPI trực tiếp trong bộ nhớ
        List<User> customers = userService.getCustomers();
        UserService.CustomerKpi kpi = userService.getCustomerKpi(customers);

        request.setAttribute("customers", customers);
        request.setAttribute("totalCustomers", kpi.total());
        request.setAttribute("activeCustomers", kpi.active());
        request.setAttribute("lockedCustomers", kpi.locked());

        request.getRequestDispatcher("/WEB-INF/views/admin/user/customerList.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (!isAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            int userId = Integer.parseInt(request.getParameter("userId"));
            String newStatus = request.getParameter("newStatus");

            boolean success = userService.updateCustomerStatus(userId, newStatus);
            response.sendRedirect(request.getContextPath() + "/admin/customers?" + (success ? "success=statusUpdated" : "error=statusFailed"));
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/admin/customers?error=statusFailed");
        }
    }
}
