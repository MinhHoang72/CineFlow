package controller.pricing;

import dao.BranchPricingDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.entity.BranchPricing;
import service.ShowtimeService;

import java.io.IOException;
import java.util.List;

/**
 * Controller: Quản lý giá vé cơ bản của từng chi nhánh rạp chiếu theo tiêu chuẩn CINEFLOW_ARCHITECTURE_STANDARD.
 * URL ánh xạ:
 * - GET  /pricing: Hiển thị giá cơ bản hiện tại và lịch sử điều chỉnh
 * - POST /pricing/update: Cập nhật mức giá vé cơ bản mới
 */
@WebServlet(name = "BranchPricingServlet", urlPatterns = {"/pricing", "/pricing/update"})
public class BranchPricingServlet extends HttpServlet {

    private final BranchPricingDAO dao = new BranchPricingDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int centreId = ShowtimeService.requireCentreId(request);

        BranchPricing currentPrice = dao.getCurrentPrice(centreId);
        List<BranchPricing> history = dao.getPriceHistory(centreId);

        request.setAttribute("currentPrice", currentPrice);
        request.setAttribute("history", history);
        request.setAttribute("centreId", centreId);

        request.getRequestDispatcher("/WEB-INF/views/pricing/pricingOverview.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            int centreId = ShowtimeService.requireCentreId(request);
            double newPrice = Double.parseDouble(request.getParameter("basePrice"));
            int managerId = ShowtimeService.requireUserId(request);

            if (newPrice < 0) {
                request.getSession().setAttribute("error", "Giá cơ bản không được âm.");
                response.sendRedirect(request.getContextPath() + "/pricing");
                return;
            }

            BranchPricing current = dao.getCurrentPrice(centreId);
            if (current != null && current.getBasePrice() == newPrice) {
                request.getSession().setAttribute("error", "Mức giá mới phải khác giá đang áp dụng.");
                response.sendRedirect(request.getContextPath() + "/pricing");
                return;
            }

            boolean success = dao.updateBasePrice(centreId, newPrice, managerId);
            if (success) {
                request.getSession().setAttribute("message", "Cập nhật giá vé cơ bản thành công.");
            } else {
                request.getSession().setAttribute("error", "Không thể cập nhật giá vé cơ bản.");
            }

        } catch (NumberFormatException e) {
            request.getSession().setAttribute("error", "Định dạng giá vé không hợp lệ.");
        } catch (Exception e) {
            request.getSession().setAttribute("error", "Lỗi: " + e.getMessage());
        }

        response.sendRedirect(request.getContextPath() + "/pricing");
    }
}
