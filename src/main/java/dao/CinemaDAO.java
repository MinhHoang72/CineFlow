package dao;

import model.Centre;
import model.Movie;
import model.ShowtimeView;
import utils.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Xử lý truy vấn dữ liệu liên quan đến phim, rạp chiếu (trung tâm) và lịch chiếu.
 */
public class CinemaDAO {

    /**
     * Lấy danh sách phim đang chiếu tại các rạp.
     *
     * @return danh sách phim có trạng thái NOW_SHOWING
     */
    public List<Movie> getNowShowingMovies() {
        return getMoviesByStatus("NOW_SHOWING");
    }

    /**
     * Lấy danh sách phim sắp chiếu.
     *
     * @return danh sách phim có trạng thái COMING_SOON
     */
    public List<Movie> getComingSoonMovies() {
        return getMoviesByStatus("COMING_SOON");
    }

    /**
     * Lấy danh sách phim theo trạng thái phát hành.
     *
     * @param status trạng thái của phim
     * @return danh sách phim thỏa mãn điều kiện
     */
    private List<Movie> getMoviesByStatus(String status) {
        String sql = "SELECT MovieID, Title, Description, Duration, Genre, Director, Cast, Language, "
                + "ReleaseDate, EndDate, PosterURL, TrailerURL, AgeRating, Status FROM Movies WHERE Status = ? "
                + "ORDER BY ReleaseDate DESC, MovieID DESC";
        List<Movie> movies = new ArrayList<>();

        try (Connection connection = DBContext.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    movies.add(mapMovie(resultSet));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return movies;
    }

    /**
     * Lấy thông tin chi tiết của một bộ phim theo mã phim.
     *
     * @param movieId mã định danh phim
     * @return đối tượng Movie nếu tìm thấy, ngược lại null
     */
    public Movie getMovieById(int movieId) {
        String sql = "SELECT MovieID, Title, Description, Duration, Genre, Director, Cast, Language, "
                + "ReleaseDate, EndDate, PosterURL, TrailerURL, AgeRating, Status "
                + "FROM Movies WHERE MovieID = ?";

        try (Connection connection = DBContext.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, movieId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Movie movie = new Movie();
                    movie.setMovieId(resultSet.getInt("MovieID"));
                    movie.setTitle(resultSet.getString("Title"));
                    movie.setDescription(resultSet.getString("Description"));
                    movie.setDuration(resultSet.getInt("Duration"));
                    movie.setGenre(resultSet.getString("Genre"));
                    movie.setDirector(resultSet.getString("Director"));
                    movie.setCast(resultSet.getString("Cast"));
                    movie.setLanguage(resultSet.getString("Language"));
                    movie.setReleaseDate(resultSet.getDate("ReleaseDate"));
                    movie.setEndDate(resultSet.getDate("EndDate"));
                    movie.setPosterUrl(resultSet.getString("PosterURL"));
                    movie.setTrailerUrl(resultSet.getString("TrailerURL"));
                    movie.setAgeRating(resultSet.getString("AgeRating"));
                    movie.setStatus(resultSet.getString("Status"));
                    return movie;
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return null;
    }

    /**
     * Lấy danh sách tất cả các trung tâm chiếu phim đang hoạt động.
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
     * Lấy danh sách các thành phố có rạp hoạt động kèm theo số lượng rạp.
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
     * Lấy danh sách rạp đang hoạt động theo thành phố.
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
     * Lấy thông tin chi tiết một trung tâm theo mã, bao gồm thông tin Quản lý hiện tại.
     *
     * @param centreId mã định danh trung tâm
     * @return đối tượng Centre nếu tìm thấy, ngược lại null
     */
    public Centre getCentreById(int centreId) {
        String sql = "SELECT b.CentreID, b.CentreName, b.Address, b.City, b.Phone, b.Status, "
                + "COUNT(DISTINCT h.HallID) AS HallCount, "
                + "COALESCE(SUM(h.Capacity), 0) AS SeatCount, "
                + "cma.ManagerID, u.FullName AS ManagerName, u.Email AS ManagerEmail, u.Status AS ManagerStatus "
                + "FROM Centres b "
                + "LEFT JOIN Halls h ON h.CentreID = b.CentreID "
                + "LEFT JOIN CentreManagerAssignments cma ON cma.CentreID = b.CentreID AND cma.Status = 'ACTIVE' AND cma.EndDate IS NULL "
                + "LEFT JOIN Users u ON u.UserID = cma.ManagerID "
                + "WHERE b.CentreID = ? "
                + "GROUP BY b.CentreID, b.CentreName, b.Address, b.City, b.Phone, b.Status, cma.ManagerID, u.FullName, u.Email, u.Status";

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
                    }
                    return centre;
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return null;
    }

    /**
     * Lấy danh sách các trung tâm kèm trạng thái đã được phân phối một bộ phim cụ thể hay chưa,
     * đồng thời đếm số lượng suất chiếu sắp tới của phim tại từng trung tâm.
     *
     * @param movieId mã định danh phim
     * @return danh sách trung tâm kèm cờ distributed và số suất chiếu tương lai
     */
    public List<Centre> getDistributionCentres(int movieId) {
        String sql = "SELECT b.CentreID, b.CentreName, b.Address, b.City, b.Phone, b.Status, "
                + "CAST(CASE WHEN UPPER(b.Status) = 'ACTIVE' AND bm.IsActive = 1 THEN 1 ELSE 0 END AS bit) AS [Distributed], "
                + "COALESCE((SELECT COUNT(*) FROM Showtimes s JOIN Halls h ON s.HallID = h.HallID "
                + "          WHERE s.MovieID = ? AND h.CentreID = b.CentreID AND s.StartTime > GETDATE() "
                + "          AND UPPER(s.Status) = 'SCHEDULED'), 0) AS [FutureShowtimeCount] "
                + "FROM Centres b LEFT JOIN CentreMovies bm ON bm.CentreID = b.CentreID AND bm.MovieID = ? "
                + "ORDER BY b.City, b.CentreName";
        List<Centre> centres = new ArrayList<>();
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            ps.setInt(2, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Centre centre = new Centre();
                    centre.setCentreId(rs.getInt("CentreID"));
                    centre.setCentreName(rs.getString("CentreName"));
                    centre.setAddress(rs.getString("Address"));
                    centre.setCity(rs.getString("City"));
                    centre.setPhone(rs.getString("Phone"));
                    centre.setStatus(rs.getString("Status"));
                    centre.setDistributed(rs.getBoolean("Distributed"));
                    centre.setFutureShowtimeCount(rs.getInt("FutureShowtimeCount"));
                    centres.add(centre);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return centres;
    }

    /**
     * Đếm số lượng suất chiếu trong tương lai của một bộ phim tại một trung tâm cụ thể.
     */
    public int countFutureShowtimes(int movieId, int centreId) {
        String sql = "SELECT COUNT(*) FROM Showtimes s JOIN Halls h ON s.HallID = h.HallID "
                + "WHERE s.MovieID = ? AND h.CentreID = ? AND s.StartTime > GETDATE() "
                + "AND UPPER(s.Status) = 'SCHEDULED'";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            ps.setInt(2, centreId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return 0;
    }

    /**
     * Lấy thông tin phân phối của một phim tại một trung tâm cụ thể.
     *
     * @param movieId  mã định danh phim
     * @param centreId mã định danh trung tâm
     * @return danh sách trung tâm kèm cờ distributed
     */
    public List<Centre> getDistributionCentres(int movieId, int centreId) {
        List<Centre> centres = new ArrayList<>();
        String sql = "SELECT b.CentreID, b.CentreName, b.Address, b.City, b.Phone, b.Status, "
                + "CAST(CASE WHEN UPPER(b.Status) = 'ACTIVE' AND bm.IsActive = 1 THEN 1 ELSE 0 END AS bit) AS [Distributed] "
                + "FROM Centres b LEFT JOIN CentreMovies bm ON bm.CentreID = b.CentreID AND bm.MovieID = ? "
                + "WHERE b.CentreID = ?";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            ps.setInt(2, centreId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Centre centre = new Centre();
                    centre.setCentreId(rs.getInt("CentreID"));
                    centre.setCentreName(rs.getString("CentreName"));
                    centre.setAddress(rs.getString("Address"));
                    centre.setCity(rs.getString("City"));
                    centre.setPhone(rs.getString("Phone"));
                    centre.setStatus(rs.getString("Status"));
                    centre.setDistributed(rs.getBoolean("Distributed"));
                    centres.add(centre);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return centres;
    }

    /**
     * Bật hoặc tắt trạng thái phân phối của một bộ phim tại một trung tâm cụ thể.
     *
     * @param movieId  mã định danh phim
     * @param centreId mã định danh trung tâm
     * @param enabled  trạng thái kích hoạt phân phối
     * @return true nếu lưu thành công, ngược lại false
     */
    public boolean saveMovieDistributionForCentre(int movieId, int centreId, boolean enabled) {
        String sql = "MERGE CentreMovies AS target USING (SELECT ? AS CentreID, ? AS MovieID) AS source "
                + "ON target.CentreID = source.CentreID AND target.MovieID = source.MovieID "
                + "WHEN MATCHED THEN UPDATE SET IsActive = ?, UpdatedAt = GETDATE() "
                + "WHEN NOT MATCHED THEN INSERT (CentreID, MovieID, IsActive) VALUES (source.CentreID, source.MovieID, ?);";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, centreId);
            ps.setInt(2, movieId);
            ps.setBoolean(3, enabled);
            ps.setBoolean(4, enabled);
            ps.executeUpdate();
            return true;
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }

    /**
     * Cập nhật danh sách trung tâm được phân phối phim trong một giao dịch.
     *
     * @param movieId           mã định danh phim
     * @param selectedCentreIDs tập hợp mã các trung tâm được chọn phân phối
     * @return true nếu lưu thành công, ngược lại false
     */
    public boolean saveMovieDistribution(int movieId, Set<Integer> selectedCentreIDs) {
        String disableInactiveSql = "UPDATE bm SET bm.IsActive = 0, bm.UpdatedAt = GETDATE() "
                + "FROM CentreMovies bm JOIN Centres b ON b.CentreID = bm.CentreID "
                + "WHERE bm.MovieID = ? AND (b.Status IS NULL OR UPPER(b.Status) <> 'ACTIVE')";
        String disableAllSql = "UPDATE CentreMovies SET IsActive = 0, UpdatedAt = GETDATE() WHERE MovieID = ?";
        String mergeSql = "MERGE CentreMovies AS target "
                + "USING (SELECT ? AS CentreID, ? AS MovieID WHERE EXISTS ("
                + "SELECT 1 FROM Centres b WHERE b.CentreID = ? AND UPPER(b.Status) = 'ACTIVE')) AS source "
                + "ON target.CentreID = source.CentreID AND target.MovieID = source.MovieID "
                + "WHEN MATCHED THEN UPDATE SET IsActive = 1, UpdatedAt = GETDATE() "
                + "WHEN NOT MATCHED THEN INSERT (CentreID, MovieID, IsActive) VALUES (source.CentreID, source.MovieID, 1);";

        // 1. Mở kết nối và bắt đầu khối Transaction
        try (Connection connection = DBContext.getConnection()) {
            connection.setAutoCommit(false);
            try {
                // 2. Vô hiệu hóa phân phối đối với các trung tâm không còn ở trạng thái ACTIVE
                try (PreparedStatement inactive = connection.prepareStatement(disableInactiveSql)) {
                    inactive.setInt(1, movieId);
                    inactive.executeUpdate();
                }

                // 3. Tạm thời vô hiệu hóa tất cả phân phối cũ của bộ phim này
                try (PreparedStatement disable = connection.prepareStatement(disableAllSql)) {
                    disable.setInt(1, movieId);
                    disable.executeUpdate();
                }

                // 4. Thực thi câu lệnh MERGE theo Batch để cập nhật/thêm mới các rạp được chọn
                if (selectedCentreIDs != null && !selectedCentreIDs.isEmpty()) {
                    try (PreparedStatement merge = connection.prepareStatement(mergeSql)) {
                        for (Integer centreId : selectedCentreIDs) {
                            merge.setInt(1, centreId);
                            merge.setInt(2, movieId);
                            merge.setInt(3, centreId);
                            merge.addBatch();
                        }
                        merge.executeBatch();
                    }
                }

                // 5. Xác nhận thành công (Commit Transaction)
                connection.commit();
                return true;
            } catch (Exception exception) {
                // 6. Hoàn tác thay đổi nếu xảy ra lỗi trong Transaction
                connection.rollback();
                throw exception;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }


    private static final String DYNAMIC_PRICE_SELECT = "(COALESCE(bp.BasePrice, 80000) + COALESCE(tsp.Surcharge, 0)) AS BaseTicketPrice ";
    private static final String DYNAMIC_PRICE_APPLY = "OUTER APPLY ( "
            + "    SELECT TOP 1 bp.BasePrice FROM BranchPricing bp "
            + "    WHERE bp.BranchID = b.CentreID AND s.StartTime >= bp.EffectiveFrom "
            + "      AND (bp.EffectiveTo IS NULL OR s.StartTime <= bp.EffectiveTo) "
            + "    ORDER BY bp.EffectiveFrom DESC "
            + ") bp "
            + "OUTER APPLY ( "
            + "    SELECT TOP 1 tsp.Surcharge FROM TimeSlotPricing tsp "
            + "    WHERE tsp.BranchID = b.CentreID "
            + "      AND CAST(s.StartTime AS TIME) >= tsp.StartTime "
            + "      AND CAST(s.StartTime AS TIME) < tsp.EndTime "
            + "      AND s.StartTime >= tsp.EffectiveFrom "
            + "      AND (tsp.EffectiveTo IS NULL OR s.StartTime <= tsp.EffectiveTo) "
            + "    ORDER BY tsp.EffectiveFrom DESC "
            + ") tsp ";

    /**
     * Lấy tất cả các suất chiếu đã lên lịch của những phim đang chiếu hoặc sắp chiếu.
     *
     * @return danh sách các suất chiếu đã lên lịch
     */
    public List<ShowtimeView> getScheduledShowtimes() {
        String sql = "SELECT s.ShowtimeID, m.MovieID, b.CentreID, m.Title, "
                + "b.CentreName, b.City, h.HallName, s.StartTime, "
                + DYNAMIC_PRICE_SELECT
                + "FROM Showtimes s "
                + "JOIN Movies m ON m.MovieID = s.MovieID "
                + "JOIN Halls h ON h.HallID = s.HallID "
                + "JOIN Centres b ON b.CentreID = h.CentreID "
                + DYNAMIC_PRICE_APPLY
                + "WHERE s.Status = 'SCHEDULED' AND m.Status IN ('NOW_SHOWING', 'COMING_SOON') "
                + "ORDER BY s.StartTime";
        List<ShowtimeView> showtimes = new ArrayList<>();

        try (Connection connection = DBContext.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                ShowtimeView showtime = new ShowtimeView();
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
     * Lấy các suất chiếu đã lên lịch cho một phim cụ thể.
     *
     * @param movieId mã định danh phim
     * @return danh sách các suất chiếu của phim
     */
    public List<ShowtimeView> getScheduledShowtimesByMovieId(int movieId) {
        String sql = "SELECT s.ShowtimeID, m.MovieID, b.CentreID, m.Title, "
                + "b.CentreName, b.City, h.HallName, s.StartTime, "
                + DYNAMIC_PRICE_SELECT
                + "FROM Showtimes s "
                + "JOIN Movies m ON m.MovieID = s.MovieID "
                + "JOIN Halls h ON h.HallID = s.HallID "
                + "JOIN Centres b ON b.CentreID = h.CentreID "
                + DYNAMIC_PRICE_APPLY
                + "WHERE s.Status = 'SCHEDULED' AND s.MovieID = ? "
                + "ORDER BY s.StartTime";
        List<ShowtimeView> showtimes = new ArrayList<>();

        try (Connection connection = DBContext.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, movieId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ShowtimeView showtime = new ShowtimeView();
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
     * Lấy các suất chiếu đã lên lịch theo từng trung tâm rạp.
     *
     * @param centreId mã định danh trung tâm
     * @return danh sách suất chiếu tại trung tâm
     */
    public List<ShowtimeView> getScheduledShowtimesByCentreId(int centreId) {
        String sql = "SELECT s.ShowtimeID, m.MovieID, b.CentreID, m.Title, m.PosterURL, m.AgeRating, m.Duration, m.Genre, "
                + "b.CentreName, b.City, h.HallName, s.StartTime, "
                + DYNAMIC_PRICE_SELECT
                + "FROM Showtimes s "
                + "JOIN Movies m ON m.MovieID = s.MovieID "
                + "JOIN Halls h ON h.HallID = s.HallID "
                + "JOIN Centres b ON b.CentreID = h.CentreID "
                + DYNAMIC_PRICE_APPLY
                + "WHERE s.Status = 'SCHEDULED' AND m.Status IN ('NOW_SHOWING', 'COMING_SOON') AND b.CentreID = ? "
                + "ORDER BY s.StartTime";
        List<ShowtimeView> showtimes = new ArrayList<>();

        try (Connection connection = DBContext.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, centreId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ShowtimeView showtime = new ShowtimeView();
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

    /**
     * Lấy toàn bộ danh sách phim để phục vụ trang quản trị,
     * bao gồm số lượng trung tâm đang phân phối phim và tổng số trung tâm hoạt động.
     *
     * @return danh sách tất cả các phim
     */
    public List<Movie> getAllMovies() {
        String sql = "SELECT m.MovieID, m.Title, m.Description, m.Duration, m.Genre, m.Director, m.Cast, m.Language, "
                + "m.ReleaseDate, m.EndDate, m.PosterURL, m.TrailerURL, m.AgeRating, m.Status, "
                + "(SELECT COUNT(*) FROM Centres WHERE UPPER(Status) = 'ACTIVE') AS TotalCentres, "
                + "COALESCE((SELECT COUNT(DISTINCT cm.CentreID) FROM CentreMovies cm "
                + "          JOIN Centres c ON c.CentreID = cm.CentreID "
                + "          WHERE cm.MovieID = m.MovieID AND cm.IsActive = 1 AND UPPER(c.Status) = 'ACTIVE'), 0) AS DistributedCentres "
                + "FROM Movies m ORDER BY m.MovieID DESC";
        List<Movie> movies = new ArrayList<>();
        try (Connection connection = DBContext.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                movies.add(mapMovie(rs));
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return movies;
    }

    /**
     * Tạo mới một bộ phim trong cơ sở dữ liệu.
     *
     * @param movie đối tượng thông tin phim cần tạo
     * @return mã định danh phim tự sinh nếu thành công, ngược lại 0
     */
    public int createMovie(Movie movie) {
        String sql = "INSERT INTO Movies (Title, Description, Duration, Genre, Director, Cast, Language, "
                + "ReleaseDate, EndDate, PosterURL, TrailerURL, AgeRating, Status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        // 1. Mở kết nối và chuẩn bị PreparedStatement với cờ trả về khóa tự sinh
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            // 2. Thiết lập các tham số thông tin phim vào câu lệnh SQL
            setMovieParams(ps, movie, false);
            ps.executeUpdate();

            // 3. Lấy mã MovieID mới sinh từ cơ sở dữ liệu
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return 0;
    }

    /**
     * Cập nhật thông tin của một bộ phim.
     *
     * @param movie đối tượng phim đã thay đổi thông tin
     * @return true nếu cập nhật thành công, ngược lại false
     */
    public boolean updateMovie(Movie movie) {
        String sql = "UPDATE Movies SET Title = ?, Description = ?, Duration = ?, Genre = ?, Director = ?, "
                + "Cast = ?, Language = ?, ReleaseDate = ?, EndDate = ?, PosterURL = ?, TrailerURL = ?, "
                + "AgeRating = ?, Status = ?, UpdatedAt = GETDATE() WHERE MovieID = ?";
        // 1. Mở kết nối và chuẩn bị câu lệnh cập nhật phim
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            // 2. Điền các tham số và cập nhật thông tin
            setMovieParams(ps, movie, true);
            return ps.executeUpdate() > 0;
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }

    /**
     * Lưu trữ (lưu kho) phim thay vì xóa cứng.
     *
     * @param movieId mã định danh phim
     * @return true nếu cập nhật thành công, ngược lại false
     */
    public boolean archiveMovie(int movieId) {
        return updateMovieStatus(movieId, "ARCHIVED");
    }

    /**
     * Cập nhật trạng thái của phim (COMING_SOON, NOW_SHOWING, ENDED, ARCHIVED).
     *
     * @param movieId mã định danh phim
     * @param status trạng thái mới
     * @return true nếu cập nhật thành công, ngược lại false
     */
    public boolean updateMovieStatus(int movieId, String status) {
        String sql = "UPDATE Movies SET Status = ?, UpdatedAt = GETDATE() WHERE MovieID = ?";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, movieId);
            return ps.executeUpdate() > 0;
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }

    /**
     * Xóa hoàn toàn một bộ phim theo mã định danh.
     *
     * @param movieId mã định danh phim
     * @return true nếu xóa thành công, ngược lại false
     */
    public boolean deleteMovie(int movieId) {
        String sql = "DELETE FROM Movies WHERE MovieID = ?";
        // 1. Mở kết nối và thực thi lệnh xóa phim theo MovieID
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            return ps.executeUpdate() > 0;
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }

    /**
     * Chuyển đổi dữ liệu từ dòng ResultSet sang đối tượng Movie.
     */
    private Movie mapMovie(ResultSet rs) throws SQLException {
        Movie movie = new Movie();
        movie.setMovieId(rs.getInt("MovieID"));
        movie.setTitle(rs.getString("Title"));
        movie.setDescription(rs.getString("Description"));
        movie.setDuration(rs.getInt("Duration"));
        movie.setGenre(rs.getString("Genre"));
        movie.setDirector(rs.getString("Director"));
        movie.setCast(rs.getString("Cast"));
        movie.setLanguage(rs.getString("Language"));
        movie.setReleaseDate(rs.getDate("ReleaseDate"));
        movie.setEndDate(rs.getDate("EndDate"));
        movie.setPosterUrl(rs.getString("PosterURL"));
        movie.setTrailerUrl(rs.getString("TrailerURL"));
        movie.setAgeRating(rs.getString("AgeRating"));
        movie.setStatus(rs.getString("Status"));
        try {
            movie.setDistributedCentres(rs.getInt("DistributedCentres"));
            movie.setTotalCentres(rs.getInt("TotalCentres"));
        } catch (SQLException ignored) {
        }
        return movie;
    }

    /**
     * Thiết lập các tham số vào PreparedStatement cho thao tác tạo hoặc cập nhật phim.
     */
    private void setMovieParams(PreparedStatement ps, Movie movie, boolean update) throws SQLException {
        ps.setString(1, movie.getTitle());
        ps.setString(2, movie.getDescription());
        ps.setInt(3, movie.getDuration());
        ps.setString(4, movie.getGenre());
        ps.setString(5, movie.getDirector());
        ps.setString(6, movie.getCast());
        ps.setString(7, movie.getLanguage());
        ps.setDate(8, movie.getReleaseDate());
        ps.setDate(9, movie.getEndDate());
        ps.setString(10, movie.getPosterUrl());
        ps.setString(11, movie.getTrailerUrl());
        ps.setString(12, movie.getAgeRating());
        ps.setString(13, movie.getStatus());
        if (update) {
            ps.setInt(14, movie.getMovieId());
        }
    }

    /**
     * Lấy toàn bộ danh sách trung tâm cùng thông tin Quản lý phụ trách tương ứng.
     *
     * @return danh sách các trung tâm kèm thông tin Quản lý
     */
    public List<Centre> getAllCentresWithManager() {
        String sql = "SELECT b.CentreID, b.CentreName, b.Address, b.City, b.Phone, b.Status, "
                + "COUNT(DISTINCT h.HallID) AS HallCount, "
                + "COALESCE(SUM(h.Capacity), 0) AS SeatCount, "
                + "cma.ManagerID, u.FullName AS ManagerName, u.Email AS ManagerEmail, u.Status AS ManagerStatus "
                + "FROM Centres b "
                + "LEFT JOIN Halls h ON h.CentreID = b.CentreID "
                + "LEFT JOIN CentreManagerAssignments cma ON cma.CentreID = b.CentreID AND cma.Status = 'ACTIVE' AND cma.EndDate IS NULL "
                + "LEFT JOIN Users u ON u.UserID = cma.ManagerID "
                + "GROUP BY b.CentreID, b.CentreName, b.Address, b.City, b.Phone, b.Status, cma.ManagerID, u.FullName, u.Email, u.Status "
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
}

