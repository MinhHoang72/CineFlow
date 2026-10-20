package model.dto.report;

import java.math.BigDecimal;

public class SalesRowDTO {

    private final String label;
    private final int bookings;
    private final int tickets;
    private final BigDecimal ticketRevenue;
    private final BigDecimal fnbRevenue;

    public SalesRowDTO(String label, int bookings, int tickets, BigDecimal ticketRevenue, BigDecimal fnbRevenue) {
        this.label = label;
        this.bookings = bookings;
        this.tickets = tickets;
        this.ticketRevenue = ticketRevenue != null ? ticketRevenue : BigDecimal.ZERO;
        this.fnbRevenue = fnbRevenue != null ? fnbRevenue : BigDecimal.ZERO;
    }

    public String getLabel() { return label; }
    public int getBookings() { return bookings; }
    public int getTickets() { return tickets; }
    public BigDecimal getTicketRevenue() { return ticketRevenue; }
    public BigDecimal getFnbRevenue() { return fnbRevenue; }
    public BigDecimal getTotalRevenue() {
        return ticketRevenue.add(fnbRevenue);
    }
    public BigDecimal getTotal() {
        return getTotalRevenue();
    }
}
