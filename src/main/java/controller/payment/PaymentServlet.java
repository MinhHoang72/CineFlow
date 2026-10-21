package controller.payment;

import dao.BookingDAO;
import dao.PaymentDAO;
import dao.PaymentDAO.Outcome;
import dao.PaymentDAO.VnpayRef;
import model.entity.Booking;
import model.entity.User;
import utils.VNPayUtil;
import utils.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * FE-07 Thanh toan qua VNPay (sandbox), tu dong xac nhan khi VNPay bao ket qua:
 *   POST /payment/vnpay          tao giao dich va chuyen khach sang trang thanh toan VNPay
 *   GET  /payment/vnpay-return   VNPay chuyen khach ve sau khi thanh toan -> xu ly ket qua
 *   GET  /payment/vnpay-ipn      VNPay goi server-to-server (can URL public, vd ngrok)
 *   GET  /payment/failed         6.4 thanh toan that bai / het han, cho thanh toan lai hoac huy
 *   GET  /payment/result         6.3 -> chuyen sang chi tiet don (ve dien tu)
 * Return va IPN deu goi processVnpay; confirmPaid an toan khi goi lap lai.
 */
@WebServlet(name = "PaymentServlet", urlPatterns = {"/payment/vnpay", "/payment/vnpay-return",
        "/payment/vnpay-ipn", "/payment/failed", "/payment/result"})
public class PaymentServlet extends HttpServlet {

    private static final String VIEW_DIR = "/WEB-INF/views/";

    private final BookingDAO bookingDAO = new BookingDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();

    /** Ket qua xu ly du lieu VNPay gui ve. */
    enum VnpayResult { SUCCESS, ALREADY_PAID, SEATS_LOST, FAILED, NOT_FOUND, INVALID_AMOUNT, INVALID_SIGNATURE }

