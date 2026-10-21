package model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Voucher {

    private int voucherId;
    private String voucherCode;
    private String voucherName;
    private String description;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minimumOrderValue;
    private BigDecimal maximumDiscount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer usageLimit;
    private int usedCount;
    private String status;

    /**
     * NULL = voucher toàn chuỗi do Admin tạo (áp dụng mọi chi nhánh).
     * Có giá trị = voucher chi nhánh do Manager tạo (chỉ áp dụng tại chi nhánh đó).
     */
    private Integer centreId;

    /** ID người tạo voucher (Admin hoặc Manager). */
    private Integer createdBy;

    /** Tên chi nhánh, JOIN từ Centres – dùng cho hiển thị, không lưu DB. */
    private String centreName;

    public Voucher() {
    }

    public Voucher(int voucherId, String voucherCode, String voucherName, String description,
                   String discountType, BigDecimal discountValue, BigDecimal minimumOrderValue,
                   BigDecimal maximumDiscount, LocalDateTime startDate, LocalDateTime endDate,
                   Integer usageLimit, int usedCount, String status) {
        this.voucherId = voucherId;
        this.voucherCode = voucherCode;
        this.voucherName = voucherName;
        this.description = description;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minimumOrderValue = minimumOrderValue;
        this.maximumDiscount = maximumDiscount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.usageLimit = usageLimit;
        this.usedCount = usedCount;
        this.status = status;
    }

    public int getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(int voucherId) {
        this.voucherId = voucherId;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }

    public String getVoucherName() {
        return voucherName;
    }

    public void setVoucherName(String voucherName) {
        this.voucherName = voucherName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public BigDecimal getMinimumOrderValue() {
        return minimumOrderValue;
    }

    public void setMinimumOrderValue(BigDecimal minimumOrderValue) {
        this.minimumOrderValue = minimumOrderValue;
    }

    public BigDecimal getMaximumDiscount() {
        return maximumDiscount;
    }

    public void setMaximumDiscount(BigDecimal maximumDiscount) {
        this.maximumDiscount = maximumDiscount;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public Integer getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(Integer usageLimit) {
        this.usageLimit = usageLimit;
    }

    public int getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(int usedCount) {
        this.usedCount = usedCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCentreId() {
        return centreId;
    }

    public void setCentreId(Integer centreId) {
        this.centreId = centreId;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public String getCentreName() {
        return centreName;
    }

    public void setCentreName(String centreName) {
        this.centreName = centreName;
    }

    /** Voucher áp dụng toàn chuỗi (do Admin tạo, CentreID = NULL). */
    public boolean isGlobal() {
        return centreId == null;
    }

    public boolean isExpired() {
        return endDate != null && endDate.isBefore(LocalDateTime.now());
    }

    public boolean isExhausted() {
        return usageLimit != null && usedCount >= usageLimit;
    }

    @Override
    public String toString() {
        return "Voucher{" + "voucherId=" + voucherId + ", voucherCode=" + voucherCode
                + ", voucherName=" + voucherName + ", centreId=" + centreId
                + ", status=" + status + '}';
    }
}
