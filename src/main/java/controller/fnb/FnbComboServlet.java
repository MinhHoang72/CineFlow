package controller.fnb;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Controller: Quản lý Combo bắp nước.
 * URL ánh xạ: /manager/fnb/combos, /fnb/combos
 */
@WebServlet(name = "FnbComboServlet", urlPatterns = {"/manager/fnb/combos", "/fnb/combos"})
public class FnbComboServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/fnb?category=COMBO");
    }
}