    record Processed(VnpayResult result, Integer bookingId) {}

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            switch (req.getServletPath()) {
                case "/payment/vnpay-ipn" -> ipn(req, resp);
                case "/payment/vnpay-return" -> vnpayReturn(req, resp);
                case "/payment/failed", "/payment/result" -> {
                    User user = WebUtil.requireUser(req, resp);
                    if (user == null) return;
                    Booking booking = loadOwnBooking(req, user);
                    if (booking == null) {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    } else if ("/payment/failed".equals(req.getServletPath())) {
                        showFailed(req, resp, booking);
                    } else {
                        resp.sendRedirect(detailUrl(req, booking));
                    }
                }
                default -> resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!"/payment/vnpay".equals(req.getServletPath())) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        User user = WebUtil.requireUser(req, resp);
        if (user == null) return;
        try {
            Booking booking = loadOwnBooking(req, user);
            if (booking == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if ("CONFIRMED".equals(booking.getStatus())) {
                resp.sendRedirect(detailUrl(req, booking));
                return;
            }
            if (!booking.isHoldActive()) {
                resp.sendRedirect(failedUrl(req, booking));
                return;
            }
            String payUrl = startVnpay(req, booking);
            if (payUrl == null) {
                resp.sendRedirect(req.getContextPath() + "/booking?action=checkout&bookingId=" + booking.getId());
                return;
            }
            resp.sendRedirect(payUrl);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    /**
     * Tao giao dich VNPay cho don dang giu ghe (dung chung cho dat ve online va ban tai quay).
     * @return URL trang thanh toan VNPay, null neu chua cau hinh VNPay (da flash loi)
     */
    public static String startVnpay(HttpServletRequest req, Booking booking) throws SQLException {
        if (!VNPayUtil.isConfigured()) {
            WebUtil.flash(req, "danger", "Chưa cấu hình VNPay: điền vnp.tmnCode và vnp.hashSecret trong "
                    + "src/main/resources/vnpay.properties rồi build lại.");
            return null;
        }
        String txnRef = new PaymentDAO().createVnpayPayment(booking.getId(), booking.getCode(), booking.getTotalAmount());
        return VNPayUtil.buildPayUrl(req, txnRef, booking.getTotalAmount(),
                "Thanh toan ve xem phim don " + booking.getCode(), booking.getSecondsLeft());
    }

    /** Kiem tra chu ky, so tien roi ghi nhan ket qua giao dich. */
    private Processed processVnpay(Map<String, String> params) throws SQLException {
        if (!VNPayUtil.verify(params)) return new Processed(VnpayResult.INVALID_SIGNATURE, null);
        VnpayRef ref = paymentDAO.findVnpay(params.get("vnp_TxnRef"));
        if (ref == null) return new Processed(VnpayResult.NOT_FOUND, null);
        if (!VNPayUtil.toVnpAmount(ref.amount()).equals(params.get("vnp_Amount"))) {
            return new Processed(VnpayResult.INVALID_AMOUNT, ref.bookingId());
        }
        if ("PAID".equals(ref.status())) return new Processed(VnpayResult.ALREADY_PAID, ref.bookingId());
        if (!VNPayUtil.isSuccess(params)) {
            paymentDAO.markFailed(ref.paymentId());
            return new Processed(VnpayResult.FAILED, ref.bookingId());
        }
        Outcome outcome = paymentDAO.confirmPaid(ref.paymentId(), null);
        return new Processed(switch (outcome.status()) {
            case SUCCESS -> VnpayResult.SUCCESS;
            case ALREADY_PAID -> VnpayResult.ALREADY_PAID;
            case PAID_BUT_SEATS_LOST -> VnpayResult.SEATS_LOST;
            case NOT_FOUND -> VnpayResult.NOT_FOUND;
        }, ref.bookingId());
    }

    /** IPN: VNPay goi den, tra JSON theo dac ta (RspCode/Message). */
    private void ipn(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String code;
        String message;
        try {
            Processed p = processVnpay(VNPayUtil.vnpParams(req));
            switch (p.result()) {
                case SUCCESS, FAILED, SEATS_LOST -> { code = "00"; message = "Confirm Success"; }
                case ALREADY_PAID -> { code = "02"; message = "Order already confirmed"; }
                case NOT_FOUND -> { code = "01"; message = "Order not Found"; }
                case INVALID_AMOUNT -> { code = "04"; message = "Invalid Amount"; }
                default -> { code = "97"; message = "Invalid Checksum"; }
            }
        } catch (SQLException | RuntimeException e) {
            log("VNPay IPN error", e);
            code = "99";
            message = "Unknown error";
        }
        resp.setContentType("application/json");
        resp.getWriter().write("{\"RspCode\":\"" + code + "\",\"Message\":\"" + message + "\"}");
    }

    /** Khach (hoac nhan vien quay) duoc VNPay chuyen ve. */
    private void vnpayReturn(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        Map<String, String> params = VNPayUtil.vnpParams(req);
        Processed p = processVnpay(params);
        String ctx = req.getContextPath();
        Booking booking = p.bookingId() == null ? null : bookingDAO.getBooking(p.bookingId());
        if (booking == null || p.result() == VnpayResult.INVALID_AMOUNT) {
            WebUtil.flash(req, "danger", "Dữ liệu thanh toán từ VNPay không hợp lệ.");
            resp.sendRedirect(ctx + "/booking?action=history");
            return;
        }
        if (booking.isCounter()) {
            switch (p.result()) {
                case SUCCESS, ALREADY_PAID -> {
                    WebUtil.flash(req, "success", "VNPay đã xác nhận thanh toán đơn " + booking.getCode() + ". In vé cho khách.");
                    resp.sendRedirect(ctx + "/staff/counter?action=ticket&bookingId=" + booking.getId());
                    return;
                }
                case SEATS_LOST -> WebUtil.flash(req, "danger", "Đơn đã hết hạn giữ ghế và ghế đã được người khác đặt. "
                        + "Đã ghi nhận tiền, cần hoàn lại cho khách.");
                default -> WebUtil.flash(req, "warning", "VNPay: " + VNPayUtil.failureMessage(params.get("vnp_ResponseCode")));
            }
            resp.sendRedirect(ctx + "/staff/counter?action=checkout&bookingId=" + booking.getId());
            return;
        }
        switch (p.result()) {
            case SUCCESS, ALREADY_PAID -> {
                WebUtil.flash(req, "success", "Thanh toán thành công! Vé điện tử của bạn đã sẵn sàng.");
                resp.sendRedirect(detailUrl(req, booking));
            }
            case SEATS_LOST -> {
                WebUtil.flash(req, "danger", "Thanh toán đã nhận nhưng đơn đã hết hạn giữ ghế và ghế đã có người khác đặt. "
                        + "Vui lòng liên hệ quầy vé để được hoàn tiền.");
                resp.sendRedirect(detailUrl(req, booking));
            }
            default -> {
                WebUtil.flash(req, "warning", VNPayUtil.failureMessage(params.get("vnp_ResponseCode")));
                resp.sendRedirect(failedUrl(req, booking));
            }
        }
    }

    /** 6.4: ly do that bai + cach xu ly. */
    private void showFailed(HttpServletRequest req, HttpServletResponse resp, Booking booking)
            throws SQLException, ServletException, IOException {
        if ("CONFIRMED".equals(booking.getStatus())) {
            resp.sendRedirect(detailUrl(req, booking));
            return;
        }
        String reason;
        if ("CANCELLED".equals(booking.getStatus())) {
            reason = "CANCELLED";
        } else if (!booking.isHoldActive()) {
            reason = "EXPIRED";
        } else if ("FAILED".equals(paymentDAO.latestQrStatus(booking.getId()))) {
            reason = "PAYMENT_FAILED";
        } else {
            resp.sendRedirect(req.getContextPath() + "/booking?action=checkout&bookingId=" + booking.getId());
            return;
        }
        req.setAttribute("booking", booking);
        req.setAttribute("reason", reason);
        req.getRequestDispatcher(VIEW_DIR + "payment/paymentFailed.jsp").forward(req, resp);
    }

    private Booking loadOwnBooking(HttpServletRequest req, User user) throws SQLException {
        Integer bookingId = WebUtil.parseInt(req.getParameter("bookingId"));
        if (bookingId == null) return null;
        Booking booking = bookingDAO.getBooking(bookingId);
        if (booking == null || booking.getUserId() == null || booking.getUserId() != user.getUserId()) return null;
        return booking;
    }

    private static String detailUrl(HttpServletRequest req, Booking booking) {
        return req.getContextPath() + "/booking?action=detail&bookingId=" + booking.getId();
    }

    private static String failedUrl(HttpServletRequest req, Booking booking) {
        return req.getContextPath() + "/payment/failed?bookingId=" + booking.getId();
    }
}
