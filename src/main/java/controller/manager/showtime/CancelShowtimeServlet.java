package controller.manager.showtime;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import service.ShowtimeService;

import java.io.IOException;

@WebServlet("/showtimes/cancel")
public class CancelShowtimeServlet extends HttpServlet {

    private final ShowtimeService showtimeService = new ShowtimeService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);
        String role = session != null ? (String) session.getAttribute("role") : null;
        if (!"MANAGER".equalsIgnoreCase(role)) {
            request.getSession().setAttribute("error", "Chỉ Quản lý cụm rạp mới có quyền hủy suất chiếu.");
            response.sendRedirect(request.getContextPath() + "/showtimes");
            return;
        }

        try {
            int showtimeId = Integer.parseInt(request.getParameter("showtimeId"));
            showtimeService.cancelShowtime(showtimeId);
            request.getSession().setAttribute("message", "Suất chiếu #" + showtimeId + " đã được hủy thành công.");
        } catch (IllegalStateException e) {
            request.getSession().setAttribute("error", e.getMessage());
        } catch (Exception e) {
            request.getSession().setAttribute("error", "Không thể hủy suất chiếu. Vui lòng thử lại.");
        }

        response.sendRedirect(request.getContextPath() + "/showtimes");
    }
}
