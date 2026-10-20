package model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Model đại diện cho bảng giá cơ sở theo cụm rạp / chi nhánh (BranchPricing / CentrePricing).
 */
public class CentrePricing implements Serializable {
    private static final long serialVersionUID = 1L;

    private int pricingId;
    private int centreId;
    private BigDecimal basePrice;
    private Timestamp effectiveFrom;
    private Timestamp effectiveTo;
    private Integer updatedBy;
    private Timestamp createdAt;

    public CentrePricing() {
    }

    public CentrePricing(int pricingId, int centreId, BigDecimal basePrice, Timestamp effectiveFrom, Timestamp effectiveTo, Integer updatedBy, Timestamp createdAt) {
        this.pricingId = pricingId;
        this.centreId = centreId;
        this.basePrice = basePrice;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.updatedBy = updatedBy;
        this.createdAt = createdAt;
    }

    public int getPricingId() {
        return pricingId;
    }

    public void setPricingId(int pricingId) {
        this.pricingId = pricingId;
    }

    public int getBranchPricingId() {
        return pricingId;
    }

    public void setBranchPricingId(int pricingId) {
        this.pricingId = pricingId;
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

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
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
