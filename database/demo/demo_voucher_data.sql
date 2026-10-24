/* DEMO VOUCHER: thêm 12 voucher thử (TEST01..TEST12) để demo phân trang, lọc và các nhãn trạng thái.
   TEST01-03: đã hết hạn | TEST04: hết lượt (giới hạn 0) | TEST05: tạm ngưng | còn lại: đang hiệu lực
   Chạy lại nhiều lần vẫn an toàn (voucher nào có rồi thì bỏ qua).
   Xóa sau khi demo:  DELETE FROM Vouchers WHERE VoucherCode LIKE 'TEST%'; */
USE CineFlowDB;
GO
DECLARE @i INT = 1;
WHILE @i <= 12
BEGIN
    IF NOT EXISTS (SELECT 1 FROM Vouchers WHERE VoucherCode = 'TEST' + RIGHT('0' + CAST(@i AS VARCHAR(2)), 2))
        INSERT INTO Vouchers (VoucherCode, VoucherName, DiscountType, DiscountValue,
                              MinimumOrderValue, StartDate, EndDate, UsageLimit, UsedCount, Status)
        VALUES (
            'TEST' + RIGHT('0' + CAST(@i AS VARCHAR(2)), 2),
            N'Voucher thử số ' + CAST(@i AS NVARCHAR(2)),
            CASE WHEN @i % 2 = 0 THEN 'FIXED' ELSE 'PERCENT' END,
            CASE WHEN @i % 2 = 0 THEN 20000 ELSE 10 END,
            0,
            '2026-01-01',
            CASE WHEN @i <= 3 THEN '2026-02-01' ELSE '2026-12-31' END,
            CASE WHEN @i = 4 THEN 0 ELSE 100 END,
            0,
            CASE WHEN @i = 5 THEN 'INACTIVE' ELSE 'ACTIVE' END
        );
    SET @i += 1;
END
GO
