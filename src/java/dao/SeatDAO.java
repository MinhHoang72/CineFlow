package dao;
import utils.DBContext;


import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.SeatAvailability;

public class SeatDAO extends DBContext {

    public List<SeatAvailability>
            getSeatAvailability(
                    int showtimeId) {

        List<SeatAvailability> list =
                new ArrayList<>();

        String sql =
                "SELECT "
                + "s.SeatID, "
                + "s.SeatRow, "
                + "s.SeatNumber, "
                + "s.SeatType, "

                + "CASE "

                + "WHEN bs.SeatStatus = 'CONFIRMED' "
                + "THEN 'BOOKED' "

                + "WHEN bs.SeatStatus = 'HELD' "
                + "THEN 'HELD' "

                + "ELSE 'AVAILABLE' "

                + "END AS AvailabilityStatus "

                + "FROM Showtimes sh "

                + "JOIN Seats s "
                + "ON s.HallID = sh.HallID "

                + "LEFT JOIN ( "

                + "SELECT bs2.SeatID, "
                + "bs2.SeatStatus "

                + "FROM BookingSeats bs2 "

                + "JOIN Bookings b "
                + "ON b.BookingID = bs2.BookingID "

                + "WHERE b.ShowtimeID = ? "

                + "AND b.BookingStatus "
                + "IN ('PENDING','CONFIRMED') "

                + "AND bs2.SeatStatus "
                + "IN ('HELD','CONFIRMED') "

                + ") bs "

                + "ON bs.SeatID = s.SeatID "

                + "WHERE sh.ShowtimeID = ? "

                + "AND s.Status = 'AVAILABLE' "

                + "ORDER BY "
                + "s.SeatRow, "
                + "s.SeatNumber";

        try (PreparedStatement ps =
                     connection.prepareStatement(sql)) {

            ps.setInt(
                    1,
                    showtimeId
            );

            ps.setInt(
                    2,
                    showtimeId
            );

            ResultSet rs =
                    ps.executeQuery();

            while (rs.next()) {

                SeatAvailability seat =
                        new SeatAvailability();

                seat.setSeatId(
                        rs.getInt("SeatID")
                );

                seat.setSeatRow(
                        rs.getString("SeatRow")
                );

                seat.setSeatNumber(
                        rs.getInt("SeatNumber")
                );

                seat.setSeatType(
                        rs.getString("SeatType")
                );

                seat.setAvailabilityStatus(
                        rs.getString(
                                "AvailabilityStatus"
                        )
                );

                list.add(seat);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }
}