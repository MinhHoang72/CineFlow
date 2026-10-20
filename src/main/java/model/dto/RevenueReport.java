package model.dto;
import model.entity.*;

import java.math.BigDecimal;

/**
 * Báo cáo doanh thu và số lượng vé bán ra theo từng rạp hoặc phim.
 */
public class RevenueReport {

    private String name;
    private String city;
    private int bookings;
    private int tickets;
    private BigDecimal revenue = BigDecimal.ZERO;

    public RevenueReport() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public int getBookings() {
        return bookings;
    }

    public void setBookings(int bookings) {
        this.bookings = bookings;
    }

    public int getTickets() {
        return tickets;
    }

    public void setTickets(int tickets) {
        this.tickets = tickets;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue == null ? BigDecimal.ZERO : revenue;
    }
}
