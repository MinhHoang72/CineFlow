import dao.CentreDAO;
import dao.MovieDAO;
import model.entity.Centre;
import model.entity.Movie;
import service.MovieService;
import utils.DBContext;
import utils.ValidationUtil;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test runner kiểm thử 3 quy tắc nghiệp vụ:
 * 1. Khi ấn archive hoặc end phim -> ngày kết thúc lập tức kích hoạt là hôm nay, phân phối tại các rạp bị hủy.
 * 2. Khi phim chuyển từ ARCHIVED/ENDED sang COMING_SOON -> bắt buộc validation ngày phát hành và kết thúc, không được lưu khi chưa set lại ngày tháng tương lai.
 * 3. Phim ở trạng thái ARCHIVED hoặc ENDED không còn được phân phối nữa ở các rạp.
 */
public class MovieArchiveAndReissueTestRunner {

    private static final MovieDAO movieDAO = new MovieDAO();
    private static final MovieService movieService = new MovieService();
    private static final CentreDAO centreDAO = new CentreDAO();

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("  KIỂM THỬ NGHIỆP VỤ ARCHIVE, VALIDATION REISSUE VÀ DỪNG PHÂN PHỐI");
        System.out.println("======================================================================");

        int testCentreId = 0;
        int testMovieId = 0;

        try {
            // Setup centre
            testCentreId = createTestCentre("CineFlow Test Centre - Archive");
            // Setup movie
            testMovieId = createTestMovie("Phim Test Archive Quy Trinh", LocalDate.now().minusDays(10), LocalDate.now().plusDays(20), "NOW_SHOWING");

            // Phân phối phim cho rạp
            Set<Integer> distSet = new HashSet<>();
            distSet.add(testCentreId);
            boolean distOk = movieDAO.saveMovieDistribution(testMovieId, distSet);
            assertCondition(distOk, "Phân phối ban đầu cho phim NOW_SHOWING phải thành công");

            // Kiểm tra phân phối đang ACTIVE
            List<Centre> initialCentres = movieDAO.getDistributionCentres(testMovieId);
            boolean foundActive = false;
            for (Centre c : initialCentres) {
                if (c.getCentreId() == testCentreId && c.isDistributed()) {
                    foundActive = true;
                    break;
                }
            }
            assertCondition(foundActive, "Rạp test phải có trạng thái Distributed = true");

            // -------------------------------------------------------------
            // TEST 1: Khi archive phim -> EndDate lập tức thành hôm nay & phân phối bị hủy
            // -------------------------------------------------------------
            System.out.println("\n[TEST 1] Archive phim -> EndDate = hôm nay và hủy toàn bộ phân phối");
            boolean archiveOk = movieService.updateMovieStatus(testMovieId, "ARCHIVED");
            assertCondition(archiveOk, "Cập nhật phim sang ARCHIVED phải thành công");

            Movie archivedMovie = movieDAO.getMovieById(testMovieId);
            assertCondition("ARCHIVED".equalsIgnoreCase(archivedMovie.getStatus()), "Trạng thái phim phải là ARCHIVED");
            assertCondition(archivedMovie.getEndDate() != null && archivedMovie.getEndDate().toLocalDate().equals(LocalDate.now()),
                    "EndDate của phim ARCHIVED phải được cập nhật ngay lập tức thành hôm nay: " + archivedMovie.getEndDate());

            // Kiểm tra CentreMovies trong database đã chuyển sang CANCELLED và IsActive = 0
            String distStatusInDb = getCentreMovieStatus(testCentreId, testMovieId);
            assertCondition("CANCELLED".equalsIgnoreCase(distStatusInDb), "Bản ghi CentreMovies phải chuyển sang CANCELLED, thực tế: " + distStatusInDb);

            // Kiểm tra getDistributionCentres trả về isDistributed = false
            List<Centre> postCentres = movieDAO.getDistributionCentres(testMovieId);
            for (Centre c : postCentres) {
                if (c.getCentreId() == testCentreId) {
                    assertCondition(!c.isDistributed(), "Sau khi archive, rạp không còn được phân phối (isDistributed = false)");
                }
            }
            System.out.println("  [PASS] Test 1 hoàn thành xuất sắc.");

            // -------------------------------------------------------------
            // TEST 2: Phim ARCHIVED không cho phép phân phối lại
            // -------------------------------------------------------------
            System.out.println("\n[TEST 2] Phim ARCHIVED không được phân phối ở bất kỳ rạp nào");
            boolean saveDistArchived = movieService.saveMovieDistribution(testMovieId, distSet);
            assertCondition(!saveDistArchived, "Lưu phân phối cho phim ARCHIVED phải bị từ chối (trả về false)");
            System.out.println("  [PASS] Test 2 hoàn thành xuất sắc.");

            // -------------------------------------------------------------
            // TEST 3: Chuyển từ ARCHIVED sang COMING_SOON khi chưa set lại ngày tháng tương lai -> Bị chặn
            // -------------------------------------------------------------
            System.out.println("\n[TEST 3] Chuyển ARCHIVED -> COMING_SOON khi chưa set ngày tháng mới -> Phải bị validation chặn");
            // Đổi nhanh không kèm ngày tháng:
            boolean quickSwitchBlocked = movieDAO.updateMovieStatus(testMovieId, "COMING_SOON");
            assertCondition(!quickSwitchBlocked, "Đổi nhanh sang COMING_SOON khi ngày kết thúc là hôm nay/quá khứ phải bị chặn");

            // Kiểm tra ValidationUtil
            String valErrNoDateChange = ValidationUtil.validateTransitionToComingSoon(
                    "ARCHIVED", "COMING_SOON",
                    archivedMovie.getReleaseDate(), // trong quá khứ
                    archivedMovie.getEndDate()     // hôm nay
            );
            assertCondition(valErrNoDateChange != null, "ValidationUtil phải bắt lỗi reissue_dates_required hoặc coming_soon_in_past");
            System.out.println("  [PASS] Test 3 chặn chuyển đổi không hợp lệ thành công (mã lỗi: " + valErrNoDateChange + ").");

            // -------------------------------------------------------------
            // TEST 4: Chuyển từ ARCHIVED sang COMING_SOON khi ĐÃ SET ngày tháng mới trong tương lai -> Thành công
            // -------------------------------------------------------------
            System.out.println("\n[TEST 4] Chuyển ARCHIVED -> COMING_SOON với ngày khởi chiếu và kết thúc mới trong tương lai");
            LocalDate newRel = LocalDate.now().plusDays(10);
            LocalDate newEnd = LocalDate.now().plusDays(40);

            String valErrValid = ValidationUtil.validateTransitionToComingSoon(
                    "ARCHIVED", "COMING_SOON",
                    Date.valueOf(newRel),
                    Date.valueOf(newEnd)
            );
            assertCondition(valErrValid == null, "ValidationUtil phải cho phép khi ngày tháng hợp lệ trong tương lai");

            archivedMovie.setStatus("COMING_SOON");
            archivedMovie.setReleaseDate(Date.valueOf(newRel));
            archivedMovie.setEndDate(Date.valueOf(newEnd));
            boolean updateMovieOk = movieDAO.updateMovie(archivedMovie);
            assertCondition(updateMovieOk, "Cập nhật thông tin phim sang COMING_SOON với ngày mới phải thành công");

            Movie updatedMovie = movieDAO.getMovieById(testMovieId);
            assertCondition("COMING_SOON".equalsIgnoreCase(updatedMovie.getStatus()), "Trạng thái mới phải là COMING_SOON");
            assertCondition(updatedMovie.getReleaseDate().toLocalDate().equals(newRel), "ReleaseDate mới phải được lưu");
            assertCondition(updatedMovie.getEndDate().toLocalDate().equals(newEnd), "EndDate mới phải được lưu");
            System.out.println("  [PASS] Test 4 chuyển sang COMING_SOON thành công.");

            // -------------------------------------------------------------
            // TEST 5: Phim COMING_SOON sau khi tái phát hành được phân phối lại bình thường
            // -------------------------------------------------------------
            System.out.println("\n[TEST 5] Phim COMING_SOON được phân phối lại bình thường");
            boolean reDistOk = movieService.saveMovieDistribution(testMovieId, distSet);
            assertCondition(reDistOk, "Phân phối cho phim COMING_SOON phải thành công");
            System.out.println("  [PASS] Test 5 phân phối lại thành công.");

            System.out.println("\n======================================================================");
            System.out.println("  [SUCCESS] TẤT CẢ CÁC ĐẶC TẢ NGHIỆP VỤ MỚI ĐÃ ĐƯỢC KIỂM CHỨNG 100%!");
            System.out.println("======================================================================");

        } catch (Exception e) {
            System.err.println("❌ LỖI TRONG QUÁ TRÌNH TEST: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanupData(testCentreId, testMovieId);
        }
    }

