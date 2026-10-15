<%@page contentType="text/html"
        pageEncoding="UTF-8"%>

<%@taglib prefix="c"
          uri="http://java.sun.com/jsp/jstl/core"%>

<!DOCTYPE html>

<html>

    <head>

        <meta charset="UTF-8">

        <title>
            Pricing Management
        </title>

    </head>

    <body>

        <h1>Pricing Management</h1>


        <c:if test="${not empty sessionScope.message}">

            <p style="color: green;">
                ${sessionScope.message}
            </p>

            <c:remove var="message"
                      scope="session"/>

        </c:if>


        <c:if test="${not empty sessionScope.error}">

            <p style="color: red;">
                ${sessionScope.error}
            </p>

            <c:remove var="error"
                      scope="session"/>

        </c:if>


        <h2>Current Base Price</h2>


        <c:choose>

            <c:when test="${not empty currentPrice}">

                <p>
                    Current price:
                    <strong>
                       ${currentPrice.basePriceFormatted}
                    </strong>
                </p>

                <p>
                    Effective from:
                    ${currentPrice.effectiveFromFormatted}
                </p>

            </c:when>

            <c:otherwise>

                <p>
                    No base price configured.
                </p>

            </c:otherwise>

        </c:choose>


        <hr>


        <h2>Update Base Price</h2>


        <form action="${pageContext.request.contextPath}/pricing/update"
              method="post">

            <input type="hidden"
                   name="branchId"
                   value="${branchId}">

            <label>
                New Base Price:
            </label>

            <input type="number"
                   name="basePrice"
                   min="0"
                   step="1000"
                   required>

            <button type="submit">
                Update Price
            </button>

        </form>


        <hr>


        <h2>Price History</h2>


        <table border="1"
               cellpadding="8">

            <tr>

                <th>ID</th>

                <th>Base Price</th>

                <th>Effective From</th>

                <th>Effective To</th>

                <th>Updated By</th>

            </tr>


            <c:forEach var="p"
                       items="${history}">

                <tr>

                    <td>
                        ${p.branchPricingId}
                    </td>

                    <td>
                        ${p.basePriceFormatted}
                    </td>

                    <td>
                        ${p.effectiveFromFormatted}
                    </td>

                    <td>

                        <c:choose>

                            <c:when test="${empty p.effectiveTo}">
                                Current
                            </c:when>

                            <c:otherwise>
                                ${p.effectiveToFormatted}
                            </c:otherwise>

                        </c:choose>

                    </td>

                    <td>
                        ${p.updatedBy}
                    </td>

                </tr>

            </c:forEach>

        </table>


    </body>

</html>