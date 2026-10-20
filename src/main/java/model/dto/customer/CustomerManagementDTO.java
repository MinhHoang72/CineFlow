package model.dto.customer;

import model.entity.User;
import java.math.BigDecimal;

/**
 * DTO phục vụ màn hình Quản lý Khách hàng (Admin: /admin/customers).
 * Theo CineFlow Architecture Standard: User + bookingCount + totalSpent + rank.
 */
public class CustomerManagementDTO extends User {

    private int bookingCount;
    private BigDecimal totalSpent = BigDecimal.ZERO;
    private String rank = "STANDARD";

    public CustomerManagementDTO() {
        super();
    }

    public CustomerManagementDTO(User user, int bookingCount, BigDecimal totalSpent, String rank) {
        if (user != null) {
            setUserId(user.getUserId());
            setAccount(user.getAccount());
            setEmail(user.getEmail());
            setPhone(user.getPhone());
            setFullName(user.getFullName());
            setRole(user.getRole());
            setStatus(user.getStatus());
            setAvatar(user.getAvatar());
            setEmailVerified(user.isEmailVerified());
            setPhoneVerified(user.isPhoneVerified());
            setCreatedAt(user.getCreatedAt());
            setUpdatedAt(user.getUpdatedAt());
        }
        this.bookingCount = bookingCount;
        this.totalSpent = totalSpent != null ? totalSpent : BigDecimal.ZERO;
        this.rank = rank != null ? rank : "STANDARD";
    }

    @Override
    public int getBookingCount() {
        return bookingCount;
    }

    @Override
    public void setBookingCount(int bookingCount) {
        this.bookingCount = bookingCount;
        super.setBookingCount(bookingCount);
    }

    @Override
    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    @Override
    public void setTotalSpent(BigDecimal totalSpent) {
        this.totalSpent = totalSpent != null ? totalSpent : BigDecimal.ZERO;
        super.setTotalSpent(this.totalSpent);
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public String getCustomerRank() {
        return rank;
    }

    public void setCustomerRank(String rank) {
        this.rank = rank;
    }
}
