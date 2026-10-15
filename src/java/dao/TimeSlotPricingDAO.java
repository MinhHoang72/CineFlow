package dao;
import utils.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.TimeSlotPricing;

public class TimeSlotPricingDAO extends DBContext {

    private TimeSlotPricing map(ResultSet rs)
            throws SQLException {

        TimeSlotPricing item
                = new TimeSlotPricing();

        item.setTimeSlotPricingId(
                rs.getInt("TimeSlotPricingID")
        );

        item.setBranchId(
                rs.getInt("BranchID")
        );

        item.setSlotName(
                rs.getString("SlotName")
        );

        item.setStartTime(
                rs.getTime("StartTime")
                        .toLocalTime()
        );

        item.setEndTime(
                rs.getTime("EndTime")
                        .toLocalTime()
        );

        item.setSurcharge(
                rs.getDouble("Surcharge")
        );

        Timestamp from
                = rs.getTimestamp("EffectiveFrom");

        if (from != null) {
            item.setEffectiveFrom(
                    from.toLocalDateTime()
            );
        }

        Timestamp to
                = rs.getTimestamp("EffectiveTo");

        if (to != null) {
            item.setEffectiveTo(
                    to.toLocalDateTime()
            );
        }

        int updatedBy
                = rs.getInt("UpdatedBy");

        if (!rs.wasNull()) {
            item.setUpdatedBy(updatedBy);
        }

        Timestamp created
                = rs.getTimestamp("CreatedAt");

        if (created != null) {
            item.setCreatedAt(
                    created.toLocalDateTime()
            );
        }

        return item;
    }

