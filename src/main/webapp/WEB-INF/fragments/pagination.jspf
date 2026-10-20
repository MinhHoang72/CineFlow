<%@ page pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Phân trang dùng chung theo tiêu chuẩn CINEFLOW_ARCHITECTURE_STANDARD --%>
<c:if test="${totalPages > 1}">
    <nav class="cf-pagination" aria-label="Phân trang">
        <ul class="pagination justify-content-center">
            <li class="page-item ${currentPage <= 1 ? 'disabled' : ''}">
                <a class="page-link" href="?page=${currentPage - 1}&q=${param.q}" aria-label="Trang trước">&laquo;</a>
            </li>
            <c:forEach begin="1" end="${totalPages}" var="p">
                <li class="page-item ${p == currentPage ? 'active' : ''}">
                    <a class="page-link" href="?page=${p}&q=${param.q}">${p}</a>
                </li>
            </c:forEach>
            <li class="page-item ${currentPage >= totalPages ? 'disabled' : ''}">
                <a class="page-link" href="?page=${currentPage + 1}&q=${param.q}" aria-label="Trang sau">&raquo;</a>
            </li>
        </ul>
    </nav>
</c:if>
