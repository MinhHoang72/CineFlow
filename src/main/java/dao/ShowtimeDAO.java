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

    /** The parent showtime lock is also taken by BookingService before it inserts a booking. */
    private void lockEditable(Connection c,int centre,int id) throws SQLException {
        try(PreparedStatement ps=c.prepareStatement("SELECT s.Status,s.SaleState,s.StartTime FROM Showtimes s WITH(UPDLOCK,HOLDLOCK) JOIN Halls h ON h.HallID=s.HallID WHERE s.ShowtimeID=? AND h.CentreID=?")){
            ps.setInt(1,id);ps.setInt(2,centre);
            try(ResultSet rs=ps.executeQuery()){
                if(!rs.next()||!"SCHEDULED".equals(rs.getString(1)))throw new IllegalArgumentException("Suất chiếu không thuộc rạp của bạn hoặc đã chiếu/đã hủy.");
            }
        }
        try(PreparedStatement ps=c.prepareStatement("SELECT 1 FROM Bookings bk WITH(UPDLOCK,HOLDLOCK) WHERE bk.ShowtimeID=? AND (bk.BookingStatus='CONFIRMED' OR (bk.BookingStatus='PENDING' AND EXISTS(SELECT 1 FROM BookingSeats bs WHERE bs.BookingID=bk.BookingID AND bs.SeatStatus='HELD' AND bs.HoldUntil>SYSDATETIME())))")){
            ps.setInt(1,id);try(ResultSet rs=ps.executeQuery()){if(rs.next())throw new IllegalArgumentException("Suất chiếu đang có vé đã thanh toán hoặc ghế đang giữ: không thể sửa hoặc hủy.");}
        }
        try(PreparedStatement ps=c.prepareStatement("SELECT 1 FROM Showtimes WHERE ShowtimeID=? AND StartTime>SYSDATETIME()")){
            ps.setInt(1,id);try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new IllegalArgumentException("Suất chiếu đã đến giờ bắt đầu.");}
        }
    }

    public void saveShowtime(int centre,int user,Integer id,int movie,int hall,LocalDateTime start) throws SQLException {
        try(Connection c=DBContext.getConnection()){
            c.setAutoCommit(false);
            try{
                if(id!=null){
                    lockEditable(c,centre,id);
                    // Expired/cancelled orders cannot later confirm against a changed schedule.
                    try(PreparedStatement ps=c.prepareStatement("UPDATE Bookings SET BookingStatus='CANCELLED',UpdatedAt=SYSDATETIME() WHERE ShowtimeID=? AND BookingStatus<>'CONFIRMED'")){ps.setInt(1,id);ps.executeUpdate();}
                    try(PreparedStatement ps=c.prepareStatement("UPDATE BookingSeats SET SeatStatus='CANCELLED' WHERE ShowtimeID=? AND SeatStatus='HELD'")){ps.setInt(1,id);ps.executeUpdate();}
                }
                // Lock the destination hall to serialize concurrent schedule creation and edits.
                try(PreparedStatement ps=c.prepareStatement("SELECT HallID FROM Halls WITH(UPDLOCK,HOLDLOCK) WHERE HallID=? AND CentreID=? AND Status='AVAILABLE'")){
                    ps.setInt(1,hall);ps.setInt(2,centre);try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new IllegalArgumentException("Phòng không khả dụng tại cụm rạp của bạn.");}
                }
                int duration;
                try(PreparedStatement ps=c.prepareStatement("SELECT m.Duration FROM Movies m JOIN CentreMovies cm ON cm.MovieID=m.MovieID WHERE m.MovieID=? AND cm.CentreID=? AND cm.IsActive=1")){
                    ps.setInt(1,movie);ps.setInt(2,centre);try(ResultSet rs=ps.executeQuery()){if(!rs.next()||(duration=rs.getInt(1))<=0)throw new IllegalArgumentException("Phim chưa được phân phối hoặc thời lượng không hợp lệ.");}
                }
                LocalDateTime end=start.plusMinutes(duration);
                try(PreparedStatement ps=c.prepareStatement("SELECT 1 WHERE ?>SYSDATETIME()")){
                    ps.setTimestamp(1,Timestamp.valueOf(start));try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new IllegalArgumentException("Giờ bắt đầu phải nằm trong tương lai.");}
                }
                try(PreparedStatement ps=c.prepareStatement("SELECT 1 FROM Showtimes WHERE HallID=? AND Status<>'CANCELLED' AND ShowtimeID<>? AND StartTime<DATEADD(MINUTE,15,?) AND DATEADD(MINUTE,CleaningBufferMinutes,EndTime)>?")){
                    ps.setInt(1,hall);ps.setInt(2,id==null?-1:id);ps.setTimestamp(3,Timestamp.valueOf(end));ps.setTimestamp(4,Timestamp.valueOf(start));
                    try(ResultSet rs=ps.executeQuery()){if(rs.next())throw new IllegalArgumentException("Trùng lịch chiếu hoặc thời gian dọn phòng.");}
                }
                String sql=id==null?"INSERT Showtimes(MovieID,HallID,StartTime,EndTime,CleaningBufferMinutes,Status,CreatedBy,SaleState) VALUES(?,?,?,?,15,'SCHEDULED',?,'DRAFT')":"UPDATE Showtimes SET MovieID=?,HallID=?,StartTime=?,EndTime=?,UpdatedAt=SYSDATETIME() WHERE ShowtimeID=?";
                try(PreparedStatement ps=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){
                    ps.setInt(1,movie);ps.setInt(2,hall);ps.setTimestamp(3,Timestamp.valueOf(start));ps.setTimestamp(4,Timestamp.valueOf(end));ps.setInt(5,id==null?user:id);
                    if(ps.executeUpdate()!=1)throw new SQLException("Không lưu được suất chiếu.");
                    if(id==null)try(ResultSet keys=ps.getGeneratedKeys()){if(!keys.next())throw new SQLException("Không lấy được mã suất chiếu.");id=keys.getInt(1);}
                }
                // Rebuild the snapshot atomically while the parent row blocks new bookings.
                try(PreparedStatement ps=c.prepareStatement("UPDATE Showtimes SET SaleState='DRAFT',PriceLockedAt=NULL,SaleOpensAt=NULL WHERE ShowtimeID=?")){ps.setInt(1,id);ps.executeUpdate();}
                try(PreparedStatement ps=c.prepareStatement("EXEC dbo.CfOpenShowtime ?")){ps.setInt(1,id);ps.execute();}
                c.commit();
            }catch(Exception e){if(!c.isClosed())c.rollback();throw e;}
        }
    }

    public void cancelShowtime(int centre,int id) throws SQLException {
        try(Connection c=DBContext.getConnection()){
            c.setAutoCommit(false);
            try{
                lockEditable(c,centre,id);
                try(PreparedStatement ps=c.prepareStatement("UPDATE Showtimes SET Status='CANCELLED',UpdatedAt=SYSDATETIME() WHERE ShowtimeID=?")){ps.setInt(1,id);ps.executeUpdate();}
                c.commit();
            }catch(Exception e){if(!c.isClosed())c.rollback();throw e;}
        }
    }

    /** Advisory conflict preview, using the same half-open intervals as the create operation. */
    public List<Showtime> findScheduleConflicts(int centreId, int hallId,
            LocalDateTime start, LocalDateTime end, int cleaningBuffer) throws SQLException {
        try (Connection connection = DBContext.getConnection()) {
            return findScheduleConflicts(connection, centreId, hallId, start, end, cleaningBuffer);
        }
    }

    public List<Showtime> findScheduleConflicts(Connection connection, int centreId, int hallId,
            LocalDateTime start, LocalDateTime end, int cleaningBuffer) throws SQLException {
        String sql = "SELECT s.*, m.Title AS MovieTitle, h.HallName, h.HallType, h.CentreID, CASE WHEN EXISTS(SELECT 1 FROM Bookings bk WHERE bk.ShowtimeID=s.ShowtimeID AND (bk.BookingStatus='CONFIRMED' OR (bk.BookingStatus='PENDING' AND EXISTS(SELECT 1 FROM BookingSeats bs WHERE bs.BookingID=bk.BookingID AND bs.SeatStatus='HELD' AND bs.HoldUntil>SYSDATETIME())))) THEN 1 ELSE 0 END HasBookings "
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
                + "WHERE (SaleState='OPEN' OR EndTime<=@Now) AND Status IN ('SCHEDULED', 'ONGOING') AND StartTime <= @Now "
                + "AND Status <> CASE WHEN EndTime <= @Now THEN 'COMPLETED' ELSE 'ONGOING' END";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            return ps.executeUpdate();
        }
    }

    private static final String CENTRE_BASE_PRICE="(SELECT Total FROM ShowtimeEffectivePrices p WHERE p.ShowtimeID=s.ShowtimeID AND p.SeatType='STANDARD')";
    private static final String TICKET_PRICE=CENTRE_BASE_PRICE;

    static final String SELECT =
            "SELECT s.ShowtimeID, s.MovieID, m.Title, b.CentreID, b.CentreName, h.HallID, h.HallName, "
          + "       h.HallType, s.StartTime, s.EndTime, " + TICKET_PRICE + " AS TicketPrice "
          + "FROM Showtimes s "
          + "JOIN Movies m ON m.MovieID = s.MovieID "
          + "JOIN Halls h ON h.HallID = s.HallID "
          + "JOIN Centres b ON b.CentreID = h.CentreID ";

    /** Suat con mo ban: chua chieu, phim dang chieu, phong va rap hoat dong, rap da co bang gia. */
    private static final String BOOKABLE =
            "s.SaleState = 'OPEN' AND s.PriceLockedAt IS NOT NULL AND s.Status = 'SCHEDULED' AND s.StartTime > SYSDATETIME() AND m.Status = 'NOW_SHOWING' "
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

    public Showtime getBookableById(Connection conn, int showtimeId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT.replace("FROM Showtimes s ","FROM Showtimes s WITH (UPDLOCK,HOLDLOCK) ") + "WHERE " + BOOKABLE + "AND s.ShowtimeID = ?")) {
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
                + "FROM Halls h "
                + "JOIN Centres c ON c.CentreID = h.CentreID "
                + "WHERE h.HallID = ? "
                + "AND h.Status = 'AVAILABLE' "
                + "AND c.Status = 'ACTIVE'";

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
                = "SELECT s.*, m.Title AS MovieTitle, h.HallName, h.HallType, h.CentreID, CASE WHEN EXISTS(SELECT 1 FROM Bookings bk WHERE bk.ShowtimeID=s.ShowtimeID AND (bk.BookingStatus='CONFIRMED' OR (bk.BookingStatus='PENDING' AND EXISTS(SELECT 1 FROM BookingSeats bs WHERE bs.BookingID=bk.BookingID AND bs.SeatStatus='HELD' AND bs.HoldUntil>SYSDATETIME())))) THEN 1 ELSE 0 END HasBookings "
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
                = "SELECT s.*, m.Title AS MovieTitle, h.HallName, h.HallType, h.CentreID, CASE WHEN EXISTS(SELECT 1 FROM Bookings bk WHERE bk.ShowtimeID=s.ShowtimeID AND (bk.BookingStatus='CONFIRMED' OR (bk.BookingStatus='PENDING' AND EXISTS(SELECT 1 FROM BookingSeats bs WHERE bs.BookingID=bk.BookingID AND bs.SeatStatus='HELD' AND bs.HoldUntil>SYSDATETIME())))) THEN 1 ELSE 0 END HasBookings "
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
                + "AND Status = 'SCHEDULED' AND SaleState='DRAFT' AND StartTime > SYSDATETIME() AND NOT EXISTS(SELECT 1 FROM Bookings bk WHERE bk.ShowtimeID=Showtimes.ShowtimeID)";

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
                + "('PENDING','CONFIRMED','COMPLETED')";

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
                + "AND Status = 'SCHEDULED' AND SaleState='DRAFT' AND StartTime > SYSDATETIME() AND NOT EXISTS(SELECT 1 FROM Bookings bk WHERE bk.ShowtimeID=Showtimes.ShowtimeID)";

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

        s.setCentreId(rs.getInt("CentreID"));
        s.setSaleState(rs.getString("SaleState"));
        s.setSaleOpensAt(rs.getTimestamp("SaleOpensAt"));
        s.setPriceLockedAt(rs.getTimestamp("PriceLockedAt"));
        s.setHasBookings(rs.getBoolean("HasBookings"));
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
