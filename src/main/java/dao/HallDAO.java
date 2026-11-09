package dao;

import model.dto.common.PageDTO;
import model.entity.Hall;
import utils.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Xử lý truy vấn và cập nhật dữ liệu Phòng chiếu (Halls) trong hệ thống CineFlow.
 * Phục vụ Use Case UC-FE-03.2 (Manage Cinema Halls) và các quy tắc nghiệp vụ BR-HS-01, Alt 1, Alt 2.
 */
public class HallDAO {

    /**
     * Tìm kiếm và phân trang danh sách phòng chiếu trực thuộc một cụm rạp.
     * Cấu trúc chuẩn 1 hàm duy nhất như VoucherDAO & CentreDAO.
     */
    public PageDTO<Hall> search(int centreId, String search, String status, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;

        StringBuilder where = new StringBuilder(" WHERE h.CentreID = ? ");
        List<Object> params = new ArrayList<>();
        params.add(centreId);

        // Lọc theo từ khóa tìm kiếm tên phòng
        if (search != null && !search.trim().isEmpty()) {
            where.append("AND (h.HallName COLLATE Latin1_General_100_CI_AI LIKE ?) ");
            String safeKey = search.trim().replace("[", "[[]").replace("%", "[%]").replace("_", "[_]");
            params.add("%" + safeKey + "%");
        }

        // Lọc theo trạng thái hoặc định dạng kỹ thuật
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            if ("ACTIVE".equalsIgnoreCase(status)) {
                where.append("AND UPPER(h.Status) = 'AVAILABLE' ");
            } else if ("INACTIVE".equalsIgnoreCase(status)) {
                where.append("AND UPPER(h.Status) IN ('INACTIVE', 'MAINTENANCE') ");
            } else if ("2D".equalsIgnoreCase(status) || "3D".equalsIgnoreCase(status)) {
                where.append("AND UPPER(h.HallType) = ? ");
                params.add(status.trim().toUpperCase());
            }
        }

        int total = 0;
        List<Hall> items = new ArrayList<>();

