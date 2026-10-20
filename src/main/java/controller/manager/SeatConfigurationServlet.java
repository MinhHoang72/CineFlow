package controller.manager;

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
import model.entity.Centre;
import model.entity.Hall;
import model.dto.pricing.SeatConfigDTO;
import model.entity.User;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controller xử lý phân hệ Cấu hình Sơ đồ & Trạng thái Ghế (UC-FE-03.3: Configure Seat Layout & Maintenance).
 * Primary Actor: Centre Manager / Technical Staff (hoặc Admin).
 *
 * Nghiệp vụ:
 * - BR-HS-02: Mỗi ghế thuộc về đúng một phòng chiếu. Ghế ở trạng thái Under Maintenance không thể đặt vé.
 * - Trường hợp A: Khởi tạo sơ đồ lưới ghế (Generate Layout Grid: Hàng, Cột, Phân hạng Standard, VIP, Sweetbox, Lối đi).
 * - Trường hợp B: Vận hành & Khóa ghế bảo trì hàng ngày (Set Maintenance Status).
 * - Alt 1: Chặn khởi tạo lại kích thước lưới nếu phòng đã có lịch chiếu / đặt vé mở bán.
 * - Alt 2: Chặn khóa bảo trì nếu ghế đã được khách đặt trước trong một suất chiếu sắp tới.
 * - Tự động đồng bộ lại tổng sức chứa khả dụng (Capacity) của phòng chiếu.
 */
@WebServlet(name = "SeatConfigurationServlet", urlPatterns = {"/manager/seat-config", "/admin/seat-config"})
public class SeatConfigurationServlet extends HttpServlet {

    private final SeatDAO seatDAO = new SeatDAO();
    private final HallDAO hallDAO = new HallDAO();
    private final CentreDAO centreDAO = new CentreDAO();
    private final Gson gson = new Gson();

