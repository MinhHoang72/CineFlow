package controller.booking;

import dao.BookingDAO;
import dao.BookingDAO.SeatUnavailableException;
import dao.ShowtimeDAO.CityOption;
import dao.MovieDAO;
import dao.PaymentDAO;
import dao.SeatDAO;
import dao.ShowtimeDAO;
import model.entity.Booking;
import model.entity.Centre;
import model.entity.Seat;
import model.entity.Showtime;
import model.entity.Ticket;
import model.entity.User;
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
 * FE-05 Dat ve:
 *   GET  /booking                          chon thanh pho -> rap -> phim -> ngay -> suat chieu
 *   GET  /booking?action=select-seats      chon ghe (can dang nhap)
 *   POST /booking?action=hold              giu ghe 5 phut, tao booking PENDING
 *   GET  /booking?action=checkout          xem lai don + dem nguoc + nut thanh toan
 *   POST /booking?action=cancel            huy booking dang cho thanh toan
 *   GET  /booking?action=history           lich su dat ve cua khach
 *   GET  /booking?action=detail            chi tiet don (khach, nhan vien, quan ly, admin)
 */
@WebServlet(name = "BookingServlet", urlPatterns = {"/booking"})
public class BookingServlet extends HttpServlet {

    private static final String VIEW_DIR = "/WEB-INF/views/";
    private static final String[] WEEKDAYS = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};

    private final MovieDAO movieDAO = new MovieDAO();
    private final ShowtimeDAO showtimeDAO = new ShowtimeDAO();
    private final SeatDAO seatDAO = new SeatDAO();
    private final BookingDAO bookingDAO = new BookingDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        try {
            if (action == null || action.isEmpty() || "select-showtime".equals(action)) {
                selectShowtime(req, resp);
            } else if ("select-seats".equals(action)) {
                selectSeats(req, resp);
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

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        try {
            if ("hold".equals(action)) {
                hold(req, resp);
            } else if ("cancel".equals(action)) {
                cancel(req, resp);
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void selectShowtime(HttpServletRequest req, HttpServletResponse resp)
            throws SQLException, ServletException, IOException {
        String city = req.getParameter("city");
        if (city != null && city.isBlank()) city = null;
        Integer branchId = WebUtil.parseInt(req.getParameter("branchId"));
        Integer movieId = WebUtil.parseInt(req.getParameter("movieId"));

        // Da chon phim (tu trang chu / chi tiet phim, hoac o buoc 3): thanh pho va rap chi hien noi chieu phim do
        List<CityOption> cities = movieId == null ? List.of() : showtimeDAO.getCities(movieId);
        if (movieId != null && cities.isEmpty()) {
            WebUtil.flash(req, "info", "Phim này hiện chưa có suất chiếu mở bán.");
            movieId = null;
        }
        if (movieId == null) cities = showtimeDAO.getCities(null);

        // Chi co phim: chon san rap co suat som nhat cua phim
        if (movieId != null && city == null && branchId == null) {
            branchId = showtimeDAO.findFirstCentreForMovie(movieId);
        }
        // Link cu chi co branchId (booking/booking.jsp?centreId=...): suy ra thanh pho cua rap
        if (city == null && branchId != null) city = showtimeDAO.getCityOfCentre(branchId);
        final String chosenCity = city;
        if (city != null && cities.stream().noneMatch(c -> c.name().equals(chosenCity))) city = null;
        if (city == null && cities.size() == 1) city = cities.get(0).name();

        List<Centre> centres = city == null ? List.of() : showtimeDAO.getCentresInCity(city, movieId);
        final Integer chosenBranch = branchId;
        if (branchId != null && centres.stream().noneMatch(c -> c.getCentreId() == chosenBranch)) branchId = null;
        if (branchId == null && centres.size() == 1) branchId = centres.get(0).getCentreId();

        req.setAttribute("cities", cities);
        req.setAttribute("selectedCity", city);
        req.setAttribute("centres", centres);
        req.setAttribute("selectedBranchId", branchId);
        req.setAttribute("selectedMovieId", movieId);

        if (branchId != null) {
            req.setAttribute("movies", movieDAO.getMoviesShowingAt(branchId));
        }
        if (branchId != null && movieId != null) {
            List<LocalDate> dates = showtimeDAO.getShowDates(branchId, movieId);
            LocalDate date = parseDate(req.getParameter("date"));
            if ((date == null || !dates.contains(date)) && !dates.isEmpty()) date = dates.get(0);

            req.setAttribute("dateOptions", toDateOptions(dates));
            if (date != null) {
                req.setAttribute("selectedDate", date.toString());
                req.setAttribute("showtimes", showtimeDAO.getShowtimes(branchId, movieId, date));
            }
        }
        req.getRequestDispatcher(VIEW_DIR + "booking/selectShowtime.jsp").forward(req, resp);
    }

    private void selectSeats(HttpServletRequest req, HttpServletResponse resp)
            throws SQLException, ServletException, IOException {
        if (WebUtil.requireUser(req, resp) == null) return;

        Integer showtimeId = WebUtil.parseInt(req.getParameter("showtimeId"));
        Showtime showtime = showtimeId == null ? null : showtimeDAO.getBookableById(showtimeId);
        if (showtime == null) {
            WebUtil.flash(req, "warning", "Suất chiếu không tồn tại hoặc đã ngừng bán vé.");
            resp.sendRedirect(req.getContextPath() + "/booking");
            return;
        }

        Map<String, List<Seat>> seatRows = new LinkedHashMap<>();
        Map<String, BigDecimal> priceByType = new TreeMap<>();
        for (Seat seat : seatDAO.getSeatMap(showtime)) {
            seatRows.computeIfAbsent(seat.getRow(), k -> new ArrayList<>()).add(seat);
            priceByType.putIfAbsent(seat.getType(), seat.getPrice());
        }

        req.setAttribute("showtime", showtime);
        req.setAttribute("seatRows", seatRows);
        req.setAttribute("priceByType", priceByType);
        req.setAttribute("maxSeats", BookingDAO.MAX_SEATS_PER_BOOKING);
        req.setAttribute("holdMinutes", BookingDAO.HOLD_MINUTES);
        req.getRequestDispatcher(VIEW_DIR + "booking/selectSeats.jsp").forward(req, resp);
    }

    private void hold(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        User user = WebUtil.requireUser(req, resp);
        if (user == null) return;

        Integer showtimeId = WebUtil.parseInt(req.getParameter("showtimeId"));
        if (showtimeId == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
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

        try {
            int bookingId = bookingDAO.holdSeats(user.getUserId(), showtimeId, seatIds);
            resp.sendRedirect(req.getContextPath() + "/booking?action=checkout&bookingId=" + bookingId);
        } catch (SeatUnavailableException e) {
            WebUtil.flash(req, "warning", e.getMessage());
            resp.sendRedirect(req.getContextPath() + "/booking?action=select-seats&showtimeId=" + showtimeId);
        }
    }

    private void checkout(HttpServletRequest req, HttpServletResponse resp)
            throws SQLException, ServletException, IOException {
        User user = WebUtil.requireUser(req, resp);
        if (user == null) return;

        Booking booking = loadOwnBooking(req, user);
        if (booking == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if ("CONFIRMED".equals(booking.getStatus())) {
            resp.sendRedirect(req.getContextPath() + "/booking?action=detail&bookingId=" + booking.getId());
            return;
        }
        req.setAttribute("booking", booking);
        req.getRequestDispatcher(VIEW_DIR + "booking/bookingCheckout.jsp").forward(req, resp);
    }

    private void cancel(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        User user = WebUtil.requireUser(req, resp);
        if (user == null) return;

        Booking booking = loadOwnBooking(req, user);
        if (booking == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (bookingDAO.cancelPending(booking.getId(), user.getUserId())) {
            WebUtil.flash(req, "info", "Đã huỷ đơn " + booking.getCode() + ", ghế đã được nhả.");
        }
        resp.sendRedirect(req.getContextPath() + "/booking?action=select-seats&showtimeId="
                + booking.getShowtime().getId());
    }

    /** 5.5 Lich su dat ve cua khach. */
    private void history(HttpServletRequest req, HttpServletResponse resp)
            throws SQLException, ServletException, IOException {
        User user = WebUtil.requireUser(req, resp);
        if (user == null) return;
        req.setAttribute("bookings", bookingDAO.getUserBookings(user.getUserId()));
        req.getRequestDispatcher(VIEW_DIR + "booking/bookingHistory.jsp").forward(req, resp);
    }

    /** 5.4 Chi tiet don: ve, chi tiet gia, cac giao dich thanh toan. */
    private void detail(HttpServletRequest req, HttpServletResponse resp)
            throws SQLException, ServletException, IOException {
        User user = WebUtil.requireUser(req, resp);
        if (user == null) return;
        Integer bookingId = WebUtil.parseInt(req.getParameter("bookingId"));
        Booking booking = bookingId == null ? null : bookingDAO.getBooking(bookingId);
        if (booking == null || !canView(user, booking)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        List<Ticket> tickets = paymentDAO.getTickets(booking.getId());
        req.setAttribute("booking", booking);
        req.setAttribute("tickets", tickets);
        req.setAttribute("transactions", paymentDAO.getTransactions(booking.getId()));
        req.setAttribute("isOwner", booking.getUserId() != null && booking.getUserId() == user.getUserId());
        if (!tickets.isEmpty()) req.setAttribute("bookingQr", QrUtil.toDataUri(booking.getQrContent()));
        req.getRequestDispatcher(VIEW_DIR + "booking/bookingDetail.jsp").forward(req, resp);
    }

    /** Khach xem don cua minh; nhan vien ban ve/MANAGER xem don cua chi nhanh minh (MANAGER chua gan chi nhanh: tat ca); ADMIN tat ca. */
    private static boolean canView(User user, Booking booking) {
        if (booking.getUserId() != null && booking.getUserId() == user.getUserId()) return true;
        int centreId = booking.getShowtime().getBranchId();
        if ("ADMIN".equals(user.getRole())) return true;
        if ("MANAGER".equals(user.getRole())) return user.getCentreId() == null || user.getCentreId() == centreId;
        if (WebUtil.isSalesStaff(user)) return user.getCentreId() != null && user.getCentreId() == centreId;
        return false;
    }

    private Booking loadOwnBooking(HttpServletRequest req, User user) throws SQLException {
        Integer bookingId = WebUtil.parseInt(req.getParameter("bookingId"));
        if (bookingId == null) return null;
        Booking booking = bookingDAO.getBooking(bookingId);
        if (booking == null || booking.getUserId() == null || booking.getUserId() != user.getUserId()) return null;
        return booking;
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** yyyy-MM-dd -> "T3 13/10" */
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
