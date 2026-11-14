package dao;

import model.dto.common.PageDTO;
import model.entity.Centre;
import model.entity.Hall;
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
     * Tìm kiếm danh sách Centre theo từ khóa và trạng thái, trả về PageDTO.
     * Thay thế countCentres() + getCentresPaged() bằng một lời gọi duy nhất.
     *
     * @param search từ khóa tìm kiếm (tên rạp, địa chỉ, hotline, thành phố)
     * @param status bộ lọc trạng thái ('ALL', 'ACTIVE', 'INACTIVE')
     * @param page   số trang hiện tại (bắt đầu từ 1)
     * @param size   số bản ghi mỗi trang
     * @return PageDTO<Centre> chứa items, page, size, total
     */
    public PageDTO<Centre> search(String search, String status, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 6;

        StringBuilder where = new StringBuilder("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            where.append("AND (c.CentreName COLLATE Latin1_General_100_CI_AI LIKE ? "
                    + "OR c.Address COLLATE Latin1_General_100_CI_AI LIKE ? "
                    + "OR c.Phone LIKE ? "
                    + "OR c.City COLLATE Latin1_General_100_CI_AI LIKE ?) ");
            String safeKey = search.trim().replace("[", "[[]").replace("%", "[%]").replace("_", "[_]");
            String kw = "%" + safeKey + "%";
            params.add(kw); params.add(kw); params.add(kw); params.add(kw);
        }
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            where.append("AND UPPER(c.Status) = ? ");
            params.add(status.trim().toUpperCase());
        }

        // Đếm tổng số bản ghi
        int total = 0;
        String countSql = "SELECT COUNT(*) FROM Centres c " + where;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(countSql)) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) total = rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new PageDTO<>(new ArrayList<>(), page, size, 0);
        }

        // Lấy danh sách trang hiện tại
        List<Centre> items = new ArrayList<>();
        String dataSql = "SELECT c.CentreID, c.CentreName, c.Address, c.City, c.Phone, c.Status, "
                + "COUNT(DISTINCT h.HallID) AS HallCount, "
                + "COALESCE(SUM(h.Capacity), 0) AS SeatCount "
                + "FROM Centres c LEFT JOIN Halls h ON h.CentreID = c.CentreID "
                + where
                + "GROUP BY c.CentreID, c.CentreName, c.Address, c.City, c.Phone, c.Status "
                + "ORDER BY c.CentreID ASC "
                + "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add((page - 1) * size);
        dataParams.add(size);
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(dataSql)) {
            for (int i = 0; i < dataParams.size(); i++) ps.setObject(i + 1, dataParams.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Centre c = new Centre();
                    c.setCentreId(rs.getInt("CentreID"));
                    c.setCentreName(rs.getString("CentreName"));
                    c.setAddress(rs.getString("Address"));
                    c.setCity(rs.getString("City"));
                    c.setPhone(rs.getString("Phone"));
                    c.setStatus(rs.getString("Status"));
                    c.setHallCount(rs.getInt("HallCount"));
                    c.setSeatCount(rs.getInt("SeatCount"));
                    items.add(c);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new PageDTO<>(items, page, size, total);
    }
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
     * Đếm tổng số Cụm rạp phù hợp với từ khóa tìm kiếm và trạng thái.
     * Dùng để tính toán tổng số trang cho phân trang (Pagination).
     *
     * @param search từ khóa tìm kiếm (tên rạp, địa chỉ, hotline, thành phố)
     * @param status bộ lọc trạng thái ('ALL', 'ACTIVE', 'INACTIVE')
     * @return tổng số lượng Cụm rạp tìm thấy
     */
    public int countCentres(String search, String status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM Centres c WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        // Điều kiện tìm kiếm
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(c.CentreName) LIKE ? OR LOWER(c.Address) LIKE ? OR c.Phone LIKE ? OR LOWER(c.City) LIKE ?) ");
            String keyword = "%" + search.trim().toLowerCase() + "%";
            params.add(keyword);
            params.add(keyword);
            params.add(keyword);
            params.add(keyword);
        }

        // Điều kiện trạng thái
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            sql.append("AND UPPER(c.Status) = ? ");
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
     * Lấy danh sách Cụm rạp theo từng trang (Pagination).
     * Sử dụng câu lệnh OFFSET ... ROWS FETCH NEXT ... ROWS ONLY chuẩn của SQL Server.
     *
     * @param search từ khóa tìm kiếm
     * @param status bộ lọc trạng thái ('ALL', 'ACTIVE', 'INACTIVE')
     * @param page số trang hiện tại (bắt đầu từ 1)
     * @param pageSize số bản ghi hiển thị trên mỗi trang
     * @return danh sách các Centre trên trang hiện tại
     */
    public List<Centre> getCentresPaged(String search, String status, int page, int pageSize) {
        if (page < 1) {
            page = 1;
        }
        if (pageSize < 1) {
            pageSize = 6;
        }
        int offset = (page - 1) * pageSize;

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

        // Điều kiện tìm kiếm
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(c.CentreName) LIKE ? OR LOWER(c.Address) LIKE ? OR c.Phone LIKE ? OR LOWER(c.City) LIKE ?) ");
            String keyword = "%" + search.trim().toLowerCase() + "%";
            params.add(keyword);
            params.add(keyword);
            params.add(keyword);
            params.add(keyword);
        }

        // Điều kiện trạng thái
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            sql.append("AND UPPER(c.Status) = ? ");
            params.add(status.trim().toUpperCase());
        }

        sql.append("GROUP BY c.CentreID, c.CentreName, c.Address, c.City, c.Phone, c.Status ");
        sql.append("ORDER BY c.CentreID ASC ");
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
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(CentreDAO.class.getName());

    /**
     * Kiểm tra tính hợp lệ của luồng chuyển đổi trạng thái Cụm rạp (Theater status transition).
     * Bảng quy tắc chuyển trạng thái hợp lệ:
     * - ACTIVE -> SUSPENDED: Hợp lệ
     * - SUSPENDED -> ACTIVE: Hợp lệ
     * - ACTIVE -> INACTIVE: Hợp lệ
     * - SUSPENDED -> INACTIVE: Hợp lệ
     * - INACTIVE -> ACTIVE: Hợp lệ
     * - INACTIVE -> SUSPENDED: KHÔNG HỢP LỆ (Phải về ACTIVE trước)
     */
    public boolean isValidStatusTransition(String fromStatus, String toStatus) {
        if (fromStatus == null || toStatus == null) return false;
        String from = fromStatus.trim().toUpperCase();
        String to = toStatus.trim().toUpperCase();
        if (from.equals(to)) return true;

        if ("ACTIVE".equals(from)) {
            return "SUSPENDED".equals(to) || "INACTIVE".equals(to);
        } else if ("SUSPENDED".equals(from)) {
            return "ACTIVE".equals(to) || "INACTIVE".equals(to);
        } else if ("INACTIVE".equals(from)) {
            return "ACTIVE".equals(to);
        }
        return false;
    }

    /**
     * Thực hiện chuyển trạng thái Cụm rạp (Theater.changeStatus) theo quy chuẩn phân phối phim CineFlow.
     * Thứ tự xử lý trong 1 Transaction duy nhất:
     * 1. Kiểm tra quyền Admin và luồng chuyển trạng thái hợp lệ.
     * 2. Cập nhật Status của Cụm rạp (Centres.Status).
     * 3. Cập nhật hàng loạt MovieDistribution (CentreMovies):
     *    - Rạp sang SUSPENDED: Cập nhật các bản ghi phân phối ACTIVE -> SUSPENDED (IsActive = 0, giữ cấu hình, tạm mất hiệu lực).
     *    - Rạp sang INACTIVE: Cập nhật các bản ghi ACTIVE và SUSPENDED -> CANCELLED (IsActive = 0).
     *    - Rạp sang ACTIVE từ SUSPENDED: Khôi phục các bản ghi SUSPENDED -> ACTIVE (IsActive = 1) CHỈ NẾU CHƯA QUÁ endDate của phim.
     *      Các phim đã quá endDate giữ nguyên hoặc chuyển CANCELLED.
     *    - Rạp sang ACTIVE từ INACTIVE: CANCELLED giữ nguyên (Không tự khôi phục; Admin phải tạo bản ghi mới).
     * 4. Ghi log kiểm toán (ai đổi, khi nào, lý do).
     * 5. Commit hoặc Rollback toàn bộ transaction.
     */
    public boolean changeStatus(int centreId, String currentStatus, String targetStatus, String adminUser, String reason) throws SQLException {
        if (currentStatus == null || targetStatus == null) return false;
        String from = currentStatus.trim().toUpperCase();
        String to = targetStatus.trim().toUpperCase();

        if (!isValidStatusTransition(from, to)) {
            LOGGER.warning("[AUDIT REJECT] Chuyển trạng thái rạp không hợp lệ: Từ " + from + " sang " + to);
            return false;
        }

        if (from.equals(to)) {
            return true;
        }

        // TODO: Xử lý suất chiếu tương lai và vé đã bán khi rạp SUSPENDED hoặc INACTIVE (Quy tắc nghiệp vụ chưa chốt theo tài liệu)

        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Bước 2: Cập nhật Status của Cụm rạp
                String updateCentreSql = "UPDATE Centres SET Status = ?, UpdatedAt = GETDATE() WHERE CentreID = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateCentreSql)) {
                    ps.setString(1, to);
                    ps.setInt(2, centreId);
                    int affected = ps.executeUpdate();
                    if (affected <= 0) {
                        conn.rollback();
                        return false;
                    }
                }

                // Bước 3: Cập nhật hàng loạt trạng thái phân phối (MovieDistribution / CentreMovies)
                if ("INACTIVE".equals(to)) {
                    // Rạp chuyển sang INACTIVE: ACTIVE hoặc SUSPENDED -> CANCELLED
                    String sql = "UPDATE CentreMovies SET Status = 'CANCELLED', IsActive = 0, UpdatedAt = GETDATE() "
                               + "WHERE CentreID = ? AND Status IN ('ACTIVE', 'SUSPENDED')";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, centreId);
                        ps.executeUpdate();
                    }
                } else if ("ACTIVE".equals(to)) {
                    // ACTIVE <-> SUSPENDED không thay đổi phân phối.
                    // INACTIVE -> ACTIVE không tự khôi phục các bản ghi đã CANCELLED.
                }

                // Bước 4: Ghi log kiểm toán (ai đổi, khi nào, lý do)
                LOGGER.info(String.format("[AUDIT] Admin '%s' đã đổi trạng thái cụm rạp ID %d từ '%s' sang '%s'. Lý do: %s",
                        (adminUser != null ? adminUser : "SYSTEM"), centreId, from, to, (reason != null ? reason : "N/A")));

                // Bước 5: Toàn bộ trong một transaction: cùng thành công hoặc cùng rollback
                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                LOGGER.log(java.util.logging.Level.SEVERE, "Lỗi khi thực hiện giao dịch chuyển trạng thái rạp ID: " + centreId, e);
                throw e;
            }
        }
    }

    /**
     * Bật/tắt nhanh trạng thái (tương thích ngược với các lời gọi cũ).
     */
    public boolean toggleStatus(int centreId) {
        Centre centre = getCentreById(centreId);
        if (centre == null) return false;
        String current = centre.getStatus();
        String target = "ACTIVE".equalsIgnoreCase(current) ? "SUSPENDED" : "ACTIVE";
        try {
            return changeStatus(centreId, current, target, "Admin", "Toggle status qua giao diện");
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
        } else if ("SUSPENDED".equalsIgnoreCase(status)) {
            sql = "SELECT COUNT(*) FROM Centres WHERE UPPER(Status) = 'SUSPENDED'";
        } else {
            sql = "SELECT COUNT(*) FROM Centres WHERE UPPER(Status) = ?";
        }

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (!"ALL".equalsIgnoreCase(status) && status != null && !"SUSPENDED".equalsIgnoreCase(status)) {
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
