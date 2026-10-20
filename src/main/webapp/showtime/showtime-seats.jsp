<%@page contentType="text/html"
        pageEncoding="UTF-8"%>

<%@taglib prefix="c"
          uri="http://java.sun.com/jsp/jstl/core"%>

<!DOCTYPE html>

<html>

<head>

    <meta charset="UTF-8">

    <title>
        Seat Availability
    </title>

</head>

<body>

<h1>
    Seat Availability
</h1>


<p>
    Showtime ID:
    ${showtime.showtimeId}
</p>


<table
    border="1"
    cellpadding="8">

    <tr>

        <th>Seat</th>

        <th>Type</th>

        <th>Status</th>

    </tr>


    <c:forEach
        var="seat"
        items="${seats}">

        <tr>

            <td>
                ${seat.seatRow}${seat.seatNumber}
            </td>

            <td>
                ${seat.seatType}
            </td>

            <td>

                <c:choose>

                    <c:when test="${seat.availabilityStatus == 'AVAILABLE'}">
                        Available
                    </c:when>

                    <c:when test="${seat.availabilityStatus == 'HELD'}">
                        Held
                    </c:when>

                    <c:otherwise>
                        Booked
                    </c:otherwise>

                </c:choose>

            </td>

        </tr>

    </c:forEach>

</table>


<br>


<a href="${pageContext.request.contextPath}/showtimes">
    Back
</a>


</body>

</html>