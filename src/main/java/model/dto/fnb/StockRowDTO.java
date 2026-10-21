package model.dto.fnb;

public class StockRowDTO {
    private int fnbItemId;
    private String itemName;
    private String categoryName;
    private int stockQuantity;
    private boolean available;

    public StockRowDTO() {
    }

    public StockRowDTO(int fnbItemId, String itemName, String categoryName, int stockQuantity, boolean available) {
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
}
