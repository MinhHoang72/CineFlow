package controller.admin.cinema;

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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller Cấu hình Sơ đồ Ghế (Seat Configuration). Đồng nhất 100% theo form
 * Voucher / Centre / Hall: - Gọi trực tiếp SeatDAO, HallDAO, CentreDAO (không
 * qua service trung gian thừa). - Kiểm tra phiên tập trung qua
 * authorizedUser(). - Phân tách 4 luồng: showSeats (GET), createNewGrid (POST),
 * saveSeats (POST), toggleSeatStatus (POST).
 */
@WebServlet(name = "SeatConfigurationServlet", urlPatterns = {"/manager/seat-config", "/admin/seat-config"})
public class SeatConfigurationServlet extends HttpServlet {

    private static final int PAGE_SIZE = 10;
    private final SeatDAO seatDAO = new SeatDAO();
    private final HallDAO hallDAO = new HallDAO();
    private final CentreDAO centreDAO = new CentreDAO();

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
        //check hall đã trùng với manager đã trùng centrer ID 
        if (user.getCentreId() == null || !user.getCentreId().equals(hall.getCentreId())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn chỉ được cấu hình sơ đồ ghế cho Cụm rạp mình phụ trách.");
            return;
        }

        showSeats(request, response, hall);
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
            toggleSeatStatus(request, response, hallId);
        } else if ("generateGrid".equalsIgnoreCase(action)) {
            createNewGrid(request, response, hallId);
        } else {
            saveSeats(request, response, hallId);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. SHOW SEATS (Đổ dữ liệu sơ đồ & phân trang)
    // ─────────────────────────────────────────────────────────────────────────
    private void showSeats(HttpServletRequest request, HttpServletResponse response,
            Hall hall) throws ServletException, IOException {
        int hallId = hall.getHallId();
        Centre centre = centreDAO.getCentreById(hall.getCentreId());

    
        List<SeatConfigDTO> Seats = seatDAO.getSeatConfigsByHall(hallId);

        // Tham số tìm kiếm và lọc ghế
        String search = request.getParameter("search");
        String seatType = request.getParameter("seatType");
        String seatStatus = request.getParameter("seatStatus");
        if (seatType == null || seatType.trim().isEmpty()) {
            seatType = "ALL";
        }
        if (seatStatus == null || seatStatus.trim().isEmpty()) {
            seatStatus = "ALL";
        }

        int page = 1;
        try {
            String pageParam = request.getParameter("page");
            if (pageParam != null && !pageParam.trim().isEmpty()) {
                page = Integer.parseInt(pageParam.trim());
            }
        } catch (NumberFormatException ignored) {
        }

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
  //gửi list sang jsp 
        request.setAttribute("existingSeats", Seats);
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
    // 2. CREATE NEW GRID (Tạo lưới sơ đồ mới theo số hàng x cột)
    // ─────────────────────────────────────────────────────────────────────────
    private void createNewGrid(HttpServletRequest request, HttpServletResponse response, int hallId)
            throws IOException {
        String base = baseUrl(request);
        int numRows = 6;
        int numCols = 10;
        try {
            numRows = Integer.parseInt(request.getParameter("rows").trim());
            numCols = Integer.parseInt(request.getParameter("cols").trim());
        } catch (Exception ignored) {
        }

        if (numRows < 1 || numRows > 26 || numCols < 1 || numCols > 30) {
            response.sendRedirect(base + "?hallId=" + hallId + "&error=failed");
            return;
        }

        List<SeatConfigDTO> newSeats = new ArrayList<>();
        for (int i = 0; i < numRows; i++) {
            String rowName = String.valueOf((char) ('A' + i));
            for (int c = 1; c <= numCols; c++) {
                newSeats.add(new SeatConfigDTO(0, hallId, rowName, c, "STANDARD", "AVAILABLE"));
            }
        }

        boolean ok = seatDAO.saveSeatLayout(hallId, newSeats, true);
        response.sendRedirect(base + "?hallId=" + hallId + (ok ? "&success=saved" : "&error=failed"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. SAVE SEATS (Đọc trực tiếp từ form HTML, không qua JSON)
    // ─────────────────────────────────────────────────────────────────────────
    private void saveSeats(HttpServletRequest request, HttpServletResponse response, int hallId)
            throws IOException {
        String base = baseUrl(request);
        String[] rows = request.getParameterValues("seatRow");
        String[] numbers = request.getParameterValues("seatNumber");
        String[] types = request.getParameterValues("seatType");
        String[] statuses = request.getParameterValues("status");

        if (rows == null || numbers == null || types == null || statuses == null
                || rows.length == 0
                || rows.length != numbers.length
                || rows.length != types.length
                || rows.length != statuses.length) {
            response.sendRedirect(base + "?hallId=" + hallId + "&error=empty_data");
            return;
        }

        try {
            List<SeatConfigDTO> seatList = new ArrayList<>();
            for (int i = 0; i < rows.length; i++) {
                int seatNum = Integer.parseInt(numbers[i].trim());
                seatList.add(new SeatConfigDTO(0, hallId, rows[i], seatNum, types[i], statuses[i]));
            }

            boolean ok = seatDAO.saveSeatLayout(hallId, seatList, false);
            response.sendRedirect(base + "?hallId=" + hallId + (ok ? "&success=saved" : "&error=failed"));
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(base + "?hallId=" + hallId + "&error=failed");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. TOGGLE SEAT STATUS
    // ─────────────────────────────────────────────────────────────────────────
    private void toggleSeatStatus(HttpServletRequest request, HttpServletResponse response, int hallId)
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