    private static void assertCondition(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("ASSERTION FAILED: " + message);
        }
        System.out.println("  ✓ " + message);
    }

    private static int createTestCentre(String name) throws Exception {
        String sql = "INSERT INTO Centres (CentreName, Address, City, Phone, Status, CreatedAt, UpdatedAt) "
                + "OUTPUT INSERTED.CentreID VALUES (?, ?, ?, ?, 'ACTIVE', GETDATE(), GETDATE())";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, "Test Address");
            ps.setString(3, "Hà Nội");
            ps.setString(4, "0999888777");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new IllegalStateException("Không tạo được rạp test");
    }

    private static int createTestMovie(String title, LocalDate rel, LocalDate end, String status) throws Exception {
        String sql = "INSERT INTO Movies (Title, Description, Duration, Genre, Director, Cast, Language, "
                + "ReleaseDate, EndDate, PosterURL, TrailerURL, AgeRating, Status, CreatedAt, UpdatedAt) "
                + "OUTPUT INSERTED.MovieID VALUES (?, 'Mô tả test', 120, 'Hành động', 'Đạo diễn', 'Diễn viên', 'Tiếng Việt', "
                + "?, ?, '/test.jpg', 'https://youtube.com', 'T16', ?, GETDATE(), GETDATE())";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nameWithTimestamp(title));
            ps.setDate(2, Date.valueOf(rel));
            ps.setDate(3, Date.valueOf(end));
            ps.setString(4, status);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new IllegalStateException("Không tạo được phim test");
    }

    private static String getCentreMovieStatus(int centreId, int movieId) throws Exception {
        String sql = "SELECT Status FROM CentreMovies WHERE CentreID = ? AND MovieID = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, centreId);
            ps.setInt(2, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        }
        return null;
    }

    private static String nameWithTimestamp(String base) {
        return base + " (" + System.currentTimeMillis() % 10000 + ")";
    }

    private static void cleanupData(int centreId, int movieId) {
        try (Connection conn = DBContext.getConnection()) {
            if (movieId > 0) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM CentreMovies WHERE MovieID = ?")) {
                    ps.setInt(1, movieId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Movies WHERE MovieID = ?")) {
                    ps.setInt(1, movieId);
                    ps.executeUpdate();
                }
            }
            if (centreId > 0) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Centres WHERE CentreID = ?")) {
                    ps.setInt(1, centreId);
                    ps.executeUpdate();
                }
            }
            System.out.println("[CLEANUP] Đã dọn sạch dữ liệu test.");
        } catch (Exception e) {
            System.err.println("Lỗi khi cleanup: " + e.getMessage());
        }
    }
}
