package model.entity;

import java.math.BigDecimal;
import java.sql.Timestamp;

/** Mot giao dich thanh toan ve (VNPay hoac tien mat) tren trang theo doi giao dich. */
public class Payment {

    private final int id, bookingId;
    private final String transactionCode, method, status, bookingCode, bookingType, bookingStatus;
    private final String customerName, customerPhone, branchName, movieTitle;
    private final BigDecimal amount;
    private final Timestamp createdAt, paidAt;

    public Payment(int id, String transactionCode, String method, BigDecimal amount, String status,
            Timestamp createdAt, Timestamp paidAt, int bookingId, String bookingCode, String bookingType,
            String bookingStatus, String customerName, String customerPhone, String branchName, String movieTitle) {
        this.id = id;
        this.transactionCode = transactionCode;
        this.method = method;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
        this.paidAt = paidAt;
        this.bookingId = bookingId;
        this.bookingCode = bookingCode;
        this.bookingType = bookingType;
        this.bookingStatus = bookingStatus;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.branchName = branchName;
        this.movieTitle = movieTitle;
    }

    public int getId() { return id; }
    public String getTransactionCode() { return transactionCode; }
    public String getMethod() { return method; }
    /** QR trong DB = thanh toan qua VNPay. */
    public String getMethodLabel() { return "CASH".equals(method) ? "Tiền mặt" : "VNPay"; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public String getStatusLabel() {
        return switch (status) {
            case "PAID" -> "Thành công";
            case "PENDING" -> "Đang chờ";
            case "FAILED" -> "Thất bại";
            case "TIMEOUT" -> "Hết hạn";
            case "CANCELLED" -> "Đã huỷ";
            default -> status;
        };
    }
    public String getStatusBadge() {
        return switch (status) {
            case "PAID" -> "bg-success";
            case "PENDING" -> "bg-warning text-dark";
            case "FAILED" -> "bg-danger";
            default -> "bg-secondary";
        };
    }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getPaidAt() { return paidAt; }
    public int getBookingId() { return bookingId; }
    public String getBookingCode() { return bookingCode; }
    public boolean isCounter() { return "COUNTER".equals(bookingType); }
    public String getBookingStatus() { return bookingStatus; }
    public String getCustomerName() { return customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public String getBranchName() { return branchName; }
    public String getMovieTitle() { return movieTitle; }
}
