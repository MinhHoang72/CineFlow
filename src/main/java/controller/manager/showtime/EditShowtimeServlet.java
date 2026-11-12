package controller.manager.showtime;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.entity.Showtime;
import service.ShowtimeService;

import java.io.IOException;

@WebServlet("/showtimes/edit")
public class EditShowtimeServlet extends HttpServlet {

    private final ShowtimeService showtimeService = new ShowtimeService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        String role = session != null ? (String) session.getAttribute("role") : null;
        if (!"MANAGER".equalsIgnoreCase(role)) {
            request.getSession().setAttribute("error", "Chỉ Quản lý cụm rạp mới có quyền chỉnh sửa suất chiếu.");
            response.sendRedirect(request.getContextPath() + "/showtimes");
            return;
        }

        try {
            int id = Integer.parseInt(request.getParameter("id"));
            Showtime showtime = showtimeService.getShowtimeById(id);

            if (showtime == null || !showtime.isEditable() || showtime.getCentreId()!=ShowtimeService.requireCentreId(request)) {
                request.getSession().setAttribute("error", "Suất chiếu không tồn tại hoặc đã chiếu/đã hủy.");
                response.sendRedirect(request.getContextPath() + "/showtimes");
                return;
            }

            int centreId = ShowtimeService.requireCentreId(request);

            request.setAttribute("showtime", showtime);
            request.setAttribute("movies", showtimeService.getAvailableMoviesByCentre(centreId));
            request.setAttribute("halls", showtimeService.getAvailableHallsByCentre(centreId));

            request.getRequestDispatcher("/WEB-INF/views/manager/showtime/showtimeEdit.jsp")
                    .forward(request, response);
        } catch (Exception e) {
            request.getSession().setAttribute("error", "Không tìm thấy thông tin suất chiếu.");
            response.sendRedirect(request.getContextPath() + "/showtimes");
        }
    }
}
