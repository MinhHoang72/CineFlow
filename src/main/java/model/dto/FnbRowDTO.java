package model.dto;

import java.math.BigDecimal;

/**
 * Tương thích ngược với mã nguồn cũ.
 * Khuyến nghị sử dụng model.dto.fnb.FnbRowDTO.
 */
public class FnbRowDTO extends model.dto.fnb.FnbRowDTO {
    public FnbRowDTO() {
        super();
    }

    public FnbRowDTO(String kind, int id, String name, String categoryName, BigDecimal price, String status, String imageUrl, int centreId) {
        super(kind, id, name, categoryName, price, status, imageUrl, centreId);
    }

    public FnbRowDTO(String kind, int id, String name, String categoryName, BigDecimal price, String status, String imageUrl, int centreId, String centreName) {
        super(kind, id, name, categoryName, price, status, imageUrl, centreId, centreName);
    }
}
