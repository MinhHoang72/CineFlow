package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Showtime {

    private int showtimeId;
    private int movieId;
    private int hallId;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

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

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
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

    return startTime.format(
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    );
}

public String getEndTimeFormatted() {
    if (endTime == null) {
        return "";
    }

    return endTime.format(
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    );
}

public String getStartTimeInputFormatted() {

    if (startTime == null) {
        return "";
    }

    return startTime.format(
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd'T'HH:mm"
            )
    );
}
}