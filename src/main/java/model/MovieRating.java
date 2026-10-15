package model;

import java.sql.Timestamp;

/**
 * Lưu trữ thông tin đánh giá (rating) của người dùng cho một bộ phim.
 */
public class MovieRating {
    private int ratingId;
    private int movieId;
    private int userId;
    private int score;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public MovieRating() {
    }

    public MovieRating(int movieId, int userId, int score) {
        this.movieId = movieId;
        this.userId = userId;
        this.score = score;
    }

    public int getRatingId() {
        return ratingId;
    }

    public void setRatingId(int ratingId) {
        this.ratingId = ratingId;
    }

    public int getMovieId() {
        return movieId;
    }

    public void setMovieId(int movieId) {
        this.movieId = movieId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
