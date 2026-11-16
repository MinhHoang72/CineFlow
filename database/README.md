# HƯỚNG DẪN CƠ SỞ DỮ LIỆU CINEFLOW (SQL SERVER)

Cơ sở dữ liệu hệ thống **CineFlow - Multi-Centre Cinema Management System** đã được chuẩn hóa và hợp nhất toàn diện thành **1 File Master duy nhất**.

---

## 1. Khởi tạo Cơ sở dữ liệu (All-in-One Master)

Chỉ cần thực hiện **1 bước duy nhất**:
1. Mở file **`database/CineFlow.sql`** bằng **SQL Server Management Studio (SSMS)** hoặc **Azure Data Studio**.
2. Nhấn phím **F5** (hoặc nút **Execute**) để khởi chạy.

> **Tự động hóa hoàn toàn:** Script sẽ tự động xóa CSDL cũ (nếu có), tạo mới `CineFlowDB`, thiết lập đầy đủ các bảng, quan hệ khóa ngoại, ràng buộc nghiệp vụ, và nạp toàn bộ dữ liệu mẫu (16 cụm rạp, 64 phòng chiếu, hàng ngàn ghế, suất chiếu, giá vé chi nhánh & khung giờ, F&B bắp nước, 204 Vouchers, 15 nhân viên, vé đặt).

---

## 2. Thông tin kết nối mặc định (`DBContext.java`)

```properties
Database Name: CineFlowDB
Port: 1433
Default User: sa
Default Password: 123 (hoặc mật khẩu SQL Server của máy bạn)
```

---

## 3. Sau khi pull code mới: chạy lại `CineFlow.sql`

Từ ngày 16/11/2026, mọi thay đổi CSDL của nhóm đã được gộp vào **`database/CineFlow.sql`** (giá chốt theo suất chiếu, bỏ soát vé và đánh giá phim, hàng ghế Sweetbox Phòng 01 Cầu Giấy và giá Sweetbox, voucher trang chủ, dữ liệu demo bảo vệ, 7 phòng demo phân trang, voucher TEST01–TEST12). Không còn file patch riêng.

Nếu ứng dụng báo lỗi 500 sau khi pull, nhiều khả năng CSDL trên máy đang cũ hơn code: chạy lại `CineFlow.sql` (script tự xoá và tạo lại CineFlowDB, dữ liệu test cũ trên máy sẽ mất), rồi Clean and Build và chạy lại Tomcat.

Khi thêm hoặc sửa dữ liệu dùng chung, ghi thẳng vào `CineFlow.sql` thay vì sửa trên giao diện hoặc tạo file patch riêng, để máy nào cũng có cùng dữ liệu.

---

## 4. Dữ liệu demo bảo vệ

Dữ liệu demo bảo vệ (đơn test để thử chặn tắt phòng chiếu, tên voucher demo) đã nằm trong `CineFlow.sql`. File `database/demo_defense_data_setup.sql` vẫn giữ để chạy lại riêng phần này khi cần làm mới dữ liệu demo (ví dụ đưa suất test về 7 ngày sau thời điểm demo).
