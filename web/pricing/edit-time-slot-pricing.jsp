<%@page contentType="text/html"
        pageEncoding="UTF-8"%>

<!DOCTYPE html>

<html>

<head>

    <meta charset="UTF-8">

    <title>
        Edit Time-Slot Pricing
    </title>

</head>

<body>


<h1>
    Edit Time-Slot Pricing
</h1>


<form
    action="${pageContext.request.contextPath}/pricing/time-slots/update"
    method="post">

    <input
        type="hidden"
        name="id"
        value="${rule.timeSlotPricingId}">

    <input
        type="hidden"
        name="branchId"
        value="${rule.branchId}">


    <div>

        Slot Name:

        <input
            type="text"
            name="slotName"
            value="${rule.slotName}"
            required>

    </div>


    <br>


    <div>

        Start Time:

        <input
            type="time"
            name="startTime"
            value="${rule.startTimeFormatted}"
            required>

    </div>


    <br>


    <div>

        End Time:

        <input
            type="time"
            name="endTime"
            value="${rule.endTimeFormatted}"
            required>

    </div>


    <br>


    <div>

        Surcharge:

        <input
            type="number"
            name="surcharge"
            value="${rule.surcharge}"
            min="0"
            step="1000"
            required>

    </div>


    <br>


    <button type="submit">
        Save Changes
    </button>


    <a href="${pageContext.request.contextPath}/pricing/time-slots">
        Cancel
    </a>

</form>


</body>

</html>