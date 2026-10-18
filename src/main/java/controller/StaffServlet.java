package controller;

import dao.CinemaDAO;
import dao.UserDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Centre;
import model.User;
import utils.PasswordUtils;
import utils.ValidationUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Điều khiển trang quản lý nhân sự cấp cơ sở (STAFF) dành cho Quản lý trung tâm (Manager) và Quản trị viên (Admin).
 */
public class StaffServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final CinemaDAO cinemaDAO = new CinemaDAO();

    /**
     * Kiểm tra người dùng có quyền quản lý nhân sự (MANAGER hoặc ADMIN) hay không.
     */
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

    /**
     * Kiểm tra người dùng có phải Quản trị viên cấp cao (ADMIN) hay không.
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
     * Kiểm tra yêu cầu gửi đến có phải là AJAX hay không.
     */
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
            String loginUrl = request.getContextPath() + "/auth/login.jsp?redirect="
                    + URLEncoder.encode("/staff", StandardCharsets.UTF_8);
            response.sendRedirect(loginUrl);
            return;
        }
        if (!canManage(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập quản lý nhân sự.");
            return;
        }

        int sessionUserId = (int) session.getAttribute("userId");
        User currentUser = userDAO.getUserById(sessionUserId);
        if (currentUser == null) {
            currentUser = (User) session.getAttribute("user");
        } else {
            session.setAttribute("user", currentUser);
        }

        boolean adminRole = isAdmin(session);
        Integer centreId = null;

        if (!adminRole) {
            // Manager: Chỉ xem nhân viên rạp mình phụ trách
            centreId = currentUser.getCentreId();
        } else {
            // Admin: Xem theo bộ lọc trung tâm hoặc toàn bộ hệ thống
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
        List<User> staffList = new ArrayList<>();
        if (adminRole || centreId != null) {
            staffList = userDAO.getStaffDirectory(centreId, searchParam, roleFilter);
        }

        List<Centre> centres = cinemaDAO.getActiveCentres();
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
        request.getRequestDispatcher("/staff/directory.jsp").forward(request, response);
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
        User currentUser = userDAO.getUserById(sessionUserId);
        if (currentUser == null) {
            currentUser = (User) session.getAttribute("user");
        }
        boolean adminRole = isAdmin(session);

        String action = request.getParameter("action");

        // 1. TẠO TÀI KHOẢN NHÂN VIÊN MỚI
        if ("create".equalsIgnoreCase(action)) {
            Integer targetCentreId = null;
            if (!adminRole) {
                // Manager chỉ được tạo nhân viên thuộc rạp mình phụ trách
                if (currentUser.getCentreId() == null || currentUser.getCentreId() <= 0) {
                    response.sendRedirect(request.getContextPath() + "/staff?error=unassigned_manager");
                    return;
                }
                targetCentreId = currentUser.getCentreId();
            } else {
                // Admin chọn trung tâm trực thuộc cho nhân viên
                String cp = request.getParameter("centreId");
                if (ValidationUtils.isBlank(cp)) {
                    response.sendRedirect(request.getContextPath() + "/staff?error=missing_centre");
                    return;
                }
                try {
                    targetCentreId = Integer.parseInt(cp.trim());
                } catch (NumberFormatException e) {
                    response.sendRedirect(request.getContextPath() + "/staff?error=invalid_centre");
                    return;
                }
                Centre targetCentre = cinemaDAO.getCentreById(targetCentreId);
                if (targetCentre == null) {
                    response.sendRedirect(request.getContextPath() + "/staff?error=invalid_centre");
                    return;
                }
            }

            String fullName = request.getParameter("fullName");
            String account = request.getParameter("account");
            String email = request.getParameter("email");
            String password = request.getParameter("password");
            String phone = request.getParameter("phone");

            if (ValidationUtils.isBlank(fullName)
                    || ValidationUtils.isBlank(account)
                    || ValidationUtils.isBlank(email)
                    || ValidationUtils.isBlank(password)) {
                response.sendRedirect(request.getContextPath() + "/staff?error=missing_fields");
                return;
            }

            fullName = fullName.trim();
            account = account.trim();
            email = email.trim();
            phone = phone != null ? phone.trim() : "";

            if (!ValidationUtils.isValidFullName(fullName)) {
                response.sendRedirect(request.getContextPath() + "/staff?error=invalid_name");
                return;
            }

            if (!ValidationUtils.isValidAccount(account)) {
                response.sendRedirect(request.getContextPath() + "/staff?error=invalid_account");
                return;
            }

            if (!ValidationUtils.isValidEmail(email)) {
                response.sendRedirect(request.getContextPath() + "/staff?error=invalid_email");
                return;
            }

            if (ValidationUtils.isDisposableEmail(email)) {
                response.sendRedirect(request.getContextPath() + "/staff?error=disposable_email");
                return;
            }

            if (!ValidationUtils.isValidPassword(password)) {
                response.sendRedirect(request.getContextPath() + "/staff?error=weak_password");
                return;
            }

            if (!ValidationUtils.isValidPhone(phone, false)) {
                response.sendRedirect(request.getContextPath() + "/staff?error=invalid_phone");
                return;
            }

            if (userDAO.isAccountExists(account)) {
                response.sendRedirect(request.getContextPath() + "/staff?error=account_exists");
                return;
            }

            if (userDAO.isEmailExists(email)) {
                response.sendRedirect(request.getContextPath() + "/staff?error=email_exists");
                return;
            }

            String role = request.getParameter("role");
            if (role == null || role.isBlank() || !ValidationUtils.isStaffRole(role)) {
                role = User.ROLE_TICKET_SALES;
            } else {
                role = role.trim().toUpperCase();
            }

            User newStaff = new User();
            newStaff.setFullName(fullName);
            newStaff.setAccount(account);
            newStaff.setEmail(email);
            newStaff.setPassword(PasswordUtils.hashPassword(password.trim()));
            newStaff.setPhone(phone);
            newStaff.setRole(role);
            newStaff.setCentreId(targetCentreId);

            boolean created = userDAO.createStaff(newStaff);
            if (created) {
                response.sendRedirect(request.getContextPath() + "/staff?success=created");
            } else {
                response.sendRedirect(request.getContextPath() + "/staff?error=create_failed");
            }
            return;
        }

        // 2. CHỈNH SỬA THÔNG TIN NHÂN VIÊN (FullName, Phone, Email, Role)
        if ("update".equalsIgnoreCase(action)) {
            try {
                int userId = Integer.parseInt(request.getParameter("userId"));
                String fullName = request.getParameter("fullName");
                String phone = request.getParameter("phone");
                String email = request.getParameter("email");
                String role = request.getParameter("role");

                if (ValidationUtils.isBlank(fullName) || ValidationUtils.isBlank(email)) {
                    response.sendRedirect(request.getContextPath() + "/staff?error=missing_fields");
                    return;
                }

                fullName = fullName.trim();
                email = email.trim();
                phone = phone != null ? phone.trim() : "";

                if (!ValidationUtils.isValidFullName(fullName)) {
                    response.sendRedirect(request.getContextPath() + "/staff?error=invalid_name");
                    return;
                }

                if (!ValidationUtils.isValidEmail(email)) {
                    response.sendRedirect(request.getContextPath() + "/staff?error=invalid_email");
                    return;
                }

                if (ValidationUtils.isDisposableEmail(email)) {
                    response.sendRedirect(request.getContextPath() + "/staff?error=disposable_email");
                    return;
                }

                if (!ValidationUtils.isValidPhone(phone, false)) {
                    response.sendRedirect(request.getContextPath() + "/staff?error=invalid_phone");
                    return;
                }

                User targetUser = userDAO.getUserById(userId);
                if (targetUser == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy tài khoản nhân viên.");
                    return;
                }

                // Manager chỉ được sửa nhân viên cấp rạp (STAFF, TICKET_SALES_STAFF, TICKET_CHECKER_STAFF)
                if (!targetUser.isStaff()) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ được chỉnh sửa nhân viên cấp rạp.");
                    return;
                }

                if (role == null || role.isBlank() || !ValidationUtils.isStaffRole(role)) {
                    role = targetUser.getRole();
                } else {
                    role = role.trim().toUpperCase();
                }

                // Manager không được quản lý Staff thuộc rạp khác
                if (!adminRole) {
                    if (currentUser.getCentreId() == null || targetUser.getCentreId() == null
                            || !currentUser.getCentreId().equals(targetUser.getCentreId())) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền quản lý nhân viên của rạp khác.");
                        return;
                    }
                }

                if (userDAO.isEmailExistsForOtherUser(email, userId)) {
                    response.sendRedirect(request.getContextPath() + "/staff?error=email_exists");
                    return;
                }

                boolean updated = userDAO.updateStaffBasicInfo(userId, fullName, phone, email, role);
                if (updated) {
                    response.sendRedirect(request.getContextPath() + "/staff?success=updated");
                } else {
                    response.sendRedirect(request.getContextPath() + "/staff?error=update_failed");
                }
                return;
            } catch (Exception e) {
                response.sendRedirect(request.getContextPath() + "/staff?error=invalid_data");
                return;
            }
        }

        // 3. KÍCH HOẠT HOẶC NGỪNG HOẠT ĐỘNG (STATUS) NHÂN VIÊN
        if ("status".equalsIgnoreCase(action) || request.getParameter("status") != null) {
            try {
                int userId = Integer.parseInt(request.getParameter("userId"));
                String status = request.getParameter("status");
                if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status) && !"LOCKED".equals(status)) {
                    status = "ACTIVE";
                }

                User targetUser = userDAO.getUserById(userId);
                if (targetUser == null) {
                    if (isAjax(request)) {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"success\":false,\"message\":\"Không tìm thấy tài khoản nhân viên.\"}");
                        return;
                    }
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy tài khoản nhân viên.");
                    return;
                }

                // Manager chỉ được đổi trạng thái tài khoản nhân viên cấp rạp
                if (!targetUser.isStaff()) {
                    if (isAjax(request)) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"success\":false,\"message\":\"Chỉ được cập nhật trạng thái nhân viên cấp rạp.\"}");
                        return;
                    }
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ được cập nhật trạng thái nhân viên cấp rạp.");
                    return;
                }

                // Manager không được quản lý Staff thuộc rạp khác
                if (!adminRole) {
                    if (currentUser.getCentreId() == null || targetUser.getCentreId() == null
                            || !currentUser.getCentreId().equals(targetUser.getCentreId())) {
                        if (isAjax(request)) {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"success\":false,\"message\":\"Bạn không có quyền quản lý nhân viên của rạp khác.\"}");
                            return;
                        }
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền quản lý nhân viên của rạp khác.");
                        return;
                    }
                }

                userDAO.updateUserStatus(userId, status);
                if (isAjax(request)) {
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"success\":true,\"userId\":" + userId + ",\"status\":\"" + status + "\",\"message\":\"Đã cập nhật trạng thái thành công.\"}");
                    return;
                }
                response.sendRedirect(request.getContextPath() + "/staff?success=status_updated");
                return;
            } catch (Exception e) {
                if (isAjax(request)) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"success\":false,\"message\":\"Dữ liệu không hợp lệ hoặc lỗi xử lý.\"}");
                    return;
                }
                response.sendRedirect(request.getContextPath() + "/staff?error=status_failed");
                return;
            }
        }

        response.sendRedirect(request.getContextPath() + "/staff");
    }
}
