package filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Bộ lọc xác thực người dùng cho các khu vực yêu cầu đăng nhập.
 */
public class AuthenticationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Lấy phiên làm việc hiện tại, không tự ý tạo phiên mới nếu chưa tồn tại
        HttpSession session = httpRequest.getSession(false);

        // Kiểm tra xem người dùng đã hoàn tất đăng nhập hay chưa
        boolean loggedIn = session != null && session.getAttribute("userId") != null;

        if (loggedIn) {
            // Đã đăng nhập hợp lệ -> tiếp tục chuỗi xử lý
            chain.doFilter(request, response);
        } else {
            // Phiên không tồn tại hoặc hết hạn: lưu đường dẫn hiện tại và chuyển hướng về trang đăng nhập
            String uri = httpRequest.getRequestURI();
            String qs = httpRequest.getQueryString();
            String fullPath = uri + (qs != null && !qs.isEmpty() ? "?" + qs : "");
            if (fullPath.startsWith(httpRequest.getContextPath())) {
                fullPath = fullPath.substring(httpRequest.getContextPath().length());
            }
            String encoded = java.net.URLEncoder.encode(fullPath, "UTF-8");
            String loginUrl = httpRequest.getContextPath() + "/auth/login.jsp?error=session&redirect=" + encoded;
            httpResponse.sendRedirect(httpResponse.encodeRedirectURL(loginUrl));
        }
    }
}
