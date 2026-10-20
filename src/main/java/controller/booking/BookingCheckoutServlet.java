package controller.booking;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Controller: Thanh toán rà soát đơn hàng vé (Booking Checkout).
 * URL ánh xạ: /booking/checkout
 */
@WebServlet(name = "BookingCheckoutServlet", urlPatterns = {"/booking/checkout"})
public class BookingCheckoutServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String bookingId = request.getParameter("bookingId");
        if (bookingId != null && !bookingId.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/booking?action=checkout&bookingId=" + bookingId);
        } else {
            response.sendRedirect(request.getContextPath() + "/booking?action=checkout");
        }
    }
}
