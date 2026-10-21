package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import model.dto.report.OccupancyRowDTO;
import model.dto.report.OperationsSummaryDTO;
import model.dto.common.PageDTO;
import model.dto.showtime.ShowtimeStatDTO;
import model.dto.checkin.StaffScanDTO;
import utils.DBContext;

public class OperationsReportDAO {

    private static final String SHOWTIME_FROM
            = " FROM Showtimes st "
            + "JOIN Movies m ON m.MovieID = st.MovieID "
            + "JOIN Halls h ON h.HallID = st.HallID "
            + "JOIN Centres c ON c.CentreID = h.CentreID "
            + "LEFT JOIN (SELECT b.ShowtimeID, COUNT(bs.BookingSeatID) AS sold, "
            + "COUNT(CASE WHEN t.TicketStatus = 'USED' THEN 1 END) AS used "
            + "FROM Bookings b JOIN BookingSeats bs ON bs.BookingID = b.BookingID "
            + "LEFT JOIN Tickets t ON t.BookingSeatID = bs.BookingSeatID "
            + "WHERE b.BookingStatus IN ('CONFIRMED','COMPLETED') GROUP BY b.ShowtimeID) x ON x.ShowtimeID = st.ShowtimeID "
            + "WHERE st.Status <> 'CANCELLED' AND st.StartTime >= ? AND st.StartTime < ?";

    private int bind(PreparedStatement statement, int index, LocalDate from, LocalDate to, Integer centreId) throws SQLException {
        statement.setTimestamp(index++, Timestamp.valueOf(from.atStartOfDay()));
        statement.setTimestamp(index++, Timestamp.valueOf(to.atStartOfDay()));
        if (centreId != null) {
            statement.setInt(index++, centreId);
        }
        return index;
    }

    private String showtimeScope(Integer centreId) {
        return centreId == null ? "" : " AND h.CentreID = ?";
    }

    public OperationsSummaryDTO summary(LocalDate from, LocalDate to, Integer centreId) {
        int showtimes = 0;
        int capacity = 0;
        int sold = 0;
        int used = 0;
        int rejected = 0;
        int outOfStock = 0;
        String showtimeSql = "SELECT COUNT(*), ISNULL(SUM(h.Capacity), 0), ISNULL(SUM(ISNULL(x.sold, 0)), 0), "
                + "ISNULL(SUM(ISNULL(x.used, 0)), 0)" + SHOWTIME_FROM + showtimeScope(centreId);
        String scanSql = "SELECT COUNT(*) FROM CheckInLogs l JOIN Users u ON u.UserID = l.CheckedBy "
                + "WHERE l.Result = 'REJECTED' AND l.CheckInTime >= ? AND l.CheckInTime < ?"
                + (centreId == null ? "" : " AND u.CentreID = ?");
        String stockSql = "SELECT COUNT(*) FROM FnbItems i "
                + "LEFT JOIN CentreFnbInventory inv ON inv.FnbItemID = i.FnbItemID AND inv.CentreID = i.CentreID "
                + "WHERE i.Status = 'ACTIVE' AND i.CentreID IS NOT NULL AND ISNULL(inv.IsAvailable, 0) = 0"
                + (centreId == null ? "" : " AND i.CentreID = ?");
        try (Connection connection = DBContext.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(showtimeSql)) {
                bind(statement, 1, from, to, centreId);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        showtimes = rs.getInt(1);
                        capacity = rs.getInt(2);
                        sold = rs.getInt(3);
                        used = rs.getInt(4);
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(scanSql)) {
                bind(statement, 1, from, to, centreId);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        rejected = rs.getInt(1);
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(stockSql)) {
                if (centreId != null) {
                    statement.setInt(1, centreId);
                }
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        outOfStock = rs.getInt(1);
                    }
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return new OperationsSummaryDTO(showtimes, capacity, sold, used, rejected, outOfStock);
    }

    public PageDTO<ShowtimeStatDTO> showtimes(LocalDate from, LocalDate to, Integer centreId, int page, int size) {
        List<ShowtimeStatDTO> rows = new ArrayList<>();
        int total = 0;
        try (Connection connection = DBContext.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*)" + SHOWTIME_FROM + showtimeScope(centreId))) {
                bind(statement, 1, from, to, centreId);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        total = rs.getInt(1);
                    }
                }
            }
            int pages = Math.max(1, (total + size - 1) / size);
            int current = Math.min(Math.max(1, page), pages);
            String sql = "SELECT m.Title, c.CentreName, h.HallName, st.StartTime, h.Capacity, "
                    + "ISNULL(x.sold, 0) AS sold, ISNULL(x.used, 0) AS used"
                    + SHOWTIME_FROM + showtimeScope(centreId)
                    + " ORDER BY st.StartTime DESC, st.ShowtimeID DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                int index = bind(statement, 1, from, to, centreId);
                statement.setInt(index++, (current - 1) * size);
                statement.setInt(index, size);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        rows.add(new ShowtimeStatDTO(rs.getString(1), rs.getString(2), rs.getString(3),
                                rs.getTimestamp(4), rs.getInt(5), rs.getInt("sold"), rs.getInt("used")));
                    }
                }
            }
            return new PageDTO<>(rows, current, size, total);
        } catch (SQLException exception) {
            exception.printStackTrace();
            return new PageDTO<>(rows, 1, size, 0);
        }
    }

    public List<StaffScanDTO> scansByStaff(LocalDate from, LocalDate to, Integer centreId) {
        List<StaffScanDTO> rows = new ArrayList<>();
        String sql = "SELECT u.FullName, COUNT(CASE WHEN l.Result = 'SUCCESS' THEN 1 END) AS ok, "
                + "COUNT(CASE WHEN l.Result = 'REJECTED' THEN 1 END) AS bad "
                + "FROM CheckInLogs l JOIN Users u ON u.UserID = l.CheckedBy "
                + "WHERE l.CheckInTime >= ? AND l.CheckInTime < ?" + (centreId == null ? "" : " AND u.CentreID = ?")
                + " GROUP BY u.FullName ORDER BY COUNT(*) DESC";
        try (Connection connection = DBContext.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, 1, from, to, centreId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new StaffScanDTO(rs.getString(1), rs.getInt("ok"), rs.getInt("bad")));
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return rows;
    }

    public List<OccupancyRowDTO> occupancyByCentre(LocalDate from, LocalDate to) {
        List<OccupancyRowDTO> rows = new ArrayList<>();
        String sql = "SELECT c.CentreName, ISNULL(SUM(h.Capacity), 0) AS capacity, ISNULL(SUM(ISNULL(x.sold, 0)), 0) AS sold"
                + SHOWTIME_FROM + " GROUP BY c.CentreName ORDER BY c.CentreName";
        try (Connection connection = DBContext.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, 1, from, to, null);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new OccupancyRowDTO(rs.getString(1), rs.getInt("capacity"), rs.getInt("sold")));
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return rows;
    }

    public int activeCentres() {
        return count("SELECT COUNT(*) FROM Centres WHERE Status = 'ACTIVE'");
    }

    public int activeCustomers() {
        return count("SELECT COUNT(*) FROM Users WHERE Role = 'CUSTOMER' AND Status = 'ACTIVE'");
    }

    private int count(String sql) {
        try (Connection connection = DBContext.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException exception) {
            exception.printStackTrace();
            return 0;
        }
    }
}
