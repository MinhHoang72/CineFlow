/* =========================================================
   CINEFLOW - MULTI-BRANCH CINEMA MANAGEMENT SYSTEM
   DATABASE: SQL SERVER
   VERSION: 2.0 - ALIGNED WITH FEATURE TREE
   ========================================================= */

USE master;
GO

IF DB_ID('CineFlowDB') IS NOT NULL
BEGIN
    ALTER DATABASE CineFlowDB SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE CineFlowDB;
END
GO

CREATE DATABASE CineFlowDB;
GO

USE CineFlowDB;
GO


/* =========================================================
   3. USERS - FE-01
   ========================================================= */
CREATE TABLE Users (
    UserID INT IDENTITY(1,1) PRIMARY KEY,
    FullName NVARCHAR(100) NOT NULL,
    Account VARCHAR(50) NOT NULL UNIQUE,
    Email VARCHAR(150) NOT NULL UNIQUE,
    Password VARCHAR(255) NOT NULL,
    Phone VARCHAR(20) NULL,
    Avatar VARCHAR(500) NULL,
    Role VARCHAR(20) NOT NULL
        CONSTRAINT CK_Users_Role CHECK (Role IN ('CUSTOMER','STAFF','MANAGER','ADMIN')),
    Status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    BranchID INT NULL,
    ResetToken VARCHAR(255) NULL,
    ResetTokenExpiry DATETIME2 NULL,
    EmailVerified BIT NOT NULL DEFAULT 0,
    PhoneVerified BIT NOT NULL DEFAULT 0,
    LastLoginAt DATETIME2 NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    UpdatedAt DATETIME2 NULL,
    CONSTRAINT CK_Users_Status CHECK (Status IN ('ACTIVE','INACTIVE','LOCKED'))
);
GO


/* =========================================================
   4. BRANCHES - FE-01.1 / FE-03.1
   ========================================================= */
CREATE TABLE Branches (
    BranchID INT IDENTITY(1,1) PRIMARY KEY,
    BranchName NVARCHAR(150) NOT NULL,
    Address NVARCHAR(300) NOT NULL,
    City NVARCHAR(100) NULL,
    Phone VARCHAR(20) NULL,
    Status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    CreatedAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    UpdatedAt DATETIME2 NULL,
    CONSTRAINT CK_Branches_Status CHECK (Status IN ('ACTIVE','INACTIVE','TEMPORARILY_CLOSED'))
);
GO

ALTER TABLE Users
ADD CONSTRAINT FK_Users_Branch FOREIGN KEY (BranchID) REFERENCES Branches(BranchID);
GO


/* =========================================================
   5. HALLS - FE-03.2
   ========================================================= */
CREATE TABLE Halls (
    HallID INT IDENTITY(1,1) PRIMARY KEY,
    BranchID INT NOT NULL,
    HallName NVARCHAR(100) NOT NULL,
    HallType VARCHAR(20) NOT NULL DEFAULT '2D',
    Capacity INT NOT NULL,
    Status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    CreatedAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_Halls_Branches FOREIGN KEY (BranchID) REFERENCES Branches(BranchID),
    CONSTRAINT CK_Halls_Type CHECK (HallType IN ('2D','3D')),
    CONSTRAINT CK_Halls_Capacity CHECK (Capacity > 0),
    CONSTRAINT CK_Halls_Status CHECK (Status IN ('AVAILABLE','MAINTENANCE','INACTIVE'))
);
GO


/* =========================================================
   6. SEATS - FE-03.3
   ========================================================= */
CREATE TABLE Seats (
    SeatID INT IDENTITY(1,1) PRIMARY KEY,
    HallID INT NOT NULL,
    SeatRow VARCHAR(5) NOT NULL,
    SeatNumber INT NOT NULL,
    SeatType VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    Status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    CONSTRAINT FK_Seats_Halls FOREIGN KEY (HallID) REFERENCES Halls(HallID),
    CONSTRAINT CK_Seats_Type CHECK (SeatType IN ('STANDARD','VIP','SWEETBOX')),
    CONSTRAINT CK_Seats_Status CHECK (Status IN ('AVAILABLE','MAINTENANCE','UNAVAILABLE')),
    CONSTRAINT CK_Seats_Number CHECK (SeatNumber > 0),
    CONSTRAINT UQ_Seat_Position UNIQUE (HallID,SeatRow,SeatNumber)
);
GO


/* =========================================================
   7. MOVIES - FE-01.1 / FE-02
   ========================================================= */
CREATE TABLE Movies (
    MovieID INT IDENTITY(1,1) PRIMARY KEY,
    Title NVARCHAR(200) NOT NULL,
    Description NVARCHAR(MAX) NULL,
    Duration INT NOT NULL,
    Genre NVARCHAR(200) NULL,
    Director NVARCHAR(150) NULL,
    Cast NVARCHAR(1000) NULL,
    Language NVARCHAR(100) NULL,
    ReleaseDate DATE NULL,
    EndDate DATE NULL,
    PosterURL VARCHAR(500) NULL,
    TrailerURL VARCHAR(500) NULL,
    AgeRating VARCHAR(20) NOT NULL DEFAULT 'P',
    Status VARCHAR(20) NOT NULL DEFAULT 'COMING_SOON',
    CreatedAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    UpdatedAt DATETIME2 NULL,
    CONSTRAINT CK_Movies_Duration CHECK (Duration > 0),
    CONSTRAINT CK_Movies_AgeRating CHECK (AgeRating IN ('P','K','T13','T16','T18','C')),
    CONSTRAINT CK_Movies_Status CHECK (Status IN ('COMING_SOON','NOW_SHOWING','ENDED','ARCHIVED')),
    CONSTRAINT CK_Movies_Date CHECK (EndDate IS NULL OR ReleaseDate IS NULL OR EndDate >= ReleaseDate)
);
GO


/* =========================================================
   8. BRANCH MOVIES - FE-02.2
   ========================================================= */
CREATE TABLE BranchMovies (
    BranchMovieID INT IDENTITY(1,1) PRIMARY KEY,
    BranchID INT NOT NULL,
    MovieID INT NOT NULL,
    IsActive BIT NOT NULL DEFAULT 1,
    CreatedAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    UpdatedAt DATETIME2 NULL,
    CONSTRAINT FK_BranchMovies_Branch FOREIGN KEY (BranchID) REFERENCES Branches(BranchID),
    CONSTRAINT FK_BranchMovies_Movie FOREIGN KEY (MovieID) REFERENCES Movies(MovieID),
    CONSTRAINT UQ_Branch_Movie UNIQUE (BranchID,MovieID)
);
GO


/* =========================================================
   8.1. MOVIE DISTRIBUTION SETTINGS
   ========================================================= */
CREATE TABLE MovieDistributionSettings (
    MovieID INT NOT NULL,
    Scope VARCHAR(10) NOT NULL,
    City NVARCHAR(120) NULL,
    UpdatedAt DATETIME NOT NULL CONSTRAINT DF_MovieDistributionSettings_UpdatedAt DEFAULT GETDATE(),
    CONSTRAINT PK_MovieDistributionSettings PRIMARY KEY (MovieID),
    CONSTRAINT CK_MovieDistributionSettings_Scope CHECK (Scope IN ('system', 'city', 'selected')),
    CONSTRAINT FK_MovieDistributionSettings_Movies FOREIGN KEY (MovieID)
        REFERENCES Movies(MovieID) ON DELETE CASCADE
);
GO


/* =========================================================
   8.2. MOVIE DISTRIBUTION EXCEPTIONS
   ========================================================= */
CREATE TABLE MovieDistributionExceptions (
    MovieID INT NOT NULL,
    BranchID INT NOT NULL,
    CONSTRAINT PK_MovieDistributionExceptions PRIMARY KEY (MovieID, BranchID),
    CONSTRAINT FK_MovieDistributionExceptions_Settings FOREIGN KEY (MovieID)
        REFERENCES MovieDistributionSettings(MovieID) ON DELETE CASCADE,
    CONSTRAINT FK_MovieDistributionExceptions_Branches FOREIGN KEY (BranchID)
        REFERENCES Branches(BranchID) ON DELETE CASCADE
);
GO


/* =========================================================
   9. SHOWTIMES - FE-04
   ========================================================= */
CREATE TABLE Showtimes (
    ShowtimeID INT IDENTITY(1,1) PRIMARY KEY,
    MovieID INT NOT NULL,
    HallID INT NOT NULL,
    StartTime DATETIME2 NOT NULL,
    EndTime DATETIME2 NOT NULL,
    CleaningBufferMinutes INT NOT NULL DEFAULT 15,
    BaseTicketPrice DECIMAL(12,2) NOT NULL DEFAULT 0,
    Status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    CreatedBy INT NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    UpdatedAt DATETIME2 NULL,
    CONSTRAINT FK_Showtimes_Movies FOREIGN KEY (MovieID) REFERENCES Movies(MovieID),
    CONSTRAINT FK_Showtimes_Halls FOREIGN KEY (HallID) REFERENCES Halls(HallID),
    CONSTRAINT FK_Showtimes_CreatedBy FOREIGN KEY (CreatedBy) REFERENCES Users(UserID),
    CONSTRAINT CK_Showtimes_Time CHECK (EndTime > StartTime),
    CONSTRAINT CK_Showtimes_Buffer CHECK (CleaningBufferMinutes >= 0),
    CONSTRAINT CK_Showtimes_BasePrice CHECK (BaseTicketPrice >= 0),
    CONSTRAINT CK_Showtimes_Status CHECK (Status IN ('SCHEDULED','ONGOING','COMPLETED','CANCELLED'))
);
GO


