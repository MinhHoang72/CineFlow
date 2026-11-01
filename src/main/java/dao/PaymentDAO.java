package dao;

import model.entity.Payment;
import model.dto.booking.PaymentTransaction;
import model.entity.Ticket;
import utils.DBContext;
import utils.DbUtil;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Truy cap bang Payments va Tickets. Chi doc / ghi du lieu; quy tac xac nhan thanh toan, sinh ve,
 * xu ly ket qua VNPay nam o service.PaymentService va service.FnbSaleService.
 */
public class PaymentDAO {

    /** Giao dich VNPay cua don dat ve (PaymentMethod = 'QR', TransactionCode = vnp_TxnRef). */
    public record VnpayRef(int paymentId, int bookingId, BigDecimal amount, String status) {}

    /** Giao dich VNPay cua don bap nuoc le (FnbOrderID co gia tri, BookingID = NULL). */
    public record FnbVnpayRef(int paymentId, int orderId, BigDecimal amount, String status) {}

    /** Giao dich ve da khoa de xac nhan, kem tong tien hien tai cua don. */
    public record LockedPayment(int bookingId, String status, BigDecimal amount, BigDecimal bookingTotal) {}

    /** Giao dich bap nuoc le da khoa de xac nhan. */
    public record LockedFnbPayment(int orderId, String status) {}

    /** Ve moi can ghi. */
    public record NewTicket(int bookingSeatId, String ticketCode, String qrCode) {}

    // ---------------------------------------------------------------- giao dich ve

    /** @return PaymentID */
    public int insertBookingPayment(Connection conn, int bookingId, String transactionCode, String method,
            BigDecimal amount, BigDecimal cashReceived) throws SQLException {
        return insert(conn, "BookingID", bookingId, transactionCode, method, amount, cashReceived);
    }

    /** Giao dich VNPay dang cho cua don -> CANCELLED (khach tao giao dich moi). */
    public void cancelPendingQr(Connection conn, int bookingId) throws SQLException {
        update(conn, "UPDATE Payments SET PaymentStatus = 'CANCELLED' "
                   + "WHERE BookingID = ? AND PaymentMethod = 'QR' AND PaymentStatus = 'PENDING'", bookingId);
    }

    /** Giao dich dang cho cua cac don -> trang thai moi (CANCELLED / TIMEOUT). */
    public void updatePendingForBookings(Connection conn, List<Integer> bookingIds, String newStatus) throws SQLException {
        updatePendingIn(conn, "BookingID", bookingIds, newStatus);
    }

