package service;

import dao.CinemaDAO;
import model.entity.Centre;
import model.entity.Movie;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Service: Xử lý toàn bộ logic nghiệp vụ liên quan đến Phim (Movie) và Phân phối phim (Distribution).
 * Nhiệm vụ:
 * - Quản lý danh mục phim: Lấy danh sách phim kèm số lượng rạp phân phối, tìm kiếm theo mã định danh.
 * - Quản lý vòng đời phim: Thêm mới, cập nhật thông tin, thay đổi trạng thái (COMING_SOON, NOW_SHOWING, ENDED, ARCHIVED).
 * - Quản lý phân phối phim tới các cụm rạp chiếu: Lấy danh sách rạp kèm số lượng suất chiếu tương lai, cập nhật gán phim theo cụm rạp trong khối Transaction an toàn.
 */
public class MovieService {

    private final CinemaDAO cinemaDAO;

    public MovieService() {
        this(new CinemaDAO());
    }

    public MovieService(CinemaDAO cinemaDAO) {
        this.cinemaDAO = cinemaDAO;
    }

    /**
     * Lấy toàn bộ danh sách phim kèm tỷ lệ phân phối rạp thực tế phục vụ màn hình quản trị.
     *
     * @return danh sách các phim sắp xếp mới nhất lên đầu
     */
    public List<Movie> getAllMovies() {
        return cinemaDAO.getAllMovies();
    }

    /**
     * Tìm kiếm thông tin chi tiết một bộ phim theo mã định danh.
     *
     * @param movieId mã định danh phim
     * @return đối tượng Movie nếu tìm thấy, ngược lại null
     */
    public Movie getMovieById(int movieId) {
        if (movieId <= 0) {
            return null;
        }
        return cinemaDAO.getMovieById(movieId);
    }

    /**
     * Tự động xác định phim được chọn cho màn hình phân phối/chi tiết:
     * - Nếu có movieId hợp lệ từ client -> tìm và trả về phim đó.
     * - Nếu không truyền movieId -> ưu tiên chọn phim đầu tiên đang chiếu (NOW_SHOWING) hoặc sắp chiếu (COMING_SOON).
     *
     * @param allMovies danh sách tất cả phim hiện có
     * @param movieIdParam chuỗi tham số movieId từ request
     * @return đối tượng Movie được chọn, hoặc null nếu danh sách rỗng
     */
    public Movie resolveSelectedMovie(List<Movie> allMovies, String movieIdParam) {
        if (movieIdParam != null && !movieIdParam.isBlank()) {
            try {
                int id = Integer.parseInt(movieIdParam.trim());
                if (allMovies != null) {
                    for (Movie m : allMovies) {
                        if (m.getMovieId() == id) {
                            return m;
                        }
                    }
                }
                return getMovieById(id);
            } catch (NumberFormatException ignored) {
            }
        }

        if (allMovies != null && !allMovies.isEmpty()) {
            for (Movie m : allMovies) {
                if ("NOW_SHOWING".equalsIgnoreCase(m.getStatus()) || "COMING_SOON".equalsIgnoreCase(m.getStatus())) {
                    return m;
                }
            }
            return allMovies.get(0);
        }
        return null;
    }

    /**
     * Lưu thông tin phim: Tự động phân nhánh Tạo mới (INSERT) hoặc Cập nhật (UPDATE).
     *
     * @param movie đối tượng thông tin phim
     * @return mã movieId của phim nếu thành công (> 0), ngược lại 0
     */
    public int saveMovie(Movie movie) {
        if (movie == null) {
            return 0;
        }
        if (movie.getMovieId() == 0) {
            return cinemaDAO.createMovie(movie);
        } else {
            boolean updated = cinemaDAO.updateMovie(movie);
            return updated ? movie.getMovieId() : 0;
        }
    }

    /**
     * Cập nhật nhanh trạng thái phát hành của một bộ phim.
     * Quy tắc nghiệp vụ: Nếu trạng thái chuyển thành 'ENDED', tự động chốt ngày kết thúc (EndDate) là hôm nay.
     *
     * @param movieId mã định danh phim
     * @param status trạng thái mới (COMING_SOON, NOW_SHOWING, ENDED, ARCHIVED)
     * @return true nếu cập nhật thành công, ngược lại false
     */
    public boolean updateMovieStatus(int movieId, String status) {
        if (movieId <= 0 || status == null || status.isBlank()) {
            return false;
        }
        String targetStatus = status.trim().toUpperCase();
        if ("ENDED".equals(targetStatus)) {
            return cinemaDAO.endMovie(movieId);
        } else {
            return cinemaDAO.updateMovieStatus(movieId, targetStatus);
        }
    }

    /**
     * Lấy danh sách toàn bộ cụm rạp và trạng thái đã phân phối (Distributed) của một bộ phim.
     *
     * @param movieId mã định danh phim
     * @return danh sách các cụm rạp kèm số lượng suất chiếu trong tương lai
     */
    public List<Centre> getDistributionCentres(int movieId) {
        if (movieId <= 0) {
            return Collections.emptyList();
        }
        List<Centre> centres = cinemaDAO.getDistributionCentres(movieId);
        // Quy tắc Opt-out thông minh: Phim mới chưa từng cấu hình phân phối trong CSDL
        // sẽ mặc định phân phối cho toàn bộ các cụm rạp đang hoạt động (ACTIVE).
        if (!cinemaDAO.hasDistributionConfig(movieId)) {
            for (Centre c : centres) {
                if ("ACTIVE".equalsIgnoreCase(c.getStatus())) {
                    c.setDistributed(true);
                }
            }
        }
        return centres;
    }

    /**
     * Cập nhật danh sách các cụm rạp được phép chiếu bộ phim này trong một Transaction CSDL an toàn.
     *
     * @param movieId mã định danh phim
     * @param selectedCentreIds tập hợp mã ID các cụm rạp được chọn
     * @return true nếu lưu thành công toàn bộ, ngược lại false
     */
    public boolean saveMovieDistribution(int movieId, Set<Integer> selectedCentreIds) {
        if (movieId <= 0) {
            return false;
        }
        return cinemaDAO.saveMovieDistribution(movieId, selectedCentreIds);
    }
}
