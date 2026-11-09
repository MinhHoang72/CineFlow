package dao;
import utils.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.entity.Surcharge;

public class SurchargeDAO extends DBContext {

    public java.util.List<java.util.Map<String,Object>> policies(int centreId,String type) throws SQLException {
        var rows=new java.util.ArrayList<java.util.Map<String,Object>>();
        try(Connection c=DBContext.getConnection();PreparedStatement ps=c.prepareStatement("SELECT p.*,u.FullName UpdatedByName,CASE WHEN p.EffectiveTo IS NOT NULL AND (p.EffectiveTo<=p.EffectiveFrom OR p.EffectiveTo<=SYSDATETIME()) THEN N'Đã kết thúc' WHEN p.EffectiveFrom>SYSDATETIME() THEN N'Chờ áp dụng' ELSE N'Đang áp dụng' END PolicyState FROM CentreSurcharges p LEFT JOIN Users u ON u.UserID=p.UpdatedBy WHERE p.CentreID=? AND p.SurchargeType=? ORDER BY p.EffectiveFrom DESC,p.SurchargeID DESC")) {
            ps.setInt(1,centreId);ps.setString(2,type);
            try(ResultSet rs=ps.executeQuery()){while(rs.next()){
                var row=new java.util.LinkedHashMap<String,Object>();
                for(String key:new String[]{"SurchargeID","TargetValue","SurchargeName","Amount","StartTime","EndTime","EffectiveFrom","EffectiveTo","UpdatedByName","PolicyState"})row.put(key,rs.getObject(key));
                rows.add(row);
            }}
        }
        return rows;
    }

    public java.util.Map<String,String> targets(int centreId,String type) throws SQLException {
        String sql=switch(type){
            case "CENTRE" -> "SELECT CAST(CentreID AS VARCHAR(30)),CentreName FROM Centres WHERE CentreID=?";
            case "HALL" -> "SELECT DISTINCT HallType,HallType FROM Halls WHERE CentreID=?";
            case "SEAT" -> "SELECT DISTINCT s.SeatType,s.SeatType FROM Seats s JOIN Halls h ON h.HallID=s.HallID WHERE h.CentreID=?";
            case "MOVIE" -> "SELECT CAST(m.MovieID AS VARCHAR(30)),m.Title FROM Movies m JOIN CentreMovies cm ON cm.MovieID=m.MovieID WHERE cm.CentreID=? AND cm.IsActive=1";
            case "TIME" -> null;
            default -> throw new IllegalArgumentException("Loại phụ thu không hợp lệ.");
        };
        var result=new java.util.LinkedHashMap<String,String>();
        if(sql==null)return result;
        try(Connection c=DBContext.getConnection();PreparedStatement ps=c.prepareStatement(sql)){
            ps.setInt(1,centreId);try(ResultSet rs=ps.executeQuery()){while(rs.next())result.put(rs.getString(1),rs.getString(2));}
        }
        return result;
    }

