package controller;
import jakarta.servlet.annotation.WebServlet;

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
 * Controller: Xử lý xác thực đăng nhập người dùng, kiểm tra trạng thái kích hoạt tài khoản và điều hướng theo vai trò.
 * URL ánh xạ: /login
 */
@WebServlet(name = "LoginServlet", urlPatterns = {"/login", "/auth/login"})
public class LoginServlet extends HttpServlet {

    private UserDAO userDAO;

    /**
     * Khởi tạo Servlet và đối tượng DAO truy xuất cơ sở dữ liệu.
     */
    @Override
    public void init() {
        userDAO = new UserDAO();
    }

    /**
     * Xử lý yêu cầu HTTP POST: Xác thực tài khoản/mật khẩu, kiểm tra trạng thái hoạt động và điều hướng người dùng.
     *
     * @param request  yêu cầu HTTP từ client
     * @param response phản hồi HTTP gửi về client
     * @throws ServletException nếu xảy ra lỗi servlet
     * @throws IOException      nếu xảy ra lỗi vào/ra dữ liệu
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 1. Thiết lập encoding UTF-8 cho request
        request.setCharacterEncoding("UTF-8");

        // 2. Nhận thông tin đăng nhập từ biểu mẫu
        String accountOrEmail = request.getParameter("accountOrEmail");
        String password = request.getParameter("password");

        // 3. Kiểm tra tính hợp lệ cơ bản: Tài khoản/Email và Mật khẩu không được để trống
        if (accountOrEmail == null || accountOrEmail.trim().isEmpty()
                || password == null || password.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Account and Password are required.");
            return;
        }

        accountOrEmail = accountOrEmail.trim();

        // 4. Tìm kiếm người dùng trong cơ sở dữ liệu qua username hoặc email
        User user = userDAO.getUserByAccountOrEmail(accountOrEmail);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login.jsp?error=invalid");
            return;
        }

        // 5. Xác thực mật khẩu:
        String storedPassword = user.getPassword();
        boolean passwordCorrect;

        // 5.1. Nếu mật khẩu đã được băm bằng BCrypt ($2a$, $2b$...) -> đối soát qua BCrypt
        if (storedPassword != null && storedPassword.startsWith("$2")) {
            passwordCorrect = PasswordUtils.checkPassword(password, storedPassword);
        } else {
            // 5.2. Hỗ trợ dữ liệu mật khẩu cũ (nếu có) và tự động nâng cấp sang chuẩn băm BCrypt an toàn
            passwordCorrect = password.equals(storedPassword);
            if (passwordCorrect) {
                userDAO.updatePassword(user.getUserId(), PasswordUtils.hashPassword(password));
            }
        }

        // 5.3. Mật khẩu không khớp -> Báo lỗi đăng nhập thất bại
        if (!passwordCorrect) {
            response.sendRedirect(request.getContextPath() + "/auth/login.jsp?error=invalid");
            return;
        }

        // 6. Kiểm tra trạng thái tài khoản:
        // 6.1. Nếu tài khoản bị KHÓA (LOCKED) do vi phạm chính sách -> Ngăn chặn đăng nhập
        if ("LOCKED".equalsIgnoreCase(user.getStatus())) {
            response.sendRedirect(request.getContextPath() + "/auth/login.jsp?error=locked");
            return;
        }

        boolean isCustomer = "CUSTOMER".equalsIgnoreCase(user.getRole());

        // 6.2. Nếu tài khoản ở trạng thái TẠM NGƯNG (INACTIVE)
        if ("INACTIVE".equalsIgnoreCase(user.getStatus())) {
            // Trường hợp khách hàng mới đăng ký nhưng chưa kích hoạt OTP email
            if (isCustomer && !user.isEmailVerified()) {
                String encodedEmail = URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8);
                response.sendRedirect(request.getContextPath() + "/auth/login.jsp?error=unverified&email=" + encodedEmail);
                return;
            }
            // Trường hợp nhân viên/khách hàng bị Admin tạm ngừng hoạt động
            response.sendRedirect(request.getContextPath() + "/auth/login.jsp?error=inactive");
            return;
        }

        // 6.3. Khách hàng chưa xác thực email (dù status có thể là ACTIVE)
        if (isCustomer && !user.isEmailVerified()) {
            String encodedEmail = URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8);
            response.sendRedirect(request.getContextPath() + "/auth/login.jsp?error=unverified&email=" + encodedEmail);
            return;
        }

        // 6.4. Tài khoản nội bộ (Admin, Manager, Staff) do Admin tạo mặc định đã kích hoạt email
        if (!isCustomer && !user.isEmailVerified()) {
            userDAO.activateUser(user.getUserId());
            user.setEmailVerified(true);
        }

        // 7. Xác thực thành công: Khởi tạo phiên làm việc (HttpSession) và lưu thông tin định danh
        HttpSession session = request.getSession();
        session.setAttribute("user", user);
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("role", user.getRole());

        // 8. Cập nhật thời điểm đăng nhập gần nhất (Last Login) vào cơ sở dữ liệu
        userDAO.updateLastLogin(user.getUserId());

        // 9. Kiểm tra và xử lý đường dẫn chuyển tiếp (Redirect URL) an toàn nếu có
        String redirect = request.getParameter("redirect");
        if (redirect != null && !redirect.trim().isEmpty()) {
            redirect = redirect.trim();
            if (redirect.startsWith(request.getContextPath())) {
                redirect = redirect.substring(request.getContextPath().length());
            }
            // Phòng chống lỗ hổng Open Redirect: Chỉ chấp nhận đường dẫn nội bộ hợp lệ
            if (redirect.startsWith("/") && !redirect.startsWith("//")
                    && !redirect.toLowerCase().contains("/auth/login.jsp")
                    && !redirect.toLowerCase().contains("/login")) {
                response.sendRedirect(request.getContextPath() + redirect);
                return;
            }
        }

        // 10. Điều hướng mặc định theo vai trò: Khách hàng về trang chủ, Cán bộ quản trị về Dashboard
        String dashboard = "CUSTOMER".equalsIgnoreCase(user.getRole()) ? "/home/index.jsp" : "/dashboard";
        response.sendRedirect(request.getContextPath() + dashboard);
    }

    /**
     * Xử lý yêu cầu HTTP GET: Chặn truy cập trực tiếp bằng GET vào endpoint đăng nhập.
     *
     * @param request  yêu cầu HTTP từ client
     * @param response phản hồi HTTP gửi về client
     * @throws ServletException nếu xảy ra lỗi servlet
     * @throws IOException      nếu xảy ra lỗi vào/ra dữ liệu
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "GET method is not allowed.");
    }
}
