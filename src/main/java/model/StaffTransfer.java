package model;

import java.sql.Timestamp;

public class StaffTransfer {
    private int transferId;
    private int userId;
    private String userName;
    private String userAccount;
    private String userEmail;
    private String userRole;
    private Integer fromBranchId;
    private String fromBranchName;
    private int toBranchId;
    private String toBranchName;
    private int transferredBy;
    private String transferredByName;
    private Timestamp transferDate;
    private String reason;
    private String notes;

    public StaffTransfer() {
    }

    public int getTransferId() {
        return transferId;
    }

    public void setTransferId(int transferId) {
        this.transferId = transferId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getEmployeeCode() {
        if ("MANAGER".equalsIgnoreCase(userRole)) {
            return String.format("QL%05d", userId);
        }
        return String.format("NV%05d", userId);
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserAccount() {
        return userAccount;
    }

    public void setUserAccount(String userAccount) {
        this.userAccount = userAccount;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public Integer getFromBranchId() {
        return fromBranchId;
    }

    public void setFromBranchId(Integer fromBranchId) {
        this.fromBranchId = fromBranchId;
    }

    public String getFromBranchName() {
        return fromBranchName;
    }

    public void setFromBranchName(String fromBranchName) {
        this.fromBranchName = fromBranchName;
    }

    public int getToBranchId() {
        return toBranchId;
    }

    public void setToBranchId(int toBranchId) {
        this.toBranchId = toBranchId;
    }

    public String getToBranchName() {
        return toBranchName;
    }

    public void setToBranchName(String toBranchName) {
        this.toBranchName = toBranchName;
    }

    public int getTransferredBy() {
        return transferredBy;
    }

    public void setTransferredBy(int transferredBy) {
        this.transferredBy = transferredBy;
    }

    public String getTransferredByName() {
        return transferredByName;
    }

    public void setTransferredByName(String transferredByName) {
        this.transferredByName = transferredByName;
    }

    public Timestamp getTransferDate() {
        return transferDate;
    }

    public void setTransferDate(Timestamp transferDate) {
        this.transferDate = transferDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
