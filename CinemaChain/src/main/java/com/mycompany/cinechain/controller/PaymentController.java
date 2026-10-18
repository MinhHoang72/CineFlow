package com.mycompany.cinechain.controller;

import com.mycompany.cinechain.dao.BookingDAO;
import com.mycompany.cinechain.dao.PaymentDAO;
import com.mycompany.cinechain.model.Booking;
import com.mycompany.cinechain.model.User;
import com.mycompany.cinechain.utils.QrUtil;
import com.mycompany.cinechain.utils.VietQrUtil;
import com.mycompany.cinechain.utils.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * FE-07 Thanh toan bang chuyen khoan VietQR (nhan vien xac nhan thu cong):
 *   GET  /payment/qr       6.1 hien ma QR chuyen khoan cho booking dang giu ghe
 *   GET  /payment/status   JSON trang thai don + giao dich, trang QR goi dinh ky
 *   GET  /payment/failed   6.4 thanh toan that bai / het han, cho thanh toan lai hoac huy
 *   POST /payment/retry    tao giao dich QR moi (khi don con giu ghe)
 *   GET  /payment/result   6.3 -> chuyen sang chi tiet don (ve dien tu)
 */
@WebServlet(name = "PaymentController",
        urlPatterns = {"/payment/qr", "/payment/status", "/payment/failed", "/payment/retry", "/payment/result"})
public class PaymentController extends HttpServlet {

    private static final String VIEW_DIR = "/WEB-INF/views/";

    private final BookingDAO bookingDAO = new BookingDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = WebUtil.requireUser(req, resp);
        if (user == null) return;
        try {
            Booking booking = loadOwnBooking(req, user);
            if (booking == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            switch (req.getServletPath()) {
                case "/payment/qr" -> showQr(req, resp, booking);
                case "/payment/status" -> status(resp, booking);
                case "/payment/failed" -> showFailed(req, resp, booking);
                case "/payment/result" -> resp.sendRedirect(detailUrl(req, booking));
                default -> resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!"/payment/retry".equals(req.getServletPath())) {
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
            if (!booking.isHoldActive()) {
                resp.sendRedirect(failedUrl(req, booking));
                return;
            }
            paymentDAO.retryQr(booking.getId(), booking.getCode(), booking.getTotalAmount());
            WebUtil.flash(req, "info", "Đã tạo giao dịch thanh toán mới, vui lòng chuyển khoản lại.");
            resp.sendRedirect(req.getContextPath() + "/payment/qr?bookingId=" + booking.getId());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void showQr(HttpServletRequest req, HttpServletResponse resp, Booking booking)
            throws SQLException, ServletException, IOException {
        if ("CONFIRMED".equals(booking.getStatus())) {
            resp.sendRedirect(detailUrl(req, booking));
            return;
        }
        if (!booking.isHoldActive() || "FAILED".equals(paymentDAO.latestQrStatus(booking.getId()))) {
            resp.sendRedirect(failedUrl(req, booking));
            return;
        }
        if (!VietQrUtil.isConfigured()) {
            WebUtil.flash(req, "danger", "Chưa cấu hình tài khoản nhận tiền: điền bank.accountNo và bank.accountName "
                    + "trong src/main/resources/bank.properties rồi build lại.");
            resp.sendRedirect(req.getContextPath() + "/booking?action=checkout&bookingId=" + booking.getId());
            return;
        }

        paymentDAO.getOrCreatePendingQr(booking.getId(), booking.getCode(), booking.getTotalAmount());
        long amount = booking.getTotalAmount().longValue();

        req.setAttribute("booking", booking);
        req.setAttribute("qrImage", QrUtil.toDataUri(VietQrUtil.buildPayload(amount, booking.getCode())));
        req.setAttribute("bankName", VietQrUtil.bankName());
        req.setAttribute("accountNo", VietQrUtil.accountNo());
        req.setAttribute("accountName", VietQrUtil.accountName());
        req.getRequestDispatcher(VIEW_DIR + "payment-qr.jsp").forward(req, resp);
    }

    private void status(HttpServletResponse resp, Booking booking) throws SQLException, IOException {
        String status = booking.isPending() && !booking.isHoldActive() ? "EXPIRED" : booking.getStatus();
        String paymentStatus = paymentDAO.latestQrStatus(booking.getId());
        resp.setContentType("application/json");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write("{\"status\":\"" + status + "\",\"paymentStatus\":\""
                + (paymentStatus == null ? "" : paymentStatus) + "\",\"secondsLeft\":" + booking.getSecondsLeft() + "}");
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
            resp.sendRedirect(req.getContextPath() + "/payment/qr?bookingId=" + booking.getId());
            return;
        }
        req.setAttribute("booking", booking);
        req.setAttribute("reason", reason);
        req.getRequestDispatcher(VIEW_DIR + "payment-failed.jsp").forward(req, resp);
    }

    private Booking loadOwnBooking(HttpServletRequest req, User user) throws SQLException {
        Integer bookingId = WebUtil.parseInt(req.getParameter("bookingId"));
        if (bookingId == null) return null;
        Booking booking = bookingDAO.getBooking(bookingId);
        if (booking == null || booking.getUserId() == null || booking.getUserId() != user.getId()) return null;
        return booking;
    }

    private static String detailUrl(HttpServletRequest req, Booking booking) {
        return req.getContextPath() + "/booking?action=detail&bookingId=" + booking.getId();
    }

    private static String failedUrl(HttpServletRequest req, Booking booking) {
        return req.getContextPath() + "/payment/failed?bookingId=" + booking.getId();
    }
}
