package controller;

import com.google.gson.Gson;
import dao.CentreDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Centre;
import model.Hall;
import model.User;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controller điều khiển phân hệ Quản lý Cụm rạp (UC-FE-03.1: Manage Cinema Centres).
 * Dành cho Quản trị viên (Admin).
 *
 * Nghiệp vụ chuẩn:
 * - Hiển thị danh sách Cụm rạp (ID, Tên, Địa chỉ, Hotline, Trạng thái)
 * - BR-HS-01: Quản lý danh sách các Hall trực thuộc từng Centre
 * - Thêm Centre: Nhập Tên rạp, Địa chỉ, Hotline -> Kiểm tra không trùng tên -> Lưu DB
 * - Sửa Centre: Cập nhật thông tin liên hệ, hotline, địa chỉ, tên rạp -> Kiểm tra không trùng tên -> Lưu DB
 * - Toggle Status: Bật/tắt trạng thái (Active / Inactive) của Centre. (Nếu Inactive, các Hall và Suất chiếu
 *   của Centre này sẽ tạm ngưng hoạt động trên luồng đặt vé).
 */
@WebServlet(name = "CentreManagementServlet", urlPatterns = {"/admin/centres", "/admin/centre"})
public class CentreManagementServlet extends HttpServlet {

    private final CentreDAO centreDAO = new CentreDAO();
    private final Gson gson = new Gson();

    /**
     * Kiểm tra phiên đăng nhập hiện tại có vai trò ADMIN hay không.
     *
     * @param session phiên làm việc HttpSession
     * @return true nếu là Admin, ngược lại false
     */
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

