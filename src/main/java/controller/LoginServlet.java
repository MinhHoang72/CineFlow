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

        String accountOrEmail = request.getParameter("accountOrEmail");
        String password = request.getParameter("password");

        // Kiểm tra dữ liệu rỗng
        if (accountOrEmail == null || accountOrEmail.trim().isEmpty()
                || password == null || password.isEmpty()) {

            response.sendError(
            HttpServletResponse.SC_BAD_REQUEST,
            "Account and Password are required."
            );
            return;
        }

        accountOrEmail = accountOrEmail.trim();

        // Cho phép đăng nhập bằng account hoặc email.
        User user = userDAO.getUserByAccountOrEmail(accountOrEmail);

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login.jsp?error=invalid");
            return;
        }

        // Lấy password trong database
        String storedPassword = user.getPassword();

        boolean passwordCorrect;

        if (storedPassword != null && storedPassword.startsWith("$2")) {
            passwordCorrect = PasswordUtils.checkPassword(password, storedPassword);
        } else {
            // Hỗ trợ tài khoản cũ và nâng cấp hash sau lần đăng nhập thành công.
            passwordCorrect = password.equals(storedPassword);
            if (passwordCorrect) {
                userDAO.updatePassword(user.getUserId(), PasswordUtils.hashPassword(password));
            }
        }

        if (!passwordCorrect) {
            response.sendRedirect(request.getContextPath() + "/auth/login.jsp?error=invalid");
            return;
        }

        // Kiểm tra trạng thái tài khoản
        if ("LOCKED".equalsIgnoreCase(user.getStatus())) {
            response.sendRedirect(request.getContextPath() + "/auth/login.jsp?error=locked");
            return;
        }

        // Chỉ tài khoản khách hàng (CUSTOMER) mới bắt buộc xác thực OTP kích hoạt email
        // Các tài khoản nội bộ (ADMIN, MANAGER, STAFF) do quản trị cấp trực tiếp
        boolean requiresVerification = "CUSTOMER".equalsIgnoreCase(user.getRole());
        if ("INACTIVE".equalsIgnoreCase(user.getStatus()) || (requiresVerification && !user.isEmailVerified())) {
            String encodedEmail = java.net.URLEncoder.encode(user.getEmail(), java.nio.charset.StandardCharsets.UTF_8);
            response.sendRedirect(request.getContextPath() + "/auth/login.jsp?error=unverified&email=" + encodedEmail);
            return;
        }

        // Tự động đồng bộ EmailVerified cho tài khoản nội bộ nếu chưa được đánh dấu
        if (!requiresVerification && !user.isEmailVerified()) {
            userDAO.activateUser(user.getUserId());
            user.setEmailVerified(true);
        }


        // Login thành công
        HttpSession session = request.getSession();

        session.setAttribute("user", user);
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("role", user.getRole());

        // Cập nhật thời gian đăng nhập
        userDAO.updateLastLogin(user.getUserId());

        // Kiểm tra tham số chuyển hướng (redirect) sau khi đăng nhập thành công
        String redirect = request.getParameter("redirect");
        if (redirect != null && !redirect.trim().isEmpty()) {
            redirect = redirect.trim();
            if (redirect.startsWith(request.getContextPath())) {
                redirect = redirect.substring(request.getContextPath().length());
            }
            // Chỉ chấp nhận đường dẫn nội bộ hợp lệ, tránh Open Redirect
            if (redirect.startsWith("/") && !redirect.startsWith("//")
                    && !redirect.toLowerCase().contains("/auth/login.jsp")
                    && !redirect.toLowerCase().contains("/login")) {
                response.sendRedirect(request.getContextPath() + redirect);
                return;
            }
        }

        // Mặc định chuyển về trang chủ (CUSTOMER) hoặc trang quản trị (ADMIN/MANAGER)
        String dashboard = "CUSTOMER".equalsIgnoreCase(user.getRole()) ? "/home/index.jsp" : "/dashboard";
        response.sendRedirect(request.getContextPath() + dashboard);
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


