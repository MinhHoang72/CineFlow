package controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.entity.User;
import utils.WebUtil;
import java.io.IOException;

/**
 * Controller: Trang chủ CineFlow (/home, /home/index.jsp, /index.jsp).
 * Phân luồng điều hướng:
 * - Nếu tài khoản đăng nhập là nhân sự nội bộ (ADMIN, MANAGER, STAFF): tự động chuyển hướng ngay lập tức (Silent Redirect)
 *   về trang Dashboard tương ứng với vai trò của họ, ngăn ngừa việc tài khoản nội bộ bị đưa vào giao diện khách hàng thông thường.
 * - Khách hàng (CUSTOMER) hoặc người dùng chưa đăng nhập: Điều hướng tới View chuẩn hóa /WEB-INF/views/customer/homeIndex.jsp.
 */
@WebServlet(name = "HomeServlet", urlPatterns = {"/home", "/home/index.jsp", "/index.jsp"})
public class HomeServlet extends HttpServlet {

    /**
     * Kiểm tra phiên hiện tại có thuộc về tài khoản nội bộ (Admin, Manager, Staff) hay không.
     *
     * @param session phiên làm việc HttpSession
     * @return true nếu là tài khoản nội bộ, false nếu là khách hàng hoặc chưa đăng nhập
     */
    private boolean isInternalStaff(HttpSession session) {
        if (session == null) {
            return false;
        }
        Object roleObj = session.getAttribute("role");
        String role = null;
        if (roleObj instanceof String) {
            role = (String) roleObj;
        }
        if (role == null) {
            Object userObj = session.getAttribute("user");
            if (userObj instanceof User) {
                role = ((User) userObj).getRole();
            }
        }
        if (role == null || role.isBlank()) {
            return false;
        }
        return "ADMIN".equalsIgnoreCase(role)
                || "MANAGER".equalsIgnoreCase(role)
                || "STAFF".equalsIgnoreCase(role)
                || "TICKET_SALES_STAFF".equalsIgnoreCase(role)
                || "TICKET_CHECKER_STAFF".equalsIgnoreCase(role)
                || "TICKET_SALES".equalsIgnoreCase(role)
                || "TICKET_CHECKER".equalsIgnoreCase(role);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);

        // Nếu tài khoản hiện tại là nhân sự nội bộ (ADMIN/MANAGER/STAFF),
        // tự động chuyển hướng về trang làm việc đúng vai trò, không cho vào giao diện khách hàng.
        if (isInternalStaff(session)) {
            String targetPath = WebUtil.getRoleDefaultPath(session);
            response.sendRedirect(request.getContextPath() + targetPath);
            return;
        }

        request.getRequestDispatcher("/WEB-INF/views/customer/homeIndex.jsp").forward(request, response);
    }
}
