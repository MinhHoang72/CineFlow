<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Dashboard Admin | CineFlow</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/admin/dashboard.css">
</head>
<body>
<div class="admin-layout">
    <aside class="admin-sidebar">
        <a class="admin-brand" href="${pageContext.request.contextPath}/dashboard">Cine<span>Flow</span></a>
        <p class="admin-caption">KHU VỰC QUẢN TRỊ</p>
        <nav>
            <a class="active" href="${pageContext.request.contextPath}/dashboard"><span>⌂</span> Tổng quan</a>
            <a href="${pageContext.request.contextPath}/reports"><span>▤</span> Báo cáo toàn hệ thống</a>
            <a href="${pageContext.request.contextPath}/staff"><span>♙</span> Quản lý nhân sự</a>
            <a href="${pageContext.request.contextPath}/account"><span>●</span> Hồ sơ Admin</a>
        </nav>
        <a class="admin-logout" href="${pageContext.request.contextPath}/logout"><span>↪</span> Đăng xuất</a>
    </aside>
    <div class="admin-main">
        <header class="admin-topbar"><div><span class="top-label">CINEFLOW ADMIN</span><strong>Trung tâm điều hành</strong></div><a href="${pageContext.request.contextPath}/reports">Xem báo cáo toàn hệ thống →</a></header>
        <main class="admin-content">
            <section class="welcome"><div><p class="eyebrow">TỔNG QUAN HỆ THỐNG</p><h1>Chào mừng trở lại, Admin</h1><p>Theo dõi hoạt động của toàn bộ chuỗi rạp CineFlow tại một nơi.</p></div><div class="welcome-mark">C</div></section>
            <section class="admin-section"><div class="section-heading"><div><h2>Khu vực điều hành</h2><p>Chọn chức năng bạn muốn truy cập.</p></div></div><div class="admin-actions">
                <a class="action-card action-primary" href="${pageContext.request.contextPath}/reports"><span class="action-icon">▥</span><div><strong>Báo cáo toàn hệ thống</strong><small>Doanh thu, tỷ lệ lấp đầy và top phim bán chạy theo toàn chuỗi.</small></div><b>→</b></a>
                <a class="action-card" href="${pageContext.request.contextPath}/staff"><span class="action-icon">♙</span><div><strong>Quản lý nhân sự</strong><small>Xem danh sách Manager và Staff, trung tâm và trạng thái tài khoản.</small></div><b>→</b></a>
                <a class="action-card" href="${pageContext.request.contextPath}/account"><span class="action-icon">●</span><div><strong>Hồ sơ Admin</strong><small>Cập nhật thông tin cá nhân và bảo mật tài khoản.</small></div><b>→</b></a>
            </div></section>
            <section class="admin-section guide"><div class="section-heading"><div><h2>Phạm vi của Admin</h2><p>Admin theo dõi và điều hành ở cấp độ toàn hệ thống.</p></div></div><div class="guide-grid"><div><b>Toàn chuỗi</b><span>Xem số liệu tổng hợp của tất cả trung tâm.</span></div><div><b>Nhân sự</b><span>Kiểm tra tài khoản và trạng thái nhân viên.</span></div><div><b>Phối hợp</b><span>Manager chịu trách nhiệm quản lý phim và phân phối phim.</span></div></div></section>
        </main>
    </div>
</div>
</body>
</html>
