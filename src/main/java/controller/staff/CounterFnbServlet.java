package controller.staff;

import dao.FnbOrderDAO;
import dao.PaymentDAO;
import dao.PaymentDAO.Status;
import model.dto.booking.FnbMenuItemDTO;
import model.dto.booking.FnbSaleDTO;
import model.dto.booking.PaymentTransaction;
import model.entity.User;
import utils.VNPayUtil;
import utils.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bán bắp nước lẻ tại quầy, không kèm vé (dành cho TICKET_SALES_STAFF tại rạp của mình).
 * Đơn FnbOrders COUNTER, BookingID = NULL; chưa thanh toán sau 15 phút thì tự huỷ.
 *   GET  /staff/fnb-sale                      menu bắp nước của rạp
 *   POST /staff/fnb-sale?action=create        tạo đơn chờ thanh toán (qty_I{id}, qty_C{id})
 *   GET  /staff/fnb-sale?action=checkout      tóm tắt đơn + chọn cách thu tiền
 *   POST /staff/fnb-sale?action=cash          thu tiền mặt -> trừ kho, in hoá đơn
 *   POST /staff/fnb-sale?action=vnpay         chuyển sang VNPay (QR / thẻ); kết quả về /payment/vnpay-return
 *   POST /staff/fnb-sale?action=cancel        huỷ đơn chưa thanh toán
 *   GET  /staff/fnb-sale?action=receipt       hoá đơn
 */
@WebServlet(name = "CounterFnbServlet", urlPatterns = {"/staff/fnb-sale"})
public class CounterFnbServlet extends HttpServlet {

    private static final String VIEW_BASE = "/WEB-INF/views/staff/counter/";

