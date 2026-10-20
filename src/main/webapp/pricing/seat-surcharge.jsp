<%@page contentType="text/html"
        pageEncoding="UTF-8"%>

<%@taglib prefix="c"
          uri="http://java.sun.com/jsp/jstl/core"%>

<!DOCTYPE html>

<html>

<head>
    <meta charset="UTF-8">
    <title>Surcharge Management</title>
</head>

<body>

<h1>Surcharge Management</h1>


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


<h2>Current Surcharges</h2>

<table border="1"
       cellpadding="8">

    <tr>
        <th>Target (SEAT / HALL)</th>
        <th>Name</th>
        <th>Surcharge</th>
        <th>Effective From</th>
        <th>Action</th>
    </tr>

    <c:forEach var="s"
               items="${currentRules}">

        <tr>

            <td>
                ${s.surchargeType}: ${s.targetValue}
            </td>

            <td>
                ${s.surchargeName}
            </td>

            <td>
                ${s.amountFormatted}
            </td>

            <td>
                ${s.effectiveFromFormatted}
            </td>

            <td>

                <a href="${pageContext.request.contextPath}/pricing/seat-surcharges/edit?id=${s.surchargeId}">
                    Edit
                </a>

                <form
                    action="${pageContext.request.contextPath}/pricing/seat-surcharges/delete"
                    method="post"
                    style="display:inline;">

                    <input
                        type="hidden"
                        name="id"
                        value="${s.surchargeId}">

                    <button
                        type="submit"
                        onclick="return confirm('Remove this surcharge?');">
                        Delete
                    </button>

                </form>

            </td>

        </tr>

    </c:forEach>

</table>


<hr>


<h2>Surcharge History</h2>

<table border="1"
       cellpadding="8">

    <tr>
        <th>ID</th>
        <th>Target (SEAT / HALL)</th>
        <th>Name</th>
        <th>Amount</th>
        <th>Effective From</th>
        <th>Effective To</th>
        <th>Updated By</th>
    </tr>

    <c:forEach var="s"
               items="${history}">

        <tr>

            <td>
                ${s.surchargeId}
            </td>

            <td>
                ${s.surchargeType}: ${s.targetValue}
            </td>

            <td>
                ${s.surchargeName}
            </td>

            <td>
                ${s.amountFormatted}
            </td>

            <td>
                ${s.effectiveFromFormatted}
            </td>

            <td>
                ${s.effectiveToFormatted}
            </td>

            <td>
                ${s.updatedBy}
            </td>

        </tr>

    </c:forEach>

</table>


</body>

</html>