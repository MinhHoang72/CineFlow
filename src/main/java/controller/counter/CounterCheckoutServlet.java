package controller.counter;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Controller: Thanh toán vé tại quầy (Counter Checkout).
 * URL ánh xạ: /staff/counter/checkout
 */
@WebServlet(name = "CounterCheckoutServlet", urlPatterns = {"/staff/counter/checkout"})
public class CounterCheckoutServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String bookingId = request.getParameter("bookingId");
        if (bookingId != null && !bookingId.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/staff/counter?action=checkout&bookingId=" + bookingId);
        } else {
            response.sendRedirect(request.getContextPath() + "/staff/counter?action=checkout");
        }
    }
}
