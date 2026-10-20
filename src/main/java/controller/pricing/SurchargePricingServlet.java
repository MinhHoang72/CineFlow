package controller.pricing;

import dao.SurchargeDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Controller: Quản lý chính sách phụ thu tiền hạng ghế VIP/Sweetbox và loại phòng chiếu theo chuẩn CINEFLOW_ARCHITECTURE_STANDARD.
 * URL ánh xạ: /pricing/seat-surcharges
 */
@WebServlet(name = "SurchargePricingServlet", urlPatterns = {"/pricing/seat-surcharges"})
public class SurchargePricingServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        SurchargeDAO dao = new SurchargeDAO();

        request.setAttribute("currentRules", dao.getCurrentRules());
        request.setAttribute("history", dao.getHistory());

        request.getRequestDispatcher("/WEB-INF/views/pricing/seatSurcharge.jsp").forward(request, response);
    }
}
