package controller.admin;
import jakarta.servlet.annotation.WebServlet;

import dao.CinemaDAO;
import dao.UserDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.entity.Centre;
import model.entity.User;
import utils.PasswordUtils;
import utils.ValidationUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controller điều khiển phân hệ Quản lý Tài khoản Quản lý (Manager).
 * Trách nhiệm: Quản lý thông tin tài khoản Manager (tạo mới, cập nhật thông tin cá nhân, kích hoạt hoặc tạm ngưng tài khoản).
 * Nghiệp vụ Bổ nhiệm vào rạp và Điều chuyển nhân sự được quản lý riêng biệt tại StaffTransferServlet (/admin/transfers).
 */
@WebServlet(name = "ManagerAccountServlet", urlPatterns = {"/admin/managers"})
public class ManagerAccountServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final CinemaDAO cinemaDAO = new CinemaDAO();

    /**
     * Kiểm tra phiên đăng nhập hiện tại có vai trò ADMIN hay không.
     *
     * @param session phiên làm việc HttpSession
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
     * Xử lý yêu cầu HTTP GET: Tải danh sách Manager, danh sách cụm rạp trống và hiển thị giao diện.
     *
     * @param request  yêu cầu HTTP từ client
     * @param response phản hồi HTTP gửi về client
     * @throws ServletException nếu xảy ra lỗi servlet
     * @throws IOException      nếu xảy ra lỗi vào/ra dữ liệu
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 1. Kiểm tra đăng nhập (nếu chưa đăng nhập chuyển hướng về trang đăng nhập)
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            String loginUrl = request.getContextPath() + "/auth/login.jsp?redirect="
                    + URLEncoder.encode("/admin/managers", StandardCharsets.UTF_8);
            response.sendRedirect(loginUrl);
            return;
        }

        // 2. Kiểm tra quyền Admin (nếu không phải Admin thì trả về mã lỗi 403 Forbidden)
        if (!isAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập quản lý tài khoản Manager.");
            return;
        }

        // 3. Tự động đồng bộ các Manager đang bị tạm ngưng/khóa để đảm bảo không rạp nào bị liên kết sai lệch
        userDAO.syncLockedManagersAssignments();

        // 4. Nhận tham số tìm kiếm (theo tên, tài khoản hoặc email) nếu có
        String searchParam = request.getParameter("search");

        // 5. Truy vấn cơ sở dữ liệu lấy danh sách Quản lý rạp và danh sách cụm rạp chưa có Quản lý
        List<User> managers = userDAO.getManagers(searchParam);
        List<Centre> centres = cinemaDAO.getCentresWithoutActiveManager();

        // 6. Gửi dữ liệu ra tầng giao diện JSP
        request.setAttribute("managers", managers);
        request.setAttribute("centres", centres);
        request.setAttribute("searchParam", searchParam);
        request.setAttribute("activeNav", "managers");

        // 7. Chuyển tiếp tới giao diện managers.jsp
        request.getRequestDispatcher("/admin/managers.jsp").forward(request, response);
    }

    /**
     * Xử lý yêu cầu HTTP POST: Tiếp nhận và điều hướng các thao tác tạo mới, cập nhật hoặc đổi trạng thái Manager.
     *
     * @param request  yêu cầu HTTP từ client
     * @param response phản hồi HTTP gửi về client
     * @throws IOException nếu xảy ra lỗi vào/ra dữ liệu
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 1. Thiết lập bảng mã UTF-8 cho biểu mẫu
        request.setCharacterEncoding("UTF-8");

        // 2. Kiểm tra xác thực phiên và quyền Admin
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null || !isAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền thực hiện thao tác này.");
            return;
        }

        // 3. Đọc hành động cần thực thi
        String action = request.getParameter("action");

        // 4. Xử lý tạo mới tài khoản Manager (action = "create")
        if ("create".equalsIgnoreCase(action)) {
            // 4.1. Lấy dữ liệu các trường nhập vào từ form
            String fullName = request.getParameter("fullName");
            String account = request.getParameter("account");
            String email = request.getParameter("email");
            String password = request.getParameter("password");
            String phone = request.getParameter("phone");
            String centreIdParam = request.getParameter("centreId");

            // 4.2. Kiểm tra các trường bắt buộc không được để trống
            if (ValidationUtils.isBlank(fullName)
                    || ValidationUtils.isBlank(account)
                    || ValidationUtils.isBlank(email)
                    || ValidationUtils.isBlank(password)
                    || ValidationUtils.isBlank(centreIdParam)) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=missing_fields");
                return;
            }

            // 4.3. Kiểm tra định dạng số của centreId
            int centreId;
            try {
                centreId = Integer.parseInt(centreIdParam.trim());
            } catch (NumberFormatException e) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=invalid_centre");
                return;
            }

            // 4.4. Kiểm tra xem rạp được chọn đã có Quản lý hoạt động hay chưa (1 rạp chỉ có 1 Quản lý)
            if (cinemaDAO.hasActiveManager(centreId)) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=centre_has_manager");
                return;
            }

            fullName = fullName.trim();
            account = account.trim();
            email = email.trim();
            phone = phone != null ? phone.trim() : "";

            // 4.5. Validate định dạng Họ và tên
            if (!ValidationUtils.isValidFullName(fullName)) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=invalid_name");
                return;
            }

            // 4.6. Validate định dạng Tên đăng nhập (Account)
            if (!ValidationUtils.isValidAccount(account)) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=invalid_account");
                return;
            }

            // 4.7. Validate định dạng Email
            if (!ValidationUtils.isValidEmail(email)) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=invalid_email");
                return;
            }

            // 4.8. Chặn các tên miền email rác dùng 1 lần (disposable email)
            if (ValidationUtils.isDisposableEmail(email)) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=disposable_email");
                return;
            }

            // 4.9. Kiểm tra độ mạnh của mật khẩu (tối thiểu 8 ký tự, chữ hoa, chữ thường, số, ký tự đặc biệt)
            if (!ValidationUtils.isValidPassword(password)) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=weak_password");
                return;
            }

            // 4.10. Validate số điện thoại hợp lệ (đầu số Việt Nam 10 chữ số)
            if (!ValidationUtils.isValidPhone(phone, false)) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=invalid_phone");
                return;
            }

            // 4.11. Kiểm tra tính duy nhất: Tài khoản không được trùng
            if (userDAO.isAccountExists(account)) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=account_exists");
                return;
            }

            // 4.12. Kiểm tra tính duy nhất: Email không được trùng
            if (userDAO.isEmailExists(email)) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=email_exists");
                return;
            }

            // 4.13. Đóng gói thông tin đối tượng User và mã hóa mật khẩu bằng BCrypt
            int sessionUserId = (int) session.getAttribute("userId");
            User newManager = new User();
            newManager.setFullName(fullName);
            newManager.setAccount(account);
            newManager.setEmail(email);
            newManager.setPassword(PasswordUtils.hashPassword(password.trim()));
            newManager.setPhone(phone);
            newManager.setCentreId(centreId);

            // 4.14. Gọi UserDAO để tạo mới Manager và ghi nhận lịch sử phân công
            int createdId = userDAO.createManager(newManager, sessionUserId);
            if (createdId > 0) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?success=created");
            } else {
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=create_failed");
            }
            return;
        }

        // 5. Xử lý chỉnh sửa thông tin cá nhân Manager (action = "update")
        if ("update".equalsIgnoreCase(action)) {
            try {
                // 5.1. Đọc và ép kiểu dữ liệu từ form
                int userId = Integer.parseInt(request.getParameter("userId"));
                String fullName = request.getParameter("fullName");
                String phone = request.getParameter("phone");
                String email = request.getParameter("email");

                // 5.2. Kiểm tra các trường thông tin bắt buộc
                if (ValidationUtils.isBlank(fullName) || ValidationUtils.isBlank(email)) {
                    response.sendRedirect(request.getContextPath() + "/admin/managers?error=missing_fields");
                    return;
                }

                fullName = fullName.trim();
                email = email.trim();
                phone = phone != null ? phone.trim() : "";

                // 5.3. Validate định dạng Họ tên, Email, Phone
                if (!ValidationUtils.isValidFullName(fullName)) {
                    response.sendRedirect(request.getContextPath() + "/admin/managers?error=invalid_name");
                    return;
                }

                if (!ValidationUtils.isValidEmail(email)) {
                    response.sendRedirect(request.getContextPath() + "/admin/managers?error=invalid_email");
                    return;
                }

                if (ValidationUtils.isDisposableEmail(email)) {
                    response.sendRedirect(request.getContextPath() + "/admin/managers?error=disposable_email");
                    return;
                }

                if (!ValidationUtils.isValidPhone(phone, false)) {
                    response.sendRedirect(request.getContextPath() + "/admin/managers?error=invalid_phone");
                    return;
                }

                // 5.4. Xác thực đối tượng mục tiêu: phải tồn tại và có vai trò MANAGER
                User target = userDAO.getUserById(userId);
                if (target == null || !"MANAGER".equalsIgnoreCase(target.getRole())) {
                    response.sendRedirect(request.getContextPath() + "/admin/managers?error=not_manager");
                    return;
                }

                // 5.5. Kiểm tra email có bị trùng với tài khoản người dùng khác trong hệ thống không
                if (userDAO.isEmailExistsForOtherUser(email, userId)) {
                    response.sendRedirect(request.getContextPath() + "/admin/managers?error=email_exists");
                    return;
                }

                // 5.6. Cập nhật thông tin vào DB thông qua UserDAO
                boolean updated = userDAO.updateUserBasicInfo(userId, fullName, phone, email);
                if (updated) {
                    response.sendRedirect(request.getContextPath() + "/admin/managers?success=updated");
                } else {
                    response.sendRedirect(request.getContextPath() + "/admin/managers?error=update_failed");
                }
                return;
            } catch (Exception e) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=invalid_data");
                return;
            }
        }

        // 6. Xử lý kích hoạt hoặc tạm ngưng tài khoản (action = "toggle_status")
        if ("toggle_status".equalsIgnoreCase(action)) {
            try {
                // 6.1. Nhận ID người dùng và trạng thái mới muốn thiết lập
                int userId = Integer.parseInt(request.getParameter("userId"));
                String status = request.getParameter("status");
                if (!ValidationUtils.isValidUserStatus(status)) {
                    status = "ACTIVE";
                }

                // 6.2. Kiểm tra tài khoản mục tiêu phải tồn tại và đúng vai trò MANAGER
                User target = userDAO.getUserById(userId);
                if (target == null || !"MANAGER".equalsIgnoreCase(target.getRole())) {
                    response.sendRedirect(request.getContextPath() + "/admin/managers?error=not_manager");
                    return;
                }

                // 6.3. Cập nhật trạng thái người dùng (ACTIVE / INACTIVE / LOCKED)
                String newStatus = status.toUpperCase();
                userDAO.updateUserStatus(userId, newStatus);

                // 6.4. Nếu khóa hoặc tạm ngưng tài khoản, lập tức giải phóng cụm rạp để rạp không bị phong tỏa
                if (!"ACTIVE".equalsIgnoreCase(newStatus)) {
                    int sessionAdminId = session.getAttribute("userId") != null ? (int) session.getAttribute("userId") : 0;
                    userDAO.unassignManagerByUserId(userId, sessionAdminId, "Tài khoản Quản lý bị khóa/tạm ngưng");
                }

                // 6.5. Đồng bộ lại toàn bộ trạng thái gán cụm rạp trong cơ sở dữ liệu
                userDAO.syncLockedManagersAssignments();

                response.sendRedirect(request.getContextPath() + "/admin/managers?success=status_updated");
                return;
            } catch (Exception e) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=status_failed");
                return;
            }
        }

        // 7. Nếu không khớp hành động nào, chuyển hướng về trang danh sách Quản lý
        response.sendRedirect(request.getContextPath() + "/admin/managers");
    }
}
