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
 * Filter: Bộ lọc kiểm tra trạng thái xác thực người dùng cho các đường dẫn yêu cầu đăng nhập.
 */
public class AuthenticationFilter implements Filter {

    /**
     * Thực thi lọc request: Kiểm tra trạng thái phiên làm việc (Session) của người dùng.
     *
     * @param request  yêu cầu servlet
     * @param response phản hồi servlet
     * @param chain    chuỗi FilterChain
     * @throws IOException      nếu xảy ra lỗi vào/ra dữ liệu
     * @throws ServletException nếu xảy ra lỗi servlet
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 1. Lấy phiên làm việc hiện tại, không tự ý tạo phiên mới nếu chưa tồn tại
        HttpSession session = httpRequest.getSession(false);

        // 2. Kiểm tra xem người dùng đã hoàn tất đăng nhập hay chưa (userId tồn tại trong session)
        boolean loggedIn = session != null && session.getAttribute("userId") != null;

        if (loggedIn) {
            // 3. Đã đăng nhập hợp lệ -> Cho phép tiếp tục chuỗi xử lý
            chain.doFilter(request, response);
        } else {
            // 4. Chưa đăng nhập hoặc phiên đã hết hạn: Lưu đường dẫn hiện tại và chuyển hướng về trang đăng nhập
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
