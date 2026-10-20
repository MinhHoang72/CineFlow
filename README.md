# CineFlow — Hệ Thống Quản Lý Chuỗi Rạp Chiếu Phim Đa Trung Tâm
> **MULTI-CENTRE CINEMA MANAGEMENT SYSTEM**  
> Dự án môn SWP391 — Xây dựng trên nền tảng Java Servlet/JSP, SQL Server, và mô hình MVC phân tầng chuẩn hoá.

---

## 🚀 Hướng Dẫn Chạy Dự Án

1. **Cơ sở dữ liệu (SQL Server)**: Chạy file `database/Cine_Flow.sql` bằng SSMS hoặc `sqlcmd` (bắt buộc có cờ `-I` để đảm bảo định danh trích dẫn):
   ```bash
   sqlcmd -S localhost -U sa -P <mật_khẩu> -C -I -f 65001 -i database/Cine_Flow.sql
   ```
   *Ghi chú*: Script sẽ khởi tạo database `CineFlowDB` và nạp sẵn dữ liệu mẫu. Cấu hình chuỗi kết nối tại `src/main/java/utils/DBContext.java` (mặc định: `localhost:1433`, user: `sa`).

2. **Build và Triển khai**: 
   - Sử dụng Maven:
     ```bash
     mvn clean package -DskipTests
     ```
   - Chạy trên máy chủ **Apache Tomcat 10+** (hỗ trợ Jakarta EE).
   - Địa chỉ truy cập: `http://localhost:8080/Cine_Flow/`

3. **Tài khoản mẫu** (mật khẩu mặc định: `123456` hoặc theo thông báo):
   - **Quản trị viên (Admin)**: `admin`
   - **Quản lý cụm rạp (Manager)**: `manager` (phụ trách rạp CineFlow Thủ Đức / Centre 21)
   - **Nhân viên rạp (Staff)**: `staff` (nhân viên bán vé, rạp 1)

---

## 📌 Phân Công Chức Năng Theo Nhánh (Branch Features)

### 🌟 Nhánh `ThaiDQHE181794` (Dương Quốc Thái)
Phụ trách các phân hệ cốt lõi về **Quản trị Người dùng & Nhân sự**, **Quản lý Phim & Phân phối Phim**, **Quản trị Cụm rạp** và **Chuẩn hoá Kiến trúc Hệ thống**:

| Phân hệ / Chức năng | Đường dẫn (URL) | Vai trò | Mô tả chi tiết |
| :--- | :--- | :--- | :--- |
| **Quản lý Quản lý rạp (Manager)** | `/admin/managers` | Admin | Tạo tài khoản Quản lý, bổ nhiệm quản lý vào từng cụm rạp/trung tâm, kích hoạt hoặc khóa tài khoản quản lý. |
| **Quản lý Nhân sự cụm rạp (Staff)** | `/staff` | Manager, Admin | Xem danh sách nhân viên trực thuộc cụm rạp, tạo tài khoản nhân viên bán vé & soát vé, lọc theo vị trí và trạng thái, cập nhật trạng thái hoạt động tức thì (AJAX), giao diện co gọn không tràn màn hình. |
| **Điều chuyển nhân sự** | `/admin/staff/transfer` | Admin | Điều chuyển nhân viên giữa các rạp chiếu khác nhau trong toàn chuỗi hệ thống. |
| **Quản lý Khách hàng** | `/admin/customers` | Admin | Quản lý danh sách khách hàng đăng ký, xem số điện thoại, email, điểm thưởng tích lũy và trạng thái tài khoản. |
| **Xác thực & Hồ sơ cá nhân** | `/profile`<br>`/login`<br>`/register`<br>`/forgot-password`<br>`/reset-password`<br>`/verify-otp` | Mọi người dùng | Đăng nhập, đăng ký tài khoản khách hàng, đổi mật khẩu, xác thực OTP qua Email khi quên mật khẩu, cập nhật thông tin cá nhân. |
| **Quản lý Danh mục Phim** | `/movies` | Admin | Thêm phim mới, chỉnh sửa thông tin phim (tiêu đề, đạo diễn, diễn viên, thể loại, thời lượng, ngày khởi chiếu, độ tuổi phân loại), upload poster và video trailer. |
| **Phân phối Phim** | `/admin/distribution` | Admin | Phân phối phim về từng cụm rạp/trung tâm, kiểm soát bật/tắt trạng thái chiếu phim tại từng chi nhánh cụ thể. |
| **Chi tiết & Đánh giá Phim** | `/movie`<br>`/movie/rating` | Public, Customer | Xem thông tin chi tiết phim, xem trailer trực tuyến và đánh giá/bình luận phim. |
| **Quản lý Cụm rạp (Centre)** | `/admin/centres` | Admin | Thêm mới và quản lý danh sách các trung tâm/cụm rạp CineFlow trên toàn quốc (tên rạp, thành phố, địa chỉ, số phòng chiếu). |
| **Chuẩn hoá Kiến trúc Hệ thống** | Toàn bộ dự án | Toàn hệ thống | Tái cấu trúc chuẩn mô hình MVC phân tầng (`service`, `dao`, `model.entity`, `model.dto`, `controller.*`), khắc phục triệt để lỗi mã hoá tiếng Việt UTF-8, dọn dẹp file thừa và chuẩn hoá giao diện Dark Slate responsive. |

---

### 🎬 Các Phân Hệ Khác Trong Hệ Thống

| Phân hệ / Chức năng | Đường dẫn (URL) | Vai trò | Mô tả |
| :--- | :--- | :--- | :--- |
| **Thực đơn F&B** | `/fnb` | Admin, Manager, Staff | Quản lý danh mục combo, bắp, nước, đồ ăn kèm tại rạp. |
| **Kho F&B theo cụm rạp** | `/fnb/stock` | Manager, Staff | Quản lý bật/tắt tồn kho đồ ăn uống theo từng chi nhánh. |
| **Mã giảm giá (Voucher)** | `/admin/vouchers` | Admin | Quản lý các chương trình ưu đãi, mã voucher giảm giá vé. |
| **Soát vé tại cửa rạp** | `/checkin` | Staff (Checker), Manager | Nhân viên soát vé quét/nhập mã vé kiểm tra hợp lệ trước giờ chiếu, Manager xem lịch sử check-in. |
| **Báo cáo & Thống kê** | `/report/sales`<br>`/report/operations`<br>`/report/chain` | Admin, Manager | Báo cáo doanh thu bán vé, doanh thu F&B, tỷ lệ lấp đầy phòng chiếu và Dashboard tổng quan toàn chuỗi. |
| **Suất chiếu & Bảng giá** | `/showtimes`<br>`/pricing` | Admin, Manager | Lập lịch chiếu phim theo phòng, cấu hình bảng giá vé theo khung giờ và phụ thu loại ghế. |
| **Đặt vé & Thanh toán** | `/booking`<br>`/payment` | Customer, Staff (Counter) | Chọn suất chiếu, sơ đồ ghế ngồi trực quan, áp dụng voucher, thanh toán VNPay và in vé tại quầy. |

*Lưu ý*: Nếu cơ sở dữ liệu đã nạp nhưng danh mục F&B chưa có dữ liệu, hãy chạy thêm script phụ `database/fnb_seed_per_centre.sql`.
