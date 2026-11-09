/* Apply after CineFlow.sql (or Cine_Flow.sql). No business rows are deleted.
   Existing global seat/hall policies are copied for each centre, time policies retain their centre.
   Run with a client that stops on SQL errors. The transaction spans GO batches.
   Keep this patch with the application release; rerunning it does not recopy legacy policies. */
USE CineFlowDB;
GO
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF OBJECT_ID('dbo.CentreSurcharges','U') IS NULL
CREATE TABLE dbo.CentreSurcharges (
    SurchargeID INT IDENTITY PRIMARY KEY,
    CentreID INT NOT NULL REFERENCES dbo.Centres(CentreID),
    SurchargeType VARCHAR(20) NOT NULL,
    TargetValue VARCHAR(30) NOT NULL,
    SurchargeName NVARCHAR(100) NOT NULL,
    Amount DECIMAL(12,2) NOT NULL CHECK (Amount>=0),
    StartTime TIME NULL, EndTime TIME NULL,
    EffectiveFrom DATETIME2 NOT NULL, EffectiveTo DATETIME2 NULL,
    UpdatedBy INT NULL REFERENCES dbo.Users(UserID),
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT CK_CentreSurcharges_Type CHECK (SurchargeType IN ('CENTRE','HALL','SEAT','MOVIE','TIME')),
    CONSTRAINT CK_CentreSurcharges_Period CHECK (EffectiveTo IS NULL OR EffectiveTo>=EffectiveFrom),
    CONSTRAINT CK_CentreSurcharges_Time CHECK ((SurchargeType='TIME' AND StartTime IS NOT NULL AND EndTime IS NOT NULL AND StartTime<EndTime) OR (SurchargeType<>'TIME' AND StartTime IS NULL AND EndTime IS NULL))
);
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('CentreSurcharges') AND name='IX_CentreSurcharges_Policy')
CREATE INDEX IX_CentreSurcharges_Policy ON CentreSurcharges(CentreID,SurchargeType,TargetValue,EffectiveFrom) INCLUDE(EffectiveTo,Amount,StartTime,EndTime);
IF COL_LENGTH('Showtimes','SaleState') IS NULL
    ALTER TABLE dbo.Showtimes ADD SaleState VARCHAR(20) NOT NULL CONSTRAINT DF_Showtimes_SaleState DEFAULT 'DRAFT'
        CONSTRAINT CK_Showtimes_SaleState CHECK(SaleState IN ('DRAFT','SCHEDULED','OPEN'));
IF COL_LENGTH('Showtimes','SaleOpensAt') IS NULL ALTER TABLE dbo.Showtimes ADD SaleOpensAt DATETIME2 NULL;
IF COL_LENGTH('Showtimes','PriceLockedAt') IS NULL ALTER TABLE dbo.Showtimes ADD PriceLockedAt DATETIME2 NULL;
IF OBJECT_ID('dbo.ShowtimeSeatPrices','U') IS NULL
CREATE TABLE dbo.ShowtimeSeatPrices (
    ShowtimeID INT NOT NULL REFERENCES dbo.Showtimes(ShowtimeID),
    SeatType VARCHAR(30) NOT NULL,
    BasePrice DECIMAL(12,2) NOT NULL,
    CentreAmount DECIMAL(12,2) NOT NULL, HallAmount DECIMAL(12,2) NOT NULL,
    MovieAmount DECIMAL(12,2) NOT NULL, TimeAmount DECIMAL(12,2) NOT NULL, SeatAmount DECIMAL(12,2) NOT NULL,
    Total AS (BasePrice+CentreAmount+HallAmount+MovieAmount+TimeAmount+SeatAmount),
    PRIMARY KEY(ShowtimeID,SeatType)
);
IF OBJECT_ID('dbo.CineFlowMigrations','U') IS NULL CREATE TABLE dbo.CineFlowMigrations(Name VARCHAR(100) PRIMARY KEY, AppliedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME());
GO
IF NOT EXISTS(SELECT 1 FROM CineFlowMigrations WHERE Name='pricing_sale_lifecycle')
BEGIN
    INSERT CentreSurcharges(CentreID,SurchargeType,TargetValue,SurchargeName,Amount,EffectiveFrom,EffectiveTo,UpdatedBy,CreatedAt)
    SELECT c.CentreID,s.SurchargeType,s.TargetValue,s.SurchargeName,s.Amount,s.EffectiveFrom,s.EffectiveTo,s.UpdatedBy,s.CreatedAt
    FROM Surcharges s CROSS JOIN Centres c;
    INSERT CentreSurcharges(CentreID,SurchargeType,TargetValue,SurchargeName,Amount,StartTime,EndTime,EffectiveFrom,EffectiveTo,UpdatedBy,CreatedAt)
    SELECT CentreID,'TIME',CONVERT(VARCHAR(8),StartTime,108)+'-'+CONVERT(VARCHAR(8),EndTime,108),SlotName,Surcharge,StartTime,EndTime,EffectiveFrom,EffectiveTo,UpdatedBy,CreatedAt
    FROM TimeSlotPricing WHERE StartTime<EndTime;
    IF EXISTS(SELECT 1 FROM TimeSlotPricing WHERE StartTime>=EndTime)
        THROW 51000,'Invalid legacy time window. Review it before migration; no rows have been deleted.',1;
