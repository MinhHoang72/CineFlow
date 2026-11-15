/* =====================================================================================
   PATCH: THÊM 6 VOUCHER TRANG CHỦ & CẬP NHẬT SƠ ĐỒ GHẾ PHÒNG 1 (CẦU GIẤY)
   Hệ thống: CineFlow (CineFlowDB)
   
   Nội dung:
   1. Thêm 6 Voucher thật toàn hệ thống (Admin, CentreID = NULL) vào bảng Vouchers
      để hiển thị trên khối "TIN MỚI & ƯU ĐÃI" của Trang chủ khách hàng (/home).
   2. Cập nhật sơ đồ ghế cho Phòng chiếu 01 (HallID = 1, rạp CineFlow Cầu Giấy):
      - 7 Hàng (A -> G) x 10 Cột (1 -> 10)
      - Lối đi ở giữa: Cột 5 và Cột 6 (UNAVAILABLE)
      - Hàng A, B, C, D: Ghế thường (STANDARD)
      - Hàng E, F: Ghế VIP
      - Hàng G (Hàng cuối): Ghế đôi SWEETBOX ghép cặp (1-2, 3-4, 7-8, 9-10)
      - Đồng bộ sức chứa Capacity cho Hall 1 = 56 ghế.
   ===================================================================================== */

USE CineFlowDB;
GO

SET NOCOUNT ON;

-- =====================================================================================
-- PHẦN 1: THÊM 6 VOUCHER MẪU THẬT CHO TRANG CHỦ
-- =====================================================================================
PRINT N'1. Đang thêm 6 voucher thật cho trang chủ...';

-- 1. Chào Bạn Mới
IF NOT EXISTS (SELECT 1 FROM Vouchers WHERE VoucherCode = 'CINEFLOW_NEWBIE')
BEGIN
    INSERT INTO Vouchers (
        VoucherCode, VoucherName, Description, CentreID, CreatedBy,
        DiscountType, DiscountValue, MinimumOrderValue, MaximumDiscount,
        StartDate, EndDate, UsageLimit, UsedCount, PerCustomerLimit, Status
    ) VALUES (
        'CINEFLOW_NEWBIE', N'Chào Bạn Mới - Giảm 50K', 
        N'Ưu đãi dành riêng cho thành viên mới đăng ký tài khoản CineFlow.',
        NULL, 1, 'FIXED', 50000.00, 100000.00, 50000.00,
        '2026-01-01', '2026-12-31', 1000, 0, 1, 'ACTIVE'
    );
END

-- 2. Thứ Tư Vui Vẻ
IF NOT EXISTS (SELECT 1 FROM Vouchers WHERE VoucherCode = 'HAPPY_WEDNESDAY')
BEGIN
    INSERT INTO Vouchers (
        VoucherCode, VoucherName, Description, CentreID, CreatedBy,
        DiscountType, DiscountValue, MinimumOrderValue, MaximumDiscount,
        StartDate, EndDate, UsageLimit, UsedCount, PerCustomerLimit, Status
    ) VALUES (
        'HAPPY_WEDNESDAY', N'Thứ Tư Vui Vẻ - Giảm 30%', 
        N'Tận hưởng ngày hội điện ảnh giữa tuần với giá vé cực sốc tại mọi rạp.',
        NULL, 1, 'PERCENT', 30.00, 90000.00, 40000.00,
        '2026-01-01', '2026-12-31', 1000, 0, 1, 'ACTIVE'
    );
END

-- 3. HSSV Siêu Ưu Đãi
IF NOT EXISTS (SELECT 1 FROM Vouchers WHERE VoucherCode = 'STUDENT_POWER')
BEGIN
    INSERT INTO Vouchers (
        VoucherCode, VoucherName, Description, CentreID, CreatedBy,
        DiscountType, DiscountValue, MinimumOrderValue, MaximumDiscount,
        StartDate, EndDate, UsageLimit, UsedCount, PerCustomerLimit, Status
    ) VALUES (
        'STUDENT_POWER', N'HSSV Siêu Ưu Đãi - Giảm 20%', 
        N'Ưu đãi giá vé dành cho học sinh, sinh viên áp dụng tất cả các ngày.',
        NULL, 1, 'PERCENT', 20.00, 80000.00, 35000.00,
        '2026-01-01', '2026-12-31', 1000, 0, 1, 'ACTIVE'
    );
END

-- 4. Cuối Tuần Bom Tấn
IF NOT EXISTS (SELECT 1 FROM Vouchers WHERE VoucherCode = 'WEEKEND_BOOM')
BEGIN
    INSERT INTO Vouchers (
        VoucherCode, VoucherName, Description, CentreID, CreatedBy,
        DiscountType, DiscountValue, MinimumOrderValue, MaximumDiscount,
        StartDate, EndDate, UsageLimit, UsedCount, PerCustomerLimit, Status
    ) VALUES (
        'WEEKEND_BOOM', N'Cuối Tuần Bom Tấn - Giảm 40K', 
        N'Bùng nổ cảm xúc cùng các siêu phẩm điện ảnh hot nhất dịp cuối tuần.',
        NULL, 1, 'FIXED', 40000.00, 150000.00, 40000.00,
        '2026-01-01', '2026-12-31', 1000, 0, 1, 'ACTIVE'
    );
END