/* =========================================================
   10. SURCHARGES - FE-04.2
   ========================================================= */
CREATE TABLE Surcharges (
    SurchargeID INT IDENTITY(1,1) PRIMARY KEY,
    SurchargeName NVARCHAR(100) NOT NULL,
    SurchargeType VARCHAR(20) NOT NULL DEFAULT 'SEAT',
    SeatType VARCHAR(20) NOT NULL,
    Amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    Percentage DECIMAL(5,2) NULL,
    IsActive BIT NOT NULL DEFAULT 1,
    CreatedAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT CK_Surcharges_Type CHECK (SurchargeType = 'SEAT'),
    CONSTRAINT CK_Surcharges_SeatType CHECK (SeatType IN ('VIP','SWEETBOX')),
    CONSTRAINT CK_Surcharges_Amount CHECK (Amount >= 0),
    CONSTRAINT CK_Surcharges_Percentage CHECK (Percentage IS NULL OR (Percentage >= 0 AND Percentage <= 100))
);
GO


/* =========================================================
   13. F&B CATEGORIES
   ========================================================= */

CREATE TABLE FnbCategories (
    CategoryID INT IDENTITY(1,1) PRIMARY KEY,

    CategoryName NVARCHAR(100) NOT NULL,

    Status VARCHAR(20) NOT NULL
        DEFAULT 'ACTIVE',

    CONSTRAINT CK_FnbCategories_Status
        CHECK (Status IN (
            'ACTIVE',
            'INACTIVE'
        ))
);
GO


/* =========================================================
   14. F&B ITEMS
   ========================================================= */

CREATE TABLE FnbItems (
    FnbItemID INT IDENTITY(1,1) PRIMARY KEY,

    CategoryID INT NOT NULL,

    ItemName NVARCHAR(150) NOT NULL,

    Description NVARCHAR(500) NULL,

    Price DECIMAL(12,2) NOT NULL,

    ImageURL VARCHAR(500) NULL,

    Status VARCHAR(20) NOT NULL
        DEFAULT 'ACTIVE',

    CreatedAt DATETIME2 NOT NULL
        DEFAULT GETDATE(),

    UpdatedAt DATETIME2 NULL,

    CONSTRAINT FK_FnbItems_Category
        FOREIGN KEY (CategoryID)
        REFERENCES FnbCategories(CategoryID),

    CONSTRAINT CK_FnbItems_Price
        CHECK (Price >= 0),

    CONSTRAINT CK_FnbItems_Status
        CHECK (Status IN (
            'ACTIVE',
            'INACTIVE'
        ))
);
GO


/* =========================================================
   15. F&B COMBOS
   ========================================================= */

CREATE TABLE FnbCombos (
    ComboID INT IDENTITY(1,1) PRIMARY KEY,

    ComboName NVARCHAR(150) NOT NULL,

    Description NVARCHAR(500) NULL,

    Price DECIMAL(12,2) NOT NULL,

    ImageURL VARCHAR(500) NULL,

    Status VARCHAR(20) NOT NULL
        DEFAULT 'ACTIVE',

    CreatedAt DATETIME2 NOT NULL
        DEFAULT GETDATE(),

    UpdatedAt DATETIME2 NULL,

    CONSTRAINT CK_FnbCombos_Price
        CHECK (Price >= 0),

    CONSTRAINT CK_FnbCombos_Status
        CHECK (Status IN (
            'ACTIVE',
            'INACTIVE'
        ))
);
GO


/* =========================================================
   16. COMBO ITEMS
   ========================================================= */

CREATE TABLE ComboItems (
    ComboItemID INT IDENTITY(1,1) PRIMARY KEY,

    ComboID INT NOT NULL,

    FnbItemID INT NOT NULL,

    Quantity INT NOT NULL,

    CONSTRAINT FK_ComboItems_Combo
        FOREIGN KEY (ComboID)
        REFERENCES FnbCombos(ComboID),

    CONSTRAINT FK_ComboItems_Item
        FOREIGN KEY (FnbItemID)
        REFERENCES FnbItems(FnbItemID),

    CONSTRAINT CK_ComboItems_Quantity
        CHECK (Quantity > 0),

    CONSTRAINT UQ_Combo_Item
        UNIQUE (
            ComboID,
            FnbItemID
        )
);
GO


/* =========================================================
   17. BRANCH F&B INVENTORY
   ========================================================= */

CREATE TABLE BranchFnbInventory (
    InventoryID INT IDENTITY(1,1) PRIMARY KEY,

    BranchID INT NOT NULL,

    FnbItemID INT NOT NULL,

    StockQuantity INT NOT NULL
        DEFAULT 0,

    IsAvailable BIT NOT NULL
        DEFAULT 1,

    UpdatedAt DATETIME2 NOT NULL
        DEFAULT GETDATE(),

    CONSTRAINT FK_Inventory_Branch
        FOREIGN KEY (BranchID)
        REFERENCES Branches(BranchID),

    CONSTRAINT FK_Inventory_Item
        FOREIGN KEY (FnbItemID)
        REFERENCES FnbItems(FnbItemID),

    CONSTRAINT CK_Inventory_Stock
        CHECK (StockQuantity >= 0),

    CONSTRAINT UQ_Branch_Fnb
        UNIQUE (
            BranchID,
            FnbItemID
        )
);
GO



/* =========================================================
   20. VOUCHERS
   ========================================================= */

CREATE TABLE Vouchers (
    VoucherID INT IDENTITY(1,1) PRIMARY KEY,

    VoucherCode VARCHAR(50) NOT NULL UNIQUE,

    VoucherName NVARCHAR(150) NOT NULL,

    Description NVARCHAR(500) NULL,

    DiscountType VARCHAR(20) NOT NULL,

    DiscountValue DECIMAL(12,2) NOT NULL,

    MinimumOrderValue DECIMAL(12,2) NOT NULL
        DEFAULT 0,

    MaximumDiscount DECIMAL(12,2) NULL,

    StartDate DATETIME2 NOT NULL,

    EndDate DATETIME2 NOT NULL,

    UsageLimit INT NULL,

    UsedCount INT NOT NULL
        DEFAULT 0,

    Status VARCHAR(20) NOT NULL
        DEFAULT 'ACTIVE',

    CONSTRAINT CK_Vouchers_Type
        CHECK (DiscountType IN (
            'FIXED',
            'PERCENT'
        )),

    CONSTRAINT CK_Vouchers_Value
        CHECK (DiscountValue >= 0),

    CONSTRAINT CK_Vouchers_Minimum
        CHECK (MinimumOrderValue >= 0),

    CONSTRAINT CK_Vouchers_Maximum
        CHECK (
            MaximumDiscount IS NULL
            OR MaximumDiscount >= 0
        ),

    CONSTRAINT CK_Vouchers_Date
        CHECK (EndDate > StartDate),

    CONSTRAINT CK_Vouchers_Usage
        CHECK (
            UsageLimit IS NULL
            OR UsageLimit >= 0
        ),

    CONSTRAINT CK_Vouchers_Used
        CHECK (UsedCount >= 0),

    CONSTRAINT CK_Vouchers_Status
        CHECK (Status IN (
            'ACTIVE',
            'INACTIVE',
            'EXPIRED'
        ))
);
GO



/* =========================================================
   21. PROMOTIONS
   ========================================================= */

CREATE TABLE Promotions (
    PromotionID INT IDENTITY(1,1) PRIMARY KEY,

    PromotionName NVARCHAR(150) NOT NULL,

    Description NVARCHAR(1000) NULL,

    DiscountPercentage DECIMAL(5,2) NULL,

    StartDate DATETIME2 NOT NULL,

    EndDate DATETIME2 NOT NULL,

    Status VARCHAR(20) NOT NULL
        DEFAULT 'ACTIVE',

    CreatedAt DATETIME2 NOT NULL
        DEFAULT GETDATE(),

    CONSTRAINT CK_Promotions_Discount
        CHECK (
            DiscountPercentage IS NULL
            OR (
                DiscountPercentage >= 0
                AND DiscountPercentage <= 100
            )
        ),

    CONSTRAINT CK_Promotions_Date
        CHECK (EndDate > StartDate),

    CONSTRAINT CK_Promotions_Status
        CHECK (Status IN (
            'ACTIVE',
            'INACTIVE',
            'EXPIRED'
        ))
);
GO



/* =========================================================
   22. BOOKINGS
   ========================================================= */

