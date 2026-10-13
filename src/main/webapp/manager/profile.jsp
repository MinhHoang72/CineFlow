<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.User" %>
<%
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && session.getAttribute("user") instanceof model.User) {
        authRole = ((model.User) session.getAttribute("user")).getRole();
    }
    if (!"MANAGER".equalsIgnoreCase(authRole) && !"ADMIN".equalsIgnoreCase(authRole)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Quản lý (MANAGER) mới có quyền truy cập trang này.");
        return;
    }
    if (request.getAttribute("accountUser") == null) {
        response.sendRedirect(request.getContextPath() + "/account");
        return;
    }
%>
<%!
    private String esc(String value) { return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
%>
<% User user = (User) request.getAttribute("accountUser"); String error = request.getParameter("error"); String updated = request.getParameter("updated"); %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Hồ sơ Manager | CineFlow</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/shell.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/forms.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/alerts.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/profile.css">
</head>
<body>
<div class="cf-manager-shell">
    <jsp:include page="/manager/sidebar.jsp" />
    <div class="cf-manager-main">
        <jsp:include page="/manager/topbar.jsp" />
        <main class="cf-content">
            <div class="cf-page-header">
                <div><p class="cf-breadcrumb">Tài khoản / Hồ sơ của tôi</p><h1>Hồ sơ Manager</h1><p>Cập nhật thông tin tài khoản quản lý của bạn.</p></div>
                <a class="cf-btn cf-btn-secondary" href="${pageContext.request.contextPath}/dashboard">← Về Dashboard</a>
            </div>
            <% if ("true".equals(updated)) { %><div class="cf-alert cf-alert-success">Thông tin hồ sơ đã được cập nhật.</div><% } %>
            <% if ("email".equals(error)) { %><div class="cf-alert cf-alert-error">Email này đã được sử dụng bởi tài khoản khác.</div><% } %>
            <% if ("required".equals(error)) { %><div class="cf-alert cf-alert-error">Họ tên và email là thông tin bắt buộc.</div><% } %>
            <div class="manager-profile-grid">
                <section class="cf-card">
                    <div class="cf-card-header"><div><h2>Thông tin cá nhân</h2><p>Thông tin hiển thị trong hệ thống quản lý.</p></div></div>
                    <div class="cf-card-body">
                        <form method="post" action="${pageContext.request.contextPath}/account" class="cf-form-grid">
                            <div class="cf-field cf-field-full"><label for="fullName">Họ và tên</label><input id="fullName" name="fullName" value="<%= esc(user.getFullName()) %>" required></div>
                            <div class="cf-field"><label for="account">Tên tài khoản</label><input id="account" value="<%= esc(user.getAccount()) %>" disabled></div>
                            <div class="cf-field"><label for="email">Email</label><input id="email" type="email" name="email" value="<%= esc(user.getEmail()) %>" required></div>
                            <div class="cf-field"><label for="phone">Số điện thoại</label><input id="phone" name="phone" value="<%= esc(user.getPhone()) %>"></div>
                            <div class="cf-field"><label for="avatar">Ảnh đại diện (URL)</label><input id="avatar" name="avatar" value="<%= esc(user.getAvatar()) %>"></div>
                            <div class="cf-form-actions cf-field-full"><button class="cf-btn cf-btn-primary" type="submit">Lưu thay đổi</button></div>
                        </form>
                    </div>
                </section>
                <aside class="cf-card manager-profile-summary">
                    <div class="profile-mark"><%= user.getFullName() == null || user.getFullName().isBlank() ? "M" : user.getFullName().substring(0, 1).toUpperCase() %></div>
                    <h2><%= esc(user.getFullName()) %></h2><p class="profile-role">MANAGER</p>
                    <div class="profile-facts"><div><span>Trạng thái</span><strong><%= "ACTIVE".equals(user.getStatus()) ? "Đang hoạt động" : esc(user.getStatus()) %></strong></div><div><span>Email</span><strong><%= user.isEmailVerified() ? "Đã xác minh" : "Chưa xác minh" %></strong></div></div>
                    <a class="cf-btn cf-btn-secondary profile-password" href="${pageContext.request.contextPath}/auth/change-password.jsp">Đổi mật khẩu</a>
                </aside>
            </div>
        </main>
    </div>
</div>
</body>
</html>
