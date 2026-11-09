package controller.admin.user;
import jakarta.servlet.annotation.WebServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.entity.User;
import service.UserService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Controller điều khiển nghiệp vụ Bổ nhiệm Quản lý trung tâm và Điều chuyển nhân sự dành cho Quản trị viên (Admin).
 * URL ánh xạ: /admin/transfers
 */
@WebServlet(name = "TransferServlet", urlPatterns = {"/admin/transfers"})
public class TransferServlet extends HttpServlet {

    private final UserService userService = new UserService();

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
            String loginUrl = request.getContextPath() + "/login?redirect="
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

        // 4. Lấy dữ liệu nguồn và tính toán KPI thông qua UserService
        UserService.TransferPageData data = userService.getTransferPageData(centreFilter, searchParam, roleFilter);

        // 5. Gắn các thuộc tính kết quả vào request
        request.setAttribute("kpiTotalCentres", data.kpi().totalCentres());
        request.setAttribute("kpiAssignedCentres", data.kpi().assignedCentres());
        request.setAttribute("kpiUnassignedCentres", data.kpi().unassignedCentres());
        request.setAttribute("kpiUnassignedManagers", data.kpi().unassignedManagers());

        request.setAttribute("centresWithManager", data.centresWithManager());
        request.setAttribute("activeCentres", data.activeCentres());
        request.setAttribute("selectedCentreId", centreFilter);

        request.setAttribute("managers", data.managers());
        request.setAttribute("managerAssignments", data.managerAssignments());
        request.setAttribute("staffList", data.staffList());
        request.setAttribute("transferHistory", data.transferHistory());
        request.setAttribute("searchParam", searchParam);
        request.setAttribute("roleFilter", roleFilter);
        request.setAttribute("activeNav", "transfers");

        // 6. Chuyển tiếp tới giao diện transfers.jsp
        request.getRequestDispatcher("/WEB-INF/views/admin/user/transfers.jsp").forward(request, response);
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

                // 3.2. Gọi Service để thực thi nghiệp vụ bổ nhiệm
                String result = userService.assignManager(centreId, managerId, adminId, notes);

                // 3.3. Xử lý các mã kết quả trả về từ Service và điều hướng kèm thông báo tương ứng
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

        // 4. Xử lý miễn nhiệm Quản lý (action = "unassign_manager")
        if ("unassign_manager".equalsIgnoreCase(action)) {
            try {
                String cParam = request.getParameter("centreId");
                int centreId = Integer.parseInt(cParam);
                String mParam = request.getParameter("managerId");
                int managerId = Integer.parseInt(mParam);
                String reason = request.getParameter("reason");

                if (reason == null || reason.trim().isEmpty()) {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?error=missing_unassign_reason&tab=manager");
                    return;
                }

                boolean ok = userService.unassignManager(centreId, managerId, adminId, reason.trim());
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

        // 5. Xử lý điều chuyển nhân sự (action = "transfer_staff" / "transfer")
        // Lưu ý: Chức năng điều chuyển chỉ áp dụng cho nhân viên thường (Bán vé, Soát vé).
        // Vị trí Quản lý trung tâm thực hiện độc lập qua Bổ nhiệm & Miễn nhiệm.
        if ("transfer_staff".equalsIgnoreCase(action) || "transfer".equalsIgnoreCase(action)) {
            try {
                // 5.1. Đọc thông tin nhân sự cần chuyển và cụm rạp đích đến
                int userId = Integer.parseInt(request.getParameter("userId"));
                String toParam = request.getParameter("toCentreId");
                int toCentreId = Integer.parseInt(toParam);
                String reason = request.getParameter("reason");
                String notes = request.getParameter("notes");
                String effectiveDateParam = request.getParameter("effectiveDate");
                String newRoleParam = request.getParameter("newRole");

                // 5.2. Gọi Service xử lý logic kiểm tra vai trò, ngày hiệu lực và ghi nhận điều chuyển
                String result = userService.transferStaff(userId, toCentreId, adminId, reason, notes, effectiveDateParam, newRoleParam);

                // 5.3. Xử lý kết quả trả về từ Service
                if ("SUCCESS".equals(result)) {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?success=transferred&tab=staff");
                } else if ("USER_NOT_FOUND".equals(result)) {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?error=user_not_found&tab=staff");
                } else if ("MANAGER_TRANSFER_NOT_ALLOWED".equals(result)) {
                    response.sendRedirect(request.getContextPath() + "/admin/transfers?error=manager_transfer_not_allowed&tab=staff");
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