END;
UPDATE CentreSurcharges SET TargetValue=CONVERT(VARCHAR(8),StartTime,108)+'-'+CONVERT(VARCHAR(8),EndTime,108)
WHERE SurchargeType='TIME' AND TargetValue='TIME';
GO
CREATE OR ALTER FUNCTION dbo.CfPriceAt(@CentreID INT,@MovieID INT,@HallType VARCHAR(30),@At DATETIME2)
RETURNS TABLE AS RETURN (
    SELECT bp.BasePrice,
      ISNULL((SELECT TOP 1 Amount FROM CentreSurcharges WHERE CentreID=@CentreID AND SurchargeType='CENTRE' AND EffectiveFrom<=@At AND (EffectiveTo IS NULL OR @At<EffectiveTo) ORDER BY EffectiveFrom DESC,SurchargeID DESC),0) CentreAmount,
      ISNULL((SELECT TOP 1 Amount FROM CentreSurcharges WHERE CentreID=@CentreID AND SurchargeType='HALL' AND TargetValue=@HallType AND EffectiveFrom<=@At AND (EffectiveTo IS NULL OR @At<EffectiveTo) ORDER BY EffectiveFrom DESC,SurchargeID DESC),0) HallAmount,
      ISNULL((SELECT TOP 1 Amount FROM CentreSurcharges WHERE CentreID=@CentreID AND SurchargeType='MOVIE' AND TargetValue=CONVERT(VARCHAR(30),@MovieID) AND EffectiveFrom<=@At AND (EffectiveTo IS NULL OR @At<EffectiveTo) ORDER BY EffectiveFrom DESC,SurchargeID DESC),0) MovieAmount,
      ISNULL((SELECT TOP 1 Amount FROM CentreSurcharges WHERE CentreID=@CentreID AND SurchargeType='TIME' AND StartTime<=CAST(@At AS TIME) AND CAST(@At AS TIME)<EndTime AND EffectiveFrom<=@At AND (EffectiveTo IS NULL OR @At<EffectiveTo) ORDER BY EffectiveFrom DESC,StartTime DESC,SurchargeID DESC),0) TimeAmount
    FROM (SELECT TOP 1 BasePrice FROM CentrePricing WHERE CentreID=@CentreID AND EffectiveFrom<=@At AND (EffectiveTo IS NULL OR @At<EffectiveTo) ORDER BY EffectiveFrom DESC,CentrePricingID DESC) bp
);
GO
CREATE OR ALTER VIEW dbo.ShowtimeLivePrices AS
SELECT s.ShowtimeID,ty.SeatType,p.BasePrice,p.CentreAmount,p.HallAmount,p.MovieAmount,p.TimeAmount,ISNULL(sp.Amount,0) SeatAmount,
       p.BasePrice+p.CentreAmount+p.HallAmount+p.MovieAmount+p.TimeAmount+ISNULL(sp.Amount,0) Total
