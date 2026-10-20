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

        s.setTargetValue(
                rs.getString("TargetValue")
        );

        s.setSurchargeType(rs.getString("SurchargeType"));
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
                + "ORDER BY SurchargeType, TargetValue";

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
                + "(SurchargeName, TargetValue, SurchargeType, Amount, "
                + "EffectiveFrom, EffectiveTo, UpdatedBy) "
                + "VALUES (?, ?, ?, ?, GETDATE(), NULL, ?)";

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
                        newRule.getTargetValue()
                );

                psInsert.setString(3, newRule.getSurchargeType());
                psInsert.setDouble(
                        4,
                        newRule.getAmount()
                );

                if (newRule.getUpdatedBy() == null) psInsert.setNull(5, Types.INTEGER);
                else psInsert.setInt(5, newRule.getUpdatedBy());

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

    public double getSeatSurchargeAt(String seatType, Timestamp showtime) {
        if ("STANDARD".equalsIgnoreCase(seatType)) return 0;
        return getSurchargeAt("SEAT", seatType, showtime);
    }

    public double getHallSurchargeAt(String hallType, Timestamp showtime) {
        return getSurchargeAt("HALL", hallType, showtime);
    }

    private double getSurchargeAt(String type, String target, Timestamp showtime) {
        String sql = "SELECT TOP 1 Amount FROM Surcharges "
                + "WHERE SurchargeType = ? AND TargetValue = ? AND EffectiveFrom <= ? "
                + "AND (EffectiveTo IS NULL OR ? < EffectiveTo) "
                + "ORDER BY EffectiveFrom DESC, SurchargeID DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, type);
            ps.setString(2, target);
            ps.setTimestamp(3, showtime);
            ps.setTimestamp(4, showtime);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("Amount");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load " + type + " surcharge", e);
        }
        return 0;
    }
}