CREATE TABLE Bookings (
    BookingID INT IDENTITY(1,1) PRIMARY KEY,

    BookingCode VARCHAR(30) NOT NULL UNIQUE,

    UserID INT NULL,

    ShowtimeID INT NOT NULL,

    VoucherID INT NULL,

    BookingType VARCHAR(20) NOT NULL
        DEFAULT 'ONLINE',

    BookingStatus VARCHAR(30) NOT NULL
        DEFAULT 'PENDING',

    SubTotal DECIMAL(12,2) NOT NULL
        DEFAULT 0,

    DiscountAmount DECIMAL(12,2) NOT NULL
        DEFAULT 0,

    TotalAmount DECIMAL(12,2) NOT NULL
        DEFAULT 0,

    CreatedAt DATETIME2 NOT NULL
        DEFAULT GETDATE(),

    UpdatedAt DATETIME2 NULL,

    CONSTRAINT FK_Bookings_User
        FOREIGN KEY (UserID)
        REFERENCES Users(UserID),

    CONSTRAINT FK_Bookings_Showtime
        FOREIGN KEY (ShowtimeID)
        REFERENCES Showtimes(ShowtimeID),

    CONSTRAINT FK_Bookings_Voucher
        FOREIGN KEY (VoucherID)
        REFERENCES Vouchers(VoucherID),

    CONSTRAINT CK_Bookings_Type
        CHECK (BookingType IN (
            'ONLINE',
            'COUNTER'
        )),

    CONSTRAINT CK_Bookings_Status
        CHECK (BookingStatus IN (
            'PENDING',
            'CONFIRMED',
            'COMPLETED',
            'CANCELLED',
            'EXPIRED'
        )),

    CONSTRAINT CK_Bookings_Amount
        CHECK (
            SubTotal >= 0
            AND DiscountAmount >= 0
            AND TotalAmount >= 0
        )
);
GO



/* =========================================================
   23. BOOKING SEATS
   ========================================================= */

CREATE TABLE BookingSeats (
    BookingSeatID INT IDENTITY(1,1) PRIMARY KEY,

    BookingID INT NOT NULL,

    SeatID INT NOT NULL,

    Price DECIMAL(12,2) NOT NULL,

    SeatStatus VARCHAR(20) NOT NULL
        DEFAULT 'HELD',

    HoldUntil DATETIME2 NULL,

    CONSTRAINT FK_BookingSeats_Booking
        FOREIGN KEY (BookingID)
        REFERENCES Bookings(BookingID),

    CONSTRAINT FK_BookingSeats_Seat
        FOREIGN KEY (SeatID)
        REFERENCES Seats(SeatID),

    CONSTRAINT CK_BookingSeats_Price
        CHECK (Price >= 0),

    CONSTRAINT CK_BookingSeats_Status
        CHECK (SeatStatus IN (
            'HELD',
            'CONFIRMED',
            'CANCELLED',
            'RELEASED'
        ))
);
GO



/* =========================================================
   24. TICKETS
   ========================================================= */

CREATE TABLE Tickets (
    TicketID INT IDENTITY(1,1) PRIMARY KEY,

    BookingSeatID INT NOT NULL UNIQUE,

    TicketCode VARCHAR(50) NOT NULL UNIQUE,

    QRCode VARCHAR(500) NOT NULL UNIQUE,

    TicketStatus VARCHAR(20) NOT NULL
        DEFAULT 'VALID',

    CheckedInAt DATETIME2 NULL,

    CheckedInBy INT NULL,

    CreatedAt DATETIME2 NOT NULL
        DEFAULT GETDATE(),

    CONSTRAINT FK_Tickets_BookingSeat
        FOREIGN KEY (BookingSeatID)
        REFERENCES BookingSeats(BookingSeatID),

    CONSTRAINT FK_Tickets_CheckedInBy
        FOREIGN KEY (CheckedInBy)
        REFERENCES Users(UserID),

    CONSTRAINT CK_Tickets_Status
        CHECK (TicketStatus IN (
            'VALID',
            'USED',
            'CANCELLED',
            'EXPIRED'
        ))
);
GO



/* =========================================================
   25. F&B ORDERS
   ========================================================= */

CREATE TABLE FnbOrders (
    FnbOrderID INT IDENTITY(1,1) PRIMARY KEY,

    BookingID INT NULL,

    BranchID INT NOT NULL,

    UserID INT NULL,

    OrderCode VARCHAR(30) NOT NULL UNIQUE,

    OrderType VARCHAR(20) NOT NULL
        DEFAULT 'ONLINE',

    OrderStatus VARCHAR(20) NOT NULL
        DEFAULT 'PENDING',

    TotalAmount DECIMAL(12,2) NOT NULL
        DEFAULT 0,

    CreatedAt DATETIME2 NOT NULL
        DEFAULT GETDATE(),

    UpdatedAt DATETIME2 NULL,

    CONSTRAINT FK_FnbOrders_Booking
        FOREIGN KEY (BookingID)
        REFERENCES Bookings(BookingID),

    CONSTRAINT FK_FnbOrders_Branch
        FOREIGN KEY (BranchID)
        REFERENCES Branches(BranchID),

    CONSTRAINT FK_FnbOrders_User
        FOREIGN KEY (UserID)
        REFERENCES Users(UserID),

    CONSTRAINT CK_FnbOrders_Type
        CHECK (OrderType IN (
            'ONLINE',
            'COUNTER'
        )),

    CONSTRAINT CK_FnbOrders_Status
        CHECK (OrderStatus IN (
            'PENDING',
            'PREPARING',
            'READY',
            'DELIVERED',
            'CANCELLED'
        )),

    CONSTRAINT CK_FnbOrders_Total
        CHECK (TotalAmount >= 0)
);
GO



/* =========================================================
   26. F&B ORDER ITEMS
   ========================================================= */

CREATE TABLE FnbOrderItems (
    FnbOrderItemID INT IDENTITY(1,1) PRIMARY KEY,

    FnbOrderID INT NOT NULL,

    FnbItemID INT NULL,

    ComboID INT NULL,

    Quantity INT NOT NULL,

    UnitPrice DECIMAL(12,2) NOT NULL,

    CONSTRAINT FK_FnbOrderItems_Order
        FOREIGN KEY (FnbOrderID)
        REFERENCES FnbOrders(FnbOrderID),

    CONSTRAINT FK_FnbOrderItems_Item
        FOREIGN KEY (FnbItemID)
        REFERENCES FnbItems(FnbItemID),

    CONSTRAINT FK_FnbOrderItems_Combo
        FOREIGN KEY (ComboID)
        REFERENCES FnbCombos(ComboID),

    CONSTRAINT CK_FnbOrderItems_Quantity
        CHECK (Quantity > 0),

    CONSTRAINT CK_FnbOrderItems_Price
        CHECK (UnitPrice >= 0),

    CONSTRAINT CK_FnbOrderItems_Product
        CHECK (
            (
                FnbItemID IS NOT NULL
                AND ComboID IS NULL
            )
            OR
            (
                FnbItemID IS NULL
                AND ComboID IS NOT NULL
            )
        )
);
GO



/* =========================================================
   27. PAYMENTS
   ========================================================= */

CREATE TABLE Payments (
    PaymentID INT IDENTITY(1,1) PRIMARY KEY,

    BookingID INT NULL,

    FnbOrderID INT NULL,

    TransactionCode VARCHAR(100) NOT NULL UNIQUE,

    PaymentMethod VARCHAR(20) NOT NULL,

    Amount DECIMAL(12,2) NOT NULL,

    PaymentStatus VARCHAR(20) NOT NULL
        DEFAULT 'PENDING',

    PaidAt DATETIME2 NULL,

    CreatedAt DATETIME2 NOT NULL
        DEFAULT GETDATE(),

    CONSTRAINT FK_Payments_Booking
        FOREIGN KEY (BookingID)
        REFERENCES Bookings(BookingID),

    CONSTRAINT FK_Payments_FnbOrder
        FOREIGN KEY (FnbOrderID)
        REFERENCES FnbOrders(FnbOrderID),

    CONSTRAINT CK_Payments_Method
        CHECK (PaymentMethod IN (
            'CASH',
            'QR'
        )),

    CONSTRAINT CK_Payments_Status
        CHECK (PaymentStatus IN (
            'PENDING',
            'PAID',
            'FAILED',
            'CANCELLED',
            'TIMEOUT'
        )),

    CONSTRAINT CK_Payments_Amount
        CHECK (Amount >= 0),

    CONSTRAINT CK_Payments_Target
        CHECK (
            (
                BookingID IS NOT NULL
                AND FnbOrderID IS NULL
            )
            OR
            (
                BookingID IS NULL
                AND FnbOrderID IS NOT NULL
            )
        )
);
GO



/* =========================================================
   29. CHECK-IN LOGS
   ========================================================= */

CREATE TABLE CheckInLogs (
    CheckInID INT IDENTITY(1,1) PRIMARY KEY,

    TicketID INT NOT NULL,

    CheckedBy INT NOT NULL,

    CheckInTime DATETIME2 NOT NULL
        DEFAULT GETDATE(),

    Result VARCHAR(20) NOT NULL,

    Note NVARCHAR(500) NULL,

    CONSTRAINT FK_CheckInLogs_Ticket
        FOREIGN KEY (TicketID)
        REFERENCES Tickets(TicketID),

    CONSTRAINT FK_CheckInLogs_User
        FOREIGN KEY (CheckedBy)
        REFERENCES Users(UserID),

    CONSTRAINT CK_CheckInLogs_Result
        CHECK (Result IN (
            'SUCCESS',
            'REJECTED'
        ))
);
GO



