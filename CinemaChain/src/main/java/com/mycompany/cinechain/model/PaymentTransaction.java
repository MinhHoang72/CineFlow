package com.mycompany.cinechain.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/** Mot giao dich thanh toan cua don, hien trong chi tiet don va hoa don. */
public class PaymentTransaction {

    private final int id;
    private final String transactionCode, method, status;
    private final BigDecimal amount, cashReceived;
    private final Timestamp createdAt, paidAt;

    public PaymentTransaction(int id, String transactionCode, String method, BigDecimal amount, String status,
            Timestamp createdAt, Timestamp paidAt, BigDecimal cashReceived) {
        this.id = id;
        this.transactionCode = transactionCode;
        this.method = method;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
        this.paidAt = paidAt;
        this.cashReceived = cashReceived;
    }

    public int getId() { return id; }
    public String getTransactionCode() { return transactionCode; }
    public String getMethod() { return method; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getPaidAt() { return paidAt; }
    public BigDecimal getCashReceived() { return cashReceived; }

    /** Tien thoi lai cho khach (chi voi tien mat). */
    public BigDecimal getChange() {
        return cashReceived == null ? null : cashReceived.subtract(amount);
    }
}
