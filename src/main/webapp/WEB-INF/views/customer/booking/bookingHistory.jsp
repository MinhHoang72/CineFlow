<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="model.dto.booking.BookingHistoryDTO,java.util.List,java.text.SimpleDateFormat" %>
<%-- 5.5 Lich su dat ve cua khach hang (tach khoi trang ho so /account). Can co: bookingHistory. --%>
<%!
    private String esc(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
    private String money(java.math.BigDecimal value) {
        return value == null ? "0 đ" : String.format("%,.0f đ", value);
    }
%>
<%
    List<BookingHistoryDTO> history = (List<BookingHistoryDTO>) request.getAttribute("bookingHistory");
    SimpleDateFormat dateTime = new SimpleDateFormat("dd/MM/yyyy HH:mm");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Lịch sử đặt vé | CineFlow</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/reset.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/variables.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/common/layout.css?v=<%= System.currentTimeMillis() %>">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/account/account.css?v=<%= System.currentTimeMillis() %>">
</head>
<body>
<div class="page-shell">
    <jsp:include page="/common/header.jsp"/>
    <main class="account-page">

        <a href="${pageContext.request.contextPath}/account" class="history-back-link">← Hồ sơ của tôi</a>

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
                                    <% if (item.isShowtimeCancelled()) { %>
                                        <span style="font-size: 11.5px; color: #ef4444; margin-top: 5px; font-weight: 600;">
                                            ⚠️ Suất chiếu đã huỷ · Bấm "Xem vé" để biết cách hoàn tiền
                                        </span>
                                    <% } %>
                                </div>
                            </div>
                            <div class="history-right">
                                <span class="total-price"><%= money(item.getTotalAmount()) %></span>
                                <%
                                    String st = item.getBookingStatus();
                                    boolean isCancelled = "CANCELLED".equalsIgnoreCase(st);
                                    String tagClass = item.isPayable() ? " is-pending"
                                            : isCancelled ? " is-cancelled"
                                            : ("CONFIRMED".equals(st) || "COMPLETED".equals(st)) ? "" : " is-muted";
                                %>
                                <span class="booking-status-tag<%= tagClass %>"<%= isCancelled ? " style=\"background: rgba(239, 68, 68, 0.15); color: #ef4444; border: 1px solid rgba(239, 68, 68, 0.35); font-weight: 700;\"" : "" %>>
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
    </main>
    <jsp:include page="/common/footer.jsp"/>
</div>
</body>
</html>
