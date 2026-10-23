# 🏛️ CINEFLOW ARCHITECTURAL & NAMING CONVENTION SPECIFICATION
> **Tiêu chuẩn:** Oracle Code Conventions for Java, Jakarta EE Web Standards & Baeldung Architecture Guidelines  
> **Dự án:** CineFlow - Hệ thống Quản trị & Bán vé Cụm rạp Chiếu phim  
> **Áp dụng cho:** Toàn bộ thành viên đội ngũ phát triển (Thái, Hải, Hoàng, Việt, Huy)  
> **Trạng thái:** Bản đề xuất chính thức trình duyệt đội ngũ kỹ thuật  

---

## 1. CƠ SỞ TIÊU CHUẨN QUỐC TẾ & LÝ DO CHUẨN HÓA

### 1.1. Vì sao TUYỆT ĐỐI KHÔNG dùng dấu gạch ngang (`-`) trong Java & JSP?
1. **Vi phạm cú pháp định danh Java (JLS §6.1):**
   - Trong ngôn ngữ Java, dấu `-` là toán tử trừ (`minus operator`). Nó **không phải** là ký tự hợp lệ cho định danh (`Identifier`).
2. **Cơ chế biên dịch JSP của Tomcat Jasper Engine:**
   - JSP không phải HTML tĩnh. Khi chạy, Tomcat (Jasper) sẽ biên dịch từng file `.jsp` thành một Servlet Java (`.java`) rồi dịch tiếp ra bytecode (`.class`).
   - Nếu đặt tên file có dấu gạch ngang (ví dụ: `select-seats.jsp`, `seat-config.jsp`), Jasper **bắt buộc phải mã hóa escape dấu `-` sang mã Unicode hex `_002d`**:
     - `select-seats.jsp` &rarr; `select_002dseats_jsp.java` / `select_002dseats_jsp.class`
     - `movie-management.jsp` &rarr; `movie_002dmanagement_jsp.java`
   - Điều này làm sinh ra các lớp Java dị dạng, làm vỡ quy chuẩn Stack Trace, gây khó khăn khi debug ngoại lệ, profiling, logging và reflection.
3. **Quy ước quốc tế của Oracle Java BluePrints & Baeldung:**
   - Các tài liệu chuẩn của Sun Microsystems / Oracle (như Java Pet Store, Jakarta EE Guides) và Baeldung quy định rõ: Tên file JSP trong Java Web phải tuân theo quy ước Java: dùng **`lowerCamelCase`** (hoặc `PascalCase`) để tương thích tự nhiên với Java bytecode và Servlet container.

### 1.2. Vì sao CẤM tên file cộc lốc, mơ hồ (`list.jsp`, `edit.jsp`, `form.jsp`)?
- **Vấn đề thực tế khi làm việc nhóm:** Khi lập trình trên IDE (IntelliJ, Eclipse, NetBeans, VS Code), các lập trình viên thường mở nhiều tab cùng lúc. Nếu mỗi thư mục đều có 1 file `list.jsp`, `edit.jsp`, `form.jsp`, trên thanh tiêu đề IDE sẽ xuất hiện hàng loạt tab cùng tên: `list.jsp | list.jsp | list.jsp | form.jsp | form.jsp`. Lập trình viên không thể phân biệt được đâu là danh sách phim, danh sách khách hàng, hay danh sách món ăn, dẫn đến việc sửa nhầm file, merge conflict và tốn thời gian debug.
- **Tiêu chuẩn bắt buộc:** Mọi tên file phải tự thân mang đầy đủ ý nghĩa (Self-descriptive) theo công thức:  
  $$\textbf{[Domain / Entity]} + \textbf{[Action / ViewType]} + \textbf{.jsp}$$
  - *Thay vì:* `fnb/list.jsp` &rarr; *Chuẩn hóa thành:* `fnb/fnbItemList.jsp`
  - *Thay vì:* `voucher/list.jsp` &rarr; *Chuẩn hóa thành:* `voucher/voucherList.jsp`
  - *Thay vì:* `showtime/edit.jsp` &rarr; *Chuẩn hóa thành:* `showtime/showtimeEdit.jsp`
  - *Thay vì:* `movie/details.jsp` &rarr; *Chuẩn hóa thành:* `movie/movieDetail.jsp`
  - *Thay vì:* `customer/list.jsp` &rarr; *Chuẩn hóa thành:* `customer/customerList.jsp`

---

## 2. BẢNG QUY CHUẨN ĐẶT TÊN CHI TIẾT (NAMING CONVENTIONS)

