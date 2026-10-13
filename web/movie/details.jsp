<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.Movie" %>
<%@ page import="model.User" %>
<%@ page import="model.Branch" %>
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
<body style="background: #f8f9fa; font-family: sans-serif; text-align: center; padding: 100px 20px;">
    <h2>Không tìm thấy thông tin phim</h2>
    <p>Phim bạn đang tìm kiếm không tồn tại hoặc đã ngừng chiếu.</p>
    <a href="${pageContext.request.contextPath}/home/index.jsp" style="display: inline-block; margin-top: 20px; padding: 12px 24px; background: #e21b23; color: #fff; text-decoration: none; border-radius: 6px; font-weight: 700;">Quay lại trang chủ</a>
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
        statusText = "Sắp chiếu";
        statusClass = "coming-soon";
    } else if ("ENDED".equalsIgnoreCase(movie.getStatus()) || "ARCHIVED".equalsIgnoreCase(movie.getStatus())) {
        statusText = "Đã kết thúc";
        statusClass = "ended";
    }

    String ageRating = movie.getAgeRating() == null || movie.getAgeRating().trim().isEmpty() ? "P" : movie.getAgeRating().trim().toUpperCase();
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#e21b23">
    <title><%= escapeHtml(movie.getTitle()) %> | Chi tiết phim CineFlow</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/layout.css?v=<%= System.currentTimeMillis() %>">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/movie/details.css?v=<%= System.currentTimeMillis() %>">
    <style>
        /* Inline CSS guarantee: ensures instant styling without browser cache delays */
        :root {
            --red: #e21b23;
            --red-hover: #c4131a;
            --red-subtle: #fef2f2;
            --red-border: #fecaca;
            --gold: #c9b27a;
            --cream: #eee9d8;
            --ink: #1f2937;
            --ink-dark: #111827;
            --text-muted: #6b7280;
            --text-body: #374151;
            --border-color: #e5e7eb;
            --bg-page: #f8f9fa;
            --bg-card: #ffffff;
            --star-gold: #f59e0b;
            --star-gray: #d1d5db;
            --radius-sm: 6px;
            --radius-md: 10px;
            --radius-lg: 16px;
            --shadow-card: 0 4px 20px rgba(0, 0, 0, 0.05);
            --font-main: "Be Vietnam Pro", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
        }
        
        /* Container & Breadcrumb */
        .movie-detail-container { width: min(1200px, calc(100% - 40px)); margin: 0 auto; padding: 24px 0 64px; }
        .breadcrumb-nav { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; font-size: 13px; color: #6b7280; margin-bottom: 24px; }
        .breadcrumb-nav a { color: #4b5563; }
        .breadcrumb-nav a:hover { color: var(--red); }
        .breadcrumb-separator { color: #9ca3af; font-size: 12px; }
        .breadcrumb-current { color: var(--red); font-weight: 600; }
        
        /* Section Heading */
        .section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; padding-bottom: 4px; }
        .section-title { display: flex; align-items: center; gap: 10px; margin: 0; font-size: 20px; font-weight: 700; color: var(--ink-dark); }
        .section-title .red-bar { width: 4px; height: 20px; background: var(--red); border-radius: 2px; display: inline-block; }
        .section-subtitle { font-size: 13px; color: var(--text-muted); }
        
        /* Trailer */
        .trailer-section { margin-bottom: 32px; }
        .trailer-wrapper { width: 100%; margin: 0 auto; }
        .trailer-main-player { position: relative; width: 100%; aspect-ratio: 16 / 9; border-radius: var(--radius-md); overflow: hidden; background: #111; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2); }
        .trailer-cover { position: absolute; inset: 0; width: 100%; height: 100%; cursor: pointer; z-index: 2; }
        .trailer-cover.playing { display: none; }
        .trailer-backdrop-wrap { position: absolute; inset: 0; width: 100%; height: 100%; }
        .trailer-backdrop { width: 100%; height: 100%; object-fit: cover; filter: brightness(0.65); display: block; }
        .trailer-backdrop-fallback { width: 100%; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; background: radial-gradient(circle at center, #381316 0%, #121013 100%); color: rgba(255, 255, 255, 0.4); }
        .cinema-reel-icon { width: 60px; height: 60px; border-radius: 50%; border: 2px dashed rgba(255, 255, 255, 0.25); display: grid; place-items: center; font-size: 22px; font-weight: 800; color: var(--red); }
        .fallback-cine-text { font-size: 12px; letter-spacing: 3px; font-weight: 700; color: rgba(255, 255, 255, 0.6); }
        .trailer-overlay { position: absolute; inset: 0; background: linear-gradient(180deg, rgba(0,0,0,0.2) 0%, rgba(0,0,0,0.4) 50%, rgba(0,0,0,0.85) 100%); }
        .trailer-play-button { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); width: 72px; height: 72px; border-radius: 50%; background: var(--red); color: #fff; border: 3px solid rgba(255, 255, 255, 0.85); display: grid; place-items: center; box-shadow: 0 6px 24px rgba(226, 27, 35, 0.6); transition: transform 0.25s, background 0.2s; cursor: pointer; z-index: 3; }
        .trailer-play-button svg { width: 28px; height: 28px; fill: #fff; margin-left: 4px; }
        .trailer-cover:hover .trailer-play-button { transform: translate(-50%, -50%) scale(1.12); background: #c8131a; }
        .trailer-label-bar { position: absolute; bottom: 0; left: 0; right: 0; padding: 16px 20px; display: flex; align-items: center; justify-content: space-between; color: #fff; font-size: 14px; font-weight: 700; letter-spacing: 0.5px; text-transform: uppercase; z-index: 3; }
        .trailer-accent-line { position: absolute; bottom: 0; left: 0; width: 100%; height: 4px; background: var(--red); z-index: 4; }
        .trailer-frame-container { width: 100%; height: 100%; }
        .trailer-frame-container iframe, .trailer-frame-container video { width: 100%; height: 100%; border: 0; display: block; }
        .trailer-placeholder { width: 100%; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 14px; background: linear-gradient(135deg, #1f1f23, #111113); color: #9ca3af; text-align: center; padding: 24px; }
        .trailer-placeholder svg { width: 56px; height: 56px; fill: #4b5563; }
        .trailer-placeholder h4 { margin: 0; font-size: 16px; color: #e5e7eb; }
        .trailer-placeholder p { margin: 0; font-size: 13px; max-width: 360px; }
        
        /* Movie Details Card */
        .movie-info-card { background: var(--bg-card); border: 1px solid var(--border-color); border-radius: var(--radius-lg); padding: 36px; box-shadow: var(--shadow-card); margin-bottom: 32px; }
        .movie-info-grid { display: grid; grid-template-columns: 260px 1fr; gap: 36px; align-items: start; }
        .movie-poster-wrap { position: relative; border-radius: var(--radius-md); overflow: hidden; box-shadow: 0 10px 24px rgba(0, 0, 0, 0.15); aspect-ratio: 2 / 3; background: #1a1a1a; }
        .movie-poster-wrap img { width: 100%; height: 100%; object-fit: cover; display: block; }
        .poster-fallback { width: 100%; height: 100%; display: grid; place-items: center; background: linear-gradient(135deg, #381216, #1f1214); color: rgba(255,255,255,0.7); font-size: 54px; font-weight: 800; font-family: Georgia, serif; }
        .movie-details-col { display: flex; flex-direction: column; }
        .status-pill { display: inline-flex; align-items: center; gap: 6px; padding: 4px 12px; border-radius: 999px; font-size: 12px; font-weight: 700; width: fit-content; margin-bottom: 10px; text-transform: uppercase; }
        .status-pill.now-showing { background: var(--red-subtle); color: var(--red); border: 1px solid var(--red-border); }
        .status-pill.coming-soon { background: #fef3c7; color: #b45309; border: 1px solid #fde68a; }
        .status-pill.ended { background: #f3f4f6; color: #4b5563; border: 1px solid #e5e7eb; }
        .status-dot { width: 6px; height: 6px; border-radius: 50%; background: currentColor; }
        .movie-title { margin: 0 0 6px; font-size: 32px; font-weight: 800; line-height: 1.2; color: var(--ink-dark); }
        .age-rating-box { display: inline-block; padding: 2px 7px; border-radius: var(--radius-sm); font-size: 12px; font-weight: 700; border: 1.5px solid var(--red); color: var(--red); background: #fff; line-height: 1.2; }
        .age-rating-box.general { border-color: #2563eb; color: #2563eb; }
        
        /* Movie Specs (LOTTE Cinema Style) */
        .movie-specs-list { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px 24px; margin-bottom: 24px; font-size: 13.5px; }
        .spec-row { display: flex; align-items: baseline; gap: 8px; }
        .spec-label { color: #6b7280; font-weight: 500; min-width: 80px; flex-shrink: 0; }
        .spec-value { color: var(--ink-dark); font-weight: 600; line-height: 1.4; }
        
        .movie-action-buttons { display: flex; flex-wrap: wrap; gap: 12px; margin-bottom: 24px; }
        .btn-book-now { display: inline-flex; align-items: center; justify-content: center; gap: 8px; min-height: 44px; padding: 0 28px; border-radius: var(--radius-sm); background: var(--red); color: #fff; font-size: 14px; font-weight: 700; transition: all 0.2s; box-shadow: 0 4px 14px rgba(226, 27, 35, 0.3); }
        .btn-book-now:hover { background: var(--red-hover); transform: translateY(-1px); box-shadow: 0 6px 18px rgba(226, 27, 35, 0.4); }
        .btn-book-now svg { width: 18px; height: 18px; fill: currentColor; }
        
        /* Rating Section */
        .rating-section-bar { border-top: 1px solid var(--border-color); padding-top: 20px; display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 24px; }
        .rating-left { display: flex; align-items: center; gap: 16px; }
        .rating-number-score { font-size: 32px; font-weight: 800; color: var(--ink-dark); line-height: 1; }
        .rating-number-score span { font-size: 15px; color: var(--text-muted); font-weight: 500; }
        .rating-stars-visual { display: flex; gap: 3px; color: var(--star-gold); font-size: 18px; }
        .rating-count-label { font-size: 12.5px; color: var(--text-muted); }
        .rating-right { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; }
        .rating-vote-prompt { font-size: 13.5px; font-weight: 600; color: var(--ink); }
        .interactive-stars { display: inline-flex; gap: 4px; }
        .star-btn { border: 0; background: transparent; padding: 2px; font-size: 22px; color: var(--star-gray); cursor: pointer; line-height: 1; transition: all 0.15s; }
        .star-btn:hover, .star-btn.hovered, .star-btn.selected { color: var(--star-gold); transform: scale(1.18); }
        .rating-chosen-label { font-size: 13px; color: #4b5563; font-weight: 500; min-width: 100px; }
        .btn-submit-vote { padding: 6px 14px; border-radius: var(--radius-sm); border: 1px solid var(--red); background: var(--red); color: #fff; font-size: 12.5px; font-weight: 600; transition: all 0.2s; }
        .btn-submit-vote:hover { background: var(--red-hover); }
        .rating-feedback-msg { width: 100%; margin-top: 8px; padding: 8px 12px; border-radius: var(--radius-sm); font-size: 13px; }
        .rating-feedback-msg.info { background: #eff6ff; color: #1e40af; border: 1px solid #bfdbfe; }
        .rating-feedback-msg.warning { background: #fffbeb; color: #92400e; border: 1px solid #fde68a; }
        .rating-feedback-msg.success { background: #ecfdf5; color: #065f46; border: 1px solid #a7f3d0; }
        .rating-coming-soon-notice { display: inline-flex; align-items: center; gap: 10px; background: #fff7ed; border: 1px solid #fed7aa; padding: 10px 16px; border-radius: var(--radius-sm); color: #9a3412; font-size: 13.5px; font-weight: 500; line-height: 1.5; }
        .rating-coming-soon-notice svg { flex-shrink: 0; color: #ea580c; }
        
        /* Synopsis Card (LOTTE Cinema Style) */
        .movie-synopsis-card { background: var(--bg-card); border: 1px solid var(--border-color); border-radius: var(--radius-lg); padding: 32px 36px; box-shadow: var(--shadow-card); margin-bottom: 32px; }
        .synopsis-content { margin-top: 14px; }
        .synopsis-text { font-size: 15px; line-height: 1.9; color: var(--text-body); margin: 0; }
        
        /* Footer */
        .footer { border-top: 1px solid #ddd; background: #fff; padding: 50px 0 30px; }
        .footer-inner { width: min(1250px, calc(100% - 40px)); display: grid; grid-template-columns: 2fr 1fr 1fr 1fr; gap: 40px; margin: auto; }
        .footer-brand p { margin-top: 15px; color: #777; font-size: 13px; }
        .footer h4 { margin: 0 0 16px; font-size: 14px; font-weight: 700; color: #252525; }
        .footer a { display: block; margin-bottom: 10px; color: #666; font-size: 13px; transition: color 0.2s; }
        .footer a:hover { color: var(--red); }
        .copyright { margin-top: 40px; padding-top: 20px; border-top: 1px solid #eee; color: #999; text-align: center; font-size: 12px; }
        
        /* Quick rail */
        .quick-rail { position: fixed; right: 18px; bottom: 24px; z-index: 99; display: flex; flex-direction: column; gap: 8px; }
        .quick-rail a { display: flex; flex-direction: column; align-items: center; justify-content: center; width: 52px; height: 52px; border-radius: 50%; background: #fff; color: #333; font-size: 10px; font-weight: 600; border: 1px solid #e0e0e0; box-shadow: 0 4px 14px rgba(0,0,0,0.12); transition: all 0.2s; }
        .quick-rail a:hover { background: var(--red); color: #fff; border-color: var(--red); transform: translateY(-2px); }
        .quick-rail .quick-icon { font-size: 16px; }

        @media (max-width: 992px) {
            .movie-info-grid { grid-template-columns: 220px 1fr; gap: 28px; }
            .footer-inner { grid-template-columns: 1fr 1fr; gap: 30px; }
        }
        @media (max-width: 768px) {
            .movie-detail-container { width: calc(100% - 24px); padding: 16px 0 48px; }
            .movie-info-grid { grid-template-columns: 1fr; gap: 20px; }
            .movie-poster-wrap { max-width: 220px; margin: 0 auto; }
            .movie-details-col { text-align: left; }
            .movie-specs-list { grid-template-columns: 1fr; gap: 8px; }
            .movie-action-buttons { justify-content: flex-start; }
            .rating-section-bar { flex-direction: column; align-items: flex-start; text-align: left; }
        }
    </style>
</head>
<body id="top">
<div class="page-shell">
    <!-- Common Header & Navigation -->
    <jsp:include page="/common/header.jsp">
        <jsp:param name="activeNav" value="home"/>
    </jsp:include>

    <!-- Main Page Content -->
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
        <!-- 1. TRAILER SECTION (LOTTE Cinema style: Big 16:9 player on top) -->
        <!-- ================================================================= -->
        <section class="trailer-section" id="trailer-section" aria-label="Trailer phim">
            <div class="section-header">
                <h2 class="section-title">
                    <span class="red-bar" aria-hidden="true"></span>
                    <span>Trailer phim</span>
                </h2>
            </div>

            <div class="trailer-wrapper">
                <!-- Main Trailer Player (16:9) -->
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
        <!-- 2. MOVIE POSTER & DETAILS CARD (LOTTE Cinema Style)              -->
        <!-- ================================================================= -->
        <article class="movie-info-card">
            <div class="movie-info-grid">
                <!-- Left Poster Column (2:3) -->
                <div class="movie-poster-wrap">
                    <% if (hasPoster) { %>
                        <img src="<%= escapeHtml(posterUrl) %>" alt="Poster <%= escapeHtml(movie.getTitle()) %>" onerror="this.style.display='none'; document.getElementById('mainPosterFallback').style.display='grid';">
                        <div class="poster-fallback" id="mainPosterFallback" style="display: none;"><%= getInitials(movie.getTitle()) %></div>
                    <% } else { %>
                        <div class="poster-fallback" id="mainPosterFallback"><%= getInitials(movie.getTitle()) %></div>
                    <% } %>
                </div>

                <!-- Right Information Column -->
                <div class="movie-details-col">
                    <!-- Status pill -->
                    <div class="status-pill <%= statusClass %>">
                        <span class="status-dot"></span>
                        <span><%= statusText %></span>
                    </div>

                    <!-- Title -->
                    <h1 class="movie-title"><%= escapeHtml(movie.getTitle()) %></h1>

                    <!-- Movie Specifications (LOTTE Cinema Style) -->
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

                    <!-- Action buttons -->
                    <div class="movie-action-buttons">
                        <a class="btn-book-now" href="${pageContext.request.contextPath}/booking/booking.jsp?movieId=<%= movie.getMovieId() %>">
                            <svg viewBox="0 0 24 24"><path d="M22 10V6c0-1.11-.9-2-2-2H4c-1.1 0-1.99.89-1.99 2v4c1.1 0 1.99.9 1.99 2s-.89 2-2 2v4c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2v-4c-1.1 0-2-.9-2-2s.9-2 2-2zm-9 7.5h-2v-2h2v2zm0-4.5h-2v-2h2v2zm0-4.5h-2v-2h2v2z"/></svg>
                            <span>Đặt vé ngay</span>
                        </a>
                    </div>

                    <!-- 3. Rating & Voting Section (LOTTE Cinema Style) -->
                    <div class="rating-section-bar">
                        <!-- Left: Overall Rating -->
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

                        <!-- Right: Interactive Voting -->
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
        <!-- 4. TÓM TẮT (LOTTE Cinema Style)                                  -->
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

    <!-- Footer (CineFlow Standard) -->
    <footer class="footer">
        <div class="footer-inner">
            <div class="footer-brand">
                <a class="logo" href="${pageContext.request.contextPath}/home/index.jsp">
                    <span class="logo-mark">C</span>
                    <span class="logo-word">Cine<span>Flow</span></span>
                </a>
                <p>Hệ thống rạp chiếu phim CineFlow — Trải nghiệm điện ảnh đỉnh cao.</p>
            </div>
            <div>
                <h4>CINEFLOW</h4>
                <a href="${pageContext.request.contextPath}/home/index.jsp#promotions">Tin mới &amp; ưu đãi</a>
                <a href="${pageContext.request.contextPath}/cinema/index.jsp">Rạp chiếu phim</a>
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

    <!-- Quick Utilities Rail -->
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

    <!-- Client-side Scripts -->
    <script>
        // State variables
        const defaultMovieTitle = "<%= escapeHtml(movie.getTitle()).replace("\"", "\\\"") %>";
        const defaultTrailerUrl = "<%= escapeHtml(trailerUrl == null ? "" : trailerUrl).replace("\"", "\\\"") %>";
        const defaultYtId = "<%= ytVideoId == null ? "" : ytVideoId %>";
        const defaultIsDirect = <%= hasDirectVideo %>;
        const isUserLoggedIn = <%= signedInUser != null %>;
        const currentMovieId = <%= movie.getMovieId() %>;
        let selectedStarValue = <%= userScore %>;

        // Trailer Player Control
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


        // Interactive Rating Star Handler
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

        // Hover effect on stars
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

        // Submit Rating Action (Real DB AJAX Integration)
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

                // Update UI score
                const avgEl = document.getElementById("avgScoreDisplay");
                if (avgEl && data.averageScore10 !== undefined) {
                    avgEl.textContent = data.averageScore10 > 0 ? Number(data.averageScore10).toFixed(1) : "--";
                }
                const countEl = document.getElementById("totalVotesDisplay");
                if (countEl && data.totalVotes !== undefined) {
                    countEl.textContent = data.totalVotes;
                }

                // Update visual star icons
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

    <!-- Common Footer -->
    <jsp:include page="/common/footer.jsp"/>
</div>
</body>
</html>
