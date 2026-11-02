package service;

import dao.CentreDAO;
import dao.HallDAO;
import dao.SeatDAO;
import model.dto.common.PageDTO;
import model.dto.pricing.SeatConfigDTO;
import model.entity.Centre;
import model.entity.Hall;

import java.util.List;

/**
 * Service: Xử lý logic nghiệp vụ liên quan đến Cụm rạp (Centre), Phòng chiếu (Hall), và Sơ đồ ghế (Seat Layout).
 * Toàn bộ các hàm nghiệp vụ trả về chuỗi kết quả trực quan (success, missing_fields, duplicate_name, v.v.).
 */
public class CinemaService {

    private final CentreDAO centreDAO;
    private final HallDAO hallDAO;
    private final SeatDAO seatDAO;

    public CinemaService() {
        this(new CentreDAO(), new HallDAO(), new SeatDAO());
    }

    public CinemaService(CentreDAO centreDAO, HallDAO hallDAO, SeatDAO seatDAO) {
        this.centreDAO = centreDAO;
        this.hallDAO = hallDAO;
        this.seatDAO = seatDAO;
    }

    public record CentreKpi(int totalCentres, int activeCentres, int inactiveCentres, int totalHalls) {}

    public record HallKpi(int totalHalls, int activeHalls, int inactiveHalls, int totalCapacity) {}

    // ==============================================================
    // 1. NGHIỆP VỤ CỤM RẠP (CENTRE)
    // ==============================================================

    public PageDTO<Centre> searchCentres(String search, String status, int page, int size) {
        return centreDAO.search(search, status, page, size);
    }

    public CentreKpi getCentresKpi() {
        int totalCentres = centreDAO.countCentresByStatus("ALL");
        int activeCentres = centreDAO.countCentresByStatus("ACTIVE");
        int inactiveCentres = centreDAO.countCentresByStatus("INACTIVE");
        int totalHalls = centreDAO.countTotalHalls();
        return new CentreKpi(totalCentres, activeCentres, inactiveCentres, totalHalls);
    }

    public Centre getCentreById(int centreId) {
        if (centreId <= 0) return null;
        return centreDAO.getCentreById(centreId);
    }

    public List<Centre> getAllCentres(String search, String status) {
        return centreDAO.getAllCentres(search, status);
    }

    public List<Hall> getHallsByCentreId(int centreId) {
        if (centreId <= 0) return List.of();
        return centreDAO.getHallsByCentreId(centreId);
    }

    public String createCentre(String name, String address, String phone, String city) {
        if (name == null || name.trim().isEmpty()
                || address == null || address.trim().isEmpty()
                || phone == null || phone.trim().isEmpty()) {
            return "missing_fields";
        }

        name = name.trim();
        address = address.trim();
        phone = phone.trim();
        city = city != null ? city.trim() : "";

        if (centreDAO.isCentreNameExists(name, 0)) {
            return "duplicate_name";
        }

        Centre centre = new Centre();
        centre.setCentreName(name);
        centre.setAddress(address);
        centre.setPhone(phone);
        centre.setCity(city);
        centre.setStatus("ACTIVE");

        boolean ok = centreDAO.addCentre(centre);
        return ok ? "success" : "failed";
    }

    public String updateCentre(int centreId, String name, String address, String phone, String city) {
        if (centreId <= 0) {
            return "invalid_id";
        }
        if (name == null || name.trim().isEmpty()
                || address == null || address.trim().isEmpty()
                || phone == null || phone.trim().isEmpty()) {
            return "missing_fields";
        }

        name = name.trim();
        address = address.trim();
        phone = phone.trim();
        city = city != null ? city.trim() : "";

        if (centreDAO.isCentreNameExists(name, centreId)) {
            return "duplicate_name";
        }

        Centre centre = new Centre();
        centre.setCentreId(centreId);
        centre.setCentreName(name);
        centre.setAddress(address);
        centre.setPhone(phone);
        centre.setCity(city);

        boolean ok = centreDAO.updateCentre(centre);
        return ok ? "success" : "failed";
    }

    public boolean toggleCentreStatus(int centreId) {
        if (centreId <= 0) return false;
        return centreDAO.toggleStatus(centreId);
    }

    // ==============================================================
    // 2. NGHIỆP VỤ PHÒNG CHIẾU (HALL)
    // ==============================================================

    public PageDTO<Hall> searchHalls(int centreId, String search, String status, int page, int size) {
        if (centreId <= 0) {
            return new PageDTO<>(List.of(), page, size, 0);
        }
        return hallDAO.search(centreId, search, status, page, size);
    }

    public HallKpi getHallKpi(int centreId) {
        if (centreId <= 0) {
            return new HallKpi(0, 0, 0, 0);
        }
        int totalHalls = hallDAO.countHallsByStatus(centreId, "ALL");
        int activeHalls = hallDAO.countHallsByStatus(centreId, "ACTIVE");
        int inactiveHalls = hallDAO.countHallsByStatus(centreId, "INACTIVE");
        int totalCapacity = hallDAO.sumCapacityByCentre(centreId);
        return new HallKpi(totalHalls, activeHalls, inactiveHalls, totalCapacity);
    }

    public HallKpi getHallsKpi(int centreId) {
        return getHallKpi(centreId);
    }

    public Hall getHallById(int hallId) {
        if (hallId <= 0) return null;
        return hallDAO.getHallById(hallId);
    }

