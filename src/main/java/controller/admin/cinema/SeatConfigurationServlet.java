package controller.admin.cinema;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.entity.Centre;
import model.entity.Hall;
import model.dto.pricing.SeatConfigDTO;
import model.entity.User;
import service.CinemaService;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controller điều khiển cấu hình Sơ đồ Ghế (UC-FE-03.3: Configure Cinema Hall Seat Layout).
 * Phân quyền chuẩn: Chỉ Branch Manager mới được quyền cấu hình sơ đồ ghế cho phòng chiếu thuộc chi nhánh mình.
 * Admin quản lý hệ thống vĩ mô, không trực tiếp vẽ hay cấu hình sơ đồ ghế.
 * Toàn bộ logic nghiệp vụ được ủy quyền cho CinemaService (Mô hình 3 lớp).
 */
@WebServlet(name = "SeatConfigurationServlet", urlPatterns = {"/manager/seat-config", "/admin/seat-config"})
public class SeatConfigurationServlet extends HttpServlet {

    private final CinemaService cinemaService = new CinemaService();
    private final Gson gson = new Gson();

    /**
     * Kiểm tra người dùng hiện tại có vai trò MANAGER hay không.
     */
    private boolean isManager(HttpSession session) {
        if (session == null) {
            return false;
        }
        Object roleObj = session.getAttribute("role");
        if (roleObj instanceof String) {
            String role = (String) roleObj;
            if ("MANAGER".equalsIgnoreCase(role)) {
                return true;
            }
        }
        Object userObj = session.getAttribute("user");
        if (userObj instanceof User) {
            String role = ((User) userObj).getRole();
            return role != null && "MANAGER".equalsIgnoreCase(role);
        }
        return false;
    }

    private User getCurrentUser(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object userObj = session.getAttribute("user");
        return userObj instanceof User ? (User) userObj : null;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            String redirectUrl = request.getContextPath() + "/login?redirect="
                    + URLEncoder.encode("/manager/halls", StandardCharsets.UTF_8);
            response.sendRedirect(redirectUrl);
            return;
        }

        // Chỉ cho phép Quản lý chi nhánh
        if (!isManager(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Chức năng cấu hình sơ đồ ghế chỉ dành cho Quản lý chi nhánh (Manager). Quản trị viên (Admin) không trực tiếp cấu hình ghế.");
            return;
        }

        String hallIdParam = request.getParameter("hallId");
        int hallId;
        try {
            hallId = Integer.parseInt(hallIdParam.trim());
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/manager/halls?error=invalid_hall");
            return;
        }

        Hall hall = cinemaService.getHallById(hallId);
        if (hall == null) {
            response.sendRedirect(request.getContextPath() + "/manager/halls?error=invalid_hall");
            return;
        }

        User user = getCurrentUser(session);

        // Bắt buộc phòng chiếu phải thuộc Cụm rạp mà Manager phụ trách
        if (user == null || user.getCentreId() == null || !user.getCentreId().equals(hall.getCentreId())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn chỉ được cấu hình sơ đồ ghế cho Cụm rạp mình phụ trách.");
            return;
        }

        Centre centre = cinemaService.getCentreById(hall.getCentreId());
        boolean hasBookedShowtimes = cinemaService.hasFutureBookedShowtimes(hallId);

        List<SeatConfigDTO> existingSeats = cinemaService.getSeatConfigsByHall(hallId);
        String seatsJson = gson.toJson(existingSeats);

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
        int pageSize = 12;
        try {
            String pageParam = request.getParameter("page");
            if (pageParam != null && !pageParam.trim().isEmpty()) {
                page = Integer.parseInt(pageParam.trim());
            }
        } catch (NumberFormatException ignored) {
        }
        if (page < 1) {
            page = 1;
        }

        int totalFilteredSeats = cinemaService.countSeatsByHall(hallId, search, seatType, seatStatus);
        int totalPages = (int) Math.ceil((double) totalFilteredSeats / pageSize);
        if (totalPages < 1) {
            totalPages = 1;
        }
        if (page > totalPages) {
            page = totalPages;
        }

        List<SeatConfigDTO> pagedSeats = cinemaService.getSeatsByHallPaged(hallId, search, seatType, seatStatus, page, pageSize);

        request.setAttribute("hall", hall);
        request.setAttribute("centre", centre);
        request.setAttribute("existingSeats", existingSeats);
        request.setAttribute("seatsJson", seatsJson);
        request.setAttribute("hasBookedShowtimes", hasBookedShowtimes);
        request.setAttribute("isAdmin", false);

        request.setAttribute("pagedSeats", pagedSeats);
        request.setAttribute("currentPage", page);
        request.setAttribute("pageSize", pageSize);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalFilteredSeats", totalFilteredSeats);
        request.setAttribute("currentSearch", search != null ? search.trim() : "");
        request.setAttribute("currentSeatType", seatType);
        request.setAttribute("currentSeatStatus", seatStatus);

        request.setAttribute("activeNav", "halls");
        request.setAttribute("pageTitle", "Cấu hình Sơ đồ ghế - " + hall.getHallName());

        request.getRequestDispatcher("/WEB-INF/views/admin/cinema/seatConfig.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (!isManager(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền thực hiện thao tác này. Chức năng chỉ dành cho Quản lý chi nhánh (Manager).");
            return;
        }

