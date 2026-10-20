package controller.admin;

import dao.CentreDAO;
import dao.MovieDAO;
import dao.ReportDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.dto.ReportSummary;
import model.entity.User;

import java.io.IOException;
import java.net.URLEncoder;

/**
 * Controller: Bảng điều khiển quản trị viên cấp cao (Admin Dashboard).
 * Phân luồng điều hướng theo vai trò (Admin, Manager, Staff, Customer).
 * URL ánh xạ: /dashboard, /admin/dashboard
 */
@WebServlet(name = "AdminDashboardServlet", urlPatterns = {"/dashboard", "/admin/dashboard"})
public class AdminDashboardServlet extends HttpServlet {

    private final ReportDAO reportDAO = new ReportDAO();
    private final CentreDAO centreDAO = new CentreDAO();
    private final MovieDAO movieDAO = new MovieDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Kiểm tra xác thực: Nếu chưa đăng nhập thì chuyển hướng sang trang đăng nhập
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            String loginUrl = request.getContextPath() + "/login?redirect=" + URLEncoder.encode("/dashboard", "UTF-8");
            response.sendRedirect(loginUrl);
            return;
        }

        // 2. Xác định vai trò (Role) của người dùng hiện tại từ session
        String role = (String) session.getAttribute("role");
        if (role == null && session.getAttribute("user") instanceof User) {
            role = ((User) session.getAttribute("user")).getRole();
        }

        // 3. Phân luồng điều hướng theo vai trò:
        // 3.1. Vai trò Quản trị viên (ADMIN) -> Hiển thị Dashboard tổng quan số liệu toàn hệ thống
        if ("ADMIN".equalsIgnoreCase(role)) {
            ReportSummary summary = reportDAO.getSummary();
            request.setAttribute("reportSummary", summary);
            request.setAttribute("topMovies", reportDAO.revenueByMovie());
            request.setAttribute("movies", movieDAO.getAllMovies());
            request.setAttribute("centres", centreDAO.getAllCentres("", "ACTIVE"));
            request.getRequestDispatcher("/WEB-INF/views/admin/adminDashboard.jsp").forward(request, response);
            return;
        }

        // 3.2. Vai trò Quản lý trung tâm (MANAGER) -> Dashboard vận hành cụm rạp
        if ("MANAGER".equalsIgnoreCase(role)) {
            response.sendRedirect(request.getContextPath() + "/manager/dashboard");
            return;
        }

        // 3.3. Vai trò Nhân viên cụm rạp (Bán vé, Soát vé) -> Màn hình tác vụ nhân viên
        if ("STAFF".equalsIgnoreCase(role)
                || "TICKET_SALES_STAFF".equalsIgnoreCase(role)
                || "TICKET_CHECKER_STAFF".equalsIgnoreCase(role)
                || "TICKET_SALES".equalsIgnoreCase(role)
                || "TICKET_CHECKER".equalsIgnoreCase(role)) {
            request.getRequestDispatcher("/WEB-INF/views/staff/staffDashboard.jsp").forward(request, response);
            return;
        }

        // 3.4. Vai trò Khách hàng (CUSTOMER) -> Điều hướng về trang chủ
        response.sendRedirect(request.getContextPath() + "/home");
    }
}
