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

public class LoginServlet extends HttpServlet {

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

    String account = request.getParameter("account");
        String password = request.getParameter("password");

        // Kiểm tra dữ liệu rỗng
    if (account == null || account.trim().isEmpty()
                || password == null || password.isEmpty()) {

            response.sendError(
            HttpServletResponse.SC_BAD_REQUEST,
            "Account and Password are required."
            );
            return;
        }

    account = account.trim();

    // Login chỉ dùng Account và Password.
    User user = userDAO.getUserByAccount(account);

        if (user == null) {

            response.sendError(
            HttpServletResponse.SC_UNAUTHORIZED,
            "Invalid Account or Password."
            );
            return;
        }

        // Kiểm tra trạng thái tài khoản
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {

            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "Your account is not active."
            );
            return;
        }

        // Lấy password trong database
        String storedPassword = user.getPassword();

        // Password phải là BCrypt
        if (storedPassword == null
                || !storedPassword.startsWith("$2")) {

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Password is not using BCrypt."
            );
            return;
        }

        // Kiểm tra password
        boolean passwordCorrect =
                PasswordUtils.checkPassword(
                        password,
                        storedPassword
                );

        if (!passwordCorrect) {

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid Account/Email or Password."
            );
            return;
        }

        // Login thành công
        HttpSession session = request.getSession();

        session.setAttribute("user", user);
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("role", user.getRole());

        // Cập nhật thời gian đăng nhập
        userDAO.updateLastLogin(user.getUserId());

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println("Login successful.");
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