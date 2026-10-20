/* =========================================================
   F&B: nhân bản danh mục món/combo mẫu cho TỪNG Centre
   Vì sao: dữ liệu mẫu của nhóm thêm món/combo KHÔNG có CentreID,
   còn trang F&B (FnbServlet/FnbDAO) quản lý thực đơn THEO Centre.
   Script này:
     1) bổ sung dòng combo còn thiếu cho combo mẫu 3, 4, 5;
     2) với mỗi Centre chưa có món nào: copy 12 món + 5 combo gốc
        (CentreID IS NULL), copy dòng combo và tạo tồn kho 100/món.
   Có thể chạy lại nhiều lần: Centre đã có món sẽ bị bỏ qua.
   Các dòng gốc (CentreID NULL) được giữ nguyên.
   ========================================================= */
USE CineFlowDB;
GO

SET XACT_ABORT ON;
BEGIN TRANSACTION;

/* 1) Bổ sung dòng combo cho combo gốc 3, 4, 5 (nếu chưa có) */
INSERT INTO ComboItems (ComboID, FnbItemID, Quantity)
SELECT v.ComboID, v.FnbItemID, v.Quantity
FROM (VALUES
    (3, 6, 1), (3, 3, 1), (3, 4, 1),   -- Combo Movie Night: 1 bắp phô mai + 2 nước ngọt
    (4, 2, 1), (4, 3, 1),              -- Combo Solo: 1 bắp caramel + 1 nước ngọt
    (5, 1, 2), (5, 3, 4), (5, 5, 1)    -- Combo Premium: 2 bắp + 4 nước + 1 snack
) AS v(ComboID, FnbItemID, Quantity)
WHERE EXISTS (SELECT 1 FROM FnbCombos WHERE ComboID = v.ComboID AND CentreID IS NULL)
  AND EXISTS (SELECT 1 FROM FnbItems WHERE FnbItemID = v.FnbItemID AND CentreID IS NULL)
  AND NOT EXISTS (SELECT 1 FROM ComboItems ci WHERE ci.ComboID = v.ComboID AND ci.FnbItemID = v.FnbItemID);

/* 2) Nhân bản cho từng Centre */
DECLARE @centreId INT;
DECLARE centre_cursor CURSOR LOCAL FAST_FORWARD FOR
    SELECT CentreID FROM Centres ORDER BY CentreID;
OPEN centre_cursor;
FETCH NEXT FROM centre_cursor INTO @centreId;

WHILE @@FETCH_STATUS = 0
BEGIN
    IF NOT EXISTS (SELECT 1 FROM FnbItems WHERE CentreID = @centreId)
       AND NOT EXISTS (SELECT 1 FROM FnbCombos WHERE CentreID = @centreId)
    BEGIN
        DECLARE @itemMap TABLE (OldId INT PRIMARY KEY, NewId INT);
        DECLARE @comboMap TABLE (OldId INT PRIMARY KEY, NewId INT);
        DELETE FROM @itemMap;
        DELETE FROM @comboMap;

        /* Món: MERGE ... ON 1 = 0 để chèn mọi dòng và lấy lại ID mới */
        MERGE FnbItems AS target
        USING (SELECT FnbItemID, CategoryID, ItemName, Description, Price, ImageURL, Status
               FROM FnbItems WHERE CentreID IS NULL) AS source
        ON 1 = 0
        WHEN NOT MATCHED THEN
            INSERT (CategoryID, CentreID, ItemName, Description, Price, ImageURL, Status)
            VALUES (source.CategoryID, @centreId, source.ItemName, source.Description,
                    source.Price, source.ImageURL, source.Status)
        OUTPUT source.FnbItemID, inserted.FnbItemID INTO @itemMap (OldId, NewId);

        /* Combo */
        MERGE FnbCombos AS target
        USING (SELECT ComboID, ComboName, Description, Price, ImageURL, Status
               FROM FnbCombos WHERE CentreID IS NULL) AS source
        ON 1 = 0
        WHEN NOT MATCHED THEN
            INSERT (CentreID, ComboName, Description, Price, ImageURL, Status)
            VALUES (@centreId, source.ComboName, source.Description,
                    source.Price, source.ImageURL, source.Status)
        OUTPUT source.ComboID, inserted.ComboID INTO @comboMap (OldId, NewId);

        /* Dòng combo: đổi ID cũ sang ID mới của Centre này */
        INSERT INTO ComboItems (ComboID, FnbItemID, Quantity)
        SELECT cm.NewId, im.NewId, ci.Quantity
        FROM ComboItems ci
        JOIN @comboMap cm ON cm.OldId = ci.ComboID
        JOIN @itemMap im ON im.OldId = ci.FnbItemID;

        /* Tồn kho: mỗi món mới có sẵn 100 */
        INSERT INTO CentreFnbInventory (CentreID, FnbItemID, StockQuantity, IsAvailable)
        SELECT @centreId, NewId, 100, 1 FROM @itemMap;
    END

    FETCH NEXT FROM centre_cursor INTO @centreId;
END

CLOSE centre_cursor;
DEALLOCATE centre_cursor;

COMMIT TRANSACTION;
GO

/* Kiểm tra: mỗi Centre nên có 12 món và 5 combo */
SELECT c.CentreID, c.CentreName,
       (SELECT COUNT(*) FROM FnbItems i WHERE i.CentreID = c.CentreID) AS SoMon,
       (SELECT COUNT(*) FROM FnbCombos co WHERE co.CentreID = c.CentreID) AS SoCombo
FROM Centres c
ORDER BY c.CentreID;
GO
