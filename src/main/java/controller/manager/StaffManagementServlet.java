
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

@WebServlet(name = "StaffManagementServlet", urlPatterns = {"/staffList"})
public class StaffManagementServlet extends HttpServlet {

    private final StaffService staffService = new StaffService();
    private final UserService userService = new UserService();

    // Lấy Manager hiện tại từ database.
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

        if (manager == null
                || !"MANAGER".equalsIgnoreCase(manager.getRole())) {
            return null;
        }

        // Cập nhật lại thông tin tài khoản trong session.
        session.setAttribute("user", manager);
        session.setAttribute("role", manager.getRole());

        return manager;
    }

    // Kiểm tra request AJAX.
    private boolean isAjax(HttpServletRequest request) {
        return "XMLHttpRequest".equalsIgnoreCase(
                request.getHeader("X-Requested-With")
        ) || "1".equals(request.getParameter("ajax"));
    }

    // Trả JSON.
    private void writeJson(
            HttpServletResponse response,
            int statusCode,
            String json) throws IOException {

        response.setStatus(statusCode);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(json);
    }

    // Mã hóa chuỗi trước khi đưa vào JSON.
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    // Kiểm tra đăng nhập.
    private boolean requireLogin(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userId") == null) {
            String redirect = URLEncoder.encode(
                    "/staffList",
                    StandardCharsets.UTF_8
            );

            response.sendRedirect(
                    request.getContextPath() + "/login?redirect=" + redirect
            );

            return false;
        }

        return true;
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        if (!requireLogin(request, response)) {
            return;
        }

        HttpSession session = request.getSession(false);
        User manager = getCurrentManager(session);

        // Chỉ MANAGER được truy cập.
        if (manager == null) {
            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "Chỉ Manager mới được truy cập trang quản lý nhân sự."
            );
            return;
        }

        // Manager phải được bổ nhiệm trung tâm.
        if (!staffService.isManagerAssigned(manager)) {
            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "Tài khoản Manager chưa được phân công trung tâm."
            );
            return;
        }

        // Lấy danh sách nhân viên của trung tâm hiện tại.
        String search = request.getParameter("search");
        String role = request.getParameter("role");

        List<User> staffList = staffService.getStaffDirectory(manager, search, role);
        Centre assignedCentre = staffService.getManagerCentre(manager);
        request.setAttribute("staff", staffList);
        request.setAttribute("assignedCentre", assignedCentre);
        request.setAttribute("selectedCentreId", manager.getCentreId());

        request.getRequestDispatcher(
                "/WEB-INF/views/manager/staffList.jsp"
        ).forward(request, response);
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        request.setCharacterEncoding("UTF-8");

        if (!requireLogin(request, response)) {
            return;
        }

        HttpSession session = request.getSession(false);
        User manager = getCurrentManager(session);

        // Không cho ADMIN hoặc tài khoản khác thao tác.
        if (manager == null || !staffService.isManagerAssigned(manager)) {
            if (isAjax(request)) {
                writeJson(
                        response,
                        HttpServletResponse.SC_FORBIDDEN,
                        "{\"success\":false,\"message\":\"Bạn không có quyền thao tác.\"}"
                );
            } else {
                response.sendError(
                        HttpServletResponse.SC_FORBIDDEN,
                        "Bạn không có quyền quản lý nhân sự."
                );
            }
            return;
        }

        String action = request.getParameter("action");

        switch (action == null ? "" : action.toLowerCase()) {

            case "create":
                createStaff(request, response, manager);
                break;

            case "update":
                updateStaff(request, response, manager);
                break;

            case "status":
                updateStaffStatus(request, response, manager);
                break;

            default:
                response.sendRedirect(request.getContextPath() + "/staffList");
        }
    }

    // Tạo nhân viên.
    private void createStaff(
            HttpServletRequest request,
            HttpServletResponse response,
            User manager) throws IOException {

        StaffService.OperationResult result = staffService.createStaff(
                manager,
                request.getParameter("fullName"),
                request.getParameter("account"),
                request.getParameter("email"),
                request.getParameter("password"),
                request.getParameter("phone"),
                request.getParameter("role")
        );

        if (result.isSuccess()) {
            response.sendRedirect(
                    request.getContextPath() + "/staffList?success=created"
            );
            return;
        }

        String errorCode = switch (result.getStatus()) {
            case UNASSIGNED_MANAGER -> "unassigned_manager";
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

        response.sendRedirect(
                request.getContextPath() + "/staffList?error=" + errorCode
        );
    }

    // Cập nhật thông tin nhân viên.
    private void updateStaff(
            HttpServletRequest request,
            HttpServletResponse response,
            User manager) throws IOException {

        try {
            int userId = Integer.parseInt(request.getParameter("userId"));

            StaffService.OperationResult result = staffService.updateStaff(
                    manager,
                    userId,
                    request.getParameter("fullName"),
                    request.getParameter("phone"),
                    request.getParameter("email"),
                    request.getParameter("role")
            );

            if (result.isSuccess()) {
                response.sendRedirect(
                        request.getContextPath() + "/staffList?success=updated"
                );
                return;
            }

            if (result.getStatus() == StaffService.Status.NOT_FOUND) {
                response.sendError(
                        HttpServletResponse.SC_NOT_FOUND,
                        result.getMessage()
                );
                return;
            }

            if (result.getStatus() == StaffService.Status.FORBIDDEN) {
                response.sendError(
                        HttpServletResponse.SC_FORBIDDEN,
                        result.getMessage()
                );
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

            response.sendRedirect(
                    request.getContextPath() + "/staffList?error=" + errorCode
            );

        } catch (NumberFormatException e) {
            response.sendRedirect(
                    request.getContextPath() + "/staffList?error=invalid_data"
            );
        }
    }

    // Cập nhật trạng thái nhân viên.
    private void updateStaffStatus(
            HttpServletRequest request,
            HttpServletResponse response,
            User manager) throws IOException {

        try {
            int userId = Integer.parseInt(request.getParameter("userId"));
            String status = request.getParameter("status");

            StaffService.OperationResult result =
                    staffService.updateStaffStatus(manager, userId, status);

            if (result.isSuccess()) {

                if (isAjax(request)) {
                    writeJson(
                            response,
                            HttpServletResponse.SC_OK,
                            "{\"success\":true,\"userId\":" + userId
                                    + ",\"status\":\"" + escapeJson(status)
                                    + "\",\"message\":\"Cập nhật thành công.\"}"
                    );
                    return;
                }

                response.sendRedirect(
                        request.getContextPath() + "/staffList?success=status_updated"
                );
                return;
            }

            int statusCode = switch (result.getStatus()) {
                case INVALID_STATUS -> HttpServletResponse.SC_BAD_REQUEST;
                case NOT_FOUND -> HttpServletResponse.SC_NOT_FOUND;
                case FORBIDDEN -> HttpServletResponse.SC_FORBIDDEN;
                default -> HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            };

            if (isAjax(request)) {
                writeJson(
                        response,
                        statusCode,
                        "{\"success\":false,\"message\":\""
                                + escapeJson(result.getMessage()) + "\"}"
                );
                return;
            }

            response.sendRedirect(
                    request.getContextPath() + "/staffList?error=status_failed"
            );

        } catch (NumberFormatException e) {

            if (isAjax(request)) {
                writeJson(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        "{\"success\":false,\"message\":\"Dữ liệu không hợp lệ.\"}"
                );
                return;
            }

            response.sendRedirect(
                    request.getContextPath() + "/staffList?error=status_failed"
            );
        }
    }
}
