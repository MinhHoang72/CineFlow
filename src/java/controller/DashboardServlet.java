package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

public class DashboardServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        String role = session == null ? null : (String) session.getAttribute("role");
        if (role == null) { response.sendRedirect(request.getContextPath() + "/auth/login.jsp"); return; }
        String page = "ADMIN".equals(role) ? "/admin/dashboard.jsp" : "MANAGER".equals(role) ? "/manager/dashboard.jsp" : "STAFF".equals(role) ? "/staff/dashboard.jsp" : "/home/index.jsp";
        request.getRequestDispatcher(page).forward(request, response);
    }
}
