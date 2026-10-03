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

            // Chưa đăng nhập → từ chối truy cập
            httpResponse.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "You must login first."
            );
        }
    }
}