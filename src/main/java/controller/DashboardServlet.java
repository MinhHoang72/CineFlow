package controller;

import dao.CinemaDAO;
import dao.ReportDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.User;
import java.io.IOException;

public class DashboardServlet extends HttpServlet {
    private final ReportDAO reportDAO = new ReportDAO();
    private final CinemaDAO cinemaDAO = new CinemaDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            String loginUrl = request.getContextPath() + "/auth/login.jsp?redirect=" + java.net.URLEncoder.encode("/dashboard", "UTF-8");
            response.sendRedirect(loginUrl);
            return;
        }

        String role = (String) session.getAttribute("role");
        if (role == null && session.getAttribute("user") instanceof User) {
            role = ((User) session.getAttribute("user")).getRole();
        }

        if ("ADMIN".equalsIgnoreCase(role)) {
            request.setAttribute("reportSummary", reportDAO.getSummary());
            request.setAttribute("topMovies", reportDAO.revenueByMovie());
            request.setAttribute("centreRevenue", reportDAO.revenueByCentre());
            request.setAttribute("movies", cinemaDAO.getAllMovies());
            request.setAttribute("centres", cinemaDAO.getActiveCentres());
            request.getRequestDispatcher("/admin/dashboard.jsp").forward(request, response);
            return;
        }
        if ("MANAGER".equalsIgnoreCase(role)) {
            request.getRequestDispatcher("/manager/dashboard.jsp").forward(request, response);
            return;
        }
        if ("STAFF".equalsIgnoreCase(role)) {
            request.getRequestDispatcher("/staff/dashboard.jsp").forward(request, response);
            return;
        }

        // CUSTOMER role
        response.sendRedirect(request.getContextPath() + "/home/index.jsp");
    }
}
