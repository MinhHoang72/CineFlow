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
     * Đếm tổng số phòng chiếu của Cụm rạp thỏa mãn điều kiện tìm kiếm và bộ lọc.
     * Dùng để tính toán số trang cho phân trang (Pagination).
     *
     * @param centreId mã định danh cụm rạp
     * @param search từ khóa tìm kiếm theo tên phòng
     * @param status bộ lọc trạng thái ('ALL', 'ACTIVE', 'INACTIVE', '2D', '3D')
     * @return tổng số phòng chiếu tìm thấy
     */
    public int countHallsByCentre(int centreId, String search, String status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM Halls h WHERE h.CentreID = ? ");
        List<Object> params = new ArrayList<>();
        params.add(centreId);

        // Lọc theo từ khóa tìm kiếm tên phòng
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND LOWER(h.HallName) LIKE ? ");
            params.add("%" + search.trim().toLowerCase() + "%");
        }

        // Lọc theo trạng thái hoặc định dạng kỹ thuật
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            if ("ACTIVE".equalsIgnoreCase(status)) {
                sql.append("AND UPPER(h.Status) = 'AVAILABLE' ");
            } else if ("INACTIVE".equalsIgnoreCase(status)) {
                sql.append("AND UPPER(h.Status) IN ('INACTIVE', 'MAINTENANCE') ");
            } else if ("2D".equalsIgnoreCase(status) || "3D".equalsIgnoreCase(status)) {
                sql.append("AND UPPER(h.HallType) = ? ");
                params.add(status.trim().toUpperCase());
            }
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
     * Lấy danh sách các phòng chiếu trực thuộc Cụm rạp có phân trang (Pagination).
     * Sử dụng lệnh OFFSET ... ROWS FETCH NEXT ... ROWS ONLY chuẩn của SQL Server.
     *
     * @param centreId mã định danh cụm rạp
     * @param search từ khóa tìm kiếm theo tên phòng
     * @param status bộ lọc trạng thái ('ALL', 'ACTIVE', 'INACTIVE', '2D', '3D')
     * @param page số trang hiện tại (bắt đầu từ 1)
     * @param pageSize số bản ghi hiển thị trên mỗi trang
     * @return danh sách các phòng chiếu trên trang hiện tại
     */
    public List<Hall> getHallsByCentrePaged(int centreId, String search, String status, int page, int pageSize) {
        if (page < 1) {
            page = 1;
        }
        if (pageSize < 1) {
            pageSize = 6;
        }
        int offset = (page - 1) * pageSize;

        List<Hall> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT h.HallID, h.CentreID, h.HallName, h.HallType, h.Capacity, h.Status, h.CreatedAt, c.CentreName "
            + "FROM Halls h "
            + "JOIN Centres c ON c.CentreID = h.CentreID "
            + "WHERE h.CentreID = ? "
        );

        List<Object> params = new ArrayList<>();
        params.add(centreId);

        // Lọc theo từ khóa tìm kiếm tên phòng
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND LOWER(h.HallName) LIKE ? ");
            params.add("%" + search.trim().toLowerCase() + "%");
        }

        // Lọc theo trạng thái hoặc định dạng kỹ thuật
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            if ("ACTIVE".equalsIgnoreCase(status)) {
                sql.append("AND UPPER(h.Status) = 'AVAILABLE' ");
            } else if ("INACTIVE".equalsIgnoreCase(status)) {
                sql.append("AND UPPER(h.Status) IN ('INACTIVE', 'MAINTENANCE') ");
            } else if ("2D".equalsIgnoreCase(status) || "3D".equalsIgnoreCase(status)) {
                sql.append("AND UPPER(h.HallType) = ? ");
                params.add(status.trim().toUpperCase());
            }
        }

        sql.append("ORDER BY h.HallID ASC ");
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