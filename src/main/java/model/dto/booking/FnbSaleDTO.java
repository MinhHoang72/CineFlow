package model.dto.booking;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Don ban bap nuoc le tai quay (FnbOrders khong kem ve, BookingID = NULL). */
public class FnbSaleDTO {

    private final int id, centreId;
    private final String code, centreName, status;
    private final BigDecimal totalAmount;
    private final Timestamp createdAt;
    private final long secondsLeft;
    private List<FnbLineDTO> lines = new ArrayList<>();

    public FnbSaleDTO(int id, String code, int centreId, String centreName, String status, BigDecimal totalAmount,
            Timestamp createdAt, long secondsLeft) {
        this.id = id;
        this.code = code;
        this.centreId = centreId;
        this.centreName = centreName;
        this.status = status;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.secondsLeft = secondsLeft;
    }

    public int getId() { return id; }
    public String getCode() { return code; }
    public int getCentreId() { return centreId; }
    public String getCentreName() { return centreName; }
    /** PENDING (cho thanh toan), PREPARING (da thanh toan), CANCELLED... */
    public String getStatus() { return status; }
    public boolean isPending() { return "PENDING".equals(status); }
    /** Da thanh toan (bep dang chuan bi / da giao). */
    public boolean isPaid() { return "PREPARING".equals(status) || "READY".equals(status) || "DELIVERED".equals(status); }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Timestamp getCreatedAt() { return createdAt; }
    /** So giay con lai truoc khi don cho thanh toan tu huy. */
    public long getSecondsLeft() { return secondsLeft; }
    public List<FnbLineDTO> getLines() { return lines; }
    public void setLines(List<FnbLineDTO> lines) { this.lines = lines; }
}