    private final FnbOrderDAO fnbOrderDAO = new FnbOrderDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User staff = CounterShowtimeServlet.requireCounterStaff(req, resp);
        if (staff == null) return;
        String action = req.getParameter("action");
        try {
            if (action == null || action.isEmpty()) {
                menu(req, resp, staff);
            } else if ("checkout".equals(action)) {
                checkout(req, resp, staff);
            } else if ("receipt".equals(action)) {
                receipt(req, resp, staff);
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User staff = CounterShowtimeServlet.requireCounterStaff(req, resp);
        if (staff == null) return;
        String action = req.getParameter("action");
        try {
            switch (action == null ? "" : action) {
                case "create" -> create(req, resp, staff);
                case "cash" -> payCash(req, resp, staff);
                case "vnpay" -> payVnpay(req, resp, staff);
                case "cancel" -> cancel(req, resp, staff);
                default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void menu(HttpServletRequest req, HttpServletResponse resp, User staff)
            throws SQLException, ServletException, IOException {
        List<FnbMenuItemDTO> menu = fnbOrderDAO.getMenu(staff.getCentreId());
        req.setAttribute("fnbMenu", menu);
        req.getRequestDispatcher(VIEW_BASE + "fnbSale.jsp").forward(req, resp);
    }

    private void create(HttpServletRequest req, HttpServletResponse resp, User staff) throws SQLException, IOException {
        String ctx = req.getContextPath();
        try {
            int orderId = fnbOrderDAO.createStandaloneOrder(staff.getCentreId(), readCart(req.getParameterMap()));
            resp.sendRedirect(ctx + "/staff/fnb-sale?action=checkout&orderId=" + orderId);
        } catch (IllegalArgumentException e) {
            WebUtil.flash(req, "warning", e.getMessage());
            resp.sendRedirect(ctx + "/staff/fnb-sale");
        }
    }

    private void checkout(HttpServletRequest req, HttpServletResponse resp, User staff)
            throws SQLException, ServletException, IOException {
        FnbSaleDTO sale = loadSale(req, staff);
        if (sale == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (sale.isPaid()) {
            resp.sendRedirect(req.getContextPath() + "/staff/fnb-sale?action=receipt&orderId=" + sale.getId());
            return;
        }
        req.setAttribute("sale", sale);
        req.getRequestDispatcher(VIEW_BASE + "fnbSaleCheckout.jsp").forward(req, resp);
    }

    private void payCash(HttpServletRequest req, HttpServletResponse resp, User staff) throws SQLException, IOException {
        FnbSaleDTO sale = loadSale(req, staff);
        if (sale == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String ctx = req.getContextPath();
        String checkoutUrl = ctx + "/staff/fnb-sale?action=checkout&orderId=" + sale.getId();
        if (!sale.isPending()) {
            WebUtil.flash(req, "warning", "Đơn không còn chờ thanh toán.");
            resp.sendRedirect(checkoutUrl);
            return;
        }
        BigDecimal received = CounterShowtimeServlet.parseMoney(req.getParameter("cashReceived"));
        if (received == null || received.compareTo(sale.getTotalAmount()) < 0) {
            WebUtil.flash(req, "warning", "Tiền khách đưa phải lớn hơn hoặc bằng tổng tiền của đơn.");
            resp.sendRedirect(checkoutUrl);
            return;
        }
        int paymentId = paymentDAO.createFnbCashPayment(sale.getId(), sale.getCode(), sale.getTotalAmount(), received);
        Status status = paymentDAO.confirmFnbPaid(paymentId, staff.getCentreId());
        if (status == Status.NOT_FOUND) {
            WebUtil.flash(req, "danger", "Không tìm thấy giao dịch.");
            resp.sendRedirect(checkoutUrl);
            return;
        }
        WebUtil.flash(req, "success", "Đã thu tiền đơn " + sale.getCode() + ". In hoá đơn cho khách.");
        resp.sendRedirect(ctx + "/staff/fnb-sale?action=receipt&orderId=" + sale.getId());
    }

    /** Khách quét QR / nhập thẻ trên trang VNPay; VNPay chuyển về /payment/vnpay-return -> hoá đơn. */
    private void payVnpay(HttpServletRequest req, HttpServletResponse resp, User staff) throws SQLException, IOException {
        FnbSaleDTO sale = loadSale(req, staff);
        if (sale == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String checkoutUrl = req.getContextPath() + "/staff/fnb-sale?action=checkout&orderId=" + sale.getId();
        if (!sale.isPending()) {
            WebUtil.flash(req, "warning", "Đơn không còn chờ thanh toán.");
            resp.sendRedirect(checkoutUrl);
            return;
        }
        if (!VNPayUtil.isConfigured()) {
            WebUtil.flash(req, "danger", "Chưa cấu hình VNPay: điền vnp.tmnCode và vnp.hashSecret trong "
                    + "src/main/resources/vnpay.properties rồi build lại.");
            resp.sendRedirect(checkoutUrl);
            return;
        }
        String txnRef = paymentDAO.createFnbVnpayPayment(sale.getId(), sale.getCode(), sale.getTotalAmount());
        resp.sendRedirect(VNPayUtil.buildPayUrl(req, txnRef, sale.getTotalAmount(),
                "Thanh toan bap nuoc don " + sale.getCode(), sale.getSecondsLeft()));
    }

    private void cancel(HttpServletRequest req, HttpServletResponse resp, User staff) throws SQLException, IOException {
        FnbSaleDTO sale = loadSale(req, staff);
        if (sale == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (fnbOrderDAO.cancelStandaloneOrder(sale.getId())) {
            WebUtil.flash(req, "info", "Đã huỷ đơn " + sale.getCode() + ".");
        }
        resp.sendRedirect(req.getContextPath() + "/staff/fnb-sale");
    }

    private void receipt(HttpServletRequest req, HttpServletResponse resp, User staff)
            throws SQLException, ServletException, IOException {
        FnbSaleDTO sale = loadSale(req, staff);
        if (sale == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!sale.isPaid()) {
            resp.sendRedirect(req.getContextPath() + "/staff/fnb-sale?action=checkout&orderId=" + sale.getId());
            return;
        }
        PaymentTransaction paid = null;
        for (PaymentTransaction p : paymentDAO.getFnbTransactions(sale.getId())) {
            if ("PAID".equals(p.getStatus())) paid = p;
        }
        req.setAttribute("sale", sale);
        req.setAttribute("paidTransaction", paid);
        req.getRequestDispatcher(VIEW_BASE + "fnbSaleReceipt.jsp").forward(req, resp);
    }

    /** Đơn bắp nước lẻ thuộc rạp của nhân viên, null nếu không hợp lệ. */
    private FnbSaleDTO loadSale(HttpServletRequest req, User staff) throws SQLException {
        Integer orderId = WebUtil.parseInt(req.getParameter("orderId"));
        if (orderId == null) return null;
        FnbSaleDTO sale = fnbOrderDAO.getStandaloneOrder(orderId);
        return sale == null || sale.getCentreId() != staff.getCentreId() ? null : sale;
    }

    /** Tham số qty_I12 / qty_C3 -> giỏ hàng; số lượng không hợp lệ thì báo lỗi. */
    private static Map<String, Integer> readCart(Map<String, String[]> params) {
        Map<String, Integer> cart = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> e : params.entrySet()) {
            if (!e.getKey().matches("qty_[IC]\\d{1,9}")) continue;
            String raw = e.getValue().length == 0 ? "" : e.getValue()[0].trim();
            try {
                cart.put(e.getKey().substring(4), raw.isEmpty() ? 0 : Integer.parseInt(raw));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Số lượng bắp nước không hợp lệ.");
            }
        }
        return cart;
    }
}
