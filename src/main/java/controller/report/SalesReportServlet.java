package controller.report;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Controller: Báo cáo doanh thu bán vé & F&B chi tiết (Sales Report).
 * URL ánh xạ: /admin/reports/sales, /report/sales-direct
 */
@WebServlet(name = "SalesReportServlet", urlPatterns = {"/admin/reports/sales", "/report/sales-direct"})
public class SalesReportServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/report/sales");
    }
}
