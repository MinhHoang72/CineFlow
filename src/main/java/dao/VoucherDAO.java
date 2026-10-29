package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import model.dto.common.PageDTO;
import model.entity.Voucher;
import utils.DBContext;

public class VoucherDAO {

    /**
     * Các cột SELECT từ bảng Vouchers (join thêm CentreName).
     * CentreID, CreatedBy được thêm vào để hỗ trợ nghiệp vụ:
     *   - Admin CRUD voucher toàn chuỗi (CentreID = NULL)
     *   - Manager CRUD voucher chi nhánh của mình (CentreID = centreId của manager)
     */
    private static final String COLUMNS =
            "v.VoucherID, v.VoucherCode, v.VoucherName, v.Description, v.CentreID, v.CreatedBy, "
            + "v.DiscountType, v.DiscountValue, v.MinimumOrderValue, v.MaximumDiscount, "
            + "v.StartDate, v.EndDate, v.UsageLimit, v.UsedCount, v.Status, "
            + "c.CentreName";

    private static final String FROM_JOIN =
            " FROM Vouchers v LEFT JOIN Centres c ON v.CentreID = c.CentreID";

 // trả về 1 trang page dto nên nó phải có 3 4 yếu tố 
    public PageDTO<Voucher> search(String key, String status, String type,
                                   Integer scopeCentreId, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        //params lưu giá trị các dấu ? 
        List<Object> params = new ArrayList<>();

        // Phân quyền scope: Manager chỉ thấy voucher của chi nhánh mình
        if (scopeCentreId != null) {
            where.append(" AND v.CentreID = ?");
            params.add(scopeCentreId);
        }

        if (key != null && !key.isEmpty()) {
            
            //CI không phân biệt hoa thường , AI k phân biệt dấu 
            where.append(" AND (v.VoucherCode COLLATE Latin1_General_100_CI_AI LIKE ?"
                    + " OR v.VoucherName COLLATE Latin1_General_100_CI_AI LIKE ?)");
            String like = "%" + key.replace("[", "[[]").replace("%", "[%]").replace("_", "[_]") + "%";
            params.add(like);
            params.add(like);
        }
        if ("EXPIRED".equals(status)) {
            where.append(" AND v.EndDate < GETDATE()");
        } else if ("EXHAUSTED".equals(status)) {
            where.append(" AND v.UsageLimit IS NOT NULL AND v.UsedCount >= v.UsageLimit");
        } else if ("ACTIVE".equals(status)) {
            where.append(" AND v.Status = 'ACTIVE' AND v.EndDate >= GETDATE()");
        } else if ("INACTIVE".equals(status)) {
            where.append(" AND v.Status = 'INACTIVE'");
        }
        
        if ("FIXED".equals(type) || "PERCENT".equals(type)) {
            where.append(" AND v.DiscountType = ?");
            params.add(type);
        }

        List<Voucher> rows = new ArrayList<>();
        int total = 0;
        try (Connection connection = DBContext.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(*)" + FROM_JOIN + where)) {
                for (int i = 0; i < params.size(); i++) {
                    //đang thiếu dấu ? setobject vào cho ? 
                    statement.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        //lấy dòng đầu tiên 
                        total = rs.getInt(1);//45 phần từ 
                    }
                }
            }
            // pages là số trang tính đc
            int pages = Math.max(1, (total + size - 1) / size); 
            // page này do người dùng nhập 
            //lọc người dùng nhập vào 
            int current = Math.min(Math.max(1, page), pages);
            String sql = "SELECT " + COLUMNS + FROM_JOIN + where
                   
