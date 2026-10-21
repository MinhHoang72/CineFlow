package service;

import dao.PaymentDAO;
import dao.PaymentDAO.Outcome;
import dao.PaymentDAO.VnpayRef;
import model.entity.Payment;
import model.entity.Ticket;
import utils.VNPayUtil;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Service: Xử lý logic nghiệp vụ Thanh toán (Payment) & Cổng thanh toán trực tuyến VNPay.
 * Nhiệm vụ:
 * - Khởi tạo giao dịch thanh toán trực tuyến qua cổng VNPay (Sandbox / Production).
 * - Băm chữ ký bảo mật HMAC-SHA512 để xác thực toàn vẹn dữ liệu giao dịch.
 * - Đối soát chữ ký và cập nhật kết quả giao dịch VNPay Return / IPN.
 * - Xác nhận thanh toán thành công (chốt trạng thái vé, ghi nhận dòng tiền).
 * - Hỗ trợ thanh toán tiền mặt trực tiếp tại quầy POS.
 */
public class PaymentService {

    private final PaymentDAO paymentDAO;

    public PaymentService() {
        this.paymentDAO = new PaymentDAO();
    }

    public PaymentService(PaymentDAO paymentDAO) {
        this.paymentDAO = paymentDAO;
    }

    /**
     * Khởi tạo giao dịch VNPay trong CSDL và trả về mã tham chiếu giao dịch (TxnRef).
     */
    public String initVnpayPayment(int bookingId, String bookingCode, BigDecimal amount) throws SQLException {
        return paymentDAO.createVnpayPayment(bookingId, bookingCode, amount);
    }

    /**
     * Tìm thông tin giao dịch VNPay theo mã tham chiếu TxnRef.
     */
    public VnpayRef findVnpayTransaction(String txnRef) throws SQLException {
        return paymentDAO.findVnpay(txnRef);
    }

    /**
     * Xác thực chữ ký số HMAC-SHA512 do VNPay gửi về.
     *
     * @param vnpParams Map các tham số VNPay gửi kèm
     * @return true nếu chữ ký hợp lệ và nguyên vẹn, ngược lại false
     */
    public boolean verifyVnpaySignature(Map<String, String> vnpParams) {
        return VNPayUtil.verify(vnpParams);
    }

    /**
     * Xác nhận đơn đặt vé đã thanh toán thành công và kích hoạt vé điện tử.
     */
    public Outcome confirmPaymentSuccess(int paymentId, Integer branchId) throws SQLException {
        return paymentDAO.confirmPaid(paymentId, branchId);
    }

    /**
     * Đánh dấu giao dịch thanh toán thất bại.
     */
    public void markPaymentFailed(int paymentId) throws SQLException {
        paymentDAO.markFailed(paymentId);
    }

    /**
     * Xử lý thanh toán tiền mặt tại quầy vé.
     */
    public int processCashPayment(int bookingId, String bookingCode, BigDecimal amount, BigDecimal cashReceived)
            throws SQLException {
        return paymentDAO.createCashPayment(bookingId, bookingCode, amount, cashReceived);
    }

    /**
     * Lấy danh sách vé đã xuất của một đơn hàng.
     */
    public List<Ticket> getIssuedTickets(int bookingId) throws SQLException {
        return paymentDAO.getTickets(bookingId);
    }

    /**
     * Lấy danh sách giao dịch phục vụ tra cứu, đối soát.
     */
    public List<Payment> getTransactionsForTracking(Integer branchId, String status, String method, int days)
            throws SQLException {
        return paymentDAO.getTransactionsForTracking(branchId, status, method, days);
    }
}
