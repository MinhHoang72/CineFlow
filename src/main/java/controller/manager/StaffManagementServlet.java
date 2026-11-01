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
 * Controller: Tiếp nhận HTTP request cho trang quản lý nhân sự (STAFF).
 * Mọi nghiệp vụ logic được ủy quyền cho StaffService (Mô hình 3 lớp Controller -> Service -> DAO).
 */
@WebServlet(name = "StaffManagementServlet", urlPatterns = {
        "/staffList", "/manager/staff-list", "/admin/staff-list",
        "/staff", "/manager/staff"
})
public class StaffManagementServlet extends HttpServlet {

    private final StaffService staffService = new StaffService();
    private final UserService userService = new UserService();

    private boolean canManage(HttpSession session) {
        if (session == null) {
            return false;
        }
        Object role = session.getAttribute("role");
        if (role == null && session.getAttribute("user") instanceof User) {
            role = ((User) session.getAttribute("user")).getRole();
        }
        return "MANAGER".equalsIgnoreCase((String) role) || "ADMIN".equalsIgnoreCase((String) role);
    }

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

    private boolean isAjax(HttpServletRequest request) {
        String xrw = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        String ajaxParam = request.getParameter("ajax");
        return "XMLHttpRequest".equalsIgnoreCase(xrw)
                || (accept != null && accept.contains("application/json"))
                || "1".equals(ajaxParam) || "true".equalsIgnoreCase(ajaxParam);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            String loginUrl = request.getContextPath() + "/login?redirect="
                    + URLEncoder.encode("/staffList", StandardCharsets.UTF_8);
            response.sendRedirect(loginUrl);
            return;
        }

        if (!canManage(session)) {
            String role = (String) session.getAttribute("role");
            if (role == null && session.getAttribute("user") instanceof User) {
                role = ((User) session.getAttribute("user")).getRole();
            }
            if (role != null && (role.toUpperCase().contains("STAFF") || role.toUpperCase().contains("TICKET"))) {
                response.sendRedirect(request.getContextPath() + "/dashboard");
                return;
            }
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập quản lý nhân sự.");
            return;
        }

        int sessionUserId = (int) session.getAttribute("userId");
        User currentUser = userService.getUserById(sessionUserId);
        if (currentUser == null) {
            currentUser = (User) session.getAttribute("user");
        } else {
            session.setAttribute("user", currentUser);
        }

        boolean adminRole = isAdmin(session);
        if (!adminRole && (currentUser == null || !"MANAGER".equalsIgnoreCase(currentUser.getRole())
                || !staffService.isManagerAssigned(currentUser))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tài khoản Manager chưa được phân công chi nhánh.");
            return;
        }

        Integer centreId = null;
        if (!adminRole) {
            centreId = currentUser.getCentreId();
        } else {
            String cp = request.getParameter("centreId");
            if (cp != null && !cp.trim().isEmpty()) {
                try {
                    centreId = Integer.parseInt(cp.trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }

        String searchParam = request.getParameter("search");
        String roleFilter = request.getParameter("role");

        List<User> staffList = staffService.getStaffDirectory(adminRole, centreId, searchParam, roleFilter);
        List<Centre> centres = staffService.getActiveCentres();
        Centre assignedCentre = null;
        if (centreId != null) {
            for (Centre c : centres) {
                if (c.getCentreId() == centreId) {
                    assignedCentre = c;
                    break;
                }
            }
        }

        request.setAttribute("staff", staffList);
        request.setAttribute("centres", centres);
        request.setAttribute("assignedCentre", assignedCentre);
        request.setAttribute("selectedCentreId", centreId);
        request.setAttribute("selectedRole", roleFilter);
        request.setAttribute("isAdmin", adminRole);

        request.getRequestDispatcher("/WEB-INF/views/manager/staffList.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null || !canManage(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền cập nhật nhân sự.");
            return;
        }

        int sessionUserId = (int) session.getAttribute("userId");
        User currentUser = userService.getUserById(sessionUserId);
        if (currentUser == null) {
            currentUser = (User) session.getAttribute("user");
        }
        boolean adminRole = isAdmin(session);

        String action = request.getParameter("action");

        // 1. Thao tác Tạo mới nhân viên
        if ("create".equalsIgnoreCase(action)) {
            String centreIdParam = request.getParameter("centreId");
            String fullName = request.getParameter("fullName");
            String account = request.getParameter("account");
            String email = request.getParameter("email");
            String password = request.getParameter("password");
            String phone = request.getParameter("phone");
            String role = request.getParameter("role");

            StaffService.OperationResult result = staffService.createStaff(
                    currentUser, adminRole, centreIdParam, fullName, account, email, password, phone, role
            );

            if (result.isSuccess()) {
                response.sendRedirect(request.getContextPath() + "/staffList?success=created");
                return;
            }

            String errorCode = switch (result.getStatus()) {
                case UNASSIGNED_MANAGER -> "unassigned_manager";
                case INVALID_CENTRE -> (centreIdParam == null || centreIdParam.isBlank()) ? "missing_centre" : "invalid_centre";
                case MISSING_FIELDS -> "missing_fields";
                case INVALID_NAME -> "invalid_name";
                case INVALID_ACCOUNT -> "invalid_account";
                case INVALID_EMAIL -> "invalid_email";
                case WEAK_PASSWORD -> "weak_password";
                case INVALID_PHONE -> "invalid_phone";
                case ACCOUNT_EXISTS -> "account_exists";
                case EMAIL_EXISTS -> "email_exists";
                default -> "create_failed";
            };
            response.sendRedirect(request.getContextPath() + "/staffList?error=" + errorCode);
            return;
        }

        // 2. Thao tác Cập nhật thông tin nhân viên
        if ("update".equalsIgnoreCase(action)) {
            try {
                int userId = Integer.parseInt(request.getParameter("userId"));
                String fullName = request.getParameter("fullName");
                String phone = request.getParameter("phone");
                String email = request.getParameter("email");
                String role = request.getParameter("role");

                StaffService.OperationResult result = staffService.updateStaff(
                        currentUser, adminRole, userId, fullName, phone, email, role
                );

                if (result.isSuccess()) {
                    response.sendRedirect(request.getContextPath() + "/staffList?success=updated");
                    return;
                }

                if (result.getStatus() == StaffService.Status.NOT_FOUND) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, result.getMessage());
                    return;
                }
                if (result.getStatus() == StaffService.Status.FORBIDDEN) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, result.getMessage());
                    return;
                }

