package controller.admin;

import dao.ReportDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

public class ReportServlet extends HttpServlet {
    private final ReportDAO reportDAO = new ReportDAO();
    private boolean allowed(HttpSession session) {
        if (session == null) return false;
        String role = (String) session.getAttribute("role");
        return "MANAGER".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role);
    }
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (!allowed(session)) { response.sendRedirect(request.getContextPath() + "/auth/login.jsp"); return; }
        request.setAttribute("reportSummary", reportDAO.getSummary());
        request.setAttribute("branchRevenue", reportDAO.revenueByBranch());
        request.setAttribute("movieRevenue", reportDAO.revenueByMovie());
        request.setAttribute("showtimeMetrics", reportDAO.occupancyByShowtime());
        String role = (String) session.getAttribute("role");
        request.getRequestDispatcher("ADMIN".equalsIgnoreCase(role) ? "/admin/reports.jsp" : "/manager/reports.jsp").forward(request, response);
    }
}


