<%@page contentType="text/html"
        pageEncoding="UTF-8"%>

<%@taglib prefix="c"
          uri="http://java.sun.com/jsp/jstl/core"%>

<!DOCTYPE html>

<html>

<head>

    <meta charset="UTF-8">

    <meta name="viewport"
          content="width=device-width, initial-scale=1.0">

    <title>Showtime Management</title>


    <!-- Common CSS -->
    <link rel="stylesheet"
      href="${pageContext.request.contextPath}/css/common/layout.css">

<link rel="stylesheet"
      href="${pageContext.request.contextPath}/css/common/button.css">

<link rel="stylesheet"
      href="${pageContext.request.contextPath}/css/common/form.css">

<link rel="stylesheet"
      href="${pageContext.request.contextPath}/css/common/table.css">

<link rel="stylesheet"
      href="${pageContext.request.contextPath}/css/common/badge.css">

<link rel="stylesheet"
      href="${pageContext.request.contextPath}/css/common/alert.css">

<link rel="stylesheet"
      href="${pageContext.request.contextPath}/css/management/showtime-pricing.css">


    <!-- CSS riêng cho Showtime / Pricing -->
    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/css/showtime-pricing/showtime-pricing.css">

</head>


<body>

<div class="page-shell">


    <!-- =====================================================
         BREADCRUMB
         ===================================================== -->

    <nav class="cf-breadcrumb-nav">

        <a href="${pageContext.request.contextPath}/">
            Home
        </a>

        <span class="cf-breadcrumb-sep">
            /
        </span>

        <span class="cf-breadcrumb-current">
            Showtime Management
        </span>

    </nav>


    <!-- =====================================================
         MAIN CONTENT
         ===================================================== -->

    <main class="cf-management-page">


        <!-- PAGE HEADER -->

        <div class="cf-page-heading">

            <div class="cf-page-title">

                <h1>
                    Showtime Management
                </h1>

            </div>

            <p class="cf-page-description">
                Create, manage and monitor cinema showtimes
            </p>

        </div>


        <!-- =================================================
             MESSAGE
             ================================================= -->

        <c:if test="${not empty sessionScope.message}">

            <div class="cf-alert cf-alert-success">

                ${sessionScope.message}

            </div>

            <c:remove
                var="message"
                scope="session"/>

        </c:if>


        <c:if test="${not empty sessionScope.error}">

            <div class="cf-alert cf-alert-error">

                ${sessionScope.error}

            </div>

            <c:remove
                var="error"
                scope="session"/>

        </c:if>


        <!-- =================================================
             CREATE SHOWTIME
             ================================================= -->

        <section class="cf-card">


            <div class="cf-card-header">

                <div>

                    <h2 class="cf-card-title">
                        Create Showtime
                    </h2>

                    <p class="cf-card-subtitle">
                        Select movie, hall and screening time
                    </p>

                </div>

            </div>


            <div class="cf-card-body">


                <form
                    action="${pageContext.request.contextPath}/showtimes/create"
                    method="post">


                    <div class="cf-form-grid">


                        <!-- Movie -->

                        <div class="cf-field">

                            <label for="movieId">

                                Movie

                                <span>*</span>

                            </label>


                            <select
                                id="movieId"
                                name="movieId"
                                required>

                                <option value="">
                                    -- Select Movie --
                                </option>


                                <c:forEach
                                    var="m"
                                    items="${movies}">

                                    <option value="${m.movieId}">

                                        ${m.title}
                                        (${m.duration} min)

                                    </option>

                                </c:forEach>

                            </select>


                            <div class="cf-help">
                                Only movies available at this branch are shown.
                            </div>

                        </div>


                        <!-- Hall -->

                        <div class="cf-field">

                            <label for="hallId">

                                Hall

                                <span>*</span>

                            </label>


                            <select
                                id="hallId"
                                name="hallId"
                                required>

                                <option value="">
                                    -- Select Hall --
                                </option>


                                <c:forEach
                                    var="h"
                                    items="${halls}">

                                    <option value="${h.hallId}">

                                        ${h.hallName}
                                        - ${h.hallType}

                                    </option>

                                </c:forEach>

                            </select>


                            <div class="cf-help">
                                Only available halls are shown.
                            </div>

                        </div>


                        <!-- Start Time -->

                        <div class="cf-field cf-field-full">

                            <label for="startTime">

                                Start Time

                                <span>*</span>

                            </label>


                            <input
                                id="startTime"
                                type="datetime-local"
                                name="startTime"
                                required>


                            <div class="cf-help">
                                End time is calculated automatically
                                from the movie duration.
                            </div>

                        </div>


                    </div>


                    <!-- Actions -->

                    <div class="cf-action-row"
                         style="margin-top: 20px;">

                        <button
                            type="submit"
                            class="cf-btn cf-btn-primary">

                            Create Showtime

                        </button>

                    </div>


                </form>


            </div>

        </section>


        <!-- =================================================
             SHOWTIME LIST
             ================================================= -->

        <section class="cf-card">


            <div class="cf-card-header">

                <div>

                    <h2 class="cf-card-title">
                        Showtime List
                    </h2>

                    <p class="cf-card-subtitle">
                        Current cinema schedule
                    </p>

                </div>

            </div>


            <div class="cf-card-body">


                <!-- EMPTY -->

                <c:if test="${empty showtimes}">

                    <div class="cf-empty-state">

                        <strong>
                            No showtimes found
                        </strong>

                        There are currently no showtimes
                        scheduled for this branch.

                    </div>

                </c:if>


                <!-- TABLE -->

                <c:if test="${not empty showtimes}">


                    <div class="cf-table-wrap">


                        <table class="cf-table">


                            <thead>

                            <tr>

                                <th>ID</th>

                                <th>Movie</th>

                                <th>Hall</th>

                                <th>Start Time</th>

                                <th>End Time</th>

                                <th>Cleaning Buffer</th>

                                <th>Status</th>

                                <th>Actions</th>

                            </tr>

                            </thead>


                            <tbody>


                            <c:forEach
                                var="s"
                                items="${showtimes}">


                                <tr>


                                    <!-- ID -->

                                    <td>
                                        #${s.showtimeId}
                                    </td>


                                    <!-- Movie -->

                                    <td>

                                        <strong>
                                            ${s.movieTitle}
                                        </strong>

                                    </td>


                                    <!-- Hall -->

                                    <td>

                                        ${s.hallName} - ${s.hallType}

                                    </td>


                                    <!-- Start -->

                                    <td>

                                        ${s.startTimeFormatted}

                                    </td>


                                    <!-- End -->

                                    <td>

                                        ${s.endTimeFormatted}

                                    </td>


                                    <!-- Buffer -->

                                    <td>

                                        ${s.cleaningBufferMinutes}
                                        min

                                    </td>


                                    <!-- Status -->

                                    <td>


                                        <c:choose>


                                            <c:when test="${s.status == 'SCHEDULED'}">

                                                <span class="cf-badge cf-badge-info">

                                                    Scheduled

                                                </span>

                                            </c:when>


                                            <c:when test="${s.status == 'ONGOING'}">

                                                <span class="cf-badge cf-badge-warning">

                                                    Ongoing

                                                </span>

                                            </c:when>


                                            <c:when test="${s.status == 'COMPLETED'}">

                                                <span class="cf-badge cf-badge-success">

                                                    Completed

                                                </span>

                                            </c:when>


                                            <c:when test="${s.status == 'CANCELLED'}">

                                                <span class="cf-badge cf-badge-danger">

                                                    Cancelled

                                                </span>

                                            </c:when>


                                            <c:otherwise>

                                                <span class="cf-badge cf-badge-muted">

                                                    ${s.status}

                                                </span>

                                            </c:otherwise>


                                        </c:choose>


                                    </td>


                                    <!-- ACTION -->

                                    <td>


                                        <div class="cf-table-actions">


                                            <!-- View seats -->

                                            <a
                                                href="${pageContext.request.contextPath}/showtimes/seats?id=${s.showtimeId}"
                                                class="cf-btn cf-btn-secondary cf-btn-sm">

                                                View Seats

                                            </a>


                                            <!-- Scheduled actions -->

                                            <c:if test="${s.status == 'SCHEDULED'}">


                                                <!-- Edit -->

                                                <a
                                                    href="${pageContext.request.contextPath}/showtimes/edit?id=${s.showtimeId}"
                                                    class="cf-btn cf-btn-secondary cf-btn-sm">

                                                    Edit

                                                </a>


                                                <!-- Cancel -->

                                                <form
                                                    action="${pageContext.request.contextPath}/showtimes/cancel"
                                                    method="post"
                                                    style="margin: 0;">


                                                    <input
                                                        type="hidden"
                                                        name="showtimeId"
                                                        value="${s.showtimeId}">


                                                    <button
                                                        type="submit"
                                                        class="cf-btn cf-btn-danger cf-btn-sm"
                                                        onclick="return confirm('Cancel this showtime?');">

                                                        Cancel

                                                    </button>


                                                </form>


                                            </c:if>


                                        </div>


                                    </td>


                                </tr>


                            </c:forEach>


                            </tbody>


                        </table>


                    </div>


                </c:if>


            </div>

        </section>


    </main>


</div>


</body>

</html>