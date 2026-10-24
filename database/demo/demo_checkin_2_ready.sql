/* DEMO SOÁT VÉ - BƯỚC 2: suất chiếu bắt đầu sau 30 phút (trong khung giờ soát vé)
   Sau khi chạy: quét vé TKBK2026101500034491 sẽ thành công. */
USE CineFlowDB;
GO
UPDATE Showtimes
SET StartTime = DATEADD(MINUTE, 30, GETDATE()),
    EndTime   = DATEADD(MINUTE, 168, GETDATE())
WHERE ShowtimeID = 939;
GO