    /** Serialize revisions per centre. Old versions are closed, never overwritten. */
    public void savePolicy(int centreId,int userId,String type,String target,String name,java.math.BigDecimal amount,
            Timestamp from,Time start,Time end,Integer editId) throws SQLException {
        try(Connection c=DBContext.getConnection()){
            c.setAutoCommit(false);
            try{
                try(PreparedStatement ps=c.prepareStatement("SELECT CentreID FROM Centres WITH(UPDLOCK,HOLDLOCK) WHERE CentreID=?")){
                    ps.setInt(1,centreId);try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new IllegalArgumentException("Không tìm thấy cụm rạp.");}
                }
                Timestamp now;try(Statement st=c.createStatement();ResultSet rs=st.executeQuery("SELECT SYSDATETIME()")){rs.next();now=rs.getTimestamp(1);}
                if(from==null)from=now;else if(!from.after(now))throw new IllegalArgumentException("Thời gian áp dụng phải nằm trong tương lai.");
                if(editId!=null){
                    try(PreparedStatement ps=c.prepareStatement("SELECT TargetValue FROM CentreSurcharges WHERE SurchargeID=? AND CentreID=? AND SurchargeType=? AND (EffectiveTo IS NULL OR EffectiveTo>SYSDATETIME())")){
                        ps.setInt(1,editId);ps.setInt(2,centreId);ps.setString(3,type);
                        try(ResultSet rs=ps.executeQuery()){if(!rs.next()||!target.equals(rs.getString(1)))throw new IllegalArgumentException("Chỉ được sửa chính sách cùng đối tượng thuộc rạp của bạn.");}
                    }
                }
                Timestamp next=null;
                try(PreparedStatement ps=c.prepareStatement("SELECT TOP 1 EffectiveFrom FROM CentreSurcharges WHERE CentreID=? AND SurchargeType=? AND TargetValue=? AND EffectiveFrom>=? AND (EffectiveTo IS NULL OR EffectiveTo>EffectiveFrom) AND SurchargeID<>? ORDER BY EffectiveFrom")){
                    ps.setInt(1,centreId);ps.setString(2,type);ps.setString(3,target);ps.setTimestamp(4,from);ps.setInt(5,editId==null?-1:editId);
                    try(ResultSet rs=ps.executeQuery()){if(rs.next())next=rs.getTimestamp(1);}
                }
                if(from.equals(next))throw new IllegalArgumentException("Đã có chính sách bắt đầu tại thời điểm này.");
                if("TIME".equals(type)){
                    try(PreparedStatement ps=c.prepareStatement("SELECT 1 FROM CentreSurcharges WHERE CentreID=? AND SurchargeType='TIME' AND TargetValue<>? AND StartTime<CAST(? AS TIME) AND EndTime>CAST(? AS TIME) AND (EffectiveTo IS NULL OR EffectiveTo>?) AND (? IS NULL OR EffectiveFrom<?)")){
                        ps.setInt(1,centreId);ps.setString(2,target);ps.setTime(3,end);ps.setTime(4,start);ps.setTimestamp(5,from);ps.setTimestamp(6,next);ps.setTimestamp(7,next);
                        try(ResultSet rs=ps.executeQuery()){if(rs.next())throw new IllegalArgumentException("Khung giờ trùng với chính sách khác trong thời gian áp dụng.");}
                    }
                }
                try(PreparedStatement ps=c.prepareStatement("UPDATE CentreSurcharges SET EffectiveTo=CASE WHEN EffectiveFrom>? THEN EffectiveFrom ELSE ? END WHERE CentreID=? AND SurchargeType=? AND TargetValue=? AND (EffectiveTo IS NULL OR EffectiveTo>?) AND (EffectiveFrom<? OR SurchargeID=?)")){
                    ps.setTimestamp(1,from);ps.setTimestamp(2,from);ps.setInt(3,centreId);ps.setString(4,type);ps.setString(5,target);ps.setTimestamp(6,from);ps.setTimestamp(7,from);ps.setInt(8,editId==null?-1:editId);ps.executeUpdate();
                }
                try(PreparedStatement ps=c.prepareStatement("INSERT CentreSurcharges(CentreID,SurchargeType,TargetValue,SurchargeName,Amount,StartTime,EndTime,EffectiveFrom,EffectiveTo,UpdatedBy) VALUES(?,?,?,?,?,?,?,?,?,?)")){
                    ps.setInt(1,centreId);ps.setString(2,type);ps.setString(3,target);ps.setString(4,name);ps.setBigDecimal(5,amount);ps.setTime(6,start);ps.setTime(7,end);ps.setTimestamp(8,from);ps.setTimestamp(9,next);ps.setInt(10,userId);ps.executeUpdate();
                }
                c.commit();
            }catch(Exception e){c.rollback();throw e;}
        }
    }

    public void endPolicy(int centreId,int id) throws SQLException {
        try(Connection c=DBContext.getConnection()){
            c.setAutoCommit(false);
            try{
                try(PreparedStatement ps=c.prepareStatement("SELECT CentreID FROM Centres WITH(UPDLOCK,HOLDLOCK) WHERE CentreID=?")){ps.setInt(1,centreId);ps.executeQuery().close();}
                try(PreparedStatement ps=c.prepareStatement("UPDATE CentreSurcharges SET EffectiveTo=CASE WHEN EffectiveFrom>SYSDATETIME() THEN EffectiveFrom ELSE SYSDATETIME() END WHERE CentreID=? AND SurchargeID=? AND (EffectiveTo IS NULL OR EffectiveTo>SYSDATETIME())")){
                    ps.setInt(1,centreId);ps.setInt(2,id);if(ps.executeUpdate()!=1)throw new IllegalArgumentException("Không tìm thấy phụ thu còn hiệu lực trong rạp của bạn.");
                }
                c.commit();
            }catch(Exception e){c.rollback();throw e;}
        }
    }

    /** Surcharges are shared globally, so list types configured across all halls/seats. */
    public List<String> getConfiguredTargets(String type) throws SQLException {
        String sql;
        if ("SEAT".equals(type))
            sql = "SELECT DISTINCT SeatType AS TargetValue FROM Seats WHERE SeatType <> 'STANDARD' ORDER BY SeatType";
        else if ("HALL".equals(type))
            sql = "SELECT DISTINCT HallType AS TargetValue FROM Halls ORDER BY HallType";
        else throw new IllegalArgumentException("Loại phụ thu không hợp lệ.");
        List<String> targets = new ArrayList<>();
        try (Connection c = DBContext.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) targets.add(rs.getString("TargetValue"));
        }
        return targets;
    }

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

        s.setUpdatedByName(rs.getString("UpdatedByName"));
        return s;
    }

    public List<Surcharge> getCurrentRules() {

        List<Surcharge> list =
                new ArrayList<>();

        String sql =
                "SELECT p.*, u.FullName AS UpdatedByName "
                + "FROM Surcharges p LEFT JOIN Users u ON u.UserID = p.UpdatedBy "
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
                "SELECT p.*, u.FullName AS UpdatedByName "
                + "FROM Surcharges p LEFT JOIN Users u ON u.UserID = p.UpdatedBy "
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
                "SELECT p.*, u.FullName AS UpdatedByName "
                + "FROM Surcharges p LEFT JOIN Users u ON u.UserID = p.UpdatedBy "
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

    /** Prevent duplicate active rules even when two forms are submitted concurrently. */
    public boolean createSurcharge(Surcharge rule) {
        try (Connection c = DBContext.getConnection()) {
            c.setAutoCommit(false);
            try {
                boolean inserted = createSurcharge(c, rule);
                c.commit();
                return inserted;
            } catch (SQLException | RuntimeException e) {
                c.rollback(); throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không thể thêm phụ thu.", e);
        }
    }

    public boolean createSurcharge(Connection c, Surcharge rule) throws SQLException {
        if (c.getAutoCommit()) throw new SQLException("Surcharge changes require a transaction");
        try (PreparedStatement ps = c.prepareStatement("SELECT SurchargeID FROM Surcharges WITH (UPDLOCK,HOLDLOCK) WHERE SurchargeType=? AND TargetValue=? AND (EffectiveTo IS NULL OR EffectiveTo>SYSDATETIME())")) {
            ps.setString(1, rule.getSurchargeType()); ps.setString(2, rule.getTargetValue());
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return false; }
        }
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO Surcharges (SurchargeName,SurchargeType,TargetValue,Amount,EffectiveFrom,UpdatedBy) VALUES (?,?,?,?,SYSDATETIME(),?)")) {
            ps.setString(1, rule.getSurchargeName()); ps.setString(2, rule.getSurchargeType());
            ps.setString(3, rule.getTargetValue()); ps.setDouble(4, rule.getAmount());
            ps.setInt(5, rule.getUpdatedBy()); return ps.executeUpdate() == 1;
        }
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
