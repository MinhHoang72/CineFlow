package model.entity;

import java.sql.Timestamp;

public class VoucherCentre {
    private int voucherCentreId;
    private int voucherId;
    private int centreId;
    private String centreName;
    private Timestamp createdAt;

    public VoucherCentre() {
    }

    public VoucherCentre(int voucherId, int centreId) {
        this.voucherId = voucherId;
        this.centreId = centreId;
    }

    public int getVoucherCentreId() {
        return voucherCentreId;
    }

    public void setVoucherCentreId(int voucherCentreId) {
        this.voucherCentreId = voucherCentreId;
    }

    public int getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(int voucherId) {
        this.voucherId = voucherId;
    }

    public int getCentreId() {
        return centreId;
    }

    public void setCentreId(int centreId) {
        this.centreId = centreId;
    }

    public String getCentreName() {
        return centreName;
    }

    public void setCentreName(String centreName) {
        this.centreName = centreName;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
