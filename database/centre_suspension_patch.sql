USE CineFlowDB;
GO

-- Không suy đoán nguyên nhân các suất CANCELLED cũ để tránh mở lại suất huỷ thủ công.
IF COL_LENGTH('dbo.Showtimes', 'CancelledByCentreSuspension') IS NULL
BEGIN
    ALTER TABLE dbo.Showtimes ADD CancelledByCentreSuspension BIT NOT NULL
        CONSTRAINT DF_Showtimes_CentreSuspension DEFAULT 0 WITH VALUES;
END;
GO
