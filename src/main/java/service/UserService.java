package service;

import dao.CinemaDAO;
import dao.UserDAO;
import model.entity.Centre;
import model.entity.User;
import utils.PasswordUtil;
import utils.ValidationUtil;

import java.util.List;

/**
 * Service: Xử lý logic nghiệp vụ Người dùng, Xác thực & Tài khoản (User & Authentication).
 * Nhiệm vụ:
 * - Xác thực đăng nhập (Authentication), đối soát mật khẩu bằng chuẩn băm BCrypt.
 * - Kiểm tra các ràng buộc trạng thái tài khoản (ACTIVE, LOCKED, INACTIVE, kích hoạt email).
 * - Quản lý đổi mật khẩu, quên mật khẩu và băm mật khẩu bảo mật.
 * - Quản lý tài khoản Quản lý chi nhánh (Manager).
 */
public class UserService {

    private final UserDAO userDAO;
    private final CinemaDAO cinemaDAO;

    public UserService() {
        this(new UserDAO(), new CinemaDAO());
    }

    public UserService(UserDAO userDAO) {
        this(userDAO, new CinemaDAO());
    }

    public UserService(UserDAO userDAO, CinemaDAO cinemaDAO) {
        this.userDAO = userDAO;
        this.cinemaDAO = cinemaDAO;
    }

