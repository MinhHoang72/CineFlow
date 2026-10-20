package model.dto;

/**
 * Tương thích ngược với mã nguồn cũ.
 * Khuyến nghị sử dụng model.dto.report.OperationsSummaryDTO.
 */
public class OperationsSummaryDTO extends model.dto.report.OperationsSummaryDTO {
    public OperationsSummaryDTO(int showtimes, int capacity, int soldSeats, int checkedIn, int rejectedScans, int outOfStockItems) {
        super(showtimes, capacity, soldSeats, checkedIn, rejectedScans, outOfStockItems);
    }
}
