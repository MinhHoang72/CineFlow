package dao;

import model.dto.booking.FnbLineDTO;
import model.dto.booking.FnbMenuItemDTO;
import utils.DBContext;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bap nuoc kem don dat ve (FnbOrders / FnbOrderItems gan BookingID).
 * Gio hang luu o FnbOrders PENDING; thanh toan xong -> PREPARING va tru ton kho; huy don -> CANCELLED.
 */
public class FnbOrderDAO {

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

    /** Combo + mon le dang ban tai rap (combo truoc). */
    public List<FnbMenuItemDTO> getMenu(int centreId) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            return getMenu(conn, centreId);
        }
    }

    static List<FnbMenuItemDTO> getMenu(Connection conn, int centreId) throws SQLException {
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

    /** Cac dong bap nuoc cua don (bo don da huy). */
    public static List<FnbLineDTO> getLines(Connection conn, int bookingId) throws SQLException {
        String sql = "SELECT oi.FnbItemID, oi.ComboID, COALESCE(i.ItemName, c.ComboName) AS Name, oi.Quantity, oi.UnitPrice "
                   + "FROM FnbOrders o JOIN FnbOrderItems oi ON oi.FnbOrderID = o.FnbOrderID "
                   + "LEFT JOIN FnbItems i ON i.FnbItemID = oi.FnbItemID LEFT JOIN FnbCombos c ON c.ComboID = oi.ComboID "
                   + "WHERE o.BookingID = ? AND o.OrderStatus <> 'CANCELLED' "
                   + "ORDER BY CASE WHEN oi.ComboID IS NULL THEN 1 ELSE 0 END, Name";
        List<FnbLineDTO> lines = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
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
     * Thay gio bap nuoc cua don dang giu ghe (gia lay theo menu hien tai) va tinh lai tong tien don.
     * @param cart key menu (I12 / C3) -> so luong; so luong 0 = bo mon
     * @throws IllegalArgumentException mon khong con ban / vuot ton kho / so luong khong hop le
     */
    public void replaceCart(int bookingId, int centreId, Integer userId, String orderType, Map<String, Integer> cart)
            throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Map<String, FnbMenuItemDTO> menu = new HashMap<>();
                for (FnbMenuItemDTO m : getMenu(conn, centreId)) menu.put(m.getKey(), m);

                Map<String, Integer> lines = new LinkedHashMap<>();
                for (Map.Entry<String, Integer> e : cart.entrySet()) {
                    int qty = e.getValue();
                    if (qty == 0) continue;
                    FnbMenuItemDTO item = menu.get(e.getKey());
                    if (item == null) throw new IllegalArgumentException("Có món vừa hết hàng hoặc ngừng bán, vui lòng chọn lại.");
                    if (qty < 0 || qty > FnbMenuItemDTO.MAX_PER_LINE) {
                        throw new IllegalArgumentException("Số lượng mỗi món từ 0 đến " + FnbMenuItemDTO.MAX_PER_LINE + ".");
                    }
                    lines.put(e.getKey(), qty);
                }
                checkStock(conn, centreId, lines, menu);

                deletePendingOrder(conn, bookingId);
                if (!lines.isEmpty()) insertOrder(conn, bookingId, centreId, userId, orderType, lines, menu);
                BookingDAO.recalculateTotal(conn, bookingId);
                BookingDAO.cancelPendingPayments(conn, bookingId);
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /** Tong nhu cau theo mon (mon le + thanh phan combo) phai khong vuot ton kho cua rap. */
    private static void checkStock(Connection conn, int centreId, Map<String, Integer> lines,
            Map<String, FnbMenuItemDTO> menu) throws SQLException {
        if (lines.isEmpty()) return;
        Map<Integer, Integer> need = new HashMap<>();
        for (Map.Entry<String, Integer> e : lines.entrySet()) {
            FnbMenuItemDTO item = menu.get(e.getKey());
            if (!item.isCombo()) {
                need.merge(item.getId(), e.getValue(), Integer::sum);
                continue;
            }
            try (PreparedStatement ps = conn.prepareStatement("SELECT FnbItemID, Quantity FROM ComboItems WHERE ComboID = ?")) {
                ps.setInt(1, item.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) need.merge(rs.getInt(1), rs.getInt(2) * e.getValue(), Integer::sum);
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT i.ItemName, ISNULL(inv.StockQuantity, 0) FROM FnbItems i "
              + "LEFT JOIN CentreFnbInventory inv ON inv.FnbItemID = i.FnbItemID AND inv.CentreID = ? AND inv.IsAvailable = 1 "
              + "WHERE i.FnbItemID = ?")) {
            for (Map.Entry<Integer, Integer> e : need.entrySet()) {
                ps.setInt(1, centreId);
                ps.setInt(2, e.getKey());
                try (ResultSet rs = ps.executeQuery()) {
                    boolean found = rs.next();
                    if (!found || rs.getInt(2) < e.getValue()) {
                        String name = found ? rs.getString(1) : "một món";
                        throw new IllegalArgumentException("Không đủ " + name + " trong kho của rạp, vui lòng giảm số lượng.");
                    }
                }
            }
        }
    }

    private static void deletePendingOrder(Connection conn, int bookingId) throws SQLException {
        String pending = "SELECT FnbOrderID FROM FnbOrders WHERE BookingID = ? AND OrderStatus = 'PENDING'";
        try (PreparedStatement items = conn.prepareStatement("DELETE FROM FnbOrderItems WHERE FnbOrderID IN (" + pending + ")");
             PreparedStatement orders = conn.prepareStatement("DELETE FROM FnbOrders WHERE BookingID = ? AND OrderStatus = 'PENDING'")) {
            items.setInt(1, bookingId);
            items.executeUpdate();
            orders.setInt(1, bookingId);
            orders.executeUpdate();
        }
    }

    private static void insertOrder(Connection conn, int bookingId, int centreId, Integer userId, String orderType,
            Map<String, Integer> lines, Map<String, FnbMenuItemDTO> menu) throws SQLException {
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, Integer> e : lines.entrySet()) {
            total = total.add(menu.get(e.getKey()).getPrice().multiply(BigDecimal.valueOf(e.getValue())));
        }
        int orderId;
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO FnbOrders (BookingID, CentreID, UserID, OrderCode, OrderType, OrderStatus, TotalAmount) "
              + "VALUES (?, ?, ?, ?, ?, 'PENDING', ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, bookingId);
            ps.setInt(2, centreId);
            if (userId == null) ps.setNull(3, Types.INTEGER);
            else ps.setInt(3, userId);
            ps.setString(4, BookingDAO.randomCode("FB", 12));
            ps.setString(5, orderType);
            ps.setBigDecimal(6, total);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                orderId = keys.getInt(1);
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO FnbOrderItems (FnbOrderID, FnbItemID, ComboID, Quantity, UnitPrice) VALUES (?, ?, ?, ?, ?)")) {
            for (Map.Entry<String, Integer> e : lines.entrySet()) {
                FnbMenuItemDTO item = menu.get(e.getKey());
                ps.setInt(1, orderId);
                if (item.isCombo()) {
                    ps.setNull(2, Types.INTEGER);
                    ps.setInt(3, item.getId());
                } else {
                    ps.setInt(2, item.getId());
                    ps.setNull(3, Types.INTEGER);
                }
                ps.setInt(4, e.getValue());
                ps.setBigDecimal(5, item.getPrice());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Don da thanh toan: don bap nuoc -> PREPARING va tru ton kho (khong xuong duoi 0).
     * Gom ca don CANCELLED: khach tra tien muon sau khi het gio giu ghe, tien da tinh ca bap nuoc.
     * (Gio hang cu bi xoa khi khach doi mon, nen CANCELLED chi do huy / het han don.)
     */
    static void confirmForBooking(Connection conn, int bookingId) throws SQLException {
        int moved;
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE FnbOrders SET OrderStatus = 'PREPARING', UpdatedAt = SYSDATETIME() "
              + "WHERE BookingID = ? AND OrderStatus IN ('PENDING', 'CANCELLED')")) {
            ps.setInt(1, bookingId);
            moved = ps.executeUpdate();
        }
        if (moved == 0) return;
        String demand = "SELECT o.CentreID, x.FnbItemID, SUM(x.Qty) AS Qty FROM FnbOrders o JOIN ("
                      + "  SELECT FnbOrderID, FnbItemID, Quantity AS Qty FROM FnbOrderItems WHERE FnbItemID IS NOT NULL "
                      + "  UNION ALL "
                      + "  SELECT oi.FnbOrderID, ci.FnbItemID, oi.Quantity * ci.Quantity FROM FnbOrderItems oi "
                      + "  JOIN ComboItems ci ON ci.ComboID = oi.ComboID"
                      + ") x ON x.FnbOrderID = o.FnbOrderID WHERE o.BookingID = ? AND o.OrderStatus = 'PREPARING' "
                      + "GROUP BY o.CentreID, x.FnbItemID";
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE inv SET StockQuantity = CASE WHEN inv.StockQuantity - d.Qty <= 0 THEN 0 ELSE inv.StockQuantity - d.Qty END, "
              + "  IsAvailable = CASE WHEN inv.StockQuantity - d.Qty <= 0 THEN 0 ELSE inv.IsAvailable END, "
              + "  UpdatedAt = SYSDATETIME() "
              + "FROM CentreFnbInventory inv JOIN (" + demand + ") d ON d.CentreID = inv.CentreID AND d.FnbItemID = inv.FnbItemID")) {
            ps.setInt(1, bookingId);
            ps.executeUpdate();
        }
    }

    /** Huy don bap nuoc chua thanh toan cua cac booking (cau lenh con tra ve BookingID). */
    static void cancelPendingForBookings(Connection conn, String bookingIdsSql, int... params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE FnbOrders SET OrderStatus = 'CANCELLED', UpdatedAt = SYSDATETIME() "
              + "WHERE OrderStatus = 'PENDING' AND BookingID IN (" + bookingIdsSql + ")")) {
            for (int i = 0; i < params.length; i++) ps.setInt(i + 1, params[i]);
            ps.executeUpdate();
        }
    }
}
