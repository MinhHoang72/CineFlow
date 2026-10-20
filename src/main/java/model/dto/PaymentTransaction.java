package model.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Tương thích ngược với mã nguồn cũ.
 * Khuyến nghị sử dụng model.dto.booking.PaymentTransaction.
 */
public class PaymentTransaction extends model.dto.booking.PaymentTransaction {
    public PaymentTransaction(int id, String transactionCode, String method, BigDecimal amount, String status,
                              Timestamp createdAt, Timestamp paidAt, BigDecimal cashReceived) {
        super(id, transactionCode, method, amount, status, createdAt, paidAt, cashReceived);
    }
}
