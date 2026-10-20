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
import model.BranchPricing;
import utils.DBContext;

public class BranchPricingDAO extends DBContext {

    public BranchPricing getCurrentPrice(int centreId) {

        String sql =
            "SELECT TOP 1 * "
            + "FROM BranchPricing "
            + "WHERE BranchID = ? "
            + "AND EffectiveTo IS NULL "
            + "ORDER BY EffectiveFrom DESC";

        try (PreparedStatement ps =
                connection.prepareStatement(sql)) {

            ps.setInt(1, centreId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                BranchPricing bp = new BranchPricing();

                bp.setBranchPricingId(
                        rs.getInt("BranchPricingID")
                );

                bp.setBranchId(
                        rs.getInt("BranchID")
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
        public List<BranchPricing> getPriceHistory(
            int centreId) {

        List<BranchPricing> list =
                new ArrayList<>();

        String sql =
            "SELECT * "
            + "FROM BranchPricing "
            + "WHERE BranchID = ? "
            + "ORDER BY EffectiveFrom DESC";

        try (PreparedStatement ps =
                connection.prepareStatement(sql)) {

            ps.setInt(1, centreId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                BranchPricing bp =
                        new BranchPricing();

                bp.setBranchPricingId(
                        rs.getInt("BranchPricingID")
                );

                bp.setBranchId(
                        rs.getInt("BranchID")
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
            "UPDATE BranchPricing "
            + "SET EffectiveTo = GETDATE() "
            + "WHERE BranchID = ? "
            + "AND EffectiveTo IS NULL";

        String insertNewPrice =
            "INSERT INTO BranchPricing "
            + "(BranchID, BasePrice, "
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
        + "FROM BranchPricing "
        + "WHERE BranchID = ? "
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
