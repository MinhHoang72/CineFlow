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
        Map<String, BigDecimal[]> surcharges = loadSurcharges(conn);
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

    /** SeatType -> {tong phu thu co dinh, tong phan tram phu thu}. */
    private static Map<String, BigDecimal[]> loadSurcharges(Connection conn) throws SQLException {
        Map<String, BigDecimal[]> map = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT SeatType, Amount, Percentage FROM Surcharges WHERE IsActive = 1");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                BigDecimal[] v = map.computeIfAbsent(rs.getString("SeatType"),
                        k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                v[0] = v[0].add(rs.getBigDecimal("Amount"));
                BigDecimal pct = rs.getBigDecimal("Percentage");
                if (pct != null) v[1] = v[1].add(pct);
            }
        }
        return map;
    }

    private static BigDecimal priceFor(BigDecimal base, String seatType, Map<String, BigDecimal[]> surcharges) {
        BigDecimal[] s = surcharges.get(seatType);
        if (s == null) return base;
        BigDecimal percentPart = base.multiply(s[1]).divide(BigDecimal.valueOf(100));
        return base.add(s[0]).add(percentPart);
    }
}
