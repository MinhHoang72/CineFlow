$server = "localhost"
$user   = "sa"
$pass   = "123"
$db     = "CineFlowDB"

$conn = New-Object System.Data.SqlClient.SqlConnection
$conn.ConnectionString = "Server=$server;Database=$db;User Id=$user;Password=$pass;TrustServerCertificate=True;"
$conn.Open()

function Exec($sql) {
    $cmd = $conn.CreateCommand()
    $cmd.CommandText = $sql
    $cmd.ExecuteNonQuery() | Out-Null
}

function ExecScalar($sql) {
    $cmd = $conn.CreateCommand()
    $cmd.CommandText = $sql
    return $cmd.ExecuteScalar()
}

# --- Danh sach rạp can them ---
$branches = @(
    @{ Name="CineFlow Hoàn Kiếm";  Address="88 Hàng Bài, Hoàn Kiếm";             City="Hà Nội";           Phone="02433333333" },
    @{ Name="CineFlow Hai Bà Trưng"; Address="191 Bà Triệu, Hai Bà Trưng";        City="Hà Nội";           Phone="02437777777" },
    @{ Name="CineFlow Tây Hồ";     Address="272 Võ Chí Công, Tây Hồ";             City="Hà Nội";           Phone="02438888888" },
    @{ Name="CineFlow Landmark 81"; Address="Vinhomes Central Park, Bình Thạnh";   City="TP. Hồ Chí Minh"; Phone="02855555555" },
    @{ Name="CineFlow Gò Vấp";     Address="304 Quang Trung, Gò Vấp";             City="TP. Hồ Chí Minh"; Phone="02866666666" },
    @{ Name="CineFlow Thủ Đức";    Address="216 Võ Văn Ngân, TP. Thủ Đức";        City="TP. Hồ Chí Minh"; Phone="02877777777" },
    @{ Name="CineFlow Cẩm Lệ";    Address="100 Cách Mạng Tháng 8, Cẩm Lệ";       City="Đà Nẵng";          Phone="02366666666" }
)

foreach ($b in $branches) {
    $exists = ExecScalar "SELECT COUNT(1) FROM Branches WHERE BranchName = N'$($b.Name)'"
    if ($exists -eq 0) {
        Exec "INSERT INTO Branches (BranchName, Address, City, Phone, Status) VALUES (N'$($b.Name)', N'$($b.Address)', N'$($b.City)', '$($b.Phone)', 'ACTIVE')"
        Write-Host "Inserted branch: $($b.Name)"
    } else {
        Write-Host "Already exists: $($b.Name)"
    }
}

# --- Tao phong chieu cho cac rạp chua co ---
$halls = @(
    @{ Name="Phòng 2D 01"; Type="2D"; Cap=60 },
    @{ Name="Phòng 2D 02"; Type="2D"; Cap=50 },
    @{ Name="Phòng 3D 01"; Type="3D"; Cap=80 }
)

$branchIds = @()
$cmd2 = $conn.CreateCommand()
$cmd2.CommandText = "SELECT BranchID FROM Branches WHERE Status='ACTIVE'"
$reader = $cmd2.ExecuteReader()
while ($reader.Read()) { $branchIds += $reader.GetInt32(0) }
$reader.Close()

foreach ($bid in $branchIds) {
    foreach ($h in $halls) {
        $hexists = ExecScalar "SELECT COUNT(1) FROM Halls WHERE BranchID=$bid AND HallName=N'$($h.Name)'"
        if ($hexists -eq 0) {
            Exec "INSERT INTO Halls (BranchID, HallName, HallType, Capacity, Status) VALUES ($bid, N'$($h.Name)', '$($h.Type)', $($h.Cap), 'AVAILABLE')"
        }
    }
}
Write-Host "Halls created."

# --- Tao ghe cho cac phong chua co ghe ---
$hallIds = @()
$cmd3 = $conn.CreateCommand()
$cmd3.CommandText = "SELECT h.HallID FROM Halls h WHERE NOT EXISTS (SELECT 1 FROM Seats s WHERE s.HallID = h.HallID)"
$reader2 = $cmd3.ExecuteReader()
while ($reader2.Read()) { $hallIds += $reader2.GetInt32(0) }
$reader2.Close()

