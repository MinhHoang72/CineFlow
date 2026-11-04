package model.entity;

import java.sql.Timestamp;

/**
 * Đại diện cho một bản ghi phân phối phim tới cụm rạp (MovieDistribution / CentreMovies).
 * Theo tài liệu đặc tả nghiệp vụ phân phối phim CineFlow:
 * - ACTIVE: Phân phối đang có hiệu lực.
 * - SUSPENDED: Tạm ngưng do rạp tạm ngưng; giữ cấu hình.
 * - CANCELLED: Đã hủy (do rạp INACTIVE hoặc Admin hủy thủ công); coi như chưa phân phối.
 */
public class MovieDistribution {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_SUSPENDED = "SUSPENDED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private int centreMovieId;
    private int centreId;
    private int movieId;
    private String status;
    private boolean isActive;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public MovieDistribution() {
    }

    public MovieDistribution(int centreId, int movieId, String status) {
        this.centreId = centreId;
        this.movieId = movieId;
        this.status = status;
        this.isActive = STATUS_ACTIVE.equalsIgnoreCase(status);
    }

    public int getCentreMovieId() {
        return centreMovieId;
    }

    public void setCentreMovieId(int centreMovieId) {
        this.centreMovieId = centreMovieId;
    }

    public int getCentreId() {
        return centreId;
    }

    public void setCentreId(int centreId) {
        this.centreId = centreId;
    }

    public int getMovieId() {
        return movieId;
    }

    public void setMovieId(int movieId) {
        this.movieId = movieId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.isActive = STATUS_ACTIVE.equalsIgnoreCase(status);
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
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
