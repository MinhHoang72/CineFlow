package controller.manager.showtime;

import dao.HallDAO;
import dao.MovieDAO;
import dao.ShowtimeDAO;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/showtimes")
public class ShowtimeListServlet
        extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        int centreId = service.ShowtimeService.requireCentreId(request);

        ShowtimeDAO showtimeDAO =
                new ShowtimeDAO();

        if ("availability".equals(request.getParameter("action"))) {
            previewAvailability(request, response, centreId, showtimeDAO);
            return;
        }

        try {
            showtimeDAO.synchronizeStatuses();
        } catch (java.sql.SQLException e) {
            throw new ServletException("Unable to synchronize showtime statuses", e);
        }

        MovieDAO movieDAO =
                new MovieDAO();

        HallDAO hallDAO =
                new HallDAO();

        request.setAttribute(
                "showtimes",
                showtimeDAO.getByCentre(centreId)
        );

        request.setAttribute(
                "movies",
                movieDAO.getAvailableMoviesByCentre(
                        centreId
                )
        );

        request.setAttribute(
                "halls",
                hallDAO.getAvailableHallsByCentre(
                        centreId
                )
        );

        request.getRequestDispatcher(
                "/WEB-INF/views/manager/showtime/showtimeList.jsp"
        ).forward(
                request,
                response
        );
    }

    private void previewAvailability(HttpServletRequest request, HttpServletResponse response,
            int centreId, ShowtimeDAO dao) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        try {
            int hallId = Integer.parseInt(request.getParameter("hallId"));
            int movieId = Integer.parseInt(request.getParameter("movieId"));
            java.time.LocalDateTime start = java.time.LocalDateTime.parse(request.getParameter("startTime"));
            boolean hallAllowed = new HallDAO().getAvailableHallsByCentre(centreId).stream()
                    .anyMatch(h -> h.getHallId() == hallId);
            model.entity.Movie movie = new MovieDAO().getAvailableMoviesByCentre(centreId).stream()
                    .filter(m -> m.getMovieId() == movieId).findFirst().orElse(null);
            if (!hallAllowed || movie == null || movie.getDuration() <= 0) {
                throw new IllegalArgumentException("Phim hoặc phòng không khả dụng tại cụm rạp này.");
            }
            java.time.LocalDateTime end = start.plusMinutes(movie.getDuration());
            java.util.List<model.entity.Showtime> conflicts = dao.findScheduleConflicts(centreId, hallId, start, end, 15);
            result.put("available", conflicts.isEmpty());
            result.put("endTime", end.toString());
            result.put("cleaningUntil", end.plusMinutes(15).toString());
            java.util.List<java.util.Map<String, String>> details = new java.util.ArrayList<>();
            for (model.entity.Showtime conflict : conflicts) {
                details.add(java.util.Map.of("title", conflict.getMovieTitle(),
                        "start", conflict.getStartTime().toLocalDateTime().toString(),
                        "end", conflict.getEndTime().toLocalDateTime().toString(),
                        "busyUntil", conflict.getEndTime().toLocalDateTime()
                                .plusMinutes(conflict.getCleaningBufferMinutes()).toString()));
            }
            result.put("conflicts", details);
        } catch (IllegalArgumentException | java.time.DateTimeException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            result.put("error", "Kiểm tra lại phim, phòng và thời gian bắt đầu.");
        } catch (Exception e) {
            getServletContext().log("Unable to preview showtime availability", e);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            result.put("error", "Chưa kiểm tra được lịch phòng. Vui lòng thử lại.");
        }
        response.getWriter().write(new com.google.gson.Gson().toJson(result));
    }
}