    public VnpayRef findVnpay(String txnRef) throws SQLException {
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT PaymentID, BookingID, Amount, PaymentStatus FROM Payments "
                   + "WHERE TransactionCode = ? AND PaymentMethod = 'QR' AND BookingID IS NOT NULL")) {
            ps.setString(1, txnRef);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new VnpayRef(rs.getInt("PaymentID"), rs.getInt("BookingID"),
                        rs.getBigDecimal("Amount"), rs.getString("PaymentStatus")) : null;
            }
        }
    }

    /** PENDING -> FAILED (VNPay bao giao dich khong thanh cong). */
    public void markFailed(int paymentId) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            update(conn, "UPDATE Payments SET PaymentStatus = 'FAILED' WHERE PaymentID = ? AND PaymentStatus = 'PENDING'",
                    paymentId);
        }
    }

    /** Trang thai giao dich VNPay moi nhat cua don, null neu chua co. */
    public String latestQrStatus(int bookingId) throws SQLException {
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT TOP 1 PaymentStatus FROM Payments WHERE BookingID = ? AND PaymentMethod = 'QR' "
                   + "ORDER BY PaymentID DESC")) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /**
     * Khoa giao dich ve (UPDLOCK) de xac nhan, kem tong tien don. null neu khong co / khong thuoc rap.
     * @param centreId null = moi rap
     */
    public LockedPayment lockBookingPayment(Connection conn, int paymentId, Integer centreId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT p.BookingID, p.PaymentStatus, p.Amount, b.TotalAmount "
              + "FROM Payments p WITH (UPDLOCK, ROWLOCK) "
              + "JOIN Bookings b ON b.BookingID = p.BookingID "
              + "JOIN Showtimes s ON s.ShowtimeID = b.ShowtimeID JOIN Halls h ON h.HallID = s.HallID "
              + "WHERE p.PaymentID = ? AND (? IS NULL OR h.CentreID = ?)")) {
            ps.setInt(1, paymentId);
            setNullableInt(ps, 2, centreId);
            setNullableInt(ps, 3, centreId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new LockedPayment(rs.getInt("BookingID"), rs.getString("PaymentStatus"),
                        rs.getBigDecimal("Amount"), rs.getBigDecimal("TotalAmount")) : null;
            }
        }
    }

    public void markPaid(Connection conn, int paymentId) throws SQLException {
        update(conn, "UPDATE Payments SET PaymentStatus = 'PAID', PaidAt = SYSDATETIME() WHERE PaymentID = ?", paymentId);
    }

    /** Cac giao dich con cho (PENDING / TIMEOUT) khac cua don -> CANCELLED. */
    public void cancelOtherPending(Connection conn, int bookingId, int paidPaymentId) throws SQLException {
        cancelOthers(conn, "BookingID", bookingId, paidPaymentId);
    }

    /** Tat ca giao dich cua don, cu nhat truoc. */
    public List<PaymentTransaction> getTransactions(int bookingId) throws SQLException {
        return transactions("BookingID", bookingId);
    }

    /** Tat ca giao dich cua don bap nuoc le, cu nhat truoc. */
    public List<PaymentTransaction> getFnbTransactions(int orderId) throws SQLException {
        return transactions("FnbOrderID", orderId);
    }

    /**
     * Giao dich ve + bap nuoc le, moi nhat truoc.
     * @param centreId null = moi rap; status / method null = khong loc
     */
    public List<Payment> getTransactionsForTracking(Integer centreId, String status, String method, int days)
            throws SQLException {
        // Gom ca giao dich ban bap nuoc le (FnbOrderID, khong co BookingID): BookingType = 'FNB'
        String sql = "SELECT TOP 300 p.PaymentID, p.TransactionCode, p.PaymentMethod, p.Amount, p.PaymentStatus, "
                   + "  p.CreatedAt, p.PaidAt, COALESCE(b.BookingID, fo.FnbOrderID) AS BookingID, "
                   + "  COALESCE(b.BookingCode, fo.OrderCode) AS BookingCode, COALESCE(b.BookingType, 'FNB') AS BookingType, "
                   + "  COALESCE(b.BookingStatus, fo.OrderStatus) AS BookingStatus, "
                   + "  u.FullName, u.Phone, br.CentreName, COALESCE(m.Title, N'Bán bắp nước lẻ') AS Title "
                   + "FROM Payments p "
                   + "LEFT JOIN Bookings b ON b.BookingID = p.BookingID "
                   + "LEFT JOIN FnbOrders fo ON fo.FnbOrderID = p.FnbOrderID "
                   + "LEFT JOIN Users u ON u.UserID = b.UserID "
                   + "LEFT JOIN Showtimes s ON s.ShowtimeID = b.ShowtimeID "
                   + "LEFT JOIN Movies m ON m.MovieID = s.MovieID "
                   + "LEFT JOIN Halls h ON h.HallID = s.HallID "
                   + "JOIN Centres br ON br.CentreID = COALESCE(h.CentreID, fo.CentreID) "
                   + "WHERE p.CreatedAt > DATEADD(DAY, ?, SYSDATETIME()) "
                   + "  AND (? IS NULL OR br.CentreID = ?) "
                   + "  AND (? IS NULL OR p.PaymentStatus = ?) "
                   + "  AND (? IS NULL OR p.PaymentMethod = ?) "
                   + "ORDER BY p.CreatedAt DESC, p.PaymentID DESC";
        List<Payment> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, -days);
            setNullableInt(ps, 2, centreId);
            setNullableInt(ps, 3, centreId);
            ps.setString(4, status);
            ps.setString(5, status);
            ps.setString(6, method);
            ps.setString(7, method);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Payment(rs.getInt("PaymentID"), rs.getString("TransactionCode"),
                            rs.getString("PaymentMethod"), rs.getBigDecimal("Amount"), rs.getString("PaymentStatus"),
                            rs.getTimestamp("CreatedAt"), rs.getTimestamp("PaidAt"),
                            rs.getInt("BookingID"), rs.getString("BookingCode"), rs.getString("BookingType"),
                            rs.getString("BookingStatus"), rs.getString("FullName"), rs.getString("Phone"),
                            rs.getString("CentreName"), rs.getString("Title")));
                }
            }
        }
        return list;
    }

    // ---------------------------------------------------------------- ve

    public void insertTickets(Connection conn, List<NewTicket> tickets) throws SQLException {
        if (tickets.isEmpty()) return;
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Tickets (BookingSeatID, TicketCode, QRCode, TicketStatus) VALUES (?, ?, ?, 'VALID')")) {
            for (NewTicket t : tickets) {
                ps.setInt(1, t.bookingSeatId());
                ps.setString(2, t.ticketCode());
                ps.setString(3, t.qrCode());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public List<Ticket> getTickets(int bookingId) throws SQLException {
        String sql = "SELECT t.TicketCode, t.QRCode, t.TicketStatus, se.SeatRow, se.SeatNumber, se.SeatType "
                   + "FROM Tickets t JOIN BookingSeats bs ON bs.BookingSeatID = t.BookingSeatID "
                   + "JOIN Seats se ON se.SeatID = bs.SeatID "
                   + "WHERE bs.BookingID = ? ORDER BY se.SeatRow, se.SeatNumber";
        List<Ticket> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Ticket(rs.getString("TicketCode"), rs.getString("QRCode"),
                            rs.getString("SeatRow") + rs.getInt("SeatNumber"), rs.getString("SeatType"),
                            rs.getString("TicketStatus")));
                }
            }
        }
        return list;
    }

    // ---------------------------------------------------------------- giao dich bap nuoc le

    /** @return PaymentID */
    public int insertFnbPayment(Connection conn, int orderId, String transactionCode, String method,
            BigDecimal amount, BigDecimal cashReceived) throws SQLException {
        return insert(conn, "FnbOrderID", orderId, transactionCode, method, amount, cashReceived);
    }

    /** Giao dich VNPay dang cho cua don bap nuoc le -> CANCELLED. */
    public void cancelPendingQrForOrder(Connection conn, int orderId) throws SQLException {
        update(conn, "UPDATE Payments SET PaymentStatus = 'CANCELLED' "
                   + "WHERE FnbOrderID = ? AND PaymentMethod = 'QR' AND PaymentStatus = 'PENDING'", orderId);
    }

    /** Giao dich dang cho cua cac don bap nuoc le -> trang thai moi (CANCELLED / TIMEOUT). */
    public void updatePendingForOrders(Connection conn, List<Integer> orderIds, String newStatus) throws SQLException {
        updatePendingIn(conn, "FnbOrderID", orderIds, newStatus);
    }

    public FnbVnpayRef findFnbVnpay(String txnRef) throws SQLException {
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT PaymentID, FnbOrderID, Amount, PaymentStatus FROM Payments "
                   + "WHERE TransactionCode = ? AND PaymentMethod = 'QR' AND FnbOrderID IS NOT NULL")) {
            ps.setString(1, txnRef);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new FnbVnpayRef(rs.getInt("PaymentID"), rs.getInt("FnbOrderID"),
                        rs.getBigDecimal("Amount"), rs.getString("PaymentStatus")) : null;
            }
        }
    }

    /** Khoa giao dich bap nuoc le (UPDLOCK). null neu khong co / khong thuoc rap. @param centreId null = moi rap */
    public LockedFnbPayment lockFnbPayment(Connection conn, int paymentId, Integer centreId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT p.FnbOrderID, p.PaymentStatus FROM Payments p WITH (UPDLOCK, ROWLOCK) "
              + "JOIN FnbOrders o ON o.FnbOrderID = p.FnbOrderID "
              + "WHERE p.PaymentID = ? AND o.BookingID IS NULL AND (? IS NULL OR o.CentreID = ?)")) {
            ps.setInt(1, paymentId);
            setNullableInt(ps, 2, centreId);
            setNullableInt(ps, 3, centreId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new LockedFnbPayment(rs.getInt("FnbOrderID"), rs.getString("PaymentStatus")) : null;
            }
        }
    }

    /** Cac giao dich con cho khac cua don bap nuoc le -> CANCELLED. */
    public void cancelOtherPendingForOrder(Connection conn, int orderId, int paidPaymentId) throws SQLException {
        cancelOthers(conn, "FnbOrderID", orderId, paidPaymentId);
    }

    // ---------------------------------------------------------------- noi bo

    private static int insert(Connection conn, String ownerColumn, int ownerId, String transactionCode, String method,
            BigDecimal amount, BigDecimal cashReceived) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Payments (" + ownerColumn + ", TransactionCode, PaymentMethod, Amount, PaymentStatus, CashReceived) "
              + "VALUES (?, ?, ?, ?, 'PENDING', ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, ownerId);
            ps.setString(2, transactionCode);
            ps.setString(3, method);
            ps.setBigDecimal(4, amount);
            ps.setBigDecimal(5, cashReceived);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private static void updatePendingIn(Connection conn, String ownerColumn, List<Integer> ids, String newStatus)
            throws SQLException {
        if (ids.isEmpty()) return;
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE Payments SET PaymentStatus = ? WHERE PaymentStatus = 'PENDING' "
              + "AND " + ownerColumn + " IN (" + DbUtil.placeholders(ids.size()) + ")")) {
            ps.setString(1, newStatus);
            DbUtil.setInts(ps, 2, ids);
            ps.executeUpdate();
        }
    }

    private static void cancelOthers(Connection conn, String ownerColumn, int ownerId, int paidPaymentId)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE Payments SET PaymentStatus = 'CANCELLED' "
              + "WHERE " + ownerColumn + " = ? AND PaymentID <> ? AND PaymentStatus IN ('PENDING', 'TIMEOUT')")) {
            ps.setInt(1, ownerId);
            ps.setInt(2, paidPaymentId);
            ps.executeUpdate();
        }
    }

    private List<PaymentTransaction> transactions(String column, int id) throws SQLException {
        String sql = "SELECT PaymentID, TransactionCode, PaymentMethod, Amount, PaymentStatus, CreatedAt, PaidAt, CashReceived "
                   + "FROM Payments WHERE " + column + " = ? ORDER BY PaymentID";
        List<PaymentTransaction> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new PaymentTransaction(rs.getInt("PaymentID"), rs.getString("TransactionCode"),
                            rs.getString("PaymentMethod"), rs.getBigDecimal("Amount"), rs.getString("PaymentStatus"),
                            rs.getTimestamp("CreatedAt"), rs.getTimestamp("PaidAt"), rs.getBigDecimal("CashReceived")));
                }
            }
        }
        return list;
    }

    private static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) ps.setNull(index, Types.INTEGER);
        else ps.setInt(index, value);
    }

    private static void update(Connection conn, String sql, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
