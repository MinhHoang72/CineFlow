package model.dto;

/**
 * Tương thích ngược với mã nguồn cũ.
 * Khuyến nghị sử dụng model.dto.report.OccupancyRowDTO.
 */
public class OccupancyRowDTO extends model.dto.report.OccupancyRowDTO {
    public OccupancyRowDTO(String label, int capacity, int soldSeats) {
        super(label, capacity, soldSeats);
    }
}
