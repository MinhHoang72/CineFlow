package controller.manager.pricing;

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

        if (Boolean.TRUE.equals(request.getSession().getAttribute("reopenSurcharge")))
            request.setAttribute("surchargeFormError", request.getSession().getAttribute("error"));
        request.setAttribute("currentRules", dao.getCurrentRules());
        request.setAttribute("history", dao.getHistory());
        try {
            request.setAttribute("seatTargets", dao.getConfiguredTargets("SEAT"));
            request.setAttribute("hallTargets", dao.getConfiguredTargets("HALL"));
        } catch (java.sql.SQLException e) {
            throw new ServletException("Không thể tải danh sách loại ghế / phòng.", e);
        }

        request.getRequestDispatcher("/WEB-INF/views/manager/pricing/seatSurcharge.jsp").forward(request, response);
    }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        try {
            int userId = service.ShowtimeService.requireUserId(request);
            String type = request.getParameter("surchargeType");
            String target = request.getParameter("targetValue");
            String name = request.getParameter("surchargeName");
            target = target == null ? "" : target.trim().toUpperCase(java.util.Locale.ROOT);
            name = name == null ? "" : name.trim();
            if (!("SEAT".equals(type) || "HALL".equals(type)))
                throw new IllegalArgumentException("Chọn loại phụ thu ghế hoặc phòng.");
            if (!new SurchargeDAO().getConfiguredTargets(type).contains(target))
                throw new IllegalArgumentException("Vui lòng chọn loại ghế / phòng đang được cấu hình trong hệ thống.");
            if (name.isEmpty() || name.length() > 100)
                throw new IllegalArgumentException("Tên phụ thu phải có từ 1 đến 100 ký tự.");
            model.entity.Surcharge rule = new model.entity.Surcharge();
            rule.setSurchargeType(type); rule.setTargetValue(target); rule.setSurchargeName(name);
            rule.setAmount(service.PricingService.parseVnd(request.getParameter("amount")));
            rule.setUpdatedBy(userId);
            if (!new SurchargeDAO().createSurcharge(rule))
                throw new IllegalArgumentException("Đối tượng này đã có phụ thu đang áp dụng. Hãy sửa cấu hình hiện có.");
            request.getSession().setAttribute("message", "Đã thêm phụ thu ghế / phòng.");
        } catch (Exception e) {
            request.getSession().setAttribute("error", e instanceof IllegalArgumentException
                    ? e.getMessage() : "Không thể thêm phụ thu. Vui lòng thử lại.");
            request.getSession().setAttribute("reopenSurcharge", true);
            java.util.Map<String,String> draft = new java.util.HashMap<>();
            for (String key : new String[]{"surchargeType","targetValue","surchargeName","amount"})
                draft.put(key, request.getParameter(key));
            request.getSession().setAttribute("surchargeDraft", draft);
        }
        response.sendRedirect(request.getContextPath() + "/pricing/seat-surcharges");
    }
}
