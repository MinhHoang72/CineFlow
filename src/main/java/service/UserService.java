package service;

import dao.UserDAO;
import model.entity.User;
import utils.PasswordUtils;

/**
 * Service: Xử lý logic nghiệp vụ Người dùng, Xác thực & Tài khoản (User & Authentication).
 * Nhiệm vụ:
 * - Xác thực đăng nhập (Authentication), đối soát mật khẩu bằng chuẩn băm BCrypt.
 * - Kiểm tra các ràng buộc trạng thái tài khoản (ACTIVE, LOCKED, INACTIVE, kích hoạt email).
 * - Quản lý đổi mật khẩu, quên mật khẩu và băm mật khẩu bảo mật.
 */
public class UserService {

    private final UserDAO userDAO;

    public UserService() {
        this.userDAO = new UserDAO();
    }

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Kết quả xác thực đăng nhập.
     */
    public enum AuthStatus {
        SUCCESS,
        INVALID_CREDENTIALS,
        ACCOUNT_LOCKED,
        ACCOUNT_INACTIVE,
        EMAIL_UNVERIFIED
    }

    public static class AuthResult {
        private final AuthStatus status;
        private final User user;

        public AuthResult(AuthStatus status, User user) {
            this.status = status;
            this.user = user;
        }

        public AuthStatus getStatus() {
            return status;
        }

        public User getUser() {
            return user;
        }

        public boolean isSuccess() {
            return status == AuthStatus.SUCCESS;
        }
    }

    /**
     * Xác thực thông tin đăng nhập của người dùng.
     *
     * @param accountOrEmail Tên tài khoản hoặc Email
     * @param rawPassword    Mật khẩu thô do người dùng nhập
     * @return AuthResult chứa trạng thái xác thực và đối tượng User (nếu thành công)
     */
    public AuthResult authenticate(String accountOrEmail, String rawPassword) {
        if (accountOrEmail == null || rawPassword == null) {
            return new AuthResult(AuthStatus.INVALID_CREDENTIALS, null);
        }

        User user = userDAO.getUserByAccountOrEmail(accountOrEmail.trim());
        if (user == null) {
            return new AuthResult(AuthStatus.INVALID_CREDENTIALS, null);
        }

        String storedPassword = user.getPassword();
        boolean passwordCorrect;

        if (storedPassword != null && storedPassword.startsWith("$2")) {
            passwordCorrect = PasswordUtils.checkPassword(rawPassword, storedPassword);
        } else {
            // Hỗ trợ mật khẩu cũ và tự động nâng cấp sang chuẩn băm BCrypt
            passwordCorrect = rawPassword.equals(storedPassword);
            if (passwordCorrect) {
                userDAO.updatePassword(user.getUserId(), PasswordUtils.hashPassword(rawPassword));
            }
        }

        if (!passwordCorrect) {
            return new AuthResult(AuthStatus.INVALID_CREDENTIALS, null);
        }

        if ("LOCKED".equalsIgnoreCase(user.getStatus())) {
            return new AuthResult(AuthStatus.ACCOUNT_LOCKED, user);
        }

        boolean isCustomer = "CUSTOMER".equalsIgnoreCase(user.getRole());

        if ("INACTIVE".equalsIgnoreCase(user.getStatus())) {
            if (isCustomer && !user.isEmailVerified()) {
                return new AuthResult(AuthStatus.EMAIL_UNVERIFIED, user);
            }
            return new AuthResult(AuthStatus.ACCOUNT_INACTIVE, user);
        }

        if (isCustomer && !user.isEmailVerified()) {
            return new AuthResult(AuthStatus.EMAIL_UNVERIFIED, user);
        }

        return new AuthResult(AuthStatus.SUCCESS, user);
    }

    /**
     * Đổi mật khẩu người dùng (có băm BCrypt an toàn).
     */
    public boolean changePassword(int userId, String oldPassword, String newPassword) {
        User user = userDAO.getUserById(userId);
        if (user == null) {
            return false;
        }

        String storedPassword = user.getPassword();
        boolean validOld;
        if (storedPassword != null && storedPassword.startsWith("$2")) {
            validOld = PasswordUtils.checkPassword(oldPassword, storedPassword);
        } else {
            validOld = oldPassword.equals(storedPassword);
        }

        if (!validOld) {
            return false;
        }

        String hashedNew = PasswordUtils.hashPassword(newPassword);
        return userDAO.changePassword(userId, hashedNew);
    }

    /**
     * Đặt lại mật khẩu (quên mật khẩu).
     */
    public boolean resetPassword(int userId, String newPassword) {
        String hashedPassword = PasswordUtils.hashPassword(newPassword);
        return userDAO.resetPassword(userId, hashedPassword);
    }

    /**
     * Lấy thông tin người dùng theo ID.
     */
    public User getUserById(int userId) {
        return userDAO.getUserById(userId);
    }

    /**
     * Lấy thông tin người dùng theo Account hoặc Email.
     */
    public User getUserByAccountOrEmail(String accountOrEmail) {
        return userDAO.getUserByAccountOrEmail(accountOrEmail);
    }
}
