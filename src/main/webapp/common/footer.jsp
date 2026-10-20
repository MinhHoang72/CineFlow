<%@ page pageEncoding="UTF-8" %>
<%@ page import="model.entity.User" %>
<%
    User footerUser = (User) session.getAttribute("user");
    String fCtx = request.getContextPath();
%>
<footer class="footer">
    <div class="footer-inner">
        <div class="footer-brand">
            <a class="logo" href="<%= fCtx %>/home/index.jsp" aria-label="CineFlow Cinema">
                <span class="logo-mark">C</span>
                <span class="logo-word">Cine<span>Flow</span></span>
            </a>
            <p>Hệ thống rạp chiếu phim CineFlow.</p>
        </div>
        <div>
            <h4>CINEFLOW</h4>
            <a href="<%= fCtx %>/home/index.jsp#movies">Phim đang chiếu</a>
            <a href="<%= fCtx %>/booking/booking.jsp?view=schedule">Lịch chiếu</a>
            <a href="<%= fCtx %>/cinema/index.jsp">Rạp chiếu phim</a>
            <a href="<%= fCtx %>/home/index.jsp#promotions">Tin mới &amp; ưu đãi</a>
        </div>
        <div id="support">
            <h4>HỖ TRỢ</h4>
            <a href="<%= fCtx %>/home/index.jsp#support">Hỗ trợ khách hàng</a>
            <a href="<%= fCtx %>/home/index.jsp#support">Câu hỏi thường gặp</a>
            <a href="<%= fCtx %>/home/index.jsp#support">Liên hệ</a>
        </div>
        <div>
            <h4>TÀI KHOẢN</h4>
            <% if (footerUser == null) { %>
                <a href="<%= fCtx %>/login">Đăng nhập</a>
                <a href="<%= fCtx %>/register">Tạo tài khoản</a>
            <% } else { %>
                <a href="<%= fCtx %>/account">Tài khoản của tôi</a>
                <a href="<%= fCtx %>/change-password">Đổi mật khẩu</a>
            <% } %>
        </div>
    </div>
    <div class="copyright">© 2026 CineFlow Cinema. All rights reserved.</div>
</footer>
