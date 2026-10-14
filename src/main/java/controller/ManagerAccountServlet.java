package controller;

import dao.UserDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import model.User;
import utils.PasswordUtils;

/**
 * Màn hình Quản lý Tài khoản Quản lý (Manager).
 * Trách nhiệm DUY NHẤT: Quản lý thông tin tài khoản Manager (tạo, sửa thông tin, kích hoạt/tạm ngưng).
 * Nghiệp vụ Bổ nhiệm vào rạp và Điều chuyển nhân sự được quản lý riêng biệt tại StaffTransferServlet (/admin/transfers).
 */
public class ManagerAccountServlet extends HttpServlet {
    private final UserDAO userDAO = new UserDAO();

    private boolean isAdmin(HttpSession session) {
        if (session == null) return false;
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
            String loginUrl = request.getContextPath() + "/auth/login.jsp?redirect=" + URLEncoder.encode("/admin/managers", StandardCharsets.UTF_8);
            response.sendRedirect(loginUrl);
            return;
        }
        if (!isAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập quản lý tài khoản Manager.");
            return;
        }

        String searchParam = request.getParameter("search");
        List<User> managers = userDAO.getManagers(searchParam);

        request.setAttribute("managers", managers);
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

            if (fullName == null || fullName.trim().isEmpty()
                    || account == null || account.trim().isEmpty()
                    || email == null || email.trim().isEmpty()
                    || password == null || password.trim().isEmpty()) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=missing_fields");
                return;
            }

            account = account.trim();
            email = email.trim();

            if (userDAO.isAccountExists(account)) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=account_exists");
                return;
            }

            if (userDAO.isEmailExists(email)) {
                response.sendRedirect(request.getContextPath() + "/admin/managers?error=email_exists");
                return;
            }

            User newManager = new User();
            newManager.setFullName(fullName.trim());
            newManager.setAccount(account);
            newManager.setEmail(email);
            newManager.setPassword(PasswordUtils.hashPassword(password.trim()));
            newManager.setPhone(phone != null ? phone.trim() : "");
            newManager.setBranchId(null); // Manager mới tạo chưa phân công rạp

            int createdId = userDAO.createManager(newManager);
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

                if (fullName == null || fullName.trim().isEmpty() || email == null || email.trim().isEmpty()) {
                    response.sendRedirect(request.getContextPath() + "/admin/managers?error=missing_fields");
                    return;
                }

                User target = userDAO.getUserById(userId);
                if (target == null || !"MANAGER".equalsIgnoreCase(target.getRole())) {
                    response.sendRedirect(request.getContextPath() + "/admin/managers?error=not_manager");
                    return;
                }

                email = email.trim();
                fullName = fullName.trim();
                phone = phone != null ? phone.trim() : "";

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

        // 3. KÍCH HOẠT / TẠM NGƯNG TÀI KHOẢN MANAGER
        if ("toggle_status".equalsIgnoreCase(action)) {
            try {
                int userId = Integer.parseInt(request.getParameter("userId"));
                String status = request.getParameter("status");
                if (!"ACTIVE".equalsIgnoreCase(status) && !"INACTIVE".equalsIgnoreCase(status) && !"LOCKED".equalsIgnoreCase(status)) {
                    status = "ACTIVE";
                }

                User target = userDAO.getUserById(userId);
                if (target == null || !"MANAGER".equalsIgnoreCase(target.getRole())) {
                    response.sendRedirect(request.getContextPath() + "/admin/managers?error=not_manager");
                    return;
                }

                // Cập nhật trạng thái tài khoản User.
                // Lưu ý nghiệp vụ: Nếu Manager đang phụ trách rạp mà tài khoản bị tạm ngưng,
                // chỉ đổi trạng thái tài khoản người dùng, tuyệt đối không tự động vô hiệu hóa rạp.
                userDAO.updateUserStatus(userId, status.toUpperCase());
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
