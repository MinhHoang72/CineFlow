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

public class CentrePricing {

    private int centrePricingId;
    private int centreId;
    private double basePrice;

    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;

    private Integer updatedBy;
    private String updatedByName;
    private LocalDateTime createdAt;

    public CentrePricing() {
    }

    public CentrePricing(int centrePricingId,
                         int centreId,
                         double basePrice,
                         LocalDateTime effectiveFrom,
                         LocalDateTime effectiveTo,
                         Integer updatedBy,
                         LocalDateTime createdAt) {

        this.centrePricingId = centrePricingId;
        this.centreId = centreId;
        this.basePrice = basePrice;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.updatedBy = updatedBy;
        this.createdAt = createdAt;
    }

    public int getCentrePricingId() {
        return centrePricingId;
    }

    public void setCentrePricingId(int centrePricingId) {
        this.centrePricingId = centrePricingId;
    }

    public int getCentreId() { return centreId; }
    public void setCentreId(int centreId) { this.centreId = centreId; }





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

    public String getUpdatedByName() {
        return updatedByName;
    }

    public void setUpdatedByName(String updatedByName) {
        this.updatedByName = updatedByName;
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

    return formatter.format(basePrice) + " đ";
}

    public boolean isScheduled() {
        return effectiveFrom != null && effectiveFrom.isAfter(LocalDateTime.now());
    }
    public boolean isCurrent() {
        LocalDateTime now = LocalDateTime.now();
        return effectiveFrom != null && !effectiveFrom.isAfter(now)
                && (effectiveTo == null || effectiveTo.isAfter(now));
    }
}
