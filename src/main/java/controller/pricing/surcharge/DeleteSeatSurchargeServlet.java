package controller.pricing.surcharge;

import dao.SurchargeDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/pricing/seat-surcharges/delete")
public class DeleteSeatSurchargeServlet
        extends HttpServlet {

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        try {

            int id =
                    Integer.parseInt(
                            request.getParameter("id")
                    );

            SurchargeDAO dao =
                    new SurchargeDAO();

            boolean success =
                    dao.deleteRule(id);

            if (success) {

                request.getSession()
                        .setAttribute(
                                "message",
                                "Seat surcharge removed successfully."
                        );

            } else {

                request.getSession()
                        .setAttribute(
                                "error",
                                "Unable to remove seat surcharge."
                        );
            }

        } catch (Exception e) {

            e.printStackTrace();

            request.getSession()
                    .setAttribute(
                            "error",
                            "Invalid surcharge."
                    );
        }

        response.sendRedirect(
                request.getContextPath()
                + "/pricing/seat-surcharges"
        );
    }
}