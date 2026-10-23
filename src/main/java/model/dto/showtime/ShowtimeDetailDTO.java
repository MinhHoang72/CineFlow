package model.dto.showtime;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

/**
 * DTO hiển thị chi tiết suất chiếu (Showtime + movieTitle + hallName + centreName).
 * Phục vụ màn hình danh sách suất chiếu, đặt vé và chi tiết phim.
 */
public class ShowtimeDetailDTO {

    private int showtimeId;
    private int movieId;
    private int hallId;
    private int centreId;
    private Timestamp startTime;
    private Timestamp endTime;
    private BigDecimal price;
    private String status;

    private String movieTitle;
    private String hallName;
    private String centreName;
    private String hallType;

    private String posterUrl;
    private String ageRating;
    private int duration;
    private String genre;
    private String city;
    private BigDecimal baseTicketPrice;

    public ShowtimeDetailDTO() {
    }

    public ShowtimeDetailDTO(int showtimeId, int movieId, int hallId, int centreId,
                             Timestamp startTime, Timestamp endTime, BigDecimal price,
                             String status, String movieTitle, String hallName, String centreName) {
        this.showtimeId = showtimeId;
        this.movieId = movieId;
        this.hallId = hallId;
        this.centreId = centreId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.price = price;
        this.baseTicketPrice = price;
        this.status = status;
        this.movieTitle = movieTitle;
        this.hallName = hallName;
        this.centreName = centreName;
    }

    public int getShowtimeId() { return showtimeId; }
    public void setShowtimeId(int showtimeId) { this.showtimeId = showtimeId; }

    public int getId() { return showtimeId; }
    public void setId(int id) { this.showtimeId = id; }

    public int getMovieId() { return movieId; }
    public void setMovieId(int movieId) { this.movieId = movieId; }

    public int getHallId() { return hallId; }
    public void setHallId(int hallId) { this.hallId = hallId; }

    public int getCentreId() { return centreId; }
    public void setCentreId(int centreId) { this.centreId = centreId; }

    public int getCentreID() { return centreId; }
    public void setCentreID(int centreId) { this.centreId = centreId; }




    public Timestamp getStartTime() { return startTime; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }

    public Timestamp getEndTime() { return endTime; }
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }

    public BigDecimal getPrice() { return price != null ? price : baseTicketPrice; }
    public void setPrice(BigDecimal price) {
        this.price = price;
        this.baseTicketPrice = price;
    }

    public BigDecimal getBasePrice() { return getPrice(); }
    public void setBasePrice(BigDecimal price) { setPrice(price); }

    public BigDecimal getBaseTicketPrice() { return getPrice(); }
    public void setBaseTicketPrice(BigDecimal baseTicketPrice) { setPrice(baseTicketPrice); }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMovieTitle() { return movieTitle; }
    public void setMovieTitle(String movieTitle) { this.movieTitle = movieTitle; }

    public String getHallName() { return hallName; }
    public void setHallName(String hallName) { this.hallName = hallName; }

    public String getHallType() { return hallType; }
    public void setHallType(String hallType) { this.hallType = hallType; }

    public String getCentreName() { return centreName; }
    public void setCentreName(String centreName) { this.centreName = centreName; }




    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public String getAgeRating() { return ageRating; }
    public void setAgeRating(String ageRating) { this.ageRating = ageRating; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getStartTimeFormatted() {
        if (startTime == null) return "";
        return startTime.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public String getEndTimeFormatted() {
        if (endTime == null) return "";
        return endTime.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public String getTimeRangeFormatted() {
        if (startTime == null) return "";
        String startStr = startTime.toLocalDateTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        if (endTime == null) return startStr;
        String endStr = endTime.toLocalDateTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        return startStr + " - " + endStr;
    }
}
