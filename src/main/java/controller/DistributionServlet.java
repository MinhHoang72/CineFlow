package controller;

import dao.CinemaDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Centre;
import model.Movie;
import model.User;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Điều khiển chức năng phân phối phim tới các trung tâm chiếu phim dành cho Quản trị viên (Admin).
 */
public class DistributionServlet extends HttpServlet {

    private final CinemaDAO dao = new CinemaDAO();

    /**
     * Kiểm tra quyền truy cập của Quản trị viên (Admin).
     */
    private boolean allowed(HttpSession session) {
        if (session == null) {
            return false;
        }
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

        List<Movie> allMovies = dao.getAllMovies();
        Movie selectedMovie = null;

        String movieIdValue = request.getParameter("movieId");
        if (movieIdValue != null && !movieIdValue.isBlank()) {
            try {
                int movieId = Integer.parseInt(movieIdValue.trim());
                for (Movie m : allMovies) {
                    if (m.getMovieId() == movieId) {
                        selectedMovie = m;
                        break;
                    }
                }
                if (selectedMovie == null) {
                    selectedMovie = dao.getMovieById(movieId);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        // Nếu không có movieId hoặc không tìm thấy, mặc định chọn phim đầu tiên
        if (selectedMovie == null && allMovies != null && !allMovies.isEmpty()) {
            // Ưu tiên phim Đang chiếu hoặc Sắp khởi chiếu
            for (Movie m : allMovies) {
                if ("NOW_SHOWING".equalsIgnoreCase(m.getStatus()) || "COMING_SOON".equalsIgnoreCase(m.getStatus())) {
                    selectedMovie = m;
                    break;
                }
            }
            if (selectedMovie == null) {
                selectedMovie = allMovies.get(0);
            }
        }

        List<Centre> centres = java.util.Collections.emptyList();
        if (selectedMovie != null) {
            centres = dao.getDistributionCentres(selectedMovie.getMovieId());
        }

        request.setAttribute("activeNav", "distribution");
        request.setAttribute("movie", selectedMovie);
        request.setAttribute("allMovies", allMovies != null ? allMovies : java.util.Collections.emptyList());
        request.setAttribute("centres", centres);
        request.getRequestDispatcher("/admin/distribution.jsp").forward(request, response);
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
            String[] ids = request.getParameterValues("centreIds");
            Set<Integer> selectedCentreIds = new HashSet<>();
            if (ids != null) {
                for (String id : ids) {
                    if (id != null && !id.isBlank()) {
                        selectedCentreIds.add(Integer.parseInt(id.trim()));
                    }
                }
            }

            if (!dao.saveMovieDistribution(movieId, selectedCentreIds)) {
                throw new IllegalStateException("Không lưu được phân phối phim.");
            }
            response.sendRedirect(request.getContextPath() + "/admin/distribution?movieId=" + movieId + "&saved=1");
        } catch (Exception exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Không thể lưu phân phối phim.");
        }
    }
}
