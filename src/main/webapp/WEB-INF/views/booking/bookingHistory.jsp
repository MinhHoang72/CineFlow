<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" value="Lịch sử đặt vé" scope="request"/>
<c:set var="pageStep" value="🕘" scope="request"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<c:choose>
    <c:when test="${empty bookings}">
        <div class="alert alert-info">Bạn chưa có đơn đặt vé nào.</div>
        <a href="${ctx}/booking" class="btn btn-primary">Đặt vé ngay</a>
    </c:when>
    <c:otherwise>
        <div class="card">
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light">
                    <tr><th>Mã đơn</th><th>Phim</th><th>Suất chiếu</th><th>Ghế</th>
                        <th class="text-end">Tổng tiền</th><th>Trạng thái</th><th></th></tr>
                    </thead>
                    <tbody>
                    <c:forEach var="b" items="${bookings}">
                        <tr>
                            <td><strong><c:out value="${b.code}"/></strong><br>
                                <small class="text-muted">Đặt lúc <fmt:formatDate value="${b.createdAt}" pattern="HH:mm dd/MM/yyyy"/></small></td>
                            <td><c:out value="${b.showtime.movieTitle}"/><br>
                                <small class="text-muted"><c:out value="${b.showtime.branchName}"/></small></td>
                            <td><fmt:formatDate value="${b.showtime.startTime}" pattern="HH:mm dd/MM/yyyy"/></td>
                            <td>${b.seatLabels}</td>
                            <td class="text-end"><fmt:formatNumber value="${b.totalAmount}" pattern="#,##0"/> đ</td>
                            <td><span class="badge ${b.statusBadge}">${b.statusLabel}</span></td>
                            <td class="text-end">
                                <c:choose>
                                    <c:when test="${b.holdActive}">
                                        <a href="${ctx}/booking?action=checkout&bookingId=${b.id}" class="btn btn-warning btn-sm">Thanh toán</a>
                                    </c:when>
                                    <c:otherwise>
                                        <a href="${ctx}/booking?action=detail&bookingId=${b.id}" class="btn btn-outline-primary btn-sm">Chi tiết</a>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
