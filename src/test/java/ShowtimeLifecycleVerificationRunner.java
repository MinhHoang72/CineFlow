import dao.CentrePricingDAO;
import dao.ShowtimeDAO;
import model.entity.CentrePricing;
import utils.DBContext;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Integration checks against the configured database. All showtime changes are rolled back. */
public class ShowtimeLifecycleVerificationRunner {
    public static void main(String[] args) throws Exception {
        DriverManager.setLoginTimeout(5);
        try (Connection connection = DBContext.getConnection()) {
            connection.setAutoCommit(false);
            try {
                LocalDateTime now;
                int movieId;
                int hallId;
                try (Statement ps = connection.createStatement();
                     ResultSet rs = ps.executeQuery("SELECT TOP 1 MovieID, HallID, SYSDATETIME() AS Now FROM Showtimes")) {
                    if (!rs.next()) throw new AssertionError("Seed a movie, hall and showtime before running verification");
                    movieId = rs.getInt("MovieID");
                    hallId = rs.getInt("HallID");
                    now = rs.getTimestamp("Now").toLocalDateTime();
                }
                Map<Integer, String> expected = new LinkedHashMap<>();
                expected.put(insert(connection, movieId, hallId, now.plusHours(1), now.plusHours(3), "SCHEDULED"), "SCHEDULED");
                expected.put(insert(connection, movieId, hallId, now.minusMinutes(10), now.plusHours(1), "SCHEDULED"), "ONGOING");
                expected.put(insert(connection, movieId, hallId, now.minusHours(3), now.minusHours(1), "SCHEDULED"), "COMPLETED");
                expected.put(insert(connection, movieId, hallId, now.minusHours(3), now.minusHours(1), "ONGOING"), "COMPLETED");
                expected.put(insert(connection, movieId, hallId, now.minusMinutes(10), now.plusHours(1), "ONGOING"), "ONGOING");
                expected.put(insert(connection, movieId, hallId, now.minusHours(3), now.minusHours(1), "CANCELLED"), "CANCELLED");
                expected.put(insert(connection, movieId, hallId, now.minusHours(3), now.minusHours(1), "COMPLETED"), "COMPLETED");
                // Film ended two minutes ago: the 15-minute cleaning buffer must not delay COMPLETED.
                expected.put(insert(connection, movieId, hallId, now.minusHours(2), now.minusMinutes(2), "SCHEDULED"), "COMPLETED");
                ShowtimeDAO dao = new ShowtimeDAO();
                dao.synchronizeStatuses(connection);
                for (Map.Entry<Integer, String> entry : expected.entrySet()) {
                    try (PreparedStatement ps = connection.prepareStatement("SELECT Status FROM Showtimes WHERE ShowtimeID = ?")) {
                        ps.setInt(1, entry.getKey());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next() || !entry.getValue().equals(rs.getString(1))) {
                                throw new AssertionError("Incorrect lifecycle state for fixture " + entry.getKey());
                            }
                        }
                    }
                }
                if (dao.synchronizeStatuses(connection) != 0) throw new AssertionError("Repeated synchronization should not rewrite unchanged states");
                System.out.println("PASS: 8 lifecycle cases, cleaning buffer, cancelled states and idempotence");
            } finally {
                connection.rollback();
            }

            Integer centreId = null;
            try (Statement ps = connection.createStatement(); ResultSet rs = ps.executeQuery("SELECT TOP 1 CentreID FROM CentrePricing")) {
                if (rs.next()) centreId = rs.getInt(1);
            }
            if (centreId == null) throw new AssertionError("Seed base pricing before running verification");
            for (CentrePricing price : new CentrePricingDAO().getPriceHistory(centreId)) {
                try (PreparedStatement ps = connection.prepareStatement("SELECT FullName FROM Users WHERE UserID = ?")) {
                    if (price.getUpdatedBy() == null) ps.setNull(1, Types.INTEGER);
                    else ps.setInt(1, price.getUpdatedBy());
                    try (ResultSet rs = ps.executeQuery()) {
                        String expectedName = rs.next() ? rs.getString(1) : null;
                        if (!java.util.Objects.equals(expectedName, price.getUpdatedByName())) throw new AssertionError("Incorrect updater full name");
                    }
                }
            }
            System.out.println("PASS: base pricing history maps updater names");
        } finally {
            DBContext.closeRequestConnections();
        }
    }

    private static int insert(Connection connection, int movieId, int hallId,
            LocalDateTime start, LocalDateTime end, String status) throws SQLException {
        String sql = "INSERT INTO Showtimes (MovieID, HallID, StartTime, EndTime, CleaningBufferMinutes, Status) "
                + "OUTPUT INSERTED.ShowtimeID VALUES (?, ?, ?, ?, 15, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            ps.setInt(2, hallId);
            ps.setTimestamp(3, Timestamp.valueOf(start));
            ps.setTimestamp(4, Timestamp.valueOf(end));
            ps.setString(5, status);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
