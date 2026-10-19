package dao;

import model.Showtime;
import utils.DBContext;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
