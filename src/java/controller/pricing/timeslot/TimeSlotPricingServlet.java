package controller.pricing.timeslot;

import dao.TimeSlotPricingDAO;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/pricing/time-slots")
public class TimeSlotPricingServlet
        extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        /*
         * Demo trước:
         * BranchID = 1
         */

        int branchId = 1;

        TimeSlotPricingDAO dao =
                new TimeSlotPricingDAO();

        request.setAttribute(
                "rules",
                dao.getCurrentRules(branchId)
        );

        request.setAttribute(
                "history",
                dao.getHistory(branchId)
        );

        request.setAttribute(
                "branchId",
                branchId
        );

        request.getRequestDispatcher(
                "/time-slot-pricing.jsp"
        ).forward(
                request,
                response
        );
    }
}