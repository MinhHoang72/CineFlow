package controller.movie;

import dao.CinemaDAO;
import dao.RatingDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Locale;
import model.Movie;
import model.User;

public class MovieRatingServlet extends HttpServlet {

    private final CinemaDAO cinemaDAO = new CinemaDAO();
    private final RatingDAO ratingDAO = new RatingDAO();

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

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");

        int movieId = 0;
        try {
            movieId = Integer.parseInt(request.getParameter("movieId"));
        } catch (Exception ignored) {}

        if (movieId <= 0) {
            response.getWriter().write("{\"success\":false,\"message\":\"ID phim không hợp lệ.\"}");
            return;
        }

        Movie movie = cinemaDAO.getMovieById(movieId);
        if (movie == null) {
            response.getWriter().write("{\"success\":false,\"message\":\"Không tìm thấy phim.\"}");
            return;
        }

        boolean comingSoon = movie.isComingSoon();
        int userId = extractUserId(request);
        RatingDAO.MovieRatingSummary summary = ratingDAO.getRatingSummary(movieId, userId > 0 ? userId : null);

        String json = String.format(Locale.US,
            "{\"success\":true,\"isRateable\":%b,\"isComingSoon\":%b,\"averageScore10\":%.1f,\"averageStars5\":%.1f,\"totalVotes\":%d,\"userScore\":%d}",
            !comingSoon, comingSoon, summary.getAverageScore10(), summary.getAverageStars5(), summary.getTotalVotes(), summary.getUserScore()
        );
        response.getWriter().write(json);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");

        int userId = extractUserId(request);
        if (userId <= 0) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"success\":false,\"needLogin\":true,\"message\":\"Vui lòng đăng nhập để bình chọn phim.\"}");
            return;
        }

        int movieId = 0;
        int score = 0;
        try {
            movieId = Integer.parseInt(request.getParameter("movieId"));
            score = Integer.parseInt(request.getParameter("score"));
        } catch (Exception ignored) {}

        if (movieId <= 0 || score < 1 || score > 5) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Dữ liệu đánh giá không hợp lệ. Vui lòng chọn từ 1 đến 5 sao.\"}");
            return;
        }

        Movie movie = cinemaDAO.getMovieById(movieId);
        if (movie == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{\"success\":false,\"message\":\"Không tìm thấy phim.\"}");
            return;
        }

        if (movie.isComingSoon()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Phim sắp chiếu chưa mở cổng đánh giá. Bạn có thể đánh giá sau khi phim chính thức công chiếu!\"}");
            return;
        }

        boolean saved = ratingDAO.saveOrUpdateRating(movieId, userId, score);
        if (!saved) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"success\":false,\"message\":\"Không thể lưu đánh giá. Vui lòng thử lại sau.\"}");
            return;
        }

        RatingDAO.MovieRatingSummary summary = ratingDAO.getRatingSummary(movieId, userId);
        String json = String.format(Locale.US,
            "{\"success\":true,\"message\":\"Cảm ơn bạn đã bình chọn %d/5 sao! Đánh giá đã được lưu vào hệ thống.\",\"averageScore10\":%.1f,\"averageStars5\":%.1f,\"totalVotes\":%d,\"userScore\":%d}",
            score, summary.getAverageScore10(), summary.getAverageStars5(), summary.getTotalVotes(), summary.getUserScore()
        );
        response.getWriter().write(json);
    }
}