    /**
     * Kết quả xác thực đăng nhập.
     */
    public enum AuthStatus {
        SUCCESS,
        INVALID_CREDENTIALS,
        ACCOUNT_LOCKED,
        ACCOUNT_INACTIVE,
        EMAIL_UNVERIFIED,
        MANAGER_UNASSIGNED
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
            passwordCorrect = PasswordUtil.checkPassword(rawPassword, storedPassword);
        } else {
            // Hỗ trợ mật khẩu cũ và tự động nâng cấp sang chuẩn băm BCrypt
            passwordCorrect = rawPassword.equals(storedPassword);
            if (passwordCorrect) {
                userDAO.updatePassword(user.getUserId(), PasswordUtil.hashPassword(rawPassword));
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

        if ("MANAGER".equalsIgnoreCase(user.getRole())) {
            Integer managerCentreId = user.getCentreId();
            if (managerCentreId == null || managerCentreId <= 0
                    || !userDAO.hasActiveManagerAssignment(user.getUserId(), managerCentreId)) {
                return new AuthResult(AuthStatus.MANAGER_UNASSIGNED, user);
            }
        }

        if (isCustomer && !user.isEmailVerified()) {
            return new AuthResult(AuthStatus.EMAIL_UNVERIFIED, user);
        }

        if (!isCustomer && !user.isEmailVerified()) {
            userDAO.activateUser(user.getUserId());
            user.setEmailVerified(true);
        }

        return new AuthResult(AuthStatus.SUCCESS, user);
    }

    /**
     * Đối chiếu mật khẩu người dùng nhập với mật khẩu lưu trong CSDL (hỗ trợ cả BCrypt lẫn dữ liệu cũ).
     *
     * @param rawPassword    mật khẩu thô người dùng nhập
     * @param storedPassword mật khẩu đang lưu trong CSDL
     * @return true nếu mật khẩu khớp; ngược lại false
     */
    private boolean isPasswordMatch(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        return storedPassword.startsWith("$2")
                ? PasswordUtil.checkPassword(rawPassword, storedPassword)
                : rawPassword.equals(storedPassword);
    }

    /**
     * Kết quả kiểm tra và đổi mật khẩu.
     */
    public enum ChangePasswordResult {
        SUCCESS,
        USER_NOT_FOUND,
        WRONG_CURRENT_PASSWORD,
        SAME_AS_CURRENT,
        FAILED
    }

    /**
     * Đổi mật khẩu người dùng có kiểm tra mật khẩu hiện tại và đảm bảo mật khẩu mới không trùng mật khẩu cũ.
     *
     * @param userId          mã người dùng
     * @param currentPassword mật khẩu hiện tại
     * @param newPassword     mật khẩu mới
     * @return kết quả thao tác thuộc {@link ChangePasswordResult}
     */
    public ChangePasswordResult changePasswordWithValidation(int userId, String currentPassword, String newPassword) {
        User user = userDAO.getUserById(userId);
        if (user == null) {
            return ChangePasswordResult.USER_NOT_FOUND;
        }

        String storedPassword = user.getPassword();
        if (!isPasswordMatch(currentPassword, storedPassword)) {
            return ChangePasswordResult.WRONG_CURRENT_PASSWORD;
        }
        if (isPasswordMatch(newPassword, storedPassword)) {
            return ChangePasswordResult.SAME_AS_CURRENT;
        }

        boolean ok = userDAO.changePassword(userId, PasswordUtil.hashPassword(newPassword));
        return ok ? ChangePasswordResult.SUCCESS : ChangePasswordResult.FAILED;
    }

    /**
     * Lấy thông tin người dùng theo ID.
     */
    public User getUserById(int userId) {
        return userDAO.getUserById(userId);
    }

    // ==============================================================
    // NGHIỆP VỤ QUẢN LÝ TÀI KHOẢN MANAGER
    // ==============================================================

    /**
     * Lấy danh sách tài khoản Quản lý (Manager).
     *
     * @return danh sách các đối tượng {@link User} có vai trò MANAGER
     */
    public List<User> getManagers() {
        return userDAO.getManagers();
    }

    /**
     * Lấy danh sách các cụm rạp hiện chưa có Quản lý hoạt động (phục vụ form tạo mới Manager).
     *
     * @return danh sách cụm rạp còn trống vị trí Quản lý
     */
    public List<Centre> getCentresWithoutActiveManager() {
        return cinemaDAO.getCentresWithoutActiveManager();
    }

    /**
     * Đồng bộ hóa trạng thái phân công cụm rạp cho các tài khoản Manager đang bị khóa hoặc tạm ngưng.
     */
    public void syncLockedManagersAssignments() {
        userDAO.syncLockedManagersAssignments();
    }

    /**
     * Tạo mới tài khoản Quản lý chi nhánh và phân công cụm rạp phụ trách ban đầu.
     *
     * @return null nếu thành công; ngược lại chuỗi mã lỗi
     */
    public String createManager(int sessionUserId, String fullName, String account,
                                String email, String password, String phone, String centreIdParam) {
        if (ValidationUtil.isBlank(fullName)
                || ValidationUtil.isBlank(account)
                || ValidationUtil.isBlank(email)
                || ValidationUtil.isBlank(password)
                || ValidationUtil.isBlank(centreIdParam)) {
            return "missing_fields";
        }

        int centreId;
        try {
            centreId = Integer.parseInt(centreIdParam.trim());
        } catch (NumberFormatException e) {
            return "invalid_centre";
        }

        if (cinemaDAO.hasActiveManager(centreId)) {
            return "centre_has_manager";
        }

        fullName = fullName.trim();
        account = account.trim();
        email = email.trim();
        phone = phone != null ? phone.trim() : "";

        if (!ValidationUtil.isValidFullName(fullName)) return "invalid_name";
        if (!ValidationUtil.isValidAccount(account)) return "invalid_account";
        if (!ValidationUtil.isValidEmail(email)) return "invalid_email";
        if (ValidationUtil.isDisposableEmail(email)) return "disposable_email";
        if (!ValidationUtil.isValidPassword(password)) return "weak_password";
        if (!ValidationUtil.isValidPhone(phone, false)) return "invalid_phone";
        if (userDAO.isAccountExists(account)) return "account_exists";
        if (userDAO.isEmailExists(email)) return "email_exists";

        User newManager = new User();
        newManager.setFullName(fullName);
        newManager.setAccount(account);
        newManager.setEmail(email);
        newManager.setPassword(PasswordUtil.hashPassword(password.trim()));
        newManager.setPhone(phone);
        newManager.setCentreId(centreId);

        int createdId = userDAO.createManager(newManager, sessionUserId);
        return createdId > 0 ? null : "create_failed";
    }

    /**
     * Cập nhật thông tin Quản lý (họ tên, số điện thoại, email, phân công rạp).
     *
     * @param adminId        mã Admin thực hiện
     * @param userId         mã Quản lý cần cập nhật
     * @param fullName       họ và tên mới
     * @param phone          số điện thoại
     * @param email          email liên hệ
     * @param centreIdParam  mã cụm rạp phân công mới (hoặc rỗng/null nếu gỡ rạp)
     * @return null nếu thành công; ngược lại chuỗi mã lỗi
     */
    public String updateManager(int adminId, int userId, String fullName, String phone, String email, String centreIdParam) {
        if (ValidationUtil.isBlank(fullName) || ValidationUtil.isBlank(email)) {
            return "missing_fields";
        }

        fullName = fullName.trim();
        email = email.trim();
        phone = phone != null ? phone.trim() : "";

        if (!ValidationUtil.isValidFullName(fullName)) return "invalid_name";
        if (!ValidationUtil.isValidEmail(email)) return "invalid_email";
        if (ValidationUtil.isDisposableEmail(email)) return "disposable_email";
        if (!ValidationUtil.isValidPhone(phone, false)) return "invalid_phone";

        User target = userDAO.getUserById(userId);
        if (target == null || !"MANAGER".equalsIgnoreCase(target.getRole())) {
            return "not_manager";
        }

        if (userDAO.isEmailExistsForOtherUser(email, userId)) {
            return "email_exists";
        }

        Integer newCentreId = null;
        if (centreIdParam != null && !centreIdParam.isBlank()) {
            try {
                newCentreId = Integer.parseInt(centreIdParam.trim());
            } catch (NumberFormatException e) {
                return "invalid_centre";
            }
        }

        // Kiểm tra quy tắc nghiệp vụ phân công cụm rạp:
        // - Mỗi cụm rạp tại một thời điểm chỉ có tối đa 1 Quản lý hoạt động (Quy tắc GBR-09).
        // - Nếu chọn rạp mới (khác rạp hiện tại): rạp đó bắt buộc phải đang trống (chưa có Quản lý).
        // - Nếu giữ nguyên rạp hiện tại hoặc chọn bỏ trống (gỡ rạp về trạng thái tự do): cho phép thực hiện.
        if (newCentreId != null) {
            boolean isSameCentre = target.getCentreId() != null && target.getCentreId().intValue() == newCentreId.intValue();
            if (!isSameCentre) {
                // Kiểm tra xem rạp mới đã có Quản lý nào đang hoạt động phụ trách hay chưa
                if (cinemaDAO.hasActiveManager(newCentreId)) {
                    return "centre_has_manager";
                }
            }
        }

        boolean updated = userDAO.updateManagerWithCentreTx(userId, fullName, phone, email, newCentreId, adminId);
        return updated ? null : "update_failed";
    }

    /**
     * Cập nhật thông tin cơ bản của tài khoản Quản lý (họ tên, số điện thoại, email).
     *
     * @return null nếu thành công; ngược lại chuỗi mã lỗi
     */
    public String updateManager(int userId, String fullName, String phone, String email) {
        return updateManager(0, userId, fullName, phone, email, null);
    }

    /**
     * Chuyển đổi trạng thái tài khoản Quản lý (ACTIVE / INACTIVE / LOCKED).
     *
     * @param userId         mã tài khoản Manager
     * @param status         trạng thái mới cần áp dụng
     * @param sessionAdminId mã Admin thực hiện thao tác
     * @return true nếu thành công; ngược lại false
     */
    public boolean toggleManagerStatus(int userId, String status, int sessionAdminId) {
        if (!ValidationUtil.isValidUserStatus(status)) {
            status = "ACTIVE";
        }
        User target = userDAO.getUserById(userId);
        if (target == null || !"MANAGER".equalsIgnoreCase(target.getRole())) {
            return false;
        }
        return userDAO.toggleManagerStatusTx(userId, status.toUpperCase(), sessionAdminId);
    }

    // ==============================================================
    // NGHIỆP VỤ QUẢN LÝ KHÁCH HÀNG (CUSTOMER)
    // ==============================================================

    /**
     * Lấy toàn bộ danh sách khách hàng phục vụ trang quản trị.
     */
    public List<User> getCustomers() {
        return userDAO.getCustomers();
    }

    /**
     * Chỉ số KPI tổng quan về tài khoản khách hàng.
     *
     * @param total  tổng số khách hàng
     * @param active số khách hàng đang hoạt động
     * @param locked số khách hàng đang bị tạm khóa
     */
    public record CustomerKpi(int total, int active, int locked) {}

    /**
     * Thống kê các chỉ số KPI của danh mục khách hàng trực tiếp trên danh sách khách hàng trong bộ nhớ.
     *
     * @param customers danh sách khách hàng
     * @return bản ghi {@link CustomerKpi}
     */
    public CustomerKpi getCustomerKpi(List<User> customers) {
        if (customers == null) return new CustomerKpi(0, 0, 0);
        int active = 0;
        int locked = 0;
        for (User u : customers) {
            if ("ACTIVE".equalsIgnoreCase(u.getStatus())) {
                active++;
            } else if ("LOCKED".equalsIgnoreCase(u.getStatus())) {
                locked++;
            }
        }
        return new CustomerKpi(customers.size(), active, locked);
    }

    /**
     * Khóa hoặc mở khóa tài khoản khách hàng (ACTIVE hoặc LOCKED).
     *
     * @param userId    mã tài khoản khách hàng
     * @param newStatus trạng thái mới (ACTIVE hoặc LOCKED)
     * @return true nếu cập nhật thành công, ngược lại false
     */
    public boolean updateCustomerStatus(int userId, String newStatus) {
        if (userId <= 0 || newStatus == null) return false;
        String status = newStatus.trim().toUpperCase();
        if (!"ACTIVE".equals(status) && !"LOCKED".equals(status)) {
            return false;
        }
        return userDAO.updateUserStatus(userId, status);
    }
}
