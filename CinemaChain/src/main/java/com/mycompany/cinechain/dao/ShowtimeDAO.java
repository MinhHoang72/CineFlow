package com.mycompany.cinechain.dao;

import com.mycompany.cinechain.model.Showtime;
import com.mycompany.cinechain.utils.DBContext;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ShowtimeDAO {

    static final String SELECT =
            "SELECT s.ShowtimeID, s.MovieID, m.Title, b.BranchID, b.BranchName, h.HallID, h.HallName, "
          + "       h.HallType, s.StartTime, s.EndTime, s.BaseTicketPrice "
          + "FROM Showtimes s "
          + "JOIN Movies m ON m.MovieID = s.MovieID "
          + "JOIN Halls h ON h.HallID = s.HallID "
          + "JOIN Branches b ON b.BranchID = h.BranchID ";

    private static final String BOOKABLE =
            "s.Status = 'SCHEDULED' AND s.StartTime > SYSDATETIME() AND m.Status = 'NOW_SHOWING' "
          + "AND h.Status = 'AVAILABLE' AND b.Status = 'ACTIVE' ";

    static Showtime map(ResultSet rs) throws SQLException {
        return new Showtime(rs.getInt("ShowtimeID"), rs.getInt("MovieID"), rs.getString("Title"),
                rs.getInt("BranchID"), rs.getString("BranchName"), rs.getInt("HallID"),
                rs.getString("HallName"), rs.getString("HallType"), rs.getTimestamp("StartTime"),
                rs.getTimestamp("EndTime"), rs.getBigDecimal("BaseTicketPrice"));
    }

    /** Cac ngay con suat chieu cua phim tai chi nhanh. */
    public List<LocalDate> getShowDates(int branchId, int movieId) throws SQLException {
        String sql = "SELECT DISTINCT CAST(s.StartTime AS DATE) AS ShowDate "
                   + "FROM Showtimes s JOIN Movies m ON m.MovieID = s.MovieID "
                   + "JOIN Halls h ON h.HallID = s.HallID JOIN Branches b ON b.BranchID = h.BranchID "
                   + "WHERE " + BOOKABLE + "AND b.BranchID = ? AND s.MovieID = ? ORDER BY ShowDate";
        List<LocalDate> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setInt(2, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getDate("ShowDate").toLocalDate());
            }
        }
        return list;
    }

    public List<Showtime> getShowtimes(int branchId, int movieId, LocalDate date) throws SQLException {
        String sql = SELECT + "WHERE " + BOOKABLE
                   + "AND b.BranchID = ? AND s.MovieID = ? "
                   + "AND s.StartTime >= ? AND s.StartTime < ? ORDER BY s.StartTime";
        List<Showtime> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setInt(2, movieId);
            ps.setDate(3, Date.valueOf(date));
            ps.setDate(4, Date.valueOf(date.plusDays(1)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /** Cac ngay con suat chieu tai chi nhanh (ban tai quay). */
    public List<LocalDate> getShowDatesAtBranch(int branchId) throws SQLException {
        String sql = "SELECT DISTINCT CAST(s.StartTime AS DATE) AS ShowDate "
                   + "FROM Showtimes s JOIN Movies m ON m.MovieID = s.MovieID "
                   + "JOIN Halls h ON h.HallID = s.HallID JOIN Branches b ON b.BranchID = h.BranchID "
                   + "WHERE " + BOOKABLE + "AND b.BranchID = ? ORDER BY ShowDate";
        List<LocalDate> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getDate("ShowDate").toLocalDate());
            }
        }
        return list;
    }

    /** Tat ca suat con mo ban cua chi nhanh trong ngay, sap theo phim roi gio chieu. */
    public List<Showtime> getShowtimesAtBranch(int branchId, LocalDate date) throws SQLException {
        String sql = SELECT + "WHERE " + BOOKABLE
                   + "AND b.BranchID = ? AND s.StartTime >= ? AND s.StartTime < ? ORDER BY m.Title, s.StartTime";
        List<Showtime> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setDate(2, Date.valueOf(date));
            ps.setDate(3, Date.valueOf(date.plusDays(1)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /** Suat chieu con mo ban, null neu khong ton tai hoac da qua gio. */
    public Showtime getBookableById(int showtimeId) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            return getBookableById(conn, showtimeId);
        }
    }

    static Showtime getBookableById(Connection conn, int showtimeId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + "WHERE " + BOOKABLE + "AND s.ShowtimeID = ?")) {
            ps.setInt(1, showtimeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }
}
