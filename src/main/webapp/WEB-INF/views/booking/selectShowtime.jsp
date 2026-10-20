<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="Chọn suất chiếu" scope="request"/>
<c:set var="pageStep" value="1" scope="request"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<!-- Buoc 1: Thanh pho -->
<div class="d-flex flex-wrap align-items-center gap-2 mb-2">
    <h5 class="mb-0 me-2">Thành phố</h5>
    <c:forEach var="city" items="${cities}">
        <c:url var="cityUrl" value="/booking">
            <c:param name="city" value="${city.name()}"/>
            <c:if test="${not empty selectedMovieId}"><c:param name="movieId" value="${selectedMovieId}"/></c:if>
        </c:url>
        <a href="${cityUrl}" class="btn btn-sm ${city.name() == selectedCity ? 'btn-primary' : 'btn-outline-primary'}">
            <c:out value="${city.name()}"/> <span class="badge ${city.name() == selectedCity ? 'bg-light text-primary' : 'bg-secondary'}">${city.centreCount()}</span>
        </a>
    </c:forEach>
</div>
<c:if test="${empty cities}">
    <div class="alert alert-warning">Hiện chưa có suất chiếu nào mở bán.</div>
</c:if>

<div class="row g-4 mt-1">
    <!-- Buoc 2: Rap -->
    <div class="col-lg-3">
        <h5>Rạp phim</h5>
        <c:choose>
            <c:when test="${empty selectedCity}">
                <div class="alert alert-info">Chọn thành phố để xem các rạp.</div>
            </c:when>
            <c:when test="${empty centres}">
                <div class="alert alert-warning">Thành phố này chưa có rạp nào mở bán.</div>
            </c:when>
            <c:otherwise>
                <div class="list-group">
                    <c:forEach var="b" items="${centres}">
                        <c:url var="centreUrl" value="/booking">
                            <c:param name="city" value="${selectedCity}"/>
                            <c:param name="branchId" value="${b.centreId}"/>
                            <c:if test="${not empty selectedMovieId}"><c:param name="movieId" value="${selectedMovieId}"/></c:if>
                        </c:url>
                        <a href="${centreUrl}"
                           class="list-group-item list-group-item-action ${b.centreId == selectedBranchId ? 'active' : ''}">
                            <strong><c:out value="${b.centreName}"/></strong><br>
                            <small><c:out value="${b.address}"/></small>
                        </a>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </div>

    <!-- Buoc 3: Phim -->
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
                        <c:url var="movieUrl" value="/booking">
                            <c:param name="city" value="${selectedCity}"/>
                            <c:param name="branchId" value="${selectedBranchId}"/>
                            <c:param name="movieId" value="${m.movieId}"/>
                        </c:url>
                        <a href="${movieUrl}"
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

    <!-- Buoc 4: Ngay + suat chieu -->
    <div class="col-lg-5">
        <h5>Ngày &amp; suất chiếu</h5>
        <c:choose>
            <c:when test="${empty selectedMovieId or empty selectedBranchId}">
                <div class="alert alert-info">Chọn phim để xem lịch chiếu.</div>
            </c:when>
            <c:when test="${empty dateOptions}">
                <div class="alert alert-warning">Phim này không còn suất chiếu tại rạp đã chọn.</div>
            </c:when>
            <c:otherwise>
                <div class="d-flex flex-wrap gap-2 mb-3">
                    <c:forEach var="d" items="${dateOptions}">
                        <c:url var="dateUrl" value="/booking">
                            <c:param name="city" value="${selectedCity}"/>
                            <c:param name="branchId" value="${selectedBranchId}"/>
                            <c:param name="movieId" value="${selectedMovieId}"/>
                            <c:param name="date" value="${d.key}"/>
                        </c:url>
                        <a href="${dateUrl}"
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

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
