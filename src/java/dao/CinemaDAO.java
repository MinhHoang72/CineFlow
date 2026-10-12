package dao;

import model.Branch;
import model.Movie;
import model.ShowtimeView;
import utils.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CinemaDAO {

    public List<Movie> getNowShowingMovies() {
        return getMoviesByStatus("NOW_SHOWING");
    }

    public List<Movie> getComingSoonMovies() {
        return getMoviesByStatus("COMING_SOON");
    }

    private List<Movie> getMoviesByStatus(String status) {
        String sql = "SELECT MovieID, Title, Description, Duration, Genre, Director, Cast, Language, "
                + "ReleaseDate, EndDate, PosterURL, TrailerURL, AgeRating FROM Movies WHERE Status = ? "
                + "ORDER BY ReleaseDate DESC, MovieID DESC";
        List<Movie> movies = new ArrayList<>();

        try (Connection connection = DBContext.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Movie movie = new Movie();
                    movie.setMovieId(resultSet.getInt("MovieID"));
                    movie.setTitle(resultSet.getString("Title"));
                    movie.setDescription(resultSet.getString("Description"));
                    movie.setDuration(resultSet.getInt("Duration"));
                    movie.setGenre(resultSet.getString("Genre"));
                    movie.setDirector(resultSet.getString("Director"));
                    movie.setCast(resultSet.getString("Cast"));
                    movie.setLanguage(resultSet.getString("Language"));
                    movie.setReleaseDate(resultSet.getDate("ReleaseDate"));
                    movie.setEndDate(resultSet.getDate("EndDate"));
                    movie.setPosterUrl(resultSet.getString("PosterURL"));
                    movie.setTrailerUrl(resultSet.getString("TrailerURL"));
                    movie.setAgeRating(resultSet.getString("AgeRating"));
                    movies.add(movie);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return movies;
    }

    public Movie getMovieById(int movieId) {
        String sql = "SELECT MovieID, Title, Description, Duration, Genre, Director, Cast, Language, "
                + "ReleaseDate, EndDate, PosterURL, TrailerURL, AgeRating, Status "
                + "FROM Movies WHERE MovieID = ?";

        try (Connection connection = DBContext.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, movieId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Movie movie = new Movie();
                    movie.setMovieId(resultSet.getInt("MovieID"));
                    movie.setTitle(resultSet.getString("Title"));
                    movie.setDescription(resultSet.getString("Description"));
                    movie.setDuration(resultSet.getInt("Duration"));
                    movie.setGenre(resultSet.getString("Genre"));
                    movie.setDirector(resultSet.getString("Director"));
                    movie.setCast(resultSet.getString("Cast"));
                    movie.setLanguage(resultSet.getString("Language"));
                    movie.setReleaseDate(resultSet.getDate("ReleaseDate"));
                    movie.setEndDate(resultSet.getDate("EndDate"));
                    movie.setPosterUrl(resultSet.getString("PosterURL"));
                    movie.setTrailerUrl(resultSet.getString("TrailerURL"));
                    movie.setAgeRating(resultSet.getString("AgeRating"));
                    movie.setStatus(resultSet.getString("Status"));
                    return movie;
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return null;
    }

    public List<Branch> getActiveBranches() {
        String sql = "SELECT BranchID, BranchName, Address, City, Phone "
                + "FROM Branches WHERE Status = 'ACTIVE' ORDER BY BranchID";
        List<Branch> branches = new ArrayList<>();

        try (Connection connection = DBContext.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Branch branch = new Branch();
                branch.setBranchId(resultSet.getInt("BranchID"));
                branch.setBranchName(resultSet.getString("BranchName"));
                branch.setAddress(resultSet.getString("Address"));
                branch.setCity(resultSet.getString("City"));
                branch.setPhone(resultSet.getString("Phone"));
                branch.setStatus("ACTIVE");
                branches.add(branch);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return branches;
    }

    public List<Branch> getDistributionBranches(int movieId) {
        String sql = "SELECT b.BranchID, b.BranchName, b.Address, b.City, b.Phone, b.Status, "
                + "CAST(CASE WHEN UPPER(b.Status) = 'ACTIVE' AND bm.IsActive = 1 THEN 1 ELSE 0 END AS bit) AS [Distributed] "
                + "FROM Branches b LEFT JOIN BranchMovies bm ON bm.BranchID = b.BranchID AND bm.MovieID = ? "
                + "ORDER BY b.City, b.BranchName";
        List<Branch> branches = new ArrayList<>();
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Branch branch = new Branch();
                    branch.setBranchId(rs.getInt("BranchID"));
                    branch.setBranchName(rs.getString("BranchName"));
                    branch.setAddress(rs.getString("Address"));
                    branch.setCity(rs.getString("City"));
                    branch.setPhone(rs.getString("Phone"));
                    branch.setStatus(rs.getString("Status"));
                    branch.setDistributed(rs.getBoolean("Distributed"));
                    branches.add(branch);
                }
            }
        } catch (Exception exception) { exception.printStackTrace(); }
        return branches;
    }

    public String getMovieDistributionScope(int movieId) {
        String sql = "SELECT Scope FROM MovieDistributionSettings WHERE MovieID = ?";
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("Scope");
            }
        } catch (Exception exception) { exception.printStackTrace(); }
        return null;
    }

    public String getMovieDistributionCity(int movieId) {
        String sql = "SELECT City FROM MovieDistributionSettings WHERE MovieID = ?";
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("City");
            }
        } catch (Exception exception) { exception.printStackTrace(); }
        return null;
    }

    public Set<Integer> getMovieDistributionExceptions(int movieId) {
        String sql = "SELECT BranchID FROM MovieDistributionExceptions WHERE MovieID = ?";
        Set<Integer> branchIds = new HashSet<>();
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) branchIds.add(rs.getInt("BranchID"));
            }
        } catch (Exception exception) { exception.printStackTrace(); }
        return branchIds;
    }

    public List<Branch> getDistributionBranches(int movieId, int branchId) {
        List<Branch> branches = new ArrayList<>();
        String sql = "SELECT b.BranchID, b.BranchName, b.Address, b.City, b.Phone, b.Status, "
                + "CAST(CASE WHEN UPPER(b.Status) = 'ACTIVE' AND bm.IsActive = 1 THEN 1 ELSE 0 END AS bit) AS [Distributed] "
                + "FROM Branches b LEFT JOIN BranchMovies bm ON bm.BranchID = b.BranchID AND bm.MovieID = ? "
                + "WHERE b.BranchID = ?";
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            ps.setInt(2, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Branch branch = new Branch();
                    branch.setBranchId(rs.getInt("BranchID"));
                    branch.setBranchName(rs.getString("BranchName"));
                    branch.setAddress(rs.getString("Address"));
                    branch.setCity(rs.getString("City"));
                    branch.setPhone(rs.getString("Phone"));
                    branch.setStatus(rs.getString("Status"));
                    branch.setDistributed(rs.getBoolean("Distributed"));
                    branches.add(branch);
                }
            }
        } catch (Exception exception) { exception.printStackTrace(); }
        return branches;
    }

    public boolean saveMovieDistributionForBranch(int movieId, int branchId, boolean enabled) {
        String sql = "MERGE BranchMovies AS target USING (SELECT ? AS BranchID, ? AS MovieID) AS source "
                + "ON target.BranchID = source.BranchID AND target.MovieID = source.MovieID "
                + "WHEN MATCHED THEN UPDATE SET IsActive = ?, UpdatedAt = GETDATE() "
                + "WHEN NOT MATCHED THEN INSERT (BranchID, MovieID, IsActive) VALUES (source.BranchID, source.MovieID, ?);";
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setInt(2, movieId);
            ps.setBoolean(3, enabled);
            ps.setBoolean(4, enabled);
            ps.executeUpdate();
            return true;
        } catch (Exception exception) { exception.printStackTrace(); return false; }
    }

    public boolean saveMovieDistribution(int movieId, String scope, String city,
            Set<Integer> selectedBranchIds, Set<Integer> exceptionBranchIds) {
        if (!"system".equals(scope) && !"city".equals(scope)) return false;
        if ("city".equals(scope) && city == null) return false;
        String disableInactiveSql = "UPDATE bm SET bm.IsActive = 0, bm.UpdatedAt = GETDATE() "
                + "FROM BranchMovies bm JOIN Branches b ON b.BranchID = bm.BranchID "
                + "WHERE bm.MovieID = ? AND (b.Status IS NULL OR UPPER(b.Status) <> 'ACTIVE')";
        String disableSystemSql = "UPDATE BranchMovies SET IsActive = 0, UpdatedAt = GETDATE() WHERE MovieID = ?";
        String disableCitySql = "UPDATE bm SET bm.IsActive = 0, bm.UpdatedAt = GETDATE() "
                + "FROM BranchMovies bm JOIN Branches b ON b.BranchID = bm.BranchID "
                + "WHERE bm.MovieID = ? AND ((? = 1 AND (b.City IS NULL OR LTRIM(RTRIM(b.City)) = '')) "
                + "OR (? = 0 AND LTRIM(RTRIM(b.City)) = ?))";
        String mergeSql = "MERGE BranchMovies AS target "
                + "USING (SELECT ? AS BranchID, ? AS MovieID WHERE EXISTS ("
                + "SELECT 1 FROM Branches b WHERE b.BranchID = ? AND UPPER(b.Status) = 'ACTIVE')) AS source "
                + "ON target.BranchID = source.BranchID AND target.MovieID = source.MovieID "
                + "WHEN MATCHED THEN UPDATE SET IsActive = 1, UpdatedAt = GETDATE() "
                + "WHEN NOT MATCHED THEN INSERT (BranchID, MovieID, IsActive) VALUES (source.BranchID, source.MovieID, 1);";
        String saveSettingsSql = "MERGE MovieDistributionSettings AS target "
                + "USING (SELECT ? AS MovieID, ? AS Scope, ? AS City) AS source ON target.MovieID = source.MovieID "
                + "WHEN MATCHED THEN UPDATE SET Scope = source.Scope, City = source.City, UpdatedAt = GETDATE() "
                + "WHEN NOT MATCHED THEN INSERT (MovieID, Scope, City) VALUES (source.MovieID, source.Scope, source.City);";
        String deleteAllExceptionsSql = "DELETE FROM MovieDistributionExceptions WHERE MovieID = ?";
        String deleteCityExceptionsSql = "DELETE e FROM MovieDistributionExceptions e "
                + "JOIN Branches b ON b.BranchID = e.BranchID WHERE e.MovieID = ? "
                + "AND ((? = 1 AND (b.City IS NULL OR LTRIM(RTRIM(b.City)) = '')) OR (? = 0 AND LTRIM(RTRIM(b.City)) = ?))";
        String insertExceptionSql = "INSERT INTO MovieDistributionExceptions (MovieID, BranchID) VALUES (?, ?)";
        try (Connection connection = DBContext.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement inactive = connection.prepareStatement(disableInactiveSql)) {
                    inactive.setInt(1, movieId);
                    inactive.executeUpdate();
                }
                try (PreparedStatement disable = connection.prepareStatement(
                        "system".equals(scope) ? disableSystemSql : disableCitySql)) {
                    disable.setInt(1, movieId);
                    if ("city".equals(scope)) {
                        boolean uncategorized = city.isBlank();
                        disable.setBoolean(2, uncategorized);
                        disable.setBoolean(3, uncategorized);
                        disable.setString(4, city);
                    }
                    disable.executeUpdate();
                }
                try (PreparedStatement merge = connection.prepareStatement(mergeSql)) {
                    for (Integer branchId : selectedBranchIds) {
                        merge.setInt(1, branchId);
                        merge.setInt(2, movieId);
                        merge.setInt(3, branchId);
                        merge.addBatch();
                    }
                    merge.executeBatch();
                }
                try (PreparedStatement settings = connection.prepareStatement(saveSettingsSql)) {
                    settings.setInt(1, movieId);
                    settings.setString(2, scope);
                    if (city == null || city.isBlank()) settings.setNull(3, java.sql.Types.NVARCHAR);
                    else settings.setString(3, city);
                    settings.executeUpdate();
                }
                if ("system".equals(scope)) {
                    try (PreparedStatement delete = connection.prepareStatement(deleteAllExceptionsSql)) {
                        delete.setInt(1, movieId);
                        delete.executeUpdate();
                    }
                    if (exceptionBranchIds != null && !exceptionBranchIds.isEmpty()) {
                        try (PreparedStatement insert = connection.prepareStatement(insertExceptionSql)) {
                            for (Integer branchId : exceptionBranchIds) {
                                insert.setInt(1, movieId);
                                insert.setInt(2, branchId);
                                insert.addBatch();
                            }
                            insert.executeBatch();
                        }
                    }
                }
                connection.commit();
                return true;
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        } catch (Exception exception) { exception.printStackTrace(); return false; }
    }

    public List<ShowtimeView> getScheduledShowtimes() {
        String sql = "SELECT s.ShowtimeID, m.MovieID, b.BranchID, m.Title, "
            + "b.BranchName, b.City, h.HallName, "
                + "s.StartTime, s.BaseTicketPrice "
                + "FROM Showtimes s "
                + "JOIN Movies m ON m.MovieID = s.MovieID "
                + "JOIN Halls h ON h.HallID = s.HallID "
                + "JOIN Branches b ON b.BranchID = h.BranchID "
                + "WHERE s.Status = 'SCHEDULED' AND m.Status = 'NOW_SHOWING' "
                + "ORDER BY s.StartTime";
        List<ShowtimeView> showtimes = new ArrayList<>();

        try (Connection connection = DBContext.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                ShowtimeView showtime = new ShowtimeView();
                showtime.setShowtimeId(resultSet.getInt("ShowtimeID"));
                showtime.setMovieId(resultSet.getInt("MovieID"));
                showtime.setBranchId(resultSet.getInt("BranchID"));
                showtime.setMovieTitle(resultSet.getString("Title"));
                showtime.setBranchName(resultSet.getString("BranchName"));
                showtime.setCity(resultSet.getString("City"));
                showtime.setHallName(resultSet.getString("HallName"));
                showtime.setStartTime(resultSet.getTimestamp("StartTime"));
                showtime.setBaseTicketPrice(resultSet.getBigDecimal("BaseTicketPrice"));
                showtimes.add(showtime);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return showtimes;
    }

    public List<Movie> getAllMovies() {
        String sql = "SELECT MovieID, Title, Description, Duration, Genre, Director, Cast, Language, ReleaseDate, EndDate, PosterURL, TrailerURL, AgeRating, Status FROM Movies ORDER BY MovieID DESC";
        List<Movie> movies = new ArrayList<>();
        try (Connection connection = DBContext.getConnection(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet rs = statement.executeQuery()) {
            while (rs.next()) movies.add(mapMovie(rs));
        } catch (Exception exception) { exception.printStackTrace(); }
        return movies;
    }

    public int createMovie(Movie movie) {
        String sql = "INSERT INTO Movies (Title, Description, Duration, Genre, Director, Cast, Language, ReleaseDate, EndDate, PosterURL, TrailerURL, AgeRating, Status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            setMovieParams(ps, movie, false); ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) return keys.getInt(1); }
        } catch (Exception exception) { exception.printStackTrace(); }
        return 0;
    }

    public boolean updateMovie(Movie movie) {
        String sql = "UPDATE Movies SET Title=?, Description=?, Duration=?, Genre=?, Director=?, Cast=?, Language=?, ReleaseDate=?, EndDate=?, PosterURL=?, TrailerURL=?, AgeRating=?, Status=?, UpdatedAt=GETDATE() WHERE MovieID=?";
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            setMovieParams(ps, movie, true); return ps.executeUpdate() > 0;
        } catch (Exception exception) { exception.printStackTrace(); return false; }
    }

    public boolean archiveMovie(int movieId) {
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement("UPDATE Movies SET Status='ARCHIVED', UpdatedAt=GETDATE() WHERE MovieID=?")) {
            ps.setInt(1, movieId); return ps.executeUpdate() > 0;
        } catch (Exception exception) { exception.printStackTrace(); return false; }
    }

    public boolean deleteMovie(int movieId) {
        String sql = "DELETE FROM Movies WHERE MovieID = ?";
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            return ps.executeUpdate() > 0;
        } catch (Exception exception) { exception.printStackTrace(); return false; }
    }

    private Movie mapMovie(ResultSet rs) throws java.sql.SQLException {
        Movie movie = new Movie(); movie.setMovieId(rs.getInt("MovieID")); movie.setTitle(rs.getString("Title")); movie.setDescription(rs.getString("Description")); movie.setDuration(rs.getInt("Duration")); movie.setGenre(rs.getString("Genre")); movie.setDirector(rs.getString("Director")); movie.setCast(rs.getString("Cast")); movie.setLanguage(rs.getString("Language")); movie.setReleaseDate(rs.getDate("ReleaseDate")); movie.setEndDate(rs.getDate("EndDate")); movie.setPosterUrl(rs.getString("PosterURL")); movie.setTrailerUrl(rs.getString("TrailerURL")); movie.setAgeRating(rs.getString("AgeRating")); movie.setStatus(rs.getString("Status")); return movie;
    }

    private void setMovieParams(PreparedStatement ps, Movie movie, boolean update) throws java.sql.SQLException {
        ps.setString(1, movie.getTitle()); ps.setString(2, movie.getDescription()); ps.setInt(3, movie.getDuration()); ps.setString(4, movie.getGenre()); ps.setString(5, movie.getDirector()); ps.setString(6, movie.getCast()); ps.setString(7, movie.getLanguage()); ps.setDate(8, movie.getReleaseDate()); ps.setDate(9, movie.getEndDate()); ps.setString(10, movie.getPosterUrl()); ps.setString(11, movie.getTrailerUrl()); ps.setString(12, movie.getAgeRating()); ps.setString(13, movie.getStatus()); if (update) ps.setInt(14, movie.getMovieId());
    }
}