    public List<TimeSlotPricing>
            getCurrentRules(int branchId) {

        List<TimeSlotPricing> list
                = new ArrayList<>();

        String sql
                = "SELECT * "
                + "FROM TimeSlotPricing "
                + "WHERE BranchID = ? "
                + "AND EffectiveTo IS NULL "
                + "ORDER BY StartTime";

        try (PreparedStatement ps
                = connection.prepareStatement(sql)) {

            ps.setInt(1, branchId);

            ResultSet rs
                    = ps.executeQuery();

            while (rs.next()) {
                list.add(map(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    public List<TimeSlotPricing>
            getHistory(int branchId) {

        List<TimeSlotPricing> list
                = new ArrayList<>();

        String sql
                = "SELECT * "
                + "FROM TimeSlotPricing "
                + "WHERE BranchID = ? "
                + "ORDER BY EffectiveFrom DESC, StartTime";

        try (PreparedStatement ps
                = connection.prepareStatement(sql)) {

            ps.setInt(1, branchId);

            ResultSet rs
                    = ps.executeQuery();

            while (rs.next()) {
                list.add(map(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    public boolean hasOverlap(
        int branchId,
        Time startTime,
        Time endTime,
        Integer excludeId) {

    System.out.println("===== CHECK OVERLAP =====");
    System.out.println("BranchID: " + branchId);
    System.out.println("New Start: " + startTime);
    System.out.println("New End: " + endTime);
    System.out.println("Exclude ID: " + excludeId);

    String sql =
            "SELECT TOP 1 "
            + "TimeSlotPricingID, "
            + "SlotName, "
            + "StartTime, "
            + "EndTime "
            + "FROM TimeSlotPricing "
            + "WHERE BranchID = ? "
            + "AND EffectiveTo IS NULL "
            + "AND StartTime < CAST(? AS TIME) "
            + "AND EndTime > CAST(? AS TIME) ";

    if (excludeId != null) {
        sql += "AND TimeSlotPricingID <> ? ";
    }

    try (PreparedStatement ps =
                 connection.prepareStatement(sql)) {

        ps.setInt(1, branchId);

        // existing StartTime < new EndTime
        ps.setString(
                2,
                endTime.toLocalTime().toString()
        );

        // existing EndTime > new StartTime
        ps.setString(
                3,
                startTime.toLocalTime().toString()
        );

        if (excludeId != null) {
            ps.setInt(4, excludeId);
        }

        System.out.println(
                "End param: "
                + endTime.toLocalTime()
        );

        System.out.println(
                "Start param: "
                + startTime.toLocalTime()
        );

        ResultSet rs =
                ps.executeQuery();

        if (rs.next()) {

            System.out.println(
                    "OVERLAP FOUND: "
                    + rs.getString("SlotName")
                    + " "
                    + rs.getTime("StartTime")
                    + " - "
                    + rs.getTime("EndTime")
            );

            return true;
        }

        System.out.println(
                "NO OVERLAP FOUND"
        );

    } catch (SQLException e) {

        System.out.println(
                "ERROR CHECKING OVERLAP"
        );

        e.printStackTrace();
    }

    return false;
}

    public boolean insert(
            TimeSlotPricing item) {

        String sql
                = "INSERT INTO TimeSlotPricing "
                + "(BranchID, SlotName, "
                + "StartTime, EndTime, "
                + "Surcharge, EffectiveFrom, "
                + "EffectiveTo, UpdatedBy) "
                + "VALUES (?, ?, ?, ?, ?, "
                + "GETDATE(), NULL, ?)";

        try (PreparedStatement ps
                = connection.prepareStatement(sql)) {

            ps.setInt(
                    1,
                    item.getBranchId()
            );

            ps.setString(
                    2,
                    item.getSlotName()
            );

            ps.setTime(
                    3,
                    Time.valueOf(
                            item.getStartTime()
                    )
            );

            ps.setTime(
                    4,
                    Time.valueOf(
                            item.getEndTime()
                    )
            );

            ps.setDouble(
                    5,
                    item.getSurcharge()
            );

            ps.setInt(
                    6,
                    item.getUpdatedBy()
            );

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean updateRule(
            int oldRuleId,
            TimeSlotPricing newRule) {

        String closeOld
                = "UPDATE TimeSlotPricing "
                + "SET EffectiveTo = GETDATE() "
                + "WHERE TimeSlotPricingID = ? "
                + "AND EffectiveTo IS NULL";

        String insertNew
                = "INSERT INTO TimeSlotPricing "
                + "(BranchID, SlotName, "
                + "StartTime, EndTime, "
                + "Surcharge, EffectiveFrom, "
                + "EffectiveTo, UpdatedBy) "
                + "VALUES (?, ?, ?, ?, ?, "
                + "GETDATE(), NULL, ?)";

        try {

            connection.setAutoCommit(false);

            try (
                    PreparedStatement psClose
                    = connection.prepareStatement(
                            closeOld
                    ); PreparedStatement psInsert
                    = connection.prepareStatement(
                            insertNew
                    )) {

                        psClose.setInt(
                                1,
                                oldRuleId
                        );

                        int closed
                                = psClose.executeUpdate();

                        if (closed == 0) {
                            connection.rollback();
                            return false;
                        }

                        psInsert.setInt(
                                1,
                                newRule.getBranchId()
                        );

                        psInsert.setString(
                                2,
                                newRule.getSlotName()
                        );

                        psInsert.setTime(
                                3,
                                Time.valueOf(
                                        newRule.getStartTime()
                                )
                        );

                        psInsert.setTime(
                                4,
                                Time.valueOf(
                                        newRule.getEndTime()
                                )
                        );

                        psInsert.setDouble(
                                5,
                                newRule.getSurcharge()
                        );

                        psInsert.setInt(
                                6,
                                newRule.getUpdatedBy()
                        );

                        int inserted
                                = psInsert.executeUpdate();

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

    public TimeSlotPricing getById(int id) {

        String sql
                = "SELECT * "
                + "FROM TimeSlotPricing "
                + "WHERE TimeSlotPricingID = ?";

        try (PreparedStatement ps
                = connection.prepareStatement(sql)) {

            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return map(rs);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public double getSurchargeAt(
            int branchId,
            Timestamp showtime) {

        String sql
                = "SELECT TOP 1 Surcharge "
                + "FROM TimeSlotPricing "
                + "WHERE BranchID = ? "
                + "AND EffectiveFrom <= ? "
                + "AND (EffectiveTo IS NULL "
                + "OR ? < EffectiveTo) "
                + "AND CAST(? AS TIME) >= StartTime "
                + "AND CAST(? AS TIME) < EndTime "
                + "ORDER BY EffectiveFrom DESC";

        try (PreparedStatement ps
                = connection.prepareStatement(sql)) {

            ps.setInt(
                    1,
                    branchId
            );

            ps.setTimestamp(
                    2,
                    showtime
            );

            ps.setTimestamp(
                    3,
                    showtime
            );

            ps.setTimestamp(
                    4,
                    showtime
            );

            ps.setTimestamp(
                    5,
                    showtime
            );

            ResultSet rs
                    = ps.executeQuery();

            if (rs.next()) {

                return rs.getDouble(
                        "Surcharge"
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }
    public boolean deleteRule(int id) {

    String sql =
            "UPDATE TimeSlotPricing "
            + "SET EffectiveTo = GETDATE() "
            + "WHERE TimeSlotPricingID = ? "
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
    
}
