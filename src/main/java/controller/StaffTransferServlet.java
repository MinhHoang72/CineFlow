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
import java.util.List;
import model.Branch;
import model.ManagerAssignment;
import model.StaffTransfer;
import model.User;

public class StaffTransferServlet extends HttpServlet {
    private final UserDAO userDAO = new UserDAO();
    private final CinemaDAO cinemaDAO = new CinemaDAO();

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
            String loginUrl = request.getContextPath() + "/auth/login.jsp?redirect=" + URLEncoder.encode("/admin/transfers", StandardCharsets.UTF_8);
            response.sendRedirect(loginUrl);
            return;
        }
        if (!isAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập quản lý điều chuyển nhân sự.");
            return;
        }

        Integer branchFilter = null;
        String branchParam = request.getParameter("branchId");
        if (branchParam != null && !branchParam.trim().isEmpty()) {
            try {
                branchFilter = Integer.parseInt(branchParam.trim());
            } catch (NumberFormatException ignored) {}
        }

        String searchParam = request.getParameter("search");
        List<Branch> branchesWithManager = cinemaDAO.getAllBranchesWithManager();
        List<Branch> activeBranches = cinemaDAO.getActiveBranches();
        List<User> managers = userDAO.getManagers();
        List<ManagerAssignment> managerAssignments = userDAO.getManagerAssignmentHistory(branchFilter, null);

        List<User> staffList = userDAO.getStaffDirectory(branchFilter, searchParam);
        List<StaffTransfer> transferHistory = userDAO.getTransferHistory(branchFilter, null);

        request.setAttribute("branchesWithManager", branchesWithManager);
        request.setAttribute("activeBranches", activeBranches);
        request.setAttribute("managers", managers);
        request.setAttribute("managerAssignments", managerAssignments);
        request.setAttribute("staffList", staffList);
        request.setAttribute("transferHistory", transferHistory);
        request.setAttribute("selectedBranchId", branchFilter);
        request.setAttribute("searchParam", searchParam);
        request.setAttribute("activeNav", "transfers");

        request.getRequestDispatcher("/admin/transfers.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null || !isAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền thực hiện thao tác này.");
            return;
        }

        User adminUser = (User) session.getAttribute("user");
        int adminId = adminUser != null ? adminUser.getUserId() : (int) session.getAttribute("userId");
        String action = request.getParameter("action");

        // 1. NGHIỆP VỤ: BỔ NHIỆM / THAY THẾ MANAGER CHI NHÁNH
        if ("assign_manager".equalsIgnoreCase(action)) {
            try {
                int branchId = Integer.parseInt(request.getParameter("branchId"));
                int managerId = Integer.parseInt(request.getParameter("managerId"));
                String notes = request.getParameter("notes");

                User targetManager = userDAO.getUserById(managerId);
                if (targetManager == null || !"MANAGER".equalsIgnoreCase(targetManager.getRole())) {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?error=not_manager&tab=manager");
                    return;
                }
                if (!"ACTIVE".equalsIgnoreCase(targetManager.getStatus())) {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?error=manager_inactive&tab=manager");
                    return;
                }

                boolean ok = userDAO.assignManagerToBranchWithHistory(branchId, managerId, adminId, notes);
                if (ok) {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?success=manager_assigned&tab=manager");
                } else {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?error=assign_failed&tab=manager");
                }
                return;
            } catch (Exception e) {
                response.sendRedirect(request.getContextPath() + "/admin/transfers?error=invalid_data&tab=manager");
                return;
            }
        }

        // 2. NGHIỆP VỤ: MIỄN NHIỆM MANAGER
        if ("unassign_manager".equalsIgnoreCase(action)) {
            try {
                int branchId = Integer.parseInt(request.getParameter("branchId"));
                int managerId = Integer.parseInt(request.getParameter("managerId"));
                String reason = request.getParameter("reason");

                User targetManager = userDAO.getUserById(managerId);
                if (targetManager == null || !"MANAGER".equalsIgnoreCase(targetManager.getRole())) {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?error=not_manager&tab=manager");
                    return;
                }

                boolean ok = userDAO.unassignManager(branchId, managerId, adminId, reason);
                if (ok) {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?success=manager_unassigned&tab=manager");
                } else {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?error=unassign_failed&tab=manager");
                }
                return;
            } catch (Exception e) {
                response.sendRedirect(request.getContextPath() + "/admin/transfers?error=invalid_data&tab=manager");
                return;
            }
        }

        // 3. NGHIỆP VỤ: ĐIỀU CHUYỂN STAFF GIỮA CÁC CHI NHÁNH
        if ("transfer_staff".equalsIgnoreCase(action) || "transfer".equalsIgnoreCase(action)) {
            try {
                int userId = Integer.parseInt(request.getParameter("userId"));
                int toBranchId = Integer.parseInt(request.getParameter("toBranchId"));
                String reason = request.getParameter("reason");
                String notes = request.getParameter("notes");

                if (reason == null || reason.trim().isEmpty()) {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?error=missing_reason&tab=staff");
                    return;
                }

                User targetUser = userDAO.getUserById(userId);
                if (targetUser == null) {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?error=user_not_found&tab=staff");
                    return;
                }

                if (!"STAFF".equalsIgnoreCase(targetUser.getRole())) {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?error=not_staff&tab=staff");
                    return;
                }

                if (targetUser.getBranchId() != null && targetUser.getBranchId() == toBranchId) {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?error=same_branch&tab=staff");
                    return;
                }

                boolean ok = userDAO.transferStaff(userId, targetUser.getBranchId(), toBranchId, adminId, reason.trim(), notes != null ? notes.trim() : "");
                if (ok) {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?success=transferred&tab=staff");
                } else {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?error=transfer_failed&tab=staff");
                }
                return;
            } catch (Exception e) {
                response.sendRedirect(request.getContextPath() + "/admin/transfers?error=invalid_data&tab=staff");
                return;
            }
        }

        response.sendRedirect(request.getContextPath() + "/admin/transfers");
    }
}
