package model;

public class Ticket {

    private final String code, qrContent, seatLabel, seatType, status;

    public Ticket(String code, String qrContent, String seatLabel, String seatType, String status) {
        this.code = code;
        this.qrContent = qrContent;
        this.seatLabel = seatLabel;
        this.seatType = seatType;
        this.status = status;
    }

    public String getCode() { return code; }
    public String getQrContent() { return qrContent; }
    public String getSeatLabel() { return seatLabel; }
    public String getSeatType() { return seatType; }
    public String getStatus() { return status; }
}
