package dao;

import model.entity.Booking;
import model.entity.Seat;
import model.entity.Showtime;
import utils.DBContext;
import utils.DbUtil;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Truy cap bang Bookings / BookingSeats. Chi doc / ghi du lieu; quy tac giu ghe, tinh tien, chuyen trang thai
 * nam o service.BookingService. Cac ham nhan Connection de service gom nhieu thao tac vao mot transaction.
 */
public class BookingDAO {

    /** Tien ve, giam gia va trang thai hien tai cua don. */
    public record Amounts(BigDecimal subTotal, BigDecimal discountAmount, String status) {}

    /** Voucher va khach cua don (null neu khong co). */
    public record VoucherOwner(Integer voucherId, Integer userId) {}

    // ---------------------------------------------------------------- doc don

    /** Don kem suat chieu va ghe (chua gom bap nuoc), null neu khong co. */
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
                return booking;
            }
        }
    }

    public Amounts getAmounts(Connection conn, int bookingId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT SubTotal, DiscountAmount, BookingStatus FROM Bookings WHERE BookingID = ?")) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Amounts(rs.getBigDecimal(1), rs.getBigDecimal(2), rs.getString(3)) : null;
            }
        }
    }

    public VoucherOwner getVoucherOwner(Connection conn, int bookingId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT VoucherID, UserID FROM Bookings WHERE BookingID = ?")) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return new VoucherOwner(null, null);
                int v = rs.getInt("VoucherID");
                Integer voucherId = rs.wasNull() ? null : v;
                int u = rs.getInt("UserID");
                Integer userId = rs.wasNull() ? null : u;
                return new VoucherOwner(voucherId, userId);
            }
        }
    }

    // ---------------------------------------------------------------- tim don cho thanh toan

    /** Don PENDING co ghe HELD da qua HoldUntil (theo gio SQL Server). */
    public List<Integer> findExpiredPendingIds(Connection conn) throws SQLException {
        return ids(conn, "SELECT BookingID FROM Bookings b WHERE b.BookingStatus = 'PENDING' AND EXISTS ("
                       + "SELECT 1 FROM BookingSeats bs WHERE bs.BookingID = b.BookingID "
                       + "AND bs.SeatStatus = 'HELD' AND bs.HoldUntil <= SYSDATETIME())");
    }

    /** Don PENDING cua khach cho mot suat chieu. */
    public List<Integer> findPendingIdsOfUserForShowtime(Connection conn, int userId, int showtimeId) throws SQLException {
        return ids(conn, "SELECT BookingID FROM Bookings WHERE BookingStatus = 'PENDING' AND UserID = ? AND ShowtimeID = ?",
                userId, showtimeId);
    }

    /** Don PENDING co ma bookingId thuoc khach userId (0 hoac 1 phan tu). */
    public List<Integer> findPendingIdOfUser(Connection conn, int bookingId, int userId) throws SQLException {
        return ids(conn, "SELECT BookingID FROM Bookings WHERE BookingStatus = 'PENDING' AND BookingID = ? AND UserID = ?",
                bookingId, userId);
    }

    /** Don tai quay PENDING co ma bookingId (0 hoac 1 phan tu). */
    public List<Integer> findPendingCounterId(Connection conn, int bookingId) throws SQLException {
        return ids(conn, "SELECT BookingID FROM Bookings WHERE BookingStatus = 'PENDING' AND BookingID = ? "
                       + "AND BookingType = 'COUNTER'", bookingId);
    }

    // ---------------------------------------------------------------- ghi don

    /** @return BookingID vua tao */
    public int insertBooking(Connection conn, String code, Integer userId, String bookingType, int showtimeId,
            BigDecimal subTotal) throws SQLException {
        String sql = "INSERT INTO Bookings (BookingCode, UserID, ShowtimeID, BookingType, BookingStatus, "
                   + "SubTotal, DiscountAmount, TotalAmount) VALUES (?, ?, ?, ?, 'PENDING', ?, 0, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, code);
            if (userId == null) ps.setNull(2, Types.INTEGER);
            else ps.setInt(2, userId);
            ps.setInt(3, showtimeId);
            ps.setString(4, bookingType);
            ps.setBigDecimal(5, subTotal);
            ps.setBigDecimal(6, subTotal);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    /** Doi trang thai cac don con PENDING. @return so don da doi */
    public int updatePendingStatus(Connection conn, List<Integer> bookingIds, String newStatus) throws SQLException {
        if (bookingIds.isEmpty()) return 0;
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE Bookings SET BookingStatus = ?, UpdatedAt = SYSDATETIME() "
              + "WHERE BookingStatus = 'PENDING' AND BookingID IN (" + DbUtil.placeholders(bookingIds.size()) + ")")) {
            ps.setString(1, newStatus);
            DbUtil.setInts(ps, 2, bookingIds);
            return ps.executeUpdate();
        }
    }

    public void updateStatus(Connection conn, int bookingId, String newStatus) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE Bookings SET BookingStatus = ?, UpdatedAt = SYSDATETIME() WHERE BookingID = ?")) {
            ps.setString(1, newStatus);
            ps.setInt(2, bookingId);
            ps.executeUpdate();
        }
    }

    /** Don CONFIRMED cua suat da chieu xong (khong tinh suat bi huy) -> COMPLETED. @return so don da doi */
    public int completeFinishedBookings(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE b SET BookingStatus = 'COMPLETED', UpdatedAt = SYSDATETIME() "
              + "FROM Bookings b JOIN Showtimes s ON s.ShowtimeID = b.ShowtimeID "
              + "WHERE b.BookingStatus = 'CONFIRMED' AND s.Status <> 'CANCELLED' AND s.EndTime <= SYSDATETIME()")) {
            return ps.executeUpdate();
        }
    }

    /** Ghi tong tien moi cho don con PENDING. */
    public void updateTotal(Connection conn, int bookingId, BigDecimal total) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE Bookings SET TotalAmount = ?, UpdatedAt = SYSDATETIME() WHERE BookingID = ? AND BookingStatus = 'PENDING'")) {
            ps.setBigDecimal(1, total);
            ps.setInt(2, bookingId);
            ps.executeUpdate();
        }
    }

    /** Gan / bo voucher cua don con PENDING. @param voucherId null = bo voucher */
    public void updateVoucher(Connection conn, int bookingId, Integer voucherId, BigDecimal discount) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE Bookings SET VoucherID = ?, DiscountAmount = ?, UpdatedAt = SYSDATETIME() "
              + "WHERE BookingID = ? AND BookingStatus = 'PENDING'")) {
            if (voucherId == null) ps.setNull(1, Types.INTEGER);
            else ps.setInt(1, voucherId);
            ps.setBigDecimal(2, discount);
            ps.setInt(3, bookingId);
            ps.executeUpdate();
        }
    }

    // ---------------------------------------------------------------- ghe cua don

    /** Ghi cac ghe dang giu (HELD) cua don, gia lay theo tung ghe. */
    public void insertHeldSeats(Connection conn, int bookingId, int showtimeId, List<Seat> seats, Timestamp holdUntil)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO BookingSeats (BookingID, ShowtimeID, SeatID, Price, SeatStatus, HoldUntil) "
              + "VALUES (?, ?, ?, ?, 'HELD', ?)")) {
            for (Seat seat : seats) {
                ps.setInt(1, bookingId);
                ps.setInt(2, showtimeId);
                ps.setInt(3, seat.getId());
                ps.setBigDecimal(4, seat.getPrice());
                ps.setTimestamp(5, holdUntil);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /** Ghe HELD da qua HoldUntil -> RELEASED (ra khoi unique index, nguoi khac giu duoc). */
    public void releaseOverdueSeats(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("UPDATE BookingSeats SET SeatStatus = 'RELEASED' "
                    + "WHERE SeatStatus = 'HELD' AND HoldUntil <= SYSDATETIME()");
        }
    }

    /** Ghe HELD cua cac don -> CANCELLED. */
    public void cancelHeldSeats(Connection conn, List<Integer> bookingIds) throws SQLException {
        if (bookingIds.isEmpty()) return;
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE BookingSeats SET SeatStatus = 'CANCELLED' WHERE SeatStatus = 'HELD' "
              + "AND BookingID IN (" + DbUtil.placeholders(bookingIds.size()) + ")")) {
            DbUtil.setInts(ps, 1, bookingIds);
            ps.executeUpdate();
        }
    }

    /**
     * Ghe cua don -> CONFIRMED (gom ca RELEASED / CANCELLED khi tien ve sau khi het han giu ghe).
     * @throws SQLException loi trung khoa neu ghe da thuoc don khac (unique index UX_BookingSeats_ActiveSeat)
     */
    public void confirmSeats(Connection conn, int bookingId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE BookingSeats SET SeatStatus = 'CONFIRMED', HoldUntil = NULL "
              + "WHERE BookingID = ? AND SeatStatus IN ('HELD', 'RELEASED', 'CANCELLED')")) {
            ps.setInt(1, bookingId);
            ps.executeUpdate();
        }
    }

    /** BookingSeatID cua cac ghe CONFIRMED chua xuat ve. */
    public List<Integer> findConfirmedSeatsWithoutTicket(Connection conn, int bookingId) throws SQLException {
        return ids(conn, "SELECT bs.BookingSeatID FROM BookingSeats bs "
                       + "WHERE bs.BookingID = ? AND bs.SeatStatus = 'CONFIRMED' "
                       + "AND NOT EXISTS (SELECT 1 FROM Tickets t WHERE t.BookingSeatID = bs.BookingSeatID)", bookingId);
    }

    /** Gio SQL Server cong them so phut (moc het han giu ghe dung chung mot dong ho voi cac cau lenh SQL). */
    public Timestamp getDbTimePlusMinutes(Connection conn, int minutes) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT DATEADD(MINUTE, ?, SYSDATETIME())")) {
            ps.setInt(1, minutes);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getTimestamp(1);
            }
        }
    }

    // ---------------------------------------------------------------- noi bo

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

    private static List<Integer> ids(Connection conn, String sql, int... params) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setInt(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt(1));
            }
        }
        return ids;
    }
}
