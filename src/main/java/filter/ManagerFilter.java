package filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.entity.User;
import dao.UserDAO;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Filter: Bảo vệ khu vực Quản lý (Manager): /manager/*, /manager, /reports.
 * Quy tắc:
 * - Chưa đăng nhập: chuyển hướng về /login kèm URL redirect.
 * - Cho phép tài khoản MANAGER và ADMIN truy cập.
 * - Tài khoản khác bị từ chối 403.
 */
public class ManagerFilter implements Filter {

    private final UserDAO userDAO = new UserDAO();

    /**
     * Kiểm tra phiên hiện tại đã đăng nhập tài khoản hay chưa.
     *
     * @param session phiên làm việc HttpSession
     * @return true nếu đã đăng nhập (tồn tại userId), ngược lại false
     */
    private boolean isLoggedIn(HttpSession session) {
        return session != null && session.getAttribute("userId") != null;
    }

    /**
     * Kiểm tra người dùng có vai trò MANAGER hoặc ADMIN hay không.
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

    private boolean isAdmin(HttpSession session) {
        if (session == null) {
            return false;
        }
        Object roleObj = session.getAttribute("role");
        if (roleObj instanceof String) {
            return "ADMIN".equalsIgnoreCase((String) roleObj);
        }
        Object userObj = session.getAttribute("user");
        return userObj instanceof User
                && "ADMIN".equalsIgnoreCase(((User) userObj).getRole());
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Khởi tạo tài nguyên cho Filter nếu cần thiết
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
            String loginUrl = httpRequest.getContextPath() + "/login?redirect=" + encoded;
            httpResponse.sendRedirect(httpResponse.encodeRedirectURL(loginUrl));
            return;
        }

        // 2. Khu vực quản lý chỉ dành cho MANAGER và ADMIN
        if (!isManagerOrAdmin(session)) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Bạn không có quyền truy cập khu vực quản lý.");
            return;
        }

        String servletPath = httpRequest.getServletPath();
        boolean pricingRoute = "/pricing".equals(servletPath)
                || (servletPath != null && servletPath.startsWith("/pricing/"));
        if (pricingRoute && !isAdmin(session)) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Chức năng quản lý giá vé và phụ thu chỉ dành cho quản trị viên.");
            return;
        }

        // Admin không bị giới hạn bởi chi nhánh. Manager phải được xác thực lại
        // từ database để session cũ không giữ quyền sau khi bị miễn nhiệm.
        Object roleObj = session.getAttribute("role");
        if (roleObj instanceof String && "MANAGER".equalsIgnoreCase((String) roleObj)) {
            Object idObj = session.getAttribute("userId");
            int userId = idObj instanceof Number ? ((Number) idObj).intValue() : -1;
            User freshUser = userId > 0 ? userDAO.getUserById(userId) : null;
            boolean active = freshUser != null && "ACTIVE".equalsIgnoreCase(freshUser.getStatus());
            boolean assigned = freshUser != null && freshUser.getCentreId() != null
                    && freshUser.getCentreId() > 0
                    && userDAO.hasActiveManagerAssignment(freshUser.getUserId(), freshUser.getCentreId());

            if (!active || !assigned) {
                session.invalidate();
                httpResponse.sendRedirect(httpResponse.encodeRedirectURL(
                        httpRequest.getContextPath() + "/login?error=manager_unassigned"));
                return;
            }

            session.setAttribute("user", freshUser);
            session.setAttribute("userId", freshUser.getUserId());
            session.setAttribute("role", freshUser.getRole());
            session.setAttribute("centreId", freshUser.getCentreId());
        }

        // 3. Đã xác thực hợp lệ -> Cho phép tiếp tục đi vào controller
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // Hủy bỏ tài nguyên khi dừng ứng dụng nếu cần thiết
    }
}
