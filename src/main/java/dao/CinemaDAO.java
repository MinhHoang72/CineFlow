package dao;

import model.entity.Centre;
import model.dto.showtime.ShowtimeDetailDTO;
import utils.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object: Xử lý toàn bộ các truy vấn liên quan đến Cụm rạp chiếu (Centres),
 * phân công nhân sự quản lý cụm rạp, và lịch chiếu / giá vé theo từng rạp.
 *
 * Lưu ý kiến trúc: Các nghiệp vụ liên quan đến Phim (Movies) và Phân phối phim (CentreMovies)
 * đã được quy hoạch độc lập sang MovieDAO.
 */
public class CinemaDAO {

    // =========================================================================
    // 1. QUẢN LÝ THÔNG TIN CỤM RẠP CHIẾU (Centres)
    // =========================================================================

    /**
     * Lấy danh sách tất cả các trung tâm / cụm rạp chiếu phim đang hoạt động.
     *
     * @return danh sách trung tâm đang hoạt động
     */
    public List<Centre> getActiveCentres() {
        String sql = "SELECT b.CentreID, b.CentreName, b.Address, b.City, b.Phone, "
                + "COUNT(DISTINCT h.HallID) AS HallCount, "
                + "COALESCE(SUM(h.Capacity), 0) AS SeatCount "
                + "FROM Centres b "
                + "LEFT JOIN Halls h ON h.CentreID = b.CentreID "
                + "WHERE b.Status = 'ACTIVE' "
                + "GROUP BY b.CentreID, b.CentreName, b.Address, b.City, b.Phone "
                + "ORDER BY b.City, b.CentreName";
        List<Centre> centres = new ArrayList<>();

        try (Connection connection = DBContext.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Centre centre = new Centre();
                centre.setCentreId(resultSet.getInt("CentreID"));
                centre.setCentreName(resultSet.getString("CentreName"));
                centre.setAddress(resultSet.getString("Address"));
                centre.setCity(resultSet.getString("City"));
                centre.setPhone(resultSet.getString("Phone"));
                centre.setStatus("ACTIVE");
                centre.setHallCount(resultSet.getInt("HallCount"));
                centre.setSeatCount(resultSet.getInt("SeatCount"));
                centres.add(centre);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return centres;
    }

    /**
     * Lấy danh sách các tỉnh/thành phố có cụm rạp hoạt động kèm theo số lượng rạp tương ứng.
     *
     * @return bản đồ tên thành phố và số lượng rạp tương ứng
     */
    public Map<String, Integer> getActiveCitiesWithCount() {
        String sql = "SELECT City, COUNT(CentreID) AS CinemaCount "
                + "FROM Centres WHERE Status = 'ACTIVE' AND City IS NOT NULL AND LTRIM(RTRIM(City)) <> '' "
                + "GROUP BY City ORDER BY City";
        Map<String, Integer> cities = new LinkedHashMap<>();
        try (Connection connection = DBContext.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String city = resultSet.getString("City");
                if (city != null && !city.trim().isEmpty()) {
                    cities.put(city.trim(), resultSet.getInt("CinemaCount"));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return cities;
    }

    /**
     * Lấy danh sách các cụm rạp đang hoạt động lọc theo tỉnh/thành phố cụ thể.
     *
     * @param city tên thành phố cần lọc
     * @return danh sách các trung tâm chiếu phim tại thành phố
     */
    public List<Centre> getActiveCentresByCity(String city) {
        String sql = "SELECT b.CentreID, b.CentreName, b.Address, b.City, b.Phone, "
                + "COUNT(DISTINCT h.HallID) AS HallCount, "
                + "COALESCE(SUM(h.Capacity), 0) AS SeatCount "
                + "FROM Centres b "
                + "LEFT JOIN Halls h ON h.CentreID = b.CentreID "
                + "WHERE b.Status = 'ACTIVE' AND LTRIM(RTRIM(b.City)) = LTRIM(RTRIM(?)) "
                + "GROUP BY b.CentreID, b.CentreName, b.Address, b.City, b.Phone "
                + "ORDER BY b.CentreName";
        List<Centre> centres = new ArrayList<>();
        try (Connection connection = DBContext.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, city != null ? city.trim() : "");
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Centre centre = new Centre();
                    centre.setCentreId(resultSet.getInt("CentreID"));
                    centre.setCentreName(resultSet.getString("CentreName"));
                    centre.setAddress(resultSet.getString("Address"));
                    centre.setCity(resultSet.getString("City"));
                    centre.setPhone(resultSet.getString("Phone"));
                    centre.setStatus("ACTIVE");
                    centre.setHallCount(resultSet.getInt("HallCount"));
                    centre.setSeatCount(resultSet.getInt("SeatCount"));
                    centres.add(centre);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return centres;
    }

    /**
     * Lấy thông tin chi tiết một trung tâm theo mã ID, bao gồm thông tin Quản lý hiện tại phụ trách.
     *
     * @param centreId mã định danh trung tâm
     * @return đối tượng Centre nếu tìm thấy, ngược lại null
     */
    public Centre getCentreById(int centreId) {
        String sql = "SELECT b.CentreID, b.CentreName, b.Address, b.City, b.Phone, b.Status, "
                + "COUNT(DISTINCT h.HallID) AS HallCount, "
                + "COALESCE(SUM(h.Capacity), 0) AS SeatCount, "
                + "cma.ManagerID, cma.StartDate AS AssignmentStartDate, u.FullName AS ManagerName, u.Email AS ManagerEmail, u.Phone AS ManagerPhone, u.Status AS ManagerStatus "
                + "FROM Centres b "
                + "LEFT JOIN Halls h ON h.CentreID = b.CentreID "
                + "LEFT JOIN CentreManagerAssignments cma ON cma.CentreID = b.CentreID AND cma.Status = 'ACTIVE' AND cma.EndDate IS NULL "
                + "LEFT JOIN Users u ON u.UserID = cma.ManagerID "
                + "WHERE b.CentreID = ? "
                + "GROUP BY b.CentreID, b.CentreName, b.Address, b.City, b.Phone, b.Status, cma.ManagerID, cma.StartDate, u.FullName, u.Email, u.Phone, u.Status";

        try (Connection connection = DBContext.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, centreId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Centre centre = new Centre();
                    centre.setCentreId(resultSet.getInt("CentreID"));
                    centre.setCentreName(resultSet.getString("CentreName"));
                    centre.setAddress(resultSet.getString("Address"));
                    centre.setCity(resultSet.getString("City"));
                    centre.setPhone(resultSet.getString("Phone"));
                    centre.setStatus(resultSet.getString("Status"));
                    centre.setHallCount(resultSet.getInt("HallCount"));
                    centre.setSeatCount(resultSet.getInt("SeatCount"));
                    int mgrId = resultSet.getInt("ManagerID");
                    if (!resultSet.wasNull()) {
                        centre.setManagerId(mgrId);
                        centre.setManagerName(resultSet.getString("ManagerName"));
                        centre.setManagerEmail(resultSet.getString("ManagerEmail"));
                        centre.setManagerStatus(resultSet.getString("ManagerStatus"));
                        centre.setManagerPhone(resultSet.getString("ManagerPhone"));
                        centre.setAssignmentStartDate(resultSet.getTimestamp("AssignmentStartDate"));
                    }
                    return centre;
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return null;
    }

    // =========================================================================
    // 2. QUẢN LÝ BỔ NHIỆM QUẢN LÝ CỤM RẠP (Centre Manager Assignments)
    // =========================================================================

    /**
     * Lấy toàn bộ danh sách trung tâm cùng thông tin Quản lý phụ trách tương ứng.
     *
     * @return danh sách các trung tâm kèm thông tin Quản lý
     */
    public List<Centre> getAllCentresWithManager() {
        String sql = "SELECT b.CentreID, b.CentreName, b.Address, b.City, b.Phone, b.Status, "
                + "COUNT(DISTINCT h.HallID) AS HallCount, "
                + "COALESCE(SUM(h.Capacity), 0) AS SeatCount, "
                + "cma.ManagerID, cma.StartDate AS AssignmentStartDate, u.FullName AS ManagerName, u.Email AS ManagerEmail, u.Phone AS ManagerPhone, u.Status AS ManagerStatus "
                + "FROM Centres b "
                + "LEFT JOIN Halls h ON h.CentreID = b.CentreID "
                + "LEFT JOIN CentreManagerAssignments cma ON cma.CentreID = b.CentreID AND cma.Status = 'ACTIVE' AND cma.EndDate IS NULL "
                + "LEFT JOIN Users u ON u.UserID = cma.ManagerID "
                + "GROUP BY b.CentreID, b.CentreName, b.Address, b.City, b.Phone, b.Status, cma.ManagerID, cma.StartDate, u.FullName, u.Email, u.Phone, u.Status "
                + "ORDER BY b.CentreID DESC";
        List<Centre> list = new ArrayList<>();
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
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

                int mgrId = rs.getInt("ManagerID");
                if (!rs.wasNull()) {
                    centre.setManagerId(mgrId);
                    centre.setManagerName(rs.getString("ManagerName"));
                    centre.setManagerEmail(rs.getString("ManagerEmail"));
                    centre.setManagerStatus(rs.getString("ManagerStatus"));
                    centre.setManagerPhone(rs.getString("ManagerPhone"));
                    centre.setAssignmentStartDate(rs.getTimestamp("AssignmentStartDate"));
                }
                list.add(centre);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Lấy danh sách các cụm rạp đang hoạt động nhưng chưa có Quản lý phụ trách.
     */
    public List<Centre> getCentresWithoutActiveManager() {
        String sql = "SELECT b.CentreID, b.CentreName, b.Address, b.City, b.Phone, b.Status "
                + "FROM Centres b "
                + "WHERE b.Status = 'ACTIVE' "
                + "AND NOT EXISTS ("
                + "    SELECT 1 FROM CentreManagerAssignments cma "
                + "    JOIN Users u ON u.UserID = cma.ManagerID "
                + "    WHERE cma.CentreID = b.CentreID AND cma.Status = 'ACTIVE' AND cma.EndDate IS NULL AND UPPER(u.Status) = 'ACTIVE'"
                + ") "
                + "ORDER BY b.City, b.CentreName";
        List<Centre> list = new ArrayList<>();
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Centre centre = new Centre();
                centre.setCentreId(rs.getInt("CentreID"));
                centre.setCentreName(rs.getString("CentreName"));
                centre.setAddress(rs.getString("Address"));
                centre.setCity(rs.getString("City"));
                centre.setPhone(rs.getString("Phone"));
                centre.setStatus(rs.getString("Status"));
                list.add(centre);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Kiểm tra xem một cụm rạp đã có Quản lý đang hoạt động hay chưa.
     */
    public boolean hasActiveManager(int centreId) {
        String sql = "SELECT 1 FROM CentreManagerAssignments cma "
                + "JOIN Users u ON u.UserID = cma.ManagerID "
                + "WHERE cma.CentreID = ? AND cma.Status = 'ACTIVE' AND cma.EndDate IS NULL AND UPPER(u.Status) = 'ACTIVE'";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, centreId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return true;
        }
    }

    // =========================================================================
    // 3. QUẢN LÝ SUẤT CHIẾU VÀ GIÁ VÉ TỰ ĐỘNG THEO RẠP (Showtimes & Dynamic Pricing)
    // =========================================================================

    private static final String DYNAMIC_PRICE_SELECT = "(COALESCE(bp.BasePrice, 80000) + COALESCE(tsp.Surcharge, 0) + COALESCE(sur.Amount, 0)) AS BaseTicketPrice ";
    private static final String DYNAMIC_PRICE_APPLY = "OUTER APPLY ( "
            + "    SELECT TOP 1 bp.BasePrice FROM CentrePricing bp "
            + "    WHERE bp.CentreID = b.CentreID AND s.StartTime >= bp.EffectiveFrom "
            + "      AND (bp.EffectiveTo IS NULL OR s.StartTime <= bp.EffectiveTo) "
            + "    ORDER BY bp.EffectiveFrom DESC "
            + ") bp "
            + "OUTER APPLY ( "
            + "    SELECT TOP 1 tsp.Surcharge FROM TimeSlotPricing tsp "
            + "    WHERE tsp.CentreID = b.CentreID "
            + "      AND CAST(s.StartTime AS TIME) >= tsp.StartTime "
            + "      AND CAST(s.StartTime AS TIME) < tsp.EndTime "
            + "      AND s.StartTime >= tsp.EffectiveFrom "
            + "      AND (tsp.EffectiveTo IS NULL OR s.StartTime <= tsp.EffectiveTo) "
            + "    ORDER BY tsp.EffectiveFrom DESC "
            + ") tsp "
            + "OUTER APPLY ( "
            + "    SELECT TOP 1 sur.Amount FROM Surcharges sur "
            + "    WHERE sur.SurchargeType = 'HALL' AND sur.TargetValue = h.HallType "
            + "      AND s.StartTime >= sur.EffectiveFrom "
            + "      AND (sur.EffectiveTo IS NULL OR s.StartTime <= sur.EffectiveTo) "
            + "    ORDER BY sur.EffectiveFrom DESC "
            + ") sur ";

    /**
     * Lấy tất cả các suất chiếu đã lên lịch của những phim đang chiếu hoặc sắp chiếu.
     *
     * @return danh sách các suất chiếu đã lên lịch
     */
    public List<ShowtimeDetailDTO> getScheduledShowtimes() {
        String sql = "SELECT s.ShowtimeID, m.MovieID, b.CentreID, m.Title, "
                + "b.CentreName, b.City, h.HallName, s.StartTime, "
                + DYNAMIC_PRICE_SELECT
                + "FROM Showtimes s "
                + "JOIN Movies m ON m.MovieID = s.MovieID "
                + "JOIN Halls h ON h.HallID = s.HallID "
                + "JOIN Centres b ON b.CentreID = h.CentreID "
                + DYNAMIC_PRICE_APPLY
                + "WHERE s.Status = 'SCHEDULED' AND s.StartTime > SYSDATETIME() AND m.Status IN ('NOW_SHOWING', 'COMING_SOON') "
                + "ORDER BY s.StartTime";
        List<ShowtimeDetailDTO> showtimes = new ArrayList<>();

        try (Connection connection = DBContext.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                ShowtimeDetailDTO showtime = new ShowtimeDetailDTO();
                showtime.setShowtimeId(resultSet.getInt("ShowtimeID"));
                showtime.setMovieId(resultSet.getInt("MovieID"));
                showtime.setCentreId(resultSet.getInt("CentreID"));
                showtime.setMovieTitle(resultSet.getString("Title"));
                showtime.setCentreName(resultSet.getString("CentreName"));
                showtime.setCity(resultSet.getString("City"));
                showtime.setHallName(resultSet.getString("HallName"));
                showtime.setStartTime(resultSet.getTimestamp("StartTime"));
                showtime.setBaseTicketPrice(resultSet.getBigDecimal("BaseTicketPrice"));
                showtimes.add(showtime);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return showtimes;
    }

    /**
     * Lấy các suất chiếu đã lên lịch cho một phim cụ thể trên toàn bộ chuỗi rạp.
     *
     * @param movieId mã định danh phim
     * @return danh sách các suất chiếu của phim
     */
    public List<ShowtimeDetailDTO> getScheduledShowtimesByMovieId(int movieId) {
        String sql = "SELECT s.ShowtimeID, m.MovieID, b.CentreID, m.Title, "
                + "b.CentreName, b.City, h.HallName, s.StartTime, "
                + DYNAMIC_PRICE_SELECT
                + "FROM Showtimes s "
                + "JOIN Movies m ON m.MovieID = s.MovieID "
                + "JOIN Halls h ON h.HallID = s.HallID "
                + "JOIN Centres b ON b.CentreID = h.CentreID "
                + DYNAMIC_PRICE_APPLY
                + "WHERE s.Status = 'SCHEDULED' AND s.StartTime > SYSDATETIME() AND s.MovieID = ? "
                + "ORDER BY s.StartTime";
        List<ShowtimeDetailDTO> showtimes = new ArrayList<>();

        try (Connection connection = DBContext.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);) {
            statement.setInt(1, movieId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ShowtimeDetailDTO showtime = new ShowtimeDetailDTO();
                    showtime.setShowtimeId(resultSet.getInt("ShowtimeID"));
                    showtime.setMovieId(resultSet.getInt("MovieID"));
                    showtime.setCentreId(resultSet.getInt("CentreID"));
                    showtime.setMovieTitle(resultSet.getString("Title"));
                    showtime.setCentreName(resultSet.getString("CentreName"));
                    showtime.setCity(resultSet.getString("City"));
                    showtime.setHallName(resultSet.getString("HallName"));
                    showtime.setStartTime(resultSet.getTimestamp("StartTime"));
                    showtime.setBaseTicketPrice(resultSet.getBigDecimal("BaseTicketPrice"));
                    showtimes.add(showtime);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return showtimes;
    }

    /**
     * Lấy các suất chiếu đã lên lịch theo từng cụm rạp cụ thể.
     *
     * @param centreId mã định danh trung tâm
     * @return danh sách suất chiếu tại trung tâm
     */
    public List<ShowtimeDetailDTO> getScheduledShowtimesByCentreId(int centreId) {
        String sql = "SELECT s.ShowtimeID, m.MovieID, b.CentreID, m.Title, m.PosterURL, m.AgeRating, m.Duration, m.Genre, "
                + "b.CentreName, b.City, h.HallName, s.StartTime, "
                + DYNAMIC_PRICE_SELECT
                + "FROM Showtimes s "
                + "JOIN Movies m ON m.MovieID = s.MovieID "
                + "JOIN Halls h ON h.HallID = s.HallID "
                + "JOIN Centres b ON b.CentreID = h.CentreID "
                + DYNAMIC_PRICE_APPLY
                + "WHERE s.Status = 'SCHEDULED' AND s.StartTime > SYSDATETIME() AND m.Status IN ('NOW_SHOWING', 'COMING_SOON') AND b.CentreID = ? "
                + "ORDER BY s.StartTime";
        List<ShowtimeDetailDTO> showtimes = new ArrayList<>();

        try (Connection connection = DBContext.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, centreId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ShowtimeDetailDTO showtime = new ShowtimeDetailDTO();
                    showtime.setShowtimeId(resultSet.getInt("ShowtimeID"));
                    showtime.setMovieId(resultSet.getInt("MovieID"));
                    showtime.setCentreId(resultSet.getInt("CentreID"));
                    showtime.setMovieTitle(resultSet.getString("Title"));
                    showtime.setPosterUrl(resultSet.getString("PosterURL"));
                    showtime.setAgeRating(resultSet.getString("AgeRating"));
                    showtime.setDuration(resultSet.getInt("Duration"));
                    showtime.setGenre(resultSet.getString("Genre"));
                    showtime.setCentreName(resultSet.getString("CentreName"));
                    showtime.setCity(resultSet.getString("City"));
                    showtime.setHallName(resultSet.getString("HallName"));
                    showtime.setStartTime(resultSet.getTimestamp("StartTime"));
                    showtime.setBaseTicketPrice(resultSet.getBigDecimal("BaseTicketPrice"));
                    showtimes.add(showtime);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return showtimes;
    }
}
