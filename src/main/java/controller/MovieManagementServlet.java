package controller;

import dao.CinemaDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import model.Movie;
import model.User;
import utils.MediaStorage;
import utils.ValidationUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.util.UUID;

/**
 * Điều khiển trang quản lý danh mục phim dành cho Quản trị viên (Admin): xem danh sách, tạo mới, chỉnh sửa thông tin phim.
 */
@MultipartConfig(maxFileSize = 50 * 1024 * 1024, maxRequestSize = 60 * 1024 * 1024)
public class MovieManagementServlet extends HttpServlet {

    private final CinemaDAO dao = new CinemaDAO();

    /**
     * Kiểm tra quyền Quản trị viên (Admin).
     */
    private boolean allowed(HttpSession session) {
        if (session == null) {
            return false;
        }
        Object roleObj = session.getAttribute("role");
        if (roleObj instanceof String && "ADMIN".equalsIgnoreCase((String) roleObj)) {
            return true;
        }
        Object userObj = session.getAttribute("user");
        if (userObj instanceof User) {
            String role = ((User) userObj).getRole();
            return role != null && "ADMIN".equalsIgnoreCase(role);
        }
        return false;
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
        if ("save".equals(action)) {
            String movieIdStr = request.getParameter("movieId");
            int movieId = 0;
            if (movieIdStr != null && !movieIdStr.isBlank()) {
                try {
                    movieId = Integer.parseInt(movieIdStr.trim());
                } catch (NumberFormatException ignored) {
                }
            }

            String title = request.getParameter("title");
            String durationStr = request.getParameter("duration");
            String ageRating = request.getParameter("ageRating");
            String status = request.getParameter("status");
            String releaseDateStr = request.getParameter("releaseDate");
            String endDateStr = request.getParameter("endDate");

            String redirectTarget = request.getContextPath() + (movieId > 0 ? "/admin/movie?movieId=" + movieId : "/admin/movie");
            String sep = redirectTarget.contains("?") ? "&" : "?";

            if (ValidationUtils.isBlank(title)) {
                response.sendRedirect(redirectTarget + sep + "error=missing_title");
                return;
            }

            int duration;
            try {
                duration = Integer.parseInt(durationStr != null ? durationStr.trim() : "0");
                if (!ValidationUtils.isValidDuration(duration)) {
                    response.sendRedirect(redirectTarget + sep + "error=invalid_duration");
                    return;
                }
            } catch (NumberFormatException e) {
                response.sendRedirect(redirectTarget + sep + "error=invalid_duration");
                return;
            }

            if (!ValidationUtils.isValidAgeRating(ageRating)) {
                response.sendRedirect(redirectTarget + sep + "error=invalid_age_rating");
                return;
            }

            if (!ValidationUtils.isValidMovieStatus(status)) {
                response.sendRedirect(redirectTarget + sep + "error=invalid_status");
                return;
            }

            Date releaseDate = parseDate(releaseDateStr);
            Date endDate = parseDate(endDateStr);
            if (!ValidationUtils.isValidDateRange(releaseDate, endDate)) {
                response.sendRedirect(redirectTarget + sep + "error=invalid_date_range");
                return;
            }

            try {
                Movie movie = readMovie(request);
                String posterUrl = saveUpload(request, "posterFile", "poster");
                String trailerUrl = saveUpload(request, "trailerFile", "trailer");
                if (posterUrl != null) {
                    movie.setPosterUrl(posterUrl);
                }
                if (trailerUrl != null) {
                    movie.setTrailerUrl(trailerUrl);
                }
                if (movie.getMovieId() == 0) {
                    dao.createMovie(movie);
                } else {
                    dao.updateMovie(movie);
                }
                response.sendRedirect(request.getContextPath() + "/admin/movies?success=saved");
            } catch (IllegalArgumentException e) {
                response.sendRedirect(redirectTarget + sep + "error=" + e.getMessage());
            } catch (Exception e) {
                response.sendRedirect(redirectTarget + sep + "error=save_failed");
            }
            return;
        }

        if ("changeStatus".equals(action)) {
            String movieIdParam = request.getParameter("movieId");
            String newStatus = request.getParameter("newStatus");
            if (movieIdParam != null && newStatus != null) {
                try {
                    int movieId = Integer.parseInt(movieIdParam.trim());
                    String targetStatus = newStatus.trim().toUpperCase();
                    if (ValidationUtils.isValidMovieStatus(targetStatus)) {
                        dao.updateMovieStatus(movieId, targetStatus);
                        response.sendRedirect(request.getContextPath() + "/admin/movies?success=statusUpdated");
                        return;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            response.sendRedirect(request.getContextPath() + "/admin/movies?error=invalid_status");
            return;
        }

        response.sendRedirect(request.getContextPath() + "/admin/movies");
    }

    /**
     * Đọc và gán dữ liệu phim từ các tham số trong HttpServletRequest.
     */
    private Movie readMovie(HttpServletRequest request) {
        Movie movie = new Movie();
        String id = request.getParameter("movieId");
        if (id != null && !id.isBlank()) {
            movie.setMovieId(Integer.parseInt(id));
        }
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
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Date.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Lưu trữ tệp tin tải lên (poster hoặc trailer) vào thư mục uploads/movies.
     */
    private String saveUpload(HttpServletRequest request, String partName, String prefix) throws Exception {
        Part part = request.getPart(partName);
        if (part == null || part.getSize() == 0) {
            return null;
        }
        String submitted = part.getSubmittedFileName();
        if (submitted == null || submitted.isBlank()) {
            return null;
        }
        String original = Paths.get(submitted).getFileName().toString();
        String extension = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            extension = original.substring(dot).toLowerCase();
        }
        if ("poster".equals(prefix)) {
            if (!ValidationUtils.isValidImageExtension(original)) {
                throw new IllegalArgumentException("invalid_poster_file");
            }
        } else if ("trailer".equals(prefix)) {
            if (!ValidationUtils.isValidVideoExtension(original)) {
                throw new IllegalArgumentException("invalid_trailer_file");
            }
        } else if (!extension.matches("\\.(jpg|jpeg|png|webp|mp4|webm|ogg)")) {
            return null;
        }
        Path directory = MediaStorage.uploadDirectory(getServletContext(), "movies");
        String fileName = prefix + "-" + UUID.randomUUID() + extension;
        part.write(directory.resolve(fileName).toString());
        return request.getContextPath() + "/uploads/movies/" + fileName;
    }
}
