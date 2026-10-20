package model.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Đại diện cho một trung tâm chiếu phim trong hệ thống CineFlow.
 */
public class Centre {

    private int centreId;
    private String centreName;
    private String address;
    private String city;
    private String phone;
    private String status;
    private boolean distributed;
    private int futureShowtimeCount;
    private int hallCount;
    private int seatCount;
    private Integer managerId;
    private String managerName;
    private String managerEmail;
    private String managerStatus;
    private List<Hall> halls = new ArrayList<>();

    public String getManagerStatus() {
        return managerStatus;
    }

    public void setManagerStatus(String managerStatus) {
        this.managerStatus = managerStatus;
    }

    public boolean hasActiveManager() {
        return managerId != null && managerId > 0 && "ACTIVE".equalsIgnoreCase(managerStatus);
    }

    public boolean hasLockedManager() {
        return managerId != null && managerId > 0 && ("LOCKED".equalsIgnoreCase(managerStatus) || "INACTIVE".equalsIgnoreCase(managerStatus));
    }

    public boolean hasNoManager() {
        return managerId == null || managerId <= 0;
    }

    public int getFutureShowtimeCount() {
        return futureShowtimeCount;
    }

    public void setFutureShowtimeCount(int futureShowtimeCount) {
        this.futureShowtimeCount = futureShowtimeCount;
    }

    public Centre() {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isDistributed() {
        return distributed;
    }

    public void setDistributed(boolean distributed) {
        this.distributed = distributed;
    }

    public int getHallCount() {
        return hallCount;
    }

    public void setHallCount(int hallCount) {
        this.hallCount = hallCount;
    }

    public int getSeatCount() {
        return seatCount;
    }

    public void setSeatCount(int seatCount) {
        this.seatCount = seatCount;
    }

    public Integer getManagerId() {
        return managerId;
    }

    public void setManagerId(Integer managerId) {
        this.managerId = managerId;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public String getManagerEmail() {
        return managerEmail;
    }

    public void setManagerEmail(String managerEmail) {
        this.managerEmail = managerEmail;
    }

    public int getCentreID() {
        return centreId;
    }

    public void setCentreID(int centreId) {
        this.centreId = centreId;
    }

    public List<Hall> getHalls() {
        return halls;
    }

    public void setHalls(List<Hall> halls) {
        this.halls = halls;
    }
}
