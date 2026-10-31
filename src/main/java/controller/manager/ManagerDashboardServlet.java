package controller.manager;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.entity.User;

import java.io.IOException;
import java.net.URLEncoder;

/**
 * Controller: Bảng điều khiển Quản lý cụm rạp (Cinema Manager Dashboard).
 * URL ánh xạ: /manager/dashboard, /manager
 */
@WebServlet(name = "ManagerDashboardServlet", urlPatterns = {"/manager/dashboard", "/manager"})
public class ManagerDashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            String loginUrl = request.getContextPath() + "/login?redirect=" + URLEncoder.encode("/manager/dashboard", "UTF-8");
            response.sendRedirect(loginUrl);
            return;
        }

        String role = (String) session.getAttribute("role");
        if (role == null && session.getAttribute("user") instanceof User) {
            role = ((User) session.getAttribute("user")).getRole();
        }

        if (!"MANAGER".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Quản lý (MANAGER) mới có quyền truy cập trang này.");
            return;
        }

        request.setAttribute("activeNav", "dashboard");
        request.setAttribute("pageTitle", "Tổng quan cụm rạp");
        request.setAttribute("pageBreadcrumb", "Dashboard");
        try {
            int centreId = service.ShowtimeService.requireCentreId(request);
            model.entity.Centre centre = new dao.CentreDAO().getCentreById(centreId);
            dao.HallDAO hallDAO = new dao.HallDAO();
            request.setAttribute("managedCentre", centre);
            request.setAttribute("totalHalls", hallDAO.countHallsByStatus(centreId, "ALL"));
            request.setAttribute("availableHalls", hallDAO.countHallsByStatus(centreId, "ACTIVE"));
            request.setAttribute("totalCapacity", hallDAO.sumCapacityByCentre(centreId));
            java.time.LocalDate today = java.time.LocalDate.now();
            java.util.List<model.entity.Showtime> shows = new dao.ShowtimeDAO().getByCentre(centreId);
            request.setAttribute("todayShowtimes", shows.stream().filter(s -> s.getStartTime() != null
                    && s.getStartTime().toLocalDateTime().toLocalDate().equals(today)
                    && !"CANCELLED".equals(s.getStatus())).count());
        } catch (IllegalStateException e) {
            request.setAttribute("dashboardError", "Chưa xác định được cụm rạp đang quản lý. Vui lòng kiểm tra phân công.");
        }
        request.getRequestDispatcher("/WEB-INF/views/manager/managerDashboard.jsp").forward(request, response);
    }
}
