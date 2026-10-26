package model.dto.booking;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/** Mot voucher khach chon duoc o trang thanh toan, kem so tien giam cho don dang dat. */
public class VoucherOptionDTO {

    private final String code, name;
    private final boolean percent;
    private final BigDecimal value, maximumDiscount, minimumOrderValue;
    private final LocalDateTime endDate;
    private final BigDecimal discount;
    private final BigDecimal missingAmount;

    /**
     * @param discount so tien giam cho don nay (0 neu chua dung duoc)
     * @param missingAmount tien ve con thieu de dat gia tri toi thieu (0 = dung duoc ngay)
     */
    public VoucherOptionDTO(String code, String name, boolean percent, BigDecimal value, BigDecimal maximumDiscount,
            BigDecimal minimumOrderValue, LocalDateTime endDate, BigDecimal discount, BigDecimal missingAmount) {
        this.code = code;
        this.name = name;
        this.percent = percent;
        this.value = value;
        this.maximumDiscount = maximumDiscount;
        this.minimumOrderValue = minimumOrderValue;
        this.endDate = endDate;
        this.discount = discount;
        this.missingAmount = missingAmount;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    /** true = giam theo %, false = giam so tien co dinh. */
    public boolean isPercent() { return percent; }
    public BigDecimal getValue() { return value; }
    public BigDecimal getMaximumDiscount() { return maximumDiscount; }
    public BigDecimal getMinimumOrderValue() { return minimumOrderValue; }
    /** Han dung kieu Date de dung fmt:formatDate trong JSP; null neu khong gioi han. */
    public Date getEndDate() { return endDate == null ? null : java.sql.Timestamp.valueOf(endDate); }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getMissingAmount() { return missingAmount; }
    public boolean isUsable() { return missingAmount.signum() == 0; }
}
