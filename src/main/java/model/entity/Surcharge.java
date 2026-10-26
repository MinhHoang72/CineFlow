package model.entity;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Surcharge {

    private int surchargeId;
    private String surchargeName;
    private String targetValue;
    private String surchargeType;
    public String getSurchargeType() { return surchargeType; }
    public void setSurchargeType(String surchargeType) { this.surchargeType = surchargeType; }
    private double amount;

    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;

    private Integer updatedBy;
    private String updatedByName;
    private LocalDateTime createdAt;

    public Surcharge() {
    }

    public int getSurchargeId() {
        return surchargeId;
    }

    public void setSurchargeId(int surchargeId) {
        this.surchargeId = surchargeId;
    }

    public String getSurchargeName() {
        return surchargeName;
    }

    public void setSurchargeName(String surchargeName) {
        this.surchargeName = surchargeName;
    }

    public String getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(String targetValue) {
        this.targetValue = targetValue;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
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

    public String getUpdatedByName() {
        return updatedByName;
    }

    public void setUpdatedByName(String updatedByName) {
        this.updatedByName = updatedByName;
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

    public String getAmountFormatted() {
        NumberFormat formatter =
                NumberFormat.getCurrencyInstance(
                        new Locale("vi", "VN")
                );

        return formatter.format(amount);
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
