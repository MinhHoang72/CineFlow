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

    public FnbSaleService() {
        this(new FnbOrderDAO(), new PaymentDAO(), new FnbOrderService());
    }

    public FnbSaleService(FnbOrderDAO fnbOrderDAO, PaymentDAO paymentDAO, FnbOrderService fnbOrderService) {
        this.fnbOrderDAO = fnbOrderDAO;
        this.paymentDAO = paymentDAO;
        this.fnbOrderService = fnbOrderService;
    }

    /** Combo + món lẻ đang bán tại rạp. */
    public List<FnbMenuItemDTO> getMenu(int centreId) throws SQLException {
        return fnbOrderDAO.getMenu(centreId);
    }

    /**
     * Tạo đơn chờ thanh toán từ form (qty_I12 / qty_C3).
     * @return FnbOrderID
     * @throws IllegalArgumentException chưa chọn món / món không còn bán / vượt tồn kho / số lượng sai
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

    /** Đơn bắp nước lẻ thuộc rạp, null nếu không có / rạp khác. Huỷ trước các đơn chờ quá hạn. */
    public FnbSaleDTO getOrder(int orderId, int centreId) throws SQLException {
        DbUtil.inTransaction(conn -> {
            expireStalePending(conn);
            return null;
        });
        FnbSaleDTO sale = fnbOrderDAO.getStandaloneOrder(orderId, PENDING_MINUTES);
        return sale == null || sale.getCentreId() != centreId ? null : sale;
    }

    /** Huỷ đơn chưa thanh toán, giao dịch đang chờ -> CANCELLED. @return false nếu đơn không còn chờ thanh toán */
    public boolean cancel(int orderId) throws SQLException {
        return DbUtil.inTransaction(conn -> {
            boolean cancelled = fnbOrderDAO.cancelPendingStandalone(conn, List.of(orderId)) > 0;
            if (cancelled) paymentDAO.updatePendingForOrders(conn, List.of(orderId), "CANCELLED");
            return cancelled;
        });
    }

    /**
     * Thu tiền mặt: tạo giao dịch CASH rồi xác nhận ngay.
     * @throws IllegalArgumentException tiền khách đưa không hợp lệ / ít hơn tổng tiền
     */
    public Status payCash(FnbSaleDTO sale, BigDecimal cashReceived, int centreId) throws SQLException {
        if (cashReceived == null || cashReceived.compareTo(sale.getTotalAmount()) < 0) {
            throw new IllegalArgumentException("Tiền khách đưa phải lớn hơn hoặc bằng tổng tiền của đơn.");
        }
        int paymentId = DbUtil.inTransaction(conn -> paymentDAO.insertFnbPayment(conn, sale.getId(),
                sale.getCode() + "-CASH-" + System.currentTimeMillis(), "CASH", sale.getTotalAmount(), cashReceived));
        return confirmPaid(paymentId, centreId);
    }

    /** Giao dịch VNPay mới cho đơn (huỷ giao dịch VNPay đang chờ trước đó). @return vnp_TxnRef */
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
     * Ghi nhận đã thu tiền (tiền mặt, hoặc VNPay báo thành công): giao dịch PAID, đơn -> PREPARING, trừ kho.
     * Gồm cả đơn CANCELLED (khách trả VNPay muộn sau khi đơn hết hạn chờ). Gọi lặp lại vẫn an toàn.
     * @param centreId rạp của nhân viên; null = mọi rạp (VNPay)
     * @return SUCCESS, ALREADY_PAID hoặc NOT_FOUND
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

    /** Giao dịch đã thanh toán gần nhất của đơn (in hoá đơn), null nếu chưa có. */
    public PaymentTransaction getPaidTransaction(int orderId) throws SQLException {
        PaymentTransaction paid = null;
        for (PaymentTransaction p : PaymentService.withChange(paymentDAO.getFnbTransactions(orderId))) {
            if ("PAID".equals(p.getStatus())) paid = p;
        }
        return paid;
    }

    /** Huỷ các đơn chờ thanh toán quá PENDING_MINUTES phút, giao dịch đang chờ -> TIMEOUT. */
    void expireStalePending(Connection conn) throws SQLException {
        List<Integer> stale = fnbOrderDAO.findStalePendingStandalone(conn, PENDING_MINUTES);
        paymentDAO.updatePendingForOrders(conn, stale, "TIMEOUT");
        fnbOrderDAO.cancelPendingStandalone(conn, stale);
    }
}
