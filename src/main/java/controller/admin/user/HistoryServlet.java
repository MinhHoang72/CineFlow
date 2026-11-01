package controller.admin.user;

import dao.CinemaDAO;
import dao.UserDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.entity.Centre;
import model.entity.ManagerAssignment;
import model.entity.StaffTransfer;
import model.entity.User;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controller chuyên trách phân hệ Nhật ký Nhân sự (Staff Audit Log).
 * Tách biệt hoàn toàn phần Lưu vết / Kiểm toán lịch sử khỏi màn hình thao tác Bổ nhiệm & Điều chuyển.
 * URL: /admin/transfers/history hoặc /admin/staff-history
 */
@WebServlet(name = "HistoryServlet", urlPatterns = {"/admin/transfers/history", "/admin/staff-history"})
public class HistoryServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final CinemaDAO cinemaDAO = new CinemaDAO();

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
        // 1. Kiểm tra xác thực Admin
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            String loginUrl = request.getContextPath() + "/login?redirect="
                    + URLEncoder.encode("/admin/transfers/history", StandardCharsets.UTF_8);
            response.sendRedirect(loginUrl);
            return;
        }

        if (!isAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập nhật ký nhân sự.");
            return;
        }

        // 2. Nhận các bộ lọc
        Integer centreFilter = null;
        String centreParam = request.getParameter("centreId");
        if (centreParam != null && !centreParam.trim().isEmpty()) {
            try {
                centreFilter = Integer.parseInt(centreParam.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        String searchParam = request.getParameter("search");
        String activeTab = request.getParameter("tab");
        if (activeTab == null || activeTab.trim().isEmpty()) {
            activeTab = "assignment";
        }

        // 3. Tự động đồng bộ các quản lý bị khóa
        userDAO.syncLockedManagersAssignments();

        // 4. Lấy dữ liệu nguồn
        List<Centre> activeCentres = cinemaDAO.getActiveCentres();
        List<ManagerAssignment> managerAssignments = userDAO.getManagerAssignmentHistory(centreFilter, null);
        List<StaffTransfer> transferHistory = userDAO.getTransferHistory(centreFilter, null);

        // 5. Thống kê KPI kiểm toán
        int totalAssignments = managerAssignments != null ? managerAssignments.size() : 0;
        int activeAssignments = 0;
        if (managerAssignments != null) {
            for (ManagerAssignment ma : managerAssignments) {
                if ("ACTIVE".equalsIgnoreCase(ma.getStatus()) && ma.getEndDate() == null) {
                    activeAssignments++;
                }
            }
        }
        int totalTransfers = transferHistory != null ? transferHistory.size() : 0;

        // 6. Gắn thuộc tính vào request
        request.setAttribute("activeCentres", activeCentres);
        request.setAttribute("selectedCentreId", centreFilter);
        request.setAttribute("searchParam", searchParam);
        request.setAttribute("activeTab", activeTab);

        request.setAttribute("managerAssignments", managerAssignments);
        request.setAttribute("transferHistory", transferHistory);

        request.setAttribute("kpiTotalAssignments", totalAssignments);
        request.setAttribute("kpiActiveAssignments", activeAssignments);
        request.setAttribute("kpiTotalTransfers", totalTransfers);

        request.setAttribute("activeNav", "staff-history");
        request.setAttribute("pageTitle", "Nhật ký Nhân sự");

        // 7. Chuyển tiếp tới giao diện lịch sử bổ nhiệm và điều chuyển
        request.getRequestDispatcher("/WEB-INF/views/admin/user/history.jsp").forward(request, response);
    }
}
