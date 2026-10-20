package model.dto;

/**
 * Tương thích ngược với mã nguồn cũ.
 * Khuyến nghị sử dụng model.dto.fnb.StockRowDTO.
 */
public class StockRowDTO extends model.dto.fnb.StockRowDTO {
    public StockRowDTO() {
        super();
    }

    public StockRowDTO(int fnbItemId, String itemName, String categoryName, boolean available) {
        super(fnbItemId, itemName, categoryName, available);
    }
}
