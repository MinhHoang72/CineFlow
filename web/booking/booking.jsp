<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.User" %>
<%@ page import="model.Movie" %>
<%@ page import="model.Branch" %>
<%@ page import="model.ShowtimeView" %>
<%@ page import="dao.CinemaDAO" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.LinkedHashSet" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale" %>
<%@ page import="java.util.Set" %>
<%!
    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String formatTime(java.sql.Timestamp value) {
        return value == null ? "" : new SimpleDateFormat("HH:mm").format(value);
    }

    private String formatDateKey(java.sql.Timestamp value) {
        return value == null ? "" : new SimpleDateFormat("yyyy-MM-dd").format(value);
    }

    private String formatDateLabel(java.sql.Timestamp value) {
        return value == null ? "" : new SimpleDateFormat("EEE", new Locale("vi", "VN")).format(value);
    }

    private String formatMonthYear(java.sql.Timestamp value) {
        return value == null ? "" : new SimpleDateFormat("MMMM yyyy", new Locale("vi", "VN")).format(value);
    }

    private String formatMoney(java.math.BigDecimal value) {
        return value == null ? "" : String.format("%,.0f đ", value);
    }
%>
<%
    User signedInUser = (User) session.getAttribute("user");
    String memberName = signedInUser == null ? "" : signedInUser.getFullName();
    if (memberName == null || memberName.trim().isEmpty()) {
        memberName = signedInUser == null ? "" : signedInUser.getAccount();
    }
    boolean isCustomer = signedInUser != null && "CUSTOMER".equalsIgnoreCase(signedInUser.getRole());
    String contextPath = request.getContextPath();
    CinemaDAO cinemaDAO = new CinemaDAO();
    List<Movie> movies = cinemaDAO.getNowShowingMovies();
    List<Branch> branches = cinemaDAO.getActiveBranches();
    List<ShowtimeView> showtimes = cinemaDAO.getScheduledShowtimes();
    Set<String> cityNames = new LinkedHashSet<>();
    Set<String> dateKeys = new LinkedHashSet<>();
    List<java.sql.Timestamp> scheduleDates = new ArrayList<>();
    for (Branch branch : branches) {
        if (branch.getCity() != null && !branch.getCity().trim().isEmpty()) cityNames.add(branch.getCity());
    }
    for (ShowtimeView showtime : showtimes) {
        String dateKey = formatDateKey(showtime.getStartTime());
        if (dateKeys.add(dateKey)) scheduleDates.add(showtime.getStartTime());
    }
    int requestedBranchId = -1;
    try {
        requestedBranchId = Integer.parseInt(request.getParameter("branchId"));
    } catch (Exception ignored) {
        requestedBranchId = -1;
    }
    Branch requestedBranch = null;
    for (Branch branch : branches) {
        if (branch.getBranchId() == requestedBranchId) {
            requestedBranch = branch;
            break;
        }
    }
    ShowtimeView initialShowtime = null;
    for (ShowtimeView showtime : showtimes) {
        if (requestedBranchId > 0 && showtime.getBranchId() == requestedBranchId) {
            initialShowtime = showtime;
            break;
        }
    }
    if (initialShowtime == null && !showtimes.isEmpty()) initialShowtime = showtimes.get(0);
        String selectedDate = initialShowtime == null
            ? (scheduleDates.isEmpty() ? "" : formatDateKey(scheduleDates.get(0)))
            : formatDateKey(initialShowtime.getStartTime());
        int selectedBranchId = requestedBranch != null
            ? requestedBranch.getBranchId()
            : (initialShowtime == null ? -1 : initialShowtime.getBranchId());
        String selectedCity = requestedBranch != null
            ? requestedBranch.getCity()
            : (initialShowtime == null ? "" : initialShowtime.getCity());
        int selectedMovieId = initialShowtime != null && initialShowtime.getBranchId() == selectedBranchId
            ? initialShowtime.getMovieId() : -1;
    boolean scheduleView = "schedule".equalsIgnoreCase(request.getParameter("view"));
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#f0eee4">
    <title>CineFlow - Mua vé xem phim</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/booking/booking.css">
