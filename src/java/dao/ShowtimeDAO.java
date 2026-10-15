package dao;
import utils.DBContext;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import model.Showtime;

public class ShowtimeDAO extends DBContext {

    /* =====================================================
       1. CHECK HALL AVAILABLE
       ===================================================== */
    public boolean isHallAvailable(int hallId) {

        String sql
                = "SELECT 1 "
                + "FROM Halls "
                + "WHERE HallID = ? "
                + "AND Status = 'AVAILABLE'";

        try (PreparedStatement ps
                = connection.prepareStatement(sql)) {

            ps.setInt(1, hallId);

            ResultSet rs = ps.executeQuery();

            return rs.next();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }


    /* =====================================================
       2. CHECK MOVIE IS DISTRIBUTED TO HALL'S BRANCH
       ===================================================== */
    public boolean isMovieAvailableForHall(
            int movieId,
            int hallId) {

        String sql
                = "SELECT 1 "
                + "FROM Halls h "
                + "JOIN BranchMovies bm "
                + "ON bm.BranchID = h.BranchID "
                + "WHERE h.HallID = ? "
                + "AND bm.MovieID = ? "
                + "AND bm.IsActive = 1";

        try (PreparedStatement ps
                = connection.prepareStatement(sql)) {

            ps.setInt(1, hallId);
            ps.setInt(2, movieId);

            ResultSet rs = ps.executeQuery();

            return rs.next();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }


    /* =====================================================
       3. CHECK SCHEDULE CONFLICT
       ===================================================== */
    public boolean hasScheduleConflict(
            int hallId,
            LocalDateTime newStart,
            LocalDateTime newEnd,
            int newBufferMinutes,
            Integer excludeShowtimeId) {

        String sql
                = "SELECT COUNT(*) AS Total "
                + "FROM Showtimes "
                + "WHERE HallID = ? "
                + "AND Status <> 'CANCELLED' "
                + "AND StartTime < DATEADD(MINUTE, ?, ?) "
                + "AND DATEADD(MINUTE, CleaningBufferMinutes, EndTime) > ? ";

        if (excludeShowtimeId != null) {
            sql += "AND ShowtimeID <> ?";
        }

        try (PreparedStatement ps
                = connection.prepareStatement(sql)) {

            ps.setInt(1, hallId);

            ps.setInt(
                    2,
                    newBufferMinutes
            );

            ps.setTimestamp(
                    3,
                    Timestamp.valueOf(newEnd)
            );

            ps.setTimestamp(
                    4,
                    Timestamp.valueOf(newStart)
            );

            if (excludeShowtimeId != null) {
                ps.setInt(
                        5,
                        excludeShowtimeId
                );
            }

            ResultSet rs
                    = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("Total") > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }


    /* =====================================================
       4. INSERT SHOWTIME
       ===================================================== */
    public boolean insertShowtime(
            Showtime showtime) {

        String sql
                = "INSERT INTO Showtimes "
                + "(MovieID, HallID, "
                + "StartTime, EndTime, "
                + "CleaningBufferMinutes, "
                + "Status, CreatedBy) "
                + "VALUES (?, ?, ?, ?, ?, 'SCHEDULED', ?)";

        try (PreparedStatement ps
                = connection.prepareStatement(sql)) {

            ps.setInt(
                    1,
                    showtime.getMovieId()
            );

            ps.setInt(
                    2,
                    showtime.getHallId()
            );

            ps.setTimestamp(
                    3,
                    Timestamp.valueOf(
                            showtime.getStartTime()
                    )
            );

            ps.setTimestamp(
                    4,
                    Timestamp.valueOf(
                            showtime.getEndTime()
                    )
            );

            ps.setInt(
                    5,
                    showtime.getCleaningBufferMinutes()
            );

            if (showtime.getCreatedBy() != null) {
                ps.setInt(
                        6,
                        showtime.getCreatedBy()
                );
            } else {
                ps.setNull(
                        6,
                        Types.INTEGER
                );
            }

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }


    /* =====================================================
       5. GET SHOWTIMES BY BRANCH
       ===================================================== */
    public List<Showtime> getByBranch(
            int branchId) {

        List<Showtime> list
                = new ArrayList<>();

        String sql
                = "SELECT s.*, m.Title AS MovieTitle "
                + "FROM Showtimes s "
                + "JOIN Halls h "
                + "ON h.HallID = s.HallID "
                + "JOIN Movies m "
                + "ON m.MovieID = s.MovieID "
                + "WHERE h.BranchID = ? "
                + "ORDER BY s.StartTime";

        try (PreparedStatement ps
                = connection.prepareStatement(sql)) {

            ps.setInt(1, branchId);

            ResultSet rs
                    = ps.executeQuery();

            while (rs.next()) {

                Showtime s = mapShowtime(rs);

                s.setMovieTitle(
                        rs.getString("MovieTitle")
                );

                list.add(s);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }


    /* =====================================================
       6. GET SHOWTIME BY ID
       ===================================================== */
    public Showtime getById(
            int showtimeId) {

        String sql
                = "SELECT * "
                + "FROM Showtimes "
                + "WHERE ShowtimeID = ?";

        try (PreparedStatement ps
                = connection.prepareStatement(sql)) {

            ps.setInt(
                    1,
                    showtimeId
            );

            ResultSet rs
                    = ps.executeQuery();

            if (rs.next()) {
                return mapShowtime(rs);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }


    /* =====================================================
       7. UPDATE SHOWTIME
       ===================================================== */
    public boolean updateShowtime(
            Showtime showtime) {

        String sql
                = "UPDATE Showtimes "
                + "SET MovieID = ?, "
                + "HallID = ?, "
                + "StartTime = ?, "
                + "EndTime = ?, "
                + "CleaningBufferMinutes = ?, "
                + "UpdatedAt = GETDATE() "
                + "WHERE ShowtimeID = ? "
                + "AND Status = 'SCHEDULED'";

        try (PreparedStatement ps
                = connection.prepareStatement(sql)) {

            ps.setInt(
                    1,
                    showtime.getMovieId()
            );

            ps.setInt(
                    2,
                    showtime.getHallId()
            );

            ps.setTimestamp(
                    3,
                    Timestamp.valueOf(
                            showtime.getStartTime()
                    )
            );

            ps.setTimestamp(
                    4,
                    Timestamp.valueOf(
                            showtime.getEndTime()
                    )
            );

            ps.setInt(
                    5,
                    showtime.getCleaningBufferMinutes()
            );

            ps.setInt(
                    6,
                    showtime.getShowtimeId()
            );

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }


    /* =====================================================
       8. CHECK ACTIVE BOOKINGS
       ===================================================== */
    public boolean hasActiveBookings(
            int showtimeId) {

        String sql
                = "SELECT COUNT(*) AS Total "
                + "FROM Bookings "
                + "WHERE ShowtimeID = ? "
                + "AND BookingStatus IN "
                + "('PENDING','CONFIRMED')";

        try (PreparedStatement ps
                = connection.prepareStatement(sql)) {

            ps.setInt(
                    1,
                    showtimeId
            );

            ResultSet rs
                    = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("Total") > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }


    /* =====================================================
       9. CANCEL SHOWTIME
       ===================================================== */
    public boolean cancelShowtime(
            int showtimeId) {

        String sql
                = "UPDATE Showtimes "
                + "SET Status = 'CANCELLED', "
                + "UpdatedAt = GETDATE() "
                + "WHERE ShowtimeID = ? "
                + "AND Status = 'SCHEDULED'";

        try (PreparedStatement ps
                = connection.prepareStatement(sql)) {

            ps.setInt(
                    1,
                    showtimeId
            );

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }


    /* =====================================================
       10. RESULTSET -> SHOWTIME
       ===================================================== */
    private Showtime mapShowtime(
            ResultSet rs)
            throws SQLException {

        Showtime s
                = new Showtime();

        s.setShowtimeId(
                rs.getInt(
                        "ShowtimeID"
                )
        );

        s.setMovieId(
                rs.getInt(
                        "MovieID"
                )
        );

        s.setHallId(
                rs.getInt(
                        "HallID"
                )
        );

        Timestamp start
                = rs.getTimestamp(
                        "StartTime"
                );

        if (start != null) {
            s.setStartTime(
                    start.toLocalDateTime()
            );
        }

        Timestamp end
                = rs.getTimestamp(
                        "EndTime"
                );

        if (end != null) {
            s.setEndTime(
                    end.toLocalDateTime()
            );
        }

        s.setCleaningBufferMinutes(
                rs.getInt(
                        "CleaningBufferMinutes"
                )
        );

        s.setStatus(
                rs.getString(
                        "Status"
                )
        );

        int createdBy
                = rs.getInt(
                        "CreatedBy"
                );

        if (!rs.wasNull()) {
            s.setCreatedBy(createdBy);
        }

        Timestamp createdAt
                = rs.getTimestamp(
                        "CreatedAt"
                );

        if (createdAt != null) {
            s.setCreatedAt(
                    createdAt.toLocalDateTime()
            );
        }

        Timestamp updatedAt
                = rs.getTimestamp(
                        "UpdatedAt"
                );

        if (updatedAt != null) {
            s.setUpdatedAt(
                    updatedAt.toLocalDateTime()
            );
        }

        return s;
    }
}
