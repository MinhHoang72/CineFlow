package dao;
import utils.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Hall;

public class HallDAO extends DBContext {

    public List<Hall> getAvailableHallsByBranch(
            int branchId) {

        List<Hall> list =
                new ArrayList<>();

        String sql =
                "SELECT * "
                + "FROM Halls "
                + "WHERE BranchID = ? "
                + "AND Status = 'AVAILABLE' "
                + "ORDER BY HallName";

        try (PreparedStatement ps =
                     connection.prepareStatement(sql)) {

            ps.setInt(1, branchId);

            ResultSet rs =
                    ps.executeQuery();

            while (rs.next()) {

                Hall h = new Hall();

                h.setHallId(
                        rs.getInt("HallID")
                );

                h.setBranchId(
                        rs.getInt("BranchID")
                );

                h.setHallName(
                        rs.getString("HallName")
                );

                h.setHallType(
                        rs.getString("HallType")
                );

                h.setCapacity(
                        rs.getInt("Capacity")
                );

                h.setStatus(
                        rs.getString("Status")
                );

                list.add(h);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }
    
}