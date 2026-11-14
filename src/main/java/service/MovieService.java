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

        if (movie.getTitle() == null || movie.getTitle().trim().length() > 200) {
            throw new IllegalArgumentException("invalid_title");
        }
        if (movieDAO.existsTitle(movie.getTitle(), movie.getMovieId())) {
            throw new IllegalArgumentException("duplicate_title");
        }

        Movie existing = movie.getMovieId() > 0 ? movieDAO.getMovieById(movie.getMovieId()) : null;

        // ENDED/ARCHIVED: giữ lại ngày đã có làm hồ sơ lịch sử.
        // Chỉ chốt EndDate = hôm nay khi ngày kết thúc đang trống hoặc còn ở tương lai.
        if ("ARCHIVED".equals(status) || "ENDED".equals(status)) {
            if (movie.getReleaseDate() == null && existing != null) {
                movie.setReleaseDate(existing.getReleaseDate());
            }
            if (movie.getEndDate() == null && existing != null) {
                movie.setEndDate(existing.getEndDate());
            }
            if (movie.getEndDate() == null || movie.getEndDate().after(today)) {
                movie.setEndDate(today);
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

        if ("ENDED".equals(targetStatus)) {
            return movieDAO.endMovie(movieId);
        } else if ("ARCHIVED".equals(targetStatus)) {
            return movieDAO.archiveMovie(movieId);
        } else {
            return movieDAO.updateMovieStatus(movieId, targetStatus);
        }
    }

    /**
     * Lấy danh sách toàn bộ cụm rạp và trạng thái đã phân phối (Distributed) thực tế của một bộ phim từ CSDL.
     * Quy tắc nghiệp vụ chuẩn:
     * - Khi tạo phim mới hoặc chưa có cấu hình: trả về đúng trạng thái thực tế trong database (mặc định chưa phân phối = false).
     * - Tuyệt đối không tự động đánh dấu tất cả cụm rạp là đã phân phối.
     *
     * @param movieId mã định danh phim
     * @return danh sách các cụm rạp kèm số lượng suất chiếu trong tương lai
     */
    public List<Centre> getDistributionCentres(int movieId) {
        if (movieId <= 0) {
            return Collections.emptyList();
        }
        return movieDAO.getDistributionCentres(movieId);
    }

    /**
     * Kết quả xử lý nghiệp vụ phân phối phim tới các cụm rạp.
     */
    public enum DistributionResult {
        SUCCESS,
        MOVIE_NOT_FOUND,
        MOVIE_ENDED_OR_ARCHIVED,
        FAILED
    }

    /**
     * Cập nhật danh sách các cụm rạp được phép chiếu bộ phim này trong một Transaction CSDL an toàn.
     * Toàn bộ quy tắc nghiệp vụ được thẩm định tập trung tại Service:
     * - Phim không tồn tại -> MOVIE_NOT_FOUND
     * - Phim ở trạng thái ARCHIVED hoặc ENDED không được phân phối -> MOVIE_ENDED_OR_ARCHIVED
     * - Lưu thành công -> SUCCESS
     * - Lỗi cơ sở dữ liệu -> FAILED
     *
     * @param movieId mã định danh phim
     * @param selectedCentreIds tập hợp mã ID các cụm rạp được chọn
     * @return DistributionResult chứa mã kết quả nghiệp vụ
     */
    public DistributionResult saveDistribution(int movieId, Set<Integer> selectedCentreIds) {
        if (movieId <= 0) {
            return DistributionResult.MOVIE_NOT_FOUND;
        }
        Movie targetMovie = movieDAO.getMovieById(movieId);
        if (targetMovie == null) {
            return DistributionResult.MOVIE_NOT_FOUND;
        }
        String st = targetMovie.getStatus() != null ? targetMovie.getStatus().toUpperCase() : "";
        if ("ARCHIVED".equals(st) || "ENDED".equals(st)) {
            if (selectedCentreIds != null && !selectedCentreIds.isEmpty()) {
                return DistributionResult.MOVIE_ENDED_OR_ARCHIVED;
            }
        }
        boolean ok = movieDAO.saveMovieDistribution(movieId, selectedCentreIds);
        return ok ? DistributionResult.SUCCESS : DistributionResult.FAILED;
    }

    /**
     * Cập nhật danh sách các cụm rạp được phép chiếu bộ phim này (hỗ trợ tương thích ngược boolean).
     *
     * @param movieId mã định danh phim
     * @param selectedCentreIds tập hợp mã ID các cụm rạp được chọn
     * @return true nếu lưu thành công toàn bộ, ngược lại false
     */
    public boolean saveMovieDistribution(int movieId, Set<Integer> selectedCentreIds) {
        return saveDistribution(movieId, selectedCentreIds) == DistributionResult.SUCCESS;
    }
}
