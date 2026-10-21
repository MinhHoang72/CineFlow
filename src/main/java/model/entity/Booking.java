package model.entity;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.dto.booking.FnbLineDTO;

public class Booking {

    private final int id;
    private final String code;
    private final Integer userId;
    private final String type, status;
    private final BigDecimal totalAmount;
    private final Timestamp holdUntil, createdAt;
    private final Showtime showtime;
    private final List<Seat> seats;

    public Booking(int id, String code, Integer userId, String type, String status, BigDecimal totalAmount,
            Timestamp holdUntil, Timestamp createdAt, Showtime showtime, List<Seat> seats) {
        this.createdAt = createdAt;
        this.id = id;
        this.code = code;
        this.userId = userId;
        this.type = type;
        this.status = status;
        this.totalAmount = totalAmount;
        this.holdUntil = holdUntil;
        this.showtime = showtime;
        this.seats = seats;
    }

    public int getId() { return id; }
    public int getBookingId() { return id; }
    public String getCode() { return code; }
    public String getBookingCode() { return code; }
    public Integer getUserId() { return userId; }
    public String getType() { return type; }
    public boolean isCounter() { return "COUNTER".equals(type); }
    public String getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Timestamp getHoldUntil() { return holdUntil; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Showtime getShowtime() { return showtime; }
    public List<Seat> getSeats() { return seats; }

    // Tien ve / giam gia / bap nuoc (BookingDAO.getBooking dien vao)
    private BigDecimal subTotal = BigDecimal.ZERO, discountAmount = BigDecimal.ZERO;
    private String voucherCode;
    private List<FnbLineDTO> fnbLines = List.of();

    /** Tien ve (tong gia ghe), voucher chi giam tren khoan nay. */
    public BigDecimal getSubTotal() { return subTotal; }
    public void setSubTotal(BigDecimal subTotal) { if (subTotal != null) this.subTotal = subTotal; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { if (discountAmount != null) this.discountAmount = discountAmount; }
    /** Ma voucher da ap dung, null neu khong co. */
    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }
    public List<FnbLineDTO> getFnbLines() { return fnbLines; }
    public void setFnbLines(List<FnbLineDTO> fnbLines) { if (fnbLines != null) this.fnbLines = fnbLines; }
    public BigDecimal getFnbTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (FnbLineDTO line : fnbLines) total = total.add(line.getLineTotal());
        return total;
    }
    /** So luong da chon theo key menu (I12 / C3), de dien lai form chon bap nuoc. */
    public Map<String, Integer> getFnbQuantities() {
        Map<String, Integer> map = new HashMap<>();
        for (FnbLineDTO line : fnbLines) map.put(line.getKey(), line.getQuantity());
        return map;
    }

    public boolean isPending() { return "PENDING".equals(status); }

    /** Trang thai hien thi: don PENDING da qua gio giu ghe coi nhu het han. */
    public String getStatusLabel() {
        if (isPending()) return isHoldActive() ? "Chờ thanh toán" : "Hết hạn";
        return switch (status) {
            case "CONFIRMED" -> "Đã thanh toán";
            case "COMPLETED" -> "Hoàn tất";
            case "CANCELLED" -> "Đã huỷ";
            case "EXPIRED" -> "Hết hạn";
            default -> status;
        };
    }

    /** Lop mau Bootstrap cho badge trang thai. */
    public String getStatusBadge() {
        if (isPending()) return isHoldActive() ? "bg-warning text-dark" : "bg-secondary";
        return switch (status) {
            case "CONFIRMED", "COMPLETED" -> "bg-success";
            case "CANCELLED" -> "bg-danger";
            default -> "bg-secondary";
        };
    }

    /** Noi dung ma QR cua ca don (mot ma cho tat ca ghe; sau nay dung chung cho F&B). */
    public String getQrContent() { return "CINEFLOW-BOOKING:" + code; }

    public boolean isHoldActive() {
        return isPending() && holdUntil != null && holdUntil.getTime() > System.currentTimeMillis();
    }

    public long getSecondsLeft() {
        if (!isHoldActive()) return 0;
        return (holdUntil.getTime() - System.currentTimeMillis()) / 1000;
    }

    public String getSeatLabels() {
        StringBuilder sb = new StringBuilder();
        for (Seat s : seats) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(s.getLabel());
        }
        return sb.toString();
    }
}
