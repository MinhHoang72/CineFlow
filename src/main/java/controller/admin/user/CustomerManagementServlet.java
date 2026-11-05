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
 * Controller: Quản lý Tài khoản Khách hàng & Thành viên (/admin/customers).
 * 
 * <p>Chức năng chính:</p>
 * <ul>
 *   <li>Xem danh sách tài khoản khách hàng, thông tin liên hệ, chi tiêu, trạng thái.</li>
 *   <li>Thống kê số lượng khách hàng theo chỉ số KPI (Tổng số, Đang hoạt động, Bị tạm khóa).</li>
 *   <li>Tìm kiếm và lọc theo trạng thái tài khoản (ALL, ACTIVE, LOCKED).</li>
 *   <li>Khóa hoặc mở khóa (toggle status) tài khoản khách hàng.</li>
 * </ul>
 * 
 * <p>Kiến trúc: Tuân thủ mô hình 3 lớp (3-Tier Architecture):</p>
 * <pre>
 *   CustomerManagementServlet (Controller) -&gt; UserService (Business Logic) -&gt; UserDAO (Data Access)
 * </pre>
 * 
 * @author CineFlow Team
 */
@WebServlet(name = "CustomerManagementServlet", urlPatterns = {"/admin/customers"})
public class CustomerManagementServlet extends HttpServlet {

    /** Tầng nghiệp vụ xử lý logic người dùng */
    private final UserService userService = new UserService();

    /**
     * Kiểm tra phiên đăng nhập hiện tại có quyền Quản trị viên (ADMIN) hay không.
     *
     * @param session Phiên làm việc HTTP hiện tại của người dùng
     * @return true nếu người dùng đã đăng nhập và mang vai trò ADMIN; ngược lại false
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
     * Xử lý yêu cầu HTTP GET: Hiển thị giao diện và danh sách khách hàng.
     * 
     * Các bước thực hiện:
     * 1. Xác thực đăng nhập: nếu chưa đăng nhập, chuyển hướng về trang /login.
     * 2. Phân quyền: chỉ cho phép tài khoản có vai trò ADMIN truy cập.
     * 3. Đọc các tham số lọc: từ khóa tìm kiếm (search) và trạng thái (status).
     * 4. Gọi UserService lấy dữ liệu khách hàng và chỉ số KPI.
     * 5. Gán thuộc tính vào request và forward tới view JSP (customerList.jsp).
     *
     * @param request  Yêu cầu HTTP gửi từ client
     * @param response Phản hồi HTTP trả về cho client
     * @throws ServletException Nếu có lỗi xảy ra trong quá trình servlet xử lý
     * @throws IOException      Nếu có lỗi I/O khi chuyển tiếp hoặc chuyển hướng
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 1. Kiểm tra xác thực đăng nhập
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            String loginUrl = request.getContextPath() + "/login?redirect="
                    + URLEncoder.encode("/admin/customers", StandardCharsets.UTF_8);
            response.sendRedirect(loginUrl);
            return;
        }

        // 2. Kiểm tra quyền Quản trị viên (ADMIN)
        if (!isAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, 
                    "Chỉ Quản trị viên (ADMIN) mới có quyền truy cập trang quản lý khách hàng.");
            return;
        }

        // 3. Đọc và chuẩn hóa tham số lọc (search, status)
        String search = request.getParameter("search");
        String status = request.getParameter("status");
        if (status == null || status.isBlank()) {
            status = "ALL";
        }

        // 4. Lấy dữ liệu từ tầng Service
        // Nạp toàn bộ khách hàng để hỗ trợ tìm kiếm và phân trang mượt mà phía client
        List<User> customers = userService.getCustomers(search, "ALL");
        UserService.CustomerKpi kpi = userService.getCustomerKpi();

        // 5. Gán dữ liệu vào Request Scope để JSP hiển thị
        request.setAttribute("customers", customers);
        request.setAttribute("totalCustomers", kpi.total());
        request.setAttribute("activeCustomers", kpi.active());
        request.setAttribute("lockedCustomers", kpi.locked());
        request.setAttribute("currentSearch", search != null ? search.trim() : "");
        request.setAttribute("currentStatus", status);

        // Chuyển tiếp tới giao diện quản lý khách hàng
        request.getRequestDispatcher("/WEB-INF/views/admin/user/customerList.jsp").forward(request, response);
    }

    /**
     * Xử lý yêu cầu HTTP POST: Thực hiện các thao tác thay đổi dữ liệu người dùng.
     * 
     * Các bước thực hiện:
     * 1. Cấu hình encoding UTF-8 và xác thực quyền ADMIN.
     * 2. Xác định hành động (action) cần thực hiện (ví dụ: toggleStatus - Khóa/Mở khóa tài khoản).
     * 3. Đọc và kiểm tra tính hợp lệ của tham số (userId, newStatus).
     * 4. Gọi UserService.updateCustomerStatus(...) để cập nhật trạng thái trong CSDL.
     * 5. Chuyển hướng (Redirect) về lại trang danh sách, kèm thông báo thành công hoặc mã lỗi.
     *
     * @param request  Yêu cầu HTTP gửi từ client
     * @param response Phản hồi HTTP trả về cho client
     * @throws ServletException Nếu có lỗi xảy ra trong quá trình servlet xử lý
     * @throws IOException      Nếu có lỗi I/O khi chuyển tiếp hoặc chuyển hướng
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        // 1. Kiểm tra quyền Quản trị viên (ADMIN)
        HttpSession session = request.getSession(false);
        if (!isAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // 2. Kiểm tra action yêu cầu
        String action = request.getParameter("action");
        if (!"toggleStatus".equalsIgnoreCase(action)) {
            response.sendRedirect(request.getContextPath() + "/admin/customers");
            return;
        }

        // 3. Đọc các tham số đầu vào
        String userIdParam = request.getParameter("userId");
        String newStatus = request.getParameter("newStatus");
        String search = request.getParameter("search");
        String currentStatus = request.getParameter("status");

        String baseUrl = request.getContextPath() + "/admin/customers";

        // Kiểm tra dữ liệu bắt buộc
        if (userIdParam == null || newStatus == null) {
            response.sendRedirect(baseUrl + "?error=missing_data");
            return;
        }

        // Kiểm tra định dạng ID người dùng
        int userId;
        try {
            userId = Integer.parseInt(userIdParam.trim());
        } catch (NumberFormatException e) {
            response.sendRedirect(baseUrl + "?error=invalidId");
            return;
        }

        // 4. Ủy quyền cho UserService thực hiện cập nhật trạng thái
        UserService.CustomerStatusResult result = userService.updateCustomerStatus(userId, newStatus);

        // 5. Xây dựng URL chuyển hướng giữ lại từ khóa tìm kiếm và bộ lọc hiện tại
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

        // Đính kèm thông báo kết quả thao tác
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
