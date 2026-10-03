package controller;

import dao.UserDAO;
import model.User;
import utils.PasswordUtils;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class ResetPasswordServlet extends HttpServlet {

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

        jakarta.servlet.http.HttpSession session = request.getSession(false);
        Integer resetUserId = session == null
                ? null
                : (Integer) session.getAttribute("resetUserId");

        Boolean resetVerified = session == null
                ? null
                : (Boolean) session.getAttribute("resetVerified");

        if (resetUserId == null || !Boolean.TRUE.equals(resetVerified)) {
            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "OTP verification is required."
            );
            return;
        }

        // =========================================
        // 2. GET NEW PASSWORD
        // =========================================
        String newPassword =
                request.getParameter("newPassword");

        String confirmPassword =
                request.getParameter("confirmPassword");

        if (newPassword == null
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
        // 3. CHECK PASSWORD CONFIRMATION
        // =========================================
        if (!newPassword.equals(confirmPassword)) {

            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "New password and Confirm Password do not match."
            );
            return;
        }

        // =========================================
        // 4. HASH NEW PASSWORD
        // =========================================
        String hashedPassword =
                PasswordUtils.hashPassword(newPassword);

        // =========================================
        // 5. RESET PASSWORD and clear OTP
        // =========================================
        boolean success =
                userDAO.resetPassword(
                        resetUserId,
                        hashedPassword
                );

        if (!success) {
            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Failed to reset password."
            );
            return;
        }

        // =========================================
        // 7. SUCCESS
        // =========================================
        session.invalidate();
        response.sendRedirect(request.getContextPath() + "/login.jsp");
    }

    @Override
    protected void doGet(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        jakarta.servlet.http.HttpSession session = request.getSession(false);
        if (session == null
                || !Boolean.TRUE.equals(session.getAttribute("resetVerified"))) {
            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "OTP verification is required."
            );
            return;
        }

        request.getRequestDispatcher("/reset-password.jsp")
                .forward(request, response);
    }
}