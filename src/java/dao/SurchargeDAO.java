package dao;
import utils.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Surcharge;

public class SurchargeDAO extends DBContext {

    private Surcharge map(ResultSet rs)
            throws SQLException {

        Surcharge s = new Surcharge();

        s.setSurchargeId(
                rs.getInt("SurchargeID")
        );

        s.setSurchargeName(
                rs.getString("SurchargeName")
        );

        s.setSeatType(
                rs.getString("SeatType")
        );

        s.setAmount(
                rs.getDouble("Amount")
        );

        Timestamp from =
                rs.getTimestamp("EffectiveFrom");

        if (from != null) {
            s.setEffectiveFrom(
                    from.toLocalDateTime()
            );
        }

        Timestamp to =
                rs.getTimestamp("EffectiveTo");

        if (to != null) {
            s.setEffectiveTo(
                    to.toLocalDateTime()
            );
        }

        int updatedBy =
                rs.getInt("UpdatedBy");

        if (!rs.wasNull()) {
            s.setUpdatedBy(updatedBy);
        }

        Timestamp created =
                rs.getTimestamp("CreatedAt");

        if (created != null) {
            s.setCreatedAt(
                    created.toLocalDateTime()
            );
        }

        return s;
    }

    public List<Surcharge> getCurrentRules() {

        List<Surcharge> list =
                new ArrayList<>();

        String sql =
                "SELECT * "
                + "FROM Surcharges "
                + "WHERE EffectiveTo IS NULL "
                + "ORDER BY SeatType";

        try (PreparedStatement ps =
                     connection.prepareStatement(sql)) {

            ResultSet rs =
                    ps.executeQuery();

            while (rs.next()) {
                list.add(map(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    public List<Surcharge> getHistory() {

        List<Surcharge> list =
                new ArrayList<>();

        String sql =
                "SELECT * "
                + "FROM Surcharges "
                + "ORDER BY EffectiveFrom DESC";

        try (PreparedStatement ps =
                     connection.prepareStatement(sql)) {

            ResultSet rs =
                    ps.executeQuery();

            while (rs.next()) {
                list.add(map(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    public Surcharge getById(int id) {

        String sql =
                "SELECT * "
                + "FROM Surcharges "
                + "WHERE SurchargeID = ?";

        try (PreparedStatement ps =
                     connection.prepareStatement(sql)) {

            ps.setInt(1, id);

            ResultSet rs =
                    ps.executeQuery();

            if (rs.next()) {
                return map(rs);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean updateSurcharge(
            int oldId,
            Surcharge newRule) {

        String closeOld =
                "UPDATE Surcharges "
                + "SET EffectiveTo = GETDATE() "
                + "WHERE SurchargeID = ? "
                + "AND EffectiveTo IS NULL";

        String insertNew =
                "INSERT INTO Surcharges "
                + "(SurchargeName, SeatType, Amount, "
                + "EffectiveFrom, EffectiveTo, UpdatedBy) "
                + "VALUES (?, ?, ?, GETDATE(), NULL, ?)";

        try {

            connection.setAutoCommit(false);

            try (
                PreparedStatement psClose =
                        connection.prepareStatement(
                                closeOld
                        );

                PreparedStatement psInsert =
                        connection.prepareStatement(
                                insertNew
                        )
            ) {

                psClose.setInt(
                        1,
                        oldId
                );

                int closed =
                        psClose.executeUpdate();

                if (closed == 0) {
                    connection.rollback();
                    return false;
                }

                psInsert.setString(
                        1,
                        newRule.getSurchargeName()
                );

                psInsert.setString(
                        2,
                        newRule.getSeatType()
                );

                psInsert.setDouble(
                        3,
                        newRule.getAmount()
                );

                psInsert.setInt(
                        4,
                        newRule.getUpdatedBy()
                );

                int inserted =
                        psInsert.executeUpdate();

                if (inserted == 0) {
                    connection.rollback();
                    return false;
                }

                connection.commit();

                return true;
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

    public boolean deleteRule(int id) {

        String sql =
                "UPDATE Surcharges "
                + "SET EffectiveTo = GETDATE() "
                + "WHERE SurchargeID = ? "
                + "AND EffectiveTo IS NULL";

        try (PreparedStatement ps =
                     connection.prepareStatement(sql)) {

            ps.setInt(1, id);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public double getSeatSurchargeAt(
            String seatType,
            Timestamp showtime) {

        if ("STANDARD".equalsIgnoreCase(
                seatType)) {

            return 0;
        }

        String sql =
                "SELECT TOP 1 Amount "
                + "FROM Surcharges "
                + "WHERE SeatType = ? "
                + "AND EffectiveFrom <= ? "
                + "AND (EffectiveTo IS NULL "
                + "OR ? < EffectiveTo) "
                + "ORDER BY EffectiveFrom DESC";

        try (PreparedStatement ps =
                     connection.prepareStatement(sql)) {

            ps.setString(
                    1,
                    seatType
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
                return rs.getDouble("Amount");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }
}