    /**
     * Xử lý yêu cầu HTTP GET:
     * 1. Lấy danh sách các Hall trực thuộc cụm rạp qua AJAX (trả về JSON).
     * 2. Hoặc tải toàn bộ danh sách Cụm rạp kèm thống kê và hiển thị giao diện JSP.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Kiểm tra xác thực đăng nhập
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            String loginUrl = request.getContextPath() + "/auth/login.jsp?redirect="
                    + URLEncoder.encode("/admin/centres", StandardCharsets.UTF_8);
            response.sendRedirect(loginUrl);
            return;
        }

        // 2. Kiểm tra quyền hạn Admin (RBAC)
        if (!isAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Quản trị viên (ADMIN) mới có quyền truy cập trang Quản lý Cụm rạp.");
            return;
        }

        // 3. Nếu yêu cầu lấy danh sách các Hall trực thuộc của một Centre (BR-HS-01) qua AJAX
        String action = request.getParameter("action");
        if ("getHalls".equalsIgnoreCase(action)) {
            response.setContentType("application/json;charset=UTF-8");
            String centreIdParam = request.getParameter("centreId");
            try {
                int centreId = Integer.parseInt(centreIdParam.trim());
                List<Hall> halls = centreDAO.getHallsByCentreId(centreId);
                response.getWriter().write(gson.toJson(halls));
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("[]");
            }
            return;
        }

        // 4. Đọc các tham số tìm kiếm và bộ lọc trạng thái
        String search = request.getParameter("search");
        String status = request.getParameter("status");
        if (status == null || status.trim().isEmpty()) {
            status = "ALL";
        }

        // 5. Truy vấn danh sách Centre từ CSDL
        List<Centre> centres = centreDAO.getAllCentres(search, status);

        // 6. Thống kê số lượng
        int totalCentres = centreDAO.countCentresByStatus("ALL");
        int activeCentres = centreDAO.countCentresByStatus("ACTIVE");
        int inactiveCentres = centreDAO.countCentresByStatus("INACTIVE");
        int totalHalls = centreDAO.countTotalHalls();

        // 7. Gán dữ liệu vào request attributes
        request.setAttribute("centres", centres);
        request.setAttribute("totalCentres", totalCentres);
        request.setAttribute("activeCentres", activeCentres);
        request.setAttribute("inactiveCentres", inactiveCentres);
        request.setAttribute("totalHalls", totalHalls);
        request.setAttribute("currentSearch", search != null ? search.trim() : "");
        request.setAttribute("currentStatus", status);
        request.setAttribute("activeNav", "centres");
        request.setAttribute("pageTitle", "Quản lý Cụm rạp");

        // 8. Chuyển tiếp tới giao diện admin/centres.jsp
        request.getRequestDispatcher("/admin/centres.jsp").forward(request, response);
    }

    /**
     * Xử lý yêu cầu HTTP POST:
     * - Thêm Centre mới (kiểm tra không trùng tên rạp)
     * - Sửa Centre (kiểm tra không trùng tên rạp)
     * - Toggle Status (bật/tắt trạng thái Active / Inactive)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Thiết lập mã hóa tiếng Việt UTF-8
        request.setCharacterEncoding("UTF-8");

        // 2. Kiểm tra phiên và quyền Admin
        HttpSession session = request.getSession(false);
        if (!isAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền thực hiện thao tác này.");
            return;
        }

        // 3. Đọc hành động cần thực thi và lưu lại tham số lọc tìm kiếm để chuyển hướng tiện lợi
        String action = request.getParameter("action");
        String search = request.getParameter("search");
        String status = request.getParameter("status");

        StringBuilder redirectUrl = new StringBuilder(request.getContextPath() + "/admin/centres?");
        if (search != null && !search.trim().isEmpty()) {
            redirectUrl.append("search=").append(URLEncoder.encode(search.trim(), StandardCharsets.UTF_8)).append("&");
        }
        if (status != null && !status.trim().isEmpty()) {
            redirectUrl.append("status=").append(URLEncoder.encode(status.trim(), StandardCharsets.UTF_8)).append("&");
        }

        // ==============================================================
        // TÁC VỤ 1: THÊM CENTRE MỚI
        // ==============================================================
        if ("create".equalsIgnoreCase(action)) {
            String centreName = request.getParameter("centreName");
            String address = request.getParameter("address");
            String phone = request.getParameter("phone");
            String city = request.getParameter("city");

            // Kiểm tra rỗng các trường bắt buộc
            if (centreName == null || centreName.trim().isEmpty()
                    || address == null || address.trim().isEmpty()
                    || phone == null || phone.trim().isEmpty()) {
                redirectUrl.append("error=missing_fields");
                response.sendRedirect(redirectUrl.toString());
                return;
            }

            centreName = centreName.trim();
            address = address.trim();
            phone = phone.trim();
            city = city != null ? city.trim() : "";

            // Kiểm tra không trùng tên cụm rạp
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

            boolean ok = centreDAO.addCentre(centre);
            if (ok) {
                redirectUrl.append("success=created");
            } else {
                redirectUrl.append("error=failed");
            }
            response.sendRedirect(redirectUrl.toString());
            return;
        }

        // ==============================================================
        // TÁC VỤ 2: SỬA THÔNG TIN CENTRE
        // ==============================================================
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
                redirectUrl.append("error=invalid_id");
                response.sendRedirect(redirectUrl.toString());
                return;
            }

            // Kiểm tra rỗng
            if (centreName == null || centreName.trim().isEmpty()
                    || address == null || address.trim().isEmpty()
                    || phone == null || phone.trim().isEmpty()) {
                redirectUrl.append("error=missing_fields");
                response.sendRedirect(redirectUrl.toString());
                return;
            }

            centreName = centreName.trim();
            address = address.trim();
            phone = phone.trim();
            city = city != null ? city.trim() : "";

            // Kiểm tra không trùng tên với các rạp khác
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

            boolean ok = centreDAO.updateCentre(centre);
            if (ok) {
                redirectUrl.append("success=updated");
            } else {
                redirectUrl.append("error=failed");
            }
            response.sendRedirect(redirectUrl.toString());
            return;
        }

        // ==============================================================
        // TÁC VỤ 3: TOGGLE STATUS (BẬT / TẮT TRẠNG THÁI ACTIVE <-> INACTIVE)
        // ==============================================================
        if ("toggleStatus".equalsIgnoreCase(action)) {
            String centreIdParam = request.getParameter("centreId");
            try {
                int centreId = Integer.parseInt(centreIdParam.trim());
                boolean ok = centreDAO.toggleStatus(centreId);
                if (ok) {
                    redirectUrl.append("success=status_updated");
                } else {
                    redirectUrl.append("error=failed");
                }
            } catch (Exception e) {
                redirectUrl.append("error=invalid_id");
            }
            response.sendRedirect(redirectUrl.toString());
            return;
        }

        // Hành động không xác định -> trở về trang chính
        response.sendRedirect(redirectUrl.toString());
    }
}
