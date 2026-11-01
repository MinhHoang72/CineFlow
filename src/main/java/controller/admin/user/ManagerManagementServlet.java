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
 * Controller: Quản lý Tài khoản Quản lý (Manager).
 * Toàn bộ logic nghiệp vụ được ủy quyền cho UserService (Mô hình 3 lớp).
 */
@WebServlet(name = "ManagerManagementServlet", urlPatterns = {"/admin/managers"})
public class ManagerManagementServlet extends HttpServlet {

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
                    + URLEncoder.encode("/admin/managers", StandardCharsets.UTF_8);
            response.sendRedirect(loginUrl);
            return;
        }

        if (!isAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập quản lý tài khoản Manager.");
            return;
        }

        userService.syncLockedManagersAssignments();

        String searchParam = request.getParameter("search");
        List<User> managers = userService.getManagers(searchParam);
        List<Centre> centres = userService.getCentresWithoutActiveManager();

        request.setAttribute("managers", managers);
        request.setAttribute("centres", centres);
        request.setAttribute("searchParam", searchParam);
        request.setAttribute("activeNav", "managers");

        request.getRequestDispatcher("/WEB-INF/views/admin/user/managerList.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null || !isAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền thực hiện thao tác này.");
            return;
        }

        int sessionUserId = (int) session.getAttribute("userId");
        String action = request.getParameter("action");

        // 1. Tạo mới Manager
        if ("create".equalsIgnoreCase(action)) {
            String fullName = request.getParameter("fullName");
            String account = request.getParameter("account");
            String email = request.getParameter("email");
            String password = request.getParameter("password");
            String phone = request.getParameter("phone");
            String centreIdParam = request.getParameter("centreId");

            UserService.ManagerOpResult result = userService.createManager(
                    sessionUserId, fullName, account, email, password, phone, centreIdParam
            );

            if (result.isSuccess()) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?success=created");
                return;
            }

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

        // 2. Chỉnh sửa thông tin Manager
        if ("update".equalsIgnoreCase(action)) {
            try {
                int userId = Integer.parseInt(request.getParameter("userId"));
                String fullName = request.getParameter("fullName");
                String phone = request.getParameter("phone");
                String email = request.getParameter("email");

                UserService.ManagerOpResult result = userService.updateManager(userId, fullName, phone, email);
                if (result.isSuccess()) {
                    response.sendRedirect(request.getContextPath() + "/admin/managers?success=updated");
                    return;
                }

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

        // 3. Toggle Status Manager
        if ("toggle_status".equalsIgnoreCase(action)) {
            try {
                int userId = Integer.parseInt(request.getParameter("userId"));
                String status = request.getParameter("status");

                boolean ok = userService.toggleManagerStatus(userId, status, sessionUserId);
                if (ok) {
                    response.sendRedirect(request.getContextPath() + "/admin/managers?success=status_updated");
                } else {
                    response.sendRedirect(request.getContextPath() + "/admin/managers?error=not_manager");
                }
                return;
            } catch (Exception e) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=status_failed");
                return;
            }
        }

        response.sendRedirect(request.getContextPath() + "/admin/managers");
    }
}
