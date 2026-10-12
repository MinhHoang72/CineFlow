package controller.admin;

import dao.CinemaDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.Part;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Movie;
import utils.MediaStorage;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Date;
import java.util.UUID;

@MultipartConfig(maxFileSize = 50 * 1024 * 1024, maxRequestSize = 60 * 1024 * 1024)
public class MovieManagementServlet extends HttpServlet {
    private final CinemaDAO dao = new CinemaDAO();

    private boolean allowed(HttpSession session) {
        return session != null && "ADMIN".equals(session.getAttribute("role"));
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!allowed(request.getSession(false))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Admin được quản lý danh mục phim.");
            return;
        }
        if ("/admin/movies".equals(request.getServletPath())) {
            request.setAttribute("movies", dao.getAllMovies());
            request.getRequestDispatcher("/admin/movies.jsp").forward(request, response);
            return;
        }

        String id = request.getParameter("movieId");
        if (id != null && !id.isBlank()) {
            try {
                Movie movie = dao.getMovieById(Integer.parseInt(id));
                if (movie == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy phim.");
                    return;
                }
                request.setAttribute("movie", movie);
            } catch (NumberFormatException exception) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã phim không hợp lệ.");
                return;
            }
        }
        request.getRequestDispatcher("/admin/movie-management.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!allowed(request.getSession(false))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String action = request.getParameter("action");
        try {
            int movieId = Integer.parseInt(request.getParameter("movieId"));
            if ("archive".equals(action)) dao.archiveMovie(movieId);
            else if ("delete".equals(action)) dao.deleteMovie(movieId);
        } catch (Exception ignored) { }

        if ("save".equals(action)) {
            try {
                Movie movie = readMovie(request);
                String posterUrl = saveUpload(request, "posterFile", "poster");
                String trailerUrl = saveUpload(request, "trailerFile", "trailer");
                if (posterUrl != null) movie.setPosterUrl(posterUrl);
                if (trailerUrl != null) movie.setTrailerUrl(trailerUrl);
                if (movie.getMovieId() == 0) dao.createMovie(movie);
                else dao.updateMovie(movie);
            } catch (Exception ignored) { }
        }
        response.sendRedirect(request.getContextPath() + "/admin/movies");
    }

    private Movie readMovie(HttpServletRequest request) {
        Movie movie = new Movie();
        String id = request.getParameter("movieId");
        if (id != null && !id.isBlank()) movie.setMovieId(Integer.parseInt(id));
        movie.setTitle(request.getParameter("title"));
        movie.setDescription(request.getParameter("description"));
        movie.setDuration(Integer.parseInt(request.getParameter("duration")));
        movie.setGenre(request.getParameter("genre"));
        movie.setDirector(request.getParameter("director"));
        movie.setCast(request.getParameter("cast"));
        movie.setLanguage(request.getParameter("language"));
        movie.setReleaseDate(parseDate(request.getParameter("releaseDate")));
        movie.setEndDate(parseDate(request.getParameter("endDate")));
        movie.setPosterUrl(request.getParameter("posterUrl"));
        movie.setTrailerUrl(request.getParameter("trailerUrl"));
        movie.setAgeRating(request.getParameter("ageRating"));
        movie.setStatus(request.getParameter("status"));
        return movie;
    }

    private Date parseDate(String value) {
        return value == null || value.isBlank() ? null : Date.valueOf(value);
    }

    private String saveUpload(HttpServletRequest request, String partName, String prefix) throws Exception {
        Part part = request.getPart(partName);
        if (part == null || part.getSize() == 0) return null;
        String submitted = part.getSubmittedFileName();
        if (submitted == null || submitted.isBlank()) return null;
        String original = Paths.get(submitted).getFileName().toString();
        String extension = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) extension = original.substring(dot).toLowerCase();
        if (!extension.matches("\\.(jpg|jpeg|png|webp|mp4|webm|ogg)")) return null;
        java.nio.file.Path directory = MediaStorage.uploadDirectory(getServletContext(), "movies");
        String fileName = prefix + "-" + UUID.randomUUID() + extension;
        part.write(directory.resolve(fileName).toString());
        return request.getContextPath() + "/uploads/movies/" + fileName;
    }
}
