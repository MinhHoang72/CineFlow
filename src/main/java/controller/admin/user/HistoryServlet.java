package controller.admin.user;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
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
 * Controller chuyên trách phân hệ Nhật ký Nhân sự (Staff Audit Log).
 * Tách biệt hoàn toàn phần Lưu vết / Kiểm toán lịch sử khỏi màn hình thao tác Bổ nhiệm & Điều chuyển.
 * URL: /admin/transfers/history hoặc /admin/staff-history
 */
@WebServlet(name = "HistoryServlet", urlPatterns = {"/admin/transfers/history", "/admin/staff-history"})
public class HistoryServlet extends HttpServlet {

    private final UserService userService = new UserService();

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

        // 3. Lấy dữ liệu nguồn và tính KPI kiểm toán thông qua UserService
        UserService.HistoryPageData data = userService.getHistoryPageData(centreFilter);

        // 4. Gắn thuộc tính vào request
        request.setAttribute("activeCentres", data.activeCentres());
        request.setAttribute("selectedCentreId", centreFilter);
        request.setAttribute("searchParam", searchParam);
        request.setAttribute("activeTab", activeTab);

        request.setAttribute("managerAssignments", data.managerAssignments());
        request.setAttribute("transferHistory", data.transferHistory());

        request.setAttribute("kpiTotalAssignments", data.kpi().totalAssignments());
        request.setAttribute("kpiActiveAssignments", data.kpi().activeAssignments());
        request.setAttribute("kpiTotalTransfers", data.kpi().totalTransfers());

        request.setAttribute("activeNav", "staff-history");
        request.setAttribute("pageTitle", "Nhật ký Nhân sự");

        // 5. Chuyển tiếp tới giao diện lịch sử bổ nhiệm và điều chuyển
        request.getRequestDispatcher("/WEB-INF/views/admin/user/history.jsp").forward(request, response);
    }
}
