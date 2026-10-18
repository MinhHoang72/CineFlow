package controller;

import dao.CinemaDAO;
import dao.UserDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Centre;
import model.ManagerAssignment;
import model.StaffTransfer;
import model.User;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Điều khiển nghiệp vụ điều chuyển nhân viên và bổ nhiệm, miễn nhiệm Quản lý trung tâm dành cho Quản trị viên (Admin).
 */
public class StaffTransferServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final CinemaDAO cinemaDAO = new CinemaDAO();

    /**
     * Kiểm tra phiên người dùng có vai trò Quản trị viên (Admin) hay không.
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
                    + URLEncoder.encode("/admin/transfers", StandardCharsets.UTF_8);
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
            } catch (NumberFormatException ignored) {
            }
        }

        String searchParam = request.getParameter("search");
        String roleFilter = request.getParameter("role");

        // Đồng bộ và giải phóng trung tâm của các Quản lý có tài khoản bị khóa/tạm ngưng
        userDAO.syncLockedManagersAssignments();

        List<Centre> centresWithManager = cinemaDAO.getAllCentresWithManager();
        List<Centre> activeCentres = cinemaDAO.getActiveCentres();
        List<User> managers = userDAO.getManagers();
        List<ManagerAssignment> managerAssignments = userDAO.getManagerAssignmentHistory(centreFilter, null);

        // Lấy danh sách toàn bộ nhân sự có thể điều chuyển (Staff bán vé & soát vé)
        List<User> staffList = userDAO.getTransferableUsers(centreFilter, searchParam, roleFilter);
        List<StaffTransfer> transferHistory = userDAO.getTransferHistory(centreFilter, null);

        // Tính toán các chỉ số KPI vận hành nhân sự
        int totalCentres = centresWithManager != null ? centresWithManager.size() : 0;
        int assignedCentres = 0;
        int unassignedCentres = 0;
        if (centresWithManager != null) {
            for (Centre c : centresWithManager) {
                if (c.hasActiveManager()) {
                    assignedCentres++;
                } else if ("ACTIVE".equalsIgnoreCase(c.getStatus())) {
                    unassignedCentres++;
                }
            }
        }

        int unassignedManagers = 0;
        if (managers != null) {
            for (User m : managers) {
                if ("ACTIVE".equalsIgnoreCase(m.getStatus()) && (m.getCentreId() == null || m.getCentreId() <= 0)) {
                    unassignedManagers++;
                }
            }
        }

        request.setAttribute("kpiTotalCentres", totalCentres);
        request.setAttribute("kpiAssignedCentres", assignedCentres);
        request.setAttribute("kpiUnassignedCentres", unassignedCentres);
        request.setAttribute("kpiUnassignedManagers", unassignedManagers);

        request.setAttribute("centresWithManager", centresWithManager);
        request.setAttribute("activeCentres", activeCentres);
        request.setAttribute("selectedCentreId", centreFilter);

        request.setAttribute("managers", managers);
        request.setAttribute("managerAssignments", managerAssignments);
        request.setAttribute("staffList", staffList);
        request.setAttribute("transferHistory", transferHistory);
        request.setAttribute("searchParam", searchParam);
        request.setAttribute("roleFilter", roleFilter);
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

        // 1. NGHIỆP VỤ: BỔ NHIỆM MANAGER TRUNG TÂM (CHỈ DÀNH CHO MANAGER CHƯA CÓ PHÂN CÔNG)
        if ("assign_manager".equalsIgnoreCase(action)) {
            try {
                String cParam = request.getParameter("centreId");
                int centreId = Integer.parseInt(cParam);
                int managerId = Integer.parseInt(request.getParameter("managerId"));
                String notes = request.getParameter("notes");

                String result = userDAO.assignManager(centreId, managerId, adminId, notes);
                if ("SUCCESS".equals(result)) {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?success=manager_assigned&tab=manager");
                } else if ("MANAGER_ALREADY_ASSIGNED".equals(result)) {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?error=manager_already_assigned&tab=manager");
                } else if ("CENTRE_ALREADY_HAS_MANAGER".equals(result)) {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?error=centre_already_has_manager&tab=manager");
                } else if ("MANAGER_NOT_ACTIVE".equals(result)) {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?error=manager_inactive&tab=manager");
                } else if ("NOT_A_MANAGER".equals(result)) {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?error=not_manager&tab=manager");
                } else if ("CENTRE_INACTIVE".equals(result)) {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?error=centre_inactive&tab=manager");
                } else if ("CONCURRENCY_CONFLICT".equals(result)) {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?error=concurrency_conflict&tab=manager");
                } else {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?error=assign_failed&tab=manager");
                }
                return;
            } catch (Exception e) {
                response.sendRedirect(request.getContextPath() + "/admin/transfers?error=invalid_data&tab=manager");
                return;
            }
        }

        // 2. PHẠM VI KHÔNG THỰC HIỆN: MIỄN NHIỆM MANAGER TRONG ĐỢT 1
        if ("unassign_manager".equalsIgnoreCase(action)) {
            response.sendRedirect(request.getContextPath() + "/admin/transfers?error=unassign_not_supported&tab=manager");
            return;
        }

        // 3. NGHIỆP VỤ: ĐIỀU CHUYỂN NHÂN SỰ / MANAGER GIỮA CÁC TRUNG TÂM
        if ("transfer_staff".equalsIgnoreCase(action) || "transfer".equalsIgnoreCase(action) || "transfer_manager".equalsIgnoreCase(action)) {
            try {
                int userId = Integer.parseInt(request.getParameter("userId"));
                String toParam = request.getParameter("toCentreId");
                int toCentreId = Integer.parseInt(toParam);
                String reason = request.getParameter("reason");
                String notes = request.getParameter("notes");
                String effectiveDateParam = request.getParameter("effectiveDate");

                if (reason == null || reason.trim().isEmpty()) {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?error=missing_reason&tab=staff");
                    return;
                }

                User targetUser = userDAO.getUserById(userId);
                if (targetUser == null) {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?error=user_not_found&tab=staff");
                    return;
                }

                // Nếu là Manager: Thực hiện nghiệp vụ Điều chuyển Manager với ràng buộc nghiêm ngặt
                if ("MANAGER".equalsIgnoreCase(targetUser.getRole())) {
                    String result = userDAO.transferManager(userId, toCentreId, adminId, reason.trim(), notes != null ? notes.trim() : "");
                    if ("SUCCESS".equals(result)) {
                        response.sendRedirect(request.getContextPath() + "/admin/transfers?success=transferred&tab=staff");
                    } else if ("SAME_CENTRE".equals(result)) {
                        response.sendRedirect(request.getContextPath() + "/admin/transfers?error=same_centre&tab=staff");
                    } else if ("CENTRE_ALREADY_HAS_MANAGER".equals(result)) {
                        response.sendRedirect(request.getContextPath() + "/admin/transfers?error=dest_centre_has_manager&tab=staff");
                    } else if ("MANAGER_NOT_ACTIVE".equals(result)) {
                        response.sendRedirect(request.getContextPath() + "/admin/transfers?error=user_inactive&tab=staff");
                    } else if ("MANAGER_NOT_ASSIGNED".equals(result)) {
                        response.sendRedirect(request.getContextPath() + "/admin/transfers?error=manager_not_assigned&tab=staff");
                    } else if ("NOT_A_MANAGER".equals(result)) {
                        response.sendRedirect(request.getContextPath() + "/admin/transfers?error=not_manager&tab=staff");
                    } else if ("CENTRE_INACTIVE".equals(result)) {
                        response.sendRedirect(request.getContextPath() + "/admin/transfers?error=centre_inactive&tab=staff");
                    } else if ("CONCURRENCY_CONFLICT".equals(result)) {
                        response.sendRedirect(request.getContextPath() + "/admin/transfers?error=concurrency_conflict&tab=staff");
                    } else {
                        response.sendRedirect(request.getContextPath() + "/admin/transfers?error=transfer_failed&tab=staff");
                    }
                    return;
                }

                // Nếu là Staff thông thường (Rule 12): Thực hiện điều chuyển Staff thông thường
                java.sql.Date effectiveDate = null;
                if (effectiveDateParam != null && !effectiveDateParam.trim().isEmpty()) {
                    try {
                        effectiveDate = java.sql.Date.valueOf(effectiveDateParam.trim());
                    } catch (Exception ignored) {
                    }
                }
                if (effectiveDate == null) {
                    effectiveDate = new java.sql.Date(System.currentTimeMillis());
                }

                String result = userDAO.transferRegularStaff(userId, toCentreId, adminId, reason.trim(), notes != null ? notes.trim() : "", effectiveDate);
                if ("SUCCESS".equals(result)) {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?success=transferred&tab=staff");
                } else if ("SAME_CENTRE".equals(result)) {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?error=same_centre&tab=staff");
                } else if ("USER_NOT_ACTIVE".equals(result)) {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?error=user_inactive&tab=staff");
                } else if ("CENTRE_INACTIVE".equals(result)) {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?error=centre_inactive&tab=staff");
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
