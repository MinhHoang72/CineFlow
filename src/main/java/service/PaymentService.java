package service;

import dao.BookingDAO;
import dao.BookingDAO.VoucherOwner;
import dao.BookingVoucherDAO;
import dao.FnbOrderDAO;
import dao.PaymentDAO;
import dao.PaymentDAO.FnbVnpayRef;
import dao.PaymentDAO.LockedPayment;
import dao.PaymentDAO.NewTicket;
import dao.PaymentDAO.VnpayRef;
import model.dto.booking.PaymentTransaction;
import model.entity.Booking;
import model.entity.Payment;
import model.entity.Ticket;
import utils.CodeUtil;
import utils.DbUtil;
import utils.VNPayUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service: Xử lý logic nghiệp vụ Thanh toán (Payment) & Cổng thanh toán trực tuyến VNPay.
 * Nhiệm vụ:
 * - Tạo giao dịch VNPay cho đơn đang giữ ghế (huỷ giao dịch VNPay đang chờ trước đó).
 * - Xử lý kết quả VNPay (Return URL / IPN): kiểm tra chữ ký HMAC-SHA512, số tiền, trạng thái giao dịch.
 * - Thu tiền mặt tại quầy (tiền khách đưa phải đủ).
 * - Xác nhận thanh toán: chốt ghế, xác nhận đơn, trừ kho bắp nước, ghi lượt dùng voucher, xuất vé.
 *   Gọi lặp lại vẫn an toàn (khoá dòng giao dịch, giao dịch đã PAID thì bỏ qua).
 * - Lý do thanh toán thất bại, tra cứu giao dịch.
 */
public class PaymentService {

    public enum Status {
        SUCCESS,
        ALREADY_PAID,
        NOT_FOUND,
        /** Tiền đã nhận nhưng đơn hết hạn giữ ghế và ghế đã bị người khác lấy -> cần hoàn tiền. */
        PAID_BUT_SEATS_LOST,
        /** Tiền đã nhận theo số tiền cũ, khách đã đổi bắp nước / voucher sau khi tạo giao dịch -> cần hoàn tiền. */
        PAID_AMOUNT_MISMATCH
    }

    public record Outcome(Status status, Integer bookingId) {}

    /** Kết quả xử lý dữ liệu VNPay gửi về. */
    public enum VnpayResult { SUCCESS, ALREADY_PAID, SEATS_LOST, AMOUNT_CHANGED, FAILED, NOT_FOUND, INVALID_AMOUNT, INVALID_SIGNATURE }

    public record VnpayProcessed(VnpayResult result, Integer bookingId) {}

    /** Phản hồi IPN theo đặc tả VNPay (RspCode / Message). */
    public record IpnResponse(String code, String message) {}

    /** Danh sách giao dịch theo dõi kèm tổng tiền và số giao dịch đã thu. */
    public record TrackingSummary(List<Payment> payments, BigDecimal paidTotal, int paidCount) {}

    private final PaymentDAO paymentDAO;
    private final BookingDAO bookingDAO;
    private final FnbOrderDAO fnbOrderDAO;
    private final BookingVoucherDAO voucherDAO;
    private final FnbOrderService fnbOrderService;
    private final BookingService bookingService;
    private final FnbSaleService fnbSaleService;

    public PaymentService() {
        this(new PaymentDAO(), new BookingDAO(), new FnbOrderDAO(), new BookingVoucherDAO(), new FnbOrderService(),
                new BookingService(), new FnbSaleService());
    }

    public PaymentService(PaymentDAO paymentDAO, BookingDAO bookingDAO, FnbOrderDAO fnbOrderDAO,
            BookingVoucherDAO voucherDAO, FnbOrderService fnbOrderService, BookingService bookingService,
            FnbSaleService fnbSaleService) {
        this.paymentDAO = paymentDAO;
        this.bookingDAO = bookingDAO;
        this.fnbOrderDAO = fnbOrderDAO;
        this.voucherDAO = voucherDAO;
        this.fnbOrderService = fnbOrderService;
        this.bookingService = bookingService;
        this.fnbSaleService = fnbSaleService;
    }

    // ---------------------------------------------------------------- VNPay

