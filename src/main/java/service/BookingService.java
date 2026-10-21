package service;

import dao.BookingDAO;
import dao.BookingDAO.SeatUnavailableException;
import dao.SeatDAO;
import model.entity.Booking;

import java.sql.SQLException;
import java.util.List;

/**
 * Service: Xử lý logic nghiệp vụ Đặt vé & Giữ ghế (Booking & Seat Reservation).
 * Nhiệm vụ:
 * - Kiểm tra giới hạn số lượng ghế đặt mỗi đơn (tối đa 8 ghế).
 * - Chống race condition khi giữ ghế (Seat Reservation), khóa ghế tạm thời (5 phút).
 * - Hủy đơn giữ chỗ khi hết hạn hoặc khách hàng yêu cầu hủy.
 * - Truy xuất thông tin đơn đặt vé và lịch sử mua vé.
 */
public class BookingService {

    public static final int MAX_SEATS_PER_BOOKING = BookingDAO.MAX_SEATS_PER_BOOKING;
    public static final int HOLD_MINUTES = BookingDAO.HOLD_MINUTES;

    private final BookingDAO bookingDAO;
    private final SeatDAO seatDAO;

    public BookingService() {
        this.bookingDAO = new BookingDAO();
        this.seatDAO = new SeatDAO();
    }

    public BookingService(BookingDAO bookingDAO, SeatDAO seatDAO) {
        this.bookingDAO = bookingDAO;
        this.seatDAO = seatDAO;
    }

    /**
     * Giữ ghế trực tuyến cho khách hàng đặt vé trên website.
     *
     * @param userId     ID khách hàng
     * @param showtimeId ID suất chiếu
     * @param seatIds    Danh sách ID ghế cần giữ
     * @return ID đơn đặt vé tạm thời (BookingID)
     * @throws SeatUnavailableException nếu một hoặc nhiều ghế đã bị người khác đặt/giữ
     * @throws IllegalArgumentException nếu danh sách ghế rỗng hoặc vượt quá giới hạn
     * @throws SQLException nếu xảy ra lỗi cơ sở dữ liệu
     */
    public int holdSeatsForOnlineCustomer(int userId, int showtimeId, List<Integer> seatIds)
            throws SeatUnavailableException, SQLException {
        validateSeatSelection(seatIds);
        return bookingDAO.holdSeats(userId, showtimeId, seatIds);
    }

    /**
     * Giữ ghế trực tiếp tại quầy bán vé (POS Counter) cho nhân viên.
     *
     * @param showtimeId ID suất chiếu
     * @param seatIds    Danh sách ID ghế cần giữ
     * @return ID đơn đặt vé tạm thời (BookingID)
     * @throws SeatUnavailableException nếu ghế không khả dụng
     * @throws SQLException nếu xảy ra lỗi cơ sở dữ liệu
     */
    public int holdSeatsForCounter(int showtimeId, List<Integer> seatIds)
            throws SeatUnavailableException, SQLException {
        validateSeatSelection(seatIds);
        return bookingDAO.holdCounterSeats(showtimeId, seatIds);
    }

    /**
     * Hủy đơn đặt vé đang chờ thanh toán của khách hàng trực tuyến.
     */
    public boolean cancelPendingBooking(int bookingId, int userId) throws SQLException {
        return bookingDAO.cancelPending(bookingId, userId);
    }

    /**
     * Hủy đơn đặt vé tại quầy đang chờ thanh toán.
     */
    public boolean cancelCounterPendingBooking(int bookingId) throws SQLException {
        return bookingDAO.cancelCounterPending(bookingId);
    }

    /**
     * Lấy thông tin chi tiết đơn đặt vé.
     */
    public Booking getBooking(int bookingId) throws SQLException {
        return bookingDAO.getBooking(bookingId);
    }


    /**
     * Kiểm tra tính hợp lệ của danh sách ghế lựa chọn.
     */
    private void validateSeatSelection(List<Integer> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một ghế.");
        }
        if (seatIds.size() > MAX_SEATS_PER_BOOKING) {
            throw new IllegalArgumentException("Mỗi đơn đặt vé chỉ được chọn tối đa " + MAX_SEATS_PER_BOOKING + " ghế.");
        }
    }
}
