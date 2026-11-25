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

    /* ============================== MỤC LỤC ==============================
     * Tìm nhanh: Ctrl + F tên hàm. Mỗi hàm có dòng ">> Dùng ở: ... | Loại: ĐỌC / GHI" trong chú thích.
     * Đọc đơn ......... getBooking (+ loadShowtime, loadSeats), getAmounts, getVoucherOwner
     * Giữ ghế ......... findExpiredPendingIds, releaseOverdueSeats, findPendingIdsOfUserForShowtime,
     *                   getDbTimePlusMinutes, insertBooking, insertHeldSeats
     * Huỷ / hết hạn ... updatePendingStatus, cancelHeldSeats, findPendingIdOfUser, findPendingCounterId
     * Tiền / voucher .. updateTotal, updateVoucher
     * Đã thanh toán ... confirmSeats, updateStatus, findConfirmedSeatsWithoutTicket
     * Suất chiếu xong . completeFinishedBookings
     * ===================================================================== */

    /**
     * Tiền vé (SubTotal), số tiền giảm (DiscountAmount) và trạng thái hiện tại của đơn, dùng để tính lại tổng tiền.
     */
    public record Amounts(BigDecimal subTotal, BigDecimal discountAmount, String status) {}

    /** Voucher đang áp và khách (UserID) của đơn; null nếu không có. Dùng khi ghi lượt dùng voucher. */
    public record VoucherOwner(Integer voucherId, Integer userId) {}

    // ---------------------------------------------------------------- doc don

    /**
     * Lấy đơn kèm suất chiếu và danh sách ghế (chưa gồm bắp nước); không có thì null.
     * Bảng: Bookings, Vouchers, BookingSeats, Showtimes, Movies, Halls, Centres, Seats.
     * >> Dùng ở: mọi trang có đơn: bắp nước, checkout, chi tiết đơn, in vé (BookingService.getBooking) | Loại: ĐỌC (SELECT)
     */
    public Booking getBooking(int bookingId) throws SQLException {
        String sql = """
                SELECT BookingID, BookingCode, UserID, ShowtimeID, BookingType, BookingStatus, SubTotal, DiscountAmount,
                  TotalAmount, CreatedAt, v.VoucherCode,
                  (SELECT MIN(HoldUntil) FROM BookingSeats WHERE BookingID = b.BookingID AND SeatStatus = 'HELD') AS HoldUntil
                FROM Bookings b LEFT JOIN Vouchers v ON v.VoucherID = b.VoucherID WHERE BookingID = ?
                """;
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

    /**
     * Đọc tiền vé, số tiền giảm và trạng thái của đơn (trong transaction có sẵn).
     * >> Dùng ở: tính lại tổng tiền (BookingService.recalculateTotal) | Loại: ĐỌC (SELECT)
     */
    public Amounts getAmounts(Connection conn, int bookingId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                        SELECT SubTotal, DiscountAmount, BookingStatus FROM Bookings WHERE BookingID = ?
                        """)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Amounts(rs.getBigDecimal(1), rs.getBigDecimal(2), rs.getString(3)) : null;
            }
        }
    }

    /**
     * Đọc voucher đang áp và khách của đơn (trong transaction có sẵn).
     * >> Dùng ở: thanh toán xong - ghi lượt dùng voucher (PaymentService.recordVoucherUsage) | Loại: ĐỌC (SELECT)
     */
    public VoucherOwner getVoucherOwner(Connection conn, int bookingId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT VoucherID, UserID FROM Bookings WHERE BookingID = ?
                """)) {
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

    /**
     * Tìm các đơn PENDING có ghế HELD đã quá HoldUntil (so theo giờ SQL Server).
     * >> Dùng ở: giữ ghế - dọn đơn quá 5 phút (BookingService.releaseExpiredHolds) | Loại: ĐỌC (SELECT)
     */
    public List<Integer> findExpiredPendingIds(Connection conn) throws SQLException {
        return ids(conn, """
                SELECT BookingID FROM Bookings b WHERE b.BookingStatus = 'PENDING' AND EXISTS (
                SELECT 1 FROM BookingSeats bs WHERE bs.BookingID = b.BookingID
                AND bs.SeatStatus = 'HELD' AND bs.HoldUntil <= SYSDATETIME())
                """);
    }

    /**
     * Tìm các đơn PENDING của một khách cho một suất chiếu (để huỷ trước khi khách giữ ghế mới).
     * >> Dùng ở: giữ ghế - đơn chờ cũ của khách ở cùng suất (BookingService.hold) | Loại: ĐỌC (SELECT)
     */
    public List<Integer> findPendingIdsOfUserForShowtime(Connection conn, int userId, int showtimeId) throws SQLException {
        return ids(conn, """
                SELECT BookingID FROM Bookings WHERE BookingStatus = 'PENDING' AND UserID = ? AND ShowtimeID = ?
                """,
                userId, showtimeId);
    }

    /**
     * Tìm đơn PENDING có mã bookingId và thuộc khách userId (danh sách 0 hoặc 1 phần tử).
     * >> Dùng ở: khách bấm Huỷ đơn (BookingService.cancelPendingBooking) | Loại: ĐỌC (SELECT)
     */
    public List<Integer> findPendingIdOfUser(Connection conn, int bookingId, int userId) throws SQLException {
        return ids(conn, """
                SELECT BookingID FROM Bookings WHERE BookingStatus = 'PENDING' AND BookingID = ? AND UserID = ?
                """,
                bookingId, userId);
    }

    /**
     * Tìm đơn tại quầy PENDING có mã bookingId (danh sách 0 hoặc 1 phần tử).
     * >> Dùng ở: nhân viên huỷ đơn tại quầy (BookingService.cancelCounterPendingBooking) | Loại: ĐỌC (SELECT)
     */
    public List<Integer> findPendingCounterId(Connection conn, int bookingId) throws SQLException {
        return ids(conn, """
                SELECT BookingID FROM Bookings WHERE BookingStatus = 'PENDING' AND BookingID = ?
                AND BookingType = 'COUNTER'
                """, bookingId);
    }

    // ---------------------------------------------------------------- ghi don

    /**
     * Ghi đơn mới: mã đơn, khách (null nếu tại quầy), loại ONLINE / COUNTER, suất chiếu, tiền vé; trạng thái PENDING.
     * @return BookingID vừa tạo
     * >> Dùng ở: giữ ghế - tạo đơn PENDING (BookingService.hold) | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    public int insertBooking(Connection conn, String code, Integer userId, String bookingType, int showtimeId,
            BigDecimal subTotal) throws SQLException {
        String sql = """
                INSERT INTO Bookings (BookingCode, UserID, ShowtimeID, BookingType, BookingStatus,
                SubTotal, DiscountAmount, TotalAmount) VALUES (?, ?, ?, ?, 'PENDING', ?, 0, ?)
                """;
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

    /**
     * Đổi trạng thái các đơn còn PENDING (ví dụ sang EXPIRED hoặc CANCELLED). Đơn đã đổi trạng thái khác thì không bị đụng.
     * @return số đơn đã đổi
     * >> Dùng ở: hết hạn / huỷ đơn - đơn chờ -> EXPIRED / CANCELLED (releaseExpiredHolds, cancelPending) | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    public int updatePendingStatus(Connection conn, List<Integer> bookingIds, String newStatus) throws SQLException {
        if (bookingIds.isEmpty()) return 0;
        // %s = "?, ?, ?" (một dấu ? cho mỗi đơn)
        try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE Bookings SET BookingStatus = ?, UpdatedAt = SYSDATETIME()
                WHERE BookingStatus = 'PENDING' AND BookingID IN (%s)
                """.formatted(DbUtil.placeholders(bookingIds.size())))) {
            ps.setString(1, newStatus);
            DbUtil.setInts(ps, 2, bookingIds);
            return ps.executeUpdate();
        }
    }

    /**
     * Đổi trạng thái một đơn (CONFIRMED, CANCELLED...) và cập nhật UpdatedAt.
     * >> Dùng ở: thanh toán - đơn -> CONFIRMED / CANCELLED (PaymentService.confirmPaid) | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    public void updateStatus(Connection conn, int bookingId, String newStatus) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                        UPDATE Bookings SET BookingStatus = ?, UpdatedAt = SYSDATETIME() WHERE BookingID = ?
                        """)) {
            ps.setString(1, newStatus);
            ps.setInt(2, bookingId);
            ps.executeUpdate();
        }
    }

    /**
     * Đơn CONFIRMED của suất đã chiếu xong (không tính suất bị huỷ) -> COMPLETED.
     * @return số đơn đã đổi
     * >> Dùng ở: job mỗi phút - suất chiếu xong: CONFIRMED -> COMPLETED (BookingCompletionJob) | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    public int completeFinishedBookings(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                        UPDATE b SET BookingStatus = 'COMPLETED', UpdatedAt = SYSDATETIME()
                        FROM Bookings b JOIN Showtimes s ON s.ShowtimeID = b.ShowtimeID
                        WHERE b.BookingStatus = 'CONFIRMED' AND s.Status <> 'CANCELLED' AND s.EndTime <= SYSDATETIME()
                        """)) {
            return ps.executeUpdate();
        }
    }

    /**
     * Ghi tổng tiền mới cho đơn còn PENDING.
     * >> Dùng ở: lưu bắp nước / áp voucher - ghi tổng tiền mới (BookingService.recalculateTotal) | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    public void updateTotal(Connection conn, int bookingId, BigDecimal total) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                        UPDATE Bookings SET TotalAmount = ?, UpdatedAt = SYSDATETIME() WHERE BookingID = ? AND BookingStatus = 'PENDING'
                        """)) {
            ps.setBigDecimal(1, total);
            ps.setInt(2, bookingId);
            ps.executeUpdate();
        }
    }

    /**
     * Gán hoặc bỏ voucher và số tiền giảm của đơn còn PENDING.
     * @param voucherId null = bỏ voucher
     * >> Dùng ở: trang checkout - áp / bỏ voucher (BookingExtrasService.setVoucher) | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    public void updateVoucher(Connection conn, int bookingId, Integer voucherId, BigDecimal discount) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                        UPDATE Bookings SET VoucherID = ?, DiscountAmount = ?, UpdatedAt = SYSDATETIME()
                        WHERE BookingID = ? AND BookingStatus = 'PENDING'
                        """)) {
            if (voucherId == null) ps.setNull(1, Types.INTEGER);
            else ps.setInt(1, voucherId);
            ps.setBigDecimal(2, discount);
            ps.setInt(3, bookingId);
            ps.executeUpdate();
        }
    }

    // ---------------------------------------------------------------- ghe cua don

    /**
     * Ghi các ghế đang giữ của đơn vào BookingSeats: trạng thái HELD, giá từng ghế, hạn giữ ghế (HoldUntil).
     * Unique index UX_BookingSeats_ActiveSeat chặn hai đơn cùng giữ một ghế của một suất.
     * >> Dùng ở: giữ ghế - ghi ghế HELD + hạn giữ (BookingService.hold) | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    public void insertHeldSeats(Connection conn, int bookingId, int showtimeId, List<Seat> seats, Timestamp holdUntil)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                        INSERT INTO BookingSeats (BookingID, ShowtimeID, SeatID, Price, SeatStatus, HoldUntil)
                        VALUES (?, ?, ?, ?, 'HELD', ?)
                        """)) {
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

    /**
     * Ghế HELD đã quá HoldUntil -> RELEASED. Khi đó ghế ra khỏi unique index nên người khác giữ được.
     * >> Dùng ở: giữ ghế - nhả ghế HELD quá hạn (BookingService.releaseExpiredHolds) | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    public void releaseOverdueSeats(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                    UPDATE BookingSeats SET SeatStatus = 'RELEASED'
                    WHERE SeatStatus = 'HELD' AND HoldUntil <= SYSDATETIME()
                    """);
        }
    }

    /**
     * Ghế HELD của các đơn -> CANCELLED (đơn bị huỷ).
     * >> Dùng ở: huỷ đơn - nhả ghế HELD (BookingService.cancelPending) | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    public void cancelHeldSeats(Connection conn, List<Integer> bookingIds) throws SQLException {
        if (bookingIds.isEmpty()) return;
        // %s = "?, ?, ?" (một dấu ? cho mỗi đơn)
        try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE BookingSeats SET SeatStatus = 'CANCELLED' WHERE SeatStatus = 'HELD'
                AND BookingID IN (%s)
                """.formatted(DbUtil.placeholders(bookingIds.size())))) {
            DbUtil.setInts(ps, 1, bookingIds);
            ps.executeUpdate();
        }
    }

    /**
     * Ghế của đơn -> CONFIRMED khi đã nhận tiền. Gồm cả ghế RELEASED / CANCELLED khi tiền về sau lúc hết hạn giữ ghế.
     * @throws SQLException lỗi trùng khoá nếu ghế đã thuộc đơn khác (confirmPaid bắt lỗi này để huỷ đơn)
     * >> Dùng ở: thanh toán xong - ghế -> CONFIRMED (PaymentService.confirmPaid) | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    public void confirmSeats(Connection conn, int bookingId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                        UPDATE BookingSeats SET SeatStatus = 'CONFIRMED', HoldUntil = NULL
                        WHERE BookingID = ? AND SeatStatus IN ('HELD', 'RELEASED', 'CANCELLED')
                        """)) {
            ps.setInt(1, bookingId);
            ps.executeUpdate();
        }
    }

    /**
     * Các BookingSeatID đã CONFIRMED mà chưa có vé (để xuất vé, tránh xuất trùng).
     * >> Dùng ở: thanh toán xong - ghế chưa có vé để xuất vé (PaymentService.issueTickets) | Loại: ĐỌC (SELECT)
     */
    public List<Integer> findConfirmedSeatsWithoutTicket(Connection conn, int bookingId) throws SQLException {
        return ids(conn, """
                SELECT bs.BookingSeatID FROM BookingSeats bs
                WHERE bs.BookingID = ? AND bs.SeatStatus = 'CONFIRMED'
                AND NOT EXISTS (SELECT 1 FROM Tickets t WHERE t.BookingSeatID = bs.BookingSeatID)
                """, bookingId);
    }

    /**
     * Giờ hiện tại của SQL Server cộng thêm số phút, dùng làm hạn giữ ghế.
     * Dùng giờ SQL Server (không dùng giờ Java) để khớp với các câu lệnh so sánh HoldUntil với SYSDATETIME().
     * >> Dùng ở: giữ ghế - hạn giữ ghế theo giờ SQL Server (BookingService.hold) | Loại: ĐỌC (SELECT)
     */
    public Timestamp getDbTimePlusMinutes(Connection conn, int minutes) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT DATEADD(MINUTE, ?, SYSDATETIME())
                """)) {
            ps.setInt(1, minutes);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getTimestamp(1);
            }
        }
    }

    // ---------------------------------------------------------------- noi bo

    /**
     * Đọc thông tin suất chiếu của đơn (phim, rạp, phòng, giờ) để gắn vào đối tượng Booking.
     * >> Dùng ở: nội bộ - getBooking | Loại: ĐỌC (SELECT)
     */
    private static Showtime loadShowtime(Connection conn, int showtimeId) throws SQLException {
        // %s = ShowtimeDAO.SELECT (cột suất chiếu + phim + phòng + rạp)
        try (PreparedStatement ps = conn.prepareStatement("""
                %s
                WHERE s.ShowtimeID = ?
                """.formatted(ShowtimeDAO.SELECT))) {
            ps.setInt(1, showtimeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? ShowtimeDAO.map(rs) : null;
            }
        }
    }

    /**
     * Đọc danh sách ghế của đơn (nhãn ghế, loại, giá, trạng thái) để gắn vào đối tượng Booking.
     * >> Dùng ở: nội bộ - getBooking | Loại: ĐỌC (SELECT)
     */
    private static List<Seat> loadSeats(Connection conn, int bookingId) throws SQLException {
        String sql = """
                SELECT se.SeatID, se.SeatRow, se.SeatNumber, se.SeatType, bs.Price
                FROM BookingSeats bs JOIN Seats se ON se.SeatID = bs.SeatID
                WHERE bs.BookingID = ? ORDER BY se.SeatRow, se.SeatNumber
                """;
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

    /**
     * Chạy câu SELECT trả về một cột số nguyên với các tham số truyền vào, gom thành danh sách id.
     * >> Dùng ở: nội bộ - chạy câu SELECT trả về danh sách mã (gán ? theo thứ tự tham số) | Loại: ĐỌC (SELECT)
     */
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
