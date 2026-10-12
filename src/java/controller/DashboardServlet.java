package controller;

import dao.CinemaDAO;
import dao.ReportDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

public class DashboardServlet extends HttpServlet {
    private final ReportDAO reportDAO = new ReportDAO();
    private final CinemaDAO cinemaDAO = new CinemaDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        String role = session == null ? null : (String) session.getAttribute("role");
        if (role == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login.jsp");
            return;
        }
        if ("ADMIN".equals(role)) {
            request.setAttribute("reportSummary", reportDAO.getSummary());
            request.setAttribute("topMovies", reportDAO.revenueByMovie());
            request.setAttribute("branchRevenue", reportDAO.revenueByBranch());
            request.setAttribute("movies", cinemaDAO.getAllMovies());
            request.setAttribute("branches", cinemaDAO.getActiveBranches());
            request.getRequestDispatcher("/admin/dashboard.jsp").forward(request, response);
            return;
        }
        String page = "MANAGER".equals(role) ? "/manager/dashboard.jsp" : "STAFF".equals(role) ? "/staff/dashboard.jsp" : "/home/index.jsp";
        request.getRequestDispatcher(page).forward(request, response);
    }
}
