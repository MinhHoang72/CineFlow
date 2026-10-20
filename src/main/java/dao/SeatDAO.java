package dao;
import java.sql.*;
import java.time.LocalDateTime;
import model.entity.SeatAvailability;

import model.entity.Seat;
import model.dto.pricing.SeatConfigDTO;
import model.entity.Showtime;
import utils.DBContext;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Xử lý truy vấn và cập nhật dữ liệu Ghế ngồi (Seats).
 * Phục vụ nghiệp vụ đặt vé (Seat Map) và phân hệ cấu hình sơ đồ ghế (UC-FE-03.3).
 */
public class SeatDAO {

    /**
     * Sơ đồ ghế của phòng chiếu, kèm trạng thái đã giữ/đã bán và giá vé cho suất chiếu (giữ nguyên cho luồng đặt vé).
     */
    public List<Seat> getSeatMap(Showtime showtime) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            return getSeatMap(conn, showtime);
        }
    }

    static List<Seat> getSeatMap(Connection conn, Showtime showtime) throws SQLException {
        String sql = "SELECT se.SeatID, se.SeatRow, se.SeatNumber, se.SeatType, se.Status, "
                   + "  CASE WHEN EXISTS (SELECT 1 FROM BookingSeats bs "
                   + "       WHERE bs.ShowtimeID = ? AND bs.SeatID = se.SeatID "
                   + "         AND (bs.SeatStatus = 'CONFIRMED' "
                   + "              OR (bs.SeatStatus = 'HELD' AND bs.HoldUntil > SYSDATETIME()))) "
                   + "  THEN 1 ELSE 0 END AS Taken "
                   + "FROM Seats se WHERE se.HallID = ? ORDER BY se.SeatRow, se.SeatNumber";
        Map<String, BigDecimal> surcharges = loadSeatSurcharges(conn, showtime);
        List<Seat> seats = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, showtime.getId());
            ps.setInt(2, showtime.getHallId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String type = rs.getString("SeatType");
                    seats.add(new Seat(rs.getInt("SeatID"), rs.getString("SeatRow"), rs.getInt("SeatNumber"),
                            type, "AVAILABLE".equals(rs.getString("Status")), rs.getInt("Taken") == 1,
                            priceFor(showtime.getBasePrice(), type, surcharges)));
                }
            }
        }
        return seats;
    }

    private static Map<String, BigDecimal> loadSeatSurcharges(Connection conn, Showtime showtime) throws SQLException {
        Map<String, BigDecimal> map = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT TargetValue, SUM(Amount) AS Amount FROM Surcharges "
              + "WHERE SurchargeType = 'SEAT' AND EffectiveFrom <= ? AND (EffectiveTo IS NULL OR EffectiveTo > ?) "
              + "GROUP BY TargetValue")) {
            ps.setTimestamp(1, showtime.getStartTime());
            ps.setTimestamp(2, showtime.getStartTime());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) map.put(rs.getString("TargetValue"), rs.getBigDecimal("Amount"));
            }
        }
        return map;
    }

    private static BigDecimal priceFor(BigDecimal base, String seatType, Map<String, BigDecimal> surcharges) {
        return base.add(surcharges.getOrDefault(seatType, BigDecimal.ZERO));
    }

    // =========================================================================
    // CÁC PHƯƠNG THỨC MỚI PHỤC VỤ UC-FE-03.3 (SEAT CONFIGURATION & MAINTENANCE)
    // =========================================================================

    /**
     * Lấy toàn bộ danh sách cấu hình ghế của một phòng chiếu.
     *
     * @param hallId mã phòng chiếu
     * @return danh sách SeatConfigDTO
     */
    public List<SeatConfigDTO> getSeatConfigsByHall(int hallId) {
        List<SeatConfigDTO> list = new ArrayList<>();
        String sql = "SELECT SeatID, HallID, SeatRow, SeatNumber, SeatType, Status "
                   + "FROM Seats WHERE HallID = ? "
                   + "ORDER BY SeatRow ASC, SeatNumber ASC";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, hallId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SeatConfigDTO dto = new SeatConfigDTO();
                    dto.setSeatId(rs.getInt("SeatID"));
                    dto.setHallId(rs.getInt("HallID"));
                    dto.setSeatRow(rs.getString("SeatRow"));
                    dto.setSeatNumber(rs.getInt("SeatNumber"));
                    dto.setSeatType(rs.getString("SeatType"));
                    dto.setStatus(rs.getString("Status"));
                    list.add(dto);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    /**
     * Đếm tổng số ghế của phòng chiếu theo điều kiện tìm kiếm và bộ lọc.
     * Phục vụ phân trang (Pagination) danh sách ghế.
     *
     * @param hallId mã phòng chiếu
     * @param search từ khóa tìm kiếm (theo hàng 'A' hoặc mã ghế 'A1' hoặc số '1')
     * @param seatType loại ghế ('ALL', 'STANDARD', 'VIP', 'SWEETBOX')
     * @param status trạng thái ('ALL', 'AVAILABLE', 'MAINTENANCE')
     * @return tổng số ghế tìm thấy
     */
    public int countSeatsByHall(int hallId, String search, String seatType, String status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM Seats WHERE HallID = ? ");
        List<Object> params = new ArrayList<>();
        params.add(hallId);

        if (search != null && !search.trim().isEmpty()) {
            String kw = "%" + search.trim().toLowerCase() + "%";
            sql.append("AND (LOWER(SeatRow) LIKE ? OR LOWER(CONCAT(SeatRow, CAST(SeatNumber AS VARCHAR))) LIKE ? OR CAST(SeatNumber AS VARCHAR) LIKE ?) ");
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }

        if (seatType != null && !seatType.trim().isEmpty() && !"ALL".equalsIgnoreCase(seatType)) {
            sql.append("AND UPPER(SeatType) = ? ");
            params.add(seatType.trim().toUpperCase());
        }

        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            sql.append("AND UPPER(Status) = ? ");
            params.add(status.trim().toUpperCase());
        }

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Lấy danh sách ghế của một phòng chiếu có phân trang (Pagination).
     *
     * @param hallId mã phòng chiếu
     * @param search từ khóa tìm kiếm
     * @param seatType loại ghế
     * @param status trạng thái
     * @param page số trang hiện tại
     * @param pageSize số ghế mỗi trang
     * @return danh sách các SeatConfigDTO trên trang
     */
    public List<SeatConfigDTO> getSeatsByHallPaged(int hallId, String search, String seatType, String status, int page, int pageSize) {
        if (page < 1) {
            page = 1;
        }
        if (pageSize < 1) {
            pageSize = 12;
        }
        int offset = (page - 1) * pageSize;

        List<SeatConfigDTO> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT SeatID, HallID, SeatRow, SeatNumber, SeatType, Status "
            + "FROM Seats WHERE HallID = ? "
        );

        List<Object> params = new ArrayList<>();
        params.add(hallId);

        if (search != null && !search.trim().isEmpty()) {
            String kw = "%" + search.trim().toLowerCase() + "%";
            sql.append("AND (LOWER(SeatRow) LIKE ? OR LOWER(CONCAT(SeatRow, CAST(SeatNumber AS VARCHAR))) LIKE ? OR CAST(SeatNumber AS VARCHAR) LIKE ?) ");
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }

        if (seatType != null && !seatType.trim().isEmpty() && !"ALL".equalsIgnoreCase(seatType)) {
            sql.append("AND UPPER(SeatType) = ? ");
            params.add(seatType.trim().toUpperCase());
        }

        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            sql.append("AND UPPER(Status) = ? ");
            params.add(status.trim().toUpperCase());
        }

        sql.append("ORDER BY SeatRow ASC, SeatNumber ASC ");
        sql.append("OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        params.add(offset);
        params.add(pageSize);

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SeatConfigDTO dto = new SeatConfigDTO();
                    dto.setSeatId(rs.getInt("SeatID"));
                    dto.setHallId(rs.getInt("HallID"));
                    dto.setSeatRow(rs.getString("SeatRow"));
                    dto.setSeatNumber(rs.getInt("SeatNumber"));
                    dto.setSeatType(rs.getString("SeatType"));
                    dto.setStatus(rs.getString("Status"));
                    list.add(dto);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    /**
     * Bật / Tắt trạng thái bảo trì cho một ghế đơn lẻ (Trường hợp B).
     * Tự động đồng bộ lại Capacity (tổng số ghế AVAILABLE) của phòng chiếu.
     *
     * @param seatId mã định danh ghế
     * @param hallId mã phòng chiếu
     * @return chuỗi kết quả: "SUCCESS", "HAS_FUTURE_BOOKINGS:[thời gian]", "FAILED"
     */
    public String toggleSeatMaintenance(int seatId, int hallId) {
        String queryCurrent = "SELECT SeatRow, SeatNumber, Status FROM Seats WHERE SeatID = ? AND HallID = ?";
        String seatRow = null;
        int seatNumber = 0;
        String currentStatus = null;

        try (Connection conn = DBContext.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(queryCurrent)) {
                ps.setInt(1, seatId);
                ps.setInt(2, hallId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        seatRow = rs.getString("SeatRow");
                        seatNumber = rs.getInt("SeatNumber");
                        currentStatus = rs.getString("Status");
                    } else {
                        return "FAILED";
                    }
                }
            }

            // Nếu ghế đang hoạt động (AVAILABLE) và chuẩn bị chuyển sang bảo trì (MAINTENANCE)
            // Cần kiểm tra Alt 2: Ghế này có khách đặt trong suất chiếu tương lai không
            if ("AVAILABLE".equalsIgnoreCase(currentStatus)) {
                String bookedShowtime = checkFutureBookingForSeat(hallId, seatRow, seatNumber);
                if (bookedShowtime != null) {
                    return "HAS_FUTURE_BOOKINGS:" + bookedShowtime;
                }
            }

            String newStatus = "AVAILABLE".equalsIgnoreCase(currentStatus) ? "MAINTENANCE" : "AVAILABLE";

            // Cập nhật trạng thái ghế
            String updateSql = "UPDATE Seats SET Status = ? WHERE SeatID = ? AND HallID = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setString(1, newStatus);
                ps.setInt(2, seatId);
                ps.setInt(3, hallId);
                ps.executeUpdate();
            }

            // Đồng bộ lại Capacity của phòng chiếu (Total Capacity = số ghế AVAILABLE)
            String syncCapacitySql = "UPDATE Halls SET Capacity = (SELECT COUNT(*) FROM Seats WHERE HallID = ? AND UPPER(Status) = 'AVAILABLE') WHERE HallID = ?";
            try (PreparedStatement ps = conn.prepareStatement(syncCapacitySql)) {
                ps.setInt(1, hallId);
                ps.setInt(2, hallId);
                ps.executeUpdate();
            }

            return "SUCCESS";
        } catch (SQLException e) {
            e.printStackTrace();
            return "FAILED";
        }
    }

    /**
     * Alt 2: Kiểm tra xem vị trí ghế cụ thể có đang được khách đặt trước trong một suất chiếu sắp tới hay không.
     *
     * @param hallId mã phòng chiếu
     * @param seatRow hàng ghế (VD: 'A', 'B')
     * @param seatNumber số ghế (VD: 1, 2)
     * @return Chuỗi thời gian suất chiếu bị trùng nếu có (VD: "19:30 25/10/2026"), ngược lại trả về null.
     */
    public String checkFutureBookingForSeat(int hallId, String seatRow, int seatNumber) {
        String sql = "SELECT TOP 1 s.StartTime, b.BookingCode "
                   + "FROM Showtimes s "
                   + "JOIN Bookings b ON b.ShowtimeID = s.ShowtimeID "
                   + "JOIN BookingSeats bs ON bs.BookingID = b.BookingID "
                   + "JOIN Seats se ON se.SeatID = bs.SeatID "
                   + "WHERE se.HallID = ? "
                   + "  AND se.SeatRow = ? "
                   + "  AND se.SeatNumber = ? "
                   + "  AND s.StartTime > GETDATE() "
                   + "  AND UPPER(s.Status) = 'SCHEDULED' "
                   + "  AND UPPER(b.BookingStatus) IN ('CONFIRMED', 'COMPLETED') "
                   + "ORDER BY s.StartTime ASC";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, hallId);
            ps.setString(2, seatRow);
            ps.setInt(3, seatNumber);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp startTime = rs.getTimestamp("StartTime");
                    if (startTime != null) {
                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy");
                        return sdf.format(startTime);
                    }
                    return "suất chiếu tương lai";
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Alt 1: Kiểm tra xem phòng chiếu đã phát sinh lịch chiếu tương lai có vé đặt hay chưa.
     *
     * @param hallId mã phòng chiếu
     * @return true nếu có suất chiếu tương lai đã bán vé
     */
    public boolean hasFutureBookedShowtimes(int hallId) {
        String sql = "SELECT COUNT(*) "
                   + "FROM Showtimes s "
                   + "JOIN Bookings b ON b.ShowtimeID = s.ShowtimeID "
                   + "WHERE s.HallID = ? "
                   + "  AND s.StartTime > GETDATE() "
                   + "  AND UPPER(s.Status) = 'SCHEDULED' "
                   + "  AND UPPER(b.BookingStatus) IN ('CONFIRMED', 'COMPLETED')";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, hallId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Lưu toàn bộ cấu hình sơ đồ ghế vào CSDL trong một Transaction an toàn,
     * đồng thời tự động đồng bộ lại tổng sức chứa khả dụng (Capacity) của phòng chiếu.
     *
     * @param hallId mã phòng chiếu
     * @param seats danh sách cấu hình ghế trên ma trận
     * @param isRegeneratingGrid cờ đánh dấu có phải đang tạo lại lưới ma trận từ đầu hay không
     * @return true nếu lưu và đồng bộ thành công, ngược lại false
     */
    public boolean saveSeatLayout(int hallId, List<SeatConfigDTO> seats, boolean isRegeneratingGrid) {
        // Alt 1: Nếu cố tình khởi tạo lại lưới trong khi phòng đã có lịch chiếu mở bán vé -> Chặn ngay
        if (isRegeneratingGrid && hasFutureBookedShowtimes(hallId)) {
            return false;
        }

        String mergeSql = "MERGE Seats AS target "
                        + "USING (SELECT ? AS HallID, ? AS SeatRow, ? AS SeatNumber) AS source "
                        + "ON target.HallID = source.HallID AND target.SeatRow = source.SeatRow AND target.SeatNumber = source.SeatNumber "
                        + "WHEN MATCHED THEN UPDATE SET SeatType = ?, Status = ? "
                        + "WHEN NOT MATCHED THEN INSERT (HallID, SeatRow, SeatNumber, SeatType, Status) VALUES (source.HallID, source.SeatRow, source.SeatNumber, ?, ?);";

        String syncCapacitySql = "UPDATE Halls SET Capacity = (SELECT COUNT(*) FROM Seats WHERE HallID = ? AND Status = 'AVAILABLE') WHERE HallID = ?";

        Connection conn = null;
        try {
            conn = DBContext.getConnection();
            conn.setAutoCommit(false);

            // 1. Nếu khởi tạo lại kích thước lưới (Generate Layout Grid):
            // Dọn dẹp những ghế cũ không còn nằm trong lưới mới (nếu chưa từng phát sinh vé đặt)
            if (isRegeneratingGrid && seats != null && !seats.isEmpty()) {
                // Xây dựng danh sách vị trí ghế hợp lệ để giữ lại
                // Những ghế không còn nằm trong danh sách nếu chưa có vé thì xóa, nếu đã có vé thì đánh dấu UNAVAILABLE
                StringBuilder keepPositions = new StringBuilder();
                for (int i = 0; i < seats.size(); i++) {
                    SeatConfigDTO s = seats.get(i);
                    keepPositions.append("('").append(s.getSeatRow()).append("', ").append(s.getSeatNumber()).append(")");
                    if (i < seats.size() - 1) {
                        keepPositions.append(", ");
                    }
                }

                if (keepPositions.length() > 0) {
                    // Đánh dấu các ghế nằm ngoài lưới mới thành UNAVAILABLE
                    String disableRemovedSql = "UPDATE Seats SET Status = 'UNAVAILABLE' WHERE HallID = ? "
                                             + "AND (SeatRow + CAST(SeatNumber AS VARCHAR)) NOT IN ("
                                             + "    SELECT SeatRow + CAST(SeatNumber AS VARCHAR) FROM (VALUES " + keepPositions + ") AS V(SeatRow, SeatNumber)"
                                             + ")";
                    try (PreparedStatement psDisable = conn.prepareStatement(disableRemovedSql)) {
                        psDisable.setInt(1, hallId);
                        psDisable.executeUpdate();
                    }

                    // Xóa an toàn những ghế ngoài lưới chưa từng có vé đặt
                    String cleanRemovedSql = "DELETE FROM Seats WHERE HallID = ? AND Status = 'UNAVAILABLE' "
                                           + "AND SeatID NOT IN (SELECT SeatID FROM BookingSeats)";
                    try (PreparedStatement psClean = conn.prepareStatement(cleanRemovedSql)) {
                        psClean.setInt(1, hallId);
                        psClean.executeUpdate();
                    }
                }
            }

            // 2. Thực thi MERGE / Cập nhật chi tiết từng ghế
            if (seats != null && !seats.isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement(mergeSql)) {
                    for (SeatConfigDTO s : seats) {
                        // Chuẩn hóa loại ghế: STANDARD, VIP, SWEETBOX
                        String type = "STANDARD";
                        if ("VIP".equalsIgnoreCase(s.getSeatType())) {
                            type = "VIP";
                        } else if ("SWEETBOX".equalsIgnoreCase(s.getSeatType())) {
                            type = "SWEETBOX";
                        }

                        // Chuẩn hóa trạng thái: AVAILABLE, MAINTENANCE, UNAVAILABLE
                        String status = "AVAILABLE";
                        if ("MAINTENANCE".equalsIgnoreCase(s.getStatus())) {
                            status = "MAINTENANCE";
                        } else if ("UNAVAILABLE".equalsIgnoreCase(s.getStatus())) {
                            status = "UNAVAILABLE";
                        }

                        ps.setInt(1, hallId);
                        ps.setString(2, s.getSeatRow().trim().toUpperCase());
                        ps.setInt(3, s.getSeatNumber());

                        ps.setString(4, type);
                        ps.setString(5, status);

                        ps.setString(6, type);
                        ps.setString(7, status);

                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

            // 3. Tự động đồng bộ lại tổng sức chứa khả dụng (Capacity) của phòng chiếu:
            // Total Capacity = Tổng số ghế có trạng thái 'AVAILABLE'
            try (PreparedStatement psSync = conn.prepareStatement(syncCapacitySql)) {
                psSync.setInt(1, hallId);
                psSync.setInt(2, hallId);
                psSync.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }


    public List<SeatAvailability>
            getSeatAvailability(
                    int showtimeId) {

        List<SeatAvailability> list =
                new ArrayList<>();

        String sql =
                "SELECT "
                + "s.SeatID, "
                + "s.SeatRow, "
                + "s.SeatNumber, "
                + "s.SeatType, "

                + "CASE "

                + "WHEN bs.SeatStatus = 'CONFIRMED' "
                + "THEN 'BOOKED' "

                + "WHEN bs.SeatStatus = 'HELD' "
                + "THEN 'HELD' "

                + "ELSE 'AVAILABLE' "

                + "END AS AvailabilityStatus "

                + "FROM Showtimes sh "

                + "JOIN Seats s "
                + "ON s.HallID = sh.HallID "

                + "LEFT JOIN ( "

                + "SELECT bs2.SeatID, "
                + "bs2.SeatStatus "

                + "FROM BookingSeats bs2 "

                + "JOIN Bookings b "
                + "ON b.BookingID = bs2.BookingID "

                + "WHERE b.ShowtimeID = ? "

                + "AND b.BookingStatus "
                + "IN ('PENDING','CONFIRMED') "

                + "AND bs2.SeatStatus "
                + "IN ('HELD','CONFIRMED') "

                + ") bs "

                + "ON bs.SeatID = s.SeatID "

                + "WHERE sh.ShowtimeID = ? "

                + "AND s.Status = 'AVAILABLE' "

                + "ORDER BY "
                + "s.SeatRow, "
                + "s.SeatNumber";

        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(
                    1,
                    showtimeId
            );

            ps.setInt(
                    2,
                    showtimeId
            );

            ResultSet rs =
                    ps.executeQuery();

            while (rs.next()) {

                SeatAvailability seat =
                        new SeatAvailability();

                seat.setSeatId(
                        rs.getInt("SeatID")
                );

                seat.setSeatRow(
                        rs.getString("SeatRow")
                );

                seat.setSeatNumber(
                        rs.getInt("SeatNumber")
                );

                seat.setSeatType(
                        rs.getString("SeatType")
                );

                seat.setAvailabilityStatus(
                        rs.getString(
                                "AvailabilityStatus"
                        )
                );

                list.add(seat);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

}
