package controller;

import dao.UserDAO;
import model.User;
import utils.PasswordUtils;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

public class ChangePasswordServlet extends HttpServlet {

    private UserDAO userDAO;

    @Override
    public void init() {
        userDAO = new UserDAO();
    }

    @Override
    protected void doPost(HttpServletRequest request,
                           HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        // =========================================
        // 1. CHECK LOGIN SESSION
        // =========================================
        HttpSession session = request.getSession(false);

        if (session == null) {
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "You are not logged in."
            );
            return;
        }

        // =========================================
        // 2. GET USER ID FROM SESSION
        // =========================================
        Integer userIdObj =
                (Integer) session.getAttribute("userId");

        if (userIdObj == null) {
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "You are not logged in."
            );
            return;
        }

        int userId = userIdObj;

        // =========================================
        // 3. GET PASSWORD DATA
        // =========================================
        String currentPassword =
                request.getParameter("currentPassword");

        String newPassword =
                request.getParameter("newPassword");

        String confirmPassword =
                request.getParameter("confirmPassword");

        // =========================================
        // 4. CHECK EMPTY FIELDS
        // =========================================
        if (currentPassword == null
                || currentPassword.isEmpty()
                || newPassword == null
                || newPassword.isEmpty()
                || confirmPassword == null
                || confirmPassword.isEmpty()) {

            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "All password fields are required."
            );
            return;
        }

        // =========================================
        // 5. CHECK NEW PASSWORD = CONFIRM PASSWORD
        // =========================================
        if (!newPassword.equals(confirmPassword)) {

            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "New password and Confirm Password do not match."
            );
            return;
        }

        // =========================================
        // 6. GET USER FROM DATABASE
        // =========================================
        User user = userDAO.getUserById(userId);

        if (user == null) {

            response.sendError(
                    HttpServletResponse.SC_NOT_FOUND,
                    "User not found."
            );
            return;
        }

        // =========================================
        // 7. CHECK CURRENT PASSWORD
        // =========================================
        String storedPassword = user.getPassword();

        if (storedPassword == null
                || !storedPassword.startsWith("$2")) {

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Current password is not using BCrypt."
            );
            return;
        }

        boolean currentPasswordCorrect =
                PasswordUtils.checkPassword(
                        currentPassword,
                        storedPassword
                );

        if (!currentPasswordCorrect) {

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Current password is incorrect."
            );
            return;
        }

        // =========================================
        // 8. NEW PASSWORD MUST BE DIFFERENT
        // =========================================
        if (PasswordUtils.checkPassword(
                newPassword,
                storedPassword)) {

            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "New password must be different from current password."
            );
            return;
        }

        // =========================================
        // 9. HASH NEW PASSWORD
        // =========================================
        String hashedPassword =
                PasswordUtils.hashPassword(newPassword);

        // =========================================
        // 10. UPDATE DATABASE
        // =========================================
        boolean success =
                userDAO.changePassword(
                        userId,
                        hashedPassword
                );

        if (!success) {

            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Failed to change password."
            );
            return;
        }

        // =========================================
        // 11. SUCCESS
        // =========================================
        response.setStatus(
                HttpServletResponse.SC_OK
        );

        response.getWriter().println(
                "Password changed successfully."
        );
    }

    @Override
    protected void doGet(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        response.sendError(
                HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "GET method is not allowed."
        );
    }
}


