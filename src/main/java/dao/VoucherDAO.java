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
import model.dto.PageDTO;
import model.entity.Voucher;
import utils.DBContext;

public class VoucherDAO {

    private static final String COLUMNS = "VoucherID, VoucherCode, VoucherName, Description, DiscountType, DiscountValue, "
            + "MinimumOrderValue, MaximumDiscount, StartDate, EndDate, UsageLimit, UsedCount, Status";

    public PageDTO<Voucher> search(String key, String status, String type, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (key != null && !key.isEmpty()) {
            where.append(" AND (VoucherCode COLLATE Latin1_General_100_CI_AI LIKE ?"
                    + " OR VoucherName COLLATE Latin1_General_100_CI_AI LIKE ?)");
            String like = "%" + key.replace("[", "[[]").replace("%", "[%]").replace("_", "[_]") + "%";
            params.add(like);
            params.add(like);
        }
        if ("EXPIRED".equals(status)) {
            where.append(" AND EndDate < GETDATE()");
        } else if ("EXHAUSTED".equals(status)) {
            where.append(" AND UsageLimit IS NOT NULL AND UsedCount >= UsageLimit");
        } else if ("ACTIVE".equals(status)) {
            where.append(" AND Status = 'ACTIVE' AND EndDate >= GETDATE()");
        } else if ("INACTIVE".equals(status)) {
            where.append(" AND Status = 'INACTIVE'");
        }
        if ("FIXED".equals(type) || "PERCENT".equals(type)) {
            where.append(" AND DiscountType = ?");
            params.add(type);
        }

        List<Voucher> rows = new ArrayList<>();
        int total = 0;
        try (Connection connection = DBContext.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM Vouchers" + where)) {
                for (int i = 0; i < params.size(); i++) {
                    statement.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        total = rs.getInt(1);
                    }
                }
            }
            int pages = Math.max(1, (total + size - 1) / size);
            int current = Math.min(Math.max(1, page), pages);
            String sql = "SELECT " + COLUMNS + " FROM Vouchers" + where
                    + " ORDER BY VoucherID DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                int index = 1;
                for (Object param : params) {
                    statement.setObject(index++, param);
                }
                statement.setInt(index++, (current - 1) * size);
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

    public Voucher find(int id) {
        try (Connection connection = DBContext.getConnection();
                PreparedStatement statement = connection.prepareStatement("SELECT " + COLUMNS + " FROM Vouchers WHERE VoucherID = ?")) {
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

    public boolean codeExists(String code, int excludeId) {
        try (Connection connection = DBContext.getConnection();
                PreparedStatement statement = connection.prepareStatement(
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

    public void insert(Voucher voucher) throws SQLException {
        String sql = "INSERT INTO Vouchers (VoucherCode, VoucherName, Description, DiscountType, DiscountValue, "
                + "MinimumOrderValue, MaximumDiscount, StartDate, EndDate, UsageLimit, UsedCount, Status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?)";
        try (Connection connection = DBContext.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, voucher);
            statement.setString(11, voucher.getStatus());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    voucher.setVoucherId(keys.getInt(1));
                }
            }
        }
    }

    public void update(Voucher voucher) throws SQLException {
        String sql = "UPDATE Vouchers SET VoucherCode = ?, VoucherName = ?, Description = ?, DiscountType = ?, "
                + "DiscountValue = ?, MinimumOrderValue = ?, MaximumDiscount = ?, StartDate = ?, EndDate = ?, "
                + "UsageLimit = ?, Status = ? WHERE VoucherID = ?";
        try (Connection connection = DBContext.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, voucher);
            statement.setString(11, voucher.getStatus());
            statement.setInt(12, voucher.getVoucherId());
            statement.executeUpdate();
        }
    }

    public void setStatus(int id, String status) throws SQLException {
        try (Connection connection = DBContext.getConnection();
                PreparedStatement statement = connection.prepareStatement("UPDATE Vouchers SET Status = ? WHERE VoucherID = ?")) {
            statement.setString(1, status);
            statement.setInt(2, id);
            statement.executeUpdate();
        }
    }

    private void bind(PreparedStatement statement, Voucher voucher) throws SQLException {
        statement.setString(1, voucher.getVoucherCode());
        statement.setString(2, voucher.getVoucherName());
        statement.setString(3, voucher.getDescription());
        statement.setString(4, voucher.getDiscountType());
        statement.setBigDecimal(5, voucher.getDiscountValue());
        statement.setBigDecimal(6, voucher.getMinimumOrderValue());
        statement.setBigDecimal(7, voucher.getMaximumDiscount());
        statement.setTimestamp(8, Timestamp.valueOf(voucher.getStartDate()));
        statement.setTimestamp(9, Timestamp.valueOf(voucher.getEndDate()));
        if (voucher.getUsageLimit() == null) {
            statement.setNull(10, Types.INTEGER);
        } else {
            statement.setInt(10, voucher.getUsageLimit());
        }
    }

    private Voucher read(ResultSet rs) throws SQLException {
        Voucher voucher = new Voucher();
        voucher.setVoucherId(rs.getInt("VoucherID"));
        voucher.setVoucherCode(rs.getString("VoucherCode"));
        voucher.setVoucherName(rs.getString("VoucherName"));
        voucher.setDescription(rs.getString("Description"));
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
        return voucher;
    }
}
