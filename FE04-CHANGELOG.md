## Merge origin/main vào vietnq — 20/10/2026
- Giữ cấu trúc Maven của main: Java trong src/main/java, JSP/CSS trong src/main/webapp.
- Chuyển FE-04 vào cấu trúc mới; hợp nhất API HallDAO/MovieDAO/ShowtimeDAO/SeatDAO để giữ cả quản lý và đặt vé/cấu hình ghế.
- Showtime giữ kiểu Timestamp và constructor của luồng đặt vé, bổ sung các setter và thuộc tính/định dạng cho FE-04.
- Giữ User/UserDAO từ main vì đã hỗ trợ Centre; giữ các API/model giá FE-04 hiện dùng.
- Maven package (bỏ chạy test database) thành công. Trên môi trường Windows này cần JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 để Maven xử lý đường dẫn có dấu.
- Chưa deploy hoặc chạy các verification runner vì chúng cần và có thể sửa dữ liệu SQL Server.

Phần dưới là báo cáo trước merge; đường dẫn và nhận xét về module ngoài FE-04 phản ánh trạng thái trước khi lấy main.

# FE-04 — cập nhật theo database/CineFlow.sql

## Thay đổi
- HallDAO, MovieDAO, ShowtimeDAO: CentreMovies/CentreID; API lấy danh sách theo Centre.
- Hall: centreId. Showtime: hallName/hallType, map cả danh sách và chi tiết.
- Surcharge/SurchargeDAO: SurchargeType + TargetValue; truy vấn SEAT/HALL theo thời điểm; thêm getHallSurchargeAt. Cập nhật rule vẫn đóng bản cũ và thêm bản mới trong transaction.
- PricingService: Base + TimeSlot + Seat + Hall. API 4 tham số nhận hallType. API 3 tham số được giữ tương thích, mặc định phòng 2D.
- Servlet FE-04: bỏ ID centre/manager cố định, sử dụng userId/session và CentreManagerAssignments. ADMIN chọn centre qua centreId (có nhận branchId cũ); lựa chọn được giữ trong session. MANAGER dùng phân công ACTIVE có hiệu lực.
- Sửa các đường dẫn forward JSP theo thư mục hiện tại. Màn phụ thu hiển thị và chỉnh sửa cả SEAT/HALL; giữ loại/đối tượng từ rule gốc khi sửa.
- Sửa mapping UserDAO đọc CentreID để đăng nhập hoạt động với schema mới; thêm alias centreId vào User, giữ API branchId cho các module cũ.
- Giữ nguyên logic kiểm tra trùng lịch, cleaning buffer, hủy suất và lịch sử giá.

## Các file đã chỉnh trong lần này (tính từ thư mục project)
- src/java/dao/HallDAO.java
- src/java/dao/MovieDAO.java
- src/java/dao/ShowtimeDAO.java
- src/java/dao/SurchargeDAO.java
- src/java/dao/BranchPricingDAO.java
- src/java/dao/TimeSlotPricingDAO.java
- src/java/dao/UserDAO.java
- src/java/model/Hall.java
- src/java/model/Showtime.java
- src/java/model/Surcharge.java
- src/java/model/User.java
- src/java/service/PricingService.java
- src/java/utils/FE04Context.java (mới)
- src/java/controller/showtime/CreateShowtimeServlet.java
- src/java/controller/showtime/EditShowtimeServlet.java
- src/java/controller/showtime/ShowtimeListServlet.java (giữ thay đổi có sẵn)
- src/java/controller/showtime/UpdateShowtimeServlet.java
- src/java/controller/pricing/PricingManagementServlet.java
- src/java/controller/pricing/UpdateBasePriceServlet.java
- src/java/controller/pricing/surcharge/EditSeatSurchargeServlet.java
- src/java/controller/pricing/surcharge/SeatSurchargeServlet.java
- src/java/controller/pricing/surcharge/UpdateSeatSurchargeServlet.java
- src/java/controller/pricing/timeslot/EditTimeSlotPricingServlet.java
- src/java/controller/pricing/timeslot/SaveTimeSlotPricingServlet.java
- src/java/controller/pricing/timeslot/TimeSlotPricingServlet.java
- src/java/controller/pricing/timeslot/UpdateTimeSlotPricingServlet.java
- web/pricing/edit-seat-surcharge.jsp
- web/pricing/edit-time-slot-pricing.jsp
- web/pricing/pricing-management.jsp
- web/pricing/seat-surcharge.jsp
- web/pricing/time-slot-pricing.jsp
- web/showtime/showtime-list.jsp (giữ thay đổi có sẵn)

## Kiểm tra và giới hạn
- Biên dịch toàn bộ src/java bằng javac 17, thư viện lib và Tomcat 10.1.44: PASS (exit 0).
- git diff --check: PASS.
- Chưa build WAR qua NetBeans/Ant (ant không có trên PATH), chưa deploy Tomcat hay chạy truy vấn trên SQL Server thực tế. Biên dịch không xác nhận hoạt động SQL/JSP lúc chạy.
- BranchPricing và TimeSlotPricing trong SQL hiện tại vẫn có cột vật lý BranchID; CentreID là computed column. Vì vậy DAO giá vẫn ghi BranchID đúng schema, biến Java dùng centreId.
- Ngoài phạm vi FE-04, CinemaDAO, ReportDAO và một số chức năng của UserDAO (lịch sử đặt vé, danh sách/cập nhật nhân viên) vẫn tham chiếu Branches/BranchMovies/BranchID cũ. Chưa di chuyển các module này; chúng có thể lỗi SQL với schema mới.
- Các thay đổi có sẵn tại DBContext, trang xem ghế và database được giữ nguyên. Không chỉnh CineFlowDemo.
