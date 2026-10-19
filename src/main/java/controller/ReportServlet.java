package controller;

import dao.ReportDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.User;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Controller: Điều khiển màn hình báo cáo doanh thu và công suất phòng chiếu dành cho Quản lý (Manager) và Quản trị viên (Admin).
 * URL ánh xạ: /reports
 */
public class ReportServlet extends HttpServlet {

    private final ReportDAO reportDAO = new ReportDAO();

    /**
     * Kiểm tra quyền xem báo cáo của người dùng trong phiên (Chỉ ADMIN hoặc MANAGER).
     *
     * @param session phiên làm việc HttpSession
     * @return true nếu có quyền, ngược lại false
     */
    private boolean allowed(HttpSession session) {
        if (session == null) {
            return false;
        }
        Object role = session.getAttribute("role");
        if (role == null && session.getAttribute("user") instanceof User) {
            role = ((User) session.getAttribute("user")).getRole();
        }
        return "MANAGER".equalsIgnoreCase((String) role) || "ADMIN".equalsIgnoreCase((String) role);
    }

    /**
     * Xử lý yêu cầu HTTP GET: Kiểm tra quyền hạn và tải dữ liệu báo cáo kinh doanh để hiển thị.
     *
     * @param request  yêu cầu HTTP từ client
     * @param response phản hồi HTTP gửi về client
     * @throws ServletException nếu xảy ra lỗi servlet
     * @throws IOException      nếu xảy ra lỗi vào/ra dữ liệu
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 1. Kiểm tra xác thực đăng nhập
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            String loginUrl = request.getContextPath() + "/auth/login.jsp?redirect="
                    + URLEncoder.encode("/reports", StandardCharsets.UTF_8);
            response.sendRedirect(loginUrl);
            return;
        }

        // 2. Kiểm tra phân quyền truy cập (Admin hoặc Manager)
        if (!allowed(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập báo cáo kinh doanh.");
            return;
        }

        // 3. Tải các số liệu thống kê kinh doanh từ ReportDAO
        request.setAttribute("reportSummary", reportDAO.getSummary());
        request.setAttribute("centreRevenue", reportDAO.revenueByCentre());
        request.setAttribute("movieRevenue", reportDAO.revenueByMovie());
        request.setAttribute("showtimeMetrics", reportDAO.occupancyByShowtime());

        // 4. Chuyển tiếp tới giao diện báo cáo reports.jsp
        request.getRequestDispatcher("/admin/reports.jsp").forward(request, response);
    }
}
