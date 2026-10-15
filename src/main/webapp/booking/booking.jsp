<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.User" %>
<%@ page import="model.Movie" %>
<%@ page import="model.Centre" %>
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
    Set<String> cityNames = new LinkedHashSet<>();
    Set<String> dateKeys = new LinkedHashSet<>();
    List<java.sql.Timestamp> scheduleDates = new ArrayList<>();
    for (Centre centre : centres) {
        if (centre.getCity() != null && !centre.getCity().trim().isEmpty()) cityNames.add(centre.getCity());
    }
    for (ShowtimeView showtime : showtimes) {
        String dateKey = formatDateKey(showtime.getStartTime());
        if (dateKeys.add(dateKey)) scheduleDates.add(showtime.getStartTime());
    }
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

    // Đảm bảo phim được yêu cầu có mặt trong danh sách phim
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
    int nowShowingCount = 0;
    int comingSoonCount = 0;
    for (Movie m : movies) {
        if ("COMING_SOON".equalsIgnoreCase(m.getStatus())) {
            comingSoonCount++;
        } else {
            nowShowingCount++;
        }
    }

    ShowtimeView initialShowtime = null;
    if (requestedMovieId > 0) {
        if (requestedCentreId > 0) {
            for (ShowtimeView showtime : showtimes) {
                if (showtime.getMovieId() == requestedMovieId && showtime.getCentreId() == requestedCentreId) {
                    initialShowtime = showtime;
                    break;
                }
            }
        }
        if (initialShowtime == null) {
            for (ShowtimeView showtime : showtimes) {
                if (showtime.getMovieId() == requestedMovieId) {
                    initialShowtime = showtime;
                    break;
                }
            }
        }
    } else if (requestedCentreId > 0) {
        for (ShowtimeView showtime : showtimes) {
            if (showtime.getCentreId() == requestedCentreId) {
                initialShowtime = showtime;
                break;
            }
        }
    }

    if (initialShowtime == null && !showtimes.isEmpty()) {
        initialShowtime = showtimes.get(0);
    }

    int selectedMovieId = requestedMovieId > 0 ? requestedMovieId : (initialShowtime != null ? initialShowtime.getMovieId() : (!movies.isEmpty() ? movies.get(0).getMovieId() : -1));
    int selectedCentreId = -1;
    String selectedCity = "";
    String selectedDate = "";

    if (requestedMovieId > 0 && initialShowtime != null && initialShowtime.getMovieId() == requestedMovieId) {
        selectedCentreId = requestedCentre != null ? requestedCentre.getCentreId() : initialShowtime.getCentreId();
        selectedCity = requestedCentre != null ? requestedCentre.getCity() : initialShowtime.getCity();
        selectedDate = formatDateKey(initialShowtime.getStartTime());
    } else {
        selectedCentreId = requestedCentre != null ? requestedCentre.getCentreId() : (initialShowtime != null ? initialShowtime.getCentreId() : (!centres.isEmpty() ? centres.get(0).getCentreId() : -1));
        selectedCity = requestedCentre != null ? requestedCentre.getCity() : (initialShowtime != null ? initialShowtime.getCity() : "");
        selectedDate = initialShowtime != null ? formatDateKey(initialShowtime.getStartTime()) : (!scheduleDates.isEmpty() ? formatDateKey(scheduleDates.get(0)) : "");
    }
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
             data-initial-centre="<%= selectedCentreId %>"
             data-initial-movie="<%= selectedMovieId %>">
            <section class="cinema-panel" aria-labelledby="cinema-title">
                <h1 class="panel-title" id="cinema-title">Rạp</h1>
                <div class="login-row">
                    <% if (signedInUser == null) { %>
                        <div><strong>Rạp chiếu phim của tôi</strong><span>Bạn có thể kiểm tra sau khi đăng nhập</span></div>
                        <a href="<%= contextPath %>/auth/login.jsp">Đăng nhập</a>
                    <% } else { %>
                        <div><strong><%= escapeHtml(memberName) %></strong><span>Chọn rạp và suất chiếu phù hợp</span></div>
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
                        <% for (Centre centre : centres) { %>
                            <button class="centre<%= centre.getCentreId() == selectedCentreId ? " active" : "" %>" type="button" data-centre-id="<%= centre.getCentreId() %>" data-city="<%= escapeHtml(centre.getCity()) %>" data-name="<%= escapeHtml(centre.getCentreName()) %>"><%= escapeHtml(centre.getCentreName()) %></button>
                        <% } %>
                        <% if (centres.isEmpty()) { %><p class="empty-state">Chưa có trung tâm chiếu phim đang hoạt động.</p><% } %>
                    </div>
                </div>
            </section>

            <section class="movie-panel" aria-labelledby="movie-title">
                <h2 class="panel-title" id="movie-title">Phim</h2>
                <div class="movie-tab-filter" role="tablist" aria-label="Bộ lọc loại phim">
                    <button class="movie-tab-btn active" type="button" data-filter="all">
                        <span class="tab-text">Tất cả</span>
                        <span class="tab-count"><%= movies.size() %></span>
                    </button>
                    <button class="movie-tab-btn" type="button" data-filter="NOW_SHOWING">
                        <span class="tab-text">Đang chiếu</span>
                        <span class="tab-count"><%= nowShowingCount %></span>
                    </button>
                    <button class="movie-tab-btn" type="button" data-filter="COMING_SOON">
                        <span class="tab-text">Sắp chiếu</span>
                        <span class="tab-count"><%= comingSoonCount %></span>
                    </button>
                </div>
                <div class="movie-list" id="bookingMovies">
                    <% for (Movie movie : movies) {
                        String rating = movie.getAgeRating() == null ? "" : movie.getAgeRating().toUpperCase();
                        String ratingClass = "";
                        if ("T13".equals(rating)) ratingClass = " age-13";
                        else if ("T16".equals(rating)) ratingClass = " age-16";
                        else if ("T18".equals(rating)) ratingClass = " age-18";
                        else if ("P".equals(rating) || "K".equals(rating)) ratingClass = " age-p";
                        boolean isSoon = "COMING_SOON".equalsIgnoreCase(movie.getStatus());
                    %>
                        <button class="movie-item<%= movie.getMovieId() == selectedMovieId ? " active" : "" %>"
                                type="button"
                                data-movie-id="<%= movie.getMovieId() %>"
                                data-status="<%= isSoon ? "COMING_SOON" : "NOW_SHOWING" %>"
                                data-title="<%= escapeHtml(movie.getTitle()) %>">
                            <span class="movie-age<%= ratingClass %>"><%= escapeHtml(movie.getAgeRating()) %></span>
                            <span class="movie-name"><%= escapeHtml(movie.getTitle()) %></span>
                            <% if (isSoon) { %>
                                <span class="badge-coming-soon">Sắp chiếu</span>
                            <% } %>
                        </button>
                    <% } %>
                    <% if (movies.isEmpty()) { %><p class="empty-state">Chưa có phim.</p><% } %>
                    <p class="empty-state" id="bookingMovieEmpty" hidden>Không có phim nào theo bộ lọc đã chọn.</p>
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
            const centreButtons = Array.from(document.querySelectorAll(".centre[data-centre-id]"));
            const movieButtons = Array.from(document.querySelectorAll(".movie-item[data-movie-id]"));
            const movieFilterButtons = Array.from(document.querySelectorAll(".movie-tab-btn"));
            const showtimeButtons = Array.from(document.querySelectorAll(".showtime[data-showtime-id]"));
            const bookingMonth = document.getElementById("bookingMonth");
            const movieEmpty = document.getElementById("bookingMovieEmpty");
            const showtimeEmpty = document.getElementById("bookingShowtimeEmpty");
            const selectionInfo = document.getElementById("bookingSelectionInfo");
            const message = document.getElementById("bookingMessage");
            const loginLink = document.getElementById("bookingLoginLink");
            const dates = document.getElementById("bookingDates");

            let selectedDate = content.dataset.initialDate || "";
            let selectedCity = content.dataset.initialCity || "";
            let selectedCentreId = content.dataset.initialCentre || "";
            let selectedMovieId = content.dataset.initialMovie || "";
            let selectedShowtimeId = "";
            let currentMovieFilter = "all";
            const isCustomer = content.dataset.customer === "true";

            function normalize(value) {
                if (!value) return "";
                return value.normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/\u0111/g, "d").replace(/\u0110/g, "D").toLowerCase().trim();
            }

            function escapeHtmlText(str) {
                if (!str) return "";
                return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
            }

            // Nếu phim ban đầu là sắp chiếu, mặc định đặt bộ lọc về tất cả
            const initialMovie = movieButtons.find((m) => m.dataset.movieId === selectedMovieId);
            if (initialMovie && initialMovie.dataset.status === "COMING_SOON") {
                currentMovieFilter = "all";
            }

            function scrollActiveIntoView() {
                const activeMovie = document.querySelector('.movie-item[data-movie-id="' + selectedMovieId + '"]');
                if (activeMovie) {
                    activeMovie.scrollIntoView({ block: "nearest", behavior: "smooth" });
                }
                const activeDate = document.querySelector('.date[data-date="' + selectedDate + '"]');
                if (activeDate) {
                    activeDate.scrollIntoView({ inline: "center", block: "nearest", behavior: "smooth" });
                }
            }

            function render() {
                // 1. Các nút chọn ngày
                dateButtons.forEach((button) => {
                    button.classList.toggle("active", button.dataset.date === selectedDate);
                    if (selectedMovieId) {
                        const dateHasShows = showtimeButtons.some((s) =>
                            s.dataset.date === button.dataset.date &&
                            s.dataset.movieId === selectedMovieId
                        );
                        button.classList.toggle("no-shows", !dateHasShows);
                    } else {
                        button.classList.remove("no-shows");
                    }
                });
                const activeDate = dateButtons.find((button) => button.dataset.date === selectedDate);
                if (activeDate && activeDate.dataset.month) {
                    bookingMonth.textContent = activeDate.dataset.month;
                }

                // 2. Các tab thành phố
                cityButtons.forEach((button) => {
                    const btnCity = button.dataset.city || "";
                    button.classList.toggle("active", normalize(btnCity) === normalize(selectedCity));
                    if (selectedMovieId && btnCity) {
                        const cityHasShows = showtimeButtons.some((s) =>
                            normalize(s.dataset.city || "") === normalize(btnCity) &&
                            s.dataset.movieId === selectedMovieId &&
                            (!selectedDate || s.dataset.date === selectedDate)
                        );
                        button.classList.toggle("no-shows", !cityHasShows);
                    } else {
                        button.classList.remove("no-shows");
                    }
                });

                // 3. Các nút chọn cụm rạp
                centreButtons.forEach((button) => {
                    const matchCity = !selectedCity || normalize(button.dataset.city || "") !== normalize(selectedCity) ? false : true;
                    button.hidden = !matchCity;

                    if (selectedMovieId) {
                        const centreHasShows = showtimeButtons.some((s) =>
                            s.dataset.centreId === button.dataset.centreId &&
                            s.dataset.movieId === selectedMovieId &&
                            s.dataset.date === selectedDate
                        );
                        button.classList.toggle("has-shows", centreHasShows);
                        button.classList.toggle("no-shows", !centreHasShows);
                    } else {
                        button.classList.remove("has-shows");
                        button.classList.remove("no-shows");
                    }
                });

                // Tự động chọn rạp tối ưu:
                // Nếu rạp hiện tại bị ẩn hoặc không có suất chiếu cho phim và ngày này,
                // ưu tiên tự động chọn rạp đầu tiên CÓ suất chiếu trong thành phố đã chọn!
                let activeCentre = centreButtons.find((button) => button.dataset.centreId === selectedCentreId && !button.hidden);
                const activeHasShows = activeCentre && showtimeButtons.some((s) =>
                    s.dataset.centreId === activeCentre.dataset.centreId &&
                    s.dataset.movieId === selectedMovieId &&
                    s.dataset.date === selectedDate
                );

                if ((!activeCentre || !activeHasShows) && Boolean(selectedMovieId)) {
                    const firstAvailableInCity = centreButtons.find((button) =>
                        !button.hidden &&
                        showtimeButtons.some((s) =>
                            s.dataset.centreId === button.dataset.centreId &&
                            s.dataset.movieId === selectedMovieId &&
                            s.dataset.date === selectedDate
                        )
                    );
                    if (firstAvailableInCity) {
                        activeCentre = firstAvailableInCity;
                        selectedCentreId = activeCentre.dataset.centreId;
                    }
                }

                if (!activeCentre) {
                    activeCentre = centreButtons.find((button) => !button.hidden);
                    selectedCentreId = activeCentre ? activeCentre.dataset.centreId : "";
                }
                centreButtons.forEach((button) => button.classList.toggle("active", button === activeCentre));

                // 4. Tab lọc phim theo trạng thái
                movieFilterButtons.forEach((btn) => btn.classList.toggle("active", btn.dataset.filter === currentMovieFilter));

                // 5. Danh sách phim
                let visibleCount = 0;
                movieButtons.forEach((movie) => {
                    const matchFilter = (currentMovieFilter === "all" || movie.dataset.status === currentMovieFilter);
                    movie.hidden = !matchFilter;
                    movie.classList.toggle("active", movie.dataset.movieId === selectedMovieId);
                    if (matchFilter) visibleCount++;
                });
                if (movieEmpty) movieEmpty.hidden = visibleCount > 0;

                // 6. Các nút suất chiếu
                const matchingShowtimes = showtimeButtons.filter((showtime) =>
                    showtime.dataset.date === selectedDate &&
                    showtime.dataset.centreId === selectedCentreId &&
                    showtime.dataset.movieId === selectedMovieId
                );

                if (!matchingShowtimes.some((showtime) => showtime.dataset.showtimeId === selectedShowtimeId)) {
                    selectedShowtimeId = "";
                }

                showtimeButtons.forEach((button) => {
                    const isMatch = matchingShowtimes.includes(button);
                    button.hidden = !isMatch;
                    button.classList.toggle("active", button.dataset.showtimeId === selectedShowtimeId);
                });

                // 7. Trạng thái rỗng và thông tin tóm tắt đã chọn
                const activeMovie = movieButtons.find((button) => button.dataset.movieId === selectedMovieId);
                const activeMovieTitle = activeMovie ? activeMovie.dataset.title : "";
                const isComingSoon = activeMovie && activeMovie.dataset.status === "COMING_SOON";

                if (matchingShowtimes.length === 0) {
                    showtimeEmpty.hidden = false;
                    if (isComingSoon) {
                        showtimeEmpty.innerHTML = "Phim <strong>" + escapeHtmlText(activeMovieTitle) + "</strong> là phim sắp chiếu và hiện chưa mở bán suất chiếu. Vui lòng quay lại sau.";
                    } else if (activeMovieTitle) {
                        const altDateSlot = showtimeButtons.find((s) => s.dataset.movieId === selectedMovieId && s.dataset.centreId === selectedCentreId);
                        const altCentreSlot = showtimeButtons.find((s) => s.dataset.movieId === selectedMovieId && s.dataset.date === selectedDate);
                        let hint = "";
                        if (altDateSlot) {
                            hint = " (Phim có suất chiếu vào ngày khác tại rạp này)";
                        } else if (altCentreSlot) {
                            hint = " (Phim có suất chiếu tại cụm rạp khác)";
                        }
                        showtimeEmpty.innerHTML = "Không có suất chiếu cho phim <strong>" + escapeHtmlText(activeMovieTitle) + "</strong> tại rạp và ngày đã chọn." + hint;
                    } else {
                        showtimeEmpty.textContent = "Không có suất chiếu phù hợp với lựa chọn này.";
                    }
                } else {
                    showtimeEmpty.hidden = true;
                }

                const selectedShowtime = showtimeButtons.find((button) => button.dataset.showtimeId === selectedShowtimeId);
                const dateLabel = activeDate ? (activeDate.querySelector(".day-number").textContent + " " + activeDate.querySelector(".day-name").textContent) : "";
                selectionInfo.textContent = [selectedCity, activeCentre && activeCentre.dataset.name, activeMovieTitle, dateLabel].filter(Boolean).join(" / ") || "Chọn ngày, rạp và phim để xem giờ chiếu.";

                if (selectedShowtime) {
                    const details = [selectedShowtime.dataset.time, selectedShowtime.dataset.hall, selectedShowtime.dataset.price].filter(Boolean).join(" · ");
                    message.textContent = isCustomer ? ("Đã chọn " + details + ". Bước chọn ghế chưa được tích hợp.") : ("Đã chọn " + details + ". Đăng nhập để tiếp tục đặt vé.");
                    loginLink.href = "<%= contextPath %>/auth/login.jsp?redirect=" + encodeURIComponent(window.location.pathname + window.location.search);
                    loginLink.hidden = isCustomer;
                } else {
                    message.textContent = "Chọn một suất chiếu để tiếp tục.";
                    loginLink.hidden = true;
                }
            }

            // Lắng nghe sự kiện người dùng:
            // Xử lý chọn phim
            movieButtons.forEach((button) => {
                button.addEventListener("click", () => {
                    selectedMovieId = button.dataset.movieId;
                    selectedShowtimeId = "";

                    // Tự động căn chỉnh: Phim này có suất chiếu vào ngày và rạp đang chọn không?
                    const hasCurrentSlot = showtimeButtons.some((s) =>
                        s.dataset.movieId === selectedMovieId &&
                        s.dataset.centreId === selectedCentreId &&
                        s.dataset.date === selectedDate
                    );

                    if (!hasCurrentSlot) {
                        // 1. Ưu tiên tìm rạp khác trong CÙNG thành phố và CÙNG ngày hiện tại
                        const sameCitySameDate = showtimeButtons.find((s) =>
                            s.dataset.movieId === selectedMovieId &&
                            s.dataset.date === selectedDate &&
                            (!selectedCity || normalize(s.dataset.city || "") === normalize(selectedCity))
                        );
                        if (sameCitySameDate) {
                            selectedCentreId = sameCitySameDate.dataset.centreId;
                        } else {
                            // 2. Tìm ngày khác tại chính rạp đang chọn
                            const centreSlot = showtimeButtons.find((s) =>
                                s.dataset.movieId === selectedMovieId &&
                                s.dataset.centreId === selectedCentreId
                            );
                            if (centreSlot) {
                                selectedDate = centreSlot.dataset.date;
                            } else {
                                // 3. Tìm bất kỳ rạp/ngày nào trong cùng thành phố
                                const citySlot = showtimeButtons.find((s) =>
                                    s.dataset.movieId === selectedMovieId &&
                                    (!selectedCity || normalize(s.dataset.city || "") === normalize(selectedCity))
                                );
                                if (citySlot) {
                                    selectedDate = citySlot.dataset.date;
                                    selectedCentreId = citySlot.dataset.centreId;
                                } else {
                                    // 4. Mở rộng tìm toàn quốc nếu thành phố không có rạp chiếu
                                    const anySlot = showtimeButtons.find((s) => s.dataset.movieId === selectedMovieId);
                                    if (anySlot) {
                                        selectedDate = anySlot.dataset.date;
                                        selectedCentreId = anySlot.dataset.centreId;
                                        selectedCity = anySlot.dataset.city || "";
                                    }
                                }
                            }
                        }
                    }

                    render();
                    scrollActiveIntoView();
                });
            });

            // Xử lý chọn ngày
            dateButtons.forEach((button) => {
                button.addEventListener("click", () => {
                    selectedDate = button.dataset.date;
                    selectedShowtimeId = "";
                    render();
                });
            });

            // Xử lý chọn cụm rạp
            centreButtons.forEach((button) => {
                button.addEventListener("click", () => {
                    selectedCentreId = button.dataset.centreId;
                    selectedCity = button.dataset.city;
                    selectedShowtimeId = "";

                    // Nếu phim đang chọn có suất chiếu tại rạp này vào ngày khác, chuyển sang ngày đó
                    if (selectedMovieId) {
                        const hasDateSlot = showtimeButtons.some((s) =>
                            s.dataset.movieId === selectedMovieId &&
                            s.dataset.centreId === selectedCentreId &&
                            s.dataset.date === selectedDate
                        );
                        if (!hasDateSlot) {
                            const altDateSlot = showtimeButtons.find((s) =>
                                s.dataset.movieId === selectedMovieId &&
                                s.dataset.centreId === selectedCentreId
                            );
                            if (altDateSlot) {
                                selectedDate = altDateSlot.dataset.date;
                            }
                        }
                    }
                    render();
                    scrollActiveIntoView();
                });
            });

            // Xử lý chọn tab thành phố
            cityButtons.forEach((button) => {
                button.addEventListener("click", () => {
                    selectedCity = button.dataset.city;
                    selectedShowtimeId = "";
                    const currentCentreBtn = centreButtons.find((c) => c.dataset.centreId === selectedCentreId);
                    if (!currentCentreBtn || (selectedCity && normalize(currentCentreBtn.dataset.city || "") !== normalize(selectedCity))) {
                        // Nếu phim đang chọn có suất chiếu tại thành phố này, chọn rạp và ngày tương ứng
                        const movieSlotInCity = showtimeButtons.find((s) =>
                            s.dataset.movieId === selectedMovieId &&
                            (!selectedCity || normalize(s.dataset.city || "") === normalize(selectedCity))
                        );
                        if (movieSlotInCity) {
                            selectedCentreId = movieSlotInCity.dataset.centreId;
                            selectedDate = movieSlotInCity.dataset.date;
                        } else {
                            const firstCentreInCity = centreButtons.find((c) =>
                                !selectedCity || normalize(c.dataset.city || "") === normalize(selectedCity)
                            );
                            if (firstCentreInCity) {
                                selectedCentreId = firstCentreInCity.dataset.centreId;
                            }
                        }
                    }
                    render();
                    scrollActiveIntoView();
                });
            });

            // Tab lọc phân loại phim
            movieFilterButtons.forEach((btn) => {
                btn.addEventListener("click", () => {
                    currentMovieFilter = btn.dataset.filter;
                    render();
                });
            });

            // Chọn suất chiếu
            showtimeButtons.forEach((button) => {
                button.addEventListener("click", () => {
                    selectedShowtimeId = button.dataset.showtimeId;
                    render();
                });
            });

            // Cuộn ngang thanh chọn ngày
            document.querySelectorAll("[data-date-scroll]").forEach((button) => {
                button.addEventListener("click", () => {
                    dates.scrollBy({ left: button.dataset.dateScroll === "right" ? 240 : -240, behavior: "smooth" });
                });
            });

            // Khởi tạo hiển thị và cuộn tới mục đang chọn
            render();
            setTimeout(scrollActiveIntoView, 100);

            if (new URLSearchParams(window.location.search).get("view") === "schedule") {
                document.getElementById("booking-showtimes").scrollIntoView();
            }
        }());
    </script>

    <!-- Chân trang chung -->
    <jsp:include page="/common/footer.jsp"/>
</div>
</body>
</html>




