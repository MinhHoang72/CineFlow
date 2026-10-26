package dao;

import model.entity.Payment;
import model.dto.booking.PaymentTransaction;
import model.entity.Ticket;
import utils.DBContext;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class PaymentDAO {

    public enum Status {
        SUCCESS,
        ALREADY_PAID,
        NOT_FOUND,
        /** Tien da nhan nhung don het han giu ghe va ghe da bi nguoi khac lay -> can hoan tien. */
        PAID_BUT_SEATS_LOST,
        /** Tien da nhan theo so tien cu, khach da doi bap nuoc / voucher sau khi tao giao dich -> can hoan tien. */
        PAID_AMOUNT_MISMATCH
    }

    public record Outcome(Status status, Integer bookingId) {}

    /** Giao dich VNPay (PaymentMethod = 'QR', TransactionCode = vnp_TxnRef). */
    public record VnpayRef(int paymentId, int bookingId, BigDecimal amount, String status) {}

    /**
     * Tao giao dich VNPay moi cho booking, huy giao dich VNPay dang cho truoc do (khach bam thanh toan lai).
     * @return TransactionCode, dung lam vnp_TxnRef
     */
    public String createVnpayPayment(int bookingId, String bookingCode, BigDecimal amount) throws SQLException {
        String txnRef = bookingCode + System.currentTimeMillis();
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                update(conn, "UPDATE Payments SET PaymentStatus = 'CANCELLED' "
                           + "WHERE BookingID = ? AND PaymentMethod = 'QR' AND PaymentStatus = 'PENDING'", bookingId);
                insertPending(conn, bookingId, txnRef, "QR", amount, null);
                conn.commit();
                return txnRef;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /** Giao dich VNPay theo vnp_TxnRef, null neu khong co. */
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

    /** VNPay bao giao dich khong thanh cong: PENDING -> FAILED. Don van giu ghe de khach thanh toan lai. */
    public void markFailed(int paymentId) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            update(conn, "UPDATE Payments SET PaymentStatus = 'FAILED' WHERE PaymentID = ? AND PaymentStatus = 'PENDING'",
                    paymentId);
        }
    }

    /** Trang thai giao dich VNPay moi nhat cua booking (PENDING/FAILED/...), null neu chua co. */
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

    /** Tat ca giao dich cua don, cu nhat truoc. */
    public List<PaymentTransaction> getTransactions(int bookingId) throws SQLException {
        return transactions("BookingID", bookingId);
    }

    /** Tat ca giao dich cua don bap nuoc le, cu nhat truoc. */
    public List<PaymentTransaction> getFnbTransactions(int orderId) throws SQLException {
        return transactions("FnbOrderID", orderId);
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

    /**
     * Tao giao dich tien mat (ban tai quay); nhan vien xac nhan ngay bang confirmPaid.
     * @param cashReceived so tien khach dua (>= amount), dung de tinh tien thoi va in hoa don
     * @return PaymentID
     */
    public int createCashPayment(int bookingId, String bookingCode, BigDecimal amount, BigDecimal cashReceived)
            throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            return insertPending(conn, bookingId, bookingCode + "-CASH-" + System.currentTimeMillis(), "CASH",
                    amount, cashReceived);
        }
    }

    private static int insertPending(Connection conn, int bookingId, String transactionCode, String method,
            BigDecimal amount, BigDecimal cashReceived) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Payments (BookingID, TransactionCode, PaymentMethod, Amount, PaymentStatus, CashReceived) "
              + "VALUES (?, ?, ?, ?, 'PENDING', ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, bookingId);
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

    /**
     * Ghi nhan da nhan tien (tien mat tai quay, hoac VNPay bao thanh cong) va xuat ve. Goi lap lai van an toan.
     * @param centreId chi nhanh cua nhan vien; null = moi chi nhanh (VNPay)
     */
    public Outcome confirmPaid(int paymentId, Integer centreId) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Outcome outcome = confirm(conn, paymentId, centreId);
                conn.commit();
                return outcome;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private Outcome confirm(Connection conn, int paymentId, Integer centreId) throws SQLException {
        int bookingId;
        String paymentStatus;
        boolean amountMatches;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT p.BookingID, p.PaymentStatus, CASE WHEN p.Amount = b.TotalAmount THEN 1 ELSE 0 END AS AmountMatches "
              + "FROM Payments p WITH (UPDLOCK, ROWLOCK) "
              + "JOIN Bookings b ON b.BookingID = p.BookingID "
              + "JOIN Showtimes s ON s.ShowtimeID = b.ShowtimeID JOIN Halls h ON h.HallID = s.HallID "
              + "WHERE p.PaymentID = ? AND (? IS NULL OR h.CentreID = ?)")) {
            ps.setInt(1, paymentId);
            setNullableInt(ps, 2, centreId);
            setNullableInt(ps, 3, centreId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return new Outcome(Status.NOT_FOUND, null);
                bookingId = rs.getInt("BookingID");
                paymentStatus = rs.getString("PaymentStatus");
                amountMatches = rs.getInt("AmountMatches") == 1;
            }
        }
        if ("PAID".equals(paymentStatus)) return new Outcome(Status.ALREADY_PAID, bookingId);

        update(conn, "UPDATE Payments SET PaymentStatus = 'PAID', PaidAt = SYSDATETIME() WHERE PaymentID = ?", paymentId);
        // Giao dich tao truoc khi khach doi bap nuoc / voucher: ghi nhan tien nhung khong xac nhan don
        if (!amountMatches) return new Outcome(Status.PAID_AMOUNT_MISMATCH, bookingId);

        try {
            // Thuong la HELD; RELEASED khi VNPay bao thanh cong sau khi don da het han giu ghe.
            update(conn, "UPDATE BookingSeats SET SeatStatus = 'CONFIRMED', HoldUntil = NULL "
                       + "WHERE BookingID = ? AND SeatStatus IN ('HELD', 'RELEASED', 'CANCELLED')", bookingId);
        } catch (SQLException e) {
            if (!BookingDAO.isDuplicateKey(e)) throw e;
            update(conn, "UPDATE Bookings SET BookingStatus = 'CANCELLED', UpdatedAt = SYSDATETIME() WHERE BookingID = ?", bookingId);
            FnbOrderDAO.cancelPendingForBookings(conn, "?", bookingId);
            return new Outcome(Status.PAID_BUT_SEATS_LOST, bookingId);
        }

        update(conn, "UPDATE Bookings SET BookingStatus = 'CONFIRMED', UpdatedAt = SYSDATETIME() WHERE BookingID = ?", bookingId);
        FnbOrderDAO.confirmForBooking(conn, bookingId);
        BookingVoucherDAO.recordUsage(conn, bookingId);
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE Payments SET PaymentStatus = 'CANCELLED' "
              + "WHERE BookingID = ? AND PaymentID <> ? AND PaymentStatus IN ('PENDING', 'TIMEOUT')")) {
            ps.setInt(1, bookingId);
            ps.setInt(2, paymentId);
            ps.executeUpdate();
        }
        issueTickets(conn, bookingId);
        return new Outcome(Status.SUCCESS, bookingId);
    }

    /**
     * Theo doi giao dich thanh toan ve (VNPay + tien mat), moi nhat truoc.
     * @param centreId null = moi chi nhanh
     * @param status null = moi trang thai
     * @param method null = moi phuong thuc (QR = VNPay, CASH = tien mat)
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
        try (Connection conn = DBContext.getConnection()) {
            BookingDAO.releaseExpiredHolds(conn);
            FnbOrderDAO.expireStandalonePending(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
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
        }
        return list;
    }

    private void issueTickets(Connection conn, int bookingId) throws SQLException {
        List<Integer> bookingSeatIds = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT bs.BookingSeatID FROM BookingSeats bs "
              + "WHERE bs.BookingID = ? AND bs.SeatStatus = 'CONFIRMED' "
              + "AND NOT EXISTS (SELECT 1 FROM Tickets t WHERE t.BookingSeatID = bs.BookingSeatID)")) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) bookingSeatIds.add(rs.getInt(1));
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Tickets (BookingSeatID, TicketCode, QRCode, TicketStatus) VALUES (?, ?, ?, 'VALID')")) {
            for (int id : bookingSeatIds) {
                String code = BookingDAO.randomCode("TK", 10);
                ps.setInt(1, id);
                ps.setString(2, code);
                ps.setString(3, "CINEFLOW-TICKET:" + code);
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

    // ---------------------------------------------------------------- ban bap nuoc le tai quay

    /** Giao dich VNPay cua don bap nuoc le (FnbOrderID co gia tri, BookingID = NULL). */
    public record FnbVnpayRef(int paymentId, int orderId, BigDecimal amount, String status) {}

    /** Giao dich tien mat cho don bap nuoc le. @return PaymentID */
    public int createFnbCashPayment(int orderId, String orderCode, BigDecimal amount, BigDecimal cashReceived)
            throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            return insertFnbPending(conn, orderId, orderCode + "-CASH-" + System.currentTimeMillis(), "CASH",
                    amount, cashReceived);
        }
    }

    /** Giao dich VNPay moi cho don bap nuoc le, huy giao dich VNPay dang cho truoc do. @return vnp_TxnRef */
    public String createFnbVnpayPayment(int orderId, String orderCode, BigDecimal amount) throws SQLException {
        String txnRef = orderCode + System.currentTimeMillis();
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                update(conn, "UPDATE Payments SET PaymentStatus = 'CANCELLED' "
                           + "WHERE FnbOrderID = ? AND PaymentMethod = 'QR' AND PaymentStatus = 'PENDING'", orderId);
                insertFnbPending(conn, orderId, txnRef, "QR", amount, null);
                conn.commit();
                return txnRef;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /** Giao dich VNPay cua don bap nuoc le theo vnp_TxnRef, null neu khong phai. */
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

    /**
     * Ghi nhan da thu tien don bap nuoc le (tien mat, hoac VNPay bao thanh cong): giao dich PAID,
     * don -> PREPARING va tru ton kho. Goi lap lai van an toan.
     * @param centreId rap cua nhan vien; null = moi rap (VNPay)
     * @return SUCCESS, ALREADY_PAID hoac NOT_FOUND
     */
    public Status confirmFnbPaid(int paymentId, Integer centreId) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int orderId;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT p.FnbOrderID, p.PaymentStatus FROM Payments p WITH (UPDLOCK, ROWLOCK) "
                      + "JOIN FnbOrders o ON o.FnbOrderID = p.FnbOrderID "
                      + "WHERE p.PaymentID = ? AND o.BookingID IS NULL AND (? IS NULL OR o.CentreID = ?)")) {
                    ps.setInt(1, paymentId);
                    setNullableInt(ps, 2, centreId);
                    setNullableInt(ps, 3, centreId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return Status.NOT_FOUND;
                        }
                        if ("PAID".equals(rs.getString("PaymentStatus"))) {
                            conn.rollback();
                            return Status.ALREADY_PAID;
                        }
                        orderId = rs.getInt("FnbOrderID");
                    }
                }
                update(conn, "UPDATE Payments SET PaymentStatus = 'PAID', PaidAt = SYSDATETIME() WHERE PaymentID = ?", paymentId);
                FnbOrderDAO.confirmStandalone(conn, orderId);
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE Payments SET PaymentStatus = 'CANCELLED' "
                      + "WHERE FnbOrderID = ? AND PaymentID <> ? AND PaymentStatus IN ('PENDING', 'TIMEOUT')")) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, paymentId);
                    ps.executeUpdate();
                }
                conn.commit();
                return Status.SUCCESS;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private static int insertFnbPending(Connection conn, int orderId, String transactionCode, String method,
            BigDecimal amount, BigDecimal cashReceived) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Payments (FnbOrderID, TransactionCode, PaymentMethod, Amount, PaymentStatus, CashReceived) "
              + "VALUES (?, ?, ?, ?, 'PENDING', ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, orderId);
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
