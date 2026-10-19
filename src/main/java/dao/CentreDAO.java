package dao;

import model.Centre;
import model.Hall;
import utils.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Xử lý truy vấn và cập nhật dữ liệu Cụm rạp (Centres) và Phòng chiếu (Halls).
 * Phục vụ UC-FE-03.1 (Manage Cinema Centres) và quy tắc BR-HS-01.
 */
public class CentreDAO {

    /**
     * Danh sách rạp đang hoạt động cho bước chọn rạp khi đặt vé (giữ nguyên để tương thích).
     */
    public List<Centre> getActiveCentres() throws SQLException {
        String sql = "SELECT CentreID, CentreName, Address, City FROM Centres "
                   + "WHERE Status = 'ACTIVE' ORDER BY City, CentreName";
        List<Centre> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Centre centre = new Centre();
                centre.setCentreId(rs.getInt("CentreID"));
                centre.setCentreName(rs.getString("CentreName"));
                centre.setAddress(rs.getString("Address"));
                centre.setCity(rs.getString("City"));
                list.add(centre);
            }
        }
        return list;
    }

    /**
     * Lấy danh sách tất cả Cụm rạp phục vụ trang Quản lý Cụm rạp (Admin).
     * Có hỗ trợ tìm kiếm theo từ khóa và lọc theo trạng thái.
     * Đồng thời tính toán số lượng Hall (BR-HS-01) và tổng số ghế của mỗi cụm rạp.
     *
     * @param search từ khóa tìm kiếm (tên rạp, địa chỉ, hotline)
     * @param status bộ lọc trạng thái ('ALL', 'ACTIVE', 'INACTIVE')
     * @return danh sách các Centre
     */
    public List<Centre> getAllCentres(String search, String status) {
        List<Centre> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT c.CentreID, c.CentreName, c.Address, c.City, c.Phone, c.Status, "
            + "COUNT(DISTINCT h.HallID) AS HallCount, "
            + "COALESCE(SUM(h.Capacity), 0) AS SeatCount "
            + "FROM Centres c "
            + "LEFT JOIN Halls h ON h.CentreID = c.CentreID "
            + "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        // Lọc theo từ khóa tìm kiếm
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(c.CentreName) LIKE ? OR LOWER(c.Address) LIKE ? OR c.Phone LIKE ? OR LOWER(c.City) LIKE ?) ");
            String keyword = "%" + search.trim().toLowerCase() + "%";
            params.add(keyword);
            params.add(keyword);
            params.add(keyword);
            params.add(keyword);
        }

        // Lọc theo trạng thái hoạt động
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            sql.append("AND UPPER(c.Status) = ? ");
            params.add(status.trim().toUpperCase());
        }

        sql.append("GROUP BY c.CentreID, c.CentreName, c.Address, c.City, c.Phone, c.Status ");
        sql.append("ORDER BY c.CentreID ASC");

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Centre centre = new Centre();
                    centre.setCentreId(rs.getInt("CentreID"));
                    centre.setCentreName(rs.getString("CentreName"));
                    centre.setAddress(rs.getString("Address"));
                    centre.setCity(rs.getString("City"));
                    centre.setPhone(rs.getString("Phone"));
                    centre.setStatus(rs.getString("Status"));
                    centre.setHallCount(rs.getInt("HallCount"));
                    centre.setSeatCount(rs.getInt("SeatCount"));
                    list.add(centre);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    /**
     * Lấy thông tin một Cụm rạp theo ID.
     *
     * @param centreId mã định danh rạp
     * @return đối tượng Centre hoặc null nếu không tìm thấy
     */
    public Centre getCentreById(int centreId) {
        String sql = "SELECT c.CentreID, c.CentreName, c.Address, c.City, c.Phone, c.Status, "
                   + "COUNT(DISTINCT h.HallID) AS HallCount, "
                   + "COALESCE(SUM(h.Capacity), 0) AS SeatCount "
                   + "FROM Centres c "
                   + "LEFT JOIN Halls h ON h.CentreID = c.CentreID "
                   + "WHERE c.CentreID = ? "
                   + "GROUP BY c.CentreID, c.CentreName, c.Address, c.City, c.Phone, c.Status";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, centreId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Centre centre = new Centre();
                    centre.setCentreId(rs.getInt("CentreID"));
                    centre.setCentreName(rs.getString("CentreName"));
                    centre.setAddress(rs.getString("Address"));
                    centre.setCity(rs.getString("City"));
                    centre.setPhone(rs.getString("Phone"));
                    centre.setStatus(rs.getString("Status"));
                    centre.setHallCount(rs.getInt("HallCount"));
                    centre.setSeatCount(rs.getInt("SeatCount"));
                    return centre;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * BR-HS-01: Lấy danh sách các Hall trực thuộc một Centre.
     *
     * @param centreId mã định danh cụm rạp
     * @return danh sách các phòng chiếu Hall
     */
    public List<Hall> getHallsByCentreId(int centreId) {
        List<Hall> halls = new ArrayList<>();
        String sql = "SELECT HallID, CentreID, HallName, HallType, Capacity, Status, CreatedAt "
                   + "FROM Halls WHERE CentreID = ? ORDER BY HallName ASC";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, centreId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Hall h = new Hall();
                    h.setHallId(rs.getInt("HallID"));
                    h.setCentreId(rs.getInt("CentreID"));
                    h.setHallName(rs.getString("HallName"));
                    h.setHallType(rs.getString("HallType"));
                    h.setCapacity(rs.getInt("Capacity"));
                    h.setStatus(rs.getString("Status"));
                    h.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    halls.add(h);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return halls;
    }

    /**
     * Kiểm tra xem tên cụm rạp đã tồn tại trong Database hay chưa.
     * Phục vụ tác vụ Thêm rạp và Sửa rạp (kiểm tra không trùng tên).
     *
     * @param centreName tên cụm rạp cần kiểm tra
     * @param excludeCentreId ID cụm rạp cần bỏ qua khi kiểm tra (truyền 0 nếu thêm mới)
     * @return true nếu đã tồn tại tên rạp, ngược lại false
     */
    public boolean isCentreNameExists(String centreName, int excludeCentreId) {
        if (centreName == null || centreName.trim().isEmpty()) {
            return false;
        }

        String sql = "SELECT COUNT(*) FROM Centres WHERE LOWER(LTRIM(RTRIM(CentreName))) = LOWER(LTRIM(RTRIM(?))) AND CentreID <> ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, centreName.trim());
            ps.setInt(2, excludeCentreId);

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
     * Thêm mới một Cụm rạp vào Database.
     * Trạng thái mặc định khi thêm mới là 'ACTIVE'.
     *
     * @param centre đối tượng Centre cần thêm
     * @return true nếu thêm thành công, ngược lại false
     */
    public boolean addCentre(Centre centre) {
        String sql = "INSERT INTO Centres (CentreName, Address, City, Phone, Status, CreatedAt) "
                   + "VALUES (?, ?, ?, ?, 'ACTIVE', GETDATE())";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, centre.getCentreName().trim());
            ps.setString(2, centre.getAddress().trim());
            ps.setString(3, centre.getCity() != null ? centre.getCity().trim() : "");
            ps.setString(4, centre.getPhone() != null ? centre.getPhone().trim() : "");

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Cập nhật thông tin Cụm rạp (Tên rạp, Địa chỉ, Hotline, Thành phố).
     *
     * @param centre đối tượng Centre chứa thông tin cập nhật
     * @return true nếu cập nhật thành công, ngược lại false
     */
    public boolean updateCentre(Centre centre) {
        String sql = "UPDATE Centres SET CentreName = ?, Address = ?, City = ?, Phone = ?, UpdatedAt = GETDATE() "
                   + "WHERE CentreID = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, centre.getCentreName().trim());
            ps.setString(2, centre.getAddress().trim());
            ps.setString(3, centre.getCity() != null ? centre.getCity().trim() : "");
            ps.setString(4, centre.getPhone() != null ? centre.getPhone().trim() : "");
            ps.setInt(5, centre.getCentreId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Toggle Status: Bật/tắt trạng thái (Active / Inactive) của Centre.
     * Quy tắc nghiệp vụ: Nếu Inactive, các Hall và Suất chiếu của Centre này sẽ tự động
     * tạm ngưng hoạt động trên luồng đặt vé (vì luồng đặt vé ShowtimeDAO ràng buộc b.Status = 'ACTIVE').
     *
     * @param centreId mã định danh rạp cần bật/tắt trạng thái
     * @return true nếu đổi trạng thái thành công, ngược lại false
     */
    public boolean toggleStatus(int centreId) {
        String sql = "UPDATE Centres "
                   + "SET Status = CASE WHEN UPPER(Status) = 'ACTIVE' THEN 'INACTIVE' ELSE 'ACTIVE' END, "
                   + "    UpdatedAt = GETDATE() "
                   + "WHERE CentreID = ?";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, centreId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Đếm số lượng rạp theo trạng thái phục vụ hiển thị thống kê.
     *
     * @param status 'ALL', 'ACTIVE', hoặc 'INACTIVE'
     * @return số lượng rạp
     */
    public int countCentresByStatus(String status) {
        String sql;
        if ("ALL".equalsIgnoreCase(status) || status == null) {
            sql = "SELECT COUNT(*) FROM Centres";
        } else {
            sql = "SELECT COUNT(*) FROM Centres WHERE UPPER(Status) = ?";
        }

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (!"ALL".equalsIgnoreCase(status) && status != null) {
                ps.setString(1, status.trim().toUpperCase());
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
     * Đếm tổng số phòng chiếu (Hall) trong toàn bộ hệ thống.
     *
     * @return tổng số phòng chiếu
     */
    public int countTotalHalls() {
        String sql = "SELECT COUNT(*) FROM Halls";

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }
}
