package controller.customer;

import dao.UserDAO;
import service.BookingExtrasService;
import service.BookingService;
import service.BookingService.SeatLayout;
import service.BookingService.SeatUnavailableException;
import service.BookingService.ShowtimeSelection;
import service.PaymentService;
import model.dto.booking.FnbMenuItemDTO;
import model.entity.Booking;
import model.entity.Showtime;
import model.entity.User;
import utils.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * FE-05 Dat ve (controller: doc request, goi service, chuyen trang; nghiep vu nam o BookingService):
 *   GET  /booking                          chon thanh pho -> rap -> phim -> ngay -> suat chieu
 *   GET  /booking?action=select-seats      chon ghe (can dang nhap)
 *   POST /booking?action=hold              giu ghe 5 phut, tao booking PENDING
 *   GET  /booking?action=fnb               chon bap nuoc (bo qua neu rap chua ban mon nao)
 *   POST /booking?action=save-fnb          luu bap nuoc (qty_I{id}, qty_C{id}) roi sang trang thanh toan
 *   GET  /booking?action=checkout          xem lai don + voucher + dem nguoc + nut thanh toan
 *   POST /booking?action=apply-voucher     ap ma giam gia (chi giam tien ve)
 *   POST /booking?action=remove-voucher    bo ma giam gia
 *   POST /booking?action=cancel            huy booking dang cho thanh toan
 *   GET  /booking?action=history           lich su dat ve cua khach (trang rieng, tach khoi /account)
 *   GET  /booking?action=detail            chi tiet don (khach, nhan vien, quan ly, admin)
 */
@WebServlet(name = "BookingServlet", urlPatterns = {"/booking", "/booking/booking.jsp"})
public class BookingServlet extends HttpServlet {

