<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="Bán vé tại quầy" scope="request"/>
<c:set var="pageStep" value="1" scope="request"/>
<%@ include file="/WEB-INF/fragments/header.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<c:choose>
    <c:when test="${empty dateOptions}">
        <div class="alert alert-warning">Rạp của bạn hiện không có suất chiếu nào đang mở bán.</div>
    </c:when>
    <c:otherwise>
        <div class="d-flex flex-wrap gap-2 mb-4">
            <c:forEach var="d" items="${dateOptions}">
                <a href="${ctx}/staff/counter?date=${d.key}"
                   class="btn btn-sm ${d.key == selectedDate ? 'btn-primary' : 'btn-outline-primary'}">${d.value}</a>
            </c:forEach>
        </div>

        <c:forEach var="entry" items="${showtimesByMovie}">
            <div class="card mb-3">
                <div class="card-body">
                    <h6 class="mb-3"><c:out value="${entry.key}"/></h6>
                    <div class="d-flex flex-wrap gap-2">
                        <c:forEach var="s" items="${entry.value}">
                            <a href="${ctx}/staff/counter?action=seats&showtimeId=${s.id}"
                               class="btn btn-outline-dark text-center" style="min-width: 110px">
                                <strong><fmt:formatDate value="${s.startTime}" pattern="HH:mm"/></strong><br>
                                <small><c:out value="${s.hallName}"/> · ${s.hallType}</small>
                            </a>
                        </c:forEach>
                    </div>
                </div>
            </div>
        </c:forEach>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/fragments/footer.jspf" %>
