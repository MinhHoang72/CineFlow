# CineFlow
MULTI-CENTRE CINEMA MANAGEMENT

## Chạy dự án

1. **Database (SQL Server)**: chạy `database/Cine_Flow.sql` bằng SSMS, hoặc bằng `sqlcmd` (bắt buộc có `-I`):

   ```
   sqlcmd -S localhost -U sa -P <mật khẩu> -C -I -f 65001 -i database/Cine_Flow.sql
   ```

   Script xóa và tạo lại database `CineFlowDB`, nạp dữ liệu mẫu (kể cả thực đơn F&B cho từng Centre).
   Kết nối của ứng dụng cấu hình ở `src/main/java/utils/DBContext.java` (mặc định `localhost:1433`, user `sa`).
2. **Ứng dụng**: build bằng Maven (`mvn clean package`) hoặc *Clean and Build* trong NetBeans, chạy trên Tomcat 10+.
   Địa chỉ: `http://localhost:8080/Cine_Flow/`
3. **Tài khoản mẫu** (mật khẩu mặc định của dữ liệu mẫu theo ghi chú trong `Cine_Flow.sql`: `123456`): `admin`, `manager` (Centre 21), `staff` (nhân viên bán vé, Centre 1).

## Các chức năng: F&B, Voucher, Soát vé, Báo cáo

| Chức năng | Địa chỉ | Vai trò |
| --- | --- | --- |
| Thực đơn F&B | `/fnb` | Admin xem; Manager sửa; nhân viên bán vé xem |
| Kho F&B theo Centre | `/fnb/stock` | Manager bật/tắt; nhân viên bán vé xem |
| Voucher | `/admin/vouchers` | Admin |
| Soát vé | `/checkin` | Nhân viên soát vé; Manager xem lịch sử |
| Báo cáo doanh thu, vận hành | `/report/sales`, `/report/operations` | Admin, Manager |
| Dashboard chuỗi | `/report/chain` | Admin |

Nếu database đã có sẵn nhưng trang F&B trống, chạy thêm `database/fnb_seed_per_centre.sql` (chạy lại nhiều lần vẫn an toàn).
