package service;

import dao.FnbOrderDAO;
import dao.PaymentDAO;
import dao.PaymentDAO.LockedFnbPayment;
import model.dto.booking.FnbMenuItemDTO;
import model.dto.booking.FnbSaleDTO;
import model.dto.booking.PaymentTransaction;
import service.PaymentService.Status;
import utils.DbUtil;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Bán bắp nước lẻ tại quầy, không kèm vé (FnbOrders COUNTER, BookingID = NULL, khách vãng lai).
 * Đơn chưa thanh toán sau PENDING_MINUTES phút thì tự huỷ; thanh toán xong -> PREPARING và trừ kho.
 */
public class FnbSaleService {

    /** Đơn bắp nước lẻ chưa thanh toán quá số phút này thì tự huỷ (khách bỏ đi, VNPay không hoàn tất). */
    public static final int PENDING_MINUTES = 15;

    private final FnbOrderDAO fnbOrderDAO;
    private final PaymentDAO paymentDAO;
    private final FnbOrderService fnbOrderService;

    /** Khởi tạo với các DAO và service thật (dùng khi chạy ứng dụng). */
    public FnbSaleService() {
        this(new FnbOrderDAO(), new PaymentDAO(), new FnbOrderService());
    }

    /** Khởi tạo với DAO và service truyền vào từ ngoài (dùng khi viết test). */
    public FnbSaleService(FnbOrderDAO fnbOrderDAO, PaymentDAO paymentDAO, FnbOrderService fnbOrderService) {
        this.fnbOrderDAO = fnbOrderDAO;
        this.paymentDAO = paymentDAO;
        this.fnbOrderService = fnbOrderService;
    }

    /** Combo và món lẻ đang bán tại rạp (trang menu bán lẻ tại quầy). */
    public List<FnbMenuItemDTO> getMenu(int centreId) throws SQLException {
        return fnbOrderDAO.getMenu(centreId);
    }

    /**
     * Tạo đơn bắp nước lẻ chờ thanh toán, trong một transaction:
     * huỷ các đơn chờ quá hạn -> đọc giỏ -> lọc món hợp lệ (phải có ít nhất một món) -> kiểm tra kho -> ghi đơn PENDING (BookingID = NULL).
     * @return FnbOrderID
     * @throws IllegalArgumentException chưa chọn món, món hoặc số lượng không hợp lệ, không đủ hàng
     */
    public int createOrder(int centreId, Map<String, String[]> params) throws SQLException {
        Map<String, Integer> cart = FnbOrderService.readCart(params);
        return DbUtil.inTransaction(conn -> {
            expireStalePending(conn);
            Map<String, FnbMenuItemDTO> menu = fnbOrderService.loadMenu(conn, centreId);
            Map<String, Integer> lines = fnbOrderService.validLines(cart, menu);
            if (lines.isEmpty()) throw new IllegalArgumentException("Bạn chưa chọn món nào.");
            fnbOrderService.checkStock(conn, centreId, lines, menu);
            return fnbOrderService.createOrder(conn, null, centreId, null, "COUNTER", lines, menu);
        });
    }

    /**
     * Lấy đơn bắp nước lẻ thuộc rạp của nhân viên (trước đó huỷ các đơn chờ quá hạn); không có hoặc rạp khác thì null.
     */
    public FnbSaleDTO getOrder(int orderId, int centreId) throws SQLException {
        DbUtil.inTransaction(conn -> {
            expireStalePending(conn);
            return null;
        });
        FnbSaleDTO sale = fnbOrderDAO.getStandaloneOrder(orderId, PENDING_MINUTES);
        return sale == null || sale.getCentreId() != centreId ? null : sale;
    }

    /**
     * Huỷ đơn chưa thanh toán; giao dịch đang chờ -> CANCELLED.
     * @return false nếu đơn không còn chờ thanh toán
     */
    public boolean cancel(int orderId) throws SQLException {
        return DbUtil.inTransaction(conn -> {
            boolean cancelled = fnbOrderDAO.cancelPendingStandalone(conn, List.of(orderId)) > 0;
            if (cancelled) paymentDAO.updatePendingForOrders(conn, List.of(orderId), "CANCELLED");
            return cancelled;
        });
    }

