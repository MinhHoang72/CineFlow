<%@ page pageEncoding="UTF-8" %>
<% model.User cfUser = (model.User) session.getAttribute("user"); String cfName = cfUser == null ? "Người dùng" : cfUser.getFullName(); String cfInitial = cfName == null || cfName.isBlank() ? "C" : cfName.substring(0, 1).toUpperCase(); %>
<header class="cf-topbar"><span class="cf-topbar-title">Hệ thống quản lý CineFlow</span><div class="cf-topbar-user"><span class="avatar"><%= cfInitial %></span><span><%= cfName %></span></div></header>

