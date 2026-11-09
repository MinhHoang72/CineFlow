package controller.admin.user;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.entity.Centre;
import model.entity.User;
import service.UserService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controller: Quản lý Tài khoản Quản lý chi nhánh (Manager) (/admin/managers).
 * 
 * <p>Chức năng chính:</p>
 * <ul>
 *   <li>Xem danh sách các tài khoản Quản lý (Manager), thông tin nhân sự và cụm rạp phụ trách.</li>
 *   <li>Tạo mới tài khoản Manager và phân công cụm rạp phụ trách ban đầu.</li>
 *   <li>Cập nhật thông tin cá nhân của Quản lý (Họ tên, SĐT, Email).</li>
 *   <li>Kích hoạt (ACTIVE) hoặc Tạm ngưng (INACTIVE) tài khoản Quản lý.</li>
 *   <li>Đồng bộ trạng thái phân công khi Quản lý bị khóa hoặc tạm ngưng.</li>
 * </ul>
 * 
 * <p>Kiến trúc: Tuân thủ mô hình 3 lớp (3-Tier Architecture):</p>
 * <pre>
 *   ManagerManagementServlet (Controller) -&gt; UserService (Business Logic) -&gt; UserDAO / CinemaDAO (Data Access)
 * </pre>
 * 
 * @author CineFlow Team
 */
@WebServlet(name = "ManagerManagementServlet", urlPatterns = {"/admin/managers"})
public class ManagerManagementServlet extends HttpServlet {

