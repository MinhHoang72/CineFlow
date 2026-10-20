package controller.counter;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Controller: In vé xem phim tại quầy (Counter Ticket Print).
 * URL ánh xạ: /staff/counter/ticket
 */
@WebServlet(name = "CounterTicketServlet", urlPatterns = {"/staff/counter/ticket"})
public class CounterTicketServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String bookingId = request.getParameter("bookingId");
        if (bookingId != null && !bookingId.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/staff/counter?action=ticket&bookingId=" + bookingId);
        } else {
            response.sendRedirect(request.getContextPath() + "/staff/counter");
        }
    }
}
