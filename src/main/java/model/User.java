package model;

import java.sql.Timestamp;

/**
 * Đại diện cho tài khoản người dùng trong hệ thống CineFlow (Admin, Manager, Staff, Customer).
 */
public class User {

    private int userId;
    private String fullName;
    private String account;
    private String email;
    private String password;
    private String phone;
    private String avatar;
    private String role;
    private String status;
    private Integer centreId;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    private boolean emailVerified;
    private boolean phoneVerified;
    private Timestamp lastLoginAt;

    private String resetToken;
    private Timestamp resetTokenExpiry;

    public User() {
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_MANAGER = "MANAGER";
    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_STAFF = "STAFF";
    public static final String ROLE_TICKET_SALES = "TICKET_SALES_STAFF";
    public static final String ROLE_TICKET_CHECKER = "TICKET_CHECKER_STAFF";

    public String getEmployeeCode() {
        if ("MANAGER".equalsIgnoreCase(role)) {
            return String.format("QL%05d", userId);
        } else if ("TICKET_SALES_STAFF".equalsIgnoreCase(role) || "TICKET_SALES".equalsIgnoreCase(role)) {
            return String.format("BV%05d", userId);
        } else if ("TICKET_CHECKER_STAFF".equalsIgnoreCase(role) || "TICKET_CHECKER".equalsIgnoreCase(role)) {
            return String.format("SV%05d", userId);
        } else if ("STAFF".equalsIgnoreCase(role)) {
            return String.format("NV%05d", userId);
        } else if ("ADMIN".equalsIgnoreCase(role)) {
            return String.format("AD%05d", userId);
        }
        return String.format("EMP%05d", userId);
    }

    public boolean isStaff() {
        return "STAFF".equalsIgnoreCase(role)
                || "TICKET_SALES_STAFF".equalsIgnoreCase(role)
                || "TICKET_CHECKER_STAFF".equalsIgnoreCase(role)
                || "TICKET_SALES".equalsIgnoreCase(role)
                || "TICKET_CHECKER".equalsIgnoreCase(role);
    }

    public boolean isTicketSalesStaff() {
        return "TICKET_SALES_STAFF".equalsIgnoreCase(role) || "TICKET_SALES".equalsIgnoreCase(role);
    }

    public boolean isTicketCheckerStaff() {
        return "TICKET_CHECKER_STAFF".equalsIgnoreCase(role) || "TICKET_CHECKER".equalsIgnoreCase(role);
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    public boolean isManager() {
        return "MANAGER".equalsIgnoreCase(role);
    }

    public boolean isCustomer() {
        return role == null || "CUSTOMER".equalsIgnoreCase(role);
    }

    public boolean isInternalRole() {
        return isAdmin() || isManager() || isStaff();
    }

    public String getRoleDisplayName() {
        if (isTicketSalesStaff()) {
            return "Nhân viên bán vé";
        }
        if (isTicketCheckerStaff()) {
            return "Nhân viên soát vé";
        }
        if ("STAFF".equalsIgnoreCase(role)) {
            return "Nhân viên rạp";
        }
        if ("MANAGER".equalsIgnoreCase(role)) {
            return "Quản lý trung tâm";
        }
        if ("ADMIN".equalsIgnoreCase(role)) {
            return "Quản trị viên";
        }
        if ("CUSTOMER".equalsIgnoreCase(role)) {
            return "Khách hàng";
        }
        return role != null ? role : "";
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCentreId() { return centreId; }
    public void setCentreId(Integer centreId) { this.centreId = centreId; }
    public Integer getCentreID() { return centreId; }
    public void setCentreID(Integer centreId) { this.centreId = centreId; }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }


    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public boolean isPhoneVerified() {
        return phoneVerified;
    }

    public void setPhoneVerified(boolean phoneVerified) {
        this.phoneVerified = phoneVerified;
    }

    public Timestamp getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Timestamp lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public Timestamp getResetTokenExpiry() {
        return resetTokenExpiry;
    }

    public void setResetTokenExpiry(Timestamp resetTokenExpiry) {
        this.resetTokenExpiry = resetTokenExpiry;
    }

    private String centreName;

    public String getCentreName() {
        return centreName;
    }

    public void setCentreName(String centreName) {
        this.centreName = centreName;
    }

    private int bookingCount;
    private java.math.BigDecimal totalSpent;

    public int getBookingCount() {
        return bookingCount;
    }

    public void setBookingCount(int bookingCount) {
        this.bookingCount = bookingCount;
    }

    public java.math.BigDecimal getTotalSpent() {
        return totalSpent != null ? totalSpent : java.math.BigDecimal.ZERO;
    }

    public void setTotalSpent(java.math.BigDecimal totalSpent) {
        this.totalSpent = totalSpent;
    }

    public String getCustomerCode() {
        return String.format("KH%05d", userId);
    }
}
