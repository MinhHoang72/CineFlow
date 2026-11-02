package service;

import dao.CinemaDAO;
import dao.UserDAO;
import model.entity.Centre;
import model.entity.User;
import utils.PasswordUtil;
import utils.ValidationUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Service: Xử lý toàn bộ nghiệp vụ quản lý nhân sự cấp cơ sở (STAFF).
 * Phân quyền:
 * - Admin: Quản lý nhân viên toàn hệ thống hoặc theo từng chi nhánh rạp.
 * - Manager: Quản lý nhân viên trực thuộc chi nhánh mình phụ trách.
 */
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

    /**
     * Lấy danh bạ nhân sự theo quyền hạn và bộ lọc.
     */
    public List<User> getStaffDirectory(boolean isAdmin, Integer centreId, String searchParam, String roleFilter) {
        if (!isAdmin && centreId == null) {
            return new ArrayList<>();
        }
        return userDAO.getStaffDirectory(centreId, searchParam, roleFilter);
    }

    /**
     * Lấy danh sách cụm rạp đang hoạt động.
     */
    public List<Centre> getActiveCentres() {
        return cinemaDAO.getActiveCentres();
    }

    /**
     * Lấy thông tin cụm rạp theo ID.
     */
    public Centre getCentreById(int centreId) {
        if (centreId <= 0) {
            return null;
        }
        return cinemaDAO.getCentreById(centreId);
    }

    /**
     * Kiểm tra Manager có chi nhánh hợp lệ và đang hoạt động không.
     */
    public boolean isManagerAssigned(User manager) {
        if (manager == null || manager.getCentreId() == null || manager.getCentreId() <= 0) {
            return false;
        }
        return userDAO.hasActiveManagerAssignment(manager.getUserId(), manager.getCentreId());
    }

    /**
     * Tạo mới tài khoản nhân viên.
     */
    public OperationResult createStaff(User actor, boolean isAdmin, String centreIdParam,
                                       String fullName, String account, String email,
                                       String password, String phone, String role) {
        // 1. Xác định rạp trực thuộc
        Integer targetCentreId = null;
        if (!isAdmin) {
            if (actor == null || actor.getCentreId() == null || actor.getCentreId() <= 0) {
                return new OperationResult(Status.UNASSIGNED_MANAGER, "Tài khoản Manager chưa được phân công chi nhánh.");
            }
            targetCentreId = actor.getCentreId();
        } else {
            if (ValidationUtil.isBlank(centreIdParam)) {
                return new OperationResult(Status.INVALID_CENTRE, "Vui lòng chọn cụm rạp cho nhân viên.");
            }
            try {
                targetCentreId = Integer.parseInt(centreIdParam.trim());
            } catch (NumberFormatException e) {
                return new OperationResult(Status.INVALID_CENTRE, "Cụm rạp không hợp lệ.");
            }
            Centre targetCentre = cinemaDAO.getCentreById(targetCentreId);
            if (targetCentre == null) {
                return new OperationResult(Status.INVALID_CENTRE, "Cụm rạp không tồn tại.");
            }
        }

        // 2. Kiểm tra các trường bắt buộc
        if (ValidationUtil.isBlank(fullName) || ValidationUtil.isBlank(account)
                || ValidationUtil.isBlank(email) || ValidationUtil.isBlank(password)) {
            return new OperationResult(Status.MISSING_FIELDS, "Vui lòng điền đầy đủ các thông tin bắt buộc.");
        }

        fullName = fullName.trim();
        account = account.trim();
        email = email.trim();
        phone = phone != null ? phone.trim() : "";

        // 3. Validate định dạng Họ tên
        if (!ValidationUtil.isValidFullName(fullName)) {
            return new OperationResult(Status.INVALID_NAME, "Họ và tên không hợp lệ.");
        }

        // 4. Validate định dạng Tên tài khoản (cho phép tự do theo ý người dùng, độ dài <= 50)
        if (account.length() > 50) {
            return new OperationResult(Status.INVALID_ACCOUNT, "Tên tài khoản không được vượt quá 50 ký tự.");
        }

        // 5. Validate định dạng Email
        if (!email.contains("@") || email.length() > 100) {
            return new OperationResult(Status.INVALID_EMAIL, "Email không hợp lệ.");
        }

        // 6. Kiểm tra mật khẩu (>= 6 ký tự)
        if (password.trim().length() < 6) {
            return new OperationResult(Status.WEAK_PASSWORD, "Mật khẩu phải có ít nhất 6 ký tự.");
        }

        // 7. Validate Số điện thoại
        if (phone.length() > 20) {
            return new OperationResult(Status.INVALID_PHONE, "Số điện thoại không được vượt quá 20 ký tự.");
        }

        // 8. Kiểm tra trùng lặp tài khoản và email
        if (userDAO.isAccountExists(account)) {
            return new OperationResult(Status.ACCOUNT_EXISTS, "Tên tài khoản đã tồn tại trong hệ thống.");
        }
        if (userDAO.isEmailExists(email)) {
            return new OperationResult(Status.EMAIL_EXISTS, "Email đã được sử dụng.");
        }

        // 9. Xác định vai trò
        if (role == null || role.isBlank() || !ValidationUtil.isStaffRole(role)) {
            role = User.ROLE_TICKET_SALES;
        } else {
            role = role.trim().toUpperCase();
        }

        // 10. Tạo thực thể và mã hóa mật khẩu bằng BCrypt
        User newStaff = new User();
        newStaff.setFullName(fullName);
        newStaff.setAccount(account);
        newStaff.setEmail(email);
        newStaff.setPassword(PasswordUtil.hashPassword(password.trim()));
        newStaff.setPhone(phone);
        newStaff.setRole(role);
        newStaff.setCentreId(targetCentreId);

        boolean created = userDAO.createStaff(newStaff);
        if (created) {
            return new OperationResult(Status.SUCCESS, "Tạo nhân viên thành công.", newStaff);
        } else {
            return new OperationResult(Status.FAILED, "Không thể tạo tài khoản nhân viên.");
        }
    }

    /**
     * Cập nhật thông tin cơ bản của nhân viên.
     */
    public OperationResult updateStaff(User actor, boolean isAdmin, int staffId,
                                       String fullName, String phone, String email, String role) {
        // 1. Kiểm tra các trường bắt buộc
        if (ValidationUtil.isBlank(fullName) || ValidationUtil.isBlank(email)) {
            return new OperationResult(Status.MISSING_FIELDS, "Họ tên và email không được để trống.");
        }

        fullName = fullName.trim();
        email = email.trim();
        phone = phone != null ? phone.trim() : "";

        // 2. Validate định dạng
        if (!ValidationUtil.isValidFullName(fullName)) {
            return new OperationResult(Status.INVALID_NAME, "Họ và tên không hợp lệ.");
        }
        if (!email.contains("@") || email.length() > 100) {
            return new OperationResult(Status.INVALID_EMAIL, "Email không hợp lệ.");
        }
        if (phone.length() > 20) {
            return new OperationResult(Status.INVALID_PHONE, "Số điện thoại không được vượt quá 20 ký tự.");
        }

        // 3. Kiểm tra tài khoản mục tiêu
        User targetUser = userDAO.getUserById(staffId);
        if (targetUser == null) {
            return new OperationResult(Status.NOT_FOUND, "Không tìm thấy tài khoản nhân viên.");
        }

        // 4. Chỉ cho phép sửa nhân viên cấp rạp
        if (!targetUser.isStaff()) {
            return new OperationResult(Status.FORBIDDEN, "Chỉ được chỉnh sửa nhân viên cấp rạp.");
        }

        // 5. Kiểm tra quyền của Manager (chỉ sửa rạp mình)
        if (!isAdmin) {
            if (actor == null || actor.getCentreId() == null || targetUser.getCentreId() == null
                    || !actor.getCentreId().equals(targetUser.getCentreId())) {
                return new OperationResult(Status.FORBIDDEN, "Bạn không có quyền quản lý nhân viên của rạp khác.");
            }
        }

        // 6. Kiểm tra trùng lặp email với user khác
        if (userDAO.isEmailExistsForOtherUser(email, staffId)) {
            return new OperationResult(Status.EMAIL_EXISTS, "Email đã được tài khoản khác sử dụng.");
        }

        // 7. Xác định vai trò
        if (role == null || role.isBlank() || !ValidationUtil.isStaffRole(role)) {
            role = targetUser.getRole();
        } else {
            role = role.trim().toUpperCase();
        }

        // 8. Cập nhật DB
        boolean updated = userDAO.updateStaffBasicInfo(staffId, fullName, phone, email, role);
        if (updated) {
            targetUser.setFullName(fullName);
            targetUser.setPhone(phone);
            targetUser.setEmail(email);
            targetUser.setRole(role);
            return new OperationResult(Status.SUCCESS, "Cập nhật nhân viên thành công.", targetUser);
        } else {
            return new OperationResult(Status.FAILED, "Cập nhật thông tin thất bại.");
        }
    }

    /**
     * Cập nhật trạng thái hoạt động của nhân viên (ACTIVE, INACTIVE, LOCKED).
     */
    public OperationResult updateStaffStatus(User actor, boolean isAdmin, int staffId, String status) {
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status) && !"LOCKED".equals(status)) {
            return new OperationResult(Status.INVALID_STATUS, "Trạng thái không hợp lệ.");
        }

        User targetUser = userDAO.getUserById(staffId);
        if (targetUser == null) {
            return new OperationResult(Status.NOT_FOUND, "Không tìm thấy tài khoản nhân viên.");
        }

        if (!targetUser.isStaff()) {
            return new OperationResult(Status.FORBIDDEN, "Chỉ được cập nhật trạng thái nhân viên cấp rạp.");
        }

        if (!isAdmin) {
            if (actor == null || actor.getCentreId() == null || targetUser.getCentreId() == null
                    || !actor.getCentreId().equals(targetUser.getCentreId())) {
                return new OperationResult(Status.FORBIDDEN, "Bạn không có quyền quản lý nhân viên của rạp khác.");
            }
        }

        boolean updated = userDAO.updateUserStatus(staffId, status);
        if (updated) {
            targetUser.setStatus(status);
            return new OperationResult(Status.SUCCESS, "Cập nhật trạng thái thành công.", targetUser);
        } else {
            return new OperationResult(Status.FAILED, "Không thể cập nhật trạng thái.");
        }
    }
}
