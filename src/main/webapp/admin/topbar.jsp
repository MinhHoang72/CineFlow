<%@ page pageEncoding="UTF-8" %>
<%@ page import="model.User" %>
<%
    String tbTitle = (String) request.getAttribute("pageTitle");
    if (tbTitle == null) tbTitle = "Hệ thống quản lý CineFlow";
    String tbCrumb = (String) request.getAttribute("pageBreadcrumb");
    User tbUser = (User) session.getAttribute("user");
    String tbRole = (String) session.getAttribute("role");
    if (tbRole == null && tbUser != null) tbRole = tbUser.getRole();
    if (tbRole == null) tbRole = "ADMIN";
    String defaultName = "ADMIN".equalsIgnoreCase(tbRole) ? "Admin" : "Manager";
    String tbName = tbUser != null && tbUser.getFullName() != null && !tbUser.getFullName().isBlank() ? tbUser.getFullName() : defaultName;
    String tbInitial = tbName.substring(0, 1).toUpperCase();
    String tbAvatar = tbUser != null ? tbUser.getAvatar() : null;
    String tbCtx = request.getContextPath();
    String badgeClass = "ADMIN".equalsIgnoreCase(tbRole) ? "cf-badge-danger" : "cf-badge-info";
%>
<header class="cf-topbar">
    <div style="display: flex; align-items: center; gap: 8px;">
        <span class="cf-topbar-title">
            <a href="<%= tbCtx %>/dashboard" style="color: inherit; text-decoration: none;">CineFlow</a>
            <% if (tbCrumb != null && !tbCrumb.isBlank()) { %>
                / <span><%= tbCrumb %></span>
            <% } %>
            / <strong style="color: #111827;"><%= tbTitle %></strong>
        </span>
    </div>
    <div class="cf-topbar-user">
        <a href="<%= tbCtx %>/account" style="display: flex; align-items: center; gap: 9px; text-decoration: none; color: #111827;" title="Hồ sơ tài khoản">
            <span class="avatar" style="overflow: hidden;">
                <% if (tbAvatar != null && !tbAvatar.isBlank()) { %>
                    <img src="<%= tbAvatar %>" alt="<%= tbName %>" style="width: 100%; height: 100%; object-fit: cover; border-radius: 50%;">
                <% } else { %>
                    <%= tbInitial %>
                <% } %>
            </span>
            <span style="color: #111827; font-weight: 600;"><%= tbName %></span>
            <span class="cf-badge <%= badgeClass %>" style="font-size: 10px; margin-left: 2px; padding: 2px 7px;"><%= tbRole.toUpperCase() %></span>
        </a>
    </div>
</header>
