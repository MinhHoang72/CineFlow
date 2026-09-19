<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html lang="en">
<head>

    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <title>CineFlow - Cinema Experience</title>

    <style>

        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
            font-family: "Segoe UI", Arial, sans-serif;
        }

        html {
            scroll-behavior: smooth;
        }

        body {
            background: #080808;
            color: white;
        }

        a {
            color: inherit;
            text-decoration: none;
        }

        /* =========================
           NAVBAR
        ========================= */

        .navbar {
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 72px;
            z-index: 1000;

            display: flex;
            align-items: center;

            background: rgba(8, 8, 8, 0.86);
            border-bottom: 1px solid rgba(255,255,255,0.06);

            backdrop-filter: blur(18px);
        }

        .nav-container {
            width: 100%;
            max-width: 1250px;
            margin: auto;
            padding: 0 30px;

            display: flex;
            align-items: center;
            justify-content: space-between;
        }

        .logo {
            font-size: 28px;
            font-weight: 800;
            letter-spacing: -1px;
        }

        .logo span {
            color: #e50914;
        }

        .nav-links {
            display: flex;
            align-items: center;
            gap: 30px;
            margin-left: 50px;
        }

        .nav-links a {
            color: #aaa;
            font-size: 14px;
            transition: 0.25s;
        }

        .nav-links a:hover,
        .nav-links a.active {
            color: white;
        }

        .nav-links a.active {
            position: relative;
        }

        .nav-links a.active::after {
            content: "";

            position: absolute;
            left: 0;
            right: 0;
            bottom: -9px;

            height: 2px;
            background: #e50914;
            border-radius: 10px;
        }

        .nav-right {
            display: flex;
            align-items: center;
            gap: 15px;
        }

        .login-link {
            color: #aaa;
            font-size: 14px;
        }

        .login-link:hover {
            color: white;
        }

        .signup-btn {
            background: #e50914;
            padding: 10px 18px;
            border-radius: 9px;
            font-size: 13px;
            font-weight: 700;
            transition: 0.25s;
        }

        .signup-btn:hover {
            background: #f20d19;
            transform: translateY(-1px);
        }


        /* =========================
           HERO
        ========================= */

        .hero {
            min-height: 720px;
            position: relative;

            display: flex;
            align-items: center;

            overflow: hidden;

            background:
                radial-gradient(
                    circle at 75% 40%,
                    rgba(229, 9, 20, 0.20),
                    transparent 35%
                ),
                linear-gradient(
                    90deg,
                    #080808 0%,
                    rgba(8,8,8,0.88) 42%,
                    rgba(8,8,8,0.45) 100%
                );
        }

        .hero::after {
            content: "";

            position: absolute;
            inset: 0;

            background:
                linear-gradient(
                    to bottom,
                    transparent 65%,
                    #080808 100%
                );

            pointer-events: none;
        }

        .hero-content {
            position: relative;
            z-index: 2;

            width: 100%;
            max-width: 1250px;
            margin: auto;
            padding: 110px 30px 60px;
        }

        .hero-badge {
            display: inline-flex;
            align-items: center;
            gap: 8px;

            padding: 7px 12px;

            background: rgba(229, 9, 20, 0.10);
            border: 1px solid rgba(229, 9, 20, 0.25);

            border-radius: 20px;

            color: #e50914;
            font-size: 11px;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 1px;

            margin-bottom: 22px;
        }

        .hero h1 {
            max-width: 650px;

            font-size: 68px;
            line-height: 1.02;
            letter-spacing: -3px;

            margin-bottom: 22px;
        }

        .hero h1 span {
            color: #e50914;
        }

        .hero-description {
            max-width: 540px;

            color: #999;
            font-size: 16px;
            line-height: 1.7;

            margin-bottom: 32px;
        }

        .hero-buttons {
            display: flex;
            gap: 13px;
        }

        .primary-btn,
        .secondary-btn {
            height: 48px;
            padding: 0 22px;

            display: inline-flex;
            align-items: center;
            justify-content: center;

            border-radius: 10px;

            font-size: 13px;
            font-weight: 700;

            transition: 0.25s;
        }

        .primary-btn {
            background: #e50914;
            box-shadow: 0 10px 25px rgba(229, 9, 20, 0.20);
        }

        .primary-btn:hover {
            background: #f20d19;
            transform: translateY(-2px);
        }

        .secondary-btn {
            background: rgba(255,255,255,0.06);
            border: 1px solid rgba(255,255,255,0.10);
            color: #ddd;
        }

        .secondary-btn:hover {
            background: rgba(255,255,255,0.10);
        }


        /* =========================
           MOVIE SHOWCASE
        ========================= */

        .movie-showcase {
            position: absolute;
            right: 7%;
            top: 145px;

            width: 310px;
            height: 430px;

            transform: rotate(5deg);

            border-radius: 18px;

            background:
                linear-gradient(
                    145deg,
                    #242424,
                    #111
                );

            border: 1px solid rgba(255,255,255,0.12);

            box-shadow:
                0 35px 80px rgba(0,0,0,0.70),
                0 0 70px rgba(229,9,20,0.10);

            overflow: hidden;
        }

        .movie-showcase::before {
            content: "";

            position: absolute;
            inset: 0;

            background:
                radial-gradient(
                    circle at 50% 35%,
                    rgba(229,9,20,0.42),
                    transparent 30%
                ),
                linear-gradient(
                    145deg,
                    rgba(255,255,255,0.08),
                    transparent 45%
                );
        }

        .poster-content {
            position: absolute;
            inset: 0;

            padding: 30px;

            display: flex;
            flex-direction: column;
            justify-content: flex-end;
        }

        .poster-small {
            color: #aaa;
            font-size: 11px;
            letter-spacing: 2px;
            text-transform: uppercase;
        }

        .poster-title {
            font-size: 42px;
            font-weight: 900;
            line-height: 0.95;
            margin: 8px 0;
        }

        .poster-title span {
            color: #e50914;
        }

        .poster-info {
            color: #999;
            font-size: 12px;
        }


        /* =========================
           CONTENT
        ========================= */

        .section {
            max-width: 1250px;
            margin: auto;
            padding: 80px 30px 20px;
        }

        .section-header {
            display: flex;
            align-items: flex-end;
            justify-content: space-between;

            margin-bottom: 25px;
        }

        .section-title {
            font-size: 28px;
            letter-spacing: -1px;
        }

        .section-subtitle {
            color: #777;
            font-size: 13px;
            margin-top: 7px;
        }

        .view-all {
            color: #e50914;
            font-size: 13px;
            font-weight: 600;
        }

        .view-all:hover {
            text-decoration: underline;
        }


        /* =========================
           MOVIE CARDS
        ========================= */

        .movie-grid {
            display: grid;

            grid-template-columns:
                repeat(4, 1fr);

            gap: 20px;
        }

        .movie-card {
            background: #111;

            border: 1px solid rgba(255,255,255,0.06);

            border-radius: 14px;

            overflow: hidden;

            transition: 0.3s;
        }

        .movie-card:hover {
            transform: translateY(-6px);

            border-color:
                rgba(229,9,20,0.35);

            box-shadow:
                0 20px 45px rgba(0,0,0,0.40);
        }

        .poster {
            height: 290px;

            position: relative;

            background:
                linear-gradient(
                    145deg,
                    #292929,
                    #111
                );
        }

        .poster::before {
            content: "";

            position: absolute;
            inset: 0;

            background:
                radial-gradient(
                    circle at 50% 35%,
                    rgba(229,9,20,0.28),
                    transparent 32%
                );
        }

        .movie-number {
            position: absolute;

            top: 14px;
            left: 14px;

            width: 29px;
            height: 29px;

            display: flex;
            align-items: center;
            justify-content: center;

            border-radius: 8px;

            background: rgba(0,0,0,0.65);

            color: white;
            font-size: 12px;
            font-weight: 700;
        }

        .age {
            position: absolute;

            top: 14px;
            right: 14px;

            padding: 5px 7px;

            border-radius: 5px;

            background: #e50914;

            color: white;

            font-size: 10px;
            font-weight: 800;
        }

        .poster-text {
            position: absolute;

            bottom: 20px;
            left: 20px;
            right: 20px;
        }

        .poster-text h3 {
            font-size: 26px;
            line-height: 0.95;
            text-transform: uppercase;
        }

        .poster-text p {
            color: #888;
            font-size: 10px;
            margin-top: 8px;
            letter-spacing: 1px;
        }

        .movie-info {
            padding: 16px;
        }

        .movie-info h3 {
            font-size: 15px;
            margin-bottom: 7px;
        }

        .movie-meta {
            display: flex;
            justify-content: space-between;

            color: #777;
            font-size: 11px;
        }


        /* =========================
           FEATURES
        ========================= */

        .feature-section {
            margin-top: 60px;
        }

        .feature-grid {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 18px;
        }

        .feature-card {
            padding: 28px;

            border-radius: 15px;

            background: #111;

            border: 1px solid rgba(255,255,255,0.06);

            transition: 0.25s;
        }

        .feature-card:hover {
            border-color:
                rgba(229,9,20,0.30);
        }

        .feature-icon {
            width: 45px;
            height: 45px;

            display: flex;
            align-items: center;
            justify-content: center;

            border-radius: 11px;

            background:
                rgba(229,9,20,0.10);

            border: 1px solid
                rgba(229,9,20,0.20);

            color: #e50914;

            font-size: 18px;

            margin-bottom: 20px;
        }

        .feature-card h3 {
            font-size: 16px;
            margin-bottom: 9px;
        }

        .feature-card p {
            color: #777;
            font-size: 13px;
            line-height: 1.6;
        }


        /* =========================
           FOOTER
        ========================= */

        footer {
            margin-top: 100px;

            padding: 45px 30px;

            border-top: 1px solid
                rgba(255,255,255,0.06);

            background: #050505;
        }

        .footer-container {
            max-width: 1250px;
            margin: auto;

            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .footer-logo {
            font-size: 22px;
            font-weight: 800;
        }

        .footer-logo span {
            color: #e50914;
        }

        .footer-text {
            color: #555;
            font-size: 12px;
        }


        /* =========================
           RESPONSIVE
        ========================= */

        @media (max-width: 1050px) {

            .movie-showcase {
                right: 3%;
                width: 260px;
                height: 370px;
            }

            .hero h1 {
                font-size: 55px;
            }

            .movie-grid {
                grid-template-columns:
                    repeat(3, 1fr);
            }
        }

        @media (max-width: 850px) {

            .nav-links {
                display: none;
            }

            .movie-showcase {
                display: none;
            }

            .hero {
                min-height: 620px;
            }

            .hero h1 {
                font-size: 50px;
            }

            .movie-grid {
                grid-template-columns:
                    repeat(2, 1fr);
            }

            .feature-grid {
                grid-template-columns: 1fr;
            }
        }

        @media (max-width: 550px) {

            .nav-container {
                padding: 0 18px;
            }

            .logo {
                font-size: 24px;
            }

            .login-link {
                display: none;
            }

            .hero-content {
                padding-left: 20px;
                padding-right: 20px;
            }

            .hero h1 {
                font-size: 43px;
                letter-spacing: -2px;
            }

            .hero-description {
                font-size: 14px;
            }

            .section {
                padding-left: 20px;
                padding-right: 20px;
            }

            .movie-grid {
                grid-template-columns: 1fr 1fr;
                gap: 12px;
            }

            .poster {
                height: 230px;
            }

            .poster-text h3 {
                font-size: 20px;
            }

            .section-title {
                font-size: 23px;
            }

            .footer-container {
                flex-direction: column;
                gap: 12px;
                text-align: center;
            }
        }

    </style>

</head>


<body>


<!-- =========================
     NAVBAR
========================= -->

<nav class="navbar">

    <div class="nav-container">

        <a href="${pageContext.request.contextPath}/index.jsp"
           class="logo">
            Cine<span>Flow</span>
        </a>


        <div class="nav-links">

            <a href="#home" class="active">
                Home
            </a>

            <a href="#movies">
                Movies
            </a>

            <a href="#showtimes">
                Showtimes
            </a>

            <a href="#branches">
                Cinemas
            </a>

        </div>


        <div class="nav-right">

            <a
                class="login-link"
                href="${pageContext.request.contextPath}/login.jsp">
                Sign In
            </a>

            <a
                class="signup-btn"
                href="${pageContext.request.contextPath}/register.jsp">
                Join CineFlow
            </a>

        </div>

    </div>

</nav>



<!-- =========================
     HERO
========================= -->

<section class="hero" id="home">

    <div class="hero-content">

        <div class="hero-badge">
            ● Your cinema experience
        </div>


        <h1>
            Every movie has<br>
            a <span>moment.</span>
        </h1>


        <p class="hero-description">
            Discover what's playing, explore showtimes
            across our cinemas and find the perfect seat
            for your next movie experience.
        </p>


        <div class="hero-buttons">

            <a
                href="#movies"
                class="primary-btn">
                Explore Movies
            </a>

            <a
                href="#showtimes"
                class="secondary-btn">
                View Showtimes
            </a>

        </div>

    </div>


    <!-- Decorative poster -->

    <div class="movie-showcase">

        <div class="poster-content">

            <div class="poster-small">
                Now showing
            </div>

            <div class="poster-title">
                Cine<br>
                <span>Flow</span>
            </div>

            <div class="poster-info">
                Experience cinema differently
            </div>

        </div>

    </div>

</section>



<!-- =========================
     NOW SHOWING
========================= -->

<section class="section" id="movies">

    <div class="section-header">

        <div>

            <h2 class="section-title">
                Now Showing
            </h2>

            <p class="section-subtitle">
                Movies currently playing in our cinemas
            </p>

        </div>

        <a href="#" class="view-all">
            View all →
        </a>

    </div>


    <div class="movie-grid">


        <!-- Movie 1 -->

        <div class="movie-card">

            <div class="poster">

                <div class="movie-number">
                    01
                </div>

                <div class="age">
                    T16
                </div>

                <div class="poster-text">

                    <h3>
                        Night<br>
                        Run
                    </h3>

                    <p>
                        ACTION · THRILLER
                    </p>

                </div>

            </div>

            <div class="movie-info">

                <h3>
                    Night Run
                </h3>

                <div class="movie-meta">

                    <span>
                        128 min
                    </span>

                    <span>
                        ★ 8.4
                    </span>

                </div>

            </div>

        </div>


        <!-- Movie 2 -->

        <div class="movie-card">

            <div class="poster">

                <div class="movie-number">
                    02
                </div>

                <div class="age">
                    T13
                </div>

                <div class="poster-text">

                    <h3>
                        Beyond<br>
                        Stars
                    </h3>

                    <p>
                        SCI-FI · ADVENTURE
                    </p>

                </div>

            </div>

            <div class="movie-info">

                <h3>
                    Beyond Stars
                </h3>

                <div class="movie-meta">

                    <span>
                        142 min
                    </span>

                    <span>
                        ★ 8.7
                    </span>

                </div>

            </div>

        </div>


        <!-- Movie 3 -->

        <div class="movie-card">

            <div class="poster">

                <div class="movie-number">
                    03
                </div>

                <div class="age">
                    T16
                </div>

                <div class="poster-text">

                    <h3>
                        Last<br>
                        Signal
                    </h3>

                    <p>
                        MYSTERY · DRAMA
                    </p>

                </div>

            </div>

            <div class="movie-info">

                <h3>
                    Last Signal
                </h3>

                <div class="movie-meta">

                    <span>
                        116 min
                    </span>

                    <span>
                        ★ 8.1
                    </span>

                </div>

            </div>

        </div>


        <!-- Movie 4 -->

        <div class="movie-card">

            <div class="poster">

                <div class="movie-number">
                    04
                </div>

                <div class="age">
                    P
                </div>

                <div class="poster-text">

                    <h3>
                        Little<br>
                        World
                    </h3>

                    <p>
                        FAMILY · ANIMATION
                    </p>

                </div>

            </div>

            <div class="movie-info">

                <h3>
                    Little World
                </h3>

                <div class="movie-meta">

                    <span>
                        98 min
                    </span>

                    <span>
                        ★ 8.9
                    </span>

                </div>

            </div>

        </div>


    </div>

</section>



<!-- =========================
     SHOWTIMES
========================= -->

<section class="section" id="showtimes">

    <div class="section-header">

        <div>

            <h2 class="section-title">
                Find Your Showtime
            </h2>

            <p class="section-subtitle">
                Choose a cinema and discover available sessions
            </p>

        </div>

        <a href="#" class="view-all">
            All showtimes →
        </a>

    </div>


    <div class="feature-grid">


        <div class="feature-card">

            <div class="feature-icon">
                ◷
            </div>

            <h3>
                Flexible Showtimes
            </h3>

            <p>
                Browse movie sessions throughout the day
                and find a time that works for you.
            </p>

        </div>


        <div class="feature-card">

            <div class="feature-icon">
                ◉
            </div>

            <h3>
                Multiple Cinemas
            </h3>

            <p>
                Explore showtimes across CineFlow cinema
                branches in different locations.
            </p>

        </div>


        <div class="feature-card">

            <div class="feature-icon">
                ◆
            </div>

            <h3>
                Choose Your Seat
            </h3>

            <p>
                Check seat availability and select your
                preferred seat before watching.
            </p>

        </div>


    </div>

</section>



<!-- =========================
     CINEMAS
========================= -->

<section class="section" id="branches">

    <div class="section-header">

        <div>

            <h2 class="section-title">
                Our Cinemas
            </h2>

            <p class="section-subtitle">
                Find a CineFlow cinema near you
            </p>

        </div>

        <a href="#" class="view-all">
            View branches →
        </a>

    </div>


    <div class="feature-grid">


        <div class="feature-card">

            <div class="feature-icon">
                01
            </div>

            <h3>
                CineFlow Hà Nội
            </h3>

            <p>
                Modern cinema experience with premium
                screens and comfortable seating.
            </p>

        </div>


        <div class="feature-card">

            <div class="feature-icon">
                02
            </div>

            <h3>
                CineFlow Hồ Chí Minh
            </h3>

            <p>
                Enjoy the latest movies with convenient
                showtimes throughout the day.
            </p>

        </div>


        <div class="feature-card">

            <div class="feature-icon">
                03
            </div>

            <h3>
                CineFlow Đà Nẵng
            </h3>

            <p>
                Discover movies and book your cinema
                experience at your nearest branch.
            </p>

        </div>


    </div>

</section>



<!-- =========================
     FOOTER
========================= -->

<footer>

    <div class="footer-container">

        <div class="footer-logo">
            Cine<span>Flow</span>
        </div>

        <div class="footer-text">
            © 2026 CineFlow Multi-Branch Cinema Management System
        </div>

    </div>

</footer>


</body>
</html>