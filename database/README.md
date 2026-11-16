# HƯỚNG DẪN CƠ SỞ DỮ LIỆU CINEFLOW (SQL SERVER)

Cơ sở dữ liệu hệ thống **CineFlow - Multi-Centre Cinema Management System** đã được chuẩn hóa, dọn dẹp và hợp nhất toàn diện thành **1 File Master duy nhất**: `CineFlowDB.sql`.

---

## 1. Khởi tạo Cơ sở dữ liệu (All-in-One Master)

Chỉ cần thực hiện **1 bước duy nhất**:
1. Mở file **`database/CineFlowDB.sql`** bằng **SQL Server Management Studio (SSMS)** hoặc **Azure Data Studio**.
2. Nhấn phím **F5** (hoặc nút **Execute**) để khởi chạy.

> **Tự động hóa hoàn toàn (Chỉ mất khoảng 10-15 giây):**
> - Tự động xóa CSDL cũ (nếu tồn tại) và tạo mới database `CineFlowDB` hoàn chỉnh.
> - Thiết lập toàn bộ 30 bảng thực thể, quan hệ khóa ngoại, ràng buộc nghiệp vụ, view, function và stored procedure.
> - Nạp đầy đủ dữ liệu mẫu chuẩn hóa:
>   - **16 Cụm rạp**: Bao gồm rạp Cầu Giấy, Hà Đông, Hoàn Kiếm, Quận 1...
>   - **70+ Phòng chiếu**: Đầy đủ 2D, 3D, IMAX Laser, VIP Lounge... với cấu hình phân trang quản lý (>6 phòng).
>   - **Sơ đồ ghế chuẩn**: Phòng 01 Cầu Giấy chuẩn 56 ghế (7 hàng A-G x 10 cột, hàng cuối Sweetbox đôi ghép cặp, 2 cột lối đi ở giữa).
>   - **Suất chiếu & Bảng giá**: Cơ chế giá động theo cụm rạp, khung giờ, loại phòng, loại ghế và phim.
>   - **204+ Vouchers**: Bao gồm 6 voucher thật toàn hệ thống hiển thị khối Tin mới trang chủ (`/home`), 20 voucher demo thực tế cho Manager Cầu Giấy và 20 voucher cho Admin.
>   - **Kịch bản Demo bảo vệ**: Tích hợp sẵn test case suất chiếu tương lai có vé đặt `CONFIRMED` cho Phòng 01 và Phòng 61 để kiểm tra logic chặn đóng phòng chiếu khi có vé bán trước.

---

## 2. Thông tin kết nối mặc định (`DBContext.java`)

```properties
Database Name: CineFlowDB
Port: 1433
Default User: sa
Default Password: 123 (hoặc mật khẩu SQL Server của máy bạn)
```

---

## 3. Chạy qua dòng lệnh (Command Line)

Nếu không dùng SSMS, bạn có thể chạy bằng công cụ `sqlcmd` với cờ `-I` và mã hóa UTF-8 `-f 65001`:

```bash
sqlcmd -S localhost -U sa -P 123 -C -I -f 65001 -i database/CineFlowDB.sql
```


## 4. Đồng bộ sau khi pull

Nếu giữ database hiện tại, chạy `centre_suspension_patch.sql` một lần trước khi triển khai code mới.
Patch chỉ bổ sung dấu nhận biết suất bị huỷ do tạm ngưng rạp, không xoá dữ liệu.
Các suất CANCELLED từ trước không tự được khôi phục vì không xác định được nguyên nhân huỷ.

Toàn bộ schema, bảng giá chốt, dữ liệu demo phòng chiếu, voucher và dữ liệu bảo vệ nằm trong `CineFlowDB.sql`.
Các bản sửa voucher trên main và phòng 02 không có booking tương lai để demo đã được giữ lại khi hợp nhất.
Script khởi tạo sẽ xóa và tạo lại database; sao lưu dữ liệu cần giữ trước khi chạy.
Sau khi cập nhật database, Clean and Build rồi triển khai lại ứng dụng.
Không cần chạy các file patch cũ đã được gộp vào master.
