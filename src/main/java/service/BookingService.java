package service;

import dao.BookingDAO;
import dao.BookingDAO.Amounts;
import dao.FnbOrderDAO;
import dao.MovieDAO;
import dao.PaymentDAO;
import dao.SeatDAO;
import dao.ShowtimeDAO;
import dao.ShowtimeDAO.CityOption;
import model.entity.Booking;
import model.entity.Centre;
import model.entity.Movie;
import model.entity.Seat;
import model.entity.Showtime;
import model.entity.User;
import utils.CodeUtil;
import utils.DBContext;
import utils.DbUtil;
import utils.WebUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Service: Xử lý logic nghiệp vụ Đặt vé & Giữ ghế (Booking & Seat Reservation).
 * Nhiệm vụ:
 * - Chọn thành phố -> rạp -> phim -> ngày -> suất chiếu còn mở bán.
 * - Tính giá từng ghế (giá suất chiếu + phụ thu loại ghế) và sơ đồ ghế.
 * - Giữ ghế 5 phút (tối đa 8 ghế), chống hai người giữ cùng một ghế, tạo đơn PENDING.
 * - Nhả ghế / hết hạn đơn khi quá thời gian giữ ghế, huỷ đơn khi khách hoặc nhân viên yêu cầu.
 * - Tính tổng tiền đơn: tiền vé - giảm giá + bắp nước.
 * DAO chỉ đọc / ghi dữ liệu; service mở transaction và quyết định nghiệp vụ.
 */
public class BookingService {

    public static final int HOLD_MINUTES = 5;
    public static final int MAX_SEATS_PER_BOOKING = 8;

    /** Lý do không giữ được ghế, hiển thị cho người dùng. */
    public static class SeatUnavailableException extends Exception {
        public SeatUnavailableException(String message) { super(message); }
    }

    /** Sơ đồ ghế theo hàng và bảng giá theo loại ghế. */
    public record SeatLayout(Map<String, List<Seat>> rows, Map<String, BigDecimal> priceByType) {}

    /**
     * Kết quả bước chọn suất chiếu.
     * @param movieUnavailable phim được chọn hiện không có suất mở bán (đã bỏ chọn phim)
     */
    public record ShowtimeSelection(List<CityOption> cities, String city, List<Centre> centres, Integer centreId,
            Integer movieId, boolean movieUnavailable, List<Movie> movies, List<LocalDate> dates, LocalDate date,
            List<Showtime> showtimes) {}

    private final BookingDAO bookingDAO;
    private final SeatDAO seatDAO;
    private final ShowtimeDAO showtimeDAO;
    private final MovieDAO movieDAO;
    private final FnbOrderDAO fnbOrderDAO;
    private final PaymentDAO paymentDAO;

    public BookingService() {
        this(new BookingDAO(), new SeatDAO(), new ShowtimeDAO(), new MovieDAO(), new FnbOrderDAO(), new PaymentDAO());
    }

    public BookingService(BookingDAO bookingDAO, SeatDAO seatDAO, ShowtimeDAO showtimeDAO, MovieDAO movieDAO,
            FnbOrderDAO fnbOrderDAO, PaymentDAO paymentDAO) {
        this.bookingDAO = bookingDAO;
        this.seatDAO = seatDAO;
        this.showtimeDAO = showtimeDAO;
        this.movieDAO = movieDAO;
        this.fnbOrderDAO = fnbOrderDAO;
        this.paymentDAO = paymentDAO;
    }

    // ---------------------------------------------------------------- chọn suất chiếu

