package model.dto.booking;

import java.sql.Timestamp;

public class TicketInfoDTO {

    private int ticketId;
    private String ticketCode;
    private String ticketStatus;
    private Timestamp checkedInAt;
    private String bookingCode;
    private String bookingStatus;
    private String seatLabel;
    private String movieTitle;
    private String hallName;
    private int centreId;
    private String centreName;
    private Timestamp startTime;
    private Timestamp endTime;
    private String showtimeStatus;

    public int getTicketId() { return ticketId; }
    public void setTicketId(int ticketId) { this.ticketId = ticketId; }

    public String getTicketCode() { return ticketCode; }
    public void setTicketCode(String ticketCode) { this.ticketCode = ticketCode; }

    public String getTicketStatus() { return ticketStatus; }
    public void setTicketStatus(String ticketStatus) { this.ticketStatus = ticketStatus; }

    public Timestamp getCheckedInAt() { return checkedInAt; }
    public void setCheckedInAt(Timestamp checkedInAt) { this.checkedInAt = checkedInAt; }

    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }

    public String getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }

    public String getSeatLabel() { return seatLabel; }
    public void setSeatLabel(String seatLabel) { this.seatLabel = seatLabel; }

    public String getMovieTitle() { return movieTitle; }
    public void setMovieTitle(String movieTitle) { this.movieTitle = movieTitle; }

    public String getHallName() { return hallName; }
    public void setHallName(String hallName) { this.hallName = hallName; }

    public int getCentreId() { return centreId; }
    public void setCentreId(int centreId) { this.centreId = centreId; }

    public int getBranchId() { return centreId; }
    public void setBranchId(int branchId) { this.centreId = branchId; }

    public String getCentreName() { return centreName; }
    public void setCentreName(String centreName) { this.centreName = centreName; }

    public String getBranchName() { return centreName; }
    public void setBranchName(String branchName) { this.centreName = branchName; }

    public Timestamp getStartTime() { return startTime; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }

    public Timestamp getEndTime() { return endTime; }
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }

    public String getShowtimeStatus() { return showtimeStatus; }
    public void setShowtimeStatus(String showtimeStatus) { this.showtimeStatus = showtimeStatus; }
}
