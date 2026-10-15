package controller;

import dao.UserDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.BookingHistory;
import model.User;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controller Quản lý Khách hàng & Thành viên (/admin/customers) dành cho Quản trị viên (Admin).
 * Nghiệp vụ:
 * - Tra cứu danh sách thành viên toàn hệ thống
 * - Tìm kiếm theo tên, username, email, số điện thoại
 * - Lọc theo trạng thái tài khoản (ACTIVE, SUSPENDED)
 * - Khóa / Kích hoạt lại tài khoản khách hàng
 * - Tra cứu chi tiết lịch sử đặt vé của khách hàng
 */
public class CustomerManagementServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    private boolean isAdmin(HttpSession session) {
        if (session == null) return false;
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
            String loginUrl = request.getContextPath() + "/auth/login.jsp?redirect=" 
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

        // Kiểm tra xem có yêu cầu xem chi tiết khách hàng & lịch sử vé không
        String viewIdParam = request.getParameter("viewUserId");
        if (viewIdParam != null && !viewIdParam.isBlank()) {
            try {
                int viewUserId = Integer.parseInt(viewIdParam.trim());
                User customerDetail = userDAO.getUserById(viewUserId);
                if (customerDetail != null && "CUSTOMER".equalsIgnoreCase(customerDetail.getRole())) {
                    List<BookingHistory> history = userDAO.getBookingHistory(viewUserId);
                    request.setAttribute("customerDetail", customerDetail);
                    request.setAttribute("bookingHistory", history);
                }
            } catch (NumberFormatException ignored) {}
        }

        List<User> customers = userDAO.getCustomers(search, status);
        int totalCustomers = userDAO.countCustomersByStatus("ALL");
        int activeCustomers = userDAO.countCustomersByStatus("ACTIVE");
        int suspendedCustomers = userDAO.countCustomersByStatus("SUSPENDED");

        request.setAttribute("customers", customers);
        request.setAttribute("totalCustomers", totalCustomers);
        request.setAttribute("activeCustomers", activeCustomers);
        request.setAttribute("suspendedCustomers", suspendedCustomers);
        request.setAttribute("currentSearch", search != null ? search.trim() : "");
        request.setAttribute("currentStatus", status);

        request.getRequestDispatcher("/admin/customers.jsp").forward(request, response);
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
        String search = request.getParameter("search");
        String status = request.getParameter("status");

        StringBuilder redirectUrl = new StringBuilder(request.getContextPath() + "/admin/customers?");
        if (search != null && !search.isBlank()) {
            redirectUrl.append("search=").append(URLEncoder.encode(search.trim(), StandardCharsets.UTF_8)).append("&");
        }
        if (status != null && !status.isBlank()) {
            redirectUrl.append("status=").append(URLEncoder.encode(status.trim(), StandardCharsets.UTF_8)).append("&");
        }

        if ("toggleStatus".equalsIgnoreCase(action)) {
            String userIdParam = request.getParameter("userId");
            String newStatus = request.getParameter("newStatus");

            if (userIdParam != null && newStatus != null) {
                try {
                    int userId = Integer.parseInt(userIdParam.trim());
                    User targetUser = userDAO.getUserById(userId);
                    // Chỉ cho phép thao tác trên tài khoản khách hàng CUSTOMER
                    if (targetUser != null && "CUSTOMER".equalsIgnoreCase(targetUser.getRole())) {
                        boolean ok = userDAO.updateUserStatus(userId, newStatus.trim().toUpperCase());
                        redirectUrl.append(ok ? "success=statusUpdated" : "error=statusFailed");
                    } else {
                        redirectUrl.append("error=invalidUser");
                    }
                } catch (NumberFormatException e) {
                    redirectUrl.append("error=invalidId");
                }
            }
        }

        response.sendRedirect(redirectUrl.toString());
    }
}
