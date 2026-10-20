package model;

import java.sql.Timestamp;

/**
 * Đại diện cho một phòng chiếu (Hall) trực thuộc cụm rạp (Centre).
 * Đáp ứng Business Rule BR-HS-01: Mỗi phòng chiếu phải thuộc về duy nhất một cụm rạp (Centre).
 */
public class Hall {

    private int hallId;
    private int centreId;
    private String centreName;
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

    /**
     * Kiểm tra phòng chiếu có đang ở trạng thái hoạt động (Active) hay không.
     * Trong database, trạng thái hoạt động là 'AVAILABLE' (hoặc 'ACTIVE').
     */
    public boolean isActive() {
        return "AVAILABLE".equalsIgnoreCase(status) || "ACTIVE".equalsIgnoreCase(status);
    }

    /**
     * Trả về tên hiển thị chuẩn cho định dạng phòng chiếu (Standard 2D / 3D).
     */
    public String getFormattedType() {
        if ("3D".equalsIgnoreCase(hallType)) {
            return "3D";
        }
        return "Standard 2D";
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

    public String getCentreName() {
        return centreName;
    }

    public void setCentreName(String centreName) {
        this.centreName = centreName;
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