    /**
     * Tạo giao dịch VNPay mới cho đơn (giao dịch VNPay đang chờ trước đó bị huỷ khi khách bấm thanh toán lại).
     * @return mã giao dịch, dùng làm vnp_TxnRef
     */
    public String createVnpayPayment(Booking booking) throws SQLException {
        String txnRef = booking.getCode() + System.currentTimeMillis();
        DbUtil.inTransaction(conn -> {
            paymentDAO.cancelPendingQr(conn, booking.getId());
            paymentDAO.insertBookingPayment(conn, booking.getId(), txnRef, "QR", booking.getTotalAmount(), null);
            return null;
        });
        return txnRef;
    }

    /** Kiểm tra chữ ký, số tiền rồi ghi nhận kết quả giao dịch VNPay của đơn đặt vé. */
    public VnpayProcessed processVnpay(Map<String, String> params) throws SQLException {
        if (!VNPayUtil.verify(params)) return new VnpayProcessed(VnpayResult.INVALID_SIGNATURE, null);
        VnpayRef ref = paymentDAO.findVnpay(params.get("vnp_TxnRef"));
        if (ref == null) return new VnpayProcessed(VnpayResult.NOT_FOUND, null);
        if (!VNPayUtil.toVnpAmount(ref.amount()).equals(params.get("vnp_Amount"))) {
            return new VnpayProcessed(VnpayResult.INVALID_AMOUNT, ref.bookingId());
        }
        if ("PAID".equals(ref.status())) return new VnpayProcessed(VnpayResult.ALREADY_PAID, ref.bookingId());
        if (!VNPayUtil.isSuccess(params)) {
            paymentDAO.markFailed(ref.paymentId());
            return new VnpayProcessed(VnpayResult.FAILED, ref.bookingId());
        }
        Outcome outcome = confirmPaid(ref.paymentId(), null);
        return new VnpayProcessed(switch (outcome.status()) {
            case SUCCESS -> VnpayResult.SUCCESS;
            case ALREADY_PAID -> VnpayResult.ALREADY_PAID;
            case PAID_BUT_SEATS_LOST -> VnpayResult.SEATS_LOST;
            case PAID_AMOUNT_MISMATCH -> VnpayResult.AMOUNT_CHANGED;
            case NOT_FOUND -> VnpayResult.NOT_FOUND;
        }, ref.bookingId());
    }

    /** Mã đơn bắp nước lẻ của giao dịch VNPay (vnp_TxnRef), null nếu là giao dịch vé. */
    public Integer findFnbOrderId(String txnRef) throws SQLException {
        FnbVnpayRef ref = paymentDAO.findFnbVnpay(txnRef);
        return ref == null ? null : ref.orderId();
    }

    /** Kiểm tra chữ ký, số tiền rồi ghi nhận kết quả giao dịch VNPay của đơn bắp nước lẻ. */
    public VnpayResult processFnbVnpay(Map<String, String> params) throws SQLException {
        FnbVnpayRef ref = paymentDAO.findFnbVnpay(params.get("vnp_TxnRef"));
        return ref == null ? VnpayResult.NOT_FOUND : processFnbVnpay(params, ref);
    }

    private VnpayResult processFnbVnpay(Map<String, String> params, FnbVnpayRef ref) throws SQLException {
        if (!VNPayUtil.verify(params)) return VnpayResult.INVALID_SIGNATURE;
        if (!VNPayUtil.toVnpAmount(ref.amount()).equals(params.get("vnp_Amount"))) return VnpayResult.INVALID_AMOUNT;
        if ("PAID".equals(ref.status())) return VnpayResult.ALREADY_PAID;
        if (!VNPayUtil.isSuccess(params)) {
            paymentDAO.markFailed(ref.paymentId());
            return VnpayResult.FAILED;
        }
        return switch (fnbSaleService.confirmPaid(ref.paymentId(), null)) {
            case SUCCESS -> VnpayResult.SUCCESS;
            case ALREADY_PAID -> VnpayResult.ALREADY_PAID;
            default -> VnpayResult.NOT_FOUND;
        };
    }

    /** IPN (VNPay gọi server-to-server): xử lý giao dịch vé hoặc bắp nước lẻ và trả mã phản hồi theo đặc tả. */
    public IpnResponse handleIpn(Map<String, String> params) throws SQLException {
        FnbVnpayRef fnb = paymentDAO.findFnbVnpay(params.get("vnp_TxnRef"));
        VnpayResult result = fnb != null ? processFnbVnpay(params, fnb) : processVnpay(params).result();
        return switch (result) {
            case SUCCESS, FAILED, SEATS_LOST, AMOUNT_CHANGED -> new IpnResponse("00", "Confirm Success");
            case ALREADY_PAID -> new IpnResponse("02", "Order already confirmed");
            case NOT_FOUND -> new IpnResponse("01", "Order not Found");
            case INVALID_AMOUNT -> new IpnResponse("04", "Invalid Amount");
            default -> new IpnResponse("97", "Invalid Checksum");
        };
    }

