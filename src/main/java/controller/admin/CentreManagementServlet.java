package controller.admin;

import com.google.gson.Gson;
import dao.CentreDAO;
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

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controller dieu khien phan he Quan ly Cum rap (UC-FE-03.1: Manage Cinema Centres).
 * Danh cho Quan tri vien (Admin).
 *
 * Nghiep vu chuan:
 * - Hien thi danh sach Cum rap (ID, Ten, Dia chi, Hotline, Trang thai)
 * - BR-HS-01: Quan ly danh sach cac Hall truc thuoc tung Centre
 * - Them Centre: Nhap Ten rap, Dia chi, Hotline -> Kiem tra khong trung ten -> Luu DB
 * - Sua Centre: Cap nhat thong tin lien he, hotline, dia chi, ten rap -> Kiem tra khong trung ten -> Luu DB
 * - Toggle Status: Bat/tat trang thai (Active / Inactive) cua Centre.
 */
@WebServlet(name = "CentreManagementServlet", urlPatterns = {"/admin/centres", "/admin/centre"})
public class CentreManagementServlet extends HttpServlet {

    private static final int PAGE_SIZE = 6;
    private final CentreDAO centreDAO = new CentreDAO();
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
                    "Chi Quan tri vien (ADMIN) moi co quyen truy cap trang Quan ly Cum rap.");
            return;
        }

        // AJAX: lay danh sach Hall cua mot Centre (BR-HS-01)
        String action = request.getParameter("action");
        if ("getHalls".equalsIgnoreCase(action)) {
            response.setContentType("application/json;charset=UTF-8");
            try {
                int centreId = Integer.parseInt(request.getParameter("centreId").trim());
                List<Hall> halls = centreDAO.getHallsByCentreId(centreId);
                response.getWriter().write(gson.toJson(halls));
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("[]");
            }
            return;
        }

        // Doc tham so tim kiem va phan trang
        String search = request.getParameter("search");
        String status = request.getParameter("status");
        if (status == null || status.trim().isEmpty()) status = "ALL";

        int page = 1;
        try {
            String p = request.getParameter("page");
            if (p != null && !p.trim().isEmpty()) page = Integer.parseInt(p.trim());
        } catch (NumberFormatException ignored) { }

        // Dung PageDTO: lay du lieu + phan trang trong mot lan goi
        PageDTO<Centre> pageResult = centreDAO.search(search, status, page, PAGE_SIZE);

        // Thong ke KPI (khong bi anh huong boi bo loc/phan trang)
        int totalCentres    = centreDAO.countCentresByStatus("ALL");
        int activeCentres   = centreDAO.countCentresByStatus("ACTIVE");
        int inactiveCentres = centreDAO.countCentresByStatus("INACTIVE");
        int totalHalls      = centreDAO.countTotalHalls();

        // Gan du lieu vao request attributes
        request.setAttribute("page", pageResult);
        request.setAttribute("centres", pageResult.getItems());
        request.setAttribute("totalCentres", totalCentres);
        request.setAttribute("activeCentres", activeCentres);
        request.setAttribute("inactiveCentres", inactiveCentres);
        request.setAttribute("totalHalls", totalHalls);
        request.setAttribute("currentSearch", search != null ? search.trim() : "");
        request.setAttribute("currentStatus", status);
        request.setAttribute("activeNav", "centres");
        request.setAttribute("pageTitle", "Quan ly Cum rap");

        request.getRequestDispatcher("/WEB-INF/views/admin/centreList.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        if (!isAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Ban khong co quyen thuc hien thao tac nay.");
            return;
        }

        String action    = request.getParameter("action");
        String search    = request.getParameter("search");
        String status    = request.getParameter("status");
        String pageParam = request.getParameter("page");

        StringBuilder redirectUrl = new StringBuilder(request.getContextPath() + "/admin/centres?");
        if (search != null && !search.trim().isEmpty())
            redirectUrl.append("search=").append(URLEncoder.encode(search.trim(), StandardCharsets.UTF_8)).append("&");
        if (status != null && !status.trim().isEmpty())
            redirectUrl.append("status=").append(URLEncoder.encode(status.trim(), StandardCharsets.UTF_8)).append("&");
        if (pageParam != null && !pageParam.trim().isEmpty())
            redirectUrl.append("page=").append(URLEncoder.encode(pageParam.trim(), StandardCharsets.UTF_8)).append("&");

        // ==============================================================
        // TAC VU 1: THEM CENTRE MOI
        // ==============================================================
        if ("create".equalsIgnoreCase(action)) {
            String centreName = request.getParameter("centreName");
            String address    = request.getParameter("address");
            String phone      = request.getParameter("phone");
            String city       = request.getParameter("city");

            if (centreName == null || centreName.trim().isEmpty()
                    || address == null || address.trim().isEmpty()
                    || phone == null || phone.trim().isEmpty()) {
                redirectUrl.append("error=missing_fields");
                response.sendRedirect(redirectUrl.toString());
                return;
            }

            centreName = centreName.trim();
            address    = address.trim();
            phone      = phone.trim();
            city       = city != null ? city.trim() : "";

            if (centreDAO.isCentreNameExists(centreName, 0)) {
                redirectUrl.append("error=duplicate_name");
                response.sendRedirect(redirectUrl.toString());
                return;
            }

            Centre centre = new Centre();
            centre.setCentreName(centreName);
            centre.setAddress(address);
            centre.setPhone(phone);
            centre.setCity(city);
            centre.setStatus("ACTIVE");

            redirectUrl.append(centreDAO.addCentre(centre) ? "success=created" : "error=failed");
            response.sendRedirect(redirectUrl.toString());
            return;
        }

        // ==============================================================
        // TAC VU 2: SUA THONG TIN CENTRE
        // ==============================================================
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

            if (centreName == null || centreName.trim().isEmpty()
                    || address == null || address.trim().isEmpty()
                    || phone == null || phone.trim().isEmpty()) {
                redirectUrl.append("error=missing_fields");
                response.sendRedirect(redirectUrl.toString());
                return;
            }

            centreName = centreName.trim();
            address    = address.trim();
            phone      = phone.trim();
            city       = city != null ? city.trim() : "";

            if (centreDAO.isCentreNameExists(centreName, centreId)) {
                redirectUrl.append("error=duplicate_name");
                response.sendRedirect(redirectUrl.toString());
                return;
            }

            Centre centre = new Centre();
            centre.setCentreId(centreId);
            centre.setCentreName(centreName);
            centre.setAddress(address);
            centre.setPhone(phone);
            centre.setCity(city);

            redirectUrl.append(centreDAO.updateCentre(centre) ? "success=updated" : "error=failed");
            response.sendRedirect(redirectUrl.toString());
            return;
        }

        // ==============================================================
        // TAC VU 3: TOGGLE STATUS (BAT / TAT TRANG THAI ACTIVE <-> INACTIVE)
        // ==============================================================
        if ("toggleStatus".equalsIgnoreCase(action)) {
            try {
                int centreId = Integer.parseInt(request.getParameter("centreId").trim());
                redirectUrl.append(centreDAO.toggleStatus(centreId) ? "success=status_updated" : "error=failed");
            } catch (Exception e) {
                redirectUrl.append("error=invalid_id");
            }
            response.sendRedirect(redirectUrl.toString());
            return;
        }

        response.sendRedirect(redirectUrl.toString());
    }
}