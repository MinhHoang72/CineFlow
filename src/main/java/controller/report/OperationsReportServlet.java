package controller.report;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Controller: Báo cáo vận hành & công suất phòng chiếu (Operations Report).
 * URL ánh xạ: /admin/reports/operations, /report/operations-direct
 */
@WebServlet(name = "OperationsReportServlet", urlPatterns = {"/admin/reports/operations", "/report/operations-direct"})
public class OperationsReportServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/report/operations");
    }
}
