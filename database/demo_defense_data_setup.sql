-- =========================================================================================
-- FILE SQL CẤU HÌNH DỮ LIỆU DEMO BẢO VỆ ĐỒ ÁN CINEFLOW (DEMO DEFENSE DATA SETUP)
-- Hướng dẫn: Mở file này trong SSMS trên máy bất kỳ -> Chọn Database CineFlowDB -> Bấm F5 (Execute)
-- =========================================================================================

USE CineFlowDB;
GO

PRINT N'>>> BẮT ĐẦU CẤU HÌNH DỮ LIỆU TEST BẢO VỆ...';

-- =========================================================================================
-- PHẦN 1: CẤU HÌNH TEST CASE PHÒNG CHIẾU (CHẶN TẮT PHÒNG 01 DO CÓ VÉ TƯƠNG LAI)
-- =========================================================================================
PRINT N'1. Đang cấu hình test case Phòng chiếu 01 (Cầu Giấy)...';

-- Đảm bảo bảng BookingSeats có cột ShowtimeID (tránh lỗi 500 khi vào sơ đồ ghế)
IF COL_LENGTH('BookingSeats', 'ShowtimeID') IS NULL
BEGIN
    ALTER TABLE BookingSeats ADD ShowtimeID INT NULL;
    EXEC('UPDATE bs SET bs.ShowtimeID = b.ShowtimeID FROM BookingSeats bs JOIN Bookings b ON b.BookingID = bs.BookingID');
END

-- 1.1 Gán tài khoản 'manager' về Cụm rạp Cầu Giấy (CentreID = 1) để đăng nhập tài khoản nào cũng vào đúng Cầu Giấy
UPDATE Users SET CentreID = 1 WHERE Account = 'manager';

-- 1.2 Đảm bảo Cụm rạp Cầu Giấy và các phòng test đang ở trạng thái Hoạt động
UPDATE Centres SET Status = 'ACTIVE' WHERE CentreID = 1;
UPDATE Halls SET Status = 'AVAILABLE' WHERE HallID IN (1, 2, 61, 62);

-- 1.2 Cập nhật suất chiếu của Phòng 01 (HallID = 1) và Phòng 2D 01 (HallID = 61) sang tương lai (+7 ngày)
DECLARE @ShowtimeID_Hall1 INT;
SELECT TOP 1 @ShowtimeID_Hall1 = ShowtimeID FROM Showtimes WHERE HallID = 1;

IF @ShowtimeID_Hall1 IS NOT NULL
BEGIN
    UPDATE Showtimes 
    SET StartTime = DATEADD(DAY, 7, GETDATE()), 
        EndTime = DATEADD(DAY, 7, DATEADD(HOUR, 2, GETDATE())),
        Status = 'SCHEDULED'
    WHERE ShowtimeID = @ShowtimeID_Hall1;

    IF NOT EXISTS (SELECT 1 FROM Bookings WHERE BookingCode = 'BK_TEST_HALL1')
    BEGIN
        DECLARE @CustID INT;
        SELECT TOP 1 @CustID = UserID FROM Users WHERE Role = 'CUSTOMER';
        IF @CustID IS NULL SET @CustID = 1;

        INSERT INTO Bookings (BookingCode, UserID, ShowtimeID, BookingType, BookingStatus, SubTotal, DiscountAmount, TotalAmount, CreatedAt, UpdatedAt)
        VALUES ('BK_TEST_HALL1', @CustID, @ShowtimeID_Hall1, 'ONLINE', 'CONFIRMED', 100000, 0, 100000, GETDATE(), GETDATE());
    END
    ELSE
    BEGIN
        UPDATE Bookings 
        SET ShowtimeID = @ShowtimeID_Hall1, BookingStatus = 'CONFIRMED' 
        WHERE BookingCode = 'BK_TEST_HALL1';
    END
    PRINT N'   => Đã thiết lập xong: Phòng 01 có suất chiếu tương lai + vé đặt CONFIRMED!';
