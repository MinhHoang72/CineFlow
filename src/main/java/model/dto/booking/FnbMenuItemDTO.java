package model.dto.booking;

import java.math.BigDecimal;

/** Mot mon hoac combo dang ban tai rap, dung o buoc chon bap nuoc khi dat ve. */
public class FnbMenuItemDTO {

    /** So luong toi da khach duoc chon cho moi mon trong mot don. */
    public static final int MAX_PER_LINE = 10;

    private final boolean combo;
    private final int id;
    private final String name, description;
    private final BigDecimal price;
    private final int available;

    public FnbMenuItemDTO(boolean combo, int id, String name, String description, BigDecimal price, int available) {
        this.combo = combo;
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.available = available;
    }

    /** Khoa dung trong form: I12 = mon 12, C3 = combo 3. */
    public String getKey() { return (combo ? "C" : "I") + id; }
    public boolean isCombo() { return combo; }
    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    /** So phan con ban duoc theo ton kho. */
    public int getAvailable() { return available; }
    /** Gioi han o o nhap so luong. */
    public int getMaxQuantity() { return Math.min(available, MAX_PER_LINE); }
}
