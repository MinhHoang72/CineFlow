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
     * Đổi mật khẩu người dùng (có kiểm tra mật khẩu cũ và băm BCrypt an toàn).
     *
     * @param userId      mã người dùng
     * @param oldPassword mật khẩu cũ
     * @param newPassword mật khẩu mới
     * @return true nếu đổi mật khẩu thành công; ngược lại false
     */
    public boolean changePassword(int userId, String oldPassword, String newPassword) {
        User user = userDAO.getUserById(userId);
        if (user == null || !isPasswordMatch(oldPassword, user.getPassword())) {
            return false;
        }
        return userDAO.changePassword(userId, PasswordUtil.hashPassword(newPassword));
    }

    /**
     * Đặt lại mật khẩu (quên mật khẩu).
     */
    public boolean resetPassword(int userId, String newPassword) {
        String hashedPassword = PasswordUtil.hashPassword(newPassword);
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

    // ==============================================================
    // NGHIỆP VỤ QUẢN LÝ TÀI KHOẢN MANAGER
    // ==============================================================

    /**
     * Lấy danh sách tài khoản Quản lý (Manager), có hỗ trợ lọc theo từ khóa tìm kiếm.
     *
     * @param searchParam từ khóa tìm kiếm (có thể null hoặc rỗng để lấy tất cả)
     * @return danh sách các đối tượng {@link User} có vai trò MANAGER
     */
    public List<User> getManagers(String searchParam) {
        return userDAO.getManagers(searchParam);
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
     * Mã kết quả xử lý nghiệp vụ tạo mới / cập nhật Quản lý.
     */
    public enum ManagerOpStatus {
        SUCCESS,
        MISSING_FIELDS,
        INVALID_CENTRE,
        CENTRE_HAS_MANAGER,
        INVALID_NAME,
        INVALID_ACCOUNT,
        INVALID_EMAIL,
        DISPOSABLE_EMAIL,
        WEAK_PASSWORD,
        INVALID_PHONE,
        ACCOUNT_EXISTS,
        EMAIL_EXISTS,
        NOT_MANAGER,
        FAILED
    }

    /**
     * Kết quả trả về khi tạo hoặc cập nhật tài khoản Manager.
     *
     * @param status  trạng thái kết quả nghiệp vụ
     * @param message thông điệp mô tả chi tiết
     * @param user    đối tượng Manager sau khi thao tác thành công (null nếu lỗi)
     */
    public record ManagerOpResult(ManagerOpStatus status, String message, User user) {
        public boolean isSuccess() {
            return status == ManagerOpStatus.SUCCESS;
        }
    }

    /**
     * Tạo mới tài khoản Quản lý chi nhánh và phân công cụm rạp phụ trách ban đầu.
     * Kiểm tra toàn bộ định dạng dữ liệu đầu vào, chống trùng tài khoản/email và đảm bảo mỗi rạp chỉ có 1 Quản lý.
     *
     * @param sessionUserId mã Admin đang thực hiện tạo tài khoản
     * @param fullName      họ và tên Quản lý
     * @param account       tên đăng nhập (Account)
     * @param email         địa chỉ email liên hệ
     * @param password      mật khẩu thô (sẽ được băm BCrypt trước khi lưu)
     * @param phone         số điện thoại di động
     * @param centreIdParam mã cụm rạp phụ trách (chuỗi từ form)
     * @return {@link ManagerOpResult} chứa trạng thái và thông tin Manager vừa tạo
     */
    public ManagerOpResult createManager(int sessionUserId, String fullName, String account,
                                         String email, String password, String phone, String centreIdParam) {
        if (ValidationUtil.isBlank(fullName)
                || ValidationUtil.isBlank(account)
                || ValidationUtil.isBlank(email)
                || ValidationUtil.isBlank(password)
                || ValidationUtil.isBlank(centreIdParam)) {
            return new ManagerOpResult(ManagerOpStatus.MISSING_FIELDS, "Vui lòng nhập đầy đủ các trường thông tin.", null);
        }

        int centreId;
        try {
            centreId = Integer.parseInt(centreIdParam.trim());
        } catch (NumberFormatException e) {
            return new ManagerOpResult(ManagerOpStatus.INVALID_CENTRE, "Cụm rạp không hợp lệ.", null);
        }

        if (cinemaDAO.hasActiveManager(centreId)) {
            return new ManagerOpResult(ManagerOpStatus.CENTRE_HAS_MANAGER, "Cụm rạp này đã có Quản lý hoạt động.", null);
        }

        fullName = fullName.trim();
        account = account.trim();
        email = email.trim();
        phone = phone != null ? phone.trim() : "";

        if (!ValidationUtil.isValidFullName(fullName)) {
            return new ManagerOpResult(ManagerOpStatus.INVALID_NAME, "Họ tên không hợp lệ.", null);
        }

        if (!ValidationUtil.isValidAccount(account)) {
            return new ManagerOpResult(ManagerOpStatus.INVALID_ACCOUNT, "Tên tài khoản không hợp lệ.", null);
        }

        if (!ValidationUtil.isValidEmail(email)) {
            return new ManagerOpResult(ManagerOpStatus.INVALID_EMAIL, "Email không hợp lệ.", null);
        }

        if (ValidationUtil.isDisposableEmail(email)) {
            return new ManagerOpResult(ManagerOpStatus.DISPOSABLE_EMAIL, "Không sử dụng email tạm thời.", null);
        }

        if (!ValidationUtil.isValidPassword(password)) {
            return new ManagerOpResult(ManagerOpStatus.WEAK_PASSWORD, "Mật khẩu chưa đủ độ mạnh.", null);
        }

        if (!ValidationUtil.isValidPhone(phone, false)) {
            return new ManagerOpResult(ManagerOpStatus.INVALID_PHONE, "Số điện thoại không hợp lệ.", null);
        }

        if (userDAO.isAccountExists(account)) {
            return new ManagerOpResult(ManagerOpStatus.ACCOUNT_EXISTS, "Tên tài khoản đã tồn tại.", null);
        }

        if (userDAO.isEmailExists(email)) {
            return new ManagerOpResult(ManagerOpStatus.EMAIL_EXISTS, "Email đã tồn tại.", null);
        }

        User newManager = new User();
        newManager.setFullName(fullName);
        newManager.setAccount(account);
        newManager.setEmail(email);
        newManager.setPassword(PasswordUtil.hashPassword(password.trim()));
        newManager.setPhone(phone);
        newManager.setCentreId(centreId);

        int createdId = userDAO.createManager(newManager, sessionUserId);
        if (createdId > 0) {
            newManager.setUserId(createdId);
            return new ManagerOpResult(ManagerOpStatus.SUCCESS, "Tạo tài khoản quản lý thành công.", newManager);
        } else {
            return new ManagerOpResult(ManagerOpStatus.FAILED, "Không thể tạo tài khoản quản lý.", null);
        }
    }

    /**
     * Cập nhật thông tin cơ bản của tài khoản Quản lý (họ tên, số điện thoại, email).
     *
     * @param userId   mã định danh của Manager
     * @param fullName họ và tên mới
     * @param phone    số điện thoại mới
     * @param email    địa chỉ email mới (kiểm tra trùng lặp với tài khoản khác)
     * @return {@link ManagerOpResult} chứa kết quả cập nhật
     */
    public ManagerOpResult updateManager(int userId, String fullName, String phone, String email) {
        if (ValidationUtil.isBlank(fullName) || ValidationUtil.isBlank(email)) {
            return new ManagerOpResult(ManagerOpStatus.MISSING_FIELDS, "Họ tên và email không được để trống.", null);
        }

        fullName = fullName.trim();
        email = email.trim();
        phone = phone != null ? phone.trim() : "";

        if (!ValidationUtil.isValidFullName(fullName)) {
            return new ManagerOpResult(ManagerOpStatus.INVALID_NAME, "Họ tên không hợp lệ.", null);
        }

        if (!ValidationUtil.isValidEmail(email)) {
            return new ManagerOpResult(ManagerOpStatus.INVALID_EMAIL, "Email không hợp lệ.", null);
        }

        if (ValidationUtil.isDisposableEmail(email)) {
            return new ManagerOpResult(ManagerOpStatus.DISPOSABLE_EMAIL, "Không dùng email tạm thời.", null);
        }

        if (!ValidationUtil.isValidPhone(phone, false)) {
            return new ManagerOpResult(ManagerOpStatus.INVALID_PHONE, "Số điện thoại không hợp lệ.", null);
        }

        User target = userDAO.getUserById(userId);
        if (target == null || !"MANAGER".equalsIgnoreCase(target.getRole())) {
            return new ManagerOpResult(ManagerOpStatus.NOT_MANAGER, "Không tìm thấy tài khoản quản lý.", null);
        }

        if (userDAO.isEmailExistsForOtherUser(email, userId)) {
            return new ManagerOpResult(ManagerOpStatus.EMAIL_EXISTS, "Email đã được sử dụng.", null);
        }

        boolean updated = userDAO.updateUserBasicInfo(userId, fullName, phone, email);
        if (updated) {
            target.setFullName(fullName);
            target.setPhone(phone);
            target.setEmail(email);
            return new ManagerOpResult(ManagerOpStatus.SUCCESS, "Cập nhật thành công.", target);
        } else {
            return new ManagerOpResult(ManagerOpStatus.FAILED, "Cập nhật thất bại.", null);
        }
    }

    /**
     * Mã kết quả chuyển đổi trạng thái hoạt động của tài khoản Manager.
     */
    public enum ManagerStatusResult {
        SUCCESS,
        NOT_FOUND,
        NOT_MANAGER,
        FAILED
    }

    /**
     * Chuyển đổi trạng thái tài khoản Quản lý (ACTIVE / INACTIVE / LOCKED) trong khối Transaction an toàn.
     *
     * @param userId         mã tài khoản Manager
     * @param status         trạng thái mới cần áp dụng
     * @param sessionAdminId mã Admin thực hiện thao tác
     * @return kết quả thuộc {@link ManagerStatusResult}
     */
    public ManagerStatusResult toggleManagerStatus(int userId, String status, int sessionAdminId) {
        if (!ValidationUtil.isValidUserStatus(status)) {
            status = "ACTIVE";
        }
        User target = userDAO.getUserById(userId);
        if (target == null) {
            return ManagerStatusResult.NOT_FOUND;
        }
        if (!"MANAGER".equalsIgnoreCase(target.getRole())) {
            return ManagerStatusResult.NOT_MANAGER;
        }
        String newStatus = status.toUpperCase();
        boolean ok = userDAO.toggleManagerStatusTx(userId, newStatus, sessionAdminId);
        return ok ? ManagerStatusResult.SUCCESS : ManagerStatusResult.FAILED;
    }

    // ==============================================================
    // NGHIỆP VỤ QUẢN LÝ KHÁCH HÀNG (CUSTOMER)
    // ==============================================================

    /**
     * Lấy danh sách tài khoản khách hàng theo từ khóa tìm kiếm và trạng thái.
     *
     * @param search từ khóa tìm kiếm (họ tên, tài khoản, email, SĐT hoặc ID)
     * @param status bộ lọc trạng thái (ALL, ACTIVE, LOCKED)
     * @return danh sách khách hàng kèm số đơn đặt vé và tổng chi tiêu
     */
    public List<User> getCustomers(String search, String status) {
        return userDAO.getCustomers(search, status);
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
     * Thống kê các chỉ số KPI của danh mục khách hàng.
     *
     * @return bản ghi {@link CustomerKpi}
     */
    public CustomerKpi getCustomerKpi() {
        int total = userDAO.countCustomersByStatus("ALL");
        int active = userDAO.countCustomersByStatus("ACTIVE");
        int locked = userDAO.countCustomersByStatus("LOCKED");
        return new CustomerKpi(total, active, locked);
    }

    /**
     * Mã kết quả cập nhật trạng thái tài khoản khách hàng.
     */
    public enum CustomerStatusResult {
        SUCCESS,
        INVALID_USER,
        INVALID_STATUS,
        FAILED
    }

    /**
     * Khóa hoặc mở khóa tài khoản khách hàng (chỉ chấp nhận hai trạng thái ACTIVE hoặc LOCKED).
     *
     * @param userId    mã tài khoản khách hàng
     * @param newStatus trạng thái mới (ACTIVE hoặc LOCKED)
     * @return kết quả thuộc {@link CustomerStatusResult}
     */
    public CustomerStatusResult updateCustomerStatus(int userId, String newStatus) {
        User targetCustomer = userDAO.getUserById(userId);
        if (targetCustomer == null || !"CUSTOMER".equalsIgnoreCase(targetCustomer.getRole())) {
            return CustomerStatusResult.INVALID_USER;
        }

        String normalizedStatus = newStatus != null ? newStatus.trim().toUpperCase() : "";
        if (!"ACTIVE".equals(normalizedStatus) && !"LOCKED".equals(normalizedStatus)) {
            return CustomerStatusResult.INVALID_STATUS;
        }

        boolean updated = userDAO.updateUserStatus(userId, normalizedStatus);
        return updated ? CustomerStatusResult.SUCCESS : CustomerStatusResult.FAILED;
    }
}