$rows = @('A','B','C','D','E')
$cols = 1..10
foreach ($hid in $hallIds) {
    foreach ($row in $rows) {
        foreach ($col in $cols) {
            $seatType = if ($row -in @('C','D')) { 'VIP' } elseif ($row -eq 'E' -and $col -in @(5,6)) { 'SWEETBOX' } else { 'STANDARD' }
            Exec "INSERT INTO Seats (HallID, SeatRow, SeatNumber, SeatType, Status) VALUES ($hid, '$row', $col, '$seatType', 'AVAILABLE')"
        }
    }
}
Write-Host "Seats created."

# --- Tao suất chiếu theo ngay hom nay & ngay mai ---
$adminId = ExecScalar "SELECT TOP 1 UserID FROM Users WHERE Role IN ('ADMIN','MANAGER') ORDER BY UserID"
$today   = (Get-Date).ToString("yyyy-MM-dd")
$tomorrow = (Get-Date).AddDays(1).ToString("yyyy-MM-dd")

$showtimes = @(
    # MovieID, BranchName, Date, Time, Price
    @{ M=1;  B="CineFlow Cầu Giấy";    D=$today;    T="10:00"; P=85000 },
    @{ M=1;  B="CineFlow Cầu Giấy";    D=$today;    T="14:30"; P=95000 },
    @{ M=1;  B="CineFlow Cầu Giấy";    D=$today;    T="19:15"; P=110000 },
    @{ M=1;  B="CineFlow Cầu Giấy";    D=$tomorrow; T="11:00"; P=85000 },
    @{ M=1;  B="CineFlow Cầu Giấy";    D=$tomorrow; T="20:00"; P=110000 },
    @{ M=1;  B="CineFlow Hoàn Kiếm";   D=$today;    T="09:30"; P=95000 },
    @{ M=1;  B="CineFlow Hoàn Kiếm";   D=$today;    T="15:45"; P=110000 },
    @{ M=1;  B="CineFlow Hoàn Kiếm";   D=$tomorrow; T="18:30"; P=120000 },
    @{ M=1;  B="CineFlow Landmark 81"; D=$today;    T="13:00"; P=120000 },
    @{ M=1;  B="CineFlow Landmark 81"; D=$today;    T="17:30"; P=140000 },
    @{ M=1;  B="CineFlow Landmark 81"; D=$tomorrow; T="20:15"; P=140000 },
    @{ M=1;  B="CineFlow Quận 1";      D=$today;    T="10:45"; P=105000 },
    @{ M=1;  B="CineFlow Quận 1";      D=$tomorrow; T="19:00"; P=125000 },
    @{ M=1;  B="CineFlow Hải Châu";    D=$today;    T="14:00"; P=85000 },
    @{ M=1;  B="CineFlow Hải Châu";    D=$tomorrow; T="18:45"; P=95000 },

    @{ M=2;  B="CineFlow Hà Đông";     D=$today;    T="11:15"; P=80000 },
    @{ M=2;  B="CineFlow Hà Đông";     D=$today;    T="16:00"; P=90000 },
    @{ M=2;  B="CineFlow Hà Đông";     D=$tomorrow; T="19:30"; P=100000 },
    @{ M=2;  B="CineFlow Tây Hồ";     D=$today;    T="13:45"; P=90000 },
    @{ M=2;  B="CineFlow Tây Hồ";     D=$tomorrow; T="21:00"; P=110000 },
    @{ M=2;  B="CineFlow Gò Vấp";     D=$today;    T="09:15"; P=85000 },
    @{ M=2;  B="CineFlow Gò Vấp";     D=$today;    T="15:30"; P=95000 },
    @{ M=2;  B="CineFlow Gò Vấp";     D=$tomorrow; T="18:15"; P=105000 },
    @{ M=2;  B="CineFlow Thủ Đức";    D=$today;    T="14:20"; P=90000 },
    @{ M=2;  B="CineFlow Thủ Đức";    D=$tomorrow; T="20:30"; P=110000 },
    @{ M=2;  B="CineFlow Cẩm Lệ";    D=$today;    T="15:00"; P=80000 },

    @{ M=4;  B="CineFlow Cầu Giấy";    D=$today;    T="09:00"; P=80000 },
    @{ M=4;  B="CineFlow Cầu Giấy";    D=$today;    T="13:15"; P=85000 },
    @{ M=4;  B="CineFlow Cầu Giấy";    D=$tomorrow; T="10:30"; P=85000 },
    @{ M=4;  B="CineFlow Hà Đông";     D=$today;    T="10:00"; P=80000 },
    @{ M=4;  B="CineFlow Hai Bà Trưng"; D=$today;   T="11:30"; P=90000 },
    @{ M=4;  B="CineFlow Hai Bà Trưng"; D=$tomorrow; T="16:45"; P=100000 },
    @{ M=4;  B="CineFlow Quận 1";      D=$today;    T="09:30"; P=95000 },
    @{ M=4;  B="CineFlow Quận 1";      D=$today;    T="15:00"; P=110000 },
    @{ M=4;  B="CineFlow Quận 1";      D=$tomorrow; T="17:15"; P=120000 },
    @{ M=4;  B="CineFlow Ngô Quyền";   D=$today;    T="10:15"; P=80000 },
    @{ M=4;  B="CineFlow Ngô Quyền";   D=$tomorrow; T="15:30"; P=90000 },

    @{ M=5;  B="CineFlow Hoàn Kiếm";   D=$today;    T="13:00"; P=100000 },
    @{ M=5;  B="CineFlow Hoàn Kiếm";   D=$today;    T="18:15"; P=120000 },
    @{ M=5;  B="CineFlow Hoàn Kiếm";   D=$today;    T="21:30"; P=130000 },
    @{ M=5;  B="CineFlow Hoàn Kiếm";   D=$tomorrow; T="19:45"; P=125000 },
    @{ M=5;  B="CineFlow Hai Bà Trưng"; D=$today;   T="14:15"; P=110000 },
    @{ M=5;  B="CineFlow Hai Bà Trưng"; D=$tomorrow; T="20:30"; P=125000 },
    @{ M=5;  B="CineFlow Landmark 81"; D=$today;    T="11:00"; P=120000 },
    @{ M=5;  B="CineFlow Landmark 81"; D=$today;    T="16:15"; P=140000 },
    @{ M=5;  B="CineFlow Landmark 81"; D=$today;    T="21:00"; P=150000 },
    @{ M=5;  B="CineFlow Landmark 81"; D=$tomorrow; T="18:30"; P=145000 },
    @{ M=5;  B="CineFlow Thủ Đức";    D=$today;    T="17:00"; P=105000 },
    @{ M=5;  B="CineFlow Thủ Đức";    D=$tomorrow; T="19:15"; P=115000 },
    @{ M=5;  B="CineFlow Hải Châu";    D=$today;    T="16:30"; P=95000 },
    @{ M=5;  B="CineFlow Hải Châu";    D=$tomorrow; T="20:15"; P=105000 }
)

$insertCount = 0
foreach ($s in $showtimes) {
    $startDT = "$($s.D) $($s.T):00"
    $hallId = ExecScalar "SELECT TOP 1 h.HallID FROM Halls h JOIN Branches b ON b.BranchID=h.BranchID WHERE b.BranchName=N'$($s.B)' ORDER BY h.HallID"
    if (-not $hallId) { Write-Host "No hall for branch: $($s.B)"; continue }
    $duration = ExecScalar "SELECT Duration FROM Movies WHERE MovieID=$($s.M)"
    $exists = ExecScalar "SELECT COUNT(1) FROM CineFlowDB.dbo.Showtimes WHERE MovieID=$($s.M) AND HallID=$hallId AND StartTime='$startDT'"
    if ($exists -eq 0) {
        Exec "INSERT INTO Showtimes (MovieID, HallID, StartTime, EndTime, CleaningBufferMinutes, BaseTicketPrice, Status, CreatedBy)
              VALUES ($($s.M), $hallId, '$startDT', DATEADD(MINUTE, $duration, '$startDT'), 15, $($s.P), 'SCHEDULED', $adminId)"
        $insertCount++
    }
}
Write-Host "Inserted $insertCount showtimes."

$conn.Close()
Write-Host "Done!"
