<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.User" %>
<%
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && session.getAttribute("user") instanceof model.User) {
        authRole = ((model.User) session.getAttribute("user")).getRole();
    }
    boolean isAdmin = "ADMIN".equalsIgnoreCase(authRole);
    if (!isAdmin && !"MANAGER".equalsIgnoreCase(authRole)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập trang này.");
        return;
    }
%>
<%!
    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
%>
<%
    request.setAttribute("activeNav", "account");
    request.setAttribute("pageTitle", "Hồ sơ của tôi");
    request.setAttribute("pageBreadcrumb", "Tài khoản");

    User user = (User) request.getAttribute("accountUser");
    if (user == null) {
        user = (User) session.getAttribute("user");
    }
    String error = request.getParameter("error");
    String updated = request.getParameter("updated");
    String defaultName = isAdmin ? "Admin" : "Manager";
    String fullName = user != null && user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : defaultName;
    String initial = fullName.isBlank() ? (isAdmin ? "A" : "M") : fullName.substring(0, 1).toUpperCase();
    String avatar = user != null ? user.getAvatar() : null;
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%= isAdmin ? "Hồ sơ Admin | CineFlow" : "Hồ sơ Manager | CineFlow" %></title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/reset.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/variables.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/buttons.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/badges.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/forms.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/alerts.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/shell.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/profile.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/dashboard.css">
</head>
<body>
<div class="cf-manager-shell">
    <jsp:include page='<%= isAdmin ? "/admin/sidebar.jsp" : "/manager/sidebar.jsp" %>' />
    <div class="cf-manager-main">
        <jsp:include page="/admin/topbar.jsp" />
        <main class="cf-content">
            <div class="cf-page-header">
                <div>
                    <p class="cf-breadcrumb">Tài khoản / Hồ sơ của tôi</p>
                    <h1><%= isAdmin ? "Hồ sơ Quản trị viên (Admin)" : "Hồ sơ Quản lý (Manager)" %></h1>
                    <p><%= isAdmin ? "Cập nhật thông tin định danh và quản lý tài khoản quản trị hệ thống CineFlow." : "Cập nhật thông tin định danh và tài khoản quản lý cụm rạp CineFlow." %></p>
                </div>
                <a class="cf-btn cf-btn-secondary" href="${pageContext.request.contextPath}/dashboard">← Về Dashboard</a>
            </div>

            <% if ("true".equals(updated)) { %>
                <div class="cf-alert cf-alert-success">✓ Thông tin hồ sơ đã được cập nhật thành công.</div>
            <% } %>
            <% if ("email".equals(error)) { %>
                <div class="cf-alert cf-alert-error">⚠️ Địa chỉ email này đã được sử dụng bởi tài khoản khác trong hệ thống.</div>
            <% } %>
            <% if ("required".equals(error)) { %>
                <div class="cf-alert cf-alert-error">⚠️ Họ tên và Email là thông tin bắt buộc.</div>
            <% } %>

            <div class="manager-profile-grid">
                <!-- THÔNG TIN FORM -->
                <section class="cf-card">
                    <div class="cf-card-header">
                        <div>
                            <h2>Thông tin cá nhân</h2>
                            <p>Thông tin hiển thị khi thao tác trong hệ thống quản lý CineFlow.</p>
                        </div>
                    </div>
                    <div class="cf-card-body">
                        <form method="post" action="${pageContext.request.contextPath}/account" class="cf-form-grid">
                            <div class="cf-field cf-field-full">
                                <label for="fullName">Họ và tên <span style="color: var(--cf-primary);">*</span></label>
                                <input id="fullName" name="fullName" value="<%= esc(fullName) %>" required placeholder="Nhập họ và tên">
                            </div>

                            <div class="cf-field">
                                <label for="account">Tên tài khoản</label>
                                <input id="account" value="<%= user != null ? esc(user.getAccount()) : "" %>" disabled>
                            </div>

                            <div class="cf-field">
                                <label for="role">Vai trò tài khoản</label>
                                <input id="role" value="<%= isAdmin ? "ADMIN (Toàn quyền hệ thống)" : "MANAGER (Quản lý cụm rạp)" %>" disabled style="font-weight: 700; color: var(--cf-primary);">
                            </div>

                            <div class="cf-field">
                                <label for="email">Địa chỉ Email <span style="color: var(--cf-primary);">*</span></label>
                                <input id="email" type="email" name="email" value="<%= user != null ? esc(user.getEmail()) : "" %>" required placeholder="<%= isAdmin ? "admin@cineflow.com" : "manager@cineflow.com" %>">
                            </div>

                            <div class="cf-field">
                                <label for="phone">Số điện thoại</label>
                                <input id="phone" name="phone" value="<%= user != null ? esc(user.getPhone()) : "" %>" placeholder="09xxxxxxxx">
                            </div>

                            <div class="cf-field cf-field-full">
                                <label for="avatar">Đường dẫn ảnh đại diện (Avatar URL)</label>
                                <input id="avatar" name="avatar" value="<%= user != null ? esc(user.getAvatar()) : "" %>" placeholder="https://example.com/avatar.jpg">
                            </div>

                            <div class="cf-form-actions cf-field-full">
                                <button class="cf-btn cf-btn-primary" type="submit">💾 Lưu thay đổi</button>
                            </div>
                        </form>
                    </div>
                </section>

                <!-- TỔNG KẾT BÊN PHẢI -->
                <aside class="cf-card manager-profile-summary">
                    <div class="profile-mark" style="overflow: hidden;">
                        <% if (avatar != null && !avatar.isBlank()) { %>
                            <img src="<%= avatar %>" alt="<%= fullName %>" style="width: 100%; height: 100%; object-fit: cover; border-radius: 50%;" onerror="this.parentElement.innerHTML='<%= initial %>';">
                        <% } else { %>
                            <%= initial %>
                        <% } %>
                    </div>
                    <h2><%= esc(fullName) %></h2>
                    <p class="profile-role" style="letter-spacing: .08em;"><%= isAdmin ? "SUPER ADMINISTRATOR" : "CINEMA MANAGER" %></p>

                    <div class="profile-facts">
                        <div>
                            <span>Tên đăng nhập</span>
                            <strong><%= user != null ? esc(user.getAccount()) : "user" %></strong>
                        </div>
                        <div>
                            <span>Trạng thái</span>
                            <strong style="color: var(--cf-success);"><%= user != null && "ACTIVE".equals(user.getStatus()) ? "Đang hoạt động" : (user != null ? esc(user.getStatus()) : "ACTIVE") %></strong>
                        </div>
                        <div>
                            <span>Xác minh Email</span>
                            <strong><%= user != null && user.isEmailVerified() ? "✓ Đã xác minh" : "Chưa xác minh" %></strong>
                        </div>
                    </div>

                    <a class="cf-btn cf-btn-secondary profile-password" href="${pageContext.request.contextPath}/auth/change-password.jsp">🔑 Đổi mật khẩu</a>
                    <a class="cf-btn cf-btn-secondary profile-password" style="margin-top: 8px; color: var(--cf-danger); border-color: #fca5a5;" href="${pageContext.request.contextPath}/logout">↪ Đăng xuất</a>
                </aside>
            </div>
        </main>
    </div>
</div>
</body>
</html>
