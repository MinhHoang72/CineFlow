package model;

public class ShowtimeMetric {
    private String movieTitle, centreName, hallName;
    private java.sql.Timestamp startTime;
    private int soldSeats, capacity;
    private double occupancy;

    public String getMovieTitle() { return movieTitle; }
    public void setMovieTitle(String value) { movieTitle = value; }
    public String getCentreName() { return centreName; }
    public void setCentreName(String value) { centreName = value; }
    public String getHallName() { return hallName; }
    public void setHallName(String value) { hallName = value; }
    public java.sql.Timestamp getStartTime() { return startTime; }
    public void setStartTime(java.sql.Timestamp value) { startTime = value; }
    public int getSoldSeats() { return soldSeats; }
    public void setSoldSeats(int value) { soldSeats = value; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int value) { capacity = value; }
    public double getOccupancy() { return occupancy; }
    public void setOccupancy(double value) { occupancy = value; }
}
