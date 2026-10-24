/* DEMO SOÁT VÉ - BƯỚC 1: suất chiếu còn 3 giờ nữa mới chiếu
   Centre 21 (manager, staff_td_02). Vé thử: TKBK2026101500034491 (Lật Mặt 7, ghế A1).
   Sau khi chạy: quét vé này sẽ báo "Chưa đến giờ soát vé". */
USE CineFlowDB;
GO
UPDATE Showtimes
SET StartTime = DATEADD(HOUR, 3, GETDATE()),
    EndTime   = DATEADD(MINUTE, 138, DATEADD(HOUR, 3, GETDATE()))
WHERE ShowtimeID = 939;
GO
