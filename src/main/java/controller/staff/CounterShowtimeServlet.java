package controller.staff;

import controller.customer.BookingServlet;
import controller.customer.PaymentServlet;
import dao.BookingDAO;
import dao.BookingDAO.SeatUnavailableException;
import dao.PaymentDAO;
import dao.PaymentDAO.Outcome;
import dao.SeatDAO;
import dao.ShowtimeDAO;
import model.entity.Booking;
import model.dto.booking.FnbMenuItemDTO;
import model.dto.booking.PaymentTransaction;
import model.entity.Seat;
import model.entity.Showtime;
import model.entity.Ticket;
import model.entity.User;
import service.BookingExtrasService;
import utils.QrUtil;
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
import java.util.TreeMap;

/**
 * Bán vé tại quầy (dành cho STAFF/TICKET_SALES_STAFF tại rạp của mình).
 * Khách vãng lai: booking COUNTER, UserID = NULL.
 *   GET  /staff/counter                       chọn ngày + suất chiếu của chi nhánh
 *   GET  /staff/counter?action=seats          sơ đồ ghế
 *   POST /staff/counter?action=hold           giữ ghế 5 phút
 *   GET  /staff/counter?action=fnb            chọn bắp nước (bỏ qua nếu rạp chưa bán món nào)
 *   POST /staff/counter?action=save-fnb       lưu bắp nước rồi sang trang thu tiền
 *   GET  /staff/counter?action=checkout       tóm tắt đơn + voucher + chọn cách thu tiền
 *   POST /staff/counter?action=vnpay          chuyển sang VNPay để khách quét QR / thẻ
 *   POST /staff/counter?action=cash           thu tiền mặt -> xuất vé
 *   POST /staff/counter?action=cancel         huỷ đơn
 *   GET  /staff/counter?action=ticket         in vé
 */
@WebServlet(name = "CounterShowtimeServlet", urlPatterns = {"/staff/counter"})
public class CounterShowtimeServlet extends HttpServlet {

