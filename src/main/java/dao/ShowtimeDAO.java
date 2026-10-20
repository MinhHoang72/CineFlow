package dao;

import model.Centre;
import model.Showtime;
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

    /** Gia goc cua rap (BranchPricing) dang hieu luc tai gio chieu; NULL neu rap chua co bang gia. */
    private static final String CENTRE_BASE_PRICE =
            "(SELECT TOP 1 bp.BasePrice FROM BranchPricing bp "
          + " WHERE bp.BranchID = b.CentreID AND bp.EffectiveFrom <= s.StartTime "
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
          + "     WHERE tp.BranchID = b.CentreID "
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
    public List<LocalDate> getShowDates(int branchId, int movieId) throws SQLException {
        String sql = "SELECT DISTINCT CAST(s.StartTime AS DATE) AS ShowDate "
                   + "FROM Showtimes s JOIN Movies m ON m.MovieID = s.MovieID "
                   + "JOIN Halls h ON h.HallID = s.HallID JOIN Centres b ON b.CentreID = h.CentreID "
                   + "WHERE " + BOOKABLE + "AND b.CentreID = ? AND s.MovieID = ? ORDER BY ShowDate";
        List<LocalDate> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setInt(2, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getDate("ShowDate").toLocalDate());
            }
        }
        return list;
    }

    public List<Showtime> getShowtimes(int branchId, int movieId, LocalDate date) throws SQLException {
        String sql = SELECT + "WHERE " + BOOKABLE
                   + "AND b.CentreID = ? AND s.MovieID = ? "
                   + "AND s.StartTime >= ? AND s.StartTime < ? ORDER BY s.StartTime";
        List<Showtime> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
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
    public List<LocalDate> getShowDatesAtBranch(int branchId) throws SQLException {
        String sql = "SELECT DISTINCT CAST(s.StartTime AS DATE) AS ShowDate "
                   + "FROM Showtimes s JOIN Movies m ON m.MovieID = s.MovieID "
                   + "JOIN Halls h ON h.HallID = s.HallID JOIN Centres b ON b.CentreID = h.CentreID "
                   + "WHERE " + BOOKABLE + "AND b.CentreID = ? ORDER BY ShowDate";
        List<LocalDate> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getDate("ShowDate").toLocalDate());
            }
        }
        return list;
    }

    /** Tat ca suat con mo ban cua chi nhanh trong ngay, sap theo phim roi gio chieu. */
    public List<Showtime> getShowtimesAtBranch(int branchId, LocalDate date) throws SQLException {
        String sql = SELECT + "WHERE " + BOOKABLE
                   + "AND b.CentreID = ? AND s.StartTime >= ? AND s.StartTime < ? ORDER BY m.Title, s.StartTime";
        List<Showtime> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
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
}