    /**
     * Bước 1 đặt vé online: lọc thành phố, rạp, phim, ngày, suất theo lựa chọn hiện tại và tự chọn sẵn
     * khi chỉ có một lựa chọn. Giá trị không hợp lệ (rạp không chiếu phim, ngày không có suất...) bị bỏ qua.
     */
    public ShowtimeSelection selectShowtime(String city, Integer centreId, Integer movieId, LocalDate date)
            throws SQLException {
        if (city != null && city.isBlank()) city = null;

        // Đã chọn phim (từ trang chủ / chi tiết phim): thành phố và rạp chỉ hiện nơi chiếu phim đó
        List<CityOption> cities = movieId == null ? List.of() : showtimeDAO.getCities(movieId);
        boolean movieUnavailable = movieId != null && cities.isEmpty();
        if (movieUnavailable) movieId = null;
        if (movieId == null) cities = showtimeDAO.getCities(null);

        // Chỉ có phim: chọn sẵn rạp có suất sớm nhất của phim
        if (movieId != null && city == null && centreId == null) centreId = showtimeDAO.findFirstCentreForMovie(movieId);
        // Link cũ chỉ có centreId: suy ra thành phố của rạp
        if (city == null && centreId != null) city = showtimeDAO.getCityOfCentre(centreId);
        final String chosenCity = city;
        if (city != null && cities.stream().noneMatch(c -> c.name().equals(chosenCity))) city = null;
        if (city == null && cities.size() == 1) city = cities.get(0).name();

        List<Centre> centres = city == null ? List.of() : showtimeDAO.getCentresInCity(city, movieId);
        final Integer chosenCentre = centreId;
        if (centreId != null && centres.stream().noneMatch(c -> c.getCentreId() == chosenCentre)) centreId = null;
        if (centreId == null && centres.size() == 1) centreId = centres.get(0).getCentreId();

        List<Movie> movies = centreId == null ? null : movieDAO.getMoviesShowingAt(centreId);
        List<LocalDate> dates = null;
        List<Showtime> showtimes = null;
        if (centreId != null && movieId != null) {
            dates = showtimeDAO.getShowDates(centreId, movieId);
            if ((date == null || !dates.contains(date)) && !dates.isEmpty()) date = dates.get(0);
            if (date != null) showtimes = showtimeDAO.getShowtimes(centreId, movieId, date);
        } else {
            date = null;
        }
        return new ShowtimeSelection(cities, city, centres, centreId, movieId, movieUnavailable, movies, dates, date,
                showtimes);
    }

    /** Suất chiếu còn mở bán, null nếu không có. */
    public Showtime getBookableShowtime(int showtimeId) throws SQLException {
        return showtimeDAO.getBookableById(showtimeId);
    }

    /** Suất chiếu còn mở bán thuộc rạp, null nếu không có / rạp khác (bán tại quầy). */
    public Showtime getBookableShowtimeAtCentre(int showtimeId, int centreId) throws SQLException {
        Showtime showtime = showtimeDAO.getBookableById(showtimeId);
        return showtime == null || showtime.getCentreId() != centreId ? null : showtime;
    }

    /** Các ngày còn suất chiếu tại rạp (bán tại quầy). */
    public List<LocalDate> getShowDatesAtCentre(int centreId) throws SQLException {
        return showtimeDAO.getShowDatesAtCentre(centreId);
    }

    /** Suất chiếu trong ngày tại rạp, nhóm theo tên phim (bán tại quầy). */
    public Map<String, List<Showtime>> getShowtimesByMovieAtCentre(int centreId, LocalDate date) throws SQLException {
        Map<String, List<Showtime>> byMovie = new LinkedHashMap<>();
        for (Showtime s : showtimeDAO.getShowtimesAtCentre(centreId, date)) {
            byMovie.computeIfAbsent(s.getMovieTitle(), k -> new ArrayList<>()).add(s);
        }
        return byMovie;
    }

    // ---------------------------------------------------------------- sơ đồ ghế, giá ghế

    /** Sơ đồ ghế theo hàng kèm giá từng loại ghế. */
    public SeatLayout getSeatLayout(Showtime showtime) throws SQLException {
        List<Seat> seats;
        try (Connection conn = DBContext.getConnection()) {
            seats = pricedSeats(conn, showtime);
        }
        Map<String, List<Seat>> rows = new LinkedHashMap<>();
        Map<String, BigDecimal> priceByType = new TreeMap<>();
        for (Seat seat : seats) {
            rows.computeIfAbsent(seat.getRow(), k -> new ArrayList<>()).add(seat);
            priceByType.putIfAbsent(seat.getType(), seat.getPrice());
        }
        return new SeatLayout(rows, priceByType);
    }

    /**
     * Ghế của suất chiếu kèm giá: giá ghế thường của suất (giá gốc rạp + phụ thu khung giờ + phụ thu phòng,
     * tính trong ShowtimeDAO) cộng phụ thu loại ghế (VIP, Sweetbox) đang hiệu lực tại giờ chiếu.
     */
    private List<Seat> pricedSeats(Connection conn, Showtime showtime) throws SQLException {
        Map<String, BigDecimal> surcharges = seatDAO.getSeatTypeSurcharges(conn, showtime.getStartTime());
        List<Seat> priced = new ArrayList<>();
        for (Seat s : seatDAO.getSeatMap(conn, showtime)) {
            BigDecimal price = showtime.getBasePrice().add(surcharges.getOrDefault(s.getType(), BigDecimal.ZERO));
            priced.add(new Seat(s.getId(), s.getRow(), s.getNumber(), s.getType(), s.isAvailable(), s.isTaken(), price));
        }
        return priced;
    }

