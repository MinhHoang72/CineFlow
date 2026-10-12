<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="dao.CinemaDAO,model.Branch,model.User" %>
<%!
    private User sessionUser(Object value) {
        if (value instanceof User) {
            return (User) value;
        }
        if (value == null) {
            return null;
        }
        try {
            User compatible = new User();
            Class<?> type = value.getClass();
            compatible.setFullName((String) type.getMethod("getFullName").invoke(value));
            compatible.setAccount((String) type.getMethod("getAccount").invoke(value));
            compatible.setRole((String) type.getMethod("getRole").invoke(value));
            return compatible;
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            return null;
        }
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    String contextPath = request.getContextPath();
    User signedInUser = sessionUser(session.getAttribute("user"));
    String memberName = signedInUser == null ? "" : signedInUser.getFullName();
    if (memberName == null || memberName.trim().isEmpty()) {
        memberName = signedInUser == null ? "" : signedInUser.getAccount();
    }

    int branchId = -1;
    try {
        String idParam = request.getParameter("branchId");
        if (idParam != null && !idParam.trim().isEmpty()) {
            branchId = Integer.parseInt(idParam.trim());
        }
    } catch (Exception ignored) {
        branchId = -1;
    }

    CinemaDAO cinemaDAO = new CinemaDAO();
    Branch branch = branchId > 0 ? cinemaDAO.getBranchById(branchId) : null;
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="theme-color" content="#e21b23">
    <title><%= branch != null ? escapeHtml(branch.getBranchName()) + " | CineFlow Cinema" : "Thông tin rạp chiếu phim | CineFlow Cinema" %></title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Barlow+Condensed:wght@500;600;700&family=Be+Vietnam+Pro:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="<%= contextPath %>/css/home/index.css?v=<%= System.currentTimeMillis() %>">
    <link rel="stylesheet" href="<%= contextPath %>/css/cinema/cinemas.css?v=<%= System.currentTimeMillis() %>">
</head>
<body id="top">
<div class="page-shell">

    <!-- Top Utility Bar (Synchronized with home/index.jsp) -->
    <div class="top-bar">
        <div class="top-inner">
            <div class="top-left">
                <span>CineFlow</span>
                <span>Hệ thống rạp chiếu phim</span>
            </div>
            <div class="top-right">
                <% if (signedInUser == null) { %>
                    <a href="<%= contextPath %>/auth/login.jsp">Đăng nhập</a>
                    <a href="<%= contextPath %>/auth/register.jsp">Đăng ký</a>
                <% } else { %>
                    <span>Xin chào, <%= escapeHtml(memberName) %></span>
                    <a href="<%= contextPath %>/account">Tài khoản của tôi</a>
                    <% if ("ADMIN".equals(signedInUser.getRole())) { %>
                        <a href="<%= contextPath %>/admin/movies">Quản lý phim</a>
                    <% } else if ("MANAGER".equals(signedInUser.getRole())) { %>
                        <a href="<%= contextPath %>/staff">Trung tâm của tôi</a>
                    <% } %>
                    <form class="account-form" action="<%= contextPath %>/logout" method="post">
                        <button type="submit">Đăng xuất</button>
                    </form>
                <% } %>
                <a href="<%= contextPath %>/home/index.jsp#support">Hỗ trợ khách hàng</a>
            </div>
        </div>
    </div>

    <!-- CineFlow Brand Header (Synchronized with home/index.jsp) -->
    <header class="brand-header">
        <a class="logo" href="<%= contextPath %>/home/index.jsp" aria-label="CineFlow Cinema">
            <span class="logo-mark">C</span>
            <span class="logo-word">Cine<span>Flow</span></span>
        </a>
    </header>

    <!-- Main Navigation Bar (Synchronized with home/index.jsp) -->
    <nav class="main-nav" aria-label="Điều hướng chính">
        <div class="nav-inner">
            <a href="<%= contextPath %>/home/index.jsp">TRANG CHỦ</a>
            <a href="<%= contextPath %>/booking/booking.jsp">MUA VÉ</a>
            <a href="<%= contextPath %>/home/index.jsp#promotions">TIN MỚI &amp; ƯU ĐÃI</a>
            <a class="active" href="<%= contextPath %>/cinema/index.jsp">RẠP CHIẾU PHIM</a>
            <a href="<%= contextPath %>/home/index.jsp#support">HỖ TRỢ KHÁCH HÀNG</a>
        </div>
    </nav>

    <!-- Breadcrumb Bar -->
    <div class="cinema-breadcrumb-wrap">
        <div class="breadcrumb-inner">
            <a href="<%= contextPath %>/home/index.jsp">Trang chủ</a>
            <span class="breadcrumb-sep">›</span>
            <a href="<%= contextPath %>/cinema/index.jsp">Rạp chiếu phim</a>
            <% if (branch != null) { %>
                <span class="breadcrumb-sep">›</span>
                <span class="breadcrumb-current"><%= escapeHtml(branch.getBranchName()) %></span>
            <% } %>
        </div>
    </div>

    <!-- Main Container -->
    <main class="cinema-main-container">

        <% if (branch != null) { %>
            <!-- Back Link -->
            <div class="detail-nav-back">
                <a href="<%= contextPath %>/cinema/index.jsp?city=<%= java.net.URLEncoder.encode(branch.getCity() != null ? branch.getCity() : "", "UTF-8") %>" 
                   class="btn-back-link">
                    <span>← Quay lại danh sách rạp tại <%= escapeHtml(branch.getCity()) %></span>
                </a>
            </div>

            <!-- Cinema Detail Card -->
            <article class="cinema-detail-card">
                
                <!-- Visual Header -->
                <div class="detail-visual-header">
                    <div class="detail-visual-badge">CINEFLOW CINEMA</div>
                    <div class="detail-visual-center">
                        <span class="detail-visual-c">C</span>
                        <span class="detail-visual-brand">CINEFLOW</span>
                    </div>
                    <span class="detail-city-tag">📍 <%= escapeHtml(branch.getCity()) %></span>
                </div>

                <!-- Detail Body Content -->
                <div class="detail-card-body">
                    
                    <div class="detail-title-section">
                        <span class="detail-kicker">THÔNG TIN CHI TIẾT CỤM RẠP</span>
                        <h1 class="detail-cinema-title"><%= escapeHtml(branch.getBranchName()) %></h1>
                        <p class="detail-intro-desc">
                            Cụm rạp <strong><%= escapeHtml(branch.getBranchName()) %></strong> tọa lạc tại vị trí đắc địa ở <%= escapeHtml(branch.getCity()) %>, mang đến không gian giải trí điện ảnh đẳng cấp với hệ thống phòng chiếu hiện đại, công nghệ âm thanh sống động và chất lượng phục vụ chu đáo chuẩn quốc tế.
                        </p>
                    </div>

                    <!-- Information Specs Grid -->
                    <div class="detail-info-grid">
                        
                        <!-- Address -->
                        <div class="detail-info-item">
                            <div class="info-icon-box">📍</div>
                            <div class="info-content-box">
                                <span class="info-field-label">Địa chỉ rạp</span>
                                <strong class="info-field-value"><%= escapeHtml(branch.getAddress()) %></strong>
                                <span class="info-field-sub"><%= escapeHtml(branch.getCity()) %></span>
                            </div>
                        </div>

                        <!-- Hotline -->
                        <div class="detail-info-item">
                            <div class="info-icon-box">📞</div>
                            <div class="info-content-box">
                                <span class="info-field-label">Hotline liên hệ</span>
                                <strong class="info-field-value"><%= escapeHtml(branch.getPhone() != null && !branch.getPhone().trim().isEmpty() ? branch.getPhone() : "1900 2099") %></strong>
                                <span class="info-field-sub">Hỗ trợ khách hàng từ 08:00 - 23:00 hàng ngày</span>
                            </div>
                        </div>

                        <!-- Halls Count -->
                        <div class="detail-info-item">
                            <div class="info-icon-box">🏛️</div>
                            <div class="info-content-box">
                                <span class="info-field-label">Quy mô phòng chiếu</span>
                                <strong class="info-field-value"><%= branch.getHallCount() %> phòng chiếu</strong>
                                <span class="info-field-sub">Trang bị màn chiếu kỹ thuật số Laser 4K</span>
                            </div>
                        </div>

                        <!-- Seats Count -->
                        <div class="detail-info-item">
                            <div class="info-icon-box">💺</div>
                            <div class="info-content-box">
                                <span class="info-field-label">Sức chứa khán phòng</span>
                                <strong class="info-field-value"><%= branch.getSeatCount() %> ghế ngồi</strong>
                                <span class="info-field-sub">Ghế da cao cấp, khoảng cách ngồi thoải mái</span>
                            </div>
                        </div>

                    </div>

                    <!-- Actions Row -->
                    <div class="detail-actions-row">
                        <!-- Primary CTA: Go to Booking schedule -->
                        <a href="<%= contextPath %>/booking/booking.jsp?branchId=<%= branch.getBranchId() %>&amp;view=schedule" 
                           class="btn-primary-booking">
                            <span class="btn-cta-icon">🎟</span>
                            <span>Xem lịch chiếu &amp; Đặt vé tại rạp này</span>
                            <span class="btn-cta-arrow">→</span>
                        </a>

                        <!-- Map CTA -->
                        <a href="https://www.google.com/maps/search/?api=1&amp;query=<%= java.net.URLEncoder.encode(branch.getBranchName() + " " + branch.getAddress(), "UTF-8") %>" 
                           target="_blank" 
                           rel="noopener noreferrer" 
                           class="btn-secondary-map">
                            <span class="btn-cta-icon">🗺️</span>
                            <span>Xem chỉ đường Google Maps</span>
                            <span class="btn-cta-ext">↗</span>
                        </a>
                    </div>

                </div>

            </article>

        <% } else { %>

            <div class="empty-cinemas-box">
                <div class="empty-icon">⚠️</div>
                <h3>Không tìm thấy rạp chiếu phim</h3>
                <p>Rạp bạn đang tìm kiếm không tồn tại hoặc đã ngừng hoạt động trên hệ thống.</p>
                <a href="<%= contextPath %>/cinema/index.jsp" class="btn-empty-home">Quay lại danh sách rạp</a>
            </div>

        <% } %>

    </main>

    <!-- Quick Floating Rail (Synchronized with home/index.jsp) -->
    <nav class="quick-rail" aria-label="Tiện ích nhanh">
        <a href="<%= contextPath %>/booking/booking.jsp">
            <span class="quick-icon">🎟</span>
            <span>Đặt vé</span>
        </a>
        <a href="<%= contextPath %>/home/index.jsp#support">
            <span class="quick-icon">❔</span>
            <span>Hỗ trợ</span>
        </a>
        <a class="top" href="#top">
            <span class="quick-icon">↑</span>
            <span>Đầu trang</span>
        </a>
    </nav>

    <!-- Standard Footer (Synchronized with home/index.jsp) -->
    <footer class="footer">
        <div class="footer-inner">
            <div class="footer-brand">
                <a class="logo" href="<%= contextPath %>/home/index.jsp">
                    <span class="logo-mark">C</span>
                    <span class="logo-word">Cine<span>Flow</span></span>
                </a>
                <p>Hệ thống rạp chiếu phim CineFlow — Trải nghiệm điện ảnh đỉnh cao.</p>
            </div>
            <div>
                <h4>CINEFLOW</h4>
                <a href="<%= contextPath %>/home/index.jsp#promotions">Tin mới &amp; ưu đãi</a>
                <a href="<%= contextPath %>/cinema/index.jsp">Rạp chiếu phim</a>
                <a href="<%= contextPath %>/home/index.jsp#movies">Phim đang chiếu</a>
                <a href="<%= contextPath %>/booking/booking.jsp?view=schedule">Lịch chiếu</a>
            </div>
            <div id="support">
                <h4>HỖ TRỢ</h4>
                <a href="<%= contextPath %>/home/index.jsp#support">Hỗ trợ khách hàng</a>
                <a href="<%= contextPath %>/home/index.jsp#support">Câu hỏi thường gặp</a>
                <a href="<%= contextPath %>/home/index.jsp#support">Chính sách bảo mật</a>
                <a href="<%= contextPath %>/home/index.jsp#support">Điều khoản sử dụng</a>
            </div>
            <div>
                <h4>TÀI KHOẢN</h4>
                <% if (signedInUser == null) { %>
                    <a href="<%= contextPath %>/auth/login.jsp">Đăng nhập</a>
                    <a href="<%= contextPath %>/auth/register.jsp">Tạo tài khoản</a>
                <% } else { %>
                    <a href="<%= contextPath %>/account">Tài khoản của tôi</a>
                <% } %>
            </div>
        </div>
        <div class="copyright">© 2026 CineFlow. All rights reserved.</div>
    </footer>

</div>
</body>
</html>
