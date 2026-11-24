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

    /**
     * Kết quả xác nhận thanh toán: status (thành công, đã trả trước đó, mất ghế, lệch số tiền, không tìm thấy) và bookingId.
     */
    public record Outcome(Status status, Integer bookingId) {}

    /** Kết quả xử lý dữ liệu VNPay gửi về. */
    public enum VnpayResult { SUCCESS, ALREADY_PAID, SEATS_LOST, AMOUNT_CHANGED, FAILED, NOT_FOUND, INVALID_AMOUNT, INVALID_SIGNATURE }

    /**
     * Kết quả xử lý dữ liệu VNPay trả về của đơn vé: kết quả kiểm tra và bookingId để servlet chuyển trang.
     */
    public record VnpayProcessed(VnpayResult result, Integer bookingId) {}

    /** Phản hồi gửi lại cho VNPay khi nhận IPN: mã RspCode và Message theo đặc tả VNPay. */
    public record IpnResponse(String code, String message) {}

    /** Dữ liệu trang theo dõi giao dịch: danh sách giao dịch, tổng tiền đã thu và số giao dịch đã thu. */
    public record TrackingSummary(List<Payment> payments, BigDecimal paidTotal, int paidCount) {}

    private final PaymentDAO paymentDAO;
    private final BookingDAO bookingDAO;
    private final FnbOrderDAO fnbOrderDAO;
    private final BookingVoucherDAO voucherDAO;
    private final FnbOrderService fnbOrderService;
    private final BookingService bookingService;
    private final FnbSaleService fnbSaleService;

    /** Khởi tạo với các DAO và service thật (dùng khi chạy ứng dụng). */
    public PaymentService() {
        this(new PaymentDAO(), new BookingDAO(), new FnbOrderDAO(), new BookingVoucherDAO(), new FnbOrderService(),
                new BookingService(), new FnbSaleService());
    }

    /** Khởi tạo với DAO và service truyền vào từ ngoài (dùng khi viết test). */
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
     * Tạo giao dịch VNPay mới cho đơn vé, trong một transaction:
     * 1. Sinh mã giao dịch = mã đơn + thời gian hiện tại (mili giây), mỗi lần thanh toán một mã riêng.
     * 2. Huỷ giao dịch VNPay đang chờ trước đó của đơn (khách bấm thanh toán lại).
     * 3. Ghi giao dịch mới PENDING, PaymentMethod = QR, Amount = tổng đơn.
     * @return mã giao dịch, gửi cho VNPay làm vnp_TxnRef
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

    /**
     * Kiểm tra dữ liệu VNPay trả về rồi ghi nhận kết quả cho đơn vé. Không tin dữ liệu ngay vì nó đi qua trình duyệt của khách:
     * 1. Chữ ký HMAC-SHA512 đúng (VNPayUtil.verify), sai thì INVALID_SIGNATURE.
     * 2. Mã giao dịch (vnp_TxnRef) có trong CSDL, không có thì NOT_FOUND.
     * 3. Số tiền khớp giao dịch đã tạo, lệch thì INVALID_AMOUNT.
     * 4. Giao dịch đã PAID thì ALREADY_PAID (tránh xử lý hai lần khi cả return URL và IPN cùng về).
     * 5. Mã kết quả khác 00 thì ghi FAILED.
     * 6. Thành công thì gọi confirmPaid và đổi kết quả của nó sang VnpayResult.
     * Gọi từ: PaymentServlet.vnpayReturn và handleIpn.
     */
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

    /**
     * Mã giao dịch VNPay này thuộc đơn bắp nước lẻ nào; null nếu là giao dịch của đơn vé (để servlet rẽ nhánh).
     */
    public Integer findFnbOrderId(String txnRef) throws SQLException {
        FnbVnpayRef ref = paymentDAO.findFnbVnpay(txnRef);
        return ref == null ? null : ref.orderId();
    }

    /**
     * Kiểm tra và ghi nhận kết quả VNPay cho đơn bắp nước lẻ (tìm giao dịch theo vnp_TxnRef rồi gọi bản bên dưới).
     */
    public VnpayResult processFnbVnpay(Map<String, String> params) throws SQLException {
        FnbVnpayRef ref = paymentDAO.findFnbVnpay(params.get("vnp_TxnRef"));
        return ref == null ? VnpayResult.NOT_FOUND : processFnbVnpay(params, ref);
    }

    /**
     * Các bước giống processVnpay nhưng cho đơn bắp nước lẻ: chữ ký, số tiền, đã PAID chưa, mã 00;
     * thành công thì FnbSaleService.confirmPaid (đơn PREPARING, trừ kho).
     */
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

    /**
     * Xử lý IPN (VNPay gọi thẳng server): rẽ sang đơn bắp nước lẻ hoặc đơn vé, rồi đổi kết quả thành mã phản hồi cho VNPay:
     * 00 = đã nhận (kể cả thất bại hay mất ghế vì giao dịch đã được ghi nhận), 02 = đã xử lý trước đó,
     * 01 = không tìm thấy, 04 = sai số tiền, 97 = sai chữ ký.
     */
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
     * Thu tiền mặt đơn tại quầy: tiền khách đưa phải lớn hơn hoặc bằng tổng đơn;
     * ghi giao dịch CASH (lưu tiền khách đưa để in tiền thối) rồi xác nhận ngay bằng confirmPaid (chỉ đơn của rạp nhân viên).
     * @throws IllegalArgumentException tiền khách đưa không hợp lệ hoặc ít hơn tổng tiền
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
     * Ghi nhận đã nhận tiền (tiền mặt, hoặc VNPay báo thành công) và xuất vé. Toàn bộ chạy trong một transaction:
     * 1. Khoá dòng giao dịch (UPDLOCK); không có thì NOT_FOUND; đã PAID thì dừng (ALREADY_PAID), không xuất vé lần hai.
     * 2. Giao dịch -> PAID.
     * 3. Suất chiếu đã bị sửa hoặc không còn bán (rạp, phòng, phim, phân phối) -> huỷ đơn, cần hoàn tiền (PAID_BUT_SEATS_LOST).
     * 4. Số tiền giao dịch lệch tổng đơn (khách đổi bắp nước hoặc voucher sau khi mở VNPay) -> chỉ ghi nhận tiền (PAID_AMOUNT_MISMATCH).
     * 5. Ghế -> CONFIRMED; ghế đã thuộc người khác (unique index báo trùng) -> huỷ đơn, cần hoàn tiền (PAID_BUT_SEATS_LOST).
     * 6. Đơn -> CONFIRMED; bắp nước -> PREPARING và trừ kho; ghi lượt dùng voucher; huỷ các giao dịch chờ khác; xuất vé.
     * @param centreId rạp của nhân viên khi thu tại quầy; null = không giới hạn rạp (VNPay)
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
     * Đơn đã thanh toán có voucher: tăng UsedCount của voucher; đơn của khách có tài khoản ghi thêm VoucherUsages
     * (để giới hạn lượt dùng mỗi khách). Khách vãng lai tại quầy không có tài khoản nên chỉ tăng UsedCount.
     */
    private void recordVoucherUsage(Connection conn, int bookingId) throws SQLException {
        VoucherOwner owner = bookingDAO.getVoucherOwner(conn, bookingId);
        if (owner.voucherId() == null) return;
        if (owner.userId() != null) voucherDAO.insertUsageIfAbsent(conn, owner.voucherId(), owner.userId(), bookingId);
        voucherDAO.incrementUsedCount(conn, owner.voucherId());
    }

    /**
     * Xuất vé: mỗi ghế đã CONFIRMED mà chưa có vé được tạo một vé, mã vé = "TK" + 10 ký tự ngẫu nhiên.
     * Cột QRCode vẫn được lưu để giữ ràng buộc CSDL nhưng không hiện trên vé.
     */
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
     * Lý do hiện trên trang thanh toán thất bại:
     * CONFIRMED (đơn đã thanh toán), CANCELLED (đã huỷ), EXPIRED (hết giờ giữ ghế), PAYMENT_FAILED (giao dịch VNPay gần nhất thất bại);
     * null nếu đơn vẫn thanh toán được bình thường.
     */
    public String getFailureReason(Booking booking) throws SQLException {
        if (booking.isPaid()) return "CONFIRMED";
        if ("CANCELLED".equals(booking.getStatus())) return "CANCELLED";
        if (!booking.isHoldActive()) return "EXPIRED";
        if ("FAILED".equals(paymentDAO.latestQrStatus(booking.getId()))) return "PAYMENT_FAILED";
        return null;
    }

    /** Danh sách vé của đơn (mỗi ghế một vé) để hiện trên trang chi tiết đơn và trang in vé. */
    public List<Ticket> getTickets(int bookingId) throws SQLException {
        return paymentDAO.getTickets(bookingId);
    }

    /** Tiền thối = tiền khách đưa - số tiền giao dịch; null nếu không phải giao dịch tiền mặt. */
    static BigDecimal calculateChange(BigDecimal amount, BigDecimal cashReceived) {
        return cashReceived == null ? null : cashReceived.subtract(amount);
    }

    /** Điền tiền thối cho từng giao dịch trước khi trả về cho servlet / JSP. */
    static List<PaymentTransaction> withChange(List<PaymentTransaction> transactions) {
        for (PaymentTransaction p : transactions) {
            p.setChange(calculateChange(p.getAmount(), p.getCashReceived()));
        }
        return transactions;
    }

    /** Tất cả giao dịch của đơn (đã điền tiền thối), hiện ở trang chi tiết đơn. */
    public List<PaymentTransaction> getTransactions(int bookingId) throws SQLException {
        return withChange(paymentDAO.getTransactions(bookingId));
    }

    /** Giao dịch đã thanh toán gần nhất của đơn (dùng khi in hoá đơn); chưa có thì null. */
    public PaymentTransaction getPaidTransaction(int bookingId) throws SQLException {
        PaymentTransaction paid = null;
        for (PaymentTransaction p : getTransactions(bookingId)) {
            if ("PAID".equals(p.getStatus())) paid = p;
        }
        return paid;
    }

    /**
     * Dữ liệu trang theo dõi giao dịch (vé và bắp nước lẻ), mới nhất trước, kèm tổng tiền và số giao dịch đã thu.
     * Trước khi đọc, dọn các đơn quá hạn giữ ghế để trạng thái giao dịch hiện đúng.
     * @param centreId null = mọi rạp; status, method null = không lọc; days = số ngày gần nhất
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
