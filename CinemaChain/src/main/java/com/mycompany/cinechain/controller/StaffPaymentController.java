package com.mycompany.cinechain.controller;

import com.mycompany.cinechain.dao.PaymentDAO;
import com.mycompany.cinechain.dao.PaymentDAO.Outcome;
import com.mycompany.cinechain.model.User;
import com.mycompany.cinechain.utils.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * FE-07 Nhan vien doi soat chuyen khoan:
 *   GET  /staff/payments   danh sach giao dich chuyen khoan chua xac nhan
 *   POST /staff/payments   xac nhan da nhan tien -> xuat ve
 * STAFF chi thay chi nhanh cua minh; MANAGER/ADMIN thay tat ca.
 */
@WebServlet(name = "StaffPaymentController", urlPatterns = {"/staff/payments"})
public class StaffPaymentController extends HttpServlet {

    private final PaymentDAO paymentDAO = new PaymentDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User staff = requireStaff(req, resp);
        if (staff == null) return;
        try {
            req.setAttribute("payments", paymentDAO.getUnconfirmedQr(branchScope(staff)));
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        req.getRequestDispatcher("/WEB-INF/views/staff-payments.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User staff = requireStaff(req, resp);
        if (staff == null) return;

        Integer paymentId = WebUtil.parseInt(req.getParameter("paymentId"));
        if (paymentId == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        String code = req.getParameter("bookingCode");
        Outcome outcome;
        try {
            if ("reject".equals(req.getParameter("action"))) {
                if (paymentDAO.rejectQr(paymentId, branchScope(staff))) {
                    WebUtil.flash(req, "warning", "Đã đánh dấu đơn " + code + " là không nhận được tiền. "
                            + "Khách sẽ được báo thanh toán thất bại và có thể thanh toán lại.");
                } else {
                    WebUtil.flash(req, "danger", "Giao dịch không còn ở trạng thái chờ hoặc không thuộc rạp của bạn.");
                }
                resp.sendRedirect(req.getContextPath() + "/staff/payments");
                return;
            }
            outcome = paymentDAO.confirmPaid(paymentId, branchScope(staff));
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        switch (outcome.status()) {
            case SUCCESS -> WebUtil.flash(req, "success", "Đã xác nhận đơn " + code + ", vé đã được xuất.");
            case ALREADY_PAID -> WebUtil.flash(req, "info", "Đơn " + code + " đã được xác nhận trước đó.");
            case PAID_BUT_SEATS_LOST -> WebUtil.flash(req, "danger", "Đơn " + code + " đã hết hạn giữ ghế và ghế đã "
                    + "được người khác đặt. Đã ghi nhận tiền, cần hoàn tiền cho khách.");
            case NOT_FOUND -> WebUtil.flash(req, "danger", "Không tìm thấy giao dịch hoặc giao dịch không thuộc rạp của bạn.");
        }
        resp.sendRedirect(req.getContextPath() + "/staff/payments");
    }

    /** null = moi chi nhanh. STAFF chua duoc gan chi nhanh thi khong thay giao dich nao (-1). */
    private static Integer branchScope(User staff) {
        if (!"STAFF".equals(staff.getRole())) return null;
        return staff.getBranchId() != null ? staff.getBranchId() : -1;
    }

    private static User requireStaff(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = WebUtil.requireUser(req, resp);
        if (user == null) return null;
        if (!user.isStaff()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        return user;
    }
}
