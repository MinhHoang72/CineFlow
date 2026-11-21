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
 * Controller: Điều khiển phân hệ Quản lý danh mục phim dành cho Quản trị viên (Admin).
 *
 * <p>Chức năng chính:</p>
 * <ul>
 *   <li>Xem danh mục phim ({@code /admin/movies}) kèm tỷ lệ phân phối rạp.</li>
 *   <li>Mở biểu mẫu thêm mới hoặc chỉnh sửa thông tin phim ({@code /admin/movie}).</li>
 *   <li>Tải lên áp phích (Poster) và video giới thiệu (Trailer) chuẩn Multipart.</li>
 *   <li>Lưu thông tin phim (tạo mới / cập nhật) và cập nhật trạng thái phát hành.</li>
 * </ul>
 *
 * <p>Kiến trúc 3 lớp: {@code MovieManagementServlet -> MovieService -> MovieDAO}</p>
 */
@WebServlet(name = "MovieManagementServlet", urlPatterns = {"/admin/movies", "/admin/movie"})
@MultipartConfig(maxFileSize = 50 * 1024 * 1024, maxRequestSize = 60 * 1024 * 1024)
public class MovieManagementServlet extends HttpServlet {

    /** Tầng nghiệp vụ xử lý danh mục và vòng đời phim */
    private final MovieService movieService = new MovieService();

    /**
     * Kiểm tra phiên đăng nhập hiện tại có quyền Quản trị viên (ADMIN) hay không.
     *
     * @param session phiên làm việc HTTP hiện tại
     * @return true nếu người dùng là ADMIN; ngược lại false
     */
    private boolean allowed(HttpSession session) {
        if (session == null) return false;
        Object roleObj = session.getAttribute("role");
        if (roleObj instanceof String && "ADMIN".equalsIgnoreCase((String) roleObj)) return true;
        Object userObj = session.getAttribute("user");
        return userObj instanceof User && "ADMIN".equalsIgnoreCase(((User) userObj).getRole());
    }

