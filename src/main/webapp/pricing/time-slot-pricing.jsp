<%@page contentType="text/html"
        pageEncoding="UTF-8"%>

<%@taglib prefix="c"
          uri="http://java.sun.com/jsp/jstl/core"%>

<!DOCTYPE html>

<html>

    <head>

        <meta charset="UTF-8">

        <title>
            Time-Slot Pricing
        </title>

    </head>

    <body>


        <h1>
            Time-Slot Pricing
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
            Current Pricing Rules
        </h2>


        <table border="1"
               cellpadding="8">

            <tr>

                <th>
                    Slot
                </th>

                <th>
                    Start
                </th>

                <th>
                    End
                </th>

                <th>
                    Surcharge
                </th>

                <th>
                    Effective From
                </th>

                <th>
                    Action
                </th>

                <th>Action</th>

            </tr>


            <c:forEach
                var="r"
                items="${rules}">

                <tr>

                    <td>
                        ${r.slotName}
                    </td>

                    <td>
                        ${r.startTimeFormatted}
                    </td>

                    <td>
                        ${r.endTimeFormatted}
                    </td>

                    <td>
                        ${r.surchargeFormatted}
                    </td>

                    <td>
                        ${r.effectiveFromFormatted}
                    </td>

                    <td>

                        <a href="${pageContext.request.contextPath}/pricing/time-slots/edit?id=${r.timeSlotPricingId}">
                            Edit
                        </a>

                    </td>
                    <td>

                       
                        <form
                            action="${pageContext.request.contextPath}/pricing/time-slots/delete"
                            method="post"
                            style="display:inline;">

                            <input
                                type="hidden"
                                name="id"
                                value="${r.timeSlotPricingId}">

                            <button
                                type="submit"
                                onclick="return confirm('Remove this pricing rule?');">
                                Delete
                            </button>

                        </form>

                    </td>

                </tr>

            </c:forEach>

        </table>


        <hr>


        <h2>
            Add Time-Slot Pricing
        </h2>


        <form
            action="${pageContext.request.contextPath}/pricing/time-slots/save"
            method="post">

            <input
                type="hidden"
                name="centreId"
                value="${centreId}">


            <div>

                Slot Name:

                <input
                    type="text"
                    name="slotName"
                    required>

            </div>


            <br>


            <div>

                Start Time:

                <input
                    type="time"
                    name="startTime"
                    required>

            </div>


            <br>


            <div>

                End Time:

                <input
                    type="time"
                    name="endTime"
                    required>

            </div>


            <br>


            <div>

                Surcharge:

                <input
                    type="number"
                    name="surcharge"
                    min="0"
                    step="1000"
                    required>

            </div>


            <br>


            <button type="submit">
                Add Pricing Rule
            </button>


        </form>


        <hr>


        <h2>
            Time-Slot Pricing History
        </h2>


        <table border="1"
               cellpadding="8">

            <tr>

                <th>
                    ID
                </th>

                <th>
                    Slot
                </th>

                <th>
                    Time
                </th>

                <th>
                    Surcharge
                </th>

                <th>
                    Effective From
                </th>

                <th>
                    Effective To
                </th>

            </tr>


            <c:forEach
                var="r"
                items="${history}">

                <tr>

                    <td>
                        ${r.timeSlotPricingId}
                    </td>

                    <td>
                        ${r.slotName}
                    </td>

                    <td>

                        ${r.startTimeFormatted}
                        -
                        ${r.endTimeFormatted}

                    </td>

                    <td>
                        ${r.surchargeFormatted}
                    </td>

                    <td>
                        ${r.effectiveFromFormatted}
                    </td>

                    <td>
                        ${r.effectiveToFormatted}
                    </td>

                </tr>

            </c:forEach>


        </table>


    </body>

</html>