/* =========================================================
   30. VOUCHER USAGES
   ========================================================= */

CREATE TABLE VoucherUsages (
    VoucherUsageID INT IDENTITY(1,1) PRIMARY KEY,

    VoucherID INT NOT NULL,

    UserID INT NOT NULL,

    BookingID INT NOT NULL,

    UsedAt DATETIME2 NOT NULL
        DEFAULT GETDATE(),

    CONSTRAINT FK_VoucherUsage_Voucher
        FOREIGN KEY (VoucherID)
        REFERENCES Vouchers(VoucherID),

    CONSTRAINT FK_VoucherUsage_User
        FOREIGN KEY (UserID)
        REFERENCES Users(UserID),

    CONSTRAINT FK_VoucherUsage_Booking
        FOREIGN KEY (BookingID)
        REFERENCES Bookings(BookingID),

    CONSTRAINT UQ_Voucher_Booking
        UNIQUE (
            VoucherID,
            BookingID
        )
);
GO

/* =========================================================
   31. MOVIE RATINGS
   ========================================================= */
CREATE TABLE MovieRatings (
    RatingID INT IDENTITY(1,1) PRIMARY KEY,
    MovieID INT NOT NULL,
    UserID INT NOT NULL,
    Score INT NOT NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT GETDATE(),
    UpdatedAt DATETIME2 NULL,
    CONSTRAINT FK_MovieRatings_Movie FOREIGN KEY (MovieID) REFERENCES Movies(MovieID) ON DELETE CASCADE,
    CONSTRAINT FK_MovieRatings_User FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    CONSTRAINT CK_MovieRatings_Score CHECK (Score >= 1 AND Score <= 5),
    CONSTRAINT UQ_MovieRatings_UserMovie UNIQUE (MovieID, UserID)
);
GO



/* =========================================================
   11. SAMPLE BRANCHES
   ========================================================= */
INSERT INTO Branches
(BranchName, Address, City, Phone, Status)
VALUES
(N'CineFlow Cầu Giấy', N'123 Cầu Giấy', N'Hà Nội', '02411111111', 'ACTIVE'),
(N'CineFlow Hà Đông', N'456 Nguyễn Trãi', N'Hà Nội', '02422222222', 'ACTIVE');
GO


/* =========================================================
   12. SAMPLE USERS
   ========================================================= */
INSERT INTO Users
(FullName, Account, Email, Password, Phone, Role, Status, BranchID, EmailVerified)
VALUES
(N'Nguyễn Văn Customer', 'customer', 'customer@gmail.com', '123456', '0900000001', 'CUSTOMER', 'ACTIVE', NULL, 1),
(N'Nguyễn Văn Staff',    'staff',    'staff@gmail.com',    '123456', '0900000002', 'STAFF',    'ACTIVE', 1, 1),
(N'Nguyễn Văn Manager',  'manager',  'manager@gmail.com',  '123456', '0900000003', 'MANAGER',  'ACTIVE', NULL, 1),
(N'Administrator',       'admin',    'admin@gmail.com',    '123456', '0900000004', 'ADMIN',    'ACTIVE', NULL, 1);
GO


/* =========================================================
   13. SAMPLE HALLS
   ========================================================= */
INSERT INTO Halls
(BranchID, HallName, HallType, Capacity, Status)
VALUES
(1, N'Phòng 01', '2D', 60, 'AVAILABLE'),
(1, N'Phòng 02', '2D', 50, 'AVAILABLE'),
(2, N'Phòng 01', '2D', 60, 'AVAILABLE'),
(2, N'Phòng 02', '3D', 70, 'AVAILABLE');
GO


/* =========================================================
   14. SAMPLE SEATS
   ========================================================= */
INSERT INTO Seats (HallID, SeatRow, SeatNumber, SeatType, Status)
VALUES
(1,'A',1,'STANDARD','AVAILABLE'), (1,'A',2,'STANDARD','AVAILABLE'), (1,'A',3,'STANDARD','AVAILABLE'),
(1,'B',1,'STANDARD','AVAILABLE'), (1,'B',2,'STANDARD','AVAILABLE'), (1,'B',3,'STANDARD','AVAILABLE'),
(2,'A',1,'VIP','AVAILABLE'), (2,'A',2,'VIP','AVAILABLE'), (2,'A',3,'VIP','AVAILABLE'),
(2,'B',1,'SWEETBOX','AVAILABLE'), (2,'B',2,'SWEETBOX','AVAILABLE'),
(3,'A',1,'STANDARD','AVAILABLE'), (3,'A',2,'STANDARD','AVAILABLE'), (3,'A',3,'STANDARD','AVAILABLE'),
(4,'A',1,'VIP','AVAILABLE'), (4,'A',2,'VIP','AVAILABLE'), (4,'A',3,'VIP','AVAILABLE');
GO


/* =========================================================
   15. MOVIES (15 PHIM BOM TẤN CHUẨN HÓA VÀ POSTER CỤC BỘ)
   ========================================================= */

