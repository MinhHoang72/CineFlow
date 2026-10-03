package controller.staff;

import dao.CinemaDAO;
import dao.UserDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import model.Branch;
import model.User;

public class StaffServlet extends HttpServlet {
    private final UserDAO userDAO = new UserDAO();
    private final CinemaDAO cinemaDAO = new CinemaDAO();

    private boolean canManage(HttpSession session) {
        Object role = session == null ? null : session.getAttribute("role");
        return "ADMIN".equals(role) || "MANAGER".equals(role);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (!canManage(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập.");
            return;
        }
        User currentUser = (User) session.getAttribute("user");
        Integer branchId = null;
        if ("MANAGER".equals(currentUser.getRole())) {
            branchId = currentUser.getBranchId();
        } else {
            try { if (request.getParameter("branchId") != null) branchId = Integer.valueOf(request.getParameter("branchId")); }
            catch (NumberFormatException ignored) { }
        }
        request.setAttribute("staff", userDAO.getStaffDirectory(branchId));
        List<Branch> branches = cinemaDAO.getActiveBranches();
        if ("MANAGER".equals(currentUser.getRole()) && currentUser.getBranchId() != null) {
            branches.removeIf(branch -> branch.getBranchId() != currentUser.getBranchId());
        }
        request.setAttribute("branches", branches);
        request.setAttribute("selectedBranchId", branchId);
        request.getRequestDispatcher("/staff/directory.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (!canManage(session)) { response.sendError(HttpServletResponse.SC_FORBIDDEN); return; }
        User currentUser = (User) session.getAttribute("user");
        try {
            int userId = Integer.parseInt(request.getParameter("userId"));
            String branchValue = request.getParameter("branchId");
            Integer branchId = branchValue == null || branchValue.isBlank() ? null : Integer.valueOf(branchValue);
            String status = request.getParameter("status");
            if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status) && !"LOCKED".equals(status)) status = "ACTIVE";
            if ("MANAGER".equals(currentUser.getRole())) {
                branchId = currentUser.getBranchId();
            }
            userDAO.updateStaffAssignment(userId, branchId, status);
        } catch (NumberFormatException ignored) { }
        response.sendRedirect(request.getContextPath() + "/staff");
    }
}


