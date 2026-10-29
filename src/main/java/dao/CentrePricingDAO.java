/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

/**
 *
 * @author vktuy
 */

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.entity.CentrePricing;
import utils.DBContext;

public class CentrePricingDAO extends DBContext {

    public CentrePricing getCurrentPrice(int centreId) {

        String sql =
            "SELECT TOP 1 * "
            + "FROM CentrePricing "
            + "WHERE CentreID = ? "
            + "AND EffectiveFrom <= SYSDATETIME() "
            + "AND (EffectiveTo IS NULL OR EffectiveTo > SYSDATETIME()) "
            + "ORDER BY EffectiveFrom DESC";

        try (PreparedStatement ps =
                connection.prepareStatement(sql)) {

            ps.setInt(1, centreId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                CentrePricing bp = new CentrePricing();

                bp.setCentrePricingId(
                        rs.getInt("CentrePricingID")
                );

                bp.setCentreId(
                        rs.getInt("CentreID")
                );

                bp.setBasePrice(
                        rs.getDouble("BasePrice")
                );

                Timestamp from =
                        rs.getTimestamp("EffectiveFrom");

                if (from != null) {
                    bp.setEffectiveFrom(
                            from.toLocalDateTime()
                    );
                }

                Timestamp to =
                        rs.getTimestamp("EffectiveTo");

                if (to != null) {
                    bp.setEffectiveTo(
                            to.toLocalDateTime()
                    );
                }

                int updatedBy =
                        rs.getInt("UpdatedBy");

                if (!rs.wasNull()) {
                    bp.setUpdatedBy(updatedBy);
                }

                Timestamp created =
                        rs.getTimestamp("CreatedAt");

                if (created != null) {
                    bp.setCreatedAt(
                            created.toLocalDateTime()
                    );
                }

                return bp;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
        public List<CentrePricing> getPriceHistory(
            int centreId) {

        List<CentrePricing> list =
                new ArrayList<>();

        String sql =
            "SELECT bp.*, u.FullName AS UpdatedByName "
            + "FROM CentrePricing bp LEFT JOIN Users u ON u.UserID = bp.UpdatedBy "
            + "WHERE bp.CentreID = ? "
            + "ORDER BY bp.EffectiveFrom DESC, bp.CentrePricingID DESC";

        try (PreparedStatement ps =
                connection.prepareStatement(sql)) {

            ps.setInt(1, centreId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                CentrePricing bp =
                        new CentrePricing();

                bp.setCentrePricingId(
                        rs.getInt("CentrePricingID")
                );

                bp.setCentreId(
                        rs.getInt("CentreID")
                );

                bp.setBasePrice(
                        rs.getDouble("BasePrice")
                );

                Timestamp from =
                        rs.getTimestamp("EffectiveFrom");

                if (from != null) {
                    bp.setEffectiveFrom(
                            from.toLocalDateTime()
                    );
                }

                Timestamp to =
                        rs.getTimestamp("EffectiveTo");

                if (to != null) {
                    bp.setEffectiveTo(
                            to.toLocalDateTime()
                    );
                }

                int updatedBy =
                        rs.getInt("UpdatedBy");

                if (!rs.wasNull()) {
                    bp.setUpdatedBy(updatedBy);
                }

                Timestamp created =
                        rs.getTimestamp("CreatedAt");

                if (created != null) {
                    bp.setCreatedAt(
                            created.toLocalDateTime()
                    );
                }

                bp.setUpdatedByName(rs.getString("UpdatedByName"));
                list.add(bp);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }
    public boolean updateBasePrice(int centreId, double newPrice, int managerId) {
        return updateBasePrice(centreId, newPrice, managerId, null);
    }

    public boolean updateBasePrice(int centreId, double newPrice, int managerId,
            java.time.LocalDateTime effectiveFrom) {
        try (Connection c = DBContext.getConnection()) {
            c.setAutoCommit(false);
            try {
                scheduleBasePrice(c, centreId, newPrice, managerId, effectiveFrom);
                c.commit();
                return true;
            } catch (SQLException | IllegalArgumentException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể lưu giá cơ bản.", e);
        }
    }

    /** Caller owns transaction. Lock the centre even when no pricing rows exist yet. */
    public void scheduleBasePrice(Connection c, int centreId, double price, int managerId,
            java.time.LocalDateTime requestedStart) throws SQLException {
        if (!Double.isFinite(price) || price < 0 || price > 9999999999d)
            throw new IllegalArgumentException("Giá vé không hợp lệ.");
        if (c.getAutoCommit()) throw new SQLException("Pricing changes require a transaction");
        try (PreparedStatement ps = c.prepareStatement("SELECT CentreID FROM Centres WITH (UPDLOCK,HOLDLOCK) WHERE CentreID=?")) {
            ps.setInt(1, centreId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Cụm rạp không tồn tại.");
            }
        }
        Timestamp now;
        try (Statement ps = c.createStatement(); ResultSet rs = ps.executeQuery("SELECT SYSDATETIME()")) {
            rs.next(); now = rs.getTimestamp(1);
        }
        Timestamp start = requestedStart == null ? now : Timestamp.valueOf(requestedStart);
        if (requestedStart != null && !start.after(now))
            throw new IllegalArgumentException("Thời gian áp dụng phải nằm trong tương lai.");
        Timestamp next = null;
        try (PreparedStatement ps = c.prepareStatement("SELECT TOP 1 EffectiveFrom FROM CentrePricing WITH (UPDLOCK,HOLDLOCK) WHERE CentreID=? AND EffectiveFrom>=? ORDER BY EffectiveFrom")) {
            ps.setInt(1, centreId); ps.setTimestamp(2, start);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) next = rs.getTimestamp(1);
            }
        }
        if (start.equals(next)) throw new IllegalArgumentException("Đã có mức giá bắt đầu tại thời điểm này.");
        try (PreparedStatement ps = c.prepareStatement("UPDATE CentrePricing SET EffectiveTo=? WHERE CentreID=? AND EffectiveFrom<? AND (EffectiveTo IS NULL OR EffectiveTo>?)")) {
            ps.setTimestamp(1, start); ps.setInt(2, centreId); ps.setTimestamp(3, start); ps.setTimestamp(4, start);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO CentrePricing (CentreID,BasePrice,EffectiveFrom,EffectiveTo,UpdatedBy) VALUES (?,?,?,?,?)")) {
            ps.setInt(1, centreId); ps.setDouble(2, price); ps.setTimestamp(3, start);
            ps.setTimestamp(4, next); ps.setInt(5, managerId); ps.executeUpdate();
        }
    }

            public double getBasePriceAt(
        int centreId,
        Timestamp showtime) {

    String sql =
        "SELECT TOP 1 BasePrice "
        + "FROM CentrePricing "
        + "WHERE CentreID = ? "
        + "AND EffectiveFrom <= ? "
        + "AND (EffectiveTo IS NULL "
        + "OR ? < EffectiveTo) "
        + "ORDER BY EffectiveFrom DESC";

    try (PreparedStatement ps =
            connection.prepareStatement(sql)) {

        ps.setInt(
            1,
            centreId
        );

        ps.setTimestamp(
            2,
            showtime
        );

        ps.setTimestamp(
            3,
            showtime
        );

        ResultSet rs =
                ps.executeQuery();

        if (rs.next()) {
            return rs.getDouble(
                    "BasePrice"
            );
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }

    return 0;
}
}
