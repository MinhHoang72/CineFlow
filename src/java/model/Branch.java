package model;

public class Branch {

    private int branchId;
    private String branchName;
    private String address;
    private String city;
    private String phone;
    private String status;
    private boolean distributed;

    public int getBranchId() {
        return branchId;
    }

    public void setBranchId(int branchId) {
        this.branchId = branchId;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isDistributed() { return distributed; }
    public void setDistributed(boolean distributed) { this.distributed = distributed; }
}
