package controller.manager.showtime;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import service.ShowtimeService;

import java.io.IOException;
import java.time.LocalDateTime;

@WebServlet("/showtimes/create")
public class CreateShowtimeServlet extends HttpServlet {

    private final ShowtimeService showtimeService = new ShowtimeService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        String role = session != null ? (String) session.getAttribute("role") : null;
        if (!"MANAGER".equalsIgnoreCase(role)) {
            request.getSession().setAttribute("error", "Chỉ Quản lý cụm rạp mới có quyền tạo suất chiếu.");
            response.sendRedirect(request.getContextPath() + "/showtimes");
            return;
        }

        try {
            int movieId = Integer.parseInt(request.getParameter("movieId"));
            int hallId = Integer.parseInt(request.getParameter("hallId"));
            LocalDateTime startTime = LocalDateTime.parse(request.getParameter("startTime"));
            showtimeService.saveShowtime(request,null,movieId,hallId,startTime);
            request.getSession().setAttribute("message", "Đã tạo suất chiếu, chốt giá và mở bán.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            request.getSession().setAttribute("error", e.getMessage());
        } catch (Exception e) {
            request.getSession().setAttribute("error", "Lỗi dữ liệu đầu vào khi tạo suất chiếu.");
        }

        response.sendRedirect(request.getContextPath() + "/showtimes");
    }
}
