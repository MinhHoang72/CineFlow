<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%--
    Trang mua ve cu da duoc thay bang chuc nang dat ve FE-05 tai /booking.
    Giu lai duong dan nay de cac link cu (home, header, footer, chi tiet phim, trang ca nhan) van dung.
--%>
<%
    StringBuilder target = new StringBuilder(request.getContextPath()).append("/booking");
    String movieId = request.getParameter("movieId");
    String centreId = request.getParameter("centreId");
    String separator = "?";
    if (movieId != null && movieId.matches("\\d{1,9}")) {
        target.append(separator).append("movieId=").append(movieId);
        separator = "&";
    }
    if (centreId != null && centreId.matches("\\d{1,9}")) {
        target.append(separator).append("branchId=").append(centreId);
    }
    response.sendRedirect(target.toString());
%>
