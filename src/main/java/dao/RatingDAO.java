package dao;

import utils.DBContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class RatingDAO {

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
     * Save or update user rating for a movie.
     * Score is constrained to [1..5].
     */
    public boolean saveOrUpdateRating(int movieId, int userId, int score) {
        if (score < 1) score = 1;
        if (score > 5) score = 5;

        String updateSql = "UPDATE MovieRatings SET Score = ?, UpdatedAt = GETDATE() WHERE MovieID = ? AND UserID = ?";
        String insertSql = "INSERT INTO MovieRatings (MovieID, UserID, Score, CreatedAt) VALUES (?, ?, ?, GETDATE())";

        try (Connection con = DBContext.getConnection()) {
            try (PreparedStatement updatePs = con.prepareStatement(updateSql)) {
                updatePs.setInt(1, score);
                updatePs.setInt(2, movieId);
                updatePs.setInt(3, userId);
                int updatedRows = updatePs.executeUpdate();
                if (updatedRows > 0) {
                    return true;
                }
            }

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
     * Get aggregate rating info for a movie and the user's vote if logged in.
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

        double avgScore10 = Math.round(avgStars * 2.0 * 10.0) / 10.0;
        double roundedAvgStars5 = Math.round(avgStars * 10.0) / 10.0;
        return new MovieRatingSummary(avgScore10, roundedAvgStars5, count, userScore);
    }
}