| Thành phần | Quy ước Case | Cú pháp đặt tên | Ví dụ chuẩn | Ví dụ sai (Cấm dùng) |
| :--- | :--- | :--- | :--- | :--- |
| **Package** | `all-lowercase` | `[layer].[module]` (chỉ gồm chữ cái viết thường, không `-`, không `_`) | `controller.movie`<br>`controller.pricing`<br>`model.entity`<br>`model.dto.customer` | ❌ `controller.showtime-pricing`<br>❌ `controller.ShowTime`<br>❌ `model.DTO` |
| **Class Controller** | `PascalCase` | `[Feature/Entity]Servlet` | `MovieManagementServlet`<br>`ShowtimeListServlet`<br>`BookingServlet`<br>`LoginServlet` | ❌ `MovieController` (không đồng nhất)<br>❌ `showtime_servlet` |
| **Class Service** | `PascalCase` | `[Domain]Service` | `BookingService`<br>`PricingService`<br>`PaymentService`<br>`ShowtimeService` | ❌ `BookingServiceImpl` (khi không có interface)<br>❌ `bookingService` |
| **Class DAO** | `PascalCase` | `[Entity]DAO` | `UserDAO`<br>`MovieDAO`<br>`ShowtimeDAO`<br>`BookingDAO` | ❌ `UserDao` (chữ o thường)<br>❌ `movie_dao` |
| **Class Entity** | `PascalCase` | **Danh từ số ít**, khớp 1-1 bảng CSDL | `User`<br>`Movie`<br>`Showtime`<br>`Centre`<br>`Ticket` | ❌ `Users` (số nhiều)<br>❌ `TblMovie`<br>❌ `MovieEntity` |
| **Class DTO** | `PascalCase` | `[Domain/Use]DTO` | `CustomerManagementDTO`<br>`ShowtimeDetailDTO`<br>`SalesSummaryDTO`<br>`CheckInResultDTO` | ❌ `CustomerRow` (thiếu DTO)<br>❌ `customer_dto` |
| **Class Filter** | `PascalCase` | `[Purpose]Filter` | `AuthFilter`<br>`AdminFilter`<br>`ManagerFilter`<br>`EncodingFilter` | ❌ `CheckAuth` |
| **Class Util** | `PascalCase` | `[Domain]Util` | `PasswordUtil`<br>`EmailUtil`<br>`ValidationUtil`<br>`VNPayUtil` | ❌ `Utils`<br>❌ `helper` |
| **Method (Hàm)** | `camelCase` | Bắt đầu bằng **Động từ rõ nghĩa**: `get...`, `find...`, `exists...`, `calculate...` | `findByEmail()`<br>`findActiveMovies()`<br>`calculateTotalPrice()`<br>`updateStatus()` | ❌ `Email()`<br>❌ `GetData()`<br>❌ `do_process()` |
| **Constants (Hằng số)** | `UPPER_SNAKE_CASE` | Chữ in hoa nối bằng dấu gạch dưới `_` | `MAX_SEATS_PER_ROW`<br>`DEFAULT_PAGE_SIZE`<br>`BOOKING_TIMEOUT_MINUTES` | ❌ `maxSeats`<br>❌ `timeout_minute` |
| **View Page (JSP)** | `lowerCamelCase` | `[entity][ViewAction].jsp` | `movieList.jsp`<br>`movieDetail.jsp`<br>`movieForm.jsp`<br>`customerList.jsp`<br>`showtimeList.jsp`<br>`showtimeEdit.jsp`<br>`seatConfig.jsp`<br>`selectSeats.jsp` | ❌ `list.jsp`<br>❌ `edit.jsp`<br>❌ `select-seats.jsp`<br>❌ `seat-config.jsp` |
| **View Fragment (JSPF)** | `lowerCamelCase` | `[name].jspf` | `adminSidebar.jspf`<br>`adminTopbar.jspf`<br>`header.jspf`<br>`footer.jspf`<br>`pagination.jspf` | ❌ `admin-sidebar.jsp`<br>❌ `foot.jspf` |

---

## 3. CẤU TRÚC THƯ MỤC JAVA CHUẨN HÓA (`src/main/java/`)

