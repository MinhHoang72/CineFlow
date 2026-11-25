package dao;

import model.dto.booking.FnbLineDTO;
import model.dto.booking.FnbMenuItemDTO;
import model.dto.booking.FnbSaleDTO;
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
 * Truy cap FnbOrders / FnbOrderItems (bap nuoc kem don dat ve hoac ban le tai quay, BookingID = NULL),
 * ComboItems va ton kho CentreFnbInventory. Chi doc / ghi du lieu; kiem tra gio hang, ton kho, tru kho
 * nam o service.FnbOrderService.
 */
public class FnbOrderDAO {

    /* ============================== MỤC LỤC ==============================
     * Tìm nhanh: Ctrl + F tên hàm. Mỗi hàm có dòng ">> Dùng ở: ... | Loại: ĐỌC / GHI" trong chú thích.
     * Menu ................. getMenu (combo còn làm được + món lẻ còn hàng)
     * Kiểm kho ............. getComboParts, getAvailableStock
     * Lưu bắp nước kèm vé .. deletePendingForBooking, insertOrder, insertOrderLines
     * Đọc bắp nước của đơn . getLines, sumActiveTotalForBooking
     * Đã thanh toán ........ markBookingOrdersPreparing, getDemandForBooking, lockInventory, updateInventory
     * Huỷ / hết hạn đơn vé . cancelPendingForBookings
     * Bắp nước lẻ tại quầy . getStandaloneOrder, findStalePendingStandalone, cancelPendingStandalone,
     *                        markStandalonePreparing, getDemandForOrder
     * Hàm nội bộ ........... lines (đọc món), demand (tính trừ kho, tách combo)
     * ===================================================================== */

    /** Một thành phần của combo: món lẻ và số lượng. */
    public record ComboPart(int itemId, int quantity) {}

    /**
     * Tồn kho đang bán của một món tại rạp (0 nếu đã tắt bán hoặc chưa có trong kho), kèm tên món để báo lỗi.
     */
    public record AvailableStock(String itemName, int quantity) {}

    /** Tồn kho của một món tại rạp: số lượng và đang bán hay không. */
    public record Inventory(int quantity, boolean available) {}

    /** Số lượng một món lẻ cần trừ kho tại rạp (combo đã được tách thành món lẻ). */
    public record Demand(int centreId, int itemId, int quantity) {}

    /** Một dòng món của đơn: món lẻ (itemId) hoặc combo (comboId), số lượng, đơn giá. */
    public record OrderLine(Integer itemId, Integer comboId, int quantity, BigDecimal unitPrice) {}

    /** Mon le dang ban tai rap: ACTIVE, con hang trong kho cua rap. */
    private static final String MENU_ITEMS =
            """
                    SELECT i.FnbItemID, i.ItemName, i.Description, i.Price, inv.StockQuantity
                    FROM FnbItems i JOIN CentreFnbInventory inv ON inv.FnbItemID = i.FnbItemID AND inv.CentreID = i.CentreID
                    WHERE i.CentreID = ? AND i.Status = 'ACTIVE' AND inv.IsAvailable = 1 AND inv.StockQuantity > 0
                    ORDER BY i.ItemName
                    """;

    /** Combo dang ban: moi thanh phan con hang; so phan toi da = MIN(ton kho / so luong trong combo). */
    private static final String MENU_COMBOS =
            """
                    SELECT c.ComboID, c.ComboName, c.Price,
                      (SELECT STRING_AGG(CAST(ci.Quantity AS VARCHAR(5)) + N' ' + i.ItemName, N', ')
                         FROM ComboItems ci JOIN FnbItems i ON i.FnbItemID = ci.FnbItemID WHERE ci.ComboID = c.ComboID) AS Parts,
                      (SELECT MIN(CASE WHEN i.Status = 'ACTIVE' AND inv.IsAvailable = 1 THEN inv.StockQuantity / ci.Quantity ELSE 0 END)
                         FROM ComboItems ci JOIN FnbItems i ON i.FnbItemID = ci.FnbItemID
                         LEFT JOIN CentreFnbInventory inv ON inv.FnbItemID = ci.FnbItemID AND inv.CentreID = c.CentreID
                         WHERE ci.ComboID = c.ComboID) AS Available
                    FROM FnbCombos c WHERE c.CentreID = ? AND c.Status = 'ACTIVE'
                      AND EXISTS (SELECT 1 FROM ComboItems WHERE ComboID = c.ComboID)
                    ORDER BY c.ComboName
                    """;

