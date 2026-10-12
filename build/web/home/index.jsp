<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.User" %>
<%@ page import="model.Movie" %>
<%@ page import="model.Branch" %>
<%@ page import="dao.CinemaDAO" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.List" %>
<%!
    private User sessionUser(Object value) {
        if (value instanceof User) {
            return (User) value;
        }
        // Compatibility with sessions created by an older deployment that used
        // model.user.User. Do not let a stale session cause a ClassCastException.
        if (value == null) {
            return null;
        }
        try {
            User compatible = new User();
            Class<?> type = value.getClass();
            compatible.setFullName((String) type.getMethod("getFullName").invoke(value));
            compatible.setAccount((String) type.getMethod("getAccount").invoke(value));
            compatible.setRole((String) type.getMethod("getRole").invoke(value));
            return compatible;
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            return null;
        }
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String initials(String title) {
        if (title == null || title.trim().isEmpty()) {
            return "CF";
        }
        String[] words = title.trim().split("\\s+");
        String result = words[0].substring(0, 1);
        if (words.length > 1) {
            result += words[1].substring(0, 1);
        }
        return result.toUpperCase();
    }

    private String formatDate(java.sql.Date date) {
        return date == null ? "" : new SimpleDateFormat("dd/MM/yyyy").format(date);
    }

%>
<%
    User signedInUser = sessionUser(session.getAttribute("user"));
    String memberName = signedInUser == null ? "" : signedInUser.getFullName();
    if (memberName == null || memberName.trim().isEmpty()) {
        memberName = signedInUser == null ? "" : signedInUser.getAccount();
    }
    String contextPath = request.getContextPath();
    CinemaDAO cinemaDAO = new CinemaDAO();
    List<Movie> movies = cinemaDAO.getNowShowingMovies();
    List<Movie> comingSoonMovies = cinemaDAO.getComingSoonMovies();
    List<Branch> branches = cinemaDAO.getActiveBranches();
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#e31b23">
    <title>CineFlow Cinema</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Barlow+Condensed:wght@500;600;700&family=Be+Vietnam+Pro:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/home/index.css">
</head>
<body id="top">
<div class="page-shell">
    <!-- Common Header & Navigation -->
    <jsp:include page="/common/header.jsp">
        <jsp:param name="activeNav" value="home"/>
    </jsp:include>

    <main>
    <section class="hot-section">
        <div class="hot-heading"><span></span><h2>PHIM HOT TẠI RẠP</h2><span></span></div>
        <div class="hero" id="hero" aria-label="Phim nổi bật">
        <% if (movies.isEmpty()) { %>
            <article class="hero-slide active">
                <div class="hero-fallback">CF</div>
                <div class="hero-overlay"></div>
                <div class="hero-content"><p class="hero-label">CINEFLOW FEATURE</p><h1>TRẢI NGHIỆM<br>ĐIỆN ẢNH ĐỈNH CAO</h1><p>Khám phá những bộ phim nổi bật đang được trình chiếu tại CineFlow.</p><div class="hero-buttons"><a href="#movies">Xem phim</a><a class="outline" href="#promotions">Tin mới &amp; ưu đãi</a></div></div>
            </article>
        <% } else { %>
            <% String[] heroImages = {"https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=1800&q=85", "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?auto=format&fit=crop&w=1800&q=85", "https://images.unsplash.com/photo-1485846234645-a62644f84728?auto=format&fit=crop&w=1800&q=85"}; %>
            <% int heroCount = Math.min(movies.size(), 3); for (int heroIndex = 0; heroIndex < heroCount; heroIndex++) { Movie featuredMovie = movies.get(heroIndex); %>
                <article class="hero-slide<%= heroIndex == 0 ? " active" : "" %>">
                    <img class="hero-image" src="<%= heroImages[heroIndex] %>" alt="Không gian điện ảnh CineFlow">
                    <div class="hero-overlay"></div>
                    <div class="hero-content">
                        <p class="hero-label"><%= heroIndex == 0 ? "CINEFLOW FEATURE" : "PHIM ĐANG CHIẾU" %></p>
                        <h1><%= escapeHtml(featuredMovie.getTitle()) %></h1>
                        <p><%= escapeHtml(featuredMovie.getDescription()) %></p>
                        <div class="hero-buttons"><a href="<%= contextPath %>/booking/booking.jsp">Mua vé</a><a class="outline" href="#movies">Chi tiết</a></div>
                    </div>
                </article>
            <% } %>
            <% if (heroCount > 1) { %>
                <button class="hero-control previous" type="button" aria-label="Phim trước" onclick="changeHero(-1)">‹</button>
                <button class="hero-control next" type="button" aria-label="Phim tiếp theo" onclick="changeHero(1)">›</button>
                <div class="hero-dots" aria-label="Chọn phim nổi bật">
                    <% for (int dotIndex = 0; dotIndex < heroCount; dotIndex++) { %>
                        <button class="hero-dot<%= dotIndex == 0 ? " active" : "" %>" type="button" aria-label="Phim nổi bật <%= dotIndex + 1 %>" onclick="showHero(<%= dotIndex %>)"></button>
                    <% } %>
                </div>
            <% } %>
        <% } %>
        </div>
    </section>

        <section class="movies-section" id="movies" aria-label="Danh mục phim">
          <div class="catalog">
            <nav class="tabs" aria-label="Danh mục phim">
                <button class="tab active" type="button" data-catalog="nowShowingGrid" aria-pressed="true">Phim đang chiếu</button>
                <button class="tab" type="button" data-catalog="comingSoonGrid" aria-pressed="false">Phim sắp chiếu</button>
            </nav>
            <section class="movie-grid" id="nowShowingGrid" aria-label="Phim đang chiếu">
                <% if (movies.isEmpty()) { %>
                    <p class="empty-state">Chưa có phim đang chiếu.</p>
                <% } else { %>
                    <% for (Movie movie : movies) { %>
                            <article class="movie-card" data-title="<%= escapeHtml(movie.getTitle()) %>" data-genre="<%= escapeHtml(movie.getGenre()) %>">
                            <div class="poster">
                                <% if (movie.getPosterUrl() != null && !movie.getPosterUrl().trim().isEmpty()) { %>
                                    <img src="<%= escapeHtml(movie.getPosterUrl()) %>" alt="Poster <%= escapeHtml(movie.getTitle()) %>" loading="lazy">
                                <% } else { %>
                                    <div class="poster-placeholder"><%= initials(movie.getTitle()) %></div>
                                <% } %>
                                <span class="age-badge<%= "T18".equalsIgnoreCase(movie.getAgeRating()) ? " restricted" : ("T13".equalsIgnoreCase(movie.getAgeRating()) || "P".equalsIgnoreCase(movie.getAgeRating()) ? " general" : "") %>"><%= escapeHtml(movie.getAgeRating()) %></span>
                                <div class="poster-actions">
                                    <a class="poster-hover-link detail" href="<%= contextPath %>/movie/details?movieId=<%= movie.getMovieId() %>">Chi tiết</a>
                                    <a class="poster-hover-link book" href="<%= contextPath %>/booking/booking.jsp">Mua vé</a>
                                </div>
                            </div>
                            <div class="movie-info">
                                <h3><%= escapeHtml(movie.getTitle()) %></h3>
                                <p><%= movie.getDuration() %> phút <b>|</b> <%= formatDate(movie.getReleaseDate()) %></p>
                            </div>
                        </article>
                    <% } %>
                <% } %>
                <p class="empty-state filter-empty" hidden>Không tìm thấy phim phù hợp.</p>
            </section>

            <section class="movie-grid" id="comingSoonGrid" aria-label="Phim sắp chiếu" hidden>
                <% if (comingSoonMovies.isEmpty()) { %>
                    <p class="empty-state">Chưa có phim sắp chiếu.</p>
                <% } else { %>
                    <% for (Movie movie : comingSoonMovies) { %>
                            <article class="movie-card" data-title="<%= escapeHtml(movie.getTitle()) %>" data-genre="<%= escapeHtml(movie.getGenre()) %>">
                            <div class="poster">
                                <% if (movie.getPosterUrl() != null && !movie.getPosterUrl().trim().isEmpty()) { %>
                                    <img src="<%= escapeHtml(movie.getPosterUrl()) %>" alt="Poster <%= escapeHtml(movie.getTitle()) %>" loading="lazy">
                                <% } else { %>
                                    <div class="poster-placeholder"><%= initials(movie.getTitle()) %></div>
                                <% } %>
                                <span class="age-badge<%= "T18".equalsIgnoreCase(movie.getAgeRating()) ? " restricted" : ("T13".equalsIgnoreCase(movie.getAgeRating()) || "P".equalsIgnoreCase(movie.getAgeRating()) ? " general" : "") %>"><%= escapeHtml(movie.getAgeRating()) %></span>
                                <div class="poster-actions">
                                    <a class="poster-hover-link detail" href="<%= contextPath %>/movie/details?movieId=<%= movie.getMovieId() %>">Chi tiết</a>
                                </div>
                            </div>
                            <div class="movie-info">
                                <h3><%= escapeHtml(movie.getTitle()) %></h3>
                                <p><%= formatDate(movie.getReleaseDate()) %> <b>|</b> Sắp chiếu</p>
                            </div>
                        </article>
                    <% } %>
                <% } %>
                <p class="empty-state filter-empty" hidden>Không tìm thấy phim phù hợp.</p>
            </section>

            <div class="movie-pagination" aria-label="Phân trang phim">
                <button class="movie-arrow" type="button" data-page="previous" aria-label="Trang trước">‹</button>
                <div class="movie-page-dots"></div>
                <button class="movie-arrow" type="button" data-page="next" aria-label="Trang tiếp theo">›</button>
            </div>
                    </div>
        </section>

        <section class="promotion-section" id="promotions" aria-labelledby="promotions-title">
            <div class="section-title"><span></span><h2 id="promotions-title">TIN MỚI &amp; ƯU ĐÃI</h2><span></span></div>
            <div class="promotion-grid">
                <article class="promotion-card"><img src="https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&amp;fit=crop&amp;w=900&amp;q=85" alt="Không gian phòng chiếu CineFlow"><div class="promotion-content"><span class="promotion-tag">ƯU ĐÃI ĐẶC BIỆT</span><h3>Thứ Ba thành viên</h3><p>Giảm đến 30% giá vé cho thành viên CineFlow mỗi thứ Ba.</p><a href="<%= contextPath %>/booking/booking.jsp">Đặt vé ngay →</a></div></article>
                <article class="promotion-card"><img src="https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?auto=format&amp;fit=crop&amp;w=900&amp;q=85" alt="Trải nghiệm điện ảnh CineFlow"><div class="promotion-content"><span class="promotion-tag">TIN MỚI</span><h3>Đêm phim cuối tuần</h3><p>Cùng bạn bè tận hưởng những suất chiếu đặc biệt cuối tuần.</p><a href="<%= contextPath %>/booking/booking.jsp">Xem lịch chiếu →</a></div></article>
                <article class="promotion-card"><img src="https://images.unsplash.com/photo-1485846234645-a62644f84728?auto=format&amp;fit=crop&amp;w=900&amp;q=85" alt="Bắp nước tại CineFlow"><div class="promotion-content"><span class="promotion-tag">COMBO MỚI</span><h3>Combo đi cùng bạn</h3><p>Thưởng thức phim trọn vẹn với combo bắp nước dành cho hai người.</p><a href="<%= contextPath %>/booking/booking.jsp">Khám phá ngay →</a></div></article>
            </div>
        </section>

    </main>

    <!-- Common Footer -->
    <jsp:include page="/common/footer.jsp"/>
</div>

<nav class="quick-rail" aria-label="Tiện ích nhanh">
    <a href="<%= contextPath %>/booking/booking.jsp"><span class="quick-icon">🎟</span><span>Đặt vé</span></a>
    <a href="#support"><span class="quick-icon">❔</span><span>Hỗ trợ</span></a>
    <a class="top" href="#top"><span class="quick-icon">↑</span><span>Đầu trang</span></a>
</nav>
<script>
    (function () {
        const heroSlides = Array.from(document.querySelectorAll(".hero-slide"));
        const heroDots = Array.from(document.querySelectorAll(".hero-dot"));
        let activeHero = 0;
        let heroTimer;

        function showHero(index) {
            if (heroSlides.length < 2) return;
            activeHero = (index + heroSlides.length) % heroSlides.length;
            heroSlides.forEach((slide, position) => slide.classList.toggle("active", position === activeHero));
            heroDots.forEach((dot, position) => dot.classList.toggle("active", position === activeHero));
        }

        window.showHero = function (index) {
            showHero(index);
            window.clearInterval(heroTimer);
            heroTimer = window.setInterval(() => showHero(activeHero + 1), 5000);
        };
        window.changeHero = function (direction) {
            window.showHero(activeHero + direction);
        };
        if (heroSlides.length > 1) {
            heroTimer = window.setInterval(() => showHero(activeHero + 1), 5000);
        }

        const catalogTabs = Array.from(document.querySelectorAll("[data-catalog]"));
        const moviePagination = document.querySelector(".movie-pagination");
        const pageDots = document.querySelector(".movie-page-dots");
        const pageButtons = Array.from(document.querySelectorAll("[data-page]"));
        let activeGrid = document.getElementById("nowShowingGrid");
        let currentPage = 0;
        const pageSize = 4;
        let searchTerm = "";
        let selectedGenre = "";
        let selectedBranchId = "";

        function normalize(value) {
            return value.normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/\u0111/g, "d").replace(/\u0110/g, "D").toLowerCase();
        }

        function renderMovies() {
            const movieCards = Array.from(activeGrid.querySelectorAll(".movie-card"));
            const filteredCards = movieCards.filter((card) => {
                const titleMatches = normalize(card.dataset.title || "").includes(searchTerm);
                const genreMatches = !selectedGenre || normalize(card.dataset.genre || "") === selectedGenre;
                return titleMatches && genreMatches;
            });
            const pageCount = Math.max(1, Math.ceil(filteredCards.length / pageSize));
            if (currentPage >= pageCount) currentPage = pageCount - 1;
            movieCards.forEach((card) => { card.hidden = true; });
            filteredCards.slice(currentPage * pageSize, (currentPage + 1) * pageSize).forEach((card) => {
                card.hidden = false;
            });
            const filterEmpty = activeGrid.querySelector(".filter-empty");
            if (filterEmpty) filterEmpty.hidden = filteredCards.length > 0 || (searchTerm === "" && selectedGenre === "");
            pageDots.replaceChildren();
            for (let page = 0; page < pageCount; page++) {
                const dot = document.createElement("button");
                dot.type = "button";
                dot.className = "movie-dot" + (page === currentPage ? " active" : "");
                dot.setAttribute("aria-label", "Trang " + (page + 1));
                dot.setAttribute("aria-current", page === currentPage ? "page" : "false");
                dot.addEventListener("click", () => {
                    currentPage = page;
                    renderMovies();
                });
                pageDots.appendChild(dot);
            }
            pageButtons[0].disabled = currentPage === 0;
            pageButtons[1].disabled = currentPage === pageCount - 1;
            moviePagination.hidden = filteredCards.length === 0;
        }

        catalogTabs.forEach((tab) => {
            tab.addEventListener("click", () => {
                catalogTabs.forEach((item) => {
                    const selected = item === tab;
                    item.classList.toggle("active", selected);
                    item.setAttribute("aria-pressed", String(selected));
                });
                activeGrid.hidden = true;
                activeGrid = document.getElementById(tab.dataset.catalog);
                activeGrid.hidden = false;
                currentPage = 0;
                renderMovies();
            });
        });

        pageButtons.forEach((button) => {
            button.addEventListener("click", () => {
                currentPage += button.dataset.page === "next" ? 1 : -1;
                renderMovies();
            });
        });

        renderMovies();
    }());
</script>
</body>
</html>