    // ---------------------------------------------------------------- giữ ghế

    /**
     * Giữ ghế trực tuyến cho khách hàng. Đơn PENDING khác của khách cho cùng suất bị huỷ trước.
     * @return BookingID của đơn PENDING vừa tạo
     * @throws SeatUnavailableException ghế không hợp lệ / đã có người giữ, suất ngừng bán, chọn sai số ghế
     */
    public int holdSeatsForOnlineCustomer(int userId, int showtimeId, List<Integer> seatIds)
            throws SeatUnavailableException, SQLException {
        return hold(userId, "ONLINE", showtimeId, seatIds);
    }

    /** Giữ ghế tại quầy cho khách vãng lai: đơn COUNTER, không gắn tài khoản khách. */
    public int holdSeatsForCounter(int showtimeId, List<Integer> seatIds) throws SeatUnavailableException, SQLException {
        return hold(null, "COUNTER", showtimeId, seatIds);
    }

    private int hold(Integer userId, String bookingType, int showtimeId, List<Integer> seatIds)
            throws SeatUnavailableException, SQLException {
        List<Integer> ids = new ArrayList<>(new LinkedHashSet<>(seatIds));
        if (ids.isEmpty()) throw new SeatUnavailableException("Bạn chưa chọn ghế nào.");
        if (ids.size() > MAX_SEATS_PER_BOOKING) {
            throw new SeatUnavailableException("Mỗi lần chỉ được đặt tối đa " + MAX_SEATS_PER_BOOKING + " ghế.");
        }
        try {
            return DbUtil.inTransaction(conn -> {
                releaseExpiredHolds(conn);
                if (userId != null) {
                    cancelPending(conn, bookingDAO.findPendingIdsOfUserForShowtime(conn, userId, showtimeId));
                }

                Showtime showtime = showtimeDAO.getBookableById(conn, showtimeId);
                if (showtime == null) throw new SeatUnavailableException("Suất chiếu không còn mở bán.");

                // Đọc lại sơ đồ ghế mới nhất trong transaction, kiểm tra từng ghế và cộng tiền vé
                Map<Integer, Seat> seatMap = new HashMap<>();
                for (Seat s : pricedSeats(conn, showtime)) seatMap.put(s.getId(), s);
                List<Seat> chosen = new ArrayList<>();
                BigDecimal subTotal = BigDecimal.ZERO;
                for (Integer id : ids) {
                    Seat seat = seatMap.get(id);
                    if (seat == null || !seat.isAvailable()) {
                        throw new SeatUnavailableException("Ghế bạn chọn không hợp lệ cho suất chiếu này.");
                    }
                    if (seat.isTaken()) throw new SeatUnavailableException("Ghế " + seat.getLabel() + " vừa có người khác chọn.");
                    chosen.add(seat);
                    subTotal = subTotal.add(seat.getPrice());
                }

                int bookingId = bookingDAO.insertBooking(conn, CodeUtil.randomCode("CF", 10), userId, bookingType,
                        showtimeId, subTotal);
                // Hạn giữ ghế theo giờ SQL Server (cùng đồng hồ với các câu lệnh kiểm tra HoldUntil)
                Timestamp holdUntil = bookingDAO.getDbTimePlusMinutes(conn, HOLD_MINUTES);
                bookingDAO.insertHeldSeats(conn, bookingId, showtimeId, chosen, holdUntil);
                return bookingId;
            });
        } catch (SQLException e) {
            // Unique index UX_BookingSeats_ActiveSeat: người khác vừa giữ cùng ghế trước khi transaction này ghi
            if (DbUtil.isDuplicateKey(e)) {
                throw new SeatUnavailableException("Một trong các ghế bạn chọn vừa có người khác đặt. Vui lòng chọn lại.");
            }
            throw e;
        }
    }

    // ---------------------------------------------------------------- hết hạn, huỷ đơn

    /** Đơn quá thời gian giữ ghế -> EXPIRED, nhả ghế, huỷ bắp nước chờ, giao dịch đang chờ -> TIMEOUT. */
    public void releaseExpiredHolds() throws SQLException {
        DbUtil.inTransaction(conn -> {
            releaseExpiredHolds(conn);
            return null;
        });
    }

