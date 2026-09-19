package dao;

import model.User;
import utils.DBContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

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

        String sql
                = "INSERT INTO Users "
                + "(FullName, Account, Email, Password, Role, Status, LoginProvider) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DBContext.getConnection(); PreparedStatement ps
                = connection.prepareStatement(sql)) {

            ps.setString(1, user.getFullName());
            ps.setString(2, user.getAccount());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPassword());

            ps.setString(5, "CUSTOMER");
            ps.setString(6, "ACTIVE");
            ps.setString(7, "LOCAL");

            int rows = ps.executeUpdate();

            System.out.println();
            System.out.println("----------------------------------------");
            System.out.println("USERDAO REGISTER");
            System.out.println("Rows inserted = " + rows);
            System.out.println("----------------------------------------");

            return rows > 0;

        } catch (SQLException e) {

            System.out.println();
            System.out.println("----------------------------------------");
            System.out.println("USERDAO REGISTER SQL ERROR");
            System.out.println("----------------------------------------");

            LOGGER.log(Level.SEVERE, "Registration INSERT failed", e);
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

        user.setCreatedAt(rs.getTimestamp("CreatedAt"));
        user.setUpdatedAt(rs.getTimestamp("UpdatedAt"));

        user.setGoogleId(rs.getString("GoogleID"));
        user.setLoginProvider(rs.getString("LoginProvider"));

        user.setEmailVerified(rs.getBoolean("EmailVerified"));
        user.setPhoneVerified(rs.getBoolean("PhoneVerified"));

        user.setLastLoginAt(rs.getTimestamp("LastLoginAt"));

        user.setResetToken(rs.getString("ResetToken"));
        user.setResetTokenExpiry(rs.getTimestamp("ResetTokenExpiry"));

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
}
