<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="Chọn suất chiếu" scope="request"/>
<c:set var="pageStep" value="1" scope="request"/>
<%@ include file="common/header.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<div class="row g-4">
    <!-- Cot 1: Chi nhanh -->
    <div class="col-lg-3">
        <h5>Rạp phim</h5>
        <div class="list-group">
            <c:forEach var="b" items="${branches}">
                <a href="${ctx}/booking?branchId=${b.centreId}"
                   class="list-group-item list-group-item-action ${b.centreId == selectedBranchId ? 'active' : ''}">
                    <strong><c:out value="${b.centreName}"/></strong><br>
                    <small><c:out value="${b.city}"/></small>
                </a>
            </c:forEach>
        </div>
    </div>

    <!-- Cot 2: Phim -->
    <div class="col-lg-4">
        <h5>Phim</h5>
        <c:choose>
            <c:when test="${empty selectedBranchId}">
                <div class="alert alert-info">Chọn một rạp để xem phim đang chiếu.</div>
            </c:when>
            <c:when test="${empty movies}">
                <div class="alert alert-warning">Rạp này hiện chưa có suất chiếu nào.</div>
            </c:when>
            <c:otherwise>
                <div class="list-group">
                    <c:forEach var="m" items="${movies}">
                        <a href="${ctx}/booking?branchId=${selectedBranchId}&movieId=${m.movieId}"
                           class="list-group-item list-group-item-action ${m.movieId == selectedMovieId ? 'active' : ''}">
                            <div class="d-flex justify-content-between">
                                <strong><c:out value="${m.title}"/></strong>
                                <span class="badge bg-secondary align-self-start">${m.ageRating}</span>
                            </div>
                            <small><c:out value="${m.genre}"/> · ${m.duration} phút</small>
                        </a>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </div>

    <!-- Cot 3: Ngay + suat chieu -->
    <div class="col-lg-5">
        <h5>Ngày &amp; suất chiếu</h5>
        <c:choose>
            <c:when test="${empty selectedMovieId}">
                <div class="alert alert-info">Chọn phim để xem lịch chiếu.</div>
            </c:when>
            <c:when test="${empty dateOptions}">
                <div class="alert alert-warning">Phim này không còn suất chiếu tại rạp đã chọn.</div>
            </c:when>
            <c:otherwise>
                <div class="d-flex flex-wrap gap-2 mb-3">
                    <c:forEach var="d" items="${dateOptions}">
                        <a href="${ctx}/booking?branchId=${selectedBranchId}&movieId=${selectedMovieId}&date=${d.key}"
                           class="btn btn-sm ${d.key == selectedDate ? 'btn-primary' : 'btn-outline-primary'}">${d.value}</a>
                    </c:forEach>
                </div>
                <div class="row g-2">
                    <c:forEach var="s" items="${showtimes}">
                        <div class="col-6 col-md-4">
                            <a href="${ctx}/booking?action=select-seats&showtimeId=${s.id}"
                               class="btn btn-outline-dark w-100">
                                <strong><fmt:formatDate value="${s.startTime}" pattern="HH:mm"/></strong><br>
                                <small><c:out value="${s.hallName}"/> · ${s.hallType}</small><br>
                                <small><fmt:formatNumber value="${s.basePrice}" pattern="#,##0"/> đ</small>
                            </a>
                        </div>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%@ include file="common/footer.jspf" %>