    void releaseExpiredHolds(Connection conn) throws SQLException {
        List<Integer> expired = bookingDAO.findExpiredPendingIds(conn);
        paymentDAO.updatePendingForBookings(conn, expired, "TIMEOUT");
        fnbOrderDAO.cancelPendingForBookings(conn, expired);
        bookingDAO.updatePendingStatus(conn, expired, "EXPIRED");
        bookingDAO.releaseOverdueSeats(conn);
    }

    /** Khách tự huỷ đơn đang chờ thanh toán của mình. @return false nếu đơn không còn chờ thanh toán */
    public boolean cancelPendingBooking(int bookingId, int userId) throws SQLException {
        return DbUtil.inTransaction(conn -> cancelPending(conn, bookingDAO.findPendingIdOfUser(conn, bookingId, userId)) > 0);
    }

    /** Nhân viên huỷ đơn tại quầy đang chờ thanh toán. */
    public boolean cancelCounterPendingBooking(int bookingId) throws SQLException {
        return DbUtil.inTransaction(conn -> cancelPending(conn, bookingDAO.findPendingCounterId(conn, bookingId)) > 0);
    }

    /** Huỷ các đơn PENDING: giao dịch chờ -> CANCELLED, nhả ghế, huỷ bắp nước chờ. @return số đơn đã huỷ */
    private int cancelPending(Connection conn, List<Integer> bookingIds) throws SQLException {
        if (bookingIds.isEmpty()) return 0;
        paymentDAO.updatePendingForBookings(conn, bookingIds, "CANCELLED");
        bookingDAO.cancelHeldSeats(conn, bookingIds);
        fnbOrderDAO.cancelPendingForBookings(conn, bookingIds);
        return bookingDAO.updatePendingStatus(conn, bookingIds, "CANCELLED");
    }

    // ---------------------------------------------------------------- tổng tiền

    /**
     * Tổng tiền đơn đang chờ thanh toán = max(tiền vé - giảm giá, 0) + bắp nước chưa huỷ.
     * Gọi sau mỗi lần đổi bắp nước / voucher.
     */
    public void recalculateTotal(Connection conn, int bookingId) throws SQLException {
        Amounts amounts = bookingDAO.getAmounts(conn, bookingId);
        if (amounts == null || !"PENDING".equals(amounts.status())) return;
        BigDecimal tickets = amounts.subTotal().subtract(amounts.discountAmount()).max(BigDecimal.ZERO);
        bookingDAO.updateTotal(conn, bookingId, tickets.add(fnbOrderDAO.sumActiveTotalForBooking(conn, bookingId)));
    }

    /** Đơn đổi số tiền: giao dịch đang chờ (số tiền cũ) bị huỷ, khách thanh toán lại sẽ tạo giao dịch mới. */
    public void cancelPendingPayments(Connection conn, int bookingId) throws SQLException {
        paymentDAO.updatePendingForBookings(conn, List.of(bookingId), "CANCELLED");
    }

    // ---------------------------------------------------------------- xem đơn

    /** Đơn kèm suất chiếu, ghế và bắp nước, null nếu không có. */
    public Booking getBooking(int bookingId) throws SQLException {
        Booking booking = bookingDAO.getBooking(bookingId);
        if (booking != null) booking.setFnbLines(fnbOrderDAO.getLines(bookingId));
        return booking;
    }

    /** Đơn của chính khách hàng, null nếu không có / của người khác. */
    public Booking getOwnBooking(int bookingId, int userId) throws SQLException {
        Booking booking = getBooking(bookingId);
        return booking == null || booking.getUserId() == null || booking.getUserId() != userId ? null : booking;
    }

    /** Đơn tại quầy thuộc rạp của nhân viên, null nếu không hợp lệ. */
    public Booking getCounterBooking(int bookingId, int centreId) throws SQLException {
        Booking booking = getBooking(bookingId);
        return booking == null || !booking.isCounter() || booking.getShowtime().getCentreId() != centreId ? null : booking;
    }

    /**
     * Quyền xem chi tiết đơn: khách xem đơn của mình; nhân viên bán vé xem đơn của rạp mình;
     * MANAGER xem đơn của rạp mình (chưa gán rạp: tất cả); ADMIN xem tất cả.
     */
    public boolean canView(User user, Booking booking) {
        if (booking.getUserId() != null && booking.getUserId() == user.getUserId()) return true;
        int centreId = booking.getShowtime().getCentreId();
        if ("ADMIN".equals(user.getRole())) return true;
        if ("MANAGER".equals(user.getRole())) return user.getCentreId() == null || user.getCentreId() == centreId;
        if (WebUtil.isSalesStaff(user)) return user.getCentreId() != null && user.getCentreId() == centreId;
        return false;
    }
}
