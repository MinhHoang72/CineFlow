package service;

import dao.CentreDAO;
import dao.HallDAO;
import dao.MovieDAO;
import dao.ShowtimeDAO;
import dao.UserDAO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import model.entity.Centre;
import model.entity.Showtime;
import model.entity.User;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service: Xử lý toàn bộ logic nghiệp vụ về Lịch chiếu (Showtime) và Ngữ cảnh Cụm rạp (Cinema Context).
 * Nhiệm vụ:
 * - Xác định cụm rạp đang thao tác theo quyền hạn (Admin / Manager).
 * - Kiểm tra tính sẵn sàng của phòng chiếu và quyền phân phối phim.
 * - Kiểm tra xung đột thời gian chiếu (Schedule Conflict) kèm thời gian đệm dọn phòng (Cleaning Buffer).
 * - Thực hiện thêm, cập nhật, hủy suất chiếu.
 */
public class ShowtimeService {

    private static final ShowtimeDAO showtimeDAO = new ShowtimeDAO();
    private static final MovieDAO movieDAO = new MovieDAO();
    private static final HallDAO hallDAO = new HallDAO();
    private static final CentreDAO centreDAO = new CentreDAO();
    private static final UserDAO userDAO = new UserDAO();

    public ShowtimeService() {
    }

    /**
     * Lấy ID người dùng hiện tại từ phiên đăng nhập.
     *
     * @param request yêu cầu HTTP
     * @return ID người dùng
     * @throws IllegalStateException nếu chưa đăng nhập
     */
    public static int requireUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object id = session == null ? null : session.getAttribute("userId");
        if (id instanceof Number) {
            return ((Number) id).intValue();
        }
        Object user = session == null ? null : session.getAttribute("user");
        if (user instanceof User) {
            return ((User) user).getUserId();
        }
        throw new IllegalStateException("Vui lòng đăng nhập để thực hiện thao tác này.");
    }

    /**
     * Xác định ID cụm rạp thao tác của người dùng.
     * - Admin: Cho phép chọn qua request param/session hoặc mặc định lấy rạp đầu tiên đang hoạt động.
     * - Manager: Lấy rạp trực thuộc được phân công trong bảng CentreManagerAssignments.
     *
     * @param request yêu cầu HTTP
     * @return ID cụm rạp
     */
    public static int requireCentreId(HttpServletRequest request) {
        int userId = requireUserId(request);
        String[] roleAndCentre = userDAO.getUserRoleAndAssignedCentre(userId);
        if (roleAndCentre != null) {
            String role = roleAndCentre[0];
            if ("ADMIN".equalsIgnoreCase(role)) {
                List<Centre> centres;
                try {
                    centres = centreDAO.getActiveCentres();
                } catch (SQLException e) {
                    throw new IllegalStateException("Không thể tải danh sách cụm rạp hoạt động.", e);
                }
                Integer centre = parseCentre(request.getParameter("centreId"));
                if (centre == null) {
                    centre = parseCentre(request.getParameter("branchId"));
                }
                if (centre == null && request.getSession().getAttribute("centreId") != null) {
                    centre = parseCentre(request.getSession().getAttribute("centreId").toString());
                }
                final Integer chosen = centre;
                if (chosen == null || centres.stream().noneMatch(c -> c.getCentreId() == chosen)) {
                    centre = centres.isEmpty() ? null : centres.get(0).getCentreId();
                }
                if (centre != null) {
                    request.getSession().setAttribute("centreId", centre);
                    request.setAttribute("cinemaCentres", centres);
                    request.setAttribute("cinemaCentreId", centre);
                    request.setAttribute("fe04Centres", centres);
                    request.setAttribute("fe04CentreId", centre);
                    return centre;
                }
            } else {
                String centreStr = roleAndCentre[1];
                if (centreStr != null) {
                    try {
                        int centre = Integer.parseInt(centreStr);
                        if (centre > 0) {
                            return centre;
                        }
                    } catch (NumberFormatException ignored) { }
                }
            }
        }
        throw new IllegalStateException("Vui lòng chọn cụm rạp hoặc cấu hình phân công rạp hoạt động.");
    }

    /**
     * Kiểm tra phòng chiếu có khả dụng để lên lịch không.
     */
    public boolean isHallAvailable(int hallId) {
        return showtimeDAO.isHallAvailable(hallId);
    }

    /**
     * Kiểm tra phim có được phân phối tới rạp chứa phòng chiếu này không.
     */
    public boolean isMovieAvailableForHall(int movieId, int hallId) {
        return showtimeDAO.isMovieAvailableForHall(movieId, hallId);
    }

    /**
     * Lấy thời lượng phim (phút).
     */
    public int getMovieDuration(int movieId) {
        return movieDAO.getMovieDuration(movieId);
    }

    /**
     * Kiểm tra trùng lặp thời gian chiếu với các suất chiếu hiện có.
     */
    public boolean hasScheduleConflict(int hallId, LocalDateTime startTime, LocalDateTime endTime,
                                       int cleaningBufferMinutes, Integer excludeShowtimeId) {
        return showtimeDAO.hasScheduleConflict(hallId, startTime, endTime, cleaningBufferMinutes, excludeShowtimeId);
    }

    /**
     * Tạo mới một suất chiếu sau khi đã xác thực toàn bộ điều kiện nghiệp vụ.
     */
    public boolean createShowtime(int movieId, int hallId, LocalDateTime startTime, int cleaningBuffer, int managerId) {
        int duration = getMovieDuration(movieId);
        if (duration <= 0) {
            throw new IllegalArgumentException("Thời lượng phim không hợp lệ.");
        }
        if (!isHallAvailable(hallId)) {
            throw new IllegalStateException("Phòng chiếu đã chọn không khả dụng.");
        }
        if (!isMovieAvailableForHall(movieId, hallId)) {
            throw new IllegalStateException("Phim chưa được phân phối tới cụm rạp này.");
        }
        LocalDateTime endTime = startTime.plusMinutes(duration);
        if (hasScheduleConflict(hallId, startTime, endTime, cleaningBuffer, null)) {
            throw new IllegalStateException("Trùng lịch chiếu hoặc thời gian dọn phòng với suất chiếu khác.");
        }

        Showtime showtime = new Showtime();
        showtime.setMovieId(movieId);
        showtime.setHallId(hallId);
        showtime.setStartTime(startTime);
        showtime.setEndTime(endTime);
        showtime.setCleaningBufferMinutes(cleaningBuffer);
        showtime.setStatus("SCHEDULED");
        showtime.setCreatedBy(managerId);

        return showtimeDAO.insertShowtime(showtime);
    }

    /**
     * Hủy suất chiếu.
     */
    public boolean cancelShowtime(int showtimeId) {
        return showtimeDAO.cancelShowtime(showtimeId);
    }

    /**
     * Cập nhật thông tin suất chiếu.
     */
    public boolean updateShowtime(Showtime showtime) {
        return showtimeDAO.updateShowtime(showtime);
    }

    private static Integer parseCentre(String value) {
        if (value == null) {
            return null;
        }
        try {
            int id = Integer.parseInt(value.trim());
            return id > 0 ? id : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
