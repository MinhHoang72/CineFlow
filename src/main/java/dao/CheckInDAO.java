package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import model.dto.checkin.CheckInLogDTO;
import model.dto.checkin.CheckInResultDTO;
import model.dto.booking.TicketInfoDTO;
import utils.DBContext;

public class CheckInDAO {

    private static final String TICKET_SELECT
            = "SELECT t.TicketID, t.TicketCode, t.TicketStatus, t.CheckedInAt, b.BookingCode, b.BookingStatus, "
            + "se.SeatRow + CAST(se.SeatNumber AS VARCHAR(10)) AS SeatLabel, m.Title, h.HallName, "
            + "c.CentreID, c.CentreName, st.StartTime, st.EndTime, st.Status AS ShowtimeStatus "
            + "FROM Tickets t WITH (UPDLOCK, ROWLOCK) "
            + "JOIN BookingSeats bs ON bs.BookingSeatID = t.BookingSeatID "
            + "JOIN Bookings b ON b.BookingID = bs.BookingID "
            + "JOIN Seats se ON se.SeatID = bs.SeatID "
            + "JOIN Showtimes st ON st.ShowtimeID = b.ShowtimeID "
            + "JOIN Movies m ON m.MovieID = st.MovieID "
            + "JOIN Halls h ON h.HallID = st.HallID "
            + "JOIN Centres c ON c.CentreID = h.CentreID "
            + "WHERE t.TicketCode = ? OR t.QRCode = ?";

