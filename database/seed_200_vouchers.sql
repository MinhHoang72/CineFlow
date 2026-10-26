/* =====================================================================================
   SCRIPT SEED DỮ LIỆU MẪU: 200 VOUCHERS CHO ADMIN VÀ MANAGER
   Hệ thống: CineFlow (CineFlowDB)
   
   Phân bổ 200 Voucher:
   1. ADMIN (Toàn hệ thống - CentreID = NULL): 80 vouchers (GLB01 -> GLB80)
      - 40 voucher đang hoạt động (ACTIVE): 20 PERCENT (10%-48%), 20 FIXED (20k-143.5k)
      - 16 voucher đã hết hạn (EXPIRED: EndDate trong quá khứ)
      - 12 voucher đã hết số lượt dùng (EXHAUSTED: UsedCount = UsageLimit)
      - 12 voucher tạm ngừng (INACTIVE)
      
   2. MANAGER (Chi nhánh riêng biệt): 120 vouchers (30 voucher mỗi rạp)
      - Chi nhánh 1 (Cầu Giấy - CentreID = 1): 30 vouchers (CGY01 -> CGY30)
      - Chi nhánh 2 (Hà Đông - CentreID = 2): 30 vouchers (HDO01 -> HDO30)
      - Chi nhánh 3 (Hoàn Kiếm - CentreID = 3): 30 vouchers (HKI01 -> HKI30)
      - Chi nhánh 4 (Quận 1 - CentreID = 4): 30 vouchers (Q0101 -> Q0130)
      
   An toàn: Chạy lại nhiều lần không bao giờ bị trùng khóa (IF NOT EXISTS).
   Xóa toàn bộ 200 voucher này khi cần:
     DELETE FROM Vouchers WHERE VoucherCode LIKE 'GLB%' OR VoucherCode LIKE 'CGY%' 
                            OR VoucherCode LIKE 'HDO%' OR VoucherCode LIKE 'HKI%' OR VoucherCode LIKE 'Q01%';
   ===================================================================================== */

USE CineFlowDB;
GO

SET NOCOUNT ON;

PRINT N'Bắt đầu thêm 200 voucher mẫu...';

-- =====================================================================================
-- PHẦN 1: 80 VOUCHER TOÀN HỆ THỐNG CHO ADMIN (CentreID = NULL)
-- =====================================================================================
DECLARE @i INT = 1;
WHILE @i <= 80
BEGIN
    DECLARE @code VARCHAR(50) = 'GLB' + RIGHT('0' + CAST(@i AS VARCHAR(2)), 2);
    
    IF NOT EXISTS (SELECT 1 FROM Vouchers WHERE VoucherCode = @code)
    BEGIN
        DECLARE @name NVARCHAR(150);
        DECLARE @desc NVARCHAR(500);
        DECLARE @type VARCHAR(20);
        DECLARE @val DECIMAL(12,2);
        DECLARE @minOrder DECIMAL(12,2);
        DECLARE @maxDisc DECIMAL(12,2);
        DECLARE @startDate DATETIME2 = '2026-01-01 00:00:00';
        DECLARE @endDate DATETIME2 = '2026-12-31 23:59:59';
        DECLARE @limit INT = 500;
        DECLARE @used INT = 0;
        DECLARE @status VARCHAR(20) = 'ACTIVE';

        IF @i <= 20 
        BEGIN
            -- 20 Voucher giảm % đang hoạt động (10% -> 48%)
            SET @type = 'PERCENT';
            SET @val = 10.00 + ((@i - 1) * 2);
            SET @name = N'Voucher Toàn Chuỗi Giảm ' + CAST(CAST(@val AS INT) AS NVARCHAR) + N'%';
            SET @desc = N'Áp dụng cho mọi cụm rạp CineFlow toàn quốc';
            SET @minOrder = 100000;
            SET @maxDisc = 60000;
        END
        ELSE IF @i <= 40
        BEGIN
            -- 20 Voucher giảm tiền mặt đang hoạt động (20k -> 143.5k)
            SET @type = 'FIXED';
            SET @val = 20000.00 + ((@i - 21) * 6500);
            SET @name = N'Voucher Toàn Chuỗi Giảm ' + CAST(CAST(@val AS INT) AS NVARCHAR) + N'đ';
            SET @desc = N'Giảm trực tiếp vào tổng hóa đơn vé xem phim';
            SET @minOrder = 120000;
            SET @maxDisc = @val;
        END
        ELSE IF @i <= 56
        BEGIN
            -- 16 Voucher đã hết hạn (EXPIRED: EndDate quá khứ)
            SET @type = CASE WHEN @i % 2 = 0 THEN 'PERCENT' ELSE 'FIXED' END;
            SET @val = CASE WHEN @type = 'PERCENT' THEN 25.00 ELSE 30000.00 END;
            SET @name = N'Ưu đãi Mùa Lễ Toàn Chuỗi #' + CAST(@i AS NVARCHAR);
            SET @desc = N'Chương trình khuyến mãi mùa trước đã kết thúc';
            SET @minOrder = 80000;
            SET @maxDisc = 50000;
            SET @startDate = '2026-01-01 00:00:00';
            SET @endDate = '2026-02-15 23:59:59';
            SET @status = 'ACTIVE';
        END
        ELSE IF @i <= 68
        BEGIN
            -- 12 Voucher đã hết lượt sử dụng (EXHAUSTED)
            SET @type = 'PERCENT';
            SET @val = 50.00;
            SET @name = N'Flash Sale 50% Toàn Chuỗi #' + CAST(@i AS NVARCHAR);
            SET @desc = N'Số lượng có hạn, đã phát hết sạch lượt dùng';
            SET @minOrder = 150000;
            SET @maxDisc = 100000;
            SET @limit = 100;
            SET @used = 100;
            SET @status = 'ACTIVE';
        END
        ELSE
        BEGIN
            -- 12 Voucher tạm ngừng (INACTIVE)
            SET @type = 'FIXED';
            SET @val = 50000.00;
            SET @name = N'Voucher Tạm Ngừng Phát Hành #' + CAST(@i AS NVARCHAR);
            SET @desc = N'Voucher đã tạm ngưng theo chính sách ban quản trị';
            SET @minOrder = 200000;
            SET @maxDisc = 50000;
            SET @status = 'INACTIVE';
        END

        INSERT INTO Vouchers (
            VoucherCode, VoucherName, Description, CentreID, CreatedBy,
            DiscountType, DiscountValue, MinimumOrderValue, MaximumDiscount,
            StartDate, EndDate, UsageLimit, UsedCount, PerCustomerLimit, Status
        ) VALUES (
            @code, @name, @desc, NULL, 1,
            @type, @val, @minOrder, @maxDisc,
            @startDate, @endDate, @limit, @used, 1, @status
        );
    END
    SET @i += 1;
