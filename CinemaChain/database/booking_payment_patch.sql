/* =========================================================
   PATCH CHO FE-05 (BOOKING) + FE-07 (PAYMENT)
   Chay SAU CineFlowDB.sql. Chay lai nhieu lan van an toan.
   ========================================================= */
USE CineFlowDB;
GO
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO

/* 1. Chong dat trung ghe: them ShowtimeID vao BookingSeats + unique index co dieu kien */
IF COL_LENGTH('BookingSeats', 'ShowtimeID') IS NULL
    ALTER TABLE BookingSeats ADD ShowtimeID INT NULL;
GO

UPDATE bs SET bs.ShowtimeID = b.ShowtimeID
FROM BookingSeats bs JOIN Bookings b ON b.BookingID = bs.BookingID
WHERE bs.ShowtimeID IS NULL;
GO

IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('BookingSeats') AND name = 'ShowtimeID' AND is_nullable = 1)
    ALTER TABLE BookingSeats ALTER COLUMN ShowtimeID INT NOT NULL;
GO

IF OBJECT_ID('FK_BookingSeats_Showtime') IS NULL
    ALTER TABLE BookingSeats ADD CONSTRAINT FK_BookingSeats_Showtime
        FOREIGN KEY (ShowtimeID) REFERENCES Showtimes(ShowtimeID);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_BookingSeats_ActiveSeat')
    CREATE UNIQUE INDEX UX_BookingSeats_ActiveSeat
        ON BookingSeats(ShowtimeID, SeatID)
        WHERE SeatStatus IN ('HELD', 'CONFIRMED');
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_BookingSeats_BookingID')
    CREATE INDEX IX_BookingSeats_BookingID ON BookingSeats(BookingID);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Payments_BookingID')
    CREATE INDEX IX_Payments_BookingID ON Payments(BookingID);
GO

/* 2. Suat chieu demo cho 7 ngay toi (tinh tu ngay chay script).
      Moi phong 4 suat: 09h, 13h, 17h, 21h; phim xoay vong trong cac phim NOW_SHOWING cua chi nhanh.
      Bo qua suat da qua gio hoac trung lich voi suat co san trong cung phong. */
;WITH Days AS (
    SELECT d FROM (VALUES (0),(1),(2),(3),(4),(5),(6)) x(d)
), Slots AS (
    SELECT s, hr FROM (VALUES (1, 9), (2, 13), (3, 17), (4, 21)) x(s, hr)
), CentreNowShowing AS (
    SELECT bm.CentreID, m.MovieID, m.Duration,
           ROW_NUMBER() OVER (PARTITION BY bm.CentreID ORDER BY m.MovieID) AS rn,
           COUNT(*) OVER (PARTITION BY bm.CentreID) AS cnt
    FROM CentreMovies bm
    JOIN Movies m ON m.MovieID = bm.MovieID
    WHERE bm.IsActive = 1 AND m.Status = 'NOW_SHOWING'
), Candidates AS (
    SELECT h.HallID, bn.MovieID,
           DATEADD(HOUR, sl.hr, CAST(DATEADD(DAY, dy.d, CAST(SYSDATETIME() AS DATE)) AS DATETIME2)) AS StartTime,
           bn.Duration,
           CASE WHEN h.HallType = '3D' THEN 100000 ELSE 80000 END AS Price
    FROM Halls h
    JOIN Centres b ON b.CentreID = h.CentreID AND b.Status = 'ACTIVE'
    CROSS JOIN Days dy
    CROSS JOIN Slots sl
    JOIN CentreNowShowing bn ON bn.CentreID = h.CentreID
                            AND bn.rn = ((dy.d + sl.s + h.HallID) % bn.cnt) + 1
    WHERE h.Status = 'AVAILABLE'
)
INSERT INTO Showtimes (MovieID, HallID, StartTime, EndTime, CleaningBufferMinutes, BaseTicketPrice, Status, CreatedBy)
SELECT c.MovieID, c.HallID, c.StartTime, DATEADD(MINUTE, c.Duration, c.StartTime), 15, c.Price, 'SCHEDULED',
       (SELECT UserID FROM Users WHERE Account = 'manager')
FROM Candidates c
WHERE c.StartTime > SYSDATETIME()
  AND NOT EXISTS (
      SELECT 1 FROM Showtimes s
      WHERE s.HallID = c.HallID
        AND s.Status <> 'CANCELLED'
        AND s.StartTime < DATEADD(MINUTE, c.Duration + 15, c.StartTime)
        AND c.StartTime < DATEADD(MINUTE, s.CleaningBufferMinutes, s.EndTime)
  );
GO

SELECT COUNT(*) AS FutureShowtimes FROM Showtimes WHERE StartTime > SYSDATETIME() AND Status = 'SCHEDULED';
GO

/* 3. Tien khach dua khi thanh toan tien mat tai quay (de in hoa don, tinh tien thoi) */
IF COL_LENGTH('Payments', 'CashReceived') IS NULL
    ALTER TABLE Payments ADD CashReceived DECIMAL(12,2) NULL;
GO

/* 4. Tai khoan khach thu hai de test 2 nguoi dat trung ghe */
IF NOT EXISTS (SELECT 1 FROM Users WHERE Account = 'customer1')
    INSERT INTO Users (FullName, Account, Email, Password, Phone, Role, Status)
    VALUES (N'Trần Thị Customer 1', 'customer1', 'customer1@gmail.com', '123456', '0900000011', 'CUSTOMER', 'ACTIVE');
GO
