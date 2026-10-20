package controller.cinema;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
        String uri = request.getRequestURI();
        String centreId = request.getParameter("centreId");
        if (centreId == null || centreId.isBlank()) {
            centreId = request.getParameter("branchId");
        }

        if (uri.endsWith("/details") || uri.endsWith("/details.jsp") || (centreId != null && !centreId.isBlank())) {
            request.getRequestDispatcher("/WEB-INF/views/cinema/cinemaDetail.jsp").forward(request, response);
        } else {
            request.getRequestDispatcher("/WEB-INF/views/cinema/cinemaList.jsp").forward(request, response);
        }
    }
}
