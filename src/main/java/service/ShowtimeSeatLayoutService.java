package service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import model.dto.pricing.SeatConfigDTO;
import model.dto.showtime.ShowtimeSeatMatrixDTO;
import model.entity.SeatAvailability;

/** Combines the hall's physical grid with booking states for one showtime. */
public class ShowtimeSeatLayoutService {
    public Map<String, List<ShowtimeSeatMatrixDTO>> buildRows(
            List<SeatConfigDTO> layout, List<SeatAvailability> availability) {
        Map<Integer, String> states = new HashMap<>();
        for (SeatAvailability seat : availability) states.put(seat.getSeatId(), seat.getAvailabilityStatus());
        Map<String, Map<Integer, SeatConfigDTO>> physicalRows = new TreeMap<>();
        int columns = 0;
        for (SeatConfigDTO seat : layout) {
            if (seat.getSeatRow() == null || seat.getSeatNumber() < 1) continue;
            physicalRows.computeIfAbsent(seat.getSeatRow(), key -> new HashMap<>()).put(seat.getSeatNumber(), seat);
            columns = Math.max(columns, seat.getSeatNumber());
        }
        Map<String, List<ShowtimeSeatMatrixDTO>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Map<Integer, SeatConfigDTO>> row : physicalRows.entrySet()) {
            List<ShowtimeSeatMatrixDTO> cells = new ArrayList<>();
            int label = 0;
            for (int column = 1; column <= columns; column++) {
                SeatConfigDTO config = row.getValue().get(column);
                ShowtimeSeatMatrixDTO cell = new ShowtimeSeatMatrixDTO();
                cell.setSeatRow(row.getKey());
                cell.setSeatNumber(column);
                cell.setSeatType(config == null ? "STANDARD" : config.getSeatType());
                if (config == null || config.isUnavailable()) {
                    cell.setAvailabilityStatus("UNAVAILABLE");
                    cell.setDisplayLabel("—");
                } else {
                    cell.setSeatId(config.getSeatId());
                    cell.setHallId(config.getHallId());
                    cell.setDisplayLabel(row.getKey() + (++label));
                    cell.setAvailabilityStatus(config.isMaintenance() ? "MAINTENANCE"
                            : config.isAvailable() ? states.getOrDefault(config.getSeatId(), "AVAILABLE") : "MAINTENANCE");
                }
                cells.add(cell);
            }
            result.put(row.getKey(), cells);
        }
        return result;
    }
    public void attachPrices(Map<String,List<ShowtimeSeatMatrixDTO>> rows,
            Map<Integer,ShowtimeSeatMatrixDTO> prices) {
        for (List<ShowtimeSeatMatrixDTO> row : rows.values()) for (ShowtimeSeatMatrixDTO seat : row) {
            ShowtimeSeatMatrixDTO price=prices.get(seat.getSeatId());
            if (price==null) continue;
            seat.setBasePrice(price.getBasePrice()); seat.setTimeSurcharge(price.getTimeSurcharge());
            seat.setHallSurcharge(price.getHallSurcharge()); seat.setSeatSurcharge(price.getSeatSurcharge());
            seat.setPrice(price.getPrice()); seat.setStoredPrice(price.getStoredPrice());
        }
    }
}
