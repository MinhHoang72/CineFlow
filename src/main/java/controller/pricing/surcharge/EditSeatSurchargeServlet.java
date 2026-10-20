package controller.pricing.surcharge;

import dao.SurchargeDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import model.entity.Surcharge;

@WebServlet("/pricing/seat-surcharges/edit")
public class EditSeatSurchargeServlet
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

            SurchargeDAO dao =
                    new SurchargeDAO();

            Surcharge rule =
                    dao.getById(id);

            if (rule == null) {

                response.sendRedirect(
                        request.getContextPath()
                        + "/pricing/seat-surcharges"
                );

                return;
            }

            request.setAttribute(
                    "rule",
                    rule
            );

            request.getRequestDispatcher(
                    "/WEB-INF/views/pricing/surchargeEdit.jsp"
            ).forward(
                    request,
                    response
            );

        } catch (Exception e) {

            response.sendRedirect(
                    request.getContextPath()
                    + "/pricing/seat-surcharges"
            );
        }
    }
}