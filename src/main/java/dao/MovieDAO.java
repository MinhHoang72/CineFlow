package dao;

import model.Movie;
import utils.DBContext;
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
                   + "JOIN CentreMovies bm ON bm.MovieID = m.MovieID AND bm.IsActive = 1 "
                   + "WHERE bm.CentreID = ? AND m.Status = 'NOW_SHOWING' "
                   + "  AND EXISTS (SELECT 1 FROM Showtimes s JOIN Halls h ON h.HallID = s.HallID "
                   + "              WHERE s.MovieID = m.MovieID AND h.CentreID = bm.CentreID "
                   + "                AND h.Status = 'AVAILABLE' AND s.Status = 'SCHEDULED' "
                   + "                AND s.StartTime > SYSDATETIME()) "
                   + "ORDER BY m.Title";
        List<Movie> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Movie movie = new Movie();
                    movie.setMovieId(rs.getInt("MovieID"));
                    movie.setTitle(rs.getString("Title"));
                    movie.setGenre(rs.getString("Genre"));
                    movie.setDuration(rs.getInt("Duration"));
                    movie.setAgeRating(rs.getString("AgeRating"));
                    list.add(movie);
                }
            }
        }
        return list;
    }
}
