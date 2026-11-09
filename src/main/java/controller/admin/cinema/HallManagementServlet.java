package controller.admin.cinema;

import dao.CentreDAO;
import dao.HallDAO;
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



@WebServlet(name = "HallManagementServlet", urlPatterns = {"/manager/halls", "/admin/halls"})
public class HallManagementServlet extends HttpServlet {

    private static final int PAGE_SIZE = 10;
    private final HallDAO hallDAO = new HallDAO();
    private final CentreDAO centreDAO = new CentreDAO();

    // ─────────────────────────────────────────────────────────────────────────
    // AUTHENTICATION & AUTHORIZATION
    // ─────────────────────────────────────────────────────────────────────────

    private User authorizedUser(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        //nếu người dùng chưa đăng nhập => session thành null => đã đăng nhập 
        HttpSession session = request.getSession(false);
        User user = (session == null) ? null : (User) session.getAttribute("user");

        if (user == null || session.getAttribute("userId") == null) {
            String redirectUrl = request.getContextPath() + "/login?redirect="
                    + URLEncoder.encode("/manager/halls", StandardCharsets.UTF_8);
            response.sendRedirect(redirectUrl);
            return null;
        }
        // phân role => đăng nhập phải có vai trò là manager
        if (!"MANAGER".equalsIgnoreCase(user.getRole()) && !user.isManager()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Chức năng này chỉ dành cho Quản lý chi nhánh (Branch Manager). Quản trị viên (Admin) không trực tiếp quản lý phòng chiếu.");
            return null;
        }

        return user;
    }

