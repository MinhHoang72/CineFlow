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
    public String createStaff(
            User actor,
            String fullName,
            String account,
            String email,
            String password,
            String phone,
            String role) {

        if (!isValidManager(actor)) {
            return "unassigned_manager";
        }

        if (ValidationUtil.isBlank(fullName)
                || ValidationUtil.isBlank(account)
                || ValidationUtil.isBlank(email)
                || ValidationUtil.isBlank(password)) {
            return "missing_fields";
        }

        fullName = fullName.trim();
        account = account.trim();
        email = email.trim();
        password = password.trim();
        phone = phone == null ? "" : phone.trim();

        if (!ValidationUtil.isValidFullName(fullName)) {
            return "invalid_name";
        }

        if (account.length() > 50) {
            return "invalid_account";
        }

        if (!email.contains("@") || email.length() > 100) {
            return "invalid_email";
        }

        if (password.length() < 8 || !password.matches(".*[A-Z].*") || !password.matches(".*[^a-zA-Z0-9].*")) {
            return "weak_password";
        }

        if (phone.length() > 20) {
            return "invalid_phone";
        }

        if (userDAO.isAccountExists(account)) {
            return "account_exists";
        }

        if (userDAO.isEmailExists(email)) {
            return "email_exists";
        }

        if (role == null || !ValidationUtil.isStaffRole(role.trim())) {
            role = User.ROLE_TICKET_SALES;
        } else {
            role = role.trim().toUpperCase();
        }

        User newStaff = new User();
        newStaff.setFullName(fullName);
        newStaff.setAccount(account);
        newStaff.setEmail(email);
        newStaff.setPassword(PasswordUtil.hashPassword(password));
        newStaff.setPhone(phone);
        newStaff.setRole(role);
        newStaff.setCentreId(actor.getCentreId());

        boolean created = userDAO.createStaff(newStaff);
        return created ? null : "create_failed";
    }

    // Cập nhật thông tin nhân viên.
    public String updateStaff(
            User actor,
            int staffId,
            String fullName,
            String phone,
            String email,
            String role) {

        if (!isValidManager(actor)) {
            return "forbidden";
        }

        if (ValidationUtil.isBlank(fullName) || ValidationUtil.isBlank(email)) {
            return "missing_fields";
        }

        fullName = fullName.trim();
        email = email.trim();
        phone = phone == null ? "" : phone.trim();

        if (!ValidationUtil.isValidFullName(fullName)) {
            return "invalid_name";
        }

        if (!email.contains("@") || email.length() > 100) {
            return "invalid_email";
        }

        if (phone.length() > 20) {
            return "invalid_phone";
        }

        User targetUser = userDAO.getUserById(staffId);
        if (targetUser == null) {
            return "not_found";
        }

        if (!targetUser.isStaff()
                || targetUser.getCentreId() == null
                || !actor.getCentreId().equals(targetUser.getCentreId())) {
            return "forbidden";
        }

        if (userDAO.isEmailExistsForOtherUser(email, staffId)) {
            return "email_exists";
        }

        if (role == null || !ValidationUtil.isStaffRole(role.trim())) {
            role = User.ROLE_TICKET_SALES;
        } else {
            role = role.trim().toUpperCase();
        }

        boolean updated = userDAO.updateStaffBasicInfo(
                staffId, fullName, phone, email, role
        );

        return updated ? null : "update_failed";
    }

    // Cập nhật trạng thái nhân viên.
    public boolean updateStaffStatus(User actor, int staffId, String status) {
        if (!isValidManager(actor) || status == null) {
            return false;
        }

        status = status.trim().toUpperCase();
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status) && !"LOCKED".equals(status)) {
            return false;
        }

        User targetUser = userDAO.getUserById(staffId);
        if (targetUser == null) {
            return false;
        }

        if (!targetUser.isStaff()
                || targetUser.getCentreId() == null
                || !actor.getCentreId().equals(targetUser.getCentreId())) {
            return false;
        }

        return userDAO.updateUserStatus(staffId, status);
    }
}
