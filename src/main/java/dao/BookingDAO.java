package dao;

import model.entity.Booking;
import model.entity.Seat;
import model.entity.Showtime;
import utils.DBContext;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class BookingDAO {

    public static final int HOLD_MINUTES = 5;
    public static final int MAX_SEATS_PER_BOOKING = 8;

    private static final SecureRandom RANDOM = new SecureRandom();

    public static class SeatUnavailableException extends Exception {
        public SeatUnavailableException(String message) { super(message); }
    }

    /**
     * Giu ghe trong HOLD_MINUTES phut: tao Booking PENDING + BookingSeats HELD.
     * Neu nguoi dung dang co booking PENDING khac cho cung suat chieu thi huy booking do truoc.
     * @return BookingID vua tao
     */
    public int holdSeats(int userId, int showtimeId, List<Integer> seatIds)
            throws SQLException, SeatUnavailableException {
        return hold(userId, "ONLINE", showtimeId, seatIds);
    }

    /** Ban tai quay cho khach vang lai: booking COUNTER, khong gan tai khoan khach. */
    public int holdCounterSeats(int showtimeId, List<Integer> seatIds)
            throws SQLException, SeatUnavailableException {
        return hold(null, "COUNTER", showtimeId, seatIds);
    }

    private int hold(Integer userId, String bookingType, int showtimeId, List<Integer> seatIds)
            throws SQLException, SeatUnavailableException {
        List<Integer> ids = new ArrayList<>(new LinkedHashSet<>(seatIds));
        if (ids.isEmpty()) throw new SeatUnavailableException("Bạn chưa chọn ghế nào.");
        if (ids.size() > MAX_SEATS_PER_BOOKING) {
            throw new SeatUnavailableException("Mỗi lần chỉ được đặt tối đa " + MAX_SEATS_PER_BOOKING + " ghế.");
        }

        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                releaseExpiredHolds(conn);
                if (userId != null) cancelUserPendingForShowtime(conn, userId, showtimeId);

                Showtime showtime = ShowtimeDAO.getBookableById(conn, showtimeId);
                if (showtime == null) throw new SeatUnavailableException("Suất chiếu không còn mở bán.");

                Map<Integer, Seat> seatMap = new HashMap<>();
                for (Seat s : SeatDAO.getSeatMap(conn, showtime)) seatMap.put(s.getId(), s);

                BigDecimal total = BigDecimal.ZERO;
                for (Integer id : ids) {
                    Seat seat = seatMap.get(id);
                    if (seat == null || !seat.isAvailable()) {
                        throw new SeatUnavailableException("Ghế bạn chọn không hợp lệ cho suất chiếu này.");
                    }
                    if (seat.isTaken()) {
                        throw new SeatUnavailableException("Ghế " + seat.getLabel() + " vừa có người khác chọn.");
                    }
                    total = total.add(seat.getPrice());
                }

                int bookingId = insertBooking(conn, userId, bookingType, showtimeId, total);
                Timestamp holdUntil = queryHoldUntil(conn);
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO BookingSeats (BookingID, ShowtimeID, SeatID, Price, SeatStatus, HoldUntil) "
                      + "VALUES (?, ?, ?, ?, 'HELD', ?)")) {
                    for (Integer id : ids) {
                        ps.setInt(1, bookingId);
                        ps.setInt(2, showtimeId);
                        ps.setInt(3, id);
                        ps.setBigDecimal(4, seatMap.get(id).getPrice());
                        ps.setTimestamp(5, holdUntil);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                conn.commit();
                return bookingId;
            } catch (SQLException e) {
                conn.rollback();
                if (isDuplicateKey(e)) {
                    throw new SeatUnavailableException("Một trong các ghế bạn chọn vừa có người khác đặt. Vui lòng chọn lại.");
                }
                throw e;
            } catch (SeatUnavailableException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public Booking getBooking(int bookingId) throws SQLException {
        String sql = "SELECT BookingID, BookingCode, UserID, ShowtimeID, BookingType, BookingStatus, SubTotal, DiscountAmount, "
                   + "  TotalAmount, CreatedAt, v.VoucherCode, "
                   + "  (SELECT MIN(HoldUntil) FROM BookingSeats WHERE BookingID = b.BookingID AND SeatStatus = 'HELD') AS HoldUntil "
                   + "FROM Bookings b LEFT JOIN Vouchers v ON v.VoucherID = b.VoucherID WHERE BookingID = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                int userId = rs.getInt("UserID");
                Integer owner = rs.wasNull() ? null : userId;
                Showtime showtime = loadShowtime(conn, rs.getInt("ShowtimeID"));
                Booking booking = new Booking(rs.getInt("BookingID"), rs.getString("BookingCode"), owner,
                        rs.getString("BookingType"), rs.getString("BookingStatus"), rs.getBigDecimal("TotalAmount"),
                        rs.getTimestamp("HoldUntil"), rs.getTimestamp("CreatedAt"), showtime, loadSeats(conn, bookingId));
                booking.setSubTotal(rs.getBigDecimal("SubTotal"));
                booking.setDiscountAmount(rs.getBigDecimal("DiscountAmount"));
                booking.setVoucherCode(rs.getString("VoucherCode"));
                booking.setFnbLines(FnbOrderDAO.getLines(conn, bookingId));
                return booking;
            }
        }
    }

    /** Tong tien don = tien ve (SubTotal) - giam gia + bap nuoc chua huy. */
    static void recalculateTotal(Connection conn, int bookingId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE b SET TotalAmount = CASE WHEN b.SubTotal - b.DiscountAmount < 0 THEN 0 ELSE b.SubTotal - b.DiscountAmount END "
              + "  + ISNULL((SELECT SUM(o.TotalAmount) FROM FnbOrders o WHERE o.BookingID = b.BookingID AND o.OrderStatus <> 'CANCELLED'), 0), "
              + "  UpdatedAt = SYSDATETIME() "
              + "FROM Bookings b WHERE b.BookingID = ? AND b.BookingStatus = 'PENDING'")) {
            ps.setInt(1, bookingId);
            ps.executeUpdate();
        }
    }

    /** Don doi so tien: huy giao dich VNPay dang cho (so tien cu), khach bam thanh toan lai se tao giao dich moi. */
    static void cancelPendingPayments(Connection conn, int bookingId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE Payments SET PaymentStatus = 'CANCELLED' WHERE BookingID = ? AND PaymentStatus = 'PENDING'")) {
            ps.setInt(1, bookingId);
            ps.executeUpdate();
        }
    }

    /** Nguoi dung tu huy booking dang cho thanh toan. */
    public boolean cancelPending(int bookingId, int userId) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean done = cancelPendingBookings(conn,
                        "BookingID = ? AND UserID = ?", bookingId, userId) > 0;
                conn.commit();
                return done;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /** Nhan vien huy don tai quay dang cho thanh toan. */
    public boolean cancelCounterPending(int bookingId) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                boolean done = cancelPendingBookings(conn,
                        "BookingID = ? AND BookingType = 'COUNTER'", bookingId) > 0;
                conn.commit();
                return done;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /** Booking PENDING qua han giu ghe -> EXPIRED, nha ghe, payment dang cho -> TIMEOUT. */
    static void releaseExpiredHolds(Connection conn) throws SQLException {
        String expired = "SELECT BookingID FROM Bookings b WHERE b.BookingStatus = 'PENDING' AND EXISTS ("
                       + "SELECT 1 FROM BookingSeats bs WHERE bs.BookingID = b.BookingID "
                       + "AND bs.SeatStatus = 'HELD' AND bs.HoldUntil <= SYSDATETIME())";
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("UPDATE Payments SET PaymentStatus = 'TIMEOUT' "
                    + "WHERE PaymentStatus = 'PENDING' AND BookingID IN (" + expired + ")");
            FnbOrderDAO.cancelPendingForBookings(conn, expired);
            st.executeUpdate("UPDATE Bookings SET BookingStatus = 'EXPIRED', UpdatedAt = SYSDATETIME() "
                    + "WHERE BookingID IN (" + expired + ")");
            st.executeUpdate("UPDATE BookingSeats SET SeatStatus = 'RELEASED' "
                    + "WHERE SeatStatus = 'HELD' AND HoldUntil <= SYSDATETIME()");
        }
    }

    private static void cancelUserPendingForShowtime(Connection conn, int userId, int showtimeId) throws SQLException {
        cancelPendingBookings(conn, "UserID = ? AND ShowtimeID = ?", userId, showtimeId);
    }

    private static int cancelPendingBookings(Connection conn, String where, int... params) throws SQLException {
        String ids = "SELECT BookingID FROM Bookings WHERE BookingStatus = 'PENDING' AND " + where;
        try (PreparedStatement pay = conn.prepareStatement(
                 "UPDATE Payments SET PaymentStatus = 'CANCELLED' WHERE PaymentStatus = 'PENDING' AND BookingID IN (" + ids + ")");
             PreparedStatement seats = conn.prepareStatement(
                 "UPDATE BookingSeats SET SeatStatus = 'CANCELLED' WHERE SeatStatus = 'HELD' AND BookingID IN (" + ids + ")");
             PreparedStatement booking = conn.prepareStatement(
                 "UPDATE Bookings SET BookingStatus = 'CANCELLED', UpdatedAt = SYSDATETIME() WHERE BookingStatus = 'PENDING' AND " + where)) {
            for (PreparedStatement ps : new PreparedStatement[]{pay, seats, booking}) {
                for (int i = 0; i < params.length; i++) ps.setInt(i + 1, params[i]);
            }
            pay.executeUpdate();
            seats.executeUpdate();
            FnbOrderDAO.cancelPendingForBookings(conn, ids, params);
            return booking.executeUpdate();
        }
    }

    private static int insertBooking(Connection conn, Integer userId, String bookingType, int showtimeId,
            BigDecimal total) throws SQLException {
        String sql = "INSERT INTO Bookings (BookingCode, UserID, ShowtimeID, BookingType, BookingStatus, "
                   + "SubTotal, DiscountAmount, TotalAmount) VALUES (?, ?, ?, ?, 'PENDING', ?, 0, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, newBookingCode());
            if (userId == null) ps.setNull(2, java.sql.Types.INTEGER);
            else ps.setInt(2, userId);
            ps.setInt(3, showtimeId);
            ps.setString(4, bookingType);
            ps.setBigDecimal(5, total);
            ps.setBigDecimal(6, total);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private static Timestamp queryHoldUntil(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT DATEADD(MINUTE, ?, SYSDATETIME())")) {
            ps.setInt(1, HOLD_MINUTES);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getTimestamp(1);
            }
        }
    }

    private static Showtime loadShowtime(Connection conn, int showtimeId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(ShowtimeDAO.SELECT + "WHERE s.ShowtimeID = ?")) {
            ps.setInt(1, showtimeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? ShowtimeDAO.map(rs) : null;
            }
        }
    }

    private static List<Seat> loadSeats(Connection conn, int bookingId) throws SQLException {
        String sql = "SELECT se.SeatID, se.SeatRow, se.SeatNumber, se.SeatType, bs.Price "
                   + "FROM BookingSeats bs JOIN Seats se ON se.SeatID = bs.SeatID "
                   + "WHERE bs.BookingID = ? ORDER BY se.SeatRow, se.SeatNumber";
        List<Seat> seats = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    seats.add(new Seat(rs.getInt("SeatID"), rs.getString("SeatRow"), rs.getInt("SeatNumber"),
                            rs.getString("SeatType"), true, true, rs.getBigDecimal("Price")));
                }
            }
        }
        return seats;
    }

    static boolean isDuplicateKey(SQLException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql && (sql.getErrorCode() == 2601 || sql.getErrorCode() == 2627)) return true;
        }
        return false;
    }

    static String randomCode(String prefix, int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < length; i++) sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        return sb.toString();
    }

    private static String newBookingCode() {
        return randomCode("CF", 10);
    }
}
