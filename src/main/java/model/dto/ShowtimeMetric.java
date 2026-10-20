package model.dto;
import model.entity.*;

import java.sql.Timestamp;

/**
 * Thống kê hiệu suất và tỷ lệ lấp đầy ghế theo từng suất chiếu cụ thể.
 */
public class ShowtimeMetric {

    private String movieTitle;
    private String centreName;
    private String hallName;
    private Timestamp startTime;
    private int soldSeats;
    private int capacity;
    private double occupancy;

    public ShowtimeMetric() {
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public String getCentreName() {
        return centreName;
    }

    public void setCentreName(String centreName) {
        this.centreName = centreName;
    }

    public String getHallName() {
        return hallName;
    }

    public void setHallName(String hallName) {
        this.hallName = hallName;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public int getSoldSeats() {
        return soldSeats;
    }

    public void setSoldSeats(int soldSeats) {
        this.soldSeats = soldSeats;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public double getOccupancy() {
        return occupancy;
    }

    public void setOccupancy(double occupancy) {
        this.occupancy = occupancy;
    }
}
