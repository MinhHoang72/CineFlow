package dao;
import java.sql.*;
import java.time.LocalDateTime;
import model.entity.SeatAvailability;

import model.entity.Centre;
import model.entity.Showtime;
import utils.DBContext;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ShowtimeDAO {

    /** Advisory conflict preview, using the same half-open intervals as the create operation. */
    public List<Showtime> findScheduleConflicts(int centreId, int hallId,
            LocalDateTime start, LocalDateTime end, int cleaningBuffer) throws SQLException {
        try (Connection connection = DBContext.getConnection()) {
            return findScheduleConflicts(connection, centreId, hallId, start, end, cleaningBuffer);
        }
    }

    public List<Showtime> findScheduleConflicts(Connection connection, int centreId, int hallId,
            LocalDateTime start, LocalDateTime end, int cleaningBuffer) throws SQLException {
        String sql = "SELECT s.*, m.Title AS MovieTitle, h.HallName, h.HallType "
                + "FROM Showtimes s JOIN Halls h ON h.HallID = s.HallID "
                + "JOIN Movies m ON m.MovieID = s.MovieID "
                + "WHERE h.CentreID = ? AND s.HallID = ? AND s.Status <> 'CANCELLED' "
                + "AND s.StartTime < DATEADD(MINUTE, ?, ?) "
                + "AND DATEADD(MINUTE, s.CleaningBufferMinutes, s.EndTime) > ? "
                + "ORDER BY s.StartTime";
        List<Showtime> conflicts = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, centreId);
            ps.setInt(2, hallId);
            ps.setInt(3, cleaningBuffer);
            ps.setTimestamp(4, Timestamp.valueOf(end));
            ps.setTimestamp(5, Timestamp.valueOf(start));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) conflicts.add(mapShowtime(rs));
            }
        }
        return conflicts;
    }

    /** Use the database clock so every application instance uses the same time. */
    public int synchronizeStatuses() throws SQLException {
        try (Connection connection = DBContext.getConnection()) {
            return synchronizeStatuses(connection);
        }
    }

    public int synchronizeStatuses(Connection connection) throws SQLException {
        String sql = "DECLARE @Now DATETIME2 = SYSDATETIME(); "
                + "UPDATE Showtimes SET Status = CASE WHEN EndTime <= @Now "
                + "THEN 'COMPLETED' ELSE 'ONGOING' END, UpdatedAt = @Now "
                + "WHERE Status IN ('SCHEDULED', 'ONGOING') AND StartTime <= @Now "
                + "AND Status <> CASE WHEN EndTime <= @Now THEN 'COMPLETED' ELSE 'ONGOING' END";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            return ps.executeUpdate();
        }
    }

    /** Gia goc cua rap (CentrePricing) dang hieu luc tai gio chieu; NULL neu rap chua co bang gia. */
    private static final String CENTRE_BASE_PRICE =
            "(SELECT TOP 1 bp.BasePrice FROM CentrePricing bp "
          + " WHERE bp.CentreID = b.CentreID AND bp.EffectiveFrom <= s.StartTime "
          + "   AND (bp.EffectiveTo IS NULL OR bp.EffectiveTo > s.StartTime) "
          + " ORDER BY bp.EffectiveFrom DESC)";

    /**
     * Gia ve ghe thuong cua suat = gia goc rap + phu thu khung gio (TimeSlotPricing) + phu thu phong (Surcharges HALL).
     * Phu thu loai ghe (Surcharges SEAT) duoc cong them trong SeatDAO.
     * Moc gio trung giua 2 khung (vd 12:00) tinh vao khung bat dau muon hon.
     */
    private static final String TICKET_PRICE =
            CENTRE_BASE_PRICE
          + " + ISNULL((SELECT TOP 1 tp.Surcharge FROM TimeSlotPricing tp "
          + "     WHERE tp.CentreID = b.CentreID "
          + "       AND CAST(s.StartTime AS TIME) BETWEEN tp.StartTime AND tp.EndTime "
          + "       AND tp.EffectiveFrom <= s.StartTime AND (tp.EffectiveTo IS NULL OR tp.EffectiveTo > s.StartTime) "
          + "     ORDER BY tp.StartTime DESC), 0)"
          + " + ISNULL((SELECT SUM(sc.Amount) FROM Surcharges sc "
          + "     WHERE sc.SurchargeType = 'HALL' AND sc.TargetValue = h.HallType "
          + "       AND sc.EffectiveFrom <= s.StartTime AND (sc.EffectiveTo IS NULL OR sc.EffectiveTo > s.StartTime)), 0)";

    static final String SELECT =
            "SELECT s.ShowtimeID, s.MovieID, m.Title, b.CentreID, b.CentreName, h.HallID, h.HallName, "
          + "       h.HallType, s.StartTime, s.EndTime, " + TICKET_PRICE + " AS TicketPrice "
          + "FROM Showtimes s "
          + "JOIN Movies m ON m.MovieID = s.MovieID "
          + "JOIN Halls h ON h.HallID = s.HallID "
          + "JOIN Centres b ON b.CentreID = h.CentreID ";

    /** Suat con mo ban: chua chieu, phim dang chieu, phong va rap hoat dong, rap da co bang gia. */
    private static final String BOOKABLE =
            "s.Status = 'SCHEDULED' AND s.StartTime > SYSDATETIME() AND m.Status = 'NOW_SHOWING' "
          + "AND h.Status = 'AVAILABLE' AND b.Status = 'ACTIVE' "
          + "AND " + CENTRE_BASE_PRICE + " IS NOT NULL ";

    static Showtime map(ResultSet rs) throws SQLException {
        return new Showtime(rs.getInt("ShowtimeID"), rs.getInt("MovieID"), rs.getString("Title"),
                rs.getInt("CentreID"), rs.getString("CentreName"), rs.getInt("HallID"),
                rs.getString("HallName"), rs.getString("HallType"), rs.getTimestamp("StartTime"),
                rs.getTimestamp("EndTime"), rs.getBigDecimal("TicketPrice"));
    }

    /** Cac ngay con suat chieu cua phim tai chi nhanh. */
    public List<LocalDate> getShowDates(int centreId, int movieId) throws SQLException {
        String sql = "SELECT DISTINCT CAST(s.StartTime AS DATE) AS ShowDate "
                   + "FROM Showtimes s JOIN Movies m ON m.MovieID = s.MovieID "
                   + "JOIN Halls h ON h.HallID = s.HallID JOIN Centres b ON b.CentreID = h.CentreID "
                   + "WHERE " + BOOKABLE + "AND b.CentreID = ? AND s.MovieID = ? ORDER BY ShowDate";
        List<LocalDate> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, centreId);
            ps.setInt(2, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getDate("ShowDate").toLocalDate());
            }
        }
        return list;
    }

    public List<Showtime> getShowtimes(int centreId, int movieId, LocalDate date) throws SQLException {
        String sql = SELECT + "WHERE " + BOOKABLE
                   + "AND b.CentreID = ? AND s.MovieID = ? "
                   + "AND s.StartTime >= ? AND s.StartTime < ? ORDER BY s.StartTime";
        List<Showtime> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, centreId);
            ps.setInt(2, movieId);
            ps.setDate(3, Date.valueOf(date));
            ps.setDate(4, Date.valueOf(date.plusDays(1)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /** Cac ngay con suat chieu tai chi nhanh (ban tai quay). */
    public List<LocalDate> getShowDatesAtCentre(int centreId) throws SQLException {
        String sql = "SELECT DISTINCT CAST(s.StartTime AS DATE) AS ShowDate "
                   + "FROM Showtimes s JOIN Movies m ON m.MovieID = s.MovieID "
                   + "JOIN Halls h ON h.HallID = s.HallID JOIN Centres b ON b.CentreID = h.CentreID "
                   + "WHERE " + BOOKABLE + "AND b.CentreID = ? ORDER BY ShowDate";
        List<LocalDate> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, centreId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getDate("ShowDate").toLocalDate());
            }
        }
        return list;
    }

    /** Tat ca suat con mo ban cua chi nhanh trong ngay, sap theo phim roi gio chieu. */
    public List<Showtime> getShowtimesAtCentre(int centreId, LocalDate date) throws SQLException {
        String sql = SELECT + "WHERE " + BOOKABLE
                   + "AND b.CentreID = ? AND s.StartTime >= ? AND s.StartTime < ? ORDER BY m.Title, s.StartTime";
        List<Showtime> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, centreId);
            ps.setDate(2, Date.valueOf(date));
            ps.setDate(3, Date.valueOf(date.plusDays(1)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /** Thanh pho cua rap; rap chua nhap thanh pho duoc xep vao nhom nay. */
    public static final String OTHER_CITY = "Khác";
    private static final String CITY = "ISNULL(NULLIF(LTRIM(RTRIM(b.City)), ''), N'" + OTHER_CITY + "')";

    static final String BOOKABLE_FROM =
            "FROM Showtimes s JOIN Movies m ON m.MovieID = s.MovieID "
          + "JOIN Halls h ON h.HallID = s.HallID JOIN Centres b ON b.CentreID = h.CentreID "
          + "WHERE " + BOOKABLE;

    /** Thanh pho dang co suat mo ban va so rap co suat. */
    public record CityOption(String name, int centreCount) {}

    /** Buoc 1: thanh pho con suat mo ban. @param movieId != null: chi thanh pho co chieu phim nay */
    public List<CityOption> getCities(Integer movieId) throws SQLException {
        String sql = "SELECT " + CITY + " AS City, COUNT(DISTINCT b.CentreID) AS Centres " + BOOKABLE_FROM
                   + "AND (? IS NULL OR s.MovieID = ?) GROUP BY " + CITY + " ORDER BY City";
        List<CityOption> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setNullableInt(ps, 1, movieId);
            setNullableInt(ps, 2, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new CityOption(rs.getString("City"), rs.getInt("Centres")));
            }
        }
        return list;
    }

    /** Buoc 2: rap trong thanh pho con suat mo ban. @param movieId != null: chi rap co chieu phim nay */
    public List<Centre> getCentresInCity(String city, Integer movieId) throws SQLException {
        String sql = "SELECT DISTINCT b.CentreID, b.CentreName, b.Address, " + CITY + " AS City " + BOOKABLE_FROM
                   + "AND " + CITY + " = ? AND (? IS NULL OR s.MovieID = ?) ORDER BY b.CentreName";
        List<Centre> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, city);
            setNullableInt(ps, 2, movieId);
            setNullableInt(ps, 3, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Centre centre = new Centre();
                    centre.setCentreId(rs.getInt("CentreID"));
                    centre.setCentreName(rs.getString("CentreName"));
                    centre.setAddress(rs.getString("Address"));
                    centre.setCity(rs.getString("City"));
                    list.add(centre);
                }
            }
        }
        return list;
    }

    /** Thanh pho cua rap (theo cach nhom o getCities), null neu khong co rap. */
    public String getCityOfCentre(int centreId) throws SQLException {
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT " + CITY + " FROM Centres b WHERE b.CentreID = ?")) {
            ps.setInt(1, centreId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /** Rap co suat chieu som nhat con mo ban cua phim, null neu phim khong con suat nao. */
    public Integer findFirstCentreForMovie(int movieId) throws SQLException {
        String sql = "SELECT TOP 1 b.CentreID " + BOOKABLE_FROM + "AND s.MovieID = ? ORDER BY s.StartTime";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    private static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) ps.setNull(index, Types.INTEGER);
        else ps.setInt(index, value);
    }

    /** Suat chieu con mo ban, null neu khong ton tai hoac da qua gio. */
    public Showtime getBookableById(int showtimeId) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            return getBookableById(conn, showtimeId);
        }
    }

    static Showtime getBookableById(Connection conn, int showtimeId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE " + BOOKABLE + "AND s.ShowtimeID = ?")) {
            ps.setInt(1, showtimeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }


    /* =====================================================
       1. CHECK HALL AVAILABLE
       ===================================================== */
    public boolean isHallAvailable(int hallId) {

        String sql
                = "SELECT 1 "
                + "FROM Halls "
                + "WHERE HallID = ? "
                + "AND Status = 'AVAILABLE'";

        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, hallId);

            ResultSet rs = ps.executeQuery();

            return rs.next();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }


    /* =====================================================
       2. CHECK MOVIE IS DISTRIBUTED TO HALL'S CENTRE
       ===================================================== */
    public boolean isMovieAvailableForHall(
            int movieId,
            int hallId) {

        String sql
                = "SELECT 1 "
                + "FROM Halls h "
                + "JOIN CentreMovies bm "
                + "ON bm.CentreID = h.CentreID "
                + "WHERE h.HallID = ? "
                + "AND bm.MovieID = ? "
                + "AND bm.IsActive = 1";

        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, hallId);
            ps.setInt(2, movieId);

            ResultSet rs = ps.executeQuery();

            return rs.next();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }


    /* =====================================================
       3. CHECK SCHEDULE CONFLICT
       ===================================================== */
    public boolean hasScheduleConflict(
            int hallId,
            LocalDateTime newStart,
            LocalDateTime newEnd,
            int newBufferMinutes,
            Integer excludeShowtimeId) {

        String sql
                = "SELECT COUNT(*) AS Total "
                + "FROM Showtimes "
                + "WHERE HallID = ? "
                + "AND Status <> 'CANCELLED' "
                + "AND StartTime < DATEADD(MINUTE, ?, ?) "
                + "AND DATEADD(MINUTE, CleaningBufferMinutes, EndTime) > ? ";

        if (excludeShowtimeId != null) {
            sql += "AND ShowtimeID <> ?";
        }

        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, hallId);

            ps.setInt(
                    2,
                    newBufferMinutes
            );

            ps.setTimestamp(
                    3,
                    Timestamp.valueOf(newEnd)
            );

            ps.setTimestamp(
                    4,
                    Timestamp.valueOf(newStart)
            );

            if (excludeShowtimeId != null) {
                ps.setInt(
                        5,
                        excludeShowtimeId
                );
            }

            ResultSet rs
                    = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("Total") > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }


    /* =====================================================
       4. INSERT SHOWTIME
       ===================================================== */
    public boolean insertShowtime(
            Showtime showtime) {

        String sql
                = "INSERT INTO Showtimes "
                + "(MovieID, HallID, "
                + "StartTime, EndTime, "
                + "CleaningBufferMinutes, "
                + "Status, CreatedBy) "
                + "VALUES (?, ?, ?, ?, ?, 'SCHEDULED', ?)";

        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(
                    1,
                    showtime.getMovieId()
            );

            ps.setInt(
                    2,
                    showtime.getHallId()
            );

            ps.setTimestamp(
                    3,
                    showtime.getStartTime()
            );

            ps.setTimestamp(
                    4,
                    showtime.getEndTime()
            );

            ps.setInt(
                    5,
                    showtime.getCleaningBufferMinutes()
            );

            if (showtime.getCreatedBy() != null) {
                ps.setInt(
                        6,
                        showtime.getCreatedBy()
                );
            } else {
                ps.setNull(
                        6,
                        Types.INTEGER
                );
            }

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }


    /* =====================================================
       5. GET SHOWTIMES BY CENTRE
       ===================================================== */
    public List<Showtime> getByCentre(
            int centreId) {

        List<Showtime> list
                = new ArrayList<>();

        String sql
                = "SELECT s.*, m.Title AS MovieTitle, h.HallName, h.HallType "
                + "FROM Showtimes s "
                + "JOIN Halls h "
                + "ON h.HallID = s.HallID "
                + "JOIN Movies m "
                + "ON m.MovieID = s.MovieID "
                + "WHERE h.CentreID = ? "
                + "ORDER BY s.StartTime";

        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, centreId);

            ResultSet rs
                    = ps.executeQuery();

            while (rs.next()) {

                Showtime s = mapShowtime(rs);

                s.setMovieTitle(
                        rs.getString("MovieTitle")
                );

                list.add(s);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }


    /* =====================================================
       6. GET SHOWTIME BY ID
       ===================================================== */
    public Showtime getById(
            int showtimeId) {

        String sql
                = "SELECT s.*, m.Title AS MovieTitle, h.HallName, h.HallType "
                + "FROM Showtimes s JOIN Halls h ON h.HallID = s.HallID "
                + "JOIN Movies m ON m.MovieID = s.MovieID WHERE s.ShowtimeID = ?";

        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(
                    1,
                    showtimeId
            );

            ResultSet rs
                    = ps.executeQuery();

            if (rs.next()) {
                return mapShowtime(rs);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }


    /* =====================================================
       7. UPDATE SHOWTIME
       ===================================================== */
    public boolean updateShowtime(
            Showtime showtime) {

        String sql
                = "UPDATE Showtimes "
                + "SET MovieID = ?, "
                + "HallID = ?, "
                + "StartTime = ?, "
                + "EndTime = ?, "
                + "CleaningBufferMinutes = ?, "
                + "UpdatedAt = GETDATE() "
                + "WHERE ShowtimeID = ? "
                + "AND Status = 'SCHEDULED' AND StartTime > SYSDATETIME()";

        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(
                    1,
                    showtime.getMovieId()
            );

            ps.setInt(
                    2,
                    showtime.getHallId()
            );

            ps.setTimestamp(
                    3,
                    showtime.getStartTime()
            );

            ps.setTimestamp(
                    4,
                    showtime.getEndTime()
            );

            ps.setInt(
                    5,
                    showtime.getCleaningBufferMinutes()
            );

            ps.setInt(
                    6,
                    showtime.getShowtimeId()
            );

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }


    /* =====================================================
       8. CHECK ACTIVE BOOKINGS
       ===================================================== */
    public boolean hasActiveBookings(
            int showtimeId) {

        String sql
                = "SELECT COUNT(*) AS Total "
                + "FROM Bookings "
                + "WHERE ShowtimeID = ? "
                + "AND BookingStatus IN "
                + "('PENDING','CONFIRMED')";

        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(
                    1,
                    showtimeId
            );

            ResultSet rs
                    = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("Total") > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }


    /* =====================================================
       9. CANCEL SHOWTIME
       ===================================================== */
    public boolean cancelShowtime(
            int showtimeId) {

        String sql
                = "UPDATE Showtimes "
                + "SET Status = 'CANCELLED', "
                + "UpdatedAt = GETDATE() "
                + "WHERE ShowtimeID = ? "
                + "AND Status = 'SCHEDULED' AND StartTime > SYSDATETIME()";

        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(
                    1,
                    showtimeId
            );

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }


    /* =====================================================
       10. RESULTSET -> SHOWTIME
       ===================================================== */
    private Showtime mapShowtime(
            ResultSet rs)
            throws SQLException {

        Showtime s
                = new Showtime();

        s.setShowtimeId(
                rs.getInt(
                        "ShowtimeID"
                )
        );

        s.setMovieId(
                rs.getInt(
                        "MovieID"
                )
        );

        s.setHallId(
                rs.getInt(
                        "HallID"
                )
        );

        Timestamp start
                = rs.getTimestamp(
                        "StartTime"
                );

        if (start != null) {
            s.setStartTime(
                    start.toLocalDateTime()
            );
        }

        Timestamp end
                = rs.getTimestamp(
                        "EndTime"
                );

        if (end != null) {
            s.setEndTime(
                    end.toLocalDateTime()
            );
        }

        s.setCleaningBufferMinutes(
                rs.getInt(
                        "CleaningBufferMinutes"
                )
        );

        s.setStatus(
                rs.getString(
                        "Status"
                )
        );

        int createdBy
                = rs.getInt(
                        "CreatedBy"
                );

        if (!rs.wasNull()) {
            s.setCreatedBy(createdBy);
        }

        Timestamp createdAt
                = rs.getTimestamp(
                        "CreatedAt"
                );

        if (createdAt != null) {
            s.setCreatedAt(
                    createdAt.toLocalDateTime()
            );
        }

        Timestamp updatedAt
                = rs.getTimestamp(
                        "UpdatedAt"
                );

        if (updatedAt != null) {
            s.setUpdatedAt(
                    updatedAt.toLocalDateTime()
            );
        }

        s.setHallName(rs.getString("HallName"));
        s.setHallType(rs.getString("HallType"));
        s.setMovieTitle(rs.getString("MovieTitle"));
        return s;
    }

}
