package com.mycompany.cinechain.dao;

import com.mycompany.cinechain.model.Movie;
import com.mycompany.cinechain.utils.DBContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MovieDAO {

    /** Phim dang chieu tai chi nhanh va con it nhat mot suat chua chieu. */
    public List<Movie> getMoviesShowingAt(int branchId) throws SQLException {
        String sql = "SELECT m.MovieID, m.Title, m.Genre, m.Duration, m.AgeRating "
                   + "FROM Movies m "
                   + "JOIN BranchMovies bm ON bm.MovieID = m.MovieID AND bm.IsActive = 1 "
                   + "WHERE bm.BranchID = ? AND m.Status = 'NOW_SHOWING' "
                   + "  AND EXISTS (SELECT 1 FROM Showtimes s JOIN Halls h ON h.HallID = s.HallID "
                   + "              WHERE s.MovieID = m.MovieID AND h.BranchID = bm.BranchID "
                   + "                AND h.Status = 'AVAILABLE' AND s.Status = 'SCHEDULED' "
                   + "                AND s.StartTime > SYSDATETIME()) "
                   + "ORDER BY m.Title";
        List<Movie> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Movie(rs.getInt("MovieID"), rs.getString("Title"), rs.getString("Genre"),
                            rs.getInt("Duration"), rs.getString("AgeRating")));
                }
            }
        }
        return list;
    }
}
