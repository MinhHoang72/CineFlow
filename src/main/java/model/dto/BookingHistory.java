package model.dto;

import model.dto.booking.BookingHistoryDTO;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Tương thích ngược với mã nguồn cũ.
 * Khuyến nghị sử dụng model.dto.booking.BookingHistoryDTO theo CineFlow Architecture Standard.
 */
public class BookingHistory extends BookingHistoryDTO {

    public BookingHistory() {
        super();
    }

    public BookingHistory(String bookingCode, String movieTitle, String centreName,
                          Timestamp startTime, String bookingStatus, BigDecimal totalAmount,
                          Timestamp createdAt) {
        super(bookingCode, movieTitle, centreName, startTime, bookingStatus, totalAmount, createdAt);
    }
}
