package service;

import dao.MovieDAO;
import model.entity.Centre;
import model.entity.Movie;
import utils.ValidationUtil;

import java.sql.Date;
import java.time.LocalDate;

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

    private final MovieDAO movieDAO;

    public MovieService() {
        this(new MovieDAO());
    }

    public MovieService(MovieDAO movieDAO) {
        this.movieDAO = movieDAO;
    }

    /**
     * Lấy toàn bộ danh sách phim kèm tỷ lệ phân phối rạp thực tế phục vụ màn hình quản trị.
     *
     * @return danh sách các phim sắp xếp mới nhất lên đầu
     */
    public List<Movie> getAllMovies() {
        return movieDAO.getAllMovies();
    }

    /**
     * Lấy danh sách phim đang chiếu tại các rạp.
     */
    public List<Movie> getNowShowingMovies() {
        return movieDAO.getNowShowingMovies();
    }

    /**
     * Lấy danh sách phim sắp chiếu.
     */
    public List<Movie> getComingSoonMovies() {
        return movieDAO.getComingSoonMovies();
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
        return movieDAO.getMovieById(movieId);
    }

    /**
     * Lấy thống kê số suất chiếu sắp tới và số vé đã bán của phim.
     *
     * @param movieId mã định danh phim
     * @return mảng int[] với [0] = số suất chiếu, [1] = số vé đã bán
     */
    public int[] getUpcomingStats(int movieId) {
        if (movieId <= 0) {
            return new int[]{0, 0};
        }
        return movieDAO.getUpcomingStats(movieId);
    }

    /**
     * Lấy danh sách phim có thể phân phối tới các rạp.
     * Logic nghiệp vụ:
     * 1. Bắt đầu bằng việc lấy toàn bộ danh sách phim từ DAO (getAllMovies).
     * 2. Lọc chỉ giữ lại các phim đang chiếu (NOW_SHOWING) hoặc sắp khởi chiếu (COMING_SOON).
     *
     * @return danh sách các bộ phim thỏa mãn điều kiện phân phối
     */
    public List<Movie> getDistributableMovies() {
        List<Movie> allMovies = getAllMovies();
        List<Movie> distributable = new java.util.ArrayList<>();
        if (allMovies != null) {
            for (Movie m : allMovies) {
                if ("NOW_SHOWING".equalsIgnoreCase(m.getStatus()) || "COMING_SOON".equalsIgnoreCase(m.getStatus())) {
                    distributable.add(m);
                }
            }
        }
        return distributable;
    }

    /**
     * Tự động xác định phim được chọn cho màn hình phân phối:
     * - Nếu có movieId hợp lệ từ request -> tìm và trả về phim đó trong danh sách.
     * - Nếu không truyền movieId -> lấy phim đầu tiên trong danh sách làm mặc định.
     *
     * @param movies danh sách phim phân phối
     * @param movieIdParam chuỗi tham số movieId từ request
     * @return đối tượng Movie được chọn, hoặc null nếu danh sách rỗng
     */
    public Movie resolveSelectedMovie(List<Movie> movies, String movieIdParam) {
        if (movies == null || movies.isEmpty()) {
            return null;
        }
        if (movieIdParam != null && !movieIdParam.isBlank()) {
            try {
                int id = Integer.parseInt(movieIdParam.trim());
                for (Movie m : movies) {
                    if (m.getMovieId() == id) {
                        return m;
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return movies.get(0);
    }


    /**
     * Lưu thông tin phim: Tự động phân nhánh Tạo mới (INSERT) hoặc Cập nhật (UPDATE).
     * Kiểm tra toàn bộ quy tắc nghiệp vụ về ngày phát hành, tính nhất quán trạng thái và chuyển đổi trạng thái.
     *
     * @param movie đối tượng thông tin phim
     * @return mã movieId của phim nếu thành công (> 0), ngược lại 0
     * @throws IllegalArgumentException nếu dữ liệu hoặc quy tắc chuyển trạng thái không hợp lệ
     */
    public int saveMovie(Movie movie) {
        if (movie == null) {
            return 0;
        }
        Date today = Date.valueOf(LocalDate.now());
        String status = movie.getStatus() != null ? movie.getStatus().trim().toUpperCase() : "";
        movie.setGenre(ValidationUtil.normalizeGenres(movie.getGenre()));

        if (movie.getTitle() == null || movie.getTitle().trim().length() > 200) {
            throw new IllegalArgumentException("invalid_title");
        }
        if (movie.getGenre().length() > 200) {
            throw new IllegalArgumentException("genre_too_long");
        }
        if (!ValidationUtil.isValidDuration(movie.getDuration())) {
            throw new IllegalArgumentException("invalid_duration");
        }
        if (!ValidationUtil.isValidAgeRating(movie.getAgeRating())) {
            throw new IllegalArgumentException("invalid_age_rating");
        }
        if (!ValidationUtil.isValidMovieStatus(status)) {
            throw new IllegalArgumentException("invalid_status");
        }
        if (movieDAO.existsTitle(movie.getTitle(), movie.getMovieId())) {
            throw new IllegalArgumentException("duplicate_title");
        }

        Movie existing = movie.getMovieId() > 0 ? movieDAO.getMovieById(movie.getMovieId()) : null;

        // ENDED/ARCHIVED: giữ lại ngày đã có làm hồ sơ lịch sử.
        // - Nếu phim đã khởi chiếu (ReleaseDate <= hôm nay): chốt EndDate = hôm nay khi còn ở tương lai.
        // - Nếu phim chưa tới ngày khởi chiếu (COMING_SOON bị cấm/hủy sớm): chốt EndDate = ReleaseDate để luôn đảm bảo EndDate >= ReleaseDate.
        if ("ARCHIVED".equals(status) || "ENDED".equals(status)) {
            if (movie.getReleaseDate() == null && existing != null) {
                movie.setReleaseDate(existing.getReleaseDate());
            }
            if (movie.getEndDate() == null && existing != null) {
                movie.setEndDate(existing.getEndDate());
            }
            Date minEndDate = (movie.getReleaseDate() != null && movie.getReleaseDate().after(today))
                    ? movie.getReleaseDate()
                    : today;
            if (movie.getEndDate() == null || movie.getEndDate().after(minEndDate)) {
                movie.setEndDate(minEndDate);
            }
        }

        // Kiểm tra chuyển trạng thái đối với phim đã có
        if (existing != null) {
            checkTransition(existing, status, movie.getReleaseDate(), movie.getEndDate());
        }

        // Bắt buộc có đủ ngày khởi chiếu và ngày kết thúc
        if (movie.getReleaseDate() == null || movie.getEndDate() == null) {
            throw new IllegalArgumentException("missing_dates");
        }

        // Kiểm tra khoảng thời gian chiếu (Ngày kết thúc phải lớn hơn hoặc bằng ngày khởi chiếu)
        if (!ValidationUtil.isValidDateRange(movie.getReleaseDate(), movie.getEndDate())) {
            throw new IllegalArgumentException("invalid_date_range");
        }

        if ("NOW_SHOWING".equals(status) && movie.getEndDate().before(today)) {
            throw new IllegalArgumentException("end_date_in_past");
        }
        if ("COMING_SOON".equals(status) && !movie.getEndDate().after(today)) {
            throw new IllegalArgumentException("end_date_not_future");
        }

        // Kiểm tra tính nhất quán giữa trạng thái và ngày phát hành
        String dateConsistencyError = ValidationUtil.validateMovieStatusDateConsistency(status, movie.getReleaseDate(), today);
        if (dateConsistencyError != null) {
            throw new IllegalArgumentException(dateConsistencyError);
        }

        if (movie.getMovieId() == 0) {
            return movieDAO.insertMovie(movie);
        } else {
            boolean updated = movieDAO.updateMovie(movie);
            return updated ? movie.getMovieId() : 0;
        }
    }

    /**
     * Kiểm tra quy tắc chuyển trạng thái từ trạng thái cũ sang COMING_SOON hoặc NOW_SHOWING.
     * Dùng chung cho cả saveMovie và updateMovieStatus.
     */
    private void checkTransition(Movie existing, String targetStatus, Date releaseDate, Date endDate) {
        if (existing == null) {
            return;
        }
        String oldStatus = existing.getStatus() != null ? existing.getStatus().toUpperCase() : "";
        LocalDate today = LocalDate.now();

        // 1. Phim ARCHIVED (đã lưu trữ/hết bản quyền): Không thể chuyển trực tiếp sang NOW_SHOWING
        if ("ARCHIVED".equals(oldStatus)) {
            if ("NOW_SHOWING".equals(targetStatus)) {
                throw new IllegalArgumentException("cannot_reopen_to_now_showing");
            }
            if ("COMING_SOON".equals(targetStatus)) {
                if (releaseDate == null || endDate == null) {
                    throw new IllegalArgumentException("reissue_dates_required");
                }
                if (existing.getEndDate() != null && !existing.getEndDate().toLocalDate().isAfter(today)) {
                    if (endDate.equals(existing.getEndDate())) {
                        throw new IllegalArgumentException("reissue_dates_required");
                    }
                }
                String dateErr = ValidationUtil.validateTransitionToComingSoon(oldStatus, targetStatus, releaseDate, endDate);
                if (dateErr != null) {
                    throw new IllegalArgumentException(dateErr);
                }
            }
        }

        // 2. Phim ENDED (đã kết thúc đợt chiếu):
        if ("ENDED".equals(oldStatus)) {
            // Chuyển sang NOW_SHOWING bắt buộc phải có ngày kết thúc mới từ hôm nay trở đi
            if ("NOW_SHOWING".equals(targetStatus)) {
                if (endDate == null || endDate.toLocalDate().isBefore(today)) {
                    throw new IllegalArgumentException("extend_end_date_required");
                }
            }
            if ("COMING_SOON".equals(targetStatus)) {
                if (releaseDate == null || endDate == null) {
                    throw new IllegalArgumentException("reissue_dates_required");
                }
                if (existing.getEndDate() != null && !existing.getEndDate().toLocalDate().isAfter(today)) {
                    if (endDate.equals(existing.getEndDate())) {
                        throw new IllegalArgumentException("reissue_dates_required");
                    }
                }
                String dateErr = ValidationUtil.validateTransitionToComingSoon(oldStatus, targetStatus, releaseDate, endDate);
                if (dateErr != null) {
                    throw new IllegalArgumentException(dateErr);
                }
            }
        }
    }

    /**
     * Cập nhật nhanh trạng thái phát hành của một bộ phim.
     * Quy tắc nghiệp vụ: Nếu trạng thái chuyển thành 'ENDED', tự động chốt ngày kết thúc (EndDate) là hôm nay.
     *
     * @param movieId mã định danh phim
     * @param status trạng thái mới (COMING_SOON, NOW_SHOWING, ENDED, ARCHIVED)
     * @return true nếu cập nhật thành công, ngược lại false
     * @throws IllegalArgumentException nếu chuyển trạng thái không hợp lệ
     */
    public boolean updateMovieStatus(int movieId, String status) {
        if (movieId <= 0 || status == null || status.isBlank()) {
            return false;
        }
        String targetStatus = status.trim().toUpperCase();

        Movie current = movieDAO.getMovieById(movieId);
        if (current == null) {
            return false;
        }

        String currentStatus = current.getStatus() != null ? current.getStatus().toUpperCase() : "";
        // Chặn đổi nhanh từ ENDED sang NOW_SHOWING vì không có ô nhập ngày kết thúc mới (yêu cầu vào form gia hạn)
        if ("ENDED".equals(currentStatus) && "NOW_SHOWING".equals(targetStatus)) {
            throw new IllegalArgumentException("extend_end_date_required");
        }

        // Kiểm tra quy tắc chuyển trạng thái
        checkTransition(current, targetStatus, current.getReleaseDate(), current.getEndDate());

        return movieDAO.updateMovieStatus(movieId, targetStatus);
    }


    /**
     * Lấy danh sách toàn bộ cụm rạp và trạng thái đã phân phối (Distributed) thực tế của một bộ phim từ CSDL.
     *
     * @param movieId mã định danh phim
     * @return danh sách các cụm rạp kèm cờ isDistributed
     */
    public List<Centre> getDistributionCentres(int movieId) {
        if (movieId <= 0) {
            return Collections.emptyList();
        }
        return movieDAO.getDistributionCentres(movieId);
    }

    /**
     * Cập nhật danh sách các cụm rạp được phép chiếu bộ phim này.
     * Logic nghiệp vụ:
     * - Chỉ phim đang chiếu (NOW_SHOWING) hoặc sắp chiếu (COMING_SOON) mới được phân phối.
     * - Phim không tồn tại, đã kết thúc (ENDED) hoặc lưu trữ (ARCHIVED) sẽ bị từ chối.
     *
     * @param movieId mã định danh phim
     * @param selectedCentreIds tập hợp mã ID các cụm rạp được chọn
     * @return true nếu lưu thành công, ngược lại false
     */
    public boolean saveDistribution(int movieId, Set<Integer> selectedCentreIds) {
        if (movieId <= 0) {
            return false;
        }
        Movie movie = movieDAO.getMovieById(movieId);
        if (movie == null) {
            return false;
        }
        String status = movie.getStatus() != null ? movie.getStatus().toUpperCase() : "";
        if (!"NOW_SHOWING".equals(status) && !"COMING_SOON".equals(status)) {
            return false;
        }
        return movieDAO.saveMovieDistribution(movieId, selectedCentreIds);
    }

    /**
     * Tương thích ngược: Ủy quyền cho saveDistribution.
     */
    public boolean saveMovieDistribution(int movieId, Set<Integer> selectedCentreIds) {
        return saveDistribution(movieId, selectedCentreIds);
    }
}
