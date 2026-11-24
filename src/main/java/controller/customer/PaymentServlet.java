package controller.customer;

import model.entity.Booking;
import model.entity.User;
import service.BookingService;
import service.PaymentService;
import service.PaymentService.IpnResponse;
import service.PaymentService.VnpayProcessed;
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
 * FE-07 Thanh toan qua VNPay (sandbox), tu dong xac nhan khi VNPay bao ket qua
 * (controller: doc request, goi PaymentService, chuyen trang):
 *   POST /payment/vnpay          tao giao dich va chuyen khach sang trang thanh toan VNPay
 *   GET  /payment/vnpay-return   VNPay chuyen khach ve sau khi thanh toan -> xu ly ket qua
 *   GET  /payment/vnpay-ipn      VNPay goi server-to-server (can URL public, vd ngrok)
 *   GET  /payment/failed         6.4 thanh toan that bai / het han, cho thanh toan lai hoac huy
 *   GET  /payment/result         6.3 -> chuyen sang chi tiet don (ve dien tu)
 * Return va IPN deu goi PaymentService.processVnpay; xac nhan an toan khi goi lap lai.
 */
@WebServlet(name = "PaymentServlet", urlPatterns = {"/payment/vnpay", "/payment/vnpay-return",
        "/payment/vnpay-ipn", "/payment/failed", "/payment/result"})
public class PaymentServlet extends HttpServlet {

    private static final String VIEW_DIR = "/WEB-INF/views/customer/";

    private final BookingService bookingService = new BookingService();
    private final PaymentService paymentService = new PaymentService();

    /**
     * Nhận các request GET của thanh toán, rẽ nhánh theo đường dẫn:
     * - /payment/vnpay-ipn -> ipn: VNPay gọi thẳng server báo kết quả
     * - /payment/vnpay-return -> vnpayReturn: VNPay chuyển trình duyệt của khách quay về
     * - /payment/failed, /payment/result -> trang thanh toán thất bại của đơn
     * Lưu ý: return và IPN là GET nhưng có ghi dữ liệu (theo quy định của VNPay), nên phía service phải chống xử lý lặp.
     */
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

