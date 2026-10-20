package controller.pricing.surcharge;

import dao.SurchargeDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/pricing/seat-surcharges")
public class SeatSurchargeServlet
        extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        SurchargeDAO dao =
                new SurchargeDAO();

        request.setAttribute(
                "currentRules",
                dao.getCurrentRules()
        );

        request.setAttribute(
                "history",
                dao.getHistory()
        );

        request.getRequestDispatcher(
                "/pricing/seat-surcharge.jsp"
        ).forward(
                request,
                response
        );
    }
}