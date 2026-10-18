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
import java.net.URLEncoder;

/**
 * Điều hướng người dùng đến bảng điều khiển tương ứng với vai trò (Admin, Manager, Staff, Customer).
 */
public class DashboardServlet extends HttpServlet {

    private final ReportDAO reportDAO = new ReportDAO();
    private final CinemaDAO cinemaDAO = new CinemaDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            String loginUrl = request.getContextPath() + "/auth/login.jsp?redirect=" + URLEncoder.encode("/dashboard", "UTF-8");
            response.sendRedirect(loginUrl);
            return;
        }

        String role = (String) session.getAttribute("role");
        if (role == null && session.getAttribute("user") instanceof User) {
            role = ((User) session.getAttribute("user")).getRole();
        }

        // 1. Quản trị viên hệ thống (ADMIN) -> Bảng điều khiển phân tích số liệu tổng hợp
        if ("ADMIN".equalsIgnoreCase(role)) {
            request.setAttribute("reportSummary", reportDAO.getSummary());
            request.setAttribute("topMovies", reportDAO.revenueByMovie());
            request.setAttribute("centreRevenue", reportDAO.revenueByCentre());
            request.setAttribute("movies", cinemaDAO.getAllMovies());
            request.setAttribute("centres", cinemaDAO.getActiveCentres());
            request.getRequestDispatcher("/admin/dashboard.jsp").forward(request, response);
            return;
        }

        // 2. Quản lý trung tâm (MANAGER) -> Bảng điều khiển quản lý nhân sự & rạp
        if ("MANAGER".equalsIgnoreCase(role)) {
            request.getRequestDispatcher("/manager/dashboard.jsp").forward(request, response);
            return;
        }

        // 3. Nhân viên cụm rạp (Ticket Sales Staff, Ticket Checker Staff, Staff chung)
        if ("STAFF".equalsIgnoreCase(role)
                || "TICKET_SALES_STAFF".equalsIgnoreCase(role)
                || "TICKET_CHECKER_STAFF".equalsIgnoreCase(role)
                || "TICKET_SALES".equalsIgnoreCase(role)
                || "TICKET_CHECKER".equalsIgnoreCase(role)) {
            request.getRequestDispatcher("/staff/dashboard.jsp").forward(request, response);
            return;
        }

        // 4. Khách hàng (CUSTOMER) -> Chuyển hướng về trang chủ
        response.sendRedirect(request.getContextPath() + "/home/index.jsp");
    }
}
