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

        boolean admin="ADMIN".equals(request.getSession().getAttribute("role"));
        if(admin && !"true".equals(request.getParameter("edit"))){
            try{request.setAttribute("centrePrices",dao.getCentrePriceOverview());}
            catch(java.sql.SQLException e){throw new ServletException("Không thể tải bảng giá các rạp.",e);}
            request.setAttribute("hideCinemaCentreSelector",true);
            request.getRequestDispatcher("/WEB-INF/views/manager/pricing/centrePricingList.jsp").forward(request,response);
            return;
        }
        int centreId;
        if(admin){
            try{centreId=Integer.parseInt(request.getParameter("centreId"));}
            catch(NumberFormatException e){response.sendError(400,"Mã rạp không hợp lệ.");return;}
            if(new dao.CentreDAO().getCentreById(centreId)==null){response.sendError(404,"Không tìm thấy rạp.");return;}
            request.setAttribute("hideCinemaCentreSelector",true);
        }else centreId=ShowtimeService.requireCentreId(request);

        CentrePricing currentPrice = dao.getCurrentPrice(centreId);
        List<CentrePricing> history = dao.getPriceHistory(centreId);

        request.setAttribute("currentPrice", currentPrice);
        request.setAttribute("history", history);
        request.setAttribute("nextPrice",history.stream().filter(CentrePricing::isScheduled)
                .min(java.util.Comparator.comparing(CentrePricing::getEffectiveFrom)).orElse(null));
        request.setAttribute("centreId", centreId);
        model.entity.Centre centre = new dao.CentreDAO().getCentreById(centreId);
        request.setAttribute("centreName", centre == null ? "Chưa xác định cụm rạp" : centre.getCentreName());

        request.getRequestDispatcher("/WEB-INF/views/manager/pricing/pricingOverview.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if(!"ADMIN".equals(request.getSession().getAttribute("role"))){response.sendError(403);return;}
        int centreId;
        try{centreId=Integer.parseInt(request.getParameter("centreId"));}
        catch(NumberFormatException e){response.sendError(400,"Mã rạp không hợp lệ.");return;}
        if(new dao.CentreDAO().getCentreById(centreId)==null){response.sendError(404,"Không tìm thấy rạp.");return;}
        String editUrl=request.getContextPath()+"/pricing?centreId="+centreId+"&edit=true";
        boolean saved=false;
        try {
            double newPrice = service.PricingService.parseVnd(request.getParameter("basePrice"));
            int managerId = ShowtimeService.requireUserId(request);

            if (newPrice < 0) {
                request.getSession().setAttribute("error", "Giá cơ bản không được âm.");
                response.sendRedirect(editUrl);
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
                response.sendRedirect(editUrl);
                return;
            }

            boolean success = dao.updateBasePrice(centreId, newPrice, managerId, effectiveFrom);
            if (success) {
                saved=true;
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

        response.sendRedirect(saved?request.getContextPath()+"/pricing":editUrl);
    }
}
