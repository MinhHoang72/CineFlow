package controller.pricing.timeslot;

import dao.TimeSlotPricingDAO;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import model.TimeSlotPricing;

@WebServlet("/pricing/time-slots/edit")
public class EditTimeSlotPricingServlet
        extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        try {

            int id =
                Integer.parseInt(
                    request.getParameter("id")
                );

            TimeSlotPricingDAO dao =
                    new TimeSlotPricingDAO();

            TimeSlotPricing rule =
                    dao.getById(id);

            if (rule == null) {

                response.sendRedirect(
                    request.getContextPath()
                    + "/pricing/time-slots"
                );

                return;
            }

            request.setAttribute(
                    "rule",
                    rule
            );

            request.getRequestDispatcher(
                    "/pricing/edit-time-slot-pricing.jsp"
            ).forward(
                    request,
                    response
            );

        } catch (NumberFormatException e) {

            response.sendRedirect(
                request.getContextPath()
                + "/pricing/time-slots"
            );
        }
    }
}