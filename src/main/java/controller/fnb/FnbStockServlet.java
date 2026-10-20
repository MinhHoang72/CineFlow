package controller.fnb;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Controller: Quản lý kho hàng & số lượng tồn bắp nước.
 * URL ánh xạ: /manager/fnb/stock, /fnb/stock-management
 */
@WebServlet(name = "FnbStockServlet", urlPatterns = {"/manager/fnb/stock", "/fnb/stock-management"})
public class FnbStockServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/fnb/stock");
    }
}
