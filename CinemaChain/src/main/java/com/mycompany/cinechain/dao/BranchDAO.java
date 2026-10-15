package com.mycompany.cinechain.dao;

import com.mycompany.cinechain.model.Branch;
import com.mycompany.cinechain.utils.DBContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BranchDAO {

    public List<Branch> getActiveBranches() throws SQLException {
        String sql = "SELECT BranchID, BranchName, Address, City FROM Branches "
                   + "WHERE Status = 'ACTIVE' ORDER BY City, BranchName";
        List<Branch> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Branch(rs.getInt("BranchID"), rs.getString("BranchName"),
                        rs.getString("Address"), rs.getString("City")));
            }
        }
        return list;
    }
}
