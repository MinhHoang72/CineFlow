<%@page contentType="text/html"
        pageEncoding="UTF-8"%>

<!DOCTYPE html>

<html>

<head>
    <meta charset="UTF-8">
    <title>Edit Seat Surcharge</title>
</head>

<body>

<h1>Edit Seat Surcharge</h1>

<form
    action="${pageContext.request.contextPath}/pricing/seat-surcharges/update"
    method="post">

    <input
        type="hidden"
        name="id"
        value="${rule.surchargeId}">


    Seat Type:

    <input
        type="text"
        name="seatType"
        value="${rule.seatType}"
        readonly>


    <br><br>


    Name:

    <input
        type="text"
        name="surchargeName"
        value="${rule.surchargeName}"
        required>


    <br><br>


    Amount:

    <input
        type="number"
        name="amount"
        value="${rule.amount}"
        min="0"
        step="1000"
        required>


    <br><br>


    <button type="submit">
        Save Changes
    </button>


    <a href="${pageContext.request.contextPath}/pricing/seat-surcharges">
        Cancel
    </a>

</form>

</body>

</html>