    // ---------------------------------------------------------------- tiền mặt tại quầy

    /**
     * Thu tiền mặt đơn tại quầy: tạo giao dịch CASH rồi xác nhận ngay.
     * @throws IllegalArgumentException tiền khách đưa không hợp lệ / ít hơn tổng tiền
     */
    public Outcome payCash(Booking booking, BigDecimal cashReceived, int centreId) throws SQLException {
        if (cashReceived == null || cashReceived.compareTo(booking.getTotalAmount()) < 0) {
            throw new IllegalArgumentException("Tiền khách đưa phải lớn hơn hoặc bằng tổng tiền của đơn.");
        }
        int paymentId = DbUtil.inTransaction(conn -> paymentDAO.insertBookingPayment(conn, booking.getId(),
                booking.getCode() + "-CASH-" + System.currentTimeMillis(), "CASH", booking.getTotalAmount(), cashReceived));
        return confirmPaid(paymentId, centreId);
    }

    // ---------------------------------------------------------------- xác nhận thanh toán

    /**
     * Ghi nhận đã nhận tiền (tiền mặt, hoặc VNPay báo thành công) và xuất vé, trong một transaction.
     * @param centreId rạp của nhân viên; null = mọi rạp (VNPay)
     */
    public Outcome confirmPaid(int paymentId, Integer centreId) throws SQLException {
        return DbUtil.inTransaction(conn -> {
            LockedPayment payment = paymentDAO.lockBookingPayment(conn, paymentId, centreId);
            if (payment == null) return new Outcome(Status.NOT_FOUND, null);
            int bookingId = payment.bookingId();
            if ("PAID".equals(payment.status())) return new Outcome(Status.ALREADY_PAID, bookingId);

            paymentDAO.markPaid(conn, paymentId);
            // A late callback must not issue tickets for a showtime edited after this booking.
            if (!paymentDAO.bookingStillMatchesShowtime(conn, bookingId)) {
                bookingDAO.updateStatus(conn, bookingId, "CANCELLED");
                fnbOrderDAO.cancelPendingForBookings(conn, List.of(bookingId));
                return new Outcome(Status.PAID_BUT_SEATS_LOST, bookingId);
            }
            // Giao dịch tạo trước khi khách đổi bắp nước / voucher: ghi nhận tiền nhưng không xác nhận đơn
            if (payment.amount().compareTo(payment.bookingTotal()) != 0) {
                return new Outcome(Status.PAID_AMOUNT_MISMATCH, bookingId);
            }

            try {
                // Thường là HELD; RELEASED khi VNPay báo thành công sau khi đơn đã hết hạn giữ ghế
                bookingDAO.confirmSeats(conn, bookingId);
            } catch (SQLException e) {
                if (!DbUtil.isDuplicateKey(e)) throw e;
                // Ghế đã thuộc đơn khác (unique index): huỷ đơn này, cần hoàn tiền cho khách
                bookingDAO.updateStatus(conn, bookingId, "CANCELLED");
                fnbOrderDAO.cancelPendingForBookings(conn, List.of(bookingId));
                return new Outcome(Status.PAID_BUT_SEATS_LOST, bookingId);
            }

            // Thanh toán xong là đơn đã xác nhận; suất chiếu kết thúc thì BookingCompletionJob chuyển sang COMPLETED
            bookingDAO.updateStatus(conn, bookingId, "CONFIRMED");
            // Bắp nước kèm vé -> PREPARING và trừ kho (gồm cả đơn CANCELLED khi tiền về sau khi hết hạn giữ ghế)
            if (fnbOrderDAO.markBookingOrdersPreparing(conn, bookingId) > 0) {
                fnbOrderService.deductStock(conn, fnbOrderDAO.getDemandForBooking(conn, bookingId));
            }
            recordVoucherUsage(conn, bookingId);
            paymentDAO.cancelOtherPending(conn, bookingId, paymentId);
            issueTickets(conn, bookingId);
            return new Outcome(Status.SUCCESS, bookingId);
        });
    }

