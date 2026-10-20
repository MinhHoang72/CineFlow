package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.dto.FnbSaleDTO;
import model.dto.SalesRowDTO;
import model.dto.SalesSummaryDTO;
import utils.DBContext;

public class SalesReportDAO {

    private static final String TICKET_FROM
            = " FROM Bookings b "
            + "JOIN Showtimes st ON st.ShowtimeID = b.ShowtimeID "
            + "JOIN Halls h ON h.HallID = st.HallID "
            + "JOIN Centres c ON c.CentreID = h.CentreID "
            + "JOIN Movies m ON m.MovieID = st.MovieID "
            + "OUTER APPLY (SELECT COUNT(*) AS n FROM BookingSeats s WHERE s.BookingID = b.BookingID) tk "
            + "WHERE b.BookingStatus IN ('CONFIRMED','COMPLETED') AND b.CreatedAt >= ? AND b.CreatedAt < ?";

    private static final String FNB_FROM
            = " FROM FnbOrders f JOIN Centres c ON c.CentreID = f.CentreID "
            + "WHERE f.OrderStatus IN ('PREPARING','READY','DELIVERED') AND f.CreatedAt >= ? AND f.CreatedAt < ?";

    private int bind(PreparedStatement statement, int index, LocalDate from, LocalDate to, Integer centreId) throws SQLException {
        statement.setTimestamp(index++, Timestamp.valueOf(from.atStartOfDay()));
        statement.setTimestamp(index++, Timestamp.valueOf(to.atStartOfDay()));
        if (centreId != null) {
            statement.setInt(index++, centreId);
        }
        return index;
    }

    private String ticketScope(Integer centreId) {
        return centreId == null ? "" : " AND h.CentreID = ?";
    }

    private String fnbScope(Integer centreId) {
        return centreId == null ? "" : " AND f.CentreID = ?";
    }

    public SalesSummaryDTO summary(LocalDate from, LocalDate to, Integer centreId) {
        BigDecimal ticketRevenue = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal fnbRevenue = BigDecimal.ZERO;
        int bookings = 0;
        int tickets = 0;
        int voucherBookings = 0;
        String ticketSql = "SELECT COUNT(*) AS bookings, ISNULL(SUM(b.TotalAmount), 0) AS revenue, "
                + "ISNULL(SUM(b.DiscountAmount), 0) AS discount, "
                + "COUNT(CASE WHEN b.VoucherID IS NOT NULL THEN 1 END) AS vouchers, ISNULL(SUM(tk.n), 0) AS tickets"
                + TICKET_FROM + ticketScope(centreId);
        String fnbSql = "SELECT ISNULL(SUM(f.TotalAmount), 0)" + FNB_FROM + fnbScope(centreId);
        try (Connection connection = DBContext.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(ticketSql)) {
                bind(statement, 1, from, to, centreId);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        bookings = rs.getInt("bookings");
                        ticketRevenue = rs.getBigDecimal("revenue");
                        discount = rs.getBigDecimal("discount");
                        voucherBookings = rs.getInt("vouchers");
                        tickets = rs.getInt("tickets");
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(fnbSql)) {
                bind(statement, 1, from, to, centreId);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        fnbRevenue = rs.getBigDecimal(1);
                    }
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return new SalesSummaryDTO(ticketRevenue, fnbRevenue, discount, bookings, tickets, voucherBookings);
    }

