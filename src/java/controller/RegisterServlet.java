package controller;

import dao.UserDAO;
import model.User;
import utils.PasswordUtils;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class RegisterServlet extends HttpServlet {

    private UserDAO userDAO;

    @Override
    public void init() {

        userDAO = new UserDAO();

        System.out.println();
        System.out.println("========================================");
        System.out.println(" REGISTER SERVLET INITIALIZED");
        System.out.println("========================================");
    }

    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        try {

            request.setCharacterEncoding("UTF-8");

            System.out.println();
            System.out.println("========================================");
            System.out.println(" REGISTER REQUEST START");
            System.out.println("========================================");

            // =========================================
            // 1. GET DATA
            // =========================================
            String fullName = request.getParameter("fullName");
            String account = request.getParameter("account");
            String email = request.getParameter("email");
            String password = request.getParameter("password");
            String confirmPassword = request.getParameter("confirmPassword");

            System.out.println("FullName = " + fullName);
            System.out.println("Account  = " + account);
            System.out.println("Email    = " + email);
            System.out.println("Password received = " + (password != null));
            System.out.println("Confirm received = " + (confirmPassword != null));

            // =========================================
            // 2. CHECK EMPTY
            // =========================================
            if (fullName == null || fullName.trim().isEmpty()
                    || account == null || account.trim().isEmpty()
                    || email == null || email.trim().isEmpty()
                    || password == null || password.isEmpty()
                    || confirmPassword == null || confirmPassword.isEmpty()) {

                response.sendError(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "All fields are required."
                );
                return;
            }

            fullName = fullName.trim();
            account = account.trim();
            email = email.trim();

            // =========================================
            // 3. CONFIRM PASSWORD
            // =========================================
            if (!password.equals(confirmPassword)) {

                response.sendError(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Password and Confirm Password do not match."
                );
                return;
            }

            // =========================================
            // 4. PASSWORD VALIDATION
            // =========================================
            String passwordRegex
                    = "^(?=.*[A-Z])"
                    + "(?=.*[a-z])"
                    + "(?=.*\\d)"
                    + "(?=.*[@#$%^&*!])"
                    + ".{9,}$";

            if (!password.matches(passwordRegex)) {

                response.sendError(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Password must be at least 9 characters "
                        + "and contain uppercase, lowercase, number "
                        + "and special character."
                );
                return;
            }

            System.out.println("PASSWORD VALIDATION: OK");

            // =========================================
            // 5. CHECK ACCOUNT
            // =========================================
            System.out.println("Checking account...");

            boolean accountExists
                    = userDAO.isAccountExists(account);

            System.out.println(
                    "Account exists = " + accountExists
            );

            if (accountExists) {

                response.sendError(
                        HttpServletResponse.SC_CONFLICT,
                        "Account already exists."
                );
                return;
            }

            // =========================================
            // 6. CHECK EMAIL
            // =========================================
            System.out.println("Checking email...");

            boolean emailExists
                    = userDAO.isEmailExists(email);

            System.out.println(
                    "Email exists = " + emailExists
            );

            if (emailExists) {

                response.sendError(
                        HttpServletResponse.SC_CONFLICT,
                        "Email already exists."
                );
                return;
            }

            // =========================================
            // 7. HASH PASSWORD
            // =========================================
            System.out.println("Hashing password...");

            String hashedPassword
                    = PasswordUtils.hashPassword(password);

            System.out.println(
                    "Password hash created = "
                    + (hashedPassword != null)
            );

            // =========================================
            // 8. CREATE USER
            // =========================================
            User user = new User();

            user.setFullName(fullName);
            user.setAccount(account);
            user.setEmail(email);
            user.setPassword(hashedPassword);

            System.out.println("User object created.");

            // =========================================
            // 9. REGISTER
            // =========================================
            System.out.println(
                    ">>> CALLING userDAO.register()..."
            );

            boolean success
                    = userDAO.register(user);

            System.out.println(
                    ">>> userDAO.register() RESULT = "
                    + success
            );

            // =========================================
            // 10. REGISTER FAILED
            // =========================================
            if (!success) {

                System.out.println(
                        "ERROR: DATABASE INSERT FAILED"
                );

                response.sendError(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Registration failed."
                );
                return;
            }

            // =========================================
            // 11. SUCCESS
            // =========================================
            System.out.println();
            System.out.println("========================================");
            System.out.println(" REGISTER SUCCESS");
            System.out.println(" Account = " + account);
            System.out.println(" Email   = " + email);
            System.out.println("========================================");

            response.sendRedirect(
                    request.getContextPath()
                    + "/login.jsp"
            );

        } catch (Exception e) {

            System.out.println();
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println(" REGISTER SERVLET EXCEPTION");
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

            e.printStackTrace();

            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

            response.sendError(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Internal server error. Check NetBeans Output."
            );
        }
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