    private static final String VIEW_DIR = "/WEB-INF/views/customer/";
    private static final String[] WEEKDAYS = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};

    private final BookingService bookingService = new BookingService();
    private final BookingExtrasService extrasService = new BookingExtrasService();
    private final PaymentService paymentService = new PaymentService();

    /**
     * Nhận mọi request GET tới /booking (xem trang, không đổi dữ liệu).
     * Luồng:
     * 1. Tài khoản nội bộ (ADMIN, MANAGER, nhân viên) bị chuyển về trang của họ; riêng action=detail vẫn cho xem.
     * 2. Rẽ nhánh theo tham số action:
     *    - rỗng / select-showtime -> selectShowtime (bước 1: chọn thành phố, rạp, phim, ngày, suất)
     *    - select-seats -> selectSeats (bước 2: sơ đồ ghế)
     *    - fnb -> selectFnb (bước 3: bắp nước)
     *    - checkout -> checkout (bước 4: xem lại đơn, voucher, nút thanh toán)
     *    - history -> history (lịch sử đặt vé)
     *    - detail -> detail (chi tiết đơn và vé)
     * 3. Lỗi CSDL được bọc thành ServletException để Tomcat hiện trang lỗi.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        // Silent Redirect: Tai khoan noi bo (ADMIN, MANAGER, STAFF) khong vao luong dat ve online (ngoai tru xem chi tiet don 'detail')
        jakarta.servlet.http.HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            String role = (String) session.getAttribute("role");
            if (role == null && session.getAttribute("user") instanceof User) {
                role = ((User) session.getAttribute("user")).getRole();
            }
            if (role != null && !"CUSTOMER".equalsIgnoreCase(role) && !"detail".equals(action)) {
                resp.sendRedirect(req.getContextPath() + WebUtil.getRoleDefaultPath(session));
                return;
            }
        }
        try {
            if (action == null || action.isEmpty() || "select-showtime".equals(action)) {
                selectShowtime(req, resp);
            } else if ("select-seats".equals(action)) {
                selectSeats(req, resp);
            } else if ("fnb".equals(action)) {
                selectFnb(req, resp);
            } else if ("checkout".equals(action)) {
                checkout(req, resp);
            } else if ("history".equals(action)) {
                history(req, resp);
            } else if ("detail".equals(action)) {
                detail(req, resp);
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    /**
     * Nhận mọi request POST tới /booking (thao tác làm thay đổi dữ liệu).
     * Rẽ nhánh theo action:
     * - hold -> hold: giữ ghế, tạo đơn PENDING
     * - cancel -> cancel: khách huỷ đơn chưa thanh toán
     * - save-fnb / apply-voucher / remove-voucher -> updateExtras: lưu bắp nước, áp hoặc bỏ voucher
     * Tài khoản nội bộ không được thao tác đặt vé online.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        // Silent Redirect: Tai khoan noi bo khong thuc hien cac thao tac dat ve online
        jakarta.servlet.http.HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            String role = (String) session.getAttribute("role");
            if (role == null && session.getAttribute("user") instanceof User) {
                role = ((User) session.getAttribute("user")).getRole();
            }
            if (role != null && !"CUSTOMER".equalsIgnoreCase(role)) {
                resp.sendRedirect(req.getContextPath() + WebUtil.getRoleDefaultPath(session));
                return;
            }
        }
        try {
            if ("hold".equals(action)) {
                hold(req, resp);
            } else if ("cancel".equals(action)) {
                cancel(req, resp);
            } else if ("save-fnb".equals(action) || "apply-voucher".equals(action) || "remove-voucher".equals(action)) {
                updateExtras(req, resp, action);
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    /**
     * Xử lý form bắp nước và voucher của khách (POST save-fnb / apply-voucher / remove-voucher).
     * Luồng: bắt đăng nhập -> lấy đúng đơn của khách -> đơn còn hạn giữ ghế thì gọi applyExtras.
     * Sau đó chuyển trang: lưu bắp nước lỗi thì quay lại trang bắp nước, còn lại sang trang xem lại đơn (checkout).
     */
    private void updateExtras(HttpServletRequest req, HttpServletResponse resp, String action)
            throws SQLException, IOException {
        User user = WebUtil.requireUser(req, resp);
        if (user == null) return;
        Booking booking = loadOwnBooking(req, user);
        if (booking == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        boolean ok = false;
        if (booking.isHoldActive()) {
            ok = applyExtras(req, booking, action, extrasService);
        } else {
            WebUtil.flash(req, "warning", "Đơn đã hết thời gian giữ ghế.");
        }
        String next = "save-fnb".equals(action) && !ok ? "fnb" : "checkout";
        resp.sendRedirect(req.getContextPath() + "/booking?action=" + next + "&bookingId=" + booking.getId());
    }

    /**
     * Gọi service theo thao tác và đặt thông báo cho người dùng. Dùng chung cho đặt vé online và bán vé tại quầy.
     * - save-fnb -> BookingExtrasService.saveFnb (lưu giỏ bắp nước, tính lại tổng)
     * - apply-voucher -> BookingExtrasService.applyVoucher (kiểm tra và áp mã, báo số tiền được giảm)
     * - còn lại (remove-voucher) -> BookingExtrasService.removeVoucher
     * @return false nếu dữ liệu không hợp lệ (service ném IllegalArgumentException, đã hiện thông báo lỗi)
     */
    public static boolean applyExtras(HttpServletRequest req, Booking booking, String action, BookingExtrasService service)
            throws SQLException {
        try {
            switch (action) {
                case "save-fnb" -> service.saveFnb(booking, req.getParameterMap());
                case "apply-voucher" -> {
                    BigDecimal discount = service.applyVoucher(booking, req.getParameter("voucherCode"));
                    WebUtil.flash(req, "success", "Đã áp dụng mã giảm giá, giảm "
                            + String.format("%,d", discount.longValue()).replace(',', '.') + " đ trên tiền vé.");
                }
                default -> {
                    service.removeVoucher(booking);
                    WebUtil.flash(req, "info", "Đã bỏ mã giảm giá.");
                }
            }
            return true;
        } catch (IllegalArgumentException e) {
            WebUtil.flash(req, "warning", e.getMessage());
            return false;
        }
    }

    /**
     * Bước 1 - trang chọn suất (GET /booking).
     * Đọc 4 tham số city, centreId, movieId, date trên URL (chưa chọn thì null), gọi BookingService.selectShowtime
     * để lọc lần lượt thành phố -> rạp -> phim -> ngày -> suất, rồi đặt các danh sách vào request và mở selectShowtime.jsp.
     * Mỗi lần khách bấm một lựa chọn, trang tự gọi lại chính nó với thêm một tham số.
     */
    private void selectShowtime(HttpServletRequest req, HttpServletResponse resp)
            throws SQLException, ServletException, IOException {
        ShowtimeSelection sel = bookingService.selectShowtime(req.getParameter("city"),
                WebUtil.parseInt(req.getParameter("centreId")), WebUtil.parseInt(req.getParameter("movieId")),
                parseDate(req.getParameter("date")));
        if (sel.movieUnavailable()) WebUtil.flash(req, "info", "Phim này hiện chưa có suất chiếu mở bán.");

        req.setAttribute("cities", sel.cities());
        req.setAttribute("selectedCity", sel.city());
        req.setAttribute("centres", sel.centres());
        req.setAttribute("selectedCentreId", sel.centreId());
        req.setAttribute("selectedMovieId", sel.movieId());
        if (sel.movies() != null) req.setAttribute("movies", sel.movies());
        if (sel.dates() != null) req.setAttribute("dateOptions", toDateOptions(sel.dates()));
        if (sel.date() != null) {
            req.setAttribute("selectedDate", sel.date().toString());
            req.setAttribute("showtimes", sel.showtimes());
        }
        req.getRequestDispatcher(VIEW_DIR + "booking/selectShowtime.jsp").forward(req, resp);
    }

    /**
     * Bước 2 - trang sơ đồ ghế (GET /booking?action=select-seats&showtimeId=...).
     * Bắt đăng nhập; suất không còn bán thì báo lỗi và về trang chọn suất.
     * Lấy ghế kèm giá (BookingService.getSeatLayout), số ghế tối đa, thời gian giữ ghế rồi mở selectSeats.jsp.
     */
    private void selectSeats(HttpServletRequest req, HttpServletResponse resp)
            throws SQLException, ServletException, IOException {
        if (WebUtil.requireUser(req, resp) == null) return;

        Integer showtimeId = WebUtil.parseInt(req.getParameter("showtimeId"));
        Showtime showtime = showtimeId == null ? null : bookingService.getBookableShowtime(showtimeId);
        if (showtime == null) {
            WebUtil.flash(req, "warning", "Suất chiếu không tồn tại hoặc đã ngừng bán vé.");
            resp.sendRedirect(req.getContextPath() + "/booking");
            return;
        }
        SeatLayout layout = bookingService.getSeatLayout(showtime);
        req.setAttribute("showtime", showtime);
        req.setAttribute("seatRows", layout.rows());
        req.setAttribute("priceByType", layout.priceByType());
        req.setAttribute("maxSeats", BookingService.MAX_SEATS_PER_BOOKING);
        req.setAttribute("holdMinutes", BookingService.HOLD_MINUTES);
        req.getRequestDispatcher(VIEW_DIR + "booking/selectSeats.jsp").forward(req, resp);
    }

    /**
     * Giữ ghế khi khách bấm "Tiếp tục" ở trang sơ đồ ghế (POST action=hold).
     * Gọi BookingService.holdSeatsForOnlineCustomer với các seatId đã chọn.
     * - Thành công: tạo đơn PENDING, ghế HELD 5 phút -> chuyển sang trang bắp nước.
     * - Lỗi (ghế vừa có người giữ, quá 8 ghế, Sweetbox đặt lẻ...): hiện lý do và quay lại sơ đồ ghế.
     */
    private void hold(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        User user = WebUtil.requireUser(req, resp);
        if (user == null) return;

        Integer showtimeId = WebUtil.parseInt(req.getParameter("showtimeId"));
        if (showtimeId == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            int bookingId = bookingService.holdSeatsForOnlineCustomer(user.getUserId(), showtimeId, readSeatIds(req));
            resp.sendRedirect(req.getContextPath() + "/booking?action=fnb&bookingId=" + bookingId);
        } catch (SeatUnavailableException e) {
            WebUtil.flash(req, "warning", e.getMessage());
            resp.sendRedirect(req.getContextPath() + "/booking?action=select-seats&showtimeId=" + showtimeId);
        }
    }

    /**
     * Bước 4 - trang xem lại đơn (GET action=checkout): bảng tiền, voucher, nút thanh toán VNPay, nút huỷ.
     * Đơn đã thanh toán thì chuyển sang trang chi tiết đơn.
     * Đơn còn hạn giữ ghế thì nạp thêm danh sách voucher khách chọn được (BookingExtrasService.getVoucherOptions).
     */
    private void checkout(HttpServletRequest req, HttpServletResponse resp)
            throws SQLException, ServletException, IOException {
        User user = WebUtil.requireUser(req, resp);
        if (user == null) return;

        Booking booking = loadOwnBooking(req, user);
        if (booking == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (booking.isPaid()) {
            resp.sendRedirect(req.getContextPath() + "/booking?action=detail&bookingId=" + booking.getId());
            return;
        }
        req.setAttribute("booking", booking);
        if (booking.isHoldActive()) {
            req.setAttribute("extrasAction", req.getContextPath() + "/booking");
            req.setAttribute("fnbUrl", req.getContextPath() + "/booking?action=fnb&bookingId=" + booking.getId());
            req.setAttribute("voucherOptions", extrasService.getVoucherOptions(booking));
        }
        req.getRequestDispatcher(VIEW_DIR + "booking/bookingCheckout.jsp").forward(req, resp);
    }

    /**
     * Bước 3 - trang chọn bắp nước (GET action=fnb).
     * Đơn hết hạn giữ ghế hoặc rạp không bán món nào thì bỏ qua, chuyển thẳng sang trang xem lại đơn.
     * Còn lại: lấy menu của rạp (BookingExtrasService.getMenu) và mở selectFnb.jsp; form gửi về /booking (extrasAction).
     */
    private void selectFnb(HttpServletRequest req, HttpServletResponse resp)
            throws SQLException, ServletException, IOException {
        User user = WebUtil.requireUser(req, resp);
        if (user == null) return;

        Booking booking = loadOwnBooking(req, user);
        if (booking == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String checkoutUrl = req.getContextPath() + "/booking?action=checkout&bookingId=" + booking.getId();
        if (!booking.isHoldActive()) {
            resp.sendRedirect(checkoutUrl);
            return;
        }
        List<FnbMenuItemDTO> menu = extrasService.getMenu(booking);
        if (menu.isEmpty()) {
            resp.sendRedirect(checkoutUrl);
            return;
        }
        req.setAttribute("booking", booking);
        req.setAttribute("fnbMenu", menu);
        req.setAttribute("extrasAction", req.getContextPath() + "/booking");
        req.getRequestDispatcher(VIEW_DIR + "booking/selectFnb.jsp").forward(req, resp);
    }

    /**
     * Khách tự huỷ đơn chưa thanh toán (POST action=cancel).
     * Gọi BookingService.cancelPendingBooking: đơn và ghế -> CANCELLED, ghế trống lại cho người khác.
     * Sau đó quay về sơ đồ ghế của suất đó.
     */
    private void cancel(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        User user = WebUtil.requireUser(req, resp);
        if (user == null) return;

        Booking booking = loadOwnBooking(req, user);
        if (booking == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (bookingService.cancelPendingBooking(booking.getId(), user.getUserId())) {
            WebUtil.flash(req, "info", "Đã huỷ đơn " + booking.getCode() + ", ghế đã được nhả.");
        }
        resp.sendRedirect(req.getContextPath() + "/booking?action=select-seats&showtimeId="
                + booking.getShowtime().getId());
    }

    /**
     * Trang Lịch sử đặt vé (GET action=history), tách riêng khỏi trang hồ sơ.
     * Chỉ khách hàng (CUSTOMER) xem được; lấy danh sách đơn qua UserDAO.getBookingHistory rồi mở bookingHistory.jsp.
     */
    private void history(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = WebUtil.requireUser(req, resp);
        if (user == null) return;
        if (user.getRole() == null || !"CUSTOMER".equalsIgnoreCase(user.getRole())) {
            resp.sendRedirect(req.getContextPath() + WebUtil.getRoleDefaultPath(user));
            return;
        }
        req.setAttribute("bookingHistory", new UserDAO().getBookingHistory(user.getUserId()));
        req.getRequestDispatcher(VIEW_DIR + "booking/bookingHistory.jsp").forward(req, resp);
    }

    /**
     * Trang chi tiết đơn (GET action=detail): vé từng ghế, bắp nước, các giao dịch thanh toán.
     * Kiểm tra quyền xem bằng BookingService.canView (khách xem đơn của mình, nhân viên xem đơn của rạp mình);
     * không có quyền thì trả 404. Lấy vé và giao dịch qua PaymentService rồi mở bookingDetail.jsp.
     */
    private void detail(HttpServletRequest req, HttpServletResponse resp)
            throws SQLException, ServletException, IOException {
        User user = WebUtil.requireUser(req, resp);
        if (user == null) return;
        Integer bookingId = WebUtil.parseInt(req.getParameter("bookingId"));
        Booking booking = bookingId == null ? null : bookingService.getBooking(bookingId);
        if (booking == null || !bookingService.canView(user, booking)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        req.setAttribute("booking", booking);
        req.setAttribute("tickets", paymentService.getTickets(booking.getId()));
        req.setAttribute("transactions", paymentService.getTransactions(booking.getId()));
        req.setAttribute("isOwner", booking.getUserId() != null && booking.getUserId() == user.getUserId());
        req.getRequestDispatcher(VIEW_DIR + "booking/bookingDetail.jsp").forward(req, resp);
    }

    /**
     * Đọc bookingId trên request và lấy đơn đó, chỉ khi đơn thuộc chính khách đang đăng nhập; ngược lại trả null.
     */
    private Booking loadOwnBooking(HttpServletRequest req, User user) throws SQLException {
        Integer bookingId = WebUtil.parseInt(req.getParameter("bookingId"));
        return bookingId == null ? null : bookingService.getOwnBooking(bookingId, user.getUserId());
    }

    /**
     * Đọc các ô seatId khách tích trong form chọn ghế, bỏ giá trị không phải số. Dùng chung cho đặt vé online và tại quầy.
     */
    public static List<Integer> readSeatIds(HttpServletRequest req) {
        List<Integer> seatIds = new ArrayList<>();
        String[] values = req.getParameterValues("seatId");
        if (values != null) {
            for (String v : values) {
                Integer id = WebUtil.parseInt(v);
                if (id != null) seatIds.add(id);
            }
        }
        return seatIds;
    }

    /**
     * Chuyển chuỗi "yyyy-MM-dd" trên URL thành LocalDate; rỗng hoặc sai định dạng thì trả null (coi như chưa chọn ngày).
     */
    private static LocalDate parseDate(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Tạo nhãn cho các nút ngày: key "yyyy-MM-dd" (dùng trên link), nhãn "Hôm nay 24/11" hoặc "T3 25/11" (hiện cho khách).
     */
    private static Map<String, String> toDateOptions(List<LocalDate> dates) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        Map<String, String> options = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (LocalDate d : dates) {
            String day = d.equals(today) ? "Hôm nay" : WEEKDAYS[d.getDayOfWeek().getValue() - 1];
            options.put(d.toString(), day + " " + d.format(fmt));
        }
        return options;
    }
}
