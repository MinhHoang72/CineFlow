# Pricing và lịch mở bán

Áp dụng `pricing_sale_lifecycle.sql` **sau** database gốc, trước khi chạy phiên bản ứng dụng này. Chạy bằng client dừng khi có lỗi; transaction bao trùm các batch `GO`. Không chạy riêng từng đoạn. Có thể chạy lại script mà không sao chép trùng dữ liệu cũ.

- Admin quản lý `CentrePricing`. Manager đọc giá cơ bản nhưng chỉ chỉnh phụ thu của cụm rạp được phân công.
- `CentreSurcharges` lưu 5 nhóm: chi nhánh, loại phòng, loại ghế, từng phim, khung giờ. Mỗi lần lưu tạo một phiên bản; thời điểm áp dụng dựa trên giờ bắt đầu chiếu. Khung giờ dùng khoảng `[từ giờ, đến giờ)` và không chồng lấn trong cùng giai đoạn hiệu lực.
- Các bảng `Surcharges` và `TimeSlotPricing` cũ được giữ để đối chiếu. Ứng dụng mới dùng `CentreSurcharges`; không nhập thêm chính sách mới vào hai bảng cũ.
- Script sao chép phụ thu chung sang từng rạp, sao chép phụ thu giờ theo rạp và chốt giá cũ cho các suất đã có chính sách giá. `BookingSeats.Price` không bị sửa. Nếu suất đã có booking nhưng thiếu giá cơ bản lịch sử, toàn bộ migration dừng thay vì xóa hoặc tự đặt giá.
- Tạo suất mới đồng nghĩa mở bán ngay. Tạo/sửa và chốt `ShowtimeSeatPrices` diễn ra trong cùng giao dịch; thiếu giá cơ bản hoặc phim/phòng không khả dụng thì rollback toàn bộ.
- Giá lấy từ chính sách có hiệu lực tại giờ bắt đầu chiếu. Chính sách đổi sau đó không làm đổi bảng giá đã chốt.
- Trước giờ chiếu, được sửa/hủy nếu không có booking CONFIRMED hoặc booking PENDING còn ghế HELD chưa hết hạn. Đơn hủy/hết hạn không khóa suất.
- Lưu sửa sẽ chốt lại giá và tiếp tục mở bán ngay. Đơn cũ không còn hiệu lực được hủy để callback thanh toán muộn không xuất vé theo lịch đã thay đổi; tiền về muộn được ghi nhận theo luồng cần hoàn tiền hiện có.
- Booking, thanh toán và thao tác sửa/hủy cùng khóa hàng suất chiếu. Tác vụ `ShowtimeService` chỉ đồng bộ Sắp chiếu → Đang chiếu → Đã hoàn thành mỗi 30 giây.
- Không còn giao diện/endpoint mở bán thủ công hoặc lên lịch mở bán. Các cột SaleState/SaleOpensAt/PriceLockedAt được giữ để tương thích booking và lưu thời điểm chốt giá; DRAFT chỉ được dùng nội bộ trong giao dịch tái chốt giá.
- Dữ liệu cũ chưa OPEN (nếu có) được giữ nguyên, hiển thị cảnh báo để Manager lưu cập nhật và mở bán; không tự công bố hàng loạt.

Kiểm thử tích hợp cục bộ: sau Maven build, chạy `PricingLifecycleIntegrationTest --local-db` với classpath gồm `target/classes`, `target/test-classes`, JDBC và Servlet API. Bài kiểm thử tạo rạp/suất/booking thử riêng và dọn các bản ghi đó trong `finally`; không dùng trên môi trường phục vụ khách thật.
