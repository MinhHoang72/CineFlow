import java.util.List;
import java.util.Map;
import model.dto.pricing.SeatConfigDTO;
import model.dto.showtime.ShowtimeSeatMatrixDTO;
import model.entity.SeatAvailability;
import service.ShowtimeSeatLayoutService;

public class ShowtimeSeatLayoutVerificationRunner {
    public static void main(String[] args) {
        List<SeatConfigDTO> layout = List.of(
                new SeatConfigDTO(4, 1, "B", 2, "STANDARD", "AVAILABLE"),
                new SeatConfigDTO(1, 1, "A", 1, "STANDARD", "AVAILABLE"),
                new SeatConfigDTO(2, 1, "A", 2, "STANDARD", "UNAVAILABLE"),
                new SeatConfigDTO(3, 1, "A", 3, "VIP", "AVAILABLE"),
                new SeatConfigDTO(5, 1, "A", 4, "SWEETBOX", "MAINTENANCE"));
        SeatAvailability held = state(1, "HELD");
        SeatAvailability booked = state(3, "BOOKED");
        ShowtimeSeatLayoutService service = new ShowtimeSeatLayoutService();
        Map<String, List<ShowtimeSeatMatrixDTO>> rows = service.buildRows(layout, List.of(held, booked));
        check(List.copyOf(rows.keySet()).equals(List.of("A", "B")), "Row order");
        check(rows.get("A").size() == 4 && rows.get("B").size() == 4, "Aligned columns");
        check(rows.get("A").get(1).getAvailabilityStatus().equals("UNAVAILABLE"), "Explicit aisle");
        check(rows.get("B").get(0).getAvailabilityStatus().equals("UNAVAILABLE"), "Missing position stays empty");
        check(rows.get("A").get(2).getDisplayLabel().equals("A2"), "Sequential numbering skips aisle");
        check(rows.get("A").get(2).getSeatNumber() == 3 && rows.get("A").get(2).getSeatId() == 3, "Physical position and identity preserved");
        check(rows.get("A").get(2).getSeatType().equals("VIP"), "Seat type retained");
        check(rows.get("A").get(0).getAvailabilityStatus().equals("HELD"), "Held state");
        check(rows.get("A").get(2).getAvailabilityStatus().equals("BOOKED"), "Booked state");
        check(rows.get("A").get(3).getAvailabilityStatus().equals("MAINTENANCE"), "Maintenance retained");
        Map<String, List<ShowtimeSeatMatrixDTO>> anotherShowtime = service.buildRows(layout, List.of());
        check(anotherShowtime.get("A").get(0).getAvailabilityStatus().equals("AVAILABLE"), "States isolated between showtimes");
        check(service.buildRows(List.of(), List.of()).isEmpty(), "Empty layout");
        System.out.println("PASS: 12 checks for layout, aisles, numbering, types and showtime states");
    }

    private static SeatAvailability state(int id, String status) {
        SeatAvailability seat = new SeatAvailability();
        seat.setSeatId(id);
        seat.setAvailabilityStatus(status);
        return seat;
    }

    private static void check(boolean value, String description) {
        if (!value) throw new AssertionError(description);
    }
}