    /**
     * Xử lý yêu cầu HTTP GET:
     * <ul>
     *   <li>{@code /admin/movies}: Nạp toàn bộ danh sách phim và chuyển tới {@code movieList.jsp}.</li>
     *   <li>{@code /admin/movie}: Nạp thông tin phim cần sửa (nếu có {@code movieId}), kiểm tra còn suất chiếu tương lai không, khôi phục bản nháp nếu lưu lỗi rồi chuyển tới {@code movieForm.jsp}.</li>
     * </ul>
     *
     * @param request  yêu cầu HTTP từ client
     * @param response phản hồi HTTP trả về client
     * @throws ServletException nếu xảy ra lỗi trong quá trình forward JSP
     * @throws IOException      nếu xảy ra lỗi đọc/ghi luồng HTTP
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!allowed(request.getSession(false))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Admin được quản lý danh mục phim.");
            return;
        }

        if ("/admin/movies".equals(request.getServletPath())) {
            request.setAttribute("movies", movieService.getAllMovies());
            request.getRequestDispatcher("/WEB-INF/views/admin/movie/movieList.jsp").forward(request, response);
            return;
        }

        // /admin/movie: Form thêm mới hoặc chỉnh sửa
        String idParam = request.getParameter("movieId");
        int targetMovieId = 0;
        if (idParam != null && !idParam.isBlank()) {
            try {
                int movieId = Integer.parseInt(idParam.trim());
                Movie movie = movieService.getMovieById(movieId);
                if (movie == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy phim yêu cầu.");
                    return;
                }
                request.setAttribute("movie", movie);
                targetMovieId = movieId;
            } catch (NumberFormatException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã phim không hợp lệ.");
                return;
            }
        }

        // Khôi phục dữ liệu nháp nếu lưu lỗi
        if (request.getParameter("error") != null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                Object draft = session.getAttribute("movieFormDraft");
                if (draft instanceof Movie) {
                    request.setAttribute("movie", draft);
                    if (((Movie) draft).getMovieId() > 0) {
                        targetMovieId = ((Movie) draft).getMovieId();
                    }
                    session.removeAttribute("movieFormDraft");
                }
            }
        }

        if (targetMovieId > 0) {
            request.setAttribute("hasUpcomingShowtimes", movieService.hasUpcomingShowtimes(targetMovieId));
        }

        request.getRequestDispatcher("/WEB-INF/views/admin/movie/movieForm.jsp").forward(request, response);
    }

    /**
     * Xử lý yêu cầu HTTP POST: điều phối hành động {@code save} (lưu phim) hoặc {@code changeStatus} (đổi trạng thái).
     *
     * @param request  yêu cầu HTTP từ client
     * @param response phản hồi HTTP trả về client
     * @throws IOException nếu xảy ra lỗi chuyển hướng hoặc gửi mã lỗi
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        if (!allowed(request.getSession(false))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String action = request.getParameter("action");
        if ("save".equals(action)) {
            handleSaveMovie(request, response);
        } else if ("changeStatus".equals(action)) {
            handleChangeStatus(request, response);
        } else {
            response.sendRedirect(request.getContextPath() + "/admin/movies");
        }
    }

    /**
     * Xử lý lưu thông tin phim (tạo mới hoặc cập nhật) kèm upload Poster/Trailer và cơ chế rollback tệp khi lỗi.
     *
     * @param request  yêu cầu HTTP chứa dữ liệu form multipart
     * @param response phản hồi HTTP dùng để chuyển hướng kèm thông báo kết quả
     * @throws IOException nếu xảy ra lỗi chuyển hướng
     */
    private void handleSaveMovie(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int movieId = parseIntOrZero(request.getParameter("movieId"));
        String title = request.getParameter("title");
        String durationStr = request.getParameter("duration");
        String releaseDateStr = request.getParameter("releaseDate");
        String endDateStr = request.getParameter("endDate");

        String redirectTarget = request.getContextPath() + (movieId > 0 ? "/admin/movie?movieId=" + movieId : "/admin/movie");
        String sep = redirectTarget.contains("?") ? "&" : "?";

        // Lưu bản nháp vào session để giữ dữ liệu nếu lỗi
        Movie draft = readMovieFromRequest(request, parseIntOrZero(durationStr));
        draft.setReleaseDate(parseDate(releaseDateStr));
        draft.setEndDate(parseDate(endDateStr));
        request.getSession(true).setAttribute("movieFormDraft", draft);

        if (ValidationUtil.isBlank(title)) {
            response.sendRedirect(redirectTarget + sep + "error=missing_title");
            return;
        }

        int duration;
        try {
            duration = Integer.parseInt(durationStr != null ? durationStr.trim() : "0");
        } catch (NumberFormatException e) {
            response.sendRedirect(redirectTarget + sep + "error=invalid_duration");
            return;
        }

        Date releaseDate = parseDate(releaseDateStr);
        Date endDate = parseDate(endDateStr);
        if ((releaseDateStr != null && !releaseDateStr.isBlank() && releaseDate == null)
                || (endDateStr != null && !endDateStr.isBlank() && endDate == null)) {
            response.sendRedirect(redirectTarget + sep + "error=invalid_date_format");
            return;
        }

        String posterUrl = null;
        String trailerUrl = null;
        try {
            Movie movie = readMovieFromRequest(request, duration);
            movie.setReleaseDate(releaseDate);
            movie.setEndDate(endDate);

            posterUrl = saveUpload(request, "posterFile", "poster");
            trailerUrl = saveUpload(request, "trailerFile", "trailer");
            if (posterUrl != null) movie.setPosterUrl(posterUrl);
            if (trailerUrl != null) movie.setTrailerUrl(trailerUrl);

            int savedId = movieService.saveMovie(movie);
            if (savedId <= 0) {
                deleteUploadedFile(posterUrl);
                deleteUploadedFile(trailerUrl);
                response.sendRedirect(redirectTarget + sep + "error=save_failed");
                return;
            }

            request.getSession(false).removeAttribute("movieFormDraft");
            response.sendRedirect(request.getContextPath() + "/admin/movies?success=saved");
        } catch (IllegalArgumentException e) {
            deleteUploadedFile(posterUrl);
            deleteUploadedFile(trailerUrl);
            response.sendRedirect(redirectTarget + sep + "error=" + e.getMessage());
        } catch (Exception e) {
            deleteUploadedFile(posterUrl);
            deleteUploadedFile(trailerUrl);
            response.sendRedirect(redirectTarget + sep + "error=save_failed");
        }
    }

