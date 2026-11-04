package dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import model.entity.Centre;
import model.entity.Movie;
import utils.DBContext;

/**
 * Data Access Object: Xử lý toàn bộ các thao tác truy vấn và cập nhật CSDL liên quan đến Phim (Movies)
 * và phân phối phim tới các cụm rạp (CentreMovies).
 */
public class MovieDAO {

    /**
     * Lấy danh sách phim đang chiếu tại các rạp (NOW_SHOWING).
     *
     * @return danh sách các bộ phim đang chiếu
     */
    public List<Movie> getNowShowingMovies() {
        return getMoviesByStatus("NOW_SHOWING");
    }

    /**
     * Lấy danh sách phim sắp khởi chiếu (COMING_SOON).
     *
     * @return danh sách các bộ phim sắp khởi chiếu
     */
    public List<Movie> getComingSoonMovies() {
        return getMoviesByStatus("COMING_SOON");
    }

    /**
     * Lấy danh sách phim theo trạng thái phát hành.
     *
     * @param status trạng thái phim (NOW_SHOWING, COMING_SOON, ENDED, ARCHIVED)
     * @return danh sách các phim thỏa mãn
     */
    public List<Movie> getMoviesByStatus(String status) {
        String sql = "SELECT MovieID, Title, Description, Duration, Genre, Director, Cast, Language, "
                + "ReleaseDate, EndDate, PosterURL, TrailerURL, AgeRating, Status, CreatedAt, UpdatedAt FROM Movies WHERE Status = ? "
                + "ORDER BY MovieID DESC, ReleaseDate DESC";
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
     * Lấy thông tin chi tiết của một bộ phim theo mã định danh MovieID.
     *
     * @param movieId mã định danh phim
     * @return đối tượng Movie nếu tìm thấy, ngược lại null
     */
    public Movie getMovieById(int movieId) {
        String sql = "SELECT MovieID, Title, Description, Duration, Genre, Director, Cast, Language, "
                + "ReleaseDate, EndDate, PosterURL, TrailerURL, AgeRating, Status, CreatedAt, UpdatedAt "
                + "FROM Movies WHERE MovieID = ?";

        try (Connection connection = DBContext.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, movieId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapMovie(resultSet);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return null;
    }

    /**
     * Lấy toàn bộ danh sách phim để phục vụ trang quản trị,
     * bao gồm số lượng trung tâm đang phân phối phim và tổng số trung tâm hoạt động.
     *
     * @return danh sách tất cả các phim sắp xếp mới nhất lên đầu
     */
    public List<Movie> getAllMovies() {
        String sql = "SELECT m.MovieID, m.Title, m.Description, m.Duration, m.Genre, m.Director, m.Cast, m.Language, "
                + "m.ReleaseDate, m.EndDate, m.PosterURL, m.TrailerURL, m.AgeRating, m.Status, m.CreatedAt, m.UpdatedAt, "
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
     * Tạo mới một bộ phim trong cơ sở dữ liệu và trả về mã MovieID tự sinh.
     *
     * @param movie đối tượng thông tin phim cần tạo
     * @return mã định danh phim tự sinh nếu thành công (> 0), ngược lại 0
     */
    public int createMovie(Movie movie) {
        String sql = "INSERT INTO Movies (Title, Description, Duration, Genre, Director, Cast, Language, "
                + "ReleaseDate, EndDate, PosterURL, TrailerURL, AgeRating, Status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setMovieParams(ps, movie, false);
            ps.executeUpdate();

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
     * Phương thức tương thích ngược thêm phim trả về boolean.
     */
    public boolean insertMovie(Movie movie) {
        return createMovie(movie) > 0;
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
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            setMovieParams(ps, movie, true);
            return ps.executeUpdate() > 0;
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
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
     * Kết thúc chiếu phim ngay thời điểm hiện tại: Cập nhật Status = 'ENDED' và gán EndDate = ngày hôm nay.
     *
     * @param movieId mã định danh phim
     * @return true nếu cập nhật thành công, ngược lại false
     */
    public boolean endMovie(int movieId) {
        String sql = "UPDATE Movies SET Status = 'ENDED', EndDate = CASE WHEN EndDate IS NULL OR EndDate > CAST(GETDATE() AS DATE) THEN CAST(GETDATE() AS DATE) ELSE EndDate END, UpdatedAt = GETDATE() WHERE MovieID = ?";
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
     * Lưu trữ (lưu kho) phim thay vì xóa cứng.
     *
     * @param movieId mã định danh phim
     * @return true nếu cập nhật thành công, ngược lại false
     */
    public boolean archiveMovie(int movieId) {
        return updateMovieStatus(movieId, "ARCHIVED");
    }

    /**
     * Xóa hoàn toàn một bộ phim theo mã định danh.
     *
     * @param movieId mã định danh phim
     * @return true nếu xóa thành công, ngược lại false
     */
    public boolean deleteMovie(int movieId) {
        String sql = "DELETE FROM Movies WHERE MovieID = ?";
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
     * Lấy thời lượng phim (phút).
     */
    public int getMovieDuration(int movieId) {
        String sql = "SELECT Duration FROM Movies WHERE MovieID = ?";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("Duration");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Phim còn ít nhất một suất mở bán tại rạp (phục vụ luồng đặt vé).
     */
    public List<Movie> getMoviesShowingAt(int centreId) throws SQLException {
        String sql = "SELECT mv.MovieID, mv.Title, mv.Genre, mv.Duration, mv.AgeRating, mv.PosterURL, mv.Status "
                   + "FROM Movies mv "
                   + "WHERE EXISTS (SELECT 1 " + ShowtimeDAO.BOOKABLE_FROM
                   + "              AND s.MovieID = mv.MovieID AND b.CentreID = ?) "
                   + "ORDER BY mv.Title";
        List<Movie> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, centreId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapMovie(rs));
                }
            }
        }
        return list;
    }

    /**
     * Lấy danh sách phim đang chiếu được phân phối cho một cụm rạp.
     */
    public List<Movie> getAvailableMoviesByCentre(int centreId) {
        List<Movie> list = new ArrayList<>();
        String sql = "SELECT m.* "
                + "FROM Movies m "
                + "JOIN CentreMovies bm ON bm.MovieID = m.MovieID "
                + "WHERE bm.CentreID = ? "
                + "AND bm.IsActive = 1 "
                + "AND m.Status = 'NOW_SHOWING' "
                + "ORDER BY m.Title";

        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, centreId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapMovie(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // =========================================================================
    // PHÂN HỆ PHÂN PHỐI PHIM TỚI CÁC CỤM RẠP (CentreMovies)
    // =========================================================================

    /**
     * Lấy danh sách các trung tâm kèm trạng thái đã được phân phối một bộ phim cụ thể hay chưa,
     * đồng thời đếm số lượng suất chiếu sắp tới của phim tại từng trung tâm.
     *
     * @param movieId mã định danh phim
     * @return danh sách trung tâm kèm cờ distributed và số suất chiếu tương lai
     */
    public List<Centre> getDistributionCentres(int movieId) {
        String sql = "SELECT b.CentreID, b.CentreName, b.Address, b.City, b.Phone, b.Status, "
                + "CAST(CASE WHEN bm.IsActive = 1 THEN 1 ELSE 0 END AS bit) AS [Distributed], "
                + "COALESCE((SELECT COUNT(*) FROM Showtimes s JOIN Halls h ON s.HallID = h.HallID "
                + "          WHERE s.MovieID = ? AND h.CentreID = b.CentreID AND s.StartTime > GETDATE() "
                + "          AND UPPER(s.Status) = 'SCHEDULED'), 0) AS [FutureShowtimeCount], "
                + "COALESCE((SELECT COUNT(t.TicketID) FROM Tickets t "
                + "          JOIN BookingSeats bs ON t.BookingSeatID = bs.BookingSeatID "
                + "          JOIN Bookings bk ON bs.BookingID = bk.BookingID "
                + "          JOIN Showtimes s ON bk.ShowtimeID = s.ShowtimeID "
                + "          JOIN Halls h ON s.HallID = h.HallID "
                + "          WHERE s.MovieID = ? AND h.CentreID = b.CentreID AND s.StartTime > GETDATE() "
                + "          AND UPPER(s.Status) = 'SCHEDULED' AND UPPER(t.TicketStatus) <> 'CANCELLED'), 0) AS [SoldTicketCount] "
                + "FROM Centres b LEFT JOIN CentreMovies bm ON bm.CentreID = b.CentreID AND bm.MovieID = ? "
                + "ORDER BY b.City, b.CentreName";
        List<Centre> centres = new ArrayList<>();
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            ps.setInt(2, movieId);
            ps.setInt(3, movieId);
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
                    centre.setSoldTicketCount(rs.getInt("SoldTicketCount"));
                    centres.add(centre);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return centres;
    }

    /**
     * Lấy thông tin phân phối của một phim tại một trung tâm cụ thể.
     */
    public List<Centre> getDistributionCentres(int movieId, int centreId) {
        List<Centre> centres = new ArrayList<>();
        String sql = "SELECT b.CentreID, b.CentreName, b.Address, b.City, b.Phone, b.Status, "
                + "CAST(CASE WHEN bm.IsActive = 1 THEN 1 ELSE 0 END AS bit) AS [Distributed] "
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
     * Kiểm tra xem bộ phim đã từng được cấu hình phân phối trong CSDL hay chưa.
     */
    public boolean hasDistributionConfig(int movieId) {
        String sql = "SELECT COUNT(*) FROM CentreMovies WHERE MovieID = ?";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return false;
    }

    /**
     * Bật hoặc tắt trạng thái phân phối của một bộ phim tại một trung tâm cụ thể.
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
     * Cập nhật danh sách trung tâm được phân phối phim trong một Transaction an toàn.
     */
    public boolean saveMovieDistribution(int movieId, Set<Integer> selectedCentreIDs) {
        String disableAllSql = "UPDATE CentreMovies SET IsActive = 0, UpdatedAt = GETDATE() WHERE MovieID = ?";
        String mergeSql = "MERGE CentreMovies AS target "
                + "USING (SELECT ? AS CentreID, ? AS MovieID WHERE EXISTS ("
                + "SELECT 1 FROM Centres b WHERE b.CentreID = ?)) AS source "
                + "ON target.CentreID = source.CentreID AND target.MovieID = source.MovieID "
                + "WHEN MATCHED THEN UPDATE SET IsActive = 1, UpdatedAt = GETDATE() "
                + "WHEN NOT MATCHED THEN INSERT (CentreID, MovieID, IsActive) VALUES (source.CentreID, source.MovieID, 1);";

        try (Connection connection = DBContext.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement disable = connection.prepareStatement(disableAllSql)) {
                    disable.setInt(1, movieId);
                    disable.executeUpdate();
                }

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

                connection.commit();
                return true;
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

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
     * Chuyển đổi dữ liệu từ dòng ResultSet sang đối tượng Movie với đầy đủ tất cả các trường trong SQL.
     */
    private Movie mapMovie(ResultSet rs) throws SQLException {
        Movie movie = new Movie();
        movie.setMovieId(rs.getInt("MovieID"));
        movie.setTitle(rs.getString("Title"));
        try { movie.setDescription(rs.getString("Description")); } catch (SQLException ignored) {}
        try { movie.setDuration(rs.getInt("Duration")); } catch (SQLException ignored) {}
        try { movie.setGenre(rs.getString("Genre")); } catch (SQLException ignored) {}
        try { movie.setDirector(rs.getString("Director")); } catch (SQLException ignored) {}
        try { movie.setCast(rs.getString("Cast")); } catch (SQLException ignored) {}
        try { movie.setLanguage(rs.getString("Language")); } catch (SQLException ignored) {}
        try { movie.setReleaseDate(rs.getDate("ReleaseDate")); } catch (SQLException ignored) {}
        try { movie.setEndDate(rs.getDate("EndDate")); } catch (SQLException ignored) {}
        try { movie.setPosterUrl(rs.getString("PosterURL")); } catch (SQLException ignored) {}
        try { movie.setTrailerUrl(rs.getString("TrailerURL")); } catch (SQLException ignored) {}
        try { movie.setAgeRating(rs.getString("AgeRating")); } catch (SQLException ignored) {}
        try { movie.setStatus(rs.getString("Status")); } catch (SQLException ignored) {}
        try { movie.setDistributedCentres(rs.getInt("DistributedCentres")); } catch (SQLException ignored) {}
        try { movie.setTotalCentres(rs.getInt("TotalCentres")); } catch (SQLException ignored) {}
        try { movie.setCreatedAt(rs.getTimestamp("CreatedAt")); } catch (SQLException ignored) {}
        try { movie.setUpdatedAt(rs.getTimestamp("UpdatedAt")); } catch (SQLException ignored) {}
        return movie;
    }
}
