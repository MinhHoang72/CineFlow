package model;

/**
 * DTO hiển thị một dòng trong trang Kho (CentreFnbInventory).
 *
 * Cơ chế nghiệp vụ:
 *  - Manager nhập StockQuantity → khi = 0 hệ thống tự đặt IsAvailable = false.
 *  - Manager cũng có thể toggle IsAvailable tay (ví dụ: hàng lỗi, cần đóng dù còn số lượng).
 *  - TICKET_SALES_STAFF chỉ xem, không chỉnh sửa.
 *  - ADMIN không truy cập trang kho.
 */
public class StockRowDTO {
    private int fnbItemId;
    private String itemName;
    private String categoryName;

    /** Số lượng tồn kho (Manager nhập thủ công). */
    private int stockQuantity;

    /**
     * Tình trạng còn hàng.
     * Tự động = false khi stockQuantity = 0.
     * Manager có thể tắt tay dù stockQuantity > 0.
     */
    private boolean available;

    public StockRowDTO() {
    }

    public StockRowDTO(int fnbItemId, String itemName, String categoryName,
                       int stockQuantity, boolean available) {
        this.fnbItemId = fnbItemId;
        this.itemName = itemName;
        this.categoryName = categoryName;
        this.stockQuantity = stockQuantity;
        this.available = available;
    }

    public int getFnbItemId() { return fnbItemId; }
    public void setFnbItemId(int fnbItemId) { this.fnbItemId = fnbItemId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    @Override
    public String toString() {
        return "StockRowDTO{fnbItemId=" + fnbItemId + ", itemName=" + itemName
                + ", stockQuantity=" + stockQuantity + ", available=" + available + '}';
    }
}
