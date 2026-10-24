<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="dao.CinemaDAO,model.entity.Centre,model.entity.User" %>
<%@ page import="java.util.List,java.util.ArrayList,java.util.Map,java.util.LinkedHashMap,java.net.URLEncoder" %>
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

    private String getCinemaImageUrl(int centreId) {
        String[] images = {
            "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=600&h=380&q=80",
            "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?auto=format&fit=crop&w=600&h=380&q=80",
            "https://images.unsplash.com/photo-1485846234645-a62644f84728?auto=format&fit=crop&w=600&h=380&q=80",
            "https://images.unsplash.com/photo-1595769816263-9b910be24d5f?auto=format&fit=crop&w=600&h=380&q=80"
        };
        int index = Math.abs(centreId) % images.length;
        return images[index];
    }
%>
<%
    String contextPath = request.getContextPath();

    String centreIdParam = request.getParameter("centreId");
    if (centreIdParam == null || centreIdParam.trim().isEmpty()) centreIdParam = request.getParameter("centreId");
    if (centreIdParam != null && !centreIdParam.trim().isEmpty()) {
        response.sendRedirect(contextPath + "/cinema/details?centreId=" + URLEncoder.encode(centreIdParam.trim(), "UTF-8"));
        return;
    }

    User signedInUser = sessionUser(session.getAttribute("user"));
    String memberName = signedInUser == null ? "" : signedInUser.getFullName();
    if (memberName == null || memberName.trim().isEmpty()) {
        memberName = signedInUser == null ? "" : signedInUser.getAccount();
    }

    CinemaDAO cinemaDAO = new CinemaDAO();
    Map<String, Integer> activeCities = cinemaDAO.getActiveCitiesWithCount();
    List<Centre> activeCentres = cinemaDAO.getActiveCentres();

    // Nhóm các rạp chiếu theo thành phố từ cơ sở dữ liệu
    Map<String, List<Centre>> centresByCity = new LinkedHashMap<>();
    for (String cityName : activeCities.keySet()) {
        centresByCity.put(cityName, new ArrayList<>());
    }
    for (Centre centre : activeCentres) {
        String city = centre.getCity() != null ? centre.getCity().trim() : "";
        for (String cityName : activeCities.keySet()) {
            if (cityName.equalsIgnoreCase(city)) {
                centresByCity.get(cityName).add(centre);
                break;
            }
        }
    }

    // Xác định thành phố mặc định hiển thị
    String defaultCity = "";
    String requestedCity = request.getParameter("city");
    if (requestedCity != null && !requestedCity.trim().isEmpty()) {
        for (String cityName : activeCities.keySet()) {
            if (cityName.equalsIgnoreCase(requestedCity.trim())) {
                defaultCity = cityName;
                break;
            }
        }
    }
    if (defaultCity.isEmpty() && !activeCities.isEmpty()) {
        if (activeCities.containsKey("Hà Nội")) {
            defaultCity = "Hà Nội";
        } else {
            defaultCity = activeCities.keySet().iterator().next();
        }
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="theme-color" content="#252525">
    <title>Hệ thống trung tâm chiếu phim | CineFlow</title>
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

    <!-- Khu vực nội dung chính -->
    <main class="cinema-page-wrapper">
        <!-- Đường dẫn điều hướng phân cấp (Breadcrumb) -->
        <nav class="cf-breadcrumb-nav" aria-label="Breadcrumb">
            <a href="<%= contextPath %>/home">Trang chủ</a>
            <span class="cf-breadcrumb-sep">›</span>
            <span class="cf-breadcrumb-current">Hệ thống trung tâm chiếu phim</span>
        </nav>

        <div class="cinema-main-wrap">

            <!-- Tiêu đề trang bên ngoài khung (phong cách phẳng, hiện đại) -->
            <div class="cf-page-header">
                <h1 class="cf-page-title">HỆ THỐNG TRUNG TÂM CHIẾU PHIM</h1>
            </div>

            <!-- Khung nội dung chính -->
            <div class="cinema-container">

            <!-- Bộ chọn thành phố (Nút pill) -->
            <% if (activeCities.isEmpty()) { %>
                <div class="cf-empty-state">
                    Hiện chưa có trung tâm chiếu phim nào hoạt động trên hệ thống.
                </div>
            <% } else { %>
                <div class="cf-city-selector" role="tablist" aria-label="Chọn thành phố">
                    <% for (Map.Entry<String, Integer> entry : activeCities.entrySet()) { 
                        String cityName = entry.getKey();
                        int count = entry.getValue();
                        boolean isSelected = cityName.equals(defaultCity);
                    %>
                        <button type="button" 
                                class="cf-city-pill <%= isSelected ? "active" : "" %>"
                                data-city="<%= escapeHtml(cityName) %>"
                                role="tab"
                                aria-selected="<%= isSelected ? "true" : "false" %>"
                                onclick="selectCity('<%= escapeHtml(cityName) %>')">
                            <span><%= escapeHtml(cityName) %></span>
                            <span class="pill-count">(<%= count %>)</span>
                        </button>
                    <% } %>
                </div>

                <!-- Đường phân cách mảnh -->
                <hr class="cf-section-divider">

                <!-- Tiêu đề động theo thành phố đang chọn -->
                <h2 class="cf-city-heading" id="dynamicCityTitle">
                    TRUNG TÂM CHIẾU PHIM TẠI <%= escapeHtml(defaultCity.toUpperCase()) %>
                </h2>

                <!-- Danh sách trung tâm theo từng thành phố -->
                <% for (Map.Entry<String, List<Centre>> entry : centresByCity.entrySet()) { 
                    String cityName = entry.getKey();
                    List<Centre> cityCentres = entry.getValue();
                    boolean isDefaultGroup = cityName.equals(defaultCity);
                %>
                    <div class="cf-cinema-group" 
                         id="group-<%= escapeHtml(cityName) %>" 
                         style="<%= isDefaultGroup ? "" : "display: none;" %>">
                        
                        <% if (cityCentres.isEmpty()) { %>
                            <div class="cf-empty-state">
                                Chưa có trung tâm chiếu phim đang hoạt động tại <%= escapeHtml(cityName) %>.
                            </div>
                        <% } else { %>
                            <div class="cf-cinema-list">
                                <% for (Centre centre : cityCentres) { 
                                    String phone = (centre.getPhone() != null && !centre.getPhone().trim().isEmpty()) 
                                                    ? centre.getPhone().trim() 
                                                    : "1900 2099";
                                %>
                                    <article class="cf-cinema-card">
                                        <div class="cf-cinema-img-wrap">
                                            <img class="cf-cinema-img" 
                                                 src="<%= getCinemaImageUrl(centre.getCentreId()) %>" 
                                                 alt="<%= escapeHtml(centre.getCentreName()) %>" 
                                                 loading="lazy">
                                        </div>
                                        <div class="cf-cinema-body">
                                            <h3 class="cf-cinema-name">
                                                <a href="<%= contextPath %>/cinema/details?centreId=<%= centre.getCentreId() %>">
                                                    <%= escapeHtml(centre.getCentreName()) %>
                                                </a>
                                            </h3>
                                            <p class="cf-cinema-address">Địa chỉ: <%= escapeHtml(centre.getAddress()) %></p>
                                            <p class="cf-cinema-phone">Số điện thoại: <strong><%= escapeHtml(phone) %></strong></p>
                                        </div>
                                        <div class="cf-cinema-action">
                                            <a href="<%= contextPath %>/cinema/details?centreId=<%= centre.getCentreId() %>" 
                                               class="cf-btn-detail">
                                                Chi tiết trung tâm
                                            </a>
                                        </div>
                                    </article>
                                <% } %>
                            </div>
                        <% } %>

                    </div>
                <% } %>

            <% } %>

        </div>
        </div>
    </main>

    <!-- Chân trang chung -->
    <jsp:include page="/common/footer.jsp"/>

</div>

<!-- Script chuyển đổi thành phố phía máy khách -->
<script>
    function selectCity(cityName) {
        if (!cityName) return;

        // 1. Cập nhật trạng thái active cho các nút thành phố
        var pills = document.querySelectorAll('.cf-city-pill');
        pills.forEach(function(pill) {
            var isCurrent = pill.getAttribute('data-city') === cityName;
            if (isCurrent) {
                pill.classList.add('active');
                pill.setAttribute('aria-selected', 'true');
            } else {
                pill.classList.remove('active');
                pill.setAttribute('aria-selected', 'false');
            }
        });

        // 2. Cập nhật tiêu đề động
        var dynamicTitle = document.getElementById('dynamicCityTitle');
        if (dynamicTitle) {
            dynamicTitle.textContent = 'TRUNG TÂM CHIẾU PHIM TẠI ' + cityName.toUpperCase();
        }

        // 3. Hiển thị danh sách rạp của thành phố được chọn, ẩn các thành phố khác
        var groups = document.querySelectorAll('.cf-cinema-group');
        groups.forEach(function(group) {
            if (group.id === 'group-' + cityName) {
                group.style.display = '';
            } else {
                group.style.display = 'none';
            }
        });

        // 4. Cập nhật URL trình duyệt (không tải lại trang)
        if (window.history && window.history.replaceState) {
            var newUrl = window.location.pathname + '?city=' + encodeURIComponent(cityName);
            window.history.replaceState(null, '', newUrl);
        }
    }
</script>
</body>
</html>