    public String createHall(int centreId, String hallName, String hallType, String capacityParam, String status) {
        if (centreId <= 0) {
            return "invalid_id";
        }
        if (hallName == null || hallName.trim().isEmpty()) {
            return "missing_fields";
        }

        hallName = hallName.trim();
        if (status == null || status.trim().isEmpty()) {
            status = "AVAILABLE";
        }

        int capacity = 60;
        if (capacityParam != null && !capacityParam.trim().isEmpty()) {
            try {
                capacity = Integer.parseInt(capacityParam.trim());
                if (capacity <= 0) capacity = 60;
            } catch (NumberFormatException ignored) {
            }
        }

        if (hallDAO.isHallNameExists(centreId, hallName, 0)) {
            return "duplicate_name";
        }

        Hall hall = new Hall();
        hall.setCentreId(centreId);
        hall.setHallName(hallName);
        hall.setHallType(hallType != null ? hallType.trim() : "2D");
        hall.setCapacity(capacity);
        hall.setStatus(status);

        boolean ok = hallDAO.addHall(hall);
        return ok ? "success" : "failed";
    }

    public String updateHall(int centreId, int hallId, String hallName, String hallType, String capacityParam, String newStatus) {
        if (centreId <= 0 || hallId <= 0) {
            return "invalid_id";
        }

        Hall existingHall = hallDAO.getHallById(hallId);
        if (existingHall == null || existingHall.getCentreId() != centreId) {
            return "invalid_hall";
        }

        if (hallName == null || hallName.trim().isEmpty()) {
            return "missing_fields";
        }

        hallName = hallName.trim();
        int capacity = existingHall.getCapacity();
        if (capacityParam != null && !capacityParam.trim().isEmpty()) {
            try {
                capacity = Integer.parseInt(capacityParam.trim());
                if (capacity <= 0) capacity = existingHall.getCapacity();
            } catch (NumberFormatException ignored) {
            }
        }

        // Chặn chuyển sang INACTIVE nếu đang có suất chiếu tương lai có vé đặt
        boolean isChangingToInactive = existingHall.isActive() && ("INACTIVE".equalsIgnoreCase(newStatus) || "MAINTENANCE".equalsIgnoreCase(newStatus));
        if (isChangingToInactive && hallDAO.hasFutureBookedShowtimes(hallId)) {
            return "has_future_bookings";
        }

        if (hallDAO.isHallNameExists(centreId, hallName, hallId)) {
            return "duplicate_name";
        }

        Hall updatedHall = new Hall();
        updatedHall.setHallId(hallId);
        updatedHall.setCentreId(centreId);
        updatedHall.setHallName(hallName);
        updatedHall.setHallType(hallType != null ? hallType.trim() : "2D");
        updatedHall.setCapacity(capacity);
        updatedHall.setStatus(newStatus != null ? newStatus.trim() : "AVAILABLE");

        boolean ok = hallDAO.updateHall(updatedHall);
        return ok ? "success" : "failed";
    }

    public String toggleHallStatus(int centreId, int hallId) {
        if (centreId <= 0 || hallId <= 0) {
            return "invalid_id";
        }
        Hall existingHall = hallDAO.getHallById(hallId);
        if (existingHall == null || existingHall.getCentreId() != centreId) {
            return "invalid_hall";
        }

        String result = hallDAO.toggleStatus(hallId);
        if ("SUCCESS".equalsIgnoreCase(result)) {
            return "success";
        } else if ("HAS_FUTURE_BOOKINGS".equalsIgnoreCase(result)) {
            return "has_future_bookings";
        } else {
            return "failed";
        }
    }

    // ==============================================================
    // 3. NGHIỆP VỤ SƠ ĐỒ GHẾ (SEAT LAYOUT & MAINTENANCE)
    // ==============================================================

    public List<SeatConfigDTO> getSeatConfigsByHall(int hallId) {
        if (hallId <= 0) return List.of();
        return seatDAO.getSeatConfigsByHall(hallId);
    }

    public int countSeatsByHall(int hallId, String search, String seatType, String seatStatus) {
        return seatDAO.countSeatsByHall(hallId, search, seatType, seatStatus);
    }

    public List<SeatConfigDTO> getSeatsByHallPaged(int hallId, String search, String seatType, String seatStatus, int page, int pageSize) {
        return seatDAO.getSeatsByHallPaged(hallId, search, seatType, seatStatus, page, pageSize);
    }

    public boolean hasFutureBookedShowtimes(int hallId) {
        return seatDAO.hasFutureBookedShowtimes(hallId);
    }

    public String toggleSeatMaintenance(int hallId, int seatId) {
        if (hallId <= 0 || seatId <= 0) {
            return "invalid_id";
        }

        String result = seatDAO.toggleSeatMaintenance(seatId, hallId);
        if ("SUCCESS".equalsIgnoreCase(result)) {
            return "success";
        } else if (result != null && result.startsWith("HAS_FUTURE_BOOKINGS:")) {
            return "booked_seat";
        } else {
            return "failed";
        }
    }

    public String saveSeatLayout(int hallId, List<SeatConfigDTO> seatList, boolean isRegenerating) {
        if (hallId <= 0) {
            return "invalid_id";
        }
        if (isRegenerating && seatDAO.hasFutureBookedShowtimes(hallId)) {
            return "cannot_regenerate";
        }
        if (seatList == null || seatList.isEmpty()) {
            return "empty_data";
        }

        // Kiểm tra ghế bảo trì có khách đặt trước không
        for (SeatConfigDTO s : seatList) {
            if ("MAINTENANCE".equalsIgnoreCase(s.getStatus())) {
                String bookedShowtime = seatDAO.checkFutureBookingForSeat(hallId, s.getSeatRow(), s.getSeatNumber());
                if (bookedShowtime != null) {
                    return "booked_seat";
                }
            }
        }

        boolean ok = seatDAO.saveSeatLayout(hallId, seatList, isRegenerating);
        return ok ? "success" : "failed";
    }
}
