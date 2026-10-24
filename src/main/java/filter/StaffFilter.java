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
import utils.WebUtil;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Filter: Bảo vệ không gian nhân viên (/staff/dashboard.jsp).
 * Quy tắc:
 * - Chưa đăng nhập: chuyển hướng về trang đăng nhập kèm URL chuyển hướng ban đầu.
 * - Khách hàng (CUSTOMER): tự động chuyển hướng ngay lập tức (Silent Redirect) về trang chủ /home, không hiện lỗi.
 * - Nhân viên hoặc cấp trên (STAFF, MANAGER, ADMIN): cho phép truy cập.
 */
public class StaffFilter implements Filter {

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
     * Kiểm tra người dùng đăng nhập có vai trò nhân viên cấp cơ sở hoặc quản lý trở lên hay không.
     *
     * @param session phiên làm việc HttpSession
     * @return true nếu có quyền, ngược lại false
     */
    private boolean isStaffOrAbove(HttpSession session) {
        if (session == null) {
            return false;
        }
        String role = (String) session.getAttribute("role");
        if (role == null && session.getAttribute("user") instanceof User) {
            role = ((User) session.getAttribute("user")).getRole();
        }
        return "STAFF".equalsIgnoreCase(role)
                || "TICKET_SALES_STAFF".equalsIgnoreCase(role)
                || "TICKET_CHECKER_STAFF".equalsIgnoreCase(role)
                || "TICKET_SALES".equalsIgnoreCase(role)
                || "TICKET_CHECKER".equalsIgnoreCase(role)
                || "MANAGER".equalsIgnoreCase(role)
                || "ADMIN".equalsIgnoreCase(role);
    }

    /**
     * Thực thi lọc request: Kiểm tra trạng thái đăng nhập và phân quyền truy cập cho nhân viên.
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

        // 2. Chặn các tài khoản khách hàng (CUSTOMER) truy cập vào khu vực nội bộ nhân viên
        // Tự động chuyển hướng ngay lập tức (Silent Redirect) về trang chủ khách hàng, không hiện lỗi
        if (!isStaffOrAbove(session)) {
            String targetPath = WebUtil.getRoleDefaultPath(session);
            httpResponse.sendRedirect(httpResponse.encodeRedirectURL(httpRequest.getContextPath() + targetPath));
            return;
        }

        // 3. Đã xác thực vai trò nhân sự hợp lệ -> Cho phép đi tiếp
        chain.doFilter(request, response);
    }
}
