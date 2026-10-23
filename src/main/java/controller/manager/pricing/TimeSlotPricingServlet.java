package controller.manager.pricing;

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

        int centreId = service.ShowtimeService.requireCentreId(request);

        TimeSlotPricingDAO dao =
                new TimeSlotPricingDAO();

        request.setAttribute(
                "rules",
                dao.getCurrentRules(centreId)
        );

        request.setAttribute(
                "history",
                dao.getHistory(centreId)
        );

        request.setAttribute(
                "centreId",
                centreId
        );

        request.getRequestDispatcher(
                "/WEB-INF/views/manager/pricing/timeSlotPricing.jsp"
        ).forward(
                request,
                response
        );
    }
}