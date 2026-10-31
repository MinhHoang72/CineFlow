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
 * Filter: Bảo vệ khu vực Nhân viên (Staff): /staff/*, /staff/dashboard.jsp.
 * Cho phép STAFF, MANAGER, ADMIN truy cập các tác vụ nhân sự/vận hành phù hợp.
 */
public class StaffFilter implements Filter {

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
     * Kiểm tra người dùng có quyền nhân sự hoặc quản lý trở lên.
     *
     * @param session phiên làm việc HttpSession
     * @return true nếu có quyền, ngược lại false
     */
    private boolean isStaffOrAbove(HttpSession session) {
        if (session == null) {
            return false;
        }
        Object roleObj = session.getAttribute("role");
        String role = (roleObj instanceof String) ? (String) roleObj : null;
        if (role == null && session.getAttribute("user") instanceof User) {
            role = ((User) session.getAttribute("user")).getRole();
        }
        return role != null && (
                "ADMIN".equalsIgnoreCase(role)
                || "MANAGER".equalsIgnoreCase(role)
                || "STAFF".equalsIgnoreCase(role)
                || "TICKET_SALES_STAFF".equalsIgnoreCase(role)
                || "TICKET_CHECKER_STAFF".equalsIgnoreCase(role)
                || "TICKET_SALES".equalsIgnoreCase(role)
                || "TICKET_CHECKER".equalsIgnoreCase(role)
        );
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Khởi tạo tài nguyên cho Filter nếu cần thiết
    }

    /**
     * Thực thi lọc request: Kiểm tra trạng thái đăng nhập và phân quyền truy cập vai trò nhân sự.
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

        // 1. Kiểm tra trạng thái đăng nhập
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

        // 2. Chặn các tài khoản không thuộc nhóm nhân sự truy cập
        if (!isStaffOrAbove(session)) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Bạn không có quyền truy cập khu vực nhân viên.");
            return;
        }

        // Đối với nhân viên tiền tuyến (front-line staff), đọc lại User để session nhận CentreID mới sau điều chuyển
        String role = (String) session.getAttribute("role");
        if (role == null && session.getAttribute("user") instanceof User) {
            role = ((User) session.getAttribute("user")).getRole();
        }
        if (role != null && !"ADMIN".equalsIgnoreCase(role) && !"MANAGER".equalsIgnoreCase(role)) {
            Object idObj = session.getAttribute("userId");
            int userId = idObj instanceof Number ? ((Number) idObj).intValue() : -1;
            User freshUser = userId > 0 ? userDAO.getUserById(userId) : null;
            boolean active = freshUser != null && "ACTIVE".equalsIgnoreCase(freshUser.getStatus());
            boolean assigned = freshUser != null && freshUser.getCentreId() != null
                    && freshUser.getCentreId() > 0;

            if (!active || !assigned) {
                session.invalidate();
                httpResponse.sendRedirect(httpResponse.encodeRedirectURL(
                        httpRequest.getContextPath() + "/login?error=staff_unassigned"));
                return;
            }

            session.setAttribute("user", freshUser);
            session.setAttribute("userId", freshUser.getUserId());
            session.setAttribute("role", freshUser.getRole());
            session.setAttribute("centreId", freshUser.getCentreId());
        }

        // 3. Đã xác thực vai trò hợp lệ -> Cho phép đi tiếp
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // Hủy bỏ tài nguyên khi dừng ứng dụng nếu cần thiết
    }
}
