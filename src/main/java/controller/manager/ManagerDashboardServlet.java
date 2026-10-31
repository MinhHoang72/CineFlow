package controller.manager;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.entity.User;
import model.entity.Centre;
import dao.OperationsReportDAO;
import dao.CinemaDAO;
import model.dto.report.OperationsSummaryDTO;
import java.time.LocalDate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Controller: Bảng điều khiển Quản lý cụm rạp (Cinema Manager Dashboard).
 * URL ánh xạ: /manager/dashboard, /manager
 */
@WebServlet(name = "ManagerDashboardServlet", urlPatterns = {"/manager/dashboard", "/manager"})
public class ManagerDashboardServlet extends HttpServlet {

    private final OperationsReportDAO operationsReportDAO = new OperationsReportDAO();
    private final CinemaDAO cinemaDAO = new CinemaDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            String loginUrl = request.getContextPath() + "/login?redirect="
                    + URLEncoder.encode("/manager/dashboard", StandardCharsets.UTF_8);
            response.sendRedirect(loginUrl);
            return;
        }

        String role = (String) session.getAttribute("role");
        if (role == null && session.getAttribute("user") instanceof User) {
            role = ((User) session.getAttribute("user")).getRole();
        }

        if (!"MANAGER".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Chỉ Quản lý (MANAGER) hoặc Quản trị viên (ADMIN) mới có quyền truy cập trang này.");
            return;
        }

        User manager = (User) session.getAttribute("user");
        LocalDate today = LocalDate.now();
        OperationsSummaryDTO summary = null;

        request.setAttribute("activeNav", "dashboard");
        request.setAttribute("pageTitle", "Tổng quan điều hành cụm rạp");
        request.setAttribute("pageBreadcrumb", "Dashboard");
        request.setAttribute("managerUser", manager);
        request.setAttribute("dashboardFrom", today);
        request.setAttribute("dashboardTo", today.plusDays(7));
        request.setAttribute("totalHalls", 0);
        request.setAttribute("availableHalls", 0);
        request.setAttribute("totalCapacity", 0);
        request.setAttribute("todayShowtimes", 0L);

        try {
            int centreId = service.ShowtimeService.requireCentreId(request);
            Centre centre = cinemaDAO.getCentreById(centreId);
            request.setAttribute("managedCentre", centre);
            request.setAttribute("managerCentreName", centre == null ? null : centre.getCentreName());

            if (manager != null && centre != null
                    && (manager.getCentreName() == null || manager.getCentreName().isBlank())) {
                manager.setCentreName(centre.getCentreName());
                session.setAttribute("user", manager);
            }

            dao.HallDAO hallDAO = new dao.HallDAO();
            request.setAttribute("totalHalls", hallDAO.countHallsByStatus(centreId, "ALL"));
            request.setAttribute("availableHalls", hallDAO.countHallsByStatus(centreId, "ACTIVE"));
            request.setAttribute("totalCapacity", hallDAO.sumCapacityByCentre(centreId));

            long todayShowtimes = new dao.ShowtimeDAO().getByCentre(centreId).stream()
                    .filter(showtime -> showtime.getStartTime() != null
                            && showtime.getStartTime().toLocalDateTime().toLocalDate().equals(today)
                            && !"CANCELLED".equalsIgnoreCase(showtime.getStatus()))
                    .count();
            request.setAttribute("todayShowtimes", todayShowtimes);

            try {
                summary = operationsReportDAO.summary(today, today.plusDays(7), centreId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (IllegalStateException e) {
            request.setAttribute("dashboardError",
                    "Chưa xác định được cụm rạp đang quản lý. Vui lòng kiểm tra phân công.");
        }

        request.setAttribute("operationsSummary", summary);
        request.getRequestDispatcher("/WEB-INF/views/manager/managerDashboard.jsp").forward(request, response);
    }
}
