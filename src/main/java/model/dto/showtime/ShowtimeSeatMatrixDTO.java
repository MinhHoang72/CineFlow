package model.dto.showtime;

import java.math.BigDecimal;

/**
 * DTO ma trận ghế hiển thị trạng thái màu sắc phòng chiếu (ShowtimeSeatMatrixDTO).
 * Phục vụ màn hình sơ đồ ghế suất chiếu (/showtimes/seats).
 */
public class ShowtimeSeatMatrixDTO {

    private int seatId;
    private int hallId;
    private String seatRow;
    private int seatNumber;
    private String seatType;           // STANDARD, VIP, SWEETBOX
    private String availabilityStatus; // AVAILABLE, HELD, BOOKED, MAINTENANCE
    private BigDecimal price;
    private String displayLabel;

    public ShowtimeSeatMatrixDTO() {
    }

    public ShowtimeSeatMatrixDTO(int seatId, int hallId, String seatRow, int seatNumber,
                                 String seatType, String availabilityStatus, BigDecimal price) {
        this.seatId = seatId;
        this.hallId = hallId;
        this.seatRow = seatRow;
        this.seatNumber = seatNumber;
        this.seatType = seatType;
        this.availabilityStatus = availabilityStatus;
        this.price = price;
    }

    public String getLabel() {
        return (seatRow != null ? seatRow : "") + seatNumber;
    }

    public String getDisplayLabel() { return displayLabel != null ? displayLabel : getLabel(); }
    public void setDisplayLabel(String displayLabel) { this.displayLabel = displayLabel; }

    public String getAvailabilityLabel() {
        if ("AVAILABLE".equals(availabilityStatus)) return "Trống";
        if ("HELD".equals(availabilityStatus)) return "Đang giữ";
        if ("BOOKED".equals(availabilityStatus)) return "Đã đặt";
        if ("MAINTENANCE".equals(availabilityStatus)) return "Bảo trì";
        return "Lối đi / Trống";
    }

    public String getStatusBadgeClass() {
        if ("AVAILABLE".equalsIgnoreCase(availabilityStatus)) return "st-status-COMPLETED";
        if ("HELD".equalsIgnoreCase(availabilityStatus)) return "st-status-ONGOING";
        if ("BOOKED".equalsIgnoreCase(availabilityStatus)) return "st-status-CANCELLED";
        return "st-status-DEFAULT";
    }

    public int getSeatId() { return seatId; }
    public void setSeatId(int seatId) { this.seatId = seatId; }

    public int getHallId() { return hallId; }
    public void setHallId(int hallId) { this.hallId = hallId; }

    public String getSeatRow() { return seatRow; }
    public void setSeatRow(String seatRow) { this.seatRow = seatRow; }

    public int getSeatNumber() { return seatNumber; }
    public void setSeatNumber(int seatNumber) { this.seatNumber = seatNumber; }

    public String getSeatType() { return seatType; }
    public void setSeatType(String seatType) { this.seatType = seatType; }

    public String getAvailabilityStatus() { return availabilityStatus; }
    public void setAvailabilityStatus(String availabilityStatus) { this.availabilityStatus = availabilityStatus; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
