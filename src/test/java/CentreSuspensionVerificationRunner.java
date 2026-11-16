import dao.CentreDAO;
import utils.DBContext;
import java.lang.reflect.Method;
import java.sql.*;
import java.nio.file.Files;
import java.nio.file.Path;

/** Integration fixtures and all business-data changes are rolled back. */
public class CentreSuspensionVerificationRunner {
    public static void main(String[] args) throws Exception {
        try (Connection c = DBContext.getConnection()) {
            if (args.length > 0 && "--apply-patch".equals(args[0])) {
                String script = Files.readString(Path.of("database/centre_suspension_patch.sql"));
                for (String batch : script.split("(?im)^GO\\s*$")) {
                    if (!batch.isBlank()) try (Statement s = c.createStatement()) { s.execute(batch); }
                }
                System.out.println("PASS: additive schema patch applied");
            }
            c.setAutoCommit(false);
            try {
                int movie = value(c, "SELECT TOP 1 MovieID FROM Movies");
                int centre = insert(c, "INSERT Centres(CentreName,Address) VALUES(N'Suspension verification',N'Test')");
                int other = insert(c, "INSERT Centres(CentreName,Address) VALUES(N'Other verification',N'Test')");
                int hall = insert(c, "INSERT Halls(CentreID,HallName,Capacity) VALUES(" + centre + ",N'Test',10)");
                int otherHall = insert(c, "INSERT Halls(CentreID,HallName,Capacity) VALUES(" + other + ",N'Test',10)");
                int scheduled = show(c, movie, hall, 10, "SCHEDULED");
                int manual = show(c, movie, hall, 20, "CANCELLED");
                int elapsed = show(c, movie, hall, 30, "SCHEDULED");
                int ongoing = show(c, movie, hall, -1, "ONGOING");
                int unrelated = show(c, movie, otherHall, 10, "SCHEDULED");
                int seat = insert(c, "INSERT Seats(HallID,SeatRow,SeatNumber) VALUES(" + hall + ",'A',1)");
                String code = "SUSPTEST" + scheduled;
                int booking = insert(c, "INSERT Bookings(BookingCode,ShowtimeID,BookingStatus) VALUES('" + code + "'," + scheduled + ",'CONFIRMED')");
                int bookingSeat = insert(c, "INSERT BookingSeats(BookingID,SeatID,ShowtimeID,Price,SeatStatus) VALUES(" + booking + "," + seat + "," + scheduled + ",70000,'CONFIRMED')");
                int ticket = insert(c, "INSERT Tickets(BookingSeatID,TicketCode,QRCode) VALUES(" + bookingSeat + ",'" + code + "','" + code + "')");
                int paid = insert(c, "INSERT Payments(BookingID,TransactionCode,PaymentMethod,Amount,PaymentStatus) VALUES(" + booking + ",'" + code + "P','CASH',70000,'PAID')");
                int pending = insert(c, "INSERT Payments(BookingID,TransactionCode,PaymentMethod,Amount) VALUES(" + booking + ",'" + code + "W','QR',70000)");
                int fnb = insert(c, "INSERT FnbOrders(BookingID,CentreID,OrderCode) VALUES(" + booking + "," + centre + ",'" + code + "')");
                exec(c, "INSERT ShowtimeSeatPrices(ShowtimeID,SeatType,BasePrice,CentreAmount,HallAmount,MovieAmount,TimeAmount,SeatAmount) VALUES(" + scheduled + ",'STANDARD',70000,500,10000,2000,3000,0)");

                CentreDAO dao = new CentreDAO();
                Method cancel = CentreDAO.class.getDeclaredMethod("cancelUpcomingShowtimes", Connection.class, int.class, boolean.class);
                Method restore = CentreDAO.class.getDeclaredMethod("restoreSuspendedShowtimes", Connection.class, int.class);
                Method clear = CentreDAO.class.getDeclaredMethod("clearShowtimeSuspension", Connection.class, int.class);
                cancel.setAccessible(true); restore.setAccessible(true); clear.setAccessible(true);
                cancel.invoke(dao, c, centre, true);
                state(c, "Showtimes", "ShowtimeID", scheduled, "Status", "CANCELLED");
                check(value(c, "SELECT CancelledByCentreSuspension FROM Showtimes WHERE ShowtimeID=" + scheduled) == 1, "Suspension marker");
                check(value(c, "SELECT CancelledByCentreSuspension FROM Showtimes WHERE ShowtimeID=" + manual) == 0, "Manual cancellation not marked");
                state(c, "Bookings", "BookingID", booking, "BookingStatus", "CANCELLED");
                state(c, "BookingSeats", "BookingSeatID", bookingSeat, "SeatStatus", "CANCELLED");
                state(c, "Tickets", "TicketID", ticket, "TicketStatus", "CANCELLED");
                state(c, "Payments", "PaymentID", paid, "PaymentStatus", "PAID");
                state(c, "Payments", "PaymentID", pending, "PaymentStatus", "CANCELLED");
                state(c, "FnbOrders", "FnbOrderID", fnb, "OrderStatus", "CANCELLED");
                state(c, "Showtimes", "ShowtimeID", ongoing, "Status", "ONGOING");
                state(c, "Showtimes", "ShowtimeID", unrelated, "Status", "SCHEDULED");

                exec(c, "UPDATE Showtimes SET StartTime=DATEADD(HOUR,-3,SYSDATETIME()),EndTime=DATEADD(HOUR,-1,SYSDATETIME()) WHERE ShowtimeID=" + elapsed);
                restore.invoke(dao, c, centre);
                state(c, "Showtimes", "ShowtimeID", scheduled, "Status", "SCHEDULED");
                state(c, "Showtimes", "ShowtimeID", manual, "Status", "CANCELLED");
                state(c, "Showtimes", "ShowtimeID", elapsed, "Status", "CANCELLED");
                state(c, "Bookings", "BookingID", booking, "BookingStatus", "CANCELLED");
                state(c, "Tickets", "TicketID", ticket, "TicketStatus", "CANCELLED");
                check(value(c, "SELECT COUNT(*) FROM Showtimes s JOIN Halls h ON h.HallID=s.HallID WHERE h.CentreID=" + centre + " AND s.CancelledByCentreSuspension=1") == 0, "Markers cleared on reopen");
                check(value(c, "SELECT Total FROM ShowtimeSeatPrices WHERE ShowtimeID=" + scheduled) == 85500, "Snapshot unchanged");

                cancel.invoke(dao, c, centre, true);
                restore.invoke(dao, c, centre);
                state(c, "Showtimes", "ShowtimeID", scheduled, "Status", "SCHEDULED");
                // Permanent closure after suspension must never make these screenings restorable.
                cancel.invoke(dao, c, centre, true);
                cancel.invoke(dao, c, centre, false);
                clear.invoke(dao, c, centre);
                restore.invoke(dao, c, centre);
                state(c, "Showtimes", "ShowtimeID", scheduled, "Status", "CANCELLED");
                System.out.println("PASS: suspend/reopen, manual cancellation, elapsed/ongoing/other-centre screenings, booking/seat/ticket/F&B cancellation, payment preservation, frozen price, repeated cycle and permanent closure");
            } finally { c.rollback(); }
        }
    }

    private static int show(Connection c, int movie, int hall, int hours, String status) throws Exception {
        return insert(c, "INSERT Showtimes(MovieID,HallID,StartTime,EndTime,Status) VALUES(" + movie + "," + hall + ",DATEADD(HOUR," + hours + ",SYSDATETIME()),DATEADD(HOUR," + (hours + 2) + ",SYSDATETIME()),'" + status + "')");
    }
    private static int insert(Connection c, String sql) throws Exception {
        try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.executeUpdate(); try (ResultSet rs = s.getGeneratedKeys()) { rs.next(); return rs.getInt(1); }
        }
    }
    private static int value(Connection c, String sql) throws Exception {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) { rs.next(); return rs.getInt(1); }
    }
    private static void exec(Connection c, String sql) throws Exception {
        try (Statement s = c.createStatement()) { s.executeUpdate(sql); }
    }
    private static void state(Connection c, String table, String key, int id, String column, String expected) throws Exception {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT " + column + " FROM " + table + " WHERE " + key + "=" + id)) {
            check(rs.next() && expected.equals(rs.getString(1)), table + " " + id + " expected " + expected);
        }
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
