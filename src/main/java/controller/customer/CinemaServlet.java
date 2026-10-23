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
 * Controller: Hệ thống Cụm rạp chiếu phim (/cinema, /cinema/details).
 * Điều hướng tới danh sách cụm rạp hoặc chi tiết cụm rạp.
 */
@WebServlet(name = "CinemaServlet", urlPatterns = {"/cinema", "/cinema/details", "/cinema/index.jsp", "/cinema/details.jsp"})
public class CinemaServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Silent Redirect: Tài khoản nội bộ (ADMIN, MANAGER, STAFF) không vào trang cụm rạp của khách hàng
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            String role = (String) session.getAttribute("role");
            if (role == null && session.getAttribute("user") instanceof User) {
                role = ((User) session.getAttribute("user")).getRole();
            }
            if (role != null && !"CUSTOMER".equalsIgnoreCase(role)) {
                response.sendRedirect(request.getContextPath() + WebUtil.getRoleDefaultPath(session));
                return;
            }
        }

        String uri = request.getRequestURI();
        String centreId = request.getParameter("centreId");
        if (centreId == null || centreId.isBlank()) {
            centreId = request.getParameter("branchId");
        }

        if (uri.endsWith("/details") || uri.endsWith("/details.jsp") || (centreId != null && !centreId.isBlank())) {
            request.getRequestDispatcher("/WEB-INF/views/customer/cinema/cinemaDetail.jsp").forward(request, response);
        } else {
            request.getRequestDispatcher("/WEB-INF/views/customer/cinema/cinemaList.jsp").forward(request, response);
        }
    }
}
