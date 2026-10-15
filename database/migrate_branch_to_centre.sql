USE CineFlowDB;
GO

/* ==========================================================================
   CINEFLOW DATABASE MIGRATION: RENAME BRANCH -> CENTRE
   Đổi toàn bộ tên bảng, cột và khóa ngoại từ Branch sang Centre
   ========================================================================== */

-- 1. XÓA CÁC KHÓA NGOẠI LIÊN QUAN TRƯỚC KHI ĐỔI TÊN
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Users_Branch')
    ALTER TABLE Users DROP CONSTRAINT FK_Users_Branch;
GO
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Halls_Branches')
    ALTER TABLE Halls DROP CONSTRAINT FK_Halls_Branches;
GO
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_BranchMovies_Branch')
    ALTER TABLE BranchMovies DROP CONSTRAINT FK_BranchMovies_Branch;
GO
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_MovieDistributionExceptions_Branches')
    ALTER TABLE MovieDistributionExceptions DROP CONSTRAINT FK_MovieDistributionExceptions_Branches;
GO
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Inventory_Branch')
    ALTER TABLE BranchFnbInventory DROP CONSTRAINT FK_Inventory_Branch;
GO
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_FnbOrders_Branch')
    ALTER TABLE FnbOrders DROP CONSTRAINT FK_FnbOrders_Branch;
GO
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Transfer_FromBranch')
    ALTER TABLE StaffTransferHistory DROP CONSTRAINT FK_Transfer_FromBranch;
GO
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Transfer_ToBranch')
    ALTER TABLE StaffTransferHistory DROP CONSTRAINT FK_Transfer_ToBranch;
GO
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_BMA_Branch')
    ALTER TABLE BranchManagerAssignments DROP CONSTRAINT FK_BMA_Branch;
GO

-- 2. ĐỔI TÊN BẢNG VÀ CỘT TRONG BẢNG BRANCHES -> CENTRES
IF OBJECT_ID('Branches', 'U') IS NOT NULL AND OBJECT_ID('Centres', 'U') IS NULL
BEGIN
    EXEC sp_rename 'Branches', 'Centres';
END
GO

IF COL_LENGTH('Centres', 'BranchID') IS NOT NULL
BEGIN
    EXEC sp_rename 'Centres.BranchID', 'CentreID', 'COLUMN';
END
GO

IF COL_LENGTH('Centres', 'BranchName') IS NOT NULL
BEGIN
    EXEC sp_rename 'Centres.BranchName', 'CentreName', 'COLUMN';
END
GO

-- 3. ĐỔI TÊN CỘT TRONG CÁC BẢNG LIÊN QUAN
IF COL_LENGTH('Users', 'BranchID') IS NOT NULL
BEGIN
    EXEC sp_rename 'Users.BranchID', 'CentreID', 'COLUMN';
END
GO

IF COL_LENGTH('Halls', 'BranchID') IS NOT NULL
BEGIN
    EXEC sp_rename 'Halls.BranchID', 'CentreID', 'COLUMN';
END
GO

-- Đổi tên bảng BranchMovies -> CentreMovies
IF OBJECT_ID('BranchMovies', 'U') IS NOT NULL AND OBJECT_ID('CentreMovies', 'U') IS NULL
BEGIN
    EXEC sp_rename 'BranchMovies', 'CentreMovies';
END
GO
IF COL_LENGTH('CentreMovies', 'BranchID') IS NOT NULL
BEGIN
    EXEC sp_rename 'CentreMovies.BranchID', 'CentreID', 'COLUMN';
END
GO

-- Đổi tên trong MovieDistributionExceptions
IF COL_LENGTH('MovieDistributionExceptions', 'BranchID') IS NOT NULL
BEGIN
    EXEC sp_rename 'MovieDistributionExceptions.BranchID', 'CentreID', 'COLUMN';
END
GO

-- Đổi tên bảng BranchFnbInventory -> CentreFnbInventory
IF OBJECT_ID('BranchFnbInventory', 'U') IS NOT NULL AND OBJECT_ID('CentreFnbInventory', 'U') IS NULL
BEGIN
    EXEC sp_rename 'BranchFnbInventory', 'CentreFnbInventory';