                    + " ORDER BY v.VoucherID DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                int index = 1;
                for (Object param : params) {
                    statement.setObject(index++, param);
                }
                //tính 1 trang bao nhiêu dòng ,cút bn dòng 
                statement.setInt(index++, (current - 1) * size);
                //
                statement.setInt(index, size);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        rows.add(read(rs));
                    }
                }
            }
            return new PageDTO<>(rows, current, size, total);
        } catch (SQLException exception) {
            exception.printStackTrace();
            return new PageDTO<>(rows, 1, size, 0);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FIND
    // ─────────────────────────────────────────────────────────────────────────

    public Voucher find(int id) {
        try (Connection connection = DBContext.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT " + COLUMNS + FROM_JOIN + " WHERE v.VoucherID = ?")) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return read(rs);
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VALIDATE
    // ─────────────────────────────────────────────────────────────────────────

    public boolean codeExists(String code, int excludeId) {
        try (Connection connection = DBContext.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     //bỏ qua <>mã hiện tại nếu đang sửa
                     "SELECT 1 FROM Vouchers WHERE VoucherCode = ? AND VoucherID <> ?")) {
            statement.setString(1, code);
            statement.setInt(2, excludeId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            return true;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INSERT / UPDATE / STATUS
    // ─────────────────────────────────────────────────────────────────────────

    public void insert(Voucher voucher) throws SQLException {
        String sql = "INSERT INTO Vouchers "
                + "(VoucherCode, VoucherName, Description, CentreID, CreatedBy, "
                + "DiscountType, DiscountValue, MinimumOrderValue, MaximumDiscount, "
                + "StartDate, EndDate, UsageLimit, UsedCount, Status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?)";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, voucher);
            statement.setString(13, voucher.getStatus());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    voucher.setVoucherId(keys.getInt(1));
                }
            }
        }
    }

    public void update(Voucher voucher) throws SQLException {
        // CentreID và CreatedBy không thay đổi sau khi tạo
        String sql = "UPDATE Vouchers SET VoucherCode = ?, VoucherName = ?, Description = ?, "
                + "CentreID = ?, CreatedBy = ?, "
                + "DiscountType = ?, DiscountValue = ?, MinimumOrderValue = ?, MaximumDiscount = ?, "
                + "StartDate = ?, EndDate = ?, UsageLimit = ?, Status = ? "
                + "WHERE VoucherID = ?";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, voucher);
            statement.setString(13, voucher.getStatus());
            statement.setInt(14, voucher.getVoucherId());
            statement.executeUpdate();
        }
    }

    public void setStatus(int id, String status) throws SQLException {
        try (Connection connection = DBContext.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE Vouchers SET Status = ? WHERE VoucherID = ?")) {
            statement.setString(1, status);
            statement.setInt(2, id);
            statement.executeUpdate();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OWNERSHIP CHECK
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Kiểm tra xem voucher có thuộc về chi nhánh chỉ định hay không.
     * Manager chỉ được phép sửa/đổi trạng thái voucher của chính chi nhánh mình.
     *
     * @param voucherId  ID voucher cần kiểm tra
     * @param centreId   ID chi nhánh của manager đang đăng nhập
     * @return true nếu CentreID của voucher khớp với centreId truyền vào
     */
    // đem voucher id 10 ra xem có cnetrer id đó k 
    
    public boolean belongsToCentre(int voucherId, int centreId) {
        try (Connection connection = DBContext.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM Vouchers WHERE VoucherID = ? AND CentreID = ?")) {
            statement.setInt(1, voucherId);
            statement.setInt(2, centreId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INTERNAL HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Bind các tham số chung cho INSERT và UPDATE.
     * Thứ tự: 1=Code, 2=Name, 3=Desc, 4=CentreID, 5=CreatedBy,
     *         6=Type, 7=Value, 8=Minimum, 9=Maximum, 10=Start, 11=End, 12=Limit
     */
    private void bind(PreparedStatement statement, Voucher voucher) throws SQLException {
        statement.setString(1, voucher.getVoucherCode());
        statement.setString(2, voucher.getVoucherName());
        statement.setString(3, voucher.getDescription());
        if (voucher.getCentreId() == null) {
            statement.setNull(4, Types.INTEGER);
        } else {
            statement.setInt(4, voucher.getCentreId());
        }
        if (voucher.getCreatedBy() == null) {
            statement.setNull(5, Types.INTEGER);
        } else {
            statement.setInt(5, voucher.getCreatedBy());
        }
        statement.setString(6, voucher.getDiscountType());
        statement.setBigDecimal(7, voucher.getDiscountValue());
        statement.setBigDecimal(8, voucher.getMinimumOrderValue());
        statement.setBigDecimal(9, voucher.getMaximumDiscount());
        statement.setTimestamp(10, Timestamp.valueOf(voucher.getStartDate()));
        statement.setTimestamp(11, Timestamp.valueOf(voucher.getEndDate()));
        if (voucher.getUsageLimit() == null) {
            statement.setNull(12, Types.INTEGER);
        } else {
            statement.setInt(12, voucher.getUsageLimit());
        }
    }

    private Voucher read(ResultSet rs) throws SQLException {
        Voucher voucher = new Voucher();
        voucher.setVoucherId(rs.getInt("VoucherID"));
        voucher.setVoucherCode(rs.getString("VoucherCode"));
        voucher.setVoucherName(rs.getString("VoucherName"));
        voucher.setDescription(rs.getString("Description"));
        int cid = rs.getInt("CentreID");
        voucher.setCentreId(rs.wasNull() ? null : cid);
        int createdBy = rs.getInt("CreatedBy");
        voucher.setCreatedBy(rs.wasNull() ? null : createdBy);
        voucher.setDiscountType(rs.getString("DiscountType"));
        voucher.setDiscountValue(rs.getBigDecimal("DiscountValue"));
        voucher.setMinimumOrderValue(rs.getBigDecimal("MinimumOrderValue"));
        voucher.setMaximumDiscount(rs.getBigDecimal("MaximumDiscount"));
        voucher.setStartDate(rs.getTimestamp("StartDate").toLocalDateTime());
        voucher.setEndDate(rs.getTimestamp("EndDate").toLocalDateTime());
        int limit = rs.getInt("UsageLimit");
        voucher.setUsageLimit(rs.wasNull() ? null : limit);
        voucher.setUsedCount(rs.getInt("UsedCount"));
        voucher.setStatus(rs.getString("Status"));
        voucher.setCentreName(rs.getString("CentreName"));
        return voucher;
    }
}