    private boolean isManagerOrAdmin(HttpSession session) {
        if (session == null) {
            return false;
        }
        Object roleObj = session.getAttribute("role");
        if (roleObj instanceof String) {
            String role = (String) roleObj;
            if ("MANAGER".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) {
                return true;
            }
        }
        Object userObj = session.getAttribute("user");
        if (userObj instanceof User) {
            String role = ((User) userObj).getRole();
            return role != null && ("MANAGER".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role));
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

        if (!isManagerOrAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập cấu hình sơ đồ ghế.");
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

        Hall hall = hallDAO.getHallById(hallId);
        if (hall == null) {
            response.sendRedirect(request.getContextPath() + "/manager/halls?error=invalid_hall");
            return;
        }

        User user = getCurrentUser(session);
        boolean isAdmin = user != null && "ADMIN".equalsIgnoreCase(user.getRole());

        // Nếu là Manager, bắt buộc phòng chiếu phải thuộc Cụm rạp mà Manager phụ trách
        if (!isAdmin && (user == null || user.getCentreId() == null || user.getCentreId() != hall.getCentreId())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn chỉ được cấu hình sơ đồ ghế cho Cụm rạp mình phụ trách.");
            return;
        }

        Centre centre = centreDAO.getCentreById(hall.getCentreId());

        // Kiểm tra xem phòng chiếu đã phát sinh lịch chiếu tương lai có vé đặt hay chưa (Alt 1)
        boolean hasBookedShowtimes = seatDAO.hasFutureBookedShowtimes(hallId);

        // Lấy danh sách cấu hình ghế hiện có trong DB
        List<SeatConfigDTO> existingSeats = seatDAO.getSeatConfigsByHall(hallId);
        String seatsJson = gson.toJson(existingSeats);

        // Đọc các tham số tìm kiếm & phân trang cho bảng danh sách ghế
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
        int pageSize = 12; // Hiển thị 12 ghế / trang
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

        int totalFilteredSeats = seatDAO.countSeatsByHall(hallId, search, seatType, seatStatus);
        int totalPages = (int) Math.ceil((double) totalFilteredSeats / pageSize);
        if (totalPages < 1) {
            totalPages = 1;
        }
        if (page > totalPages) {
            page = totalPages;
        }

        List<SeatConfigDTO> pagedSeats = seatDAO.getSeatsByHallPaged(hallId, search, seatType, seatStatus, page, pageSize);

        request.setAttribute("hall", hall);
        request.setAttribute("centre", centre);
        request.setAttribute("existingSeats", existingSeats);
        request.setAttribute("seatsJson", seatsJson);
        request.setAttribute("hasBookedShowtimes", hasBookedShowtimes);
        request.setAttribute("isAdmin", isAdmin);

        // Thuộc tính phân trang và bộ lọc danh sách ghế
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

        request.getRequestDispatcher("/WEB-INF/views/manager/seatConfig.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (!isManagerOrAdmin(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền thực hiện thao tác này.");
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

        Hall hall = hallDAO.getHallById(hallId);
        if (hall == null) {
            response.sendRedirect(request.getContextPath() + "/manager/halls?error=invalid_hall");
            return;
        }

        User user = getCurrentUser(session);
        boolean isAdmin = user != null && "ADMIN".equalsIgnoreCase(user.getRole());

        if (!isAdmin && (user == null || user.getCentreId() == null || user.getCentreId() != hall.getCentreId())) {
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

        // TÁC VỤ B: KHÓA / MỞ BẢO TRÌ NHANH MỘT GHẾ CỤ THỂ TỪ BẢNG
        if ("toggleMaintenance".equalsIgnoreCase(action)) {
            String seatIdParam = request.getParameter("seatId");
            try {
                int seatId = Integer.parseInt(seatIdParam.trim());
                String result = seatDAO.toggleSeatMaintenance(seatId, hallId);
                if ("SUCCESS".equals(result)) {
                    redirectUrl.append("success=maintenance_toggled");
                } else if (result != null && result.startsWith("HAS_FUTURE_BOOKINGS:")) {
                    String bookedTime = result.substring("HAS_FUTURE_BOOKINGS:".length());
                    String msg = "CẢNH BÁO: Ghế này đã được đặt vé ở suất chiếu [" + bookedTime + "]. Vui lòng đổi chỗ ngồi thủ công cho khách hàng trước khi khóa bảo trì.";
                    redirectUrl.append("error=booked_seat&msg=").append(URLEncoder.encode(msg, StandardCharsets.UTF_8));
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

        // Alt 1: Phòng chiếu đã có lịch chiếu tương lai mở bán vé nhưng cố tình tái cấu trúc lưới ghế
        if (isRegenerating && seatDAO.hasFutureBookedShowtimes(hallId)) {
            redirectUrl.append("error=cannot_regenerate");
            response.sendRedirect(redirectUrl.toString());
            return;
        }

        if (seatDataJson == null || seatDataJson.trim().isEmpty()) {
            redirectUrl.append("error=empty_data");
            response.sendRedirect(redirectUrl.toString());
            return;
        }

        try {
            Type listType = new TypeToken<List<SeatConfigDTO>>() {}.getType();
            List<SeatConfigDTO> seatList = gson.fromJson(seatDataJson, listType);

            if (seatList == null || seatList.isEmpty()) {
                redirectUrl.append("error=empty_data");
                response.sendRedirect(redirectUrl.toString());
                return;
            }

            // Alt 2: Kiểm tra các ghế bị khóa bảo trì (MAINTENANCE) xem đã có khách đặt trước ở suất chiếu tương lai hay không
            for (SeatConfigDTO s : seatList) {
                if ("MAINTENANCE".equalsIgnoreCase(s.getStatus())) {
                    String bookedShowtime = seatDAO.checkFutureBookingForSeat(hallId, s.getSeatRow(), s.getSeatNumber());
                    if (bookedShowtime != null) {
                        String seatLabel = s.getSeatRow() + s.getSeatNumber();
                        String errorMsg = "CẢNH BÁO: Ghế " + seatLabel + " đã được đặt vé ở suất chiếu [" + bookedShowtime + "]. Vui lòng đổi chỗ ngồi thủ công cho khách hàng trước khi khóa bảo trì.";
                        redirectUrl.append("error=booked_seat&msg=").append(URLEncoder.encode(errorMsg, StandardCharsets.UTF_8));
                        response.sendRedirect(redirectUrl.toString());
                        return;
                    }
                }
            }

            // Thực hiện lưu cấu hình ghế và tự động đồng bộ lại Capacity của phòng chiếu
            boolean ok = seatDAO.saveSeatLayout(hallId, seatList, isRegenerating);
            if (ok) {
                redirectUrl.append("success=saved");
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