END
PRINT N'-> Đã hoàn thành 80 voucher toàn hệ thống cho Admin!';

-- =====================================================================================
-- PHẦN 2: 120 VOUCHER CHO 4 CHI NHÁNH (30 VOUCHER / RẠP)
-- =====================================================================================
DECLARE @BranchList TABLE (CentreId INT, Prefix VARCHAR(10), BranchName NVARCHAR(100));
INSERT INTO @BranchList VALUES 
(1, 'CGY', N'Cầu Giấy'),
(2, 'HDO', N'Hà Đông'),
(3, 'HKI', N'Hoàn Kiếm'),
(4, 'Q01', N'Quận 1');

DECLARE @curCentre INT, @curPrefix VARCHAR(10), @curName NVARCHAR(100);
DECLARE branch_cursor CURSOR FOR SELECT CentreId, Prefix, BranchName FROM @BranchList;

OPEN branch_cursor;
FETCH NEXT FROM branch_cursor INTO @curCentre, @curPrefix, @curName;

WHILE @@FETCH_STATUS = 0
BEGIN
    DECLARE @j INT = 1;
    WHILE @j <= 30
    BEGIN
        DECLARE @bCode VARCHAR(50) = @curPrefix + RIGHT('0' + CAST(@j AS VARCHAR(2)), 2);
        
        IF NOT EXISTS (SELECT 1 FROM Vouchers WHERE VoucherCode = @bCode)
        BEGIN
            DECLARE @bType VARCHAR(20) = CASE WHEN @j % 2 = 0 THEN 'PERCENT' ELSE 'FIXED' END;
            DECLARE @bVal DECIMAL(12,2) = CASE WHEN @bType = 'PERCENT' THEN 10.00 + (@j * 1.5) ELSE 15000.00 + (@j * 2500) END;
            DECLARE @bEndDate DATETIME2 = CASE WHEN @j <= 6 THEN '2026-02-01' ELSE '2026-12-31' END;
            DECLARE @bLimit INT = CASE WHEN @j IN (7, 8) THEN 50 ELSE 200 END;
            DECLARE @bUsed INT = CASE WHEN @j IN (7, 8) THEN 50 ELSE @j * 2 END;
            DECLARE @bStatus VARCHAR(20) = CASE WHEN @j IN (9, 10) THEN 'INACTIVE' ELSE 'ACTIVE' END;
            DECLARE @bTitle NVARCHAR(150) = N'Ưu Đãi Rạp ' + @curName + N' #' + RIGHT('0' + CAST(@j AS VARCHAR(2)), 2);
            DECLARE @bDesc NVARCHAR(500) = N'Chỉ áp dụng khi xem phim trực tiếp tại CineFlow ' + @curName;

            INSERT INTO Vouchers (
                VoucherCode, VoucherName, Description, CentreID, CreatedBy,
                DiscountType, DiscountValue, MinimumOrderValue, MaximumDiscount,
                StartDate, EndDate, UsageLimit, UsedCount, PerCustomerLimit, Status
            ) VALUES (
                @bCode, @bTitle, @bDesc, @curCentre, NULL,
                @bType, @bVal, 80000, 50000,
                '2026-01-01', @bEndDate, @bLimit, @bUsed, 1, @bStatus
            );
        END
        SET @j += 1;
    END

    PRINT N'-> Đã hoàn thành 30 voucher cho chi nhánh ' + @curName;
    FETCH NEXT FROM branch_cursor INTO @curCentre, @curPrefix, @curName;
END

CLOSE branch_cursor;
DEALLOCATE branch_cursor;

PRINT N'======================================================';
PRINT N'TẤT CẢ 200 VOUCHER MẪU ĐÃ ĐƯỢC THÊM THÀNH CÔNG!';
PRINT N'======================================================';

SELECT COUNT(*) AS [Tổng Số Voucher Trong Database] FROM Vouchers;
GO