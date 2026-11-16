<%@ page pageEncoding="UTF-8" %>
<%@ page import="model.entity.User" %>
<%@ page import="model.dto.booking.BookingHistoryDTO,java.util.List" %>
<%!
    private User safeSessionUser(Object value) {
        if (value instanceof User) {
            return (User) value;
        }
        if (value == null) {
            return null;
        }
        try {
            User compatible = new User();
            Class<?> type = value.getClass();
            try {
                compatible.setUserId((Integer) type.getMethod("getUserId").invoke(value));
            } catch (Exception ignored) {}
            compatible.setFullName((String) type.getMethod("getFullName").invoke(value));
            compatible.setAccount((String) type.getMethod("getAccount").invoke(value));
            compatible.setRole((String) type.getMethod("getRole").invoke(value));
            try {
                compatible.setAvatar((String) type.getMethod("getAvatar").invoke(value));
            } catch (Exception ignored) {}
            return compatible;
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            return null;
        }
    }

    private String safeEscape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }
%>
<%
    String activeNav = request.getParameter("activeNav");
    if (activeNav == null || activeNav.isBlank()) {
        activeNav = (String) request.getAttribute("activeNav");
    }
    if (activeNav == null) {
        activeNav = "";
    }

    User headerUser = safeSessionUser(session.getAttribute("user"));
    String headerMemberName = headerUser == null ? "" : headerUser.getFullName();
    if (headerMemberName == null || headerMemberName.isBlank()) {
        headerMemberName = headerUser == null ? "" : headerUser.getAccount();
    }
    String headerAccount = headerUser == null ? "" : headerUser.getAccount();
    if (headerAccount == null) {
        headerAccount = "";
    }
    String headerRole = headerUser == null ? "" : (headerUser.getRole() == null ? "" : headerUser.getRole().toUpperCase());
    String headerAvatar = headerUser == null ? null : headerUser.getAvatar();
    String headerInitial = "U";
    if (!headerMemberName.isEmpty()) {
        headerInitial = headerMemberName.substring(0, 1).toUpperCase();
    } else if (!headerAccount.isEmpty()) {
        headerInitial = headerAccount.substring(0, 1).toUpperCase();
    }
    String ctx = request.getContextPath();
    List<BookingHistoryDTO> loginCancellationNotices = null;
    if ("CUSTOMER".equalsIgnoreCase(headerRole)) {
        Object noticeValue = session.getAttribute("loginShowtimeCancellationNotices");
        Object cycleValue = session.getAttribute("loginCancellationNoticeCycle");
        Object shownCycleValue = session.getAttribute("loginCancellationNoticeShownCycle");
        long cycle = cycleValue instanceof Number ? ((Number) cycleValue).longValue() : 0;
        long shownCycle = shownCycleValue instanceof Number ? ((Number) shownCycleValue).longValue() : -1;
        if (noticeValue instanceof List<?> && cycle != shownCycle) {
            List<?> notices = (List<?>) noticeValue;
            if (!notices.isEmpty() && notices.get(0) instanceof BookingHistoryDTO) {
                loginCancellationNotices = (List<BookingHistoryDTO>) notices;
            }
            session.setAttribute("loginCancellationNoticeShownCycle", cycle);
        }
    }
