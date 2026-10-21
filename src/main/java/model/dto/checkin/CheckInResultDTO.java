package model.dto.checkin;

import model.dto.booking.TicketInfoDTO;

public class CheckInResultDTO {

    private final boolean success;
    private final String message;
    private final TicketInfoDTO ticket;

    public CheckInResultDTO(boolean success, String message, TicketInfoDTO ticket) {
        this.success = success;
        this.message = message;
        this.ticket = ticket;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public TicketInfoDTO getTicket() { return ticket; }
}