                String errorCode = switch (result.getStatus()) {
                    case MISSING_FIELDS -> "missing_fields";
                    case INVALID_NAME -> "invalid_name";
                    case INVALID_EMAIL -> "invalid_email";
                    case INVALID_PHONE -> "invalid_phone";
                    case EMAIL_EXISTS -> "email_exists";
                    default -> "update_failed";
                };
                response.sendRedirect(request.getContextPath() + "/staffList?error=" + errorCode);
                return;
            } catch (Exception e) {
                response.sendRedirect(request.getContextPath() + "/staffList?error=invalid_data");
                return;
            }
        }

        // 3. Thao tác Cập nhật trạng thái nhân viên
        if ("status".equalsIgnoreCase(action) || request.getParameter("status") != null) {
            try {
                int userId = Integer.parseInt(request.getParameter("userId"));
                String status = request.getParameter("status");

                StaffService.OperationResult result = staffService.updateStaffStatus(
                        currentUser, adminRole, userId, status
                );

                if (result.isSuccess()) {
                    if (isAjax(request)) {
                        response.setContentType("application/json;charset=UTF-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.getWriter().write("{\"success\":true,\"userId\":" + userId + ",\"status\":\"" + status + "\",\"message\":\"Đã cập nhật trạng thái thành công.\"}");
                        return;
                    }
                    response.sendRedirect(request.getContextPath() + "/staffList?success=status_updated");
                    return;
                }

                int statusCode = switch (result.getStatus()) {
                    case INVALID_STATUS -> HttpServletResponse.SC_BAD_REQUEST;
                    case NOT_FOUND -> HttpServletResponse.SC_NOT_FOUND;
                    case FORBIDDEN -> HttpServletResponse.SC_FORBIDDEN;
                    default -> HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                };

                if (isAjax(request)) {
                    response.setStatus(statusCode);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"success\":false,\"message\":\"" + result.getMessage() + "\"}");
                    return;
                }

                if (statusCode == HttpServletResponse.SC_NOT_FOUND || statusCode == HttpServletResponse.SC_FORBIDDEN) {
                    response.sendError(statusCode, result.getMessage());
                    return;
                }

                String errorCode = (result.getStatus() == StaffService.Status.INVALID_STATUS) ? "invalid_status" : "status_failed";
                response.sendRedirect(request.getContextPath() + "/staffList?error=" + errorCode);
                return;
            } catch (Exception e) {
                if (isAjax(request)) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"success\":false,\"message\":\"Dữ liệu không hợp lệ hoặc lỗi xử lý.\"}");
                    return;
                }
                response.sendRedirect(request.getContextPath() + "/staffList?error=status_failed");
                return;
            }
        }

        response.sendRedirect(request.getContextPath() + "/staffList");
    }
}
