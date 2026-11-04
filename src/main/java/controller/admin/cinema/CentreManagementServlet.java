package controller.admin.cinema;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.dto.common.PageDTO;
import model.entity.Centre;
import model.entity.User;
import service.CinemaService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Controller: Quản lý Cụm rạp (Centres). Toàn bộ logic nghiệp vụ được ủy quyền
 * cho CinemaService (Mô hình 3 lớp).
 */
@WebServlet(name = "CentreManagementServlet", urlPatterns = {"/admin/centres", "/admin/centre"})
public class CentreManagementServlet extends HttpServlet {

    private static final int PAGE_SIZE = 10;
    private final CinemaService cinemaService = new CinemaService();

    private boolean isAdmin(HttpSession session) {
        if (session == null) {
            return false;
        }
        Object role = session.getAttribute("role");
        if (role == null && session.getAttribute("user") instanceof User) {
            role = ((User) session.getAttribute("user")).getRole();
        }
        return "ADMIN".equalsIgnoreCase((String) role);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        //kiểm tra đăng nhập 
        HttpSession session = request.getSession(false);//=>trả về session null 
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect(request.getContextPath() + "/login?redirect="
                    + URLEncoder.encode("/admin/centres", StandardCharsets.UTF_8));
            return;
        }
        //kiểm tra phân quyền => nếu đăng nhập bỏ qua test 1 sang nhập url admin/center=> đẩy lỗi 403
        if (!isAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Chỉ Quản trị viên (ADMIN) mới có quyền truy cập trang Quản lý Cụm rạp.");
            return;
        }
        // thêm mới 
        String base = request.getContextPath() + "/admin/centres";
        String action = request.getParameter("action");

        // Điều hướng sang Form dùng chung (Thêm mới: action=create, Sửa: action=edit)
        if ("create".equalsIgnoreCase(action) || "edit".equalsIgnoreCase(action)) {
            Centre centre = null;
            if ("edit".equalsIgnoreCase(action)) {
                try {
                    int centreId = Integer.parseInt(request.getParameter("centreId"));
                    //lấy center
                    centre = cinemaService.getCentreById(centreId);
                    if (centre == null) {
                        response.sendRedirect(base + "?error=invalid_id");
                        return;
                    }
                } catch (Exception e) {
                    response.sendRedirect(base + "?error=invalid_id");
                    return;
                }
            }

            request.setAttribute("centre", centre);
            request.setAttribute("baseUrl", base);
            request.setAttribute("activeNav", "centres");
            request.setAttribute("pageTitle", centre != null ? "Sửa Cụm rạp" : "Thêm Cụm rạp mới");
            request.getRequestDispatcher("/WEB-INF/views/admin/cinema/centreForm.jsp").forward(request, response);
            return;
        }

        // Đọc tham số tìm kiếm và phân trang
        String search = request.getParameter("search");
        String status = request.getParameter("status");
        if (status == null || status.trim().isEmpty()) {
            status = "ALL";
        }

        int page = 1;
        try {
            String p = request.getParameter("page");
            if (p != null && !p.trim().isEmpty()) {
                page = Integer.parseInt(p.trim());
            }
        } catch (NumberFormatException ignored) {
        }

        /**
         * *****************************************************************************************************************************************
         */
        PageDTO<Centre> pageResult = cinemaService.searchCentres(search, status, page, PAGE_SIZE);
        //thống kê
        CinemaService.CentreKpi kpi = cinemaService.getCentresKpi();

        String filters = "search=" + URLEncoder.encode(search != null ? search.trim() : "", StandardCharsets.UTF_8)
                + "&status=" + URLEncoder.encode(status, StandardCharsets.UTF_8);

        request.setAttribute("result", pageResult);
        //thống kê
        request.setAttribute("totalCentres", kpi.totalCentres());
        request.setAttribute("activeCentres", kpi.activeCentres());
        request.setAttribute("inactiveCentres", kpi.inactiveCentres());
        request.setAttribute("totalHalls", kpi.totalHalls());
        //lấy lại người dùng nhập
        request.setAttribute("currentSearch", search != null ? search.trim() : "");
        request.setAttribute("currentStatus", status);
        //+filter để lưu tìm kiếm cũ 
        request.setAttribute("pagerBase", base + "?" + filters + "&page=");
        request.setAttribute("backQuery", filters + "&page=" + pageResult.getPage());
        request.setAttribute("baseUrl", base);
        //chuyển đỏ 
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
// lấy action 
        String action = request.getParameter("action");

        String search = request.getParameter("search");
        String status = request.getParameter("status");
        String pageParam = request.getParameter("page");
        String back = request.getParameter("back");

        StringBuilder redirectUrl;
        if (back != null && !back.trim().isEmpty()) {
            redirectUrl = new StringBuilder(request.getContextPath() + "/admin/centres?" + back.replaceAll("[^A-Za-z0-9%&=._~+-]", "") + "&");
        } else {
            redirectUrl = new StringBuilder(request.getContextPath() + "/admin/centres?");
            if (search != null && !search.trim().isEmpty()) {
                redirectUrl.append("search=").append(URLEncoder.encode(search.trim(), StandardCharsets.UTF_8)).append("&");
            }
            if (status != null && !status.trim().isEmpty()) {
                redirectUrl.append("status=").append(URLEncoder.encode(status.trim(), StandardCharsets.UTF_8)).append("&");
            }
            if (pageParam != null && !pageParam.trim().isEmpty()) {
                redirectUrl.append("page=").append(URLEncoder.encode(pageParam.trim(), StandardCharsets.UTF_8)).append("&");
            }
        }

        // Thêm Centre mới
        if ("create".equalsIgnoreCase(action)) {
            String centreName = request.getParameter("centreName");
            String address = request.getParameter("address");
            String phone = request.getParameter("phone");
            String city = request.getParameter("city");
// create center trả về success or fail 
            String result = cinemaService.createCentre(centreName, address, phone, city);
            if ("success".equals(result)) {//do post
                response.sendRedirect(request.getContextPath() + "/admin/centres?success=created");
            } else {//do get vì có tham số action 
                response.sendRedirect(request.getContextPath() + "/admin/centres?action=create&error=" + result);
            }
            return;
        }

        // Sửa thông tin Centre
        if ("update".equalsIgnoreCase(action)) {
            String centreIdParam = request.getParameter("centreId");
            String centreName = request.getParameter("centreName");
            String address = request.getParameter("address");
            String phone = request.getParameter("phone");
            String city = request.getParameter("city");

            int centreId;
            try {
                centreId = Integer.parseInt(centreIdParam.trim());
            } catch (Exception e) {
                response.sendRedirect(request.getContextPath() + "/admin/centres?error=invalid_id");
                return;
            }

            String result = cinemaService.updateCentre(centreId, centreName, address, phone, city);
            if ("success".equals(result)) {
                response.sendRedirect(request.getContextPath() + "/admin/centres?success=updated");
            } else {
                response.sendRedirect(request.getContextPath() + "/admin/centres?action=edit&centreId=" + centreId + "&error=" + result);
            }
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