```
src/main/java/
├── controller/                       # TẦNG CONTROLLER: Tiếp nhận & điều hướng HTTP
│   ├── admin/                        # Nghiệp vụ Quản trị cấp cao (Admin)
│   │   ├── AdminDashboardServlet.java
│   │   ├── CentreManagementServlet.java
│   │   ├── ManagerManagementServlet.java
│   │   ├── DistributionServlet.java
│   │   └── StaffTransferServlet.java
│   ├── manager/                      # Nghiệp vụ Quản lý rạp (Cinema Manager)
│   │   ├── ManagerDashboardServlet.java
│   │   ├── HallManagementServlet.java
│   │   ├── SeatConfigurationServlet.java
│   │   └── StaffManagementServlet.java
│   ├── auth/                         # Xác thực & Tài khoản
│   │   ├── LoginServlet.java
│   │   ├── LogoutServlet.java
│   │   ├── RegisterServlet.java
│   │   ├── ForgotPasswordServlet.java
│   │   ├── ResetPasswordServlet.java
│   │   ├── VerifyOtpServlet.java
│   │   ├── ChangePasswordServlet.java
│   │   └── AccountProfileServlet.java
│   ├── customer/                     # Quản lý khách hàng
│   │   └── CustomerManagementServlet.java
│   ├── movie/                        # Phim & Đánh giá
│   │   ├── MovieManagementServlet.java
│   │   ├── MovieDetailsServlet.java
│   │   └── MovieRatingServlet.java
│   ├── showtime/                     # Lịch chiếu
│   │   ├── ShowtimeListServlet.java
│   │   ├── CreateShowtimeServlet.java
│   │   ├── EditShowtimeServlet.java
│   │   ├── UpdateShowtimeServlet.java
│   │   ├── CancelShowtimeServlet.java
│   │   └── ViewShowtimeSeatsServlet.java
│   ├── pricing/                      # Bảng giá & Phụ thu (Chính sách tiền tệ, tách biệt với Ghế vật lý)
│   │   ├── CentrePricingServlet.java     # Quản lý giá vé cơ bản của từng chi nhánh
│   │   ├── TimeSlotPricingServlet.java   # Quản lý bảng giá theo khung giờ (Sáng / Tối / Cuối tuần)
│   │   └── SurchargePricingServlet.java  # Quản lý chính sách phụ thu tiền hạng ghế VIP/Sweetbox
│   │   # (LƯU Ý: Sơ đồ ghế vật lý nằm ở manager/SeatConfigurationServlet, không nhét vào pricing)
│   ├── booking/                      # Đặt vé Online (Khách hàng)
│   │   ├── BookingServlet.java
│   │   ├── BookingCheckoutServlet.java
│   │   └── BookingHistoryServlet.java
│   ├── counter/                      # Bán vé tại quầy (Nhân viên POS)
│   │   ├── CounterShowtimeServlet.java
│   │   ├── CounterCheckoutServlet.java
│   │   └── CounterTicketServlet.java
│   ├── fnb/                          # Bắp nước & Combo
│   │   ├── FnbManagementServlet.java
│   │   ├── FnbStockServlet.java
│   │   └── FnbComboServlet.java
│   ├── voucher/                      # Khuyến mãi & Giảm giá
│   │   ├── VoucherManagementServlet.java
│   │   └── VoucherFormServlet.java
│   ├── checkin/                      # Soát vé điện tử (QR Code)
│   │   └── TicketCheckInServlet.java
│   ├── payment/                      # Cổng thanh toán & Lịch sử
│   │   ├── PaymentServlet.java
│   │   └── PaymentCallbackServlet.java
│   └── report/                       # Báo cáo doanh thu & Vận hành
│       ├── ReportHubServlet.java
│       ├── SalesReportServlet.java
│       └── OperationsReportServlet.java
│
├── service/                          # TẦNG SERVICE: Logic nghiệp vụ, Tính toán & Transaction
│   ├── BookingService.java           # Chống race condition giữ ghế, transaction đặt vé
│   ├── PaymentService.java           # Hash VNPay, xác nhận giao dịch, sinh vé
│   ├── PricingService.java           # Tính tổng tiền vé (Base + TimeSlot + Surcharge)
│   ├── ShowtimeService.java          # Kiểm tra trùng lịch phòng chiếu, dọn dẹp buffer
│   ├── UserService.java              # Mã hóa mật khẩu, kiểm tra tài khoản, gửi OTP
│   └── VoucherService.java           # Kiểm tra điều kiện áp dụng voucher, tính chiết khấu
│
├── dao/                              # TẦNG DAO: Chỉ thuần túy thực thi SQL PreparedStatement
│   ├── UserDAO.java
│   ├── CentreDAO.java
│   ├── HallDAO.java
│   ├── SeatDAO.java
│   ├── MovieDAO.java
│   ├── ShowtimeDAO.java
│   ├── CentrePricingDAO.java
│   ├── TimeSlotPricingDAO.java
│   ├── SurchargeDAO.java
│   ├── BookingDAO.java
│   ├── PaymentDAO.java
│   ├── FnbDAO.java
│   ├── VoucherDAO.java
│   ├── CheckInDAO.java
│   ├── SalesReportDAO.java
│   └── OperationsReportDAO.java
│
├── model/                            # TẦNG DỮ LIỆU: Phân tách rạch ròi Entity & DTO
│   ├── entity/                       # Khớp 1-1 CHÍNH XÁC với các cột vật lý trong SQL Server
│   │   ├── User.java                 # Bảng Users
│   │   ├── Centre.java               # Bảng Centres
│   │   ├── Hall.java                 # Bảng Halls
│   │   ├── Seat.java                 # Bảng Seats
│   │   ├── Movie.java                # Bảng Movies
│   │   ├── Showtime.java             # Bảng Showtimes
│   │   ├── CentrePricing.java        # Bảng CentrePricing
│   │   ├── TimeSlotPricing.java      # Bảng TimeSlotPricing
│   │   ├── Surcharge.java            # Bảng Surcharges
│   │   ├── Booking.java              # Bảng Bookings
│   │   ├── Payment.java              # Bảng Payments
│   │   ├── Ticket.java               # Bảng Tickets
│   │   ├── FnbItem.java              # Bảng FnbItems
│   │   ├── FnbCombo.java             # Bảng FnbCombos
│   │   ├── Voucher.java              # Bảng Vouchers
│   │   ├── ManagerAssignment.java    # Bảng ManagerAssignments
│   │   └── StaffTransfer.java        # Bảng StaffTransfers
│   │
│   └── dto/                          # DTO: Gom dữ liệu JOIN nhiều bảng hiển thị màn hình
│       ├── customer/
│       │   └── CustomerManagementDTO.java # User + bookingCount + totalSpent + rank
│       ├── centre/
│       │   └── CentreManagementDTO.java   # Centre + managerName + hallCount + totalSeats
│       ├── movie/
│       │   └── MovieDistributionDTO.java  # Movie + distributedCentres + totalCentres
│       ├── showtime/
│       │   ├── ShowtimeDetailDTO.java     # Showtime + movieTitle + hallName + centreName
│       │   └── ShowtimeSeatMatrixDTO.java # Ma trận ghế hiển thị màu sắc trạng thái
│       ├── booking/
│       │   ├── BookingDetailDTO.java      # Chi tiết đơn: Ghế + F&B + Voucher + Tiền
│       │   └── BookingHistoryDTO.java     # Lịch sử đơn hàng của khách hàng
│       ├── pricing/
│       │   └── SeatConfigDTO.java         # Cấu hình phụ thu loại ghế
│       ├── fnb/
│       │   ├── FnbRowDTO.java             # Dòng hiển thị F&B
│       │   ├── FnbSaleDTO.java            # Thống kê bán F&B
│       │   └── StockRowDTO.java           # Số lượng tồn kho nguyên vật liệu
│       ├── checkin/
│       │   ├── CheckInResultDTO.java      # Kết quả quét vé (Hợp lệ / Đã dùng / Sai giờ)
│       │   └── CheckInLogDTO.java         # Nhật ký quét vé của nhân viên
│       └── report/
│           ├── SalesSummaryDTO.java       # Tổng hợp doanh thu theo ngày/tháng
│           ├── SalesRowDTO.java           # Chi tiết từng dòng giao dịch
│           └── OccupancyRowDTO.java       # Tỷ lệ lấp đầy phòng chiếu
│
├── filter/                           # TẦNG FILTER: Bộ lọc middleware
│   ├── AuthFilter.java               # Kiểm tra đăng nhập
│   ├── AdminFilter.java              # Kiểm tra quyền ADMIN (/admin/*)
│   ├── ManagerFilter.java            # Kiểm tra quyền MANAGER (/manager/*)
│   ├── StaffFilter.java              # Kiểm tra quyền STAFF (/counter/*, /checkin/*)
│   ├── EncodingFilter.java           # Ép UTF-8 cho toàn bộ Request & Response
│   └── DBConnectionFilter.java       # Tự động đóng/kiểm tra dọn dẹp kết nối CSDL
│
└── utils/                            # TIỆN ÍCH DÙNG CHUNG
    ├── DBContext.java                # Quản trị kết nối JDBC duy nhất tới CineFlowDB
    ├── PasswordUtil.java             # Mã hóa và kiểm tra băm mật khẩu (BCrypt)
    ├── EmailUtil.java                # Gửi email thông báo vé, mã OTP
    ├── ValidationUtil.java           # Kiểm tra định dạng Regex (email, phone, pass)
    └── VNPayUtil.java                # Tạo URL thanh toán và băm chữ ký HMAC-SHA512
```

