package controller.staff;

import model.entity.User;
import service.PaymentService;
import service.PaymentService.TrackingSummary;
import utils.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;

/**
 * FE-07 Theo dõi giao dịch thanh toán (chỉ xem; VNPay tự xác nhận, tiền mặt xác nhận tại quầy):
 *   GET /staff/payments or /manager/payments?status=&method=&days=
 * Nhân viên bán vé chỉ thấy chi nhánh của mình; MANAGER/ADMIN thấy tất cả.
 */
@WebServlet(name = "StaffPaymentServlet", urlPatterns = {"/staff/payments", "/manager/payments"})
public class StaffPaymentServlet extends HttpServlet {

    private static final Set<String> STATUSES = Set.of("PENDING", "PAID", "FAILED", "TIMEOUT", "CANCELLED");
    private static final Set<String> METHODS = Set.of("QR", "CASH");
    private static final Set<Integer> DAY_OPTIONS = Set.of(1, 7, 30);

    private final PaymentService paymentService = new PaymentService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User staff = WebUtil.requireUser(req, resp);
        if (staff == null) return;
        if (!WebUtil.canConfirmPayments(staff)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String status = allowed(STATUSES, req.getParameter("status"));
        String method = allowed(METHODS, req.getParameter("method"));
        Integer days = WebUtil.parseInt(req.getParameter("days"));
        if (days == null || !DAY_OPTIONS.contains(days)) days = 1;

        TrackingSummary summary;
        try {
            summary = paymentService.getTransactionsForTracking(centreScope(staff), status, method, days);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        req.setAttribute("payments", summary.payments());
        req.setAttribute("paidTotal", summary.paidTotal());
        req.setAttribute("paidCount", summary.paidCount());
        req.setAttribute("status", status);
        req.setAttribute("method", method);
        req.setAttribute("days", days);
        req.setAttribute("activeNav", "payments");
        req.getRequestDispatcher("/WEB-INF/views/staff/paymentHistory.jsp").forward(req, resp);
    }

    /** Giá trị bộ lọc hợp lệ, null = không lọc (Set.of không nhận contains(null)). */
    private static String allowed(Set<String> values, String value) {
        return value != null && values.contains(value) ? value : null;
    }

    /** null = mọi chi nhánh. Nhân viên bán vé chưa được gắn chi nhánh thì không thấy giao dịch nào (-1). */
    private static Integer centreScope(User staff) {
        if (!WebUtil.isSalesStaff(staff)) return null;
        return staff.getCentreId() != null ? staff.getCentreId() : -1;
    }
}
