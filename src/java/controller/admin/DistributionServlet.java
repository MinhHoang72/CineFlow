package controller.admin;

import dao.CinemaDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Branch;
import model.Movie;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Admin distributes movies to cinema branches across the system. */
public class DistributionServlet extends HttpServlet {
    private final CinemaDAO dao = new CinemaDAO();

    private String cityKey(String city) {
        return city == null || city.isBlank() ? "" : city.trim();
    }

    private String firstAvailableCity(List<Branch> branches, boolean preferDistributed) {
        if (preferDistributed) {
            for (Branch branch : branches) {
                if ("ACTIVE".equalsIgnoreCase(branch.getStatus()) && branch.isDistributed()) {
                    return cityKey(branch.getCity());
                }
            }
        }
        for (Branch branch : branches) return cityKey(branch.getCity());
        return "";
    }

    private boolean allowed(HttpSession session) {
        return session != null && "ADMIN".equals(session.getAttribute("role"));
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
            String scope = dao.getMovieDistributionScope(movieId);
            String city = dao.getMovieDistributionCity(movieId);
            Set<Integer> exceptions = dao.getMovieDistributionExceptions(movieId);
            boolean cityExists = false;
            boolean allActiveDistributed = true;
            boolean hasActiveBranch = false;
            boolean hasActiveDistribution = false;
            for (Branch branch : branches) {
                if (cityKey(branch.getCity()).equals(cityKey(city))) cityExists = true;
                if ("ACTIVE".equalsIgnoreCase(branch.getStatus())) {
                    hasActiveBranch = true;
                    if (!branch.isDistributed()) allActiveDistributed = false;
                    else hasActiveDistribution = true;
                }
            }
            if (scope == null) scope = !hasActiveDistribution || (hasActiveBranch && allActiveDistributed) ? "system" : "city";
            if ("selected".equals(scope)) scope = "city";
            if (!"system".equals(scope) && !"city".equals(scope)) scope = "city";
            if (city == null || !cityExists) city = firstAvailableCity(branches, "city".equals(scope));
            request.setAttribute("movie", movie);
            request.setAttribute("branches", branches);
            request.setAttribute("distributionScope", scope);
            request.setAttribute("distributionCity", cityKey(city));
            request.setAttribute("exceptionBranchIds", exceptions);
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
            String scope = request.getParameter("scope");
            if (!"system".equals(scope) && !"city".equals(scope)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Phạm vi phân phối không hợp lệ.");
                return;
            }

            List<Branch> branches = dao.getDistributionBranches(movieId);
            String city = cityKey(request.getParameter("city"));
            Set<Integer> selected = new HashSet<>();
            Set<Integer> activeIds = new HashSet<>();
            Set<Integer> activeCityIds = new HashSet<>();
            boolean cityExists = false;
            for (Branch branch : branches) {
                boolean inCity = cityKey(branch.getCity()).equals(city);
                if (inCity) cityExists = true;
                if ("ACTIVE".equalsIgnoreCase(branch.getStatus())) {
                    activeIds.add(branch.getBranchId());
                    if (inCity) activeCityIds.add(branch.getBranchId());
                }
            }
            if ("city".equals(scope) && !cityExists) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Thành phố không hợp lệ.");
                return;
            }

            Set<Integer> exceptions = new HashSet<>();
            if ("system".equals(scope)) {
                String[] exceptionValues = request.getParameterValues("exceptionBranchIds");
                if (exceptionValues != null) {
                    for (String id : exceptionValues) {
                        int branchId = Integer.parseInt(id);
                        if (activeIds.contains(branchId)) exceptions.add(branchId);
                    }
                }
                selected.addAll(activeIds);
                selected.removeAll(exceptions);
            } else {
                String[] ids = request.getParameterValues("branchIds");
                if (ids != null) {
                    for (String id : ids) {
                        int branchId = Integer.parseInt(id);
                        if (activeCityIds.contains(branchId)) selected.add(branchId);
                    }
                }
            }

            if (!dao.saveMovieDistribution(movieId, scope, city, selected, exceptions)) {
                throw new IllegalStateException("Không lưu được phân phối phim.");
            }
            response.sendRedirect(request.getContextPath() + "/admin/distribution?movieId=" + movieId + "&saved=1");
        } catch (Exception exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Không thể lưu phân phối phim.");
        }
    }
}
