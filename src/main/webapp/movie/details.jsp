<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.Movie" %>
<%@ page import="model.User" %>
<%@ page import="model.Centre" %>
<%@ page import="model.ShowtimeView" %>
<%@ page import="dao.CinemaDAO" %>
<%@ page import="dao.RatingDAO" %>
<%@ page import="dao.RatingDAO.MovieRatingSummary" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="java.util.regex.Pattern" %>
<%@ page import="java.util.regex.Matcher" %>
<%!
    private User sessionUser(Object value) {
        if (value instanceof User) {
            return (User) value;
        }
        if (value == null) {
            return null;
        }
        try {
            User compatible = new User();
            Class<?> type = value.getClass();
            try {
                compatible.setUserId((Integer) type.getMethod("getUserId").invoke(value));
            } catch (Exception ignored) {}
            compatible.setFullName((String) type.getMethod("getFullName").invoke(value));
            compatible.setAccount((String) type.getMethod("getAccount").invoke(value));
            compatible.setRole((String) type.getMethod("getRole").invoke(value));
            return compatible;
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            return null;
        }
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String formatDate(java.sql.Date date) {
        return date == null ? "Đang cập nhật" : new SimpleDateFormat("dd/MM/yyyy").format(date);
    }

    private String extractYouTubeId(String url) {
        if (url == null || url.trim().isEmpty()) return null;
        Pattern pattern = Pattern.compile(
            "(?:youtube\\.com\\/(?:watch\\?v=|embed\\/|v\\/)|youtu\\.be\\/)([a-zA-Z0-9_-]{6,12})"
        );
        Matcher matcher = pattern.matcher(url.trim());
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private boolean isDirectVideo(String url) {
        if (url == null) return false;
        String lower = url.trim().toLowerCase();
        return lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".ogg") || lower.contains("/uploads/");
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "CF";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
%>
<%
    User signedInUser = sessionUser(session.getAttribute("user"));
    String memberName = signedInUser == null ? "" : signedInUser.getFullName();
    if (memberName == null || memberName.trim().isEmpty()) {
        memberName = signedInUser == null ? "" : signedInUser.getAccount();
    }
    String contextPath = request.getContextPath();

    Movie movie = (Movie) request.getAttribute("movie");
    if (movie == null) {
        int movieId = 1;
        try {
            movieId = Integer.parseInt(request.getParameter("movieId"));
        } catch (Exception ignored) {}
        movie = new CinemaDAO().getMovieById(movieId);
    }

    if (movie == null) {
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Không tìm thấy phim | CineFlow</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/movie/details.css">
</head>
<body style="background: #0B0F17; color: #F8FAFC; font-family: sans-serif; text-align: center; padding: 100px 20px;">
    <h2>Không tìm thấy thông tin phim</h2>
    <p style="color: #94A3B8;">Phim bạn đang tìm kiếm không tồn tại hoặc đã ngừng chiếu.</p>
    <a href="${pageContext.request.contextPath}/home/index.jsp" style="display: inline-block; margin-top: 20px; padding: 12px 24px; background: #FF2A54; color: #fff; text-decoration: none; border-radius: 6px; font-weight: 700;">Quay lại trang chủ</a>
</body>
</html>
<%
        return;
    }

    RatingDAO.MovieRatingSummary ratingSummary = (RatingDAO.MovieRatingSummary) request.getAttribute("ratingSummary");
    if (ratingSummary == null && movie != null) {
        Integer currentUid = signedInUser != null && signedInUser.getUserId() > 0 ? signedInUser.getUserId() : null;
        ratingSummary = new RatingDAO().getRatingSummary(movie.getMovieId(), currentUid);
    }
    double avgScore10 = ratingSummary != null ? ratingSummary.getAverageScore10() : 0.0;
    double avgStars5 = ratingSummary != null ? ratingSummary.getAverageStars5() : 0.0;
    int totalVotes = ratingSummary != null ? ratingSummary.getTotalVotes() : 0;
    int userScore = ratingSummary != null ? ratingSummary.getUserScore() : 0;

    String currentDetailsPath = "/movie/details?movieId=" + movie.getMovieId();
    String encodedRedirectParam = java.net.URLEncoder.encode(currentDetailsPath, "UTF-8");
    String loginWithRedirectUrl = request.getContextPath() + "/auth/login.jsp?redirect=" + encodedRedirectParam;
    String registerWithRedirectUrl = request.getContextPath() + "/auth/register.jsp?redirect=" + encodedRedirectParam;

    String trailerUrl = movie.getTrailerUrl();
    String ytVideoId = extractYouTubeId(trailerUrl);
    boolean hasDirectVideo = isDirectVideo(trailerUrl);
    boolean hasTrailer = (ytVideoId != null || hasDirectVideo || (trailerUrl != null && !trailerUrl.trim().isEmpty()));

    String posterUrl = movie.getPosterUrl();
    boolean hasPoster = (posterUrl != null && !posterUrl.trim().isEmpty());

    boolean isComingSoon = movie.isComingSoon();
    String statusText = "Đang chiếu";
    String statusClass = "now-showing";
    if (isComingSoon) {
        statusText = "Sắp khởi chiếu";
        statusClass = "coming-soon";
    } else if ("ENDED".equalsIgnoreCase(movie.getStatus())) {
        statusText = "Đã kết thúc";
        statusClass = "ended";
    } else if ("ARCHIVED".equalsIgnoreCase(movie.getStatus())) {
        statusText = "Lưu trữ";
        statusClass = "archived";
    }

    String ageRating = movie.getAgeRating() == null || movie.getAgeRating().trim().isEmpty() ? "P" : movie.getAgeRating().trim().toUpperCase();
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#0B0F17">
    <title><%= escapeHtml(movie.getTitle()) %> | Chi tiết phim CineFlow</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700;800&family=Barlow+Condensed:wght@700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/layout.css?v=<%= System.currentTimeMillis() %>">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/movie/details.css?v=<%= System.currentTimeMillis() %>">
</head>
<body id="top">
<div class="page-shell">
    <!-- Thanh điều hướng và đầu trang chung -->
    <jsp:include page="/common/header.jsp">
        <jsp:param name="activeNav" value="home"/>
    </jsp:include>

    <!-- Nội dung trang chi tiết phim -->
    <main class="movie-detail-container">

        <!-- Breadcrumb: Trang chủ → Phim đang chiếu → Chi tiết phim -->
        <nav class="breadcrumb-nav" aria-label="Đường dẫn điều hướng">
            <a href="${pageContext.request.contextPath}/home/index.jsp">Trang chủ</a>
            <span class="breadcrumb-separator">›</span>
            <a href="${pageContext.request.contextPath}/home/index.jsp#movies">Phim đang chiếu</a>
            <span class="breadcrumb-separator">›</span>
            <span class="breadcrumb-current"><%= escapeHtml(movie.getTitle()) %></span>
        </nav>

        <!-- ================================================================= -->
        <!-- 1. Phân mục trình phát Trailer phim                              -->
        <!-- ================================================================= -->
        <section class="trailer-section" id="trailer-section" aria-label="Trailer phim">
            <div class="section-header">
                <h2 class="section-title">
                    <span class="red-bar" aria-hidden="true"></span>
                    <span>Trailer phim</span>
                </h2>
            </div>

            <div class="trailer-wrapper">
                <!-- Trình phát Trailer chính (tỷ lệ 16:9) -->
                <div class="trailer-main-player" id="mainPlayer">
                    <% if (hasTrailer) { %>
                        <div class="trailer-cover" id="trailerCover" onclick="startTrailerPlayer()">
                            <div class="trailer-backdrop-wrap">
                                <% if (hasPoster) { %>
                                    <img class="trailer-backdrop" src="<%= escapeHtml(posterUrl) %>" alt="Trailer <%= escapeHtml(movie.getTitle()) %>" onerror="this.style.display='none'; var fb=document.getElementById('trailerFallback'); if(fb) fb.style.display='flex';">
                                <% } %>
                                <div class="trailer-backdrop-fallback" id="trailerFallback" style="<%= hasPoster ? "display: none;" : "display: flex;" %>">
                                    <div class="cinema-reel-icon">CF</div>
                                    <span class="fallback-cine-text">CINEFLOW CINEMA</span>
                                </div>
                            </div>
                            <div class="trailer-overlay"></div>
                            <button type="button" class="trailer-play-button" aria-label="Phát trailer">
                                <svg viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>
                            </button>
                            <div class="trailer-label-bar">
                                <span id="currentTrailerTitle"><%= escapeHtml(movie.getTitle()).toUpperCase() %> — OFFICIAL TRAILER</span>
                            </div>
                            <div class="trailer-accent-line"></div>
                        </div>
                        <div class="trailer-frame-container" id="trailerMediaMount"></div>
                    <% } else { %>
                        <div class="trailer-placeholder">
                            <svg viewBox="0 0 24 24"><path d="M18 4l2 4h-3l-2-4h-2l2 4h-3l-2-4H8l2 4H7L5 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V4h-4zM8 17H6v-2h2v2zm0-4H6v-2h2v2zm0-4H6V7h2v2zm10 8h-8v-2h8v2zm0-4h-8v-2h8v2zm0-4h-8V7h8v2z"/></svg>
                            <h4>Trailer phim đang được cập nhật</h4>
                            <p>Video giới thiệu cho bộ phim này sẽ sớm được tải lên hệ thống CineFlow.</p>
                        </div>
                    <% } %>
                </div>
            </div>
        </section>

        <!-- ================================================================= -->
        <!-- 2. Thẻ Poster và Thông tin chi tiết phim                        -->
        <!-- ================================================================= -->
        <article class="movie-info-card">
            <div class="movie-info-grid">
                <!-- Cột áp phích bên trái (tỷ lệ 2:3) -->
                <div class="movie-poster-wrap">
                    <% if (hasPoster) { %>
                        <img src="<%= escapeHtml(posterUrl) %>" alt="Poster <%= escapeHtml(movie.getTitle()) %>" onerror="this.style.display='none'; document.getElementById('mainPosterFallback').style.display='grid';">
                        <div class="poster-fallback" id="mainPosterFallback" style="display: none;"><%= getInitials(movie.getTitle()) %></div>
                    <% } else { %>
                        <div class="poster-fallback" id="mainPosterFallback"><%= getInitials(movie.getTitle()) %></div>
                    <% } %>
                </div>

                <!-- Cột thông tin chi tiết bên phải -->
                <div class="movie-details-col">
                    <!-- Huy hiệu trạng thái -->
                    <div class="status-pill <%= statusClass %>">
                        <span class="status-dot"></span>
                        <span><%= statusText %></span>
                    </div>

                    <!-- Tên phim -->
                    <h1 class="movie-title"><%= escapeHtml(movie.getTitle()) %></h1>

                    <!-- Thông số chi tiết của phim -->
                    <div class="movie-specs-list">
                        <% if (movie.getDirector() != null && !movie.getDirector().trim().isEmpty()) { %>
                            <div class="spec-row">
                                <span class="spec-label">Đạo diễn:</span>
                                <span class="spec-value"><%= escapeHtml(movie.getDirector()) %></span>
                            </div>
                        <% } %>
                        <% if (movie.getCast() != null && !movie.getCast().trim().isEmpty()) { %>
                            <div class="spec-row">
                                <span class="spec-label">Diễn viên:</span>
                                <span class="spec-value"><%= escapeHtml(movie.getCast()) %></span>
                            </div>
                        <% } %>
                        <% if (movie.getGenre() != null && !movie.getGenre().trim().isEmpty()) { %>
                            <div class="spec-row">
                                <span class="spec-label">Thể loại:</span>
                                <span class="spec-value"><%= escapeHtml(movie.getGenre()) %></span>
                            </div>
                        <% } %>
                        <% if (movie.getReleaseDate() != null) { %>
                            <div class="spec-row">
                                <span class="spec-label">Khởi chiếu:</span>
                                <span class="spec-value"><%= formatDate(movie.getReleaseDate()) %></span>
                            </div>
                        <% } %>
                        <div class="spec-row">
                            <span class="spec-label">Thời lượng:</span>
                            <span class="spec-value"><%= movie.getDuration() %> phút</span>
                        </div>
                        <% if (movie.getLanguage() != null && !movie.getLanguage().trim().isEmpty()) { %>
                            <div class="spec-row">
                                <span class="spec-label">Ngôn ngữ:</span>
                                <span class="spec-value"><%= escapeHtml(movie.getLanguage()) %></span>
                            </div>
                        <% } %>
                        <div class="spec-row">
                            <span class="spec-label">Phân loại:</span>
                            <span class="spec-value">
                                <span class="age-rating-box <%= "P".equalsIgnoreCase(ageRating) || "K".equalsIgnoreCase(ageRating) ? "general" : "" %>"><%= escapeHtml(ageRating) %></span>
                            </span>
                        </div>
                    </div>

                    <!-- Nút thao tác đặt vé -->
                    <div class="movie-action-buttons">
                        <a class="btn-book-now" href="${pageContext.request.contextPath}/booking/booking.jsp?movieId=<%= movie.getMovieId() %>">
                            <svg viewBox="0 0 24 24"><path d="M22 10V6c0-1.11-.9-2-2-2H4c-1.1 0-1.99.89-1.99 2v4c1.1 0 1.99.9 1.99 2s-.89 2-2 2v4c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2v-4c-1.1 0-2-.9-2-2s.9-2 2-2zm-9 7.5h-2v-2h2v2zm0-4.5h-2v-2h2v2zm0-4.5h-2v-2h2v2z"/></svg>
                            <span>Đặt vé ngay</span>
                        </a>
                    </div>

                    <!-- 3. Phân mục điểm đánh giá và bình chọn -->
                    <div class="rating-section-bar">
                        <!-- Bên trái: Điểm số trung bình -->
                        <div class="rating-left">
                            <div class="rating-number-score">
                                <span id="avgScoreDisplay"><%= !isComingSoon && totalVotes > 0 ? String.format(java.util.Locale.US, "%.1f", avgScore10) : "--" %></span> <span>/10</span>
                            </div>
                            <div>
                                <div class="rating-stars-visual" id="visualStarsDisplay" title="<%= isComingSoon ? "Phim sắp chiếu - Chưa mở đánh giá" : (totalVotes > 0 ? ("Đánh giá trung bình " + String.format(java.util.Locale.US, "%.1f", avgStars5) + "/5 sao") : "Chưa có lượt đánh giá nào") %>">
                                    <% 
                                        int fullStars = (!isComingSoon && totalVotes > 0) ? (int) Math.round(avgStars5) : 0;
                                        for (int s = 1; s <= 5; s++) { 
                                            if (s <= fullStars) { %>★<% } else { %><span style="color: #d1d5db;">★</span><% }
                                        } 
                                    %>
                                </div>
                                <div class="rating-count-label">
                                    <% if (isComingSoon) { %>
                                        <span id="totalVotesDisplay" style="color: #ea580c; font-weight: 600;">Sắp chiếu (Chưa mở đánh giá)</span>
                                    <% } else { %>
                                        <span id="totalVotesDisplay"><%= totalVotes %></span> lượt đánh giá
                                    <% } %>
                                </div>
                            </div>
                        </div>

                        <!-- Bên phải: Tương tác đánh giá -->
                        <div class="rating-right">
                            <% if (isComingSoon) { %>
                                <div class="rating-coming-soon-notice">
                                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
                                    <span>Phim sắp chiếu chưa mở cổng đánh giá. Khán giả có thể bình chọn sau khi phim khởi chiếu tại rạp.</span>
                                </div>
                            <% } else { %>
                                <span class="rating-vote-prompt" id="ratingVotePrompt">
                                    <%= userScore > 0 ? "Đánh giá của bạn:" : "Bạn đánh giá phim này?" %>
                                </span>
                                <div class="interactive-stars" id="interactiveStars" role="radiogroup" aria-label="Đánh giá từ 1 đến 5 sao">
                                    <% for (int val = 1; val <= 5; val++) { %>
                                        <button type="button" class="star-btn <%= val <= userScore ? "selected" : "" %>" data-value="<%= val %>" aria-label="<%= val %> sao" onclick="selectRating(<%= val %>)">★</button>
                                    <% } %>
                                </div>
                                <span class="rating-chosen-label" id="ratingChosenLabel">
                                    <%= userScore > 0 ? ("Bạn chọn " + userScore + "/5 sao") : "Chọn số sao" %>
                                </span>
                                <button type="button" class="btn-submit-vote" id="btnSubmitVote" onclick="submitRating()">
                                    <%= userScore > 0 ? "Cập nhật đánh giá" : "Gửi đánh giá" %>
                                </button>

                                <div class="rating-feedback-msg" id="ratingFeedbackBox" hidden></div>
                            <% } %>
                        </div>
                    </div>
                </div>
            </div>
        </article>

        <!-- ================================================================= -->
        <!-- 4. Tóm tắt nội dung kịch bản phim                                -->
        <!-- ================================================================= -->
        <section class="movie-synopsis-card" aria-label="Tóm tắt nội dung phim">
            <div class="section-header">
                <h2 class="section-title">
                    <span class="red-bar" aria-hidden="true"></span>
                    <span>Tóm tắt</span>
                </h2>
            </div>
            <div class="synopsis-content">
                <p class="synopsis-text">
                    <%= movie.getDescription() != null && !movie.getDescription().trim().isEmpty()
                            ? escapeHtml(movie.getDescription())
                            : "Nội dung phim đang được cập nhật." %>
                </p>
            </div>
        </section>

    </main>

    <!-- Chân trang CineFlow -->
    <footer class="footer">
        <div class="footer-inner">
            <div class="footer-brand">
                <a class="logo" href="${pageContext.request.contextPath}/home/index.jsp">
                    <span class="logo-mark">C</span>
                    <span class="logo-word">Cine<span>Flow</span></span>
                </a>
                <p>Hệ thống trung tâm chiếu phim CineFlow — Trải nghiệm điện ảnh đỉnh cao.</p>
            </div>
            <div>
                <h4>CINEFLOW</h4>
                <a href="${pageContext.request.contextPath}/home/index.jsp#promotions">Tin mới &amp; ưu đãi</a>
                <a href="${pageContext.request.contextPath}/cinema/index.jsp">Trung tâm chiếu phim</a>
                <a href="${pageContext.request.contextPath}/home/index.jsp#movies">Phim đang chiếu</a>
                <a href="${pageContext.request.contextPath}/booking/booking.jsp?view=schedule">Lịch chiếu</a>
            </div>
            <div id="support">
                <h4>HỖ TRỢ</h4>
                <a href="${pageContext.request.contextPath}/home/index.jsp#support">Hỗ trợ khách hàng</a>
                <a href="${pageContext.request.contextPath}/home/index.jsp#support">Câu hỏi thường gặp</a>
                <a href="${pageContext.request.contextPath}/home/index.jsp#support">Chính sách bảo mật</a>
                <a href="${pageContext.request.contextPath}/home/index.jsp#support">Điều khoản sử dụng</a>
            </div>
            <div>
                <h4>TÀI KHOẢN</h4>
                <% if (signedInUser == null) { %>
                    <a href="<%= loginWithRedirectUrl %>">Đăng nhập</a>
                    <a href="<%= registerWithRedirectUrl %>">Tạo tài khoản</a>
                <% } else { %>
                    <a href="${pageContext.request.contextPath}/account">Tài khoản của tôi</a>
                <% } %>
            </div>
        </div>
        <div class="copyright">© 2026 CineFlow. All rights reserved.</div>
    </footer>

    <!-- Thanh tiện ích nhanh -->
    <nav class="quick-rail" aria-label="Tiện ích nhanh">
        <a href="${pageContext.request.contextPath}/booking/booking.jsp?movieId=<%= movie.getMovieId() %>" title="Đặt vé ngay">
            <span class="quick-icon">🎟</span>
            <span>Đặt vé</span>
        </a>
        <a href="${pageContext.request.contextPath}/home/index.jsp#support" title="Hỗ trợ">
            <span class="quick-icon">❔</span>
            <span>Hỗ trợ</span>
        </a>
        <a class="top" href="#top" title="Lên đầu trang">
            <span class="quick-icon">↑</span>
            <span>Đầu trang</span>
        </a>
    </nav>

    <!-- Kịch bản phía máy khách (Client-side Scripts) -->
    <script>
        // Khai báo biến trạng thái
        const defaultMovieTitle = "<%= escapeHtml(movie.getTitle()).replace("\"", "\\\"") %>";
        const defaultTrailerUrl = "<%= escapeHtml(trailerUrl == null ? "" : trailerUrl).replace("\"", "\\\"") %>";
        const defaultYtId = "<%= ytVideoId == null ? "" : ytVideoId %>";
        const defaultIsDirect = <%= hasDirectVideo %>;
        const isUserLoggedIn = <%= signedInUser != null %>;
        const currentMovieId = <%= movie.getMovieId() %>;
        let selectedStarValue = <%= userScore %>;

        // Điều khiển trình phát trailer
        function startTrailerPlayer(scrollFirst) {
            if (scrollFirst) {
                const section = document.getElementById("trailer-section");
                if (section) {
                    section.scrollIntoView({ behavior: "smooth", block: "start" });
                }
            }

            const cover = document.getElementById("trailerCover");
            const mount = document.getElementById("trailerMediaMount");
            if (!mount) return;

            mount.innerHTML = "";

            if (defaultYtId) {
                const iframe = document.createElement("iframe");
                iframe.src = "https://www.youtube.com/embed/" + defaultYtId + "?autoplay=1&rel=0";
                iframe.allow = "accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture";
                iframe.allowFullscreen = true;
                mount.appendChild(iframe);
            } else if (defaultIsDirect) {
                const video = document.createElement("video");
                video.src = defaultTrailerUrl;
                video.controls = true;
                video.autoplay = true;
                video.playsInline = true;
                mount.appendChild(video);
            } else if (defaultTrailerUrl) {
                const iframe = document.createElement("iframe");
                iframe.src = defaultTrailerUrl;
                iframe.allow = "autoplay";
                iframe.allowFullscreen = true;
                mount.appendChild(iframe);
            }

            if (cover) {
                cover.classList.add("playing");
            }
        }


        // Xử lý chọn số sao đánh giá
        function selectRating(val) {
            selectedStarValue = val;
            const stars = document.querySelectorAll(".star-btn");
            stars.forEach((star, idx) => {
                star.classList.toggle("selected", idx < val);
            });
            const label = document.getElementById("ratingChosenLabel");
            if (label) {
                label.textContent = "Bạn chọn " + val + "/5 sao";
            }
        }

        // Hiệu ứng rê chuột trên các ngôi sao
        document.querySelectorAll(".star-btn").forEach((star, idx) => {
            star.addEventListener("mouseenter", () => {
                document.querySelectorAll(".star-btn").forEach((s, i) => {
                    s.classList.toggle("hovered", i <= idx);
                });
            });
            star.addEventListener("mouseleave", () => {
                document.querySelectorAll(".star-btn").forEach(s => s.classList.remove("hovered"));
            });
        });

        const isMovieComingSoon = <%= isComingSoon %>;

        // Gửi đánh giá qua AJAX cập nhật cơ sở dữ liệu
        function submitRating() {
            if (isMovieComingSoon) {
                const feedback = document.getElementById("ratingFeedbackBox");
                if (feedback) {
                    feedback.hidden = false;
                    feedback.className = "rating-feedback-msg warning";
                    feedback.textContent = "Phim sắp chiếu chưa mở cổng đánh giá.";
                }
                return;
            }
            const feedback = document.getElementById("ratingFeedbackBox");
            const btn = document.getElementById("btnSubmitVote");
            if (!feedback) return;
            feedback.hidden = false;

            const loginUrlWithRedirect = "${pageContext.request.contextPath}/auth/login.jsp?redirect=" + encodeURIComponent(window.location.pathname + window.location.search);
            if (!isUserLoggedIn) {
                feedback.className = "rating-feedback-msg warning";
                feedback.innerHTML = 'Vui lòng <a href="' + loginUrlWithRedirect + '" style="text-decoration: underline; font-weight: 700;">Đăng nhập</a> để gửi đánh giá phim.';
                return;
            }

            if (selectedStarValue <= 0) {
                feedback.className = "rating-feedback-msg warning";
                feedback.textContent = "Vui lòng chọn số sao (từ 1 đến 5 sao) trước khi gửi đánh giá.";
                return;
            }

            if (btn) {
                btn.disabled = true;
                btn.textContent = "Đang lưu...";
            }

            const formData = new URLSearchParams();
            formData.append("movieId", currentMovieId);
            formData.append("score", selectedStarValue);

            fetch("${pageContext.request.contextPath}/movie/rate", {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8"
                },
                body: formData.toString()
            })
            .then(res => res.json())
            .then(data => {
                if (btn) btn.disabled = false;
                if (!data.success) {
                    if (data.needLogin) {
                        feedback.className = "rating-feedback-msg warning";
                        feedback.innerHTML = 'Vui lòng <a href="' + loginUrlWithRedirect + '" style="text-decoration: underline; font-weight: 700;">Đăng nhập</a> để gửi đánh giá phim.';
                    } else {
                        feedback.className = "rating-feedback-msg warning";
                        feedback.textContent = data.message || "Không thể lưu đánh giá. Vui lòng thử lại.";
                    }
                    if (btn) btn.textContent = selectedStarValue > 0 ? "Cập nhật đánh giá" : "Gửi đánh giá";
                    return;
                }

                feedback.className = "rating-feedback-msg success";
                feedback.textContent = data.message || ("Đã lưu đánh giá " + selectedStarValue + "/5 sao thành công!");

                if (btn) btn.textContent = "Cập nhật đánh giá";
                const promptEl = document.getElementById("ratingVotePrompt");
                if (promptEl) promptEl.textContent = "Đánh giá của bạn:";

                // Cập nhật điểm số hiển thị trên giao diện
                const avgEl = document.getElementById("avgScoreDisplay");
                if (avgEl && data.averageScore10 !== undefined) {
                    avgEl.textContent = data.averageScore10 > 0 ? Number(data.averageScore10).toFixed(1) : "--";
                }
                const countEl = document.getElementById("totalVotesDisplay");
                if (countEl && data.totalVotes !== undefined) {
                    countEl.textContent = data.totalVotes;
                }

                // Cập nhật biểu tượng sao hiển thị
                const starsWrap = document.getElementById("visualStarsDisplay");
                if (starsWrap && data.averageStars5 !== undefined) {
                    const rounded = Math.round(Number(data.averageStars5));
                    starsWrap.title = "Đánh giá trung bình " + Number(data.averageStars5).toFixed(1) + "/5 sao";
                    let starHtml = "";
                    for (let s = 1; s <= 5; s++) {
                        if (s <= rounded) {
                            starHtml += "★ ";
                        } else {
                            starHtml += '<span style="color: #d1d5db;">★</span> ';
                        }
                    }
                    starsWrap.innerHTML = starHtml.trim();
                }
            })
            .catch(err => {
                if (btn) {
                    btn.disabled = false;
                    btn.textContent = selectedStarValue > 0 ? "Cập nhật đánh giá" : "Gửi đánh giá";
                }
                feedback.className = "rating-feedback-msg warning";
                feedback.textContent = "Lỗi kết nối khi gửi đánh giá. Vui lòng thử lại sau.";
            });
        }
    </script>

    <!-- Chân trang chung -->
    <jsp:include page="/common/footer.jsp"/>
</div>
</body>
</html>
