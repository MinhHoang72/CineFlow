package model;

import java.sql.Timestamp;

/**
 * Đại diện cho một phòng chiếu (Hall) trực thuộc cụm rạp (Centre).
 * Đáp ứng Business Rule BR-HS-01: Một Centre quản lý danh sách các Hall trực thuộc.
 */
public class Hall {

    private int hallId;
    private int centreId;
    private String hallName;
    private String hallType; // '2D', '3D'
    private int capacity;
    private String status;   // 'AVAILABLE', 'MAINTENANCE', 'INACTIVE'
    private Timestamp createdAt;

    public Hall() {
    }

    public Hall(int hallId, int centreId, String hallName, String hallType, int capacity, String status) {
        this.hallId = hallId;
        this.centreId = centreId;
        this.hallName = hallName;
        this.hallType = hallType;
        this.capacity = capacity;
        this.status = status;
    }

    public int getHallId() {
        return hallId;
    }

    public void setHallId(int hallId) {
        this.hallId = hallId;
    }

    public int getCentreId() {
        return centreId;
    }

    public void setCentreId(int centreId) {
        this.centreId = centreId;
    }

    public String getHallName() {
        return hallName;
    }

    public void setHallName(String hallName) {
        this.hallName = hallName;
    }

    public String getHallType() {
        return hallType;
    }

    public void setHallType(String hallType) {
        this.hallType = hallType;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
