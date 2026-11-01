<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    Integer statusCode = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
    if (statusCode == null) statusCode = 500;
    String title, message, badge;
    if (statusCode == 400) {
        badge = "YÊU CẦU KHÔNG HỢP LỆ"; title = "Thông tin chưa chính xác"; message = "Vui lòng kiểm tra lại thông tin hoặc đường dẫn bạn vừa gửi.";
    } else if (statusCode == 401) {
        badge = "CẦN ĐĂNG NHẬP"; title = "Bạn chưa đăng nhập"; message = "Đăng nhập để tiếp tục sử dụng chức năng này.";
    } else if (statusCode == 402) {
        badge = "CẦN THANH TOÁN"; title = "Chưa hoàn tất thanh toán"; message = "Hoàn tất thanh toán để tiếp tục thao tác này.";
    } else if (statusCode == 403) {
        badge = "TRUY CẬP BỊ TỪ CHỐI"; title = "Bạn không có quyền vào đây"; message = "Tài khoản hiện tại không được cấp quyền cho trang này.";
    } else if (statusCode == 404) {
        badge = "KHÔNG TÌM THẤY"; title = "Trang này không tồn tại"; message = "Đường dẫn có thể đã thay đổi hoặc không còn khả dụng.";
    } else if (statusCode >= 400 && statusCode < 500) {
        badge = "YÊU CẦU BỊ TỪ CHỐI"; title = "Không thể xử lý yêu cầu"; message = "Vui lòng kiểm tra lại đường dẫn hoặc thử lại.";
    } else {
        statusCode = 500; badge = "LỖI HỆ THỐNG"; title = "Đã có sự cố xảy ra"; message = "Hệ thống chưa thể hoàn tất yêu cầu. Vui lòng thử lại sau.";
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%= statusCode %> | CineFlow</title>
    <style>
        * { box-sizing: border-box; }
        body { margin:0; min-height:100vh; display:grid; place-items:center; padding:28px; color:#20283a; background:radial-gradient(ellipse at 50% 44%,#fff8fa 0,#fff 48%); font-family:Inter,"Segoe UI",Arial,sans-serif; }
        .error { position:relative; width:min(100%,520px); padding:34px 28px 38px; text-align:center; border:1px solid #f0e8ed; border-radius:26px; background:rgba(255,255,255,.92); box-shadow:0 24px 70px rgba(79,45,91,.09),0 4px 14px rgba(237,40,80,.04); }
        .brand { margin-bottom:34px; color:#20283a; font-size:22px; font-weight:850; letter-spacing:-1px; }
        .brand span { color:#ed2850; background:linear-gradient(100deg,#f02b52,#a43ad5 65%,#5468e8); background-clip:text; -webkit-background-clip:text; -webkit-text-fill-color:transparent; }
        .symbol { width:76px; height:76px; display:grid; place-items:center; margin:0 auto 22px; border:1px solid #f4d6e5; border-radius:24px; color:#fff; background:linear-gradient(140deg,#ff5471,#d63ca9 58%,#7866ed); box-shadow:0 12px 28px rgba(218,59,137,.2); }
        .symbol svg { width:30px; height:30px; }
        .badge { margin:0 0 12px; color:#bd3285; font-size:11px; font-weight:800; letter-spacing:1.5px; }
        .code { margin:0; color:#20283a; background:linear-gradient(115deg,#ed2850 8%,#a13bc9 55%,#5265df 95%); background-clip:text; -webkit-background-clip:text; -webkit-text-fill-color:transparent; font-size:clamp(58px,12vw,82px); font-weight:850; letter-spacing:-5px; line-height:1; }
        h1 { margin:16px 0 10px; font-size:clamp(24px,5vw,32px); font-weight:700; letter-spacing:-.8px; }
        .message { max-width:390px; margin:0 auto 26px; color:#737d8e; font-size:15px; line-height:1.7; }
        .back { min-height:46px; display:inline-flex; align-items:center; justify-content:center; gap:9px; padding:0 20px; border:0; border-radius:10px; color:#fff; background:linear-gradient(105deg,#ed2850,#c53ca7 62%,#7161df); box-shadow:0 8px 20px rgba(191,48,129,.22); text-decoration:none; font-size:14px; font-weight:700; transition:filter .18s,transform .18s; }
        .back:hover { transform:translateY(-2px); filter:saturate(1.18) brightness(1.03); }
        .back svg { width:16px; height:16px; }
        @media(max-width:480px) { body{padding:22px}.brand{margin-bottom:34px}.symbol{width:66px;height:66px;border-radius:21px}.message{font-size:14px} }
    </style>
</head>
<body>
    <main class="error">
        <div class="brand">Cine<span>Flow</span></div>
        <div class="symbol" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M4 7.5A2.5 2.5 0 0 1 6.5 5h11A2.5 2.5 0 0 1 20 7.5v9a2.5 2.5 0 0 1-2.5 2.5h-11A2.5 2.5 0 0 1 4 16.5z"/><path d="M9 5v14M15 5v14M4 12h5m6 0h5"/></svg>
        </div>
        <p class="badge"><%= badge %></p>
        <p class="code"><%= statusCode %></p>
        <h1><%= title %></h1>
        <p class="message"><%= message %></p>
        <a class="back" href="javascript:history.back()">Quay lại</a>
    </main>
</body>
</html>
