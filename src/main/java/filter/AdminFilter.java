package filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.entity.User;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Filter bảo vệ toàn bộ khu vực quản trị Admin (/admin/*).
 * Quy tắc:
 * - Nếu chưa đăng nhập: chuyển hướng về trang đăng nhập kèm URL chuyển hướng ban đầu.
 * - Nếu đã đăng nhập nhưng không phải ADMIN: chặn truy cập với HTTP 403 Forbidden.
 * - Nếu có vai trò ADMIN: cho phép tiếp tục chuỗi filter.
 */
public class AdminFilter implements Filter {

    /**
     * Kiểm tra phiên hiện tại đã đăng nhập tài khoản hay chưa.
     *
     * @param session phiên làm việc HttpSession
     * @return true nếu đã đăng nhập, ngược lại false
     */
    private boolean isLoggedIn(HttpSession session) {
        if (session == null) {
            return false;
        }
        if (session.getAttribute("userId") != null) {
            return true;
        }
        Object userObj = session.getAttribute("user");
        if (userObj instanceof User && ((User) userObj).getUserId() > 0) {
            return true;
        }
        return false;
    }

    /**
     * Kiểm tra người dùng đăng nhập trong phiên có vai trò Quản trị viên (ADMIN) hay không.
     *
     * @param session phiên làm việc HttpSession
     * @return true nếu là Admin, ngược lại false
     */
    private boolean isAdmin(HttpSession session) {
        if (session == null) {
            return false;
        }
        Object roleObj = session.getAttribute("role");
        if (roleObj instanceof String && "ADMIN".equalsIgnoreCase((String) roleObj)) {
            return true;
        }
        Object userObj = session.getAttribute("user");
        if (userObj instanceof User) {
            String userRole = ((User) userObj).getRole();
            if (userRole != null && "ADMIN".equalsIgnoreCase(userRole)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Thực thi chặn lọc yêu cầu: Kiểm tra xác thực đăng nhập và quyền hạn vai trò ADMIN.
     *
     * @param request  yêu cầu servlet
     * @param response phản hồi servlet
     * @param chain    chuỗi FilterChain
     * @throws IOException      nếu xảy ra lỗi vào/ra dữ liệu
     * @throws ServletException nếu xảy ra lỗi servlet
     */
    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        HttpSession session = httpRequest.getSession(false);

        // 1. Kiểm tra trạng thái đăng nhập (nếu chưa đăng nhập chuyển về trang login kèm redirect URL)
        if (!isLoggedIn(session)) {
            String uri = httpRequest.getRequestURI();
            String qs = httpRequest.getQueryString();
            String fullPath = uri + (qs != null && !qs.isEmpty() ? "?" + qs : "");
            if (fullPath.startsWith(httpRequest.getContextPath())) {
                fullPath = fullPath.substring(httpRequest.getContextPath().length());
            }
            String encoded = URLEncoder.encode(fullPath, StandardCharsets.UTF_8);
            String loginUrl = httpRequest.getContextPath() + "/login?redirect=" + encoded;
            httpResponse.sendRedirect(httpResponse.encodeRedirectURL(loginUrl));
            return;
        }

        // 2. Kiểm tra vai trò ADMIN (Role-Based Access Control - RBAC)
        if (!isAdmin(session)) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập khu vực Quản trị viên (Admin).");
            return;
        }

        // 3. Đã xác thực quyền Admin hợp lệ -> Cho phép đi tiếp vào Controller
        chain.doFilter(request, response);
    }
}
