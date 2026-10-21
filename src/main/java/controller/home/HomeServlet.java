package controller.home;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Controller: Trang chủ CineFlow (/home, /home/index.jsp).
 * Điều hướng trực tiếp tới View chuẩn hóa /WEB-INF/views/home/homeIndex.jsp.
 */
@WebServlet(name = "HomeServlet", urlPatterns = {"/home", "/home/index.jsp"})
public class HomeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/home/homeIndex.jsp").forward(request, response);
    }
}
