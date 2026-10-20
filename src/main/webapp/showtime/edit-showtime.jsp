<%@page contentType="text/html"
        pageEncoding="UTF-8"%>

<%@taglib prefix="c"
          uri="http://java.sun.com/jsp/jstl/core"%>

<!DOCTYPE html>

<html>

<head>

    <meta charset="UTF-8">

    <title>Edit Showtime</title>

</head>

<body>

<h1>Edit Showtime</h1>

<form
    action="${pageContext.request.contextPath}/showtimes/update"
    method="post">

    <input
        type="hidden"
        name="showtimeId"
        value="${showtime.showtimeId}">


    Movie:

    <select
        name="movieId"
        required>

        <c:forEach
            var="m"
            items="${movies}">

            <option
                value="${m.movieId}"
                ${m.movieId == showtime.movieId
                    ? 'selected' : ''}>

                ${m.title}
                (${m.duration} min)

            </option>

        </c:forEach>

    </select>


    <br><br>


    Hall:

    <select
        name="hallId"
        required>

        <c:forEach
            var="h"
            items="${halls}">

            <option
                value="${h.hallId}"
                ${h.hallId == showtime.hallId
                    ? 'selected' : ''}>

                ${h.hallName}
                - ${h.hallType}

            </option>

        </c:forEach>

    </select>


    <br><br>


    Start Time:

    <input
        type="datetime-local"
        name="startTime"
        value="${showtime.startTimeInputFormatted}"
        required>


    <br><br>


    <button type="submit">
        Save Changes
    </button>


    <a href="${pageContext.request.contextPath}/showtimes">
        Cancel
    </a>

</form>

</body>

</html>