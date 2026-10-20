package model;

public class OccupancyRowDTO {

    private final String label;
    private final int capacity;
    private final int soldSeats;

    public OccupancyRowDTO(String label, int capacity, int soldSeats) {
        this.label = label;
        this.capacity = capacity;
        this.soldSeats = soldSeats;
    }

    public String getLabel() {
        return label;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getSoldSeats() {
        return soldSeats;
    }

    public double getOccupancy() {
        return capacity == 0 ? 0 : 100.0 * soldSeats / capacity;
    }
}
