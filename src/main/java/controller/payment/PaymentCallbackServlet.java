package controller.payment;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Controller: Nhận phản hồi thanh toán từ cổng VNPay (Callback / IPN).
 * URL ánh xạ: /payment/callback
 */
@WebServlet(name = "PaymentCallbackServlet", urlPatterns = {"/payment/callback"})
public class PaymentCallbackServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Chuyển tiếp tới trình xử lý vnpay-return chính
        request.getRequestDispatcher("/payment/vnpay-return").forward(request, response);
    }
}
