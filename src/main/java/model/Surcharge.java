package model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Model đại diện cho bảng phụ thu loại ghế và loại phòng chiếu (Surcharges).
 */
public class Surcharge implements Serializable {
    private static final long serialVersionUID = 1L;

    private int surchargeId;
    private String surchargeName;
    private String surchargeType; // SEAT, HALL
    private String targetValue;   // VIP, SWEETBOX, 3D, IMAX...
    private BigDecimal amount;
    private Timestamp effectiveFrom;
    private Timestamp effectiveTo;
    private Integer updatedBy;
    private Timestamp createdAt;

    public Surcharge() {
    }

    public Surcharge(int surchargeId, String surchargeName, String surchargeType, String targetValue, BigDecimal amount, Timestamp effectiveFrom, Timestamp effectiveTo, Integer updatedBy, Timestamp createdAt) {
        this.surchargeId = surchargeId;
        this.surchargeName = surchargeName;
        this.surchargeType = surchargeType;
        this.targetValue = targetValue;
        this.amount = amount;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.updatedBy = updatedBy;
        this.createdAt = createdAt;
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

    public String getSurchargeType() {
        return surchargeType;
    }

    public void setSurchargeType(String surchargeType) {
        this.surchargeType = surchargeType;
    }

    public String getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(String targetValue) {
        this.targetValue = targetValue;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
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
