package com.mycompany.cinechain.dao;

import com.mycompany.cinechain.model.Seat;
import com.mycompany.cinechain.model.Showtime;
import com.mycompany.cinechain.utils.DBContext;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SeatDAO {

    /** So do ghe cua phong chieu, kem trang thai da giu/da ban va gia ve cho suat chieu. */
    public List<Seat> getSeatMap(Showtime showtime) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            return getSeatMap(conn, showtime);
        }
    }

    static List<Seat> getSeatMap(Connection conn, Showtime showtime) throws SQLException {
        String sql = "SELECT se.SeatID, se.SeatRow, se.SeatNumber, se.SeatType, se.Status, "
                   + "  CASE WHEN EXISTS (SELECT 1 FROM BookingSeats bs "
                   + "       WHERE bs.ShowtimeID = ? AND bs.SeatID = se.SeatID "
                   + "         AND (bs.SeatStatus = 'CONFIRMED' "
                   + "              OR (bs.SeatStatus = 'HELD' AND bs.HoldUntil > SYSDATETIME()))) "
                   + "  THEN 1 ELSE 0 END AS Taken "
                   + "FROM Seats se WHERE se.HallID = ? ORDER BY se.SeatRow, se.SeatNumber";
        Map<String, BigDecimal> surcharges = loadSeatSurcharges(conn, showtime);
        List<Seat> seats = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, showtime.getId());
            ps.setInt(2, showtime.getHallId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String type = rs.getString("SeatType");
                    seats.add(new Seat(rs.getInt("SeatID"), rs.getString("SeatRow"), rs.getInt("SeatNumber"),
                            type, "AVAILABLE".equals(rs.getString("Status")), rs.getInt("Taken") == 1,
                            priceFor(showtime.getBasePrice(), type, surcharges)));
                }
            }
        }
        return seats;
    }

    /** Loai ghe (TargetValue) -> tong phu thu ghe (Surcharges SEAT) dang hieu luc tai gio chieu. */
    private static Map<String, BigDecimal> loadSeatSurcharges(Connection conn, Showtime showtime) throws SQLException {
        Map<String, BigDecimal> map = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT TargetValue, SUM(Amount) AS Amount FROM Surcharges "
              + "WHERE SurchargeType = 'SEAT' AND EffectiveFrom <= ? AND (EffectiveTo IS NULL OR EffectiveTo > ?) "
              + "GROUP BY TargetValue")) {
            ps.setTimestamp(1, showtime.getStartTime());
            ps.setTimestamp(2, showtime.getStartTime());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) map.put(rs.getString("TargetValue"), rs.getBigDecimal("Amount"));
            }
        }
        return map;
    }

    /** Gia ghe = gia ve ghe thuong cua suat + phu thu theo loai ghe. */
    private static BigDecimal priceFor(BigDecimal base, String seatType, Map<String, BigDecimal> surcharges) {
        return base.add(surcharges.getOrDefault(seatType, BigDecimal.ZERO));
    }
}
