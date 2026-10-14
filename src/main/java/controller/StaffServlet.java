package controller;

import dao.CinemaDAO;
import dao.UserDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import model.Branch;
import model.User;
import utils.PasswordUtils;

public class StaffServlet extends HttpServlet {
    private final UserDAO userDAO = new UserDAO();
    private final CinemaDAO cinemaDAO = new CinemaDAO();

    private boolean canManage(HttpSession session) {
        if (session == null) return false;
        Object role = session.getAttribute("role");
        if (role == null && session.getAttribute("user") instanceof User) {
            role = ((User) session.getAttribute("user")).getRole();
        }
        return "MANAGER".equalsIgnoreCase((String) role) || "ADMIN".equalsIgnoreCase((String) role);
    }

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
            String loginUrl = request.getContextPath() + "/auth/login.jsp?redirect=" + URLEncoder.encode("/staff", StandardCharsets.UTF_8);
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
        Integer branchId = null;

        if (!adminRole) {
            // Manager: Chỉ xem nhân viên rạp mình phụ trách
            branchId = currentUser.getBranchId();
        } else {
            // Admin: Xem theo filter hoặc toàn bộ
            String bp = request.getParameter("branchId");
            if (bp != null && !bp.trim().isEmpty()) {
                try {
                    branchId = Integer.parseInt(bp.trim());
                } catch (NumberFormatException ignored) {}
            }
        }

        String searchParam = request.getParameter("search");
        List<User> staffList = new ArrayList<>();
        if (adminRole || branchId != null) {
            staffList = userDAO.getStaffDirectory(branchId, searchParam);
        }

        List<Branch> branches = cinemaDAO.getActiveBranches();
        Branch assignedBranch = null;
        if (branchId != null) {
            for (Branch b : branches) {
                if (b.getBranchId() == branchId) {
                    assignedBranch = b;
                    break;
                }
            }
        }

        request.setAttribute("staff", staffList);
        request.setAttribute("branches", branches);
        request.setAttribute("assignedBranch", assignedBranch);
        request.setAttribute("selectedBranchId", branchId);
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
            // Manager chỉ được tạo nhân viên thuộc rạp mình phụ trách
            if (!adminRole && currentUser.getBranchId() == null) {
                response.sendRedirect(request.getContextPath() + "/staff?error=unassigned_manager");
                return;
            }

            Integer targetBranchId = currentUser.getBranchId();
            if (adminRole) {
                String bp = request.getParameter("branchId");
                if (bp != null && !bp.trim().isEmpty()) {
                    try {
                        targetBranchId = Integer.parseInt(bp.trim());
                    } catch (NumberFormatException ignored) {}
                }
            }

            String fullName = request.getParameter("fullName");
            String account = request.getParameter("account");
            String email = request.getParameter("email");
            String password = request.getParameter("password");
            String phone = request.getParameter("phone");

            if (fullName == null || fullName.trim().isEmpty()
                    || account == null || account.trim().isEmpty()
                    || email == null || email.trim().isEmpty()
                    || password == null || password.trim().isEmpty()) {
                response.sendRedirect(request.getContextPath() + "/staff?error=missing_fields");
                return;
            }

            account = account.trim();
            email = email.trim();

            if (userDAO.isAccountExists(account)) {
                response.sendRedirect(request.getContextPath() + "/staff?error=account_exists");
                return;
            }

            if (userDAO.isEmailExists(email)) {
                response.sendRedirect(request.getContextPath() + "/staff?error=email_exists");
                return;
            }

            User newStaff = new User();
            newStaff.setFullName(fullName.trim());
            newStaff.setAccount(account);
            newStaff.setEmail(email);
            newStaff.setPassword(PasswordUtils.hashPassword(password.trim()));
            newStaff.setPhone(phone != null ? phone.trim() : "");
            newStaff.setBranchId(targetBranchId);

            boolean created = userDAO.createStaff(newStaff);
            if (created) {
                response.sendRedirect(request.getContextPath() + "/staff?success=created");
            } else {
                response.sendRedirect(request.getContextPath() + "/staff?error=create_failed");
            }
            return;
        }

        // 2. CHỈNH SỬA THÔNG TIN NHÂN VIÊN (FullName, Phone, Email)
        if ("update".equalsIgnoreCase(action)) {
            try {
                int userId = Integer.parseInt(request.getParameter("userId"));
                String fullName = request.getParameter("fullName");
                String phone = request.getParameter("phone");
                String email = request.getParameter("email");

                if (fullName == null || fullName.trim().isEmpty() || email == null || email.trim().isEmpty()) {
                    response.sendRedirect(request.getContextPath() + "/staff?error=missing_fields");
                    return;
                }

                User targetUser = userDAO.getUserById(userId);
                if (targetUser == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy tài khoản nhân viên.");
                    return;
                }

                // Manager chỉ được sửa nhân viên (STAFF)
                if (!"STAFF".equalsIgnoreCase(targetUser.getRole())) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ được chỉnh sửa nhân viên cấp STAFF.");
                    return;
                }

                // BẢO MẬT: Manager KHÔNG ĐƯỢC quản lý Staff thuộc rạp khác
                if (!adminRole) {
                    if (currentUser.getBranchId() == null || targetUser.getBranchId() == null || !currentUser.getBranchId().equals(targetUser.getBranchId())) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền quản lý nhân viên của rạp khác.");
                        return;
                    }
                }

                email = email.trim();
                fullName = fullName.trim();
                phone = phone != null ? phone.trim() : "";

                if (userDAO.isEmailExistsForOtherUser(email, userId)) {
                    response.sendRedirect(request.getContextPath() + "/staff?error=email_exists");
                    return;
                }

                boolean updated = userDAO.updateUserBasicInfo(userId, fullName, phone, email);
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

        // 3. KÍCH HOẠT / NGỪNG HOẠT ĐỘNG (STATUS) NHÂN VIÊN
        if ("status".equalsIgnoreCase(action) || request.getParameter("status") != null) {
            try {
                int userId = Integer.parseInt(request.getParameter("userId"));
                String status = request.getParameter("status");
                if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status) && !"LOCKED".equals(status)) {
                    status = "ACTIVE";
                }

                User targetUser = userDAO.getUserById(userId);
                if (targetUser == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy tài khoản nhân viên.");
                    return;
                }

                // Manager chỉ được đổi trạng thái tài khoản STAFF
                if (!"STAFF".equalsIgnoreCase(targetUser.getRole())) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ được cập nhật trạng thái nhân viên cấp STAFF.");
                    return;
                }

                // BẢO MẬT: Manager KHÔNG ĐƯỢC quản lý Staff thuộc rạp khác
                if (!adminRole) {
                    if (currentUser.getBranchId() == null || targetUser.getBranchId() == null || !currentUser.getBranchId().equals(targetUser.getBranchId())) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền quản lý nhân viên của rạp khác.");
                        return;
                    }
                }

                userDAO.updateUserStatus(userId, status);
                response.sendRedirect(request.getContextPath() + "/staff?success=status_updated");
                return;
            } catch (Exception e) {
                response.sendRedirect(request.getContextPath() + "/staff?error=status_failed");
                return;
            }
        }

        response.sendRedirect(request.getContextPath() + "/staff");
    }
}
