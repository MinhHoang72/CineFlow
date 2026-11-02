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
 * Controller: Quản lý Khách hàng & Thành viên (/admin/customers).
 * Ủy quyền nghiệp vụ xử lý cho UserService (Mô hình 3 lớp).
 */
@WebServlet(name = "CustomerManagementServlet", urlPatterns = {"/admin/customers"})
public class CustomerManagementServlet extends HttpServlet {

    private final UserService userService = new UserService();

    private boolean isAdmin(HttpSession session) {
        if (session == null) {
            return false;
        }
        Object role = session.getAttribute("role");
        if (role == null && session.getAttribute("user") instanceof User) {
            role = ((User) session.getAttribute("user")).getRole();
        }
        return "ADMIN".equalsIgnoreCase((String) role);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            String loginUrl = request.getContextPath() + "/login?redirect="
                    + URLEncoder.encode("/admin/customers", StandardCharsets.UTF_8);
            response.sendRedirect(loginUrl);
            return;
        }

        if (!isAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Quản trị viên (ADMIN) mới có quyền truy cập trang quản lý khách hàng.");
            return;
        }

        String search = request.getParameter("search");
        String status = request.getParameter("status");
        if (status == null || status.isBlank()) {
            status = "ALL";
        }

        List<User> customers = userService.getCustomers(search, status);
        UserService.CustomerKpi kpi = userService.getCustomerKpi();

        request.setAttribute("customers", customers);
        request.setAttribute("totalCustomers", kpi.total());
        request.setAttribute("activeCustomers", kpi.active());
        request.setAttribute("lockedCustomers", kpi.locked());
        request.setAttribute("currentSearch", search != null ? search.trim() : "");
        request.setAttribute("currentStatus", status);

        request.getRequestDispatcher("/WEB-INF/views/admin/user/customerList.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        if (!isAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String action = request.getParameter("action");
        if (!"toggleStatus".equalsIgnoreCase(action)) {
            response.sendRedirect(request.getContextPath() + "/admin/customers");
            return;
        }

        String userIdParam = request.getParameter("userId");
        String newStatus = request.getParameter("newStatus");
        String search = request.getParameter("search");
        String currentStatus = request.getParameter("status");

        String baseUrl = request.getContextPath() + "/admin/customers";

        if (userIdParam == null || newStatus == null) {
            response.sendRedirect(baseUrl + "?error=missing_data");
            return;
        }

        int userId;
        try {
            userId = Integer.parseInt(userIdParam.trim());
        } catch (NumberFormatException e) {
            response.sendRedirect(baseUrl + "?error=invalidId");
            return;
        }

        UserService.CustomerStatusResult result = userService.updateCustomerStatus(userId, newStatus);

        StringBuilder redirectUrl = new StringBuilder(baseUrl).append("?");
        if (search != null && !search.isBlank()) {
            redirectUrl.append("search=")
                    .append(URLEncoder.encode(search.trim(), StandardCharsets.UTF_8))
                    .append("&");
        }
        if (currentStatus != null && !currentStatus.isBlank()) {
            redirectUrl.append("status=")
                    .append(URLEncoder.encode(currentStatus.trim(), StandardCharsets.UTF_8))
                    .append("&");
        }

        if (result == UserService.CustomerStatusResult.SUCCESS) {
            redirectUrl.append("success=statusUpdated");
        } else if (result == UserService.CustomerStatusResult.INVALID_USER) {
            redirectUrl.append("error=invalidUser");
        } else if (result == UserService.CustomerStatusResult.INVALID_STATUS) {
            redirectUrl.append("error=invalidStatus");
        } else {
            redirectUrl.append("error=statusFailed");
        }

        response.sendRedirect(redirectUrl.toString());
    }
}
