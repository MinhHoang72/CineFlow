package dao;

import model.BookingHistory;
import model.ManagerAssignment;
import model.StaffTransfer;
import model.User;
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
     * Tìm thông tin người dùng theo tên tài khoản hoặc địa chỉ email.
     *
     * @param accountOrEmail tên tài khoản hoặc email cần tìm
     * @return đối tượng User nếu tìm thấy, ngược lại null
     */
    public User getUserByAccountOrEmail(String accountOrEmail) {
        String sql = "SELECT * FROM Users WHERE Account = ? OR Email = ?";

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
        String sql = "SELECT * FROM Users WHERE Account = ?";

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
        String sql = "SELECT * FROM Users WHERE Email = ?";

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
                int branchId = rs.getInt("BranchID");
                user.setCentreId(rs.wasNull() ? null : branchId);
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
                user.setCentreName(rs.getString("BranchName"));
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
        String sql = "SELECT * FROM Users WHERE UserID = ?";

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
    public List<BookingHistory> getBookingHistory(int userId) {
        String sql = "SELECT b.BookingCode, m.Title, br.CentreName, s.StartTime, "
                + "b.BookingStatus, b.TotalAmount, b.CreatedAt "
                + "FROM Bookings b "
                + "JOIN Showtimes s ON s.ShowtimeID = b.ShowtimeID "
                + "JOIN Movies m ON m.MovieID = s.MovieID "
                + "JOIN Halls h ON h.HallID = s.HallID "
                + "JOIN Centres br ON br.CentreID = h.CentreID "
                + "WHERE b.UserID = ? ORDER BY b.CreatedAt DESC";
        List<BookingHistory> history = new ArrayList<>();
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BookingHistory item = new BookingHistory();
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
        StringBuilder sql = new StringBuilder("SELECT u.*, b.CentreName FROM Users u "
                + "LEFT JOIN Centres b ON b.CentreID = u.CentreID "
                + "WHERE u.Role = 'STAFF' ");
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
                + "WHERE UserID = ? AND Role IN ('STAFF','MANAGER')";
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
     * Tạo tài khoản nhân viên mới (STAFF).
     *
     * @param user đối tượng người dùng có vai trò nhân viên
     * @return true nếu tạo thành công, ngược lại false
     */
    public boolean createStaff(User user) {
        String sql = "INSERT INTO Users (FullName, Account, Email, Password, Phone, Role, Status, CentreID, EmailVerified, CreatedAt) "
                + "VALUES (?, ?, ?, ?, ?, 'STAFF', 'ACTIVE', ?, 1, GETDATE())";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
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
    public int createManager(User user) {
        String sql = "INSERT INTO Users (FullName, Account, Email, Password, Phone, Role, Status, CentreID, EmailVerified, CreatedAt) "
                + "VALUES (?, ?, ?, ?, ?, 'MANAGER', 'ACTIVE', ?, 1, GETDATE())";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
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
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Không thể tạo tài khoản Quản lý", e);
        }
        return -1;
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
        StringBuilder sql = new StringBuilder("SELECT u.*, b.CentreName FROM Users u "
                + "LEFT JOIN Centres b ON b.CentreID = u.CentreID "
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
                + "WHERE u.Role IN ('STAFF', 'MANAGER') "
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
     * @param reason       lý do điều động
     * @param notes        ghi chú bổ sung
     * @return true nếu luân chuyển thành công, ngược lại false
     */
    public boolean transferStaff(int userId, Integer fromCentreId, int toCentreId, int adminId, String reason, String notes) {
        String updateSql = "UPDATE Users SET CentreID = ?, UpdatedAt = GETDATE() WHERE UserID = ? AND Role = 'STAFF'";
        String insertSql = "INSERT INTO StaffTransferHistory (UserID, FromCentreID, ToCentreID, TransferredBy, TransferDate, Reason, Notes) "
                + "VALUES (?, ?, ?, ?, GETDATE(), ?, ?)";

        Connection connection = null;
        try {
            connection = DBContext.getConnection();
            connection.setAutoCommit(false);

            try (PreparedStatement updatePs = connection.prepareStatement(updateSql)) {
                updatePs.setInt(1, toCentreId);
                updatePs.setInt(2, userId);
                updatePs.executeUpdate();
            }

            try (PreparedStatement insertPs = connection.prepareStatement(insertSql)) {
                insertPs.setInt(1, userId);
                if (fromCentreId != null) {
                    insertPs.setInt(2, fromCentreId);
                } else {
                    insertPs.setNull(2, Types.INTEGER);
                }
                insertPs.setInt(3, toCentreId);
                insertPs.setInt(4, adminId);
                insertPs.setString(5, reason);
                insertPs.setString(6, notes);
                insertPs.executeUpdate();
            }

            connection.commit();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Giao dịch luân chuyển nhân sự thất bại", e);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }
    }

    /**
     * Lấy lịch sử điều động, luân chuyển nhân sự theo trung tâm hoặc nhân viên.
     */
    public List<StaffTransfer> getTransferHistory(Integer centreId, Integer userId) {
        StringBuilder sql = new StringBuilder(
                "SELECT t.TransferID, t.UserID, t.FromCentreID, t.ToCentreID, t.TransferredBy, "
                + "t.TransferDate, t.Reason, t.Notes, "
                + "u.FullName AS UserName, u.Account AS UserAccount, u.Email AS UserEmail, u.Role AS UserRole, "
                + "fb.CentreName AS FromCentreName, tb.CentreName AS ToCentreName, "
                + "adm.FullName AS TransferredByName "
                + "FROM StaffTransferHistory t "
                + "JOIN Users u ON u.UserID = t.UserID "
                + "LEFT JOIN Centres fb ON fb.CentreID = t.FromCentreID "
                + "JOIN Centres tb ON tb.CentreID = t.ToCentreID "
                + "JOIN Users adm ON adm.UserID = t.TransferredBy "
                + "WHERE 1=1 "
        );

        if (centreId != null) {
            sql.append("AND (t.FromCentreID = ? OR t.ToCentreID = ?) ");
        }
        if (userId != null) {
            sql.append("AND t.UserID = ? ");
        }
        sql.append("ORDER BY t.TransferDate DESC");

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
     * Cập nhật trạng thái hoạt động của tài khoản người dùng (ACTIVE, INACTIVE, LOCKED...).
     */
    public boolean updateUserStatus(int userId, String status) {
        if ("SUSPENDED".equalsIgnoreCase(status)) {
            status = "LOCKED";
        }
        String sql = "UPDATE Users SET Status = ?, UpdatedAt = GETDATE() WHERE UserID = ?";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
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
     * Bổ nhiệm Quản lý phụ trách trung tâm chiếu phim và ghi nhận vào lịch sử bổ nhiệm.
     *
     * @param centreId  mã trung tâm chiếu phim
     * @param managerId mã người dùng vai trò Quản lý
     * @param adminId   mã Admin ra quyết định bổ nhiệm
     * @param notes     ghi chú bổ nhiệm
     * @return true nếu bổ nhiệm thành công, ngược lại false
     */
    public boolean assignManagerToCentreWithHistory(int centreId, int managerId, int adminId, String notes) {
        String endCurrentCentreAssignment = "UPDATE CentreManagerAssignments SET EndDate = GETDATE(), Status = 'REPLACED' WHERE CentreID = ? AND EndDate IS NULL";
        String endCurrentManagerAssignment = "UPDATE CentreManagerAssignments SET EndDate = GETDATE(), Status = 'REPLACED' WHERE ManagerID = ? AND EndDate IS NULL";
        String insertNewAssignment = "INSERT INTO CentreManagerAssignments (CentreID, ManagerID, AssignedBy, StartDate, EndDate, Status, Notes, CreatedAt) "
                + "VALUES (?, ?, ?, GETDATE(), NULL, 'ACTIVE', ?, GETDATE())";
        String clearOldManagerCentre = "UPDATE Users SET CentreID = NULL, UpdatedAt = GETDATE() WHERE CentreID = ? AND Role = 'MANAGER' AND UserID <> ?";
        String updateNewManagerCentre = "UPDATE Users SET CentreID = ?, UpdatedAt = GETDATE() WHERE UserID = ? AND Role = 'MANAGER'";

        Connection connection = null;
        try {
            connection = DBContext.getConnection();
            connection.setAutoCommit(false);

            // 1. Kết thúc nhiệm kỳ Quản lý cũ tại trung tâm này (nếu có)
            try (PreparedStatement ps1 = connection.prepareStatement(endCurrentCentreAssignment)) {
                ps1.setInt(1, centreId);
                ps1.executeUpdate();
            }

            // 2. Kết thúc nhiệm kỳ hiện tại của Quản lý này ở trung tâm khác (nếu có)
            try (PreparedStatement ps2 = connection.prepareStatement(endCurrentManagerAssignment)) {
                ps2.setInt(1, managerId);
                ps2.executeUpdate();
            }

            // 3. Tạo bản ghi bổ nhiệm mới
            try (PreparedStatement ps3 = connection.prepareStatement(insertNewAssignment)) {
                ps3.setInt(1, centreId);
                ps3.setInt(2, managerId);
                ps3.setInt(3, adminId);
                ps3.setString(4, notes != null ? notes : "Bổ nhiệm quản lý trung tâm");
                ps3.executeUpdate();
            }

            // 4. Đồng bộ Users.CentreID: Xóa CentreID ở các quản lý khác của trung tâm này
            try (PreparedStatement ps4 = connection.prepareStatement(clearOldManagerCentre)) {
                ps4.setInt(1, centreId);
                ps4.setInt(2, managerId);
                ps4.executeUpdate();
            }

            // 5. Cập nhật CentreID mới cho Quản lý
            try (PreparedStatement ps5 = connection.prepareStatement(updateNewManagerCentre)) {
                ps5.setInt(1, centreId);
                ps5.setInt(2, managerId);
                ps5.executeUpdate();
            }

            connection.commit();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Giao dịch bổ nhiệm Quản lý thất bại", e);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }
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
        String endAssignment = "UPDATE CentreManagerAssignments SET EndDate = GETDATE(), Status = 'ENDED', Notes = ? "
                + "WHERE CentreID = ? AND ManagerID = ? AND EndDate IS NULL";
        String clearCentre = "UPDATE Users SET CentreID = NULL, UpdatedAt = GETDATE() WHERE UserID = ? AND Role = 'MANAGER'";

        Connection connection = null;
        try {
            connection = DBContext.getConnection();
            connection.setAutoCommit(false);

            try (PreparedStatement ps1 = connection.prepareStatement(endAssignment)) {
                ps1.setString(1, reason != null ? reason : "Miễn nhiệm quản lý trung tâm");
                ps1.setInt(2, centreId);
                ps1.setInt(3, managerId);
                ps1.executeUpdate();
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
            String filterStatus = "SUSPENDED".equalsIgnoreCase(status.trim()) ? "LOCKED" : status.trim();
            sql.append("AND u.Status = ? ");
            params.add(filterStatus);
        }

        if (search != null && !search.isBlank()) {
            sql.append("AND (u.FullName LIKE ? OR u.Account LIKE ? OR u.Email LIKE ? OR u.Phone LIKE ?) ");
            String keyword = "%" + search.trim() + "%";
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
            String filterStatus = "SUSPENDED".equalsIgnoreCase(status.trim()) ? "LOCKED" : status.trim();
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
}
