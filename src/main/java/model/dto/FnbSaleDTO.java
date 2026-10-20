package model.dto;

import java.math.BigDecimal;

/**
 * Tương thích ngược với mã nguồn cũ.
 * Khuyến nghị sử dụng model.dto.fnb.FnbSaleDTO.
 */
public class FnbSaleDTO extends model.dto.fnb.FnbSaleDTO {
    public FnbSaleDTO(String name, int quantity, BigDecimal revenue) {
        super(name, quantity, revenue);
    }
}
