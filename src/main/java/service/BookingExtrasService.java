package service;

import dao.BookingVoucherDAO;
import dao.FnbOrderDAO;
import model.dto.booking.FnbMenuItemDTO;
import model.entity.Booking;
import model.entity.Voucher;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bap nuoc va voucher cho don dang giu ghe (dat ve online va ban tai quay).
 * Voucher chi giam tren tien ve (Booking.subTotal), khong giam bap nuoc.
 */
public class BookingExtrasService {

    private final FnbOrderDAO fnbOrderDAO;
    private final BookingVoucherDAO voucherDAO;
    private final VoucherService voucherService;

    public BookingExtrasService() {
        this(new FnbOrderDAO(), new BookingVoucherDAO(), new VoucherService());
    }

    public BookingExtrasService(FnbOrderDAO fnbOrderDAO, BookingVoucherDAO voucherDAO, VoucherService voucherService) {
        this.fnbOrderDAO = fnbOrderDAO;
        this.voucherDAO = voucherDAO;
        this.voucherService = voucherService;
    }

    /** Menu bap nuoc dang ban tai rap cua suat chieu. */
    public List<FnbMenuItemDTO> getMenu(Booking booking) throws SQLException {
        return fnbOrderDAO.getMenu(booking.getShowtime().getCentreId());
    }

    /**
     * Doc so luong tu form (tham so qty_I12, qty_C3...) va luu gio bap nuoc.
     * @throws IllegalArgumentException du lieu khong hop le / het hang, message hien cho nguoi dung
     */
    public void saveFnb(Booking booking, Map<String, String[]> params) throws SQLException {
        Map<String, Integer> cart = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> e : params.entrySet()) {
            if (!e.getKey().matches("qty_[IC]\\d{1,9}")) continue;
            String raw = e.getValue().length == 0 ? "" : e.getValue()[0].trim();
            int qty;
            try {
                qty = raw.isEmpty() ? 0 : Integer.parseInt(raw);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Số lượng bắp nước không hợp lệ.");
            }
            cart.put(e.getKey().substring(4), qty);
        }
        String orderType = booking.isCounter() ? "COUNTER" : "ONLINE";
        fnbOrderDAO.replaceCart(booking.getId(), booking.getShowtime().getCentreId(), booking.getUserId(), orderType, cart);
    }

    /**
     * Kiem tra va ap voucher cho don.
     * @return so tien duoc giam
     * @throws IllegalArgumentException voucher khong ap dung duoc, message hien cho nguoi dung
     */
    public BigDecimal applyVoucher(Booking booking, String code) throws SQLException {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("Vui lòng nhập mã giảm giá.");
        Voucher voucher = voucherDAO.findByCode(code);
        if (voucher == null) throw new IllegalArgumentException("Mã giảm giá không tồn tại.");

        // Tra trang thai, han dung, luot dung, gia tri toi thieu va tinh so tien giam tren tien ve
        VoucherService.VoucherApplicationResult result =
                voucherService.applyVoucher(voucher, booking.getSubTotal(), LocalDateTime.now());
        if (!result.isValid()) throw new IllegalArgumentException(result.getMessage());

        String scopeError = voucherDAO.checkScope(voucher, booking.getShowtime().getCentreId(),
                booking.getShowtime().getMovieId(), booking.getShowtime().getStartTime().toLocalDateTime().toLocalDate());
        if (scopeError != null) throw new IllegalArgumentException(scopeError);

        // Gioi han luot dung moi khach (don tai quay khach vang lai khong co tai khoan: bo qua)
        if (booking.getUserId() != null
                && voucherDAO.countUserUsages(voucher.getVoucherId(), booking.getUserId())
                   >= voucherDAO.perCustomerLimit(voucher.getVoucherId())) {
            throw new IllegalArgumentException("Bạn đã dùng hết lượt của mã giảm giá này.");
        }

        BigDecimal discount = result.getDiscountAmount().min(booking.getSubTotal());
        voucherDAO.setBookingVoucher(booking.getId(), voucher.getVoucherId(), discount);
        return discount;
    }

    /** Bo voucher khoi don. */
    public void removeVoucher(Booking booking) throws SQLException {
        voucherDAO.setBookingVoucher(booking.getId(), null, BigDecimal.ZERO);
    }
}
