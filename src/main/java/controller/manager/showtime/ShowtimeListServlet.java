package controller.manager.showtime;

import com.google.gson.Gson;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import service.ShowtimeService;

@WebServlet("/showtimes")
public class ShowtimeListServlet extends HttpServlet {

    private final ShowtimeService showtimeService = new ShowtimeService();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        int centreId = ShowtimeService.requireCentreId(request);
        request.setAttribute("showtimeListStateKey", ShowtimeService.requireUserId(request) + ":" + centreId);

        if ("availability".equals(request.getParameter("action"))) {
            previewAvailability(request, response, centreId);
            return;
        }

        try {
            showtimeService.synchronizeStatuses();
        } catch (java.sql.SQLException e) {
            throw new ServletException("Unable to synchronize showtime statuses", e);
        }

        request.setAttribute("showtimes", showtimeService.getShowtimesByCentre(centreId));
        request.setAttribute("movies", showtimeService.getAvailableMoviesByCentre(centreId));
        request.setAttribute("halls", showtimeService.getAvailableHallsByCentre(centreId));

        request.getRequestDispatcher("/WEB-INF/views/manager/showtime/showtimeList.jsp")
                .forward(request, response);
    }

    private void previewAvailability(HttpServletRequest request, HttpServletResponse response,
            int centreId) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        try {
            int hallId = Integer.parseInt(request.getParameter("hallId"));
            int movieId = Integer.parseInt(request.getParameter("movieId"));
            java.time.LocalDateTime start = java.time.LocalDateTime.parse(request.getParameter("startTime"));
            java.util.Map<String, Object> result = showtimeService.checkAvailability(centreId, hallId, movieId, start);
            response.getWriter().write(gson.toJson(result));
        } catch (IllegalArgumentException | java.time.DateTimeException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(java.util.Map.of("error", e.getMessage())));
        }
    }
}
