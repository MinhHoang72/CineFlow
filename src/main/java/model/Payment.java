package model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/** Mot giao dich chuyen khoan cho nhan vien doi soat. */
public class Payment {

    private final int id, bookingId;
    private final String transactionCode, status, bookingCode, bookingStatus;
    private final String customerName, customerPhone, branchName, movieTitle;
    private final BigDecimal amount;
    private final Timestamp createdAt, holdUntil;

    public Payment(int id, String transactionCode, BigDecimal amount, String status, Timestamp createdAt,
            int bookingId, String bookingCode, String bookingStatus, Timestamp holdUntil,
            String customerName, String customerPhone, String branchName, String movieTitle) {
        this.id = id;
        this.transactionCode = transactionCode;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
        this.bookingId = bookingId;
        this.bookingCode = bookingCode;
        this.bookingStatus = bookingStatus;
        this.holdUntil = holdUntil;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.branchName = branchName;
        this.movieTitle = movieTitle;
    }

    public int getId() { return id; }
    public String getTransactionCode() { return transactionCode; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public Timestamp getCreatedAt() { return createdAt; }
    public int getBookingId() { return bookingId; }
    public String getBookingCode() { return bookingCode; }
    public String getBookingStatus() { return bookingStatus; }
    public Timestamp getHoldUntil() { return holdUntil; }
    public String getCustomerName() { return customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public String getBranchName() { return branchName; }
    public String getMovieTitle() { return movieTitle; }
}
