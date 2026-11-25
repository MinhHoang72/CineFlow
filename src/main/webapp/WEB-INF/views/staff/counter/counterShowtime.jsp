<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%-- Cấu hình trang: bán vé tại quầy, bước 1, header nhân viên --%>
<c:set var="pageTitle" value="Bán vé tại quầy" scope="request"/>
<c:set var="pageStep" value="1" scope="request"/>
<c:set var="activeNav" value="counter" scope="request"/>
<%@ include file="/WEB-INF/fragments/header.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<%-- Rạp không có suất mở bán thì cảnh báo --%>
<c:choose>
    <c:when test="${empty dateOptions}">
        <div class="alert alert-warning">Rạp của bạn hiện không có suất chiếu nào đang mở bán.</div>
    </c:when>
    <c:otherwise>
        <%-- [1] HÀNG NÚT NGÀY: link /staff/counter?date=... --%>
        <div class="d-flex flex-wrap gap-2 mb-4">
            <c:forEach var="d" items="${dateOptions}">
                <a href="${ctx}/staff/counter?date=${d.key}"
                   class="btn btn-sm ${d.key == selectedDate ? 'btn-primary' : 'btn-outline-primary'}">${d.value}</a>
            </c:forEach>
        </div>

        <%-- [2] MỖI PHIM MỘT THẺ: các ô giờ chiếu, bấm → /staff/counter?action=seats (sơ đồ ghế) --%>
        <c:forEach var="entry" items="${showtimesByMovie}">
            <div class="card mb-3">
                <div class="card-body">
                    <h6 class="mb-3"><c:out value="${entry.key}"/></h6>
                    <div class="d-flex flex-wrap gap-2">
                        <c:forEach var="s" items="${entry.value}">
                            <a href="${ctx}/staff/counter?action=seats&showtimeId=${s.id}"
                               class="btn btn-outline-light text-center" style="min-width: 110px">
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