INSERT INTO Movies
(
    Title,
    Description,
    Duration,
    Genre,
    Director,
    Cast,
    Language,
    ReleaseDate,
    EndDate,
    PosterURL,
    TrailerURL,
    AgeRating,
    Status
)
VALUES
(
    N'Avatar: Dòng Chảy Của Nước',
    N'Hơn một thập kỷ sau các sự kiện của phần đầu tiên, Avatar: Dòng Chảy Của Nước kể câu chuyện về gia đình Sully, những rắc rối bám theo họ, những nỗ lực để giữ an toàn cho nhau và những trận chiến sinh tử để bảo vệ giống loài Na''vi.',
    192,
    N'Khoa Học Viễn Tưởng, Hành Động, Phiêu Lưu',
    N'James Cameron',
    N'Sam Worthington, Zoe Saldana, Sigourney Weaver, Stephen Lang, Kate Winslet',
    N'Tiếng Anh - Phụ đề Tiếng Việt',
    '2022-12-16',
    '2026-12-31',
    '/Cine_Flow/uploads/movies/poster-avatar-2.jpg',
    'https://www.youtube.com/watch?v=d9MyW72ELq0',
    'T13',
    'NOW_SHOWING'
),
(
    N'Inception (Kẻ Đánh Cắp Giấc Mơ)',
    N'Dom Cobb là một đạo chích bậc thầy chuyên đánh cắp các bí mật sâu kín nhất trong tiềm thức khi đối tượng đang ngủ mơ. Cơ hội chuộc lại cuộc đời đến khi anh nhận nhiệm vụ bất khả thi: cấy ghép một ý tưởng vào tâm trí người khác thay vì đánh cắp.',
    148,
    N'Khoa Học Viễn Tưởng, Hành Động, Giật Gân',
    N'Christopher Nolan',
    N'Leonardo DiCaprio, Joseph Gordon-Levitt, Elliot Page, Tom Hardy, Ken Watanabe, Cillian Murphy',
    N'Tiếng Anh - Phụ đề Tiếng Việt',
    '2010-07-16',
    '2026-12-31',
    '/Cine_Flow/uploads/movies/poster-inception.jpg',
    'https://www.youtube.com/watch?v=YoHD9XEInc0',
    'T16',
    'NOW_SHOWING'
),
(
    N'Địa Đạo: Mặt Trời Trong Bóng Tối',
    N'Tác phẩm điện ảnh lịch sử quy mô lớn tái hiện cuộc sống kiên cường và tinh thần chiến đấu bất khuất của quân và dân Củ Chi sâu trong mê cung lòng đất anh hùng thời kháng chiến chống Mỹ cứu nước.',
    125,
    N'Chiến Tranh, Lịch Sử, Hành Động',
    N'Bùi Thạc Chuyên',
    N'Thái Hòa, Quang Tuấn, Hồ Thu Anh, Diễm Hằng',
    N'Tiếng Việt',
    '2025-04-30',
    '2026-12-31',
    '/Cine_Flow/uploads/movies/poster-dia-dao.jpg',
    'https://www.youtube.com/watch?v=3g8z4b8mNlA',
    'T16',
    'COMING_SOON'
),
(
    N'Kẻ Trộm Mặt Trăng 4 (Despicable Me 4)',
    N'Gia đình Gru và Lucy chào đón quý tử Gru Jr., cậu bé chuyên gây khó dễ cho bố. Cuộc sống êm đềm bị đảo lộn khi kẻ thù Maxime Le Mal trốn thoát và tuyên thệ trả thù, buộc cả gia đình phải đi lánh nạn.',
    94,
    N'Hoạt Hình, Hài Hước, Gia Đình, Phiêu Lưu',
    N'Chris Renaud',
    N'Steve Carell, Kristen Wiig, Will Ferrell, Joey King, Sofia Vergara',
    N'Tiếng Anh - Lồng tiếng & Phụ đề Tiếng Việt',
    '2024-07-05',
    '2026-12-31',
    '/Cine_Flow/uploads/movies/poster-despicable-me-4.jpg',
    'https://www.youtube.com/watch?v=qQlr9-rF32A',
    'P',
    'NOW_SHOWING'
),
(
    N'Deadpool & Wolverine',
    N'Wade Wilson đang tận hưởng cuộc sống bình lặng thì tổ chức TVA kéo anh vào sứ mệnh giải cứu vũ trụ của mình khỏi sự diệt vong, buộc anh phải hợp tác cùng phiên bản bất cần đời của Wolverine.',
    128,
    N'Hành Động, Hài Hước, Khoa Học Viễn Tưởng',
    N'Shawn Levy',
    N'Ryan Reynolds, Hugh Jackman, Emma Corrin, Matthew Macfadyen, Morena Baccarin',
    N'Tiếng Anh - Phụ đề Tiếng Việt',
    '2024-07-26',
    '2026-12-31',
    '/Cine_Flow/uploads/movies/poster-deadpool-wolverine.jpg',
    'https://www.youtube.com/watch?v=73_1biulkYk',
    'T18',
    'NOW_SHOWING'
),
(
    N'Dune: Hành Tinh Cát - Phần 2',
    N'Paul Atreides đồng hành cùng Chani và tộc người Fremen trên sa mạc Arrakis nhằm trả thù những kẻ đã âm mưu tiêu diệt gia tộc Atreides, đồng thời gánh vác sứ mệnh định đoạt tương lai toàn cõi thiên hà.',
    166,
    N'Khoa Học Viễn Tưởng, Phiêu Lưu, Hành Động',
    N'Denis Villeneuve',
    N'Timothée Chalamet, Zendaya, Rebecca Ferguson, Javier Bardem, Austin Butler, Florence Pugh',
    N'Tiếng Anh - Phụ đề Tiếng Việt',
    '2024-03-01',
    '2026-12-31',
    '/Cine_Flow/uploads/movies/poster-dune-2.jpg',
    'https://www.youtube.com/watch?v=Way9Dexny3w',
    'T16',
    'NOW_SHOWING'
),
(
    N'Godzilla x Kong: Đế Chế Mới',
    N'Hai quái thú huyền thoại Godzilla và Kong phải gạt bỏ hiềm khích để cùng liên minh chống lại mối hiểm họa diệt vong mới nổi lên từ lòng Trái Đất Rỗng, đe dọa sự tồn vong của cả giống loài titan và nhân loại.',
    115,
    N'Hành Động, Khoa Học Viễn Tưởng, Phiêu Lưu',
    N'Adam Wingard',
    N'Rebecca Hall, Brian Tyree Henry, Dan Stevens, Kaylee Hottle, Alex Ferns',
    N'Tiếng Anh - Phụ đề Tiếng Việt',
    '2024-03-29',
    '2026-12-31',
    '/Cine_Flow/uploads/movies/poster-godzilla-kong.jpg',
    'https://www.youtube.com/watch?v=lV1OOlGwExg',
    'T13',
    'NOW_SHOWING'
),
(
    N'Transformers Một (Transformers One)',
    N'Bộ phim hoạt hình bom tấn hé lộ nguồn gốc sâu xa mối quan hệ hữu nghị keo sơn giữa Orion Pax và D-16 trên hành tinh Cybertron trước khi họ trở thành tử thù Optimus Prime và Megatron.',
    104,
    N'Hoạt Hình, Khoa Học Viễn Tưởng, Hành Động',
    N'Josh Cooley',
    N'Chris Hemsworth, Brian Tyree Henry, Scarlett Johansson, Keegan-Michael Key, Steve Buscemi',
    N'Tiếng Anh - Lồng tiếng & Phụ đề Tiếng Việt',
    '2024-09-20',
    '2026-12-31',
    '/Cine_Flow/uploads/movies/poster-transformers-one.jpg',
    'https://www.youtube.com/watch?v=u2NuUVuuWO4',
    'K',
    'COMING_SOON'
),
(
    N'Mai',
    N'Chuyện tình đầy trắc trở giữa Mai - một phụ nữ làm nghề massage trị liệu chịu nhiều định kiến xã hội với Dương - chàng thiếu gia lãng tử mê âm nhạc. Khi quá khứ đau thương ùa về, liệu tình yêu có đủ lớn để vượt qua rào cản?',
    131,
    N'Tâm Lý, Tình Cảm, Chính Kịch',
    N'Trấn Thành',
    N'Phương Anh Đào, Tuấn Trần, Trấn Thành, Hồng Đào, Uyển Ân, Quốc Khánh',
    N'Tiếng Việt',
    '2024-02-10',
    '2026-12-31',
    '/Cine_Flow/uploads/movies/poster-mai.jpg',
    'https://www.youtube.com/watch?v=845b4B1u4n0',
    'T18',
    'NOW_SHOWING'
),
(
    N'Oppenheimer',
    N'Bộ phim lịch sử đỉnh cao tái hiện cuộc đời thăng trầm của J. Robert Oppenheimer, nhà vật lý chỉ huy dự án Manhattan phát minh ra bom nguyên tử, và những giằng xé lương tâm khi thay đổi vĩnh viễn vận mệnh nhân loại.',
    180,
    N'Lịch Sử, Chính Kịch, Tiểu Sử',
    N'Christopher Nolan',
    N'Cillian Murphy, Emily Blunt, Matt Damon, Robert Downey Jr., Florence Pugh',
    N'Tiếng Anh - Phụ đề Tiếng Việt',
    '2023-07-21',
    '2026-12-31',
    '/Cine_Flow/uploads/movies/poster-oppenheimer.jpg',
    'https://www.youtube.com/watch?v=uYPbbksJxIg',
    'T18',
    'NOW_SHOWING'
),
(
    N'Kung Fu Panda 4',
    N'Po chuẩn bị trở thành Thủ lĩnh Tinh thần của Thung lũng Bình Yên và cần tìm kiếm người kế vị Thần Long Đại Hiệp. Đúng lúc này, mụ phù thủy Tắc Kè Bông với thuật biến hình quỷ quyệt xuất hiện đe dọa hòa bình thế giới.',
    94,
    N'Hoạt Hình, Hành Động, Hài Hước, Gia Đình',
    N'Mike Mitchell',
    N'Jack Black, Awkwafina, Viola Davis, Dustin Hoffman, Bryan Cranston, James Hong',
    N'Tiếng Anh - Lồng tiếng & Phụ đề Tiếng Việt',
    '2024-03-08',
    '2026-12-31',
    '/Cine_Flow/uploads/movies/poster-kung-fu-panda-4.jpg',
    'https://www.youtube.com/watch?v=_inKs4eeHiI',
    'P',
    'NOW_SHOWING'
),
(
    N'Lật Mặt 7: Một Điều Ước',
    N'Câu chuyện cảm động về bà Hai 73 tuổi sống cô quạnh tại vùng quê hẻo lánh và 5 người con đã lập gia đình ở tứ xứ. Khi người mẹ gặp nạn gãy chân, cuộc họp gia đình đầy éo le mở ra những góc khuất tình thân sâu sắc.',
    138,
    N'Gia Đình, Chính Kịch, Hài Hước',
    N'Lý Hải',
    N'Thanh Hiền, Trương Minh Cường, Đinh Y Nhung, Quách Ngọc Tuyên, Trâm Anh',
    N'Tiếng Việt',
    '2024-04-26',
    '2026-12-31',
    '/Cine_Flow/uploads/movies/poster-lat-mat-7.jpg',
    'https://www.youtube.com/watch?v=d_k8kI0u_pE',
    'K',
    'NOW_SHOWING'
),
(
    N'Những Mảnh Ghép Cảm Xúc 2 (Inside Out 2)',
    N'Riley bước vào tuổi dậy thì cũng là lúc tổng hành dinh tâm trí xuất hiện các cảm xúc vị thành niên phức tạp: Lo Âu, Ganh Tị, Xấu Hổ và Chán Nản. Một cuộc đại chiến cảm xúc bùng nổ để định hình bản ngã của cô bé.',
    96,
    N'Hoạt Hình, Hài Hước, Gia Đình, Phiêu Lưu',
    N'Kelsey Mann',
    N'Amy Poehler, Maya Hawke, Kensington Tallman, Liza Lapira, Tony Hale, Phyllis Smith',
    N'Tiếng Anh - Lồng tiếng & Phụ đề Tiếng Việt',
    '2024-06-14',
    '2026-12-31',
    '/Cine_Flow/uploads/movies/poster-inside-out-2.jpg',
    'https://www.youtube.com/watch?v=LEjhY15eCx0',
    'P',
    'NOW_SHOWING'
),
(
    N'Đất Rừng Phương Nam',
    N'Hành trình tìm cha giàu cảm xúc của cậu bé An giữa vùng sông nước Nam Bộ trù phú và hào sảng đầu thế kỷ 20, nơi tôi luyện ý chí kiên cường và lòng yêu quê hương đất nước qua bao thăng trầm thời cuộc.',
    134,
    N'Phiêu Lưu, Lịch Sử, Chính Kịch',
    N'Nguyễn Quang Dũng',
    N'Hạo Khang, Tuấn Trần, Trấn Thành, Tiến Luật, Băng Di, Hứa Vĩ Văn',
    N'Tiếng Việt',
    '2023-10-20',
    '2026-12-31',
    '/Cine_Flow/uploads/movies/poster-dat-rung-phuong-nam.jpg',
    'https://www.youtube.com/watch?v=hkn9tKIb3w4',
    'K',
    'ENDED'
),
(
    N'Bố Già',
    N'Chuyện đời thường nhật đầy cảm xúc và tiếng cười nơi con hẻm nghèo Sài Gòn của ba Sang và cậu con trai Quắn. Sự hy sinh thầm lặng của bậc làm cha và những bất đồng thế hệ khắc họa nên tình phụ tử thiêng liêng.',
    128,
    N'Gia Đình, Hài Hước, Chính Kịch',
    N'Trấn Thành, Vũ Ngọc Đãng',
    N'Trấn Thành, Tuấn Trần, Ngân Chi, NSND Ngọc Giàu, Lê Giang, Lan Phương, Hoàng Mèo',
    N'Tiếng Việt',
    '2021-03-05',
    '2026-12-31',
    '/Cine_Flow/uploads/movies/poster-bo-gia.jpg',
    'https://www.youtube.com/watch?v=jluSu8Rw6YE',
    'T13',
    'ENDED'
);
GO



