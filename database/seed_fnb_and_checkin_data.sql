USE CineFlowDB;
GO

-- 1. Cập nhật tồn kho bắp nước chi nhánh (Centre 1 và Centre 2) để luôn sẵn sàng bán
UPDATE CentreFnbInventory
SET IsAvailable = 1, StockQuantity = 150
WHERE CentreID IN (1, 2);

-- 2. Thêm vé hợp lệ cho Centre 2 (Hà Đông) với suất chiếu đang diễn ra để demo soát vé
-- Cập nhật vé 21 và 22 thành VALID và đưa suất chiếu về hôm nay
DECLARE @showtimeHadong INT;
SELECT TOP 1 @showtimeHadong = st.ShowtimeID 
FROM Showtimes st 
JOIN Halls h ON h.HallID = st.HallID 
WHERE h.CentreID = 2;

IF @showtimeHadong IS NOT NULL
BEGIN
    UPDATE Showtimes
    SET StartTime = DATEADD(minute, -15, GETDATE()),
        EndTime = DATEADD(minute, 105, GETDATE()),
        Status = 'ACTIVE'
    WHERE ShowtimeID = @showtimeHadong;

    -- Đặt vé 21 và 22 thành hợp lệ thuộc suất chiếu này
    UPDATE t
    SET t.TicketStatus = 'VALID', t.CheckedInAt = NULL, t.CheckedInBy = NULL
    FROM Tickets t
    WHERE t.TicketID IN (21, 22);

    UPDATE b
    SET b.BookingStatus = 'CONFIRMED', b.ShowtimeID = @showtimeHadong
    FROM Bookings b
    JOIN BookingSeats bs ON bs.BookingID = b.BookingID
    JOIN Tickets t ON t.BookingSeatID = bs.BookingSeatID
    WHERE t.TicketID IN (21, 22);
END;

-- 3. Thêm các bản ghi nhật ký soát vé (CheckInLogs) cho Centre 2 (staff_hadong, UserID = 6)
-- để danh sách lịch sử hiển thị đầy đủ, đẹp mắt
INSERT INTO CheckInLogs (TicketID, CheckedBy, CheckInTime, Result, Note)
VALUES
    (20, 6, DATEADD(minute, -30, GETDATE()), 'SUCCESS', N'Quét mã QR qua cổng soát vé thành công'),
    (NULL, 6, DATEADD(minute, -25, GETDATE()), 'REJECTED', N'Không tìm thấy vé với mã này.'),
    (20, 6, DATEADD(minute, -20, GETDATE()), 'REJECTED', N'Vé đã được sử dụng trước đó.'),
    (1, 6, DATEADD(minute, -15, GETDATE()), 'REJECTED', N'Vé thuộc Centre khác (CineFlow Cẩm Lệ).'),
    (20, 6, DATEADD(minute, -10, GETDATE()), 'SUCCESS', N'Nhân viên xác thực vé thủ công'),
    (NULL, 6, DATEADD(minute, -5, GETDATE()), 'REJECTED', N'Không tìm thấy vé với mã này.'),
    (20, 6, DATEADD(minute, -2, GETDATE()), 'SUCCESS', N'Quét mã vé thành công');

-- 4. Thêm các bản ghi nhật ký soát vé cho Centre 1 (staff_cg_03, UserID = 25)
INSERT INTO CheckInLogs (TicketID, CheckedBy, CheckInTime, Result, Note)
VALUES
    (NULL, 25, DATEADD(minute, -40, GETDATE()), 'REJECTED', N'Không tìm thấy vé với mã này.'),
    (NULL, 25, DATEADD(minute, -35, GETDATE()), 'REJECTED', N'Mã vé không đúng định dạng.'),
    (1, 25, DATEADD(minute, -18, GETDATE()), 'REJECTED', N'Vé thuộc Centre khác.'),
    (NULL, 25, DATEADD(minute, -5, GETDATE()), 'REJECTED', N'Không tìm thấy vé với mã này.');

PRINT N'Dữ liệu cho FnB và Soát vé đã được bổ sung thành công!';
GO
