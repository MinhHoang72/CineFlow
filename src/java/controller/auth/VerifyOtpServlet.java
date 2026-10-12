package controller.auth;

import dao.UserDAO;
import model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class VerifyOtpServlet extends HttpServlet {

    private UserDAO userDAO;

    @Override
    public void init() {
        userDAO = new UserDAO();
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String email = request.getParameter("email");
        String otp = request.getParameter("otp");

        if (email == null || email.trim().isEmpty()
                || otp == null || !otp.matches("\\d{6}")) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "A valid 6-digit OTP is required."
            );
            return;
        }

        User user = userDAO.getUserByEmail(email.trim());
        User otpUser = user == null
                ? null
                : userDAO.getUserByResetToken(otp);

        if (otpUser == null || otpUser.getUserId() != user.getUserId()) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid or expired OTP."
            );
            return;
        }

        request.getSession(true).setAttribute("resetUserId", user.getUserId());
        request.getSession().setAttribute("resetVerified", true);
        response.sendRedirect(request.getContextPath() + "/reset-password");
    }
}


