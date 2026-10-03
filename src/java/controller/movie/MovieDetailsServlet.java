package controller.movie;

import dao.CinemaDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import model.Movie;

public class MovieDetailsServlet extends HttpServlet {

    private final CinemaDAO cinemaDAO = new CinemaDAO();

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

        request.setAttribute("movie", movie);
        request.getRequestDispatcher("/movie/details.jsp").forward(request, response);
    }
}