    private static final String VIEW_BASE = "/WEB-INF/views/staff/counter/";
    private static final String[] WEEKDAYS = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};

    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO();
    private final SeatDAO seatDAO = new SeatDAO();
    private final BookingDAO bookingDAO = new BookingDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final BookingExtrasService extrasService = new BookingExtrasService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User staff = requireCounterStaff(req, resp);
        if (staff == null) return;
        String action = req.getParameter("action");
        try {
            if (action == null || action.isEmpty()) {
                showtimes(req, resp, staff);
            } else if ("seats".equals(action)) {
                seats(req, resp, staff);
            } else if ("fnb".equals(action)) {
                selectFnb(req, resp, staff);
            } else if ("checkout".equals(action)) {
                checkout(req, resp, staff);
            } else if ("ticket".equals(action)) {
                ticket(req, resp, staff);
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User staff = requireCounterStaff(req, resp);
        if (staff == null) return;
        String action = req.getParameter("action");
        try {
            switch (action == null ? "" : action) {
                case "hold" -> hold(req, resp, staff);
                case "cash" -> payCash(req, resp, staff);
                case "vnpay" -> payVnpay(req, resp, staff);
                case "cancel" -> cancel(req, resp, staff);
                case "save-fnb", "apply-voucher", "remove-voucher" -> updateExtras(req, resp, staff, action);
                default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void showtimes(HttpServletRequest req, HttpServletResponse resp, User staff)
            throws SQLException, ServletException, IOException {
        List<LocalDate> dates = showtimeDAO.getShowDatesAtCentre(staff.getCentreId());
        LocalDate date = parseDate(req.getParameter("date"));
        if ((date == null || !dates.contains(date)) && !dates.isEmpty()) date = dates.get(0);

        Map<String, List<Showtime>> byMovie = new LinkedHashMap<>();
        if (date != null) {
            for (Showtime s : showtimeDAO.getShowtimesAtCentre(staff.getCentreId(), date)) {
                byMovie.computeIfAbsent(s.getMovieTitle(), k -> new ArrayList<>()).add(s);
            }
        }
        req.setAttribute("dateOptions", toDateOptions(dates));
        req.setAttribute("selectedDate", date == null ? null : date.toString());
        req.setAttribute("showtimesByMovie", byMovie);
        req.getRequestDispatcher(VIEW_BASE + "counterShowtime.jsp").forward(req, resp);
    }

    private void seats(HttpServletRequest req, HttpServletResponse resp, User staff)
            throws SQLException, ServletException, IOException {
        Integer showtimeId = WebUtil.parseInt(req.getParameter("showtimeId"));
        Showtime showtime = showtimeId == null ? null : showtimeDAO.getBookableById(showtimeId);
        if (showtime == null || showtime.getCentreId() != staff.getCentreId()) {
            WebUtil.flash(req, "warning", "Suất chiếu không tồn tại, đã ngừng bán hoặc không thuộc rạp của bạn.");
            resp.sendRedirect(req.getContextPath() + "/staff/counter");
            return;
        }

        Map<String, List<Seat>> seatRows = new LinkedHashMap<>();
        Map<String, BigDecimal> priceByType = new TreeMap<>();
        for (Seat seat : seatDAO.getSeatMap(showtime)) {
            seatRows.computeIfAbsent(seat.getRow(), k -> new ArrayList<>()).add(seat);
            priceByType.putIfAbsent(seat.getType(), seat.getPrice());
        }
        String ctx = req.getContextPath();
        req.setAttribute("showtime", showtime);
        req.setAttribute("seatRows", seatRows);
        req.setAttribute("priceByType", priceByType);
        req.setAttribute("maxSeats", BookingDAO.MAX_SEATS_PER_BOOKING);
        req.setAttribute("holdMinutes", BookingDAO.HOLD_MINUTES);
        req.setAttribute("seatFormAction", ctx + "/staff/counter");
        req.setAttribute("seatBackUrl", ctx + "/staff/counter?date=" + showtime.getStartTime().toLocalDateTime().toLocalDate());
        req.setAttribute("pageTitle", "Bán vé tại quầy - Chọn ghế");
        req.getRequestDispatcher("/WEB-INF/views/customer/booking/selectSeats.jsp").forward(req, resp);
    }

    private void hold(HttpServletRequest req, HttpServletResponse resp, User staff) throws SQLException, IOException {
        Integer showtimeId = WebUtil.parseInt(req.getParameter("showtimeId"));
        Showtime showtime = showtimeId == null ? null : showtimeDAO.getBookableById(showtimeId);
        if (showtime == null || showtime.getCentreId() != staff.getCentreId()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        List<Integer> seatIds = new ArrayList<>();
        String[] values = req.getParameterValues("seatId");
        if (values != null) {
            for (String v : values) {
                Integer id = WebUtil.parseInt(v);
                if (id != null) seatIds.add(id);
            }
        }
        String ctx = req.getContextPath();
        try {
            int bookingId = bookingDAO.holdCounterSeats(showtimeId, seatIds);
            resp.sendRedirect(ctx + "/staff/counter?action=fnb&bookingId=" + bookingId);
        } catch (SeatUnavailableException e) {
            WebUtil.flash(req, "warning", e.getMessage());
            resp.sendRedirect(ctx + "/staff/counter?action=seats&showtimeId=" + showtimeId);
        }
    }

    private void checkout(HttpServletRequest req, HttpServletResponse resp, User staff)
            throws SQLException, ServletException, IOException {
        Booking booking = loadCounterBooking(req, staff);
        if (booking == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String ctx = req.getContextPath();
        if ("CONFIRMED".equals(booking.getStatus())) {
            resp.sendRedirect(ctx + "/staff/counter?action=ticket&bookingId=" + booking.getId());
            return;
        }
        req.setAttribute("booking", booking);
        if (booking.isHoldActive()) {
            req.setAttribute("extrasAction", ctx + "/staff/counter");
            req.setAttribute("fnbUrl", ctx + "/staff/counter?action=fnb&bookingId=" + booking.getId());
            req.setAttribute("voucherOptions", extrasService.getVoucherOptions(booking));
        }
        req.getRequestDispatcher(VIEW_BASE + "counterCheckout.jsp").forward(req, resp);
    }

    /** Chọn bắp nước cho đơn tại quầy; dùng chung trang với đặt vé online. */
    private void selectFnb(HttpServletRequest req, HttpServletResponse resp, User staff)
            throws SQLException, ServletException, IOException {
        Booking booking = loadCounterBooking(req, staff);
        if (booking == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String ctx = req.getContextPath();
        String checkoutUrl = ctx + "/staff/counter?action=checkout&bookingId=" + booking.getId();
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
        req.setAttribute("extrasAction", ctx + "/staff/counter");
        req.setAttribute("pageTitle", "Bán vé tại quầy - Bắp nước");
        req.getRequestDispatcher("/WEB-INF/views/customer/booking/selectFnb.jsp").forward(req, resp);
    }

    private void payCash(HttpServletRequest req, HttpServletResponse resp, User staff) throws SQLException, IOException {
        Booking booking = loadCounterBooking(req, staff);
        if (booking == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!booking.isHoldActive()) {
            WebUtil.flash(req, "warning", "Đơn đã hết thời gian giữ ghế, vui lòng chọn ghế lại.");
            resp.sendRedirect(req.getContextPath() + "/staff/counter?action=checkout&bookingId=" + booking.getId());
            return;
        }
        BigDecimal received = parseMoney(req.getParameter("cashReceived"));
        if (received == null || received.compareTo(booking.getTotalAmount()) < 0) {
            WebUtil.flash(req, "warning", "Tiền khách đưa phải lớn hơn hoặc bằng tổng tiền của đơn.");
            resp.sendRedirect(req.getContextPath() + "/staff/counter?action=checkout&bookingId=" + booking.getId());
            return;
        }
        int paymentId = paymentDAO.createCashPayment(booking.getId(), booking.getCode(), booking.getTotalAmount(), received);
        finishPayment(req, resp, staff, booking, paymentDAO.confirmPaid(paymentId, staff.getCentreId()));
    }

    /** Khách quét QR / nhập thẻ trên trang VNPay; VNPay chuyển về /payment/vnpay-return -> in vé. */
    private void payVnpay(HttpServletRequest req, HttpServletResponse resp, User staff) throws SQLException, IOException {
        Booking booking = loadCounterBooking(req, staff);
        if (booking == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String checkoutUrl = req.getContextPath() + "/staff/counter?action=checkout&bookingId=" + booking.getId();
        if (!booking.isHoldActive()) {
            WebUtil.flash(req, "warning", "Đơn đã hết thời gian giữ ghế, vui lòng chọn ghế lại.");
            resp.sendRedirect(checkoutUrl);
            return;
        }
        String payUrl = PaymentServlet.startVnpay(req, booking);
        resp.sendRedirect(payUrl == null ? checkoutUrl : payUrl);
    }

    private void finishPayment(HttpServletRequest req, HttpServletResponse resp, User staff, Booking booking,
            Outcome outcome) throws IOException {
        String ctx = req.getContextPath();
        switch (outcome.status()) {
            case SUCCESS, ALREADY_PAID -> {
                WebUtil.flash(req, "success", "Đã thu tiền đơn " + booking.getCode() + ". In vé cho khách.");
                resp.sendRedirect(ctx + "/staff/counter?action=ticket&bookingId=" + booking.getId());
                return;
            }
            case PAID_BUT_SEATS_LOST -> WebUtil.flash(req, "danger", "Đơn đã hết hạn giữ ghế và ghế đã được người "
                    + "khác đặt. Đã ghi nhận tiền, cần hoàn lại cho khách.");
            case PAID_AMOUNT_MISMATCH -> WebUtil.flash(req, "danger", "Giao dịch được tạo trước khi đơn thay đổi "
                    + "(bắp nước / mã giảm giá). Đã ghi nhận tiền, cần hoàn lại cho khách và thanh toán lại.");
            case NOT_FOUND -> WebUtil.flash(req, "danger", "Không tìm thấy giao dịch.");
        }
        resp.sendRedirect(ctx + "/staff/counter?action=checkout&bookingId=" + booking.getId());
    }

    /** Chọn bắp nước / áp voucher cho đơn tại quầy đang giữ ghế. */
    private void updateExtras(HttpServletRequest req, HttpServletResponse resp, User staff, String action)
            throws SQLException, IOException {
        Booking booking = loadCounterBooking(req, staff);
        if (booking == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        boolean ok = false;
        if (booking.isHoldActive()) {
            ok = BookingServlet.applyExtras(req, booking, action, extrasService);
        } else {
            WebUtil.flash(req, "warning", "Đơn đã hết thời gian giữ ghế, vui lòng chọn ghế lại.");
        }
        String next = "save-fnb".equals(action) && !ok ? "fnb" : "checkout";
        resp.sendRedirect(req.getContextPath() + "/staff/counter?action=" + next + "&bookingId=" + booking.getId());
    }

    private void cancel(HttpServletRequest req, HttpServletResponse resp, User staff) throws SQLException, IOException {
        Booking booking = loadCounterBooking(req, staff);
        if (booking == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (bookingDAO.cancelCounterPending(booking.getId())) {
            WebUtil.flash(req, "info", "Đã huỷ đơn " + booking.getCode() + ", ghế đã được nhả.");
        }
        resp.sendRedirect(req.getContextPath() + "/staff/counter?action=seats&showtimeId=" + booking.getShowtime().getId());
    }

    private void ticket(HttpServletRequest req, HttpServletResponse resp, User staff)
            throws SQLException, ServletException, IOException {
        Booking booking = loadCounterBooking(req, staff);
        if (booking == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        List<Ticket> tickets = paymentDAO.getTickets(booking.getId());
        PaymentTransaction paid = null;
        for (PaymentTransaction p : paymentDAO.getTransactions(booking.getId())) {
            if ("PAID".equals(p.getStatus())) paid = p;
        }
        req.setAttribute("booking", booking);
        req.setAttribute("tickets", tickets);
        req.setAttribute("paidTransaction", paid);
        if (!tickets.isEmpty()) req.setAttribute("bookingQr", QrUtil.toDataUri(booking.getQrContent()));
        req.getRequestDispatcher(VIEW_BASE + "counterTicketPrint.jsp").forward(req, resp);
    }

    /** Đơn tại quầy thuộc chi nhánh của nhân viên, null nếu không hợp lệ. */
    private Booking loadCounterBooking(HttpServletRequest req, User staff) throws SQLException {
        Integer bookingId = WebUtil.parseInt(req.getParameter("bookingId"));
        if (bookingId == null) return null;
        Booking booking = bookingDAO.getBooking(bookingId);
        if (booking == null || !booking.isCounter() || booking.getShowtime().getCentreId() != staff.getCentreId()) {
            return null;
        }
        return booking;
    }

    /** Chỉ nhân viên bán vé (TICKET_SALES_STAFF) đã được gán chi nhánh mới bán vé tại quầy. */
    private static User requireCounterStaff(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = WebUtil.requireUser(req, resp);
        if (user == null) return null;
        if (!WebUtil.isSalesStaff(user) || user.getCentreId() == null) {
            resp.sendRedirect(req.getContextPath() + WebUtil.getRoleDefaultPath(user));
            return null;
        }
        return user;
    }

    /** "200.000", "200,000 đ", "200000" -> 200000; null nếu không hợp lệ. */
    private static BigDecimal parseMoney(String value) {
        if (value == null) return null;
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isEmpty() || digits.length() > 12) return null;
        return new BigDecimal(digits);
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

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
