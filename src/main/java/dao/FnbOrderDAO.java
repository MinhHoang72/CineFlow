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

    /** Mot thanh phan cua combo. */
    public record ComboPart(int itemId, int quantity) {}

    /** Ton kho dang ban cua mot mon tai rap (0 neu tat ban / chua co trong kho). */
    public record AvailableStock(String itemName, int quantity) {}

    /** Ton kho cua mot mon tai rap. */
    public record Inventory(int quantity, boolean available) {}

    /** So luong mon le can tru kho (da tach combo thanh mon le). */
    public record Demand(int centreId, int itemId, int quantity) {}

    /** Mot dong mon cua don: mon le (itemId) hoac combo (comboId). */
    public record OrderLine(Integer itemId, Integer comboId, int quantity, BigDecimal unitPrice) {}

    /** Mon le dang ban tai rap: ACTIVE, con hang trong kho cua rap. */
    private static final String MENU_ITEMS =
            "SELECT i.FnbItemID, i.ItemName, i.Description, i.Price, inv.StockQuantity "
          + "FROM FnbItems i JOIN CentreFnbInventory inv ON inv.FnbItemID = i.FnbItemID AND inv.CentreID = i.CentreID "
          + "WHERE i.CentreID = ? AND i.Status = 'ACTIVE' AND inv.IsAvailable = 1 AND inv.StockQuantity > 0 "
          + "ORDER BY i.ItemName";

    /** Combo dang ban: moi thanh phan con hang; so phan toi da = MIN(ton kho / so luong trong combo). */
    private static final String MENU_COMBOS =
            "SELECT c.ComboID, c.ComboName, c.Price, "
          + "  (SELECT STRING_AGG(CAST(ci.Quantity AS VARCHAR(5)) + N' ' + i.ItemName, N', ') "
          + "     FROM ComboItems ci JOIN FnbItems i ON i.FnbItemID = ci.FnbItemID WHERE ci.ComboID = c.ComboID) AS Parts, "
          + "  (SELECT MIN(CASE WHEN i.Status = 'ACTIVE' AND inv.IsAvailable = 1 THEN inv.StockQuantity / ci.Quantity ELSE 0 END) "
          + "     FROM ComboItems ci JOIN FnbItems i ON i.FnbItemID = ci.FnbItemID "
          + "     LEFT JOIN CentreFnbInventory inv ON inv.FnbItemID = ci.FnbItemID AND inv.CentreID = c.CentreID "
          + "     WHERE ci.ComboID = c.ComboID) AS Available "
          + "FROM FnbCombos c WHERE c.CentreID = ? AND c.Status = 'ACTIVE' "
          + "  AND EXISTS (SELECT 1 FROM ComboItems WHERE ComboID = c.ComboID) "
          + "ORDER BY c.ComboName";

    // ---------------------------------------------------------------- menu, ton kho

    /** Combo + mon le dang ban tai rap (combo truoc). */
    public List<FnbMenuItemDTO> getMenu(int centreId) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            return getMenu(conn, centreId);
        }
    }

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

    public List<ComboPart> getComboParts(Connection conn, int comboId) throws SQLException {
        List<ComboPart> parts = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT FnbItemID, Quantity FROM ComboItems WHERE ComboID = ?")) {
            ps.setInt(1, comboId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) parts.add(new ComboPart(rs.getInt(1), rs.getInt(2)));
            }
        }
        return parts;
    }

    /** null neu khong co mon nay. */
    public AvailableStock getAvailableStock(Connection conn, int centreId, int itemId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT i.ItemName, ISNULL(inv.StockQuantity, 0) FROM FnbItems i "
              + "LEFT JOIN CentreFnbInventory inv ON inv.FnbItemID = i.FnbItemID AND inv.CentreID = ? AND inv.IsAvailable = 1 "
              + "WHERE i.FnbItemID = ?")) {
            ps.setInt(1, centreId);
            ps.setInt(2, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new AvailableStock(rs.getString(1), rs.getInt(2)) : null;
            }
        }
    }

    /** Khoa dong ton kho (UPDLOCK) de tru, null neu rap chua co mon nay trong kho. */
    public Inventory lockInventory(Connection conn, int centreId, int itemId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT StockQuantity, IsAvailable FROM CentreFnbInventory WITH (UPDLOCK, ROWLOCK) "
              + "WHERE CentreID = ? AND FnbItemID = ?")) {
            ps.setInt(1, centreId);
            ps.setInt(2, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Inventory(rs.getInt(1), rs.getBoolean(2)) : null;
            }
        }
    }

    public void updateInventory(Connection conn, int centreId, int itemId, int quantity, boolean available)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE CentreFnbInventory SET StockQuantity = ?, IsAvailable = ?, UpdatedAt = SYSDATETIME() "
              + "WHERE CentreID = ? AND FnbItemID = ?")) {
            ps.setInt(1, quantity);
            ps.setBoolean(2, available);
            ps.setInt(3, centreId);
            ps.setInt(4, itemId);
            ps.executeUpdate();
        }
    }

    /** Nhu cau mon le (gom thanh phan combo) cua cac don PREPARING cua booking. */
    public List<Demand> getDemandForBooking(Connection conn, int bookingId) throws SQLException {
        return demand(conn, "o.BookingID = ?", bookingId);
    }

    /** Nhu cau mon le (gom thanh phan combo) cua don bap nuoc le PREPARING. */
    public List<Demand> getDemandForOrder(Connection conn, int orderId) throws SQLException {
        return demand(conn, "o.FnbOrderID = ?", orderId);
    }

    // ---------------------------------------------------------------- don bap nuoc kem ve

    /** Cac dong bap nuoc cua don dat ve (bo don da huy). */
    public List<FnbLineDTO> getLines(int bookingId) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            return lines(conn, "FROM FnbOrders o JOIN FnbOrderItems oi ON oi.FnbOrderID = o.FnbOrderID ",
                    "o.BookingID = ? AND o.OrderStatus <> 'CANCELLED'", bookingId);
        }
    }

    /** Tong tien cac don bap nuoc chua huy cua booking. */
    public BigDecimal sumActiveTotalForBooking(Connection conn, int bookingId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT ISNULL(SUM(TotalAmount), 0) FROM FnbOrders WHERE BookingID = ? AND OrderStatus <> 'CANCELLED'")) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        }
    }

    /** Xoa gio bap nuoc chua thanh toan cua booking (truoc khi ghi gio moi). */
    public void deletePendingForBooking(Connection conn, int bookingId) throws SQLException {
        String pending = "SELECT FnbOrderID FROM FnbOrders WHERE BookingID = ? AND OrderStatus = 'PENDING'";
        try (PreparedStatement items = conn.prepareStatement("DELETE FROM FnbOrderItems WHERE FnbOrderID IN (" + pending + ")");
             PreparedStatement orders = conn.prepareStatement("DELETE FROM FnbOrders WHERE BookingID = ? AND OrderStatus = 'PENDING'")) {
            items.setInt(1, bookingId);
            items.executeUpdate();
            orders.setInt(1, bookingId);
            orders.executeUpdate();
        }
    }

    /** Don bap nuoc cua booking (PENDING / CANCELLED) -> PREPARING. @return so don da doi */
    public int markBookingOrdersPreparing(Connection conn, int bookingId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE FnbOrders SET OrderStatus = 'PREPARING', UpdatedAt = SYSDATETIME() "
              + "WHERE BookingID = ? AND OrderStatus IN ('PENDING', 'CANCELLED')")) {
            ps.setInt(1, bookingId);
            return ps.executeUpdate();
        }
    }

    /** Don bap nuoc PENDING cua cac booking -> CANCELLED. */
    public void cancelPendingForBookings(Connection conn, List<Integer> bookingIds) throws SQLException {
        if (bookingIds.isEmpty()) return;
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE FnbOrders SET OrderStatus = 'CANCELLED', UpdatedAt = SYSDATETIME() "
              + "WHERE OrderStatus = 'PENDING' AND BookingID IN (" + DbUtil.placeholders(bookingIds.size()) + ")")) {
            DbUtil.setInts(ps, 1, bookingIds);
            ps.executeUpdate();
        }
    }

    // ---------------------------------------------------------------- tao don (kem ve hoac ban le)

    /** @param bookingId null = don bap nuoc le khong kem ve. @return FnbOrderID */
    public int insertOrder(Connection conn, Integer bookingId, int centreId, Integer userId, String orderCode,
            String orderType, BigDecimal total) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO FnbOrders (BookingID, CentreID, UserID, OrderCode, OrderType, OrderStatus, TotalAmount) "
              + "VALUES (?, ?, ?, ?, ?, 'PENDING', ?)", Statement.RETURN_GENERATED_KEYS)) {
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

    public void insertOrderLines(Connection conn, int orderId, List<OrderLine> lines) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO FnbOrderItems (FnbOrderID, FnbItemID, ComboID, Quantity, UnitPrice) VALUES (?, ?, ?, ?, ?)")) {
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
     * Don bap nuoc le kem cac dong mon, null neu khong co.
     * @param pendingMinutes thoi gian cho thanh toan, de tinh so giay con lai
     */
    public FnbSaleDTO getStandaloneOrder(int orderId, int pendingMinutes) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            FnbSaleDTO sale;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT o.FnbOrderID, o.OrderCode, o.CentreID, c.CentreName, o.OrderStatus, o.TotalAmount, o.CreatedAt, "
                  + "  DATEDIFF(SECOND, SYSDATETIME(), DATEADD(MINUTE, ?, o.CreatedAt)) AS SecondsLeft "
                  + "FROM FnbOrders o JOIN Centres c ON c.CentreID = o.CentreID "
                  + "WHERE o.FnbOrderID = ? AND o.BookingID IS NULL")) {
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

    /** Don bap nuoc le PENDING tao truoc thoi diem hien tai qua so phut. */
    public List<Integer> findStalePendingStandalone(Connection conn, int minutes) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT FnbOrderID FROM FnbOrders WHERE BookingID IS NULL AND OrderStatus = 'PENDING' "
              + "AND CreatedAt <= DATEADD(MINUTE, ?, SYSDATETIME())")) {
            ps.setInt(1, -minutes);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt(1));
            }
        }
        return ids;
    }

    /** Don bap nuoc le con PENDING -> CANCELLED. @return so don da huy */
    public int cancelPendingStandalone(Connection conn, List<Integer> orderIds) throws SQLException {
        if (orderIds.isEmpty()) return 0;
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE FnbOrders SET OrderStatus = 'CANCELLED', UpdatedAt = SYSDATETIME() "
              + "WHERE BookingID IS NULL AND OrderStatus = 'PENDING' "
              + "AND FnbOrderID IN (" + DbUtil.placeholders(orderIds.size()) + ")")) {
            DbUtil.setInts(ps, 1, orderIds);
            return ps.executeUpdate();
        }
    }

    /** Don bap nuoc le (PENDING / CANCELLED) -> PREPARING. @return so don da doi (0 = da xac nhan truoc do) */
    public int markStandalonePreparing(Connection conn, int orderId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE FnbOrders SET OrderStatus = 'PREPARING', UpdatedAt = SYSDATETIME() "
              + "WHERE FnbOrderID = ? AND BookingID IS NULL AND OrderStatus IN ('PENDING', 'CANCELLED')")) {
            ps.setInt(1, orderId);
            return ps.executeUpdate();
        }
    }

    // ---------------------------------------------------------------- noi bo

    private static List<FnbLineDTO> lines(Connection conn, String from, String where, int id) throws SQLException {
        String sql = "SELECT oi.FnbItemID, oi.ComboID, COALESCE(i.ItemName, c.ComboName) AS Name, oi.Quantity, oi.UnitPrice "
                   + from
                   + "LEFT JOIN FnbItems i ON i.FnbItemID = oi.FnbItemID LEFT JOIN FnbCombos c ON c.ComboID = oi.ComboID "
                   + "WHERE " + where + " "
                   + "ORDER BY CASE WHEN oi.ComboID IS NULL THEN 1 ELSE 0 END, Name";
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

    private static List<Demand> demand(Connection conn, String orderFilter, int id) throws SQLException {
        String sql = "SELECT o.CentreID, x.FnbItemID, SUM(x.Qty) AS Qty FROM FnbOrders o JOIN ("
                   + "  SELECT FnbOrderID, FnbItemID, Quantity AS Qty FROM FnbOrderItems WHERE FnbItemID IS NOT NULL "
                   + "  UNION ALL "
                   + "  SELECT oi.FnbOrderID, ci.FnbItemID, oi.Quantity * ci.Quantity FROM FnbOrderItems oi "
                   + "  JOIN ComboItems ci ON ci.ComboID = oi.ComboID"
                   + ") x ON x.FnbOrderID = o.FnbOrderID WHERE " + orderFilter + " AND o.OrderStatus = 'PREPARING' "
                   + "GROUP BY o.CentreID, x.FnbItemID";
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
