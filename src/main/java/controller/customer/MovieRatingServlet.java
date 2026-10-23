package controller.customer;

import dao.CinemaDAO;
import dao.RatingDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.entity.Movie;
import model.entity.User;

import java.io.IOException;
import java.util.Locale;

/**
 * Controller: Điều khiển API chấm điểm và lấy dữ liệu thống kê đánh giá phim của người dùng dưới định dạng JSON.
 * URL ánh xạ: /movie/rate
 */
@WebServlet(name = "MovieRatingServlet", urlPatterns = {"/movie/rate"})
public class MovieRatingServlet extends HttpServlet {

    private final CinemaDAO cinemaDAO = new CinemaDAO();
    private final RatingDAO ratingDAO = new RatingDAO();

    /**
     * Phương thức tiện ích: Trích xuất mã ID người dùng từ phiên đăng nhập hiện tại nếu có.
     *
     * @param request yêu cầu HTTP từ client
     * @return userId nếu đã đăng nhập, ngược lại 0
     */
    private int extractUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            return 0;
        }
        Object obj = session.getAttribute("user");
        if (obj instanceof User) {
            return ((User) obj).getUserId();
        }
        try {
            return (Integer) obj.getClass().getMethod("getUserId").invoke(obj);
        } catch (Exception ignored) {
            return 0;
        }
    }

    /**
     * Xử lý yêu cầu HTTP GET: Trả về dữ liệu JSON điểm số đánh giá trung bình và điểm số của người dùng hiện tại.
     *
     * @param request  yêu cầu HTTP từ client
     * @param response phản hồi HTTP gửi về client
     * @throws ServletException nếu xảy ra lỗi servlet
     * @throws IOException      nếu xảy ra lỗi vào/ra dữ liệu
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 1. Thiết lập định dạng dữ liệu trả về là JSON UTF-8
        response.setContentType("application/json;charset=UTF-8");

        // 2. Đọc mã phim từ tham số request
        int movieId = 0;
        try {
            movieId = Integer.parseInt(request.getParameter("movieId"));
        } catch (Exception ignored) {
        }

        if (movieId <= 0) {
            response.getWriter().write("{\"success\":false,\"message\":\"ID phim không hợp lệ.\"}");
            return;
        }

        // 3. Kiểm tra phim có tồn tại trong hệ thống hay không
        Movie movie = cinemaDAO.getMovieById(movieId);
        if (movie == null) {
            response.getWriter().write("{\"success\":false,\"message\":\"Không tìm thấy phim.\"}");
            return;
        }

        // 4. Kiểm tra trạng thái phim và lấy thống kê điểm đánh giá
        boolean comingSoon = movie.isComingSoon();
        int userId = extractUserId(request);
        RatingDAO.MovieRatingSummary summary = ratingDAO.getRatingSummary(movieId, userId > 0 ? userId : null);

        // 5. Đóng gói kết quả thành chuỗi JSON và trả về cho client
        String json = String.format(Locale.US,
                "{\"success\":true,\"isRateable\":%b,\"isComingSoon\":%b,\"averageScore10\":%.1f,\"averageStars5\":%.1f,\"totalVotes\":%d,\"userScore\":%d}",
                !comingSoon, comingSoon, summary.getAverageScore10(), summary.getAverageStars5(), summary.getTotalVotes(), summary.getUserScore()
        );
        response.getWriter().write(json);
    }

    /**
     * Xử lý yêu cầu HTTP POST: Lưu hoặc cập nhật điểm đánh giá sao của người dùng cho bộ phim.
     *
     * @param request  yêu cầu HTTP từ client
     * @param response phản hồi HTTP gửi về client
     * @throws ServletException nếu xảy ra lỗi servlet
     * @throws IOException      nếu xảy ra lỗi vào/ra dữ liệu
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 1. Thiết lập định dạng dữ liệu trả về là JSON UTF-8
        response.setContentType("application/json;charset=UTF-8");

        // 2. Yêu cầu bắt buộc người dùng phải đăng nhập trước khi đánh giá
        int userId = extractUserId(request);
        if (userId <= 0) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"success\":false,\"needLogin\":true,\"message\":\"Vui lòng đăng nhập để bình chọn phim.\"}");
            return;
        }

        // Kiểm tra phân quyền: Chỉ tài khoản khách hàng (CUSTOMER) mới được phép đánh giá phim
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            String role = (String) session.getAttribute("role");
            if (role == null && session.getAttribute("user") instanceof User) {
                role = ((User) session.getAttribute("user")).getRole();
            }
            if (role != null && !"CUSTOMER".equalsIgnoreCase(role)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"success\":false,\"message\":\"Chức năng đánh giá phim chỉ áp dụng cho tài khoản khách hàng.\"}");
                return;
            }
        }

        // 3. Đọc dữ liệu mã phim và số sao bình chọn (từ 1 đến 5 sao)
        int movieId = 0;
        int score = 0;
        try {
            movieId = Integer.parseInt(request.getParameter("movieId"));
            score = Integer.parseInt(request.getParameter("score"));
        } catch (Exception ignored) {
        }

        if (movieId <= 0 || score < 1 || score > 5) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Dữ liệu đánh giá không hợp lệ. Vui lòng chọn từ 1 đến 5 sao.\"}");
            return;
        }

        // 4. Kiểm tra phim tồn tại
        Movie movie = cinemaDAO.getMovieById(movieId);
        if (movie == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{\"success\":false,\"message\":\"Không tìm thấy phim.\"}");
            return;
        }

        // 5. Chặn đánh giá đối với phim sắp chiếu (Coming Soon)
        if (movie.isComingSoon()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Phim sắp chiếu chưa mở cổng đánh giá. Bạn có thể đánh giá sau khi phim chính thức công chiếu!\"}");
            return;
        }

        // 6. Ghi nhận đánh giá vào cơ sở dữ liệu qua RatingDAO
        boolean saved = ratingDAO.saveOrUpdateRating(movieId, userId, score);
        if (!saved) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"success\":false,\"message\":\"Không thể lưu đánh giá. Vui lòng thử lại sau.\"}");
            return;
        }

        // 7. Lấy lại tóm tắt điểm số mới cập nhật và phản hồi JSON thành công
        RatingDAO.MovieRatingSummary summary = ratingDAO.getRatingSummary(movieId, userId);
        String json = String.format(Locale.US,
                "{\"success\":true,\"message\":\"Cảm ơn bạn đã bình chọn %d/5 sao! Đánh giá đã được lưu vào hệ thống.\",\"averageScore10\":%.1f,\"averageStars5\":%.1f,\"totalVotes\":%d,\"userScore\":%d}",
                score, summary.getAverageScore10(), summary.getAverageStars5(), summary.getTotalVotes(), summary.getUserScore()
        );
        response.getWriter().write(json);
    }
}
