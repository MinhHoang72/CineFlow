/* =====================================================================================
   GỠ CHỨC NĂNG SOÁT VÉ (CHECK-IN) VÀ ĐÁNH GIÁ PHIM KHỎI CSDL ĐANG CHẠY
   - Chỉ cần chạy 1 lần trên CineFlowDB đã tạo từ bản CineFlow.sql cũ.
   - CSDL tạo mới từ CineFlow.sql hiện tại đã không còn các phần này, không cần chạy.
   ===================================================================================== */
USE CineFlowDB;
GO
SET XACT_ABORT ON;
BEGIN TRANSACTION;

-- 1. Trigger tự chuyển đơn sang COMPLETED khi soát đủ vé (JOB-05 đã đảm nhận việc này)
IF OBJECT_ID('dbo.TR_Tickets_CompleteBooking', 'TR') IS NOT NULL
    DROP TRIGGER dbo.TR_Tickets_CompleteBooking;

-- 2. Nhật ký soát vé và đánh giá phim
IF OBJECT_ID('dbo.CheckInLogs', 'U') IS NOT NULL DROP TABLE dbo.CheckInLogs;
IF OBJECT_ID('dbo.MovieRatings', 'U') IS NOT NULL DROP TABLE dbo.MovieRatings;

-- 3. Vé: bỏ trạng thái USED và hai cột soát vé
UPDATE dbo.Tickets SET TicketStatus = 'VALID' WHERE TicketStatus = 'USED';
IF OBJECT_ID('dbo.FK_Tickets_CheckedInBy', 'F') IS NOT NULL
    ALTER TABLE dbo.Tickets DROP CONSTRAINT FK_Tickets_CheckedInBy;
IF COL_LENGTH('dbo.Tickets', 'CheckedInAt') IS NOT NULL ALTER TABLE dbo.Tickets DROP COLUMN CheckedInAt;
IF COL_LENGTH('dbo.Tickets', 'CheckedInBy') IS NOT NULL ALTER TABLE dbo.Tickets DROP COLUMN CheckedInBy;
IF OBJECT_ID('dbo.CK_Tickets_Status', 'C') IS NOT NULL
    ALTER TABLE dbo.Tickets DROP CONSTRAINT CK_Tickets_Status;
ALTER TABLE dbo.Tickets ADD CONSTRAINT CK_Tickets_Status
    CHECK (TicketStatus IN ('VALID', 'CANCELLED', 'EXPIRED'));

-- 4. Vai trò nhân viên soát vé: chuyển thành nhân viên bán vé
UPDATE dbo.Users SET Role = 'TICKET_SALES_STAFF', UpdatedAt = SYSDATETIME()
WHERE Role = 'TICKET_CHECKER_STAFF';
UPDATE dbo.StaffTransferHistory SET OldRole = 'TICKET_SALES_STAFF' WHERE OldRole = 'TICKET_CHECKER_STAFF';
UPDATE dbo.StaffTransferHistory SET NewRole = 'TICKET_SALES_STAFF' WHERE NewRole = 'TICKET_CHECKER_STAFF';
IF OBJECT_ID('dbo.CK_Users_Role', 'C') IS NOT NULL ALTER TABLE dbo.Users DROP CONSTRAINT CK_Users_Role;
ALTER TABLE dbo.Users ADD CONSTRAINT CK_Users_Role
    CHECK (Role IN ('CUSTOMER', 'STAFF', 'MANAGER', 'ADMIN', 'TICKET_SALES_STAFF'));
IF OBJECT_ID('dbo.CK_Users_Staff_Manager_Centre', 'C') IS NOT NULL
    ALTER TABLE dbo.Users DROP CONSTRAINT CK_Users_Staff_Manager_Centre;
ALTER TABLE dbo.Users ADD CONSTRAINT CK_Users_Staff_Manager_Centre
    CHECK (Role NOT IN ('STAFF', 'TICKET_SALES_STAFF') OR CentreID IS NOT NULL);

COMMIT TRANSACTION;
GO