    /**
     * Khách bấm "Thanh toán qua VNPay" (POST /payment/vnpay).
     * Luồng: bắt đăng nhập -> lấy đúng đơn của khách -> đơn đã trả thì sang trang chi tiết ->
     * đơn hết hạn giữ ghế thì sang trang thất bại -> startVnpay tạo giao dịch và link -> chuyển khách sang trang VNPay.
     */
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
            if (booking.isPaid()) {
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
     * Tạo giao dịch VNPay cho đơn đang giữ ghế và dựng link thanh toán. Dùng chung cho đặt vé online và bán vé tại quầy.
     * 1. Chưa cấu hình khoá VNPay thì báo lỗi, trả null.
     * 2. PaymentService.createVnpayPayment: huỷ giao dịch chờ cũ, ghi giao dịch PENDING mới, trả mã giao dịch (vnp_TxnRef).
     * 3. VNPayUtil.buildPayUrl: tạo link có chữ ký HMAC-SHA512, hết hạn cùng lúc với hạn giữ ghế.
     * @return link trang VNPay, hoặc null nếu chưa cấu hình VNPay
     */
    public static String startVnpay(HttpServletRequest req, Booking booking) throws SQLException {
        if (!VNPayUtil.isConfigured()) {
            WebUtil.flash(req, "danger", "Chưa cấu hình VNPay: điền vnp.tmnCode và vnp.hashSecret trong "
                    + "src/main/resources/vnpay.properties rồi build lại.");
            return null;
        }
        String txnRef = new PaymentService().createVnpayPayment(booking);
        return VNPayUtil.buildPayUrl(req, txnRef, booking.getTotalAmount(),
                "Thanh toan ve xem phim don " + booking.getCode(), booking.getSecondsLeft());
    }

    /**
     * VNPay trả kết quả cho đơn bắp nước lẻ (bán tại quầy).
     * Gọi PaymentService.processFnbVnpay; thành công thì mở hoá đơn, sai dữ liệu hoặc thất bại thì quay lại trang thu tiền kèm thông báo.
     */
    private void fnbReturn(HttpServletRequest req, HttpServletResponse resp, Map<String, String> params, int orderId)
            throws SQLException, IOException {
        String base = req.getContextPath() + "/staff/fnb-sale?orderId=" + orderId + "&action=";
        switch (paymentService.processFnbVnpay(params)) {
            case SUCCESS, ALREADY_PAID -> {
                WebUtil.flash(req, "success", "VNPay đã xác nhận thanh toán. In hoá đơn cho khách.");
                resp.sendRedirect(base + "receipt");
            }
            case INVALID_SIGNATURE, INVALID_AMOUNT -> {
                WebUtil.flash(req, "danger", "Dữ liệu thanh toán từ VNPay không hợp lệ.");
                resp.sendRedirect(base + "checkout");
            }
            default -> {
                WebUtil.flash(req, "warning", "VNPay: " + VNPayUtil.failureMessage(params.get("vnp_ResponseCode")));
                resp.sendRedirect(base + "checkout");
            }
        }
    }

    /**
     * IPN: server VNPay gọi thẳng server CineFlow để báo kết quả (dự phòng khi khách tắt trình duyệt).
     * Gọi PaymentService.handleIpn rồi trả JSON {"RspCode","Message"} theo đặc tả VNPay
     * (00 đã nhận, 02 đã xử lý trước đó, 01 không tìm thấy, 04 sai số tiền, 97 sai chữ ký, 99 lỗi khác).
     * Chạy ở localhost thì VNPay không gọi vào được.
     */
    private void ipn(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        IpnResponse result;
        try {
            result = paymentService.handleIpn(VNPayUtil.vnpParams(req));
        } catch (SQLException | RuntimeException e) {
            log("VNPay IPN error", e);
            result = new IpnResponse("99", "Unknown error");
        }
        resp.setContentType("application/json");
        resp.getWriter().write("{\"RspCode\":\"" + result.code() + "\",\"Message\":\"" + result.message() + "\"}");
    }

    /**
     * VNPay chuyển trình duyệt của khách (hoặc nhân viên quầy) quay về sau khi thanh toán.
     * Luồng:
     * 1. Gom các tham số vnp_* trên URL.
     * 2. Mã giao dịch là đơn bắp nước lẻ -> fnbReturn.
     * 3. Đơn vé -> PaymentService.processVnpay (kiểm tra chữ ký, số tiền, trạng thái, mã 00; thành công thì confirmPaid).
     * 4. Chuyển trang theo kết quả:
     *    - đơn quầy: thành công -> trang in vé; còn lại -> trang thu tiền kèm thông báo
     *    - đơn online: thành công -> chi tiết đơn; mất ghế / đơn đổi số tiền -> báo liên hệ hoàn tiền; thất bại -> trang thất bại
     */
    private void vnpayReturn(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        Map<String, String> params = VNPayUtil.vnpParams(req);
        String ctx = req.getContextPath();
        Integer fnbOrderId = paymentService.findFnbOrderId(params.get("vnp_TxnRef"));
        if (fnbOrderId != null) {
            fnbReturn(req, resp, params, fnbOrderId);
            return;
        }
        VnpayProcessed p = paymentService.processVnpay(params);
        Booking booking = p.bookingId() == null ? null : bookingService.getBooking(p.bookingId());
        if (booking == null || p.result() == PaymentService.VnpayResult.INVALID_AMOUNT) {
            WebUtil.flash(req, "danger", "Dữ liệu thanh toán từ VNPay không hợp lệ.");
            resp.sendRedirect(ctx + "/account");
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
                case AMOUNT_CHANGED -> WebUtil.flash(req, "danger", "Giao dịch được tạo trước khi đơn thay đổi (bắp nước / "
                        + "mã giảm giá). Đã ghi nhận tiền, cần hoàn lại cho khách và thanh toán lại.");
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
            case AMOUNT_CHANGED -> {
                WebUtil.flash(req, "danger", "Đơn đã thay đổi (bắp nước / mã giảm giá) sau khi bạn mở trang thanh toán nên "
                        + "giao dịch này không được dùng. Vui lòng liên hệ quầy vé để được hoàn tiền, rồi thanh toán lại đơn.");
                resp.sendRedirect(req.getContextPath() + "/booking?action=checkout&bookingId=" + booking.getId());
            }
            default -> {
                WebUtil.flash(req, "warning", VNPayUtil.failureMessage(params.get("vnp_ResponseCode")));
                resp.sendRedirect(failedUrl(req, booking));
            }
        }
    }

    /**
     * Trang thanh toán thất bại: lấy lý do qua PaymentService.getFailureReason.
     * Đơn đã thanh toán thì sang chi tiết đơn; không có lý do (đơn vẫn trả tiền được) thì về trang xem lại đơn;
     * còn lại mở paymentFailed.jsp (hết giờ giữ ghế, VNPay thất bại, đơn đã huỷ).
     */
    private void showFailed(HttpServletRequest req, HttpServletResponse resp, Booking booking)
            throws SQLException, ServletException, IOException {
        String reason = paymentService.getFailureReason(booking);
        if ("CONFIRMED".equals(reason)) {
            resp.sendRedirect(detailUrl(req, booking));
            return;
        }
        if (reason == null) {
            resp.sendRedirect(req.getContextPath() + "/booking?action=checkout&bookingId=" + booking.getId());
            return;
        }
        req.setAttribute("booking", booking);
        req.setAttribute("reason", reason);
        req.getRequestDispatcher(VIEW_DIR + "payment/paymentFailed.jsp").forward(req, resp);
    }

    /**
     * Đọc bookingId trên request và lấy đơn đó, chỉ khi đơn thuộc chính khách đang đăng nhập; ngược lại trả null.
     */
    private Booking loadOwnBooking(HttpServletRequest req, User user) throws SQLException {
        Integer bookingId = WebUtil.parseInt(req.getParameter("bookingId"));
        return bookingId == null ? null : bookingService.getOwnBooking(bookingId, user.getUserId());
    }

    /** Đường dẫn trang chi tiết đơn: /booking?action=detail&bookingId=... */
    private static String detailUrl(HttpServletRequest req, Booking booking) {
        return req.getContextPath() + "/booking?action=detail&bookingId=" + booking.getId();
    }

    /** Đường dẫn trang thanh toán thất bại của đơn: /payment/failed?bookingId=... */
    private static String failedUrl(HttpServletRequest req, Booking booking) {
        return req.getContextPath() + "/payment/failed?bookingId=" + booking.getId();
    }
}