%>
<%@ include file="/WEB-INF/fragments/theme.jspf" %>
<header class="cf-main-header" id="cfMainHeader">
    <div class="cf-header-container">
        <!-- Logo thương hiệu -->
        <% String homeBrandLink = "ADMIN".equalsIgnoreCase(headerRole) ? (ctx + "/dashboard") : (ctx + "/home"); %><a class="cf-brand" href="<%= homeBrandLink %>" aria-label="CineFlow Cinema Home">
            <span class="cf-brand-icon">C</span>
            <span class="cf-brand-name">Cine<span class="cf-brand-glow">Flow</span></span>
        </a>

        <!-- Menu điều hướng chính -->
        <nav class="cf-navbar" aria-label="Điều hướng chính">
            <a class="cf-nav-item <%= "home".equalsIgnoreCase(activeNav) ? "active" : "" %>" href="<%= ctx %>/home">TRANG CHỦ</a>
            <a class="cf-nav-item <%= "booking".equalsIgnoreCase(activeNav) ? "active" : "" %>" href="<%= ctx %>/booking">MUA VÉ</a>
            <a class="cf-nav-item <%= "promotions".equalsIgnoreCase(activeNav) ? "active" : "" %>" href="<%= ctx %>/home#promotions">TIN MỚI &amp; ƯU ĐÃI</a>
            <a class="cf-nav-item <%= "cinema".equalsIgnoreCase(activeNav) ? "active" : "" %>" href="<%= ctx %>/cinema">RẠP CHIẾU PHIM</a>
            <a class="cf-nav-item <%= "support".equalsIgnoreCase(activeNav) ? "active" : "" %>" href="<%= ctx %>/support">HỖ TRỢ</a>
        </nav>

        <!-- Khu vực tài khoản và xác thực bên phải -->
        <div class="cf-header-right">
            <% if (headerUser == null) { %>
                <a class="cf-auth-link" href="<%= ctx %>/login">Đăng nhập</a>
                <a class="cf-auth-btn" href="<%= ctx %>/register">Đăng ký</a>
            <% } else { %>
                <% if ("ADMIN".equals(headerRole)) { %>
                    <a class="cf-badge-role admin" href="<%= ctx %>/dashboard" title="Bảng quản trị hệ thống">Admin</a>
                <% } else if ("MANAGER".equals(headerRole)) { %>
                    <a class="cf-badge-role manager" href="<%= ctx %>/staff" title="Quản lý cụm rạp">Quản lý</a>
                <% } %>
                <a class="cf-account-btn" href="<%= ctx %>/account" title="Trang cá nhân của <%= safeEscape(headerMemberName) %>">Tài khoản</a>
                <form class="cf-logout-inline" action="<%= ctx %>/logout" method="post">
                    <button type="submit" class="cf-logout-btn" title="Đăng xuất khỏi hệ thống">
                        Đăng xuất
                    </button>
                </form>
            <% } %>
        </div>
    </div>
</header>
<% if (loginCancellationNotices != null && !loginCancellationNotices.isEmpty()) { %>
    <div id="cfCancellationNotice" role="dialog" aria-modal="true" aria-labelledby="cfCancellationTitle"
         style="position:fixed;inset:0;z-index:12000;background:rgba(8,12,24,.72);display:flex;align-items:center;justify-content:center;padding:20px;">
        <section style="width:min(560px,100%);max-height:85vh;overflow:auto;background:#fff;color:#202534;border-radius:16px;padding:24px;box-shadow:0 24px 80px rgba(0,0,0,.35);">
            <div style="display:flex;align-items:flex-start;justify-content:space-between;gap:16px;">
                <div>
                    <div style="font-size:13px;font-weight:700;color:#c62828;letter-spacing:.04em;">THÔNG BÁO QUAN TRỌNG</div>
                    <h2 id="cfCancellationTitle" style="font-size:22px;margin:6px 0 8px;">Suất chiếu của bạn đã bị hủy</h2>
                    <p style="margin:0 0 16px;line-height:1.5;">Vé không còn hiệu lực. Vui lòng liên hệ CSKH để được hỗ trợ và đối soát hoàn tiền.</p>
                </div>
                <button type="button" data-cf-dismiss aria-label="Đóng thông báo" style="border:0;background:transparent;font-size:26px;cursor:pointer;line-height:1;">×</button>
            </div>
            <ul style="padding-left:20px;margin:0 0 16px;">
                <% for (BookingHistoryDTO notice : loginCancellationNotices) { %>
                    <li style="margin:8px 0;line-height:1.5;">
                        <strong><%= safeEscape(notice.getMovieTitle()) %></strong> · mã đơn
                        <code><%= safeEscape(notice.getBookingCode()) %></code>
                        <a href="<%= ctx %>/booking?action=detail&amp;bookingId=<%= notice.getBookingId() %>" style="margin-left:6px;">Xem đơn</a>
                    </li>
                <% } %>
            </ul>
            <p style="margin:0 0 20px;line-height:1.5;">Hotline <strong>1900 6868</strong> · Email <strong>cskh@cineflow.vn</strong></p>
            <div style="display:flex;justify-content:flex-end;gap:10px;flex-wrap:wrap;">
                <a href="<%= ctx %>/support" style="display:inline-block;padding:10px 16px;border:1px solid #c62828;border-radius:8px;color:#b71c1c;text-decoration:none;font-weight:600;">Liên hệ hỗ trợ</a>
                <button type="button" data-cf-dismiss style="padding:10px 16px;border:0;border-radius:8px;background:#c62828;color:#fff;font-weight:600;cursor:pointer;">Đã hiểu</button>
            </div>
        </section>
    </div>
    <script>
        (function () {
            var notice = document.getElementById('cfCancellationNotice');
            if (!notice) return;
            notice.querySelectorAll('[data-cf-dismiss]').forEach(function (button) {
                button.addEventListener('click', function () { notice.remove(); });
            });
        })();
    </script>
<% } %>