</head>
<body>
    <div class="top-bar">
        <div class="top-inner">
            <div class="top-left"><span>CineFlow</span><span>Hệ thống rạp chiếu phim</span></div>
            <div class="top-right">
                <% if (signedInUser == null) { %>
                    <a href="<%= contextPath %>/auth/login.jsp">Đăng nhập</a>
                    <a href="<%= contextPath %>/auth/register.jsp">Đăng ký</a>
                <% } else { %>
                    <span>Xin chào, <%= escapeHtml(memberName) %></span>
                    <form class="account-form" action="<%= contextPath %>/logout" method="post"><button type="submit">Đăng xuất</button></form>
                <% } %>
                <a href="<%= contextPath %>/home/index.jsp#support">Hỗ trợ khách hàng</a>
            </div>
        </div>
    </div>
    <header class="brand-header">
        <a class="logo" href="<%= contextPath %>/home/index.jsp"><span class="logo-mark">C</span><span class="logo-word">Cine<span>Flow</span></span></a>
    </header>
    <nav class="main-nav" aria-label="Điều hướng chính">
        <div class="nav-inner">
            <a href="<%= contextPath %>/home/index.jsp">TRANG CHỦ</a>
            <a class="active" href="<%= contextPath %>/booking/booking.jsp">MUA VÉ</a>
            <a href="<%= contextPath %>/home/index.jsp#movies">PHIM</a>
            <a href="<%= contextPath %>/home/index.jsp#movies">TIN MỚI &amp; ƯU ĐÃI</a>
            <a href="<%= contextPath %>/home/index.jsp#cinemas">RẠP CHIẾU PHIM</a>
            <a href="<%= contextPath %>/home/index.jsp#support">HỖ TRỢ KHÁCH HÀNG</a>
        </div>
    </nav>
    <nav class="top-menu" aria-label="Điều hướng mua vé">
        <a class="<%= scheduleView ? "" : "active" %>" href="#booking">MUA VÉ XEM PHIM</a>
        <a class="<%= scheduleView ? "active" : "" %>" href="#booking-showtimes">LỊCH CHIẾU PHIM</a>
    </nav>
    <main class="container" id="booking">
        <section class="date-section" aria-label="Chọn ngày chiếu">
            <button class="date-arrow left" type="button" aria-label="Ngày trước" data-date-scroll="left">‹</button>
            <div class="month-label" id="bookingMonth"><%= scheduleDates.isEmpty() ? "Chưa có lịch chiếu" : formatMonthYear(scheduleDates.get(0)) %></div>
            <div class="dates" id="bookingDates">
                <% for (java.sql.Timestamp scheduleDate : scheduleDates) {
                    String dateKey = formatDateKey(scheduleDate);
                    String dayOfWeek = new SimpleDateFormat("u").format(scheduleDate);
                    String dateClass = "date" + (dateKey.equals(selectedDate) ? " active" : ("6".equals(dayOfWeek) ? " saturday" : ("7".equals(dayOfWeek) ? " sunday" : "")));
                %>
                    <button class="<%= dateClass %>" type="button" data-date="<%= dateKey %>" data-month="<%= formatMonthYear(scheduleDate) %>">
                        <span class="day-name"><%= escapeHtml(formatDateLabel(scheduleDate)) %></span>
                        <span class="day-number"><%= new SimpleDateFormat("dd").format(scheduleDate) %></span>
                    </button>
                <% } %>
                <% if (scheduleDates.isEmpty()) { %><p class="empty-state">Chưa có ngày chiếu nào được lên lịch.</p><% } %>
            </div>
            <button class="date-arrow right" type="button" aria-label="Ngày tiếp theo" data-date-scroll="right">›</button>
        </section>

        <div class="booking-content" id="bookingContent"
             data-customer="<%= isCustomer %>"
             data-initial-date="<%= selectedDate %>"
             data-initial-city="<%= escapeHtml(selectedCity) %>"
             data-initial-branch="<%= selectedBranchId %>"
             data-initial-movie="<%= selectedMovieId %>">
            <section class="cinema-panel" aria-labelledby="cinema-title">
                <h1 class="panel-title" id="cinema-title">Rạp</h1>
                <div class="login-row">
                    <% if (signedInUser == null) { %>
                        <div><strong>Rạp chiếu phim của tôi</strong><span>Bạn có thể kiểm tra sau khi đăng nhập</span></div>
                        <a href="<%= contextPath %>/auth/login.jsp">Đăng nhập</a>
                    <% } else { %>
                        <div><strong><%= escapeHtml(memberName) %></strong><span>Chọn rạp và suất chiếu phù hợp</span></div>
                        <a href="<%= contextPath %>/home/index.jsp#cinemas">Đổi rạp</a>
                    <% } %>
                </div>
                <div class="city-tabs" aria-label="Lọc theo thành phố">
                    <button class="city-tab<%= selectedCity == null || selectedCity.isEmpty() ? " active" : "" %>" type="button" data-city="">Tất cả</button>
                    <% for (String city : cityNames) { %>
                        <button class="city-tab<%= city.equalsIgnoreCase(selectedCity) ? " active" : "" %>" type="button" data-city="<%= escapeHtml(city) %>"><%= escapeHtml(city) %></button>
                    <% } %>
                </div>
                <div class="centre-area">
                    <div class="centre-grid" id="bookingCentres">
                        <% for (Branch branch : branches) { %>
                            <button class="centre<%= branch.getBranchId() == selectedBranchId ? " active" : "" %>" type="button" data-branch-id="<%= branch.getBranchId() %>" data-city="<%= escapeHtml(branch.getCity()) %>" data-name="<%= escapeHtml(branch.getBranchName()) %>"><%= escapeHtml(branch.getBranchName()) %></button>
                        <% } %>
                        <% if (branches.isEmpty()) { %><p class="empty-state">Chưa có rạp đang hoạt động.</p><% } %>
                    </div>
                </div>
            </section>

            <section class="movie-panel" aria-labelledby="movie-title">
                <h2 class="panel-title" id="movie-title">Phim</h2>
                <div class="movie-tabs"><strong>Phim đang chiếu</strong><span>|</span><span>Chọn phim có suất tại rạp</span></div>
                <div class="movie-list" id="bookingMovies">
                    <% for (Movie movie : movies) {
                        String rating = movie.getAgeRating() == null ? "" : movie.getAgeRating().toUpperCase();
                        String ratingClass = "";
                        if ("T13".equals(rating)) ratingClass = " age-13";
                        else if ("T16".equals(rating)) ratingClass = " age-16";
                        else if ("T18".equals(rating)) ratingClass = " age-18";
                        else if ("P".equals(rating) || "K".equals(rating)) ratingClass = " age-p";
                    %>
                        <button class="movie-item<%= movie.getMovieId() == selectedMovieId ? " active" : "" %>" type="button" data-movie-id="<%= movie.getMovieId() %>" data-title="<%= escapeHtml(movie.getTitle()) %>">
                            <span class="movie-age<%= ratingClass %>"><%= escapeHtml(movie.getAgeRating()) %></span>
                            <span class="movie-name"><%= escapeHtml(movie.getTitle()) %></span>
                        </button>
                    <% } %>
                    <% if (movies.isEmpty()) { %><p class="empty-state">Chưa có phim đang chiếu.</p><% } %>
                    <p class="empty-state" id="bookingMovieEmpty" hidden>Rạp và ngày đã chọn chưa có phim được xếp suất.</p>
                </div>
            </section>

            <section class="booking-showtimes" id="booking-showtimes" aria-labelledby="showtimes-title">
                <h2 id="showtimes-title">Lịch chiếu</h2>
                <p class="selected-info" id="bookingSelectionInfo">Chọn ngày, rạp và phim để xem giờ chiếu.</p>
                <div class="showtimes" id="bookingShowtimes">
                    <% for (ShowtimeView showtime : showtimes) { %>
                        <button class="showtime" type="button"
                                data-showtime-id="<%= showtime.getShowtimeId() %>"
                                data-date="<%= formatDateKey(showtime.getStartTime()) %>"
                                data-branch-id="<%= showtime.getBranchId() %>"
                                data-movie-id="<%= showtime.getMovieId() %>"
                                data-time="<%= formatTime(showtime.getStartTime()) %>"
                                data-hall="<%= escapeHtml(showtime.getHallName()) %>"
                                data-price="<%= formatMoney(showtime.getBaseTicketPrice()) %>">
                            <%= formatTime(showtime.getStartTime()) %>
                        </button>
                    <% } %>
                </div>
                <p class="empty-state" id="bookingShowtimeEmpty" hidden>Không có suất chiếu phù hợp với lựa chọn này.</p>
                <p class="booking-message"><span id="bookingMessage">Chọn một suất chiếu để tiếp tục.</span> <a id="bookingLoginLink" href="<%= contextPath %>/auth/login.jsp" hidden>Đăng nhập</a></p>
            </section>
        </div>
    </main>
    <script>
        (function () {
            const content = document.getElementById("bookingContent");
            const dateButtons = Array.from(document.querySelectorAll(".date[data-date]"));
            const cityButtons = Array.from(document.querySelectorAll(".city-tab"));
            const centreButtons = Array.from(document.querySelectorAll(".centre[data-branch-id]"));
            const movieButtons = Array.from(document.querySelectorAll(".movie-item[data-movie-id]"));
            const showtimeButtons = Array.from(document.querySelectorAll(".showtime[data-showtime-id]"));
            const bookingMonth = document.getElementById("bookingMonth");
            const movieEmpty = document.getElementById("bookingMovieEmpty");
            const showtimeEmpty = document.getElementById("bookingShowtimeEmpty");
            const selectionInfo = document.getElementById("bookingSelectionInfo");
            const message = document.getElementById("bookingMessage");
            const loginLink = document.getElementById("bookingLoginLink");
            const dates = document.getElementById("bookingDates");
            let selectedDate = content.dataset.initialDate;
            let selectedCity = content.dataset.initialCity;
            let selectedBranchId = content.dataset.initialBranch;
            let selectedMovieId = content.dataset.initialMovie;
            let selectedShowtimeId = "";
            const isCustomer = content.dataset.customer === "true";

            function normalize(value) {
                return value.normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/\u0111/g, "d").replace(/\u0110/g, "D").toLowerCase();
            }

            function render() {
                dateButtons.forEach((button) => button.classList.toggle("active", button.dataset.date === selectedDate));
                const activeDate = dateButtons.find((button) => button.dataset.date === selectedDate);
                if (activeDate) bookingMonth.textContent = activeDate.dataset.month;
                cityButtons.forEach((button) => button.classList.toggle("active", normalize(button.dataset.city || "") === normalize(selectedCity || "")));
                centreButtons.forEach((button) => { button.hidden = Boolean(selectedCity) && normalize(button.dataset.city || "") !== normalize(selectedCity); });
                let activeCentre = centreButtons.find((button) => button.dataset.branchId === selectedBranchId && !button.hidden);
                if (!activeCentre) {
                    activeCentre = centreButtons.find((button) => !button.hidden);
                    selectedBranchId = activeCentre ? activeCentre.dataset.branchId : "";
                }
                centreButtons.forEach((button) => button.classList.toggle("active", button === activeCentre));
                const availableMovies = movieButtons.filter((movie) => showtimeButtons.some((showtime) => showtime.dataset.date === selectedDate && showtime.dataset.branchId === selectedBranchId && showtime.dataset.movieId === movie.dataset.movieId));
                movieButtons.forEach((movie) => { movie.hidden = !availableMovies.includes(movie); });
                if (!availableMovies.some((movie) => movie.dataset.movieId === selectedMovieId)) {
                    selectedMovieId = availableMovies.length ? availableMovies[0].dataset.movieId : "";
                    selectedShowtimeId = "";
                }
                movieButtons.forEach((movie) => movie.classList.toggle("active", movie.dataset.movieId === selectedMovieId));
                movieEmpty.hidden = availableMovies.length > 0;
                const matchingShowtimes = showtimeButtons.filter((showtime) => showtime.dataset.date === selectedDate && showtime.dataset.branchId === selectedBranchId && showtime.dataset.movieId === selectedMovieId);
                if (!matchingShowtimes.some((showtime) => showtime.dataset.showtimeId === selectedShowtimeId)) selectedShowtimeId = "";
                showtimeButtons.forEach((button) => {
                    button.hidden = !matchingShowtimes.includes(button);
                    button.classList.toggle("active", button.dataset.showtimeId === selectedShowtimeId);
                });
                showtimeEmpty.hidden = matchingShowtimes.length > 0;
                const movie = movieButtons.find((button) => button.dataset.movieId === selectedMovieId);
                const showtime = showtimeButtons.find((button) => button.dataset.showtimeId === selectedShowtimeId);
                const dateLabel = activeDate ? activeDate.querySelector(".day-number").textContent + " " + activeDate.querySelector(".day-name").textContent : "";
                selectionInfo.textContent = [selectedCity, activeCentre && activeCentre.dataset.name, movie && movie.dataset.title, dateLabel].filter(Boolean).join(" / ") || "Chọn ngày, rạp và phim để xem giờ chiếu.";
                if (showtime) {
                    const details = [showtime.dataset.time, showtime.dataset.hall, showtime.dataset.price].filter(Boolean).join(" · ");
                    message.textContent = isCustomer ? "Đã chọn " + details + ". Bước chọn ghế chưa được tích hợp." : "Đã chọn " + details + ". Đăng nhập để tiếp tục đặt vé.";
                    loginLink.hidden = isCustomer;
                } else {
                    message.textContent = "Chọn một suất chiếu để tiếp tục.";
                    loginLink.hidden = true;
                }
            }

            dateButtons.forEach((button) => button.addEventListener("click", () => { selectedDate = button.dataset.date; selectedShowtimeId = ""; render(); }));
            cityButtons.forEach((button) => button.addEventListener("click", () => { selectedCity = button.dataset.city; selectedShowtimeId = ""; render(); }));
            centreButtons.forEach((button) => button.addEventListener("click", () => { selectedBranchId = button.dataset.branchId; selectedCity = button.dataset.city; selectedShowtimeId = ""; render(); }));
            movieButtons.forEach((button) => button.addEventListener("click", () => { selectedMovieId = button.dataset.movieId; selectedShowtimeId = ""; render(); }));
            showtimeButtons.forEach((button) => button.addEventListener("click", () => { selectedShowtimeId = button.dataset.showtimeId; render(); }));
            document.querySelectorAll("[data-date-scroll]").forEach((button) => button.addEventListener("click", () => dates.scrollBy({ left: button.dataset.dateScroll === "right" ? 240 : -240, behavior: "smooth" })));
            render();
            if (new URLSearchParams(window.location.search).get("view") === "schedule") document.getElementById("booking-showtimes").scrollIntoView();
        }());
    </script>
</body>
</html>




