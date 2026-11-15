package service;

import dao.CinemaDAO;
import dao.UserDAO;
import model.entity.Centre;
import model.entity.User;
import utils.PasswordUtil;
import utils.ValidationUtil;

import java.util.ArrayList;
import java.util.List;

public class StaffService {

    private final UserDAO userDAO;
    private final CinemaDAO cinemaDAO;

    public StaffService() {
        this(new UserDAO(), new CinemaDAO());
    }

    public StaffService(UserDAO userDAO, CinemaDAO cinemaDAO) {
        this.userDAO = userDAO;
        this.cinemaDAO = cinemaDAO;
    }

    public enum Status {
        SUCCESS,
        MISSING_FIELDS,
        INVALID_NAME,
        INVALID_ACCOUNT,
        INVALID_EMAIL,
        WEAK_PASSWORD,
        INVALID_PHONE,
        ACCOUNT_EXISTS,
        EMAIL_EXISTS,
        INVALID_CENTRE,
        UNASSIGNED_MANAGER,
        NOT_FOUND,
        FORBIDDEN,
        INVALID_STATUS,
        FAILED
    }

    public static class OperationResult {

        private final Status status;
        private final String message;
        private final User user;

        public OperationResult(Status status, String message) {
            this(status, message, null);
        }

        public OperationResult(Status status, String message, User user) {
            this.status = status;
            this.message = message;
            this.user = user;
        }

        public boolean isSuccess() {
            return status == Status.SUCCESS;
        }

        public Status getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public User getUser() {
            return user;
        }
    }

    // Kiểm tra người thao tác có phải Manager được bổ nhiệm hay không.
    private boolean isValidManager(User manager) {
        return manager != null
                && "MANAGER".equalsIgnoreCase(manager.getRole())
                && isManagerAssigned(manager);
    }

    // Kiểm tra Manager có trung tâm đang được phân công.
    public boolean isManagerAssigned(User manager) {
        if (manager == null || manager.getCentreId() == null
                || manager.getCentreId() <= 0) {
            return false;
        }

        return userDAO.hasActiveManagerAssignment(
                manager.getUserId(),
                manager.getCentreId()
        );
    }

    // Lấy danh sách nhân viên thuộc trung tâm của Manager.
    public List<User> getStaffDirectory(
            User manager, String searchParam, String roleFilter) {

        if (!isValidManager(manager)) {
            return new ArrayList<>();
        }

        return userDAO.getStaffDirectory(
                manager.getCentreId(),
                searchParam,
                roleFilter
        );
    }

    // Lấy thông tin trung tâm Manager đang phụ trách.
    public Centre getManagerCentre(User manager) {
        if (!isValidManager(manager)) {
            return null;
        }

        return cinemaDAO.getCentreById(manager.getCentreId());
    }

