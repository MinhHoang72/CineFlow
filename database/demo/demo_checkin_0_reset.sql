/* DEMO SOÁT VÉ - ĐẶT LẠI về trạng thái ban đầu để demo lại từ đầu.
   - Vé TKBK2026101500034491 trở lại VALID, suất chiếu 939 trở về 19/10 22:00-00:18.
   - Xóa nhật ký soát vé phát sinh từ 20/10/2026 của 4 vé dùng để demo. */
USE CineFlowDB;
GO
BEGIN TRANSACTION;

DECLARE @codes TABLE (TicketCode VARCHAR(50));
INSERT INTO @codes VALUES
    ('TKBK2026101500034491'), ('TKBK2026101500044491'),
    ('TKBK2026101500054441'), ('TKBK2026101500231');

DELETE FROM CheckInLogs
WHERE CheckInTime >= '2026-10-20'
  AND TicketID IN (SELECT t.TicketID FROM Tickets t JOIN @codes c ON c.TicketCode = t.TicketCode);

UPDATE Tickets
SET TicketStatus = 'VALID', CheckedInAt = NULL, CheckedInBy = NULL
WHERE TicketCode = 'TKBK2026101500034491';

UPDATE Showtimes
SET StartTime = '2026-10-19 22:00:00', EndTime = '2026-10-20 00:18:00'
WHERE ShowtimeID = 939;

COMMIT TRANSACTION;
GO
