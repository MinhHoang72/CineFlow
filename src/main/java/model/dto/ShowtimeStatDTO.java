package model.dto;

import java.sql.Timestamp;

/**
 * Tương thích ngược với mã nguồn cũ.
 * Khuyến nghị sử dụng model.dto.showtime.ShowtimeStatDTO.
 */
public class ShowtimeStatDTO extends model.dto.showtime.ShowtimeStatDTO {
    public ShowtimeStatDTO(String movieTitle, String centreName, String hallName, Timestamp startTime, int capacity, int soldSeats, int checkedIn) {
        super(movieTitle, centreName, hallName, startTime, capacity, soldSeats, checkedIn);
    }
}
