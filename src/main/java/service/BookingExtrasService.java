package service;

import dao.BookingDAO;
import dao.BookingVoucherDAO;
import dao.FnbOrderDAO;
import model.dto.booking.FnbMenuItemDTO;
import model.dto.booking.VoucherOptionDTO;
import model.entity.Booking;
import model.entity.Showtime;
import model.entity.Voucher;
import utils.DbUtil;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Bap nuoc va voucher cho don dang giu ghe (dat ve online va ban tai quay).
 * Voucher chi giam tren tien ve (Booking.subTotal), khong giam bap nuoc.
 * Moi lan doi bap nuoc / voucher: tinh lai tong tien va huy giao dich dang cho (so tien cu).
 */
public class BookingExtrasService {

    private final FnbOrderDAO fnbOrderDAO;
    private final BookingVoucherDAO voucherDAO;
    private final BookingDAO bookingDAO;
    private final VoucherService voucherService;
    private final FnbOrderService fnbOrderService;
    private final BookingService bookingService;

    public BookingExtrasService() {
        this(new FnbOrderDAO(), new BookingVoucherDAO(), new BookingDAO(), new VoucherService(), new FnbOrderService(),
                new BookingService());
    }

    public BookingExtrasService(FnbOrderDAO fnbOrderDAO, BookingVoucherDAO voucherDAO, BookingDAO bookingDAO,
            VoucherService voucherService, FnbOrderService fnbOrderService, BookingService bookingService) {
        this.fnbOrderDAO = fnbOrderDAO;
        this.voucherDAO = voucherDAO;
        this.bookingDAO = bookingDAO;
        this.voucherService = voucherService;
        this.fnbOrderService = fnbOrderService;
        this.bookingService = bookingService;
    }

    /** Menu bap nuoc dang ban tai rap cua suat chieu. */
    public List<FnbMenuItemDTO> getMenu(Booking booking) throws SQLException {
        return fnbOrderDAO.getMenu(booking.getShowtime().getCentreId());
    }

    /**
     * Voucher khach chon duoc cho don nay (tru ma dang dung): ma dung duoc ngay truoc, giam nhieu truoc;
     * sau do la ma chi con thieu gia tri don toi thieu (kem so tien ve con thieu).
     */
    public List<VoucherOptionDTO> getVoucherOptions(Booking booking) throws SQLException {
        Showtime st = booking.getShowtime();
        BigDecimal ticketTotal = booking.getSubTotal();
        LocalDateTime now = LocalDateTime.now();
        List<VoucherOptionDTO> options = new ArrayList<>();
        for (Voucher v : voucherDAO.findAvailable(st.getCentreId(), st.getMovieId(),
                st.getStartTime().toLocalDateTime().toLocalDate(), booking.getUserId())) {
            if (v.getVoucherCode().equalsIgnoreCase(booking.getVoucherCode())) continue;
            BigDecimal minimum = v.getMinimumOrderValue() == null ? BigDecimal.ZERO : v.getMinimumOrderValue();
            BigDecimal missing = minimum.subtract(ticketTotal).max(BigDecimal.ZERO);
            BigDecimal discount = BigDecimal.ZERO;
            if (missing.signum() == 0) {
                VoucherService.VoucherApplicationResult result = voucherService.applyVoucher(v, ticketTotal, now);
                if (!result.isValid()) continue;
                discount = result.getDiscountAmount().min(ticketTotal);
            }
            boolean percent = "PERCENT".equalsIgnoreCase(v.getDiscountType())
                    || "PERCENTAGE".equalsIgnoreCase(v.getDiscountType());
            options.add(new VoucherOptionDTO(v.getVoucherCode(), v.getVoucherName(), percent, v.getDiscountValue(),
                    v.getMaximumDiscount(), minimum, v.getEndDate(), discount, missing));
        }
        options.sort(Comparator.comparing(VoucherOptionDTO::isUsable).reversed()
                .thenComparing(VoucherOptionDTO::getDiscount, Comparator.reverseOrder())
                .thenComparing(VoucherOptionDTO::getMissingAmount));
        return options;
    }

