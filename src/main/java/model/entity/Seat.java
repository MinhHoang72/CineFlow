package model.entity;

import java.math.BigDecimal;

public class Seat {

    private final int id, number;
    private final String row, type;
    private final boolean available, taken;
    private final BigDecimal price;

    /**
     * @param available false neu ghe dang bao tri / khong su dung
     * @param taken true neu ghe da co nguoi giu hoac da ban cho suat chieu nay
     */
    public Seat(int id, String row, int number, String type, boolean available, boolean taken, BigDecimal price) {
        this.id = id;
        this.row = row;
        this.number = number;
        this.type = type;
        this.available = available;
        this.taken = taken;
        this.price = price;
    }

    public int getId() { return id; }
    public String getRow() { return row; }
    public int getNumber() { return number; }
    public String getLabel() { return row + number; }
    public String getType() { return type; }
    public boolean isAvailable() { return available; }
    public boolean isTaken() { return taken; }
    public boolean isSelectable() { return available && !taken; }
    public BigDecimal getPrice() { return price; }
}
