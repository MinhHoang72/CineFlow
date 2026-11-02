package controller.auth;
import jakarta.servlet.annotation.WebServlet;

import dao.UserDAO;
import model.entity.User;
import utils.PasswordUtil;
import utils.WebUtil;

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
@WebServlet(name = "LoginServlet", urlPatterns = {"/login", "/auth/login", "/auth/login.jsp"})
public class LoginServlet extends HttpServlet {

    private final service.UserService userService = new service.UserService();
    private final UserDAO userDAO = new UserDAO();

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

        // 4. Ủy quyền toàn bộ xác thực nghiệp vụ cho UserService (3-layer architecture)
        service.UserService.AuthResult authResult = userService.authenticate(accountOrEmail, password);
        if (!authResult.isSuccess()) {
            switch (authResult.getStatus()) {
                case ACCOUNT_LOCKED:
                    response.sendRedirect(request.getContextPath() + "/login?error=locked");
                    return;
                case ACCOUNT_INACTIVE:
                    response.sendRedirect(request.getContextPath() + "/login?error=inactive");
                    return;
                case EMAIL_UNVERIFIED:
                    String encodedEmail = URLEncoder.encode(authResult.getUser() != null ? authResult.getUser().getEmail() : "", StandardCharsets.UTF_8);
                    response.sendRedirect(request.getContextPath() + "/login?error=unverified&email=" + encodedEmail);
                    return;
                case MANAGER_UNASSIGNED:
                    response.sendRedirect(request.getContextPath() + "/login?error=manager_unassigned");
                    return;
                default:
                    response.sendRedirect(request.getContextPath() + "/login?error=invalid");
                    return;
            }
        }

        User user = authResult.getUser();

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
            // Kiem tra duong dan chuyen tiep hop le va dung phan quyen
            if (WebUtil.isAllowedRedirect(redirect, user.getRole())) {
                response.sendRedirect(request.getContextPath() + redirect);
                return;
            }
        }

        // 10. Dieu huong mac dinh theo vai tro (Silent Redirect chuan hoa)
        response.sendRedirect(request.getContextPath() + WebUtil.getRoleDefaultPath(user));
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
        request.getRequestDispatcher("/WEB-INF/views/auth/authLogin.jsp").forward(request, response);
    }
}
