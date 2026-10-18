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
import java.util.List;

/**
 * Điều khiển màn hình Quản lý Tài khoản Quản lý (Manager).
 * Trách nhiệm: Quản lý thông tin tài khoản Manager (tạo, cập nhật thông tin, kích hoạt hoặc tạm ngưng).
 * Nghiệp vụ Bổ nhiệm vào rạp và Điều chuyển nhân sự được quản lý riêng biệt tại StaffTransferServlet (/admin/transfers).
 */
public class ManagerAccountServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final CinemaDAO cinemaDAO = new CinemaDAO();

    /**
     * Kiểm tra phiên hiện tại có vai trò Quản trị viên (Admin) hay không.
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

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            String loginUrl = request.getContextPath() + "/auth/login.jsp?redirect="
                    + URLEncoder.encode("/admin/managers", StandardCharsets.UTF_8);
            response.sendRedirect(loginUrl);
            return;
        }
        if (!isAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập quản lý tài khoản Manager.");
            return;
        }

        userDAO.syncLockedManagersAssignments();

        String searchParam = request.getParameter("search");
        List<User> managers = userDAO.getManagers(searchParam);
        List<Centre> centres = cinemaDAO.getCentresWithoutActiveManager();

        request.setAttribute("managers", managers);
        request.setAttribute("centres", centres);
        request.setAttribute("searchParam", searchParam);
        request.setAttribute("activeNav", "managers");
        request.getRequestDispatcher("/admin/managers.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null || !isAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền thực hiện thao tác này.");
            return;
        }

        String action = request.getParameter("action");

        // 1. TẠO TÀI KHOẢN MANAGER MỚI
        if ("create".equalsIgnoreCase(action)) {
            String fullName = request.getParameter("fullName");
            String account = request.getParameter("account");
            String email = request.getParameter("email");
            String password = request.getParameter("password");
            String phone = request.getParameter("phone");
            String centreIdParam = request.getParameter("centreId");

            if (ValidationUtils.isBlank(fullName)
                    || ValidationUtils.isBlank(account)
                    || ValidationUtils.isBlank(email)
                    || ValidationUtils.isBlank(password)
                    || ValidationUtils.isBlank(centreIdParam)) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=missing_fields");
                return;
            }

            int centreId;
            try {
                centreId = Integer.parseInt(centreIdParam.trim());
            } catch (NumberFormatException e) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=invalid_centre");
                return;
            }

            if (cinemaDAO.hasActiveManager(centreId)) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=centre_has_manager");
                return;
            }

            fullName = fullName.trim();
            account = account.trim();
            email = email.trim();
            phone = phone != null ? phone.trim() : "";

            if (!ValidationUtils.isValidFullName(fullName)) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=invalid_name");
                return;
            }

            if (!ValidationUtils.isValidAccount(account)) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=invalid_account");
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

            if (!ValidationUtils.isValidPassword(password)) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=weak_password");
                return;
            }

            if (!ValidationUtils.isValidPhone(phone, false)) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=invalid_phone");
                return;
            }

            if (userDAO.isAccountExists(account)) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=account_exists");
                return;
            }

            if (userDAO.isEmailExists(email)) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=email_exists");
                return;
            }

            int sessionUserId = (int) session.getAttribute("userId");
            User newManager = new User();
            newManager.setFullName(fullName);
            newManager.setAccount(account);
            newManager.setEmail(email);
            newManager.setPassword(PasswordUtils.hashPassword(password.trim()));
            newManager.setPhone(phone);
            newManager.setCentreId(centreId);

            int createdId = userDAO.createManager(newManager, sessionUserId);
            if (createdId > 0) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?success=created");
            } else {
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=create_failed");
            }
            return;
        }

        // 2. CHỈNH SỬA THÔNG TIN CÁ NHÂN MANAGER
        if ("update".equalsIgnoreCase(action)) {
            try {
                int userId = Integer.parseInt(request.getParameter("userId"));
                String fullName = request.getParameter("fullName");
                String phone = request.getParameter("phone");
                String email = request.getParameter("email");

                if (ValidationUtils.isBlank(fullName) || ValidationUtils.isBlank(email)) {
                    response.sendRedirect(request.getContextPath() + "/admin/managers?error=missing_fields");
                    return;
                }

                fullName = fullName.trim();
                email = email.trim();
                phone = phone != null ? phone.trim() : "";

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

                User target = userDAO.getUserById(userId);
                if (target == null || !"MANAGER".equalsIgnoreCase(target.getRole())) {
                    response.sendRedirect(request.getContextPath() + "/admin/managers?error=not_manager");
                    return;
                }

                if (userDAO.isEmailExistsForOtherUser(email, userId)) {
                    response.sendRedirect(request.getContextPath() + "/admin/managers?error=email_exists");
                    return;
                }

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

        // 3. KÍCH HOẠT HOẶC TẠM NGƯNG TÀI KHOẢN MANAGER
        if ("toggle_status".equalsIgnoreCase(action)) {
            try {
                int userId = Integer.parseInt(request.getParameter("userId"));
                String status = request.getParameter("status");
                if (!ValidationUtils.isValidUserStatus(status)) {
                    status = "ACTIVE";
                }

                User target = userDAO.getUserById(userId);
                if (target == null || !"MANAGER".equalsIgnoreCase(target.getRole())) {
                    response.sendRedirect(request.getContextPath() + "/admin/managers?error=not_manager");
                    return;
                }

                // Cập nhật trạng thái tài khoản User.
                String newStatus = status.toUpperCase();
                userDAO.updateUserStatus(userId, newStatus);
                if (!"ACTIVE".equalsIgnoreCase(newStatus)) {
                    // Khi khóa/tạm ngưng tài khoản Manager, giải phóng cụm rạp để rạp không bị vô chủ
                    int sessionAdminId = session.getAttribute("userId") != null ? (int) session.getAttribute("userId") : 0;
                    userDAO.unassignManagerByUserId(userId, sessionAdminId, "Tài khoản Quản lý bị khóa/tạm ngưng");
                }

                // Luôn đồng bộ lại phân công và giải phóng CentreID nếu Quản lý không có rạp hợp lệ
                userDAO.syncLockedManagersAssignments();

                response.sendRedirect(request.getContextPath() + "/admin/managers?success=status_updated");
                return;
            } catch (Exception e) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=status_failed");
                return;
            }
        }

        response.sendRedirect(request.getContextPath() + "/admin/managers");
    }
}