---

## 4. CẤU TRÚC GIAO DIỆN CHUẨN HÓA (`src/main/webapp/`)

Toàn bộ file JSP nội bộ **bắt buộc đưa vào trong `WEB-INF/views/`** để bảo mật (tránh người dùng gõ trực tiếp URL vào file JSP). Tên file tuân thủ nghiêm ngặt chuẩn **`lowerCamelCase`**, không dấu gạch ngang, mang đầy đủ tên đối tượng:

```
src/main/webapp/
├── css/                              # CSS tĩnh phân chia theo từng module
│   ├── common/                       # variables.css, layout.css, buttons.css, tables.css
│   ├── auth/                         # auth.css
│   ├── admin/                        # adminDashboard.css, centreManagement.css
│   ├── manager/                      # managerDashboard.css, hallManagement.css
│   ├── showtime/                     # showtimeList.css, seatMatrix.css
│   ├── pricing/                      # pricingManagement.css
│   ├── booking/                      # bookingFlow.css
│   ├── counter/                      # counterPos.css
│   └── report/                       # reportDashboard.css
│
├── js/                               # JavaScript tĩnh phân chia theo module
│   ├── common/                       # global.js, notification.js
│   ├── admin/                        # adminPagination.js, centreModal.js
│   ├── showtime/                     # showtimeMatrix.js
│   ├── pricing/                      # pricingCalculator.js
│   ├── booking/                      # seatSelection.js
│   └── checkin/                      # qrScanner.js
│
├── uploads/                          # Thư mục lưu media upload từ admin
│   ├── posters/                      # Poster phim
│   ├── avatars/                      # Ảnh đại diện người dùng
│   └── trailers/                     # Video trailer phim
│
└── WEB-INF/
    ├── web.xml                       # Cấu hình servlet-mapping, filter, error-page
    │
    ├── fragments/                    # Các đoạn giao diện tái sử dụng (.jspf)
    │   ├── header.jspf               # Header chung người dùng
    │   ├── footer.jspf               # Footer chung người dùng
    │   ├── adminSidebar.jspf         # Menu điều hướng bên trái Admin
    │   ├── adminTopbar.jspf          # Thanh công cụ trên cùng Admin
    │   ├── managerSidebar.jspf       # Menu điều hướng Quản lý rạp
    │   ├── managerTopbar.jspf        # Thanh công cụ Quản lý rạp
    │   └── pagination.jspf           # Phân trang dùng chung cho mọi bảng
    │
    └── views/                        # TẤT CẢ FILE JSP VIEW ĐẶT TẠI ĐÂY (lowerCamelCase)
        ├── home/
        │   └── homeIndex.jsp         # Trang chủ giới thiệu phim & rạp
        │
        ├── auth/
        │   ├── authLogin.jsp         # Đăng nhập
        │   ├── authRegister.jsp      # Đăng ký tài khoản
        │   ├── forgotPassword.jsp    # Yêu cầu quên mật khẩu
        │   ├── resetPassword.jsp     # Nhập mật khẩu mới
        │   ├── verifyOtp.jsp         # Nhập mã OTP xác thực
        │   ├── changePassword.jsp    # Đổi mật khẩu
        │   └── userProfile.jsp       # Thông tin cá nhân
        │
        ├── admin/
        │   ├── adminDashboard.jsp    # Tổng quan số liệu toàn hệ thống
        │   ├── centreList.jsp        # Quản lý danh sách chi nhánh cụm rạp
        │   ├── managerList.jsp       # Quản lý danh sách Quản lý rạp
        │   ├── staffTransfer.jsp     # Điều chuyển nhân sự giữa các rạp
        │   └── adminReport.jsp       # Báo cáo tổng hợp Admin
        │
        ├── manager/
        │   ├── managerDashboard.jsp  # Bảng điều khiển Quản lý chi nhánh
        │   ├── hallList.jsp          # Danh sách phòng chiếu của rạp
        │   ├── seatConfig.jsp        # Sơ đồ và cấu hình loại ghế phòng chiếu
        │   └── staffList.jsp         # Danh sách nhân viên trực thuộc rạp
        │
        ├── customer/
        │   ├── customerList.jsp      # Danh sách khách hàng (bảng có doanh thu, số vé)
        │   └── customerDetail.jsp    # Chi tiết lịch sử giao dịch khách hàng
        │
        ├── movie/
        │   ├── movieList.jsp         # Danh sách phim trong rạp (Admin/Manager)
        │   ├── movieForm.jsp         # Form thêm mới / Sửa thông tin phim
        │   ├── movieDetail.jsp       # Trang chi tiết phim (Trailer, Diễn viên, Đánh giá)
        │   └── movieDistribution.jsp # Phân bổ phim về các cụm rạp
        │
        ├── showtime/
        │   ├── showtimeList.jsp      # Bảng danh sách suất chiếu
        │   ├── showtimeCreate.jsp    # Màn hình tạo lịch chiếu mới
        │   ├── showtimeEdit.jsp      # Màn hình chỉnh sửa lịch chiếu
        │   └── showtimeSeatMatrix.jsp# Màn hình xem sơ đồ ghế của suất chiếu
        │
        ├── pricing/
        │   ├── pricingOverview.jsp   # Tổng quan quản trị giá vé
        │   ├── timeSlotPricing.jsp   # Bảng cấu hình giá theo khung giờ
        │   ├── timeSlotEdit.jsp      # Chỉnh sửa giá khung giờ
        │   ├── seatSurcharge.jsp     # Bảng cấu hình phụ thu ghế (VIP, Sweetbox)
        │   └── seatSurchargeEdit.jsp # Chỉnh sửa phụ thu ghế
        │
        ├── booking/
        │   ├── selectShowtime.jsp    # Khách hàng chọn ngày và suất chiếu
        │   ├── selectSeats.jsp       # Khách hàng chọn ghế ngồi trong phòng
        │   ├── bookingCheckout.jsp   # Rà soát đơn hàng vé + F&B, áp voucher
        │   └── bookingHistory.jsp    # Lịch sử đơn hàng vé của tài khoản
        │
        ├── counter/
        │   ├── counterShowtime.jsp   # Nhân viên chọn suất chiếu tại quầy
        │   ├── counterCheckout.jsp   # Nhân viên chọn ghế, F&B và thu tiền mặt
        │   └── counterTicketPrint.jsp# Màn hình xem và in vé giấy tại quầy
        │
        ├── fnb/
        │   ├── fnbItemList.jsp       # Danh sách mặt hàng F&B
        │   ├── fnbItemForm.jsp       # Form thêm / sửa mặt hàng F&B
        │   ├── fnbComboList.jsp      # Danh sách Combo bắp nước
        │   ├── fnbComboForm.jsp      # Form thêm / sửa Combo
        │   └── fnbInventory.jsp      # Quản lý kho hàng & số lượng tồn
        │
        ├── voucher/
        │   ├── voucherList.jsp       # Danh sách mã giảm giá khuyến mãi
        │   └── voucherForm.jsp       # Form tạo mới / sửa voucher
        │
        ├── checkin/
        │   ├── ticketCheckIn.jsp     # Màn hình quét QR soát vé
        │   └── checkInHistory.jsp    # Lịch sử quét vé trong ca trực
        │
        ├── report/
        │   ├── reportChain.jsp       # Báo cáo tổng chuỗi rạp
        │   ├── reportSales.jsp       # Báo cáo doanh thu chi tiết
        │   └── reportOperations.jsp  # Báo cáo vận hành & tỷ lệ lấp đầy
        │
        └── payment/
            ├── paymentSuccess.jsp    # Thông báo thanh toán thành công
            ├── paymentFailed.jsp     # Thông báo thanh toán thất bại
            └── paymentHistory.jsp    # Lịch sử đối soát giao dịch VNPay
```

