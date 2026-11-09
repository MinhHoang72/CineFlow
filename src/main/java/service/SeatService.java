package service;

import com.google.gson.Gson;
import dao.CentreDAO;
import dao.HallDAO;
import dao.SeatDAO;
import model.dto.common.PageDTO;
import model.dto.pricing.SeatConfigDTO;
import model.entity.Centre;
import model.entity.Hall;

import java.util.List;

/**
 * Service xử lý toàn bộ nghiệp vụ Ghế ngồi và Sơ đồ ghế (Seat Management).
 * Chuẩn hóa theo phong cách VoucherService:
 * - Trả về trực tiếp dữ liệu (List, PageDTO, JSON, Entity), không trả về cờ String (success/failed) gây rối code.
 * - Đóng gói logic nghiệp vụ kiểm tra vé đặt và đồng bộ sức chứa.
 */
public class SeatService {

    private final SeatDAO seatDAO;
    private final HallDAO hallDAO;
    private final CentreDAO centreDAO;
    private final Gson gson;

    public SeatService() {
        this.seatDAO = new SeatDAO();
        this.hallDAO = new HallDAO();
        this.centreDAO = new CentreDAO();
        this.gson = new Gson();
    }

    public Hall getHallById(int hallId) {
        if (hallId <= 0) return null;
        return hallDAO.getHallById(hallId);
    }

    public Centre getCentreById(int centreId) {
        if (centreId <= 0) return null;
        return centreDAO.getCentreById(centreId);
    }

    public boolean hasFutureBookedShowtimes(int hallId) {
        return seatDAO.hasFutureBookedShowtimes(hallId);
    }

    /**
     * Lấy toàn bộ danh sách ghế của phòng chiếu.
     */
    public List<SeatConfigDTO> getSeatConfigsByHall(int hallId) {
        if (hallId <= 0) return List.of();
        return seatDAO.getSeatConfigsByHall(hallId);
    }

    /**
     * Chuyển đổi danh sách ghế sang chuỗi JSON để giao diện JSP vẽ sơ đồ.
     */
    public String getSeatMatrixJson(int hallId) {
        List<SeatConfigDTO> seats = getSeatConfigsByHall(hallId);
        return gson.toJson(seats);
    }

    /**
     * Tìm kiếm và phân trang danh sách ghế (dùng cho bảng tra cứu).
     */
    public PageDTO<SeatConfigDTO> searchSeatsPaged(int hallId, String search, String seatType, String seatStatus, int page, int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 10;

        int total = seatDAO.countSeatsByHall(hallId, search, seatType, seatStatus);
        List<SeatConfigDTO> items = seatDAO.getSeatsByHallPaged(hallId, search, seatType, seatStatus, page, pageSize);
        return new PageDTO<>(items, page, pageSize, total);
    }

    /**
     * Lưu cấu hình sơ đồ ghế vào Database.
     * Kiểm tra chặt chẽ điều kiện: không cho tạo lại lưới nếu phòng đang có suất chiếu đã bán vé.
     */
    public boolean saveSeatLayout(int hallId, List<SeatConfigDTO> seatList, boolean isRegenerating) {
        if (hallId <= 0 || seatList == null || seatList.isEmpty()) {
            return false;
        }
        if (isRegenerating && seatDAO.hasFutureBookedShowtimes(hallId)) {
            return false;
        }
        return seatDAO.saveSeatLayout(hallId, seatList, isRegenerating);
    }

    /**
     * Khóa / Mở bảo trì nhanh một vị trí ghế.
     */
    public boolean toggleSeatMaintenance(int seatId, int hallId) {
        if (seatId <= 0 || hallId <= 0) {
            return false;
        }
        String result = seatDAO.toggleSeatMaintenance(seatId, hallId);
        return "SUCCESS".equalsIgnoreCase(result);
    }
}