END

-- 1.3 Cấu hình thêm cho Phòng 2D 01 (HallID = 61) để bấm phòng nào cũng bị chặn đúng luật!
DECLARE @ShowtimeID_Hall61 INT;
SELECT TOP 1 @ShowtimeID_Hall61 = ShowtimeID FROM Showtimes WHERE HallID = 61 AND StartTime > GETDATE() AND Status = 'SCHEDULED';
IF @ShowtimeID_Hall61 IS NULL
BEGIN
    SELECT TOP 1 @ShowtimeID_Hall61 = ShowtimeID FROM Showtimes WHERE HallID = 61;
    IF @ShowtimeID_Hall61 IS NOT NULL
    BEGIN
        UPDATE Showtimes 
        SET StartTime = DATEADD(DAY, 7, GETDATE()), 
            EndTime = DATEADD(DAY, 7, DATEADD(HOUR, 2, GETDATE())),
            Status = 'SCHEDULED'
        WHERE ShowtimeID = @ShowtimeID_Hall61;
    END
END

IF @ShowtimeID_Hall61 IS NOT NULL
BEGIN
    IF NOT EXISTS (SELECT 1 FROM Bookings WHERE BookingCode = 'BK_TEST_HALL61')
    BEGIN
        INSERT INTO Bookings (BookingCode, UserID, ShowtimeID, BookingType, BookingStatus, SubTotal, DiscountAmount, TotalAmount, CreatedAt, UpdatedAt)
        VALUES ('BK_TEST_HALL61', 1, @ShowtimeID_Hall61, 'ONLINE', 'CONFIRMED', 100000, 0, 100000, GETDATE(), GETDATE());
    END
    ELSE
    BEGIN
        UPDATE Bookings SET ShowtimeID = @ShowtimeID_Hall61, BookingStatus = 'CONFIRMED' WHERE BookingCode = 'BK_TEST_HALL61';
    END
    PRINT N'   => Đã thiết lập xong: Phòng 2D 01 (HallID 61) có suất chiếu tương lai + vé đặt CONFIRMED!';
END


-- =========================================================================================
-- PHẦN 2: CẤU HÌNH 20 VOUCHER CHO MANAGER CẦU GIẤY (ĐẦY ĐỦ ẢNH + REALISTIC NAMES)
-- =========================================================================================
PRINT N'2. Đang cấu hình 20 Voucher cho Manager Cầu Giấy (CentreID = 1)...';

-- Voucher 335: Dùng để test SỬA MÃ (UsedCount = 0)
UPDATE Vouchers SET VoucherCode = 'CGY_VIPMEMBER', VoucherName = N'Ưu Đãi Thành Viên VIP Cầu Giấy', ImageURL = '/Cine_Flow/uploads/poster/poster-dune-2.jpg', UsedCount = 0 WHERE VoucherID = 335;

-- Voucher 334: Dùng để test KHÓA MÃ CODE READONLY (UsedCount = 45)
UPDATE Vouchers SET VoucherCode = 'CGY_STUDENT', VoucherName = N'HSSV Siêu Tiết Kiệm - Giảm 20%', ImageURL = '/Cine_Flow/uploads/poster/poster-inside-out-2.jpg', UsedCount = 45 WHERE VoucherID = 334;

