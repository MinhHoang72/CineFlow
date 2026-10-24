/* Run once on an existing CineFlowDB before deploying the renamed application.
   Legacy identifiers only appear here so existing data can be renamed in place.
   Fresh installations use Cine_Flow.sql and do not need this migration.
   Re-running this migration is safe. No business rows are deleted. */
USE CineFlowDB;
GO
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    IF OBJECT_ID(N'dbo.BranchPricing', N'U') IS NOT NULL
    BEGIN
        IF OBJECT_ID(N'dbo.CentrePricing', N'V') IS NOT NULL
            EXEC(N'DROP VIEW dbo.CentrePricing');
        IF OBJECT_ID(N'dbo.CentrePricing', N'U') IS NOT NULL
            THROW 50001, 'Both pricing tables exist; reconcile them before migrating.', 1;
        EXEC sys.sp_rename N'dbo.BranchPricing', N'CentrePricing';
    END;
    IF OBJECT_ID(N'dbo.CentrePricing', N'U') IS NULL
        THROW 50002, 'The expected pricing table does not exist.', 1;

    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.CentrePricing')
               AND name = N'CentreID' AND is_computed = 1)
        EXEC(N'ALTER TABLE dbo.CentrePricing DROP COLUMN CentreID');
    IF COL_LENGTH(N'dbo.CentrePricing', N'BranchPricingID') IS NOT NULL
        EXEC sys.sp_rename N'dbo.CentrePricing.BranchPricingID', N'CentrePricingID', N'COLUMN';
    IF COL_LENGTH(N'dbo.CentrePricing', N'BranchID') IS NOT NULL
        EXEC sys.sp_rename N'dbo.CentrePricing.BranchID', N'CentreID', N'COLUMN';

    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.TimeSlotPricing')
               AND name = N'CentreID' AND is_computed = 1)
        EXEC(N'ALTER TABLE dbo.TimeSlotPricing DROP COLUMN CentreID');
    IF COL_LENGTH(N'dbo.TimeSlotPricing', N'BranchID') IS NOT NULL
        EXEC sys.sp_rename N'dbo.TimeSlotPricing.BranchID', N'CentreID', N'COLUMN';

    DECLARE @oldName NVARCHAR(260), @newName SYSNAME;
    DECLARE constraintsToRename CURSOR LOCAL FAST_FORWARD FOR
        SELECT N'dbo.' + QUOTENAME(name), REPLACE(name, N'Branch', N'Centre')
        FROM sys.objects
        WHERE parent_object_id IN (OBJECT_ID(N'dbo.CentrePricing'), OBJECT_ID(N'dbo.TimeSlotPricing'))
          AND type IN ('C', 'F', 'PK', 'UQ', 'D') AND name LIKE N'%Branch%';
    OPEN constraintsToRename;
    FETCH NEXT FROM constraintsToRename INTO @oldName, @newName;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        EXEC sys.sp_rename @oldName, @newName, N'OBJECT';
        FETCH NEXT FROM constraintsToRename INTO @oldName, @newName;
    END;
    CLOSE constraintsToRename;
    DEALLOCATE constraintsToRename;
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
