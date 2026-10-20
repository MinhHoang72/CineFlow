<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="dao.CinemaDAO,model.Centre,model.User" %>
<%@ page import="java.net.URLEncoder" %>
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

    private String getCinemaHeroImageUrl(int centreId) {
        String[] heroImages = {
            "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=1200&q=85",
            "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?auto=format&fit=crop&w=1200&q=85",
            "https://images.unsplash.com/photo-1485846234645-a62644f84728?auto=format&fit=crop&w=1200&q=85",
            "https://images.unsplash.com/photo-1595769816263-9b910be24d5f?auto=format&fit=crop&w=1200&q=85"
        };
        int index = Math.abs(centreId) % heroImages.length;
        return heroImages[index];
    }
%>
<%
    String contextPath = request.getContextPath();
    User signedInUser = sessionUser(session.getAttribute("user"));
    String memberName = signedInUser == null ? "" : signedInUser.getFullName();
    if (memberName == null || memberName.trim().isEmpty()) {
        memberName = signedInUser == null ? "" : signedInUser.getAccount();
    }

    int centreId = -1;
    try {
        String idParam = request.getParameter("centreId");
        if (idParam != null && !idParam.trim().isEmpty()) {
            centreId = Integer.parseInt(idParam.trim());
        }
    } catch (Exception ignored) {
        centreId = -1;
    }

    CinemaDAO cinemaDAO = new CinemaDAO();
    Centre centre = centreId > 0 ? cinemaDAO.getCentreById(centreId) : null;
    String cityName = centre != null && centre.getCity() != null ? centre.getCity().trim() : "";
    String phone = centre != null && centre.getPhone() != null && !centre.getPhone().trim().isEmpty() 
                    ? centre.getPhone().trim() 
                    : "1900 2099";
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="theme-color" content="#252525">
    <title><%= centre != null ? escapeHtml(centre.getCentreName()) + " | CineFlow Centre" : "Thông tin trung tâm chiếu phim | CineFlow Cinema" %></title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="<%= contextPath %>/css/common/layout.css?v=<%= System.currentTimeMillis() %>">
    <link rel="stylesheet" href="<%= contextPath %>/css/cinema/cinemas.css?v=<%= System.currentTimeMillis() %>">
</head>
<body id="top">
<div class="page-shell">

    <!-- Thanh điều hướng và đầu trang chung -->
    <jsp:include page="/common/header.jsp">
        <jsp:param name="activeNav" value="cinema"/>
    </jsp:include>

    <!-- Khung chứa nội dung chính -->
    <main class="cinema-page-wrapper">
        <div class="cinema-main-wrap">
            <div class="cinema-container">

            <% if (centre != null) { %>
                <div class="cf-detail-container">
                    
                    <!-- Quay lại danh sách trung tâm -->
                    <div class="cf-detail-back-bar">
                        <a href="<%= contextPath %>/cinema/index.jsp?city=<%= URLEncoder.encode(cityName, "UTF-8") %>" 
                           class="cf-btn-back">
                            Quay lại danh sách trung tâm tại <%= escapeHtml(cityName) %>
                        </a>
                    </div>

                    <!-- Card Chi Tiết Trung Tâm -->
                    <article class="cf-detail-card">
                        
                        <!-- Hero Banner trung tâm -->
                        <div class="cf-detail-img-banner">
                            <img src="<%= getCinemaHeroImageUrl(centre.getCentreId()) %>" 
                                 alt="Không gian <%= escapeHtml(centre.getCentreName()) %>" 
                                 loading="lazy">
                            <div class="cf-detail-img-overlay">
                                <span class="cf-detail-badge">CineFlow Centre</span>
                                <h1 class="cf-detail-title"><%= escapeHtml(centre.getCentreName()) %></h1>
                                <span class="cf-detail-city-tag">Khu vực: <%= escapeHtml(cityName) %></span>
                            </div>
                        </div>

                        <!-- Nội dung chi tiết trung tâm -->
                        <div class="cf-detail-body">
                            
                            <!-- Giới thiệu ngắn -->
                            <p class="cf-detail-intro">
                                Trung tâm chiếu phim <strong><%= escapeHtml(centre.getCentreName()) %></strong> tọa lạc tại vị trí thuận tiện ở <%= escapeHtml(cityName) %>, mang đến không gian giải trí điện ảnh đẳng cấp với màn hình sắc nét tiêu chuẩn quốc tế và chất lượng phục vụ tận tâm, mang lại trải nghiệm điện ảnh trọn vẹn nhất cho khán giả.
                            </p>

                            <!-- Bảng thông tin liên hệ và địa chỉ -->
                            <div class="cf-detail-info-table">
                                <div class="cf-detail-info-row">
                                    <span class="cf-detail-label">Tên trung tâm:</span>
                                    <span class="cf-detail-value"><strong><%= escapeHtml(centre.getCentreName()) %></strong></span>
                                </div>
                                <div class="cf-detail-info-row">
                                    <span class="cf-detail-label">Khu vực:</span>
                                    <span class="cf-detail-value"><%= escapeHtml(cityName) %></span>
                                </div>
                                <div class="cf-detail-info-row">
                                    <span class="cf-detail-label">Địa chỉ:</span>
                                    <span class="cf-detail-value"><%= escapeHtml(centre.getAddress()) %></span>
                                </div>
                                <div class="cf-detail-info-row">
                                    <span class="cf-detail-label">Số điện thoại:</span>
                                    <span class="cf-detail-value"><strong><%= escapeHtml(phone) %></strong></span>
                                </div>
                                <div class="cf-detail-info-row">
                                    <span class="cf-detail-label">Giờ mở cửa:</span>
                                    <span class="cf-detail-value">08:00 – 23:30 (Tất cả các ngày trong tuần)</span>
                                </div>
                            </div>

                            <!-- Chỉ đường Google Maps -->
                            <div class="cf-detail-actions">
                                <a href="https://www.google.com/maps/search/?api=1&amp;query=<%= URLEncoder.encode(centre.getCentreName() + " " + centre.getAddress(), "UTF-8") %>" 
                                   target="_blank" 
                                   rel="noopener noreferrer" 
                                   class="cf-btn-map">
                                    Xem vị trí &amp; chỉ đường trên Google Maps
                                </a>
                            </div>

                        </div>

                    </article>

                </div>

            <% } else { %>

                <div class="cf-empty-state">
                    Không tìm thấy thông tin cụm rạp chiếu phim này hoặc rạp đã ngừng hoạt động.
                    <div style="margin-top: 15px;">
                        <a href="<%= contextPath %>/cinema/index.jsp" class="cf-btn-detail">Quay lại hệ thống rạp</a>
                    </div>
                </div>

            <% } %>

        </div>
        </div>
    </main>

    <!-- Chân trang chung -->
    <jsp:include page="/common/footer.jsp"/>

</div>
</body>
</html>
