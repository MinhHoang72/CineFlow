USE CineFlowDB;
GO

-- ====================================================================================
-- THÊM 15 NHÂN VIÊN CHO CỤM RẠP CINEFLOW THANH KHÊ (ĐÀ NẴNG) - CENTREID = 23
-- Mật khẩu mặc định của tất cả tài khoản: 123456
-- Phân bổ: 8 Nhân viên bán vé (TICKET_SALES_STAFF) & 7 Nhân viên soát vé (TICKET_CHECKER_STAFF)
-- ====================================================================================

INSERT INTO Users (
    FullName, Account, Email, Password, Phone, Avatar, Role, Status, CentreID, EmailVerified, PhoneVerified, CreatedAt
) VALUES
(N'Nguyễn Hoàng Nam', 'staff_tk_01', 'staff.tk01@cineflow.vn', '$2a$10$4AeCwdkcGvspefpI3xi8K.F5mdPwC7zGSm3nVb82VfnRgp9BaUrkq', '0921230001', NULL, 'TICKET_SALES_STAFF', 'ACTIVE', 23, 1, 1, GETDATE()),
(N'Trần Thị Thùy Dung', 'staff_tk_02', 'staff.tk02@cineflow.vn', '$2a$10$4AeCwdkcGvspefpI3xi8K.F5mdPwC7zGSm3nVb82VfnRgp9BaUrkq', '0921230002', NULL, 'TICKET_CHECKER_STAFF', 'ACTIVE', 23, 1, 1, GETDATE()),
(N'Lê Quốc Bảo', 'staff_tk_03', 'staff.tk03@cineflow.vn', '$2a$10$4AeCwdkcGvspefpI3xi8K.F5mdPwC7zGSm3nVb82VfnRgp9BaUrkq', '0921230003', NULL, 'TICKET_SALES_STAFF', 'ACTIVE', 23, 1, 1, GETDATE()),
(N'Phạm Minh Thư', 'staff_tk_04', 'staff.tk04@cineflow.vn', '$2a$10$4AeCwdkcGvspefpI3xi8K.F5mdPwC7zGSm3nVb82VfnRgp9BaUrkq', '0921230004', NULL, 'TICKET_CHECKER_STAFF', 'ACTIVE', 23, 1, 1, GETDATE()),
(N'Đặng Tuấn Kiệt', 'staff_tk_05', 'staff.tk05@cineflow.vn', '$2a$10$4AeCwdkcGvspefpI3xi8K.F5mdPwC7zGSm3nVb82VfnRgp9BaUrkq', '0921230005', NULL, 'TICKET_SALES_STAFF', 'ACTIVE', 23, 1, 1, GETDATE()),
(N'Huỳnh Mai Phương', 'staff_tk_06', 'staff.tk06@cineflow.vn', '$2a$10$4AeCwdkcGvspefpI3xi8K.F5mdPwC7zGSm3nVb82VfnRgp9BaUrkq', '0921230006', NULL, 'TICKET_CHECKER_STAFF', 'ACTIVE', 23, 1, 1, GETDATE()),
(N'Võ Thành Đạt', 'staff_tk_07', 'staff.tk07@cineflow.vn', '$2a$10$4AeCwdkcGvspefpI3xi8K.F5mdPwC7zGSm3nVb82VfnRgp9BaUrkq', '0921230007', NULL, 'TICKET_SALES_STAFF', 'ACTIVE', 23, 1, 1, GETDATE()),
(N'Bùi Thảo Nhi', 'staff_tk_08', 'staff.tk08@cineflow.vn', '$2a$10$4AeCwdkcGvspefpI3xi8K.F5mdPwC7zGSm3nVb82VfnRgp9BaUrkq', '0921230008', NULL, 'TICKET_CHECKER_STAFF', 'ACTIVE', 23, 1, 1, GETDATE()),
(N'Đỗ Gia Huy', 'staff_tk_09', 'staff.tk09@cineflow.vn', '$2a$10$4AeCwdkcGvspefpI3xi8K.F5mdPwC7zGSm3nVb82VfnRgp9BaUrkq', '0921230009', NULL, 'TICKET_SALES_STAFF', 'ACTIVE', 23, 1, 1, GETDATE()),
(N'Trương Bảo Châu', 'staff_tk_10', 'staff.tk10@cineflow.vn', '$2a$10$4AeCwdkcGvspefpI3xi8K.F5mdPwC7zGSm3nVb82VfnRgp9BaUrkq', '0921230010', NULL, 'TICKET_CHECKER_STAFF', 'ACTIVE', 23, 1, 1, GETDATE()),
(N'Ngô Đình Khang', 'staff_tk_11', 'staff.tk11@cineflow.vn', '$2a$10$4AeCwdkcGvspefpI3xi8K.F5mdPwC7zGSm3nVb82VfnRgp9BaUrkq', '0921230011', NULL, 'TICKET_SALES_STAFF', 'ACTIVE', 23, 1, 1, GETDATE()),
(N'Hoàng Ngọc Ánh', 'staff_tk_12', 'staff.tk12@cineflow.vn', '$2a$10$4AeCwdkcGvspefpI3xi8K.F5mdPwC7zGSm3nVb82VfnRgp9BaUrkq', '0921230012', NULL, 'TICKET_CHECKER_STAFF', 'ACTIVE', 23, 1, 1, GETDATE()),
(N'Phan Nhật Anh', 'staff_tk_13', 'staff.tk13@cineflow.vn', '$2a$10$4AeCwdkcGvspefpI3xi8K.F5mdPwC7zGSm3nVb82VfnRgp9BaUrkq', '0921230013', NULL, 'TICKET_SALES_STAFF', 'ACTIVE', 23, 1, 1, GETDATE()),
(N'Lâm Quỳnh Như', 'staff_tk_14', 'staff.tk14@cineflow.vn', '$2a$10$4AeCwdkcGvspefpI3xi8K.F5mdPwC7zGSm3nVb82VfnRgp9BaUrkq', '0921230014', NULL, 'TICKET_CHECKER_STAFF', 'ACTIVE', 23, 1, 1, GETDATE()),
(N'Vũ Đức Hải', 'staff_tk_15', 'staff.tk15@cineflow.vn', '$2a$10$4AeCwdkcGvspefpI3xi8K.F5mdPwC7zGSm3nVb82VfnRgp9BaUrkq', '0921230015', NULL, 'TICKET_SALES_STAFF', 'ACTIVE', 23, 1, 1, GETDATE());
GO
