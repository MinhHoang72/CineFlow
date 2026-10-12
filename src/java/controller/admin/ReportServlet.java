package controller.admin;

import dao.ReportDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.User;
import java.io.IOException;

public class ReportServlet extends HttpServlet {
    private final ReportDAO reportDAO = new ReportDAO();

    private boolean allowed(HttpSession session) {
        if (session == null) return false;
        Object role = session.getAttribute("role");
        if (role == null && session.getAttribute("user") instanceof User) {
            role = ((User) session.getAttribute("user")).getRole();
        }
        return "MANAGER".equalsIgnoreCase((String) role) || "ADMIN".equalsIgnoreCase((String) role);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            String loginUrl = request.getContextPath() + "/auth/login.jsp?redirect=" + java.net.URLEncoder.encode("/reports", "UTF-8");
            response.sendRedirect(loginUrl);
            return;
        }
        if (!allowed(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập báo cáo kinh doanh.");
            return;
        }

        request.setAttribute("reportSummary", reportDAO.getSummary());
        request.setAttribute("branchRevenue", reportDAO.revenueByBranch());
        request.setAttribute("movieRevenue", reportDAO.revenueByMovie());
        request.setAttribute("showtimeMetrics", reportDAO.occupancyByShowtime());

        String role = (String) session.getAttribute("role");
        if (role == null && session.getAttribute("user") instanceof User) {
            role = ((User) session.getAttribute("user")).getRole();
        }
        request.getRequestDispatcher("ADMIN".equalsIgnoreCase(role) ? "/admin/reports.jsp" : "/manager/reports.jsp").forward(request, response);
    }
}


