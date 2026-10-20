package model.dto;

import java.math.BigDecimal;

/**
 * Tương thích ngược với mã nguồn cũ.
 * Khuyến nghị sử dụng model.dto.report.SalesSummaryDTO.
 */
public class SalesSummaryDTO extends model.dto.report.SalesSummaryDTO {
    public SalesSummaryDTO(BigDecimal ticketRevenue, BigDecimal fnbRevenue, BigDecimal discountTotal, int bookings, int tickets, int voucherBookings) {
        super(ticketRevenue, fnbRevenue, discountTotal, bookings, tickets, voucherBookings);
    }
}
