package controller.admin.cinema;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dao.CentreDAO;
import dao.HallDAO;
import dao.SeatDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.dto.common.PageDTO;
import model.dto.pricing.SeatConfigDTO;
import model.entity.Centre;
import model.entity.Hall;
import model.entity.User;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controller Cấu hình Sơ đồ Ghế (Seat Configuration).
 * Đồng nhất 100% theo form Voucher / Centre / Hall:
 * - Gọi trực tiếp SeatDAO, HallDAO, CentreDAO (không qua service trung gian thừa).
 * - Kiểm tra phiên tập trung qua authorizedUser().
 * - Phân tách 3 luồng: showLayout (GET), saveLayout (POST), toggleMaintenance (POST).
 */
@WebServlet(name = "SeatConfigurationServlet", urlPatterns = {"/manager/seat-config", "/admin/seat-config"})
public class SeatConfigurationServlet extends HttpServlet {

    private static final int PAGE_SIZE = 10;
    private final SeatDAO seatDAO = new SeatDAO();
    private final HallDAO hallDAO = new HallDAO();
    private final CentreDAO centreDAO = new CentreDAO();
    private final Gson gson = new Gson();

    // ─────────────────────────────────────────────────────────────────────────
    // AUTHENTICATION & AUTHORIZATION
    // ─────────────────────────────────────────────────────────────────────────

    private User authorizedUser(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        User user = (session == null) ? null : (User) session.getAttribute("user");

        if (user == null || session.getAttribute("userId") == null) {
            String redirectUrl = request.getContextPath() + "/login?redirect="
                    + URLEncoder.encode("/manager/halls", StandardCharsets.UTF_8);
            response.sendRedirect(redirectUrl);
            return null;
        }

        if (!"MANAGER".equalsIgnoreCase(user.getRole()) && !user.isManager()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Chức năng cấu hình sơ đồ ghế chỉ dành cho Quản lý chi nhánh (Manager).");
            return null;
        }

        return user;
    }

