package controller;

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
        if (session == null) return false;
        Object role = session.getAttribute("role");
        if (role == null && session.getAttribute("user") instanceof User) {
            role = ((User) session.getAttribute("user")).getRole();
        }
        return "ADMIN".equalsIgnoreCase((String) role) || "MANAGER".equalsIgnoreCase((String) role);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            String loginUrl = request.getContextPath() + "/auth/login.jsp?redirect=" + java.net.URLEncoder.encode("/staff", "UTF-8");
            response.sendRedirect(loginUrl);
            return;
        }
        if (!canManage(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập quản lý nhân sự.");
            return;
        }
        User currentUser = (User) session.getAttribute("user");
        Integer branchId = null;
        if ("MANAGER".equalsIgnoreCase(currentUser.getRole())) {
            branchId = currentUser.getBranchId();
        } else {
            try {
                if (request.getParameter("branchId") != null && !request.getParameter("branchId").isBlank()) {
                    branchId = Integer.valueOf(request.getParameter("branchId"));
                }
            } catch (NumberFormatException ignored) { }
        }
        request.setAttribute("staff", userDAO.getStaffDirectory(branchId));
        List<Branch> branches = cinemaDAO.getActiveBranches();
        if ("MANAGER".equalsIgnoreCase(currentUser.getRole()) && currentUser.getBranchId() != null) {
            branches.removeIf(branch -> branch.getBranchId() != currentUser.getBranchId());
        }
        request.setAttribute("branches", branches);
        request.setAttribute("selectedBranchId", branchId);
        request.getRequestDispatcher("/staff/directory.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null || !canManage(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền cập nhật nhân sự.");
            return;
        }
        User currentUser = (User) session.getAttribute("user");
        try {
            int userId = Integer.parseInt(request.getParameter("userId"));
            String branchValue = request.getParameter("branchId");
            Integer branchId = branchValue == null || branchValue.isBlank() ? null : Integer.valueOf(branchValue);
            String status = request.getParameter("status");
            if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status) && !"LOCKED".equals(status)) {
                status = "ACTIVE";
            }

            // LAYER C: RÀNG BUỘC PHẠM VI DỮ LIỆU CỦA MANAGER
            if ("MANAGER".equalsIgnoreCase(currentUser.getRole())) {
                User targetUser = userDAO.getUserById(userId);
                if (targetUser == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy tài khoản nhân viên.");
                    return;
                }
                // Manager không được sửa tài khoản Admin hoặc Manager khác
                if (!"STAFF".equalsIgnoreCase(targetUser.getRole())) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Quản lý chỉ được phân công nhân sự cấp nhân viên (STAFF).");
                    return;
                }
                // Manager chỉ được quản lý nhân viên thuộc rạp của mình
                if (targetUser.getBranchId() == null || !targetUser.getBranchId().equals(currentUser.getBranchId())) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn chỉ có quyền quản lý nhân viên tại cụm rạp của mình.");
                    return;
                }
                // Khóa branchId luôn là branchId của Manager
                branchId = currentUser.getBranchId();
            }

            userDAO.updateStaffAssignment(userId, branchId, status);
        } catch (NumberFormatException ignored) { }
        response.sendRedirect(request.getContextPath() + "/staff");
    }
}


