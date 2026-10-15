package dao;

import model.User;
import model.BookingHistory;
import model.StaffTransfer;
import model.ManagerAssignment;
import utils.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    private static final Logger LOGGER = Logger.getLogger(UserDAO.class.getName());

    // =========================================================
    // 1. CHECK ACCOUNT EXISTS
    // =========================================================
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

    // =========================================================
    // 2. CHECK EMAIL EXISTS
    // =========================================================
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

    public boolean register(User user) {
        return register(user, null, null);
    }

    public boolean register(User user, String otp, java.sql.Timestamp expiry) {
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
            LOGGER.log(Level.SEVERE, "Registration INSERT failed", e);
            e.printStackTrace();
            return false;
        }
    }

    public boolean activateUser(int userId) {
        String sql = "UPDATE Users SET Status = 'ACTIVE', EmailVerified = 1, ResetToken = NULL, ResetTokenExpiry = NULL, UpdatedAt = GETDATE() WHERE UserID = ?";
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "activateUser failed", e);
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateActivationOtp(int userId, String otp, java.sql.Timestamp expiry) {
        String sql = "UPDATE Users SET ResetToken = ?, ResetTokenExpiry = ?, UpdatedAt = GETDATE() WHERE UserID = ?";
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, otp);
            ps.setTimestamp(2, expiry);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "updateActivationOtp failed", e);
            e.printStackTrace();
            return false;
        }
    }


    // =========================================================
    // 4. FIND USER BY ACCOUNT OR EMAIL
    // =========================================================
    public User getUserByAccountOrEmail(String accountOrEmail) {

        String sql = "SELECT * FROM Users "
                + "WHERE Account = ? OR Email = ?";

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

    public User getUserByAccount(String account) {

        String sql = "SELECT * FROM Users WHERE Account = ?";

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

    // =========================================================
    // 5. FIND USER BY EMAIL
    // =========================================================
    public User getUserByEmail(String email) {

        String sql = "SELECT * FROM Users WHERE Email = ?";

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

    // =========================================================
    // 6. UPDATE PASSWORD
    // =========================================================
    public boolean updatePassword(int userId, String newPassword) {

        String sql = "UPDATE Users "
                + "SET Password = ?, UpdatedAt = GETDATE() "
                + "WHERE UserID = ?";

        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, newPassword);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // =========================================================
    // 7. SAVE RESET TOKEN
    // =========================================================
    public boolean saveResetToken(int userId, String token,
            java.sql.Timestamp expiry) {

        String sql = "UPDATE Users "
                + "SET ResetToken = ?, ResetTokenExpiry = ? "
                + "WHERE UserID = ?";

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

    // =========================================================
    // 8. FIND USER BY RESET TOKEN
    // =========================================================
    public User getUserByResetToken(String token) {

        String sql = "SELECT * FROM Users "
                + "WHERE ResetToken = ? "
                + "AND ResetTokenExpiry > GETDATE()";

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

    // =========================================================
    // 9. RESET PASSWORD + CLEAR TOKEN
    // =========================================================
    public boolean resetPassword(int userId, String newPassword) {

        String sql = "UPDATE Users "
                + "SET Password = ?, "
                + "ResetToken = NULL, "
                + "ResetTokenExpiry = NULL, "
                + "UpdatedAt = GETDATE() "
                + "WHERE UserID = ?";

        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, newPassword);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // =========================================================
    // 10. MAP RESULTSET → USER
    // =========================================================
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
        int branchId = rs.getInt("BranchID");
        user.setBranchId(rs.wasNull() ? null : branchId);

        user.setCreatedAt(rs.getTimestamp("CreatedAt"));
        user.setUpdatedAt(rs.getTimestamp("UpdatedAt"));

        user.setEmailVerified(rs.getBoolean("EmailVerified"));
        user.setPhoneVerified(rs.getBoolean("PhoneVerified"));

        user.setLastLoginAt(rs.getTimestamp("LastLoginAt"));

        user.setResetToken(rs.getString("ResetToken"));
        user.setResetTokenExpiry(rs.getTimestamp("ResetTokenExpiry"));

        try {
            user.setBranchName(rs.getString("BranchName"));
        } catch (SQLException ignored) {
        }

        return user;
    }

    public boolean updateLastLogin(int userId) {

        String sql = "UPDATE Users "
                + "SET LastLoginAt = GETDATE(), "
                + "UpdatedAt = GETDATE() "
                + "WHERE UserID = ?";

        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean changePassword(int userId, String newPassword) {

        String sql = "UPDATE Users "
                + "SET Password = ?, "
                + "UpdatedAt = GETDATE() "
                + "WHERE UserID = ?";

        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, newPassword);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public User getUserById(int userId) {

        String sql = "SELECT * FROM Users WHERE UserID = ?";

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

    public boolean isEmailUsedByOtherUser(String email, int userId) {
        String sql = "SELECT UserID FROM Users WHERE Email = ? AND UserID <> ?";
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not check email", e);
            return true;
        }
    }

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
            LOGGER.log(Level.SEVERE, "Could not update profile", e);
            return false;
        }
    }

    public List<BookingHistory> getBookingHistory(int userId) {
        String sql = "SELECT b.BookingCode, m.Title, br.BranchName, s.StartTime, "
                + "b.BookingStatus, b.TotalAmount, b.CreatedAt "
                + "FROM Bookings b JOIN Showtimes s ON s.ShowtimeID = b.ShowtimeID "
                + "JOIN Movies m ON m.MovieID = s.MovieID "
                + "JOIN Halls h ON h.HallID = s.HallID "
                + "JOIN Branches br ON br.BranchID = h.BranchID "
                + "WHERE b.UserID = ? ORDER BY b.CreatedAt DESC";
        List<BookingHistory> history = new ArrayList<>();
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BookingHistory item = new BookingHistory();
                    item.setBookingCode(rs.getString("BookingCode"));
                    item.setMovieTitle(rs.getString("Title"));
                    item.setBranchName(rs.getString("BranchName"));
                    item.setStartTime(rs.getTimestamp("StartTime"));
                    item.setBookingStatus(rs.getString("BookingStatus"));
                    item.setTotalAmount(rs.getBigDecimal("TotalAmount"));
                    item.setCreatedAt(rs.getTimestamp("CreatedAt"));
                    history.add(item);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not load booking history", e);
        }
        return history;
    }

    public List<User> getStaffDirectory(Integer branchId) {
        return getStaffDirectory(branchId, null);
    }

    public List<User> getStaffDirectory(Integer branchId, String search) {
        StringBuilder sql = new StringBuilder("SELECT u.*, b.BranchName FROM Users u "
                + "LEFT JOIN Branches b ON b.BranchID = u.BranchID "
                + "WHERE u.Role = 'STAFF' ");
        if (branchId != null) {
            sql.append("AND u.BranchID = ? ");
        }

        Integer searchId = null;
        String keyword = null;
        if (search != null && !search.trim().isEmpty()) {
            keyword = search.trim();
            String cleanDigits = keyword.replaceAll("(?i)^[#A-Z]+", "");
            if (!cleanDigits.isEmpty() && cleanDigits.matches("^\\d+$")) {
                try {
                    searchId = Integer.parseInt(cleanDigits);
                } catch (NumberFormatException ignored) {}
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
            if (branchId != null) {
                ps.setInt(pIdx++, branchId);
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
            LOGGER.log(Level.SEVERE, "Could not load staff directory with search", e);
        }
        return staff;
    }

    public boolean updateStaffAssignment(int userId, Integer branchId, String status) {
        String sql = "UPDATE Users SET BranchID = ?, Status = ?, UpdatedAt = GETDATE() "
                + "WHERE UserID = ? AND Role IN ('STAFF','MANAGER')";
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            if (branchId == null) ps.setNull(1, java.sql.Types.INTEGER); else ps.setInt(1, branchId);
            ps.setString(2, status);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not update staff account", e);
            return false;
        }
    }

    public boolean createStaff(User user) {
        String sql = "INSERT INTO Users (FullName, Account, Email, Password, Phone, Role, Status, BranchID, EmailVerified, CreatedAt) "
                + "VALUES (?, ?, ?, ?, ?, 'STAFF', 'ACTIVE', ?, 1, GETDATE())";
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getAccount());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPassword());
            ps.setString(5, user.getPhone());
            if (user.getBranchId() != null) {
                ps.setInt(6, user.getBranchId());
            } else {
                ps.setNull(6, java.sql.Types.INTEGER);
            }
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not create staff account", e);
            return false;
        }
    }

    public int createManager(User user) {
        String sql = "INSERT INTO Users (FullName, Account, Email, Password, Phone, Role, Status, BranchID, EmailVerified, CreatedAt) "
                + "VALUES (?, ?, ?, ?, ?, 'MANAGER', 'ACTIVE', ?, 1, GETDATE())";
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getAccount());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPassword());
            ps.setString(5, user.getPhone());
            if (user.getBranchId() != null) {
                ps.setInt(6, user.getBranchId());
            } else {
                ps.setNull(6, java.sql.Types.INTEGER);
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
            LOGGER.log(Level.SEVERE, "Could not create manager account", e);
        }
        return -1;
    }

    public boolean assignManagerToBranch(int managerUserId, int branchId) {
        String sql = "UPDATE Users SET BranchID = ?, UpdatedAt = GETDATE() WHERE UserID = ? AND Role = 'MANAGER'";
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, branchId);
            ps.setInt(2, managerUserId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not assign manager to branch", e);
            return false;
        }
    }

    public List<User> getManagers() {
        return getManagers(null);
    }

    public List<User> getManagers(String search) {
        StringBuilder sql = new StringBuilder("SELECT u.*, b.BranchName FROM Users u "
                + "LEFT JOIN Branches b ON b.BranchID = u.BranchID "
                + "WHERE u.Role = 'MANAGER' ");

        Integer searchId = null;
        String keyword = null;
        if (search != null && !search.trim().isEmpty()) {
            keyword = search.trim();
            String cleanDigits = keyword.replaceAll("(?i)^[#A-Z]+", "");
            if (!cleanDigits.isEmpty() && cleanDigits.matches("^\\d+$")) {
                try {
                    searchId = Integer.parseInt(cleanDigits);
                } catch (NumberFormatException ignored) {}
            }
            if (searchId != null) {
                sql.append("AND (u.UserID = ? OR u.Account LIKE ? OR u.FullName LIKE ? OR u.Email LIKE ? OR u.Phone LIKE ? OR b.BranchName LIKE ?) ");
            } else {
                sql.append("AND (u.Account LIKE ? OR u.FullName LIKE ? OR u.Email LIKE ? OR u.Phone LIKE ? OR b.BranchName LIKE ?) ");
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
            LOGGER.log(Level.SEVERE, "Could not load managers with search", e);
        }
        return list;
    }

    public List<User> getAllStaffAndManagers(Integer branchId) {
        String sql = "SELECT u.*, b.BranchName FROM Users u "
                + "LEFT JOIN Branches b ON b.BranchID = u.BranchID "
                + "WHERE u.Role IN ('STAFF', 'MANAGER') "
                + (branchId != null ? "AND u.BranchID = ? " : "")
                + "ORDER BY u.Role DESC, b.BranchName, u.FullName";
        List<User> list = new ArrayList<>();
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            if (branchId != null) {
                ps.setInt(1, branchId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not load staff and managers", e);
        }
        return list;
    }

    public boolean transferStaff(int userId, Integer fromBranchId, int toBranchId, int adminId, String reason, String notes) {
        String updateSql = "UPDATE Users SET BranchID = ?, UpdatedAt = GETDATE() WHERE UserID = ? AND Role = 'STAFF'";
        String insertSql = "INSERT INTO StaffTransferHistory (UserID, FromBranchID, ToBranchID, TransferredBy, TransferDate, Reason, Notes) "
                + "VALUES (?, ?, ?, ?, GETDATE(), ?, ?)";

        Connection connection = null;
        try {
            connection = DBContext.getConnection();
            connection.setAutoCommit(false);

            try (PreparedStatement updatePs = connection.prepareStatement(updateSql)) {
                updatePs.setInt(1, toBranchId);
                updatePs.setInt(2, userId);
                updatePs.executeUpdate();
            }

            try (PreparedStatement insertPs = connection.prepareStatement(insertSql)) {
                insertPs.setInt(1, userId);
                if (fromBranchId != null) {
                    insertPs.setInt(2, fromBranchId);
                } else {
                    insertPs.setNull(2, java.sql.Types.INTEGER);
                }
                insertPs.setInt(3, toBranchId);
                insertPs.setInt(4, adminId);
                insertPs.setString(5, reason);
                insertPs.setString(6, notes);
                insertPs.executeUpdate();
            }

            connection.commit();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Staff transfer transaction failed", e);
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

    public List<StaffTransfer> getTransferHistory(Integer branchId, Integer userId) {
        StringBuilder sql = new StringBuilder(
                "SELECT t.TransferID, t.UserID, t.FromBranchID, t.ToBranchID, t.TransferredBy, "
                + "t.TransferDate, t.Reason, t.Notes, "
                + "u.FullName AS UserName, u.Account AS UserAccount, u.Email AS UserEmail, u.Role AS UserRole, "
                + "fb.BranchName AS FromBranchName, tb.BranchName AS ToBranchName, "
                + "adm.FullName AS TransferredByName "
                + "FROM StaffTransferHistory t "
                + "JOIN Users u ON u.UserID = t.UserID "
                + "LEFT JOIN Branches fb ON fb.BranchID = t.FromBranchID "
                + "JOIN Branches tb ON tb.BranchID = t.ToBranchID "
                + "JOIN Users adm ON adm.UserID = t.TransferredBy "
                + "WHERE 1=1 "
        );

        if (branchId != null) {
            sql.append("AND (t.FromBranchID = ? OR t.ToBranchID = ?) ");
        }
        if (userId != null) {
            sql.append("AND t.UserID = ? ");
        }
        sql.append("ORDER BY t.TransferDate DESC");

        List<StaffTransfer> list = new ArrayList<>();
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (branchId != null) {
                ps.setInt(paramIndex++, branchId);
                ps.setInt(paramIndex++, branchId);
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

                    int fromB = rs.getInt("FromBranchID");
                    item.setFromBranchId(rs.wasNull() ? null : fromB);
                    item.setFromBranchName(rs.getString("FromBranchName"));

                    item.setToBranchId(rs.getInt("ToBranchID"));
                    item.setToBranchName(rs.getString("ToBranchName"));

                    item.setTransferredBy(rs.getInt("TransferredBy"));
                    item.setTransferredByName(rs.getString("TransferredByName"));

                    item.setTransferDate(rs.getTimestamp("TransferDate"));
                    item.setReason(rs.getString("Reason"));
                    item.setNotes(rs.getString("Notes"));

                    list.add(item);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not load staff transfer history", e);
        }
        return list;
    }

    public boolean updateUserStatus(int userId, String status) {
        String sql = "UPDATE Users SET Status = ?, UpdatedAt = GETDATE() WHERE UserID = ?";
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not update user status", e);
            return false;
        }
    }

    public boolean isEmailExistsForOtherUser(String email, int userId) {
        String sql = "SELECT 1 FROM Users WHERE Email = ? AND UserID <> ?";
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Check email exists for other user failed", e);
            return false;
        }
    }

    public boolean updateUserBasicInfo(int userId, String fullName, String phone, String email) {
        String sql = "UPDATE Users SET FullName = ?, Phone = ?, Email = ?, UpdatedAt = GETDATE() WHERE UserID = ?";
        try (Connection connection = DBContext.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, phone);
            ps.setString(3, email);
            ps.setInt(4, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not update user basic info", e);
            return false;
        }
    }

    public boolean assignManagerToBranchWithHistory(int branchId, int managerId, int adminId, String notes) {
        String endCurrentBranchAssignment = "UPDATE BranchManagerAssignments SET EndDate = GETDATE(), Status = 'REPLACED' WHERE BranchID = ? AND EndDate IS NULL";
        String endCurrentManagerAssignment = "UPDATE BranchManagerAssignments SET EndDate = GETDATE(), Status = 'REPLACED' WHERE ManagerID = ? AND EndDate IS NULL";
        String insertNewAssignment = "INSERT INTO BranchManagerAssignments (BranchID, ManagerID, AssignedBy, StartDate, EndDate, Status, Notes, CreatedAt) "
                + "VALUES (?, ?, ?, GETDATE(), NULL, 'ACTIVE', ?, GETDATE())";
        String clearOldManagerBranch = "UPDATE Users SET BranchID = NULL, UpdatedAt = GETDATE() WHERE BranchID = ? AND Role = 'MANAGER' AND UserID <> ?";
        String updateNewManagerBranch = "UPDATE Users SET BranchID = ?, UpdatedAt = GETDATE() WHERE UserID = ? AND Role = 'MANAGER'";

        Connection connection = null;
        try {
            connection = DBContext.getConnection();
            connection.setAutoCommit(false);

            // 1. Kết thúc nhiệm kỳ Manager cũ tại rạp này (nếu có)
            try (PreparedStatement ps1 = connection.prepareStatement(endCurrentBranchAssignment)) {
                ps1.setInt(1, branchId);
                ps1.executeUpdate();
            }

            // 2. Kết thúc nhiệm kỳ hiện tại của Manager này ở rạp khác (nếu có)
            try (PreparedStatement ps2 = connection.prepareStatement(endCurrentManagerAssignment)) {
                ps2.setInt(1, managerId);
                ps2.executeUpdate();
            }

            // 3. Tạo bản ghi bổ nhiệm mới
            try (PreparedStatement ps3 = connection.prepareStatement(insertNewAssignment)) {
                ps3.setInt(1, branchId);
                ps3.setInt(2, managerId);
                ps3.setInt(3, adminId);
                ps3.setString(4, notes != null ? notes : "Bổ nhiệm quản lý chi nhánh");
                ps3.executeUpdate();
            }

            // 4. Đồng bộ Users.BranchID: Xóa branchId ở các manager khác của rạp này
            try (PreparedStatement ps4 = connection.prepareStatement(clearOldManagerBranch)) {
                ps4.setInt(1, branchId);
                ps4.setInt(2, managerId);
                ps4.executeUpdate();
            }

            // 5. Cập nhật branchId mới cho Manager
            try (PreparedStatement ps5 = connection.prepareStatement(updateNewManagerBranch)) {
                ps5.setInt(1, branchId);
                ps5.setInt(2, managerId);
                ps5.executeUpdate();
            }

            connection.commit();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Assign manager transaction failed", e);
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

    public boolean unassignManager(int branchId, int managerId, int adminId, String reason) {
        String endAssignment = "UPDATE BranchManagerAssignments SET EndDate = GETDATE(), Status = 'ENDED', Notes = ? WHERE BranchID = ? AND ManagerID = ? AND EndDate IS NULL";
        String clearBranch = "UPDATE Users SET BranchID = NULL, UpdatedAt = GETDATE() WHERE UserID = ? AND Role = 'MANAGER'";

        Connection connection = null;
        try {
            connection = DBContext.getConnection();
            connection.setAutoCommit(false);

            try (PreparedStatement ps1 = connection.prepareStatement(endAssignment)) {
                ps1.setString(1, reason != null ? reason : "Miễn nhiệm quản lý chi nhánh");
                ps1.setInt(2, branchId);
                ps1.setInt(3, managerId);
                ps1.executeUpdate();
            }

            try (PreparedStatement ps2 = connection.prepareStatement(clearBranch)) {
                ps2.setInt(1, managerId);
                ps2.executeUpdate();
            }

            connection.commit();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Unassign manager failed", e);
            if (connection != null) {
                try { connection.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            return false;
        } finally {
            if (connection != null) {
                try { connection.setAutoCommit(true); connection.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
    }

    public List<ManagerAssignment> getManagerAssignmentHistory(Integer branchId, Integer managerId) {
        StringBuilder sql = new StringBuilder(
                "SELECT bma.AssignmentID, bma.BranchID, bma.ManagerID, bma.AssignedBy, "
                + "bma.StartDate, bma.EndDate, bma.Status, bma.Notes, bma.CreatedAt, "
                + "b.BranchName, b.City AS BranchCity, "
                + "u.FullName AS ManagerName, u.Account AS ManagerAccount, u.Email AS ManagerEmail, "
                + "adm.FullName AS AssignedByName "
                + "FROM BranchManagerAssignments bma "
                + "JOIN Branches b ON b.BranchID = bma.BranchID "
                + "JOIN Users u ON u.UserID = bma.ManagerID "
                + "JOIN Users adm ON adm.UserID = bma.AssignedBy "
                + "WHERE 1=1 "
        );

        if (branchId != null) {
            sql.append("AND bma.BranchID = ? ");
        }
        if (managerId != null) {
            sql.append("AND bma.ManagerID = ? ");
        }
        sql.append("ORDER BY bma.StartDate DESC, bma.AssignmentID DESC");

        List<ManagerAssignment> list = new ArrayList<>();
        try (Connection connection = DBContext.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (branchId != null) {
                ps.setInt(paramIndex++, branchId);
            }
            if (managerId != null) {
                ps.setInt(paramIndex++, managerId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ManagerAssignment item = new ManagerAssignment();
                    item.setAssignmentId(rs.getInt("AssignmentID"));
                    item.setBranchId(rs.getInt("BranchID"));
                    item.setBranchName(rs.getString("BranchName"));
                    item.setBranchCity(rs.getString("BranchCity"));
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
            LOGGER.log(Level.SEVERE, "Could not load manager assignments", e);
        }
        return list;
    }
}
