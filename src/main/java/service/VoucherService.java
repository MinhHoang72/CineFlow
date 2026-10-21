package service;

import dao.VoucherDAO;
import model.dto.common.PageDTO;
import model.entity.Voucher;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Service: Xử lý logic nghiệp vụ Mã giảm giá & Khuyến mãi (Voucher & Promotions).
 * Nhiệm vụ:
 * - Kiểm tra điều kiện hiệu lực của voucher (ngày áp dụng, số lượt dùng tối đa, trạng thái).
 * - Kiểm tra giá trị đơn hàng tối thiểu (Minimum Order Value).
 * - Tính toán chính xác số tiền giảm giá theo tỷ lệ phần trăm (%) hoặc số tiền cố định (VNĐ), áp trần tối đa.
 * - Quản lý CRUD và cập nhật trạng thái voucher.
 */
public class VoucherService {

    private final VoucherDAO voucherDAO;

    public VoucherService() {
        this.voucherDAO = new VoucherDAO();
    }

    public VoucherService(VoucherDAO voucherDAO) {
        this.voucherDAO = voucherDAO;
    }

    /**
     * Kết quả kiểm tra và tính toán giảm giá của Voucher.
     */
    public static class VoucherApplicationResult {
        private final boolean valid;
        private final String message;
        private final BigDecimal discountAmount;
        private final Voucher voucher;

        public VoucherApplicationResult(boolean valid, String message, BigDecimal discountAmount, Voucher voucher) {
            this.valid = valid;
            this.message = message;
            this.discountAmount = discountAmount;
            this.voucher = voucher;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        public BigDecimal getDiscountAmount() {
            return discountAmount;
        }

        public Voucher getVoucher() {
            return voucher;
        }
    }

    /**
     * Kiểm tra điều kiện và tính toán số tiền giảm giá cho một đơn hàng.
     *
     * @param voucher     Đối tượng voucher cần áp dụng
     * @param orderAmount Tổng giá trị đơn hàng trước giảm giá
     * @param checkTime   Thời điểm áp dụng voucher
     * @return VoucherApplicationResult chứa kết quả và số tiền được giảm
     */
    public VoucherApplicationResult applyVoucher(Voucher voucher, BigDecimal orderAmount, LocalDateTime checkTime) {
        if (voucher == null) {
            return new VoucherApplicationResult(false, "Mã giảm giá không tồn tại.", BigDecimal.ZERO, null);
        }

        if (!"ACTIVE".equalsIgnoreCase(voucher.getStatus())) {
            return new VoucherApplicationResult(false, "Mã giảm giá hiện không hoạt động.", BigDecimal.ZERO, voucher);
        }

        if (voucher.getStartDate() != null && checkTime.isBefore(voucher.getStartDate())) {
            return new VoucherApplicationResult(false, "Mã giảm giá chưa đến thời gian áp dụng.", BigDecimal.ZERO, voucher);
        }

        if (voucher.getEndDate() != null && checkTime.isAfter(voucher.getEndDate())) {
            return new VoucherApplicationResult(false, "Mã giảm giá đã hết hạn sử dụng.", BigDecimal.ZERO, voucher);
        }

        if (voucher.getUsageLimit() != null && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            return new VoucherApplicationResult(false, "Mã giảm giá đã hết số lượt sử dụng.", BigDecimal.ZERO, voucher);
        }

        if (voucher.getMinimumOrderValue() != null && orderAmount.compareTo(voucher.getMinimumOrderValue()) < 0) {
            return new VoucherApplicationResult(false,
                    "Đơn hàng chưa đạt giá trị tối thiểu " + voucher.getMinimumOrderValue() + "đ để áp dụng voucher này.",
                    BigDecimal.ZERO, voucher);
        }

        BigDecimal discount = BigDecimal.ZERO;
        // DB luu PERCENT (CK_Vouchers_Type), giu PERCENTAGE cho tuong thich
        if ("PERCENT".equalsIgnoreCase(voucher.getDiscountType()) || "PERCENTAGE".equalsIgnoreCase(voucher.getDiscountType())) {
            discount = orderAmount.multiply(voucher.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            if (voucher.getMaximumDiscount() != null && discount.compareTo(voucher.getMaximumDiscount()) > 0) {
                discount = voucher.getMaximumDiscount();
            }
        } else {
            // FIXED_AMOUNT
            discount = voucher.getDiscountValue();
            if (discount.compareTo(orderAmount) > 0) {
                discount = orderAmount;
            }
        }

        return new VoucherApplicationResult(true, "Áp dụng mã giảm giá thành công.", discount, voucher);
    }

    /**
     * Tìm kiếm và phân trang danh sách Voucher.
     */
    public PageDTO<Voucher> searchVouchers(String key, String status, String type, int page, int size) {
        return voucherDAO.search(key, status, type, null, page, size); // null = moi chi nhanh (Admin)
    }

    /**
     * Lấy thông tin Voucher theo ID.
     */
    public Voucher getVoucherById(int id) {
        return voucherDAO.find(id);
    }

    /**
     * Kiểm tra mã code voucher đã tồn tại hay chưa.
     */
    public boolean isVoucherCodeExists(String code, int excludeId) {
        return voucherDAO.codeExists(code, excludeId);
    }

    /**
     * Thêm mới Voucher.
     */
    public void createVoucher(Voucher voucher) throws SQLException {
        if (voucherDAO.codeExists(voucher.getVoucherCode(), 0)) {
            throw new IllegalArgumentException("Mã voucher '" + voucher.getVoucherCode() + "' đã tồn tại.");
        }
        voucherDAO.insert(voucher);
    }

    /**
     * Cập nhật Voucher.
     */
    public void updateVoucher(Voucher voucher) throws SQLException {
        if (voucherDAO.codeExists(voucher.getVoucherCode(), voucher.getVoucherId())) {
            throw new IllegalArgumentException("Mã voucher '" + voucher.getVoucherCode() + "' đã tồn tại ở bản ghi khác.");
        }
        voucherDAO.update(voucher);
    }

    /**
     * Đổi trạng thái Voucher (ACTIVE / INACTIVE / EXPIRED).
     */
    public void changeVoucherStatus(int id, String status) throws SQLException {
        voucherDAO.setStatus(id, status);
    }
}
