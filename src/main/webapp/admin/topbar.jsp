
<%@ page pageEncoding="UTF-8" %>
<%@ page import="model.User" %>

<%
    String tbTitle = (String) request.getAttribute("pageTitle");
    if (tbTitle == null || tbTitle.isBlank()) {
        tbTitle = "Tổng quan điều hành";
    }

    String tbCrumb = (String) request.getAttribute("pageBreadcrumb");

    User tbUser = (User) session.getAttribute("user");

    String tbRole = (String) session.getAttribute("role");
    if (tbRole == null && tbUser != null) {
        tbRole = tbUser.getRole();
    }
    if (tbRole == null || tbRole.isBlank()) {
        tbRole = "ADMIN";
    }

    String defaultName = "ADMIN".equalsIgnoreCase(tbRole)
            ? "Administrator"
            : "Manager";

    String tbName = defaultName;
    if (tbUser != null
            && tbUser.getFullName() != null
            && !tbUser.getFullName().isBlank()) {
        tbName = tbUser.getFullName().trim();
    }

    String tbInitial = tbName.substring(0, 1).toUpperCase();
    String tbAvatar = tbUser != null ? tbUser.getAvatar() : null;
    String tbCtx = request.getContextPath();

    String badgeClass = "ADMIN".equalsIgnoreCase(tbRole)
            ? "cf-badge-danger"
            : "cf-badge-info";
%>

<header class="cf-topbar">
    <div class="cf-topbar-breadcrumb">
        <div class="cf-topbar-title">
            <a href="<%= tbCtx %>/dashboard">CineFlow</a>

            <% if (tbCrumb != null && !tbCrumb.isBlank()) { %>
                <span class="cf-breadcrumb-separator">/</span>
                <span class="cf-breadcrumb-parent"><%= tbCrumb %></span>
            <% } %>

            <span class="cf-breadcrumb-separator">/</span>
            <strong><%= tbTitle %></strong>
        </div>
    </div>

    <div class="cf-topbar-user">
        <a href="<%= tbCtx %>/account" title="Xem hồ sơ tài khoản">
            <span class="avatar">
                <% if (tbAvatar != null && !tbAvatar.isBlank()) { %>
                    <img
                        src="<%= tbAvatar %>"
                        alt="Ảnh đại diện"
                    >
                <% } else { %>
                    <%= tbInitial %>
                <% } %>
            </span>

            <span class="cf-topbar-name"><%= tbName %></span>

            <span class="cf-topbar-role <%= badgeClass %>">
                <%= tbRole.toUpperCase() %>
            </span>
        </a>
    </div>
</header>