    private String baseUrl(HttpServletRequest request) {
        return request.getContextPath() + "/manager/seat-config";
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

        int hallId;
        try {
            hallId = Integer.parseInt(request.getParameter("hallId").trim());
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/manager/halls?error=invalid_hall");
            return;
        }

        Hall hall = hallDAO.getHallById(hallId);
        if (hall == null) {
            response.sendRedirect(request.getContextPath() + "/manager/halls?error=invalid_hall");
            return;
        }

        if (user.getCentreId() == null || !user.getCentreId().equals(hall.getCentreId())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn chỉ được cấu hình sơ đồ ghế cho Cụm rạp mình phụ trách.");
            return;
        }

        showLayout(request, response, hall, hallId);
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

        int hallId;
        try {
            hallId = Integer.parseInt(request.getParameter("hallId").trim());
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/manager/halls?error=invalid_hall");
            return;
        }

        Hall hall = hallDAO.getHallById(hallId);
        if (hall == null || user.getCentreId() == null || !user.getCentreId().equals(hall.getCentreId())) {
            response.sendRedirect(request.getContextPath() + "/manager/halls?error=invalid_hall");
            return;
        }

        String action = request.getParameter("action");
        if ("toggleMaintenance".equalsIgnoreCase(action)) {
            toggleMaintenance(request, response, hallId);
        } else {
            saveLayout(request, response, hallId);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. SHOW LAYOUT (Đổ dữ liệu sơ đồ & phân trang)
    // ─────────────────────────────────────────────────────────────────────────

    private void showLayout(HttpServletRequest request, HttpServletResponse response,
                            Hall hall, int hallId) throws ServletException, IOException {
        Centre centre = centreDAO.getCentreById(hall.getCentreId());

        // Lấy danh sách ghế & JSON trực tiếp từ SeatDAO
        List<SeatConfigDTO> existingSeats = seatDAO.getSeatConfigsByHall(hallId);
        String seatsJson = gson.toJson(existingSeats);

        // Tham số tìm kiếm và lọc ghế
        String search = request.getParameter("search");
        String seatType = request.getParameter("seatType");
        String seatStatus = request.getParameter("seatStatus");
        if (seatType == null || seatType.trim().isEmpty()) seatType = "ALL";
        if (seatStatus == null || seatStatus.trim().isEmpty()) seatStatus = "ALL";

        int page = 1;
        try {
            String pageParam = request.getParameter("page");
            if (pageParam != null && !pageParam.trim().isEmpty()) {
                page = Integer.parseInt(pageParam.trim());
            }
        } catch (NumberFormatException ignored) {}

        // Đếm và lấy danh sách phân trang trực tiếp từ SeatDAO
        int total = seatDAO.countSeatsByHall(hallId, search, seatType, seatStatus);
        List<SeatConfigDTO> items = seatDAO.getSeatsByHallPaged(hallId, search, seatType, seatStatus, page, PAGE_SIZE);
        PageDTO<SeatConfigDTO> seatPageResult = new PageDTO<>(items, page, PAGE_SIZE, total);

        String base = baseUrl(request);
        String filters = "hallId=" + hallId
                + "&search=" + URLEncoder.encode(search != null ? search.trim() : "", StandardCharsets.UTF_8)
                + "&seatType=" + URLEncoder.encode(seatType, StandardCharsets.UTF_8)
                + "&seatStatus=" + URLEncoder.encode(seatStatus, StandardCharsets.UTF_8);

        request.setAttribute("hall", hall);
        request.setAttribute("centre", centre);
        request.setAttribute("existingSeats", existingSeats);
        request.setAttribute("seatsJson", seatsJson);
        request.setAttribute("hasBookedShowtimes", false);
        request.setAttribute("isAdmin", false);

        request.setAttribute("pagedSeats", seatPageResult.getItems());
        request.setAttribute("result", seatPageResult);
        request.setAttribute("page", seatPageResult);
        request.setAttribute("currentPage", seatPageResult.getPage());
        request.setAttribute("pageSize", PAGE_SIZE);
        request.setAttribute("totalPages", seatPageResult.getTotalPages());
        request.setAttribute("totalFilteredSeats", seatPageResult.getTotal());
        request.setAttribute("currentSearch", search != null ? search.trim() : "");
        request.setAttribute("currentSeatType", seatType);
        request.setAttribute("currentSeatStatus", seatStatus);

        request.setAttribute("pagerBase", base + "?" + filters + "&page=");
        request.setAttribute("backQuery", filters + "&page=" + seatPageResult.getPage());
        request.setAttribute("baseUrl", base);
        request.setAttribute("activeNav", "halls");
        request.setAttribute("pageTitle", "Cấu hình Sơ đồ ghế - " + hall.getHallName());

        request.getRequestDispatcher("/WEB-INF/views/admin/cinema/seatConfig.jsp").forward(request, response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. SAVE LAYOUT
    // ─────────────────────────────────────────────────────────────────────────

    private void saveLayout(HttpServletRequest request, HttpServletResponse response, int hallId)
            throws IOException {
        String base = baseUrl(request);
        String seatDataJson = request.getParameter("seatData");
        boolean isRegenerating = "true".equalsIgnoreCase(request.getParameter("isRegenerating"));

        if (seatDataJson == null || seatDataJson.trim().isEmpty()) {
            response.sendRedirect(base + "?hallId=" + hallId + "&error=empty_data");
            return;
        }

        try {
            Type listType = new TypeToken<List<SeatConfigDTO>>() {}.getType();
            List<SeatConfigDTO> seatList = gson.fromJson(seatDataJson, listType);

            boolean ok = seatDAO.saveSeatLayout(hallId, seatList, isRegenerating);
            if (ok) {
                response.sendRedirect(base + "?hallId=" + hallId + "&success=saved");
            } else {
                response.sendRedirect(base + "?hallId=" + hallId + "&error=failed");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(base + "?hallId=" + hallId + "&error=json_error");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. TOGGLE MAINTENANCE
    // ─────────────────────────────────────────────────────────────────────────

    private void toggleMaintenance(HttpServletRequest request, HttpServletResponse response, int hallId)
            throws IOException {
        String base = baseUrl(request);
        String seatIdParam = request.getParameter("seatId");
        String back = request.getParameter("back");
        String redirectBase = (back != null && !back.trim().isEmpty())
                ? (base + "?" + back.replaceAll("[^A-Za-z0-9%&=._~+-]", "") + "&")
                : (base + "?hallId=" + hallId + "&");

        try {
            int seatId = Integer.parseInt(seatIdParam.trim());
            String result = seatDAO.toggleSeatMaintenance(seatId, hallId);
            boolean ok = "SUCCESS".equalsIgnoreCase(result);
            response.sendRedirect(redirectBase + (ok ? "success=maintenance_toggled" : "error=failed"));
        } catch (Exception e) {
            response.sendRedirect(redirectBase + "error=invalid_id");
        }
    }
}
