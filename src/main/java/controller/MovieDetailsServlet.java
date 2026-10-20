package controller;

import dao.CinemaDAO;
import dao.RatingDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Movie;
import model.User;

import java.io.IOException;

/**
 * Controller: Hiển thị thông tin chi tiết của một bộ phim, điểm đánh giá tổng hợp và danh sách suất chiếu của phim.
 * URL ánh xạ: /movie-details
 */
public class MovieDetailsServlet extends HttpServlet {

    private final CinemaDAO cinemaDAO = new CinemaDAO();
    private final RatingDAO ratingDAO = new RatingDAO();

    /**
     * Xử lý yêu cầu HTTP GET: Tải thông tin chi tiết phim, điểm đánh giá và lịch chiếu của phim để hiển thị.
     *
     * @param request  yêu cầu HTTP từ client
     * @param response phản hồi HTTP gửi về client
     * @throws ServletException nếu xảy ra lỗi servlet
     * @throws IOException      nếu xảy ra lỗi vào/ra dữ liệu
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 1. Nhận và kiểm tra tham số mã phim (movieId) từ URL request
        String value = request.getParameter("movieId");
        int movieId;
        try {
            movieId = Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Movie ID is invalid.");
            return;
        }

        // 2. Truy vấn thông tin chi tiết phim từ cơ sở dữ liệu qua CinemaDAO
        Movie movie = cinemaDAO.getMovieById(movieId);
        if (movie == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Movie not found.");
            return;
        }

        // 3. Xác định định danh người dùng đăng nhập hiện tại (nếu có) để lấy đánh giá cá nhân
        Integer currentUserId = null;
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            Object uObj = session.getAttribute("user");
            if (uObj instanceof User) {
                currentUserId = ((User) uObj).getUserId();
            } else {
                try {
                    currentUserId = (Integer) uObj.getClass().getMethod("getUserId").invoke(uObj);
                } catch (Exception ignored) {
                }
            }
        }

        // 4. Lấy thông tin tóm tắt đánh giá (điểm trung bình, số lượng đánh giá, đánh giá của user)
        RatingDAO.MovieRatingSummary ratingSummary = ratingDAO.getRatingSummary(movieId, currentUserId);

        // 5. Gán dữ liệu vào các thuộc tính request
        request.setAttribute("movie", movie);
        request.setAttribute("ratingSummary", ratingSummary);
        request.setAttribute("showtimes", cinemaDAO.getScheduledShowtimesByMovieId(movieId));
        request.setAttribute("nowShowingMovies", cinemaDAO.getNowShowingMovies());

        // 6. Chuyển tiếp tới giao diện chi tiết phim details.jsp
        request.getRequestDispatcher("/movie/details.jsp").forward(request, response);
    }
}
