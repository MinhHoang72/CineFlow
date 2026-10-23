package controller.manager.pricing.timeslot;

import dao.TimeSlotPricingDAO;
import java.io.IOException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/pricing/time-slots/delete")
public class DeleteTimeSlotPricingServlet
        extends HttpServlet {

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        try {

            int id = Integer.parseInt(
                    request.getParameter("id")
            );

            TimeSlotPricingDAO dao =
                    new TimeSlotPricingDAO();

            boolean success =
                    dao.deleteRule(id);

            if (success) {
                request.getSession()
                        .setAttribute(
                                "message",
                                "Pricing rule removed successfully."
                        );
            } else {
                request.getSession()
                        .setAttribute(
                                "error",
                                "Unable to remove pricing rule."
                        );
            }

        } catch (Exception e) {

            e.printStackTrace();

            request.getSession()
                    .setAttribute(
                            "error",
                            "Invalid pricing rule."
                    );
        }

        response.sendRedirect(
                request.getContextPath()
                + "/pricing/time-slots"
        );
    }
}