    public CheckInResultDTO checkIn(String code, int staffId, int centreId) throws SQLException {
        try (Connection connection = DBContext.getConnection()) {
            connection.setAutoCommit(false);
            try {
                TicketInfoDTO ticket = null;
                try (PreparedStatement statement = connection.prepareStatement(TICKET_SELECT)) {
                    statement.setString(1, code);
                    statement.setString(2, code);
                    try (ResultSet rs = statement.executeQuery()) {
                        if (rs.next()) {
                            ticket = new TicketInfoDTO();
                            ticket.setTicketId(rs.getInt("TicketID"));
                            ticket.setTicketCode(rs.getString("TicketCode"));
                            ticket.setTicketStatus(rs.getString("TicketStatus"));
                            ticket.setCheckedInAt(rs.getTimestamp("CheckedInAt"));
                            ticket.setBookingCode(rs.getString("BookingCode"));
                            ticket.setBookingStatus(rs.getString("BookingStatus"));
                            ticket.setSeatLabel(rs.getString("SeatLabel"));
                            ticket.setMovieTitle(rs.getString("Title"));
                            ticket.setHallName(rs.getString("HallName"));
                            ticket.setCentreId(rs.getInt("CentreID"));
                            ticket.setCentreName(rs.getString("CentreName"));
                            ticket.setStartTime(rs.getTimestamp("StartTime"));
                            ticket.setEndTime(rs.getTimestamp("EndTime"));
                            ticket.setShowtimeStatus(rs.getString("ShowtimeStatus"));
                        }
                    }
                }

                if (ticket == null) {
                    connection.rollback();
                    return new CheckInResultDTO(false, "Không tìm thấy vé với mã này.", null);
                }

                String reason = null;
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime start = ticket.getStartTime().toLocalDateTime();
                LocalDateTime end = ticket.getEndTime().toLocalDateTime();
                if (ticket.getCentreId() != centreId) {
                    reason = "Vé thuộc Centre khác (" + ticket.getCentreName() + ").";
                } else if ("USED".equals(ticket.getTicketStatus())) {
                    reason = "Vé đã được sử dụng lúc " + ticket.getCheckedInAt().toLocalDateTime().toString().replace('T', ' ') + ".";
                } else if ("CANCELLED".equals(ticket.getTicketStatus())) {
                    reason = "Vé đã bị hủy.";
                } else if ("EXPIRED".equals(ticket.getTicketStatus())) {
                    reason = "Vé đã hết hạn.";
                } else if (!"CONFIRMED".equals(ticket.getBookingStatus()) && !"COMPLETED".equals(ticket.getBookingStatus())) {
                    reason = "Đơn đặt vé chưa được thanh toán hoặc đã hủy.";
                } else if ("CANCELLED".equals(ticket.getShowtimeStatus())) {
                    reason = "Suất chiếu đã bị hủy.";
                } else if (now.isBefore(start.minusMinutes(60))) {
                    reason = "Chưa đến giờ soát vé (mở trước giờ chiếu 60 phút).";
                } else if (now.isAfter(end)) {
                    reason = "Suất chiếu đã kết thúc.";
                }

                if (reason == null) {
                    int updated;
                    try (PreparedStatement statement = connection.prepareStatement(
                            "UPDATE Tickets SET TicketStatus = 'USED', CheckedInAt = GETDATE(), CheckedInBy = ? "
                            + "WHERE TicketID = ? AND TicketStatus = 'VALID'")) {
                        statement.setInt(1, staffId);
                        statement.setInt(2, ticket.getTicketId());
                        updated = statement.executeUpdate();
                    }
                    if (updated == 0) {
                        reason = "Vé vừa được sử dụng ở nơi khác.";
                    }
                }

                String result = reason == null ? "SUCCESS" : "REJECTED";
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO CheckInLogs (TicketID, CheckedBy, Result, Note) VALUES (?, ?, ?, ?)")) {
                    statement.setInt(1, ticket.getTicketId());
                    statement.setInt(2, staffId);
                    statement.setString(3, result);
                    statement.setString(4, reason);
                    statement.executeUpdate();
                }
                connection.commit();
                if (reason == null) {
                    ticket.setTicketStatus("USED");
                    ticket.setCheckedInAt(Timestamp.valueOf(now));
                    return new CheckInResultDTO(true, "Soát vé thành công.", ticket);
                }
                return new CheckInResultDTO(false, reason, ticket);
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public List<CheckInLogDTO> recent(int centreId, int limit) {
        List<CheckInLogDTO> list = new ArrayList<>();
        String sql = "SELECT TOP (?) l.CheckInID, t.TicketCode, m.Title, h.HallName, "
                + "se.SeatRow + CAST(se.SeatNumber AS VARCHAR(10)) AS SeatLabel, u.FullName, l.CheckInTime, l.Result, l.Note "
                + "FROM CheckInLogs l "
                + "JOIN Tickets t ON t.TicketID = l.TicketID "
                + "JOIN BookingSeats bs ON bs.BookingSeatID = t.BookingSeatID "
                + "JOIN Bookings b ON b.BookingID = bs.BookingID "
                + "JOIN Seats se ON se.SeatID = bs.SeatID "
                + "JOIN Showtimes st ON st.ShowtimeID = b.ShowtimeID "
                + "JOIN Movies m ON m.MovieID = st.MovieID "
                + "JOIN Halls h ON h.HallID = st.HallID "
                + "JOIN Users u ON u.UserID = l.CheckedBy "
                + "WHERE u.CentreID = ? ORDER BY l.CheckInTime DESC, l.CheckInID DESC";
        try (Connection connection = DBContext.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            statement.setInt(2, centreId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    CheckInLogDTO log = new CheckInLogDTO();
                    log.setCheckInId(rs.getInt("CheckInID"));
                    log.setTicketCode(rs.getString("TicketCode"));
                    log.setMovieTitle(rs.getString("Title"));
                    log.setHallName(rs.getString("HallName"));
                    log.setSeatLabel(rs.getString("SeatLabel"));
                    log.setStaffName(rs.getString("FullName"));
                    log.setCheckInTime(rs.getTimestamp("CheckInTime"));
                    log.setResult(rs.getString("Result"));
                    log.setNote(rs.getString("Note"));
                    list.add(log);
                }
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return list;
    }
}