/* =========================================================
   16. BRANCH MOVIES
   ========================================================= */

INSERT INTO BranchMovies
(
    BranchID,
    MovieID,
    IsActive
)
VALUES
(1, 1, 1),
(1, 2, 1),
(2, 1, 1),
(2, 2, 1);
GO



/* =========================================================
   17. SAMPLE SHOWTIMES
   ========================================================= */
INSERT INTO Showtimes
(MovieID, HallID, StartTime, EndTime, CleaningBufferMinutes, BaseTicketPrice, Status, CreatedBy)
VALUES
(1,1,'2026-09-15 18:00:00','2026-09-15 21:12:00',15,80000,'SCHEDULED',3),
(2,2,'2026-09-15 19:00:00','2026-09-15 21:28:00',15,80000,'SCHEDULED',3),
(1,4,'2026-09-15 20:00:00','2026-09-15 23:12:00',15,100000,'SCHEDULED',3);
GO


/* =========================================================
   18. SAMPLE SURCHARGES
   ========================================================= */
INSERT INTO Surcharges
(SurchargeName, SurchargeType, SeatType, Amount, Percentage)
VALUES
(N'Ghế VIP', 'SEAT', 'VIP', 20000, NULL),
(N'Ghế Sweetbox', 'SEAT', 'SWEETBOX', 100000, NULL);
GO


/* =========================================================
   42. F&B CATEGORIES
   ========================================================= */

INSERT INTO FnbCategories
(
    CategoryName,
    Status
)
VALUES
(N'Popcorn', 'ACTIVE'),
(N'Đồ uống', 'ACTIVE'),
(N'Snack', 'ACTIVE');
GO


/* =========================================================
   43. F&B ITEMS
   ========================================================= */

INSERT INTO FnbItems
(
    CategoryID,
    ItemName,
    Description,
    Price,
    Status
)
VALUES
(
    1,
    N'Bắp rang bơ',
    N'Bắp rang bơ truyền thống',
    50000,
    'ACTIVE'
),
(
    1,
    N'Bắp caramel',
    N'Bắp caramel ngọt',
    60000,
    'ACTIVE'
),
(
    2,
    N'Coca Cola',
    N'Nước ngọt Coca Cola',
    35000,
    'ACTIVE'
),
(
    2,
    N'Sprite',
    N'Nước ngọt Sprite',
    35000,
    'ACTIVE'
),
(
    3,
    N'Snack khoai tây',
    N'Snack khoai tây',
    30000,
    'ACTIVE'
);
GO


/* =========================================================
   44. F&B COMBOS
   ========================================================= */

INSERT INTO FnbCombos
(
    ComboName,
    Description,
    Price,
    Status
)
VALUES
(
    N'Combo Couple',
    N'1 bắp + 2 nước',
    110000,
    'ACTIVE'
),
(
    N'Combo Family',
    N'2 bắp + 4 nước',
    200000,
    'ACTIVE'
);
GO


/* =========================================================
   45. COMBO ITEMS
   ========================================================= */

INSERT INTO ComboItems
(
    ComboID,
    FnbItemID,
    Quantity
)
VALUES
(1, 1, 1),
(1, 3, 2),
(2, 1, 2),
(2, 3, 4);
GO


/* =========================================================
   46. BRANCH F&B INVENTORY
   ========================================================= */

INSERT INTO BranchFnbInventory
(
    BranchID,
    FnbItemID,
    StockQuantity,
    IsAvailable
)
VALUES
(1, 1, 100, 1),
(1, 2, 100, 1),
(1, 3, 200, 1),
(1, 4, 200, 1),
(1, 5, 100, 1),

(2, 1, 100, 1),
(2, 2, 100, 1),
(2, 3, 200, 1),
(2, 4, 200, 1),
(2, 5, 100, 1);
GO



/* =========================================================
   49. VOUCHERS
   ========================================================= */

INSERT INTO Vouchers
(
    VoucherCode,
    VoucherName,
    Description,
    DiscountType,
    DiscountValue,
    MinimumOrderValue,
    MaximumDiscount,
    StartDate,
    EndDate,
    UsageLimit,
    Status
)
VALUES
(
    'WELCOME10',
    N'Giảm 10% cho khách hàng mới',
    N'Voucher chào mừng',
    'PERCENT',
    10,
    100000,
    50000,
    '2026-01-01',
    '2026-12-31',
    1000,
    'ACTIVE'
),
(
    'SAVE50000',
    N'Giảm 50.000đ',
    N'Voucher giảm trực tiếp',
    'FIXED',
    50000,
    200000,
    50000,
    '2026-01-01',
    '2026-12-31',
    500,
    'ACTIVE'
);
GO



/* =========================================================
   50. PROMOTIONS
   ========================================================= */

INSERT INTO Promotions
(
    PromotionName,
    Description,
    DiscountPercentage,
    StartDate,
    EndDate,
    Status
)
VALUES
(
    N'Thứ 4 vui vẻ',
    N'Giảm giá vé vào thứ 4',
    10,
    '2026-01-01',
    '2026-12-31',
    'ACTIVE'
),
(
    N'Weekend Cinema',
    N'Ưu đãi cuối tuần',
    5,
    '2026-01-01',
    '2026-12-31',
    'ACTIVE'
);
GO



/* =========================================================
   25. INDEXES
   ========================================================= */
CREATE INDEX IX_Users_Role ON Users(Role);
CREATE INDEX IX_Users_BranchID ON Users(BranchID);
CREATE INDEX IX_Branches_City ON Branches(City);
CREATE INDEX IX_Halls_BranchID ON Halls(BranchID);
CREATE INDEX IX_Seats_HallID ON Seats(HallID);
CREATE INDEX IX_Showtimes_MovieID ON Showtimes(MovieID);
CREATE INDEX IX_Showtimes_HallID ON Showtimes(HallID);
CREATE INDEX IX_Showtimes_StartTime ON Showtimes(StartTime);
CREATE INDEX IX_Bookings_UserID ON Bookings(UserID);
CREATE INDEX IX_Bookings_ShowtimeID ON Bookings(ShowtimeID);
CREATE INDEX IX_Tickets_TicketCode ON Tickets(TicketCode);
CREATE INDEX IX_Payments_TransactionCode ON Payments(TransactionCode);
GO


/* =========================================================
   26. DATABASE CHECK
   ========================================================= */
SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE='BASE TABLE' ORDER BY TABLE_NAME;
GO

SELECT UserID, FullName, Account, Email, Role, Status, BranchID FROM Users ORDER BY UserID;
GO

SELECT b.BranchName, h.HallName, h.HallType, h.Capacity
FROM Branches b
JOIN Halls h ON b.BranchID = h.BranchID
ORDER BY b.BranchID, h.HallID;
GO

SELECT s.ShowtimeID, m.Title AS Movie, b.BranchName, h.HallName, h.HallType,
       s.StartTime, s.EndTime, s.BaseTicketPrice, s.Status
FROM Showtimes s
JOIN Movies m ON s.MovieID = m.MovieID
JOIN Halls h ON s.HallID = h.HallID
JOIN Branches b ON h.BranchID = b.BranchID
ORDER BY s.StartTime;
GO
USE CineFlowDB;
GO

/* =====================================================
   1. THÊM CHI NHÁNH
   ===================================================== */

INSERT INTO Branches
(BranchName, Address, City, Phone, Status)
SELECT v.BranchName, v.Address, v.City, v.Phone, 'ACTIVE'
FROM
(
    VALUES
    (N'CineFlow Hoàn Kiếm', N'88 Hàng Bài, Hoàn Kiếm', N'Hà Nội', '02433333333'),
    (N'CineFlow Quận 1', N'120 Nguyễn Huệ, Quận 1', N'TP. Hồ Chí Minh', '02844444444'),
    (N'CineFlow Hải Châu', N'50 Bạch Đằng, Hải Châu', N'Đà Nẵng', '02365555555'),
    (N'CineFlow Ngô Quyền', N'20 Lạch Tray, Ngô Quyền', N'Hải Phòng', '02256666666')
) v(BranchName, Address, City, Phone)
WHERE NOT EXISTS
(
    SELECT 1
    FROM Branches b
    WHERE b.BranchName = v.BranchName
);
GO


