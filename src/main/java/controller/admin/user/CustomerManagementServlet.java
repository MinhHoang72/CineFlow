package controller.admin.user;
import jakarta.servlet.annotation.WebServlet;

import dao.UserDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.entity.User;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Điều khiển khu vực Quản lý Khách hàng & Thành viên (/admin/customers) dành cho Quản trị viên (Admin).
 * Nghiệp vụ:
 * - Tra cứu danh sách thành viên toàn hệ thống
 * - Tìm kiếm theo tên, username, email, số điện thoại
 * - Lọc theo trạng thái tài khoản (ACTIVE, LOCKED)
 * - Khóa hoặc mở khóa tài khoản khách hàng
 */
@WebServlet(name = "CustomerManagementServlet", urlPatterns = {"/admin/customers"})
public class CustomerManagementServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    /**
     * Kiểm tra phiên hiện tại có vai trò Quản trị viên (Admin) hay không.
     *
     * @param session phiên làm việc hiện tại
     * @return true nếu là Admin, ngược lại false
     */
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

    /**
     * Xử lý yêu cầu HTTP GET: Lấy danh sách khách hàng và thống kê số lượng.
     *
     * @param request  yêu cầu HTTP từ client
     * @param response phản hồi HTTP gửi về client
     * @throws ServletException nếu xảy ra lỗi servlet
     * @throws IOException      nếu xảy ra lỗi vào/ra dữ liệu
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 1. Kiểm tra đăng nhập (nếu chưa đăng nhập thì redirect về trang login kèm redirect URL)
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            String loginUrl = request.getContextPath() + "/login?redirect="
                    + URLEncoder.encode("/admin/customers", StandardCharsets.UTF_8);
            response.sendRedirect(loginUrl);
            return;
        }

        // 2. Kiểm tra quyền Admin (chỉ Admin mới có quyền quản lý khách hàng)
        if (!isAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Quản trị viên (ADMIN) mới có quyền truy cập trang quản lý khách hàng.");
            return;
        }

        // 3. Đọc các tham số tìm kiếm và bộ lọc trạng thái từ request
        String search = request.getParameter("search");
        String status = request.getParameter("status");
        if (status == null || status.isBlank()) {
            status = "ALL";
        }

        // 4. Truy vấn danh sách khách hàng và thống kê số lượng theo trạng thái
        List<User> customers = userDAO.getCustomers(search, status);
        int totalCustomers = userDAO.countCustomersByStatus("ALL");
        int activeCustomers = userDAO.countCustomersByStatus("ACTIVE");
        int lockedCustomers = userDAO.countCustomersByStatus("LOCKED");

        // 5. Gán các thuộc tính vào request scope để JSP render giao diện
        request.setAttribute("customers", customers);
        request.setAttribute("totalCustomers", totalCustomers);
        request.setAttribute("activeCustomers", activeCustomers);
        request.setAttribute("lockedCustomers", lockedCustomers);
        request.setAttribute("currentSearch", search != null ? search.trim() : "");
        request.setAttribute("currentStatus", status);

        // 6. Chuyển tiếp tới giao diện quản lý khách hàng
        request.getRequestDispatcher("/WEB-INF/views/admin/user/customerList.jsp").forward(request, response);
    }

    /**
     * Xử lý yêu cầu HTTP POST: Khóa hoặc mở khóa tài khoản khách hàng.
     *
     * @param request  yêu cầu HTTP từ client
     * @param response phản hồi HTTP gửi về client
     * @throws ServletException nếu xảy ra lỗi servlet
     * @throws IOException      nếu xảy ra lỗi vào/ra dữ liệu
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 1. Thiết lập mã hóa và kiểm tra quyền Admin
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        if (!isAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // 2. Đọc hành động cần thực hiện
        String action = request.getParameter("action");
        if (!"toggleStatus".equalsIgnoreCase(action)) {
            response.sendRedirect(request.getContextPath() + "/admin/customers");
            return;
        }

        // 3. Đọc mã khách hàng và trạng thái mới
        String userIdParam = request.getParameter("userId");
        String newStatus = request.getParameter("newStatus");
        String search = request.getParameter("search");
        String currentStatus = request.getParameter("status");

        String baseUrl = request.getContextPath() + "/admin/customers";

        if (userIdParam == null || newStatus == null) {
            response.sendRedirect(baseUrl + "?error=missing_data");
            return;
        }

        // 4. Chuyển mã khách hàng từ chuỗi sang số
        int userId;
        try {
            userId = Integer.parseInt(userIdParam.trim());
        } catch (NumberFormatException e) {
            response.sendRedirect(baseUrl + "?error=invalidId");
            return;
        }

        // 5. Kiểm tra tài khoản mục tiêu phải tồn tại và đúng vai trò CUSTOMER
        User targetCustomer = userDAO.getUserById(userId);
        if (targetCustomer == null
                || !"CUSTOMER".equalsIgnoreCase(targetCustomer.getRole())) {
            response.sendRedirect(baseUrl + "?error=invalidUser");
            return;
        }

        // 6. Chỉ chấp nhận hai trạng thái của tài khoản khách hàng
        String normalizedStatus = newStatus.trim().toUpperCase();
        if (!"ACTIVE".equals(normalizedStatus)
                && !"LOCKED".equals(normalizedStatus)) {
            response.sendRedirect(baseUrl + "?error=invalidStatus");
            return;
        }

        // 7. Cập nhật trạng thái trong database
        boolean updated = userDAO.updateUserStatus(userId, normalizedStatus);

        // 8. Giữ lại bộ lọc hiện tại khi quay về danh sách
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

        redirectUrl.append(updated
                ? "success=statusUpdated"
                : "error=statusFailed");

        response.sendRedirect(redirectUrl.toString());
    }
}
