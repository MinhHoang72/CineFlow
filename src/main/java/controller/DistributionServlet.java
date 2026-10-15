package controller;

import dao.CinemaDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Branch;
import model.Movie;
import model.User;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Admin distributes movies to cinema branches across the system. */
public class DistributionServlet extends HttpServlet {
    private final CinemaDAO dao = new CinemaDAO();

    private boolean allowed(HttpSession session) {
        if (session == null) return false;
        Object roleObj = session.getAttribute("role");
        if (roleObj instanceof String && "ADMIN".equalsIgnoreCase((String) roleObj)) {
            return true;
        }
        Object userObj = session.getAttribute("user");
        if (userObj instanceof User) {
            String role = ((User) userObj).getRole();
            return role != null && "ADMIN".equalsIgnoreCase(role);
        }
        return false;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!allowed(request.getSession(false))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Admin được phân phối phim.");
            return;
        }

        String movieIdValue = request.getParameter("movieId");
        if (movieIdValue == null || movieIdValue.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/admin/movies");
            return;
        }

        try {
            int movieId = Integer.parseInt(movieIdValue);
            Movie movie = dao.getMovieById(movieId);
            if (movie == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy phim.");
                return;
            }
            List<Branch> branches = dao.getDistributionBranches(movieId);
            request.setAttribute("movie", movie);
            request.setAttribute("branches", branches);
            request.getRequestDispatcher("/admin/distribution.jsp").forward(request, response);
        } catch (NumberFormatException exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã phim không hợp lệ.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!allowed(request.getSession(false))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            request.setCharacterEncoding("UTF-8");
            int movieId = Integer.parseInt(request.getParameter("movieId"));
            String[] ids = request.getParameterValues("branchIds");
            Set<Integer> selectedBranchIds = new HashSet<>();
            if (ids != null) {
                for (String id : ids) {
                    if (id != null && !id.isBlank()) {
                        selectedBranchIds.add(Integer.parseInt(id.trim()));
                    }
                }
            }

            if (!dao.saveMovieDistribution(movieId, selectedBranchIds)) {
                throw new IllegalStateException("Không lưu được phân phối phim.");
            }
            response.sendRedirect(request.getContextPath() + "/admin/distribution?movieId=" + movieId + "&saved=1");
        } catch (Exception exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Không thể lưu phân phối phim.");
        }
    }
}
