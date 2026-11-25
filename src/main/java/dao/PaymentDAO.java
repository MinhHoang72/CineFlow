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

    /* ============================== MỤC LỤC ==============================
     * Tìm nhanh: Ctrl + F tên hàm. Mỗi hàm có dòng ">> Dùng ở: ... | Loại: ĐỌC / GHI" trong chú thích.
     * Bấm Thanh toán VNPay ... cancelPendingQr, insertBookingPayment
     * VNPay báo về ........... findVnpay, markFailed, lockBookingPayment, markPaid,
     *                          bookingStillMatchesShowtime, cancelOtherPending, insertTickets
     * Trang thất bại ......... latestQrStatus
     * Trang chi tiết đơn ..... getTickets, getTransactions
     * Hết hạn / huỷ đơn ...... updatePendingForBookings
     * Bắp nước lẻ tại quầy ... insertFnbPayment, cancelPendingQrForOrder, updatePendingForOrders, findFnbVnpay,
     *                          lockFnbPayment, cancelOtherPendingForOrder, getFnbTransactions
     * Nhân viên theo dõi ..... getTransactionsForTracking
     * Hàm nội bộ dùng chung (đơn vé / đơn lẻ): insert, updatePendingIn, cancelOthers, transactions
     * ===================================================================== */

    /**
     * Giao dịch VNPay của đơn vé (PaymentMethod = 'QR', TransactionCode = vnp_TxnRef): id, đơn, số tiền, trạng thái.
     */
    public record VnpayRef(int paymentId, int bookingId, BigDecimal amount, String status) {}

    /** Giao dịch VNPay của đơn bắp nước lẻ (FnbOrderID có giá trị, BookingID = NULL). */
    public record FnbVnpayRef(int paymentId, int orderId, BigDecimal amount, String status) {}

    /** Giao dịch vé đã khoá để xác nhận, kèm tổng tiền hiện tại của đơn (để so số tiền). */
    public record LockedPayment(int bookingId, String status, BigDecimal amount, BigDecimal bookingTotal) {}

    /** Giao dịch bắp nước lẻ đã khoá để xác nhận. */
    public record LockedFnbPayment(int orderId, String status) {}

    /** Dữ liệu một vé sắp ghi: ghế (BookingSeatID), mã vé, chuỗi QR lưu trong CSDL. */
    public record NewTicket(int bookingSeatId, String ticketCode, String qrCode) {}

    // ---------------------------------------------------------------- giao dich ve

    /**
     * Ghi giao dịch PENDING cho đơn vé (VNPay hoặc tiền mặt).
     * @return PaymentID
     * >> Dùng ở: bấm Thanh toán VNPay / thu tiền mặt - tạo giao dịch (createVnpayPayment, payCash) | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    public int insertBookingPayment(Connection conn, int bookingId, String transactionCode, String method,
            BigDecimal amount, BigDecimal cashReceived) throws SQLException {
        return insert(conn, "BookingID", bookingId, transactionCode, method, amount, cashReceived);
    }

    /**
     * Giao dịch VNPay đang chờ của đơn -> CANCELLED (khách tạo giao dịch mới khi thanh toán lại).
     * >> Dùng ở: bấm Thanh toán VNPay - huỷ giao dịch chờ cũ (PaymentService.createVnpayPayment) | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    public void cancelPendingQr(Connection conn, int bookingId) throws SQLException {
        update(conn, """
                UPDATE Payments SET PaymentStatus = 'CANCELLED'
                WHERE BookingID = ? AND PaymentMethod = 'QR' AND PaymentStatus = 'PENDING'
                """, bookingId);
    }

    /**
     * Giao dịch đang chờ của các đơn vé -> trạng thái mới (CANCELLED khi huỷ đơn, TIMEOUT khi hết hạn giữ ghế).
     * >> Dùng ở: hết hạn / huỷ đơn - giao dịch chờ -> TIMEOUT / CANCELLED (releaseExpiredHolds, cancelPending) | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    public void updatePendingForBookings(Connection conn, List<Integer> bookingIds, String newStatus) throws SQLException {
        updatePendingIn(conn, "BookingID", bookingIds, newStatus);
    }

    /**
     * Tìm giao dịch VNPay của đơn vé theo mã giao dịch (vnp_TxnRef); không có thì null.
     * >> Dùng ở: VNPay báo về - tìm giao dịch theo vnp_TxnRef (PaymentService.processVnpay) | Loại: ĐỌC (SELECT)
     */
    public VnpayRef findVnpay(String txnRef) throws SQLException {
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     """
                             SELECT PaymentID, BookingID, Amount, PaymentStatus FROM Payments
                             WHERE TransactionCode = ? AND PaymentMethod = 'QR' AND BookingID IS NOT NULL
                             """)) {
            ps.setString(1, txnRef);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new VnpayRef(rs.getInt("PaymentID"), rs.getInt("BookingID"),
                        rs.getBigDecimal("Amount"), rs.getString("PaymentStatus")) : null;
            }
        }
    }

    /**
     * Giao dịch PENDING -> FAILED (VNPay báo không thành công).
     * >> Dùng ở: VNPay báo thất bại - giao dịch -> FAILED (PaymentService.processVnpay) | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    public void markFailed(int paymentId) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            update(conn, """
                    UPDATE Payments SET PaymentStatus = 'FAILED' WHERE PaymentID = ? AND PaymentStatus = 'PENDING'
                    """,
                    paymentId);
        }
    }

    /**
     * Trạng thái giao dịch VNPay mới nhất của đơn (để biết lý do thanh toán thất bại); chưa có thì null.
     * >> Dùng ở: trang thanh toán thất bại - lý do (PaymentService.getFailureReason) | Loại: ĐỌC (SELECT)
     */
    public String latestQrStatus(int bookingId) throws SQLException {
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     """
                             SELECT TOP 1 PaymentStatus FROM Payments WHERE BookingID = ? AND PaymentMethod = 'QR'
                             ORDER BY PaymentID DESC
                             """)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /**
     * Khoá dòng giao dịch vé (UPDLOCK) trong transaction để xác nhận, kèm tổng tiền đơn.
     * Khoá giúp hai yêu cầu xác nhận cùng lúc (return URL và IPN) không chạy chồng nhau.
     * @param centreId null = không giới hạn rạp; có giá trị thì giao dịch phải thuộc rạp đó
     * @return null nếu không có hoặc không thuộc rạp
     * >> Dùng ở: VNPay báo thành công / thu tiền mặt - khoá trước khi xác nhận (PaymentService.confirmPaid) | Loại: ĐỌC + KHOÁ DÒNG (UPDLOCK)
     */
    public LockedPayment lockBookingPayment(Connection conn, int paymentId, Integer centreId) throws SQLException {
        // Serialize payment confirmation with manager edits/cancellation and seat holds.
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT s.ShowtimeID FROM Showtimes s WITH(UPDLOCK,HOLDLOCK) WHERE s.ShowtimeID=(SELECT b.ShowtimeID FROM Payments p JOIN Bookings b ON b.BookingID=p.BookingID WHERE p.PaymentID=?)
                """)) {
            ps.setInt(1, paymentId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) { /* hold until commit */ } }
        }
        try (PreparedStatement ps = conn.prepareStatement(
                """
                        SELECT p.BookingID, p.PaymentStatus, p.Amount, b.TotalAmount
                        FROM Payments p WITH (UPDLOCK, ROWLOCK)
                        JOIN Bookings b ON b.BookingID = p.BookingID
                        JOIN Showtimes s ON s.ShowtimeID = b.ShowtimeID JOIN Halls h ON h.HallID = s.HallID
                        WHERE p.PaymentID = ? AND (? IS NULL OR h.CentreID = ?)
                        """)) {
            ps.setInt(1, paymentId);
            setNullableInt(ps, 2, centreId);
            setNullableInt(ps, 3, centreId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new LockedPayment(rs.getInt("BookingID"), rs.getString("PaymentStatus"),
                        rs.getBigDecimal("Amount"), rs.getBigDecimal("TotalAmount")) : null;
            }
        }
    }

    /**
     * Giao dịch -> PAID và ghi thời điểm thanh toán (PaidAt).
     * >> Dùng ở: xác nhận thanh toán - giao dịch -> PAID (PaymentService.confirmPaid) | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    public void markPaid(Connection conn, int paymentId) throws SQLException {
        update(conn, """
                UPDATE Payments SET PaymentStatus = 'PAID', PaidAt = SYSDATETIME() WHERE PaymentID = ?
                """, paymentId);
    }

    /**
     * Đơn còn hợp lệ để xác nhận không: đơn chưa huỷ; suất vẫn SCHEDULED, chưa chiếu, đang mở bán;
     * phim đang chiếu; rạp ACTIVE; phòng AVAILABLE; phim còn được phân phối cho rạp.
     * Sai một điều kiện thì confirmPaid huỷ đơn (tiền về muộn sau khi suất bị sửa hoặc ngừng bán).
     * >> Dùng ở: xác nhận thanh toán - suất còn hợp lệ không (PaymentService.confirmPaid) | Loại: ĐỌC (SELECT)
     */
    public boolean bookingStillMatchesShowtime(Connection conn, int bookingId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT 1 FROM Bookings b JOIN Showtimes s ON s.ShowtimeID=b.ShowtimeID JOIN Halls h ON h.HallID=s.HallID JOIN Movies m ON m.MovieID=s.MovieID JOIN Centres c ON c.CentreID=h.CentreID WHERE b.BookingID=? AND b.BookingStatus<>'CANCELLED' AND s.Status='SCHEDULED' AND s.StartTime>SYSDATETIME() AND s.SaleState='OPEN' AND m.Status='NOW_SHOWING' AND c.Status='ACTIVE' AND h.Status='AVAILABLE' AND EXISTS (SELECT 1 FROM CentreMovies cm WHERE cm.MovieID=s.MovieID AND cm.CentreID=c.CentreID AND cm.IsActive=1 AND cm.Status='ACTIVE')
                """)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    /**
     * Các giao dịch còn chờ khác (PENDING / TIMEOUT) của đơn -> CANCELLED, sau khi một giao dịch đã PAID.
     * >> Dùng ở: xác nhận thanh toán - huỷ giao dịch chờ khác của đơn (PaymentService.confirmPaid) | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    public void cancelOtherPending(Connection conn, int bookingId, int paidPaymentId) throws SQLException {
        cancelOthers(conn, "BookingID", bookingId, paidPaymentId);
    }

    /**
     * Tất cả giao dịch của đơn vé, cũ nhất trước.
     * >> Dùng ở: trang chi tiết đơn - bảng Thanh toán (PaymentService.getTransactions) | Loại: ĐỌC (SELECT)
     */
    public List<PaymentTransaction> getTransactions(int bookingId) throws SQLException {
        return transactions("BookingID", bookingId);
    }

    /**
     * Tất cả giao dịch của đơn bắp nước lẻ, cũ nhất trước.
     * >> Dùng ở: hoá đơn bắp nước lẻ (FnbSaleService) | Loại: ĐỌC (SELECT)
     */
    public List<PaymentTransaction> getFnbTransactions(int orderId) throws SQLException {
        return transactions("FnbOrderID", orderId);
    }

    /**
     * Giao dịch vé và bắp nước lẻ cho trang theo dõi giao dịch, mới nhất trước.
     * @param centreId null = mọi rạp; status, method null = không lọc; days = số ngày gần nhất
     * >> Dùng ở: nhân viên - trang theo dõi giao dịch /staff/payments | Loại: ĐỌC (SELECT)
     */
    public List<Payment> getTransactionsForTracking(Integer centreId, String status, String method, int days)
            throws SQLException {
        // Gom ca giao dich ban bap nuoc le (FnbOrderID, khong co BookingID): BookingType = 'FNB'
        String sql = """
                SELECT TOP 300 p.PaymentID, p.TransactionCode, p.PaymentMethod, p.Amount, p.PaymentStatus,
                  p.CreatedAt, p.PaidAt, COALESCE(b.BookingID, fo.FnbOrderID) AS BookingID,
                  COALESCE(b.BookingCode, fo.OrderCode) AS BookingCode, COALESCE(b.BookingType, 'FNB') AS BookingType,
                  COALESCE(b.BookingStatus, fo.OrderStatus) AS BookingStatus,
                  u.FullName, u.Phone, br.CentreName, COALESCE(m.Title, N'Bán bắp nước lẻ') AS Title
                FROM Payments p
                LEFT JOIN Bookings b ON b.BookingID = p.BookingID
                LEFT JOIN FnbOrders fo ON fo.FnbOrderID = p.FnbOrderID
                LEFT JOIN Users u ON u.UserID = b.UserID
                LEFT JOIN Showtimes s ON s.ShowtimeID = b.ShowtimeID
                LEFT JOIN Movies m ON m.MovieID = s.MovieID
                LEFT JOIN Halls h ON h.HallID = s.HallID
                JOIN Centres br ON br.CentreID = COALESCE(h.CentreID, fo.CentreID)
                WHERE p.CreatedAt > DATEADD(DAY, ?, SYSDATETIME())
                  AND (? IS NULL OR br.CentreID = ?)
                  AND (? IS NULL OR p.PaymentStatus = ?)
                  AND (? IS NULL OR p.PaymentMethod = ?)
                ORDER BY p.CreatedAt DESC, p.PaymentID DESC
                """;
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

    /**
     * Ghi các vé mới vào bảng Tickets (trạng thái VALID), mỗi vé gắn với một ghế đã CONFIRMED.
     * >> Dùng ở: thanh toán xong - xuất vé, mỗi ghế một vé (PaymentService.issueTickets) | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    public void insertTickets(Connection conn, List<NewTicket> tickets) throws SQLException {
        if (tickets.isEmpty()) return;
        try (PreparedStatement ps = conn.prepareStatement(
                """
                        INSERT INTO Tickets (BookingSeatID, TicketCode, QRCode, TicketStatus) VALUES (?, ?, ?, 'VALID')
                        """)) {
            for (NewTicket t : tickets) {
                ps.setInt(1, t.bookingSeatId());
                ps.setString(2, t.ticketCode());
                ps.setString(3, t.qrCode());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Danh sách vé của đơn kèm thông tin phim, rạp, phòng, ghế để hiện và in vé.
     * >> Dùng ở: trang chi tiết đơn / in vé tại quầy - vé điện tử (PaymentService.getTickets) | Loại: ĐỌC (SELECT)
     */
    public List<Ticket> getTickets(int bookingId) throws SQLException {
        String sql = """
                SELECT t.TicketCode, t.QRCode, t.TicketStatus, se.SeatRow, se.SeatNumber, se.SeatType
                FROM Tickets t JOIN BookingSeats bs ON bs.BookingSeatID = t.BookingSeatID
                JOIN Seats se ON se.SeatID = bs.SeatID
                WHERE bs.BookingID = ? ORDER BY se.SeatRow, se.SeatNumber
                """;
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

    /**
     * Ghi giao dịch PENDING cho đơn bắp nước lẻ (cột FnbOrderID).
     * @return PaymentID
     * >> Dùng ở: bắp nước lẻ tại quầy - tạo giao dịch (FnbSaleService) | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    public int insertFnbPayment(Connection conn, int orderId, String transactionCode, String method,
            BigDecimal amount, BigDecimal cashReceived) throws SQLException {
        return insert(conn, "FnbOrderID", orderId, transactionCode, method, amount, cashReceived);
    }

    /**
     * Giao dịch VNPay đang chờ của đơn bắp nước lẻ -> CANCELLED.
     * >> Dùng ở: bắp nước lẻ tại quầy - huỷ giao dịch VNPay chờ cũ (FnbSaleService) | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    public void cancelPendingQrForOrder(Connection conn, int orderId) throws SQLException {
        update(conn, """
                UPDATE Payments SET PaymentStatus = 'CANCELLED'
                WHERE FnbOrderID = ? AND PaymentMethod = 'QR' AND PaymentStatus = 'PENDING'
                """, orderId);
    }

    /**
     * Giao dịch đang chờ của các đơn bắp nước lẻ -> trạng thái mới (CANCELLED / TIMEOUT).
     * >> Dùng ở: bắp nước lẻ - dọn đơn quá hạn (FnbSaleService) | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    public void updatePendingForOrders(Connection conn, List<Integer> orderIds, String newStatus) throws SQLException {
        updatePendingIn(conn, "FnbOrderID", orderIds, newStatus);
    }

    /**
     * Tìm giao dịch VNPay của đơn bắp nước lẻ theo mã giao dịch; không có thì null.
     * >> Dùng ở: VNPay báo về cho đơn bắp nước lẻ (PaymentService.processFnbVnpay, findFnbOrderId) | Loại: ĐỌC (SELECT)
     */
    public FnbVnpayRef findFnbVnpay(String txnRef) throws SQLException {
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     """
                             SELECT PaymentID, FnbOrderID, Amount, PaymentStatus FROM Payments
                             WHERE TransactionCode = ? AND PaymentMethod = 'QR' AND FnbOrderID IS NOT NULL
                             """)) {
            ps.setString(1, txnRef);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new FnbVnpayRef(rs.getInt("PaymentID"), rs.getInt("FnbOrderID"),
                        rs.getBigDecimal("Amount"), rs.getString("PaymentStatus")) : null;
            }
        }
    }

    /**
     * Khoá dòng giao dịch bắp nước lẻ (UPDLOCK) để xác nhận.
     * @param centreId null = không giới hạn rạp
     * @return null nếu không có hoặc không thuộc rạp
     * >> Dùng ở: xác nhận thanh toán đơn bắp nước lẻ (FnbSaleService.confirmPaid) | Loại: ĐỌC + KHOÁ DÒNG (UPDLOCK)
     */
    public LockedFnbPayment lockFnbPayment(Connection conn, int paymentId, Integer centreId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                        SELECT p.FnbOrderID, p.PaymentStatus FROM Payments p WITH (UPDLOCK, ROWLOCK)
                        JOIN FnbOrders o ON o.FnbOrderID = p.FnbOrderID
                        WHERE p.PaymentID = ? AND o.BookingID IS NULL AND (? IS NULL OR o.CentreID = ?)
                        """)) {
            ps.setInt(1, paymentId);
            setNullableInt(ps, 2, centreId);
            setNullableInt(ps, 3, centreId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new LockedFnbPayment(rs.getInt("FnbOrderID"), rs.getString("PaymentStatus")) : null;
            }
        }
    }

    /**
     * Các giao dịch còn chờ khác của đơn bắp nước lẻ -> CANCELLED, sau khi một giao dịch đã PAID.
     * >> Dùng ở: xác nhận thanh toán đơn bắp nước lẻ - huỷ giao dịch chờ khác (FnbSaleService.confirmPaid) | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    public void cancelOtherPendingForOrder(Connection conn, int orderId, int paidPaymentId) throws SQLException {
        cancelOthers(conn, "FnbOrderID", orderId, paidPaymentId);
    }

    // ---------------------------------------------------------------- noi bo

    /**
     * Hàm ghi giao dịch dùng chung cho đơn vé (BookingID) và đơn bắp nước lẻ (FnbOrderID):
     * trạng thái PENDING, mã giao dịch, phương thức, số tiền, tiền khách đưa (null nếu VNPay).
     * @return PaymentID vừa tạo
     * >> Dùng ở: nội bộ - insertBookingPayment / insertFnbPayment (%s = BookingID hoặc FnbOrderID) | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    private static int insert(Connection conn, String ownerColumn, int ownerId, String transactionCode, String method,
            BigDecimal amount, BigDecimal cashReceived) throws SQLException {
        // %s = ownerColumn (BookingID hoặc FnbOrderID)
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO Payments (%s, TransactionCode, PaymentMethod, Amount, PaymentStatus, CashReceived)
                VALUES (?, ?, ?, ?, 'PENDING', ?)
                """.formatted(ownerColumn), Statement.RETURN_GENERATED_KEYS)) {
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

    /**
     * Đổi trạng thái các giao dịch PENDING của danh sách đơn (theo cột BookingID hoặc FnbOrderID).
     * >> Dùng ở: nội bộ - updatePendingForBookings / updatePendingForOrders | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    private static void updatePendingIn(Connection conn, String ownerColumn, List<Integer> ids, String newStatus)
            throws SQLException {
        if (ids.isEmpty()) return;
        // %1$s = ownerColumn (BookingID hoặc FnbOrderID), %2$s = "?, ?, ?" (một dấu ? cho mỗi đơn)
        try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE Payments SET PaymentStatus = ? WHERE PaymentStatus = 'PENDING'
                AND %1$s IN (%2$s)
                """.formatted(ownerColumn, DbUtil.placeholders(ids.size())))) {
            ps.setString(1, newStatus);
            DbUtil.setInts(ps, 2, ids);
            ps.executeUpdate();
        }
    }

    /**
     * Huỷ các giao dịch còn chờ khác của một đơn, trừ giao dịch vừa PAID.
     * >> Dùng ở: nội bộ - cancelOtherPending / cancelOtherPendingForOrder | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    private static void cancelOthers(Connection conn, String ownerColumn, int ownerId, int paidPaymentId)
            throws SQLException {
        // %s = ownerColumn (BookingID hoặc FnbOrderID)
        try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE Payments SET PaymentStatus = 'CANCELLED'
                WHERE %s = ? AND PaymentID <> ? AND PaymentStatus IN ('PENDING', 'TIMEOUT')
                """.formatted(ownerColumn))) {
            ps.setInt(1, ownerId);
            ps.setInt(2, paidPaymentId);
            ps.executeUpdate();
        }
    }

    /**
     * Đọc danh sách giao dịch của một đơn theo cột BookingID hoặc FnbOrderID, cũ nhất trước.
     * >> Dùng ở: nội bộ - getTransactions / getFnbTransactions | Loại: ĐỌC (SELECT)
     */
    private List<PaymentTransaction> transactions(String column, int id) throws SQLException {
        // %s = column (BookingID hoặc FnbOrderID)
        String sql = """
                SELECT PaymentID, TransactionCode, PaymentMethod, Amount, PaymentStatus, CreatedAt, PaidAt, CashReceived
                FROM Payments WHERE %s = ? ORDER BY PaymentID
                """.formatted(column);
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

    /** Gán tham số số nguyên có thể null vào PreparedStatement (null thì gán NULL của SQL). */
    private static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) ps.setNull(index, Types.INTEGER);
        else ps.setInt(index, value);
    }

    /** Chạy một câu UPDATE với một tham số id. */
    private static void update(Connection conn, String sql, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
