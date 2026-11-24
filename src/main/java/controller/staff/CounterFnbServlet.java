package controller.staff;

import model.dto.booking.FnbSaleDTO;
import model.entity.User;
import service.FnbSaleService;
import service.PaymentService.Status;
import utils.VNPayUtil;
import utils.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Bán bắp nước lẻ tại quầy, không kèm vé (dành cho TICKET_SALES_STAFF tại rạp của mình).
 * Đơn FnbOrders COUNTER, BookingID = NULL; chưa thanh toán sau 15 phút thì tự huỷ. Nghiệp vụ nằm ở FnbSaleService.
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

    private final FnbSaleService fnbSaleService = new FnbSaleService();

    /**
     * Nhận các request GET của bán lẻ bắp nước tại quầy (/staff/fnb-sale). Chỉ nhân viên bán vé đã gán rạp.
     * Rẽ nhánh theo action: rỗng -> menu; checkout -> trang thu tiền; receipt -> hoá đơn.
     */
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

    /**
     * Nhận các thao tác POST của bán lẻ bắp nước:
     * - create: tạo đơn chờ thanh toán
     * - cash: thu tiền mặt
     * - vnpay: chuyển sang VNPay
     * - cancel: huỷ đơn
     */
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

    /** Trang menu bắp nước của rạp nhân viên (món lẻ và combo đang bán). */
    private void menu(HttpServletRequest req, HttpServletResponse resp, User staff)
            throws SQLException, ServletException, IOException {
        req.setAttribute("fnbMenu", fnbSaleService.getMenu(staff.getCentreId()));
        req.getRequestDispatcher(VIEW_BASE + "fnbSale.jsp").forward(req, resp);
    }

    /**
     * Tạo đơn bắp nước lẻ từ số lượng nhân viên nhập (POST action=create) qua FnbSaleService.createOrder.
     * Thành công -> trang thu tiền; lỗi (chưa chọn món, hết hàng...) -> quay lại menu kèm thông báo.
     */
    private void create(HttpServletRequest req, HttpServletResponse resp, User staff) throws SQLException, IOException {
        String ctx = req.getContextPath();
        try {
            int orderId = fnbSaleService.createOrder(staff.getCentreId(), req.getParameterMap());
            resp.sendRedirect(ctx + "/staff/fnb-sale?action=checkout&orderId=" + orderId);
        } catch (IllegalArgumentException e) {
            WebUtil.flash(req, "warning", e.getMessage());
            resp.sendRedirect(ctx + "/staff/fnb-sale");
        }
    }

    /** Trang thu tiền của đơn bắp nước lẻ; đơn đã thanh toán thì chuyển sang hoá đơn. */
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

    /**
     * Thu tiền mặt cho đơn bắp nước lẻ: đơn phải còn chờ thanh toán; gọi FnbSaleService.payCash
     * (tiền đưa phải đủ; tạo giao dịch CASH, xác nhận, trừ kho). Thành công -> trang hoá đơn.
     */
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
        Status status;
        try {
            status = fnbSaleService.payCash(sale, CounterShowtimeServlet.parseMoney(req.getParameter("cashReceived")),
                    staff.getCentreId());
        } catch (IllegalArgumentException e) {
            WebUtil.flash(req, "warning", e.getMessage());
            resp.sendRedirect(checkoutUrl);
            return;
        }
        if (status == Status.NOT_FOUND) {
            WebUtil.flash(req, "danger", "Không tìm thấy giao dịch.");
            resp.sendRedirect(checkoutUrl);
            return;
        }
        WebUtil.flash(req, "success", "Đã thu tiền đơn " + sale.getCode() + ". In hoá đơn cho khách.");
        resp.sendRedirect(ctx + "/staff/fnb-sale?action=receipt&orderId=" + sale.getId());
    }

    /**
     * Khách trả đơn bắp nước lẻ bằng VNPay: tạo giao dịch (FnbSaleService.createVnpayPayment) và link (VNPayUtil.buildPayUrl).
     * VNPay trả về /payment/vnpay-return -> PaymentServlet.fnbReturn -> hoá đơn.
     */
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
        String txnRef = fnbSaleService.createVnpayPayment(sale);
        resp.sendRedirect(VNPayUtil.buildPayUrl(req, txnRef, sale.getTotalAmount(),
                "Thanh toan bap nuoc don " + sale.getCode(), sale.getSecondsLeft()));
    }

    /** Huỷ đơn bắp nước lẻ chưa thanh toán rồi quay về menu. */
    private void cancel(HttpServletRequest req, HttpServletResponse resp, User staff) throws SQLException, IOException {
        FnbSaleDTO sale = loadSale(req, staff);
        if (sale == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (fnbSaleService.cancel(sale.getId())) {
            WebUtil.flash(req, "info", "Đã huỷ đơn " + sale.getCode() + ".");
        }
        resp.sendRedirect(req.getContextPath() + "/staff/fnb-sale");
    }

    /**
     * Trang hoá đơn bắp nước lẻ: chỉ mở khi đơn đã thanh toán; kèm giao dịch đã thu để in tiền khách đưa và tiền thối.
     */
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
        req.setAttribute("sale", sale);
        req.setAttribute("paidTransaction", fnbSaleService.getPaidTransaction(sale.getId()));
        req.getRequestDispatcher(VIEW_BASE + "fnbSaleReceipt.jsp").forward(req, resp);
    }

    /**
     * Đọc orderId và lấy đơn bắp nước lẻ thuộc rạp của nhân viên; không có hoặc thuộc rạp khác thì trả null.
     */
    private FnbSaleDTO loadSale(HttpServletRequest req, User staff) throws SQLException {
        Integer orderId = WebUtil.parseInt(req.getParameter("orderId"));
        return orderId == null ? null : fnbSaleService.getOrder(orderId, staff.getCentreId());
    }
}
