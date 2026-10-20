<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%
    request.setAttribute("pageTitle", "Tạo suất chiếu mới");
    request.setAttribute("pageDescription", "Thiết lập suất chiếu mới cho phim tại phòng chiếu được chọn.");
    request.setAttribute("activeTab", "showtimes");
%>
<%@ include file="/WEB-INF/fragments/cinemaBegin.jspf" %>
<section class="st-panel" id="create-showtime" aria-labelledby="create-title">
    <div class="st-panel-heading">
        <div>
            <h2 id="create-title">Tạo suất chiếu mới</h2>
            <p>Chọn phim, phòng chiếu và thời gian bắt đầu.</p>
        </div>
        <a class="st-link" href="${pageContext.request.contextPath}/showtimes">← Danh sách suất chiếu</a>
    </div>
    <form action="${pageContext.request.contextPath}/showtimes/create" method="post" class="st-create-form">
        <div class="st-fields">
            <div class="st-field">
                <label for="movieId">Phim <span>*</span></label>
                <select id="movieId" name="movieId" required>
                    <option value="">Chọn phim</option>
                    <c:forEach var="m" items="${movies}">
                        <option value="${m.movieId}"><c:out value="${m.title}" /> · ${m.duration} phút</option>
                    </c:forEach>
                </select>
            </div>
            <div class="st-field">
                <label for="hallId">Phòng chiếu <span>*</span></label>
                <select id="hallId" name="hallId" required>
                    <option value="">Chọn phòng chiếu</option>
                    <c:forEach var="h" items="${halls}">
                        <option value="${h.hallId}"><c:out value="${h.hallName}" /> · <c:out value="${h.hallType}" /></option>
                    </c:forEach>
                </select>
            </div>
            <div class="st-field">
                <label for="startTime">Thời gian bắt đầu <span>*</span></label>
                <input id="startTime" name="startTime" type="datetime-local" required>
            </div>
        </div>
        <div class="st-form-footer">
            <p>Thời gian kết thúc được tính tự động theo thời lượng phim (kèm buffer dọn phòng).</p>
            <div class="st-row-actions">
                <a class="st-button st-button-muted" href="${pageContext.request.contextPath}/showtimes">Quay lại</a>
                <button type="submit" class="st-button st-button-primary" ${empty movies or empty halls ? 'disabled' : ''}>Tạo suất chiếu</button>
            </div>
        </div>
        <c:if test="${empty movies or empty halls}">
            <p class="st-prerequisite">Cần có phim được phân phối và phòng sẵn sàng để tạo suất chiếu.</p>
        </c:if>
    </form>
</section>
<%@ include file="/WEB-INF/fragments/cinemaEnd.jspf" %>
