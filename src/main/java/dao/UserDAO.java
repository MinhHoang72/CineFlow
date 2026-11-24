package dao;

import model.dto.booking.BookingHistoryDTO;
import model.entity.User;
import utils.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO: Xử lý truy vấn và cập nhật dữ liệu người dùng (Khách hàng, Nhân viên, Quản lý, Admin),
 * bao gồm đăng nhập, đăng ký, hồ sơ cá nhân và quản lý trạng thái tài khoản.
 */
public class UserDAO {

    private static final Logger LOGGER = Logger.getLogger(UserDAO.class.getName());

    /**
     * Kiểm tra tên tài khoản (username) đã tồn tại trong hệ thống chưa.
     *
     * @param account tên tài khoản cần kiểm tra
     * @return true nếu tài khoản đã tồn tại, ngược lại false
     */
    public boolean isAccountExists(String account) {
        String sql = "SELECT UserID FROM Users WHERE Account = ?";

        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, account);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Kiểm tra địa chỉ email đã tồn tại trong hệ thống chưa.
     *
     * @param email địa chỉ email cần kiểm tra
     * @return true nếu email đã tồn tại, ngược lại false
     */
    public boolean isEmailExists(String email) {
        String sql = "SELECT UserID FROM Users WHERE Email = ?";

        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Đăng ký tài khoản người dùng mới (không kèm OTP xác thực email).
     *
     * @param user thông tin tài khoản người dùng
     * @return true nếu đăng ký thành công, ngược lại false
     */
    public boolean register(User user) {
        return register(user, null, null);
    }

    /**
     * Đăng ký tài khoản người dùng mới kèm mã OTP kích hoạt tài khoản.
     *
     * @param user thông tin tài khoản người dùng
     * @param otp mã OTP kích hoạt
     * @param expiry thời điểm hết hạn của OTP
     * @return true nếu đăng ký thành công, ngược lại false
     */
    public boolean register(User user, String otp, Timestamp expiry) {
        String sql = "INSERT INTO Users "
                + "(FullName, Account, Email, Password, Role, Status, ResetToken, ResetTokenExpiry, EmailVerified) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getAccount());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPassword());
            ps.setString(5, "CUSTOMER");
            ps.setString(6, otp != null ? "INACTIVE" : "ACTIVE");
            ps.setString(7, otp);
            ps.setTimestamp(8, expiry);
            ps.setBoolean(9, otp == null);

            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi chèn dữ liệu đăng ký người dùng mới", e);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Kích hoạt tài khoản người dùng sau khi xác thực email thành công.
     *
     * @param userId mã định danh người dùng
     * @return true nếu kích hoạt thành công, ngược lại false
     */
    public boolean activateUser(int userId) {
        String sql = "UPDATE Users SET Status = 'ACTIVE', EmailVerified = 1, ResetToken = NULL, "
                + "ResetTokenExpiry = NULL, UpdatedAt = GETDATE() WHERE UserID = ?";
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi kích hoạt tài khoản người dùng", e);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Cập nhật mã OTP kích hoạt mới và thời gian hết hạn cho người dùng.
     *
     * @param userId mã định danh người dùng
     * @param otp mã OTP mới
     * @param expiry thời điểm hết hạn OTP
     * @return true nếu cập nhật thành công, ngược lại false
     */
    public boolean updateActivationOtp(int userId, String otp, Timestamp expiry) {
        String sql = "UPDATE Users SET ResetToken = ?, ResetTokenExpiry = ?, UpdatedAt = GETDATE() WHERE UserID = ?";
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, otp);
            ps.setTimestamp(2, expiry);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi cập nhật mã OTP kích hoạt", e);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Cập nhật địa chỉ email và trạng thái xác thực của người dùng sau khi xác
     * thực OTP thành công.
     *
     * @param userId ID người dùng
     * @param newEmail địa chỉ email mới
     * @param verified trạng thái xác thực email
     * @return true nếu cập nhật thành công, ngược lại false
     */
    public boolean updateUserEmail(int userId, String newEmail, boolean verified) {
        String sql = "UPDATE Users SET Email = ?, EmailVerified = ?, ResetToken = NULL, ResetTokenExpiry = NULL, UpdatedAt = GETDATE() WHERE UserID = ?";
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newEmail);
            ps.setBoolean(2, verified);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi cập nhật email người dùng", e);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Tìm thông tin người dùng theo tên tài khoản hoặc địa chỉ email.
     *
     * @param accountOrEmail tên tài khoản hoặc email cần tìm
     * @return đối tượng User nếu tìm thấy, ngược lại null
     */
    public User getUserByAccountOrEmail(String accountOrEmail) {
        String sql = "SELECT u.*, b.CentreName FROM Users u "
                + "LEFT JOIN Centres b ON b.CentreID = u.CentreID "
                + "WHERE u.Account = ? OR u.Email = ?";

        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, accountOrEmail);
            ps.setString(2, accountOrEmail);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Tìm người dùng theo tên tài khoản (username).
     *
     * @param account tên tài khoản cần tìm
     * @return đối tượng User nếu tìm thấy, ngược lại null
     */
    public User getUserByAccount(String account) {
        String sql = "SELECT u.*, b.CentreName FROM Users u "
                + "LEFT JOIN Centres b ON b.CentreID = u.CentreID "
                + "WHERE u.Account = ?";

        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, account);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Tìm người dùng theo địa chỉ email.
     *
     * @param email địa chỉ email cần tìm
     * @return đối tượng User nếu tìm thấy, ngược lại null
     */
    public User getUserByEmail(String email) {
        String sql = "SELECT u.*, b.CentreName FROM Users u "
                + "LEFT JOIN Centres b ON b.CentreID = u.CentreID "
                + "WHERE u.Email = ?";

        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Cập nhật mật khẩu mới cho người dùng.
     *
     * @param userId mã định danh người dùng
     * @param newPassword mật khẩu mới (đã mã hóa)
     * @return true nếu cập nhật thành công, ngược lại false
     */
    public boolean updatePassword(int userId, String newPassword) {
        String sql = "UPDATE Users SET Password = ?, UpdatedAt = GETDATE() WHERE UserID = ?";

        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Lưu mã đặt lại mật khẩu và thời gian hết hạn của mã.
     *
     * @param userId mã người dùng
     * @param token mã xác thực đặt lại mật khẩu
     * @param expiry thời điểm hết hạn của token
     * @return true nếu lưu thành công, ngược lại false
     */
    public boolean saveResetToken(int userId, String token, Timestamp expiry) {
        String sql = "UPDATE Users SET ResetToken = ?, ResetTokenExpiry = ?, UpdatedAt = GETDATE() WHERE UserID = ?";

        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setTimestamp(2, expiry);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Tìm người dùng theo token đặt lại mật khẩu còn hiệu lực.
     *
     * @param token mã đặt lại mật khẩu
     * @return đối tượng User nếu token hợp lệ và còn hạn, ngược lại null
     */
    public User getUserByResetToken(String token) {
        String sql = "SELECT * FROM Users WHERE ResetToken = ? AND ResetTokenExpiry > GETDATE()";

        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Đặt lại mật khẩu người dùng và xóa token đặt lại mật khẩu.
     *
     * @param userId mã định danh người dùng
     * @param newPassword mật khẩu mới đã mã hóa
     * @return true nếu đặt lại mật khẩu thành công, ngược lại false
     */
    public boolean resetPassword(int userId, String newPassword) {
        String sql = "UPDATE Users SET Password = ?, ResetToken = NULL, ResetTokenExpiry = NULL, "
                + "UpdatedAt = GETDATE() WHERE UserID = ?";

        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Chuyển đổi dữ liệu một dòng từ ResultSet sang đối tượng User.
     */
    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();

        user.setUserId(rs.getInt("UserID"));
        user.setFullName(rs.getString("FullName"));
        user.setAccount(rs.getString("Account"));
        user.setEmail(rs.getString("Email"));
        user.setPassword(rs.getString("Password"));
        user.setPhone(rs.getString("Phone"));
        user.setAvatar(rs.getString("Avatar"));
        user.setRole(rs.getString("Role"));
        user.setStatus(rs.getString("Status"));

        try {
            int centreId = rs.getInt("CentreID");
            user.setCentreId(rs.wasNull() ? null : centreId);
        } catch (SQLException e) {
            try {
                int centreId = rs.getInt("CentreID");
                user.setCentreId(rs.wasNull() ? null : centreId);
            } catch (SQLException ignored) {
                user.setCentreId(null);
            }
        }

        user.setCreatedAt(rs.getTimestamp("CreatedAt"));
        user.setUpdatedAt(rs.getTimestamp("UpdatedAt"));
        user.setEmailVerified(rs.getBoolean("EmailVerified"));
        user.setPhoneVerified(rs.getBoolean("PhoneVerified"));
        user.setLastLoginAt(rs.getTimestamp("LastLoginAt"));
        user.setResetToken(rs.getString("ResetToken"));
        user.setResetTokenExpiry(rs.getTimestamp("ResetTokenExpiry"));

        try {
            user.setCentreName(rs.getString("CentreName"));
        } catch (SQLException ignored) {
            try {
                user.setCentreName(rs.getString("CentreName"));
            } catch (SQLException ignored2) {
                // Cột tên cụm rạp không bắt buộc trong các truy vấn không thực hiện JOIN
            }
        }

        return user;
    }

    /**
     * Cập nhật thời điểm đăng nhập gần nhất của người dùng.
     *
     * @param userId mã định danh người dùng
     * @return true nếu cập nhật thành công, ngược lại false
     */
    public boolean updateLastLogin(int userId) {
        String sql = "UPDATE Users SET LastLoginAt = GETDATE(), UpdatedAt = GETDATE() WHERE UserID = ?";

        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Thay đổi mật khẩu tài khoản người dùng.
     *
     * @param userId mã định danh người dùng
     * @param newPassword mật khẩu mới đã mã hóa
     * @return true nếu đổi mật khẩu thành công, ngược lại false
     */
    public boolean changePassword(int userId, String newPassword) {
        String sql = "UPDATE Users SET Password = ?, UpdatedAt = GETDATE() WHERE UserID = ?";

        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Lấy thông tin người dùng theo mã định danh.
     *
     * @param userId mã định danh người dùng
     * @return đối tượng User nếu tìm thấy, ngược lại null
     */
    public User getUserById(int userId) {
        String sql = "SELECT u.*, b.CentreName FROM Users u "
                + "LEFT JOIN Centres b ON b.CentreID = u.CentreID "
                + "WHERE u.UserID = ?";

        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Kiểm tra Manager còn phân công hiện hành tại đúng chi nhánh hay không.
     */
    public boolean hasActiveManagerAssignment(int managerId, int centreId) {
        String sql = "SELECT 1 FROM CentreManagerAssignments "
                + "WHERE ManagerID = ? AND CentreID = ? "
                + "AND Status = 'ACTIVE' AND EndDate IS NULL";
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, managerId);
            ps.setInt(2, centreId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Không thể kiểm tra phân công Manager", e);
            return false;
        }
    }

    /**
     * Kiểm tra địa chỉ email đã được người dùng khác sử dụng hay chưa.
     *
     * @param email địa chỉ email cần kiểm tra
     * @param userId mã người dùng hiện tại (bỏ qua không đối chiếu)
     * @return true nếu email đã thuộc về người dùng khác, ngược lại false
     */
    public boolean isEmailUsedByOtherUser(String email, int userId) {
        String sql = "SELECT UserID FROM Users WHERE Email = ? AND UserID <> ?";
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Không thể kiểm tra trùng lặp email", e);
            return true;
        }
    }

    /**
     * Cập nhật thông tin hồ sơ người dùng (họ tên, email, số điện thoại, ảnh
     * đại diện).
     */
    public boolean updateProfile(int userId, String fullName, String email, String phone, String avatar) {
        String sql = "UPDATE Users SET FullName = ?, Email = ?, Phone = ?, Avatar = ?, UpdatedAt = GETDATE() WHERE UserID = ?";
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.setString(4, avatar);
            ps.setInt(5, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Không thể cập nhật hồ sơ người dùng", e);
            return false;
        }
    }

    /**
     * Lấy lịch sử đặt vé của một khách hàng cụ thể.
     *
     * @param userId mã định danh khách hàng
     * @return danh sách lịch sử đặt vé
     */
    public List<BookingHistoryDTO> getBookingHistory(int userId) {
        String sql = "SELECT b.BookingID, b.BookingCode, m.Title, br.CentreName, s.StartTime, "
                + "b.BookingStatus, b.TotalAmount, b.CreatedAt, "
                + "CASE WHEN s.Status = 'CANCELLED' OR cm.Status = 'CANCELLED' "
                + "THEN 'CANCELLED' ELSE s.Status END AS ShowtimeStatus, "
                + "CASE WHEN EXISTS (SELECT 1 FROM Payments p WHERE p.BookingID = b.BookingID AND p.PaymentStatus = 'PAID') "
                + "AND EXISTS (SELECT 1 FROM BookingSeats bs JOIN Tickets t ON t.BookingSeatID = bs.BookingSeatID "
                + "WHERE bs.BookingID = b.BookingID AND t.TicketStatus = 'CANCELLED') THEN 1 ELSE 0 END AS PaidTicketCancelled, "
                // Don cho thanh toan con trong thoi gian giu ghe (de hien nut Thanh toan)
                + "CASE WHEN b.BookingStatus = 'PENDING' AND EXISTS (SELECT 1 FROM BookingSeats bs "
                + "  WHERE bs.BookingID = b.BookingID AND bs.SeatStatus = 'HELD' AND bs.HoldUntil > SYSDATETIME()) "
                + "  THEN 1 ELSE 0 END AS HoldActive "
                + "FROM Bookings b "
                + "JOIN Showtimes s ON s.ShowtimeID = b.ShowtimeID "
                + "JOIN Movies m ON m.MovieID = s.MovieID "
                + "JOIN Halls h ON h.HallID = s.HallID "
                + "JOIN Centres br ON br.CentreID = h.CentreID "
                + "LEFT JOIN CentreMovies cm ON cm.MovieID = s.MovieID AND cm.CentreID = h.CentreID "
                + "WHERE b.UserID = ? ORDER BY b.CreatedAt DESC";
        List<BookingHistoryDTO> history = new ArrayList<>();
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BookingHistoryDTO item = new BookingHistoryDTO();
                    item.setBookingId(rs.getInt("BookingID"));
                    item.setHoldActive(rs.getInt("HoldActive") == 1);
                    item.setBookingCode(rs.getString("BookingCode"));
                    item.setMovieTitle(rs.getString("Title"));
                    item.setCentreName(rs.getString("CentreName"));
                    item.setStartTime(rs.getTimestamp("StartTime"));
                    item.setBookingStatus(rs.getString("BookingStatus"));
                    item.setTotalAmount(rs.getBigDecimal("TotalAmount"));
                    item.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    item.setShowtimeStatus(rs.getString("ShowtimeStatus"));
                    item.setPaidTicketCancelled(rs.getInt("PaidTicketCancelled") == 1);
                    history.add(item);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Không thể tải lịch sử đặt vé", e);
        }
        return history;
    }

    /**
     * Lấy danh bạ nhân viên theo trung tâm.
     */
    public List<User> getStaffDirectory(Integer centreId) {
        return getStaffDirectory(centreId, null);
    }

    /**
     * Lấy danh bạ nhân viên theo trung tâm và từ khóa tìm kiếm.
     *
     * @param centreId mã trung tâm (null nếu tìm trên toàn bộ hệ thống)
     * @param search từ khóa tìm kiếm (họ tên, tài khoản, email, số điện thoại
     * hoặc mã ID)
     * @return danh sách nhân viên thỏa mãn điều kiện
     */
    public List<User> getStaffDirectory(Integer centreId, String search) {
        return getStaffDirectory(centreId, search, null);
    }

    /**
     * Lấy danh sách nhân viên cấp rạp (Ticket Sales Staff, Ticket Checker
     * Staff) có lọc theo trung tâm, vai trò và tìm kiếm.
     */
    public List<User> getStaffDirectory(Integer centreId, String search, String roleFilter) {
        StringBuilder sql = new StringBuilder("SELECT u.*, b.CentreName FROM Users u "
                + "LEFT JOIN Centres b ON b.CentreID = u.CentreID "
                + "WHERE u.Role IN ('STAFF', 'TICKET_SALES_STAFF', 'TICKET_SALES') ");
        if (roleFilter != null && !roleFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(roleFilter.trim())) {
            sql.append("AND u.Role = ? ");
        }
        if (centreId != null) {
            sql.append("AND u.CentreID = ? ");
        }

        Integer searchId = null;
        String keyword = null;
        if (search != null && !search.trim().isEmpty()) {
            keyword = search.trim();
            String cleanDigits = keyword.replaceAll("(?i)^[#A-Z]+", "");
            if (!cleanDigits.isEmpty() && cleanDigits.matches("^\\d+$")) {
                try {
                    searchId = Integer.parseInt(cleanDigits);
                } catch (NumberFormatException ignored) {
                }
            }
            if (searchId != null) {
                sql.append("AND (u.UserID = ? OR u.Account LIKE ? OR u.FullName LIKE ? OR u.Email LIKE ? OR u.Phone LIKE ?) ");
            } else {
                sql.append("AND (u.Account LIKE ? OR u.FullName LIKE ? OR u.Email LIKE ? OR u.Phone LIKE ?) ");
            }
        }
        sql.append("ORDER BY u.FullName");

        List<User> staff = new ArrayList<>();
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int pIdx = 1;
            if (roleFilter != null && !roleFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(roleFilter.trim())) {
                ps.setString(pIdx++, roleFilter.trim().toUpperCase());
            }
            if (centreId != null) {
                ps.setInt(pIdx++, centreId);
            }
            if (keyword != null) {
                String likePattern = "%" + keyword + "%";
                if (searchId != null) {
                    ps.setInt(pIdx++, searchId);
                }
                ps.setString(pIdx++, likePattern);
                ps.setString(pIdx++, likePattern);
                ps.setString(pIdx++, likePattern);
                ps.setString(pIdx++, likePattern);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    staff.add(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Không thể tải danh sách nhân viên theo tìm kiếm", e);
        }
        return staff;
    }

    /**
     * Cập nhật trung tâm công tác và trạng thái hoạt động của nhân viên hoặc
     * quản lý.
     */
    public boolean updateStaffAssignment(int userId, Integer centreId, String status) {
        String sql = "UPDATE Users SET CentreID = ?, Status = ?, UpdatedAt = GETDATE() "
                + "WHERE UserID = ? AND Role IN ('STAFF', 'TICKET_SALES_STAFF', 'TICKET_SALES', 'MANAGER')";
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            if (centreId == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, centreId);
            }
            ps.setString(2, status);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Không thể cập nhật phân công nhân sự", e);
            return false;
        }
    }

    /**
     * Tạo tài khoản nhân viên mới (Ticket Sales Staff hoặc Ticket Checker
     * Staff).
     *
     * @param user đối tượng người dùng có vai trò nhân viên
     * @return true nếu tạo thành công, ngược lại false
     */
    public boolean createStaff(User user) {
        String staffRole = user.getRole();
        if (staffRole == null || staffRole.trim().isEmpty() || "STAFF".equalsIgnoreCase(staffRole)) {
            staffRole = User.ROLE_TICKET_SALES;
        }
        String sql = "INSERT INTO Users (FullName, Account, Email, Password, Phone, Role, Status, CentreID, EmailVerified, CreatedAt) "
                + "VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', ?, 1, GETDATE())";
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getAccount());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPassword());
            ps.setString(5, user.getPhone());
            ps.setString(6, staffRole);
            if (user.getCentreId() != null) {
                ps.setInt(7, user.getCentreId());
            } else {
                ps.setNull(7, Types.INTEGER);
            }
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Không thể tạo tài khoản nhân viên mới", e);
            return false;
        }
    }

    /**
     * Tạo tài khoản Quản lý mới (MANAGER) và trả về mã ID tự sinh.
     *
     * @param user đối tượng người dùng có vai trò Quản lý
     * @return mã ID của Quản lý nếu tạo thành công, ngược lại -1
     */
    public int createManager(User user, int adminId) {
        String sql = "INSERT INTO Users (FullName, Account, Email, Password, Phone, Role, Status, CentreID, EmailVerified, CreatedAt) "
                + "VALUES (?, ?, ?, ?, ?, 'MANAGER', 'ACTIVE', ?, 1, GETDATE())";
        try (Connection connection = DBContext.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, user.getFullName());
                ps.setString(2, user.getAccount());
                ps.setString(3, user.getEmail());
                ps.setString(4, user.getPassword());
                ps.setString(5, user.getPhone());
                if (user.getCentreId() != null) {
                    ps.setInt(6, user.getCentreId());
                } else {
                    ps.setNull(6, Types.INTEGER);
                }
                int affected = ps.executeUpdate();
                int newUserId = -1;
                if (affected > 0) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            newUserId = rs.getInt(1);
                        }
                    }
                }
                if (newUserId > 0 && user.getCentreId() != null) {
                    // 1. Chấm dứt phân công cũ (nếu có) tại rạp này
                    String endOldSql = "UPDATE CentreManagerAssignments SET EndDate = GETDATE(), Status = 'REPLACED', "
                            + "Notes = ISNULL(Notes, '') + N' [Thay thế khi khởi tạo Quản lý mới]' "
                            + "WHERE CentreID = ? AND Status = 'ACTIVE' AND EndDate IS NULL";
                    try (PreparedStatement psEnd = connection.prepareStatement(endOldSql)) {
                        psEnd.setInt(1, user.getCentreId());
                        psEnd.executeUpdate();
                    }

                    // 2. Giải phóng CentreID của bất kỳ Quản lý cũ nào từng giữ rạp này
                    String clearOldUserSql = "UPDATE Users SET CentreID = NULL, UpdatedAt = GETDATE() "
                            + "WHERE CentreID = ? AND Role = 'MANAGER' AND UserID <> ?";
                    try (PreparedStatement psClr = connection.prepareStatement(clearOldUserSql)) {
                        psClr.setInt(1, user.getCentreId());
                        psClr.setInt(2, newUserId);
                        psClr.executeUpdate();
                    }

                    // 3. Tạo phân công mới trong CentreManagerAssignments
                    String cmaSql = "INSERT INTO CentreManagerAssignments (CentreID, ManagerID, StartDate, Status, AssignedBy, Notes) VALUES (?, ?, GETDATE(), 'ACTIVE', ?, N'Bổ nhiệm khi khởi tạo tài khoản')";
                    try (PreparedStatement psCma = connection.prepareStatement(cmaSql)) {
                        psCma.setInt(1, user.getCentreId());
                        psCma.setInt(2, newUserId);
                        psCma.setInt(3, adminId);
                        psCma.executeUpdate();
                    }
                }
                connection.commit();
                return newUserId;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Không thể tạo tài khoản Quản lý", e);
        }
        return -1;
    }

    /**
     * Lấy toàn bộ danh sách Quản lý trong hệ thống kèm thông tin cụm rạp phụ trách.
     *
     * @return danh sách các tài khoản có vai trò MANAGER
     */
    public List<User> getManagers() {
        String sql = "SELECT u.*, b.CentreName "
                + "FROM Users u "
                + "LEFT JOIN CentreManagerAssignments cma ON cma.ManagerID = u.UserID AND cma.Status = 'ACTIVE' AND cma.EndDate IS NULL "
                + "LEFT JOIN Centres b ON b.CentreID = cma.CentreID "
                + "WHERE u.Role = 'MANAGER' "
                + "ORDER BY u.UserID DESC";
        List<User> list = new ArrayList<>();
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapUser(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Không thể tải danh sách Quản lý", e);
        }
        return list;
    }

    /**
     * Cập nhật trạng thái hoạt động của tài khoản người dùng (ACTIVE, INACTIVE, LOCKED).
     * Nếu tài khoản không còn ở trạng thái ACTIVE, tự động kết thúc phân công quản lý hiện tại (nếu là Manager).
     *
     * @param userId mã định danh người dùng
     * @param status trạng thái mới
     * @return true nếu cập nhật thành công; ngược lại false
     */
    public boolean updateUserStatus(int userId, String status) {
        String sql = "UPDATE Users SET Status = ?, UpdatedAt = GETDATE() WHERE UserID = ?";

        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, userId);
            if (ps.executeUpdate() == 0) {
                return false;
            }
            if (!"ACTIVE".equalsIgnoreCase(status)) {
                unassignManagerByUserId(userId, 0, "Tài khoản Quản lý bị khóa/tạm ngưng");
            }
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Không thể cập nhật trạng thái người dùng (userId=" + userId + ")", e);
            return false;
        }
    }

    /**
     * Kiểm tra email đã được tài khoản khác sử dụng hay chưa (ủy quyền sang {@link #isEmailUsedByOtherUser}).
     *
     * @param email  địa chỉ email cần kiểm tra
     * @param userId mã người dùng hiện tại cần loại trừ
     * @return true nếu email đã tồn tại ở tài khoản khác
     */
    public boolean isEmailExistsForOtherUser(String email, int userId) {
        return isEmailUsedByOtherUser(email, userId);
    }

    /**
     * Cập nhật thông tin cơ bản của người dùng (họ tên, số điện thoại, email).
     */
    public boolean updateUserBasicInfo(int userId, String fullName, String phone, String email) {
        String sql = "UPDATE Users SET FullName = ?, Phone = ?, Email = ?, UpdatedAt = GETDATE() WHERE UserID = ?";
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, phone);
            ps.setString(3, email);
            ps.setInt(4, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Không thể cập nhật thông tin cơ bản người dùng", e);
            return false;
        }
    }

    /**
     * Cập nhật thông tin Quản lý kèm theo điều chuyển hoặc bổ nhiệm cụm rạp trong 1 Transaction.
     *
     * @param userId      mã Quản lý
     * @param fullName    họ tên
     * @param phone       số điện thoại
     * @param email       email
     * @param newCentreId mã cụm rạp mới (hoặc null nếu gỡ rạp)
     * @param adminId     mã Admin thực hiện
     * @return true nếu thành công; ngược lại false
     */
    public boolean updateManagerWithCentreTx(int userId, String fullName, String phone, String email, Integer newCentreId, int adminId) {
        String updateUserSql = "UPDATE Users SET FullName = ?, Phone = ?, Email = ?, CentreID = ?, "
                + "Status = CASE WHEN ? IS NOT NULL AND Status <> 'ACTIVE' THEN 'ACTIVE' ELSE Status END, "
                + "UpdatedAt = GETDATE() WHERE UserID = ? AND Role = 'MANAGER'";
        String endOldManagerAssignmentSql = "UPDATE CentreManagerAssignments SET EndDate = GETDATE(), Status = 'TRANSFERRED', "
                + "Notes = LEFT(ISNULL(Notes, '') + N' [Kết thúc khi cập nhật thông tin/điều chuyển bởi Admin #' + CAST(? AS NVARCHAR) + N']', 500) "
                + "WHERE ManagerID = ? AND Status = 'ACTIVE' AND EndDate IS NULL";
        String endNewCentreOldAssignmentsSql = "UPDATE CentreManagerAssignments SET EndDate = GETDATE(), Status = 'REPLACED', "
                + "Notes = LEFT(ISNULL(Notes, '') + N' [Thay thế khi bổ nhiệm Quản lý mới]', 500) "
                + "WHERE CentreID = ? AND Status = 'ACTIVE' AND EndDate IS NULL";
        String clearNewCentreOldUsersSql = "UPDATE Users SET CentreID = NULL, UpdatedAt = GETDATE() "
                + "WHERE CentreID = ? AND Role = 'MANAGER' AND UserID <> ?";
        String insertNewAssignmentSql = "INSERT INTO CentreManagerAssignments (CentreID, ManagerID, StartDate, Status, AssignedBy, Notes) "
                + "VALUES (?, ?, GETDATE(), 'ACTIVE', ?, N'Bổ nhiệm/Điều chuyển khi cập nhật thông tin')";

        Connection connection = null;
        try {
            connection = DBContext.getConnection();
            connection.setAutoCommit(false);

            // Lấy thông tin cụm rạp hiện tại của Quản lý trước khi cập nhật
            Integer currentCentreId = null;
            String checkSql = "SELECT CentreID FROM Users WHERE UserID = ? AND Role = 'MANAGER'";
            try (PreparedStatement psCheck = connection.prepareStatement(checkSql)) {
                psCheck.setInt(1, userId);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next()) {
                        int cid = rs.getInt("CentreID");
                        if (!rs.wasNull()) {
                            currentCentreId = cid;
                        }
                    } else {
                        connection.rollback();
                        return false;
                    }
                }
            }

            // 1. Cập nhật bảng Users
            try (PreparedStatement psUser = connection.prepareStatement(updateUserSql)) {
                psUser.setString(1, fullName);
                psUser.setString(2, phone);
                psUser.setString(3, email);
                if (newCentreId != null) {
                    psUser.setInt(4, newCentreId);
                    psUser.setInt(5, newCentreId);
                } else {
                    psUser.setNull(4, Types.INTEGER);
                    psUser.setNull(5, Types.INTEGER);
                }
                psUser.setInt(6, userId);
                int affected = psUser.executeUpdate();
                if (affected == 0) {
                    connection.rollback();
                    return false;
                }
            }

            // 2. Xử lý phân công nếu có sự thay đổi về rạp (newCentreId khác currentCentreId)
            boolean centreChanged = (currentCentreId == null && newCentreId != null)
                    || (currentCentreId != null && !currentCentreId.equals(newCentreId));

            if (centreChanged) {
                // 2.1. Chấm dứt phân công cũ của Quản lý này (nếu có)
                try (PreparedStatement psEndOld = connection.prepareStatement(endOldManagerAssignmentSql)) {
                    psEndOld.setInt(1, adminId);
                    psEndOld.setInt(2, userId);
                    psEndOld.executeUpdate();
                }

                // 2.2. Nếu có gán rạp mới:
                if (newCentreId != null) {
                    // Dọn dẹp phân công cũ tại rạp mới (nếu có ai từng giữ)
                    try (PreparedStatement psEndNewCentre = connection.prepareStatement(endNewCentreOldAssignmentsSql)) {
                        psEndNewCentre.setInt(1, newCentreId);
                        psEndNewCentre.executeUpdate();
                    }
                    try (PreparedStatement psClr = connection.prepareStatement(clearNewCentreOldUsersSql)) {
                        psClr.setInt(1, newCentreId);
                        psClr.setInt(2, userId);
                        psClr.executeUpdate();
                    }
                    // Tạo bản ghi phân công mới
                    try (PreparedStatement psIns = connection.prepareStatement(insertNewAssignmentSql)) {
                        psIns.setInt(1, newCentreId);
                        psIns.setInt(2, userId);
                        psIns.setInt(3, adminId);
                        psIns.executeUpdate();
                    }
                }
            }

            connection.commit();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi cập nhật thông tin Quản lý kèm phân công rạp", e);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Cập nhật thông tin nhân viên (họ tên, số điện thoại, email, vai trò:
     * Ticket Sales hoặc Ticket Checker).
     */
    public boolean updateStaffBasicInfo(int userId, String fullName, String phone, String email, String role) {
        if (role == null || role.trim().isEmpty() || "STAFF".equalsIgnoreCase(role)) {
            role = User.ROLE_TICKET_SALES;
        }
        String sql = "UPDATE Users SET FullName = ?, Phone = ?, Email = ?, Role = ?, UpdatedAt = GETDATE() WHERE UserID = ?";
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, phone);
            ps.setString(3, email);
            ps.setString(4, role);
            ps.setInt(5, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Không thể cập nhật thông tin nhân viên", e);
            return false;
        }
    }

    /**
     * Giải phóng cụm rạp mà Quản lý đang phụ trách theo UserId (dùng khi tài khoản bị khóa/tạm ngưng).
     *
     * @param managerId mã Quản lý cần giải phóng phân công
     * @param adminId   mã Admin thực hiện thao tác
     * @param reason    lý do kết thúc phân công
     * @return true nếu xử lý giao dịch thành công; ngược lại false
     */
    public boolean unassignManagerByUserId(int managerId, int adminId, String reason) {
        String endAssignment = "UPDATE CentreManagerAssignments SET EndDate = GETDATE(), Status = 'ENDED', "
                + "Notes = ISNULL(Notes, '') + N' [' + ? + ']' "
                + "WHERE ManagerID = ? AND Status = 'ACTIVE' AND EndDate IS NULL";
        String clearCentre = "UPDATE Users SET CentreID = NULL, UpdatedAt = GETDATE() WHERE UserID = ? AND Role = 'MANAGER'";

        Connection connection = null;
        try {
            connection = DBContext.getConnection();
            connection.setAutoCommit(false);

            try (PreparedStatement ps1 = connection.prepareStatement(endAssignment)) {
                ps1.setString(1, reason != null ? reason : "Tài khoản Quản lý bị khóa/tạm ngưng");
                ps1.setInt(2, managerId);
                ps1.executeUpdate();
            }

            try (PreparedStatement ps2 = connection.prepareStatement(clearCentre)) {
                ps2.setInt(1, managerId);
                ps2.executeUpdate();
            }

            connection.commit();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Giải phóng cụm rạp khi khóa Quản lý thất bại", e);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Đồng bộ và giải phóng các cụm rạp có Quản lý đang bị khóa/tạm ngưng tài
     * khoản. Khi tài khoản Quản lý không ở trạng thái ACTIVE, rạp đó được giải
     * phóng thành "Chưa có Quản lý" để Admin có thể bổ nhiệm Quản lý mới ngay
     * lập tức.
     */
    public void syncLockedManagersAssignments() {
        // 1. Kết thúc phân công của Quản lý có tài khoản không ACTIVE
        String endAssignmentsSql
                = "UPDATE cma "
                + "SET cma.EndDate = GETDATE(), "
                + "    cma.Status = 'ENDED', "
                + "    cma.Notes = LEFT(ISNULL(cma.Notes, '') + N' [Tài khoản Quản lý bị khóa/tạm ngưng]', 500) "
                + "FROM CentreManagerAssignments cma "
                + "JOIN Users u ON u.UserID = cma.ManagerID "
                + "WHERE cma.Status = 'ACTIVE' "
                + "AND cma.EndDate IS NULL "
                + "AND UPPER(u.Status) <> 'ACTIVE'";

        // 2. Đồng bộ CentreID theo phân công ACTIVE
        String syncCentreIdSql
                = "UPDATE u "
                + "SET u.CentreID = cma.CentreID, "
                + "    u.UpdatedAt = GETDATE() "
                + "FROM Users u "
                + "LEFT JOIN CentreManagerAssignments cma "
                + "    ON u.UserID = cma.ManagerID "
                + "    AND cma.Status = 'ACTIVE' "
                + "    AND cma.EndDate IS NULL "
                + "WHERE u.Role = 'MANAGER' "
                + "AND ("
                + "    u.CentreID <> cma.CentreID "
                + "    OR (u.CentreID IS NULL AND cma.CentreID IS NOT NULL) "
                + "    OR (u.CentreID IS NOT NULL AND cma.CentreID IS NULL)"
                + ")";

        try (Connection conn = DBContext.getConnection()) {
            try {
                conn.setAutoCommit(false);

                try (PreparedStatement ps1 = conn.prepareStatement(endAssignmentsSql); PreparedStatement ps2 = conn.prepareStatement(syncCentreIdSql)) {

                    ps1.executeUpdate();
                    ps2.executeUpdate();

                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }

            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Lỗi đồng bộ phân công Quản lý", e);
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi kết nối Database khi đồng bộ phân công Quản lý", e);
        }
    }

    /**
     * Cập nhật trạng thái Quản lý và giải phóng phân công rạp (nếu khóa/tạm ngưng) trong 1 transaction.
     */
    public boolean toggleManagerStatusTx(int managerId, String newStatus, int adminId) {
        String updateStatusSql = "UPDATE Users SET Status = ?, UpdatedAt = GETDATE() WHERE UserID = ? AND Role = 'MANAGER'";
        String endAssignmentSql = "UPDATE CentreManagerAssignments SET EndDate = GETDATE(), Status = 'ENDED', "
                + "Notes = LEFT(ISNULL(Notes, '') + N' [Tài khoản Quản lý bị khóa/tạm ngưng' + CASE WHEN ? > 0 THEN N' bởi Admin #' + CAST(? AS NVARCHAR) ELSE N'' END + N']', 500) "
                + "WHERE ManagerID = ? AND Status = 'ACTIVE' AND EndDate IS NULL";
        String clearCentreSql = "UPDATE Users SET CentreID = NULL, UpdatedAt = GETDATE() WHERE UserID = ? AND Role = 'MANAGER'";

        Connection conn = null;
        try {
            conn = DBContext.getConnection();
            conn.setAutoCommit(false);

            // 1. Cập nhật Status trong bảng Users
            try (PreparedStatement ps1 = conn.prepareStatement(updateStatusSql)) {
                ps1.setString(1, newStatus);
                ps1.setInt(2, managerId);
                int rows = ps1.executeUpdate();
                if (rows == 0) {
                    conn.rollback();
                    return false;
                }
            }

            // 2. Nếu không phải ACTIVE -> tự động gỡ bỏ quyền quản lý khỏi cụm rạp
            if (!"ACTIVE".equalsIgnoreCase(newStatus)) {
                try (PreparedStatement ps2a = conn.prepareStatement(endAssignmentSql)) {
                    ps2a.setInt(1, adminId);
                    ps2a.setInt(2, adminId);
                    ps2a.setInt(3, managerId);
                    ps2a.executeUpdate();
                }
                try (PreparedStatement ps2b = conn.prepareStatement(clearCentreSql)) {
                    ps2b.setInt(1, managerId);
                    ps2b.executeUpdate();
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi thực thi toggleManagerStatusTx cho managerId=" + managerId, e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Lấy danh sách toàn bộ khách hàng phục vụ trang quản trị.
     *
     * @return danh sách tất cả người dùng có vai trò CUSTOMER
     */
    public List<User> getCustomers() {
        String sql = "SELECT * FROM Users WHERE Role = 'CUSTOMER' ORDER BY UserID DESC";
        List<User> list = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapUser(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Không thể tải danh sách khách hàng", e);
        }
        return list;
    }


    /**
     * Lay vai tro va ID rap chieu duoc phan cong cua nguoi dung.
     *
     * @param userId ID nguoi dung
     * @return mang String [Role, CentreId] hoac null neu khong tim thay
     */
    public String[] getUserRoleAndAssignedCentre(int userId) {
        String sql = "SELECT u.Role, u.CentreID, a.CentreID AS AssignedCentreID "
                + "FROM Users u LEFT JOIN CentreManagerAssignments a ON a.ManagerID = u.UserID "
                + "AND a.Status = 'ACTIVE' AND a.StartDate <= GETDATE() "
                + "AND (a.EndDate IS NULL OR a.EndDate > GETDATE()) "
                + "WHERE u.UserID = ?";
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String role = rs.getString("Role");
                    int centre = rs.getInt("MANAGER".equalsIgnoreCase(role) ? "AssignedCentreID" : "CentreID");
                    String centreStr = (!rs.wasNull() && centre > 0) ? String.valueOf(centre) : null;
                    return new String[]{role, centreStr};
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Loi khi lay thong tin phan cong rap cua nguoi dung: " + userId, e);
        }
        return null;
    }
}
