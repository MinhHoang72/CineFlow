package model.dto;

import model.dto.booking.TicketInfoDTO;

/**
 * Tương thích ngược với mã nguồn cũ.
 * Khuyến nghị sử dụng model.dto.checkin.CheckInResultDTO.
 */
public class CheckInResultDTO extends model.dto.checkin.CheckInResultDTO {

    public CheckInResultDTO(boolean success, String message, TicketInfoDTO ticket) {
        super(success, message, ticket);
    }
}
