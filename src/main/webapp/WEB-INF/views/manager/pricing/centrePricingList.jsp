<%@ page pageEncoding="UTF-8" %>
<%
    request.setAttribute("pageTitle","Giá vé cơ bản các rạp");
    request.setAttribute("pageDescription","");
    request.setAttribute("activeTab","base");
%>
<%@ include file="/WEB-INF/fragments/cinemaBegin.jspf" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" />
<section class="st-panel">
    <div class="st-panel-heading"><div><h2>Bảng giá theo rạp <span class="st-count" id="cp-result-count" aria-live="polite">${fn:length(centrePrices)}</span></h2></div></div>
    <div class="bp-history-filters" role="search" aria-label="Lọc bảng giá các rạp">
        <div class="st-field"><label for="cp-search">Tên rạp</label><input id="cp-search" type="search" placeholder="Tìm theo tên rạp"></div>
        <div class="st-field"><label for="cp-city">Thành phố</label><select id="cp-city"><option value="">Tất cả</option></select></div>
        <div class="st-field"><label for="cp-price-state">Giá vé</label><select id="cp-price-state"><option value="">Tất cả</option><option value="set">Đã thiết lập</option><option value="unset">Chưa thiết lập</option><option value="scheduled">Có giá đã lên lịch</option></select></div>
        <button id="cp-reset" type="button" class="st-button st-button-muted">Xóa bộ lọc</button>
    </div>
    <div class="st-table-scroll" tabindex="0" role="region" aria-label="Bảng giá cơ bản tất cả rạp">
        <table class="st-table st-table-compact">
            <thead><tr><th scope="col">Rạp / Thành phố</th><th scope="col">Giá hiện tại</th><th scope="col">Người cập nhật</th><th scope="col">Giá đã lên lịch</th><th scope="col">Thao tác</th></tr></thead>
            <tbody><c:forEach var="p" items="${centrePrices}">
                <tr data-centre-id="${p.CentreID}" data-centre-name="<c:out value='${p.CentreName}' />" data-city="<c:out value='${p.City}' />" data-price-set="${p.BasePrice != null}" data-price-scheduled="${p.NextPrice != null}">
                    <td><strong><c:out value="${p.CentreName}" /></strong><br><small><c:out value="${p.City}" /> · <c:choose><c:when test="${p.Status == 'ACTIVE'}">Hoạt động</c:when><c:when test="${p.Status == 'TEMPORARILY_CLOSED'}">Tạm đóng</c:when><c:otherwise>Ngừng hoạt động</c:otherwise></c:choose></small></td>
                    <td><c:choose><c:when test="${p.BasePrice != null}"><strong class="st-money"><fmt:formatNumber value="${p.BasePrice}" maxFractionDigits="0" /> đ</strong><br><small>Từ <fmt:formatDate value="${p.EffectiveFrom}" pattern="dd/MM/yyyy HH:mm" /></small></c:when><c:otherwise><span class="st-status st-status-DRAFT">Chưa thiết lập</span></c:otherwise></c:choose></td>
                    <td><c:out value="${p.UpdatedByName}" default="—" /></td>
                    <td><c:choose><c:when test="${p.NextPrice != null}"><strong class="st-money"><fmt:formatNumber value="${p.NextPrice}" maxFractionDigits="0" /> đ</strong><br><small>Từ <fmt:formatDate value="${p.NextFrom}" pattern="dd/MM/yyyy HH:mm" /></small></c:when><c:otherwise>—</c:otherwise></c:choose></td>
                    <td><a class="st-button st-button-muted" href="${pageContext.request.contextPath}/pricing?centreId=${p.CentreID}&amp;edit=true" aria-label="Sửa giá rạp ${fn:escapeXml(p.CentreName)}">Sửa giá</a></td>
                </tr>
            </c:forEach><tr id="cp-empty" ${not empty centrePrices ? 'hidden' : ''}><td colspan="5" class="bp-empty">${empty centrePrices ? 'Chưa có rạp trong hệ thống' : 'Không có rạp phù hợp'}</td></tr></tbody>
        </table>
    </div>
</section>
<%@ include file="/WEB-INF/fragments/cinemaEnd.jspf" %>
