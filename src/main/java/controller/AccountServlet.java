package controller;

import dao.UserDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.util.UUID;
import utils.MediaStorage;

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
        String role = (String) session.getAttribute("role");
        String page = "MANAGER".equalsIgnoreCase(role) ? "/manager/profile.jsp" : "ADMIN".equalsIgnoreCase(role) ? "/admin/profile.jsp" : "/account/profile.jsp";
        request.getRequestDispatcher(page).forward(request, response);
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
        if (fullName == null || fullName.trim().isEmpty() || email == null || email.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/account?error=required");
            return;
        }
        if (userDAO.isEmailUsedByOtherUser(email.trim(), userId)) {
            response.sendRedirect(request.getContextPath() + "/account?error=email");
            return;
        }
        if (avatarPart != null && avatarPart.getSize() > 0) {
            String contentType = avatarPart.getContentType();
            String extension = "image/png".equalsIgnoreCase(contentType) ? ".png"
                    : "image/jpeg".equalsIgnoreCase(contentType) ? ".jpg"
                    : "image/webp".equalsIgnoreCase(contentType) ? ".webp" : null;
            if (extension == null) {
                response.sendRedirect(request.getContextPath() + "/account?error=avatarType");
                return;
            }
            String fileName = UUID.randomUUID().toString() + extension;
            java.nio.file.Path uploadDirectory = MediaStorage.uploadDirectory(getServletContext(), "avatars");
            avatarPart.write(uploadDirectory.resolve(fileName).toString());
            avatar = request.getContextPath() + "/uploads/avatars/" + fileName;
        }
        boolean updated = userDAO.updateProfile(userId, fullName.trim(), email.trim(),
                phone == null ? "" : phone.trim(), avatar == null ? "" : avatar.trim());
        if (updated) {
            session.setAttribute("user", userDAO.getUserById(userId));
        }
        response.sendRedirect(request.getContextPath() + "/account?updated=" + updated);
    }
}