-- 5. Combo Hẹn Hò
IF NOT EXISTS (SELECT 1 FROM Vouchers WHERE VoucherCode = 'SWEET_COUPLE')
BEGIN
    INSERT INTO Vouchers (
        VoucherCode, VoucherName, Description, CentreID, CreatedBy,
        DiscountType, DiscountValue, MinimumOrderValue, MaximumDiscount,
        StartDate, EndDate, UsageLimit, UsedCount, PerCustomerLimit, Status
    ) VALUES (
        'SWEET_COUPLE', N'Combo Hẹn Hò - Giảm 25%', 
        N'Ưu đãi ngọt ngào khi đặt vé ghế đôi hoặc đơn hàng từ 2 vé trở lên.',
        NULL, 1, 'PERCENT', 25.00, 160000.00, 50000.00,
        '2026-01-01', '2026-12-31', 1000, 0, 1, 'ACTIVE'
    );
END

-- 6. Cú Đêm Mê Phim
IF NOT EXISTS (SELECT 1 FROM Vouchers WHERE VoucherCode = 'NIGHT_OWL')
BEGIN
    INSERT INTO Vouchers (
        VoucherCode, VoucherName, Description, CentreID, CreatedBy,
        DiscountType, DiscountValue, MinimumOrderValue, MaximumDiscount,
        StartDate, EndDate, UsageLimit, UsedCount, PerCustomerLimit, Status
    ) VALUES (
        'NIGHT_OWL', N'Cú Đêm Mê Phim - Giảm 35K', 
        N'Giảm giá đặc biệt cho các suất chiếu sau 21h00 mỗi ngày.',
        NULL, 1, 'FIXED', 35000.00, 110000.00, 35000.00,
        '2026-01-01', '2026-12-31', 1000, 0, 1, 'ACTIVE'
    );
END

PRINT N'-> Thêm 6 voucher thành công!';
GO

-- =====================================================================================
-- PHẦN 2: CẬP NHẬT SƠ ĐỒ GHẾ CHO PHÒNG 1 (CẦU GIẤY)
-- =====================================================================================
PRINT N'2. Đang cấu hình sơ đồ ghế Phòng 1 (Cầu Giấy)...';

-- Bảng tạm chứa ma trận cấu hình 7 Hàng (A-G) x 10 Cột (1-10)
DECLARE @NewLayout TABLE (
    SeatRow VARCHAR(5),
    SeatNumber INT,
    SeatType VARCHAR(20),
    Status VARCHAR(20)
);

DECLARE @r INT = 1;
DECLARE @c INT = 1;
DECLARE @rName VARCHAR(5);
DECLARE @sType VARCHAR(20);
DECLARE @sStatus VARCHAR(20);

WHILE @r <= 7
BEGIN
    SET @rName = CHAR(64 + @r); -- A, B, C, D, E, F, G
    SET @c = 1;
    WHILE @c <= 10
    BEGIN
        -- Lối đi ở giữa (Cột 5 và Cột 6)
        IF @c IN (5, 6)
            SET @sStatus = 'UNAVAILABLE';
        ELSE
            SET @sStatus = 'AVAILABLE';

        -- Xác định loại ghế theo hàng:
        -- Hàng A, B, C, D: STANDARD
        -- Hàng E, F: VIP
        -- Hàng G: SWEETBOX (ghế đôi)
        IF @rName IN ('A', 'B', 'C', 'D')
            SET @sType = 'STANDARD';
        ELSE IF @rName IN ('E', 'F')
            SET @sType = 'VIP';
        ELSE
            SET @sType = 'SWEETBOX';

        INSERT INTO @NewLayout (SeatRow, SeatNumber, SeatType, Status)
        VALUES (@rName, @c, @sType, @sStatus);

        SET @c += 1;
    END
    SET @r += 1;
END

-- Áp dụng MERGE vào bảng Seats cho HallID = 1
MERGE Seats AS target
USING @NewLayout AS src
ON target.HallID = 1 AND target.SeatRow = src.SeatRow AND target.SeatNumber = src.SeatNumber
WHEN MATCHED THEN
    UPDATE SET 
        SeatType = src.SeatType,
        Status = src.Status
WHEN NOT MATCHED BY TARGET THEN
    INSERT (HallID, SeatRow, SeatNumber, SeatType, Status)
    VALUES (1, src.SeatRow, src.SeatNumber, src.SeatType, src.Status);

-- Các ghế cũ của Hall 1 không nằm trong ma trận A-G (1-10) sẽ chuyển sang UNAVAILABLE nếu đã có booking, hoặc xóa nếu chưa có
UPDATE Seats
SET Status = 'UNAVAILABLE'
WHERE HallID = 1
  AND NOT EXISTS (
      SELECT 1 FROM @NewLayout n 
      WHERE n.SeatRow = Seats.SeatRow AND n.SeatNumber = Seats.SeatNumber
  );

DELETE FROM Seats
WHERE HallID = 1
  AND Status = 'UNAVAILABLE'
  AND SeatID NOT IN (SELECT SeatID FROM BookingSeats);

-- Cập nhật lại sức chứa Capacity thực tế cho Phòng 1
UPDATE Halls
SET Capacity = (SELECT COUNT(*) FROM Seats WHERE HallID = 1 AND Status = 'AVAILABLE')
WHERE HallID = 1;

PRINT N'-> Cập nhật sơ đồ ghế Phòng 1 thành công!';
PRINT N'======================================================';
PRINT N'ĐÃ HOÀN TẤT PATCH TOÀN BỘ VOUCHER VÀ GHẾ!';
PRINT N'======================================================';
GO
