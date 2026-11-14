package model.dto.report;

public class OperationsSummaryDTO {

    private final int showtimes;
    private final int capacity;
    private final int soldSeats;
    private final int outOfStockItems;

    public OperationsSummaryDTO(int showtimes, int capacity, int soldSeats, int outOfStockItems) {
        this.showtimes = showtimes;
        this.capacity = capacity;
        this.soldSeats = soldSeats;
        this.outOfStockItems = outOfStockItems;
    }

    public int getShowtimes() { return showtimes; }
    public int getCapacity() { return capacity; }
    public int getSoldSeats() { return soldSeats; }
    public int getOutOfStockItems() { return outOfStockItems; }
    public double getOccupancy() {
        return capacity == 0 ? 0 : 100.0 * soldSeats / capacity;
    }
}
