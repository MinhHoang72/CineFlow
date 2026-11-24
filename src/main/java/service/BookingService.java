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

    /**
     * Kết quả sơ đồ ghế: rows = ghế nhóm theo hàng (A, B, C...), priceByType = giá của từng loại ghế để hiện bảng giá.
     */
    public record SeatLayout(Map<String, List<Seat>> rows, Map<String, BigDecimal> priceByType) {}

    /**
     * Kết quả bước chọn suất gửi về servlet: danh sách và lựa chọn hiện tại của từng cấp
     * (thành phố, rạp, phim, ngày, suất). movieUnavailable = phim khách chọn hiện không còn suất mở bán (đã bỏ chọn phim).
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

    /** Khởi tạo với các DAO thật (dùng khi chạy ứng dụng). */
    public BookingService() {
        this(new BookingDAO(), new SeatDAO(), new ShowtimeDAO(), new MovieDAO(), new FnbOrderDAO(), new PaymentDAO());
    }

    /** Khởi tạo với DAO truyền vào từ ngoài (dùng khi viết test để thay DAO giả). */
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
     * Bước 1 đặt vé online: lọc thành phố -> rạp -> phim -> ngày -> suất theo các lựa chọn khách đã bấm.
     * Luồng:
     * 1. Lấy thành phố còn suất mở bán (nếu đã chọn phim thì chỉ nơi chiếu phim đó; phim hết suất thì bỏ chọn phim).
     * 2. Có phim mà chưa chọn rạp: tự chọn rạp có suất sớm nhất. Có rạp mà chưa có thành phố: suy ra thành phố của rạp.
     * 3. Lựa chọn không hợp lệ (ví dụ sửa URL) thì bỏ; chỉ có đúng một lựa chọn thì tự chọn luôn.
     * 4. Có thành phố -> lấy rạp; có rạp -> lấy phim; có rạp và phim -> lấy ngày (mặc định ngày đầu) và suất của ngày đó.
     * Gọi từ: BookingServlet.selectShowtime.
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

    /**
     * Lấy suất chiếu nếu còn mở bán (chưa chiếu, rạp và phòng hoạt động, phim còn phân phối...); không còn bán thì null.
     */
    public Showtime getBookableShowtime(int showtimeId) throws SQLException {
        return showtimeDAO.getBookableById(showtimeId);
    }

    /**
     * Như getBookableShowtime nhưng thêm điều kiện suất thuộc rạp của nhân viên (bán tại quầy); rạp khác thì null.
     */
    public Showtime getBookableShowtimeAtCentre(int showtimeId, int centreId) throws SQLException {
        Showtime showtime = showtimeDAO.getBookableById(showtimeId);
        return showtime == null || showtime.getCentreId() != centreId ? null : showtime;
    }

    /** Các ngày còn suất chiếu mở bán tại rạp (trang chọn suất tại quầy). */
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

    /**
     * Sơ đồ ghế của suất: lấy ghế kèm giá (pricedSeats), nhóm theo hàng và lập bảng giá theo loại ghế.
     * Gọi từ: trang chọn ghế online (BookingServlet.selectSeats) và tại quầy (CounterShowtimeServlet.seats).
     */
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
     * Ghế của suất kèm giá từng ghế.
     * Giá lấy từ bảng giá đã chốt của suất theo loại ghế (bảng ShowtimeSeatPrices, qua SeatDAO.getLockedPrices),
     * không tính lại theo bảng giá hiện tại. Suất chưa có giá cho một loại ghế trong phòng thì báo lỗi.
     * Ghế trả về có đánh dấu taken = đã có người giữ hoặc đã bán.
     */
    private List<Seat> pricedSeats(Connection conn, Showtime showtime) throws SQLException {
        Map<String, BigDecimal> prices = seatDAO.getLockedPrices(conn, showtime.getId());
        List<Seat> priced = new ArrayList<>();
        for (Seat s : seatDAO.getSeatMap(conn, showtime)) {
            BigDecimal price = prices.get(s.getType());
            if(price==null) throw new SQLException("Bảng giá của suất chiếu chưa có loại ghế này. Vui lòng liên hệ quản lý.");
            priced.add(new Seat(s.getId(), s.getRow(), s.getNumber(), s.getType(), s.isAvailable(), s.isTaken(), price));
        }
        return priced;
    }

    // ---------------------------------------------------------------- giữ ghế

    /**
     * Giữ ghế cho khách đặt online: gọi hold với userId của khách và loại đơn ONLINE.
     * @return BookingID của đơn PENDING vừa tạo
     * @throws SeatUnavailableException ghế không hợp lệ hoặc vừa có người giữ, suất ngừng bán, chọn sai số ghế, Sweetbox đặt lẻ
     */
    public int holdSeatsForOnlineCustomer(int userId, int showtimeId, List<Integer> seatIds)
            throws SeatUnavailableException, SQLException {
        return hold(userId, "ONLINE", showtimeId, seatIds);
    }

    /**
     * Giữ ghế tại quầy cho khách vãng lai: gọi hold với userId = null và loại đơn COUNTER (đơn không gắn tài khoản).
     */
    public int holdSeatsForCounter(int showtimeId, List<Integer> seatIds) throws SeatUnavailableException, SQLException {
        return hold(null, "COUNTER", showtimeId, seatIds);
    }

    /**
     * Ghép cặp ghế Sweetbox: trong mỗi hàng, các ghế Sweetbox xếp theo số ghế rồi ghép lần lượt E1-E2, E3-E4...
     * @return map SeatID -> ghế cùng cặp. Cách ghép giống đoạn JavaScript ở selectSeats.jsp.
     */
    private static Map<Integer, Seat> sweetboxPartners(Iterable<Seat> seats) {
        Map<String, List<Seat>> byRow = new TreeMap<>();
        for (Seat s : seats) {
            if ("SWEETBOX".equalsIgnoreCase(s.getType())) byRow.computeIfAbsent(s.getRow(), k -> new ArrayList<>()).add(s);
        }
        Map<Integer, Seat> partners = new HashMap<>();
        for (List<Seat> row : byRow.values()) {
            row.sort((a, b) -> Integer.compare(a.getNumber(), b.getNumber()));
            for (int i = 0; i + 1 < row.size(); i += 2) {
                partners.put(row.get(i).getId(), row.get(i + 1));
                partners.put(row.get(i + 1).getId(), row.get(i));
            }
        }
        return partners;
    }

    /**
     * Xử lý chính của việc giữ ghế, dùng chung cho online và tại quầy. Chạy trong một transaction:
     * 1. Bỏ ghế trùng; phải chọn từ 1 đến MAX_SEATS_PER_BOOKING (8) ghế.
     * 2. Dọn các đơn đã quá hạn giữ ghế (releaseExpiredHolds).
     * 3. Khách online: huỷ đơn đang chờ cũ của chính khách ở cùng suất.
     * 4. Suất phải còn mở bán.
     * 5. Đọc lại sơ đồ ghế mới nhất; từng ghế phải thuộc phòng, đang hoạt động, chưa ai giữ; cộng tiền vé (subTotal).
     * 6. Luật Sweetbox: chọn một ghế mà ghế cùng cặp còn trống nhưng không chọn thì từ chối.
     * 7. Ghi Bookings (PENDING) và BookingSeats (HELD), hạn giữ ghế = giờ SQL Server + HOLD_MINUTES (5 phút).
     * Hai người giữ cùng một ghế cùng lúc: unique index UX_BookingSeats_ActiveSeat báo trùng khoá, người sau bị từ chối.
     * @return BookingID vừa tạo
     */
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
                // Ghế Sweetbox là ghế đôi: chọn 1 ghế thì phải chọn cả ghế cùng cặp (trừ khi ghế kia không còn bán)
                Map<Integer, Seat> partners = sweetboxPartners(seatMap.values());
                for (Seat seat : chosen) {
                    Seat partner = partners.get(seat.getId());
                    if (partner != null && partner.isSelectable() && !ids.contains(partner.getId())) {
                        throw new SeatUnavailableException("Ghế Sweetbox phải đặt theo cặp: "
                                + seat.getLabel() + " và " + partner.getLabel() + ".");
                    }
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

    /** Dọn các đơn quá hạn giữ ghế (mở transaction riêng rồi gọi bản dùng chung connection bên dưới). */
    public void releaseExpiredHolds() throws SQLException {
        DbUtil.inTransaction(conn -> {
            releaseExpiredHolds(conn);
            return null;
        });
    }

    /**
     * Dọn các đơn quá hạn giữ ghế trong transaction có sẵn:
     * đơn PENDING quá hạn -> EXPIRED, giao dịch đang chờ -> TIMEOUT, bắp nước chờ -> CANCELLED, ghế HELD quá hạn -> RELEASED.
     * Không có bộ đếm giờ riêng: hàm chạy mỗi khi có người giữ ghế (hold) và khi mở trang theo dõi giao dịch.
     */
    void releaseExpiredHolds(Connection conn) throws SQLException {
        List<Integer> expired = bookingDAO.findExpiredPendingIds(conn);
        paymentDAO.updatePendingForBookings(conn, expired, "TIMEOUT");
        fnbOrderDAO.cancelPendingForBookings(conn, expired);
        bookingDAO.updatePendingStatus(conn, expired, "EXPIRED");
        bookingDAO.releaseOverdueSeats(conn);
    }

    /**
     * Chuyển các đơn CONFIRMED của suất đã chiếu xong sang COMPLETED. Được BookingCompletionJob gọi mỗi phút.
     * @return số đơn đã đổi
     */
    public int completeFinishedBookings() throws SQLException {
        return DbUtil.inTransaction(bookingDAO::completeFinishedBookings);
    }

    /**
     * Khách tự huỷ đơn đang chờ thanh toán của mình (chỉ đơn PENDING thuộc đúng khách).
     * @return false nếu đơn không còn chờ thanh toán
     */
    public boolean cancelPendingBooking(int bookingId, int userId) throws SQLException {
        return DbUtil.inTransaction(conn -> cancelPending(conn, bookingDAO.findPendingIdOfUser(conn, bookingId, userId)) > 0);
    }

    /** Nhân viên huỷ đơn tại quầy đang chờ thanh toán. */
    public boolean cancelCounterPendingBooking(int bookingId) throws SQLException {
        return DbUtil.inTransaction(conn -> cancelPending(conn, bookingDAO.findPendingCounterId(conn, bookingId)) > 0);
    }

    /**
     * Huỷ các đơn PENDING trong transaction có sẵn: giao dịch chờ -> CANCELLED, ghế đang giữ -> CANCELLED (trống lại),
     * bắp nước chờ -> CANCELLED, đơn -> CANCELLED.
     * @return số đơn đã huỷ
     */
    private int cancelPending(Connection conn, List<Integer> bookingIds) throws SQLException {
        if (bookingIds.isEmpty()) return 0;
        paymentDAO.updatePendingForBookings(conn, bookingIds, "CANCELLED");
        bookingDAO.cancelHeldSeats(conn, bookingIds);
        fnbOrderDAO.cancelPendingForBookings(conn, bookingIds);
        return bookingDAO.updatePendingStatus(conn, bookingIds, "CANCELLED");
    }

    // ---------------------------------------------------------------- tổng tiền

    /**
     * Tính lại tổng tiền đơn đang chờ: max(tiền vé - giảm giá, 0) + tiền bắp nước chưa huỷ. Voucher chỉ giảm tiền vé.
     * Gọi sau mỗi lần đổi bắp nước hoặc voucher.
     */
    public void recalculateTotal(Connection conn, int bookingId) throws SQLException {
        Amounts amounts = bookingDAO.getAmounts(conn, bookingId);
        if (amounts == null || !"PENDING".equals(amounts.status())) return;
        BigDecimal tickets = amounts.subTotal().subtract(amounts.discountAmount()).max(BigDecimal.ZERO);
        bookingDAO.updateTotal(conn, bookingId, tickets.add(fnbOrderDAO.sumActiveTotalForBooking(conn, bookingId)));
    }

    /**
     * Huỷ giao dịch đang chờ của đơn khi số tiền đơn thay đổi (giao dịch cũ mang số tiền cũ).
     * Khách bấm thanh toán lại sẽ tạo giao dịch mới đúng số tiền.
     */
    public void cancelPendingPayments(Connection conn, int bookingId) throws SQLException {
        paymentDAO.updatePendingForBookings(conn, List.of(bookingId), "CANCELLED");
    }

    // ---------------------------------------------------------------- xem đơn

    /**
     * Lấy đơn kèm suất chiếu, ghế và các dòng bắp nước; không có thì null. Không kiểm tra quyền (servlet tự kiểm tra bằng canView).
     */
    public Booking getBooking(int bookingId) throws SQLException {
        Booking booking = bookingDAO.getBooking(bookingId);
        if (booking != null) booking.setFnbLines(fnbOrderDAO.getLines(bookingId));
        return booking;
    }

    /** Lấy đơn chỉ khi đơn thuộc khách userId; không có hoặc của người khác thì null. */
    public Booking getOwnBooking(int bookingId, int userId) throws SQLException {
        Booking booking = getBooking(bookingId);
        return booking == null || booking.getUserId() == null || booking.getUserId() != userId ? null : booking;
    }

    /** Lấy đơn tại quầy (BookingType = COUNTER) thuộc rạp centreId; không hợp lệ thì null. */
    public Booking getCounterBooking(int bookingId, int centreId) throws SQLException {
        Booking booking = getBooking(bookingId);
        return booking == null || !booking.isCounter() || booking.getShowtime().getCentreId() != centreId ? null : booking;
    }

    /**
     * Quyền xem chi tiết đơn: khách xem đơn của mình; nhân viên bán vé xem đơn của rạp mình;
     * MANAGER xem đơn của rạp mình (chưa gán rạp thì xem tất cả); ADMIN xem mọi đơn.
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
