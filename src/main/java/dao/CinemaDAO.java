package dao;

import model.Centre;
import model.Movie;
import model.ShowtimeView;
import utils.DBContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
                + "ReleaseDate, EndDate, PosterURL, TrailerURL, AgeRating, Status FROM Movies WHERE Status = ? "
                + "ORDER BY ReleaseDate DESC, MovieID DESC";
        List<Movie> movies = new ArrayList<>();

        try (Connection connection = DBContext.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    movies.add(mapMovie(resultSet));
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

    public List<Centre> getActiveCentres() {
        String sql = "SELECT b.CentreID, b.CentreName, b.Address, b.City, b.Phone, "
                + "COUNT(DISTINCT h.HallID) AS HallCount, "
                + "COALESCE(SUM(h.Capacity), 0) AS SeatCount "
                + "FROM Centres b "
                + "LEFT JOIN Halls h ON h.CentreID = b.CentreID "
                + "WHERE b.Status = 'ACTIVE' "
                + "GROUP BY b.CentreID, b.CentreName, b.Address, b.City, b.Phone "
                + "ORDER BY b.City, b.CentreName";
        List<Centre> centres = new ArrayList<>();

        try (Connection connection = DBContext.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Centre centre = new Centre();
                centre.setCentreId(resultSet.getInt("CentreID"));
                centre.setCentreName(resultSet.getString("CentreName"));
                centre.setAddress(resultSet.getString("Address"));
                centre.setCity(resultSet.getString("City"));
                centre.setPhone(resultSet.getString("Phone"));
                centre.setStatus("ACTIVE");
                centre.setHallCount(resultSet.getInt("HallCount"));
                centre.setSeatCount(resultSet.getInt("SeatCount"));
                centres.add(centre);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return centres;
    }

    public Map<String, Integer> getActiveCitiesWithCount() {
        String sql = "SELECT City, COUNT(CentreID) AS CinemaCount "
                + "FROM Centres WHERE Status = 'ACTIVE' AND City IS NOT NULL AND LTRIM(RTRIM(City)) <> '' "
                + "GROUP BY City ORDER BY City";
        Map<String, Integer> cities = new LinkedHashMap<>();
        try (Connection connection = DBContext.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String city = resultSet.getString("City");
                if (city != null && !city.trim().isEmpty()) {
                    cities.put(city.trim(), resultSet.getInt("CinemaCount"));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return cities;
    }

    public List<Centre> getActiveCentresByCity(String city) {
        String sql = "SELECT b.CentreID, b.CentreName, b.Address, b.City, b.Phone, "
                + "COUNT(DISTINCT h.HallID) AS HallCount, "
                + "COALESCE(SUM(h.Capacity), 0) AS SeatCount "
                + "FROM Centres b "
                + "LEFT JOIN Halls h ON h.CentreID = b.CentreID "
                + "WHERE b.Status = 'ACTIVE' AND LTRIM(RTRIM(b.City)) = LTRIM(RTRIM(?)) "
                + "GROUP BY b.CentreID, b.CentreName, b.Address, b.City, b.Phone "
                + "ORDER BY b.CentreName";
        List<Centre> centres = new ArrayList<>();
        try (Connection connection = DBContext.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, city != null ? city.trim() : "");
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Centre centre = new Centre();
                    centre.setCentreId(resultSet.getInt("CentreID"));
                    centre.setCentreName(resultSet.getString("CentreName"));
                    centre.setAddress(resultSet.getString("Address"));
                    centre.setCity(resultSet.getString("City"));
                    centre.setPhone(resultSet.getString("Phone"));
                    centre.setStatus("ACTIVE");
                    centre.setHallCount(resultSet.getInt("HallCount"));
                    centre.setSeatCount(resultSet.getInt("SeatCount"));
                    centres.add(centre);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return centres;
    }

    public Centre getCentreById(int centreId) {
        String sql = "SELECT b.CentreID, b.CentreName, b.Address, b.City, b.Phone, b.Status, "
                + "COUNT(DISTINCT h.HallID) AS HallCount, "
                + "COALESCE(SUM(h.Capacity), 0) AS SeatCount, "
                + "u.UserID AS ManagerID, u.FullName AS ManagerName, u.Email AS ManagerEmail "
                + "FROM Centres b "
                + "LEFT JOIN Halls h ON h.CentreID = b.CentreID "
                + "LEFT JOIN Users u ON u.CentreID = b.CentreID AND u.Role = 'MANAGER' AND u.Status = 'ACTIVE' "
                + "WHERE b.CentreID = ? "
                + "GROUP BY b.CentreID, b.CentreName, b.Address, b.City, b.Phone, b.Status, u.UserID, u.FullName, u.Email";

        try (Connection connection = DBContext.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, centreId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Centre centre = new Centre();
                    centre.setCentreId(resultSet.getInt("CentreID"));
                    centre.setCentreName(resultSet.getString("CentreName"));
                    centre.setAddress(resultSet.getString("Address"));
                    centre.setCity(resultSet.getString("City"));
                    centre.setPhone(resultSet.getString("Phone"));
                    centre.setStatus(resultSet.getString("Status"));
                    centre.setHallCount(resultSet.getInt("HallCount"));
                    centre.setSeatCount(resultSet.getInt("SeatCount"));
                    int mgrId = resultSet.getInt("ManagerID");
                    if (!resultSet.wasNull()) {
                        centre.setManagerId(mgrId);
                        centre.setManagerName(resultSet.getString("ManagerName"));
                        centre.setManagerEmail(resultSet.getString("ManagerEmail"));
                    }
                    return centre;
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return null;
    }

    public List<Centre> getDistributionCentres(int movieId) {
        String sql = "SELECT b.CentreID, b.CentreName, b.Address, b.City, b.Phone, b.Status, "
                + "CAST(CASE WHEN UPPER(b.Status) = 'ACTIVE' AND bm.IsActive = 1 THEN 1 ELSE 0 END AS bit) AS [Distributed] "
                + "FROM Centres b LEFT JOIN CentreMovies bm ON bm.CentreID = b.CentreID AND bm.MovieID = ? "
                + "ORDER BY b.City, b.CentreName";
        List<Centre> centres = new ArrayList<>();
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Centre centre = new Centre();
                    centre.setCentreId(rs.getInt("CentreID"));
                    centre.setCentreName(rs.getString("CentreName"));
                    centre.setAddress(rs.getString("Address"));
                    centre.setCity(rs.getString("City"));
                    centre.setPhone(rs.getString("Phone"));
                    centre.setStatus(rs.getString("Status"));
                    centre.setDistributed(rs.getBoolean("Distributed"));
                    centres.add(centre);
                }
            }
        } catch (Exception exception) { exception.printStackTrace(); }
        return centres;
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
        String sql = "SELECT CentreID FROM MovieDistributionExceptions WHERE MovieID = ?";
        Set<Integer> CentreIDs = new HashSet<>();
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) CentreIDs.add(rs.getInt("CentreID"));
            }
        } catch (Exception exception) { exception.printStackTrace(); }
        return CentreIDs;
    }

    public List<Centre> getDistributionCentres(int movieId, int centreId) {
        List<Centre> centres = new ArrayList<>();
        String sql = "SELECT b.CentreID, b.CentreName, b.Address, b.City, b.Phone, b.Status, "
                + "CAST(CASE WHEN UPPER(b.Status) = 'ACTIVE' AND bm.IsActive = 1 THEN 1 ELSE 0 END AS bit) AS [Distributed] "
                + "FROM Centres b LEFT JOIN CentreMovies bm ON bm.CentreID = b.CentreID AND bm.MovieID = ? "
                + "WHERE b.CentreID = ?";
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, movieId);
            ps.setInt(2, centreId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Centre centre = new Centre();
                    centre.setCentreId(rs.getInt("CentreID"));
                    centre.setCentreName(rs.getString("CentreName"));
                    centre.setAddress(rs.getString("Address"));
                    centre.setCity(rs.getString("City"));
                    centre.setPhone(rs.getString("Phone"));
                    centre.setStatus(rs.getString("Status"));
                    centre.setDistributed(rs.getBoolean("Distributed"));
                    centres.add(centre);
                }
            }
        } catch (Exception exception) { exception.printStackTrace(); }
        return centres;
    }

    public boolean saveMovieDistributionForCentre(int movieId, int centreId, boolean enabled) {
        String sql = "MERGE CentreMovies AS target USING (SELECT ? AS CentreID, ? AS MovieID) AS source "
                + "ON target.CentreID = source.CentreID AND target.MovieID = source.MovieID "
                + "WHEN MATCHED THEN UPDATE SET IsActive = ?, UpdatedAt = GETDATE() "
                + "WHEN NOT MATCHED THEN INSERT (CentreID, MovieID, IsActive) VALUES (source.CentreID, source.MovieID, ?);";
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, centreId);
            ps.setInt(2, movieId);
            ps.setBoolean(3, enabled);
            ps.setBoolean(4, enabled);
            ps.executeUpdate();
            return true;
        } catch (Exception exception) { exception.printStackTrace(); return false; }
    }

    public boolean saveMovieDistribution(int movieId, Set<Integer> selectedCentreIDs) {
        String disableInactiveSql = "UPDATE bm SET bm.IsActive = 0, bm.UpdatedAt = GETDATE() "
                + "FROM CentreMovies bm JOIN Centres b ON b.CentreID = bm.CentreID "
                + "WHERE bm.MovieID = ? AND (b.Status IS NULL OR UPPER(b.Status) <> 'ACTIVE')";
        String disableAllSql = "UPDATE CentreMovies SET IsActive = 0, UpdatedAt = GETDATE() WHERE MovieID = ?";
        String mergeSql = "MERGE CentreMovies AS target "
                + "USING (SELECT ? AS CentreID, ? AS MovieID WHERE EXISTS ("
                + "SELECT 1 FROM Centres b WHERE b.CentreID = ? AND UPPER(b.Status) = 'ACTIVE')) AS source "
                + "ON target.CentreID = source.CentreID AND target.MovieID = source.MovieID "
                + "WHEN MATCHED THEN UPDATE SET IsActive = 1, UpdatedAt = GETDATE() "
                + "WHEN NOT MATCHED THEN INSERT (CentreID, MovieID, IsActive) VALUES (source.CentreID, source.MovieID, 1);";

        try (Connection connection = DBContext.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement inactive = connection.prepareStatement(disableInactiveSql)) {
                    inactive.setInt(1, movieId);
                    inactive.executeUpdate();
                }
                try (PreparedStatement disable = connection.prepareStatement(disableAllSql)) {
                    disable.setInt(1, movieId);
                    disable.executeUpdate();
                }
                if (selectedCentreIDs != null && !selectedCentreIDs.isEmpty()) {
                    try (PreparedStatement merge = connection.prepareStatement(mergeSql)) {
                        for (Integer CentreID : selectedCentreIDs) {
                            merge.setInt(1, CentreID);
                            merge.setInt(2, movieId);
                            merge.setInt(3, CentreID);
                            merge.addBatch();
                        }
                        merge.executeBatch();
                    }
                }
                connection.commit();
                return true;
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }

    public boolean saveMovieDistribution(int movieId, String scope, String city,
            Set<Integer> selectedCentreIDs, Set<Integer> exceptionCentreIDs) {
        return saveMovieDistribution(movieId, selectedCentreIDs);
    }

    public List<ShowtimeView> getScheduledShowtimes() {
        String sql = "SELECT s.ShowtimeID, m.MovieID, b.CentreID, m.Title, "
            + "b.CentreName, b.City, h.HallName, "
                + "s.StartTime, s.BaseTicketPrice "
                + "FROM Showtimes s "
                + "JOIN Movies m ON m.MovieID = s.MovieID "
                + "JOIN Halls h ON h.HallID = s.HallID "
                + "JOIN Centres b ON b.CentreID = h.CentreID "
                + "WHERE s.Status = 'SCHEDULED' AND m.Status IN ('NOW_SHOWING', 'COMING_SOON') "
                + "ORDER BY s.StartTime";
        List<ShowtimeView> showtimes = new ArrayList<>();

        try (Connection connection = DBContext.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                ShowtimeView showtime = new ShowtimeView();
                showtime.setShowtimeId(resultSet.getInt("ShowtimeID"));
                showtime.setMovieId(resultSet.getInt("MovieID"));
                showtime.setCentreID(resultSet.getInt("CentreID"));
                showtime.setMovieTitle(resultSet.getString("Title"));
                showtime.setCentreName(resultSet.getString("CentreName"));
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

    public List<ShowtimeView> getScheduledShowtimesByMovieId(int movieId) {
        String sql = "SELECT s.ShowtimeID, m.MovieID, b.CentreID, m.Title, "
                + "b.CentreName, b.City, h.HallName, "
                + "s.StartTime, s.BaseTicketPrice "
                + "FROM Showtimes s "
                + "JOIN Movies m ON m.MovieID = s.MovieID "
                + "JOIN Halls h ON h.HallID = s.HallID "
                + "JOIN Centres b ON b.CentreID = h.CentreID "
                + "WHERE s.Status = 'SCHEDULED' AND s.MovieID = ? "
                + "ORDER BY s.StartTime";
        List<ShowtimeView> showtimes = new ArrayList<>();

        try (Connection connection = DBContext.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, movieId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ShowtimeView showtime = new ShowtimeView();
                    showtime.setShowtimeId(resultSet.getInt("ShowtimeID"));
                    showtime.setMovieId(resultSet.getInt("MovieID"));
                    showtime.setCentreID(resultSet.getInt("CentreID"));
                    showtime.setMovieTitle(resultSet.getString("Title"));
                    showtime.setCentreName(resultSet.getString("CentreName"));
                    showtime.setCity(resultSet.getString("City"));
                    showtime.setHallName(resultSet.getString("HallName"));
                    showtime.setStartTime(resultSet.getTimestamp("StartTime"));
                    showtime.setBaseTicketPrice(resultSet.getBigDecimal("BaseTicketPrice"));
                    showtimes.add(showtime);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return showtimes;
    }

    public List<ShowtimeView> getScheduledShowtimesByCentreId(int centreId) {
        String sql = "SELECT s.ShowtimeID, m.MovieID, b.CentreID, m.Title, m.PosterURL, m.AgeRating, m.Duration, m.Genre, "
                + "b.CentreName, b.City, h.HallName, "
                + "s.StartTime, s.BaseTicketPrice "
                + "FROM Showtimes s "
                + "JOIN Movies m ON m.MovieID = s.MovieID "
                + "JOIN Halls h ON h.HallID = s.HallID "
                + "JOIN Centres b ON b.CentreID = h.CentreID "
                + "WHERE s.Status = 'SCHEDULED' AND b.CentreID = ? "
                + "ORDER BY s.StartTime";
        List<ShowtimeView> showtimes = new ArrayList<>();

        try (Connection connection = DBContext.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, centreId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ShowtimeView showtime = new ShowtimeView();
                    showtime.setShowtimeId(resultSet.getInt("ShowtimeID"));
                    showtime.setMovieId(resultSet.getInt("MovieID"));
                    showtime.setCentreId(resultSet.getInt("CentreID"));
                    showtime.setMovieTitle(resultSet.getString("Title"));
                    showtime.setPosterUrl(resultSet.getString("PosterURL"));
                    showtime.setAgeRating(resultSet.getString("AgeRating"));
                    showtime.setDuration(resultSet.getInt("Duration"));
                    showtime.setGenre(resultSet.getString("Genre"));
                    showtime.setCentreName(resultSet.getString("CentreName"));
                    showtime.setCity(resultSet.getString("City"));
                    showtime.setHallName(resultSet.getString("HallName"));
                    showtime.setStartTime(resultSet.getTimestamp("StartTime"));
                    showtime.setBaseTicketPrice(resultSet.getBigDecimal("BaseTicketPrice"));
                    showtimes.add(showtime);
                }
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

    public List<Centre> getAllCentresWithManager() {
        String sql = "SELECT b.CentreID, b.CentreName, b.Address, b.City, b.Phone, b.Status, "
                + "COUNT(DISTINCT h.HallID) AS HallCount, "
                + "COALESCE(SUM(h.Capacity), 0) AS SeatCount, "
                + "u.UserID AS ManagerID, u.FullName AS ManagerName, u.Email AS ManagerEmail "
                + "FROM Centres b "
                + "LEFT JOIN Halls h ON h.CentreID = b.CentreID "
                + "LEFT JOIN Users u ON u.CentreID = b.CentreID AND u.Role = 'MANAGER' AND u.Status = 'ACTIVE' "
                + "GROUP BY b.CentreID, b.CentreName, b.Address, b.City, b.Phone, b.Status, u.UserID, u.FullName, u.Email "
                + "ORDER BY b.CentreID DESC";
        List<Centre> list = new ArrayList<>();
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Centre centre = new Centre();
                centre.setCentreId(rs.getInt("CentreID"));
                centre.setCentreName(rs.getString("CentreName"));
                centre.setAddress(rs.getString("Address"));
                centre.setCity(rs.getString("City"));
                centre.setPhone(rs.getString("Phone"));
                centre.setStatus(rs.getString("Status"));
                centre.setHallCount(rs.getInt("HallCount"));
                centre.setSeatCount(rs.getInt("SeatCount"));

                int mgrId = rs.getInt("ManagerID");
                if (!rs.wasNull()) {
                    centre.setManagerId(mgrId);
                    centre.setManagerName(rs.getString("ManagerName"));
                    centre.setManagerEmail(rs.getString("ManagerEmail"));
                }
                list.add(centre);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}

