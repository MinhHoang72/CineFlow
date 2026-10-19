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
 * Điều khiển phân hệ Quản lý danh mục phim dành cho Quản trị viên (Admin).
 * Chức năng:
 * - Xem toàn bộ danh sách phim kèm số lượng rạp đã phân phối (/admin/movies)
 * - Mở form thêm phim mới hoặc chỉnh sửa thông tin phim có sẵn (/admin/movie)
 * - Lưu thông tin phim, xác thực dữ liệu và upload file Poster / Trailer vật lý
 * - Cập nhật trạng thái phát hành (Coming Soon, Now Showing, Ended, Archived)
 */
@MultipartConfig(maxFileSize = 50 * 1024 * 1024, maxRequestSize = 60 * 1024 * 1024)
public class MovieManagementServlet extends HttpServlet {

    private final CinemaDAO dao = new CinemaDAO();

    /**
     * Kiểm tra quyền truy cập: Chỉ tài khoản có vai trò Quản trị viên (ADMIN) mới được thao tác.
     *
     * @param session phiên làm việc hiện tại của người dùng
     * @return true nếu là Admin hợp lệ, ngược lại false
     */
    private boolean allowed(HttpSession session) {
        if (session == null) {
            return false;
        }
        // Kiểm tra quyền từ attribute role trong session
        Object roleObj = session.getAttribute("role");
        if (roleObj instanceof String && "ADMIN".equalsIgnoreCase((String) roleObj)) {
            return true;
        }
        // Kiểm tra quyền dự phòng từ đối tượng User trong session
        Object userObj = session.getAttribute("user");
        if (userObj instanceof User) {
            String role = ((User) userObj).getRole();
            return role != null && "ADMIN".equalsIgnoreCase(role);
        }
        return false;
    }

