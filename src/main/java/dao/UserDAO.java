package dao;

import model.dto.booking.BookingHistoryDTO;
import model.entity.ManagerAssignment;
import model.entity.StaffTransfer;
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
 * Xử lý truy vấn và quản lý dữ liệu người dùng, phân quyền, luân chuyển nhân sự và quản lý tài khoản.
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

        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
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

        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
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
     * @param user   thông tin tài khoản người dùng
     * @param otp    mã OTP kích hoạt
     * @param expiry thời điểm hết hạn của OTP
     * @return true nếu đăng ký thành công, ngược lại false
     */
    public boolean register(User user, String otp, Timestamp expiry) {
        String sql = "INSERT INTO Users "
                + "(FullName, Account, Email, Password, Role, Status, ResetToken, ResetTokenExpiry, EmailVerified) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
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
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
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
     * @param otp    mã OTP mới
     * @param expiry thời điểm hết hạn OTP
     * @return true nếu cập nhật thành công, ngược lại false
     */
    public boolean updateActivationOtp(int userId, String otp, Timestamp expiry) {
        String sql = "UPDATE Users SET ResetToken = ?, ResetTokenExpiry = ?, UpdatedAt = GETDATE() WHERE UserID = ?";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
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
     * Cập nhật địa chỉ email và trạng thái xác thực của người dùng sau khi xác thực OTP thành công.
     *
     * @param userId ID người dùng
     * @param newEmail địa chỉ email mới
     * @param verified trạng thái xác thực email
     * @return true nếu cập nhật thành công, ngược lại false
     */
    public boolean updateUserEmail(int userId, String newEmail, boolean verified) {
        String sql = "UPDATE Users SET Email = ?, EmailVerified = ?, ResetToken = NULL, ResetTokenExpiry = NULL, UpdatedAt = GETDATE() WHERE UserID = ?";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
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

        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
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

        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
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

        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
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
     * @param userId      mã định danh người dùng
     * @param newPassword mật khẩu mới (đã mã hóa)
     * @return true nếu cập nhật thành công, ngược lại false
     */
    public boolean updatePassword(int userId, String newPassword) {
        String sql = "UPDATE Users SET Password = ?, UpdatedAt = GETDATE() WHERE UserID = ?";

        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
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
     * @param token  mã xác thực đặt lại mật khẩu
     * @param expiry thời điểm hết hạn của token
     * @return true nếu lưu thành công, ngược lại false
     */
    public boolean saveResetToken(int userId, String token, Timestamp expiry) {
        String sql = "UPDATE Users SET ResetToken = ?, ResetTokenExpiry = ? WHERE UserID = ?";

        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
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

        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
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
     * @param userId      mã định danh người dùng
     * @param newPassword mật khẩu mới đã mã hóa
     * @return true nếu đặt lại mật khẩu thành công, ngược lại false
     */
    public boolean resetPassword(int userId, String newPassword) {
        String sql = "UPDATE Users SET Password = ?, ResetToken = NULL, ResetTokenExpiry = NULL, "
                + "UpdatedAt = GETDATE() WHERE UserID = ?";

        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
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

        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
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
     * @param userId      mã định danh người dùng
     * @param newPassword mật khẩu mới đã mã hóa
     * @return true nếu đổi mật khẩu thành công, ngược lại false
     */
    public boolean changePassword(int userId, String newPassword) {
        String sql = "UPDATE Users SET Password = ?, UpdatedAt = GETDATE() WHERE UserID = ?";

        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
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

        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
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

    /** Kiểm tra Manager còn phân công hiện hành tại đúng chi nhánh hay không. */
    public boolean hasActiveManagerAssignment(int managerId, int centreId) {
        String sql = "SELECT 1 FROM CentreManagerAssignments "
                + "WHERE ManagerID = ? AND CentreID = ? "
                + "AND Status = 'ACTIVE' AND EndDate IS NULL";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
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
     * @param email  địa chỉ email cần kiểm tra
     * @param userId mã người dùng hiện tại (bỏ qua không đối chiếu)
     * @return true nếu email đã thuộc về người dùng khác, ngược lại false
     */
    public boolean isEmailUsedByOtherUser(String email, int userId) {
        String sql = "SELECT UserID FROM Users WHERE Email = ? AND UserID <> ?";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
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
     * Cập nhật thông tin hồ sơ người dùng (họ tên, email, số điện thoại, ảnh đại diện).
     */
    public boolean updateProfile(int userId, String fullName, String email, String phone, String avatar) {
        String sql = "UPDATE Users SET FullName = ?, Email = ?, Phone = ?, Avatar = ?, UpdatedAt = GETDATE() WHERE UserID = ?";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
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
                // Don cho thanh toan con trong thoi gian giu ghe (de hien nut Thanh toan)
                + "CASE WHEN b.BookingStatus = 'PENDING' AND EXISTS (SELECT 1 FROM BookingSeats bs "
                + "  WHERE bs.BookingID = b.BookingID AND bs.SeatStatus = 'HELD' AND bs.HoldUntil > SYSDATETIME()) "
                + "  THEN 1 ELSE 0 END AS HoldActive "
                + "FROM Bookings b "
                + "JOIN Showtimes s ON s.ShowtimeID = b.ShowtimeID "
                + "JOIN Movies m ON m.MovieID = s.MovieID "
                + "JOIN Halls h ON h.HallID = s.HallID "
                + "JOIN Centres br ON br.CentreID = h.CentreID "
                + "WHERE b.UserID = ? ORDER BY b.CreatedAt DESC";
        List<BookingHistoryDTO> history = new ArrayList<>();
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
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
     * @param search   từ khóa tìm kiếm (họ tên, tài khoản, email, số điện thoại hoặc mã ID)
     * @return danh sách nhân viên thỏa mãn điều kiện
     */
    public List<User> getStaffDirectory(Integer centreId, String search) {
        return getStaffDirectory(centreId, search, null);
    }

    /**
     * Lấy danh sách nhân viên cấp rạp (Ticket Sales Staff, Ticket Checker Staff) có lọc theo trung tâm, vai trò và tìm kiếm.
     */
    public List<User> getStaffDirectory(Integer centreId, String search, String roleFilter) {
        StringBuilder sql = new StringBuilder("SELECT u.*, b.CentreName FROM Users u "
                + "LEFT JOIN Centres b ON b.CentreID = u.CentreID "
                + "WHERE u.Role IN ('STAFF', 'TICKET_SALES_STAFF', 'TICKET_CHECKER_STAFF', 'TICKET_SALES', 'TICKET_CHECKER') ");
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
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
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
     * Cập nhật trung tâm công tác và trạng thái hoạt động của nhân viên hoặc quản lý.
     */
    public boolean updateStaffAssignment(int userId, Integer centreId, String status) {
        String sql = "UPDATE Users SET CentreID = ?, Status = ?, UpdatedAt = GETDATE() "
                + "WHERE UserID = ? AND Role IN ('STAFF', 'TICKET_SALES_STAFF', 'TICKET_CHECKER_STAFF', 'TICKET_SALES', 'TICKET_CHECKER', 'MANAGER')";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
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
     * Tạo tài khoản nhân viên mới (Ticket Sales Staff hoặc Ticket Checker Staff).
     *
     * @param user đối tượng người dùng có vai trò nhân viên
     * @return true nếu tạo thành công, ngược lại false
     */
    public boolean createStaff(User user) {
        String staffRole = user.getRole();
        if (staffRole == null || staffRole.trim().isEmpty()) {
            staffRole = User.ROLE_TICKET_SALES;
        }
        String sql = "INSERT INTO Users (FullName, Account, Email, Password, Phone, Role, Status, CentreID, EmailVerified, CreatedAt) "
                + "VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', ?, 1, GETDATE())";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
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

    public int createManager(User user) {
        return createManager(user, 4);
    }

    /**
     * Gán trực tiếp một Quản lý vào trung tâm chiếu phim.
     */
    public boolean assignManagerToCentre(int managerUserId, int centreId) {
        String sql = "UPDATE Users SET CentreID = ?, UpdatedAt = GETDATE() WHERE UserID = ? AND Role = 'MANAGER'";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, centreId);
            ps.setInt(2, managerUserId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Không thể gán Quản lý cho trung tâm", e);
            return false;
        }
    }

    /**
     * Lấy toàn bộ danh sách Quản lý trong hệ thống.
     */
    public List<User> getManagers() {
        return getManagers(null);
    }

    /**
     * Tìm kiếm và lấy danh sách Quản lý theo từ khóa.
     *
     * @param search từ khóa tìm kiếm (họ tên, tài khoản, email, số điện thoại, tên trung tâm hoặc mã ID)
     * @return danh sách các Quản lý
     */
    public List<User> getManagers(String search) {
        StringBuilder sql = new StringBuilder("SELECT u.UserID, u.FullName, u.Account, u.Email, u.Password, u.Phone, u.Avatar, u.Role, u.Status, "
                + "cma.CentreID, b.CentreName, "
                + "u.EmailVerified, u.PhoneVerified, u.CreatedAt, u.UpdatedAt, u.LastLoginAt, u.ResetToken, u.ResetTokenExpiry "
                + "FROM Users u "
                + "LEFT JOIN CentreManagerAssignments cma ON cma.ManagerID = u.UserID AND cma.Status = 'ACTIVE' AND cma.EndDate IS NULL "
                + "LEFT JOIN Centres b ON b.CentreID = cma.CentreID "
                + "WHERE u.Role = 'MANAGER' ");

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
                sql.append("AND (u.UserID = ? OR u.Account LIKE ? OR u.FullName LIKE ? OR u.Email LIKE ? OR u.Phone LIKE ? OR b.CentreName LIKE ?) ");
            } else {
                sql.append("AND (u.Account LIKE ? OR u.FullName LIKE ? OR u.Email LIKE ? OR u.Phone LIKE ? OR b.CentreName LIKE ?) ");
            }
        }
        sql.append("ORDER BY u.FullName");

        List<User> list = new ArrayList<>();
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            if (keyword != null) {
                int pIdx = 1;
                String likePattern = "%" + keyword + "%";
                if (searchId != null) {
                    ps.setInt(pIdx++, searchId);
                }
                ps.setString(pIdx++, likePattern);
                ps.setString(pIdx++, likePattern);
                ps.setString(pIdx++, likePattern);
                ps.setString(pIdx++, likePattern);
                ps.setString(pIdx++, likePattern);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Không thể tải danh sách Quản lý theo tìm kiếm", e);
        }
        return list;
    }

    /**
     * Lấy danh sách cả Nhân viên và Quản lý theo trung tâm (hoặc toàn bộ nếu centreId là null).
     */
    public List<User> getAllStaffAndManagers(Integer centreId) {
        String sql = "SELECT u.*, b.CentreName FROM Users u "
                + "LEFT JOIN Centres b ON b.CentreID = u.CentreID "
                + "WHERE u.Role IN ('STAFF', 'TICKET_SALES_STAFF', 'TICKET_CHECKER_STAFF', 'TICKET_SALES', 'TICKET_CHECKER', 'MANAGER') "
                + (centreId != null ? "AND u.CentreID = ? " : "")
                + "ORDER BY u.Role DESC, b.CentreName, u.FullName";
        List<User> list = new ArrayList<>();
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            if (centreId != null) {
                ps.setInt(1, centreId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Không thể tải danh sách Nhân viên và Quản lý", e);
        }
        return list;
    }

    /**
     * Luân chuyển nhân viên sang trung tâm rạp khác và lưu lịch sử điều động trong giao dịch.
     *
     * @param userId       mã nhân viên
     * @param fromCentreId mã trung tâm hiện tại (hoặc null nếu chưa gán)
     * @param toCentreId   mã trung tâm chuyển đến
     * @param adminId      mã Admin thực hiện lệnh luân chuyển
    /**
     * Luân chuyển nhân sự (Nhân viên hoặc Quản lý) sang trung tâm rạp khác,
     * hỗ trợ thay đổi vai trò/thăng chức và ngày hiệu lực, bảo toàn tính toàn vẹn trong giao dịch.
     *
     * @param userId        mã nhân sự
     * @param fromCentreId  mã trung tâm hiện tại (hoặc null nếu chưa gán)
     * @param toCentreId    mã trung tâm chuyển đến
     * @param adminId       mã Admin thực hiện lệnh luân chuyển
     * @param reason        lý do điều động
     * @param notes         ghi chú bổ sung
     * @param newRole       vai trò mới (nếu có thay đổi hoặc thăng chức)
     * @param effectiveDate ngày có hiệu lực
     * @return true nếu luân chuyển thành công, ngược lại false
     */
    public boolean transferStaff(int userId, Integer fromCentreId, int toCentreId, int adminId,
                                 String reason, String notes, String newRole, java.sql.Date effectiveDate) {
        User u = getUserById(userId);
        if (u == null) {
            return false;
        }
        if ("MANAGER".equalsIgnoreCase(u.getRole())) {
            return "SUCCESS".equals(transferManager(userId, toCentreId, adminId, reason, notes));
        } else {
            return "SUCCESS".equals(transferRegularStaff(userId, toCentreId, adminId, reason, notes, effectiveDate));
        }
    }

    /**
     * Luân chuyển nhân viên với các tham số mặc định (tương thích ngược).
     */
    public boolean transferStaff(int userId, Integer fromCentreId, int toCentreId, int adminId, String reason, String notes) {
        return transferStaff(userId, fromCentreId, toCentreId, adminId, reason, notes, null, null);
    }

    /**
     * Lấy lịch sử điều động, luân chuyển nhân sự theo trung tâm hoặc nhân viên.
     */
    public List<StaffTransfer> getTransferHistory(Integer centreId, Integer userId) {
        StringBuilder sql = new StringBuilder(
                "SELECT t.TransferID, t.UserID, t.FromCentreID, t.ToCentreID, t.TransferredBy, "
                + "t.TransferDate, t.EffectiveDate, t.OldRole, t.NewRole, t.Reason, t.Notes, "
                + "u.FullName AS UserName, u.Account AS UserAccount, u.Email AS UserEmail, u.Role AS UserRole, "
                + "fb.CentreName AS FromCentreName, tb.CentreName AS ToCentreName, "
                + "adm.FullName AS TransferredByName "
                + "FROM StaffTransferHistory t "
                + "JOIN Users u ON u.UserID = t.UserID "
                + "LEFT JOIN Centres fb ON fb.CentreID = t.FromCentreID "
                + "JOIN Centres tb ON tb.CentreID = t.ToCentreID "
                + "JOIN Users adm ON adm.UserID = t.TransferredBy "
                // Lịch sử điều chuyển chỉ dành cho nhân viên.
                // Các bản ghi Manager thuộc lịch sử bổ nhiệm/nhiệm kỳ, không hiển thị ở đây.
                + "WHERE ISNULL(t.OldRole, '') <> 'MANAGER' "
                + "AND ISNULL(t.NewRole, '') <> 'MANAGER' "
        );

        if (centreId != null) {
            sql.append("AND (t.FromCentreID = ? OR t.ToCentreID = ?) ");
        }
        if (userId != null) {
            sql.append("AND t.UserID = ? ");
        }
        sql.append("ORDER BY t.TransferDate DESC, t.TransferID DESC");

        List<StaffTransfer> list = new ArrayList<>();
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (centreId != null) {
                ps.setInt(paramIndex++, centreId);
                ps.setInt(paramIndex++, centreId);
            }
            if (userId != null) {
                ps.setInt(paramIndex++, userId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StaffTransfer item = new StaffTransfer();
                    item.setTransferId(rs.getInt("TransferID"));
                    item.setUserId(rs.getInt("UserID"));
                    item.setUserName(rs.getString("UserName"));
                    item.setUserAccount(rs.getString("UserAccount"));
                    item.setUserEmail(rs.getString("UserEmail"));
                    item.setUserRole(rs.getString("UserRole"));

                    int fromB = rs.getInt("FromCentreID");
                    item.setFromCentreId(rs.wasNull() ? null : fromB);
                    item.setFromCentreName(rs.getString("FromCentreName"));

                    item.setToCentreId(rs.getInt("ToCentreID"));
                    item.setToCentreName(rs.getString("ToCentreName"));

                    item.setTransferredBy(rs.getInt("TransferredBy"));
                    item.setTransferredByName(rs.getString("TransferredByName"));

                    item.setTransferDate(rs.getTimestamp("TransferDate"));
                    item.setEffectiveDate(rs.getDate("EffectiveDate"));
                    item.setOldRole(rs.getString("OldRole"));
                    item.setNewRole(rs.getString("NewRole"));
                    item.setReason(rs.getString("Reason"));
                    item.setNotes(rs.getString("Notes"));

                    list.add(item);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Không thể tải lịch sử luân chuyển nhân sự", e);
        }
        return list;
    }

    /**
     * Lấy danh sách toàn bộ nhân viên có thể điều chuyển (Staff / Ticket Sales / Ticket Checker),
     * hỗ trợ lọc theo trung tâm, vai trò và tìm kiếm theo tên/mã nhân sự.
     */
    public List<User> getTransferableUsers(Integer centreId, String search, String roleFilter) {
        StringBuilder sql = new StringBuilder("SELECT u.*, b.CentreName FROM Users u "
                + "LEFT JOIN Centres b ON b.CentreID = u.CentreID "
                + "WHERE u.Role IN ('STAFF', 'TICKET_SALES_STAFF', 'TICKET_CHECKER_STAFF', 'TICKET_SALES', 'TICKET_CHECKER') ");
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
        sql.append("ORDER BY u.Role, u.FullName");

        List<User> list = new ArrayList<>();
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
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
                    list.add(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Không thể tải danh sách nhân sự điều chuyển", e);
        }
        return list;
    }

    /**
     * Cập nhật trạng thái hoạt động của tài khoản người dùng (ACTIVE, INACTIVE, LOCKED...).
     */
    public boolean updateUserStatus(int userId, String status) {
        String sql = "UPDATE Users SET Status = ?, UpdatedAt = GETDATE() WHERE UserID = ?";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, userId);
            boolean ok = ps.executeUpdate() > 0;
            if (ok && !"ACTIVE".equalsIgnoreCase(status)) {
                unassignManagerByUserId(userId, 0, "Tài khoản Quản lý bị khóa/tạm ngưng");
            }
            return ok;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Không thể cập nhật trạng thái người dùng", e);
            return false;
        }
    }

    /**
     * Kiểm tra email đã được tài khoản khác sử dụng hay chưa (dùng cho form cập nhật thông tin).
     */
    public boolean isEmailExistsForOtherUser(String email, int userId) {
        String sql = "SELECT 1 FROM Users WHERE Email = ? AND UserID <> ?";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi kiểm tra email trùng lặp với người dùng khác", e);
            return false;
        }
    }

    /**
     * Cập nhật thông tin cơ bản của người dùng (họ tên, số điện thoại, email).
     */
    public boolean updateUserBasicInfo(int userId, String fullName, String phone, String email) {
        String sql = "UPDATE Users SET FullName = ?, Phone = ?, Email = ?, UpdatedAt = GETDATE() WHERE UserID = ?";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
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
     * Cập nhật thông tin nhân viên (họ tên, số điện thoại, email, vai trò: Ticket Sales hoặc Ticket Checker).
     */
    public boolean updateStaffBasicInfo(int userId, String fullName, String phone, String email, String role) {
        String sql = "UPDATE Users SET FullName = ?, Phone = ?, Email = ?, Role = ?, UpdatedAt = GETDATE() WHERE UserID = ?";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
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
     * Bổ nhiệm Quản lý phụ trách trung tâm chiếu phim và ghi nhận vào lịch sử bổ nhiệm.
     *
     * @param centreId  mã trung tâm chiếu phim
     * @param managerId mã người dùng vai trò Quản lý
     * @param adminId   mã Admin ra quyết định bổ nhiệm
     * @param notes     ghi chú bổ nhiệm
     * @return true nếu bổ nhiệm thành công, ngược lại false
     */
    /**
     * Bổ nhiệm Quản lý mới cho trung tâm chiếu phim (chỉ dành cho Manager chưa có phân công hiện hành).
     * Áp dụng Transaction và khóa UPDLOCK, HOLDLOCK chống tranh chấp đồng thời (concurrency safe).
     *
     * @param centreId  mã trung tâm chiếu phim
     * @param managerId mã người dùng vai trò Quản lý
     * @param adminId   mã Admin ra quyết định bổ nhiệm
     * @param notes     ghi chú bổ nhiệm
     * @return mã kết quả (SUCCESS, MANAGER_ALREADY_ASSIGNED, CENTRE_ALREADY_HAS_MANAGER,...)
     */
    public String assignManager(int centreId, int managerId, int adminId, String notes) {
        Connection connection = null;
        try {
            connection = DBContext.getConnection();
            connection.setAutoCommit(false);

            // 1. Kiểm tra trung tâm đích với khóa UPDLOCK, HOLDLOCK
            String checkCentreSql = "SELECT CentreID, Status FROM Centres WITH (UPDLOCK, HOLDLOCK) WHERE CentreID = ?";
            try (PreparedStatement ps = connection.prepareStatement(checkCentreSql)) {
                ps.setInt(1, centreId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        connection.rollback();
                        return "CENTRE_NOT_FOUND";
                    }
                    String cStatus = rs.getString("Status");
                    if (!"ACTIVE".equalsIgnoreCase(cStatus)) {
                        connection.rollback();
                        return "CENTRE_INACTIVE";
                    }
                }
            }

            // 2. Kiểm tra vai trò và trạng thái của Quản lý với khóa UPDLOCK, HOLDLOCK
            String checkUserSql = "SELECT UserID, Role, Status FROM Users WITH (UPDLOCK, HOLDLOCK) WHERE UserID = ?";
            try (PreparedStatement ps = connection.prepareStatement(checkUserSql)) {
                ps.setInt(1, managerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        connection.rollback();
                        return "MANAGER_NOT_FOUND";
                    }
                    String role = rs.getString("Role");
                    String status = rs.getString("Status");

                    if (!"MANAGER".equalsIgnoreCase(role)) {
                        connection.rollback();
                        return "NOT_A_MANAGER";
                    }
                    if (!"ACTIVE".equalsIgnoreCase(status)) {
                        connection.rollback();
                        return "MANAGER_NOT_ACTIVE";
                    }
                }
            }

            // 3. Kiểm tra xem Manager này đã có bản ghi ACTIVE nào trong CentreManagerAssignments chưa
            String checkMgrAssignSql = "SELECT AssignmentID FROM CentreManagerAssignments WITH (UPDLOCK, HOLDLOCK) "
                    + "WHERE ManagerID = ? AND Status = 'ACTIVE' AND EndDate IS NULL";
            try (PreparedStatement ps = connection.prepareStatement(checkMgrAssignSql)) {
                ps.setInt(1, managerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        connection.rollback();
                        return "MANAGER_ALREADY_ASSIGNED";
                    }
                }
            }

            // 4. Kiểm tra xem Centre này đã có Quản lý nào phân công ACTIVE chưa
            String checkCentreAssignSql = "SELECT cma.AssignmentID, cma.ManagerID, u.Status AS MgrStatus "
                    + "FROM CentreManagerAssignments cma WITH (UPDLOCK, HOLDLOCK) "
                    + "LEFT JOIN Users u WITH (UPDLOCK, HOLDLOCK) ON u.UserID = cma.ManagerID "
                    + "WHERE cma.CentreID = ? AND cma.Status = 'ACTIVE' AND cma.EndDate IS NULL";
            try (PreparedStatement ps = connection.prepareStatement(checkCentreAssignSql)) {
                ps.setInt(1, centreId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String mgrStatus = rs.getString("MgrStatus");
                        if ("ACTIVE".equalsIgnoreCase(mgrStatus)) {
                            connection.rollback();
                            return "CENTRE_ALREADY_HAS_MANAGER";
                        } else {
                            // Quản lý cũ đang bị khóa/ngưng hoạt động -> Tự động kết thúc phân công cũ để nhường chỗ cho Quản lý mới
                            int oldAssignId = rs.getInt("AssignmentID");
                            int oldMgrId = rs.getInt("ManagerID");
                            String endOldAssignSql = "UPDATE CentreManagerAssignments SET EndDate = GETDATE(), Status = 'REPLACED', "
                                    + "Notes = ISNULL(Notes, '') + N' [Bổ nhiệm Quản lý mới thay thế Quản lý bị khóa]' WHERE AssignmentID = ?";
                            try (PreparedStatement psEnd = connection.prepareStatement(endOldAssignSql)) {
                                psEnd.setInt(1, oldAssignId);
                                psEnd.executeUpdate();
                            }
                            String clearOldUserCentreSql = "UPDATE Users SET CentreID = NULL, UpdatedAt = GETDATE() WHERE UserID = ?";
                            try (PreparedStatement psClr = connection.prepareStatement(clearOldUserCentreSql)) {
                                psClr.setInt(1, oldMgrId);
                                psClr.executeUpdate();
                            }
                        }
                    }
                }
            }

            // 5. Tạo bản ghi bổ nhiệm trong CentreManagerAssignments
            String insertAssignSql = "INSERT INTO CentreManagerAssignments (CentreID, ManagerID, AssignedBy, StartDate, EndDate, Status, Notes, CreatedAt) "
                    + "VALUES (?, ?, ?, GETDATE(), NULL, 'ACTIVE', ?, GETDATE())";
            try (PreparedStatement ps = connection.prepareStatement(insertAssignSql)) {
                ps.setInt(1, centreId);
                ps.setInt(2, managerId);
                ps.setInt(3, adminId);
                ps.setString(4, (notes != null && !notes.trim().isEmpty()) ? notes.trim() : "Bổ nhiệm Quản lý phụ trách trung tâm");
                ps.executeUpdate();
            }

            // 6. Cập nhật Users.CentreID: Giải phóng rạp khỏi Quản lý cũ (nếu có) và gán cho Quản lý mới
            String clearOtherManagersSql = "UPDATE Users SET CentreID = NULL, UpdatedAt = GETDATE() WHERE CentreID = ? AND Role = 'MANAGER' AND UserID <> ?";
            try (PreparedStatement psClear = connection.prepareStatement(clearOtherManagersSql)) {
                psClear.setInt(1, centreId);
                psClear.setInt(2, managerId);
                psClear.executeUpdate();
            }

            String updateCentreSql = "UPDATE Users SET CentreID = ?, UpdatedAt = GETDATE() WHERE UserID = ?";
            try (PreparedStatement ps = connection.prepareStatement(updateCentreSql)) {
                ps.setInt(1, centreId);
                ps.setInt(2, managerId);
                ps.executeUpdate();
            }

            // 7. Ghi nhận lịch sử luân chuyển trong StaffTransferHistory
            String insertTransferLog = "INSERT INTO StaffTransferHistory (UserID, FromCentreID, ToCentreID, TransferredBy, TransferDate, EffectiveDate, OldRole, NewRole, Reason, Notes) "
                    + "VALUES (?, NULL, ?, ?, GETDATE(), CAST(GETDATE() AS DATE), 'MANAGER', 'MANAGER', N'Bổ nhiệm Quản lý phụ trách trung tâm', ?)";
            try (PreparedStatement ps = connection.prepareStatement(insertTransferLog)) {
                ps.setInt(1, managerId);
                ps.setInt(2, centreId);
                ps.setInt(3, adminId);
                ps.setString(4, (notes != null && !notes.trim().isEmpty()) ? notes.trim() : "");
                ps.executeUpdate();
            }

            connection.commit();
            return "SUCCESS";
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Giao dịch bổ nhiệm Quản lý thất bại", e);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            if (e.getErrorCode() == 2601 || e.getErrorCode() == 2627) {
                return "CONCURRENCY_CONFLICT";
            }
            return "ERROR";
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
     * Điều chuyển Quản lý từ trung tâm hiện tại sang trung tâm đích.
     * Quy tắc:
     * - Manager phải ACTIVE và đang có phân công hiện hành.
     * - Centre đích phải ACTIVE, khác Centre hiện tại và CHƯA CÓ Manager phụ trách.
     * - Không tự động ghi đè hay kết thúc phân công của người ở Centre đích.
     * - Thực hiện trong Transaction với khóa UPDLOCK, HOLDLOCK chống xung đột đồng thời.
     */
    public String transferManager(int managerId, int toCentreId, int adminId, String reason, String notes) {
        Connection connection = null;
        try {
            connection = DBContext.getConnection();
            connection.setAutoCommit(false);

            // 1. Kiểm tra Manager với khóa UPDLOCK, HOLDLOCK
            int fromCentreId = -1;
            String checkUserSql = "SELECT UserID, Role, Status, CentreID FROM Users WITH (UPDLOCK, HOLDLOCK) WHERE UserID = ?";
            try (PreparedStatement ps = connection.prepareStatement(checkUserSql)) {
                ps.setInt(1, managerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        connection.rollback();
                        return "MANAGER_NOT_FOUND";
                    }
                    String role = rs.getString("Role");
                    String status = rs.getString("Status");
                    int cid = rs.getInt("CentreID");
                    boolean hasCentre = !rs.wasNull() && cid > 0;

                    if (!"MANAGER".equalsIgnoreCase(role)) {
                        connection.rollback();
                        return "NOT_A_MANAGER";
                    }
                    if (!"ACTIVE".equalsIgnoreCase(status)) {
                        connection.rollback();
                        return "MANAGER_NOT_ACTIVE";
                    }
                    if (!hasCentre) {
                        connection.rollback();
                        return "MANAGER_NOT_ASSIGNED";
                    }
                    fromCentreId = cid;
                }
            }

            // 2. Kiểm tra Centre đích khác Centre hiện tại
            if (fromCentreId == toCentreId) {
                connection.rollback();
                return "SAME_CENTRE";
            }

            // 3. Kiểm tra Centre đích với khóa UPDLOCK, HOLDLOCK
            String checkDestCentreSql = "SELECT CentreID, Status FROM Centres WITH (UPDLOCK, HOLDLOCK) WHERE CentreID = ?";
            try (PreparedStatement ps = connection.prepareStatement(checkDestCentreSql)) {
                ps.setInt(1, toCentreId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        connection.rollback();
                        return "CENTRE_NOT_FOUND";
                    }
                    String cStatus = rs.getString("Status");
                    if (!"ACTIVE".equalsIgnoreCase(cStatus)) {
                        connection.rollback();
                        return "CENTRE_INACTIVE";
                    }
                }
            }

            // 4. Kiểm tra Centre đích có Manager nào đang phân công ACTIVE không (kể cả ACTIVE hay LOCKED)
            String checkDestAssignSql = "SELECT AssignmentID, ManagerID FROM CentreManagerAssignments WITH (UPDLOCK, HOLDLOCK) "
                    + "WHERE CentreID = ? AND Status = 'ACTIVE' AND EndDate IS NULL";
            try (PreparedStatement ps = connection.prepareStatement(checkDestAssignSql)) {
                ps.setInt(1, toCentreId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        // Trung tâm đích đã có Quản lý, tuyệt đối không tự động ghi đè
                        connection.rollback();
                        return "CENTRE_ALREADY_HAS_MANAGER";
                    }
                }
            }

            // 5. Kết thúc phân công hiện tại của Manager này ở trung tâm cũ
            String endCurrentSql = "UPDATE CentreManagerAssignments SET EndDate = GETDATE(), Status = 'ENDED', Notes = ? "
                    + "WHERE ManagerID = ? AND Status = 'ACTIVE' AND EndDate IS NULL";
            try (PreparedStatement ps = connection.prepareStatement(endCurrentSql)) {
                ps.setString(1, "Điều chuyển sang trung tâm mới (CentreID: " + toCentreId + ")");
                ps.setInt(2, managerId);
                ps.executeUpdate();
            }

            // 6. Tạo phân công mới tại trung tâm đích
            String insertAssignSql = "INSERT INTO CentreManagerAssignments (CentreID, ManagerID, AssignedBy, StartDate, EndDate, Status, Notes, CreatedAt) "
                    + "VALUES (?, ?, ?, GETDATE(), NULL, 'ACTIVE', ?, GETDATE())";
            try (PreparedStatement ps = connection.prepareStatement(insertAssignSql)) {
                ps.setInt(1, toCentreId);
                ps.setInt(2, managerId);
                ps.setInt(3, adminId);
                ps.setString(4, (notes != null && !notes.trim().isEmpty()) ? notes.trim() : ("Điều chuyển: " + reason));
                ps.executeUpdate();
            }

            // 7. Cập nhật Users.CentreID: Giải phóng rạp đích khỏi Quản lý cũ (nếu có) và gán cho Quản lý được điều chuyển
            String clearOtherSql = "UPDATE Users SET CentreID = NULL, UpdatedAt = GETDATE() WHERE CentreID = ? AND Role = 'MANAGER' AND UserID <> ?";
            try (PreparedStatement psClear = connection.prepareStatement(clearOtherSql)) {
                psClear.setInt(1, toCentreId);
                psClear.setInt(2, managerId);
                psClear.executeUpdate();
            }

            String updateCentreSql = "UPDATE Users SET CentreID = ?, UpdatedAt = GETDATE() WHERE UserID = ?";
            try (PreparedStatement ps = connection.prepareStatement(updateCentreSql)) {
                ps.setInt(1, toCentreId);
                ps.setInt(2, managerId);
                ps.executeUpdate();
            }

            // 8. Lưu vết vào StaffTransferHistory
            String insertTransferLog = "INSERT INTO StaffTransferHistory (UserID, FromCentreID, ToCentreID, TransferredBy, TransferDate, EffectiveDate, OldRole, NewRole, Reason, Notes) "
                    + "VALUES (?, ?, ?, ?, GETDATE(), CAST(GETDATE() AS DATE), 'MANAGER', 'MANAGER', ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(insertTransferLog)) {
                ps.setInt(1, managerId);
                ps.setInt(2, fromCentreId);
                ps.setInt(3, toCentreId);
                ps.setInt(4, adminId);
                ps.setString(5, reason != null ? reason.trim() : "Điều chuyển Quản lý");
                ps.setString(6, notes != null ? notes.trim() : "");
                ps.executeUpdate();
            }

            connection.commit();
            return "SUCCESS";
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Giao dịch điều chuyển Quản lý thất bại", e);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            if (e.getErrorCode() == 2601 || e.getErrorCode() == 2627) {
                return "CONCURRENCY_CONFLICT";
            }
            return "ERROR";
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
     * Điều chuyển Nhân sự (Staff) thông thường.
     * Bảo đảm quy tắc Rule 12: Giữ nguyên hoạt động của Staff transfer, không can thiệp bảng phân công Manager.
     */
    public String transferRegularStaff(int staffUserId, int toCentreId, int adminId, String reason, String notes, java.sql.Date effectiveDate) {
        return transferRegularStaff(staffUserId, toCentreId, adminId, reason, notes, effectiveDate, null);
    }

    public String transferRegularStaff(int staffUserId, int toCentreId, int adminId, String reason, String notes, java.sql.Date effectiveDate, String newRole) {
        Connection connection = null;
        try {
            connection = DBContext.getConnection();
            connection.setAutoCommit(false);

            // 1. Kiểm tra tài khoản nhân viên
            Integer fromCentreId = null;
            String currentRole = null;
            String checkUserSql = "SELECT UserID, Role, Status, CentreID FROM Users WITH (UPDLOCK, HOLDLOCK) WHERE UserID = ?";
            try (PreparedStatement ps = connection.prepareStatement(checkUserSql)) {
                ps.setInt(1, staffUserId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        connection.rollback();
                        return "USER_NOT_FOUND";
                    }
                    String status = rs.getString("Status");
                    currentRole = rs.getString("Role");
                    int cid = rs.getInt("CentreID");
                    if (!rs.wasNull()) {
                        fromCentreId = cid;
                    }

                    if (!"ACTIVE".equalsIgnoreCase(status)) {
                        connection.rollback();
                        return "USER_NOT_ACTIVE";
                    }
                    if ("MANAGER".equalsIgnoreCase(currentRole)) {
                        connection.rollback();
                        return "USE_MANAGER_TRANSFER";
                    }
                }
            }

            if (fromCentreId != null && fromCentreId == toCentreId) {
                connection.rollback();
                return "SAME_CENTRE";
            }

            // 2. Kiểm tra Centre đích
            String checkCentreSql = "SELECT CentreID, Status FROM Centres WITH (UPDLOCK, HOLDLOCK) WHERE CentreID = ?";
            try (PreparedStatement ps = connection.prepareStatement(checkCentreSql)) {
                ps.setInt(1, toCentreId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        connection.rollback();
                        return "CENTRE_NOT_FOUND";
                    }
                    String cStatus = rs.getString("Status");
                    if (!"ACTIVE".equalsIgnoreCase(cStatus)) {
                        connection.rollback();
                        return "CENTRE_INACTIVE";
                    }
                }
            }

            // 3. Cập nhật Users.CentreID và Role mới (nếu có)
            String targetRole = (newRole != null && !newRole.trim().isEmpty() && !"KEEP".equalsIgnoreCase(newRole.trim())) ? newRole.trim().toUpperCase() : currentRole;
            String updateUserSql = "UPDATE Users SET CentreID = ?, Role = ?, UpdatedAt = GETDATE() WHERE UserID = ?";
            try (PreparedStatement ps = connection.prepareStatement(updateUserSql)) {
                ps.setInt(1, toCentreId);
                ps.setString(2, targetRole);
                ps.setInt(3, staffUserId);
                ps.executeUpdate();
            }

            // 4. Lưu vết vào StaffTransferHistory
            String insertSql = "INSERT INTO StaffTransferHistory (UserID, FromCentreID, ToCentreID, TransferredBy, TransferDate, EffectiveDate, OldRole, NewRole, Reason, Notes) "
                    + "VALUES (?, ?, ?, ?, GETDATE(), ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                ps.setInt(1, staffUserId);
                if (fromCentreId != null) {
                    ps.setInt(2, fromCentreId);
                } else {
                    ps.setNull(2, Types.INTEGER);
                }
                ps.setInt(3, toCentreId);
                ps.setInt(4, adminId);
                if (effectiveDate != null) {
                    ps.setDate(5, effectiveDate);
                } else {
                    ps.setDate(5, new java.sql.Date(System.currentTimeMillis()));
                }
                ps.setString(6, currentRole);
                ps.setString(7, targetRole);
                ps.setString(8, reason != null ? reason.trim() : "Điều chuyển nhân viên");
                ps.setString(9, notes != null ? notes.trim() : "");
                ps.executeUpdate();
            }

            connection.commit();
            return "SUCCESS";
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Giao dịch điều chuyển nhân viên thất bại", e);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return "ERROR";
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
     * Thăng chức nhân viên lên Quản lý và điều chuyển tới trung tâm mới.
     * Trung tâm đích bắt buộc phải CHƯA CÓ Quản lý hoạt động.
     */
    public String promoteStaffToManager(int staffUserId, int toCentreId, int adminId, String reason, String notes, java.sql.Date effectiveDate) {
        Connection connection = null;
        try {
            connection = DBContext.getConnection();
            connection.setAutoCommit(false);

            // 1. Kiểm tra tài khoản nhân viên
            Integer fromCentreId = null;
            String currentRole = null;
            String checkUserSql = "SELECT UserID, Role, Status, CentreID FROM Users WITH (UPDLOCK, HOLDLOCK) WHERE UserID = ?";
            try (PreparedStatement ps = connection.prepareStatement(checkUserSql)) {
                ps.setInt(1, staffUserId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        connection.rollback();
                        return "USER_NOT_FOUND";
                    }
                    String status = rs.getString("Status");
                    currentRole = rs.getString("Role");
                    int cid = rs.getInt("CentreID");
                    if (!rs.wasNull()) {
                        fromCentreId = cid;
                    }
                    if (!"ACTIVE".equalsIgnoreCase(status)) {
                        connection.rollback();
                        return "USER_NOT_ACTIVE";
                    }
                }
            }

            // 2. Kiểm tra Centre đích
            String checkCentreSql = "SELECT CentreID, Status FROM Centres WITH (UPDLOCK, HOLDLOCK) WHERE CentreID = ?";
            try (PreparedStatement ps = connection.prepareStatement(checkCentreSql)) {
                ps.setInt(1, toCentreId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        connection.rollback();
                        return "CENTRE_NOT_FOUND";
                    }
                    String cStatus = rs.getString("Status");
                    if (!"ACTIVE".equalsIgnoreCase(cStatus)) {
                        connection.rollback();
                        return "CENTRE_INACTIVE";
                    }
                }
            }

            // 3. Kiểm tra Centre đích đã có Quản lý ACTIVE chưa
            String checkDestAssignSql = "SELECT AssignmentID FROM CentreManagerAssignments WITH (UPDLOCK, HOLDLOCK) "
                    + "WHERE CentreID = ? AND Status = 'ACTIVE' AND EndDate IS NULL";
            try (PreparedStatement ps = connection.prepareStatement(checkDestAssignSql)) {
                ps.setInt(1, toCentreId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        connection.rollback();
                        return "CENTRE_ALREADY_HAS_MANAGER";
                    }
                }
            }

            // 4. Tạo bản ghi bổ nhiệm trong CentreManagerAssignments
            String insertAssignSql = "INSERT INTO CentreManagerAssignments (CentreID, ManagerID, AssignedBy, StartDate, EndDate, Status, Notes, CreatedAt) "
                    + "VALUES (?, ?, ?, GETDATE(), NULL, 'ACTIVE', ?, GETDATE())";
            try (PreparedStatement ps = connection.prepareStatement(insertAssignSql)) {
                ps.setInt(1, toCentreId);
                ps.setInt(2, staffUserId);
                ps.setInt(3, adminId);
                ps.setString(4, (notes != null && !notes.trim().isEmpty()) ? notes.trim() : ("Thăng chức & điều chuyển Quản lý: " + reason));
                ps.executeUpdate();
            }

            // 5. Cập nhật Users: Role = 'MANAGER', CentreID = toCentreId
            String updateUserSql = "UPDATE Users SET CentreID = ?, Role = 'MANAGER', UpdatedAt = GETDATE() WHERE UserID = ?";
            try (PreparedStatement ps = connection.prepareStatement(updateUserSql)) {
                ps.setInt(1, toCentreId);
                ps.setInt(2, staffUserId);
                ps.executeUpdate();
            }

            // 6. Lưu vết vào StaffTransferHistory
            String insertSql = "INSERT INTO StaffTransferHistory (UserID, FromCentreID, ToCentreID, TransferredBy, TransferDate, EffectiveDate, OldRole, NewRole, Reason, Notes) "
                    + "VALUES (?, ?, ?, ?, GETDATE(), ?, ?, 'MANAGER', ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                ps.setInt(1, staffUserId);
                if (fromCentreId != null) {
                    ps.setInt(2, fromCentreId);
                } else {
                    ps.setNull(2, Types.INTEGER);
                }
                ps.setInt(3, toCentreId);
                ps.setInt(4, adminId);
                if (effectiveDate != null) {
                    ps.setDate(5, effectiveDate);
                } else {
                    ps.setDate(5, new java.sql.Date(System.currentTimeMillis()));
                }
                ps.setString(6, currentRole);
                ps.setString(7, reason != null ? reason.trim() : "Thăng chức lên Quản lý");
                ps.setString(8, notes != null ? notes.trim() : "");
                ps.executeUpdate();
            }

            connection.commit();
            return "SUCCESS";
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Thăng chức Quản lý thất bại", e);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            if (e.getErrorCode() == 2601 || e.getErrorCode() == 2627) {
                return "CONCURRENCY_CONFLICT";
            }
            return "ERROR";
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
     * Chuyển Quản lý sang vai trò nhân viên thông thường tại trung tâm mới.
     * Kết thúc nhiệm kỳ Manager tại trung tâm cũ.
     */
    public String demoteManagerToStaff(int managerId, int toCentreId, String targetRole, int adminId, String reason, String notes, java.sql.Date effectiveDate) {
        Connection connection = null;
        try {
            connection = DBContext.getConnection();
            connection.setAutoCommit(false);

            // 1. Kiểm tra Manager
            Integer fromCentreId = null;
            String checkUserSql = "SELECT UserID, Role, Status, CentreID FROM Users WITH (UPDLOCK, HOLDLOCK) WHERE UserID = ?";
            try (PreparedStatement ps = connection.prepareStatement(checkUserSql)) {
                ps.setInt(1, managerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        connection.rollback();
                        return "USER_NOT_FOUND";
                    }
                    int cid = rs.getInt("CentreID");
                    if (!rs.wasNull()) fromCentreId = cid;
                }
            }

            // 2. Kết thúc phân công Manager trong CentreManagerAssignments
            String endCurrentSql = "UPDATE CentreManagerAssignments SET EndDate = GETDATE(), Status = 'ENDED', "
                    + "Notes = ISNULL(Notes, '') + N' [Điều chuyển sang nhân viên: ' + ? + ']' "
                    + "WHERE ManagerID = ? AND Status = 'ACTIVE' AND EndDate IS NULL";
            try (PreparedStatement ps = connection.prepareStatement(endCurrentSql)) {
                ps.setString(1, reason != null ? reason : "");
                ps.setInt(2, managerId);
                ps.executeUpdate();
            }

            // 3. Cập nhật Role và CentreID của User
            String newStaffRole = (targetRole != null && !targetRole.trim().isEmpty()) ? targetRole.trim().toUpperCase() : "TICKET_SALES_STAFF";
            String updateUserSql = "UPDATE Users SET CentreID = ?, Role = ?, UpdatedAt = GETDATE() WHERE UserID = ?";
            try (PreparedStatement ps = connection.prepareStatement(updateUserSql)) {
                ps.setInt(1, toCentreId);
                ps.setString(2, newStaffRole);
                ps.setInt(3, managerId);
                ps.executeUpdate();
            }

            // 4. Ghi nhận nhật ký
            String insertSql = "INSERT INTO StaffTransferHistory (UserID, FromCentreID, ToCentreID, TransferredBy, TransferDate, EffectiveDate, OldRole, NewRole, Reason, Notes) "
                    + "VALUES (?, ?, ?, ?, GETDATE(), ?, 'MANAGER', ?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                ps.setInt(1, managerId);
                if (fromCentreId != null) ps.setInt(2, fromCentreId);
                else ps.setNull(2, Types.INTEGER);
                ps.setInt(3, toCentreId);
                ps.setInt(4, adminId);
                if (effectiveDate != null) ps.setDate(5, effectiveDate);
                else ps.setDate(5, new java.sql.Date(System.currentTimeMillis()));
                ps.setString(6, newStaffRole);
                ps.setString(7, reason != null ? reason.trim() : "Chuyển đổi vị trí công tác");
                ps.setString(8, notes != null ? notes.trim() : "");
                ps.executeUpdate();
            }

            connection.commit();
            return "SUCCESS";
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Chuyển Quản lý sang Staff thất bại", e);
            if (connection != null) {
                try { connection.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            return "ERROR";
        } finally {
            if (connection != null) {
                try { connection.setAutoCommit(true); connection.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
    }

    /**
     * Phương thức tương thích ngược cho bổ nhiệm Manager.
     */
    public boolean assignManagerToCentreWithHistory(int centreId, int managerId, int adminId, String notes) {
        return "SUCCESS".equals(assignManager(centreId, managerId, adminId, notes));
    }

    /**
     * Miễn nhiệm Quản lý đang phụ trách trung tâm chiếu phim.
     *
     * @param centreId  mã trung tâm chiếu phim
     * @param managerId mã Quản lý
     * @param adminId   mã Admin thực hiện miễn nhiệm
     * @param reason    lý do miễn nhiệm
     * @return true nếu miễn nhiệm thành công, ngược lại false
     */
    public boolean unassignManager(int centreId, int managerId, int adminId, String reason) {
        String endAssignment = "UPDATE CentreManagerAssignments SET EndDate = GETDATE(), Status = 'ENDED', "
                + "Notes = CASE WHEN Notes IS NULL OR LTRIM(RTRIM(Notes)) = '' THEN ? ELSE Notes + N' [Miễn nhiệm: ' + ? + N']' END "
                + "WHERE CentreID = ? AND ManagerID = ? AND (Status = 'ACTIVE' OR Status IS NULL) AND EndDate IS NULL";
        String clearCentre = "UPDATE Users SET CentreID = NULL, UpdatedAt = GETDATE() WHERE UserID = ? AND Role = 'MANAGER'";

        Connection connection = null;
        try {
            connection = DBContext.getConnection();
            connection.setAutoCommit(false);

            int endedRows;
            try (PreparedStatement ps1 = connection.prepareStatement(endAssignment)) {
                String noteReason = (reason != null && !reason.trim().isEmpty()) ? reason.trim() : "Miễn nhiệm quản lý trung tâm";
                ps1.setString(1, "Miễn nhiệm: " + noteReason);
                ps1.setString(2, noteReason);
                ps1.setInt(3, centreId);
                ps1.setInt(4, managerId);
                endedRows = ps1.executeUpdate();
            }

            // Không được giải phóng Manager nếu phân công gửi lên không còn tồn tại
            // hoặc không thuộc đúng chi nhánh đang thao tác.
            if (endedRows == 0) {
                connection.rollback();
                return false;
            }

            try (PreparedStatement ps2 = connection.prepareStatement(clearCentre)) {
                ps2.setInt(1, managerId);
                ps2.executeUpdate();
            }

            connection.commit();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Miễn nhiệm Quản lý thất bại", e);
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
     * Giải phóng cụm rạp mà Quản lý đang phụ trách theo UserId (dùng khi tài khoản bị khóa/tạm ngưng).
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
     * Đồng bộ và giải phóng các cụm rạp có Quản lý đang bị khóa/tạm ngưng tài khoản.
     * Khi tài khoản Quản lý không ở trạng thái ACTIVE, rạp đó được giải phóng thành "Chưa có Quản lý" (vô chủ)
     * để Admin có thể bổ nhiệm Quản lý mới ngay lập tức.
     */
    public void syncLockedManagersAssignments() {
        // 1. Chấm dứt phân công trong CentreManagerAssignments của Quản lý không ở trạng thái ACTIVE
        String endAssignmentsSql = "UPDATE cma "
                + "SET cma.EndDate = GETDATE(), cma.Status = 'ENDED', "
                + "cma.Notes = ISNULL(cma.Notes, '') + N' [Chấm dứt phân công do tài khoản Quản lý bị khóa/tạm ngưng]' "
                + "FROM CentreManagerAssignments cma "
                + "JOIN Users u ON u.UserID = cma.ManagerID "
                + "WHERE cma.Status = 'ACTIVE' AND cma.EndDate IS NULL AND UPPER(u.Status) <> 'ACTIVE'";

        // 2. Xóa sạch CentreID trong bảng Users của TẤT CẢ Quản lý KHÔNG có phân công ACTIVE hợp lệ
        // (đặc biệt khi mở lại tài khoản cho Quản lý cũ mà rạp đó đã có người mới)
        String clearUnassignedManagersSql = "UPDATE Users SET CentreID = NULL, UpdatedAt = GETDATE() "
                + "WHERE Role = 'MANAGER' AND CentreID IS NOT NULL "
                + "AND UserID NOT IN (SELECT ManagerID FROM CentreManagerAssignments WHERE Status = 'ACTIVE' AND EndDate IS NULL)";

        // 3. Đồng bộ chuẩn xác Users.CentreID với phân công ACTIVE hiện tại
        String syncActiveManagersSql = "UPDATE u SET u.CentreID = cma.CentreID, u.UpdatedAt = GETDATE() "
                + "FROM Users u "
                + "JOIN CentreManagerAssignments cma ON cma.ManagerID = u.UserID AND cma.Status = 'ACTIVE' AND cma.EndDate IS NULL "
                + "WHERE u.Role = 'MANAGER' AND (u.CentreID IS NULL OR u.CentreID <> cma.CentreID)";

        try (Connection conn = DBContext.getConnection()) {
            try (PreparedStatement ps1 = conn.prepareStatement(endAssignmentsSql)) {
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = conn.prepareStatement(clearUnassignedManagersSql)) {
                ps2.executeUpdate();
            }
            try (PreparedStatement ps3 = conn.prepareStatement(syncActiveManagersSql)) {
                ps3.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi đồng bộ phân công của Quản lý", e);
        }
    }

    /**
     * Lấy lịch sử bổ nhiệm, miễn nhiệm Quản lý trung tâm chiếu phim.
     */
    public List<ManagerAssignment> getManagerAssignmentHistory(Integer centreId, Integer managerId) {
        StringBuilder sql = new StringBuilder(
                "SELECT bma.AssignmentID, bma.CentreID, bma.ManagerID, bma.AssignedBy, "
                + "bma.StartDate, bma.EndDate, bma.Status, bma.Notes, bma.CreatedAt, "
                + "b.CentreName, b.City AS CentreCity, "
                + "u.FullName AS ManagerName, u.Account AS ManagerAccount, u.Email AS ManagerEmail, "
                + "adm.FullName AS AssignedByName "
                + "FROM CentreManagerAssignments bma "
                + "JOIN Centres b ON b.CentreID = bma.CentreID "
                + "JOIN Users u ON u.UserID = bma.ManagerID "
                + "JOIN Users adm ON adm.UserID = bma.AssignedBy "
                + "WHERE 1=1 "
        );

        if (centreId != null) {
            sql.append("AND bma.CentreID = ? ");
        }
        if (managerId != null) {
            sql.append("AND bma.ManagerID = ? ");
        }
        sql.append("ORDER BY bma.StartDate DESC, bma.AssignmentID DESC");

        List<ManagerAssignment> list = new ArrayList<>();
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (centreId != null) {
                ps.setInt(paramIndex++, centreId);
            }
            if (managerId != null) {
                ps.setInt(paramIndex++, managerId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ManagerAssignment item = new ManagerAssignment();
                    item.setAssignmentId(rs.getInt("AssignmentID"));
                    item.setCentreId(rs.getInt("CentreID"));
                    item.setCentreName(rs.getString("CentreName"));
                    item.setCentreCity(rs.getString("CentreCity"));
                    item.setManagerId(rs.getInt("ManagerID"));
                    item.setManagerName(rs.getString("ManagerName"));
                    item.setManagerAccount(rs.getString("ManagerAccount"));
                    item.setManagerEmail(rs.getString("ManagerEmail"));
                    item.setAssignedBy(rs.getInt("AssignedBy"));
                    item.setAssignedByName(rs.getString("AssignedByName"));
                    item.setStartDate(rs.getTimestamp("StartDate"));
                    item.setEndDate(rs.getTimestamp("EndDate"));
                    item.setStatus(rs.getString("Status"));
                    item.setNotes(rs.getString("Notes"));
                    item.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    list.add(item);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Không thể tải lịch sử bổ nhiệm Quản lý", e);
        }
        return list;
    }

    /**
     * Lấy danh sách khách hàng phục vụ trang quản trị kèm số vé đã đặt và tổng chi tiêu.
     *
     * @param search từ khóa tìm kiếm
     * @param status bộ lọc trạng thái (ACTIVE, INACTIVE, LOCKED hoặc ALL)
     * @return danh sách khách hàng
     */
    public List<User> getCustomers(String search, String status) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT u.*, ")
           .append("  (SELECT COUNT(*) FROM Bookings b WHERE b.UserID = u.UserID) AS BookingCount, ")
           .append("  (SELECT ISNULL(SUM(b.TotalAmount), 0) FROM Bookings b WHERE b.UserID = u.UserID AND b.BookingStatus <> 'CANCELLED') AS TotalSpent ")
           .append("FROM Users u ")
           .append("WHERE u.Role = 'CUSTOMER' ");

        List<Object> params = new ArrayList<>();

        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            String filterStatus = status.trim();
            sql.append("AND u.Status = ? ");
            params.add(filterStatus);
        }

        if (search != null && !search.isBlank()) {
            sql.append("AND (u.FullName LIKE ? OR u.Account LIKE ? OR u.Email LIKE ? OR u.Phone LIKE ? ")
               .append("OR ('KH' + RIGHT('00000' + CAST(u.UserID AS VARCHAR(10)), 5)) LIKE ? ")
               .append("OR CAST(u.UserID AS VARCHAR(10)) LIKE ?) ");
            String keyword = "%" + search.trim() + "%";
            params.add(keyword);
            params.add(keyword);
            params.add(keyword);
            params.add(keyword);
            params.add(keyword);
            params.add(keyword);
        }

        sql.append("ORDER BY u.CreatedAt DESC, u.UserID DESC");

        List<User> list = new ArrayList<>();
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User u = mapUser(rs);
                    u.setBookingCount(rs.getInt("BookingCount"));
                    u.setTotalSpent(rs.getBigDecimal("TotalSpent"));
                    list.add(u);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Không thể tải danh sách khách hàng", e);
        }
        return list;
    }

    /**
     * Đếm tổng số lượng khách hàng theo trạng thái tài khoản.
     *
     * @param status trạng thái tài khoản hoặc ALL
     * @return số lượng khách hàng tương ứng
     */
    public int countCustomersByStatus(String status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM Users WHERE Role = 'CUSTOMER' ");
        List<Object> params = new ArrayList<>();
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            String filterStatus = status.trim();
            sql.append("AND Status = ? ");
            params.add(filterStatus);
        }
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Không thể đếm số lượng khách hàng", e);
        }
        return 0;
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
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
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
