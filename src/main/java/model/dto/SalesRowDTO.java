package model.dto;
import model.entity.*;

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
        this.ticketRevenue = ticketRevenue;
        this.fnbRevenue = fnbRevenue;
    }

    public String getLabel() {
        return label;
    }

    public int getBookings() {
        return bookings;
    }

    public int getTickets() {
        return tickets;
    }

    public BigDecimal getTicketRevenue() {
        return ticketRevenue;
    }

    public BigDecimal getFnbRevenue() {
        return fnbRevenue;
    }

    public BigDecimal getTotal() {
        return ticketRevenue.add(fnbRevenue);
    }
}
