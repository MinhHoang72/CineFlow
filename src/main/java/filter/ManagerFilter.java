package filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.User;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Filter: Bảo vệ khu vực Quản lý (Manager): /manager/*, /staff, /reports.
 * Quy tắc:
 * - Chưa đăng nhập: chuyển hướng về /auth/login.jsp kèm URL redirect.
 * - Đã đăng nhập nhưng không có vai trò MANAGER hoặc ADMIN: từ chối với HTTP 403 Forbidden.
 * - Có vai trò MANAGER hoặc ADMIN: cho phép tiếp tục.
 */
public class ManagerFilter implements Filter {

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
        return userObj instanceof User && ((User) userObj).getUserId() > 0;
    }

    /**
     * Kiểm tra người dùng đăng nhập có vai trò Quản lý (MANAGER) hoặc Quản trị viên (ADMIN) hay không.
     *
     * @param session phiên làm việc HttpSession
     * @return true nếu có quyền, ngược lại false
     */
    private boolean isManagerOrAdmin(HttpSession session) {
        if (session == null) {
            return false;
        }
        Object roleObj = session.getAttribute("role");
        if (roleObj instanceof String) {
            String role = (String) roleObj;
            if ("MANAGER".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) {
                return true;
            }
        }
        Object userObj = session.getAttribute("user");
        if (userObj instanceof User) {
            String role = ((User) userObj).getRole();
            if (role != null && ("MANAGER".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Thực thi lọc request: Kiểm tra trạng thái đăng nhập và phân quyền truy cập vai trò MANAGER/ADMIN.
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
            String loginUrl = httpRequest.getContextPath() + "/auth/login.jsp?redirect=" + encoded;
            httpResponse.sendRedirect(httpResponse.encodeRedirectURL(loginUrl));
            return;
        }

        // 2. Kiểm tra quyền hạn vai trò MANAGER hoặc ADMIN
        if (!isManagerOrAdmin(session)) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập khu vực Quản lý (Manager).");
            return;
        }

        // 3. Đã xác thực quyền hạn hợp lệ -> Cho phép tiếp tục tới Controller
        chain.doFilter(request, response);
    }
}