    public List<SalesRowDTO> byDay(LocalDate from, LocalDate to, Integer centreId) {
        Map<LocalDate, int[]> counts = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal[]> money = new LinkedHashMap<>();
        for (LocalDate day = from; day.isBefore(to); day = day.plusDays(1)) {
            counts.put(day, new int[2]);
            money.put(day, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }
        String ticketSql = "SELECT CAST(b.CreatedAt AS date) AS d, COUNT(*) AS bookings, SUM(tk.n) AS tickets, SUM(b.TotalAmount) AS revenue"
                + TICKET_FROM + ticketScope(centreId) + " GROUP BY CAST(b.CreatedAt AS date)";
        String fnbSql = "SELECT CAST(f.CreatedAt AS date) AS d, SUM(f.TotalAmount) AS revenue"
                + FNB_FROM + fnbScope(centreId) + " GROUP BY CAST(f.CreatedAt AS date)";
        try (Connection connection = DBContext.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(ticketSql)) {
                bind(statement, 1, from, to, centreId);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        LocalDate day = rs.getDate("d").toLocalDate();
                        if (counts.containsKey(day)) {
                            counts.get(day)[0] = rs.getInt("bookings");
                            counts.get(day)[1] = rs.getInt("tickets");
                            money.get(day)[0] = rs.getBigDecimal("revenue");
                        }
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(fnbSql)) {
                bind(statement, 1, from, to, centreId);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        LocalDate day = rs.getDate("d").toLocalDate();
                        if (money.containsKey(day)) {
                            money.get(day)[1] = rs.getBigDecimal("revenue");
                        }
                    }
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        DateTimeFormatter format = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        List<SalesRowDTO> rows = new ArrayList<>();
        for (LocalDate day : counts.keySet()) {
            rows.add(new SalesRowDTO(day.format(format), counts.get(day)[0], counts.get(day)[1],
                    money.get(day)[0], money.get(day)[1]));
        }
        return rows;
    }

    public List<SalesRowDTO> byMovie(LocalDate from, LocalDate to, Integer centreId, int limit) {
        List<SalesRowDTO> rows = new ArrayList<>();
        String sql = "SELECT TOP (?) m.Title AS label, COUNT(*) AS bookings, SUM(tk.n) AS tickets, SUM(b.TotalAmount) AS revenue"
                + TICKET_FROM + ticketScope(centreId) + " GROUP BY m.Title ORDER BY SUM(b.TotalAmount) DESC";
        try (Connection connection = DBContext.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            bind(statement, 2, from, to, centreId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new SalesRowDTO(rs.getString("label"), rs.getInt("bookings"), rs.getInt("tickets"),
                            rs.getBigDecimal("revenue"), BigDecimal.ZERO));
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return rows;
    }

    public List<SalesRowDTO> byCentre(LocalDate from, LocalDate to, Integer centreId) {
        Map<String, int[]> counts = new LinkedHashMap<>();
        Map<String, BigDecimal[]> money = new LinkedHashMap<>();
        String ticketSql = "SELECT c.CentreName AS label, COUNT(*) AS bookings, SUM(tk.n) AS tickets, SUM(b.TotalAmount) AS revenue"
                + TICKET_FROM + ticketScope(centreId) + " GROUP BY c.CentreName";
        String fnbSql = "SELECT c.CentreName AS label, SUM(f.TotalAmount) AS revenue"
                + FNB_FROM + fnbScope(centreId) + " GROUP BY c.CentreName";
        String nameSql = "SELECT CentreName FROM Centres" + (centreId == null ? "" : " WHERE CentreID = ?") + " ORDER BY CentreName";
        try (Connection connection = DBContext.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(nameSql)) {
                if (centreId != null) {
                    statement.setInt(1, centreId);
                }
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        counts.put(rs.getString(1), new int[2]);
                        money.put(rs.getString(1), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(ticketSql)) {
                bind(statement, 1, from, to, centreId);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        String name = rs.getString("label");
                        counts.put(name, new int[]{rs.getInt("bookings"), rs.getInt("tickets")});
                        money.computeIfAbsent(name, key -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO})[0] = rs.getBigDecimal("revenue");
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(fnbSql)) {
                bind(statement, 1, from, to, centreId);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        String name = rs.getString("label");
                        counts.putIfAbsent(name, new int[2]);
                        money.computeIfAbsent(name, key -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO})[1] = rs.getBigDecimal("revenue");
                    }
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        List<SalesRowDTO> rows = new ArrayList<>();
        for (String name : counts.keySet()) {
            rows.add(new SalesRowDTO(name, counts.get(name)[0], counts.get(name)[1], money.get(name)[0], money.get(name)[1]));
        }
        rows.sort((a, b) -> b.getTotal().compareTo(a.getTotal()));
        return rows;
    }

    public List<FnbSaleDTO> topFnb(LocalDate from, LocalDate to, Integer centreId, int limit) {
        List<FnbSaleDTO> rows = new ArrayList<>();
        String sql = "SELECT TOP (?) COALESCE(i.ItemName, co.ComboName) AS name, SUM(oi.Quantity) AS qty, "
                + "SUM(oi.Quantity * oi.UnitPrice) AS revenue "
                + "FROM FnbOrderItems oi JOIN FnbOrders f ON f.FnbOrderID = oi.FnbOrderID "
                + "LEFT JOIN FnbItems i ON i.FnbItemID = oi.FnbItemID LEFT JOIN FnbCombos co ON co.ComboID = oi.ComboID "
                + "WHERE f.OrderStatus IN ('PREPARING','READY','DELIVERED') AND f.CreatedAt >= ? AND f.CreatedAt < ?"
                + fnbScope(centreId)
                + " GROUP BY COALESCE(i.ItemName, co.ComboName) ORDER BY SUM(oi.Quantity * oi.UnitPrice) DESC";
        try (Connection connection = DBContext.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            bind(statement, 2, from, to, centreId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new FnbSaleDTO(rs.getString("name"), rs.getInt("qty"), rs.getBigDecimal("revenue")));
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return rows;
    }
}
