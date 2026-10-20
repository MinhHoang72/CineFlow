<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Chi tiết khách hàng | CineFlow Admin</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/dashboard.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/customers.css">
</head>
<body>
<div class="cf-manager-shell">
    <jsp:include page="/WEB-INF/fragments/adminSidebar.jspf" />
    <div class="cf-manager-main">
        <jsp:include page="/WEB-INF/fragments/adminTopbar.jspf" />
        <main class="cf-content">
            <header class="cf-page-header">
                <div>
                    <span class="cf-eyebrow">QUẢN LÝ KHÁCH HÀNG</span>
                    <h1>Chi tiết khách hàng #${customer.userId}</h1>
                </div>
                <a class="cf-btn cf-btn-secondary" href="${pageContext.request.contextPath}/admin/customers">← Danh sách khách hàng</a>
            </header>
            <div class="cf-card mb-4">
                <div class="row">
                    <div class="col-md-6">
                        <p><strong>Họ và tên:</strong> <c:out value="${customer.fullName}" /></p>
                        <p><strong>Tài khoản:</strong> <c:out value="${customer.account}" /></p>
                        <p><strong>Email:</strong> <c:out value="${customer.email}" /></p>
                    </div>
                    <div class="col-md-6">
                        <p><strong>Số điện thoại:</strong> <c:out value="${customer.phone}" default="Chưa cập nhật" /></p>
                        <p><strong>Trạng thái:</strong> <span class="badge ${customer.status == 'ACTIVE' ? 'bg-success' : 'bg-danger'}"><c:out value="${customer.status}" /></span></p>
                    </div>
                </div>
            </div>
        </main>
    </div>
</div>
</body>
</html>
