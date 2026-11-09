package service;

import dao.CinemaDAO;
import dao.UserDAO;
import model.entity.Centre;
import model.entity.ManagerAssignment;
import model.entity.StaffTransfer;
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

    public enum ChangePasswordResult {
        SUCCESS,
        USER_NOT_FOUND,
        WRONG_CURRENT_PASSWORD,
        SAME_AS_CURRENT,
        FAILED
    }

    public ChangePasswordResult changePasswordWithValidation(int userId, String currentPassword, String newPassword) {
        User user = userDAO.getUserById(userId);
        if (user == null) {
            return ChangePasswordResult.USER_NOT_FOUND;
        }

        String storedPassword = user.getPassword();
        boolean validOld;
        if (storedPassword != null && storedPassword.startsWith("$2")) {
            validOld = PasswordUtil.checkPassword(currentPassword, storedPassword);
        } else {
            validOld = currentPassword.equals(storedPassword);
        }

        if (!validOld) {
            return ChangePasswordResult.WRONG_CURRENT_PASSWORD;
        }

        if (storedPassword != null && (storedPassword.startsWith("$2") ? PasswordUtil.checkPassword(newPassword, storedPassword) : newPassword.equals(storedPassword))) {
            return ChangePasswordResult.SAME_AS_CURRENT;
        }

        String hashedNew = PasswordUtil.hashPassword(newPassword);
        boolean ok = userDAO.changePassword(userId, hashedNew);
        return ok ? ChangePasswordResult.SUCCESS : ChangePasswordResult.FAILED;
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
            validOld = PasswordUtil.checkPassword(oldPassword, storedPassword);
        } else {
            validOld = oldPassword.equals(storedPassword);
        }

        if (!validOld) {
            return false;
        }

        String hashedNew = PasswordUtil.hashPassword(newPassword);
        return userDAO.changePassword(userId, hashedNew);
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

    public List<User> getManagers(String searchParam) {
        return userDAO.getManagers(searchParam);
    }

    public List<Centre> getCentresWithoutActiveManager() {
        return cinemaDAO.getCentresWithoutActiveManager();
    }

    public void syncLockedManagersAssignments() {
        userDAO.syncLockedManagersAssignments();
    }

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

    public record ManagerOpResult(ManagerOpStatus status, String message, User user) {
        public boolean isSuccess() {
            return status == ManagerOpStatus.SUCCESS;
        }
    }

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

    public enum ManagerStatusResult {
        SUCCESS,
        NOT_FOUND,
        NOT_MANAGER,
        FAILED
    }

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

    public List<User> getCustomers(String search, String status) {
        return userDAO.getCustomers(search, status);
    }

    public record CustomerKpi(int total, int active, int locked) {}

    public CustomerKpi getCustomerKpi() {
        int total = userDAO.countCustomersByStatus("ALL");
        int active = userDAO.countCustomersByStatus("ACTIVE");
        int locked = userDAO.countCustomersByStatus("LOCKED");
        return new CustomerKpi(total, active, locked);
    }

    public enum CustomerStatusResult {
        SUCCESS,
        INVALID_USER,
        INVALID_STATUS,
        FAILED
    }

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

    // ==============================================================
    // NGHIỆP VỤ BỔ NHIỆM, ĐIỀU CHUYỂN & LỊCH SỬ NHÂN SỰ
    // ==============================================================

    public record TransferKpi(int totalCentres, int assignedCentres, int unassignedCentres, int unassignedManagers) {}

    public record TransferPageData(
            List<Centre> centresWithManager,
            List<Centre> activeCentres,
            List<User> managers,
            List<ManagerAssignment> managerAssignments,
            List<User> staffList,
            List<StaffTransfer> transferHistory,
            TransferKpi kpi
    ) {}

    public TransferPageData getTransferPageData(Integer centreFilter, String searchParam, String roleFilter) {
        syncLockedManagersAssignments();
        List<Centre> centresWithManager = cinemaDAO.getAllCentresWithManager();
        List<Centre> activeCentres = cinemaDAO.getActiveCentres();
        List<User> managers = userDAO.getManagers();
        List<ManagerAssignment> managerAssignments = userDAO.getManagerAssignmentHistory(centreFilter, null);
        List<User> staffList = userDAO.getTransferableUsers(centreFilter, searchParam, roleFilter);
        List<StaffTransfer> transferHistory = userDAO.getTransferHistory(centreFilter, null);

        int totalCentres = centresWithManager != null ? centresWithManager.size() : 0;
        int assignedCentres = 0;
        int unassignedCentres = 0;
        if (centresWithManager != null) {
            for (Centre c : centresWithManager) {
                if (c.hasActiveManager()) {
                    assignedCentres++;
                } else if ("ACTIVE".equalsIgnoreCase(c.getStatus())) {
                    unassignedCentres++;
                }
            }
        }

        int unassignedManagers = 0;
        if (managers != null) {
            for (User m : managers) {
                if ("ACTIVE".equalsIgnoreCase(m.getStatus()) && (m.getCentreId() == null || m.getCentreId() <= 0)) {
                    unassignedManagers++;
                }
            }
        }
        TransferKpi kpi = new TransferKpi(totalCentres, assignedCentres, unassignedCentres, unassignedManagers);
        return new TransferPageData(centresWithManager, activeCentres, managers, managerAssignments, staffList, transferHistory, kpi);
    }

    public String assignManager(int centreId, int managerId, int adminId, String notes) {
        return userDAO.assignManager(centreId, managerId, adminId, notes);
    }

    public boolean unassignManager(int centreId, int managerId, int adminId, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            return false;
        }
        return userDAO.unassignManager(centreId, managerId, adminId, reason.trim());
    }

    public String transferStaff(int userId, int toCentreId, int adminId, String reason, String notes, String effectiveDateParam, String newRoleParam) {
        User targetUser = userDAO.getUserById(userId);
        if (targetUser == null) {
            return "USER_NOT_FOUND";
        }
        if ("MANAGER".equalsIgnoreCase(targetUser.getRole()) || targetUser.isManager()) {
            return "MANAGER_TRANSFER_NOT_ALLOWED";
        }

        String finalRole = (newRoleParam != null && !newRoleParam.trim().isEmpty() && !"KEEP".equalsIgnoreCase(newRoleParam.trim()))
                ? newRoleParam.trim().toUpperCase()
                : targetUser.getRole();

        if ("MANAGER".equalsIgnoreCase(finalRole)) {
            return "MANAGER_TRANSFER_NOT_ALLOWED";
        }

        java.sql.Date effectiveDate = null;
        if (effectiveDateParam != null && !effectiveDateParam.trim().isEmpty()) {
            try {
                effectiveDate = java.sql.Date.valueOf(effectiveDateParam.trim());
            } catch (Exception ignored) {
            }
        }
        if (effectiveDate == null) {
            effectiveDate = new java.sql.Date(System.currentTimeMillis());
        }

        String transferReason = reason != null ? reason.trim() : "";
        String transferNotes = notes != null ? notes.trim() : "";
        return userDAO.transferRegularStaff(userId, toCentreId, adminId, transferReason, transferNotes, effectiveDate, finalRole);
    }

    public record HistoryKpi(int totalAssignments, int activeAssignments, int totalTransfers) {}

    public record HistoryPageData(
            List<Centre> activeCentres,
            List<ManagerAssignment> managerAssignments,
            List<StaffTransfer> transferHistory,
            HistoryKpi kpi
    ) {}

    public HistoryPageData getHistoryPageData(Integer centreFilter) {
        syncLockedManagersAssignments();
        List<Centre> activeCentres = cinemaDAO.getActiveCentres();
        List<ManagerAssignment> managerAssignments = userDAO.getManagerAssignmentHistory(centreFilter, null);
        List<StaffTransfer> transferHistory = userDAO.getTransferHistory(centreFilter, null);

        int totalAssignments = managerAssignments != null ? managerAssignments.size() : 0;
        int activeAssignments = 0;
        if (managerAssignments != null) {
            for (ManagerAssignment ma : managerAssignments) {
                if ("ACTIVE".equalsIgnoreCase(ma.getStatus()) && ma.getEndDate() == null) {
                    activeAssignments++;
                }
            }
        }
        int totalTransfers = transferHistory != null ? transferHistory.size() : 0;
        HistoryKpi kpi = new HistoryKpi(totalAssignments, activeAssignments, totalTransfers);

        return new HistoryPageData(activeCentres, managerAssignments, transferHistory, kpi);
    }

}
