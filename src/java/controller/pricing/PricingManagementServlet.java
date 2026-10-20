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
import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import model.BranchPricing;

@WebServlet("/pricing")
public class PricingManagementServlet
        extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession();



        int centreId = utils.FE04Context.requireCentreId(request);

        BranchPricingDAO dao =
                new BranchPricingDAO();

        BranchPricing currentPrice =
                dao.getCurrentPrice(centreId);

        List<BranchPricing> history =
                dao.getPriceHistory(centreId);

        request.setAttribute(
                "currentPrice",
                currentPrice
        );

        request.setAttribute(
                "history",
                history
        );

        request.setAttribute(
                "centreId",
                centreId
        );

        request.getRequestDispatcher(
                "/pricing/pricing-management.jsp"
        ).forward(
                request,
                response
        );
    }
}
