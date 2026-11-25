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
 * Data Access Object: Xử lý toàn bộ các thao tác truy vấn và cập nhật CSDL liên
 * quan đến Phim (Movies) và phân phối phim tới các cụm rạp (CentreMovies).
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
        String sql = "SELECT * FROM Movies WHERE Status = ? ORDER BY MovieID DESC, ReleaseDate DESC";
        List<Movie> movies = new ArrayList<>();

        try (Connection connection = DBContext.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
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
        String sql = "SELECT * FROM Movies WHERE MovieID = ?";

        try (Connection connection = DBContext.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
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
            ps.setString(1, title);
            ps.setInt(2, excludeMovieId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Lấy toàn bộ danh sách phim kèm số lượng rạp phân phối và tổng số rạp.
     *
     * @return danh sách tất cả các phim sắp xếp mới nhất lên đầu
     */
    public List<Movie> getAllMovies() {
        String sql = "SELECT m.*, "
                + "(SELECT COUNT(*) FROM Centres WHERE Status = 'ACTIVE') AS TotalCentres, "
                + "(SELECT COUNT(*) FROM CentreMovies cm "
                + " JOIN Centres c ON c.CentreID = cm.CentreID "
                + " WHERE cm.MovieID = m.MovieID AND cm.Status = 'ACTIVE' AND c.Status = 'ACTIVE') AS DistributedCentres "
                + "FROM Movies m ORDER BY m.MovieID DESC";
        List<Movie> movies = new ArrayList<>();
        try (Connection connection = DBContext.getConnection(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                movies.add(mapMovie(rs));
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return movies;
    }

    /**
     * Thêm mới một bộ phim vào cơ sở dữ liệu và trả về mã MovieID tự sinh
     * (IDENTITY).
     *
     * @param movie đối tượng thông tin phim cần thêm
     * @return mã định danh phim tự sinh nếu thành công (> 0), ngược lại 0
     */
    public int insertMovie(Movie movie) {
        String sql = "INSERT INTO Movies (Title, Description, Duration, Genre, Director, Cast, Language, "
                + "ReleaseDate, EndDate, PosterURL, TrailerURL, AgeRating, Status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
     * Cập nhật thông tin của một bộ phim. Sử dụng Transaction (ACID) để đảm bảo
     * an toàn dữ liệu: - Nếu phim chuyển sang ENDED hoặc ARCHIVED (kết thúc đợt
     * chiếu), tự động kích hoạt quy trình huỷ suất chiếu, vé, bắp nước và hoàn
     * trả lượt dùng voucher cho khách hàng. - Nếu bất kỳ thao tác nào lỗi, toàn
     * bộ giao dịch sẽ rollback để không phát sinh dữ liệu rác.
     *
     * @param movie đối tượng phim đã thay đổi thông tin
     * @return true nếu cập nhật thành công, ngược lại false
     */
    public boolean updateMovie(Movie movie) {
        String sql = "UPDATE Movies SET Title = ?, Description = ?, Duration = ?, Genre = ?, Director = ?, "
                + "Cast = ?, Language = ?, ReleaseDate = ?, EndDate = ?, PosterURL = ?, TrailerURL = ?, "
                + "AgeRating = ?, Status = ?, UpdatedAt = GETDATE() WHERE MovieID = ?";
        try (Connection connection = DBContext.getConnection()) {
            // Tắt auto-commit để bắt đầu Transaction thủ công
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                setMovieParams(ps, movie, true);
                int rows = ps.executeUpdate();
                if (rows == 0) {
                    connection.rollback();
                    return false;
                }

                // Nếu phim chuyển sang ENDED hoặc ARCHIVED: tự động huỷ các suất chiếu tương lai
                String status = movie.getStatus();
                if ("ENDED".equalsIgnoreCase(status) || "ARCHIVED".equalsIgnoreCase(status)) {
                    cancelUpcomingShowtimesForMovie(connection, movie.getMovieId());
                }

                // Xác nhận giao dịch thành công
                connection.commit();
                return true;
            } catch (Exception e) {
                // Có lỗi phát sinh -> Rollback toàn bộ dữ liệu về trạng thái trước đó
                connection.rollback();
                throw e;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }

    /**
     * Cập nhật nhanh trạng thái của phim (COMING_SOON, NOW_SHOWING, ENDED,
     * ARCHIVED). Tận dụng lại logic Transaction của updateMovie(): - Tự động
     * chốt ngày kết thúc (EndDate) là hôm nay nếu phim dừng chiếu. - Kích hoạt
     * huỷ suất chiếu và hoàn trả voucher tự động.
     *
     * @param movieId mã định danh phim
     * @param status trạng thái phát hành mới
     * @return true nếu thành công, ngược lại false
     */
    public boolean updateMovieStatus(int movieId, String status) {
        if (movieId <= 0 || status == null || status.isBlank()) {
            return false;
        }
        Movie movie = getMovieById(movieId);
        if (movie == null) {
            return false;
        }
        movie.setStatus(status.trim().toUpperCase());
        if ("ENDED".equalsIgnoreCase(status) || "ARCHIVED".equalsIgnoreCase(status)) {
            movie.setEndDate(java.sql.Date.valueOf(java.time.LocalDate.now()));
        }
        return updateMovie(movie);
    }

    /**
     * Bản ghi tạm lưu thông tin để gửi email thông báo hủy vé cho khách hàng.
     */
    private record NoticeInfo(
            String email, String fullName, String bookingCode,
            String movieTitle, String showtimeInfo, String reason) {

    }

    /**
     * Huỷ các suất chiếu chưa bắt đầu của phim khi kết thúc chiếu (ENDED hoặc
     * ARCHIVED). Được thực thi bên trong Transaction của updateMovie(): 1. Lấy
     * danh sách khách hàng có vé CONFIRMED bị ảnh hưởng để chuẩn bị gửi mail.
     * 2. Thực thi chuỗi lệnh UPDATE / DELETE đồng bộ theo đúng thứ tự ràng buộc
     * khóa ngoại (FK): - 2.1: Hoàn lại lượt dùng cho Voucher trong bảng
     * Vouchers (giảm UsedCount). - 2.2: Thu hồi bản ghi sử dụng voucher trong
     * bảng VoucherUsages (trả lại quyền dùng mã cho khách). - 2.3: Huỷ vé xem
     * phim (Tickets -> CANCELLED). - 2.4: Nhả ghế trên sơ đồ phòng chiếu
     * (BookingSeats -> CANCELLED). - 2.5: Huỷ giao dịch thanh toán chờ
     * (Payments -> CANCELLED). - 2.6: Huỷ đơn bắp nước đi kèm vé (FnbOrders ->
     * CANCELLED). - 2.7: Huỷ đơn đặt vé (Bookings -> CANCELLED). - 2.8: Huỷ
     * lịch chiếu (Showtimes -> CANCELLED). 3. Gửi email thông báo hủy vé đến
     * từng khách hàng.
     */
    private void cancelUpcomingShowtimesForMovie(Connection connection, int movieId) throws SQLException {
        // 1. Lấy thông tin khách hàng có vé bị ảnh hưởng để chuẩn bị gửi mail
        List<NoticeInfo> notices = new ArrayList<>();
        String sql = "SELECT u.Email, u.FullName, b.BookingCode, m.Title, "
                + "c.CentreName, h.HallName, s.StartTime "
                + "FROM Bookings b "
                + "JOIN Users u ON b.UserID = u.UserID "
                + "JOIN Showtimes s ON b.ShowtimeID = s.ShowtimeID "
                + "JOIN Movies m ON s.MovieID = m.MovieID "
                + "JOIN Halls h ON s.HallID = h.HallID "
                + "JOIN Centres c ON h.CentreID = c.CentreID "
                + "WHERE b.BookingStatus IN ('CONFIRMED') "
                + "AND s.MovieID = ? AND s.Status = 'SCHEDULED' AND s.StartTime > SYSDATETIME()";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
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

        // 2. Hủy toàn bộ dữ liệu liên quan đến các suất chiếu tương lai
        String condition = " IN (SELECT ShowtimeID FROM Showtimes "
                + "WHERE MovieID = ? AND Status = 'SCHEDULED' AND StartTime > SYSDATETIME())";

        String[] sqls = {
            // 2.1 Hoàn lại số lượt đã dùng cho Voucher (giảm UsedCount tương ứng số đơn bị huỷ)
            "UPDATE v SET v.UsedCount = CASE WHEN v.UsedCount > sub.cnt THEN v.UsedCount - sub.cnt ELSE 0 END FROM Vouchers v JOIN (SELECT b.VoucherID, COUNT(*) AS cnt FROM Bookings b WHERE b.VoucherID IS NOT NULL AND b.BookingStatus IN ('PENDING', 'CONFIRMED') AND b.ShowtimeID " + condition + " GROUP BY b.VoucherID) sub ON v.VoucherID = sub.VoucherID",
            // 2.2 Thu hồi bản ghi sử dụng voucher để khách hàng có thể dùng lại mã cho lần đặt sau
            "DELETE FROM VoucherUsages WHERE BookingID IN (SELECT BookingID FROM Bookings WHERE ShowtimeID " + condition + ")",
            // 2.3 Chuyển trạng thái vé thành CANCELLED
            "UPDATE Tickets SET TicketStatus = 'CANCELLED' WHERE TicketStatus = 'VALID' AND BookingSeatID IN (SELECT BookingSeatID FROM BookingSeats WHERE ShowtimeID " + condition + ")",
            // 2.4 Nhả ghế phòng chiếu (CANCELLED) để không bị chiếm chỗ
            "UPDATE BookingSeats SET SeatStatus = 'CANCELLED' WHERE SeatStatus IN ('HELD', 'CONFIRMED') AND ShowtimeID " + condition,
            // 2.5 Hủy giao dịch thanh toán đang ở trạng thái PENDING
            "UPDATE Payments SET PaymentStatus = 'CANCELLED' WHERE PaymentStatus = 'PENDING' AND BookingID IN (SELECT BookingID FROM Bookings WHERE ShowtimeID " + condition + ")",
            // 2.6 Hủy đơn bắp nước đi kèm đơn đặt vé
            "UPDATE FnbOrders SET OrderStatus = 'CANCELLED', UpdatedAt = SYSDATETIME() WHERE OrderStatus <> 'CANCELLED' AND BookingID IN (SELECT BookingID FROM Bookings WHERE ShowtimeID " + condition + ")",
            // 2.7 Hủy đơn đặt vé chính
            "UPDATE Bookings SET BookingStatus = 'CANCELLED', UpdatedAt = SYSDATETIME() WHERE BookingStatus IN ('PENDING', 'CONFIRMED') AND ShowtimeID " + condition,
            // 2.8 Cuối cùng, hủy lịch chiếu của suất tương lai
            "UPDATE Showtimes SET Status = 'CANCELLED', UpdatedAt = SYSDATETIME() WHERE ShowtimeID " + condition
        };

        // Thực thi lần lượt từng câu lệnh UPDATE/DELETE trong cùng kết nối Transaction
        for (String statement : sqls) {
            try (PreparedStatement ps = connection.prepareStatement(statement)) {
                ps.setInt(1, movieId);
                ps.executeUpdate();
            }
        }

        // 3. Gửi email thông báo hủy vé cho khách hàng
        for (NoticeInfo notice : notices) {
            EmailUtil.sendTicketCancellationEmail(
                    notice.email(), notice.fullName(), notice.bookingCode(),
                    notice.movieTitle(), notice.showtimeInfo(), notice.reason()
            );
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
        String sql = "SELECT "
                + "(SELECT COUNT(*) "
                + "FROM Showtimes "
                + "WHERE MovieID = ? "
                + "AND Status = 'SCHEDULED' "
                + "AND StartTime > SYSDATETIME()) AS UpcomingShowtimes, "
                + "(SELECT COUNT(t.TicketID) "
                + "FROM Tickets t "
                + "JOIN BookingSeats bs ON t.BookingSeatID = bs.BookingSeatID "
                + "JOIN Showtimes s ON bs.ShowtimeID = s.ShowtimeID "
                + "WHERE s.MovieID = ? "
                + "AND s.Status = 'SCHEDULED' "
                + "AND s.StartTime > SYSDATETIME() "
                + "AND t.TicketStatus <> 'CANCELLED') AS SoldTickets";

        try (Connection con = DBContext.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
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
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
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
     * Lấy danh sách phim đang có ít nhất một suất chiếu mở bán tại một cụm rạp
     * cụ thể (phục vụ luồng đặt vé).
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
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
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

        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
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
     * Lấy danh sách các trung tâm kèm trạng thái đã được phân phối một bộ phim
     * cụ thể hay chưa.
     *
     * @param movieId mã định danh phim
     * @return danh sách trung tâm kèm cờ isDistributed (true nếu rạp được quyền
     * chiếu phim)
     */
    public List<Centre> getDistributionCentres(int movieId) {
        // Câu SQL rút gọn: Nếu tìm thấy thì là 1, không thấy (NULL) thì là 0
        String sql = "SELECT b.CentreID, b.CentreName, b.Address, b.City, b.Phone, b.Status, "
                + "       (CASE WHEN bm.CentreID IS NOT NULL THEN 1 ELSE 0 END) AS Distributed "
                + "FROM Centres b "
                + "LEFT JOIN CentreMovies bm ON bm.CentreID = b.CentreID "
                + "                          AND bm.MovieID = ? "
                + "                          AND bm.Status IN ('ACTIVE', 'SUSPENDED') "
                + "ORDER BY b.City, b.CentreName";
        List<Centre> centres = new ArrayList<>();
        try (Connection conn = DBContext.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Centre c = new Centre();
                    c.setCentreId(rs.getInt("CentreID"));
                    c.setCentreName(rs.getString("CentreName"));
                    c.setAddress(rs.getString("Address"));
                    c.setCity(rs.getString("City"));
                    c.setPhone(rs.getString("Phone"));
                    c.setStatus(rs.getString("Status"));
                    c.setDistributed(rs.getInt("Distributed") == 1); // 1 là true, 0 là false
                    centres.add(c);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
     * Thiết lập các tham số vào PreparedStatement cho thao tác tạo hoặc cập
     * nhật phim.
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
     * Chuyển đổi dữ liệu từ dòng ResultSet sang đối tượng Movie với đầy đủ tất
     * cả các trường trong SQL.
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
        try {
            return rs.getString(col);
        } catch (SQLException ignored) {
            return null;
        }
    }

    private static int getSafeInt(ResultSet rs, String col) {
        try {
            return rs.getInt(col);
        } catch (SQLException ignored) {
            return 0;
        }
    }

    private static Date getSafeDate(ResultSet rs, String col) {
        try {
            return rs.getDate(col);
        } catch (SQLException ignored) {
            return null;
        }
    }

    private static Timestamp getSafeTimestamp(ResultSet rs, String col) {
        try {
            return rs.getTimestamp(col);
        } catch (SQLException ignored) {
            return null;
        }
    }
}
