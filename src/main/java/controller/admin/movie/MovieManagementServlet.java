package controller.admin.movie;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import model.entity.Movie;
import model.entity.User;
import service.MovieService;
import utils.MediaStorage;
import utils.ValidationUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.util.UUID;

/**
 * Controller: Quản lý danh mục phim dành cho Quản trị viên (Admin).
 *
 * <p>Kiến trúc 3 lớp: {@code MovieManagementServlet -> MovieService -> MovieDAO}</p>
 */
@WebServlet(name = "MovieManagementServlet", urlPatterns = {"/admin/movies", "/admin/movie"})
@MultipartConfig(maxFileSize = 50 * 1024 * 1024, maxRequestSize = 60 * 1024 * 1024)
public class MovieManagementServlet extends HttpServlet {

    private final MovieService movieService = new MovieService();

    /**
     * Kiểm tra phiên đăng nhập có quyền ADMIN hay không.
     */
    private boolean allowed(HttpSession session) {
        if (session == null) return false;
        Object roleObj = session.getAttribute("role");
        if (roleObj instanceof String && "ADMIN".equalsIgnoreCase((String) roleObj)) return true;
        Object userObj = session.getAttribute("user");
        return userObj instanceof User && "ADMIN".equalsIgnoreCase(((User) userObj).getRole());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!allowed(request.getSession(false))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Admin được quản lý danh mục phim.");
            return;
        }

        // Danh sách phim
        if ("/admin/movies".equals(request.getServletPath())) {
            MovieService.MovieKpi kpi = movieService.getMovieKpi();
            request.setAttribute("movies", movieService.getAllMovies());
            request.setAttribute("totalMovies", kpi.total());
            request.setAttribute("comingSoonCount", kpi.comingSoon());
            request.setAttribute("nowShowingCount", kpi.nowShowing());
            request.setAttribute("endedCount", kpi.ended());
            request.setAttribute("archivedCount", kpi.archived());
            request.getRequestDispatcher("/WEB-INF/views/admin/movie/movieList.jsp").forward(request, response);
            return;
        }

        // Form thêm mới hoặc chỉnh sửa phim (/admin/movie)
        String idParam = request.getParameter("movieId");
        if (idParam != null && !idParam.isBlank()) {
            try {
                int movieId = Integer.parseInt(idParam.trim());
                Movie movie = movieService.getMovieById(movieId);
                if (movie == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy phim yêu cầu.");
                    return;
                }
                request.setAttribute("movie", movie);

                // Thống kê số suất chiếu và vé đã bán để cảnh báo khi đổi trạng thái
                int[] stats = movieService.getUpcomingStats(movieId);
                request.setAttribute("upcomingShowtimesCount", stats[0]);
                request.setAttribute("soldTicketsCount", stats[1]);
            } catch (NumberFormatException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã phim không hợp lệ.");
                return;
            }
        }

        request.getRequestDispatcher("/WEB-INF/views/admin/movie/movieForm.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        if (!allowed(request.getSession(false))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        handleSaveMovie(request, response);
    }

    /**
     * Xử lý lưu thông tin phim (tạo mới hoặc cập nhật) kèm tải lên Poster / Trailer.
     */
    private void handleSaveMovie(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int movieId = parseIntOrZero(request.getParameter("movieId"));
        String posterUrl = null;
        String trailerUrl = null;
        try {
            Movie movie = readMovieFromRequest(request);

            posterUrl = saveUpload(request, "posterFile", "poster");
            trailerUrl = saveUpload(request, "trailerFile", "trailer");
            if (posterUrl != null) movie.setPosterUrl(posterUrl);
            if (trailerUrl != null) movie.setTrailerUrl(trailerUrl);

            int savedId = movieService.saveMovie(movie);
            if (savedId <= 0) {
                deleteFiles(posterUrl, trailerUrl);
                redirectError(request, response, movieId, "save_failed");
                return;
            }

            response.sendRedirect(request.getContextPath() + "/admin/movies?success=saved");
        } catch (IllegalArgumentException e) {
            deleteFiles(posterUrl, trailerUrl);
            redirectError(request, response, movieId, e.getMessage());
        } catch (Exception e) {
            deleteFiles(posterUrl, trailerUrl);
            redirectError(request, response, movieId, "save_failed");
        }
    }