    /**
     * Xử lý đổi nhanh trạng thái phát hành phim.
     *
     * @param request  yêu cầu HTTP chứa {@code movieId} và {@code newStatus}
     * @param response phản hồi HTTP để chuyển hướng về danh mục phim
     * @throws IOException nếu xảy ra lỗi chuyển hướng
     */
    private void handleChangeStatus(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String movieIdParam = request.getParameter("movieId");
        String newStatus = request.getParameter("newStatus");
        if (movieIdParam != null && newStatus != null) {
            try {
                int movieId = Integer.parseInt(movieIdParam.trim());
                String targetStatus = newStatus.trim().toUpperCase();
                if (ValidationUtil.isValidMovieStatus(targetStatus)) {
                    try {
                        boolean ok = movieService.updateMovieStatus(movieId, targetStatus);
                        if (ok) {
                            response.sendRedirect(request.getContextPath() + "/admin/movies?success=statusUpdated");
                            return;
                        }
                    } catch (IllegalArgumentException e) {
                        response.sendRedirect(request.getContextPath() + "/admin/movies?error=" + e.getMessage());
                        return;
                    }
                }
            } catch (NumberFormatException ignored) {}
        }
        response.sendRedirect(request.getContextPath() + "/admin/movies?error=invalid_status");
    }

    /**
     * Trích xuất dữ liệu các trường từ form request sang đối tượng thực thể {@link Movie}.
     *
     * @param request  yêu cầu HTTP chứa các tham số biểu mẫu
     * @param duration thời lượng phim đã phân tích (tính bằng phút)
     * @return đối tượng {@link Movie} đã nạp dữ liệu
     */
    private Movie readMovieFromRequest(HttpServletRequest request, int duration) {
        Movie movie = new Movie();
        movie.setMovieId(parseIntOrZero(request.getParameter("movieId")));
        movie.setTitle(request.getParameter("title"));
        movie.setDescription(request.getParameter("description"));
        movie.setDuration(duration);
        movie.setGenre(request.getParameter("genre"));
        movie.setDirector(request.getParameter("director"));
        movie.setCast(request.getParameter("cast"));
        movie.setLanguage(request.getParameter("language"));
        movie.setPosterUrl(request.getParameter("posterUrl"));
        movie.setTrailerUrl(request.getParameter("trailerUrl"));
        movie.setAgeRating(request.getParameter("ageRating"));
        movie.setStatus(request.getParameter("status"));
        return movie;
    }

    /**
     * Kiểm tra định dạng, dung lượng và lưu tệp tải lên (Poster hoặc Trailer) vào thư mục lưu trữ.
     *
     * @param request  yêu cầu HTTP dạng multipart
     * @param partName tên trường input file ({@code posterFile} hoặc {@code trailerFile})
     * @param prefix   tiền tố tên tệp lưu trữ ({@code poster} hoặc {@code trailer})
     * @return đường dẫn URL công khai của tệp vừa lưu, hoặc {@code null} nếu người dùng không tải tệp mới
     * @throws Exception nếu tệp sai định dạng, vượt dung lượng hoặc lỗi ghi ổ đĩa
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

    /**
     * Xóa tệp vừa tải lên trên ổ đĩa nếu quá trình ghi cơ sở dữ liệu thất bại (tránh tệp rác).
     *
     * @param url đường dẫn URL của tệp cần xóa
     */
    private void deleteUploadedFile(String url) {
        if (url == null) return;
        String category = url.contains("/uploads/video_trailer/") ? "video_trailer"
                : url.contains("/uploads/poster/") ? "poster" : null;
        if (category == null) return;
        try {
            String fileName = url.substring(url.lastIndexOf('/') + 1);
            Files.deleteIfExists(MediaStorage.uploadDirectory(getServletContext(), category).resolve(fileName));
        } catch (Exception ignored) {}
    }

    /**
     * Chuyển đổi chuỗi định dạng {@code yyyy-MM-dd} sang {@link Date} an toàn.
     *
     * @param value chuỗi ngày cần chuyển đổi
     * @return đối tượng {@link Date}, hoặc {@code null} nếu rỗng/sai định dạng
     */
    private Date parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Date.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Chuyển đổi chuỗi sang số nguyên, trả về {@code 0} nếu rỗng hoặc không hợp lệ.
     *
     * @param value chuỗi số cần phân tích
     * @return giá trị số nguyên hoặc {@code 0}
     */
    private int parseIntOrZero(String value) {
        if (value == null || value.isBlank()) return 0;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