    // ---------------------------------------------------------------- menu, ton kho

    /**
     * Combo và món lẻ đang bán tại rạp, combo đứng trước (mở kết nối riêng).
     * >> Dùng ở: trang chọn bắp nước / bán bắp nước lẻ - menu của rạp (BookingExtrasService.getMenu, FnbOrderService.loadMenu) | Loại: ĐỌC (SELECT)
     */
    public List<FnbMenuItemDTO> getMenu(int centreId) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            return getMenu(conn, centreId);
        }
    }

    /**
     * Như getMenu nhưng chạy trong transaction có sẵn (dùng khi tạo đơn để đọc menu và kiểm kho cùng lúc).
     * >> Dùng ở: trang chọn bắp nước / bán bắp nước lẻ - menu của rạp (BookingExtrasService.getMenu, FnbOrderService.loadMenu) | Loại: ĐỌC (SELECT)
     */
    public List<FnbMenuItemDTO> getMenu(Connection conn, int centreId) throws SQLException {
        List<FnbMenuItemDTO> menu = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(MENU_COMBOS)) {
            ps.setInt(1, centreId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int available = rs.getInt("Available");
                    if (available > 0) {
                        menu.add(new FnbMenuItemDTO(true, rs.getInt("ComboID"), rs.getString("ComboName"),
                                rs.getString("Parts"), rs.getBigDecimal("Price"), available));
                    }
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(MENU_ITEMS)) {
            ps.setInt(1, centreId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    menu.add(new FnbMenuItemDTO(false, rs.getInt("FnbItemID"), rs.getString("ItemName"),
                            rs.getString("Description"), rs.getBigDecimal("Price"), rs.getInt("StockQuantity")));
                }
            }
        }
        return menu;
    }

    /**
     * Các món lẻ và số lượng tạo nên một combo (để tách combo khi kiểm và trừ kho).
     * >> Dùng ở: lưu bắp nước - tách combo để kiểm kho (FnbOrderService.checkStock) | Loại: ĐỌC (SELECT)
     */
    public List<ComboPart> getComboParts(Connection conn, int comboId) throws SQLException {
        List<ComboPart> parts = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT FnbItemID, Quantity FROM ComboItems WHERE ComboID = ?
                """)) {
            ps.setInt(1, comboId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) parts.add(new ComboPart(rs.getInt(1), rs.getInt(2)));
            }
        }
        return parts;
    }

    /**
     * Tồn kho đang bán của một món tại rạp; null nếu món không tồn tại.
     * >> Dùng ở: lưu bắp nước - tồn kho của món (FnbOrderService.checkStock) | Loại: ĐỌC (SELECT)
     */
    public AvailableStock getAvailableStock(Connection conn, int centreId, int itemId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                        SELECT i.ItemName, ISNULL(inv.StockQuantity, 0) FROM FnbItems i
                        LEFT JOIN CentreFnbInventory inv ON inv.FnbItemID = i.FnbItemID AND inv.CentreID = ? AND inv.IsAvailable = 1
                        WHERE i.FnbItemID = ?
                        """)) {
            ps.setInt(1, centreId);
            ps.setInt(2, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new AvailableStock(rs.getString(1), rs.getInt(2)) : null;
            }
        }
    }

    /**
     * Khoá dòng tồn kho (UPDLOCK) của món tại rạp trước khi trừ; null nếu rạp chưa có món này trong kho.
     * >> Dùng ở: thanh toán xong - khoá dòng kho trước khi trừ (FnbOrderService.deductStock) | Loại: ĐỌC + KHOÁ DÒNG (UPDLOCK)
     */
    public Inventory lockInventory(Connection conn, int centreId, int itemId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                        SELECT StockQuantity, IsAvailable FROM CentreFnbInventory WITH (UPDLOCK, ROWLOCK)
                        WHERE CentreID = ? AND FnbItemID = ?
                        """)) {
            ps.setInt(1, centreId);
            ps.setInt(2, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Inventory(rs.getInt(1), rs.getBoolean(2)) : null;
            }
        }
    }

    /**
     * Ghi số lượng tồn kho mới và trạng thái đang bán của món tại rạp.
     * >> Dùng ở: thanh toán xong - trừ kho (FnbOrderService.deductStock) | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    public void updateInventory(Connection conn, int centreId, int itemId, int quantity, boolean available)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                        UPDATE CentreFnbInventory SET StockQuantity = ?, IsAvailable = ?, UpdatedAt = SYSDATETIME()
                        WHERE CentreID = ? AND FnbItemID = ?
                        """)) {
            ps.setInt(1, quantity);
            ps.setBoolean(2, available);
            ps.setInt(3, centreId);
            ps.setInt(4, itemId);
            ps.executeUpdate();
        }
    }

    /**
     * Nhu cầu trừ kho (đã tách combo thành món lẻ) của các đơn bắp nước PREPARING kèm đơn vé.
     * >> Dùng ở: thanh toán đơn vé xong - cần trừ bao nhiêu mỗi món (PaymentService.confirmPaid) | Loại: ĐỌC (SELECT)
     */
    public List<Demand> getDemandForBooking(Connection conn, int bookingId) throws SQLException {
        return demand(conn, "o.BookingID = ?", bookingId);
    }

    /**
     * Nhu cầu trừ kho (đã tách combo thành món lẻ) của một đơn bắp nước lẻ PREPARING.
     * >> Dùng ở: thanh toán đơn bắp nước lẻ xong - cần trừ bao nhiêu mỗi món (FnbSaleService.confirmPaid) | Loại: ĐỌC (SELECT)
     */
    public List<Demand> getDemandForOrder(Connection conn, int orderId) throws SQLException {
        return demand(conn, "o.FnbOrderID = ?", orderId);
    }

    // ---------------------------------------------------------------- don bap nuoc kem ve

    /**
     * Các dòng bắp nước của đơn vé (bỏ đơn bắp nước đã huỷ), để hiện ở trang xem lại đơn, chi tiết đơn, vé.
     * >> Dùng ở: checkout / chi tiết đơn / vé - các món của đơn (BookingService.getBooking) | Loại: ĐỌC (SELECT)
     */
    public List<FnbLineDTO> getLines(int bookingId) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            return lines(conn, "FROM FnbOrders o JOIN FnbOrderItems oi ON oi.FnbOrderID = o.FnbOrderID ",
                    "o.BookingID = ? AND o.OrderStatus <> 'CANCELLED'", bookingId);
        }
    }

    /**
     * Tổng tiền các đơn bắp nước chưa huỷ của đơn vé (dùng khi tính lại tổng tiền đơn).
     * >> Dùng ở: tính lại tổng tiền - tiền bắp nước (BookingService.recalculateTotal) | Loại: ĐỌC (SELECT)
     */
    public BigDecimal sumActiveTotalForBooking(Connection conn, int bookingId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                        SELECT ISNULL(SUM(TotalAmount), 0) FROM FnbOrders WHERE BookingID = ? AND OrderStatus <> 'CANCELLED'
                        """)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        }
    }

    /**
     * Xoá giỏ bắp nước chưa thanh toán của đơn vé (trước khi ghi giỏ mới).
     * >> Dùng ở: trang bắp nước bấm Tiếp tục - xoá giỏ cũ (BookingExtrasService.saveFnb) | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    public void deletePendingForBooking(Connection conn, int bookingId) throws SQLException {
        try (PreparedStatement items = conn.prepareStatement("""
                DELETE FROM FnbOrderItems WHERE FnbOrderID IN (SELECT FnbOrderID FROM FnbOrders WHERE BookingID = ? AND OrderStatus = 'PENDING')
                """);
             PreparedStatement orders = conn.prepareStatement("""
                     DELETE FROM FnbOrders WHERE BookingID = ? AND OrderStatus = 'PENDING'
                     """)) {
            items.setInt(1, bookingId);
            items.executeUpdate();
            orders.setInt(1, bookingId);
            orders.executeUpdate();
        }
    }

    /**
     * Đơn bắp nước kèm đơn vé (PENDING hoặc CANCELLED) -> PREPARING khi đã nhận tiền.
     * @return số đơn đã đổi (0 = không có bắp nước)
     * >> Dùng ở: thanh toán đơn vé xong - bắp nước -> PREPARING (PaymentService.confirmPaid) | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    public int markBookingOrdersPreparing(Connection conn, int bookingId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                        UPDATE FnbOrders SET OrderStatus = 'PREPARING', UpdatedAt = SYSDATETIME()
                        WHERE BookingID = ? AND OrderStatus IN ('PENDING', 'CANCELLED')
                        """)) {
            ps.setInt(1, bookingId);
            return ps.executeUpdate();
        }
    }

    /**
     * Đơn bắp nước PENDING của các đơn vé -> CANCELLED (đơn vé bị huỷ hoặc hết hạn).
     * >> Dùng ở: hết hạn / huỷ đơn vé - bắp nước -> CANCELLED (releaseExpiredHolds, cancelPending) | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    public void cancelPendingForBookings(Connection conn, List<Integer> bookingIds) throws SQLException {
        if (bookingIds.isEmpty()) return;
        // %s = "?, ?, ?" (một dấu ? cho mỗi đơn vé)
        try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE FnbOrders SET OrderStatus = 'CANCELLED', UpdatedAt = SYSDATETIME()
                WHERE OrderStatus = 'PENDING' AND BookingID IN (%s)
                """.formatted(DbUtil.placeholders(bookingIds.size())))) {
            DbUtil.setInts(ps, 1, bookingIds);
            ps.executeUpdate();
        }
    }

    // ---------------------------------------------------------------- tao don (kem ve hoac ban le)

    /**
     * Ghi đơn bắp nước mới, trạng thái PENDING.
     * @param bookingId null = đơn bắp nước lẻ không kèm vé
     * @return FnbOrderID
     * >> Dùng ở: lưu bắp nước / tạo đơn lẻ - tạo đơn PENDING (FnbOrderService.createOrder) | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    public int insertOrder(Connection conn, Integer bookingId, int centreId, Integer userId, String orderCode,
            String orderType, BigDecimal total) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                        INSERT INTO FnbOrders (BookingID, CentreID, UserID, OrderCode, OrderType, OrderStatus, TotalAmount)
                        VALUES (?, ?, ?, ?, ?, 'PENDING', ?)
                        """, Statement.RETURN_GENERATED_KEYS)) {
            if (bookingId == null) ps.setNull(1, Types.INTEGER);
            else ps.setInt(1, bookingId);
            ps.setInt(2, centreId);
            if (userId == null) ps.setNull(3, Types.INTEGER);
            else ps.setInt(3, userId);
            ps.setString(4, orderCode);
            ps.setString(5, orderType);
            ps.setBigDecimal(6, total);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    /**
     * Ghi các dòng món của đơn bắp nước (món lẻ hoặc combo, số lượng, đơn giá).
     * >> Dùng ở: lưu bắp nước / tạo đơn lẻ - ghi từng món (FnbOrderService.createOrder) | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    public void insertOrderLines(Connection conn, int orderId, List<OrderLine> lines) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                        INSERT INTO FnbOrderItems (FnbOrderID, FnbItemID, ComboID, Quantity, UnitPrice) VALUES (?, ?, ?, ?, ?)
                        """)) {
            for (OrderLine line : lines) {
                ps.setInt(1, orderId);
                if (line.itemId() == null) ps.setNull(2, Types.INTEGER);
                else ps.setInt(2, line.itemId());
                if (line.comboId() == null) ps.setNull(3, Types.INTEGER);
                else ps.setInt(3, line.comboId());
                ps.setInt(4, line.quantity());
                ps.setBigDecimal(5, line.unitPrice());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ---------------------------------------------------------------- don bap nuoc le

    /**
     * Lấy đơn bắp nước lẻ kèm các dòng món; không có thì null.
     * @param pendingMinutes thời gian chờ thanh toán, để tính số giây còn lại hiện cho nhân viên
     * >> Dùng ở: bán bắp nước lẻ - trang thu tiền / hoá đơn (FnbSaleService) | Loại: ĐỌC (SELECT)
     */
    public FnbSaleDTO getStandaloneOrder(int orderId, int pendingMinutes) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            FnbSaleDTO sale;
            try (PreparedStatement ps = conn.prepareStatement(
                    """
                            SELECT o.FnbOrderID, o.OrderCode, o.CentreID, c.CentreName, o.OrderStatus, o.TotalAmount, o.CreatedAt,
                              DATEDIFF(SECOND, SYSDATETIME(), DATEADD(MINUTE, ?, o.CreatedAt)) AS SecondsLeft
                            FROM FnbOrders o JOIN Centres c ON c.CentreID = o.CentreID
                            WHERE o.FnbOrderID = ? AND o.BookingID IS NULL
                            """)) {
                ps.setInt(1, pendingMinutes);
                ps.setInt(2, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    sale = new FnbSaleDTO(rs.getInt("FnbOrderID"), rs.getString("OrderCode"), rs.getInt("CentreID"),
                            rs.getString("CentreName"), rs.getString("OrderStatus"), rs.getBigDecimal("TotalAmount"),
                            rs.getTimestamp("CreatedAt"), Math.max(0, rs.getLong("SecondsLeft")));
                }
            }
            sale.setLines(lines(conn, "FROM FnbOrderItems oi ", "oi.FnbOrderID = ?", orderId));
            return sale;
        }
    }

    /**
     * Tìm các đơn bắp nước lẻ PENDING đã tạo quá số phút cho phép.
     * >> Dùng ở: bán bắp nước lẻ - đơn chờ quá 15 phút (FnbSaleService) | Loại: ĐỌC (SELECT)
     */
    public List<Integer> findStalePendingStandalone(Connection conn, int minutes) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                """
                        SELECT FnbOrderID FROM FnbOrders WHERE BookingID IS NULL AND OrderStatus = 'PENDING'
                        AND CreatedAt <= DATEADD(MINUTE, ?, SYSDATETIME())
                        """)) {
            ps.setInt(1, -minutes);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt(1));
            }
        }
        return ids;
    }

    /**
     * Đơn bắp nước lẻ còn PENDING -> CANCELLED.
     * @return số đơn đã huỷ
     * >> Dùng ở: bán bắp nước lẻ - huỷ đơn quá hạn (FnbSaleService) | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    public int cancelPendingStandalone(Connection conn, List<Integer> orderIds) throws SQLException {
        if (orderIds.isEmpty()) return 0;
        // %s = "?, ?, ?" (một dấu ? cho mỗi đơn bắp nước)
        try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE FnbOrders SET OrderStatus = 'CANCELLED', UpdatedAt = SYSDATETIME()
                WHERE BookingID IS NULL AND OrderStatus = 'PENDING'
                AND FnbOrderID IN (%s)
                """.formatted(DbUtil.placeholders(orderIds.size())))) {
            DbUtil.setInts(ps, 1, orderIds);
            return ps.executeUpdate();
        }
    }

    /**
     * Đơn bắp nước lẻ (PENDING hoặc CANCELLED) -> PREPARING khi đã nhận tiền.
     * @return số đơn đã đổi (0 = đã xác nhận trước đó)
     * >> Dùng ở: bán bắp nước lẻ - đã thu tiền -> PREPARING (FnbSaleService.confirmPaid) | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    public int markStandalonePreparing(Connection conn, int orderId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                        UPDATE FnbOrders SET OrderStatus = 'PREPARING', UpdatedAt = SYSDATETIME()
                        WHERE FnbOrderID = ? AND BookingID IS NULL AND OrderStatus IN ('PENDING', 'CANCELLED')
                        """)) {
            ps.setInt(1, orderId);
            return ps.executeUpdate();
        }
    }

    // ---------------------------------------------------------------- noi bo

    /**
     * Đọc các dòng món (tên, số lượng, đơn giá) theo điều kiện truyền vào; dùng chung cho đơn kèm vé và đơn lẻ.
     * >> Dùng ở: nội bộ - getLines / getStandaloneOrder (%1$s = FROM, %2$s = điều kiện) | Loại: ĐỌC (SELECT)
     */
    private static List<FnbLineDTO> lines(Connection conn, String from, String where, int id) throws SQLException {
        // %1$s = from (FROM ... JOIN ...), %2$s = where (điều kiện theo đơn vé hoặc đơn lẻ)
        String sql = """
                SELECT oi.FnbItemID, oi.ComboID, COALESCE(i.ItemName, c.ComboName) AS Name, oi.Quantity, oi.UnitPrice
                %1$s
                LEFT JOIN FnbItems i ON i.FnbItemID = oi.FnbItemID LEFT JOIN FnbCombos c ON c.ComboID = oi.ComboID
                WHERE %2$s
                ORDER BY CASE WHEN oi.ComboID IS NULL THEN 1 ELSE 0 END, Name
                """.formatted(from, where);
        List<FnbLineDTO> lines = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int comboId = rs.getInt("ComboID");
                    String key = rs.wasNull() ? "I" + rs.getInt("FnbItemID") : "C" + comboId;
                    lines.add(new FnbLineDTO(key, rs.getString("Name"), rs.getInt("Quantity"), rs.getBigDecimal("UnitPrice")));
                }
            }
        }
        return lines;
    }

    /**
     * Tính nhu cầu trừ kho theo món lẻ: món lẻ lấy trực tiếp, combo tách thành các món thành phần nhân số lượng.
     * >> Dùng ở: nội bộ - getDemandForBooking / getDemandForOrder (%s = điều kiện) | Loại: ĐỌC (SELECT)
     */
    private static List<Demand> demand(Connection conn, String orderFilter, int id) throws SQLException {
        // %s = orderFilter (o.BookingID = ? hoặc o.FnbOrderID = ?)
        String sql = """
                SELECT o.CentreID, x.FnbItemID, SUM(x.Qty) AS Qty FROM FnbOrders o JOIN (
                  SELECT FnbOrderID, FnbItemID, Quantity AS Qty FROM FnbOrderItems WHERE FnbItemID IS NOT NULL
                  UNION ALL
                  SELECT oi.FnbOrderID, ci.FnbItemID, oi.Quantity * ci.Quantity FROM FnbOrderItems oi
                  JOIN ComboItems ci ON ci.ComboID = oi.ComboID
                ) x ON x.FnbOrderID = o.FnbOrderID WHERE %s AND o.OrderStatus = 'PREPARING'
                GROUP BY o.CentreID, x.FnbItemID
                """.formatted(orderFilter);
        List<Demand> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new Demand(rs.getInt(1), rs.getInt(2), rs.getInt(3)));
            }
        }
        return list;
    }
}
