<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.entity.User,model.dto.booking.BookingHistoryDTO,java.util.List,java.text.SimpleDateFormat" %>
<%
    if (session.getAttribute("userId") == null) {
        String loginUrl = request.getContextPath() + "/login?redirect=" + java.net.URLEncoder.encode("/account", "UTF-8");
        response.sendRedirect(loginUrl);
        return;
    }
    if (request.getAttribute("accountUser") == null) {
        response.sendRedirect(request.getContextPath() + "/account");
        return;
    }
%>
<%!
    private String esc(String value) { 
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); 
    }
    private String money(java.math.BigDecimal value) { 
        return value == null ? "0 đ" : String.format("%,.0f đ", value); 
    }
%>
<%
    User user = (User) request.getAttribute("accountUser");
    List<BookingHistoryDTO> history = (List<BookingHistoryDTO>) request.getAttribute("bookingHistory");
    String error = request.getParameter("error");
    String updated = request.getParameter("updated");
    SimpleDateFormat dateTime = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    SimpleDateFormat dateOnly = new SimpleDateFormat("dd/MM/yyyy");
    String avatarUrl = user == null || user.getAvatar() == null ? "" : user.getAvatar();

    boolean isInternal = user != null && user.isInternalRole();
    boolean isAdmin = user != null && user.isAdmin();
    boolean isCustomer = !isInternal;

    dao.CinemaDAO cinemaDAO = new dao.CinemaDAO();
    model.entity.Centre userCentre = null;
    if (user != null && user.getCentreId() != null && user.getCentreId() > 0) {
        userCentre = cinemaDAO.getCentreById(user.getCentreId());
    }
    String centreDisplayName = "Toàn bộ hệ thống CineFlow";
    if (userCentre != null) {
        centreDisplayName = userCentre.getCentreName() + " (" + userCentre.getCity() + ")";
    } else if (isCustomer) {
        centreDisplayName = "Toàn bộ hệ thống CineFlow";
    } else if (!isAdmin) {
        centreDisplayName = "Chưa phân công cụm rạp";
    }

    if (isInternal) {
        request.setAttribute("pageTitle", "Hồ sơ của tôi");
        request.setAttribute("pageBreadcrumb", "Tài khoản");
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Hồ sơ của tôi | CineFlow</title>

    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/reset.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/variables.css">

    <% if (isInternal) { %>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/buttons.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/badges.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/forms.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/alerts.css">
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/manager/shell.css">
    <% } else { %>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/layout.css?v=<%= System.currentTimeMillis() %>">
    <% } %>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account/account.css?v=<%= System.currentTimeMillis() %>">
</head>
<body>
<% if (isInternal) { %>
    <div class="cf-manager-shell">
        <jsp:include page='<%= isAdmin ? "/WEB-INF/fragments/adminSidebar.jspf" : "/WEB-INF/fragments/managerSidebar.jspf" %>' />
        <div class="cf-manager-main">
            <jsp:include page="/WEB-INF/fragments/adminTopbar.jspf" />
            <main class="cf-content">
                <div class="account-page staff-account-container">
<% } else { %>
    <div class="page-shell">
        <jsp:include page="/common/header.jsp"/>
        <main class="account-page">
<% } %>

        <!-- THÔNG BÁO HỆ THỐNG -->
        <% if ("emailVerified".equals(updated)) { %>
            <div class="notice success">✓ Địa chỉ email mới đã được xác thực thành công và cập nhật vào tài khoản của bạn.</div>
        <% } else if ("true".equals(updated)) { %>
            <div class="notice success">✓ Thông tin cá nhân đã được cập nhật thành công.</div>
        <% } %>
        <% if ("email".equals(error)) { %>
            <div class="notice error">✕ Địa chỉ email này đã được sử dụng bởi một tài khoản khác trong hệ thống.</div>
        <% } %>
        <% if ("sessionExpired".equals(error)) { %>
            <div class="notice error">✕ Phiên làm việc xác thực email đã hết hạn. Vui lòng thực hiện lại.</div>
        <% } %>
        <% if ("updateFailed".equals(error)) { %>
            <div class="notice error">✕ Không thể cập nhật địa chỉ email. Vui lòng thử lại.</div>
        <% } %>
        <% if ("required".equals(error)) { %>
            <div class="notice error">✕ Họ tên và email là thông tin bắt buộc.</div>
        <% } %>
        <% if ("invalidName".equals(error)) { %>
            <div class="notice error">✕ Họ và tên chỉ chứa chữ cái và không được quá ngắn.</div>
        <% } %>
        <% if ("invalidEmail".equals(error)) { %>
            <div class="notice error">✕ Định dạng email không hợp lệ.</div>
        <% } %>
        <% if ("invalidPhone".equals(error)) { %>
            <div class="notice error">✕ Số điện thoại không hợp lệ (cần 10 chữ số).</div>
        <% } %>
        <% if ("avatarType".equals(error)) { %>
            <div class="notice error">✕ Ảnh đại diện chỉ hỗ trợ định dạng PNG, JPG hoặc WEBP.</div>
        <% } %>

        <!-- ===================================================================
             1. KHÁI QUÁT THÔNG TIN CÁ NHÂN (HERO CARD)
             =================================================================== -->
        <section class="profile-overview">
            <div class="overview-left">
                <div class="profile-avatar-wrap">
                    <div class="profile-avatar">
                        <% if (!avatarUrl.isBlank()) { %>
                            <img id="avatarPreview" src="<%= esc(avatarUrl) %>" alt="Ảnh đại diện">
                        <% } else { %>
                            <span id="avatarInitial"><%= user != null && user.getFullName() != null && !user.getFullName().isBlank() ? esc(user.getFullName().substring(0,1).toUpperCase()) : "C" %></span>
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
                        <h2><%= user != null ? esc(user.getFullName()) : "" %></h2>
                        <span class="role-badge"><%= user != null ? (isCustomer ? "Khách hàng" : esc(user.getRoleDisplayName())) : "Thành viên" %></span>
                        <% if (user != null && user.isEmailVerified()) { %>
                            <span class="verified-badge">✓ Đã xác thực email</span>
                        <% } else { %>
                            <span class="unverified-badge">⚠ Chưa xác thực</span>
                        <% } %>
                    </div>

                    <div class="overview-details">
                        <span class="detail-item">✉ <%= user != null ? esc(user.getEmail()) : "" %></span>
                        <span class="detail-dot">•</span>
                        <span class="detail-item">📞 <%= user != null && user.getPhone() != null && !user.getPhone().isBlank() ? esc(user.getPhone()) : "Chưa có SĐT" %></span>
                        <% if (!isCustomer) { %>
                            <span class="detail-dot">•</span>
                            <span class="detail-item">🏢 <%= esc(centreDisplayName) %></span>
                        <% } %>
                    </div>

                    <div class="overview-dates">
                        <span>Tham gia: <%= user != null && user.getCreatedAt() != null ? dateOnly.format(user.getCreatedAt()) : "—" %></span>
                        <span class="detail-dot">•</span>
                        <span>Cập nhật: <%= user != null && user.getUpdatedAt() != null ? dateOnly.format(user.getUpdatedAt()) : "—" %></span>
                    </div>
                </div>
            </div>

            <div class="overview-stats">
                <% if (isCustomer) { %>
                    <div class="stat-card">
                        <span class="stat-number"><%= history == null ? 0 : history.size() %></span>
                        <span class="stat-label">Vé đã đặt</span>
                    </div>
                <% } else { %>
                    <div class="stat-card">
                        <span class="stat-number" style="font-size: 19px; letter-spacing: 0.5px;"><%= user != null ? user.getEmployeeCode() : "NV00000" %></span>
                        <span class="stat-label">Mã nhân sự</span>
                    </div>
                <% } %>
                <div class="stat-card">
                    <span class="stat-badge" style="background: rgba(16, 185, 129, 0.15); color: #10b981; border: 1px solid rgba(16, 185, 129, 0.3);">● <%= user != null ? esc(user.getStatus()) : "ACTIVE" %></span>
                    <span class="stat-label">Trạng thái</span>
                </div>
            </div>
        </section>

        <!-- ===================================================================
             2. LƯỚI THÔNG TIN TÀI KHOẢN (BIỂU MẪU + BẢO MẬT + TỔNG KẾT)
             =================================================================== -->
        <section class="account-grid">
            <!-- CỘT TRÁI: FORM CẬP NHẬT THÔNG TIN -->
            <article class="panel profile-panel">
                <div class="panel-heading">
                    <div class="heading-title-group">
                        <span class="heading-icon">👤</span>
                        <div>
                            <h2>Thông tin cá nhân</h2>
                            <p class="panel-desc"><%= isCustomer ? "Cập nhật họ tên và thông tin liên lạc của bạn để sử dụng dịch vụ tốt nhất." : "Cập nhật họ tên và thông tin liên lạc phục vụ công tác điều hành và phối hợp tại CineFlow." %></p>
                        </div>
                    </div>
                </div>

                <form method="post" enctype="multipart/form-data" action="${pageContext.request.contextPath}/account" class="profile-form">
                    <div class="form-group full-width">
                        <label for="fullName">Họ và tên <span class="required">*</span></label>
                        <input id="fullName" name="fullName" value="<%= user != null ? esc(user.getFullName()) : "" %>" placeholder="Nhập họ và tên đầy đủ" required>
                    </div>

                    <div class="form-group">
                        <label for="account">Tên tài khoản</label>
                        <input id="account" value="<%= user != null ? esc(user.getAccount()) : "" %>" disabled title="Tên tài khoản cố định">
                    </div>

                    <% if (!isCustomer) { %>
                    <div class="form-group">
                        <label for="roleDisplay">Vai trò hệ thống</label>
                        <input id="roleDisplay" value="<%= user != null ? esc(user.getRoleDisplayName()) : "" %>" disabled style="font-weight: 700; color: #38bdf8;">
                    </div>

                    <div class="form-group full-width">
                        <label for="centreDisplay">Cụm rạp trực thuộc</label>
                        <input id="centreDisplay" value="<%= esc(centreDisplayName) %>" disabled style="color: #cbd5e1;">
                    </div>
                    <% } %>

                    <div class="form-group">
                        <label for="email">Địa chỉ Email <span class="required">*</span></label>
                        <input id="email" type="email" name="email" value="<%= user != null ? esc(user.getEmail()) : "" %>" placeholder="name@example.com" required>
                    </div>

                    <div class="form-group">
                        <label for="phone">Số điện thoại liên hệ</label>
                        <input id="phone" name="phone" value="<%= user != null && user.getPhone() != null ? esc(user.getPhone()) : "" %>" placeholder="<%= isCustomer ? "Nhập số điện thoại (dùng để nhận vé tại quầy & Kiosk)" : "09xxxxxxxx" %>">
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
                        <% if (isCustomer) { %>
                            <div class="summary-row">
                                <span class="row-label">Loại tài khoản</span>
                                <strong class="row-value">Khách hàng</strong>
                            </div>
                        <% } else { %>
                            <div class="summary-row">
                                <span class="row-label">Mã nhân sự</span>
                                <strong class="row-value highlight"><%= user != null ? user.getEmployeeCode() : "NV00000" %></strong>
                            </div>
                            <div class="summary-row">
                                <span class="row-label">Vai trò chức vụ</span>
                                <strong class="row-value"><%= user != null ? esc(user.getRoleDisplayName()) : "Nhân viên" %></strong>
                            </div>
                            <div class="summary-row">
                                <span class="row-label">Cụm rạp làm việc</span>
                                <strong class="row-value"><%= esc(centreDisplayName) %></strong>
                            </div>
                        <% } %>

                        <div class="summary-row">
                            <span class="row-label">Trạng thái</span>
                            <span class="status-pill active">● <%= user != null ? esc(user.getStatus()) : "ACTIVE" %></span>
                        </div>

                        <div class="summary-row">
                            <span class="row-label">Xác thực Email</span>
                            <% if (user != null && user.isEmailVerified()) { %>
                                <span class="status-pill verified">✓ Đã xác thực</span>
                            <% } else { %>
                                <span class="status-pill unverified">
                                    ⚠ Chưa xác thực
                                    <% if (isCustomer && user != null && user.getEmail() != null) { %>
                                        <a href="${pageContext.request.contextPath}/verify-otp?action=resend&email=<%= java.net.URLEncoder.encode(user.getEmail(), "UTF-8") %>&type=register" class="verify-action-link">[Xác minh ngay]</a>
                                    <% } %>
                                </span>
                            <% } %>
                        </div>

                        <div class="summary-row">
                            <span class="row-label">Ngày tham gia</span>
                            <strong class="row-value"><%= user != null && user.getCreatedAt() != null ? dateOnly.format(user.getCreatedAt()) : "—" %></strong>
                        </div>

                        <% if (isCustomer) { %>
                            <div class="summary-row">
                                <span class="row-label">Tổng vé đã đặt</span>
                                <strong class="row-value highlight"><%= history == null ? 0 : history.size() %> vé</strong>
                            </div>
                        <% } %>
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

                    <a class="security-action-link" href="${pageContext.request.contextPath}/change-password">
                        <div class="action-icon">🔑</div>
                        <div class="action-text">
                            <strong>Đổi mật khẩu</strong>
                            <small>Cập nhật mật khẩu định kỳ để bảo vệ tài khoản</small>
                        </div>
                        <span class="action-arrow">›</span>
                    </a>

                    <div class="security-tip-box">
                        <span class="tip-icon"><%= isCustomer ? "💡" : "🛡️" %></span>
                        <p><%= isCustomer ? "Không chia sẻ mật khẩu và mã OTP cho bất kỳ ai để bảo vệ tài khoản CineFlow của bạn." : "Tài khoản nhân sự và quản trị có quyền truy cập dữ liệu và vận hành hệ thống CineFlow. Vui lòng định kỳ cập nhật mật khẩu và không chia sẻ cho người khác." %></p>
                    </div>
                </article>
            </div>
        </section>

        <% if (isCustomer) { %>
        <!-- ===================================================================
             3. BẢNG LỊCH SỬ ĐẶT VÉ (CHỈ DÀNH CHO KHÁCH HÀNG)
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
                    Đặt vé mới
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
                    <% for (BookingHistoryDTO item : history) { %>
                        <article class="history-item">
                            <div class="history-left">
                                <div class="movie-ticket-icon">🎟</div>
                                <div class="history-details">
                                    <strong><%= esc(item.getMovieTitle()) %></strong>
                                    <span class="booking-code">Mã vé: <code><%= esc(item.getBookingCode()) %></code></span>
                                    <span class="booking-meta">
                                        <%= esc(item.getCentreName()) %> · <%= item.getStartTime() == null ? "Chưa có lịch" : dateTime.format(item.getStartTime()) %>
                                    </span>
                                </div>
                            </div>
                            <div class="history-right">
                                <span class="total-price"><%= money(item.getTotalAmount()) %></span>
                                <%
                                    String st = item.getBookingStatus();
                                    String tagClass = item.isPayable() ? " is-pending"
                                            : ("CONFIRMED".equals(st) || "COMPLETED".equals(st)) ? "" : " is-muted";
                                %>
                                <span class="booking-status-tag<%= tagClass %>">
                                    ● <%= esc(item.getStatusLabel()) %>
                                </span>
                                <div class="history-actions">
                                    <% if (item.isPayable()) { %>
                                        <a href="${pageContext.request.contextPath}/booking?action=checkout&bookingId=<%= item.getBookingId() %>"
                                           class="new-booking-btn">Thanh toán</a>
                                    <% } %>
                                    <a href="${pageContext.request.contextPath}/booking?action=detail&bookingId=<%= item.getBookingId() %>"
                                       class="new-booking-btn">Xem vé</a>
                                </div>
                            </div>
                        </article>
                    <% } %>
                </div>
            <% } %>
        </section>
        <% } %>

<% if (isInternal) { %>
                </div>
            </main>
        </div>
    </div>
<% } else { %>
        </main>
        <!-- Footer chung -->
        <jsp:include page="/common/footer.jsp"/>
    </div>
<% } %>
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