    // Tạo nhân viên mới.
    public OperationResult createStaff(
            User actor,
            String fullName,
            String account,
            String email,
            String password,
            String phone,
            String role) {

        // 1. Kiểm tra quyền Manager và trung tâm được phân công.
        if (!isValidManager(actor)) {
            return new OperationResult(
                    Status.UNASSIGNED_MANAGER,
                    "Manager chưa được phân công trung tâm hợp lệ."
            );
        }

        // 2. Kiểm tra dữ liệu bắt buộc.
        if (ValidationUtil.isBlank(fullName)
                || ValidationUtil.isBlank(account)
                || ValidationUtil.isBlank(email)
                || ValidationUtil.isBlank(password)) {

            return new OperationResult(
                    Status.MISSING_FIELDS,
                    "Vui lòng điền đầy đủ thông tin bắt buộc."
            );
        }

        fullName = fullName.trim();
        account = account.trim();
        email = email.trim();
        password = password.trim();
        phone = phone == null ? "" : phone.trim();

        // 3. Kiểm tra họ tên.
        if (!ValidationUtil.isValidFullName(fullName)) {
            return new OperationResult(
                    Status.INVALID_NAME,
                    "Họ và tên không hợp lệ."
            );
        }

        // 4. Kiểm tra tài khoản.
        if (account.length() > 50) {
            return new OperationResult(
                    Status.INVALID_ACCOUNT,
                    "Tên tài khoản không được vượt quá 50 ký tự."
            );
        }

        // 5. Kiểm tra email.
        if (!email.contains("@") || email.length() > 100) {
            return new OperationResult(
                    Status.INVALID_EMAIL,
                    "Email không hợp lệ."
            );
        }

// 6. Kiểm tra mật khẩu.
        if (password.length() < 8) {
            return new OperationResult(
                    Status.WEAK_PASSWORD,
                    "Mật khẩu phải có ít nhất 8 ký tự."
            );
        }

        if (!password.matches(".*[A-Z].*")) {
            return new OperationResult(
                    Status.WEAK_PASSWORD,
                    "Mật khẩu phải có ít nhất 1 chữ cái viết hoa."
            );
        }

        if (!password.matches(".*[^a-zA-Z0-9].*")) {
            return new OperationResult(
                    Status.WEAK_PASSWORD,
                    "Mật khẩu phải có ít nhất 1 ký tự đặc biệt."
            );
        }

        // 7. Kiểm tra số điện thoại.
        if (phone.length() > 20) {
            return new OperationResult(
                    Status.INVALID_PHONE,
                    "Số điện thoại không được vượt quá 20 ký tự."
            );
        }

        // 8. Kiểm tra trùng tài khoản và email.
        if (userDAO.isAccountExists(account)) {
            return new OperationResult(
                    Status.ACCOUNT_EXISTS,
                    "Tên tài khoản đã tồn tại."
            );
        }

        if (userDAO.isEmailExists(email)) {
            return new OperationResult(
                    Status.EMAIL_EXISTS,
                    "Email đã được sử dụng."
            );
        }

        // 9. Chỉ cho phép hai vai trò nhân viên.
        if (role == null || !ValidationUtil.isStaffRole(role.trim())) {
            return new OperationResult(
                    Status.FORBIDDEN,
                    "Vị trí nhân viên không hợp lệ."
            );
        }

        role = role.trim().toUpperCase();

        // 10. Tạo tài khoản.
        User newStaff = new User();
        newStaff.setFullName(fullName);
        newStaff.setAccount(account);
        newStaff.setEmail(email);
        newStaff.setPassword(PasswordUtil.hashPassword(password));
        newStaff.setPhone(phone);
        newStaff.setRole(role);

        // Quan trọng: lấy trung tâm từ Manager, không lấy từ request.
        newStaff.setCentreId(actor.getCentreId());

        boolean created = userDAO.createStaff(newStaff);

        if (created) {
            return new OperationResult(
                    Status.SUCCESS,
                    "Tạo nhân viên thành công.",
                    newStaff
            );
        }

        return new OperationResult(
                Status.FAILED,
                "Không thể tạo tài khoản nhân viên."
        );
    }