---

## 5. BẢNG ĐỐI CHIẾU CHUYỂN ĐỔI CHI TIẾT (MIGRATION MATRIX)

| Tên file hiện tại (Cũ / Sai chuẩn) | Vấn đề vi phạm | Tên file mới (Chuẩn hóa) | Đường dẫn mới |
| :--- | :--- | :--- | :--- |
| `admin/customers.jsp` | Nằm ngoài `WEB-INF`, tên số nhiều | `customerList.jsp` | `WEB-INF/views/customer/customerList.jsp` |
| `admin/centres.jsp` | Nằm ngoài `WEB-INF`, tên số nhiều | `centreList.jsp` | `WEB-INF/views/admin/centreList.jsp` |
| `admin/managers.jsp` | Nằm ngoài `WEB-INF`, tên số nhiều | `managerList.jsp` | `WEB-INF/views/admin/managerList.jsp` |
| `admin/movie-management.jsp` | Chứa dấu gạch ngang `-`, nằm ngoài `WEB-INF` | `movieList.jsp` | `WEB-INF/views/movie/movieList.jsp` |
| `admin/transfers.jsp` | Nằm ngoài `WEB-INF`, tên chưa rõ | `staffTransfer.jsp` | `WEB-INF/views/admin/staffTransfer.jsp` |
| `manager/halls.jsp` | Nằm ngoài `WEB-INF`, tên số nhiều | `hallList.jsp` | `WEB-INF/views/manager/hallList.jsp` |
| `manager/seat-config.jsp` | Chứa dấu gạch ngang `-`, nằm ngoài `WEB-INF` | `seatConfig.jsp` | `WEB-INF/views/manager/seatConfig.jsp` |
| `showtime/showtime-list.jsp` | Chứa dấu gạch ngang `-`, nằm ngoài `WEB-INF` | `showtimeList.jsp` | `WEB-INF/views/showtime/showtimeList.jsp` |
| `showtime/edit-showtime.jsp` | Chứa dấu gạch ngang `-`, nằm ngoài `WEB-INF` | `showtimeEdit.jsp` | `WEB-INF/views/showtime/showtimeEdit.jsp` |
| `showtime/showtime-seats.jsp` | Chứa dấu gạch ngang `-`, nằm ngoài `WEB-INF` | `showtimeSeatMatrix.jsp` | `WEB-INF/views/showtime/showtimeSeatMatrix.jsp` |
| `pricing/pricing-management.jsp`| Chứa dấu gạch ngang `-`, nằm ngoài `WEB-INF` | `pricingOverview.jsp` | `WEB-INF/views/pricing/pricingOverview.jsp` |
| `pricing/edit-seat-surcharge.jsp`| Chứa dấu gạch ngang `-`, nằm ngoài `WEB-INF` | `seatSurchargeEdit.jsp` | `WEB-INF/views/pricing/seatSurchargeEdit.jsp` |
| `pricing/time-slot-pricing.jsp` | Chứa dấu gạch ngang `-`, nằm ngoài `WEB-INF` | `timeSlotPricing.jsp` | `WEB-INF/views/pricing/timeSlotPricing.jsp` |
| `fnb/list.jsp` | Tên mơ hồ, không rõ list cái gì | `fnbItemList.jsp` | `WEB-INF/views/fnb/fnbItemList.jsp` |
| `fnb/item-form.jsp` | Chứa dấu gạch ngang `-` | `fnbItemForm.jsp` | `WEB-INF/views/fnb/fnbItemForm.jsp` |
| `fnb/combo-form.jsp` | Chứa dấu gạch ngang `-` | `fnbComboForm.jsp` | `WEB-INF/views/fnb/fnbComboForm.jsp` |
| `fnb/stock.jsp` | Tên cộc lốc | `fnbInventory.jsp` | `WEB-INF/views/fnb/fnbInventory.jsp` |
| `voucher/list.jsp` | Tên mơ hồ, không rõ list cái gì | `voucherList.jsp` | `WEB-INF/views/voucher/voucherList.jsp` |
| `voucher/form.jsp` | Tên mơ hồ, không rõ form cái gì | `voucherForm.jsp` | `WEB-INF/views/voucher/voucherForm.jsp` |
| `checkin/index.jsp` | Tên `index` cộc lốc, không mang nghĩa nghiệp vụ | `ticketCheckIn.jsp` | `WEB-INF/views/checkin/ticketCheckIn.jsp` |
| `booking/select-seats.jsp` | Chứa dấu gạch ngang `-` | `selectSeats.jsp` | `WEB-INF/views/booking/selectSeats.jsp` |
| `booking/select-showtime.jsp` | Chứa dấu gạch ngang `-` | `selectShowtime.jsp` | `WEB-INF/views/booking/selectShowtime.jsp` |
| `auth/change-password.jsp` | Chứa dấu gạch ngang `-`, nằm ngoài `WEB-INF` | `changePassword.jsp` | `WEB-INF/views/auth/changePassword.jsp` |
| `auth/forgot-password.jsp` | Chứa dấu gạch ngang `-`, nằm ngoài `WEB-INF` | `forgotPassword.jsp` | `WEB-INF/views/auth/forgotPassword.jsp` |
| `auth/reset-password.jsp` | Chứa dấu gạch ngang `-`, nằm ngoài `WEB-INF` | `resetPassword.jsp` | `WEB-INF/views/auth/resetPassword.jsp` |
| `auth/verify-otp.jsp` | Chứa dấu gạch ngang `-`, nằm ngoài `WEB-INF` | `verifyOtp.jsp` | `WEB-INF/views/auth/verifyOtp.jsp` |

