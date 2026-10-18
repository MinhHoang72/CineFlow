package model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;

/**
 * Model đại diện cho bảng phụ thu theo khung giờ chiếu (TimeSlotPricing).
 */
public class TimeSlotPricing implements Serializable {
    private static final long serialVersionUID = 1L;

    private int timeSlotPricingId;
    private int centreId; // BranchID / CentreID
    private String slotName;
    private Time startTime;
    private Time endTime;
    private BigDecimal surcharge;
    private Timestamp effectiveFrom;
    private Timestamp effectiveTo;
    private Integer updatedBy;
    private Timestamp createdAt;

    public TimeSlotPricing() {
    }

    public TimeSlotPricing(int timeSlotPricingId, int centreId, String slotName, Time startTime, Time endTime, BigDecimal surcharge, Timestamp effectiveFrom, Timestamp effectiveTo, Integer updatedBy, Timestamp createdAt) {
        this.timeSlotPricingId = timeSlotPricingId;
        this.centreId = centreId;
        this.slotName = slotName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.surcharge = surcharge;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.updatedBy = updatedBy;
        this.createdAt = createdAt;
    }

    public int getTimeSlotPricingId() {
        return timeSlotPricingId;
    }

    public void setTimeSlotPricingId(int timeSlotPricingId) {
        this.timeSlotPricingId = timeSlotPricingId;
    }

    public int getCentreId() {
        return centreId;
    }

    public void setCentreId(int centreId) {
        this.centreId = centreId;
    }

    public int getBranchId() {
        return centreId;
    }

    public void setBranchId(int branchId) {
        this.centreId = branchId;
    }

    public String getSlotName() {
        return slotName;
    }

    public void setSlotName(String slotName) {
        this.slotName = slotName;
    }

    public Time getStartTime() {
        return startTime;
    }

    public void setStartTime(Time startTime) {
        this.startTime = startTime;
    }

    public Time getEndTime() {
        return endTime;
    }

    public void setEndTime(Time endTime) {
        this.endTime = endTime;
    }

    public BigDecimal getSurcharge() {
        return surcharge;
    }

    public void setSurcharge(BigDecimal surcharge) {
        this.surcharge = surcharge;
    }

    public Timestamp getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Timestamp effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Timestamp getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(Timestamp effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public Integer getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Integer updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
