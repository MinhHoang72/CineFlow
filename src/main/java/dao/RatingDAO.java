package dao;

import utils.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Xử lý truy vấn và lưu trữ điểm đánh giá phim của người dùng.
 */
public class RatingDAO {

    /**
     * Lớp đóng gói dữ liệu tổng hợp đánh giá phim.
     */
    public static class MovieRatingSummary {
        private final double averageScore10;
        private final double averageStars5;
        private final int totalVotes;
        private final int userScore;

        public MovieRatingSummary(double averageScore10, double averageStars5, int totalVotes, int userScore) {
            this.averageScore10 = averageScore10;
            this.averageStars5 = averageStars5;
            this.totalVotes = totalVotes;
            this.userScore = userScore;
        }

        public double getAverageScore10() {
            return averageScore10;
        }

        public double getAverageStars5() {
            return averageStars5;
        }

        public int getTotalVotes() {
            return totalVotes;
        }

        public int getUserScore() {
            return userScore;
        }
    }

    /**
     * Lưu hoặc cập nhật số sao đánh giá của người dùng cho một phim.
     * Điểm đánh giá nằm trong thang điểm từ 1 đến 5 sao.
     *
     * @param movieId mã định danh phim
     * @param userId  mã người dùng
     * @param score   số sao người dùng chấm (1 - 5)
     * @return true nếu lưu thành công, ngược lại false
     */
    public boolean saveOrUpdateRating(int movieId, int userId, int score) {
        if (score < 1) {
            score = 1;
        }
        if (score > 5) {
            score = 5;
        }

        String updateSql = "UPDATE MovieRatings SET Score = ?, UpdatedAt = GETDATE() WHERE MovieID = ? AND UserID = ?";
        String insertSql = "INSERT INTO MovieRatings (MovieID, UserID, Score, CreatedAt) VALUES (?, ?, ?, GETDATE())";

        try (Connection con = DBContext.getConnection()) {
            // Thử cập nhật đánh giá đã tồn tại trước đó
            try (PreparedStatement updatePs = con.prepareStatement(updateSql)) {
                updatePs.setInt(1, score);
                updatePs.setInt(2, movieId);
                updatePs.setInt(3, userId);
                int updatedRows = updatePs.executeUpdate();
                if (updatedRows > 0) {
                    return true;
                }
            }

            // Nếu người dùng chưa từng đánh giá thì tạo mới bản ghi
            try (PreparedStatement insertPs = con.prepareStatement(insertSql)) {
                insertPs.setInt(1, movieId);
                insertPs.setInt(2, userId);
                insertPs.setInt(3, score);
                return insertPs.executeUpdate() > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Lấy dữ liệu tổng hợp đánh giá trung bình và điểm số của người dùng hiện tại (nếu đã đăng nhập).
     *
     * @param movieId       mã phim
     * @param currentUserId mã người dùng hiện tại hoặc null
     * @return đối tượng tổng hợp đánh giá
     */
    public MovieRatingSummary getRatingSummary(int movieId, Integer currentUserId) {
        String sql = "SELECT AVG(CAST(Score AS FLOAT)) AS AvgStar, COUNT(RatingID) AS TotalCount "
                + "FROM MovieRatings WHERE MovieID = ?";
        double avgStars = 0.0;
        int count = 0;

        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt("TotalCount");
                    if (count > 0) {
                        avgStars = rs.getDouble("AvgStar");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        int userScore = 0;
        if (currentUserId != null && currentUserId > 0) {
            String userSql = "SELECT Score FROM MovieRatings WHERE MovieID = ? AND UserID = ?";
            try (Connection con = DBContext.getConnection();
                 PreparedStatement ps = con.prepareStatement(userSql)) {
                ps.setInt(1, movieId);
                ps.setInt(2, currentUserId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        userScore = rs.getInt("Score");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Quy đổi sao sang thang điểm 10 và làm tròn đến 1 chữ số thập phân
        double avgScore10 = Math.round(avgStars * 2.0 * 10.0) / 10.0;
        double roundedAvgStars5 = Math.round(avgStars * 10.0) / 10.0;
        return new MovieRatingSummary(avgScore10, roundedAvgStars5, count, userScore);
    }
}
