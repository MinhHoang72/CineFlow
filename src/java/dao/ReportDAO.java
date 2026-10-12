package dao;

import model.ReportSummary;
import model.RevenueReport;
import model.ShowtimeMetric;
import utils.DBContext;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReportDAO {
    private static final String VALID_BOOKINGS = "('CONFIRMED','COMPLETED')";

    public ReportSummary getSummary() {
        ReportSummary s = new ReportSummary();
        String sql = "SELECT "
                + "(SELECT COALESCE(SUM(TotalAmount),0) FROM Bookings WHERE BookingStatus IN " + VALID_BOOKINGS + ") Revenue, "
                + "(SELECT COUNT(*) FROM Bookings WHERE BookingStatus IN " + VALID_BOOKINGS + ") Bookings, "
                + "(SELECT COUNT(*) FROM BookingSeats bs JOIN Bookings b ON b.BookingID=bs.BookingID WHERE b.BookingStatus IN " + VALID_BOOKINGS + ") Tickets, "
                + "(SELECT COUNT(*) FROM Movies WHERE Status IN ('COMING_SOON','NOW_SHOWING')) ActiveMovies, "
                + "(SELECT COUNT(*) FROM Branches WHERE Status='ACTIVE') ActiveBranches, "
                + "(SELECT COUNT(*) FROM Users WHERE Status='ACTIVE') ActiveUsers, "
                + "(SELECT COALESCE(100.0 * SUM(CASE WHEN bs.SeatStatus IN ('HELD','CONFIRMED') THEN 1 ELSE 0 END) / NULLIF(COUNT(*),0),0) "
                + "FROM Showtimes st JOIN Halls h ON h.HallID=st.HallID LEFT JOIN Seats se ON se.HallID=h.HallID "
                + "LEFT JOIN BookingSeats bs ON bs.SeatID=se.SeatID JOIN Bookings b ON b.BookingID=bs.BookingID AND b.ShowtimeID=st.ShowtimeID "
                + "WHERE b.BookingStatus IN " + VALID_BOOKINGS + ") Occupancy";
        try (Connection c=DBContext.getConnection(); PreparedStatement p=c.prepareStatement(sql); ResultSet r=p.executeQuery()) {
            if(r.next()){ s.setRevenue(r.getBigDecimal("Revenue")); s.setBookings(r.getInt("Bookings")); s.setTickets(r.getInt("Tickets")); s.setActiveMovies(r.getInt("ActiveMovies")); s.setActiveBranches(r.getInt("ActiveBranches")); s.setActiveUsers(r.getInt("ActiveUsers")); s.setOccupancy(r.getDouble("Occupancy")); }
        } catch(Exception e){ e.printStackTrace(); }
        return s;
    }

    public List<RevenueReport> revenueByBranch() {
        String sql="SELECT br.BranchName Name, br.City, COUNT(x.BookingID) Bookings, COALESCE(SUM(x.Tickets),0) Tickets, COALESCE(SUM(x.TotalAmount),0) Revenue FROM Branches br LEFT JOIN (SELECT b.BookingID,b.TotalAmount,st.HallID,COUNT(bs.BookingSeatID) Tickets FROM Bookings b JOIN Showtimes st ON st.ShowtimeID=b.ShowtimeID LEFT JOIN BookingSeats bs ON bs.BookingID=b.BookingID WHERE b.BookingStatus IN "+VALID_BOOKINGS+" GROUP BY b.BookingID,b.TotalAmount,st.HallID) x ON x.HallID IN (SELECT HallID FROM Halls WHERE BranchID=br.BranchID) GROUP BY br.BranchName,br.City ORDER BY Revenue DESC";
        return revenueList(sql);
    }

    public List<RevenueReport> revenueByMovie() {
        String sql="SELECT m.Title Name, NULL City, COUNT(x.BookingID) Bookings, COALESCE(SUM(x.Tickets),0) Tickets, COALESCE(SUM(x.TotalAmount),0) Revenue FROM Movies m LEFT JOIN (SELECT b.BookingID,b.TotalAmount,st.MovieID,COUNT(bs.BookingSeatID) Tickets FROM Bookings b JOIN Showtimes st ON st.ShowtimeID=b.ShowtimeID LEFT JOIN BookingSeats bs ON bs.BookingID=b.BookingID WHERE b.BookingStatus IN "+VALID_BOOKINGS+" GROUP BY b.BookingID,b.TotalAmount,st.MovieID) x ON x.MovieID=m.MovieID GROUP BY m.Title ORDER BY Revenue DESC";
        return revenueList(sql);
    }

    private List<RevenueReport> revenueList(String sql) {
        List<RevenueReport> list=new ArrayList<>();
        try(Connection c=DBContext.getConnection(); PreparedStatement p=c.prepareStatement(sql); ResultSet r=p.executeQuery()){ while(r.next()){ RevenueReport x=new RevenueReport(); x.setName(r.getString("Name")); x.setCity(r.getString("City")); x.setBookings(r.getInt("Bookings")); x.setTickets(r.getInt("Tickets")); x.setRevenue(r.getBigDecimal("Revenue")); list.add(x); } } catch(Exception e){e.printStackTrace();} return list;
    }

    public List<ShowtimeMetric> occupancyByShowtime() {
        String sql="SELECT m.Title MovieTitle, br.BranchName, h.HallName, st.StartTime, h.Capacity, COUNT(CASE WHEN bs.SeatStatus IN ('HELD','CONFIRMED') AND b.BookingStatus IN "+VALID_BOOKINGS+" THEN 1 END) SoldSeats FROM Showtimes st JOIN Movies m ON m.MovieID=st.MovieID JOIN Halls h ON h.HallID=st.HallID JOIN Branches br ON br.BranchID=h.BranchID LEFT JOIN BookingSeats bs ON bs.SeatID IN (SELECT SeatID FROM Seats WHERE HallID=h.HallID) LEFT JOIN Bookings b ON b.BookingID=bs.BookingID AND b.ShowtimeID=st.ShowtimeID GROUP BY m.Title,br.BranchName,h.HallName,st.StartTime,h.Capacity ORDER BY st.StartTime DESC";
        List<ShowtimeMetric> list=new ArrayList<>();
        try(Connection c=DBContext.getConnection(); PreparedStatement p=c.prepareStatement(sql); ResultSet r=p.executeQuery()){ while(r.next()){ ShowtimeMetric x=new ShowtimeMetric(); x.setMovieTitle(r.getString("MovieTitle")); x.setBranchName(r.getString("BranchName")); x.setHallName(r.getString("HallName")); x.setStartTime(r.getTimestamp("StartTime")); x.setCapacity(r.getInt("Capacity")); x.setSoldSeats(r.getInt("SoldSeats")); x.setOccupancy(x.getCapacity()==0?0:100.0*x.getSoldSeats()/x.getCapacity()); list.add(x); } } catch(Exception e){e.printStackTrace();} return list;
    }
}
