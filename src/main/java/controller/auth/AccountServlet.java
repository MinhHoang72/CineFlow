package controller.auth;
import jakarta.servlet.annotation.WebServlet;

import dao.UserDAO;
import model.entity.User;
import utils.EmailUtils;
import utils.MediaStorage;
import utils.ValidationUtils;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Controller: Điều khiển trang quản lý thông tin tài khoản cá nhân (/account),
 * xem lịch sử đặt vé và cập nhật thông tin người dùng (họ tên, số điện thoại, avatar, đổi email qua OTP).
 * URL ánh xạ: /account
 */
@WebServlet(name = "AccountServlet", urlPatterns = {"/account", "/profile"})
@MultipartConfig(maxFileSize = 2 * 1024 * 1024, maxRequestSize = 3 * 1024 * 1024)
public class AccountServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    /**
     * Xử lý yêu cầu HTTP GET: Tải thông tin hồ sơ cá nhân và lịch sử đặt vé của người dùng hiện tại.
     *
     * @param request  yêu cầu HTTP từ client
     * @param response phản hồi HTTP gửi về client
     * @throws ServletException nếu xảy ra lỗi servlet
     * @throws IOException      nếu xảy ra lỗi vào/ra dữ liệu
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 1. Kiểm tra xác thực phiên đăng nhập (nếu chưa đăng nhập thì chuyển về trang Login)
        HttpSession session = request.getSession(false);
        Integer userId = session == null ? null : (Integer) session.getAttribute("userId");
        if (userId == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login.jsp");
            return;
        }

        // 2. Truy vấn dữ liệu người dùng và danh sách lịch sử đặt vé từ UserDAO
        request.setAttribute("accountUser", userDAO.getUserById(userId));
        request.setAttribute("bookingHistory", userDAO.getBookingHistory(userId));

        // 3. Chuyển tiếp tới giao diện trang hồ sơ cá nhân profile.jsp
        request.getRequestDispatcher("/WEB-INF/views/auth/userProfile.jsp").forward(request, response);
    }

    /**
     * Xử lý yêu cầu HTTP POST: Cập nhật thông tin cá nhân (họ tên, số điện thoại, ảnh đại diện, hoặc kích hoạt đổi email qua OTP).
     *
     * @param request  yêu cầu HTTP từ client
     * @param response phản hồi HTTP gửi về client
     * @throws ServletException nếu xảy ra lỗi servlet
     * @throws IOException      nếu xảy ra lỗi vào/ra dữ liệu
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 1. Thiết lập bảng mã UTF-8
        request.setCharacterEncoding("UTF-8");

        // 2. Kiểm tra xác thực người dùng trong phiên làm việc
        HttpSession session = request.getSession(false);
        Integer userId = session == null ? null : (Integer) session.getAttribute("userId");
        if (userId == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login.jsp");
            return;
        }

        // 3. Đọc dữ liệu từ form cập nhật
        String fullName = request.getParameter("fullName");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String avatar = request.getParameter("avatar");
        Part avatarPart = request.getContentType() != null
                && request.getContentType().toLowerCase().startsWith("multipart/form-data")
                ? request.getPart("avatarFile") : null;

        // 4. Kiểm tra các trường bắt buộc không được rỗng
        if (ValidationUtils.isBlank(fullName) || ValidationUtils.isBlank(email)) {
            response.sendRedirect(request.getContextPath() + "/account?error=required");
            return;
        }

        // 5. Kiểm tra tính hợp lệ của định dạng Họ tên, Email, Số điện thoại
        if (!ValidationUtils.isValidFullName(fullName)) {
            response.sendRedirect(request.getContextPath() + "/account?error=invalidName");
            return;
        }

        if (!ValidationUtils.isValidEmail(email)) {
            response.sendRedirect(request.getContextPath() + "/account?error=invalidEmail");
            return;
        }

        if (!ValidationUtils.isValidPhone(phone, false)) {
            response.sendRedirect(request.getContextPath() + "/account?error=invalidPhone");
            return;
        }

        // 6. Kiểm tra xem email mới có bị trùng lặp với người dùng khác trong hệ thống không
        if (userDAO.isEmailUsedByOtherUser(email.trim(), userId)) {
            response.sendRedirect(request.getContextPath() + "/account?error=email");
            return;
        }

        // 7. Xử lý tải lên file ảnh đại diện (Avatar) nếu người dùng có chọn file mới
        if (avatarPart != null && avatarPart.getSize() > 0) {
            String submittedFileName = avatarPart.getSubmittedFileName();
            if (submittedFileName != null && !ValidationUtils.isValidImageExtension(submittedFileName)) {
                response.sendRedirect(request.getContextPath() + "/account?error=avatarType");
                return;
            }

            String contentType = avatarPart.getContentType();
            String extension = "image/png".equalsIgnoreCase(contentType) ? ".png"
                    : "image/jpeg".equalsIgnoreCase(contentType) ? ".jpg"
                    : "image/webp".equalsIgnoreCase(contentType) ? ".webp" : null;
            if (extension == null) {
                response.sendRedirect(request.getContextPath() + "/account?error=avatarType");
                return;
            }

            String fileName = UUID.randomUUID().toString() + extension;
            Path uploadDirectory = MediaStorage.uploadDirectory(getServletContext(), "avatars");
            avatarPart.write(uploadDirectory.resolve(fileName).toString());
            avatar = request.getContextPath() + "/uploads/avatars/" + fileName;
        }

        User currentUser = userDAO.getUserById(userId);
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login.jsp");
            return;
        }

        if (avatar == null || avatar.trim().isEmpty()) {
            avatar = currentUser.getAvatar();
        }

        // 8. Kiểm tra xem người dùng có yêu cầu thay đổi địa chỉ email hay không
        boolean isEmailChanged = !currentUser.getEmail().equalsIgnoreCase(email.trim());

        // 8.1. Trường hợp không đổi email: Cập nhật trực tiếp thông tin vào DB
        if (!isEmailChanged) {
            boolean updated = userDAO.updateProfile(userId, fullName.trim(), currentUser.getEmail(),
                    phone == null ? "" : phone.trim(), avatar == null ? "" : avatar.trim());
            if (updated) {
                session.setAttribute("user", userDAO.getUserById(userId));
            }
            response.sendRedirect(request.getContextPath() + "/account?updated=" + updated);
        } else {
            // 8.2. Trường hợp có đổi email: Cập nhật thông tin phụ, giữ nguyên email cũ và yêu cầu xác thực OTP email mới
            userDAO.updateProfile(userId, fullName.trim(), currentUser.getEmail(),
                    phone == null ? "" : phone.trim(), avatar == null ? "" : avatar.trim());
            session.setAttribute("user", userDAO.getUserById(userId));

            // Tạo mã OTP 6 chữ số ngẫu nhiên có hiệu lực 15 phút
            String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
            Timestamp expiry = new Timestamp(System.currentTimeMillis() + (15 * 60 * 1000));
            userDAO.updateActivationOtp(userId, otp, expiry);

            // Lưu tạm email mới vào session chờ người dùng nhập mã OTP xác thực
            session.setAttribute("pendingNewEmail", email.trim());
            session.setAttribute("pendingEmailUserId", userId);

            // Gửi email chứa mã OTP xác nhận đổi email
            try {
                EmailUtils.sendEmailChangeOtpEmail(email.trim(), otp, fullName.trim());
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            // Chuyển hướng người dùng sang trang nhập mã OTP xác thực
            response.sendRedirect(request.getContextPath() + "/auth/verify-otp.jsp?email="
                    + URLEncoder.encode(email.trim(), StandardCharsets.UTF_8)
                    + "&action=changeEmail");
        }
    }
}
