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

public class AuthenticationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request,
                          ServletResponse response,
                          FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest =
                (HttpServletRequest) request;

        HttpServletResponse httpResponse =
                (HttpServletResponse) response;

        // Lấy session hiện tại, không tạo session mới
        HttpSession session =
                httpRequest.getSession(false);

        // Kiểm tra user đã đăng nhập chưa
        boolean loggedIn =
                session != null
                && session.getAttribute("userId") != null;

        if (loggedIn) {

            // Đã đăng nhập → cho phép tiếp tục
            chain.doFilter(request, response);

        } else {
            // Session không còn hợp lệ: đưa người dùng về màn đăng nhập và lưu lại URL đang truy cập
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
