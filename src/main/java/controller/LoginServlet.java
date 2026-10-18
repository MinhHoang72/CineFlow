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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Xử lý xác thực đăng nhập người dùng, kiểm tra trạng thái kích hoạt tài khoản và điều hướng theo vai trò.
 */
public class LoginServlet extends HttpServlet {

    private UserDAO userDAO;

    @Override
    public void init() {
        userDAO = new UserDAO();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        String accountOrEmail = request.getParameter("accountOrEmail");
        String password = request.getParameter("password");

        // Kiểm tra dữ liệu bắt buộc
        if (accountOrEmail == null || accountOrEmail.trim().isEmpty()
                || password == null || password.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Account and Password are required.");
            return;
        }

        accountOrEmail = accountOrEmail.trim();

        // Cho phép đăng nhập linh hoạt bằng tài khoản (username) hoặc email
        User user = userDAO.getUserByAccountOrEmail(accountOrEmail);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login.jsp?error=invalid");
            return;
        }

        // Kiểm tra mật khẩu trong cơ sở dữ liệu
        String storedPassword = user.getPassword();
        boolean passwordCorrect;

        if (storedPassword != null && storedPassword.startsWith("$2")) {
            passwordCorrect = PasswordUtils.checkPassword(password, storedPassword);
        } else {
            // Hỗ trợ kiểm tra tài khoản cũ và nâng cấp băm mật khẩu sang BCrypt
            passwordCorrect = password.equals(storedPassword);
            if (passwordCorrect) {
                userDAO.updatePassword(user.getUserId(), PasswordUtils.hashPassword(password));
            }
        }

        if (!passwordCorrect) {
            response.sendRedirect(request.getContextPath() + "/auth/login.jsp?error=invalid");
            return;
        }

        // Kiểm tra trạng thái tài khoản có bị khóa hay không
        if ("LOCKED".equalsIgnoreCase(user.getStatus())) {
            response.sendRedirect(request.getContextPath() + "/auth/login.jsp?error=locked");
            return;
        }

        // Tài khoản khách hàng (CUSTOMER) bắt buộc phải xác thực email trước khi đăng nhập
        boolean isCustomer = "CUSTOMER".equalsIgnoreCase(user.getRole());

        // Kiểm tra tài khoản tạm ngưng (INACTIVE)
        if ("INACTIVE".equalsIgnoreCase(user.getStatus())) {
            // Chỉ hiển thị yêu cầu nhập OTP xác thực email nếu là CUSTOMER chưa kích hoạt email
            if (isCustomer && !user.isEmailVerified()) {
                String encodedEmail = URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8);
                response.sendRedirect(request.getContextPath() + "/auth/login.jsp?error=unverified&email=" + encodedEmail);
                return;
            }
            // Tài khoản nội bộ (Admin, Manager, Staff) hoặc tài khoản bị Admin tạm ngưng
            response.sendRedirect(request.getContextPath() + "/auth/login.jsp?error=inactive");
            return;
        }

        // Khách hàng chưa xác thực email (dù status có thể là ACTIVE hoặc rỗng)
        if (isCustomer && !user.isEmailVerified()) {
            String encodedEmail = URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8);
            response.sendRedirect(request.getContextPath() + "/auth/login.jsp?error=unverified&email=" + encodedEmail);
            return;
        }

        // Tự động đồng bộ trạng thái xác thực email cho tài khoản nội bộ nếu chưa kích hoạt
        if (!isCustomer && !user.isEmailVerified()) {
            userDAO.activateUser(user.getUserId());
            user.setEmailVerified(true);
        }

        // Đăng nhập thành công, khởi tạo session
        HttpSession session = request.getSession();
        session.setAttribute("user", user);
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("role", user.getRole());

        // Cập nhật thời điểm đăng nhập gần nhất
        userDAO.updateLastLogin(user.getUserId());

        // Kiểm tra tham số chuyển hướng (redirect) sau khi đăng nhập thành công
        String redirect = request.getParameter("redirect");
        if (redirect != null && !redirect.trim().isEmpty()) {
            redirect = redirect.trim();
            if (redirect.startsWith(request.getContextPath())) {
                redirect = redirect.substring(request.getContextPath().length());
            }
            // Chỉ chấp nhận đường dẫn nội bộ hợp lệ, tránh lỗi bảo mật Open Redirect
            if (redirect.startsWith("/") && !redirect.startsWith("//")
                    && !redirect.toLowerCase().contains("/auth/login.jsp")
                    && !redirect.toLowerCase().contains("/login")) {
                response.sendRedirect(request.getContextPath() + redirect);
                return;
            }
        }

        // Điều hướng mặc định: CUSTOMER về trang chủ, ADMIN/MANAGER/STAFF về dashboard
        String dashboard = "CUSTOMER".equalsIgnoreCase(user.getRole()) ? "/home/index.jsp" : "/dashboard";
        response.sendRedirect(request.getContextPath() + dashboard);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "GET method is not allowed.");
    }
}
