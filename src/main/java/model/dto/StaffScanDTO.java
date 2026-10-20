package model.dto;

/**
 * Tương thích ngược với mã nguồn cũ.
 * Khuyến nghị sử dụng model.dto.checkin.StaffScanDTO.
 */
public class StaffScanDTO extends model.dto.checkin.StaffScanDTO {
    public StaffScanDTO(String staffName, int success, int rejected) {
        super(staffName, success, rejected);
    }
}
