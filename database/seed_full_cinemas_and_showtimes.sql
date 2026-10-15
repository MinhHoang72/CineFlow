USE CineFlowDB;
GO

/* =========================================================================
   CINEFLOW - BỘ DỮ LIỆU MẪU ĐẦY ĐỦ CHO TEST ĐẶT VÉ (BOOKING & SHOWTIMES)
   - Thêm nhiều cụm rạp tại Hà Nội, TP.HCM, Đà Nẵng, Hải Phòng
   - Thêm phòng chiếu và ghế ngồi
   - Tạo hàng chục suất chiếu động theo ngày hiện tại (GETDATE())
   ========================================================================= */

-- 1. THÊM CHI NHÁNH / CỤM RẠP NẾU CHƯA CÓ
IF NOT EXISTS (SELECT 1 FROM Centres WHERE CentreName = N'CineFlow Hoàn Kiếm')
    INSERT INTO Centres (CentreName, Address, City, Phone, Status) VALUES (N'CineFlow Hoàn Kiếm', N'88 Hàng Bài, Hoàn Kiếm', N'Hà Nội', '02433333333', 'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM Centres WHERE CentreName = N'CineFlow Hai Bà Trưng')
    INSERT INTO Centres (CentreName, Address, City, Phone, Status) VALUES (N'CineFlow Hai Bà Trưng', N'191 Bà Triệu, Hai Bà Trưng', N'Hà Nội', '02437777777', 'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM Centres WHERE CentreName = N'CineFlow Tây Hồ')
    INSERT INTO Centres (CentreName, Address, City, Phone, Status) VALUES (N'CineFlow Tây Hồ', N'272 Võ Chí Công, Tây Hồ', N'Hà Nội', '02438888888', 'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM Centres WHERE CentreName = N'CineFlow Quận 1')
    INSERT INTO Centres (CentreName, Address, City, Phone, Status) VALUES (N'CineFlow Quận 1', N'120 Nguyễn Huệ, Quận 1', N'TP. Hồ Chí Minh', '02844444444', 'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM Centres WHERE CentreName = N'CineFlow Landmark 81')
    INSERT INTO Centres (CentreName, Address, City, Phone, Status) VALUES (N'CineFlow Landmark 81', N'Vinhomes Central Park, Bình Thạnh', N'TP. Hồ Chí Minh', '02855555555', 'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM Centres WHERE CentreName = N'CineFlow Gò Vấp')
    INSERT INTO Centres (CentreName, Address, City, Phone, Status) VALUES (N'CineFlow Gò Vấp', N'304 Quang Trung, Gò Vấp', N'TP. Hồ Chí Minh', '02866666666', 'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM Centres WHERE CentreName = N'CineFlow Thủ Đức')
    INSERT INTO Centres (CentreName, Address, City, Phone, Status) VALUES (N'CineFlow Thủ Đức', N'216 Võ Văn Ngân, TP. Thủ Đức', N'TP. Hồ Chí Minh', '02877777777', 'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM Centres WHERE CentreName = N'CineFlow Hải Châu')
    INSERT INTO Centres (CentreName, Address, City, Phone, Status) VALUES (N'CineFlow Hải Châu', N'50 Bạch Đằng, Hải Châu', N'Đà Nẵng', '02365555555', 'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM Centres WHERE CentreName = N'CineFlow Cẩm Lệ')
    INSERT INTO Centres (CentreName, Address, City, Phone, Status) VALUES (N'CineFlow Cẩm Lệ', N'100 Cách Mạng Tháng 8, Cẩm Lệ', N'Đà Nẵng', '02366666666', 'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM Centres WHERE CentreName = N'CineFlow Ngô Quyền')
    INSERT INTO Centres (CentreName, Address, City, Phone, Status) VALUES (N'CineFlow Ngô Quyền', N'20 Lạch Tray, Ngô Quyền', N'Hải Phòng', '02256666666', 'ACTIVE');
GO

-- 2. TẠO PHÒNG CHIẾU CHO TẤT CẢ CÁC RẠP
INSERT INTO Halls (CentreID, HallName, HallType, Capacity, Status)
SELECT b.CentreID, h.HallName, h.HallType, h.Capacity, 'AVAILABLE'
FROM Centres b
CROSS JOIN (
    VALUES
    (N'Phòng 2D 01', '2D', 60),
    (N'Phòng 2D 02', '2D', 50),
    (N'Phòng 3D 01', '3D', 80)
) h(HallName, HallType, Capacity)
WHERE NOT EXISTS (
    SELECT 1 FROM Halls existing
    WHERE existing.CentreID = b.CentreID AND existing.HallName = h.HallName
);
GO

-- 3. TẠO GHẾ NGỒI CHO CÁC PHÒNG MỚI (NẾU CHƯA CÓ GHẾ)
INSERT INTO Seats (HallID, SeatRow, SeatNumber, SeatType, Status)
SELECT h.HallID, r.SeatRow, n.SeatNumber,
       CASE WHEN r.SeatRow IN ('C','D') THEN 'VIP'
            WHEN r.SeatRow = 'E' AND n.SeatNumber IN (5,6) THEN 'SWEETBOX'
            ELSE 'STANDARD' END,
       'AVAILABLE'
FROM Halls h
CROSS JOIN (VALUES ('A'),('B'),('C'),('D'),('E')) r(SeatRow)
CROSS JOIN (VALUES (1),(2),(3),(4),(5),(6),(7),(8),(9),(10)) n(SeatNumber)
WHERE NOT EXISTS (
    SELECT 1 FROM Seats s WHERE s.HallID = h.HallID
);
GO

-- 4. TẠO SUẤT CHIẾU ĐỘNG TỪ HÔM NAY ĐẾN 6 NGÀY TIẾP THEO
-- Xóa các suất chiếu cũ quá hạn nếu cần hoặc thêm mới các suất chiếu hôm nay/ngày mai
DECLARE @Today DATE = CAST(GETDATE() AS DATE);
DECLARE @AdminId INT = (SELECT TOP 1 UserID FROM Users WHERE Role IN ('ADMIN','MANAGER') ORDER BY UserID);

-- Bảng ánh xạ: Phim nào chiếu ở rạp nào với các khung giờ (đảm bảo có rạp có phim, có rạp không có phim để test hiệu ứng mờ/sáng)
;WITH MovieCentreSchedule AS (
    -- Phim 1 (Avatar 2): Chiếu tại Cầu Giấy, Hoàn Kiếm, Tây Hồ, Landmark 81, Quận 1, Hải Châu
    SELECT 1 AS MovieID, N'CineFlow Cầu Giấy' AS CentreName, 0 AS DayOffset, '10:00:00' AS ShowTime, 85000 AS Price UNION ALL
    SELECT 1, N'CineFlow Cầu Giấy', 0, '14:30:00', 95000 UNION ALL
    SELECT 1, N'CineFlow Cầu Giấy', 0, '19:15:00', 110000 UNION ALL
    SELECT 1, N'CineFlow Cầu Giấy', 1, '11:00:00', 85000 UNION ALL
    SELECT 1, N'CineFlow Cầu Giấy', 1, '20:00:00', 110000 UNION ALL
    SELECT 1, N'CineFlow Hoàn Kiếm', 0, '09:30:00', 95000 UNION ALL
    SELECT 1, N'CineFlow Hoàn Kiếm', 0, '15:45:00', 110000 UNION ALL
    SELECT 1, N'CineFlow Hoàn Kiếm', 1, '18:30:00', 120000 UNION ALL
    SELECT 1, N'CineFlow Landmark 81', 0, '13:00:00', 120000 UNION ALL
    SELECT 1, N'CineFlow Landmark 81', 0, '17:30:00', 140000 UNION ALL
    SELECT 1, N'CineFlow Landmark 81', 1, '20:15:00', 140000 UNION ALL
    SELECT 1, N'CineFlow Quận 1', 0, '10:45:00', 105000 UNION ALL
    SELECT 1, N'CineFlow Quận 1', 1, '19:00:00', 125000 UNION ALL
    SELECT 1, N'CineFlow Hải Châu', 0, '14:00:00', 85000 UNION ALL
    SELECT 1, N'CineFlow Hải Châu', 1, '18:45:00', 95000 UNION ALL

    -- Phim 2 (Inception): Chiếu tại Hà Đông, Tây Hồ, Gò Vấp, Thủ Đức, Cẩm Lệ (KHÔNG chiếu Cầu Giấy, Hoàn Kiếm)
    SELECT 2, N'CineFlow Hà Đông', 0, '11:15:00', 80000 UNION ALL
    SELECT 2, N'CineFlow Hà Đông', 0, '16:00:00', 90000 UNION ALL
    SELECT 2, N'CineFlow Hà Đông', 1, '19:30:00', 100000 UNION ALL
    SELECT 2, N'CineFlow Tây Hồ', 0, '13:45:00', 90000 UNION ALL
    SELECT 2, N'CineFlow Tây Hồ', 1, '21:00:00', 110000 UNION ALL
    SELECT 2, N'CineFlow Gò Vấp', 0, '09:15:00', 85000 UNION ALL
    SELECT 2, N'CineFlow Gò Vấp', 0, '15:30:00', 95000 UNION ALL
    SELECT 2, N'CineFlow Gò Vấp', 1, '18:15:00', 105000 UNION ALL
    SELECT 2, N'CineFlow Thủ Đức', 0, '14:20:00', 90000 UNION ALL
    SELECT 2, N'CineFlow Thủ Đức', 1, '20:30:00', 110000 UNION ALL

    -- Phim 4 (Despicable Me 4): Chiếu tại Cầu Giấy, Hà Đông, Hai Bà Trưng, Quận 1, Gò Vấp, Ngô Quyền
    SELECT 4, N'CineFlow Cầu Giấy', 0, '09:00:00', 80000 UNION ALL
    SELECT 4, N'CineFlow Cầu Giấy', 0, '13:15:00', 85000 UNION ALL
    SELECT 4, N'CineFlow Cầu Giấy', 1, '10:30:00', 85000 UNION ALL
    SELECT 4, N'CineFlow Hà Đông', 0, '10:00:00', 80000 UNION ALL
    SELECT 4, N'CineFlow Hà Đông', 1, '14:00:00', 85000 UNION ALL
    SELECT 4, N'CineFlow Hai Bà Trưng', 0, '11:30:00', 90000 UNION ALL
    SELECT 4, N'CineFlow Hai Bà Trưng', 1, '16:45:00', 100000 UNION ALL
    SELECT 4, N'CineFlow Quận 1', 0, '09:30:00', 95000 UNION ALL
    SELECT 4, N'CineFlow Quận 1', 0, '15:00:00', 110000 UNION ALL
    SELECT 4, N'CineFlow Quận 1', 1, '17:15:00', 120000 UNION ALL
    SELECT 4, N'CineFlow Ngô Quyền', 0, '10:15:00', 80000 UNION ALL
    SELECT 4, N'CineFlow Ngô Quyền', 1, '15:30:00', 90000 UNION ALL

    -- Phim 5 (Deadpool & Wolverine): Chiếu tại Hoàn Kiếm, Hai Bà Trưng, Landmark 81, Thủ Đức, Hải Châu
    SELECT 5, N'CineFlow Hoàn Kiếm', 0, '13:00:00', 100000 UNION ALL
    SELECT 5, N'CineFlow Hoàn Kiếm', 0, '18:15:00', 120000 UNION ALL
    SELECT 5, N'CineFlow Hoàn Kiếm', 0, '21:30:00', 130000 UNION ALL
    SELECT 5, N'CineFlow Hoàn Kiếm', 1, '19:45:00', 125000 UNION ALL
    SELECT 5, N'CineFlow Hai Bà Trưng', 0, '14:15:00', 110000 UNION ALL
    SELECT 5, N'CineFlow Hai Bà Trưng', 1, '20:30:00', 125000 UNION ALL
    SELECT 5, N'CineFlow Landmark 81', 0, '11:00:00', 120000 UNION ALL
    SELECT 5, N'CineFlow Landmark 81', 0, '16:15:00', 140000 UNION ALL
    SELECT 5, N'CineFlow Landmark 81', 0, '21:00:00', 150000 UNION ALL
    SELECT 5, N'CineFlow Landmark 81', 1, '18:30:00', 145000 UNION ALL
    SELECT 5, N'CineFlow Thủ Đức', 0, '17:00:00', 105000 UNION ALL
    SELECT 5, N'CineFlow Thủ Đức', 1, '19:15:00', 115000 UNION ALL
    SELECT 5, N'CineFlow Hải Châu', 0, '16:30:00', 95000 UNION ALL
    SELECT 5, N'CineFlow Hải Châu', 1, '20:15:00', 105000
)
INSERT INTO Showtimes
(
    MovieID,
    HallID,
    StartTime,
    EndTime,
    CleaningBufferMinutes,
    BaseTicketPrice,
    Status,
    CreatedBy
)
SELECT
    s.MovieID,
    h.HallID,
    CAST(CONCAT(CONVERT(VARCHAR(10), DATEADD(DAY, s.DayOffset, @Today), 120), ' ', s.ShowTime) AS DATETIME2) AS StartTime,
    DATEADD(MINUTE, m.Duration, CAST(CONCAT(CONVERT(VARCHAR(10), DATEADD(DAY, s.DayOffset, @Today), 120), ' ', s.ShowTime) AS DATETIME2)) AS EndTime,
    15,
    s.Price,
    'SCHEDULED',
    ISNULL(@AdminId, 1)
FROM MovieCentreSchedule s
JOIN Movies m ON m.MovieID = s.MovieID
JOIN Centres b ON b.CentreName = s.CentreName
CROSS APPLY (
    SELECT TOP 1 HallID FROM Halls WHERE CentreID = b.CentreID ORDER BY HallID
) h
WHERE NOT EXISTS (
    SELECT 1 FROM Showtimes existing
    WHERE existing.MovieID = s.MovieID
      AND existing.HallID = h.HallID
      AND CAST(existing.StartTime AS DATE) = DATEADD(DAY, s.DayOffset, @Today)
      AND CAST(existing.StartTime AS TIME) = CAST(s.ShowTime AS TIME)
);
GO

SELECT 
    b.City,
    b.CentreName,
    m.Title,
    CONVERT(VARCHAR(10), s.StartTime, 120) AS ShowDate,
    CONVERT(VARCHAR(5), s.StartTime, 108) AS ShowTime,
    s.BaseTicketPrice
FROM Showtimes s
JOIN Movies m ON m.MovieID = s.MovieID
JOIN Halls h ON h.HallID = s.HallID
JOIN Centres b ON b.CentreID = h.CentreID
WHERE s.StartTime >= CAST(GETDATE() AS DATE)
ORDER BY b.City, b.CentreName, s.StartTime;
GO