END
GO
IF COL_LENGTH('CentreFnbInventory', 'BranchID') IS NOT NULL
BEGIN
    EXEC sp_rename 'CentreFnbInventory.BranchID', 'CentreID', 'COLUMN';
END
GO

-- Đổi tên trong FnbOrders
IF COL_LENGTH('FnbOrders', 'BranchID') IS NOT NULL
BEGIN
    EXEC sp_rename 'FnbOrders.BranchID', 'CentreID', 'COLUMN';
END
GO

-- Đổi tên trong StaffTransferHistory
IF COL_LENGTH('StaffTransferHistory', 'FromBranchID') IS NOT NULL
BEGIN
    EXEC sp_rename 'StaffTransferHistory.FromBranchID', 'FromCentreID', 'COLUMN';
END
GO
IF COL_LENGTH('StaffTransferHistory', 'ToBranchID') IS NOT NULL
BEGIN
    EXEC sp_rename 'StaffTransferHistory.ToBranchID', 'ToCentreID', 'COLUMN';
END
GO

-- Đổi tên bảng BranchManagerAssignments -> CentreManagerAssignments
IF OBJECT_ID('BranchManagerAssignments', 'U') IS NOT NULL AND OBJECT_ID('CentreManagerAssignments', 'U') IS NULL
BEGIN
    EXEC sp_rename 'BranchManagerAssignments', 'CentreManagerAssignments';
END
GO
IF COL_LENGTH('CentreManagerAssignments', 'BranchID') IS NOT NULL
BEGIN
    EXEC sp_rename 'CentreManagerAssignments.BranchID', 'CentreID', 'COLUMN';
END
GO

-- 4. TẠO LẠI CÁC KHÓA NGOẠI VỚI TÊN MỚI
ALTER TABLE Users
ADD CONSTRAINT FK_Users_Centre FOREIGN KEY (CentreID) REFERENCES Centres(CentreID);
GO

ALTER TABLE Halls
ADD CONSTRAINT FK_Halls_Centres FOREIGN KEY (CentreID) REFERENCES Centres(CentreID);
GO

IF OBJECT_ID('CentreMovies', 'U') IS NOT NULL
BEGIN
    ALTER TABLE CentreMovies
    ADD CONSTRAINT FK_CentreMovies_Centre FOREIGN KEY (CentreID) REFERENCES Centres(CentreID);
END
GO

IF OBJECT_ID('MovieDistributionExceptions', 'U') IS NOT NULL
BEGIN
    ALTER TABLE MovieDistributionExceptions
    ADD CONSTRAINT FK_MovieDistributionExceptions_Centres FOREIGN KEY (CentreID)
        REFERENCES Centres(CentreID) ON DELETE CASCADE;
END
GO

IF OBJECT_ID('CentreFnbInventory', 'U') IS NOT NULL
BEGIN
    ALTER TABLE CentreFnbInventory
    ADD CONSTRAINT FK_Inventory_Centre FOREIGN KEY (CentreID) REFERENCES Centres(CentreID);
END
GO

IF OBJECT_ID('FnbOrders', 'U') IS NOT NULL
BEGIN
    ALTER TABLE FnbOrders
    ADD CONSTRAINT FK_FnbOrders_Centre FOREIGN KEY (CentreID) REFERENCES Centres(CentreID);
END
GO

IF OBJECT_ID('StaffTransferHistory', 'U') IS NOT NULL
BEGIN
    ALTER TABLE StaffTransferHistory
    ADD CONSTRAINT FK_Transfer_FromCentre FOREIGN KEY (FromCentreID) REFERENCES Centres(CentreID);

    ALTER TABLE StaffTransferHistory
    ADD CONSTRAINT FK_Transfer_ToCentre FOREIGN KEY (ToCentreID) REFERENCES Centres(CentreID);
END
GO

IF OBJECT_ID('CentreManagerAssignments', 'U') IS NOT NULL
BEGIN
    ALTER TABLE CentreManagerAssignments
    ADD CONSTRAINT FK_CMA_Centre FOREIGN KEY (CentreID) REFERENCES Centres(CentreID);
END
GO

PRINT N'✓ Đã di chuyển thành công toàn bộ Database từ Branch sang Centre!';
GO
