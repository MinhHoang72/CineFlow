package controller;

import dao.UserDAO;
import model.User;
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
 * Điều khiển trang quản lý tài khoản cá nhân, xem lịch sử đặt vé và cập nhật thông tin người dùng.
 */
@MultipartConfig(maxFileSize = 2 * 1024 * 1024, maxRequestSize = 3 * 1024 * 1024)
public class AccountServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        Integer userId = session == null ? null : (Integer) session.getAttribute("userId");
        if (userId == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login.jsp");
            return;
        }

        request.setAttribute("accountUser", userDAO.getUserById(userId));
        request.setAttribute("bookingHistory", userDAO.getBookingHistory(userId));

        request.getRequestDispatcher("/account/profile.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        Integer userId = session == null ? null : (Integer) session.getAttribute("userId");
        if (userId == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login.jsp");
            return;
        }

        String fullName = request.getParameter("fullName");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String avatar = request.getParameter("avatar");
        Part avatarPart = request.getContentType() != null
                && request.getContentType().toLowerCase().startsWith("multipart/form-data")
                ? request.getPart("avatarFile") : null;

        if (ValidationUtils.isBlank(fullName) || ValidationUtils.isBlank(email)) {
            response.sendRedirect(request.getContextPath() + "/account?error=required");
            return;
        }

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

        // Kiểm tra xem email mới có bị trùng lặp với người dùng khác hay không
        if (userDAO.isEmailUsedByOtherUser(email.trim(), userId)) {
            response.sendRedirect(request.getContextPath() + "/account?error=email");
            return;
        }

        // Xử lý tải ảnh đại diện nếu có
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

        boolean isEmailChanged = !currentUser.getEmail().equalsIgnoreCase(email.trim());

        if (!isEmailChanged) {
            // Không đổi email: Cập nhật các thông tin khác ngay
            boolean updated = userDAO.updateProfile(userId, fullName.trim(), currentUser.getEmail(),
                    phone == null ? "" : phone.trim(), avatar == null ? "" : avatar.trim());
            if (updated) {
                session.setAttribute("user", userDAO.getUserById(userId));
            }
            response.sendRedirect(request.getContextPath() + "/account?updated=" + updated);
        } else {
            // Có thay đổi email: Cập nhật trước thông tin cá nhân (vẫn giữ email cũ trong DB cho tới khi xác thực OTP)
            userDAO.updateProfile(userId, fullName.trim(), currentUser.getEmail(),
                    phone == null ? "" : phone.trim(), avatar == null ? "" : avatar.trim());
            session.setAttribute("user", userDAO.getUserById(userId));

            // Sinh mã OTP 6 chữ số và thời hạn 15 phút
            String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
            Timestamp expiry = new Timestamp(System.currentTimeMillis() + (15 * 60 * 1000));
            userDAO.updateActivationOtp(userId, otp, expiry);

            // Lưu email mới vào session để đối soát khi xác thực
            session.setAttribute("pendingNewEmail", email.trim());
            session.setAttribute("pendingEmailUserId", userId);

            // Gửi OTP đến địa chỉ email mới
            try {
                EmailUtils.sendEmailChangeOtpEmail(email.trim(), otp, fullName.trim());
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            // Chuyển hướng người dùng sang trang nhập OTP xác thực email
            response.sendRedirect(request.getContextPath() + "/auth/verify-otp.jsp?email="
                    + URLEncoder.encode(email.trim(), StandardCharsets.UTF_8)
                    + "&action=changeEmail");
        }
    }
}
