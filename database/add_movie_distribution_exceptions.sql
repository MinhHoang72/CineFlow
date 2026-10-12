/* Persist the distribution mode and exclusions configured by Admin. */
IF OBJECT_ID(N'dbo.MovieDistributionSettings', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.MovieDistributionSettings (
        MovieID INT NOT NULL,
        Scope VARCHAR(10) NOT NULL,
        City NVARCHAR(120) NULL,
        UpdatedAt DATETIME NOT NULL CONSTRAINT DF_MovieDistributionSettings_UpdatedAt DEFAULT GETDATE(),
        CONSTRAINT PK_MovieDistributionSettings PRIMARY KEY (MovieID),
        CONSTRAINT CK_MovieDistributionSettings_Scope CHECK (Scope IN ('system', 'city', 'selected')),
        CONSTRAINT FK_MovieDistributionSettings_Movies FOREIGN KEY (MovieID)
            REFERENCES dbo.Movies(MovieID) ON DELETE CASCADE
    );
END;
GO

IF COL_LENGTH(N'dbo.MovieDistributionSettings', N'City') IS NULL
BEGIN
    ALTER TABLE dbo.MovieDistributionSettings ADD City NVARCHAR(120) NULL;
END;
GO

IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_MovieDistributionSettings_Scope')
BEGIN
    ALTER TABLE dbo.MovieDistributionSettings DROP CONSTRAINT CK_MovieDistributionSettings_Scope;
END;
GO

ALTER TABLE dbo.MovieDistributionSettings WITH CHECK
    ADD CONSTRAINT CK_MovieDistributionSettings_Scope CHECK (Scope IN ('system', 'city', 'selected'));
GO

IF OBJECT_ID(N'dbo.MovieDistributionExceptions', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.MovieDistributionExceptions (
        MovieID INT NOT NULL,
        BranchID INT NOT NULL,
        CONSTRAINT PK_MovieDistributionExceptions PRIMARY KEY (MovieID, BranchID),
        CONSTRAINT FK_MovieDistributionExceptions_Settings FOREIGN KEY (MovieID)
            REFERENCES dbo.MovieDistributionSettings(MovieID) ON DELETE CASCADE,
        CONSTRAINT FK_MovieDistributionExceptions_Branches FOREIGN KEY (BranchID)
            REFERENCES dbo.Branches(BranchID) ON DELETE CASCADE
    );
END;
GO
