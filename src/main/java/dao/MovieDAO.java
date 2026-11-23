package dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import model.entity.Centre;
import model.entity.Movie;
import utils.DBContext;
import utils.EmailUtil;

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
    private List<Movie> getMoviesByStatus(String status) {
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

    public boolean existsTitle(String title, int excludeMovieId) {
        String sql = "SELECT 1 FROM Movies WHERE UPPER(LTRIM(RTRIM(Title))) = UPPER(LTRIM(RTRIM(?))) AND MovieID <> ?";
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, title); ps.setInt(2, excludeMovieId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    /**
     * Lấy toàn bộ danh sách phim trong hệ thống, sắp xếp phim mới nhất lên đầu.
     *
     * @return danh sách tất cả các phim
     */
    public List<Movie> getAllMovies() {
        String sql = "SELECT * FROM Movies ORDER BY MovieID DESC";
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
     * Thêm mới một bộ phim vào cơ sở dữ liệu và trả về mã MovieID tự sinh (IDENTITY).
     *
     * @param movie đối tượng thông tin phim cần thêm
     * @return mã định danh phim tự sinh nếu thành công (> 0), ngược lại 0
     */
    public int insertMovie(Movie movie) {
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
     * Cập nhật thông tin của một bộ phim.
     *
     * @param movie đối tượng phim đã thay đổi thông tin
     * @return true nếu cập nhật thành công, ngược lại false
     */
    public boolean updateMovie(Movie movie) {
        String sql = "UPDATE Movies SET Title = ?, Description = ?, Duration = ?, Genre = ?, Director = ?, "
                + "Cast = ?, Language = ?, ReleaseDate = ?, EndDate = ?, PosterURL = ?, TrailerURL = ?, "
                + "AgeRating = ?, Status = ?, UpdatedAt = GETDATE() WHERE MovieID = ?";
        try (Connection connection = DBContext.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                setMovieParams(ps, movie, true);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    String st = movie.getStatus() != null ? movie.getStatus().trim().toUpperCase() : "";
                    // Nhánh phim bị cấm hoặc dừng chiếu khẩn cấp (ENDED hoặc ARCHIVED):
                    // Hệ thống tự động hủy các suất chiếu tương lai và gửi email thông báo hủy vé cho khách hàng
                    if ("ARCHIVED".equals(st) || "ENDED".equals(st)) {
                        cancelUpcomingShowtimesForMovie(connection, movie.getMovieId());
                    }
                    connection.commit();
                    return true;
                } else {
                    connection.rollback();
                    return false;
                }
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }

    /**
     * Cập nhật trạng thái của phim (COMING_SOON, NOW_SHOWING, ENDED, ARCHIVED).
     * Quy tắc nghiệp vụ:
     * - ENDED: Chốt ngày kết thúc là hôm nay và hủy phân phối tại tất cả rạp.
     * - ARCHIVED: Chốt ngày kết thúc ngay lập tức là hôm nay và hủy phân phối tại tất cả rạp.
     * - Chuyển từ ARCHIVED hoặc ENDED sang COMING_SOON: Phải có ngày khởi chiếu và kết thúc mới trong tương lai.
     *
     * @param movieId mã định danh phim
     * @param status trạng thái mới
     * @return true nếu cập nhật thành công, ngược lại false
     */
    public boolean updateMovieStatus(int movieId, String status) {
        if (movieId <= 0 || status == null || status.isBlank()) {
            return false;
        }
        String st = status.trim().toUpperCase();
        if ("ENDED".equals(st) || "ARCHIVED".equals(st)) {
            return closeMovie(movieId, st);
        }
        if ("COMING_SOON".equals(st)) {
            Movie curr = getMovieById(movieId);
            if (curr != null) {
                String oldStatus = curr.getStatus() != null ? curr.getStatus().toUpperCase() : "";
                if ("ARCHIVED".equals(oldStatus) || "ENDED".equals(oldStatus)) {
                    // Không được chuyển đổi sang COMING_SOON nếu chưa set lại ngày tháng mới trong tương lai
                    if (curr.getReleaseDate() == null || curr.getEndDate() == null
                            || curr.getReleaseDate().toLocalDate().isBefore(java.time.LocalDate.now())
                            || !curr.getEndDate().toLocalDate().isAfter(java.time.LocalDate.now())) {
                        return false;
                    }
                }
            }
        }

        String sql = "UPDATE Movies SET Status = ?, UpdatedAt = GETDATE() WHERE MovieID = ?";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, st);
            ps.setInt(2, movieId);
            return ps.executeUpdate() > 0;
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }

    /**
     * Đóng đợt chiếu phim (chuyển sang ENDED hoặc ARCHIVED):
     * - Cập nhật Status sang trạng thái yêu cầu.
     * - Chốt EndDate = hôm nay nếu phim đã khởi chiếu; nếu phim chưa tới ngày khởi chiếu (COMING_SOON bị cấm/hủy sớm)
     *   thì chốt EndDate = ReleaseDate để luôn đảm bảo EndDate >= ReleaseDate.
     * - Giữ nguyên phân phối tại các rạp (CentreMovies) để lưu vết lịch sử và hỗ trợ tái chiếu.
     * - Huỷ toàn bộ suất chiếu tương lai chưa bắt đầu của phim (nếu có) và gửi email thông báo tới khách hàng.
     */
    private boolean closeMovie(int movieId, String status) {
        String updateMovieSql = "UPDATE Movies SET Status = ?, "
                + "EndDate = CASE "
                + "    WHEN ReleaseDate IS NOT NULL AND ReleaseDate > CAST(GETDATE() AS DATE) "
                + "        THEN CASE WHEN EndDate IS NULL OR EndDate > ReleaseDate THEN ReleaseDate ELSE EndDate END "
                + "    WHEN EndDate IS NULL OR EndDate > CAST(GETDATE() AS DATE) "
                + "        THEN CAST(GETDATE() AS DATE) "
                + "    ELSE EndDate END, "
                + "UpdatedAt = GETDATE() WHERE MovieID = ?";
        try (Connection connection = DBContext.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int rows;
                try (PreparedStatement psMovie = connection.prepareStatement(updateMovieSql)) {
                    psMovie.setString(1, status);
                    psMovie.setInt(2, movieId);
                    rows = psMovie.executeUpdate();
                }
                if (rows > 0) {
                    // Huỷ các suất chiếu tương lai chưa bắt đầu của bộ phim này trên toàn hệ thống
                    cancelUpcomingShowtimesForMovie(connection, movieId);
                    connection.commit();
                    return true;
                } else {
                    connection.rollback();
                    return false;
                }
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }

    /** Bản ghi tạm lưu thông tin để gửi email thông báo hủy vé cho khách hàng. */
    private record NoticeInfo(String email, String fullName, String bookingCode, String movieTitle, String showtimeInfo, String reason) {}

    /** Huỷ các suất chiếu chưa bắt đầu của phim khi kết thúc chiếu (ENDED hoặc ARCHIVED) và gửi email thông báo. */
    private void cancelUpcomingShowtimesForMovie(Connection connection, int movieId) throws SQLException {
        List<NoticeInfo> notices = new ArrayList<>();
        String noticeSql = "SELECT b.BookingCode, u.Email, u.FullName, m.Title, s.StartTime, c.CentreName, h.HallName "
                + "FROM Bookings b "
                + "JOIN Users u ON u.UserID = b.UserID "
                + "JOIN Showtimes s ON s.ShowtimeID = b.ShowtimeID "
                + "JOIN Movies m ON m.MovieID = s.MovieID "
                + "JOIN Halls h ON h.HallID = s.HallID "
                + "JOIN Centres c ON c.CentreID = h.CentreID "
                + "WHERE b.BookingStatus IN ('CONFIRMED', 'PENDING') "
                + "AND s.MovieID = ? AND s.Status = 'SCHEDULED' AND s.StartTime > SYSDATETIME()";
        try (PreparedStatement ps = connection.prepareStatement(noticeSql)) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notices.add(new NoticeInfo(
                            rs.getString("Email"),
                            rs.getString("FullName"),
                            rs.getString("BookingCode"),
                            rs.getString("Title"),
                            rs.getString("CentreName") + " - " + rs.getString("HallName") + " (" + rs.getTimestamp("StartTime") + ")",
                            "Bộ phim đã kết thúc đợt chiếu tại hệ thống rạp CineFlow"
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String target = "IN (SELECT s.ShowtimeID FROM Showtimes s "
                + "WHERE s.MovieID=? AND s.Status='SCHEDULED' AND s.StartTime>SYSDATETIME())";
        String bookings = "IN (SELECT BookingID FROM Bookings WHERE ShowtimeID " + target + ")";
        String[] statements = {
            "UPDATE t SET t.TicketStatus='CANCELLED' FROM Tickets t JOIN BookingSeats bs ON bs.BookingSeatID=t.BookingSeatID WHERE t.TicketStatus='VALID' AND bs.ShowtimeID " + target,
            "UPDATE BookingSeats SET SeatStatus='CANCELLED' WHERE SeatStatus IN ('HELD','CONFIRMED') AND ShowtimeID " + target,
            "UPDATE Payments SET PaymentStatus='CANCELLED' WHERE PaymentStatus='PENDING' AND BookingID " + bookings,
            "UPDATE FnbOrders SET OrderStatus='CANCELLED',UpdatedAt=SYSDATETIME() WHERE OrderStatus<>'CANCELLED' AND BookingID " + bookings,
            "UPDATE Bookings SET BookingStatus='CANCELLED',UpdatedAt=SYSDATETIME() WHERE BookingStatus IN ('PENDING','CONFIRMED') AND ShowtimeID " + target,
            "UPDATE Showtimes SET Status='CANCELLED',UpdatedAt=SYSDATETIME() WHERE ShowtimeID " + target
        };
        for (String sql : statements) {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, movieId);
                ps.executeUpdate();
            }
        }

        for (NoticeInfo n : notices) {
            EmailUtil.sendTicketCancellationEmail(n.email(), n.fullName(), n.bookingCode(), n.movieTitle(), n.showtimeInfo(), n.reason());
        }
    }

    /**
     * Lấy thống kê số suất chiếu tương lai và số vé đã bán của phim.
     *
     * @param movieId mã định danh phim
     * @return mảng int[] với [0] = số suất chiếu tương lai, [1] = số vé đã bán
     */
    public int[] getUpcomingStats(int movieId) {
        if (movieId <= 0) {
            return new int[]{0, 0};
        }

        String sql = """
            SELECT
                (SELECT COUNT(*)
                 FROM Showtimes
                 WHERE MovieID = ?
                   AND Status = 'SCHEDULED'
                   AND StartTime > SYSDATETIME()) AS UpcomingShowtimes,

                (SELECT COUNT(t.TicketID)
                 FROM Tickets t
                 JOIN BookingSeats bs ON t.BookingSeatID = bs.BookingSeatID
                 JOIN Showtimes s ON bs.ShowtimeID = s.ShowtimeID
                 WHERE s.MovieID = ?
                   AND s.Status = 'SCHEDULED'
                   AND s.StartTime > SYSDATETIME()
                   AND t.TicketStatus <> 'CANCELLED') AS SoldTickets
            """;

        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            ps.setInt(2, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new int[]{rs.getInt(1), rs.getInt(2)};
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new int[]{0, 0};
    }

    /**
     * Lấy thời lượng phim (tính bằng phút).
     *
     * @param movieId mã định danh phim
     * @return thời lượng phim (phút), hoặc {@code -1} nếu không tìm thấy
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
     * Lấy danh sách phim đang có ít nhất một suất chiếu mở bán tại một cụm rạp cụ thể (phục vụ luồng đặt vé).
     *
     * @param centreId mã cụm rạp chiếu phim
     * @return danh sách phim đang mở bán tại rạp
     * @throws SQLException nếu xảy ra lỗi truy vấn cơ sở dữ liệu
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
     * Lấy danh sách các trung tâm kèm trạng thái đã được phân phối một bộ phim cụ thể hay chưa.
     *
     * @param movieId mã định danh phim
     * @return danh sách trung tâm kèm cờ isDistributed (true nếu rạp được quyền chiếu phim)
     */
    public List<Centre> getDistributionCentres(int movieId) {
        String sql = "SELECT b.CentreID, b.CentreName, b.Address, b.City, b.Phone, b.Status, "
                + "CAST(CASE WHEN UPPER(bm.Status) IN ('ACTIVE', 'SUSPENDED') OR (bm.Status IS NULL AND bm.IsActive = 1) "
                + "          THEN 1 ELSE 0 END AS bit) AS [Distributed] "
                + "FROM Centres b "
                + "LEFT JOIN CentreMovies bm ON bm.CentreID = b.CentreID AND bm.MovieID = ? "
                + "ORDER BY b.City, b.CentreName";
        List<Centre> centres = new ArrayList<>();
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, movieId);
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
     * Cập nhật danh sách cụm rạp được phân phối phim.
     */
    public boolean saveMovieDistribution(int movieId, Set<Integer> selectedCentreIDs) {
        String disableSql = "UPDATE CentreMovies SET Status = 'CANCELLED', IsActive = 0, UpdatedAt = GETDATE() WHERE MovieID = ?";
        String mergeSql = "MERGE CentreMovies t USING (SELECT ? AS c, ? AS m) s ON t.CentreID = s.c AND t.MovieID = s.m "
                + "WHEN MATCHED THEN UPDATE SET Status = 'ACTIVE', IsActive = 1, UpdatedAt = GETDATE() "
                + "WHEN NOT MATCHED THEN INSERT (CentreID, MovieID, Status, IsActive, CreatedAt) VALUES (s.c, s.m, 'ACTIVE', 1, GETDATE());";

        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false);

            // 1. Tắt phân phối tất cả các rạp cũ của phim
            try (PreparedStatement ps = conn.prepareStatement(disableSql)) {
                ps.setInt(1, movieId);
                ps.executeUpdate();
            }

            // 2. Kích hoạt phân phối cho các rạp được tích chọn
            if (selectedCentreIDs != null && !selectedCentreIDs.isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement(mergeSql)) {
                    for (int centreId : selectedCentreIDs) {
                        ps.setInt(1, centreId);
                        ps.setInt(2, movieId);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
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
        movie.setDescription(getSafeString(rs, "Description"));
        movie.setDuration(getSafeInt(rs, "Duration"));
        movie.setGenre(getSafeString(rs, "Genre"));
        movie.setDirector(getSafeString(rs, "Director"));
        movie.setCast(getSafeString(rs, "Cast"));
        movie.setLanguage(getSafeString(rs, "Language"));
        movie.setReleaseDate(getSafeDate(rs, "ReleaseDate"));
        movie.setEndDate(getSafeDate(rs, "EndDate"));
        movie.setPosterUrl(getSafeString(rs, "PosterURL"));
        movie.setTrailerUrl(getSafeString(rs, "TrailerURL"));
        movie.setAgeRating(getSafeString(rs, "AgeRating"));
        movie.setStatus(getSafeString(rs, "Status"));
        movie.setDistributedCentres(getSafeInt(rs, "DistributedCentres"));
        movie.setTotalCentres(getSafeInt(rs, "TotalCentres"));
        movie.setCreatedAt(getSafeTimestamp(rs, "CreatedAt"));
        movie.setUpdatedAt(getSafeTimestamp(rs, "UpdatedAt"));
        return movie;
    }

    private static String getSafeString(ResultSet rs, String col) {
        try { return rs.getString(col); } catch (SQLException ignored) { return null; }
    }

    private static int getSafeInt(ResultSet rs, String col) {
        try { return rs.getInt(col); } catch (SQLException ignored) { return 0; }
    }

    private static Date getSafeDate(ResultSet rs, String col) {
        try { return rs.getDate(col); } catch (SQLException ignored) { return null; }
    }

    private static Timestamp getSafeTimestamp(ResultSet rs, String col) {
        try { return rs.getTimestamp(col); } catch (SQLException ignored) { return null; }
    }
}
