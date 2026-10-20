package dao;
import java.sql.*;
import java.time.LocalDateTime;
import model.SeatAvailability;

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


    public ArrayList<Movie> getAllMovies() {

        ArrayList<Movie> list = new ArrayList<>();

        String sql = "SELECT * FROM Movies ORDER BY MovieID DESC";

        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                Movie movie = new Movie();

                movie.setMovieId(rs.getInt("MovieId"));
                movie.setTitle(rs.getString("Title"));
                movie.setGenre(rs.getString("Genre"));
                movie.setDuration(rs.getInt("Duration"));
                movie.setAgeRating(rs.getString("AgeRating"));
                movie.setReleaseDate(rs.getDate("ReleaseDate"));
                movie.setStatus(rs.getString("Status"));

                list.add(movie);
            }

            rs.close();
            ps.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public boolean insertMovie(Movie movie) {

        String sql = "INSERT INTO Movies "
                + "(Title, Genre, Duration, AgeRating, ReleaseDate, Status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, movie.getTitle());
            ps.setString(2, movie.getGenre());
            ps.setInt(3, movie.getDuration());
            ps.setString(4, movie.getAgeRating());
            ps.setDate(5, movie.getReleaseDate());
            ps.setString(6, movie.getStatus());

            int result = ps.executeUpdate();

            ps.close();

            return result > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
    
    public int getMovieDuration(int movieId) {

    String sql =
        "SELECT Duration "
        + "FROM Movies "
        + "WHERE MovieID = ?";

    try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

        ps.setInt(1, movieId);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getInt("Duration");
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }

    return -1;
}
public List<Movie> getAvailableMoviesByCentre(int centreId) {

    List<Movie> list = new ArrayList<>();

    String sql =
            "SELECT m.* "
            + "FROM Movies m "
            + "JOIN CentreMovies bm "
            + "ON bm.MovieID = m.MovieID "
            + "WHERE bm.CentreID = ? "
            + "AND bm.IsActive = 1 "
            + "AND m.Status = 'NOW_SHOWING' "
            + "ORDER BY m.Title";

    try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

        ps.setInt(1, centreId);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {

            Movie m = new Movie();

            m.setMovieId(
                    rs.getInt("MovieId")
            );

            m.setTitle(
                    rs.getString("Title")
            );

            m.setDuration(
                    rs.getInt("Duration")
            );

            list.add(m);
        }

    } catch (SQLException e) {
        e.printStackTrace();
    }

    return list;
}

}
