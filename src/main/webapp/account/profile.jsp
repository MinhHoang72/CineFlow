<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.User,model.BookingHistory,java.util.List,java.text.SimpleDateFormat" %>
<%
    if (session.getAttribute("userId") == null) {
        String loginUrl = request.getContextPath() + "/auth/login.jsp?redirect=" + java.net.URLEncoder.encode("/account", "UTF-8");
        response.sendRedirect(loginUrl);
        return;
    }
    if (request.getAttribute("accountUser") == null) {
        response.sendRedirect(request.getContextPath() + "/account");
        return;
    }
%>
<%!
    private String esc(String value) { return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
    private String money(java.math.BigDecimal value) { return value == null ? "0 đ" : String.format("%,.0f đ", value); }
%>
<%
    User user = (User) request.getAttribute("accountUser");
    List<BookingHistory> history = (List<BookingHistory>) request.getAttribute("bookingHistory");
    String error = request.getParameter("error");
    String updated = request.getParameter("updated");
    SimpleDateFormat dateTime = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    String avatarUrl = user == null || user.getAvatar() == null ? "" : user.getAvatar();
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Tài khoản của tôi | CineFlow</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/layout.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account/account.css?v=<%= System.currentTimeMillis() %>">
</head>
<body>
<div class="page-shell">
    <!-- Common Header & Navigation -->
    <jsp:include page="/common/header.jsp"/>

    <main class="account-page">
        <% if ("true".equals(updated)) { %>
            <div class="notice success">✓ Thông tin cá nhân đã được cập nhật thành công.</div>
        <% } %>
        <% if ("email".equals(error)) { %>
            <div class="notice error">✕ Email này đã được sử dụng bởi một tài khoản khác.</div>
        <% } %>
        <% if ("required".equals(error)) { %>
            <div class="notice error">✕ Họ tên và email là thông tin bắt buộc.</div>
        <% } %>
        <% if ("avatarType".equals(error)) { %>
            <div class="notice error">✕ Ảnh đại diện chỉ hỗ trợ định dạng PNG, JPG hoặc WEBP.</div>
        <% } %>

        <!-- ===================================================================
             1. PROFILE OVERVIEW BANNER (HERO CARD)
             =================================================================== -->
        <section class="profile-overview">
            <div class="overview-left">
                <div class="profile-avatar-wrap">
                    <div class="profile-avatar">
                        <% if (!avatarUrl.isBlank()) { %>
                            <img id="avatarPreview" src="<%= esc(avatarUrl) %>" alt="Ảnh đại diện">
                        <% } else { %>
                            <span id="avatarInitial"><%= user.getFullName() == null || user.getFullName().isBlank() ? "C" : esc(user.getFullName().substring(0,1).toUpperCase()) %></span>
                        <% } %>
                    </div>
                    <label class="avatar-camera" for="avatarUpload" aria-label="Đổi ảnh đại diện" title="Đổi ảnh đại diện">
                        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path>
                            <circle cx="12" cy="13" r="4"></circle>
                        </svg>
                    </label>
                </div>

                <div class="overview-info">
                    <div class="overview-name-row">
                        <h2><%= esc(user.getFullName()) %></h2>
                        <span class="role-badge"><%= "CUSTOMER".equalsIgnoreCase(user.getRole()) ? "Thành viên CineFlow" : esc(user.getRole()) %></span>
                        <% if (user.isEmailVerified()) { %>
                            <span class="verified-badge">✓ Đã xác thực email</span>
                        <% } else { %>
                            <span class="unverified-badge">⚠ Chưa xác thực</span>
                        <% } %>
                    </div>

                    <div class="overview-details">
                        <span class="detail-item"><strong>@</strong><%= esc(user.getAccount()) %></span>
                        <span class="detail-dot">•</span>
                        <span class="detail-item">✉ <%= esc(user.getEmail()) %></span>
                        <span class="detail-dot">•</span>
                        <span class="detail-item">📞 <%= user.getPhone() == null || user.getPhone().isBlank() ? "Chưa có SĐT" : esc(user.getPhone()) %></span>
                    </div>

                    <div class="overview-dates">
                        <span>Tham gia: <%= user.getCreatedAt() == null ? "—" : new SimpleDateFormat("dd/MM/yyyy").format(user.getCreatedAt()) %></span>
                        <span class="detail-dot">•</span>
                        <span>Cập nhật: <%= user.getUpdatedAt() == null ? "—" : new SimpleDateFormat("dd/MM/yyyy").format(user.getUpdatedAt()) %></span>
                    </div>
                </div>
            </div>

            <div class="overview-stats">
                <div class="stat-card">
                    <span class="stat-number"><%= history == null ? 0 : history.size() %></span>
                    <span class="stat-label">Vé đã đặt</span>
                </div>
                <div class="stat-card">
                    <span class="stat-badge"><%= (history != null && history.size() >= 5) ? "VIP MEMBER" : "STANDARD" %></span>
                    <span class="stat-label">Hạng thành viên</span>
                </div>
            </div>
        </section>

        <!-- ===================================================================
             2. MAIN ACCOUNT GRID (FORM + SECURITY + SUMMARY)
             =================================================================== -->
        <section class="account-grid">
            <!-- CỘT TRÁI: FORM CẬP NHẬT THÔNG TIN -->
            <article class="panel profile-panel">
                <div class="panel-heading">
                    <div class="heading-title-group">
                        <span class="heading-icon">👤</span>
                        <div>
                            <h2>Thông tin cá nhân</h2>
                            <p class="panel-desc">Cập nhật họ tên và thông tin liên lạc của bạn để sử dụng dịch vụ tốt nhất.</p>
                        </div>
                    </div>
                </div>

                <form method="post" enctype="multipart/form-data" action="${pageContext.request.contextPath}/account" class="profile-form">
                    <div class="form-group full-width">
                        <label for="fullName">Họ và tên <span class="required">*</span></label>
                        <input id="fullName" name="fullName" value="<%= esc(user.getFullName()) %>" placeholder="Nhập họ và tên đầy đủ" required>
                    </div>

                    <div class="form-group">
                        <label for="account">Tên tài khoản</label>
                        <input id="account" value="<%= esc(user.getAccount()) %>" disabled title="Tên tài khoản cố định">
                    </div>

                    <div class="form-group">
                        <label for="email">Địa chỉ Email <span class="required">*</span></label>
                        <input id="email" type="email" name="email" value="<%= esc(user.getEmail()) %>" placeholder="name@example.com" required>
                    </div>

                    <div class="form-group full-width">
                        <label for="phone">Số điện thoại liên hệ</label>
                        <input id="phone" name="phone" value="<%= esc(user.getPhone()) %>" placeholder="Nhập số điện thoại (dùng để nhận vé tại quầy & Kiosk)">
                    </div>

                    <input id="avatarValue" type="hidden" name="avatar" value="<%= esc(avatarUrl) %>">
                    <input id="avatarUpload" type="file" name="avatarFile" accept="image/png,image/jpeg,image/webp" hidden onchange="previewAvatar(this)">

                    <button type="submit" class="save-btn">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
                            <polyline points="17 21 17 13 7 13 7 21"></polyline>
                            <polyline points="7 3 7 8 15 8"></polyline>
                        </svg>
                        Lưu thay đổi
                    </button>
                </form>
            </article>

            <!-- CỘT PHẢI: BẢO MẬT & TỔNG QUAN -->
            <div class="account-side">
                <!-- TỔNG QUAN TÀI KHOẢN -->
                <article class="panel account-summary">
                    <div class="panel-heading">
                        <div class="heading-title-group">
                            <span class="heading-icon">📋</span>
                            <div>
                                <h2>Tổng quan tài khoản</h2>
                            </div>
                        </div>
                    </div>

                    <div class="summary-list">
                        <div class="summary-row">
                            <span class="row-label">Loại tài khoản</span>
                            <strong class="row-value"><%= "CUSTOMER".equalsIgnoreCase(user.getRole()) ? "Khách hàng" : esc(user.getRole()) %></strong>
                        </div>

                        <div class="summary-row">
                            <span class="row-label">Trạng thái</span>
                            <span class="status-pill active">● <%= esc(user.getStatus()) %></span>
                        </div>

                        <div class="summary-row">
                            <span class="row-label">Xác thực Email</span>
                            <% if (user.isEmailVerified()) { %>
                                <span class="status-pill verified">✓ Đã xác thực</span>
                            <% } else { %>
                                <span class="status-pill unverified">
                                    ⚠ Chưa xác thực
                                    <a href="${pageContext.request.contextPath}/verify-otp?action=resend&email=<%= java.net.URLEncoder.encode(user.getEmail(), "UTF-8") %>&type=register" class="verify-action-link">[Xác minh ngay]</a>
                                </span>
                            <% } %>
                        </div>

                        <div class="summary-row">
                            <span class="row-label">Ngày tham gia</span>
                            <strong class="row-value"><%= user.getCreatedAt() == null ? "—" : new SimpleDateFormat("dd/MM/yyyy").format(user.getCreatedAt()) %></strong>
                        </div>

                        <div class="summary-row">
                            <span class="row-label">Tổng vé đã đặt</span>
                            <strong class="row-value highlight"><%= history == null ? 0 : history.size() %> vé</strong>
                        </div>
                    </div>
                </article>

                <!-- BẢO MẬT TÀI KHOẢN -->
                <article class="panel security-panel">
                    <div class="panel-heading">
                        <div class="heading-title-group">
                            <span class="heading-icon">🔒</span>
                            <div>
                                <h2>Bảo mật tài khoản</h2>
                            </div>
                        </div>
                    </div>

                    <a class="security-action-link" href="${pageContext.request.contextPath}/auth/change-password.jsp">
                        <div class="action-icon">🔑</div>
                        <div class="action-text">
                            <strong>Đổi mật khẩu</strong>
                            <small>Cập nhật mật khẩu định kỳ để bảo vệ tài khoản</small>
                        </div>
                        <span class="action-arrow">›</span>
                    </a>

                    <div class="security-tip-box">
                        <span class="tip-icon">💡</span>
                        <p>Không chia sẻ mật khẩu và mã OTP cho bất kỳ ai để bảo vệ quyền lợi thành viên CineFlow của bạn.</p>
                    </div>
                </article>
            </div>
        </section>

        <!-- ===================================================================
             3. BOOKING HISTORY PANEL
             =================================================================== -->
        <section class="panel history-panel">
            <div class="panel-heading history-heading">
                <div class="heading-title-group">
                    <span class="heading-icon">🎟️</span>
                    <div>
                        <h2>Lịch sử đặt vé</h2>
                        <p class="panel-desc">Danh sách các suất chiếu và vé xem phim bạn đã đặt gần đây.</p>
                    </div>
                </div>
                <a href="${pageContext.request.contextPath}/booking/booking.jsp" class="new-booking-btn">
                    + Đặt vé mới
                </a>
            </div>

            <% if (history == null || history.isEmpty()) { %>
                <div class="empty-history">
                    <div class="empty-icon">🎬</div>
                    <p>Bạn chưa có lần đặt vé nào.</p>
                    <a href="${pageContext.request.contextPath}/booking/booking.jsp" class="browse-movies-btn">
                        Khám phá phim đang chiếu
                    </a>
                </div>
            <% } else { %>
                <div class="history-list">
                    <% for (BookingHistory item : history) { %>
                        <article class="history-item">
                            <div class="history-left">
                                <div class="movie-ticket-icon">🎟</div>
                                <div class="history-details">
                                    <strong><%= esc(item.getMovieTitle()) %></strong>
                                    <span class="booking-code">Mã vé: <code><%= esc(item.getBookingCode()) %></code></span>
                                    <span class="booking-meta">
                                        <%= esc(item.getBranchName()) %> · <%= item.getStartTime() == null ? "Chưa có lịch" : dateTime.format(item.getStartTime()) %>
                                    </span>
                                </div>
                            </div>
                            <div class="history-right">
                                <span class="total-price"><%= money(item.getTotalAmount()) %></span>
                                <span class="booking-status-tag">
                                    ● <%= esc(item.getBookingStatus()) %>
                                </span>
                            </div>
                        </article>
                    <% } %>
                </div>
            <% } %>
        </section>
    </main>

    <!-- Common Footer -->
    <jsp:include page="/common/footer.jsp"/>
</div>
</body>

<script>
function previewAvatar(input) {
    if (!input.files || !input.files[0]) return;
    const file = input.files[0];
    if (file.size > 2000000) {
        alert('Ảnh đại diện không được lớn hơn 2MB.');
        input.value = '';
        return;
    }
    const reader = new FileReader();
    reader.onload = function(e) {
        let preview = document.getElementById('avatarPreview');
        if (preview) {
            preview.src = e.target.result;
        } else {
            const image = document.createElement('img');
            image.id = 'avatarPreview';
            image.alt = 'Ảnh đại diện';
            image.src = e.target.result;
            const initial = document.getElementById('avatarInitial');
            if (initial) initial.replaceWith(image);
        }
    };
    reader.readAsDataURL(file);
}
</script>
</html>
