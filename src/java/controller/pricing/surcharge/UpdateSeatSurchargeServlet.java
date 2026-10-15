package controller.pricing.surcharge;

import dao.SurchargeDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import model.Surcharge;

@WebServlet("/pricing/seat-surcharges/update")
public class UpdateSeatSurchargeServlet
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

            String surchargeName =
                    request.getParameter(
                            "surchargeName"
                    );

            String seatType =
                    request.getParameter(
                            "seatType"
                    );

            double amount =
                    Double.parseDouble(
                            request.getParameter(
                                    "amount"
                            )
                    );

            int managerId = 1;

            if (amount < 0) {

                request.getSession()
                        .setAttribute(
                                "error",
                                "Surcharge cannot be negative."
                        );

                redirect(request, response);

                return;
            }

            Surcharge newRule =
                    new Surcharge();

            newRule.setSurchargeName(
                    surchargeName
            );

            newRule.setSeatType(
                    seatType
            );

            newRule.setAmount(
                    amount
            );

            newRule.setUpdatedBy(
                    managerId
            );

            SurchargeDAO dao =
                    new SurchargeDAO();

            boolean success =
                    dao.updateSurcharge(
                            id,
                            newRule
                    );

            if (success) {

                request.getSession()
                        .setAttribute(
                                "message",
                                "Seat surcharge updated successfully."
                        );

            } else {

                request.getSession()
                        .setAttribute(
                                "error",
                                "Unable to update seat surcharge."
                        );
            }

        } catch (Exception e) {

            e.printStackTrace();

            request.getSession()
                    .setAttribute(
                            "error",
                            "Invalid surcharge information."
                    );
        }

        redirect(request, response);
    }

    private void redirect(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        response.sendRedirect(
                request.getContextPath()
                + "/pricing/seat-surcharges"
        );
    }
}