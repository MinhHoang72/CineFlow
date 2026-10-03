package controller.manager;

import dao.CinemaDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Movie;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

    /** Manager phân phối phim trên toàn hệ thống. */
public class DistributionServlet extends HttpServlet {
    private final CinemaDAO dao = new CinemaDAO();

    private boolean allowed(HttpSession session) {
        return session != null && "MANAGER".equals(session.getAttribute("role"));
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!allowed(request.getSession(false))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Manager được phân phối phim.");
            return;
        }
        try {
            int movieId = Integer.parseInt(request.getParameter("movieId"));
            Movie movie = dao.getMovieById(movieId);
            List<?> branches = dao.getDistributionBranches(movieId);
            if (movie == null) { response.sendError(404, "Không tìm thấy phim."); return; }
            request.setAttribute("movie", movie);
            request.setAttribute("branches", branches);
            request.getRequestDispatcher("/manager/distribution.jsp").forward(request, response);
        } catch (NumberFormatException exception) { response.sendError(400, "Mã phim không hợp lệ."); }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!allowed(request.getSession(false))) { response.sendError(403); return; }
        try {
            int movieId = Integer.parseInt(request.getParameter("movieId"));
            Set<Integer> selected = new HashSet<>();
            String[] ids = request.getParameterValues("branchIds");
            if (ids != null) for (String id : ids) selected.add(Integer.parseInt(id));
            dao.saveMovieDistribution(movieId, selected);
            response.sendRedirect(request.getContextPath() + "/manager/distribution?movieId=" + movieId + "&saved=1");
        } catch (Exception exception) { response.sendError(400, "Không thể lưu phân phối phim."); }
    }
}


