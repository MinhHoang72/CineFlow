package controller.movie;

import dao.CinemaDAO;
import dao.RatingDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import model.Movie;
import model.User;

public class MovieDetailsServlet extends HttpServlet {

    private final CinemaDAO cinemaDAO = new CinemaDAO();
    private final RatingDAO ratingDAO = new RatingDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String value = request.getParameter("movieId");
        int movieId;
        try {
            movieId = Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Movie ID is invalid.");
            return;
        }

        Movie movie = cinemaDAO.getMovieById(movieId);
        if (movie == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Movie not found.");
            return;
        }

        Integer currentUserId = null;
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            Object uObj = session.getAttribute("user");
            if (uObj instanceof User) {
                currentUserId = ((User) uObj).getUserId();
            } else {
                try {
                    currentUserId = (Integer) uObj.getClass().getMethod("getUserId").invoke(uObj);
                } catch (Exception ignored) {}
            }
        }

        RatingDAO.MovieRatingSummary ratingSummary = ratingDAO.getRatingSummary(movieId, currentUserId);

        request.setAttribute("movie", movie);
        request.setAttribute("ratingSummary", ratingSummary);
        request.setAttribute("showtimes", cinemaDAO.getScheduledShowtimesByMovieId(movieId));
        request.setAttribute("nowShowingMovies", cinemaDAO.getNowShowingMovies());
        request.getRequestDispatcher("/movie/details.jsp").forward(request, response);
    }
}
