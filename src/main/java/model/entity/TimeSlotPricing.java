package model.entity;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class TimeSlotPricing {

    private int timeSlotPricingId;
    private int branchId;
    private String slotName;

    private LocalTime startTime;
    private LocalTime endTime;

    private double surcharge;

    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;

    private Integer updatedBy;
    private LocalDateTime createdAt;

    public TimeSlotPricing() {
    }

    public int getTimeSlotPricingId() {
        return timeSlotPricingId;
    }

    public void setTimeSlotPricingId(int timeSlotPricingId) {
        this.timeSlotPricingId = timeSlotPricingId;
    }

    public int getCentreId() { return branchId; }
    public void setCentreId(int centreId) { this.branchId = centreId; }

    public int getBranchId() {
        return branchId;
    }

    public void setBranchId(int branchId) {
        this.branchId = branchId;
    }

    public String getSlotName() {
        return slotName;
    }

    public void setSlotName(String slotName) {
        this.slotName = slotName;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public double getSurcharge() {
        return surcharge;
    }

    public void setSurcharge(double surcharge) {
        this.surcharge = surcharge;
    }

    public LocalDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDateTime effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDateTime getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDateTime effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public Integer getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Integer updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getStartTimeFormatted() {
        if (startTime == null) {
            return "";
        }

        return startTime.format(
                DateTimeFormatter.ofPattern("HH:mm")
        );
    }

    public String getEndTimeFormatted() {
        if (endTime == null) {
            return "";
        }

        return endTime.format(
                DateTimeFormatter.ofPattern("HH:mm")
        );
    }

    public String getSurchargeFormatted() {
        NumberFormat formatter =
                NumberFormat.getCurrencyInstance(
                        new Locale("vi", "VN")
                );

        return formatter.format(surcharge);
    }

    public String getEffectiveFromFormatted() {
        if (effectiveFrom == null) {
            return "";
        }

        return effectiveFrom.format(
                DateTimeFormatter.ofPattern(
                        "dd/MM/yyyy HH:mm:ss"
                )
        );
    }

    public String getEffectiveToFormatted() {

        if (effectiveTo == null) {
            return "Current";
        }

        return effectiveTo.format(
                DateTimeFormatter.ofPattern(
                        "dd/MM/yyyy HH:mm:ss"
                )
        );
    }
}