---

## 6. NGUYÊN TẮC BẢO MẬT VÀ ĐIỀU HƯỚNG VIEW (REQUEST DISPATCHER)

Khi toàn bộ file JSP đã được đưa an toàn vào trong thư mục `WEB-INF/views/`, người dùng trên trình duyệt **không thể truy cập trực tiếp bằng URL** (ví dụ: gõ `http://localhost:8080/CineFlow/WEB-INF/views/movie/movieList.jsp` sẽ bị máy chủ trả về lỗi `404 Not Found`).

Mọi luồng hiển thị bắt buộc phải đi qua Controller (Servlet) và chuyển tiếp bằng `RequestDispatcher.forward()`:

```java
@WebServlet(name = "MovieListServlet", urlPatterns = {"/admin/movies", "/manager/movies"})
public class MovieListServlet extends HttpServlet {
    private final MovieDAO movieDAO = new MovieDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // 1. Gọi DAO / Service lấy dữ liệu
        List<Movie> movieList = movieDAO.findAllActive();
        
        // 2. Gán dữ liệu vào Request Scope
        request.setAttribute("movieList", movieList);
        
        // 3. Chuyển tiếp tới JSP nằm an toàn trong WEB-INF/views
        request.getRequestDispatcher("/WEB-INF/views/movie/movieList.jsp").forward(request, response);
    }
}
```

---

## 7. NGUYÊN TẮC TRANSACTION BẮT BUỘC TẠI TẦNG SERVICE

Mọi nghiệp vụ liên quan đến tiền bạc, thanh toán, giữ ghế, hoặc ghi nhận nhiều bảng cùng lúc **bắt buộc phải đặt Transaction tại tầng Service**, tuyệt đối không để rải rác trong từng DAO riêng lẻ.

```java
package service;

import dao.BookingDAO;
import dao.PaymentDAO;
import dao.SeatDAO;
import dao.TicketDAO;
import model.dto.payment.PaymentRowDTO;
import utils.DBContext;

import java.sql.Connection;
import java.sql.SQLException;

public class PaymentService {
    private final BookingDAO bookingDAO = new BookingDAO();
    private final SeatDAO seatDAO = new SeatDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final TicketDAO ticketDAO = new TicketDAO();

    public boolean processSuccessfulPayment(int bookingId, PaymentRowDTO paymentDTO) throws SQLException {
        // Quản lý Transaction tập trung tại Service bằng Connection duy nhất
        try (Connection conn = DBContext.getConnection()) {
            conn.setAutoCommit(false); // Bắt đầu Transaction
            try {
                // Bước 1: Đổi trạng thái Booking sang PAID
                bookingDAO.updateBookingStatus(conn, bookingId, "PAID");

                // Bước 2: Chốt trạng thái Ghế đã chọn sang BOOKED
                seatDAO.confirmSeatsBooked(conn, bookingId);

                // Bước 3: Ghi nhận lịch sử giao dịch vào bảng Payments
                paymentDAO.recordTransaction(conn, paymentDTO);

                // Bước 4: Tạo vé điện tử chính thức vào bảng Tickets
                ticketDAO.generateTickets(conn, bookingId);

                conn.commit(); // Tất cả các bước thành công -> Commit vĩnh viễn
                return true;
            } catch (Exception ex) {
                conn.rollback(); // Bất kỳ bước nào lỗi -> Rollback 100% dữ liệu
                throw new SQLException("Thanh toán thất bại, toàn bộ dữ liệu đã được hoàn tác an toàn.", ex);
            }
        }
    }
}
```

---

## 8. QUY TRÌNH KIỂM SOÁT VÀ ĐỒNG BỘ HÓA `web.xml` (CHỐNG LỖI `ClassNotFoundException`)

### 8.1. Rủi ro thực tế: 22 Servlet đang khai báo cứng FQCN trong `web.xml`
Hiện tại file `src/main/webapp/WEB-INF/web.xml` đang chứa **chính xác 22 khai báo `<servlet-class>` cố định** theo dạng:
```xml
<servlet>
    <servlet-name>LoginServlet</servlet-name>
    <servlet-class>controller.LoginServlet</servlet-class>
</servlet>
```
Nếu ta di chuyển class sang các sub-package mới (`controller.auth.LoginServlet`, `controller.admin.CentreManagementServlet`...) mà **quên cập nhật `web.xml`**, Tomcat khi khởi động sẽ cố tải lớp `controller.LoginServlet`, lập tức ném ra ngoại lệ:
$$\textbf{java.lang.ClassNotFoundException: controller.LoginServlet}$$
và ứng dụng sẽ **sập hoàn toàn ngay từ bước deploy, không thể khởi động được**.