/* =====================================================
   2. TÀI KHOẢN MANAGER VÀ STAFF
   Mật khẩu đăng nhập: 123456
   ===================================================== */

INSERT INTO Users
(FullName, Account, Email, Password, Phone, Role, Status, BranchID, EmailVerified)
SELECT
    v.FullName,
    v.Account,
    v.Email,
    '123456',
    v.Phone,
    v.Role,
    'ACTIVE',
    b.BranchID,
    1
FROM
(
    VALUES
    (
        N'Trần Minh Quản Lý',
        'manager_hanoi',
        'manager.hanoi@cineflow.vn',
        '0911000001',
        'MANAGER',
        N'CineFlow Hoàn Kiếm'
    ),
    (
        N'Lê Hoàng Nhân Viên',
        'staff_caugiay',
        'staff.caugiay@cineflow.vn',
        '0911000002',
        'STAFF',
        N'CineFlow Cầu Giấy'
    ),
    (
        N'Phạm Ngọc Nhân Viên',
        'staff_hadong',
        'staff.hadong@cineflow.vn',
        '0911000003',
        'STAFF',
        N'CineFlow Hà Đông'
    ),
    (
        N'Nguyễn Thu Nhân Viên',
        'staff_quan1',
        'staff.quan1@cineflow.vn',
        '0911000004',
        'STAFF',
        N'CineFlow Quận 1'
    )
) v(FullName, Account, Email, Phone, Role, BranchName)
JOIN Branches b ON b.BranchName = v.BranchName
WHERE NOT EXISTS
(
    SELECT 1
    FROM Users u
    WHERE u.Account = v.Account
       OR u.Email = v.Email
);
GO


/* =====================================================
   3. PHIM ĐÃ ĐƯỢC CHUẨN HÓA VÀ THÊM TRỌN VẸN TẠI MỤC 15
   ===================================================== */
GO


/* =====================================================
   4. PHÂN PHỐI PHIM ĐẾN CÁC TRUNG TÂM
   ===================================================== */

INSERT INTO BranchMovies
(BranchID, MovieID, IsActive)
SELECT b.BranchID, m.MovieID, 1
FROM Branches b
CROSS JOIN Movies m
WHERE m.Title IN
(
    N'Địa Đạo: Mặt Trời Trong Bóng Tối',
    N'Kẻ Trộm Mặt Trăng 4 (Despicable Me 4)',
    N'Deadpool & Wolverine'
)
AND b.Status = 'ACTIVE'
AND NOT EXISTS
(
    SELECT 1
    FROM BranchMovies bm
    WHERE bm.BranchID = b.BranchID
      AND bm.MovieID = m.MovieID
);
GO


/* =====================================================
   5. THÊM PHÒNG CHIẾU CHO CÁC CHI NHÁNH MỚI
   ===================================================== */

INSERT INTO Halls
(BranchID, HallName, HallType, Capacity, Status)
SELECT b.BranchID, v.HallName, v.HallType, v.Capacity, 'AVAILABLE'
FROM
(
    VALUES
    (N'CineFlow Hoàn Kiếm', N'Phòng Premium 01', '2D', 80),
    (N'CineFlow Hoàn Kiếm', N'Phòng IMAX 01', '3D', 120),
    (N'CineFlow Quận 1', N'Phòng Premium 01', '2D', 90),
    (N'CineFlow Hải Châu', N'Phòng 01', '2D', 70),
    (N'CineFlow Ngô Quyền', N'Phòng 01', '2D', 70)
) v(BranchName, HallName, HallType, Capacity)
JOIN Branches b ON b.BranchName = v.BranchName
WHERE NOT EXISTS
(
    SELECT 1
    FROM Halls h
    WHERE h.BranchID = b.BranchID
      AND h.HallName = v.HallName
);
GO


/* =====================================================
   6. THÊM SUẤT CHIẾU
   ===================================================== */

INSERT INTO Showtimes
(
    MovieID,
    HallID,
    StartTime,
    EndTime,
    CleaningBufferMinutes,
    BaseTicketPrice,
    Status,
    CreatedBy
)
SELECT
    m.MovieID,
    h.HallID,
    v.StartTime,
    DATEADD(MINUTE, m.Duration, v.StartTime),
    15,
    v.BaseTicketPrice,
    'SCHEDULED',
    u.UserID
FROM
(
    VALUES
    (
        N'Địa Đạo: Mặt Trời Trong Bóng Tối',
        N'CineFlow Hoàn Kiếm',
        N'Phòng Premium 01',
        CAST('2026-10-05 18:00:00' AS DATETIME2),
        CAST(90000 AS DECIMAL(12,2))
    ),
    (
        N'Kẻ Trộm Mặt Trăng 4 (Despicable Me 4)',
        N'CineFlow Hoàn Kiếm',
        N'Phòng IMAX 01',
        CAST('2026-10-10 14:00:00' AS DATETIME2),
        CAST(120000 AS DECIMAL(12,2))
    ),
    (
        N'Deadpool & Wolverine',
        N'CineFlow Quận 1',
        N'Phòng Premium 01',
        CAST('2026-10-06 20:00:00' AS DATETIME2),
        CAST(95000 AS DECIMAL(12,2))
    ),
    (
        N'Địa Đạo: Mặt Trời Trong Bóng Tối',
        N'CineFlow Hải Châu',
        N'Phòng 01',
        CAST('2026-10-07 19:00:00' AS DATETIME2),
        CAST(80000 AS DECIMAL(12,2))
    )
) v(MovieTitle, BranchName, HallName, StartTime, BaseTicketPrice)
JOIN Movies m ON m.Title = v.MovieTitle
JOIN Branches b ON b.BranchName = v.BranchName
JOIN Halls h ON h.BranchID = b.BranchID
           AND h.HallName = v.HallName
LEFT JOIN Users u ON u.Account = 'manager'
WHERE NOT EXISTS
(
    SELECT 1
    FROM Showtimes s
    WHERE s.MovieID = m.MovieID
      AND s.HallID = h.HallID
      AND s.StartTime = v.StartTime
);
GO


/* =====================================================
   7. THÊM ƯU ĐÃI
   ===================================================== */

INSERT INTO Vouchers
(
    VoucherCode,
    VoucherName,
    Description,
    DiscountType,
    DiscountValue,
    MinimumOrderValue,
    MaximumDiscount,
    StartDate,
    EndDate,
    UsageLimit,
    Status
)
VALUES
(
    'CINEFLOW20',
    N'Ưu đãi CineFlow 20%',
    N'Giảm 20% cho đơn hàng từ 150.000đ',
    'PERCENT',
    20,
    150000,
    70000,
    '2026-01-01',
    '2026-12-31',
    1000,
    'ACTIVE'
),
(
    'NEWUSER100',
    N'Khách hàng mới giảm 100.000đ',
    N'Áp dụng cho khách hàng mới',
    'FIXED',
    100000,
    300000,
    100000,
    '2026-01-01',
    '2026-12-31',
    500,
    'ACTIVE'
);
GO


/* =====================================================
   8. KIỂM TRA DỮ LIỆU
   ===================================================== */

SELECT BranchID, BranchName, City, Status
FROM Branches
ORDER BY BranchID;

SELECT UserID, FullName, Account, Role, BranchID, Status
FROM Users
ORDER BY UserID;

SELECT MovieID, Title, AgeRating, Status
FROM Movies
ORDER BY MovieID;

SELECT
    b.BranchName,
    m.Title,
    bm.IsActive
FROM BranchMovies bm
JOIN Branches b ON b.BranchID = bm.BranchID
JOIN Movies m ON m.MovieID = bm.MovieID
ORDER BY b.BranchName, m.Title;

SELECT
    m.Title,
    b.BranchName,
    h.HallName,
    s.StartTime,
    s.EndTime,
    s.BaseTicketPrice
FROM Showtimes s
JOIN Movies m ON m.MovieID = s.MovieID
JOIN Halls h ON h.HallID = s.HallID
JOIN Branches b ON b.BranchID = h.BranchID
ORDER BY s.StartTime;
GO
USE CineFlowDB;
GO

/* =====================================================
   1. TOÀN BỘ PHIM ĐÃ ĐƯỢC CHUẨN HÓA VÀ THÊM TẠI MỤC 15
   ===================================================== */
GO


/* =====================================================
   2. PHÂN PHỐI CÁC PHIM MỚI ĐẾN TOÀN BỘ CHI NHÁNH
   ===================================================== */

INSERT INTO BranchMovies
(BranchID, MovieID, IsActive)
SELECT b.BranchID, m.MovieID, 1
FROM Branches b
CROSS JOIN Movies m
WHERE m.Title IN
(
    N'Mai',
    N'Oppenheimer',
    N'Lật Mặt 7: Một Điều Ước',
    N'Những Mảnh Ghép Cảm Xúc 2 (Inside Out 2)',
    N'Đất Rừng Phương Nam',
    N'Dune: Hành Tinh Cát - Phần 2'
)
AND b.Status = 'ACTIVE'
AND NOT EXISTS
(
    SELECT 1
    FROM BranchMovies bm
    WHERE bm.BranchID = b.BranchID
      AND bm.MovieID = m.MovieID
);
GO


/* =====================================================
   3. THÊM SUẤT CHIẾU CHO PHIM MỚI
   ===================================================== */