FROM Showtimes s JOIN Halls h ON h.HallID=s.HallID
CROSS APPLY dbo.CfPriceAt(h.CentreID,s.MovieID,h.HallType,s.StartTime) p
CROSS APPLY (SELECT SeatType FROM Seats WHERE HallID=s.HallID UNION SELECT CAST('STANDARD' AS VARCHAR(30))) ty
OUTER APPLY (SELECT TOP 1 Amount FROM CentreSurcharges sc WHERE sc.CentreID=h.CentreID AND sc.SurchargeType='SEAT' AND sc.TargetValue=ty.SeatType AND sc.EffectiveFrom<=s.StartTime AND (sc.EffectiveTo IS NULL OR s.StartTime<sc.EffectiveTo) ORDER BY sc.EffectiveFrom DESC,sc.SurchargeID DESC) sp;
GO
CREATE OR ALTER VIEW dbo.ShowtimeEffectivePrices AS
SELECT p.ShowtimeID,p.SeatType,p.BasePrice,p.CentreAmount,p.HallAmount,p.MovieAmount,p.TimeAmount,p.SeatAmount,p.Total
FROM ShowtimeSeatPrices p JOIN Showtimes s ON s.ShowtimeID=p.ShowtimeID WHERE s.SaleState='OPEN'
UNION ALL
SELECT p.ShowtimeID,p.SeatType,p.BasePrice,p.CentreAmount,p.HallAmount,p.MovieAmount,p.TimeAmount,p.SeatAmount,p.Total
FROM ShowtimeLivePrices p JOIN Showtimes s ON s.ShowtimeID=p.ShowtimeID WHERE s.SaleState<>'OPEN';
GO
/* Locks the same parent row as booking before changing state or prices. */
CREATE OR ALTER PROCEDURE dbo.CfOpenShowtime @ID INT,@ScheduledOnly BIT=0 AS
BEGIN
 SET NOCOUNT ON; SET XACT_ABORT ON;
 BEGIN TRY
  BEGIN TRANSACTION;
  DECLARE @state VARCHAR(20),@status VARCHAR(20),@start DATETIME2;
  SELECT @state=SaleState,@status=Status,@start=StartTime FROM Showtimes WITH(UPDLOCK,HOLDLOCK) WHERE ShowtimeID=@ID;
  IF @ScheduledOnly=1 AND NOT EXISTS(SELECT 1 FROM Showtimes WHERE ShowtimeID=@ID AND SaleState='SCHEDULED' AND SaleOpensAt<=SYSDATETIME())
  BEGIN COMMIT; RETURN; END;
  IF @state IS NULL OR @status<>'SCHEDULED' OR @start<=SYSDATETIME() THROW 51001,'Showtime is not available for sale.',1;
  IF @state<>'OPEN'
  BEGIN
   DECLARE @centre INT;
   SELECT @centre=h.CentreID FROM Showtimes s JOIN Halls h ON h.HallID=s.HallID WHERE s.ShowtimeID=@ID;
   SELECT CentreID FROM Centres WITH(UPDLOCK,HOLDLOCK) WHERE CentreID=@centre;
   IF NOT EXISTS(SELECT 1 FROM ShowtimeLivePrices WHERE ShowtimeID=@ID) THROW 51002,'Base price at showtime start is missing.',1;
   IF EXISTS(SELECT 1 FROM ShowtimeLivePrices WHERE ShowtimeID=@ID AND Total>9999999999) THROW 51006,'Total ticket price exceeds the supported amount.',1;
   IF NOT EXISTS(SELECT 1 FROM Showtimes s JOIN Halls h ON h.HallID=s.HallID JOIN Centres c ON c.CentreID=h.CentreID JOIN Movies m ON m.MovieID=s.MovieID JOIN CentreMovies cm ON cm.CentreID=c.CentreID AND cm.MovieID=m.MovieID WHERE s.ShowtimeID=@ID AND h.Status='AVAILABLE' AND c.Status='ACTIVE' AND cm.IsActive=1 AND m.Status='NOW_SHOWING') THROW 51003,'Movie, hall or centre is unavailable.',1;
   DELETE FROM ShowtimeSeatPrices WHERE ShowtimeID=@ID;
   INSERT ShowtimeSeatPrices(ShowtimeID,SeatType,BasePrice,CentreAmount,HallAmount,MovieAmount,TimeAmount,SeatAmount)
    SELECT ShowtimeID,SeatType,BasePrice,CentreAmount,HallAmount,MovieAmount,TimeAmount,SeatAmount FROM ShowtimeLivePrices WHERE ShowtimeID=@ID;
   UPDATE Showtimes SET SaleState='OPEN',PriceLockedAt=SYSDATETIME(),SaleOpensAt=COALESCE(SaleOpensAt,SYSDATETIME()),UpdatedAt=SYSDATETIME() WHERE ShowtimeID=@ID;
  END;
  COMMIT;
 END TRY BEGIN CATCH IF @@TRANCOUNT>0 ROLLBACK; THROW; END CATCH;
