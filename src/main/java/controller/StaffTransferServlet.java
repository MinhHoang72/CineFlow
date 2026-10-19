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
 * Controller điều khiển nghiệp vụ Bổ nhiệm Quản lý trung tâm và Điều chuyển nhân sự dành cho Quản trị viên (Admin).
 * URL ánh xạ: /admin/transfers
 */
public class StaffTransferServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final CinemaDAO cinemaDAO = new CinemaDAO();

    /**
     * Kiểm tra phiên người dùng có vai trò Quản trị viên (Admin) hay không.
     *
     * @param session phiên làm việc HttpSession
     * @return true nếu là Admin, ngược lại false
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
     * Xử lý yêu cầu HTTP GET: Tải dữ liệu các cụm rạp, quản lý, danh bạ nhân sự và lịch sử điều chuyển để hiển thị giao diện.
     *
     * @param request  yêu cầu HTTP từ client
     * @param response phản hồi HTTP gửi về client
     * @throws ServletException nếu xảy ra lỗi servlet
     * @throws IOException      nếu xảy ra lỗi vào/ra dữ liệu
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 1. Kiểm tra xác thực (nếu chưa đăng nhập thì chuyển hướng sang trang Login)
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            String loginUrl = request.getContextPath() + "/auth/login.jsp?redirect="
                    + URLEncoder.encode("/admin/transfers", StandardCharsets.UTF_8);
            response.sendRedirect(loginUrl);
            return;
        }

        // 2. Kiểm tra phân quyền (chỉ tài khoản Admin mới có quyền truy cập)
        if (!isAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập quản lý điều chuyển nhân sự.");
            return;
        }

        // 3. Đọc các bộ lọc tìm kiếm từ request (lọc theo cụm rạp, từ khóa tên/email, vai trò nhân sự)
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

        // 4. Đồng bộ và giải phóng liên kết rạp của các Quản lý đang bị khóa/tạm ngưng tài khoản
        userDAO.syncLockedManagersAssignments();

        // 5. Lấy dữ liệu nguồn phục vụ hiển thị các tab điều chuyển và bổ nhiệm
        List<Centre> centresWithManager = cinemaDAO.getAllCentresWithManager();
        List<Centre> activeCentres = cinemaDAO.getActiveCentres();
        List<User> managers = userDAO.getManagers();
        List<ManagerAssignment> managerAssignments = userDAO.getManagerAssignmentHistory(centreFilter, null);
        List<User> staffList = userDAO.getTransferableUsers(centreFilter, searchParam, roleFilter);
        List<StaffTransfer> transferHistory = userDAO.getTransferHistory(centreFilter, null);

        // 6. Tính toán các chỉ số thống kê (KPI) phục vụ khối điều hành trên đầu trang
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

        // 7. Gắn các thuộc tính kết quả vào request
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

        // 8. Chuyển tiếp tới giao diện transfers.jsp
        request.getRequestDispatcher("/admin/transfers.jsp").forward(request, response);
    }

    /**
     * Xử lý yêu cầu HTTP POST: Tiếp nhận thao tác bổ nhiệm Manager hoặc điều chuyển nhân sự giữa các cụm rạp.
     *
     * @param request  yêu cầu HTTP từ client
     * @param response phản hồi HTTP gửi về client
     * @throws IOException nếu xảy ra lỗi vào/ra dữ liệu
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 1. Thiết lập encoding UTF-8
        request.setCharacterEncoding("UTF-8");

        // 2. Kiểm tra xác thực phiên và quyền Quản trị viên
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null || !isAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền thực hiện thao tác này.");
            return;
        }

        User adminUser = (User) session.getAttribute("user");
        int adminId = adminUser != null ? adminUser.getUserId() : (int) session.getAttribute("userId");
        String action = request.getParameter("action");

        // 3. Xử lý bổ nhiệm Manager vào trung tâm (action = "assign_manager")
        if ("assign_manager".equalsIgnoreCase(action)) {
            try {
                // 3.1. Đọc các tham số bổ nhiệm từ form
                String cParam = request.getParameter("centreId");
                int centreId = Integer.parseInt(cParam);
                int managerId = Integer.parseInt(request.getParameter("managerId"));
                String notes = request.getParameter("notes");

                // 3.2. Gọi UserDAO để thực thi Transaction bổ nhiệm và xử lý concurrency check
                String result = userDAO.assignManager(centreId, managerId, adminId, notes);

                // 3.3. Xử lý các mã kết quả trả về từ DAO và điều hướng kèm thông báo tương ứng
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

        // 4. Xử lý miễn nhiệm Manager (action = "unassign_manager")
        // Hệ thống không cho phép rạp bị bỏ trống Quản lý; muốn thay thế phải dùng chức năng Điều chuyển
        if ("unassign_manager".equalsIgnoreCase(action)) {
            response.sendRedirect(request.getContextPath() + "/admin/transfers?error=unassign_not_supported&tab=manager");
            return;
        }

        // 5. Xử lý điều chuyển nhân sự hoặc điều chuyển Quản lý (action = "transfer_staff" / "transfer")
        if ("transfer_staff".equalsIgnoreCase(action) || "transfer".equalsIgnoreCase(action) || "transfer_manager".equalsIgnoreCase(action)) {
            try {
                // 5.1. Đọc thông tin nhân sự cần chuyển và cụm rạp đích đến
                int userId = Integer.parseInt(request.getParameter("userId"));
                String toParam = request.getParameter("toCentreId");
                int toCentreId = Integer.parseInt(toParam);
                String reason = request.getParameter("reason");
                String notes = request.getParameter("notes");
                String effectiveDateParam = request.getParameter("effectiveDate");

                // 5.2. Bắt buộc phải có lý do điều chuyển nhân sự
                if (reason == null || reason.trim().isEmpty()) {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?error=missing_reason&tab=staff");
                    return;
                }

                // 5.3. Kiểm tra xem người dùng cần điều chuyển có tồn tại trong hệ thống không
                User targetUser = userDAO.getUserById(userId);
                if (targetUser == null) {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?error=user_not_found&tab=staff");
                    return;
                }

                // 5.4. Trường hợp điều chuyển Quản lý (Role = MANAGER)
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

                // 5.5. Trường hợp điều chuyển nhân viên thông thường (Staff bán vé / soát vé)
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

                // 5.6. Thực thi điều chuyển nhân viên thông thường qua UserDAO
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

        // 6. Nếu không khớp hành động nào, chuyển hướng về trang điều chuyển
        response.sendRedirect(request.getContextPath() + "/admin/transfers");
    }
}