-- Các voucher còn lại trang 1
UPDATE Vouchers SET VoucherCode = 'CGY_SWEETPAIR', VoucherName = N'Combo Hẹn Hò Sweetbox - Giảm 25%', ImageURL = '/Cine_Flow/uploads/poster/poster-mai.jpg', UsedCount = 18 WHERE VoucherID = 333;
UPDATE Vouchers SET VoucherCode = 'CGY_WEEKEND_BOOM', VoucherName = N'Cuối Tuần Bùng Nổ - Giảm 40K', ImageURL = '/Cine_Flow/uploads/poster/poster-deadpool-wolverine.jpg', UsedCount = 28 WHERE VoucherID = 332;
UPDATE Vouchers SET VoucherCode = 'CGY_HAPPY_WED', VoucherName = N'Thứ Tư Vui Vẻ - Giảm 30%', ImageURL = '/Cine_Flow/uploads/poster/poster-kung-fu-panda-4.jpg', UsedCount = 35 WHERE VoucherID = 331;
UPDATE Vouchers SET VoucherCode = 'CGY_NIGHT_OWL', VoucherName = N'Cú Đêm Mê Phim - Giảm 35K', ImageURL = '/Cine_Flow/uploads/poster/poster-avatar-2.jpg', UsedCount = 15 WHERE VoucherID = 330;
UPDATE Vouchers SET VoucherCode = 'CGY_EARLY_BIRD', VoucherName = N'Đặt Vé Sớm - Giảm Liền 30K', ImageURL = '/Cine_Flow/uploads/poster/poster-interstellar.jpg', UsedCount = 22 WHERE VoucherID = 329;
UPDATE Vouchers SET VoucherCode = 'CGY_FAMILY_DAY', VoucherName = N'Gia Đình Gắn Kết - Giảm 30%', ImageURL = '/Cine_Flow/uploads/poster/poster-despicable-me-4.jpg', UsedCount = 8 WHERE VoucherID = 328;
UPDATE Vouchers SET VoucherCode = 'CGY_POPCORN_COMBO', VoucherName = N'Đại Tiệc Bắp Nước - Giảm 20K', ImageURL = '/Cine_Flow/uploads/poster/poster-transformers-one.jpg', UsedCount = 40 WHERE VoucherID = 327;
UPDATE Vouchers SET VoucherCode = 'CGY_NEWBIE_WELCOME', VoucherName = N'Chào Bạn Mới - Giảm 50K', ImageURL = '/Cine_Flow/uploads/poster/poster-oppenheimer.jpg', UsedCount = 50 WHERE VoucherID = 326;

-- Các voucher trang 2 của Cầu Giấy (325 -> 316)
UPDATE Vouchers SET VoucherCode = 'CGY_MATINEE', VoucherName = N'Suất Chiếu Sớm Matinee - Giảm 20K', ImageURL = '/Cine_Flow/uploads/poster/poster-godzilla-kong.jpg' WHERE VoucherID = 325;
UPDATE Vouchers SET VoucherCode = 'CGY_FAMILY_COMBO', VoucherName = N'Combo Gia Đình Cuối Tuần - Giảm 25%', ImageURL = '/Cine_Flow/uploads/poster/poster-lat-mat-7.jpg' WHERE VoucherID = 324;
UPDATE Vouchers SET VoucherCode = 'CGY_LATE_NIGHT', VoucherName = N'Suất Chiếu Đêm Khuya - Giảm 30K', ImageURL = '/Cine_Flow/uploads/poster/poster-inception.jpg' WHERE VoucherID = 323;
UPDATE Vouchers SET VoucherCode = 'CGY_VIET_FILM', VoucherName = N'Tôn Vinh Phim Việt - Giảm 20%', ImageURL = '/Cine_Flow/uploads/poster/poster-bo-gia.jpg' WHERE VoucherID = 322;
UPDATE Vouchers SET VoucherCode = 'CGY_WAR_EPIC', VoucherName = N'Phim Lịch Sử Hào Hùng - Giảm 35K', ImageURL = '/Cine_Flow/uploads/poster/poster-dao-pho-va-piano.jpg' WHERE VoucherID = 321;
UPDATE Vouchers SET VoucherCode = 'CGY_SOUTHERN_HERITAGE', VoucherName = N'Hành Trình Đất Phương Nam - Giảm 25K', ImageURL = '/Cine_Flow/uploads/poster/poster-dat-rung-phuong-nam.jpg' WHERE VoucherID = 320;
UPDATE Vouchers SET VoucherCode = 'CGY_HISTORICAL_LEGEND', VoucherName = N'Ký Ức Lịch Sử Địa Đạo - Giảm 30%', ImageURL = '/Cine_Flow/uploads/poster/poster-dia-dao.jpg' WHERE VoucherID = 319;
UPDATE Vouchers SET VoucherCode = 'CGY_MUSIC_PIANO', VoucherName = N'Giai Điệu Nghệ Thuật - Giảm 40K', ImageURL = '/Cine_Flow/uploads/poster/poster-the-pianist.jpg' WHERE VoucherID = 318;
UPDATE Vouchers SET VoucherCode = 'CGY_SCI_FI_FEST', VoucherName = N'Lễ Hội Khoa Học Viễn Tưởng - Giảm 20%', ImageURL = '/Cine_Flow/uploads/poster/poster-dune-2.jpg' WHERE VoucherID = 317;
UPDATE Vouchers SET VoucherCode = 'CGY_ANIMATION_FUN', VoucherName = N'Thế Giới Hoạt Hình - Giảm 25K', ImageURL = '/Cine_Flow/uploads/poster/poster-despicable-me-4.jpg' WHERE VoucherID = 316;


