package model.dto;

import java.math.BigDecimal;

/**
 * Tương thích ngược với mã nguồn cũ.
 * Khuyến nghị sử dụng model.dto.report.SalesRowDTO.
 */
public class SalesRowDTO extends model.dto.report.SalesRowDTO {
    public SalesRowDTO(String label, int bookings, int tickets, BigDecimal ticketRevenue, BigDecimal fnbRevenue) {
        super(label, bookings, tickets, ticketRevenue, fnbRevenue);
    }
}
