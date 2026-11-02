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
 * Service: Xử lý toàn bộ logic nghiệp vụ liên quan đến Cụm rạp (Centre), Phòng chiếu (Hall), và Sơ đồ ghế (Seat Layout).
 * Tuân thủ mô hình 3 lớp chuẩn: Controller -> Service -> DAO.
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

    public enum Status {
        SUCCESS,
        MISSING_FIELDS,
        INVALID_ID,
        INVALID_HALL,
        DUPLICATE_NAME,
        HAS_FUTURE_BOOKINGS,
        CANNOT_REGENERATE,
        BOOKED_SEAT,
        EMPTY_DATA,
        FAILED
    }

    public static class Result<T> {
        private final Status status;
        private final String message;
        private final T data;

        public Result(Status status, String message) {
            this(status, message, null);
        }

        public Result(Status status, String message, T data) {
            this.status = status;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() {
            return status == Status.SUCCESS;
        }

        public Status getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public T getData() {
            return data;
        }
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

    public Result<Centre> createCentre(String name, String address, String phone, String city) {
        if (name == null || name.trim().isEmpty()
                || address == null || address.trim().isEmpty()
                || phone == null || phone.trim().isEmpty()) {
            return new Result<>(Status.MISSING_FIELDS, "Vui lòng nhập đầy đủ tên rạp, địa chỉ và số điện thoại.");
        }

        name = name.trim();
        address = address.trim();
        phone = phone.trim();
        city = city != null ? city.trim() : "";

        if (centreDAO.isCentreNameExists(name, 0)) {
            return new Result<>(Status.DUPLICATE_NAME, "Tên cụm rạp đã tồn tại trong hệ thống.");
        }

        Centre centre = new Centre();
        centre.setCentreName(name);
        centre.setAddress(address);
        centre.setPhone(phone);
        centre.setCity(city);
        centre.setStatus("ACTIVE");

        boolean ok = centreDAO.addCentre(centre);
        if (ok) {
            return new Result<>(Status.SUCCESS, "Thêm cụm rạp thành công.", centre);
        } else {
            return new Result<>(Status.FAILED, "Không thể lưu cụm rạp vào cơ sở dữ liệu.");
        }
    }

    public Result<Centre> updateCentre(int centreId, String name, String address, String phone, String city) {
        if (centreId <= 0) {
            return new Result<>(Status.INVALID_ID, "Mã cụm rạp không hợp lệ.");
        }
        if (name == null || name.trim().isEmpty()
                || address == null || address.trim().isEmpty()
                || phone == null || phone.trim().isEmpty()) {
            return new Result<>(Status.MISSING_FIELDS, "Vui lòng nhập đầy đủ tên rạp, địa chỉ và số điện thoại.");
        }

        name = name.trim();
        address = address.trim();
        phone = phone.trim();
        city = city != null ? city.trim() : "";

        if (centreDAO.isCentreNameExists(name, centreId)) {
            return new Result<>(Status.DUPLICATE_NAME, "Tên cụm rạp đã bị trùng với rạp khác.");
        }

        Centre centre = new Centre();
        centre.setCentreId(centreId);
        centre.setCentreName(name);
        centre.setAddress(address);
        centre.setPhone(phone);
        centre.setCity(city);

        boolean ok = centreDAO.updateCentre(centre);
        if (ok) {
            return new Result<>(Status.SUCCESS, "Cập nhật cụm rạp thành công.", centre);
        } else {
            return new Result<>(Status.FAILED, "Không thể cập nhật thông tin cụm rạp.");
        }
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

    public HallKpi getHallsKpi(int centreId) {
        if (centreId <= 0) {
            return new HallKpi(0, 0, 0, 0);
        }
        int totalHalls = hallDAO.countHallsByStatus(centreId, "ALL");
        int activeHalls = hallDAO.countHallsByStatus(centreId, "ACTIVE");
        int inactiveHalls = hallDAO.countHallsByStatus(centreId, "INACTIVE");
        int totalCapacity = hallDAO.sumCapacityByCentre(centreId);
        return new HallKpi(totalHalls, activeHalls, inactiveHalls, totalCapacity);
    }

    public Hall getHallById(int hallId) {
        if (hallId <= 0) return null;
        return hallDAO.getHallById(hallId);
    }

    public Result<Hall> createHall(int centreId, String hallName, String hallType, String capacityParam, String status) {
        if (centreId <= 0) {
            return new Result<>(Status.INVALID_ID, "Cụm rạp không hợp lệ.");
        }
        if (hallName == null || hallName.trim().isEmpty()) {
            return new Result<>(Status.MISSING_FIELDS, "Tên phòng chiếu không được để trống.");
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
            return new Result<>(Status.DUPLICATE_NAME, "Tên phòng chiếu đã tồn tại trong cụm rạp.");
        }

        Hall hall = new Hall();
        hall.setCentreId(centreId);
        hall.setHallName(hallName);
        hall.setHallType(hallType != null ? hallType.trim() : "2D");
        hall.setCapacity(capacity);
        hall.setStatus(status);

        boolean ok = hallDAO.addHall(hall);
        if (ok) {
            return new Result<>(Status.SUCCESS, "Thêm phòng chiếu thành công.", hall);
        } else {
            return new Result<>(Status.FAILED, "Không thể lưu phòng chiếu.");
        }
    }

    public Result<Hall> updateHall(int centreId, int hallId, String hallName, String hallType, String capacityParam, String newStatus) {
        if (centreId <= 0 || hallId <= 0) {
            return new Result<>(Status.INVALID_ID, "Mã phòng chiếu hoặc rạp không hợp lệ.");
        }

        Hall existingHall = hallDAO.getHallById(hallId);
        if (existingHall == null || existingHall.getCentreId() != centreId) {
            return new Result<>(Status.INVALID_HALL, "Không tìm thấy phòng chiếu thuộc cụm rạp.");
        }

        if (hallName == null || hallName.trim().isEmpty()) {
            return new Result<>(Status.MISSING_FIELDS, "Tên phòng chiếu không được để trống.");
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
            return new Result<>(Status.HAS_FUTURE_BOOKINGS, "Phòng chiếu đang có suất chiếu tương lai đã bán vé, không thể khóa phòng.");
        }

        if (hallDAO.isHallNameExists(centreId, hallName, hallId)) {
            return new Result<>(Status.DUPLICATE_NAME, "Tên phòng chiếu đã bị trùng trong cụm rạp.");
        }

        Hall updatedHall = new Hall();
        updatedHall.setHallId(hallId);
        updatedHall.setCentreId(centreId);
        updatedHall.setHallName(hallName);
        updatedHall.setHallType(hallType != null ? hallType.trim() : "2D");
        updatedHall.setCapacity(capacity);
        updatedHall.setStatus(newStatus != null ? newStatus.trim() : "AVAILABLE");

        boolean ok = hallDAO.updateHall(updatedHall);
        if (ok) {
            return new Result<>(Status.SUCCESS, "Cập nhật phòng chiếu thành công.", updatedHall);
        } else {
            return new Result<>(Status.FAILED, "Không thể cập nhật phòng chiếu.");
        }
    }

    public Result<Void> toggleHallStatus(int centreId, int hallId) {
        if (centreId <= 0 || hallId <= 0) {
            return new Result<>(Status.INVALID_ID, "Mã phòng không hợp lệ.");
        }
        Hall existingHall = hallDAO.getHallById(hallId);
        if (existingHall == null || existingHall.getCentreId() != centreId) {
            return new Result<>(Status.INVALID_HALL, "Không tìm thấy phòng chiếu thuộc cụm rạp.");
        }

        String result = hallDAO.toggleStatus(hallId);
        if ("SUCCESS".equals(result)) {
            return new Result<>(Status.SUCCESS, "Đổi trạng thái phòng chiếu thành công.");
        } else if ("HAS_FUTURE_BOOKINGS".equals(result)) {
            return new Result<>(Status.HAS_FUTURE_BOOKINGS, "Phòng chiếu đang có suất chiếu tương lai đã bán vé.");
        } else {
            return new Result<>(Status.FAILED, "Không thể đổi trạng thái phòng.");
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

    public Result<String> toggleSeatMaintenance(int hallId, int seatId) {
        if (hallId <= 0 || seatId <= 0) {
            return new Result<>(Status.INVALID_ID, "Mã ghế không hợp lệ.");
        }

        String result = seatDAO.toggleSeatMaintenance(seatId, hallId);
        if ("SUCCESS".equals(result)) {
            return new Result<>(Status.SUCCESS, "Đã cập nhật trạng thái bảo trì ghế.");
        } else if (result != null && result.startsWith("HAS_FUTURE_BOOKINGS:")) {
            String bookedTime = result.substring("HAS_FUTURE_BOOKINGS:".length());
            String msg = "CẢNH BÁO: Ghế này đã được đặt vé ở suất chiếu [" + bookedTime + "]. Vui lòng đổi chỗ ngồi thủ công cho khách hàng trước khi khóa bảo trì.";
            return new Result<>(Status.BOOKED_SEAT, msg);
        } else {
            return new Result<>(Status.FAILED, "Không thể cập nhật bảo trì ghế.");
        }
    }

    public Result<Void> saveSeatLayout(int hallId, List<SeatConfigDTO> seatList, boolean isRegenerating) {
        if (hallId <= 0) {
            return new Result<>(Status.INVALID_ID, "Phòng chiếu không hợp lệ.");
        }
        if (isRegenerating && seatDAO.hasFutureBookedShowtimes(hallId)) {
            return new Result<>(Status.CANNOT_REGENERATE, "Phòng chiếu đã có lịch chiếu mở bán vé, không thể tái cấu trúc lưới ghế.");
        }
        if (seatList == null || seatList.isEmpty()) {
            return new Result<>(Status.EMPTY_DATA, "Dữ liệu cấu hình ghế trống.");
        }

        // Kiểm tra ghế bảo trì có khách đặt trước không
        for (SeatConfigDTO s : seatList) {
            if ("MAINTENANCE".equalsIgnoreCase(s.getStatus())) {
                String bookedShowtime = seatDAO.checkFutureBookingForSeat(hallId, s.getSeatRow(), s.getSeatNumber());
                if (bookedShowtime != null) {
                    String seatLabel = s.getSeatRow() + s.getSeatNumber();
                    String errorMsg = "CẢNH BÁO: Ghế " + seatLabel + " đã được đặt vé ở suất chiếu [" + bookedShowtime + "]. Vui lòng đổi chỗ ngồi thủ công cho khách hàng trước khi khóa bảo trì.";
                    return new Result<>(Status.BOOKED_SEAT, errorMsg);
                }
            }
        }

        boolean ok = seatDAO.saveSeatLayout(hallId, seatList, isRegenerating);
        if (ok) {
            return new Result<>(Status.SUCCESS, "Đã lưu cấu hình sơ đồ ghế thành công.");
        } else {
            return new Result<>(Status.FAILED, "Không thể lưu sơ đồ ghế.");
        }
    }
}
