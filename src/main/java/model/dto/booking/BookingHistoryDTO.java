package model.dto.booking;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * DTO lịch sử đơn hàng của khách hàng (BookingHistoryDTO).
 * Phục vụ màn hình tra cứu đơn (/booking?action=history) và hồ sơ cá nhân (/account).
 */
public class BookingHistoryDTO {

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

    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }

    public String getMovieTitle() { return movieTitle; }
    public void setMovieTitle(String movieTitle) { this.movieTitle = movieTitle; }

    public String getCentreName() { return centreName; }
    public void setCentreName(String centreName) { this.centreName = centreName; }

    public String getBranchName() { return centreName; }
    public void setBranchName(String branchName) { this.centreName = branchName; }

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
