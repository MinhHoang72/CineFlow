package controller.manager;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.entity.Centre;
import model.entity.User;
import service.StaffService;
import service.UserService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controller: Quản lý Nhân sự (Staff) của Quản lý chi nhánh (/staffList).
 *
 * <p>Chức năng chính:</p>
 * <ul>
 *   <li>Xem danh sách nhân viên thuộc cụm rạp do Manager phụ trách.</li>
 *   <li>Tạo mới tài khoản nhân viên qua staffForm.jsp.</li>
 *   <li>Chỉnh sửa thông tin nhân viên qua staffForm.jsp.</li>
 *   <li>Cập nhật trạng thái nhân viên (ACTIVE, INACTIVE, LOCKED).</li>
 * </ul>
 *
 * @author CineFlow Team
 */
@WebServlet(name = "StaffManagementServlet", urlPatterns = {"/staffList"})
public class StaffManagementServlet extends HttpServlet {

    private final StaffService staffService = new StaffService();
    private final UserService userService = new UserService();

    private User getCurrentManager(HttpSession session) {
        if (session == null || session.getAttribute("userId") == null) {
            return null;
        }
        Object idObject = session.getAttribute("userId");
        if (!(idObject instanceof Number)) {
            return null;
        }
        int userId = ((Number) idObject).intValue();
        User manager = userService.getUserById(userId);
        if (manager == null || !"MANAGER".equalsIgnoreCase(manager.getRole())) {
            return null;
        }
        session.setAttribute("user", manager);
        session.setAttribute("role", manager.getRole());
        return manager;
    }

    private boolean isAjax(HttpServletRequest request) {
        return "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                || "1".equals(request.getParameter("ajax"));
    }

    private void writeJson(HttpServletResponse response, int statusCode, String json) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(json);
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private boolean requireLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            String redirect = URLEncoder.encode("/staffList", StandardCharsets.UTF_8);
            response.sendRedirect(request.getContextPath() + "/login?redirect=" + redirect);
            return false;
        }
        return true;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!requireLogin(request, response)) {
            return;
        }

        HttpSession session = request.getSession(false);
        User manager = getCurrentManager(session);

        if (manager == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Manager mới được truy cập trang quản lý nhân sự.");
            return;
        }

        if (!staffService.isManagerAssigned(manager)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tài khoản Manager chưa được phân công trung tâm.");
            return;
        }

        String action = request.getParameter("action");
        if ("create".equalsIgnoreCase(action)) {
            request.setAttribute("assignedCentre", staffService.getManagerCentre(manager));
            request.setAttribute("activeNav", "staff");
            request.getRequestDispatcher("/WEB-INF/views/manager/staffForm.jsp").forward(request, response);
            return;
        }

        if ("edit".equalsIgnoreCase(action)) {
            try {
                int userId = Integer.parseInt(request.getParameter("userId"));
                User staff = userService.getUserById(userId);
                if (staff == null || !staff.isStaff() || !manager.getCentreId().equals(staff.getCentreId())) {
                    response.sendRedirect(request.getContextPath() + "/staffList?error=not_found");
                    return;
                }
                request.setAttribute("staff", staff);
                request.setAttribute("assignedCentre", staffService.getManagerCentre(manager));
                request.setAttribute("activeNav", "staff");
                request.getRequestDispatcher("/WEB-INF/views/manager/staffForm.jsp").forward(request, response);
                return;
            } catch (NumberFormatException e) {
                response.sendRedirect(request.getContextPath() + "/staffList?error=invalid_data");
                return;
            }
        }

        String search = request.getParameter("search");
        String role = request.getParameter("role");

        List<User> staffList = staffService.getStaffDirectory(manager, search, role);
        Centre assignedCentre = staffService.getManagerCentre(manager);
        request.setAttribute("staff", staffList);
        request.setAttribute("assignedCentre", assignedCentre);
        request.setAttribute("selectedCentreId", manager.getCentreId());

        request.getRequestDispatcher("/WEB-INF/views/manager/staffList.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");

        if (!requireLogin(request, response)) {
            return;
        }

        HttpSession session = request.getSession(false);
        User manager = getCurrentManager(session);

        if (manager == null || !staffService.isManagerAssigned(manager)) {
            if (isAjax(request)) {
                writeJson(response, HttpServletResponse.SC_FORBIDDEN, "{\"success\":false,\"message\":\"Bạn không có quyền thao tác.\"}");
            } else {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền quản lý nhân sự.");
            }
            return;
        }

        String action = request.getParameter("action");

        switch (action == null ? "" : action.toLowerCase()) {
            case "create" -> createStaff(request, response, manager);
            case "update" -> updateStaff(request, response, manager);
            case "status" -> updateStaffStatus(request, response, manager);
            default -> response.sendRedirect(request.getContextPath() + "/staffList");
        }
    }

    private void createStaff(HttpServletRequest request, HttpServletResponse response, User manager) throws IOException {
        String error = staffService.createStaff(
                manager,
                request.getParameter("fullName"),
                request.getParameter("account"),
                request.getParameter("email"),
                request.getParameter("password"),
                request.getParameter("phone"),
                request.getParameter("role")
        );

        if (error == null) {
            response.sendRedirect(request.getContextPath() + "/staffList?success=created");
        } else {
            response.sendRedirect(request.getContextPath() + "/staffList?action=create&error=" + error);
        }
    }

    private void updateStaff(HttpServletRequest request, HttpServletResponse response, User manager) throws IOException {
        try {
            int userId = Integer.parseInt(request.getParameter("userId"));
            String error = staffService.updateStaff(
                    manager,
                    userId,
                    request.getParameter("fullName"),
                    request.getParameter("phone"),
                    request.getParameter("email"),
                    request.getParameter("role")
            );

            if (error == null) {
                response.sendRedirect(request.getContextPath() + "/staffList?success=updated");
            } else {
                response.sendRedirect(request.getContextPath() + "/staffList?action=edit&userId=" + userId + "&error=" + error);
            }
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/staffList?error=invalid_data");
        }
    }

    private void updateStaffStatus(HttpServletRequest request, HttpServletResponse response, User manager) throws IOException {
        try {
            int userId = Integer.parseInt(request.getParameter("userId"));
            String status = request.getParameter("status");

            boolean ok = staffService.updateStaffStatus(manager, userId, status);

            if (isAjax(request)) {
                if (ok) {
                    writeJson(response, HttpServletResponse.SC_OK,
                            "{\"success\":true,\"userId\":" + userId + ",\"status\":\"" + escapeJson(status) + "\",\"message\":\"Cập nhật thành công.\"}");
                } else {
                    writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                            "{\"success\":false,\"message\":\"Cập nhật thất bại.\"}");
                }
                return;
            }

            response.sendRedirect(request.getContextPath() + "/staffList?" + (ok ? "success=status_updated" : "error=status_failed"));

        } catch (NumberFormatException e) {
            if (isAjax(request)) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, "{\"success\":false,\"message\":\"Dữ liệu không hợp lệ.\"}");
                return;
            }
            response.sendRedirect(request.getContextPath() + "/staffList?error=status_failed");
        }
    }
}
