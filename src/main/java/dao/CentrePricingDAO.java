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
            + "AND EffectiveTo IS NULL "
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
            public boolean updateBasePrice(
            int centreId,
            double newPrice,
            int managerId) {

        String closeOldPrice =
            "UPDATE CentrePricing "
            + "SET EffectiveTo = GETDATE() "
            + "WHERE CentreID = ? "
            + "AND EffectiveTo IS NULL";

        String insertNewPrice =
            "INSERT INTO CentrePricing "
            + "(CentreID, BasePrice, "
            + "EffectiveFrom, EffectiveTo, "
            + "UpdatedBy) "
            + "VALUES (?, ?, GETDATE(), NULL, ?)";

        try {

            connection.setAutoCommit(false);

            try (
                PreparedStatement psClose =
                    connection.prepareStatement(
                        closeOldPrice
                    );

                PreparedStatement psInsert =
                    connection.prepareStatement(
                        insertNewPrice
                    )
            ) {

                // Close old price
                psClose.setInt(1, centreId);
                psClose.executeUpdate();

                // Insert new price
                psInsert.setInt(1, centreId);
                psInsert.setDouble(2, newPrice);
                psInsert.setInt(3, managerId);

                int affected =
                        psInsert.executeUpdate();

                connection.commit();

                return affected > 0;
            }

        } catch (SQLException e) {

            try {
                connection.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            e.printStackTrace();

        } finally {

            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return false;
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
