package model.dto;
import model.entity.*;

/**
 * DTO đại diện cho một vị trí ghế trong sơ đồ cấu hình phòng chiếu (UC-FE-03.3).
 * Lưu trữ tọa độ hàng, cột, phân hạng ghế và trạng thái hoạt động / bảo trì.
 */
public class SeatConfigDTO {

    private int seatId;
    private int hallId;
    private String seatRow;    // A, B, C...
    private int seatNumber;    // 1, 2, 3...
    private String seatType;   // 'STANDARD', 'VIP', 'SWEETBOX'
    private String status;     // 'AVAILABLE', 'MAINTENANCE', 'UNAVAILABLE'

    public SeatConfigDTO() {
    }

    public SeatConfigDTO(int seatId, int hallId, String seatRow, int seatNumber, String seatType, String status) {
        this.seatId = seatId;
        this.hallId = hallId;
        this.seatRow = seatRow;
        this.seatNumber = seatNumber;
        this.seatType = seatType;
        this.status = status;
    }

    public String getLabel() {
        return seatRow + seatNumber;
    }

    public boolean isAvailable() {
        return "AVAILABLE".equalsIgnoreCase(status);
    }

    public boolean isMaintenance() {
        return "MAINTENANCE".equalsIgnoreCase(status);
    }

    public boolean isUnavailable() {
        return "UNAVAILABLE".equalsIgnoreCase(status);
    }

    public int getSeatId() {
        return seatId;
    }

    public void setSeatId(int seatId) {
        this.seatId = seatId;
    }

    public int getHallId() {
        return hallId;
    }

    public void setHallId(int hallId) {
        this.hallId = hallId;
    }

    public String getSeatRow() {
        return seatRow;
    }

    public void setSeatRow(String seatRow) {
        this.seatRow = seatRow;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(int seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getSeatType() {
        return seatType;
    }

    public void setSeatType(String seatType) {
        this.seatType = seatType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