-- =========================================================================================
-- PHẦN 3: CẤU HÌNH 20 VOUCHER CHO ADMIN (TOÀN CHUỖI & QUẬN 1)
-- =========================================================================================
PRINT N'3. Đang cấu hình 20 Voucher cho Admin...';

UPDATE Vouchers SET VoucherCode = 'NIGHT_OWL', VoucherName = N'Cú Đêm Mê Phim - Giảm 35K', ImageURL = '/Cine_Flow/uploads/poster/poster-avatar-2.jpg' WHERE VoucherID = 435;
UPDATE Vouchers SET VoucherCode = 'SWEET_COUPLE', VoucherName = N'Combo Hẹn Hò - Giảm 25%', ImageURL = '/Cine_Flow/uploads/poster/poster-mai.jpg' WHERE VoucherID = 434;
UPDATE Vouchers SET VoucherCode = 'WEEKEND_BOOM', VoucherName = N'Cuối Tuần Bom Tấn - Giảm 40K', ImageURL = '/Cine_Flow/uploads/poster/poster-deadpool-wolverine.jpg' WHERE VoucherID = 433;
UPDATE Vouchers SET VoucherCode = 'STUDENT_POWER', VoucherName = N'HSSV Siêu Ưu Đãi - Giảm 20%', ImageURL = '/Cine_Flow/uploads/poster/poster-inside-out-2.jpg' WHERE VoucherID = 432;
UPDATE Vouchers SET VoucherCode = 'HAPPY_WEDNESDAY', VoucherName = N'Thứ Tư Vui Vẻ - Giảm 30%', ImageURL = '/Cine_Flow/uploads/poster/poster-kung-fu-panda-4.jpg' WHERE VoucherID = 431;
UPDATE Vouchers SET VoucherCode = 'CINEFLOW_NEWBIE', VoucherName = N'Chào Bạn Mới - Giảm 50K', ImageURL = '/Cine_Flow/uploads/poster/poster-oppenheimer.jpg' WHERE VoucherID = 430;
UPDATE Vouchers SET VoucherCode = 'TET2026_VIP', VoucherName = N'Giảm 20% Khai Xuân 2026', ImageURL = '/Cine_Flow/uploads/poster/poster-lat-mat-7.jpg' WHERE VoucherID = 429;
UPDATE Vouchers SET VoucherCode = 'SUMMER_BLOCKBUSTER', VoucherName = N'Bom Tấn Mùa Hè - Giảm 30K', ImageURL = '/Cine_Flow/uploads/poster/poster-transformers-one.jpg' WHERE VoucherID = 428;
UPDATE Vouchers SET VoucherCode = 'HOALAC_CAMPUS_VIP', VoucherName = N'Đặc Quyền Sinh Viên Hòa Lạc - Giảm 25%', ImageURL = '/Cine_Flow/uploads/poster/poster-interstellar.jpg' WHERE VoucherID = 427;
UPDATE Vouchers SET VoucherCode = 'HANOI_CINEMA_PASS', VoucherName = N'Thẻ Phim Thủ Đô 2026 - Giảm 35K', ImageURL = '/Cine_Flow/uploads/poster/poster-dao-pho-va-piano.jpg' WHERE VoucherID = 426;

