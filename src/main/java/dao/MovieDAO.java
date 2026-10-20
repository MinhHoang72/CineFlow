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

    /** Buoc 3: phim con it nhat mot suat mo ban tai rap (cung dieu kien voi buoc chon thanh pho / rap). */
    public List<Movie> getMoviesShowingAt(int branchId) throws SQLException {
        String sql = "SELECT mv.MovieID, mv.Title, mv.Genre, mv.Duration, mv.AgeRating "
                   + "FROM Movies mv "
                   + "WHERE EXISTS (SELECT 1 " + ShowtimeDAO.BOOKABLE_FROM
                   + "              AND s.MovieID = mv.MovieID AND b.CentreID = ?) "
                   + "ORDER BY mv.Title";
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