    /**
     * Đơn đã thanh toán có voucher: tăng UsedCount; đơn của khách có tài khoản ghi thêm VoucherUsages
     * (để giới hạn lượt dùng mỗi khách). Khách vãng lai tại quầy không có UserID nên chỉ tăng UsedCount.
     */
    private void recordVoucherUsage(Connection conn, int bookingId) throws SQLException {
        VoucherOwner owner = bookingDAO.getVoucherOwner(conn, bookingId);
        if (owner.voucherId() == null) return;
        if (owner.userId() != null) voucherDAO.insertUsageIfAbsent(conn, owner.voucherId(), owner.userId(), bookingId);
        voucherDAO.incrementUsedCount(conn, owner.voucherId());
    }

    /** Mỗi ghế đã xác nhận một vé: mã TK + 10 ký tự, nội dung QR "CINEFLOW-TICKET:<mã>". */
    private void issueTickets(Connection conn, int bookingId) throws SQLException {
        List<NewTicket> tickets = new ArrayList<>();
        for (int bookingSeatId : bookingDAO.findConfirmedSeatsWithoutTicket(conn, bookingId)) {
            String code = CodeUtil.randomCode("TK", 10);
            tickets.add(new NewTicket(bookingSeatId, code, "CINEFLOW-TICKET:" + code));
        }
        paymentDAO.insertTickets(conn, tickets);
    }

    // ---------------------------------------------------------------- tra cứu

    /**
     * Lý do hiển thị trang thanh toán thất bại: CONFIRMED (đã thanh toán), CANCELLED, EXPIRED (hết giờ giữ ghế),
     * PAYMENT_FAILED (giao dịch VNPay gần nhất thất bại); null nếu đơn vẫn thanh toán được bình thường.
     */
    public String getFailureReason(Booking booking) throws SQLException {
        if (booking.isPaid()) return "CONFIRMED";
        if ("CANCELLED".equals(booking.getStatus())) return "CANCELLED";
        if (!booking.isHoldActive()) return "EXPIRED";
        if ("FAILED".equals(paymentDAO.latestQrStatus(booking.getId()))) return "PAYMENT_FAILED";
        return null;
    }

    public List<Ticket> getTickets(int bookingId) throws SQLException {
        return paymentDAO.getTickets(bookingId);
    }

    /** Tiền thối = tiền khách đưa - số tiền giao dịch; null nếu không phải tiền mặt. */
    static BigDecimal calculateChange(BigDecimal amount, BigDecimal cashReceived) {
        return cashReceived == null ? null : cashReceived.subtract(amount);
    }

    /** Điền tiền thối cho các giao dịch trước khi trả về cho servlet / JSP. */
    static List<PaymentTransaction> withChange(List<PaymentTransaction> transactions) {
        for (PaymentTransaction p : transactions) {
            p.setChange(calculateChange(p.getAmount(), p.getCashReceived()));
        }
        return transactions;
    }

    public List<PaymentTransaction> getTransactions(int bookingId) throws SQLException {
        return withChange(paymentDAO.getTransactions(bookingId));
    }

    /** Giao dịch đã thanh toán gần nhất của đơn (in hoá đơn), null nếu chưa có. */
    public PaymentTransaction getPaidTransaction(int bookingId) throws SQLException {
        PaymentTransaction paid = null;
        for (PaymentTransaction p : getTransactions(bookingId)) {
            if ("PAID".equals(p.getStatus())) paid = p;
        }
        return paid;
    }

    /**
     * Theo dõi giao dịch (vé + bắp nước lẻ), mới nhất trước, kèm tổng tiền / số giao dịch đã thu.
     * Cập nhật trước các đơn quá hạn để trạng thái hiển thị đúng.
     * @param centreId null = mọi rạp; status / method null = không lọc
     */
    public TrackingSummary getTransactionsForTracking(Integer centreId, String status, String method, int days)
            throws SQLException {
        DbUtil.inTransaction(conn -> {
            bookingService.releaseExpiredHolds(conn);
            fnbSaleService.expireStalePending(conn);
            return null;
        });
        List<Payment> payments = paymentDAO.getTransactionsForTracking(centreId, status, method, days);
        BigDecimal paidTotal = BigDecimal.ZERO;
        int paidCount = 0;
        for (Payment p : payments) {
            if ("PAID".equals(p.getStatus())) {
                paidTotal = paidTotal.add(p.getAmount());
                paidCount++;
            }
        }
        return new TrackingSummary(payments, paidTotal, paidCount);
    }
}
