package model.dto;
import model.entity.*;

import java.math.BigDecimal;

public class FnbSaleDTO {

    private final String name;
    private final int quantity;
    private final BigDecimal revenue;

    public FnbSaleDTO(String name, int quantity, BigDecimal revenue) {
        this.name = name;
        this.quantity = quantity;
        this.revenue = revenue;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }
}