    /**
     * Doc so luong tu form (qty_I12, qty_C3...), kiem tra mon / so luong / ton kho, thay gio bap nuoc cua don
     * (gia theo menu hien tai) va tinh lai tong tien.
     * @throws IllegalArgumentException du lieu khong hop le / het hang, message hien cho nguoi dung
     */
    public void saveFnb(Booking booking, Map<String, String[]> params) throws SQLException {
        Map<String, Integer> cart = FnbOrderService.readCart(params);
        int centreId = booking.getShowtime().getCentreId();
        String orderType = booking.isCounter() ? "COUNTER" : "ONLINE";
        DbUtil.inTransaction(conn -> {
            Map<String, FnbMenuItemDTO> menu = fnbOrderService.loadMenu(conn, centreId);
            Map<String, Integer> lines = fnbOrderService.validLines(cart, menu);
            fnbOrderService.checkStock(conn, centreId, lines, menu);

            fnbOrderDAO.deletePendingForBooking(conn, booking.getId());
            if (!lines.isEmpty()) {
                fnbOrderService.createOrder(conn, booking.getId(), centreId, booking.getUserId(), orderType, lines, menu);
            }
            bookingService.recalculateTotal(conn, booking.getId());
            bookingService.cancelPendingPayments(conn, booking.getId());
            return null;
        });
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

        Showtime st = booking.getShowtime();
        String scopeError = checkScope(voucher, st.getCentreId(), st.getMovieId(),
                st.getStartTime().toLocalDateTime().toLocalDate());
        if (scopeError != null) throw new IllegalArgumentException(scopeError);

        // Gioi han luot dung moi khach (don tai quay khach vang lai khong co tai khoan: bo qua)
        if (booking.getUserId() != null
                && voucherDAO.countUserUsages(voucher.getVoucherId(), booking.getUserId())
                   >= voucherDAO.perCustomerLimit(voucher.getVoucherId())) {
            throw new IllegalArgumentException("Bạn đã dùng hết lượt của mã giảm giá này.");
        }

        BigDecimal discount = result.getDiscountAmount().min(booking.getSubTotal());
        setVoucher(booking.getId(), voucher.getVoucherId(), discount);
        return discount;
    }

    /** Bo voucher khoi don. */
    public void removeVoucher(Booking booking) throws SQLException {
        setVoucher(booking.getId(), null, BigDecimal.ZERO);
    }

    /**
     * Voucher co ap dung cho rap / phim / ngay chieu nay khong (khong khai bao rap / phim / ngay = ap dung tat ca).
     * @return null neu hop le, nguoc lai la ly do khong ap dung duoc
     */
    private String checkScope(Voucher voucher, int centreId, int movieId, LocalDate showDate) throws SQLException {
        int id = voucher.getVoucherId();
        if (voucher.getCentreId() != null && voucher.getCentreId() != centreId) {
            return "Mã giảm giá không áp dụng tại rạp này.";
        }
        List<Integer> centres = voucherDAO.getScopeCentreIds(id);
        if (!centres.isEmpty() && !centres.contains(centreId)) return "Mã giảm giá không áp dụng tại rạp này.";
        List<Integer> movies = voucherDAO.getScopeMovieIds(id);
        if (!movies.isEmpty() && !movies.contains(movieId)) return "Mã giảm giá không áp dụng cho phim này.";
        List<String> days = voucherDAO.getScopeDays(id);
        if (!days.isEmpty() && !days.contains(showDate.getDayOfWeek().name())) {
            return "Mã giảm giá không áp dụng cho ngày chiếu này.";
        }
        return null;
    }

    /** Gan / bo voucher cua don dang giu ghe, tinh lai tong tien, huy giao dich dang cho. */
    private void setVoucher(int bookingId, Integer voucherId, BigDecimal discount) throws SQLException {
        DbUtil.inTransaction(conn -> {
            bookingDAO.updateVoucher(conn, bookingId, voucherId, voucherId == null ? BigDecimal.ZERO : discount);
            bookingService.recalculateTotal(conn, bookingId);
            bookingService.cancelPendingPayments(conn, bookingId);
            return null;
        });
    }
}
