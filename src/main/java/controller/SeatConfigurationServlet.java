package controller;

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
import model.Centre;
import model.Hall;
import model.SeatConfigDTO;
import model.User;

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
            String redirectUrl = request.getContextPath() + "/auth/login.jsp?redirect="
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

        request.setAttribute("hall", hall);
        request.setAttribute("centre", centre);
        request.setAttribute("existingSeats", existingSeats);
        request.setAttribute("seatsJson", seatsJson);
        request.setAttribute("hasBookedShowtimes", hasBookedShowtimes);
        request.setAttribute("isAdmin", isAdmin);
        request.setAttribute("activeNav", "halls");
        request.setAttribute("pageTitle", "Cấu hình Sơ đồ ghế - " + hall.getHallName());

        request.getRequestDispatcher("/manager/seat-config.jsp").forward(request, response);
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

        String seatDataJson = request.getParameter("seatData");
        boolean isRegenerating = "true".equalsIgnoreCase(request.getParameter("isRegenerating"));

        StringBuilder redirectUrl = new StringBuilder(request.getContextPath() + "/manager/seat-config?hallId=" + hallId + "&");

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