    /**
     * Xử lý yêu cầu HTTP GET: Điều hướng hiển thị danh mục phim hoặc mở form tạo/chỉnh sửa phim.
     *
     * @param request  yêu cầu HTTP từ client
     * @param response phản hồi HTTP gửi về client
     * @throws ServletException nếu xảy ra lỗi servlet
     * @throws IOException      nếu xảy ra lỗi vào/ra dữ liệu
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 1. Kiểm tra quyền Admin (nếu không phải Admin thì trả về mã lỗi 403 Forbidden)
        if (!allowed(request.getSession(false))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Admin được quản lý danh mục phim.");
            return;
        }

        // 2. Nếu là URL /admin/movies: Lấy toàn bộ danh sách phim và hiển thị trang danh mục
        if ("/admin/movies".equals(request.getServletPath())) {
            request.setAttribute("movies", dao.getAllMovies());
            request.getRequestDispatcher("/admin/movies.jsp").forward(request, response);
            return;
        }

        // 3. Nếu là URL /admin/movie: Kiểm tra xem là form Chỉnh sửa (có movieId) hay Thêm mới
        String id = request.getParameter("movieId");
        if (id != null && !id.isBlank()) {
            try {
                int movieId = Integer.parseInt(id.trim());
                Movie movie = dao.getMovieById(movieId);
                if (movie == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy phim yêu cầu.");
                    return;
                }
                // Đưa đối tượng phim vào request để form hiển thị sẵn dữ liệu cũ
                request.setAttribute("movie", movie);
            } catch (NumberFormatException exception) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã phim không hợp lệ.");
                return;
            }
        }

        // 4. Chuyển tiếp (forward) sang giao diện form nhập liệu (movie-management.jsp)
        request.getRequestDispatcher("/admin/movie-management.jsp").forward(request, response);
    }

    /**
     * Xử lý yêu cầu HTTP POST: Lưu thông tin phim mới/cập nhật và cập nhật trạng thái phát hành.
     *
     * @param request  yêu cầu HTTP từ client
     * @param response phản hồi HTTP gửi về client
     * @throws IOException nếu xảy ra lỗi vào/ra dữ liệu
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 1. Kiểm tra quyền hạn Admin trước khi xử lý bất kỳ thao tác thay đổi dữ liệu nào
        if (!allowed(request.getSession(false))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String action = request.getParameter("action");

        // 2. Xử lý hành động LƯU PHIM (Thêm mới hoặc Cập nhật thông tin phim)
        if ("save".equals(action)) {
            // 2.1. Đọc mã định danh phim (movieId = 0 nếu là thêm mới)
            String movieIdStr = request.getParameter("movieId");
            int movieId = 0;
            if (movieIdStr != null && !movieIdStr.isBlank()) {
                try {
                    movieId = Integer.parseInt(movieIdStr.trim());
                } catch (NumberFormatException ignored) {
                }
            }

            // 2.2. Đọc các trường thông tin cơ bản từ biểu mẫu
            String title = request.getParameter("title");
            String durationStr = request.getParameter("duration");
            String ageRating = request.getParameter("ageRating");
            String status = request.getParameter("status");
            String releaseDateStr = request.getParameter("releaseDate");
            String endDateStr = request.getParameter("endDate");

            // Xác định URL chuyển hướng khi có lỗi nhập liệu
            String redirectTarget = request.getContextPath() + (movieId > 0 ? "/admin/movie?movieId=" + movieId : "/admin/movie");
            String sep = redirectTarget.contains("?") ? "&" : "?";

            // 2.3. Kiểm tra tính hợp lệ (Validation) của tiêu đề phim
            if (ValidationUtils.isBlank(title)) {
                response.sendRedirect(redirectTarget + sep + "error=missing_title");
                return;
            }

            // 2.4. Kiểm tra thời lượng phim (phải là số nguyên dương và không quá 300 phút)
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

            // 2.5. Kiểm tra phân loại độ tuổi hợp lệ (P, K, T13, T16, T18, C)
            if (!ValidationUtils.isValidAgeRating(ageRating)) {
                response.sendRedirect(redirectTarget + sep + "error=invalid_age_rating");
                return;
            }

            // 2.6. Kiểm tra trạng thái phát hành hợp lệ (COMING_SOON, NOW_SHOWING, ENDED, ARCHIVED)
            if (!ValidationUtils.isValidMovieStatus(status)) {
                response.sendRedirect(redirectTarget + sep + "error=invalid_status");
                return;
            }

            // 2.7. Kiểm tra khoảng thời gian chiếu (Ngày kết thúc phải lớn hơn hoặc bằng ngày khởi chiếu)
            Date releaseDate = parseDate(releaseDateStr);
            Date endDate = parseDate(endDateStr);
            Date today = new Date(System.currentTimeMillis());

            // Nghiệp vụ: Nếu trạng thái được set là ENDED, tự động gán ngày kết thúc (EndDate) là hôm nay nếu chưa có hoặc đang ở tương lai
            if ("ENDED".equals(status)) {
                if (endDate == null || endDate.after(today)) {
                    endDate = today;
                }
            }

            if (!ValidationUtils.isValidDateRange(releaseDate, endDate)) {
                response.sendRedirect(redirectTarget + sep + "error=invalid_date_range");
                return;
            }

            // Nghiệp vụ trạng thái theo ngày phát hành (COMING_SOON không được ở quá khứ, NOW_SHOWING không được ở tương lai)
            String dateConsistencyError = ValidationUtils.validateMovieStatusDateConsistency(status, releaseDate, today);
            if (dateConsistencyError != null) {
                response.sendRedirect(redirectTarget + sep + "error=" + dateConsistencyError);
                return;
            }

            try {
                // 2.8. Đóng gói thông tin form vào đối tượng Movie POJO
                Movie movie = readMovie(request);
                movie.setReleaseDate(releaseDate);
                movie.setEndDate(endDate);

                // 2.9. Xử lý lưu trữ tệp tin Poster và Trailer tải lên thư mục máy chủ nếu có
                String posterUrl = saveUpload(request, "posterFile", "poster");
                String trailerUrl = saveUpload(request, "trailerFile", "trailer");
                if (posterUrl != null) {
                    movie.setPosterUrl(posterUrl);
                }
                if (trailerUrl != null) {
                    movie.setTrailerUrl(trailerUrl);
                }

                // 2.10. Phân nhánh: Tạo mới (INSERT) nếu movieId == 0, Cập nhật (UPDATE) nếu movieId > 0
                if (movie.getMovieId() == 0) {
                    dao.createMovie(movie);
                } else {
                    dao.updateMovie(movie);
                }

                // 2.11. Chuyển hướng về trang danh sách phim kèm thông báo thành công
                response.sendRedirect(request.getContextPath() + "/admin/movies?success=saved");
            } catch (IllegalArgumentException e) {
                response.sendRedirect(redirectTarget + sep + "error=" + e.getMessage());
            } catch (Exception e) {
                response.sendRedirect(redirectTarget + sep + "error=save_failed");
            }
            return;
        }

        // 3. Xử lý hành động ĐỔI NHANH TRẠNG THÁI PHÁT HÀNH (Now Showing, Coming Soon, Ended, Archived)
        if ("changeStatus".equals(action)) {
            String movieIdParam = request.getParameter("movieId");
            String newStatus = request.getParameter("newStatus");
            if (movieIdParam != null && newStatus != null) {
                try {
                    int movieId = Integer.parseInt(movieIdParam.trim());
                    String targetStatus = newStatus.trim().toUpperCase();
                    if (ValidationUtils.isValidMovieStatus(targetStatus)) {
                        if ("ENDED".equals(targetStatus)) {
                            dao.endMovie(movieId);
                        } else {
                            dao.updateMovieStatus(movieId, targetStatus);
                        }
                        response.sendRedirect(request.getContextPath() + "/admin/movies?success=statusUpdated");
                        return;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            response.sendRedirect(request.getContextPath() + "/admin/movies?error=invalid_status");
            return;
        }

        // Mặc định chuyển hướng về trang danh mục phim nếu không khớp hành động nào
        response.sendRedirect(request.getContextPath() + "/admin/movies");
    }

    /**
     * Đọc và trích xuất dữ liệu của bộ phim từ các tham số trong HttpServletRequest.
     *
     * @param request đối tượng yêu cầu HTTP
     * @return đối tượng Movie đã được thiết lập đầy đủ thông tin
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

    /**
     * Chuyển đổi chuỗi ngày tháng dạng YYYY-MM-DD sang đối tượng java.sql.Date.
     *
     * @param value chuỗi ngày tháng đầu vào
     * @return đối tượng java.sql.Date hoặc null nếu chuỗi rỗng/không hợp lệ
     */
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
     * Lưu trữ tệp tin tải lên (poster hoặc trailer) vào thư mục uploads/movies trên máy chủ.
     * Sử dụng UUID ngẫu nhiên để đảm bảo tên tệp không bao giờ bị trùng lặp.
     *
     * @param request  đối tượng yêu cầu HTTP chứa multipart data
     * @param partName tên trường upload trong biểu mẫu (posterFile hoặc trailerFile)
     * @param prefix   tiền tố đặt tên file (poster hoặc trailer)
     * @return đường dẫn URL tương đối dẫn đến tệp đã lưu trên máy chủ
     * @throws Exception khi tệp tin không hợp lệ hoặc lỗi ghi đĩa
     */
    private String saveUpload(HttpServletRequest request, String partName, String prefix) throws Exception {
        // 1. Lấy phần dữ liệu tải lên (Part) từ request
        Part part = request.getPart(partName);
        if (part == null || part.getSize() == 0) {
            return null;
        }

        // 2. Lấy tên tệp tin gốc do người dùng tải lên
        String submitted = part.getSubmittedFileName();
        if (submitted == null || submitted.isBlank()) {
            return null;
        }

        // 3. Trích xuất phần đuôi mở rộng (extension) của tệp tin
        String original = Paths.get(submitted).getFileName().toString();
        String extension = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            extension = original.substring(dot).toLowerCase();
        }

        // 4. Kiểm tra phần mở rộng tệp tin theo quy chuẩn an toàn
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

        // 5. Xác định thư mục lưu trữ trên máy chủ và tạo tên tệp ngẫu nhiên bằng UUID
        Path directory = MediaStorage.uploadDirectory(getServletContext(), "movies");
        String fileName = prefix + "-" + UUID.randomUUID() + extension;

        // 6. Ghi dữ liệu nhị phân xuống đĩa cứng máy chủ
        part.write(directory.resolve(fileName).toString());

        // 7. Trả về đường dẫn truy cập phục vụ hiển thị trên giao diện web
        return request.getContextPath() + "/uploads/movies/" + fileName;
    }
}
