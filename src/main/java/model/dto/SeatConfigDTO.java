package model.dto;

/**
 * Tương thích ngược với mã nguồn cũ.
 * Khuyến nghị sử dụng model.dto.pricing.SeatConfigDTO.
 */
public class SeatConfigDTO extends model.dto.pricing.SeatConfigDTO {
    public SeatConfigDTO() {
        super();
    }

    public SeatConfigDTO(int seatId, int hallId, String seatRow, int seatNumber, String seatType, String status) {
        super(seatId, hallId, seatRow, seatNumber, seatType, status);
    }
}