        String hallIdParam = request.getParameter("hallId");
        int hallId;
        try {
            hallId = Integer.parseInt(hallIdParam.trim());
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/manager/halls?error=invalid_hall");
            return;
        }

        Hall hall = cinemaService.getHallById(hallId);
        if (hall == null) {
            response.sendRedirect(request.getContextPath() + "/manager/halls?error=invalid_hall");
            return;
        }

        User user = getCurrentUser(session);

        if (user == null || user.getCentreId() == null || !user.getCentreId().equals(hall.getCentreId())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn chỉ được cấu hình sơ đồ ghế cho Cụm rạp mình phụ trách.");
            return;
        }

        String action = request.getParameter("action");
        String filterSearch = request.getParameter("search");
        String filterSeatType = request.getParameter("seatType");
        String filterSeatStatus = request.getParameter("seatStatus");
        String pageParam = request.getParameter("page");

        StringBuilder redirectUrl = new StringBuilder(request.getContextPath() + "/manager/seat-config?hallId=" + hallId + "&");
        if (filterSearch != null && !filterSearch.trim().isEmpty()) {
            redirectUrl.append("search=").append(URLEncoder.encode(filterSearch.trim(), StandardCharsets.UTF_8)).append("&");
        }
        if (filterSeatType != null && !filterSeatType.trim().isEmpty()) {
            redirectUrl.append("seatType=").append(URLEncoder.encode(filterSeatType.trim(), StandardCharsets.UTF_8)).append("&");
        }
        if (filterSeatStatus != null && !filterSeatStatus.trim().isEmpty()) {
            redirectUrl.append("seatStatus=").append(URLEncoder.encode(filterSeatStatus.trim(), StandardCharsets.UTF_8)).append("&");
        }
        if (pageParam != null && !pageParam.trim().isEmpty()) {
            redirectUrl.append("page=").append(URLEncoder.encode(pageParam.trim(), StandardCharsets.UTF_8)).append("&");
        }

        // Khóa / Mở bảo trì nhanh một ghế
        if ("toggleMaintenance".equalsIgnoreCase(action)) {
            String seatIdParam = request.getParameter("seatId");
            try {
                int seatId = Integer.parseInt(seatIdParam.trim());
                CinemaService.Result<String> result = cinemaService.toggleSeatMaintenance(hallId, seatId);
                if (result.isSuccess()) {
                    redirectUrl.append("success=maintenance_toggled");
                } else if (result.getStatus() == CinemaService.Status.BOOKED_SEAT) {
                    redirectUrl.append("error=booked_seat&msg=").append(URLEncoder.encode(result.getMessage(), StandardCharsets.UTF_8));
                } else {
                    redirectUrl.append("error=failed");
                }
            } catch (Exception e) {
                redirectUrl.append("error=invalid_id");
            }
            response.sendRedirect(redirectUrl.toString());
            return;
        }

        String seatDataJson = request.getParameter("seatData");
        boolean isRegenerating = "true".equalsIgnoreCase(request.getParameter("isRegenerating"));

        if (seatDataJson == null || seatDataJson.trim().isEmpty()) {
            redirectUrl.append("error=empty_data");
            response.sendRedirect(redirectUrl.toString());
            return;
        }

        try {
            Type listType = new TypeToken<List<SeatConfigDTO>>() {}.getType();
            List<SeatConfigDTO> seatList = gson.fromJson(seatDataJson, listType);

            CinemaService.Result<Void> result = cinemaService.saveSeatLayout(hallId, seatList, isRegenerating);
            if (result.isSuccess()) {
                redirectUrl.append("success=saved");
            } else if (result.getStatus() == CinemaService.Status.CANNOT_REGENERATE) {
                redirectUrl.append("error=cannot_regenerate");
            } else if (result.getStatus() == CinemaService.Status.BOOKED_SEAT) {
                redirectUrl.append("error=booked_seat&msg=").append(URLEncoder.encode(result.getMessage(), StandardCharsets.UTF_8));
            } else if (result.getStatus() == CinemaService.Status.EMPTY_DATA) {
                redirectUrl.append("error=empty_data");
            } else {
                redirectUrl.append("error=failed");
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectUrl.append("error=json_error");
        }

        response.sendRedirect(redirectUrl.toString());
    }
}
