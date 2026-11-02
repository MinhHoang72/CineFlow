package controller.admin.cinema;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.dto.common.PageDTO;
import model.entity.Centre;
import model.entity.Hall;
import model.entity.User;
import service.CinemaService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controller: Quản lý Cụm rạp (Centres).
 * Toàn bộ logic nghiệp vụ được ủy quyền cho CinemaService (Mô hình 3 lớp).
 */
@WebServlet(name = "CentreManagementServlet", urlPatterns = {"/admin/centres", "/admin/centre"})
public class CentreManagementServlet extends HttpServlet {

    private static final int PAGE_SIZE = 6;
    private final CinemaService cinemaService = new CinemaService();
    private final Gson gson = new Gson();

    private boolean isAdmin(HttpSession session) {
        if (session == null) return false;
        Object role = session.getAttribute("role");
        if (role == null && session.getAttribute("user") instanceof User) {
            role = ((User) session.getAttribute("user")).getRole();
        }
        return "ADMIN".equalsIgnoreCase((String) role);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect(request.getContextPath() + "/login?redirect="
                    + URLEncoder.encode("/admin/centres", StandardCharsets.UTF_8));
            return;
        }
        if (!isAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Chỉ Quản trị viên (ADMIN) mới có quyền truy cập trang Quản lý Cụm rạp.");
            return;
        }

        // AJAX: lấy danh sách Hall của một Centre (BR-HS-01)
        String action = request.getParameter("action");
        if ("getHalls".equalsIgnoreCase(action)) {
            response.setContentType("application/json;charset=UTF-8");
            try {
                int centreId = Integer.parseInt(request.getParameter("centreId").trim());
                List<Hall> halls = cinemaService.getHallsByCentreId(centreId);
                response.getWriter().write(gson.toJson(halls));
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("[]");
            }
            return;
        }

        // Đọc tham số tìm kiếm và phân trang
        String search = request.getParameter("search");
        String status = request.getParameter("status");
        if (status == null || status.trim().isEmpty()) status = "ALL";

        int page = 1;
        try {
            String p = request.getParameter("page");
            if (p != null && !p.trim().isEmpty()) page = Integer.parseInt(p.trim());
        } catch (NumberFormatException ignored) { }

        PageDTO<Centre> pageResult = cinemaService.searchCentres(search, status, page, PAGE_SIZE);
        CinemaService.CentreKpi kpi = cinemaService.getCentresKpi();

        String base = request.getContextPath() + "/admin/centres";
        String filters = "search=" + URLEncoder.encode(search != null ? search.trim() : "", StandardCharsets.UTF_8)
                + "&status=" + URLEncoder.encode(status, StandardCharsets.UTF_8);

        request.setAttribute("result", pageResult);
        request.setAttribute("page", pageResult);
        request.setAttribute("centres", pageResult.getItems());
        request.setAttribute("totalCentres", kpi.totalCentres());
        request.setAttribute("activeCentres", kpi.activeCentres());
        request.setAttribute("inactiveCentres", kpi.inactiveCentres());
        request.setAttribute("totalHalls", kpi.totalHalls());
        request.setAttribute("currentSearch", search != null ? search.trim() : "");
        request.setAttribute("currentStatus", status);
        request.setAttribute("pagerBase", base + "?" + filters + "&page=");
        request.setAttribute("backQuery", filters + "&page=" + pageResult.getPage());
        request.setAttribute("baseUrl", base);
        request.setAttribute("activeNav", "centres");
        request.setAttribute("pageTitle", "Quản lý Cụm rạp");

        request.getRequestDispatcher("/WEB-INF/views/admin/cinema/centreList.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        if (!isAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền thực hiện thao tác này.");
            return;
        }

        String action    = request.getParameter("action");
        String search    = request.getParameter("search");
        String status    = request.getParameter("status");
        String pageParam = request.getParameter("page");
        String back      = request.getParameter("back");

        StringBuilder redirectUrl;
        if (back != null && !back.trim().isEmpty()) {
            redirectUrl = new StringBuilder(request.getContextPath() + "/admin/centres?" + back.replaceAll("[^A-Za-z0-9%&=._~+-]", "") + "&");
        } else {
            redirectUrl = new StringBuilder(request.getContextPath() + "/admin/centres?");
            if (search != null && !search.trim().isEmpty())
                redirectUrl.append("search=").append(URLEncoder.encode(search.trim(), StandardCharsets.UTF_8)).append("&");
            if (status != null && !status.trim().isEmpty())
                redirectUrl.append("status=").append(URLEncoder.encode(status.trim(), StandardCharsets.UTF_8)).append("&");
            if (pageParam != null && !pageParam.trim().isEmpty())
                redirectUrl.append("page=").append(URLEncoder.encode(pageParam.trim(), StandardCharsets.UTF_8)).append("&");
        }

        // Thêm Centre mới
        if ("create".equalsIgnoreCase(action)) {
            String centreName = request.getParameter("centreName");
            String address    = request.getParameter("address");
            String phone      = request.getParameter("phone");
            String city       = request.getParameter("city");

            CinemaService.Result<Centre> result = cinemaService.createCentre(centreName, address, phone, city);
            if (result.isSuccess()) {
                redirectUrl.append("success=created");
            } else {
                String err = (result.getStatus() == CinemaService.Status.DUPLICATE_NAME) ? "duplicate_name"
                           : (result.getStatus() == CinemaService.Status.MISSING_FIELDS) ? "missing_fields" : "failed";
                redirectUrl.append("error=").append(err);
            }
            response.sendRedirect(redirectUrl.toString());
            return;
        }

        // Sửa thông tin Centre
        if ("update".equalsIgnoreCase(action)) {
            String centreIdParam = request.getParameter("centreId");
            String centreName    = request.getParameter("centreName");
            String address       = request.getParameter("address");
            String phone         = request.getParameter("phone");
            String city          = request.getParameter("city");

            int centreId;
            try {
                centreId = Integer.parseInt(centreIdParam.trim());
            } catch (Exception e) {
                redirectUrl.append("error=invalid_id");
                response.sendRedirect(redirectUrl.toString());
                return;
            }

            CinemaService.Result<Centre> result = cinemaService.updateCentre(centreId, centreName, address, phone, city);
            if (result.isSuccess()) {
                redirectUrl.append("success=updated");
            } else {
                String err = (result.getStatus() == CinemaService.Status.DUPLICATE_NAME) ? "duplicate_name"
                           : (result.getStatus() == CinemaService.Status.MISSING_FIELDS) ? "missing_fields" : "failed";
                redirectUrl.append("error=").append(err);
            }
            response.sendRedirect(redirectUrl.toString());
            return;
        }

        // Toggle Status
        if ("toggleStatus".equalsIgnoreCase(action)) {
            try {
                int centreId = Integer.parseInt(request.getParameter("centreId").trim());
                boolean ok = cinemaService.toggleCentreStatus(centreId);
                redirectUrl.append(ok ? "success=status_updated" : "error=failed");
            } catch (Exception e) {
                redirectUrl.append("error=invalid_id");
            }
            response.sendRedirect(redirectUrl.toString());
            return;
        }

        response.sendRedirect(redirectUrl.toString());
    }
}
