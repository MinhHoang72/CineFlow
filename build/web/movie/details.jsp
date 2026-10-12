<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.Movie" %>
<%!
    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<% Movie movie = (Movie) request.getAttribute("movie"); %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%= escapeHtml(movie.getTitle()) %> | CineFlow</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/movie/details.css">
</head>
<body>
    <main class="movie-detail-page">
<a class="back-link" href="${pageContext.request.contextPath}/home/index.jsp">← Quay lại danh mục phim</a>
        <article class="movie-detail-card">
            <div class="movie-detail-poster">
                <% if (movie.getPosterUrl() != null && !movie.getPosterUrl().trim().isEmpty()) { %>
                    <img src="<%= escapeHtml(movie.getPosterUrl()) %>" alt="Poster <%= escapeHtml(movie.getTitle()) %>">
                <% } else { %>
                    <div class="poster-placeholder">CF</div>
                <% } %>
            </div>
            <div class="movie-detail-content">
                <p class="eyebrow">CINEFLOW MOVIE</p>
                <h1><%= escapeHtml(movie.getTitle()) %></h1>
                <div class="movie-meta">
                    <span><%= escapeHtml(movie.getAgeRating()) %></span>
                    <span><%= movie.getDuration() %> phút</span>
                    <span><%= escapeHtml(movie.getLanguage()) %></span>
                    <span><%= escapeHtml(movie.getGenre()) %></span>
                </div>
                <p class="description"><%= escapeHtml(movie.getDescription()) %></p>
                <dl class="movie-facts">
                    <div><dt>Đạo diễn</dt><dd><%= escapeHtml(movie.getDirector()) %></dd></div>
                    <div><dt>Diễn viên</dt><dd><%= escapeHtml(movie.getCast()) %></dd></div>
                    <div><dt>Khởi chiếu</dt><dd><%= movie.getReleaseDate() == null ? "Đang cập nhật" : movie.getReleaseDate() %></dd></div>
                </dl>
                <a class="primary-button" href="${pageContext.request.contextPath}/booking/booking.jsp?movieId=<%= movie.getMovieId() %>">Xem suất chiếu và mua vé</a>
            </div>
        </article>
    </main>
</body>
</html>


