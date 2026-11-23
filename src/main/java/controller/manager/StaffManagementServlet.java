package controller.manager;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.entity.User;
import service.StaffService;
import service.UserService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Controller: Quản lý Nhân sự (Staff) của Quản lý chi nhánh (/staffList).
 *
 * <p>Chức năng chính:</p>
 * <ul>
 *   <li>Xem danh sách nhân viên thuộc cụm rạp do Manager phụ trách.</li>
 *   <li>Tạo mới / Chỉnh sửa tài khoản nhân viên qua staffForm.jsp (action="save").</li>
 *   <li>Cập nhật trạng thái nhân viên (action="status").</li>
 * </ul>
 *
 * @author CineFlow Team
 */
@WebServlet(name = "StaffManagementServlet", urlPatterns = {"/staffList"})
public class StaffManagementServlet extends HttpServlet {

    private final StaffService staffService = new StaffService();
    private final UserService userService = new UserService();

    /**
     * Lấy tài khoản Manager từ Session (nếu chưa có mới tải từ DB).
     */
    private User getManager(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object userObj = session.getAttribute("user");
        if (userObj instanceof User) {
            User u = (User) userObj;
            if ("MANAGER".equalsIgnoreCase(u.getRole())) {
                return u;
            }
        }
        Object idObj = session.getAttribute("userId");
        if (idObj instanceof Number) {
            int userId = ((Number) idObj).intValue();
            User u = userService.getUserById(userId);
            if (u != null && "MANAGER".equalsIgnoreCase(u.getRole())) {
                session.setAttribute("user", u);
                session.setAttribute("role", u.getRole());
                return u;
            }
        }
        return null;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User manager = getManager(session);

        if (manager == null) {
            response.sendRedirect(request.getContextPath() + "/login?redirect="
                    + URLEncoder.encode("/staffList", StandardCharsets.UTF_8));
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

        request.setAttribute("staff", staffService.getStaffDirectory(manager, request.getParameter("search"), null));
        request.setAttribute("assignedCentre", staffService.getManagerCentre(manager));
        request.setAttribute("selectedCentreId", manager.getCentreId());
        request.getRequestDispatcher("/WEB-INF/views/manager/staffList.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        User manager = getManager(request.getSession(false));

        if (manager == null || !staffService.isManagerAssigned(manager)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền quản lý nhân sự.");
            return;
        }

        String action = request.getParameter("action");
        if (action == null) {
            action = "";
        }

        switch (action.toLowerCase()) {
            case "save":
                handleSaveStaff(request, response, manager);
                break;
            case "status":
                handleToggleStatus(request, response, manager);
                break;
            default:
                response.sendRedirect(request.getContextPath() + "/staffList");
                break;
        }
    }

    private void handleSaveStaff(HttpServletRequest request, HttpServletResponse response, User manager) throws IOException {
        String userIdParam = request.getParameter("userId");
        boolean isUpdate = userIdParam != null && !userIdParam.isBlank();

        if (isUpdate) {
            try {
                int userId = Integer.parseInt(userIdParam.trim());
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
        } else {
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
    }

    private void handleToggleStatus(HttpServletRequest request, HttpServletResponse response, User manager) throws IOException {
        try {
            int userId = Integer.parseInt(request.getParameter("userId"));
            String status = request.getParameter("status");
            boolean ok = staffService.updateStaffStatus(manager, userId, status);
            response.sendRedirect(request.getContextPath() + "/staffList?" + (ok ? "success=status_updated" : "error=status_failed"));
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/staffList?error=status_failed");
        }
    }
}
