package controller.voucher;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Controller: Điều hướng tới Form tạo / chỉnh sửa Voucher khuyến mãi.
 * URL ánh xạ: /admin/voucher-form
 */
@WebServlet(name = "VoucherFormServlet", urlPatterns = {"/admin/voucher-form"})
public class VoucherFormServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String id = request.getParameter("id");
        if (id != null && !id.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/admin/vouchers/form?id=" + id);
        } else {
            response.sendRedirect(request.getContextPath() + "/admin/vouchers/form");
        }
    }
}