    /**
     * Thu tiền mặt đơn bắp nước lẻ: tiền khách đưa phải đủ; ghi giao dịch CASH rồi xác nhận ngay (confirmPaid).
     * @throws IllegalArgumentException tiền khách đưa không hợp lệ hoặc thiếu
     */
    public Status payCash(FnbSaleDTO sale, BigDecimal cashReceived, int centreId) throws SQLException {
        if (cashReceived == null || cashReceived.compareTo(sale.getTotalAmount()) < 0) {
            throw new IllegalArgumentException("Tiền khách đưa phải lớn hơn hoặc bằng tổng tiền của đơn.");
        }
        int paymentId = DbUtil.inTransaction(conn -> paymentDAO.insertFnbPayment(conn, sale.getId(),
                sale.getCode() + "-CASH-" + System.currentTimeMillis(), "CASH", sale.getTotalAmount(), cashReceived));
        return confirmPaid(paymentId, centreId);
    }

    /**
     * Tạo giao dịch VNPay mới cho đơn bắp nước lẻ (huỷ giao dịch VNPay đang chờ trước đó).
     * @return mã giao dịch dùng làm vnp_TxnRef
     */
    public String createVnpayPayment(FnbSaleDTO sale) throws SQLException {
        String txnRef = sale.getCode() + System.currentTimeMillis();
        DbUtil.inTransaction(conn -> {
            paymentDAO.cancelPendingQrForOrder(conn, sale.getId());
            paymentDAO.insertFnbPayment(conn, sale.getId(), txnRef, "QR", sale.getTotalAmount(), null);
            return null;
        });
        return txnRef;
    }

    /**
     * Ghi nhận đã thu tiền đơn bắp nước lẻ, trong một transaction:
     * khoá giao dịch -> đã PAID thì dừng -> giao dịch PAID -> đơn PREPARING -> trừ kho -> huỷ các giao dịch chờ khác.
     */
    public Status confirmPaid(int paymentId, Integer centreId) throws SQLException {
        return DbUtil.inTransaction(conn -> {
            LockedFnbPayment payment = paymentDAO.lockFnbPayment(conn, paymentId, centreId);
            if (payment == null) return Status.NOT_FOUND;
            if ("PAID".equals(payment.status())) return Status.ALREADY_PAID;
            paymentDAO.markPaid(conn, paymentId);
            if (fnbOrderDAO.markStandalonePreparing(conn, payment.orderId()) > 0) {
                fnbOrderService.deductStock(conn, fnbOrderDAO.getDemandForOrder(conn, payment.orderId()));
            }
            paymentDAO.cancelOtherPendingForOrder(conn, payment.orderId(), paymentId);
            return Status.SUCCESS;
        });
    }

    /**
     * Giao dịch đã thanh toán gần nhất của đơn (in hoá đơn, tiền khách đưa và tiền thối); chưa có thì null.
     */
    public PaymentTransaction getPaidTransaction(int orderId) throws SQLException {
        PaymentTransaction paid = null;
        for (PaymentTransaction p : PaymentService.withChange(paymentDAO.getFnbTransactions(orderId))) {
            if ("PAID".equals(p.getStatus())) paid = p;
        }
        return paid;
    }

    /** Huỷ các đơn bắp nước lẻ chờ thanh toán quá PENDING_MINUTES (15) phút; giao dịch đang chờ -> TIMEOUT. */
    void expireStalePending(Connection conn) throws SQLException {
        List<Integer> stale = fnbOrderDAO.findStalePendingStandalone(conn, PENDING_MINUTES);
        paymentDAO.updatePendingForOrders(conn, stale, "TIMEOUT");
        fnbOrderDAO.cancelPendingStandalone(conn, stale);
    }
}
