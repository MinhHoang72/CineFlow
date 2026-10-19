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
import model.Centre;
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

        Integer centreFilter = null;
        String centreParam = request.getParameter("centreId");
        if (centreParam != null && !centreParam.trim().isEmpty()) {
            try {
                centreFilter = Integer.parseInt(centreParam.trim());
            } catch (NumberFormatException ignored) {}
        }

        String searchParam = request.getParameter("search");
        List<Centre> centresWithManager = cinemaDAO.getAllCentresWithManager();
        List<Centre> activeCentres = cinemaDAO.getActiveCentres();
        List<User> managers = userDAO.getManagers();
        List<ManagerAssignment> managerAssignments = userDAO.getManagerAssignmentHistory(centreFilter, null);

        List<User> staffList = userDAO.getStaffDirectory(centreFilter, searchParam);
        List<StaffTransfer> transferHistory = userDAO.getTransferHistory(centreFilter, null);

        request.setAttribute("centresWithManager", centresWithManager);
        request.setAttribute("activeCentres", activeCentres);
        request.setAttribute("selectedCentreId", centreFilter);

        request.setAttribute("managers", managers);
        request.setAttribute("managerAssignments", managerAssignments);
        request.setAttribute("staffList", staffList);
        request.setAttribute("transferHistory", transferHistory);
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

        // 1. NGHIỆP VỤ: BỔ NHIỆM / THAY THẾ MANAGER TRUNG TÂM (CENTRE)
        if ("assign_manager".equalsIgnoreCase(action)) {
            try {
                String cParam = request.getParameter("centreId");
                int centreId = Integer.parseInt(cParam);
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

                boolean ok = userDAO.assignManagerToCentreWithHistory(centreId, managerId, adminId, notes);
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
                String cParam = request.getParameter("centreId");
                int centreId = Integer.parseInt(cParam);
                int managerId = Integer.parseInt(request.getParameter("managerId"));
                String reason = request.getParameter("reason");

                User targetManager = userDAO.getUserById(managerId);
                if (targetManager == null || !"MANAGER".equalsIgnoreCase(targetManager.getRole())) {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?error=not_manager&tab=manager");
                    return;
                }

                boolean ok = userDAO.unassignManager(centreId, managerId, adminId, reason);
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

        // 3. NGHIỆP VỤ: ĐIỀU CHUYỂN STAFF GIỮA CÁC TRUNG TÂM (CENTRES)
        if ("transfer_staff".equalsIgnoreCase(action) || "transfer".equalsIgnoreCase(action)) {
            try {
                int userId = Integer.parseInt(request.getParameter("userId"));
                String toParam = request.getParameter("toCentreId");
                int toCentreId = Integer.parseInt(toParam);
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

                if (targetUser.getCentreId() != null && targetUser.getCentreId() == toCentreId) {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?error=same_centre&tab=staff");
                    return;
                }

                boolean ok = userDAO.transferStaff(userId, targetUser.getCentreId(), toCentreId, adminId, reason.trim(), notes != null ? notes.trim() : "");
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
