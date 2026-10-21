package model.dto.booking;

import java.math.BigDecimal;

/** Mot dong bap nuoc trong don dat ve. */
public class FnbLineDTO {

    private final String key, name;
    private final int quantity;
    private final BigDecimal unitPrice;

    public FnbLineDTO(String key, String name, int quantity, BigDecimal unitPrice) {
        this.key = key;
        this.name = name;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    /** I12 / C3, khop voi FnbMenuItemDTO.getKey(). */
    public String getKey() { return key; }
    public boolean isCombo() { return key.startsWith("C"); }
    public String getName() { return name; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getLineTotal() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }
}