        try (Connection conn = DBContext.getConnection()) {
            // 1. Đếm tổng số bản ghi
            String countSql = "SELECT COUNT(*) FROM Halls h " + where;
            try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        total = rs.getInt(1);
                    }
                }
            }

            // 2. Lấy dữ liệu trang hiện tại với OFFSET ... FETCH NEXT
            int offset = (page - 1) * size;
            String dataSql = "SELECT h.HallID, h.CentreID, h.HallName, h.HallType, h.Capacity, h.Status, h.CreatedAt, c.CentreName "
                    + "FROM Halls h "
                    + "JOIN Centres c ON c.CentreID = h.CentreID "
                    + where
                    + "ORDER BY h.HallID ASC "
                    + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

            try (PreparedStatement ps = conn.prepareStatement(dataSql)) {
                int idx = 1;
                for (Object param : params) {
                    ps.setObject(idx++, param);
                }
                ps.setInt(idx++, offset);
                ps.setInt(idx, size);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Hall h = new Hall();
                        h.setHallId(rs.getInt("HallID"));
                        h.setCentreId(rs.getInt("CentreID"));
                        h.setCentreName(rs.getString("CentreName"));
                        h.setHallName(rs.getString("HallName"));
                        h.setHallType(rs.getString("HallType"));
                        h.setCapacity(rs.getInt("Capacity"));
                        h.setStatus(rs.getString("Status"));
                        h.setCreatedAt(rs.getTimestamp("CreatedAt"));
                        items.add(h);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new PageDTO<>(items, page, size, total);
    }

    /**
     * Lấy danh sách các phòng chiếu trực thuộc một Cụm rạp (Centre).
     *
     * @param centreId mã định danh cụm rạp
     * @return danh sách các phòng chiếu
     */
    public List<Hall> getHallsByCentre(int centreId) {
        List<Hall> list = new ArrayList<>();
        String sql = "SELECT h.HallID, h.CentreID, h.HallName, h.HallType, h.Capacity, h.Status, h.CreatedAt, c.CentreName "
                   + "FROM Halls h "
                   + "JOIN Centres c ON c.CentreID = h.CentreID "
                   + "WHERE h.CentreID = ? "
                   + "ORDER BY h.HallID ASC";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, centreId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Hall h = new Hall();
                    h.setHallId(rs.getInt("HallID"));
                    h.setCentreId(rs.getInt("CentreID"));
                    h.setCentreName(rs.getString("CentreName"));
                    h.setHallName(rs.getString("HallName"));
                    h.setHallType(rs.getString("HallType"));
                    h.setCapacity(rs.getInt("Capacity"));
                    h.setStatus(rs.getString("Status"));
                    h.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    list.add(h);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    /**
     * Lấy thông tin chi tiết một phòng chiếu theo mã HallID.
     *
     * @param hallId mã định danh phòng chiếu
     * @return đối tượng Hall hoặc null nếu không tìm thấy
     */
    public Hall getHallById(int hallId) {
        String sql = "SELECT h.HallID, h.CentreID, h.HallName, h.HallType, h.Capacity, h.Status, h.CreatedAt, c.CentreName "
                   + "FROM Halls h "
                   + "JOIN Centres c ON c.CentreID = h.CentreID "
                   + "WHERE h.HallID = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, hallId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Hall h = new Hall();
                    h.setHallId(rs.getInt("HallID"));
                    h.setCentreId(rs.getInt("CentreID"));
                    h.setCentreName(rs.getString("CentreName"));
                    h.setHallName(rs.getString("HallName"));
                    h.setHallType(rs.getString("HallType"));
                    h.setCapacity(rs.getInt("Capacity"));
                    h.setStatus(rs.getString("Status"));
                    h.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    return h;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Alt 1: Kiểm tra xem tên phòng chiếu đã tồn tại tại Cụm rạp này hay chưa.
     * Mỗi cụm rạp không được có hai phòng chiếu trùng tên nhau.
     *
     * @param centreId mã cụm rạp
     * @param hallName tên phòng chiếu cần kiểm tra
     * @param excludeHallId mã phòng chiếu cần bỏ qua (truyền 0 nếu thêm mới)
     * @return true nếu tên phòng đã tồn tại trong cùng cụm rạp, ngược lại false
     */
    public boolean isHallNameExists(int centreId, String hallName, int excludeHallId) {
        if (hallName == null || hallName.trim().isEmpty()) {
            return false;
        }

        String sql = "SELECT COUNT(*) FROM Halls "
                   + "WHERE CentreID = ? "
                   + "  AND LOWER(LTRIM(RTRIM(HallName))) = LOWER(LTRIM(RTRIM(?))) "
                   + "  AND HallID <> ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, centreId);
            ps.setString(2, hallName.trim());
            ps.setInt(3, excludeHallId);

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
     * Alt 2 & Business Rule: Kiểm tra xem phòng chiếu có đang gắn với các suất chiếu tương lai đã bán vé hay không.
     * Không thể chuyển phòng sang Inactive nếu phòng đang có suất chiếu tương lai đã mở bán và phát sinh vé đặt.
     *
     * @param hallId mã phòng chiếu
     * @return true nếu có suất chiếu tương lai đã phát sinh vé đặt, ngược lại false
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
     * Thêm mới một phòng chiếu vào CSDL.
     * Trạng thái khởi tạo mặc định là Active (trong DB lưu là 'AVAILABLE' để tương thích với kiểm tra CK_Halls_Status).
     *
     * @param hall đối tượng Hall cần thêm
     * @return true nếu thêm thành công, ngược lại false
     */
    public boolean addHall(Hall hall) {
        String sql = "INSERT INTO Halls (CentreID, HallName, HallType, Capacity, Status, CreatedAt) "
                   + "VALUES (?, ?, ?, ?, ?, GETDATE())";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Chuẩn hóa định dạng HallType: '2D' hoặc '3D'
            String hallType = "3D".equalsIgnoreCase(hall.getHallType()) ? "3D" : "2D";

            // Chuẩn hóa trạng thái: Active lưu 'AVAILABLE', Inactive lưu 'INACTIVE'
            String status = "INACTIVE".equalsIgnoreCase(hall.getStatus()) ? "INACTIVE" : "AVAILABLE";

            // Sức chứa tối thiểu là 1
            int capacity = hall.getCapacity() > 0 ? hall.getCapacity() : 60;

            ps.setInt(1, hall.getCentreId());
            ps.setString(2, hall.getHallName().trim());
            ps.setString(3, hallType);
            ps.setInt(4, capacity);
            ps.setString(5, status);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Cập nhật thông tin phòng chiếu (Tên phòng, Định dạng Standard 2D/3D, Sức chứa, Trạng thái).
     *
     * @param hall đối tượng Hall chứa dữ liệu mới
     * @return true nếu cập nhật thành công, ngược lại false
     */
    public boolean updateHall(Hall hall) {
        String sql = "UPDATE Halls "
                   + "SET HallName = ?, HallType = ?, Capacity = ?, Status = ? "
                   + "WHERE HallID = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String hallType = "3D".equalsIgnoreCase(hall.getHallType()) ? "3D" : "2D";
            String status = "INACTIVE".equalsIgnoreCase(hall.getStatus()) ? "INACTIVE" : "AVAILABLE";
            int capacity = hall.getCapacity() > 0 ? hall.getCapacity() : 60;

            ps.setString(1, hall.getHallName().trim());
            ps.setString(2, hallType);
            ps.setInt(3, capacity);
            ps.setString(4, status);
            ps.setInt(5, hall.getHallId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Toggle Status: Bật/tắt trạng thái (Active <-> Inactive) của phòng chiếu.
     * Kết quả trả về:
     * - "SUCCESS": Đổi trạng thái thành công.
     * - "HAS_FUTURE_BOOKINGS": Bị chặn do có suất chiếu tương lai đã bán vé (Alt 2).
     * - "FAILED": Thao tác thất bại hoặc không tìm thấy phòng.
     *
     * @param hallId mã phòng chiếu
     * @return chuỗi kết quả
     */
    public String toggleStatus(int hallId) {
        Hall currentHall = getHallById(hallId);
        if (currentHall == null) {
            return "FAILED";
        }

        // Nếu phòng đang hoạt động (Active / AVAILABLE) và người dùng muốn tắt sang Inactive
        if (currentHall.isActive()) {
            if (hasFutureBookedShowtimes(hallId)) {
                return "HAS_FUTURE_BOOKINGS";
            }
        }

        String newStatus = currentHall.isActive() ? "INACTIVE" : "AVAILABLE";
        String sql = "UPDATE Halls SET Status = ? WHERE HallID = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newStatus);
            ps.setInt(2, hallId);

            return ps.executeUpdate() > 0 ? "SUCCESS" : "FAILED";
        } catch (SQLException e) {
            e.printStackTrace();
            return "FAILED";
        }
    }

    /**
     * Đếm số lượng phòng chiếu của một cụm rạp theo trạng thái phục vụ hiển thị thẻ thống kê.
     *
     * @param centreId mã cụm rạp
     * @param status 'ALL', 'ACTIVE', 'INACTIVE'
     * @return số lượng phòng
     */
    public int countHallsByStatus(int centreId, String status) {
        String sql;
        if ("ACTIVE".equalsIgnoreCase(status)) {
            sql = "SELECT COUNT(*) FROM Halls WHERE CentreID = ? AND UPPER(Status) = 'AVAILABLE'";
        } else if ("INACTIVE".equalsIgnoreCase(status)) {
            sql = "SELECT COUNT(*) FROM Halls WHERE CentreID = ? AND UPPER(Status) IN ('INACTIVE', 'MAINTENANCE')";
        } else {
            sql = "SELECT COUNT(*) FROM Halls WHERE CentreID = ?";
        }

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, centreId);
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
     * Tính tổng sức chứa (ghế) của tất cả các phòng chiếu trong một cụm rạp.
     *
     * @param centreId mã cụm rạp
     * @return tổng số ghế
     */
    public int sumCapacityByCentre(int centreId) {
        String sql = "SELECT COALESCE(SUM(Capacity), 0) FROM Halls WHERE CentreID = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, centreId);
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
    public List<Hall> getAvailableHallsByCentre(int centreId) {
        List<Hall> available = new ArrayList<>();
        for (Hall hall : getHallsByCentre(centreId)) {
            if ("AVAILABLE".equals(hall.getStatus())) available.add(hall);
        }
        available.sort(java.util.Comparator.comparing(Hall::getHallName));
        return available;
    }
}