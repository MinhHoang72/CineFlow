/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package controller.pricing;

/**
 *
 * @author vktuy
 */

import dao.BranchPricingDAO;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/pricing/update")
public class UpdateBasePriceServlet
        extends HttpServlet {

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        try {

            int centreId = service.ShowtimeService.requireCentreId(request);

            double newPrice =
                    Double.parseDouble(
                        request.getParameter(
                            "basePrice"
                        )
                    );



            int managerId = service.ShowtimeService.requireUserId(request);

            if (newPrice < 0) {

                request.getSession()
                        .setAttribute(
                            "error",
                            "Base price must be greater than or equal to 0."
                        );

                response.sendRedirect(
                    request.getContextPath()
                    + "/pricing"
                );

                return;
            }

            BranchPricingDAO dao =
                    new BranchPricingDAO();

            boolean success =
                    dao.updateBasePrice(
                        centreId,
                        newPrice,
                        managerId
                    );

            if (success) {

                request.getSession()
                        .setAttribute(
                            "message",
                            "Base price updated successfully."
                        );

            } else {

                request.getSession()
                        .setAttribute(
                            "error",
                            "Unable to update base price."
                        );
            }

        } catch (NumberFormatException e) {

            request.getSession()
                    .setAttribute(
                        "error",
                        "Invalid price."
                    );
        }

        response.sendRedirect(
            request.getContextPath()
            + "/pricing"
        );
    }
}
