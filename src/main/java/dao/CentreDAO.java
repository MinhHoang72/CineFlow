package dao;

import model.Centre;
import utils.DBContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Danh sach rap dang hoat dong cho buoc chon rap khi dat ve. */
public class CentreDAO {

    public List<Centre> getActiveCentres() throws SQLException {
        String sql = "SELECT CentreID, CentreName, Address, City FROM Centres "
                   + "WHERE Status = 'ACTIVE' ORDER BY City, CentreName";
        List<Centre> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Centre centre = new Centre();
                centre.setCentreId(rs.getInt("CentreID"));
                centre.setCentreName(rs.getString("CentreName"));
                centre.setAddress(rs.getString("Address"));
                centre.setCity(rs.getString("City"));
                list.add(centre);
            }
        }
        return list;
    }
}
