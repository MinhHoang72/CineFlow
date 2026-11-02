package dao;

import model.entity.Voucher;
import utils.DBContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Truy cap Vouchers / VoucherCentres / VoucherMovies / VoucherDays / VoucherUsages khi dat ve.
 * Chi doc / ghi du lieu; kiem tra pham vi, luot dung va ap ma nam o service.BookingExtrasService.
 */
public class BookingVoucherDAO {

    private static final String COLUMNS = "v.VoucherID, v.VoucherCode, v.VoucherName, v.Description, v.CentreID, "
            + "v.DiscountType, v.DiscountValue, v.MinimumOrderValue, v.MaximumDiscount, v.StartDate, v.EndDate, "
            + "v.UsageLimit, v.UsedCount, v.PerCustomerLimit, v.Status";

    /** Voucher theo ma (khong phan biet hoa thuong), null neu khong co. */
    public Voucher findByCode(String code) throws SQLException {
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT " + COLUMNS + " FROM Vouchers v WHERE v.VoucherCode = ?")) {
            ps.setString(1, code.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /**
     * Voucher dang dung duoc cho suat chieu nay: dang hoat dong, trong han, con luot, dung rap / phim / ngay chieu,
     * va khach chua dung het luot cua minh. Chua xet gia tri don toi thieu (VoucherService.applyVoucher xet).
     * @param userId null = khach vang lai tai quay (bo qua gioi han luot moi khach)
     */
    public List<Voucher> findAvailable(int centreId, int movieId, LocalDate showDate, Integer userId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM Vouchers v "
                   + "WHERE v.Status = 'ACTIVE' AND v.StartDate <= SYSDATETIME() AND v.EndDate >= SYSDATETIME() "
                   + "  AND (v.UsageLimit IS NULL OR v.UsedCount < v.UsageLimit) "
                   + "  AND (v.CentreID IS NULL OR v.CentreID = ?) "
                   + "  AND (NOT EXISTS (SELECT 1 FROM VoucherCentres x WHERE x.VoucherID = v.VoucherID) "
                   + "       OR EXISTS (SELECT 1 FROM VoucherCentres x WHERE x.VoucherID = v.VoucherID AND x.CentreID = ?)) "
                   + "  AND (NOT EXISTS (SELECT 1 FROM VoucherMovies x WHERE x.VoucherID = v.VoucherID) "
                   + "       OR EXISTS (SELECT 1 FROM VoucherMovies x WHERE x.VoucherID = v.VoucherID AND x.MovieID = ?)) "
                   + "  AND (NOT EXISTS (SELECT 1 FROM VoucherDays x WHERE x.VoucherID = v.VoucherID) "
                   + "       OR EXISTS (SELECT 1 FROM VoucherDays x WHERE x.VoucherID = v.VoucherID AND x.DayOfWeek = ?)) "
                   + "  AND (? IS NULL OR (SELECT COUNT(*) FROM VoucherUsages u "
                   + "                     WHERE u.VoucherID = v.VoucherID AND u.UserID = ?) < v.PerCustomerLimit) "
                   + "ORDER BY v.EndDate, v.VoucherCode";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, centreId);
            ps.setInt(2, centreId);
            ps.setInt(3, movieId);
            ps.setString(4, showDate.getDayOfWeek().name());
            if (userId == null) {
                ps.setNull(5, Types.INTEGER);
                ps.setNull(6, Types.INTEGER);
            } else {
                ps.setInt(5, userId);
                ps.setInt(6, userId);
            }
            List<Voucher> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
            return list;
        }
    }

    private static Voucher map(ResultSet rs) throws SQLException {
        Voucher v = new Voucher();
        v.setVoucherId(rs.getInt("VoucherID"));
        v.setVoucherCode(rs.getString("VoucherCode"));
        v.setVoucherName(rs.getString("VoucherName"));
        v.setDescription(rs.getString("Description"));
        int centreId = rs.getInt("CentreID");
        v.setCentreId(rs.wasNull() ? null : centreId);
        v.setDiscountType(rs.getString("DiscountType"));
        v.setDiscountValue(rs.getBigDecimal("DiscountValue"));
        v.setMinimumOrderValue(rs.getBigDecimal("MinimumOrderValue"));
        v.setMaximumDiscount(rs.getBigDecimal("MaximumDiscount"));
        Timestamp start = rs.getTimestamp("StartDate"), end = rs.getTimestamp("EndDate");
        v.setStartDate(start == null ? null : start.toLocalDateTime());
        v.setEndDate(end == null ? null : end.toLocalDateTime());
        int limit = rs.getInt("UsageLimit");
        v.setUsageLimit(rs.wasNull() ? null : limit);
        v.setUsedCount(rs.getInt("UsedCount"));
        v.setStatus(rs.getString("Status"));
        return v;
    }

    /** So luot moi khach duoc dung voucher (PerCustomerLimit). */
    public int perCustomerLimit(int voucherId) throws SQLException {
        return queryInt("SELECT PerCustomerLimit FROM Vouchers WHERE VoucherID = ?", voucherId);
    }

    /** So lan khach da dung voucher o cac don da thanh toan. */
    public int countUserUsages(int voucherId, int userId) throws SQLException {
        return queryInt("SELECT COUNT(*) FROM VoucherUsages WHERE VoucherID = ? AND UserID = ?", voucherId, userId);
    }

    /** Cac rap duoc khai bao rieng cho voucher (rong = khong gioi han). */
    public List<Integer> getScopeCentreIds(int voucherId) throws SQLException {
        return intList("SELECT CentreID FROM VoucherCentres WHERE VoucherID = ?", voucherId);
    }

    /** Cac phim duoc khai bao rieng cho voucher (rong = khong gioi han). */
    public List<Integer> getScopeMovieIds(int voucherId) throws SQLException {
        return intList("SELECT MovieID FROM VoucherMovies WHERE VoucherID = ?", voucherId);
    }

    /** Cac thu trong tuan (MONDAY...) duoc khai bao rieng cho voucher (rong = khong gioi han). */
    public List<String> getScopeDays(int voucherId) throws SQLException {
        List<String> days = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT DayOfWeek FROM VoucherDays WHERE VoucherID = ?")) {
            ps.setInt(1, voucherId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) days.add(rs.getString(1));
            }
        }
        return days;
    }

    /** Ghi luot dung voucher cua khach cho don (bo qua neu da ghi). */
    public void insertUsageIfAbsent(Connection conn, int voucherId, int userId, int bookingId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO VoucherUsages (VoucherID, UserID, BookingID) SELECT ?, ?, ? "
              + "WHERE NOT EXISTS (SELECT 1 FROM VoucherUsages WHERE VoucherID = ? AND BookingID = ?)")) {
            ps.setInt(1, voucherId);
            ps.setInt(2, userId);
            ps.setInt(3, bookingId);
            ps.setInt(4, voucherId);
            ps.setInt(5, bookingId);
            ps.executeUpdate();
        }
    }

    public void incrementUsedCount(Connection conn, int voucherId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE Vouchers SET UsedCount = UsedCount + 1 WHERE VoucherID = ?")) {
            ps.setInt(1, voucherId);
            ps.executeUpdate();
        }
    }

    private static List<Integer> intList(String sql, int param) throws SQLException {
        List<Integer> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getInt(1));
            }
        }
        return list;
    }

    private static int queryInt(String sql, int... params) throws SQLException {
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setInt(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}
