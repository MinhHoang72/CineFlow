<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page import="model.dto.common.PageDTO, model.entity.Voucher, model.entity.User" %>
<%@ include file="header.jspf" %>
<%
    java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    pageContext.setAttribute("dateFmt", fmt);
%>
<div class="cf-page-header">
    <div>
        <h1>Voucher</h1>
        <c:choose>
            <c:when test="${isAdminScope}">
                <p>Quản lý mã giảm giá áp dụng cho toàn chuỗi rạp CineFlow.</p>
            </c:when>
            <c:otherwise>
                <p>Quản lý mã giảm giá tại chi nhánh của bạn.</p>
            </c:otherwise>
        </c:choose>
    </div>
    <a class="cf-btn cf-btn-primary" href="${baseUrl}/form">Thêm voucher</a>
</div>

<div class="cf-card">
    <!-- FORM 1: TÌM KIẾM & BỘ LỌC (GET) -->
    <form class="vc-filter" method="get" action="${baseUrl}">
        <label>Tìm kiếm
            <input class="cf-form-control" type="text" name="q" maxlength="100" value="${q}" placeholder="Mã hoặc tên">
        </label>
        <label>Trạng thái
            <select class="cf-form-control" name="status">
                <option value="">Tất cả</option>
                <option value="ACTIVE" ${status == 'ACTIVE' ? 'selected' : ''}>Đang hiệu lực</option>
                <option value="INACTIVE" ${status == 'INACTIVE' ? 'selected' : ''}>Tạm ngưng</option>
                <option value="EXPIRED" ${status == 'EXPIRED' ? 'selected' : ''}>Hết hạn</option>
                <option value="EXHAUSTED" ${status == 'EXHAUSTED' ? 'selected' : ''}>Hết lượt</option>
            </select>
        </label>
        <label>Loại
            <select class="cf-form-control" name="type">
                <option value="">Tất cả</option>
                <option value="PERCENT" ${type == 'PERCENT' ? 'selected' : ''}>Phần trăm</option>
                <option value="FIXED" ${type == 'FIXED' ? 'selected' : ''}>Số tiền</option>
            </select>
        </label>
        <button class="cf-btn cf-btn-primary" type="submit">Lọc</button>
        <a class="cf-btn cf-btn-ghost" href="${baseUrl}">Xóa lọc</a>
    </form>

    <!-- BẢNG DANH SÁCH VOUCHER -->
    <div class="cf-table-wrap">
        <table class="cf-table">
            <thead>
                <tr>
                    <th>Mã</th>
                    <th>Tên</th>
                    <th>Giảm</th>
                    <th>Đơn tối thiểu</th>
                    <th>Thời gian</th>
                    <th>Đã dùng</th>
                    <c:if test="${isAdminScope}"><th>Phạm vi</th></c:if>
                    <th>Trạng thái</th>
                    <th>Thao tác</th>
                </tr>
            </thead>
            <tbody>
            <c:if test="${empty result.items}">
                <tr><td colspan="${isAdminScope ? 9 : 8}" class="cf-empty">Không có voucher nào.</td></tr>
            </c:if>

            <c:forEach items="${result.items}" var="v">
                <tr>
                    <td><strong>${v.voucherCode}</strong></td>
                    <td>${v.voucherName}</td>
                    <td>
                        <c:choose>
                            <c:when test="${v.discountType == 'PERCENT'}">
                                ${v.discountValue.stripTrailingZeros().toPlainString()}%
                                <c:if test="${not empty v.maximumDiscount}">
                                    (tối đa <%= money(((model.entity.Voucher)pageContext.findAttribute("v")).getMaximumDiscount()) %>)
                                </c:if>
                            </c:when>
                            <c:otherwise>
                                <%= money(((model.entity.Voucher)pageContext.findAttribute("v")).getDiscountValue()) %>
                            </c:otherwise>
                        </c:choose>
                    </td>
                    <td><%= money(((model.entity.Voucher)pageContext.findAttribute("v")).getMinimumOrderValue()) %></td>
                    <td>
                        <br>
                        
                    </td>
                    <td>${v.usedCount}${not empty v.usageLimit ? ' / '.concat(v.usageLimit) : ''}</td>

                    <c:if test="${isAdminScope}">
                    <td>
                        <c:choose>
                            <c:when test="${v.global}">
                                <span class="vc-scope-badge vc-scope-global">&#127759; Toàn chuỗi</span>
                            </c:when>
                            <c:otherwise>
                                <span class="vc-scope-badge vc-scope-centre">&#127970; ${not empty v.centreName ? v.centreName : 'Chi nhánh #'.concat(v.centreId)}</span>
                            </c:otherwise>
                        </c:choose>
                    </td>
                    </c:if>

                    <td>
                        <c:choose>
                            <c:when test="${v.status != 'ACTIVE'}">
                                <span class="cf-badge cf-badge-muted">Tạm ngưng</span>
                            </c:when>
                            <c:when test="${v.expired}">
                                <span class="cf-badge cf-badge-warning">Hết hạn</span>
                            </c:when>
                            <c:when test="${v.exhausted}">
                                <span class="cf-badge cf-badge-warning">Hết lượt</span>
                            </c:when>
                            <c:otherwise>
                                <span class="cf-badge cf-badge-success">Đang hiệu lực</span>
                            </c:otherwise>
                        </c:choose>
                    </td>

                    <!-- THAO TÁC (Sửa & Form POST đổi trạng thái) -->
                    <td class="cf-table-actions">
                        <a class="cf-btn cf-btn-sm cf-btn-secondary" href="${baseUrl}/form?id=${v.voucherId}">Sửa</a>
                        
                        <form class="vc-inline" method="post" action="${baseUrl}/status">
                            <input type="hidden" name="id" value="${v.voucherId}">
                            <input type="hidden" name="newStatus" value="${v.status == 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'}">
                            <input type="hidden" name="back" value="${backQuery}">
                            <button class="cf-btn cf-btn-sm ${v.status == 'ACTIVE' ? 'cf-btn-danger' : 'cf-btn-primary'}" type="submit">
                                ${v.status == 'ACTIVE' ? 'Tạm ngưng' : 'Kích hoạt'}
                            </button>
                        </form>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>

    <!-- PHÂN TRANG -->
    <div class="vc-pager">
        <span class="info">Hiển thị ${result.from}-${result.to} trên tổng ${result.total} kết quả</span>
        <%= pager(ctx, (String)request.getAttribute("pagerBase"), ((model.dto.common.PageDTO<?>)request.getAttribute("result")).getPage(), ((model.dto.common.PageDTO<?>)request.getAttribute("result")).getTotalPages()) %>
    </div>
</div>

<%@ include file="footer.jspf" %>
