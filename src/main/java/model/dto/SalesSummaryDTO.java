package model.dto;
import model.entity.*;

import java.math.BigDecimal;

public class SalesSummaryDTO {

    private final BigDecimal ticketRevenue;
    private final BigDecimal fnbRevenue;
    private final BigDecimal discountTotal;
    private final int bookings;
    private final int tickets;
    private final int voucherBookings;

    public SalesSummaryDTO(BigDecimal ticketRevenue, BigDecimal fnbRevenue, BigDecimal discountTotal, int bookings, int tickets, int voucherBookings) {
        this.ticketRevenue = ticketRevenue;
        this.fnbRevenue = fnbRevenue;
        this.discountTotal = discountTotal;
        this.bookings = bookings;
        this.tickets = tickets;
        this.voucherBookings = voucherBookings;
    }

    public BigDecimal getTicketRevenue() {
        return ticketRevenue;
    }

    public BigDecimal getFnbRevenue() {
        return fnbRevenue;
    }

    public BigDecimal getDiscountTotal() {
        return discountTotal;
    }

    public int getBookings() {
        return bookings;
    }

    public int getTickets() {
        return tickets;
    }

    public int getVoucherBookings() {
        return voucherBookings;
    }

    public BigDecimal getTotalRevenue() {
        return ticketRevenue.add(fnbRevenue);
    }

    public BigDecimal getAverageOrder() {
        return bookings == 0 ? BigDecimal.ZERO : ticketRevenue.divide(BigDecimal.valueOf(bookings), 0, java.math.RoundingMode.HALF_UP);
    }
}
