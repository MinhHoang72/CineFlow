/* Repair overlapping showtimes without deleting shows or changing halls/bookings.
   Run with @Apply = 0 first. Set to 1 to apply the reviewed plan.
   Every changed row is backed up under one RepairID inside the same transaction.
   All non-cancelled states are considered, including historical seed data.
   A show keeps its original start unless the previous show + cleaning requires a delay.
*/
SET NOCOUNT ON;
SET XACT_ABORT ON;
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
DECLARE @Apply bit = 0;
DECLARE @RepairID uniqueidentifier = NEWID();
DECLARE @Now datetime2 = SYSDATETIME();
BEGIN TRY
    BEGIN TRANSACTION;
    DECLARE @Plan TABLE (
        ShowtimeID int PRIMARY KEY, HallID int,
        OldStart datetime2, OldEnd datetime2, NewStart datetime2, NewEnd datetime2,
        OldStatus varchar(20), NewStatus varchar(20), OldUpdatedAt datetime2
    );
    DECLARE @ID int, @Hall int, @PreviousHall int = NULL, @Buffer int;
    DECLARE @Start datetime2, @End datetime2, @NewStart datetime2, @NewEnd datetime2;
    DECLARE @BusyUntil datetime2 = NULL, @Status varchar(20), @UpdatedAt datetime2;
    DECLARE schedule_cursor CURSOR LOCAL FAST_FORWARD FOR
        SELECT ShowtimeID, HallID, StartTime, EndTime, CleaningBufferMinutes, Status, UpdatedAt
        FROM dbo.Showtimes WITH (TABLOCKX, HOLDLOCK)
        WHERE Status <> 'CANCELLED'
        ORDER BY HallID, StartTime, ShowtimeID;
    OPEN schedule_cursor;
    FETCH NEXT FROM schedule_cursor INTO @ID,@Hall,@Start,@End,@Buffer,@Status,@UpdatedAt;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        IF @PreviousHall IS NULL OR @PreviousHall <> @Hall SET @BusyUntil = NULL;
        SET @NewStart = CASE WHEN @BusyUntil > @Start THEN @BusyUntil ELSE @Start END;
        SET @NewEnd = DATEADD(SECOND, DATEDIFF(SECOND,@Start,@End), @NewStart);
        IF @NewStart <> @Start
            INSERT INTO @Plan VALUES (@ID,@Hall,@Start,@End,@NewStart,@NewEnd,@Status,
                CASE WHEN @NewEnd <= @Now THEN 'COMPLETED' WHEN @NewStart <= @Now THEN 'ONGOING' ELSE 'SCHEDULED' END,@UpdatedAt);
        SET @BusyUntil = DATEADD(MINUTE,ISNULL(@Buffer,0),@NewEnd);
        SET @PreviousHall = @Hall;
        FETCH NEXT FROM schedule_cursor INTO @ID,@Hall,@Start,@End,@Buffer,@Status,@UpdatedAt;
    END;
    CLOSE schedule_cursor;
    DEALLOCATE schedule_cursor;

    SELECT @RepairID AS RepairID, COUNT(*) AS ChangedShows,
        SUM(CASE WHEN CAST(OldStart AS date) <> CAST(NewStart AS date) THEN 1 ELSE 0 END) AS ChangedDate,
        MAX(DATEDIFF(MINUTE,OldStart,NewStart)) AS MaxDelayMinutes,
        SUM(CASE WHEN EXISTS_BOOKING.ShowtimeID IS NOT NULL THEN 1 ELSE 0 END) AS BookedShowsChanged
    FROM @Plan p OUTER APPLY (SELECT TOP 1 b.ShowtimeID FROM dbo.Bookings b WHERE b.ShowtimeID=p.ShowtimeID) EXISTS_BOOKING;
    SELECT TOP 12 ShowtimeID,HallID,OldStart,NewStart,OldEnd,NewEnd FROM @Plan ORDER BY DATEDIFF(MINUTE,OldStart,NewStart) DESC;

    IF @Apply = 1
    BEGIN
        IF OBJECT_ID('dbo.ShowtimeConflictRepairBackup','U') IS NULL
            CREATE TABLE dbo.ShowtimeConflictRepairBackup (
                RepairID uniqueidentifier NOT NULL, RepairedAt datetime2 NOT NULL,
                ShowtimeID int NOT NULL, HallID int NOT NULL,
                OldStart datetime2 NOT NULL, OldEnd datetime2 NOT NULL,
                NewStart datetime2 NOT NULL, NewEnd datetime2 NOT NULL,
                OldStatus varchar(20) NOT NULL, NewStatus varchar(20) NOT NULL,
                OldUpdatedAt datetime2 NULL,
                CONSTRAINT PK_ShowtimeConflictRepairBackup PRIMARY KEY (RepairID,ShowtimeID)
            );
        INSERT INTO dbo.ShowtimeConflictRepairBackup
            SELECT @RepairID,@Now,ShowtimeID,HallID,OldStart,OldEnd,NewStart,NewEnd,OldStatus,NewStatus,OldUpdatedAt FROM @Plan;
        UPDATE s SET StartTime=p.NewStart,EndTime=p.NewEnd,Status=p.NewStatus,UpdatedAt=@Now
        FROM dbo.Showtimes s JOIN @Plan p ON p.ShowtimeID=s.ShowtimeID;
        IF EXISTS (
            SELECT 1 FROM dbo.Showtimes a JOIN dbo.Showtimes b
                ON a.HallID=b.HallID AND a.ShowtimeID<b.ShowtimeID
                AND a.StartTime<DATEADD(MINUTE,ISNULL(b.CleaningBufferMinutes,0),b.EndTime)
                AND b.StartTime<DATEADD(MINUTE,ISNULL(a.CleaningBufferMinutes,0),a.EndTime)
            WHERE a.Status<>'CANCELLED' AND b.Status<>'CANCELLED'
        ) THROW 51000, 'Conflicts remain. Entire repair rolled back.', 1;
        COMMIT;
        SELECT 'APPLIED; zero conflicts' AS Result,@RepairID AS RepairID;
    END
    ELSE
    BEGIN
        ROLLBACK;
        SELECT 'PREVIEW ONLY; no data changed' AS Result;
    END;
END TRY
BEGIN CATCH
    IF CURSOR_STATUS('local','schedule_cursor') >= 0 CLOSE schedule_cursor;
    IF CURSOR_STATUS('local','schedule_cursor') >= -1 DEALLOCATE schedule_cursor;
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
