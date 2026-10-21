package model.entity;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author vktuy
 */

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.Locale;

public class BranchPricing {

    private int branchPricingId;
    private int branchId;
    private double basePrice;

    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;

    private Integer updatedBy;
    private LocalDateTime createdAt;

    public BranchPricing() {
    }

    public BranchPricing(int branchPricingId,
                         int branchId,
                         double basePrice,
                         LocalDateTime effectiveFrom,
                         LocalDateTime effectiveTo,
                         Integer updatedBy,
                         LocalDateTime createdAt) {

        this.branchPricingId = branchPricingId;
        this.branchId = branchId;
        this.basePrice = basePrice;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.updatedBy = updatedBy;
        this.createdAt = createdAt;
    }

    public int getBranchPricingId() {
        return branchPricingId;
    }

    public void setBranchPricingId(int branchPricingId) {
        this.branchPricingId = branchPricingId;
    }

    public int getCentreId() { return branchId; }
    public void setCentreId(int centreId) { this.branchId = centreId; }

    public int getBranchId() {
        return branchId;
    }

    public void setBranchId(int branchId) {
        this.branchId = branchId;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
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
    
    public String getEffectiveFromFormatted() {
    if (effectiveFrom == null) {
        return "";
    }

    DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    return effectiveFrom.format(formatter);
}

public String getEffectiveToFormatted() {
    if (effectiveTo == null) {
        return "Current";
    }

    DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    return effectiveTo.format(formatter);
}

public String getBasePriceFormatted() {
    NumberFormat formatter =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    return formatter.format(basePrice) + " vnd";
}
    
}
