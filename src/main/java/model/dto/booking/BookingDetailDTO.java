package model.dto.booking;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO Chi tiết đơn hàng: Ghế + F&B + Voucher + Tiền (BookingDetailDTO).
 * Phục vụ màn hình xác nhận đơn, thanh toán và xem chi tiết vé điện tử.
 */
public class BookingDetailDTO {

    private int bookingId;
    private String bookingCode;
    private Integer userId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    private int showtimeId;
    private String movieTitle;
    private String centreName;
    private String hallName;
    private String hallType;
    private Timestamp startTime;

    private List<String> seatLabels = new ArrayList<>();
    private List<String> fnbItems = new ArrayList<>();
    private String voucherCode;

    private BigDecimal seatAmount = BigDecimal.ZERO;
    private BigDecimal fnbAmount = BigDecimal.ZERO;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private BigDecimal totalAmount = BigDecimal.ZERO;

    private String status;
    private Timestamp createdAt;

    public BookingDetailDTO() {
    }

    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }

    public int getId() { return bookingId; }
    public void setId(int id) { this.bookingId = id; }

    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }

    public String getCode() { return bookingCode; }
    public void setCode(String code) { this.bookingCode = code; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public int getShowtimeId() { return showtimeId; }
    public void setShowtimeId(int showtimeId) { this.showtimeId = showtimeId; }

    public String getMovieTitle() { return movieTitle; }
    public void setMovieTitle(String movieTitle) { this.movieTitle = movieTitle; }

    public String getCentreName() { return centreName; }
    public void setCentreName(String centreName) { this.centreName = centreName; }

    public String getBranchName() { return centreName; }
    public void setBranchName(String branchName) { this.centreName = branchName; }

    public String getHallName() { return hallName; }
    public void setHallName(String hallName) { this.hallName = hallName; }

    public String getHallType() { return hallType; }
    public void setHallType(String hallType) { this.hallType = hallType; }

    public Timestamp getStartTime() { return startTime; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }

    public List<String> getSeatLabels() { return seatLabels; }
    public void setSeatLabels(List<String> seatLabels) { this.seatLabels = seatLabels != null ? seatLabels : new ArrayList<>(); }

    public List<String> getFnbItems() { return fnbItems; }
    public void setFnbItems(List<String> fnbItems) { this.fnbItems = fnbItems != null ? fnbItems : new ArrayList<>(); }

    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }

    public BigDecimal getSeatAmount() { return seatAmount; }
    public void setSeatAmount(BigDecimal seatAmount) { this.seatAmount = seatAmount != null ? seatAmount : BigDecimal.ZERO; }

    public BigDecimal getFnbAmount() { return fnbAmount; }
    public void setFnbAmount(BigDecimal fnbAmount) { this.fnbAmount = fnbAmount != null ? fnbAmount : BigDecimal.ZERO; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
