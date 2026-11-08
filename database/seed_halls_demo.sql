-- ====================================================================
-- CINEFLOW - SCRIPT SEED DỮ LIỆU MANAGER VÀ PHÒNG CHIẾU ĐỂ TEST PHÂN TRANG
-- MẬT KHẨU TẤT CẢ TÀI KHOẢN MANAGER: 123456
-- LƯU Ý: KHÔNG COMMIT FILE NÀY LÊN GIT
-- ====================================================================

-- 1. Cập nhật mật khẩu chuẩn 123456 (BCrypt) cho TẤT CẢ tài khoản Manager
UPDATE Users 
SET Password = '$2a$10$4AeCwdkcGvspefpI3xi8K.F5mdPwC7zGSm3nVb82VfnRgp9BaUrkq' 
WHERE Role = 'MANAGER';

-- Đảm bảo tài khoản manager_caugiay quản lý rạp Cầu Giấy (CentreID = 1)
UPDATE Users 
SET CentreID = 1, Status = 'ACTIVE' 
WHERE Username = 'manager_caugiay';

-- 2. Thêm các phòng chiếu mới cho Cụm rạp Cầu Giấy (CentreID = 1)
-- Hiện tại rạp Cầu Giấy chỉ có 2 phòng (< 6) nên chỉ có 1 trang duy nhất -> PageDTO tự ẩn nút phân trang.
-- Thêm 7 phòng này vào -> Tổng thành 9 phòng (> 6) -> LẬP TỨC HIỆN THANH PHÂN TRANG (Trang 1, 2, «, »)!

IF NOT EXISTS (SELECT 1 FROM Halls WHERE CentreID = 1 AND HallName = N'Phòng 03 (Standard)')
BEGIN
    INSERT INTO Halls (CentreID, HallName, HallType, Capacity, Status, CreatedAt) VALUES 
    (1, N'Phòng 03 (Standard)',   N'2D', 65,  'AVAILABLE', GETDATE()),
    (1, N'Phòng 04 (Digital 3D)',  N'3D', 80,  'AVAILABLE', GETDATE()),
    (1, N'Phòng 05 (VIP Lounge)',  N'2D', 50,  'AVAILABLE', GETDATE()),
    (1, N'Phòng 06 (IMAX Laser)',  N'3D', 120, 'AVAILABLE', GETDATE()),
    (1, N'Phòng 07 (Standard)',   N'2D', 60,  'AVAILABLE', GETDATE()),
    (1, N'Phòng 08 (Gold Class)',  N'2D', 40,  'AVAILABLE', GETDATE()),
    (1, N'Phòng 09 (Tạm ngưng)',   N'2D', 60,  'INACTIVE',  GETDATE());
END
GO

-- 3. Kiểm tra lại kết quả
SELECT HallID, CentreID, HallName, HallType, Capacity, Status 
FROM Halls 
WHERE CentreID = 1 
ORDER BY HallID ASC;
