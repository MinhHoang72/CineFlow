package model;

import java.math.BigDecimal;

public class ReportSummary {
    private BigDecimal revenue = BigDecimal.ZERO;
    private int bookings, tickets, activeMovies, activeBranches, activeUsers;
    private double occupancy;
    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal v) { revenue = v == null ? BigDecimal.ZERO : v; }
    public int getBookings() { return bookings; }
    public void setBookings(int v) { bookings = v; }
    public int getTickets() { return tickets; }
    public void setTickets(int v) { tickets = v; }
    public int getActiveMovies() { return activeMovies; }
    public void setActiveMovies(int v) { activeMovies = v; }
    public int getActiveBranches() { return activeBranches; }
    public void setActiveBranches(int v) { activeBranches = v; }
    public int getActiveUsers() { return activeUsers; }
    public void setActiveUsers(int v) { activeUsers = v; }
    public double getOccupancy() { return occupancy; }
    public void setOccupancy(double v) { occupancy = v; }
}
