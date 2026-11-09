package controller.admin.cinema;

import dao.CentreDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.dto.common.PageDTO;
import model.entity.Centre;
import model.entity.User;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Controller Quản lý Cụm rạp (Centre Management).
 * Cấu trúc tinh gọn tương tác trực tiếp với CentreDAO (chuẩn Voucher & Hall):
 * - Kiểm tra phiên và quyền Admin tập trung qua authorizedUser().
 * - Phân tách 4 luồng rõ ràng: showList, showForm, save, toggleStatus.
 */
@WebServlet(name = "CentreManagementServlet", urlPatterns = {"/admin/centres", "/admin/centre"})
public class CentreManagementServlet extends HttpServlet {

    private static final int PAGE_SIZE = 10;
    private final CentreDAO centreDAO = new CentreDAO();

    // ─────────────────────────────────────────────────────────────────────────
    // AUTHENTICATION & AUTHORIZATION
    // ─────────────────────────────────────────────────────────────────────────

    private User authorizedUser(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        User user = (session == null) ? null : (User) session.getAttribute("user");

        // 1. Kiểm tra đăng nhập
        if (user == null || session.getAttribute("userId") == null) {
            response.sendRedirect(request.getContextPath() + "/login?redirect="
                    + URLEncoder.encode("/admin/centres", StandardCharsets.UTF_8));
            return null;
        }

        // 2. Kiểm tra quyền ADMIN
        if (!"ADMIN".equalsIgnoreCase(user.getRole()) && !user.isAdmin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Chỉ Quản trị viên (ADMIN) mới có quyền truy cập trang Quản lý Cụm rạp.");
            return null;
        }

        return user;
    }

