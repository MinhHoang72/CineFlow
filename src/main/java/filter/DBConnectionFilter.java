package filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import java.io.IOException;
import utils.DBContext;

/**
 * Đóng các kết nối do DAO kế thừa DBContext mở trong request (constructor DBContext mở kết nối nhưng
 * servlet không đóng), tránh cạn kết nối tới SQL Server. Chạy sau khi servlet và JSP đã xử lý xong.
 */
@WebFilter(filterName = "DBConnectionFilter", urlPatterns = {"/*"})
public class DBConnectionFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } finally {
            DBContext.closeRequestConnections();
        }
    }
}
