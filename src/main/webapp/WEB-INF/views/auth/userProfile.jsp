<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.entity.User,model.dto.BookingHistory,java.util.List,java.text.SimpleDateFormat" %>
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
        return value == null ? "0 d" : String.format("%,.0f d", value); 
    }
%>
<%
    User user = (User) request.getAttribute("accountUser");
    List<BookingHistory> history = (List<BookingHistory>) request.getAttribute("bookingHistory");
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
    String centreDisplayName = "Toàn b? h? th?ng CineFlow";
    if (userCentre != null) {
        centreDisplayName = userCentre.getCentreName() + " (" + userCentre.getCity() + ")";
    } else if (isCustomer) {
        centreDisplayName = "Toàn b? h? th?ng CineFlow";
    } else if (!isAdmin) {
        centreDisplayName = "Chua phân công c?m r?p";
    }

    if (isInternal) {
        request.setAttribute("pageTitle", "H? so c?a tôi");
        request.setAttribute("pageBreadcrumb", "Tài kho?n");
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>H? so c?a tôi | CineFlow</title>

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

        <!-- THÔNG BÁO H? TH?NG -->
        <% if ("emailVerified".equals(updated)) { %>
            <div class="notice success">? Ð?a ch? email m?i dã du?c xác th?c thành công và c?p nh?t vào tài kho?n c?a b?n.</div>
        <% } else if ("true".equals(updated)) { %>
            <div class="notice success">? Thông tin cá nhân dã du?c c?p nh?t thành công.</div>
        <% } %>
        <% if ("email".equals(error)) { %>
            <div class="notice error">? Ð?a ch? email này dã du?c s? d?ng b?i m?t tài kho?n khác trong h? th?ng.</div>
        <% } %>
        <% if ("sessionExpired".equals(error)) { %>
            <div class="notice error">? Phiên làm vi?c xác th?c email dã h?t h?n. Vui lòng th?c hi?n l?i.</div>
        <% } %>
        <% if ("updateFailed".equals(error)) { %>
            <div class="notice error">? Không th? c?p nh?t d?a ch? email. Vui lòng th? l?i.</div>
        <% } %>
        <% if ("required".equals(error)) { %>
            <div class="notice error">? H? tên và email là thông tin b?t bu?c.</div>
        <% } %>
        <% if ("invalidName".equals(error)) { %>
            <div class="notice error">? H? và tên ch? ch?a ch? cái và không du?c quá ng?n.</div>
        <% } %>
        <% if ("invalidEmail".equals(error)) { %>
            <div class="notice error">? Ð?nh d?ng email không h?p l?.</div>
        <% } %>
        <% if ("invalidPhone".equals(error)) { %>
            <div class="notice error">? S? di?n tho?i không h?p l? (c?n 10 ch? s?).</div>
        <% } %>
        <% if ("avatarType".equals(error)) { %>
            <div class="notice error">? ?nh d?i di?n ch? h? tr? d?nh d?ng PNG, JPG ho?c WEBP.</div>
        <% } %>

        <!-- ===================================================================
             1. KHÁI QUÁT THÔNG TIN CÁ NHÂN (HERO CARD)
             =================================================================== -->
        <section class="profile-overview">
            <div class="overview-left">
                <div class="profile-avatar-wrap">
                    <div class="profile-avatar">
                        <% if (!avatarUrl.isBlank()) { %>
                            <img id="avatarPreview" src="<%= esc(avatarUrl) %>" alt="?nh d?i di?n">
                        <% } else { %>
                            <span id="avatarInitial"><%= user != null && user.getFullName() != null && !user.getFullName().isBlank() ? esc(user.getFullName().substring(0,1).toUpperCase()) : "C" %></span>
                        <% } %>
                    </div>
                    <label class="avatar-camera" for="avatarUpload" aria-label="Ð?i ?nh d?i di?n" title="Ð?i ?nh d?i di?n">
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
                            <span class="verified-badge">? Ðã xác th?c email</span>
                        <% } else { %>
                            <span class="unverified-badge">? Chua xác th?c</span>
                        <% } %>
                    </div>

                    <div class="overview-details">
                        <span class="detail-item">? <%= user != null ? esc(user.getEmail()) : "" %></span>
                        <span class="detail-dot">•</span>
                        <span class="detail-item">?? <%= user != null && user.getPhone() != null && !user.getPhone().isBlank() ? esc(user.getPhone()) : "Chua có SÐT" %></span>
                        <% if (!isCustomer) { %>
                            <span class="detail-dot">•</span>
                            <span class="detail-item">?? <%= esc(centreDisplayName) %></span>
                        <% } %>
                    </div>

                    <div class="overview-dates">
                        <span>Tham gia: <%= user != null && user.getCreatedAt() != null ? dateOnly.format(user.getCreatedAt()) : "—" %></span>
                        <span class="detail-dot">•</span>
                        <span>C?p nh?t: <%= user != null && user.getUpdatedAt() != null ? dateOnly.format(user.getUpdatedAt()) : "—" %></span>
                    </div>
                </div>
            </div>

            <div class="overview-stats">
                <% if (isCustomer) { %>
                    <div class="stat-card">
                        <span class="stat-number"><%= history == null ? 0 : history.size() %></span>
                        <span class="stat-label">Vé dã d?t</span>
                    </div>
                <% } else { %>
                    <div class="stat-card">
                        <span class="stat-number" style="font-size: 19px; letter-spacing: 0.5px;"><%= user != null ? user.getEmployeeCode() : "NV00000" %></span>
                        <span class="stat-label">Mã nhân s?</span>
                    </div>
                <% } %>
                <div class="stat-card">
                    <span class="stat-badge" style="background: rgba(16, 185, 129, 0.15); color: #10b981; border: 1px solid rgba(16, 185, 129, 0.3);">? <%= user != null ? esc(user.getStatus()) : "ACTIVE" %></span>
                    <span class="stat-label">Tr?ng thái</span>
                </div>
            </div>
        </section>

        <!-- ===================================================================
             2. LU?I THÔNG TIN TÀI KHO?N (BI?U M?U + B?O M?T + T?NG K?T)
             =================================================================== -->
        <section class="account-grid">
            <!-- C?T TRÁI: FORM C?P NH?T THÔNG TIN -->
            <article class="panel profile-panel">
                <div class="panel-heading">
                    <div class="heading-title-group">
                        <span class="heading-icon">??</span>
                        <div>
                            <h2>Thông tin cá nhân</h2>
                            <p class="panel-desc"><%= isCustomer ? "C?p nh?t h? tên và thông tin liên l?c c?a b?n d? s? d?ng d?ch v? t?t nh?t." : "C?p nh?t h? tên và thông tin liên l?c ph?c v? công tác di?u hành và ph?i h?p t?i CineFlow." %></p>
                        </div>
                    </div>
                </div>

                <form method="post" enctype="multipart/form-data" action="${pageContext.request.contextPath}/account" class="profile-form">
                    <div class="form-group full-width">
                        <label for="fullName">H? và tên <span class="required">*</span></label>
                        <input id="fullName" name="fullName" value="<%= user != null ? esc(user.getFullName()) : "" %>" placeholder="Nh?p h? và tên d?y d?" required>
                    </div>

                    <div class="form-group">
                        <label for="account">Tên tài kho?n</label>
                        <input id="account" value="<%= user != null ? esc(user.getAccount()) : "" %>" disabled title="Tên tài kho?n c? d?nh">
                    </div>

                    <% if (!isCustomer) { %>
                    <div class="form-group">
                        <label for="roleDisplay">Vai trò h? th?ng</label>
                        <input id="roleDisplay" value="<%= user != null ? esc(user.getRoleDisplayName()) : "" %>" disabled style="font-weight: 700; color: #38bdf8;">
                    </div>

                    <div class="form-group full-width">
                        <label for="centreDisplay">C?m r?p tr?c thu?c</label>
                        <input id="centreDisplay" value="<%= esc(centreDisplayName) %>" disabled style="color: #cbd5e1;">
                    </div>
                    <% } %>

                    <div class="form-group">
                        <label for="email">Ð?a ch? Email <span class="required">*</span></label>
                        <input id="email" type="email" name="email" value="<%= user != null ? esc(user.getEmail()) : "" %>" placeholder="name@example.com" required>
                    </div>

                    <div class="form-group">
                        <label for="phone">S? di?n tho?i liên h?</label>
                        <input id="phone" name="phone" value="<%= user != null && user.getPhone() != null ? esc(user.getPhone()) : "" %>" placeholder="<%= isCustomer ? "Nh?p s? di?n tho?i (dùng d? nh?n vé t?i qu?y & Kiosk)" : "09xxxxxxxx" %>">
                    </div>

                    <input id="avatarValue" type="hidden" name="avatar" value="<%= esc(avatarUrl) %>">
                    <input id="avatarUpload" type="file" name="avatarFile" accept="image/png,image/jpeg,image/webp" hidden onchange="previewAvatar(this)">

                    <button type="submit" class="save-btn">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
                            <polyline points="17 21 17 13 7 13 7 21"></polyline>
                            <polyline points="7 3 7 8 15 8"></polyline>
                        </svg>
                        Luu thay d?i
                    </button>
                </form>
            </article>

            <!-- C?T PH?I: B?O M?T & T?NG QUAN -->
            <div class="account-side">
                <!-- T?NG QUAN TÀI KHO?N -->
                <article class="panel account-summary">
                    <div class="panel-heading">
                        <div class="heading-title-group">
                            <span class="heading-icon">??</span>
                            <div>
                                <h2>T?ng quan tài kho?n</h2>
                            </div>
                        </div>
                    </div>

                    <div class="summary-list">
                        <% if (isCustomer) { %>
                            <div class="summary-row">
                                <span class="row-label">Lo?i tài kho?n</span>
                                <strong class="row-value">Khách hàng</strong>
                            </div>
                        <% } else { %>
                            <div class="summary-row">
                                <span class="row-label">Mã nhân s?</span>
                                <strong class="row-value highlight"><%= user != null ? user.getEmployeeCode() : "NV00000" %></strong>
                            </div>
                            <div class="summary-row">
                                <span class="row-label">Vai trò ch?c v?</span>
                                <strong class="row-value"><%= user != null ? esc(user.getRoleDisplayName()) : "Nhân viên" %></strong>
                            </div>
                            <div class="summary-row">
                                <span class="row-label">C?m r?p làm vi?c</span>
                                <strong class="row-value"><%= esc(centreDisplayName) %></strong>
                            </div>
                        <% } %>

                        <div class="summary-row">
                            <span class="row-label">Tr?ng thái</span>
                            <span class="status-pill active">? <%= user != null ? esc(user.getStatus()) : "ACTIVE" %></span>
                        </div>

                        <div class="summary-row">
                            <span class="row-label">Xác th?c Email</span>
                            <% if (user != null && user.isEmailVerified()) { %>
                                <span class="status-pill verified">? Ðã xác th?c</span>
                            <% } else { %>
                                <span class="status-pill unverified">
                                    ? Chua xác th?c
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
                                <span class="row-label">T?ng vé dã d?t</span>
                                <strong class="row-value highlight"><%= history == null ? 0 : history.size() %> vé</strong>
                            </div>
                        <% } %>
                    </div>
                </article>

                <!-- B?O M?T TÀI KHO?N -->
                <article class="panel security-panel">
                    <div class="panel-heading">
                        <div class="heading-title-group">
                            <span class="heading-icon">??</span>
                            <div>
                                <h2>B?o m?t tài kho?n</h2>
                            </div>
                        </div>
                    </div>

                    <a class="security-action-link" href="${pageContext.request.contextPath}/change-password">
                        <div class="action-icon">??</div>
                        <div class="action-text">
                            <strong>Ð?i m?t kh?u</strong>
                            <small>C?p nh?t m?t kh?u d?nh k? d? b?o v? tài kho?n</small>
                        </div>
                        <span class="action-arrow">›</span>
                    </a>

                    <div class="security-tip-box">
                        <span class="tip-icon"><%= isCustomer ? "??" : "???" %></span>
                        <p><%= isCustomer ? "Không chia s? m?t kh?u và mã OTP cho b?t k? ai d? b?o v? tài kho?n CineFlow c?a b?n." : "Tài kho?n nhân s? và qu?n tr? có quy?n truy c?p d? li?u và v?n hành h? th?ng CineFlow. Vui lòng d?nh k? c?p nh?t m?t kh?u và không chia s? cho ngu?i khác." %></p>
                    </div>
                </article>
            </div>
        </section>

        <% if (isCustomer) { %>
        <!-- ===================================================================
             3. B?NG L?CH S? Ð?T VÉ (CH? DÀNH CHO KHÁCH HÀNG)
             =================================================================== -->
        <section class="panel history-panel">
            <div class="panel-heading history-heading">
                <div class="heading-title-group">
                    <span class="heading-icon">???</span>
                    <div>
                        <h2>L?ch s? d?t vé</h2>
                        <p class="panel-desc">Danh sách các su?t chi?u và vé xem phim b?n dã d?t g?n dây.</p>
                    </div>
                </div>
                <a href="${pageContext.request.contextPath}/booking" class="new-booking-btn">
                    Ð?t vé m?i
                </a>
            </div>

            <% if (history == null || history.isEmpty()) { %>
                <div class="empty-history">
                    <div class="empty-icon">??</div>
                    <p>B?n chua có l?n d?t vé nào.</p>
                    <a href="${pageContext.request.contextPath}/booking" class="browse-movies-btn">
                        Khám phá phim dang chi?u
                    </a>
                </div>
            <% } else { %>
                <div class="history-list">
                    <% for (BookingHistory item : history) { %>
                        <article class="history-item">
                            <div class="history-left">
                                <div class="movie-ticket-icon">??</div>
                                <div class="history-details">
                                    <strong><%= esc(item.getMovieTitle()) %></strong>
                                    <span class="booking-code">Mã vé: <code><%= esc(item.getBookingCode()) %></code></span>
                                    <span class="booking-meta">
                                        <%= esc(item.getCentreName()) %> · <%= item.getStartTime() == null ? "Chua có l?ch" : dateTime.format(item.getStartTime()) %>
                                    </span>
                                </div>
                            </div>
                            <div class="history-right">
                                <span class="total-price"><%= money(item.getTotalAmount()) %></span>
                                <span class="booking-status-tag">
                                    ? <%= esc(item.getBookingStatus()) %>
                                </span>
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
        alert('?nh d?i di?n không du?c l?n hon 2MB.');
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
            image.alt = '?nh d?i di?n';
            image.src = e.target.result;
            const initial = document.getElementById('avatarInitial');
            if (initial) initial.replaceWith(image);
        }
    };
    reader.readAsDataURL(file);
}
</script>
</html>
