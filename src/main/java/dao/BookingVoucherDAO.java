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

    /* ============================== MỤC LỤC ==============================
     * Tìm nhanh: Ctrl + F tên hàm. Mỗi hàm có dòng ">> Dùng ở: ... | Loại: ĐỌC / GHI" trong chú thích.
     * Danh sách mã gợi ý ở trang checkout .. findAvailable
     * Khách nhập / bấm Dùng một mã ......... findByCode, getScopeCentreIds, getScopeMovieIds, getScopeDays,
     *                                        perCustomerLimit, countUserUsages
     * Thanh toán xong (ghi lượt dùng) ...... insertUsageIfAbsent, incrementUsedCount
     * Hằng COLUMNS = danh sách cột voucher dùng chung
     * ===================================================================== */

    private static final String COLUMNS = """
            v.VoucherID, v.VoucherCode, v.VoucherName, v.Description, v.CentreID,
            v.DiscountType, v.DiscountValue, v.MinimumOrderValue, v.MaximumDiscount, v.StartDate, v.EndDate,
            v.UsageLimit, v.UsedCount, v.PerCustomerLimit, v.Status
            """;

    /**
     * Tìm voucher theo mã (không phân biệt hoa thường); không có thì null.
     * >> Dùng ở: trang checkout - khách nhập / bấm Dùng một mã (BookingExtrasService.applyVoucher) | Loại: ĐỌC (SELECT)
     */
    public Voucher findByCode(String code) throws SQLException {
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                     SELECT %s
                     FROM Vouchers v WHERE v.VoucherCode = ?
                     """.formatted(COLUMNS))) {   // %s = COLUMNS (danh sách cột voucher)
            ps.setString(1, code.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /**
     * Các voucher đang dùng được cho suất chiếu: đang hoạt động, trong hạn, còn lượt, đúng rạp / phim / thứ trong tuần,
     * và khách (nếu có tài khoản) chưa dùng hết lượt của mình. Dùng cho danh sách voucher gợi ý ở trang xem lại đơn.
     * >> Dùng ở: trang checkout - danh sách mã có sẵn (BookingExtrasService.getVoucherOptions) | Loại: ĐỌC (SELECT)
     */
    public List<Voucher> findAvailable(int centreId, int movieId, LocalDate showDate, Integer userId) throws SQLException {
        // %s = COLUMNS (danh sách cột voucher)
        String sql = """
                SELECT %s
                FROM Vouchers v
                WHERE v.Status = 'ACTIVE' AND v.StartDate <= SYSDATETIME() AND v.EndDate >= SYSDATETIME()
                  AND (v.UsageLimit IS NULL OR v.UsedCount < v.UsageLimit)
                  AND (v.CentreID IS NULL OR v.CentreID = ?)
                  AND (NOT EXISTS (SELECT 1 FROM VoucherCentres x WHERE x.VoucherID = v.VoucherID)
                       OR EXISTS (SELECT 1 FROM VoucherCentres x WHERE x.VoucherID = v.VoucherID AND x.CentreID = ?))
                  AND (NOT EXISTS (SELECT 1 FROM VoucherMovies x WHERE x.VoucherID = v.VoucherID)
                       OR EXISTS (SELECT 1 FROM VoucherMovies x WHERE x.VoucherID = v.VoucherID AND x.MovieID = ?))
                  AND (NOT EXISTS (SELECT 1 FROM VoucherDays x WHERE x.VoucherID = v.VoucherID)
                       OR EXISTS (SELECT 1 FROM VoucherDays x WHERE x.VoucherID = v.VoucherID AND x.DayOfWeek = ?))
                  AND (? IS NULL OR (SELECT COUNT(*) FROM VoucherUsages u
                                     WHERE u.VoucherID = v.VoucherID AND u.UserID = ?) < v.PerCustomerLimit)
                ORDER BY v.EndDate, v.VoucherCode
                """.formatted(COLUMNS);
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

    /** Đọc một dòng kết quả SQL thành đối tượng Voucher. */
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

    /**
     * Số lượt mỗi khách được dùng voucher (cột PerCustomerLimit).
     * >> Dùng ở: áp voucher - mỗi khách dùng tối đa mấy lần (BookingExtrasService.applyVoucher) | Loại: ĐỌC (SELECT)
     */
    public int perCustomerLimit(int voucherId) throws SQLException {
        return queryInt("""
                SELECT PerCustomerLimit FROM Vouchers WHERE VoucherID = ?
                """, voucherId);
    }

    /**
     * Số lần khách đã dùng voucher ở các đơn đã thanh toán.
     * >> Dùng ở: áp voucher - khách đã dùng mấy lần (BookingExtrasService.applyVoucher) | Loại: ĐỌC (SELECT)
     */
    public int countUserUsages(int voucherId, int userId) throws SQLException {
        return queryInt("""
                SELECT COUNT(*) FROM VoucherUsages WHERE VoucherID = ? AND UserID = ?
                """, voucherId, userId);
    }

    /**
     * Các rạp được khai báo riêng cho voucher (rỗng = áp dụng mọi rạp).
     * >> Dùng ở: áp voucher - mã chỉ dùng cho rạp nào (BookingExtrasService.checkScope) | Loại: ĐỌC (SELECT)
     */
    public List<Integer> getScopeCentreIds(int voucherId) throws SQLException {
        return intList("""
                SELECT CentreID FROM VoucherCentres WHERE VoucherID = ?
                """, voucherId);
    }

    /**
     * Các phim được khai báo riêng cho voucher (rỗng = áp dụng mọi phim).
     * >> Dùng ở: áp voucher - mã chỉ dùng cho phim nào (BookingExtrasService.checkScope) | Loại: ĐỌC (SELECT)
     */
    public List<Integer> getScopeMovieIds(int voucherId) throws SQLException {
        return intList("""
                SELECT MovieID FROM VoucherMovies WHERE VoucherID = ?
                """, voucherId);
    }

    /**
     * Các thứ trong tuần (MONDAY...) được khai báo riêng cho voucher (rỗng = áp dụng mọi ngày).
     * >> Dùng ở: áp voucher - mã chỉ dùng thứ nào (BookingExtrasService.checkScope) | Loại: ĐỌC (SELECT)
     */
    public List<String> getScopeDays(int voucherId) throws SQLException {
        List<String> days = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                     SELECT DayOfWeek FROM VoucherDays WHERE VoucherID = ?
                     """)) {
            ps.setInt(1, voucherId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) days.add(rs.getString(1));
            }
        }
        return days;
    }

    /**
     * Ghi lượt dùng voucher của khách cho đơn vào VoucherUsages; đã ghi rồi thì bỏ qua.
     * >> Dùng ở: thanh toán xong - ghi lượt dùng của khách (PaymentService.recordVoucherUsage) | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    public void insertUsageIfAbsent(Connection conn, int voucherId, int userId, int bookingId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                        INSERT INTO VoucherUsages (VoucherID, UserID, BookingID) SELECT ?, ?, ?
                        WHERE NOT EXISTS (SELECT 1 FROM VoucherUsages WHERE VoucherID = ? AND BookingID = ?)
                        """)) {
            ps.setInt(1, voucherId);
            ps.setInt(2, userId);
            ps.setInt(3, bookingId);
            ps.setInt(4, voucherId);
            ps.setInt(5, bookingId);
            ps.executeUpdate();
        }
    }

    /**
     * Tăng số lượt đã dùng (UsedCount) của voucher thêm 1.
     * >> Dùng ở: thanh toán xong - tăng tổng lượt dùng của mã (PaymentService.recordVoucherUsage) | Loại: GHI (INSERT / UPDATE / DELETE)
     */
    public void incrementUsedCount(Connection conn, int voucherId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                UPDATE Vouchers SET UsedCount = UsedCount + 1 WHERE VoucherID = ?
                """)) {
            ps.setInt(1, voucherId);
            ps.executeUpdate();
        }
    }

    /**
     * Chạy câu SELECT một cột số nguyên với một tham số id, trả về danh sách.
     * >> Dùng ở: nội bộ - SELECT trả về danh sách số | Loại: ĐỌC (SELECT)
     */
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

    /**
     * Chạy câu SELECT trả về một số nguyên (ví dụ COUNT); không có dòng thì 0.
     * >> Dùng ở: nội bộ - SELECT trả về một số | Loại: ĐỌC (SELECT)
     */
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
