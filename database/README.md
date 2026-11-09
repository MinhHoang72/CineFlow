# HƯỚNG DẪN CƠ SỞ DỮ LIỆU CINEFLOW (SQL SERVER)

Cơ sở dữ liệu hệ thống **CineFlow - Multi-Centre Cinema Management System** đã được chuẩn hóa và hợp nhất toàn diện thành **1 File Master duy nhất**.

---

## 1. Khởi tạo Cơ sở dữ liệu (All-in-One Master)

Chỉ cần thực hiện **1 bước duy nhất**:
1. Mở file **`database/CineFlow.sql`** bằng **SQL Server Management Studio (SSMS)** hoặc **Azure Data Studio**.
2. Nhấn phím **F5** (hoặc nút **Execute**) để khởi chạy.

> **Tự động hóa hoàn toàn:** Script sẽ tự động xóa CSDL cũ (nếu có), tạo mới `CineFlowDB`, thiết lập đầy đủ 30 bảng, quan hệ khóa ngoại, ràng buộc nghiệp vụ, trigger tự động cập nhật trạng thái đơn vé, và nạp toàn bộ dữ liệu mẫu (16 cụm rạp, 64 phòng chiếu, hàng ngàn ghế, suất chiếu, giá vé chi nhánh & khung giờ, F&B bắp nước, 204 Vouchers, 15 nhân viên, vé đặt và lịch sử check-in).

---

## 2. Thông tin kết nối mặc định (`DBContext.java`)

```properties
Database Name: CineFlowDB
Port: 1433
Default User: sa
Default Password: 123 (hoặc mật khẩu SQL Server của máy bạn)
```

---

## 3. Thư mục `database/demo/` (Kịch bản thuyết trình bảo vệ)

Thư mục `database/demo/` chứa các kịch bản SQL độc lập phục vụ lúc **thuyết trình / demo trước hội đồng**:
- `demo_checkin_0_reset.sql`: Reset dữ liệu vé và check-in về trạng thái ban đầu để demo lại từ đầu.
- `demo_checkin_1_too_early.sql`: Giả lập quét vé quá sớm trước giờ chiếu để kiểm tra thông báo từ chối.
- `demo_checkin_2_ready.sql`: Giả lập quét vé hợp lệ trong khung giờ chiếu (check-in thành công).
- `demo_voucher_data.sql`: Kịch bản nạp voucher thử nghiệm riêng.
