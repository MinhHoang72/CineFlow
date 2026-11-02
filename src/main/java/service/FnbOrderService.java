package service;

import dao.FnbOrderDAO;
import dao.FnbOrderDAO.AvailableStock;
import dao.FnbOrderDAO.ComboPart;
import dao.FnbOrderDAO.Demand;
import dao.FnbOrderDAO.Inventory;
import dao.FnbOrderDAO.OrderLine;
import model.dto.booking.FnbMenuItemDTO;
import utils.CodeUtil;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Quy tac don bap nuoc dung chung cho bap nuoc kem ve (BookingExtrasService) va ban le tai quay (FnbSaleService):
 * doc gio hang tu form, kiem tra mon / so luong / ton kho, tinh tien, tao don, tru kho khi da thanh toan.
 */
public class FnbOrderService {

    private final FnbOrderDAO fnbOrderDAO;

    public FnbOrderService() {
        this(new FnbOrderDAO());
    }

    public FnbOrderService(FnbOrderDAO fnbOrderDAO) {
        this.fnbOrderDAO = fnbOrderDAO;
    }

    /**
     * Tham so form qty_I12 / qty_C3 -> gio hang (key menu -> so luong), o trong = 0.
     * @throws IllegalArgumentException so luong khong phai so
     */
    public static Map<String, Integer> readCart(Map<String, String[]> params) {
        Map<String, Integer> cart = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> e : params.entrySet()) {
            if (!e.getKey().matches("qty_[IC]\\d{1,9}")) continue;
            String raw = e.getValue().length == 0 ? "" : e.getValue()[0].trim();
            try {
                cart.put(e.getKey().substring(4), raw.isEmpty() ? 0 : Integer.parseInt(raw));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Số lượng bắp nước không hợp lệ.");
            }
        }
        return cart;
    }

    /** Menu dang ban tai rap theo key (I12 / C3). */
    public Map<String, FnbMenuItemDTO> loadMenu(Connection conn, int centreId) throws SQLException {
        Map<String, FnbMenuItemDTO> menu = new HashMap<>();
        for (FnbMenuItemDTO m : fnbOrderDAO.getMenu(conn, centreId)) menu.put(m.getKey(), m);
        return menu;
    }

    /**
     * Gio hang hop le: bo mon so luong 0; mon phai con trong menu, so luong 1..MAX_PER_LINE.
     * @throws IllegalArgumentException mon het hang / ngung ban hoac so luong sai
     */
    public Map<String, Integer> validLines(Map<String, Integer> cart, Map<String, FnbMenuItemDTO> menu) {
        Map<String, Integer> lines = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : cart.entrySet()) {
            int qty = e.getValue();
            if (qty == 0) continue;
            if (menu.get(e.getKey()) == null) {
                throw new IllegalArgumentException("Có món vừa hết hàng hoặc ngừng bán, vui lòng chọn lại.");
            }
            if (qty < 0 || qty > FnbMenuItemDTO.MAX_PER_LINE) {
                throw new IllegalArgumentException("Số lượng mỗi món từ 0 đến " + FnbMenuItemDTO.MAX_PER_LINE + ".");
            }
            lines.put(e.getKey(), qty);
        }
        return lines;
    }

    /**
     * Tong nhu cau theo mon le (mon le + thanh phan combo) khong duoc vuot ton kho dang ban cua rap.
     * @throws IllegalArgumentException khong du hang
     */
    public void checkStock(Connection conn, int centreId, Map<String, Integer> lines, Map<String, FnbMenuItemDTO> menu)
            throws SQLException {
        Map<Integer, Integer> need = new HashMap<>();
        for (Map.Entry<String, Integer> e : lines.entrySet()) {
            FnbMenuItemDTO item = menu.get(e.getKey());
            if (!item.isCombo()) {
                need.merge(item.getId(), e.getValue(), Integer::sum);
                continue;
            }
            for (ComboPart part : fnbOrderDAO.getComboParts(conn, item.getId())) {
                need.merge(part.itemId(), part.quantity() * e.getValue(), Integer::sum);
            }
        }
        for (Map.Entry<Integer, Integer> e : need.entrySet()) {
            AvailableStock stock = fnbOrderDAO.getAvailableStock(conn, centreId, e.getKey());
            if (stock == null || stock.quantity() < e.getValue()) {
                String name = stock == null ? "một món" : stock.itemName();
                throw new IllegalArgumentException("Không đủ " + name + " trong kho của rạp, vui lòng giảm số lượng.");
            }
        }
    }

    /** Tong tien gio hang theo gia menu hien tai. */
    public BigDecimal total(Map<String, Integer> lines, Map<String, FnbMenuItemDTO> menu) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, Integer> e : lines.entrySet()) {
            total = total.add(menu.get(e.getKey()).getPrice().multiply(BigDecimal.valueOf(e.getValue())));
        }
        return total;
    }

    /**
     * Ghi don bap nuoc PENDING kem cac dong mon (gia lay theo menu).
     * @param bookingId null = don ban le khong kem ve
     * @return FnbOrderID
     */
    public int createOrder(Connection conn, Integer bookingId, int centreId, Integer userId, String orderType,
            Map<String, Integer> lines, Map<String, FnbMenuItemDTO> menu) throws SQLException {
        int orderId = fnbOrderDAO.insertOrder(conn, bookingId, centreId, userId, CodeUtil.randomCode("FB", 12),
                orderType, total(lines, menu));
        List<OrderLine> orderLines = new ArrayList<>();
        for (Map.Entry<String, Integer> e : lines.entrySet()) {
            FnbMenuItemDTO item = menu.get(e.getKey());
            orderLines.add(new OrderLine(item.isCombo() ? null : item.getId(), item.isCombo() ? item.getId() : null,
                    e.getValue(), item.getPrice()));
        }
        fnbOrderDAO.insertOrderLines(conn, orderId, orderLines);
        return orderId;
    }

    /**
     * Don da thanh toan: tru ton kho theo nhu cau (mon le + thanh phan combo).
     * Khong xuong duoi 0; het hang thi tat ban mon do (IsAvailable = 0).
     */
    public void deductStock(Connection conn, List<Demand> demand) throws SQLException {
        for (Demand d : demand) {
            Inventory inv = fnbOrderDAO.lockInventory(conn, d.centreId(), d.itemId());
            if (inv == null) continue;
            int left = inv.quantity() - d.quantity();
            boolean soldOut = left <= 0;
            fnbOrderDAO.updateInventory(conn, d.centreId(), d.itemId(), soldOut ? 0 : left, !soldOut && inv.available());
        }
    }
}
