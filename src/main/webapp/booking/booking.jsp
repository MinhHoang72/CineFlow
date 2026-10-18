<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.User" %>
<%@ page import="model.Movie" %>
<%@ page import="model.Centre" %>
<%@ page import="model.ShowtimeView" %>
<%@ page import="dao.CinemaDAO" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Calendar" %>
<%@ page import="java.util.Collections" %>
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

    private String formatDayOfWeekVi(java.sql.Timestamp value, String todayKey) {
        if (value == null) return "";
        String dateKey = formatDateKey(value);
        if (dateKey.equals(todayKey)) {
            return "Hôm nay";
        }
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(value.getTime());
        int dow = c.get(Calendar.DAY_OF_WEEK);
        switch (dow) {
            case Calendar.SUNDAY: return "CN";
            case Calendar.MONDAY: return "T2";
            case Calendar.TUESDAY: return "T3";
            case Calendar.WEDNESDAY: return "T4";
            case Calendar.THURSDAY: return "T5";
            case Calendar.FRIDAY: return "T6";
            case Calendar.SATURDAY: return "T7";
            default: return "";
        }
    }

    private String formatDateMonthYearVi(java.sql.Timestamp value) {
        if (value == null) return "";
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(value.getTime());
        int m = c.get(Calendar.MONTH) + 1;
        int y = c.get(Calendar.YEAR);
        return "Tháng " + m + ", " + y;
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

    List<Movie> nowShowing = cinemaDAO.getNowShowingMovies();
    List<Movie> comingSoon = cinemaDAO.getComingSoonMovies();
    List<Movie> movies = new ArrayList<>(nowShowing);
    for (Movie cm : comingSoon) {
        boolean found = false;
        for (Movie m : movies) {
            if (m.getMovieId() == cm.getMovieId()) {
                found = true;
                break;
            }
        }
        if (!found) {
            movies.add(cm);
        }
    }

    List<Centre> centres = cinemaDAO.getActiveCentres();
    List<ShowtimeView> showtimes = cinemaDAO.getScheduledShowtimes();

    // Lấy ngày hôm nay theo hệ thống (00:00:00)
    Calendar todayCal = Calendar.getInstance();
    todayCal.set(Calendar.HOUR_OF_DAY, 0);
    todayCal.set(Calendar.MINUTE, 0);
    todayCal.set(Calendar.SECOND, 0);
    todayCal.set(Calendar.MILLISECOND, 0);
    java.sql.Timestamp todayTimestamp = new java.sql.Timestamp(todayCal.getTimeInMillis());
    String todayKey = formatDateKey(todayTimestamp);

    Set<String> cityNames = new LinkedHashSet<>();
    Set<String> genreNames = new LinkedHashSet<>();
    Set<String> dateKeys = new LinkedHashSet<>();
    List<java.sql.Timestamp> scheduleDates = new ArrayList<>();

    for (Centre centre : centres) {
        if (centre.getCity() != null && !centre.getCity().trim().isEmpty()) {
            cityNames.add(centre.getCity().trim());
        }
    }

    for (Movie m : movies) {
        if (m.getGenre() != null && !m.getGenre().trim().isEmpty()) {
            String[] parts = m.getGenre().split("[,/·•]");
            for (String p : parts) {
                String g = p.trim();
                if (!g.isEmpty()) {
                    genreNames.add(g);
                }
            }
        }
    }

    // Chỉ thu thập các ngày có suất chiếu từ HÔM NAY trở đi (không xem được hôm qua, hôm kia)
    for (ShowtimeView showtime : showtimes) {
        String dateKey = formatDateKey(showtime.getStartTime());
        if (dateKey.compareTo(todayKey) >= 0) {
            if (dateKeys.add(dateKey)) {
                scheduleDates.add(showtime.getStartTime());
            }
        }
    }

    // Đảm bảo HÔM NAY luôn có trong danh sách lựa chọn
    if (!dateKeys.contains(todayKey)) {
        scheduleDates.add(0, todayTimestamp);
        dateKeys.add(todayKey);
    }

    // Sắp xếp ngày tăng dần từ Hôm nay trở đi
    Collections.sort(scheduleDates, (d1, d2) -> d1.compareTo(d2));

    int requestedCentreId = -1;
    try {
        String cParam = request.getParameter("centreId");
        if (cParam != null && !cParam.trim().isEmpty()) {
            requestedCentreId = Integer.parseInt(cParam.trim());
        }
    } catch (Exception ignored) {
        requestedCentreId = -1;
    }
    Centre requestedCentre = null;
    for (Centre centre : centres) {
        if (centre.getCentreId() == requestedCentreId) {
            requestedCentre = centre;
            break;
        }
    }

    int requestedMovieId = -1;
    try {
        requestedMovieId = Integer.parseInt(request.getParameter("movieId"));
    } catch (Exception ignored) {
        requestedMovieId = -1;
    }

    if (requestedMovieId > 0) {
        boolean reqFound = false;
        for (Movie m : movies) {
            if (m.getMovieId() == requestedMovieId) {
                reqFound = true;
                break;
            }
        }
        if (!reqFound) {
            Movie reqMovie = cinemaDAO.getMovieById(requestedMovieId);
            if (reqMovie != null) movies.add(reqMovie);
        }
    }

    // Yêu cầu:
    // 1. Phim mặc định: nếu có param movieId thì mặc định là phim đó, ngược lại lấy phim đầu tiên
    int selectedMovieId = requestedMovieId > 0 ? requestedMovieId : (!movies.isEmpty() ? movies.get(0).getMovieId() : -1);

    // 2. Ngày tháng mặc định: BẮT BUỘC LÀ NGÀY HÔM NAY
    String selectedDate = todayKey;

    // 3. Thành phố mặc định: BẮT BUỘC LÀ "TẤT CẢ THÀNH PHỐ" ("") khi chuyển tới booking
    String selectedCity = request.getParameter("city") != null ? request.getParameter("city").trim() : "";

    // 4. Cụm rạp mặc định: lấy rạp đầu tiên phù hợp
    int selectedCentreId = requestedCentre != null ? requestedCentre.getCentreId() : (!centres.isEmpty() ? centres.get(0).getCentreId() : -1);
%>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta name="theme-color" content="#0B0F17">
        <title>CineFlow - Mua vé xem phim</title>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/layout.css?v=<%= System.currentTimeMillis() %>">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/booking/booking.css?v=<%= System.currentTimeMillis() %>">
    </head>
    <body>
        <div class="page-shell">
            <!-- Thanh điều hướng và đầu trang chung -->
            <jsp:include page="/common/header.jsp">
                <jsp:param name="activeNav" value="booking"/>
            </jsp:include>

            <main class="container" id="booking">
                <!-- ========================================================= -->
                <!-- 1. BỘ CHỌN NGÀY CHIẾU (TOP DATE BAR)                     -->
                <!-- ========================================================= -->
                <section class="date-section" aria-label="Chọn ngày chiếu">
                    <div class="month-label" id="bookingMonth">
                        <%= scheduleDates.isEmpty() ? "Tháng 10, 2026" : formatDateMonthYearVi(scheduleDates.get(0)) %>
                    </div>
                    <div class="date-carousel-wrap">
                        <button class="date-nav-btn prev" id="btnDatePrev" type="button" aria-label="Ngày trước đó" title="Trước">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M15 18l-6-6 6-6"/></svg>
                        </button>
                        <div class="dates" id="bookingDates">
                            <% for (java.sql.Timestamp scheduleDate : scheduleDates) {
                                String dateKey = formatDateKey(scheduleDate);
                                String dayOfWeek = new SimpleDateFormat("u").format(scheduleDate);
                                String dayNameVi = formatDayOfWeekVi(scheduleDate, todayKey);
                                String dayNum = new SimpleDateFormat("dd").format(scheduleDate);
                                boolean isToday = dateKey.equals(todayKey);
                                String dateClass = "date" + (dateKey.equals(selectedDate) ? " active" : "")
                                        + (isToday ? " is-today" : "")
                                        + ("6".equals(dayOfWeek) ? " saturday" : "")
                                        + ("7".equals(dayOfWeek) ? " sunday" : "");
                            %>
                            <button class="<%= dateClass %>" type="button"
                                    data-date="<%= dateKey %>"
                                    data-month="<%= formatDateMonthYearVi(scheduleDate) %>"
                                    title="<%= dayNameVi %>, <%= new SimpleDateFormat("dd/MM/yyyy").format(scheduleDate) %>">
                                <span class="day-name"><%= dayNameVi %></span>
                                <span class="day-number"><%= dayNum %></span>
                            </button>
                            <% } %>
                        </div>
                        <button class="date-nav-btn next" id="btnDateNext" type="button" aria-label="Ngày tiếp theo" title="Sau">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18l6-6-6-6"/></svg>
                        </button>
                    </div>
                </section>

                <!-- ========================================================= -->
                <!-- 2. BỐ CỤC 2 CỘT: CỤM RẠP & PHIM (MIDDLE 2-COLUMNS)         -->
                <!-- ========================================================= -->
                <div class="booking-content" id="bookingContent"
                     data-customer="<%= isCustomer %>"
                     data-initial-date="<%= selectedDate %>"
                     data-initial-city="<%= escapeHtml(selectedCity) %>"
                     data-initial-centre="<%= selectedCentreId %>"
                     data-initial-movie="<%= selectedMovieId %>">

                    <!-- CỘT TRÁI: CỤM RẠP -->
                    <section class="cinema-panel" aria-labelledby="cinema-title">
                        <div class="column-header">
                            <h2 class="column-title" id="cinema-title">Cụm rạp</h2>
                            <a id="bookingCinemaLoginLink"
                               href="<%= contextPath %>/auth/login.jsp"
                               class="btn-cinema-login"
                               hidden
                               title="Đăng nhập để đặt vé">
                                <span>Đăng nhập</span>
                            </a>
                        </div>
                        <div class="column-filter-bar">
                            <label for="citySelect" class="filter-label">Thành phố</label>
                            <select id="citySelect" class="cf-filter-select" aria-label="Lọc theo thành phố">
                                <option value="">Tất cả thành phố</option>
                                <% for (String city : cityNames) { %>
                                <option value="<%= escapeHtml(city) %>" <%= city.equalsIgnoreCase(selectedCity) ? "selected" : "" %>><%= escapeHtml(city) %></option>
                                <% } %>
                            </select>
                        </div>
                        <div class="centre-list" id="bookingCentres">
                            <% for (Centre centre : centres) {
                                boolean isSelected = centre.getCentreId() == selectedCentreId;
                            %>
                            <button class="centre-row<%= isSelected ? " active" : "" %>"
                                    type="button"
                                    data-centre-id="<%= centre.getCentreId() %>"
                                    data-city="<%= escapeHtml(centre.getCity()) %>"
                                    data-name="<%= escapeHtml(centre.getCentreName()) %>">
                                <span class="centre-name"><%= escapeHtml(centre.getCentreName()) %></span>
                                <span class="centre-city"><%= escapeHtml(centre.getCity()) %></span>
                            </button>
                            <% } %>
                            <% if (centres.isEmpty()) { %>
                            <p class="empty-state">Chưa có trung tâm chiếu phim đang hoạt động.</p>
                            <% } %>
                            <p class="empty-state" id="bookingCentreEmpty" hidden>Không có cụm rạp nào tại thành phố này.</p>
                        </div>
                    </section>

                    <!-- CỘT PHẢI: PHIM -->
                    <section class="movie-panel" aria-labelledby="movie-title">
                        <div class="column-header">
                            <h2 class="column-title" id="movie-title">Phim</h2>
                            <div style="display: flex; align-items: center; gap: 10px;">
                                <label for="genreSelect" class="filter-label">Thể loại</label>
                                <select id="genreSelect" class="cf-filter-select" aria-label="Lọc theo thể loại phim">
                                    <option value="">Tất cả thể loại</option>
                                    <% for (String genre : genreNames) { %>
                                    <option value="<%= escapeHtml(genre) %>"><%= escapeHtml(genre) %></option>
                                    <% } %>
                                </select>
                            </div>
                        </div>
                        <div class="movie-list" id="bookingMovies">
                            <% for (Movie movie : movies) {
                                String rating = movie.getAgeRating() == null ? "P" : movie.getAgeRating().toUpperCase().trim();
                                String ratingClass = "age-p";
                                if ("T13".equalsIgnoreCase(rating)) ratingClass = "age-t13";
                                else if ("T16".equalsIgnoreCase(rating)) ratingClass = "age-t16";
                                else if ("T18".equalsIgnoreCase(rating)) ratingClass = "age-t18";
                                else if ("K".equalsIgnoreCase(rating)) ratingClass = "age-k";

                                String genreStr = movie.getGenre() != null ? movie.getGenre() : "Phim chiếu rạp";
                                int duration = movie.getDuration() > 0 ? movie.getDuration() : 120;
                                boolean isSelected = movie.getMovieId() == selectedMovieId;
                            %>
                            <button class="movie-row<%= isSelected ? " active" : "" %>"
                                    type="button"
                                    data-movie-id="<%= movie.getMovieId() %>"
                                    data-genres="<%= escapeHtml(genreStr) %>"
                                    data-status="<%= movie.getStatus() %>"
                                    data-title="<%= escapeHtml(movie.getTitle()) %>">
                                <span class="movie-age-circle <%= ratingClass %>"><%= escapeHtml(rating) %></span>
                                <div class="movie-row-info">
                                    <div class="movie-title"><%= escapeHtml(movie.getTitle()) %></div>
                                    <div class="movie-meta"><%= escapeHtml(genreStr) %> · <%= duration %> phút</div>
                                </div>
                            </button>
                            <% } %>
                            <% if (movies.isEmpty()) { %>
                            <p class="empty-state">Chưa có phim nào trong danh mục.</p>
                            <% } %>
                            <p class="empty-state" id="bookingMovieEmpty" hidden>Không có phim nào theo thể loại đã chọn.</p>
                        </div>
                    </section>
                </div>

                <!-- ========================================================= -->
                <!-- 3. SUẤT CHIẾU DƯỚI CÙNG (BOTTOM SHOWTIMES FULL WIDTH)     -->
                <!-- ========================================================= -->
                <section class="booking-showtimes" id="bookingShowtimesSection" aria-labelledby="showtimes-title">
                    <div class="showtimes-header">
                        <h2 class="showtimes-title" id="showtimes-title">Suất chiếu</h2>
                    </div>

                    <!-- Lưới nút giờ chiếu -->
                    <div class="showtimes-grid" id="bookingShowtimes">
                        <% for (ShowtimeView showtime : showtimes) {
                            String showDateKey = formatDateKey(showtime.getStartTime());
                            if (showDateKey.compareTo(todayKey) < 0) continue;
                        %>
                        <button class="showtime-pill" type="button"
                                data-showtime-id="<%= showtime.getShowtimeId() %>"
                                data-date="<%= showDateKey %>"
                                data-centre-id="<%= showtime.getCentreId() %>"
                                data-city="<%= escapeHtml(showtime.getCity()) %>"
                                data-movie-id="<%= showtime.getMovieId() %>"
                                data-time="<%= formatTime(showtime.getStartTime()) %>"
                                data-hall="<%= escapeHtml(showtime.getHallName()) %>"
                                data-price="<%= formatMoney(showtime.getBaseTicketPrice()) %>">
                            <%= formatTime(showtime.getStartTime()) %>
                        </button>
                        <% } %>
                    </div>

                    <!-- Trạng thái không có suất chiếu -->
                    <div class="empty-state" id="bookingShowtimeEmpty" hidden>
                        Không có suất chiếu phù hợp với lựa chọn này. Vui lòng chọn ngày khác hoặc cụm rạp khác.
                    </div>

                    <!-- Dải xác nhận & hành động -->
                    <div class="showtime-action-strip">
                        <div class="showtime-action-info">
                            <span id="bookingMessage">Chọn một suất chiếu để tiếp tục.</span>
                        </div>
                    </div>
                </section>
            </main>

            <!-- ========================================================= -->
            <!-- JAVASCRIPT ĐIỀU KHIỂN BỘ LỌC VÀ TƯƠNG TÁC                -->
            <!-- ========================================================= -->
            <script>
                (function () {
                    const content = document.getElementById("bookingContent");
                    const dateButtons = Array.from(document.querySelectorAll(".date[data-date]"));
                    const citySelect = document.getElementById("citySelect");
                    const genreSelect = document.getElementById("genreSelect");
                    const centreButtons = Array.from(document.querySelectorAll(".centre-row[data-centre-id]"));
                    const movieButtons = Array.from(document.querySelectorAll(".movie-row[data-movie-id]"));
                    const showtimeButtons = Array.from(document.querySelectorAll(".showtime-pill[data-showtime-id]"));
                    const bookingMonth = document.getElementById("bookingMonth");
                    const centreEmpty = document.getElementById("bookingCentreEmpty");
                    const movieEmpty = document.getElementById("bookingMovieEmpty");
                    const showtimeEmpty = document.getElementById("bookingShowtimeEmpty");
                    const message = document.getElementById("bookingMessage");
                    const loginLink = document.getElementById("bookingLoginLink");
                    const cinemaLoginLink = document.getElementById("bookingCinemaLoginLink");
                    const btnProceed = document.getElementById("btnProceedBooking");

                    let selectedDate = content.dataset.initialDate || "";
                    let selectedCity = content.dataset.initialCity || "";
                    let selectedGenre = "";
                    let selectedCentreId = content.dataset.initialCentre || "";
                    let selectedMovieId = content.dataset.initialMovie || "";
                    let selectedShowtimeId = "";
                    const isCustomer = content.dataset.customer === "true";

                    function normalize(value) {
                        if (!value)
                            return "";
                        return value.normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/\u0111/g, "d").replace(/\u0110/g, "D").toLowerCase().trim();
                    }

                    function escapeHtmlText(str) {
                        if (!str)
                            return "";
                        return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
                    }

                    function render() {
                        // 1. CẬP NHẬT CÁC NÚT NGÀY
                        dateButtons.forEach((button) => {
                            const isSelected = button.dataset.date === selectedDate;
                            button.classList.toggle("active", isSelected);
                            if (selectedMovieId && selectedCentreId) {
                                const hasShows = showtimeButtons.some((s) =>
                                    s.dataset.date === button.dataset.date &&
                                            s.dataset.movieId === selectedMovieId &&
                                            s.dataset.centreId === selectedCentreId
                                );
                                button.classList.toggle("no-shows", !hasShows);
                            } else {
                                button.classList.remove("no-shows");
                            }
                        });

                        const activeDateBtn = dateButtons.find((button) => button.dataset.date === selectedDate);
                        if (activeDateBtn && activeDateBtn.dataset.month && bookingMonth) {
                            bookingMonth.textContent = activeDateBtn.dataset.month;
                        }

                        // 2. LỌC DANH SÁCH CỤM RẠP THEO CITY
                        let visibleCentres = 0;
                        centreButtons.forEach((button) => {
                            const centreCity = button.dataset.city || "";
                            const matchCity = !selectedCity || normalize(centreCity) === normalize(selectedCity);
                            button.style.display = matchCity ? "" : "none";
                            if (matchCity)
                                visibleCentres++;
                        });
                        if (centreEmpty)
                            centreEmpty.hidden = visibleCentres > 0;

                        // Nếu rạp đang chọn bị ẩn theo thành phố mới, tự động chọn rạp đầu tiên hiển thị
                        let currentCentre = centreButtons.find((b) => b.dataset.centreId === selectedCentreId && b.style.display !== "none");
                        if (!currentCentre && visibleCentres > 0) {
                            currentCentre = centreButtons.find((b) => b.style.display !== "none");
                            selectedCentreId = currentCentre ? currentCentre.dataset.centreId : "";
                        }
                        centreButtons.forEach((b) => b.classList.toggle("active", b === currentCentre));

                        // 3. LỌC DANH SÁCH PHIM THEO GENRE
                        let visibleMovies = 0;
                        movieButtons.forEach((button) => {
                            const movieGenres = button.dataset.genres || "";
                            const matchGenre = !selectedGenre || normalize(movieGenres).indexOf(normalize(selectedGenre)) !== -1;
                            button.style.display = matchGenre ? "" : "none";
                            if (matchGenre)
                                visibleMovies++;
                        });
                        if (movieEmpty)
                            movieEmpty.hidden = visibleMovies > 0;

                        // Nếu phim đang chọn bị ẩn theo thể loại mới, tự động chọn phim đầu tiên hiển thị
                        let currentMovie = movieButtons.find((b) => b.dataset.movieId === selectedMovieId && b.style.display !== "none");
                        if (!currentMovie && visibleMovies > 0) {
                            currentMovie = movieButtons.find((b) => b.style.display !== "none");
                            selectedMovieId = currentMovie ? currentMovie.dataset.movieId : "";
                        }
                        movieButtons.forEach((b) => b.classList.toggle("active", b === currentMovie));

                        // 4. LỌC SUẤT CHIẾU THEO (NGÀY, RẠP, PHIM)
                        const matchingShowtimes = showtimeButtons.filter((showtime) =>
                            showtime.dataset.date === selectedDate &&
                                    showtime.dataset.centreId === selectedCentreId &&
                                    showtime.dataset.movieId === selectedMovieId
                        );

                        if (!matchingShowtimes.some((s) => s.dataset.showtimeId === selectedShowtimeId)) {
                            selectedShowtimeId = "";
                        }

                        showtimeButtons.forEach((button) => {
                            const isMatch = matchingShowtimes.includes(button);
                            button.style.display = isMatch ? "" : "none";
                            button.classList.toggle("active", button.dataset.showtimeId === selectedShowtimeId);
                        });

                        const centreName = currentCentre ? currentCentre.dataset.name : "CineFlow";
                        const movieTitle = currentMovie ? currentMovie.dataset.title : "Chọn phim";

                        // 6. XỬ LÝ TRẠNG THÁI RỖNG / THÔNG BÁO
                        if (matchingShowtimes.length === 0) {
                            showtimeEmpty.hidden = false;
                            const isComingSoon = currentMovie && currentMovie.dataset.status === "COMING_SOON";
                            if (isComingSoon) {
                                showtimeEmpty.innerHTML = "Phim <strong>" + escapeHtmlText(movieTitle) + "</strong> là phim sắp chiếu và hiện chưa mở bán suất chiếu. Vui lòng quay lại sau.";
                            } else {
                                showtimeEmpty.innerHTML = "Không có suất chiếu cho phim <strong>" + escapeHtmlText(movieTitle) + "</strong> tại <strong>" + escapeHtmlText(centreName) + "</strong> vào ngày đã chọn. Vui lòng chọn ngày khác hoặc cụm rạp khác.";
                            }
                        } else {
                            showtimeEmpty.hidden = true;
                        }

                        // 7. XỬ LÝ NÚT HÀNH ĐỘNG
                        const activeShowtime = showtimeButtons.find((b) => b.dataset.showtimeId === selectedShowtimeId);
                        if (activeShowtime) {
                            const details = [activeShowtime.dataset.time, activeShowtime.dataset.hall, activeShowtime.dataset.price].filter(Boolean).join(" · ");
                            message.innerHTML = "Đã chọn suất chiếu: <strong>" + details + "</strong>.";
                            if (!isCustomer) {
                                const redirectUrl = "<%= contextPath %>/auth/login.jsp?redirect=" + encodeURIComponent(
                                        "<%= contextPath %>/booking/booking.jsp?movieId=" + selectedMovieId + "&date=" + selectedDate + (selectedCity ? "&city=" + encodeURIComponent(selectedCity) : "") + (selectedCentreId ? "&centreId=" + selectedCentreId : "")
                                        );
                                if (loginLink) {
                                    loginLink.href = redirectUrl;
                                    loginLink.hidden = false;
                                }
                                if (cinemaLoginLink) {
                                    cinemaLoginLink.href = redirectUrl;
                                    cinemaLoginLink.hidden = false;
                                }
                                if (btnProceed)
                                    btnProceed.style.display = "none";
                            } else {
                                if (loginLink)
                                    loginLink.hidden = true;
                                if (cinemaLoginLink)
                                    cinemaLoginLink.hidden = true;
                                if (btnProceed)
                                    btnProceed.style.display = "inline-flex";
                            }
                        } else {
                            message.textContent = "Chọn một suất chiếu để tiếp tục.";
                            if (loginLink)
                                loginLink.hidden = true;
                            if (cinemaLoginLink)
                                cinemaLoginLink.hidden = true;
                            if (btnProceed)
                                btnProceed.style.display = "none";
                        }
                    }

                    // GẮN SỰ KIỆN:
                    // 1. Dropdown Thành phố
                    if (citySelect) {
                        citySelect.addEventListener("change", function () {
                            selectedCity = this.value;
                            render();
                        });
                    }

                    // 2. Dropdown Thể loại
                    if (genreSelect) {
                        genreSelect.addEventListener("change", function () {
                            selectedGenre = this.value;
                            render();
                        });
                    }

                    // 3. Chọn Rạp
                    centreButtons.forEach((button) => {
                        button.addEventListener("click", function () {
                            selectedCentreId = this.dataset.centreId;
                            render();
                        });
                    });

                    // 4. Chọn Phim
                    movieButtons.forEach((button) => {
                        button.addEventListener("click", function () {
                            selectedMovieId = this.dataset.movieId;
                            render();
                        });
                    });

                    // 5. Chọn Ngày
                    dateButtons.forEach((button) => {
                        button.addEventListener("click", function () {
                            selectedDate = this.dataset.date;
                            render();
                            this.scrollIntoView({behavior: "smooth", inline: "center", block: "nearest"});
                        });
                    });

                    // 6. Điều khiển thanh trượt chọn ngày (Nút Trượt Trái / Trượt Phải)
                    const bookingDates = document.getElementById("bookingDates");
                    const btnDatePrev = document.getElementById("btnDatePrev");
                    const btnDateNext = document.getElementById("btnDateNext");

                    function updateNavBtnState() {
                        if (!bookingDates || !btnDatePrev || !btnDateNext)
                            return;
                        const sl = bookingDates.scrollLeft;
                        const maxScroll = Math.max(0, bookingDates.scrollWidth - bookingDates.clientWidth - 2);
                        const isAtStart = sl <= 2;
                        const isAtEnd = sl >= maxScroll;

                        btnDatePrev.disabled = isAtStart;
                        btnDatePrev.classList.toggle("disabled", isAtStart);

                        btnDateNext.disabled = isAtEnd;
                        btnDateNext.classList.toggle("disabled", isAtEnd);
                    }

                    if (btnDatePrev && bookingDates) {
                        btnDatePrev.addEventListener("click", function () {
                            bookingDates.scrollBy({left: -260, behavior: "smooth"});
                        });
                    }

                    if (btnDateNext && bookingDates) {
                        btnDateNext.addEventListener("click", function () {
                            bookingDates.scrollBy({left: 260, behavior: "smooth"});
                        });
                    }

                    function updateVisibleMonth() {
                        if (!bookingDates || !bookingMonth)
                            return;
                        const containerLeft = bookingDates.getBoundingClientRect().left;
                        for (let i = 0; i < dateButtons.length; i++) {
                            const rect = dateButtons[i].getBoundingClientRect();
                            if (rect.right > containerLeft + 30) {
                                if (dateButtons[i].dataset.month) {
                                    bookingMonth.textContent = dateButtons[i].dataset.month;
                                }
                                break;
                            }
                        }
                    }

                    if (bookingDates) {
                        bookingDates.addEventListener("scroll", function () {
                            updateNavBtnState();
                            updateVisibleMonth();
                        }, {passive: true});
                        window.addEventListener("resize", function () {
                            updateNavBtnState();
                            updateVisibleMonth();
                        });
                    }

                    // 7. Chọn Suất chiếu
                    showtimeButtons.forEach((button) => {
                        button.addEventListener("click", function () {
                            selectedShowtimeId = this.dataset.showtimeId;
                            render();
                        });
                    });

                    // 8. Nút Tiếp tục đặt vé
                    if (btnProceed) {
                        btnProceed.addEventListener("click", function () {
                            if (!selectedShowtimeId) {
                                alert("Vui lòng chọn một suất chiếu!");
                                return;
                            }
                            const activeShowtime = showtimeButtons.find((b) => b.dataset.showtimeId === selectedShowtimeId);
                            const time = activeShowtime ? activeShowtime.dataset.time : "";
                            const hall = activeShowtime ? activeShowtime.dataset.hall : "";
                            const price = activeShowtime ? activeShowtime.dataset.price : "";
                            alert("Bạn đã chọn suất chiếu: " + time + " (" + hall + ") - Giá vé: " + price + ".\nHệ thống đang chuẩn bị mở sơ đồ ghế ngồi!");
                        });
                    }

                    // Khởi chạy hiển thị ban đầu
                    render();

                    // Cuộn ngày hiện tại vào giữa tầm nhìn và khởi tạo trạng thái nút trượt
                    const initialActiveDate = dateButtons.find((b) => b.dataset.date === selectedDate);
                    if (initialActiveDate && bookingDates) {
                        initialActiveDate.scrollIntoView({behavior: "smooth", inline: "center", block: "nearest"});
                    }
                    setTimeout(updateNavBtnState, 200);
                }());
            </script>

            <!-- Chân trang chung -->
            <jsp:include page="/common/footer.jsp"/>
        </div>
    </body>
</html>
