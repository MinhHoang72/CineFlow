package model.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Tương thích ngược với mã nguồn cũ.
 * Khuyến nghị sử dụng model.dto.showtime.ShowtimeDetailDTO.
 */
public class ShowtimeView extends model.dto.showtime.ShowtimeDetailDTO {

    public ShowtimeView() {
        super();
    }

    public ShowtimeView(int showtimeId, int movieId, int hallId, int centreId,
                        Timestamp startTime, Timestamp endTime, BigDecimal price,
                        String status, String movieTitle, String hallName, String centreName) {
        super(showtimeId, movieId, hallId, centreId, startTime, endTime, price, status, movieTitle, hallName, centreName);
    }
}
