<%@page contentType="text/html"
        pageEncoding="UTF-8"%>

<%@taglib prefix="c"
          uri="http://java.sun.com/jsp/jstl/core"%>

<!DOCTYPE html>

<html>

    <head>

        <meta charset="UTF-8">

        <title>
            Showtime Management
        </title>

    </head>

    <body>


        <h1>
            Showtime Management
        </h1>


        <c:if test="${not empty sessionScope.message}">

            <p style="color: green;">
                ${sessionScope.message}
            </p>

            <c:remove
                var="message"
                scope="session"/>

        </c:if>


        <c:if test="${not empty sessionScope.error}">

            <p style="color: red;">
                ${sessionScope.error}
            </p>

            <c:remove
                var="error"
                scope="session"/>

        </c:if>


        <h2>
            Create Showtime
        </h2>


        <form
            action="${pageContext.request.contextPath}/showtimes/create"
            method="post">

            Movie ID:

            <select name="movieId"
                    required>

                <option value="">
                    -- Select Movie --
                </option>

                <c:forEach var="m"
                           items="${movies}">

                    <option value="${m.movieId}">
                        ${m.title}
                        (${m.duration} min)
                    </option>

                </c:forEach>

            </select>
            <br><br>


            Hall ID:

            <select name="hallId"
                    required>

                <option value="">
                    -- Select Hall --
                </option>

                <c:forEach var="h"
                           items="${halls}">

                    <option value="${h.hallId}">
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
                required>

            <br><br>


            <button type="submit">
                Create Showtime
            </button>

        </form>


        <hr>


        <h2>
            Showtime List
        </h2>


        <table
            border="1"
            cellpadding="8">

            <tr>

                <th>ID</th>

                <th>Movie</th>

                <th>Hall</th>

                <th>Start</th>

                <th>End</th>

                <th>Buffer</th>

                <th>Status</th>

                <th>Action</th>
                
                <th>Action</th>

            </tr>


            <c:forEach
                var="s"
                items="${showtimes}">

                <tr>

                    <td>
                        ${s.showtimeId}
                    </td>

                    <td>
                        ${s.movieTitle}
                    </td>

                    <td>
                        ${s.hallId}
                    </td>

                    <td>
                        ${s.startTimeFormatted}
                    </td>

                    <td>
                        ${s.endTimeFormatted}
                    </td>

                    <td>
                        ${s.cleaningBufferMinutes}
                        min
                    </td>

                    <td>
                        ${s.status}
                    </td>

                    <td>

                        <c:if test="${s.status == 'SCHEDULED'}">

                            <a href="${pageContext.request.contextPath}/showtimes/edit?id=${s.showtimeId}">
                                Edit
                            </a>

                        </c:if>

                    </td>
                    <td>
                        <c:if test="${s.status == 'SCHEDULED'}">

                            <form
                                action="${pageContext.request.contextPath}/showtimes/cancel"
                                method="post"
                                style="display:inline;">

                                <input
                                    type="hidden"
                                    name="showtimeId"
                                    value="${s.showtimeId}">

                                <button
                                    type="submit"
                                    onclick="return confirm('Cancel this showtime?');">

                                    Cancel

                                </button>

                            </form>

                        </c:if>
                    </td>

                </tr>

            </c:forEach>

        </table>


    </body>

</html>