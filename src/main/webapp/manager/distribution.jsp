<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<% if ("ADMIN".equals(session.getAttribute("role"))) { response.sendRedirect(request.getContextPath() + "/admin/distribution"); } else { response.sendError(HttpServletResponse.SC_FORBIDDEN); } %>
