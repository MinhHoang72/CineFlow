/* =========================================================
   PATCH CHO FE-05 (BOOKING) + FE-07 (PAYMENT)
   Chay SAU Cine_Flow.sql cua nhom. Chay lai nhieu lan van an toan.
   Khong sua bang/du lieu cua nhom, chi them:
     1. BookingSeats.ShowtimeID + unique index chong dat trung ghe
     2. Payments.CashReceived (tien khach dua khi tra tien mat tai quay)
     3. Tai khoan customer1 de test 2 khach dat trung ghe
   Gia ve lay tu BranchPricing + TimeSlotPricing + Surcharges cua nhom (khong can bo sung).
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

/* 2. Tien khach dua khi thanh toan tien mat tai quay (de in hoa don, tinh tien thoi) */
IF COL_LENGTH('Payments', 'CashReceived') IS NULL
    ALTER TABLE Payments ADD CashReceived DECIMAL(12,2) NULL;
GO

/* 3. Tai khoan khach thu hai de test 2 nguoi dat trung ghe */
IF NOT EXISTS (SELECT 1 FROM Users WHERE Account = 'customer1')
    INSERT INTO Users (FullName, Account, Email, Password, Phone, Role, Status, EmailVerified)
    VALUES (N'Trần Thị Customer 1', 'customer1', 'customer1@gmail.com', '123456', '0900000011', 'CUSTOMER', 'ACTIVE', 1);
ELSE
    UPDATE Users SET EmailVerified = 1 WHERE Account = 'customer1';
GO
