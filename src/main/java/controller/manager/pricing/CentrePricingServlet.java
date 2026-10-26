package controller.manager.pricing;

import dao.CentrePricingDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.entity.CentrePricing;
import service.ShowtimeService;

import java.io.IOException;
import java.util.List;

/**
 * Controller: Quản lý giá vé cơ bản của từng chi nhánh rạp chiếu theo tiêu chuẩn CINEFLOW_ARCHITECTURE_STANDARD.
 * URL ánh xạ:
 * - GET  /pricing: Hiển thị giá cơ bản hiện tại và lịch sử điều chỉnh
 * - POST /pricing/update: Cập nhật mức giá vé cơ bản mới
 */
@WebServlet(name = "CentrePricingServlet", urlPatterns = {"/pricing", "/pricing/update"})
public class CentrePricingServlet extends HttpServlet {

    private final CentrePricingDAO dao = new CentrePricingDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int centreId = ShowtimeService.requireCentreId(request);

        CentrePricing currentPrice = dao.getCurrentPrice(centreId);
        List<CentrePricing> history = dao.getPriceHistory(centreId);

        request.setAttribute("currentPrice", currentPrice);
        request.setAttribute("history", history);
        request.setAttribute("centreId", centreId);
        model.entity.Centre centre = new dao.CentreDAO().getCentreById(centreId);
        request.setAttribute("centreName", centre == null ? "Chưa xác định cụm rạp" : centre.getCentreName());

        request.getRequestDispatcher("/WEB-INF/views/manager/pricing/pricingOverview.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            int centreId = ShowtimeService.requireCentreId(request);
            double newPrice = service.PricingService.parseVnd(request.getParameter("basePrice"));
            int managerId = ShowtimeService.requireUserId(request);

            if (newPrice < 0) {
                request.getSession().setAttribute("error", "Giá cơ bản không được âm.");
                response.sendRedirect(request.getContextPath() + "/pricing");
                return;
            }

            String mode = request.getParameter("effectiveMode");
            if (mode != null && !"now".equals(mode) && !"scheduled".equals(mode))
                throw new IllegalArgumentException("Thời điểm áp dụng không hợp lệ.");
            if ("scheduled".equals(mode) && (request.getParameter("effectiveFrom") == null
                    || request.getParameter("effectiveFrom").isBlank()))
                throw new IllegalArgumentException("Chọn ngày và giờ bắt đầu áp dụng.");
            java.time.LocalDateTime effectiveFrom = "scheduled".equals(mode)
                    ? java.time.LocalDateTime.parse(request.getParameter("effectiveFrom")) : null;
            CentrePricing current = dao.getCurrentPrice(centreId);
            if (effectiveFrom == null && current != null && current.getBasePrice() == newPrice) {
                request.getSession().setAttribute("error", "Mức giá mới phải khác giá đang áp dụng.");
                response.sendRedirect(request.getContextPath() + "/pricing");
                return;
            }

            boolean success = dao.updateBasePrice(centreId, newPrice, managerId, effectiveFrom);
            if (success) {
                request.getSession().setAttribute("message", effectiveFrom == null ? "Cập nhật giá vé cơ bản thành công." : "Đã lên lịch áp dụng giá cơ bản mới.");
            } else {
                request.getSession().setAttribute("error", "Không thể cập nhật giá vé cơ bản.");
            }

        } catch (NumberFormatException e) {
            request.getSession().setAttribute("error", "Định dạng giá vé không hợp lệ.");
        } catch (IllegalArgumentException | java.time.DateTimeException e) {
            request.getSession().setAttribute("error", "Dữ liệu không hợp lệ: " + e.getMessage());
        } catch (Exception e) {
            request.getSession().setAttribute("error", "Lỗi: " + e.getMessage());
        }

        response.sendRedirect(request.getContextPath() + "/pricing");
    }
}
