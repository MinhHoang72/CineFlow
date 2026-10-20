package controller.payment;

import dao.PaymentDAO;
import model.Payment;
import model.User;
import utils.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/**
 * FE-07 Theo doi giao dich thanh toan (chi xem; VNPay tu xac nhan, tien mat xac nhan tai quay):
 *   GET /staff/payments?status=&method=&days=
 * Nhan vien ban ve chi thay chi nhanh cua minh; MANAGER/ADMIN thay tat ca.
 */
@WebServlet(name = "StaffPaymentController", urlPatterns = {"/staff/payments"})
public class StaffPaymentController extends HttpServlet {

    private static final Set<String> STATUSES = Set.of("PENDING", "PAID", "FAILED", "TIMEOUT", "CANCELLED");
    private static final Set<String> METHODS = Set.of("QR", "CASH");
    private static final Set<Integer> DAY_OPTIONS = Set.of(1, 7, 30);

    private final PaymentDAO paymentDAO = new PaymentDAO();

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

        List<Payment> payments;
        try {
            payments = paymentDAO.getTransactionsForTracking(branchScope(staff), status, method, days);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        BigDecimal paidTotal = BigDecimal.ZERO;
        int paidCount = 0;
        for (Payment p : payments) {
            if ("PAID".equals(p.getStatus())) {
                paidTotal = paidTotal.add(p.getAmount());
                paidCount++;
            }
        }
        req.setAttribute("payments", payments);
        req.setAttribute("paidTotal", paidTotal);
        req.setAttribute("paidCount", paidCount);
        req.setAttribute("status", status);
        req.setAttribute("method", method);
        req.setAttribute("days", days);
        req.getRequestDispatcher("/WEB-INF/views/payment/transactions.jsp").forward(req, resp);
    }

    /** Gia tri bo loc hop le, null = khong loc (Set.of khong nhan contains(null)). */
    private static String allowed(Set<String> values, String value) {
        return value != null && values.contains(value) ? value : null;
    }

    /** null = moi chi nhanh. Nhan vien ban ve chua duoc gan chi nhanh thi khong thay giao dich nao (-1). */
    private static Integer branchScope(User staff) {
        if (!WebUtil.isSalesStaff(staff)) return null;
        return staff.getCentreId() != null ? staff.getCentreId() : -1;
    }
}