    /**
     * Đọc các tham số từ request và đóng gói vào đối tượng Movie.
     */
    private Movie readMovieFromRequest(HttpServletRequest request) {
        String title = request.getParameter("title");
        if (ValidationUtil.isBlank(title)) {
            throw new IllegalArgumentException("missing_title");
        }

        String durationStr = request.getParameter("duration");
        int duration;
        try {
            duration = Integer.parseInt(durationStr != null ? durationStr.trim() : "0");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid_duration");
        }

        String releaseDateStr = request.getParameter("releaseDate");
        String endDateStr = request.getParameter("endDate");
        Date releaseDate = parseDate(releaseDateStr);
        Date endDate = parseDate(endDateStr);
        if ((releaseDateStr != null && !releaseDateStr.isBlank() && releaseDate == null)
                || (endDateStr != null && !endDateStr.isBlank() && endDate == null)) {
            throw new IllegalArgumentException("invalid_date_format");
        }

        Movie movie = new Movie();
        movie.setMovieId(parseIntOrZero(request.getParameter("movieId")));
        movie.setTitle(title.trim());
        movie.setDescription(request.getParameter("description"));
        movie.setDuration(duration);
        movie.setGenre(request.getParameter("genre"));
        movie.setDirector(request.getParameter("director"));
        movie.setCast(request.getParameter("cast"));
        movie.setLanguage(request.getParameter("language"));
        movie.setReleaseDate(releaseDate);
        movie.setEndDate(endDate);
        movie.setPosterUrl(request.getParameter("posterUrl"));
        movie.setTrailerUrl(request.getParameter("trailerUrl"));
        movie.setAgeRating(request.getParameter("ageRating"));
        movie.setStatus(request.getParameter("status"));
        return movie;
    }

    /**
     * Chuyển hướng về trang form kèm mã lỗi.
     */
    private void redirectError(HttpServletRequest request, HttpServletResponse response, int movieId, String error) throws IOException {
        String url = request.getContextPath() + (movieId > 0 ? "/admin/movie?movieId=" + movieId + "&error=" : "/admin/movie?error=") + error;
        response.sendRedirect(url);
    }

    /**
     * Xóa các tệp tải lên nếu quá trình lưu thất bại (tránh rác trên máy chủ).
     */
    private void deleteFiles(String... urls) {
        for (String url : urls) {
                    // Bỏ qua nếu không có URL
            if (url == null) continue;
                    // Xác định thư mục chứa file
            String category = url.contains("/uploads/video_trailer/") ? "video_trailer"
                    : url.contains("/uploads/poster/") ? "poster" : null;
                    // Không phải Poster hoặc Trailer thì bỏ qua
            if (category == null) continue;
            try {
                     // Lấy tên file
                String fileName = url.substring(url.lastIndexOf('/') + 1);
                    // Xóa file nếu tồn tại
                Files.deleteIfExists(MediaStorage.uploadDirectory(getServletContext(), category).resolve(fileName));
            } catch (Exception ignored) {}
        }
    }

    /**
     * Kiểm tra định dạng, dung lượng và lưu tệp tải lên (Poster hoặc Trailer).
     */
    private String saveUpload(HttpServletRequest request, String partName, String prefix) throws Exception {
        Part part = request.getPart(partName);
        if (part == null || part.getSize() == 0) return null;

        String submitted = part.getSubmittedFileName();
        if (submitted == null || submitted.isBlank()) return null;

        String original = Paths.get(submitted).getFileName().toString();
        String extension = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) extension = original.substring(dot).toLowerCase();

        boolean isPoster = "poster".equals(prefix);
        if (isPoster) {
            if (part.getSize() > 5L * 1024 * 1024) throw new IllegalArgumentException("poster_too_large");
            if (part.getContentType() == null || !part.getContentType().toLowerCase().startsWith("image/")
                    || !ValidationUtil.isValidImageExtension(original)) {
                throw new IllegalArgumentException("invalid_poster_file");
            }
        } else if ("trailer".equals(prefix)) {
            if (part.getSize() > 50L * 1024 * 1024) throw new IllegalArgumentException("trailer_too_large");
            if (part.getContentType() == null || !part.getContentType().toLowerCase().startsWith("video/")
                    || !ValidationUtil.isValidVideoExtension(original)) {
                throw new IllegalArgumentException("invalid_trailer_file");
            }
        }

        String category = isPoster ? "poster" : "video_trailer";
        Path directory = MediaStorage.uploadDirectory(getServletContext(), category);
        String fileName = prefix + "-" + UUID.randomUUID() + extension;
        part.write(directory.resolve(fileName).toString());
        return request.getContextPath() + "/uploads/" + category + "/" + fileName;
    }

    private Date parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Date.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private int parseIntOrZero(String value) {
        if (value == null || value.isBlank()) return 0;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
