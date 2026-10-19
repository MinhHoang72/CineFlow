package dao;

import model.ReportSummary;
import model.RevenueReport;
import model.ShowtimeMetric;
import utils.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO: Truy xuất dữ liệu báo cáo thống kê, doanh thu và hiệu suất lấp đầy phòng chiếu.
 */
public class ReportDAO {

    private static final String VALID_BOOKINGS = "('CONFIRMED','COMPLETED')";

    /**
     * Lấy dữ liệu tổng quan toàn hệ thống (doanh thu, số vé, rạp hoạt động, tỷ lệ lấp đầy).
     *
     * @return đối tượng ReportSummary chứa các chỉ số KPI tổng hợp
     */
    public ReportSummary getSummary() {
        ReportSummary summary = new ReportSummary();
        // 1. Câu truy vấn tổng hợp đa bảng lấy toàn bộ chỉ số vận hành
        String sql = "SELECT "
                + "(SELECT COALESCE(SUM(TotalAmount),0) FROM Bookings WHERE BookingStatus IN " + VALID_BOOKINGS + ") Revenue, "
                + "(SELECT COUNT(*) FROM Bookings WHERE BookingStatus IN " + VALID_BOOKINGS + ") Bookings, "
                + "(SELECT COUNT(*) FROM BookingSeats bs JOIN Bookings b ON b.BookingID=bs.BookingID WHERE b.BookingStatus IN " + VALID_BOOKINGS + ") Tickets, "
                + "(SELECT COUNT(*) FROM Movies WHERE Status IN ('COMING_SOON','NOW_SHOWING')) ActiveMovies, "
                + "(SELECT COUNT(*) FROM Centres WHERE Status='ACTIVE') ActiveCentres, "
                + "(SELECT COUNT(*) FROM Users WHERE Status='ACTIVE') ActiveUsers, "
                + "(SELECT COALESCE(100.0 * SUM(CASE WHEN bs.SeatStatus IN ('HELD','CONFIRMED') THEN 1 ELSE 0 END) / NULLIF(COUNT(*),0),0) "
                + "FROM Showtimes st JOIN Halls h ON h.HallID=st.HallID LEFT JOIN Seats se ON se.HallID=h.HallID "
                + "LEFT JOIN BookingSeats bs ON bs.SeatID=se.SeatID JOIN Bookings b ON b.BookingID=bs.BookingID AND b.ShowtimeID=st.ShowtimeID "
                + "WHERE b.BookingStatus IN " + VALID_BOOKINGS + ") Occupancy";

        // 2. Thực thi câu truy vấn và ánh xạ kết quả vào model
        try (Connection connection = DBContext.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                summary.setRevenue(resultSet.getBigDecimal("Revenue"));
                summary.setBookings(resultSet.getInt("Bookings"));
                summary.setTickets(resultSet.getInt("Tickets"));
                summary.setActiveMovies(resultSet.getInt("ActiveMovies"));
                summary.setActiveCentres(resultSet.getInt("ActiveCentres"));
                summary.setActiveUsers(resultSet.getInt("ActiveUsers"));
                summary.setOccupancy(resultSet.getDouble("Occupancy"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return summary;
    }

    /**
     * Báo cáo doanh thu và số lượng vé phân bổ theo từng trung tâm chiếu phim.
     *
     * @return danh sách doanh thu theo cụm rạp
     */
    public List<RevenueReport> revenueByCentre() {
        String sql = "SELECT c.CentreName Name, c.City, COUNT(x.BookingID) Bookings, COALESCE(SUM(x.Tickets),0) Tickets, COALESCE(SUM(x.TotalAmount),0) Revenue "
                + "FROM Centres c LEFT JOIN (SELECT b.BookingID,b.TotalAmount,st.HallID,COUNT(bs.BookingSeatID) Tickets FROM Bookings b JOIN Showtimes st ON st.ShowtimeID=b.ShowtimeID LEFT JOIN BookingSeats bs ON bs.BookingID=b.BookingID WHERE b.BookingStatus IN " + VALID_BOOKINGS + " GROUP BY b.BookingID,b.TotalAmount,st.HallID) x ON x.HallID IN (SELECT HallID FROM Halls WHERE CentreID=c.CentreID) "
                + "GROUP BY c.CentreName,c.City ORDER BY Revenue DESC";
        return revenueList(sql);
    }

    /**
     * Báo cáo doanh thu và số lượng vé phân bổ theo từng bộ phim.
     *
     * @return danh sách doanh thu theo phim
     */
    public List<RevenueReport> revenueByMovie() {
        String sql = "SELECT m.Title Name, NULL City, COUNT(x.BookingID) Bookings, COALESCE(SUM(x.Tickets),0) Tickets, COALESCE(SUM(x.TotalAmount),0) Revenue FROM Movies m LEFT JOIN (SELECT b.BookingID,b.TotalAmount,st.MovieID,COUNT(bs.BookingSeatID) Tickets FROM Bookings b JOIN Showtimes st ON st.ShowtimeID=b.ShowtimeID LEFT JOIN BookingSeats bs ON bs.BookingID=b.BookingID WHERE b.BookingStatus IN " + VALID_BOOKINGS + " GROUP BY b.BookingID,b.TotalAmount,st.MovieID) x ON x.MovieID=m.MovieID GROUP BY m.Title ORDER BY Revenue DESC";
        return revenueList(sql);
    }

    /**
     * Phương thức tiện ích: Thực thi truy vấn doanh thu và chuyển đổi tập kết quả thành danh sách RevenueReport.
     *
     * @param sql câu lệnh SQL truy vấn
     * @return danh sách đối tượng RevenueReport
     */
    private List<RevenueReport> revenueList(String sql) {
        List<RevenueReport> list = new ArrayList<>();
        try (Connection connection = DBContext.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                RevenueReport report = new RevenueReport();
                report.setName(resultSet.getString("Name"));
                report.setCity(resultSet.getString("City"));
                report.setBookings(resultSet.getInt("Bookings"));
                report.setTickets(resultSet.getInt("Tickets"));
                report.setRevenue(resultSet.getBigDecimal("Revenue"));
                list.add(report);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Thống kê tỷ lệ lấp đầy ghế ngồi theo từng suất chiếu.
     *
     * @return danh sách chỉ số lấp đầy của các suất chiếu
     */
    public List<ShowtimeMetric> occupancyByShowtime() {
        // 1. Câu truy vấn nhóm theo từng suất chiếu để tính toán số ghế đã bán
        String sql = "SELECT m.Title MovieTitle, c.CentreName, h.HallName, st.StartTime, h.Capacity, COUNT(CASE WHEN bs.SeatStatus IN ('HELD','CONFIRMED') AND b.BookingStatus IN " + VALID_BOOKINGS + " THEN 1 END) SoldSeats "
                + "FROM Showtimes st JOIN Movies m ON m.MovieID=st.MovieID JOIN Halls h ON h.HallID=st.HallID JOIN Centres c ON c.CentreID=h.CentreID LEFT JOIN BookingSeats bs ON bs.SeatID IN (SELECT SeatID FROM Seats WHERE HallID=h.HallID) LEFT JOIN Bookings b ON b.BookingID=bs.BookingID AND b.ShowtimeID=st.ShowtimeID "
                + "GROUP BY m.Title,c.CentreName,h.HallName,st.StartTime,h.Capacity ORDER BY st.StartTime DESC";
        List<ShowtimeMetric> list = new ArrayList<>();

        // 2. Thực thi và tính toán tỷ lệ lấp đầy (Occupancy %)
        try (Connection connection = DBContext.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                ShowtimeMetric metric = new ShowtimeMetric();
                metric.setMovieTitle(resultSet.getString("MovieTitle"));
                metric.setCentreName(resultSet.getString("CentreName"));
                metric.setHallName(resultSet.getString("HallName"));
                metric.setStartTime(resultSet.getTimestamp("StartTime"));
                int capacity = resultSet.getInt("Capacity");
                int soldSeats = resultSet.getInt("SoldSeats");
                metric.setCapacity(capacity);
                metric.setSoldSeats(soldSeats);
                metric.setOccupancy(capacity == 0 ? 0.0 : (100.0 * soldSeats / capacity));
                list.add(metric);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