    private String baseUrl(HttpServletRequest request) {
        return request.getContextPath() + "/admin/centres";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HTTP GET
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = authorizedUser(request, response);
        if (user == null) {
            return;
        }

        String action = request.getParameter("action");
        if ("create".equalsIgnoreCase(action) || "edit".equalsIgnoreCase(action)) {
            showForm(request, response, action);
        } else {
            showList(request, response);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HTTP POST
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        User user = authorizedUser(request, response);
        if (user == null) {
            return;
        }

        String action = request.getParameter("action");
        if ("create".equalsIgnoreCase(action) || "update".equalsIgnoreCase(action)) {
            save(request, response, action);
        } else if ("toggleStatus".equalsIgnoreCase(action)) {
            toggleStatus(request, response);
        } else {
            response.sendRedirect(baseUrl(request));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. SHOW LIST (Hiển thị danh sách cụm rạp & phân trang)
    // ─────────────────────────────────────────────────────────────────────────

    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
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
        } catch (NumberFormatException ignored) {}
     
      
        // Gọi thẳng CentreDAO lấy dữ liệu phân trang
        PageDTO<Centre> pageResult = centreDAO.search(search, status, page, PAGE_SIZE);

        // Gọi thẳng CentreDAO đếm KPI thống kê
        int totalCentres = centreDAO.countCentresByStatus("ALL");
        int activeCentres = centreDAO.countCentresByStatus("ACTIVE");
        int inactiveCentres = centreDAO.countCentresByStatus("INACTIVE");
        int totalHalls = centreDAO.countTotalHalls();
        //lấy đường dẫn để tiện gửi page phân trang 
         String base = baseUrl(request);
         //lấy lại search cũ để ấn trang chỉ cần điền page 
        String filters = "search=" + URLEncoder.encode(search != null ? search.trim() : "", StandardCharsets.UTF_8)
                + "&status=" + URLEncoder.encode(status, StandardCharsets.UTF_8);

        request.setAttribute("result", pageResult);
        request.setAttribute("totalCentres", totalCentres);
        request.setAttribute("activeCentres", activeCentres);
        request.setAttribute("inactiveCentres", inactiveCentres);
        request.setAttribute("totalHalls", totalHalls);
        request.setAttribute("currentSearch", search != null ? search.trim() : "");
        request.setAttribute("currentStatus", status);
        //trả về cho cái phân trang lấy  filter để lấy cái giá trị cũ 
        request.setAttribute("Base", base + "?" + filters + "&page=");
        request.setAttribute("backQuery", filters + "&page=" + pageResult.getPage());
        request.setAttribute("baseUrl", base);
        request.setAttribute("activeNav", "centres");
        request.setAttribute("pageTitle", "Quản lý Cụm rạp");

        request.getRequestDispatcher("/WEB-INF/views/admin/cinema/centreList.jsp").forward(request, response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. SHOW FORM (Mở form Thêm mới / Sửa cụm rạp)
    // ─────────────────────────────────────────────────────────────────────────

    private void showForm(HttpServletRequest request, HttpServletResponse response, String action)
            throws ServletException, IOException {
        String base = baseUrl(request);
        Centre centre = null;

        if ("edit".equalsIgnoreCase(action)) {
            try {
                int centreId = Integer.parseInt(request.getParameter("centreId"));
                centre = centreDAO.getCentreById(centreId);
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
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. SAVE (Tạo mới hoặc Cập nhật cụm rạp)
    // ─────────────────────────────────────────────────────────────────────────

    private void save(HttpServletRequest request, HttpServletResponse response, String action)
            throws IOException {
        String base = baseUrl(request);
        String centreName = request.getParameter("centreName");
        String address = request.getParameter("address");
        String phone = request.getParameter("phone");
        String city = request.getParameter("city");
        String centreIdParam = request.getParameter("centreId");

        String returnPrefix = "create".equalsIgnoreCase(action)
                ? (base + "?action=create&error=")
                : (base + "?action=edit&centreId=" + (centreIdParam != null ? centreIdParam.trim() : "") + "&error=");

        // 1. Validate không được để trống
        if (centreName == null || centreName.trim().isEmpty()
                || address == null || address.trim().isEmpty()
                || phone == null || phone.trim().isEmpty()
                || city == null || city.trim().isEmpty()) {
            response.sendRedirect(returnPrefix + "missing_fields");
            return;
        }

        centreName = centreName.trim();
        address = address.trim();
        phone = phone.trim();
        city = city.trim();

        // 2. Validate độ dài Tên cụm rạp (3 - 100 ký tự)
        if (centreName.length() < 3 || centreName.length() > 100) {
            response.sendRedirect(returnPrefix + "invalid_name");
            return;
        }

        // 3. Validate độ dài Địa chỉ (5 - 200 ký tự)
        if (address.length() < 3 || address.length() > 100) {
            response.sendRedirect(returnPrefix + "invalid_address");
            return;
        }

        // 4. Validate Số điện thoại Hotline: Phải bắt đầu bằng số 0, gồm 10 đến 11 chữ số {} số lần xuát hiện tối thiểu 
        if (!phone.matches("^0[0-9]{9,10}$")) {
            response.sendRedirect(returnPrefix + "invalid_phone");
            return;
        }

        // 5. Validate Tên thành phố (2 - 50 ký tự)
        if (city.length() < 3 || city.length() > 100) {
            response.sendRedirect(returnPrefix + "invalid_city");
            return;
        }
// create
        if ("create".equalsIgnoreCase(action)) {
            // Kiểm tra trùng tên cụm rạp
            if (centreDAO.isCentreNameExists(centreName, 0)) {
                response.sendRedirect(base + "?action=create&error=duplicate_name");
                return;
            }

            Centre centre = new Centre();
            centre.setCentreName(centreName);
            centre.setAddress(address);
            centre.setPhone(phone);
            centre.setCity(city);
            centre.setStatus("ACTIVE");

            boolean ok = centreDAO.addCentre(centre);
            response.sendRedirect(ok ? (base + "?success=created") : (base + "?action=create&error=failed"));
//update
        } else {
            // Update
            int centreId;
            try {
                centreId = Integer.parseInt(centreIdParam.trim());
            } catch (Exception e) {
                response.sendRedirect(base + "?error=invalid_id");
                return;
            }

            Centre existingCentre = centreDAO.getCentreById(centreId);
            if (existingCentre == null) {
                response.sendRedirect(base + "?error=invalid_id");
                return;
            }

            // Kiểm tra trùng tên với cụm rạp khác
            if (centreDAO.isCentreNameExists(centreName, centreId)) {
                response.sendRedirect(base + "?action=edit&centreId=" + centreId + "&error=duplicate_name");
                return;
            }

            Centre updated = new Centre();
            updated.setCentreId(centreId);
            updated.setCentreName(centreName);
            updated.setAddress(address);
            updated.setPhone(phone);
            updated.setCity(city);
            updated.setStatus(existingCentre.getStatus());

            boolean ok = centreDAO.updateCentre(updated);
            response.sendRedirect(ok ? (base + "?success=updated") : (base + "?action=edit&centreId=" + centreId + "&error=failed"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. TOGGLE STATUS (Bật / Tắt hoạt động của cụm rạp)
    // ─────────────────────────────────────────────────────────────────────────

    private void toggleStatus(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String base = baseUrl(request);
        String back = request.getParameter("back");
        String redirectBase = (back != null && !back.trim().isEmpty())
                ? (base + "?" + back.replaceAll("[^A-Za-z0-9%&=._~+-]", "") + "&")
                : (base + "?");

        try {
            int centreId = Integer.parseInt(request.getParameter("centreId").trim());
            boolean ok = centreDAO.toggleStatus(centreId);
            response.sendRedirect(redirectBase + (ok ? "success=status_updated" : "error=failed"));
        } catch (Exception e) {
            response.sendRedirect(redirectBase + "error=invalid_id");
        }
    }
}