UPDATE Vouchers SET VoucherCode = 'Q1_PREMIERE_PASS', VoucherName = N'Suất Chiếu Đặc Biệt Quận 1 - Giảm 30%', ImageURL = '/Cine_Flow/uploads/poster/poster-dune-2.jpg' WHERE VoucherID = 425;
UPDATE Vouchers SET VoucherCode = 'Q1_MONSTER_MAYHEM', VoucherName = N'Đại Chiến Quái Thú - Giảm 40K', ImageURL = '/Cine_Flow/uploads/poster/poster-godzilla-kong.jpg' WHERE VoucherID = 424;
UPDATE Vouchers SET VoucherCode = 'Q1_MIND_BENDER', VoucherName = N'Đỉnh Cao Trí Tuệ Điện Ảnh - Giảm 25%', ImageURL = '/Cine_Flow/uploads/poster/poster-inception.jpg' WHERE VoucherID = 423;
UPDATE Vouchers SET VoucherCode = 'Q1_SOUTHERN_SOUL', VoucherName = N'Hương Sắc Miền Nam - Giảm 30K', ImageURL = '/Cine_Flow/uploads/poster/poster-dat-rung-phuong-nam.jpg' WHERE VoucherID = 422;
UPDATE Vouchers SET VoucherCode = 'Q1_VIET_FAMILY', VoucherName = N'Tình Thân Gia Đình - Giảm 20%', ImageURL = '/Cine_Flow/uploads/poster/poster-bo-gia.jpg' WHERE VoucherID = 421;
UPDATE Vouchers SET VoucherCode = 'Q1_HISTORIC_HERO', VoucherName = N'Bản Hùng Ca Lịch Sử - Giảm 35K', ImageURL = '/Cine_Flow/uploads/poster/poster-dia-dao.jpg' WHERE VoucherID = 420;
UPDATE Vouchers SET VoucherCode = 'Q1_CLASSIC_MELODY', VoucherName = N'Giai Điệu Nghệ Thuật - Giảm 30%', ImageURL = '/Cine_Flow/uploads/poster/poster-the-pianist.jpg' WHERE VoucherID = 419;
UPDATE Vouchers SET VoucherCode = 'Q1_MINION_MADNESS', VoucherName = N'Biệt Đội Kẻ Trộm Mặt Trăng - Giảm 25K', ImageURL = '/Cine_Flow/uploads/poster/poster-despicable-me-4.jpg' WHERE VoucherID = 418;
UPDATE Vouchers SET VoucherCode = 'Q1_AVATAR_EXP', VoucherName = N'Trải Nghiệm Thế Giới Pandora - Giảm 50K', ImageURL = '/Cine_Flow/uploads/poster/poster-avatar-2.jpg' WHERE VoucherID = 417;
UPDATE Vouchers SET VoucherCode = 'Q1_SUPERHERO_NIGHT', VoucherName = N'Đêm Hội Siêu Anh Hùng - Giảm 20%', ImageURL = '/Cine_Flow/uploads/poster/poster-deadpool-wolverine.jpg' WHERE VoucherID = 416;

PRINT N'>>> HOÀN TẤT THIẾT LẬP DỮ LIỆU DEMO BẢO VỆ 100%! BẠN CÓ THỂ BẮT ĐẦU DEMO.';
GO
