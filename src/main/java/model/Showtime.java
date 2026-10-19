package model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class Showtime {

    private final int id, movieId, branchId, hallId;
    private final String movieTitle, branchName, hallName, hallType;
    private final Timestamp startTime, endTime;
    private final BigDecimal basePrice;

    public Showtime(int id, int movieId, String movieTitle, int branchId, String branchName,
            int hallId, String hallName, String hallType, Timestamp startTime, Timestamp endTime,
            BigDecimal basePrice) {
        this.id = id;
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

    public int getId() { return id; }
    public int getMovieId() { return movieId; }
    public String getMovieTitle() { return movieTitle; }
    public int getBranchId() { return branchId; }
    public String getBranchName() { return branchName; }
    public int getHallId() { return hallId; }
    public String getHallName() { return hallName; }
    public String getHallType() { return hallType; }
    public Timestamp getStartTime() { return startTime; }
    public Timestamp getEndTime() { return endTime; }
    public BigDecimal getBasePrice() { return basePrice; }
}
