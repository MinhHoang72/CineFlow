/* Local demonstration bookings for the seat map.
   Up to 3 upcoming shows per centre per day, today and tomorrow; 6 seats each.
   COUNTER + CONFIRMED, no customer identity, no external payment calls.
   DEMOSEAT-<ShowtimeID> makes reruns idempotent. Existing occupied seats are excluded.
*/
SET NOCOUNT ON;
SET XACT_ABORT ON;
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;
DECLARE @Now datetime2=SYSDATETIME();
DECLARE @Day date=CAST(@Now AS date);
DECLARE @Created TABLE(BookingID int,BookingCode varchar(30),ShowtimeID int,Seats int);
BEGIN TRY
 BEGIN TRANSACTION;
 DECLARE @Shows TABLE(ShowtimeID int PRIMARY KEY,CentreID int,HallID int,HallType varchar(30),StartTime datetime2);
 ;WITH candidates AS (
  SELECT s.ShowtimeID,h.CentreID,s.HallID,h.HallType,s.StartTime,
   ROW_NUMBER() OVER(PARTITION BY h.CentreID,CAST(s.StartTime AS date) ORDER BY s.StartTime,s.ShowtimeID) rn
  FROM dbo.Showtimes s WITH(UPDLOCK,HOLDLOCK) JOIN dbo.Halls h ON h.HallID=s.HallID
  WHERE s.Status='SCHEDULED' AND s.StartTime>@Now AND s.StartTime<DATEADD(day,2,CAST(@Day AS datetime2))
   AND h.Status='AVAILABLE'
   AND (SELECT COUNT(*) FROM dbo.Seats se WHERE se.HallID=s.HallID AND se.Status='AVAILABLE')>=6
 )
 INSERT INTO @Shows SELECT ShowtimeID,CentreID,HallID,HallType,StartTime FROM candidates WHERE rn<=3;
 DECLARE @Show int,@Centre int,@Hall int,@HallType varchar(30),@Start datetime2,@Booking int,@Code varchar(30);
 DECLARE @Seats TABLE(SeatID int PRIMARY KEY,Price decimal(12,2));
 DECLARE show_cursor CURSOR LOCAL FAST_FORWARD FOR SELECT ShowtimeID,CentreID,HallID,HallType,StartTime FROM @Shows ORDER BY CentreID,StartTime;
 OPEN show_cursor;
 FETCH NEXT FROM show_cursor INTO @Show,@Centre,@Hall,@HallType,@Start;
 WHILE @@FETCH_STATUS=0
 BEGIN
  SET @Code=CONCAT('DEMOSEAT-',@Show);
  IF NOT EXISTS(SELECT 1 FROM dbo.Bookings WITH(UPDLOCK,HOLDLOCK) WHERE BookingCode=@Code)
  BEGIN
   DELETE FROM @Seats;
   INSERT INTO @Seats
   SELECT TOP 6 se.SeatID,base.BasePrice+ISNULL(ts.Surcharge,0)+ISNULL(ss.Amount,0)+ISNULL(hs.Amount,0)
   FROM dbo.Seats se
   CROSS APPLY(SELECT TOP 1 BasePrice FROM dbo.CentrePricing WHERE CentreID=@Centre AND EffectiveFrom<=@Start AND (EffectiveTo IS NULL OR EffectiveTo>@Start) ORDER BY EffectiveFrom DESC,CentrePricingID DESC) base
   OUTER APPLY(SELECT TOP 1 Surcharge FROM dbo.TimeSlotPricing WHERE CentreID=@Centre AND EffectiveFrom<=@Start AND (EffectiveTo IS NULL OR EffectiveTo>@Start) AND CAST(@Start AS time)>=StartTime AND CAST(@Start AS time)<EndTime ORDER BY EffectiveFrom DESC,TimeSlotPricingID DESC) ts
   OUTER APPLY(SELECT TOP 1 Amount FROM dbo.Surcharges WHERE SurchargeType='SEAT' AND TargetValue=se.SeatType AND EffectiveFrom<=@Start AND (EffectiveTo IS NULL OR EffectiveTo>@Start) ORDER BY EffectiveFrom DESC,SurchargeID DESC) ss
   OUTER APPLY(SELECT TOP 1 Amount FROM dbo.Surcharges WHERE SurchargeType='HALL' AND TargetValue=@HallType AND EffectiveFrom<=@Start AND (EffectiveTo IS NULL OR EffectiveTo>@Start) ORDER BY EffectiveFrom DESC,SurchargeID DESC) hs
   WHERE se.HallID=@Hall AND se.Status='AVAILABLE'
    AND NOT EXISTS(SELECT 1 FROM dbo.BookingSeats bs WITH(UPDLOCK,HOLDLOCK) JOIN dbo.Bookings b ON b.BookingID=bs.BookingID WHERE b.ShowtimeID=@Show AND b.BookingStatus IN('PENDING','CONFIRMED','COMPLETED') AND bs.SeatID=se.SeatID AND (bs.SeatStatus='CONFIRMED' OR (bs.SeatStatus='HELD' AND bs.HoldUntil>@Now)))
   ORDER BY CASE WHEN se.SeatRow IN('D','E') THEN 0 ELSE 1 END, CASE WHEN se.SeatNumber BETWEEN 4 AND 9 THEN 0 ELSE 1 END,se.SeatRow,se.SeatNumber;
   IF (SELECT COUNT(*) FROM @Seats)=6
   BEGIN
    INSERT INTO dbo.Bookings(BookingCode,UserID,ShowtimeID,BookingType,BookingStatus,SubTotal,DiscountAmount,TotalAmount)
     SELECT @Code,NULL,@Show,'COUNTER','CONFIRMED',SUM(Price),0,SUM(Price) FROM @Seats;
    SET @Booking=CAST(SCOPE_IDENTITY() AS int);
    INSERT INTO dbo.BookingSeats(BookingID,SeatID,Price,SeatStatus,HoldUntil)
     SELECT @Booking,SeatID,Price,'CONFIRMED',NULL FROM @Seats;
    INSERT INTO @Created VALUES(@Booking,@Code,@Show,6);
   END;
  END;
  FETCH NEXT FROM show_cursor INTO @Show,@Centre,@Hall,@HallType,@Start;
 END;
 CLOSE show_cursor;
 DEALLOCATE show_cursor;
 IF EXISTS(SELECT 1 FROM @Created c JOIN dbo.Bookings b ON b.BookingID=c.BookingID JOIN dbo.BookingSeats bs ON bs.BookingID=b.BookingID JOIN dbo.Seats se ON se.SeatID=bs.SeatID JOIN dbo.Showtimes s ON s.ShowtimeID=b.ShowtimeID WHERE se.HallID<>s.HallID)
  THROW 51001,'Seat hall mismatch; seed rolled back',1;
 COMMIT;
 SELECT COUNT(*) AddedBookings,ISNULL(SUM(Seats),0) AddedBookedSeats FROM @Created;
 SELECT h.CentreID,h.HallName,s.ShowtimeID,s.StartTime,c.BookingCode,c.Seats FROM @Created c JOIN dbo.Showtimes s ON s.ShowtimeID=c.ShowtimeID JOIN dbo.Halls h ON h.HallID=s.HallID ORDER BY h.CentreID,s.StartTime;
END TRY
BEGIN CATCH
 IF CURSOR_STATUS('local','show_cursor')>=0 CLOSE show_cursor;
 IF CURSOR_STATUS('local','show_cursor')>=-1 DEALLOCATE show_cursor;
 IF @@TRANCOUNT>0 ROLLBACK;
 THROW;
END CATCH;