    // Cập nhật thông tin nhân viên.
    public OperationResult updateStaff(
            User actor,
            int staffId,
            String fullName,
            String phone,
            String email,
            String role) {

        // 1. Kiểm tra Manager.
        if (!isValidManager(actor)) {
            return new OperationResult(
                    Status.FORBIDDEN,
                    "Bạn không có quyền quản lý nhân viên."
            );
        }

        // 2. Kiểm tra dữ liệu.
        if (ValidationUtil.isBlank(fullName)
                || ValidationUtil.isBlank(email)) {

            return new OperationResult(
                    Status.MISSING_FIELDS,
                    "Họ tên và email không được để trống."
            );
        }

        fullName = fullName.trim();
        email = email.trim();
        phone = phone == null ? "" : phone.trim();

        if (!ValidationUtil.isValidFullName(fullName)) {
            return new OperationResult(
                    Status.INVALID_NAME,
                    "Họ và tên không hợp lệ."
            );
        }

        if (!email.contains("@") || email.length() > 100) {
            return new OperationResult(
                    Status.INVALID_EMAIL,
                    "Email không hợp lệ."
            );
        }

        if (phone.length() > 20) {
            return new OperationResult(
                    Status.INVALID_PHONE,
                    "Số điện thoại không được vượt quá 20 ký tự."
            );
        }

        // 3. Lấy nhân viên cần sửa.
        User targetUser = userDAO.getUserById(staffId);

        if (targetUser == null) {
            return new OperationResult(
                    Status.NOT_FOUND,
                    "Không tìm thấy nhân viên."
            );
        }

        // 4. Chỉ được sửa nhân viên thuộc trung tâm của Manager.
        if (!targetUser.isStaff()
                || targetUser.getCentreId() == null
                || !actor.getCentreId().equals(targetUser.getCentreId())) {

            return new OperationResult(
                    Status.FORBIDDEN,
                    "Bạn không có quyền sửa nhân viên này."
            );
        }

        // 5. Kiểm tra email trùng.
        if (userDAO.isEmailExistsForOtherUser(email, staffId)) {
            return new OperationResult(
                    Status.EMAIL_EXISTS,
                    "Email đã được tài khoản khác sử dụng."
            );
        }

        // 6. Kiểm tra vai trò.
        if (role == null || !ValidationUtil.isStaffRole(role.trim())) {
            return new OperationResult(
                    Status.FORBIDDEN,
                    "Vị trí nhân viên không hợp lệ."
            );
        }

        role = role.trim().toUpperCase();

        // 7. Cập nhật.
        boolean updated = userDAO.updateStaffBasicInfo(
                staffId, fullName, phone, email, role
        );

        if (!updated) {
            return new OperationResult(
                    Status.FAILED,
                    "Cập nhật thông tin thất bại."
            );
        }

        targetUser.setFullName(fullName);
        targetUser.setPhone(phone);
        targetUser.setEmail(email);
        targetUser.setRole(role);

        return new OperationResult(
                Status.SUCCESS,
                "Cập nhật nhân viên thành công.",
                targetUser
        );
    }

    // Cập nhật trạng thái nhân viên.
    public OperationResult updateStaffStatus(
            User actor, int staffId, String status) {

        // 1. Kiểm tra Manager.
        if (!isValidManager(actor)) {
            return new OperationResult(
                    Status.FORBIDDEN,
                    "Bạn không có quyền quản lý nhân viên."
            );
        }

        // 2. Kiểm tra trạng thái.
        if (status == null) {
            return new OperationResult(
                    Status.INVALID_STATUS,
                    "Trạng thái không hợp lệ."
            );
        }

        status = status.trim().toUpperCase();

        if (!"ACTIVE".equals(status)
                && !"INACTIVE".equals(status)
                && !"LOCKED".equals(status)) {

            return new OperationResult(
                    Status.INVALID_STATUS,
                    "Trạng thái không hợp lệ."
            );
        }

        // 3. Lấy nhân viên.
        User targetUser = userDAO.getUserById(staffId);

        if (targetUser == null) {
            return new OperationResult(
                    Status.NOT_FOUND,
                    "Không tìm thấy nhân viên."
            );
        }

        // 4. Chỉ được thay đổi nhân viên thuộc trung tâm của Manager.
        if (!targetUser.isStaff()
                || targetUser.getCentreId() == null
                || !actor.getCentreId().equals(targetUser.getCentreId())) {

            return new OperationResult(
                    Status.FORBIDDEN,
                    "Bạn không có quyền thay đổi nhân viên này."
            );
        }

        // 5. Cập nhật trạng thái.
        boolean updated = userDAO.updateUserStatus(staffId, status);

        if (!updated) {
            return new OperationResult(
                    Status.FAILED,
                    "Không thể cập nhật trạng thái."
            );
        }

        targetUser.setStatus(status);

        return new OperationResult(
                Status.SUCCESS,
                "Cập nhật trạng thái thành công.",
                targetUser
        );
    }
}
