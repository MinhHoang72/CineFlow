package model.entity;

import java.sql.Timestamp;

public class VoucherDay {
    private int voucherDayId;
    private int voucherId;
    private String dayOfWeek;
    private Timestamp createdAt;

    public VoucherDay() {
    }

    public VoucherDay(int voucherId, String dayOfWeek) {
        this.voucherId = voucherId;
        this.dayOfWeek = dayOfWeek;
    }

    public int getVoucherDayId() {
        return voucherDayId;
    }

    public void setVoucherDayId(int voucherDayId) {
        this.voucherDayId = voucherDayId;
    }

    public int getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(int voucherId) {
        this.voucherId = voucherId;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
