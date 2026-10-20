package controller;
import jakarta.servlet.annotation.WebServlet;

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
 * Điều khiển khu vực Quản lý Khách hàng & Thành viên (/admin/customers) dành cho Quản trị viên (Admin).
 * Nghiệp vụ:
 * - Tra cứu danh sách thành viên toàn hệ thống
 * - Tìm kiếm theo tên, username, email, số điện thoại
 * - Lọc theo trạng thái tài khoản (ACTIVE, LOCKED / SUSPENDED...)
 * - Khóa hoặc mở khóa tài khoản khách hàng
 * - Tra cứu chi tiết lịch sử đặt vé của khách hàng
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
     * Xử lý yêu cầu HTTP GET: Lấy danh sách khách hàng, thống kê số lượng và xem lịch sử đặt vé nếu có.
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
            String loginUrl = request.getContextPath() + "/auth/login.jsp?redirect="
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

        // 4. Kiểm tra xem có yêu cầu xem chi tiết khách hàng và lịch sử đặt vé hay không (viewUserId)
        String viewIdParam = request.getParameter("viewUserId");
        if (viewIdParam != null && !viewIdParam.isBlank()) {
            try {
                int viewUserId = Integer.parseInt(viewIdParam.trim());
                User customerDetail = userDAO.getUserById(viewUserId);
                // Đảm bảo chỉ xem chi tiết nếu tài khoản đó đúng là vai trò CUSTOMER
                if (customerDetail != null && "CUSTOMER".equalsIgnoreCase(customerDetail.getRole())) {
                    List<BookingHistory> history = userDAO.getBookingHistory(viewUserId);
                    request.setAttribute("customerDetail", customerDetail);
                    request.setAttribute("bookingHistory", history);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        // 5. Truy vấn danh sách khách hàng và thống kê số lượng theo trạng thái
        List<User> customers = userDAO.getCustomers(search, status);
        int totalCustomers = userDAO.countCustomersByStatus("ALL");
        int activeCustomers = userDAO.countCustomersByStatus("ACTIVE");
        int suspendedCustomers = userDAO.countCustomersByStatus("LOCKED");

        // 6. Gán các thuộc tính vào request scope để JSP render giao diện
        request.setAttribute("customers", customers);
        request.setAttribute("totalCustomers", totalCustomers);
        request.setAttribute("activeCustomers", activeCustomers);
        request.setAttribute("suspendedCustomers", suspendedCustomers);
        request.setAttribute("lockedCustomers", suspendedCustomers);
        request.setAttribute("currentSearch", search != null ? search.trim() : "");
        request.setAttribute("currentStatus", status);

        // 7. Chuyển tiếp tới giao diện quản lý khách hàng
        request.getRequestDispatcher("/admin/customers.jsp").forward(request, response);
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
        // 1. Thiết lập mã hóa tiếng Việt UTF-8 và kiểm tra quyền Admin
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        if (!isAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // 2. Đọc các tham số hành động, từ khóa tìm kiếm và bộ lọc hiện tại
        String action = request.getParameter("action");
        String search = request.getParameter("search");
        String status = request.getParameter("status");

        // Xây dựng URL chuyển hướng giữ nguyên từ khóa tìm kiếm và bộ lọc trạng thái
        StringBuilder redirectUrl = new StringBuilder(request.getContextPath() + "/admin/customers?");
        if (search != null && !search.isBlank()) {
            redirectUrl.append("search=").append(URLEncoder.encode(search.trim(), StandardCharsets.UTF_8)).append("&");
        }
        if (status != null && !status.isBlank()) {
            redirectUrl.append("status=").append(URLEncoder.encode(status.trim(), StandardCharsets.UTF_8)).append("&");
        }

        // 3. Xử lý hành động KHÓA hoặc MỞ KHÓA tài khoản khách hàng (toggleStatus)
        if ("toggleStatus".equalsIgnoreCase(action)) {
            String userIdParam = request.getParameter("userId");
            String newStatus = request.getParameter("newStatus");

            if (userIdParam != null && newStatus != null) {
                try {
                    int userId = Integer.parseInt(userIdParam.trim());
                    User targetUser = userDAO.getUserById(userId);

                    // 4. Kiểm tra an toàn: Chỉ cho phép thao tác trên tài khoản khách hàng (CUSTOMER)
                    if (targetUser != null && "CUSTOMER".equalsIgnoreCase(targetUser.getRole())) {
                        String normalizedStatus = newStatus.trim().toUpperCase();
                        if ("SUSPENDED".equals(normalizedStatus)) {
                            normalizedStatus = "LOCKED";
                        }
                        // 5. Cập nhật trạng thái người dùng trong DB
                        boolean ok = userDAO.updateUserStatus(userId, normalizedStatus);
                        redirectUrl.append(ok ? "success=statusUpdated" : "error=statusFailed");
                    } else {
                        redirectUrl.append("error=invalidUser");
                    }
                } catch (NumberFormatException e) {
                    redirectUrl.append("error=invalidId");
                }
            }
        }

        // 6. Chuyển hướng người dùng về lại trang danh sách khách hàng
        response.sendRedirect(redirectUrl.toString());
    }
}