INSERT INTO Showtimes
(
    MovieID,
    HallID,
    StartTime,
    EndTime,
    CleaningBufferMinutes,
    BaseTicketPrice,
    Status,
    CreatedBy
)
SELECT
    m.MovieID,
    h.HallID,
    v.StartTime,
    DATEADD(MINUTE, m.Duration, v.StartTime),
    15,
    v.BaseTicketPrice,
    'SCHEDULED',
    u.UserID
FROM
(
    VALUES
    (
        N'Mai',
        N'CineFlow Cầu Giấy',
        N'Phòng 01',
        CAST('2026-10-05 15:00:00' AS DATETIME2),
        CAST(75000 AS DECIMAL(12,2))
    ),
    (
        N'Oppenheimer',
        N'CineFlow Hà Đông',
        N'Phòng 02',
        CAST('2026-10-05 19:30:00' AS DATETIME2),
        CAST(95000 AS DECIMAL(12,2))
    ),
    (
        N'Lật Mặt 7: Một Điều Ước',
        N'CineFlow Quận 1',
        N'Phòng Premium 01',
        CAST('2026-10-06 21:00:00' AS DATETIME2),
        CAST(100000 AS DECIMAL(12,2))
    ),
    (
        N'Dune: Hành Tinh Cát - Phần 2',
        N'CineFlow Hoàn Kiếm',
        N'Phòng Premium 01',
        CAST('2026-10-06 17:00:00' AS DATETIME2),
        CAST(85000 AS DECIMAL(12,2))
    ),
    (
        N'Kung Fu Panda 4',
        N'CineFlow Hải Châu',
        N'Phòng 01',
        CAST('2026-10-20 10:00:00' AS DATETIME2),
        CAST(70000 AS DECIMAL(12,2))
    )
) v(MovieTitle, BranchName, HallName, StartTime, BaseTicketPrice)
JOIN Movies m ON m.Title = v.MovieTitle
JOIN Branches b ON b.BranchName = v.BranchName
JOIN Halls h ON h.BranchID = b.BranchID
           AND h.HallName = v.HallName
LEFT JOIN Users u ON u.Account = 'manager'
WHERE NOT EXISTS
(
    SELECT 1
    FROM Showtimes s
    WHERE s.MovieID = m.MovieID
      AND s.HallID = h.HallID
      AND s.StartTime = v.StartTime
);
GO


/* =====================================================
   4. THÊM GHẾ CHO CÁC PHÒNG CHIẾU
   ===================================================== */

INSERT INTO Seats
(HallID, SeatRow, SeatNumber, SeatType, Status)
SELECT
    h.HallID,
    r.SeatRow,
    n.SeatNumber,
    CASE
        WHEN r.SeatRow IN ('E', 'F') THEN 'VIP'
        ELSE 'STANDARD'
    END,
    'AVAILABLE'
FROM Halls h
CROSS JOIN
(
    VALUES
    ('A'), ('B'), ('C'), ('D'), ('E'), ('F')
) r(SeatRow)
CROSS JOIN
(
    VALUES
    (1), (2), (3), (4), (5),
    (6), (7), (8), (9), (10)
) n(SeatNumber)
WHERE NOT EXISTS
(
    SELECT 1
    FROM Seats s
    WHERE s.HallID = h.HallID
      AND s.SeatRow = r.SeatRow
      AND s.SeatNumber = n.SeatNumber
);
GO


/* =====================================================
   5. THÊM ĐỒ ĂN VÀ ĐỒ UỐNG
   ===================================================== */

INSERT INTO FnbItems
(CategoryID, ItemName, Description, Price, Status)
SELECT
    c.CategoryID,
    v.ItemName,
    v.Description,
    v.Price,
    'ACTIVE'
FROM
(
    VALUES
    (N'Popcorn', N'Bắp phô mai', N'Bắp rang vị phô mai', 65000),
    (N'Popcorn', N'Bắp chocolate', N'Bắp rang vị chocolate', 70000),
    (N'Đồ uống', N'Pepsi', N'Nước ngọt Pepsi', 35000),
    (N'Đồ uống', N'Trà đào', N'Trà đào mát lạnh', 40000),
    (N'Đồ uống', N'Nước suối', N'Nước suối đóng chai', 20000),
    (N'Snack', N'Xúc xích nướng', N'Xúc xích nướng ăn nhanh', 45000),
    (N'Snack', N'Khoai tây chiên', N'Khoai tây chiên giòn', 50000)
) v(CategoryName, ItemName, Description, Price)
JOIN FnbCategories c ON c.CategoryName = v.CategoryName
WHERE NOT EXISTS
(
    SELECT 1
    FROM FnbItems i
    WHERE i.ItemName = v.ItemName
);
GO


/* =====================================================
   6. THÊM COMBO
   ===================================================== */

INSERT INTO FnbCombos
(ComboName, Description, Price, Status)
SELECT
    v.ComboName,
    v.Description,
    v.Price,
    'ACTIVE'
FROM
(
    VALUES
    (
        N'Combo Movie Night',
        N'1 bắp phô mai + 2 nước ngọt',
        125000
    ),
    (
        N'Combo Solo',
        N'1 bắp caramel + 1 nước ngọt',
        85000
    ),
    (
        N'Combo Premium',
        N'2 bắp + 4 nước + 1 snack',
        250000
    )
) v(ComboName, Description, Price)
WHERE NOT EXISTS
(
    SELECT 1
    FROM FnbCombos c
    WHERE c.ComboName = v.ComboName
);
GO


/* =====================================================
   7. CẬP NHẬT TỒN KHO ĐỒ ĂN CHO CÁC CHI NHÁNH
   ===================================================== */

INSERT INTO BranchFnbInventory
(BranchID, FnbItemID, StockQuantity, IsAvailable)
SELECT
    b.BranchID,
    i.FnbItemID,
    150,
    1
FROM Branches b
CROSS JOIN FnbItems i
WHERE b.Status = 'ACTIVE'
AND NOT EXISTS
(
    SELECT 1
    FROM BranchFnbInventory inv
    WHERE inv.BranchID = b.BranchID
      AND inv.FnbItemID = i.FnbItemID
);
GO


/* =====================================================
   8. TẠO LỊCH SỬ ĐẶT VÉ CHO CUSTOMER
   ===================================================== */

DECLARE @CustomerID INT;
DECLARE @ShowtimeID INT;
DECLARE @BookingID INT;
DECLARE @SeatID INT;

SELECT @CustomerID = UserID
FROM Users
WHERE Account = 'customer';

SELECT TOP 1 @ShowtimeID = ShowtimeID
FROM Showtimes
WHERE Status = 'SCHEDULED'
ORDER BY ShowtimeID;

SELECT TOP 1 @SeatID = s.SeatID
FROM Seats s
JOIN Halls h ON h.HallID = s.HallID
JOIN Showtimes st ON st.HallID = h.HallID
WHERE st.ShowtimeID = @ShowtimeID
  AND s.Status = 'AVAILABLE'
ORDER BY s.SeatID;

IF @CustomerID IS NOT NULL
AND @ShowtimeID IS NOT NULL
AND @SeatID IS NOT NULL
AND NOT EXISTS
(
    SELECT 1
    FROM Bookings
    WHERE BookingCode = 'CF-DEMO-0001'
)
BEGIN
    INSERT INTO Bookings
    (
        BookingCode,
        UserID,
        ShowtimeID,
        BookingType,
        BookingStatus,
        SubTotal,
        DiscountAmount,
        TotalAmount
    )
    VALUES
    (
        'CF-DEMO-0001',
        @CustomerID,
        @ShowtimeID,
        'ONLINE',
        'CONFIRMED',
        80000,
        0,
        80000
    );

    SET @BookingID = SCOPE_IDENTITY();

    INSERT INTO BookingSeats
    (
        BookingID,
        SeatID,
        Price,
        SeatStatus
    )
    VALUES
    (
        @BookingID,
        @SeatID,
        80000,
        'CONFIRMED'
    );
END;
GO


/* =====================================================
   9. KIỂM TRA SỐ LƯỢNG DỮ LIỆU
   ===================================================== */

SELECT 'Branches' AS TableName, COUNT(*) AS TotalRows FROM Branches
UNION ALL
SELECT 'Users', COUNT(*) FROM Users
UNION ALL
SELECT 'Movies', COUNT(*) FROM Movies
UNION ALL
SELECT 'BranchMovies', COUNT(*) FROM BranchMovies
UNION ALL
SELECT 'MovieDistributionSettings', COUNT(*) FROM MovieDistributionSettings
UNION ALL
SELECT 'MovieDistributionExceptions', COUNT(*) FROM MovieDistributionExceptions
UNION ALL
SELECT 'Halls', COUNT(*) FROM Halls
UNION ALL
SELECT 'Seats', COUNT(*) FROM Seats
UNION ALL
SELECT 'Showtimes', COUNT(*) FROM Showtimes
UNION ALL
SELECT 'FnbItems', COUNT(*) FROM FnbItems
UNION ALL
SELECT 'FnbCombos', COUNT(*) FROM FnbCombos
UNION ALL
SELECT 'Bookings', COUNT(*) FROM Bookings;
GO