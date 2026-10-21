package controller.admin;
import jakarta.servlet.annotation.WebServlet;

import dao.CinemaDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.entity.Centre;
import model.entity.Movie;
import model.entity.User;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Điều khiển chức năng Phân phối phim tới các trung tâm chiếu phim (/admin/distribution) dành cho Quản trị viên (Admin).
 * Nghiệp vụ:
 * - Chọn một bộ phim trong kho phim chuỗi
 * - Xem danh sách các cụm rạp đã hoặc chưa được phân phối phim này
 * - Bật/tắt gán phim cho từng cụm rạp cụ thể để rạp đó có quyền lên lịch chiếu
 */
@WebServlet(name = "DistributionServlet", urlPatterns = {"/admin/distribution"})
public class DistributionServlet extends HttpServlet {

    private final CinemaDAO dao = new CinemaDAO();

    /**
     * Kiểm tra quyền truy cập: Chỉ Quản trị viên (Admin) mới có quyền phân phối phim.
     *
     * @param session phiên làm việc hiện tại của người dùng
     * @return true nếu là Admin hợp lệ, ngược lại false
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

    /**
     * Xử lý yêu cầu HTTP GET: Tải danh sách phim và danh sách rạp được phân phối của phim đã chọn để hiển thị.
     *
     * @param request  yêu cầu HTTP từ client
     * @param response phản hồi HTTP gửi về client
     * @throws ServletException nếu xảy ra lỗi servlet
     * @throws IOException      nếu xảy ra lỗi vào/ra dữ liệu
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 1. Kiểm tra quyền Admin (nếu không phải Admin thì chặn và trả về lỗi 403 Forbidden)
        if (!allowed(request.getSession(false))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Admin được phân phối phim.");
            return;
        }

        // 2. Lấy toàn bộ danh sách phim từ database để hiển thị thanh chọn phim
        List<Movie> allMovies = dao.getAllMovies();
        Movie selectedMovie = null;

        // 3. Đọc tham số movieId từ URL (nếu người dùng bấm chọn một phim cụ thể)
        String movieIdValue = request.getParameter("movieId");
        if (movieIdValue != null && !movieIdValue.isBlank()) {
            try {
                int movieId = Integer.parseInt(movieIdValue.trim());
                // Tìm kiếm trong danh sách phim đã load
                for (Movie m : allMovies) {
                    if (m.getMovieId() == movieId) {
                        selectedMovie = m;
                        break;
                    }
                }
                // Nếu chưa tìm thấy thì truy vấn trực tiếp từ DAO
                if (selectedMovie == null) {
                    selectedMovie = dao.getMovieById(movieId);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        // 4. Nếu không truyền movieId, mặc định chọn phim đầu tiên (ưu tiên phim Đang chiếu hoặc Sắp chiếu)
        if (selectedMovie == null && allMovies != null && !allMovies.isEmpty()) {
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

        // 5. Lấy danh sách toàn bộ cụm rạp kèm trạng thái đã phân phối (checked/unchecked) của phim được chọn
        List<Centre> centres = java.util.Collections.emptyList();
        if (selectedMovie != null) {
            centres = dao.getDistributionCentres(selectedMovie.getMovieId());
        }

        // 6. Đưa dữ liệu vào request attributes và chuyển tiếp sang trang JSP hiển thị
        request.setAttribute("activeNav", "distribution");
        request.setAttribute("movie", selectedMovie);
        request.setAttribute("allMovies", allMovies != null ? allMovies : java.util.Collections.emptyList());
        request.setAttribute("centres", centres);
        request.getRequestDispatcher("/WEB-INF/views/movie/movieDistribution.jsp").forward(request, response);
    }

    /**
     * Xử lý yêu cầu HTTP POST: Cập nhật danh sách cụm rạp được phân phối cho phim đã chọn.
     *
     * @param request  yêu cầu HTTP từ client
     * @param response phản hồi HTTP gửi về client
     * @throws IOException nếu xảy ra lỗi vào/ra dữ liệu
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 1. Kiểm tra quyền Admin trước khi lưu phân phối
        if (!allowed(request.getSession(false))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            // 2. Thiết lập mã hóa UTF-8 cho yêu cầu
            request.setCharacterEncoding("UTF-8");

            // 3. Đọc mã phim và danh sách mã cụm rạp được tích chọn (checkbox centreIds)
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

            // 4. Gọi DAO cập nhật bảng quan hệ CentreMovies trong khối Transaction
            if (!dao.saveMovieDistribution(movieId, selectedCentreIds)) {
                throw new IllegalStateException("Không lưu được phân phối phim.");
            }

            // 5. Chuyển hướng về lại trang phân phối của phim đó kèm cờ thông báo thành công (saved=1)
            response.sendRedirect(request.getContextPath() + "/admin/distribution?movieId=" + movieId + "&saved=1");
        } catch (Exception exception) {
            // 6. Xử lý khi có lỗi dữ liệu hoặc lỗi cơ sở dữ liệu
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Không thể lưu phân phối phim.");
        }
    }
}
