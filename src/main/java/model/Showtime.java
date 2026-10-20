package model;

import java.time.LocalDateTime;
import java.sql.Timestamp;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public class Showtime {

    private int showtimeId;
    private int movieId;
    private int hallId;

    private Timestamp startTime;
    private Timestamp endTime;

    private int cleaningBufferMinutes;
    private String status;

    private Integer createdBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private String hallName;
    private String hallType;
    public String getHallName() { return hallName; }
    public void setHallName(String hallName) { this.hallName = hallName; }
    public String getHallType() { return hallType; }
    public void setHallType(String hallType) { this.hallType = hallType; }
    private String movieTitle;

public String getMovieTitle() {
    return movieTitle;
}

public void setMovieTitle(String movieTitle) {
    this.movieTitle = movieTitle;
}

    public Showtime() {
    }

    public int getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(int showtimeId) {
        this.showtimeId = showtimeId;
    }

    public int getMovieId() {
        return movieId;
    }

    public void setMovieId(int movieId) {
        this.movieId = movieId;
    }

    public int getHallId() {
        return hallId;
    }

    public void setHallId(int hallId) {
        this.hallId = hallId;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime == null ? null : Timestamp.valueOf(startTime);
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime == null ? null : Timestamp.valueOf(endTime);
    }

    public int getCleaningBufferMinutes() {
        return cleaningBufferMinutes;
    }

    public void setCleaningBufferMinutes(int cleaningBufferMinutes) {
        this.cleaningBufferMinutes = cleaningBufferMinutes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    public String getStartTimeFormatted() {
    if (startTime == null) {
        return "";
    }

    return startTime.toLocalDateTime().format(
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    );
}

public String getEndTimeFormatted() {
    if (endTime == null) {
        return "";
    }

    return endTime.toLocalDateTime().format(
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    );
}

public String getStartTimeInputFormatted() {

    if (startTime == null) {
        return "";
    }

    return startTime.toLocalDateTime().format(
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd'T'HH:mm"
            )
    );
}
    private int branchId;
    private String branchName;
    private BigDecimal basePrice;
    public int getId() { return showtimeId; }
    public int getBranchId() { return branchId; }
    public String getBranchName() { return branchName; }
    public BigDecimal getBasePrice() { return basePrice; }
    public Showtime(int id, int movieId, String movieTitle, int branchId, String branchName,
            int hallId, String hallName, String hallType, Timestamp startTime, Timestamp endTime,
            BigDecimal basePrice) {
        this.showtimeId = id;
        this.movieId = movieId;
        this.movieTitle = movieTitle;
        this.branchId = branchId;
        this.branchName = branchName;
        this.hallId = hallId;
        this.hallName = hallName;
        this.hallType = hallType;
        this.startTime = startTime;
        this.endTime = endTime;
        this.basePrice = basePrice;
    }
}