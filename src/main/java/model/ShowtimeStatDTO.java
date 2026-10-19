package model;

import java.sql.Timestamp;

public class ShowtimeStatDTO {

    private final String movieTitle;
    private final String centreName;
    private final String hallName;
    private final Timestamp startTime;
    private final int capacity;
    private final int soldSeats;
    private final int checkedIn;

    public ShowtimeStatDTO(String movieTitle, String centreName, String hallName, Timestamp startTime, int capacity, int soldSeats, int checkedIn) {
        this.movieTitle = movieTitle;
        this.centreName = centreName;
        this.hallName = hallName;
        this.startTime = startTime;
        this.capacity = capacity;
        this.soldSeats = soldSeats;
        this.checkedIn = checkedIn;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public String getCentreName() {
        return centreName;
    }

    public String getHallName() {
        return hallName;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getSoldSeats() {
        return soldSeats;
    }

    public int getCheckedIn() {
        return checkedIn;
    }

    public double getOccupancy() {
        return capacity == 0 ? 0 : 100.0 * soldSeats / capacity;
    }
}