END;
GO
IF NOT EXISTS(SELECT 1 FROM CineFlowMigrations WHERE Name='pricing_sale_lifecycle')
BEGIN
    /* Legacy bookings keep BookingSeats.Price. Snapshot current policy only for subsequent sales. */
    IF EXISTS(SELECT 1 FROM Showtimes s JOIN Bookings b ON b.ShowtimeID=s.ShowtimeID WHERE NOT EXISTS(SELECT 1 FROM ShowtimeLivePrices p WHERE p.ShowtimeID=s.ShowtimeID))
        THROW 51004,'Some booked showtimes have no base price. Supply their historical base policy before migration.',1;
    INSERT ShowtimeSeatPrices(ShowtimeID,SeatType,BasePrice,CentreAmount,HallAmount,MovieAmount,TimeAmount,SeatAmount)
    SELECT p.ShowtimeID,p.SeatType,p.BasePrice,0,ISNULL(hp.Amount,0),0,ISNULL(tp.Surcharge,0),ISNULL(sp.Amount,0)
    FROM ShowtimeLivePrices p JOIN Showtimes s ON s.ShowtimeID=p.ShowtimeID JOIN Halls h ON h.HallID=s.HallID
    OUTER APPLY(SELECT SUM(Amount) Amount FROM Surcharges WHERE SurchargeType='HALL' AND TargetValue=h.HallType AND EffectiveFrom<=s.StartTime AND (EffectiveTo IS NULL OR s.StartTime<EffectiveTo))hp
    OUTER APPLY(SELECT SUM(Amount) Amount FROM Surcharges WHERE SurchargeType='SEAT' AND TargetValue=p.SeatType AND EffectiveFrom<=s.StartTime AND (EffectiveTo IS NULL OR s.StartTime<EffectiveTo))sp
    OUTER APPLY(SELECT TOP 1 Surcharge FROM TimeSlotPricing WHERE CentreID=h.CentreID AND CAST(s.StartTime AS TIME) BETWEEN StartTime AND EndTime AND EffectiveFrom<=s.StartTime AND (EffectiveTo IS NULL OR s.StartTime<EffectiveTo) ORDER BY StartTime DESC)tp;
    UPDATE s SET SaleState='OPEN',SaleOpensAt=SYSDATETIME(),PriceLockedAt=SYSDATETIME()
    FROM Showtimes s WHERE EXISTS(SELECT 1 FROM ShowtimeSeatPrices p WHERE p.ShowtimeID=s.ShowtimeID);
    INSERT CineFlowMigrations(Name) VALUES('pricing_sale_lifecycle');
END;
COMMIT TRANSACTION;
GO
