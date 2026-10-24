<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    String authRole = (String) session.getAttribute("role");
    if (authRole == null && session.getAttribute("user") instanceof model.entity.User) {
        authRole = ((model.entity.User) session.getAttribute("user")).getRole();
    }
    String contextPath = request.getContextPath();
    if (authRole != null && !"CUSTOMER".equalsIgnoreCase(authRole)) {
        response.sendRedirect(contextPath + utils.WebUtil.getRoleDefaultPath(session));
        return;
    }
%>
<!DOCTYPE html>
<html lang="vi">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Trung tâm hỗ trợ &amp; Chăm sóc khách hàng | CineFlow</title>
        <link rel="preconnect" href="https://fonts.googleapis.com">
        <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
        <link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700&display=swap" rel="stylesheet">
        <link rel="stylesheet" href="<%= contextPath %>/css/common/layout.css?v=<%= System.currentTimeMillis() %>">
        <link rel="stylesheet" href="<%= contextPath %>/css/support/support.css?v=<%= System.currentTimeMillis() %>">
    </head>
    <body id="top">
        <div class="page-shell">

            <!-- Thanh điều hướng và đầu trang chung -->
            <jsp:include page="/common/header.jsp">
                <jsp:param name="activeNav" value="support"/>
            </jsp:include>

            <!-- Khu vực nội dung chính -->
            <main class="support-page-wrapper">
                <!-- Đường dẫn phân cấp (Breadcrumb) -->
                <nav class="cf-breadcrumb-nav" aria-label="Breadcrumb">
                    <a href="<%= contextPath %>/home">Trang chủ</a>
                    <span class="cf-breadcrumb-sep">›</span>
                    <span class="cf-breadcrumb-current">Hỗ trợ khách hàng</span>
                </nav>

                <!-- Banner Giới thiệu Trung tâm Chăm sóc khách hàng CineFlow -->
                <section class="support-hero">
                    <div class="support-hero-badge">CINEFLOW CUSTOMER CARE</div>
                    <h1>TRUNG TÂM HỖ TRỢ KHÁCH HÀNG</h1>
                    <p>
                        Đội ngũ chăm sóc khách hàng CineFlow luôn sẵn sàng lắng nghe và giải đáp mọi thắc mắc
                        về lịch chiếu, quy trình đặt vé, thanh toán trực tuyến, chính sách hoàn hủy vé và ưu đãi thành viên.
                    </p>
                </section>

                <!-- 4 Thẻ thông tin liên hệ chính (Hotline, Email, SĐT di động, Giờ làm việc) -->
                <section class="support-cards-grid" aria-label="Kênh liên hệ trực tiếp">
                    <!-- 1. TỔNG ĐÀI CSKH -->
                    <div class="support-card">
                        <div class="support-card-icon red">📞</div>
                        <div class="support-card-title">Tổng đài CSKH (Toàn quốc)</div>
                        <div class="support-card-value">
                            <a href="tel:19006868">1900 6868</a>
                        </div>
                        <p class="support-card-desc">Cước phí 1.000 đ/phút. Hỗ trợ tra cứu sự cố thanh toán và thông tin phòng chiếu.</p>
                    </div>

                    <!-- 2. EMAIL HỖ TRỢ -->
                    <div class="support-card">
                        <div class="support-card-icon emerald">✉</div>
                        <div class="support-card-title">Email tiếp nhận hỗ trợ</div>
                        <div class="support-card-value">
                            <a href="mailto:chamsockhachhang@cineflow.vn">chamsockhachhang@cineflow.vn</a>
                        </div>
                        <p class="support-card-desc">Tiếp nhận yêu cầu xuất hóa đơn VAT, phản ánh dịch vụ và đối soát hoàn tiền 24/7.</p>
                    </div>

                    <!-- 3. HOTLINE DI ĐỘNG VIỆT NAM -->
                    <div class="support-card">
                        <div class="support-card-icon blue">📱</div>
                        <div class="support-card-title">Đường dây nóng di động</div>
                        <div class="support-card-value">
                            <a href="tel:0988123456">0988 123 456</a>
                        </div>
                        <p class="support-card-desc">Hỗ trợ nhanh qua tin nhắn SMS, Zalo CSKH và giải quyết sự cố ca chiếu trực tiếp.</p>
                    </div>

                    <!-- 4. KHUNG GIỜ LÀM VIỆC -->
                    <div class="support-card">
                        <div class="support-card-icon amber">🕒</div>
                        <div class="support-card-title">Khung giờ hoạt động</div>
                        <div class="support-card-value">08:00 - 23:00</div>
                        <p class="support-card-desc">Phục vụ xuyên suốt tất cả các ngày trong tuần, bao gồm cả các dịp Lễ &amp; Tết Nguyên Đán.</p>
                    </div>
                </section>

                <!-- Khung Câu hỏi thường gặp (FAQ) trọng tâm -->
                <section class="support-faq-section" id="faqSection">
                    <div class="support-faq-header">
                        <div>
                            <h2>CÂU HỎI THƯỜNG GẶP (FAQ)</h2>
                            <p>Giải đáp nhanh chóng những vấn đề phổ biến nhất của khán giả khi trải nghiệm dịch vụ tại CineFlow.</p>
                        </div>
                    </div>

                    <div class="support-faq-list" id="faqList">
                        <!-- FAQ 1 -->
                        <div class="faq-item" data-keywords="trừ tiền ngân hàng thanh toán lỗi vé vnpay mã vé mất tiền">                    <div class="faq-question">
                                <span>1. Tôi bị trừ tiền ngân hàng nhưng không thấy mã vé trong tài khoản?</span>
                                <span class="faq-chevron">▼</span>
                            </div>
                            <div class="faq-answer">
                                <p>Trong một số trường hợp cổng thanh toán (VNPay / Thẻ ngân hàng) bị gián đoạn đường truyền, hệ thống có thể mất từ 3 - 5 phút để xác thực giao dịch. Quý khách vui lòng kiểm tra tại mục <b>Tài khoản &gt; Lịch sử đặt vé</b>.</p>
                                <p>Nếu sau 10 phút vẫn chưa thấy vé được tạo, quý khách hãy gửi email ngay đến <b>chamsockhachhang@cineflow.vn</b> hoặc gọi <b>1900 6868</b> kèm ảnh chụp biên lai trừ tiền để được nhân viên kiểm tra kích hoạt vé hoặc hoàn tiền 100%.</p>
                            </div>
                        </div>

                        <!-- FAQ 2 -->
                        <div class="faq-item" data-keywords="hủy vé đổi vé đổi suất chiếu trả vé đổi ghế">
                            <div class="faq-question">
                                <span>2. Vé xem phim đã mua có được hủy hoặc đổi sang suất chiếu khác không?</span>
                                <span class="faq-chevron">▼</span>
                            </div>
                            <div class="faq-answer">
                                <p>Theo quy chuẩn vận hành hệ thống rạp chiếu phim, vé đã thanh toán thành công <b>không hỗ trợ tự hủy hoặc đổi vé trực tuyến</b> nhằm bảo đảm tính chính xác của sơ đồ ghế đã giữ cho toàn bộ khán giả.</p>
                                <p>Trường hợp bất khả kháng, quý khách vui lòng liên hệ hotline <b>1900 6868</b> hoặc gặp trực tiếp Quản lý tại cụm rạp trước giờ chiếu ít nhất <b>60 phút</b> để được xem xét hỗ trợ đổi ca chiếu phù hợp.</p>
                            </div>
                        </div>

                        <!-- FAQ 3 -->
                        <div class="faq-item" data-keywords="trẻ em học sinh sinh viên giá vé miễn phí chiều cao độ tuổi">
                            <div class="faq-question">
                                <span>3. Quy định về giá vé ưu đãi cho trẻ em, học sinh và sinh viên như thế nào?</span>
                                <span class="faq-chevron">▼</span>
                            </div>
                            <div class="faq-answer">
                                <p>• <b>Trẻ em dưới 0.7m</b>: Miễn phí vé khi vào rạp ngồi cùng ghế với người lớn đi kèm (áp dụng cho phim phân loại P và K).<br>
                                    • <b>Trẻ em từ 0.7m đến 1.3m</b>: Áp dụng mức giá vé ưu đãi giảm 20% so với giá vé người lớn.<br>
                                    • <b>Học sinh - Sinh viên (dưới 22 tuổi)</b>: Áp dụng giá vé đồng giá ưu đãi vào các ngày từ Thứ 2 đến Thứ 6 khi xuất trình thẻ học sinh/sinh viên còn hiệu lực tại quầy vé CineFlow.</p>
                            </div>
                        </div>

                        <!-- FAQ 4 -->
                        <div class="faq-item" data-keywords="voucher khuyến mãi mã giảm giá áp dụng coupon giảm tiền">
                            <div class="faq-question">
                                <span>4. Làm sao để sử dụng mã giảm giá (Voucher) khi đặt vé trực tuyến?</span>
                                <span class="faq-chevron">▼</span>
                            </div>
                            <div class="faq-answer">
                                <p>Tại bước 3 (Xác nhận đơn hàng &amp; Bắp nước) trên trang Đặt vé trực tuyến, quý khách hãy nhập mã vào ô <b>Mã giảm giá</b> và bấm nút <b>Áp dụng</b>.</p>
                                <p>Hệ thống CineFlow sẽ tự động khấu trừ số tiền giảm trực tiếp trên tổng giá vé của đơn hàng. Mỗi đơn hàng chỉ áp dụng tối đa 01 mã voucher và không quy đổi thành tiền mặt.</p>
                            </div>
                        </div>

                        <!-- FAQ 5 -->
                        <div class="faq-item" data-keywords="đồ ăn thức uống mang vào rạp thức ăn bên ngoài bắp nước quy định">
                            <div class="faq-question">
                                <span>5. Tôi có được mang thức ăn, đồ uống từ bên ngoài vào rạp chiếu phim không?</span>
                                <span class="faq-chevron">▼</span>
                            </div>
                            <div class="faq-answer">
                                <p>Để đảm bảo không gian sạch sẽ, vệ sinh an toàn thực phẩm và tránh mùi khó chịu ảnh hưởng tới khán giả xung quanh, CineFlow <b>không cho phép mang thức ăn, đồ uống có mùi từ bên ngoài</b> vào phòng chiếu.</p>
                                <p>Quý khách có thể mua các món bắp rang bơ giòn rụm (Vị Phô mai, Caramel, Truyền thống), nước ngọt giải khát và snack chính hãng tại quầy F&amp;B của CineFlow trước giờ vào xem phim.</p>
                            </div>
                        </div>

                        <!-- FAQ 6 -->
                        <div class="faq-item" data-keywords="hóa đơn đỏ vat hóa đơn giá trị gia tăng doanh nghiệp công ty">
                            <div class="faq-question">
                                <span>6. Tôi muốn xuất hóa đơn giá trị gia tăng (VAT) cho doanh nghiệp thì làm thế nào?</span>
                                <span class="faq-chevron">▼</span>
                            </div>
                            <div class="faq-answer">
                                <p>Quý khách vui lòng gửi thông tin xuất hóa đơn gồm: Mã đơn hàng, Tên công ty, Mã số thuế và Địa chỉ đăng ký kinh doanh về email <b>chamsockhachhang@cineflow.vn</b> trong vòng <b>48 giờ</b> kể từ khi thanh toán vé.</p>
                                <p>Hóa đơn điện tử hợp lệ theo quy định của Tổng cục Thuế sẽ được gửi tự động qua email của quý khách trong 1 - 2 ngày làm việc.</p>
                            </div>
                        </div>
                    </div>

                    <div class="faq-empty-state" id="faqEmptyState" style="display: none;">
                        <p>Không tìm thấy câu hỏi phù hợp với từ khóa của bạn. Vui lòng liên hệ hotline <b>1900 6868</b> hoặc email <b>chamsockhachhang@cineflow.vn</b> để được nhân viên hỗ trợ trực tiếp!</p>
                    </div>
                </section>

                <!-- Trụ sở và mạng lưới văn phòng đại diện -->
                <section class="office-locations-section">
                    <div class="support-section-header">
                        <h2>HỆ THỐNG VĂN PHÒNG ĐẠI DIỆN CINEFLOW</h2>
                        <p>Địa điểm tiếp nhận hồ sơ hợp tác thương mại, đối tác phát hành phim và xử lý khiếu nại trực tiếp.</p>
                    </div>

                    <div class="office-grid">
                        <div class="office-card">
                            <h3>🏢 Trụ sở chính (Hà Nội)</h3>
                            <p class="office-address">Tầng 6, Tòa nhà CineFlow Tower, Số 1 Phố Thái Hà, Quận Đống Đa, TP. Hà Nội.</p>
                            <div class="office-meta">
                                <div>Hotline: <a href="tel:02473006868">(024) 7300 6868</a></div>
                                <div>Email: <a href="mailto:hanoi@cineflow.vn">hanoi@cineflow.vn</a></div>
                                <div>Giờ làm việc: 08:30 - 17:30 (Thứ 2 - Thứ 6)</div>
                            </div>
                        </div>

                        <div class="office-card">
                            <h3>🏛 Chi nhánh miền Nam (TP. Hồ Chí Minh)</h3>
                            <p class="office-address">Tầng 8, Tòa nhà CineFlow Plaza, 126 Nguyễn Trãi, Phường 3, Quận 5, TP. Hồ Chí Minh.</p>
                            <div class="office-meta">
                                <div>Hotline: <a href="tel:02873006868">(028) 7300 6868</a></div>
                                <div>Email: <a href="mailto:hcm@cineflow.vn">hcm@cineflow.vn</a></div>
                                <div>Giờ làm việc: 08:30 - 17:30 (Thứ 2 - Thứ 6)</div>
                            </div>
                        </div>

                        <div class="office-card">
                            <h3>🌐 Kênh hỗ trợ trực tuyến &amp; Mạng xã hội</h3>
                            <p class="office-address">Theo dõi trang cộng đồng chính thức để nhận thông tin suất chiếu sớm và mã giảm giá độc quyền.</p>
                            <div class="office-meta">
                                <div>Fanpage: <a href="https://facebook.com" target="_blank" rel="noopener">fb.com/cineflowvietnam</a></div>
                                <div>Zalo OA: <a href="https://zalo.me" target="_blank" rel="noopener">CineFlow Cinema CSKH</a></div>
                                <div>TikTok: <a href="https://tiktok.com" target="_blank" rel="noopener">@cineflow.official</a></div>
                            </div>
                        </div>
                    </div>
                </section>

                <!-- Thẻ nhắc nhở hỗ trợ khẩn cấp tại rạp -->
                <section class="support-emergency-banner">
                    <div class="emergency-icon">🛎</div>
                    <div class="emergency-text">
                        <h3>Cần hỗ trợ gấp tại rạp chiếu phim?</h3>
                        <p>Nếu bạn đang có mặt tại rạp chiếu phim và cần đổi chỗ ngồi, xử lý sự cố máy in vé hoặc tìm đồ thất lạc, xin vui lòng liên hệ ngay với <b>Nhân viên Quầy vé</b> hoặc <b>Quản lý ca trực</b> tại rạp để được xử lý ngay lập tức.</p>
                    </div>
                    <a href="<%= contextPath %>/cinema" class="emergency-btn">Xem danh sách cụm rạp</a>
                </section>
            </main>

            <!-- Chân trang chung -->
            <jsp:include page="/common/footer.jsp"/>
        </div>

        <script>
            // 1. Xử lý đóng mở FAQ Accordion
            document.querySelectorAll('.faq-question').forEach(function (question) {
                question.addEventListener('click', function () {
                    var item = this.parentElement;
                    var isActive = item.classList.contains('active');

                    // Đóng tất cả các câu khác
                    document.querySelectorAll('.faq-item').forEach(function (el) {
                        el.classList.remove('active');
                    });

                    if (!isActive) {
                        item.classList.add('active');
                    }
                });
            });

            // 2. Tìm kiếm nhanh câu hỏi FAQ theo từ khóa
            var searchInput = document.getElementById('faqSearchInput');
            if (searchInput) {
                searchInput.addEventListener('input', function () {
                    var keyword = this.value.toLowerCase().trim();
                    var items = document.querySelectorAll('.faq-item');
                    var matchedCount = 0;

                    items.forEach(function (item) {
                        var text = (item.textContent || '').toLowerCase();
                        var keywords = (item.getAttribute('data-keywords') || '').toLowerCase();
                        if (!keyword || text.includes(keyword) || keywords.includes(keyword)) {
                            item.style.display = '';
                            matchedCount++;
                        } else {
                            item.style.display = 'none';
                            item.classList.remove('active');
                        }
                    });

                    var emptyState = document.getElementById('faqEmptyState');
                    if (emptyState) {
                        emptyState.style.display = matchedCount === 0 ? 'block' : 'none';
                    }
                });
            }
        </script>
    </body>
</html>
