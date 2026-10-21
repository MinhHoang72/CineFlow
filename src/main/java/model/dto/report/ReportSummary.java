package model.dto.report;

import java.math.BigDecimal;

/**
 * Thống kê tổng hợp số liệu kinh doanh và vận hành toàn hệ thống.
 */
public class ReportSummary {

    private BigDecimal revenue = BigDecimal.ZERO;
    private int bookings;
    private int tickets;
    private int activeMovies;
    private int activeCentres;
    private int activeUsers;
    private double occupancy;

    public ReportSummary() {
    }

    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue == null ? BigDecimal.ZERO : revenue; }

    public int getBookings() { return bookings; }
    public void setBookings(int bookings) { this.bookings = bookings; }

    public int getTickets() { return tickets; }
    public void setTickets(int tickets) { this.tickets = tickets; }

    public int getActiveMovies() { return activeMovies; }
    public void setActiveMovies(int activeMovies) { this.activeMovies = activeMovies; }

    public int getActiveCentres() { return activeCentres; }
    public void setActiveCentres(int activeCentres) { this.activeCentres = activeCentres; }

    public int getActiveUsers() { return activeUsers; }
    public void setActiveUsers(int activeUsers) { this.activeUsers = activeUsers; }

    public double getOccupancy() { return occupancy; }
    public void setOccupancy(double occupancy) { this.occupancy = occupancy; }
}