### 8.2. Bảng ánh xạ 22 Servlet trong `web.xml` sang Package mới

| STT | Tên Servlet | Khai báo cũ trong `web.xml` | Khai báo chuẩn hóa mới | Ghi chú |
| :---: | :--- | :--- | :--- | :--- |
| 1 | `DashboardServlet` | `controller.DashboardServlet` | `controller.admin.AdminDashboardServlet` | Quản trị Admin |
| 2 | `MovieDetailsServlet` | `controller.MovieDetailsServlet` | `controller.movie.MovieDetailsServlet` | Chi tiết phim |
| 3 | `MovieRatingServlet` | `controller.MovieRatingServlet` | `controller.movie.MovieRatingServlet` | Đánh giá phim |
| 4 | `AccountServlet` | `controller.AccountServlet` | `controller.auth.AccountServlet` | Tài khoản cá nhân |
| 5 | `MediaResourceServlet` | `controller.MediaResourceServlet` | `controller.common.MediaResourceServlet` | Phục vụ file media |
| 6 | `StaffServlet` | `controller.StaffServlet` | `controller.manager.StaffServlet` | Nhân viên cụm rạp |
| 7 | `MovieManagementServlet` | `controller.MovieManagementServlet` | `controller.movie.MovieManagementServlet` | Quản lý phim |
| 8 | `DistributionServlet` | `controller.DistributionServlet` | `controller.admin.DistributionServlet` | Phân bổ phim |
| 9 | `CentreManagementServlet` | `controller.CentreManagementServlet` | `controller.admin.CentreManagementServlet` | Quản lý chi nhánh |
| 10 | `HallManagementServlet` | `controller.HallManagementServlet` | `controller.manager.HallManagementServlet` | Quản lý phòng chiếu |
| 11 | `SeatConfigurationServlet` | `controller.SeatConfigurationServlet` | `controller.manager.SeatConfigurationServlet` | Sơ đồ & bảo trì ghế |
| 12 | `ReportServlet` | `controller.ReportServlet` | `controller.report.ReportServlet` | Báo cáo |
| 13 | `StaffTransferServlet` | `controller.StaffTransferServlet` | `controller.admin.StaffTransferServlet` | Điều chuyển nhân sự |
| 14 | `ManagerAccountServlet` | `controller.ManagerAccountServlet` | `controller.admin.ManagerAccountServlet` | Tài khoản quản lý |
| 15 | `CustomerManagementServlet` | `controller.CustomerManagementServlet` | `controller.customer.CustomerManagementServlet` | Khách hàng |
| 16 | `RegisterServlet` | `controller.RegisterServlet` | `controller.auth.RegisterServlet` | Đăng ký |
| 17 | `LoginServlet` | `controller.LoginServlet` | `controller.auth.LoginServlet` | Đăng nhập |
| 18 | `LogoutServlet` | `controller.LogoutServlet` | `controller.auth.LogoutServlet` | Đăng xuất |
| 19 | `ChangePasswordServlet` | `controller.ChangePasswordServlet` | `controller.auth.ChangePasswordServlet` | Đổi mật khẩu |
| 20 | `ForgotPasswordServlet` | `controller.ForgotPasswordServlet` | `controller.auth.ForgotPasswordServlet` | Quên mật khẩu |
| 21 | `VerifyOtpServlet` | `controller.VerifyOtpServlet` | `controller.auth.VerifyOtpServlet` | Xác thực OTP |
| 22 | `ResetPasswordServlet` | `controller.ResetPasswordServlet` | `controller.auth.ResetPasswordServlet` | Đặt lại mật khẩu |

### 8.3. Tiêu chuẩn chính thức được phê duyệt: 100% chuyển sang `@WebServlet` (Chuẩn Jakarta EE 10)

Nhóm thống nhất **chọn Phương án A**: Loại bỏ hoàn toàn sự phụ thuộc vào các thẻ `<servlet>` thủ công trong `web.xml` để triệt tiêu vĩnh viễn nguy cơ `ClassNotFoundException`.

#### 1. Nguyên tắc cấu hình:
- **Xóa sạch toàn bộ 22 cặp thẻ `<servlet>` và `<servlet-mapping>` trong `web.xml`** (giảm hơn 270 dòng XML rườm rà).
- **Thêm trực tiếp annotation `@WebServlet` lên đầu class Java tương ứng.**
- **`web.xml` từ nay CHỈ quản lý 3 thành phần duy nhất:**
  1. `<welcome-file-list>` (Trang mặc định ban đầu: `home/index.jsp`)
  2. `<filter>` & `<filter-mapping>` (Bộ lọc phân quyền & mã hóa UTF-8)
  3. `<error-page>` (Trang báo lỗi 404, 500)

#### 2. Danh sách 22 Annotation `@WebServlet` chuẩn hóa gắn vào từng Class:

```java
// 1. Phân hệ Admin (controller.admin)
@WebServlet(name = "AdminDashboardServlet", urlPatterns = {"/dashboard", "/admin/dashboard"})
public class AdminDashboardServlet extends HttpServlet { ... }

@WebServlet(name = "DistributionServlet", urlPatterns = {"/admin/distribution"})
public class DistributionServlet extends HttpServlet { ... }

@WebServlet(name = "CentreManagementServlet", urlPatterns = {"/admin/centres", "/admin/centre"})
public class CentreManagementServlet extends HttpServlet { ... }

@WebServlet(name = "StaffTransferServlet", urlPatterns = {"/admin/transfers"})
public class StaffTransferServlet extends HttpServlet { ... }

@WebServlet(name = "ManagerAccountServlet", urlPatterns = {"/admin/managers"})
public class ManagerAccountServlet extends HttpServlet { ... }

// 2. Phân hệ Quản lý Rạp (controller.manager)
@WebServlet(name = "HallManagementServlet", urlPatterns = {"/manager/halls", "/admin/halls"})
public class HallManagementServlet extends HttpServlet { ... }

@WebServlet(name = "SeatConfigurationServlet", urlPatterns = {"/manager/seat-config", "/admin/seat-config"})
public class SeatConfigurationServlet extends HttpServlet { ... }

@WebServlet(name = "StaffServlet", urlPatterns = {"/staff", "/manager/staff"})
public class StaffServlet extends HttpServlet { ... }

// 3. Phân hệ Khách hàng (controller.customer)
@WebServlet(name = "CustomerManagementServlet", urlPatterns = {"/admin/customers"})
public class CustomerManagementServlet extends HttpServlet { ... }

// 4. Phân hệ Phim (controller.movie)
@WebServlet(name = "MovieManagementServlet", urlPatterns = {"/admin/movies", "/admin/movie"})
public class MovieManagementServlet extends HttpServlet { ... }

@WebServlet(name = "MovieDetailsServlet", urlPatterns = {"/movie/details"})
public class MovieDetailsServlet extends HttpServlet { ... }

@WebServlet(name = "MovieRatingServlet", urlPatterns = {"/movie/rate"})
public class MovieRatingServlet extends HttpServlet { ... }

// 5. Phân hệ Xác thực & Tài khoản (controller.auth)
@WebServlet(name = "LoginServlet", urlPatterns = {"/login", "/auth/login"})
public class LoginServlet extends HttpServlet { ... }

@WebServlet(name = "LogoutServlet", urlPatterns = {"/logout", "/auth/logout"})
public class LogoutServlet extends HttpServlet { ... }

@WebServlet(name = "RegisterServlet", urlPatterns = {"/register", "/auth/register"})
public class RegisterServlet extends HttpServlet { ... }

@WebServlet(name = "ForgotPasswordServlet", urlPatterns = {"/forgot-password", "/auth/forgot-password"})
public class ForgotPasswordServlet extends HttpServlet { ... }

@WebServlet(name = "ResetPasswordServlet", urlPatterns = {"/reset-password", "/auth/reset-password"})
public class ResetPasswordServlet extends HttpServlet { ... }

@WebServlet(name = "VerifyOtpServlet", urlPatterns = {"/verify-otp", "/auth/verify-otp"})
public class VerifyOtpServlet extends HttpServlet { ... }

@WebServlet(name = "ChangePasswordServlet", urlPatterns = {"/change-password", "/auth/change-password"})
public class ChangePasswordServlet extends HttpServlet { ... }

@WebServlet(name = "AccountServlet", urlPatterns = {"/account", "/profile"})
public class AccountServlet extends HttpServlet { ... }

// 6. Phân hệ Báo cáo (controller.report)
@WebServlet(name = "ReportServlet", urlPatterns = {"/admin/reports"})
public class ReportServlet extends HttpServlet { ... }

// 7. Phân hệ Tiện ích chung (controller.common)
@WebServlet(name = "MediaResourceServlet", urlPatterns = {"/uploads/*"})
public class MediaResourceServlet extends HttpServlet { ... }
```

#### 3. Lợi ích khi chọn giải pháp này:
- **Triệt tiêu 100% nguy cơ `ClassNotFoundException`**: Vì không còn bất kỳ FQCN nào bị fix cứng trong XML.
- **Tự động hóa Refactor**: Khi dùng tính năng Refactor/Move package của IntelliJ hoặc NetBeans, annotation `@WebServlet` nằm trong file Java nên sẽ tự động ăn theo và không bao giờ bị lệch.
- **Thống nhất toàn dự án**: Toàn bộ các module hiện tại của team (Booking, Counter, Payment, Checkin, Fnb, Voucher, Showtime, Pricing) đều đã dùng `@WebServlet`. Việc chuyển 19 file còn lại sang `@WebServlet` sẽ giúp codebase đạt mức **đồng bộ 100% chuẩn Jakarta EE 10**.

---

## 9. LỘ TRÌNH THỰC THI REFACTORING (SAFE TEAM WORKFLOW)

Để đảm bảo việc cấu trúc lại không gây xung đột Git (Merge Conflicts) giữa 5 thành viên:

1. **Bước 1 (Họp chốt tiêu chuẩn):** Thái gửi tài liệu này cho cả nhóm (Hải, Hoàng, Việt, Huy). Thống nhất biểu quyết 100% thông qua quy chuẩn.
2. **Bước 2 (Code Freeze trong 24 giờ):** Tất cả thành viên commit và push hết code đang làm dở lên nhánh cá nhân, sau đó merge sạch vào `main`. Nhóm tạm dừng code mới.
3. **Bước 3 (Nhánh Refactor):** Tạo một nhánh riêng `refactor/clean-architecture` từ `main`.
4. **Bước 4 (Di chuyển file & Đồng bộ web.xml):**
   - Dùng lệnh `git mv` để di chuyển file JSP và Java (giữ nguyên lịch sử commit `git log`).
   - Cập nhật lại các câu lệnh `import` trong Java.
   - **Đồng bộ triệt để `web.xml` theo Phương án A:** Thêm `@WebServlet` vào 19 Servlet còn thiếu (mục 8.3) và xóa sạch 22 cặp thẻ `<servlet>` / `<servlet-mapping>` trong `web.xml`. Triệt tiêu hoàn toàn nguy cơ `ClassNotFoundException`.
   - Cập nhật lại đường dẫn `request.getRequestDispatcher("/WEB-INF/views/...")` trong các Servlet.
   - Cập nhật các đường dẫn CSS/JS trong thẻ `<link>` và `<script>`.
5. **Bước 5 (Kiểm thử nghiêm ngặt):**
   - Chạy lệnh `mvn clean compile test-compile` trên terminal &rarr; Đảm bảo 100% `BUILD SUCCESS`.
   - Khởi động Apache Tomcat, click kiểm tra toàn bộ luồng: Đăng nhập &rarr; Quản lý phim &rarr; Quản lý suất chiếu &rarr; Đặt vé &rarr; Soát vé &rarr; Báo cáo.
6. **Bước 6 (Merge & Pull):** Tạo Pull Request merge nhánh `refactor/clean-architecture` vào `main`. 4 thành viên còn lại chỉ cần `git pull origin main` về máy là có ngay một codebase chuẩn hóa quốc tế, sạch đẹp và đồng bộ 100%.
