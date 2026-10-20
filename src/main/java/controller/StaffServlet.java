package controller;
import jakarta.servlet.annotation.WebServlet;

import dao.CinemaDAO;
import dao.UserDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Centre;
import model.User;
import utils.PasswordUtils;
import utils.ValidationUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller điều khiển trang quản lý nhân sự cấp cơ sở (STAFF).
 * Phân quyền:
 * - Quản lý trung tâm (Manager): Quản lý nhân viên thuộc rạp mình phụ trách.
 * - Quản trị viên (Admin): Quản lý nhân viên trên toàn hệ thống hoặc lọc theo từng rạp.
 * URL ánh xạ: /staff
 */
@WebServlet(name = "StaffServlet", urlPatterns = {"/staff", "/manager/staff"})
public class StaffServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final CinemaDAO cinemaDAO = new CinemaDAO();

    /**
     * Kiểm tra người dùng có quyền quản lý nhân sự (MANAGER hoặc ADMIN) hay không.
     *
     * @param session phiên làm việc HttpSession
     * @return true nếu có quyền, ngược lại false
     */
    private boolean canManage(HttpSession session) {
        if (session == null) {
            return false;
        }
        Object role = session.getAttribute("role");
        if (role == null && session.getAttribute("user") instanceof User) {
            role = ((User) session.getAttribute("user")).getRole();
        }
        return "MANAGER".equalsIgnoreCase((String) role) || "ADMIN".equalsIgnoreCase((String) role);
    }

    /**
     * Kiểm tra người dùng có phải Quản trị viên cấp cao (ADMIN) hay không.
     *
     * @param session phiên làm việc HttpSession
     * @return true nếu là Admin, ngược lại false
     */
    private boolean isAdmin(HttpSession session) {
        if (session == null) {
            return false;
        }
        Object role = session.getAttribute("role");
        if (role == null && session.getAttribute("user") instanceof User) {
            role = ((User) session.getAttribute("user")).getRole();
        }
        return "ADMIN".equalsIgnoreCase((String) role);
    }

    /**
     * Kiểm tra yêu cầu gửi đến có phải là AJAX request hay không.
     *
     * @param request yêu cầu HTTP
     * @return true nếu là AJAX request, ngược lại false
     */
    private boolean isAjax(HttpServletRequest request) {
        String xrw = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        String ajaxParam = request.getParameter("ajax");
        return "XMLHttpRequest".equalsIgnoreCase(xrw)
                || (accept != null && accept.contains("application/json"))
                || "1".equals(ajaxParam) || "true".equalsIgnoreCase(ajaxParam);
    }

    /**
     * Xử lý yêu cầu HTTP GET: Tải danh bạ nhân sự theo rạp hoặc theo toàn hệ thống và hiển thị giao diện.
     *
     * @param request  yêu cầu HTTP từ client
     * @param response phản hồi HTTP gửi về client
     * @throws ServletException nếu xảy ra lỗi servlet
     * @throws IOException      nếu xảy ra lỗi vào/ra dữ liệu
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 1. Kiểm tra xác thực (nếu chưa đăng nhập thì chuyển hướng sang trang Login)
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            String loginUrl = request.getContextPath() + "/auth/login.jsp?redirect="
                    + URLEncoder.encode("/staff", StandardCharsets.UTF_8);
            response.sendRedirect(loginUrl);
            return;
        }

        // 2. Kiểm tra quyền hạn (chỉ Admin hoặc Manager mới được truy cập)
        if (!canManage(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập quản lý nhân sự.");
            return;
        }

        // 3. Tải thông tin người dùng đăng nhập hiện tại từ cơ sở dữ liệu để đảm bảo dữ liệu mới nhất
        int sessionUserId = (int) session.getAttribute("userId");
        User currentUser = userDAO.getUserById(sessionUserId);
        if (currentUser == null) {
            currentUser = (User) session.getAttribute("user");
        } else {
            session.setAttribute("user", currentUser);
        }

        boolean adminRole = isAdmin(session);
        Integer centreId = null;

        // 4. Phân loại phạm vi dữ liệu theo vai trò
        if (!adminRole) {
            // Đối với Manager: Chỉ được xem nhân viên thuộc cụm rạp mà mình đang phụ trách
            centreId = currentUser.getCentreId();
        } else {
            // Đối với Admin: Có thể xem toàn bộ hoặc lọc theo một cụm rạp được chọn
            String cp = request.getParameter("centreId");
            if (cp != null && !cp.trim().isEmpty()) {
                try {
                    centreId = Integer.parseInt(cp.trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // 5. Đọc từ khóa tìm kiếm và bộ lọc vai trò (Ticket Sales / Ticket Checker)
        String searchParam = request.getParameter("search");
        String roleFilter = request.getParameter("role");

        // 6. Truy vấn danh sách nhân viên từ UserDAO theo điều kiện rạp và bộ lọc
        List<User> staffList = new ArrayList<>();
        if (adminRole || centreId != null) {
            staffList = userDAO.getStaffDirectory(centreId, searchParam, roleFilter);
        }

        // 7. Lấy danh sách các rạp đang hoạt động để Admin chọn rạp hoặc hiển thị tên rạp cho Manager
        List<Centre> centres = cinemaDAO.getActiveCentres();
        Centre assignedCentre = null;
        if (centreId != null) {
            for (Centre c : centres) {
                if (c.getCentreId() == centreId) {
                    assignedCentre = c;
                    break;
                }
            }
        }

        // 8. Đưa các thuộc tính cần thiết vào request
        request.setAttribute("staff", staffList);
        request.setAttribute("centres", centres);
        request.setAttribute("assignedCentre", assignedCentre);
        request.setAttribute("selectedCentreId", centreId);
        request.setAttribute("selectedRole", roleFilter);
        request.setAttribute("isAdmin", adminRole);

        // 9. Forward tới giao diện danh bạ nhân sự directory.jsp
        request.getRequestDispatcher("/staff/directory.jsp").forward(request, response);
    }

    /**
     * Xử lý yêu cầu HTTP POST: Tiếp nhận thao tác tạo mới nhân viên, cập nhật thông tin hoặc chuyển trạng thái hoạt động.
     *
     * @param request  yêu cầu HTTP từ client
     * @param response phản hồi HTTP gửi về client
     * @throws IOException nếu xảy ra lỗi vào/ra dữ liệu
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 1. Thiết lập encoding UTF-8
        request.setCharacterEncoding("UTF-8");

        // 2. Kiểm tra xác thực phiên và quyền quản lý (Admin hoặc Manager)
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null || !canManage(session)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền cập nhật nhân sự.");
            return;
        }

        int sessionUserId = (int) session.getAttribute("userId");
        User currentUser = userDAO.getUserById(sessionUserId);
        if (currentUser == null) {
            currentUser = (User) session.getAttribute("user");
        }
        boolean adminRole = isAdmin(session);

        String action = request.getParameter("action");

        // 3. Xử lý tạo mới tài khoản nhân viên (action = "create")
        if ("create".equalsIgnoreCase(action)) {
            // 3.1. Xác định rạp trực thuộc cho nhân viên mới
            Integer targetCentreId = null;
            if (!adminRole) {
                // Nếu là Manager: Bắt buộc nhân viên mới phải thuộc rạp của chính Manager đó
                if (currentUser.getCentreId() == null || currentUser.getCentreId() <= 0) {
                    response.sendRedirect(request.getContextPath() + "/staff?error=unassigned_manager");
                    return;
                }
                targetCentreId = currentUser.getCentreId();
            } else {
                // Nếu là Admin: Phải chọn cụm rạp từ danh sách
                String cp = request.getParameter("centreId");
                if (ValidationUtils.isBlank(cp)) {
                    response.sendRedirect(request.getContextPath() + "/staff?error=missing_centre");
                    return;
                }
                try {
                    targetCentreId = Integer.parseInt(cp.trim());
                } catch (NumberFormatException e) {
                    response.sendRedirect(request.getContextPath() + "/staff?error=invalid_centre");
                    return;
                }
                Centre targetCentre = cinemaDAO.getCentreById(targetCentreId);
                if (targetCentre == null) {
                    response.sendRedirect(request.getContextPath() + "/staff?error=invalid_centre");
                    return;
                }
            }

            // 3.2. Đọc các trường thông tin từ form
            String fullName = request.getParameter("fullName");
            String account = request.getParameter("account");
            String email = request.getParameter("email");
            String password = request.getParameter("password");
            String phone = request.getParameter("phone");

            // 3.3. Kiểm tra các trường bắt buộc không để trống
            if (ValidationUtils.isBlank(fullName)
                    || ValidationUtils.isBlank(account)
                    || ValidationUtils.isBlank(email)
                    || ValidationUtils.isBlank(password)) {
                response.sendRedirect(request.getContextPath() + "/staff?error=missing_fields");
                return;
            }

            fullName = fullName.trim();
            account = account.trim();
            email = email.trim();
            phone = phone != null ? phone.trim() : "";

            // 3.4. Validate định dạng Họ tên
            if (!ValidationUtils.isValidFullName(fullName)) {
                response.sendRedirect(request.getContextPath() + "/staff?error=invalid_name");
                return;
            }

            // 3.5. Validate định dạng Tên tài khoản
            if (!ValidationUtils.isValidAccount(account)) {
                response.sendRedirect(request.getContextPath() + "/staff?error=invalid_account");
                return;
            }

            // 3.6. Validate định dạng Email
            if (!ValidationUtils.isValidEmail(email)) {
                response.sendRedirect(request.getContextPath() + "/staff?error=invalid_email");
                return;
            }

            // 3.7. Kiểm tra email rác dùng 1 lần
            if (ValidationUtils.isDisposableEmail(email)) {
                response.sendRedirect(request.getContextPath() + "/staff?error=disposable_email");
                return;
            }

            // 3.8. Kiểm tra mật khẩu đạt chuẩn bảo mật
            if (!ValidationUtils.isValidPassword(password)) {
                response.sendRedirect(request.getContextPath() + "/staff?error=weak_password");
                return;
            }

            // 3.9. Validate định dạng Số điện thoại
            if (!ValidationUtils.isValidPhone(phone, false)) {
                response.sendRedirect(request.getContextPath() + "/staff?error=invalid_phone");
                return;
            }

            // 3.10. Kiểm tra tài khoản đã tồn tại chưa
            if (userDAO.isAccountExists(account)) {
                response.sendRedirect(request.getContextPath() + "/staff?error=account_exists");
                return;
            }

            // 3.11. Kiểm tra email đã được người dùng khác đăng ký chưa
            if (userDAO.isEmailExists(email)) {
                response.sendRedirect(request.getContextPath() + "/staff?error=email_exists");
                return;
            }

            // 3.12. Xác định vai trò nhân sự: Bán vé (TICKET_SALES_STAFF) hoặc Soát vé (TICKET_CHECKER_STAFF)
            String role = request.getParameter("role");
            if (role == null || role.isBlank() || !ValidationUtils.isStaffRole(role)) {
                role = User.ROLE_TICKET_SALES;
            } else {
                role = role.trim().toUpperCase();
            }

            // 3.13. Đóng gói đối tượng User và băm mật khẩu bảo mật
            User newStaff = new User();
            newStaff.setFullName(fullName);
            newStaff.setAccount(account);
            newStaff.setEmail(email);
            newStaff.setPassword(PasswordUtils.hashPassword(password.trim()));
            newStaff.setPhone(phone);
            newStaff.setRole(role);
            newStaff.setCentreId(targetCentreId);

            // 3.14. Ghi nhận thông tin nhân viên vào DB thông qua UserDAO
            boolean created = userDAO.createStaff(newStaff);
            if (created) {
                response.sendRedirect(request.getContextPath() + "/staff?success=created");
            } else {
                response.sendRedirect(request.getContextPath() + "/staff?error=create_failed");
            }
            return;
        }

        // 4. Xử lý cập nhật thông tin nhân viên (action = "update")
        if ("update".equalsIgnoreCase(action)) {
            try {
                // 4.1. Đọc dữ liệu cập nhật từ request
                int userId = Integer.parseInt(request.getParameter("userId"));
                String fullName = request.getParameter("fullName");
                String phone = request.getParameter("phone");
                String email = request.getParameter("email");
                String role = request.getParameter("role");

                // 4.2. Kiểm tra các trường bắt buộc
                if (ValidationUtils.isBlank(fullName) || ValidationUtils.isBlank(email)) {
                    response.sendRedirect(request.getContextPath() + "/staff?error=missing_fields");
                    return;
                }

                fullName = fullName.trim();
                email = email.trim();
                phone = phone != null ? phone.trim() : "";

                // 4.3. Validate định dạng Họ tên, Email, Phone
                if (!ValidationUtils.isValidFullName(fullName)) {
                    response.sendRedirect(request.getContextPath() + "/staff?error=invalid_name");
                    return;
                }

                if (!ValidationUtils.isValidEmail(email)) {
                    response.sendRedirect(request.getContextPath() + "/staff?error=invalid_email");
                    return;
                }

                if (ValidationUtils.isDisposableEmail(email)) {
                    response.sendRedirect(request.getContextPath() + "/staff?error=disposable_email");
                    return;
                }

                if (!ValidationUtils.isValidPhone(phone, false)) {
                    response.sendRedirect(request.getContextPath() + "/staff?error=invalid_phone");
                    return;
                }

                // 4.4. Kiểm tra tài khoản nhân viên cần sửa có tồn tại không
                User targetUser = userDAO.getUserById(userId);
                if (targetUser == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy tài khoản nhân viên.");
                    return;
                }

                // 4.5. Chỉ cho phép chỉnh sửa nhân viên cấp cơ sở (Staff bán vé/soát vé)
                if (!targetUser.isStaff()) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ được chỉnh sửa nhân viên cấp rạp.");
                    return;
                }

                if (role == null || role.isBlank() || !ValidationUtils.isStaffRole(role)) {
                    role = targetUser.getRole();
                } else {
                    role = role.trim().toUpperCase();
                }

                // 4.6. Kiểm tra quyền hạn: Manager không được sửa nhân viên của rạp khác
                if (!adminRole) {
                    if (currentUser.getCentreId() == null || targetUser.getCentreId() == null
                            || !currentUser.getCentreId().equals(targetUser.getCentreId())) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền quản lý nhân viên của rạp khác.");
                        return;
                    }
                }

                // 4.7. Kiểm tra email mới có bị trùng với tài khoản khác không
                if (userDAO.isEmailExistsForOtherUser(email, userId)) {
                    response.sendRedirect(request.getContextPath() + "/staff?error=email_exists");
                    return;
                }

                // 4.8. Thực hiện cập nhật thông tin qua UserDAO
                boolean updated = userDAO.updateStaffBasicInfo(userId, fullName, phone, email, role);
                if (updated) {
                    response.sendRedirect(request.getContextPath() + "/staff?success=updated");
                } else {
                    response.sendRedirect(request.getContextPath() + "/staff?error=update_failed");
                }
                return;
            } catch (Exception e) {
                response.sendRedirect(request.getContextPath() + "/staff?error=invalid_data");
                return;
            }
        }

        // 5. Xử lý kích hoạt hoặc tạm ngưng nhân viên (action = "status")
        if ("status".equalsIgnoreCase(action) || request.getParameter("status") != null) {
            try {
                // 5.1. Đọc ID nhân viên và trạng thái mới (ACTIVE / INACTIVE / LOCKED)
                int userId = Integer.parseInt(request.getParameter("userId"));
                String status = request.getParameter("status");
                if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status) && !"LOCKED".equals(status)) {
                    status = "ACTIVE";
                }

                // 5.2. Kiểm tra tài khoản nhân viên mục tiêu
                User targetUser = userDAO.getUserById(userId);
                if (targetUser == null) {
                    if (isAjax(request)) {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"success\":false,\"message\":\"Không tìm thấy tài khoản nhân viên.\"}");
                        return;
                    }
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy tài khoản nhân viên.");
                    return;
                }

                // 5.3. Đảm bảo chỉ thao tác trên nhân viên cấp rạp
                if (!targetUser.isStaff()) {
                    if (isAjax(request)) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"success\":false,\"message\":\"Chỉ được cập nhật trạng thái nhân viên cấp rạp.\"}");
                        return;
                    }
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ được cập nhật trạng thái nhân viên cấp rạp.");
                    return;
                }

                // 5.4. Manager chỉ có quyền đổi trạng thái nhân viên rạp mình
                if (!adminRole) {
                    if (currentUser.getCentreId() == null || targetUser.getCentreId() == null
                            || !currentUser.getCentreId().equals(targetUser.getCentreId())) {
                        if (isAjax(request)) {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"success\":false,\"message\":\"Bạn không có quyền quản lý nhân viên của rạp khác.\"}");
                            return;
                        }
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền quản lý nhân viên của rạp khác.");
                        return;
                    }
                }

                // 5.5. Cập nhật trạng thái người dùng trong DB
                userDAO.updateUserStatus(userId, status);

                // 5.6. Trả về phản hồi JSON nếu là AJAX request hoặc redirect nếu là form thông thường
                if (isAjax(request)) {
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"success\":true,\"userId\":" + userId + ",\"status\":\"" + status + "\",\"message\":\"Đã cập nhật trạng thái thành công.\"}");
                    return;
                }
                response.sendRedirect(request.getContextPath() + "/staff?success=status_updated");
                return;
            } catch (Exception e) {
                if (isAjax(request)) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"success\":false,\"message\":\"Dữ liệu không hợp lệ hoặc lỗi xử lý.\"}");
                    return;
                }
                response.sendRedirect(request.getContextPath() + "/staff?error=status_failed");
                return;
            }
        }

        // 6. Nếu không khớp hành động nào, chuyển hướng về danh bạ nhân sự
        response.sendRedirect(request.getContextPath() + "/staff");
    }
}