    /** Tầng nghiệp vụ xử lý logic tài khoản */
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
     * Xử lý yêu cầu HTTP GET: Hiển thị giao diện danh sách Quản lý (Manager).
     * 
     * Các bước thực hiện:
     * 1. Xác thực đăng nhập: nếu chưa đăng nhập, chuyển hướng về trang /login.
     * 2. Phân quyền: chỉ cho phép ADMIN truy cập.
     * 3. Đồng bộ hóa trạng thái phân công quản lý đối với các tài khoản bị khóa/ngưng hoạt động.
     * 4. Lấy danh sách Manager theo từ khóa tìm kiếm (nếu có).
     * 5. Lấy danh sách các cụm rạp đang còn trống Quản lý để phục vụ modal tạo mới.
     * 6. Gán thuộc tính vào request scope và chuyển tiếp (forward) tới managerList.jsp.
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
                    + URLEncoder.encode("/admin/managers", StandardCharsets.UTF_8);
            response.sendRedirect(loginUrl);
            return;
        }

        // 2. Kiểm tra quyền Quản trị viên (ADMIN)
        if (!isAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập quản lý tài khoản Manager.");
            return;
        }

        // 3. Đồng bộ hóa phân công cho các quản lý bị khóa
        userService.syncLockedManagersAssignments();

        // 4. Lấy danh sách quản lý theo từ khóa tìm kiếm
        String searchParam = request.getParameter("search");
        List<User> managers = userService.getManagers(searchParam);

        // 5. Lấy danh sách các cụm rạp hiện chưa có quản lý phụ trách
        List<Centre> centres = userService.getCentresWithoutActiveManager();

        // 6. Gán dữ liệu vào Request Scope để JSP hiển thị
        request.setAttribute("managers", managers);
        request.setAttribute("centres", centres);
        request.setAttribute("searchParam", searchParam);
        request.setAttribute("activeNav", "managers");

        // Chuyển tiếp tới giao diện quản lý Manager
        request.getRequestDispatcher("/WEB-INF/views/admin/user/managerList.jsp").forward(request, response);
    }

    /**
     * Xử lý yêu cầu HTTP POST: Thực hiện các nghiệp vụ thay đổi thông tin Manager.
     * 
     * Hỗ trợ các hành động (action):
     * <ul>
     *   <li><b>create</b>: Tạo tài khoản Manager mới và phân công cụm rạp phụ trách ban đầu.</li>
     *   <li><b>update</b>: Cập nhật thông tin cơ bản của Manager (họ tên, số điện thoại, email).</li>
     *   <li><b>toggle_status</b>: Chuyển đổi trạng thái tài khoản giữa ACTIVE (Hoạt động) và INACTIVE (Tạm ngưng).</li>
     * </ul>
     *
     * @param request  Yêu cầu HTTP gửi từ client
     * @param response Phản hồi HTTP trả về cho client
     * @throws IOException Nếu có lỗi I/O khi chuyển hướng hoặc ghi phản hồi
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");

        // Kiểm tra xác thực và quyền Quản trị viên (ADMIN)
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null || !isAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền thực hiện thao tác này.");
            return;
        }

        int sessionUserId = (int) session.getAttribute("userId");
        String action = request.getParameter("action");

        // =================================================================
        // HÀNH ĐỘNG 1: TẠO MỚI TÀI KHOẢN MANAGER
        // =================================================================
        if ("create".equalsIgnoreCase(action)) {
            String fullName = request.getParameter("fullName");
            String account = request.getParameter("account");
            String email = request.getParameter("email");
            String password = request.getParameter("password");
            String phone = request.getParameter("phone");
            String centreIdParam = request.getParameter("centreId");

            // Ủy quyền xử lý cho tầng Service (kiểm tra tính hợp lệ dữ liệu, mã hóa mật khẩu, gán rạp)
            UserService.ManagerOpResult result = userService.createManager(
                    sessionUserId, fullName, account, email, password, phone, centreIdParam
            );

            // Xử lý thành công
            if (result.isSuccess()) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?success=created");
                return;
            }

            // Chuyển đổi trạng thái lỗi từ enum sang mã lỗi trên URL
            String errorCode = switch (result.status()) {
                case MISSING_FIELDS -> "missing_fields";
                case INVALID_CENTRE -> "invalid_centre";
                case CENTRE_HAS_MANAGER -> "centre_has_manager";
                case INVALID_NAME -> "invalid_name";
                case INVALID_ACCOUNT -> "invalid_account";
                case INVALID_EMAIL -> "invalid_email";
                case DISPOSABLE_EMAIL -> "disposable_email";
                case WEAK_PASSWORD -> "weak_password";
                case INVALID_PHONE -> "invalid_phone";
                case ACCOUNT_EXISTS -> "account_exists";
                case EMAIL_EXISTS -> "email_exists";
                default -> "create_failed";
            };
            response.sendRedirect(request.getContextPath() + "/admin/managers?error=" + errorCode);
            return;
        }

        // =================================================================
        // HÀNH ĐỘNG 2: CẬP NHẬT THÔNG TIN MANAGER
        // =================================================================
        if ("update".equalsIgnoreCase(action)) {
            try {
                int userId = Integer.parseInt(request.getParameter("userId"));
                String fullName = request.getParameter("fullName");
                String phone = request.getParameter("phone");
                String email = request.getParameter("email");

                // Gọi tầng Service cập nhật thông tin
                UserService.ManagerOpResult result = userService.updateManager(userId, fullName, phone, email);
                if (result.isSuccess()) {
                    response.sendRedirect(request.getContextPath() + "/admin/managers?success=updated");
                    return;
                }

                // Xử lý mã lỗi tương ứng
                String errorCode = switch (result.status()) {
                    case MISSING_FIELDS -> "missing_fields";
                    case INVALID_NAME -> "invalid_name";
                    case INVALID_EMAIL -> "invalid_email";
                    case DISPOSABLE_EMAIL -> "disposable_email";
                    case INVALID_PHONE -> "invalid_phone";
                    case NOT_MANAGER -> "not_manager";
                    case EMAIL_EXISTS -> "email_exists";
                    default -> "update_failed";
                };
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=" + errorCode);
                return;
            } catch (Exception e) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=invalid_data");
                return;
            }
        }

        // =================================================================
        // HÀNH ĐỘNG 3: BẬT/TẮT TRẠNG THÁI TÀI KHOẢN (ACTIVE / INACTIVE)
        // =================================================================
        if ("toggle_status".equalsIgnoreCase(action)) {
            try {
                int userId = Integer.parseInt(request.getParameter("userId"));
                String status = request.getParameter("status");

                // Gọi Service để cập nhật trạng thái hoạt động của quản lý
                UserService.ManagerStatusResult result = userService.toggleManagerStatus(userId, status, sessionUserId);
                switch (result) {
                    case SUCCESS -> response.sendRedirect(request.getContextPath() + "/admin/managers?success=status_updated");
                    case NOT_MANAGER, NOT_FOUND -> response.sendRedirect(request.getContextPath() + "/admin/managers?error=not_manager");
                    case FAILED -> response.sendRedirect(request.getContextPath() + "/admin/managers?error=status_failed");
                }
                return;
            } catch (Exception e) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=status_failed");
                return;
            }
        }

        // Mặc định chuyển hướng về trang danh sách
        response.sendRedirect(request.getContextPath() + "/admin/managers");
    }
}
