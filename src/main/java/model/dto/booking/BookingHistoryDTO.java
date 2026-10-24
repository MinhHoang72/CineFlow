package model.dto.booking;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * DTO lịch sử đơn hàng của khách hàng (BookingHistoryDTO).
 * Phục vụ lịch sử đặt vé ở hồ sơ cá nhân (/account) và trang quản lý khách hàng.
 */
public class BookingHistoryDTO {

    private int bookingId;
    /** Đơn chờ thanh toán còn trong thời gian giữ ghế. */
    private boolean holdActive;
    private String bookingCode;
    private String movieTitle;
    private String centreName;
    private Timestamp startTime;
    private String bookingStatus;
    private BigDecimal totalAmount;
    private Timestamp createdAt;

    public BookingHistoryDTO() {
    }

    public BookingHistoryDTO(String bookingCode, String movieTitle, String centreName,
                             Timestamp startTime, String bookingStatus, BigDecimal totalAmount,
                             Timestamp createdAt) {
        this.bookingCode = bookingCode;
        this.movieTitle = movieTitle;
        this.centreName = centreName;
        this.startTime = startTime;
        this.bookingStatus = bookingStatus;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
    }

    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }

    public boolean isHoldActive() { return holdActive; }
    public void setHoldActive(boolean holdActive) { this.holdActive = holdActive; }

    /** Còn thanh toán tiếp được (đơn chờ thanh toán, chưa hết giờ giữ ghế). */
    public boolean isPayable() { return "PENDING".equals(bookingStatus) && holdActive; }

    /** Trạng thái hiển thị tiếng Việt; đơn chờ thanh toán đã quá giờ giữ ghế coi như hết hạn. */
    public String getStatusLabel() {
        if (bookingStatus == null) return "";
        return switch (bookingStatus) {
            case "PENDING" -> holdActive ? "Chờ thanh toán" : "Hết hạn";
            case "CONFIRMED" -> "Đã thanh toán";
            case "COMPLETED" -> "Hoàn tất";
            case "CANCELLED" -> "Đã huỷ";
            case "EXPIRED" -> "Hết hạn";
            default -> bookingStatus;
        };
    }

    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }

    public String getMovieTitle() { return movieTitle; }
    public void setMovieTitle(String movieTitle) { this.movieTitle = movieTitle; }

    public String getCentreName() { return centreName; }
    public void setCentreName(String centreName) { this.centreName = centreName; }




    public Timestamp getStartTime() { return startTime; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }

    public String getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }

    public String getStatus() { return bookingStatus; }
    public void setStatus(String status) { this.bookingStatus = status; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
