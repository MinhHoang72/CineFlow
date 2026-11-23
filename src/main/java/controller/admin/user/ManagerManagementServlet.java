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

/**
 * Controller: Quản lý Tài khoản Quản lý chi nhánh (Manager) (/admin/managers).
 *
 * <p>Chức năng chính:</p>
 * <ul>
 *   <li>Xem danh sách các tài khoản Quản lý (Manager) và cụm rạp phụ trách.</li>
 *   <li>Tạo mới tài khoản Manager và phân công cụm rạp ban đầu qua trang managerForm.jsp.</li>
 *   <li>Cập nhật thông tin cá nhân của Quản lý qua trang managerForm.jsp.</li>
 *   <li>Kích hoạt, tạm ngưng hoặc khóa tài khoản Quản lý.</li>
 * </ul>
 *
 * @author CineFlow Team
 */
@WebServlet(name = "ManagerManagementServlet", urlPatterns = {"/admin/managers"})
public class ManagerManagementServlet extends HttpServlet {

    private final UserService userService = new UserService();

    private boolean isAdmin(HttpSession session) {
        if (session == null) {
            return false;
        }
        Object role = session.getAttribute("role");
        if (role == null) {
            Object userObj = session.getAttribute("user");
            if (userObj instanceof User) {
                role = ((User) userObj).getRole();
            }
        }
        return "ADMIN".equalsIgnoreCase((String) role);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect(request.getContextPath() + "/login?redirect="
                    + URLEncoder.encode("/admin/managers", StandardCharsets.UTF_8));
            return;
        }

        if (!isAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập quản lý tài khoản Manager.");
            return;
        }

        String action = request.getParameter("action");
        if ("create".equalsIgnoreCase(action)) {
            request.setAttribute("centres", userService.getCentresWithoutActiveManager());
            request.setAttribute("activeNav", "managers");
            request.getRequestDispatcher("/WEB-INF/views/admin/user/managerForm.jsp").forward(request, response);
            return;
        }

        if ("edit".equalsIgnoreCase(action)) {
            try {
                int userId = Integer.parseInt(request.getParameter("userId"));
                User manager = userService.getUserById(userId);
                if (manager == null || !"MANAGER".equalsIgnoreCase(manager.getRole())) {
                    response.sendRedirect(request.getContextPath() + "/admin/managers?error=not_manager");
                    return;
                }
                request.setAttribute("manager", manager);
                request.setAttribute("centres", userService.getCentresWithoutActiveManager());
                request.setAttribute("activeNav", "managers");
                request.getRequestDispatcher("/WEB-INF/views/admin/user/managerForm.jsp").forward(request, response);
                return;
            } catch (NumberFormatException e) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=invalid_data");
                return;
            }
        }

        request.setAttribute("managers", userService.getManagers());
        request.setAttribute("centres", userService.getCentresWithoutActiveManager());
        request.setAttribute("activeNav", "managers");
        request.getRequestDispatcher("/WEB-INF/views/admin/user/managerList.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        // 1. Kiểm tra có phải admin không?
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null || !isAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền thực hiện thao tác này.");
            return;
        }
        // 2. Lấy tham số action từ form
        int sessionUserId = (int) session.getAttribute("userId");
        String action = request.getParameter("action");

        if ("save".equalsIgnoreCase(action)) {
            handleSaveManager(request, response, sessionUserId);
        } else if ("status".equalsIgnoreCase(action) || "toggle_status".equalsIgnoreCase(action)) {
            handleToggleStatus(request, response, sessionUserId);
        } else {
            response.sendRedirect(request.getContextPath() + "/admin/managers");
        }
    }

    /**
     * Xử lý lưu thông tin Manager từ managerForm.jsp:
     * - Nếu có userId > 0  -> Thực hiện CẬP NHẬT (Sửa).
     * - Nếu không có userId -> Thực hiện TẠO MỚI.
     */
    private void handleSaveManager(HttpServletRequest request, HttpServletResponse response, int sessionUserId)
            throws IOException {
        String userIdParam = request.getParameter("userId");
        boolean isUpdate = userIdParam != null && !userIdParam.isBlank();

        if (isUpdate) {
            try {
                int userId = Integer.parseInt(userIdParam.trim());
                String error = userService.updateManager(
                        sessionUserId,
                        userId,
                        request.getParameter("fullName"),
                        request.getParameter("phone"),
                        request.getParameter("email"),
                        request.getParameter("centreId")
                );
                if (error == null) {
                    response.sendRedirect(request.getContextPath() + "/admin/managers?success=updated");
                } else {
                    response.sendRedirect(request.getContextPath() + "/admin/managers?action=edit&userId=" + userId + "&error=" + error);
                }
            } catch (NumberFormatException e) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=invalid_data");
            }
        } else {
            String error = userService.createManager(
                    sessionUserId,
                    request.getParameter("fullName"),
                    request.getParameter("account"),
                    request.getParameter("email"),
                    request.getParameter("password"),
                    request.getParameter("phone"),
                    request.getParameter("centreId")
            );
            if (error == null) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?success=created");
            } else {
                response.sendRedirect(request.getContextPath() + "/admin/managers?action=create&error=" + error);
            }
        }
    }

    private void handleToggleStatus(HttpServletRequest request, HttpServletResponse response, int sessionUserId)
            throws IOException {
        try {
            int userId = Integer.parseInt(request.getParameter("userId"));
            String status = request.getParameter("status");
            boolean ok = userService.toggleManagerStatus(userId, status, sessionUserId);
            response.sendRedirect(request.getContextPath() + "/admin/managers?" + (ok ? "success=status_updated" : "error=status_failed"));
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/admin/managers?error=status_failed");
        }
    }
}
