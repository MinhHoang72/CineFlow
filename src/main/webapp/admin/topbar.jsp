<%@ page pageEncoding="UTF-8" %>
<%@ page import="model.User" %>
<%
    String tbTitle = (String) request.getAttribute("pageTitle");
    if (tbTitle == null) tbTitle = "Hệ thống quản lý CineFlow";
    String tbCrumb = (String) request.getAttribute("pageBreadcrumb");
    User tbUser = (User) session.getAttribute("user");
    String tbName = tbUser != null && tbUser.getFullName() != null && !tbUser.getFullName().isBlank() ? tbUser.getFullName() : "Admin";
    String tbInitial = tbName.substring(0, 1).toUpperCase();
    String tbAvatar = tbUser != null ? tbUser.getAvatar() : null;
    String tbCtx = request.getContextPath();
%>
<header class="cf-topbar">
    <div style="display: flex; align-items: center; gap: 8px;">
        <span class="cf-topbar-title">
            <a href="<%= tbCtx %>/dashboard" style="color: inherit; text-decoration: none;">CineFlow</a>
            <% if (tbCrumb != null && !tbCrumb.isBlank()) { %>
                / <span><%= tbCrumb %></span>
            <% } %>
            / <strong style="color: var(--cf-text);"><%= tbTitle %></strong>
        </span>
    </div>
    <div class="cf-topbar-user">
        <a href="<%= tbCtx %>/account" style="display: flex; align-items: center; gap: 9px; text-decoration: none; color: inherit;" title="Hồ sơ quản trị viên">
            <span class="avatar" style="overflow: hidden;">
                <% if (tbAvatar != null && !tbAvatar.isBlank()) { %>
                    <img src="<%= tbAvatar %>" alt="<%= tbName %>" style="width: 100%; height: 100%; object-fit: cover; border-radius: 50%;">
                <% } else { %>
                    <%= tbInitial %>
                <% } %>
            </span>
            <span><%= tbName %></span>
            <span class="cf-badge cf-badge-danger" style="font-size: 10px; margin-left: 2px; padding: 2px 7px;">ADMIN</span>
        </a>
    </div>
</header>
