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
 * Filter bảo vệ không gian nhân viên (/staff/dashboard.jsp).
 * - Chưa đăng nhập: chuyển hướng về trang đăng nhập.
 * - CUSTOMER: chặn truy cập với HTTP 403 Forbidden.
 * - STAFF, MANAGER, ADMIN: cho phép truy cập.
 */
public class StaffFilter implements Filter {

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

    private boolean isStaffOrAbove(HttpSession session) {
        if (session == null) {
            return false;
        }
        String role = (String) session.getAttribute("role");
        if (role == null && session.getAttribute("user") instanceof User) {
            role = ((User) session.getAttribute("user")).getRole();
        }
        if (role == null) {
            return false;
        }
        return "STAFF".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role);
    }

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        HttpSession session = httpRequest.getSession(false);

        // 1. Kiểm tra đăng nhập
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

        // 2. Chặn CUSTOMER
        if (!isStaffOrAbove(session)) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Khách hàng không có quyền truy cập không gian nhân viên.");
            return;
        }

        chain.doFilter(request, response);
    }
}