    private String baseUrl(HttpServletRequest request) {
        return request.getContextPath() + "/manager/halls";
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

        int centreId = (user.getCentreId() != null) ? user.getCentreId() : 0;
        //manager chưa được gán id 
        if (centreId <= 0) {
            request.setAttribute("noCentreAssigned", true);
            request.setAttribute("activeNav", "halls");
            request.setAttribute("pageTitle", "Quản lý Phòng chiếu");
            request.getRequestDispatcher("/WEB-INF/views/admin/cinema/hallList.jsp").forward(request, response);
            return;
        }

        String action = request.getParameter("action");
        if ("create".equalsIgnoreCase(action) || "edit".equalsIgnoreCase(action)) {
            showForm(request, response, centreId, action);
        } else {
            showList(request, response, centreId);
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

        int centreId = (user.getCentreId() != null) ? user.getCentreId() : 0;
        if (centreId <= 0) {
            response.sendRedirect(baseUrl(request) + "?error=no_centre");
            return;
        }

        String action = request.getParameter("action");
        if ("create".equalsIgnoreCase(action) || "update".equalsIgnoreCase(action)) {
            save(request, response, centreId, action);
        } else if ("toggleStatus".equalsIgnoreCase(action)) {
            toggleStatus(request, response, centreId);
        } else {
            response.sendRedirect(baseUrl(request));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. SHOW LIST (Hiển thị danh sách phòng & phân trang)
    // ─────────────────────────────────────────────────────────────────────────

    private void showList(HttpServletRequest request, HttpServletResponse response, int centreId)
            throws ServletException, IOException {
        String search = request.getParameter("search");
        String status = request.getParameter("status");
        if (status == null || status.trim().isEmpty()) {
            status = "ALL";
        }

        int page = 1;
        try {
            String pageParam = request.getParameter("page");
            if (pageParam != null && !pageParam.trim().isEmpty()) {
                page = Integer.parseInt(pageParam.trim());
            }
        } catch (NumberFormatException ignored) {}

        String base = baseUrl(request);
       //sql=select*from hall => all hall of centrer 
        PageDTO<Hall> pageResult = hallDAO.search(centreId, search, status, page, PAGE_SIZE);

        // Gọi thẳng HallDAO đếm KPI
        int totalHalls = hallDAO.countHallsByStatus(centreId, "ALL");
        int activeHalls = hallDAO.countHallsByStatus(centreId, "ACTIVE");
        int inactiveHalls = hallDAO.countHallsByStatus(centreId, "INACTIVE");
        int totalCapacity = hallDAO.sumCapacityByCentre(centreId);

        // Lấy thông tin cụm rạp
        Centre currentCentre = centreDAO.getCentreById(centreId);

        String filters = "search=" + URLEncoder.encode(search != null ? search.trim() : "", StandardCharsets.UTF_8)
                + "&status=" + URLEncoder.encode(status, StandardCharsets.UTF_8);

        request.setAttribute("result", pageResult);
        request.setAttribute("page", pageResult);
        request.setAttribute("currentCentre", currentCentre);
        request.setAttribute("currentSearch", search);
        request.setAttribute("currentStatus", status);
        request.setAttribute("targetCentreId", centreId);
        request.setAttribute("totalHalls", totalHalls);
        request.setAttribute("activeHalls", activeHalls);
        request.setAttribute("inactiveHalls", inactiveHalls);
        request.setAttribute("totalCapacity", totalCapacity);
        request.setAttribute("pagerBase", base + "?" + filters + "&page=");
        request.setAttribute("backQuery", filters + "&page=" + pageResult.getPage());
        request.setAttribute("baseUrl", base);
        request.setAttribute("activeNav", "halls");
        request.setAttribute("pageTitle", "Quản lý Phòng chiếu");

        request.getRequestDispatcher("/WEB-INF/views/admin/cinema/hallList.jsp").forward(request, response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. SHOW FORM (Mở Form Thêm mới / Sửa)
    // ─────────────────────────────────────────────────────────────────────────

    private void showForm(HttpServletRequest request, HttpServletResponse response, int centreId, String action)
            throws ServletException, IOException {
        String base = baseUrl(request);
        Hall hall = null;

        if ("edit".equalsIgnoreCase(action)) {
            try {
                int hallId = Integer.parseInt(request.getParameter("hallId"));
                hall = hallDAO.getHallById(hallId);
                if (hall == null || hall.getCentreId() != centreId) {
                    response.sendRedirect(base + "?error=invalid_hall");
                    return;
                }
            } catch (Exception e) {
                response.sendRedirect(base + "?error=invalid_id");
                return;
            }
        }

        Centre currentCentre = centreDAO.getCentreById(centreId);

        request.setAttribute("hall", hall);
        request.setAttribute("currentCentre", currentCentre);
        request.setAttribute("targetCentreId", centreId);
        request.setAttribute("baseUrl", base);
        request.setAttribute("activeNav", "halls");
        request.setAttribute("pageTitle", hall != null ? "Sửa Phòng chiếu" : "Thêm Phòng chiếu mới");

        request.getRequestDispatcher("/WEB-INF/views/admin/cinema/hallForm.jsp").forward(request, response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. SAVE (Tạo mới hoặc Cập nhật phòng chiếu)
    // ─────────────────────────────────────────────────────────────────────────

    private void save(HttpServletRequest request, HttpServletResponse response, int centreId, String action)
            throws IOException {
        String base = baseUrl(request);
        String hallName = request.getParameter("hallName");
        String hallType = request.getParameter("hallType");
        String capacityParam = request.getParameter("capacity");
        String status = request.getParameter("status");

        // Validate cơ bản
        if (hallName == null || hallName.trim().isEmpty()) {
            String errorUrl = "create".equalsIgnoreCase(action)
                    ? (base + "?action=create&error=missing_fields")
                    : (base + "?action=edit&hallId=" + request.getParameter("hallId") + "&error=missing_fields");
            response.sendRedirect(errorUrl);
            return;
        }

        hallName = hallName.trim();
        int capacity = 60;
        try {
            if (capacityParam != null && !capacityParam.trim().isEmpty()) {
                capacity = Integer.parseInt(capacityParam.trim());
                if (capacity <= 0) capacity = 60;
            }
        } catch (NumberFormatException ignored) {}

        if ("create".equalsIgnoreCase(action)) {
            // Kiểm tra trùng tên phòng trong chi nhánh
            if (hallDAO.isHallNameExists(centreId, hallName, 0)) {
                response.sendRedirect(base + "?action=create&error=duplicate_name");
                return;
            }

            Hall hall = new Hall();
            hall.setCentreId(centreId);
            hall.setHallName(hallName);
            hall.setHallType(hallType != null ? hallType.trim() : "2D");
            hall.setCapacity(capacity);
            hall.setStatus(status != null && !status.trim().isEmpty() ? status.trim() : "AVAILABLE");

            boolean ok = hallDAO.addHall(hall);
            response.sendRedirect(ok ? (base + "?success=created") : (base + "?action=create&error=failed"));

        } else {
            // Cập nhật (update)
            int hallId;
            try {
                hallId = Integer.parseInt(request.getParameter("hallId").trim());
            } catch (Exception e) {
                response.sendRedirect(base + "?error=invalid_id");
                return;
            }

            Hall existingHall = hallDAO.getHallById(hallId);
            if (existingHall == null || existingHall.getCentreId() != centreId) {
                response.sendRedirect(base + "?error=invalid_hall");
                return;
            }

            // Chặn chuyển sang INACTIVE nếu có suất chiếu tương lai đã có khách đặt
            boolean isDeactivating = existingHall.isActive() && ("INACTIVE".equalsIgnoreCase(status) || "MAINTENANCE".equalsIgnoreCase(status));
            if (isDeactivating && hallDAO.hasFutureBookedShowtimes(hallId)) {
                response.sendRedirect(base + "?action=edit&hallId=" + hallId + "&error=has_future_bookings");
                return;
            }

            // Kiểm tra trùng tên với phòng khác
            if (hallDAO.isHallNameExists(centreId, hallName, hallId)) {
                response.sendRedirect(base + "?action=edit&hallId=" + hallId + "&error=duplicate_name");
                return;
            }

            Hall updated = new Hall();
            updated.setHallId(hallId);
            updated.setCentreId(centreId);
            updated.setHallName(hallName);
            updated.setHallType(hallType != null ? hallType.trim() : "2D");
            updated.setCapacity(capacity);
            updated.setStatus(status != null ? status.trim() : "AVAILABLE");

            boolean ok = hallDAO.updateHall(updated);
            response.sendRedirect(ok ? (base + "?success=updated") : (base + "?action=edit&hallId=" + hallId + "&error=failed"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. TOGGLE STATUS (Bật / Tắt hoạt động trực tiếp)
    // ─────────────────────────────────────────────────────────────────────────

    private void toggleStatus(HttpServletRequest request, HttpServletResponse response, int centreId)
            throws IOException {
        String base = baseUrl(request);
        String back = request.getParameter("back");
        String redirectBase = (back != null && !back.trim().isEmpty())
                ? (base + "?" + back.replaceAll("[^A-Za-z0-9%&=._~+-]", "") + "&")
                : (base + "?");

        int hallId;
        try {
            hallId = Integer.parseInt(request.getParameter("hallId").trim());
        } catch (Exception e) {
            response.sendRedirect(redirectBase + "error=invalid_id");
            return;
        }

        Hall existingHall = hallDAO.getHallById(hallId);
        if (existingHall == null || existingHall.getCentreId() != centreId) {
            response.sendRedirect(redirectBase + "error=invalid_hall");
            return;
        }

        // Gọi hàm toggleStatus trực tiếp của DAO
        String result = hallDAO.toggleStatus(hallId);
        if ("SUCCESS".equalsIgnoreCase(result)) {
            response.sendRedirect(redirectBase + "success=status_updated");
        } else if ("HAS_FUTURE_BOOKINGS".equalsIgnoreCase(result)) {
            response.sendRedirect(redirectBase + "error=has_future_bookings");
        } else {
            response.sendRedirect(redirectBase + "error=failed");
        }
    }
}
