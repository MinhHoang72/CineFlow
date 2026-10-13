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
    int requestedMovieId = -1;
    try {
        requestedMovieId = Integer.parseInt(request.getParameter("movieId"));
    } catch (Exception ignored) {
        requestedMovieId = -1;
    }

    // Ensure requested movie is present in movies list
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
        if (requestedBranchId > 0) {
            for (ShowtimeView showtime : showtimes) {
                if (showtime.getMovieId() == requestedMovieId && showtime.getBranchId() == requestedBranchId) {
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
    } else if (requestedBranchId > 0) {
        for (ShowtimeView showtime : showtimes) {
            if (showtime.getBranchId() == requestedBranchId) {
                initialShowtime = showtime;
                break;
            }
        }
    }

    if (initialShowtime == null && !showtimes.isEmpty()) {
        initialShowtime = showtimes.get(0);
    }

    int selectedMovieId = requestedMovieId > 0 ? requestedMovieId : (initialShowtime != null ? initialShowtime.getMovieId() : (!movies.isEmpty() ? movies.get(0).getMovieId() : -1));
    int selectedBranchId = -1;
    String selectedCity = "";
    String selectedDate = "";

    if (requestedMovieId > 0 && initialShowtime != null && initialShowtime.getMovieId() == requestedMovieId) {
        selectedBranchId = requestedBranch != null ? requestedBranch.getBranchId() : initialShowtime.getBranchId();
        selectedCity = requestedBranch != null ? requestedBranch.getCity() : initialShowtime.getCity();
        selectedDate = formatDateKey(initialShowtime.getStartTime());
    } else {
        selectedBranchId = requestedBranch != null ? requestedBranch.getBranchId() : (initialShowtime != null ? initialShowtime.getBranchId() : (!branches.isEmpty() ? branches.get(0).getBranchId() : -1));
        selectedCity = requestedBranch != null ? requestedBranch.getCity() : (initialShowtime != null ? initialShowtime.getCity() : "");
        selectedDate = initialShowtime != null ? formatDateKey(initialShowtime.getStartTime()) : (!scheduleDates.isEmpty() ? formatDateKey(scheduleDates.get(0)) : "");
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#f0eee4">
    <title>CineFlow - Mua vé xem phim</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/layout.css?v=<%= System.currentTimeMillis() %>">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/booking/booking.css?v=<%= System.currentTimeMillis() %>">
    <style>
        /* Modern Cinema Movie Filter Pills (Lotte Cinema style) */
        .movie-tab-filter {
            display: flex;
            gap: 8px;
            padding: 12px 18px;
            background: #faf8f5;
            border-bottom: 1px solid #e5e1d7;
        }
        .movie-tab-btn {
            flex: 1;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            gap: 7px;
            padding: 8px 10px;
            border: 1px solid #dcd7cb;
            border-radius: 20px;
            background: #ffffff;
            color: #4b5563;
            font-family: inherit;
            font-size: 12.5px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
            outline: none;
            -webkit-appearance: none;
            appearance: none;
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
        }
        .movie-tab-btn:hover {
            border-color: #e21b23;
            color: #e21b23;
            background: #ffffff;
            transform: translateY(-1px);
            box-shadow: 0 3px 6px rgba(0, 0, 0, 0.06);
        }
        .movie-tab-btn.active {
            background: #e21b23 !important;
            border-color: #e21b23 !important;
            color: #ffffff !important;
            font-weight: 700 !important;
            box-shadow: 0 3px 10px rgba(226, 27, 35, 0.3) !important;
            transform: translateY(-1px);
        }
        .movie-tab-btn .tab-count {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            min-width: 19px;
            height: 19px;
            padding: 0 6px;
            border-radius: 999px;
            font-size: 11px;
            font-weight: 700;
            background: #f0eee8;
            color: #6b7280;
            line-height: 1;
            transition: all 0.2s;
        }
        .movie-tab-btn.active .tab-count {
            background: rgba(255, 255, 255, 0.28) !important;
            color: #ffffff !important;
        }
        .movie-tab-btn:hover:not(.active) .tab-count {
            background: #fee2e2;
            color: #e21b23;
        }

        /* Movie list and items */
        .movie-list { max-height: 460px; overflow-y: auto; scroll-behavior: smooth; }
        .movie-item {
            width: 100%;
            display: flex;
            align-items: center;
            gap: 12px;
            padding: 13px 20px;
            border: 0;
            border-bottom: 1px solid #f0eee9;
            border-left: 4px solid transparent;
            background: #fff;
            cursor: pointer;
            text-align: left;
            transition: all .15s ease;
            font-family: inherit;
            outline: none;
        }
        .movie-item:hover { background: #fbfaf7; }
        .movie-item.active { border-left: 4px solid #e21b23 !important; background: #fff5f5 !important; }
        .movie-item.active .movie-name { color: #e21b23 !important; font-weight: 700 !important; }
        .badge-coming-soon { font-size: 10px; font-weight: 700; color: #b45309; background: #fef3c7; border: 1px solid #fde68a; padding: 2px 7px; border-radius: 999px; margin-left: auto; flex-shrink: 0; }
        .badge-now-showing { font-size: 10px; font-weight: 600; color: #15803d; background: #dcfce7; border: 1px solid #bbf7d0; padding: 2px 7px; border-radius: 999px; margin-left: auto; flex-shrink: 0; }
        .showtime:hover, .showtime.active { background: #e21b23 !important; border-color: #e21b23 !important; color: #fff !important; }
    </style>
</head>
<body>
<div class="page-shell">
    <!-- Common Header & Navigation -->
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
                                data-branch-id="<%= showtime.getBranchId() %>"
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
            const centreButtons = Array.from(document.querySelectorAll(".centre[data-branch-id]"));
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
            let selectedBranchId = content.dataset.initialBranch || "";
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

            // If initial movie is COMING_SOON, default filter to all
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
                // 1. Date Buttons
                dateButtons.forEach((button) => button.classList.toggle("active", button.dataset.date === selectedDate));
                const activeDate = dateButtons.find((button) => button.dataset.date === selectedDate);
                if (activeDate && activeDate.dataset.month) {
                    bookingMonth.textContent = activeDate.dataset.month;
                }

                // 2. City Tabs
                cityButtons.forEach((button) => {
                    const btnCity = button.dataset.city || "";
                    button.classList.toggle("active", normalize(btnCity) === normalize(selectedCity));
                });

                // 3. Cinema / Centre Buttons
                centreButtons.forEach((button) => {
                    button.hidden = Boolean(selectedCity) && normalize(button.dataset.city || "") !== normalize(selectedCity);
                });
                let activeCentre = centreButtons.find((button) => button.dataset.branchId === selectedBranchId && !button.hidden);
                if (!activeCentre) {
                    activeCentre = centreButtons.find((button) => !button.hidden);
                    selectedBranchId = activeCentre ? activeCentre.dataset.branchId : "";
                }
                centreButtons.forEach((button) => button.classList.toggle("active", button === activeCentre));

                // 4. Movie Filter Tabs
                movieFilterButtons.forEach((btn) => btn.classList.toggle("active", btn.dataset.filter === currentMovieFilter));

                // 5. Movie Items - NEVER auto-overwrite selectedMovieId to another movie!
                let visibleCount = 0;
                movieButtons.forEach((movie) => {
                    const matchFilter = (currentMovieFilter === "all" || movie.dataset.status === currentMovieFilter);
                    movie.hidden = !matchFilter;
                    movie.classList.toggle("active", movie.dataset.movieId === selectedMovieId);
                    if (matchFilter) visibleCount++;
                });
                if (movieEmpty) movieEmpty.hidden = visibleCount > 0;

                // 6. Showtimes Buttons
                const matchingShowtimes = showtimeButtons.filter((showtime) =>
                    showtime.dataset.date === selectedDate &&
                    showtime.dataset.branchId === selectedBranchId &&
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

                // 7. Showtimes Empty State & Selection Info
                const activeMovie = movieButtons.find((button) => button.dataset.movieId === selectedMovieId);
                const activeMovieTitle = activeMovie ? activeMovie.dataset.title : "";
                const isComingSoon = activeMovie && activeMovie.dataset.status === "COMING_SOON";

                if (matchingShowtimes.length === 0) {
                    showtimeEmpty.hidden = false;
                    if (isComingSoon) {
                        showtimeEmpty.innerHTML = "Phim <strong>" + escapeHtmlText(activeMovieTitle) + "</strong> là phim sắp chiếu và hiện chưa mở bán suất chiếu. Vui lòng quay lại sau.";
                    } else if (activeMovieTitle) {
                        const altDateSlot = showtimeButtons.find((s) => s.dataset.movieId === selectedMovieId && s.dataset.branchId === selectedBranchId);
                        const altBranchSlot = showtimeButtons.find((s) => s.dataset.movieId === selectedMovieId && s.dataset.date === selectedDate);
                        let hint = "";
                        if (altDateSlot) {
                            hint = " (Phim có suất chiếu vào ngày khác tại rạp này)";
                        } else if (altBranchSlot) {
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

            // Event Listeners:
            // Movie selection (Lotte Cinema behavior)
            movieButtons.forEach((button) => {
                button.addEventListener("click", () => {
                    selectedMovieId = button.dataset.movieId;
                    selectedShowtimeId = "";

                    // Auto-align: Does this movie have showtimes on currently selected date & branch?
                    const hasCurrentSlot = showtimeButtons.some((s) =>
                        s.dataset.movieId === selectedMovieId &&
                        s.dataset.branchId === selectedBranchId &&
                        s.dataset.date === selectedDate
                    );

                    if (!hasCurrentSlot) {
                        // Check if this movie has showtimes at the currently selected branch on any date
                        const branchSlot = showtimeButtons.find((s) =>
                            s.dataset.movieId === selectedMovieId &&
                            s.dataset.branchId === selectedBranchId
                        );
                        if (branchSlot) {
                            selectedDate = branchSlot.dataset.date;
                        } else {
                            // Find any showtime for this movie across all branches
                            const anySlot = showtimeButtons.find((s) => s.dataset.movieId === selectedMovieId);
                            if (anySlot) {
                                selectedDate = anySlot.dataset.date;
                                selectedBranchId = anySlot.dataset.branchId;
                                selectedCity = anySlot.dataset.city || "";
                            }
                        }
                    }

                    render();
                    scrollActiveIntoView();
                });
            });

            // Date selection
            dateButtons.forEach((button) => {
                button.addEventListener("click", () => {
                    selectedDate = button.dataset.date;
                    selectedShowtimeId = "";
                    render();
                });
            });

            // Cinema / Branch selection
            centreButtons.forEach((button) => {
                button.addEventListener("click", () => {
                    selectedBranchId = button.dataset.branchId;
                    selectedCity = button.dataset.city;
                    selectedShowtimeId = "";

                    // If selected movie has showtimes at this branch on another date, switch date
                    if (selectedMovieId) {
                        const hasDateSlot = showtimeButtons.some((s) =>
                            s.dataset.movieId === selectedMovieId &&
                            s.dataset.branchId === selectedBranchId &&
                            s.dataset.date === selectedDate
                        );
                        if (!hasDateSlot) {
                            const altDateSlot = showtimeButtons.find((s) =>
                                s.dataset.movieId === selectedMovieId &&
                                s.dataset.branchId === selectedBranchId
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

            // City tab selection
            cityButtons.forEach((button) => {
                button.addEventListener("click", () => {
                    selectedCity = button.dataset.city;
                    selectedShowtimeId = "";
                    const currentBranchBtn = centreButtons.find((c) => c.dataset.branchId === selectedBranchId);
                    if (!currentBranchBtn || (selectedCity && normalize(currentBranchBtn.dataset.city || "") !== normalize(selectedCity))) {
                        // If selected movie has a showtime in this city, pick that branch & date!
                        const movieSlotInCity = showtimeButtons.find((s) =>
                            s.dataset.movieId === selectedMovieId &&
                            (!selectedCity || normalize(s.dataset.city || "") === normalize(selectedCity))
                        );
                        if (movieSlotInCity) {
                            selectedBranchId = movieSlotInCity.dataset.branchId;
                            selectedDate = movieSlotInCity.dataset.date;
                        } else {
                            const firstBranchInCity = centreButtons.find((c) =>
                                !selectedCity || normalize(c.dataset.city || "") === normalize(selectedCity)
                            );
                            if (firstBranchInCity) {
                                selectedBranchId = firstBranchInCity.dataset.branchId;
                            }
                        }
                    }
                    render();
                    scrollActiveIntoView();
                });
            });

            // Movie category filter tabs
            movieFilterButtons.forEach((btn) => {
                btn.addEventListener("click", () => {
                    currentMovieFilter = btn.dataset.filter;
                    render();
                });
            });

            // Showtime selection
            showtimeButtons.forEach((button) => {
                button.addEventListener("click", () => {
                    selectedShowtimeId = button.dataset.showtimeId;
                    render();
                });
            });

            // Date arrow scroll
            document.querySelectorAll("[data-date-scroll]").forEach((button) => {
                button.addEventListener("click", () => {
                    dates.scrollBy({ left: button.dataset.dateScroll === "right" ? 240 : -240, behavior: "smooth" });
                });
            });

            // Initial render and scroll to active items
            render();
            setTimeout(scrollActiveIntoView, 100);

            if (new URLSearchParams(window.location.search).get("view") === "schedule") {
                document.getElementById("booking-showtimes").scrollIntoView();
            }
        }());
    </script>

    <!-- Common Footer -->
    <jsp:include page="/common/footer.jsp"/>
</div>
</body>
</html>




